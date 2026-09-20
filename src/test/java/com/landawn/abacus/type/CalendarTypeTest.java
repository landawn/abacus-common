package com.landawn.abacus.type;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class CalendarTypeTest extends TestBase {

    private final CalendarType type = new CalendarType();

    @Test
    public void testClazz() {
        Class<Calendar> result = type.javaType();
        assertEquals(Calendar.class, result);
    }

    @Test
    public void test_valueOf_String() {
        // Test with null
        Object result = type.valueOf((String) null);
        // Result may be null or default value depending on type
        assertNull(result);
    }

    @Test
    public void testValueOf_Object_Number() {
        long timeMillis = System.currentTimeMillis();
        Calendar result = type.valueOf(timeMillis);

        Assertions.assertNotNull(result);
        assertEquals(timeMillis, result.getTimeInMillis());
    }

    @Test
    public void testValueOf_Object_Date() {
        Date date = new Date();
        Calendar result = type.valueOf(date);

        Assertions.assertNotNull(result);
        assertEquals(date.getTime(), result.getTimeInMillis());
    }

    @Test
    public void testValueOf_Object_Calendar() {
        Calendar original = Calendar.getInstance();
        original.set(2023, Calendar.JANUARY, 15, 10, 30, 45);

        Calendar result = type.valueOf(original);

        Assertions.assertNotNull(result);
        Assertions.assertNotSame(original, result);
        assertEquals(original.getTimeInMillis(), result.getTimeInMillis());
    }

    @Test
    public void testValueOf_Object_String() {
        String dateString = "2023-06-15";
        Calendar result = type.valueOf((Object) dateString);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH));
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testValueOf_Object_Null() {
        Calendar result = type.valueOf((Object) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_String_Null() {
        Calendar result = type.valueOf((String) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_String_Empty() {
        Calendar result = type.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_String_SysTime() {
        long beforeTime = System.currentTimeMillis();
        Calendar result = type.valueOf("sysTime");
        long afterTime = System.currentTimeMillis();

        Assertions.assertNotNull(result);
        long resultTime = result.getTimeInMillis();
        Assertions.assertTrue(resultTime >= beforeTime);
        Assertions.assertTrue(resultTime <= afterTime);
    }

    @Test
    public void testValueOf_String_DateFormat() {
        String dateString = "2023-12-25 15:30:45";
        Calendar result = type.valueOf(dateString);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, result.get(Calendar.MONTH));
        assertEquals(25, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testValueOf_CharArray_Null() {
        Calendar result = type.valueOf(null, 0, 0);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] chars = new char[0];
        Calendar result = type.valueOf(chars, 0, 0);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_CharArray_Timestamp() {
        long timestamp = 1234567890123L;
        char[] chars = String.valueOf(timestamp).toCharArray();

        Calendar result = type.valueOf(chars, 0, chars.length);

        Assertions.assertNotNull(result);
        assertEquals(timestamp, result.getTimeInMillis());
    }

    @Test
    public void testValueOf_CharArray_DateString() {
        String dateString = "2023-06-15 10:20:30";
        char[] chars = dateString.toCharArray();

        Calendar result = type.valueOf(chars, 0, chars.length);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH));
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testValueOf_CharArray_PartialString() {
        String fullString = "prefix2023-06-15suffix";
        char[] chars = fullString.toCharArray();

        Calendar result = type.valueOf(chars, 6, 10);

        Assertions.assertNotNull(result);
        assertEquals(2023, result.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH));
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void testGet_ResultSet_Int() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        Calendar result = type.get(rs, 1);

        Assertions.assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(rs).getTimestamp(1);
    }

    @Test
    public void testGet_ResultSet_Int_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        Calendar result = type.get(rs, 1);

        Assertions.assertNull(result);
        verify(rs).getTimestamp(1);
    }

    @Test
    public void testGet_ResultSet_String() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1234567890000L);
        when(rs.getTimestamp("dateColumn")).thenReturn(timestamp);

        Calendar result = type.get(rs, "dateColumn");

        Assertions.assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(rs).getTimestamp("dateColumn");
    }

    @Test
    public void testGet_ResultSet_String_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("dateColumn")).thenReturn(null);

        Calendar result = type.get(rs, "dateColumn");

        Assertions.assertNull(result);
        verify(rs).getTimestamp("dateColumn");
    }

    @Test
    public void test_name() {
        assertNotNull(type.name());
        assertFalse(type.name().isEmpty());
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }

    // --- review fixes 2026-09-06 (T9-02, T9-03, T9-06, T9-09) ---

    @Test
    public void reviewFixes20260906_T902_T903_charArrayAgreesWithStringOverload() {
        for (final String s : new String[] { "1700000000000L", "1700000000000f", "12345L" }) {
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "99999999999999999999", "9223372036854775808", "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> type.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807" }) {
            final long expected = Long.parseLong(s);
            assertEquals(expected, type.valueOf(s).getTimeInMillis(), s);
            assertEquals(expected, type.valueOf(s.toCharArray(), 0, s.length()).getTimeInMillis(), s);
        }
    }

    @Test
    public void reviewFixes20260906_T906_textFormsRejectYearsOutsideCommonEra0001To9999() throws IOException {
        final Calendar max = Calendar.getInstance();
        max.setTimeInMillis(Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> type.stringOf(max));
        assertThrows(IllegalArgumentException.class, () -> type.appendTo(new StringBuilder(), max));

        // the check applies to the calendar's own zone: 0001-01-01T00:00Z is still year 0000 west of Greenwich
        final Calendar yearOneUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        yearOneUtc.setTimeInMillis(-62135596800000L);
        assertTrue(type.stringOf(yearOneUtc).startsWith("0001-01-01T00:00:00Z"));

        final Calendar yearOneWest = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        yearOneWest.setTimeInMillis(-62135596800000L);
        assertThrows(IllegalArgumentException.class, () -> type.stringOf(yearOneWest));

        // serializeTo: the default format measures the range in the calendar's own zone, ISO_8601_* in UTC,
        // and LONG writes any instant (the three cases the @throws now names)
        assertThrows(IllegalArgumentException.class, () -> type.serializeTo(Objectory.createBufferedJsonWriter(), yearOneWest, null));
        assertThrows(IllegalArgumentException.class,
                () -> type.serializeTo(Objectory.createBufferedJsonWriter(), max, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)));

        final BufferedJsonWriter isoWriter = Objectory.createBufferedJsonWriter();
        type.serializeTo(isoWriter, yearOneWest, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP));
        assertEquals("\"0001-01-01T00:00:00.000Z\"", isoWriter.toString());

        final BufferedJsonWriter longWriter = Objectory.createBufferedJsonWriter();
        type.serializeTo(longWriter, max, JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.LONG));
        assertEquals(String.valueOf(Long.MAX_VALUE), longWriter.toString());

        assertNull(type.stringOf(null));
    }

    @Test
    public void reviewFixes20260906_T909_calendarArgumentIsRebuiltAsGregorianAtSameInstantAndZone() {
        // a Buddhist calendar is NOT cloned: the result is a GregorianCalendar at the same instant and zone
        final Calendar buddhist = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok"), new Locale("th", "TH"));
        buddhist.setTimeInMillis(1700000000123L);
        assertEquals(2566, buddhist.get(Calendar.YEAR)); // Buddhist era year - precondition for the test

        final Calendar result = type.valueOf((Object) buddhist);
        assertNotSame(buddhist, result);
        assertEquals(GregorianCalendar.class, result.getClass());
        assertEquals(1700000000123L, result.getTimeInMillis());
        assertEquals("Asia/Bangkok", result.getTimeZone().getID());
        assertEquals(2023, result.get(Calendar.YEAR));
    }

}
