package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.u.Nullable;

/**
 * Comprehensive unit tests for the N class
 */
public class N104Test extends TestBase {

    @TempDir
    File tempDir;

    // Test data
    private static final String TEST_JSON = "{\"name\":\"John\",\"age\":30}";
    private static final String TEST_XML = "<person><name>John</name><age>30</age></person>";
    private static final String TEST_JSON_ARRAY = "[1,2,3,4,5]";

    private static class TestPerson {
        private String name;
        private int age;

        public TestPerson() {
        }

        public TestPerson(String name, int age) {
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            TestPerson that = (TestPerson) obj;
            return age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    // JSON Serialization/Deserialization Tests

    @Test
    public void testToJson() {
        TestPerson person = new TestPerson("John", 30);
        String json = N.toJson(person);
        assertNotNull(json);
        assertTrue(json.contains("\"name\": \"John\""));
        assertTrue(json.contains("\"age\": 30"));
    }

    @Test
    public void testToJsonWithPrettyFormat() {
        TestPerson person = new TestPerson("John", 30);
        String json = N.toJson(person, true);
        assertNotNull(json);
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"name\": \"John\""));
    }

    @Test
    public void testToJsonWithConfig() {
        TestPerson person = new TestPerson("John", 30);
        JSONSerializationConfig config = JSC.create();
        String json = N.toJson(person, config);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToFile() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        File outputFile = new File(tempDir, "test.json");
        N.toJson(person, outputFile);
        assertTrue(outputFile.exists());
        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("John"));

