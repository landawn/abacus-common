package com.landawn.abacus.util;

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
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DatesParseTest extends TestBase {

    @Test
    public void testParseToDate_offsetPreservesInstant() {
        final String text = "2025-01-01T00:30:00+14:00"; // instant: 2024-12-31T10:30:00Z
        final long expected = Instant.parse("2024-12-31T10:30:00Z").toEpochMilli();

        assertEquals(expected, Dates.parseToDate(text, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTime());
        assertEquals(expected, Dates.parseToDate(text).getTime());
        assertEquals(expected, Dates.parseToJUDate(text).getTime());
        assertEquals(LocalDate.of(2025, 1, 1), Dates.parseToLocalDate(text));
    }

    // ===== parseToJUDate =====

    @Test
    public void testParseJUDate() {
        assertNull(Dates.parseToJUDate((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(""));
        assertNull(Dates.parseToJUDate("null"));

        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("1000000000"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("0"));
        assertEquals(1000000000L, Dates.parseEpochMillis("1000000000"));
        assertEquals(0L, Dates.parseEpochMillis("0"));
    }

    @Test
    public void testParseJUDate_variousFormats() {
        // A year-like bare numeric string is ambiguous: rejected, not parsed as a year.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2022"));

        // Month-day text has no year and therefore cannot identify an instant.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("01-15"));

        java.util.Date rfc1123 = Dates.parseToJUDate("Sat, 01 Jan 2022 00:00:00 GMT");
        assertNotNull(rfc1123);

        java.util.Date isoLocal = Dates.parseToJUDate("2022-01-01T00:00:00");
        assertNotNull(isoLocal);

        java.util.Date isoOffset = Dates.parseToJUDate("2022-01-01T00:00:00+05:00");
        assertNotNull(isoOffset);
    }

    @Test
    public void testParseJUDate_withFormat() {
        assertNull(Dates.parseToJUDate(null, Dates.LOCAL_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseToJUDate("null", Dates.LOCAL_DATE_FORMAT));

        java.util.Date date = Dates.parseToJUDate("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_withIsoFormat() {
        java.util.Date date = Dates.parseToJUDate("2025-10-04T14:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_withRfc1123Format() {
        java.util.Date date = Dates.parseToJUDate("Sat, 04 Oct 2025 14:30:45 GMT", Dates.RFC_1123_DATE_TIME_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_withFormatAndTimeZone() {
        assertNull(Dates.parseToJUDate(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseToJUDate("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        java.util.Date date = Dates.parseToJUDate("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_emptyInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(""));
    }

    @Test
    public void testParseJUDate_nullMarker_throws() {
        assertNull(Dates.parseToJUDate("null"));
    }

    @Test
    public void testParseJUDate_monthDayFormat() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("03-15"));
    }

    @Test
    public void testParseJUDate_timeFormat() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("10:30:45"));
        assertEquals(LocalTime.of(10, 30, 45), Dates.parseToLocalTime("10:30:45"));
    }

    @Test
    public void testParseJUDate_localTimestampFormat() {
        java.util.Date date = Dates.parseToJUDate("2023-03-15 10:30:45.123");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_iso8601TimestampFormat() {
        java.util.Date date = Dates.parseToJUDate("2023-03-15T10:30:45.123Z");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_isoLocalDateTimeFormat() {
        java.util.Date date = Dates.parseToJUDate("2023-03-15T10:30:45");
        assertNotNull(date);
    }

    @Test
    public void testParseJUDate_iso8601DateTimeFormat() {
        java.util.Date date = Dates.parseToJUDate("2023-03-15T10:30:45Z");
        assertNotNull(date);
    }

    // ===== parseToJUDate(String, String, TimeZone) =====

    @Test
    public void testParseJUDate_withFormatAndTimeZone_UTC() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        java.util.Date date = Dates.parseToJUDate("2023-03-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, utc);
        assertNotNull(date);
        Calendar cal = Calendar.getInstance(utc);
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void testParseJUDate_withFormatAndTimeZone_localDate() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        java.util.Date date = Dates.parseToJUDate("2023-06-20", Dates.LOCAL_DATE_FORMAT, tz);
        assertNotNull(date);
        Calendar cal = Calendar.getInstance(tz);
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testParseJUDate_nullString_withFormat() {
        assertNull(Dates.parseToJUDate(null, Dates.LOCAL_DATE_FORMAT, null));
    }

    @Test
    public void testParseJUDate_emptyString_withFormat_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("", Dates.LOCAL_DATE_FORMAT, null));
    }

    @Test
    public void testParseJUDate_withFormat_noTimeZone() {
        java.util.Date date = Dates.parseToJUDate("2023-12-25", Dates.LOCAL_DATE_FORMAT, null);
        assertNotNull(date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH));
    }

    // ===== parseToJUDate with format + timezone =====

    @Test
    public void testParseJUDate_withFormatAndTimeZone_localTimestamp() {
        String dateStr = "2023-06-15 12:30:45";
        java.util.Date date = Dates.parseToJUDate(dateStr, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(date);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        assertEquals(2023, cal.get(Calendar.YEAR));
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testParseJUDate_withNullFormat_longString_throws() {
        // Bare numeric text is ambiguous: rejected; epoch millis need the explicit parseEpochMillis API.
        final long millis = 1000000000000L;
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(String.valueOf(millis), null, null));
        assertEquals(millis, Dates.parseEpochMillis(String.valueOf(millis)));
    }

    @Test
    public void testParseJUDate_invalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("invalid date string"));
    }

    // ===== parseToJUDate via ISO_8601_DATE_TIME_FORMAT =====

    @Test
    public void testParseJUDate_iso8601_withNonUtcTz_throwsRuntime() {
        assertThrows(RuntimeException.class,
                () -> Dates.parseToJUDate("2023-01-01T00:00:00Z", Dates.ISO_8601_DATE_TIME_FORMAT, TimeZone.getTimeZone("America/New_York")));
    }

    // ===== parseToDate =====

    @Test
    public void testParseDate() {
        assertNull(Dates.parseToDate((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(""));
        assertNull(Dates.parseToDate("null"));

        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("1000000000"));
        assertEquals(1000000000L, Dates.parseEpochMillis("1000000000"));
    }

    @Test
    public void testParseDate_withFormat() {
        assertNull(Dates.parseToDate(null, Dates.LOCAL_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseToDate("null", Dates.LOCAL_DATE_FORMAT));

        java.sql.Date date = Dates.parseToDate("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(date);
    }

    @Test
    public void testParseDate_nullInput() {
        assertNull(Dates.parseToDate(null));
    }

    @Test
    public void testParseDate_emptyInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(""));
    }

    // ===== parseToTime =====

    @Test
    public void testParseTime() {
        assertNull(Dates.parseToTime((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime(""));
        assertNull(Dates.parseToTime("null"));

        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("1000000000"));
        assertEquals(1000000000L, Dates.parseEpochMillis("1000000000"));
    }

    @Test
    public void testParseTime_withFormat() {
        assertNull(Dates.parseToTime(null, Dates.LOCAL_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("", Dates.LOCAL_TIME_FORMAT));
        assertNull(Dates.parseToTime("null", Dates.LOCAL_TIME_FORMAT));

        Time time = Dates.parseToTime("14:30:45", Dates.LOCAL_TIME_FORMAT);
        assertNotNull(time);
    }

    @Test
    public void testParseTime_nullInput() {
        assertNull(Dates.parseToTime(null));
    }

    @Test
    public void testParseTime_emptyInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime(""));
    }

    // ===== parseToTimestamp =====

    @Test
    public void testParseTimestamp() {
        assertNull(Dates.parseToTimestamp((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(""));
        assertNull(Dates.parseToTimestamp("null"));

        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("1000000000"));
        assertEquals(1000000000L, Dates.parseEpochMillis("1000000000"));
    }

    @Test
    public void testParseTimestamp_withFormat() {
        assertNull(Dates.parseToTimestamp(null, Dates.LOCAL_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT));
        assertNull(Dates.parseToTimestamp("null", Dates.LOCAL_DATE_TIME_FORMAT));

        Timestamp timestamp = Dates.parseToTimestamp("2025-10-04 14:30:45", Dates.LOCAL_DATE_TIME_FORMAT);
        assertNotNull(timestamp);
    }

    @Test
    public void testParseTimestamp_withIsoFormat() {
        Timestamp ts = Dates.parseToTimestamp("2025-10-04T14:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT);
        assertNotNull(ts);
    }

    @Test
    public void testParseTimestamp_withFormatAndTimeZone() {
        assertNull(Dates.parseToTimestamp(null, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseToTimestamp("null", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getDefault()));

        Timestamp timestamp = Dates.parseToTimestamp("2025-10-04 14:30:45", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(timestamp);
    }

    @Test
    public void testParseTimestamp_nullInput() {
        assertNull(Dates.parseToTimestamp(null));
    }

    @Test
    public void testParseTimestamp_emptyInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(""));
    }

    @Test
    public void testParseTimestamp_timestampToStringFormat_roundTrip() {
        // Every string produced by java.sql.Timestamp.toString() must parse back to an equal Timestamp,
        // for all supported fractional-second lengths (1 to 9 digits, with trailing zeros trimmed).
        final long base = 1736937045000L; // arbitrary instant
        final int[] nanos = { 0, 1, 100000000, 120000000, 123000000, 123400000, 123450000, 123456000, 123456700, 123456780, 123456789, 500000000 };

        for (final int n : nanos) {
            final Timestamp orig = new Timestamp(base);
            orig.setNanos(n);
            final String s = orig.toString();

            final Timestamp parsed = Dates.parseToTimestamp(s);
            assertNotNull(parsed, "failed to parse: " + s);
            assertEquals(orig, parsed, "round-trip mismatch for: " + s);
            assertEquals(s, parsed.toString());
        }
    }

    @Test
    public void testParseTimestamp_timestampToStringFormat_matchesValueOf() {
        // Dates.parseToTimestamp must agree with Timestamp.valueOf (the exact inverse of Timestamp.toString)
        // for the JDBC timestamp escape format, regardless of the number of fractional digits.
        final String[] inputs = { "2023-12-25 15:30:00.0", "2025-01-15 10:30:45.5", "2025-01-15 10:30:45.12", "2025-01-15 10:30:45.123",
                "2025-01-15 10:30:45.1234", "2025-01-15 10:30:45.123456", "2025-01-15 10:30:45.123456789", "2025-01-15 10:30:45.000000001" };

        for (final String s : inputs) {
            assertEquals(Timestamp.valueOf(s), Dates.parseToTimestamp(s), "mismatch for: " + s);
        }
    }

    @Test
    public void testParseTimestamp_timestampToStringFormat_fractionSemantics() {
        // The fractional part is a fraction of a second (nanoseconds), not a millisecond count.
        assertEquals(123456789, Dates.parseToTimestamp("2025-01-15 10:30:45.123456789").getNanos());
        assertEquals(500000000, Dates.parseToTimestamp("2025-01-15 10:30:45.5").getNanos());
        assertEquals(120000000, Dates.parseToTimestamp("2025-01-15 10:30:45.12").getNanos());
        assertEquals(1, Dates.parseToTimestamp("2025-01-15 10:30:45.000000001").getNanos());
        // A 3-digit fraction (the previously supported case) keeps meaning milliseconds: .123 == 123 ms == 123000000 ns.
        assertEquals(123000000, Dates.parseToTimestamp("2025-01-15 10:30:45.123").getNanos());
    }

    @Test
    public void testParseTimestamp_timestampToStringFormat_doesNotAffectOtherFormats() {
        // Non-JDBC inputs must keep their existing handling and not be diverted to the Timestamp.valueOf fast path.
        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("1736937045123"));
        assertEquals(1736937045123L, Dates.parseEpochMillis("1736937045123"));
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z").getTime()); // ISO-8601 UTC
        assertNotNull(Dates.parseToTimestamp("2024-07-31T23:42:38-07:00")); // ISO offset (T-separated)
        assertNotNull(Dates.parseToTimestamp("2025-01-15 10:30:45")); // no fractional second (length 19)
        assertNull(Dates.parseToTimestamp((String) null));
        assertNull(Dates.parseToTimestamp("null"));
    }

    // ===== parseToCalendar =====

    @Test
    public void testParseCalendar() {
        assertNull(Dates.parseToCalendar((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(""));
        assertNull(Dates.parseToCalendar("null"));

        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("1000000000"));
        assertEquals(1000000000L, Dates.parseEpochMillis("1000000000"));
    }

    @Test
    public void testParseCalendar_withFormat() {
        assertNull(Dates.parseToCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseToCalendar("null", Dates.LOCAL_DATE_FORMAT));

        Calendar calendar = Dates.parseToCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void testParseCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseToCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseToCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        Calendar calendar = Dates.parseToCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void testParseCalendar_withUTCTimeZone() {
        Calendar calendar = Dates.parseToCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone());
    }

    @Test
    public void testParseCalendar_nullInput() {
        assertNull(Dates.parseToCalendar(null));
    }

    @Test
    public void testParseCalendar_emptyInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(""));
    }

    // ===== parseToGregorianCalendar =====

    @Test
    public void testParseGregorianCalendar() {
        assertNull(Dates.parseToGregorianCalendar((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar(""));
        assertNull(Dates.parseToGregorianCalendar("null"));

        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar("1000000000"));
        assertEquals(1000000000L, Dates.parseEpochMillis("1000000000"));
    }

    @Test
    public void testParseGregorianCalendar_withFormat() {
        assertNull(Dates.parseToGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseToGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT));

        GregorianCalendar calendar = Dates.parseToGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void testParseGregorianCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseToGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseToGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        GregorianCalendar calendar = Dates.parseToGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void testParseGregorianCalendar_nullInput() {
        assertNull(Dates.parseToGregorianCalendar(null));
    }

    @Test
    public void testParseGregorianCalendar_emptyInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar(""));
    }

    // ===== parseToXMLGregorianCalendar =====

    @Test
    public void testParseXMLGregorianCalendar() {
        assertNull(Dates.parseToXMLGregorianCalendar((String) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(""));
        assertNull(Dates.parseToXMLGregorianCalendar("null"));

        // Bare numeric text is rejected as ambiguous; epoch millis go through the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar("1000000000"));
        assertEquals(1000000000L, Dates.parseEpochMillis("1000000000"));
    }

    @Test
    public void testParseXMLGregorianCalendar_withFormat() {
        assertNull(Dates.parseToXMLGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar("", Dates.LOCAL_DATE_FORMAT));
        assertNull(Dates.parseToXMLGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT));

        XMLGregorianCalendar calendar = Dates.parseToXMLGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT);
        assertNotNull(calendar);
    }

    @Test
    public void testParseXMLGregorianCalendar_withFormatAndTimeZone() {
        assertNull(Dates.parseToXMLGregorianCalendar(null, Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar("", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));
        assertNull(Dates.parseToXMLGregorianCalendar("null", Dates.LOCAL_DATE_FORMAT, TimeZone.getDefault()));

        XMLGregorianCalendar calendar = Dates.parseToXMLGregorianCalendar("2025-10-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(calendar);
    }

    @Test
    public void testParseXMLGregorianCalendar_nullInput() {
        assertNull(Dates.parseToXMLGregorianCalendar(null));
    }

    @Test
    public void testParseXMLGregorianCalendar_emptyInput_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(""));
    }

    // ===== isPossibleLong returns false for "-" (L2251) =====

    @Test
    public void testParse_singleMinus_isPossibleLongReturnsFalse() {
        // "-" makes isPossibleLong return false at the fromIndex==length check
        // It then falls to checkDateFormat -> ISO8601Util.parseInstant which throws
        assertThrows(RuntimeException.class, () -> Dates.parseToDate("-", null));
    }

    // ===== Missing tests: null input handling for parse methods =====

    @Test
    public void testParseJUDate_nullInput() {
        assertNull(Dates.parseToJUDate(null));
    }

    // ===== parse (via parseToDate, parseToTimestamp etc. with format+tz) =====

    @Test
    public void testParse_withFormatAndTimeZone_localDateTime() {
        String dateStr = "2022-11-20 08:00:00";
        java.sql.Timestamp ts = Dates.parseToTimestamp(dateStr, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(ts);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(ts);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH));
    }

    @Test
    public void testParse_withNullFormat_longString_throws() {
        // Bare numeric text is rejected as ambiguous; epoch millis need the explicit parseEpochMillis API.
        final long millis = 1234567890000L;
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(String.valueOf(millis)));
        assertTrue(e.getMessage().startsWith("Ambiguous numeric date/time text"), e.getMessage());
        assertEquals(millis, Dates.parseEpochMillis(String.valueOf(millis)));
    }

    // ===== Numeric epoch-millisecond overflow =====

    @Test
    public void testParse_overflowLong_throwsIllegalArgumentException() {
        // Bare numeric text is rejected as ambiguous regardless of range; parseEpochMillis validates
        // the value and rejects an out-of-long-range number with IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("99999999999999999999", null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseEpochMillis("99999999999999999999"));
    }

    // ===== Missing tests: parse with format auto-detection for various lengths =====

    @Test
    public void testParseJUDate_yearLikeNumericString_throws() {
        // "2023" is bare numeric text: ambiguous (a year? epoch millis?), so it is rejected.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2023"));
        assertEquals(2023L, Dates.parseEpochMillis("2023"));
    }

    // ===== Missing tests: format with ISO_OFFSET =====

    @Test
    public void testParseJUDate_isoOffsetFormat() {
        java.util.Date date = Dates.parseToJUDate("2023-03-15T10:30:45+05:30");
        assertNotNull(date);
    }

    // ===== checkDateFormat returning ISO_LOCAL_TIMESTAMP_FORMAT for 23-char T-date (L5513) =====

    @Test
    public void testParseDate_iso8601TimestampFormat2() {
        // A 23-char string "yyyy-MM-ddTHH:mm:ss.SSS" (no trailing Z) triggers L5513
        java.sql.Date result = Dates.parseToDate("2023-01-15T14:30:45.123", null);
        assertNotNull(result);
        assertEquals(LocalDate.of(2023, 1, 15), result.toLocalDate());
    }

    // ===== checkDateFormat returning LOCAL_YEAR_FORMAT for 4-char non-numeric string (L5488) =====

    @Test
    public void testParseDate_fourCharNonNumeric_localYearFormat() {
        // A 4-char non-numeric string reaches checkDateFormat which returns LOCAL_YEAR_FORMAT
        // Parsing "ABCD" as "yyyy" throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("ABCD", null));
    }

    // ===== DTF parseToLocalDate =====

    @Test
    public void testDTF_parseToLocalDate() {
        LocalDate localDate = Dates.DTF.LOCAL_DATE.parseToLocalDate("2025-10-04");
        assertEquals(LocalDate.of(2025, 10, 4), localDate);
    }

    @Test
    public void testDTF_parseToLocalTime() {
        LocalTime localTime = Dates.DTF.LOCAL_TIME.parseToLocalTime("14:30:45");
        assertEquals(LocalTime.of(14, 30, 45), localTime);
    }

    @Test
    public void testDTF_parseToLocalDateTime() {
        LocalDateTime localDateTime = Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime("2025-10-04 14:30:45");
        assertEquals(LocalDateTime.of(2025, 10, 4, 14, 30, 45), localDateTime);
    }

    @Test
    public void testDTF_parseToOffsetDateTime() {
        OffsetDateTime offsetDateTime = Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2025-10-04T14:30:45+00:00");
        assertEquals(2025, offsetDateTime.getYear());
        assertEquals(10, offsetDateTime.getMonthValue());
        assertEquals(4, offsetDateTime.getDayOfMonth());
    }

    @Test
    public void testDTF_parseToZonedDateTime() {
        ZonedDateTime zonedDateTime = new Dates.DTF("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'").parseToZonedDateTime("2025-10-04T14:30:45+00:00[UTC]");
        assertEquals(2025, zonedDateTime.getYear());
        assertEquals(10, zonedDateTime.getMonthValue());
        assertEquals(4, zonedDateTime.getDayOfMonth());
    }

    @Test
    public void testDTF_parseToInstant() {
        Instant instant = Dates.DTF.ISO_OFFSET_DATE_TIME.parseToInstant("2025-10-04T14:30:45+00:00");
        assertTrue(instant.toEpochMilli() > 0);
    }

    @Test
    public void testDTF_parseToJUDate() {
        Dates.DTF dtf = Dates.DTF.LOCAL_DATE;
        assertNotNull(dtf.parseToJUDate("2025-10-04"));
        assertNotNull(dtf.parseToJUDate("2025-10-04", TimeZone.getTimeZone("UTC")));
        assertNotNull(dtf.parseToDate("2025-10-04"));
        assertNotNull(Dates.DTF.LOCAL_TIME.parseToTime("14:30:45"));
        assertNotNull(Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp("2025-10-04 14:30:45"));
        assertNotNull(Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp("2025-10-04 14:30:45", TimeZone.getTimeZone("UTC")));
        assertNotNull(dtf.parseToCalendar("2025-10-04"));
        Calendar utcCal = dtf.parseToCalendar("2023-03-15", TimeZone.getTimeZone("UTC"));
        assertEquals(2023, utcCal.get(Calendar.YEAR));
    }

    @Test
    public void testDTF_parseToDate_sqlDate() {
        java.sql.Date sqlDate = Dates.DTF.LOCAL_DATE.parseToDate("2023-10-26");
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.OCTOBER, 26, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTimeInMillis(), sqlDate.getTime());
    }

    @Test
    public void testDTF_parseToTime_sqlTime() {
        java.sql.Time sqlTime = Dates.DTF.LOCAL_TIME.parseToTime("10:15:30");
        Calendar cal = Calendar.getInstance();
        cal.set(1970, Calendar.JANUARY, 1, 10, 15, 30);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTimeInMillis(), sqlTime.getTime());
    }

    @Test
    public void testDTF_parse_nullAndEmpty() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate(null));
        assertNull(Dates.DTF.LOCAL_TIME.parseToLocalTime(null));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime(null));
        assertNull(Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(null));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(null));
        assertNull(Dates.DTF.ISO_OFFSET_DATE_TIME.parseToInstant(null));
        assertNull(Dates.DTF.LOCAL_DATE.parseToJUDate(null));
        assertNull(Dates.DTF.LOCAL_DATE.parseToJUDate(null, utc));
        assertNull(Dates.DTF.LOCAL_DATE.parseToDate(null));
        assertNull(Dates.DTF.LOCAL_TIME.parseToTime(null));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp(null));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp(null, utc));
        assertNull(Dates.DTF.LOCAL_DATE.parseToCalendar(null));
        assertNull(Dates.DTF.LOCAL_DATE.parseToCalendar(null, utc));

        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToLocalDate(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_TIME.parseToLocalTime(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_OFFSET_DATE_TIME.parseToInstant(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToJUDate(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToJUDate("", utc));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToDate(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_TIME.parseToTime(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp("", utc));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToCalendar(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToCalendar("", utc));
    }

    @Test
    public void test_DTF_parseToX_nullLiteral_throws() {
        // The "null" marker (any case) parses as Java null, matching a null reference, so formatTo of
        // a null value round-trips through parse.
        assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate(new StringBuilder("NuLl")));
        assertNull(Dates.DTF.LOCAL_TIME.parseToLocalTime("NULL"));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime("null"));
        assertNull(Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("null"));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("null"));
        assertNull(Dates.DTF.ISO_OFFSET_DATE_TIME.parseToInstant("null"));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToJUDate("null"));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToJUDate("null", TimeZone.getTimeZone("UTC")));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToDate("null"));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToTime("null"));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToTimestamp("null"));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToTimestamp("null", TimeZone.getTimeZone("UTC")));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToCalendar("null"));
        assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToCalendar("null", TimeZone.getTimeZone("UTC")));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor("null"));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor("NULL"));

        // Keep generic cross-format branches covered as well.
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToLocalDate("null"));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToLocalTime("null"));
        assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDateTime("null"));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToOffsetDateTime("null"));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToZonedDateTime("null"));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToInstant("null"));

        // Null references still return null.
        org.junit.jupiter.api.Assertions.assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate(null));
        org.junit.jupiter.api.Assertions.assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToJUDate(null));
        org.junit.jupiter.api.Assertions.assertNull(Dates.DTF.ISO_ZONED_DATE_TIME.parseToTimestamp(null, TimeZone.getTimeZone("UTC")));
        org.junit.jupiter.api.Assertions.assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor(null));
    }

    @Test
    public void testParseIsoZStringAcceptsAnyZeroOffsetZone() {
        // regression: any zero-offset zone other than the exact UTC instance (GMT, Etc/UTC) was
        // rejected for 'Z' strings with a raw RuntimeException; 'Z' fully determines the instant
        final java.util.Date expected = Dates.parseToJUDate("2025-01-15T10:30:45Z");

        assertEquals(expected, Dates.parseToJUDate("2025-01-15T10:30:45Z", null, TimeZone.getTimeZone("GMT")));
        assertEquals(expected.getTime(), Dates.parseToTimestamp("2025-01-15T10:30:45Z", null, TimeZone.getTimeZone("Etc/UTC")).getTime());

        // the 'Z' the text carries is data and wins over the fallback zone, exactly as a numeric offset
        // does (2026-09-02); only the explicitly supplied fixed-UTC constant treats the zone as a conflict
        assertEquals(expected, Dates.parseToJUDate("2025-01-15T10:30:45Z", null, TimeZone.getTimeZone("America/New_York")));
        assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, TimeZone.getTimeZone("America/New_York")));
    }

    @Test
    public void testParseTimestamp_OffsetSeparatorRoundTrip() {
        final Timestamp timestamp = Dates.parseToTimestamp("2023-03-15T10:20:30.123+02:30");

        assertEquals("2023-03-15T07:50:30.123Z", Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT, TimeZone.getTimeZone("UTC")));
        assertEquals(123000000, timestamp.getNanos());
    }

    // ===== parse*: the "null" literal is case-insensitive and parses as Java null =====

    @Test
    public void testParse_nullLiteral_throws() {
        assertNull(Dates.parseToJUDate("NULL"));
        assertNull(Dates.parseToDate("Null"));
        assertNull(Dates.parseToTime("nUlL"));
        assertNull(Dates.parseToTimestamp("NULL"));
        assertNull(Dates.parseToCalendar("NULL"));
        assertNull(Dates.parseToGregorianCalendar("NULL"));
        assertNull(Dates.parseToXMLGregorianCalendar("NULL"));
        assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate("NULL"));
        assertNull(Dates.DTF.LOCAL_TIME.parseToLocalTime("NULL"));
        assertEquals(0L, Dates.parseEpochMillis("NULL"));
        assertEquals(0L, Dates.parseEpochMillis("null"));
        assertEquals(0L, Dates.parseEpochMillis((String) null));
        assertEquals(Instant.EPOCH, Dates.parseEpochMillisToInstant("null"));
        assertEquals(Instant.EPOCH, Dates.parseEpochMillisToInstant(null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseEpochMillis(""));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseEpochMillisToInstant(""));
    }

    // ===== parse*: bare numeric strings (even negative) are ambiguous and rejected =====

    @Test
    public void testParse_negativeNumericString_throws() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("-1000"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("-1000"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("-1000"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("-1000"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("-1000"));

        // Signed decimal epoch millis remain available through the explicit API.
        assertEquals(-1000L, Dates.parseEpochMillis("-1000"));
        assertEquals(Instant.ofEpochMilli(-1000L), Dates.parseEpochMillisToInstant("-1000"));
    }

    // ===== parseToJUDate: auto-detection of offset and non-3-digit fraction forms =====

    @Test
    public void testParseJUDate_isoOffset_autoDetectMatchesExplicit() {
        final String s = "2025-01-15T10:30:45+05:30";
        assertEquals(Dates.parseToJUDate(s, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTime(), Dates.parseToJUDate(s).getTime());
    }

    @Test
    public void testParseJUDate_shortFraction_fractionSemantics() {
        // fraction lengths other than exactly three digits miss the fast-format detection and fall to the
        // ISO-8601 parser, which still interprets the fraction as a fraction of a second (.5 = 500 ms).
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        assertEquals(1736937045500L, Dates.parseToJUDate("2025-01-15T10:30:45.5", null, utc).getTime());
        assertEquals(1736937045120L, Dates.parseToJUDate("2025-01-15T10:30:45.12", null, utc).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15T10:30:45.1234", null, utc).getTime());
    }

    // ===== parseToDate: an invalid leap day is rejected (strict, non-lenient parsing) =====

    @Test
    public void testParseDate_nonLeapYearFeb29_throws() {
        // 2023 is not a leap year: the strict parser rejects Feb 29 instead of rolling it into Mar 1
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("2023-02-29", Dates.LOCAL_DATE_FORMAT));
        // a real leap day parses as-is
        assertEquals(LocalDate.of(2024, 2, 29), Dates.parseToDate("2024-02-29", Dates.LOCAL_DATE_FORMAT).toLocalDate());
    }

    // ===== DTF: invalid input rejected with IllegalArgumentException (uniform parse-failure contract) =====

    @Test
    public void testDtf_parseToLocalDateTime_invalidInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToLocalDate("not-a-date"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_TIME.parseToLocalTime("25:00:00"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25 14:30:45"));
    }

    @Test
    public void testParseJUDateAutoDetects29CharacterIso8601OffsetTimestamp() {
        final String input = "2023-12-25T14:30:45.123+05:00";

        assertEquals(OffsetDateTime.parse(input).toInstant().toEpochMilli(), Dates.parseToJUDate(input).getTime());
    }

    /**
     * C-007: {@code LOCAL_DATE_FORMAT} carries no offset, so a date whose local midnight occurs twice names two
     * instants and is rejected - exactly as a date whose midnight does not exist is. The constant's javadoc used
     * to claim the overlap case was accepted and resolved to the first midnight; the code has never done that.
     * Historical dates are used here so the assertion does not depend on future tzdb rule changes.
     */
    @Test
    public void test20260906_dateOnlyTextAtADstOverlapIsRejected() {
        final TimeZone havana = TimeZone.getTimeZone("America/Havana");
        final TimeZone cuiaba = TimeZone.getTimeZone("America/Cuiaba");
        final TimeZone saoPaulo = TimeZone.getTimeZone("America/Sao_Paulo");

        // fall-back overlaps: local midnight occurs twice
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2018-11-04", Dates.LOCAL_DATE_FORMAT, havana));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("1950-04-16", Dates.LOCAL_DATE_FORMAT, cuiaba));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("2018-11-04", Dates.LOCAL_DATE_FORMAT, havana));

        // spring-forward gap: local midnight does not exist - the case the javadoc always described correctly
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2018-11-04", Dates.LOCAL_DATE_FORMAT, saoPaulo));

        // an ordinary day in the same zone still parses
        assertNotNull(Dates.parseToJUDate("2018-11-05", Dates.LOCAL_DATE_FORMAT, havana));
    }

    /**
     * C2-D1: the width-rejection message suggested a variable-width pattern with a quoted Z. In a custom
     * pattern that is a plain literal with no zone semantics, so the suggested pattern resolved the text in
     * the JVM default zone and silently shifted the instant by that zone's offset - eight hours in
     * America/Los_Angeles - while still parsing without complaint. XXX reads the Z as the zero offset, so the
     * suggestion is now the drop-in replacement it claims to be. (The pre-existing tests pass UTC explicitly,
     * where both spellings agree, which is why this survived.)
     */
    @Test
    public void test20260906c3_widthRejectionSuggestsAPatternThatKeepsTheUtcDesignator() {
        final java.util.TimeZone utc = java.util.TimeZone.getTimeZone("UTC");
        final java.util.TimeZone la = java.util.TimeZone.getTimeZone("America/Los_Angeles");

        // Both UTC-designator constants must now suggest an offset pattern. The bad texts are the ones the
        // existing pin uses, because those are the shapes that reach the width check rather than the
        // fraction-width check that guards the timestamp constant.
        for (final String[] c : new String[][] { { Dates.ISO_8601_DATE_TIME_FORMAT, "2025-1-15T10:30:45Z" },
                { Dates.ISO_8601_TIMESTAMP_FORMAT, "2025-001-5T10:30:45.123Z" } }) {

            final String message = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(c[1], c[0], utc)).getMessage();
            final String suggestion = message.replaceAll("^.*such as \"", "").replaceAll("\".*$", "");
            assertTrue(suggestion.endsWith("XXX"), () -> "expected an offset pattern, got: " + message);
        }

        // The point of the fix: the text spells UTC, so following the suggestion must land on the same instant
        // the constant does even when the parse runs in another zone. A quoted 'Z' is a plain literal in a
        // custom pattern, so the old suggestion silently shifted the result by that zone's offset.
        final String canonical = "2025-01-15T10:30:45Z";
        final String shortForm = "2025-1-15T10:30:45Z";
        final String suggestion = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(shortForm, Dates.ISO_8601_DATE_TIME_FORMAT, utc))
                .getMessage()
                .replaceAll("^.*such as \"", "")
                .replaceAll("\".*$", "");

        assertEquals(Dates.parseToJUDate(canonical, Dates.ISO_8601_DATE_TIME_FORMAT, utc).getTime(), Dates.parseToJUDate(shortForm, suggestion, la).getTime(),
                () -> "suggested pattern " + suggestion + " must not shift the instant");
    }

    /**
     * C2-D2: an auto-detected parse failure named the DETECTED grammar, telling callers they had passed a
     * pattern they never wrote. parseToTimestamp already reported it as "the auto-detected format"; the
     * legacy front doors now agree.
     */
    @Test
    public void test20260906c3_autoDetectedFailuresNameTheAutoDetectedFormat() {
        final java.util.TimeZone ny = java.util.TimeZone.getTimeZone("America/New_York");

        // The first two texts reach the JDBC-escape and ISO-local-timestamp branches; the third reaches the
        // fractional-ISO-offset branch, which was a further site with the same shape - it handed the message
        // builder a grammar DESCRIPTION, which then rendered in the "format '...'" slot that means "the format
        // you gave me". Asserting the invariant only on the first two is what left it there.
        //
        // Not covered here: text that fails inside the DateTimeFormatter layer (e.g. a bad region id). That
        // layer reports "with pattern 'ISO_ZONED_DATE_TIME' at index N" - a named constant plus an offset, not
        // a pattern presented as the caller's - and is a different message contract from these front doors.
        for (final String text : new String[] { "2025-03-09 02:30:00.5", "2025-11-02T01:30:00.5", "2025-13-15T10:30:45.123+05:00" }) {
            for (final org.junit.jupiter.api.function.Executable call : new org.junit.jupiter.api.function.Executable[] {
                    () -> Dates.parseToJUDate(text, null, ny), () -> Dates.parseToCalendar(text, null, ny), () -> Dates.parseToTimestamp(text, null, ny) }) {

                final String message = assertThrows(IllegalArgumentException.class, call).getMessage();
                assertTrue(message.contains("the auto-detected format"), () -> "should say auto-detected: " + message);
                assertFalse(message.contains("with format "), () -> "should not name a pattern: " + message);
            }
        }

        // ... and an explicitly supplied format is still named, which is what makes the message useful.
        final String named = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2025-13-15T10:30:45.123+05:00", Dates.ISO_ZONED_DATE_TIME_FORMAT, ny)).getMessage();
        assertTrue(named.contains("with format "), () -> "an explicit format must still be named: " + named);
    }
}
