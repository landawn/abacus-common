package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class ObjectArrayType100Test extends TestBase {

    private ObjectArrayType<String> stringArrayType;
    private ObjectArrayType<Integer> intArrayType;
    private ObjectArrayType<Object> objectArrayType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        stringArrayType = (ObjectArrayType<String>) createType(String[].class);
        intArrayType = (ObjectArrayType<Integer>) createType(Integer[].class);
        objectArrayType = (ObjectArrayType<Object>) createType(Object[].class);
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(String[].class, stringArrayType.clazz());
        assertEquals(Integer[].class, intArrayType.clazz());
        assertEquals(Object[].class, objectArrayType.clazz());
    }

    @Test
    public void testGetElementType() {
        assertEquals("String", stringArrayType.getElementType().name());
        assertEquals("Integer", intArrayType.getElementType().name());
        assertEquals("Object", objectArrayType.getElementType().name());
    }

    @Test
    public void testIsObjectArray() {
        assertTrue(stringArrayType.isObjectArray());
        assertTrue(intArrayType.isObjectArray());
        assertTrue(objectArrayType.isObjectArray());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(stringArrayType.isSerializable());
        assertTrue(intArrayType.isSerializable());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(stringArrayType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmptyArray() {
        String[] emptyArray = new String[0];
        assertEquals("[]", stringArrayType.stringOf(emptyArray));
    }

    @Test
    public void testStringOfWithSingleElement() {
        String[] array = { "test" };
        String result = stringArrayType.stringOf(array);
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    public void testStringOfWithMultipleElements() {
        String[] array = { "first", "second", "third" };
        String result = stringArrayType.stringOf(array);
        assertNotNull(result);
        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
        assertTrue(result.contains("third"));
    }

    @Test
    public void testStringOfWithNullElements() {
        String[] array = { "first", null, "third" };
        String result = stringArrayType.stringOf(array);
        assertNotNull(result);
        assertTrue(result.contains("first"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("third"));
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(stringArrayType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        String[] result = stringArrayType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfWithEmptyArrayString() {
        String[] result = stringArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfWithValidJsonArray() {
        String[] result = stringArrayType.valueOf("[\"first\",\"second\",\"third\"]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals("first", result[0]);
        assertEquals("second", result[1]);
        assertEquals("third", result[2]);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringArrayType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        String[] emptyArray = new String[0];
        stringArrayType.appendTo(sb, emptyArray);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToWithArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        String[] array = { "first", "second" };
        stringArrayType.appendTo(sb, array);
        String result = sb.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
    }

    @Test
    public void testAppendToWithWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        String[] array = { "test1", "test2" };
        stringArrayType.appendTo(stringWriter, array);
        String result = stringWriter.toString();
        assertTrue(result.contains("test1"));
        assertTrue(result.contains("test2"));
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        stringArrayType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithEmptyArray() throws IOException {
        String[] emptyArray = new String[0];
        stringArrayType.writeCharacter(writer, emptyArray, config);
        verify(writer, times(2)).write(anyChar());
    }

    @Test
    public void testWriteCharacterWithArray() throws IOException {
        String[] array = { "test" };
        stringArrayType.writeCharacter(writer, array, config);
        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testCollection2ArrayWithNull() {
        assertNull(stringArrayType.collectionToArray(null));
    }

    @Test
    public void testCollection2ArrayWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();
        String[] result = stringArrayType.collectionToArray(emptyList);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayWithElements() {
        List<String> list = Arrays.asList("one", "two", "three");
        String[] result = stringArrayType.collectionToArray(list);
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals("one", result[0]);
        assertEquals("two", result[1]);
        assertEquals("three", result[2]);
    }

    @Test
    public void testArray2CollectionWithNull() {
        List<String> list = new ArrayList<>();
        stringArrayType.arrayToCollection(null, list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testArray2CollectionWithEmptyArray() {
        List<String> list = new ArrayList<>();
        String[] emptyArray = new String[0];
        stringArrayType.arrayToCollection(emptyArray, list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testArray2CollectionWithArray() {
        List<String> list = new ArrayList<>();
        String[] array = { "a", "b", "c" };
        stringArrayType.arrayToCollection(array, list);
        assertEquals(3, list.size());
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains("c"));
    }

    @Test
    public void testHashCode() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "a", "b", "c" };
        String[] array3 = { "x", "y", "z" };

        assertEquals(stringArrayType.hashCode(array1), stringArrayType.hashCode(array2));
        assertNotEquals(stringArrayType.hashCode(array1), stringArrayType.hashCode(array3));
    }

    @Test
    public void testDeepHashCode() {
        Object[] array1 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array2 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array3 = { new String[] { "x", "y" }, new String[] { "z", "w" } };

        assertEquals(objectArrayType.deepHashCode(array1), objectArrayType.deepHashCode(array2));
        assertNotEquals(objectArrayType.deepHashCode(array1), objectArrayType.deepHashCode(array3));
    }

    @Test
    public void testEquals() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "a", "b", "c" };
        String[] array3 = { "x", "y", "z" };

        assertTrue(stringArrayType.equals(array1, array2));
        assertFalse(stringArrayType.equals(array1, array3));
        assertTrue(stringArrayType.equals(null, null));
        assertFalse(stringArrayType.equals(array1, null));
        assertFalse(stringArrayType.equals(null, array1));
    }

    @Test
    public void testDeepEquals() {
        Object[] array1 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array2 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array3 = { new String[] { "x", "y" }, new String[] { "z", "w" } };

        assertTrue(objectArrayType.deepEquals(array1, array2));
        assertFalse(objectArrayType.deepEquals(array1, array3));
        assertTrue(objectArrayType.deepEquals(null, null));
        assertFalse(objectArrayType.deepEquals(array1, null));
        assertFalse(objectArrayType.deepEquals(null, array1));
    }

    @Test
    public void testToString() {
        assertNull(stringArrayType.toString(null));
        assertEquals("[]", stringArrayType.toString(new String[0]));

        String[] array = { "hello", "world" };
        String result = stringArrayType.toString(array);
        assertNotNull(result);
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void testDeepToString() {
        assertNull(objectArrayType.deepToString(null));
        assertEquals("[]", objectArrayType.deepToString(new Object[0]));

        Object[] array = { new String[] { "a", "b" }, new Integer[] { 1, 2 } };
        String result = objectArrayType.deepToString(array);
        assertNotNull(result);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
    }
}
