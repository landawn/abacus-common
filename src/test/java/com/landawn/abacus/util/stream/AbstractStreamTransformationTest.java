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
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.guava.Files;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;

/** AbstractStream transformation contracts. */
public class AbstractStreamTransformationTest extends AbstractStreamTestSupport {

    // collapse

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
    public void test_collapse_biPredicate() {
        List<List<Integer>> result = Stream.of(1, 2, 4, 5, 3, 6).collapse((a, b) -> b > a).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 4, 5), Arrays.asList(3, 6)), result);
    }

    @Test
    public void test_collapse_biPredicateAndCollector() {
        List<Integer> result = Stream.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p < c, Collectors.summingInt(i -> i)).toList();
        assertEquals(Arrays.asList(6, 3, 2, 1), result);
    }

    @Test
    public void test_collapse_biPredicateAndInitAndOp() {
        List<Integer> result = Stream.of(1, 2, 3, 1).collapse((p, c) -> p < c, 0, (r, c) -> r + c).toList();
        assertEquals(Arrays.asList(6, 1), result);
    }

    @Test
    public void test_collapse_biPredicateAndMergeFunction() {
        List<Integer> result = Stream.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p < c, (r, c) -> r + c).toList();
        assertEquals(Arrays.asList(6, 3, 2, 1), result);
    }

    @Test
    public void test_collapse_biPredicateAndSupplier() {
        List<Set<Integer>> result = Stream.of(1, 1, 2, 2, 3).collapse((a, b, c) -> a == c, Suppliers.ofSet()).toList();
        assertEquals(Arrays.asList(Set.of(1), Set.of(2), Set.of(3)), result);
    }



    // G110-06: a null merge function passed to collapse(BiPredicate, init, mergeFunction) is reported under its public name
    @Test
    public void testCollapse_biPredicateInitNullMergeFunction_messageNamesMergeFunction() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1, 2).collapse((a, b) -> true, "", (java.util.function.BiFunction<String, Integer, String>) null));
        assertEquals("'mergeFunction' cannot be null", e.getMessage());
    }



    // G110-06: same for collapse(TriPredicate, init, mergeFunction)
    @Test
    public void testCollapse_triPredicateInitNullMergeFunction_messageNamesMergeFunction() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1, 2).collapse((first, last, next) -> true, "", (java.util.function.BiFunction<String, Integer, String>) null));
        assertEquals("'mergeFunction' cannot be null", e.getMessage());
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
    public void testCollapseTriPredicate_collector() {
        stream = createStream(1, 2, 3, 10, 11, 12);
        List<Integer> result = stream.collapse((first, prev, curr) -> curr - first <= 2, Integer::sum).toList();
        assertEquals(2, result.size());
        assertEquals(6, result.get(0).intValue());
        assertEquals(33, result.get(1).intValue());
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
    public void testCollapseTriPredicateWithMerge() {
        Stream<Integer> stream = createStream(1, 1, 2, 2, 2, 3, 3);
        List<Integer> result = stream.collapse((prev, cur, next) -> prev.equals(cur), Integer::sum).toList();
        assertNotNull(result);
        assertTrue(result.size() >= 1);
    }

    @Test
    public void testCollapseWithCollector() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<String> result = stream.collapse((a, b) -> a.equals(b), Collectors.mapping(Object::toString, Collectors.joining(",")));
        assertEquals(Arrays.asList("1", "2,2", "3,3,3"), result.toList());
    }

    @Test
    public void testCollapseWithInitAndOp() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<Integer> result = stream.collapse((a, b) -> a.equals(b), 10, Integer::sum);
        assertEquals(Arrays.asList(11, 14, 19), result.toList());
    }

    @Test
    public void testCollapseWithMergeFunction() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<Integer> result = stream.collapse((a, b) -> a.equals(b), Integer::sum);
        assertEquals(Arrays.asList(1, 4, 9), result.toList());
    }

    @Test
    public void testCollapseWithSupplier() {
        stream = createStream(1, 2, 2, 3, 3, 3);
        Stream<Set<Integer>> result = stream.collapse((a, b) -> a.equals(b), Suppliers.ofSet());
        List<Set<Integer>> sets = result.toList();
        assertEquals(3, sets.size());
    }

    // cycled

    @Test
    public void test_cycled() {
        List<Integer> result = Stream.of(1, 2).cycled().limit(5).toList();
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), result);
    }

    @Test
    public void testCycled() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.cycled();
        List<Integer> list = result.limit(10).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2, 3, 1), list);
    }

    @Test
    public void test_cycled_withRounds() {
        List<Integer> result = Stream.of(1, 2).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 1, 2), result);
    }

    @Test
    public void testCycledWithRounds() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.cycled(2);
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result.toList());
    }

    // difference

    @Test
    public void test_difference() {
        List<Integer> result = Stream.of(1, 2, 2, 3, 4).difference(Arrays.asList(2, 3, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 4), result);
    }

    @Test
    public void testDifference() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.difference(Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2, 5), result.toList());
    }

    @Test
    public void test_difference_withMapper() {
        List<String> result = Stream.of("a", "b", "c").difference(s -> s.toUpperCase(), Arrays.asList("B")).toList();
        assertEquals(Arrays.asList("a", "c"), result);
    }

    @Test
    public void testDifferenceWithMapper() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.difference(i -> i % 2, Arrays.asList(0));
        assertEquals(Arrays.asList(1, 3, 4, 5), result.toList());
    }

    @Test
    public void test_distinct() {
        List<Integer> result = Stream.of(1, 2, 1, 3, 2).distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDistinct() {
        stream = createStream(1, 2, 2, 3, 3, 3, 4);
        Stream<Integer> result = stream.distinct();
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());
    }

    @Test
    public void test_distinctBy() {
        List<String> result = Stream.of("apple", "banana", "apricot", "blueberry").distinctBy(s -> s.charAt(0)).toList();
        assertEquals(Arrays.asList("apple", "banana"), result);
    }

    @Test
    public void testDistinctBy() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Integer> result = stream.distinctBy(i -> i % 3);
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    // dropWhile

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
    public void test_dropWhileWithActionOnDropped() {
        List<Integer> droppedItems = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 1).dropWhile(x -> x < 3, droppedItems::add).toList();
        assertEquals(Arrays.asList(3, 4, 1), result);
        assertEquals(Arrays.asList(1, 2), droppedItems);
    }

    @Test
    public void testDropWhileWithActionOnDropped() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> dropped = new ArrayList<>();
        Stream<Integer> result = stream.dropWhile(i -> i < 3, dropped::add);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
        assertEquals(Arrays.asList(1, 2), dropped);
    }

    // filter

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
    public void test_filterWithActionOnDropped() {
        List<Integer> droppedItems = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, droppedItems::add).toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 3, 5), droppedItems);
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
    public void test_indexed() {
        List<Indexed<String>> result = Stream.of("a", "b").indexed().toList();
        assertEquals(0, result.get(0).index());
        assertEquals("a", result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals("b", result.get(1).value());
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

    // intersection

    @Test
    public void test_intersection() {
        List<Integer> result = Stream.of(1, 2, 2, 3).intersection(Arrays.asList(2, 2, 4)).toList();
        assertEquals(Arrays.asList(2, 2), result);
    }

    @Test
    public void testIntersection() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.intersection(Arrays.asList(3, 4, 5, 6));
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
    }

    @Test
    public void test_intersection_withMapper() {
        List<String> result = Stream.of("a", "b", "c").intersection(s -> s.toUpperCase(), Arrays.asList("A", "C", "D")).toList();
        assertEquals(Arrays.asList("a", "c"), result);
    }

    @Test
    public void testIntersectionWithMapper() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.intersection(i -> i % 2, Arrays.asList(0, 1));
        assertEquals(Arrays.asList(1, 2), result.toList());
    }

    // intersperse

    @Test
    public void test_intersperse() {
        List<Integer> result = Stream.of(1, 2, 3).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Test
    public void testIntersperse() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.intersperse(0);
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result.toList());
    }

    @Test
    public void testIntersperse_method() {
        stream = createStream(1, 2, 3);
        List<Integer> result = stream.intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    // onEachSave

    @Test
    public void testonEachSave() throws IOException {
        File file = java.nio.file.Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        stream.onEachSave(file).toList();

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }

    @Test
    public void testonEachSave_2() throws IOException {
        File file = java.nio.file.Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        try (FileWriter writer = new FileWriter(file)) {
            stream.onEachSave((e, w) -> IOUtil.write(e, w), writer).toList();
        }

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(input, lines);
    }



    // onEachSave(Connection, batch): a failed first prepareStatement loses only the element pulled by that next(); the
    // batch count starts from the retry, so the remaining elements are flushed in full batches plus one tail batch on close.
    @Test
    public void testOnEachSave_connectionBatchCountAfterFailedPrepare() throws Exception {
        final Connection connection = mock(Connection.class);
        final PreparedStatement stmt = mock(PreparedStatement.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("prepare failed")).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[0]);

        final Stream<Integer> saved = Stream.of(1, 2, 3, 4).onEachSave(connection, "INSERT INTO t VALUES(?)", 2, 0, (e, ps) -> ps.setInt(1, e));
        final Iterator<Integer> iter = saved.iterator();

        assertThrows(RuntimeException.class, iter::next);
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        verify(stmt, times(2)).addBatch();
        verify(stmt, times(1)).executeBatch();
        assertEquals(4, iter.next());
        saved.close();

        verify(stmt, times(3)).addBatch();
        verify(stmt, times(2)).executeBatch();
        verify(stmt, times(1)).close();
        verify(connection, times(0)).close();
    }





    // onEachSave(DataSource): each failed prepareStatement (SQLException) releases the connection it obtained, a later
    // next() retries with a fresh connection, and closing the stream afterwards releases nothing twice.
    @Test
    public void testOnEachSave_dataSourceFailedPrepareReleasesEachConnection() throws Exception {
        final javax.sql.DataSource dataSource = mock(javax.sql.DataSource.class);
        final Connection conn1 = mock(Connection.class);
        final Connection conn2 = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(conn1, conn2);
        when(conn1.prepareStatement(anyString())).thenThrow(new SQLException("prepare failed 1"));
        when(conn2.prepareStatement(anyString())).thenThrow(new SQLException("prepare failed 2"));

        final Stream<Integer> saved = Stream.of(1, 2).onEachSave(dataSource, "INSERT INTO t VALUES(?)", (e, stmt) -> stmt.setInt(1, e));
        final Iterator<Integer> iter = saved.iterator();

        assertTrue(assertThrows(RuntimeException.class, iter::next).getMessage().contains("prepare failed 1"));
        assertTrue(assertThrows(RuntimeException.class, iter::next).getMessage().contains("prepare failed 2"));
        verify(dataSource, times(2)).getConnection();
        verify(conn1, times(1)).close();
        verify(conn2, times(1)).close();

        saved.close();
        verify(conn1, times(1)).close();
        verify(conn2, times(1)).close();
    }



    // onEachSave(DataSource): after an unchecked failure of prepareStatement the retry reuses the connection already
    // obtained (it is not replaced and leaked), and close() then executes nothing more and releases it exactly once.
    @Test
    public void testOnEachSave_dataSourceUncheckedPrepareFailureReusesConnection() throws Exception {
        final javax.sql.DataSource dataSource = mock(javax.sql.DataSource.class);
        final Connection conn1 = mock(Connection.class);
        final Connection conn2 = mock(Connection.class);
        final PreparedStatement stmt = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(conn1, conn2);
        when(conn1.prepareStatement(anyString())).thenThrow(new IllegalStateException("driver state")).thenReturn(stmt);

        final Stream<Integer> saved = Stream.of(1, 2, 3).onEachSave(dataSource, "INSERT INTO t VALUES(?)", (e, ps) -> ps.setInt(1, e));
        final Iterator<Integer> iter = saved.iterator();

        assertThrows(IllegalStateException.class, iter::next);
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
        saved.close();

        verify(dataSource, times(1)).getConnection();
        verify(stmt, times(2)).execute();
        verify(stmt, times(1)).close();
        verify(conn1, times(1)).close();
        verify(conn2, times(0)).close();
    }





    // G114-06: onEachSave(Connection/DataSource, ...) marked itself initialized before preparing the statement, so
    // after a failed prepareStatement/getConnection the next next() passed a null statement on (NPE) instead of
    // retrying and reporting the SQL failure again, as the File overloads do.
    @Test
    public void testOnEachSave_failedPrepareIsRetriedNotNullStatement() throws Exception {
        final Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("prepare failed"));

        final Iterator<Integer> iter = Stream.of(1, 2).onEachSave(connection, "INSERT INTO t VALUES(?)", (e, stmt) -> stmt.setInt(1, e)).iterator();

        final RuntimeException first = assertThrows(RuntimeException.class, iter::next);
        final RuntimeException second = assertThrows(RuntimeException.class, iter::next);
        assertEquals(first.getClass(), second.getClass());
        assertTrue(second.getMessage().contains("prepare failed"), String.valueOf(second));
        verify(connection, times(2)).prepareStatement(anyString());

        final javax.sql.DataSource dataSource = mock(javax.sql.DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("no connection"));

        final Iterator<Integer> iter2 = Stream.of(1, 2).onEachSave(dataSource, "INSERT INTO t VALUES(?)", (e, stmt) -> stmt.setInt(1, e)).iterator();

        assertThrows(RuntimeException.class, iter2::next);
        final RuntimeException dsSecond = assertThrows(RuntimeException.class, iter2::next);
        assertTrue(dsSecond.getMessage().contains("no connection"), String.valueOf(dsSecond));
        verify(dataSource, times(2)).getConnection();
    }

    @Test
    public void test_onEachSave_withConnection() throws SQLException {
        Connection connMock = mock(Connection.class);
        PreparedStatement stmtMock = mock(PreparedStatement.class);
        when(connMock.prepareStatement(anyString())).thenReturn(stmtMock);

        Stream.of("a", "b").onEachSave(connMock, "INSERT INTO foo VALUES(?)", (val, ps) -> ps.setString(1, val)).count();

        verify(connMock, times(1)).prepareStatement("INSERT INTO foo VALUES(?)");
        verify(stmtMock, times(2)).setString(anyInt(), anyString());
        verify(stmtMock, times(2)).execute();
        verify(stmtMock, times(1)).close();
    }

    @Test
    public void test_onEachSave_withStmtSetter() throws SQLException {
        PreparedStatement stmtMock = mock(PreparedStatement.class);
        final AtomicInteger counter = new AtomicInteger(0);

        doAnswer(invocation -> {
            counter.incrementAndGet();
            return null;
        }).when(stmtMock).addBatch();

        Stream.of("a", "b", "c").onEachSave(stmtMock, 2, 0, (val, ps) -> ps.setString(1, val)).count();

        verify(stmtMock, times(3)).setString(anyInt(), anyString());
        verify(stmtMock, times(3)).addBatch();
        verify(stmtMock, times(2)).executeBatch();

    }

    @Test
    public void testOnEachSaveNullConnectionMessageNamesConn() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1).onEachSave((Connection) null, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> {
                }));
        assertEquals("'connection' cannot be null", e.getMessage());
    }

    @Test
    public void testOnEachSaveNullDataSourceMessageNamesDs() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1).onEachSave((javax.sql.DataSource) null, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> {
                }));
        assertEquals("'dataSource' cannot be null", e.getMessage());
    }

    @Test
    public void testOnEachSaveNullStatementMessageNamesStmt() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1).onEachSave((PreparedStatement) null, 1, 0, (i, ps) -> {
                }));
        assertEquals("'statement' cannot be null", e.getMessage());
    }

    @Test
    public void testOnEachSaveReleasesConnectionOnceWhenPreparationFails() throws SQLException {
        for (final boolean terminal : new boolean[] { false, true }) {
            final javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
            final Connection connection = mock(Connection.class);
            final SQLException failure = new SQLException("prepare failed");
            when(ds.getConnection()).thenReturn(connection);
            when(connection.prepareStatement(anyString())).thenThrow(failure);
            final AtomicInteger closeCount = new AtomicInteger();
            final Stream<Integer> saved = Stream.of(1)
                    .onClose(closeCount::incrementAndGet)
                    .onEachSave(ds, "INSERT INTO test VALUES (?)", (value, statement) -> statement.setInt(1, value));

            final RuntimeException thrown = terminal ? assertThrows(RuntimeException.class, saved::count)
                    : assertThrows(RuntimeException.class, () -> saved.iterator().next());
            org.junit.jupiter.api.Assertions.assertSame(failure, thrown.getCause());
            verify(connection, times(1)).close();
            saved.close();
            verify(connection, times(1)).close();
            assertEquals(1, closeCount.get());
        }
    }

    @Test
    public void testonEachSaveToConnection() throws SQLException {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        stream = createStream(1, 2, 3);
        stream.onEachSave(conn, "INSERT INTO test VALUES (?)", (i, ps) -> ps.setInt(1, i)).toList();

        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testonEachSaveToDataSource() throws SQLException {
        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        stream = createStream(1, 2, 3);
        stream.onEachSave(ds, "INSERT INTO test VALUES (?)", (i, ps) -> ps.setInt(1, i)).toList();

        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testonEachSaveToFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        stringStream = createStream("line1", "line2", "line3");
        stringStream.onEachSave(tempFile).toList();

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
    }

    @Test
    public void testonEachSaveToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        stringStream = createStream("line1", "line2", "line3");
        stringStream.onEachSave(Fn.identity(), baos).toList();

        String result = baos.toString();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
    }

    @Test
    public void testonEachSaveToPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        stream = createStream(1, 2, 3);
        stream.onEachSave(stmt, (i, ps) -> ps.setInt(1, i)).toList();

        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testonEachSaveToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        stringStream = createStream("line1", "line2", "line3");
        stringStream.onEachSave(Fn.identity(), writer).toList();

        String result = writer.toString();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
    }

    @Test
    public void testonEachSaveWithBatchSize() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        List<TestBean> input = Arrays.asList(new TestBean("A", 1), new TestBean("B", 2), new TestBean("C", 3), new TestBean("D", 4));
        Stream<TestBean> stream = createStream(input);

        List<TestBean> result = stream.onEachSave(stmt, 2, 0, (bean, ps) -> {
            ps.setString(1, bean.getName());
            ps.setInt(2, bean.getAge());
        }).toList();

        assertEquals(input, result);
        verify(stmt, times(4)).addBatch();
        verify(stmt, times(2)).executeBatch();
    }

    @Test
    public void testonEachSaveWithBiConsumer() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        stringStream = createStream("line1", "line2", "line3");
        stringStream.onEachSave((s, writer) -> writer.write(s.toUpperCase()), tempFile).toList();

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(Arrays.asList("LINE1", "LINE2", "LINE3"), lines);
    }

    // onFirst

    @Test
    public void testOnFirst() {
        stream = createStream(1, 2, 3);
        List<Integer> firstElement = new ArrayList<>();
        stream.onFirst(firstElement::add).toList();
        assertEquals(Arrays.asList(1), firstElement);
    }

    @Test
    public void test_onFirst() {
        Holder<Integer> first = new Holder<>();
        Stream.of(1, 2, 3).onFirst(first::setValue).forEach(Fn.emptyConsumer());
        assertEquals(1, first.value());
    }

    @Test
    public void testOnFirst_action() {
        stream = createStream(1, 2, 3);
        Holder<Integer> first = Holder.of(0);
        List<Integer> result = stream.onFirst(first::setValue).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(1, first.value().intValue());
    }

    // onLast

    @Test
    public void testOnLast() {
        stream = createStream(1, 2, 3);
        List<Integer> lastElement = new ArrayList<>();
        stream.onLast(lastElement::add).toList();
        assertEquals(Arrays.asList(3), lastElement);
    }

    @Test
    public void test_onLast() {
        Holder<Integer> last = new Holder<>();
        Stream.of(1, 2, 3).onLast(last::setValue).forEach(Fn.emptyConsumer());
        assertEquals(3, last.value());
    }

    @Test
    public void testOnLast_action() {
        stream = createStream(1, 2, 3);
        Holder<Integer> last = Holder.of(0);
        List<Integer> result = stream.onLast(last::setValue).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(3, last.value().intValue());
    }

    // onlyOne

    @Test
    public void test_onlyOne() {
        Optional<Integer> result = Stream.of(1).onlyOne();
        assertEquals(Optional.of(1), result);
        assertThrows(Exception.class, () -> Stream.of(1, 2).onlyOne());
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
    public void testOnlyOne_tooMany() {
        stream = createStream(1, 2, 3);
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testOnlyOneWithMultipleElements() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2));
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
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
    public void test_reversed() {
        List<Integer> result = Stream.of(1, 2, 3).reversed().toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testReversed() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.reversed();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result.toList());
    }

    // reverseSorted

    @Test
    public void test_reverseSorted() {
        List<Integer> result = Stream.of(1, 3, 2).reverseSorted().toList();
        assertEquals(List.of(3, 2, 1), result);
    }

    @Test
    public void testReverseSorted() {
        stream = createStream(3, 1, 4, 1, 5, 9);
        Stream<Integer> result = stream.reverseSorted();
        assertEquals(Arrays.asList(9, 5, 4, 3, 1, 1), result.toList());
    }

    @Test
    public void testReverseSorted_withComparator() {
        stream = createStream(3, 1, 2, 5, 4);
        List<Integer> result = stream.reverseSorted(Comparator.naturalOrder()).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
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
    public void testReverseSortedByDouble() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.reverseSortedByDouble(s -> (double) s.length());
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

    // rotated

    @Test
    public void test_rotated() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).rotated(2).toList();
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result);

        result = Stream.of(1, 2, 3, 4, 5).rotated(-2).toList();
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), result);
    }

    @Test
    public void testRotated() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.rotated(2);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result.toList());
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
    public void testRotated_NegativeDistance() {
        Stream<Integer> s = Stream.of(1, 2, 3, 4, 5);
        java.util.List<Integer> result = s.rotated(-2).toList();
        assertEquals(java.util.Arrays.asList(3, 4, 5, 1, 2), result);
    }



    // G086-02 corrected 2026-10-03: the 09-26 performance oracle pinned overwrite corruption.
    // toArray must preserve the rotated encounter order even when the destination aliases the source.
    @Test
    public void testRotated_toArrayIntoBackingArrayPreservesRotatedElements() {
        final Integer[] backing = { 1, 2, 3, 4, 5 };
        final Integer[] expected = { 4, 5, 1, 2, 3 };

        final ObjIterator<Integer> iter = Stream.of(backing).rotated(2).iterator();

        org.junit.jupiter.api.Assertions.assertSame(backing, iter.toArray(backing));
        assertArrayEquals(expected, backing);
    }



    // G086-02: a target array whose type cannot hold the elements still fails element by element with the usual ArrayStoreException
    @Test
    public void testRotated_toArrayIntoIncompatibleArrayThrowsArrayStoreException() {
        final ObjIterator<Integer> iter = Stream.of(1, 2, 3).rotated(1).iterator();
        final ArrayStoreException e = assertThrows(ArrayStoreException.class, () -> iter.toArray(new String[3]));

        assertFalse(String.valueOf(e.getMessage()).contains("arraycopy"), e.getMessage());
    }

    @Test
    public void testRotated_ZeroDistance() {
        Stream<Integer> s = Stream.of(1, 2, 3, 4, 5);
        java.util.List<Integer> result = s.rotated(0).toList();
        assertEquals(java.util.Arrays.asList(1, 2, 3, 4, 5), result);
    }

    // scan

    @Test
    public void testScan() {
        stream = createStream(1, 2, 3, 4);
        Stream<Integer> result = stream.scan(Integer::sum);
        assertEquals(Arrays.asList(1, 3, 6, 10), result.toList());
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
    public void test_scan_initAndAccumulatorAndFlag_false() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, false, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(11, 13, 16), result);
    }

    @Test
    public void test_scan_initAndAccumulatorAndFlag_true() {
        List<Integer> result = Stream.of(1, 2, 3).scan(10, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(10, 11, 13, 16), result);
    }

    @Test
    public void testScan_initAndAccumulatorAndFlag_true_empty() {
        stream = createStream();
        List<Integer> result = stream.scan(10, true, Integer::sum).toList();
        assertEquals(Arrays.asList(10), result);
    }

    @Test
    public void testScanInitializesOnlyAfterSuccessfulSourceRead() {
        for (final String first : new String[] { "a", null }) {
            final IllegalStateException failure = new IllegalStateException("first read failed");
            final AtomicInteger attempts = new AtomicInteger();
            final AtomicInteger accumulatorCalls = new AtomicInteger();
            final Iterator<String> source = new Iterator<>() {
                private final Iterator<String> values = Arrays.asList(first, "b").iterator();

                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public String next() {
                    if (attempts.getAndIncrement() == 0) {
                        throw failure;
                    }
                    return values.next();
                }
            };

            try (Stream<String> stream = Stream.of(source).scan((left, right) -> {
                accumulatorCalls.incrementAndGet();
                return left + right;
            })) {
                final ObjIterator<String> iter = stream.iterator();
                org.junit.jupiter.api.Assertions.assertSame(failure, assertThrows(IllegalStateException.class, iter::next));
                assertEquals(first, iter.next());
                assertEquals(0, accumulatorCalls.get());
                assertEquals(first + "b", iter.next());
                assertEquals(1, accumulatorCalls.get());
                assertFalse(iter.hasNext());
            }
        }
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
    public void test_shuffled() {
        List<Integer> source = List.of(1, 2, 3, 4, 5);
        List<Integer> result = Stream.of(source).shuffled(new Random(1)).toList();
        assertFalse(source.equals(result));
        assertEquals(source.size(), result.size());
        assertTrue(result.containsAll(source));
    }

    @Test
    public void testShuffled() {
        stream = createStream(1, 2, 3, 4, 5);
        Random random = new Random(42);
        List<Integer> result = stream.shuffled(random).toList();
        assertEquals(5, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5)));
    }

    // skip

    @Test
    public void test_skip_withAction() {
        List<Integer> skipped = new ArrayList<>();
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).skip(2, skipped::add).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    @Test
    public void testSkipNegative() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> stream.skip(-1, i -> {
        }));
    }

    @Test
    public void testSkipWithAction() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> skipped = new ArrayList<>();
        Stream<Integer> result = stream.skip(2, skipped::add);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
        assertEquals(Arrays.asList(1, 2), skipped);
    }

    // skipNulls

    @Test
    public void test_skipNulls() {
        List<Integer> source = Arrays.asList(1, null, 2, null, 3);
        List<Integer> result = Stream.of(source).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipNulls() {
        objectStream = createStream("a", null, "b", null, "c");
        Stream<Object> result = objectStream.skipNulls();
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());
    }

    @Test
    public void testSkipNulls_Parallel() {
        List<Integer> result = Stream.of(1, null, 2, null, 3).parallel().skipNulls().sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipNulls_Sequential() {
        List<Integer> result = Stream.of(1, null, 2, null, 3).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // skipRange

    @Test
    public void test_skipRange() {
        List<Integer> result = Stream.of(0, 1, 2, 3, 4, 5).skipRange(2, 4).toList();
        assertEquals(Arrays.asList(0, 1, 4, 5), result);
    }

    @Test
    public void testSkipRange() {
        stream = createStream(1, 2, 3, 4, 5, 6);
        Stream<Integer> result = stream.skipRange(2, 4);
        assertEquals(Arrays.asList(1, 2, 5, 6), result.toList());
    }

    @Test
    public void testSkipRange_fullRange() {
        stream = createStream(1, 2, 3, 4, 5);
        List<Integer> result = stream.skipRange(0, 5).toList();
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testSkipRangeDoesNotCountFailedRetainedRead() {
        final IllegalStateException failure = new IllegalStateException("retained read failed");
        try (Stream<Integer> stream = Stream.of(sourceFailingOnceAt(0, false, failure)).skipRange(1, 4)) {
            final ObjIterator<Integer> iter = stream.iterator();
            org.junit.jupiter.api.Assertions.assertSame(failure, assertThrows(IllegalStateException.class, iter::next));
            assertEquals(0, iter.next());
            assertEquals(4, iter.next());
            assertEquals(5, iter.next());
            assertFalse(iter.hasNext());
        }
    }

    @Test
    public void testSkipRangeResumesUnfinishedSkipAfterSourceFailure() {
        for (final boolean failInHasNext : new boolean[] { false, true }) {
            final IllegalStateException failure = new IllegalStateException("skip read failed");
            try (Stream<Integer> stream = Stream.of(sourceFailingOnceAt(2, failInHasNext, failure)).skipRange(1, 4)) {
                final ObjIterator<Integer> iter = stream.iterator();
                assertEquals(0, iter.next());
                org.junit.jupiter.api.Assertions.assertSame(failure, assertThrows(IllegalStateException.class, iter::hasNext));
                assertTrue(iter.hasNext());
                assertEquals(4, iter.next());
                assertEquals(5, iter.next());
                assertFalse(iter.hasNext());
            }
        }
    }

    @Test
    public void test_skipUntil() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 2).skipUntil(x -> x > 3).toList();
        assertEquals(Arrays.asList(4, 5, 2), result);
    }

    @Test
    public void testSkipUntil() {
        stream = createStream(1, 2, 3, 4, 5);
        Stream<Integer> result = stream.skipUntil(i -> i > 2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());
    }

    // sorted

    @Test
    public void test_sorted() {
        List<Integer> result = Stream.of(3, 1, 2).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSorted() {
        stream = createStream(3, 1, 4, 1, 5, 9);
        Stream<Integer> result = stream.sorted();
        assertEquals(Arrays.asList(1, 1, 3, 4, 5, 9), result.toList());
    }

    @Test
    public void testSorted_RejectsNullComparator() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(3, 1, 2).sorted((java.util.Comparator<Integer>) null));
    }

    @Test
    public void test_sorted_withComparator() {
        List<Integer> result = Stream.of(1, 2, 3).sorted(Comparator.reverseOrder()).toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
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
    public void testSortedByDouble() {
        stringStream = createStream("aaa", "b", "cc");
        Stream<String> result = stringStream.sortedByDouble(s -> (double) s.length());
        assertEquals(Arrays.asList("b", "cc", "aaa"), result.toList());
    }

    @Test
    public void test_sortedByInt() {
        List<String> result = Stream.of("apple", "kiwi", "banana").sortedByInt(String::length).toList();
        assertEquals(List.of("kiwi", "apple", "banana"), result);
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

    // step

    @Test
    public void test_step() {
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6).step(2).toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void testStep() {
        stream = createStream(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertEquals(Arrays.asList(1, 3, 5, 7, 9), stream.step(2).toList());
    }

    @Test
    public void testStep_OneReturnsAllElements() {
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(1, 2, 3).step(1).toList());
    }

    @Test
    public void testStepWithInvalidArgument() {
        Stream<Integer> stream = createStream(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> stream.step(0));
    }

    @Test
    public void test_symmetricDifference() {
        List<Integer> result = Stream.of(1, 2, 3).symmetricDifference(Arrays.asList(3, 4, 5)).toSet().stream().sorted().toList();
        assertEquals(Arrays.asList(1, 2, 4, 5), result);
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



    // G086-02: rotated() toArray(A[]) copies the two contiguous runs in bulk when no array-store check can fail; checked against a modulo oracle
    @Test
    public void testRotated_toArrayMatchesModuloOracle() {
        final int[] distances = { 0, 1, 2, 3, 5, 7, 8, 13, -1, -2, -3, -7, -8, -13, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE + 1 };

        for (int len = 0; len <= 8; len++) {
            final int from = 2;
            final Integer[] backing = new Integer[len + 5];

            for (int i = 0; i < backing.length; i++) {
                backing[i] = i * 10 - 7;
            }

            for (final int distance : distances) {
                final Integer[] expected = new Integer[len];

                for (int i = 0; i < len; i++) {
                    expected[i] = backing[from + (int) Math.floorMod((long) i - distance, (long) len)];
                }

                // source 0: array-backed (the rotated range is the caller's Integer[]); source 1: iterator-backed (an Object[] snapshot)
                for (int source = 0; source < 2; source++) {
                    final String msg = "len=" + len + ", distance=" + distance + ", source=" + source;

                    assertArrayEquals(expected, rotatedForG086(backing, from, len, source, distance).toArray(), msg);
                    assertEquals(Arrays.asList(expected), rotatedForG086(backing, from, len, source, distance).toList(), msg);

                    for (int k = 0; k <= len + 1; k++) {
                        final Object[] rest = Arrays.copyOfRange(expected, Math.min(k, len), len);

                        assertArrayEquals(rest, rotatedForG086(backing, from, len, source, distance).skip(k).toArray(), msg + ", skip=" + k);

                        // Integer[] target: bulk copy for the array source, element-wise for the Object[] snapshot
                        final ObjIterator<Integer> byNext = rotatedForG086(backing, from, len, source, distance).iterator();

                        for (int j = 0; j < Math.min(k, len); j++) {
                            assertEquals(expected[j], byNext.next(), msg + ", k=" + k + ", j=" + j);
                        }

                        assertArrayEquals(rest, byNext.toArray(new Integer[0]), msg + ", k=" + k);
                        assertFalse(byNext.hasNext(), msg);

                        // a larger target keeps its tail and gets a null terminator
                        final Object[] target = new Object[rest.length + 2];
                        target[rest.length + 1] = "tail";
                        final ObjIteratorEx<Integer> byAdvance = rotatedForG086(backing, from, len, source, distance).iteratorEx();
                        byAdvance.advance(k);
                        org.junit.jupiter.api.Assertions.assertSame(target, byAdvance.toArray(target), msg);
                        assertArrayEquals(rest, Arrays.copyOf(target, rest.length), msg + ", advance=" + k);
                        org.junit.jupiter.api.Assertions.assertNull(target[rest.length], msg);
                        assertEquals("tail", target[rest.length + 1], msg);
                    }
                }
            }
        }
    }

    @Test
    public void test_top() {
        List<Integer> result = Stream.of(3, 1, 4, 1, 5, 9, 2, 6).top(3).toList();
        assertEquals(Arrays.asList(5, 9, 6), result);
    }

    @Test
    public void testTop() {
        stream = createStream(5, 2, 8, 1, 9, 3);
        Stream<Integer> result = stream.top(3);
        assertEquals(Arrays.asList(5, 8, 9), result.toList());
    }

}
