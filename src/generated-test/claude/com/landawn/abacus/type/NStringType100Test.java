package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

public class NStringType100Test extends TestBase {

    private NStringType nStringType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        nStringType = (NStringType) createType("NString");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testGetByIndex() throws SQLException {
        String expectedValue = "test unicode string 测试";
        Mockito.when(mockResultSet.getNString(1)).thenReturn(expectedValue);

        String result = nStringType.get(mockResultSet, 1);
        assertEquals(expectedValue, result);
    }

    @Test
    public void testGetByIndexNull() throws SQLException {
        Mockito.when(mockResultSet.getNString(1)).thenReturn(null);

        String result = nStringType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabel() throws SQLException {
        String expectedValue = "test unicode string 測試";
        Mockito.when(mockResultSet.getNString("nstringColumn")).thenReturn(expectedValue);

        String result = nStringType.get(mockResultSet, "nstringColumn");
        assertEquals(expectedValue, result);
    }

    @Test
    public void testGetByLabelNull() throws SQLException {
        Mockito.when(mockResultSet.getNString("nstringColumn")).thenReturn(null);

        String result = nStringType.get(mockResultSet, "nstringColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        String value = "unicode test 유니코드";
        nStringType.set(mockPreparedStatement, 1, value);
        Mockito.verify(mockPreparedStatement).setNString(1, value);
    }

    @Test
    public void testSetPreparedStatementNull() throws SQLException {
        nStringType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setNString(1, null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        String value = "unicode test текст";
        nStringType.set(mockCallableStatement, "param", value);
        Mockito.verify(mockCallableStatement).setNString("param", value);
    }

    @Test
    public void testSetCallableStatementNull() throws SQLException {
        nStringType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setNString("param", null);
    }
}
