package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import com.landawn.abacus.exception.UncheckedSQLException;
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
    public void testValueOfObject() throws SQLException, IOException {
        Clob clob = mock(Clob.class);
        Reader clobReader = mock(Reader.class);
        when(clob.getCharacterStream()).thenReturn(clobReader);

        Reader result = readerType.valueOf(clob);
        assertNotNull(result);
        result.close();
        result.close();
        verify(clobReader).close();
        verify(clob, times(1)).free();

        assertNull(readerType.valueOf((Object) null));

        result = readerType.valueOf("test");
        assertNotNull(result);
    }

    @Test
    public void testValueOfClobFreesLocatorWhenOpeningFails() throws SQLException {
        final Clob clob = mock(Clob.class);
        final SQLException openFailure = new SQLException("open");
        final SQLException freeFailure = new SQLException("free");
        when(clob.getCharacterStream()).thenThrow(openFailure);
        doThrow(freeFailure).when(clob).free();

        final UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> readerType.valueOf(clob));

        assertSame(openFailure, thrown.getCause());
        assertSame(freeFailure, openFailure.getSuppressed()[0]);
        verify(clob).free();
    }

    @Test
    public void testValueOfClobFreesLocatorWhenDriverReturnsNullReader() throws SQLException {
        final Clob clob = mock(Clob.class);
        when(clob.getCharacterStream()).thenReturn(null);

        assertNull(readerType.valueOf(clob));

        verify(clob).free();
    }

    @Test
    public void testValueOfClobFreesLocatorWhenDelegateCloseFails() throws SQLException, IOException {
        final Clob clob = mock(Clob.class);
        final Reader delegate = mock(Reader.class);
        final IOException closeFailure = new IOException("close");
        final SQLException freeFailure = new SQLException("free");
        when(clob.getCharacterStream()).thenReturn(delegate);
        doThrow(closeFailure).when(delegate).close();
        doThrow(freeFailure).when(clob).free();

        final Reader result = readerType.valueOf(clob);
        final IOException thrown = assertThrows(IOException.class, result::close);

        assertSame(closeFailure, thrown);
        assertSame(freeFailure, thrown.getSuppressed()[0]);
        verify(clob).free();
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
    public void testSerializeTo() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        Reader reader = new StringReader("test");
        readerType.serializeTo(writer, reader, config);

        readerType.serializeTo(writer, null, config);

        when(config.getStringQuotation()).thenReturn('"');
        reader = new StringReader("test");
        readerType.serializeTo(writer, reader, config);
        assertNotNull(reader);
    }

    @Test
    public void testSerializeToPropagatesReaderIOException() {
        final IOException failure = new IOException("read failure");
        final Reader reader = new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                throw failure;
            }

            @Override
            public void close() {
                // no-op
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> readerType.serializeTo(createCharacterWriter(), reader, null)));
    }

    @Test
    public void test_name() {
        assertNotNull(readerType.name());
        assertFalse(readerType.name().isEmpty());
    }

}
