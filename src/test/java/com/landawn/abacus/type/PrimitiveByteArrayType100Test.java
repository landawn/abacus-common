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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
public class PrimitiveByteArrayType100Test extends TestBase {

    private PrimitiveByteArrayType type;
    private Type<byte[]> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(byte[].class);
        type = (PrimitiveByteArrayType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(byte[].class, type.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<Byte> elementType = type.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testIsPrimitiveByteArray() {
        assertTrue(type.isPrimitiveByteArray());
    }

    @Test
    public void testStringOfNull() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOfEmptyArray() {
        byte[] empty = new byte[0];
        assertEquals("[]", type.stringOf(empty));
    }

    @Test
    public void testStringOfNonEmptyArray() {
        byte[] array = { 1, 2, 3 };
        assertEquals("[1, 2, 3]", type.stringOf(array));
    }

    @Test
    public void testStringOfSingleElement() {
        byte[] array = { 42 };
        assertEquals("[42]", type.stringOf(array));
    }

    @Test
    public void testValueOfNull() {
        assertNull(type.valueOf((String) null));
    }

    @Test
    public void testValueOfEmptyString() {
        byte[] result = type.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfEmptyArray() {
        byte[] result = type.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfNonEmptyArray() {
        byte[] result = type.valueOf("[1, 2, 3]");
        assertNotNull(result);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testValueOfSingleElement() {
        byte[] result = type.valueOf("[42]");
        assertNotNull(result);
        assertArrayEquals(new byte[] { 42 }, result);
    }

    @Test
    public void testValueOfObjectNull() {
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void testValueOfObjectBlob() throws SQLException {
        Blob blob = mock(Blob.class);
        byte[] data = { 1, 2, 3 };
        when(blob.length()).thenReturn(3L);
        when(blob.getBytes(1, 3)).thenReturn(data);

        byte[] result = type.valueOf(blob);
        assertArrayEquals(data, result);
        verify(blob).free();
    }

    @Test
    public void testValueOfObjectBlobThrowsException() throws SQLException {
        Blob blob = mock(Blob.class);
        when(blob.length()).thenThrow(new SQLException("Test exception"));

        assertThrows(Exception.class, () -> type.valueOf(blob));
    }

    @Test
    public void testValueOfObjectOther() {
        String input = "[1, 2, 3]";
        byte[] result = type.valueOf((Object) input);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] expected = { 1, 2, 3 };
        when(rs.getBytes(1)).thenReturn(expected);

        byte[] result = type.get(rs, 1);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] expected = { 1, 2, 3 };
        when(rs.getBytes("column")).thenReturn(expected);

        byte[] result = type.get(rs, "column");
        assertArrayEquals(expected, result);
    }

    @Test
    public void testSetPreparedStatementByIndex() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        byte[] data = { 1, 2, 3 };

        type.set(stmt, 1, data);
        verify(stmt).setBytes(1, data);
    }

    @Test
    public void testSetCallableStatementByName() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        byte[] data = { 1, 2, 3 };

        type.set(stmt, "param", data);
        verify(stmt).setBytes("param", data);
    }

    @Test
    public void testSetPreparedStatementWithSqlType() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        byte[] data = { 1, 2, 3 };

        type.set(stmt, 1, data, java.sql.Types.BINARY);
        verify(stmt).setBytes(1, data);
    }

    @Test
    public void testSetCallableStatementWithSqlType() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        byte[] data = { 1, 2, 3 };

        type.set(stmt, "param", data, java.sql.Types.BINARY);
        verify(stmt).setBytes("param", data);
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
        type.appendTo(sb, new byte[0]);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToNonEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, new byte[] { 1, 2, 3 });
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
        type.writeCharacter(writer, new byte[0], null);
        verify(writer).write('[');
        verify(writer).write(']');
    }

    @Test
    public void testWriteCharacterNonEmptyArray() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        type.writeCharacter(writer, new byte[] { 1, 2 }, null);
        verify(writer).write('[');
        verify(writer).write((byte) 1);
        verify(writer).write(", ");
        verify(writer).write((byte) 2);
        verify(writer).write(']');
    }

    @Test
    public void testCollection2ArrayNull() {
        assertNull(type.collectionToArray(null));
    }

    @Test
    public void testCollection2ArrayEmpty() {
        Collection<Byte> collection = new ArrayList<>();
        byte[] result = type.collectionToArray(collection);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayNonEmpty() {
        Collection<Byte> collection = Arrays.asList((byte) 1, (byte) 2, (byte) 3);
        byte[] result = type.collectionToArray(collection);
        assertArrayEquals(new byte[] { 1, 2, 3 }, result);
    }

    @Test
    public void testArray2CollectionNull() {
        List<Byte> output = new ArrayList<>();
        type.arrayToCollection(null, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionEmpty() {
        List<Byte> output = new ArrayList<>();
        type.arrayToCollection(new byte[0], output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testArray2CollectionNonEmpty() {
        List<Byte> output = new ArrayList<>();
        type.arrayToCollection(new byte[] { 1, 2, 3 }, output);
        assertEquals(3, output.size());
        assertEquals(Byte.valueOf((byte) 1), output.get(0));
        assertEquals(Byte.valueOf((byte) 2), output.get(1));
        assertEquals(Byte.valueOf((byte) 3), output.get(2));
    }

    @Test
    public void testHashCode() {
        byte[] array1 = { 1, 2, 3 };
        byte[] array2 = { 1, 2, 3 };
        assertEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testHashCodeNull() {
        assertEquals(0, type.hashCode(null));
    }

    @Test
    public void testHashCodeDifferent() {
        byte[] array1 = { 1, 2, 3 };
        byte[] array2 = { 1, 2, 4 };
        assertNotEquals(type.hashCode(array1), type.hashCode(array2));
    }

    @Test
    public void testEqualsNull() {
        assertTrue(type.equals(null, null));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(type.equals(new byte[] { 1 }, null));
        assertFalse(type.equals(null, new byte[] { 1 }));
    }

    @Test
    public void testEqualsSame() {
        byte[] array = { 1, 2, 3 };
        assertTrue(type.equals(array, array));
    }

    @Test
    public void testEqualsEqual() {
        byte[] array1 = { 1, 2, 3 };
        byte[] array2 = { 1, 2, 3 };
        assertTrue(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentLength() {
        byte[] array1 = { 1, 2, 3 };
        byte[] array2 = { 1, 2 };
        assertFalse(type.equals(array1, array2));
    }

    @Test
    public void testEqualsDifferentContent() {
        byte[] array1 = { 1, 2, 3 };
        byte[] array2 = { 1, 2, 4 };
        assertFalse(type.equals(array1, array2));
    }
}
