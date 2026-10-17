package com.landawn.abacus.util.stream;

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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.guava.Files;
import com.landawn.abacus.util.ByteArrayOutputStream;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.RateLimiter;

/** AbstractStream concurrency contracts. */
public class AbstractStreamConcurrencyTest extends AbstractStreamTestSupport {

    // debounce

    @Test
    public void testDebounce_coldStreamEmitsOnlyLastElement() {
        assertEquals(Arrays.asList(5), createStream(1, 2, 3, 4, 5).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList());
    }

    @Test
    public void testDebounce_emptyEmitsNothing() {
        assertTrue(this.<Integer> createStream().debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList().isEmpty());
    }

    @Test
    public void testDebounce_invalidDurationThrows() {
        assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).debounce((com.landawn.abacus.util.Duration) null).toList());
        assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(0)).toList());
        assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).debounce(com.landawn.abacus.util.Duration.ofMillis(-100)).toList());
    }

    @Test
    public void testDebounce_singleElementSurvives() {
        assertEquals(Arrays.asList(42), createStream(42).debounce(com.landawn.abacus.util.Duration.ofSeconds(1)).toList());
    }

    @Test
    public void testDebounce_slowSourceAllSurviveWhenGapAtLeastDuration() {
        assertEquals(Arrays.asList(1, 2, 3),
                createStream(1, 2, 3).delay(com.landawn.abacus.util.Duration.ofMillis(60)).debounce(com.landawn.abacus.util.Duration.ofMillis(20)).toList());
    }

    // delay

    @Test
    public void testDelay() {
        stream = createStream(1, 2, 3);
        long start = System.currentTimeMillis();
        stream.delay(Duration.ofMillis(100)).toList();
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 300);
    }

    @Test
    public void testDelay_Parallel() {
        List<Integer> result = Stream.of(1, 2, 3).parallel().delay(Duration.ofMillis(0)).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
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

    // persist

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
    public void test_persist_to_writer() throws IOException {
        StringWriter writer = new StringWriter();
        long count = Stream.of("line1", "line2").persist(Object::toString, writer);

        assertEquals(2, count);
        String expected = "line1" + IOUtil.LINE_SEPARATOR_UNIX + "line2" + IOUtil.LINE_SEPARATOR_UNIX;
        assertEquals(expected, writer.toString());
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
    public void testPersist_toWriter() throws IOException {
        StringWriter sw = new StringWriter();
        stream = createStream(1, 2, 3);
        long count = stream.persist(Object::toString, sw);
        assertEquals(3, count);
        assertTrue(sw.toString().contains("1"));
        assertTrue(sw.toString().contains("3"));
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
    public void testPersist_WithHeaderAndTail_BiConsumer_ToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        long count = Stream.of("line1", "line2").persist("START", "END", (s, w) -> w.write(s.toUpperCase()), writer);
        assertEquals(2, count);
        String result = writer.toString();
        assertTrue(result.contains("START"));
        assertTrue(result.contains("END"));
        assertTrue(result.contains("LINE1"));
        assertTrue(result.contains("LINE2"));
    }

    @Test
    public void testPersist_WithHeaderAndTail_ToWriter() throws Exception {
        StringWriter writer = new StringWriter();
        long count = Stream.of("data1", "data2", "data3").persist("HEADER", "TAIL", java.util.function.Function.identity(), writer);
        assertEquals(3, count);
        String result = writer.toString();
        assertTrue(result.contains("HEADER"));
        assertTrue(result.contains("TAIL"));
        assertTrue(result.contains("data1"));
        assertTrue(result.contains("data3"));
    }

    @Test
    public void testPersistNullConnectionMessageNamesConn() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1).persist((Connection) null, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> {
                }));
        assertEquals("'connection' cannot be null", e.getMessage());
    }

    @Test
    public void testPersistNullDataSourceMessageNamesDs() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.of(1).persist((javax.sql.DataSource) null, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> {
                }));
        assertEquals("'dataSource' cannot be null", e.getMessage());
    }

    @Test
    public void testPersistNullStatementMessageNamesStmt() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist((PreparedStatement) null, 1, 0, (i, ps) -> {
        }));
        assertEquals("'statement' cannot be null", e.getMessage());
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
    public void testPersistToConnectionValidatesArgumentsBeforePreparingStatement() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist((Connection) null, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> {
        }));

        Connection conn = mock(Connection.class);
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist(conn, null, 1, 0, (i, ps) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist(conn, "INSERT INTO test VALUES (?)", -1, 0, (i, ps) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist(conn, "INSERT INTO test VALUES (?)", 1, -1, (i, ps) -> {
        }));

        verify(conn, times(0)).prepareStatement(anyString());
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
    public void testPersistToDataSourceValidatesArgumentsBeforeAcquiringConnection() throws SQLException {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist((javax.sql.DataSource) null, "INSERT INTO test VALUES (?)", 1, 0, (i, ps) -> {
        }));

        javax.sql.DataSource ds = mock(javax.sql.DataSource.class);
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist(ds, null, 1, 0, (i, ps) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist(ds, "INSERT INTO test VALUES (?)", -1, 0, (i, ps) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1).persist(ds, "INSERT INTO test VALUES (?)", 1, -1, (i, ps) -> {
        }));

        verify(ds, times(0)).getConnection();
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
    public void testPersistToPreparedStatement_BatchSize1() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Stream<Integer> s = Stream.of(1, 2, 3);
        long count = s.persist(stmt, 1, 0, (i, ps) -> ps.setInt(1, i));
        assertEquals(3, count);
        // batchSize=1 means each element is executed individually (not addBatch)
        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testPersistToPreparedStatement_WithBatchInterval() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Stream<Integer> s = Stream.of(1, 2);
        // batchIntervalInMillis = 0, batchSize = 2
        long count = s.persist(stmt, 2, 0, (i, ps) -> ps.setInt(1, i));
        assertEquals(2, count);
        verify(stmt, times(2)).addBatch();
        verify(stmt, times(1)).executeBatch();
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
    public void testPersistWithHeaderAndTail() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        stringStream = createStream("data1", "data2");
        long count = stringStream.persist("HEADER", "TAIL", tempFile);

        assertEquals(2, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(Arrays.asList("HEADER", "data1", "data2", "TAIL"), lines);
    }

    // G089-01: a pooled writer whose final flush failed is never handed out again with its unwritten characters: the
    // next persist to a non-buffered target writes exactly its own line
    @Test
    public void testPersistWriter_failedFinalFlushLeavesNoStaleCharactersForTheNextPersist() throws IOException {
        for (int i = 0; i < 3; i++) {
            assertThrows(Exception.class, () -> Stream.of("stale").persist(Fn.identity(), g089FailingWriter(true, new AtomicInteger())));
        }

        final StringWriter target = new StringWriter();
        assertEquals(1, Stream.of("fresh").persist(Fn.identity(), target));
        assertEquals("fresh\n", target.toString());
    }

    // G089-01: persist(toLine, Writer) - the final flush is the first write to the target; its IOException must surface
    // (documented "@throws IOException if ... flushing the output fails") instead of being replaced by the
    // UncheckedIOException of the recycle that retries the same buffered write.
    @Test
    public void testPersistWriter_finalFlushFailureThrowsIOException() {
        final AtomicInteger failures = new AtomicInteger();
        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<String> source = Stream.of("a", "b").onClose(closeCount::incrementAndGet);

        final IOException e = assertThrows(IOException.class, () -> source.persist(Fn.identity(), g089FailingWriter(true, failures)));

        assertEquals("write failed #1", e.getMessage());
        assertEquals(1, e.getSuppressed().length);
        assertTrue(e.getSuppressed()[0] instanceof UncheckedIOException);
        assertEquals(1, closeCount.get());

        final AtomicInteger flushFailures = new AtomicInteger();
        final IOException e2 = assertThrows(IOException.class,
                () -> Stream.of("a").persist("H", "T", (s, w) -> w.write(s), g089FailingWriter(false, flushFailures)));
        assertEquals("flush failed #1", e2.getMessage());
    }

    // G089-01: a failure of the write loop (the toLine mapper here) stays the primary failure when the target fails too;
    // the flush and recycle failures are added to it as suppressed, as before
    @Test
    public void testPersistWriter_loopFailureStaysPrimaryWhenTheTargetFailsToo() {
        final AtomicInteger failures = new AtomicInteger();
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Stream.of("a", "b").persist(s -> {
            if ("b".equals(s)) {
                throw new IllegalStateException("mapper failed");
            }

            return s;
        }, g089FailingWriter(true, failures)));

        assertEquals("mapper failed", e.getMessage());
        assertEquals(2, e.getSuppressed().length);
        assertEquals("write failed #1", e.getSuppressed()[0].getMessage());
        assertTrue(e.getSuppressed()[1] instanceof UncheckedIOException);
    }

    // persistToCsv

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
    public void testPersistToCsv_EmptyHeaders() throws IOException {
        stream = Stream.of(1, 2, 3);
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> stream.persistToCsv(Collections.emptyList(), writer));
    }

    @Test
    public void test_persistToCsv_invalidHeader_throwsIAE() throws java.io.IOException {
        // Regression: a header not matching a bean property must throw IllegalArgumentException
        // instead of NullPointerException (#62).
        final java.io.StringWriter writer = new java.io.StringWriter();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> com.landawn.abacus.util.stream.Stream.of(new TestPerson(1, "Alice")).persistToCsv(java.util.List.of("id", "nonexistent"), writer));
    }

    @Test
    public void testPersistToCsv_MapRowsWithoutHeaders() throws Exception {
        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("name", "John");
        row1.put("age", 30);
        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("name", "Jane");
        row2.put("age", 28);

        StringWriter writer = new StringWriter();
        long count = Stream.of(row1, row2).persistToCsv(writer);

        assertEquals(2L, count);
        assertTrue(writer.toString().contains("name"));
        assertTrue(writer.toString().contains("John"));
    }

    // Covers AbstractStream.persistToCsv(Writer) - no-headers path (canCsvHeadersBeEmpty=true)
    @Test
    public void testPersistToCsv_NoHeaders() throws IOException {
        StringWriter writer = new StringWriter();
        Stream<TestBean> beanStream = createStream(new TestBean("Alice", 30), new TestBean("Bob", 25));
        long count = beanStream.persistToCsv(writer);
        assertEquals(2, count);
        String output = writer.toString();
        assertNotNull(output);
        assertFalse(output.isEmpty());
    }

    @Test
    public void test_persistToCsv_unsupportedRowType_throwsIAE() {
        // regression: an unsupported row type threw a bare RuntimeException while the sibling
        // unmatched-header check in the same method throws IllegalArgumentException.
        StringWriter writer = new StringWriter();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2, 3).persistToCsv(List.of("col1"), writer));
        assertTrue(exception.getMessage().contains("Collection/Object[] rows with explicit headers"));
    }

    @Test
    public void testPersistToCsv_WithHeadersAndWriter_Sequential() throws IOException {
        // persistToCsv requires bean/Map types; use Map entries
        java.io.StringWriter writer = new java.io.StringWriter();
        java.util.LinkedHashMap<String, String> row1 = new java.util.LinkedHashMap<>();
        row1.put("col1", "val1");
        row1.put("col2", "val2");
        java.util.LinkedHashMap<String, String> row2 = new java.util.LinkedHashMap<>();
        row2.put("col1", "val3");
        row2.put("col2", "val4");
        long count = Stream.of(row1, row2).persistToCsv(Arrays.asList("col1", "col2"), writer);
        assertEquals(2, count);
        String output = writer.toString();
        assertTrue(output.contains("col1"));
        assertTrue(output.contains("col2"));
    }

    @Test
    public void testPersistToCsv_WithHeadersToWriter() throws Exception {
        // persistToCsv requires bean/Map types
        java.util.LinkedHashMap<String, Object> row1 = new java.util.LinkedHashMap<>();
        row1.put("name", "Alice");
        row1.put("age", 30);
        java.util.LinkedHashMap<String, Object> row2 = new java.util.LinkedHashMap<>();
        row2.put("name", "Bob");
        row2.put("age", 25);

        StringWriter writer = new StringWriter();
        long count = Stream.of(row1, row2).persistToCsv(java.util.Arrays.asList("name", "age"), writer);
        assertEquals(2, count);
        String result = writer.toString();
        assertTrue(result.contains("name"));
        assertTrue(result.contains("Alice"));
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
    public void testPersistToCsvNullFirstRowThrowsIllegalArgumentException() {
        StringWriter writer = new StringWriter();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> Stream.<Object> of((Object) null).persistToCsv(List.of("col1"), writer));
        assertTrue(exception.getMessage().startsWith("null is not supported for CSV format"));
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

    // persistToJson

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

    /** Characters are scalars too, and a null element must be written as JSON {@code null}, not as nothing. */
    @Test
    public void testPersistToJson_charactersAndNulls() throws IOException {
        final StringWriter writer = new StringWriter();
        Stream.of('a', null, 'b').persistToJson(writer);

        assertEquals("[\"a\",null,\"b\"]", writer.toString().replaceAll("\\s", ""));
        assertEquals(Arrays.asList("a", null, "b"), N.fromJson(writer.toString(), List.class));
    }

    /**
     * {@code persistToJson} must write each element as a JSON <i>value</i>, quoted and escaped.
     *
     * <p>It used to call {@code N.toJson(element, writer)}, the <i>root document</i> entry point, whose
     * root-scalar shortcut writes a String unquoted. Inside the array that produced text the library's own
     * parser rejects: {@code Stream.of("A","B","C")} wrote {@code [\nA,\nB,\nC\n]} while the javadoc
     * example promised {@code ["A", "B", "C"]}.
     */
    @Test
    public void testPersistToJson_quotesAndEscapesScalarElements() throws IOException {
        final StringWriter writer = new StringWriter();
        final long count = Stream.of("A", "B", "C").persistToJson(writer);

        assertEquals(3, count);
        assertEquals("[\"A\",\"B\",\"C\"]", writer.toString().replaceAll("\\s", ""));
    }

    /**
     * A quote, a comma, a bracket or a {@code null} element must survive a round trip. Before the fix a null
     * element vanished entirely and any of the others made the document unparseable.
     */
    @Test
    public void testPersistToJson_roundTripsAwkwardScalarElements() throws IOException {
        final List<String> elements = Arrays.asList("he said \"hi\"", "a,b", null, "]", "{\"k\":1}");

        final StringWriter writer = new StringWriter();
        assertEquals(elements.size(), Stream.of(elements).persistToJson(writer));

        final List<?> readBack = N.fromJson(writer.toString(), List.class);
        assertEquals(elements, readBack);
    }

    /** Numbers, Maps and beans were already correct and must stay byte-identical. */
    @Test
    public void testPersistToJson_structuredElementsUnchanged() throws IOException {
        final StringWriter ints = new StringWriter();
        Stream.of(1, 2, 3).persistToJson(ints);
        assertEquals("[1,2,3]", ints.toString().replaceAll("\\s", ""));

        final StringWriter maps = new StringWriter();
        Stream.of(Map.of("key", "val1")).persistToJson(maps);
        assertEquals("[{\"key\":\"val1\"}]", maps.toString().replaceAll("\\s", ""));
    }

    // G089-01: persistToJson/persistToCsv(.., OutputStream) end in the Writer terminal's final flush too, so a failing flush
    // of the target is the documented IOException (it was the UncheckedIOException of the recycle's retry); the toLine
    // OutputStream overload, which already threw it, is unchanged
    @Test
    public void testPersistToJsonAndCsvOutputStream_finalFlushFailureThrowsIOException() {
        final AtomicInteger jsonFailures = new AtomicInteger();
        final IOException jsonFailure = assertThrows(IOException.class, () -> Stream.of(1, 2).persistToJson(g122FlushFailingStream(jsonFailures)));
        assertEquals("flush failed #1", jsonFailure.getMessage());

        final AtomicInteger csvFailures = new AtomicInteger();
        final IOException csvFailure = assertThrows(IOException.class,
                () -> Stream.of(List.of(List.of(1, 2))).persistToCsv(List.of("a", "b"), g122FlushFailingStream(csvFailures)));
        assertEquals("flush failed #1", csvFailure.getMessage());

        final AtomicInteger lineFailures = new AtomicInteger();
        final IOException lineFailure = assertThrows(IOException.class, () -> Stream.of("a").persist(Fn.identity(), g122FlushFailingStream(lineFailures)));
        assertEquals("flush failed #1", lineFailure.getMessage());
    }

    // G089-01: same for persistToJson(Writer) and persistToCsv(.., Writer), which share the final flush/recycle step.
    @Test
    public void testPersistToJsonAndCsvWriter_finalFlushFailureThrowsIOException() {
        final AtomicInteger jsonFailures = new AtomicInteger();
        final IOException jsonFailure = assertThrows(IOException.class, () -> Stream.of(1, 2).persistToJson(g089FailingWriter(true, jsonFailures)));
        assertEquals("write failed #1", jsonFailure.getMessage());

        final AtomicInteger csvFailures = new AtomicInteger();
        final IOException csvFailure = assertThrows(IOException.class,
                () -> Stream.of(List.of(List.of(1, 2))).persistToCsv(List.of("a", "b"), g089FailingWriter(true, csvFailures)));
        assertEquals("write failed #1", csvFailure.getMessage());

        final AtomicInteger csvFlushFailures = new AtomicInteger();
        final IOException csvFlushFailure = assertThrows(IOException.class,
                () -> Stream.of(List.of(Map.of("k", 1))).persistToCsv(g089FailingWriter(false, csvFlushFailures)));
        assertEquals("flush failed #1", csvFlushFailure.getMessage());
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
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(2);
        stream = createStream(1, 2, 3, 4);
        long start = System.currentTimeMillis();
        stream.rateLimited(rateLimiter).toList();
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 1000);
    }

    @Test
    public void testRateLimited_Parallel() {
        RateLimiter rateLimiter = RateLimiter.create(1000);
        List<Integer> result = Stream.of(1, 2, 3, 4, 5).parallel().rateLimited(rateLimiter).sorted().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

}
