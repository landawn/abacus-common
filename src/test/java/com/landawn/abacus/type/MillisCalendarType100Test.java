package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

public class MillisCalendarType100Test extends TestBase {

    private MillisCalendarType millisCalendarType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        millisCalendarType = (MillisCalendarType) createType("MillisCalendar");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testGetByIndexWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong(1)).thenReturn(0L);
        Calendar result = millisCalendarType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByIndexWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong(1)).thenReturn(testMillis);
        Calendar result = millisCalendarType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTimeInMillis());
    }

    @Test
    public void testGetByLabelWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong("testColumn")).thenReturn(0L);
        Calendar result = millisCalendarType.get(mockResultSet, "testColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabelWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong("testColumn")).thenReturn(testMillis);
        Calendar result = millisCalendarType.get(mockResultSet, "testColumn");
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTimeInMillis());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        millisCalendarType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setLong(1, 0L);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        Calendar calendar = Calendar.getInstance();
        long expectedMillis = calendar.getTimeInMillis();
        millisCalendarType.set(mockPreparedStatement, 1, calendar);
        Mockito.verify(mockPreparedStatement).setLong(1, expectedMillis);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        millisCalendarType.set(mockCallableStatement, "param", null);
        Mockito.verify(mockCallableStatement).setLong("param", 0L);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        Calendar calendar = Calendar.getInstance();
        long expectedMillis = calendar.getTimeInMillis();
        millisCalendarType.set(mockCallableStatement, "param", calendar);
        Mockito.verify(mockCallableStatement).setLong("param", expectedMillis);
    }
}
