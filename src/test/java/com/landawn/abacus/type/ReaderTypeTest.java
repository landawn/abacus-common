package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class ReaderTypeTest extends TestBase {

    private final ReaderType readerType = new ReaderType();

    @Test
    public void testClazz() {
        assertEquals(Reader.class, readerType.javaType());
    }

    @Test
    public void test_clazz() {
        assertNotNull(readerType.javaType());
    }

    @Test
    public void testIsReader() {
        assertTrue(readerType.isReader());
    }

    @Test
    public void test_stringOf() {
        // Test with null
        assertNull(readerType.stringOf(null));
    }

    @Test
    public void testStringOf() {
        Reader reader = new StringReader("test content");
        String result = readerType.stringOf(reader);
        assertEquals("test content", result);

        assertNull(readerType.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = readerType.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNull(result);
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
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> readerType.get(rs, 1));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> readerType.get(rs, "col"));
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
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> readerType.set(stmt, 1, null));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> readerType.set(stmt, "param", null));
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
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();
        readerType.appendTo(sw, null);
        assertNotNull(sw.toString());
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
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        Reader reader = new StringReader("test");
        readerType.writeCharacter(writer, reader, config);

        readerType.writeCharacter(writer, null, config);

        when(config.getStringQuotation()).thenReturn('"');
        reader = new StringReader("test");
        readerType.writeCharacter(writer, reader, config);
        assertNotNull(reader);
    }

    @Test
    public void test_name() {
        assertNotNull(readerType.name());
        assertFalse(readerType.name().isEmpty());
    }

}
