package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Pair;

public class PairType100Test extends TestBase {

    private PairType<String, Integer> stringIntPairType;
    private PairType<Double, Boolean> doubleBoolPairType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        stringIntPairType = (PairType<String, Integer>) createType("Pair<String, Integer>");
        doubleBoolPairType = (PairType<Double, Boolean>) createType("Pair<Double, Boolean>");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(stringIntPairType.declaringName());
        assertTrue(stringIntPairType.declaringName().contains("Pair"));
        assertTrue(stringIntPairType.declaringName().contains("String"));
        assertTrue(stringIntPairType.declaringName().contains("Integer"));
    }

    @Test
    public void testClazz() {
        assertEquals(Pair.class, stringIntPairType.clazz());
        assertEquals(Pair.class, doubleBoolPairType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = stringIntPairType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);
        assertEquals("String", paramTypes[0].name());
        assertEquals("Integer", paramTypes[1].name());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(stringIntPairType.isGenericType());
        assertTrue(doubleBoolPairType.isGenericType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(stringIntPairType.stringOf(null));
    }

    @Test
    public void testStringOfWithPair() {
        Pair<String, Integer> pair = Pair.of("test", 123);
        String result = stringIntPairType.stringOf(pair);
        assertNotNull(result);
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testStringOfWithNullElements() {
        Pair<String, Integer> pair = Pair.of(null, null);
        String result = stringIntPairType.stringOf(pair);
        assertNotNull(result);
        assertTrue(result.contains("null"));
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(stringIntPairType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        assertNull(stringIntPairType.valueOf(""));
    }

    @Test
    public void testValueOfWithValidJsonArray() {
        String json = "[\"hello\", 42]";
        Pair<String, Integer> result = stringIntPairType.valueOf(json);
        assertNotNull(result);
        assertEquals("hello", result.left());
        assertEquals(Integer.valueOf(42), result.right());
    }

    @Test
    public void testValueOfWithNullElements() {
        String json = "[null, null]";
        Pair<String, Integer> result = stringIntPairType.valueOf(json);
        assertNotNull(result);
        assertNull(result.left());
        assertNull(result.right());
    }

    @Test
    public void testValueOfWithTypeConversion() {
        String json = "[\"123\", \"456\"]"; // Both strings, but second should convert to Integer
        Pair<String, Integer> result = stringIntPairType.valueOf(json);
        assertNotNull(result);
        assertEquals("123", result.left());
        assertEquals(Integer.valueOf(456), result.right());
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringIntPairType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithPair() throws IOException {
        StringBuilder sb = new StringBuilder();
        Pair<String, Integer> pair = Pair.of("test", 123);
        stringIntPairType.appendTo(sb, pair);
        String result = sb.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("test"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testAppendToWithWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        Pair<String, Integer> pair = Pair.of("abc", 789);
        stringIntPairType.appendTo(stringWriter, pair);
        String result = stringWriter.toString();
        assertTrue(result.contains("abc"));
        assertTrue(result.contains("789"));
    }

    @Test
    public void testAppendToWithNullElements() throws IOException {
        StringBuilder sb = new StringBuilder();
        Pair<String, Integer> pair = Pair.of(null, null);
        stringIntPairType.appendTo(sb, pair);
        String result = sb.toString();
        assertTrue(result.contains("null"));
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        stringIntPairType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithPair() throws IOException {
        Pair<String, Integer> pair = Pair.of("test", 456);
        stringIntPairType.writeCharacter(writer, pair, config);
        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testWriteCharacterWithNullElements() throws IOException {
        Pair<String, Integer> pair = Pair.of(null, null);
        stringIntPairType.writeCharacter(writer, pair, config);
        verify(writer, atLeastOnce()).write(any(char[].class));
    }

    @Test
    public void testGetTypeName() {
        // Test the static getTypeName method indirectly through type creation
        PairType<Long, String> longStringPairType = (PairType<Long, String>) createType("Pair<Long, String>");
        assertNotNull(longStringPairType);
        assertTrue(longStringPairType.name().contains("Long"));
        assertTrue(longStringPairType.name().contains("String"));
    }

    @Test
    public void testComplexPairTypes() {
        // Test with different type combinations
        PairType<Object, Object> objectPairType = (PairType<Object, Object>) createType("Pair<Object, Object>");
        Pair<Object, Object> pair = Pair.of("string", 123);
        String result = objectPairType.stringOf(pair);
        assertNotNull(result);

        // Test valueOf with mixed types
        Pair<Object, Object> parsed = objectPairType.valueOf(result);
        assertNotNull(parsed);
    }
}
