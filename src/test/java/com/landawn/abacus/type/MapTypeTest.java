package com.landawn.abacus.type;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeReference;

import lombok.Data;

public class MapTypeTest extends TestBase {

    private MapType<String, Integer, Map<String, Integer>> mapType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        mapType = (MapType<String, Integer, Map<String, Integer>>) createType("Map<String, Integer>");
        characterWriter = createCharacterWriter();
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
    public void testDeclaringName() {
        String declaringName = mapType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Map"));
    }

    @Test
    public void testClazz() {
        Class<Map<String, Integer>> clazz = mapType.javaType();
        Assertions.assertNotNull(clazz);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = mapType.parameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(2, paramTypes.size());
    }

    @Test
    public void testSpringMultiValueMapUsesDeclaredValueParameter() {
        final Type<LinkedMultiValueMap<String, Integer>> type = createType(new TypeReference<LinkedMultiValueMap<String, Integer>>() {
        });

        assertEquals(String.class, type.parameterTypes().get(0).javaType());
        assertEquals(Integer.class, type.parameterTypes().get(1).javaType());

        final LinkedMultiValueMap<String, Integer> result = type.valueOf("{\"a\":[1,2]}");
        assertEquals(List.of(1, 2), result.get("a"));
    }

    @Test
    public void testSpringMultiValueMapInterfaceUsesConcreteImplementation() {
        final Type<MultiValueMap<String, Integer>> type = createType(new TypeReference<MultiValueMap<String, Integer>>() {
        });

        final MultiValueMap<String, Integer> populated = type.valueOf("{\"a\":[1,2]}");
        final MultiValueMap<String, Integer> empty = type.valueOf("{}");

        assertEquals(LinkedMultiValueMap.class, populated.getClass());
        assertEquals(List.of(1, 2), populated.get("a"));
        assertEquals(LinkedMultiValueMap.class, empty.getClass());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testIsMap() {
        boolean isMap = mapType.isMap();
        Assertions.assertTrue(isMap);
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = mapType.isParameterizedType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testIsSerializable() {
        boolean isSerializable = mapType.isSerializable();
        Assertions.assertFalse(isSerializable);
    }

    @Test
    public void testGetSerializationType() {
        Type.SerializationType serType = mapType.serializationType();
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
        assertThrows(ParsingException.class, () -> mapType.valueOf("{invalid json}"));

        final String json = "{ \"test2\":{  }}";

        N.println(N.fromJson(json, TestModel.class));

        String str = "[, [\"123\", \"345\"]]";

        String[][] twoArray2 = N.fromJson(str, String[][].class);

        assertTrue(N.deepEquals(new String[][] { null, { "123", "345" } }, twoArray2));

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

    @Test
    public void testAppendTo_unquotedToStringForm() throws IOException {
        Map<String, Integer> map = new HashMap<>();
        map.put("k", 1);
        StringBuilder sb = new StringBuilder();
        mapType.appendTo(sb, map);
        // appendTo emits the plain, toString()-style form: keys/values are NOT quoted
        assertEquals("{k:1}", sb.toString());

        sb.setLength(0);
        mapType.appendTo(sb, new HashMap<>());
        assertEquals("{}", sb.toString());
    }

    @Test
    public void testSerializeTo_inheritedQuotedForm() throws IOException {
        // MapType does not override serializeTo; it inherits AbstractType's default, which writes
        // stringOf(map) as a quoted/escaped JSON string -- distinct from appendTo's unquoted toString form.
        Map<String, Integer> map = new HashMap<>();
        map.put("k", 1);
        com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        mapType.serializeTo(writer, map, com.landawn.abacus.parser.JsonSerConfig.create());
        String json = writer.toString();
        com.landawn.abacus.util.Objectory.recycle(writer);

        org.junit.jupiter.api.Assertions.assertTrue(json.startsWith("\"") && json.endsWith("\""));
        org.junit.jupiter.api.Assertions.assertTrue(json.contains("k"));

        StringBuilder sb = new StringBuilder();
        mapType.appendTo(sb, map);
        org.junit.jupiter.api.Assertions.assertNotEquals(sb.toString(), json);
    }
}
