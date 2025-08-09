package com.landawn.abacus.type;

import java.io.Reader;
import java.io.StringReader;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

public class NCharacterStreamType100Test extends TestBase {

    private NCharacterStreamType nCharacterStreamType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        nCharacterStreamType = (NCharacterStreamType) createType("NCharacterStream");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Reader expectedReader = new StringReader("test content");
        Mockito.when(mockResultSet.getNCharacterStream(1)).thenReturn(expectedReader);

        Reader result = nCharacterStreamType.get(mockResultSet, 1);
        Assertions.assertSame(expectedReader, result);
    }

    @Test
    public void testGetByIndexNull() throws SQLException {
        Mockito.when(mockResultSet.getNCharacterStream(1)).thenReturn(null);

        Reader result = nCharacterStreamType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Reader expectedReader = new StringReader("test content");
        Mockito.when(mockResultSet.getNCharacterStream("ncharColumn")).thenReturn(expectedReader);

        Reader result = nCharacterStreamType.get(mockResultSet, "ncharColumn");
        Assertions.assertSame(expectedReader, result);
    }

    @Test
    public void testGetByLabelNull() throws SQLException {
        Mockito.when(mockResultSet.getNCharacterStream("ncharColumn")).thenReturn(null);

        Reader result = nCharacterStreamType.get(mockResultSet, "ncharColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        Reader reader = new StringReader("test data");
        nCharacterStreamType.set(mockPreparedStatement, 1, reader);
        Mockito.verify(mockPreparedStatement).setNCharacterStream(1, reader);
    }

    @Test
    public void testSetPreparedStatementNull() throws SQLException {
        nCharacterStreamType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setNCharacterStream(1, null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        Reader reader = new StringReader("test data");
        nCharacterStreamType.set(mockCallableStatement, "param", reader);
        Mockito.verify(mockCallableStatement).setNCharacterStream("param", reader);
    }

    @Test
    public void testSetCallableStatementNull() throws SQLException {
        nCharacterStreamType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setNCharacterStream("param", null);
    }

    @Test
    public void testSetPreparedStatementWithLength() throws SQLException {
        Reader reader = new StringReader("test data");
        int length = 100;
        nCharacterStreamType.set(mockPreparedStatement, 1, reader, length);
        Mockito.verify(mockPreparedStatement).setNCharacterStream(1, reader, length);
    }

    @Test
    public void testSetPreparedStatementWithLengthNull() throws SQLException {
        int length = 100;
        nCharacterStreamType.set(mockPreparedStatement, 1, null, length);
        Mockito.verify(mockPreparedStatement).setNCharacterStream(1, null, length);
    }

    @Test
    public void testSetCallableStatementWithLength() throws SQLException {
        Reader reader = new StringReader("test data");
        int length = 100;
        nCharacterStreamType.set(mockCallableStatement, "param", reader, length);
        Mockito.verify(mockCallableStatement).setNCharacterStream("param", reader, length);
    }

    @Test
    public void testSetCallableStatementWithLengthNull() throws SQLException {
        int length = 100;
        nCharacterStreamType.set(mockCallableStatement, "param", null, length);
        Mockito.verify(mockCallableStatement).setNCharacterStream("param", null, length);
    }
}
