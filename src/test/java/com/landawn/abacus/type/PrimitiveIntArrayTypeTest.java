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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class PrimitiveIntArrayTypeTest extends TestBase {

    private final PrimitiveIntArrayType type = new PrimitiveIntArrayType();

    @Test
    public void test_clazz() {
        assertEquals(int[].class, type.javaType());
    }

    @Test
    public void testGetElementType() {
        Type<Integer> elementType = type.elementType();
        assertNotNull(elementType);
    }

    @Test
    public void test_stringOf() {
        int[] arr = new int[] { 1, 2, 3 };
        String result = type.stringOf(arr);
        assertNotNull(result);

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
    public void test_valueOf_String() {
        int[] result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);

        assertNull(type.valueOf((String) null));
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
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        int[] arr = new int[] { 1, 2, 3 };
        type.appendTo(sw, arr);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
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
    public void test_writeCharacter() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        int[] arr = new int[] { 1, 2, 3 };
        type.writeCharacter(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
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
    public void testCollection2ArrayEmpty() {
        Collection<Integer> collection = new ArrayList<>();
        int[] result = type.collectionToArray(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Integer> collection = Arrays.asList(1, 2, 3);
        int[] result = type.collectionToArray(collection);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Integer> output = new ArrayList<>();
        type.arrayToCollection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Integer> output = new ArrayList<>();
        type.arrayToCollection(new int[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Integer> output = new ArrayList<>();
        type.arrayToCollection(new int[] { 1, 2, 3 }, output);
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
    public void testHashCodeDifferent() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 4 };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
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
    public void test_isPrimitiveArray() {
        assertTrue(type.isPrimitiveArray());
    }

}
