package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DateTypeTest extends TestBase {

    private final DateType type = new DateType();

    @Test
    public void test_clazz() {
        assertEquals(Date.class, type.javaType());
    }

    @Test
    public void test_valueOf_Object_Number() {
        long millis = System.currentTimeMillis();
        Date result = type.valueOf(millis);
        assertEquals(millis, result.getTime());
    }

    @Test
    public void test_valueOf_Object_JavaUtilDate() {
        java.util.Date utilDate = new java.util.Date();
        Date result = type.valueOf(utilDate);
        assertEquals(utilDate.getTime(), result.getTime());
    }

    @Test
    public void test_valueOf_Object_Null() {
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void test_valueOf_String_Empty() {
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf(""));
    }

    @Test
    public void test_valueOf_String_SysTime() {
        Date result = type.valueOf("sysTime");
        assertNotNull(result);
        // Should be close to current time
        assertTrue(Math.abs(System.currentTimeMillis() - result.getTime()) < 1000);
    }

    @Test
    public void test_valueOf_String_DateString() {
        // Testing with ISO date format
        Date result = type.valueOf("2025-01-15");
        assertNotNull(result);
    }

    @Test
    public void test_valueOf_charArray() {
        // Test with timestamp
        long millis = System.currentTimeMillis();
        char[] chars = String.valueOf(millis).toCharArray();
        Date result = type.valueOf(chars, 0, chars.length);
        assertNotNull(result);

        // Test with null
        assertNull(type.valueOf((char[]) null, 0, 0));

        // Test with length 0
        assertNull(type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Date date = new Date(System.currentTimeMillis());

        // Test with date
        when(rs.getDate(1)).thenReturn(date);
        assertEquals(date, type.get(rs, 1));

        // Test with null
        when(rs.getDate(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Date date = new Date(System.currentTimeMillis());

        // Test with date
        when(rs.getDate("dateCol")).thenReturn(date);
        assertEquals(date, type.get(rs, "dateCol"));

        // Test with null
        when(rs.getDate("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Date date = new Date(System.currentTimeMillis());

        // Test with value
        type.set(stmt, 1, date);
        verify(stmt).setDate(1, date);

        // Test with null
        type.set(stmt, 2, null);
        verify(stmt).setDate(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Date date = new Date(System.currentTimeMillis());

        // Test with value
        type.set(stmt, "param1", date);
        verify(stmt).setDate("param1", date);

        // Test with null
        type.set(stmt, "param2", null);
        verify(stmt).setDate("param2", null);
    }

    @Test
    public void test_name() {
        assertEquals("Date", type.name());
    }

    @Test
    public void test_stringOf() {
        Date date = new Date(System.currentTimeMillis());
        String result = type.stringOf(date);
        assertNotNull(result);
        assertNull(type.stringOf(null));
    }

    @Test
    public void test_appendTo() throws Exception {
        StringWriter sw = new StringWriter();
        Date date = new Date(System.currentTimeMillis());

        // Test value
        type.appendTo(sw, date);
        assertFalse(sw.toString().isEmpty());

        // Test null
        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    // --- review fixes 2026-09-06 (T9-02, T9-03) ---

    @Test
    public void reviewFixes20260906_T902_T903_charArrayAgreesWithStringOverload() {
        // type suffix: rejected by both overloads (the char[] fast path used to strip it)
        for (final String s : new String[] { "1700000000000L", "1700000000000d", "12345L" }) {
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        // overflow: IllegalArgumentException on both overloads (the char[] path used to throw ArithmeticException)
        for (final String s : new String[] { "99999999999999999999", "9223372036854775808", "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807" }) {
            final long expected = Long.parseLong(s);
            assertEquals(expected, type.valueOf(s).getTime(), s);
            assertEquals(expected, type.valueOf(s.toCharArray(), 0, s.length()).getTime(), s);
        }

        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(new char[0], 0, 0));
    }
}
