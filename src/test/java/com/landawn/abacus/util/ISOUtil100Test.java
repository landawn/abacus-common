package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ISOUtil100Test extends TestBase {

    private Date testDate;
    private TimeZone utcTimeZone;
    private TimeZone estTimeZone;
    private TimeZone pstTimeZone;

    @BeforeEach
    public void setUp() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2024, Calendar.JANUARY, 15, 14, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        testDate = cal.getTime();

        utcTimeZone = TimeZone.getTimeZone("UTC");
        estTimeZone = TimeZone.getTimeZone("EST");
        pstTimeZone = TimeZone.getTimeZone("PST");
    }

    @Test
    @DisplayName("Test format(Date) with default timezone")
    public void testFormatDateOnly() {
        String result = ISO8601Util.format(testDate);
        assertNotNull(result);
        assertTrue(result.contains("2024-01-15"));
        assertTrue(result.endsWith("Z"));
        assertFalse(result.contains("."));
    }

    @Test
    @DisplayName("Test format(Date, boolean) without milliseconds")
    public void testFormatDateWithoutMillis() {
        String result = ISO8601Util.format(testDate, false);
        assertNotNull(result);
        assertTrue(result.contains("2024-01-15"));
        assertTrue(result.endsWith("Z"));
        assertFalse(result.contains("."));
    }

    @Test
    @DisplayName("Test format(Date, boolean) with milliseconds")
    public void testFormatDateWithMillis() {
        String result = ISO8601Util.format(testDate, true);
        assertNotNull(result);
        assertTrue(result.contains("2024-01-15"));
        assertTrue(result.contains(".123"));
        assertTrue(result.endsWith("Z"));
    }

    @Test
    @DisplayName("Test format(Date, boolean, TimeZone) with UTC timezone")
    public void testFormatDateWithMillisAndUTCTimezone() {
        String result = ISO8601Util.format(testDate, true, utcTimeZone);
        assertNotNull(result);
        assertTrue(result.contains("2024-01-15"));
        assertTrue(result.contains(".123"));
        assertTrue(result.endsWith("Z"));
    }

    @Test
    @DisplayName("Test format(Date, boolean, TimeZone) with EST timezone")
    public void testFormatDateWithMillisAndESTTimezone() {
        String result = ISO8601Util.format(testDate, true, estTimeZone);
        assertNotNull(result);
        assertTrue(result.contains("-05:00"));
        assertFalse(result.endsWith("Z"));
    }

    @Test
    @DisplayName("Test format(Date, boolean, TimeZone, Locale) with UTC and US locale")
    public void testFormatDateFullParametersUTC() {
        String result = ISO8601Util.format(testDate, true, utcTimeZone, Locale.US);
        assertNotNull(result);
        assertTrue(result.contains("2024-01-15"));
        assertTrue(result.contains(".123"));
        assertTrue(result.endsWith("Z"));
    }

    @Test
    @DisplayName("Test format(Date, boolean, TimeZone, Locale) with different timezone and locale")
    public void testFormatDateFullParametersWithOffset() {
        String result = ISO8601Util.format(testDate, false, pstTimeZone, Locale.CANADA);
        assertNotNull(result);
        assertTrue(result.contains("-08:00"));
        assertFalse(result.contains("."));
        assertFalse(result.endsWith("Z"));
    }

    @Test
    @DisplayName("Test format with null date should throw exception")
    public void testFormatNullDate() {
        assertThrows(NullPointerException.class, () -> {
            ISO8601Util.format(null);
        });
    }

    @Test
    @DisplayName("Test parse(String) with basic date")
    public void testParseBasicDate() {
        Date result = ISO8601Util.parse("2024-01-15");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    @DisplayName("Test parse(String) with date and time in UTC")
    public void testParseDateTimeUTC() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45Z");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(45, cal.get(Calendar.SECOND));
    }

    @Test
    @DisplayName("Test parse(String) with milliseconds")
    public void testParseDateTimeWithMillis() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45.123Z");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result);
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Test parse(String) with positive timezone offset")
    public void testParseDateTimeWithPositiveOffset() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45+05:30");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test parse(String) with negative timezone offset")
    public void testParseDateTimeWithNegativeOffset() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45-08:00");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test parse(String) without separators")
    public void testParseDateTimeWithoutSeparators() {
        Date result = ISO8601Util.parse("20240115T143045Z");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result);
        assertEquals(2024, cal.get(Calendar.YEAR));
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    @DisplayName("Test parse(String, ParsePosition) with valid date")
    public void testParseWithParsePosition() {
        ParsePosition pos = new ParsePosition(0);
        Date result = ISO8601Util.parse("2024-01-15T14:30:45Z", pos);
        assertNotNull(result);
        assertEquals(20, pos.getIndex());
    }

    @Test
    @DisplayName("Test parse(String, ParsePosition) with partial parsing")
    public void testParseWithParsePositionPartial() {
        String dateString = "2024-01-15T14:30:45Z extra text";
        ParsePosition pos = new ParsePosition(0);
        Date result = ISO8601Util.parse(dateString, pos);
        assertNotNull(result);
        assertEquals(20, pos.getIndex());
    }

    @Test
    @DisplayName("Test parse with leap seconds (should truncate to 59)")
    public void testParseLeapSeconds() {
        Date result = ISO8601Util.parse("2024-01-15T23:59:60Z");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result);
        assertEquals(59, cal.get(Calendar.SECOND));
    }

    @Test
    @DisplayName("Test parse with partial milliseconds (1 digit)")
    public void testParsePartialMillisOneDigit() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45.1Z");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result);
        assertEquals(100, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Test parse with partial milliseconds (2 digits)")
    public void testParsePartialMillisTwoDigits() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45.12Z");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result);
        assertEquals(120, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Test parse with +00:00 timezone (should be treated as UTC)")
    public void testParseZeroOffsetWithColon() {
        Date result1 = ISO8601Util.parse("2024-01-15T14:30:45+00:00");
        Date result2 = ISO8601Util.parse("2024-01-15T14:30:45Z");
        assertEquals(result1.getTime(), result2.getTime());
    }

    @Test
    @DisplayName("Test parse with +0000 timezone (should be treated as UTC)")
    public void testParseZeroOffsetWithoutColon() {
        Date result1 = ISO8601Util.parse("2024-01-15T14:30:45+0000");
        Date result2 = ISO8601Util.parse("2024-01-15T14:30:45Z");
        assertEquals(result1.getTime(), result2.getTime());
    }

    @Test
    @DisplayName("Test parse with invalid date format should throw exception")
    public void testParseInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            ISO8601Util.parse("invalid-date");
        });
    }

    @Test
    @DisplayName("Test parse with null string should throw exception")
    public void testParseNullString() {
        assertThrows(IllegalArgumentException.class, () -> {
            ISO8601Util.parse(null);
        });
    }

    @Test
    @DisplayName("Test parse with missing timezone indicator should throw exception")
    public void testParseMissingTimezone() {
        assertThrows(IllegalArgumentException.class, () -> {
            ISO8601Util.parse("2024-01-15T14:30:45");
        });
    }

    @Test
    @DisplayName("Test parse with invalid timezone indicator should throw exception")
    public void testParseInvalidTimezone() {
        assertThrows(IllegalArgumentException.class, () -> {
            ISO8601Util.parse("2024-01-15T14:30:45X");
        });
    }

    @Test
    @DisplayName("Test parse with invalid month should throw exception")
    public void testParseInvalidMonth() {
        assertThrows(IllegalArgumentException.class, () -> {
            ISO8601Util.parse("2024-13-15T14:30:45Z");
        });
    }

    @Test
    @DisplayName("Test parse with invalid day should throw exception")
    public void testParseInvalidDay() {
        assertThrows(IllegalArgumentException.class, () -> {
            ISO8601Util.parse("2024-02-30T14:30:45Z");
        });
    }

    @Test
    @DisplayName("Test parse timezone offset without colon")
    public void testParseTimezoneOffsetNoColon() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45+0530");
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test format and parse round trip")
    public void testFormatParseRoundTrip() {
        String formatted = ISO8601Util.format(testDate, true, utcTimeZone, Locale.US);
        Date parsed = ISO8601Util.parse(formatted);
        assertEquals(testDate.getTime(), parsed.getTime());
    }

    @Test
    @DisplayName("Test parse with very long fractional seconds")
    public void testParseVeryLongFractionalSeconds() {
        Date result = ISO8601Util.parse("2024-01-15T14:30:45.123456789Z");
        assertNotNull(result);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(result);
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    @DisplayName("Test parse date only without time component")
    public void testParseDateOnlyNoTime() {
        ParsePosition pos = new ParsePosition(0);
        Date result = ISO8601Util.parse("2024-01-15", pos);
        assertNotNull(result);
        assertEquals(10, pos.getIndex());

        Calendar cal = Calendar.getInstance();
        cal.setTime(result);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }
}
