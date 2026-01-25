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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class PrimitiveShortArrayType100Test extends TestBase {

    private PrimitiveShortArrayType type;
    private Type<short[]> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(short[].class);
        type = (PrimitiveShortArrayType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(short[].class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<Short> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfEmptyArray() {
        short[] empty = new short[0];
        assertEquals("[]", type.stringOf(empty));
    }

    @Test
    public void testStringOfNonEmptyArray() {
        short[] array = { 1, 2, 3 };
        assertEquals("[1, 2, 3]", type.stringOf(array));
    }

    @Test
    public void testStringOfSingleElement() {
        short[] array = { 42 };
        assertEquals("[42]", type.stringOf(array));
    }

    @Test
    public void testValueOfNull() {
        assertNull(type.valueOf(null));
    }

    @Test
    public void testValueOfEmptyString() {
        short[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyArray() {
        short[] result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfNonEmptyArray() {
        short[] result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testValueOfSingleElement() {
        short[] result = type.valueOf("[42]");
        assertNotNull(result);
        assertArrayEquals(new short[] { 42 }, result);
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
        type.appendTo(sb, new short[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new short[] { 1, 2, 3 });
        assertEquals("[1, 2, 3]", sb.toString());
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
        type.writeCharacter(writer, new short[0], null);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new short[] { 1, 2 }, null);
        verify(writer).write('[');
        verify(writer).write((short) 1);
        verify(writer).write(", ");
        verify(writer).write((short) 2);
        verify(writer).write(']');
    }

    @Test
    public void testCollection2ArrayNull() {
        assertNull(type.collectionToArray(null));
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Short> collection = new ArrayList<>();
        short[] result = type.collectionToArray(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Short> collection = Arrays.asList((short) 1, (short) 2, (short) 3);
        short[] result = type.collectionToArray(collection);
        assertArrayEquals(new short[] { 1, 2, 3 }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Short> output = new ArrayList<>();
        type.arrayToCollection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Short> output = new ArrayList<>();
        type.arrayToCollection(new short[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Short> output = new ArrayList<>();
        type.arrayToCollection(new short[] { 1, 2, 3 }, output);
        assertEquals(3, output.size());
        assertEquals(Short.valueOf((short) 1), output.get(0));
        assertEquals(Short.valueOf((short) 2), output.get(1));
        assertEquals(Short.valueOf((short) 3), output.get(2));
    }

    @Test
    public void testHashCode() {
        short[] array1 = { 1, 2, 3 };
        short[] array2 = { 1, 2, 3 };
        assertEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testHashCodeNull() {
        assertEquals(0, type.hashCode(null));
    }

    @Test
    public void testHashCodeDifferent() {
        short[] array1 = { 1, 2, 3 };
        short[] array2 = { 1, 2, 4 };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(type.equals(null, null));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(type.equals(new short[] { 1 }, null));
        assertFalse(type.equals(null, new short[] { 1 }));
    }

    @Test
    public void testEqualsSame() {
        short[] array = { 1, 2, 3 };
        assertTrue(type.equals(array, array));
    }

    @Test
    public void testEqualsEqual() {
        short[] array1 = { 1, 2, 3 };
        short[] array2 = { 1, 2, 3 };
        assertTrue(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentLength() {
        short[] array1 = { 1, 2, 3 };
        short[] array2 = { 1, 2 };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentContent() {
        short[] array1 = { 1, 2, 3 };
        short[] array2 = { 1, 2, 4 };
        assertFalse(type.equals(array1, array2));
    }
}
