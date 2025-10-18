package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Dates2025Test extends TestBase {

    @Test
    public void test_registerDateCreator() {
        boolean result = Dates.registerDateCreator(java.util.Date.class, (millis) -> new java.util.Date(millis));
        assertFalse(result);

        assertNotNull(Dates.registerDateCreator(java.util.Date.class, java.util.Date::new));
    }

    @Test
    public void test_registerCalendarCreator() {
        boolean result = Dates.registerCalendarCreator(Calendar.class, (millis, c) -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            return cal;
        });
        assertFalse(result);

        assertNotNull(Dates.registerCalendarCreator(Calendar.class, (millis, c) -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            return cal;
        }));
    }

    @Test
    public void test_currentTimeMillis() {
        long now = Dates.currentTimeMillis();
        assertTrue(now > 0);
        assertTrue(now <= System.currentTimeMillis());
    }

    @Test
    public void test_currentTime() {
        Time time = Dates.currentTime();
        assertNotNull(time);
        assertTrue(time.getTime() > 0);
    }

    @Test
    public void test_currentDate() {
        java.sql.Date date = Dates.currentDate();
        assertNotNull(date);
        assertTrue(date.getTime() > 0);
    }

    @Test
    public void test_currentTimestamp() {
        Timestamp timestamp = Dates.currentTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.getTime() > 0);
    }

    @Test
    public void test_currentJUDate() {
        java.util.Date date = Dates.currentJUDate();
        assertNotNull(date);
        assertTrue(date.getTime() > 0);
    }

    @Test
    public void test_currentCalendar() {
        Calendar cal = Dates.currentCalendar();
        assertNotNull(cal);
        assertTrue(cal.getTimeInMillis() > 0);
    }

    @Test
    public void test_currentGregorianCalendar() {
        GregorianCalendar cal = Dates.currentGregorianCalendar();
        assertNotNull(cal);
        assertTrue(cal.getTimeInMillis() > 0);
    }

    @Test
    public void test_currentXMLGregorianCalendar() {
        XMLGregorianCalendar cal = Dates.currentXMLGregorianCalendar();
        assertNotNull(cal);
    }

    @Test
    public void test_currentTimeRolled() {
        Time futureTime = Dates.currentTimeRolled(5, TimeUnit.MINUTES);
        Time currentTime = Dates.currentTime();
        assertTrue(futureTime.getTime() > currentTime.getTime());

        Time pastTime = Dates.currentTimeRolled(-5, TimeUnit.MINUTES);
        assertTrue(pastTime.getTime() < currentTime.getTime());
    }

    @Test
    public void test_currentDateRolled() {
        java.sql.Date futureDate = Dates.currentDateRolled(1, TimeUnit.DAYS);
        java.sql.Date currentDate = Dates.currentDate();
        assertTrue(futureDate.getTime() > currentDate.getTime());

        java.sql.Date pastDate = Dates.currentDateRolled(-1, TimeUnit.DAYS);
        assertTrue(pastDate.getTime() < currentDate.getTime());
    }

    @Test
    public void test_currentTimestampRolled() {
        Timestamp futureTs = Dates.currentTimestampRolled(10, TimeUnit.SECONDS);
        Timestamp currentTs = Dates.currentTimestamp();
        assertTrue(futureTs.getTime() > currentTs.getTime());

        Timestamp pastTs = Dates.currentTimestampRolled(-10, TimeUnit.SECONDS);
        assertTrue(pastTs.getTime() < currentTs.getTime());
    }

    @Test
    public void test_currentJUDateRolled() {
        java.util.Date futureDate = Dates.currentJUDateRolled(2, TimeUnit.HOURS);
        java.util.Date currentDate = Dates.currentJUDate();
        assertTrue(futureDate.getTime() > currentDate.getTime());

        java.util.Date pastDate = Dates.currentJUDateRolled(-2, TimeUnit.HOURS);
        assertTrue(pastDate.getTime() < currentDate.getTime());
    }

    @Test
    public void test_currentCalendarRolled() {
        Calendar futureCal = Dates.currentCalendarRolled(3, TimeUnit.HOURS);
        Calendar currentCal = Dates.currentCalendar();
        assertTrue(futureCal.getTimeInMillis() > currentCal.getTimeInMillis());

        Calendar pastCal = Dates.currentCalendarRolled(-3, TimeUnit.HOURS);
        assertTrue(pastCal.getTimeInMillis() < currentCal.getTimeInMillis());
    }

    @Test
    public void test_createJUDate_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        java.util.Date date = Dates.createJUDate(cal);
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((Calendar) null));
    }

    @Test
    public void test_createJUDate_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        java.util.Date created = Dates.createJUDate(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((java.util.Date) null));
    }

    @Test
    public void test_createJUDate_fromMillis() {
        long millis = 3000000000L;
        java.util.Date date = Dates.createJUDate(millis);

        assertNotNull(date);
        assertEquals(millis, date.getTime());
    }

    @Test
    public void test_createJUDate_zeroMillis() {
        java.util.Date date = Dates.createJUDate(0L);
        assertNotNull(date);
        assertEquals(0L, date.getTime());
    }

    @Test
    public void test_createDate_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        java.sql.Date date = Dates.createDate(cal);
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((Calendar) null));
    }

    @Test
    public void test_createDate_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        java.sql.Date created = Dates.createDate(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((java.util.Date) null));
    }

    @Test
    public void test_createDate_fromMillis() {
        long millis = 3000000000L;
        java.sql.Date date = Dates.createDate(millis);

        assertNotNull(date);
        assertEquals(millis, date.getTime());
    }

    @Test
    public void test_createTime_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        Time time = Dates.createTime(cal);
        assertNotNull(time);
        assertEquals(1000000000L, time.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((Calendar) null));
    }

    @Test
    public void test_createTime_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        Time created = Dates.createTime(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((java.util.Date) null));
    }

    @Test
    public void test_createTime_fromMillis() {
        long millis = 3000000000L;
        Time time = Dates.createTime(millis);

        assertNotNull(time);
        assertEquals(millis, time.getTime());
    }

    @Test
    public void test_createTimestamp_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        Timestamp timestamp = Dates.createTimestamp(cal);
        assertNotNull(timestamp);
        assertEquals(1000000000L, timestamp.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((Calendar) null));
    }

    @Test
    public void test_createTimestamp_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        Timestamp created = Dates.createTimestamp(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((java.util.Date) null));
    }

    @Test
    public void test_createTimestamp_fromMillis() {
        long millis = 3000000000L;
        Timestamp timestamp = Dates.createTimestamp(millis);

        assertNotNull(timestamp);
        assertEquals(millis, timestamp.getTime());
    }

    @Test
    public void test_createCalendar_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        Calendar created = Dates.createCalendar(cal);
        assertNotNull(created);
        assertEquals(1000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((Calendar) null));
    }

    @Test
    public void test_createCalendar_fromDate() {
        java.util.Date date = new java.util.Date(2000000000L);
        Calendar created = Dates.createCalendar(date);

        assertNotNull(created);
        assertEquals(2000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((java.util.Date) null));
    }

    @Test
    public void test_createCalendar_fromMillis() {
        long millis = 3000000000L;
        Calendar cal = Dates.createCalendar(millis);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void test_createCalendar_fromMillisWithTimeZone() {
        long millis = 3000000000L;
        TimeZone tz = TimeZone.getTimeZone("America/New_York");

        Calendar cal = Dates.createCalendar(millis, tz);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(tz, cal.getTimeZone());
    }

    @Test
    public void test_createCalendar_fromMillisWithNullTimeZone() {
        long millis = 3000000000L;
        Calendar cal = Dates.createCalendar(millis, null);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void test_createGregorianCalendar_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        GregorianCalendar created = Dates.createGregorianCalendar(cal);
        assertNotNull(created);
        assertEquals(1000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createGregorianCalendar((Calendar) null));
    }

    @Test
    public void test_createGregorianCalendar_fromDate() {
        java.util.Date date = new java.util.Date(2000000000L);
        GregorianCalendar created = Dates.createGregorianCalendar(date);

        assertNotNull(created);
        assertEquals(2000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createGregorianCalendar((java.util.Date) null));
    }

    @Test
    public void test_createGregorianCalendar_fromMillis() {
        long millis = 3000000000L;
        GregorianCalendar cal = Dates.createGregorianCalendar(millis);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void test_createGregorianCalendar_fromMillisWithTimeZone() {
        long millis = 3000000000L;
        TimeZone tz = TimeZone.getTimeZone("Europe/London");

        GregorianCalendar cal = Dates.createGregorianCalendar(millis, tz);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(tz, cal.getTimeZone());
    }

    @Test
    public void test_createGregorianCalendar_fromMillisWithNullTimeZone() {
        long millis = 3000000000L;
        GregorianCalendar cal = Dates.createGregorianCalendar(millis, null);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void test_createXMLGregorianCalendar_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(cal);
        assertNotNull(created);

        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar((Calendar) null));
    }

    @Test
    public void test_createXMLGregorianCalendar_fromDate() {
        java.util.Date date = new java.util.Date(2000000000L);
        XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(date);

        assertNotNull(created);

        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar((java.util.Date) null));
    }

    @Test
    public void test_createXMLGregorianCalendar_fromMillis() {
        long millis = 3000000000L;
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis);

        assertNotNull(cal);
    }

    @Test
    public void test_createXMLGregorianCalendar_fromMillisWithTimeZone() {
        long millis = 3000000000L;
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis, tz);
        assertNotNull(cal);
    }

    @Test
    public void test_createXMLGregorianCalendar_fromMillisWithNullTimeZone() {
        long millis = 3000000000L;
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis, null);

        assertNotNull(cal);
    }

    @Test
    public void test_parseJUDate_singleParam() {
        assertNull(Dates.parseJUDate((String) null));
        assertNull(Dates.parseJUDate(""));
        assertNull(Dates.parseJUDate("null"));

        java.util.Date date = Dates.parseJUDate("1000000000");
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());
    }

    @Test
    public void test_parseJUDate_withFormat() {
        assertNull(Dates.parseJUDate(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseJUDate("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseJUDate("null", Dates.LOCAL_DATE_FORMAT));

        java.util.Date date = Dates.parseJUDate("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void test_parseJUDate_withFormatAndTimeZone() {
        assertNull(Dates.parseJUDate(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseJUDate("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseJUDate("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        java.util.Date date = Dates.parseJUDate("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    @Test
    public void test_parseDate_singleParam() {
        assertNull(Dates.parseDate((String) null));
        assertNull(Dates.parseDate(""));
        assertNull(Dates.parseDate("null"));

        java.sql.Date date = Dates.parseDate("1000000000");
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());
    }

    @Test
    public void test_parseDate_withFormat() {
        assertNull(Dates.parseDate(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseDate("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseDate("null", Dates.LOCAL_DATE_FORMAT));

        java.sql.Date date = Dates.parseDate("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void test_parseDate_withFormatAndTimeZone() {
        assertNull(Dates.parseDate(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseDate("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseDate("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        java.sql.Date date = Dates.parseDate("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    @Test
    public void test_parseTime_singleParam() {
        assertNull(Dates.parseTime((String) null));
        assertNull(Dates.parseTime(""));
        assertNull(Dates.parseTime("null"));

        Time time = Dates.parseTime("1000000000");
        assertNotNull(time);
        assertEquals(1000000000L, time.getTime());
    }

    @Test
    public void test_parseTime_withFormat() {
        assertNull(Dates.parseTime(null, Dates.LOCAL_TIME_FORMAT));
        assertNull(Dates.parseTime("", Dates.LOCAL_TIME_FORMAT));
        assertNull(Dates.parseTime("null", Dates.LOCAL_TIME_FORMAT));

        Time time = Dates.parseTime("14:30:45", Dates.LOCAL_TIME_FORMAT);
        assertNotNull(time);
    }

    @Test
    public void test_parseTime_withFormatAndTimeZone() {
        assertNull(Dates.parseTime(null, Dates.LOCAL_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTime("", Dates.LOCAL_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTime("null", Dates.LOCAL_TIME_FORMAT, TimeZone.getDefault()));

        Time time = Dates.parseTime("14:30:45", Dates.LOCAL_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(time);
    }

    @Test
    public void test_parseTimestamp_singleParam() {
        assertNull(Dates.parseTimestamp((String) null));
        assertNull(Dates.parseTimestamp(""));
        assertNull(Dates.parseTimestamp("null"));

        Timestamp timestamp = Dates.parseTimestamp("1000000000");
        assertNotNull(timestamp);
        assertEquals(1000000000L, timestamp.getTime());
    }

    @Test
    public void test_parseTimestamp_withFormat() {
        assertNull(Dates.parseTimestamp(null, Dates.LOCAL_DATE_TIME_FORMAT));
        assertNull(Dates.parseTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT));
        assertNull(Dates.parseTimestamp("null", Dates.LOCAL_DATE_TIME_FORMAT));

        Timestamp timestamp = Dates.parseTimestamp("2025-10-04 14:30:45", Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(timestamp);
    }

    @Test
    public void test_parseTimestamp_withFormatAndTimeZone() {
        assertNull(Dates.parseTimestamp(null, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTimestamp("null", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));

        Timestamp timestamp = Dates.parseTimestamp("2025-10-04 14:30:45", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(timestamp);
    }

    @Test
    public void test_parseCalendar_singleParam() {
        assertNull(Dates.parseCalendar((String) null));
        assertNull(Dates.parseCalendar(""));
        assertNull(Dates.parseCalendar("null"));

        Calendar calendar = Dates.parseCalendar("1000000000");
        assertNotNull(calendar);
        assertEquals(1000000000L, calendar.getTimeInMillis());
    }

    @Test
    public void test_parseCalendar_withFormat() {
        assertNull(Dates.parseCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseCalendar("null", Dates.LOCAL_DATE_FORMAT));

        Calendar calendar = Dates.parseCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void test_parseCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        Calendar calendar = Dates.parseCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void test_parseGregorianCalendar_singleParam() {
        assertNull(Dates.parseGregorianCalendar((String) null));
        assertNull(Dates.parseGregorianCalendar(""));
        assertNull(Dates.parseGregorianCalendar("null"));

        GregorianCalendar calendar = Dates.parseGregorianCalendar("1000000000");
        assertNotNull(calendar);
        assertEquals(1000000000L, calendar.getTimeInMillis());
    }

    @Test
    public void test_parseGregorianCalendar_withFormat() {
        assertNull(Dates.parseGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseGregorianCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT));

        GregorianCalendar calendar = Dates.parseGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void test_parseGregorianCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseGregorianCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        GregorianCalendar calendar = Dates.parseGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void test_parseXMLGregorianCalendar_singleParam() {
        assertNull(Dates.parseXMLGregorianCalendar((String) null));
        assertNull(Dates.parseXMLGregorianCalendar(""));
        assertNull(Dates.parseXMLGregorianCalendar("null"));

        XMLGregorianCalendar calendar = Dates.parseXMLGregorianCalendar("1000000000");
        assertNotNull(calendar);
    }

    @Test
    public void test_parseXMLGregorianCalendar_withFormat() {
        assertNull(Dates.parseXMLGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseXMLGregorianCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseXMLGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT));

        XMLGregorianCalendar calendar = Dates.parseXMLGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void test_parseXMLGregorianCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseXMLGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseXMLGregorianCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseXMLGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        XMLGregorianCalendar calendar = Dates.parseXMLGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void test_formatLocalDate() {
        String formatted = Dates.formatLocalDate();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_formatLocalDateTime() {
        String formatted = Dates.formatLocalDateTime();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void test_formatCurrentDateTime() {
        String formatted = Dates.formatCurrentDateTime();
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
        assertTrue(formatted.endsWith("Z"));
    }

    @Test
    public void test_formatCurrentTimestamp() {
        String formatted = Dates.formatCurrentTimestamp();
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
        assertTrue(formatted.contains("."));
        assertTrue(formatted.endsWith("Z"));
    }

    @Test
    public void test_format_date_singleParam() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void test_format_date_withFormat() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_format_date_withFormatAndTimeZone() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_format_calendar_singleParam() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void test_format_calendar_withFormat() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_format_calendar_withFormatAndTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_format_xmlGregorianCalendar_singleParam() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void test_format_xmlGregorianCalendar_withFormat() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_format_xmlGregorianCalendar_withFormatAndTimeZone() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_formatTo_date_singleParam() {
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(date, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_date_withFormat() {
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(date, Dates.LOCAL_DATE_FORMAT, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_date_withFormatAndTimeZone() {
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(date, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_date_null() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((java.util.Date) null, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void test_formatTo_calendar_singleParam() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_calendar_withFormat() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_calendar_withFormatAndTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_calendar_null() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((Calendar) null, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void test_formatTo_xmlGregorianCalendar_singleParam() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_xmlGregorianCalendar_withFormat() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_xmlGregorianCalendar_withFormatAndTimeZone() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_formatTo_xmlGregorianCalendar_null() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((XMLGregorianCalendar) null, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void test_setYears() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setYears(date, 2025);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(2025, cal.get(Calendar.YEAR));
    }

    @Test
    public void test_setMonths() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setMonths(date, 5);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(5, cal.get(Calendar.MONTH));
    }

    @Test
    public void test_setDays() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setDays(date, 15);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void test_setHours() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setHours(date, 14);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void test_setMinutes() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setMinutes(date, 30);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(30, cal.get(Calendar.MINUTE));
    }

    @Test
    public void test_setSeconds() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setSeconds(date, 45);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(45, cal.get(Calendar.SECOND));
    }

    @Test
    public void test_setMilliseconds() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setMilliseconds(date, 123);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void test_roll_date_withTimeUnit() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.roll(date, 5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 5, TimeUnit.DAYS));
    }

    @Test
    public void test_roll_date_withTimeUnit_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.roll(date, -5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_roll_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.roll(date, 5, CalendarField.DAY_OF_MONTH);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 5, CalendarField.DAY_OF_MONTH));
    }

    @Test
    public void test_roll_calendar_withTimeUnit() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.roll(cal, 5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > 1000000000L);

        assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 5, TimeUnit.DAYS));
    }

    @Test
    public void test_roll_calendar_withTimeUnit_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.roll(cal, -5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < 1000000000L);
    }

    @Test
    public void test_roll_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.roll(cal, 5, CalendarField.DAY_OF_MONTH);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > 1000000000L);

        assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 5, CalendarField.DAY_OF_MONTH));
    }

    @Test
    public void test_addYears_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addYears(date, 1);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void test_addYears_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addYears(date, -1);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_addMonths_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMonths(date, 3);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void test_addMonths_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMonths(date, -3);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_addWeeks_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addWeeks(date, 2);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void test_addWeeks_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addWeeks(date, -2);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_addDays_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addDays(date, 7);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void test_addDays_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addDays(date, -7);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_addHours_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addHours(date, 5);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void test_addHours_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addHours(date, -5);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_addMinutes_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMinutes(date, 30);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void test_addMinutes_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMinutes(date, -30);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_addSeconds_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addSeconds(date, 45);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void test_addSeconds_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addSeconds(date, -45);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void test_addMilliseconds_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMilliseconds(date, 500);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
        assertEquals(500, result.getTime() - date.getTime());
    }

    @Test
    public void test_addMilliseconds_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMilliseconds(date, -500);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
        assertEquals(500, date.getTime() - result.getTime());
    }

    @Test
    public void test_addYears_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addYears(cal, 1);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void test_addYears_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addYears(cal, -1);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void test_addMonths_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMonths(cal, 3);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void test_addMonths_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMonths(cal, -3);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void test_addWeeks_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addWeeks(cal, 2);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void test_addWeeks_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addWeeks(cal, -2);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void test_addDays_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addDays(cal, 7);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void test_addDays_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addDays(cal, -7);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void test_addHours_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addHours(cal, 5);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void test_addHours_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addHours(cal, -5);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void test_addMinutes_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMinutes(cal, 30);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void test_addMinutes_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMinutes(cal, -30);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void test_addSeconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addSeconds(cal, 45);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void test_addSeconds_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addSeconds(cal, -45);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void test_addMilliseconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMilliseconds(cal, 500);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
        assertEquals(500, result.getTimeInMillis() - cal.getTimeInMillis());
    }

    @Test
    public void test_addMilliseconds_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMilliseconds(cal, -500);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
        assertEquals(500, cal.getTimeInMillis() - result.getTimeInMillis());
    }

    @Test
    public void test_round_date_withInt() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.round(date, Calendar.SECOND);
        assertNotNull(result);

        assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, Calendar.SECOND));
    }

    @Test
    public void test_round_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.round(date, CalendarField.SECOND);
        assertNotNull(result);
    }

    @Test
    public void test_round_calendar_withInt() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.round(cal, Calendar.SECOND);
        assertNotNull(result);

        assertThrows(IllegalArgumentException.class, () -> Dates.round((Calendar) null, Calendar.SECOND));
    }

    @Test
    public void test_round_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.round(cal, CalendarField.SECOND);
        assertNotNull(result);
    }

    @Test
    public void test_truncate_date_withInt() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.truncate(date, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() <= date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((java.util.Date) null, Calendar.SECOND));
    }

    @Test
    public void test_truncate_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.truncate(date, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() <= date.getTime());
    }

    @Test
    public void test_truncate_calendar_withInt() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.truncate(cal, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() <= 1000000123L);

        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, Calendar.SECOND));
    }

    @Test
    public void test_truncate_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.truncate(cal, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() <= 1000000123L);
    }

    @Test
    public void test_ceiling_date_withInt() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.ceiling(date, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() >= date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((java.util.Date) null, Calendar.SECOND));
    }

    @Test
    public void test_ceiling_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.ceiling(date, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() >= date.getTime());
    }

    @Test
    public void test_ceiling_calendar_withInt() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.ceiling(cal, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() >= 1000000123L);

        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((Calendar) null, Calendar.SECOND));
    }

    @Test
    public void test_ceiling_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.ceiling(cal, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() >= 1000000123L);
    }

    @Test
    public void test_truncatedEquals_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1, cal2, CalendarField.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1, cal2, CalendarField.SECOND));
    }

    @Test
    public void test_truncatedEquals_calendar_withInt() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1, cal2, Calendar.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1, cal2, Calendar.SECOND));
    }

    @Test
    public void test_truncatedEquals_date() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), CalendarField.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), CalendarField.SECOND));
    }

    @Test
    public void test_truncatedEquals_date_withInt() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), Calendar.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), Calendar.SECOND));
    }

    @Test
    public void test_truncatedCompareTo_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertTrue(Dates.truncatedCompareTo(cal1, cal2, CalendarField.HOUR_OF_DAY) == 0);
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, CalendarField.MINUTE) < 0);
    }

    @Test
    public void test_truncatedCompareTo_calendar_withInt() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertTrue(Dates.truncatedCompareTo(cal1, cal2, Calendar.HOUR_OF_DAY) == 0);
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, Calendar.MINUTE) < 0);
    }

    @Test
    public void test_truncatedCompareTo_date() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertTrue(Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), CalendarField.HOUR_OF_DAY) == 0);
        assertTrue(Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), CalendarField.MINUTE) < 0);
    }

    @Test
    public void test_truncatedCompareTo_date_withInt() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertTrue(Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), Calendar.HOUR_OF_DAY) == 0);
        assertTrue(Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), Calendar.MINUTE) < 0);
    }

    @Test
    public void test_getFragmentInMilliseconds_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);

        long result = Dates.getFragmentInMilliseconds(cal.getTime(), CalendarField.SECOND);
        assertEquals(123, result);
    }

    @Test
    public void test_getFragmentInSeconds_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);

        long result = Dates.getFragmentInSeconds(cal.getTime(), CalendarField.MINUTE);
        assertEquals(45, result);
    }

    @Test
    public void test_getFragmentInMinutes_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        long result = Dates.getFragmentInMinutes(cal.getTime(), CalendarField.HOUR_OF_DAY);
        assertEquals(30, result);
    }

    @Test
    public void test_getFragmentInHours_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        long result = Dates.getFragmentInHours(cal.getTime(), CalendarField.DAY_OF_MONTH);
        assertEquals(14, result);
    }

    @Test
    public void test_getFragmentInDays_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        long result = Dates.getFragmentInDays(cal.getTime(), CalendarField.MONTH);
        assertTrue(result >= 0);
    }

    @Test
    public void test_getFragmentInMilliseconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);

        long result = Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND);
        assertEquals(123, result);
    }

    @Test
    public void test_getFragmentInSeconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);

        long result = Dates.getFragmentInSeconds(cal, CalendarField.MINUTE);
        assertEquals(45, result);
    }

    @Test
    public void test_getFragmentInMinutes_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        long result = Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY);
        assertEquals(30, result);
    }

    @Test
    public void test_getFragmentInHours_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        long result = Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH);
        assertEquals(14, result);
    }

    @Test
    public void test_getFragmentInDays_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);

        long result = Dates.getFragmentInDays(cal, CalendarField.MONTH);
        assertTrue(result >= 0);
    }

    @Test
    public void test_isSameDay_date() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 15, 45, 30);

        assertTrue(Dates.isSameDay(cal1.getTime(), cal2.getTime()));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2025, Calendar.OCTOBER, 5, 10, 30, 45);

        assertFalse(Dates.isSameDay(cal1.getTime(), cal3.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(null, cal1.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(cal1.getTime(), null));
    }

    @Test
    public void test_isSameDay_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 15, 45, 30);

        assertTrue(Dates.isSameDay(cal1, cal2));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2025, Calendar.OCTOBER, 5, 10, 30, 45);

        assertFalse(Dates.isSameDay(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(cal1, null));
    }

    @Test
    public void test_isSameMonth_date() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 15, 15, 45, 30);

        assertTrue(Dates.isSameMonth(cal1.getTime(), cal2.getTime()));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2025, Calendar.NOVEMBER, 4, 10, 30, 45);

        assertFalse(Dates.isSameMonth(cal1.getTime(), cal3.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(null, cal1.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(cal1.getTime(), null));
    }

    @Test
    public void test_isSameMonth_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 15, 15, 45, 30);

        assertTrue(Dates.isSameMonth(cal1, cal2));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2025, Calendar.NOVEMBER, 4, 10, 30, 45);

        assertFalse(Dates.isSameMonth(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(cal1, null));
    }

    @Test
    public void test_isSameYear_date() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.DECEMBER, 31, 23, 59, 59);

        assertTrue(Dates.isSameYear(cal1.getTime(), cal2.getTime()));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2026, Calendar.JANUARY, 1, 0, 0, 0);

        assertFalse(Dates.isSameYear(cal1.getTime(), cal3.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(null, cal1.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(cal1.getTime(), null));
    }

    @Test
    public void test_isSameYear_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.DECEMBER, 31, 23, 59, 59);

        assertTrue(Dates.isSameYear(cal1, cal2));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2026, Calendar.JANUARY, 1, 0, 0, 0);

        assertFalse(Dates.isSameYear(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(cal1, null));
    }

    @Test
    public void test_isSameInstant_date() {
        java.util.Date date1 = new java.util.Date(1000000000L);
        java.util.Date date2 = new java.util.Date(1000000000L);
        java.util.Date date3 = new java.util.Date(1000000001L);

        assertTrue(Dates.isSameInstant(date1, date2));
        assertFalse(Dates.isSameInstant(date1, date3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(null, date1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(date1, null));
    }

    @Test
    public void test_isSameInstant_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(1000000000L);

        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(1000000000L);

        Calendar cal3 = Calendar.getInstance();
        cal3.setTimeInMillis(1000000001L);

        assertTrue(Dates.isSameInstant(cal1, cal2));
        assertFalse(Dates.isSameInstant(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(cal1, null));
    }

    @Test
    public void test_isSameLocalTime_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal1.set(Calendar.MILLISECOND, 123);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal2.set(Calendar.MILLISECOND, 123);

        assertTrue(Dates.isSameLocalTime(cal1, cal2));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2025, Calendar.OCTOBER, 4, 14, 30, 46);

        assertFalse(Dates.isSameLocalTime(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameLocalTime(null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameLocalTime(cal1, null));
    }

    @Test
    public void test_isLastDateOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 31, 10, 30, 45);
        assertTrue(Dates.isLastDateOfMonth(cal.getTime()));

        cal.set(2025, Calendar.JANUARY, 30, 10, 30, 45);
        assertFalse(Dates.isLastDateOfMonth(cal.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfMonth(null));
    }

    @Test
    public void test_isLastDateOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.DECEMBER, 31, 10, 30, 45);
        assertTrue(Dates.isLastDateOfYear(cal.getTime()));

        cal.set(2025, Calendar.DECEMBER, 30, 10, 30, 45);
        assertFalse(Dates.isLastDateOfYear(cal.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfYear(null));
    }

    @Test
    public void test_getLastDateOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 15, 10, 30, 45);
        int lastDate = Dates.getLastDateOfMonth(cal.getTime());
        assertEquals(31, lastDate);

        cal.set(2025, Calendar.FEBRUARY, 15, 10, 30, 45);
        lastDate = Dates.getLastDateOfMonth(cal.getTime());
        assertEquals(28, lastDate);

        assertThrows(IllegalArgumentException.class, () -> Dates.getLastDateOfMonth(null));
    }

    @Test
    public void test_getLastDateOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JUNE, 15, 10, 30, 45);
        int lastDate = Dates.getLastDateOfYear(cal.getTime());
        assertEquals(365, lastDate);

        assertThrows(IllegalArgumentException.class, () -> Dates.getLastDateOfYear(null));
    }

    @Test
    public void test_isOverlap() {
        java.util.Date start1 = new java.util.Date(1000000000L);
        java.util.Date end1 = new java.util.Date(2000000000L);
        java.util.Date start2 = new java.util.Date(1500000000L);
        java.util.Date end2 = new java.util.Date(2500000000L);

        assertTrue(Dates.isOverlap(start1, end1, start2, end2));

        java.util.Date start3 = new java.util.Date(2500000000L);
        java.util.Date end3 = new java.util.Date(3000000000L);

        assertFalse(Dates.isOverlap(start1, end1, start3, end3));
    }

    @Test
    public void test_isBetween() {
        java.util.Date date = new java.util.Date(1500000000L);
        java.util.Date startDate = new java.util.Date(1000000000L);
        java.util.Date endDate = new java.util.Date(2000000000L);

        assertTrue(Dates.isBetween(date, startDate, endDate));

        java.util.Date outsideDate = new java.util.Date(3000000000L);
        assertFalse(Dates.isBetween(outsideDate, startDate, endDate));
    }

    @Test
    public void test_DTF_format_date() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = dtf.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_DTF_format_calendar() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = dtf.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void test_DTF_formatTo_date() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo(date, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_DTF_formatTo_calendar() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo(cal, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void test_DTF_parseToJUDate() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = dtf.parseToJUDate("2025-10-04");
        assertNotNull(date);
    }

    @Test
    public void test_DTF_parseToJUDate_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = dtf.parseToJUDate("2025-10-04", TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    @Test
    public void test_DTF_parseToDate() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.sql.Date date = dtf.parseToDate("2025-10-04");
        assertNotNull(date);
    }

    @Test
    public void test_DTF_parseToDate_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.sql.Date date = dtf.parseToDate("2025-10-04", TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    @Test
    public void test_DTF_parseToTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        Time time = dtf.parseToTime("14:30:45");
        assertNotNull(time);
    }

    @Test
    public void test_DTF_parseToTime_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        Time time = dtf.parseToTime("14:30:45", TimeZone.getTimeZone("UTC"));
        assertNotNull(time);
    }

    @Test
    public void test_DTF_parseToTimestamp() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        Timestamp timestamp = dtf.parseToTimestamp("2025-10-04 14:30:45");
        assertNotNull(timestamp);
    }

    @Test
    public void test_DTF_parseToTimestamp_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        Timestamp timestamp = dtf.parseToTimestamp("2025-10-04 14:30:45", TimeZone.getTimeZone("UTC"));
        assertNotNull(timestamp);
    }

    @Test
    public void test_DTF_parseToCalendar() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar calendar = dtf.parseToCalendar("2025-10-04");
        assertNotNull(calendar);
    }

    @Test
    public void test_DTF_parseToCalendar_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar calendar = dtf.parseToCalendar("2025-10-04", TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void test_DTF_toString() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        String str = dtf.toString();
        assertNotNull(str);
        assertTrue(str.contains(Dates.LOCAL_DATE_FORMAT));
    }

    @Test
    public void test_edgeCase_negativeMillis() {
        long negativeMillis = -1000000000L;
        java.util.Date date = Dates.createJUDate(negativeMillis);
        assertNotNull(date);
        assertEquals(negativeMillis, date.getTime());
    }

    @Test
    public void test_edgeCase_veryLargeMillis() {
        long largeMillis = Long.MAX_VALUE / 2;
        java.util.Date date = Dates.createJUDate(largeMillis);
        assertNotNull(date);
        assertEquals(largeMillis, date.getTime());
    }

    @Test
    public void test_edgeCase_boundaryDates() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.FEBRUARY, 28, 23, 59, 59);
        java.util.Date date = Dates.addDays(cal.getTime(), 1);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(date);
        assertEquals(Calendar.MARCH, resultCal.get(Calendar.MONTH));
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void test_edgeCase_leapYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 29, 12, 0, 0);
        int lastDate = Dates.getLastDateOfMonth(cal.getTime());
        assertEquals(29, lastDate);
    }

    @Test
    public void test_edgeCase_daylightSavingTime() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Dates.createCalendar(1000000000L, tz);
        assertNotNull(cal);
        assertEquals(tz, cal.getTimeZone());
    }

    @Test
    public void test_edgeCase_differentTimeZones() {
        long millis = 1000000000L;
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");

        Calendar calUtc = Dates.createCalendar(millis, utc);
        Calendar calTokyo = Dates.createCalendar(millis, tokyo);

        assertEquals(millis, calUtc.getTimeInMillis());
        assertEquals(millis, calTokyo.getTimeInMillis());
    }

    @Test
    public void test_DTF_parseToLocalDate() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.time.LocalDate localDate = dtf.parseToLocalDate("2025-10-04");
        assertNotNull(localDate);
        assertEquals(2025, localDate.getYear());
        assertEquals(10, localDate.getMonthValue());
        assertEquals(4, localDate.getDayOfMonth());
    }

    @Test
    public void test_DTF_parseToLocalDate_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.time.LocalDate localDate = dtf.parseToLocalDate(null);
        assertNull(localDate);
    }

    @Test
    public void test_DTF_parseToLocalTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        java.time.LocalTime localTime = dtf.parseToLocalTime("14:30:45");
        assertNotNull(localTime);
        assertEquals(14, localTime.getHour());
        assertEquals(30, localTime.getMinute());
        assertEquals(45, localTime.getSecond());
    }

    @Test
    public void test_DTF_parseToLocalTime_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        java.time.LocalTime localTime = dtf.parseToLocalTime("");
        assertNull(localTime);
    }

    @Test
    public void test_DTF_parseToLocalDateTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        java.time.LocalDateTime localDateTime = dtf.parseToLocalDateTime("2025-10-04 14:30:45");
        assertNotNull(localDateTime);
        assertEquals(2025, localDateTime.getYear());
        assertEquals(10, localDateTime.getMonthValue());
        assertEquals(4, localDateTime.getDayOfMonth());
        assertEquals(14, localDateTime.getHour());
        assertEquals(30, localDateTime.getMinute());
        assertEquals(45, localDateTime.getSecond());
    }

    @Test
    public void test_DTF_parseToLocalDateTime_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        java.time.LocalDateTime localDateTime = dtf.parseToLocalDateTime(null);
        assertNull(localDateTime);
    }

    @Test
    public void test_DTF_parseToOffsetDateTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        java.time.OffsetDateTime offsetDateTime = dtf.parseToOffsetDateTime("2025-10-04T14:30:45+00:00");
        assertNotNull(offsetDateTime);
        assertEquals(2025, offsetDateTime.getYear());
        assertEquals(10, offsetDateTime.getMonthValue());
        assertEquals(4, offsetDateTime.getDayOfMonth());
    }

    @Test
    public void test_DTF_parseToOffsetDateTime_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        java.time.OffsetDateTime offsetDateTime = dtf.parseToOffsetDateTime("");
        assertNull(offsetDateTime);
    }

    @Test
    public void test_DTF_parseToZonedDateTime() {
        Dates.DTF dtf = new Dates.DTF("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'");
        java.time.ZonedDateTime zonedDateTime = dtf.parseToZonedDateTime("2025-10-04T14:30:45+00:00[UTC]");
        assertNotNull(zonedDateTime);
        assertEquals(2025, zonedDateTime.getYear());
        assertEquals(10, zonedDateTime.getMonthValue());
        assertEquals(4, zonedDateTime.getDayOfMonth());
    }

    @Test
    public void test_DTF_parseToZonedDateTime_null() {
        Dates.DTF dtf = new Dates.DTF("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'");
        java.time.ZonedDateTime zonedDateTime = dtf.parseToZonedDateTime(null);
        assertNull(zonedDateTime);
    }

    @Test
    public void test_DTF_parseToInstant() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        java.time.Instant instant = dtf.parseToInstant("2025-10-04T14:30:45+00:00");
        assertNotNull(instant);
        assertTrue(instant.toEpochMilli() > 0);
    }

    @Test
    public void test_DTF_parseToInstant_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        java.time.Instant instant = dtf.parseToInstant("");
        assertNull(instant);
    }

    @Test
    public void test_DTF_format_temporalAccessor() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.time.LocalDate localDate = java.time.LocalDate.of(2025, 10, 4);
        String formatted = dtf.format(localDate);
        assertNotNull(formatted);
        assertEquals("2025-10-04", formatted);
    }

    @Test
    public void test_DTF_format_temporalAccessor_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        String formatted = dtf.format((java.time.temporal.TemporalAccessor) null);
        assertNull(formatted);
    }

    @Test
    public void test_DTF_formatTo_temporalAccessor() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.time.LocalDate localDate = java.time.LocalDate.of(2025, 10, 4);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo(localDate, sb));
        assertTrue(sb.length() > 0);
        assertEquals("2025-10-04", sb.toString());
    }

    @Test
    public void test_DTF_formatTo_temporalAccessor_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo((java.time.temporal.TemporalAccessor) null, sb));
        assertEquals("null", sb.toString());
    }
}
