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

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
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
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;

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

    // ---- review fixes 2026-09-06: T3-05 valueOf(Object) with a char[] reads the raw characters ----

    @Test
    public void reviewFixes20260906_valueOfObjectCharArrayReadsRawCharacters() throws IOException {
        assertEquals("ab", IOUtil.readAllToString(readerType.valueOf(new char[] { 'a', 'b' })));
        assertEquals("", IOUtil.readAllToString(readerType.valueOf(new char[0])));
        // surrogate pair and non-Latin text survive
        assertEquals("中😀", IOUtil.readAllToString(readerType.valueOf("中😀".toCharArray())));
        // the list text is exactly what a char[] used to become
        assertEquals("['a', 'b']", Type.of(char[].class).stringOf(new char[] { 'a', 'b' }));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectCharArrayHonoursTheHandledReaderClass() throws IOException {
        final char[] chars = { 'a', 'b' };

        final Reader sr = Type.of(StringReader.class).valueOf(chars);
        assertTrue(sr instanceof StringReader);
        assertEquals("ab", IOUtil.readAllToString(sr));

        final Reader car = Type.of(CharArrayReader.class).valueOf(chars);
        assertTrue(car instanceof CharArrayReader);
        assertEquals("ab", IOUtil.readAllToString(car));

        final Reader br = Type.of(BufferedReader.class).valueOf(chars);
        assertTrue(br instanceof BufferedReader);
        assertEquals("ab", IOUtil.readAllToString(br));

        final Reader pr = Type.of(PushbackReader.class).valueOf(chars);
        assertTrue(pr instanceof PushbackReader);
        assertEquals("ab", IOUtil.readAllToString(pr));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectCharArrayUnsupportedReaderClassStillThrows() {
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileReader.class).valueOf(new char[] { 'a' }));
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileReader.class).valueOf("x"));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectOtherRoutesUnchanged() throws IOException {
        assertEquals("sb", IOUtil.readAllToString(readerType.valueOf(new StringBuilder("sb"))));
        assertEquals("7", IOUtil.readAllToString(readerType.valueOf(7)));
        // a byte[] is NOT decoded with an assumed charset: it still takes the string route
        assertEquals("[1, 2]", IOUtil.readAllToString(readerType.valueOf(new byte[] { 1, 2 })));
        assertNull(readerType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260906_convertCharArrayToReader() throws IOException {
        assertEquals("ab", IOUtil.readAllToString(N.convert(new char[] { 'a', 'b' }, Reader.class)));
        assertEquals("ab", IOUtil.readAllToString(N.convert(new char[] { 'a', 'b' }, StringReader.class)));
    }

    // ---- bug review 2026-09-30 G121 begin ----
    // G121: a Reader argument is returned as is (it was read into a String and consumed).
    @Test
    public void testValueOfObject_readerIsReturnedAsIs() throws IOException {
        final Reader reader = new StringReader("abc");
        assertSame(reader, readerType.valueOf(reader));
        assertEquals("abc", IOUtil.readAllToString(reader));

        // unchanged: char[] and other objects
        assertEquals("ab", IOUtil.readAllToString(readerType.valueOf(new char[] { 'a', 'b' })));
        assertEquals("7", IOUtil.readAllToString(readerType.valueOf(7)));
    }
    // ---- bug review 2026-09-30 G121 end ----

    // ---- bug review 2026-09-30 G142 begin ----
    /** Records close() so a test can check that the conversion leaves the caller's reader open. */
    private static final class G142CloseTrackingReader extends java.io.FilterReader {
        boolean closed;

        G142CloseTrackingReader(final String text) {
            super(new StringReader(text));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    // G142: a Reader of another class is copied into a reader of the handled class (consumed, not closed); an unsupported target
    // is rejected before a supplied reader or stream is read - it drained the caller's reader and then threw.
    @Test
    public void testValueOfObject_readerOfAnotherClassAndUnsupportedTarget() throws IOException {
        final G142CloseTrackingReader source = new G142CloseTrackingReader("abc\u00e9");
        final Reader converted = Type.of(BufferedReader.class).valueOf(source);
        org.junit.jupiter.api.Assertions.assertTrue(converted instanceof BufferedReader);
        org.junit.jupiter.api.Assertions.assertEquals("abc\u00e9", com.landawn.abacus.util.IOUtil.readAllToString(converted));
        org.junit.jupiter.api.Assertions.assertFalse(source.closed);
        org.junit.jupiter.api.Assertions.assertEquals(-1, source.read());

        final StringReader unread = new StringReader("xyz");
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileReader.class).valueOf(unread));
        org.junit.jupiter.api.Assertions.assertEquals('x', unread.read());

        final java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(new byte[] { 'q' });
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileReader.class).valueOf(stream));
        org.junit.jupiter.api.Assertions.assertEquals(1, stream.available());
        org.junit.jupiter.api.Assertions.assertEquals("q",
                com.landawn.abacus.util.IOUtil.readAllToString(Type.of(StringReader.class).valueOf(stream)));
    }
    // ---- bug review 2026-09-30 G142 end ----

    // ---- bug review 2026-09-30 G144 begin ----
    // G144: a supplied InputStream is decoded as UTF-8.
    @Test
    public void testValueOfObject_inputStreamDecodedAsUtf8() throws IOException {
        final java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream("h\u00e9".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        org.junit.jupiter.api.Assertions.assertEquals("h\u00e9",
                com.landawn.abacus.util.IOUtil.readAllToString(Type.of(StringReader.class).valueOf(stream)));
    }

    // G144: a Clob is returned as the handled reader class (it was always the internal owning wrapper): wrapped for a
    // BufferedReader (closing it releases the locator), copied for a StringReader/CharArrayReader (the locator released at
    // once), and rejected before it is opened for an unsupported class.
    @Test
    public void testValueOfObject_clobAsHandledClass() throws Exception {
        final Clob buffered = mock(Clob.class);
        when(buffered.getCharacterStream()).thenReturn(new StringReader("abc"));
        final Reader bufferedReader = Type.of(BufferedReader.class).valueOf(buffered);
        org.junit.jupiter.api.Assertions.assertTrue(bufferedReader instanceof BufferedReader);
        org.junit.jupiter.api.Assertions.assertEquals("abc", com.landawn.abacus.util.IOUtil.readAllToString(bufferedReader));
        verify(buffered, org.mockito.Mockito.never()).free();
        bufferedReader.close();
        verify(buffered).free();

        for (final Class<? extends Reader> copyClass : java.util.List.of(StringReader.class, CharArrayReader.class)) {
            final Clob copied = mock(Clob.class);
            when(copied.getCharacterStream()).thenReturn(new StringReader("xyz"));
            final Reader copy = Type.of(copyClass).valueOf(copied);
            org.junit.jupiter.api.Assertions.assertTrue(copyClass.isInstance(copy), copyClass.getName());
            verify(copied).free();
            org.junit.jupiter.api.Assertions.assertEquals("xyz", com.landawn.abacus.util.IOUtil.readAllToString(copy));
        }

        final Clob unsupported = mock(Clob.class);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileReader.class).valueOf(unsupported));
        verify(unsupported, org.mockito.Mockito.never()).getCharacterStream();
    }
    // ---- bug review 2026-09-30 G144 end ----

    // ---- bug review 2026-09-30 G145 begin ----
    // G145: more Clob shapes - PushbackReader (wrapped), FilterReader (the owning reader itself), a null reader, a copy whose read
    // fails (the locator is still released) and an unsupported class (nothing opened or released).
    @Test
    public void testValueOfObject_moreClobShapes() throws Exception {
        final Clob pushback = mock(Clob.class);
        when(pushback.getCharacterStream()).thenReturn(new StringReader("abc"));
        final Reader pushbackReader = Type.of(PushbackReader.class).valueOf(pushback);
        org.junit.jupiter.api.Assertions.assertTrue(pushbackReader instanceof PushbackReader);
        org.junit.jupiter.api.Assertions.assertEquals("abc", com.landawn.abacus.util.IOUtil.readAllToString(pushbackReader));
        pushbackReader.close();
        verify(pushback).free();

        final Clob filter = mock(Clob.class);
        when(filter.getCharacterStream()).thenReturn(new StringReader("abc"));
        org.junit.jupiter.api.Assertions.assertTrue(Type.of(java.io.FilterReader.class).valueOf(filter) instanceof java.io.FilterReader);

        final Clob nullReader = mock(Clob.class);
        org.junit.jupiter.api.Assertions.assertNull(Type.of(StringReader.class).valueOf(nullReader));
        verify(nullReader).free();

        final Clob failing = mock(Clob.class);
        when(failing.getCharacterStream()).thenReturn(new Reader() {
            @Override
            public int read(final char[] buffer, final int offset, final int length) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() {
            }
        });
        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> Type.of(StringReader.class).valueOf(failing));
        verify(failing).free();

        final Clob unsupported = mock(Clob.class);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileReader.class).valueOf(unsupported));
        verify(unsupported, org.mockito.Mockito.never()).free();
    }
    // ---- bug review 2026-09-30 G145 end ----

}
