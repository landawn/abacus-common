package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class ClobAsciiStreamType100Test extends TestBase {

    private ClobAsciiStreamType type;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        type = (ClobAsciiStreamType) createType("ClobAsciiStream");
        writer = createCharacterWriter();
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob clob = mock(Clob.class);
        InputStream expectedStream = new ByteArrayInputStream("test".getBytes());

        when(rs.getClob(1)).thenReturn(clob);
        when(clob.getAsciiStream()).thenReturn(expectedStream);

        InputStream result = type.get(rs, 1);

        assertEquals(expectedStream, result);
        verify(rs).getClob(1);
        verify(clob).getAsciiStream();
    }

    @Test
    public void testGet_ResultSet_Int_NullClob() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getClob(1)).thenReturn(null);

        InputStream result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getClob(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Clob clob = mock(Clob.class);
        InputStream expectedStream = new ByteArrayInputStream("data".getBytes());

        when(rs.getClob("columnName")).thenReturn(clob);
        when(clob.getAsciiStream()).thenReturn(expectedStream);

        InputStream result = type.get(rs, "columnName");

        assertEquals(expectedStream, result);
        verify(rs).getClob("columnName");
        verify(clob).getAsciiStream();
    }

    @Test
    public void testSet_PreparedStatement_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        type.set(stmt, 1, stream);

        verify(stmt).setAsciiStream(1, stream);
    }

    @Test
    public void testSet_PreparedStatement_Int_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        type.set(stmt, 1, null);

        verify(stmt).setAsciiStream(1, null);
    }

    @Test
    public void testSet_CallableStatement_String() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        type.set(stmt, "paramName", stream);

        verify(stmt).setAsciiStream("paramName", stream);
    }

    @Test
    public void testSet_PreparedStatement_Int_Int() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        type.set(stmt, 1, stream, 100);

        verify(stmt).setAsciiStream(1, stream, 100);
    }

    @Test
    public void testSet_CallableStatement_String_Int() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        type.set(stmt, "paramName", stream, 200);

        verify(stmt).setAsciiStream("paramName", stream, 200);
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringWriter sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void testAppendTo_InputStream() throws IOException {
        StringWriter sw = new StringWriter();
        InputStream stream = new ByteArrayInputStream("Hello ASCII".getBytes());

        type.appendTo(sw, stream);

        assertEquals("Hello ASCII", sw.toString());
    }

    @Test
    public void testAppendTo_Writer() throws IOException {
        Writer mockWriter = mock(Writer.class);
        InputStream stream = new ByteArrayInputStream("Test data".getBytes());

        type.appendTo(mockWriter, stream);

    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        type.writeCharacter(mockWriter, null, null);
        verify(mockWriter).write("null".toCharArray());
    }

    @Test
    public void testWriteCharacter_NoQuotation() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        InputStream stream = new ByteArrayInputStream("ASCII content".getBytes());
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getStringQuotation()).thenReturn((char) 0);

        type.writeCharacter(mockWriter, stream, config);

        verify(mockWriter, atLeastOnce()).writeCharacter(any(char[].class), anyInt(), anyInt());
    }

    @Test
    public void testWriteCharacter_WithQuotation() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        InputStream stream = new ByteArrayInputStream("data".getBytes());
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getStringQuotation()).thenReturn('"');

        type.writeCharacter(mockWriter, stream, config);

        verify(mockWriter, times(2)).write('"');
        verify(mockWriter, atLeastOnce()).writeCharacter(any(char[].class), anyInt(), anyInt());
    }

    @Test
    public void testClob2AsciiStream() throws SQLException {
        Clob clob = mock(Clob.class);
        InputStream expectedStream = new ByteArrayInputStream("clob data".getBytes());
        when(clob.getAsciiStream()).thenReturn(expectedStream);

        InputStream result = ClobAsciiStreamType.clob2AsciiStream(clob);

        assertEquals(expectedStream, result);
        verify(clob).getAsciiStream();
    }

    @Test
    public void testClob2AsciiStream_Null() throws SQLException {
        InputStream result = ClobAsciiStreamType.clob2AsciiStream(null);
        Assertions.assertNull(result);
    }
}
