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
public class PrimitiveDoubleArrayTypeTest extends TestBase {

    private final PrimitiveDoubleArrayType type = new PrimitiveDoubleArrayType();

    @Test
    public void test_clazz() {
        assertEquals(double[].class, type.javaType());
    }

    @Test
    public void test_isPrimitiveArray() {
        assertTrue(type.isPrimitiveArray());
    }

    @Test
    public void test_stringOf() {
        double[] arr = new double[] { 1.1, 2.2 };
        String result = type.stringOf(arr);
        assertNotNull(result);

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        double[] result = type.valueOf("[1.1, 2.2]");
        assertNotNull(result);

        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        double[] arr = new double[] { 1.1, 2.2 };
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

        double[] arr = new double[] { 1.1, 2.2 };
        type.writeCharacter(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        type.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void testGetElementType() {
        Type<Double> elementType = type.elementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfEmptyArray() {
        double[] empty = new double[0];
        assertEquals("[]", type.stringOf(empty));
    }

    @Test
    public void testStringOfNonEmptyArray() {
        double[] array = { 1.5, 2.7, 3.14 };
        assertEquals("[1.5, 2.7, 3.14]", type.stringOf(array));
    }

    @Test
    public void testStringOfSingleElement() {
        double[] array = { 42.5 };
        assertEquals("[42.5]", type.stringOf(array));
    }

    @Test
    public void testValueOfEmptyString() {
        double[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyArray() {
        double[] result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfNonEmptyArray() {
        double[] result = type.valueOf("[1.5, 2.7, 3.14]");
        assertNotNull(result);
        assertArrayEquals(new double[] { 1.5, 2.7, 3.14 }, result);
    }

    @Test
    public void testValueOfSingleElement() {
        double[] result = type.valueOf("[42.5]");
        assertNotNull(result);
        assertArrayEquals(new double[] { 42.5 }, result);
    }

    @Test
    public void testAppendToEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new double[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new double[] { 1.5, 2.7, 3.14 });
        assertEquals("[1.5, 2.7, 3.14]", sb.toString());
    }

    @Test
    public void testWriteCharacterEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new double[0], null);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new double[] { 1.5, 2.7 }, null);
        verify(writer).write('[');
        verify(writer).write(1.5);
        verify(writer).write(", ");
        verify(writer).write(2.7);
        verify(writer).write(']');
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Double> collection = new ArrayList<>();
        double[] result = type.collectionToArray(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Double> collection = Arrays.asList(1.5, 2.7, 3.14);
        double[] result = type.collectionToArray(collection);
        assertArrayEquals(new double[] { 1.5, 2.7, 3.14 }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Double> output = new ArrayList<>();
        type.arrayToCollection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Double> output = new ArrayList<>();
        type.arrayToCollection(new double[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Double> output = new ArrayList<>();
        type.arrayToCollection(new double[] { 1.5, 2.7, 3.14 }, output);
        assertEquals(3, output.size());
        assertEquals(Double.valueOf(1.5), output.get(0));
        assertEquals(Double.valueOf(2.7), output.get(1));
        assertEquals(Double.valueOf(3.14), output.get(2));
    }

    @Test
    public void testHashCode() {
        double[] array1 = { 1.5, 2.7, 3.14 };
        double[] array2 = { 1.5, 2.7, 3.14 };
        assertEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testHashCodeDifferent() {
        double[] array1 = { 1.5, 2.7, 3.14 };
        double[] array2 = { 1.5, 2.7, 3.15 };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(type.equals(new double[] { 1.5 }, null));
        assertFalse(type.equals(null, new double[] { 1.5 }));
    }

    @Test
    public void testEqualsSame() {
        double[] array = { 1.5, 2.7, 3.14 };
        assertTrue(type.equals(array, array));
    }

    @Test
    public void testEqualsEqual() {
        double[] array1 = { 1.5, 2.7, 3.14 };
        double[] array2 = { 1.5, 2.7, 3.14 };
        assertTrue(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentLength() {
        double[] array1 = { 1.5, 2.7, 3.14 };
        double[] array2 = { 1.5, 2.7 };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentContent() {
        double[] array1 = { 1.5, 2.7, 3.14 };
        double[] array2 = { 1.5, 2.7, 3.15 };
        assertFalse(type.equals(array1, array2));
    }

}
