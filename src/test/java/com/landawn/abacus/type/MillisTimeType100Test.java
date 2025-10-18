package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MillisTimeType100Test extends TestBase {

    private MillisTimeType millisTimeType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        millisTimeType = (MillisTimeType) createType("MillisTime");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testGetByIndexWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong(1)).thenReturn(0L);
        Time result = millisTimeType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByIndexWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong(1)).thenReturn(testMillis);
        Time result = millisTimeType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTime());
    }

    @Test
    public void testGetByLabelWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong("timeColumn")).thenReturn(0L);
        Time result = millisTimeType.get(mockResultSet, "timeColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabelWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong("timeColumn")).thenReturn(testMillis);
        Time result = millisTimeType.get(mockResultSet, "timeColumn");
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTime());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        millisTimeType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setLong(1, 0L);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        Time time = new Time(System.currentTimeMillis());
        long expectedMillis = time.getTime();
        millisTimeType.set(mockPreparedStatement, 1, time);
        Mockito.verify(mockPreparedStatement).setLong(1, expectedMillis);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        millisTimeType.set(mockCallableStatement, "timeParam", null);
        Mockito.verify(mockCallableStatement).setLong("timeParam", 0L);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        Time time = new Time(System.currentTimeMillis());
        long expectedMillis = time.getTime();
        millisTimeType.set(mockCallableStatement, "timeParam", time);
        Mockito.verify(mockCallableStatement).setLong("timeParam", expectedMillis);
    }
}
