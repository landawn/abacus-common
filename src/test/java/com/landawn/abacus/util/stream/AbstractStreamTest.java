package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.guava.Files;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("new-test")
public class AbstractStreamTest extends TestBase {

    private Stream<Integer> stream;
    private Stream<Object> objectStream;
    private Stream<String> stringStream;

    @TempDir
    Path tempFolder;

    @SafeVarargs
    private final <T> Stream<T> createStream(final T... elements) {
        return Stream.of(elements);
    }

    private <T> Stream<T> createStream(final Iterable<? extends T> iter) {
        return Stream.of(iter);
    }

    @Test
    public void test_joinByRange() {
        Stream<Integer> streamA = Stream.of(1, 2, 5, 6);
        Stream<Integer> streamB = Stream.of(1, 2, 3, 4);
        List<Pair<Integer, List<Integer>>> result = streamA.joinByRange(streamB.iterator(), (a, b) -> b <= a).toList();

        assertEquals(4, result.size());
        assertEquals(Pair.of(1, List.of(1)), result.get(0));
        assertEquals(Pair.of(2, List.of(2)), result.get(1));
        assertEquals(Pair.of(5, List.of(3, 4)), result.get(2));
        assertEquals(Pair.of(6, List.of()), result.get(3));
    }

    @Test
    public void test_joinByRange_withCollector() {
        Stream<String> streamA = Stream.of("a", "b", "c");
        Stream<String> streamB = Stream.of("a1", "a2", "b1", "c1", "c2");

        List<Pair<String, Long>> result = streamA.joinByRange(streamB.iterator(), (a, b) -> b.startsWith(a), Collectors.counting()).toList();

        assertEquals(3, result.size());
        assertEquals(Pair.of("a", 2L), result.get(0));
        assertEquals(Pair.of("b", 1L), result.get(1));
        assertEquals(Pair.of("c", 2L), result.get(2));
    }

