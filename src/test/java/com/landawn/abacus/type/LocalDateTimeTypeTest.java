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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class LocalDateTimeTypeTest extends TestBase {

    private LocalDateTimeType localDateTimeType;

    @BeforeEach
    public void setUp() {
        localDateTimeType = (LocalDateTimeType) createType("LocalDateTime");
    }

    @Test
    public void testClazz() {
        assertEquals(LocalDateTime.class, localDateTimeType.javaType());
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
    public void testValueOf_ParsesLocalDateTimeToString() {
        // Every form produced by LocalDateTime.toString() must round-trip through valueOf.
        LocalDateTime[] values = { //
                LocalDateTime.of(2021, 1, 1, 10, 30), // seconds omitted, "...T10:30"
                LocalDateTime.of(2021, 1, 1, 10, 30, 45), // seconds, "...T10:30:45"
                LocalDateTime.of(2021, 1, 1, 10, 30, 45, 123000000), // millis
                LocalDateTime.of(2021, 1, 1, 10, 30, 45, 123456789), // nanos
                LocalDateTime.of(2021, 1, 1, 0, 0), // midnight, seconds omitted
                LocalDateTime.now() };

        for (LocalDateTime value : values) {
            String text = value.toString();
            LocalDateTime result = localDateTimeType.valueOf(text);
            assertNotNull(result, () -> "Failed to parse: " + text);
            assertEquals(value, result, () -> "Mismatch for: " + text);
        }
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

    // --- review fixes 2026-09-06 (T10-02, T10-03, T10-04) ---

    @Test
    public void reviewFixes20260906_T1002_T1003_numericGrammarAndOverflow() {
        // overflow used to escape as ArithmeticException; hex / type suffix were accepted (Numbers.toLong grammar)
        for (final String s : new String[] { "170000000000000000000", "9223372036854775808", "-9223372036854775809", "0x1F4A0", "1700000000000L",
                "12345L" }) {
            assertThrows(DateTimeParseException.class, () -> localDateTimeType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> localDateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807", "12345" }) {
            final LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneId.systemDefault());
            assertEquals(expected, localDateTimeType.valueOf(s), s);
            assertEquals(expected, localDateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        // T10-03: numeric text is epoch millis only when longer than four characters
        assertThrows(DateTimeParseException.class, () -> localDateTimeType.valueOf("1234"));
        assertThrows(DateTimeParseException.class, () -> localDateTimeType.valueOf("0"));
    }

    @Test
    public void reviewFixes20260906_T1004_dateTimeFormatDoesNotApply() throws IOException {
        // documented: a LocalDateTime has no instant, serializeTo always writes the (quoted) ISO text
        final LocalDateTime v = LocalDateTime.of(2023, 10, 15, 10, 30, 45, 123456789);

        for (final DateTimeFormat f : new DateTimeFormat[] { DateTimeFormat.LONG, DateTimeFormat.ISO_8601_DATE_TIME, DateTimeFormat.ISO_8601_TIMESTAMP }) {
            final BufferedJsonWriter w = Objectory.createBufferedJsonWriter();
            localDateTimeType.serializeTo(w, v, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("\"2023-10-15T10:30:45.123456789\"", w.toString(), f.toString());

            final BufferedJsonWriter wn = Objectory.createBufferedJsonWriter();
            localDateTimeType.serializeTo(wn, null, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("null", wn.toString(), f.toString());
        }

        final BufferedJsonWriter w = Objectory.createBufferedJsonWriter();
        localDateTimeType.serializeTo(w, v, null);
        assertEquals("2023-10-15T10:30:45.123456789", w.toString());
    }

    // FINDING R05-3 (2026-09-08): r9506 made ZonedDateTimeType/OffsetDateTimeType read a Calendar in the calendar's
    // own zone, but this handler still rebuilt from getTimeInMillis() in the JVM default zone - so the displayed
    // fields, which is all a LocalDateTime is, silently shifted by the zone offset.
    @Test
    public void reviewFixes20260908_calendarKeepsItsOwnZone() {
        // GMT+14:00 and GMT-12:00 are 26 h apart, so whatever the JVM default zone is, at least one of them is on a
        // different calendar day from it - which is what makes this test fail against the default-zone rebuild.
        for (final String zoneName : new String[] { "Asia/Tokyo", "America/Los_Angeles", "UTC", "GMT+05:30", "GMT+14:00", "GMT-12:00" }) {
            final java.util.TimeZone tz = java.util.TimeZone.getTimeZone(zoneName);
            final java.util.GregorianCalendar cal = new java.util.GregorianCalendar(tz);
            cal.setTimeInMillis(1703502645123L);

            // GregorianCalendar.toZonedDateTime() is the JDK's own conversion, and it keeps the calendar's zone.
            assertEquals(cal.toZonedDateTime().toLocalDateTime(), localDateTimeType.valueOf(cal), zoneName);
            // ... and the java.time family now agrees with itself.
            assertEquals(createType(java.time.ZonedDateTime.class).valueOf(cal).toLocalDateTime(), localDateTimeType.valueOf(cal), zoneName);
        }
    }
}
