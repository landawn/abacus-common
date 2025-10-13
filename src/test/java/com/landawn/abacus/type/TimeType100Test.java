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
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class TimeType100Test extends TestBase {

    private TimeType timeType;

    @BeforeEach
    public void setUp() {
        timeType = (TimeType) createType("Time");
    }

    @Test
    public void testClazz() {
        assertEquals(Time.class, timeType.clazz());
    }

    @Test
    public void testValueOfObject() {
        Long time = 123456789L;
        Time result = timeType.valueOf(time);
        assertNotNull(result);
        assertEquals(time.longValue(), result.getTime());

        Date date = new Date();
        result = timeType.valueOf(date);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getTime());

        assertNull(timeType.valueOf((Object) null));

        result = timeType.valueOf((Object) "2024-01-15T14:30:45-08:00");
        assertNotNull(result);
    }

    @Test
    public void testValueOfString() {
        Time result = timeType.valueOf("2024-01-15T14:30:45-08:00");
        assertNotNull(result);

        result = timeType.valueOf("sysTime");
        assertNotNull(result);

        assertNull(timeType.valueOf((String) null));
        assertNull(timeType.valueOf(""));
    }

    @Test
    public void testValueOfCharArray() {
        char[] chars = "123456789".toCharArray();
        Time result = timeType.valueOf(chars, 0, chars.length);
        assertNotNull(result);
        assertEquals(123456789L, result.getTime());

        chars = "2024-01-15T14:30:45-08:00".toCharArray();
        result = timeType.valueOf(chars, 0, chars.length);
        assertNotNull(result);

        assertNull(timeType.valueOf(null, 0, 0));
        assertNull(timeType.valueOf(new char[0], 0, 0));
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = new Time(System.currentTimeMillis());
        when(rs.getTime(1)).thenReturn(time);

        assertEquals(time, timeType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Time time = new Time(System.currentTimeMillis());
        when(rs.getTime("column")).thenReturn(time);

        assertEquals(time, timeType.get(rs, "column"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Time time = new Time(System.currentTimeMillis());

        timeType.set(stmt, 1, time);
        verify(stmt).setTime(1, time);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Time time = new Time(System.currentTimeMillis());

        timeType.set(stmt, "param", time);
        verify(stmt).setTime("param", time);
    }
}
