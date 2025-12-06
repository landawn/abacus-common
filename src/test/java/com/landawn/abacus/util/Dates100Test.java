package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Dates100Test extends TestBase {

    private static final long FIXED_TIME_MILLIS = 1640995200000L;
    private static final String ISO_DATE_TIME_STRING = "2022-01-01T00:00:00Z";
    private static final String ISO_TIMESTAMP_STRING = "2022-01-01T00:00:00.000Z";
    private static final String LOCAL_DATE_TIME_STRING = "2022-01-01 00:00:00";
    private static final String LOCAL_DATE_STRING = "2022-01-01";
    private static final String LOCAL_TIME_STRING = "00:00:00";

    private java.util.Date testDate;
    private Calendar testCalendar;
    private TimeZone defaultTimeZone;

    @BeforeEach
    public void setUp() {
        testDate = Dates.parseJUDate("2022-01-01 00:00:00");
        testCalendar = Calendar.getInstance();
        testCalendar.setTimeInMillis(FIXED_TIME_MILLIS);
        defaultTimeZone = TimeZone.getDefault();
    }

    @Test
    public void testCurrentTimeMillis() {
        long before = System.currentTimeMillis();
        long result = Dates.currentTimeMillis();
        long after = System.currentTimeMillis();

        assertTrue(result >= before);
        assertTrue(result <= after);
    }

    @Test
    public void testCurrentTime() {
        Time result = Dates.currentTime();
        assertNotNull(result);
        assertTrue(result.getTime() <= System.currentTimeMillis());
    }

    @Test
    public void testCurrentDate() {
        Date result = Dates.currentDate();
        assertNotNull(result);
        assertTrue(result.getTime() <= System.currentTimeMillis());
    }

    @Test
    public void testCurrentTimestamp() {
        Timestamp result = Dates.currentTimestamp();
        assertNotNull(result);
        assertTrue(result.getTime() <= System.currentTimeMillis());
    }

    @Test
    public void testCurrentJUDate() {
        java.util.Date result = Dates.currentJUDate();
        assertNotNull(result);
        assertTrue(result.getTime() <= System.currentTimeMillis());
    }

    @Test
    public void testCurrentCalendar() {
        Calendar result = Dates.currentCalendar();
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() <= System.currentTimeMillis());
    }

    @Test
    public void testCurrentGregorianCalendar() {
        GregorianCalendar result = Dates.currentGregorianCalendar();
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() <= System.currentTimeMillis());
    }

    @Test
    public void testCurrentXMLGregorianCalendar() {
        XMLGregorianCalendar result = Dates.currentXMLGregorianCalendar();
        assertNotNull(result);
    }

    @Test
    public void testCurrentTimeRolled() {
        long amount = 5;
        TimeUnit unit = TimeUnit.HOURS;
        Time result = Dates.currentTimeRolled(amount, unit);
        assertNotNull(result);

        long diff = result.getTime() - System.currentTimeMillis();
        assertTrue(Math.abs(diff - unit.toMillis(amount)) < 1000);
    }

    @Test
    public void testCurrentDateRolled() {
        long amount = 2;
        TimeUnit unit = TimeUnit.DAYS;
        Date result = Dates.currentDateRolled(amount, unit);
        assertNotNull(result);

        long diff = result.getTime() - System.currentTimeMillis();
        assertTrue(Math.abs(diff - unit.toMillis(amount)) < 1000);
    }

    @Test
    public void testCurrentTimestampRolled() {
        long amount = -3;
        TimeUnit unit = TimeUnit.MINUTES;
        Timestamp result = Dates.currentTimestampRolled(amount, unit);
        assertNotNull(result);

        long diff = result.getTime() - System.currentTimeMillis();
        assertTrue(Math.abs(diff - unit.toMillis(amount)) < 1000);
    }

    @Test
    public void testCurrentJUDateRolled() {
        long amount = 1;
        TimeUnit unit = TimeUnit.SECONDS;
        java.util.Date result = Dates.currentJUDateRolled(amount, unit);
        assertNotNull(result);

        long diff = result.getTime() - System.currentTimeMillis();
        assertTrue(Math.abs(diff - unit.toMillis(amount)) < 1000);
    }

    @Test
    public void testCurrentCalendarRolled() {
        long amount = 10;
        TimeUnit unit = TimeUnit.MILLISECONDS;
        Calendar result = Dates.currentCalendarRolled(amount, unit);
        assertNotNull(result);

        long diff = result.getTimeInMillis() - System.currentTimeMillis();
        assertTrue(Math.abs(diff - unit.toMillis(amount)) < 100);
    }

    @Test
    public void testCreateJUDateFromCalendar() {
        java.util.Date result = Dates.createJUDate(testCalendar);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateJUDateFromDate() {
        java.util.Date result = Dates.createJUDate(testDate);
        assertNotNull(result);
        assertEquals(testDate.getTime(), result.getTime());
    }

    @Test
    public void testCreateJUDateFromMillis() {
        java.util.Date result = Dates.createJUDate(FIXED_TIME_MILLIS);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateJUDateFromNullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((Calendar) null));
    }

    @Test
    public void testCreateJUDateFromNullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((java.util.Date) null));
    }

    @Test
    public void testCreateDateFromCalendar() {
        Date result = Dates.createDate(testCalendar);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateDateFromDate() {
        Date result = Dates.createDate(testDate);
        assertNotNull(result);
        assertEquals(testDate.getTime(), result.getTime());
    }

    @Test
    public void testCreateDateFromMillis() {
        Date result = Dates.createDate(FIXED_TIME_MILLIS);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateTimeFromCalendar() {
        Time result = Dates.createTime(testCalendar);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateTimeFromDate() {
        Time result = Dates.createTime(testDate);
        assertNotNull(result);
        assertEquals(testDate.getTime(), result.getTime());
    }

    @Test
    public void testCreateTimeFromMillis() {
        Time result = Dates.createTime(FIXED_TIME_MILLIS);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateTimestampFromCalendar() {
        Timestamp result = Dates.createTimestamp(testCalendar);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateTimestampFromDate() {
        Timestamp result = Dates.createTimestamp(testDate);
        assertNotNull(result);
        assertEquals(testDate.getTime(), result.getTime());
    }

    @Test
    public void testCreateTimestampFromMillis() {
        Timestamp result = Dates.createTimestamp(FIXED_TIME_MILLIS);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testCreateCalendarFromCalendar() {
        Calendar result = Dates.createCalendar(testCalendar);
        assertNotNull(result);
        assertEquals(testCalendar.getTimeInMillis(), result.getTimeInMillis());
    }

    @Test
    public void testCreateCalendarFromDate() {
        Calendar result = Dates.createCalendar(testDate);
        assertNotNull(result);
        assertEquals(testDate.getTime(), result.getTimeInMillis());
    }

    @Test
    public void testCreateCalendarFromMillis() {
        Calendar result = Dates.createCalendar(FIXED_TIME_MILLIS);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
    }

    @Test
    public void testCreateCalendarFromMillisWithTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("GMT");
        Calendar result = Dates.createCalendar(FIXED_TIME_MILLIS, tz);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
        assertEquals(tz, result.getTimeZone());
    }

    @Test
    public void testCreateGregorianCalendarFromCalendar() {
        GregorianCalendar result = Dates.createGregorianCalendar(testCalendar);
        assertNotNull(result);
        assertEquals(testCalendar.getTimeInMillis(), result.getTimeInMillis());
    }

    @Test
    public void testCreateGregorianCalendarFromDate() {
        GregorianCalendar result = Dates.createGregorianCalendar(testDate);
        assertNotNull(result);
        assertEquals(testDate.getTime(), result.getTimeInMillis());
    }

    @Test
    public void testCreateGregorianCalendarFromMillis() {
        GregorianCalendar result = Dates.createGregorianCalendar(FIXED_TIME_MILLIS);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
    }

    @Test
    public void testCreateGregorianCalendarFromMillisWithTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("EST");
        GregorianCalendar result = Dates.createGregorianCalendar(FIXED_TIME_MILLIS, tz);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
        assertEquals(tz, result.getTimeZone());
    }

    @Test
    public void testCreateXMLGregorianCalendarFromCalendar() {
        XMLGregorianCalendar result = Dates.createXMLGregorianCalendar(testCalendar);
        assertNotNull(result);
    }

    @Test
    public void testCreateXMLGregorianCalendarFromDate() {
        XMLGregorianCalendar result = Dates.createXMLGregorianCalendar(testDate);
        assertNotNull(result);
    }

    @Test
    public void testCreateXMLGregorianCalendarFromMillis() {
        XMLGregorianCalendar result = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS);
        assertNotNull(result);
    }

    @Test
    public void testCreateXMLGregorianCalendarFromMillisWithTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("PST");
        XMLGregorianCalendar result = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS, tz);
        assertNotNull(result);
    }

    @Test
    public void testParseJUDate() {
        java.util.Date result = Dates.parseJUDate(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseJUDateWithFormat() {
        java.util.Date result = Dates.parseJUDate(LOCAL_DATE_TIME_STRING, Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(result);
    }

    @Test
    public void testParseJUDateWithFormatAndTimeZone() {
        java.util.Date result = Dates.parseJUDate(ISO_DATE_TIME_STRING, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseJUDateNull() {
        assertNull(Dates.parseJUDate(null));
        assertNull(Dates.parseJUDate(""));
        assertNull(Dates.parseJUDate("null"));
    }

    @Test
    public void testParseJUDateLong() {
        java.util.Date result = Dates.parseJUDate(String.valueOf(FIXED_TIME_MILLIS));
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseDate() {
        Date result = Dates.parseDate(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseDateWithFormat() {
        Date result = Dates.parseDate(LOCAL_DATE_STRING, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(result);
    }

    @Test
    public void testParseDateWithFormatAndTimeZone() {
        Date result = Dates.parseDate(ISO_DATE_TIME_STRING, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseDateNull() {
        assertNull(Dates.parseDate(null));
        assertNull(Dates.parseDate(""));
        assertNull(Dates.parseDate("null"));
    }

    @Test
    public void testParseTime() {
        Time result = Dates.parseTime(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseTimeWithFormat() {
        Time result = Dates.parseTime(LOCAL_TIME_STRING, Dates.LOCAL_TIME_FORMAT);
        assertNotNull(result);
    }

    @Test
    public void testParseTimeWithFormatAndTimeZone() {
        Time result = Dates.parseTime(ISO_DATE_TIME_STRING, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseTimeNull() {
        assertNull(Dates.parseTime(null));
        assertNull(Dates.parseTime(""));
        assertNull(Dates.parseTime("null"));
    }

    @Test
    public void testParseTimestamp() {
        Timestamp result = Dates.parseTimestamp(ISO_TIMESTAMP_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseTimestampWithFormat() {
        Timestamp result = Dates.parseTimestamp(LOCAL_DATE_TIME_STRING, Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(result);
    }

    @Test
    public void testParseTimestampWithFormatAndTimeZone() {
        Timestamp result = Dates.parseTimestamp(ISO_TIMESTAMP_STRING, Dates.ISO_8601_TIMESTAMP_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testParseTimestampNull() {
        assertNull(Dates.parseTimestamp(null));
        assertNull(Dates.parseTimestamp(""));
        assertNull(Dates.parseTimestamp("null"));
    }

    @Test
    public void testParseCalendar() {
        Calendar result = Dates.parseCalendar(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
    }

    @Test
    public void testParseCalendarWithFormat() {
        Calendar result = Dates.parseCalendar(LOCAL_DATE_TIME_STRING, Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(result);
    }

    @Test
    public void testParseCalendarWithFormatAndTimeZone() {
        Calendar result = Dates.parseCalendar(ISO_DATE_TIME_STRING, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
    }

    @Test
    public void testParseCalendarNull() {
        assertNull(Dates.parseCalendar(null));
        assertNull(Dates.parseCalendar(""));
        assertNull(Dates.parseCalendar("null"));
    }

    @Test
    public void testParseGregorianCalendar() {
        GregorianCalendar result = Dates.parseGregorianCalendar(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
    }

    @Test
    public void testParseGregorianCalendarWithFormat() {
        GregorianCalendar result = Dates.parseGregorianCalendar(LOCAL_DATE_TIME_STRING, Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(result);
    }

    @Test
    public void testParseGregorianCalendarWithFormatAndTimeZone() {
        GregorianCalendar result = Dates.parseGregorianCalendar(ISO_DATE_TIME_STRING, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
    }

    @Test
    public void testParseGregorianCalendarNull() {
        assertNull(Dates.parseGregorianCalendar(null));
        assertNull(Dates.parseGregorianCalendar(""));
        assertNull(Dates.parseGregorianCalendar("null"));
    }

    @Test
    public void testParseXMLGregorianCalendar() {
        XMLGregorianCalendar result = Dates.parseXMLGregorianCalendar(ISO_DATE_TIME_STRING);
        assertNotNull(result);
    }

    @Test
    public void testParseXMLGregorianCalendarWithFormat() {
        XMLGregorianCalendar result = Dates.parseXMLGregorianCalendar(LOCAL_DATE_TIME_STRING, Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(result);
    }

    @Test
    public void testParseXMLGregorianCalendarWithFormatAndTimeZone() {
        XMLGregorianCalendar result = Dates.parseXMLGregorianCalendar(ISO_DATE_TIME_STRING, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
    }

    @Test
    public void testParseXMLGregorianCalendarNull() {
        assertNull(Dates.parseXMLGregorianCalendar(null));
        assertNull(Dates.parseXMLGregorianCalendar(""));
        assertNull(Dates.parseXMLGregorianCalendar("null"));
    }

    @Test
    public void testFormatLocalDate() {
        String result = Dates.formatLocalDate();
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormatLocalDateTime() {
        String result = Dates.formatLocalDateTime();
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void testFormatCurrentDateTime() {
        String result = Dates.formatCurrentDateTime();
        assertNotNull(result);
        assertTrue(result.endsWith("Z"));
    }

    @Test
    public void testFormatCurrentTimestamp() {
        String result = Dates.formatCurrentTimestamp();
        assertNotNull(result);
        assertTrue(result.endsWith("Z"));
        assertTrue(result.contains("."));
    }

    @Test
    public void testFormatDate() {
        String result = Dates.format(new Date(FIXED_TIME_MILLIS));
        assertNotNull(result);
        assertEquals(ISO_DATE_TIME_STRING, result);
    }

    @Test
    public void testFormatDateWithFormat() {
        String result = Dates.format(testDate, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(result);
        assertEquals(LOCAL_DATE_STRING, result);
    }

    @Test
    public void testFormatDateWithFormatAndTimeZone() {
        String result = Dates.format(Dates.createDate(FIXED_TIME_MILLIS), Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(ISO_DATE_TIME_STRING, result);
    }

    @Test
    public void testFormatCalendar() {
        Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
        cal.setTimeInMillis(FIXED_TIME_MILLIS);
        String result = Dates.format(cal);
        assertNotNull(result);
        assertEquals(ISO_DATE_TIME_STRING, result);
    }

    @Test
    public void testFormatCalendarWithFormat() {
        String result = Dates.format(testCalendar, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormatCalendarWithFormatAndTimeZone() {
        Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
        cal.setTimeInMillis(FIXED_TIME_MILLIS);
        String result = Dates.format(cal, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(ISO_DATE_TIME_STRING, result);
    }

    @Test
    public void testFormatXMLGregorianCalendar() {
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS, Dates.UTC_TIME_ZONE);
        String result = Dates.format(xmlCal);
        assertNotNull(result);
        assertEquals(ISO_DATE_TIME_STRING, result);
    }

    @Test
    public void testFormatXMLGregorianCalendarWithFormat() {
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS);
        String result = Dates.format(xmlCal, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormatXMLGregorianCalendarWithFormatAndTimeZone() {
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS);
        String result = Dates.format(xmlCal, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(ISO_DATE_TIME_STRING, result);
    }

    @Test
    public void testFormatToDateAppendable() {
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(Dates.createDate(FIXED_TIME_MILLIS), sb);
        assertEquals(ISO_DATE_TIME_STRING, sb.toString());
    }

    @Test
    public void testFormatToDateAppendableWithFormat() {
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(testDate, Dates.LOCAL_DATE_FORMAT, sb);
        assertEquals(LOCAL_DATE_STRING, sb.toString());
    }

    @Test
    public void testFormatToDateAppendableWithFormatAndTimeZone() {
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(Dates.createDate(FIXED_TIME_MILLIS), Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE, sb);
        assertEquals(ISO_DATE_TIME_STRING, sb.toString());
    }

    @Test
    public void testFormatToCalendarAppendable() {
        StringBuilder sb = new StringBuilder();
        Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
        cal.setTimeInMillis(FIXED_TIME_MILLIS);
        Dates.formatTo(cal, sb);
        assertEquals(ISO_DATE_TIME_STRING, sb.toString());
    }

    @Test
    public void testFormatToCalendarAppendableWithFormat() {
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(testCalendar, Dates.LOCAL_DATE_FORMAT, sb);
        assertTrue(sb.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormatToCalendarAppendableWithFormatAndTimeZone() {
        StringBuilder sb = new StringBuilder();
        Calendar cal = Calendar.getInstance(Dates.UTC_TIME_ZONE);
        cal.setTimeInMillis(FIXED_TIME_MILLIS);
        Dates.formatTo(cal, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE, sb);
        assertEquals(ISO_DATE_TIME_STRING, sb.toString());
    }

    @Test
    public void testFormatToXMLGregorianCalendarAppendable() {
        StringBuilder sb = new StringBuilder();
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS, Dates.UTC_TIME_ZONE);
        Dates.formatTo(xmlCal, sb);
        assertEquals(ISO_DATE_TIME_STRING, sb.toString());
    }

    @Test
    public void testFormatToXMLGregorianCalendarAppendableWithFormat() {
        StringBuilder sb = new StringBuilder();
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS);
        Dates.formatTo(xmlCal, Dates.LOCAL_DATE_FORMAT, sb);
        assertTrue(sb.toString().matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormatToXMLGregorianCalendarAppendableWithFormatAndTimeZone() {
        StringBuilder sb = new StringBuilder();
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(FIXED_TIME_MILLIS);
        Dates.formatTo(xmlCal, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.UTC_TIME_ZONE, sb);
        assertEquals(ISO_DATE_TIME_STRING, sb.toString());
    }

    @Test
    public void testSetYears() {
        java.util.Date result = Dates.setYears(testDate, 2023);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(2023, cal.get(Calendar.YEAR));
    }

    @Test
    public void testSetMonths() {
        java.util.Date result = Dates.setMonths(testDate, 4);

        N.println("Original Date: " + result);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(4, cal.get(Calendar.MONTH));
    }

    @Test
    public void testSetDays() {
        java.util.Date result = Dates.setDays(testDate, 15);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testSetHours() {
        java.util.Date result = Dates.setHours(testDate, 12);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testSetMinutes() {
        java.util.Date result = Dates.setMinutes(testDate, 30);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(30, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testSetSeconds() {
        java.util.Date result = Dates.setSeconds(testDate, 45);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(45, cal.get(Calendar.SECOND));
    }

    @Test
    public void testSetMilliseconds() {
        java.util.Date result = Dates.setMilliseconds(testDate, 500);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(500, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testSetYearsNull() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(null, 2023));
    }

    @Test
    public void testRollDateWithTimeUnit() {
        java.util.Date result = Dates.roll(testDate, 5, TimeUnit.DAYS);
        assertNotNull(result);
        assertNotEquals(testDate, result);
        assertEquals(testDate.getTime() + TimeUnit.DAYS.toMillis(5), result.getTime());
    }

    @Test
    public void testRollDateWithCalendarField() {
        java.util.Date result = Dates.roll(testDate, 2, CalendarField.MONTH);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar expected = Calendar.getInstance();
        expected.setTime(testDate);
        expected.add(Calendar.MONTH, 2);

        assertEquals(expected.getTimeInMillis(), result.getTime());
    }

    @Test
    public void testRollCalendarWithTimeUnit() {
        Calendar result = Dates.roll(testCalendar, -3, TimeUnit.HOURS);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.getTimeInMillis() - TimeUnit.HOURS.toMillis(3), result.getTimeInMillis());
    }

    @Test
    public void testRollCalendarWithCalendarField() {
        Calendar result = Dates.roll(testCalendar, 1, CalendarField.YEAR);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.get(Calendar.YEAR) + 1, result.get(Calendar.YEAR));
    }

    @Test
    public void testRollDateNull() {
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 1, TimeUnit.DAYS));
    }

    @Test
    public void testRollCalendarNull() {
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 1, TimeUnit.DAYS));
    }

    @Test
    public void testAddYears() {
        java.util.Date result = Dates.addYears(testDate, 1);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(testDate);
        cal.add(Calendar.YEAR, 1);

        assertEquals(cal.getTimeInMillis(), result.getTime());
    }

    @Test
    public void testAddMonths() {
        java.util.Date result = Dates.addMonths(testDate, 3);
        assertNotNull(result);
        assertNotEquals(testDate, result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(testDate);
        cal.add(Calendar.MONTH, 3);

        assertEquals(cal.getTimeInMillis(), result.getTime());
    }

    @Test
    public void testAddWeeks() {
        java.util.Date result = Dates.addWeeks(testDate, 2);
        assertNotNull(result);
        assertNotEquals(testDate, result);
        assertEquals(testDate.getTime() + (2 * 7 * 24 * 60 * 60 * 1000L), result.getTime());
    }

    @Test
    public void testAddDays() {
        java.util.Date result = Dates.addDays(testDate, 10);
        assertNotNull(result);
        assertNotEquals(testDate, result);
        assertEquals(testDate.getTime() + (10 * 24 * 60 * 60 * 1000L), result.getTime());
    }

    @Test
    public void testAddHours() {
        java.util.Date result = Dates.addHours(testDate, 6);
        assertNotNull(result);
        assertNotEquals(testDate, result);
        assertEquals(testDate.getTime() + (6 * 60 * 60 * 1000L), result.getTime());
    }

    @Test
    public void testAddMinutes() {
        java.util.Date result = Dates.addMinutes(testDate, 30);
        assertNotNull(result);
        assertNotEquals(testDate, result);
        assertEquals(testDate.getTime() + (30 * 60 * 1000L), result.getTime());
    }

    @Test
    public void testAddSeconds() {
        java.util.Date result = Dates.addSeconds(testDate, 45);
        assertNotNull(result);
        assertNotEquals(testDate, result);
        assertEquals(testDate.getTime() + (45 * 1000L), result.getTime());
    }

    @Test
    public void testAddMilliseconds() {
        java.util.Date result = Dates.addMilliseconds(testDate, 500);
        assertNotNull(result);
        assertNotEquals(testDate, result);
        assertEquals(testDate.getTime() + 500, result.getTime());
    }

    @Test
    public void testAddYearsToCalendar() {
        Calendar result = Dates.addYears(testCalendar, 2);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.get(Calendar.YEAR) + 2, result.get(Calendar.YEAR));
    }

    @Test
    public void testAddMonthsToCalendar() {
        Calendar result = Dates.addMonths(testCalendar, 6);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
    }

    @Test
    public void testAddWeeksToCalendar() {
        Calendar result = Dates.addWeeks(testCalendar, 3);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.getTimeInMillis() + (3 * 7 * 24 * 60 * 60 * 1000L), result.getTimeInMillis());
    }

    @Test
    public void testAddDaysToCalendar() {
        Calendar result = Dates.addDays(testCalendar, 15);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.getTimeInMillis() + (15 * 24 * 60 * 60 * 1000L), result.getTimeInMillis());
    }

    @Test
    public void testAddHoursToCalendar() {
        Calendar result = Dates.addHours(testCalendar, 12);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.getTimeInMillis() + (12 * 60 * 60 * 1000L), result.getTimeInMillis());
    }

    @Test
    public void testAddMinutesToCalendar() {
        Calendar result = Dates.addMinutes(testCalendar, 45);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.getTimeInMillis() + (45 * 60 * 1000L), result.getTimeInMillis());
    }

    @Test
    public void testAddSecondsToCalendar() {
        Calendar result = Dates.addSeconds(testCalendar, 30);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.getTimeInMillis() + (30 * 1000L), result.getTimeInMillis());
    }

    @Test
    public void testAddMillisecondsToCalendar() {
        Calendar result = Dates.addMilliseconds(testCalendar, 250);
        assertNotNull(result);
        assertNotEquals(testCalendar, result);
        assertEquals(testCalendar.getTimeInMillis() + 250, result.getTimeInMillis());
    }

    @Test
    public void testRoundDate() {
        java.util.Date date = new java.util.Date();
        java.util.Date result = Dates.round(date, Calendar.HOUR_OF_DAY);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testRoundDateWithCalendarField() {
        java.util.Date date = new java.util.Date();
        java.util.Date result = Dates.round(date, CalendarField.DAY_OF_MONTH);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testRoundCalendar() {
        Calendar cal = Calendar.getInstance();
        Calendar result = Dates.round(cal, Calendar.MINUTE);
        assertNotNull(result);
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    @Test
    public void testRoundCalendarWithCalendarField() {
        Calendar cal = Calendar.getInstance();
        Calendar result = Dates.round(cal, CalendarField.HOUR_OF_DAY);
        assertNotNull(result);
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    @Test
    public void testRoundDateNull() {
        assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, Calendar.HOUR));
    }

    @Test
    public void testRoundCalendarNull() {
        assertThrows(IllegalArgumentException.class, () -> Dates.round((Calendar) null, Calendar.HOUR));
    }

    @Test
    public void testTruncateDate() {
        java.util.Date date = new java.util.Date();
        java.util.Date result = Dates.truncate(date, Calendar.DAY_OF_MONTH);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testTruncateDateWithCalendarField() {
        java.util.Date date = new java.util.Date();
        java.util.Date result = Dates.truncate(date, CalendarField.MONTH);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testTruncateCalendar() {
        Calendar cal = Calendar.getInstance();
        Calendar result = Dates.truncate(cal, Calendar.HOUR_OF_DAY);
        assertNotNull(result);
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    @Test
    public void testTruncateCalendarWithCalendarField() {
        Calendar cal = Calendar.getInstance();
        Calendar result = Dates.truncate(cal, CalendarField.YEAR);
        assertNotNull(result);
        assertEquals(0, result.get(Calendar.MONTH));
        assertEquals(1, result.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    @Test
    public void testCeilingDate() {
        java.util.Date date = new java.util.Date();
        java.util.Date result = Dates.ceiling(date, Calendar.MINUTE);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testCeilingDateWithCalendarField() {
        java.util.Date date = new java.util.Date();
        java.util.Date result = Dates.ceiling(date, CalendarField.DAY_OF_MONTH);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertTrue(result.getTime() >= date.getTime());
    }

    @Test
    public void testCeilingCalendar() {
        Calendar cal = Calendar.getInstance();
        Calendar result = Dates.ceiling(cal, Calendar.HOUR_OF_DAY);
        assertNotNull(result);
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    @Test
    public void testCeilingCalendarWithCalendarField() {
        Calendar cal = Calendar.getInstance();
        Calendar result = Dates.ceiling(cal, CalendarField.MONTH);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() >= cal.getTimeInMillis());
    }

    @Test
    public void testTruncatedEqualsCalendarsWithCalendarField() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.MINUTE, 1);

        assertTrue(Dates.truncatedEquals(cal1, cal2, CalendarField.HOUR_OF_DAY));
        assertFalse(Dates.truncatedEquals(cal1, cal2, CalendarField.MINUTE));
    }

    @Test
    public void testTruncatedEqualsCalendars() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.SECOND, 1);

        assertTrue(Dates.truncatedEquals(cal1, cal2, Calendar.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1, cal2, Calendar.SECOND));
    }

    @Test
    public void testTruncatedEqualsDatesWithCalendarField() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime() + 1000);

        assertTrue(Dates.truncatedEquals(date1, date2, CalendarField.MINUTE));
        assertFalse(Dates.truncatedEquals(date1, date2, CalendarField.SECOND));
    }

    @Test
    public void testTruncatedEqualsDates() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime() + 60000);

        assertTrue(Dates.truncatedEquals(date1, date2, Calendar.HOUR_OF_DAY));
        assertFalse(Dates.truncatedEquals(date1, date2, Calendar.MINUTE));
    }

    @Test
    public void testTruncatedCompareToCalendarsWithCalendarField() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.HOUR_OF_DAY, 1);

        assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, CalendarField.DAY_OF_MONTH));
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, CalendarField.HOUR_OF_DAY) < 0);
    }

    @Test
    public void testTruncatedCompareToCalendars() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.DAY_OF_MONTH, 1);

        assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, Calendar.MONTH));
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, Calendar.DAY_OF_MONTH) < 0);
    }

    @Test
    public void testTruncatedCompareToDatesWithCalendarField() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime() + 3600000);

        assertEquals(0, Dates.truncatedCompareTo(date1, date2, CalendarField.DAY_OF_MONTH));
        assertTrue(Dates.truncatedCompareTo(date1, date2, CalendarField.HOUR_OF_DAY) < 0);
    }

    @Test
    public void testTruncatedCompareToDates() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime() - 86400000);

        assertEquals(0, Dates.truncatedCompareTo(date1, date2, Calendar.YEAR));
        assertTrue(Dates.truncatedCompareTo(date1, date2, Calendar.DAY_OF_MONTH) > 0);
    }

    @Test
    public void testGetFragmentInMillisecondsDate() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInMilliseconds(date, CalendarField.SECOND);
        assertTrue(result >= 0 && result < 1000);
    }

    @Test
    public void testGetFragmentInSecondsDate() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInSeconds(date, CalendarField.MINUTE);
        assertTrue(result >= 0 && result < 60);
    }

    @Test
    public void testGetFragmentInMinutesDate() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY);
        assertTrue(result >= 0 && result < 60);
    }

    @Test
    public void testGetFragmentInHoursDate() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInHours(date, CalendarField.DAY_OF_MONTH);
        assertTrue(result >= 0 && result < 24);
    }

    @Test
    public void testGetFragmentInDaysDate() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInDays(date, CalendarField.MONTH);
        assertTrue(result >= 0 && result <= 31);
    }

    @Test
    public void testGetFragmentInMillisecondsCalendar() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND);
        assertTrue(result >= 0 && result < 1000);
    }

    @Test
    public void testGetFragmentInSecondsCalendar() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInSeconds(cal, CalendarField.MINUTE);
        assertTrue(result >= 0 && result < 60);
    }

    @Test
    public void testGetFragmentInMinutesCalendar() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY);
        assertTrue(result >= 0 && result < 60);
    }

    @Test
    public void testGetFragmentInHoursCalendar() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH);
        assertTrue(result >= 0 && result < 24);
    }

    @Test
    public void testGetFragmentInDaysCalendar() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInDays(cal, CalendarField.YEAR);
        assertTrue(result >= 0 && result <= 366);
    }

    @Test
    public void testIsSameDay() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime() + 36000);

        assertTrue(Dates.isSameDay(date1, date2));

        java.util.Date date3 = new java.util.Date(date1.getTime() + 86400000);
        assertFalse(Dates.isSameDay(date1, date3));
    }

    @Test
    public void testIsSameDayCalendars() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.MINUTE, 2);

        assertTrue(Dates.isSameDay(cal1, cal2));

        cal2.add(Calendar.DAY_OF_MONTH, 1);
        assertFalse(Dates.isSameDay(cal1, cal2));
    }

    @Test
    public void testIsSameDayNullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(null, new java.util.Date()));
    }

    @Test
    public void testIsSameDayNullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(null, Calendar.getInstance()));
    }

    @Test
    public void testIsSameMonth() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime() + 86400000);

        assertTrue(Dates.isSameMonth(date1, date2));

        Calendar cal = Calendar.getInstance();
        cal.setTime(date1);
        cal.add(Calendar.MONTH, 1);
        assertFalse(Dates.isSameMonth(date1, cal.getTime()));
    }

    @Test
    public void testIsSameMonthCalendars() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.DAY_OF_MONTH, 1);

        assertTrue(Dates.isSameMonth(cal1, cal2));

        cal2.add(Calendar.MONTH, 1);
        assertFalse(Dates.isSameMonth(cal1, cal2));
    }

    @Test
    public void testIsSameYear() {
        // Use a fixed date in the middle of the year to avoid year boundary issues
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JUNE, 15, 12, 0, 0);
        java.util.Date date1 = cal.getTime();
        cal.add(Calendar.MONTH, 1);
        java.util.Date date2 = cal.getTime();

        assertTrue(Dates.isSameYear(date1, date2));

        cal.add(Calendar.YEAR, 1);
        assertFalse(Dates.isSameYear(date1, cal.getTime()));
    }

    @Test
    public void testIsSameYearCalendars() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.SECOND, 11);

        assertTrue(Dates.isSameYear(cal1, cal2));

        cal2.add(Calendar.YEAR, 1);
        assertFalse(Dates.isSameYear(cal1, cal2));
    }

    @Test
    public void testIsSameInstant() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime());

        assertTrue(Dates.isSameInstant(date1, date2));

        java.util.Date date3 = new java.util.Date(date1.getTime() + 1);
        assertFalse(Dates.isSameInstant(date1, date3));
    }

    @Test
    public void testIsSameInstantCalendars() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();

        assertTrue(Dates.isSameInstant(cal1, cal2));

        cal2.add(Calendar.MILLISECOND, 1);
        assertFalse(Dates.isSameInstant(cal1, cal2));
    }

    @Test
    public void testIsSameLocalTime() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();

        assertTrue(Dates.isSameLocalTime(cal1, cal2));

        cal2.add(Calendar.SECOND, 1);
        assertFalse(Dates.isSameLocalTime(cal1, cal2));

        GregorianCalendar gcal = new GregorianCalendar();
        gcal.setTimeInMillis(cal1.getTimeInMillis());
        assertTrue(Dates.isSameLocalTime(cal1, gcal));
    }

    @Test
    public void testIsLastDateOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, Calendar.JANUARY, 31);
        assertTrue(Dates.isLastDateOfMonth(cal.getTime()));

        cal.set(2022, Calendar.JANUARY, 30);
        assertFalse(Dates.isLastDateOfMonth(cal.getTime()));
    }

    @Test
    public void testIsLastDateOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, Calendar.DECEMBER, 31);
        assertTrue(Dates.isLastDateOfYear(cal.getTime()));

        cal.set(2022, Calendar.DECEMBER, 30);
        assertFalse(Dates.isLastDateOfYear(cal.getTime()));
    }

    @Test
    public void testGetLastDateOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, Calendar.FEBRUARY, 15);
        assertEquals(28, Dates.getLastDateOfMonth(cal.getTime()));

        cal.set(2020, Calendar.FEBRUARY, 15);
        assertEquals(29, Dates.getLastDateOfMonth(cal.getTime()));

        cal.set(2022, Calendar.JANUARY, 1);
        assertEquals(31, Dates.getLastDateOfMonth(cal.getTime()));
    }

    @Test
    public void testGetLastDateOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, Calendar.JUNE, 15);
        assertEquals(365, Dates.getLastDateOfYear(cal.getTime()));

        cal.set(2020, Calendar.JUNE, 15);
        assertEquals(366, Dates.getLastDateOfYear(cal.getTime()));
    }

    @Test
    public void testIsOverlap() {
        java.util.Date start1 = new java.util.Date(1000);
        java.util.Date end1 = new java.util.Date(5000);
        java.util.Date start2 = new java.util.Date(3000);
        java.util.Date end2 = new java.util.Date(7000);

        assertTrue(Dates.isOverlap(start1, end1, start2, end2));

        java.util.Date start3 = new java.util.Date(6000);
        java.util.Date end3 = new java.util.Date(8000);
        assertFalse(Dates.isOverlap(start1, end1, start3, end3));
    }

    @Test
    public void testIsOverlapNullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlap(null, new java.util.Date(), new java.util.Date(), new java.util.Date()));
    }

    @Test
    public void testIsOverlapInvalidRange() {
        java.util.Date start = new java.util.Date(5000);
        java.util.Date end = new java.util.Date(1000);
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlap(start, end, new java.util.Date(), new java.util.Date()));
    }

    @Test
    public void testIsBetween() {
        java.util.Date date = new java.util.Date(3000);
        java.util.Date start = new java.util.Date(1000);
        java.util.Date end = new java.util.Date(5000);

        assertTrue(Dates.isBetween(date, start, end));

        java.util.Date beforeStart = new java.util.Date(500);
        assertFalse(Dates.isBetween(beforeStart, start, end));

        java.util.Date afterEnd = new java.util.Date(6000);
        assertFalse(Dates.isBetween(afterEnd, start, end));

        assertTrue(Dates.isBetween(start, start, end));
        assertTrue(Dates.isBetween(end, start, end));
    }

    @Test
    public void testIsBetweenNullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(null, new java.util.Date(), new java.util.Date()));
    }

    @Test
    public void testIsBetweenInvalidRange() {
        java.util.Date date = new java.util.Date();
        java.util.Date start = new java.util.Date(5000);
        java.util.Date end = new java.util.Date(1000);
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, start, end));
    }

    @Test
    public void testRegisterDateCreator() {
        class CustomDate extends java.util.Date {
            public CustomDate(long time) {
                super(time);
            }
        }

        boolean registered = Dates.registerDateCreator(CustomDate.class, CustomDate::new);
        assertFalse(registered);
    }

    @Test
    public void testRegisterCalendarCreator() {
        class CustomCalendar extends Calendar {
            public CustomCalendar() {
                super();
            }

            @Override
            protected void computeTime() {
            }

            @Override
            protected void computeFields() {
            }

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
            }

            @Override
            public int getMinimum(int field) {
                return 0;
            }

            @Override
            public int getMaximum(int field) {
                return 0;
            }

            @Override
            public int getGreatestMinimum(int field) {
                return 0;
            }

            @Override
            public int getLeastMaximum(int field) {
                return 0;
            }
        }

        boolean registered = Dates.registerCalendarCreator(CustomCalendar.class, (millis, cal) -> {
            CustomCalendar custom = new CustomCalendar();
            custom.setTimeInMillis(millis);
            return custom;
        });
        assertFalse(registered);
    }

    @Test
    public void testDTFLocalDate() {
        java.util.Date date = testDate;
        String result = Dates.DTF.LOCAL_DATE.format(date);
        assertEquals(LOCAL_DATE_STRING, result);
    }

    @Test
    public void testDTFLocalTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        String result = Dates.DTF.LOCAL_TIME.format(cal.getTime());
        assertEquals("13:45:30", result);
    }

    @Test
    public void testDTFLocalDateTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        String result = Dates.DTF.LOCAL_DATE_TIME.format(cal.getTime());
        assertTrue(result.contains("2022-01-01"));
        assertTrue(result.contains("13:45:30"));
    }

    @Test
    public void testDTFISOLocalDateTime() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        String result = Dates.DTF.ISO_LOCAL_DATE_TIME.format(cal.getTime());
        assertTrue(result.contains("2022-01-01T13:45:30"));
    }

    @Test
    public void testDTFISOOffsetDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 13, 45, 30);
        OffsetDateTime odt = ldt.atOffset(ZoneOffset.ofHours(-5));
        String result = Dates.DTF.ISO_OFFSET_DATE_TIME.format(odt);
        assertTrue(result.contains("2022-01-01T13:45:30-05:00"));
    }

    @Test
    public void testDTFISOZonedDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 13, 45, 30);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/New_York"));
        String result = Dates.DTF.ISO_ZONED_DATE_TIME.format(zdt);
        assertTrue(result.contains("2022-01-01T13:45:30"));
        assertTrue(result.contains("[America/New_York]"));
    }

    @Test
    public void testDTFISO8601DateTime() {
        java.util.Date date = new java.util.Date(FIXED_TIME_MILLIS);
        String result = Dates.DTF.ISO_8601_DATE_TIME.format(date);
        assertEquals(ISO_DATE_TIME_STRING, result);
    }

    @Test
    public void testDTFISO8601Timestamp() {
        Timestamp timestamp = new Timestamp(FIXED_TIME_MILLIS);
        String result = Dates.DTF.ISO_8601_TIMESTAMP.format(timestamp);
        assertEquals(ISO_TIMESTAMP_STRING, result);
    }

    @Test
    public void testDTFRFC1123DateTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2022, 0, 1, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        String result = Dates.DTF.RFC_1123_DATE_TIME.format(cal.getTime());
        assertTrue(result.matches("\\w{3}, \\d{2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} \\w+"));
    }

    @Test
    public void testDTFFormatNull() {
        assertNull(Dates.DTF.LOCAL_DATE.format((java.util.Date) null));
        assertNull(Dates.DTF.LOCAL_DATE.format((Calendar) null));
        assertNull(Dates.DTF.LOCAL_DATE.format((TemporalAccessor) null));
    }

    @Test
    public void testDTFFormatTo() {
        StringBuilder sb = new StringBuilder();
        Dates.DTF.LOCAL_DATE.formatTo(testDate, sb);
        assertEquals(LOCAL_DATE_STRING, sb.toString());
    }

    @Test
    public void testDTFFormatToNull() {
        StringBuilder sb = new StringBuilder();
        Dates.DTF.LOCAL_DATE.formatTo((java.util.Date) null, sb);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testDTFParseToLocalDate() {
        LocalDate result = Dates.DTF.LOCAL_DATE.parseToLocalDate(LOCAL_DATE_STRING);
        assertNotNull(result);
        assertEquals(2022, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
    }

    @Test
    public void testDTFParseToLocalDateNull() {
        assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate(null));
        assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate(""));
    }

    @Test
    public void testDTFParseToLocalDateLong() {
        LocalDate result = Dates.DTF.LOCAL_DATE.parseToLocalDate(String.valueOf(FIXED_TIME_MILLIS));
        assertNotNull(result);
    }

    @Test
    public void testDTFParseToLocalTime() {
        LocalTime result = Dates.DTF.LOCAL_TIME.parseToLocalTime("13:45:30");
        assertNotNull(result);
        assertEquals(13, result.getHour());
        assertEquals(45, result.getMinute());
        assertEquals(30, result.getSecond());
    }

    @Test
    public void testDTFParseToLocalDateTime() {
        LocalDateTime result = Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime(LOCAL_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(2022, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(0, result.getHour());
        assertEquals(0, result.getMinute());
        assertEquals(0, result.getSecond());
    }

    @Test
    public void testDTFParseToOffsetDateTime() {
        String offsetDateTimeStr = "2022-01-01T13:45:30+05:00";
        OffsetDateTime result = Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetDateTimeStr);
        assertNotNull(result);
        assertEquals(2022, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(13, result.getHour());
        assertEquals(45, result.getMinute());
        assertEquals(30, result.getSecond());
        assertEquals("+05:00", result.getOffset().toString());

        Dates.DTF.ISO_OFFSET_DATE_TIME.format(result);
    }

    @Test
    public void testDTFParseToZonedDateTime() {
        String zonedDateTimeStr = "2022-01-01T13:45:30-05:00[America/New_York]";
        ZonedDateTime result = Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zonedDateTimeStr);
        assertNotNull(result);
        assertEquals(2022, result.getYear());
        assertEquals(1, result.getMonthValue());
        assertEquals(1, result.getDayOfMonth());
        assertEquals(13, result.getHour());
        assertEquals(45, result.getMinute());
        assertEquals(30, result.getSecond());
        assertEquals("America/New_York", result.getZone().toString());

        Dates.DTF.ISO_ZONED_DATE_TIME.format(result);
    }

    @Test
    public void testDTFParseToInstant() {
        Instant result = Dates.DTF.ISO_8601_DATE_TIME.parseToInstant(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.toEpochMilli());

        N.println(Dates.DTF.ISO_8601_DATE_TIME.format(result.atZone(Dates.DEFAULT_ZONE_ID)));
        N.println(Dates.DTF.ISO_8601_DATE_TIME.format(result.atZone(Dates.UTC_ZONE_ID)));
    }

    @Test
    public void testDTFParseToJUDate() {
        java.util.Date result = Dates.DTF.ISO_8601_DATE_TIME.parseToJUDate(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testDTFParseToJUDateWithTimeZone() {
        java.util.Date result = Dates.DTF.ISO_8601_DATE_TIME.parseToJUDate(ISO_DATE_TIME_STRING, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testDTFParseToDate() {
        Date result = Dates.DTF.ISO_8601_DATE_TIME.parseToDate(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testDTFParseToDateWithTimeZone() {
        Date result = Dates.DTF.ISO_8601_DATE_TIME.parseToDate(ISO_DATE_TIME_STRING, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testDTFParseToTime() {
        Time result = Dates.DTF.LOCAL_TIME.parseToTime("13:45:30");
        assertNotNull(result);
    }

    @Test
    public void testDTFParseToTimeWithTimeZone() {
        Time result = Dates.DTF.ISO_8601_DATE_TIME.parseToTime(ISO_DATE_TIME_STRING, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testDTFParseToTimestamp() {
        Timestamp result = Dates.DTF.ISO_8601_TIMESTAMP.parseToTimestamp(ISO_TIMESTAMP_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testDTFParseToTimestampWithTimeZone() {
        Timestamp result = Dates.DTF.ISO_8601_TIMESTAMP.parseToTimestamp(ISO_TIMESTAMP_STRING, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTime());
    }

    @Test
    public void testDTFParseToCalendar() {
        Calendar result = Dates.DTF.ISO_8601_DATE_TIME.parseToCalendar(ISO_DATE_TIME_STRING);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
    }

    @Test
    public void testDTFParseToCalendarWithTimeZone() {
        Calendar result = Dates.DTF.ISO_8601_DATE_TIME.parseToCalendar(ISO_DATE_TIME_STRING, Dates.UTC_TIME_ZONE);
        assertNotNull(result);
        assertEquals(FIXED_TIME_MILLIS, result.getTimeInMillis());
        assertEquals(Dates.UTC_TIME_ZONE, result.getTimeZone());
    }

    @Test
    public void testDTFToString() {
        assertEquals(Dates.LOCAL_DATE_FORMAT, Dates.DTF.LOCAL_DATE.toString());
        assertEquals(Dates.LOCAL_TIME_FORMAT, Dates.DTF.LOCAL_TIME.toString());
        assertEquals(Dates.LOCAL_DATE_TIME_FORMAT, Dates.DTF.LOCAL_DATE_TIME.toString());
    }

    @Test
    public void testParseVariousDateFormats() {
        java.util.Date yearOnly = Dates.parseJUDate("2022");
        assertNotNull(yearOnly);

        java.util.Date monthDay = Dates.parseJUDate("01-15");
        assertNotNull(monthDay);

        java.util.Date rfc1123 = Dates.parseJUDate("Sat, 01 Jan 2022 00:00:00 GMT");
        assertNotNull(rfc1123);

        java.util.Date isoLocal = Dates.parseJUDate("2022-01-01T00:00:00");
        assertNotNull(isoLocal);

        java.util.Date isoOffset = Dates.parseJUDate("2022-01-01T00:00:00+05:00");
        assertNotNull(isoOffset);
    }

    @Test
    public void testParseInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseJUDate("invalid date string"));
    }

    @Test
    public void testFormatTimestamp() {
        Timestamp timestamp = new Timestamp(FIXED_TIME_MILLIS);
        String result = Dates.format(timestamp);
        assertEquals(ISO_TIMESTAMP_STRING, result);
    }

    @Test
    public void testCalendarFieldEnum() {
        assertEquals(Calendar.MILLISECOND, CalendarField.MILLISECOND.value());
        assertEquals(Calendar.SECOND, CalendarField.SECOND.value());
        assertEquals(Calendar.MINUTE, CalendarField.MINUTE.value());
        assertEquals(Calendar.HOUR_OF_DAY, CalendarField.HOUR_OF_DAY.value());
        assertEquals(Calendar.DAY_OF_MONTH, CalendarField.DAY_OF_MONTH.value());
        assertEquals(Calendar.WEEK_OF_YEAR, CalendarField.WEEK_OF_YEAR.value());
        assertEquals(Calendar.MONTH, CalendarField.MONTH.value());
        assertEquals(Calendar.YEAR, CalendarField.YEAR.value());
    }

    @Test
    public void testConstants() {
        assertNotNull(Dates.DEFAULT_TIME_ZONE);
        assertNotNull(Dates.UTC_TIME_ZONE);
        assertNotNull(Dates.GMT_TIME_ZONE);
        assertNotNull(Dates.DEFAULT_ZONE_ID);
        assertNotNull(Dates.UTC_ZONE_ID);
        assertNotNull(Dates.GMT_ZONE_ID);

        assertEquals("yyyy", Dates.LOCAL_YEAR_FORMAT);
        assertEquals("MM-dd", Dates.LOCAL_MONTH_DAY_FORMAT);
        assertEquals("yyyy-MM-dd", Dates.LOCAL_DATE_FORMAT);
        assertEquals("HH:mm:ss", Dates.LOCAL_TIME_FORMAT);
        assertEquals("yyyy-MM-dd HH:mm:ss", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss", Dates.ISO_LOCAL_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ssXXX", Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss'Z'", Dates.ISO_8601_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Dates.ISO_8601_TIMESTAMP_FORMAT);
        assertEquals("EEE, dd MMM yyyy HH:mm:ss zzz", Dates.RFC_1123_DATE_TIME_FORMAT);
        assertEquals(1001, Dates.SEMI_MONTH);
    }

    @Test
    public void testDateUtilClass() {
        assertTrue(Dates.class.isAssignableFrom(Dates.DateUtil.class));
    }

    @Test
    public void testConcurrentFormatting() throws InterruptedException {
        final int threadCount = 10;
        final int iterationsPerThread = 100;
        final java.util.Date testDate = new java.util.Date(FIXED_TIME_MILLIS);
        final String expectedResult = ISO_DATE_TIME_STRING;

        Thread[] threads = new Thread[threadCount];
        final boolean[] success = new boolean[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        String result = Dates.format(testDate);
                        assertEquals(expectedResult, result);
                    }
                    success[threadIndex] = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        for (boolean s : success) {
            assertTrue(s);
        }
    }
}

