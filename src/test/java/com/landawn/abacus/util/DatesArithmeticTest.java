package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DatesArithmeticTest extends TestBase {

    private static final long EPOCH = 1_000_000_000L;

    private static java.util.Date epochDate() {
        return new java.util.Date(EPOCH);
    }

    private static Calendar epochCalendar() {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(EPOCH);
        return cal;
    }

    private static void assertField(final java.util.Date date, final int field, final int expected) {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        assertEquals(expected, cal.get(field));
    }

    // ===== set* =====

    @Test
    public void testSetFields() {
        final java.util.Date date = epochDate();
        assertField(Dates.setYears(date, 2025), Calendar.YEAR, 2025);
        assertField(Dates.setMonths(date, 5), Calendar.MONTH, 5);
        assertField(Dates.setDays(date, 15), Calendar.DAY_OF_MONTH, 15);
        assertField(Dates.setHours(date, 14), Calendar.HOUR_OF_DAY, 14);
        assertField(Dates.setMinutes(date, 30), Calendar.MINUTE, 30);
        assertField(Dates.setSeconds(date, 45), Calendar.SECOND, 45);
        assertField(Dates.setMilliseconds(date, 123), Calendar.MILLISECOND, 123);
    }

    @Test
    public void testSetFields_timestamp() {
        final Timestamp ts = Dates.parseToTimestamp("2023-03-15 10:30:45.123");
        final Timestamp years = Dates.setYears(ts, 2025);
        assertTrue(years instanceof Timestamp);
        assertField(years, Calendar.YEAR, 2025);
        assertField(Dates.setMonths(ts, 5), Calendar.MONTH, Calendar.JUNE);
    }

    @Test
    public void testSetFields_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(null, 2023));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(null, 5));
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(null, 15));
        assertThrows(IllegalArgumentException.class, () -> Dates.setHours(null, 14));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMinutes(null, 30));
        assertThrows(IllegalArgumentException.class, () -> Dates.setSeconds(null, 45));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMilliseconds(null, 123));
    }

    @Test
    public void testSet_outOfRangeAmount_throws() {
        final java.util.Date date = new java.util.Date(1736937045000L);
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(date, 12));
        assertThrows(IllegalArgumentException.class, () -> Dates.setHours(date, 24));

        final java.util.Date feb1 = Dates.parseToJUDate("2023-02-01", "yyyy-MM-dd");
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(feb1, 29));
    }

    // ===== roll =====

    @Test
    public void testRoll_date() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.roll(date, 5, TimeUnit.DAYS).getTime() > date.getTime());
        assertTrue(Dates.roll(date, -5, TimeUnit.DAYS).getTime() < date.getTime());
        assertEquals(date.getTime(), Dates.roll(date, 0, TimeUnit.DAYS).getTime());
        assertTrue(Dates.roll(date, 5, CalendarField.DAY_OF_MONTH).getTime() > date.getTime());
    }

    @Test
    public void testRoll_calendar() {
        final Calendar cal = epochCalendar();
        assertTrue(Dates.roll(cal, 5, TimeUnit.DAYS).getTimeInMillis() > EPOCH);
        assertTrue(Dates.roll(cal, -5, TimeUnit.DAYS).getTimeInMillis() < EPOCH);
        assertEquals(cal.getTimeInMillis(), Dates.roll(cal, 0, TimeUnit.DAYS).getTimeInMillis());
        assertTrue(Dates.roll(cal, 5, CalendarField.DAY_OF_MONTH).getTimeInMillis() > EPOCH);
    }

    @Test
    public void testRoll_timestamp() {
        final Timestamp ts = new Timestamp(EPOCH);
        final Timestamp rolled = Dates.roll(ts, 1, TimeUnit.DAYS);
        assertTrue(rolled instanceof Timestamp);
        assertEquals(ts.getTime() + TimeUnit.DAYS.toMillis(1), rolled.getTime());
        assertTrue(Dates.roll(ts, 1, CalendarField.DAY_OF_MONTH) instanceof Timestamp);
    }

    @Test
    public void testRoll_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 5, TimeUnit.DAYS));
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((java.util.Date) null, 5, CalendarField.DAY_OF_MONTH));
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 5, TimeUnit.DAYS));
        assertThrows(IllegalArgumentException.class, () -> Dates.roll((Calendar) null, 5, CalendarField.DAY_OF_MONTH));
    }

    // ===== add* =====

    @Test
    public void testAddYears() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.addYears(date, 1).getTime() > date.getTime());
        assertTrue(Dates.addYears(date, -1).getTime() < date.getTime());
        assertEquals(date.getTime(), Dates.addYears(date, 0).getTime());

        final Calendar cal = epochCalendar();
        assertTrue(Dates.addYears(cal, 1).getTimeInMillis() > cal.getTimeInMillis());
        assertTrue(Dates.addYears(cal, -1).getTimeInMillis() < cal.getTimeInMillis());
        assertEquals(cal.getTimeInMillis(), Dates.addYears(cal, 0).getTimeInMillis());
    }

    @Test
    public void testAddYears_leapDayToLeapYear() {
        final java.util.Date feb29 = Dates.parseToJUDate("2024-02-29", "yyyy-MM-dd");
        assertEquals("2028-02-29", Dates.format(Dates.addYears(feb29, 4), "yyyy-MM-dd"));
    }

    @Test
    public void testAddMonths() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.addMonths(date, 3).getTime() > date.getTime());
        assertTrue(Dates.addMonths(date, -3).getTime() < date.getTime());

        final Calendar cal = epochCalendar();
        assertTrue(Dates.addMonths(cal, 3).getTimeInMillis() > cal.getTimeInMillis());
        assertTrue(Dates.addMonths(cal, -3).getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddWeeks() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.addWeeks(date, 2).getTime() > date.getTime());
        assertTrue(Dates.addWeeks(date, -2).getTime() < date.getTime());

        final Calendar cal = epochCalendar();
        assertTrue(Dates.addWeeks(cal, 2).getTimeInMillis() > cal.getTimeInMillis());
        assertTrue(Dates.addWeeks(cal, -2).getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddDays() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.addDays(date, 7).getTime() > date.getTime());
        assertTrue(Dates.addDays(date, -7).getTime() < date.getTime());

        final Calendar cal = epochCalendar();
        assertTrue(Dates.addDays(cal, 7).getTimeInMillis() > cal.getTimeInMillis());
        assertTrue(Dates.addDays(cal, -7).getTimeInMillis() < cal.getTimeInMillis());

        final Timestamp ts = Dates.parseToTimestamp("2023-03-15 10:30:45.123");
        final Timestamp result = Dates.addDays(ts, 1);
        assertTrue(result instanceof Timestamp);
        assertTrue(result.getTime() > ts.getTime());
    }

    @Test
    public void testAddDays_boundaryFebToMarch() {
        final Calendar cal = Calendar.getInstance();
        cal.set(2025, Calendar.FEBRUARY, 28, 23, 59, 59);
        final Calendar result = Calendar.getInstance();
        result.setTime(Dates.addDays(cal.getTime(), 1));
        assertEquals(Calendar.MARCH, result.get(Calendar.MONTH));
        assertEquals(1, result.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testAddHours() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.addHours(date, 5).getTime() > date.getTime());
        assertTrue(Dates.addHours(date, -5).getTime() < date.getTime());

        final Calendar cal = epochCalendar();
        assertTrue(Dates.addHours(cal, 5).getTimeInMillis() > cal.getTimeInMillis());
        assertTrue(Dates.addHours(cal, -5).getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddMinutes() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.addMinutes(date, 30).getTime() > date.getTime());
        assertTrue(Dates.addMinutes(date, -30).getTime() < date.getTime());

        final Calendar cal = epochCalendar();
        assertTrue(Dates.addMinutes(cal, 30).getTimeInMillis() > cal.getTimeInMillis());
        assertTrue(Dates.addMinutes(cal, -30).getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddSeconds() {
        final java.util.Date date = epochDate();
        assertTrue(Dates.addSeconds(date, 45).getTime() > date.getTime());
        assertTrue(Dates.addSeconds(date, -45).getTime() < date.getTime());

        final Calendar cal = epochCalendar();
        assertTrue(Dates.addSeconds(cal, 45).getTimeInMillis() > cal.getTimeInMillis());
        assertTrue(Dates.addSeconds(cal, -45).getTimeInMillis() < cal.getTimeInMillis());
    }

    @Test
    public void testAddMilliseconds() {
        final java.util.Date date = epochDate();
        assertEquals(500, Dates.addMilliseconds(date, 500).getTime() - date.getTime());
        assertEquals(500, date.getTime() - Dates.addMilliseconds(date, -500).getTime());

        final Calendar cal = epochCalendar();
        assertEquals(500, Dates.addMilliseconds(cal, 500).getTimeInMillis() - cal.getTimeInMillis());
        assertEquals(500, cal.getTimeInMillis() - Dates.addMilliseconds(cal, -500).getTimeInMillis());
    }

    @Test
    public void testAdd_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.addYears((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addYears((Calendar) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addMonths((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addMonths((Calendar) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addWeeks((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addWeeks((Calendar) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addDays((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addDays((Calendar) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addHours((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addHours((Calendar) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addMinutes((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addMinutes((Calendar) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addSeconds((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addSeconds((Calendar) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addMilliseconds((java.util.Date) null, 1));
        assertThrows(IllegalArgumentException.class, () -> Dates.addMilliseconds((Calendar) null, 1));
    }

    @Test
    public void testDateAdditionRejectsEpochOverflow() {
        final java.util.Date max = new java.util.Date(Long.MAX_VALUE);
        final java.util.Date min = new java.util.Date(Long.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> Dates.addMilliseconds(max, 1));
        assertThrows(ArithmeticException.class, () -> Dates.addSeconds(max, 1));
        assertThrows(ArithmeticException.class, () -> Dates.addMilliseconds(min, -1L));
    }

    @Test
    public void testTimeUnitArithmeticRejectsOverflow() {
        assertThrows(ArithmeticException.class, () -> Dates.addMilliseconds(new java.util.Date(Long.MAX_VALUE), 1L));
        assertThrows(ArithmeticException.class, () -> Dates.addMilliseconds(new java.util.Date(Long.MIN_VALUE), -1L));
        assertThrows(ArithmeticException.class, () -> Dates.currentTimeMillisPlus(Long.MAX_VALUE, TimeUnit.DAYS));
    }

    @Test
    public void testAdd_intMaxAmount_noIntermediateOverflow() {
        final java.util.Date epoch = new java.util.Date(0L);
        assertEquals(Integer.MAX_VALUE * 3600000L, Dates.addHours(epoch, Integer.MAX_VALUE).getTime());
        assertEquals(Integer.MAX_VALUE * 60000L, Dates.addMinutes(epoch, Integer.MAX_VALUE).getTime());
        assertEquals(Integer.MAX_VALUE * 1000L, Dates.addSeconds(epoch, Integer.MAX_VALUE).getTime());
        assertEquals(Integer.MAX_VALUE, Dates.addMilliseconds(epoch, Integer.MAX_VALUE).getTime());

        final Calendar utcCal = Dates.createCalendar(0L, TimeZone.getTimeZone("UTC"));
        assertEquals(Integer.MAX_VALUE * 3600000L, Dates.addHours(utcCal, Integer.MAX_VALUE).getTimeInMillis());
        assertEquals(Integer.MAX_VALUE * 60000L, Dates.addMinutes(utcCal, Integer.MAX_VALUE).getTimeInMillis());
    }

    // ===== round / truncate / ceiling =====

    @Test
    public void testRound() {
        final java.util.Date date = Dates.parseToJUDate("2023-03-15 10:45:00");
        assertField(Dates.round(date, CalendarField.HOUR_OF_DAY), Calendar.HOUR_OF_DAY, 11);
        assertField(Dates.round(date, Calendar.HOUR_OF_DAY), Calendar.HOUR_OF_DAY, 11);

        final Calendar cal = new GregorianCalendar(2023, Calendar.MARCH, 15, 10, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(46, Dates.round(cal, Calendar.MINUTE).get(Calendar.MINUTE));
        assertEquals(46, Dates.round(cal, CalendarField.MINUTE).get(Calendar.MINUTE));
    }

    @Test
    public void testRound_amPmAndSemiMonth() {
        assertField(Dates.round(Dates.parseToJUDate("2023-03-15 06:00:00"), Calendar.AM_PM), Calendar.HOUR_OF_DAY, 12);

        assertEquals("2023-01-01", Dates.format(Dates.round(Dates.parseToJUDate("2023-01-08"), Dates.SEMI_MONTH), "yyyy-MM-dd"));
        assertEquals("2023-01-16", Dates.format(Dates.round(Dates.parseToJUDate("2023-01-09"), Dates.SEMI_MONTH), "yyyy-MM-dd"));
        assertEquals("2023-01-16", Dates.format(Dates.round(Dates.parseToJUDate("2023-01-15"), Dates.SEMI_MONTH), "yyyy-MM-dd"));
        assertEquals("2023-01-16", Dates.format(Dates.round(Dates.parseToJUDate("2023-01-16"), Dates.SEMI_MONTH), "yyyy-MM-dd"));
        assertEquals("2023-01-16", Dates.format(Dates.round(Dates.parseToJUDate("2023-01-23"), Dates.SEMI_MONTH), "yyyy-MM-dd"));
        assertEquals("2023-02-01", Dates.format(Dates.round(Dates.parseToJUDate("2023-01-31"), Dates.SEMI_MONTH), "yyyy-MM-dd"));
    }

    @Test
    public void testRound_yearTooLarge_arithmeticException() {
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, 280_000_001);
        assertThrows(ArithmeticException.class, () -> Dates.round(cal.getTime(), Calendar.MINUTE));
    }

    @Test
    public void testTruncate() {
        final java.util.Date date = Dates.parseToJUDate("2023-03-15 10:45:30");
        final java.util.Date truncatedHour = Dates.truncate(date, CalendarField.HOUR_OF_DAY);
        assertField(truncatedHour, Calendar.HOUR_OF_DAY, 10);
        assertField(truncatedHour, Calendar.MINUTE, 0);
        assertField(truncatedHour, Calendar.SECOND, 0);

        final Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.JUNE, 10, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 500);
        final Calendar truncated = Dates.truncate(cal, Calendar.HOUR_OF_DAY);
        assertEquals(14, truncated.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, truncated.get(Calendar.MINUTE));
        assertEquals(0, truncated.get(Calendar.SECOND));
        assertEquals(0, Dates.truncate(cal, CalendarField.MINUTE).get(Calendar.SECOND));
    }

    @Test
    public void testTruncate_year_clearsLowerFields() {
        final java.util.Date date = Dates.parseToJUDate("2023-06-15 10:30:45.123", "yyyy-MM-dd HH:mm:ss.SSS");
        assertEquals("2023-01-01 00:00:00.000", Dates.format(Dates.truncate(date, Calendar.YEAR), "yyyy-MM-dd HH:mm:ss.SSS"));
    }

    @Test
    public void testCeiling() {
        final java.util.Date date = Dates.parseToJUDate("2023-03-15 10:01:00");
        assertField(Dates.ceiling(date, CalendarField.HOUR_OF_DAY), Calendar.HOUR_OF_DAY, 11);

        final Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.JUNE, 10, 14, 1, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final Calendar ceiled = Dates.ceiling(cal, Calendar.HOUR_OF_DAY);
        assertEquals(15, ceiled.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, ceiled.get(Calendar.MINUTE));

        final Calendar minuteCal = new GregorianCalendar(2023, Calendar.MARCH, 15, 10, 45, 1);
        minuteCal.set(Calendar.MILLISECOND, 0);
        assertEquals(46, Dates.ceiling(minuteCal, Calendar.MINUTE).get(Calendar.MINUTE));
        assertEquals(46, Dates.ceiling(minuteCal, CalendarField.MINUTE).get(Calendar.MINUTE));
    }

    @Test
    public void testCeiling_exactBoundary_isUnchanged() {
        final java.util.Date date = Dates.parseToJUDate("2024-11-24 10:00:00.000", "yyyy-MM-dd HH:mm:ss.SSS");
        assertEquals("2024-11-24 10:00:00.000", Dates.format(Dates.ceiling(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss.SSS"));
        final java.util.Date pastBoundary = Dates.parseToJUDate("2024-11-24 10:00:00.001", "yyyy-MM-dd HH:mm:ss.SSS");
        assertEquals("2024-11-24 11:00:00.000", Dates.format(Dates.ceiling(pastBoundary, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss.SSS"));
        assertEquals("2024-11-24 10:00:00.000", Dates.format(Dates.truncate(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss.SSS"));
        assertEquals("2024-11-24 10:00:00.000", Dates.format(Dates.round(date, Calendar.HOUR_OF_DAY), "yyyy-MM-dd HH:mm:ss.SSS"));
    }

    @Test
    public void testRoundTruncateCeiling_null_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, Calendar.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.round((java.util.Date) null, CalendarField.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.round((Calendar) null, Calendar.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.round((Calendar) null, CalendarField.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((java.util.Date) null, Calendar.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((java.util.Date) null, CalendarField.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, Calendar.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, CalendarField.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((java.util.Date) null, Calendar.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((java.util.Date) null, CalendarField.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((Calendar) null, Calendar.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling((Calendar) null, CalendarField.SECOND));
    }

    @Test
    public void testRoundTruncateCeiling_unsupportedField_throws() {
        final java.util.Date date = new java.util.Date(1736937045000L);
        final Calendar cal = Dates.createCalendar(1736937045000L);

        assertThrows(IllegalArgumentException.class, () -> Dates.round(date, Calendar.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(date, Calendar.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling(date, Calendar.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.round(date, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(date, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling(date, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.round(cal, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(cal, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling(cal, CalendarField.WEEK_OF_YEAR));
    }

    // ===== the deprecated roll(..., long, TimeUnit) migration notes =====

    /**
     * Pins the replacements the {@code @deprecated} note names: the {@code long} overloads exist and agree
     * with {@code roll} for a {@code long} amount, {@code addDays} has only an {@code int} overload, and
     * the two sub-millisecond units the method accepts have no {@code add*} replacement at all.
     */
    @Test
    public void testRollReplacementsAcceptALongAmount() throws Exception {
        final Calendar cal = epochCalendar();
        final long amount = 5L;

        assertEquals(Dates.roll(cal, amount, TimeUnit.HOURS).getTimeInMillis(), Dates.addHours(cal, amount).getTimeInMillis());
        assertEquals(Dates.roll(cal, amount, TimeUnit.MINUTES).getTimeInMillis(), Dates.addMinutes(cal, amount).getTimeInMillis());
        assertEquals(Dates.roll(cal, amount, TimeUnit.SECONDS).getTimeInMillis(), Dates.addSeconds(cal, amount).getTimeInMillis());
        assertEquals(Dates.roll(cal, amount, TimeUnit.MILLISECONDS).getTimeInMillis(), Dates.addMilliseconds(cal, amount).getTimeInMillis());
        assertEquals(EPOCH + 5 * 3_600_000L, Dates.addHours(cal, amount).getTimeInMillis());

        for (final String name : new String[] { "addHours", "addMinutes", "addSeconds", "addMilliseconds" }) {
            assertNotNull(Dates.class.getMethod(name, Calendar.class, long.class));
        }

        assertThrows(NoSuchMethodException.class, () -> Dates.class.getMethod("addDays", Calendar.class, long.class));
        assertNotNull(Dates.class.getMethod("addDays", Calendar.class, int.class));

        // The roll(java.util.Date, long, TimeUnit) twin's note makes the same claims.
        for (final String name : new String[] { "addHours", "addMinutes", "addSeconds", "addMilliseconds" }) {
            assertNotNull(Dates.class.getMethod(name, java.util.Date.class, long.class));
        }

        assertThrows(NoSuchMethodException.class, () -> Dates.class.getMethod("addDays", java.util.Date.class, long.class));
        assertNotNull(Dates.class.getMethod("addDays", java.util.Date.class, int.class));

        // NANOSECONDS and MICROSECONDS are accepted and truncated toward zero; no add* method takes them.
        assertEquals(1L, Dates.roll(cal, 1_500_000L, TimeUnit.NANOSECONDS).getTimeInMillis() - EPOCH);
        assertEquals(0L, Dates.roll(cal, 999_999L, TimeUnit.NANOSECONDS).getTimeInMillis() - EPOCH);
        assertEquals(2L, Dates.roll(cal, 2_500L, TimeUnit.MICROSECONDS).getTimeInMillis() - EPOCH);
        assertEquals(-1L, Dates.roll(cal, -1_500_000L, TimeUnit.NANOSECONDS).getTimeInMillis() - EPOCH);
    }
}
