package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ISO8601UtilTest extends TestBase {

    // ===== Constants =====

    @Test
    public void testDEF_8601_LEN() {
        assertEquals("yyyy-MM-ddThh:mm:ss.SSS+00:00".length(), ISO8601Util.DEF_8601_LEN);
    }

    @Test
    public void testTIMEZONE_Z() {
        assertNotNull(ISO8601Util.TIMEZONE_Z);
        assertEquals("UTC", ISO8601Util.TIMEZONE_Z.getID());
    }

    // ===== format(Date) =====

    @Test
    public void testFormat_Date() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.DECEMBER, 25, 10, 30, 45);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date);
        assertEquals("2023-12-25T10:30:45Z", formatted);
    }

    @Test
    public void testFormat_Date_Epoch() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        Date epoch = cal.getTime();

        String formatted = ISO8601Util.format(epoch);
        assertEquals("1970-01-01T00:00:00Z", formatted);
    }

    @Test
    public void testFormat_Date_NoMillis() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.JUNE, 15, 14, 0, 0);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date);
        // Should not contain milliseconds
        assertTrue(formatted.endsWith("Z"));
        assertEquals("2023-06-15T14:00:00Z", formatted);
    }

    // ===== format(Date, boolean) =====

    @Test
    public void testFormat_DateWithMillis_True() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.DECEMBER, 25, 10, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date, true);
        assertEquals("2023-12-25T10:30:45.123Z", formatted);
    }

    @Test
    public void testFormat_DateWithMillis_False() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.DECEMBER, 25, 10, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date, false);
        assertEquals("2023-12-25T10:30:45Z", formatted);
    }

    @Test
    public void testFormat_DateWithMillis_ZeroMillis() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.MARCH, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date, true);
        assertEquals("2023-03-01T12:00:00.000Z", formatted);
    }

    // ===== format(Date, boolean, TimeZone) =====

    @Test
    public void testFormat_DateWithTimezone_UTC() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.JULY, 4, 18, 30, 0);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date, false, ISO8601Util.TIMEZONE_Z);
        assertEquals("2023-07-04T18:30:00Z", formatted);
    }

    @Test
    public void testFormat_DateWithTimezone_NonUTC() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.JULY, 4, 18, 30, 0);
        Date date = cal.getTime();

        TimeZone est = TimeZone.getTimeZone("GMT+05:30");
        String formatted = ISO8601Util.format(date, false, est);
        assertEquals("2023-07-05T00:00:00+05:30", formatted);
    }

    // ===== format(Date, boolean, TimeZone, Locale) =====

    @Test
    public void testFormat_DateFullControl() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.DECEMBER, 25, 10, 30, 45);
        cal.set(Calendar.MILLISECOND, 500);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date, true, ISO8601Util.TIMEZONE_Z, Locale.US);
        assertEquals("2023-12-25T10:30:45.500Z", formatted);
    }

    @Test
    public void testFormat_DateFullControl_NegativeOffset() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.DECEMBER, 25, 15, 0, 0);
        Date date = cal.getTime();

        TimeZone est = TimeZone.getTimeZone("GMT-05:00");
        String formatted = ISO8601Util.format(date, false, est, Locale.US);
        assertEquals("2023-12-25T10:00:00-05:00", formatted);
    }

    @Test
    public void testFormat_DateFullControl_PositiveOffset() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        Date date = cal.getTime();

        TimeZone jst = TimeZone.getTimeZone("GMT+09:00");
        String formatted = ISO8601Util.format(date, false, jst, Locale.US);
        assertEquals("2023-01-01T09:00:00+09:00", formatted);
    }

    // ===== parse(String) =====

    @Test
    public void testParse_DateOnly() {
        Date date = ISO8601Util.parse("2023-12-25");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testParse_DateOnlyCompact() {
        Date date = ISO8601Util.parse("20231225");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testParse_DateTimeWithZ() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(45, cal.get(Calendar.SECOND));
    }

    @Test
    public void testParse_DateTimeWithMillis() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45.123Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(45, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testParse_DateTimeWithPositiveOffset() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45+05:30");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        // 10:30 at +05:30 is 05:00 UTC
        assertEquals(5, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testParse_DateTimeWithNegativeOffset() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45-05:00");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        // 10:30 at -05:00 is 15:30 UTC
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testParse_DateTimeWithZeroOffset() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45+00:00");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testParse_DateTimeWithZeroOffsetCompact() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45+0000");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
    }

    @Test
    public void testParse_CompactDateTimeWithZ() {
        Date date = ISO8601Util.parse("20231225T103045Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(45, cal.get(Calendar.SECOND));
    }

    @Test
    public void testParse_DateTimeWithoutSeconds() {
        Date date = ISO8601Util.parse("2023-12-25T10:30Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }

    @Test
    public void testParse_DateTimeWith1DigitMillis() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45.1Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(100, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testParse_DateTimeWith2DigitMillis() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45.12Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(120, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testParse_DateTimeWithExtraMillisDigits() {
        // More than 3 digits of millis - should only use first 3
        Date date = ISO8601Util.parse("2023-12-25T10:30:45.123456Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testParse_InvalidString_Null() {
        assertThrows(Exception.class, () -> ISO8601Util.parse(null));
    }

    @Test
    public void testParse_InvalidString_Empty() {
        assertThrows(Exception.class, () -> ISO8601Util.parse(""));
    }

    @Test
    public void testParse_InvalidString_Garbage() {
        assertThrows(Exception.class, () -> ISO8601Util.parse("not-a-date"));
    }

    @Test
    public void testParse_InvalidTimezoneIndicator() {
        assertThrows(Exception.class, () -> ISO8601Util.parse("2023-12-25T10:30:45X"));
    }

    // ===== parse(String, ParsePosition) =====

    @Test
    public void testParse_WithParsePosition() {
        ParsePosition pos = new ParsePosition(0);
        Date date = ISO8601Util.parse("2023-12-25T10:30:45Z", pos);
        assertNotNull(date);
        assertEquals(20, pos.getIndex());
    }

    @Test
    public void testParse_WithParsePosition_DateOnly() {
        ParsePosition pos = new ParsePosition(0);
        Date date = ISO8601Util.parse("2023-12-25", pos);
        assertNotNull(date);
        assertEquals(10, pos.getIndex());
    }

    @Test
    public void testParse_WithParsePosition_CompactDate() {
        ParsePosition pos = new ParsePosition(0);
        Date date = ISO8601Util.parse("20231225", pos);
        assertNotNull(date);
        assertEquals(8, pos.getIndex());
    }

    // ===== Round-trip tests =====

    @Test
    public void testFormatParse_RoundTrip() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.JUNE, 15, 14, 30, 0);
        Date original = cal.getTime();

        String formatted = ISO8601Util.format(original);
        Date parsed = ISO8601Util.parse(formatted);

        assertEquals(original.getTime(), parsed.getTime());
    }

    @Test
    public void testFormatParse_RoundTripWithMillis() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.JUNE, 15, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 678);
        Date original = cal.getTime();

        String formatted = ISO8601Util.format(original, true);
        Date parsed = ISO8601Util.parse(formatted);

        assertEquals(original.getTime(), parsed.getTime());
    }

    @Test
    public void testFormatParse_RoundTrip_Midnight() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        Date original = cal.getTime();

        String formatted = ISO8601Util.format(original);
        Date parsed = ISO8601Util.parse(formatted);

        assertEquals(original.getTime(), parsed.getTime());
    }

    @Test
    public void testFormatParse_RoundTrip_EndOfDay() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2023, Calendar.DECEMBER, 31, 23, 59, 59);
        Date original = cal.getTime();

        String formatted = ISO8601Util.format(original);
        Date parsed = ISO8601Util.parse(formatted);

        assertEquals(original.getTime(), parsed.getTime());
    }

    // ===== Edge cases =====

    @Test
    public void testParse_LeapYear() {
        Date date = ISO8601Util.parse("2024-02-29T12:00:00Z");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH));
        assertEquals(29, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testFormat_Year2000() {
        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.clear();
        cal.set(2000, Calendar.JANUARY, 1, 0, 0, 0);
        Date date = cal.getTime();

        String formatted = ISO8601Util.format(date);
        assertEquals("2000-01-01T00:00:00Z", formatted);
    }

    @Test
    public void testParse_DateTimeWithMillisAndOffset() {
        Date date = ISO8601Util.parse("2023-12-25T10:30:45.123+05:30");
        assertNotNull(date);

        Calendar cal = new GregorianCalendar(ISO8601Util.TIMEZONE_Z);
        cal.setTime(date);
        // 10:30:45.123 at +05:30 -> 05:00:45.123 UTC
        assertEquals(5, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(45, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }
}
