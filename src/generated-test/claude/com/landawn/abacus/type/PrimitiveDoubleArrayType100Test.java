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

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

public class PrimitiveDoubleArrayType100Test extends TestBase {

    private PrimitiveDoubleArrayType type;
    private Type<double[]> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(double[].class);
        type = (PrimitiveDoubleArrayType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(double[].class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<Double> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
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
    public void testValueOfNull() {
        assertNull(type.valueOf(null));
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
    public void testAppendToNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
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
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));
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
    public void testCollection2ArrayNull() {
        assertNull(type.collection2Array(null));
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Double> collection = new ArrayList<>();
        double[] result = type.collection2Array(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Double> collection = Arrays.asList(1.5, 2.7, 3.14);
        double[] result = type.collection2Array(collection);
        assertArrayEquals(new double[] { 1.5, 2.7, 3.14 }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Double> output = new ArrayList<>();
        type.array2Collection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Double> output = new ArrayList<>();
        type.array2Collection(new double[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Double> output = new ArrayList<>();
        type.array2Collection(new double[] { 1.5, 2.7, 3.14 }, output);
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
    public void testHashCodeNull() {
        assertEquals(0, type.hashCode(null));
    }

    @Test
    public void testHashCodeDifferent() {
        double[] array1 = { 1.5, 2.7, 3.14 };
        double[] array2 = { 1.5, 2.7, 3.15 };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(type.equals(null, null));
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
