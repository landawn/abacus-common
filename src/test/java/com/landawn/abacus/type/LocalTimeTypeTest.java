package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class LocalTimeTypeTest extends TestBase {

    private LocalTimeType localTimeType;

    @BeforeEach
    public void setUp() {
        localTimeType = (LocalTimeType) createType("LocalTime");
    }

    @Test
    public void testClazz() {
        assertEquals(LocalTime.class, localTimeType.javaType());
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
        LocalTime result = localTimeType.valueOf(millis);
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
    public void testValueOf_ParsesLocalTimeToString() {
        // Every form produced by LocalTime.toString() must round-trip through valueOf.
        LocalTime[] values = { //
                LocalTime.of(10, 30), // minute precision, "10:30"
                LocalTime.of(10, 30, 45), // seconds, "10:30:45"
                LocalTime.of(10, 30, 45, 123000000), // millis
                LocalTime.of(10, 30, 45, 123456789), // nanos
                LocalTime.MIDNIGHT, // "00:00"
                LocalTime.NOON, //
                LocalTime.now() };

        for (LocalTime value : values) {
            String text = value.toString();
            LocalTime result = localTimeType.valueOf(text);
            assertNotNull(result, () -> "Failed to parse: " + text);
            assertEquals(value, result, () -> "Mismatch for: " + text);
        }
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

    // --- review fixes 2026-09-06 (T10-02, T10-03, T10-04) ---

    @Test
    public void reviewFixes20260906_T1002_T1003_numericGrammarAndOverflow() {
        for (final String s : new String[] { "170000000000000000000", "9223372036854775808", "-9223372036854775809", "0x1F4A0", "1700000000000L",
                "12345L" }) {
            assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807", "12345" }) {
            final LocalTime expected = LocalTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneId.systemDefault());
            assertEquals(expected, localTimeType.valueOf(s), s);
            assertEquals(expected, localTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf("1234"));
        assertThrows(DateTimeParseException.class, () -> localTimeType.valueOf("0"));
    }

    @Test
    public void reviewFixes20260906_T1004_dateTimeFormatDoesNotApply() throws IOException {
        final LocalTime v = LocalTime.of(10, 30, 45, 123456789);

        for (final DateTimeFormat f : new DateTimeFormat[] { DateTimeFormat.LONG, DateTimeFormat.ISO_8601_DATE_TIME, DateTimeFormat.ISO_8601_TIMESTAMP }) {
            final BufferedJsonWriter w = Objectory.createBufferedJsonWriter();
            localTimeType.serializeTo(w, v, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("\"10:30:45.123456789\"", w.toString(), f.toString());

            final BufferedJsonWriter wn = Objectory.createBufferedJsonWriter();
            localTimeType.serializeTo(wn, null, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("null", wn.toString(), f.toString());
        }
    }

    // FINDING R05-3 (2026-09-08): r9506 made ZonedDateTimeType/OffsetDateTimeType read a Calendar in the calendar's
    // own zone, but this handler still rebuilt from getTimeInMillis() in the JVM default zone - so the displayed
    // fields, which is all a LocalTime is, silently shifted by the zone offset.
    @Test
    public void reviewFixes20260908_calendarKeepsItsOwnZone() {
        // GMT+14:00 and GMT-12:00 are 26 h apart, so whatever the JVM default zone is, at least one of them is on a
        // different calendar day from it - which is what makes this test fail against the default-zone rebuild.
        for (final String zoneName : new String[] { "Asia/Tokyo", "America/Los_Angeles", "UTC", "GMT+05:30", "GMT+14:00", "GMT-12:00" }) {
            final java.util.TimeZone tz = java.util.TimeZone.getTimeZone(zoneName);
            final java.util.GregorianCalendar cal = new java.util.GregorianCalendar(tz);
            cal.setTimeInMillis(1703502645123L);

            // GregorianCalendar.toZonedDateTime() is the JDK's own conversion, and it keeps the calendar's zone.
            assertEquals(cal.toZonedDateTime().toLocalTime(), localTimeType.valueOf(cal), zoneName);
            // ... and the java.time family now agrees with itself.
            assertEquals(createType(java.time.ZonedDateTime.class).valueOf(cal).toLocalTime(), localTimeType.valueOf(cal), zoneName);
        }
    }
}
