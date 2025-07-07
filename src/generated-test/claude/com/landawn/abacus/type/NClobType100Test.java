package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

public class NClobType100Test extends TestBase {

    private NClobType nClobType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;
    private NClob mockNClob;

    @BeforeEach
    public void setUp() {
        nClobType = (NClobType) createType("NClob");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
        mockNClob = Mockito.mock(NClob.class);
    }

    @Test
    public void testClazz() {
        Class<NClob> clazz = nClobType.clazz();
        assertEquals(NClob.class, clazz);
    }

    @Test
    public void testStringOfThrowsException() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            nClobType.stringOf(mockNClob);
        });
    }

    @Test
    public void testValueOfThrowsException() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            nClobType.valueOf("test");
        });
    }

    @Test
    public void testGetByIndex() throws SQLException {
        Mockito.when(mockResultSet.getNClob(1)).thenReturn(mockNClob);

        NClob result = nClobType.get(mockResultSet, 1);
        Assertions.assertSame(mockNClob, result);
    }

    @Test
    public void testGetByIndexNull() throws SQLException {
        Mockito.when(mockResultSet.getNClob(1)).thenReturn(null);

        NClob result = nClobType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabel() throws SQLException {
        Mockito.when(mockResultSet.getNClob("nclobColumn")).thenReturn(mockNClob);

        NClob result = nClobType.get(mockResultSet, "nclobColumn");
        Assertions.assertSame(mockNClob, result);
    }

    @Test
    public void testGetByLabelNull() throws SQLException {
        Mockito.when(mockResultSet.getNClob("nclobColumn")).thenReturn(null);

        NClob result = nClobType.get(mockResultSet, "nclobColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        nClobType.set(mockPreparedStatement, 1, mockNClob);
        Mockito.verify(mockPreparedStatement).setNClob(1, mockNClob);
    }

    @Test
    public void testSetPreparedStatementNull() throws SQLException {
        nClobType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setNClob(1, (NClob) null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        nClobType.set(mockCallableStatement, "param", mockNClob);
        Mockito.verify(mockCallableStatement).setNClob("param", mockNClob);
    }

    @Test
    public void testSetCallableStatementNull() throws SQLException {
        nClobType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setNClob("param", (NClob) null);
    }
}
