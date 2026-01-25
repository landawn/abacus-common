package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class PrimitiveCharArrayType100Test extends TestBase {

    private PrimitiveCharArrayType type;
    private Type<char[]> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(char[].class);
        type = (PrimitiveCharArrayType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(char[].class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<Character> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfEmptyArray() {
        char[] empty = new char[0];
        assertEquals("[]", type.stringOf(empty));
    }

    @Test
    public void testStringOfNonEmptyArray() {
        char[] array = { 'a', 'b', 'c' };
        assertEquals("['a', 'b', 'c']", type.stringOf(array));
    }

    @Test
    public void testStringOfSingleElement() {
        char[] array = { 'x' };
        assertEquals("['x']", type.stringOf(array));
    }

    @Test
    public void testValueOfNull() {
        assertNull(type.valueOf((String) null));
    }

    @Test
    public void testValueOfEmptyString() {
        char[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyArray() {
        char[] result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfQuotedChars() {
        char[] result = type.valueOf("['a', 'b', 'c']");
        assertNotNull(result);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testValueOfUnquotedChars() {
        char[] result = type.valueOf("[a, b, c]");
        assertNotNull(result);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testValueOfDoubleQuotedChars() {
        char[] result = type.valueOf("[\"a\", \"b\", \"c\"]");
        assertNotNull(result);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testValueOfObjectNull() {
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void testValueOfObjectClob() throws SQLException {
        Clob clob = mock(Clob.class);
        when(clob.length()).thenReturn(3L);
        when(clob.getSubString(1, 3)).thenReturn("abc");

        char[] result = type.valueOf(clob);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        verify(clob).free();
    }

    @Test
    public void testValueOfObjectClobThrowsException() throws SQLException {
        Clob clob = mock(Clob.class);
        when(clob.length()).thenThrow(new SQLException("Test exception"));

        assertThrows(Exception.class, () -> type.valueOf(clob));
    }

    @Test
    public void testValueOfObjectOther() {
        String input = "['a', 'b', 'c']";
        char[] result = type.valueOf((Object) input);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
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
        type.appendTo(sb, new char[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new char[] { 'a', 'b', 'c' });
        assertEquals("[a, b, c]", sb.toString());
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
        type.writeCharacter(writer, new char[0], null);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyArrayNoQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new char[] { 'a', 'b' }, null);
        verify(writer).write('[');
        verify(writer).writeCharacter('a');
        verify(writer).write(", ");
        verify(writer).writeCharacter('b');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(writer, new char[] { 'a', 'b' }, config);
        verify(writer).write('[');
        verify(writer, times(4)).write('\'');
        verify(writer).writeCharacter('a');
        verify(writer).write(", ");
        verify(writer).writeCharacter('b');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterWithQuotationAndEscape() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);
        when(config.getCharQuotation()).thenReturn('\'');

        type.writeCharacter(writer, new char[] { '\'' }, config);
        verify(writer).write('[');
        verify(writer, times(2)).write('\'');
        verify(writer).write('\\');
        verify(writer).writeCharacter('\'');
        verify(writer).write(']');
    }

    @Test
    public void testCollection2ArrayNull() {
        assertNull(type.collectionToArray(null));
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Character> collection = new ArrayList<>();
        char[] result = type.collectionToArray(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Character> collection = Arrays.asList('a', 'b', 'c');
        char[] result = type.collectionToArray(collection);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Character> output = new ArrayList<>();
        type.arrayToCollection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Character> output = new ArrayList<>();
        type.arrayToCollection(new char[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Character> output = new ArrayList<>();
        type.arrayToCollection(new char[] { 'a', 'b', 'c' }, output);
        assertEquals(3, output.size());
        assertEquals(Character.valueOf('a'), output.get(0));
        assertEquals(Character.valueOf('b'), output.get(1));
        assertEquals(Character.valueOf('c'), output.get(2));
    }

    @Test
    public void testHashCode() {
        char[] array1 = { 'a', 'b', 'c' };
        char[] array2 = { 'a', 'b', 'c' };
        assertEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testHashCodeNull() {
        assertEquals(0, type.hashCode(null));
    }

    @Test
    public void testHashCodeDifferent() {
        char[] array1 = { 'a', 'b', 'c' };
        char[] array2 = { 'a', 'b', 'd' };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(type.equals(null, null));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(type.equals(new char[] { 'a' }, null));
        assertFalse(type.equals(null, new char[] { 'a' }));
    }

    @Test
    public void testEqualsSame() {
        char[] array = { 'a', 'b', 'c' };
        assertTrue(type.equals(array, array));
    }

    @Test
    public void testEqualsEqual() {
        char[] array1 = { 'a', 'b', 'c' };
        char[] array2 = { 'a', 'b', 'c' };
        assertTrue(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentLength() {
        char[] array1 = { 'a', 'b', 'c' };
        char[] array2 = { 'a', 'b' };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentContent() {
        char[] array1 = { 'a', 'b', 'c' };
        char[] array2 = { 'a', 'b', 'd' };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testToString() {
        char[] array = { 'a', 'b', 'c' };
        assertEquals("[a, b, c]", type.toString(array));
    }

    @Test
    public void testToStringNull() {
        assertEquals("null", type.toString(null));
    }

    @Test
    public void testToStringEmpty() {
        assertEquals("[]", type.toString(new char[0]));
    }
}