    @Test
    public void test_collect_withSupplierAndAccumulator() {
        List<Integer> result = Stream.of(1, 2, 3).collect(ArrayList::new, ArrayList::add);
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_collect_withSupplierAccumulatorAndCombiner() {
        Set<Integer> result = Stream.of(1, 2, 3).parallel().collect(HashSet::new, Set::add, Set::addAll);
        assertEquals(Set.of(1, 2, 3), result);
    }

    @Test
    public void test_toListThenApply() {
        Integer result = Stream.of(1, 2, 3).toListThenApply(List::size);
        assertEquals(3, result);
    }

    @Test
    public void test_toListThenAccept() {
        final List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toListThenAccept(holder::addAll);
        assertEquals(List.of(1, 2, 3), holder);
    }

    @Test
    public void test_toSetThenApply() {
        Integer result = Stream.of(1, 2, 1).toSetThenApply(Set::size);
        assertEquals(2, result);
    }

    @Test
    public void test_toSetThenAccept() {
        final Set<Integer> holder = new HashSet<>();
        Stream.of(1, 2, 1).toSetThenAccept(holder::addAll);
        assertEquals(Set.of(1, 2), holder);
    }

    @Test
    public void test_toCollectionThenApply() {
        Integer result = Stream.of(1, 2, 3).toCollectionThenApply(ArrayList::new, Collection::size);
        assertEquals(3, result);
    }

    @Test
    public void test_toCollectionThenAccept() {
        final List<Integer> holder = new ArrayList<>();
        Stream.of(1, 2, 3).toCollectionThenAccept(ArrayList::new, holder::addAll);
        assertEquals(List.of(1, 2, 3), holder);
    }

    @Test
    public void test_iterator() {
        Iterator<Integer> it = Stream.of(1, 2, 3).iterator();
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void test_persist_to_writer() throws IOException {
        StringWriter writer = new StringWriter();
        long count = Stream.of("line1", "line2").persist(Object::toString, writer);

        assertEquals(2, count);
        String expected = "line1" + IOUtil.LINE_SEPARATOR_UNIX + "line2" + IOUtil.LINE_SEPARATOR_UNIX;
        assertEquals(expected, writer.toString());
    }

    @Test
    public void test_persist_withHeaderAndTail() throws IOException {
        StringWriter writer = new StringWriter();
        long count = Stream.of("data").persist("Header", "Tail", Object::toString, writer);

        assertEquals(1, count);
        String expected = "Header" + IOUtil.LINE_SEPARATOR_UNIX + "data" + IOUtil.LINE_SEPARATOR_UNIX + "Tail" + IOUtil.LINE_SEPARATOR_UNIX;
        assertEquals(expected, writer.toString());
    }

    @Test
    public void test_persistToCsv() throws IOException {
        List<Map<String, String>> data = List.of(Map.of("h1", "a", "h2", "b"), Map.of("h1", "c", "h2", "d"));
        StringWriter writer = new StringWriter();

        long count = Stream.of(data).persistToCsv(List.of("h1", "h2"), writer);
        assertEquals(2, count);
        String expected = "\"h1\",\"h2\"" + IOUtil.LINE_SEPARATOR_UNIX + "\"a\",\"b\"" + IOUtil.LINE_SEPARATOR_UNIX + "\"c\",\"d\"";
        assertEquals(expected, writer.toString().trim());
    }

    @Test
    public void test_persistToJson() throws IOException {
        List<Map<String, String>> data = List.of(Map.of("key", "val1"), Map.of("key", "val2"));
        StringWriter writer = new StringWriter();
        long count = Stream.of(data).persistToJson(writer);
        assertEquals(2, count);
        N.println(writer.toString());
        String result = writer.toString().replaceAll("\\s", "");
        assertEquals("[{\"key\":\"val1\"},{\"key\":\"val2\"}]", result);
    }

    @Test
    public void test_saveEach_withStmtSetter() throws SQLException {
        PreparedStatement stmtMock = mock(PreparedStatement.class);
        final AtomicInteger counter = new AtomicInteger(0);

        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(stmtMock).addBatch();

        Stream.of("a", "b", "c").saveEach(stmtMock, 2, 0, (val, ps) -> ps.setString(1, val)).count();

        verify(stmtMock, times(3)).setString(anyInt(), anyString());
        verify(stmtMock, times(3)).addBatch();
        verify(stmtMock, times(2)).executeBatch();

    }

    @Test
    public void test_saveEach_withConnection() throws SQLException {
        Connection connMock = mock(Connection.class);
        PreparedStatement stmtMock = mock(PreparedStatement.class);
        when(connMock.prepareStatement(anyString())).thenReturn(stmtMock);

        Stream.of("a", "b").saveEach(connMock, "INSERT INTO foo VALUES(?)", (val, ps) -> ps.setString(1, val)).count();

        verify(connMock, times(1)).prepareStatement("INSERT INTO foo VALUES(?)");
        verify(stmtMock, times(2)).setString(anyInt(), anyString());
        verify(stmtMock, times(2)).execute();
        verify(stmtMock, times(1)).close();
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
        List<Integer> result = Stream.of("1,2", "3,4").flatMapArray(s -> s.split(",")).map(i -> Numbers.toInt(i)).toList();
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
    public void test_sliding() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).sliding(3).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)), result);
    }

    @Test
    public void test_sliding_withIncrement() {
        List<List<Integer>> result = Stream.of(1, 2, 3, 4, 5).sliding(3, 2).toList();
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

    //

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
    public void test_containsDuplicates() {
        assertTrue(Stream.of(1, 2, 1).containsDuplicates());
        assertFalse(Stream.of(1, 2, 3).containsDuplicates());
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
        assertTrue(dataset.columnNames().containsAll(List.of("id", "name")));
    }

    @Test
    public void test_toDataset_withColumnNames() {
        List<List<Object>> data = List.of(List.of(1, "A"), List.of(2, "B"));
        com.landawn.abacus.util.Dataset dataset = Stream.of(data).toDataset(List.of("id", "name"));
        assertEquals(2, dataset.size());
        assertEquals(List.of("id", "name"), dataset.columnNames());
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
    public void test_countMatchBetween() {
        assertTrue(Stream.of(1, 2, 3, 4, 5).hasMatchCountBetween(2, 2, i -> i % 2 == 0));
        assertFalse(Stream.of(1, 2, 3, 4, 5).hasMatchCountBetween(3, 3, i -> i % 2 == 0));
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
        Stream<Integer> result = stream.slidingMap((a, b) -> a + (b == null ? 0 : b));
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
        Stream<Integer> result = stream.slidingMap((a, b, c) -> a + (b == null ? 0 : b) + (c == null ? 0 : c));
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
            Stream<Integer> result = stream.flatMapArray(i -> new Integer[] { i, i * 10 });
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.parallel().flatMapArray(i -> new Integer[] { i, i * 10 });
            assertHaveSameElements(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.flatMapArray(i -> new Integer[] { i, i * 10 });
            assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result.toList());
        }
        {
            stream = createStream(1, 2, 3).map(e -> e);
            Stream<Integer> result = stream.parallel().flatMapArray(i -> new Integer[] { i, i * 10 });
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

    //
    //
    //    @Test
    //    public void testReduceUntilWithIdentity() {
    //        stream = createStream(1, 2, 3, 4, 5);
    //        Integer result = stream.reduceUntil(0, Integer::sum, (a, b) -> a + b, sum -> sum > 5);
    //        assertEquals(6, result);
    //    }

    //
    //
    //

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
        assertTrue(stream.containsDuplicates());

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsDuplicates());
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

    @Test
    public void testJoinByRangeWithStream() {
        List<Integer> left = Arrays.asList(1, 5, 10);
        List<Integer> right = Arrays.asList(2, 3, 6, 7, 11);
        Stream<Integer> leftStream = createStream(left);
        Stream<Integer> rightStream = createStream(right);

        List<Pair<Integer, List<Integer>>> result = leftStream.joinByRange(rightStream, (l, r) -> r > l && r < l + 5).toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testJoinByRangeWithUnjoined() {
        List<Integer> left = Arrays.asList(1, 5);
        List<Integer> right = Arrays.asList(2, 3, 10, 11);
        Stream<Integer> stream = createStream(left);

        List<Integer> result = stream
                .joinByRange(right.iterator(), (l, r) -> r > l && r < l + 2, Collectors.toList(), (a, b) -> a, iter -> Stream.of(iter).map(x -> -x))
                .toList();

        assertTrue(result.contains(1));
        assertTrue(result.contains(5));
        assertTrue(result.contains(-10));
        assertTrue(result.contains(-11));
    }

    @Test
    public void testGroupJoinWithDownstream() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "100"), new TestOrder(1, "200"), new TestOrder(2, "300"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, Long>> result = stream.groupJoin(orders, TestPerson::getId, TestOrder::getPersonId, Collectors.counting()).toList();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).right().longValue());
        assertEquals(1L, result.get(1).right().longValue());
    }

    @Test
    public void testIterator() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        ObjIterator<Integer> iter = stream.iterator();
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }

        assertEquals(input, result);
    }

    @Test
    public void testPersistToCSVWriter_withHeaders() throws IOException {
        {
            StringWriter writer = new StringWriter();
            List<TestBean> input = Arrays.asList(new TestBean("John", 25));
            Stream<TestBean> stream = createStream(input);

            long count = stream.persistToCsv(N.toList("name", "age"), writer);
            assertEquals(1, count);

            String csv = writer.toString();
            assertTrue(csv.contains("name"));
            assertTrue(csv.contains("age"));
            assertTrue(csv.contains("John"));
            assertTrue(csv.contains("25"));
        }
        {
            StringWriter writer = new StringWriter();
            List<Map<String, Object>> input = Arrays.asList(Map.of("name", "John", "age", 25));
            Stream<Map<String, Object>> stream = createStream(input);

            long count = stream.persistToCsv(N.toList("name", "age"), writer);
            assertEquals(1, count);

            String csv = writer.toString();
            assertTrue(csv.contains("name"));
            assertTrue(csv.contains("age"));
            assertTrue(csv.contains("John"));
            assertTrue(csv.contains("25"));
        }
        {
            StringWriter writer = new StringWriter();
            List<List<Object>> input = Arrays.asList(N.toList("John", 25));
            Stream<List<Object>> stream = createStream(input);

            long count = stream.persistToCsv(N.toList("name", "age"), writer);
            assertEquals(1, count);

            String csv = writer.toString();
            assertTrue(csv.contains("name"));
            assertTrue(csv.contains("age"));
            assertTrue(csv.contains("John"));
            assertTrue(csv.contains("25"));
        }
        {
            StringWriter writer = new StringWriter();
            Object[] a = N.asArray("John", 25);
            List<Object[]> input = N.asSingletonList(a);
            Stream<Object[]> stream = createStream(input);

            long count = stream.persistToCsv(N.toList("name", "age"), writer);
            assertEquals(1, count);

            String csv = writer.toString();
            assertTrue(csv.contains("name"));
            assertTrue(csv.contains("age"));
            assertTrue(csv.contains("John"));
            assertTrue(csv.contains("25"));
        }
    }

    @Test
    public void testSaveEachWithBatchSize() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        List<TestBean> input = Arrays.asList(new TestBean("A", 1), new TestBean("B", 2), new TestBean("C", 3), new TestBean("D", 4));
        Stream<TestBean> stream = createStream(input);

        List<TestBean> result = stream.saveEach(stmt, 2, 0, (bean, ps) -> {
            ps.setString(1, bean.getName());
            ps.setInt(2, bean.getAge());
        }).toList();

        assertEquals(input, result);
        verify(stmt, times(4)).addBatch();
        verify(stmt, times(2)).executeBatch();
    }

    @Test
    public void testZipWith3Streams() {
        List<String> stream1Data = Arrays.asList("A", "B", "C");
        List<Integer> stream2Data = Arrays.asList(1, 2, 3);
        List<Boolean> stream3Data = Arrays.asList(true, false, true);

        Stream<String> stream = createStream(stream1Data);

        List<String> result = stream.zipWith(stream2Data, stream3Data, (s, i, b) -> s + i + (b ? "!" : "")).toList();

        assertEquals(Arrays.asList("A1!", "B2", "C3!"), result);
    }

    @Test
    public void testZipWithDefaults() {
        List<String> stream1Data = Arrays.asList("A", "B", "C", "D");
        List<Integer> stream2Data = Arrays.asList(1, 2);

        Stream<String> stream = createStream(stream1Data);

        List<String> result = stream.zipWith(stream2Data, "X", 0, (s, i) -> s + i).toList();

        assertEquals(Arrays.asList("A1", "B2", "C0", "D0"), result);
    }

    @Test
    public void testRateLimit() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        long start = System.currentTimeMillis();
        List<Integer> result = stream.rateLimited(rateLimiter).toList();
        long duration = System.currentTimeMillis() - start;

        assertEquals(input, result);
        assertTrue(duration >= 200);
    }

    @Test
    public void testDelayWithDuration() {
        List<Integer> input = Arrays.asList(1, 2);
        Stream<Integer> stream = createStream(input);

        long start = System.currentTimeMillis();
        List<Integer> result = stream.delay(Duration.ofMillis(100)).toList();
        long duration = System.currentTimeMillis() - start;

        assertEquals(input, result);
        assertTrue(duration < 200);
    }

    @Test
    public void testSkipNegative() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> stream.skip(-1, i -> {
        }));
    }

    @Test
    public void testElementAtNegative() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> stream.elementAt(-1));
    }

    @Test
    public void testFilterWithAction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.filter(x -> x % 2 == 0, dropped::add).toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testDropWhileWithAction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.dropWhile(x -> x < 3, dropped::add).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    @Test
    public void testStepWithInvalidArgument() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
    }

    @Test
    public void testSlidingMapBiFunction() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        List<Integer> sums = stream.slidingMap((a, b) -> a + (b == null ? 0 : b)).toList();
        assertEquals(Arrays.asList(3, 5, 7, 9), sums);
    }

    @Test
    public void testCollapse_2() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.collapse((a, b, c) -> b == c, (b, c) -> b + c).toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(9, result.get(2));
        assertEquals(4, result.get(3));
    }

    @Test
    public void testCollapse_3() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.collapse((a, b, c) -> b == c, 0, (b, c) -> b + c).toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(9, result.get(2));
        assertEquals(4, result.get(3));
    }

    @Test
    public void testCollapse_4() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.collapse((a, b, c) -> b == c, Collectors.summingInt(e -> e)).toList();
        assertEquals(4, result.size());
        assertEquals(1, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(9, result.get(2));
        assertEquals(4, result.get(3));
    }

    @Test
    public void testSplitAt_advice() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);

        {
            assertEquals(input, createStream(input).splitAt(4).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(4).skip(1).flatMap(s -> s).toList());
            assertTrue(createStream(input).splitAt(4).skip(2).flatMap(s -> s).toList().isEmpty());

            assertEquals(input, createStream(input).splitAt(x -> x == 5).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(x -> x == 5).skip(1).flatMap(s -> s).toList());
            assertTrue(createStream(input).splitAt(x -> x == 5).skip(2).flatMap(s -> s).toList().isEmpty());
        }

        {
            assertEquals(input, createStream(input).splitAt(4, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(4, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(createStream(input).splitAt(4, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());

            assertEquals(input, createStream(input).splitAt(x -> x == 5, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), createStream(input).splitAt(x -> x == 5, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(createStream(input).splitAt(x -> x == 5, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());
        }
    }

    @Test
    public void testSplitAt_advice_array() {
        Integer[] inputArray = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        List<Integer> input = Arrays.asList(inputArray);

        {
            assertEquals(input, Stream.of(inputArray).splitAt(4).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(4).skip(1).flatMap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(4).skip(2).flatMap(s -> s).toList().isEmpty());

            assertEquals(input, Stream.of(inputArray).splitAt(x -> x == 5).skip(0).flatMap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(x -> x == 5).skip(1).flatMap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(x -> x == 5).skip(2).flatMap(s -> s).toList().isEmpty());
        }

        {
            assertEquals(input, Stream.of(inputArray).splitAt(4, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(4, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(4, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());

            assertEquals(input, Stream.of(inputArray).splitAt(x -> x == 5, Collectors.toList()).skip(0).flatmap(s -> s).toList());
            assertEquals(Arrays.asList(5, 6, 7, 8, 9), Stream.of(inputArray).splitAt(x -> x == 5, Collectors.toList()).skip(1).flatmap(s -> s).toList());
            assertTrue(Stream.of(inputArray).splitAt(x -> x == 5, Collectors.toList()).skip(2).flatmap(s -> s).toList().isEmpty());
        }
    }

    //
    //    @Test
    //    public void testReduceUntil_2() {
    //        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3, 4);
    //        Stream<Integer> stream = createStream(input);
    //
    //        Integer result = stream.reduceUntil(0, (a, b) -> a + b, (a, b) -> a + b, sum -> sum >= 10);
    //        assertEquals(11, result);
    //    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testRotated_2() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        Stream<Integer> stream1 = createStream(input);
        assertEquals(Arrays.asList(1, 2, 3), stream1.rotated(2).skip(2).toList());

        Stream<Integer> stream2 = createStream(input);
        assertArrayEquals(Arrays.asList(3, 4, 5, 1, 2).toArray(Integer[]::new), stream2.rotated(-2).toArray(Integer[]::new));
    }

    @Test
    public void testReverse_2() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);

        Stream<Integer> stream1 = createStream(input);
        assertEquals(Arrays.asList(3, 2, 1), stream1.reversed().skip(2).toList());

        Stream<Integer> stream2 = createStream(input);
        assertArrayEquals(Arrays.asList(3, 2, 1).toArray(Integer[]::new), stream2.reversed().skip(2).toArray(Integer[]::new));
    }

    @Test
    public void testAppend() {
        List<Integer> input1 = Arrays.asList(1, 2, 3);
        List<Integer> input2 = Arrays.asList(4, 5);
        Stream<Integer> stream1 = createStream(input1);
        Stream<Integer> stream2 = createStream(input2);

        List<Integer> result = stream1.append(stream2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPrepend() {
        List<Integer> input1 = Arrays.asList(3, 4, 5);
        List<Integer> input2 = Arrays.asList(1, 2);
        Stream<Integer> stream1 = createStream(input1);
        Stream<Integer> stream2 = createStream(input2);

        List<Integer> result = stream1.prepend(stream2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testSaveEach() throws IOException {
        File file = java.nio.file.Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        stream.saveEach(file).toList();

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }

    @Test
    public void testSaveEach_2() throws IOException {
        File file = java.nio.file.Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        try (FileWriter writer = new FileWriter(file)) {
            stream.saveEach((e, w) -> IOUtil.write(e, w), writer).toList();
        }

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }

    @Test
    public void testPersist() throws IOException {
        File file = java.nio.file.Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        long count = stream.persist(file);
        assertEquals(3, count);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }

    @Test
    public void testPersistToCSV() throws IOException {
        File file = java.nio.file.Files.createTempFile(tempFolder, null, null).toFile();
        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persistToCsv(file);
        assertEquals(2, count);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("name"));
        assertTrue(lines.get(0).contains("age"));
    }

    @Test
    public void testPersistToJSON() throws IOException {
        File file = java.nio.file.Files.createTempFile(tempFolder, null, null).toFile();
        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persistToJson(file);
        assertEquals(2, count);

        String json = new String(IOUtil.readAllBytes(file));
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("John"));
        assertTrue(json.contains("Jane"));
    }

    @Test
    public void testInnerJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"), new TestPerson(3, "Bob"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(2, "Order2"), new TestOrder(1, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.innerJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testLeftJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.leftJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(Pair.of(new TestPerson(1, "John"), null), result.get(0));
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(1));
    }

    @Test
    public void testRightJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.rightJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(0));
        assertEquals(Pair.of(null, new TestOrder(3, "Order3")), result.get(1));
    }

    @Test
    public void testfullJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.fullJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(3, result.size());
        assertEquals(Pair.of(new TestPerson(1, "John"), null), result.get(0));
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(1));
        assertEquals(Pair.of(null, new TestOrder(3, "Order3")), result.get(2));
    }

    @Test
    public void testGroupJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, List<TestOrder>>> result = stream.groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).right().size());
        assertEquals(1, result.get(1).right().size());
    }

    @Test
    public void testGroupJoin_3() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, (a, b) -> a, Pair::of)
                .toList();

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).left().getName());
        assertEquals("Order1", result.get(0).right().getOrderName());
        assertEquals("Jane", result.get(1).left().getName());
        assertEquals("Order3", result.get(1).right().getOrderName());

    }

    @Test
    public void testGroupJoin_4() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(1, "Order2"), new TestOrder(2, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, List<TestOrder>>> result = stream
                .groupJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Collectors.toList(), Pair::of)
                .toList();

        assertEquals(2, result.size());
        assertEquals(2, result.get(0).right().size());
        assertEquals(1, result.get(1).right().size());
    }

    @Test
    public void testJoinByRange_2() {
        List<Integer> left = Arrays.asList(1, 5, 10, 15);
        List<Integer> right = Arrays.asList(2, 3, 6, 7, 11, 12);
        Stream<Integer> stream = createStream(left);

        List<Pair<Integer, List<Integer>>> result = stream.joinByRange(right.iterator(), (l, r) -> r > l && r < l + 5).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(2, 3), result.get(0).right());
        assertEquals(Arrays.asList(6, 7), result.get(1).right());
        assertEquals(Arrays.asList(11, 12), result.get(2).right());
        assertEquals(Collections.emptyList(), result.get(3).right());
    }

    // ==================== Missing test methods below ====================

    @Test
    public void testCollapseTriPredicateWithMerge() {
        Stream<Integer> stream = createStream(1, 1, 2, 2, 2, 3, 3);
        List<Integer> result = stream.collapse((prev, cur, next) -> prev.equals(cur), Integer::sum).toList();
        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    public void testDebounce() {
        Stream<Integer> stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = stream.debounce(10, Duration.ofMillis(100)).toList();
        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    public void testSplitAt_byPosition() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Stream<Integer>> result = stream.splitAt(3).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0).toList());
        assertEquals(Arrays.asList(4, 5), result.get(1).toList());
    }

    @Test
    public void testSplitAt_byPosition_zero() {
        stream = createStream(1, 2, 3);
        List<Stream<Integer>> result = stream.splitAt(0).toList();
        assertEquals(2, result.size());
        assertEquals(Collections.emptyList(), result.get(0).toList());
        assertEquals(Arrays.asList(1, 2, 3), result.get(1).toList());
    }

    @Test
    public void testReverseSorted_withComparator() {
        stream = createStream(3, 1, 2, 5, 4);
        List<Integer> result = stream.reverseSorted(Comparator.naturalOrder()).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testSkipRange_fullRange() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = stream.skipRange(0, 5).toList();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testCombinations_withLength() {
        stream = createStream(1, 2, 3);
        List<List<Integer>> result = stream.combinations(2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testCombinations_withLengthAndRepeat() {
        stream = createStream(1, 2);
        List<List<Integer>> result = stream.combinations(2, true).toList();
        assertTrue(result.size() > 0);
    }

    @Test
    public void testOrderedPermutations_withComparator() {
        stream = createStream(3, 1, 2);
        List<List<Integer>> result = stream.orderedPermutations(Comparator.naturalOrder()).toList();
        assertTrue(result.size() > 0);
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
    }

    @Test
    public void testOnFirst_action() {
        stream = createStream(1, 2, 3);
        Holder<Integer> first = Holder.of(0);
        List<Integer> result = stream.onFirst(first::setValue).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(1, first.value().intValue());
    }

    @Test
    public void testOnLast_action() {
        stream = createStream(1, 2, 3);
        Holder<Integer> last = Holder.of(0);
        List<Integer> result = stream.onLast(last::setValue).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(3, last.value().intValue());
    }

    @Test
    public void testBuffered_withSize() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = stream.buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPersist_toWriter() throws IOException {
        StringWriter sw = new StringWriter();
        stream = createStream(1, 2, 3);
        long count = stream.persist(Object::toString, sw);
        assertEquals(3, count);
        assertTrue(sw.toString().contains("1"));
        assertTrue(sw.toString().contains("3"));
    }

    @Test
    public void testPersist_toOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stream = createStream(1, 2, 3);
        long count = stream.persist(Object::toString, baos);
        assertEquals(3, count);
        String output = baos.toString();
        assertTrue(output.contains("1"));
        assertTrue(output.contains("3"));
    }

    @Test
    public void testSumLong_method() {
        stream = createStream(1, 2, 3, 4, 5);
        long result = stream.sumLong(i -> (long) i);
        assertEquals(15L, result);
    }

    @Test
    public void testSumDouble_method() {
        stream = createStream(1, 2, 3, 4, 5);
        double result = stream.sumDouble(i -> (double) i);
        assertEquals(15.0, result, 0.001);
    }

    @Test
    public void testAverageInt_method() {
        stream = createStream(1, 2, 3, 4, 5);
        OptionalDouble result = stream.averageInt(i -> i);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageLong_method() {
        stream = createStream(1, 2, 3, 4, 5);
        OptionalDouble result = stream.averageLong(i -> (long) i);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageDouble_method() {
        stream = createStream(1, 2, 3, 4, 5);
        OptionalDouble result = stream.averageDouble(i -> (double) i);
        assertTrue(result.isPresent());
        assertEquals(3.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testContainsAll_empty() {
        stream = createStream(1, 2, 3);
        assertTrue(stream.containsAll(Collections.emptyList()));
    }

    @Test
    public void testContainsNone_false() {
        stream = createStream(1, 2, 3);
        assertFalse(stream.containsNone(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testOnlyOne_tooMany() {
        stream = createStream(1, 2, 3);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testIterator_method() {
        stream = createStream(1, 2, 3);
        ObjIterator<Integer> iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.next().intValue());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.next().intValue());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.next().intValue());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testCollapseTriPredicate_list() {
        stream = createStream(1, 2, 3, 10, 11, 12);
        List<List<Integer>> result = stream.collapse((first, prev, curr) -> curr - first <= 2).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(10, 11, 12), result.get(1));
    }

    @Test
    public void testCollapseTriPredicate_collector() {
        stream = createStream(1, 2, 3, 10, 11, 12);
        List<Integer> result = stream.collapse((first, prev, curr) -> curr - first <= 2, Integer::sum).toList();
        assertEquals(2, result.size());
        assertEquals(6, result.get(0).intValue());
        assertEquals(33, result.get(1).intValue());
    }

    @Test
    public void testScan_initAndAccumulatorAndFlag_true_empty() {
        stream = createStream();
        List<Integer> result = stream.scan(10, true, Integer::sum).toList();
        assertEquals(Arrays.asList(10), result);
    }

    @Test
    public void testSplit_bySize() {
        stream = createStream(1, 2, 3, 4, 5);
        List<List<Integer>> result = stream.split(2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
        assertEquals(Arrays.asList(5), result.get(2));
    }

    @Test
    public void testSliding_withWindowAndIncrement() {
        stream = createStream(1, 2, 3, 4, 5);
        List<List<Integer>> result = stream.sliding(3, 2).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
    }

    @Test
    public void testIntersperse_method() {
        stream = createStream(1, 2, 3);
        List<Integer> result = stream.intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestBean {
        private String name;
        private int age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestPerson {
        private int id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestOrder {
        private int personId;
        private String orderName;
    }

    @Test
    public void testMinAll_EmptyStream() {
        stream = Stream.empty();
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMinAll_SingleElement() {
        stream = Stream.of(1);
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testMinAll_ParallelDuplicateMinimums() {
        stream = Stream.of(4, 1, 3, 1, 2).parallel();
        List<Integer> result = stream.minAll(Comparator.naturalOrder());
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(it -> it == 1));
    }

    @Test
    public void testJoinByRange_Stream_Parallel() {
        Stream<Integer> streamA = Stream.of(1, 2, 3).parallel();
        Stream<Integer> streamB = Stream.of(4, 5, 6).parallel();
        List<Pair<Integer, List<Integer>>> result = streamA.joinByRange(streamB, (a, b) -> b > a).toList();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testContainsAny_EmptyInput() {
        stream = Stream.of(1, 2, 3);
        assertFalse(stream.containsAny(Collections.emptyList()));
    }

    @Test
    public void testContainsAll_EmptyInput() {
        stream = Stream.of(1, 2, 3);
        assertTrue(stream.containsAll(Collections.emptyList()));
    }

    @Test
    public void testPersistToCsv_EmptyHeaders() throws IOException {
        stream = Stream.of(1, 2, 3);
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> stream.persistToCsv(Collections.emptyList(), writer));
    }

    @Test
    public void testPersistToCsv_ClosedStream() throws IOException {
        StringWriter writer = new StringWriter();
        Stream<TestBean> beanStream = createStream(new TestBean("John", 25));
        beanStream.close();
        assertThrows(IllegalStateException.class, () -> beanStream.persistToCsv(writer));
    }

    @Test
    public void testJoinByRangeWithUnjoined_Parallel() {
        Stream<Integer> left = Stream.of(1, 2, 3).parallel();
        List<Integer> rightList = Arrays.asList(4, 5, 6);
        List<Integer> result = left.joinByRange(rightList.iterator(), (l, r) -> false, Collectors.toList(), (a, b) -> a, iter -> Stream.of(iter).map(x -> -x))
                .toList();
        assertEquals(Arrays.asList(1, 2, 3, -4, -5, -6), result);
    }

    @Test
    public void testJoinByRangeWithCollector_StreamCloseHandler() {
        AtomicInteger closeCount = new AtomicInteger();
        Stream<Integer> left = Stream.of(1, 4);
        Stream<Integer> right = Stream.of(2, 3, 5).onClose(closeCount::incrementAndGet);

        List<Pair<Integer, Long>> result;

        try (Stream<Pair<Integer, Long>> joined = left.joinByRange(right, (l, r) -> r > l && r < l + 3, Collectors.counting())) {
            result = joined.toList();
        }

        assertEquals(1, closeCount.get());
        assertEquals(2L, result.get(0).right().longValue());
        assertEquals(1L, result.get(1).right().longValue());
    }

}
