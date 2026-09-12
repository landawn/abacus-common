package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DatesTest extends TestBase {

    @Test
    @SuppressWarnings("deprecation")
    public void testLargeMonthAdditionsClampOnlyAtFinalMonth() {
        TimeZone previous = TimeZone.getDefault();
        TimeZone utc = TimeZone.getTimeZone("UTC");

        try {
            TimeZone.setDefault(utc);

            for (LocalDateTime start : new LocalDateTime[] {LocalDateTime.of(2000, 7, 31, 12, 34, 56),
                    LocalDateTime.of(2000, 3, 31, 12, 34, 56), LocalDateTime.of(2000, 2, 29, 12, 34, 56),
                    LocalDateTime.of(-400, 2, 29, 12, 34, 56)}) {
                java.util.Date date = java.util.Date.from(start.toInstant(ZoneOffset.UTC));
                Calendar calendar = Dates.createCalendar(date.getTime(), utc);

                for (int months : new int[] {200_000_000, -200_000_000, 199_996_800, -199_996_800, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                    long expected = start.plusMonths(months).toInstant(ZoneOffset.UTC).toEpochMilli();
                    String context = start + " plus " + months + " months";

                    assertEquals(expected, Dates.addMonths(date, months).getTime(), context);
                    assertEquals(expected, Dates.addMonths(calendar, months).getTimeInMillis(), context);
                    assertEquals(expected, Dates.roll(date, months, CalendarField.MONTH).getTime(), context);
                    assertEquals(expected, Dates.roll(calendar, months, CalendarField.MONTH).getTimeInMillis(), context);
                }

                assertEquals(start.toInstant(ZoneOffset.UTC).toEpochMilli(), date.getTime());
                assertEquals(date.getTime(), calendar.getTimeInMillis());
            }

            assertThrows(ArithmeticException.class, () -> Dates.addMonths(new java.util.Date(Long.MAX_VALUE), 200_000_000));
            assertThrows(ArithmeticException.class, () -> Dates.addMonths(new java.util.Date(Long.MIN_VALUE), -200_000_000));
            assertThrows(ArithmeticException.class, () -> Dates.addMonths(Dates.createCalendar(Long.MAX_VALUE, utc), 200_000_000));
            assertThrows(ArithmeticException.class, () -> Dates.addMonths(Dates.createCalendar(Long.MIN_VALUE, utc), -200_000_000));
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    public void testTruncatedCalendarComparisonsUseBoundaryInstants() {
        long millis = Instant.parse("2025-01-15T12:00:00Z").toEpochMilli();
        Calendar utc = Dates.createCalendar(millis, TimeZone.getTimeZone("UTC"));
        Calendar gmt = Dates.createCalendar(millis, TimeZone.getTimeZone("GMT"));
        Calendar kolkata = Dates.createCalendar(millis, TimeZone.getTimeZone("Asia/Kolkata"));

        assertTrue(Dates.truncatedEquals(utc, gmt, Calendar.DAY_OF_MONTH));
        assertTrue(Dates.truncatedEquals(utc, gmt, CalendarField.DAY_OF_MONTH));
        assertEquals(0, Dates.truncatedCompareTo(utc, gmt, Calendar.DAY_OF_MONTH));
        assertEquals(0, Dates.truncatedCompareTo(utc, gmt, CalendarField.DAY_OF_MONTH));
        assertFalse(Dates.truncatedEquals(utc, kolkata, Calendar.DAY_OF_MONTH));
        assertFalse(Dates.truncatedEquals(utc, kolkata, CalendarField.DAY_OF_MONTH));
        assertTrue(Dates.truncatedCompareTo(utc, kolkata, Calendar.DAY_OF_MONTH) > 0);
        assertTrue(Dates.truncatedCompareTo(utc, kolkata, CalendarField.DAY_OF_MONTH) > 0);
    }

    @Test
    public void testRegisterDateCreator_customClass() {
        class CustomDate extends java.util.Date {
            public CustomDate(long time) {
                super(time);
            }
        }

        // CustomDate lives in com.landawn.abacus.util, a restricted package: registration is rejected.
        assertThrows(IllegalArgumentException.class, () -> Dates.registerDateCreator(CustomDate.class, CustomDate::new));
    }

    // ===== registerDateCreator =====

    @Test
    public void testRegisterDateCreator() {
        // java.util.Date is in a restricted package: registration is rejected.
        assertThrows(IllegalArgumentException.class, () -> Dates.registerDateCreator(java.util.Date.class, (millis) -> new java.util.Date(millis)));
        assertThrows(IllegalArgumentException.class, () -> Dates.registerDateCreator(java.util.Date.class, java.util.Date::new));

        assertThrows(IllegalArgumentException.class, () -> Dates.registerDateCreator(java.util.Date.class, null));
    }

    // ===== registerCalendarCreator =====

    @Test
    public void testRegisterCalendarCreator() {
        // java.util.Calendar is in a restricted package: registration is rejected.
        assertThrows(IllegalArgumentException.class, () -> Dates.registerCalendarCreator(Calendar.class, (millis, c) -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            return cal;
        }));

        assertThrows(IllegalArgumentException.class, () -> Dates.registerCalendarCreator(Calendar.class, null));
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

        // CustomCalendar lives in com.landawn.abacus.util, a restricted package: registration is rejected.
        assertThrows(IllegalArgumentException.class, () -> Dates.registerCalendarCreator(CustomCalendar.class, (millis, cal) -> {
            CustomCalendar custom = new CustomCalendar();
            custom.setTimeInMillis(millis);
            return custom;
        }));
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
        assertTrue(Dates.currentTimePlus(-5, TimeUnit.MINUTES).getTime() < Dates.currentTime().getTime());
    }

    @Test
    public void testCurrentDatePlus() {
        java.sql.Date currentDate = Dates.currentDate();
        assertTrue(Dates.currentDatePlus(1, TimeUnit.DAYS).getTime() > currentDate.getTime());
        assertTrue(Dates.currentDatePlus(-1, TimeUnit.DAYS).getTime() < currentDate.getTime());
    }

    @Test
    public void testCurrentTimestampPlus() {
        long now = System.currentTimeMillis();
        Timestamp rolled = Dates.currentTimestampPlus(-1, TimeUnit.HOURS);
        assertTrue(Math.abs(rolled.getTime() - (now - TimeUnit.HOURS.toMillis(1))) < 100);
        assertTrue(Dates.currentTimestampPlus(10, TimeUnit.SECONDS).getTime() > Dates.currentTimestamp().getTime());
    }

    @Test
    public void testCurrentJUDatePlus() {
        long now = System.currentTimeMillis();
        java.util.Date rolled = Dates.currentJUDatePlus(5, TimeUnit.MINUTES);
        long after = System.currentTimeMillis();
        long sampledTime = rolled.getTime() - TimeUnit.MINUTES.toMillis(5);
        // First-use initialization may exceed a fixed timing threshold; the sampled time must fall within the call.
        assertTrue(sampledTime >= now && sampledTime <= after);
        assertTrue(Dates.currentJUDatePlus(-2, TimeUnit.HOURS).getTime() < Dates.currentJUDate().getTime());
    }

    @Test
    public void testCurrentCalendarPlus() {
        long now = System.currentTimeMillis();
        Calendar rolled = Dates.currentCalendarPlus(10, TimeUnit.MILLISECONDS);
        assertTrue(Math.abs(rolled.getTimeInMillis() - (now + 10)) < 100);
        assertTrue(Dates.currentCalendarPlus(-3, TimeUnit.HOURS).getTimeInMillis() < Dates.currentCalendar().getTimeInMillis());
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

    // ===== Missing tests: createJUDate null calendar/date =====

    @Test
    public void testCreateJUDate_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((Calendar) null));
    }

    @Test
    public void testCreateJUDate_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createJUDate((java.util.Date) null));
    }

    @Test
    public void testCreateDate_fromMillis() {
        long millis = 3000000000L;
        java.sql.Date date = Dates.createDate(millis);

        assertNotNull(date);
        assertEquals(millis, date.getTime());
    }

    // ===== createDate(long, Class) via createDate(Calendar) path =====

    @Test
    public void testCreateDate_fromMillisViaTimestamp() {
        long millis = System.currentTimeMillis();
        java.sql.Timestamp ts = Dates.createTimestamp(millis);
        assertNotNull(ts);
        assertEquals(millis, ts.getTime());
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

    // ===== Missing tests: createDate null calendar/date =====

    @Test
    public void testCreateDate_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((Calendar) null));
    }

    @Test
    public void testCreateDate_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createDate((java.util.Date) null));
    }

    @Test
    public void testCreateTime_fromMillis() {
        long millis = 3000000000L;
        Time time = Dates.createTime(millis);

        assertNotNull(time);
        assertEquals(millis, time.getTime());
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

    // ===== Missing tests: createTime null calendar/date =====

    @Test
    public void testCreateTime_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((Calendar) null));
    }

    @Test
    public void testCreateTime_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTime((java.util.Date) null));
    }

    @Test
    public void testCreateTimestamp_fromMillis() {
        long millis = 3000000000L;
        Timestamp timestamp = Dates.createTimestamp(millis);

        assertNotNull(timestamp);
        assertEquals(millis, timestamp.getTime());
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

    // ===== Missing tests: createTimestamp null calendar/date =====

    @Test
    public void testCreateTimestamp_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((Calendar) null));
    }

    @Test
    public void testCreateTimestamp_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createTimestamp((java.util.Date) null));
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
        Calendar cal = Dates.createCalendar(millis, TimeZone.getTimeZone("UTC"));
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    // ===== Edge case: daylight saving time =====

    @Test
    public void testEdgeCase_daylightSavingTime() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Dates.createCalendar(1000000000L, tz);
        assertNotNull(cal);
        assertEquals(tz, cal.getTimeZone());
    }

    // ===== createCalendar(Calendar source, long millis) - via round/truncate/ceiling =====

    @Test
    public void testCreateCalendar_fromSource_viaRound() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/Los_Angeles"));
        cal.set(2023, Calendar.MARCH, 15, 10, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        GregorianCalendar result = Dates.round(cal, Calendar.HOUR_OF_DAY);
        assertNotNull(result);
        assertEquals("America/Los_Angeles", result.getTimeZone().getID());
    }

    @Test
    public void testCreateCalendar_fromCustomSourceWithoutLongConstructor() {
        class TokyoCalendar extends GregorianCalendar {
            TokyoCalendar() {
                super(TimeZone.getTimeZone("Asia/Tokyo"));
            }
        }

        TokyoCalendar source = new TokyoCalendar();
        source.set(2024, Calendar.JANUARY, 2, 3, 45, 50);
        source.set(Calendar.MILLISECOND, 123);

        TokyoCalendar result = Dates.truncate(source, Calendar.HOUR_OF_DAY);
        assertEquals(TokyoCalendar.class, result.getClass());
        assertEquals("Asia/Tokyo", result.getTimeZone().getID());
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND));
    }

    // ===== createCalendar(Calendar, long) via GregorianCalendar creator pool =====

    @Test
    public void testCreateCalendar_viaGregorianCalendarCreatorPool() {
        // roll(T calendar, int amount, CalendarField unit) calls createCalendar(calendar, millis)
        // which uses the calendarCreatorPool GregorianCalendar creator (L733-741)
        GregorianCalendar source = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        source.set(2024, Calendar.JUNE, 15, 12, 0, 0);
        source.set(Calendar.MILLISECOND, 0);

        GregorianCalendar rolled = Dates.roll(source, 1, CalendarField.DAY_OF_MONTH);
        assertNotNull(rolled);
        assertEquals(2024, rolled.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, rolled.get(Calendar.MONTH));
        assertEquals(16, rolled.get(Calendar.DAY_OF_MONTH));
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

    // ===== Missing tests: createCalendar null calendar/date =====

    @Test
    public void testCreateCalendar_nullCalendar() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((Calendar) null));
    }

    @Test
    public void testCreateCalendar_nullDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.createCalendar((java.util.Date) null));
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
        GregorianCalendar cal = Dates.createGregorianCalendar(millis, TimeZone.getTimeZone("GMT"));
        assertNotNull(cal);
        assertEquals(millis, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("GMT"), cal.getTimeZone());
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
        XMLGregorianCalendar cal = Dates.createXMLGregorianCalendar(millis, TimeZone.getTimeZone("GMT"));
        assertNotNull(cal);
    }

    // ===== createXMLGregorianCalendar(long) =====

    @Test
    public void testCreateXMLGregorianCalendar_fromCurrentMillis() {
        long millis = System.currentTimeMillis();
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(millis);
        assertNotNull(xmlCal);
        assertTrue(xmlCal.getYear() >= 2020);
    }

    @Test
    public void testCreateXMLGregorianCalendar_fromCurrentMillisAndTimeZone() {
        long millis = System.currentTimeMillis();
        XMLGregorianCalendar xmlCal = Dates.createXMLGregorianCalendar(millis, TimeZone.getTimeZone("UTC"));
        assertNotNull(xmlCal);
        assertTrue(xmlCal.getYear() >= 2020);
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
    public void testDTF_of_roundTrip() {
        Dates.DTF dtf = Dates.DTF.of("MM/dd/yyyy HH:mm");
        LocalDateTime ldt = LocalDateTime.of(2023, 12, 25, 15, 30);

        String formatted = dtf.format(ldt);
        assertEquals("12/25/2023 15:30", formatted);
        assertEquals(ldt, dtf.parseToLocalDateTime(formatted));
    }

    @Test
    public void testDTF_of_quotedZPatternRemainsLiteral() {
        // DTF.of honors DateTimeFormatter pattern semantics; fixed UTC conversion belongs to the named constant.
        Dates.DTF dtf = Dates.DTF.of(Dates.ISO_8601_DATE_TIME_FORMAT);
        OffsetDateTime plusFiveThirty = Instant.EPOCH.atOffset(ZoneOffset.ofHoursMinutes(5, 30));
        assertEquals("1970-01-01T05:30:00Z", dtf.format(plusFiveThirty));
        assertEquals("1970-01-01T00:00:00Z", Dates.DTF.ISO_8601_DATE_TIME.format(plusFiveThirty));
    }

    @Test
    public void testDTF_of_invalidArgs() {
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of(null));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of(""));
        // '{' is a reserved character in DateTimeFormatter patterns.
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("{"));
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
    public void testDTF_ISO_ZONED_DATE_TIME() {
        assertNotNull(Dates.DTF.ISO_ZONED_DATE_TIME);
        LocalDateTime ldt = LocalDateTime.of(2022, 1, 1, 13, 45, 30);
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/New_York"));
        String result = Dates.DTF.ISO_ZONED_DATE_TIME.format(zdt);
        assertTrue(result.contains("2022-01-01T13:45:30"));
        assertTrue(result.contains("[America/New_York]"));
        ZonedDateTime withFraction = zdt.withNano(123_456_789);
        assertEquals(withFraction.withNano(0), Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(Dates.DTF.ISO_ZONED_DATE_TIME.format(withFraction)));
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

    // ===== Constants =====

    @Test
    public void testConstants() {
        assertNotNull(Dates.UTC_ZONE_ID);
        assertNotNull(Dates.GMT_ZONE_ID);

        assertEquals(ZoneId.of("UTC"), Dates.UTC_ZONE_ID);
        assertEquals(ZoneId.of("GMT"), Dates.GMT_ZONE_ID);

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
        assertEquals("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Dates.HTTP_DATE_FORMAT);
        assertEquals(Dates.HTTP_DATE_FORMAT, Dates.RFC_1123_DATE_TIME_FORMAT);
        assertEquals(1001, Dates.SEMI_MONTH);
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

    @Test
    public void testCallerOwnedTimeZoneIsNotRetainedAcrossCalls() {
        // Calendar pooling was removed (pooled calendars leaked leniency and zone-rule state between
        // calls); every parse now uses a fresh calendar. A caller-owned mutable zone must therefore
        // affect only the calls it is passed to — mutate it between calls and observe both results.
        final SimpleTimeZone callerOwned = new SimpleTimeZone((int) TimeUnit.HOURS.toMillis(2), "DatesTest-caller-owned-zone");

        final long first = Dates.parseToJUDate("2024-01-02 03:04:05", "yyyy-MM-dd HH:mm:ss", callerOwned).getTime();

        callerOwned.setRawOffset((int) TimeUnit.HOURS.toMillis(5));

        final long second = Dates.parseToJUDate("2024-01-02 03:04:05", "yyyy-MM-dd HH:mm:ss", callerOwned).getTime();

        assertEquals(TimeUnit.HOURS.toMillis(3), first - second, "each call must honor the zone's current rules, not retained state");
    }
}
