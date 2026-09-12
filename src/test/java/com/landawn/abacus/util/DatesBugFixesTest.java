package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DatesBugFixesTest extends TestBase {

    private Locale priorDefault;

    private TimeZone priorTimeZone;

    @BeforeEach
    public void saveLocale() {
        priorDefault = Locale.getDefault();
        priorTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(priorDefault);
        TimeZone.setDefault(priorTimeZone);
    }

    /** B13: RFC_1123 format under non-English locale must still produce English DoW/month. */
    @Test
    public void format_rfc1123_underFrenchLocale_producesEnglishOutput() {
        Locale.setDefault(Locale.FRENCH);
        Date date = new Date(1767616245000L); // 2026-01-05 12:30:45 UTC (Monday)
        String formatted = Dates.format(date, Dates.RFC_1123_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        assertNotNull(formatted);
        assertTrue(formatted.matches("^(Mon|Tue|Wed|Thu|Fri|Sat|Sun), .*"), "RFC-1123 output must start with English DoW: " + formatted);
        assertTrue(formatted.matches(".*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec).*"), "RFC-1123 output must contain English month: " + formatted);
    }

    /** B13: DTF.RFC_1123_DATE_TIME constant must round-trip an English HTTP date string. */
    @Test
    public void dtf_rfc1123_parsesEnglishHttpDate() {
        Locale.setDefault(Locale.JAPAN);
        String http = "Mon, 25 Dec 2023 14:30:45 GMT";
        ZonedDateTime z = Dates.DTF.RFC_1123_DATE_TIME.parseToZonedDateTime(http);
        assertNotNull(z);
        assertEquals(2023, z.getYear());
        assertEquals(12, z.getMonthValue());
        assertEquals(25, z.getDayOfMonth());
    }

    /** B13: SDF cache uses Locale.US for stable MMM month names. */
    @Test
    public void format_monthName_stableUnderTurkishLocale() {
        Locale.setDefault(new Locale("tr", "TR"));
        Date date = new Date(1767616245000L);
        String formatted = Dates.format(date, "MMM dd, yyyy", TimeZone.getTimeZone("UTC"));
        assertEquals("Jan 05, 2026", formatted);
    }

    /** B14: addDays must preserve wall-clock across DST. */
    @Test
    public void addDays_acrossSpringForward_preservesWallClock() {
        TimeZone priorTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        try {
            Date before = Dates.parseToJUDate("2025-03-08T12:00:00", "yyyy-MM-dd'T'HH:mm:ss");
            Date after = Dates.addDays(before, 1);
            String formatted = Dates.format(after, "yyyy-MM-dd HH:mm:ss");
            assertEquals("2025-03-09 12:00:00", formatted);
        } finally {
            TimeZone.setDefault(priorTz);
        }
    }

    /** B14: addWeeks under the same DST rules. */
    @Test
    public void addWeeks_acrossSpringForward_preservesWallClock() {
        TimeZone priorTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        try {
            Date before = Dates.parseToJUDate("2025-03-02T12:00:00", "yyyy-MM-dd'T'HH:mm:ss");
            Date after = Dates.addWeeks(before, 1);
            String formatted = Dates.format(after, "yyyy-MM-dd HH:mm:ss");
            assertEquals("2025-03-09 12:00:00", formatted);
        } finally {
            TimeZone.setDefault(priorTz);
        }
    }

    /** B15: parseToDate retains the resolved instant, including a time-of-day component. */
    @Test
    public void parseToDate_withTimeComponent_preservesInstant() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        java.sql.Date d = Dates.parseToDate("2025-10-22T14:30:45", "yyyy-MM-dd'T'HH:mm:ss", utc);
        assertNotNull(d);
        assertEquals("2025-10-22T14:30:45", Dates.format(d, "yyyy-MM-dd'T'HH:mm:ss", utc));
        assertEquals(Dates.parseToJUDate("2025-10-22T14:30:45", "yyyy-MM-dd'T'HH:mm:ss", utc).getTime(), d.getTime());
    }

    /** B15: parseToTime retains the resolved instant, including a date component. */
    @Test
    public void parseToTime_withDateComponent_preservesInstant() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        java.sql.Time t = Dates.parseToTime("2025-10-22T14:30:45", "yyyy-MM-dd'T'HH:mm:ss", utc);
        assertNotNull(t);
        assertEquals("2025-10-22T14:30:45", Dates.format(t, "yyyy-MM-dd'T'HH:mm:ss", utc));
        assertEquals(Dates.parseToJUDate("2025-10-22T14:30:45", "yyyy-MM-dd'T'HH:mm:ss", utc).getTime(), t.getTime());
    }

    /** B15: time-only input remains valid and is anchored to 1970-01-01. */
    @Test
    public void parseToTime_timeOnly_unchanged() {
        java.sql.Time t = Dates.parseToTime("10:15:30", "HH:mm:ss");
        assertNotNull(t);
        assertEquals(java.time.LocalTime.of(10, 15, 30), t.toLocalTime());
    }

    /**
     * B16: addMonths must clamp to the last valid day of the target month
     * (Calendar.add semantics). Jan 31 + 1 month -> Feb 28 (or Feb 29 in a leap year).
     */
    @Test
    public void addMonths_jan31_plusOne_clampsToFebruaryEnd() {
        // Non-leap year 2023: Jan 31 + 1 month -> Feb 28
        java.util.Date jan31_2023 = Dates.parseToJUDate("2023-01-31", "yyyy-MM-dd");
        java.util.Date plus1 = Dates.addMonths(jan31_2023, 1);
        assertEquals("2023-02-28", Dates.format(plus1, "yyyy-MM-dd"));

        // Leap year 2024: Jan 31 + 1 month -> Feb 29
        java.util.Date jan31_2024 = Dates.parseToJUDate("2024-01-31", "yyyy-MM-dd");
        java.util.Date plus1Leap = Dates.addMonths(jan31_2024, 1);
        assertEquals("2024-02-29", Dates.format(plus1Leap, "yyyy-MM-dd"));
    }

    /**
     * B17: addYears on Feb 29 of a leap year must clamp to Feb 28 in non-leap target year.
     */
    @Test
    public void addYears_feb29_plusOne_clampsToFeb28() {
        java.util.Date feb29_2024 = Dates.parseToJUDate("2024-02-29", "yyyy-MM-dd");
        java.util.Date plus1 = Dates.addYears(feb29_2024, 1);
        assertEquals("2025-02-28", Dates.format(plus1, "yyyy-MM-dd"));
    }

    /**
     * B18: ISO8601Util.parseInstant must reject clearly invalid text with a date-time parse error.
     */
    @Test
    public void iso8601Util_parse_invalidInput_throwsIAE() {
        assertThrows(java.time.format.DateTimeParseException.class, () -> ISO8601Util.parseInstant("not-a-date"));
        assertThrows(java.time.format.DateTimeParseException.class, () -> ISO8601Util.parseInstant(""));
        // Garbage timezone indicator
        assertThrows(java.time.format.DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:45X"));
    }

    /**
     * B19: ISO8601Util.parseInstant should accept the date-only and time-only-component forms documented
     * in its Javadoc, and default to UTC when no timezone designator is present in those forms.
     */
    @Test
    public void iso8601Util_parse_dateOnly_assumesUtcMidnight() {
        final java.time.Instant instant = ISO8601Util.parseInstant("2023-12-25");
        assertNotNull(instant);
        assertEquals(java.time.Instant.parse("2023-12-25T00:00:00Z"), instant);
    }

    /**
     * B20: Duration.toString() with a duration whose absolute value contains hours, minutes,
     * seconds, and millis must emit a single sign at the start (regression for negative-component bug).
     */
    @Test
    public void duration_toString_negativeMixed_singleSign() {
        // -(1 hour 1 minute 1.500 seconds) = -3_661_500 ms
        Duration d = Duration.ofMillis(-3_661_500L);
        assertEquals("-PT1H1M1.500S", d.toString());
    }

    /**
     * B21: Dates.parseToTimestamp on a date-only string should default the time to midnight in
     * the parsing zone (not throw, not return null).
     */
    @Test
    public void parseToTimestamp_dateOnly_setsMidnight() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        java.sql.Timestamp ts = Dates.parseToTimestamp("2023-12-25", null, utc);
        assertNotNull(ts);
        java.util.Calendar cal = java.util.Calendar.getInstance(utc);
        cal.setTimeInMillis(ts.getTime());
        assertEquals(2023, cal.get(java.util.Calendar.YEAR));
        assertEquals(java.util.Calendar.DECEMBER, cal.get(java.util.Calendar.MONTH));
        assertEquals(25, cal.get(java.util.Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(java.util.Calendar.MINUTE));
        assertEquals(0, cal.get(java.util.Calendar.SECOND));
        assertEquals(0, cal.get(java.util.Calendar.MILLISECOND));
    }

    /**
     * B22: Concurrent format/parse should be safe (SimpleDateFormat is NOT thread-safe;
     * Dates wraps it in a per-format pool). Smoke test: many threads, no exceptions, correct results.
     */
    @Test
    public void concurrent_formatParse_threadSafety() throws Exception {
        final int threads = 8;
        final int iterations = 500;
        final java.util.concurrent.ExecutorService es = java.util.concurrent.Executors.newFixedThreadPool(threads);
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(threads);
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        for (int t = 0; t < threads; t++) {
            es.submit(() -> {
                try {
                    for (int i = 0; i < iterations; i++) {
                        Date d = new Date(1_700_000_000_000L + i * 1000L);
                        String s = Dates.format(d, Dates.LOCAL_DATE_TIME_FORMAT, utc);
                        Date back = Dates.parseToJUDate(s, Dates.LOCAL_DATE_TIME_FORMAT, utc);
                        assertEquals(d.getTime(), back.getTime());
                    }
                } catch (Throwable th) {
                    failure.compareAndSet(null, th);
                } finally {
                    done.countDown();
                }
            });
        }
        done.await();
        es.shutdownNow();
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    /**
     * B23: format() with a localized pattern must use Locale.US for stable EEE/MMM names
     * even when the JVM default locale changes (German default).
     */
    @Test
    public void format_dayOfWeek_stableUnderGermanLocale() {
        Locale.setDefault(Locale.GERMAN);
        Date d = new Date(1767616245000L); // Monday 2026-01-05 12:30:45 UTC
        String formatted = Dates.format(d, "EEEE, MMMM dd yyyy", TimeZone.getTimeZone("UTC"));
        assertEquals("Monday, January 05 2026", formatted);
    }

    /**
     * B24: fastDateParse must validate literal separators before parsing fixed offsets.
     */
    @Test
    public void fastDateParse_rejectsMismatchedSeparatorsForExplicitFormat() {
        TimeZone utc = TimeZone.getTimeZone("UTC");

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025/01/15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, utc));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15 10:30:45", Dates.ISO_LOCAL_DATE_TIME_FORMAT, utc));
        assertNotNull(Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, utc));
    }

    /**
     * Report 3.3: format(...) with a 'Z'-designator pattern must reject a conflicting non-UTC zone
     * with IllegalArgumentException (consistent with the parse side), instead of silently using UTC.
     * A null or UTC-equivalent zone is accepted.
     */
    @Test
    public void format_zPattern_rejectsConflictingNonUtcZone() {
        Date date = new Date(1736937045000L); // 2025-01-15T10:30:45Z
        TimeZone pst = TimeZone.getTimeZone("America/Los_Angeles");

        // Conflicting non-UTC zone with an explicit 'Z' pattern -> IAE.
        assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, pst));
        // formatTo must reject it too.
        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(date, Dates.ISO_8601_DATE_TIME_FORMAT, pst, new StringBuilder()));

        // A null format is exempt: with an explicit zone the default becomes the offset-bearing
        // format rendered in that zone (02:30:45-08:00 = 10:30:45Z - 8h). Review 2026-08-29 / D4
        // replaced the zone-less local default, which named no zone and could not be parsed back.
        assertEquals("2025-01-15T02:30:45-08:00", Dates.format(date, null, pst));
        assertEquals(date.getTime(), Dates.parseToJUDate(Dates.format(date, null, pst)).getTime());

        // Accepted: null zone and UTC-equivalent zones, all rendered in UTC.
        assertEquals("2025-01-15T10:30:45Z", Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, null));
        assertEquals("2025-01-15T10:30:45Z", Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
        assertEquals("2025-01-15T10:30:45Z", Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, TimeZone.getTimeZone("GMT")));
    }

    /**
     * Report 3.5: DTF 'Z'-format parseToOffsetDateTime/parseToZonedDateTime must return the UTC
     * offset/zone (matching the 'Z' designator), not the JVM default zone's offset. Verified under a
     * non-UTC default zone so a default-zone bug would surface.
     */
    @Test
    public void dtfZFormat_offsetAndZonedParsers_useUtc() {
        TimeZone prior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles")); // non-UTC default
            java.time.OffsetDateTime odt = Dates.DTF.ISO_8601_DATE_TIME.parseToOffsetDateTime("2023-12-25T14:30:45Z");
            assertEquals(java.time.ZoneOffset.UTC, odt.getOffset());
            assertEquals(1703514645000L, odt.toInstant().toEpochMilli());

            ZonedDateTime zdt = Dates.DTF.ISO_8601_DATE_TIME.parseToZonedDateTime("2023-12-25T14:30:45Z");
            assertEquals(0, zdt.getOffset().getTotalSeconds());
            assertEquals(1703514645000L, zdt.toInstant().toEpochMilli());
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    /**
     * Report 3.9: the registered GregorianCalendar creator must return a GregorianCalendar even when
     * the default locale selects a non-Gregorian calendar (e.g. the Japanese imperial calendar), where
     * Calendar.getInstance() would otherwise yield a JapaneseImperialCalendar and CCE the caller.
     */
    @Test
    public void gregorianCalendarCreator_stableUnderJapaneseCalendarLocale() {
        Locale.setDefault(Locale.forLanguageTag("ja-JP-u-ca-japanese"));
        java.util.GregorianCalendar source = new java.util.GregorianCalendar();
        java.util.GregorianCalendar rolled = Dates.addDays(source, 1); // must not ClassCastException
        assertNotNull(rolled);
        assertTrue(rolled instanceof java.util.GregorianCalendar);
    }

    @Test
    public void explicitNonIsoFormatParsers_rejectTrailingGarbage() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        // strict full-consumption parsing applies to every explicit format, not just the ISO 'Z' ones
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15xyz", Dates.LOCAL_DATE_FORMAT, utc));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("2025-01-15xyz", Dates.LOCAL_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15 10:30:45xyz", Dates.LOCAL_DATE_TIME_FORMAT, utc));
    }

    @Test
    public void explicitIsoFormatParsers_rejectTrailingGarbage() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45Zjunk", Dates.ISO_8601_DATE_TIME_FORMAT, utc));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("2025-01-15T10:30:45Zjunk", Dates.ISO_8601_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45Zjunk", Dates.ISO_8601_DATE_TIME_FORMAT, utc));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("2025-01-15T10:30:45Zjunk", Dates.ISO_8601_DATE_TIME_FORMAT, utc));
    }

    @Test
    public void currentTimePlus_rejectsNullUnitWithIAE() {
        assertThrows(IllegalArgumentException.class, () -> Dates.currentTimePlus(1, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.currentDatePlus(1, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.currentTimestampPlus(1, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.currentJUDatePlus(1, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.currentCalendarPlus(1, null));
    }

    @Test
    public void dtfIsoZonedDateTime_supportsLegacyDateAndCalendarPaths() {
        final TimeZone prior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToJUDate("1970-01-01T00:00:00Z[UTC]").getTime());
            assertEquals("1970-01-01T00:00:00Z[UTC]", Dates.DTF.ISO_ZONED_DATE_TIME.format(new Date(0L)));

            final java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTimeInMillis(0L);
            assertEquals("1970-01-01T05:30:00+05:30[Asia/Kolkata]", Dates.DTF.ISO_ZONED_DATE_TIME.format(calendar));
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    @Test
    public void dtfIsoZonedDateTime_formatToSupportsLegacyDateAndCalendarPaths() {
        final TimeZone prior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            final StringBuilder dateOut = new StringBuilder();
            Dates.DTF.ISO_ZONED_DATE_TIME.formatTo(new Date(0L), dateOut);
            assertEquals("1970-01-01T00:00:00Z[UTC]", dateOut.toString());

            final java.util.Calendar calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
            calendar.setTimeInMillis(0L);
            final StringBuilder calendarOut = new StringBuilder();
            Dates.DTF.ISO_ZONED_DATE_TIME.formatTo(calendar, calendarOut);
            assertEquals("1970-01-01T05:30:00+05:30[Asia/Kolkata]", calendarOut.toString());
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    @Test
    public void dtfIsoZonedDateTime_supportsLegacyParseOverloads() {
        final String text = "1970-01-01T00:00:00Z[UTC]";
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToJUDate(text, TimeZone.getTimeZone("Asia/Kolkata")).getTime());
        assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToDate(text).getTime());
        assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToTime(text).getTime());
        assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToTimestamp(text).getTime());
        assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToTimestamp(text, utc).getTime());
        assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToCalendar(text).getTimeInMillis());
        assertEquals(0L, Dates.DTF.ISO_ZONED_DATE_TIME.parseToCalendar(text, utc).getTimeInMillis());
    }

    /**
     * The DTF parseTo* methods no longer treat a purely numeric string as epoch milliseconds:
     * the pattern is authoritative, so numeric input that doesn't match the pattern fails.
     * Epoch input goes through the explicit epoch APIs ({@code Instant.ofEpochMilli}, {@code Dates.create*}).
     */
    @Test
    public void dtfParsers_numericInputParsedPerPattern_notAsEpochMillis() {
        final long millis = 1703514645123L; // 2023-12-25T14:30:45.123Z
        final String s = String.valueOf(millis);
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToJUDate(s));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp(s));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToCalendar(s));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToJUDate(s, TimeZone.getTimeZone("Asia/Kolkata")));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToDate(s));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_TIME.parseToTime(s));

        // A pattern that actually matches digits parses them as date fields, not epoch millis.
        assertEquals(LocalDate.of(2023, 12, 25), Dates.DTF.of("yyyyMMdd").parseToLocalDate("20231225"));

        // The explicit epoch APIs cover the old use case.
        assertEquals(millis, Dates.createTimestamp(millis).getTime());
        assertEquals(millis, Dates.createJUDate(millis).getTime());
        assertEquals(millis, Dates.createCalendar(millis, utc).getTimeInMillis());
    }

    @Test
    public void dtfParseToCalendar_epochMillisViaExplicitApi_honorsExplicitTimeZone() {
        final TimeZone prior = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

            // numeric text is no longer accepted by the patterned parser
            final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
            assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToCalendar("0", kolkata));

            // the explicit epoch API covers the old use case
            final java.util.Calendar cal = Dates.createCalendar(0L, kolkata);

            assertEquals(0L, cal.getTimeInMillis());
            assertEquals(kolkata, cal.getTimeZone());
            assertEquals(5, cal.get(java.util.Calendar.HOUR_OF_DAY));
            assertEquals(30, cal.get(java.util.Calendar.MINUTE));
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    @Test
    public void dtfFormatToTemporalAccessor_wrapsAppendIOException() {
        final Appendable throwingAppendable = new Appendable() {
            @Override
            public Appendable append(final CharSequence csq) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public Appendable append(final CharSequence csq, final int start, final int end) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public Appendable append(final char c) throws IOException {
                throw new IOException("boom");
            }
        };

        // DTF.formatTo wraps IOException from the appendable in UncheckedIOException
        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                () -> Dates.DTF.LOCAL_DATE.formatTo(LocalDate.of(2023, 12, 25), throwingAppendable));
    }

    @Test
    public void formatTo_rejectsNullAppendableWithIAE() {
        final Date date = new Date(0L);
        final java.util.Calendar calendar = Dates.createCalendar(0L);
        final javax.xml.datatype.XMLGregorianCalendar xmlCalendar = Dates.createXMLGregorianCalendar(0L);

        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(date, (Appendable) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo((Date) null, (Appendable) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(calendar, (Appendable) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(xmlCalendar, (Appendable) null));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.formatTo(LocalDate.of(2023, 12, 25), null));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.formatTo((java.time.temporal.TemporalAccessor) null, null));
    }

    /**
     * Report R9: isSameLocalTime must have a java.util.Date overload (default-zone), mirroring isSameDay.
     */
    @Test
    public void isSameLocalTime_dateOverload() {
        Date d1 = new Date(1672585530123L);
        Date d2 = new Date(1672585530123L);
        Date d3 = new Date(1672585531123L); // 1 second later
        assertTrue(Dates.isSameLocalTime(d1, d2));
        assertTrue(!Dates.isSameLocalTime(d1, d3));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameLocalTime((Date) null, d2));
    }

    /**
     * Report R10: isOverlapping/isBetween must provide Calendar overloads.
     */
    @Test
    public void overlappingAndBetween_calendarOverloads() {
        java.util.Calendar c1 = Dates.createCalendar(1000L);
        java.util.Calendar c5 = Dates.createCalendar(5000L);
        java.util.Calendar c10 = Dates.createCalendar(10000L);
        java.util.Calendar c15 = Dates.createCalendar(15000L);

        assertTrue(Dates.isOverlapping(c1, c10, c5, c15));
        assertTrue(!Dates.isOverlapping(c1, c10, c10, c15)); // adjacent, endpoints exclusive
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(c10, c1, c5, c15)); // start after end

        assertTrue(Dates.isBetween(c5, c1, c10));
        assertTrue(Dates.isBetween(c1, c1, c10)); // start boundary inclusive
        assertTrue(!Dates.isBetween(c15, c1, c10));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(c5, c10, c1)); // start after end
    }

    /**
     * Report R4: bare numeric epoch-millis text is ambiguous and rejected by the parseTo* methods
     * regardless of any supplied TimeZone; epoch input goes through the explicit zone-independent
     * parseEpochMillis API.
     */
    @Test
    public void parseToJUDate_numericEpoch_throws() {
        final long millis = 1703514645123L;
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(String.valueOf(millis), null, TimeZone.getTimeZone("Asia/Kolkata")));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(String.valueOf(millis), null, TimeZone.getTimeZone("UTC")));
        assertEquals(millis, Dates.parseEpochMillis(String.valueOf(millis)));
    }

    @Test
    public void parseSqlDateTime_numericEpoch_throws() {
        final long millis = 1703514645123L;
        final String text = String.valueOf(millis);

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(text, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime(text, null));
        assertEquals(millis, Dates.parseEpochMillis(text));
    }

    // ---------------------------------------------------------------------------------------------
    // Review 2026-08-29 / B1: round, truncate and ceiling must respect their own boundary contract
    // across daylight-saving transitions. Every case below produced a wrong instant before the fix.
    // ---------------------------------------------------------------------------------------------

    private static long instantOf(final String isoOffsetText) {
        return OffsetDateTime.parse(isoOffsetText).toInstant().toEpochMilli();
    }

    private static String render(final long millis, final String zoneId) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId)).toString();
    }

    /** B1: inside a fall-back overlap the ceiling used to jump a whole DST offset past the input. */
    @Test
    public void ceiling_insideDstOverlap_doesNotOvershoot() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        final Date input = new Date(instantOf("2009-11-01T01:11:40.691-04:00"));

        assertEquals(309L, Dates.ceiling(input, Calendar.SECOND).getTime() - input.getTime());
        assertEquals(19_309L, Dates.ceiling(input, Calendar.MINUTE).getTime() - input.getTime());
        // the next hour boundary is the second pass through 01:00, not 02:00
        assertEquals("2009-11-01T01:00-05:00[America/New_York]", render(Dates.ceiling(input, Calendar.HOUR_OF_DAY).getTime(), "America/New_York"));

        // truncate and round were already correct here; keep them pinned
        assertEquals("2009-11-01T01:11:40-04:00[America/New_York]", render(Dates.truncate(input, Calendar.SECOND).getTime(), "America/New_York"));
        assertEquals("2009-11-01T01:00-04:00[America/New_York]", render(Dates.round(input, Calendar.HOUR_OF_DAY).getTime(), "America/New_York"));

        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calendar.setTimeInMillis(input.getTime());
        assertEquals(309L, Dates.ceiling(calendar, Calendar.SECOND).getTimeInMillis() - input.getTime());
    }

    /** B1: a 30-minute spring-forward used to leave day, month and year boundaries at 00:30. */
    @Test
    public void ceiling_acrossSubHourDstGap_landsOnMidnight() {
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Lord_Howe"));
        final Date input = new Date(instantOf("2022-10-02T02:31:18.908+11:00"));

        assertEquals("2022-10-03T00:00+11:00[Australia/Lord_Howe]", render(Dates.ceiling(input, Calendar.DATE).getTime(), "Australia/Lord_Howe"));
        assertEquals("2022-11-01T00:00+11:00[Australia/Lord_Howe]", render(Dates.ceiling(input, Calendar.MONTH).getTime(), "Australia/Lord_Howe"));
        assertEquals("2023-01-01T00:00+11:00[Australia/Lord_Howe]", render(Dates.ceiling(input, Calendar.YEAR).getTime(), "Australia/Lord_Howe"));
        // local midnight does not exist on this day, so the start of day is the transition instant
        assertEquals("2022-10-02T00:00+10:30[Australia/Lord_Howe]", render(Dates.truncate(input, Calendar.DATE).getTime(), "Australia/Lord_Howe"));
    }

    /** B1: a midnight transition used to push month and year truncation to 01:00. */
    @Test
    public void truncate_acrossMidnightDstGap_staysOnMidnight() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        final Date input = new Date(instantOf("2018-11-04T05:30:00-02:00"));

        assertEquals("2018-01-01T00:00-02:00[America/Sao_Paulo]", render(Dates.truncate(input, Calendar.YEAR).getTime(), "America/Sao_Paulo"));
        assertEquals("2018-11-01T00:00-03:00[America/Sao_Paulo]", render(Dates.truncate(input, Calendar.MONTH).getTime(), "America/Sao_Paulo"));

        final Date truncated = Dates.truncate(input, Calendar.YEAR);
        assertEquals(truncated.getTime(), Dates.truncate(truncated, Calendar.YEAR).getTime(), "truncate must be idempotent");
    }

    /** B1: the AM_PM bump added twelve elapsed hours, so noon drifted to 13:00 on a spring-forward day. */
    @Test
    public void ceiling_amPm_isAWallClockBoundary() {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));

        final Date morning = new Date(instantOf("2011-03-27T09:01:42.443+02:00"));
        assertEquals("2011-03-27T12:00+02:00[Europe/Berlin]", render(Dates.ceiling(morning, Calendar.AM_PM).getTime(), "Europe/Berlin"));

        // this one used to return an instant BEFORE its input
        final Date evening = new Date(instantOf("2020-03-29T23:18:55.076+02:00"));
        assertTrue(Dates.ceiling(evening, Calendar.AM_PM).getTime() >= evening.getTime());
        assertEquals("2020-03-30T00:00+02:00[Europe/Berlin]", render(Dates.ceiling(evening, Calendar.AM_PM).getTime(), "Europe/Berlin"));
    }

    /**
     * B1: the contract itself, probed around <b>every transition</b> of each zone - truncate is
     * idempotent and never later than its input, ceiling is a boundary and never earlier, and round is
     * one of the two.
     *
     * <p>Sampling instants uniformly, as the first version of this test did, essentially never lands in
     * the few minutes just after a daylight-saving gap - which is precisely where the boundary engine
     * broke for 414 of the JDK's 632 zones. Probing around the transitions themselves finds it at once.</p>
     */
    @Test
    public void roundTruncateCeiling_holdTheirContractAroundEveryTransition() {
        final String[] zones = { "UTC", "America/New_York", "Europe/Berlin", "Australia/Lord_Howe", "America/Sao_Paulo", "Asia/Tehran", "Asia/Pontianak",
                "Africa/Algiers", "Pacific/Chatham", "America/Santiago", "Africa/Cairo", "Asia/Kolkata" };
        final int[] fields = { Calendar.SECOND, Calendar.MINUTE, Calendar.HOUR_OF_DAY, Calendar.AM_PM, Calendar.DATE, Dates.SEMI_MONTH, Calendar.MONTH,
                Calendar.YEAR };
        // instants relative to each transition, in milliseconds
        final long[] offsets = { -1L, 0L, 1L, 1000L, 60_000L, 1_800_000L, 3_600_000L, 5_400_000L, 86_400_000L };

        for (final String zoneId : zones) {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
            final ZoneId zone = ZoneId.of(zoneId);

            for (final ZoneOffsetTransition transition : zone.getRules().getTransitions()) {
                for (final long offset : offsets) {
                    final long millis = transition.getInstant().toEpochMilli() + offset;

                    for (final int field : fields) {
                        final Date input = new Date(millis);
                        final long truncated = Dates.truncate(input, field).getTime();
                        final long ceiled = Dates.ceiling(input, field).getTime();
                        final long rounded = Dates.round(input, field).getTime();
                        final String where = zoneId + " field=" + field + " at " + Instant.ofEpochMilli(millis).atZone(zone);

                        assertTrue(truncated <= millis, "truncate must not move forward: " + where);
                        assertEquals(truncated, Dates.truncate(new Date(truncated), field).getTime(), "truncate must be idempotent: " + where);
                        assertTrue(ceiled >= millis, "ceiling must not move backward: " + where);
                        assertEquals(ceiled, Dates.truncate(new Date(ceiled), field).getTime(), "ceiling must land on a boundary: " + where);
                        assertTrue(rounded == truncated || rounded == ceiled, "round must pick one of the two boundaries: " + where);

                        // Minimality, checked by playing the two against each other. Both are exact, so
                        // the greatest boundary below the ceiling cannot be past the input, and the least
                        // boundary above the truncation cannot be short of it. A skipped boundary - the
                        // second occurrence of one an overlap replays, or one a gap collapses onto a
                        // transition - breaks exactly these two.
                        if (ceiled > millis) {
                            assertTrue(Dates.truncate(new Date(ceiled - 1), field).getTime() <= millis, "ceiling skipped a boundary: " + where);
                        }

                        if (truncated < millis) {
                            // A fall-back whose replayed window straddles a unit boundary (Pacific/Chatham falls back
                            // from 03:45 to 02:45; America/St_Johns fell back from 00:01 to 23:01) interleaves the civil
                            // units in instant order: the NEXT unit's boundary was struck before the clock fell back into
                            // this one. The truncation is the start of the input's own unit (2026-09-03 review, C-009),
                            // so a boundary between it and the input is allowed exactly when it starts a later civil unit.
                            final long between = Dates.ceiling(new Date(truncated + 1), field).getTime();
                            assertTrue(between > millis || Dates.truncatedCompareTo(new Date(between), new Date(millis), field) > 0,
                                    "truncate skipped a boundary of its own unit: " + where);
                        }

                        // round must pick the genuinely nearer of the two, ties going to the later one
                        final long expected = (ceiled - millis) > (millis - truncated) ? truncated : ceiled;
                        assertEquals(expected, rounded, "round must pick the nearer boundary: " + where);
                    }
                }
            }
        }
    }

    /**
     * B1 follow-up: a local time a gap removes must resolve to the instant the gap <b>ends</b>.
     * {@code ZonedDateTime.ofLocal} instead shifts it forward by the gap's whole duration, which
     * overshot that instant - and the input - by however far into the gap the boundary fell.
     * Asia/Pontianak jumped 90 minutes in 1942, so local 01:00 sat one hour inside the gap.
     */
    @Test
    public void truncate_insideALongerGap_resolvesToTheInstantTheGapEnds() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Pontianak"));
        final long millis = OffsetDateTime.parse("1942-01-29T01:30:00.001+09:00").toInstant().toEpochMilli();
        final long truncated = Dates.truncate(new Date(millis), Calendar.HOUR_OF_DAY).getTime();

        assertEquals("1942-01-29T01:30+09:00[Asia/Pontianak]", Instant.ofEpochMilli(truncated).atZone(ZoneId.of("Asia/Pontianak")).toString());
        assertTrue(truncated <= millis);
        assertEquals(truncated, Dates.truncate(new Date(truncated), Calendar.HOUR_OF_DAY).getTime());
    }

    /**
     * B1 follow-up: a sub-minute overlap repeats the local clock, so searching forward one boundary at a
     * time from the floor walks through boundaries that are all still behind the input. Africa/Algiers
     * moved back 9m21s in 1911.
     */
    @Test
    public void ceiling_acrossASubMinuteOverlap_findsTheLeastBoundary() {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Algiers"));
        final long millis = Instant.parse("1911-03-10T23:50:39Z").toEpochMilli();
        final long ceiled = Dates.ceiling(new Date(millis), Calendar.MINUTE).getTime();

        assertTrue(ceiled >= millis, "ceiling must not move backward");
        assertEquals(ceiled, Dates.truncate(new Date(ceiled), Calendar.MINUTE).getTime(), "ceiling must land on a boundary");
        assertEquals("1911-03-10T23:51Z[Africa/Algiers]", Instant.ofEpochMilli(ceiled).atZone(ZoneId.of("Africa/Algiers")).toString());
    }

    /**
     * B1 follow-up: a fall-back replays the local clock, so the second occurrence of the boundary the
     * input has just passed can be a millisecond away while the next local boundary is a whole unit
     * away. Stepping a unit of elapsed time and snapping back walked straight over it. America/Havana
     * falls back at 01:00 on 1 November 2026, so midnight happens twice.
     */
    @Test
    public void ceiling_takesTheReplayedBoundaryInsideAnOverlap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Havana"));
        final ZoneId zone = ZoneId.of("America/Havana");
        final long millis = OffsetDateTime.parse("2026-11-01T00:59:59.999-04:00").toInstant().toEpochMilli();

        // the second midnight is one millisecond later, not twelve hours later
        assertEquals("2026-11-01T00:00-05:00[America/Havana]",
                Instant.ofEpochMilli(Dates.ceiling(new Date(millis), Calendar.AM_PM).getTime()).atZone(zone).toString());
        assertEquals(1L, Dates.ceiling(new Date(millis), Calendar.AM_PM).getTime() - millis);
        // and round therefore takes it rather than the first midnight an hour behind
        assertEquals("2026-11-01T00:00-05:00[America/Havana]",
                Instant.ofEpochMilli(Dates.round(new Date(millis), Calendar.AM_PM).getTime()).atZone(zone).toString());
        assertEquals("2026-11-01T00:00-04:00[America/Havana]",
                Instant.ofEpochMilli(Dates.truncate(new Date(millis), Calendar.AM_PM).getTime()).atZone(zone).toString());
    }

    /**
     * B1 follow-up: a gap collapses the local boundary it removes onto the instant the gap ends, and that
     * instant is itself a boundary. Asia/Pontianak jumped 90 minutes in 1942, so the hour and the day
     * both begin at 01:30.
     */
    @Test
    public void ceiling_takesTheGapCollapsedBoundary() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Pontianak"));
        final ZoneId zone = ZoneId.of("Asia/Pontianak");
        final long millis = OffsetDateTime.parse("1942-01-28T23:59:59.999+07:30").toInstant().toEpochMilli();

        for (final int field : new int[] { Calendar.HOUR_OF_DAY, Calendar.DATE }) {
            assertEquals("1942-01-29T01:30+09:00[Asia/Pontianak]",
                    Instant.ofEpochMilli(Dates.ceiling(new Date(millis), field).getTime()).atZone(zone).toString(), "field=" + field);
        }
    }

    /**
     * Review item 2: a time zone java.time cannot express is rejected outright rather than delegated to
     * arithmetic known to break these invariants. This is the requirement {@code parse}, {@code format}
     * and {@code isSameDay} already impose.
     */
    @Test
    public void roundTruncateCeiling_rejectAZoneJavaTimeCannotRepresent() {
        final TimeZone custom = new java.util.SimpleTimeZone(3_600_000, "CustomZone", Calendar.MARCH, -1, Calendar.SUNDAY, 3_600_000, Calendar.OCTOBER, -1,
                Calendar.SUNDAY, 3_600_000, 3_600_000);
        final Calendar calendar = Calendar.getInstance(custom);
        calendar.setTimeInMillis(1736937045123L);

        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(calendar, Calendar.DATE));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling(calendar, Calendar.HOUR_OF_DAY));
        assertThrows(IllegalArgumentException.class, () -> Dates.round(calendar, Calendar.MONTH));

        TimeZone.setDefault(custom);
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(new Date(0L), Calendar.DATE));
    }

    /**
     * Review item 2: a calendar carrying a customized Julian/Gregorian cutover - including one in the
     * future, which used to send every present-day value down the broken path - now uses the same engine.
     */
    @Test
    public void roundTruncateCeiling_useTheIsoEngineForACustomCutover() {
        final GregorianCalendar future = new GregorianCalendar(TimeZone.getTimeZone("Europe/Berlin"));
        future.setGregorianChange(new Date(4102444800000L)); // 2100-01-01
        future.setTimeInMillis(OffsetDateTime.parse("2020-03-29T23:18:55.076+02:00").toInstant().toEpochMilli());

        final long ceiled = Dates.ceiling(future, Calendar.AM_PM).getTimeInMillis();

        assertTrue(ceiled >= future.getTimeInMillis());
        assertEquals("2020-03-30T00:00+02:00[Europe/Berlin]", Instant.ofEpochMilli(ceiled).atZone(ZoneId.of("Europe/Berlin")).toString());
    }

    // ---------------------------------------------------------------------------------------------
    // Review 2026-08-29 / B3: setYears and setMonths clamp the day-of-month instead of rolling over.
    // ---------------------------------------------------------------------------------------------

    /** B3: 29 February used to roll silently into 1 March while setMonths rejected the same overflow. */
    @Test
    public void setYearsAndSetMonths_clampTheDayOfMonth() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        final Date leapDay = Dates.parseToJUDate("2024-02-29 12:00:00", "yyyy-MM-dd HH:mm:ss");
        assertEquals("2025-02-28 12:00:00", Dates.format(Dates.setYears(leapDay, 2025), "yyyy-MM-dd HH:mm:ss"));
        assertEquals("2028-02-29 12:00:00", Dates.format(Dates.setYears(leapDay, 2028), "yyyy-MM-dd HH:mm:ss"));

        final Date endOfJanuary = Dates.parseToJUDate("2024-01-31 10:30:45", "yyyy-MM-dd HH:mm:ss");
        assertEquals("2024-02-29 10:30:45", Dates.format(Dates.setMonths(endOfJanuary, Calendar.FEBRUARY), "yyyy-MM-dd HH:mm:ss"));
        assertEquals("2024-04-30 10:30:45", Dates.format(Dates.setMonths(endOfJanuary, Calendar.APRIL), "yyyy-MM-dd HH:mm:ss"));

        // a day the target month does have is untouched
        assertEquals("2026-03-15 01:02:03",
                Dates.format(Dates.setYears(Dates.parseToJUDate("2025-03-15 01:02:03", "yyyy-MM-dd HH:mm:ss"), 2026), "yyyy-MM-dd HH:mm:ss"));

        // setYears takes a proleptic ISO year, so 0 (1 BCE) and negatives are in range - see
        // DatesReviewFixes20260831bTest.setYears_takesAProlepticIsoYearOnBothSidesOfTheEra.
        assertEquals("0001-01-31 10:30:45 BC", Dates.format(Dates.setYears(endOfJanuary, 0), "yyyy-MM-dd HH:mm:ss G"));

        // an out-of-range value for the field itself is still rejected, for every set* method
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(endOfJanuary, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(endOfJanuary, 12));
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(Dates.parseToJUDate("2024-02-05 10:00:00", "yyyy-MM-dd HH:mm:ss"), 31));
        assertThrows(IllegalArgumentException.class, () -> Dates.setHours(endOfJanuary, 25));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMilliseconds(endOfJanuary, 1500));

        // a Timestamp keeps its sub-millisecond fraction through the clamp
        assertEquals(123456789, Dates.setYears(java.sql.Timestamp.valueOf("2024-02-29 12:00:00.123456789"), 2025).getNanos());
    }

    /**
     * B3 follow-up: an explicit day-of-month is rejected, not clamped - including an in-range day the
     * current month does not have, which is the case the reworded @throws first left out.
     */
    @Test
    public void setDays_rejectsADayTheMonthDoesNotHave() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final Date february = Dates.parseToJUDate("2024-02-05 10:00:00", "yyyy-MM-dd HH:mm:ss");

        assertEquals("2024-02-29 10:00:00", Dates.format(Dates.setDays(february, 29), "yyyy-MM-dd HH:mm:ss"));
        // 30 and 31 are inside DAY_OF_MONTH's range but not inside February
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(february, 30));
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(february, 31));
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(february, 0));
    }

    /**
     * B3 follow-up: the clamp carries the time of day over, except where the target date has no such
     * wall-clock time. Sao Paulo started DST at midnight, so 4 November 2018 has no 00:30.
     */
    @Test
    public void setYearsAndSetMonths_resolveForwardAcrossAMidnightDstGap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));

        final Date december = Dates.parseToJUDate("2018-12-04 00:30:00", "yyyy-MM-dd HH:mm:ss");
        assertEquals("2018-11-04 01:30:00", Dates.format(Dates.setMonths(december, Calendar.NOVEMBER), "yyyy-MM-dd HH:mm:ss"));

        final Date nextYear = Dates.parseToJUDate("2019-11-04 00:30:00", "yyyy-MM-dd HH:mm:ss");
        assertEquals("2018-11-04 01:30:00", Dates.format(Dates.setYears(nextYear, 2018), "yyyy-MM-dd HH:mm:ss"));

        // away from a transition the time of day is untouched
        assertEquals("2018-10-04 00:30:00", Dates.format(Dates.setMonths(december, Calendar.OCTOBER), "yyyy-MM-dd HH:mm:ss"));
    }

    // ---------------------------------------------------------------------------------------------
    // Review 2026-08-29 / B2: the Calendar add* family must work on the calendar the JVM hands out.
    // ---------------------------------------------------------------------------------------------

    /** B2: Calendar.getInstance() is not a GregorianCalendar under every default locale. */
    @Test
    public void addToCalendar_worksForNonGregorianDefaultCalendars() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        for (final String languageTag : new String[] { "ja-JP-u-ca-japanese", "th-TH-u-ca-buddhist" }) {
            Locale.setDefault(Locale.forLanguageTag(languageTag));

            final Calendar calendar = Dates.currentCalendar();
            final long base = calendar.getTimeInMillis();

            final Calendar nextDay = Dates.addDays(calendar, 1);
            assertEquals(calendar.getClass(), nextDay.getClass(), languageTag);
            assertEquals(base + 86_400_000L, nextDay.getTimeInMillis(), languageTag);
            assertEquals(base + 5L, Dates.addMilliseconds(calendar, 5).getTimeInMillis(), languageTag);
            assertTrue(Dates.addMonths(calendar, 1).getTimeInMillis() > base, languageTag);
            assertEquals(base, calendar.getTimeInMillis(), languageTag + ": the source must not be modified");
        }
    }

    /** B2: a subclass with neither a (long) nor a no-arg constructor is built by clone(). */
    @Test
    public void addToCalendar_worksForASubclassWithoutAUsableConstructor() {
        final ZoneOnlyCalendar source = new ZoneOnlyCalendar(TimeZone.getTimeZone("UTC"));
        source.setTimeInMillis(0L);

        final Calendar result = Dates.addDays(source, 1);

        assertEquals(ZoneOnlyCalendar.class, result.getClass());
        assertEquals(86_400_000L, result.getTimeInMillis());
        assertEquals(0L, source.getTimeInMillis());
    }

    /**
     * Review item 6: only a reflective/access failure falls back to clone(). An exception thrown by the
     * constructor body is a defect in that class and must reach the caller.
     */
    @Test
    public void addToCalendar_propagatesAnExceptionThrownByTheConstructor() {
        final RefusingCalendar calendar = new RefusingCalendar();
        calendar.setTimeInMillis(0L);

        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Dates.addDays(calendar, 1));
        assertTrue(e.getMessage().contains("refused"), e.getMessage());
    }

    public static class RefusingCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        public RefusingCalendar() {
            super();
        }

        public RefusingCalendar(final long millis) {
            throw new IllegalStateException("refused " + millis);
        }
    }

    private static final class ZoneOnlyCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        ZoneOnlyCalendar(final TimeZone timeZone) {
            super(timeZone);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Review 2026-08-29 / D4 and D6.
    // ---------------------------------------------------------------------------------------------

    /** D4: the zoned default format writes the offset, so it identifies an instant and parses back. */
    @Test
    public void defaultFormatWithAnExplicitZone_roundTrips() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");

        final java.sql.Timestamp timestamp = new java.sql.Timestamp(1736937045123L);
        assertEquals("2025-01-15T16:00:45.123+05:30", Dates.format(timestamp, null, kolkata));
        assertEquals(1736937045123L, Dates.parseToTimestamp(Dates.format(timestamp, null, kolkata)).getTime());

        final Date utilDate = new Date(1736937045000L);
        assertEquals("2025-01-15T16:00:45+05:30", Dates.format(utilDate, null, kolkata));
        assertEquals(1736937045000L, Dates.parseToJUDate(Dates.format(utilDate, null, kolkata)).getTime());

        // a plain java.util.Date default is second-precision, in the zoned form exactly as in the UTC
        // one, so a millisecond fraction is dropped by both; ISO_OFFSET_TIMESTAMP_FORMAT keeps it
        final Date withMillis = new Date(1736937045123L);
        assertEquals("2025-01-15T16:00:45+05:30", Dates.format(withMillis, null, kolkata));
        assertEquals("2025-01-15T10:30:45Z", Dates.format(withMillis));
        assertEquals(1736937045000L, Dates.parseToJUDate(Dates.format(withMillis, null, kolkata)).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate(Dates.format(withMillis, Dates.ISO_OFFSET_TIMESTAMP_FORMAT, kolkata)).getTime());

        // both defaults now use the same separator, and the UTC default is unchanged
        assertEquals("2025-01-15T10:30:45.123Z", Dates.format(timestamp));
        assertEquals("2025-01-15T10:30:45Z", Dates.format(utilDate));

        // the new constant accepts the basic offset form too, and rejects a missing or oversized offset
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T16:00:45.123+0530", Dates.ISO_OFFSET_TIMESTAMP_FORMAT).getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T16:00:45.123", Dates.ISO_OFFSET_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T16:00:45.123+19:00", Dates.ISO_OFFSET_TIMESTAMP_FORMAT));

        // the offset-bearing default cannot express a sub-minute offset; an explicit zone-less pattern still can
        final TimeZone subMinute = new java.util.SimpleTimeZone(30_000, "SubMinute");
        assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(0L), null, subMinute));
        assertEquals("1970-01-01 00:00:30.000", Dates.format(new Date(0L), Dates.LOCAL_TIMESTAMP_FORMAT, subMinute));
    }

    /**
     * J3 follow-up: truncatedEquals compares truncated INSTANTS, so two calendars at the same instant in
     * different zones differ even when both show the same civil day. The first wording of this caveat
     * illustrated the wrong phenomenon.
     */
    @Test
    public void truncatedEquals_truncatesEachCalendarInItsOwnZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final long instant = 1736937045123L; // 2025-01-15T10:30:45.123Z, and 16:00:45.123 in Kolkata

        final Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(instant);
        final Calendar kolkata = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        kolkata.setTimeInMillis(instant);

        // both render the same civil day...
        assertEquals(LocalDate.of(2025, 1, 15), Instant.ofEpochMilli(instant).atZone(ZoneId.of("UTC")).toLocalDate());
        assertEquals(LocalDate.of(2025, 1, 15), Instant.ofEpochMilli(instant).atZone(ZoneId.of("Asia/Kolkata")).toLocalDate());
        // ...but each truncates to its own midnight, which is a different instant
        assertFalse(Dates.truncatedEquals(utc, kolkata, Calendar.DATE));
        assertTrue(Dates.truncate(utc, Calendar.DATE).getTimeInMillis() != Dates.truncate(kolkata, Calendar.DATE).getTimeInMillis());

        // in one zone the comparison behaves as expected
        final Calendar alsoUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        alsoUtc.setTimeInMillis(instant + 3_600_000L);
        assertTrue(Dates.truncatedEquals(utc, alsoUtc, Calendar.DATE));
    }

    // ---------------------------------------------------------------------------------------------
    // Review 2026-08-29 / B4: the Date-taking fragment helpers read Gregorian fields whatever the
    // default locale's calendar system is.
    // ---------------------------------------------------------------------------------------------

    /**
     * B4: 2019-05-01 is the first day of the Reiwa era, so a JapaneseImperialCalendar restarts
     * DAY_OF_YEAR there. The Date overloads must still report the Gregorian day of the year.
     */
    @Test
    public void getFragmentOfADate_ignoresTheDefaultLocalesCalendarSystem() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        final java.util.Date reiwaDay1 = new java.util.Date(LocalDateTime.of(2019, 5, 1, 6, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli());

        for (final String languageTag : new String[] { "en-US", "ja-JP-u-ca-japanese", "th-TH-u-ca-buddhist" }) {
            Locale.setDefault(Locale.forLanguageTag(languageTag));

            assertEquals(121, Dates.getFragmentInDays(reiwaDay1, CalendarField.YEAR), languageTag);
            assertEquals(2886, Dates.getFragmentInHours(reiwaDay1, CalendarField.YEAR), languageTag);
            assertEquals(1, Dates.getFragmentInDays(reiwaDay1, CalendarField.MONTH), languageTag);
        }
    }

    /** B4: the Calendar overloads deliberately diverge - they read the calendar they were handed. */
    @Test
    public void getFragmentOfACalendar_keepsTheCallersCalendarSystem() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.forLanguageTag("ja-JP-u-ca-japanese"));

        final long reiwaDay1 = LocalDateTime.of(2019, 5, 1, 6, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();

        final Calendar japanese = Calendar.getInstance();
        japanese.setTimeInMillis(reiwaDay1);
        assertEquals(1, Dates.getFragmentInDays(japanese, CalendarField.YEAR));

        final Calendar gregorian = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gregorian.setTimeInMillis(reiwaDay1);
        assertEquals(121, Dates.getFragmentInDays(gregorian, CalendarField.YEAR));
    }

    // ---------------------------------------------------------------------------------------------
    // Review 2026-08-29 / B5: the default-format fast path must give its pooled buffer back even when
    // it throws.
    // ---------------------------------------------------------------------------------------------

    /**
     * B5: the pool is a bounded FIFO, so draining it and then recycling a single marker makes the next
     * acquisition deterministic - the marker comes back only if the throwing call returned it.
     */
    @Test
    public void format_returnsItsPooledBufferWhenTheYearIsOutOfIsoRange() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        final java.util.Date outOfRange = new java.util.Date(253_402_300_800_000L); // +10000-01-01T00:00:00Z

        assertThrows(IllegalArgumentException.class, () -> Dates.format(outOfRange));

        // The pool is shared with every other test in this JVM, so take a note of everything drained and
        // hand it all back - and read the capacity rather than restating it, so the drain stays complete
        // if it ever changes.
        final int capacity = privateStaticInt(Objectory.class, "POOL_SIZE_FOR_BUFFER");
        final List<StringBuilder> drained = new ArrayList<>();

        try {
            for (int i = 0; i < capacity; i++) {
                drained.add(Objectory.createStringBuilder());
            }

            // Ownership stays linear: the marker belongs to the pool from here until it comes back out,
            // and only what comes back out is recycled below. Recycling the same instance twice would
            // offer it to the pool twice and hand one buffer to two callers.
            final StringBuilder marker = Objectory.createStringBuilder();
            Objectory.recycle(marker);

            assertThrows(IllegalArgumentException.class, () -> Dates.format(outOfRange));

            final StringBuilder returned = Objectory.createStringBuilder();
            drained.add(returned);

            assertSame(marker, returned, "the throwing format(..) leaked its pooled StringBuilder");
        } finally {
            for (final StringBuilder sb : drained) {
                Objectory.recycle(sb);
            }
        }
    }

    private static int privateStaticInt(final Class<?> owner, final String field) {
        try {
            final java.lang.reflect.Field f = owner.getDeclaredField(field);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("cannot read " + owner.getSimpleName() + "." + field, e);
        }
    }

    /**
     * D7: {@code ISO_LOCAL_TIMESTAMP_FORMAT} was promoted from a private constant to public API, so its
     * documented contract needs pinning: exactly three fraction digits when supplied explicitly, and the
     * shape auto-detection returns for T-separated timestamp text with no zone designator.
     */
    @Test
    public void isoLocalTimestampFormat_behavesAsDocumented() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        assertEquals("yyyy-MM-dd'T'HH:mm:ss.SSS", Dates.ISO_LOCAL_TIMESTAMP_FORMAT);

        final java.sql.Timestamp value = Dates.parseToTimestamp("2023-12-25T14:30:45.123", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc);
        assertEquals("2023-12-25T14:30:45.123", Dates.format(value, Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc));

        // supplied explicitly, the fraction is exactly three digits
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2023-12-25T14:30:45.1", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2023-12-25T14:30:45.123456", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, utc));

        // auto-detection reads the same shape with a 1-9 digit fraction of a second
        assertEquals(100_000_000, Dates.parseToTimestamp("2023-12-25T14:30:45.1", null, utc).getNanos());
        assertEquals(123_456_789, Dates.parseToTimestamp("2023-12-25T14:30:45.123456789", null, utc).getNanos());
    }

    /** D6: DTF is a value, and a repeated pattern reuses one instance. */
    @Test
    public void dtf_hasValueIdentityAndIsCached() {
        assertEquals(Dates.DTF.of("dd/MM/yyyy"), Dates.DTF.of("dd/MM/yyyy"));
        assertEquals(Dates.DTF.of("dd/MM/yyyy").hashCode(), Dates.DTF.of("dd/MM/yyyy").hashCode());

        // The pattern cache never evicts and stops admitting at its bound, and whether this JVM has already reached
        // it depends on what ran before (dtf_cacheBoundHoldsUnderConcurrency fills it deliberately; DatesTest alone
        // uses well over a hundred patterns), so instance identity is asserted only while a fresh pattern is still
        // admitted - otherwise this test failed or passed with the order the classes happened to run in.
        if (Dates.DTF.of("'d6-probe'yyyy") == Dates.DTF.of("'d6-probe'yyyy")) {
            assertSame(Dates.DTF.of("dd/MM/yyyy"), Dates.DTF.of("dd/MM/yyyy"));
        }

        assertSame(Dates.DTF.LOCAL_DATE, Dates.DTF.of("uuuu-MM-dd"));

        assertFalse(Dates.DTF.of("dd/MM/yyyy").equals(Dates.DTF.of("yyyy/MM/dd")));
        assertFalse(Dates.DTF.of("dd MMM yyyy", Locale.FRENCH).equals(Dates.DTF.of("dd MMM yyyy", Locale.GERMAN)));
        assertEquals("dd/MM/yyyy", Dates.DTF.of("dd/MM/yyyy").toString());
    }

    /**
     * D6 follow-up: the cache bound is enforced atomically. Sizing and inserting as separate steps let
     * concurrent callers push it past the limit.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void dtf_cacheBoundHoldsUnderConcurrency() throws Exception {
        final java.lang.reflect.Field cacheField = Dates.DTF.class.getDeclaredField("patternCache");
        cacheField.setAccessible(true);
        final Map<Object, Object> cache = (Map<Object, Object>) cacheField.get(null);

        // This test deliberately fills a cache that never evicts, so every later caller in this JVM would
        // be refused admission - and dtf_hasValueIdentityAndIsCached asserts assertSame on a freshly
        // cached pattern. Snapshot now, restore in the finally, and the pollution stays local.
        final Map<Object, Object> before = new java.util.LinkedHashMap<>(cache);

        final int threads = 32;
        final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(threads);

        try {
            final List<java.util.concurrent.Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < threads; t++) {
                final int offset = t;
                futures.add(pool.submit(() -> {
                    start.await();

                    for (int i = 0; i < 400; i++) {
                        Dates.DTF.of("'p" + ((offset * 400 + i) % 700) + "'yyyy-MM-dd");
                    }

                    return null;
                }));
            }

            start.countDown();

            for (final java.util.concurrent.Future<?> f : futures) {
                f.get();
            }

            // 700 distinct patterns were raced through a cache that admits at most MAX_CACHED_PATTERNS,
            // so the bound must be exactly reached and never exceeded. Asserting the size is the point:
            // the backing ConcurrentCacheMap does not evict - its constructor argument is only a sizing
            // hint for the underlying ConcurrentHashMap - so the synchronized admission block is the ONLY
            // thing holding this bound, and an equals-only assertion would pass with no bound at all.
            assertEquals(privateStaticInt(Dates.DTF.class, "MAX_CACHED_PATTERNS"), cache.size(), "the pattern cache overran its bound");

            // and the cache stays a cache: an equal pattern still yields an equal formatter
            assertEquals(Dates.DTF.of("'p1'yyyy-MM-dd"), Dates.DTF.of("'p1'yyyy-MM-dd"));
        } finally {
            pool.shutdownNow();

            synchronized (cache) {
                cache.clear();
                cache.putAll(before);
            }
        }
    }

    /** D8: the LocalDateTime counterpart of dateAt/timeAt. */
    @Test
    public void dateTimeAt_returnsTheCivilDateTimeInTheGivenZone() {
        final Instant instant = Instant.ofEpochMilli(1736937045000L);

        assertEquals(java.time.LocalDateTime.parse("2025-01-15T10:30:45"), Dates.dateTimeAt(instant, ZoneId.of("UTC")));
        assertEquals(java.time.LocalDateTime.parse("2025-01-15T16:00:45"), Dates.dateTimeAt(instant, ZoneId.of("Asia/Kolkata")));
        assertEquals(Dates.dateAt(instant, ZoneId.of("Asia/Kolkata")), Dates.dateTimeAt(instant, ZoneId.of("Asia/Kolkata")).toLocalDate());
        assertEquals(Dates.timeAt(instant, ZoneId.of("Asia/Kolkata")), Dates.dateTimeAt(instant, ZoneId.of("Asia/Kolkata")).toLocalTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.dateTimeAt(null, ZoneId.of("UTC")));
        assertThrows(IllegalArgumentException.class, () -> Dates.dateTimeAt(instant, null));
    }
}
