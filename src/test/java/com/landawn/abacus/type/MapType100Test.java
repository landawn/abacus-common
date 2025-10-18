package com.landawn.abacus.type;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParseException;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;

import lombok.Data;

@Tag("new-test")
public class MapType100Test extends TestBase {

    private MapType<String, Integer, Map<String, Integer>> mapType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        mapType = (MapType<String, Integer, Map<String, Integer>>) createType("Map<String, Integer>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = mapType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Map"));
    }

    @Test
    public void testClazz() {
        Class<Map<String, Integer>> clazz = mapType.clazz();
        Assertions.assertNotNull(clazz);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = mapType.getParameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);
    }

    @Test
    public void testIsMap() {
        boolean isMap = mapType.isMap();
        Assertions.assertTrue(isMap);
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = mapType.isGenericType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testIsSerializable() {
        boolean isSerializable = mapType.isSerializable();
        Assertions.assertFalse(isSerializable);
    }

    @Test
    public void testGetSerializationType() {
        Type.SerializationType serType = mapType.getSerializationType();
        assertEquals(Type.SerializationType.MAP, serType);
    }

    @Test
    public void testStringOfNull() {
        String result = mapType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfEmptyMap() {
        Map<String, Integer> emptyMap = new HashMap<>();
        String result = mapType.stringOf(emptyMap);
        assertEquals("{}", result);
    }

    @Test
    public void testStringOfNonEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        String result = mapType.stringOf(map);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key1"));
        Assertions.assertTrue(result.contains("key2"));
    }

    @Test
    public void testValueOfNull() {
        Map<String, Integer> result = mapType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        Map<String, Integer> result = mapType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyObject() {
        Map<String, Integer> result = mapType.valueOf("{}");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOfValidJson() {
        Map<String, Integer> result = mapType.valueOf("{\"key1\":1,\"key2\":2}");
        Assertions.assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, result.get("key1"));
        assertEquals(2, result.get("key2"));
        assertThrows(ParseException.class, () -> mapType.valueOf("{invalid json}"));

        final String json = "{ \"test2\":{  }}";

        N.println(N.fromJson(json, TestModel.class));

        String str = "[, [\"123\", \"345\"]]";

        String[][] twoArray2 = N.fromJson(str, String[][].class);

        assertTrue(N.deepEquals(new String[][] { null, { "123", "345" } }, twoArray2));

    }

    @Data
    public static class TestModel {

        public boolean a = true;
        public Test2 test2 = new Test2();

        @Data
        static class Test2 {
            public boolean b = true;
        }
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mapType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 123);
        mapType.appendTo(writer, map);
        String result = writer.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }

    @Test
    public void testAppendToWithStringBuilder() throws IOException {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 123);
        mapType.appendTo(sb, map);
        String result = sb.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }
}
