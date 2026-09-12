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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class LocalDateTypeTest extends TestBase {

    private LocalDateType localDateType;

    @BeforeEach
    public void setUp() {
        localDateType = (LocalDateType) createType("LocalDate");
    }

    @Test
    public void testClazz() {
        assertEquals(LocalDate.class, localDateType.javaType());
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
        LocalDate result = localDateType.valueOf(millis);
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
    public void testValueOf_ParsesLocalDateToString() {
        // Every form produced by LocalDate.toString() must round-trip through valueOf.
        LocalDate[] values = { //
                LocalDate.of(2021, 1, 1), //
                LocalDate.of(1000, 1, 1), //
                LocalDate.of(9999, 12, 31), //
                LocalDate.now() };

        for (LocalDate value : values) {
            String text = value.toString();
            LocalDate result = localDateType.valueOf(text);
            assertNotNull(result, () -> "Failed to parse: " + text);
            assertEquals(value, result, () -> "Mismatch for: " + text);
        }
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

    // --- review fixes 2026-09-06 (T10-02, T10-03, T10-04) ---

    @Test
    public void reviewFixes20260906_T1002_T1003_numericGrammarAndOverflow() {
        for (final String s : new String[] { "170000000000000000000", "9223372036854775808", "-9223372036854775809", "0x1F4A0", "1700000000000L",
                "12345L" }) {
            assertThrows(DateTimeParseException.class, () -> localDateType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> localDateType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807", "12345" }) {
            final LocalDate expected = LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneId.systemDefault());
            assertEquals(expected, localDateType.valueOf(s), s);
            assertEquals(expected, localDateType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        assertThrows(DateTimeParseException.class, () -> localDateType.valueOf("1234"));
        assertThrows(DateTimeParseException.class, () -> localDateType.valueOf("0"));
    }

    @Test
    public void reviewFixes20260906_T1004_dateTimeFormatDoesNotApply() throws IOException {
        final LocalDate v = LocalDate.of(2023, 10, 15);

        for (final DateTimeFormat f : new DateTimeFormat[] { DateTimeFormat.LONG, DateTimeFormat.ISO_8601_DATE_TIME, DateTimeFormat.ISO_8601_TIMESTAMP }) {
            final BufferedJsonWriter w = Objectory.createBufferedJsonWriter();
            localDateType.serializeTo(w, v, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("\"2023-10-15\"", w.toString(), f.toString());

            final BufferedJsonWriter wn = Objectory.createBufferedJsonWriter();
            localDateType.serializeTo(wn, null, JsonSerConfig.create().setDateTimeFormat(f));
            assertEquals("null", wn.toString(), f.toString());
        }
    }

    // FINDING R05-3 (2026-09-08): r9506 made ZonedDateTimeType/OffsetDateTimeType read a Calendar in the calendar's
    // own zone, but this handler still rebuilt from getTimeInMillis() in the JVM default zone - so the displayed
    // fields, which is all a LocalDate is, silently shifted by the zone offset.
    @Test
    public void reviewFixes20260908_calendarKeepsItsOwnZone() {
        // GMT+14:00 and GMT-12:00 are 26 h apart, so whatever the JVM default zone is, at least one of them is on a
        // different calendar day from it - which is what makes this test fail against the default-zone rebuild.
        for (final String zoneName : new String[] { "Asia/Tokyo", "America/Los_Angeles", "UTC", "GMT+05:30", "GMT+14:00", "GMT-12:00" }) {
            final java.util.TimeZone tz = java.util.TimeZone.getTimeZone(zoneName);
            final java.util.GregorianCalendar cal = new java.util.GregorianCalendar(tz);
            cal.setTimeInMillis(1703502645123L);

            // GregorianCalendar.toZonedDateTime() is the JDK's own conversion, and it keeps the calendar's zone.
            assertEquals(cal.toZonedDateTime().toLocalDate(), localDateType.valueOf(cal), zoneName);
            // ... and the java.time family now agrees with itself.
            assertEquals(createType(java.time.ZonedDateTime.class).valueOf(cal).toLocalDate(), localDateType.valueOf(cal), zoneName);
        }
    }
}
