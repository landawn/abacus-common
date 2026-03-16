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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class DatesTest extends TestBase {

    // ===== Constants =====

    @Test
    public void testConstants() {
        assertNotNull(Dates.DEFAULT_TIME_ZONE);
        assertNotNull(Dates.UTC_TIME_ZONE);
        assertNotNull(Dates.GMT_TIME_ZONE);
        assertNotNull(Dates.DEFAULT_ZONE_ID);
        assertNotNull(Dates.UTC_ZONE_ID);
        assertNotNull(Dates.GMT_ZONE_ID);

        assertEquals("UTC", Dates.UTC_TIME_ZONE.getID());
        assertEquals("GMT", Dates.GMT_TIME_ZONE.getID());
        assertEquals(Dates.UTC_TIME_ZONE.toZoneId(), Dates.UTC_ZONE_ID);
        assertEquals(Dates.GMT_TIME_ZONE.toZoneId(), Dates.GMT_ZONE_ID);

        assertEquals("yyyy", Dates.LOCAL_YEAR_FORMAT);
        assertEquals("MM-dd", Dates.LOCAL_MONTH_DAY_FORMAT);
        assertEquals("yyyy-MM-dd", Dates.LOCAL_DATE_FORMAT);
        assertEquals("HH:mm:ss", Dates.LOCAL_TIME_FORMAT);
        assertEquals("yyyy-MM-dd HH:mm:ss", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd HH:mm:ss.SSS", Dates.LOCAL_TIMESTAMP_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss", Dates.ISO_LOCAL_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ssXXX", Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss'Z'", Dates.ISO_8601_DATE_TIME_FORMAT);
        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Dates.ISO_8601_TIMESTAMP_FORMAT);
        assertEquals("EEE, dd MMM yyyy HH:mm:ss zzz", Dates.RFC_1123_DATE_TIME_FORMAT);
        assertEquals(1001, Dates.SEMI_MONTH);
    }

    // ===== registerDateCreator =====

    @Test
    public void testRegisterDateCreator() {
        boolean result = Dates.registerDateCreator(java.util.Date.class, (millis) -> new java.util.Date(millis));
        assertFalse(result);

        assertNotNull(Dates.registerDateCreator(java.util.Date.class, java.util.Date::new));
    }

    @Test
    public void testRegisterDateCreator_customClass() {
        class CustomDate extends java.util.Date {
            public CustomDate(long time) {
                super(time);
            }
        }

        boolean registered = Dates.registerDateCreator(CustomDate.class, CustomDate::new);
        assertFalse(registered);
    }

    // ===== registerCalendarCreator =====

    @Test
    public void testRegisterCalendarCreator() {
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
    public void testRegisterCalendarCreator_customClass() {
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

    // ===== currentTimeMillis =====

    @Test
    public void testCurrentTimeMillis() {
        long start = System.currentTimeMillis();
        long dCurrent = Dates.currentTimeMillis();
        long end = System.currentTimeMillis();
        assertTrue(dCurrent >= start && dCurrent <= end);
    }

    // ===== currentTime =====

    @Test
    public void testCurrentTime() {
        Time time = Dates.currentTime();
        assertNotNull(time);
        assertTrue(time instanceof java.sql.Time);
        assertTrue(time.getTime() > 0);
    }

    // ===== currentDate =====

    @Test
    public void testCurrentDate() {
        java.sql.Date date = Dates.currentDate();
        assertNotNull(date);
        assertTrue(date instanceof java.sql.Date);
        assertTrue(date.getTime() > 0);
    }

    // ===== currentTimestamp =====

    @Test
    public void testCurrentTimestamp() {
        Timestamp timestamp = Dates.currentTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp instanceof java.sql.Timestamp);
        assertTrue(timestamp.getTime() > 0);
    }

    // ===== currentJUDate =====

    @Test
    public void testCurrentJUDate() {
        java.util.Date date = Dates.currentJUDate();
        assertNotNull(date);
        assertTrue(date instanceof java.util.Date);
        assertTrue(date.getTime() > 0);
    }

    // ===== currentCalendar =====

    @Test
    public void testCurrentCalendar() {
        Calendar cal = Dates.currentCalendar();
        assertNotNull(cal);
        assertTrue(cal instanceof Calendar);
        assertTrue(cal.getTimeInMillis() > 0);
    }

    // ===== currentGregorianCalendar =====

    @Test
    public void testCurrentGregorianCalendar() {
        GregorianCalendar cal = Dates.currentGregorianCalendar();
        assertNotNull(cal);
        assertTrue(cal instanceof GregorianCalendar);
        assertTrue(cal.getTimeInMillis() > 0);
    }

    // ===== currentXMLGregorianCalendar =====

    @Test
    public void testCurrentXMLGregorianCalendar() {
        XMLGregorianCalendar cal = Dates.currentXMLGregorianCalendar();
        assertNotNull(cal);
        assertTrue(cal.getYear() >= 2025);
    }

    // ===== currentTimePlus =====

    @Test
    public void testCurrentTimePlus() {
        long now = System.currentTimeMillis();
        Time rolled = Dates.currentTimePlus(1, TimeUnit.SECONDS);
        assertTrue(Math.abs(rolled.getTime() - (now + 1000)) < 100);
    }

    @Test
    public void testCurrentTimePlus_negative() {
        Time currentTime = Dates.currentTime();
        Time pastTime = Dates.currentTimePlus(-5, TimeUnit.MINUTES);
        assertTrue(pastTime.getTime() < currentTime.getTime());
    }

    // ===== currentDatePlus =====

    @Test
    public void testCurrentDatePlus() {
        java.sql.Date futureDate = Dates.currentDatePlus(1, TimeUnit.DAYS);
        java.sql.Date currentDate = Dates.currentDate();
        assertTrue(futureDate.getTime() > currentDate.getTime());
    }

    @Test
    public void testCurrentDatePlus_negative() {
        java.sql.Date pastDate = Dates.currentDatePlus(-1, TimeUnit.DAYS);
        java.sql.Date currentDate = Dates.currentDate();
        assertTrue(pastDate.getTime() < currentDate.getTime());
    }

    // ===== currentTimestampPlus =====

    @Test
    public void testCurrentTimestampPlus() {
        long now = System.currentTimeMillis();
        Timestamp rolled = Dates.currentTimestampPlus(-1, TimeUnit.HOURS);
        assertTrue(Math.abs(rolled.getTime() - (now - TimeUnit.HOURS.toMillis(1))) < 100);
    }

    @Test
    public void testCurrentTimestampPlus_positive() {
        Timestamp futureTs = Dates.currentTimestampPlus(10, TimeUnit.SECONDS);
        Timestamp currentTs = Dates.currentTimestamp();
        assertTrue(futureTs.getTime() > currentTs.getTime());
    }

    // ===== currentJUDatePlus =====

    @Test
    public void testCurrentJUDatePlus() {
        java.util.Date rolled = Dates.currentJUDatePlus(5, TimeUnit.MINUTES);
        long now = System.currentTimeMillis();
        N.println(rolled.getTime() - (now + TimeUnit.MINUTES.toMillis(5)));
        assertTrue(Math.abs(rolled.getTime() - (now + TimeUnit.MINUTES.toMillis(5))) < 100);
    }

    @Test
    public void testCurrentJUDatePlus_negative() {
        java.util.Date pastDate = Dates.currentJUDatePlus(-2, TimeUnit.HOURS);
        java.util.Date currentDate = Dates.currentJUDate();
        assertTrue(pastDate.getTime() < currentDate.getTime());
    }

    // ===== currentCalendarPlus =====

    @Test
    public void testCurrentCalendarPlus() {
        long now = System.currentTimeMillis();
        Calendar rolled = Dates.currentCalendarPlus(10, TimeUnit.MILLISECONDS);
        assertTrue(Math.abs(rolled.getTimeInMillis() - (now + 10)) < 100);
    }

    @Test
    public void testCurrentCalendarPlus_negative() {
        Calendar pastCal = Dates.currentCalendarPlus(-3, TimeUnit.HOURS);
        Calendar currentCal = Dates.currentCalendar();
        assertTrue(pastCal.getTimeInMillis() < currentCal.getTimeInMillis());
    }

    // ===== createJUDate =====

    @Test
    public void testCreateJUDate_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        java.util.Date date = Dates.createJUDate(cal);
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((Calendar) null));
    }

    @Test
    public void testCreateJUDate_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        java.util.Date created = Dates.createJUDate(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((java.util.Date) null));
    }

    @Test
    public void testCreateJUDate_fromMillis() {
        long millis = 3000000000L;
        java.util.Date date = Dates.createJUDate(millis);

        assertNotNull(date);
        assertEquals(millis, date.getTime());
    }

    @Test
    public void testCreateJUDate_zeroMillis() {
        java.util.Date date = Dates.createJUDate(0L);
        assertNotNull(date);
        assertEquals(0L, date.getTime());
    }

    @Test
    public void testCreateJUDate_negativeMillis() {
        long negativeMillis = -1000000000L;
        java.util.Date date = Dates.createJUDate(negativeMillis);
        assertNotNull(date);
        assertEquals(negativeMillis, date.getTime());
    }

    @Test
    public void testCreateJUDate_veryLargeMillis() {
        long largeMillis = Long.MAX_VALUE / 2;
        java.util.Date date = Dates.createJUDate(largeMillis);
        assertNotNull(date);
        assertEquals(largeMillis, date.getTime());
    }

    // ===== createDate =====

    @Test
    public void testCreateDate_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        java.sql.Date date = Dates.createDate(cal);
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((Calendar) null));
    }

    @Test
    public void testCreateDate_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        java.sql.Date created = Dates.createDate(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((java.util.Date) null));
    }

    @Test
    public void testCreateDate_fromMillis() {
        long millis = 3000000000L;
        java.sql.Date date = Dates.createDate(millis);

        assertNotNull(date);
        assertEquals(millis, date.getTime());
    }

    // ===== createTime =====

    @Test
    public void testCreateTime_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        Time time = Dates.createTime(cal);
        assertNotNull(time);
        assertEquals(1000000000L, time.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((Calendar) null));
    }

    @Test
    public void testCreateTime_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        Time created = Dates.createTime(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((java.util.Date) null));
    }

    @Test
    public void testCreateTime_fromMillis() {
        long millis = 3000000000L;
        Time time = Dates.createTime(millis);

        assertNotNull(time);
        assertEquals(millis, time.getTime());
    }

    // ===== createTimestamp =====

    @Test
    public void testCreateTimestamp_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        Timestamp timestamp = Dates.createTimestamp(cal);
        assertNotNull(timestamp);
        assertEquals(1000000000L, timestamp.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((Calendar) null));
    }

    @Test
    public void testCreateTimestamp_fromDate() {
        java.util.Date original = new java.util.Date(2000000000L);
        Timestamp created = Dates.createTimestamp(original);

        assertNotNull(created);
        assertEquals(original.getTime(), created.getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((java.util.Date) null));
    }

    @Test
    public void testCreateTimestamp_fromMillis() {
        long millis = 3000000000L;
        Timestamp timestamp = Dates.createTimestamp(millis);

        assertNotNull(timestamp);
        assertEquals(millis, timestamp.getTime());
    }

    // ===== createCalendar =====

    @Test
    public void testCreateCalendar_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        Calendar created = Dates.createCalendar(cal);
        assertNotNull(created);
        assertEquals(1000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((Calendar) null));
    }

    @Test
    public void testCreateCalendar_fromDate() {
        java.util.Date date = new java.util.Date(2000000000L);
        Calendar created = Dates.createCalendar(date);

        assertNotNull(created);
        assertEquals(2000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((java.util.Date) null));
    }

    @Test
    public void testCreateCalendar_fromMillis() {
        long millis = 3000000000L;
        Calendar cal = Dates.createCalendar(millis);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void testCreateCalendar_fromMillisWithTimeZone() {
        long millis = 3000000000L;
        TimeZone tz = TimeZone.getTimeZone("America/New_York");

        Calendar cal = Dates.createCalendar(millis, tz);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(tz, cal.getTimeZone());
    }

    @Test
    public void testCreateCalendar_fromMillisWithNullTimeZone() {
        long millis = 3000000000L;
        Calendar cal = Dates.createCalendar(millis, null);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void testCreateCalendar_withUTC() {
        long millis = 1000000000L;
        Calendar cal = Dates.createCalendar(millis, Dates.UTC_TIME_ZONE);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(Dates.UTC_TIME_ZONE, cal.getTimeZone());
    }

    @Test
    public void testCreateCalendar_differentTimeZones() {
        long millis = 1000000000L;
        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");

        Calendar calUtc = Dates.createCalendar(millis, utc);
        Calendar calTokyo = Dates.createCalendar(millis, tokyo);

        assertEquals(millis, calUtc.getTimeInMillis());
        assertEquals(millis, calTokyo.getTimeInMillis());
    }

    // ===== createGregorianCalendar =====

    @Test
    public void testCreateGregorianCalendar_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        GregorianCalendar created = Dates.createGregorianCalendar(cal);
        assertNotNull(created);
        assertEquals(1000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createGregorianCalendar((Calendar) null));
    }

    @Test
    public void testCreateGregorianCalendar_fromDate() {
        java.util.Date date = new java.util.Date(2000000000L);
        GregorianCalendar created = Dates.createGregorianCalendar(date);

        assertNotNull(created);
        assertEquals(2000000000L, created.getTimeInMillis());

        assertThrows(IllegalArgumentException.class, () -> Dates.createGregorianCalendar((java.util.Date) null));
    }

    @Test
    public void testCreateGregorianCalendar_fromMillis() {
        long millis = 3000000000L;
        GregorianCalendar cal = Dates.createGregorianCalendar(millis);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void testCreateGregorianCalendar_fromMillisWithTimeZone() {
        long millis = 3000000000L;
        TimeZone tz = TimeZone.getTimeZone("Europe/London");

        GregorianCalendar cal = Dates.createGregorianCalendar(millis, tz);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(tz, cal.getTimeZone());
    }

    @Test
    public void testCreateGregorianCalendar_fromMillisWithNullTimeZone() {
        long millis = 3000000000L;
        GregorianCalendar cal = Dates.createGregorianCalendar(millis, null);

        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
    }

    @Test
    public void testCreateGregorianCalendar_withGMT() {
        long millis = 1000000000L;
        GregorianCalendar cal = Dates.createGregorianCalendar(millis, Dates.GMT_TIME_ZONE);
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(Dates.GMT_TIME_ZONE, cal.getTimeZone());
    }

    // ===== createXMLGregorianCalendar =====

    @Test
    public void testCreateXMLGregorianCalendar_fromCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);

        XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(cal);
        assertNotNull(created);

        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar((Calendar) null));
    }

    @Test
    public void testCreateXMLGregorianCalendar_fromDate() {
        java.util.Date date = new java.util.Date(2000000000L);
        XMLGregorianCalendar created = Dates.createXMLGregorianCalendar(date);

        assertNotNull(created);

        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar((java.util.Date) null));
    }

    @Test
    public void testCreateXMLGregorianCalendar_fromMillis() {
        long millis = 3000000000L;
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis);

        assertNotNull(cal);
    }

    @Test
    public void testCreateXMLGregorianCalendar_fromMillisWithTimeZone() {
        long millis = 3000000000L;
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");

        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis, tz);
        assertNotNull(cal);
    }

    @Test
    public void testCreateXMLGregorianCalendar_fromMillisWithNullTimeZone() {
        long millis = 3000000000L;
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis, null);

        assertNotNull(cal);
    }

    @Test
    public void testCreateXMLGregorianCalendar_withGMT() {
        long millis = 1000000000L;
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis, Dates.GMT_TIME_ZONE);
        assertNotNull(cal);
    }

    // ===== parseJUDate =====

    @Test
    public void testParseJUDate() {
        assertNull(Dates.parseJUDate((String) null));
        assertNull(Dates.parseJUDate(""));
        assertNull(Dates.parseJUDate("null"));

        java.util.Date date = Dates.parseJUDate("1000000000");
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());
    }

    @Test
    public void testParseJUDate_numericStringZero() {
        java.util.Date date = Dates.parseJUDate("0");
        assertNotNull(date);
        assertEquals(0L, date.getTime());
    }

    @Test
    public void testParseJUDate_variousFormats() {
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
    public void testParseJUDate_invalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseJUDate("invalid date string"));
    }

    @Test
    public void testParseJUDate_withFormat() {
        assertNull(Dates.parseJUDate(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseJUDate("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseJUDate("null", Dates.LOCAL_DATE_FORMAT));

        java.util.Date date = Dates.parseJUDate("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_withIsoFormat() {
        java.util.Date date = Dates.parseJUDate("2025-10-04T14:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_withRfc1123Format() {
        java.util.Date date = Dates.parseJUDate("Sat, 04 Oct 2025 14:30:45 GMT", Dates.RFC_1123_DATE_TIME_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_withFormatAndTimeZone() {
        assertNull(Dates.parseJUDate(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseJUDate("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseJUDate("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        java.util.Date date = Dates.parseJUDate("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    // ===== parseDate =====

    @Test
    public void testParseDate() {
        assertNull(Dates.parseDate((String) null));
        assertNull(Dates.parseDate(""));
        assertNull(Dates.parseDate("null"));

        java.sql.Date date = Dates.parseDate("1000000000");
        assertNotNull(date);
        assertEquals(1000000000L, date.getTime());
    }

    @Test
    public void testParseDate_withFormat() {
        assertNull(Dates.parseDate(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseDate("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseDate("null", Dates.LOCAL_DATE_FORMAT));

        java.sql.Date date = Dates.parseDate("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseDate_withFormatAndTimeZone() {
        assertNull(Dates.parseDate(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseDate("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseDate("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        java.sql.Date date = Dates.parseDate("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    // ===== parseTime =====

    @Test
    public void testParseTime() {
        assertNull(Dates.parseTime((String) null));
        assertNull(Dates.parseTime(""));
        assertNull(Dates.parseTime("null"));

        Time time = Dates.parseTime("1000000000");
        assertNotNull(time);
        assertEquals(1000000000L, time.getTime());
    }

    @Test
    public void testParseTime_withFormat() {
        assertNull(Dates.parseTime(null, Dates.LOCAL_TIME_FORMAT));
        assertNull(Dates.parseTime("", Dates.LOCAL_TIME_FORMAT));
        assertNull(Dates.parseTime("null", Dates.LOCAL_TIME_FORMAT));

        Time time = Dates.parseTime("14:30:45", Dates.LOCAL_TIME_FORMAT);
        assertNotNull(time);
    }

    @Test
    public void testParseTime_withFormatAndTimeZone() {
        assertNull(Dates.parseTime(null, Dates.LOCAL_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTime("", Dates.LOCAL_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTime("null", Dates.LOCAL_TIME_FORMAT, TimeZone.getDefault()));

        Time time = Dates.parseTime("14:30:45", Dates.LOCAL_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(time);
    }

    // ===== parseTimestamp =====

    @Test
    public void testParseTimestamp() {
        assertNull(Dates.parseTimestamp((String) null));
        assertNull(Dates.parseTimestamp(""));
        assertNull(Dates.parseTimestamp("null"));

        Timestamp timestamp = Dates.parseTimestamp("1000000000");
        assertNotNull(timestamp);
        assertEquals(1000000000L, timestamp.getTime());
    }

    @Test
    public void testParseTimestamp_withFormat() {
        assertNull(Dates.parseTimestamp(null, Dates.LOCAL_DATE_TIME_FORMAT));
        assertNull(Dates.parseTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT));
        assertNull(Dates.parseTimestamp("null", Dates.LOCAL_DATE_TIME_FORMAT));

        Timestamp timestamp = Dates.parseTimestamp("2025-10-04 14:30:45", Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(timestamp);
    }

    @Test
    public void testParseTimestamp_withIsoFormat() {
        Timestamp ts = Dates.parseTimestamp("2025-10-04T14:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT);
        assertNotNull(ts);
    }

    @Test
    public void testParseTimestamp_withFormatAndTimeZone() {
        assertNull(Dates.parseTimestamp(null, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseTimestamp("null", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));

        Timestamp timestamp = Dates.parseTimestamp("2025-10-04 14:30:45", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(timestamp);
    }

    // ===== parseCalendar =====

    @Test
    public void testParseCalendar() {
        assertNull(Dates.parseCalendar((String) null));
        assertNull(Dates.parseCalendar(""));
        assertNull(Dates.parseCalendar("null"));

        Calendar calendar = Dates.parseCalendar("1000000000");
        assertNotNull(calendar);
        assertEquals(1000000000L, calendar.getTimeInMillis());
    }

    @Test
    public void testParseCalendar_withFormat() {
        assertNull(Dates.parseCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseCalendar("null", Dates.LOCAL_DATE_FORMAT));

        Calendar calendar = Dates.parseCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void testParseCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        Calendar calendar = Dates.parseCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void testParseCalendar_withUTCTimeZone() {
        Calendar calendar = Dates.parseCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(calendar);
        assertEquals(Dates.UTC_TIME_ZONE, calendar.getTimeZone());
    }

    // ===== parseGregorianCalendar =====

    @Test
    public void testParseGregorianCalendar() {
        assertNull(Dates.parseGregorianCalendar((String) null));
        assertNull(Dates.parseGregorianCalendar(""));
        assertNull(Dates.parseGregorianCalendar("null"));

        GregorianCalendar calendar = Dates.parseGregorianCalendar("1000000000");
        assertNotNull(calendar);
        assertEquals(1000000000L, calendar.getTimeInMillis());
    }

    @Test
    public void testParseGregorianCalendar_withFormat() {
        assertNull(Dates.parseGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseGregorianCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT));

        GregorianCalendar calendar = Dates.parseGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void testParseGregorianCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseGregorianCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        GregorianCalendar calendar = Dates.parseGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    // ===== parseXMLGregorianCalendar =====

    @Test
    public void testParseXMLGregorianCalendar() {
        assertNull(Dates.parseXMLGregorianCalendar((String) null));
        assertNull(Dates.parseXMLGregorianCalendar(""));
        assertNull(Dates.parseXMLGregorianCalendar("null"));

        XMLGregorianCalendar calendar = Dates.parseXMLGregorianCalendar("1000000000");
        assertNotNull(calendar);
    }

    @Test
    public void testParseXMLGregorianCalendar_withFormat() {
        assertNull(Dates.parseXMLGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseXMLGregorianCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseXMLGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT));

        XMLGregorianCalendar calendar = Dates.parseXMLGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void testParseXMLGregorianCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseXMLGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseXMLGregorianCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseXMLGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        XMLGregorianCalendar calendar = Dates.parseXMLGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    // ===== formatLocalDate =====

    @Test
    public void testFormatLocalDate() {
        String formatted = Dates.formatLocalDate();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    // ===== formatLocalDateTime =====

    @Test
    public void testFormatLocalDateTime() {
        String formatted = Dates.formatLocalDateTime();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    // ===== formatCurrentDateTime =====

    @Test
    public void testFormatCurrentDateTime() {
        String formatted = Dates.formatCurrentDateTime();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
    }

    // ===== formatCurrentTimestamp =====

    @Test
    public void testFormatCurrentTimestamp() {
        String formatted = Dates.formatCurrentTimestamp();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    // ===== format(Date) =====

    @Test
    public void testFormat_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_date_null() {
        assertNull(Dates.format((java.util.Date) null));
    }

    @Test
    public void testFormat_date_withFormat() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_date_nullWithFormat() {
        assertNull(Dates.format((java.util.Date) null, Dates.LOCAL_DATE_FORMAT));
    }

    @Test
    public void testFormat_date_withFormatAndTimeZone() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_date_nullWithFormatAndTimeZone() {
        assertNull(Dates.format((java.util.Date) null, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
    }

    @Test
    public void testFormat_date_withTimestampFormat() {
        Timestamp ts = new Timestamp(1000000123L);
        String formatted = Dates.format(ts, Dates.LOCAL_TIMESTAMP_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}"));
    }

    @Test
    public void testFormat_date_isoOffsetFormat() {
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = Dates.format(date, Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_timestamp_default() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
        assertTrue(formatted.endsWith("Z"));
    }

    // ===== format(Calendar) =====

    @Test
    public void testFormat_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_calendar_null() {
        assertNull(Dates.format((Calendar) null));
    }

    @Test
    public void testFormat_calendar_withFormat() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_calendar_nullWithFormat() {
        assertNull(Dates.format((Calendar) null, Dates.LOCAL_DATE_FORMAT));
    }

    @Test
    public void testFormat_calendar_withFormatAndTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_calendar_nullWithFormatAndTimeZone() {
        assertNull(Dates.format((Calendar) null, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
    }

    // ===== format(XMLGregorianCalendar) =====

    @Test
    public void testFormat_xmlGregorianCalendar() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testFormat_xmlGregorianCalendar_null() {
        assertNull(Dates.format((XMLGregorianCalendar) null));
    }

    @Test
    public void testFormat_xmlGregorianCalendar_withFormat() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_xmlGregorianCalendar_nullWithFormat() {
        assertNull(Dates.format((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT));
    }

    @Test
    public void testFormat_xmlGregorianCalendar_withFormatAndTimeZone() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        String formatted = Dates.format(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormat_xmlGregorianCalendar_nullWithFormatAndTimeZone() {
        assertNull(Dates.format((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC")));
    }

    // ===== formatTo(Date) =====

    @Test
    public void testFormatTo_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(date, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_date_null() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((java.util.Date) null, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testFormatTo_date_withFormat() {
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(date, Dates.LOCAL_DATE_FORMAT, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_date_nullWithFormat() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((java.util.Date) null, Dates.LOCAL_DATE_FORMAT, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testFormatTo_date_withFormatAndTimeZone() {
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(date, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_date_nullWithFormatAndTimeZone() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((java.util.Date) null, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertEquals("null", sb.toString());
    }

    // ===== formatTo(Calendar) =====

    @Test
    public void testFormatTo_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_calendar_null() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((Calendar) null, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testFormatTo_calendar_withFormat() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_calendar_nullWithFormat() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((Calendar) null, Dates.LOCAL_DATE_FORMAT, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testFormatTo_calendar_withFormatAndTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_calendar_nullWithFormatAndTimeZone() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((Calendar) null, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertEquals("null", sb.toString());
    }

    // ===== formatTo(XMLGregorianCalendar) =====

    @Test
    public void testFormatTo_xmlGregorianCalendar() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_xmlGregorianCalendar_null() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((XMLGregorianCalendar) null, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testFormatTo_xmlGregorianCalendar_withFormat() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_xmlGregorianCalendar_nullWithFormat() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testFormatTo_xmlGregorianCalendar_withFormatAndTimeZone() {
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo(cal, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testFormatTo_xmlGregorianCalendar_nullWithFormatAndTimeZone() {
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> Dates.formatTo((XMLGregorianCalendar) null, Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"), sb));
        assertEquals("null", sb.toString());
    }

    // ===== setYears =====

    @Test
    public void testSetYears() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setYears(date, 2025);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(2025, cal.get(Calendar.YEAR));
    }

    @Test
    public void testSetYears_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(null, 2023));
    }

    // ===== setMonths =====

    @Test
    public void testSetMonths() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setMonths(date, 5);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(5, cal.get(Calendar.MONTH));
    }

    @Test
    public void testSetMonths_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(null, 5));
    }

    // ===== setDays =====

    @Test
    public void testSetDays() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setDays(date, 15);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testSetDays_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(null, 15));
    }

    // ===== setHours =====

    @Test
    public void testSetHours() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setHours(date, 14);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testSetHours_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setHours(null, 14));
    }

    // ===== setMinutes =====

    @Test
    public void testSetMinutes() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setMinutes(date, 30);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(30, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testSetMinutes_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setMinutes(null, 30));
    }

    // ===== setSeconds =====

    @Test
    public void testSetSeconds() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setSeconds(date, 45);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(45, cal.get(Calendar.SECOND));
    }

    @Test
    public void testSetSeconds_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setSeconds(null, 45));
    }

    // ===== setMilliseconds =====

    @Test
    public void testSetMilliseconds() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.setMilliseconds(date, 123);
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testSetMilliseconds_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setMilliseconds(null, 123));
    }

    // ===== roll(Date, TimeUnit) =====

    @Test
    public void testRoll_date_withTimeUnit() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.roll(date, 5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testRoll_date_withTimeUnit_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.roll(date, -5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testRoll_date_withTimeUnit_zero() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.roll(date, 0, TimeUnit.DAYS);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getTime());
    }

    @Test
    public void testRoll_date_withTimeUnit_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 5, TimeUnit.DAYS));
    }

    // ===== roll(Date, CalendarField) =====

    @Test
    public void testRoll_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.roll(date, 5, CalendarField.DAY_OF_MONTH);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testRoll_date_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 5, CalendarField.DAY_OF_MONTH));
    }

    // ===== roll(Calendar, TimeUnit) =====

    @Test
    public void testRoll_calendar_withTimeUnit() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.roll(cal, 5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > 1000000000L);
    }

    @Test
    public void testRoll_calendar_withTimeUnit_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.roll(cal, -5, TimeUnit.DAYS);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < 1000000000L);
    }

    @Test
    public void testRoll_calendar_withTimeUnit_zero() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.roll(cal, 0, TimeUnit.DAYS);
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.getTimeInMillis());
    }

    @Test
    public void testRoll_calendar_withTimeUnit_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 5, TimeUnit.DAYS));
    }

    // ===== roll(Calendar, CalendarField) =====

    @Test
    public void testRoll_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.roll(cal, 5, CalendarField.DAY_OF_MONTH);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > 1000000000L);
    }

    @Test
    public void testRoll_calendar_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 5, CalendarField.DAY_OF_MONTH));
    }

    // ===== addYears(Date) =====

    @Test
    public void testAddYears_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addYears(date, 1);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testAddYears_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addYears(date, -1);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testAddYears_date_zero() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addYears(date, 0);
        assertNotNull(result);
        assertEquals(date.getTime(), result.getTime());
    }

    @Test
    public void testAddYears_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addYears((java.util.Date) null, 1));
    }

    // ===== addMonths(Date) =====

    @Test
    public void testAddMonths_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMonths(date, 3);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testAddMonths_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMonths(date, -3);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testAddMonths_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addMonths((java.util.Date) null, 1));
    }

    // ===== addWeeks(Date) =====

    @Test
    public void testAddWeeks_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addWeeks(date, 2);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testAddWeeks_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addWeeks(date, -2);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testAddWeeks_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addWeeks((java.util.Date) null, 1));
    }

    // ===== addDays(Date) =====

    @Test
    public void testAddDays_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addDays(date, 7);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testAddDays_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addDays(date, -7);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testAddDays_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addDays((java.util.Date) null, 1));
    }

    @Test
    public void testAddDays_date_boundaryFebToMarch() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.FEBRUARY, 28, 23, 59, 59);
        java.util.Date date = Dates.addDays(cal.getTime(), 1);

        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(date);
        assertEquals(Calendar.MARCH, resultCal.get(Calendar.MONTH));
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    // ===== addHours(Date) =====

    @Test
    public void testAddHours_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addHours(date, 5);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testAddHours_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addHours(date, -5);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testAddHours_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addHours((java.util.Date) null, 1));
    }

    // ===== addMinutes(Date) =====

    @Test
    public void testAddMinutes_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMinutes(date, 30);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testAddMinutes_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMinutes(date, -30);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testAddMinutes_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addMinutes((java.util.Date) null, 1));
    }

    // ===== addSeconds(Date) =====

    @Test
    public void testAddSeconds_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addSeconds(date, 45);
        assertNotNull(result);
        assertTrue(result.getTime() > date.getTime());
    }

    @Test
    public void testAddSeconds_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addSeconds(date, -45);
        assertNotNull(result);
        assertTrue(result.getTime() < date.getTime());
    }

    @Test
    public void testAddSeconds_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addSeconds((java.util.Date) null, 1));
    }

    // ===== addMilliseconds(Date) =====

    @Test
    public void testAddMilliseconds_date() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMilliseconds(date, 500);
        assertNotNull(result);
        assertEquals(500, result.getTime() - date.getTime());
    }

    @Test
    public void testAddMilliseconds_date_negative() {
        java.util.Date date = new java.util.Date(1000000000L);
        java.util.Date result = Dates.addMilliseconds(date, -500);
        assertNotNull(result);
        assertEquals(500, date.getTime() - result.getTime());
    }

    @Test
    public void testAddMilliseconds_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addMilliseconds((java.util.Date) null, 1));
    }

    // ===== addYears(Calendar) =====

    @Test
    public void testAddYears_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addYears(cal, 1);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void testAddYears_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addYears(cal, -1);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddYears_calendar_zero() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addYears(cal, 0);
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.getTimeInMillis());
    }

    @Test
    public void testAddYears_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addYears((Calendar) null, 1));
    }

    // ===== addMonths(Calendar) =====

    @Test
    public void testAddMonths_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMonths(cal, 3);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void testAddMonths_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMonths(cal, -3);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddMonths_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addMonths((Calendar) null, 1));
    }

    // ===== addWeeks(Calendar) =====

    @Test
    public void testAddWeeks_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addWeeks(cal, 2);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void testAddWeeks_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addWeeks(cal, -2);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddWeeks_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addWeeks((Calendar) null, 1));
    }

    // ===== addDays(Calendar) =====

    @Test
    public void testAddDays_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addDays(cal, 7);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void testAddDays_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addDays(cal, -7);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddDays_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addDays((Calendar) null, 1));
    }

    // ===== addHours(Calendar) =====

    @Test
    public void testAddHours_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addHours(cal, 5);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void testAddHours_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addHours(cal, -5);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddHours_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addHours((Calendar) null, 1));
    }

    // ===== addMinutes(Calendar) =====

    @Test
    public void testAddMinutes_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMinutes(cal, 30);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void testAddMinutes_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMinutes(cal, -30);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddMinutes_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addMinutes((Calendar) null, 1));
    }

    // ===== addSeconds(Calendar) =====

    @Test
    public void testAddSeconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addSeconds(cal, 45);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() > cal.getTimeInMillis());
    }

    @Test
    public void testAddSeconds_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addSeconds(cal, -45);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddSeconds_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addSeconds((Calendar) null, 1));
    }

    // ===== addMilliseconds(Calendar) =====

    @Test
    public void testAddMilliseconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMilliseconds(cal, 500);
        assertNotNull(result);
        assertEquals(500, result.getTimeInMillis() - cal.getTimeInMillis());
    }

    @Test
    public void testAddMilliseconds_calendar_negative() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        Calendar result = Dates.addMilliseconds(cal, -500);
        assertNotNull(result);
        assertEquals(500, cal.getTimeInMillis() - result.getTimeInMillis());
    }

    @Test
    public void testAddMilliseconds_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addMilliseconds((Calendar) null, 1));
    }

    // ===== round(Date) =====

    @Test
    public void testRound_date_withInt() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.round(date, Calendar.SECOND);
        assertNotNull(result);
    }

    @Test
    public void testRound_date_withInt_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, Calendar.SECOND));
    }

    @Test
    public void testRound_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.round(date, CalendarField.SECOND);
        assertNotNull(result);
    }

    @Test
    public void testRound_date_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, CalendarField.SECOND));
    }

    // ===== round(Calendar) =====

    @Test
    public void testRound_calendar_withInt() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.round(cal, Calendar.SECOND);
        assertNotNull(result);
    }

    @Test
    public void testRound_calendar_withInt_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.round((Calendar) null, Calendar.SECOND));
    }

    @Test
    public void testRound_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.round(cal, CalendarField.SECOND);
        assertNotNull(result);
    }

    @Test
    public void testRound_calendar_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.round((Calendar) null, CalendarField.SECOND));
    }

    // ===== truncate(Date) =====

    @Test
    public void testTruncate_date_withInt() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.truncate(date, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() <= date.getTime());
    }

    @Test
    public void testTruncate_date_withInt_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((java.util.Date) null, Calendar.SECOND));
    }

    @Test
    public void testTruncate_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.truncate(date, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() <= date.getTime());
    }

    @Test
    public void testTruncate_date_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((java.util.Date) null, CalendarField.SECOND));
    }

    @Test
    public void testTruncate_date_day() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.truncate(date, Calendar.DAY_OF_MONTH);
        assertNotNull(result);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    // ===== truncate(Calendar) =====

    @Test
    public void testTruncate_calendar_withInt() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.truncate(cal, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() <= 1000000123L);
    }

    @Test
    public void testTruncate_calendar_withInt_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, Calendar.SECOND));
    }

    @Test
    public void testTruncate_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.truncate(cal, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() <= 1000000123L);
    }

    @Test
    public void testTruncate_calendar_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, CalendarField.SECOND));
    }

    @Test
    public void testTruncate_calendar_day() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.truncate(cal, Calendar.DAY_OF_MONTH);
        assertNotNull(result);
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    // ===== ceiling(Date) =====

    @Test
    public void testCeiling_date_withInt() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.ceiling(date, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() >= date.getTime());
    }

    @Test
    public void testCeiling_date_withInt_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((java.util.Date) null, Calendar.SECOND));
    }

    @Test
    public void testCeiling_date_withCalendarField() {
        java.util.Date date = new java.util.Date(1000000123L);
        java.util.Date result = Dates.ceiling(date, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTime() >= date.getTime());
    }

    @Test
    public void testCeiling_date_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((java.util.Date) null, CalendarField.SECOND));
    }

    // ===== ceiling(Calendar) =====

    @Test
    public void testCeiling_calendar_withInt() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.ceiling(cal, Calendar.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() >= 1000000123L);
    }

    @Test
    public void testCeiling_calendar_withInt_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((Calendar) null, Calendar.SECOND));
    }

    @Test
    public void testCeiling_calendar_withCalendarField() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000123L);
        Calendar result = Dates.ceiling(cal, CalendarField.SECOND);
        assertNotNull(result);
        assertTrue(result.getTimeInMillis() >= 1000000123L);
    }

    @Test
    public void testCeiling_calendar_withCalendarField_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((Calendar) null, CalendarField.SECOND));
    }

    // ===== truncatedEquals =====

    @Test
    public void testTruncatedEquals_calendar_calendarField() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1, cal2, CalendarField.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1, cal2, CalendarField.SECOND));
    }

    @Test
    public void testTruncatedEquals_calendar_int() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1, cal2, Calendar.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1, cal2, Calendar.SECOND));
    }

    @Test
    public void testTruncatedEquals_calendar_same() {
        Calendar cal = Calendar.getInstance();
        assertTrue(Dates.truncatedEquals(cal, cal, CalendarField.SECOND));
        assertTrue(Dates.truncatedEquals(cal, cal, Calendar.SECOND));
    }

    @Test
    public void testTruncatedEquals_date_calendarField() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), CalendarField.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), CalendarField.SECOND));
    }

    @Test
    public void testTruncatedEquals_date_int() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), Calendar.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), Calendar.SECOND));
    }

    @Test
    public void testTruncatedEquals_date_same() {
        java.util.Date date = new java.util.Date();
        assertTrue(Dates.truncatedEquals(date, date, CalendarField.SECOND));
        assertTrue(Dates.truncatedEquals(date, date, Calendar.SECOND));
    }

    // ===== truncatedCompareTo =====

    @Test
    public void testTruncatedCompareTo_calendar_calendarField() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, CalendarField.HOUR_OF_DAY));
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, CalendarField.MINUTE) < 0);
    }

    @Test
    public void testTruncatedCompareTo_calendar_int() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, Calendar.HOUR_OF_DAY));
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, Calendar.MINUTE) < 0);
    }

    @Test
    public void testTruncatedCompareTo_calendar_same() {
        Calendar cal = Calendar.getInstance();
        assertEquals(0, Dates.truncatedCompareTo(cal, cal, CalendarField.SECOND));
        assertEquals(0, Dates.truncatedCompareTo(cal, cal, Calendar.SECOND));
    }

    @Test
    public void testTruncatedCompareTo_date_calendarField() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertEquals(0, Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), CalendarField.HOUR_OF_DAY));
        assertTrue(Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), CalendarField.MINUTE) < 0);
    }

    @Test
    public void testTruncatedCompareTo_date_int() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertEquals(0, Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), Calendar.HOUR_OF_DAY));
        assertTrue(Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), Calendar.MINUTE) < 0);
    }

    @Test
    public void testTruncatedCompareTo_date_same() {
        java.util.Date date = new java.util.Date();
        assertEquals(0, Dates.truncatedCompareTo(date, date, CalendarField.SECOND));
        assertEquals(0, Dates.truncatedCompareTo(date, date, Calendar.SECOND));
    }

    @Test
    public void testTruncatedCompareTo_date_greaterThan() {
        java.util.Date date1 = new java.util.Date();
        java.util.Date date2 = new java.util.Date(date1.getTime() - 86400000);
        assertTrue(Dates.truncatedCompareTo(date1, date2, Calendar.DAY_OF_MONTH) > 0);
    }

    // ===== getFragmentInMilliseconds(Date) =====

    @Test
    public void testGetFragmentInMilliseconds_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        long result = Dates.getFragmentInMilliseconds(cal.getTime(), CalendarField.SECOND);
        assertEquals(123, result);
    }

    @Test
    public void testGetFragmentInMilliseconds_date_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long result = Dates.getFragmentInMilliseconds(cal.getTime(), CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInMilliseconds_date_range() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInMilliseconds(date, CalendarField.SECOND);
        assertTrue(result >= 0 && result < 1000);
    }

    // ===== getFragmentInSeconds(Date) =====

    @Test
    public void testGetFragmentInSeconds_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        long result = Dates.getFragmentInSeconds(cal.getTime(), CalendarField.MINUTE);
        assertEquals(45, result);
    }

    @Test
    public void testGetFragmentInSeconds_date_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long result = Dates.getFragmentInSeconds(cal.getTime(), CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInSeconds_date_range() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInSeconds(date, CalendarField.MINUTE);
        assertTrue(result >= 0 && result < 60);
    }

    // ===== getFragmentInMinutes(Date) =====

    @Test
    public void testGetFragmentInMinutes_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        long result = Dates.getFragmentInMinutes(cal.getTime(), CalendarField.HOUR_OF_DAY);
        assertEquals(30, result);
    }

    @Test
    public void testGetFragmentInMinutes_date_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        long result = Dates.getFragmentInMinutes(cal.getTime(), CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInMinutes_date_range() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY);
        assertTrue(result >= 0 && result < 60);
    }

    // ===== getFragmentInHours(Date) =====

    @Test
    public void testGetFragmentInHours_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        long result = Dates.getFragmentInHours(cal.getTime(), CalendarField.DAY_OF_MONTH);
        assertEquals(14, result);
    }

    @Test
    public void testGetFragmentInHours_date_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        long result = Dates.getFragmentInHours(cal.getTime(), CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInHours_date_range() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInHours(date, CalendarField.DAY_OF_MONTH);
        assertTrue(result >= 0 && result < 24);
    }

    // ===== getFragmentInDays(Date) =====

    @Test
    public void testGetFragmentInDays_date() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        long result = Dates.getFragmentInDays(cal.getTime(), CalendarField.MONTH);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInDays_date_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        long result = Dates.getFragmentInDays(cal.getTime(), CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInDays_date_range() {
        java.util.Date date = new java.util.Date();
        long result = Dates.getFragmentInDays(date, CalendarField.MONTH);
        assertTrue(result >= 0 && result <= 31);
    }

    // ===== getFragmentInMilliseconds(Calendar) =====

    @Test
    public void testGetFragmentInMilliseconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        long result = Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND);
        assertEquals(123, result);
    }

    @Test
    public void testGetFragmentInMilliseconds_calendar_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long result = Dates.getFragmentInMilliseconds(cal, CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInMilliseconds_calendar_range() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND);
        assertTrue(result >= 0 && result < 1000);
    }

    // ===== getFragmentInSeconds(Calendar) =====

    @Test
    public void testGetFragmentInSeconds_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        long result = Dates.getFragmentInSeconds(cal, CalendarField.MINUTE);
        assertEquals(45, result);
    }

    @Test
    public void testGetFragmentInSeconds_calendar_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long result = Dates.getFragmentInSeconds(cal, CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInSeconds_calendar_range() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInSeconds(cal, CalendarField.MINUTE);
        assertTrue(result >= 0 && result < 60);
    }

    // ===== getFragmentInMinutes(Calendar) =====

    @Test
    public void testGetFragmentInMinutes_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        long result = Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY);
        assertEquals(30, result);
    }

    @Test
    public void testGetFragmentInMinutes_calendar_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        long result = Dates.getFragmentInMinutes(cal, CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInMinutes_calendar_range() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY);
        assertTrue(result >= 0 && result < 60);
    }

    // ===== getFragmentInHours(Calendar) =====

    @Test
    public void testGetFragmentInHours_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        long result = Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH);
        assertEquals(14, result);
    }

    @Test
    public void testGetFragmentInHours_calendar_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        long result = Dates.getFragmentInHours(cal, CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInHours_calendar_range() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH);
        assertTrue(result >= 0 && result < 24);
    }

    // ===== getFragmentInDays(Calendar) =====

    @Test
    public void testGetFragmentInDays_calendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        long result = Dates.getFragmentInDays(cal, CalendarField.MONTH);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInDays_calendar_yearFragment() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 1, 0, 0, 0);
        long result = Dates.getFragmentInDays(cal, CalendarField.YEAR);
        assertTrue(result >= 0);
    }

    @Test
    public void testGetFragmentInDays_calendar_range() {
        Calendar cal = Calendar.getInstance();
        long result = Dates.getFragmentInDays(cal, CalendarField.YEAR);
        assertTrue(result >= 0 && result <= 366);
    }

    // ===== isSameDay =====

    @Test
    public void testIsSameDay_date() {
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
    public void testIsSameDay_date_same() {
        java.util.Date date = new java.util.Date();
        assertTrue(Dates.isSameDay(date, date));
    }

    @Test
    public void testIsSameDay_calendar() {
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
    public void testIsSameDay_calendar_sameWithMinutesDifference() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = (Calendar) cal1.clone();
        cal2.add(Calendar.MINUTE, 2);
        assertTrue(Dates.isSameDay(cal1, cal2));

        cal2.add(Calendar.DAY_OF_MONTH, 1);
        assertFalse(Dates.isSameDay(cal1, cal2));
    }

    // ===== isSameMonth =====

    @Test
    public void testIsSameMonth_date() {
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
    public void testIsSameMonth_date_same() {
        java.util.Date date = new java.util.Date();
        assertTrue(Dates.isSameMonth(date, date));
    }

    @Test
    public void testIsSameMonth_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.OCTOBER, 15, 15, 45, 30);

        assertTrue(Dates.isSameMonth(cal1, cal2));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2025, Calendar.NOVEMBER, 4, 10, 30, 45);
        assertFalse(Dates.isSameMonth(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth((Calendar) null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(cal1, (Calendar) null));
    }

    // ===== isSameYear =====

    @Test
    public void testIsSameYear_date() {
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
    public void testIsSameYear_date_same() {
        java.util.Date date = new java.util.Date();
        assertTrue(Dates.isSameYear(date, date));
    }

    @Test
    public void testIsSameYear_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2025, Calendar.OCTOBER, 4, 10, 30, 45);
        Calendar cal2 = Calendar.getInstance();
        cal2.set(2025, Calendar.DECEMBER, 31, 23, 59, 59);

        assertTrue(Dates.isSameYear(cal1, cal2));

        Calendar cal3 = Calendar.getInstance();
        cal3.set(2026, Calendar.JANUARY, 1, 0, 0, 0);
        assertFalse(Dates.isSameYear(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear((Calendar) null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(cal1, (Calendar) null));
    }

    // ===== isSameInstant =====

    @Test
    public void testIsSameInstant_date() {
        java.util.Date date1 = new java.util.Date(1000000000L);
        java.util.Date date2 = new java.util.Date(1000000000L);
        java.util.Date date3 = new java.util.Date(1000000001L);

        assertTrue(Dates.isSameInstant(date1, date2));
        assertFalse(Dates.isSameInstant(date1, date3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant((java.util.Date) null, date1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(date1, (java.util.Date) null));
    }

    @Test
    public void testIsSameInstant_date_same() {
        java.util.Date date = new java.util.Date();
        assertTrue(Dates.isSameInstant(date, date));
    }

    @Test
    public void testIsSameInstant_calendar() {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTimeInMillis(1000000000L);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTimeInMillis(1000000000L);
        Calendar cal3 = Calendar.getInstance();
        cal3.setTimeInMillis(1000000001L);

        assertTrue(Dates.isSameInstant(cal1, cal2));
        assertFalse(Dates.isSameInstant(cal1, cal3));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant((Calendar) null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(cal1, (Calendar) null));
    }

    // ===== isSameLocalTime =====

    @Test
    public void testIsSameLocalTime() {
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
    public void testIsSameLocalTime_sameCalendar() {
        Calendar cal = Calendar.getInstance();
        assertTrue(Dates.isSameLocalTime(cal, cal));
    }

    // ===== isLastDateOfMonth =====

    @Test
    public void testIsLastDateOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 31, 10, 30, 45);
        assertTrue(Dates.isLastDateOfMonth(cal.getTime()));

        cal.set(2025, Calendar.JANUARY, 30, 10, 30, 45);
        assertFalse(Dates.isLastDateOfMonth(cal.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfMonth(null));
    }

    @Test
    public void testIsLastDateOfMonth_february() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.FEBRUARY, 28, 10, 30, 45);
        assertTrue(Dates.isLastDateOfMonth(cal.getTime()));
    }

    @Test
    public void testIsLastDateOfMonth_leapYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 29, 10, 30, 45);
        assertTrue(Dates.isLastDateOfMonth(cal.getTime()));
    }

    // ===== isLastDateOfYear =====

    @Test
    public void testIsLastDateOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.DECEMBER, 31, 10, 30, 45);
        assertTrue(Dates.isLastDateOfYear(cal.getTime()));

        cal.set(2025, Calendar.DECEMBER, 30, 10, 30, 45);
        assertFalse(Dates.isLastDateOfYear(cal.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfYear(null));
    }

    @Test
    public void testIsLastDateOfYear_notLastDay() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JUNE, 15);
        assertFalse(Dates.isLastDateOfYear(cal.getTime()));
    }

    // ===== getLastDayOfMonth =====

    @Test
    public void testGetLastDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JANUARY, 15, 10, 30, 45);
        assertEquals(31, Dates.getLastDayOfMonth(cal.getTime()));

        cal.set(2025, Calendar.FEBRUARY, 15, 10, 30, 45);
        assertEquals(28, Dates.getLastDayOfMonth(cal.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.getLastDayOfMonth(null));
    }

    @Test
    public void testGetLastDayOfMonth_april() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.APRIL, 15);
        assertEquals(30, Dates.getLastDayOfMonth(cal.getTime()));
    }

    @Test
    public void testGetLastDayOfMonth_leapYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.FEBRUARY, 29, 12, 0, 0);
        assertEquals(29, Dates.getLastDayOfMonth(cal.getTime()));
    }

    // ===== getLastDayOfYear =====

    @Test
    public void testGetLastDayOfYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.JUNE, 15, 10, 30, 45);
        assertEquals(365, Dates.getLastDayOfYear(cal.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.getLastDayOfYear(null));
    }

    @Test
    public void testGetLastDayOfYear_leapYear() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JUNE, 15);
        assertEquals(366, Dates.getLastDayOfYear(cal.getTime()));
    }

    // ===== isOverlapping =====

    @Test
    public void testIsOverlapping() {
        java.util.Date s1 = Dates.parseJUDate("2023-01-01");
        java.util.Date e1 = Dates.parseJUDate("2023-01-10");
        java.util.Date s2 = Dates.parseJUDate("2023-01-05");
        java.util.Date e2 = Dates.parseJUDate("2023-01-15");
        java.util.Date s3 = Dates.parseJUDate("2023-01-12");
        java.util.Date e3 = Dates.parseJUDate("2023-01-20");

        assertTrue(Dates.isOverlapping(s1, e1, s2, e2));
        assertTrue(Dates.isOverlapping(s2, e2, s1, e1));
        assertFalse(Dates.isOverlapping(s1, e1, s3, e3));
        assertTrue(Dates.isOverlapping(s1, e1, s1, e1));
        assertTrue(Dates.isOverlapping(s1, e2, s2, e1));
    }

    @Test
    public void testIsOverlapping_adjacentRanges() {
        java.util.Date start1 = new java.util.Date(1000L);
        java.util.Date end1 = new java.util.Date(2000L);
        java.util.Date start2 = new java.util.Date(2000L);
        java.util.Date end2 = new java.util.Date(3000L);

        assertFalse(Dates.isOverlapping(start1, end1, start2, end2));
    }

    @Test
    public void testIsOverlapping_containedRange() {
        java.util.Date start1 = new java.util.Date(1000L);
        java.util.Date end1 = new java.util.Date(5000L);
        java.util.Date start2 = new java.util.Date(2000L);
        java.util.Date end2 = new java.util.Date(3000L);

        assertTrue(Dates.isOverlapping(start1, end1, start2, end2));
    }

    @Test
    public void testIsOverlapping_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(null, new java.util.Date(), new java.util.Date(), new java.util.Date()));
    }

    @Test
    public void testIsOverlapping_invalidRange() {
        java.util.Date start = new java.util.Date(5000);
        java.util.Date end = new java.util.Date(1000);
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(start, end, new java.util.Date(), new java.util.Date()));
    }

    // ===== isBetween =====

    @Test
    public void testIsBetween() {
        java.util.Date date = Dates.parseJUDate("2023-01-05");
        java.util.Date start = Dates.parseJUDate("2023-01-01");
        java.util.Date end = Dates.parseJUDate("2023-01-10");

        assertTrue(Dates.isBetween(date, start, end));
        assertTrue(Dates.isBetween(start, start, end));
        assertTrue(Dates.isBetween(end, start, end));

        java.util.Date before = Dates.parseJUDate("2022-12-31");
        java.util.Date after = Dates.parseJUDate("2023-01-11");
        assertFalse(Dates.isBetween(before, start, end));
        assertFalse(Dates.isBetween(after, start, end));
    }

    @Test
    public void testIsBetween_atStartBoundary() {
        java.util.Date start = new java.util.Date(1000L);
        java.util.Date end = new java.util.Date(5000L);
        assertTrue(Dates.isBetween(start, start, end));
    }

    @Test
    public void testIsBetween_atEndBoundary() {
        java.util.Date start = new java.util.Date(1000L);
        java.util.Date end = new java.util.Date(5000L);
        assertTrue(Dates.isBetween(end, start, end));
    }

    @Test
    public void testIsBetween_nullDate() {
        java.util.Date start = new java.util.Date();
        java.util.Date end = new java.util.Date();
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(null, start, end));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(start, null, end));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(start, start, null));
    }

    @Test
    public void testIsBetween_invalidRange() {
        java.util.Date date = new java.util.Date();
        java.util.Date start = new java.util.Date(5000);
        java.util.Date end = new java.util.Date(1000);
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, start, end));
    }

    // ===== DTF class =====

    @Test
    public void testDTF_format_date() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = new java.util.Date(1000000000L);
        String formatted = dtf.format(date);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testDTF_format_date_null() {
        assertNull(Dates.DTF.LOCAL_DATE.format((java.util.Date) null));
    }

    @Test
    public void testDTF_format_calendar() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        String formatted = dtf.format(cal);
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testDTF_format_calendar_null() {
        assertNull(Dates.DTF.LOCAL_DATE.format((Calendar) null));
    }

    @Test
    public void testDTF_format_temporalAccessor() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        LocalDate localDate = LocalDate.of(2025, 10, 4);
        String formatted = dtf.format(localDate);
        assertNotNull(formatted);
        assertEquals("2025-10-04", formatted);
    }

    @Test
    public void testDTF_format_temporalAccessor_null() {
        assertNull(Dates.DTF.LOCAL_DATE.format((TemporalAccessor) null));
    }

    // ===== DTF formatTo =====

    @Test
    public void testDTF_formatTo_date() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = new java.util.Date(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo(date, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testDTF_formatTo_date_null() {
        StringBuilder sb = new StringBuilder();
        Dates.DTF.LOCAL_DATE.formatTo((java.util.Date) null, sb);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testDTF_formatTo_calendar() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1000000000L);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo(cal, sb));
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testDTF_formatTo_calendar_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo((Calendar) null, sb));
        assertEquals("null", sb.toString());
    }

    @Test
    public void testDTF_formatTo_temporalAccessor() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        LocalDate localDate = LocalDate.of(2025, 10, 4);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo(localDate, sb));
        assertEquals("2025-10-04", sb.toString());
    }

    @Test
    public void testDTF_formatTo_temporalAccessor_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        StringBuilder sb = new StringBuilder();
        assertDoesNotThrow(() -> dtf.formatTo((TemporalAccessor) null, sb));
        assertEquals("null", sb.toString());
    }

    // ===== DTF parseToLocalDate =====

    @Test
    public void testDTF_parseToLocalDate() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        LocalDate localDate = dtf.parseToLocalDate("2025-10-04");
        assertNotNull(localDate);
        assertEquals(2025, localDate.getYear());
        assertEquals(10, localDate.getMonthValue());
        assertEquals(4, localDate.getDayOfMonth());
    }

    @Test
    public void testDTF_parseToLocalDate_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        assertNull(dtf.parseToLocalDate(null));
        assertNull(dtf.parseToLocalDate(""));
    }

    // ===== DTF parseToLocalTime =====

    @Test
    public void testDTF_parseToLocalTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        LocalTime localTime = dtf.parseToLocalTime("14:30:45");
        assertNotNull(localTime);
        assertEquals(14, localTime.getHour());
        assertEquals(30, localTime.getMinute());
        assertEquals(45, localTime.getSecond());
    }

    @Test
    public void testDTF_parseToLocalTime_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        assertNull(dtf.parseToLocalTime(null));
        assertNull(dtf.parseToLocalTime(""));
    }

    // ===== DTF parseToLocalDateTime =====

    @Test
    public void testDTF_parseToLocalDateTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        LocalDateTime localDateTime = dtf.parseToLocalDateTime("2025-10-04 14:30:45");
        assertNotNull(localDateTime);
        assertEquals(2025, localDateTime.getYear());
        assertEquals(10, localDateTime.getMonthValue());
        assertEquals(4, localDateTime.getDayOfMonth());
        assertEquals(14, localDateTime.getHour());
        assertEquals(30, localDateTime.getMinute());
        assertEquals(45, localDateTime.getSecond());
    }

    @Test
    public void testDTF_parseToLocalDateTime_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        assertNull(dtf.parseToLocalDateTime(null));
        assertNull(dtf.parseToLocalDateTime(""));
    }

    // ===== DTF parseToOffsetDateTime =====

    @Test
    public void testDTF_parseToOffsetDateTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        OffsetDateTime offsetDateTime = dtf.parseToOffsetDateTime("2025-10-04T14:30:45+00:00");
        assertNotNull(offsetDateTime);
        assertEquals(2025, offsetDateTime.getYear());
        assertEquals(10, offsetDateTime.getMonthValue());
        assertEquals(4, offsetDateTime.getDayOfMonth());
    }

    @Test
    public void testDTF_parseToOffsetDateTime_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        assertNull(dtf.parseToOffsetDateTime(null));
        assertNull(dtf.parseToOffsetDateTime(""));
    }

    // ===== DTF parseToZonedDateTime =====

    @Test
    public void testDTF_parseToZonedDateTime() {
        Dates.DTF dtf = new Dates.DTF("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'");
        ZonedDateTime zonedDateTime = dtf.parseToZonedDateTime("2025-10-04T14:30:45+00:00[UTC]");
        assertNotNull(zonedDateTime);
        assertEquals(2025, zonedDateTime.getYear());
        assertEquals(10, zonedDateTime.getMonthValue());
        assertEquals(4, zonedDateTime.getDayOfMonth());
    }

    @Test
    public void testDTF_parseToZonedDateTime_null() {
        Dates.DTF dtf = new Dates.DTF("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'");
        assertNull(dtf.parseToZonedDateTime(null));
        assertNull(dtf.parseToZonedDateTime(""));
    }

    // ===== DTF parseToInstant =====

    @Test
    public void testDTF_parseToInstant() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        Instant instant = dtf.parseToInstant("2025-10-04T14:30:45+00:00");
        assertNotNull(instant);
        assertTrue(instant.toEpochMilli() > 0);
    }

    @Test
    public void testDTF_parseToInstant_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.ISO_OFFSET_DATE_TIME_FORMAT);
        assertNull(dtf.parseToInstant(null));
        assertNull(dtf.parseToInstant(""));
    }

    // ===== DTF parseToJUDate =====

    @Test
    public void testDTF_parseToJUDate() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = dtf.parseToJUDate("2025-10-04");
        assertNotNull(date);
    }

    @Test
    public void testDTF_parseToJUDate_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        assertNull(dtf.parseToJUDate(null));
        assertNull(dtf.parseToJUDate(""));
    }

    @Test
    public void testDTF_parseToJUDate_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.util.Date date = dtf.parseToJUDate("2025-10-04", TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    @Test
    public void testDTF_parseToJUDate_withTimeZone_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        assertNull(dtf.parseToJUDate(null, TimeZone.getTimeZone("UTC")));
        assertNull(dtf.parseToJUDate("", TimeZone.getTimeZone("UTC")));
    }

    // ===== DTF parseToDate =====

    @Test
    public void testDTF_parseToDate() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.sql.Date date = dtf.parseToDate("2025-10-04");
        assertNotNull(date);
    }

    @Test
    public void testDTF_parseToDate_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        assertNull(dtf.parseToDate(null));
        assertNull(dtf.parseToDate(""));
    }

    @Test
    public void testDTF_parseToDate_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        java.sql.Date date = dtf.parseToDate("2025-10-04", TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    @Test
    public void testDTF_parseToDate_withTimeZone_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        assertNull(dtf.parseToDate(null, TimeZone.getTimeZone("UTC")));
        assertNull(dtf.parseToDate("", TimeZone.getTimeZone("UTC")));
    }

    @Test
    public void testDTF_parseToDate_sqlDate() {
        java.sql.Date sqlDate = Dates.DTF.LOCAL_DATE.parseToDate("2023-10-26");
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.OCTOBER, 26, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTimeInMillis(), sqlDate.getTime());
    }

    // ===== DTF parseToTime =====

    @Test
    public void testDTF_parseToTime() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        Time time = dtf.parseToTime("14:30:45");
        assertNotNull(time);
    }

    @Test
    public void testDTF_parseToTime_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        assertNull(dtf.parseToTime(null));
        assertNull(dtf.parseToTime(""));
    }

    @Test
    public void testDTF_parseToTime_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        Time time = dtf.parseToTime("14:30:45", TimeZone.getTimeZone("UTC"));
        assertNotNull(time);
    }

    @Test
    public void testDTF_parseToTime_withTimeZone_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_TIME_FORMAT);
        assertNull(dtf.parseToTime(null, TimeZone.getTimeZone("UTC")));
        assertNull(dtf.parseToTime("", TimeZone.getTimeZone("UTC")));
    }

    @Test
    public void testDTF_parseToTime_sqlTime() {
        java.sql.Time sqlTime = Dates.DTF.LOCAL_TIME.parseToTime("10:15:30");
        Calendar cal = Calendar.getInstance();
        cal.set(1970, Calendar.JANUARY, 1, 10, 15, 30);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTimeInMillis(), sqlTime.getTime());
    }

    // ===== DTF parseToTimestamp =====

    @Test
    public void testDTF_parseToTimestamp() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        Timestamp timestamp = dtf.parseToTimestamp("2025-10-04 14:30:45");
        assertNotNull(timestamp);
    }

    @Test
    public void testDTF_parseToTimestamp_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        assertNull(dtf.parseToTimestamp(null));
        assertNull(dtf.parseToTimestamp(""));
    }

    @Test
    public void testDTF_parseToTimestamp_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        Timestamp timestamp = dtf.parseToTimestamp("2025-10-04 14:30:45", TimeZone.getTimeZone("UTC"));
        assertNotNull(timestamp);
    }

    @Test
    public void testDTF_parseToTimestamp_withTimeZone_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_TIME_FORMAT);
        assertNull(dtf.parseToTimestamp(null, TimeZone.getTimeZone("UTC")));
        assertNull(dtf.parseToTimestamp("", TimeZone.getTimeZone("UTC")));
    }

    // ===== DTF parseToCalendar =====

    @Test
    public void testDTF_parseToCalendar() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar calendar = dtf.parseToCalendar("2025-10-04");
        assertNotNull(calendar);
    }

    @Test
    public void testDTF_parseToCalendar_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        assertNull(dtf.parseToCalendar(null));
        assertNull(dtf.parseToCalendar(""));
    }

    @Test
    public void testDTF_parseToCalendar_withTimeZone() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar calendar = dtf.parseToCalendar("2025-10-04", TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void testDTF_parseToCalendar_withTimeZone_null() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        assertNull(dtf.parseToCalendar(null, TimeZone.getTimeZone("UTC")));
        assertNull(dtf.parseToCalendar("", TimeZone.getTimeZone("UTC")));
    }

    // ===== DTF toString =====

    @Test
    public void testDTF_toString() {
        assertEquals(Dates.LOCAL_DATE_FORMAT, Dates.DTF.LOCAL_DATE.toString());
        assertEquals(Dates.ISO_8601_TIMESTAMP_FORMAT, Dates.DTF.ISO_8601_TIMESTAMP.toString());
    }

    // ===== DTF constant instances =====

    @Test
    public void testDTF_LOCAL_DATE() {
        assertNotNull(Dates.DTF.LOCAL_DATE);
    }

    @Test
    public void testDTF_LOCAL_TIME() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        String result = Dates.DTF.LOCAL_TIME.format(cal.getTime());
        assertEquals("13:45:30", result);
    }

    @Test
    public void testDTF_LOCAL_DATE_TIME() {
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        String result = Dates.DTF.LOCAL_DATE_TIME.format(cal.getTime());
        assertTrue(result.contains("2022-01-01"));
        assertTrue(result.contains("13:45:30"));
    }

    @Test
    public void testDTF_ISO_LOCAL_DATE_TIME() {
        assertNotNull(Dates.DTF.ISO_LOCAL_DATE_TIME);
        Calendar cal = Calendar.getInstance();
        cal.set(2022, 0, 1, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        String result = Dates.DTF.ISO_LOCAL_DATE_TIME.format(cal.getTime());
        assertTrue(result.contains("2022-01-01T13:45:30"));
    }

    @Test
    public void testDTF_ISO_LOCAL_DATE_TIME_temporalAccessor() {
        LocalDateTime ldt = LocalDateTime.of(2025, 10, 4, 14, 30, 45);
        String formatted = Dates.DTF.ISO_LOCAL_DATE_TIME.format(ldt);
        assertNotNull(formatted);
        assertTrue(formatted.contains("T"));
    }

    @Test
    public void testDTF_ISO_OFFSET_DATE_TIME() {
        assertNotNull(Dates.DTF.ISO_OFFSET_DATE_TIME);
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 13, 45, 30);
        OffsetDateTime odt = ldt.atOffset(ZoneOffset.ofHours(-5));
        String result = Dates.DTF.ISO_OFFSET_DATE_TIME.format(odt);
        assertTrue(result.contains("2022-01-01T13:45:30-05:00"));
    }

    @Test
    public void testDTF_ISO_OFFSET_DATE_TIME_format() {
        OffsetDateTime odt = OffsetDateTime.of(2025, 10, 4, 14, 30, 45, 0, ZoneOffset.UTC);
        String formatted = Dates.DTF.ISO_OFFSET_DATE_TIME.format(odt);
        assertNotNull(formatted);
        assertTrue(formatted.contains("+00:00") || formatted.contains("Z"));
    }

    @Test
    public void testDTF_ISO_ZONED_DATE_TIME() {
        assertNotNull(Dates.DTF.ISO_ZONED_DATE_TIME);
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 13, 45, 30);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/New_York"));
        String result = Dates.DTF.ISO_ZONED_DATE_TIME.format(zdt);
        assertTrue(result.contains("2022-01-01T13:45:30"));
        assertTrue(result.contains("[America/New_York]"));
    }

    @Test
    public void testDTF_ISO_ZONED_DATE_TIME_utc() {
        ZonedDateTime zdt = ZonedDateTime.of(2025, 10, 4, 14, 30, 45, 0, ZoneId.of("UTC"));
        String formatted = Dates.DTF.ISO_ZONED_DATE_TIME.format(zdt);
        assertNotNull(formatted);
        assertTrue(formatted.contains("[UTC]"));
    }

    @Test
    public void testDTF_ISO_8601_DATE_TIME() {
        assertNotNull(Dates.DTF.ISO_8601_DATE_TIME);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        String formatted = Dates.DTF.ISO_8601_DATE_TIME.format(cal.getTime());
        assertNotNull(formatted);
        assertTrue(formatted.endsWith("Z"));
    }

    @Test
    public void testDTF_ISO_8601_TIMESTAMP() {
        assertNotNull(Dates.DTF.ISO_8601_TIMESTAMP);
        Timestamp ts = new Timestamp(1000000123L);
        String formatted = Dates.DTF.ISO_8601_TIMESTAMP.format(ts);
        assertNotNull(formatted);
        assertTrue(formatted.endsWith("Z"));
        assertTrue(formatted.contains("."));
    }

    @Test
    public void testDTF_RFC_1123_DATE_TIME() {
        assertNotNull(Dates.DTF.RFC_1123_DATE_TIME);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2022, 0, 1, 13, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        String result = Dates.DTF.RFC_1123_DATE_TIME.format(cal.getTime());
        assertTrue(result.matches("\\w{3}, \\d{2} \\w{3} \\d{4} \\d{2}:\\d{2}:\\d{2} \\w+"));
    }

    @Test
    public void testDTF_customFormat() {
        Dates.DTF dtf = new Dates.DTF("dd/MM/yyyy");
        LocalDate ld = LocalDate.of(2025, 10, 4);
        String formatted = dtf.format(ld);
        assertEquals("04/10/2025", formatted);
    }

    // ===== DateUtil class =====

    @Test
    public void testDateUtilClass() {
        assertTrue(Dates.class.isAssignableFrom(Dates.DateUtil.class));
    }

    // ===== CalendarField enum =====

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

    // ===== Edge case: daylight saving time =====

    @Test
    public void testEdgeCase_daylightSavingTime() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Dates.createCalendar(1000000000L, tz);
        assertNotNull(cal);
        assertEquals(tz, cal.getTimeZone());
    }

    // ===== Missing tests: null input handling for parse methods =====

    @Test
    public void testParseJUDate_nullInput() {
        assertNull(Dates.parseJUDate(null));
    }

    @Test
    public void testParseJUDate_emptyInput() {
        assertNull(Dates.parseJUDate(""));
    }

    @Test
    public void testParseJUDate_nullString() {
        assertNull(Dates.parseJUDate("null"));
    }

    @Test
    public void testParseDate_nullInput() {
        assertNull(Dates.parseDate(null));
    }

    @Test
    public void testParseDate_emptyInput() {
        assertNull(Dates.parseDate(""));
    }

    @Test
    public void testParseTime_nullInput() {
        assertNull(Dates.parseTime(null));
    }

    @Test
    public void testParseTime_emptyInput() {
        assertNull(Dates.parseTime(""));
    }

    @Test
    public void testParseTimestamp_nullInput() {
        assertNull(Dates.parseTimestamp(null));
    }

    @Test
    public void testParseTimestamp_emptyInput() {
        assertNull(Dates.parseTimestamp(""));
    }

    @Test
    public void testParseCalendar_nullInput() {
        assertNull(Dates.parseCalendar(null));
    }

    @Test
    public void testParseCalendar_emptyInput() {
        assertNull(Dates.parseCalendar(""));
    }

    @Test
    public void testParseGregorianCalendar_nullInput() {
        assertNull(Dates.parseGregorianCalendar(null));
    }

    @Test
    public void testParseGregorianCalendar_emptyInput() {
        assertNull(Dates.parseGregorianCalendar(""));
    }

    @Test
    public void testParseXMLGregorianCalendar_nullInput() {
        assertNull(Dates.parseXMLGregorianCalendar(null));
    }

    @Test
    public void testParseXMLGregorianCalendar_emptyInput() {
        assertNull(Dates.parseXMLGregorianCalendar(""));
    }

    // ===== Missing tests: format with null Date =====

    @Test
    public void testFormat_nullDate_noFormat() {
        assertNull(Dates.format((java.util.Date) null));
    }

    // ===== Missing tests: isSameInstant with calendars in different time zones =====

    @Test
    public void testIsSameInstant_calendar_differentTimeZones() {
        long millis = 1000000000L;
        Calendar calUtc = Dates.createCalendar(millis, Dates.UTC_TIME_ZONE);
        Calendar calTokyo = Dates.createCalendar(millis, TimeZone.getTimeZone("Asia/Tokyo"));

        // Same instant in different time zones
        assertTrue(Dates.isSameInstant(calUtc, calTokyo));
    }

    @Test
    public void testIsSameInstant_calendar_differentInstants() {
        Calendar cal1 = Dates.createCalendar(1000000000L, Dates.UTC_TIME_ZONE);
        Calendar cal2 = Dates.createCalendar(2000000000L, Dates.UTC_TIME_ZONE);
        assertFalse(Dates.isSameInstant(cal1, cal2));
    }

    // ===== Missing tests: isSameLocalTime negative case =====

    @Test
    public void testIsSameLocalTime_differentTimes() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2023, Calendar.MARCH, 15, 10, 30, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2023, Calendar.MARCH, 15, 11, 30, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        assertFalse(Dates.isSameLocalTime(cal1, cal2));
    }

    @Test
    public void testIsSameLocalTime_sameTimeExact() {
        Calendar cal1 = Calendar.getInstance();
        cal1.set(2023, Calendar.MARCH, 15, 10, 30, 45);
        cal1.set(Calendar.MILLISECOND, 123);

        Calendar cal2 = Calendar.getInstance();
        cal2.set(2023, Calendar.MARCH, 15, 10, 30, 45);
        cal2.set(Calendar.MILLISECOND, 123);

        assertTrue(Dates.isSameLocalTime(cal1, cal2));
    }

    // ===== Missing tests: isSameDay null =====

    @Test
    public void testIsSameDay_date_nullFirst() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay((java.util.Date) null, new java.util.Date()));
    }

    @Test
    public void testIsSameDay_date_nullSecond() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(new java.util.Date(), (java.util.Date) null));
    }

    @Test
    public void testIsSameDay_calendar_nullFirst() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay((Calendar) null, Calendar.getInstance()));
    }

    @Test
    public void testIsSameDay_calendar_nullSecond() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(Calendar.getInstance(), (Calendar) null));
    }

    // ===== Missing tests: isSameMonth null =====

    @Test
    public void testIsSameMonth_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth((java.util.Date) null, new java.util.Date()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(new java.util.Date(), (java.util.Date) null));
    }

    @Test
    public void testIsSameMonth_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth((Calendar) null, Calendar.getInstance()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(Calendar.getInstance(), (Calendar) null));
    }

    // ===== Missing tests: isSameYear null =====

    @Test
    public void testIsSameYear_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear((java.util.Date) null, new java.util.Date()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(new java.util.Date(), (java.util.Date) null));
    }

    @Test
    public void testIsSameYear_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear((Calendar) null, Calendar.getInstance()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(Calendar.getInstance(), (Calendar) null));
    }

    // ===== Missing tests: isSameInstant null =====

    @Test
    public void testIsSameInstant_date_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant((java.util.Date) null, new java.util.Date()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(new java.util.Date(), (java.util.Date) null));
    }

    @Test
    public void testIsSameInstant_calendar_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant((Calendar) null, Calendar.getInstance()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(Calendar.getInstance(), (Calendar) null));
    }

    // ===== Missing tests: isSameLocalTime null =====

    @Test
    public void testIsSameLocalTime_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameLocalTime(null, Calendar.getInstance()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameLocalTime(Calendar.getInstance(), null));
    }

    // ===== Missing tests: isLastDateOfMonth null =====

    @Test
    public void testIsLastDateOfMonth_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfMonth(null));
    }

    // ===== Missing tests: isLastDateOfYear null =====

    @Test
    public void testIsLastDateOfYear_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDateOfYear(null));
    }

    // ===== Missing tests: getLastDayOfMonth null =====

    @Test
    public void testGetLastDayOfMonth_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.getLastDayOfMonth(null));
    }

    // ===== Missing tests: getLastDayOfYear null =====

    @Test
    public void testGetLastDayOfYear_null() {
        assertThrows(IllegalArgumentException.class, () -> Dates.getLastDayOfYear(null));
    }

    // ===== Missing tests: createJUDate null calendar/date =====

    @Test
    public void testCreateJUDate_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((Calendar) null));
    }

    @Test
    public void testCreateJUDate_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((java.util.Date) null));
    }

    // ===== Missing tests: createDate null calendar/date =====

    @Test
    public void testCreateDate_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((Calendar) null));
    }

    @Test
    public void testCreateDate_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((java.util.Date) null));
    }

    // ===== Missing tests: createTime null calendar/date =====

    @Test
    public void testCreateTime_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((Calendar) null));
    }

    @Test
    public void testCreateTime_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((java.util.Date) null));
    }

    // ===== Missing tests: createTimestamp null calendar/date =====

    @Test
    public void testCreateTimestamp_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((Calendar) null));
    }

    @Test
    public void testCreateTimestamp_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((java.util.Date) null));
    }

    // ===== Missing tests: createCalendar null calendar/date =====

    @Test
    public void testCreateCalendar_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((Calendar) null));
    }

    @Test
    public void testCreateCalendar_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((java.util.Date) null));
    }

    // ===== Missing tests: format with Timestamp =====

    @Test
    public void testFormat_timestamp() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts);
        assertNotNull(formatted);
        assertTrue(formatted.length() > 0);
    }

    @Test
    public void testFormat_timestamp_withFormat() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts, Dates.LOCAL_TIMESTAMP_FORMAT);
        assertNotNull(formatted);
        assertTrue(formatted.contains("."));
    }

    @Test
    public void testFormat_timestamp_withFormatAndTimeZone() {
        Timestamp ts = new Timestamp(1000000000L);
        String formatted = Dates.format(ts, Dates.LOCAL_TIMESTAMP_FORMAT, Dates.UTC_TIME_ZONE);
        assertNotNull(formatted);
    }

    // ===== Missing tests: formatTo with Timestamp =====

    @Test
    public void testFormatTo_timestamp() {
        Timestamp ts = new Timestamp(1000000000L);
        StringBuilder sb = new StringBuilder();
        Dates.formatTo(ts, sb);
        assertTrue(sb.length() > 0);
    }

    // ===== Missing tests: parse with format auto-detection for various lengths =====

    @Test
    public void testParseJUDate_yearFormat() {
        java.util.Date date = Dates.parseJUDate("2023");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_monthDayFormat() {
        java.util.Date date = Dates.parseJUDate("03-15");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_timeFormat() {
        java.util.Date date = Dates.parseJUDate("10:30:45");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_localTimestampFormat() {
        java.util.Date date = Dates.parseJUDate("2023-03-15 10:30:45.123");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_iso8601TimestampFormat() {
        java.util.Date date = Dates.parseJUDate("2023-03-15T10:30:45.123Z");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_isoLocalDateTimeFormat() {
        java.util.Date date = Dates.parseJUDate("2023-03-15T10:30:45");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_iso8601DateTimeFormat() {
        java.util.Date date = Dates.parseJUDate("2023-03-15T10:30:45Z");
        assertNotNull(date);
    }

    // ===== Missing tests: truncatedEquals/truncatedCompareTo with null =====

    @Test
    public void testTruncatedEquals_date_differentDays() {
        java.util.Date date1 = Dates.parseJUDate("2023-03-15 10:30:45");
        java.util.Date date2 = Dates.parseJUDate("2023-03-16 10:30:45");
        assertFalse(Dates.truncatedEquals(date1, date2, CalendarField.DAY_OF_MONTH));
    }

    @Test
    public void testTruncatedCompareTo_date_differentDays() {
        java.util.Date date1 = Dates.parseJUDate("2023-03-15 10:30:45");
        java.util.Date date2 = Dates.parseJUDate("2023-03-16 10:30:45");
        assertTrue(Dates.truncatedCompareTo(date1, date2, CalendarField.DAY_OF_MONTH) < 0);
        assertTrue(Dates.truncatedCompareTo(date2, date1, CalendarField.DAY_OF_MONTH) > 0);
    }

    // ===== Missing tests: round/truncate/ceiling with CalendarField for specific values =====

    @Test
    public void testRound_date_hourField() {
        java.util.Date date = Dates.parseJUDate("2023-03-15 10:45:00");
        java.util.Date rounded = Dates.round(date, CalendarField.HOUR_OF_DAY);
        assertNotNull(rounded);
        Calendar cal = Calendar.getInstance();
        cal.setTime(rounded);
        assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testTruncate_date_hourField() {
        java.util.Date date = Dates.parseJUDate("2023-03-15 10:45:30");
        java.util.Date truncated = Dates.truncate(date, CalendarField.HOUR_OF_DAY);
        assertNotNull(truncated);
        Calendar cal = Calendar.getInstance();
        cal.setTime(truncated);
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    public void testCeiling_date_hourField() {
        java.util.Date date = Dates.parseJUDate("2023-03-15 10:01:00");
        java.util.Date ceiled = Dates.ceiling(date, CalendarField.HOUR_OF_DAY);
        assertNotNull(ceiled);
        Calendar cal = Calendar.getInstance();
        cal.setTime(ceiled);
        assertEquals(11, cal.get(Calendar.HOUR_OF_DAY));
    }

    // ===== Missing tests: setYears/setMonths etc with Timestamp =====

    @Test
    public void testSetYears_timestamp() {
        Timestamp ts = Dates.parseTimestamp("2023-03-15 10:30:45.123");
        Timestamp result = Dates.setYears(ts, 2025);
        assertNotNull(result);
        assertTrue(result instanceof Timestamp);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(2025, cal.get(Calendar.YEAR));
    }

    @Test
    public void testSetMonths_timestamp() {
        Timestamp ts = Dates.parseTimestamp("2023-03-15 10:30:45.123");
        Timestamp result = Dates.setMonths(ts, 5);
        assertNotNull(result);
        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
    }

    // ===== Missing tests: addDays with Timestamp =====

    @Test
    public void testAddDays_timestamp() {
        Timestamp ts = Dates.parseTimestamp("2023-03-15 10:30:45.123");
        Timestamp result = Dates.addDays(ts, 1);
        assertNotNull(result);
        assertTrue(result instanceof Timestamp);
        assertTrue(result.getTime() > ts.getTime());
    }

    // ===== Missing tests: roll with Timestamp =====

    @Test
    public void testRoll_timestamp_withTimeUnit() {
        Timestamp ts = new Timestamp(1000000000L);
        Timestamp rolled = Dates.roll(ts, 1, TimeUnit.DAYS);
        assertNotNull(rolled);
        assertTrue(rolled instanceof Timestamp);
        assertEquals(ts.getTime() + TimeUnit.DAYS.toMillis(1), rolled.getTime());
    }

    @Test
    public void testRoll_timestamp_withCalendarField() {
        Timestamp ts = new Timestamp(1000000000L);
        Timestamp rolled = Dates.roll(ts, 1, CalendarField.DAY_OF_MONTH);
        assertNotNull(rolled);
        assertTrue(rolled instanceof Timestamp);
    }

    // ===== Missing tests: DTF parseToCalendar with TimeZone =====

    @Test
    public void testDTF_parseToCalendar_UTC() {
        Dates.DTF dtf = new Dates.DTF(Dates.LOCAL_DATE_FORMAT);
        Calendar cal = dtf.parseToCalendar("2023-03-15", Dates.UTC_TIME_ZONE);
        assertNotNull(cal);
        assertEquals(2023, cal.get(Calendar.YEAR));
    }

    // ===== Missing tests: format with ISO_OFFSET =====

    @Test
    public void testParseJUDate_isoOffsetFormat() {
        java.util.Date date = Dates.parseJUDate("2023-03-15T10:30:45+05:30");
        assertNotNull(date);
    }

    // ===== Missing tests: formatLocalDate/formatLocalDateTime value validation =====

    @Test
    public void testFormatLocalDate_matchesPattern() {
        String formatted = Dates.formatLocalDate();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    public void testFormatLocalDateTime_matchesPattern() {
        String formatted = Dates.formatLocalDateTime();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }

    @Test
    public void testFormatCurrentTimestamp_matchesPattern() {
        String formatted = Dates.formatCurrentTimestamp();
        assertNotNull(formatted);
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }
}
