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
        assertArrayEquals(bytes, inputStreamType.valueOf(bytes).readAllBytes());
        assertArrayEquals(new byte[0], inputStreamType.valueOf(new byte[0]).readAllBytes());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayHonoursTheHandledStreamClass() throws IOException {
        final byte[] bytes = { 1, 2, 3 };

        final InputStream bais = Type.of(ByteArrayInputStream.class).valueOf(bytes);
        assertTrue(bais instanceof ByteArrayInputStream);
        assertArrayEquals(bytes, bais.readAllBytes());

        final InputStream buffered = Type.of(BufferedInputStream.class).valueOf(bytes);
        assertTrue(buffered instanceof BufferedInputStream);
        assertArrayEquals(bytes, buffered.readAllBytes());

        final InputStream data = Type.of(DataInputStream.class).valueOf(bytes);
        assertTrue(data instanceof DataInputStream);
        assertArrayEquals(bytes, data.readAllBytes());

        final InputStream pushback = Type.of(PushbackInputStream.class).valueOf(bytes);
        assertTrue(pushback instanceof PushbackInputStream);
        assertArrayEquals(bytes, pushback.readAllBytes());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayUnsupportedStreamClassStillThrows() {
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(new byte[] { 1 }));
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf("x"));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayIsCharsetIndependentOnAsciiHandler() throws IOException {
        final byte[] nonAscii = { (byte) 0xC3, (byte) 0xA9, (byte) 0xFF, 0 };
        final Type<InputStream> ascii = createType(AsciiStreamType.ASCII_STREAM);

        assertArrayEquals(nonAscii, ascii.valueOf(nonAscii).readAllBytes());
        assertArrayEquals(nonAscii, inputStreamType.valueOf(nonAscii).readAllBytes());
    }

    @Test
    public void reviewFixes20260906_valueOfObjectStringRouteUnchanged() throws IOException {
        assertArrayEquals("hé".getBytes(StandardCharsets.UTF_8), inputStreamType.valueOf((Object) "hé").readAllBytes());
        assertArrayEquals("7".getBytes(StandardCharsets.UTF_8), inputStreamType.valueOf(7).readAllBytes());
        assertNull(inputStreamType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260906_convertByteArrayToStreamSubclasses() throws IOException {
        final byte[] bytes = { 1, 2, 3 };

        assertArrayEquals(bytes, N.convert(bytes, InputStream.class).readAllBytes());
        assertArrayEquals(bytes, N.convert(bytes, ByteArrayInputStream.class).readAllBytes());
        assertArrayEquals(bytes, N.convert(bytes, BufferedInputStream.class).readAllBytes());
    }

    // ---- bug review 2026-09-30 G121 begin ----
    // G121: an InputStream argument is returned as is - it was read as text, which replaced non-UTF-8 bytes and consumed it.
    @Test
    public void testValueOfObject_inputStreamIsReturnedAsIs() throws IOException {
        final byte[] bytes = { (byte) 0x80, (byte) 0xFF, 0x41 };
        final InputStream in = new ByteArrayInputStream(bytes);
        assertSame(in, inputStreamType.valueOf(in));
        assertArrayEquals(bytes, in.readAllBytes());

        // a subclass-specific type returns its own instances as is, too
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        assertSame(bais, com.landawn.abacus.type.Type.of(ByteArrayInputStream.class).valueOf(bais));

        // unchanged: byte[] and other objects
        assertArrayEquals(bytes, inputStreamType.valueOf(bytes).readAllBytes());
        assertArrayEquals("7".getBytes(StandardCharsets.UTF_8), inputStreamType.valueOf(7).readAllBytes());
    }
    // ---- bug review 2026-09-30 G121 end ----

    // ---- bug review 2026-09-30 verify2 G126 begin ----
    // A stream of another class is copied byte for byte into the handled class (it was decoded as UTF-8 text: 80 FF -> EF BF BD EF BF BD).
    @Test
    public void testValueOfObject_streamOfAnotherClassCopiesRawBytes() throws IOException {
        final byte[] bytes = { (byte) 0x80, (byte) 0xFF, 0x41 };

        final InputStream buffered = Type.of(BufferedInputStream.class).valueOf(new ByteArrayInputStream(bytes));
        assertTrue(buffered instanceof BufferedInputStream);
        assertArrayEquals(bytes, buffered.readAllBytes());

        final InputStream bais = Type.of(ByteArrayInputStream.class).valueOf(new BufferedInputStream(new ByteArrayInputStream(bytes)));
        assertTrue(bais instanceof ByteArrayInputStream);
        assertArrayEquals(bytes, bais.readAllBytes());

        final InputStream pushback = Type.of(PushbackInputStream.class).valueOf(new DataInputStream(new ByteArrayInputStream(bytes)));
        assertTrue(pushback instanceof PushbackInputStream);
        assertArrayEquals(bytes, pushback.readAllBytes());

        // unchanged: valid UTF-8 content, an empty stream, and an unsupported target class
        final byte[] utf8 = ("h" + (char) 0xE9).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(utf8, Type.of(DataInputStream.class).valueOf(new ByteArrayInputStream(utf8)).readAllBytes());
        assertArrayEquals(new byte[0], Type.of(BufferedInputStream.class).valueOf(new ByteArrayInputStream(new byte[0])).readAllBytes());
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(new ByteArrayInputStream(bytes)));
    }
    // ---- bug review 2026-09-30 verify2 G126 end ----

    // ---- bug review 2026-09-30 G135 begin ----
    /** Records close() so a test can check that the conversion leaves the caller's stream open. */
    private static final class G135CloseTrackingStream extends java.io.FilterInputStream {
        boolean closed;

        G135CloseTrackingStream(final byte[] bytes) {
            super(new ByteArrayInputStream(bytes));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    // G135: every stream-to-stream conversion keeps the raw bytes: malformed UTF-8, a partially consumed source (only the
    // remaining bytes), an empty source, the target class; the source is consumed but not closed, and a compatible stream is
    // returned untouched.
    @Test
    public void testValueOfObject_streamConversionPreservesBytesAndOwnership() throws IOException {
        final byte[] malformed = { (byte) 0xFF, (byte) 0x80, 0x41 };

        final G135CloseTrackingStream source = new G135CloseTrackingStream(malformed);
        final InputStream converted = Type.of(BufferedInputStream.class).valueOf(source);
        assertTrue(converted instanceof BufferedInputStream);
        assertArrayEquals(malformed, converted.readAllBytes());
        org.junit.jupiter.api.Assertions.assertFalse(source.closed);
        assertEquals(-1, source.read());

        final ByteArrayInputStream partial = new ByteArrayInputStream(malformed);
        assertEquals(0xFF, partial.read());
        final InputStream rest = Type.of(DataInputStream.class).valueOf(new java.io.BufferedInputStream(partial));
        assertTrue(rest instanceof DataInputStream);
        assertArrayEquals(new byte[] { (byte) 0x80, 0x41 }, rest.readAllBytes());

        final InputStream empty = Type.of(PushbackInputStream.class).valueOf(new ByteArrayInputStream(new byte[0]));
        assertTrue(empty instanceof PushbackInputStream);
        assertEquals(-1, empty.read());

        final InputStream generic = Type.of(InputStream.class).valueOf(new G135CloseTrackingStream(malformed));
        assertTrue(generic instanceof G135CloseTrackingStream);

        final ByteArrayInputStream compatible = new ByteArrayInputStream(malformed);
        assertSame(compatible, Type.of(ByteArrayInputStream.class).valueOf(compatible));
        assertEquals(3, compatible.available());
    }

    // G135: an unsupported target class is rejected before the supplied stream is read.
    @Test
    public void testValueOfObject_unsupportedTargetDoesNotConsumeSource() {
        final ByteArrayInputStream source = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(source));
        assertEquals(3, source.available());
    }
    // ---- bug review 2026-09-30 G135 end ----

    // ---- bug review 2026-09-30 G142 begin ----
    // G142: a Reader is not consumed by an unsupported target (it was read to the end, then rejected), and an unsupported target
    // gets one message whatever the content is supplied as (it was "Text construction" for byte[], "Byte construction" for a stream).
    @Test
    public void testValueOfObject_readerSourceAndOneMessageForUnsupportedTargets() throws IOException {
        final java.io.StringReader reader = new java.io.StringReader("abc");
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(reader));
        org.junit.jupiter.api.Assertions.assertEquals('a', reader.read());
        org.junit.jupiter.api.Assertions.assertArrayEquals("bc".getBytes(StandardCharsets.UTF_8),
                Type.of(ByteArrayInputStream.class).valueOf(reader).readAllBytes());

        final String fromBytes = org.junit.jupiter.api.Assertions
                .assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(new byte[] { 1 }))
                .getMessage();
        final String fromStream = org.junit.jupiter.api.Assertions
                .assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(new ByteArrayInputStream(new byte[] { 1 })))
                .getMessage();
        final String fromText = org.junit.jupiter.api.Assertions
                .assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf("x"))
                .getMessage();
        org.junit.jupiter.api.Assertions.assertEquals(fromBytes, fromStream);
        org.junit.jupiter.api.Assertions.assertEquals(fromBytes, fromText);
    }
    // ---- bug review 2026-09-30 G142 end ----

    // ---- bug review 2026-09-30 G144 begin ----
    // G144: a Blob is returned as the handled stream class (it was always the internal owning wrapper): wrapped for a
    // BufferedInputStream (closing it releases the locator), copied for a ByteArrayInputStream (the locator released at once),
    // and rejected before it is opened for an unsupported class.
    @Test
    public void testValueOfObject_blobAsHandledClass() throws Exception {
        final byte[] bytes = { 1, (byte) 0x80, 3 };

        final Blob buffered = org.mockito.Mockito.mock(Blob.class);
        when(buffered.getBinaryStream()).thenReturn(new ByteArrayInputStream(bytes));
        final InputStream bufferedStream = Type.of(BufferedInputStream.class).valueOf(buffered);
        org.junit.jupiter.api.Assertions.assertTrue(bufferedStream instanceof BufferedInputStream);
        org.junit.jupiter.api.Assertions.assertArrayEquals(bytes, bufferedStream.readAllBytes());
        verify(buffered, org.mockito.Mockito.never()).free();
        bufferedStream.close();
        verify(buffered).free();

        final Blob copied = org.mockito.Mockito.mock(Blob.class);
        when(copied.getBinaryStream()).thenReturn(new ByteArrayInputStream(bytes));
        final InputStream copy = Type.of(ByteArrayInputStream.class).valueOf(copied);
        org.junit.jupiter.api.Assertions.assertTrue(copy instanceof ByteArrayInputStream);
        verify(copied).free();
        org.junit.jupiter.api.Assertions.assertArrayEquals(bytes, copy.readAllBytes());

        final Blob unsupported = org.mockito.Mockito.mock(Blob.class);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(unsupported));
        verify(unsupported, org.mockito.Mockito.never()).getBinaryStream();

        // unchanged: the generic InputStream class gets the owning stream itself
        final Blob generic = org.mockito.Mockito.mock(Blob.class);
        when(generic.getBinaryStream()).thenReturn(new ByteArrayInputStream(bytes));
        final InputStream owning = Type.of(InputStream.class).valueOf(generic);
        org.junit.jupiter.api.Assertions.assertArrayEquals(bytes, owning.readAllBytes());
        owning.close();
        verify(generic).free();
    }
    // ---- bug review 2026-09-30 G144 end ----

    // ---- bug review 2026-09-30 G145 begin ----
    // G145: more Blob shapes - the other wrapping classes, FilterInputStream (the owning stream itself), a null stream, a copy
    // whose read fails (the locator is still released), an unsupported class (nothing opened or released) - and a Clob is not
    // read (and released) before an unsupported stream class is rejected.
    @Test
    public void testValueOfObject_moreBlobShapesAndClobForUnsupportedClass() throws Exception {
        final byte[] bytes = { 1, 2, 3 };

        for (final Class<? extends InputStream> wrapping : java.util.List.of(DataInputStream.class, PushbackInputStream.class)) {
            final Blob blob = org.mockito.Mockito.mock(Blob.class);
            when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(bytes));
            final InputStream stream = Type.of(wrapping).valueOf(blob);
            org.junit.jupiter.api.Assertions.assertTrue(wrapping.isInstance(stream), wrapping.getName());
            org.junit.jupiter.api.Assertions.assertArrayEquals(bytes, stream.readAllBytes());
            stream.close();
            verify(blob).free();
        }

        final Blob filter = org.mockito.Mockito.mock(Blob.class);
        when(filter.getBinaryStream()).thenReturn(new ByteArrayInputStream(bytes));
        org.junit.jupiter.api.Assertions.assertTrue(Type.of(java.io.FilterInputStream.class).valueOf(filter) instanceof java.io.FilterInputStream);

        final Blob nullStream = org.mockito.Mockito.mock(Blob.class);
        org.junit.jupiter.api.Assertions.assertNull(Type.of(ByteArrayInputStream.class).valueOf(nullStream));
        verify(nullStream).free();

        final Blob failing = org.mockito.Mockito.mock(Blob.class);
        when(failing.getBinaryStream()).thenReturn(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }
        });
        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> Type.of(ByteArrayInputStream.class).valueOf(failing));
        verify(failing).free();

        final Blob unsupported = org.mockito.Mockito.mock(Blob.class);
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(unsupported));
        verify(unsupported, org.mockito.Mockito.never()).free();

        // a real Clob: rejected before it is read and released (it was read, freed and then rejected); a supported class reads it
        final java.sql.Clob clob = new javax.sql.rowset.serial.SerialClob("abc".toCharArray());
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class, () -> Type.of(FileInputStream.class).valueOf(clob));
        org.junit.jupiter.api.Assertions.assertEquals("abc", clob.getSubString(1, 3));
        org.junit.jupiter.api.Assertions.assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8),
                Type.of(ByteArrayInputStream.class).valueOf(new javax.sql.rowset.serial.SerialClob("abc".toCharArray())).readAllBytes());

        // G147: a Clob class the type lookup reads as a bean (a Mockito mock: get/setMockitoInterceptor) is still read by the Clob
        // type handler, and released; it failed with ParsingException ("Unsupported class").
        final java.sql.Clob beanLikeClob = org.mockito.Mockito.mock(java.sql.Clob.class);
        when(beanLikeClob.length()).thenReturn(3L);
        when(beanLikeClob.getSubString(1, 3)).thenReturn("mck");
        org.junit.jupiter.api.Assertions.assertArrayEquals("mck".getBytes(StandardCharsets.UTF_8),
                Type.of(ByteArrayInputStream.class).valueOf(beanLikeClob).readAllBytes());
        verify(beanLikeClob).free();
    }
    // ---- bug review 2026-09-30 G145 end ----

}
