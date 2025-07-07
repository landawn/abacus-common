package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.StringWriter;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AbstractStream101Test extends TestBase {

    @TempDir
    Path tempFolder;

    private <T> Stream<T> createStream(Iterable<? extends T> iter) {
        // Empty method to be implemented
        return Stream.of(iter);
    }
    @Test
    public void testJoinByRangeWithCollector() {
        List<Integer> left = Arrays.asList(1, 5, 10);
        List<Integer> right = Arrays.asList(2, 3, 6, 7, 11);
        Stream<Integer> stream = createStream(left);

        List<Pair<Integer, Integer>> result = stream.joinByRange(Stream.of(right), (l, r) -> r > l && r < l + 5, Collectors.summingInt(Integer::intValue))
                .toList();

        assertEquals(3, result.size());
        assertEquals(5, result.get(0).right().intValue()); // 1 -> sum(2,3) = 5
        assertEquals(13, result.get(1).right().intValue()); // 5 -> sum(6,7) = 13
        assertEquals(11, result.get(2).right().intValue()); // 10 -> sum(11) = 11
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
    public void testRightJoin() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.rightJoin(orders, TestPerson::getId, TestOrder::getPersonId).toList();

        assertEquals(3, result.size());
        // One of the pairs should have null person (for Order3)
        assertTrue(result.stream().anyMatch(p -> p.left() == null));
    }

    @Test
    public void testGroupJoinWithMergeFunction() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "100"), new TestOrder(1, "200"), new TestOrder(2, "300"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, String>> result = stream
                .groupJoin(orders, TestPerson::getId, TestOrder::getPersonId,
                        Fn.f((a, b) -> Pair.of(a, b.stream().map(TestOrder::getOrderName).collect(Collectors.joining(",")))))
                .toList();

        assertEquals(2, result.size());
        assertEquals("100,200", result.get(0).right());
        assertEquals("300", result.get(1).right());
    }

    @Test
    public void testGroupJoinWithDownstream() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "100"), new TestOrder(1, "200"), new TestOrder(2, "300"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, Long>> result = stream.groupJoin(orders, TestPerson::getId, TestOrder::getPersonId, Collectors.counting()).toList();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).right().longValue()); // John has 2 orders
        assertEquals(1L, result.get(1).right().longValue()); // Jane has 1 order
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
    public void testPersistWithHeaderAndTail() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("data1", "data2", "data3");
        Stream<String> stream = createStream(input);

        long count = stream.persist("HEADER", "TAIL", file);
        assertEquals(3, count);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(5, lines.size());
        assertEquals("HEADER", lines.get(0));
        assertEquals("data1", lines.get(1));
        assertEquals("data2", lines.get(2));
        assertEquals("data3", lines.get(3));
        assertEquals("TAIL", lines.get(4));
    }

    @Test
    public void testPersistToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> input = Arrays.asList("line1", "line2", "line3");
        Stream<String> stream = createStream(input);

        long count = stream.persist(Fn.identity(), baos);
        assertEquals(3, count);

        String result = baos.toString();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
    }

    @Test
    public void testPersistWithBiConsumer() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("A", "B", "C");
        Stream<String> stream = createStream(input);

        long count = stream.persist((value, writer) -> {
            writer.write("PREFIX-");
            writer.write(value);
        }, file);
        assertEquals(3, count);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).startsWith("PREFIX-A"));
        assertTrue(lines.get(1).startsWith("PREFIX-B"));
        assertTrue(lines.get(2).startsWith("PREFIX-C"));
    }

    @Test
    public void testPersistToPreparedStatement() throws SQLException {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persist(conn, "INSERT INTO users VALUES (?, ?)", 2, 0, (bean, ps) -> {
            ps.setString(1, bean.getName());
            ps.setInt(2, bean.getAge());
        });

        assertEquals(2, count);
        verify(stmt, times(2)).addBatch();
        verify(stmt, times(1)).executeBatch();
    }

    @Test
    public void testPersistToDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<TestBean> input = Arrays.asList(new TestBean("John", 25));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persist(ds, "INSERT INTO users VALUES (?, ?)", 1, 0, (bean, ps) -> {
            ps.setString(1, bean.getName());
            ps.setInt(2, bean.getAge());
        });

        assertEquals(1, count);
        verify(stmt).execute();
    }

    @Test
    public void testPersistToCSVWithHeaders() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<Map<String, Object>> input = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "John");
        row1.put("age", 25);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Jane");
        row2.put("age", 30);
        input.add(row1);
        input.add(row2);

        Stream<Map<String, Object>> stream = createStream(input);

        long count = stream.persistToCSV(Arrays.asList("name", "age"), file);
        assertEquals(2, count);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("name"));
        assertTrue(lines.get(0).contains("age"));
    }

    @Test
    public void testPersistToCSVOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persistToCSV(baos);
        assertEquals(2, count);

        String csv = baos.toString();
        assertTrue(csv.contains("name"));
        assertTrue(csv.contains("age"));
        assertTrue(csv.contains("John"));
        assertTrue(csv.contains("Jane"));
    }

    @Test
    public void testPersistToCSVWriter() throws IOException {
        StringWriter writer = new StringWriter();
        List<TestBean> input = Arrays.asList(new TestBean("John", 25));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persistToCSV(writer);
        assertEquals(1, count);

        String csv = writer.toString();
        assertTrue(csv.contains("name"));
        assertTrue(csv.contains("age"));
        assertTrue(csv.contains("John"));
        assertTrue(csv.contains("25"));
    }

    @Test
    public void testPersistToCSVWriter_withHeaders() throws IOException {
        {
            StringWriter writer = new StringWriter();
            List<TestBean> input = Arrays.asList(new TestBean("John", 25));
            Stream<TestBean> stream = createStream(input);

            long count = stream.persistToCSV(N.asList("name", "age"), writer);
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

            long count = stream.persistToCSV(N.asList("name", "age"), writer);
            assertEquals(1, count);

            String csv = writer.toString();
            assertTrue(csv.contains("name"));
            assertTrue(csv.contains("age"));
            assertTrue(csv.contains("John"));
            assertTrue(csv.contains("25"));
        }
        {
            StringWriter writer = new StringWriter();
            List<List<Object>> input = Arrays.asList(N.asList("John", 25));
            Stream<List<Object>> stream = createStream(input);

            long count = stream.persistToCSV(N.asList("name", "age"), writer);
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

            long count = stream.persistToCSV(N.asList("name", "age"), writer);
            assertEquals(1, count);

            String csv = writer.toString();
            assertTrue(csv.contains("name"));
            assertTrue(csv.contains("age"));
            assertTrue(csv.contains("John"));
            assertTrue(csv.contains("25"));
        }
    }

    @Test
    public void testPersistToJSONOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        long count = stream.persistToJSON(baos);
        assertEquals(2, count);

        String json = baos.toString();
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
        assertTrue(json.contains("John"));
        assertTrue(json.contains("Jane"));
    }

    @Test
    public void testPersistToJSONWriter() throws IOException {
        StringWriter writer = new StringWriter();
        List<Map<String, Object>> input = new ArrayList<>();
        Map<String, Object> obj = new HashMap<>();
        obj.put("key", "value");
        input.add(obj);

        Stream<Map<String, Object>> stream = createStream(input);

        long count = stream.persistToJSON(writer);
        assertEquals(1, count);

        String json = writer.toString();
        assertTrue(json.contains("["));
        assertTrue(json.contains("]"));
        assertTrue(json.contains("key"));
        assertTrue(json.contains("value"));
    }

    @Test
    public void testSaveEachToOutputStream() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        List<String> input = Arrays.asList("A", "B", "C");
        Stream<String> stream = createStream(input);

        List<String> result = stream.saveEach(s -> "Line: " + s, baos).toList();
        assertEquals(input, result);

        String output = baos.toString();
        assertTrue(output.contains("Line: A"));
        assertTrue(output.contains("Line: B"));
        assertTrue(output.contains("Line: C"));
    }

    @Test
    public void testSaveEachToWriter() {
        StringWriter writer = new StringWriter();
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        List<Integer> result = stream.saveEach(n -> "Number: " + n, writer).toList();
        assertEquals(input, result);

        String output = writer.toString();
        assertTrue(output.contains("Number: 1"));
        assertTrue(output.contains("Number: 2"));
        assertTrue(output.contains("Number: 3"));
    }

    @Test
    public void testSaveEachWithBiConsumer() throws IOException {
        File file = Files.createTempFile(tempFolder, null, null).toFile();
        List<String> input = Arrays.asList("A", "B", "C");
        Stream<String> stream = createStream(input);

        List<String> result = stream.saveEach((value, writer) -> {
            writer.write("[");
            writer.write(value);
            writer.write("]");
        }, file).toList();

        assertEquals(input, result);

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).equals("[A]"));
        assertTrue(lines.get(1).equals("[B]"));
        assertTrue(lines.get(2).equals("[C]"));
    }

    @Test
    public void testSaveEachToPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        List<TestBean> input = Arrays.asList(new TestBean("John", 25), new TestBean("Jane", 30));
        Stream<TestBean> stream = createStream(input);

        List<TestBean> result = stream.saveEach(stmt, (bean, ps) -> {
            ps.setString(1, bean.getName());
            ps.setInt(2, bean.getAge());
        }).toList();

        assertEquals(input, result);
        verify(stmt, times(2)).execute();
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
    public void testSaveEachToConnection() throws SQLException {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<TestBean> input = Arrays.asList(new TestBean("John", 25));
        Stream<TestBean> stream = createStream(input);

        List<TestBean> result = stream.saveEach(conn, "INSERT INTO users VALUES (?, ?)", (bean, ps) -> {
            ps.setString(1, bean.getName());
            ps.setInt(2, bean.getAge());
        }).toList();

        assertEquals(input, result);
        verify(stmt).execute();
    }

    @Test
    public void testSaveEachToDataSource() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<TestBean> input = Arrays.asList(new TestBean("John", 25));
        Stream<TestBean> stream = createStream(input);

        List<TestBean> result = stream.saveEach(ds, "INSERT INTO users VALUES (?, ?)", (bean, ps) -> {
            ps.setString(1, bean.getName());
            ps.setInt(2, bean.getAge());
        }).toList();

        assertEquals(input, result);
        verify(stmt).execute();
    }

    @Test
    public void testToListThenApply() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        Integer sum = stream.toListThenApply(list -> list.stream().mapToInt(Integer::intValue).sum());

        assertEquals(15, sum.intValue());
    }

    @Test
    public void testToListThenAccept() {
        List<String> input = Arrays.asList("a", "b", "c");
        List<String> captured = new ArrayList<>();
        Stream<String> stream = createStream(input);

        stream.toListThenAccept(captured::addAll);

        assertEquals(input, captured);
    }

    @Test
    public void testToSetThenApply() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3);
        Stream<Integer> stream = createStream(input);

        int size = stream.toSetThenApply(Set::size);

        assertEquals(3, size);
    }

    @Test
    public void testToSetThenAccept() {
        List<Integer> input = Arrays.asList(1, 2, 2, 3, 3, 3);
        Set<Integer> captured = new HashSet<>();
        Stream<Integer> stream = createStream(input);

        stream.toSetThenAccept(captured::addAll);

        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), captured);
    }

    @Test
    public void testToCollectionThenApply() {
        List<String> input = Arrays.asList("a", "b", "c");
        Stream<String> stream = createStream(input);

        String joined = stream.toCollectionThenApply(LinkedList::new, list -> String.join("-", list));

        assertEquals("a-b-c", joined);
    }

    @Test
    public void testToCollectionThenAccept() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        List<Integer> result = new ArrayList<>();
        Stream<Integer> stream = createStream(input);

        stream.toCollectionThenAccept(TreeSet::new, set -> result.addAll(set));

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testJoinTo() {
        List<String> input = Arrays.asList("a", "b", "c");
        Stream<String> stream = createStream(input);

        Joiner joiner = Joiner.with(", ", "[", "]");
        Joiner result = stream.joinTo(joiner);

        assertEquals("[a, b, c]", result.toString());
    }

    @Test
    public void testAppendOptional() {
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        List<Integer> result1 = stream.append(Optional.of(4)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result1);

        Stream<Integer> stream2 = createStream(input);
        List<Integer> result2 = stream2.append(Optional.<Integer> empty()).toList();
        assertEquals(input, result2);
    }

    @Test
    public void testPrependOptional() {
        List<Integer> input = Arrays.asList(2, 3, 4);
        Stream<Integer> stream = createStream(input);

        List<Integer> result1 = stream.prepend(Optional.of(1)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result1);

        Stream<Integer> stream2 = createStream(input);
        List<Integer> result2 = stream2.prepend(Optional.<Integer> empty()).toList();
        assertEquals(input, result2);
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
    public void testSumLong() {
        List<String> input = Arrays.asList("aa", "bbb", "cccc");
        Stream<String> stream = createStream(input);

        long sum = stream.sumLong(s -> (long) s.length());
        assertEquals(9L, sum);
    }

    @Test
    public void testSumDouble() {
        List<String> input = Arrays.asList("a", "bb", "ccc");
        Stream<String> stream = createStream(input);

        double sum = stream.sumDouble(s -> s.length() * 1.5);
        assertEquals(9.0, sum, 0.001);
    }

    @Test
    public void testAverageLong() {
        List<String> input = Arrays.asList("aa", "bbbb", "cccccc");
        Stream<String> stream = createStream(input);

        OptionalDouble avg = stream.averageLong(s -> (long) s.length());
        assertTrue(avg.isPresent());
        assertEquals(4.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageDouble() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        OptionalDouble avg = stream.averageDouble(i -> i * 2.0);
        assertTrue(avg.isPresent());
        assertEquals(6.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testToDataSetWithColumnNames() {
        List<Object[]> input = new ArrayList<>();
        input.add(new Object[] { "John", 25 });
        input.add(new Object[] { "Jane", 30 });

        Stream<Object[]> stream = createStream(input);
        DataSet dataSet = stream.toDataSet(Arrays.asList("name", "age"));

        assertNotNull(dataSet);
        assertEquals(2, dataSet.size());
        assertEquals(Arrays.asList("name", "age"), dataSet.columnNameList());
    }

    @Test
    public void testRateLimit() {
        RateLimiter rateLimiter = RateLimiter.create(10); // 10 permits per second
        List<Integer> input = Arrays.asList(1, 2, 3);
        Stream<Integer> stream = createStream(input);

        long start = System.currentTimeMillis();
        List<Integer> result = stream.rateLimited(rateLimiter).toList();
        long duration = System.currentTimeMillis() - start;

        assertEquals(input, result);
        assertTrue(duration >= 200); // At least 0.2 seconds for 3 items at 10/sec
    }

    @Test
    public void testDelayWithDuration() {
        List<Integer> input = Arrays.asList(1, 2);
        Stream<Integer> stream = createStream(input);

        long start = System.currentTimeMillis();
        List<Integer> result = stream.delay(Duration.ofMillis(100)).toList();
        long duration = System.currentTimeMillis() - start;

        assertEquals(input, result);
        assertTrue(duration >= 200); // 2 items * 100ms
    }

    @Test
    public void testIntersectionWithMapper() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"), new TestPerson(3, "Bob"));
        List<Integer> ids = Arrays.asList(1, 3, 5);

        Stream<TestPerson> stream = createStream(persons);
        List<TestPerson> result = stream.intersection(TestPerson::getId, ids).toList();

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getName());
        assertEquals("Bob", result.get(1).getName());
    }

    @Test
    public void testDifferenceWithMapper() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"), new TestPerson(3, "Bob"));
        List<Integer> ids = Arrays.asList(1, 3);

        Stream<TestPerson> stream = createStream(persons);
        List<TestPerson> result = stream.difference(TestPerson::getId, ids).toList();

        assertEquals(1, result.size());
        assertEquals("Jane", result.get(0).getName());
    }

    @Test
    public void testSortedByInt() {
        List<String> input = Arrays.asList("ccc", "a", "bb", "dddd");
        Stream<String> stream = createStream(input);

        List<String> result = stream.sortedByInt(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc", "dddd"), result);
    }

    @Test
    public void testSortedByLong() {
        List<String> input = Arrays.asList("ccc", "a", "bb");
        Stream<String> stream = createStream(input);

        List<String> result = stream.sortedByLong(s -> (long) s.length()).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByDouble() {
        List<String> input = Arrays.asList("ccc", "a", "bb");
        Stream<String> stream = createStream(input);

        List<String> result = stream.sortedByDouble(s -> s.length() * 1.0).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testReverseSortedBy() {
        List<String> input = Arrays.asList("a", "ccc", "bb", "dddd");
        Stream<String> stream = createStream(input);

        List<String> result = stream.reverseSortedBy(String::length).toList();
        assertEquals(Arrays.asList("dddd", "ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByInt() {
        List<String> input = Arrays.asList("a", "ccc", "bb");
        Stream<String> stream = createStream(input);

        List<String> result = stream.reverseSortedByInt(String::length).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByLong() {
        List<String> input = Arrays.asList("a", "ccc", "bb");
        Stream<String> stream = createStream(input);

        List<String> result = stream.reverseSortedByLong(s -> (long) s.length()).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByDouble() {
        List<String> input = Arrays.asList("a", "ccc", "bb");
        Stream<String> stream = createStream(input);

        List<String> result = stream.reverseSortedByDouble(s -> s.length() * 1.0).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
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
}
