package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class PrimitiveFloatArrayType100Test extends TestBase {

    private PrimitiveFloatArrayType type;
    private Type<float[]> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(float[].class);
        type = (PrimitiveFloatArrayType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(float[].class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<Float> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
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
    public void testValueOfNull() {
        assertNull(type.valueOf(null));
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
    public void testAppendToNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
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
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));
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
    public void testCollection2ArrayNull() {
        assertNull(type.collection2Array(null));
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Float> collection = new ArrayList<>();
        float[] result = type.collection2Array(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Float> collection = Arrays.asList(1.5f, 2.7f, 3.14f);
        float[] result = type.collection2Array(collection);
        assertArrayEquals(new float[] { 1.5f, 2.7f, 3.14f }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Float> output = new ArrayList<>();
        type.array2Collection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Float> output = new ArrayList<>();
        type.array2Collection(new float[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Float> output = new ArrayList<>();
        type.array2Collection(new float[] { 1.5f, 2.7f, 3.14f }, output);
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
    public void testHashCodeNull() {
        assertEquals(0, type.hashCode(null));
    }

    @Test
    public void testHashCodeDifferent() {
        float[] array1 = { 1.5f, 2.7f, 3.14f };
        float[] array2 = { 1.5f, 2.7f, 3.15f };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(type.equals(null, null));
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
