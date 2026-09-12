package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DatesCompareTest extends TestBase {

    private static Calendar of(final int year, final int month, final int day, final int hour, final int minute, final int second) {
        final Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    // ===== truncatedEquals / truncatedCompareTo =====

    @Test
    public void testTruncatedEquals() {
        final Calendar cal1 = of(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        final Calendar cal2 = of(2025, Calendar.OCTOBER, 4, 14, 30, 50);

        assertTrue(Dates.truncatedEquals(cal1, cal2, CalendarField.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1, cal2, CalendarField.SECOND));
        assertTrue(Dates.truncatedEquals(cal1, cal2, Calendar.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1, cal2, Calendar.SECOND));
        assertTrue(Dates.truncatedEquals(cal1, cal1, CalendarField.SECOND));

        assertTrue(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), CalendarField.MINUTE));
        assertFalse(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), CalendarField.SECOND));
        assertTrue(Dates.truncatedEquals(cal1.getTime(), cal2.getTime(), Calendar.MINUTE));
        assertTrue(Dates.truncatedEquals(cal1.getTime(), cal1.getTime(), Calendar.SECOND));

        final java.util.Date otherDay = Dates.parseToJUDate("2023-03-16 10:30:45");
        assertFalse(Dates.truncatedEquals(Dates.parseToJUDate("2023-03-15 10:30:45"), otherDay, CalendarField.DAY_OF_MONTH));
    }

    @Test
    public void testTruncatedCompareTo() {
        final Calendar cal1 = of(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        final Calendar cal2 = of(2025, Calendar.OCTOBER, 4, 14, 35, 50);

        assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, CalendarField.HOUR_OF_DAY));
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, CalendarField.MINUTE) < 0);
        assertEquals(0, Dates.truncatedCompareTo(cal1, cal2, Calendar.HOUR_OF_DAY));
        assertTrue(Dates.truncatedCompareTo(cal1, cal2, Calendar.MINUTE) < 0);
        assertEquals(0, Dates.truncatedCompareTo(cal1, cal1, CalendarField.SECOND));

        assertEquals(0, Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), CalendarField.HOUR_OF_DAY));
        assertTrue(Dates.truncatedCompareTo(cal1.getTime(), cal2.getTime(), Calendar.MINUTE) < 0);
        assertEquals(0, Dates.truncatedCompareTo(cal1.getTime(), cal1.getTime(), Calendar.SECOND));

        final java.util.Date earlier = Dates.parseToJUDate("2023-03-15 10:30:45");
        final java.util.Date later = Dates.parseToJUDate("2023-03-16 10:30:45");
        assertTrue(Dates.truncatedCompareTo(earlier, later, CalendarField.DAY_OF_MONTH) < 0);
        assertTrue(Dates.truncatedCompareTo(later, earlier, CalendarField.DAY_OF_MONTH) > 0);
        assertTrue(Dates.truncatedCompareTo(later, earlier, Calendar.DAY_OF_MONTH) > 0);
    }

    // ===== getFragment* =====

    @Test
    public void testGetFragment() {
        final Calendar cal = of(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);

        assertEquals(123, Dates.getFragmentInMilliseconds(cal.getTime(), CalendarField.SECOND));
        assertEquals(123, Dates.getFragmentInMilliseconds(cal, CalendarField.SECOND));
        assertEquals(45, Dates.getFragmentInSeconds(cal.getTime(), CalendarField.MINUTE));
        assertEquals(45, Dates.getFragmentInSeconds(cal, CalendarField.MINUTE));
        assertEquals(30, Dates.getFragmentInMinutes(cal.getTime(), CalendarField.HOUR_OF_DAY));
        assertEquals(30, Dates.getFragmentInMinutes(cal, CalendarField.HOUR_OF_DAY));
        assertEquals(14, Dates.getFragmentInHours(cal.getTime(), CalendarField.DAY_OF_MONTH));
        assertEquals(14, Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH));
        assertTrue(Dates.getFragmentInDays(cal.getTime(), CalendarField.MONTH) >= 0);
        assertTrue(Dates.getFragmentInDays(cal, CalendarField.MONTH) >= 0);
    }

    @Test
    public void testGetFragment_range() {
        final java.util.Date date = new java.util.Date();
        final Calendar cal = Calendar.getInstance();
        assertTrue(Dates.getFragmentInMilliseconds(date, CalendarField.SECOND) >= 0 && Dates.getFragmentInMilliseconds(date, CalendarField.SECOND) < 1000);
        assertTrue(Dates.getFragmentInSeconds(cal, CalendarField.MINUTE) >= 0 && Dates.getFragmentInSeconds(cal, CalendarField.MINUTE) < 60);
        assertTrue(Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY) >= 0 && Dates.getFragmentInMinutes(date, CalendarField.HOUR_OF_DAY) < 60);
        assertTrue(Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH) >= 0 && Dates.getFragmentInHours(cal, CalendarField.DAY_OF_MONTH) < 24);
        assertTrue(Dates.getFragmentInDays(date, CalendarField.MONTH) >= 0 && Dates.getFragmentInDays(date, CalendarField.MONTH) <= 31);
        assertTrue(Dates.getFragmentInDays(cal, CalendarField.YEAR) >= 0 && Dates.getFragmentInDays(cal, CalendarField.YEAR) <= 366);
    }

    @Test
    public void testGetFragment_unsupportedFragment_throws() {
        final java.util.Date date = new java.util.Date(1736937045000L);
        final Calendar cal = Dates.createCalendar(1736937045000L);
        assertThrows(IllegalArgumentException.class, () -> Dates.getFragmentInMilliseconds(date, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.getFragmentInSeconds(date, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.getFragmentInMinutes(cal, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.getFragmentInHours(cal, CalendarField.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.getFragmentInDays(date, CalendarField.WEEK_OF_YEAR));
    }

    // ===== isSameDay / Month / Year =====

    @Test
    public void testIsSameDay() {
        final Calendar cal1 = of(2025, Calendar.OCTOBER, 4, 10, 30, 45);
        final Calendar cal2 = of(2025, Calendar.OCTOBER, 4, 15, 45, 30);
        final Calendar nextDay = of(2025, Calendar.OCTOBER, 5, 10, 30, 45);

        assertTrue(Dates.isSameDay(cal1.getTime(), cal2.getTime()));
        assertFalse(Dates.isSameDay(cal1.getTime(), nextDay.getTime()));
        assertTrue(Dates.isSameDay(cal1, cal2));
        assertFalse(Dates.isSameDay(cal1, nextDay));
        assertTrue(Dates.isSameDay(cal1.getTime(), cal1.getTime()));

        final Calendar laterMinutes = (Calendar) cal1.clone();
        laterMinutes.add(Calendar.MINUTE, 2);
        assertTrue(Dates.isSameDay(cal1, laterMinutes));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay((java.util.Date) null, cal1.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(cal1.getTime(), (java.util.Date) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay((Calendar) null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(cal1, (Calendar) null));
    }

    @Test
    public void testIsSameMonth() {
        final Calendar cal1 = of(2025, Calendar.OCTOBER, 4, 10, 30, 45);
        final Calendar sameMonth = of(2025, Calendar.OCTOBER, 15, 15, 45, 30);
        final Calendar nextMonth = of(2025, Calendar.NOVEMBER, 4, 10, 30, 45);

        assertTrue(Dates.isSameMonth(cal1.getTime(), sameMonth.getTime()));
        assertFalse(Dates.isSameMonth(cal1.getTime(), nextMonth.getTime()));
        assertTrue(Dates.isSameMonth(cal1, sameMonth));
        assertFalse(Dates.isSameMonth(cal1, nextMonth));
        assertTrue(Dates.isSameMonth(cal1.getTime(), cal1.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth((java.util.Date) null, cal1.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(cal1.getTime(), (java.util.Date) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth((Calendar) null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(cal1, (Calendar) null));
    }

    @Test
    public void testIsSameYear() {
        final Calendar cal1 = of(2025, Calendar.OCTOBER, 4, 10, 30, 45);
        final Calendar sameYear = of(2025, Calendar.DECEMBER, 31, 23, 59, 59);
        final Calendar nextYear = of(2026, Calendar.JANUARY, 1, 0, 0, 0);

        assertTrue(Dates.isSameYear(cal1.getTime(), sameYear.getTime()));
        assertFalse(Dates.isSameYear(cal1.getTime(), nextYear.getTime()));
        assertTrue(Dates.isSameYear(cal1, sameYear));
        assertFalse(Dates.isSameYear(cal1, nextYear));
        assertTrue(Dates.isSameYear(cal1.getTime(), cal1.getTime()));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear((java.util.Date) null, cal1.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(cal1.getTime(), (java.util.Date) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear((Calendar) null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(cal1, (Calendar) null));
    }

    @Test
    public void testIsSameDay_calendarsInDifferentZones() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final long millis = Dates.parseToJUDate("2023-01-01T12:00:00Z", Dates.ISO_8601_DATE_TIME_FORMAT, utc).getTime();
        final Calendar utcCal = Dates.createCalendar(millis, utc);
        final Calendar kiritimatiCal = Dates.createCalendar(millis, TimeZone.getTimeZone("Pacific/Kiritimati"));
        final Calendar nomeCal = Dates.createCalendar(millis, TimeZone.getTimeZone("America/Nome"));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(utcCal, kiritimatiCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(kiritimatiCal, utcCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(utcCal, nomeCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(utcCal, kiritimatiCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(utcCal, kiritimatiCal));
        assertTrue(Dates.isSameDay(utcCal, kiritimatiCal, ZoneId.of("UTC")));
        assertTrue(Dates.isSameDay(utcCal, kiritimatiCal, ZoneId.of("Pacific/Kiritimati")));
        assertTrue(Dates.isSameDay(utcCal, nomeCal, utc));
    }

    // ===== isSameInstant / isSameLocalTime =====

    @Test
    public void testIsSameInstant() {
        final java.util.Date date1 = new java.util.Date(1_000_000_000L);
        final java.util.Date date2 = new java.util.Date(1_000_000_000L);
        final java.util.Date date3 = new java.util.Date(1_000_000_001L);
        assertTrue(Dates.isSameInstant(date1, date2));
        assertFalse(Dates.isSameInstant(date1, date3));
        assertTrue(Dates.isSameInstant(date1, date1));

        final Calendar cal1 = Dates.createCalendar(1_000_000_000L, TimeZone.getTimeZone("UTC"));
        final Calendar calTokyo = Dates.createCalendar(1_000_000_000L, TimeZone.getTimeZone("Asia/Tokyo"));
        final Calendar calLater = Dates.createCalendar(2_000_000_000L, TimeZone.getTimeZone("UTC"));
        assertTrue(Dates.isSameInstant(cal1, calTokyo));
        assertFalse(Dates.isSameInstant(cal1, calLater));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant((java.util.Date) null, date1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(date1, (java.util.Date) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant((Calendar) null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant(cal1, (Calendar) null));
    }

    @Test
    public void testIsSameLocalTime() {
        final Calendar cal1 = of(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal1.set(Calendar.MILLISECOND, 123);
        final Calendar cal2 = of(2025, Calendar.OCTOBER, 4, 14, 30, 45);
        cal2.set(Calendar.MILLISECOND, 123);
        final Calendar later = of(2025, Calendar.OCTOBER, 4, 14, 30, 46);

        assertTrue(Dates.isSameLocalTime(cal1, cal2));
        assertTrue(Dates.isSameLocalTime(cal1, cal1));
        assertFalse(Dates.isSameLocalTime(cal1, later));

        final GregorianCalendar gc1 = new GregorianCalendar(2023, Calendar.MARCH, 15, 10, 30, 45);
        gc1.set(Calendar.MILLISECOND, 123);
        final GregorianCalendar gc2 = new GregorianCalendar(2023, Calendar.MARCH, 15, 10, 30, 45);
        gc2.set(Calendar.MILLISECOND, 123);
        assertTrue(Dates.isSameLocalTime(gc1, gc2));

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameLocalTime(null, cal1));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameLocalTime(cal1, null));
    }

    @Test
    public void testIsSameLocalTime_differentCalendarClasses() {
        final Calendar cal1 = Calendar.getInstance();
        cal1.set(2023, Calendar.JANUARY, 1, 10, 0, 0);
        cal1.set(Calendar.MILLISECOND, 0);
        final GregorianCalendar cal2 = new GregorianCalendar();
        cal2.set(2023, Calendar.JANUARY, 1, 10, 0, 0);
        cal2.set(Calendar.MILLISECOND, 0);

        if (!cal1.getClass().equals(cal2.getClass())) {
            assertFalse(Dates.isSameLocalTime(cal1, cal2));
        } else {
            assertTrue(Dates.isSameLocalTime(cal1, cal2));
        }
    }

    // ===== last day / length =====

    @Test
    public void testIsLastDayOfMonth() {
        final Calendar jan31 = of(2025, Calendar.JANUARY, 31, 10, 30, 45);
        final Calendar jan30 = of(2025, Calendar.JANUARY, 30, 10, 30, 45);
        final Calendar feb28 = of(2025, Calendar.FEBRUARY, 28, 10, 30, 45);
        final Calendar leapFeb29 = of(2024, Calendar.FEBRUARY, 29, 10, 30, 45);

        assertTrue(Dates.isLastDayOfMonth(jan31.getTime()));
        assertFalse(Dates.isLastDayOfMonth(jan30.getTime()));
        assertTrue(Dates.isLastDayOfMonth(feb28.getTime()));
        assertTrue(Dates.isLastDayOfMonth(leapFeb29.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDayOfMonth(null));
    }

    @Test
    public void testIsLastDayOfYear() {
        final Calendar dec31 = of(2025, Calendar.DECEMBER, 31, 10, 30, 45);
        final Calendar dec30 = of(2025, Calendar.DECEMBER, 30, 10, 30, 45);
        final Calendar june = of(2025, Calendar.JUNE, 15, 0, 0, 0);

        assertTrue(Dates.isLastDayOfYear(dec31.getTime()));
        assertFalse(Dates.isLastDayOfYear(dec30.getTime()));
        assertFalse(Dates.isLastDayOfYear(june.getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isLastDayOfYear(null));
    }

    @Test
    public void testLengthOfMonth() {
        assertEquals(31, Dates.lengthOfMonth(of(2025, Calendar.JANUARY, 15, 10, 30, 45).getTime()));
        assertEquals(28, Dates.lengthOfMonth(of(2025, Calendar.FEBRUARY, 15, 10, 30, 45).getTime()));
        assertEquals(30, Dates.lengthOfMonth(of(2025, Calendar.APRIL, 15, 0, 0, 0).getTime()));
        assertEquals(29, Dates.lengthOfMonth(of(2024, Calendar.FEBRUARY, 29, 12, 0, 0).getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.lengthOfMonth(null));
    }

    @Test
    public void testLengthOfYear() {
        assertEquals(365, Dates.lengthOfYear(of(2025, Calendar.JUNE, 15, 10, 30, 45).getTime()));
        assertEquals(366, Dates.lengthOfYear(of(2024, Calendar.JUNE, 15, 0, 0, 0).getTime()));
        assertThrows(IllegalArgumentException.class, () -> Dates.lengthOfYear(null));
    }

    // ===== isOverlapping / isBetween =====

    @Test
    public void testIsOverlapping() {
        final java.util.Date s1 = Dates.parseToJUDate("2023-01-01");
        final java.util.Date e1 = Dates.parseToJUDate("2023-01-10");
        final java.util.Date s2 = Dates.parseToJUDate("2023-01-05");
        final java.util.Date e2 = Dates.parseToJUDate("2023-01-15");
        final java.util.Date s3 = Dates.parseToJUDate("2023-01-12");
        final java.util.Date e3 = Dates.parseToJUDate("2023-01-20");
        final java.util.Date monthEnd = Dates.parseToJUDate("2023-01-31");
        final java.util.Date innerStart = Dates.parseToJUDate("2023-01-10");
        final java.util.Date innerEnd = Dates.parseToJUDate("2023-01-20");

        assertTrue(Dates.isOverlapping(s1, e1, s2, e2));
        assertTrue(Dates.isOverlapping(s2, e2, s1, e1));
        assertFalse(Dates.isOverlapping(s1, e1, s3, e3));
        assertTrue(Dates.isOverlapping(s1, e1, s1, e1));
        assertTrue(Dates.isOverlapping(s1, e2, s2, e1));
        assertTrue(Dates.isOverlapping(s1, monthEnd, innerStart, innerEnd));
        assertFalse(Dates.isOverlapping(new java.util.Date(1000L), new java.util.Date(2000L), new java.util.Date(2000L), new java.util.Date(3000L)));
        assertTrue(Dates.isOverlapping(new java.util.Date(1000L), new java.util.Date(5000L), new java.util.Date(2000L), new java.util.Date(3000L)));
    }

    @Test
    public void testIsOverlapping_invalid_throws() {
        final java.util.Date d = Dates.parseToJUDate("2023-01-01");
        final java.util.Date late = Dates.parseToJUDate("2023-01-31");
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(null, d, d, d));
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(d, null, d, d));
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(d, d, null, d));
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(d, d, d, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(late, d, d, late));
    }

    @Test
    public void testIsBetween() {
        final java.util.Date date = Dates.parseToJUDate("2023-01-05");
        final java.util.Date start = Dates.parseToJUDate("2023-01-01");
        final java.util.Date end = Dates.parseToJUDate("2023-01-10");

        assertTrue(Dates.isBetween(date, start, end));
        assertTrue(Dates.isBetween(start, start, end));
        assertTrue(Dates.isBetween(end, start, end));
        assertFalse(Dates.isBetween(Dates.parseToJUDate("2022-12-31"), start, end));
        assertFalse(Dates.isBetween(Dates.parseToJUDate("2023-01-11"), start, end));

        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(null, start, end));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(start, null, end));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(start, start, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(date, end, start));
    }
}
