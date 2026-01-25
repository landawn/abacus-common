package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class ReaderType100Test extends TestBase {

    private ReaderType readerType;

    @BeforeEach
    public void setUp() {
        readerType = (ReaderType) createType(Reader.class);
    }

    @Test
    public void testClazz() {
        assertEquals(Reader.class, readerType.clazz());
    }

    @Test
    public void testIsReader() {
        assertTrue(readerType.isReader());
    }

    @Test
    public void testStringOf() {
        Reader reader = new StringReader("test content");
        String result = readerType.stringOf(reader);
        assertEquals("test content", result);

        assertNull(readerType.stringOf(null));
    }

    @Test
    public void testValueOfString() {
        Reader reader = readerType.valueOf("test content");
        assertNotNull(reader);

        assertNull(readerType.valueOf((String) null));
    }

    @Test
    public void testValueOfObject() throws SQLException {
        Clob clob = mock(Clob.class);
        Reader clobReader = new StringReader("clob content");
        when(clob.getCharacterStream()).thenReturn(clobReader);

        Reader result = readerType.valueOf(clob);
        assertEquals(clobReader, result);

        assertNull(readerType.valueOf((Object) null));

        result = readerType.valueOf("test");
        assertNotNull(result);
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Reader reader = new StringReader("test");
        when(rs.getCharacterStream(1)).thenReturn(reader);

        assertEquals(reader, readerType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Reader reader = new StringReader("test");
        when(rs.getCharacterStream("column")).thenReturn(reader);

        assertEquals(reader, readerType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Reader reader = new StringReader("test");

        readerType.set(stmt, 1, reader);
        verify(stmt).setCharacterStream(1, reader);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Reader reader = new StringReader("test");

        readerType.set(stmt, "param", reader);
        verify(stmt).setCharacterStream("param", reader);
    }

    @Test
    public void testSetPreparedStatementWithLength() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Reader reader = new StringReader("test");

        readerType.set(stmt, 1, reader, 100);
        verify(stmt).setCharacterStream(1, reader, 100);
    }

    @Test
    public void testSetCallableStatementWithLength() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Reader reader = new StringReader("test");

        readerType.set(stmt, "param", reader, 100);
        verify(stmt).setCharacterStream("param", reader, 100);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();
        Reader reader = new StringReader("test content");
        readerType.appendTo(writer, reader);
        assertEquals("test content", writer.toString());

        StringBuilder sb = new StringBuilder();
        reader = new StringReader("test content");
        readerType.appendTo(sb, reader);
        assertEquals("test content", sb.toString());

        writer = new StringWriter();
        readerType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        Reader reader = new StringReader("test");
        readerType.writeCharacter(writer, reader, config);

        readerType.writeCharacter(writer, null, config);

        when(config.getStringQuotation()).thenReturn('"');
        reader = new StringReader("test");
        readerType.writeCharacter(writer, reader, config);
    }
}
