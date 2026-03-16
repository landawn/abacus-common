package com.landawn.abacus.parser;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.base.Strings;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.FastJson;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.TypeReference;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Tag("2025")
public class JsonParserImplTest extends TestBase {

    private JsonParserImpl parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        parser = new JsonParserImpl();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestBean {
        private String name;
        private int value;
        private boolean active;
        private List<String> tags;
        private Map<String, Object> metadata;
    }

    @Data
    public static class ParameterizedBean<A, B, C, K, V> {
        private A a;
        private B[] b;
        private List<C> cList;
        private List<Map<K, V>> mapList;
        private String name;
        private B[][] bb;
        private Map<List<K[]>, List<B[][]>> map2;
        private Map<List<String[]>, B[][]> map3;
        private Map<List<B[][]>, Byte> map4;
    }

    @Test
    public void test_ParameterizedBean() throws Exception {

        ParameterizedBean<Integer, String, Double, Byte, Short> bean = new ParameterizedBean<>();
        bean.setA(100);
        bean.setB(new String[] { "one", "two", "three" });
        bean.setCList(N.toList(11.11, 22.22, 33.33));
        bean.setMapList(N.toList(N.asMap((byte) 1, (short) 10), N.asMap((byte) 2, (short) 20)));
        bean.setName("ParameterizedBean");
        bean.setBb(new String[][] { { "aa", "bb" }, { "cc", "dd" } });
        bean.setMap2(N.asMap(N.asSingletonList(new Byte[] { 1, 2 }), N.asSingletonList(new String[][] { { "v11", "v12" }, { "v21", "v22" } })));
        bean.setMap3(N.asMap(N.asSingletonList(new String[] { "s1", "s2" }), new String[][] { { "m31", "m32" }, { "m41", "m42" } }));
        bean.setMap4(N.asMap(N.asSingletonList(new String[][] { { "x1", "x2" }, { "y1", "y2" } }), (byte) 100));

        String json = N.toJson(bean);

        N.println(Strings.repeat("=", 80));

        N.println(json);

        //        final ObjectMapper mapper = new ObjectMapper();
        //
        //        ParameterizedBean<Integer, String, Double, Byte, Short> ret = mapper.readValue(json,
        //                mapper.getTypeFactory().constructParametricType(ParameterizedBean.class, Integer.class, String.class, Double.class, Byte.class, Short.class));
        //
        //        N.println(Strings.repeat("=", 80));
        //        System.out.println(ret);
        //        System.out.println(ret.getClass());
        //        System.out.println(ret.getA().getClass());
        //        System.out.println(ret.getB().getClass());
        //        System.out.println(ret.getCList().get(0).getClass());
        //        System.out.println(ret.getMapList().get(0).entrySet().iterator().next().getKey().getClass());
        //        System.out.println(ret.getMapList().get(0).entrySet().iterator().next().getValue().getClass());

        ParameterizedBean<Integer, String, Double, Byte, Short> ret = FastJson.fromJson(json,
                new TypeReference<ParameterizedBean<Integer, String, Double, Byte, Short>>() {
                }.javaType());

        N.println(Strings.repeat("=", 80));
        assertEquals(json, N.toJson(ret));
        assertEquals(ParameterizedBean.class, ret.getClass());
        assertEquals(Integer.class, ret.getA().getClass());
        assertEquals(String[].class, ret.getB().getClass());
        assertEquals(Double.class, ret.getCList().get(0).getClass());
        assertEquals(Byte.class, ret.getMapList().get(0).entrySet().iterator().next().getKey().getClass());
        assertEquals(Short.class, ret.getMapList().get(0).entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getBb().getClass());

        assertEquals(Byte[].class, ret.getMap2().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap2().entrySet().iterator().next().getValue().get(0).getClass());

        assertEquals(String[].class, ret.getMap3().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap3().entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getMap4().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(Byte.class, ret.getMap4().entrySet().iterator().next().getValue().getClass());

        Type<ParameterizedBean<Integer, String, Double, Byte, Short>> type = Type
                .of(new TypeReference<ParameterizedBean<Integer, String, Double, Byte, Short>>() {
                }.javaType());

        ret = N.fromJson(json, type);

        N.println(Strings.repeat("=", 80));
        assertEquals(json, N.toJson(ret));
        assertEquals(ParameterizedBean.class, ret.getClass());
        assertEquals(Integer.class, ret.getA().getClass());
        assertEquals(String[].class, ret.getB().getClass());
        assertEquals(Double.class, ret.getCList().get(0).getClass());
        assertEquals(Byte.class, ret.getMapList().get(0).entrySet().iterator().next().getKey().getClass());
        assertEquals(Short.class, ret.getMapList().get(0).entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getBb().getClass());

        assertEquals(Byte[].class, ret.getMap2().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap2().entrySet().iterator().next().getValue().get(0).getClass());

        assertEquals(String[].class, ret.getMap3().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(String[][].class, ret.getMap3().entrySet().iterator().next().getValue().getClass());

        assertEquals(String[][].class, ret.getMap4().entrySet().iterator().next().getKey().get(0).getClass());
        assertEquals(Byte.class, ret.getMap4().entrySet().iterator().next().getValue().getClass());

        N.println(type.name());
        N.println(type.javaType());
    }

    @Test
    public void test_constructor_default() {
        JsonParser parser = new JsonParserImpl();
        assertNotNull(parser);
    }

    @Test
    public void test_constructor_withConfig() {
        JsonSerConfig jsc = new JsonSerConfig();
        JsonDeserConfig jdc = new JsonDeserConfig();
        JsonParser parser = new JsonParserImpl(jsc, jdc);
        assertNotNull(parser);
    }

    @Test
    public void test_serialize_string() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize("test");
        assertNotNull(json);
    }

    @Test
    public void test_deserialize_string() {
        JsonParser parser = new JsonParserImpl();
        String result = parser.deserialize("\"test\"", String.class);
        assertEquals("\"test\"", result);
    }

    @Test
    public void test_serialize_integer() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize(123);
        assertEquals("123", json);
    }

