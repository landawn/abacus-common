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
public class PrimitiveIntArrayType100Test extends TestBase {

    private PrimitiveIntArrayType type;
    private Type<int[]> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(int[].class);
        type = (PrimitiveIntArrayType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(int[].class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<Integer> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfEmptyArray() {
        int[] empty = new int[0];
        assertEquals("[]", type.stringOf(empty));
    }

    @Test
    public void testStringOfNonEmptyArray() {
        int[] array = { 1, 2, 3 };
        assertEquals("[1, 2, 3]", type.stringOf(array));
    }

    @Test
    public void testStringOfSingleElement() {
        int[] array = { 42 };
        assertEquals("[42]", type.stringOf(array));
    }

    @Test
    public void testValueOfNull() {
        assertNull(type.valueOf(null));
    }

    @Test
    public void testValueOfEmptyString() {
        int[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyArray() {
        int[] result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfNonEmptyArray() {
        int[] result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testValueOfSingleElement() {
        int[] result = type.valueOf("[42]");
        assertNotNull(result);
        assertArrayEquals(new int[] { 42 }, result);
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
        type.appendTo(sb, new int[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new int[] { 1, 2, 3 });
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
        type.writeCharacter(writer, new int[0], null);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new int[] { 1, 2 }, null);
        verify(writer).write('[');
        verify(writer).writeInt(1);
        verify(writer).write(", ");
        verify(writer).writeInt(2);
        verify(writer).write(']');
    }

    @Test
    public void testCollection2ArrayNull() {
        assertNull(type.collection2Array(null));
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Integer> collection = new ArrayList<>();
        int[] result = type.collection2Array(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Integer> collection = Arrays.asList(1, 2, 3);
        int[] result = type.collection2Array(collection);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Integer> output = new ArrayList<>();
        type.array2Collection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Integer> output = new ArrayList<>();
        type.array2Collection(new int[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Integer> output = new ArrayList<>();
        type.array2Collection(new int[] { 1, 2, 3 }, output);
        assertEquals(3, output.size());
        assertEquals(Integer.valueOf(1), output.get(0));
        assertEquals(Integer.valueOf(2), output.get(1));
        assertEquals(Integer.valueOf(3), output.get(2));
    }

    @Test
    public void testHashCode() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };
        assertEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testHashCodeNull() {
        assertEquals(0, type.hashCode(null));
    }

    @Test
    public void testHashCodeDifferent() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 4 };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(type.equals(null, null));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(type.equals(new int[] { 1 }, null));
        assertFalse(type.equals(null, new int[] { 1 }));
    }

    @Test
    public void testEqualsSame() {
        int[] array = { 1, 2, 3 };
        assertTrue(type.equals(array, array));
    }

    @Test
    public void testEqualsEqual() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };
        assertTrue(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentLength() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2 };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentContent() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 4 };
        assertFalse(type.equals(array1, array2));
    }
}
