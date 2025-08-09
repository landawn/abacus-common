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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class LocalDateType100Test extends TestBase {

    private LocalDateType localDateType;

    @BeforeEach
    public void setUp() {
        localDateType = (LocalDateType) createType("LocalDate");
    }

    @Test
    public void testClazz() {
        assertEquals(LocalDate.class, localDateType.clazz());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(localDateType.stringOf(null));
    }

    @Test
    public void testStringOf_ValidLocalDate() {
        LocalDate date = LocalDate.of(2023, 12, 25);
        String result = localDateType.stringOf(date);
        assertNotNull(result);
        assertEquals("2023-12-25", result);
    }

    @Test
    public void testValueOf_Object_Null() {
        assertNull(localDateType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_Number() {
        long millis = 1703502645123L;
        LocalDate result = localDateType.valueOf((Object) millis);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_Object_String() {
        String str = "2023-12-25";
        LocalDate result = localDateType.valueOf((Object) str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_String_Null() {
        assertNull(localDateType.valueOf((String) null));
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(localDateType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        LocalDate before = LocalDate.now();
        LocalDate result = localDateType.valueOf("sysTime");
        LocalDate after = LocalDate.now();

        assertNotNull(result);
        assertTrue(result.isAfter(before) || result.isEqual(before));
        assertTrue(result.isBefore(after) || result.isEqual(after));
    }

    @Test
    public void testValueOf_String_NumericString() {
        long millis = 1703502645123L;
        LocalDate result = localDateType.valueOf(String.valueOf(millis));
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_ISO8601() {
        String str = "2023-12-25";
        LocalDate result = localDateType.valueOf(str);
        assertNotNull(result);
        assertEquals(str, result.toString());
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(localDateType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(localDateType.valueOf(cbuf, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_StandardString() {
        String str = "2023-12-25";
        char[] cbuf = str.toCharArray();
        LocalDate result = localDateType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsLocalDate() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalDate expected = LocalDate.of(2023, 12, 25);
        when(rs.getObject(1, LocalDate.class)).thenReturn(expected);

        LocalDate result = localDateType.get(rs, 1);
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_AsDate() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Date date = Date.valueOf("2023-12-25");
        when(rs.getObject(1, LocalDate.class)).thenThrow(new SQLException());
        when(rs.getDate(1)).thenReturn(date);

        LocalDate result = localDateType.get(rs, 1);
        assertNotNull(result);
        assertEquals(date.toLocalDate(), result);
    }

    @Test
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(1, LocalDate.class)).thenThrow(new SQLException());
        when(rs.getDate(1)).thenReturn(null);

        assertNull(localDateType.get(rs, 1));
    }

    @Test
    public void testGet_ResultSet_ByName_AsLocalDate() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        LocalDate expected = LocalDate.of(2023, 12, 25);
        when(rs.getObject("date_column", LocalDate.class)).thenReturn(expected);

        LocalDate result = localDateType.get(rs, "date_column");
        assertEquals(expected, result);
    }

    @Test
    public void testGet_ResultSet_ByName_AsDate() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Date date = Date.valueOf("2023-12-25");
        when(rs.getObject("date_column", LocalDate.class)).thenThrow(new SQLException());
        when(rs.getDate("date_column")).thenReturn(date);

        LocalDate result = localDateType.get(rs, "date_column");
        assertNotNull(result);
        assertEquals(date.toLocalDate(), result);
    }

    @Test
    public void testSet_PreparedStatement_AsLocalDate() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalDate date = LocalDate.of(2023, 12, 25);

        localDateType.set(stmt, 1, date);
        verify(stmt).setObject(1, date);
    }

    @Test
    public void testSet_PreparedStatement_AsDate() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        LocalDate date = LocalDate.of(2023, 12, 25);
        doThrow(new SQLException()).when(stmt).setObject(1, date);

        localDateType.set(stmt, 1, date);
        verify(stmt).setDate(eq(1), any(Date.class));
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        localDateType.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSet_CallableStatement_AsLocalDate() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalDate date = LocalDate.of(2023, 12, 25);

        localDateType.set(stmt, "param_name", date);
        verify(stmt).setObject("param_name", date);
    }

    @Test
    public void testSet_CallableStatement_AsDate() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        LocalDate date = LocalDate.of(2023, 12, 25);
        doThrow(new SQLException()).when(stmt).setObject("param_name", date);

        localDateType.set(stmt, "param_name", date);
        verify(stmt).setDate(eq("param_name"), any(Date.class));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        localDateType.set(stmt, "param_name", null);
        verify(stmt).setObject("param_name", null);
    }
}
