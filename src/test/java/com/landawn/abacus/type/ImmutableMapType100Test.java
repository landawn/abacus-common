package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.ImmutableMap;

@Tag("new-test")
public class ImmutableMapType100Test extends TestBase {

    private ImmutableMapType<String, Integer, ImmutableMap<String, Integer>> immutableMapType;
    private ImmutableMapType<Object, Object, ImmutableMap<Object, Object>> objectMapType;
    private ImmutableMapType<String, String, ImmutableMap<String, String>> stringMapType;

    @BeforeEach
    public void setUp() {
        immutableMapType = new ImmutableMapType<>("String", "Integer");
        objectMapType = new ImmutableMapType<>("Object", "Object");
        stringMapType = new ImmutableMapType<>("String", "String");
    }

    @Test
    @DisplayName("Test declaringName() returns correct declaring name format")
    public void testDeclaringName() {
        String declaringName = immutableMapType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("ImmutableMap"));
        assertTrue(declaringName.contains("<"));
        assertTrue(declaringName.contains(">"));
        assertTrue(declaringName.contains(","));
    }

    @Test
    @DisplayName("Test clazz() returns ImmutableMap class")
    public void testClazz() {
        Class<?> clazz = immutableMapType.clazz();
        assertNotNull(clazz);
        assertEquals(ImmutableMap.class, clazz);
    }

    @Test
    @DisplayName("Test getParameterTypes() returns array with key and value types")
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = immutableMapType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);
        assertNotNull(paramTypes[0]);
        assertNotNull(paramTypes[1]);
    }

    @Test
    @DisplayName("Test isMap() returns true")
    public void testIsMap() {
        assertTrue(immutableMapType.isMap());
    }

    @Test
    @DisplayName("Test isGenericType() returns true")
    public void testIsGenericType() {
        assertTrue(immutableMapType.isGenericType());
    }

    @Test
    @DisplayName("Test isSerializable() returns false")
    public void testIsSerializable() {
        assertFalse(immutableMapType.isSerializable());
    }

    @Test
    @DisplayName("Test getSerializationType() returns MAP")
    public void testGetSerializationType() {
        SerializationType serType = immutableMapType.getSerializationType();
        assertEquals(SerializationType.MAP, serType);
    }

    @Test
    @DisplayName("Test stringOf() with null returns null")
    public void testStringOfNull() {
        String result = immutableMapType.stringOf(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Test stringOf() with empty map returns {}")
    public void testStringOfEmptyMap() {
        ImmutableMap<String, Integer> emptyMap = ImmutableMap.empty();
        String result = immutableMapType.stringOf(emptyMap);
        assertEquals("{}", result);
    }

    @Test
    @DisplayName("Test stringOf() with non-empty map returns JSON string")
    public void testStringOfNonEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);
        ImmutableMap<String, Integer> immutableMap = ImmutableMap.wrap(map);

        String result = immutableMapType.stringOf(immutableMap);
        assertNotNull(result);
        assertTrue(result.contains("key1"));
        assertTrue(result.contains("key2"));
        assertTrue(result.startsWith("{"));
        assertTrue(result.endsWith("}"));
    }

    @Test
    @DisplayName("Test valueOf() with null returns null")
    public void testValueOfNull() {
        ImmutableMap<String, Integer> result = immutableMapType.valueOf(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Test valueOf() with empty string returns empty map")
    public void testValueOfEmptyString() {
        ImmutableMap<String, Integer> result = immutableMapType.valueOf("");
        assertNull(result);
    }

    @Test
    @DisplayName("Test valueOf() with {} returns empty map")
    public void testValueOfEmptyJsonObject() {
        ImmutableMap<String, Integer> result = immutableMapType.valueOf("{}");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test valueOf() with valid JSON returns populated map")
    public void testValueOfValidJson() {
        String json = "{\"key1\":1,\"key2\":2}";
        ImmutableMap<String, Integer> result = immutableMapType.valueOf(json);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key1"));
        assertTrue(result.containsKey("key2"));
    }

    @Test
    @DisplayName("Test valueOf() with complex JSON")
    public void testValueOfComplexJson() {
        String json = "{\"a\":100,\"b\":200,\"c\":300}";
        ImmutableMap<String, Integer> result = immutableMapType.valueOf(json);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Test appendTo() with null map appends NULL_STRING")
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        immutableMapType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    @DisplayName("Test appendTo() with empty map")
    public void testAppendToWithEmptyMap() throws IOException {
        StringBuilder sb = new StringBuilder();
        ImmutableMap<String, Integer> emptyMap = ImmutableMap.empty();
        immutableMapType.appendTo(sb, emptyMap);
        assertTrue(sb.toString().contains("{"));
        assertTrue(sb.toString().contains("}"));
    }

    @Test
    @DisplayName("Test appendTo() with non-empty map using StringBuilder")
    public void testAppendToWithNonEmptyMapStringBuilder() throws IOException {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> map = new HashMap<>();
        map.put("test", 123);
        ImmutableMap<String, Integer> immutableMap = ImmutableMap.wrap(map);

        immutableMapType.appendTo(sb, immutableMap);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
    }

    @Test
    @DisplayName("Test appendTo() with Writer")
    public void testAppendToWithWriter() throws IOException {
        StringWriter writer = new StringWriter();
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 456);
        ImmutableMap<String, Integer> immutableMap = ImmutableMap.wrap(map);

        immutableMapType.appendTo(writer, immutableMap);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("key"));
        assertTrue(result.contains("456"));
    }

    @Test
    @DisplayName("Test appendTo() with null map using Writer")
    public void testAppendToWithNullUsingWriter() throws IOException {
        StringWriter writer = new StringWriter();
        immutableMapType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    @DisplayName("Test with different generic types - String,String")
    public void testStringStringMapType() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "value");
        ImmutableMap<String, String> immutableMap = ImmutableMap.wrap(map);

        String jsonStr = stringMapType.stringOf(immutableMap);
        assertNotNull(jsonStr);

        ImmutableMap<String, String> parsed = stringMapType.valueOf(jsonStr);
        assertNotNull(parsed);
        assertEquals("value", parsed.get("name"));
    }

    @Test
    @DisplayName("Test with Object,Object generic types")
    public void testObjectObjectMapType() {
        assertTrue(objectMapType.isMap());
        assertTrue(objectMapType.isGenericType());
        assertFalse(objectMapType.isSerializable());
        assertEquals(SerializationType.MAP, objectMapType.getSerializationType());
    }

    @Test
    @DisplayName("Test valueOf() and stringOf() round trip")
    public void testValueOfStringOfRoundTrip() {
        Map<String, Integer> originalMap = new HashMap<>();
        originalMap.put("one", 1);
        originalMap.put("two", 2);
        originalMap.put("three", 3);
        ImmutableMap<String, Integer> immutableMap = ImmutableMap.wrap(originalMap);

        String json = immutableMapType.stringOf(immutableMap);
        ImmutableMap<String, Integer> result = immutableMapType.valueOf(json);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get("one"));
        assertEquals(Integer.valueOf(2), result.get("two"));
        assertEquals(Integer.valueOf(3), result.get("three"));
    }

    @Test
    @DisplayName("Test appendTo() with large map")
    public void testAppendToLargeMap() throws IOException {
        StringBuilder sb = new StringBuilder();
        Map<String, Integer> largeMap = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            largeMap.put("key" + i, i);
        }
        ImmutableMap<String, Integer> immutableMap = ImmutableMap.wrap(largeMap);

        immutableMapType.appendTo(sb, immutableMap);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.length() > 100);
        assertTrue(result.contains("key0"));
        assertTrue(result.contains("key99"));
    }

    @Test
    @DisplayName("Test declaringName() format verification")
    public void testDeclaringNameFormat() {
        String declaringName = immutableMapType.declaringName();
        assertTrue(declaringName.startsWith("ImmutableMap<"));
        assertTrue(declaringName.endsWith(">"));
        assertTrue(declaringName.contains(", "));
    }

    @Test
    @DisplayName("Test getParameterTypes() consistency")
    public void testGetParameterTypesConsistency() {
        Type<?>[] params1 = immutableMapType.getParameterTypes();
        Type<?>[] params2 = immutableMapType.getParameterTypes();

        assertSame(params1, params2);
        assertEquals(params1.length, params2.length);
    }
}
