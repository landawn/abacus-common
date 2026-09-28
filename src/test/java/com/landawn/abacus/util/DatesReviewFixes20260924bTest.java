package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dates.DTF;

/**
 * Pins the behaviour changed by cycle 1 of the 2026-09-23 iterative review of {@code Dates}/{@code Numbers}
 * (ledger {@code scripts/cross_review/Dates_Numbers_ledger_2026-09-23.md}), coordinator-owned items:
 * <ul>
 *   <li><b>C-002</b> a two-digit {@code yy} year parsed with a legacy pattern under a locale whose calendar
 *       is not Gregorian (Thai-Buddhist, Japanese-imperial) is anchored on the proleptic Gregorian calendar
 *       the formatter was switched to, not the century the locale's own calendar computed;</li>
 *   <li><b>C-003</b> a standard/daylight zone name written the way the pattern's {@code z} prints it
 *       disambiguates a daylight-saving overlap on the {@code DTF} and static {@code java.time} parsers, as it
 *       already did on the legacy parsers; generic names, region IDs and gaps stay rejected;</li>
 *   <li><b>C-004</b> the {@code add*}/{@code set*} field arithmetic resolves a pre-1900 source through the
 *       zone's real rules, so the wall clock is carried over exactly as {@code ZonedDateTime} does even across a
 *       local-mean-time transition;</li>
 *   <li><b>C-007</b> a whole-second fixed-offset default zone is read as that offset by the zone-less
 *       civil-field queries whatever its ID, consistent with every other operation.</li>
 * </ul>
 */
