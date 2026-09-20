package com.landawn.abacus.parser;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

public class JsonParserImplTest extends TestBase {

    @Test
    @SuppressWarnings("deprecation")
    public void testSingleQuotedStringsEscapeApostrophes() throws IOException {
        final JsonParser parser = ParserFactory.createJsonParser();
        final JsonSerConfig config = JsonSerConfig.create().setStringQuotation('\'');
        final String text = "'can't'\\\n\"''";

        for (final Object value : new Object[] { text, new StringBuilder(text), new StringBuffer(text) }) {
            final String encoded = parser.serialize(List.of(value), config);
            assertArrayEquals(new String[] { text }, parser.deserialize(encoded, String[].class));
        }

        final String encodedMap = parser.serialize(Map.of("message", text), config);
        assertEquals(text, parser.deserialize(encodedMap, Map.class).get("message"));

        for (final String typeName : new String[] { "Reader", "InputStream", "AsciiStream", "ClobAsciiStream" }) {
            final Type<Object> type = Type.of(typeName);
            final Object value = typeName.equals("Reader") ? new StringReader(text)
                    : new ByteArrayInputStream(text.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            final StringWriter output = new StringWriter();
            final com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter(output);

            try {
                type.serializeTo(writer, value, config);
                writer.flush();
                assertArrayEquals(new String[] { text }, parser.deserialize("[" + output + "]", String[].class), typeName);
            } finally {
                com.landawn.abacus.util.Objectory.recycle(writer);
            }
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSingleQuotedStreamEscapingAcrossBufferBoundaries() throws IOException {
        final JsonParser parser = ParserFactory.createJsonParser();
        final String text = "'\\\n\"".repeat(10000) + "'";
        final JsonSerConfig config = JsonSerConfig.create().setStringQuotation('\'');

        for (final String typeName : new String[] { "Reader", "InputStream", "AsciiStream", "ClobAsciiStream" }) {
            final Type<Object> type = Type.of(typeName);
            final Object value = typeName.equals("Reader") ? new StringReader(text)
                    : new ByteArrayInputStream(text.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            final StringWriter output = new StringWriter();
            final com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter(output);
            try {
                type.serializeTo(writer, value, config);
                writer.flush();
                assertArrayEquals(new String[] { text }, parser.deserialize("[" + output + "]", String[].class), typeName);
            } finally {
                com.landawn.abacus.util.Objectory.recycle(writer);
            }
        }
    }

    private JsonParserImpl parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        parser = new JsonParserImpl();
    }

    @Data
    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
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

        public A getA() {
            return a;
        }

        public void setA(A a) {
            this.a = a;
        }

        public B[] getB() {
            return b;
        }

        public void setB(B[] b) {
            this.b = b;
        }

        public List<C> getCList() {
            return cList;
        }

        public void setCList(List<C> cList) {
            this.cList = cList;
        }

        public List<Map<K, V>> getMapList() {
            return mapList;
        }

        public void setMapList(List<Map<K, V>> mapList) {
            this.mapList = mapList;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public B[][] getBb() {
            return bb;
        }

        public void setBb(B[][] bb) {
            this.bb = bb;
        }

        public Map<List<K[]>, List<B[][]>> getMap2() {
            return map2;
        }

        public void setMap2(Map<List<K[]>, List<B[][]>> map2) {
            this.map2 = map2;
        }

        public Map<List<String[]>, B[][]> getMap3() {
            return map3;
        }

        public void setMap3(Map<List<String[]>, B[][]> map3) {
            this.map3 = map3;
        }

        public Map<List<B[][]>, Byte> getMap4() {
            return map4;
        }

        public void setMap4(Map<List<B[][]>, Byte> map4) {
            this.map4 = map4;
        }
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
    public void testDeserializeMapClassHonorsConfiguredMapInstanceType() {
        JsonDeserConfig config = JsonDeserConfig.create().setMapInstanceType(LinkedHashMap.class);
        Map<String, Object> result = parser.deserialize("{\"b\":1,\"a\":2}", config, Map.class);

        assertTrue(result instanceof LinkedHashMap);
        java.util.Iterator<String> iter = result.keySet().iterator();
        assertEquals("b", iter.next());
        assertEquals("a", iter.next());
    }

    @Test
    public void testDeserializeIgnoreNullOrEmptyKeepsEmptyKeyWithNonEmptyValue() {
        JsonDeserConfig config = JsonDeserConfig.create().setIgnoreNullOrEmpty(true).setMapValueType(String.class);
        Map<String, Object> result = parser.deserialize("{\"\":\"kept\",\"blank\":\"\"}", config, Map.class);

        assertEquals("kept", result.get(""));
        assertTrue(result.containsKey(""));
        assertTrue(!result.containsKey("blank"));
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
        parser.parseInto("[\"a\",\"b\",\"c\"]", null, output);
        Assertions.assertEquals("a", output[0]);
        Assertions.assertEquals("b", output[1]);
        Assertions.assertEquals("c", output[2]);

        Object[] output2 = new Object[3];
        parser.parseInto(null, null, output2);
        Assertions.assertNull(output2[0]);
    }

    @Test
    public void testParseIntoRejectsInsufficientArrayCapacityForWrappedAndUnwrappedValues() {
        for (final String source : List.of("1", "[1]", "\"one\"", "[\"one\"]", "true", "null", "[null]")) {
            // The caller owns this array; allocating a replacement would silently discard the parsed value.
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> parser.parseInto(source, new Object[0]), source);
            Assertions.assertThrows(IndexOutOfBoundsException.class, () -> parser.parseInto(source, JsonDeserConfig.create(), new Object[0]), source);
        }

        final Object[] output = { "old", "tail" };
        parser.parseInto("1", output);
        assertArrayEquals(new Object[] { 1, "tail" }, output);
        parser.parseInto("", output);
        parser.parseInto((String) null, output);
        parser.parseInto("[]", output);
        assertArrayEquals(new Object[] { 1, "tail" }, output);
        parser.parseInto("[]", new Object[0]);
        parser.parseInto("", new Object[0]);
        parser.parseInto((String) null, new Object[0]);
        parser.parseInto("null", JsonDeserConfig.create().setIgnoreNullOrEmpty(true), new Object[0]);
        parser.parseInto("[null]", JsonDeserConfig.create().setIgnoreNullOrEmpty(true), new Object[0]);
    }

    @Test
    public void testparse_OutputCollection() {
        List<String> output = new ArrayList<>();
        parser.parseInto("[\"x\",\"y\",\"z\"]", null, output);
        Assertions.assertEquals(3, output.size());
        Assertions.assertEquals("x", output.get(0));

        Set<Integer> outputSet = new HashSet<>();
        parser.parseInto("[1,2,3]", null, outputSet);
        Assertions.assertEquals(3, outputSet.size());
        Assertions.assertTrue(outputSet.contains(1));

        List<String> output2 = new ArrayList<>();
        parser.parseInto(null, null, output2);
        Assertions.assertTrue(output2.isEmpty());
    }

    @Test
    public void testparse_OutputMap() {
        Map<String, Object> output = new HashMap<>();
        parser.parseInto("{\"a\":1,\"b\":2}", null, output);
        Assertions.assertEquals(2, output.size());
        Assertions.assertEquals(1, output.get("a"));
        Assertions.assertEquals(2, output.get("b"));

        Map<String, Object> output2 = new HashMap<>();
        parser.parseInto("", null, output2);
        Assertions.assertTrue(output2.isEmpty());
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
        parser.parseInto(json, null, array);

        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    public void testparseToCollection() {
        String json = "[\"a\",\"b\",\"c\"]";
        List<String> list = new ArrayList<>();
        parser.parseInto(json, null, list);

        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testparseToMap() {
        String json = "{\"key1\": \"value1\",\"key2\": \"value2\"}";
        Map<String, String> map = new HashMap<>();
        parser.parseInto(json, null, map);

        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
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
        Sheet<String, Integer, Integer> sheet = new Sheet<>(Arrays.asList("R1", "R2"), Arrays.asList(1, 2), new Integer[][] { { 10, 20 }, { 30, 40 } });
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

    @Test
    public void test_serialize_integer() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize(123);
        assertEquals("123", json);
    }

    @Test
    public void test_serialize_boolean() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize(true);
        assertEquals("true", json);
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
    public void testSerializeObject() {
        Person person = new Person("Jane", 25);
        String json = parser.serialize(person);

        assertTrue(json.contains("\"name\": \"Jane\""));
        assertTrue(json.contains("\"age\": 25"));
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
        Integer[][] data = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
        Sheet<String, String, Integer> sheet = new Sheet<>(rowKeys, columnKeys, data);

        sheet.println();

        sheet.freeze();
        String json = parser.serialize(sheet,
                JsonSerConfig.create().setWriteColumnType(true).setWriteRowColumnKeyType(true).setPrettyFormat(true).setQuotePropName(true));

        Sheet<String, String, Integer> sheet2 = parser.deserialize(json, Sheet.class);
        assertEquals(sheet, sheet2);
        assertTrue(sheet2.isFrozen());
    }

    @Test
    public void testSerializeSheetWithNumericColumnKeys() {
        List<String> rowKeys = Arrays.asList("R1", "R2");
        List<Integer> columnKeys = Arrays.asList(1, 2);
        Integer[][] data = { { 10, 20 }, { 30, 40 } };
        Sheet<String, Integer, Integer> sheet = new Sheet<>(rowKeys, columnKeys, data);

        sheet.freeze();
        String json = parser.serialize(sheet, JsonSerConfig.create().setWriteColumnType(true).setWriteRowColumnKeyType(true).setQuotePropName(true));

        assertTrue(json.contains("\"columnKeyType\""));

        Sheet<String, Integer, Integer> sheet2 = parser.deserialize(json, Sheet.class);
        assertEquals(sheet, sheet2);
        assertTrue(sheet2.isFrozen());
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
    public void testSerializeMapEntityWithNullValue() {
        MapEntity entity = new MapEntity("TestEntity");
        entity.set("prop1", "value1");
        entity.set("prop2", null);

        String json = parser.serialize(entity);
        assertTrue(json.contains("TestEntity"));
        assertTrue(json.contains("value1"));
        assertTrue(json.contains("\"prop2\": null"), json);
    }

    @Test
    public void testSerializeEntityIdWithNullValue() {
        Seid entityId = Seid.of("TestId");
        entityId.set("id", 123);
        entityId.set("type", null);

        String json = parser.serialize(entityId);
        assertTrue(json.contains("TestId"));
        assertTrue(json.contains("\"type\": null"), json);
    }

    @Test
    public void test_serialize_string() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize("test");
        assertNotNull(json);
    }

    @Test
    public void test_serialize_null() {
        JsonParser parser = new JsonParserImpl();
        String json = parser.serialize(null);
        assertEquals("", json);
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
    public void testSpecialCases_CircularReference() {
        JsonSerConfig config = JsonSerConfig.create().setCircularReferenceSupported(true);

        Map<String, Object> map = new HashMap<>();
        map.put("self", map);

        String result = parser.serialize(map, config);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("\"self\": "));
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
    public void testSerializeNull() {
        String result = parser.serialize(null);
        assertEquals("", result);
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
    public void test_deserialize_string() {
        JsonParser parser = new JsonParserImpl();
        String result = parser.deserialize("\"test\"", String.class);
        assertEquals("\"test\"", result);
    }

    @Test
    public void test_deserialize_integer() {
        JsonParser parser = new JsonParserImpl();
        Integer result = parser.deserialize("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    public void test_deserialize_boolean() {
        JsonParser parser = new JsonParserImpl();
        Boolean result = parser.deserialize("true", Boolean.class);
        assertEquals(true, result);
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
    public void testDeserializationConfig() {
        String json = "{\"name\": \"Test\",\"unknownField\": \"value\"}";

        JsonDeserConfig config = JsonDeserConfig.create().setIgnoreUnmatchedProperty(true);

        Person person = parser.deserialize(json, config, Person.class);
        assertEquals("Test", person.getName());
    }

    @Test
    public void testDeserializeMapEntity() {
        MapEntity entity = new MapEntity("TestEntity");
        entity.set("prop1", "value1");
        entity.set("prop2", 123);

        String json = parser.serialize(entity);
        assertTrue(json.contains("TestEntity"));
        assertTrue(json.contains("prop1"));
        assertTrue(json.contains("value1"));

        MapEntity entity2 = parser.deserialize(json, MapEntity.class);
        assertEquals(entity, entity2);
    }

    @Test
    public void testDeserializeEntityId() {
        Seid entityId = Seid.of("TestId");
        entityId.set("id", 123);
        entityId.set("type", "test");

        String json = parser.serialize(entityId);
        assertTrue(json.contains("TestId"));
        assertTrue(json.contains("\"id\": 123"));

        Seid entityId2 = parser.deserialize(json, Seid.class);
        assertEquals(entityId, entityId2);
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
    public void testDeserialize_EmptyStreamingSources_ReturnEmptyArrayOrCollection() throws IOException {
        assertArrayEquals(new Object[0], parser.deserialize(new StringReader(""), null, Object[].class));
        Assertions.assertTrue(parser.deserialize(new StringReader(""), null, List.class).isEmpty());

        assertArrayEquals(new Object[0], parser.deserialize(new ByteArrayInputStream(new byte[0]), null, Object[].class));
        Assertions.assertTrue(parser.deserialize(new ByteArrayInputStream(new byte[0]), null, List.class).isEmpty());

        File file = tempDir.resolve("empty.json").toFile();
        Files.write(file.toPath(), new byte[0]);

        assertArrayEquals(new Object[0], parser.deserialize(file, null, Object[].class));
        Assertions.assertTrue(parser.deserialize(file, null, List.class).isEmpty());
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
    public void testDeserializeDataset_NullAndScalarInput() {
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("null", Dataset.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("1", Dataset.class));
    }

    @Test
    public void testDeserializeSheet_NullAndScalarInput() {
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("null", Sheet.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("1", Sheet.class));
    }

    @Test
    public void testDeserializeBean_ScalarInput() {
        ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("1", TestBean.class));
        assertTrue(exception.getMessage().contains("Can't parse"));
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
        assertTrue(json.contains("ParameterizedBean"));

        ParameterizedBean<Integer, String, Double, Byte, Short> fromFastJson = FastJson.fromJson(json,
                new TypeReference<ParameterizedBean<Integer, String, Double, Byte, Short>>() {
                }.javaType());
        assertParameterizedBeanTypes(json, fromFastJson);

        Type<ParameterizedBean<Integer, String, Double, Byte, Short>> type = Type
                .of(new TypeReference<ParameterizedBean<Integer, String, Double, Byte, Short>>() {
                }.javaType());
        assertTrue(type.name().contains("ParameterizedBean"));
        assertNotNull(type.javaType());

        ParameterizedBean<Integer, String, Double, Byte, Short> fromN = N.fromJson(json, type);
        assertParameterizedBeanTypes(json, fromN);
    }

    private static void assertParameterizedBeanTypes(String json, ParameterizedBean<Integer, String, Double, Byte, Short> ret) {
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
    public void testStreamFromString() {
        String json = "[{\"name\": \"A\",\"age\":1},{\"name\": \"B\",\"age\":2},{\"name\": \"C\",\"age\":3}]";

        List<Person> people = parser.stream(json, null, Type.of(Person.class)).toList();

        assertEquals(3, people.size());
        assertEquals("A", people.get(0).getName());
        assertEquals("B", people.get(1).getName());
        assertEquals("C", people.get(2).getName());
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
    public void testUnsupportedTypes() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            parser.stream("[1,2,3]", null, Type.of(StringBuilder.class));
        });
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
    public void testSerializeMapEntity_NestedCollections() {
        MapEntity entity = new MapEntity("NestedEntity");
        entity.set("values", Arrays.asList("a", "b"));
        entity.set("config", N.asMap("enabled", true, "level", 2));

        String json = parser.serialize(entity);
        MapEntity parsed = parser.deserialize(json, MapEntity.class);

        assertTrue(json.contains("\"values\""));
        assertEquals(entity, parsed);
    }

    @Test
    public void testDeserializeDataset_invalidJsonText_throws() {
        // Regression: malformed row JSON (bare unquoted text) must be rejected by readDataset,
        // mirroring readMap's validation (#6).
        Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> parser.deserialize("[{abc}]", Dataset.class));
    }

    // ----- Pair / Triple / Tuple deserialization (list2PairTripleConverterMap) -----

    @Test
    public void testDeserialize_Pair() {
        Type<com.landawn.abacus.util.Pair<String, Integer>> type = Type.of(new TypeReference<com.landawn.abacus.util.Pair<String, Integer>>() {
        }.javaType());

        com.landawn.abacus.util.Pair<String, Integer> result = parser.deserialize("[\"a\", 1]", null, type);
        assertEquals("a", result.left());
        assertEquals(Integer.valueOf(1), result.right());
    }

    @Test
    public void testDeserialize_Triple() {
        Type<com.landawn.abacus.util.Triple<String, Integer, Boolean>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Triple<String, Integer, Boolean>>() {
                }.javaType());

        com.landawn.abacus.util.Triple<String, Integer, Boolean> result = parser.deserialize("[\"a\", 1, true]", null, type);
        assertEquals("a", result.left());
        assertEquals(Integer.valueOf(1), result.middle());
        assertEquals(Boolean.TRUE, result.right());
    }

    @Test
    public void testDeserialize_Tuple2() {
        Type<com.landawn.abacus.util.Tuple.Tuple2<String, Integer>> type = Type.of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple2<String, Integer>>() {
        }.javaType());

        com.landawn.abacus.util.Tuple.Tuple2<String, Integer> result = parser.deserialize("[\"x\", 5]", null, type);
        assertEquals("x", result._1);
        assertEquals(Integer.valueOf(5), result._2);
    }

    @Test
    public void testDeserialize_Tuple3() {
        Type<com.landawn.abacus.util.Tuple.Tuple3<String, Integer, Boolean>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple3<String, Integer, Boolean>>() {
                }.javaType());

        com.landawn.abacus.util.Tuple.Tuple3<String, Integer, Boolean> result = parser.deserialize("[\"x\", 5, false]", null, type);
        assertEquals("x", result._1);
        assertEquals(Integer.valueOf(5), result._2);
        assertEquals(Boolean.FALSE, result._3);
    }

    @Test
    public void testDeserialize_Tuple1() {
        Type<com.landawn.abacus.util.Tuple.Tuple1<String>> type = Type.of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple1<String>>() {
        }.javaType());

        com.landawn.abacus.util.Tuple.Tuple1<String> result = parser.deserialize("[\"only\"]", null, type);
        assertEquals("only", result._1);
    }

    @Test
    public void testDeserialize_Tuple4() {
        Type<com.landawn.abacus.util.Tuple.Tuple4<Integer, Integer, Integer, Integer>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple4<Integer, Integer, Integer, Integer>>() {
                }.javaType());

        com.landawn.abacus.util.Tuple.Tuple4<Integer, Integer, Integer, Integer> result = parser.deserialize("[1, 2, 3, 4]", null, type);
        assertEquals(Integer.valueOf(1), result._1);
        assertEquals(Integer.valueOf(2), result._2);
        assertEquals(Integer.valueOf(3), result._3);
        assertEquals(Integer.valueOf(4), result._4);
    }

    @Test
    public void testDeserialize_Tuple5() {
        Type<com.landawn.abacus.util.Tuple.Tuple5<Integer, Integer, Integer, Integer, Integer>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple5<Integer, Integer, Integer, Integer, Integer>>() {
                }.javaType());

        com.landawn.abacus.util.Tuple.Tuple5<Integer, Integer, Integer, Integer, Integer> result = parser.deserialize("[1, 2, 3, 4, 5]", null, type);
        assertEquals(Integer.valueOf(5), result._5);
    }

    @Test
    public void testDeserialize_Tuple6() {
        Type<com.landawn.abacus.util.Tuple.Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple6<Integer, Integer, Integer, Integer, Integer, Integer>>() {
                }.javaType());

        com.landawn.abacus.util.Tuple.Tuple6<Integer, Integer, Integer, Integer, Integer, Integer> result = parser.deserialize("[1, 2, 3, 4, 5, 6]", null,
                type);
        assertEquals(Integer.valueOf(6), result._6);
    }

    @Test
    public void testDeserialize_Tuple7() {
        Type<com.landawn.abacus.util.Tuple.Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer>>() {
                }.javaType());

        com.landawn.abacus.util.Tuple.Tuple7<Integer, Integer, Integer, Integer, Integer, Integer, Integer> result = parser.deserialize("[1, 2, 3, 4, 5, 6, 7]",
                null, type);
        assertEquals(Integer.valueOf(7), result._7);
    }

    @Test
    public void testDeserialize_Tuple8() {
        Type<com.landawn.abacus.util.Tuple.Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>>() {
                }.javaType());

        com.landawn.abacus.util.Tuple.Tuple8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> result = parser
                .deserialize("[1, 2, 3, 4, 5, 6, 7, 8]", null, type);
        assertEquals(Integer.valueOf(8), result._8);
    }

