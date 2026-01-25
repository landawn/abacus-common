package com.landawn.abacus.type;

import java.io.Reader;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class NClobReaderType100Test extends TestBase {

    private NClobReaderType nClobReaderType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;
    private NClob mockNClob;

    @BeforeEach
    public void setUp() {
        nClobReaderType = (NClobReaderType) createType("NClobReader");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
        mockNClob = Mockito.mock(NClob.class);
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Reader expectedReader = new StringReader("test content");
        Mockito.when(mockResultSet.getNClob(1)).thenReturn(mockNClob);
        Mockito.when(mockNClob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = nClobReaderType.get(mockResultSet, 1);
        Assertions.assertSame(expectedReader, result);
        // Mockito.verify(mockNClob).free();
    }

    @Test
    public void testGetByIndexNull() throws SQLException {
        Mockito.when(mockResultSet.getNClob(1)).thenReturn(null);

        Reader result = nClobReaderType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Reader expectedReader = new StringReader("test content");
        Mockito.when(mockResultSet.getNClob("nclobColumn")).thenReturn(mockNClob);
        Mockito.when(mockNClob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = nClobReaderType.get(mockResultSet, "nclobColumn");
        Assertions.assertSame(expectedReader, result);
        // Mockito.verify(mockNClob).free();
    }

    @Test
    public void testGetByLabelNull() throws SQLException {
        Mockito.when(mockResultSet.getNClob("nclobColumn")).thenReturn(null);

        Reader result = nClobReaderType.get(mockResultSet, "nclobColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        Reader reader = new StringReader("test data");
        nClobReaderType.set(mockPreparedStatement, 1, reader);
        Mockito.verify(mockPreparedStatement).setNClob(1, reader);
    }

    @Test
    public void testSetPreparedStatementNull() throws SQLException {
        nClobReaderType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setNClob(1, (Reader) null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        Reader reader = new StringReader("test data");
        nClobReaderType.set(mockCallableStatement, "param", reader);
        Mockito.verify(mockCallableStatement).setNClob("param", reader);
    }

    @Test
    public void testSetCallableStatementNull() throws SQLException {
        nClobReaderType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setNClob("param", (Reader) null);
    }

    @Test
    public void testSetPreparedStatementWithLength() throws SQLException {
        Reader reader = new StringReader("test data");
        int length = 100;
        nClobReaderType.set(mockPreparedStatement, 1, reader, length);
        Mockito.verify(mockPreparedStatement).setNClob(1, reader, length);
    }

    @Test
    public void testSetPreparedStatementWithLengthNull() throws SQLException {
        int length = 100;
        nClobReaderType.set(mockPreparedStatement, 1, null, length);
        Mockito.verify(mockPreparedStatement).setNClob(1, null, length);
    }

    @Test
    public void testSetCallableStatementWithLength() throws SQLException {
        Reader reader = new StringReader("test data");
        int length = 100;
        nClobReaderType.set(mockCallableStatement, "param", reader, length);
        Mockito.verify(mockCallableStatement).setNClob("param", reader, length);
    }

    @Test
    public void testSetCallableStatementWithLengthNull() throws SQLException {
        int length = 100;
        nClobReaderType.set(mockCallableStatement, "param", null, length);
        Mockito.verify(mockCallableStatement).setNClob("param", null, length);
    }

    @Test
    public void testClob2ReaderWithNull() throws SQLException {
        Reader result = NClobReaderType.clobToReader(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testClob2ReaderWithNonNull() throws SQLException {
        Reader expectedReader = new StringReader("test content");
        Mockito.when(mockNClob.getCharacterStream()).thenReturn(expectedReader);

        Reader result = NClobReaderType.clobToReader(mockNClob);
        Assertions.assertSame(expectedReader, result);
        // Mockito.verify(mockNClob).free();
    }
}