public class DatesReviewFixes20260924bTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone NY = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone KOLKATA = TimeZone.getTimeZone("Asia/Kolkata");
    private static final Locale THAI = Locale.forLanguageTag("th-TH");
    private static final Locale JAPANESE_IMPERIAL = Locale.forLanguageTag("ja-JP-u-ca-japanese");

    private TimeZone defaultZone;

    @BeforeEach
    public void saveDefaultZone() {
        defaultZone = TimeZone.getDefault();
    }

    @AfterEach
    public void restoreDefaultZone() {
        TimeZone.setDefault(defaultZone);
    }

    private static String iso(final java.util.Date date) {
        return Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, UTC);
    }

    private static String local(final java.util.Date date, final TimeZone zone) {
        return Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT, zone);
    }

    private static java.util.Date at(final String localDateTime, final TimeZone zone) {
        return Dates.parseToJUDate(localDateTime, Dates.LOCAL_DATE_TIME_FORMAT, zone);
    }

    // ----------------------------------------------------------------------------------------------------
    // C-002
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c002_twoDigitYearUnderANonGregorianLocaleIsAnchoredOnTheGregorianCalendar() {
        assertEquals("2025-01-15T10:30:45Z", iso(Dates.parseToJUDate("25-01-15 10:30:45", "yy-MM-dd HH:mm:ss", UTC, THAI)));
        assertEquals("1999-01-15T00:00:00Z", iso(Dates.parseToJUDate("99-01-15", "yy-MM-dd", UTC, THAI)));
        assertEquals("2025-01-15T00:00:00Z", iso(Dates.parseToJUDate("25-01-15", "yy-MM-dd", UTC, JAPANESE_IMPERIAL)));
        assertEquals("1999-01-15T00:00:00Z", iso(Dates.parseToJUDate("99-01-15", "yy-MM-dd", UTC, JAPANESE_IMPERIAL)));
        assertEquals("2025-01-15T00:00:00Z", iso(Dates.parseToJUDate("25-01-15", "yy-MM-dd", UTC, Locale.US)));

        // Every legacy target and the pooled formatter (second call) agree.
        assertEquals("2025-01-15T10:30:45Z", iso(Dates.parseToTimestamp("25-01-15 10:30:45", "yy-MM-dd HH:mm:ss", UTC, THAI)));
        assertEquals(2025, Dates.parseToCalendar("25-01-15", "yy-MM-dd", UTC, THAI).get(Calendar.YEAR));
        assertEquals("2025-01-15T10:30:45Z", iso(Dates.parseToJUDate("25-01-15 10:30:45", "yy-MM-dd HH:mm:ss", UTC, THAI)));
    }

    @Test
    public void c002_twoDigitYearRoundTripsAndMatchesDtf() {
        final java.util.Date date = at("2025-01-15 00:00:00", UTC);
        final String text = Dates.format(date, "yy-MM-dd", UTC, THAI);

        assertEquals("25-01-15", text);
        assertEquals(date, Dates.parseToJUDate(text, "yy-MM-dd", UTC, THAI));
        assertEquals(DTF.of("yy-MM-dd", THAI).parseToLocalDate("25-01-15"), Dates.parseToLocalDate("25-01-15", "yy-MM-dd"));

        // Four-digit years were never affected.
        assertEquals("2025-01-15T00:00:00Z", iso(Dates.parseToJUDate("2025-01-15", "yyyy-MM-dd", UTC, THAI)));
        assertEquals("2025-01-15T00:00:00Z", iso(Dates.parseToJUDate("2025-01-15", "yyyy-MM-dd", UTC, JAPANESE_IMPERIAL)));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-003
    // ----------------------------------------------------------------------------------------------------

    private static final long OVERLAP_STANDARD = 1699165800000L; // 2023-11-05T01:30-05:00[America/New_York]
    private static final long OVERLAP_DAYLIGHT = 1699162200000L; // 2023-11-05T01:30-04:00[America/New_York]

    @Test
    public void c003_zoneNameDisambiguatesAnOverlapOnTheDtfParsers() {
        final DTF dtf = DTF.of("uuuu-MM-dd HH:mm:ss z");

        assertEquals(ZoneOffset.ofHours(-5), dtf.parseToZonedDateTime("2023-11-05 01:30:00 EST").getOffset());
        assertEquals(ZoneOffset.ofHours(-4), dtf.parseToZonedDateTime("2023-11-05 01:30:00 EDT").getOffset());
        assertEquals(ZoneId.of("America/New_York"), dtf.parseToZonedDateTime("2023-11-05 01:30:00 EST").getZone());
        assertEquals(OVERLAP_STANDARD, dtf.parseToInstant("2023-11-05 01:30:00 EST").toEpochMilli());
        assertEquals(OVERLAP_DAYLIGHT, dtf.parseToInstant("2023-11-05 01:30:00 EDT").toEpochMilli());
        assertEquals(OVERLAP_STANDARD, dtf.parseToJUDate("2023-11-05 01:30:00 EST").getTime());
        assertEquals(OVERLAP_DAYLIGHT, dtf.parseToTimestamp("2023-11-05 01:30:00 EDT").getTime());
        assertEquals(OVERLAP_STANDARD, dtf.parseToCalendar("2023-11-05 01:30:00 EST").getTimeInMillis());
        assertEquals(ZoneOffset.ofHours(-5), dtf.parseToOffsetDateTime("2023-11-05 01:30:00 EST").getOffset());

        final DTF full = DTF.of("uuuu-MM-dd HH:mm:ss zzzz");
        assertEquals(OVERLAP_STANDARD, full.parseToInstant("2023-11-05 01:30:00 Eastern Standard Time").toEpochMilli());
        assertEquals(OVERLAP_DAYLIGHT, full.parseToInstant("2023-11-05 01:30:00 Eastern Daylight Time").toEpochMilli());

        // Same instants the legacy engine has always produced for the same text and pattern.
        assertEquals(OVERLAP_STANDARD, Dates.parseToJUDate("2023-11-05 01:30:00 EST", "yyyy-MM-dd HH:mm:ss z", NY).getTime());
        assertEquals(OVERLAP_DAYLIGHT, Dates.parseToJUDate("2023-11-05 01:30:00 EDT", "yyyy-MM-dd HH:mm:ss z", NY).getTime());
    }

    @Test
    public void c003_zoneNameDisambiguatesAnOverlapOnTheStaticJavaTimeParsers() {
        assertEquals(Instant.ofEpochMilli(OVERLAP_STANDARD), Dates.parseToInstant("2023-11-05 01:30:00 EST", "yyyy-MM-dd HH:mm:ss z"));
        assertEquals(Instant.ofEpochMilli(OVERLAP_DAYLIGHT), Dates.parseToInstant("2023-11-05 01:30:00 EDT", "yyyy-MM-dd HH:mm:ss z", KOLKATA));
        assertEquals(ZoneOffset.ofHours(-5), Dates.parseToZonedDateTime("2023-11-05 01:30:00 EST", "yyyy-MM-dd HH:mm:ss z").getOffset());
        assertEquals(ZoneOffset.ofHours(-4), Dates.parseToOffsetDateTime("2023-11-05 01:30:00 EDT", "yyyy-MM-dd HH:mm:ss z").getOffset());
    }

    @Test
    public void c003_namesThatDoNotDisambiguateStayRejected() {
        final DTF dtf = DTF.of("uuuu-MM-dd HH:mm:ss z");

        // A generic name under z, a generic-name letter, and a region ID name no occurrence.
        assertAmbiguous(() -> dtf.parseToZonedDateTime("2023-11-05 01:30:00 ET"));
        assertAmbiguous(() -> DTF.of("uuuu-MM-dd HH:mm:ss zzzz").parseToZonedDateTime("2023-11-05 01:30:00 Eastern Time"));
        assertAmbiguous(() -> DTF.of("uuuu-MM-dd HH:mm:ss v").parseToZonedDateTime("2023-11-05 01:30:00 ET"));
        assertAmbiguous(() -> DTF.of("uuuu-MM-dd HH:mm:ss VV").parseToZonedDateTime("2023-11-05 01:30:00 America/New_York"));
        assertAmbiguous(() -> dtf.parseToZonedDateTime("2023-11-05 01:30:00 America/New_York"));

        // Zone-less text in the overlap, and a quoted z literal, are unchanged.
        assertAmbiguous(() -> DTF.of("uuuu-MM-dd HH:mm:ss").parseToZonedDateTime("2023-11-05 01:30:00", NY));
        assertAmbiguous(() -> DTF.of("uuuu-MM-dd HH:mm:ss 'z'").parseToZonedDateTime("2023-11-05 01:30:00 z", NY));

        // A daylight name in the spring-forward gap does not create the missing wall clock.
        final IllegalArgumentException gap = assertThrows(IllegalArgumentException.class, () -> dtf.parseToZonedDateTime("2023-03-12 02:30:00 EDT"));
        assertTrue(gap.getMessage().contains("Nonexistent"), gap.getMessage());
    }

    @Test
    public void c003_unambiguousTextWithAZoneNameIsUnchanged() {
        final DTF dtf = DTF.of("uuuu-MM-dd HH:mm:ss z");

        assertEquals(ZonedDateTime.of(2023, 7, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), dtf.parseToZonedDateTime("2023-07-01 12:00:00 EDT"));
        assertEquals(ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), dtf.parseToZonedDateTime("2023-01-01 12:00:00 EST"));
        assertEquals(ZoneOffset.UTC, dtf.parseToZonedDateTime("2023-07-01 12:00:00 UTC").getOffset());
        assertThrows(IllegalArgumentException.class, () -> dtf.parseToZonedDateTime(""));
        assertEquals(null, dtf.parseToZonedDateTime((CharSequence) null));
    }

    private static void assertAmbiguous(final org.junit.jupiter.api.function.Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertTrue(e.getMessage().contains("Ambiguous"), e.getMessage());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-004
    // ----------------------------------------------------------------------------------------------------

    // The java.util.Date overloads of add*/set* evaluate in the live default zone, so each case sets it.

    @Test
    public void c004_preNineteenHundredArithmeticPreservesTheWallClockAcrossALocalMeanTimeTransition() {
        // New York left local mean time (-04:56:02) for -05:00 on 1883-11-18; Kolkata kept +05:21:10 until 1906;
        // Berlin left +00:53:28 in 1893.
        TimeZone.setDefault(NY);
        assertEquals("1890-06-15 12:00:00", local(Dates.addYears(at("1880-06-15 12:00:00", NY), 10), NY));
        assertEquals("1883-11-19 12:00:00", local(Dates.addDays(at("1883-11-17 12:00:00", NY), 2), NY));
        assertEquals("1883-11-18 12:00:00", local(Dates.addDays(at("1883-11-17 12:00:00", NY), 1), NY));

        // The result is exactly what ZonedDateTime computes.
        final ZonedDateTime expected = ZonedDateTime.of(1880, 6, 15, 12, 0, 0, 0, ZoneId.of("America/New_York")).plusYears(10);
        assertEquals(expected.toInstant().toEpochMilli(), Dates.addYears(at("1880-06-15 12:00:00", NY), 10).getTime());

        TimeZone.setDefault(KOLKATA);
        assertEquals("1880-06-15 12:00:00", local(Dates.addYears(at("1860-06-15 12:00:00", KOLKATA), 20), KOLKATA));
        assertEquals("2025-06-15 12:00:00", local(Dates.addYears(at("1850-06-15 12:00:00", KOLKATA), 175), KOLKATA));

        final TimeZone berlin = TimeZone.getTimeZone("Europe/Berlin");
        TimeZone.setDefault(berlin);
        assertEquals("1895-06-15 12:00:00", local(Dates.addYears(at("1890-06-15 12:00:00", berlin), 5), berlin));
    }

    @Test
    public void c004_preNineteenHundredArithmeticIsSymmetric() {
        TimeZone.setDefault(NY);
        final java.util.Date source = at("1905-06-15 12:00:00", NY);

        assertEquals(source, Dates.addYears(Dates.addYears(source, -25), 25));
        assertEquals("1880-06-15 12:00:00", local(Dates.addYears(source, -25), NY));
        assertEquals(source, Dates.addMonths(Dates.addMonths(source, -300), 300));
    }

    @Test
    public void c004_settersKeepTheWrittenFieldAndTheWallClockBeforeNineteenHundred() {
        TimeZone.setDefault(KOLKATA);
        assertEquals("2025-06-15 12:00:00", local(Dates.setYears(at("1850-06-15 12:00:00", KOLKATA), 2025), KOLKATA));
        assertEquals("2025-12-31 23:55:00", local(Dates.setYears(at("1899-12-31 23:55:00", KOLKATA), 2025), KOLKATA));
        assertEquals("2025-06-15 23:50:00", local(Dates.setYears(at("1850-06-15 23:50:00", KOLKATA), 2025), KOLKATA));
        assertEquals("1850-06-15 12:00:00", local(Dates.setYears(at("2025-06-15 12:00:00", KOLKATA), 1850), KOLKATA));

        TimeZone.setDefault(NY);
        assertEquals("2025-06-15 12:00:00", local(Dates.setYears(at("1850-06-15 12:00:00", NY), 2025), NY));
        assertEquals("1883-11-19 00:01:00", local(Dates.setDays(at("1883-11-18 00:01:00", NY), 19), NY));
    }

    @Test
    public void c004_calendarOverloadsAgreeAndLaterInstantsAreUnchanged() {
        // The Calendar overloads evaluate in the calendar's own zone, whatever the default.
        final Calendar source = Dates.createCalendar(at("1880-06-15 12:00:00", NY).getTime(), NY);

        assertEquals("1890-06-15 12:00:00", local(Dates.addYears(source, 10).getTime(), NY));
        assertEquals("1890-06-15 12:00:00", local(Dates.addMonths(source, 120).getTime(), NY));

        // From 1900 on the two rule tables agree, so nothing changes there.
        TimeZone.setDefault(NY);
        assertEquals("2010-06-15 12:00:00", local(Dates.addYears(at("2000-06-15 12:00:00", NY), 10), NY));
        TimeZone.setDefault(KOLKATA);
        assertEquals("2025-06-15 12:00:00", local(Dates.setYears(at("1920-06-15 12:00:00", KOLKATA), 2025), KOLKATA));
        assertEquals("1904-06-15 12:00:00", local(Dates.addYears(at("1899-06-15 12:00:00", KOLKATA), 5), KOLKATA));
    }

    @Test
    public void c004_zoneNoZoneIdCanExpressStillTakesTheLegacyEngine() {
        final SimpleTimeZone custom = new SimpleTimeZone(-5 * 3_600_000, "Custom/Rules", Calendar.APRIL, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0);
        TimeZone.setDefault(custom);

        // 17:00Z is 13:00 under the custom rules (-05:00 standard, +1 h saving from April to October).
        final java.util.Date source = new java.util.Date(Instant.parse("1880-06-15T17:00:00Z").toEpochMilli());
        assertEquals("1880-06-15 13:00:00", local(source, custom));
        assertEquals(source.getTime() + 24L * 3_600_000, Dates.addDays(source, 1).getTime());
        assertEquals("1890-06-15 13:00:00", local(Dates.addYears(source, 10), custom));
        assertEquals("1890-06-15 13:00:00", local(Dates.setYears(source, 1890), custom));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-121 (cycle 3): a Timestamp copy, comparison or rendering at the epoch floor does not wrap
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c121_timestampCopyAndInstantAtTheEpochFloorDoNotWrap() {
        // The last representable instant: nanos beyond 807_999_999 would push getTime() past Long.MAX_VALUE.
        final java.sql.Timestamp ceiling = new java.sql.Timestamp(Long.MAX_VALUE);
        ceiling.setNanos(807_999_999);

        for (final long millis : new long[] { Long.MIN_VALUE, Long.MIN_VALUE + 807, Long.MIN_VALUE + 808, -1L, 0L, 1736937045123L, Long.MAX_VALUE - 807,
                Long.MAX_VALUE }) {
            final java.sql.Timestamp source = new java.sql.Timestamp(millis);
            source.setNanos(source.getNanos() + 123_456);

            final java.sql.Timestamp copy = Dates.createTimestamp(source);

            assertTrue(source != copy);
            assertEquals(millis, copy.getTime(), () -> "copy of " + millis);
            assertEquals(source.getNanos(), copy.getNanos());
            assertEquals(source, copy);
            assertTrue(Dates.isSameInstant(source, copy));
            assertTrue(Dates.isBetween(source, new java.util.Date(millis), ceiling));
            assertFalse(Dates.isSameInstant(source, new java.util.Date(millis)));
        }

        final java.sql.Timestamp floor = new java.sql.Timestamp(Long.MIN_VALUE);
        assertFalse(Dates.isBetween(floor, new java.util.Date(0L), new java.util.Date(Long.MAX_VALUE)));
        assertTrue(Dates.isBetween(floor, new java.util.Date(Long.MIN_VALUE), new java.util.Date(0L)));
        assertTrue(Dates.isSameInstant(floor, new java.util.Date(Long.MIN_VALUE)));
        assertEquals("-292275055-05-16T16:47:04.192Z", DTF.ISO_8601_TIMESTAMP.format(floor));
        assertEquals(Long.MIN_VALUE, Dates.createTimestamp(Long.MIN_VALUE).getTime());
        assertEquals(Long.MIN_VALUE, Dates.createTimestamp(new java.util.Date(Long.MIN_VALUE)).getTime());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-122 (cycle 3): one diagnostic per malformed auto-detected text on every legacy target
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c122_malformedAutoDetectedTextGetsOneDiagnosticOnEveryLegacyTarget() {
        TimeZone.setDefault(UTC);
        final java.util.List<org.junit.jupiter.api.function.ThrowingConsumer<String>> targets = java.util.List.of(Dates::parseToJUDate,
                Dates::parseToTimestamp, Dates::parseToCalendar, Dates::parseToXMLGregorianCalendar, Dates::parseToDate, Dates::parseToGregorianCalendar);

        for (final String text : new String[] { "2025-01x15T10:30:45.123", "2025-01x15T10:30:45.5", "2025-01x15T10:30:45.123Z", "2025-01x15T10:30:45.123456789Z",
                "2025-01-15T10:30x45.123", "2025-01-15T10:30x45.123Z" }) {
            for (final org.junit.jupiter.api.function.ThrowingConsumer<String> target : targets) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> target.accept(text), text);
                assertTrue(e.getMessage().contains("requires its exact canonical shape"), text + " -> " + e.getMessage());
                assertFalse(e.getMessage().contains("could not be parsed at index"), e.getMessage());
            }
        }

        for (final org.junit.jupiter.api.function.ThrowingConsumer<String> target : targets) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> target.accept("2025-01x15T10:30:45.123Z[UTC]"));
            assertTrue(e.getMessage().startsWith("Cannot parse \"2025-01x15T10:30:45.123Z[UTC]\" with the auto-detected format"), e.getMessage());
            assertTrue(e.getMessage().contains("Inconsistent date separators"), e.getMessage());
        }

        // Well-formed text is untouched.
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z").getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15T10:30:45.123Z").getTime());
        assertEquals(500_000_000, Dates.parseToTimestamp("2025-01-15T10:30:45.5").getNanos());
        assertEquals(123_456_789, Dates.parseToTimestamp("2025-01-15T10:30:45.123456789Z").getNanos());
        assertEquals(1736917245123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123+05:30[Asia/Kolkata]").getTime());
        assertEquals(1736917245123L, Dates.parseToCalendar("2025-01-15T10:30:45.123+05:30[Asia/Kolkata]").getTimeInMillis());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-123 (cycle 3): a fixed-zone conflict on the java.time entry points names the caller's constant
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c123_fixedZoneConflictOnTheJavaTimeEntryPointsNamesTheCallersConstant() {
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final String[][] cases = { { "2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT }, { "2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT } };

        for (final String[] c : cases) {
            final String legacy = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(c[0], c[1], kolkata)).getMessage();
            assertTrue(legacy.contains("format: " + c[1]), legacy);

            assertEquals(legacy, assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(c[0], c[1], kolkata)).getMessage());
            assertEquals(legacy, assertThrows(IllegalArgumentException.class, () -> Dates.parseToOffsetDateTime(c[0], c[1], kolkata)).getMessage());
            assertEquals(legacy, assertThrows(IllegalArgumentException.class, () -> Dates.parseToZonedDateTime(c[0], c[1], kolkata)).getMessage());
            assertFalse(legacy.contains("uuuu"), legacy);

            // A parse failure under the explicit constant names no zone: the constant's own 'Z' is the zone.
            final String explicit = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(c[0].replace("-15T", "-32T"), c[1], UTC))
                    .getMessage();
            assertTrue(explicit.startsWith("Cannot parse \"" + c[0].replace("-15T", "-32T") + "\" with format '" + c[1] + "':"), explicit);
        }

        // No conflict: auto-detected text, a UTC-equivalent zone, no zone.
        final Instant expected = Instant.parse("2025-01-15T10:30:45Z");
        assertEquals(expected, Dates.parseToInstant("2025-01-15T10:30:45Z", null, kolkata));
        assertEquals(expected, Dates.parseToInstant("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, UTC));
        assertEquals(expected, Dates.parseToInstant("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, TimeZone.getTimeZone("Etc/GMT")));
        assertEquals(expected, Dates.parseToInstant("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, null));
        assertEquals(expected, Dates.parseToZonedDateTime("2025-01-15T10:30:45Z", null, kolkata).toInstant());

        // HTTP-date: the legacy wording as well.
        final IllegalArgumentException http = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToInstant("Wed, 15 Jan 2025 10:30:45 GMT", Dates.HTTP_DATE_FORMAT, kolkata));
        assertTrue(http.getMessage().startsWith("HTTP-date requires a GMT/UTC-equivalent time zone"), http.getMessage());
        assertEquals(expected, Dates.parseToInstant("Wed, 15 Jan 2025 10:30:45 GMT", Dates.HTTP_DATE_FORMAT, UTC));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-124 (cycle 3): a carried 31st into the first month of the range keeps the day
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c124_carriedDayThirtyOneIntoTheFirstMonthOfTheRangeKeepsTheDay() {
        TimeZone.setDefault(UTC);
        final long may31 = java.time.LocalDateTime.of(-292275055, 5, 31, 12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        final java.util.Date july31 = new java.util.Date(java.time.LocalDateTime.of(-292275055, 7, 31, 12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli());

        assertEquals(may31, Dates.setYears(at("2024-05-31 12:00:00", UTC), -292275055).getTime());
        assertEquals(may31, Dates.setMonths(july31, 4).getTime());
        assertEquals(may31, Dates.setMonths(new java.util.Date(may31), 4).getTime());
        assertEquals(may31, Dates.setYears(new java.util.Date(may31), -292275055).getTime());
        assertEquals(may31, Dates.setDays(Dates.setDays(new java.util.Date(may31), 30), 31).getTime());
        assertEquals(may31 - 86_400_000L, Dates.setYears(at("2024-05-30 12:00:00", UTC), -292275055).getTime());

        // Every other clamp is unchanged, and so are the rejections.
        assertEquals("2025-02-28 12:00:00", local(Dates.setMonths(at("2025-01-31 12:00:00", UTC), 1), UTC));
        assertEquals("2023-02-28 12:00:00", local(Dates.setYears(at("2024-02-29 12:00:00", UTC), 2023), UTC));
        assertEquals("2024-08-31 12:00:00", local(Dates.setMonths(at("2024-01-31 12:00:00", UTC), 7), UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(new java.util.Date(may31), 13));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(new java.util.Date(may31), Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(new java.util.Date(may31), Integer.MAX_VALUE));
        assertThrows(ArithmeticException.class, () -> Dates.setMonths(new java.util.Date(may31), 0));

        // A zoned default: the wall clock lands where ZonedDateTime puts it.
        TimeZone.setDefault(NY);
        assertEquals(java.time.ZonedDateTime.of(java.time.LocalDateTime.of(-292275055, 5, 31, 12, 0), NY.toZoneId()).toInstant().toEpochMilli(),
                Dates.setYears(at("2024-05-31 12:00:00", NY), -292275055).getTime());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-125 (cycle 3): an inadmissible millisecond amount at the range edge is still an IllegalArgumentException
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c125_anInadmissibleMillisecondAmountAtTheRangeEdgeIsStillAnIllegalArgument() {
        TimeZone.setDefault(UTC);

        for (final long source : new long[] { Long.MAX_VALUE, Long.MAX_VALUE - 1, Long.MIN_VALUE, Long.MIN_VALUE + 1, 1736937045000L }) {
            for (final int amount : new int[] { 1000, 2148, 4295, 4296, 5195, 8590, -1, Integer.MIN_VALUE, Integer.MAX_VALUE }) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.setMilliseconds(new java.util.Date(source), amount),
                        source + " / " + amount);
                assertTrue(e.getMessage().contains("MILLISECOND"), e.getMessage());
            }

            assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(new java.util.Date(source), Integer.MAX_VALUE));
            assertThrows(IllegalArgumentException.class, () -> Dates.setDays(new java.util.Date(source), 0));
        }

        // Admissible amounts keep the C-073 contract.
        assertThrows(ArithmeticException.class, () -> Dates.setMilliseconds(new java.util.Date(Long.MAX_VALUE), 999));
        assertEquals(Long.MAX_VALUE, Dates.setMilliseconds(new java.util.Date(Long.MAX_VALUE), 807).getTime());
        assertEquals(Long.MIN_VALUE, Dates.setMilliseconds(new java.util.Date(Long.MIN_VALUE), 192).getTime());
        assertEquals(Long.MIN_VALUE + 807, Dates.setMilliseconds(new java.util.Date(Long.MIN_VALUE), 999).getTime());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-126 (cycle 3): the DTF legacy-type parsers reject an instant beyond the epoch-millisecond range uniformly
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c126_dtfLegacyTargetsRejectAnInstantBeyondTheEpochMillisecondRangeUniformly() {
        for (final String text : new String[] { "+300000000-01-01", "-292275056-01-01", "+292278995-01-01" }) {
            final java.util.List<org.junit.jupiter.api.function.Executable> calls = java.util.List.of(() -> DTF.LOCAL_DATE.parseToDate(text, UTC),
                    () -> DTF.LOCAL_DATE.parseToTime(text, UTC), () -> DTF.LOCAL_DATE.parseToCalendar(text, UTC), () -> DTF.LOCAL_DATE.parseToJUDate(text),
                    () -> DTF.LOCAL_DATE.parseToJUDate(text, UTC), () -> DTF.LOCAL_DATE.parseToTimestamp(text), () -> DTF.LOCAL_DATE.parseToTimestamp(text, UTC));

            for (final org.junit.jupiter.api.function.Executable call : calls) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call, text);
                assertTrue(e.getMessage().startsWith("Cannot parse \"" + text + "\" with pattern 'uuuu-MM-dd'"), e.getMessage());
                assertTrue(e.getMessage().contains("outside the epoch-millisecond range"), e.getMessage());
                assertTrue(e.getCause() instanceof java.time.DateTimeException, String.valueOf(e.getCause()));
                assertTrue(e.getCause().getCause() instanceof ArithmeticException, String.valueOf(e.getCause().getCause()));
            }
        }

        // The civil and Instant targets hold such values.
        assertEquals(java.time.LocalDate.of(300_000_000, 1, 1), DTF.LOCAL_DATE.parseToLocalDate("+300000000-01-01"));
        assertEquals(java.time.LocalDate.of(300_000_000, 1, 1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), DTF.LOCAL_DATE.parseToInstant("+300000000-01-01", UTC));

        // The last and first representable milliseconds on every legacy target; the silent wrap window just above
        // Long.MAX_VALUE and the first 808 milliseconds above Long.MIN_VALUE that Timestamp.from mishandled.
        final DTF f = DTF.of("uuuu-MM-dd'T'HH:mm:ss.SSSXXX");
        assertEquals(Long.MAX_VALUE, f.parseToTimestamp("+292278994-08-17T07:12:55.807Z").getTime());
        assertEquals(Long.MAX_VALUE, f.parseToJUDate("+292278994-08-17T07:12:55.807Z").getTime());
        assertEquals(Long.MAX_VALUE, f.parseToCalendar("+292278994-08-17T07:12:55.807Z", UTC).getTimeInMillis());
        assertThrows(IllegalArgumentException.class, () -> f.parseToTimestamp("+292278994-08-17T07:12:55.808Z"));
        assertThrows(IllegalArgumentException.class, () -> f.parseToTimestamp("+292278994-08-17T07:12:55.999Z"));
        assertThrows(IllegalArgumentException.class, () -> f.parseToJUDate("+292278994-08-17T07:12:55.808Z"));
        assertEquals(Long.MIN_VALUE, f.parseToTimestamp("-292275055-05-16T16:47:04.192Z").getTime());
        assertEquals(Long.MIN_VALUE + 807, f.parseToTimestamp("-292275055-05-16T16:47:04.999Z").getTime());
        assertEquals(Long.MIN_VALUE, f.parseToJUDate("-292275055-05-16T16:47:04.192Z").getTime());
        assertEquals(Long.MIN_VALUE, f.parseToDate("-292275055-05-16T16:47:04.192Z", UTC).getTime());
        assertThrows(IllegalArgumentException.class, () -> f.parseToTimestamp("-292275055-05-16T16:47:04.191Z"));

        // Nanoseconds survive the Timestamp construction.
        final java.sql.Timestamp ts = DTF.of("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX").parseToTimestamp("2025-01-15T10:30:45.123456789Z");
        assertEquals(1736937045123L, ts.getTime());
        assertEquals(123_456_789, ts.getNanos());
        assertEquals(1736937045123L, DTF.of("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSSXXX").parseToTimestamp("2025-01-15T10:30:45.123456789Z", NY).getTime());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-073 (cycle 2): a set* whose target leaves the epoch-millisecond range is an ArithmeticException
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c073_setterTargetBeyondTheEpochRangeIsAnArithmeticException() {
        final java.util.Date max = new java.util.Date(Long.MAX_VALUE);
        final java.util.Date min = new java.util.Date(Long.MIN_VALUE);

        // Asia/Shanghai: the wrapped wall clock repeats the written day, which used to RETURN a wrapped instant.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        final ArithmeticException wrapped = assertThrows(ArithmeticException.class, () -> Dates.setDays(max, 18));
        assertTrue(wrapped.getMessage().contains("Dates.setDays(date, 18) on epoch millis 9223372036854775807 wrapped"), wrapped.getMessage());

        TimeZone.setDefault(UTC);
        assertThrows(ArithmeticException.class, () -> Dates.setDays(max, 31));
        assertThrows(ArithmeticException.class, () -> Dates.setMilliseconds(max, 999));
        assertThrows(ArithmeticException.class, () -> Dates.setHours(max, 23));
        assertThrows(ArithmeticException.class, () -> Dates.setMonths(min, 0));
        assertThrows(ArithmeticException.class, () -> Dates.setYears(at("2025-12-31 23:00:00", UTC), 292278994));
        assertThrows(ArithmeticException.class, () -> Dates.setYears(at("2024-01-15 12:00:00", UTC), -292275055));
        // The first year of the range starts on 16 May, so a later wall clock in it is still reachable.
        assertEquals(java.time.LocalDateTime.of(-292275055, 6, 15, 12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli(),
                Dates.setYears(at("2024-06-15 12:00:00", UTC), -292275055).getTime());

        // A value the field itself does not admit is still the documented IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(max, 292278995));
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(max, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(max, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Dates.setHours(max, 24));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(max, 13));
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(at("2025-02-05 12:00:00", UTC), 31));

        // Below the edge nothing changes, and a write that stays in range near the edge still works.
        assertEquals("2025-06-15 12:00:00", local(Dates.setYears(at("1850-06-15 12:00:00", UTC), 2025), UTC));
        assertEquals(Long.MAX_VALUE - 1, Dates.setMilliseconds(new java.util.Date(Long.MAX_VALUE - 1), 806).getTime());
        assertEquals(Long.MIN_VALUE, Dates.setMilliseconds(min, 192).getTime());

        // The legacy path (a zone no ZoneId can express) is guarded too.
        TimeZone.setDefault(new SimpleTimeZone(8 * 3_600_000, "Custom/Rules", Calendar.APRIL, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0));
        assertThrows(ArithmeticException.class, () -> Dates.setDays(max, 18));
        assertThrows(ArithmeticException.class, () -> Dates.setMonths(min, 0));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-074 / C-075 (cycle 2): every auto-detected failure names "the auto-detected format", never a pattern
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c074_c075_autoDetectedFailuresNeverNameAPatternTheCallerDidNotWrite() {
        TimeZone.setDefault(NY);
        final String[] texts = { "2025-13-15T10:30:45.5Z", "2025-13-15T10:30:45.123Z", "2025-02-30T10:30:45.5Z", "2025-13-15 10:30:45", "2025-13-15",
                "2025-13-15T10:30:45", "2025-01-15T23:59:60Z", "2025-13-15T10:30:45+01:00", "2025-13-15T10:30:45.123" };
        final java.util.List<org.junit.jupiter.api.function.ThrowingConsumer<String>> targets = java.util.List.of(Dates::parseToJUDate,
                Dates::parseToTimestamp, Dates::parseToCalendar, Dates::parseToXMLGregorianCalendar, Dates::parseToInstant, Dates::parseToDate);

        for (final String text : texts) {
            for (final org.junit.jupiter.api.function.ThrowingConsumer<String> target : targets) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> target.accept(text), text);
                final String message = e.getMessage();
                assertTrue(message.startsWith("Cannot parse \"" + text + "\" with the auto-detected format"), message);
                assertEquals(1, message.split("the auto-detected format", -1).length - 1, message);
                assertFalse(message.contains("with pattern"), message);
                assertFalse(message.contains("with format"), message);
            }
        }

        // An explicitly supplied constant is still named as the caller's format.
        final IllegalArgumentException explicit = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2025-13-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT));
        assertTrue(explicit.getMessage().contains("with format 'yyyy-MM-dd'T'HH:mm:ss'Z''"), explicit.getMessage());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-076 (cycle 2): set* resolves the zone once; behaviour is unchanged
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c076_settersStillResolveEveryDocumentedCaseTheSameWay() {
        TimeZone.setDefault(NY);
        // Overlap keeps the source offset; a gap moves the field and is rejected; clamps still apply.
        final java.util.Date firstPass = new java.util.Date(Instant.parse("2025-11-02T05:30:00Z").toEpochMilli()); // 01:30 EDT
        final java.util.Date secondPass = new java.util.Date(Instant.parse("2025-11-02T06:30:00Z").toEpochMilli()); // 01:30 EST
        assertEquals(firstPass.getTime() + 60_000, Dates.setMinutes(firstPass, 31).getTime());
        assertEquals(secondPass.getTime() + 60_000, Dates.setMinutes(secondPass, 31).getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.setHours(at("2025-03-09 01:30:00", NY), 2));
        assertEquals("2025-02-28 12:00:00", local(Dates.setMonths(at("2025-01-31 12:00:00", NY), 1), NY));
        assertEquals("2023-02-28 12:00:00", local(Dates.setYears(at("2024-02-29 12:00:00", NY), 2023), NY));
        assertEquals("1850-06-15 12:00:00", local(Dates.setYears(at("2025-06-15 12:00:00", NY), 1850), NY));

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.setDays(at("2025-02-05 12:00:00", NY), 30));
        assertTrue(e.getMessage().contains("in time zone America/New_York: DAY_OF_MONTH: 30 -> 2"), e.getMessage());

        // A zone no ZoneId can express keeps Calendar's own resolution.
        TimeZone.setDefault(new SimpleTimeZone(-5 * 3_600_000, "Custom/Rules", Calendar.APRIL, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0));
        final java.util.Date custom = new java.util.Date(Instant.parse("2025-06-15T16:00:00Z").toEpochMilli()); // 12:00 under +1 h saving
        assertEquals(custom.getTime() + 3_600_000, Dates.setHours(custom, 13).getTime());
    }

    // ----------------------------------------------------------------------------------------------------
    // C-118 (cycle 2): a fixed-offset default of any class under a registered ID is read as that offset
    // ----------------------------------------------------------------------------------------------------

    @SuppressWarnings("serial")
    private static TimeZone fixedOffsetSubclass(final int rawOffsetMillis, final String id) {
        final TimeZone zone = new TimeZone() {
            @Override
            public int getOffset(final int era, final int year, final int month, final int day, final int dayOfWeek, final int milliseconds) {
                return rawOffsetMillis;
            }

            @Override
            public int getOffset(final long date) {
                return rawOffsetMillis;
            }

            @Override
            public void setRawOffset(final int offsetMillis) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int getRawOffset() {
                return rawOffsetMillis;
            }

            @Override
            public boolean useDaylightTime() {
                return false;
            }

            @Override
            public boolean inDaylightTime(final java.util.Date date) {
                return false;
            }
        };
        zone.setID(id);
        return zone;
    }

    @Test
    public void c118_fixedOffsetDefaultOfAnyClassUnderARegionIdIsReadAsThatOffset() {
        final java.util.Date a = new java.util.Date(Instant.parse("2025-01-15T23:30:00Z").toEpochMilli());
        final java.util.Date b = new java.util.Date(Instant.parse("2025-01-16T00:30:00Z").toEpochMilli());

        TimeZone.setDefault(fixedOffsetSubclass(0, "America/New_York"));
        assertFalse(Dates.isSameDay(a, b));
        assertFalse(Dates.isLastDayOfMonth(new java.util.Date(Instant.parse("2025-02-01T00:30:00Z").toEpochMilli())));

        TimeZone.setDefault(fixedOffsetSubclass(-5 * 3_600_000, "America/New_York"));
        assertTrue(Dates.isSameDay(a, b));

        TimeZone.setDefault(fixedOffsetSubclass(500, "America/New_York"));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(a, b));

        TimeZone.setDefault(fixedOffsetSubclass(0, "No/Such_Zone"));
        assertFalse(Dates.isSameDay(a, b));

        // A real region default and a customised known ID are unchanged.
        TimeZone.setDefault(NY);
        assertTrue(Dates.isSameDay(a, b));
        TimeZone.setDefault(new SimpleTimeZone(-5 * 3_600_000, "America/New_York", Calendar.APRIL, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0));
        assertTrue(Dates.isSameDay(a, b));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-092 (cycle 2): the documented whitespace rule of custom legacy patterns
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c092_customPatternsKeepSimpleDateFormatWhitespaceSkipping() {
        final long expected = at("2025-10-22 00:00:00", UTC).getTime();

        assertEquals(expected, Dates.parseToJUDate(" 22/10/2025", "dd/MM/yyyy", UTC).getTime());
        assertEquals(expected, Dates.parseToJUDate("22/ 10/2025", "dd/MM/yyyy", UTC).getTime());
        assertEquals(expected, Dates.parseToTimestamp("\t22/10/2025", "dd/MM/yyyy", UTC).getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("22/10/2025 ", "dd/MM/yyyy", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(" 2025-10-22", Dates.LOCAL_DATE_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate(" 22/10/2025", "dd/MM/yyyy"));
        assertThrows(IllegalArgumentException.class, () -> DTF.of("dd/MM/yyyy").parseToLocalDate(" 22/10/2025"));
    }

    // ----------------------------------------------------------------------------------------------------
    // C-007
    // ----------------------------------------------------------------------------------------------------

    @Test
    public void c007_fixedOffsetDefaultUnderARegionIdIsReadAsThatOffset() {
        TimeZone.setDefault(new SimpleTimeZone(0, "America/New_York"));
        final java.util.Date a = new java.util.Date(Instant.parse("2025-01-15T23:30:00Z").toEpochMilli());
        final java.util.Date b = new java.util.Date(Instant.parse("2025-01-16T00:30:00Z").toEpochMilli());
        final java.util.Date c = new java.util.Date(Instant.parse("2025-01-31T23:30:00Z").toEpochMilli());
        final java.util.Date d = new java.util.Date(Instant.parse("2025-02-01T00:30:00Z").toEpochMilli());
        final java.util.Date e = new java.util.Date(Instant.parse("2025-12-31T23:30:00Z").toEpochMilli());
        final java.util.Date f = new java.util.Date(Instant.parse("2026-01-01T00:30:00Z").toEpochMilli());

        assertFalse(Dates.isSameDay(a, b));
        assertFalse(Dates.isSameMonth(c, d));
        assertFalse(Dates.isSameYear(e, f));
        assertEquals(Dates.isSameDay(a, b, TimeZone.getDefault()), Dates.isSameDay(a, b));
        assertEquals(Dates.truncatedEquals(a, b, Calendar.DATE), Dates.isSameDay(a, b));
        assertTrue(Dates.isLastDayOfMonth(c));
        assertFalse(Dates.isLastDayOfMonth(d));
        assertTrue(Dates.isLastDayOfYear(e));
        assertEquals(31, Dates.lengthOfMonth(c));
        assertEquals(365, Dates.lengthOfYear(e));
    }

    @Test
    public void c007_otherDefaultZonePoliciesAreUnchanged() {
        final java.util.Date a = new java.util.Date(Instant.parse("2025-01-15T23:30:00Z").toEpochMilli());
        final java.util.Date b = new java.util.Date(Instant.parse("2025-01-16T00:30:00Z").toEpochMilli());

        // A default carrying daylight-saving rules under a registered ID still uses that ID's rules.
        TimeZone.setDefault(new SimpleTimeZone(-5 * 3_600_000, "America/New_York", Calendar.APRIL, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0));
        assertTrue(Dates.isSameDay(a, b));

        // An unknown fixed-offset ID is read as its offset; an unknown ID with daylight-saving rules is rejected.
        TimeZone.setDefault(new SimpleTimeZone(0, "No/Such_Zone"));
        assertFalse(Dates.isSameDay(a, b));
        TimeZone.setDefault(new SimpleTimeZone(0, "No/Such_Zone", Calendar.APRIL, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(a, b));

        // A sub-second fixed offset is rejected whatever its ID, as everywhere else in the class.
        TimeZone.setDefault(new SimpleTimeZone(500, "America/New_York"));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(a, b));

        // A real region default is unaffected.
        TimeZone.setDefault(NY);
        assertTrue(Dates.isSameDay(a, b));
    }
}