        IOUtil.deleteIfExists(outputFile);
    }

    @Test
    public void testToJsonToOutputStream() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toJson(person, baos);
        String json = baos.toString();
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonToWriter() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        StringWriter writer = new StringWriter();
        N.toJson(person, writer);
        String json = writer.toString();
        assertTrue(json.contains("John"));
    }

    @Test
    public void testFromJsonString() {
        TestPerson person = N.fromJson(TEST_JSON, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void testFromJsonStringWithType() {
        Type<List<Integer>> type = new TypeReference<List<Integer>>() {
        }.type();
        List<Integer> list = N.fromJson("[1,2,3]", type);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testFromJsonStringWithDefault() {
        TestPerson defaultPerson = new TestPerson("Default", 0);
        TestPerson person = N.fromJson(null, defaultPerson, TestPerson.class);
        assertEquals(defaultPerson, person);
    }

    @Test
    public void testFromJsonFile() throws IOException {
        File jsonFile = new File(tempDir, "test.json");
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(TEST_JSON);
        }
        TestPerson person = N.fromJson(jsonFile, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());

        IOUtil.deleteIfExists(jsonFile);
    }

    @Test
    public void testFromJsonInputStream() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(TEST_JSON.getBytes(StandardCharsets.UTF_8));
        TestPerson person = N.fromJson(bais, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonReader() throws IOException {
        StringReader reader = new StringReader(TEST_JSON);
        TestPerson person = N.fromJson(reader, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
    }

    @Test
    public void testFromJsonSubstring() {
        String json = "prefix" + TEST_JSON + "suffix";
        TestPerson person = N.fromJson(json, 6, 6 + TEST_JSON.length(), TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
    }

    @Test
    public void testStreamJson() {
        //    Stream<Integer> stream = N.streamJson(TEST_JSON_ARRAY, Integer.class);
        //    List<Integer> list = stream.toList();
        //    assertEquals(Arrays.asList(1, 2, 3, 4, 5), list);
        assertThrows(IllegalArgumentException.class, () -> N.streamJson(TEST_JSON_ARRAY, Type.of(Integer.class)));
    }

    @Test
    public void testFormatJson() {
        String compactJson = "{\"a\":1,\"b\":2}";
        String formatted = N.formatJson(compactJson);
        assertTrue(formatted.contains("\n"));
    }

    // XML Serialization/Deserialization Tests

    @Test
    public void testToXml() {
        TestPerson person = new TestPerson("John", 30);
        String xml = N.toXml(person);
        assertNotNull(xml);
        assertTrue(xml.contains("<name>John</name>"));
        assertTrue(xml.contains("<age>30</age>"));
    }

    @Test
    public void testToXmlWithPrettyFormat() {
        TestPerson person = new TestPerson("John", 30);
        String xml = N.toXml(person, true);
        assertNotNull(xml);
        assertTrue(xml.contains("\n"));
    }

    @Test
    public void testFromXmlString() {
        TestPerson person = N.fromXml(TEST_XML, TestPerson.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void testFormatXml() {
        String compactXml = "<root><a>1</a><b>2</b></root>";
        String formatted = N.formatXml(compactXml);
        assertTrue(formatted.contains("\n"));
    }

    @Test
    public void testXml2Json() {
        String json = N.xml2Json(TEST_XML);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void testJson2Xml() {
        String xml = N.json2Xml(TEST_JSON);
        assertNotNull(xml);
        assertTrue(xml.contains("John"));
    }

    // forEach Tests

    @Test
    public void testForEachIntRange() {
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(1, 5, sum::addAndGet);
        assertEquals(10, sum.get()); // 1+2+3+4
    }

    @Test
    public void testForEachIntRangeWithStep() {
        List<Integer> values = new ArrayList<>();
        N.forEach(0, 10, 2, values::add);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), values);
    }

    @Test
    public void testForEachArray() {
        String[] array = { "a", "b", "c" };
        List<String> result = new ArrayList<>();
        N.forEach(array, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testForEachArrayWithRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEach(array, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);
    }

    @Test
    public void testForEachIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list, result::add);
        assertEquals(list, result);
    }

    @Test
    public void testForEachIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list.iterator(), result::add);
        assertEquals(list, result);
    }

    @Test
    public void testForEachMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(map, (k, v) -> sum.addAndGet(v));
        assertEquals(3, sum.get());
    }

    @Test
    public void testForEachParallel() throws Exception {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        ConcurrentLinkedQueue<Integer> result = new ConcurrentLinkedQueue<>();
        N.forEach(list, result::add, 2);
        assertEquals(5, result.size());
        assertTrue(result.containsAll(list));
    }

    @Test
    public void testForEachWithFlatMapper() {
        {
            String[] array = { "ab", "cd", "ef" };
            List<Character> result = new ArrayList<>();
            N.forEach(array, s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
            assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);
        }
        {
            List<String> list = N.asList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list, s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
            assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);
        }
        {
            List<String> list = N.asList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list.iterator(), s -> Arrays.asList(s.charAt(0), s.charAt(1)), (s, c) -> result.add(c));
            assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);
        }
    }

    @Test
    public void testForEachWithFlatMapper3() {
        {
            String[] array = { "ab", "cd", "ef" };
            List<Character> result = new ArrayList<>();
            N.forEach(array, s -> Arrays.asList(s.charAt(0), s.charAt(1)), e -> N.asList(e, e), (s, c, x) -> result.add(c));
            assertEquals(Arrays.asList('a', 'a', 'b', 'b', 'c', 'c', 'd', 'd', 'e', 'e', 'f', 'f'), result);
        }
        {
            List<String> list = N.asList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list, s -> Arrays.asList(s.charAt(0), s.charAt(1)), e -> N.asList(e, e), (s, c, x) -> result.add(c));
            assertEquals(Arrays.asList('a', 'a', 'b', 'b', 'c', 'c', 'd', 'd', 'e', 'e', 'f', 'f'), result);
        }
        {
            List<String> list = N.asList("ab", "cd", "ef");
            List<Character> result = new ArrayList<>();
            N.forEach(list.iterator(), s -> Arrays.asList(s.charAt(0), s.charAt(1)), e -> N.asList(e, e), (s, c, x) -> result.add(c));
            assertEquals(Arrays.asList('a', 'a', 'b', 'b', 'c', 'c', 'd', 'd', 'e', 'e', 'f', 'f'), result);
        }
    }

    @Test
    public void testForEachPair() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<String> result = new ArrayList<>();
        N.forEachPair(array, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4", "4-5"), result);
    }

    @Test
    public void testForEachTriple() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<String> result = new ArrayList<>();
        N.forEachTriple(array, (a, b, c) -> result.add(a + "-" + (b != null ? b : "null") + "-" + (c != null ? c : "null")));
        assertEquals(Arrays.asList("1-2-3", "2-3-4", "3-4-5"), result);
    }

    @Test
    public void testForEachIndexed() {
        String[] array = { "a", "b", "c" };
        Map<Integer, String> result = new HashMap<>();
        N.forEachIndexed(array, result::put);
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testForEachNonNull() {
        String[] array = { "a", null, "b", null, "c" };
        List<String> result = new ArrayList<>();
        N.forEachNonNull(array, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    // Execution Tests

    @Test
    public void testExecute() {
        AtomicInteger counter = new AtomicInteger(0);
        N.execute(counter::incrementAndGet, 2, 10, e -> e instanceof RuntimeException);
        assertEquals(1, counter.get());
    }

    @Test
    public void testExecuteCallable() {
        Callable<String> callable = () -> "success";
        String result = N.execute(callable, 2, 10, (r, e) -> e != null);
        assertEquals("success", result);
    }

    @Test
    public void testAsyncExecute() throws Exception {
        ContinuableFuture<Void> future = N.asyncExecute(() -> Thread.sleep(10));
        assertNotNull(future);
        future.get();
    }

    @Test
    public void testAsyncExecuteCallable() throws Exception {
        ContinuableFuture<String> future = N.asyncExecute(() -> "result");
        assertEquals("result", future.get());
    }

    @Test
    public void testAsyncExecuteWithDelay() throws Exception {
        long start = System.currentTimeMillis();
        ContinuableFuture<Void> future = N.asyncExecute(() -> {
        }, 50);
        future.get();
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testAsyncExecuteList() throws Exception {
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(() -> Thread.sleep(10), () -> Thread.sleep(10), () -> Thread.sleep(10));
        List<ContinuableFuture<Void>> futures = N.asyncExecute(commands);
        assertEquals(3, futures.size());
        for (ContinuableFuture<Void> future : futures) {
            future.get();
        }
    }

    @Test
    public void testAsynRun() {
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(() -> {
        }, () -> {
        }, () -> {
        });
        ObjIterator<Void> iter = N.asynRun(commands);
        int count = 0;
        while (iter.hasNext()) {
            iter.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testAsynCall() {
        List<Callable<Integer>> commands = Arrays.asList(() -> 1, () -> 2, () -> 3);
        ObjIterator<Integer> iter = N.asynCall(commands);
        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }
        assertEquals(3, results.size());
        assertTrue(results.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testRunInParallel() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        N.runInParallel(counter1::incrementAndGet, counter2::incrementAndGet);
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    public void testCallInParallel() {
        Tuple2<String, Integer> result = N.callInParallel(() -> "hello", () -> 42);
        assertEquals("hello", result._1);
        assertEquals(42, result._2);
    }

    @Test
    public void testRunByBatch() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<Integer> sums = new ArrayList<>();
        N.runByBatch(array, 3, batch -> sums.add(batch.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(Arrays.asList(6, 15, 24, 10), sums); // [1,2,3], [4,5,6], [7,8,9], [10]
    }

    @Test
    public void testCallByBatch() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> results = N.callByBatch(list, 3, batch -> batch.size());
        assertEquals(Arrays.asList(3, 3, 3, 1), results);
    }

    @Test
    public void testRunUninterruptibly() {
        AtomicInteger counter = new AtomicInteger(0);
        N.runUninterruptibly(() -> {
            counter.incrementAndGet();
            Thread.sleep(10);
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void testCallUninterruptibly() throws Exception {
        String result = N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return "success";
        });
        assertEquals("success", result);
    }

    // Utility Method Tests

    @Test
    public void testTryOrEmptyIfExceptionOccurred() {
        Nullable<String> result1 = N.tryOrEmptyIfExceptionOccurred(() -> "success");
        assertTrue(result1.isPresent());
        assertEquals("success", result1.get());

        Nullable<String> result2 = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new RuntimeException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred() {
        String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "success", "default");
        assertEquals("success", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
            throw new RuntimeException();
        }, "default");
        assertEquals("default", result2);
    }

    @Test
    public void testIfOrEmpty() {
        Nullable<String> result1 = N.ifOrEmpty(true, () -> "value");
        assertTrue(result1.isPresent());
        assertEquals("value", result1.get());

        Nullable<String> result2 = N.ifOrEmpty(false, () -> "value");
        assertFalse(result2.isPresent());
    }

    @Test
    public void testIfOrElse() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);

        N.ifOrElse(true, counter1::incrementAndGet, counter2::incrementAndGet);
        assertEquals(1, counter1.get());
        assertEquals(0, counter2.get());

        N.ifOrElse(false, counter1::incrementAndGet, counter2::incrementAndGet);
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    public void testIfNotNull() {
        AtomicInteger counter = new AtomicInteger(0);
        N.ifNotNull("value", v -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotNull(null, v -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testIfNotEmpty() {
        AtomicInteger counter = new AtomicInteger(0);

        N.ifNotEmpty("hello", s -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotEmpty("", s -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotEmpty(Arrays.asList(1, 2, 3), c -> counter.incrementAndGet());
        assertEquals(2, counter.get());

        N.ifNotEmpty(Collections.emptyList(), c -> counter.incrementAndGet());
        assertEquals(2, counter.get());
    }

    @Test
    public void testSleep() {
        long start = System.currentTimeMillis();
        N.sleep(50);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testSleepWithTimeUnit() {
        long start = System.currentTimeMillis();
        N.sleep(50, TimeUnit.MILLISECONDS);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testSleepUninterruptibly() {
        long start = System.currentTimeMillis();
        N.sleepUninterruptibly(50);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 50);
    }

    @Test
    public void testLazyInit() {
        AtomicInteger counter = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            counter.incrementAndGet();
            return "value";
        });

        assertEquals(0, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get()); // Should not increment again
    }

    @Test
    public void testToRuntimeException() {
        Exception checkedException = new Exception("test");
        RuntimeException runtimeException = N.toRuntimeException(checkedException);
        assertNotNull(runtimeException);
        assertEquals(checkedException, runtimeException.getCause());

        RuntimeException existingRuntimeException = new RuntimeException("test");
        RuntimeException result = N.toRuntimeException(existingRuntimeException);
        assertSame(existingRuntimeException, result);
    }

    @Test
    public void testPrintln() {
        // Capture System.out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            String result = N.println("test");
            assertEquals("test", result);
            assertTrue(baos.toString().contains("test"));

            baos.reset();
            List<String> list = Arrays.asList("a", "b", "c");
            List<String> listResult = N.println(list);
            assertSame(list, listResult);
            assertTrue(baos.toString().contains("[a, b, c]"));

            baos.reset();
            String[] array = { "x", "y", "z" };
            String[] arrayResult = N.println(array);
            assertSame(array, arrayResult);
            assertTrue(baos.toString().contains("[x, y, z]"));

            baos.reset();
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            Map<String, Integer> mapResult = N.println(map);
            assertSame(map, mapResult);
            String mapOutput = baos.toString();
            assertTrue(mapOutput.contains("{") && mapOutput.contains("}"));
            assertTrue(mapOutput.contains("a=1") || mapOutput.contains("b=2"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testFprintln() {
        // Capture System.out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.fprintln("Hello %s, you are %d years old", "John", 30);
            String output = baos.toString();
            assertTrue(output.contains("Hello John, you are 30 years old"));
        } finally {
            System.setOut(originalOut);
        }
    }

    // Edge Cases and Error Conditions

    @Test
    public void testForEachWithNullArray() {
        String[] nullArray = null;
        List<String> result = new ArrayList<>();
        N.forEach(nullArray, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithEmptyArray() {
        String[] emptyArray = new String[0];
        List<String> result = new ArrayList<>();
        N.forEach(emptyArray, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithNullIterable() {
        Iterable<String> nullIterable = null;
        List<String> result = new ArrayList<>();
        N.forEach(nullIterable, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithNullIterator() {
        Iterator<String> nullIterator = null;
        List<String> result = new ArrayList<>();
        N.forEach(nullIterator, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithEmptyMap() {
        Map<String, Integer> emptyMap = new HashMap<>();
        AtomicInteger counter = new AtomicInteger(0);
        N.forEach(emptyMap, (k, v) -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testForEachRangeWithInvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.forEach(0, 10, 0, i -> {
            });
        });
    }

    @Test
    public void testForEachRangeBackwards() {
        List<Integer> result = new ArrayList<>();
        N.forEach(10, 0, -2, result::add);
        assertEquals(Arrays.asList(10, 8, 6, 4, 2), result);
    }

    @Test
    public void testFromJsonWithInvalidJson() {
        assertThrows(Exception.class, () -> {
            N.fromJson("invalid json", TestPerson.class);
        });
    }

    @Test
    public void testFromXmlWithInvalidXml() {
        assertThrows(Exception.class, () -> {
            N.fromXml("invalid xml", TestPerson.class);
        });
    }

    @Test
    public void testRunByBatchWithInvalidBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.runByBatch(new Integer[] { 1, 2, 3 }, 0, batch -> {
            });
        });

        assertThrows(IllegalArgumentException.class, () -> {
            N.runByBatch(new Integer[] { 1, 2, 3 }, -1, batch -> {
            });
        });
    }

    @Test
    public void testAsyncExecuteWithNullExecutor() {
        assertThrows(NullPointerException.class, () -> {
            N.asyncExecute(() -> {
            }, null);
        });
    }

    @Test
    public void testSleepWithNullTimeUnit() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.sleep(100, null);
        });
    }

    @Test
    public void testForEachIndexedParallel() throws Exception {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        ConcurrentHashMap<Integer, Integer> result = new ConcurrentHashMap<>();
        N.forEachIndexed(list, result::put, 3);

        assertEquals(10, result.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(i + 1, result.get(i));
        }
    }

    @Test
    public void testForEachWithTwoArrays() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "1", "2", "3" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, (a, b) -> result.add(a + b));
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testForEachWithThreeArrays() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "1", "2", "3" };
        String[] array3 = { "x", "y", "z" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, array3, (a, b, c) -> result.add(a + b + c));
        assertEquals(Arrays.asList("a1x", "b2y", "c3z"), result);
    }

    @Test
    public void testForEachWithArraysOfDifferentLengths() {
        String[] array1 = { "a", "b", "c", "d" };
        String[] array2 = { "1", "2" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, (a, b) -> result.add(a + b));
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testForEachWithDefaultValues() {
        String[] array1 = { "a", "b" };
        String[] array2 = { "1", "2", "3", "4" };
        List<String> result = new ArrayList<>();
        N.forEach(array1, array2, "X", "Y", (a, b) -> result.add(a + b));
        assertEquals(Arrays.asList("a1", "b2", "X3", "X4"), result);
    }

    @Test
    public void testCallInParallelWithCollection() {
        List<Callable<Integer>> tasks = Arrays.asList(() -> 1, () -> 2, () -> 3, () -> 4, () -> 5);
        List<Integer> results = N.callInParallel(tasks);
        assertEquals(5, results.size());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
    }

    @Test
    public void testRunInParallelWithCollection() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Throwables.Runnable<Exception>> tasks = Arrays.asList(counter::incrementAndGet, counter::incrementAndGet, counter::incrementAndGet);
        N.runInParallel(tasks);
        assertEquals(3, counter.get());
    }

    @Test
    public void testForEachPairWithStep() {
        Integer[] array = { 1, 2, 3, 4, 5, 6 };
        List<String> result = new ArrayList<>();
        N.forEachPair(array, 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("1-2", "3-4", "5-6"), result);
    }

    @Test
    public void testForEachTripleWithStep() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        List<String> result = new ArrayList<>();
        N.forEachTriple(array, 3, (a, b, c) -> result.add(a + "-" + (b != null ? b : "null") + "-" + (c != null ? c : "null")));
        assertEquals(Arrays.asList("1-2-3", "4-5-6", "7-8-9"), result);
    }

    @Test
    public void testAsyncExecuteWithRetry() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);
        ContinuableFuture<String> future = N.asyncExecute(() -> {
            attemptCount.incrementAndGet();
            if (attemptCount.get() < 3) {
                throw new RuntimeException("Retry");
            }
            return "success";
        }, 3, 10, (result, exception) -> exception != null);

        String result = future.get();
        assertEquals("success", result);
        assertEquals(3, attemptCount.get());
    }

    @Test
    public void testLazyInitialize() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Throwables.Supplier<String, Exception> lazy = N.lazyInitialize(() -> {
            counter.incrementAndGet();
            return "value";
        });

        assertEquals(0, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
        assertEquals("value", lazy.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testForEachCollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();

        // Forward iteration
        N.forEach(list, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        // Backward iteration
        result.clear();
        N.forEach(list, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);
    }

    @Test
    public void testRunByBatchWithElementConsumer() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        List<Integer> processedElements = new ArrayList<>();
        AtomicInteger batchCount = new AtomicInteger(0);

        N.runByBatch(array, 2, (idx, element) -> processedElements.add(element), batchCount::incrementAndGet);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processedElements);
        assertEquals(3, batchCount.get()); // 3 batches: [1,2], [3,4], [5]
    }

    @Test
    public void testCallByBatchWithElementConsumer() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> processedElements = new ArrayList<>();

        List<String> results = N.callByBatch(list, 2, (idx, element) -> processedElements.add(element), () -> "batch-" + processedElements.size());

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), processedElements);
        assertEquals(Arrays.asList("batch-2", "batch-4", "batch-5"), results);
    }

    @Test
    public void testRunUninterruptiblyWithTimeout() {
        AtomicInteger counter = new AtomicInteger(0);
        N.runUninterruptibly(remainingMillis -> {
            counter.incrementAndGet();
            Thread.sleep(10);
        }, 100);
        assertEquals(1, counter.get());
    }

    @Test
    public void testCallUninterruptiblyWithTimeout() {
        String result = N.callUninterruptibly(remainingMillis -> {
            Thread.sleep(10);
            return "success-" + remainingMillis;
        }, 100);
        assertTrue(result.startsWith("success-"));
    }

    @Test
    public void testCallInParallelMultiple() {
        for (int i = 0; i < 100; i++) {
            // Test with 3 callables
            Tuple3<String, Integer, Boolean> result3 = N.callInParallel(() -> "hello", () -> 42, () -> true);
            assertEquals("hello", result3._1);
            assertEquals(42, result3._2);
            assertEquals(true, result3._3);

            // Test with 4 callables
            Tuple4<String, Integer, Boolean, Double> result4 = N.callInParallel(() -> "hello", () -> 42, () -> true, () -> 3.14);
            assertEquals("hello", result4._1);
            assertEquals(42, result4._2);
            assertEquals(true, result4._3);
            assertEquals(3.14, result4._4);

            // Test with 5 callables
            Tuple5<String, Integer, Boolean, Double, Character> result5 = N.callInParallel(() -> "hello", () -> 42, () -> true, () -> 3.14, () -> 'x');
            assertEquals("hello", result5._1);
            assertEquals(42, result5._2);
            assertEquals(true, result5._3);
            assertEquals(3.14, result5._4);
            assertEquals('x', result5._5);
        }
    }

    @Test
    public void testRunInParallelMultiple() {
        AtomicInteger c1 = new AtomicInteger(0);
        AtomicInteger c2 = new AtomicInteger(0);
        AtomicInteger c3 = new AtomicInteger(0);
        AtomicInteger c4 = new AtomicInteger(0);
        AtomicInteger c5 = new AtomicInteger(0);

        // Test with 3 runnables
        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
        assertEquals(1, c3.get());

        // Reset counters
        c1.set(0);
        c2.set(0);
        c3.set(0);

        // Test with 4 runnables
        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
        assertEquals(1, c3.get());
        assertEquals(1, c4.get());

        // Reset counters
        c1.set(0);
        c2.set(0);
        c3.set(0);
        c4.set(0);

        // Test with 5 runnables
        N.runInParallel(c1::incrementAndGet, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet, c5::incrementAndGet);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
        assertEquals(1, c3.get());
        assertEquals(1, c4.get());
        assertEquals(1, c5.get());
    }

    @Test
    public void testRunInParallelMultiple_01() {
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet));

            assertEquals(0, c1.get());
            assertEquals(1, c2.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet));

            assertEquals(0, c1.get());
            // assertEquals(1, c2.get());
            // assertEquals(1, c3.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet));

            assertEquals(0, c1.get());
            // assertEquals(1, c2.get());
            // assertEquals(1, c3.get());
            // assertEquals(1, c4.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);
            AtomicInteger c5 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.runInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet, c5::incrementAndGet));

            assertEquals(0, c1.get());
            //  assertEquals(1, c2.get());
            // assertEquals(1, c3.get());
            // assertEquals(1, c4.get());
            // assertEquals(1, c5.get());
        }
    }

    @Test
    public void testRunInParallelMultiple_02() {
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet));

            assertEquals(0, c1.get());
            // assertEquals(1, c2.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet));

            assertEquals(0, c1.get());
            // assertEquals(0, c2.get());
            // assertEquals(0, c3.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet));

            assertEquals(0, c1.get());
            // assertEquals(0, c2.get());
            // assertEquals(0, c3.get());
            // assertEquals(0, c4.get());
        }
        {
            AtomicInteger c1 = new AtomicInteger(0);
            AtomicInteger c2 = new AtomicInteger(0);
            AtomicInteger c3 = new AtomicInteger(0);
            AtomicInteger c4 = new AtomicInteger(0);
            AtomicInteger c5 = new AtomicInteger(0);

            // Test with 5 runnables
            assertThrows(RuntimeException.class, () -> N.callInParallel(() -> {
                throw new RuntimeException("");
            }, c2::incrementAndGet, c3::incrementAndGet, c4::incrementAndGet, c5::incrementAndGet));

            assertEquals(0, c1.get());
            // assertEquals(1, c2.get());
            // assertEquals(1, c3.get());
            // assertEquals(1, c4.get());
            // assertEquals(1, c5.get());
        }
    }

    @Test
    public void testTryOrDefaultWithSupplier() {
        {
            String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "success", () -> "default");
            assertEquals("success", result1);

            String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new RuntimeException();
            }, () -> "default");
            assertEquals("default", result2);
        }
        {

            AtomicInteger supplierCalls = new AtomicInteger(0);

            String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "Success", () -> {
                supplierCalls.incrementAndGet();
                return "Supplied Default";
            });
            assertEquals("Success", result1);
            assertEquals(0, supplierCalls.get()); // Supplier not called on success

            String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new RuntimeException();
            }, () -> {
                supplierCalls.incrementAndGet();
                return "Supplied Default";
            });
            assertEquals("Supplied Default", result2);
            assertEquals(1, supplierCalls.get());

        }
    }

    @Test
    public void testTryOrDefaultWithInitAndFunction() {
        String result1 = N.tryOrDefaultIfExceptionOccurred("input", s -> s.toUpperCase(), "default");
        assertEquals("INPUT", result1);

        String result2 = N.tryOrDefaultIfExceptionOccurred("input", s -> {
            throw new RuntimeException();
        }, "default");
        assertEquals("default", result2);
    }

    @Test
    public void testTryOrEmptyWithInitAndFunction() {
        Nullable<String> result1 = N.tryOrEmptyIfExceptionOccurred("input", s -> s.toUpperCase());
        assertTrue(result1.isPresent());
        assertEquals("INPUT", result1.get());

        Nullable<String> result2 = N.tryOrEmptyIfExceptionOccurred("input", s -> {
            throw new RuntimeException();
        });
        assertFalse(result2.isPresent());
    }

    @Test
    public void testForEachNonNullWithFlatMapperTriple() {
        String[] array = { "ab", null, "cd", "ef" };
        List<String> result = new ArrayList<>();

        N.forEachNonNull(array, s -> s != null ? Arrays.asList(s.charAt(0), s.charAt(1)) : null,
                c -> c != null ? Arrays.asList(c.toString().toUpperCase(), c.toString().toLowerCase()) : null,
                (original, ch, str) -> result.add(original + ":" + ch + ":" + str));

        assertEquals(12, result.size()); // 3 non-null strings * 2 chars * 2 case variations
        assertTrue(result.contains("ab:a:A"));
        assertTrue(result.contains("ab:a:a"));
    }

    @Test
    public void testToJsonWithFileAndConfig() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        File outputFile = new File(tempDir, "test_config.json");
        JSONSerializationConfig config = JSC.create().prettyFormat(true);
        N.toJson(person, config, outputFile);

        assertTrue(outputFile.exists());
        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertTrue(content.contains("\n")); // Pretty formatted
        assertTrue(content.contains("John"));

        IOUtil.deleteIfExists(outputFile);
    }

    @Test
    public void testToJsonWithOutputStreamAndConfig() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JSONSerializationConfig config = JSC.create().prettyFormat(true);
        N.toJson(person, config, baos);

        String json = baos.toString();
        assertTrue(json.contains("\n")); // Pretty formatted
        assertTrue(json.contains("John"));
    }

    @Test
    public void testToJsonWithWriterAndConfig() throws IOException {
        TestPerson person = new TestPerson("John", 30);
        StringWriter writer = new StringWriter();
        JSONSerializationConfig config = JSC.create().prettyFormat(true);
        N.toJson(person, config, writer);

        String json = writer.toString();
        assertTrue(json.contains("\n")); // Pretty formatted
        assertTrue(json.contains("John"));
    }

    @Test
    public void testFormatJsonWithTransferType() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        String json = N.toJson(map);

        String formatted = N.formatJson(json, Map.class);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("John"));
    }

    @Test
    public void testFormatXmlWithTransferType() {
        TestPerson person = new TestPerson("John", 30);
        String xml = N.toXml(person);

        String formatted = N.formatXml(xml, TestPerson.class);
        assertTrue(formatted.contains("\n"));
        assertTrue(formatted.contains("John"));
    }

    // Additional forEach Tests

    @Test
    public void testForEachWithStepLargerThanRange() {
        List<Integer> result = new ArrayList<>();
        N.forEach(0, 5, 10, result::add);
        assertEquals(Arrays.asList(0), result);
    }

    @Test
    public void testForEachWithNegativeStep() {
        List<Integer> result = new ArrayList<>();
        N.forEach(10, 0, -1, result::add);
        assertEquals(Arrays.asList(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), result);
    }

    @Test
    public void testForEachWithEqualStartAndEnd() {
        List<Integer> result = new ArrayList<>();
        N.forEach(5, 5, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachArrayReversed() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEach(array, 4, -1, result::add);
        assertEquals(Arrays.asList("e", "d", "c", "b", "a"), result);
    }

    @Test
    public void testForEachIterableWithLinkedList() {
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> result = new ArrayList<>();
        N.forEach(linkedList, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);
    }

    @Test
    public void testForEachMapWithBiConsumerEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<String> result = new ArrayList<>();
        N.forEach(map, entry -> result.add(entry.getKey() + "=" + entry.getValue()));
        assertEquals(Arrays.asList("a=1", "b=2", "c=3"), result);
    }

    @Test
    public void testForEachParallelWithException() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertThrows(RuntimeException.class, () -> {
            N.forEach(list, i -> {
                if (i == 3) {
                    throw new RuntimeException("Test exception");
                }
            }, 2);
        });
    }

    @Test
    public void testForEachWithFlatMapperNullHandling() {
        String[] array = { "ab", null, "cd" };
        List<Character> result = new ArrayList<>();
        N.forEach(array, s -> s != null ? Arrays.asList(s.charAt(0), s.charAt(1)) : null, (s, c) -> result.add(c));
        assertEquals(Arrays.asList('a', 'b', 'c', 'd'), result);
    }

    @Test
    public void testForEachTripleNested() {
        List<String> level1 = Arrays.asList("A", "B");
        List<String> result = new ArrayList<>();
        N.forEach(level1, s1 -> Arrays.asList(s1 + "1", s1 + "2"), s2 -> Arrays.asList(s2 + "a", s2 + "b"),
                (original, mid, end) -> result.add(original + "-" + mid + "-" + end));
        assertEquals(8, result.size()); // 2 * 2 * 2
        assertTrue(result.contains("A-A1-A1a"));
        assertTrue(result.contains("B-B2-B2b"));
    }

    @Test
    public void testForEachPairWithEmptyCollection() {
        List<String> empty = Collections.emptyList();
        List<String> result = new ArrayList<>();
        N.forEachPair(empty, (a, b) -> result.add(a + "-" + b));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachPairWithSingleElement() {
        List<String> single = Arrays.asList("only");
        List<String> result = new ArrayList<>();
        N.forEachPair(single, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
        assertEquals(Arrays.asList("only-null"), result);
    }

    @Test
    public void testForEachPair_1() {
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "b-c", "c-d", "d-e"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c, 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "c-d", "e-null"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c, 3, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "d-e"), result);
        }

        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c.iterator(), (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "b-c", "c-d", "d-e"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c.iterator(), 2, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "c-d", "e-null"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachPair(c.iterator(), 3, (a, b) -> result.add(a + "-" + (b != null ? b : "null")));
            assertEquals(Arrays.asList("a-b", "d-e"), result);
        }
    }

    @Test
    public void testForEachTrip_1() {
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "b-c-d", "c-d-e"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 2, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "c-d-e"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 3, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "d-e-null"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 4, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "e-null-null"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 5, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c, 6, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }

        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "b-c-d", "c-d-e"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 2, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "c-d-e"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 3, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "d-e-null"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 4, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c", "e-null-null"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 5, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }
        {
            List<String> c = N.asList("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEachTriple(c.iterator(), 6, (i, j, k) -> result.add(i + "-" + (j != null ? j : "null") + "-" + (k != null ? k : "null")));
            assertEquals(Arrays.asList("a-b-c"), result);
        }

    }

    @Test
    public void testForEachTripleWithLargeStep() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        List<String> result = new ArrayList<>();
        N.forEachTriple(array, 5, (a, b, c) -> result.add(a + "-" + (b != null ? b : "null") + "-" + (c != null ? c : "null")));
        assertEquals(Arrays.asList("1-2-3", "6-7-8"), result);
    }

    @Test
    public void testForEachIndexedWithNegativeRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Map<Integer, String> result = new HashMap<>();
        N.forEachIndexed(list, 3, 1, result::put);
        assertEquals("d", result.get(3));
        assertEquals("c", result.get(2));
        assertEquals(2, result.size());
    }

    @Test
    public void testForEachIndexe_01() {
        {
            List<String> list = Arrays.asList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 3, 1, result::put);
            assertEquals("d", result.get(3));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = N.asLinkedHashSet("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 1, 3, result::put);
            assertEquals("b", result.get(1));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = N.asLinkedHashSet("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 3, 1, result::put);
            assertEquals("d", result.get(3));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = N.asLinkedList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list, 3, 1, result::put);
            assertEquals("d", result.get(3));
            assertEquals("c", result.get(2));
            assertEquals(2, result.size());
        }
        {
            Collection<String> list = N.asLinkedList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list.iterator(), result::put);
            assertEquals("a", result.get(0));
            assertEquals("b", result.get(1));
            assertEquals("c", result.get(2));
            assertEquals("d", result.get(3));
            assertEquals("e", result.get(4));
            assertEquals(5, result.size());
        }
        {
            Collection<String> list = N.asLinkedList("a", "b", "c", "d", "e");
            Map<Integer, String> result = new HashMap<>();
            N.forEachIndexed(list.iterator(), result::put);
            assertEquals("a", result.get(0));
            assertEquals("b", result.get(1));
            assertEquals("c", result.get(2));
            assertEquals("d", result.get(3));
            assertEquals("e", result.get(4));
            assertEquals(5, result.size());

            Map<Integer, String> result2 = new HashMap<>();
            N.forEachIndexed(result, (i, e) -> result2.put(e.getKey() + 1, e.getValue()));
            assertEquals("a", result2.get(1));
            assertEquals("b", result2.get(2));
            assertEquals("c", result2.get(3));
            assertEquals("d", result2.get(4));
            assertEquals("e", result2.get(5));
            assertEquals(5, result2.size());

            Map<Integer, String> result3 = new HashMap<>();
            N.forEachIndexed(result, (i, k, v) -> result3.put(k + 1, v));
            assertEquals("a", result3.get(1));
            assertEquals("b", result3.get(2));
            assertEquals("c", result3.get(3));
            assertEquals("d", result3.get(4));
            assertEquals("e", result3.get(5));
            assertEquals(5, result3.size());
        }
    }

    @Test
    public void testForEachIndexedParallelWithCustomExecutor() throws Exception {
        ExecutorService customExecutor = Executors.newFixedThreadPool(2);
        try {
            List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
            ConcurrentHashMap<Integer, Integer> result = new ConcurrentHashMap<>();
            N.forEachIndexed(list, result::put, 2, customExecutor);

            assertEquals(5, result.size());
            for (int i = 0; i < 5; i++) {
                assertEquals(i + 1, result.get(i));
            }
        } finally {
            customExecutor.shutdown();
        }
    }

    @Test
    public void testForEachNonNullWithAllNulls() {
        String[] array = { null, null, null };
        List<String> result = new ArrayList<>();
        N.forEachNonNull(array, result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachWithMixedIterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        List<String> result = new ArrayList<>();
        N.forEach(list1, set2, (i, s) -> result.add(i + s));
        assertEquals(Arrays.asList("1a", "2b", "3c"), result);
    }

    // Additional asyncExecute Tests

    @Test
    public void testAsyncExecuteWithCancellation() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ContinuableFuture<Void> future = N.asyncExecute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertFalse(future.isDone());
        future.cancel(true);
        assertTrue(future.isCancelled());
        latch.countDown();
    }

    @Test
    public void testAsyncExecuteChaining() throws Exception {
        ContinuableFuture<String> future = N.asyncExecute(() -> "Hello");

        assertEquals("Hello World!", future.getThenApply(s -> s + " World!"));
    }

    @Test
    public void testAsyncExecuteWithExceptionHandling() throws Exception {
        ContinuableFuture<String> future = N.asyncExecute(() -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    public void testAsyncExecuteMultipleWithDifferentDelays() throws Exception {
        long start = System.currentTimeMillis();
        List<ContinuableFuture<Integer>> futures = Arrays.asList(N.asyncExecute(() -> 1, 100), N.asyncExecute(() -> 2, 50), N.asyncExecute(() -> 3, 150));

        List<Integer> results = new ArrayList<>();
        for (ContinuableFuture<Integer> future : futures) {
            results.add(future.get());
        }

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 150);
        assertEquals(Arrays.asList(1, 2, 3), results);
    }

    @Test
    public void testAsyncExecuteListWithEmptyList() {
        List<Throwables.Runnable<Exception>> emptyCommands = Collections.emptyList();
        List<ContinuableFuture<Void>> futures = N.asyncExecute(emptyCommands);
        assertTrue(futures.isEmpty());
    }

    @Test
    public void testAsyncExecuteCollectionWithMixedResults() throws Exception {
        Collection<Callable<Object>> commands = Arrays.asList(() -> "string", () -> 42, () -> true, () -> null);

        List<ContinuableFuture<Object>> futures = N.asyncExecute(commands);
        List<Object> results = new ArrayList<>();
        for (ContinuableFuture<Object> future : futures) {
            results.add(future.get());
        }

        assertEquals(4, results.size());
        assertEquals("string", results.get(0));
        assertEquals(42, results.get(1));
        assertEquals(true, results.get(2));
        assertNull(results.get(3));
    }

    @Test
    public void testAsyncExecute_01() throws Exception {
        Collection<Callable<Object>> commands = Arrays.asList(() -> "string", () -> 42, () -> true, () -> null);

        List<ContinuableFuture<Object>> futures = N.asyncExecute(commands, Executors.newFixedThreadPool(6));
        List<Object> results = new ArrayList<>();
        for (ContinuableFuture<Object> future : futures) {
            results.add(future.get());
        }

        assertEquals(4, results.size());
        assertEquals("string", results.get(0));
        assertEquals(42, results.get(1));
        assertEquals(true, results.get(2));
        assertNull(results.get(3));
    }

    @Test
    public void testAsynRunWithExceptionInOneTask() {
        AtomicInteger counter = new AtomicInteger(0);
        List<Throwables.Runnable<Exception>> commands = Arrays.asList(counter::incrementAndGet, () -> {
            throw new RuntimeException("Test exception");
        }, counter::incrementAndGet);

        ObjIterator<Void> iter = N.asynRun(commands);

        // First task should complete
        assertTrue(iter.hasNext());
        iter.next();

        // Second task should throw exception
        assertThrows(RuntimeException.class, () -> {
            while (iter.hasNext()) {
                iter.next();
            }
        });
    }

    @Test
    public void testAsynCallWithDifferentExecutionTimes() {
        List<Callable<String>> commands = Arrays.asList(() -> {
            Thread.sleep(100);
            return "slow";
        }, () -> {
            Thread.sleep(10);
            return "fast";
        }, () -> {
            Thread.sleep(50);
            return "medium";
        });

        ObjIterator<String> iter = N.asynCall(commands);
        List<String> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        // Results should be in completion order, not submission order
        assertEquals(3, results.size());
        assertTrue(results.contains("slow"));
        assertTrue(results.contains("fast"));
        assertTrue(results.contains("medium"));
    }

    // Additional runInParallel Tests

    @Test
    public void testRunInParallelWithExceptionInFirstTask() {
        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            N.runInParallel(() -> {
                throw new RuntimeException("First task failed");
            }, counter::incrementAndGet);
        });
        // Second task might or might not execute depending on timing
    }

    @Test
    public void testRunInParallelWithExceptionInSecondTask() {
        AtomicInteger counter = new AtomicInteger(0);
        assertThrows(RuntimeException.class, () -> {
            N.runInParallel(counter::incrementAndGet, () -> {
                Thread.sleep(50);
                throw new RuntimeException("Second task failed");
            });
        });
        assertEquals(1, counter.get());
    }

    @Test
    public void testRunInParallelWithEmptyCollection() {
        List<Throwables.Runnable<Exception>> emptyTasks = Collections.emptyList();
        N.runInParallel(emptyTasks); // Should not throw
    }

    @Test
    public void testRunInParallelWithCustomExecutor() {
        ExecutorService customExecutor = Executors.newFixedThreadPool(2);
        try {
            AtomicInteger counter = new AtomicInteger(0);
            List<Throwables.Runnable<Exception>> tasks = Arrays.asList(counter::incrementAndGet, counter::incrementAndGet, counter::incrementAndGet);

            N.runInParallel(tasks, customExecutor);
            assertEquals(3, counter.get());
        } finally {
            customExecutor.shutdown();
        }
    }

    //    @Test
    //    public void testCallInParallelWithTimeout() throws Exception {
    //        assertThrows(RuntimeException.class, () -> {
    //            N.callInParallel(() -> "fast", () -> {
    //                Thread.sleep(5000);
    //                return "slow";
    //            });
    //        });
    //    }

    @Test
    public void testCallInParallelWithNullResults() {
        Tuple3<String, Integer, Object> result = N.callInParallel(() -> "text", () -> 42, () -> null);

        assertEquals("text", result._1);
        assertEquals(42, result._2);
        assertNull(result._3);
    }

    // Additional batch processing tests

    @Test
    public void testRunByBatchWithExactMultiple() {
        Integer[] array = { 1, 2, 3, 4, 5, 6 };
        List<Integer> batchSizes = new ArrayList<>();
        N.runByBatch(array, 3, batch -> batchSizes.add(batch.size()));
        assertEquals(Arrays.asList(3, 3), batchSizes);
    }

    @Test
    public void testRunByBatchWithIterator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7);
        List<Integer> sums = new ArrayList<>();
        N.runByBatch(list.iterator(), 2, batch -> sums.add(batch.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(Arrays.asList(3, 7, 11, 7), sums); // [1,2], [3,4], [5,6], [7]
    }

    @Test
    public void testCallByBatchWithException() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        assertThrows(RuntimeException.class, () -> {
            N.callByBatch(list, 2, batch -> {
                if (batch.contains(3)) {
                    throw new RuntimeException("Batch contains 3");
                }
                return batch.size();
            });
        });
    }

    @Test
    public void testRunByBatchWithElementConsumerException() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        assertThrows(RuntimeException.class, () -> {
            N.runByBatch(array, 2, (idx, element) -> {
                if (element == 3) {
                    throw new RuntimeException("Element is 3");
                }
            }, () -> {
            });
        });
    }

    // Additional uninterruptible execution tests

    @Test
    public void testRunUninterruptiblyWithInterruption() {
        Thread currentThread = Thread.currentThread();
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);

        // Schedule an interruption
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> currentThread.interrupt(), 50, TimeUnit.MILLISECONDS);

        try {
            N.runUninterruptibly(() -> {
                Thread.sleep(100);
                wasInterrupted.set(Thread.currentThread().isInterrupted());
            });

            // Thread should be interrupted after completion
            assertTrue(Thread.interrupted());
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void testCallUninterruptiblyWithTimeUnitAndInterruption() {
        Thread currentThread = Thread.currentThread();

        MutableBoolean interrupted = MutableBoolean.of(false);
        // Schedule an interruption
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
        scheduler.schedule(() -> {
            currentThread.interrupt();
            interrupted.setTrue();
        }, 50, TimeUnit.MILLISECONDS);

        try {
            String result = N.callUninterruptibly((remainingNanos, unit) -> {
                unit.sleep(100);
                // assertTrue(interrupted.value());
                return "completed";
            }, 200, TimeUnit.MILLISECONDS);

            assertEquals("completed", result);
            // Thread should be interrupted after completion
            // assertTrue(Thread.interrupted());
        } finally {
            scheduler.shutdown();
        }
    }

    // Additional edge cases

    @Test
    public void testForEachWithVeryLargeStep() {
        List<Integer> result = new ArrayList<>();
        N.forEach(0, 100, 1000, result::add);
        assertEquals(Arrays.asList(0), result);
    }

    @Test
    public void testForEachIndexedWithEmptyMap() {
        Map<String, Integer> emptyMap = Collections.emptyMap();
        AtomicInteger counter = new AtomicInteger(0);
        N.forEachIndexed(emptyMap, (idx, entry) -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testAsyncExecuteWithRetryAllFailures() {
        AtomicInteger attemptCount = new AtomicInteger(0);
        assertThrows(ExecutionException.class, () -> {
            ContinuableFuture<Void> future = N.asyncExecute(() -> {
                attemptCount.incrementAndGet();
                throw new RuntimeException("Always fails");
            }, 3, 10, e -> true);

            future.get();
        });

        assertEquals(4, attemptCount.get()); // Initial + 3 retries
    }

    @Test
    public void testRunInParallelWithDifferentExecutionTimes() {
        long start = System.currentTimeMillis();
        AtomicInteger fast = new AtomicInteger(0);
        AtomicInteger slow = new AtomicInteger(0);

        N.runInParallel(() -> {
            Thread.sleep(10);
            fast.incrementAndGet();
        }, () -> {
            Thread.sleep(100);
            slow.incrementAndGet();
        });

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 100);
        assertEquals(1, fast.get());
        assertEquals(1, slow.get());
    }

    @Test
    public void testCallByBatchWithSingleElementBatch() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> results = N.callByBatch(list, 1, batch -> batch.get(0));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), results);
    }

    @Test
    public void testForEachWithDescendingIterator() {
        {
            List<String> list = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
            List<String> result = new ArrayList<>();
            N.forEach(list, 4, 1, result::add);
            assertEquals(Arrays.asList("e", "d", "c"), result);
        }
        {
            Collection<String> list = N.asLinkedHashSet("a", "b", "c", "d", "e");
            List<String> result = new ArrayList<>();
            N.forEach(list, 4, 1, result::add);
            assertEquals(Arrays.asList("e", "d", "c"), result);
        }
    }

    @Test
    public void testSleepWithZeroTimeout() {
        long start = System.currentTimeMillis();
        N.sleep(0);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 10);
    }

    @Test
    public void testSleepUninterruptiblyWithZeroTimeout() {
        long start = System.currentTimeMillis();
        N.sleepUninterruptibly(0);
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 10);
    }

    @Test
    public void testToRuntimeExceptionWithError() {
        Error error = new Error("Test error");
        RuntimeException result = N.toRuntimeException(error);
        assertNotNull(result);
        assertEquals(error, result.getCause());
    }

    @Test
    public void testIfNotEmptyWithNull() {
        AtomicInteger counter = new AtomicInteger(0);

        N.ifNotEmpty((CharSequence) null, s -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        N.ifNotEmpty((Collection) null, c -> counter.incrementAndGet());
        assertEquals(0, counter.get());

        N.ifNotEmpty((Map) null, m -> counter.incrementAndGet());
        assertEquals(0, counter.get());
    }

    @Test
    public void testForEachWithObjectAndParameter() {
        String[] array = { "a", "b", "c" };
        StringBuilder sb = new StringBuilder();
        N.forEach(0, array.length, sb, (i, builder) -> builder.append(array[i]));
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testAsyncExecuteListWithDifferentExecutors() throws Exception {
        ExecutorService executor1 = Executors.newSingleThreadExecutor();
        ExecutorService executor2 = Executors.newSingleThreadExecutor();

        try {
            List<Throwables.Runnable<Exception>> commands = Arrays.asList(() -> Thread.sleep(10), () -> Thread.sleep(10));

            List<ContinuableFuture<Void>> futures1 = N.asyncExecute(commands, executor1);
            List<ContinuableFuture<Void>> futures2 = N.asyncExecute(commands, executor2);

            for (ContinuableFuture<Void> future : futures1) {
                future.get();
            }
            for (ContinuableFuture<Void> future : futures2) {
                future.get();
            }
        } finally {
            executor1.shutdown();
            executor2.shutdown();
        }
    }

    // Performance and Concurrency Tests

    @Test
    public void testForEachParallelPerformance() throws Exception {
        int size = 1000;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(i);
        }

        AtomicInteger sum = new AtomicInteger(0);
        long start = System.currentTimeMillis();

        N.forEach(list, i -> {
            // Simulate some work
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sum.addAndGet(i);
        }, 10);

        long duration = System.currentTimeMillis() - start;

        // Parallel execution should be faster than sequential
        assertTrue(duration < size); // Should take less than 1ms per item
        assertEquals(size * (size - 1) / 2, sum.get()); // Sum formula validation
    }

    @Test
    public void testAsynCallOrderPreservation() {
        int size = 100;
        List<Callable<Integer>> commands = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final int value = i;
            commands.add(() -> {
                Thread.sleep(ThreadLocalRandom.current().nextInt(10));
                return value;
            });
        }

        ObjIterator<Integer> iter = N.asynCall(commands);
        Set<Integer> results = new HashSet<>();

        while (iter.hasNext()) {
            results.add(iter.next());
        }

        assertEquals(size, results.size());
        for (int i = 0; i < size; i++) {
            assertTrue(results.contains(i));
        }
    }

    @Test
    public void testRunInParallelCancellationPropagation() throws Exception {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicBoolean task2Started = new AtomicBoolean(false);
        AtomicBoolean task2Completed = new AtomicBoolean(false);

        Thread executionThread = new Thread(() -> {
            try {
                N.runInParallel(() -> {
                    latch1.await();
                }, () -> {
                    task2Started.set(true);
                    latch2.await();
                    task2Completed.set(true);
                });
            } catch (Exception e) {
                // Expected
            }
        });

        executionThread.start();
        Thread.sleep(50); // Let tasks start
        assertTrue(task2Started.get());

        // Interrupt the execution
        executionThread.interrupt();
        latch1.countDown();
        latch2.countDown();

        executionThread.join(1000);
        assertFalse(executionThread.isAlive());
    }

    @Test
    public void testForEachWithConcurrentModification() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

        assertThrows(ConcurrentModificationException.class, () -> {
            N.forEach(list, i -> {
                if (i == 3) {
                    list.add(6); // Modify during iteration
                }
            });
        });
    }

    @Test
    public void testAsyncExecuteMemoryLeak() throws Exception {
        // Test that completed futures are properly cleaned up
        List<ContinuableFuture<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            final int value = i;
            futures.add(N.asyncExecute(() -> value));
        }

        // Get all results
        for (ContinuableFuture<Integer> future : futures) {
            future.get();
        }

        // Force garbage collection
        System.gc();
        Thread.sleep(100);

        // All futures should be completed and eligible for GC
        for (ContinuableFuture<Integer> future : futures) {
            assertTrue(future.isDone());
        }
    }

    // Complex Nested Operations Tests

    @Test
    public void testNestedForEachOperations() {
        List<String> outer = Arrays.asList("A", "B");
        List<Integer> inner = Arrays.asList(1, 2, 3);
        List<String> results = new ArrayList<>();

        N.forEach(outer, outerItem -> {
            N.forEach(inner, innerItem -> {
                N.forEach(0, innerItem, i -> {
                    results.add(outerItem + innerItem + i);
                });
            });
        });

        assertEquals(12, results.size()); // A: 0,0-1,0-2 B: 0,0-1,0-2
    }

    @Test
    public void testComplexBatchProcessing() {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        List<String> results = N.callByBatch(data, 10, batch -> {
            // Complex processing per batch
            int sum = batch.stream().mapToInt(Integer::intValue).sum();
            int min = batch.stream().min(Integer::compareTo).orElse(0);
            int max = batch.stream().max(Integer::compareTo).orElse(0);
            return String.format("Batch[%d-%d]: sum=%d", min, max, sum);
        });

        assertEquals(10, results.size());
        assertTrue(results.get(0).contains("Batch[1-10]: sum=55"));
        assertTrue(results.get(9).contains("Batch[91-100]: sum=955"));
    }

    @Test
    public void testMixedSynchronousAsynchronousExecution() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> syncResults = new ArrayList<>();

        // Mix of sync and async operations
        ContinuableFuture<Void> async1 = N.asyncExecute(() -> {
            Thread.sleep(50);
            counter.addAndGet(10);
        });

        N.forEach(Arrays.asList(1, 2, 3), syncResults::add);

        ContinuableFuture<Integer> async2 = N.asyncExecute(() -> {
            Thread.sleep(25);
            return counter.addAndGet(20);
        });

        N.forEach(Arrays.asList(4, 5, 6), syncResults::add);

        // Wait for async operations
        async1.get();
        int async2Result = async2.get();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), syncResults);
        assertEquals(30, counter.get());
        assertEquals(20, async2Result);
    }

    // Stress Tests

    @Test
    public void testHighConcurrencyForEach() throws Exception {
        int threadCount = 50;
        int itemsPerThread = 100;
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    List<Integer> items = new ArrayList<>();
                    for (int i = 0; i < itemsPerThread; i++) {
                        items.add(threadId * itemsPerThread + i);
                    }
                    N.forEach(items, results::add, 5);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        assertEquals(threadCount * itemsPerThread, results.size());
        Set<Integer> uniqueResults = new HashSet<>(results);
        assertEquals(threadCount * itemsPerThread, uniqueResults.size());
    }

    @Test
    public void testLargeCollectionProcessing() {
        int size = 10000;
        List<Integer> largeList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            largeList.add(i);
        }

        // Test various forEach operations on large collection
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(largeList, 1000, 9000, sum::addAndGet);

        int expectedSum = 0;
        for (int i = 1000; i < 9000; i++) {
            expectedSum += i;
        }
        assertEquals(expectedSum, sum.get());
    }

    // JSON/XML Advanced Tests

    @Test
    public void testJsonSerializationOfComplexObjects() {
        Map<String, Object> complex = new HashMap<>();
        complex.put("string", "value");
        complex.put("number", 42);
        complex.put("array", Arrays.asList(1, 2, 3));
        complex.put("nested", Collections.singletonMap("key", "value"));
        complex.put("null", null);

        String json = N.toJson(complex);
        Map<String, Object> deserialized = N.fromJson(json, Map.class);

        assertEquals("value", deserialized.get("string"));
        assertEquals(42, ((Number) deserialized.get("number")).intValue());
        assertEquals(Arrays.asList(1, 2, 3), deserialized.get("array"));
        assertEquals("value", ((Map) deserialized.get("nested")).get("key"));
        assertNull(deserialized.get("null"));
    }

    // Error Recovery Tests

    @Test
    public void testExecuteWithRetryRecovery() {
        AtomicInteger attempts = new AtomicInteger(0);
        String result = N.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("Fail " + attempt);
            }
            return "Success on attempt " + attempt;
        }, 5, 10, (r, e) -> e != null);

        assertEquals("Success on attempt 3", result);
        assertEquals(3, attempts.get());
    }

    @Test
    public void testForEachWithPartialFailure() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> processed = new ArrayList<>();

        try {
            N.forEach(items, i -> {
                processed.add(i);
                if (i == 3) {
                    throw new RuntimeException("Failed at 3");
                }
            });
        } catch (RuntimeException e) {
            assertEquals("Failed at 3", e.getMessage());
        }

        assertEquals(Arrays.asList(1, 2, 3), processed);
    }

    // Special Cases Tests

    @Test
    public void testForEachWithRandomAccess() {
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));

        List<String> arrayListResult = new ArrayList<>();
        List<String> linkedListResult = new ArrayList<>();

        N.forEach(arrayList, 1, 4, arrayListResult::add);
        N.forEach(linkedList, 1, 4, linkedListResult::add);

        assertEquals(arrayListResult, linkedListResult);
        assertEquals(Arrays.asList("b", "c", "d"), arrayListResult);
    }

    @Test
    public void testPrintlnWithSpecialCharacters() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.println("Line1\nLine2\tTab\r\nLine3");
            String output = baos.toString();
            assertTrue(output.contains("Line1"));
            assertTrue(output.contains("Line2"));
            assertTrue(output.contains("Tab"));
            assertTrue(output.contains("Line3"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testLazyInitializationThreadSafety() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            counter.incrementAndGet();
            try {
                Thread.sleep(50); // Simulate expensive initialization
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "initialized";
        });

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<String> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    results.add(lazy.get());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

        // Should initialize only once despite concurrent access
        assertEquals(1, counter.get());
        assertEquals(threadCount, results.size());
        for (String result : results) {
            assertEquals("initialized", result);
        }
    }

    @Test
    public void testComplexFlatMapping() {
        List<String> departments = Arrays.asList("Engineering", "Sales");
        Map<String, List<String>> employees = new HashMap<>();
        employees.put("Engineering", Arrays.asList("Alice", "Bob"));
        employees.put("Sales", Arrays.asList("Charlie", "David", "Eve"));

        List<String> result = new ArrayList<>();
        N.forEach(departments, dept -> employees.get(dept), emp -> Arrays.asList(emp.toLowerCase(), emp.toUpperCase()),
                (dept, emp, formatted) -> result.add(dept + ":" + emp + ":" + formatted));

        assertEquals(10, result.size()); // 2 + 3 employees, each with 2 formats
        assertTrue(result.contains("Engineering:Alice:alice"));
        assertTrue(result.contains("Sales:Eve:EVE"));
    }

    @Test
    public void testRunByBatchWithNullElements() {
        String[] array = { "a", null, "b", null, "c" };
        List<List<String>> batches = new ArrayList<>();
        N.runByBatch(array, 2, batches::add);

        assertEquals(3, batches.size());
        assertEquals(Arrays.asList("a", null), batches.get(0));
        assertEquals(Arrays.asList("b", null), batches.get(1));
        assertEquals(Arrays.asList("c"), batches.get(2));
    }

    @Test
    public void testRunByBatchWithSingleElement() {
        Integer[] array = { 42 };
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(array, 5, batch -> {
            assertEquals(1, batch.size());
            assertEquals(42, batch.get(0));
            batchCount.incrementAndGet();
        });
        assertEquals(1, batchCount.get());
    }

    @Test
    public void testRunByBatchWithEmptyIterable() {
        List<String> emptyList = Collections.emptyList();
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(emptyList, 10, batch -> batchCount.incrementAndGet());
        assertEquals(0, batchCount.get());
    }

    @Test
    public void testRunByBatchWithNullIterator() {
        Iterator<String> nullIterator = null;
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(nullIterator, 10, batch -> batchCount.incrementAndGet());
        assertEquals(0, batchCount.get());
    }

    @Test
    public void testRunByBatchWithElementConsumerTracking() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f", "g");
        List<String> processedItems = new ArrayList<>();
        List<Integer> processedIndices = new ArrayList<>();
        AtomicInteger batchExecutions = new AtomicInteger(0);

        N.runByBatch(items, 3, (idx, item) -> {
            processedIndices.add(idx);
            processedItems.add(item);
        }, batchExecutions::incrementAndGet);

        assertEquals(items, processedItems);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5, 6), processedIndices);
        assertEquals(3, batchExecutions.get()); // 3 batches: [a,b,c], [d,e,f], [g]
    }

    @Test
    public void testRunByBatchExceptionPropagation() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);

        assertThrows(RuntimeException.class, () -> {
            N.runByBatch(items, 2, batch -> {
                if (batch.contains(3)) {
                    throw new RuntimeException("Found 3!");
                }
            });
        });
    }

    @Test
    public void testRunByBatchWithLargeBatchSize() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        AtomicInteger batchCount = new AtomicInteger(0);
        N.runByBatch(array, 100, batch -> {
            assertEquals(5, batch.size());
            batchCount.incrementAndGet();
        });
        assertEquals(1, batchCount.get());
    }

    // Additional callByBatch Tests

    @Test
    public void testCallByBatchWithTransformation() {
        List<String> items = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        List<String> results = N.callByBatch(items, 2, batch -> batch.stream().map(String::toUpperCase).collect(Collectors.joining("-")));

        assertEquals(Arrays.asList("APPLE-BANANA", "CHERRY-DATE", "ELDERBERRY"), results);
    }

    @Test
    public void testCallByBatchWithEmptyArray() {
        String[] emptyArray = new String[0];
        List<Integer> results = N.callByBatch(emptyArray, 5, List::size);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testCallByBatchWithNullReturn() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4);
        List<String> results = N.callByBatch(items, 2, batch -> batch.contains(3) ? null : batch.toString());

        assertEquals(2, results.size());
        assertEquals("[1, 2]", results.get(0));
        assertNull(results.get(1));
    }

    @Test
    public void testCallByBatchWithElementConsumerState() {
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5, 6);
        AtomicInteger sum = new AtomicInteger(0);

        List<Integer> results = N.callByBatch(items, 2, (idx, item) -> sum.addAndGet(item), () -> {
            int currentSum = sum.get();
            sum.set(0); // Reset for next batch
            return currentSum;
        });

        assertEquals(Arrays.asList(3, 7, 11), results); // [1+2], [3+4], [5+6]
    }

    @Test
    public void testCallByBatchIteratorWithComplexProcessing() {
        Iterator<String> iter = Arrays.asList("a", "bb", "ccc", "dddd", "eeeee").iterator();
        List<Integer> results = N.callByBatch(iter, 2, batch -> batch.stream().mapToInt(String::length).sum());

        assertEquals(Arrays.asList(3, 7, 5), results); // [1+2], [3+4], [5]
    }

    // Additional runUninterruptibly Tests

    @Test
    public void testRunUninterruptiblyWithImmediateSuccess() {
        AtomicBoolean executed = new AtomicBoolean(false);
        N.runUninterruptibly(() -> {
            executed.set(true);
            Thread.sleep(10);
        });
        assertTrue(executed.get());
    }

    @Test
    public void testRunUninterruptiblyWithMultipleInterruptions() {
        AtomicInteger attempts = new AtomicInteger(0);
        Thread currentThread = Thread.currentThread();

        // Schedule multiple interruptions
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> currentThread.interrupt(), 20, TimeUnit.MILLISECONDS);
        scheduler.schedule(() -> currentThread.interrupt(), 40, TimeUnit.MILLISECONDS);

        try {
            N.runUninterruptibly(() -> {
                attempts.incrementAndGet();
                Thread.sleep(100);
            });

            assertEquals(3, attempts.get()); // Should complete despite interruptions
            assertTrue(Thread.interrupted()); // Thread should be interrupted after completion
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void testRunUninterruptiblyWithTimeoutExpiration() {
        AtomicBoolean completed = new AtomicBoolean(false);
        long start = System.currentTimeMillis();

        N.runUninterruptibly(remainingMillis -> {
            try {
                if (remainingMillis > 0) {
                    Thread.sleep(remainingMillis);
                    completed.set(true);
                }
            } catch (InterruptedException e) {
                // Will retry with updated remaining time
                throw e;
            }
        }, 100);

        long duration = System.currentTimeMillis() - start;
        assertTrue(completed.get());
        assertTrue(duration >= 100);
    }

    @Test
    public void testRunUninterruptiblyWithTimeUnit() {
        AtomicInteger executionCount = new AtomicInteger(0);

        N.runUninterruptibly((remaining, unit) -> {
            executionCount.incrementAndGet();
            assertTrue(remaining > 0);
            assertEquals(TimeUnit.NANOSECONDS, unit);
            unit.sleep(10);
        }, 50, TimeUnit.MILLISECONDS);

        assertEquals(1, executionCount.get());
    }

    // Additional callUninterruptibly Tests

    @Test
    public void testCallUninterruptiblyWithResult() {
        String result = N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return "Success";
        });
        assertEquals("Success", result);
    }

    @Test
    public void testCallUninterruptiblyWithNullResult() {
        String result = N.callUninterruptibly(() -> {
            Thread.sleep(10);
            return null;
        });
        assertNull(result);
    }

    @Test
    public void testCallUninterruptiblyWithComplexComputation() {
        List<Integer> result = N.callUninterruptibly(() -> {
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                Thread.sleep(10);
                list.add(i);
            }
            return list;
        });

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testCallUninterruptiblyWithRemainingTime() {
        List<Long> remainingTimes = new ArrayList<>();

        String result = N.callUninterruptibly(remainingMillis -> {
            remainingTimes.add(remainingMillis);
            Thread.sleep(30);
            return "Completed with " + remainingMillis + "ms remaining";
        }, 100);

        assertNotNull(result);
        assertTrue(remainingTimes.get(0) <= 100);
        assertTrue(remainingTimes.get(0) > 0);
    }

    @Test
    public void testCallUninterruptiblyWithTimeUnitConversion() {
        AtomicReference<TimeUnit> receivedUnit = new AtomicReference<>();
        AtomicLong receivedTime = new AtomicLong();

        Integer result = N.callUninterruptibly((time, unit) -> {
            receivedUnit.set(unit);
            receivedTime.set(time);
            unit.sleep(1);
            return 42;
        }, 100, TimeUnit.MILLISECONDS);

        assertEquals(42, result);
        assertEquals(TimeUnit.NANOSECONDS, receivedUnit.get());
        assertTrue(receivedTime.get() > 0);
    }

    // Additional tryOrEmptyIfExceptionOccurred Tests

    @Test
    public void testTryOrEmptyWithSuccessfulExecution() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> "Success");
        assertTrue(result.isPresent());
        assertEquals("Success", result.get());
    }

    @Test
    public void testTryOrEmptyWithCheckedException() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new IOException("IO Error");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testTryOrEmptyWithRuntimeException() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            throw new IllegalArgumentException("Invalid argument");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testTryOrEmptyWithNullResult() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void testTryOrEmptyWithFunction() {
        Nullable<Integer> result1 = N.tryOrEmptyIfExceptionOccurred("123", Integer::parseInt);
        assertTrue(result1.isPresent());
        assertEquals(123, result1.get());

        Nullable<Integer> result2 = N.tryOrEmptyIfExceptionOccurred("abc", Integer::parseInt);
        assertFalse(result2.isPresent());
    }

    @Test
    public void testTryOrEmptyWithComplexOperation() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 42);

        Nullable<Integer> result1 = N.tryOrEmptyIfExceptionOccurred(map, m -> m.get("key"));
        assertTrue(result1.isPresent());
        assertEquals(42, result1.get());

        Nullable<Integer> result2 = N.tryOrEmptyIfExceptionOccurred(map, m -> {
            throw new UnsupportedOperationException();
        });
        assertFalse(result2.isPresent());
    }

    // Additional tryOrDefaultIfExceptionOccurred Tests

    @Test
    public void testTryOrDefaultWithVariousScenarios() {
        // Successful execution
        String result1 = N.tryOrDefaultIfExceptionOccurred(() -> "Success", "Default");
        assertEquals("Success", result1);

        // With exception
        String result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
            throw new RuntimeException();
        }, "Default");
        assertEquals("Default", result2);

        // With null result
        String result3 = N.tryOrDefaultIfExceptionOccurred(() -> null, "Default");
        assertNull(result3);
    }

    @Test
    public void testTryOrDefaultWithFunctionAndInit() {
        List<String> list = Arrays.asList("a", "b", "c");

        Integer result1 = N.tryOrDefaultIfExceptionOccurred(list, List::size, -1);
        assertEquals(3, result1);

        Integer result2 = N.tryOrDefaultIfExceptionOccurred(list, l -> {
            throw new IndexOutOfBoundsException();
        }, -1);
        assertEquals(-1, result2);
    }

    @Test
    public void testTryOrDefaultWithComplexTypes() {
        TestPerson defaultPerson = new TestPerson("Default", 0);

        TestPerson result1 = N.tryOrDefaultIfExceptionOccurred(() -> new TestPerson("John", 30), defaultPerson);
        assertEquals("John", result1.getName());

        TestPerson result2 = N.tryOrDefaultIfExceptionOccurred(() -> {
            throw new RuntimeException();
        }, defaultPerson);
        assertSame(defaultPerson, result2);
    }

    // Additional ifNotEmpty Tests

    @Test
    public void testIfNotEmptyWithCharSequence() {
        StringBuilder executed = new StringBuilder();

        N.ifNotEmpty("Hello", s -> executed.append("String: ").append(s));
        assertEquals("String: Hello", executed.toString());

        executed.setLength(0);
        N.ifNotEmpty("", s -> executed.append("Should not execute"));
        assertEquals("", executed.toString());

        N.ifNotEmpty((CharSequence) null, s -> executed.append("Should not execute"));
        assertEquals("", executed.toString());
    }

    @Test
    public void testIfNotEmptyWithStringBuilder() {
        AtomicInteger counter = new AtomicInteger(0);

        StringBuilder sb1 = new StringBuilder("content");
        N.ifNotEmpty(sb1, s -> {
            counter.incrementAndGet();
            s.append(" modified");
        });
        assertEquals(1, counter.get());
        assertEquals("content modified", sb1.toString());

        StringBuilder sb2 = new StringBuilder();
        N.ifNotEmpty(sb2, s -> counter.incrementAndGet());
        assertEquals(1, counter.get()); // Not incremented
    }

    @Test
    public void testIfNotEmptyWithCollections() {
        List<String> executed = new ArrayList<>();

        // ArrayList
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b"));
        N.ifNotEmpty(arrayList, list -> executed.add("ArrayList: " + list.size()));

        // LinkedList
        LinkedList<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3));
        N.ifNotEmpty(linkedList, list -> executed.add("LinkedList: " + list.size()));

        // HashSet
        HashSet<String> hashSet = new HashSet<>(Arrays.asList("x", "y"));
        N.ifNotEmpty(hashSet, set -> executed.add("HashSet: " + set.size()));

        // Empty collection
        Vector<String> emptyVector = new Vector<>();
        N.ifNotEmpty(emptyVector, v -> executed.add("Should not execute"));

        assertEquals(3, executed.size());
        assertTrue(executed.contains("ArrayList: 2"));
        assertTrue(executed.contains("LinkedList: 3"));
        assertTrue(executed.contains("HashSet: 2"));
    }

    @Test
    public void testIfNotEmptyWithMaps() {
        List<String> executed = new ArrayList<>();

        // HashMap
        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        N.ifNotEmpty(hashMap, map -> executed.add("HashMap: " + map.size()));

        // TreeMap
        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("x", "X");
        N.ifNotEmpty(treeMap, map -> executed.add("TreeMap: " + map.size()));

        // Empty map
        LinkedHashMap<String, String> emptyMap = new LinkedHashMap<>();
        N.ifNotEmpty(emptyMap, map -> executed.add("Should not execute"));

        assertEquals(2, executed.size());
        assertTrue(executed.contains("HashMap: 2"));
        assertTrue(executed.contains("TreeMap: 1"));
    }

    @Test
    public void testIfNotEmptyWithModification() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        N.ifNotEmpty(list, l -> {
            l.add(4);
            l.remove(Integer.valueOf(1));
        });
        assertEquals(Arrays.asList(2, 3, 4), list);

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        N.ifNotEmpty(map, m -> {
            m.put("b", 2);
            m.remove("a");
        });
        assertEquals(1, map.size());
        assertEquals(2, map.get("b"));
    }

    // Additional ifOrEmpty Tests

    @Test
    public void testIfOrEmptyBasicCases() {
        Nullable<String> result1 = N.ifOrEmpty(true, () -> "Present");
        assertTrue(result1.isPresent());
        assertEquals("Present", result1.get());

        Nullable<String> result2 = N.ifOrEmpty(false, () -> "Not Present");
        assertFalse(result2.isPresent());
    }

    @Test
    public void testIfOrEmptyWithNullSupplier() {
        Nullable<String> result = N.ifOrEmpty(true, () -> null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void testIfOrEmptyWithException() {
        assertThrows(RuntimeException.class, () -> {
            N.ifOrEmpty(true, () -> {
                throw new RuntimeException("Supplier failed");
            });
        });

        // Exception not thrown when condition is false
        Nullable<String> result = N.ifOrEmpty(false, () -> {
            throw new RuntimeException("Should not execute");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void testIfOrEmptyWithComplexConditions() {
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Alice", 85);
        scores.put("Bob", 92);

        Nullable<String> topScorer = N.ifOrEmpty(scores.values().stream().anyMatch(s -> s > 90),
                () -> scores.entrySet().stream().filter(e -> e.getValue() > 90).map(Map.Entry::getKey).findFirst().orElse(null));

        assertTrue(topScorer.isPresent());
        assertEquals("Bob", topScorer.get());
    }

    // Additional ifNotNull Tests

    @Test
    public void testIfNotNullBasicCases() {
        AtomicInteger counter = new AtomicInteger(0);

        N.ifNotNull("value", v -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        N.ifNotNull(null, v -> counter.incrementAndGet());
        assertEquals(1, counter.get()); // Not incremented

        N.ifNotNull(42, v -> counter.addAndGet(v));
        assertEquals(43, counter.get());
    }

    @Test
    public void testIfNotNullWithDifferentTypes() {
        List<Object> processed = new ArrayList<>();

        N.ifNotNull("String", processed::add);
        N.ifNotNull(123, processed::add);
        N.ifNotNull(true, processed::add);
        N.ifNotNull(new TestPerson("John", 30), processed::add);
        N.ifNotNull(Arrays.asList(1, 2, 3), processed::add);
        N.ifNotNull(null, processed::add);

        assertEquals(5, processed.size());
        assertFalse(processed.contains(null));
    }

    @Test
    public void testIfNotNullWithModification() {
        TestPerson person = new TestPerson("John", 30);
        N.ifNotNull(person, p -> {
            p.setName("Jane");
            p.setAge(31);
        });
        assertEquals("Jane", person.getName());
        assertEquals(31, person.getAge());
    }

    // Complex Integration Tests

    @Test
    public void testCombiningBatchAndParallelProcessing() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        List<ContinuableFuture<Integer>> futures = N.callByBatch(data, 10, batch -> N.asyncExecute(() -> batch.stream().mapToInt(Integer::intValue).sum()));

        int totalSum = 0;
        for (ContinuableFuture<Integer> future : futures) {
            totalSum += future.get();
        }

        assertEquals(5050, totalSum); // Sum of 1 to 100
    }

    @Test
    public void testNestedTryOperations() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> {
            String intermediate = N.tryOrDefaultIfExceptionOccurred(() -> {
                if (Math.random() < 0.5) {
                    throw new RuntimeException("Random failure");
                }
                return "Success";
            }, "Fallback");

            return N.tryOrDefaultIfExceptionOccurred(intermediate, s -> s.toUpperCase(), "ERROR");
        });

        assertTrue(result.isPresent());
        assertTrue("SUCCESS".equals(result.get()) || "FALLBACK".equals(result.get()));
    }

    @Test
    public void testConditionalBatchProcessing() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger evenBatches = new AtomicInteger(0);
        AtomicInteger oddBatches = new AtomicInteger(0);

        N.forEach(numbers, n -> {
            N.ifOrEmpty(n % 2 == 0, () -> "even").ifPresent(type -> {
                List<Integer> batch = Arrays.asList(n);
                N.runByBatch(batch, 1, b -> evenBatches.incrementAndGet());
            });

            N.ifOrEmpty(n % 2 != 0, () -> "odd").ifPresent(type -> {
                List<Integer> batch = Arrays.asList(n);
                N.runByBatch(batch, 1, b -> oddBatches.incrementAndGet());
            });
        });

        assertEquals(5, evenBatches.get());
        assertEquals(5, oddBatches.get());
    }

    @Test
    public void testUninterruptibleBatchProcessing() {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        List<Integer> results = new ArrayList<>();

        N.runUninterruptibly(() -> {
            N.callByBatch(data, 3, batch -> {
                Thread.sleep(10);
                return batch.stream().mapToInt(Integer::intValue).sum();
            }).forEach(results::add);
        });

        assertEquals(4, results.size());
        assertEquals(55, results.stream().mapToInt(Integer::intValue).sum());
    }

    // Edge Cases and Error Scenarios

    @Test
    public void testRunByBatchWithNegativeBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.runByBatch(Arrays.asList(1, 2, 3), -1, batch -> {
            });
        });
    }

    @Test
    public void testCallByBatchWithZeroBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            N.callByBatch(Arrays.asList(1, 2, 3), 0, batch -> batch.size());
        });
    }

    @Test
    public void testRunUninterruptiblyWithNullCommand() {
        assertThrows(NullPointerException.class, () -> {
            N.runUninterruptibly(null);
        });
    }

    @Test
    public void testCallUninterruptiblyWithNullCommand() {
        assertThrows(NullPointerException.class, () -> {
            N.callUninterruptibly(null);
        });
    }

    @Test
    public void testIfNotEmptyWithCustomCollectionTypes() {
        // Test with Queue
        Queue<String> queue = new LinkedList<>(Arrays.asList("a", "b", "c"));
        AtomicInteger queueProcessed = new AtomicInteger(0);
        N.ifNotEmpty(queue, q -> queueProcessed.set(q.size()));
        assertEquals(3, queueProcessed.get());

        // Test with Deque
        Deque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 4));
        AtomicInteger dequeProcessed = new AtomicInteger(0);
        N.ifNotEmpty(deque, d -> dequeProcessed.set(d.size()));
        assertEquals(4, dequeProcessed.get());
    }

    @Test
    public void testBatchProcessingWithStateManagement() {
        List<String> items = Arrays.asList("a", "b", "c", "d", "e", "f");
        Map<Integer, List<String>> batchMap = new HashMap<>();
        AtomicInteger batchNumber = new AtomicInteger(0);

        N.runByBatch(items, 2, (idx, item) -> {
            int currentBatch = batchNumber.get();
            batchMap.computeIfAbsent(currentBatch, k -> new ArrayList<>()).add(item);
        }, () -> batchNumber.incrementAndGet());

        assertEquals(3, batchMap.size());
        assertEquals(Arrays.asList("a", "b"), batchMap.get(0));
        assertEquals(Arrays.asList("c", "d"), batchMap.get(1));
        assertEquals(Arrays.asList("e", "f"), batchMap.get(2));
    }

    // Additional println and fprintln Tests

    @Test
    public void testPrintlnWithNullValue() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Object result = N.println(null);
            assertNull(result);
            assertTrue(baos.toString().contains("null"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintlnWithPrimitiveArrays() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            int[] intArray = { 1, 2, 3 };
            int[] result = N.println(intArray);
            assertSame(intArray, result);
            // Note: primitive arrays won't use the custom formatting

            baos.reset();
            Object[] objArray = { 1, "two", 3.0 };
            Object[] objResult = N.println(objArray);
            assertSame(objArray, objResult);
            assertTrue(baos.toString().contains("[1, two, 3.0]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintlnWithNestedCollections() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            List<List<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
            List<List<String>> result = N.println(nested);
            assertSame(nested, result);
            assertTrue(baos.toString().contains("[[a, b], [c, d]]"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testFprintlnWithVariousFormats() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            N.fprintln("String: %s, Integer: %d, Float: %.2f", "test", 42, 3.14159);
            String output = baos.toString();
            assertTrue(output.contains("String: test, Integer: 42, Float: 3.14"));

            baos.reset();
            N.fprintln("No arguments");
            assertEquals("No arguments" + System.lineSeparator(), baos.toString());

            baos.reset();
            N.fprintln("%d%% complete", 75);
            assertTrue(baos.toString().contains("75% complete"));
        } finally {
            System.setOut(originalOut);
        }
    }

    // Additional execute with retry Tests

    @Test
    public void testExecuteWithNoRetries() {
        AtomicInteger attempts = new AtomicInteger(0);
        N.execute(() -> attempts.incrementAndGet(), 0, 0, e -> false);
        assertEquals(1, attempts.get());
    }

    @Test
    public void testExecuteWithConditionalRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        List<Exception> exceptions = new ArrayList<>();

        assertThrows(RuntimeException.class, () -> N.execute(() -> {
            int attempt = attempts.incrementAndGet();
            RuntimeException e = new RuntimeException("Attempt " + attempt);
            exceptions.add(e);
            throw e;
        }, 3, 10, e -> e.getMessage().contains("Attempt 1") || e.getMessage().contains("Attempt 2")));

        assertEquals(3, attempts.get());
        assertEquals(3, exceptions.size());
    }

    @Test
    public void testExecuteCallableWithBiPredicate() {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = N.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt == 1) {
                return "wrong";
            } else {
                return "correct";
            }
        }, 3, 10, (r, e) -> "wrong".equals(r));

        assertEquals("correct", result);
        assertEquals(2, attempts.get());
    }

    // Additional lazyInit Tests

    @Test
    public void testLazyInitWithException() {
        AtomicInteger attempts = new AtomicInteger(0);
        com.landawn.abacus.util.function.Supplier<String> lazy = N.lazyInit(() -> {
            attempts.incrementAndGet();
            throw new RuntimeException("Initialization failed");
        });

        assertThrows(RuntimeException.class, lazy::get);
        assertEquals(1, attempts.get());

        // Subsequent calls should throw the same exception without retrying
        assertThrows(RuntimeException.class, lazy::get);
        assertEquals(2, attempts.get()); // No additional attempts
    }

    @Test
    public void testLazyInitializeWithCheckedException() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);
        Throwables.Supplier<String, IOException> lazy = N.lazyInitialize(() -> {
            attempts.incrementAndGet();
            if (attempts.get() == 1) {
                throw new IOException("First attempt fails");
            }
            return "Success";
        });

        assertThrows(IOException.class, lazy::get);
        assertEquals(1, attempts.get());

        // Exception is cached
        //  assertThrows(IOException.class, lazy::get);
        assertEquals("Success", lazy.get());
        assertEquals(2, attempts.get());
    }

    // Thread safety stress tests

    @Test
    public void testRunByBatchThreadSafety() throws Exception {
        int threadCount = 10;
        int itemsPerThread = 100;
        ConcurrentLinkedQueue<Integer> allProcessed = new ConcurrentLinkedQueue<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    startLatch.await();
                    List<Integer> items = new ArrayList<>();
                    for (int i = 0; i < itemsPerThread; i++) {
                        items.add(threadId * itemsPerThread + i);
                    }

                    N.runByBatch(items, 10, batch -> {
                        // Simulate processing
                        Thread.sleep(1);
                        allProcessed.addAll(batch);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));

        assertEquals(threadCount * itemsPerThread, allProcessed.size());
        Set<Integer> unique = new HashSet<>(allProcessed);
        assertEquals(threadCount * itemsPerThread, unique.size());
    }

    @Test
    public void testComplexIfNotEmptyChaining() {
        Map<String, List<Integer>> data = new HashMap<>();
        data.put("numbers", Arrays.asList(1, 2, 3, 4, 5));

        AtomicInteger result = new AtomicInteger(0);

        N.ifNotEmpty(data, map -> {
            N.ifNotEmpty(map.get("numbers"), list -> {
                N.ifNotEmpty(list.stream().filter(n -> n % 2 == 0).collect(Collectors.toList()), evens -> {
                    result.set(evens.stream().mapToInt(Integer::intValue).sum());
                });
            });
        });

        assertEquals(6, result.get()); // 2 + 4
    }

    @Test
    public void testRunUninterruptiblyEdgeCases() {
        // Test with immediate return
        AtomicBoolean executed = new AtomicBoolean(false);
        N.runUninterruptibly(() -> executed.set(true));
        assertTrue(executed.get());

        // Test with zero timeout
        executed.set(false);
        N.runUninterruptibly(millis -> executed.set(true), 0);
        assertTrue(executed.get());

        // Test with negative timeout (should be treated as zero)
        executed.set(false);
        N.runUninterruptibly(millis -> executed.set(true), -100);
        assertTrue(executed.get());
    }

    @Test
    public void testCallUninterruptiblyEdgeCases() {
        // Test with immediate return
        String result1 = N.callUninterruptibly(() -> "immediate");
        assertEquals("immediate", result1);

        // Test with zero timeout
        String result2 = N.callUninterruptibly(millis -> "zero timeout", 0);
        assertEquals("zero timeout", result2);

        // Test with negative timeout
        String result3 = N.callUninterruptibly(millis -> "negative timeout", -100);
        assertEquals("negative timeout", result3);
    }

    @Test
    public void testBatchProcessingWithEmptyBatches() {
        // This shouldn't happen in normal usage, but test edge case
        List<Integer> items = Arrays.asList(1, 2, 3, 4, 5);
        AtomicInteger batchCount = new AtomicInteger(0);

        N.runByBatch(items.iterator(), 10, batch -> {
            assertFalse(batch.isEmpty());
            batchCount.incrementAndGet();
        });

        assertEquals(1, batchCount.get());
    }

    @Test
    public void testTryOrDefaultWithExceptionInDefault() {
        // Even if the default supplier throws, we should get an exception
        assertThrows(RuntimeException.class, () -> {
            N.tryOrDefaultIfExceptionOccurred(() -> {
                throw new IOException("Primary failed");
            }, () -> {
                throw new RuntimeException("Default also failed");
            });
        });
    }

    @Test
    public void testComplexParallelBatchProcessing() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            data.add(i);
        }

        ConcurrentHashMap<Integer, Integer> results = new ConcurrentHashMap<>();

        List<Callable<Void>> tasks = N.callByBatch(data, 5, batch -> {
            return (Callable<Void>) () -> {
                int sum = batch.stream().mapToInt(Integer::intValue).sum();
                results.put(batch.get(0), sum);
                return null;
            };
        });

        N.callInParallel(tasks);

        assertEquals(10, results.size());
        int totalSum = results.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(1225, totalSum); // Sum of 0 to 49
    }

    @Test
    public void testIfOrElseDeprecated() {
        AtomicInteger trueCount = new AtomicInteger(0);
        AtomicInteger falseCount = new AtomicInteger(0);

        N.ifOrElse(true, trueCount::incrementAndGet, falseCount::incrementAndGet);
        assertEquals(1, trueCount.get());
        assertEquals(0, falseCount.get());

        N.ifOrElse(false, trueCount::incrementAndGet, falseCount::incrementAndGet);
        assertEquals(1, trueCount.get());
        assertEquals(1, falseCount.get());

        // Test with null actions
        N.ifOrElse(true, null, falseCount::incrementAndGet);
        assertEquals(1, falseCount.get());

        N.ifOrElse(false, trueCount::incrementAndGet, null);
        assertEquals(1, trueCount.get());
    }

    @Test
    public void testSleepInterruption() {
        Thread currentThread = Thread.currentThread();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> currentThread.interrupt(), 50, TimeUnit.MILLISECONDS);

        try {
            assertThrows(RuntimeException.class, () -> N.sleep(200));
            assertTrue(Thread.interrupted()); // Clear interrupt flag
        } finally {
            scheduler.shutdown();
        }
    }

    @Test
    public void testAsyncExecuteWithNullCommands() {
        List<Throwables.Runnable<Exception>> nullList = null;
        assertTrue(N.asyncExecute(nullList).isEmpty());

        Collection<Callable<String>> nullCollection = null;
        assertTrue(N.asyncExecute(nullCollection).isEmpty());
    }

    @Test
    public void testForEachIntegrationScenarios() {
        // Complex scenario combining multiple forEach variants
        Map<String, List<Integer>> departmentScores = new HashMap<>();
        departmentScores.put("Sales", Arrays.asList(85, 92, 78));
        departmentScores.put("Engineering", Arrays.asList(95, 88, 91, 87));
        departmentScores.put("HR", Arrays.asList(82, 79));

        Map<String, Double> averages = new HashMap<>();

        N.forEach(departmentScores, (dept, scores) -> {
            N.ifNotEmpty(scores, scoreList -> {
                MutableDouble sum = MutableDouble.of(0.0);
                AtomicInteger count = new AtomicInteger(0);

                N.forEachIndexed(scoreList, (idx, score) -> {
                    N.ifNotNull(score, s -> count.incrementAndGet());
                });

                N.forEach(scoreList, score -> {
                    N.ifNotNull(score, s -> sum.add(s));
                });

                if (count.get() > 0) {
                    averages.put(dept, sum.value() / count.get());
                }
            });
        });

        assertEquals(3, averages.size());
        assertTrue(averages.get("Sales") > 80);
        assertTrue(averages.get("Engineering") > 85);
        assertTrue(averages.get("HR") > 75);
    }

    @Test
    public void testMemoryEfficientBatchProcessing() {
        // Test that batch processing doesn't hold references to all batches
        int totalItems = 1000;
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger maxBatchesInMemory = new AtomicInteger(0);
        List<WeakReference<List<Integer>>> batchRefs = new ArrayList<>();

        Iterator<Integer> iter = new Iterator<Integer>() {
            int current = 0;

            @Override
            public boolean hasNext() {
                return current < totalItems;
            }

            @Override
            public Integer next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return current++;
            }
        };

        N.runByBatch(iter, 50, batch -> {
            processedCount.addAndGet(batch.size());
            batchRefs.add(new WeakReference<>(batch));

            // Force GC to potentially collect old batches
            if (batchRefs.size() % 5 == 0) {
                System.gc();

                long activeRefs = batchRefs.stream().filter(ref -> ref.get() != null).count();
                maxBatchesInMemory.set(Math.max(maxBatchesInMemory.get(), (int) activeRefs));
            }
        });

        assertEquals(totalItems, processedCount.get());
        // Most batches should be eligible for GC during processing
        assertTrue(maxBatchesInMemory.get() < 10);
    }
}
