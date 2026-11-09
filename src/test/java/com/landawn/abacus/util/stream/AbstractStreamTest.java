package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;

/** AbstractStream general contracts. */
public class AbstractStreamTest extends AbstractStreamTestSupport {

    @Test
    public void testHasDuplicates() {
        stream = createStream(1, 2, 3, 2, 4);
        assertTrue(stream.containsDuplicates());

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsDuplicates());
    }

    // iterator

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
    public void testIterator_HasNextAndNext() {
        Stream<Integer> s = Stream.of(10, 20, 30);
        java.util.Iterator<Integer> iter = s.iterator();
        assertTrue(iter.hasNext());
        assertEquals(10, iter.next().intValue());
        assertTrue(iter.hasNext());
        assertEquals(20, iter.next().intValue());
        assertEquals(30, iter.next().intValue());
        assertFalse(iter.hasNext());
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

    // orderedPermutations

    @Test
    public void test_orderedPermutations() {
        List<List<Integer>> result = Stream.of(1, 3, 2).orderedPermutations().toList();
        List<List<Integer>> expected = List.of(List.of(1, 2, 3), List.of(1, 3, 2), List.of(2, 1, 3), List.of(2, 3, 1), List.of(3, 1, 2), List.of(3, 2, 1));
        assertEquals(expected, result);
    }

    @Test
    public void testOrderedPermutations() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.orderedPermutations();
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    @Test
    public void testOrderedPermutations_withComparator() {
        stream = createStream(3, 1, 2);
        List<List<Integer>> result = stream.orderedPermutations(Comparator.naturalOrder()).toList();
        assertTrue(result.size() > 0);
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
    }

    @Test
    public void testOrderedPermutationsWithComparator() {
        stream = createStream(3, 2, 1);
        Stream<List<Integer>> result = stream.orderedPermutations(Comparator.naturalOrder());
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    @Test
    public void test_pairWith() {
        List<Pair<Integer, String>> result = Stream.of(1, 2, 3).pairWith(String::valueOf).toList();
        assertEquals(Arrays.asList(Pair.of(1, "1"), Pair.of(2, "2"), Pair.of(3, "3")), result);
    }

    @Test
    public void testPairWith() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> paired = stream.pairWith(i -> "val" + i);
        List<Pair<Integer, String>> result = paired.toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(1, "val1"), result.get(0));
    }

    @TempDir
    Path tempDir;

    public static class Row {
        private int id;
        private String name;

        public Row() {
        }

        public Row(final int id, final String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    private static <T> ObjIteratorEx<T> iterEx(final Stream<T> s) {
        return (ObjIteratorEx<T>) s.iterator();
    }

    private static List<Integer> range(final int n) {
        final List<Integer> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        return list;
    }

    @Test
    public void testFlattMapToEntryNullMapperResult() {
        final Map<Integer, String> m = Stream.of(1, 2, 3).flattMapToEntry(i -> i == 2 ? null : EntryStream.of(i, "v" + i)).toMap();
        assertEquals(2, m.size());
        assertEquals("v1", m.get(1));
        assertEquals("v3", m.get(3));
    }

    @Test
    public void testMapMultiLargeExpansionThenSmall() {
        final List<Integer> result = Stream.of(20000, 1, 0, 2).<Integer> mapMulti((n, consumer) -> {
            for (int i = 0; i < n; i++) {
                consumer.accept(i);
            }
        }).toList();
        assertEquals(20000 + 1 + 2, result.size());
        assertEquals(19999, result.get(19999));
        assertEquals(0, result.get(20000));
        assertEquals(1, result.get(result.size() - 1));
    }

    @Test
    public void testMapMultiPrimitiveEmptyOutputs() {
        assertEquals(Arrays.asList(1, 3, 3, 3), Stream.of(1, 0, 0, 3).mapMultiToInt((n, c) -> {
            for (int i = 0; i < n; i++) {
                c.accept(n);
            }
        }).boxed().toList());
        assertEquals(Arrays.asList(2L, 2L), Stream.of(0, 2, 0).mapMultiToLong((n, c) -> {
            for (int i = 0; i < n; i++) {
                c.accept(n);
            }
        }).boxed().toList());
        assertEquals(Arrays.asList(1.0), Stream.of(0, 0, 1).mapMultiToDouble((n, c) -> {
            for (int i = 0; i < n; i++) {
                c.accept(n);
            }
        }).boxed().toList());
    }

    @Test
    public void testSplitAtPositionAdvance() {
        ObjIteratorEx<Stream<Integer>> it = iterEx(Stream.of(1, 2, 3).map(i -> i).splitAt(5));
        assertEquals(Arrays.asList(1, 2, 3), it.next().toList());
        it.advance(1);
        assertFalse(it.hasNext());

        it = iterEx(Stream.of(1, 2, 3).map(i -> i).splitAt(1));
        it.advance(1);
        assertTrue(it.hasNext());
        assertEquals(Arrays.asList(2, 3), it.next().toList());
    }

    @Test
    public void testSplitAtPredicateInnerAndAdvance() {
        ObjIteratorEx<Stream<Integer>> it = iterEx(Stream.of(1, 2, 3, 4).map(i -> i).splitAt(i -> i == 3));
        assertEquals(Arrays.asList(1, 2), it.next().toList());
        final ObjIteratorEx<Integer> second = iterEx(it.next());
        assertEquals(3, second.next());
        assertEquals(4, second.next());
        assertThrows(NoSuchElementException.class, second::next);

        it = iterEx(Stream.of(1, 2, 3).map(i -> i).splitAt(i -> i == 9));
        it.advance(1);
        assertTrue(it.hasNext());
        assertEquals(0, it.next().count());

        it = iterEx(Stream.of(1, 2, 3).map(i -> i).splitAt(i -> i == 2));
        it.advance(2);
        assertFalse(it.hasNext());

        it = iterEx(Stream.of(1, 2, 3).map(i -> i).splitAt(i -> i == 2));
        it.next();
        it.advance(1);
        assertFalse(it.hasNext());
    }

    @Test
    public void testIntersperseNextExhausted() {
        final ObjIteratorEx<Integer> it = iterEx(Stream.of(1).map(i -> i).intersperse(0));
        assertEquals(1, it.next());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testForEachUntilParallel() {
        final List<Integer> seen = new ArrayList<>();
        Stream.of(1, 2, 3, 4, 5).parallel(2).forEachUntil((Integer t, MutableBoolean flag) -> {
            synchronized (seen) {
                seen.add(t);
            }
        });
        assertEquals(5, seen.size());
    }

    @Test
    public void testForEachUntilActionThrows() {
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2, 3).map(i -> i).forEachUntil(MutableBoolean.of(false), t -> {
            if (t == 2) {
                throw new IllegalStateException("boom");
            }
        }));
    }

    @Test
    public void testGroupByNextWithoutHasNext() {
        final ObjIteratorEx<Map.Entry<Integer, List<Integer>>> it = iterEx(
                Stream.of(1, 2, 3, 4).map(i -> i).groupBy(i -> i % 2, i -> i, Collectors.toList(), LinkedHashMap::new));
        final Map.Entry<Integer, List<Integer>> first = it.next();
        assertEquals(1, first.getKey());
        assertEquals(Arrays.asList(1, 3), first.getValue());
    }

    @Test
    public void testMinAllMaxAll() {
        assertEquals(Arrays.asList(1, 1), Stream.of(1, 1, 2).sorted().minAll(Comparator2()));
        assertEquals(Arrays.asList(1, 1), Stream.of(1, 1).sorted().minAll(Comparator2()));
        assertEquals(Arrays.asList(3, 3), Stream.of(Arrays.asList(3, 1, 3, 2).iterator()).parallel(2).maxAll(Comparator2()));
        assertTrue(Stream.<Integer> empty().map(i -> i).maxAll(Comparator2()).isEmpty());
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).minAll(Comparator2()));
    }

    private static java.util.Comparator<Integer> Comparator2() {
        return java.util.Comparator.naturalOrder();
    }

    private static Integer fail2(final Integer i) {
        if (i == 2) {
            throw new IllegalStateException("fail at 2");
        }
        return i;
    }

    @Test
    public void testContainsFamily() {
        assertTrue(Stream.of(1, 2, 3).containsAll(2, 2));
        assertTrue(Stream.of(1, 2, 3).containsAll(new java.util.HashSet<>(Arrays.asList(3))));
        assertFalse(Stream.of(1, 2, 3).containsAny());
        assertTrue(Stream.of(1, 2, 3).containsAny(3, 3));
        assertTrue(Stream.of(1, 2, 3).containsAny(9, 3));
        assertFalse(Stream.of(1, 2, 3).containsAny(8, 9));
        assertTrue(Stream.of(1, 2, 3).containsNone());
        assertTrue(Stream.of(1, 2, 3).containsNone(new ArrayList<Integer>()));

        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).containsAll(5, 6, 7));
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).containsAll(Arrays.asList(5, 6)));
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).containsAny(5, 6, 7));
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).containsAny(Arrays.asList(5, 6)));
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).containsNone(5, 6, 7));
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).containsNone(Arrays.asList(5, 6)));
    }

    @Test
    public void testDebounceAndSkipRangeNextExhausted() {
        final ObjIteratorEx<Integer> d = iterEx(Stream.<Integer> empty().map(i -> i).debounce(Duration.ofSeconds(10)));
        assertThrows(NoSuchElementException.class, d::next);

        final ObjIteratorEx<Integer> s = iterEx(Stream.of(1, 2, 3).map(i -> i).skipRange(0, 3));
        assertThrows(NoSuchElementException.class, s::next);
    }

    @Test
    public void testPercentilesFailureAndParallelSort() {
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).percentiles(Comparator2()));
        final List<Integer> data = range(50);
        java.util.Collections.reverse(data);
        assertTrue(Stream.of(data).parallel(2).percentiles(Comparator2()).isPresent());
    }

    @Test
    public void testCombinationsNextAndAdvance() {
        final ObjIteratorEx<List<Integer>> it = iterEx(Stream.of(1, 2, 3).combinations(2));
        assertEquals(Arrays.asList(1, 2), it.next());
        assertEquals(Arrays.asList(1, 3), it.next());
        assertEquals(Arrays.asList(2, 3), it.next());
        assertThrows(NoSuchElementException.class, it::next);

        assertEquals(4, iterEx(Stream.of(1, 2).map(i -> i).combinations(2, true)).count());
        final ObjIteratorEx<List<Integer>> r0 = iterEx(Stream.of(1, 2).map(i -> i).combinations(2, true));
        assertEquals(Arrays.asList(1, 1), r0.next());

        final ObjIteratorEx<List<Integer>> r = iterEx(Stream.of(1, 2).map(i -> i).combinations(2, true));
        r.advance(0);
        r.advance(1);
        assertTrue(r.hasNext());
        r.advance(100);
        assertFalse(r.hasNext());
        assertThrows(NoSuchElementException.class, r::next);
    }

    @Test
    public void testCartesianProductNextAndAdvance() {
        final ObjIteratorEx<List<Integer>> it = iterEx(Stream.of(1, 2).map(i -> i).cartesianProduct(Arrays.asList(Arrays.asList(3, 4))));
        it.advance(0);
        it.advance(1);
        assertEquals(Arrays.asList(1, 4), it.next());
        it.advance(100);
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testJoinToAndContainsDuplicatesFailure() {
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).joinTo(Joiner.with(",")));
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).containsDuplicates());
    }

    @Test
    public void testCycled() {
        final ObjIteratorEx<Integer> e = iterEx(Stream.<Integer> empty().map(i -> i).cycled());
        assertThrows(NoSuchElementException.class, e::next);

        assertEquals(0, Stream.of(1, 2).map(i -> i).cycled(0).count());
        assertEquals(Arrays.asList(1, 2), Stream.of(1, 2).map(i -> i).cycled(1).toList());

        final ObjIteratorEx<Integer> c = iterEx(Stream.of(1).map(i -> i).cycled(2));
        assertEquals(1, c.next());
        assertEquals(1, c.next());
        assertFalse(c.hasNext());
        assertFalse(c.hasNext());
    }

    @Test
    public void testRollupIteratorPaths() {
        ObjIteratorEx<List<Integer>> it = iterEx(Stream.of(1, 2).map(i -> i).rollup());
        assertEquals(Arrays.asList(), it.next());
        assertEquals(Arrays.asList(1), it.next());
        assertEquals(Arrays.asList(1, 2), it.next());
        assertThrows(NoSuchElementException.class, it::next);

        it = iterEx(Stream.of(1, 2).map(i -> i).rollup());
        assertEquals(3, it.count());

        it = iterEx(Stream.of(1, 2).map(i -> i).rollup());
        it.advance(0);
        it.advance(2);
        assertEquals(Arrays.asList(1, 2), it.next());
        it.advance(5);
        assertFalse(it.hasNext());
    }

    @Test
    public void testBufferedHugeBound() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).buffered(Integer.MAX_VALUE).toList());
    }

    @Test
    public void testOnEachSaveFileNextWithoutHasNext() throws Exception {
        final File f1 = tempDir.resolve("a.txt").toFile();
        try (Stream<Integer> s = Stream.of(1, 2).onEachSave(String::valueOf, f1)) {
            final ObjIteratorEx<Integer> it = iterEx(s);
            assertEquals(1, it.next());
            assertEquals(2, it.next());
        }
        assertEquals("1\n2\n", new String(Files.readAllBytes(f1.toPath()), StandardCharsets.UTF_8));

        final File f2 = tempDir.resolve("b.txt").toFile();
        try (Stream<Integer> s = Stream.of(7).onEachSave((t, w) -> w.write("x" + t), f2)) {
            final ObjIteratorEx<Integer> it = iterEx(s);
            assertEquals(7, it.next());
        }
        assertTrue(new String(Files.readAllBytes(f2.toPath()), StandardCharsets.UTF_8).startsWith("x7"));
    }

    @Test
    public void testOnEachSaveJdbc() throws Exception {
        final PreparedStatement stmt = mock(PreparedStatement.class);
        when(stmt.executeBatch()).thenReturn(new int[0]);
        assertEquals(2, Stream.of(1, 2).onEachSave(stmt, 1, 0, (t, ps) -> ps.setInt(1, t)).count());
        verify(stmt, times(2)).execute();
        assertEquals(2, Stream.of(1, 2).onEachSave(stmt, 2, 1, (t, ps) -> ps.setInt(1, t)).count());
        verify(stmt, atLeastOnce()).executeBatch();

        final Connection conn = mock(Connection.class);
        final PreparedStatement stmt2 = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt2);
        when(stmt2.executeBatch()).thenReturn(new int[0]);
        assertEquals(2, Stream.of(1, 2).onEachSave(conn, "insert", 2, 1, (t, ps) -> ps.setInt(1, t)).count());
        assertEquals(1, Stream.of(1).onEachSave(conn, "insert", 1, 0, (t, ps) -> ps.setInt(1, t)).count());
        verify(stmt2, times(1)).execute();

        final DataSource ds = mock(DataSource.class);
        final Connection conn3 = mock(Connection.class);
        final PreparedStatement stmt3 = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn3);
        when(conn3.prepareStatement(anyString())).thenReturn(stmt3);
        when(stmt3.executeBatch()).thenReturn(new int[0]);
        assertEquals(2, Stream.of(1, 2).onEachSave(ds, "insert", 2, 1, (t, ps) -> ps.setInt(1, t)).count());
        assertEquals(1, Stream.of(1).onEachSave(ds, "insert", 1, 0, (t, ps) -> ps.setInt(1, t)).count());
        verify(stmt3, times(1)).execute();
        // never traversed: the lazily prepared statement is not created
        Stream.of(1).onEachSave(ds, "insert", 1, 0, (t, ps) -> ps.setInt(1, t)).close();
    }

    @Test
    public void testPersistJdbcBatchInterval() throws Exception {
        final PreparedStatement stmt = mock(PreparedStatement.class);
        when(stmt.executeBatch()).thenReturn(new int[0]);
        assertEquals(2, Stream.of(1, 2).persist(stmt, 2, 1, (t, ps) -> ps.setInt(1, t)));
        assertEquals(2, Stream.of(1, 2).persist(stmt, 1, 0, (t, ps) -> ps.setInt(1, t)));
        verify(stmt, times(2)).execute();
    }

    @Test
    public void testPersistLargeBatchesFlush() throws Exception {
        final List<Integer> data = range(1000);

        StringWriter sw = new StringWriter();
        assertEquals(1000, Stream.of(data).persist("H", "T", String::valueOf, sw));
        assertTrue(sw.toString().startsWith("H"));
        assertTrue(sw.toString().contains("999"));

        sw = new StringWriter();
        assertEquals(1000, Stream.of(data).persist("H", "T", (t, w) -> w.write(String.valueOf(t)), sw));
        assertTrue(sw.toString().contains("999"));

        final File f = tempDir.resolve("p.txt").toFile();
        assertEquals(2, Stream.of(1, 2).persist((t, w) -> w.write("v" + t), f));
        assertTrue(new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).contains("v2"));

        sw = new StringWriter();
        assertEquals(1000, Stream.of(data).persistToJson(sw));
        assertTrue(sw.toString().startsWith("["));
    }

    @Test
    public void testPersistToCsvLargeAndHeaders() throws Exception {
        final List<Row> beans = new ArrayList<>();
        final List<Map<String, Object>> maps = new ArrayList<>();
        final List<List<Object>> lists = new ArrayList<>();
        final List<Object[]> arrays = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            beans.add(new Row(i, "n" + i));
            final Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i);
            maps.add(m);
            lists.add(Arrays.asList(i, "x"));
            arrays.add(new Object[] { i, "y" });
        }

        StringWriter sw = new StringWriter();
        assertEquals(1000, Stream.of(beans).persistToCsv(sw));
        assertTrue(sw.toString().contains("n999"));

        sw = new StringWriter();
        assertEquals(1000, Stream.of(maps).persistToCsv(sw));
        assertTrue(sw.toString().contains("999"));

        sw = new StringWriter();
        assertEquals(1000, Stream.of(lists).persistToCsv(Arrays.asList("a", "b"), sw));
        assertTrue(sw.toString().contains("999"));

        sw = new StringWriter();
        assertEquals(1000, Stream.of(arrays).persistToCsv(Arrays.asList("a", "b"), sw));
        assertTrue(sw.toString().contains("999"));

        sw = new StringWriter();
        assertEquals(0, Stream.<Object[]> empty().persistToCsv(Arrays.asList("a", "b"), sw));
        assertEquals("\"a\",\"b\"", sw.toString().trim());
    }

    @Test
    public void testPersistToCsvAndJsonFailures() {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).persistToCsv(os));
        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2).map(AbstractStreamTest::fail2).persistToJson(new StringWriter()));
    }

    @Test
    public void testParallelJoins() {
        final List<Integer> right = Arrays.asList(2, 3, 4);

        assertEquals(4, Stream.of(1, 2, 3).parallel(2).fullJoin(right, i -> i, i -> i, Pair::of).count());
        assertEquals(4, Stream.of(1, 2, 3).parallel(2).fullJoin(right, (a, b) -> a.equals(b), Pair::of).count());
        assertEquals(3, Stream.of(1, 2, 3).parallel(2).rightJoin(right, i -> i, i -> i, Pair::of).count());
        assertEquals(3, Stream.of(1, 2, 3).parallel(2).rightJoin(Stream.of(right), i -> i, i -> i, Pair::of).count());
        assertEquals(3, Stream.of(1, 2, 3).parallel(2).rightJoin(right, (a, b) -> a.equals(b), Pair::of).count());
    }

    @Test
    public void testParallelGroupJoins() {
        final List<Integer> right = Arrays.asList(2, 2, 3);

        List<Pair<Integer, Integer>> r = Stream.of(1, 2, 3)
                .parallel(2)
                .groupJoin(right, i -> i, i -> i, (a, b) -> a + b, Pair::of)
                .sorted(java.util.Comparator.comparing(Pair::left))
                .toList();
        assertEquals(Integer.valueOf(4), r.get(1).right());

        r = Stream.of(1, 2, 3)
                .parallel(2)
                .groupJoin(Stream.of(right), i -> i, i -> i, (a, b) -> a + b, Pair::of)
                .sorted(java.util.Comparator.comparing(Pair::left))
                .toList();
        assertEquals(Integer.valueOf(3), r.get(2).right());

        final List<Pair<Integer, Long>> c = Stream.of(1, 2, 3)
                .parallel(2)
                .groupJoin(right, i -> i, i -> i, Collectors.counting(), Pair::of)
                .sorted(java.util.Comparator.comparing(Pair::left))
                .toList();
        assertEquals(Long.valueOf(2), c.get(1).right());
    }

    @Test
    public void testJoinByRange() {
        final List<Pair<Integer, List<Integer>>> r = Stream.of(1, 2)
                .map(i -> i)
                .joinByRange(Arrays.<Integer> asList().iterator(), (a, b) -> true, Collectors.toList(), Pair::of)
                .toList();
        assertEquals(2, r.size());
        assertTrue(r.get(0).right().isEmpty());

        final List<Pair<Integer, List<Integer>>> p = Stream.of(1, 2)
                .parallel(2)
                .joinByRange(Arrays.asList(1, 1, 2).iterator(), (a, b) -> a.equals(b), Collectors.toList(), Pair::of)
                .sorted(java.util.Comparator.comparing(Pair::left))
                .toList();
        assertEquals(Arrays.asList(1, 1), p.get(0).right());

        final List<Pair<Integer, List<Integer>>> u = Stream.of(1, 2)
                .map(i -> i)
                .joinByRange(Arrays.<Integer> asList().iterator(), (a, b) -> true, Collectors.toList(), Pair::of,
                        iter -> Stream.<Pair<Integer, List<Integer>>> empty())
                .toList();
        assertEquals(2, u.size());

        final List<Object> q = Stream.of(1, 2)
                .parallel(2)
                .joinByRange(Arrays.asList(1, 5).iterator(), (a, b) -> a.equals(b), Collectors.toList(), (a, l) -> (Object) Pair.of(a, l),
                        iter -> Stream.of(iter).map(x -> (Object) ("u" + x)))
                .toList();
        assertTrue(q.contains("u5"));
        assertEquals(3, q.size());
    }

    private static final class FailingWriter extends java.io.Writer {
        @Override
        public void write(final char[] cbuf, final int off, final int len) throws java.io.IOException {
            throw new java.io.IOException("write failed");
        }

        @Override
        public void flush() throws java.io.IOException {
            throw new java.io.IOException("flush failed");
        }

        @Override
        public void close() {
        }
    }

    private static final class FailingOutputStream extends java.io.OutputStream {
        @Override
        public void write(final int b) throws java.io.IOException {
            throw new java.io.IOException("write failed");
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws java.io.IOException {
            throw new java.io.IOException("write failed");
        }
    }

    private static String longLine() {
        final StringBuilder sb = new StringBuilder(100000);
        for (int i = 0; i < 100000; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    @Test
    public void testOnEachSaveWriterFailures() {
        final String big = longLine();
        assertThrows(RuntimeException.class, () -> Stream.of(big).onEachSave(t -> t, new FailingWriter()).count());
        assertThrows(RuntimeException.class, () -> Stream.of("x").onEachSave(t -> t, new FailingWriter()).count());
        assertThrows(RuntimeException.class, () -> Stream.of(big).onEachSave((t, w) -> w.write(t), new FailingWriter()).count());
        assertThrows(RuntimeException.class, () -> Stream.of("x").onEachSave((t, w) -> w.write(t), new FailingWriter()).count());
        assertThrows(RuntimeException.class, () -> Stream.of(big).onEachSave(t -> t, new FailingOutputStream()).count());
        assertThrows(RuntimeException.class, () -> Stream.of("x").onEachSave(t -> t, new FailingOutputStream()).count());
    }

    @Test
    public void testOnEachSaveJdbcFailures() throws Exception {
        final java.sql.SQLException fail = new java.sql.SQLException("bad");

        final PreparedStatement stmt = mock(PreparedStatement.class);
        when(stmt.executeBatch()).thenThrow(fail);
        when(stmt.execute()).thenThrow(fail);
        assertThrows(RuntimeException.class, () -> Stream.of(1).onEachSave(stmt, 1, 0, (t, ps) -> ps.setInt(1, t)).count());
        assertThrows(RuntimeException.class, () -> Stream.of(1).onEachSave(stmt, 2, 0, (t, ps) -> ps.setInt(1, t)).count());

        final Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        assertThrows(RuntimeException.class, () -> Stream.of(1).onEachSave(conn, "insert", 2, 0, (t, ps) -> ps.setInt(1, t)).count());

        final DataSource ds = mock(DataSource.class);
        final Connection conn3 = mock(Connection.class);
        when(ds.getConnection()).thenReturn(conn3);
        when(conn3.prepareStatement(anyString())).thenReturn(stmt);
        assertThrows(RuntimeException.class, () -> Stream.of(1).onEachSave(ds, "insert", 2, 0, (t, ps) -> ps.setInt(1, t)).count());
    }
}
