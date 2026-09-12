package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Contract tests pinning the reviewed date/time semantics: the fixed offset-second formatting (B2),
 * the instant-preserving parseToDate/parseToTime contract and explicit epoch-millisecond API (B1),
 * the fixed nearest-boundary rounding semantics (B3), the unified live-default-zone and
 * Calendar-zone policy (D2), and the strict automatic parsing detection (D3: ambiguous numeric text
 * and null markers are rejected). It also verifies shared Dates/DTF target semantics, null-or-empty
 * default formatting, and lossless, XML-Schema-valid XMLGregorianCalendar defaults.
 * These tests intentionally lock in current behavior; changing any of them is a breaking change.
 */
public class DatesContractTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // -----------------------------------------------------------------
    // B2 (fixed): DTF offset/zoned formats must preserve offset seconds.
    // -----------------------------------------------------------------

    @Test
    public void dtf_isoOffsetDateTime_preservesNonZeroOffsetSeconds() {
        OffsetDateTime odt = OffsetDateTime.of(1880, 1, 1, 12, 0, 0, 0, ZoneOffset.of("+00:19:32"));
        String formatted = Dates.DTF.ISO_OFFSET_DATE_TIME.format(odt);
        assertEquals("1880-01-01T12:00:00+00:19:32", formatted);
        assertEquals(odt, Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(formatted));
    }

    @Test
    public void dtf_isoOffsetDateTime_wholeMinuteOffsetOutputUnchanged() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 12, 25, 15, 30, 45, 0, ZoneOffset.of("+05:30"));
        String formatted = Dates.DTF.ISO_OFFSET_DATE_TIME.format(odt);
        assertEquals("2023-12-25T15:30:45+05:30", formatted);
        assertEquals(odt, Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(formatted));
    }

    @Test
    public void dtf_isoZonedDateTime_roundTripsHistoricalOffsetWithSeconds() {
        ZonedDateTime ams = ZonedDateTime.of(1800, 1, 1, 12, 0, 0, 0, ZoneId.of("Europe/Amsterdam"));
        String formatted = Dates.DTF.ISO_ZONED_DATE_TIME.format(ams);
        assertEquals("1800-01-01T12:00:00+00:17:30[Europe/Amsterdam]", formatted);
        assertEquals(ams, Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(formatted));
    }

    @Test
    public void datesExplicitIsoOffsetFormat_hasUniformLegacyGrammarAcrossTargets() {
        final String compact = "2025-01-15T10:30:45+0530";
        final Instant expectedInstant = Instant.parse("2025-01-15T05:00:45Z");
        final long expectedMillis = expectedInstant.toEpochMilli();

        assertEquals(expectedInstant, Dates.parseToJUDate(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).toInstant());
        assertEquals(expectedMillis, Dates.parseToDate(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTime());
        assertEquals(expectedMillis, Dates.parseToTime(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTime());
        assertEquals(expectedInstant, Dates.parseToTimestamp(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).toInstant());
        assertEquals(expectedMillis, Dates.parseToCalendar(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTimeInMillis());
        assertEquals(expectedMillis, Dates.parseToGregorianCalendar(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTimeInMillis());
        assertEquals(expectedMillis, Dates.parseToXMLGregorianCalendar(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).toGregorianCalendar().getTimeInMillis());
        assertEquals(expectedInstant, Dates.parseToInstant(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertEquals(LocalDate.of(2025, 1, 15), Dates.parseToLocalDate(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertEquals(LocalTime.of(10, 30, 45), Dates.parseToLocalTime(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30, 45), Dates.parseToLocalDateTime(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), Dates.parseToOffsetDateTime(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).getOffset());
        assertEquals(expectedInstant, Dates.parseToZonedDateTime(compact, Dates.ISO_OFFSET_DATE_TIME_FORMAT).toInstant());

        // Dates.ISO_OFFSET_DATE_TIME_FORMAT is the legacy XXX pattern. DTF's named formatter is
        // deliberately XXXXX so it can round-trip historical offsets with seconds.
        final String offsetSeconds = "1880-01-01T12:00:00+00:19:32";
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalTime(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToOffsetDateTime(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToZonedDateTime(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(offsetSeconds, Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertEquals(OffsetDateTime.parse(offsetSeconds).toInstant(), Dates.parseToInstant(offsetSeconds));
        assertEquals(OffsetDateTime.parse(offsetSeconds), Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetSeconds));
    }

    // -----------------------------------------------------------------
    // Fraction grammar: the named .SSS timestamp constants mean exactly three
    // fraction digits when supplied explicitly (on every target and engine).
    // Auto-detected text follows the JDBC escape grammar instead: 1-9 digits read
    // as a fraction of a second, as Timestamp.toString() writes them (".5" = 500ms).
    // -----------------------------------------------------------------

    @Test
    public void namedTimestampFormats_requireExactlyThreeFractionDigits() {
        // Explicit named format: a non-three-digit (or omitted) fraction is rejected on every path.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15 10:30:45.5", Dates.LOCAL_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15 10:30:45.12", Dates.LOCAL_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime("2025-01-15 10:30:45.123456789", Dates.LOCAL_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime("2025-01-15 10:30:45", Dates.LOCAL_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T10:30:45.5Z", Dates.ISO_8601_TIMESTAMP_FORMAT));

        // Exactly three digits parse everywhere, with identical results.
        assertEquals(123L, Dates.parseToTimestamp("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT).getTime() % 1000);
        assertEquals(123_000_000, Dates.parseToLocalDateTime("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT).getNano());
        assertEquals(Instant.parse("2025-01-15T10:30:45.123Z"), Dates.parseToInstant("2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT));
    }

    @Test
    public void autoDetectedJdbcEscapeGrammar_allTargetsAgree() {
        // Timestamp.toString() trims trailing zeros; auto-detection must round-trip it with
        // fraction-of-second semantics, identically for every target.
        for (final java.sql.Timestamp ts : new java.sql.Timestamp[] { java.sql.Timestamp.valueOf("2025-01-15 10:30:45.5"),
                java.sql.Timestamp.valueOf("2025-01-15 10:30:45.12"), java.sql.Timestamp.valueOf("2025-01-15 10:30:45.123456789") }) {
            final String text = ts.toString();

            assertEquals(ts, Dates.parseToTimestamp(text));
            assertEquals(ts.getTime(), Dates.parseToJUDate(text).getTime());
            assertEquals(ts.getTime(), Dates.parseToCalendar(text).getTimeInMillis());
            assertEquals(ts.toLocalDateTime(), Dates.parseToLocalDateTime(text));
            assertEquals(ts.toInstant(), Dates.parseToInstant(text));
            assertEquals(ts.toLocalDateTime().toLocalDate(), Dates.parseToDate(text).toLocalDate());
        }
    }

    @Test
    public void autoDetectedIsoTimestamp_variableFraction_allTargetsAgree() {
        // T-separated (and Z-suffixed) shapes with trimmed fractions follow the same grammar.
        assertEquals(500_000_000, Dates.parseToLocalDateTime("2025-01-15T10:30:45.5").getNano());
        assertEquals(120_000_000, Dates.parseToTimestamp("2025-01-15T10:30:45.12").getNanos());
        assertEquals(Instant.parse("2025-01-15T10:30:45.500Z"), Dates.parseToInstant("2025-01-15T10:30:45.5Z"));
        assertEquals(Instant.parse("2025-01-15T10:30:45.120Z"), Dates.parseToInstant("2025-01-15T10:30:45.12Z"));

        final String zuluNanos = "2025-01-15T10:30:45.123456789Z";
        assertEquals(Instant.parse(zuluNanos), Dates.parseToInstant(zuluNanos));
        assertEquals(123_456_789, Dates.parseToTimestamp(zuluNanos).getNanos());
        assertEquals("0.123456789", Dates.parseToXMLGregorianCalendar(zuluNanos).getFractionalSecond().toPlainString());

        final String localNanos = "2025-01-15T10:30:45.123456789";
        assertEquals(123_456_789, Dates.parseToTimestamp(localNanos).getNanos());

        final String offsetNanos = "2025-01-15T10:30:45.123456789+05:30";
        assertEquals(123_456_789, Dates.parseToTimestamp(offsetNanos).getNanos());
        assertEquals(OffsetDateTime.parse(offsetNanos).toInstant(), Dates.parseToTimestamp(offsetNanos).toInstant());
        assertEquals(OffsetDateTime.parse(offsetNanos).toInstant(), Dates.parseToInstant(offsetNanos));

        final String zonedNanos = "2025-01-15T10:30:45.123456789+05:30[Asia/Kolkata]";
        assertEquals(ZonedDateTime.parse(zonedNanos).toInstant(), Dates.parseToInstant(zonedNanos));
        assertEquals(123_456_789, Dates.parseToLocalDateTime(zonedNanos).getNano());
        assertEquals(123_456_789, Dates.parseToTimestamp(zonedNanos).getNanos());
        assertEquals("0.123456789", Dates.parseToXMLGregorianCalendar(zonedNanos).getFractionalSecond().toPlainString());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T10:30:45.123456789+04:00[Asia/Kolkata]"));
    }

    // -----------------------------------------------------------------
    // DST strictness: the standard zone-less local date and date-time shapes reject gaps
    // and overlaps on every instant target; custom patterns keep legacy resolution.
    // -----------------------------------------------------------------

    @Test
    public void dstGapAndOverlap_rejectedOnStandardShapes_allTargets() {
        final TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");
        final String gap = "2024-03-10 02:30:00"; // nonexistent in America/Los_Angeles
        final String overlap = "2024-11-03 01:30:00"; // ambiguous in America/Los_Angeles

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(gap, null, la));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(overlap, null, la));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(gap, null, la));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(overlap, null, la));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar(gap, null, la));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(overlap, null, la));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime(gap, null, la));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(overlap, null, la));

        // The ISO local shape is strict as well.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2024-11-03T01:30:00", null, la));
    }

    @Test
    public void dateOnlyMidnightGapAndOverlap_datesAndDtfResolveIdentically() {
        final TimeZone cuiaba = TimeZone.getTimeZone("America/Cuiaba");
        final ZoneId zone = cuiaba.toZoneId();
        final String gap = "1932-10-03";
        final String overlap = "1950-04-16";

        assertEquals(0, zone.getRules().getValidOffsets(LocalDate.parse(gap).atStartOfDay()).size());
        assertEquals(2, zone.getRules().getValidOffsets(LocalDate.parse(overlap).atStartOfDay()).size());

        assertDateOnlyMidnightTransitionRejected(gap, cuiaba);
        assertDateOnlyMidnightTransitionRejected(overlap, cuiaba);

        // Civil-field parsers do not resolve an instant and therefore retain the date as written.
        assertEquals(LocalDate.parse(gap), Dates.parseToLocalDate(gap, Dates.LOCAL_DATE_FORMAT));
        assertEquals(LocalDate.parse(overlap), Dates.DTF.LOCAL_DATE.parseToLocalDate(overlap));
    }

    private static void assertDateOnlyMidnightTransitionRejected(final String text, final TimeZone timeZone) {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToOffsetDateTime(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToZonedDateTime(text, Dates.LOCAL_DATE_FORMAT, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(text, Dates.LOCAL_DATE_FORMAT, timeZone));

        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToJUDate(text, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToDate(text, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToTime(text, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToTimestamp(text, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToCalendar(text, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToOffsetDateTime(text, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToZonedDateTime(text, timeZone));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToInstant(text, timeZone));
    }

    @Test
    public void historicalDstOverlap_isRejectedEvenWhenZoneNoLongerUsesDst() {
        final TimeZone shanghai = TimeZone.getTimeZone("Asia/Shanghai");
        final String overlap = "1991-09-15 01:30:00";

        assertEquals(false, shanghai.useDaylightTime());
        assertEquals(2, ZoneId.of("Asia/Shanghai").getRules().getValidOffsets(LocalDateTime.of(1991, 9, 15, 1, 30)).size());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(overlap, null, shanghai));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(overlap, null, shanghai));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(overlap, null, shanghai));
    }

    @Test
    public void dstCustomPattern_isNonLenientButDoesNotRejectOverlap() {
        // SimpleDateFormat is non-lenient here, so a gap is rejected; an overlap still silently
        // resolves to one offset because the custom-pattern path has no explicit ambiguity check.
        final TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2024/03/10 02:30:00", "yyyy/MM/dd HH:mm:ss", la));

        final java.util.Date resolved = Dates.parseToJUDate("2024/11/03 01:30:00", "yyyy/MM/dd HH:mm:ss", la);
        assertEquals(Instant.parse("2024-11-03T09:30:00Z"), resolved.toInstant());
    }

    // -----------------------------------------------------------------
    // SQL legacy types preserve the resolved instant; a supplied zone only resolves zone-less text.
    // -----------------------------------------------------------------

    @Test
    public void parseToDateWithZone_preservesResolvedInstant() {
        assertEquals(1736899200000L, Dates.parseToDate("2025-01-15", "yyyy-MM-dd", UTC).getTime());
        assertEquals(1736937045000L, Dates.parseToDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", UTC).getTime());
        assertEquals(1736937045000L, Dates.DTF.LOCAL_DATE_TIME.parseToDate("2025-01-15 10:30:45", UTC).getTime());
        // Date-only, zone-less text naturally resolves to start of day in the live default zone.
        assertEquals(LocalDate.of(2025, 1, 15).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                Dates.parseToDate("2025-01-15", "yyyy-MM-dd").getTime());
        // A fixed-Z value identifies its instant without consulting the default zone.
        assertNotNull(Dates.parseToDate("2025-01-15T10:30:45Z"));
        assertNotNull(Dates.parseToCalendar("2025-01-15T10:30:45Z"));
        // An explicitly supplied non-UTC zone still conflicts with the UTC designator.
        assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToDate("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, TimeZone.getTimeZone("America/Los_Angeles")));
    }

    @Test
    public void parseToTimeWithZone_preservesResolvedInstant() {
        assertEquals(52245000L, Dates.parseToTime("14:30:45", "HH:mm:ss", UTC).getTime());
        assertEquals(52245000L, Dates.DTF.LOCAL_TIME.parseToTime("14:30:45", UTC).getTime());
        assertEquals(-48_600_000L, Dates.parseToTime("00:30:00+14:00", "HH:mm:ssXXX", UTC).getTime());
        assertEquals(-48_600_000L, Dates.DTF.of("HH:mm:ssXXX").parseToTime("00:30:00+14:00", UTC).getTime());
        assertEquals(1736937045000L, Dates.parseToTime("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", UTC).getTime());
        assertEquals(1736937045000L, Dates.DTF.LOCAL_DATE_TIME.parseToTime("2025-01-15 10:30:45", UTC).getTime());
        assertEquals(1736899200000L, Dates.DTF.LOCAL_DATE.parseToDate("2025-01-15", UTC).getTime());
    }

    @Test
    public void parseToTime_requiresCompleteDateOrActualTimeFieldsInBothDatesAndDtf() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("2025 14:30:45", "yyyy HH:mm:ss", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("01-15 14:30:45", "MM-dd HH:mm:ss", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("uuuu HH:mm:ss").parseToTime("2025 14:30:45", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("MM-dd HH:mm:ss").parseToTime("01-15 14:30:45", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("+05:30", "XXX", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("XXX").parseToTime("+05:30", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("literal", "'literal'", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("'literal'").parseToTime("literal", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("30:45", "mm:ss", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("mm:ss").parseToTime("30:45", UTC));

        assertEquals(52_245_000L, Dates.parseToTime("14:30:45", "HH:mm:ss", UTC).getTime());
        assertEquals(52_245_000L, Dates.DTF.LOCAL_TIME.parseToTime("14:30:45", UTC).getTime());
        assertEquals(52_200_000L, Dates.parseToTime("02:30 PM", "hh:mm a", UTC).getTime());
        assertEquals(52_200_000L, Dates.DTF.of("hh:mm a").parseToTime("02:30 PM", UTC).getTime());
        assertEquals(1736937045000L, Dates.parseToTime("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", UTC).getTime());
        assertEquals(1736937045000L, Dates.DTF.LOCAL_DATE_TIME.parseToTime("2025-01-15 10:30:45", UTC).getTime());
    }

    @Test
    public void dtfTimeOnly_usesActualCustomDefaultRules_notCollidingRegisteredId() {
        final TimeZone prior = TimeZone.getDefault();
        final TimeZone fixedPlusFiveFortyFive = new java.util.SimpleTimeZone((5 * 60 + 45) * 60 * 1000, "America/Los_Angeles");

        try {
            TimeZone.setDefault(fixedPlusFiveFortyFive);
            assertEquals(-20_700_000L, Dates.parseToTime("00:00:00", "HH:mm:ss").getTime());
            assertEquals(-20_700_000L, Dates.DTF.LOCAL_TIME.parseToTime("00:00:00").getTime());
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    @Test
    public void datesAndDtf_sharedTargets_resolveTheSameFieldsAndInstant() {
        assertSharedDatesAndDtfTargets("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, Dates.DTF.LOCAL_DATE_TIME, UTC,
                Instant.parse("2025-01-15T10:30:45Z"));
        assertSharedDatesAndDtfTargets("2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT, Dates.DTF.ISO_8601_DATE_TIME, UTC,
                Instant.parse("2025-01-15T10:30:45Z"));
        assertSharedDatesAndDtfTargets("2025-01-15T10:30:45+05:30", Dates.ISO_OFFSET_DATE_TIME_FORMAT, Dates.DTF.ISO_OFFSET_DATE_TIME, UTC,
                Instant.parse("2025-01-15T05:00:45Z"));
        assertSharedDatesAndDtfTargets("2025-01-15T10:30:45+05:30[Asia/Kolkata]", Dates.ISO_ZONED_DATE_TIME_FORMAT, Dates.DTF.ISO_ZONED_DATE_TIME, UTC,
                Instant.parse("2025-01-15T05:00:45Z"));
    }

    private static void assertSharedDatesAndDtfTargets(final String text, final String datesFormat, final Dates.DTF dtf, final TimeZone fallbackZone,
            final Instant expectedInstant) {
        final long expectedMillis = expectedInstant.toEpochMilli();

        assertEquals(expectedMillis, Dates.parseToJUDate(text, datesFormat, fallbackZone).getTime());
        assertEquals(expectedMillis, Dates.parseToDate(text, datesFormat, fallbackZone).getTime());
        assertEquals(expectedMillis, Dates.parseToTime(text, datesFormat, fallbackZone).getTime());
        assertEquals(expectedInstant, Dates.parseToTimestamp(text, datesFormat, fallbackZone).toInstant());
        assertEquals(expectedMillis, Dates.parseToCalendar(text, datesFormat, fallbackZone).getTimeInMillis());
        assertEquals(expectedInstant, Dates.parseToInstant(text, datesFormat, fallbackZone));

        assertEquals(expectedMillis, dtf.parseToJUDate(text, fallbackZone).getTime());
        assertEquals(expectedMillis, dtf.parseToDate(text, fallbackZone).getTime());
        assertEquals(expectedMillis, dtf.parseToTime(text, fallbackZone).getTime());
        assertEquals(expectedInstant, dtf.parseToTimestamp(text, fallbackZone).toInstant());
        assertEquals(expectedMillis, dtf.parseToCalendar(text, fallbackZone).getTimeInMillis());
        assertEquals(expectedInstant, dtf.parseToInstant(text, fallbackZone));

        assertEquals(Dates.parseToLocalDate(text, datesFormat), dtf.parseToLocalDate(text));
        assertEquals(Dates.parseToLocalTime(text, datesFormat), dtf.parseToLocalTime(text));
        assertEquals(Dates.parseToLocalDateTime(text, datesFormat), dtf.parseToLocalDateTime(text));
        assertEquals(Dates.parseToOffsetDateTime(text, datesFormat, fallbackZone), dtf.parseToOffsetDateTime(text, fallbackZone));
        assertEquals(Dates.parseToZonedDateTime(text, datesFormat, fallbackZone), dtf.parseToZonedDateTime(text, fallbackZone));
    }

    @Test
    public void sqlDateAndTime_defaultFormat_roundTripEpochMillis() {
        final java.sql.Date date = new java.sql.Date(1736937045123L);
        final java.sql.Time time = new java.sql.Time(1736937045123L);

        assertEquals("2025-01-15T10:30:45.123Z", Dates.format(date));
        assertEquals("2025-01-15T10:30:45.123Z", Dates.format(time));
        assertEquals(date, Dates.parseToDate(Dates.format(date)));
        assertEquals(time, Dates.parseToTime(Dates.format(time)));
    }

    @Test
    public void legacyFormat_emptyPatternUsesTypeDependentDefault() {
        final long millis = 1736937045123L;
        final java.util.Date date = new java.util.Date(millis);
        final java.sql.Timestamp timestamp = new java.sql.Timestamp(millis);

        assertEquals(Dates.format(date), Dates.format(date, ""));
        assertEquals(Dates.format(timestamp), Dates.format(timestamp, ""));

        // Pin the defaults themselves, not just self-consistency. Both are fixed-UTC 'Z' forms,
        // so these literals hold whatever the JVM default zone is.
        assertEquals("2025-01-15T10:30:45Z", Dates.format(date, ""));
        assertEquals("2025-01-15T10:30:45.123Z", Dates.format(timestamp, ""));
        assertEquals(Dates.format(date, null, UTC), Dates.format(date, "", UTC));
        assertEquals(Dates.format(timestamp, null, UTC, Locale.GERMAN), Dates.format(timestamp, "", UTC, Locale.GERMAN));

        final StringBuilder dateOutput = new StringBuilder();
        Dates.formatTo(date, "", dateOutput);
        assertEquals(Dates.format(date), dateOutput.toString());

        final StringBuilder timestampOutput = new StringBuilder();
        Dates.formatTo(timestamp, "", UTC, Locale.GERMAN, timestampOutput);
        assertEquals(Dates.format(timestamp, null, UTC, Locale.GERMAN), timestampOutput.toString());

        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final Calendar calendar = Dates.createCalendar(millis, kolkata);
        assertEquals(Dates.format(calendar), Dates.format(calendar, ""));
        assertEquals(Dates.format(calendar, null, UTC, Locale.GERMAN), Dates.format(calendar, "", UTC, Locale.GERMAN));

        final StringBuilder calendarOutput = new StringBuilder();
        Dates.formatTo(calendar, "", calendarOutput);
        assertEquals(Dates.format(calendar), calendarOutput.toString());

        final StringBuilder calendarZoneOutput = new StringBuilder();
        Dates.formatTo(calendar, "", UTC, calendarZoneOutput);
        assertEquals(Dates.format(calendar, null, UTC), calendarZoneOutput.toString());

        final XMLGregorianCalendar xmlCalendar = Dates.createXMLGregorianCalendar(millis, kolkata);
        assertEquals(Dates.format(xmlCalendar), Dates.format(xmlCalendar, ""));
        assertEquals(Dates.format(xmlCalendar, null, UTC, Locale.GERMAN), Dates.format(xmlCalendar, "", UTC, Locale.GERMAN));

        final StringBuilder xmlDefaultOutput = new StringBuilder();
        Dates.formatTo(xmlCalendar, "", xmlDefaultOutput);
        assertEquals(Dates.format(xmlCalendar), xmlDefaultOutput.toString());

        final StringBuilder xmlOutput = new StringBuilder();
        Dates.formatTo(xmlCalendar, "", UTC, Locale.GERMAN, xmlOutput);
        assertEquals(Dates.format(xmlCalendar, null, UTC, Locale.GERMAN), xmlOutput.toString());
    }

    @Test
    public void formatParseExamples_legacySqlAndXmlTypes_matchExactValues() throws Exception {
        final long millis = 1736937045123L;
        final String timestampText = "2025-01-15T10:30:45.123Z";

        final java.sql.Date sqlDate = new java.sql.Date(millis);
        assertEquals(timestampText, Dates.format(sqlDate));
        assertEquals(millis, Dates.parseToDate(timestampText).getTime());

        final java.sql.Time sqlTime = new java.sql.Time(millis);
        assertEquals(timestampText, Dates.format(sqlTime));
        assertEquals(millis, Dates.parseToTime(timestampText).getTime());

        final java.sql.Timestamp sqlTimestamp = new java.sql.Timestamp(millis);
        assertEquals(timestampText, Dates.format(sqlTimestamp));
        assertEquals(sqlTimestamp, Dates.parseToTimestamp(timestampText));

        final long wholeSecond = 1736937045000L;
        final String dateTimeText = "2025-01-15T10:30:45Z";

        final java.util.Date juDate = new java.util.Date(wholeSecond);
        assertEquals(dateTimeText, Dates.format(juDate));
        assertEquals(wholeSecond, Dates.parseToJUDate(dateTimeText).getTime());

        final long zonedMillis = Instant.parse("2025-01-15T05:00:45.123Z").toEpochMilli();
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final String zonedText = "2025-01-15T10:30:45.123+05:30[Asia/Kolkata]";

        final Calendar calendar = Dates.createCalendar(zonedMillis, kolkata);
        assertEquals(zonedText, Dates.format(calendar));
        final Calendar parsedCalendar = Dates.parseToCalendar(zonedText);
        assertEquals(zonedMillis, parsedCalendar.getTimeInMillis());
        assertEquals("Asia/Kolkata", parsedCalendar.getTimeZone().getID());

        final StringBuilder calendarOutput = new StringBuilder();
        Dates.formatTo(calendar, calendarOutput);
        assertEquals(zonedText, calendarOutput.toString());
        assertEquals("2025-01-15T05:00:45.123Z[UTC]", Dates.format(calendar, null, UTC));

        final StringBuilder calendarUtcOutput = new StringBuilder();
        Dates.formatTo(calendar, null, UTC, calendarUtcOutput);
        assertEquals("2025-01-15T05:00:45.123Z[UTC]", calendarUtcOutput.toString());

        final GregorianCalendar gregorianCalendar = Dates.createGregorianCalendar(zonedMillis, kolkata);
        assertEquals(zonedText, Dates.format(gregorianCalendar));
        final GregorianCalendar parsedGregorianCalendar = Dates.parseToGregorianCalendar(zonedText);
        assertEquals(zonedMillis, parsedGregorianCalendar.getTimeInMillis());
        assertEquals("Asia/Kolkata", parsedGregorianCalendar.getTimeZone().getID());

        final String xmlText = "2025-01-15T10:30:45.123+05:30";
        final XMLGregorianCalendar xmlCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(xmlText);
        assertEquals(xmlText, Dates.format(xmlCalendar));
        final XMLGregorianCalendar parsedXmlCalendar = Dates.parseToXMLGregorianCalendar(xmlText);
        assertEquals(xmlCalendar, parsedXmlCalendar);
        // the parser writes the factory's lexical fraction (2026-09-02): the round trip is text-equal too
        assertEquals("0.123", parsedXmlCalendar.getFractionalSecond().toPlainString());
        assertEquals(xmlText, Dates.format(parsedXmlCalendar));
        assertEquals(zonedMillis, parsedXmlCalendar.toGregorianCalendar().getTimeInMillis());
        assertEquals(330, parsedXmlCalendar.getTimezone());

        final StringBuilder xmlOutput = new StringBuilder();
        Dates.formatTo(xmlCalendar, xmlOutput);
        assertEquals(xmlText, xmlOutput.toString());
        assertEquals("2025-01-15T05:00:45.123Z", Dates.format(xmlCalendar, null, UTC));

        final StringBuilder xmlUtcOutput = new StringBuilder();
        Dates.formatTo(xmlCalendar, null, UTC, xmlUtcOutput);
        assertEquals("2025-01-15T05:00:45.123Z", xmlUtcOutput.toString());

        final String xmlNanosText = "2025-01-15T10:30:45.123456789+05:30";
        final XMLGregorianCalendar xmlNanos = DatatypeFactory.newInstance().newXMLGregorianCalendar(xmlNanosText);
        assertEquals(xmlNanosText, Dates.format(xmlNanos));
        assertEquals(xmlNanosText, Dates.parseToXMLGregorianCalendar(Dates.format(xmlNanos)).toXMLFormat());
        assertEquals("2025-01-15T05:00:45.123456789Z", Dates.format(xmlNanos, null, UTC));

        final StringBuilder xmlNanosUtcOutput = new StringBuilder();
        Dates.formatTo(xmlNanos, null, UTC, xmlNanosUtcOutput);
        assertEquals("2025-01-15T05:00:45.123456789Z", xmlNanosUtcOutput.toString());

        final String xmlWithoutZoneText = "2025-01-15T10:30:45.123";
        final XMLGregorianCalendar xmlWithoutZone = DatatypeFactory.newInstance().newXMLGregorianCalendar(xmlWithoutZoneText);
        assertEquals(xmlWithoutZoneText, Dates.format(xmlWithoutZone));
        assertEquals(javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED, xmlWithoutZone.getTimezone());
    }

    @Test
    public void xmlCalendar_rejectsZonesAndFractionsThatCannotBeRepresentedExactly() throws Exception {
        final String offsetSeconds = "1880-01-01T12:00:00+00:19:32";
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(offsetSeconds));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45+14:01"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45+18:00"));
        assertEquals(14 * 60, Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45+14:00").getTimezone());

        final XMLGregorianCalendar subNanosecond = DatatypeFactory.newInstance().newXMLGregorianCalendar("2025-01-15T10:30:45.1234567891+05:30");
        assertEquals("2025-01-15T10:30:45.1234567891+05:30", Dates.format(subNanosecond));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(subNanosecond, null, UTC));

        final XMLGregorianCalendar leapSecond = DatatypeFactory.newInstance().newXMLGregorianCalendar("2016-12-31T23:59:60Z");
        assertEquals("2016-12-31T23:59:60Z", Dates.format(leapSecond));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(leapSecond, null, UTC));

        final XMLGregorianCalendar ordinary = DatatypeFactory.newInstance().newXMLGregorianCalendar("2025-01-15T10:30:45Z");
        final TimeZone plusFourteen = new java.util.SimpleTimeZone(14 * 60 * 60 * 1000, "plus-fourteen");
        assertEquals("2025-01-16T00:30:45+14:00", Dates.format(ordinary, null, plusFourteen));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(ordinary, null, new java.util.SimpleTimeZone(18 * 60 * 60 * 1000, "plus-eighteen")));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(ordinary, null, new java.util.SimpleTimeZone(30_000, "plus-thirty-seconds")));
    }

    @Test
    public void formatParseExamples_javaTimeTypes_matchExactValues() {
        final LocalDate localDate = LocalDate.of(2025, 1, 15);
        final String localDateText = Dates.DTF.LOCAL_DATE.format(localDate);
        assertEquals("2025-01-15", localDateText);
        assertEquals(localDate, Dates.DTF.LOCAL_DATE.parseToLocalDate(localDateText));

        final LocalTime localTime = LocalTime.of(10, 30, 45);
        final String localTimeText = Dates.DTF.LOCAL_TIME.format(localTime);
        assertEquals("10:30:45", localTimeText);
        assertEquals(localTime, Dates.DTF.LOCAL_TIME.parseToLocalTime(localTimeText));

        final LocalDateTime localDateTime = LocalDateTime.of(2025, 1, 15, 10, 30, 45);
        final String localDateTimeText = Dates.DTF.LOCAL_DATE_TIME.format(localDateTime);
        assertEquals("2025-01-15 10:30:45", localDateTimeText);
        assertEquals(localDateTime, Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime(localDateTimeText));

        final OffsetDateTime offsetDateTime = OffsetDateTime.of(localDateTime, ZoneOffset.ofHoursMinutes(5, 30));
        final String offsetDateTimeText = Dates.DTF.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
        assertEquals("2025-01-15T10:30:45+05:30", offsetDateTimeText);
        assertEquals(offsetDateTime, Dates.DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(offsetDateTimeText));

        final ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.of("Asia/Kolkata"));
        final String zonedDateTimeText = Dates.DTF.ISO_ZONED_DATE_TIME.format(zonedDateTime);
        assertEquals("2025-01-15T10:30:45+05:30[Asia/Kolkata]", zonedDateTimeText);
        assertEquals(zonedDateTime, Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(zonedDateTimeText));

        final Instant instant = Instant.parse("2025-01-15T05:00:45.123Z");
        final String instantText = Dates.DTF.ISO_8601_TIMESTAMP.format(instant);
        assertEquals("2025-01-15T05:00:45.123Z", instantText);
        assertEquals(instant, Dates.DTF.ISO_8601_TIMESTAMP.parseToInstant(instantText));
    }

    @Test
    public void formatParseExamples_localTimeFraction_showsDefaultAndExactPrecision() {
        final LocalTime localTime = LocalTime.of(10, 30, 45, 123_456_789);

        final String defaultText = Dates.DTF.LOCAL_TIME.format(localTime);
        assertEquals("10:30:45", defaultText);
        assertEquals(LocalTime.of(10, 30, 45), Dates.DTF.LOCAL_TIME.parseToLocalTime(defaultText));

        final Dates.DTF nanosecondTime = Dates.DTF.of("HH:mm:ss.SSSSSSSSS");
        final String exactText = nanosecondTime.format(localTime);
        assertEquals("10:30:45.123456789", exactText);
        assertEquals(localTime, nanosecondTime.parseToLocalTime(exactText));
    }

    @Test
    public void civilTargets_requireTheirOwnFields() {
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToLocalTime("2025-01-15"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToLocalDateTime("2025-01-15"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalTime("2025-01-15"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime("2025-01-15"));
    }

    @Test
    public void instantAndSqlDateTargets_rejectLegacyPartialDatePatterns() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025", "yyyy", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("12-25", "MM-dd", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("14:30:45", "HH:mm:ss", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar("2025-12", "yyyy-MM", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar("2025", "yyyy", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("14:30:45", "HH:mm:ss", UTC));

        assertEquals(Instant.parse("2025-01-15T00:00:00Z"), Dates.parseToTimestamp("2025-01-15", "yyyy-MM-dd", UTC).toInstant());
        assertEquals(52_245_000L, Dates.parseToTime("14:30:45", "HH:mm:ss", UTC).getTime());
    }

    @Test
    public void calendarTargets_preserveTextualZoneAndUseArgumentOnlyAsFallback() {
        final String offsetText = "2025-01-15T10:30:45+05:30";
        final Calendar calendar = Dates.parseToCalendar(offsetText, Dates.ISO_OFFSET_DATE_TIME_FORMAT, UTC);
        final Calendar gregorian = Dates.parseToGregorianCalendar(offsetText, Dates.ISO_OFFSET_DATE_TIME_FORMAT, UTC);

        assertEquals(5 * 60 * 60 * 1000 + 30 * 60 * 1000, calendar.getTimeZone().getRawOffset());
        assertEquals(calendar.getTimeInMillis(), gregorian.getTimeInMillis());
        assertEquals(calendar.getTimeZone().getRawOffset(), gregorian.getTimeZone().getRawOffset());
        assertEquals(330, Dates.parseToXMLGregorianCalendar(offsetText, Dates.ISO_OFFSET_DATE_TIME_FORMAT, UTC).getTimezone());

        final String zonedText = "2025-01-15T10:30:45+05:30[Asia/Kolkata]";
        final Calendar dtfCalendar = Dates.DTF.ISO_ZONED_DATE_TIME.parseToCalendar(zonedText, UTC);
        assertEquals("Asia/Kolkata", dtfCalendar.getTimeZone().getID());
        assertEquals("Asia/Kolkata", Dates.parseToCalendar(zonedText, Dates.ISO_ZONED_DATE_TIME_FORMAT, UTC).getTimeZone().getID());
        assertEquals(ZonedDateTime.parse(zonedText).toInstant(), Dates.parseToJUDate(zonedText, Dates.ISO_ZONED_DATE_TIME_FORMAT, UTC).toInstant());
        assertEquals(zonedText, Dates.format(java.util.Date.from(ZonedDateTime.parse(zonedText).toInstant()), Dates.ISO_ZONED_DATE_TIME_FORMAT,
                TimeZone.getTimeZone("Asia/Kolkata")));

        final Calendar fallbackCalendar = Dates.DTF.LOCAL_DATE_TIME.parseToCalendar("2025-01-15 10:30:45", UTC);
        assertEquals("UTC", fallbackCalendar.getTimeZone().getID());

        final TimeZone losAngeles = TimeZone.getTimeZone("America/Los_Angeles");
        final Calendar literalBracket = Dates.parseToCalendar("2025-01-15 10:30:45 [UTC]", "yyyy-MM-dd HH:mm:ss '[UTC]'", losAngeles);
        assertEquals("America/Los_Angeles", literalBracket.getTimeZone().getID());
    }

    // -----------------------------------------------------------------
    // Custom java.time patterns retain their own y (year-of-era) versus u (proleptic-year) semantics.
    // -----------------------------------------------------------------

    @Test
    public void customPatternYearTokens_retainDateTimeFormatterSemantics() {
        assertEquals(LocalDate.of(2023, 1, 1), Dates.parseToLocalDate("02023-01-01", "yyyyy-MM-dd"));
        assertEquals(LocalDate.of(2023, 1, 1), Dates.parseToLocalDate("2023-01-01", "yyyy-MM-dd"));
        assertEquals(Dates.DTF.of("yyyyy-MM-dd").parseToLocalDate("02023-01-01"), Dates.parseToLocalDate("02023-01-01", "yyyyy-MM-dd"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate("0000-01-01", Dates.LOCAL_DATE_FORMAT));
        assertEquals(LocalDate.of(0, 1, 1), Dates.parseToLocalDate("0000-01-01", "uuuu-MM-dd"));
    }

    // -----------------------------------------------------------------
    // B1 (pinned contract): parseToDate/parseToTime retain the resolved instant exactly;
    // epoch-millisecond text goes through parseEpochMillis/create*. Conventional JDBC
    // civil values remain available through parseToLocalDate/parseToLocalTime + valueOf.
    // -----------------------------------------------------------------

    @Test
    public void parseToDate_numericString_throws() {
        // Bare numeric text is ambiguous: rejected; epoch millis need the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("1234"));
        assertEquals(1234L, Dates.parseEpochMillis("1234"));
    }

    @Test
    public void parseToTime_numericString_throws() {
        // Bare numeric text is ambiguous: rejected; epoch millis need the explicit API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("1234"));
        assertEquals(1234L, Dates.parseEpochMillis("1234"));
    }

    @Test
    public void createDateAndTime_longFactories_useMillisAsIs() {
        assertEquals(1234L, Dates.createDate(1234L).getTime());
        assertEquals(1234L, Dates.createTime(1234L).getTime());
    }

    @Test
    public void parseToDate_formattedString_retainsTimeOfDay() {
        final java.sql.Date date = Dates.parseToDate("2023-07-15 10:30:45", "yyyy-MM-dd HH:mm:ss", UTC);
        assertEquals(Instant.parse("2023-07-15T10:30:45Z").toEpochMilli(), date.getTime());
        assertEquals(Dates.parseToJUDate("2023-07-15 10:30:45", "yyyy-MM-dd HH:mm:ss", UTC).getTime(), date.getTime());
    }

    @Test
    public void parseToDate_offsetText_retainsResolvedInstant() {
        // 2025-01-01T00:30:00+14:00 identifies 2024-12-31T10:30:00Z, retained without normalization.
        final java.sql.Date date = Dates.parseToDate("2025-01-01T00:30:00+14:00");
        assertEquals(Instant.parse("2024-12-31T10:30:00Z").toEpochMilli(), date.getTime());
        // The field-preserving alternative remains available:
        assertEquals(LocalDate.of(2025, 1, 1), Dates.parseToLocalDate("2025-01-01T00:30:00+14:00"));
    }

    @Test
    public void parseToTime_fractionalSecond_preservesMillis() {
        final java.sql.Time time = Dates.parseToTime("14:30:45.123", "HH:mm:ss.SSS", UTC);
        assertEquals(52_245_123L, time.getTime());
    }

    @Test
    public void dtfParseToTime_fractionalSecond_preservesMillis() {
        final java.sql.Time time = Dates.DTF.ISO_8601_TIMESTAMP.parseToTime("1970-01-01T00:00:00.123Z");
        assertEquals(123L, time.getTime());
    }

    @Test
    public void dtfParseToTime_timeOnlyWithOffset_honorsWrittenOffset() {
        // A written offset is authoritative even for time-only text and its anchored instant is retained.
        final java.sql.Time time = Dates.DTF.of("HH:mm:ssXXX").parseToTime("00:30:00+14:00", UTC);
        final long expected = java.time.LocalTime.of(0, 30).atDate(LocalDate.of(1970, 1, 1)).atZone(ZoneOffset.of("+14:00")).toInstant().toEpochMilli();
        assertEquals(expected, time.getTime());
    }

    // -----------------------------------------------------------------
    // B3 (fixed): round() moves the value to the nearer of the two adjacent
    // valid boundaries by exact elapsed distance; an exact tie rounds up.
    // -----------------------------------------------------------------

    @Test
    public void round_toYear_usesElapsedDistanceToYearBoundaries() {
        // 181 days after 2023-01-01, 184 days before 2024-01-01 (2023 is not a leap year).
        Calendar beforeMidpoint = Dates.parseToCalendar("2023-07-01 00:00:00", "yyyy-MM-dd HH:mm:ss", UTC);
        assertEquals("2023-01-01 00:00:00", Dates.format(Dates.round(beforeMidpoint, Calendar.YEAR), "yyyy-MM-dd HH:mm:ss", UTC));

        // The midpoint of 2023 is July 2 at 12:00 (182.5 days); July 3 is past it.
        Calendar pastMidpoint = Dates.parseToCalendar("2023-07-03 00:00:00", "yyyy-MM-dd HH:mm:ss", UTC);
        assertEquals("2024-01-01 00:00:00", Dates.format(Dates.round(pastMidpoint, Calendar.YEAR), "yyyy-MM-dd HH:mm:ss", UTC));
    }

    @Test
    public void round_toMonth_bothSidesOfJanuaryMidpoint() {
        // January has 31 days, so the midpoint is January 16 at 12:00.
        Calendar beforeMid = Dates.parseToCalendar("2023-01-16 11:00:00", "yyyy-MM-dd HH:mm:ss", UTC);
        assertEquals("2023-01-01 00:00:00", Dates.format(Dates.round(beforeMid, Calendar.MONTH), "yyyy-MM-dd HH:mm:ss", UTC));

        Calendar pastMid = Dates.parseToCalendar("2023-01-16 23:59:59", "yyyy-MM-dd HH:mm:ss", UTC);
        assertEquals("2023-02-01 00:00:00", Dates.format(Dates.round(pastMid, Calendar.MONTH), "yyyy-MM-dd HH:mm:ss", UTC));
    }

    @Test
    public void round_toMonth_exactTie_roundsUpToLaterBoundary() {
        // January 16 at 12:00 is exactly 15.5 days from both the January 1 and February 1 boundaries.
        Calendar exactTie = Dates.parseToCalendar("2023-01-16 12:00:00", "yyyy-MM-dd HH:mm:ss", UTC);
        assertEquals("2023-02-01 00:00:00", Dates.format(Dates.round(exactTie, Calendar.MONTH), "yyyy-MM-dd HH:mm:ss", UTC));
    }

    // -----------------------------------------------------------------
    // D2 (fixed): one zone policy — the live machine default everywhere;
    // Calendar overloads honor the calendar's own zone.
    // -----------------------------------------------------------------

    @Test
    public void legacyParseAndFormat_followLiveDefaultZone() {
        final TimeZone prior = TimeZone.getDefault();
        final TimeZone kiritimati = TimeZone.getTimeZone("Pacific/Kiritimati"); // +14 (modern rules)
        try {
            TimeZone.setDefault(UTC);
            final long parsedUtc = Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss").getTime();
            assertEquals(1736937045000L, parsedUtc); // 2025-01-15T10:30:45Z
            assertEquals("2025-01-15 10:30:45", Dates.format(new java.util.Date(parsedUtc), "yyyy-MM-dd HH:mm:ss"));

            // Changing the machine default mid-run must affect subsequent no-zone operations.
            TimeZone.setDefault(kiritimati);
            assertEquals("2025-01-16 00:30:45", Dates.format(new java.util.Date(parsedUtc), "yyyy-MM-dd HH:mm:ss"));
            assertEquals(Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", kiritimati).getTime(),
                    Dates.parseToJUDate("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss").getTime());
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    @Test
    public void format_calendarOverload_honorsCalendarZone() {
        final Calendar ny = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        ny.setTimeInMillis(1736937045000L); // 2025-01-15T10:30:45Z = 05:30:45 EST
        assertEquals("2025-01-15 05:30:45", Dates.format(ny, "yyyy-MM-dd HH:mm:ss"));
        assertEquals(Dates.DTF.LOCAL_DATE_TIME.format(ny), Dates.format(ny, "yyyy-MM-dd HH:mm:ss"));

        final StringBuilder defaultZoneOutput = new StringBuilder();
        Dates.formatTo(ny, "yyyy-MM-dd HH:mm:ss", defaultZoneOutput);
        assertEquals("2025-01-15 05:30:45", defaultZoneOutput.toString());

        final StringBuilder localeOutput = new StringBuilder();
        Dates.formatTo(ny, "yyyy-MM-dd HH:mm:ss", null, Locale.US, localeOutput);
        assertEquals("2025-01-15 05:30:45", localeOutput.toString());

        // Explicit zone still overrides the calendar's zone.
        assertEquals("2025-01-15 10:30:45", Dates.format(ny, "yyyy-MM-dd HH:mm:ss", UTC));

        final StringBuilder explicitZoneOutput = new StringBuilder();
        Dates.formatTo(ny, "yyyy-MM-dd HH:mm:ss", UTC, explicitZoneOutput);
        assertEquals("2025-01-15 10:30:45", explicitZoneOutput.toString());
    }

    @Test
    public void format_calendarOverload_zoneFixedFormatsStayUtc() {
        final Calendar ny = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        ny.setTimeInMillis(1736937045000L);
        assertEquals("2025-01-15T10:30:45Z", Dates.format(ny, Dates.ISO_8601_DATE_TIME_FORMAT));
    }

    @Test
    public void createCalendar_preservesSourceZone() {
        final Calendar ny = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        ny.setTimeInMillis(1736937045000L);
        final Calendar copy = Dates.createCalendar(ny);
        assertEquals(ny.getTimeInMillis(), copy.getTimeInMillis());
        assertEquals("America/New_York", copy.getTimeZone().getID());
    }

    // -----------------------------------------------------------------
    // D3 (pinned contract): automatic parsing rejects bare numeric text as
    // ambiguous (epoch millis go through parseEpochMillis) and rejects empty
    // text with IllegalArgumentException; a null reference and the "null"
    // marker parse to Java null.
    // -----------------------------------------------------------------

    @Test
    public void parseToDate_yearLikeNumericString_throws() {
        // "2023" is bare numeric text: ambiguous (a year? epoch millis?), so it is rejected.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("2023"));
        assertEquals(2023L, Dates.parseEpochMillis("2023"));
    }

    @Test
    public void parseToDate_nullMarkers_throw() {
        assertNull(Dates.parseToDate(null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(""));
        assertNull(Dates.parseToDate("null"));
        assertNull(Dates.parseToDate("NULL"));
        assertEquals(0L, Dates.parseEpochMillis(null));
        assertEquals(0L, Dates.parseEpochMillis("null"));
        assertEquals(Instant.EPOCH, Dates.parseEpochMillisToInstant("NULL"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseEpochMillis(""));
    }

    @Test
    public void parseToTime_nullMarkers_throw() {
        assertNull(Dates.parseToTime(null));
        assertNull(Dates.parseToTime("null"));
    }
}
