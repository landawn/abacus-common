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
public class PrimitiveShortArrayTypeTest extends TestBase {

    private final PrimitiveShortArrayType type = new PrimitiveShortArrayType();

    @Test
    public void test_clazz() {
        assertEquals(short[].class, type.javaType());
    }

    @Test
    public void test_isPrimitiveArray() {
        assertTrue(type.isPrimitiveArray());
    }

    @Test
    public void test_stringOf() {
        short[] arr = new short[] { (short) 1, (short) 2 };
        String result = type.stringOf(arr);
        assertNotNull(result);

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        short[] result = type.valueOf("[1, 2]");
        assertNotNull(result);

        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        short[] arr = new short[] { (short) 1, (short) 2 };
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

        short[] arr = new short[] { (short) 1, (short) 2 };
        type.writeCharacter(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void testGetElementType() {
        Type<Short> elementType = type.elementType();
        assertNotNull(elementType);
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
    public void testHashCodeDifferent() {
        short[] array1 = { 1, 2, 3 };
        short[] array2 = { 1, 2, 4 };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
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
