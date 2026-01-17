package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("new-test")
public class AbstractStream200Test extends TestBase {

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void test_select() {
        List<Object> source = Arrays.asList(1, "two", 3.0, 4, "five");
        List<Integer> result = Stream.of(source).select(Integer.class).toList();
        assertEquals(Arrays.asList(1, 4), result);
    }

    @Test
    public void test_pairWith() {
        List<Pair<Integer, String>> result = Stream.of(1, 2, 3).pairWith(String::valueOf).toList();
        assertEquals(Arrays.asList(Pair.of(1, "1"), Pair.of(2, "2"), Pair.of(3, "3")), result);
    }

    @Test
    public void test_skipUntil() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 2).skipUntil(x -> x > 3).toList();
        assertEquals(Arrays.asList(4, 5, 2), result);
    }

    @Test
    public void test_filterWithActionOnDropped() {
        List<Integer> droppedItems = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, droppedItems::add).toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 3, 5), droppedItems);
    }

    @Test
    public void test_dropWhileWithActionOnDropped() {
        List<Integer> droppedItems = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 1).dropWhile(x -> x < 3, droppedItems::add).toList();
        assertEquals(Arrays.asList(3, 4, 1), result);
        assertEquals(Arrays.asList(1, 2), droppedItems);
    }

    @Test
    public void test_step() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6).step(2).toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void test_slidingMap_biFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 4).slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7), result);
    }

    @Test
    public void test_slidingMap_biFunctionWithIncrement() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).slidingMap(2, (a, b) -> a + (b == null ? 0 : b)).toList();
        assertEquals(Arrays.asList(3, 7, 5), result);
    }

    @Test
    public void test_slidingMap_triFunction() {
        List<String> result = Stream.of("a", "b", "c", "d").slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList("abc", "bcd"), result);
    }

    @Test
    public void test_slidingMap_triFunctionWithIncrement() {
        List<String> result = Stream.of("a", "b", "c", "d", "e", "d").slidingMap(2, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList("abc", "cde", "ednull"), result);
    }

    @Test
    public void test_mapIfNotNull() {
        List<String> source = Arrays.asList("a", null, "b");
        List<String> result = Stream.of(source).mapIfNotNull(String::toUpperCase).toList();
        assertEquals(Arrays.asList("A", "B"), result);
    }

    @Test
    public void test_mapToEntry_withMapper() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(i -> Pair.of(i, "v" + i)).toList();
        assertEquals(Arrays.asList(Pair.of(1, "v1"), Pair.of(2, "v2")), result);
    }

    @Test
    public void test_mapToEntry_withKeyAndValueMappers() {
        List<Map.Entry<Integer, String>> result = Stream.of(1, 2).mapToEntry(i -> i, i -> "v" + i).map(e -> Pair.of(e.getKey(), e.getValue())).toList();
        assertEquals(Arrays.asList(Pair.of(1, "v1"), Pair.of(2, "v2")), result);
    }

    @Test
    public void test_flatmap() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flatmap(Fn.identity()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_flattMap_array() {
        List<Integer> result = Stream.of("1,2", "3,4").flattmap(s -> s.split(",")).map(i -> Numbers.toInt(i)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_flattmap_jdkStream() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).flattMap(Collection::stream).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_flatmapToChar() {
        long count = Stream.of("a", "bc").flatmapToChar(s -> s.toCharArray()).count();
        assertEquals(3L, count);
    }

    @Test
    public void test_flatmapToByte() {
        byte[] bytes1 = { 1, 2 };
        byte[] bytes2 = { 3, 4 };
        long count = Stream.of(bytes1, bytes2).flatmapToByte(b -> b).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatmapToShort() {
        short[] shorts1 = { 1, 2 };
        short[] shorts2 = { 3, 4 };
        long count = Stream.of(shorts1, shorts2).flatmapToShort(s -> s).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatmapToInt() {
        int[] ints1 = { 1, 2 };
        int[] ints2 = { 3, 4 };
        long count = Stream.of(ints1, ints2).flatmapToInt(i -> i).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatmapToLong() {
        long[] longs1 = { 1L, 2L };
        long[] longs2 = { 3L, 4L };
        long count = Stream.of(longs1, longs2).flatmapToLong(l -> l).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatmapToFloat() {
        float[] floats1 = { 1.0f, 2.0f };
        float[] floats2 = { 3.0f, 4.0f };
        long count = Stream.of(floats1, floats2).flatmapToFloat(f -> f).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatmapToDouble() {
        double[] doubles1 = { 1.0, 2.0 };
        double[] doubles2 = { 3.0, 4.0 };
        long count = Stream.of(doubles1, doubles2).flatmapToDouble(d -> d).count();
        assertEquals(4L, count);
    }

    @Test
    public void test_flatmapIfNotNull_singleMapper() {
        List<List<Integer>> source = Arrays.asList(Arrays.asList(1, 2), null, Arrays.asList(3, 4));
        List<Integer> result = Stream.of(source).flatmapIfNotNull(Fn.identity()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_flatmapIfNotNull_doubleMapper() {
        List<List<List<Integer>>> source = Arrays.asList(Arrays.asList(Arrays.asList(1), null, Arrays.asList(2)), null, Arrays.asList(Arrays.asList(3, 4)));
        List<Integer> result = Stream.of(source).flatmapIfNotNull(Fn.identity(), Fn.identity()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_flatMapToEntry_streamMapper() {
        Map<Integer, String> map1 = Map.of(1, "a");
        Map<Integer, String> map2 = Map.of(2, "b");
        List<Map.Entry<Integer, String>> result = Stream.of(map1, map2)
                .flatMapToEntry(m -> Stream.of(m.entrySet()))
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .toList();
        assertTrue(result.contains(Pair.of(1, "a")));
        assertTrue(result.contains(Pair.of(2, "b")));
    }

    @Test
    public void test_flatmapToEntry_mapMapper() {
        Map<Integer, String> map1 = Map.of(1, "a");
        Map<Integer, String> map2 = Map.of(2, "b");
        List<Map.Entry<Integer, String>> result = Stream.of(map1, map2).flatmapToEntry(m -> m).map(e -> Pair.of(e.getKey(), e.getValue())).toList();
        assertTrue(result.contains(Pair.of(1, "a")));
        assertTrue(result.contains(Pair.of(2, "b")));
    }

    @Test
    public void test_flattMapToEntry_entryStreamMapper() {
        Map<Integer, String> map1 = Map.of(1, "a");
        Map<Integer, String> map2 = Map.of(2, "b");
        List<Map.Entry<Integer, String>> result = Stream.of(map1, map2)
                .flattMapToEntry(m -> Stream.of(m).mapToEntry(Fn.identity()))
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .toList();
        assertTrue(result.contains(Pair.of(1, "a")));
        assertTrue(result.contains(Pair.of(2, "b")));
    }

    @Test
    public void test_mapMulti() {
        List<String> result = Stream.of(1, 2).<String> mapMulti((i, consumer) -> {
            consumer.accept("v" + i);
            consumer.accept("v" + (i * 2));
        }).toList();
        assertEquals(Arrays.asList("v1", "v2", "v2", "v4"), result);
    }

    @Test
    public void test_mapMultiToInt() {
        List<Integer> result = Stream.of("1", "2").mapMultiToInt((s, consumer) -> consumer.accept(Integer.parseInt(s))).boxed().toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void test_mapMultiToLong() {
        List<Long> result = Stream.of("1", "2").mapMultiToLong((s, consumer) -> consumer.accept(Long.parseLong(s))).boxed().toList();
        assertEquals(Arrays.asList(1L, 2L), result);
    }

    @Test
    public void test_mapMultiToDouble() {
        List<Double> result = Stream.of("1.1", "2.2").mapMultiToDouble((s, consumer) -> consumer.accept(Double.parseDouble(s))).boxed().toList();
        assertEquals(Arrays.asList(1.1, 2.2), result);
    }

    @Test
    public void test_mapPartial() {
        List<String> result = Stream.of(1, 2, 3).mapPartial(i -> i % 2 == 0 ? Optional.of("even") : Optional.empty()).toList();
        assertEquals(List.of("even"), result);
    }

    @Test
    public void test_mapPartialToInt() {
        List<Integer> result = Stream.of("1", "a", "3").mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }).boxed().toList();
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void test_mapPartialToLong() {
        List<Long> result = Stream.of("1", "a", "3").mapPartialToLong(s -> {
            try {
                return OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }).boxed().toList();
        assertEquals(Arrays.asList(1L, 3L), result);
    }

    @Test
    public void test_mapPartialToDouble() {
        List<Double> result = Stream.of("1.1", "a", "3.3").mapPartialToDouble(s -> {
            try {
                return OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return OptionalDouble.empty();
            }
        }).boxed().toList();
        assertEquals(Arrays.asList(1.1, 3.3), result);
    }

    @Test
    public void test_rangeMap() {
        List<String> result = Stream.of("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "->" + b).toList();
        assertEquals(Arrays.asList("a->ac", "b->b", "c->cb"), result);
    }

    @Test
    public void test_collapse_biPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 4, 5, 3, 6).collapse((a, b) -> b > a).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 4, 5), Arrays.asList(3, 6)), result);
    }

    @Test
    public void test_collapse_biPredicateAndSupplier() {
        List<Set<Integer>> result = Stream.of(1, 1, 2, 2, 3).collapse((a, b, c) -> a == c, Suppliers.ofSet()).toList();
        assertEquals(Arrays.asList(Set.of(1), Set.of(2), Set.of(3)), result);
    }

    @Test
    public void test_collapse_biPredicateAndMergeFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p < c, (r, c) -> r + c).toList();
        assertEquals(Arrays.asList(6, 3, 2, 1), result);
    }

    @Test
    public void test_collapse_biPredicateAndInitAndOp() {
        List<Integer> result = Stream.of(1, 2, 3, 1).collapse((p, c) -> p < c, 0, (r, c) -> r + c).toList();
        assertEquals(Arrays.asList(6, 1), result);
    }

    @Test
    public void test_collapse_biPredicateAndCollector() {
        List<Integer> result = Stream.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p < c, Collectors.summingInt(i -> i)).toList();
        assertEquals(Arrays.asList(6, 3, 2, 1), result);
    }

    @Test
    public void test_scan_accumulator() {
        List<Integer> result = Stream.of(1, 2, 3).scan((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(1, 3, 6), result);
    }

    @Test
    public void test_scan_initAndAccumulator() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);
    }

    @Test
    public void test_scan_initAndAccumulatorAndFlag_true() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10, 11, 13, 16), result);
    }

    @Test
    public void test_scan_initAndAccumulatorAndFlag_false() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, false, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);
    }

    @Test
    public void test_split_bySize() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).split(2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), List.of(5)), result);
    }

    @Test
    public void test_split_byPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5, 6).split(i -> i % 3 == 0).toList();
        assertEquals(Arrays.asList(List.of(1, 2), List.of(3), List.of(4, 5), List.of(6)), result);
    }

    @Test
    public void test_splitAt_byPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).splitAt(i -> i == 3).map(Stream::toList).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void test_slide() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).slide(3).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void test_sliding_withIncrement() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).slide(3, 2).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void test_intersperse() {
        List<Integer> result = Stream.of(1, 2, 3).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Test
    public void test_onFirst() {
        Holder<Integer> first = new Holder<>();
        Stream.of(1, 2, 3).onFirst(first::setValue).forEach(Fn.emptyConsumer());
        assertEquals(1, first.value());
    }

    @Test
    public void test_onLast() {
        Holder<Integer> last = new Holder<>();
        Stream.of(1, 2, 3).onLast(last::setValue).forEach(Fn.emptyConsumer());
        assertEquals(3, last.value());
    }

    @Test
    public void test_forEach() {
        List<Integer> list = new ArrayList<>();
        Stream.of(1, 2, 3).forEach(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_forEachIndexed() {
        Map<Integer, Integer> map = new HashMap<>();
        Stream.of(10, 20, 30).forEachIndexed((i, e) -> map.put(i, e));
        assertEquals(Map.of(0, 10, 1, 20, 2, 30), map);
    }

    @Test
    public void test_forEachUntil_biConsumer() {
        List<Integer> list = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachUntil((e, flag) -> {
            list.add(e);
            if (e == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_forEachUntil_mutableBoolean() {
        List<Integer> list = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        Stream.of(1, 2, 3, 4).forEachUntil(flag, e -> {
            list.add(e);
            if (e == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    //    @Test
    //    public void test_reduceUntil() {
    //        Optional<Integer> result = Stream.of(1, 2, 3, 4, 5).reduceUntil((a, b) -> a + b, sum -> sum > 6);
    //        assertTrue(result.isPresent());
    //        assertEquals(10, result.get());
    //    }

    @Test
    public void test_groupBy_keyMapper() {
        Map<Integer, List<Integer>> result = Stream.of(1, 2, 3, 4, 5).groupByToEntry(i -> i % 2).toMap();
        assertEquals(Map.of(0, Arrays.asList(2, 4), 1, Arrays.asList(1, 3, 5)), result);
    }

    @Test
    public void test_groupBy_keyAndValueMappers() {
        Map<Integer, List<String>> result = Stream.of(1, 2, 3, 4, 5).groupByToEntry(i -> i % 2, String::valueOf).toMap();
        assertEquals(Map.of(0, Arrays.asList("2", "4"), 1, Arrays.asList("1", "3", "5")), result);
    }

    @Test
    public void test_groupBy_keyMapperAndCollector() {
        Map<Integer, Long> result = Stream.of(1, 2, 3, 4, 5).groupByToEntry(i -> i % 2, Collectors.counting()).toMap();
        assertEquals(Map.of(0, 2L, 1, 3L), result);
    }

    @Test
    public void test_partitionBy_predicate() {
        Map<Boolean, List<Integer>> result = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(i -> i % 2 == 0).toMap();
        assertEquals(Map.of(true, Arrays.asList(2, 4), false, Arrays.asList(1, 3, 5)), result);
    }

    @Test
    public void test_partitionBy_predicateAndCollector() {
        Map<Boolean, Long> result = Stream.of(1, 2, 3, 4, 5).partitionByToEntry(i -> i % 2 == 0, Collectors.counting()).toMap();
        assertEquals(Map.of(true, 2L, false, 3L), result);
    }

    @Test
    public void test_toMap_keyAndValueMappers() {
        Map<Integer, String> result = Stream.of(1, 2, 3).toMap(i -> i, String::valueOf);
        assertEquals(Map.of(1, "1", 2, "2", 3, "3"), result);
    }

    @Test
    public void test_toMultimap() {
        ListMultimap<Integer, Integer> result = Stream.of(1, 2, 3, 4).toMultimap(i -> i % 2);
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
    }

    @Test
    public void test_sumInt() {
        long result = Stream.of("1", "2", "3").sumInt(Integer::parseInt);
        assertEquals(6L, result);
    }

    @Test
    public void test_averageInt() {
        OptionalDouble result = Stream.of("1", "2", "3").averageInt(Integer::parseInt);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    @Test
    public void test_minAll() {
        List<Integer> result = Stream.of(3, 1, 2, 1, 3).minAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1, 1), result);
    }

    @Test
    public void test_maxAll() {
        List<Integer> result = Stream.of(1, 3, 2, 3, 1).maxAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(3, 3), result);
    }

    @Test
    public void test_findAny() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).findAny(i -> i > 2);
        assertTrue(result.isPresent());
        assertTrue(result.get() > 2);
    }

    @Test
    public void test_containsAll_array() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(2, 3));
        assertFalse(Stream.of(1, 2, 3, 4).containsAll(2, 5));
    }

    @Test
    public void test_containsAll_collection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 3)));
        assertFalse(Stream.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void test_containsAny_array() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAny(5, 3));
        assertFalse(Stream.of(1, 2, 3, 4).containsAny(5, 6));
    }

    @Test
    public void test_containsAny_collection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsAny(Arrays.asList(5, 3)));
        assertFalse(Stream.of(1, 2, 3, 4).containsAny(Arrays.asList(5, 6)));
    }

    @Test
    public void test_containsNone_array() {
        assertTrue(Stream.of(1, 2, 3, 4).containsNone(5, 6));
        assertFalse(Stream.of(1, 2, 3, 4).containsNone(5, 3));
    }

    @Test
    public void test_containsNone_collection() {
        assertTrue(Stream.of(1, 2, 3, 4).containsNone(Arrays.asList(5, 6)));
        assertFalse(Stream.of(1, 2, 3, 4).containsNone(Arrays.asList(5, 3)));
    }

    @Test
    public void test_first() {
        Optional<Integer> result = Stream.of(1, 2, 3).first();
        assertEquals(Optional.of(1), result);
        assertTrue(Stream.of(Collections.emptyList()).first().isEmpty());
    }

    @Test
    public void test_last() {
        Optional<Integer> result = Stream.of(1, 2, 3).last();
        assertEquals(Optional.of(3), result);
        assertTrue(Stream.of(Collections.emptyList()).last().isEmpty());
    }

    @Test
    public void test_elementAt() {
        Optional<Integer> result = Stream.of(1, 2, 3).elementAt(1);
        assertEquals(Optional.of(2), result);
        assertTrue(Stream.of(1, 2, 3).elementAt(3).isEmpty());
    }

    @Test
    public void test_onlyOne() {
        Optional<Integer> result = Stream.of(1).onlyOne();
        assertEquals(Optional.of(1), result);
        assertThrows(Exception.class, () -> Stream.of(1, 2).onlyOne());
    }

    @Test
    public void test_skipNulls() {
        List<Integer> source = Arrays.asList(1, null, 2, null, 3);
        List<Integer> result = Stream.of(source).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_skipRange() {
        List<Integer> result = Stream.of(0, 1, 2, 3, 4, 5).skipRange(2, 4).toList();
        assertEquals(Arrays.asList(0, 1, 4, 5), result);
    }

    @Test
    public void test_skip_withAction() {
        List<Integer> skipped = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void test_intersection() {
        List<Integer> result = Stream.of(1, 2, 2, 3).intersection(Arrays.asList(2, 2, 4)).toList();
        assertEquals(Arrays.asList(2, 2), result);
    }

    @Test
    public void test_intersection_withMapper() {
        List<String> result = Stream.of("a", "b", "c").intersection(s -> s.toUpperCase(), Arrays.asList("A", "C", "D")).toList();
        assertEquals(Arrays.asList("a", "c"), result);
    }

    @Test
    public void test_difference() {
        List<Integer> result = Stream.of(1, 2, 2, 3, 4).difference(Arrays.asList(2, 3, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 4), result);
    }

    @Test
    public void test_difference_withMapper() {
        List<String> result = Stream.of("a", "b", "c").difference(s -> s.toUpperCase(), Arrays.asList("B")).toList();
        assertEquals(Arrays.asList("a", "c"), result);
    }

    @Test
    public void test_symmetricDifference() {
        List<Integer> result = Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toSet().stream().sorted().toList();
        assertEquals(Arrays.asList(1, 2, 4, 5), result);
    }

    @Test
    public void test_reversed() {
        List<Integer> result = Stream.of(1, 2, 3).reversed().toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void test_rotated() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).rotated(2).toList();
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result);

        result = Stream.of(1, 2, 3, 4, 5).rotated(-2).toList();
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), result);
    }

    @Test
    public void test_shuffled() {
        List<Integer> source = List.of(1, 2, 3, 4, 5);
        List<Integer> result = Stream.of(source).shuffled(new Random(1)).toList();
        assertFalse(source.equals(result));
        assertEquals(source.size(), result.size());
        assertTrue(result.containsAll(source));
    }

    @Test
    public void test_sorted() {
        List<Integer> result = Stream.of(3, 1, 2).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_sorted_withComparator() {
        List<Integer> result = Stream.of(1, 2, 3).sorted(Comparator.reverseOrder()).toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void test_distinct() {
        List<Integer> result = Stream.of(1, 2, 1, 3, 2).distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_distinctBy() {
        List<String> result = Stream.of("apple", "banana", "apricot", "blueberry").distinctBy(s -> s.charAt(0)).toList();
        assertEquals(Arrays.asList("apple", "banana"), result);
    }

    @Test
    public void test_top() {
        List<Integer> result = Stream.of(3, 1, 4, 1, 5, 9, 2, 6).top(3).toList();
        assertEquals(Arrays.asList(5, 9, 6), result);
    }

    @Test
    public void test_percentiles() {
        Optional<Map<Percentage, Integer>> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles();
        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertEquals(43, result.get().size());
    }

    @Test
    public void test_combinations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(List.of(List.of(1, 2), List.of(1, 3), List.of(2, 3)), result);
    }

    @Test
    public void test_permutations() {
        long count = Stream.of(1, 2, 3).permutations().count();
        assertEquals(6, count);
    }

    @Test
    public void test_orderedPermutations() {
        List<List<Integer>> result = Stream.of(1, 3, 2).orderedPermutations().toList();
        List<List<Integer>> expected = List.of(List.of(1, 2, 3), List.of(1, 3, 2), List.of(2, 1, 3), List.of(2, 3, 1), List.of(3, 1, 2), List.of(3, 2, 1));
        assertEquals(expected, result);
    }

    @Test
    public void test_cartesianProduct() {
        List<List<Integer>> result = Stream.of(1, 2).cartesianProduct(List.of(List.of(3, 4))).toList();
        assertEquals(List.of(List.of(1, 3), List.of(1, 4), List.of(2, 3), List.of(2, 4)), result);
    }

    @Test
    public void test_join() {
        String result = Stream.of("a", "b", "c").join(", ", "[", "]");
        assertEquals("[a, b, c]", result);
    }

    @Test
    public void test_hasDuplicates() {
        assertTrue(Stream.of(1, 2, 1).hasDuplicates());
        assertFalse(Stream.of(1, 2, 3).hasDuplicates());
    }

    @Test
    public void test_collectThenApply() {
        int result = Stream.of(1, 2, 3).collectThenApply(Collectors.toList(), List::size);
        assertEquals(3, result);
    }

    @Test
    public void test_collectThenAccept() {
        AtomicInteger count = new AtomicInteger();
        Stream.of(1, 2, 3).collectThenAccept(Collectors.toList(), list -> count.set(list.size()));
        assertEquals(3, count.get());
    }

    @Test
    public void test_indexed() {
        List<Indexed<String>> result = Stream.of("a", "b").indexed().toList();
        assertEquals(0, result.get(0).index());
        assertEquals("a", result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals("b", result.get(1).value());
    }

    @Test
    public void test_cycled() {
        List<Integer> result = Stream.of(1, 2).cycled().limit(5).toList();
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), result);
    }

    @Test
    public void test_cycled_withRounds() {
        List<Integer> result = Stream.of(1, 2).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 1, 2), result);
    }

    @Test
    public void test_rollup() {
        List<List<Integer>> result = Stream.of(1, 2, 3).rollup().toList();
        assertEquals(List.of(List.of(), List.of(1), List.of(1, 2), List.of(1, 2, 3)), result);
    }

    @Test
    public void test_append_stream() {
        List<Integer> result = Stream.of(1, 2).append(Stream.of(3, 4)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_prepend_stream() {
        List<Integer> result = Stream.of(3, 4).prepend(Stream.of(1, 2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_crossJoin() {
        List<Pair<Integer, String>> result = Stream.of(1, 2).crossJoin(List.of("a", "b")).toList();
        assertEquals(List.of(Pair.of(1, "a"), Pair.of(1, "b"), Pair.of(2, "a"), Pair.of(2, "b")), result);
    }

    @Test
    public void test_innerJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"), Pair.of(2, "B"))
                .innerJoin(List.of(Pair.of(1, "X"), Pair.of(3, "Y")), p -> p.left(), p -> p.left(), (p1, p2) -> Pair.of(p1.left(), p2.right()))
                .toList();
        assertEquals(List.of(Pair.of(1, "X")), result);
    }

    @Test
    public void test_leftJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"), Pair.of(2, "B"))
                .leftJoin(List.of(Pair.of(1, "X"), Pair.of(3, "Y")), p -> p.left(), p -> p.left(),
                        (p1, p2) -> Pair.of(p1.left(), p2 == null ? null : p2.right()))
                .toList();
        assertEquals(List.of(Pair.of(1, "X"), Pair.of(2, null)), result);
    }

    @Test
    public void test_rightJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"))
                .rightJoin(List.of(Pair.of(1, "X"), Pair.of(2, "Y")), p -> p.left(), p -> p.left(),
                        (p1, p2) -> Pair.of(p2.left(), p1 == null ? null : p1.right()))
                .toList();
        assertEquals(List.of(Pair.of(1, "A"), Pair.of(2, null)), result);
    }

    @Test
    public void test_fullJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"), Pair.of(2, "B"))
                .fullJoin(List.of(Pair.of(1, "X"), Pair.of(3, "Y")), p -> p.left(), p -> p.left(), (p1, p2) -> {
                    if (p1 != null && p2 != null)
                        return Pair.of(p1.left(), p1.right() + p2.right());
                    if (p1 != null)
                        return Pair.of(p1.left(), p1.right());
                    return Pair.of(p2.left(), p2.right());
                })
                .sorted(Comparator.comparing(p -> p.left()))
                .toList();

        assertEquals(List.of(Pair.of(1, "AX"), Pair.of(2, "B"), Pair.of(3, "Y")), result);
    }

    @Test
    public void test_groupJoin() {
        List<Pair<Integer, Long>> result = Stream.of(Pair.of(1, "Group1"), Pair.of(2, "Group2"))
                .groupJoin(List.of(Pair.of(1, 10), Pair.of(1, 20), Pair.of(2, 30)), p -> p.left(), p -> p.left(),
                        Collectors.summingInt(p -> (Integer) p.right()), (p1, sum) -> Pair.of(p1.left(), sum.longValue()))
                .toList();
        assertEquals(List.of(Pair.of(1, 30L), Pair.of(2, 30L)), result);
    }

    @Test
    public void test_findFirst() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).first();
        assertEquals(Optional.of(1), result);
    }

    @Test
    public void test_findFirst_withPredicate() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).findFirst(i -> i > 2);
        assertEquals(Optional.of(3), result);
    }

    @Test
    public void test_findLast() {
        Optional<Integer> result = Stream.of(1, 2, 3, 4).findLast(i -> i < 4);
        assertEquals(Optional.of(3), result);
    }

    @Test
    public void test_min() {
        Optional<Integer> result = Stream.of(3, 1, 2).min(Comparator.naturalOrder());
        assertEquals(Optional.of(1), result);
    }

    @Test
    public void test_max() {
        Optional<Integer> result = Stream.of(3, 1, 2).max(Comparator.naturalOrder());
        assertEquals(Optional.of(3), result);
    }

    @Test
    public void test_minBy() {
        Optional<String> result = Stream.of("apple", "banana", "kiwi").minBy(String::length);
        assertEquals(Optional.of("kiwi"), result);
    }

    @Test
    public void test_maxBy() {
        Optional<String> result = Stream.of("apple", "banana", "kiwi").maxBy(String::length);
        assertEquals(Optional.of("banana"), result);
    }

    @Test
    public void test_sum() {
        long result = Stream.of(1, 2, 3).mapToLong(i -> i).sum();
        assertEquals(6, result);
    }

    @Test
    public void test_average() {
        OptionalDouble result = Stream.of(1, 2, 3).mapToInt(i -> i).average();
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    @Test
    public void test_forEachPair() {
        List<Pair<Integer, Integer>> pairs = new ArrayList<>();
        Stream.of(1, 2, 3, 4).forEachPair((a, b) -> pairs.add(Pair.of(a, b)));
        assertEquals(List.of(Pair.of(1, 2), Pair.of(2, 3), Pair.of(3, 4)), pairs);

        pairs.clear();
        Stream.of(1, 2, 3).forEachPair((a, b) -> pairs.add(Pair.of(a, b)));
        assertEquals(List.of(Pair.of(1, 2), Pair.of(2, 3)), pairs);
    }

    @Test
    public void test_forEachTriple() {
        List<List<Integer>> triples = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).forEachTriple((a, b, c) -> triples.add(List.of(a, b, c)));
        assertEquals(List.of(List.of(1, 2, 3), List.of(2, 3, 4), List.of(3, 4, 5)), triples);
    }

    @Test
    public void test_toArray_generator() {
        Integer[] result = Stream.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void test_toDataset() {
        List<Map<String, Object>> data = List.of(Map.of("id", 1, "name", "A"), Map.of("id", 2, "name", "B"));
        com.landawn.abacus.util.Dataset dataset = Stream.of(data).toDataset();
        assertEquals(2, dataset.size());
        assertTrue(dataset.columnNameList().containsAll(List.of("id", "name")));
    }

    @Test
    public void test_toDataset_withColumnNames() {
        List<List<Object>> data = List.of(List.of(1, "A"), List.of(2, "B"));
        com.landawn.abacus.util.Dataset dataset = Stream.of(data).toDataset(List.of("id", "name"));
        assertEquals(2, dataset.size());
        assertEquals(List.of("id", "name"), dataset.columnNameList());
    }

    @Test
    public void test_anyMatch() {
        assertTrue(Stream.of(1, 2, 3).anyMatch(i -> i == 2));
        assertFalse(Stream.of(1, 2, 3).anyMatch(i -> i == 4));
    }

    @Test
    public void test_allMatch() {
        assertTrue(Stream.of(2, 4, 6).allMatch(i -> i % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).allMatch(i -> i % 2 == 0));
    }

    @Test
    public void test_noneMatch() {
        assertTrue(Stream.of(1, 3, 5).noneMatch(i -> i % 2 == 0));
        assertFalse(Stream.of(1, 2, 3).noneMatch(i -> i % 2 == 0));
    }

    @Test
    public void test_nMatch() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).nMatch(2, 2, i -> i % 2 == 0));
        assertFalse(Stream.of(1, 2, 3, 4, 5).nMatch(3, 3, i -> i % 2 == 0));
    }

    @Test
    public void test_count() {
        assertEquals(3, Stream.of(1, 2, 3).count());
    }

    @Test
    public void test_collect() {
        List<Integer> result = Stream.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_toList() {
        List<Integer> result = Stream.of(1, 2, 3).toList();
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_toSet() {
        Set<Integer> result = Stream.of(1, 2, 1).toSet();
        assertEquals(Set.of(1, 2), result);
    }

    @Test
    public void test_toCollection() {
        ArrayList<Integer> result = Stream.of(1, 2, 3).toCollection(ArrayList::new);
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_joinTo() {
        com.landawn.abacus.util.Joiner joiner = com.landawn.abacus.util.Joiner.with(", ");
        Stream.of(1, 2, 3).joinTo(joiner);
        assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void test_reverseSorted() {
        List<Integer> result = Stream.of(1, 3, 2).reverseSorted().toList();
        assertEquals(List.of(3, 2, 1), result);
    }

    @Test
    public void test_sortedByInt() {
        List<String> result = Stream.of("apple", "kiwi", "banana").sortedByInt(String::length).toList();
        assertEquals(List.of("kiwi", "apple", "banana"), result);
    }

    @Test
    public void test_append_collection() {
        List<Integer> result = Stream.of(1, 2).append(List.of(3, 4)).toList();
        assertEquals(List.of(1, 2, 3, 4), result);
    }

    @Test
    public void test_prepend_collection() {
        List<Integer> result = Stream.of(3, 4).prepend(List.of(1, 2)).toList();
        assertEquals(List.of(1, 2, 3, 4), result);
    }

    @Test
    public void test_mergeWith() {
        Stream<Integer> s1 = Stream.of(1, 3, 5);
        Stream<Integer> s2 = Stream.of(2, 4, 6);
        List<Integer> result = s1
                .mergeWith(s2, (a, b) -> a < b ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND)
                .toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void test_zipWith_collection() {
        Stream<String> s1 = Stream.of("a", "b", "c");
        List<Integer> c2 = List.of(1, 2, 3);
        List<String> result = s1.zipWith(c2, (s, i) -> s + i).toList();
        assertEquals(List.of("a1", "b2", "c3"), result);
    }

    @Test
    public void test_zipWith_stream() {
        Stream<String> s1 = Stream.of("a", "b", "c");
        Stream<Integer> s2 = Stream.of(1, 2, 3);
        List<String> result = s1.zipWith(s2, (s, i) -> s + i).toList();
        assertEquals(List.of("a1", "b2", "c3"), result);
    }

    @Test
    public void test_append_optional() {
        Stream<Integer> s1 = Stream.of(1, 2);
        Optional<Integer> op = Optional.of(3);
        List<Integer> result = s1.append(op).toList();
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_prepend_optional() {
        Stream<Integer> s1 = Stream.of(2, 3);
        Optional<Integer> op = Optional.of(1);
        List<Integer> result = s1.prepend(op).toList();
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_kthLargest() {
        Optional<Integer> result = Stream.of(9, 1, 8, 2, 7, 3, 6, 4, 5).kthLargest(3, Comparator.naturalOrder());
        assertEquals(Optional.of(7), result);
    }

    @Test
    public void test_sumLong() {
        long result = Stream.of(1L, 2L, 3L).sumLong(l -> l);
        assertEquals(6L, result);
    }

    @Test
    public void test_sumDouble() {
        double result = Stream.of(1.1, 2.2, 3.3).sumDouble(d -> d);
        assertEquals(6.6, result, 0.001);
    }

    @Test
    public void test_averageLong() {
        OptionalDouble result = Stream.of(1L, 2L, 3L).averageLong(l -> l);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    @Test
    public void test_averageDouble() {
        OptionalDouble result = Stream.of(1.0, 2.0, 3.0).averageDouble(d -> d);
        assertTrue(result.isPresent());
        assertEquals(2.0, result.getAsDouble());
    }

    @Test
    public void test_toMultiset() {
        Multiset<Integer> result = Stream.of(1, 2, 1, 3, 2, 1).toMultiset();
        assertEquals(3, result.count(1));
        assertEquals(2, result.count(2));
        assertEquals(1, result.count(3));
    }

    @Test
    public void test_reduce() {
        Optional<Integer> result = Stream.of(1, 2, 3).reduce((a, b) -> a + b);
        assertEquals(Optional.of(6), result);
    }
}
