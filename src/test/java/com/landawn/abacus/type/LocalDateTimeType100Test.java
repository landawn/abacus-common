package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class LocalDateTimeType100Test extends TestBase {

    private LocalDateTimeType localDateTimeType;

    @BeforeEach
    public void setUp() {
        localDateTimeType = (LocalDateTimeType) createType("LocalDateTime");
    }

    @Test
    public void testClazz() {
        assertEquals(LocalDateTime.class, localDateTimeType.clazz());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(localDateTimeType.stringOf(null));
    }

    @Test
    public void testStringOf_ValidLocalDateTime() {
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 45);
        String result = localDateTimeType.stringOf(dateTime);
        assertNotNull(result);
        assertEquals("2023-12-25T10:30:45", result);
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(localDateTimeType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        LocalDateTime result = localDateTimeType.valueOf(millis);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "2023-12-25T10:30:45";
        LocalDateTime result = localDateTimeType.valueOf((Object) str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(localDateTimeType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(localDateTimeType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        LocalDateTime before = LocalDateTime.now();
        LocalDateTime result = localDateTimeType.valueOf("sysTime");
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result);
        assertTrue(result.isAfter(before) || result.isEqual(before));
        assertTrue(result.isBefore(after) || result.isEqual(after));
    }

    @Test
    public void testValueOf_String_NumericString() {
        long millis = 1703502645123L;
        LocalDateTime result = localDateTimeType.valueOf(String.valueOf(millis));
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_ISO8601() {
        String str = "2023-12-25T10:30:45";
        LocalDateTime result = localDateTimeType.valueOf(str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(localDateTimeType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(localDateTimeType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "2023-12-25T10:30:45";
        char[] cbuf = str.toCharArray();
        LocalDateTime result = localDateTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsLocalDateTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime expected = LocalDateTime.of(2023, 12, 25, 10, 30, 45);
        when(rs.getObject(1, LocalDateTime.class)).thenReturn(expected);

        LocalDateTime result = localDateTimeType.get(rs, 1);
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsTimestamp() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 10:30:45");
        when(rs.getObject(1, LocalDateTime.class)).thenThrow(new SQLException());
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        LocalDateTime result = localDateTimeType.get(rs, 1);
        assertNotNull(result);
        assertEquals(timestamp.toLocalDateTime(), result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1, LocalDateTime.class)).thenThrow(new SQLException());
        when(rs.getTimestamp(1)).thenReturn(null);

        assertNull(localDateTimeType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByName_AsLocalDateTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalDateTime expected = LocalDateTime.of(2023, 12, 25, 10, 30, 45);
        when(rs.getObject("datetime_column", LocalDateTime.class)).thenReturn(expected);

        LocalDateTime result = localDateTimeType.get(rs, "datetime_column");
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByName_AsTimestamp() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = Timestamp.valueOf("2023-12-25 10:30:45");
        when(rs.getObject("datetime_column", LocalDateTime.class)).thenThrow(new SQLException());
        when(rs.getTimestamp("datetime_column")).thenReturn(timestamp);

        LocalDateTime result = localDateTimeType.get(rs, "datetime_column");
        assertNotNull(result);
        assertEquals(timestamp.toLocalDateTime(), result);
    }

    @Test
    public void testSet_PreparedStatement_AsLocalDateTime() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 45);

        localDateTimeType.set(stmt, 1, dateTime);
        verify(stmt).setObject(1, dateTime);
    }

    @Test
    public void testSet_PreparedStatement_AsTimestamp() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 45);
        doThrow(new SQLException()).when(stmt).setObject(1, dateTime);

        localDateTimeType.set(stmt, 1, dateTime);
        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        localDateTimeType.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSet_CallableStatement_AsLocalDateTime() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 45);

        localDateTimeType.set(stmt, "param_name", dateTime);
        verify(stmt).setObject("param_name", dateTime);
    }

    @Test
    public void testSet_CallableStatement_AsTimestamp() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalDateTime dateTime = LocalDateTime.of(2023, 12, 25, 10, 30, 45);
        doThrow(new SQLException()).when(stmt).setObject("param_name", dateTime);

        localDateTimeType.set(stmt, "param_name", dateTime);
        verify(stmt).setTimestamp(eq("param_name"), any(Timestamp.class));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        localDateTimeType.set(stmt, "param_name", null);
        verify(stmt).setObject("param_name", null);
    }
}