    @Test
    public void test_deserialize_integer() {
        JsonParser parser = new JsonParserImpl();
        Integer result = parser.deserialize("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    public void test_serialize_boolean() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize(true);
        assertEquals("true", json);
    }

    @Test
    public void test_deserialize_boolean() {
        JsonParser parser = new JsonParserImpl();
        Boolean result = parser.deserialize("true", Boolean.class);
        assertEquals(true, result);
    }

    @Test
    public void test_serialize_null() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize(null);
        assertEquals("", json);
    }

    @Test
    public void testConstructors() {
        JsonParserImpl parser1 = new JsonParserImpl();
        Assertions.assertNotNull(parser1);

        JsonSerConfig jsc = JsonSerConfig.create();
        JsonDeserConfig jdc = JsonDeserConfig.create();
        JsonParserImpl parser2 = new JsonParserImpl(jsc, jdc);
        Assertions.assertNotNull(parser2);
    }

    @Test
    public void testparse_SimpleTypes() {
        String result1 = parser.parse("\"hello\"", null, String.class);
        Assertions.assertEquals("\"hello\"", result1);

        Integer result2 = parser.parse("123", null, Integer.class);
        Assertions.assertEquals(123, result2);

        Boolean result3 = parser.parse("true", null, Boolean.class);
        Assertions.assertTrue(result3);

        String result4 = parser.parse(null, null, String.class);
        Assertions.assertNull(result4);

        String result5 = parser.parse("", null, String.class);
        Assertions.assertEquals("", result5);
    }

    @Test
    public void testparse_WithConfig() {
        JsonDeserConfig config = JsonDeserConfig.create().setReadNullToEmpty(true);

        String result1 = parser.parse(null, config, String.class);
        Assertions.assertEquals("", result1);

        List<String> result2 = parser.parse(null, config, List.class);
        Assertions.assertNotNull(result2);
        Assertions.assertTrue(result2.isEmpty());

        Map<String, Object> result3 = parser.parse(null, config, Map.class);
        Assertions.assertNotNull(result3);
        Assertions.assertTrue(result3.isEmpty());
    }

