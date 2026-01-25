package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonDeserializationConfig;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;
import com.landawn.abacus.parser.JsonSerializationConfig;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.XmlSerializationConfig;
import com.landawn.abacus.parser.XmlSerializationConfig.XSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class N204Test extends TestBase {

    @TempDir
    Path tempDir;

    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    @BeforeEach
    public void setUp() {
        executorService = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    public void tearDown() {
        executorService.shutdownNow();
        scheduledExecutorService.shutdownNow();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Executor service did not terminate in time.");
            }
            if (!scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS)) {
                System.err.println("Scheduled executor service did not terminate in time.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class TestBean {
        private String name;
        private int value;
        private List<String> items;
        private Map<String, Integer> properties;

        public TestBean() {
        }

        public TestBean(String name, int value, List<String> items, Map<String, Integer> properties) {
            this.name = name;
            this.value = value;
            this.items = items;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }

        public Map<String, Integer> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Integer> properties) {
            this.properties = properties;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestBean testBean = (TestBean) o;
            return value == testBean.value && Objects.equals(name, testBean.name) && Objects.equals(items, testBean.items)
                    && Objects.equals(properties, testBean.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, items, properties);
        }
    }

    private TestBean createSampleBean() {
        List<String> items = new ArrayList<>(Arrays.asList("item1", "item2"));
        Map<String, Integer> props = new LinkedHashMap<>();
        props.put("key1", 100);
        props.put("key2", 200);
        return new TestBean("testName", 123, items, props);
    }

    private String getExpectedJsonForSampleBean(boolean pretty) {
        if (pretty) {
            return String.join(IOUtil.LINE_SEPARATOR_UNIX,
                    "{\n" + "    \"name\": \"testName\",\n" + "    \"value\": 123,\n" + "    \"items\": [\"item1\", \"item2\"],\n" + "    \"properties\": {\n"
                            + "        \"key1\": 100,\n" + "        \"key2\": 200\n" + "    }\n" + "}");
        } else {
            return "{\"name\": \"testName\", \"value\": 123, \"items\": [\"item1\", \"item2\"], \"properties\": {\"key1\": 100, \"key2\": 200}}";
        }
    }

    @Test
    public void toJson_object() {
        TestBean bean = createSampleBean();
        String json = N.toJson(bean);
        N.println(json);
        assertEquals(getExpectedJsonForSampleBean(false), json);
    }

    @Test
    public void toJson_object_prettyFormat() {
        TestBean bean = createSampleBean();
        String json = N.toJson(bean, true);
        assertTrue(json.contains("\"name\": \"testName\""));
        assertTrue(json.contains("\n"));
    }

    @Test
    public void toJson_object_withConfig() {
        TestBean bean = createSampleBean();
        JsonSerializationConfig config = JSC.create().prettyFormat(true);
        String json = N.toJson(bean, config);
        assertEquals(getExpectedJsonForSampleBean(true), json);
    }

    @Test
    public void toJson_object_toFile(@TempDir Path tempDir) throws IOException {
        TestBean bean = createSampleBean();
        File outputFile = tempDir.resolve("output.json").toFile();
        N.toJson(bean, outputFile);
        assertTrue(outputFile.exists());
        String fileContent = new String(Files.readAllBytes(outputFile.toPath()));
        assertEquals(getExpectedJsonForSampleBean(false), fileContent);
    }

    @Test
    public void toJson_object_withConfig_toFile(@TempDir Path tempDir) throws IOException {
        TestBean bean = createSampleBean();
        File outputFile = tempDir.resolve("output_pretty.json").toFile();
        JsonSerializationConfig config = JSC.create().prettyFormat(true);
        N.toJson(bean, config, outputFile);
        assertTrue(outputFile.exists());
        String fileContent = new String(Files.readAllBytes(outputFile.toPath()));
        assertEquals(getExpectedJsonForSampleBean(true), fileContent);
    }

    @Test
    public void toJson_object_toOutputStream() throws IOException {
        TestBean bean = createSampleBean();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toJson(bean, baos);
        assertEquals(getExpectedJsonForSampleBean(false), baos.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    public void toJson_object_withConfig_toOutputStream() throws IOException {
        TestBean bean = createSampleBean();
        JsonSerializationConfig config = JSC.create().prettyFormat(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        N.toJson(bean, config, baos);
        assertEquals(getExpectedJsonForSampleBean(true), baos.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    public void toJson_object_toWriter() throws IOException {
        TestBean bean = createSampleBean();
        StringWriter writer = new StringWriter();
        N.toJson(bean, writer);
        assertEquals(getExpectedJsonForSampleBean(false), writer.toString());
    }

    @Test
    public void toJson_object_withConfig_toWriter() throws IOException {
        TestBean bean = createSampleBean();
        JsonSerializationConfig config = JSC.create().prettyFormat(true);
        StringWriter writer = new StringWriter();
        N.toJson(bean, config, writer);
        assertEquals(getExpectedJsonForSampleBean(true), writer.toString());
    }

    @Test
    public void fromJson_string_toClass() {
        String json = getExpectedJsonForSampleBean(false);
        TestBean bean = N.fromJson(json, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_toType() {
        String json = getExpectedJsonForSampleBean(false);
        Type<TestBean> type = new TypeReference<TestBean>() {
        }.type();
        TestBean bean = N.fromJson(json, type);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withDefault_toClass() {
        String json = null;
        TestBean defaultBean = new TestBean("default", 0, null, null);
        TestBean bean = N.fromJson(json, defaultBean, TestBean.class);
        assertEquals(defaultBean, bean);

        String validJson = getExpectedJsonForSampleBean(false);
        bean = N.fromJson(validJson, defaultBean, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withDefault_toType() {
        String json = null;
        Type<TestBean> type = new TypeReference<TestBean>() {
        }.type();
        TestBean defaultBean = new TestBean("default", 0, null, null);
        TestBean bean = N.fromJson(json, defaultBean, type);
        assertEquals(defaultBean, bean);

        String validJson = getExpectedJsonForSampleBean(false);
        bean = N.fromJson(validJson, defaultBean, type);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withConfig_toClass() {
        String json = getExpectedJsonForSampleBean(false);
        JsonDeserializationConfig config = JDC.create();
        TestBean bean = N.fromJson(json, config, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_string_withConfig_toType() {
        String json = getExpectedJsonForSampleBean(false);
        JsonDeserializationConfig config = JDC.create();
        Type<TestBean> type = new TypeReference<TestBean>() {
        }.type();
        TestBean bean = N.fromJson(json, config, type);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_file_toClass(@TempDir Path tempDir) throws IOException {
        String jsonContent = getExpectedJsonForSampleBean(false);
        File inputFile = tempDir.resolve("input.json").toFile();
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(jsonContent);
        }
        TestBean bean = N.fromJson(inputFile, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void fromJson_inputStream_toClass() throws IOException {
        String jsonContent = getExpectedJsonForSampleBean(false);
        InputStream inputStream = new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8));
        TestBean bean = N.fromJson(inputStream, TestBean.class);
        assertEquals(createSampleBean(), bean);
        inputStream.close();
    }

    @Test
    public void fromJson_reader_toClass() throws IOException {
        String jsonContent = getExpectedJsonForSampleBean(false);
        Reader reader = new StringReader(jsonContent);
        TestBean bean = N.fromJson(reader, TestBean.class);
        assertEquals(createSampleBean(), bean);
        reader.close();
    }

    @Test
    public void fromJson_substring_toClass() {
        String prefix = "###";
        String suffix = "@@@";
        String actualJson = getExpectedJsonForSampleBean(false);
        String jsonWithPadding = prefix + actualJson + suffix;
        TestBean bean = N.fromJson(jsonWithPadding, prefix.length(), jsonWithPadding.length() - suffix.length(), TestBean.class);
        assertEquals(createSampleBean(), bean);

        assertThrows(IndexOutOfBoundsException.class, () -> N.fromJson("{}", 0, 10, TestBean.class));
    }

    @Test
    public void streamJson_string_toClass() {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "," + getExpectedJsonForSampleBean(false) + "]";
        Stream<TestBean> stream = N.streamJson(jsonArray, Type.of(TestBean.class));
        List<TestBean> list = stream.toList();
        assertEquals(2, list.size());
        assertEquals(createSampleBean(), list.get(0));
        assertEquals(createSampleBean(), list.get(1));
    }

    @Test
    public void streamJson_file_toClass(@TempDir Path tempDir) throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        File inputFile = tempDir.resolve("input_array.json").toFile();
        try (FileWriter writer = new FileWriter(inputFile)) {
            writer.write(jsonArray);
        }
        Stream<TestBean> stream = N.streamJson(inputFile, Type.of(TestBean.class));
        assertEquals(createSampleBean(), stream.first().orElse(null));
    }

    @Test
    public void streamJson_inputStream_toClass_autoClose() throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        final AtomicBoolean closed = new AtomicBoolean(false);
        InputStream inputStream = new ByteArrayInputStream(jsonArray.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                super.close();
                closed.set(true);
            }
        };

        try (Stream<TestBean> stream = N.streamJson(inputStream, true, Type.of(TestBean.class))) {
            assertEquals(createSampleBean(), stream.first().orElse(null));
        }
        assertTrue(closed.get(), "InputStream should be closed when stream is closed with autoClose=true");
    }

    @Test
    public void streamJson_reader_toClass_autoClose() throws IOException {
        String jsonArray = "[" + getExpectedJsonForSampleBean(false) + "]";
        final AtomicBoolean closed = new AtomicBoolean(false);
        Reader reader = new StringReader(jsonArray) {
            @Override
            public void close() {
                super.close();
                closed.set(true);
            }
        };

        try (Stream<TestBean> stream = N.streamJson(reader, true, Type.of(TestBean.class))) {
            assertEquals(createSampleBean(), stream.first().orElse(null));
        }
        assertTrue(closed.get(), "Reader should be closed when stream is closed with autoClose=true");
    }

    @Test
    public void formatJson_string() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String prettyJson = N.formatJson(uglyJson);
        assertTrue(prettyJson.contains("\n"));
        assertEquals(N.fromJson(uglyJson, Object.class), N.fromJson(prettyJson, Object.class));
    }

    @Test
    public void formatJson_string_withClass() {
        String uglyJson = getExpectedJsonForSampleBean(false);
        String prettyJson = N.formatJson(uglyJson, TestBean.class);
        assertEquals(getExpectedJsonForSampleBean(true), prettyJson);
    }

    private String getExpectedXmlForSampleBean(boolean pretty) {
        if (pretty) {
            return String.join(IOUtil.LINE_SEPARATOR_UNIX, "<TestBean>", "  <name>testName</name>", "  <value>123</value>", "  <items>",
                    "    <item>item1</item>", "    <item>item2</item>", "  </items>", "  <properties>", "    <entry>", "      <key>key1</key>",
                    "      <value>100</value>", "    </entry>", "    <entry>", "      <key>key2</key>", "      <value>200</value>", "    </entry>",
                    "  </properties>", "</TestBean>");
        } else {
            return "<TestBean><name>testName</name><value>123</value><items><item>item1</item><item>item2</item></items><properties><entry><key>key1</key><value>100</value></entry><entry><key>key2</key><value>200</value></entry></properties></TestBean>";
        }
    }

    @Test
    public void toXml_object() {
        TestBean bean = createSampleBean();
        String xml = N.toXml(bean);
        assertTrue(xml.contains("<testBean>"));
        assertTrue(xml.contains("<name>testName</name>"));
        assertTrue(xml.contains("<value>123</value>"));
        assertTrue(xml.contains("<items>[&quot;item1&quot;, &quot;item2&quot;]</items>"));
        assertTrue(xml.contains("<key1>100</key1>") || xml.contains("<key>key1</key><value>100</value>"));
    }

    @Test
    public void toXml_object_prettyFormat() {
        TestBean bean = createSampleBean();
        String xml = N.toXml(bean, true);
        assertTrue(xml.contains("<testBean>"));
        assertTrue(xml.contains("<name>testName</name>"));
    }

    @Test
    public void toXml_object_withConfig_toWriter() throws IOException {
        TestBean bean = createSampleBean();
        XmlSerializationConfig config = XSC.create().prettyFormat(true);
        StringWriter writer = new StringWriter();
        N.toXml(bean, config, writer);
        String xml = writer.toString();
        assertTrue(xml.contains("<testBean>"));
        assertTrue(xml.contains("<name>testName</name>"));
    }

    @Test
    public void fromXml_string_toClass() {
        String xml = "<NTest_TestBean><name>testName</name><value>123</value><items><String>item1</String><String>item2</String></items><properties><entry><key>key1</key><Object>100</Object></entry><entry><key>key2</key><Object>200</Object></entry></properties></NTest_TestBean>";
        String generatedXml = N.toXml(createSampleBean());

        TestBean bean = N.fromXml(generatedXml, TestBean.class);
        assertEquals(createSampleBean(), bean);
    }

    @Test
    public void xmlToJson_string() {
        String xml = N.toXml(createSampleBean());
        String json = N.xmlToJson(xml);

        Map<String, Object> map = N.fromJson(json, Map.class);
        assertNotNull(map);
        assertEquals("testName", map.get("name"));
    }

    @Test
    public void jsonToXml_string() {
        String json = N.toJson(createSampleBean());
        String xml = N.jsonToXml(json);

        MapEntity mapEntity = N.fromXml(xml, MapEntity.class);
        assertNotNull(mapEntity);
        assertEquals("testName", mapEntity.get("name"));
        assertEquals("123", mapEntity.get("value"));
    }

    @Test
    public void forEach_intRange_runnable() {
        AtomicInteger count = new AtomicInteger(0);
        N.forEach(0, 5, count::incrementAndGet);
        assertEquals(5, count.get());

        count.set(0);
        N.forEach(5, 0, count::incrementAndGet);
        assertEquals(0, count.get());
    }

    @Test
    public void forEach_intRange_withStep_runnable() {
        AtomicInteger count = new AtomicInteger(0);
        N.forEach(0, 5, 2, count::incrementAndGet);
        assertEquals(3, count.get());

        count.set(0);
        N.forEach(5, 0, -1, count::incrementAndGet);
        assertEquals(5, count.get());

        assertThrows(IllegalArgumentException.class, () -> N.forEach(0, 5, 0, count::incrementAndGet));
    }

    @Test
    public void forEach_intRange_intConsumer() {
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(0, 5, sum::addAndGet);
        assertEquals(10, sum.get());
    }

    @Test
    public void forEach_intRange_withStep_intConsumer() {
        List<Integer> result = new ArrayList<>();
        N.forEach(0, 6, 2, result::add);
        assertEquals(Arrays.asList(0, 2, 4), result);

        result.clear();
        N.forEach(5, -1, -2, result::add);
        assertEquals(Arrays.asList(5, 3, 1), result);
    }

    @Test
    public void forEach_intRange_withObject_intObjConsumer() {
        StringBuilder sb = new StringBuilder();
        String prefix = "val:";
        N.forEach(0, 3, prefix, (i, p) -> sb.append(p).append(i).append(" "));
        assertEquals("val:0 val:1 val:2 ", sb.toString());
    }

    @Test
    public void forEach_array_consumer() {
        String[] array = { "a", "b", "c" };
        List<String> result = new ArrayList<>();
        N.forEach(array, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        N.forEach((String[]) null, result::add);
        assertTrue(result.size() == 3);

        N.forEach(new String[0], result::add);
        assertTrue(result.size() == 3);
    }

    @Test
    public void forEach_array_fromIndex_toIndex_consumer() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEach(array, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        result.clear();
        N.forEach(array, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);

        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(array, 0, 10, result::add));
        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(array, -1, 2, result::add));
    }

    @Test
    public void forEach_iterable_consumer() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list, result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        N.forEach((List<String>) null, result::add);
        assertTrue(result.size() == 3);
    }

    @Test
    public void forEach_iterator_consumer() {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = new ArrayList<>();
        N.forEach(list.iterator(), result::add);
        assertEquals(Arrays.asList("a", "b", "c"), result);

        N.forEach((Iterator<String>) null, result::add);
        assertTrue(result.size() == 3);
    }

    @Test
    public void forEach_collection_fromIndex_toIndex_consumer() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();
        N.forEach(list, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        Collection<String> collection = new java.util.LinkedList<>(list);
        result.clear();
        N.forEach(collection, 1, 4, result::add);
        assertEquals(Arrays.asList("b", "c", "d"), result);

        result.clear();
        N.forEach(list, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);

        result.clear();
        N.forEach(collection, 3, 1, result::add);
        assertEquals(Arrays.asList("d", "c"), result);

        assertThrows(IndexOutOfBoundsException.class, () -> N.forEach(list, 0, 10, result::add));
    }

    @Test
    public void forEach_map_entryConsumer() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        AtomicInteger sum = new AtomicInteger(0);
        N.forEach(map, (Throwables.Consumer<Map.Entry<String, Integer>, RuntimeException>) entry -> sum.addAndGet(entry.getValue()));
        assertEquals(3, sum.get());
    }

    @Test
    public void forEach_map_biConsumer() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Map<String, Integer> resultMap = new HashMap<>();
        N.forEach(map, (Throwables.BiConsumer<String, Integer, RuntimeException>) resultMap::put);
        assertEquals(map, resultMap);
    }

    @Test
    public void forEach_iterable_consumer_threaded() throws InterruptedException {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger sum = new AtomicInteger(0);
        int numThreads = 2;

        N.forEach(list, (Throwables.Consumer<Integer, RuntimeException>) e -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            sum.addAndGet(e);
        }, numThreads, executorService);

        assertEquals(55, sum.get());
    }

    @Test
    public void forEach_iterator_consumer_threaded_withException() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        AtomicInteger processedCount = new AtomicInteger(0);
        Throwables.Consumer<Integer, Exception> consumerWithException = val -> {
            processedCount.incrementAndGet();
            if (val == 3) {
                throw new IOException("Test exception");
            }
        };

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> N.forEach(list.iterator(), consumerWithException, 2, executorService));
        assertTrue(thrown.getCause() instanceof IOException
                || (thrown.getCause() instanceof ExecutionException && thrown.getCause().getCause() instanceof IOException));
        assertTrue(processedCount.get() >= 1 && processedCount.get() <= list.size());
    }

    @Test
    public void forEach_array_flatMap_biConsumer() throws Exception {
        String[] array = { "a", "b" };
        List<Tuple.Tuple2<String, Integer>> result = new ArrayList<>();
        N.forEach(array, s -> s.equals("a") ? Arrays.asList(1, 2) : Arrays.asList(3, 4), (s, i) -> result.add(Tuple.of(s, i)));

        List<Tuple.Tuple2<String, Integer>> expected = Arrays.asList(Tuple.of("a", 1), Tuple.of("a", 2), Tuple.of("b", 3), Tuple.of("b", 4));
        assertEquals(expected, result);
    }

    @Test
    public void forEach_arrays_biConsumer_shortCircuit() throws Exception {
        String[] a = { "one", "two", "three" };
        Integer[] b = { 1, 2 };
        List<String> result = new ArrayList<>();
        N.forEach(a, b, (s, i) -> result.add(s + ":" + i));
        assertEquals(Arrays.asList("one:1", "two:2"), result);
    }

    @Test
    public void forEach_arrays_biConsumer_withDefaults() throws Exception {
        String[] a = { "one", "two" };
        Integer[] b = { 1, 2, 3 };
        String defaultA = "defaultA";
        Integer defaultB = -1;
        List<String> result = new ArrayList<>();
        N.forEach(a, b, defaultA, defaultB, (s, i) -> result.add(s + ":" + i));

        assertEquals(Arrays.asList("one:1", "two:2", "defaultA:3"), result);
    }

    @Test
    public void forEachNonNull_array_consumer() throws Exception {
        String[] array = { "a", null, "c", null, "e" };
        List<String> result = new ArrayList<>();
        N.forEachNonNull(array, result::add);
        assertEquals(Arrays.asList("a", "c", "e"), result);
    }

    @Test
    public void forEachIndexed_array_intObjConsumer() throws Exception {
        String[] array = { "x", "y", "z" };
        List<String> result = new ArrayList<>();
        N.forEachIndexed(array, (idx, val) -> result.add(idx + ":" + val));
        assertEquals(Arrays.asList("0:x", "1:y", "2:z"), result);
    }

    @Test
    public void forEachIndexed_collection_fromIndex_toIndex_intObjConsumer() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();
        N.forEachIndexed(list, 1, 4, (idx, val) -> result.add(idx + ":" + val));
        assertEquals(Arrays.asList("1:b", "2:c", "3:d"), result);

        result.clear();
        N.forEachIndexed(list, 3, 1, (idx, val) -> result.add(idx + ":" + val));
        assertEquals(Arrays.asList("3:d", "2:c"), result);
    }

    @Test
    public void forEachIndexed_iterable_consumer_threaded() throws InterruptedException {
        List<String> data = Arrays.asList("a", "b", "c", "d", "e", "f");
        Map<Integer, String> resultMap = Collections.synchronizedMap(new HashMap<>());
        int numThreads = 3;

        N.forEachIndexed(data, (Throwables.IntObjConsumer<String, InterruptedException>) (idx, val) -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
            resultMap.put(idx, val);
        }, numThreads, executorService);

        assertEquals(data.size(), resultMap.size());
        for (int i = 0; i < data.size(); i++) {
            assertEquals(data.get(i), resultMap.get(i));
        }
    }

    @Test
    public void forEachPair_array_biConsumer() throws Exception {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> result = new ArrayList<>();
        N.forEachPair(array, (e1, e2) -> result.add(e1 + (e2 == null ? "_null" : e2)));
        assertEquals(Arrays.asList("ab", "bc", "cd", "de"), result);

        result.clear();
        N.forEachPair(new String[] { "a" }, (e1, e2) -> result.add(e1 + (e2 == null ? "_null" : e2)));
        assertEquals(Arrays.asList("a_null"), result);

        result.clear();
        N.forEachPair(array, 2, (e1, e2) -> result.add(e1 + (e2 == null ? "_null" : e2)));
        assertEquals(Arrays.asList("ab", "cd", "e_null"), result);
    }

    @Test
    public void forEachTriple_iterable_triConsumer() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        List<String> result = new ArrayList<>();
        N.forEachTriple(list, (e1, e2, e3) -> result.add(e1 + (e2 == null ? "_null" : e2) + (e3 == null ? "_null" : e3)));
        assertEquals(Arrays.asList("abc", "bcd", "cde"), result);

        result.clear();
        N.forEachTriple(list, 2, (e1, e2, e3) -> result.add(e1 + (e2 == null ? "_null" : e2) + (e3 == null ? "_null" : e3)));
        assertEquals(Arrays.asList("abc", "cde"), result);
    }

    @Test
    public void execute_runnable_withRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Throwables.Runnable<IOException> flakyRunnable = () -> {
            attempts.incrementAndGet();
            if (attempts.get() < 3) {
                throw new IOException("Temporary failure");
            }
        };

        N.runWithRetry(flakyRunnable, 3, 10, e -> e instanceof IOException);
        assertEquals(3, attempts.get());

        attempts.set(0);
        Throwables.Runnable<IOException> failingRunnable = () -> {
            attempts.incrementAndGet();
            throw new IOException("Persistent failure");
        };
        assertThrows(RuntimeException.class, () -> N.runWithRetry(failingRunnable, 2, 10, e -> e instanceof IOException));
        assertEquals(3, attempts.get());
    }

    @Test
    public void execute_callable_withRetry() {
        AtomicInteger attempts = new AtomicInteger(0);
        Callable<String> flakyCallable = () -> {
            attempts.incrementAndGet();
            if (attempts.get() < 2) {
                throw new ExecutionException("Temp fail", new IOException());
            }
            return "Success";
        };

        String result = N.callWithRetry(flakyCallable, 3, 10, (res, e) -> e != null && e.getCause() instanceof IOException);
        assertEquals("Success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    public void asyncExecute_runnable() throws ExecutionException, InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);
        ContinuableFuture<Void> future = N.asyncExecute(() -> {
            Thread.sleep(50);
            executed.set(true);
        });
        future.get();
        assertTrue(executed.get());
    }

    @Test
    public void asyncExecute_callable_withExecutor() throws ExecutionException, InterruptedException {
        Callable<String> task = () -> {
            Thread.sleep(50);
            return "done";
        };
        ContinuableFuture<String> future = N.asyncExecute(task, executorService);
        assertEquals("done", future.get());
    }

    @Test
    public void asyncExecute_runnable_withDelay() throws ExecutionException, InterruptedException {
        AtomicLong startTime = new AtomicLong(0);
        AtomicLong execTime = new AtomicLong(0);
        long delay = 100;

        ContinuableFuture<Void> future = N.asyncExecute(() -> {
            execTime.set(System.currentTimeMillis());
        }, delay);

        startTime.set(System.currentTimeMillis());
        future.get();
        assertTrue(execTime.get() - startTime.get() >= (delay - 20), "Execution was not delayed enough. Diff: " + (execTime.get() - startTime.get()));
    }

    @Test
    public void asyncExecute_listOfRunnables() throws ExecutionException, InterruptedException {
        List<Throwables.Runnable<Exception>> tasks = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        tasks.add(counter::incrementAndGet);
        tasks.add(counter::incrementAndGet);

        List<ContinuableFuture<Void>> futures = N.asyncExecute(tasks, executorService);
        for (ContinuableFuture<Void> f : futures) {
            f.get();
        }
        assertEquals(2, counter.get());
    }

    @Test
    public void asyncRun_collectionOfRunnables() {
        List<Throwables.Runnable<Exception>> tasks = new ArrayList<>();
        AtomicInteger count = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(2);

        tasks.add(() -> {
            Thread.sleep(50);
            count.incrementAndGet();
            latch.countDown();
        });
        tasks.add(() -> {
            Thread.sleep(20);
            count.incrementAndGet();
            latch.countDown();
        });

        ObjIterator<Void> iter = N.asyncRun(tasks, executorService);
        while (iter.hasNext())
            iter.next();

        try {
            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        assertEquals(2, count.get());
    }

    @Test
    public void asyncCall_collectionOfCallables() {
        List<Callable<Integer>> tasks = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(2);

        tasks.add(() -> {
            Thread.sleep(50);
            latch.countDown();
            return 1;
        });
        tasks.add(() -> {
            Thread.sleep(20);
            latch.countDown();
            return 2;
        });

        ObjIterator<Integer> iter = N.asyncCall(tasks, executorService);
        List<Integer> results = new ArrayList<>();
        while (iter.hasNext()) {
            results.add(iter.next());
        }

        try {
            assertTrue(latch.await(500, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail("Test interrupted");
        }
        assertEquals(2, results.size());
        assertTrue(results.contains(1));
        assertTrue(results.contains(2));
    }

    @Test
    public void runInParallel_twoRunnables() {
        AtomicInteger count = new AtomicInteger(0);
        Throwables.Runnable<Exception> r1 = () -> {
            Thread.sleep(50);
            count.incrementAndGet();
        };
        Throwables.Runnable<Exception> r2 = () -> {
            Thread.sleep(50);
            count.incrementAndGet();
        };
        N.runInParallel(r1, r2);
        assertEquals(2, count.get());
    }

    @Test
    public void callInParallel_twoCallables() {
        Callable<String> c1 = () -> {
            Thread.sleep(50);
            return "first";
        };
        Callable<Integer> c2 = () -> {
            Thread.sleep(50);
            return 123;
        };
        Tuple.Tuple2<String, Integer> result = N.callInParallel(c1, c2);
        assertEquals("first", result._1);
        assertEquals(Integer.valueOf(123), result._2);
    }

    @Test
    public void runInParallel_collectionOfRunnables() {
        AtomicInteger count = new AtomicInteger(0);
        List<Throwables.Runnable<Exception>> tasks = Arrays.asList(() -> {
            Thread.sleep(30);
            count.incrementAndGet();
        }, () -> {
            Thread.sleep(30);
            count.incrementAndGet();
        }, () -> {
            Thread.sleep(30);
            count.incrementAndGet();
        });
        N.runInParallel(tasks, executorService);
        assertEquals(3, count.get());
    }

    @Test
    public void callInParallel_collectionOfCallables() {
        List<Callable<Integer>> tasks = Arrays.asList(() -> {
            Thread.sleep(30);
            return 1;
        }, () -> {
            Thread.sleep(30);
            return 2;
        }, () -> {
            Thread.sleep(30);
            return 3;
        });
        List<Integer> results = N.callInParallel(tasks, executorService);
        assertEquals(Arrays.asList(1, 2, 3), results);
        assertTrue(results.containsAll(Arrays.asList(1, 2, 3)) && results.size() == 3);
    }

    @Test
    public void runInParallel_exceptionHandling() {
        Throwables.Runnable<Exception> r1 = () -> {
            throw new IOException("Task 1 failed");
        };
        AtomicBoolean r2Executed = new AtomicBoolean(false);
        Throwables.Runnable<Exception> r2 = () -> {
            Thread.sleep(200);
            r2Executed.set(true);
        };

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> N.runInParallel(r1, r2));
        assertTrue(thrown.getCause() instanceof IOException);
    }

    @Test
    public void runByBatch_array() {
        Integer[] array = { 1, 2, 3, 4, 5, 6, 7 };
        List<List<Integer>> batches = new ArrayList<>();
        N.runByBatch(array, 3, (Throwables.Consumer<List<Integer>, RuntimeException>) batches::add);

        assertEquals(3, batches.size());
        assertEquals(Arrays.asList(1, 2, 3), batches.get(0));
        assertEquals(Arrays.asList(4, 5, 6), batches.get(1));
        assertEquals(Arrays.asList(7), batches.get(2));
    }

    @Test
    public void runByBatch_iterable_withElementConsumerAndBatchAction() {
        List<Integer> data = Arrays.asList(10, 20, 30, 40, 50);
        AtomicInteger elementSumInBatch = new AtomicInteger(0);
        List<Integer> batchSums = new ArrayList<>();

        N.runByBatch(data, 2, (Throwables.IntObjConsumer<Integer, RuntimeException>) (idx, val) -> {
            elementSumInBatch.addAndGet(val);
        }, (Throwables.Runnable<RuntimeException>) () -> {
            batchSums.add(elementSumInBatch.get());
            elementSumInBatch.set(0);
        });

        assertEquals(3, batchSums.size());
        assertEquals(Arrays.asList(30, 70, 50), batchSums);
    }

    @Test
    public void callByBatch_iterator() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> batchSums = N.callByBatch(list.iterator(), 2,
                (Throwables.Function<List<Integer>, Integer, RuntimeException>) batch -> batch.stream().mapToInt(Integer::intValue).sum());

        assertEquals(3, batchSums.size());
        assertEquals(Arrays.asList(3, 7, 5), batchSums);
    }

    @Test
    public void runUninterruptibly_runnable() {
        AtomicBoolean executed = new AtomicBoolean(false);
        N.runUninterruptibly(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void callUninterruptibly_callable() {
        String result = N.callUninterruptibly(() -> "done");
        assertEquals("done", result);
    }

    @Test
    public void tryOrEmptyIfExceptionOccurred_callable() {
        Nullable<String> result = N.tryOrEmptyIfExceptionOccurred(() -> "success");
        assertTrue(result.isPresent());
        assertEquals("success", result.get());

        result = N.tryOrEmptyIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        });
        assertFalse(result.isPresent());
    }

    @Test
    public void tryOrDefaultIfExceptionOccurred_callable_withDefaultValue() {
        String result = N.tryOrDefaultIfExceptionOccurred(() -> "success", "default");
        assertEquals("success", result);

        result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        }, "default");
        assertEquals("default", result);
    }

    @Test
    public void tryOrDefaultIfExceptionOccurred_callable_withDefaultSupplier() {
        Supplier<String> defaultSupplier = () -> "defaultSupplier";
        String result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> "success", defaultSupplier);
        assertEquals("success", result);

        result = N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new Exception("fail");
        }, (java.util.function.Supplier<String>) defaultSupplier);
        assertEquals("defaultSupplier", result);
    }

    @Test
    public void ifOrEmpty_boolean_supplier() throws Exception {
        Nullable<String> result = N.ifOrEmpty(true, () -> "supplied");
        assertTrue(result.isPresent());
        assertEquals("supplied", result.get());

        result = N.ifOrEmpty(false, () -> "supplied");
        assertFalse(result.isPresent());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void ifOrElse_deprecated() {
        AtomicBoolean trueAction = new AtomicBoolean(false);
        AtomicBoolean falseAction = new AtomicBoolean(false);
        N.ifOrElse(true, () -> trueAction.set(true), () -> falseAction.set(true));
        assertTrue(trueAction.get());
        assertFalse(falseAction.get());

        trueAction.set(false);
        falseAction.set(false);
        N.ifOrElse(false, () -> trueAction.set(true), () -> falseAction.set(true));
        assertFalse(trueAction.get());
        assertTrue(falseAction.get());
    }

    @Test
    public void ifNotNull_consumer() throws Exception {
        AtomicReference<String> ref = new AtomicReference<>();
        N.ifNotNull("test", ref::set);
        assertEquals("test", ref.get());

        ref.set(null);
        N.ifNotNull(null, (String s) -> ref.set("should not happen"));
        assertNull(ref.get());
    }

    @Test
    public void ifNotEmpty_charSequence_consumer() throws Exception {
        AtomicReference<CharSequence> ref = new AtomicReference<>();
        N.ifNotEmpty("test", ref::set);
        assertEquals("test", ref.get().toString());

        ref.set(null);
        N.ifNotEmpty("", (CharSequence cs) -> ref.set("fail"));
        assertNull(ref.get());

        N.ifNotEmpty((String) null, (CharSequence cs) -> ref.set("fail"));
        assertNull(ref.get());
    }

    @Test
    public void sleep_millis() {
        long start = System.nanoTime();
        N.sleep(10);
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(duration >= 10, "Sleep duration was less than expected.");
    }

    @Test
    public void sleepUninterruptibly_millis() {
        long start = System.nanoTime();
        N.sleepUninterruptibly(10);
        long end = System.nanoTime();
        assertTrue((end - start) >= TimeUnit.MILLISECONDS.toNanos(10) - TimeUnit.MILLISECONDS.toNanos(5));
    }

    @Test
    public void lazyInit_abacusSupplier() {
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Supplier<String> lazySupplier = N.lazyInit(() -> {
            supplierCallCount.incrementAndGet();
            return "lazyValue";
        });

        assertEquals(0, supplierCallCount.get(), "Supplier should not be called before get()");
        assertEquals("lazyValue", lazySupplier.get());
        assertEquals(1, supplierCallCount.get(), "Supplier should be called once on first get()");
        assertEquals("lazyValue", lazySupplier.get());
        assertEquals(1, supplierCallCount.get(), "Supplier should not be called again on subsequent gets()");
    }

    @Test
    public void lazyInitialize_throwablesSupplier() throws Exception {
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        Throwables.Supplier<String, IOException> lazySupplier = N.lazyInitialize(() -> {
            supplierCallCount.incrementAndGet();
            if (supplierCallCount.get() == 1)
                return "firstCall";
            throw new IOException("Simulated failure on subsequent init (should not happen)");
        });

        assertEquals(0, supplierCallCount.get());
        assertEquals("firstCall", lazySupplier.get());
        assertEquals(1, supplierCallCount.get());
        assertEquals("firstCall", lazySupplier.get());
        assertEquals(1, supplierCallCount.get());
    }

    @Test
    public void toRuntimeException_exception() {
        Exception checkedEx = new IOException("checked");
        RuntimeException runtimeEx = N.toRuntimeException(checkedEx);
        assertNotNull(runtimeEx);
        assertEquals(checkedEx, runtimeEx.getCause());

        RuntimeException originalRuntimeEx = new IllegalArgumentException("original_runtime");
        assertSame(originalRuntimeEx, N.toRuntimeException(originalRuntimeEx));
    }

    @Test
    public void toRuntimeException_throwable() {
        Throwable error = new OutOfMemoryError("error");
        assertThrows(OutOfMemoryError.class, () -> ExceptionUtil.toRuntimeException(error, true, true));

        Throwable checkedEx = new ClassNotFoundException("checked_throwable");
        RuntimeException runtimeEx = N.toRuntimeException(checkedEx);
        assertNotNull(runtimeEx);
        assertEquals(checkedEx, runtimeEx.getCause());
    }

    @Test
    public void println_object() {
        N.println("Test string");
        N.println(123);
        N.println(createSampleBean());
        N.println(Arrays.asList("a", "b"));
        N.println(new String[] { "c", "d" });
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        N.println(map);
        N.println((Object) null);
    }

    @Test
    public void fprintln_format() {
        N.fprintln("Hello, %s! You are %d.", "World", 30);
    }
}
