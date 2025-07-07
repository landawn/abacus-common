package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class ByteArrayType100Test extends TestBase {

    private ByteArrayType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (ByteArrayType) createType("Byte[]");
        writer = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        String result = type.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOf_Empty() {
        Byte[] array = new Byte[0];
        String result = type.stringOf(array);
        assertEquals("[]", result);
    }

    @Test
    public void testStringOf_SingleElement() {
        Byte[] array = new Byte[] { (byte) 42 };
        String result = type.stringOf(array);
        assertEquals("[42]", result);
    }

    @Test
    public void testStringOf_MultipleElements() {
        Byte[] array = new Byte[] { (byte) 1, (byte) -128, null, (byte) 127 };
        String result = type.stringOf(array);
        assertEquals("[1, -128, null, 127]", result);
    }

    @Test
    public void testValueOf_Null() {
        Byte[] result = type.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        Byte[] result = type.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyArray() {
        Byte[] result = type.valueOf("[]");
        Assertions.assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOf_SingleElement() {
        Byte[] result = type.valueOf("[42]");
        Assertions.assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(Byte.valueOf((byte) 42), result[0]);
    }

    @Test
    public void testValueOf_MultipleElements() {
        Byte[] result = type.valueOf("[1, -128, null, 127]");
        Assertions.assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(Byte.valueOf((byte) 1), result[0]);
        assertEquals(Byte.valueOf((byte) -128), result[1]);
        Assertions.assertNull(result[2]);
        assertEquals(Byte.valueOf((byte) 127), result[3]);
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] primitiveArray = new byte[] { 1, 2, 3 };
        when(rs.getBytes(1)).thenReturn(primitiveArray);

        Byte[] result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(Byte.valueOf((byte) 1), result[0]);
        assertEquals(Byte.valueOf((byte) 2), result[1]);
        assertEquals(Byte.valueOf((byte) 3), result[2]);
        verify(rs).getBytes(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getBytes(1)).thenReturn(null);

        Byte[] result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getBytes(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        byte[] primitiveArray = new byte[] { -1, 0, 1 };
        when(rs.getBytes("columnName")).thenReturn(primitiveArray);

        Byte[] result = type.get(rs, "columnName");

        Assertions.assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(Byte.valueOf((byte) -1), result[0]);
        assertEquals(Byte.valueOf((byte) 0), result[1]);
        assertEquals(Byte.valueOf((byte) 1), result[2]);
        verify(rs).getBytes("columnName");
    }

    @Test
    public void testSet_PreparedStatement_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Byte[] array = new Byte[] { 1, 2, 3 };

        type.set(stmt, 1, array);

        verify(stmt).setBytes(eq(1), any(byte[].class));
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setBytes(1, null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Byte[] array = new Byte[] { -128, 0, 127 };

        type.set(stmt, "paramName", array);

        verify(stmt).setBytes(eq("paramName"), any(byte[].class));
    }

    @Test
    public void testSet_PreparedStatement_Int_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Byte[] array = new Byte[] { 1, 2 };

        type.set(stmt, 1, array, 100);

        verify(stmt).setBytes(eq(1), any(byte[].class));
    }

    @Test
    public void testSet_CallableStatement_String_Int() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Byte[] array = new Byte[] { 0 };

        type.set(stmt, "paramName", array, 100);

        verify(stmt).setBytes(eq("paramName"), any(byte[].class));
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringWriter sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringWriter sw = new StringWriter();
        Byte[] array = new Byte[0];
        type.appendTo(sw, array);
        assertEquals("[]", sw.toString());
    }

    @Test
    public void testAppendTo_SingleElement() throws IOException {
        StringWriter sw = new StringWriter();
        Byte[] array = new Byte[] { (byte) -1 };
        type.appendTo(sw, array);
        assertEquals("[-1]", sw.toString());
    }

    @Test
    public void testAppendTo_MultipleElements() throws IOException {
        StringWriter sw = new StringWriter();
        Byte[] array = new Byte[] { (byte) 0, null, (byte) 100 };
        type.appendTo(sw, array);
        assertEquals("[0, null, 100]", sw.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        type.writeCharacter(mockWriter, null, null);
        verify(mockWriter).write("null".toCharArray());
    }

    @Test
    public void testWriteCharacter_Empty() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Byte[] array = new Byte[0];
        type.writeCharacter(mockWriter, array, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_SingleElement() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Byte[] array = new Byte[] { (byte) 42 };
        type.writeCharacter(mockWriter, array, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write((byte) 42);
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_MultipleElements() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        Byte[] array = new Byte[] { (byte) 1, null, (byte) -1 };
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        type.writeCharacter(mockWriter, array, config);

        verify(mockWriter).write('[');
        verify(mockWriter).write((byte) 1);
        verify(mockWriter, times(2)).write(", ");
        verify(mockWriter).write("null".toCharArray());
        verify(mockWriter).write((byte) -1);
        verify(mockWriter).write(']');
    }
}
