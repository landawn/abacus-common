package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TimestampTypeTest extends TestBase {

    private final TimestampType type = new TimestampType();

    @Test
    public void test_clazz() {
        assertEquals(Timestamp.class, type.javaType());
    }

    @Test
    public void test_valueOf_Object_Number() {
        long millis = System.currentTimeMillis();
        Timestamp result = type.valueOf(millis);
        assertEquals(millis, result.getTime());
    }

    @Test
    public void test_valueOf_Object_JavaUtilDate() {
        java.util.Date utilDate = new java.util.Date();
        Timestamp result = type.valueOf(utilDate);
        assertEquals(utilDate.getTime(), result.getTime());
    }

    @Test
    public void test_valueOf_Object_TimestampPreservesNanos() {
        Timestamp timestamp = Timestamp.valueOf("2020-01-01 00:00:00.123456789");
        Timestamp result = type.valueOf(timestamp);

        assertEquals(timestamp.getTime(), result.getTime());
        assertEquals(123456789, result.getNanos());
    }

    @Test
    public void test_stringOf_TruncatesToMillisecondsButValueOfAcceptsNanoseconds() {
        {
            Timestamp timestamp = Timestamp.valueOf("2020-01-01 00:00:00.123456789");
            String text = type.stringOf(timestamp);
            Timestamp result = type.valueOf(text);

            assertEquals("2020-01-01T08:00:00.123Z", text);
            assertEquals(timestamp.getTime(), result.getTime());
            assertEquals(123000000, result.getNanos());
        }

        {
            Timestamp timestamp = Timestamp.valueOf("2020-01-01 00:00:00.123456789");
            String text = "2020-01-01 00:00:00.123456789";
            Timestamp result = type.valueOf(text);

            assertEquals("2020-01-01 00:00:00.123456789", text);
            assertEquals(timestamp.getTime(), result.getTime());
            assertEquals(123456789, result.getNanos());
        }
    }

    @Test
    public void test_valueOf_String_SysTime() {
        Timestamp result = type.valueOf("sysTime");
        assertNotNull(result);
        assertTrue(Math.abs(System.currentTimeMillis() - result.getTime()) < 1000);
    }

    @Test
    public void test_valueOf_charArray() {
        long millis = System.currentTimeMillis();
        char[] chars = String.valueOf(millis).toCharArray();
        Timestamp result = type.valueOf(chars, 0, chars.length);
        assertNotNull(result);

        assertNull(type.valueOf((char[]) null, 0, 0));
        assertNull(type.valueOf(chars, 0, 0));
    }

    @Test
    public void test_get_ResultSet_byIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        when(rs.getTimestamp(1)).thenReturn(timestamp);
        assertEquals(timestamp, type.get(rs, 1));

        when(rs.getTimestamp(2)).thenReturn(null);
        assertNull(type.get(rs, 2));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        when(rs.getTimestamp("tsCol")).thenReturn(timestamp);
        assertEquals(timestamp, type.get(rs, "tsCol"));

        when(rs.getTimestamp("nullCol")).thenReturn(null);
        assertNull(type.get(rs, "nullCol"));
    }

    @Test
    public void test_set_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        type.set(stmt, 1, timestamp);
        verify(stmt).setTimestamp(1, timestamp);

        type.set(stmt, 2, null);
        verify(stmt).setTimestamp(2, null);
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        type.set(stmt, "param1", timestamp);
        verify(stmt).setTimestamp("param1", timestamp);

        type.set(stmt, "param2", null);
        verify(stmt).setTimestamp("param2", null);
    }

    @Test
    public void test_name() {
        assertEquals("Timestamp", type.name());
    }

    // --- review fixes 2026-09-06 (T9-02, T9-03) ---

    @Test
    public void reviewFixes20260906_T902_charArrayRejectsTypeSuffixLikeStringOverload() {
        // the char[] fast path used to strip a trailing l/L/f/F/d/D and parse "1700000000000d" as epoch millis
        // while valueOf(String) threw for the same text
        for (final String s : new String[] { "1700000000000d", "1700000000000L", "1700000000000f", "12345L" }) {
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000" }) {
            final long expected = Long.parseLong(s);
            assertEquals(expected, type.valueOf(s).getTime(), s);
            assertEquals(expected, type.valueOf(s.toCharArray(), 0, s.length()).getTime(), s);
        }

        // a digit-terminated region inside a larger buffer still takes the fast path
        final char[] padded = "xx1700000000000yy".toCharArray();
        assertEquals(1700000000000L, type.valueOf(padded, 2, 13).getTime());
    }

    @Test
    public void reviewFixes20260906_T903_charArrayOverflowIsIllegalArgument() {
        // used to escape as ArithmeticException("long overflow") on the char[] (JSON) path only
        for (final String s : new String[] { "99999999999999999999", "9223372036854775808", "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s.toCharArray(), 0, s.length()), s);
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s), s);
        }

        assertEquals(Long.MAX_VALUE, type.valueOf("9223372036854775807".toCharArray(), 0, 19).getTime());
        assertEquals(Long.MAX_VALUE, type.valueOf("9223372036854775807").getTime());
    }

    @Test
    public void testTimestampValueOfObject() {
        final Type<Timestamp> type = Type.of(Timestamp.class);
        assertEquals(1234L, type.valueOf((Object) new java.util.Date(1234L)).getTime());
        final String text = "2020-01-02 03:04:05";
        final Timestamp parsed = type.valueOf((Object) new StringBuilder(text));
        assertNotNull(parsed);
        assertEquals(type.valueOf(text), parsed);
        assertNull(type.valueOf((Object) null));
    }

    @Test
    public void testGregorianCalendarSysTime() {
        final Type<GregorianCalendar> type = Type.of(GregorianCalendar.class);
        final long before = System.currentTimeMillis();
        final GregorianCalendar cal = type.valueOf("SYS_TIME");
        assertNotNull(cal);
        assertTrue(cal.getTimeInMillis() >= before);
    }

    @Test
    public void testLocalTimeJdbcFallbacks() throws Exception {
        final Type<LocalTime> type = Type.of(LocalTime.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyString(), eq(LocalTime.class))).thenThrow(new SQLException("unsupported"));
        when(rs.getTime("c")).thenReturn(null);
        assertNull(type.get(rs, "c"));

        final PreparedStatement ps = mock(PreparedStatement.class);
        doThrow(new SQLException("unsupported")).when(ps).setObject(anyInt(), any());
        type.set(ps, 1, null);
        verify(ps).setTime(eq(1), isNull());

        final CallableStatement cs = mock(CallableStatement.class);
        doThrow(new SQLException("unsupported")).when(cs).setObject(anyString(), any());
        type.set(cs, "p", null);
        verify(cs).setTime(eq("p"), (Time) isNull());
    }

    @Test
    public void testLocalDateJdbcFallbacks() throws Exception {
        final Type<LocalDate> type = Type.of(LocalDate.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyString(), eq(LocalDate.class))).thenThrow(new SQLException("unsupported"));
        when(rs.getDate("c")).thenReturn(null);
        assertNull(type.get(rs, "c"));

        final PreparedStatement ps = mock(PreparedStatement.class);
        doThrow(new SQLException("unsupported")).when(ps).setObject(anyInt(), any());
        type.set(ps, 1, null);
        verify(ps).setDate(eq(1), isNull());

        final CallableStatement cs = mock(CallableStatement.class);
        doThrow(new SQLException("unsupported")).when(cs).setObject(anyString(), any());
        type.set(cs, "p", null);
        verify(cs).setDate(eq("p"), (Date) isNull());
    }

    @Test
    public void testLocalDateTimeJdbcFallbacks() throws Exception {
        final Type<LocalDateTime> type = Type.of(LocalDateTime.class);
        final ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyString(), eq(LocalDateTime.class))).thenThrow(new SQLException("unsupported"));
        when(rs.getTimestamp("c")).thenReturn(null);
        assertNull(type.get(rs, "c"));

        final PreparedStatement ps = mock(PreparedStatement.class);
        doThrow(new SQLException("unsupported")).when(ps).setObject(anyInt(), any());
        type.set(ps, 1, null);
        verify(ps).setTimestamp(eq(1), isNull());

        final CallableStatement cs = mock(CallableStatement.class);
        doThrow(new SQLException("unsupported")).when(cs).setObject(anyString(), any());
        type.set(cs, "p", null);
        verify(cs).setTimestamp(eq("p"), (Timestamp) isNull());
    }

    @Test
    public void testNamedConstructors() {
        assertEquals("MyJUDate", new JUDateType("MyJUDate").name());
        assertEquals("MyJodaMutable", new JodaMutableDateTimeType("MyJodaMutable").name());
        assertEquals(java.util.Date.class, new JUDateType("MyJUDate2").javaType());
    }
}
