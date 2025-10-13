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
import java.sql.Time;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class LocalTimeType100Test extends TestBase {

    private LocalTimeType localTimeType;

    @BeforeEach
    public void setUp() {
        localTimeType = (LocalTimeType) createType("LocalTime");
    }

    @Test
    public void testClazz() {
        assertEquals(LocalTime.class, localTimeType.clazz());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(localTimeType.stringOf(null));
    }

    @Test
    public void testStringOf_ValidLocalTime() {
        LocalTime time = LocalTime.of(10, 30, 45);
        String result = localTimeType.stringOf(time);
        assertNotNull(result);
        assertEquals("10:30:45", result);
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(localTimeType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        LocalTime result = localTimeType.valueOf((Object) millis);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "10:30:45";
        LocalTime result = localTimeType.valueOf((Object) str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(localTimeType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(localTimeType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        LocalTime before = LocalTime.now();
        LocalTime result = localTimeType.valueOf("sysTime");
        LocalTime after = LocalTime.now();

        assertNotNull(result);
        assertTrue(result.isAfter(before) || result.equals(before));
        assertTrue(result.isBefore(after) || result.equals(after));
    }

    @Test
    public void testValueOf_String_NumericString() {
        long millis = 1703502645123L;
        LocalTime result = localTimeType.valueOf(String.valueOf(millis));
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_ISO8601() {
        String str = "10:30:45";
        LocalTime result = localTimeType.valueOf(str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(localTimeType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(localTimeType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "10:30:45";
        char[] cbuf = str.toCharArray();
        LocalTime result = localTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsLocalTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalTime expected = LocalTime.of(10, 30, 45);
        when(rs.getObject(1, LocalTime.class)).thenReturn(expected);

        LocalTime result = localTimeType.get(rs, 1);
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = Time.valueOf("10:30:45");
        when(rs.getObject(1, LocalTime.class)).thenThrow(new SQLException());
        when(rs.getTime(1)).thenReturn(time);

        LocalTime result = localTimeType.get(rs, 1);
        assertNotNull(result);
        assertEquals(time.toLocalTime(), result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1, LocalTime.class)).thenThrow(new SQLException());
        when(rs.getTime(1)).thenReturn(null);

        assertNull(localTimeType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByName_AsLocalTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalTime expected = LocalTime.of(10, 30, 45);
        when(rs.getObject("time_column", LocalTime.class)).thenReturn(expected);

        LocalTime result = localTimeType.get(rs, "time_column");
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByName_AsTime() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = Time.valueOf("10:30:45");
        when(rs.getObject("time_column", LocalTime.class)).thenThrow(new SQLException());
        when(rs.getTime("time_column")).thenReturn(time);

        LocalTime result = localTimeType.get(rs, "time_column");
        assertNotNull(result);
        assertEquals(time.toLocalTime(), result);
    }

    @Test
    public void testSet_PreparedStatement_AsLocalTime() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);

        localTimeType.set(stmt, 1, time);
        verify(stmt).setObject(1, time);
    }

    @Test
    public void testSet_PreparedStatement_AsTime() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);
        doThrow(new SQLException()).when(stmt).setObject(1, time);

        localTimeType.set(stmt, 1, time);
        verify(stmt).setTime(eq(1), any(Time.class));
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        localTimeType.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSet_CallableStatement_AsLocalTime() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);

        localTimeType.set(stmt, "param_name", time);
        verify(stmt).setObject("param_name", time);
    }

    @Test
    public void testSet_CallableStatement_AsTime() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalTime time = LocalTime.of(10, 30, 45);
        doThrow(new SQLException()).when(stmt).setObject("param_name", time);

        localTimeType.set(stmt, "param_name", time);
        verify(stmt).setTime(eq("param_name"), any(Time.class));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        localTimeType.set(stmt, "param_name", null);
        verify(stmt).setObject("param_name", null);
    }
}
