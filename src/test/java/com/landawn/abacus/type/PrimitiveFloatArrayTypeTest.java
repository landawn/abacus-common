package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

@Tag("2025")
public class PrimitiveFloatArrayTypeTest extends TestBase {

    private final PrimitiveFloatArrayType type = new PrimitiveFloatArrayType();

    @Test
    public void test_clazz() {
        assertEquals(float[].class, type.javaType());
    }

    @Test
    public void test_isPrimitiveArray() {
        assertTrue(type.isPrimitiveArray());
    }

    @Test
    public void test_stringOf() {
        float[] arr = new float[] { 1.1f, 2.2f };
        String result = type.stringOf(arr);
        assertNotNull(result);

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        float[] result = type.valueOf("[1.1, 2.2]");
        assertNotNull(result);

        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        float[] arr = new float[] { 1.1f, 2.2f };
        type.appendTo(sw, arr);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        float[] arr = new float[] { 1.1f, 2.2f };
        type.writeCharacter(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void testGetElementType() {
        Type<Float> elementType = type.elementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfEmptyArray() {
        float[] empty = new float[0];
        assertEquals("[]", type.stringOf(empty));
    }

    @Test
    public void testStringOfNonEmptyArray() {
        float[] array = { 1.5f, 2.7f, 3.14f };
        assertEquals("[1.5, 2.7, 3.14]", type.stringOf(array));
    }

    @Test
    public void testStringOfSingleElement() {
        float[] array = { 42.5f };
        assertEquals("[42.5]", type.stringOf(array));
    }

    @Test
    public void testValueOfEmptyString() {
        float[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyArray() {
        float[] result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfNonEmptyArray() {
        float[] result = type.valueOf("[1.5, 2.7, 3.14]");
        assertNotNull(result);
        assertArrayEquals(new float[] { 1.5f, 2.7f, 3.14f }, result);
    }

    @Test
    public void testValueOfSingleElement() {
        float[] result = type.valueOf("[42.5]");
        assertNotNull(result);
        assertArrayEquals(new float[] { 42.5f }, result);
    }

    @Test
    public void testAppendToEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new float[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new float[] { 1.5f, 2.7f, 3.14f });
        assertEquals("[1.5, 2.7, 3.14]", sb.toString());
    }

    @Test
    public void testWriteCharacterEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new float[0], null);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new float[] { 1.5f, 2.7f }, null);
        verify(writer).write('[');
        verify(writer).write(1.5f);
        verify(writer).write(", ");
        verify(writer).write(2.7f);
        verify(writer).write(']');
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Float> collection = new ArrayList<>();
        float[] result = type.collectionToArray(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Float> collection = Arrays.asList(1.5f, 2.7f, 3.14f);
        float[] result = type.collectionToArray(collection);
        assertArrayEquals(new float[] { 1.5f, 2.7f, 3.14f }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Float> output = new ArrayList<>();
        type.arrayToCollection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Float> output = new ArrayList<>();
        type.arrayToCollection(new float[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Float> output = new ArrayList<>();
        type.arrayToCollection(new float[] { 1.5f, 2.7f, 3.14f }, output);
        assertEquals(3, output.size());
        assertEquals(Float.valueOf(1.5f), output.get(0));
        assertEquals(Float.valueOf(2.7f), output.get(1));
        assertEquals(Float.valueOf(3.14f), output.get(2));
    }

    @Test
    public void testHashCode() {
        float[] array1 = { 1.5f, 2.7f, 3.14f };
        float[] array2 = { 1.5f, 2.7f, 3.14f };
        assertEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testHashCodeDifferent() {
        float[] array1 = { 1.5f, 2.7f, 3.14f };
        float[] array2 = { 1.5f, 2.7f, 3.15f };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(type.equals(new float[] { 1.5f }, null));
        assertFalse(type.equals(null, new float[] { 1.5f }));
    }

    @Test
    public void testEqualsSame() {
        float[] array = { 1.5f, 2.7f, 3.14f };
        assertTrue(type.equals(array, array));
    }

    @Test
    public void testEqualsEqual() {
        float[] array1 = { 1.5f, 2.7f, 3.14f };
        float[] array2 = { 1.5f, 2.7f, 3.14f };
        assertTrue(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentLength() {
        float[] array1 = { 1.5f, 2.7f, 3.14f };
        float[] array2 = { 1.5f, 2.7f };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentContent() {
        float[] array1 = { 1.5f, 2.7f, 3.14f };
        float[] array2 = { 1.5f, 2.7f, 3.15f };
        assertFalse(type.equals(array1, array2));
    }

}
