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
public class PrimitiveLongArrayType100Test extends TestBase {

    private PrimitiveLongArrayType type;
    private Type<long[]> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(long[].class);
        type = (PrimitiveLongArrayType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(long[].class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<Long> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfEmptyArray() {
        long[] empty = new long[0];
        assertEquals("[]", type.stringOf(empty));
    }

    @Test
    public void testStringOfNonEmptyArray() {
        long[] array = { 1L, 2L, 3L };
        assertEquals("[1, 2, 3]", type.stringOf(array));
    }

    @Test
    public void testStringOfSingleElement() {
        long[] array = { 42L };
        assertEquals("[42]", type.stringOf(array));
    }

    @Test
    public void testValueOfNull() {
        assertNull(type.valueOf(null));
    }

    @Test
    public void testValueOfEmptyString() {
        long[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyArray() {
        long[] result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfNonEmptyArray() {
        long[] result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testValueOfSingleElement() {
        long[] result = type.valueOf("[42]");
        assertNotNull(result);
        assertArrayEquals(new long[] { 42L }, result);
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
        type.appendTo(sb, new long[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new long[] { 1L, 2L, 3L });
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
        type.writeCharacter(writer, new long[0], null);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new long[] { 1L, 2L }, null);
        verify(writer).write('[');
        verify(writer).write(1L);
        verify(writer).write(", ");
        verify(writer).write(2L);
        verify(writer).write(']');
    }

    @Test
    public void testCollection2ArrayNull() {
        assertNull(type.collectionToArray(null));
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Long> collection = new ArrayList<>();
        long[] result = type.collectionToArray(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Long> collection = Arrays.asList(1L, 2L, 3L);
        long[] result = type.collectionToArray(collection);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Long> output = new ArrayList<>();
        type.arrayToCollection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Long> output = new ArrayList<>();
        type.arrayToCollection(new long[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Long> output = new ArrayList<>();
        type.arrayToCollection(new long[] { 1L, 2L, 3L }, output);
        assertEquals(3, output.size());
        assertEquals(Long.valueOf(1L), output.get(0));
        assertEquals(Long.valueOf(2L), output.get(1));
        assertEquals(Long.valueOf(3L), output.get(2));
    }

    @Test
    public void testHashCode() {
        long[] array1 = { 1L, 2L, 3L };
        long[] array2 = { 1L, 2L, 3L };
        assertEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testHashCodeNull() {
        assertEquals(0, type.hashCode(null));
    }

    @Test
    public void testHashCodeDifferent() {
        long[] array1 = { 1L, 2L, 3L };
        long[] array2 = { 1L, 2L, 4L };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(type.equals(null, null));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(type.equals(new long[] { 1L }, null));
        assertFalse(type.equals(null, new long[] { 1L }));
    }

    @Test
    public void testEqualsSame() {
        long[] array = { 1L, 2L, 3L };
        assertTrue(type.equals(array, array));
    }

    @Test
    public void testEqualsEqual() {
        long[] array1 = { 1L, 2L, 3L };
        long[] array2 = { 1L, 2L, 3L };
        assertTrue(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentLength() {
        long[] array1 = { 1L, 2L, 3L };
        long[] array2 = { 1L, 2L };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentContent() {
        long[] array1 = { 1L, 2L, 3L };
        long[] array2 = { 1L, 2L, 4L };
        assertFalse(type.equals(array1, array2));
    }
}
