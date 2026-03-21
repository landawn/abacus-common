package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TimeTypeTest extends TestBase {

    private final TimeType type = new TimeType();

    @Test
    public void test_clazz() {
        assertEquals(Time.class, type.javaType());
    }

    @Test
    public void test_valueOf_Object_Number() {
        long millis = System.currentTimeMillis();
        Time result = type.valueOf(millis);
        assertEquals(millis, result.getTime());
    }

    @Test
    public void test_valueOf_Object_JavaUtilDate() {
        java.util.Date utilDate = new java.util.Date();
        Time result = type.valueOf(utilDate);
        assertEquals(utilDate.getTime(), result.getTime());
    }

    @Test
    public void test_valueOf_String_SysTime() {
        Time result = type.valueOf("sysTime");
        assertNotNull(result);
    }

    @Test
    public void test_valueOf_charArray() {
        long millis = System.currentTimeMillis();
        char[] chars = String.valueOf(millis).toCharArray();
        Time result = type.valueOf(chars, 0, chars.length);
        assertNotNull(result);

        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = new Time(System.currentTimeMillis());

        when(rs.getTime(1)).thenReturn(time);
        assertEquals(time, type.get(rs, 1));

        when(rs.getTime(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = new Time(System.currentTimeMillis());

        when(rs.getTime("timeCol")).thenReturn(time);
        assertEquals(time, type.get(rs, "timeCol"));

        when(rs.getTime("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Time time = new Time(System.currentTimeMillis());

        type.set(stmt, 1, time);
        verify(stmt).setTime(1, time);

        type.set(stmt, 2, null);
        verify(stmt).setTime(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Time time = new Time(System.currentTimeMillis());

        type.set(stmt, "param1", time);
        verify(stmt).setTime("param1", time);

        type.set(stmt, "param2", null);
        verify(stmt).setTime("param2", null);
    }

    @Test
    public void test_name() {
        assertEquals("Time", type.name());
    }
}
