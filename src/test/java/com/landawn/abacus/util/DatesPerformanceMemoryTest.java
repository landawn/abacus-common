package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Dates. */
@Tag("slow-test")
public class DatesPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G017 end ----
    // ---- perf review 2026-10-08 G021 begin ----
    @Nested
    class G021 {
        private final java.util.TimeZone newYork = java.util.TimeZone.getTimeZone("America/New_York");

        // A SimpleTimeZone with daylight-saving rules no ZoneId can express: the Calendar fallback of resolveLocalMillis.
        private final java.util.TimeZone customDst = new java.util.SimpleTimeZone(-5 * 3600_000, "custom-dst", 2, 8, -1, 7200000, 10, 1, -1, 7200000, 3600000);

        private void assertTimestamp(final java.time.Instant expected, final java.sql.Timestamp actual) {
            assertEquals(expected.toEpochMilli(), actual.getTime());
            assertEquals(expected.getNano(), actual.getNanos());
        }

        private void assertParseFailure(final String expectedMessage, final Class<? extends Throwable> expectedCause,
                final org.junit.jupiter.api.function.Executable executable) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
            assertEquals(expectedMessage, e.getMessage());
            assertNotNull(e.getCause());
            assertEquals(expectedCause, e.getCause().getClass());
        }

        // G021-01: auto-detected ISO-8601 'Z' timestamps (1-9 fraction digits) are read directly - same instant as before
        @Test
        void testParseToTimestamp_autoUtcFractionValues() {
            final String[] texts = { "2025-01-15T10:30:45.123Z", "2025-01-15T10:30:45.5Z", "2025-01-15T10:30:45.12Z", "2025-01-15T10:30:45.1234Z",
                    "2025-01-15T10:30:45.123456789Z", "2025-01-15T10:30:45.000000001Z", "2025-01-15T10:30:45.0Z", "2024-02-29T23:59:59.999999999Z",
                    "2000-02-29T00:00:00.1Z", "0001-01-01T00:00:00.000Z", "9999-12-31T23:59:59.999999999Z", "1582-10-10T12:00:00.5Z",
                    "1969-12-31T23:59:59.999Z" };

            for (final String text : texts) {
                final java.time.Instant expected = java.time.LocalDateTime.parse(text.substring(0, text.length() - 1)).toInstant(java.time.ZoneOffset.UTC);

                assertTimestamp(expected, Dates.parseToTimestamp(text));
                // the designator the text carries wins over a supplied zone, with or without an empty format
                assertTimestamp(expected, Dates.parseToTimestamp(text, null, newYork));
                assertTimestamp(expected, Dates.parseToTimestamp(text, "", customDst));
            }

            assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z").getTime());
            assertEquals(500000000, Dates.parseToTimestamp("2025-01-15T10:30:45.5Z").getNanos());
            assertEquals(123456789, Dates.parseToTimestamp("2025-01-15T10:30:45.123456789Z").getNanos());
        }

        // G021-01: out-of-range fields still get the java.time grammar's diagnostic
        @Test
        void testParseToTimestamp_autoUtcFractionInvalidFields() {
            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;

            assertParseFailure("Cannot parse \"2025-02-29T00:00:00.0Z\" with the auto-detected format: Text '2025-02-29T00:00:00.0Z' could not be parsed: "
                    + "Invalid date 'February 29' as '2025' is not a leap year", dtpe, () -> Dates.parseToTimestamp("2025-02-29T00:00:00.0Z"));
            assertParseFailure("Cannot parse \"2025-02-30T10:30:45.123Z\" with the auto-detected format: Text '2025-02-30T10:30:45.123Z' could not be parsed: "
                    + "Invalid date 'FEBRUARY 30'", dtpe, () -> Dates.parseToTimestamp("2025-02-30T10:30:45.123Z"));
            assertParseFailure("Cannot parse \"2025-13-01T00:00:00.1Z\" with the auto-detected format: Text '2025-13-01T00:00:00.1Z' could not be parsed: "
                    + "Invalid value for MonthOfYear (valid values 1 - 12): 13", dtpe, () -> Dates.parseToTimestamp("2025-13-01T00:00:00.1Z"));
            assertParseFailure("Cannot parse \"2025-01-15T24:00:00.0Z\" with the auto-detected format: Text '2025-01-15T24:00:00.0Z' could not be parsed: "
                    + "Invalid value for HourOfDay (valid values 0 - 23): 24", dtpe, () -> Dates.parseToTimestamp("2025-01-15T24:00:00.0Z"));
            assertParseFailure("Cannot parse \"2025-01-15T23:60:00.0Z\" with the auto-detected format: Text '2025-01-15T23:60:00.0Z' could not be parsed: "
                    + "Invalid value for MinuteOfHour (valid values 0 - 59): 60", dtpe, () -> Dates.parseToTimestamp("2025-01-15T23:60:00.0Z"));
            assertParseFailure("Cannot parse \"2025-01-15T23:59:60.0Z\" with the auto-detected format: Text '2025-01-15T23:59:60.0Z' could not be parsed: "
                    + "Invalid value for SecondOfMinute (valid values 0 - 59): 60", dtpe, () -> Dates.parseToTimestamp("2025-01-15T23:59:60.0Z"));

            final IllegalArgumentException yearZero = assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("0000-01-01T00:00:00.0Z"));
            assertEquals("Format 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'' requires a Common Era year from 0001 through 9999 written as exactly four digits: "
                    + "\"0000-01-01T00:00:00.0Z\"", yearZero.getMessage());
        }

        // G021-01: auto-detected ISO local timestamps (1-9 fraction digits) are read directly; zone resolution unchanged
        @Test
        void testParseToTimestamp_autoLocalFraction() {
            final String[] texts = { "2025-01-15T10:30:45.123", "2025-01-15T10:30:45.5", "2025-01-15T10:30:45.123456", "2025-07-15T10:30:45.123456789",
                    "2024-02-29T23:59:59.999999999", "0001-01-01T00:00:00.1", "9999-12-31T23:59:59.9" };

            for (final String text : texts) {
                final java.time.LocalDateTime local = java.time.LocalDateTime.parse(text);

                assertTimestamp(local.atZone(java.time.ZoneId.of("America/New_York")).toInstant(), Dates.parseToTimestamp(text, null, newYork));
                assertTimestamp(local.toInstant(java.time.ZoneOffset.UTC), Dates.parseToTimestamp(text, "", java.util.TimeZone.getTimeZone("UTC")));
            }

            // the Calendar fallback for rules no ZoneId can express: winter offset -05:00, fraction re-added
            assertTimestamp(java.time.Instant.parse("2025-01-15T15:30:45.123456Z"), Dates.parseToTimestamp("2025-01-15T10:30:45.123456", null, customDst));
            assertTimestamp(java.time.Instant.parse("2025-11-02T06:30:00.500Z"), Dates.parseToTimestamp("2025-11-02T01:30:00.5", null, customDst));

            final Class<? extends Throwable> dte = java.time.DateTimeException.class;
            assertParseFailure(
                    "Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)",
                    dte, () -> Dates.parseToTimestamp("2025-03-09T02:30:00.5", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-11-02T01:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Ambiguous local date-time 2025-11-02T01:30 in zone America/New_York (DST overlap); valid offsets are [-04:00, -05:00]",
                    dte, () -> Dates.parseToTimestamp("2025-11-02T01:30:00.5", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-04-31T00:00:00.1\" with the auto-detected format in time zone America/New_York: "
                            + "Text '2025-04-31T00:00:00.1' could not be parsed: Invalid date 'APRIL 31'",
                    java.time.format.DateTimeParseException.class, () -> Dates.parseToTimestamp("2025-04-31T00:00:00.1", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T24:00:00.1\" with the auto-detected format in time zone custom-dst: "
                            + "Text '2025-01-15T24:00:00.1' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24",
                    java.time.format.DateTimeParseException.class, () -> Dates.parseToTimestamp("2025-01-15T24:00:00.1", null, customDst));

            final IllegalArgumentException gap = assertThrows(IllegalArgumentException.class,
                    () -> Dates.parseToTimestamp("2025-03-09T02:30:00.5", null, customDst));
            assertTrue(gap.getMessage().startsWith("Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone custom-dst: "),
                    gap.getMessage());
            assertEquals(IllegalArgumentException.class, gap.getCause().getClass());
        }

        // G021-01: auto-detected fractional ISO offset timestamps (+HH:mm, compact +HHmm) are read directly
        @Test
        void testParseToTimestamp_autoOffsetFraction() {
            final String[][] cases = { { "2025-01-15T10:30:45.123+05:30", "2025-01-15T05:00:45.123Z" },
                    { "2025-01-15T10:30:45.123-0500", "2025-01-15T15:30:45.123Z" }, { "2025-01-15T10:30:45.5+0545", "2025-01-15T04:45:45.500Z" },
                    { "2025-01-15T10:30:45.123456789-09:30", "2025-01-15T20:00:45.123456789Z" },
                    { "2025-01-15T10:30:45.123+18:00", "2025-01-14T16:30:45.123Z" }, { "2025-01-15T10:30:45.123-18:00", "2025-01-16T04:30:45.123Z" },
                    { "2025-01-15T10:30:45.123-00:00", "2025-01-15T10:30:45.123Z" }, { "2025-01-15T10:30:45.123+0000", "2025-01-15T10:30:45.123Z" },
                    { "2024-02-29T00:00:00.1+01:00", "2024-02-28T23:00:00.100Z" },
                    // a seconds-precision offset keeps the java.time route
                    { "1880-01-15T10:30:45.123-04:56:02", "1880-01-15T15:26:47.123Z" } };

            for (final String[] c : cases) {
                final java.time.Instant expected = java.time.Instant.parse(c[1]);

                assertTimestamp(expected, Dates.parseToTimestamp(c[0]));
                assertTimestamp(expected, Dates.parseToTimestamp(c[0], null, newYork));
            }

            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;
            assertParseFailure(
                    "Cannot parse \"2025-01-15T10:30:45.123+18:01\" with the auto-detected format: Text '2025-01-15T10:30:45.123+18:01' "
                            + "could not be parsed: Zone offset not in valid range: -18:00 to +18:00",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+18:01"));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T10:30:45.123+19:00\" with the auto-detected format: Text '2025-01-15T10:30:45.123+19:00' "
                            + "could not be parsed: Zone offset not in valid range: -18:00 to +18:00",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+19:00"));
            assertParseFailure("Cannot parse \"2025-01-15T10:30:45.123+05:60\" with the auto-detected format: Text '2025-01-15T10:30:45.123+05:60' "
                    + "could not be parsed, unparsed text found at index 26", dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+05:60"));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T10:30:45.123+2400\" with the auto-detected format: Text '2025-01-15T10:30:45.123+24:00' "
                            + "could not be parsed: Value out of range: Hour[0-23], Minute[0-59], Second[0-59]",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+2400"));
            assertParseFailure("Cannot parse \"2025-02-30T10:30:45.123+05:30\" with the auto-detected format: Text '2025-02-30T10:30:45.123+05:30' "
                    + "could not be parsed: Invalid date 'FEBRUARY 30'", dtpe, () -> Dates.parseToTimestamp("2025-02-30T10:30:45.123+05:30"));
            assertParseFailure(
                    "Cannot parse \"2025-01-15T24:00:00.1+05:30\" with the auto-detected format: Text '2025-01-15T24:00:00.1+05:30' "
                            + "could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24",
                    dtpe, () -> Dates.parseToTimestamp("2025-01-15T24:00:00.1+05:30"));
            assertParseFailure("Cannot parse \"2025-0a-15T10:30:45.123+05:30\" with the auto-detected format: Text '2025-0a-15T10:30:45.123+05:30' "
                    + "could not be parsed at index 5", dtpe, () -> Dates.parseToTimestamp("2025-0a-15T10:30:45.123+05:30"));
        }

        // G021-01: parseToXMLGregorianCalendar shares parseAutoTimestampToInstant - same XML values and fractions
        @Test
        void testParseToXMLGregorianCalendar_autoFraction() {
            assertEquals("2025-01-15T10:30:45.123Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123Z").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.123456789Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789Z").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.500+05:30", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.5+05:30").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.123456789-05:00", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789-0500").toXMLFormat());
            assertEquals("2025-01-15T10:30:45.120-05:00", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.120", null, newYork).toXMLFormat());

            final IllegalArgumentException wide = assertThrows(IllegalArgumentException.class,
                    () -> Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.1+18:00"));
            assertEquals("XMLGregorianCalendar timezone must be a whole-minute offset in the range -14:00 through +14:00; zone 'GMT+18:00' has offset "
                    + "64800000 milliseconds at epoch millisecond 1736872245100", wide.getMessage());
        }

        // G021-01: the directly read ISO local timestamp resolves its wall clock exactly as the JDBC and LOCAL_DATE_TIME routes do
        @Test
        void testParseToTimestamp_autoLocalFractionMatchesOtherRoutes() {
            final String[] zones = { "America/New_York", "Asia/Kolkata", "Australia/Lord_Howe", "America/Sao_Paulo", "UTC" };
            final String[] wallClocks = { "2025-01-15 10:30:45", "2025-03-09 01:59:59", "2025-03-09 03:00:00", "2025-11-02 00:59:59", "2025-11-02 02:00:00",
                    "2025-10-05 02:30:00", "2025-04-06 02:00:00", "1883-11-18 11:59:59", "1899-12-31 23:59:59", "0001-01-01 00:00:00", "9999-12-31 23:59:59" };

            for (final String zoneId : zones) {
                final java.util.TimeZone zone = java.util.TimeZone.getTimeZone(zoneId);
                final java.time.zone.ZoneRules rules = java.time.ZoneId.of(zoneId).getRules();

                for (final String wallClock : wallClocks) {
                    final java.time.LocalDateTime local = java.time.LocalDateTime.parse(wallClock.replace(' ', 'T'));
                    final java.util.List<java.time.ZoneOffset> offsets = rules.getValidOffsets(local);

                    if (offsets.size() != 1) {
                        continue;
                    }

                    final long expected = local.toInstant(offsets.get(0)).toEpochMilli();
                    final String isoLocal = wallClock.replace(' ', 'T');

                    // the auto-detected ISO local timestamp shape, read directly
                    assertEquals(expected + 500, Dates.parseToTimestamp(isoLocal + ".5", null, zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(expected + 123, Dates.parseToTimestamp(isoLocal + ".123456789", "", zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(123456789, Dates.parseToTimestamp(isoLocal + ".123456789", "", zone).getNanos());
                    // the JDBC escape text and the LOCAL_DATE_TIME constant name the same instant
                    assertEquals(expected + 123, Dates.parseToTimestamp(wallClock + ".123456789", null, zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(expected, Dates.parseToTimestamp(wallClock, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime(), wallClock + " " + zoneId);
                    assertEquals(expected, Dates.parseToJUDate(wallClock, null, zone).getTime(), wallClock + " " + zoneId);
                }
            }
        }

        // G021-02: the explicitly supplied 'Z' constants read canonical text directly - same instant on every legacy target
        @Test
        void testParseExplicitUtcConstants_values() {
            final String dt = Dates.ISO_8601_DATE_TIME_FORMAT;
            final String ts = Dates.ISO_8601_TIMESTAMP_FORMAT;
            final String[] dateTimes = { "2025-01-15T10:30:45", "2024-02-29T23:59:59", "2000-02-29T00:00:00", "0001-01-01T00:00:00", "9999-12-31T23:59:59",
                    "1582-10-10T12:00:00", "1969-12-31T23:59:59", "0099-06-30T06:07:08" };
            final java.util.TimeZone[] utcEquivalent = { null, java.util.TimeZone.getTimeZone("UTC"), java.util.TimeZone.getTimeZone("GMT"),
                    java.util.TimeZone.getTimeZone("Etc/UTC"), new java.util.SimpleTimeZone(0, "custom-zero") };

            for (final String local : dateTimes) {
                final long seconds = java.time.LocalDateTime.parse(local).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();

                for (final java.util.TimeZone zone : utcEquivalent) {
                    assertEquals(seconds, Dates.parseToJUDate(local + "Z", dt, zone).getTime(), local);
                    assertEquals(seconds + 123, Dates.parseToTimestamp(local + ".123Z", ts, zone).getTime(), local);
                    assertEquals(123000000, Dates.parseToTimestamp(local + ".123Z", ts, zone).getNanos(), local);
                    assertEquals(seconds + 999, Dates.parseToDate(local + ".999Z", ts, zone).getTime(), local);
                    assertEquals(seconds, Dates.parseToTime(local + "Z", dt, zone).getTime(), local);
                }

                final java.util.Calendar calendar = Dates.parseToCalendar(local + ".005Z", ts);
                assertEquals(seconds + 5, calendar.getTimeInMillis(), local);
                assertEquals("UTC", calendar.getTimeZone().getID());
            }

            assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", ts).getTime());
            assertEquals(-12219681599500L, Dates.parseToTimestamp("1582-10-10T12:00:00.500Z", ts).getTime());
            assertEquals("2025-01-15T10:30:45.123Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123Z", ts).toXMLFormat());
            assertEquals("2025-01-15T10:30:45.000Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45Z", dt).toXMLFormat());
        }

        // G021-02: out-of-range fields still reach the pooled SimpleDateFormat and keep its diagnostic; checks before it are unchanged
        @Test
        void testParseExplicitUtcConstants_diagnostics() {
            final String dt = Dates.ISO_8601_DATE_TIME_FORMAT;
            final String ts = Dates.ISO_8601_TIMESTAMP_FORMAT;
            final String[] badDateTimes = { "2025-02-29T00:00:00Z", "2025-02-30T10:30:45Z", "2025-13-15T10:30:45Z", "2025-00-15T10:30:45Z",
                    "2025-01-00T10:30:45Z", "2025-01-15T24:00:00Z", "2025-01-15T23:60:00Z", "2025-01-15T23:59:60Z" };

            for (final String text : badDateTimes) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, dt));
                assertEquals("Cannot parse \"" + text + "\" with format 'yyyy-MM-dd'T'HH:mm:ss'Z'': Unparseable date: \"" + text + "\"", e.getMessage());
                assertEquals(java.text.ParseException.class, e.getCause().getClass());

                final String withMillis = text.substring(0, 19) + ".000Z";
                final IllegalArgumentException m = assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(withMillis, ts));
                assertEquals("Cannot parse \"" + withMillis + "\" with format 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'': Unparseable date: \"" + withMillis + "\"",
                        m.getMessage());
                assertEquals(java.text.ParseException.class, m.getCause().getClass());
            }

            assertEquals(
                    "Format 'yyyy-MM-dd'T'HH:mm:ss'Z'' requires its exact canonical shape, every field written as digits at its full width: "
                            + "\"2025-01-15T10:30:45z\"; use a variable-width pattern such as \"yyyy-M-d'T'H:m:sXXX\" to accept shorter fields",
                    assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45z", dt)).getMessage());
            assertEquals("Format 'yyyy-MM-dd'T'HH:mm:ss.SSS'Z'' requires its exact canonical shape: 24 characters, every field written as digits at its"
                    + " full width, exactly three fractional-second digits and a trailing 'Z': \"2025-01-15T10:30:45.12Z\"; use a variable-width pattern"
                    + " such as \"yyyy-M-d'T'H:m:s.SSSXXX\" to accept shorter fields, or auto-detection (no format), which accepts the JDBC escape grammar"
                    + " with 1-9 fraction digits",
                    assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.12Z", ts)).getMessage());
            assertEquals(
                    "The UTC designator 'Z' requires a UTC-equivalent time zone; input: 2025-01-15T10:30:45.123Z, format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z',"
                            + " time zone: America/New_York (raw offset -05:00)",
                    assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", ts, newYork)).getMessage());
        }
    }

    // ---- perf review 2026-10-08 G021 end ----
    // ---- perf review 2026-10-08 G022 begin ----
    @Nested
    class G022 {
        /** 0001-01-01T00:00:00Z, the first instant the fixed-'Z' constants can render. */
        private static final long YEAR_1_START = -62135596800000L;

        /** 9999-12-31T23:59:59.999Z, the last instant the fixed-'Z' constants can render. */
        private static final long YEAR_9999_END = 253402300799999L;

        private String utcReference(final long millis, final boolean withMillis) {
            return java.time.format.DateTimeFormatter.ofPattern(withMillis ? "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'" : "uuuu-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.ROOT)
                    .withZone(java.time.ZoneOffset.UTC)
                    .format(java.time.Instant.ofEpochMilli(millis));
        }

        // G022-01: the two fixed-'Z' constants render exactly the java.time UTC text and the default format's text
        @Test
        public void testFormat_iso8601ConstantsMatchUtcReferenceAndDefaultFormat() {
            final List<Long> instants = new ArrayList<>(
                    Arrays.asList(YEAR_1_START, YEAR_1_START + 1, YEAR_1_START + 999, YEAR_9999_END, YEAR_9999_END - 999, 0L, -1L, 1L, 999L, 1000L, -999L,
                            -1000L, -1001L, -12219292800001L, -12219292800000L, -2208988800001L, 4102358400000L, 1736937045123L, 951782400000L, 946684799999L));
            final Random random = new Random(20261008L);

            for (int i = 0; i < 3000; i++) {
                instants.add(YEAR_1_START + (long) (random.nextDouble() * (YEAR_9999_END - YEAR_1_START)));
            }

            final java.util.TimeZone utc = java.util.TimeZone.getTimeZone("UTC");

            for (final long millis : instants) {
                final java.util.Date date = new java.util.Date(millis);
                final java.sql.Timestamp timestamp = new java.sql.Timestamp(millis);
                timestamp.setNanos(timestamp.getNanos() + random.nextInt(1_000_000)); // a sub-millisecond fraction the 'Z' constants do not print

                assertEquals(utcReference(millis, false), Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT), "millis=" + millis);
                assertEquals(utcReference(millis, true), Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT), "millis=" + millis);
                assertEquals(utcReference(millis, false), Dates.format(timestamp, Dates.ISO_8601_DATE_TIME_FORMAT, utc), "millis=" + millis);
                assertEquals(utcReference(millis, true), Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT, null), "millis=" + millis);

                // the explicit constants and the default format agree for every value they can both render
                assertEquals(Dates.format(date), Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, null), "millis=" + millis);
                assertEquals(Dates.format(timestamp), Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT, utc), "millis=" + millis);

                final StringBuilder sb = new StringBuilder("prefix:");
                Dates.formatTo(date, Dates.ISO_8601_TIMESTAMP_FORMAT, sb);
                Dates.formatTo(timestamp, Dates.ISO_8601_DATE_TIME_FORMAT, null, sb);
                assertEquals("prefix:" + utcReference(millis, true) + utcReference(millis, false), sb.toString(), "millis=" + millis);
            }
        }

        // G022-01: out-of-range years are still rejected by the predefined format's own year check, before any output
        @Test
        public void testFormat_iso8601ConstantsYearRangeMessageAndNoOutput() {
            for (final String format : new String[] { Dates.ISO_8601_DATE_TIME_FORMAT, Dates.ISO_8601_TIMESTAMP_FORMAT }) {
                final IllegalArgumentException before = assertThrows(IllegalArgumentException.class,
                        () -> Dates.format(new java.util.Date(YEAR_1_START - 1), format));
                assertEquals("Format '" + format + "' supports Common Era years from 0001 through 9999; got instant 0000-12-31T23:59:59.999Z",
                        before.getMessage());

                final IllegalArgumentException after = assertThrows(IllegalArgumentException.class,
                        () -> Dates.format(new java.sql.Timestamp(YEAR_9999_END + 1), format, java.util.TimeZone.getTimeZone("GMT")));
                assertEquals("Format '" + format + "' supports Common Era years from 0001 through 9999; got instant +10000-01-01T00:00:00Z",
                        after.getMessage());

                assertThrows(IllegalArgumentException.class, () -> Dates.format(new java.util.Date(Long.MIN_VALUE), format));
                assertThrows(IllegalArgumentException.class, () -> Dates.format(new java.util.Date(Long.MAX_VALUE), format));

                final StringBuilder sb = new StringBuilder("unchanged");
                assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(new java.util.Date(YEAR_9999_END + 1), format, null, sb));
                assertEquals("unchanged", sb.toString());
            }
        }

        // G022-01: the Appendable still receives the text as one append(String) call; a Writer is not handed a char[] directly
        @Test
        public void testFormatTo_iso8601ConstantsAppendableAndWriterCalls() throws Exception {
            final List<String> calls = new ArrayList<>();
            final Appendable recorder = new Appendable() {
                @Override
                public Appendable append(final CharSequence csq) {
                    calls.add("append:" + csq.getClass().getName() + ":" + csq);
                    return this;
                }

                @Override
                public Appendable append(final CharSequence csq, final int start, final int end) {
                    calls.add("appendRange");
                    return this;
                }

                @Override
                public Appendable append(final char c) {
                    calls.add("appendChar");
                    return this;
                }
            };

            Dates.formatTo(new java.util.Date(1736937045123L), Dates.ISO_8601_DATE_TIME_FORMAT, null, recorder);
            Dates.formatTo(new java.sql.Timestamp(1736937045123L), Dates.ISO_8601_TIMESTAMP_FORMAT, java.util.TimeZone.getTimeZone("GMT"),
                    java.util.Locale.FRENCH, recorder);
            assertEquals(Arrays.asList("append:java.lang.String:2025-01-15T10:30:45Z", "append:java.lang.String:2025-01-15T10:30:45.123Z"), calls);

            final List<String> writerCalls = new ArrayList<>();
            final java.io.Writer writer = new java.io.Writer() {
                @Override
                public void write(final char[] cbuf, final int off, final int len) {
                    writerCalls.add("writeChars:" + new String(cbuf, off, len));
                }

                @Override
                public void write(final String str) throws java.io.IOException {
                    writerCalls.add("writeString:" + str);
                    super.write(str);
                }

                @Override
                public java.io.Writer append(final CharSequence csq) throws java.io.IOException {
                    writerCalls.add("append:" + csq);
                    return super.append(csq);
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };

            Dates.formatTo(new java.util.Date(0L), Dates.ISO_8601_DATE_TIME_FORMAT, writer);
            Dates.formatTo(new java.util.Date(-1L), Dates.ISO_8601_TIMESTAMP_FORMAT, null, writer);
            assertEquals(Arrays.asList("append:1970-01-01T00:00:00Z", "writeString:1970-01-01T00:00:00Z", "writeChars:1970-01-01T00:00:00Z",
                    "append:1969-12-31T23:59:59.999Z", "writeString:1969-12-31T23:59:59.999Z", "writeChars:1969-12-31T23:59:59.999Z"), writerCalls);

            final java.io.IOException failure = new java.io.IOException("append failed");
            final Appendable failing = new Appendable() {
                @Override
                public Appendable append(final CharSequence csq) throws java.io.IOException {
                    throw failure;
                }

                @Override
                public Appendable append(final CharSequence csq, final int start, final int end) throws java.io.IOException {
                    throw failure;
                }

                @Override
                public Appendable append(final char c) throws java.io.IOException {
                    throw failure;
                }
            };

            for (final String format : new String[] { Dates.ISO_8601_DATE_TIME_FORMAT, Dates.ISO_8601_TIMESTAMP_FORMAT }) {
                final RuntimeException e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> Dates.formatTo(new java.util.Date(0L), format, null, failing));
                assertSame(failure, e.getCause());
            }
        }

        // G022-01: zone resolution, conflict check, locale independence and every Date/Calendar entry point stay unchanged
        @Test
        public void testFormat_iso8601ConstantsZonesLocalesAndEntryPoints() throws Exception {
            final long millis = 1736937045123L;
            final java.util.Date date = new java.util.Date(millis);

            for (final java.util.TimeZone zone : new java.util.TimeZone[] { null, java.util.TimeZone.getTimeZone("UTC"), java.util.TimeZone.getTimeZone("GMT"),
                    java.util.TimeZone.getTimeZone("Etc/UTC"), java.util.TimeZone.getTimeZone("Etc/GMT"), new java.util.SimpleTimeZone(0, "Fixed0") }) {
                assertEquals("2025-01-15T10:30:45Z", Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, zone));
                assertEquals("2025-01-15T10:30:45.123Z", Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT, zone));
            }

            for (final java.util.Locale locale : new java.util.Locale[] { java.util.Locale.US, java.util.Locale.FRENCH,
                    java.util.Locale.forLanguageTag("ar-SA"), java.util.Locale.forLanguageTag("th-TH-u-nu-thai"),
                    java.util.Locale.forLanguageTag("hi-IN-u-nu-deva") }) {
                assertEquals("2025-01-15T10:30:45.123Z", Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT, null, locale), locale.toLanguageTag());
                assertEquals("2025-01-15T10:30:45Z", Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, null, locale), locale.toLanguageTag());
            }

            // an equal pattern that is not the constant instance
            assertEquals("2025-01-15T10:30:45Z", Dates.format(date, new String(Dates.ISO_8601_DATE_TIME_FORMAT.toCharArray())));
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(date, new String(Dates.ISO_8601_TIMESTAMP_FORMAT.toCharArray())));

            // a conflicting zone is still rejected first, and nothing reaches the Appendable
            final StringBuilder sb = new StringBuilder();
            final IllegalArgumentException conflict = assertThrows(IllegalArgumentException.class,
                    () -> Dates.formatTo(date, Dates.ISO_8601_TIMESTAMP_FORMAT, java.util.TimeZone.getTimeZone("Asia/Kolkata"), sb));
            assertEquals(
                    "The UTC designator 'Z' requires a UTC-equivalent time zone; format: yyyy-MM-dd'T'HH:mm:ss.SSS'Z', time zone: Asia/Kolkata (raw offset +05:30)",
                    conflict.getMessage());
            assertEquals("", sb.toString());
            assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, null, null));
            assertNull(Dates.format((java.util.Date) null, Dates.ISO_8601_DATE_TIME_FORMAT));

            final java.sql.Timestamp timestamp = new java.sql.Timestamp(millis);
            timestamp.setNanos(123_456_789);
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT));
            assertEquals("2025-01-15T10:30:45Z", Dates.format(timestamp, Dates.ISO_8601_DATE_TIME_FORMAT));
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(new java.sql.Date(millis), Dates.ISO_8601_TIMESTAMP_FORMAT));
            assertEquals("2025-01-15T10:30:45Z", Dates.format(new java.sql.Time(millis), Dates.ISO_8601_DATE_TIME_FORMAT));

            final java.util.GregorianCalendar calendar = new java.util.GregorianCalendar(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTimeInMillis(millis);
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(calendar, Dates.ISO_8601_TIMESTAMP_FORMAT));
            final StringBuilder calendarText = new StringBuilder();
            Dates.formatTo(calendar, Dates.ISO_8601_DATE_TIME_FORMAT, calendarText);
            assertEquals("2025-01-15T10:30:45Z", calendarText.toString());

            final javax.xml.datatype.XMLGregorianCalendar xml = javax.xml.datatype.DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(xml, Dates.ISO_8601_TIMESTAMP_FORMAT));
            assertEquals("2025-01-15T10:30:45Z", Dates.format(xml, Dates.ISO_8601_DATE_TIME_FORMAT, java.util.TimeZone.getTimeZone("UTC")));
        }
    }

    // ---- perf review 2026-10-08 G022 end ----
    // ---- perf review 2026-10-08 G023 begin ----
    @Nested
    class G023 {

        private static final long T0 = 1736937045123L; // 2025-01-15T10:30:45.123Z
        private static final long DAY = 86_400_000L;

        /** A Date subclass without a (long) constructor: Dates must build its results by clone(). */
        public static final class NoArgOnlyDate extends java.util.Date {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger constructed = new AtomicInteger();
            String marker;

            public NoArgOnlyDate() {
                super(0L);
                constructed.incrementAndGet();
            }
        }

        /** A Date subclass with a public (long) constructor: Dates must construct every result with it. */
        public static final class LongCtorDate extends java.util.Date {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger constructed = new AtomicInteger();
            String marker;

            public LongCtorDate(final long millis) {
                super(millis);
                constructed.incrementAndGet();
            }
        }

        /** A calendar with only a no-arg constructor: the second strategy of constructOrCloneCalendar. */
        public static final class NoArgOnlyCalendar extends java.util.GregorianCalendar {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger constructed = new AtomicInteger();
            String marker;

            public NoArgOnlyCalendar() {
                super();
                constructed.incrementAndGet();
            }
        }

        /** A calendar with a (long) and a no-arg constructor: the (long) one wins. */
        public static final class LongCtorCalendar extends java.util.GregorianCalendar {
            private static final long serialVersionUID = 1L;
            static final AtomicInteger longConstructed = new AtomicInteger();
            static final AtomicInteger noArgConstructed = new AtomicInteger();
            String marker;

            public LongCtorCalendar() {
                super();
                noArgConstructed.incrementAndGet();
            }

            public LongCtorCalendar(final long millis) {
                super();
                setTimeInMillis(millis);
                longConstructed.incrementAndGet();
            }
        }

        /** A calendar with neither constructor shape: only clone() can build its results. */
        public static final class ZoneCtorOnlyCalendar extends java.util.GregorianCalendar {
            private static final long serialVersionUID = 1L;
            String marker;

            public ZoneCtorOnlyCalendar(final java.util.TimeZone zone) {
                super(zone);
            }
        }

        /** No usable constructor and a clone() that returns the source: must fail the same way on every call. */
        public static final class SelfCloningNoCtorDate extends java.util.Date {
            private static final long serialVersionUID = 1L;

            public SelfCloningNoCtorDate(final String unusedMarker) {
                super(T0);
            }

            @Override
            public Object clone() {
                return this;
            }
        }

        private java.util.TimeZone utc() {
            return java.util.TimeZone.getTimeZone("UTC");
        }

        // G023-01: a Date subclass without a (long) constructor is cloned on every call (the cached
        // "no such constructor" answer must not change the strategy, the type, the time or the source).
        @Test
        public void testCreateDate_noLongConstructorClonesOnEveryCall() {
            final NoArgOnlyDate source = new NoArgOnlyDate();
            source.setTime(T0);
            source.marker = "source-state";
            final int constructedBefore = NoArgOnlyDate.constructed.get();

            for (int i = 1; i <= 3; i++) {
                final NoArgOnlyDate result = Dates.addDays(source, i);
                assertEquals(NoArgOnlyDate.class, result.getClass());
                assertNotSame(source, result);
                assertEquals(T0 + i * DAY, result.getTime());
                assertEquals("source-state", result.marker, "built by clone(), which carries the instance state");
                assertEquals(T0, source.getTime());
            }

            final NoArgOnlyDate rounded = Dates.truncate(source, CalendarField.SECOND);
            assertEquals(T0 - 123L, rounded.getTime());
            assertEquals("source-state", rounded.marker);
            assertEquals(constructedBefore, NoArgOnlyDate.constructed.get(), "no constructor is ever invoked");
        }

        // G023-01: a declared (long) constructor is still found and invoked on every call.
        @Test
        public void testCreateDate_longConstructorInvokedOnEveryCall() {
            final LongCtorDate source = new LongCtorDate(T0);
            source.marker = "source-state";
            final int constructedBefore = LongCtorDate.constructed.get();

            for (int i = 1; i <= 3; i++) {
                final LongCtorDate result = Dates.addHours(source, i);
                assertEquals(LongCtorDate.class, result.getClass());
                assertEquals(T0 + i * 3_600_000L, result.getTime());
                assertNull(result.marker, "built by the (long) constructor, not by clone()");
            }

            assertEquals(constructedBefore + 3, LongCtorDate.constructed.get());
        }

        // G023-01: no usable constructor and a hostile clone() fails with the same exception every time.
        @Test
        public void testCreateDate_hostileCloneFailsIdenticallyOnEveryCall() {
            final SelfCloningNoCtorDate source = new SelfCloningNoCtorDate("x");
            String firstMessage = null;

            for (int i = 0; i < 3; i++) {
                final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Dates.addDays(source, 1));

                if (firstMessage == null) {
                    firstMessage = e.getMessage();
                } else {
                    assertEquals(firstMessage, e.getMessage());
                }
            }

            assertTrue(firstMessage.contains("must return a distinct Date"), firstMessage);
            assertEquals(T0, source.getTime());
        }

        // G023-01: a calendar with only a no-arg constructor gets it on every call, plus the source settings.
        @Test
        public void testCreateCalendar_noArgConstructorInvokedOnEveryCall() {
            final java.util.TimeZone tokyo = java.util.TimeZone.getTimeZone("Asia/Tokyo");
            final NoArgOnlyCalendar source = new NoArgOnlyCalendar();
            source.setTimeZone(tokyo);
            source.setLenient(false);
            source.setFirstDayOfWeek(java.util.Calendar.WEDNESDAY);
            source.setMinimalDaysInFirstWeek(4);
            source.setTimeInMillis(T0);
            source.marker = "source-state";
            final int constructedBefore = NoArgOnlyCalendar.constructed.get();

            for (int i = 1; i <= 3; i++) {
                final NoArgOnlyCalendar result = Dates.addDays(source, i);
                assertEquals(NoArgOnlyCalendar.class, result.getClass());
                assertNotSame(source, result);
                assertEquals(T0 + i * DAY, result.getTimeInMillis());
                assertNull(result.marker, "built by the no-arg constructor, not by clone()");
                assertEquals("Asia/Tokyo", result.getTimeZone().getID());
                assertFalse(result.isLenient());
                assertEquals(java.util.Calendar.WEDNESDAY, result.getFirstDayOfWeek());
                assertEquals(4, result.getMinimalDaysInFirstWeek());
            }

            assertEquals(constructedBefore + 3, NoArgOnlyCalendar.constructed.get());
            assertEquals(T0, source.getTimeInMillis());
        }

        // G023-01: a declared (long) constructor keeps precedence over the no-arg one on every call.
        @Test
        public void testCreateCalendar_longConstructorPreferredOnEveryCall() {
            final LongCtorCalendar source = new LongCtorCalendar(T0);
            source.setTimeZone(utc());
            source.marker = "source-state";
            final int longBefore = LongCtorCalendar.longConstructed.get();
            final int noArgBefore = LongCtorCalendar.noArgConstructed.get();

            for (int i = 1; i <= 3; i++) {
                final LongCtorCalendar result = Dates.addMinutes(source, i);
                assertEquals(LongCtorCalendar.class, result.getClass());
                assertEquals(T0 + i * 60_000L, result.getTimeInMillis());
                assertNull(result.marker);
            }

            assertEquals(longBefore + 3, LongCtorCalendar.longConstructed.get());
            assertEquals(noArgBefore, LongCtorCalendar.noArgConstructed.get());
        }

        // G023-01: a calendar with neither constructor shape is cloned on every call.
        @Test
        public void testCreateCalendar_noUsableConstructorClonesOnEveryCall() {
            final ZoneCtorOnlyCalendar source = new ZoneCtorOnlyCalendar(utc());
            source.setTimeInMillis(T0);
            source.marker = "source-state";

            for (int i = 1; i <= 3; i++) {
                final ZoneCtorOnlyCalendar result = Dates.addDays(source, i);
                assertEquals(ZoneCtorOnlyCalendar.class, result.getClass());
                assertNotSame(source, result);
                assertEquals(T0 + i * DAY, result.getTimeInMillis());
                assertEquals("source-state", result.marker, "built by clone(), which carries the instance state");
            }

            final ZoneCtorOnlyCalendar truncated = Dates.truncate(source, java.util.Calendar.DATE);
            assertEquals(T0 - (T0 % DAY), truncated.getTimeInMillis());
            assertEquals(T0, source.getTimeInMillis());
        }

        // G023-01: the JDK's own alternate calendars (no (long) constructor; Japanese has no no-arg one, the
        // Buddhist one is not exported) keep their runtime class, settings and results on repeated calls.
        @Test
        public void testCreateCalendar_jdkAlternateCalendarsOnRepeatedCalls() {
            for (final String tag : new String[] { "ja-JP-u-ca-japanese", "th-TH" }) {
                final java.util.Calendar source = java.util.Calendar.getInstance(utc(), java.util.Locale.forLanguageTag(tag));
                source.setFirstDayOfWeek(java.util.Calendar.FRIDAY);
                source.setTimeInMillis(T0);
                final Class<?> type = source.getClass();
                assertNotSame(java.util.GregorianCalendar.class, type, tag);

                for (int i = 1; i <= 3; i++) {
                    final java.util.Calendar result = Dates.addDays(source, i);
                    assertEquals(type, result.getClass(), tag);
                    assertNotSame(source, result, tag);
                    assertEquals(T0 + i * DAY, result.getTimeInMillis(), tag);
                    assertEquals("UTC", result.getTimeZone().getID(), tag);
                    assertEquals(java.util.Calendar.FRIDAY, result.getFirstDayOfWeek(), tag);
                    assertEquals(source.get(java.util.Calendar.ERA), result.get(java.util.Calendar.ERA), tag);
                }

                assertEquals(T0 - (T0 % DAY), Dates.truncate(source, java.util.Calendar.DATE).getTimeInMillis(), tag);
                assertEquals(T0, source.getTimeInMillis(), tag);
            }
        }

        private String failureOf(final org.junit.jupiter.api.function.Executable call) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertNotNull(e.getCause());
            return e.getMessage() + " | " + e.getCause().getClass().getName() + ": " + e.getCause().getMessage();
        }

        /** Every DTF parser, instant-producing or not, with an optional fallback zone. */
        private List<org.junit.jupiter.api.function.Executable> allParsers(final Dates.DTF dtf, final String text, final java.util.TimeZone tz) {
            final List<org.junit.jupiter.api.function.Executable> calls = new ArrayList<>();
            calls.add(() -> dtf.parseToOffsetDateTime(text, tz));
            calls.add(() -> dtf.parseToZonedDateTime(text, tz));
            calls.add(() -> dtf.parseToInstant(text, tz));
            calls.add(() -> dtf.parseToJUDate(text, tz));
            calls.add(() -> dtf.parseToDate(text, tz));
            calls.add(() -> dtf.parseToTime(text, tz));
            calls.add(() -> dtf.parseToTimestamp(text, tz));
            calls.add(() -> dtf.parseToCalendar(text, tz));

            if (tz == null) {
                calls.add(() -> dtf.parseToOffsetDateTime(text));
                calls.add(() -> dtf.parseToZonedDateTime(text));
                calls.add(() -> dtf.parseToInstant(text));
                calls.add(() -> dtf.parseToJUDate(text));
                calls.add(() -> dtf.parseToDate(text));
                calls.add(() -> dtf.parseToTime(text));
                calls.add(() -> dtf.parseToTimestamp(text));
                calls.add(() -> dtf.parseToCalendar(text));
                calls.add(() -> dtf.parseToLocalDate(text));
                calls.add(() -> dtf.parseToLocalTime(text));
                calls.add(() -> dtf.parseToLocalDateTime(text));
                calls.add(() -> dtf.parseToTemporalAccessor(text));
            }

            return calls;
        }

        private java.util.TimeZone[] fallbackZones() {
            // the last one has rules no ZoneId can express: a zone written in the text must not consult it
            return new java.util.TimeZone[] { null, utc(), java.util.TimeZone.getTimeZone("Asia/Tokyo"),
                    new java.util.SimpleTimeZone(3_600_000, "CustomDst", java.util.Calendar.MARCH, 8, -java.util.Calendar.SUNDAY, 7_200_000,
                            java.util.Calendar.NOVEMBER, 1, -java.util.Calendar.SUNDAY, 7_200_000, 3_600_000) };
        }

        // G023-02: an offset the written region rejects fails every parser with the same message and cause,
        // whether parseRaw or the zoned resolution of an instant-producing parser reports it.
        @Test
        public void testDtfParse_datedOffsetInvalidForRegionSameFailureEverywhere() {
            final String text = "2025-07-15T10:30:45+01:00[Europe/Paris]";
            final String expected = "Cannot parse \"" + text + "\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'': "
                    + "Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45 in zone Europe/Paris; valid offsets are [+02:00]"
                    + " | java.time.DateTimeException: Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45 in zone Europe/Paris;"
                    + " valid offsets are [+02:00]";

            for (final java.util.TimeZone tz : fallbackZones()) {
                for (final org.junit.jupiter.api.function.Executable call : allParsers(Dates.DTF.ISO_ZONED_DATE_TIME, text, tz)) {
                    assertEquals(expected, failureOf(call));
                }
            }

            final String gapText = "2025-03-09T02:30:00-05:00[America/New_York]";
            final String gap = "Cannot parse \"" + gapText + "\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'': "
                    + "Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)"
                    + " | java.time.DateTimeException: Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)";

            for (final org.junit.jupiter.api.function.Executable call : allParsers(Dates.DTF.ISO_ZONED_DATE_TIME, gapText, null)) {
                assertEquals(gap, failureOf(call));
            }

            final String fixedText = "2025-01-15T10:30:45+05:00[+03:00]";
            final String fixed = "Cannot parse \"" + fixedText + "\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX'['VV']'': "
                    + "Offset +05:00 is not valid for local date-time 2025-01-15T10:30:45 in zone +03:00; valid offsets are [+03:00]"
                    + " | java.time.DateTimeException: Offset +05:00 is not valid for local date-time 2025-01-15T10:30:45 in zone +03:00;"
                    + " valid offsets are [+03:00]";

            for (final org.junit.jupiter.api.function.Executable call : allParsers(Dates.DTF.ISO_ZONED_DATE_TIME, fixedText, null)) {
                assertEquals(fixed, failureOf(call));
            }

            // the static entry points that delegate to the DTF zoned parsers report the same diagnostic
            assertTrue(failureOf(() -> Dates.parseToInstant(text)).contains("Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45"));
            assertTrue(failureOf(() -> Dates.parseToZonedDateTime(text, Dates.ISO_ZONED_DATE_TIME_FORMAT))
                    .contains("Offset +01:00 is not valid for local date-time 2025-07-15T10:30:45"));
        }

        // G023-02: time-only text keeps its offset validation (on 1970-01-01) ahead of the missing-date
        // rejection of the instant-producing parsers; parseToTime anchors a valid one.
        @Test
        public void testDtfParse_timeOnlyOffsetValidatedFirst() {
            final Dates.DTF dtf = Dates.DTF.of("HH:mm:ssXXX'['VV']'");
            final String invalid = "10:30:45-04:00[America/New_York]";
            final String expected = "Cannot parse \"" + invalid + "\" with pattern 'HH:mm:ssXXX'['VV']'': "
                    + "Offset -04:00 is not valid for local date-time 1970-01-01T10:30:45 in zone America/New_York; valid offsets are [-05:00]"
                    + " | java.time.DateTimeException: Offset -04:00 is not valid for local date-time 1970-01-01T10:30:45 in zone America/New_York;"
                    + " valid offsets are [-05:00]";

            for (final java.util.TimeZone tz : fallbackZones()) {
                for (final org.junit.jupiter.api.function.Executable call : allParsers(dtf, invalid, tz)) {
                    assertEquals(expected, failureOf(call));
                }
            }

            final String valid = "10:30:45-05:00[America/New_York]";
            assertEquals(15L * 3_600_000L + 30L * 60_000L + 45_000L, dtf.parseToTime(valid).getTime());
            assertEquals(15L * 3_600_000L + 30L * 60_000L + 45_000L, dtf.parseToTime(valid, utc()).getTime());
            assertTrue(failureOf(() -> dtf.parseToInstant(valid)).contains("No date fields in \"" + valid + "\""));
            assertTrue(failureOf(() -> dtf.parseToCalendar(valid, utc())).contains("No date fields in \"" + valid + "\""));
        }

        // G023-02: valid zoned and offset text resolves to the same instants, zones and calendars as before.
        @Test
        public void testDtfParse_validZonedAndOffsetTextResolves() {
            final String overlapStandard = "2025-11-02T01:30:00-05:00[America/New_York]";
            final String overlapDaylight = "2025-11-02T01:30:00-04:00[America/New_York]";
            final String summer = "2025-07-15T10:30:45+02:00[Europe/Paris]";

            for (final java.util.TimeZone tz : fallbackZones()) {
                final Dates.DTF dtf = Dates.DTF.ISO_ZONED_DATE_TIME;
                assertEquals(1762065000000L, dtf.parseToInstant(overlapStandard, tz).toEpochMilli());
                assertEquals(1762061400000L, dtf.parseToInstant(overlapDaylight, tz).toEpochMilli());
                assertEquals(java.time.ZonedDateTime.parse(summer), dtf.parseToZonedDateTime(summer, tz));
                assertEquals(java.time.OffsetDateTime.parse("2025-11-02T01:30-05:00"), dtf.parseToOffsetDateTime(overlapStandard, tz));
                assertEquals(1762065000000L, dtf.parseToJUDate(overlapStandard, tz).getTime());
                assertEquals(1762065000000L, dtf.parseToDate(overlapStandard, tz).getTime());
                assertEquals(1762065000000L, dtf.parseToTime(overlapStandard, tz).getTime());
                assertEquals(1762065000000L, dtf.parseToTimestamp(overlapStandard, tz).getTime());

                final java.util.Calendar calendar = dtf.parseToCalendar(summer, tz);
                assertEquals(java.time.Instant.parse("2025-07-15T08:30:45Z").toEpochMilli(), calendar.getTimeInMillis());
                assertEquals("Europe/Paris", calendar.getTimeZone().getID());

                final Dates.DTF offset = Dates.DTF.ISO_OFFSET_DATE_TIME;
                assertEquals(java.time.Instant.parse("2025-01-15T05:00:45Z"), offset.parseToInstant("2025-01-15T10:30:45+05:30", tz));
                assertEquals(java.time.ZonedDateTime.parse("2025-01-15T10:30:45+05:30"), offset.parseToZonedDateTime("2025-01-15T10:30:45+05:30", tz));
                assertEquals(java.time.Instant.parse("2025-01-15T10:13:15Z"), offset.parseToInstant("2025-01-15T10:30:45+00:17:30", tz));
            }

            assertEquals(java.time.ZonedDateTime.parse(summer), Dates.parseToZonedDateTime(summer));
            assertEquals(java.time.Instant.parse("2025-07-15T08:30:45Z"), Dates.parseToInstant(summer));
        }
    }

    // ---- perf review 2026-10-08 G118 end ----
    // ---- perf review 2026-10-08 G120 begin ----
    @Nested
    class G120 {
        private final java.util.TimeZone newYork = java.util.TimeZone.getTimeZone("America/New_York");

        // A SimpleTimeZone with daylight-saving rules no ZoneId can express: the Calendar fallback of resolveLocalMillis.
        private final java.util.TimeZone customDst = new java.util.SimpleTimeZone(-5 * 3600_000, "custom-dst", 2, 8, -1, 7200000, 10, 1, -1, 7200000, 3600000);

        // A sub-second raw offset no ZoneId can express.
        private final java.util.TimeZone subSecond = new java.util.SimpleTimeZone(1500, "sub-second");

        private void assertParseFailure(final String expectedMessage, final Class<? extends Throwable> expectedCause,
                final org.junit.jupiter.api.function.Executable executable) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, executable);
            assertEquals(expectedMessage, e.getMessage());

            if (expectedCause == null) {
                assertNull(e.getCause());
            } else {
                assertNotNull(e.getCause());
                assertEquals(expectedCause, e.getCause().getClass());
            }
        }

        // G120-01: auto-detected fractional ISO offset text (+HH:mm, compact +HHmm) on the legacy targets - same instant, the fallback zone plays no part
        @Test
        void testParseToJUDate_autoOffsetFraction() {
            final String[][] cases = { { "2025-01-15T10:30:45.123+05:30", "2025-01-15T05:00:45.123Z" },
                    { "2025-01-15T10:30:45.123-0500", "2025-01-15T15:30:45.123Z" }, { "2025-01-15T10:30:45.5+0545", "2025-01-15T04:45:45.500Z" },
                    { "2025-01-15T10:30:45.123456789-09:30", "2025-01-15T20:00:45.123456789Z" }, { "2025-07-15T10:30:45.1-08:00", "2025-07-15T18:30:45.100Z" },
                    { "2025-01-15T10:30:45.123+18:00", "2025-01-14T16:30:45.123Z" }, { "2025-01-15T10:30:45.123-18:00", "2025-01-16T04:30:45.123Z" },
                    { "2025-01-15T10:30:45.123-1800", "2025-01-16T04:30:45.123Z" }, { "2025-01-15T10:30:45.123-00:00", "2025-01-15T10:30:45.123Z" },
                    { "2025-01-15T10:30:45.123+0000", "2025-01-15T10:30:45.123Z" }, { "2024-02-29T00:00:00.1+01:00", "2024-02-28T23:00:00.100Z" },
                    { "2025-03-09T02:30:00.5-05:00", "2025-03-09T07:30:00.500Z" }, { "2025-11-02T01:30:00.25-04:00", "2025-11-02T05:30:00.250Z" },
                    { "0001-01-01T00:00:00.000000001+01:00", "0000-12-31T23:00:00.000000001Z" },
                    { "9999-12-31T23:59:59.999999999-18:00", "+10000-01-01T17:59:59.999999999Z" },
                    { "1969-12-31T23:59:59.9999+00:00", "1969-12-31T23:59:59.9999Z" },
                    // a seconds-precision offset keeps the java.time route
                    { "1880-01-15T10:30:45.123-04:56:02", "1880-01-15T15:26:47.123Z" } };

            for (final String[] c : cases) {
                final String text = c[0];
                final long expected = java.time.Instant.parse(c[1]).toEpochMilli();

                assertEquals(expected, Dates.parseToJUDate(text).getTime(), text);
                assertEquals(expected, Dates.parseToJUDate(text, null, newYork).getTime(), text);
                assertEquals(expected, Dates.parseToJUDate(text, "", customDst).getTime(), text);
                assertEquals(expected, Dates.parseToJUDate(text, null, subSecond, java.util.Locale.FRANCE).getTime(), text);
                assertEquals(expected, Dates.parseToDate(text, null, newYork).getTime(), text);
                assertEquals(expected, Dates.parseToTime(text, "", customDst).getTime(), text);

                final java.util.Calendar calendar = Dates.parseToCalendar(text, null, newYork);
                assertEquals(expected, calendar.getTimeInMillis(), text);
                assertEquals(expected, Dates.parseToGregorianCalendar(text, "", customDst).getTimeInMillis(), text);
                // the routes that read this shape through parseAutoTimestampToInstant agree
                assertEquals(expected, Dates.parseToTimestamp(text, null, newYork).getTime(), text);
            }

            assertEquals(1736917245123L, Dates.parseToJUDate("2025-01-15T10:30:45.123+05:30").getTime());

            final java.util.Calendar kolkata = Dates.parseToCalendar("2025-01-15T10:30:45.123+05:30", null, newYork);
            assertEquals(19800000, kolkata.getTimeZone().getRawOffset());
            assertEquals(10, kolkata.get(java.util.Calendar.HOUR_OF_DAY));
            assertEquals(123, kolkata.get(java.util.Calendar.MILLISECOND));
        }

        // G120-01: offset text the direct read rejects still reaches the java.time grammar and keeps its diagnostic
        @Test
        void testParseToJUDate_autoOffsetFractionInvalidFields() {
            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;
            final String[][] cases = {
                    { "2025-02-29T00:00:00.0+05:30",
                            "Text '2025-02-29T00:00:00.0+05:30' could not be parsed: Invalid date 'February 29' as '2025' is not a leap year" },
                    { "2025-02-30T10:30:45.123+05:30", "Text '2025-02-30T10:30:45.123+05:30' could not be parsed: Invalid date 'FEBRUARY 30'" },
                    { "2025-13-01T00:00:00.1+05:30",
                            "Text '2025-13-01T00:00:00.1+05:30' could not be parsed: Invalid value for MonthOfYear (valid values 1 - 12): 13" },
                    { "2025-01-15T24:00:00.1+05:30",
                            "Text '2025-01-15T24:00:00.1+05:30' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24" },
                    { "2025-01-15T23:60:00.1-0500",
                            "Text '2025-01-15T23:60:00.1-05:00' could not be parsed: Invalid value for MinuteOfHour (valid values 0 - 59): 60" },
                    { "2025-01-15T23:59:60.1-05:00",
                            "Text '2025-01-15T23:59:60.1-05:00' could not be parsed: Invalid value for SecondOfMinute (valid values 0 - 59): 60" },
                    { "2025-01-15T10:30:45.123+18:01",
                            "Text '2025-01-15T10:30:45.123+18:01' could not be parsed: Zone offset not in valid range: -18:00 to +18:00" },
                    { "2025-01-15T10:30:45.123+19:00",
                            "Text '2025-01-15T10:30:45.123+19:00' could not be parsed: Zone offset not in valid range: -18:00 to +18:00" },
                    { "2025-01-15T10:30:45.123+05:60", "Text '2025-01-15T10:30:45.123+05:60' could not be parsed, unparsed text found at index 26" },
                    { "2025-01-15T10:30:45.123+2400",
                            "Text '2025-01-15T10:30:45.123+24:00' could not be parsed: Value out of range: Hour[0-23], Minute[0-59], Second[0-59]" },
                    { "2025-0a-15T10:30:45.123+05:30", "Text '2025-0a-15T10:30:45.123+05:30' could not be parsed at index 5" },
                    { "1880-01-15T10:30:45.123-04:56:60", "Text '1880-01-15T10:30:45.123-04:56:60' could not be parsed, unparsed text found at index 29" } };

            for (final String[] c : cases) {
                final String expectedMessage = "Cannot parse \"" + c[0] + "\" with the auto-detected format: " + c[1];

                assertParseFailure(expectedMessage, dtpe, () -> Dates.parseToJUDate(c[0]));
                assertParseFailure(expectedMessage, dtpe, () -> Dates.parseToDate(c[0], null, newYork));
                assertParseFailure(expectedMessage, dtpe, () -> Dates.parseToCalendar(c[0], "", customDst));
            }

            assertParseFailure("Format 'yyyy-MM-dd'T'HH:mm:ssXXX' requires a Common Era year from 0001 through 9999 written as exactly four digits: "
                    + "\"0000-01-01T00:00:00.1+05:30\"", null, () -> Dates.parseToJUDate("0000-01-01T00:00:00.1+05:30"));
        }

        // G120-02: auto-detected ISO local text with a 1-2 or 4-9 digit fraction - same wall-clock resolution, fraction truncated to milliseconds
        @Test
        void testParseToJUDate_autoLocalFraction() {
            final String[] zones = { "America/New_York", "Asia/Kolkata", "Australia/Lord_Howe", "UTC" };
            final String[] texts = { "2025-01-15T10:30:45.5", "2025-01-15T10:30:45.12", "2025-07-15T10:30:45.1234", "2025-01-15T10:30:45.123456",
                    "2025-07-15T10:30:45.123456789", "2025-01-15T10:30:45.000000001", "2024-02-29T23:59:59.999999999", "2025-03-09T01:59:59.99",
                    "2025-03-09T03:00:00.01", "2025-10-05T02:30:00.5", "2025-04-06T02:00:00.5", "1883-11-18T11:59:59.5", "1969-12-31T23:59:59.999999",
                    "0001-01-01T00:00:00.5", "9999-12-31T23:59:59.9" };

            for (final String zoneId : zones) {
                final java.util.TimeZone zone = java.util.TimeZone.getTimeZone(zoneId);
                final java.time.zone.ZoneRules rules = java.time.ZoneId.of(zoneId).getRules();

                for (final String text : texts) {
                    final java.time.LocalDateTime local = java.time.LocalDateTime.parse(text);
                    final java.util.List<java.time.ZoneOffset> offsets = rules.getValidOffsets(local);

                    if (offsets.size() != 1) {
                        continue;
                    }

                    final long expected = local.withNano(0).toInstant(offsets.get(0)).toEpochMilli() + local.getNano() / 1_000_000;

                    assertEquals(expected, Dates.parseToJUDate(text, null, zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToDate(text, "", zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToTime(text, null, zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToCalendar(text, null, zone).getTimeInMillis(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToGregorianCalendar(text, "", zone).getTimeInMillis(), text + " " + zoneId);
                    // the JDBC escape spelling and parseToTimestamp's direct read name the same instant
                    assertEquals(expected, Dates.parseToJUDate(text.replace('T', ' '), null, zone).getTime(), text + " " + zoneId);
                    assertEquals(expected, Dates.parseToTimestamp(text, null, zone).getTime(), text + " " + zoneId);
                }
            }

            assertEquals(-1L, Dates.parseToJUDate("1969-12-31T23:59:59.999999", null, java.util.TimeZone.getTimeZone("UTC")).getTime());
            // the Calendar fallback for rules no ZoneId can express: winter offset -05:00; the overlap keeps the standard offset
            assertEquals(java.time.Instant.parse("2025-01-15T15:30:45.123Z").toEpochMilli(),
                    Dates.parseToJUDate("2025-01-15T10:30:45.123456", null, customDst).getTime());
            assertEquals(1762065000500L, Dates.parseToTime("2025-11-02T01:30:00.5", "", customDst).getTime());
            assertEquals(java.time.Instant.parse("2025-07-15T14:30:45.120Z").toEpochMilli(),
                    Dates.parseToCalendar("2025-07-15T10:30:45.12", null, customDst).getTimeInMillis());
        }

        // G120-02: ISO local text the direct read rejects still reaches the java.time grammar; gap/overlap diagnostics unchanged
        @Test
        void testParseToJUDate_autoLocalFractionInvalidFields() {
            final Class<? extends Throwable> dtpe = java.time.format.DateTimeParseException.class;
            final String[][] cases = { { "2025-04-31T00:00:00.1", "Text '2025-04-31T00:00:00.1' could not be parsed: Invalid date 'APRIL 31'" },
                    { "2025-01-15T24:00:00.1", "Text '2025-01-15T24:00:00.1' could not be parsed: Invalid value for HourOfDay (valid values 0 - 23): 24" },
                    { "2025-02-29T12:00:00.123456",
                            "Text '2025-02-29T12:00:00.123456' could not be parsed: Invalid date 'February 29' as '2025' is not a leap year" },
                    { "2025-01-15T10:60:00.12", "Text '2025-01-15T10:60:00.12' could not be parsed: Invalid value for MinuteOfHour (valid values 0 - 59): 60" },
                    { "2025-01-15T10:30:60.1234",
                            "Text '2025-01-15T10:30:60.1234' could not be parsed: Invalid value for SecondOfMinute (valid values 0 - 59): 60" } };

            for (final String[] c : cases) {
                assertParseFailure("Cannot parse \"" + c[0] + "\" with the auto-detected format in time zone America/New_York: " + c[1], dtpe,
                        () -> Dates.parseToJUDate(c[0], null, newYork));
                assertParseFailure("Cannot parse \"" + c[0] + "\" with the auto-detected format in time zone custom-dst: " + c[1], dtpe,
                        () -> Dates.parseToTime(c[0], "", customDst));
            }

            assertParseFailure(
                    "Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)",
                    java.time.DateTimeException.class, () -> Dates.parseToJUDate("2025-03-09T02:30:00.5", null, newYork));
            assertParseFailure(
                    "Cannot parse \"2025-11-02T01:30:00.5\" with the auto-detected format in time zone America/New_York: "
                            + "Ambiguous local date-time 2025-11-02T01:30 in zone America/New_York (DST overlap); valid offsets are [-04:00, -05:00]",
                    java.time.DateTimeException.class, () -> Dates.parseToDate("2025-11-02T01:30:00.5", "", newYork));
            assertParseFailure("Cannot parse \"2025-03-09T02:30:00.5\" with the auto-detected format in time zone custom-dst: HOUR_OF_DAY: 2 -> 1",
                    IllegalArgumentException.class, () -> Dates.parseToTime("2025-03-09T02:30:00.5", "", customDst));
            assertParseFailure("Format 'yyyy-MM-dd'T'HH:mm:ss.SSS' requires a Common Era year from 0001 through 9999 written as exactly four digits: "
                    + "\"0000-01-01T00:00:00.5\"", null, () -> Dates.parseToJUDate("0000-01-01T00:00:00.5", null, newYork));
        }
    }
}
