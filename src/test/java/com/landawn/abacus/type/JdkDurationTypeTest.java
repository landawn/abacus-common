package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JdkDurationTypeTest extends TestBase {

    private final JdkDurationType type = new JdkDurationType();
    private final JdkDurationType durationType = type;

    @Test
    public void testClazz() {
        assertEquals(Duration.class, durationType.javaType());
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(durationType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOf_ValidDuration() {
        Duration duration = Duration.ofMillis(1000);
        assertEquals("1000", durationType.stringOf(duration));
    }

    @Test
    public void testStringOf_Null() {
        assertNull(durationType.stringOf(null));
    }

    @Test
    public void testStringOf_ZeroDuration() {
        Duration duration = Duration.ZERO;
        assertEquals("0", durationType.stringOf(duration));
    }

    @Test
    public void testStringOf_NegativeDuration() {
        Duration duration = Duration.ofMillis(-1000);
        assertEquals("-1000", durationType.stringOf(duration));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(durationType.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        assertNull(durationType.valueOf(""));
    }

    @Test
    public void testValueOf_ValidString() {
        Duration result = durationType.valueOf("1000");
        assertNotNull(result);
        assertEquals(1000, result.toMillis());
    }

    @Test
    public void testValueOf_ZeroString() {
        Duration result = durationType.valueOf("0");
        assertNotNull(result);
        assertEquals(0, result.toMillis());
    }

    @Test
    public void testValueOf_NegativeString() {
        Duration result = durationType.valueOf("-1000");
        assertNotNull(result);
        assertEquals(-1000, result.toMillis());
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(1000L);
        assertDoesNotThrow(() -> type.get(rs, 1));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("col")).thenReturn(1000L);
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(1000L);

        Duration result = durationType.get(rs, 1);
        assertNotNull(result);
        assertEquals(1000, result.toMillis());
    }

    @Test
    public void testGet_ResultSet_ByIndex_Zero() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong(1)).thenReturn(0L);

        Duration result = durationType.get(rs, 1);
        assertNotNull(result);
        assertEquals(0, result.toMillis());
    }

    @Test
    public void testGet_ResultSet_ByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getLong("duration_column")).thenReturn(1000L);

        Duration result = durationType.get(rs, "duration_column");
        assertNotNull(result);
        assertEquals(1000, result.toMillis());
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        assertDoesNotThrow(() -> type.set(stmt, 1, Duration.ofMillis(1000)));
        verify(stmt).setLong(1, 1000L);

        assertDoesNotThrow(() -> type.set(stmt, 2, null));
        verify(stmt).setNull(2, java.sql.Types.BIGINT);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        assertDoesNotThrow(() -> type.set(stmt, "param", Duration.ofMillis(1000)));
        verify(stmt).setLong("param", 1000L);

        assertDoesNotThrow(() -> type.set(stmt, "param2", null));
        verify(stmt).setNull("param2", java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Duration duration = Duration.ofMillis(1000);

        durationType.set(stmt, 1, duration);
        verify(stmt).setLong(1, 1000L);
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        durationType.set(stmt, 1, null);
        verify(stmt).setNull(1, java.sql.Types.BIGINT);
    }

    @Test
    public void testSet_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Duration duration = Duration.ofMillis(1000);

        durationType.set(stmt, "param_name", duration);
        verify(stmt).setLong("param_name", 1000L);
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        durationType.set(stmt, "param_name", null);
        verify(stmt).setNull("param_name", java.sql.Types.BIGINT);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();
        Duration duration = Duration.ofMillis(1000);

        durationType.appendTo(sb, duration);
        assertEquals("1000", sb.toString());
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();

        durationType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

}