    @Test
    public void testDeserialize_Tuple9() {
        Type<com.landawn.abacus.util.Tuple.Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>> type = Type
                .of(new TypeReference<com.landawn.abacus.util.Tuple.Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>>() {
                }.javaType());

        com.landawn.abacus.util.Tuple.Tuple9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer> result = parser
                .deserialize("[1, 2, 3, 4, 5, 6, 7, 8, 9]", null, type);
        assertEquals(Integer.valueOf(9), result._9);
    }

    // ----- Map.Entry conversion (map2TargetTypeConverterMap) -----

    @Test
    public void testDeserialize_MapEntry() {
        Type<Map.Entry<String, Integer>> type = Type.of(new TypeReference<Map.Entry<String, Integer>>() {
        }.javaType());

        Map.Entry<String, Integer> result = parser.deserialize("{\"key\":42}", null, type);
        assertEquals("key", result.getKey());
        assertEquals(Integer.valueOf(42), result.getValue());
    }

    @Test
    public void testDeserialize_SimpleImmutableEntry() {
        Type<java.util.AbstractMap.SimpleImmutableEntry<String, Integer>> type = Type
                .of(new TypeReference<java.util.AbstractMap.SimpleImmutableEntry<String, Integer>>() {
                }.javaType());

        java.util.AbstractMap.SimpleImmutableEntry<String, Integer> result = parser.deserialize("{\"k\":7}", null, type);
        assertEquals("k", result.getKey());
        assertEquals(Integer.valueOf(7), result.getValue());
    }

    @Test
    public void testDeserialize_SimpleEntry() {
        // SimpleEntry is not special-cased by the if/else chain in readBracedValueBody; it falls
        // through Map.Entry.isAssignableFrom and yields a generic Map.Entry (not a SimpleEntry).
        Type<Map.Entry<String, Integer>> type = Type.of(new TypeReference<Map.Entry<String, Integer>>() {
        }.javaType());

        Map.Entry<String, Integer> result = parser.deserialize("{\"m\":9}", null, type);
        assertTrue(result instanceof Map.Entry);
        assertEquals("m", result.getKey());
        assertEquals(Integer.valueOf(9), result.getValue());
    }

    // ----- Object.class root dispatch to List / Map -----

    @Test
    public void testDeserialize_ObjectClass_BracketRoot() {
        Object listResult = parser.deserialize("[1, 2, 3]", null, Object.class);
        assertTrue(listResult instanceof List);
        assertEquals(Arrays.asList(1, 2, 3), listResult);
    }

    // ----- Serialization config: wrapRootValue / bracketRootValue / writeNullToEmpty -----

    @Test
    public void testSerialize_WrapRootValue() {
        JsonSerConfig config = JsonSerConfig.create().setWrapRootValue(true).setQuotePropName(true);

        Person person = new Person("Wrapped", 1);
        String json = parser.serialize(person, config);

        assertTrue(json.contains("\"Person\""));
        assertTrue(json.contains("\"name\""));
        assertTrue(json.contains("\"Wrapped\""));
    }

    @Test
    public void testSerialize_WrapRootValue_PrettyFormat() {
        JsonSerConfig config = JsonSerConfig.create().setWrapRootValue(true).setPrettyFormat(true).setQuotePropName(true);

        Person person = new Person("WrappedPretty", 2);
        String json = parser.serialize(person, config);

        assertTrue(json.contains("\n"));
        assertTrue(json.contains("\"Person\""));
        assertTrue(json.contains("\"WrappedPretty\""));
    }

    @Test
    public void testSerialize_WrapRootValue_NoQuotePropName() {
        JsonSerConfig config = JsonSerConfig.create().setWrapRootValue(true).setQuotePropName(false);

        Person person = new Person("NoQuote", 3);
        String json = parser.serialize(person, config);

        assertTrue(json.contains("Person"));
        assertTrue(json.contains("NoQuote"));
    }

    @Test
    public void testSerialize_BracketRootValueFalse_Collection() {
        JsonSerConfig config = JsonSerConfig.create().setBracketRootValue(false);

        List<Integer> list = Arrays.asList(1, 2, 3);
        String json = parser.serialize(list, config);

        // Without the enclosing brackets the elements are written inline.
        assertEquals("1, 2, 3", json);
    }

    @Test
    public void testSerialize_BracketRootValueFalse_ObjectArray() {
        JsonSerConfig config = JsonSerConfig.create().setBracketRootValue(false);

        String[] array = { "a", "b" };
        String json = parser.serialize(array, config);

        assertEquals("\"a\", \"b\"", json);
    }

    @Test
    public void testSerialize_BracketRootValueFalse_PrimitiveArray() {
        JsonSerConfig config = JsonSerConfig.create().setBracketRootValue(false);

        int[] array = { 1, 2, 3 };
        String json = parser.serialize(array, config);

        assertEquals("1, 2, 3", json);
    }

    // writeNullToEmpty covers collection/map/charSequence/other null-value rendering
    @Test
    public void testSerialize_WriteNullToEmpty_BeanProperties() {
        // Exclusion.NONE keeps null properties so the writeNullToEmpty branch is exercised.
        JsonSerConfig config = JsonSerConfig.create().setWriteNullToEmpty(true).setExclusion(Exclusion.NONE);

        TestBean bean = new TestBean();
        bean.setName(null);
        bean.setTags(null);
        bean.setMetadata(null);

        String json = parser.serialize(bean, config);

        // null String -> "", null collection -> [], null map -> {}
        assertTrue(json.contains("\"name\": \"\""));
        assertTrue(json.contains("\"tags\": []"));
        assertTrue(json.contains("\"metadata\": {}"));
    }

    @Test
    public void testSerialize_WriteDatasetAsRows() {
        List<String> columnNames = Arrays.asList("col1", "col2");
        List<List<Object>> columnList = new ArrayList<>();
        columnList.add(Arrays.asList("a", "b"));
        columnList.add(Arrays.asList(1, 2));

        Dataset ds = N.newDataset(columnNames, columnList);
        JsonSerConfig config = JsonSerConfig.create().setWriteDatasetAsRows(true);

        String json = parser.serialize(ds, config);
        assertNotNull(json);

        Dataset ds2 = parser.deserialize(json, null, Dataset.class);
        assertEquals(ds, ds2);
    }

    // ----- Map with null key, quoteMapKey=false (writes bare null) -----

    @Test
    public void testSerialize_MapNullKey_NoQuoteMapKey() {
        JsonSerConfig config = JsonSerConfig.create().setQuoteMapKey(false);

        Map<String, Object> map = new HashMap<>();
        map.put(null, "value");

        String json = parser.serialize(map, config);
        assertTrue(json.contains("null"));
        assertTrue(json.contains("value"));
    }

    // ----- Simple serializable value to File / OutputStream (fast IOUtil.write path) -----

    @Test
    public void testSerialize_SimpleValue_ToFile() throws IOException {
        File file = tempDir.resolve("simple.json").toFile();
        parser.serialize(Integer.valueOf(123), null, file);

        Assertions.assertTrue(file.exists());
        assertEquals("123", new String(Files.readAllBytes(file.toPath())));
    }

    @Test
    public void testSerialize_SimpleValue_ToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(Integer.valueOf(456), null, baos);

