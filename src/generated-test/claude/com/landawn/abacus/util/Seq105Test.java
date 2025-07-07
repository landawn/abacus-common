package com.landawn.abacus.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.guava.Files;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

public class Seq105Test extends TestBase {

    @TempDir
    File tempDir;

    private File tempFile;
    private StringWriter stringWriter;
    private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = new File(tempDir, "test.txt");
        stringWriter = new StringWriter();
        byteArrayOutputStream = new ByteArrayOutputStream();
    }

    @AfterEach
    public void tearDown() {
        // Clean up resources
        IOUtil.close(stringWriter);
        IOUtil.close(byteArrayOutputStream);
    }

    // Tests for saveEach(Function, OutputStream)
    @Test
    public void testSaveEachFunctionOutputStream() throws Exception {
        List<String> data = Arrays.asList("apple", "banana", "cherry");
        Seq<String, Exception> seq = Seq.of(data);

        List<String> result = seq.saveEach(str -> str.toUpperCase(), byteArrayOutputStream).toList();

        assertEquals(data, result);
        String output = byteArrayOutputStream.toString();
        assertTrue(output.contains("APPLE"));
        assertTrue(output.contains("BANANA"));
        assertTrue(output.contains("CHERRY"));
    }

    @Test
    public void testSaveEachFunctionOutputStreamEmpty() throws Exception {
        Seq<String, Exception> seq = Seq.empty();

        List<String> result = seq.saveEach(str -> str.toUpperCase(), byteArrayOutputStream).toList();

        assertTrue(result.isEmpty());
        assertEquals("", byteArrayOutputStream.toString());
    }

    @Test
    public void testSaveEachFunctionOutputStreamWithNulls() throws Exception {
        List<String> data = Arrays.asList("apple", null, "cherry");
        Seq<String, Exception> seq = Seq.of(data);

        assertThrows(NullPointerException.class, () -> {
            seq.saveEach(str -> str.toUpperCase(), byteArrayOutputStream).toList();
        });
    }

    // Tests for saveEach(Function, Writer)
    @Test
    public void testSaveEachFunctionWriter() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        List<Integer> result = seq.saveEach(i -> "Number: " + i, stringWriter).toList();

        assertEquals(data, result);
        String output = stringWriter.toString();
        assertTrue(output.contains("Number: 1"));
        assertTrue(output.contains("Number: 2"));
        assertTrue(output.contains("Number: 5"));
    }

    @Test
    public void testSaveEachFunctionWriterLargeData() throws Exception {
        int size = 10000;
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            data.add(i);
        }

        Seq<Integer, Exception> seq = Seq.of(data);

        long count = seq.saveEach(i -> "Line " + i, stringWriter).count();

        assertEquals(size, count);
        String output = stringWriter.toString();
        assertTrue(output.contains("Line 0"));
        assertTrue(output.contains("Line " + (size - 1)));
    }

    // Tests for saveEach(BiConsumer, File)
    @Test
    public void testSaveEachBiConsumerFile() throws Exception {
        List<String> data = Arrays.asList("one", "two", "three");
        Seq<String, Exception> seq = Seq.of(data);

        List<String> result = seq.saveEach((str, writer) -> {
            writer.write("Item: ");
            writer.write(str.toUpperCase());
        }, tempFile).toList();

        assertEquals(data, result);

        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(3, lines.size());
        assertEquals("Item: ONE", lines.get(0));
        assertEquals("Item: TWO", lines.get(1));
        assertEquals("Item: THREE", lines.get(2));
    }

    @Test
    public void testSaveEachBiConsumerFileWithException() throws Exception {
        List<String> data = Arrays.asList("one", "two", "three");
        Seq<String, Exception> seq = Seq.of(data);

        assertThrows(RuntimeException.class, () -> {
            seq.saveEach((str, writer) -> {
                if ("two".equals(str)) {
                    throw new IOException("Test exception");
                }
                writer.write(str);
            }, tempFile).toList();
        });
    }

    // Tests for saveEach(BiConsumer, Writer)
    @Test
    public void testSaveEachBiConsumerWriter() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(Map.of("name", "John", "age", 25));
        data.add(Map.of("name", "Jane", "age", 30));

        Seq<Map<String, Object>, Exception> seq = Seq.of(data);

        long count = seq.saveEach((map, writer) -> {
            writer.write(map.get("name") + " is " + map.get("age") + " years old");
        }, stringWriter).count();

        assertEquals(2, count);
        String output = stringWriter.toString();
        assertTrue(output.contains("John is 25 years old"));
        assertTrue(output.contains("Jane is 30 years old"));
    }

    // Tests for saveEach with PreparedStatement
    @Test
    public void testSaveEachPreparedStatement() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);
        List<User> users = Arrays.asList(new User("Alice", 25), new User("Bob", 30));

        Seq<User, Exception> seq = Seq.of(users);

        List<User> result = seq.saveEach(stmt, (user, ps) -> {
            ps.setString(1, user.getName());
            ps.setInt(2, user.getAge());
        }).toList();

        assertEquals(users, result);
        verify(stmt, times(2)).execute();
        verify(stmt, times(2)).setString(anyInt(), anyString());
        verify(stmt, times(2)).setInt(anyInt(), anyInt());
    }

    @Test
    public void testSaveEachPreparedStatementWithBatch() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            users.add(new User("User" + i, 20 + i));
        }

        Seq<User, Exception> seq = Seq.of(users);

        List<User> result = seq.saveEach(stmt, 2, 0, (user, ps) -> {
            ps.setString(1, user.getName());
            ps.setInt(2, user.getAge());
        }).toList();

        assertEquals(users, result);
        verify(stmt, times(5)).addBatch();
        verify(stmt, times(3)).executeBatch(); // 2 + 2 + 1
    }

    // Tests for saveEach with Connection
    @Test
    public void testSaveEachConnection() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<String> data = Arrays.asList("A", "B", "C");
        Seq<String, Exception> seq = Seq.of(data);

        List<String> result = seq.saveEach(conn, "INSERT INTO test VALUES (?)", (str, ps) -> ps.setString(1, str)).toList();

        assertEquals(data, result);
        verify(conn).prepareStatement("INSERT INTO test VALUES (?)");
        verify(stmt, times(3)).execute();
    }

    // Tests for saveEach with DataSource
    @Test
    public void testSaveEachDataSource() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<Integer> data = Arrays.asList(1, 2, 3);
        Seq<Integer, Exception> seq = Seq.of(data);

        long count = seq.saveEach(ds, "INSERT INTO numbers VALUES (?)", (num, ps) -> ps.setInt(1, num)).count();

        assertEquals(3, count);
        verify(ds).getConnection();
        verify(stmt, times(3)).execute();
    }

    // Tests for persist methods
    @Test
    public void testPersistFile() throws Exception {
        List<String> data = Arrays.asList("line1", "line2", "line3");
        Seq<String, Exception> seq = Seq.of(data);

        long count = seq.persist(tempFile);

        assertEquals(3, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(data, lines);
    }

    @Test
    public void testPersistFileWithHeaderAndTail() throws Exception {
        List<String> data = Arrays.asList("data1", "data2");
        Seq<String, Exception> seq = Seq.of(data);

        long count = seq.persist("HEADER", "FOOTER", tempFile);

        assertEquals(2, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals(4, lines.size());
        assertEquals("HEADER", lines.get(0));
        assertEquals("data1", lines.get(1));
        assertEquals("data2", lines.get(2));
        assertEquals("FOOTER", lines.get(3));
    }

    @Test
    public void testPersistWithFunction() throws Exception {
        List<Integer> data = Arrays.asList(10, 20, 30);
        Seq<Integer, Exception> seq = Seq.of(data);

        long count = seq.persist(i -> "Value: " + i, tempFile);

        assertEquals(3, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals("Value: 10", lines.get(0));
        assertEquals("Value: 20", lines.get(1));
        assertEquals("Value: 30", lines.get(2));
    }

    @Test
    public void testPersistOutputStream() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(data);

        long count = seq.persist(s -> s.toUpperCase(), byteArrayOutputStream);

        assertEquals(3, count);
        String output = byteArrayOutputStream.toString();
        String[] lines = output.split(System.lineSeparator());
        assertEquals("A", lines[0]);
        assertEquals("B", lines[1]);
        assertEquals("C", lines[2]);
    }

    @Test
    public void testPersistWriter() throws Exception {
        List<Double> data = Arrays.asList(1.1, 2.2, 3.3);
        Seq<Double, Exception> seq = Seq.of(data);

        long count = seq.persist(d -> String.format("%.2f", d), stringWriter);

        assertEquals(3, count);
        String output = stringWriter.toString();
        assertTrue(output.contains("1.10"));
        assertTrue(output.contains("2.20"));
        assertTrue(output.contains("3.30"));
    }

    // Tests for persist with BiConsumer
    @Test
    public void testPersistBiConsumer() throws Exception {
        List<String> data = Arrays.asList("x", "y", "z");
        Seq<String, Exception> seq = Seq.of(data);

        long count = seq.persist((str, writer) -> {
            writer.write("[" + str + "]");
        }, tempFile);

        assertEquals(3, count);
        List<String> lines = Files.readAllLines(tempFile);
        assertEquals("[x]", lines.get(0));
        assertEquals("[y]", lines.get(1));
        assertEquals("[z]", lines.get(2));
    }

    // Tests for database persist methods
    @Test
    public void testPersistPreparedStatement() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);
        List<String> data = Arrays.asList("A", "B", "C", "D", "E");
        Seq<String, Exception> seq = Seq.of(data);

        long count = seq.persist(stmt, 3, 100, (str, ps) -> ps.setString(1, str));

        assertEquals(5, count);
        verify(stmt, times(5)).addBatch();
        verify(stmt, times(2)).executeBatch(); // batch of 3 + batch of 2
    }

    @Test
    public void testPersistConnection() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<Integer> data = Arrays.asList(100, 200, 300);
        Seq<Integer, Exception> seq = Seq.of(data);

        long count = seq.persist(conn, "INSERT INTO numbers VALUES (?)", 0, 0, (num, ps) -> ps.setInt(1, num));

        assertEquals(3, count);
        verify(stmt, times(3)).execute();
        verify(stmt).close();
    }

    @Test
    public void testPersistDataSource() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<String> data = Arrays.asList("X", "Y");
        Seq<String, Exception> seq = Seq.of(data);

        long count = seq.persist(ds, "INSERT INTO test VALUES (?)", 0, 0, (str, ps) -> ps.setString(1, str));

        assertEquals(2, count);
        verify(stmt, times(2)).execute();
        verify(stmt).close();
        verify(conn).close();
    }

    // Tests for persistToCSV
    @Test
    public void testPersistToCSVFile() throws Exception {
        List<User> users = Arrays.asList(new User("Alice", 25), new User("Bob", 30), new User("Charlie", 35));

        Seq<User, Exception> seq = Seq.of(users);
        File csvFile = new File(tempDir, "users.csv");

        long count = seq.persistToCSV(csvFile);

        assertEquals(3, count);
        List<String> lines = Files.readAllLines(csvFile);
        assertTrue(lines.get(0).contains("name"));
        assertTrue(lines.get(0).contains("age"));
        assertTrue(lines.get(1).contains("Alice"));
        assertTrue(lines.get(1).contains("25"));
    }

    @Test
    public void testPersistToCSVWithHeaders() throws Exception {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(Map.of("id", 1, "value", "A"));
        data.add(Map.of("id", 2, "value", "B"));

        Seq<Map<String, Object>, Exception> seq = Seq.of(data);

        long count = seq.persistToCSV(Arrays.asList("id", "value"), stringWriter);

        assertEquals(2, count);
        String output = stringWriter.toString();
        assertTrue(output.contains("\"id\",\"value\""));
        assertTrue(output.contains("1,\"A\""));
        assertTrue(output.contains("2,\"B\""));
    }

    @Test
    public void testPersistToCSVOutputStream() throws Exception {
        List<Object[]> data = Arrays.asList(new Object[] { "John", 25, true }, new Object[] { "Jane", 30, false });

        Seq<Object[], Exception> seq = Seq.of(data);

        long count = seq.persistToCSV(Arrays.asList("Name", "Age", "Active"), byteArrayOutputStream);

        assertEquals(2, count);
        String output = byteArrayOutputStream.toString();
        assertTrue(output.contains("\"Name\",\"Age\",\"Active\""));
        assertTrue(output.contains("\"John\",25,true"));
        assertTrue(output.contains("\"Jane\",30,false"));
    }

    @Test
    public void testPersistToCSVEmptySequence() throws Exception {
        Seq<Map<String, Object>, Exception> seq = Seq.empty();

        long count = seq.persistToCSV(Arrays.asList("col1", "col2"), stringWriter);

        assertEquals(0, count);
        assertEquals("\"col1\",\"col2\"", stringWriter.toString().trim());
    }

    // Tests for persistToJSON
    @Test
    public void testPersistToJSONFile() throws Exception {
        List<Map<String, Object>> data = Arrays.asList(Map.of("name", "Alice", "age", 25), Map.of("name", "Bob", "age", 30));

        Seq<Map<String, Object>, Exception> seq = Seq.of(data);
        File jsonFile = new File(tempDir, "data.json");

        long count = seq.persistToJSON(jsonFile);

        assertEquals(2, count);
        String content = new String(Files.readAllBytes(jsonFile));
        assertTrue(content.startsWith("["));
        assertTrue(content.endsWith("]"));
        assertTrue(content.contains("\"name\": \"Alice\""));
        assertTrue(content.contains("\"age\": 25"));
    }

    @Test
    public void testPersistToJSONOutputStream() throws Exception {
        List<String> data = Arrays.asList("one", "two", "three");
        Seq<String, Exception> seq = Seq.of(data);

        long count = seq.persistToJSON(byteArrayOutputStream);

        assertEquals(3, count);
        String output = byteArrayOutputStream.toString();
        assertTrue(output.contains("["));
        assertTrue(output.contains("]"));
        assertTrue(output.contains("one"));
        assertTrue(output.contains("two"));
        assertTrue(output.contains("three"));
    }

    @Test
    public void testPersistToJSONWriter() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        long count = seq.persistToJSON(stringWriter);

        assertEquals(5, count);
        String output = stringWriter.toString();
        assertTrue(output.startsWith("["));
        assertTrue(output.endsWith("]"));
        for (int i = 1; i <= 5; i++) {
            assertTrue(output.contains(String.valueOf(i)));
        }
    }

    @Test
    public void testPersistToJSONEmptySequence() throws Exception {
        Seq<String, Exception> seq = Seq.empty();

        long count = seq.persistToJSON(stringWriter);

        assertEquals(0, count);
        assertEquals("[\r\n]", stringWriter.toString().trim());
    }

    @Test
    public void testPersistToJSONLargeData() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            data.add(i);
        }

        Seq<Integer, Exception> seq = Seq.of(data);

        long count = seq.persistToJSON(stringWriter);

        assertEquals(10000, count);
        String output = stringWriter.toString();
        assertTrue(output.contains("0"));
        assertTrue(output.contains("9999"));
    }

    // Test for println
    @Test
    public void testPrintln() throws Exception {
        // Capture System.out
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            List<String> data = Arrays.asList("A", "B", "C");
            Seq<String, Exception> seq = Seq.of(data);

            seq.println();

            String output = outContent.toString();
            assertTrue(output.contains("[A, B, C]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    // Test for cast()
    @Test
    public void testCast() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, IOException> seq = Seq.of(data.iterator(), IOException.class);

        Seq<String, Exception> castedSeq = seq.cast();

        assertNotNull(castedSeq);
        List<String> result = castedSeq.toList();
        assertEquals(data, result);
    }

    @Test
    public void testCastWithClosedSequence() throws Exception {
        Seq<String, IOException> seq = Seq.of(Arrays.asList("a", "b", "c").iterator(), IOException.class);
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.cast());
    }

    // Test for stream()
    @Test
    public void testStream() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        Stream<Integer> stream = seq.stream();

        assertNotNull(stream);
        List<Integer> result = stream.toList();
        assertEquals(data, result);
    }

    @Test
    public void testStreamWithCloseHandlers() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);
        List<String> data = Arrays.asList("x", "y", "z");

        Seq<String, Exception> seq = Seq.of(data).onClose(() -> closeHandlerCalled.set(true));

        Stream<String> stream = seq.stream();
        List<String> result = stream.toList();

        assertEquals(data, result);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testStreamClosedSequence() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c"));
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.stream());
    }

    // Test for transform()
    @Test
    public void testTransform() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<String, Exception> transformed = seq.transform(s -> s.map(i -> "Number: " + i));

        List<String> result = transformed.toList();
        assertEquals(5, result.size());
        assertEquals("Number: 1", result.get(0));
        assertEquals("Number: 5", result.get(4));
    }

    @Test
    public void testTransformWithFilter() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6);
        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transform(s -> s.filter(i -> i % 2 == 0));

        List<Integer> result = transformed.toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testTransformNullFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.transform(null));
    }

    // Test for transformB()
    @Test
    public void testTransformB() throws Exception {
        List<String> data = Arrays.asList("apple", "banana", "cherry");
        Seq<String, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transformB(stream -> stream.map(String::length));

        List<Integer> result = transformed.toList();
        assertEquals(Arrays.asList(5, 6, 6), result);
    }

    @Test
    public void testTransformBWithDeferred() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        AtomicBoolean transformExecuted = new AtomicBoolean(false);

        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transformB(stream -> {
            transformExecuted.set(true);
            return stream.filter(i -> i > 2);
        }, true);

        // Transform should not be executed yet (deferred)
        assertFalse(transformExecuted.get());

        // Now execute
        List<Integer> result = transformed.toList();
        assertTrue(transformExecuted.get());
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testTransformBImmediate() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(data);

        Seq<String, Exception> transformed = seq.transformB(stream -> stream.map(String::toUpperCase), false);

        List<String> result = transformed.toList();
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    // Test for sps()
    @Test
    public void testSps() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> result = seq.sps(stream -> stream.filter(i -> i % 2 == 0).map(i -> i * 2));

        List<Integer> resultList = result.toList();
        assertHaveSameElements(Arrays.asList(4, 8, 12, 16, 20), resultList);
    }

    @Test
    public void testSpsWithMaxThreadNum() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> result = seq.sps(4, stream -> stream.filter(i -> i > 50));

        assertEquals(50, result.count());
    }

    @Test
    public void testSpsInvalidThreadNum() throws Exception {

        assertThrows(IllegalArgumentException.class, () -> Seq.empty().sps(0, stream -> stream));

        assertThrows(IllegalArgumentException.class, () -> Seq.empty().sps(-1, stream -> stream));
    }

    // Test for asyncRun()
    @Test
    public void testAsyncRun() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        List<String> collectedData = new ArrayList<>();

        Seq<String, Exception> seq = Seq.of(data);

        ContinuableFuture<Void> future = seq.asyncRun(s -> s.forEach(collectedData::add));

        future.get(); // Wait for completion
        assertEquals(data, collectedData);
    }

    @Test
    public void testAsyncRunWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
            AtomicInteger sum = new AtomicInteger(0);

            Seq<Integer, Exception> seq = Seq.of(data);

            ContinuableFuture<Void> future = seq.asyncRun(s -> s.forEach(sum::addAndGet), executor);

            future.get();
            assertEquals(15, sum.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncRunNullAction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.asyncRun(null));
    }

    // Test for asyncCall()
    @Test
    public void testAsyncCall() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        ContinuableFuture<Long> future = seq.asyncCall(s -> s.sumInt(e -> e));

        Long result = future.get();
        assertEquals(15, result);
    }

    @Test
    public void testAsyncCallWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<String> data = Arrays.asList("hello", "world");
            Seq<String, Exception> seq = Seq.of(data);

            ContinuableFuture<String> future = seq.asyncCall(s -> s.join(" "), executor);

            String result = future.get();
            assertEquals("hello world", result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncCallReturningList() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        ContinuableFuture<List<Integer>> future = seq.asyncCall(s -> s.filter(i -> i > 2).toList());

        List<Integer> result = future.get();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    // Test for applyIfNotEmpty()
    @Test
    public void testApplyIfNotEmpty() throws Exception {
        List<Integer> data = Arrays.asList(10, 20, 30);
        Seq<Integer, Exception> seq = Seq.of(data);

        u.Optional<Double> result = seq.applyIfNotEmpty(s -> s.averageDouble(e -> e).orElse(0.0));

        assertTrue(result.isPresent());
        assertEquals(20.0, result.get(), 0.001);
    }

    @Test
    public void testApplyIfNotEmptyWithEmptySeq() throws Exception {
        Seq<String, Exception> seq = Seq.empty();

        Optional<String> result = seq.applyIfNotEmpty(s -> s.join(", "));

        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmptyReturningNull() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(data);

        Optional<String> result = seq.applyIfNotEmpty(s -> null);

        assertFalse(result.isPresent());
        // assertNull(result.get());
    }

    @Test
    public void testApplyIfNotEmptyNullFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.applyIfNotEmpty(null));
    }

    // Test for acceptIfNotEmpty()
    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        List<String> data = Arrays.asList("x", "y", "z");
        List<String> collectedData = new ArrayList<>();

        Seq<String, Exception> seq = Seq.of(data);

        OrElse result = seq.acceptIfNotEmpty(s -> s.forEach(collectedData::add));

        // assertTrue(result.isTrue());
        assertEquals(data, collectedData);
    }

    @Test
    public void testAcceptIfNotEmptyWithEmptySeq() throws Exception {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.empty();

        OrElse result = seq.acceptIfNotEmpty(s -> {
            actionExecuted.set(true);
            s.forEach(System.out::println);
        });

        // assertFalse(result.isTrue());
        assertFalse(actionExecuted.get());
    }

    @Test
    public void testAcceptIfNotEmptyWithOrElse() throws Exception {
        AtomicBoolean primaryExecuted = new AtomicBoolean(false);
        AtomicBoolean elseExecuted = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.empty();

        seq.acceptIfNotEmpty(s -> {
            primaryExecuted.set(true);
        }).orElse(() -> {
            elseExecuted.set(true);
        });

        assertFalse(primaryExecuted.get());
        assertTrue(elseExecuted.get());
    }

    // Test for onClose()
    @Test
    public void testOnClose() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);
        List<String> data = Arrays.asList("a", "b", "c");

        Seq<String, Exception> seq = Seq.of(data).onClose(() -> closeCount.incrementAndGet());

        List<String> result = seq.toList();

        assertEquals(data, result);
        assertEquals(1, closeCount.get());
    }

    @Test
    public void testOnCloseMultipleHandlers() throws Exception {
        List<Integer> closeOrder = new ArrayList<>();

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3))
                .onClose(() -> closeOrder.add(1))
                .onClose(() -> closeOrder.add(2))
                .onClose(() -> closeOrder.add(3));

        seq.count();

        assertEquals(Arrays.asList(1, 2, 3), closeOrder);
    }

    @Test
    public void testOnCloseWithNullHandler() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        // Should not throw exception, just return the same sequence
        //    Seq<String, Exception> result = seq.onClose(null);
        //    assertSame(seq, result);
        assertThrows(IllegalArgumentException.class, () -> seq.onClose(null));
    }

    @Test
    public void testOnCloseNotCalledTwice() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("x", "y")).onClose(() -> closeCount.incrementAndGet());

        seq.close();
        seq.close(); // Call close again

        assertEquals(1, closeCount.get());
    }

    // Test for close()
    @Test
    public void testClose() throws Exception {
        AtomicBoolean isClosed = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c")).onClose(() -> isClosed.set(true));

        assertFalse(isClosed.get());

        seq.close();

        assertTrue(isClosed.get());

        // Verify sequence is closed
        assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testCloseIdempotent() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3)).onClose(() -> closeCount.incrementAndGet());

        seq.close();
        seq.close();
        seq.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testAutoCloseOnTerminalOperation() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c")).onClose(() -> closeHandlerCalled.set(true));

        // Terminal operation should trigger close
        long count = seq.count();

        assertEquals(3, count);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testTryWithResources() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);
        List<String> result;

        try (Seq<String, Exception> seq = Seq.of(Arrays.asList("x", "y", "z")).onClose(() -> closeHandlerCalled.set(true))) {
            result = seq.toList();
        }

        assertEquals(Arrays.asList("x", "y", "z"), result);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testCloseWithExceptionInHandler() throws Exception {
        AtomicBoolean firstHandlerCalled = new AtomicBoolean(false);
        AtomicBoolean secondHandlerCalled = new AtomicBoolean(false);

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3)).onClose(() -> {
            firstHandlerCalled.set(true);
            throw new RuntimeException("First handler exception");
        }).onClose(() -> {
            secondHandlerCalled.set(true);
        });

        // Close should execute all handlers even if one throws
        assertThrows(RuntimeException.class, () -> seq.close());

        assertTrue(firstHandlerCalled.get());
        assertTrue(secondHandlerCalled.get());
    }

    // Test helper class
    private static class User {
        private final String name;
        private final int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }
}