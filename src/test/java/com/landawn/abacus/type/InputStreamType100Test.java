package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class InputStreamType100Test extends TestBase {

    private InputStreamType inputStreamType;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JSONXMLSerializationConfig<?> config;

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
        assertEquals(InputStream.class, inputStreamType.clazz());
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
    public void testValueOfString() {
        assertNull(inputStreamType.valueOf((String) null));

    }

    @Test
    public void testValueOfObject() throws SQLException {
        assertNull(inputStreamType.valueOf((Object) null));

        InputStream expectedStream = new ByteArrayInputStream(new byte[0]);
        when(blob.getBinaryStream()).thenReturn(expectedStream);

        InputStream result = inputStreamType.valueOf((Object) blob);
        assertEquals(expectedStream, result);
        verify(blob).getBinaryStream();

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
    public void testWriteCharacter() throws IOException {
        inputStreamType.writeCharacter(characterWriter, null, null);

        InputStream stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.writeCharacter(characterWriter, stream, null);

        when(config.getStringQuotation()).thenReturn('"');
        stream = new ByteArrayInputStream(new byte[0]);
        inputStreamType.writeCharacter(characterWriter, stream, config);
    }
}
