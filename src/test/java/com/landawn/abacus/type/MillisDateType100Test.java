package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.CallableStatement;
import java.sql.Date;
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
public class MillisDateType100Test extends TestBase {

    private MillisDateType millisDateType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        millisDateType = (MillisDateType) createType("MillisDate");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testGetByIndexWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong(1)).thenReturn(0L);
        Date result = millisDateType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByIndexWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong(1)).thenReturn(testMillis);
        Date result = millisDateType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTime());
    }

    @Test
    public void testGetByLabelWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong("dateColumn")).thenReturn(0L);
        Date result = millisDateType.get(mockResultSet, "dateColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabelWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong("dateColumn")).thenReturn(testMillis);
        Date result = millisDateType.get(mockResultSet, "dateColumn");
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTime());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        millisDateType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setLong(1, 0L);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        Date date = new Date(System.currentTimeMillis());
        long expectedMillis = date.getTime();
        millisDateType.set(mockPreparedStatement, 1, date);
        Mockito.verify(mockPreparedStatement).setLong(1, expectedMillis);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        millisDateType.set(mockCallableStatement, "dateParam", null);
        Mockito.verify(mockCallableStatement).setLong("dateParam", 0L);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        Date date = new Date(System.currentTimeMillis());
        long expectedMillis = date.getTime();
        millisDateType.set(mockCallableStatement, "dateParam", date);
        Mockito.verify(mockCallableStatement).setLong("dateParam", expectedMillis);
    }
}
