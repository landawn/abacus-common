package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;

@Tag("new-test")
public class AbstractStream201Test extends TestBase {

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
}
