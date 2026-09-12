package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;

public class InputStreamTypeTest extends TestBase {

    private InputStreamType inputStreamType;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JsonXmlSerConfig<?> config;

    @Mock
    private Blob blob;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        inputStreamType = (InputStreamType) createType(InputStream.class.getSimpleName());
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(InputStream.class, inputStreamType.javaType());
    }

    @Test
    public void testIsInputStream() {
        assertTrue(inputStreamType.isInputStream());
    }

    @Test
    public void testStringOf() {
        assertNull(inputStreamType.stringOf(null));

    }

    @Test
    public void testWellFormedUtf8RoundTrip() throws IOException {
        byte[] bytes = "h\u00e9llo".getBytes(StandardCharsets.UTF_8);

        String encoded = inputStreamType.stringOf(new ByteArrayInputStream(bytes));

        assertArrayEquals(bytes, inputStreamType.valueOf(encoded).readAllBytes());
    }

    @Test
    public void testValueOfString() {
        assertNull(inputStreamType.valueOf((String) null));

    }

    @Test
    public void testValueOfObject() throws SQLException, IOException {
        assertNull(inputStreamType.valueOf((Object) null));

        InputStream expectedStream = new ByteArrayInputStream(new byte[0]);
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = inputStreamType.valueOf(blob);
        assertNotNull(result);
        result.close();
        result.close();
        verify(blob).getBinaryStream();
        verify(blob, times(1)).free();
    }

    @Test
    public void testValueOfBlobFreesLocatorWhenOpeningFails() throws SQLException {
        final SQLException openFailure = new SQLException("open");
        final SQLException freeFailure = new SQLException("free");
        when(blob.getBinaryStream()).thenThrow(openFailure);
        doThrow(freeFailure).when(blob).free();

        final UncheckedSQLException thrown = assertThrows(UncheckedSQLException.class, () -> inputStreamType.valueOf(blob));

        assertSame(openFailure, thrown.getCause());
        assertSame(freeFailure, openFailure.getSuppressed()[0]);
        verify(blob).free();
    }

    @Test
    public void testValueOfBlobFreesLocatorWhenDelegateCloseFails() throws SQLException, IOException {
        final InputStream delegate = org.mockito.Mockito.mock(InputStream.class);
        final IOException closeFailure = new IOException("close");
        final SQLException freeFailure = new SQLException("free");
        when(blob.getBinaryStream()).thenReturn(delegate);
        doThrow(closeFailure).when(delegate).close();
        doThrow(freeFailure).when(blob).free();

        final InputStream result = inputStreamType.valueOf(blob);
        final IOException thrown = assertThrows(IOException.class, result::close);

        assertSame(closeFailure, thrown);
        assertSame(freeFailure, thrown.getSuppressed()[0]);
        verify(blob).free();
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        InputStream expectedStream = new ByteArrayInputStream(new byte[0]);
        when(resultSet.getBinaryStream(1)).thenReturn(expectedStream);

        InputStream result = inputStreamType.get(resultSet, 1);
        assertEquals(expectedStream, result);
        verify(resultSet).getBinaryStream(1);
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        InputStream expectedStream = new ByteArrayInputStream(new byte[0]);
        when(resultSet.getBinaryStream("streamColumn")).thenReturn(expectedStream);

        InputStream result = inputStreamType.get(resultSet, "streamColumn");
        assertEquals(expectedStream, result);
        verify(resultSet).getBinaryStream("streamColumn");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        InputStream stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.set(preparedStatement, 1, stream);
        verify(preparedStatement).setBinaryStream(1, stream);

        inputStreamType.set(preparedStatement, 2, null);
        verify(preparedStatement).setBinaryStream(2, null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        InputStream stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.set(callableStatement, "streamParam", stream);
        verify(callableStatement).setBinaryStream("streamParam", stream);

        inputStreamType.set(callableStatement, "nullParam", null);
        verify(callableStatement).setBinaryStream("nullParam", null);
    }

    @Test
    public void testSetPreparedStatementWithLength() throws SQLException {
        InputStream stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.set(preparedStatement, 1, stream, 100);
        verify(preparedStatement).setBinaryStream(1, stream, 100);
    }

    @Test
    public void testSetCallableStatementWithLength() throws SQLException {
        InputStream stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.set(callableStatement, "streamParam", stream, 200);
        verify(callableStatement).setBinaryStream("streamParam", stream, 200);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        inputStreamType.appendTo(writer, null);
        assertEquals("null", writer.toString());

    }

    @Test
    public void testSerializeTo() throws IOException {
        inputStreamType.serializeTo(characterWriter, null, null);

        InputStream stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.serializeTo(characterWriter, stream, null);

        when(config.getStringQuotation()).thenReturn('"');
        stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.serializeTo(characterWriter, stream, config);
        assertNotNull(stream);
    }

    // ---- review fixes 2026-09-06: T3-05 valueOf(Object) with a byte[] wraps the raw bytes ----

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayWrapsRawBytes() throws IOException {
        final byte[] bytes = { 1, 2, 3 };

        // raw bytes, not the array's list text "[1, 2, 3]"
        assertArrayEquals(bytes, inputStreamType.valueOf((Object) bytes).readAllBytes());
        assertArrayEquals(new byte[0], inputStreamType.valueOf((Object) new byte[0]).readAllBytes());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayHonoursTheHandledStreamClass() throws IOException {
        final byte[] bytes = { 1, 2, 3 };

        final InputStream bais = Type.of(ByteArrayInputStream.class).valueOf((Object) bytes);
        assertTrue(bais instanceof ByteArrayInputStream);
        assertArrayEquals(bytes, bais.readAllBytes());

        final InputStream buffered = Type.of(BufferedInputStream.class).valueOf((Object) bytes);
        assertTrue(buffered instanceof BufferedInputStream);
        assertArrayEquals(bytes, buffered.readAllBytes());

        final InputStream data = Type.of(DataInputStream.class).valueOf((Object) bytes);
        assertTrue(data instanceof DataInputStream);
        assertArrayEquals(bytes, data.readAllBytes());

        final InputStream pushback = Type.of(PushbackInputStream.class).valueOf((Object) bytes);
        assertTrue(pushback instanceof PushbackInputStream);
        assertArrayEquals(bytes, pushback.readAllBytes());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayUnsupportedStreamClassStillThrows() {
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf((Object) new byte[] { 1 }));
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf("x"));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayIsCharsetIndependentOnAsciiHandler() throws IOException {
        final byte[] nonAscii = { (byte) 0xC3, (byte) 0xA9, (byte) 0xFF, 0 };
        final Type<InputStream> ascii = createType(AsciiStreamType.ASCII_STREAM);

        assertArrayEquals(nonAscii, ascii.valueOf((Object) nonAscii).readAllBytes());
        assertArrayEquals(nonAscii, inputStreamType.valueOf((Object) nonAscii).readAllBytes());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectStringRouteUnchanged() throws IOException {
        assertArrayEquals("hé".getBytes(StandardCharsets.UTF_8), inputStreamType.valueOf((Object) "hé").readAllBytes());
        assertArrayEquals("7".getBytes(StandardCharsets.UTF_8), inputStreamType.valueOf((Object) 7).readAllBytes());
        assertNull(inputStreamType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260906_convertByteArrayToStreamSubclasses() throws IOException {
        final byte[] bytes = { 1, 2, 3 };

        assertArrayEquals(bytes, N.convert(bytes, InputStream.class).readAllBytes());
        assertArrayEquals(bytes, N.convert(bytes, ByteArrayInputStream.class).readAllBytes());
        assertArrayEquals(bytes, N.convert(bytes, BufferedInputStream.class).readAllBytes());
    }
}
