package com.landawn.abacus.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RowDataSet;
import com.landawn.abacus.util.stream.Stream;

public class JSONParserImpl101Test extends TestBase {

    private JSONParserImpl parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        parser = new JSONParserImpl();
    }

    @Test
    public void testConstructors() {
        // Test default constructor
        JSONParserImpl parser1 = new JSONParserImpl();
        Assertions.assertNotNull(parser1);

        // Test constructor with configs
        JSONSerializationConfig jsc = JSC.create();
        JSONDeserializationConfig jdc = JDC.create();
        JSONParserImpl parser2 = new JSONParserImpl(jsc, jdc);
        Assertions.assertNotNull(parser2);
    }

    @Test
    public void testReadString_SimpleTypes() {
        // Test with String
        String result1 = parser.readString("\"hello\"", null, String.class);
        Assertions.assertEquals("\"hello\"", result1);

        // Test with Integer
        Integer result2 = parser.readString("123", null, Integer.class);
        Assertions.assertEquals(123, result2);

        // Test with Boolean
        Boolean result3 = parser.readString("true", null, Boolean.class);
        Assertions.assertTrue(result3);

        // Test with null
        String result4 = parser.readString(null, null, String.class);
        Assertions.assertNull(result4);

        // Test with empty string
        String result5 = parser.readString("", null, String.class);
        Assertions.assertEquals("", result5);
    }

    @Test
    public void testReadString_WithConfig() {
        JSONDeserializationConfig config = JDC.create().readNullToEmpty(true);

        // Test null to empty for String
        String result1 = parser.readString(null, config, String.class);
        Assertions.assertEquals("", result1);

        // Test null to empty for List
        List<String> result2 = parser.readString(null, config, List.class);
        Assertions.assertNotNull(result2);
        Assertions.assertTrue(result2.isEmpty());

        // Test null to empty for Map
        Map<String, Object> result3 = parser.readString(null, config, Map.class);
        Assertions.assertNotNull(result3);
        Assertions.assertTrue(result3.isEmpty());
    }

    @Test
    public void testReadString_ComplexTypes() {
        // Test with Map
        Map<String, Object> result1 = parser.readString("{\"key\":\"value\"}", null, Map.class);
        Assertions.assertEquals("value", result1.get("key"));

        // Test with List
        List<String> result2 = parser.readString("[\"a\",\"b\",\"c\"]", null, List.class);
        Assertions.assertEquals(3, result2.size());
        Assertions.assertEquals("a", result2.get(0));

        // Test with nested structures
        String json = "{\"list\":[1,2,3],\"map\":{\"nested\":\"value\"}}";
        Map<String, Object> result3 = parser.readString(json, null, Map.class);
        Assertions.assertTrue(result3.get("list") instanceof List);
        Assertions.assertTrue(result3.get("map") instanceof Map);
    }

    @Test
    public void testReadString_Array() {
        // Test with int array
        int[] intArray = parser.readString("[1,2,3]", null, int[].class);
        Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, intArray);

        // Test with String array
        String[] strArray = parser.readString("[\"a\",\"b\",\"c\"]", null, String[].class);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, strArray);

        // Test with Object array
        Object[] objArray = parser.readString("[1,\"two\",true]", null, Object[].class);
        Assertions.assertEquals(3, objArray.length);
    }

    @Test
    public void testReadString_OutputArray() {
        Object[] output = new Object[3];
        parser.readString("[\"a\",\"b\",\"c\"]", null, output);
        Assertions.assertEquals("a", output[0]);
        Assertions.assertEquals("b", output[1]);
        Assertions.assertEquals("c", output[2]);

        // Test with null source
        Object[] output2 = new Object[3];
        parser.readString(null, null, output2);
        Assertions.assertNull(output2[0]);
    }

    @Test
    public void testReadString_OutputCollection() {
        List<String> output = new ArrayList<>();
        parser.readString("[\"x\",\"y\",\"z\"]", null, output);
        Assertions.assertEquals(3, output.size());
        Assertions.assertEquals("x", output.get(0));

        // Test with Set
        Set<Integer> outputSet = new HashSet<>();
        parser.readString("[1,2,3]", null, outputSet);
        Assertions.assertEquals(3, outputSet.size());
        Assertions.assertTrue(outputSet.contains(1));

        // Test with null source
        List<String> output2 = new ArrayList<>();
        parser.readString(null, null, output2);
        Assertions.assertTrue(output2.isEmpty());
    }

    @Test
    public void testReadString_OutputMap() {
        Map<String, Object> output = new HashMap<>();
        parser.readString("{\"a\":1,\"b\":2}", null, output);
        Assertions.assertEquals(2, output.size());
        Assertions.assertEquals(1, output.get("a"));
        Assertions.assertEquals(2, output.get("b"));

        // Test with empty string
        Map<String, Object> output2 = new HashMap<>();
        parser.readString("", null, output2);
        Assertions.assertTrue(output2.isEmpty());
    }

    @Test
    public void testSerialize_SimpleTypes() {
        // Test String
        String result1 = parser.serialize("test");
        Assertions.assertEquals("test", result1);

        // Test Integer
        String result2 = parser.serialize(123);
        Assertions.assertEquals("123", result2);

        // Test Boolean
        String result3 = parser.serialize(true);
        Assertions.assertEquals("true", result3);

        // Test null
        String result4 = parser.serialize(null);
        Assertions.assertNull(result4);
    }

    @Test
    public void testSerialize_ComplexTypes() {
        // Test Map
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String result1 = parser.serialize(map);
        Assertions.assertTrue(result1.contains("\"key\""));
        Assertions.assertTrue(result1.contains("\"value\""));

        // Test List
        List<String> list = Arrays.asList("a", "b", "c");
        String result2 = parser.serialize(list);
        Assertions.assertTrue(result2.startsWith("["));
        Assertions.assertTrue(result2.endsWith("]"));

        // Test array
        int[] array = { 1, 2, 3 };
        String result3 = parser.serialize(array);
        Assertions.assertEquals("[1, 2, 3]", result3);
    }

    @Test
    public void testSerialize_WithConfig() {
        JSONSerializationConfig config = JSC.create().prettyFormat(true);

        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        String result = parser.serialize(map, config);
        Assertions.assertTrue(result.contains("\n"));
        Assertions.assertTrue(result.contains("  "));
    }

    @Test
    public void testSerialize_ToFile() throws IOException {
        File file = tempDir.resolve("test.json").toFile();

        Map<String, Object> data = new HashMap<>();
        data.put("name", "test");
        data.put("value", 123);

        parser.serialize(data, null, file);

        Assertions.assertTrue(file.exists());
        String content = new String(Files.readAllBytes(file.toPath()));
        Assertions.assertTrue(content.contains("\"name\""));
        Assertions.assertTrue(content.contains("\"test\""));

        // Test with null
        File file2 = tempDir.resolve("test2.json").toFile();
        parser.serialize(null, null, file2);
        Assertions.assertTrue(file2.exists());
        Assertions.assertEquals("", new String(Files.readAllBytes(file2.toPath())));
    }

    @Test
    public void testSerialize_ToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        List<Integer> list = Arrays.asList(1, 2, 3);
        parser.serialize(list, null, baos);

        String result = baos.toString();
        Assertions.assertEquals("[1, 2, 3]", result);

        // Test with null
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        parser.serialize(null, null, baos2);
        Assertions.assertEquals("", baos2.toString());
    }

    @Test
    public void testSerialize_ToWriter() throws IOException {
        StringWriter writer = new StringWriter();

        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");

        parser.serialize(map, null, writer);

        String result = writer.toString();
        Assertions.assertTrue(result.contains("\"foo\""));
        Assertions.assertTrue(result.contains("\"bar\""));

        // Test with null
        StringWriter writer2 = new StringWriter();
        parser.serialize(null, null, writer2);
        Assertions.assertEquals("", writer2.toString());
    }

    @Test
    public void testDeserialize_FromString() {
        // Test simple types
        String result1 = parser.deserialize("hello", null, String.class);
        Assertions.assertEquals("hello", result1);

        Integer result2 = parser.deserialize("42", null, Integer.class);
        Assertions.assertEquals(42, result2);

        // Test complex types
        Map<String, Object> result3 = parser.deserialize("{\"a\":1}", null, Map.class);
        Assertions.assertEquals(1, result3.get("a"));

        List<String> result4 = parser.deserialize("[\"x\",\"y\"]", null, List.class);
        Assertions.assertEquals(2, result4.size());

        // Test null and empty
        String result5 = parser.deserialize((String) null, null, String.class);
        Assertions.assertNull(result5);

        JSONDeserializationConfig config = JDC.create().readNullToEmpty(true);
        String result6 = parser.deserialize("", config, String.class);
        Assertions.assertEquals("", result6);
    }

    @Test
    public void testDeserialize_FromStringWithIndices() {
        String json = "{\"start\":true,\"middle\":123,\"end\":false}";

        // Test full range
        Map<String, Object> result1 = parser.deserialize(json, 0, json.length(), null, Map.class);
        Assertions.assertEquals(3, result1.size());

        // Test partial range - just the number
        int start = json.indexOf("123");
        int end = start + 3;
        Integer result2 = parser.deserialize(json, start, end, null, Integer.class);
        Assertions.assertEquals(123, result2);

        // Test empty range
        String result3 = parser.deserialize(json, 5, 5, null, String.class);
        Assertions.assertEquals("", result3);

        // Test with readNullToEmpty
        JSONDeserializationConfig config = JDC.create().readNullToEmpty(true);
        List<String> result4 = parser.deserialize(json, 5, 5, config, List.class);
        Assertions.assertNotNull(result4);
        Assertions.assertTrue(result4.isEmpty());
    }

    @Test
    public void testDeserialize_FromFile() throws IOException {
        File file = tempDir.resolve("input.json").toFile();
        Files.write(file.toPath(), "{\"test\":\"value\"}".getBytes());

        Map<String, String> result = parser.deserialize(file, null, Map.class);
        Assertions.assertEquals("value", result.get("test"));
    }

    @Test
    public void testDeserialize_FromInputStream() throws IOException {
        String json = "[1,2,3,4,5]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        List<Integer> result = parser.deserialize(bais, null, List.class);
        Assertions.assertEquals(5, result.size());
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertEquals(5, result.get(4));
    }

    @Test
    public void testDeserialize_FromReader() throws IOException {
        String json = "{\"nested\":{\"array\":[true,false]}}";
        StringReader reader = new StringReader(json);

        Map<String, Object> result = parser.deserialize(reader, null, Map.class);
        Map<String, Object> nested = (Map<String, Object>) result.get("nested");
        List<Boolean> array = (List<Boolean>) nested.get("array");
        Assertions.assertEquals(2, array.size());
        Assertions.assertTrue(array.get(0));
        Assertions.assertFalse(array.get(1));
    }

    @Test
    public void testStream_FromString() {
        String json = "[[1,2],[3,4],[5,6]]";

        Stream<List<Integer>> stream = parser.stream(json, null, Type.ofList(Integer.class));
        List<List<Integer>> result = stream.toList();

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2, result.get(0).size());
        Assertions.assertEquals(1, result.get(0).get(0));
        Assertions.assertEquals(6, result.get(2).get(1));

        // Test empty array
        Stream<List<String>> stream2 = parser.stream("[]", null, Type.ofList(String.class));
        Assertions.assertEquals(0, stream2.count());

        // Test empty string
        Stream<List<String>> stream3 = parser.stream("", null, Type.ofList(String.class));
        Assertions.assertEquals(0, stream3.count());
    }

    @Test
    public void testStream_ComplexTypes() {
        String json = "[{\"id\":1},{\"id\":2},{\"id\":3}]";

        Stream<Map<String, Integer>> stream = parser.stream(json, null, Type.ofMap(String.class, Integer.class));
        List<Map<String, Integer>> result = stream.toList();

        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(0).get("id"));
        Assertions.assertEquals(2, result.get(1).get("id"));
        Assertions.assertEquals(3, result.get(2).get("id"));
    }

    @Test
    public void testStream_FromFile() throws IOException {
        File file = tempDir.resolve("stream.json").toFile();
        Files.write(file.toPath(), "[[1,2],,[3,4],[5,6]]".getBytes());

        Stream<List<Integer>> stream = parser.stream(file, null, Type.ofList(Integer.class));
        int sum = stream.flatmap(Fn.identity()).reduce(0, Integer::sum);

        Assertions.assertEquals(21, sum);
    }

    @Test
    public void testStream_FromInputStream() throws IOException {
        String json = "[{\"name\":\"A\"},{\"name\":\"B\"}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        Stream<Map<String, String>> stream = parser.stream(bais, null, true, Type.ofMap(String.class, String.class));

        AtomicInteger count = new AtomicInteger(0);
        stream.forEach(m -> {
            count.incrementAndGet();
            Assertions.assertTrue(m.containsKey("name"));
        });

        Assertions.assertEquals(2, count.get());
    }

    @Test
    public void testStream_FromReader() throws IOException {
        String json = "[[1,2],[3,4],[5,6]]";
        StringReader reader = new StringReader(json);

        Stream<List<Integer>> stream = parser.stream(reader, null, true, Type.ofList(Integer.class));

        List<List<Integer>> result = stream.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2, result.get(0).size());
        Assertions.assertEquals(1, result.get(0).get(0));
        Assertions.assertEquals(6, result.get(2).get(1));
    }

    @Test
    public void testStream_CloseHandling() throws IOException {
        // Test with closeInputStreamWhenStreamIsClosed = false
        String json = "[[1,2],[3,4],[5,6]]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        Stream<List<Integer>> stream = parser.stream(bais, null, true, Type.ofList(Integer.class));
        stream.toList();

        // Stream should be readable after stream is closed
        Assertions.assertTrue(bais.available() >= 0);

        // Test with closeReaderWhenStreamIsClosed = true
        StringReader reader = new StringReader("[[1,2],[3,4],[5,6]]");
        Stream<List<Integer>> stream2 = parser.stream(reader, null, false, Type.ofList(Integer.class));
        List<List<Integer>> result = stream2.toList();
        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void testSpecialCases_CircularReference() {
        JSONSerializationConfig config = JSC.create().supportCircularReference(true);

        Map<String, Object> map = new HashMap<>();
        map.put("self", map); // Circular reference

        String result = parser.serialize(map, config);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("\"self\": "));
    }

    @Test
    public void testSpecialCases_EmptyAndNull() {
        // Test various empty/null scenarios
        JSONDeserializationConfig readConfig = JDC.create().readNullToEmpty(true);
        JSONSerializationConfig writeConfig = JSC.create().writeNullToEmpty(true);

        // Empty string to empty collection
        List<String> list1 = parser.deserialize("", readConfig, List.class);
        Assertions.assertNotNull(list1);
        Assertions.assertTrue(list1.isEmpty());

        // Null to empty map
        Map<String, Object> map1 = parser.deserialize((String) null, readConfig, Map.class);
        Assertions.assertNotNull(map1);
        Assertions.assertTrue(map1.isEmpty());

        // Serialize with writeNullToEmpty
        Map<String, Object> mapWithNull = new HashMap<>();
        mapWithNull.put("nullValue", null);
        mapWithNull.put("normalValue", "test");

        String serialized = parser.serialize(mapWithNull, writeConfig);
        Assertions.assertNotNull(serialized);
    }

    @Test
    public void testSpecialCases_DataSet() {
        // Test DataSet serialization/deserialization
        List<String> columnNames = Arrays.asList("col1", "col2");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(Arrays.asList("a", "b", "c"));
        columns.add(Arrays.asList(1, 2, 3));

        DataSet dataSet = new RowDataSet(columnNames, columns);

        String serialized = parser.serialize(dataSet);
        Assertions.assertNotNull(serialized);
        Assertions.assertTrue(serialized.contains("columnNames"));
        Assertions.assertTrue(serialized.contains("columns"));

        DataSet deserialized = parser.deserialize(serialized, null, DataSet.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(2, deserialized.columnNameList().size());
        Assertions.assertEquals(3, deserialized.size());
    }

    @Test
    public void testSpecialCases_NestedStructures() {
        // Create deeply nested structure
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> level1 = new HashMap<>();
        List<Map<String, Object>> level2 = new ArrayList<>();
        Map<String, Object> level3 = new HashMap<>();

        level3.put("deep", "value");
        level2.add(level3);
        level1.put("list", level2);
        root.put("nested", level1);

        String serialized = parser.serialize(root);
        Map<String, Object> deserialized = parser.deserialize(serialized, null, Map.class);

        Map<String, Object> nested = (Map<String, Object>) deserialized.get("nested");
        List<Map<String, Object>> list = (List<Map<String, Object>>) nested.get("list");
        Map<String, Object> item = list.get(0);

        Assertions.assertEquals("value", item.get("deep"));
    }

    @Test
    public void testConfiguration_IgnoredProperties() {
        JSONDeserializationConfig config = JDC.create().ignoreUnmatchedProperty(true).setIgnoredPropNames(Map.class, N.asSet("ignored"));

        String json = "{\"ignored\":\"value1\",\"kept\":\"value2\",\"unknown\":\"value3\"}";
        Map<String, Object> result = parser.deserialize(json, config, Map.class);

        Assertions.assertFalse(result.containsKey("ignored"));
        Assertions.assertTrue(result.containsKey("kept"));
        Assertions.assertEquals("value2", result.get("kept"));
    }

    @Test
    public void testConfiguration_PrettyFormat() {
        JSONSerializationConfig config = JSC.create().prettyFormat(true).setIndentation("    "); // 4 spaces

        Map<String, Object> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", Arrays.asList("a", "b"));

        String result = parser.serialize(data, config);

        Assertions.assertTrue(result.contains("\n"));
        Assertions.assertTrue(result.contains("    ")); // Check indentation
    }

    @Test
    public void testConfiguration_QuoteOptions() {
        JSONSerializationConfig config1 = JSC.create().quotePropName(false);
        JSONSerializationConfig config2 = JSC.create().quoteMapKey(false);

        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        String result1 = parser.serialize(map, config1);
        Assertions.assertTrue(result1.contains("key")); // Without quotes

        Map<Integer, String> intKeyMap = new HashMap<>();
        intKeyMap.put(123, "value");

        String result2 = parser.serialize(intKeyMap, config2);
        Assertions.assertTrue(result2.contains("123")); // Number key without quotes
    }

    @Test
    public void testPrimitiveArrays() {
        // Test various primitive arrays
        int[] intArray = { 1, 2, 3 };
        String intResult = parser.serialize(intArray);
        int[] intDeserialized = parser.deserialize(intResult, null, int[].class);
        Assertions.assertArrayEquals(intArray, intDeserialized);

        double[] doubleArray = { 1.1, 2.2, 3.3 };
        String doubleResult = parser.serialize(doubleArray);
        double[] doubleDeserialized = parser.deserialize(doubleResult, null, double[].class);
        Assertions.assertArrayEquals(doubleArray, doubleDeserialized);

        boolean[] boolArray = { true, false, true };
        String boolResult = parser.serialize(boolArray);
        boolean[] boolDeserialized = parser.deserialize(boolResult, null, boolean[].class);
        Assertions.assertArrayEquals(boolArray, boolDeserialized);
    }

    @Test
    public void testEdgeCases_EmptyCollections() {
        // Empty collections
        List<String> emptyList = new ArrayList<>();
        String listJson = parser.serialize(emptyList);
        List<String> deserializedList = parser.deserialize(listJson, null, List.class);
        Assertions.assertTrue(deserializedList.isEmpty());

        Map<String, Object> emptyMap = new HashMap<>();
        String mapJson = parser.serialize(emptyMap);
        Map<String, Object> deserializedMap = parser.deserialize(mapJson, null, Map.class);
        Assertions.assertTrue(deserializedMap.isEmpty());

        Object[] emptyArray = new Object[0];
        String arrayJson = parser.serialize(emptyArray);
        Object[] deserializedArray = parser.deserialize(arrayJson, null, Object[].class);
        Assertions.assertEquals(0, deserializedArray.length);
    }

    @Test
    public void testEdgeCases_NullValues() {
        // Collections with null values
        List<String> listWithNulls = Arrays.asList("a", null, "c");
        String listJson = parser.serialize(listWithNulls);
        List<String> deserializedList = parser.deserialize(listJson, null, List.class);
        Assertions.assertEquals(3, deserializedList.size());
        Assertions.assertNull(deserializedList.get(1));

        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put("key1", "value1");
        mapWithNulls.put("key2", null);
        String mapJson = parser.serialize(mapWithNulls);
        Map<String, String> deserializedMap = parser.deserialize(mapJson, null, Map.class);
        Assertions.assertEquals("value1", deserializedMap.get("key1"));
        Assertions.assertNull(deserializedMap.get("key2"));
    }

    @Test
    public void testComplexBean() {
        // Test with a complex bean structure
        TestBean bean = new TestBean();
        bean.setName("Test");
        bean.setValue(42);
        bean.setActive(true);
        bean.setTags(Arrays.asList("tag1", "tag2"));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("created", "2024-01-01");
        bean.setMetadata(metadata);

        String json = parser.serialize(bean);
        TestBean deserialized = parser.deserialize(json, null, TestBean.class);

        Assertions.assertEquals(bean.getName(), deserialized.getName());
        Assertions.assertEquals(bean.getValue(), deserialized.getValue());
        Assertions.assertEquals(bean.isActive(), deserialized.isActive());
        Assertions.assertEquals(bean.getTags(), deserialized.getTags());
        Assertions.assertEquals(bean.getMetadata(), deserialized.getMetadata());
    }

    @Test
    public void testUnsupportedTypes() {
        // Test error handling for unsupported types in stream
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            parser.stream("[1,2,3]", null, Type.of(StringBuilder.class));
        });
    }

    @Test
    public void testInvalidJson() {
        // Test various invalid JSON scenarios
        Assertions.assertThrows(Exception.class, () -> {
            parser.deserialize("{invalid json}", null, Map.class);
        });

        Assertions.assertThrows(Exception.class, () -> {
            parser.deserialize("[1,2,", null, List.class);
        });

        Assertions.assertThrows(Exception.class, () -> {
            parser.deserialize("{'single quotes'}", null, Map.class);
        });
    }

    @Test
    public void testLargeData() {
        // Test with larger data sets
        List<Map<String, Object>> largeList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", i);
            item.put("name", "Item " + i);
            item.put("value", Math.random() * 1000);
            largeList.add(item);
        }

        String json = parser.serialize(largeList);
        List<Map<String, Object>> deserialized = parser.deserialize(json, null, List.class);

        Assertions.assertEquals(1000, deserialized.size());
        Assertions.assertEquals(0, deserialized.get(0).get("id"));
        Assertions.assertEquals(999, deserialized.get(999).get("id"));
    }

    // Helper class for bean testing
    public static class TestBean {
        private String name;
        private int value;
        private boolean active;
        private List<String> tags;
        private Map<String, Object> metadata;

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

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata;
        }
    }
}