        assertEquals("456", baos.toString());
    }

    @Test
    public void testSerialize_SimpleStringValue_ToFile() throws IOException {
        File file = tempDir.resolve("simpleStr.json").toFile();
        parser.serialize("hello", null, file);

        Assertions.assertTrue(file.exists());
        assertEquals("hello", new String(Files.readAllBytes(file.toPath())));
    }

    // ----- JSON nesting depth guard (defends against stack-overflow DoS) -----

    @Test
    public void testDeserialize_NestingDepthExceeded() {
        // Build deeply nested arrays exceeding the 256-step nested parsing budget.
        int depth = 1100;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append('[');
        }
        for (int i = 0; i < depth; i++) {
            sb.append(']');
        }

        ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(sb.toString(), null, List.class));
        assertTrue(exception.getMessage().contains("nesting depth exceeded"));
    }

    @Test
    public void testDeserialize_NestingDepthExceeded_Braces() {
        int depth = 1100;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append("{\"a\":");
        }
        sb.append("1");
        for (int i = 0; i < depth; i++) {
            sb.append('}');
        }

        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(sb.toString(), null, Map.class));
    }

    @Test
    void nestedParsingBudgetRejectsBeforeExhaustingTheDefaultStackAndRecovers() {
        // Repeat rejection and boundary parsing so both cold and compiled recursive paths are exercised.
        for (int round = 0; round < 20; round++) {
            final String leaf = round % 3 == 0 ? "null" : round % 3 == 1 ? "\"\"" : "\"\u00e9\ud83d\ude42\"";
            final Object expectedLeaf = round % 3 == 0 ? null : round % 3 == 1 ? "" : "\u00e9\ud83d\ude42";
            for (int shape = 0; shape < 3; shape++) {
                final Class<?> rootClass = shape == 1 ? List.class : Map.class;
                for (final int depth : new int[] { 1100, 258, 257, 256, 1 }) {
                    final String json = nestedBudgetJson(shape, depth, leaf);
                    for (final boolean useReader : new boolean[] { false, true }) {
                        if (depth > 257) {
                            final ParsingException failure = Assertions.assertThrows(ParsingException.class, () -> {
                                if (useReader) {
                                    parser.deserialize(new StringReader(json), null, rootClass);
                                } else {
                                    parser.deserialize(json, null, rootClass);
                                }
                            });
                            assertTrue(failure.getMessage().contains("nesting depth exceeded 256"));
                        } else {
                            Object value = useReader ? parser.deserialize(new StringReader(json), null, rootClass) : parser.deserialize(json, null, rootClass);
                            // Inspect iteratively: recursive equality would itself depend on the test thread's stack.
                            for (int level = 0; level < depth; level++) {
                                value = value instanceof Map<?, ?> map ? map.get("a") : ((List<?>) value).get(0);
                            }
                            assertEquals(expectedLeaf, value);
                        }
                    }
                }
            }
        }
        assertEquals(Map.of(), parser.deserialize("{}", Map.class));
        assertEquals(List.of(), parser.deserialize("[]", List.class));
    }

    @Test
    void nestedParsingBudgetIncludesTypedWrapperDispatch() {
        final String[] elementTypes = { "java.lang.Object", "com.landawn.abacus.util.Holder<java.lang.Object>",
                "com.landawn.abacus.util.u.Optional<com.landawn.abacus.util.Holder<java.lang.Object>>" };
        for (int wrappers = 0; wrappers < elementTypes.length; wrappers++) {
            final Type<List<?>> type = Type.of("java.util.List<" + elementTypes[wrappers] + ">");
            final int allowedMaps = 256 - wrappers;
            final String rejected = "[" + nestedBudgetJson(0, allowedMaps + 1, "null") + "]";
            final String accepted = "[" + nestedBudgetJson(0, allowedMaps, "null") + "]";
            for (final boolean useReader : new boolean[] { false, true }) {
                Assertions.assertThrows(ParsingException.class, () -> {
                    if (useReader) {
                        parser.deserialize(new StringReader(rejected), null, type);
                    } else {
                        parser.deserialize(rejected, null, type);
                    }
                });
                final List<?> values = useReader ? parser.deserialize(new StringReader(accepted), null, type) : parser.deserialize(accepted, null, type);
                assertEquals(1, values.size());
                Object value = values.get(0);
                if (value instanceof com.landawn.abacus.util.u.Optional<?> optional) {
                    value = optional.get();
                }
                if (value instanceof com.landawn.abacus.util.Holder<?> holder) {
                    value = holder.value();
                }
                for (int level = 0; level < allowedMaps; level++) {
                    value = ((Map<?, ?>) value).get("a");
                }
                assertNull(value);
            }
        }
    }

    private static String nestedBudgetJson(final int shape, final int depth, final String leaf) {
        final StringBuilder json = new StringBuilder();
        for (int level = 0; level < depth; level++) {
            json.append(shape == 0 || shape == 2 && level % 2 == 0 ? "{\"a\":" : "[");
        }
        json.append(leaf);
        for (int level = depth - 1; level >= 0; level--) {
            json.append(shape == 0 || shape == 2 && level % 2 == 0 ? '}' : ']');
        }
        return json.toString();
    }

    // ----- Error message branches via invalid token positions -----

    @Test
    public void testDeserialize_InvalidColonInArray() {
        // A colon inside an array (not allowed) should trigger a ParsingException.
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[1:2]", null, List.class));
    }

    @Test
    public void testDeserialize_InvalidCommaAsPropNameInBean() {
        // Leading comma where a property name is expected in a bean.
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{,\"name\":\"x\"}", null, Person.class));
    }

    @Test
    public void testDeserialize_UnknownPropertyInBean_Throws() {
        // ignoreUnmatchedProperty defaults to true; disable it so unmatched props throw "Unknown property".
        JsonDeserConfig config = JsonDeserConfig.create().setIgnoreUnmatchedProperty(false);
        ParsingException exception = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"nope\":1}", config, Person.class));
        assertTrue(exception.getMessage().contains("Unknown property"));
    }

    @Test
    public void testDeserialize_UnwrappedArrayMismatch_Throws() {
        // Object expected to be wrapped but text ends with bracket mismatch.
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[1,2}", null, List.class));
    }

    @Test
    public void testDeserializeRejectsWhitespaceInsideUnquotedValue() {
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[1 2]", null, List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader("[fal se]"), null, List.class));
        assertEquals(Arrays.asList(1, 2), parser.deserialize("[ 1 , 2 ]", null, List.class));
    }

    @Test
    public void testDeserializeRejectsContentAfterStructuredRoot() {
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"a\":1} trailing", null, Map.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[1][2]", null, List.class));
        assertEquals(1, parser.deserialize("{\"a\":1}   ", null, Map.class).size());
    }

    @Test
    public void testDeserializeRejectsMissingArraySeparators() {
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[{} {}]", null, List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[[] []]", null, List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[{} 1]", null, List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[\"a\" \"b\"]", null, List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[{} {}]", null, Object[].class));
        Assertions.assertThrows(ParsingException.class, () -> parser.parseInto("[{} {}]", null, new Object[2]));

        assertEquals(2, parser.deserialize("[{}, {}]", null, List.class).size());
        assertArrayEquals(new Object[] { "a", "b" }, parser.deserialize("[\"a\", \"b\"]", null, Object[].class));
    }

    @Test
    public void testDeserializeDatasetRejectsTruncatedArrayAtEof() {
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[", Dataset.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[{\"a\":1}", Dataset.class));
    }

    @Test
    public void testDeserializeRejectsMalformedObjectSeparators() {
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"a\":{} \"b\"}", null, Map.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"a\" \"b\":1}", null, Map.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{:1}", null, Map.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"a\":1,}", null, Map.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"unknown\":{} \"junk\"}", null, TestBean.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"unknown\":{},}", null, TestBean.class));

        assertEquals(2, parser.deserialize("{\"a\":{}, \"b\":1}", null, Map.class).size());
        assertEquals(1, parser.deserialize("{\"\":1}", null, Map.class).size());
    }

    // ----- readNullToEmpty for collection/array/map/charSequence target types -----

    @Test
    public void testDeserialize_ReadNullToEmpty_VariousTypes() {
        JsonDeserConfig config = JsonDeserConfig.create().setReadNullToEmpty(true);

        int[] arr = parser.deserialize((String) null, config, int[].class);
        assertNotNull(arr);
        assertEquals(0, arr.length);

        Set<String> set = parser.deserialize((String) null, config, Set.class);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    // ----- Stream over unsupported (non Collection/Array) JSON top-level -----

    @Test
    public void testStream_UnsupportedNonArray_Throws() {
        // A bean-typed stream over object JSON: only Collection/Array JSON supported by stream methods.
        Assertions.assertThrows(Exception.class, () -> parser.stream("{\"a\":1}", null, Type.of(StringBuilder.class)).toList());
    }

    @Test
    public void testStreamRejectsNullElementType() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.stream("[]", null, (Type<Object>) null));
    }

    // --- regression tests for 2026-06-11 deep-review fixes ---

    @Test
    public void testStream_SingleScalarAndEmptyInnerArrays_NoSpuriousNulls() {
        // regression: after an element consuming exactly one token ([1] or []), the stream
        // iterator reused jr.lastToken() as its sentinel and injected spurious null elements
        Assertions.assertEquals(java.util.List.of(java.util.List.of(1), java.util.List.of(2)),
                parser.stream("[[1],[2]]", null, Type.of(java.util.List.class)).toList());
        Assertions.assertEquals(java.util.List.of(java.util.List.of(), java.util.List.of(1)),
                parser.stream("[[],[1]]", null, Type.of(java.util.List.class)).toList());
    }

    @Test
    public void testStreamRequiresSeparatorsClosingBracketAndEndOfInput() {
        Assertions.assertThrows(ParsingException.class, () -> parser.stream("garbage", null, Type.of(java.util.List.class)).toList());
        Assertions.assertThrows(ParsingException.class, () -> parser.stream("[[1]] trailing", null, Type.of(java.util.List.class)).toList());
        Assertions.assertThrows(ParsingException.class, () -> parser.stream("[[1]", null, Type.of(java.util.List.class)).toList());
        Assertions.assertThrows(ParsingException.class, () -> parser.stream("[[1] [2]]", null, Type.of(java.util.List.class)).toList());
    }

    @Test
    public void testReadNumber_17DigitDoubleRoundTripAndNegativeZero() {
        // regression: the decimal fast path divided an inexact (double) long, mis-parsing ~15% of
        // 17-significant-digit doubles by 1 ulp; "-0.0" lost its sign; bare "-" parsed as 0
        final double d = 1913.4547307115454d;
        Assertions.assertEquals(d, (Double) parser.deserialize(parser.serialize(java.util.List.of(d)), java.util.List.class).get(0));

        final java.util.List<Object> nz = parser.deserialize("[-0.0]", java.util.List.class);
        Assertions.assertEquals(Double.doubleToLongBits(-0.0d), Double.doubleToLongBits(((Number) nz.get(0)).doubleValue()));
    }

    @Test
    public void testWriteDataset_ColumnNameWithQuoteIsEscaped() {
        // regression: Dataset/Sheet/MapEntity/EntityId names were written unescaped -> invalid JSON
        final com.landawn.abacus.util.Dataset ds = com.landawn.abacus.util.Dataset.rows(java.util.List.of("a\"b"), new Object[][] { { 1 }, { 2 } });
        final String json = parser.serialize(ds);

        Assertions.assertTrue(json.contains("\"a\\\"b\": [1, 2]"), json);
        Assertions.assertEquals(ds, parser.deserialize(json, com.landawn.abacus.util.Dataset.class));
    }

    @Test
    public void testReadDataset_EmptyRowsArePositionIndependent() {
        // regression: a stale key from the previous row made empty {} rows throw position-dependently
        final com.landawn.abacus.util.Dataset ds1 = parser.deserialize("[{\"a\":1},{}]", com.landawn.abacus.util.Dataset.class);
        Assertions.assertEquals(2, ds1.size());

        final com.landawn.abacus.util.Dataset ds2 = parser.deserialize("[{\"a\":1},{},{\"a\":2}]", com.landawn.abacus.util.Dataset.class);
        Assertions.assertEquals(3, ds2.size());
        Assertions.assertEquals(java.util.Arrays.asList(1, null, 2), ds2.getColumn("a"));
    }

    @Test
    public void testReadSheet_UsesDeclaredGenericTypesWithoutSerializedTypeMetadata() {
        final Type<Sheet<String, Integer, Long>> sheetType = Type.of(new TypeReference<Sheet<String, Integer, Long>>() {
        });
        final Sheet<String, Integer, Long> source = Sheet.rows(List.of("row"), List.of(7), new Long[][] { { 9L } });
        final String json = parser.serialize(source);

        final Sheet<String, Integer, Long> parsed = parser.deserialize(json, null, sheetType);

        assertEquals(String.class, parsed.rowKeySet().iterator().next().getClass());
        assertEquals(Integer.class, parsed.columnKeySet().iterator().next().getClass());
        assertEquals(Long.class, parsed.get("row", 7).getClass());
        assertEquals(9L, parsed.get("row", 7));
    }

    @Test
    public void testParse_EmptySourceIntoCollectionIsNoOp() {
        // regression: parse("", collection) fabricated a spurious "" element
        final java.util.List<String> out = new java.util.ArrayList<>();
        parser.parseInto("", null, out);
        Assertions.assertTrue(out.isEmpty());
    }

    @Test
    public void testCircularReference_WritesNullPlaceholder() {
        // regression: the cycle skip emitted '"name": ' with no value -> malformed JSON
        final java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("name", "x");
        m.put("self", m);

        final String json = parser.serialize(m, new JsonSerConfig().setCircularReferenceSupported(true));

        Assertions.assertTrue(json.contains("null"), json);
        Assertions.assertFalse(json.matches(".*:\\s*[,}].*"), json); // every name has a value
    }

    @Test
    public void testSerializeArrayToOutputStream_bracketRootValueFalse() {
        // regression: serializable arrays/collections with bracketRootValue=false were written to the
        // BufferedJsonWriter's internal OutputStreamWriter but never flushed to the underlying
        // OutputStream -> total silent data loss. The String overload (source of truth) always worked.
        final JsonSerConfig config = new JsonSerConfig().setBracketRootValue(false);

        final String[] strArray = { "a", "b" };
        final String expectedStr = parser.serialize(strArray, config);
        final ByteArrayOutputStream os1 = new ByteArrayOutputStream();
        parser.serialize(strArray, config, os1);
        assertEquals(expectedStr, new String(os1.toByteArray()));
        assertTrue(os1.size() > 0);

        final int[] intArray = { 1, 2, 3 };
        final String expectedInt = parser.serialize(intArray, config);
        final ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        parser.serialize(intArray, config, os2);
        assertEquals(expectedInt, new String(os2.toByteArray()));
        assertTrue(os2.size() > 0);
    }

    @Test
    public void testSerialize_IOExceptionFromWriter() {
        final Writer failingWriter = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("write failed");
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("flush failed");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };

        final UncheckedIOException ex = Assertions.assertThrows(UncheckedIOException.class,
                () -> parser.serialize(N.asMap("name", "json"), JsonSerConfig.create(), failingWriter));

        Assertions.assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    public void testDeserialize_IOExceptionFromReader() {
        final Reader failingReader = new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };

        final UncheckedIOException ex = Assertions.assertThrows(UncheckedIOException.class,
                () -> parser.deserialize(failingReader, JsonDeserConfig.create(), Map.class));

        Assertions.assertTrue(ex.getCause() instanceof IOException);
    }

    // ---------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06, P7 (JsonStreamReader/JsonStringReader): end-to-end cases.
    // ---------------------------------------------------------------------------------------------

    private static Reader reviewFixes20260906_P7_reader(final String json) {
        return new StringReader(json);
    }

    private static java.io.InputStream reviewFixes20260906_P7_inputStream(final String json) {
        return new ByteArrayInputStream(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private File reviewFixes20260906_P7_file(final String name, final String content) throws IOException {
        final File file = tempDir.resolve(name).toFile();
        Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return file;
    }

    // P7-01 (blocker): a document that ends with whitespace (every JSON file on disk ends with a newline) was
    // rejected with "Unexpected content after the root JSON value" on the Reader/InputStream/File overloads
    // and by stream(Reader/InputStream/File), because JsonStreamReader reported the stale buffer tail as text.
    @Test
    public void reviewFixes20260906_P7_01_readerInputStreamFileWithTrailingWhitespace() throws IOException {
        final String[] tails = { "\n", "\r\n", "   ", " \t\n", "\n\n" };

        for (final String tail : tails) {
            final String label = "tail=" + tail.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");

            assertEquals(Map.of("a", 1), parser.deserialize(reviewFixes20260906_P7_reader("{\"a\":1}" + tail), Map.class), label);
            assertEquals(Map.of("a", 1), parser.deserialize(reviewFixes20260906_P7_inputStream("{\"a\":1}" + tail), Map.class), label);
            assertEquals(Map.of("a", 1), parser.deserialize(reviewFixes20260906_P7_file("p701_map.json", "{\"a\":1}" + tail), Map.class), label);

            assertEquals(Arrays.asList(1, 2), parser.deserialize(reviewFixes20260906_P7_reader("[1,2]" + tail), List.class), label);
            assertEquals(Arrays.asList(1, 2), parser.deserialize(reviewFixes20260906_P7_inputStream("[1,2]" + tail), List.class), label);
            assertEquals(Arrays.asList(1, 2), parser.deserialize(reviewFixes20260906_P7_file("p701_list.json", "[1,2]" + tail), List.class), label);
            assertArrayEquals(new int[] { 1, 2 }, parser.deserialize(reviewFixes20260906_P7_reader("[1,2]" + tail), int[].class), label);
            assertEquals(Arrays.asList(1, 2), parser.deserialize(reviewFixes20260906_P7_reader("[1,2]" + tail), Object.class), label);

            final Person person = parser.deserialize(reviewFixes20260906_P7_file("p701_person.json", "{\"name\":\"x\",\"age\":3}" + tail), Person.class);
            assertEquals(new Person("x", 3), person, label);
            assertEquals(new Person("x", 3), parser.deserialize(reviewFixes20260906_P7_reader("{\"name\":\"x\",\"age\":3}" + tail), Person.class), label);

            assertEquals(List.of(Map.of("a", 1)), parser.stream(reviewFixes20260906_P7_reader("[{\"a\":1}]" + tail), true, Type.of(Map.class)).toList(), label);
            assertEquals(List.of(Map.of("a", 1)), parser.stream(reviewFixes20260906_P7_inputStream("[{\"a\":1}]" + tail), true, Type.of(Map.class)).toList(),
                    label);
            assertEquals(List.of(Map.of("a", 1)),
                    parser.stream(reviewFixes20260906_P7_file("p701_stream.json", "[{\"a\":1}]" + tail), Type.of(Map.class)).toList(), label);
            assertEquals(List.of(), parser.deserialize(reviewFixes20260906_P7_reader("[]" + tail), List.class), label);
            assertEquals(Map.of(), parser.deserialize(reviewFixes20260906_P7_reader("{}" + tail), Map.class), label);
        }

        // the document without a trailing newline was never affected
        assertEquals(Map.of("a", 1), parser.deserialize(reviewFixes20260906_P7_file("p701_nonl.json", "{\"a\":1}"), Map.class));
    }

    // P7-01: the pooled 16 KB buffer boundary - documents of length 16382..16386 plus a tail, through a Reader.
    @Test
    public void reviewFixes20260906_P7_01_pooledBufferBoundaryWithTrailingNewline() {
        for (final int docLength : new int[] { 8190, 8192, 8194, 16382, 16383, 16384, 16385, 16386, 32768 }) {
            // "[" + "1," x k + "1" x m + "]" with 1 + 2k + m + 1 == docLength (m is 2 or 3)
            final int k = (docLength - 2) / 2 - 1;
            final int m = docLength - 2 - 2 * k;
            final String doc = "[" + "1,".repeat(k) + "1".repeat(m) + "]";
            assertEquals(docLength, doc.length());
            final Integer last = Integer.valueOf("1".repeat(m));
            assertEquals(k + 1, parser.deserialize(doc, List.class).size());

            for (final String tail : new String[] { "\n", "\r\n", " " }) {
                final List<?> list = parser.deserialize(reviewFixes20260906_P7_reader(doc + tail), List.class);
                assertEquals(k + 1, list.size(), "docLength=" + docLength + " tail=" + (int) tail.charAt(0));
                assertEquals(last, list.get(list.size() - 1));
                assertEquals(k + 1, parser.stream(reviewFixes20260906_P7_reader("[" + doc + "]" + tail), true, Type.of(List.class)).first().get().size());
            }
        }
    }

    // P7-01: a whitespace-only Reader/InputStream yields the empty value, like the String overload (it used to
    // fabricate a single " " element from the phantom text).
    @Test
    public void reviewFixes20260906_P7_01_whitespaceOnlyReaderYieldsEmptyValue() {
        for (final String ws : new String[] { " ", "   ", "\n\n\t ", "\r\n" }) {
            assertEquals(List.of(), parser.deserialize(ws, List.class));
            assertEquals(List.of(), parser.deserialize(reviewFixes20260906_P7_reader(ws), List.class));
            assertEquals(List.of(), parser.deserialize(reviewFixes20260906_P7_inputStream(ws), List.class));
            assertArrayEquals(new Object[0], parser.deserialize(reviewFixes20260906_P7_reader(ws), Object[].class));
            assertEquals(Map.of(), parser.deserialize(reviewFixes20260906_P7_reader(ws), Map.class));
        }
    }

    // P7-01 negatives: real content after the root is still rejected through a Reader.
    @Test
    public void reviewFixes20260906_P7_01_contentAfterRootStillRejectedThroughReader() {
        for (final String bad : new String[] { "[1] x", "[1]\n[2]", "{\"a\":1}abc", "{\"a\":1}\n{\"b\":2}", "[1] 2" }) {
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(reviewFixes20260906_P7_reader(bad), List.class), bad);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(reviewFixes20260906_P7_reader(bad), Object.class), bad);
        }

        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(reviewFixes20260906_P7_reader("{\"a\":1} trailing"), Map.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.stream(reviewFixes20260906_P7_reader("[{}] x"), true, Type.of(Map.class)).toList());
    }

    // P7-02/P7-03: a near-miss literal used to swallow the structural char after it ("[tru,1]" -> ONE element
    // "tru,1"; "[t]" through a Reader -> ParsingException). Both overloads now agree with "[abc,1]".
    @Test
    public void reviewFixes20260906_P7_02_nearMissLiteralDoesNotFuseWithNeighbour() {
        for (final String json : new String[] { "[tru,1]", "[nul,1]", "[fals,1]", "[t,1]", "[f,1]", "[n,1]" }) {
            final String prefix = json.substring(1, json.indexOf(','));

            final List<?> fromString = parser.deserialize(json, List.class);
            assertEquals(2, fromString.size(), json);
            assertEquals(Arrays.asList(prefix, 1), fromString, json);
            assertEquals(Arrays.asList(prefix, 1), parser.deserialize(reviewFixes20260906_P7_reader(json), List.class), json);
            assertArrayEquals(new String[] { prefix, "1" }, parser.parse(json, String[].class), json);
        }

        assertEquals(Map.of("a", "tru", "b", 1), parser.deserialize("{\"a\":tru,\"b\":1}", Map.class));
        assertEquals(Map.of("a", "tru", "b", 1), parser.deserialize(reviewFixes20260906_P7_reader("{\"a\":tru,\"b\":1}"), Map.class));
        assertEquals(Map.of("a", "t", "b", 1), parser.deserialize("{\"a\":t,\"b\":1}", Map.class));
        assertEquals(Map.of("a", "t"), parser.deserialize(reviewFixes20260906_P7_reader("{\"a\":t}"), Map.class));

        for (final String json : new String[] { "[t]", "[n]", "[f]", "[tr]", "[fal]", "[tru]", "[nul]" }) {
            final String expected = json.substring(1, json.length() - 1);
            assertEquals(List.of(expected), parser.deserialize(json, List.class), json);
            assertEquals(List.of(expected), parser.deserialize(reviewFixes20260906_P7_reader(json), List.class), json);
        }

        // CSV-row dialect: a one-letter t/f/n field followed by a longer field
        assertArrayEquals(new String[] { "t", "12", "3" }, parser.parse("[t,12,3]", String[].class));
        assertArrayEquals(new String[] { "t", "f", "n" }, parser.parse("[t,f,n]", String[].class));
        assertEquals(Arrays.asList("a", "t", "b"), parser.deserialize(reviewFixes20260906_P7_reader("[a,t,b]"), List.class));

        // real literals and literal-prefixed words are unchanged; whitespace inside a literal is still rejected
        assertEquals(Arrays.asList(true, false, null), parser.deserialize("[true,false,null]", List.class));
        assertEquals(Arrays.asList(true, false, null), parser.deserialize(reviewFixes20260906_P7_reader("[true,false,null]"), List.class));
        assertEquals(Arrays.asList("truex", "falsehood", "nullx"), parser.deserialize("[truex,falsehood,nullx]", List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[fal se]", List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(reviewFixes20260906_P7_reader("[t rue]"), List.class));
    }

    // P7-04: an unquoted number read into a String target keeps its spelling ("1.50" used to become "1.5",
    // "007" -> "7", "+5" -> "5", "123L" -> "123" - only when the 18-digit fast path had accepted the token).
    @Test
    public void reviewFixes20260906_P7_04_unquotedNumberIntoStringKeepsSpelling() {
        final String json = "[1.50, 007, +5, 123L, 1.5f, 42, -0, 1e5, 00, 12345678901234567890]";
        final List<String> expected = Arrays.asList("1.50", "007", "+5", "123L", "1.5f", "42", "-0", "1e5", "00", "12345678901234567890");

        assertEquals(expected, parser.deserialize(json, Type.of("List<String>")));
        assertEquals(expected, parser.deserialize(reviewFixes20260906_P7_reader(json), Type.of("List<String>")));
        assertEquals(expected, parser.deserialize(json, new TypeReference<List<String>>() {
        }.type()));
        assertArrayEquals(expected.toArray(new String[0]), parser.deserialize(json, String[].class));
        assertArrayEquals(expected.toArray(new String[0]), parser.parse(json, String[].class));

        final Person person = parser.deserialize("{\"name\":1.50,\"age\":007}", Person.class);
        assertEquals("1.50", person.getName());
        assertEquals(7, person.getAge());
        assertEquals(Map.of("a", "+5", "b", "007"), parser.deserialize("{\"a\":+5,\"b\":007}", Type.of("Map<String, String>")));

        // Object / numeric targets are unchanged
        assertEquals(Map.of("a", 1.5, "b", 7), parser.deserialize("{\"a\":1.50,\"b\":007}", Map.class));
        assertEquals(Arrays.asList(1.5, 7, 5, 123L), parser.deserialize("[1.50, 007, +5, 123L]", List.class));
        assertEquals(List.of(7), parser.deserialize("[007]", Type.of("List<Integer>")));
        // G08-1: an unquoted decimal token converts to an integral target by truncating toward zero
        assertEquals(List.of(1), parser.deserialize("[1.50]", Type.of("List<Integer>")));
        assertEquals(List.of("1.50"), parser.deserialize("[\"1.50\"]", Type.of("List<String>")));
    }

    // ------------------------------------------------------------------ review fixes 2026-09-06, F4 round 1 (P1-*, R-P01, R-P03, P8-03, P7-05)

    @Data
    public static class ReviewFixes20260906_RawBean {
        private String name = "n";
        @com.landawn.abacus.annotation.JsonXmlField(isJsonRawValue = true)
        private Map<String, Object> meta = new LinkedHashMap<>();
    }

    @Data
    public static class ReviewFixes20260906_RawScalars {
        @com.landawn.abacus.annotation.JsonXmlField(isJsonRawValue = true)
        private String json = "{\"k\":\"v\\\"q\\n\"}";
        @com.landawn.abacus.annotation.JsonXmlField(isJsonRawValue = true)
        private Integer i = 42;
        @com.landawn.abacus.annotation.JsonXmlField(isJsonRawValue = true)
        private java.util.Date d = new java.util.Date(0);
        @com.landawn.abacus.annotation.JsonXmlField(isJsonRawValue = true)
        private List<Integer> nums = N.asList(1, 2);
    }

    @Data
    public static class ReviewFixes20260906_NullBean {
        private Integer num;
        private Long lng;
        private Double dbl;
        private java.math.BigDecimal bd;
        private String str;
        private Boolean flag;
        private java.util.Date date;
        private List<String> list;
        private char[] chars;
    }

    @Data
    public static class ReviewFixes20260906_AllIgnored {
        @com.landawn.abacus.annotation.JsonXmlField(ignore = true)
        private String x = "1";
    }

    @Data
    public static class ReviewFixes20260906_AllTransient {
        @com.landawn.abacus.annotation.Transient
        private String x = "1";
    }

    @Data
    public static class ReviewFixes20260906_Node {
        private String id;
        private ReviewFixes20260906_Node next;
        private List<ReviewFixes20260906_Node> kids;
    }

    @Data
    public static class ReviewFixes20260906_Prim {
        private int i = 7;
        private boolean b = true;
        private long l = 9L;
        private String s = "def";
        private Object o = "odef";
    }

    private static Map<Object, Object> reviewFixes20260906_lhm(final Object... kv) {
        final Map<Object, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    private static ReviewFixes20260906_Node reviewFixes20260906_node(final String id) {
        final ReviewFixes20260906_Node n = new ReviewFixes20260906_Node();
        n.setId(id);
        return n;
    }

    @Test
    public void reviewFixes20260906_P1_01_rawValuePropertySharesCircularReferenceTracking() {
        final ReviewFixes20260906_RawBean rb = new ReviewFixes20260906_RawBean();
        rb.getMeta().put("self", rb);
        rb.getMeta().put("q", "v\"q \u2028");

        // support on: the raw Map is written through the shared path and sees the bean already on the path
        assertEquals("{\"name\": \"n\", \"meta\": {\"self\": null, \"q\": \"v\\\"q \\u2028\"}}",
                parser.serialize(rb, JsonSerConfig.create().setCircularReferenceSupported(true)));

        // support off (default): the documented ParsingException, no StackOverflowError
        final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(rb));
        assertTrue(ex.getMessage().contains("circular"), ex.getMessage());

        // raw scalars keep the unquoted stringOf text; raw String verbatim (escapes untouched)
        final ReviewFixes20260906_RawScalars rs = new ReviewFixes20260906_RawScalars();
        assertEquals("{\"json\": {\"k\":\"v\\\"q\\n\"}, \"i\": 42, \"d\": 1970-01-01T00:00:00Z, \"nums\": [1, 2]}", parser.serialize(rs));

        // raw List under bracketRootValue=false used to be written as `"nums": 1, 2` (invalid JSON)
        assertEquals("\"json\": {\"k\":\"v\\\"q\\n\"}, \"i\": 42, \"d\": 1970-01-01T00:00:00Z, \"nums\": [1, 2]",
                parser.serialize(rs, JsonSerConfig.create().setBracketRootValue(false)));

        // raw Map with characters that need escaping, and the round trip
        final ReviewFixes20260906_RawBean rb2 = new ReviewFixes20260906_RawBean();
        rb2.getMeta().put("k", "v\"q\\");
        rb2.getMeta().put("n", N.asList(1, 2));
        final String json = parser.serialize(rb2);
        assertEquals("{\"name\": \"n\", \"meta\": {\"k\": \"v\\\"q\\\\\", \"n\": [1, 2]}}", json);
        final ReviewFixes20260906_RawBean back = parser.deserialize(json, ReviewFixes20260906_RawBean.class);
        assertEquals("v\"q\\", back.getMeta().get("k"));
        assertEquals(N.asList(1, 2), back.getMeta().get("n"));

        // an empty raw map / a null raw value are unchanged
        rb2.getMeta().clear();
        assertEquals("{\"name\": \"n\", \"meta\": {}}", parser.serialize(rb2));
        rb2.setMeta(null);
        assertEquals("{\"name\": \"n\"}", parser.serialize(rb2));
    }

    @Test
    public void reviewFixes20260906_P1_09_rawValuePropertyPrettyIndentation() {
        final ReviewFixes20260906_RawBean rb = new ReviewFixes20260906_RawBean();
        rb.getMeta().put("k", "v\"q");
        rb.getMeta().put("n", N.asList(1, 2));

        final String expected = "{\n  \"name\": \"n\",\n  \"meta\": {\n    \"k\": \"v\\\"q\",\n    \"n\": [\n      1,\n      2\n    ]\n  }\n}";
        assertEquals(expected, parser.serialize(rb, JsonSerConfig.create().setPrettyFormat(true).setIndentation("  ")));

        final String expectedWrapped = "{\n  \"ReviewFixes20260906_RawBean\": {\n    \"name\": \"n\",\n    \"meta\": {\n      \"k\": \"v\\\"q\",\n      \"n\": [\n        1,\n        2\n      ]\n    }\n  }\n}";
        assertEquals(expectedWrapped, parser.serialize(rb, JsonSerConfig.create().setPrettyFormat(true).setIndentation("  ").setWrapRootValue(true)));
    }

    @Test
    public void reviewFixes20260906_P1_03_writeNullFlagsApplyToBeanProperties() {
        final ReviewFixes20260906_NullBean bean = new ReviewFixes20260906_NullBean();
        final JsonSerConfig all = JsonSerConfig.create()
                .setExclusion(Exclusion.NONE)
                .setWriteNullStringAsEmpty(true)
                .setWriteNullNumberAsZero(true)
                .setWriteNullBooleanAsFalse(true);

        assertEquals("{\"num\": 0, \"lng\": 0, \"dbl\": 0.0, \"bd\": 0, \"str\": \"\", \"flag\": false, \"date\": null, \"list\": null, \"chars\": null}",
                parser.serialize(bean, all));

        assertEquals(
                "{\"num\": null, \"lng\": null, \"dbl\": null, \"bd\": null, \"str\": \"\", \"flag\": null, \"date\": null, \"list\": null, \"chars\": null}",
                parser.serialize(bean, JsonSerConfig.create().setExclusion(Exclusion.NONE).setWriteNullStringAsEmpty(true)));
        assertEquals("{\"num\": 0, \"lng\": 0, \"dbl\": 0.0, \"bd\": 0, \"str\": null, \"flag\": null, \"date\": null, \"list\": null, \"chars\": null}",
                parser.serialize(bean, JsonSerConfig.create().setExclusion(Exclusion.NONE).setWriteNullNumberAsZero(true)));
        assertEquals(
                "{\"num\": null, \"lng\": null, \"dbl\": null, \"bd\": null, \"str\": null, \"flag\": false, \"date\": null, \"list\": null, \"chars\": null}",
                parser.serialize(bean, JsonSerConfig.create().setExclusion(Exclusion.NONE).setWriteNullBooleanAsFalse(true)));

        // writeNullToEmpty keeps precedence for the types that have an empty form; the others still take the flags
        assertEquals("{\"num\": 0, \"lng\": 0, \"dbl\": 0.0, \"bd\": 0, \"str\": \"\", \"flag\": false, \"date\": null, \"list\": [], \"chars\": []}",
                parser.serialize(bean, all.copy().setWriteNullToEmpty(true)));
        assertEquals("{\"num\": null, \"lng\": null, \"dbl\": null, \"bd\": null, \"str\": \"\", \"flag\": null, \"date\": null, \"list\": [], \"chars\": []}",
                parser.serialize(bean, JsonSerConfig.create().setExclusion(Exclusion.NONE).setWriteNullToEmpty(true)));

        // default Exclusion.NULL: null properties are omitted regardless of the flags
        assertEquals("{}", parser.serialize(bean, JsonSerConfig.create().setWriteNullNumberAsZero(true).setWriteNullStringAsEmpty(true)));

        // unquoted names + pretty format
        final String pretty = parser.serialize(bean, all.copy().setQuotePropName(false).setPrettyFormat(true));
        assertTrue(pretty.contains("\n    num: 0,\n"), pretty);
        assertTrue(pretty.contains("\n    str: \"\",\n"), pretty);
        assertTrue(pretty.contains("\n    flag: false,\n"), pretty);

        // a non-null value is untouched by the flags
        bean.setNum(5);
        bean.setStr("s");
        bean.setFlag(true);
        assertTrue(parser.serialize(bean, all).startsWith("{\"num\": 5, \"lng\": 0, \"dbl\": 0.0, \"bd\": 0, \"str\": \"s\", \"flag\": true,"));

        // untyped Map values stay null (value type unknown) -- see testSerializeWithNullHandling
        assertEquals("{\"k\": null}", parser.serialize(N.asMap("k", null), all));
    }

    @Test
    public void reviewFixes20260906_P1_04_failOnEmptyBeanFalseCoversAllIgnoredBean() {
        final ReviewFixes20260906_AllIgnored bean = new ReviewFixes20260906_AllIgnored();

        final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(bean));
        assertTrue(ex.getMessage().startsWith("No serializable property is found in class"), ex.getMessage());
        Assertions.assertThrows(ParsingException.class, () -> parser.serialize(bean, JsonSerConfig.create().setFailOnEmptyBean(true)));

        final JsonSerConfig lenient = JsonSerConfig.create().setFailOnEmptyBean(false);
        assertEquals("{}", parser.serialize(bean, lenient));
        assertEquals("[{}]", parser.serialize(N.asList(bean), lenient));
        assertEquals("{\"a\": {}}", parser.serialize(N.asMap("a", bean), lenient));
        assertEquals("{}", parser.serialize(bean, lenient.copy().setPrettyFormat(true)));
        assertEquals("{\"ReviewFixes20260906_AllIgnored\": {}}", parser.serialize(bean, lenient.copy().setWrapRootValue(true)));

        // pins: an all-transient bean already printed {} under the DEFAULT config and must keep doing so
        assertEquals("{}", parser.serialize(new ReviewFixes20260906_AllTransient()));
        assertEquals("{\"x\": \"1\"}", parser.serialize(new ReviewFixes20260906_AllTransient(), JsonSerConfig.create().setSkipTransientField(false)));

        assertEquals("1", parser.deserialize("{}", ReviewFixes20260906_AllIgnored.class).getX());
    }

    @Test
    public void reviewFixes20260906_P1_05_mapKeysHonourSerializationConfig() {
        final Map<Long, Long> longs = N.asMap(123L, 456L);
        assertEquals("{\"123\": \"456\"}", parser.serialize(longs, JsonSerConfig.create().setWriteLongAsString(true)));
        assertEquals("{\"123\": \"456\"}", parser.serialize(longs, JsonSerConfig.create().setWriteLongAsString(true).setQuoteMapKey(false)));
        assertEquals("{123: 456}", parser.serialize(longs, JsonSerConfig.create().setQuoteMapKey(false)));
        assertEquals("{\"123\": 456}", parser.serialize(longs));
        assertEquals(N.asMap(123L, 456L), parser.deserialize(parser.serialize(longs, JsonSerConfig.create().setWriteLongAsString(true)),
                JsonDeserConfig.create().setMapKeyType(Long.class).setMapValueType(Long.class), Map.class));

        final Map<java.util.Date, java.util.Date> dates = N.asMap(new java.util.Date(0), new java.util.Date(0));
        // The DEFAULT DateTimeFormat.LONG is NOT applied to keys: bare epoch millis cannot be read back in a key
        // position ("Ambiguous numeric date/time text") and would collide distinct temporal keys of one instant
        // into a single duplicate JSON name. Keys keep the type's own text; only an explicit format is applied.
        assertEquals("{\"1970-01-01T00:00:00Z\": 0}", parser.serialize(dates));
        assertEquals("{\"1970-01-01T00:00:00Z\": 0}",
                parser.serialize(dates, JsonSerConfig.create().setDateTimeFormat(com.landawn.abacus.util.DateTimeFormat.LONG)));
        final String iso = parser.serialize(dates, JsonSerConfig.create().setDateTimeFormat(com.landawn.abacus.util.DateTimeFormat.ISO_8601_DATE_TIME));
        assertEquals("{\"1970-01-01T00:00:00Z\": \"1970-01-01T00:00:00Z\"}", iso);
        assertEquals("{\"1970-01-01T00:00:00.000Z\": \"1970-01-01T00:00:00.000Z\"}",
                parser.serialize(dates, JsonSerConfig.create().setDateTimeFormat(com.landawn.abacus.util.DateTimeFormat.ISO_8601_TIMESTAMP)));
        final Map<java.util.Date, java.util.Date> back = parser.deserialize(iso,
                JsonDeserConfig.create().setMapKeyType(java.util.Date.class).setMapValueType(java.util.Date.class), Map.class);
        assertEquals(new java.util.Date(0), back.keySet().iterator().next());

        // regression pin (R1): the default-config key round trip must keep working for every temporal key type.
        // Applying DateTimeFormat.LONG here turned each of these into the key "0", which cannot be parsed back.
        for (final Object key : new Object[] { new java.util.Date(0), new java.sql.Timestamp(0), java.time.Instant.ofEpochMilli(0),
                java.time.LocalDate.of(2020, 1, 2), com.landawn.abacus.util.Dates.createCalendar(0L) }) {
            final String json = parser.serialize(N.asMap(key, 1));
            assertFalse(json.startsWith("{\"0\":"), key.getClass() + " -> " + json);
            final Map<Object, Integer> restored = parser.deserialize(json,
                    JsonDeserConfig.create().setMapKeyType(key.getClass()).setMapValueType(Integer.class), Map.class);
            assertEquals(1, restored.size(), key.getClass().getName());
            assertEquals(key.getClass(), restored.keySet().iterator().next().getClass(), key.getClass().getName());
        }

        final Map<java.math.BigDecimal, java.math.BigDecimal> decimals = N.asMap(new java.math.BigDecimal("1E+3"), new java.math.BigDecimal("1E+3"));
        assertEquals("{1000: 1000}", parser.serialize(decimals, JsonSerConfig.create().setWriteBigDecimalAsPlain(true).setQuoteMapKey(false)));
        assertEquals("{\"1000\": 1000}", parser.serialize(decimals, JsonSerConfig.create().setWriteBigDecimalAsPlain(true)));
        assertEquals("{\"1E+3\": 1E+3}", parser.serialize(decimals));

        // unaffected key types keep the old rendering
        assertEquals("{\"a\": 1, \"2\": 2, \"true\": 3}",
                parser.serialize(reviewFixes20260906_lhm("a", 1, 2, 2, true, 3), JsonSerConfig.create().setWriteLongAsString(true)));
        assertEquals("{\"a\": 1, 2: 2, true: 3}",
                parser.serialize(reviewFixes20260906_lhm("a", 1, 2, 2, true, 3), JsonSerConfig.create().setQuoteMapKey(false)));
    }

    @Test
    public void reviewFixes20260906_R_P01_cyclicGraphThrowsParsingExceptionWithoutCircularSupport() {
        final ReviewFixes20260906_Node x = reviewFixes20260906_node("x");
        x.setNext(x);
        final List<Object> list = new ArrayList<>();
        list.add("a");
        list.add(list);
        final Map<String, Object> map = new HashMap<>();
        map.put("a", 1);
        map.put("c", map);
        final Object[] array = new Object[1];
        array[0] = array;
        final ReviewFixes20260906_Node k = reviewFixes20260906_node("k");
        k.setKids(N.asList(k)); // typed List<Node> property: the cycle runs through a nested serialize() call

        for (final Object cyclic : new Object[] { x, list, map, array, k }) {
            final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.serialize(cyclic), cyclic.getClass().getName());
            assertTrue(ex.getMessage().contains("circular"), ex.getMessage());
            Assertions.assertThrows(ParsingException.class, () -> parser.serialize(cyclic, JsonSerConfig.create().setCircularReferenceSupported(false)));
            Assertions.assertThrows(ParsingException.class, () -> parser.serialize(cyclic, JsonSerConfig.create(), new StringWriter()));
            Assertions.assertThrows(ParsingException.class, () -> parser.serialize(cyclic, JsonSerConfig.create(), new ByteArrayOutputStream()));
        }

        // support on: null placeholder, unchanged
        assertEquals("{\"id\": \"x\", \"next\": null}", parser.serialize(x, JsonSerConfig.create().setCircularReferenceSupported(true)));
        assertEquals("[\"a\", null]", parser.serialize(list, JsonSerConfig.create().setCircularReferenceSupported(true)));

        // the parser is fully usable after the rejection (thread-local depth released)
        assertEquals("[1, 2]", parser.serialize(N.asList(1, 2)));

        // a legitimately deep acyclic chain (250 < 256) still serializes, a DAG is not misreported
        final ReviewFixes20260906_Node head = reviewFixes20260906_node("0");
        ReviewFixes20260906_Node cur = head;
        for (int i = 1; i < 250; i++) {
            cur.setNext(reviewFixes20260906_node(String.valueOf(i)));
            cur = cur.getNext();
        }
        assertTrue(parser.serialize(head).contains("\"id\": \"249\""));
        final ReviewFixes20260906_Node shared = reviewFixes20260906_node("s");
        assertEquals("[{\"id\": \"s\"}, {\"id\": \"s\"}]", parser.serialize(N.asList(shared, shared)));

        // deeper than the bound is rejected even without a cycle (documented heuristic)
        for (int i = 250; i < 300; i++) {
            cur.setNext(reviewFixes20260906_node(String.valueOf(i)));
            cur = cur.getNext();
        }
        Assertions.assertThrows(ParsingException.class, () -> parser.serialize(head));
        assertEquals("[1, 2]", parser.serialize(N.asList(1, 2)));
    }

    @Test
    public void reviewFixes20260906_P1_07_unpairedSurrogatesEscapedOnEveryPath() throws IOException {
        final String[][] cases = { { "\uD800x", "[\"\\ud800x\"]" }, { "x\uDC00", "[\"x\\udc00\"]" }, { "a\uD83D", "[\"a\\ud83d\"]" },
                { "\uDE00b", "[\"\\ude00b\"]" }, { "\uD83D\uDE00", "[\"\uD83D\uDE00\"]" }, { "\uDE00\uD83D", "[\"\\ude00\\ud83d\"]" },
                { "\uD800", "[\"\\ud800\"]" }, { "", "[\"\"]" } };

        for (final String[] c : cases) {
            final List<String> value = N.asList(c[0]);
            final String viaString = parser.serialize(value);
            final StringWriter sw = new StringWriter();
            parser.serialize(value, sw);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            parser.serialize(value, bos);
            final String viaStream = new String(bos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);

            assertEquals(c[1], viaString, c[0]);
            assertEquals(c[1], sw.toString(), c[0]);
            assertEquals(c[1], viaStream, c[0]);
            assertEquals(c[0], parser.deserialize(viaString, List.class).get(0), c[0]);
        }

        assertEquals("[\"\\ud800\"]", parser.serialize(N.asList('\uD800')));
        assertEquals("{\"\\ud800\": 1}", parser.serialize(N.asMap("\uD800", 1)));
        assertEquals("{\"s\": \"a\\udc00\"}", parser.serialize(N.asMap("s", "a\uDC00")));
    }

    @Test
    public void reviewFixes20260906_P1_08_prettyFormatOfMapWithAllEntriesIgnored() {
        final JsonSerConfig config = JsonSerConfig.create().setPrettyFormat(true).setIgnoredPropNames(Map.class, N.asSet("a"));
        assertEquals("{}", parser.serialize(N.asMap("a", 1), config));
        assertEquals("{\n    \"b\": 2\n}", parser.serialize(reviewFixes20260906_lhm("a", 1, "b", 2), config));
        assertEquals("{}", parser.serialize(new HashMap<>(), config));
    }

    @Test
    public void reviewFixes20260906_R_P03_ignoreNullOrEmptyKeepsPrimitiveDefaults() {
        final JsonDeserConfig config = JsonDeserConfig.create().setIgnoreNullOrEmpty(true);

        ReviewFixes20260906_Prim p = parser.deserialize("{\"i\":null,\"b\":null,\"l\":null,\"s\":null,\"o\":null}", config, ReviewFixes20260906_Prim.class);
        assertEquals(7, p.getI());
        assertTrue(p.isB());
        assertEquals(9L, p.getL());
        assertEquals("def", p.getS());
        assertEquals("odef", p.getO());

        p = parser.deserialize("{\"i\":3,\"b\":false,\"l\":0}", config, ReviewFixes20260906_Prim.class);
        assertEquals(3, p.getI());
        assertTrue(!p.isB());
        assertEquals(0L, p.getL());

        p = parser.deserialize("{\"i\":null,\"b\":null}", config.copy().setReadNullToEmpty(true), ReviewFixes20260906_Prim.class);
        assertEquals(7, p.getI());
        assertTrue(p.isB());

        // without the option a JSON null still resets a primitive to its zero value (unchanged)
        p = parser.deserialize("{\"i\":null,\"b\":null}", ReviewFixes20260906_Prim.class);
        assertEquals(0, p.getI());
        assertTrue(!p.isB());
    }

    @Test
    public void reviewFixes20260906_P8_03_ignoreNullOrEmptyAppliesToUntypedValues() {
        final JsonDeserConfig config = JsonDeserConfig.create().setIgnoreNullOrEmpty(true);

        final Map<String, Object> map = parser.deserialize("{\"a\":null,\"b\":\"\",\"c\":[],\"d\":{},\"e\":\"v\",\"f\":0,\"g\":false,\"h\":\" \"}", config,
                Map.class);
        assertEquals(N.asMap("e", "v", "f", 0, "g", false, "h", " "), map);

        final List<Object> list = parser.deserialize("[null,\"\",[],{},\"v\",0,false,\" \"]", config, List.class);
        assertEquals(N.asList("v", 0, false, " "), list);

        // the check cascades through untyped containers
        assertEquals(N.asMap("z", 1), parser.deserialize("{\"x\":{\"y\":\"\"},\"z\":1}", config, Map.class));
        assertEquals(N.asList(N.asList(1)), parser.deserialize("[[],[1],[[]]]", config, List.class));

        // readNullToEmpty turns null into "" first, which is then skipped as well
        assertEquals(new HashMap<>(), parser.deserialize("{\"a\":null}", config.copy().setReadNullToEmpty(true), Map.class));

        // Object-typed bean property
        assertEquals("odef", parser.deserialize("{\"o\":\"\"}", config, ReviewFixes20260906_Prim.class).getO());
        assertEquals("odef", parser.deserialize("{\"o\":{}}", config, ReviewFixes20260906_Prim.class).getO());
        assertEquals("x", parser.deserialize("{\"o\":\"x\"}", config, ReviewFixes20260906_Prim.class).getO());

        // an empty KEY with a non-empty value is kept (existing contract), and the flag off keeps everything
        assertEquals(N.asMap("", "kept"), parser.deserialize("{\"\":\"kept\",\"blank\":\"\"}", config.copy().setMapValueType(String.class), Map.class));
        assertEquals(N.asMap("", "kept"), parser.deserialize("{\"\":\"kept\",\"blank\":\"\"}", config, Map.class));
        assertEquals(4, parser.deserialize("{\"a\":null,\"b\":\"\",\"c\":[],\"d\":{}}", Map.class).size());
    }

    @Test
    public void reviewFixes20260906_P7_05_leadingTextBeforeRootIsRejected() {
        for (final String src : new String[] { "abc[1]", "123[1]", "true[1]", "-[1]", "abc[{\"a\":1}]" }) {
            final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(src, List.class), src);
            assertTrue(ex.getMessage().startsWith("Unexpected content before the root JSON value: "), ex.getMessage());
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(src, Object.class), src);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(src, Object[].class), src);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(src), List.class), src);
            Assertions.assertThrows(ParsingException.class,
                    () -> parser.deserialize(new ByteArrayInputStream(src.getBytes(java.nio.charset.StandardCharsets.UTF_8)), List.class), src);
        }

        for (final String src : new String[] { "xyz{\"a\":1}", "123{\"a\":1}", "null{\"a\":1}" }) {
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(src, Map.class), src);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(src, Object.class), src);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(src), Map.class), src);
        }

        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("abc{\"id\":\"v\"}", ReviewFixes20260906_Node.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("abc{\"Person\":{\"a\":1}}", MapEntity.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("abc[{\"a\":1}]", Dataset.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.stream("abc[{\"a\":1}]", Type.of(Map.class)).toList());
        Assertions.assertThrows(ParsingException.class, () -> parser.stream(new StringReader("abc[{\"a\":1}]"), true, Type.of(Map.class)).toList());

        // exactly one leading BOM is tolerated on every overload
        assertEquals(N.asMap("a", 1), parser.deserialize("\uFEFF{\"a\":1}", Map.class));
        assertEquals(N.asList(1), parser.deserialize("\uFEFF[1]", List.class));
        assertEquals("v", parser.deserialize("\uFEFF{\"id\":\"v\"}", ReviewFixes20260906_Node.class).getId());
        assertEquals(N.asList(1), parser.deserialize(new StringReader("\uFEFF[1]"), List.class));
        assertEquals(N.asList(1), parser.deserialize(new ByteArrayInputStream("\uFEFF[1]".getBytes(java.nio.charset.StandardCharsets.UTF_8)), List.class));
        assertEquals(N.asList(N.asMap("a", 1)), parser.stream("\uFEFF[{\"a\":1}]", Type.of(Map.class)).toList());
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("\uFEFF\uFEFF[1]", List.class));

        // leading whitespace, the CSV-row dialect, the range overload and trailing junk are unchanged
        assertEquals(N.asList(1), parser.deserialize("  [1]", List.class));
        assertEquals(N.asMap("a", 1), parser.deserialize("\n\t{\"a\":1}", Map.class));
        assertArrayEquals(new String[] { "a", "b", "c" }, parser.deserialize("a,b,c", String[].class));
        assertEquals(N.asList("a", "b", "c"), parser.deserialize("a,b,c", List.class));
        assertEquals(N.asList(1), parser.deserialize("abc[1]xyz", 3, 6, List.class));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[1]abc", List.class));
    }

    // ------------------------------------------------------------------ self-review 2026-09-07 (R1): the
    // deserialization-side findings P2-01..P2-16 / T2-09 landed in r9486 with no test of their own.

    public enum ReviewFixes20260907_Color {
        RED, GREEN
    }

    @Data
    public static class ReviewFixes20260907_EnumMapBean {
        private java.util.EnumMap<ReviewFixes20260907_Color, Integer> m;
    }

    @Data
    public static class ReviewFixes20260907_NumberBean {
        private Number n;
    }

    @Data
    public static class ReviewFixes20260907_WrapperBean {
        private com.landawn.abacus.util.u.Optional<Integer> optI;
        private com.landawn.abacus.util.u.Optional<String> optS;
        private Integer[] iarr;
        private com.landawn.abacus.util.IntList il;
    }

    private static String reviewFixes20260907_ds(final Dataset d) {
        final StringBuilder sb = new StringBuilder(d.columnNames().toString()).append('|');
        for (int i = 0; i < d.size(); i++) {
            sb.append(Arrays.toString(d.getRow(i, Object[].class)));
        }
        return sb.append("|frozen=").append(d.isFrozen()).toString();
    }

    // P2-01: stream() dropped the LAST element when it was an unquoted scalar or null, and silently accepted a
    // missing comma before it. Every case is asserted against deserialize() of the same document.
    @Test
    public void reviewFixes20260907_R1_P2_01_streamKeepsTheTrailingScalarElement() {
        final Type<Object> mapType = Type.of(Map.class);
        final Type<List<Object>> listOfMap = Type.of("List<Map<String, Object>>");

        for (final String json : new String[] { "[null]", "[null,null]", "[{\"a\":1},null]", "[null,{\"a\":1}]", "[{\"a\":1},null,{\"b\":2}]", "[ null ]",
                "[{\"a\":1},null,null]", "[]", "[{\"a\":1}]" }) {
            final List<Object> expected = parser.deserialize(json, listOfMap);
            assertEquals(expected, parser.stream(json, mapType).toList(), json);
            assertEquals(expected, parser.stream(new StringReader(json), true, mapType).toList(), json);
        }

        final Type<Object> listType = Type.of(List.class);
        for (final String json : new String[] { "[[1],null]", "[null,[1]]", "[[1],null,[2]]" }) {
            final List<Object> expected = parser.deserialize(json, Type.of("List<List<Object>>"));
            assertEquals(expected, parser.stream(json, listType).toList(), json);
            assertEquals(expected, parser.stream(new StringReader(json), true, listType).toList(), json);
        }

        // a missing comma before the trailing scalar is now rejected, exactly as deserialize rejects it
        for (final String json : new String[] { "[{\"a\":1} null]", "[{\"a\":1} {\"b\":2}]" }) {
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, listOfMap), json);
            Assertions.assertThrows(ParsingException.class, () -> parser.stream(json, mapType).toList(), json);
            Assertions.assertThrows(ParsingException.class, () -> parser.stream(new StringReader(json), true, mapType).toList(), json);
        }

        // trailing junk after a trailing scalar element still fails, and only once
        Assertions.assertThrows(ParsingException.class, () -> parser.stream("[{\"a\":1},null] x", mapType).toList());
        assertEquals(N.asList((Object) null), parser.stream("[null]\n", mapType).toList());

        // an exhausted stream stays exhausted
        try (Stream<Object> stream = parser.stream("[{\"a\":1},null]", mapType)) {
            final java.util.Iterator<Object> iter = stream.iterator();
            assertEquals(N.asMap("a", 1), iter.next());
            assertNull(iter.next());
            assertFalse(iter.hasNext());
            assertFalse(iter.hasNext());
        }
    }

    // P2-09 / P2-10: stream() ignored ignoreNullOrEmpty / readNullToEmpty for scalar and null elements, and
    // leaked the caller's source when the element type was rejected.
    @Test
    public void reviewFixes20260907_R1_P2_09_10_streamHonoursNullOptionsAndClosesOnRejectedType() {
        final Type<Object> mapType = Type.of(Map.class);
        final Type<List<Object>> listOfMap = Type.of("List<Map<String, Object>>");
        final JsonDeserConfig skipEmpty = JsonDeserConfig.create().setIgnoreNullOrEmpty(true);
        final JsonDeserConfig nullToEmpty = JsonDeserConfig.create().setReadNullToEmpty(true);

        for (final String json : new String[] { "[{\"a\":1},null]", "[null,{\"a\":1}]", "[null,null]", "[{},{\"a\":1}]", "[{},{}]",
                "[{\"a\":1},null,{\"b\":2}]" }) {
            assertEquals(parser.deserialize(json, skipEmpty, listOfMap), parser.stream(json, skipEmpty, mapType).toList(), json);
            assertEquals(parser.deserialize(json, nullToEmpty, listOfMap), parser.stream(json, nullToEmpty, mapType).toList(), json);
        }
        assertEquals(N.asList(N.asMap("a", 1)), parser.stream("[{\"a\":1},null]", skipEmpty, mapType).toList());
        assertEquals(N.asList(N.asMap("a", 1), new HashMap<>()), parser.stream("[{\"a\":1},null]", nullToEmpty, mapType).toList());

        final AtomicInteger closed = new AtomicInteger();
        final Reader reader = new StringReader("[1]") {
            @Override
            public void close() {
                closed.incrementAndGet();
                super.close();
            }
        };
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.stream(reader, true, Type.of(Integer.class)));
        assertEquals(1, closed.get());

        final AtomicInteger notClosed = new AtomicInteger();
        final Reader kept = new StringReader("[1]") {
            @Override
            public void close() {
                notClosed.incrementAndGet();
                super.close();
            }
        };
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.stream(kept, false, Type.of(Integer.class)));
        assertEquals(0, notClosed.get());
    }

    // P2-02 / P2-11 (+ R1): a member after "columns" was rejected outright; a key type after its key set was
    // silently ignored, and so were columnNames/columnTypes after "columns".
    @Test
    public void reviewFixes20260907_R1_P2_02_11_datasetAndSheetMemberOrder() {
        assertEquals("[a]|[1][2]|frozen=true",
                reviewFixes20260907_ds(parser.deserialize("{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1,2]},\"isFrozen\":true}", Dataset.class)));
        assertEquals("[a]|[1]|frozen=false",
                reviewFixes20260907_ds(parser.deserialize("{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1]},\"isFrozen\":false}", Dataset.class)));
        assertEquals("v",
                parser.deserialize("{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1]},\"properties\":{\"k\":\"v\"}}", Dataset.class).getProperties().get("k"));
        assertEquals("[]||frozen=true", reviewFixes20260907_ds(parser.deserialize("{\"columnNames\":[],\"columns\":{},\"isFrozen\":true}", Dataset.class)));
        // nested in a map value, "columns" not last
        final Map<String, Dataset> nested = parser.deserialize("{\"d\":{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1]},\"isFrozen\":true}}",
                Type.of("Map<String, Dataset>"));
        assertEquals("[a]|[1]|frozen=true", reviewFixes20260907_ds(nested.get("d")));
        // an unknown member is still rejected, wherever it sits
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"columnNames\":[\"a\"],\"columns\":{\"a\":[1]},\"zz\":1}", Dataset.class));
        // R1: columnNames/columnTypes are consumed while "columns" is read, so a later one could only be
        // ignored -- it is rejected instead of silently producing the wrong column type
        assertEquals(Integer.valueOf(1),
                parser.deserialize("{\"columnNames\":[\"a\"],\"columnTypes\":[\"Integer\"],\"columns\":{\"a\":[\"1\"]}}", Dataset.class)
                        .getRow(0, Object[].class)[0]);
        final ParsingException lateTypes = Assertions.assertThrows(ParsingException.class,
                () -> parser.deserialize("{\"columnNames\":[\"a\"],\"columns\":{\"a\":[\"1\"]},\"columnTypes\":[\"Integer\"]}", Dataset.class));
        assertTrue(lateTypes.getMessage().contains("'columnTypes' must appear before 'columns'"), lateTypes.getMessage());
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"columns\":{\"a\":[1]},\"columnNames\":[\"a\"]}", Dataset.class));

        final Type<Sheet<String, String, Integer>> sheetType = Type.of("Sheet<String, String, Integer>");
        final Sheet<String, String, Integer> sheet = parser
                .deserialize("{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"c\"],\"columns\":{\"c\":[7]},\"isFrozen\":true}", sheetType);
        assertTrue(sheet.isFrozen());
        assertEquals(Integer.valueOf(7), sheet.get("r", "c"));
        final Sheet<Integer, String, Integer> intKeyed = parser.deserialize(
                "{\"rowKeyType\":\"Integer\",\"rowKeySet\":[1],\"columnKeySet\":[\"c\"],\"columns\":{\"c\":[7]}}", Type.of("Sheet<Integer, String, Integer>"));
        assertEquals(Integer.valueOf(1), intKeyed.rowKeySet().iterator().next());
        final ParsingException lateRowKeyType = Assertions.assertThrows(ParsingException.class,
                () -> parser.deserialize("{\"rowKeySet\":[1],\"rowKeyType\":\"Integer\",\"columnKeySet\":[\"c\"],\"columns\":{\"c\":[7]}}",
                        Type.of("Sheet<Integer, String, Integer>")));
        assertTrue(lateRowKeyType.getMessage().contains("'rowKeyType' must appear before 'rowKeySet'"), lateRowKeyType.getMessage());
        Assertions.assertThrows(ParsingException.class,
                () -> parser.deserialize("{\"rowKeySet\":[\"r\"],\"columnKeySet\":[1],\"columnKeyType\":\"Integer\",\"columns\":{\"c\":[7]}}",
                        Type.of("Sheet<String, Integer, Integer>")));

        // the parser's own output keeps round-tripping in both shapes
        final Dataset ds = Dataset.rows(N.asList("a", "b"), N.asList(N.asList(1, 2), N.asList(3, 4)));
        ds.freeze();
        assertEquals(reviewFixes20260907_ds(ds), reviewFixes20260907_ds(parser.deserialize(parser.serialize(ds), Dataset.class)));
        final Sheet<String, String, Integer> sh = Sheet.rows(N.asList("r1"), N.asList("c1"), new Integer[][] { { 7 } });
        sh.freeze();
        assertEquals(sh.toString(), parser.deserialize(parser.serialize(sh), sheetType).toString());
    }

    // P2-04 / P2-05 / P2-16: the row-oriented Dataset dialect.
    @Test
    public void reviewFixes20260907_R1_P2_04_05_16_rowOrientedDatasetDialect() {
        // P2-04: a key repeated inside one row overwrites the cell instead of appending a phantom row
        assertEquals("[a]|[2]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{\"a\":1,\"a\":2}]", Dataset.class)));
        assertEquals("[a]|[2][3]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{\"a\":1,\"a\":2},{\"a\":3}]", Dataset.class)));
        assertEquals("[a, b]|[3, 2]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{\"a\":1,\"b\":2,\"a\":3}]", Dataset.class)));
        assertEquals("[a, b]|[1, null][null, 2]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{\"a\":1},{\"b\":2}]", Dataset.class)));

        // P2-05: a missing value after the colon is rejected, as it is for Map/bean targets
        for (final String json : new String[] { "[{\"a\":}]", "[{\"a\":,\"b\":1}]", "[{\"a\":1},{\"b\":}]" }) {
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Dataset.class), json);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Type.of("List<Map<String, Object>>")), json);
        }

        // P2-16: rows must be comma-separated; the empty-element comma dialect stays tolerated
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[{\"a\":1}{\"a\":2}]", Dataset.class));
        assertEquals("[a]|[1]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{\"a\":1},]", Dataset.class)));
        assertEquals("[a]|[1]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[,{\"a\":1}]", Dataset.class)));
        assertEquals("[a]|[1][2]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{\"a\":1},,{\"a\":2}]", Dataset.class)));
        assertEquals("[a]|[null][1]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{},{\"a\":1}]", Dataset.class)));
        assertEquals("[]||frozen=false", reviewFixes20260907_ds(parser.deserialize("[]", Dataset.class)));
        Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("[{\"a\":1},2]", Dataset.class));
    }

    // P2-06: a structurally inconsistent document reached the caller as an IllegalArgumentException from the
    // Dataset/Sheet constructor; it is now a ParsingException like every other malformed input.
    @Test
    public void reviewFixes20260907_R1_P2_06_invalidDatasetOrSheetIsParsingException() {
        for (final String json : new String[] { "{\"columnNames\":[\"a\",\"b\"],\"columns\":{\"a\":[1,2],\"b\":[3]}}",
                "{\"columnNames\":[\"a\",\"a\"],\"columns\":{\"a\":[1]}}", "{\"columnNames\":[\"a\",\"b\"],\"columns\":{\"a\":[1]}}" }) {
            final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Dataset.class), json);
            assertTrue(ex.getMessage().startsWith("Invalid Dataset JSON: "), ex.getMessage());
            assertTrue(ex.getCause() instanceof IllegalArgumentException, String.valueOf(ex.getCause()));
        }

        final Type<Object> sheetType = Type.of("Sheet<String, String, Integer>");
        for (final String json : new String[] { "{\"rowKeySet\":[\"r1\",\"r2\"],\"columnKeySet\":[\"c\"],\"columns\":{\"c\":[7]}}",
                "{\"rowKeySet\":[\"r\"],\"columnKeySet\":[\"c\",\"d\"],\"columns\":{\"c\":[7]}}" }) {
            final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, sheetType), json);
            assertTrue(ex.getMessage().startsWith("Invalid Sheet JSON: "), ex.getMessage());
        }

        // the row-oriented reader back-fills, so its own columns are always consistent and the same guard
        // around the RowDataset constructor is defensive only
        assertEquals("[a, b]|[1, null][3, 4]|frozen=false", reviewFixes20260907_ds(parser.deserialize("[{\"a\":1},{\"a\":2,\"a\":3,\"b\":4}]", Dataset.class)));
    }

    // P2-07: an unquoted scalar root returned an EMPTY map and discarded the text; the empty/blank source must
    // still give the empty map, and the JSON null literal must fail the way it already does for every other
    // structured target.
    @Test
    public void reviewFixes20260907_R1_P2_07_scalarRootIsRejectedByReadMap() {
        assertEquals(new HashMap<>(), parser.deserialize("", Map.class));
        assertEquals(new HashMap<>(), parser.deserialize("   ", Map.class));
        assertEquals(new HashMap<>(), parser.deserialize("\n\t ", Map.class));
        assertEquals(new HashMap<>(), parser.deserialize(new StringReader("  "), Map.class));
        assertNull(parser.deserialize((String) null, Map.class));

        for (final String json : new String[] { "abc", "123", "true", "null" }) {
            final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Map.class), json);
            assertEquals("Can't parse: " + json, ex.getMessage());
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Type.of("Map<String, Integer>")), json);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(json), Map.class), json);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, java.util.Properties.class), json);

            // the same text is rejected by every other structured reader, with the same message
            for (final Class<?> target : new Class<?>[] { Person.class, Dataset.class, MapEntity.class, com.landawn.abacus.util.EntityId.class }) {
                final ParsingException sibling = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, target),
                        json + " -> " + target);
                assertEquals("Can't parse: " + json, sibling.getMessage(), json + " -> " + target);
            }
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, Type.of("Sheet<String, String, Integer>")), json);

            // a Collection root keeps its documented un-bracketed single-value dialect
            assertEquals(N.asList("null".equals(json) ? null : json), parser.deserialize(json, Type.of("List<String>")), json);
        }
    }

    // P2-03 (+ R1): the abstract java.lang.Number target could not be deserialized at all.
    @Test
    public void reviewFixes20260907_R1_P2_03_abstractNumberTarget() {
        assertEquals(Integer.valueOf(1), parser.deserialize("{\"n\":1}", ReviewFixes20260907_NumberBean.class).getN());
        assertEquals(Double.valueOf(2.5), parser.deserialize("{\"n\":2.5}", ReviewFixes20260907_NumberBean.class).getN());
        assertEquals(Integer.valueOf(3), parser.deserialize("{\"n\":\"3\"}", ReviewFixes20260907_NumberBean.class).getN());
        assertNull(parser.deserialize("{\"n\":null}", ReviewFixes20260907_NumberBean.class).getN());

        assertEquals(N.asList(1, 2.5, 3.0E10), parser.deserialize("[1, 2.5, 3e10]", Type.of("List<Number>")));
        assertEquals(N.asMap("a", 1, "b", 2.5), parser.deserialize("{\"a\":1,\"b\":2.5}", Type.of("Map<String, Number>")));
        assertArrayEquals(new Number[] { 1, 2.5 }, parser.deserialize("[1,2.5]", Number[].class));
        assertEquals(Integer.valueOf(42), parser.deserialize("42", Number.class));
        assertNull(parser.deserialize("null", Number.class));

        // the promotion is the untyped (Object) one, so both slots agree element by element
        final List<Object> asObject = parser.deserialize("[1, 2.5, 3e10, 1.5f, 9999999999999999999999]", Type.of("List<Object>"));
        final List<Number> asNumber = parser.deserialize("[1, 2.5, 3e10, 1.5f, 9999999999999999999999]", Type.of("List<Number>"));
        assertEquals(asObject, asNumber);

        // R1: a structural value in a Number slot must fail the way every other numeric slot fails
        // (NumberType.valueOf answers UnsupportedOperationException, which is not a parse error)
        for (final String json : new String[] { "{\"n\":[1]}", "{\"n\":{\"a\":1}}" }) {
            Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize(json, ReviewFixes20260907_NumberBean.class), json);
        }
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[[1]]", Type.of("List<Number>")));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[1]", Number.class));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("abc", Number.class));
    }

    // P2-12: a structural value in a scalar/wrapper slot polluted the generics (ClassCastException at the USE
    // site) or produced a raw JVM exception; it now fails exactly like the un-wrapped scalar slot.
    @Test
    public void reviewFixes20260907_R1_P2_12_structuralMismatchFailsLikeTheScalarSibling() {
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("{\"optI\":[1]}", ReviewFixes20260907_WrapperBean.class));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("{\"optI\":{\"a\":1}}", ReviewFixes20260907_WrapperBean.class));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("{\"iarr\":[[1]]}", ReviewFixes20260907_WrapperBean.class));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[[1]]", Integer[].class));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[[1]]", Type.of("List<Optional<Integer>>")));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[{\"b\":1}]", Type.of("List<Integer>")));

        // a String slot legitimately accepts the raw text of any JSON value
        assertEquals("[1]", parser.deserialize("{\"optS\":[1]}", ReviewFixes20260907_WrapperBean.class).getOptS().get());
        assertEquals(N.asList("{\"a\": 1}"), parser.deserialize("[{\"a\":1}]", Type.of("List<String>")));

        // positive controls: the wrapper / tuple / list / array branches still run BEFORE the guard
        assertEquals("[1, 2]", parser.deserialize("{\"il\":[1,2]}", ReviewFixes20260907_WrapperBean.class).getIl().toString());
        assertEquals(N.asList(com.landawn.abacus.util.IntList.of(1, 2)), parser.deserialize("[[1,2]]", Type.of("List<IntList>")));
        assertEquals("[(a, 1)]", parser.deserialize("[[\"a\",1]]", Type.of("List<Pair<String, Integer>>")).toString());
        assertEquals(com.landawn.abacus.util.u.Optional.of(N.asList(1)), parser.deserialize("[1]", Type.of("Optional<List<Integer>>")));
        assertEquals(com.landawn.abacus.util.u.Optional.of(N.asMap("a", 1)), parser.deserialize("{\"a\":1}", Type.of("Optional<Map<String, Integer>>")));
        assertEquals(N.asList(N.asList(1), N.asMap("a", 1)), parser.deserialize("[[1],{\"a\":1}]", Type.of("List<Object>")));
    }

    // P2-13: an EnumMap-typed target failed with "No default constructor found in class: java.util.EnumMap".
    @Test
    public void reviewFixes20260907_R1_P2_13_enumMapTarget() {
        final ReviewFixes20260907_EnumMapBean bean = new ReviewFixes20260907_EnumMapBean();
        final java.util.EnumMap<ReviewFixes20260907_Color, Integer> value = new java.util.EnumMap<>(ReviewFixes20260907_Color.class);
        value.put(ReviewFixes20260907_Color.RED, 1);
        bean.setM(value);

        final String json = parser.serialize(bean);
        assertEquals("{\"m\": {\"RED\": 1}}", json);
        final ReviewFixes20260907_EnumMapBean back = parser.deserialize(json, ReviewFixes20260907_EnumMapBean.class);
        assertEquals(java.util.EnumMap.class, back.getM().getClass());
        assertEquals(value, back.getM());
        assertEquals(ReviewFixes20260907_Color.RED, back.getM().keySet().iterator().next());

        final String enumMapType = "java.util.EnumMap<com.landawn.abacus.parser.JsonParserImplTest$ReviewFixes20260907_Color, Integer>";
        assertEquals(value, parser.deserialize("{\"RED\":1}", Type.of(enumMapType)));
        assertEquals(java.util.EnumMap.class, parser.deserialize("{\"RED\":1}", Type.of(enumMapType)).getClass());
        assertEquals(new java.util.EnumMap<>(ReviewFixes20260907_Color.class), parser.deserialize("{}", Type.of(enumMapType)));
        assertEquals(value, parser.deserialize("{\"RED\":1}",
                JsonDeserConfig.create().setMapKeyType(ReviewFixes20260907_Color.class).setMapValueType(Integer.class), java.util.EnumMap.class));

        // a raw EnumMap has no key type: reject it instead of handing back the documented HashMap of N.newMap
        final ParsingException ex = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize("{\"RED\":1}", java.util.EnumMap.class));
        assertTrue(ex.getMessage().startsWith("EnumMap requires an enum key type"), ex.getMessage());

        // the Map<Color,Integer> sibling is unchanged (a plain HashMap)
        assertEquals(HashMap.class,
                parser.deserialize("{\"RED\":1}", Type.of("Map<com.landawn.abacus.parser.JsonParserImplTest$ReviewFixes20260907_Color, Integer>")).getClass());
    }

    // P2-14 / T2-09: MapEntity and EntityId accepted a scalar after the entity name, and could not read back the
    // empty entity name their own factories produce.
    @Test
    public void reviewFixes20260907_R1_P2_14_T2_09_mapEntityAndEntityIdNames() {
        final com.landawn.abacus.util.EntityId id = com.landawn.abacus.util.EntityId.of("id", 1);
        final String idJson = parser.serialize(id);
        assertEquals("{\"\": {\"id\": 1}}", idJson);
        assertEquals(id, parser.deserialize(idJson, com.landawn.abacus.util.EntityId.class));
        assertEquals(com.landawn.abacus.util.EntityId.create(N.asMap("id", 1)),
                parser.deserialize(parser.serialize(com.landawn.abacus.util.EntityId.create(N.asMap("id", 1))), com.landawn.abacus.util.EntityId.class));
        assertEquals(com.landawn.abacus.util.EntityId.of("Account", "id", 1),
                parser.deserialize(parser.serialize(com.landawn.abacus.util.EntityId.of("Account", "id", 1)), com.landawn.abacus.util.EntityId.class));

        final MapEntity empty = new MapEntity("");
        empty.set("id", 1);
        final MapEntity emptyBack = parser.deserialize(parser.serialize(empty), MapEntity.class);
        assertEquals("", emptyBack.entityName());
        assertEquals(1, (int) emptyBack.get("id"));
        final MapEntity named = new MapEntity("Person");
        named.set("id", 1);
        assertEquals("Person", parser.deserialize(parser.serialize(named), MapEntity.class).entityName());

        // the entity value must be a JSON object, and there must be exactly one
        for (final String json : new String[] { "{\"E\":1}", "{\"E\"}", "{\"E\":\"x\"}", "{\"E\":{\"id\":1},\"F\":{\"id\":2}}" }) {
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, MapEntity.class), json);
            Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, com.landawn.abacus.util.EntityId.class), json);
        }
        assertNull(parser.deserialize("{}", MapEntity.class));
        assertNull(parser.deserialize("{}", com.landawn.abacus.util.EntityId.class));
    }

    @Test
    public void entityNamesRequireExactlyOneColonBeforeTheirPropertyMap() {
        for (final Class<?> target : new Class<?>[] { MapEntity.class, com.landawn.abacus.util.EntityId.class, Seid.class }) {
            final Type<?> listType = Type.of("List<" + target.getName() + ">");
            for (final String json : new String[] { "{\"E\" {\"id\":1}}", "{\"E\"::{\"id\":1}}", "{\"E\":{\"id\":1}:}", "{\"E\":ignored{\"id\":1}}",
                    "{ignored\"E\":{\"id\":1}}", "\"E\" {\"id\":1}", "E::{id:1}" }) {
                Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, target), target + ": " + json);
                Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(json), target), target + ": " + json);
                Assertions.assertThrows(ParsingException.class, () -> parser.parse(json, null, target), target + ": " + json);
                if (json.startsWith("{")) {
                    // Nested entities enter the readers after their opening brace has already been consumed.
                    final String nested = "[" + json + "]";
                    Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(nested, listType), target + ": " + nested);
                    Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader(nested), listType), target + ": " + nested);
                }
            }

            for (final String json : new String[] { "{\"E\":{\"id\":1}}", "{'E':{'id':1}}", "{E:{id:1}}", "E:{id:1}" }) {
                final Object expected = target == MapEntity.class ? new MapEntity("E").set("id", 1) : Seid.of("E").set("id", 1);
                assertEquals(expected, parser.deserialize(json, target), json);
                assertEquals(expected, parser.deserialize(new StringReader(json), target), json);
                assertEquals(expected, parser.parse(json, null, target), json);
                if (json.startsWith("{")) {
                    assertEquals(List.of(expected), parser.deserialize("[" + json + "]", listType), json);
                    assertEquals(List.of(expected), parser.deserialize(new StringReader("[" + json + "]"), listType), json);
                }
            }
        }
    }

    // P2-15: the Type overloads answered NullPointerException where the Class siblings answer IllegalArgumentException.
    @Test
    public void reviewFixes20260907_R1_P2_15_nullTargetTypeIsIllegalArgument() {
        final Type<Object> nullType = null;
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.deserialize("1", null, nullType));
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.deserialize("1", 0, 1, null, nullType));
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new StringReader("1"), null, nullType));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> parser.deserialize(new ByteArrayInputStream("1".getBytes(java.nio.charset.StandardCharsets.UTF_8)), null, nullType));
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new File("no-such-file.json"), null, nullType));
        // The range parameters precede targetType and are validated first when both are invalid.
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> parser.deserialize("1", 5, 1, null, nullType));
    }

    // G08-54: parse(String, JsonDeserConfig, Type) dereferenced a null targetType (NPE in emptyOrDefault for an
    // empty source, in targetType.serializationType() otherwise) while every deserialize sibling reports
    // IllegalArgumentException naming the argument. The failure must not depend on the source content either.
    @Test
    public void fixG08_F54_parseRejectsNullTargetType() {
        final Type<Object> nullType = null;

        for (final String source : new String[] { "{\"a\":1}", "", "   ", "[]", null }) {
            final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse(source, null, nullType),
                    String.valueOf(source));
            assertTrue(e.getMessage().contains("targetType"), e.getMessage());
        }

        // a real target type is still accepted
        assertEquals(1, (int) parser.parse("{\"a\":1}", null, Type.of(Map.class)).size());
    }

    // G08-53: writeLongAsString is honoured by AbstractLongType, AtomicLongType and MutableLongType, but the
    // map-key predicate only tested Type.isLong(). A MutableLong key (whose handler does report isNumber())
    // was therefore written unquoted through stringOf while the identical value was written as "1".
    @Test
    public void fixG08_F53_longCarryingMapKeysFollowWriteLongAsString() {
        final JsonSerConfig config = JsonSerConfig.create().setWriteLongAsString(true).setQuoteMapKey(false);

        assertEquals("{\"1\": \"1\"}",
                parser.serialize(N.asMap(com.landawn.abacus.util.MutableLong.of(1L), com.landawn.abacus.util.MutableLong.of(1L)), config));
        assertEquals("{\"1\": \"1\"}",
                parser.serialize(N.asMap(new java.util.concurrent.atomic.AtomicLong(1L), new java.util.concurrent.atomic.AtomicLong(1L)), config));
        assertEquals("{\"1\": \"1\"}", parser.serialize(N.asMap(1L, 1L), config));

        // with the flag off the key keeps its plain unquoted number text
        final JsonSerConfig plain = JsonSerConfig.create().setQuoteMapKey(false);
        assertEquals("{1: 1}", parser.serialize(N.asMap(com.landawn.abacus.util.MutableLong.of(1L), com.landawn.abacus.util.MutableLong.of(1L)), plain));
        assertEquals("{1: 1}", parser.serialize(N.asMap(1L, 1L), plain));
    }

    public static class RawJsonBean {
        @com.landawn.abacus.annotation.JsonXmlField(isJsonRawValue = true)
        private String raw;
        private int n;

        public String getRaw() {
            return raw;
        }

        public void setRaw(final String raw) {
            this.raw = raw;
        }

        public int getN() {
            return n;
        }

        public void setN(final int n) {
            this.n = n;
        }
    }

    // G08-105: both @JsonXmlField(isJsonRawValue=true) capture loops treated EOF as a normal terminator and
    // counted only their own delimiter, so a mismatched payload was stored as malformed "raw JSON" and an
    // unterminated one was reported with the unrelated "should be wrapped or unwrapped" message.
    @Test
    public void fixG08_F105_rawJsonCaptureRejectsUnterminatedAndMismatchedValues() {
        for (final String json : new String[] { "{\"raw\":{\"a\":1", "{\"raw\":[1,2", "{\"raw\":{", "{\"raw\":[" }) {
            final ParsingException e = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, RawJsonBean.class), json);
            assertTrue(e.getMessage().contains("Unterminated raw JSON value for property: raw"), e.getMessage());
        }

        for (final String json : new String[] { "{\"raw\":{\"a\":[1}}", "{\"raw\":[{\"b\":1]}" }) {
            final ParsingException e = Assertions.assertThrows(ParsingException.class, () -> parser.deserialize(json, RawJsonBean.class), json);
            assertTrue(e.getMessage().contains("Mismatched delimiters in raw JSON value for property: raw"), e.getMessage());
        }

        // well-formed payloads are unchanged, including delimiters inside quoted strings
        assertEquals("{\"a\": 1}", parser.deserialize("{\"raw\":{\"a\":1}}", RawJsonBean.class).getRaw());
        assertEquals("[1, {\"b\": 3}]", parser.deserialize("{\"raw\":[1,{\"b\":3}]}", RawJsonBean.class).getRaw());
        assertEquals("{}", parser.deserialize("{\"raw\":{}}", RawJsonBean.class).getRaw());
        assertEquals("[]", parser.deserialize("{\"raw\":[]}", RawJsonBean.class).getRaw());
        assertEquals("{\"a\": \"}\"}", parser.deserialize("{\"raw\":{\"a\":\"}\"},\"n\":7}", RawJsonBean.class).getRaw());
        assertEquals(7, parser.deserialize("{\"raw\":{\"a\":\"}\"},\"n\":7}", RawJsonBean.class).getN());
        assertEquals("[\"]\", \"[\"]", parser.deserialize("{\"raw\":[\"]\",\"[\"]}", RawJsonBean.class).getRaw());
    }

    @Data
    public static class ReviewFixes20260908_IntegralSlots {
        private byte b;
        private short s;
        private int i;
        private long l;
        private java.math.BigInteger bi;
        private Integer boxed;
    }

    /**
     * R02-8: an <b>unquoted</b> decimal token read into an integral slot truncates toward zero. This is the
     * contract of every release up to and including the one before the exact-token read was introduced -
     * {@code [1.50] -> List<Integer>} has yielded {@code [1]} since r9204 - and the intervening
     * {@code NumberFormatException} was collateral of that precision fix, not a decision. Pinned across the whole
     * family so a future precision change cannot silently flip one member: the quoted form is deliberately
     * <i>not</i> lenient, a range violation is still rejected, and the exact-token gains for
     * {@code BigDecimal}/{@code String} targets stay.
     */
    @Test
    public void reviewFixes20260908_R02_8_unquotedDecimalTruncatesIntoEveryIntegralSlot() {
        // every integral element type
        assertEquals(List.of(1), parser.deserialize("[1.50]", Type.of("List<Integer>")));
        assertEquals(List.of(1L), parser.deserialize("[1.50]", Type.of("List<Long>")));
        assertEquals(List.of((short) 1), parser.deserialize("[1.50]", Type.of("List<Short>")));
        assertEquals(List.of((byte) 1), parser.deserialize("[1.50]", Type.of("List<Byte>")));
        assertEquals(List.of(java.math.BigInteger.ONE), parser.deserialize("[1.50]", Type.of("List<BigInteger>")));

        // toward zero, not floor and not round-half-up
        assertEquals(List.of(1), parser.deserialize("[1.9]", Type.of("List<Integer>")));
        assertEquals(List.of(-1), parser.deserialize("[-1.9]", Type.of("List<Integer>")));

        // arrays and bean properties take the same route
        assertArrayEquals(new int[] { 1 }, parser.deserialize("[1.50]", int[].class));
        assertArrayEquals(new Integer[] { 1 }, parser.deserialize("[1.50]", Integer[].class));

        final ReviewFixes20260908_IntegralSlots bean = parser.deserialize("{\"b\":1.9,\"s\":2.9,\"i\":3.9,\"l\":4.9,\"bi\":5.9,\"boxed\":6.9}",
                ReviewFixes20260908_IntegralSlots.class);
        assertEquals((byte) 1, bean.getB());
        assertEquals((short) 2, bean.getS());
        assertEquals(3, bean.getI());
        assertEquals(4L, bean.getL());
        assertEquals(java.math.BigInteger.valueOf(5), bean.getBi());
        assertEquals(Integer.valueOf(6), bean.getBoxed());

        // a QUOTED decimal is handed to the target type's own valueOf(String) and is still rejected
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[\"1.50\"]", Type.of("List<Integer>")));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[\"1.50\"]", int[].class));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("{\"i\":\"1.50\"}", ReviewFixes20260908_IntegralSlots.class));

        // a value the target cannot hold is still rejected - truncation is not saturation
        Assertions.assertThrows(ArithmeticException.class, () -> parser.deserialize("[3000000000.5]", Type.of("List<Integer>")));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[1e100]", Type.of("List<Integer>")));

        // the exact-token read that introduced the intervening error is still in force where it matters
        assertEquals(List.of(new java.math.BigDecimal("1.50")), parser.deserialize("[1.50]", Type.of("List<BigDecimal>")));
        assertEquals(List.of("1.50"), parser.deserialize("[1.50]", Type.of("List<String>")));
        assertEquals(List.of(1.5d), parser.deserialize("[1.50]", Type.of("List<Double>")));

        // ... and the Indexed/Timed metadata slot keeps integer notation on purpose
        assertEquals(com.landawn.abacus.util.Pair.of(1L, "a"), parser.deserialize("[1.5, \"a\"]", Type.of("Pair<Long, String>")));
        Assertions.assertThrows(NumberFormatException.class, () -> parser.deserialize("[1.5, \"a\"]", Type.of("Indexed<String>")));
    }

}
