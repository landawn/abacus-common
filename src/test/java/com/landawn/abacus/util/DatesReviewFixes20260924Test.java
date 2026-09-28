package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dates.DTF;

/**
 * Pins the Dates fixes of the 2026-09-24 cross review: C-014 (one "auto-detected format" per failure),
 * C-016 (one DST gap/overlap wording on every resolver), C-017, C-025, C-027, C-028, C-030 (documented
 * facts), C-041 (zoneOf fallback), C-043 (private constructor), C-045 (year-range diagnostic before the
 * JDBC shortcut), C-047 (Calendar default year-range wording), C-052 (DTF error index printed once) and
 * C-053 (fixed-offset zone rejection on DTF.format); cycle 2: C-078 (no fallback zone named for text that
 * carries its own), C-079 (pattern-less year-range wording on the explicit-zone Date and XML defaults),
 * C-080 (the fractional-offset auto branch reports "the auto-detected format" like every other), C-081
 * (the JDBC auto branch owns every fraction length), C-083 (Calendar range checks reject a non-lenient
 * calendar with invalid fields) and C-119 (same-ID rules mismatch names the two classes); cycle 3: C-128
 * (createXMLGregorianCalendar snapshots only a supplied zone), C-129 (create*(Calendar) pass on a non-lenient
 * calendar's own rejection), C-133 (parseISO8601 names no fallback zone for text carrying its own offset or
 * designator), C-135 (one overflow message shape across the add* and roll family), C-139 (the java.time targets
 * report a malformed auto-detected offset in the legacy "Format '...' requires" family) and C-144/C-145 (DTF
 * zone resolution unchanged without the dead offset fallbacks; HTTP_DATE's accessor carries GMT).
 */
public class DatesReviewFixes20260924Test extends TestBase {

    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone KOLKATA = TimeZone.getTimeZone("Asia/Kolkata");

    private static int count(final String text, final String token) {
        int count = 0;

        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
            count++;
        }

