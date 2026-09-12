package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;

public class GregorianCalendarTypeTest extends TestBase {

    private GregorianCalendarType gregorianCalendarType;

    @Mock
    private ResultSet resultSet;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        gregorianCalendarType = (GregorianCalendarType) createType(GregorianCalendar.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(GregorianCalendar.class, gregorianCalendarType.javaType());
    }

    @Test
    public void testValueOfObject() {
        assertNull(gregorianCalendarType.valueOf((Object) null));

        long timestamp = System.currentTimeMillis();
        GregorianCalendar result = gregorianCalendarType.valueOf(timestamp);

        Date date = new Date();
        result = gregorianCalendarType.valueOf(date);

        Calendar calendar = Calendar.getInstance();
        result = gregorianCalendarType.valueOf(calendar);
    }

    @Test
    public void testValueOfString() {
        assertNull(gregorianCalendarType.valueOf((String) null));
        assertNull(gregorianCalendarType.valueOf(""));

    }

    @Test
    public void testValueOfStringHandlesNullLiteral() {
        // Bug: previously valueOf("null") tried to parse the literal string "null" as a date,
        // which threw an exception. Other Calendar/Date type handlers treat the "null" literal
        // (case-insensitive) as a null value via isNullDateTime; GregorianCalendarType should match.
        assertNull(gregorianCalendarType.valueOf("null"));
        assertNull(gregorianCalendarType.valueOf("NULL"));
        assertNull(gregorianCalendarType.valueOf("Null"));
    }

    @Test
    public void testValueOfCharArray() {
        assertNull(gregorianCalendarType.valueOf(null, 0, 0));

        char[] chars = new char[10];
        assertNull(gregorianCalendarType.valueOf(chars, 0, 0));

        String timestampStr = "1234567890123";
        char[] timestampChars = timestampStr.toCharArray();
        GregorianCalendar result = gregorianCalendarType.valueOf(timestampChars, 0, timestampChars.length);
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(resultSet.getTimestamp(1)).thenReturn(timestamp);

        GregorianCalendar result = gregorianCalendarType.get(resultSet, 1);
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(resultSet).getTimestamp(1);

        when(resultSet.getTimestamp(2)).thenReturn(null);
        assertNull(gregorianCalendarType.get(resultSet, 2));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(resultSet.getTimestamp("dateColumn")).thenReturn(timestamp);

        GregorianCalendar result = gregorianCalendarType.get(resultSet, "dateColumn");
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getTimeInMillis());
        verify(resultSet).getTimestamp("dateColumn");

        when(resultSet.getTimestamp("nullColumn")).thenReturn(null);
        assertNull(gregorianCalendarType.get(resultSet, "nullColumn"));
    }

    // --- review fixes 2026-09-06 (T9-02, T9-03): the char[] fast path is a sibling of the ones pinned in
    // DateTypeTest / CalendarTypeTest / TimestampTypeTest and carries the identical guard.

    @Test
    public void reviewFixes20260906_T902_T903_charArrayAgreesWithStringOverload() {
        // a trailing type suffix used to be stripped by parseLong(char[]) only, so the two overloads disagreed
        for (final String s : new String[] { "1700000000000L", "1700000000000d", "12345L" }) {
            assertThrows(IllegalArgumentException.class, () -> gregorianCalendarType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> gregorianCalendarType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        // overflowing text used to escape the char[] path as ArithmeticException("long overflow")
        for (final String s : new String[] { "99999999999999999999", "9223372036854775808", "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> gregorianCalendarType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> gregorianCalendarType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000" }) {
            assertEquals(Long.parseLong(s), gregorianCalendarType.valueOf(s.toCharArray(), 0, s.length()).getTimeInMillis(), s);
            assertEquals(Long.parseLong(s), gregorianCalendarType.valueOf(s).getTimeInMillis(), s);
        }

        assertNull(gregorianCalendarType.valueOf((char[]) null, 0, 0));
        assertNull(gregorianCalendarType.valueOf(new char[0], 0, 0));
    }
}