    @Test
    public void testparse_ComplexTypes() {
        Map<String, Object> result1 = parser.parse("{\"key\":\"value\"}", null, Map.class);
        Assertions.assertEquals("value", result1.get("key"));

        List<String> result2 = parser.parse("[\"a\",\"b\",\"c\"]", null, List.class);
        Assertions.assertEquals(3, result2.size());
        Assertions.assertEquals("a", result2.get(0));

        String json = "{\"list\":[1,2,3],\"map\":{\"nested\":\"value\"}}";
        Map<String, Object> result3 = parser.parse(json, null, Map.class);
        Assertions.assertTrue(result3.get("list") instanceof List);
        Assertions.assertTrue(result3.get("map") instanceof Map);
    }

    @Test
    public void testparse_Array() {
        int[] intArray = parser.parse("[1,2,3]", null, int[].class);
        Assertions.assertArrayEquals(new int[] { 1, 2, 3 }, intArray);

        String[] strArray = parser.parse("[\"a\",\"b\",\"c\"]", null, String[].class);
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, strArray);

        Object[] objArray = parser.parse("[1,\"two\",true]", null, Object[].class);
        Assertions.assertEquals(3, objArray.length);
    }

    @Test
    public void testparse_OutputArray() {
        Object[] output = new Object[3];
        parser.parse("[\"a\",\"b\",\"c\"]", null, output);
        Assertions.assertEquals("a", output[0]);
        Assertions.assertEquals("b", output[1]);
        Assertions.assertEquals("c", output[2]);

        Object[] output2 = new Object[3];
        parser.parse(null, null, output2);
        Assertions.assertNull(output2[0]);
    }

    @Test
    public void testparse_OutputCollection() {
        List<String> output = new ArrayList<>();
        parser.parse("[\"x\",\"y\",\"z\"]", null, output);
        Assertions.assertEquals(3, output.size());
        Assertions.assertEquals("x", output.get(0));

        Set<Integer> outputSet = new HashSet<>();
        parser.parse("[1,2,3]", null, outputSet);
        Assertions.assertEquals(3, outputSet.size());
        Assertions.assertTrue(outputSet.contains(1));

        List<String> output2 = new ArrayList<>();
        parser.parse(null, null, output2);
        Assertions.assertTrue(output2.isEmpty());
    }

    @Test
    public void testparse_OutputMap() {
        Map<String, Object> output = new HashMap<>();
        parser.parse("{\"a\":1,\"b\":2}", null, output);
        Assertions.assertEquals(2, output.size());
        Assertions.assertEquals(1, output.get("a"));
        Assertions.assertEquals(2, output.get("b"));

        Map<String, Object> output2 = new HashMap<>();
        parser.parse("", null, output2);
        Assertions.assertTrue(output2.isEmpty());
    }

    @Test
    public void testSerialize_SimpleTypes() {
        String result1 = parser.serialize("test");
        Assertions.assertEquals("test", result1);

        String result2 = parser.serialize(123);
        Assertions.assertEquals("123", result2);

        String result3 = parser.serialize(true);
        Assertions.assertEquals("true", result3);

        String result4 = parser.serialize(null);
        Assertions.assertEquals("", result4);
    }

    @Test
    public void testSerialize_ComplexTypes() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String result1 = parser.serialize(map);
        Assertions.assertTrue(result1.contains("\"key\""));
        Assertions.assertTrue(result1.contains("\"value\""));

        List<String> list = Arrays.asList("a", "b", "c");
        String result2 = parser.serialize(list);
        Assertions.assertTrue(result2.startsWith("["));
        Assertions.assertTrue(result2.endsWith("]"));

        int[] array = { 1, 2, 3 };
        String result3 = parser.serialize(array);
        Assertions.assertEquals("[1, 2, 3]", result3);
    }

    @Test
    public void testSerialize_WithConfig() {
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true);

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

        StringWriter writer2 = new StringWriter();
        parser.serialize(null, null, writer2);
        Assertions.assertEquals("", writer2.toString());
    }

    @Test
    public void testDeserialize_FromString() {
        String result1 = parser.deserialize("hello", null, String.class);
        Assertions.assertEquals("hello", result1);

        Integer result2 = parser.deserialize("42", null, Integer.class);
        Assertions.assertEquals(42, result2);

        Map<String, Object> result3 = parser.deserialize("{\"a\":1}", null, Map.class);
        Assertions.assertEquals(1, result3.get("a"));

        List<String> result4 = parser.deserialize("[\"x\",\"y\"]", null, List.class);
        Assertions.assertEquals(2, result4.size());

        String result5 = parser.deserialize((String) null, null, String.class);
        Assertions.assertNull(result5);

        JsonDeserConfig config = JsonDeserConfig.create().setReadNullToEmpty(true);
        String result6 = parser.deserialize("", config, String.class);
        Assertions.assertEquals("", result6);
    }

    @Test
    public void testDeserialize_FromStringWithIndices() {
        String json = "{\"start\":true,\"middle\":123,\"end\":false}";

        Map<String, Object> result1 = parser.deserialize(json, 0, json.length(), null, Map.class);
        Assertions.assertEquals(3, result1.size());

        int start = json.indexOf("123");
        int end = start + 3;
        Integer result2 = parser.deserialize(json, start, end, null, Integer.class);
        Assertions.assertEquals(123, result2);

        String result3 = parser.deserialize(json, 5, 5, null, String.class);
        Assertions.assertEquals("", result3);

        JsonDeserConfig config = JsonDeserConfig.create().setReadNullToEmpty(true);
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

        Stream<List<String>> stream2 = parser.stream("[]", null, Type.ofList(String.class));
        Assertions.assertEquals(0, stream2.count());

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

        Stream<Map<String, String>> stream = parser.stream(bais, true, null, Type.ofMap(String.class, String.class));

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

        Stream<List<Integer>> stream = parser.stream(reader, true, null, Type.ofList(Integer.class));

        List<List<Integer>> result = stream.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(2, result.get(0).size());
        Assertions.assertEquals(1, result.get(0).get(0));
        Assertions.assertEquals(6, result.get(2).get(1));
    }

    @Test
    public void testStream_CloseHandling() throws IOException {
        String json = "[[1,2],[3,4],[5,6]]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        Stream<List<Integer>> stream = parser.stream(bais, true, null, Type.ofList(Integer.class));
        stream.toList();

        Assertions.assertTrue(bais.available() >= 0);

        StringReader reader = new StringReader("[[1,2],[3,4],[5,6]]");
        Stream<List<Integer>> stream2 = parser.stream(reader, false, null, Type.ofList(Integer.class));
        List<List<Integer>> result = stream2.toList();
        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void testSpecialCases_CircularReference() {
        JsonSerConfig config = JsonSerConfig.create().setSupportCircularReference(true);

        Map<String, Object> map = new HashMap<>();
        map.put("self", map);

        String result = parser.serialize(map, config);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("\"self\": "));
    }

    @Test
    public void testSpecialCases_EmptyAndNull() {
        JsonDeserConfig readConfig = JsonDeserConfig.create().setReadNullToEmpty(true);
        JsonSerConfig writeConfig = JsonSerConfig.create().setWriteNullToEmpty(true);

        List<String> list1 = parser.deserialize("", readConfig, List.class);
        Assertions.assertNotNull(list1);
        Assertions.assertTrue(list1.isEmpty());

        Map<String, Object> map1 = parser.deserialize((String) null, readConfig, Map.class);
        Assertions.assertNotNull(map1);
        Assertions.assertTrue(map1.isEmpty());

        Map<String, Object> mapWithNull = new HashMap<>();
        mapWithNull.put("nullValue", null);
        mapWithNull.put("normalValue", "test");

        String serialized = parser.serialize(mapWithNull, writeConfig);
        Assertions.assertNotNull(serialized);
    }

    @Test
    public void testSpecialCases_Dataset() {
        List<String> columnNames = Arrays.asList("col1", "col2");
        List<List<Object>> columns = new ArrayList<>();
        columns.add(Arrays.asList("a", "b", "c"));
        columns.add(Arrays.asList(1, 2, 3));

        Dataset dataset = new RowDataset(columnNames, columns);

        String serialized = parser.serialize(dataset);
        Assertions.assertNotNull(serialized);
        Assertions.assertTrue(serialized.contains("columnNames"));
        Assertions.assertTrue(serialized.contains("columns"));

        Dataset deserialized = parser.deserialize(serialized, null, Dataset.class);
        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(2, deserialized.columnNames().size());
        Assertions.assertEquals(3, deserialized.size());
    }

    @Test
    public void testSpecialCases_NestedStructures() {
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
        JsonDeserConfig config = JsonDeserConfig.create().setIgnoreUnmatchedProperty(true).setIgnoredPropNames(Map.class, N.toSet("ignored"));

        String json = "{\"ignored\":\"value1\",\"kept\":\"value2\",\"unknown\":\"value3\"}";
        Map<String, Object> result = parser.deserialize(json, config, Map.class);

        Assertions.assertFalse(result.containsKey("ignored"));
        Assertions.assertTrue(result.containsKey("kept"));
        Assertions.assertEquals("value2", result.get("kept"));
    }

    @Test
    public void testConfiguration_PrettyFormat() {
        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true).setIndentation("    ");

        Map<String, Object> data = new HashMap<>();
        data.put("field1", "value1");
        data.put("field2", Arrays.asList("a", "b"));

        String result = parser.serialize(data, config);

        Assertions.assertTrue(result.contains("\n"));
        Assertions.assertTrue(result.contains("    "));
    }

    @Test
    public void testConfiguration_QuoteOptions() {
        JsonSerConfig config1 = JsonSerConfig.create().setQuotePropName(false);
        JsonSerConfig config2 = JsonSerConfig.create().setQuoteMapKey(false);

        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");

        String result1 = parser.serialize(map, config1);
        Assertions.assertTrue(result1.contains("key"));

        Map<Integer, String> intKeyMap = new HashMap<>();
        intKeyMap.put(123, "value");

        String result2 = parser.serialize(intKeyMap, config2);
        Assertions.assertTrue(result2.contains("123"));
    }

    @Test
    public void testPrimitiveArrays() {
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            parser.stream("[1,2,3]", null, Type.of(StringBuilder.class));
        });
    }

    @Test
    public void testInvalidJson() {
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

    @Test
    public void testparseToObject() {
        String json = "{\"name\": \"John\",\"age\":30}";
        Person person = parser.parse(json, null, Person.class);

        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void testparseToArray() {
        String json = "[1,2,3,4,5]";
        Integer[] array = new Integer[5];
        parser.parse(json, null, array);

        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testparseToCollection() {
        String json = "[\"a\",\"b\",\"c\"]";
        List<String> list = new ArrayList<>();
        parser.parse(json, null, list);

        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testparseToMap() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        Map<String, String> map = new HashMap<>();
        parser.parse(json, null, map);

        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testSerializeObject() {
        Person person = new Person("Jane", 25);
        String json = parser.serialize(person);

        assertTrue(json.contains("\"name\": \"Jane\""));
        assertTrue(json.contains("\"age\": 25"));
    }

    @Test
    public void testSerializeNull() {
        String result = parser.serialize(null);
        assertEquals("", result);
    }

    @Test
    public void testSerializeToFile() throws IOException {
        Person person = new Person("Bob", 35);
        File file = tempDir.resolve("person.json").toFile();

        parser.serialize(person, null, file);

        assertTrue(file.exists());
        String content = IOUtil.readAllToString(file);
        assertTrue(content.contains("\"name\": \"Bob\""));
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        Person person = new Person("Alice", 28);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        parser.serialize(person, null, baos);

        String json = baos.toString();
        assertTrue(json.contains("\"name\": \"Alice\""));
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        Person person = new Person("Charlie", 40);
        StringWriter writer = new StringWriter();

        parser.serialize(person, null, writer);

        String json = writer.toString();
        assertTrue(json.contains("\"name\": \"Charlie\""));
    }

    @Test
    public void testDeserializeFromString() {
        String json = "{\"name\": \"David\",\"age\":45}";
        Person person = parser.deserialize(json, null, Person.class);

        assertEquals("David", person.getName());
        assertEquals(45, person.getAge());
    }

    @Test
    public void testDeserializeFromSubstring() {
        String json = "prefix{\"name\": \"Eve\",\"age\":22}suffix";
        Person person = parser.deserialize(json, 6, json.length() - 6, null, Person.class);

        assertEquals("Eve", person.getName());
        assertEquals(22, person.getAge());
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        String json = "{\"name\": \"Frank\",\"age\":50}";
        File file = tempDir.resolve("test.json").toFile();
        IOUtil.write(json, file);

        Person person = parser.deserialize(file, null, Person.class);

        assertEquals("Frank", person.getName());
        assertEquals(50, person.getAge());
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        String json = "{\"name\": \"Grace\",\"age\":33}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        Person person = parser.deserialize(bais, null, Person.class);

        assertEquals("Grace", person.getName());
        assertEquals(33, person.getAge());
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        String json = "{\"name\": \"Henry\",\"age\":27}";
        StringReader reader = new StringReader(json);

        Person person = parser.deserialize(reader, null, Person.class);

        assertEquals("Henry", person.getName());
        assertEquals(27, person.getAge());
    }

    @Test
    public void testStreamFromString() {
        String json = "[{\"name\": \"A\",\"age\":1},{\"name\": \"B\",\"age\":2},{\"name\": \"C\",\"age\":3}]";

        List<Person> people = parser.stream(json, null, Type.of(Person.class)).toList();

        assertEquals(3, people.size());
        assertEquals("A", people.get(0).getName());
        assertEquals("B", people.get(1).getName());
        assertEquals("C", people.get(2).getName());
    }

    @Test
    public void testStreamFromFile() throws IOException {
        String json = "[{\"name\": \"X\",\"age\":10},{\"name\": \"Y\",\"age\":20}]";
        File file = tempDir.resolve("stream.json").toFile();
        IOUtil.write(json, file);

        List<Person> people = parser.stream(file, null, Type.of(Person.class)).toList();

        assertEquals(2, people.size());
        assertEquals("X", people.get(0).getName());
        assertEquals("Y", people.get(1).getName());
    }

    @Test
    public void testStreamFromInputStream() throws IOException {
        String json = "[{\"name\": \"M\",\"age\":15},{\"name\": \"N\",\"age\":25}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes());

        List<Person> people = parser.stream(bais, true, null, Type.of(Person.class)).toList();

        assertEquals(2, people.size());
        assertEquals("M", people.get(0).getName());
        assertEquals("N", people.get(1).getName());
    }

    @Test
    public void testStreamFromReader() throws IOException {
        String json = "[{\"name\": \"P\",\"age\":30},{\"name\": \"Q\",\"age\":40}]";
        StringReader reader = new StringReader(json);

        List<Person> people = parser.stream(reader, true, null, Type.of(Person.class)).toList();

        assertEquals(2, people.size());
        assertEquals("P", people.get(0).getName());
        assertEquals("Q", people.get(1).getName());
    }

    @Test
    public void testSerializationConfig() {
        Person person = new Person("Test", 100);

        JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true).setIndentation("  ");

        String json = parser.serialize(person, config);
        assertTrue(json.contains("\n"));
        assertTrue(json.contains("  "));
    }

    @Test
    public void testSerializeWithNullHandling() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", null);
        map.put("number", null);
        map.put("boolean", null);

        JsonSerConfig config = JsonSerConfig.create().setWriteNullStringAsEmpty(true).setWriteNullNumberAsZero(true).setWriteNullBooleanAsFalse(true);

        String json = parser.serialize(map, config);
        assertTrue(json.contains("\"string\": null"));
        assertTrue(json.contains("\"number\": null"));
        assertTrue(json.contains("\"boolean\": null"));
    }

    @Test
    public void testDeserializationConfig() {
        String json = "{\"name\": \"Test\",\"unknownField\": \"value\"}";

        JsonDeserConfig config = JsonDeserConfig.create().setIgnoreUnmatchedProperty(true);

        Person person = parser.deserialize(json, config, Person.class);
        assertEquals("Test", person.getName());
    }

    @Test
    public void testSerializeCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        String json = parser.serialize(list);

        assertEquals("[1, 2, 3, 4, 5]", json);
    }

    @Test
    public void testSerializeMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        String json = parser.serialize(map);
        assertEquals("{\"one\": 1, \"two\": 2}", json);
    }

    @Test
    public void testSerializeArray() {
        int[] array = { 1, 2, 3 };
        String json = parser.serialize(array);

        assertEquals("[1, 2, 3]", json);
    }

    @Test
    public void testSerializeDataset() {
        List<String> columnNames = Arrays.asList("col1", "col2", "col3");
        List<List<Object>> columnList = new ArrayList<>();
        columnList.add(Arrays.asList("a", "b", "c"));
        columnList.add(Arrays.asList(1, 2, 3));

        Dataset ds = N.newDataset(columnNames, columnList);
        String json = parser.serialize(ds);

        assertTrue(json.contains("columnNames"));
        assertTrue(json.contains("columns"));

        Dataset ds2 = parser.deserialize(json, Dataset.class);
        assertEquals(ds, ds2);
    }

    @Test
    public void testSerializeDataset_2() {
        List<String> columnNames = Arrays.asList("col1", "col2", "col3");
        List<List<Object>> columnList = new ArrayList<>();
        columnList.add(Arrays.asList("a", "b", "c"));
        columnList.add(Arrays.asList(1, 2, 3));

        Dataset ds = N.newDataset(columnNames, columnList);
        ds.freeze();
        String json = parser.serialize(ds, JsonSerConfig.create().setWriteColumnType(true).setPrettyFormat(true).setQuotePropName(true));

        assertTrue(json.contains("columnNames"));
        assertTrue(json.contains("columns"));

        Dataset ds2 = parser.deserialize(json, Dataset.class);
        assertEquals(ds, ds2);
    }

    @Test
    public void testSerializeSheet() {
        List<String> rowKeys = Arrays.asList("R1", "R2", "R3");
        List<String> columnKeys = Arrays.asList("C1", "C2", "C3");
        Object[][] data = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        Sheet<String, String, Integer> sheet = new Sheet<>(rowKeys, columnKeys, data);

        sheet.println();

        sheet.freeze();
        String json = parser.serialize(sheet,
                JsonSerConfig.create().setWriteColumnType(true).setWriteRowColumnKeyType(true).setPrettyFormat(true).setQuotePropName(true));

        Sheet<String, String, Integer> sheet2 = parser.deserialize(json, Sheet.class);
        assertEquals(sheet, sheet2);
    }

    @Test
    public void testSerializeSheetWithNumericColumnKeys() {
        List<String> rowKeys = Arrays.asList("R1", "R2");
        List<Integer> columnKeys = Arrays.asList(1, 2);
        Object[][] data = { { 10, 20 }, { 30, 40 } };
        Sheet<String, Integer, Integer> sheet = new Sheet<>(rowKeys, columnKeys, data);

        sheet.freeze();
        String json = parser.serialize(sheet, JsonSerConfig.create().setWriteColumnType(true).setWriteRowColumnKeyType(true).setQuotePropName(true));

        assertTrue(json.contains("\"columnKeyType\""));

        Sheet<String, Integer, Integer> sheet2 = parser.deserialize(json, Sheet.class);
        assertEquals(sheet, sheet2);
    }

    @Test
    public void testDeserializeSheet_InvalidColumnKeyType() {
        String json = "{\"rowKeyType\":\"String\",\"columnKeyType\":\"Integer\",\"rowKeySet\":[\"R1\"],\"columnKeySet\":[1],\"columnTypes\":[\"Integer\"],\"columns\":{\"bad\":[1]}}";

        ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Sheet.class));
        assertTrue(exception.getMessage().contains("can't be parsed as type"));
    }

    @Test
    public void testDeserializeSheet_UnknownColumnKey() {
        String json = "{\"rowKeyType\":\"String\",\"columnKeyType\":\"Integer\",\"rowKeySet\":[\"R1\"],\"columnKeySet\":[1],\"columnTypes\":[\"Integer\"],\"columns\":{\"2\":[1]}}";

        ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Sheet.class));
        assertTrue(exception.getMessage().contains("is not found column list"));
    }

    @Test
    public void testSerializeMapEntity() {
        MapEntity entity = new MapEntity("TestEntity");
        entity.set("prop1", "value1");
        entity.set("prop2", 123);

        String json = parser.serialize(entity);
        assertTrue(json.contains("TestEntity"));
        assertTrue(json.contains("prop1"));
        assertTrue(json.contains("value1"));
    }

    @Test
    public void testSerializeEntityId() {
        Seid entityId = Seid.of("TestId");
        entityId.set("id", 123);
        entityId.set("type", "test");

        String json = parser.serialize(entityId);
        assertTrue(json.contains("TestId"));
        assertTrue(json.contains("\"id\": 123"));
    }

    @Test
    public void testEmptyJson() {
        assertEquals(new HashMap<>(), parser.deserialize("{}", null, Map.class));
        assertEquals(new ArrayList<>(), parser.deserialize("[]", null, List.class));
    }

    @Test
    public void testSpecialValues() {
        String json = "{\"nullValue\":null,\"trueValue\":true,\"falseValue\":false}";
        Map<String, Object> map = parser.deserialize(json, null, Map.class);

        assertNull(map.get("nullValue"));
        assertEquals(true, map.get("trueValue"));
        assertEquals(false, map.get("falseValue"));
    }

    @Test
    public void testNumbers() {
        String json = "{\"int\":123,\"long\":123456789012345,\"float\":123.45,\"double\":123.456789}";
        Map<String, Object> map = parser.deserialize(json, null, Map.class);

        assertTrue(map.get("int") instanceof Integer);
        assertTrue(map.get("long") instanceof Long);
        assertTrue(map.get("double") instanceof Double);
    }

    @Test
    public void testEscapedCharacters() {
        String json = "{\"text\":\"Line1\\nLine2\\tTabbed\\\"Quoted\\\"\"}";
        Map<String, String> map = parser.deserialize(json, null, Map.class);

        assertEquals("Line1\nLine2\tTabbed\"Quoted\"", map.get("text"));
    }

    @Test
    public void testDeserializeObjectClass_DispatchesContainers() {
        Object mapResult = parser.deserialize("{\"a\":1}", null, Object.class);
        Object listResult = parser.deserialize("[1,2]", null, Object.class);

        assertTrue(mapResult instanceof Map);
        assertEquals(1, ((Map<?, ?>) mapResult).get("a"));
        assertTrue(listResult instanceof List);
        assertEquals(Arrays.asList(1, 2), listResult);
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("1", null, Appendable.class));
    }

    @Test
    public void testSerializeToWriter_IOException() {
        Writer writer = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        UncheckedIOException exception = Assertions.assertThrows(UncheckedIOException.class, () -> parser.serialize(Map.of("a", 1), null, writer));

        assertEquals("boom", exception.getCause().getMessage());
    }

    @Test
    public void testParse_DatasetRoundTrip() {
        Dataset dataset = new RowDataset(Arrays.asList("name", "age"), Arrays.asList(Arrays.asList("Tom", "Jerry"), Arrays.asList(10, 12)));
        String json = parser.serialize(dataset);

        Dataset parsed = parser.parse(json, null, Dataset.class);

        assertNotNull(parsed);
        assertEquals(dataset, parsed);
    }

    @Test
    public void testParse_SheetAndTypedMapWithArrayValues() {
        Sheet<String, Integer, Integer> sheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList(1, 2), new Object[][] { { 10, 20 }, { 30, 40 } });
        String sheetJson = parser.serialize(sheet, JsonSerConfig.create().setWriteColumnType(true).setWriteRowColumnKeyType(true).setQuotePropName(true));

        Sheet<String, Integer, Integer> parsedSheet = parser.parse(sheetJson, null, Sheet.class);
        assertEquals(sheet, parsedSheet);

        String mapJson = "{\"first\":[1,2],\"second\":[3,4,5]}";
        Type<Map<String, int[]>> mapType = Type.of(new TypeReference<Map<String, int[]>>() {
        }.javaType());
        Map<String, int[]> parsedMap = parser.parse(mapJson, null, mapType);

        assertEquals(2, parsedMap.size());
        assertArrayEquals(new int[] { 1, 2 }, parsedMap.get("first"));
        assertArrayEquals(new int[] { 3, 4, 5 }, parsedMap.get("second"));
    }

}