        return count;
    }

    private static String messageOf(final Executable call) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
        assertNotNull(e.getMessage());
        return e.getMessage();
    }

    // ---------------------------------------------------------------------------------------------
    // C-014: every auto-detected failure says "the auto-detected format" exactly once
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c014_autoDetectedFailuresNameTheAutoDetectedFormatExactlyOnce() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            TimeZone.setDefault(NEW_YORK);

            for (final String text : new String[] { "2025-01-15T10:30:45.123+01:00[Asia/Kolkata]", "2025-01-15T10:30:45+01:00[Asia/Kolkata]",
                    "2025-13-15T10:30:45.5" }) {
                for (final Executable call : new Executable[] { () -> Dates.parseToTimestamp(text), () -> Dates.parseToJUDate(text),
                        () -> Dates.parseToCalendar(text), () -> Dates.parseToInstant(text) }) {
                    final String message = messageOf(call);

                    assertEquals(1, count(message, "the auto-detected format"), () -> "exactly one auto-detected mention: " + message);
                    assertFalse(message.contains("with pattern"), () -> "a pattern must not be presented as the caller's: " + message);
                    assertFalse(message.contains("with format"), () -> "no format was supplied: " + message);
                    assertEquals(1, count(message, "Cannot parse"), () -> "not nested: " + message);
                }
            }

            // The diagnostic itself survives the unwrapping.
            final String zoned = messageOf(() -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+01:00[Asia/Kolkata]"));
            assertTrue(zoned.contains("Offset +01:00 is not valid for local date-time 2025-01-15T10:30:45.123 in zone Asia/Kolkata"), zoned);

            final String month = messageOf(() -> Dates.parseToJUDate("2025-13-15T10:30:45.5"));
            assertTrue(month.contains("Invalid value for MonthOfYear"), month);
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    @Test
    public void c014_explicitFormatIsStillNamedOnceWithoutTheDtfPattern() {
        final String named = messageOf(
                () -> Dates.parseToJUDate("2025-01-15T10:30:45+01:00[Asia/Kolkata]", Dates.ISO_ZONED_DATE_TIME_FORMAT, NEW_YORK));

        assertTrue(named.contains("with format '" + Dates.ISO_ZONED_DATE_TIME_FORMAT + "'"), named);
        assertFalse(named.contains("with pattern"), named);
        assertEquals(1, count(named, "Cannot parse"), named);
        assertTrue(named.contains("Offset +01:00 is not valid"), named);
    }

    // ---------------------------------------------------------------------------------------------
    // C-016: one DST gap/overlap wording whichever resolver reports it
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c016_overlapAndGapAreTaggedOnEveryAutoDetectedPath() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            TimeZone.setDefault(NEW_YORK);

            for (final String text : new String[] { "2025-11-02 01:30:00", "2025-11-02 01:30:00.5", "2025-11-02T01:30:00.500", "2025-11-02T01:30:00.5",
                    "2025-11-02 01:30:00.500" }) {
                for (final Executable call : new Executable[] { () -> Dates.parseToJUDate(text), () -> Dates.parseToTimestamp(text),
                        () -> Dates.parseToCalendar(text), () -> Dates.parseToInstant(text) }) {
                    final String message = messageOf(call);
                    assertTrue(message.contains("DST overlap"), () -> "overlap tag expected for " + text + ": " + message);
                    assertTrue(message.contains("Ambiguous local date-time 2025-11-02T01:30"), message);
                    assertTrue(message.contains("valid offsets are [-04:00, -05:00]"), message);
                }
            }

            for (final String text : new String[] { "2025-03-09 02:30:00.5", "2025-03-09 02:30:00", "2025-03-09T02:30:00.5" }) {
                for (final Executable call : new Executable[] { () -> Dates.parseToJUDate(text), () -> Dates.parseToTimestamp(text),
                        () -> Dates.parseToCalendar(text), () -> Dates.parseToInstant(text) }) {
                    final String message = messageOf(call);
                    assertTrue(message.contains("DST gap"), () -> "gap tag expected for " + text + ": " + message);
                    assertTrue(message.contains("Nonexistent local date-time 2025-03-09T02:30"), message);
                }
            }

            // The C2-D2 wrapper stays on the fractional shapes the JDBC and variable-fraction branches own.
            final String wrapped = messageOf(() -> Dates.parseToTimestamp("2025-11-02 01:30:00.5"));
            assertTrue(wrapped.startsWith("Cannot parse \"2025-11-02 01:30:00.5\" with the auto-detected format in time zone America/New_York: "), wrapped);
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    @Test
    public void c016_dtfAndExplicitZoneResolversCarryTheSameTags() {
        final String gap = messageOf(() -> DTF.LOCAL_DATE_TIME.parseToInstant("2025-03-09 02:30:00", NEW_YORK));
        assertTrue(gap.endsWith("Nonexistent local date-time 2025-03-09T02:30 in zone America/New_York (DST gap)"), gap);

        final String overlap = messageOf(() -> DTF.LOCAL_DATE_TIME.parseToZonedDateTime("2025-11-02 01:30:00", NEW_YORK));
        assertTrue(overlap.endsWith("Ambiguous local date-time 2025-11-02T01:30 in zone America/New_York (DST overlap); valid offsets are [-04:00, -05:00]"),
                overlap);

        final String jdbc = messageOf(() -> Dates.parseToTimestamp("2025-11-02 01:30:00.25", null, NEW_YORK));
        assertTrue(jdbc.contains("(DST overlap)"), jdbc);

        // A preferred offset that is valid resolves; an invalid one keeps its own (unchanged) wording.
        assertEquals(Instant.parse("2025-11-02T05:30:00Z"), DTF.ISO_OFFSET_DATE_TIME.parseToInstant("2025-11-02T01:30:00-04:00"));
        assertEquals(Instant.parse("2025-11-02T06:30:00Z"), DTF.ISO_OFFSET_DATE_TIME.parseToInstant("2025-11-02T01:30:00-05:00"));

        final String invalid = messageOf(() -> DTF.ISO_ZONED_DATE_TIME.parseToInstant("2025-11-02T01:30:00-03:00[America/New_York]"));
        assertTrue(invalid.contains("Offset -03:00 is not valid for local date-time 2025-11-02T01:30 in zone America/New_York"), invalid);
    }

    // ---------------------------------------------------------------------------------------------
    // C-017: the documented split between the Date and the Calendar overloads
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c017_dateOverloadsUseTheRegisteredRulesButCalendarOverloadsRejectACustomizedKnownId() {
        final SimpleTimeZone customized = new SimpleTimeZone(-5 * 3600000, "America/New_York", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0,
                3600000);
        final java.util.Date d1 = new java.util.Date(1736937045000L);
        final java.util.Date d2 = new java.util.Date(1736937045000L + 3600000L);
        final TimeZone saved = TimeZone.getDefault();

        try {
            TimeZone.setDefault(customized);
            assertTrue(Dates.isSameDay(d1, d2));
            assertTrue(Dates.isSameMonth(d1, d2));
            assertTrue(Dates.isSameYear(d1, d2));
            assertFalse(Dates.isLastDayOfMonth(d1));
        } finally {
            TimeZone.setDefault(saved);
        }

        final Calendar c1 = Calendar.getInstance(customized);
        c1.setTime(d1);
        final Calendar c2 = Calendar.getInstance(customized);
        c2.setTime(d2);

        for (final Executable call : new Executable[] { () -> Dates.isSameDay(c1, c2), () -> Dates.isSameMonth(c1, c2), () -> Dates.isSameYear(c1, c2) }) {
            final String message = messageOf(call);
            assertTrue(message.contains("America/New_York"), message);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // C-025: the bracketed-zone form may omit the seconds field on every target
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c025_bracketedZoneFormWithoutSecondsParsesOnEveryTarget() {
        assertEquals(Instant.parse("2025-01-15T10:30:00Z"), Dates.parseToInstant("2025-01-15T10:30Z[UTC]"));
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30), Dates.parseToLocalDateTime("2025-01-15T10:30Z[UTC]"));
        assertEquals(LocalTime.of(10, 30), Dates.parseToLocalTime("2025-01-15T10:30Z[UTC]"));
        assertEquals(LocalDate.of(2025, 1, 15), Dates.parseToLocalDate("2025-01-15T10:30Z[UTC]"));
        assertEquals(1736937000000L, Dates.parseToTimestamp("2025-01-15T10:30Z[UTC]").getTime());
        assertEquals(1736937000000L, Dates.parseToJUDate("2025-01-15T10:30Z[UTC]").getTime());
        assertEquals(Instant.parse("2025-01-15T05:00:00Z"), Dates.parseToInstant("2025-01-15T10:30+05:30[Asia/Kolkata]"));

        // Without the bracket the seconds-less form reaches only the legacy fallback reader.
        assertEquals(1736937000000L, Dates.parseToTimestamp("2025-01-15T10:30Z").getTime());
        assertEquals(1736937000000L, Dates.parseToJUDate("2025-01-15T10:30Z").getTime());
        assertTrue(messageOf(() -> Dates.parseToInstant("2025-01-15T10:30Z")).contains("Cannot detect"));
        assertTrue(messageOf(() -> Dates.parseToLocalDateTime("2025-01-15T10:30Z")).contains("Cannot detect"));
    }

    // ---------------------------------------------------------------------------------------------
    // C-027: the documented example values of the DTF (CharSequence, TimeZone) trio
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c027_dtfTimeZoneOverloadExamplesHold() {
        assertEquals(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.UTC), DTF.LOCAL_DATE_TIME.parseToOffsetDateTime("2023-12-25 14:30:45", UTC));
        assertEquals(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)),
                DTF.LOCAL_DATE_TIME.parseToOffsetDateTime("2023-12-25 14:30:45", KOLKATA));
        assertEquals(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)),
                DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T14:30:45+05:30", UTC));
        assertEquals(ZonedDateTime.of(2023, 12, 25, 14, 30, 45, 0, KOLKATA.toZoneId()), DTF.LOCAL_DATE_TIME.parseToZonedDateTime("2023-12-25 14:30:45", KOLKATA));
        assertEquals(ZonedDateTime.of(2023, 12, 25, 14, 30, 45, 0, KOLKATA.toZoneId()),
                DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T14:30:45+05:30[Asia/Kolkata]", UTC));
        assertEquals(1703514645000L, DTF.LOCAL_DATE_TIME.parseToInstant("2023-12-25 14:30:45", UTC).toEpochMilli());
        assertEquals(1703494845000L, DTF.LOCAL_DATE_TIME.parseToInstant("2023-12-25 14:30:45", KOLKATA).toEpochMilli());
        assertThrows(IllegalArgumentException.class, () -> DTF.ISO_8601_DATE_TIME.parseToInstant("2023-12-25T14:30:45Z", KOLKATA));

        for (final Executable call : new Executable[] { () -> DTF.LOCAL_DATE_TIME.parseToOffsetDateTime("", UTC),
                () -> DTF.LOCAL_DATE_TIME.parseToZonedDateTime("", UTC), () -> DTF.LOCAL_DATE_TIME.parseToInstant("", UTC) }) {
            assertTrue(messageOf(call).contains("empty"));
        }

        assertNull(DTF.LOCAL_DATE_TIME.parseToOffsetDateTime((CharSequence) null, UTC));
        assertNull(DTF.LOCAL_DATE_TIME.parseToZonedDateTime((CharSequence) null, UTC));
        assertNull(DTF.LOCAL_DATE_TIME.parseToInstant((CharSequence) null, UTC));
        assertNull(DTF.LOCAL_DATE_TIME.parseToInstant("null", UTC));
    }

    // ---------------------------------------------------------------------------------------------
    // C-028: the offset-versus-zone check applies to the civil-field parsers too
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c028_civilParsersRejectAnOffsetInconsistentWithItsZone() {
        final String text = "2023-07-01T12:00:00-08:00[America/Los_Angeles]";

        for (final Executable call : new Executable[] { () -> DTF.ISO_ZONED_DATE_TIME.parseToLocalDate(text),
                () -> DTF.ISO_ZONED_DATE_TIME.parseToLocalTime(text), () -> DTF.ISO_ZONED_DATE_TIME.parseToLocalDateTime(text) }) {
            assertTrue(messageOf(call).contains("Offset -08:00 is not valid"));
        }

        assertEquals(LocalDate.of(2023, 7, 1), DTF.ISO_ZONED_DATE_TIME.parseToLocalDate("2023-07-01T12:00:00-07:00[America/Los_Angeles]"));
    }

    // ---------------------------------------------------------------------------------------------
    // C-030: the zone-ID constants are region IDs, not ZoneOffset.UTC
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c030_zoneIdConstantsAreRegionIdsWithUtcRules() {
        assertEquals("UTC", Dates.UTC_ZONE_ID.getId());
        assertEquals("GMT", Dates.GMT_ZONE_ID.getId());
        assertNotEquals(ZoneOffset.UTC, Dates.UTC_ZONE_ID);
        assertNotEquals(ZoneOffset.UTC, Dates.GMT_ZONE_ID);
        assertNotEquals(Dates.UTC_ZONE_ID, Dates.GMT_ZONE_ID);
        assertEquals(ZoneOffset.UTC, Dates.UTC_ZONE_ID.normalized());
        assertEquals(ZoneOffset.UTC, Dates.GMT_ZONE_ID.normalized());
        assertEquals(ZoneOffset.UTC.getRules(), Dates.UTC_ZONE_ID.getRules());
        assertEquals(ZoneOffset.UTC.getRules(), Dates.GMT_ZONE_ID.getRules());
        assertEquals("1970-01-01T00:00Z[UTC]", ZonedDateTime.ofInstant(Instant.EPOCH, Dates.UTC_ZONE_ID).toString());
        assertEquals("1970-01-01T00:00Z[GMT]", ZonedDateTime.ofInstant(Instant.EPOCH, Dates.GMT_ZONE_ID).toString());
    }

    // ---------------------------------------------------------------------------------------------
    // C-041: the Calendar truncation comparison keeps the null-zone fallback through zoneOf
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c041_calendarWithNullZoneIsComparedInTheLiveDefaultZone() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            TimeZone.setDefault(NEW_YORK);

            final long a = Instant.parse("2025-01-15T23:30:00Z").toEpochMilli(); // 2025-01-15 18:30 New York
            final long b = Instant.parse("2025-01-16T00:30:00Z").toEpochMilli(); // 2025-01-15 19:30 New York

            final Calendar nullZone1 = new GregorianCalendar(UTC) {
                @Override
                public TimeZone getTimeZone() {
                    return null;
                }
            };
            nullZone1.setTimeInMillis(a);

            final Calendar nullZone2 = new GregorianCalendar(UTC) {
                @Override
                public TimeZone getTimeZone() {
                    return null;
                }
            };
            nullZone2.setTimeInMillis(b);

            assertEquals(0, Dates.truncatedCompareTo(nullZone1, nullZone2, Calendar.DAY_OF_MONTH));
            assertEquals(0, Dates.truncatedCompareTo(new java.util.Date(a), new java.util.Date(b), Calendar.DAY_OF_MONTH));
            assertTrue(Dates.truncatedEquals(nullZone1, nullZone2, Calendar.DAY_OF_MONTH));

            // The same instants in the calendar's own UTC zone are on different days.
            assertNotEquals(0, Dates.truncatedCompareTo(Dates.createCalendar(a, UTC), Dates.createCalendar(b, UTC), Calendar.DAY_OF_MONTH));
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // C-043: the utility-class constructor is private
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c043_constructorIsPrivate() throws Exception {
        assertTrue(Modifier.isPrivate(Dates.class.getDeclaredConstructor().getModifiers()));
    }

    // ---------------------------------------------------------------------------------------------
    // C-045: year 0000 reports the year-range diagnostic on every entry point
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c045_yearZeroReportsTheSameYearRangeMessageOnTimestampAndJUDate() {
        final String expected = "Format 'yyyy-MM-dd HH:mm:ss' requires a Common Era year from 0001 through 9999 written as exactly four digits: \"0000-01-15 10:30:45\"";

        assertEquals(expected, messageOf(() -> Dates.parseToTimestamp("0000-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC)));
        assertEquals(expected, messageOf(() -> Dates.parseToJUDate("0000-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC)));
        assertEquals(expected, messageOf(() -> Dates.parseToTimestamp("0000-01-15 10:30:45")));
        assertEquals(expected, messageOf(() -> Dates.parseToJUDate("0000-01-15 10:30:45")));

        final String fractional = "Format 'yyyy-MM-dd HH:mm:ss.SSS' requires a Common Era year from 0001 through 9999 written as exactly four digits: ";
        assertEquals(fractional + "\"0000-01-15 10:30:45.5\"", messageOf(() -> Dates.parseToTimestamp("0000-01-15 10:30:45.5")));
        assertEquals(fractional + "\"0000-01-15 10:30:45.5\"", messageOf(() -> Dates.parseToJUDate("0000-01-15 10:30:45.5")));
        assertEquals(fractional + "\"0000-01-15 10:30:45.5\"", messageOf(() -> Dates.parseToCalendar("0000-01-15 10:30:45.5")));
        assertEquals(fractional + "\"0000-01-15 10:30:45.123\"",
                messageOf(() -> Dates.parseToTimestamp("0000-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC)));
        assertEquals(fractional + "\"0000-01-15 10:30:45.123\"",
                messageOf(() -> Dates.parseToJUDate("0000-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC)));

        for (final Executable call : new Executable[] { () -> Dates.parseToTimestamp("0000-01-15 10:30:45"),
                () -> Dates.parseToTimestamp("0000-01-15 10:30:45.5"), () -> Dates.parseToTimestamp("0000-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC) }) {
            assertFalse(messageOf(call).contains("JDBC timestamp year"));
        }

        // The boundaries and the ordinary case are untouched.
        assertEquals(Dates.parseToJUDate("0001-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime(),
                Dates.parseToTimestamp("0001-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(-62134349355000L, Dates.parseToTimestamp("0001-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045000L, Dates.parseToTimestamp("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045500L, Dates.parseToTimestamp("2025-01-15 10:30:45.5", null, UTC).getTime());
        assertEquals(Instant.parse("9999-12-31T10:30:45Z").toEpochMilli(), Dates.parseToTimestamp("9999-12-31 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertNull(Dates.parseToTimestamp((String) null, Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertTrue(messageOf(() -> Dates.parseToTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT, UTC)).contains("empty"));
    }

    // ---------------------------------------------------------------------------------------------
    // C-047: the Calendar default names no pattern in its year-range error
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c047_calendarDefaultYearRangeErrorUsesTheIso8601Wording() {
        final String expected = "ISO 8601 formatting supports Common Era years from 0001 through 9999; got instant ";

        final String bce = messageOf(() -> Dates.format(Dates.createCalendar(-62135596800001L, UTC)));
        assertEquals(expected + "0000-12-31T23:59:59.999Z", bce);
        assertFalse(bce.contains("Format '"), bce);

        assertEquals(expected + "0000-12-31T23:59:59.999Z", messageOf(() -> Dates.format(Dates.createCalendar(-62135596800001L, UTC), null, UTC)));
        assertEquals(expected + "+10000-01-01T00:00:00Z", messageOf(() -> Dates.format(Dates.createCalendar(253402300800000L, UTC))));
        assertEquals(expected + "0000-12-31T23:59:59.999Z", messageOf(() -> Dates.formatTo(Dates.createCalendar(-62135596800001L, UTC), new StringBuilder())));

        // Same wording as the java.util.Date default, and the boundary itself still formats.
        assertEquals(bce, messageOf(() -> Dates.format(new java.util.Date(-62135596800001L))));
        assertEquals("0001-01-01T00:00:00Z[UTC]", Dates.format(Dates.createCalendar(-62135596800000L, UTC)));
        assertEquals("9999-12-31T23:59:59.999Z[UTC]", Dates.format(Dates.createCalendar(253402300799999L, UTC)));
    }

    // ---------------------------------------------------------------------------------------------
    // C-052: the DTF failure prints the error index once
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c052_dtfParseFailurePrintsTheErrorIndexOnce() {
        final String compact = messageOf(() -> DTF.of("uuuu-MM-dd'T'HH:mm:ssXXXXX").parseToOffsetDateTime("2023-12-25T14:30:45+0530"));
        assertEquals("Cannot parse \"2023-12-25T14:30:45+0530\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX': Text '2023-12-25T14:30:45+0530' could not be parsed at index 19",
                compact);
        assertEquals(1, count(compact, "at index 19"), compact);

        final String truncated = messageOf(() -> DTF.LOCAL_DATE.parseToLocalDate("2023-02"));
        assertEquals(1, count(truncated, "at index"), truncated);
        assertTrue(truncated.endsWith("could not be parsed at index 7"), truncated);

        final String trailing = messageOf(() -> DTF.LOCAL_DATE.parseToLocalDate("2023-02-01x"));
        assertEquals(1, count(trailing, "at index"), trailing);
        assertTrue(trailing.endsWith("unparsed text found at index 10"), trailing);

        // A failure whose JDK message carries no position keeps the single local index.
        final String invalid = messageOf(() -> DTF.LOCAL_DATE.parseToLocalDate("2023-02-30"));
        assertEquals(1, count(invalid, "at index"), invalid);
        assertTrue(invalid.contains("with pattern 'uuuu-MM-dd' at index 0: Text '2023-02-30' could not be parsed: Invalid date 'FEBRUARY 30'"), invalid);

        final String http = messageOf(() -> DTF.HTTP_DATE.parseToInstant("Wed, 15 Jan 2025 10:30:45"));
        assertEquals(1, count(http, "at index"), http);
        assertTrue(http.contains("at index 25: HTTP-date must use the canonical 29-character IMF-fixdate form"), http);

        final String zone = messageOf(() -> DTF.ISO_ZONED_DATE_TIME.parseToInstant("2023-07-01T12:00:00-08:00[America/Los_Angeles]"));
        assertEquals(0, count(zone, "at index"), zone);
    }

    // ---------------------------------------------------------------------------------------------
    // C-053: DTF.format(Date)/format(Calendar) reject a fixed offset no ZoneOffset can express
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c053_dtfFormatRejectsSubSecondAndOutOfRangeFixedOffsets() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            TimeZone.setDefault(new SimpleTimeZone(500, "sub-second"));
            assertTrue(messageOf(() -> DTF.LOCAL_DATE_TIME.format(new java.util.Date(0L))).contains("sub-second offset"));
            assertEquals("1970-01-01T00:00:00Z", DTF.ISO_8601_DATE_TIME.format(new java.util.Date(0L)));

            TimeZone.setDefault(new SimpleTimeZone(19 * 3600000, "beyond"));
            assertTrue(messageOf(() -> DTF.LOCAL_DATE_TIME.format(new java.util.Date(0L))).contains("outside the java.time range"));
        } finally {
            TimeZone.setDefault(saved);
        }

        final Calendar subSecond = Calendar.getInstance(new SimpleTimeZone(500, "sub-second"));
        subSecond.setTimeInMillis(0L);
        assertTrue(messageOf(() -> DTF.LOCAL_DATE_TIME.format(subSecond)).contains("sub-second offset"));

        final Calendar beyond = Calendar.getInstance(new SimpleTimeZone(19 * 3600000, "beyond"));
        beyond.setTimeInMillis(0L);
        assertTrue(messageOf(() -> DTF.LOCAL_DATE_TIME.format(beyond)).contains("outside the java.time range"));
    }

    // ---------------------------------------------------------------------------------------------
    // C-078: the DTF-routed branch of parse() names no fallback zone for text that carries its own
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c078_dtfRoutedBranchNamesNoFallbackZoneForTextThatCarriesItsOwn() {
        final String zoned = "2025-01-15T10:30:45+01:00[Asia/Kolkata]";

        final String explicit = messageOf(() -> Dates.parseToJUDate(zoned, Dates.ISO_ZONED_DATE_TIME_FORMAT, NEW_YORK));
        assertEquals("Cannot parse \"" + zoned + "\" with format '" + Dates.ISO_ZONED_DATE_TIME_FORMAT
                + "': Offset +01:00 is not valid for local date-time 2025-01-15T10:30:45 in zone Asia/Kolkata; valid offsets are [+05:30]", explicit);

        for (final Executable call : new Executable[] { () -> Dates.parseToCalendar(zoned, Dates.ISO_ZONED_DATE_TIME_FORMAT, NEW_YORK),
                () -> Dates.parseToTimestamp(zoned, Dates.ISO_ZONED_DATE_TIME_FORMAT, NEW_YORK), () -> Dates.parseToJUDate(zoned, null, NEW_YORK),
                () -> Dates.parseToJUDate("2025-13-15T10:30:45.5+05:30", null, NEW_YORK),
                () -> Dates.parseToCalendar("2025-13-15T10:30:45.5+05:30", null, NEW_YORK) }) {
            final String message = messageOf(call);
            assertFalse(message.contains("in time zone"), message);
            assertEquals(1, count(message, "Cannot parse"), message);
        }

        // A zone-less shape resolved in the fallback zone still names it.
        assertTrue(messageOf(() -> Dates.parseToJUDate("2025-13-15 10:30:45.5", null, NEW_YORK)).contains("in time zone America/New_York"));
        assertTrue(messageOf(() -> Dates.parseToJUDate("2025-13-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, NEW_YORK)).contains("in time zone America/New_York"));

        // Success is unchanged.
        assertEquals(Instant.parse("2025-01-15T05:00:45Z").toEpochMilli(),
                Dates.parseToJUDate("2025-01-15T10:30:45+05:30[Asia/Kolkata]", Dates.ISO_ZONED_DATE_TIME_FORMAT, NEW_YORK).getTime());
        assertEquals(Instant.parse("2025-01-15T05:00:45.5Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15T10:30:45.5+05:30", null, NEW_YORK).getTime());
    }

    // ---------------------------------------------------------------------------------------------
    // C-079: the explicit-zone Date/SQL and XMLGregorianCalendar defaults name no pattern in their
    // year-range error, exactly as the Calendar default (C-047) and the UTC default do
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c079_explicitZoneDefaultsUseTheIso8601YearRangeWording() {
        final String expected = "ISO 8601 formatting supports Common Era years from 0001 through 9999; got instant ";

        assertEquals(expected + "0000-12-31T23:59:59.999Z", messageOf(() -> Dates.format(new java.util.Date(-62135596800001L), null, UTC)));
        assertEquals(expected + "0000-12-31T23:59:59.999Z", messageOf(() -> Dates.format(new Timestamp(-62135596800001L), null, UTC)));
        assertEquals(expected + "+10000-01-01T00:00:00Z", messageOf(() -> Dates.format(new java.util.Date(253402300800000L), "", UTC)));
        assertEquals(expected + "0000-12-31T23:59:59.999Z",
                messageOf(() -> Dates.formatTo(new java.util.Date(-62135596800001L), null, UTC, new StringBuilder())));

        final XMLGregorianCalendar xml = Dates.createXMLGregorianCalendar(-62135596800001L, UTC);
        assertEquals(expected + "0000-12-31T23:59:59.999Z", messageOf(() -> Dates.format(xml, null, UTC)));
        assertEquals(expected + "0000-12-31T23:59:59.999Z", messageOf(() -> Dates.formatTo(xml, null, UTC, new StringBuilder())));
        assertEquals(expected + "+10000-01-01T00:00:00Z", messageOf(() -> Dates.format(Dates.createXMLGregorianCalendar(253402300800000L, UTC), null, UTC)));

        // The explicit constants keep naming themselves; the Calendar default is unchanged.
        final String constant = "Format 'yyyy-MM-dd'T'HH:mm:ssXXX' supports Common Era years from 0001 through 9999; got instant 0000-12-31T23:59:59.999Z";
        assertEquals(constant, messageOf(() -> Dates.format(new java.util.Date(-62135596800001L), Dates.ISO_OFFSET_DATE_TIME_FORMAT, UTC)));
        assertEquals(constant, messageOf(() -> Dates.format(xml, Dates.ISO_OFFSET_DATE_TIME_FORMAT, UTC)));
        assertEquals(expected + "0000-12-31T23:59:59.999Z", messageOf(() -> Dates.format(Dates.createCalendar(-62135596800001L, UTC), null, UTC)));

        // The boundaries still format, and the lexical XML default is not year-checked.
        assertEquals("0001-01-01T00:00:00Z", Dates.format(new java.util.Date(-62135596800000L), null, UTC));
        assertEquals("0001-01-01T00:00:00Z", Dates.format(Dates.createXMLGregorianCalendar(-62135596800000L, UTC), null, UTC));
        assertEquals("9999-12-31T23:59:59Z", Dates.format(new java.util.Date(253402300799999L), null, UTC));
        assertEquals("9999-12-31T23:59:59.999Z", Dates.format(new Timestamp(253402300799999L), null, UTC));
        assertEquals("-0001-12-31T23:59:59.999Z", Dates.format(xml));
    }

    // ---------------------------------------------------------------------------------------------
    // C-080: the fractional-offset auto branch reports "the auto-detected format" like every other
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c080_fractionalOffsetAutoBranchReportsTheAutoDetectedFormatLikeEveryOther() {
        final String text = "2025-13-15T10:30:45.5+05:30";

        for (final Executable call : new Executable[] { () -> Dates.parseToTimestamp(text), () -> Dates.parseToJUDate(text), () -> Dates.parseToInstant(text),
                () -> Dates.parseToCalendar(text), () -> Dates.parseToXMLGregorianCalendar(text) }) {
            final String message = messageOf(call);
            assertEquals(1, count(message, "the auto-detected format"), message);
            assertFalse(message.contains("(ISO_OFFSET_DATE_TIME"), message);
            assertFalse(message.contains("in time zone"), message);
            assertTrue(message.startsWith("Cannot parse \"" + text + "\" with the auto-detected format: "), message);
            assertTrue(message.contains("Invalid value for MonthOfYear"), message);
        }

        assertEquals(1736917245500L, Dates.parseToTimestamp("2025-01-15T10:30:45.5+05:30").getTime());
        assertEquals(500_000_000, Dates.parseToTimestamp("2025-01-15T10:30:45.5+05:30").getNanos());
        assertEquals(1736917245500L, Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.5+05:30").toGregorianCalendar().getTimeInMillis());
    }

    // ---------------------------------------------------------------------------------------------
    // C-081: the auto-detected JDBC branch still owns every fraction length (dead length guard removed)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c081_autoDetectedJdbcBranchStillOwnsEveryFractionLength() {
        assertEquals(1736937045500L, Dates.parseToJUDate("2025-01-15 10:30:45.5", null, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15 10:30:45.123456789", null, UTC).getTime());
        assertEquals(123456789, Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, UTC).getNanos());
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15 10:30:45", null, UTC).getTime());
        assertTrue(messageOf(() -> Dates.parseToJUDate("2025-01-15 10:30:45.1234567890", null, UTC)).contains("the auto-detected format"));
        assertTrue(messageOf(() -> Dates.parseToJUDate("2025-01-15 10:30:45.5", Dates.LOCAL_TIMESTAMP_FORMAT, UTC)).contains("exactly three"));
    }

    // ---------------------------------------------------------------------------------------------
    // C-083: the Calendar range checks reject a non-lenient calendar holding invalid fields
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c083_calendarRangeChecksRejectANonLenientCalendarWithInvalidFields() {
        final Calendar c1 = Dates.createCalendar(1000L), c5 = Dates.createCalendar(5000L), c10 = Dates.createCalendar(10000L), c15 = Dates.createCalendar(15000L);
        final GregorianCalendar bad = new GregorianCalendar(UTC);
        bad.setLenient(false);
        bad.setTimeInMillis(1000L);
        bad.set(Calendar.MONTH, 13);

        for (final Executable call : new Executable[] { () -> Dates.isOverlapping(bad, c10, c5, c15), () -> Dates.isOverlapping(c1, bad, c5, c15),
                () -> Dates.isOverlapping(c1, c10, bad, c15), () -> Dates.isOverlapping(c1, c10, c5, bad), () -> Dates.isBetween(bad, c1, c10),
                () -> Dates.isBetween(c5, bad, c10), () -> Dates.isBetween(c5, c1, bad) }) {
            final String message = messageOf(call);
            assertTrue(message.contains("MONTH"), message);
            assertFalse(message.contains("Start date must not be after end date"), message);
        }

        // Valid calendars: the documented examples are unchanged.
        assertTrue(Dates.isOverlapping(c1, c10, c5, c15));
        assertFalse(Dates.isOverlapping(c1, c10, c10, c15));
        assertFalse(Dates.isOverlapping(c5, c5, c1, c10));
        assertEquals("Start date must not be after end date", messageOf(() -> Dates.isOverlapping(c10, c1, c5, c15)));
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(null, c10, c5, c15));
        assertTrue(Dates.isBetween(c5, c1, c10));
        assertTrue(Dates.isBetween(c1, c1, c10));
        assertTrue(Dates.isBetween(c10, c1, c10));
        assertFalse(Dates.isBetween(c15, c1, c10));
        assertEquals("Start date must not be after end date", messageOf(() -> Dates.isBetween(c5, c10, c1)));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(null, c1, c10));

        // Instants are compared, whatever the calendars' zones.
        assertTrue(Dates.isBetween(Dates.createCalendar(5000L, NEW_YORK), Dates.createCalendar(1000L, KOLKATA), c10));
        assertTrue(Dates.isOverlapping(Dates.createCalendar(1000L, NEW_YORK), c10, Dates.createCalendar(5000L, KOLKATA), c15));

        // A lenient calendar with the same write is normalised, as before.
        final GregorianCalendar lenient = new GregorianCalendar(UTC);
        lenient.setTimeInMillis(1000L);
        lenient.set(Calendar.MONTH, 13);
        assertFalse(Dates.isBetween(lenient, c1, c10));
        assertTrue(Dates.isOverlapping(c1, lenient, c5, c15));
    }

    // ---------------------------------------------------------------------------------------------
    // C-119: a same-ID rules mismatch names the two classes instead of repeating the ID
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c119_sameIdDifferentRulesMessageNamesTheClasses() {
        final SimpleTimeZone customized = new SimpleTimeZone(-5 * 3600000, "America/New_York", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0,
                3600000);
        final Calendar custom = Calendar.getInstance(customized);
        custom.setTimeInMillis(1736937045000L);
        final Calendar registered = Calendar.getInstance(NEW_YORK);
        registered.setTimeInMillis(1736937045000L);

        final String sameId = messageOf(() -> Dates.isSameDay(custom, registered));
        assertEquals("Calendars must share equivalent time-zone rules to compare civil fields; both named 'America/New_York' (java.util.SimpleTimeZone vs "
                + NEW_YORK.getClass().getName() + ") but with different rules. Pass an explicit ZoneId or TimeZone to compare both instants in one zone.",
                sameId);
        assertEquals(sameId, messageOf(() -> Dates.isSameMonth(custom, registered)));
        assertEquals(sameId, messageOf(() -> Dates.isSameYear(custom, registered)));

        final Calendar kolkata = Calendar.getInstance(KOLKATA);
        kolkata.setTimeInMillis(1736937045000L);
        assertEquals("Calendars must share equivalent time-zone rules to compare civil fields; got 'America/New_York' and 'Asia/Kolkata'."
                + " Pass an explicit ZoneId or TimeZone to compare both instants in one zone.", messageOf(() -> Dates.isSameMonth(registered, kolkata)));

        // Same ID, same rules, different classes: still equivalent; so are two registered zones.
        final Calendar simpleUtc = Calendar.getInstance(new SimpleTimeZone(0, "UTC"));
        simpleUtc.setTimeInMillis(1736937045000L);
        final Calendar utc = Calendar.getInstance(UTC);
        utc.setTimeInMillis(1736937045000L);
        assertTrue(Dates.isSameDay(simpleUtc, utc));
        assertTrue(Dates.isSameYear(registered, Dates.createCalendar(1736937045000L, NEW_YORK)));
    }

    // =============================================================================================
    // Cycle 3
    // =============================================================================================

    private static String overflowOf(final Executable call) {
        final ArithmeticException e = assertThrows(ArithmeticException.class, call);
        assertNotNull(e.getMessage());
        return e.getMessage();
    }

    // ---------------------------------------------------------------------------------------------
    // C-128: createXMLGregorianCalendar(long, TimeZone) still snapshots a supplied mutable zone and
    // reads the live default when none is supplied
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c128_xmlFactorySnapshotsASuppliedZoneAndReadsTheDefaultWhenNoneIsSupplied() {
        final SimpleTimeZone mutable = new SimpleTimeZone(3_600_000, "Mutable/Zone");
        final XMLGregorianCalendar snapshot = Dates.createXMLGregorianCalendar(1736937045000L, mutable);
        mutable.setRawOffset(7_200_000);
        assertEquals(60, snapshot.getTimezone());
        assertEquals(11, snapshot.getHour());
        assertEquals(1736937045000L, snapshot.toGregorianCalendar().getTimeInMillis());

        final XMLGregorianCalendar fromDefault = Dates.createXMLGregorianCalendar(1736937045000L, null);
        assertEquals(TimeZone.getDefault().getOffset(1736937045000L) / 60_000, fromDefault.getTimezone());
        assertEquals(1736937045000L, fromDefault.toGregorianCalendar().getTimeInMillis());
        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(1736937045000L, new SimpleTimeZone(500, "Sub/Second")));
    }

    // ---------------------------------------------------------------------------------------------
    // C-129: the create*(Calendar) factories pass on a non-lenient calendar's own rejection
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c129_createFromCalendarPassesOnANonLenientCalendarsOwnRejection() {
        final GregorianCalendar invalid = new GregorianCalendar(UTC);
        invalid.setLenient(false);
        invalid.setTimeInMillis(1736937045000L);
        invalid.set(Calendar.MONTH, 13);

        for (final Executable call : new Executable[] { () -> Dates.createJUDate(invalid), () -> Dates.createDate(invalid), () -> Dates.createTime(invalid),
                () -> Dates.createTimestamp(invalid), () -> Dates.createCalendar(invalid), () -> Dates.createGregorianCalendar(invalid),
                () -> Dates.createXMLGregorianCalendar(invalid) }) {
            assertThrows(IllegalArgumentException.class, call);
        }

        // A valid non-lenient calendar and a lenient one with the same write both go through.
        final GregorianCalendar strict = new GregorianCalendar(UTC);
        strict.setLenient(false);
        strict.setTimeInMillis(1736937045000L);
        assertEquals(1736937045000L, Dates.createJUDate(strict).getTime());
        assertEquals(1736937045000L, Dates.createTimestamp(strict).getTime());
        assertEquals(1736937045000L, Dates.createCalendar(strict).getTimeInMillis());
        assertEquals(1736937045000L, Dates.createXMLGregorianCalendar(strict).toGregorianCalendar().getTimeInMillis());

        final GregorianCalendar lenient = new GregorianCalendar(UTC);
        lenient.setTimeInMillis(1736937045000L);
        lenient.set(Calendar.MONTH, 13);
        assertEquals(Instant.parse("2026-02-15T10:30:45Z").toEpochMilli(), Dates.createJUDate(lenient).getTime());
    }

    // ---------------------------------------------------------------------------------------------
    // C-133: parseISO8601 names the fallback zone only for zone-less text
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c133_isoOffsetAndDesignatorTextNamesNoFallbackZoneWhileZoneLessTextStillDoes() {
        assertEquals("Cannot parse \"2025-02-30T10:30:45.123+05:30\" with format 'yyyy-MM-dd'T'HH:mm:ss.SSSXXX': Invalid calendar date",
                messageOf(() -> Dates.parseToTimestamp("2025-02-30T10:30:45.123+05:30", Dates.ISO_OFFSET_TIMESTAMP_FORMAT, KOLKATA)));
        assertEquals("Cannot parse \"2025-02-30T10:30:45+05:30\" with format 'yyyy-MM-dd'T'HH:mm:ssXXX': Invalid calendar date",
                messageOf(() -> Dates.parseToJUDate("2025-02-30T10:30:45+05:30", Dates.ISO_OFFSET_DATE_TIME_FORMAT, KOLKATA)));
        assertEquals("Cannot parse \"2025-02-30T10:30:45+05:30\" with the auto-detected format: Invalid calendar date",
                messageOf(() -> Dates.parseToJUDate("2025-02-30T10:30:45+05:30", null, KOLKATA)));
        assertEquals("Cannot parse \"2025-02-30T10:30:45Z\" with the auto-detected format: Invalid calendar date",
                messageOf(() -> Dates.parseToJUDate("2025-02-30T10:30:45Z", null, KOLKATA)));
        assertEquals("Cannot parse \"2025-02-30T10:30:45.123Z\" with the auto-detected format: Invalid calendar date",
                messageOf(() -> Dates.parseToJUDate("2025-02-30T10:30:45.123Z", null, KOLKATA)));
        // Undetected ISO shapes take the same route: an offset in the text drops the zone, its absence keeps it.
        assertEquals("Cannot parse \"2025-13-15T10:30+05:30\" with the auto-detected format: Invalid calendar date",
                messageOf(() -> Dates.parseToJUDate("2025-13-15T10:30+05:30", null, NEW_YORK)));
        assertEquals("Cannot parse \"2025-13-15T10:30\" with the auto-detected format in time zone America/New_York: Invalid calendar date",
                messageOf(() -> Dates.parseToJUDate("2025-13-15T10:30", null, NEW_YORK)));
        assertTrue(messageOf(() -> Dates.parseToJUDate("2025-03-09T02:30", null, NEW_YORK))
                .startsWith("Cannot parse \"2025-03-09T02:30\" with the auto-detected format in time zone America/New_York: Nonexistent local date-time"));

        for (final Executable call : new Executable[] { () -> Dates.parseToCalendar("2025-02-30T10:30:45Z", null, KOLKATA),
                () -> Dates.parseToTimestamp("2025-02-30T10:30:45+05:30", null, KOLKATA),
                () -> Dates.parseToXMLGregorianCalendar("2025-02-30T10:30:45.123+05:30", Dates.ISO_OFFSET_TIMESTAMP_FORMAT, KOLKATA) }) {
            final String message = messageOf(call);
            assertFalse(message.contains("in time zone"), message);
            assertEquals(1, count(message, "Cannot parse"), message);
        }

        // Success is unchanged on every shape.
        assertEquals(1736917245000L, Dates.parseToJUDate("2025-01-15T10:30:45+05:30", null, NEW_YORK).getTime());
        assertEquals(1736917245123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123+05:30", Dates.ISO_OFFSET_TIMESTAMP_FORMAT, KOLKATA).getTime());
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15T10:30:45Z", null, KOLKATA).getTime());
        assertEquals(1736937000000L, Dates.parseToJUDate("2025-01-15T10:30Z", null, NEW_YORK).getTime());
        assertEquals(1736955000000L, Dates.parseToJUDate("2025-01-15T10:30", null, NEW_YORK).getTime());
    }

    // ---------------------------------------------------------------------------------------------
    // C-135: one ArithmeticException shape across the add*/roll family
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c135_addAndRollOverflowsShareOneMessageShape() {
        final java.util.Date max = new java.util.Date(Long.MAX_VALUE);
        final java.util.Date min = new java.util.Date(Long.MIN_VALUE);
        final java.util.Date epoch = new java.util.Date(0L);
        final Calendar calMax = Dates.createCalendar(Long.MAX_VALUE, UTC);
        final Calendar calEpoch = Dates.createCalendar(0L, UTC);
        final String prefix = "Date-time arithmetic overflow: Dates.";
        final String atMax = ") on epoch millis 9223372036854775807 wrapped the supported range (a step of ";

        // Elapsed-time paths, int and long overloads, Date and Calendar receivers.
        assertEquals(prefix + "addHours(date, 1" + atMax + "1 HOUR_OF_DAY)", overflowOf(() -> Dates.addHours(max, 1)));
        assertEquals(prefix + "addSeconds(date, 1" + atMax + "1 SECOND)", overflowOf(() -> Dates.addSeconds(max, 1)));
        assertEquals(prefix + "addHours(date, 1" + atMax + "1 HOURS)", overflowOf(() -> Dates.addHours(calMax, 1)));
        assertEquals(prefix + "addMilliseconds(date, 1" + atMax + "1 MILLISECONDS)", overflowOf(() -> Dates.addMilliseconds(calMax, 1)));
        assertEquals(prefix + "addHours(date, 9223372036854775807) on epoch millis 0 wrapped the supported range (a step of 9223372036854775807 HOURS)",
                overflowOf(() -> Dates.addHours(epoch, Long.MAX_VALUE)));
        assertEquals(prefix + "addMinutes(date, -1) on epoch millis -9223372036854775808 wrapped the supported range (a step of -1 MINUTES)",
                overflowOf(() -> Dates.addMinutes(min, -1L)));

        // roll: the add* replacement when one exists, roll itself for the units only roll accepts.
        assertEquals(prefix + "addHours(date, 9223372036854775807) on epoch millis 0 wrapped the supported range (a step of 9223372036854775807 HOURS)",
                overflowOf(() -> Dates.roll(epoch, Long.MAX_VALUE, java.util.concurrent.TimeUnit.HOURS)));
        assertEquals(prefix + "roll(date, 9223372036854775807) on epoch millis 0 wrapped the supported range (a step of 9223372036854775807 DAYS)",
                overflowOf(() -> Dates.roll(calEpoch, Long.MAX_VALUE, java.util.concurrent.TimeUnit.DAYS)));
        assertEquals(prefix + "roll(date, 9223372036854775807" + atMax + "9223372036854775807 NANOSECONDS)",
                overflowOf(() -> Dates.roll(max, Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS)));
        assertEquals(prefix + "addHours(date, 1" + atMax + "1 HOUR_OF_DAY)", overflowOf(() -> Dates.roll(max, 1, CalendarField.HOUR_OF_DAY)));
        assertEquals(prefix + "addWeeks(date, 1" + atMax + "1 WEEK_OF_YEAR)", overflowOf(() -> Dates.roll(calMax, 1, CalendarField.WEEK_OF_YEAR)));

        // Civil-field walks: the same shape, still naming the caller's instant and the unit the pins rely on.
        assertEquals(prefix + "addYears(date, 2147483647) on epoch millis 0 wrapped the supported range (a step of 2147483647 YEAR)",
                overflowOf(() -> Dates.addYears(epoch, Integer.MAX_VALUE)));
        assertEquals(prefix + "addYears(date, 2147483647) on epoch millis 0 wrapped the supported range (a step of 2147483647 YEAR)",
                overflowOf(() -> Dates.addYears(calEpoch, Integer.MAX_VALUE)));
        assertEquals(prefix + "addWeeks(date, 1" + atMax + "1 WEEK_OF_YEAR)", overflowOf(() -> Dates.addWeeks(max, 1)));
        assertEquals(prefix + "addMonths(date, 1" + atMax + "1 MONTH)", overflowOf(() -> Dates.addMonths(calMax, 1)));
        assertEquals(prefix + "addDays(date, -1) on epoch millis -9223372036854775808 wrapped the supported range (a step of -1 DAY_OF_MONTH)",
                overflowOf(() -> Dates.addDays(min, -1)));

        // In-range arithmetic at the edges is untouched.
        assertEquals(Long.MAX_VALUE, Dates.addHours(max, 0).getTime());
        assertEquals(9223372036854L, Dates.roll(epoch, Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS).getTime());
        assertEquals(Long.MAX_VALUE, Dates.addMilliseconds(calEpoch, Long.MAX_VALUE).getTimeInMillis());
        assertEquals(Long.MIN_VALUE, Dates.addMilliseconds(min, 0L).getTime());
    }

    // ---------------------------------------------------------------------------------------------
    // C-139: the java.time targets report a malformed auto-detected ISO offset like the legacy ones
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c139_javaTimeTargetsReportAMalformedAutoDetectedOffsetInTheLegacyFamily() {
        final String shape = "Format 'yyyy-MM-dd'T'HH:mm:ssXXX' requires yyyy-MM-dd'T'HH:mm:ss followed by Z or a [+-]HH:mm (or [+-]HHmm) offset: "
                + "\"2025-01-15T10:30:45+5:30\"";
        assertEquals(shape, messageOf(() -> Dates.parseToJUDate("2025-01-15T10:30:45+5:30")));

        for (final Executable call : new Executable[] { () -> Dates.parseToInstant("2025-01-15T10:30:45+5:30"),
                () -> Dates.parseToOffsetDateTime("2025-01-15T10:30:45+5:30"), () -> Dates.parseToZonedDateTime("2025-01-15T10:30:45+5:30"),
                () -> Dates.parseToLocalDateTime("2025-01-15T10:30:45+5:30"), () -> Dates.parseToLocalDate("2025-01-15T10:30:45+5:30"),
                () -> Dates.parseToLocalTime("2025-01-15T10:30:45+5:30"), () -> Dates.parseToInstant("2025-01-15T10:30:45+5:30", Dates.ISO_OFFSET_DATE_TIME_FORMAT) }) {
            assertEquals(shape, messageOf(call));
        }

        final String range = "UTC offset must be in the range -18:00 through +18:00: \"2025-01-15T10:30:45+19:00\"";
        assertEquals(range, messageOf(() -> Dates.parseToJUDate("2025-01-15T10:30:45+19:00")));
        assertEquals(range, messageOf(() -> Dates.parseToInstant("2025-01-15T10:30:45+19:00")));
        assertEquals(range, messageOf(() -> Dates.parseToOffsetDateTime("2025-01-15T10:30:45+19:00")));

        // Well-formed offsets, the compact form and the extended shapes still parse as before.
        assertEquals(Instant.parse("2025-01-15T05:00:45Z"), Dates.parseToInstant("2025-01-15T10:30:45+0530"));
        assertEquals(Instant.parse("2025-01-15T05:00:45Z"), Dates.parseToInstant("2025-01-15T10:30:45+05:30"));
        assertEquals(Instant.parse("2025-01-15T05:00:45.123456Z"), Dates.parseToInstant("2025-01-15T10:30:45.123456+05:30"));
        assertEquals(Instant.parse("2025-01-15T05:00:30Z"), Dates.parseToInstant("2025-01-15T10:30:45+05:30:15"));
        assertEquals(ZonedDateTime.parse("2025-01-15T10:30:45+05:30[Asia/Kolkata]"), Dates.parseToZonedDateTime("2025-01-15T10:30:45+05:30[Asia/Kolkata]"));
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30, 45), Dates.parseToLocalDateTime("2025-01-15T10:30:45+05:30"));
        assertEquals(OffsetDateTime.of(2025, 1, 15, 10, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)), Dates.parseToOffsetDateTime("2025-01-15T10:30:45+05:30"));
        assertTrue(messageOf(() -> Dates.parseToInstant("2025-01-15T10:30:45.123+5:30")).startsWith("Cannot detect a date/time format"));
    }

    // ---------------------------------------------------------------------------------------------
    // C-144 / C-145: DTF zone resolution is unchanged without the dead offset fallbacks, and the
    // fixed formatters' accessors carry their override zone (UTC, and GMT for HTTP_DATE)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void c144_c145_dtfZoneResolutionAndOverrideZonesAreUnchanged() {
        assertEquals("2025-01-15T10:30:45Z[UTC]", DTF.ISO_8601_DATE_TIME.parseToZonedDateTime("2025-01-15T10:30:45Z").toString());
        assertEquals("2025-01-15T10:30:45.123Z[UTC]", DTF.ISO_8601_TIMESTAMP.parseToZonedDateTime("2025-01-15T10:30:45.123Z").toString());
        assertEquals("2025-01-15T10:30:45+05:30", DTF.of("uuuu-MM-dd'T'HH:mm:ssXXX").parseToZonedDateTime("2025-01-15T10:30:45+05:30").toString());
        assertEquals("2025-01-15T10:30:45+05:30[Asia/Kolkata]",
                DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2025-01-15T10:30:45+05:30[Asia/Kolkata]").toString());
        assertEquals("2025-01-15T10:30:45+05:30[Asia/Kolkata]", DTF.LOCAL_DATE_TIME.parseToZonedDateTime("2025-01-15 10:30:45", KOLKATA).toString());
        assertEquals("2025-01-15T10:30:45Z[GMT]", DTF.HTTP_DATE.parseToZonedDateTime("Wed, 15 Jan 2025 10:30:45 GMT").toString());

        // parseToTime anchors on the written offset or zone, else the supplied one.
        assertEquals(18045000L, DTF.of("HH:mm:ssXXX").parseToTime("10:30:45+05:30", NEW_YORK).getTime());
        assertEquals(18045000L, DTF.of("HH:mm:ss VV").parseToTime("10:30:45 Asia/Kolkata", NEW_YORK).getTime());
        assertEquals(18045000L, DTF.of("HH:mm:ssXXX'['VV']'").parseToTime("10:30:45+05:30[Asia/Kolkata]", NEW_YORK).getTime());
        assertEquals(37845000L, DTF.of("HH:mm:ssX").parseToTime("10:30:45Z", NEW_YORK).getTime());
        assertEquals(55845000L, DTF.LOCAL_TIME.parseToTime("10:30:45", NEW_YORK).getTime());

        // C-145: the documented override zones on the raw accessor.
        assertEquals("UTC", DTF.ISO_8601_DATE_TIME.parseToTemporalAccessor("2025-01-15T10:30:45Z").query(java.time.temporal.TemporalQueries.zone()).getId());
        assertEquals("GMT", DTF.HTTP_DATE.parseToTemporalAccessor("Wed, 15 Jan 2025 10:30:45 GMT").query(java.time.temporal.TemporalQueries.zone()).getId());
        assertNull(DTF.LOCAL_DATE_TIME.parseToTemporalAccessor("2025-01-15 10:30:45").query(java.time.temporal.TemporalQueries.zone()));
    }
}
