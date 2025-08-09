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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

public class AsciiStreamType100Test extends TestBase {

    private Type<InputStream> asciiStreamType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        asciiStreamType = createType("AsciiStream");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        when(rs.getAsciiStream(1)).thenReturn(stream);

        assertEquals(stream, asciiStreamType.get(rs, 1));
        verify(rs).getAsciiStream(1);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        when(rs.getAsciiStream("data")).thenReturn(stream);

        assertEquals(stream, asciiStreamType.get(rs, "data"));
        verify(rs).getAsciiStream("data");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        asciiStreamType.set(stmt, 1, stream);
        verify(stmt).setAsciiStream(1, stream);

        asciiStreamType.set(stmt, 2, null);
        verify(stmt).setAsciiStream(2, null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        asciiStreamType.set(stmt, "param1", stream);
        verify(stmt).setAsciiStream("param1", stream);

        asciiStreamType.set(stmt, "param2", null);
        verify(stmt).setAsciiStream("param2", null);
    }

    @Test
    public void testSetPreparedStatementWithLength() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        asciiStreamType.set(stmt, 1, stream, 100);
        verify(stmt).setAsciiStream(1, stream, 100);
    }

    @Test
    public void testSetCallableStatementWithLength() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        InputStream stream = new ByteArrayInputStream("test".getBytes());

        asciiStreamType.set(stmt, "param", stream, 100);
        verify(stmt).setAsciiStream("param", stream, 100);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        asciiStreamType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithWriter() throws IOException {
        StringWriter sw = new StringWriter();
        InputStream stream = new ByteArrayInputStream("hello world".getBytes());

        asciiStreamType.appendTo(sw, stream);
        assertEquals("hello world", sw.toString());
    }

    @Test
    public void testAppendToWithNonWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream stream = new ByteArrayInputStream("hello world".getBytes());

        asciiStreamType.appendTo(sb, stream);
        assertEquals("hello world", sb.toString());
    }

    @Test
    public void testWriteCharacterWithNull() throws IOException {
        asciiStreamType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithStream() throws IOException {
        InputStream stream = new ByteArrayInputStream("test data".getBytes());

        asciiStreamType.writeCharacter(writer, stream, null);
        verify(writer, atLeastOnce()).writeCharacter(any(char[].class), anyInt(), anyInt());
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        InputStream stream = new ByteArrayInputStream("test".getBytes());
        when(config.getStringQuotation()).thenReturn('"');

        asciiStreamType.writeCharacter(writer, stream, config);
        verify(writer, times(2)).write('"');
        verify(writer, atLeastOnce()).writeCharacter(any(char[].class), anyInt(), anyInt());
    }
}
