package com.landawn.abacus.parser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class JSONParser2025Test extends TestBase {

    private JSONParser parser;
    private File tempFile;

    @BeforeEach
    public void setUp() {
        parser = ParserFactory.createJSONParser();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void test_readString_withClass() {
        String json = "\"hello\"";
        String result = parser.readString(json, String.class);
        Assertions.assertEquals("\"hello\"", result);

        String numJson = "42";
        Integer numResult = parser.readString(numJson, Integer.class);
        Assertions.assertEquals(42, numResult);

        String mapJson = "{\"name\":\"John\",\"age\":30}";
        Map<?, ?> mapResult = parser.readString(mapJson, Map.class);
        Assertions.assertEquals("John", mapResult.get("name"));
        Assertions.assertEquals(30, mapResult.get("age"));

        String listJson = "[1,2,3]";
        List<?> listResult = parser.readString(listJson, List.class);
        Assertions.assertEquals(3, listResult.size());
        Assertions.assertEquals(1, listResult.get(0));

        String nullResult = parser.readString(null, String.class);
        Assertions.assertNull(nullResult);
    }

    @Test
    public void test_readString_withConfigAndClass() {
        String json = "{\"name\":\"Jane\",\"age\":25}";
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        Map<?, ?> result = parser.readString(json, config, Map.class);
        Assertions.assertEquals("Jane", result.get("name"));
        Assertions.assertEquals(25, result.get("age"));

        Map<?, ?> result2 = parser.readString(json, null, Map.class);
        Assertions.assertEquals("Jane", result2.get("name"));

        String boolJson = "true";
        Boolean boolResult = parser.readString(boolJson, config, Boolean.class);
        Assertions.assertEquals(true, boolResult);
    }

    @Test
    public void test_readString_toArray() {
        String json = "[1,2,3,4,5]";
        Integer[] array = new Integer[5];
        parser.readString(json, array);

        Assertions.assertEquals(1, array[0]);
        Assertions.assertEquals(2, array[1]);
        Assertions.assertEquals(3, array[2]);
        Assertions.assertEquals(4, array[3]);
        Assertions.assertEquals(5, array[4]);

        String strJson = "[\"a\",\"b\",\"c\"]";
        String[] strArray = new String[3];
        parser.readString(strJson, strArray);
        Assertions.assertEquals("a", strArray[0]);
        Assertions.assertEquals("b", strArray[1]);
        Assertions.assertEquals("c", strArray[2]);
    }

    @Test
    public void test_readString_toArrayWithConfig() {
        String json = "[10,20,30]";
        Integer[] array = new Integer[3];
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        parser.readString(json, config, array);
        Assertions.assertEquals(10, array[0]);
        Assertions.assertEquals(20, array[1]);
        Assertions.assertEquals(30, array[2]);

        Integer[] array2 = new Integer[3];
        parser.readString(json, null, array2);
        Assertions.assertEquals(10, array2[0]);
        Assertions.assertEquals(20, array2[1]);
        Assertions.assertEquals(30, array2[2]);
    }

    @Test
    public void test_readString_toCollection() {
        String json = "[1,2,3,4]";
        List<Integer> list = new ArrayList<>();
        parser.readString(json, list);

        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
        Assertions.assertEquals(4, list.get(3));

        List<Integer> list2 = new ArrayList<>();
        list2.add(999);
        parser.readString(json, list2);
        Assertions.assertEquals(5, list2.size());
        Assertions.assertEquals(999, list2.get(0));
    }

    @Test
    public void test_readString_toCollectionWithConfig() {
        String json = "[\"x\",\"y\",\"z\"]";
        List<String> list = new ArrayList<>();
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        parser.readString(json, config, list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("x", list.get(0));
        Assertions.assertEquals("y", list.get(1));
        Assertions.assertEquals("z", list.get(2));

        List<String> list2 = new ArrayList<>();
        parser.readString(json, null, list2);
        Assertions.assertEquals(3, list2.size());
    }

    @Test
    public void test_readString_toMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}";
        Map<String, String> map = new HashMap<>();
        parser.readString(json, map);

        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("value1", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
        Assertions.assertEquals("value3", map.get("key3"));

        Map<String, String> map2 = new HashMap<>();
        map2.put("old", "data");
        parser.readString(json, map2);
        Assertions.assertEquals(4, map2.size());
        Assertions.assertEquals("data", map2.get("old"));
    }

    @Test
    public void test_readString_toMapWithConfig() {
        String json = "{\"a\":1,\"b\":2,\"c\":3}";
        Map<String, Integer> map = new LinkedHashMap<>();
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        parser.readString(json, config, map);
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
        Assertions.assertEquals(3, map.get("c"));

        Map<String, Integer> map2 = new HashMap<>();
        parser.readString(json, null, map2);
        Assertions.assertEquals(3, map2.size());
    }

    @Test
    public void test_deserialize_substring() {
        String json = "prefix{\"name\":\"Bob\",\"age\":35}suffix";
        int fromIndex = 6;
        int toIndex = json.length() - 6;

        Map<?, ?> result = parser.deserialize(json, fromIndex, toIndex, Map.class);
        Assertions.assertEquals("Bob", result.get("name"));
        Assertions.assertEquals(35, result.get("age"));

        String arrayJson = "xxx[1,2,3,4,5]yyy";
        List<?> listResult = parser.deserialize(arrayJson, 3, 14, List.class);
        Assertions.assertEquals(5, listResult.size());
        Assertions.assertEquals(1, listResult.get(0));
        Assertions.assertEquals(5, listResult.get(4));
    }

    @Test
    public void test_deserialize_substringWithConfig() {
        String json = "start{\"x\":100,\"y\":200}end";
        int fromIndex = 5;
        int toIndex = 23;
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        Map<?, ?> result = parser.deserialize(json, fromIndex, toIndex, config, Map.class);
        Assertions.assertEquals(100, result.get("x"));
        Assertions.assertEquals(200, result.get("y"));

        Map<?, ?> result2 = parser.deserialize(json, fromIndex, toIndex, null, Map.class);
        Assertions.assertEquals(100, result2.get("x"));
    }

    @Test
    public void test_stream_fromString() {
        String json = "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"},{\"id\":3,\"name\":\"Charlie\"}]";

        try (Stream<Map> stream = parser.stream(json, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0).get("id"));
            Assertions.assertEquals("Alice", list.get(0).get("name"));
            Assertions.assertEquals(2, list.get(1).get("id"));
            Assertions.assertEquals("Bob", list.get(1).get("name"));
            Assertions.assertEquals(3, list.get(2).get("id"));
            Assertions.assertEquals("Charlie", list.get(2).get("name"));
        }

        try (Stream<Map> stream = parser.stream(json, Type.of(Map.class))) {
            long count = stream.filter(m -> (Integer) m.get("id") > 1).count();
            Assertions.assertEquals(2, count);
        }
    }

    @Test
    public void test_stream_fromStringWithConfig() {
        String json = "[{\"value\":10},{\"value\":20},{\"value\":30}]";
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        try (Stream<Map> stream = parser.stream(json, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(10, list.get(0).get("value"));
            Assertions.assertEquals(20, list.get(1).get("value"));
            Assertions.assertEquals(30, list.get(2).get("value"));
        }

        try (Stream<Map> stream = parser.stream(json, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
        }
    }

    @Test
    public void test_stream_fromFile() throws Exception {
        String json = "[{\"num\":1},{\"num\":2},{\"num\":3}]";
        tempFile = File.createTempFile("test", ".json");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(json);
        }

        try (Stream<Map> stream = parser.stream(tempFile, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0).get("num"));
            Assertions.assertEquals(2, list.get(1).get("num"));
            Assertions.assertEquals(3, list.get(2).get("num"));
        }
    }

    @Test
    public void test_stream_fromFileWithConfig() throws Exception {
        String json = "[{\"data\":\"A\"},{\"data\":\"B\"}]";
        tempFile = File.createTempFile("test", ".json");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(json);
        }

        JSONDeserializationConfig config = new JSONDeserializationConfig();
        try (Stream<Map> stream = parser.stream(tempFile, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals("A", list.get(0).get("data"));
            Assertions.assertEquals("B", list.get(1).get("data"));
        }

        try (Stream<Map> stream = parser.stream(tempFile, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
        }
    }

    @Test
    public void test_stream_fromInputStream() {
        String json = "[{\"id\":100},{\"id\":200}]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        try (Stream<Map> stream = parser.stream(inputStream, true, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(100, list.get(0).get("id"));
            Assertions.assertEquals(200, list.get(1).get("id"));
        }

        InputStream inputStream2 = new ByteArrayInputStream(json.getBytes());
        try (Stream<Map> stream = parser.stream(inputStream2, false, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
        }
    }

    @Test
    public void test_stream_fromInputStreamWithConfig() {
        String json = "[{\"val\":\"X\"},{\"val\":\"Y\"},{\"val\":\"Z\"}]";
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        try (Stream<Map> stream = parser.stream(inputStream, true, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals("X", list.get(0).get("val"));
            Assertions.assertEquals("Y", list.get(1).get("val"));
            Assertions.assertEquals("Z", list.get(2).get("val"));
        }

        InputStream inputStream2 = new ByteArrayInputStream(json.getBytes());
        try (Stream<Map> stream = parser.stream(inputStream2, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
        }
    }

    @Test
    public void test_stream_fromReader() {
        String json = "[{\"code\":1001},{\"code\":1002}]";
        Reader reader = new StringReader(json);

        try (Stream<Map> stream = parser.stream(reader, true, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(1001, list.get(0).get("code"));
            Assertions.assertEquals(1002, list.get(1).get("code"));
        }

        Reader reader2 = new StringReader(json);
        try (Stream<Map> stream = parser.stream(reader2, false, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
        }
    }

    @Test
    public void test_stream_fromReaderWithConfig() {
        String json = "[{\"item\":\"apple\"},{\"item\":\"banana\"},{\"item\":\"cherry\"}]";
        Reader reader = new StringReader(json);
        JSONDeserializationConfig config = new JSONDeserializationConfig();

        try (Stream<Map> stream = parser.stream(reader, true, config, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals("apple", list.get(0).get("item"));
            Assertions.assertEquals("banana", list.get(1).get("item"));
            Assertions.assertEquals("cherry", list.get(2).get("item"));
        }

        Reader reader2 = new StringReader(json);
        try (Stream<Map> stream = parser.stream(reader2, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
        }
    }

    @Test
    public void test_edgeCases_emptyArray() {
        String json = "[]";
        List<Integer> list = new ArrayList<>();
        parser.readString(json, list);
        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void test_edgeCases_emptyMap() {
        String json = "{}";
        Map<String, Object> map = new HashMap<>();
        parser.readString(json, map);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void test_edgeCases_nestedStructures() {
        String json = "{\"outer\":{\"inner\":{\"value\":42}}}";
        Map<?, ?> result = parser.readString(json, Map.class);
        Map<?, ?> outer = (Map<?, ?>) result.get("outer");
        Map<?, ?> inner = (Map<?, ?>) outer.get("inner");
        Assertions.assertEquals(42, inner.get("value"));
    }

    @Test
    public void test_edgeCases_arrayOfArrays() {
        String json = "[[1,2],[3,4],[5,6]]";
        List<?> result = parser.readString(json, List.class);
        Assertions.assertEquals(3, result.size());
        List<?> first = (List<?>) result.get(0);
        Assertions.assertEquals(2, first.size());
        Assertions.assertEquals(1, first.get(0));
        Assertions.assertEquals(2, first.get(1));
    }
}
