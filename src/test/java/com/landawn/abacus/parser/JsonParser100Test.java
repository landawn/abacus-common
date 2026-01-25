package com.landawn.abacus.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class JsonParser100Test extends TestBase {

    private JsonParser parser;

    @BeforeEach
    public void setUp() {
        parser = ParserFactory.createJsonParser();
    }

    @Test
    public void testReadStringWithClass() {
        String json = "\"test\"";
        String result = parser.readString(json, String.class);
        Assertions.assertEquals("\"test\"", result);

        String nullResult = parser.readString(null, String.class);
        Assertions.assertNull(nullResult);
    }

    @Test
    public void testReadStringWithConfigAndClass() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Map<String, Object> result = parser.readString(json, null, Map.class);
        Assertions.assertEquals("John", result.get("name"));
        Assertions.assertEquals(30, result.get("age"));
    }

    @Test
    public void testReadStringToArray() {
        String json = "[1,2,3]";
        Integer[] array = new Integer[3];
        parser.readString(json, array);
        Assertions.assertEquals(1, array[0]);
        Assertions.assertEquals(2, array[1]);
        Assertions.assertEquals(3, array[2]);
    }

    @Test
    public void testReadStringToArrayWithConfig() {
        String json = "[\"a\",\"b\",\"c\"]";
        String[] array = new String[3];
        parser.readString(json, null, array);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    @Test
    public void testReadStringToCollection() {
        String json = "[1,2,3]";
        List<Integer> list = new ArrayList<>();
        parser.readString(json, list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
    }

    @Test
    public void testReadStringToCollectionWithConfig() {
        String json = "[\"x\",\"y\",\"z\"]";
        List<String> list = new ArrayList<>();
        parser.readString(json, null, list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("x", list.get(0));
        Assertions.assertEquals("y", list.get(1));
        Assertions.assertEquals("z", list.get(2));
    }

    @Test
    public void testReadStringToMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Map<String, String> map = new HashMap<>();
        parser.readString(json, map);
        Assertions.assertEquals("value1", map.get("key1"));
        Assertions.assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testReadStringToMapWithConfig() {
        String json = "{\"a\":1,\"b\":2}";
        Map<String, Integer> map = new HashMap<>();
        parser.readString(json, null, map);
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
    }

    @Test
    public void testDeserializeSubstring() {
        String json = "prefix{\"value\":123}suffix";
        Map<String, Object> result = parser.deserialize(json, 6, 19, Map.class);
        Assertions.assertEquals(123, result.get("value"));
    }

    @Test
    public void testDeserializeSubstringWithConfig() {
        String json = "xxx[1,2,3]yyy";
        List<Integer> result = parser.deserialize(json, 3, 10, null, List.class);
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(1, result.get(0));
        Assertions.assertEquals(2, result.get(1));
        Assertions.assertEquals(3, result.get(2));
    }

    @Test
    public void testStreamFromString() {
        String json = "[{\"id\":1},{\"id\":2},{\"id\":3}]";
        try (Stream<Map> stream = parser.stream(json, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0).get("id"));
            Assertions.assertEquals(2, list.get(1).get("id"));
            Assertions.assertEquals(3, list.get(2).get("id"));
        }
    }

    @Test
    @Disabled("This test is disabled due to the element type is not supported type: array/collection/map/bean.")
    public void testStreamFromStringWithConfig() {
        String json = "[\"a\",\"b\",\"c\"]";
        try (Stream<String> stream = parser.stream(json, null, Type.of(String.class))) {
            List<String> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals("a", list.get(0));
            Assertions.assertEquals("b", list.get(1));
            Assertions.assertEquals("c", list.get(2));
        }
    }

    @Test
    @Disabled("This test is disabled due to the element type is not supported type: array/collection/map/bean.")
    public void testStreamFromInputStream() {
        String json = "[1,2,3]";
        InputStream is = new ByteArrayInputStream(json.getBytes());
        try (Stream<Integer> stream = parser.stream(is, true, Type.of(Integer.class))) {
            List<Integer> list = stream.toList();
            Assertions.assertEquals(3, list.size());
            Assertions.assertEquals(1, list.get(0));
            Assertions.assertEquals(2, list.get(1));
            Assertions.assertEquals(3, list.get(2));
        }
    }

    @Test
    public void testStreamFromInputStreamWithConfig() {
        String json = "[{\"x\":1},{\"x\":2}]";
        InputStream is = new ByteArrayInputStream(json.getBytes());
        try (Stream<Map> stream = parser.stream(is, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(1, list.get(0).get("x"));
            Assertions.assertEquals(2, list.get(1).get("x"));
        }
    }

    @Test
    @Disabled("This test is disabled due to the element type is not supported type: array/collection/map/bean.")
    public void testStreamFromReader() {
        String json = "[\"hello\",\"world\"]";
        Reader reader = new StringReader(json);
        try (Stream<String> stream = parser.stream(reader, true, Type.of(String.class))) {
            List<String> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals("hello", list.get(0));
            Assertions.assertEquals("world", list.get(1));
        }
    }

    @Test
    public void testStreamFromReaderWithConfig() {
        String json = "[{\"n\":100},{\"n\":200}]";
        Reader reader = new StringReader(json);
        try (Stream<Map> stream = parser.stream(reader, true, null, Type.of(Map.class))) {
            List<Map> list = stream.toList();
            Assertions.assertEquals(2, list.size());
            Assertions.assertEquals(100, list.get(0).get("n"));
            Assertions.assertEquals(200, list.get(1).get("n"));
        }
    }
}
