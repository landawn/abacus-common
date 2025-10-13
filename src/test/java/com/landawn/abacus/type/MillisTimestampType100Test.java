package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mockito;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MillisTimestampType100Test extends TestBase {

    private MillisTimestampType millisTimestampType;
    private ResultSet mockResultSet;
    private PreparedStatement mockPreparedStatement;
    private CallableStatement mockCallableStatement;

    @BeforeEach
    public void setUp() {
        millisTimestampType = (MillisTimestampType) createType("MillisTimestamp");
        mockResultSet = Mockito.mock(ResultSet.class);
        mockPreparedStatement = Mockito.mock(PreparedStatement.class);
        mockCallableStatement = Mockito.mock(CallableStatement.class);
    }

    @Test
    public void testGetByIndexWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong(1)).thenReturn(0L);
        Timestamp result = millisTimestampType.get(mockResultSet, 1);
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByIndexWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong(1)).thenReturn(testMillis);
        Timestamp result = millisTimestampType.get(mockResultSet, 1);
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTime());
    }

    @Test
    public void testGetByLabelWithZeroValue() throws SQLException {
        Mockito.when(mockResultSet.getLong("timestampColumn")).thenReturn(0L);
        Timestamp result = millisTimestampType.get(mockResultSet, "timestampColumn");
        Assertions.assertNull(result);
    }

    @Test
    public void testGetByLabelWithNonZeroValue() throws SQLException {
        long testMillis = System.currentTimeMillis();
        Mockito.when(mockResultSet.getLong("timestampColumn")).thenReturn(testMillis);
        Timestamp result = millisTimestampType.get(mockResultSet, "timestampColumn");
        Assertions.assertNotNull(result);
        assertEquals(testMillis, result.getTime());
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        millisTimestampType.set(mockPreparedStatement, 1, null);
        Mockito.verify(mockPreparedStatement).setLong(1, 0L);
    }

    @Test
    public void testSetPreparedStatementWithNonNull() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long expectedMillis = timestamp.getTime();
        millisTimestampType.set(mockPreparedStatement, 1, timestamp);
        Mockito.verify(mockPreparedStatement).setLong(1, expectedMillis);
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        millisTimestampType.set(mockCallableStatement, "timestampParam", null);
        Mockito.verify(mockCallableStatement).setLong("timestampParam", 0L);
    }

    @Test
    public void testSetCallableStatementWithNonNull() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        long expectedMillis = timestamp.getTime();
        millisTimestampType.set(mockCallableStatement, "timestampParam", timestamp);
        Mockito.verify(mockCallableStatement).setLong("timestampParam", expectedMillis);
    }
}
