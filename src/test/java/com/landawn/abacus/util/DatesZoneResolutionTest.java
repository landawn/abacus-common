package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.util.Dates.DTF;

/** Date resolution and diagnostic regression contracts. */
public class DatesZoneResolutionTest extends DatesResolutionTestSupport {

    // C-125 (cycle 3): an inadmissible millisecond amount at the range edge is still an IllegalArgumentException
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

    // C-081: the auto-detected JDBC branch still owns every fraction length (dead length guard removed)
    @Test
    public void c081_autoDetectedJdbcBranchStillOwnsEveryFractionLength() {
        assertEquals(1736937045500L, Dates.parseToJUDate("2025-01-15 10:30:45.5", null, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15 10:30:45.123456789", null, UTC).getTime());
        assertEquals(123456789, Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, UTC).getNanos());
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15 10:30:45", null, UTC).getTime());
        assertTrue(messageOf(() -> Dates.parseToJUDate("2025-01-15 10:30:45.1234567890", null, UTC)).contains("the auto-detected format"));
        assertTrue(messageOf(() -> Dates.parseToJUDate("2025-01-15 10:30:45.5", Dates.LOCAL_TIMESTAMP_FORMAT, UTC)).contains("exactly three"));
    }

    // C-124 (cycle 3): a carried 31st into the first month of the range keeps the day
    @Test
    public void c124_carriedDayThirtyOneIntoTheFirstMonthOfTheRangeKeepsTheDay() {
        TimeZone.setDefault(UTC);
        final long may31 = java.time.LocalDateTime.of(-292275055, 5, 31, 12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        final java.util.Date july31 = new java.util.Date(
                java.time.LocalDateTime.of(-292275055, 7, 31, 12, 0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli());

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

    // C-092 (cycle 2): the documented whitespace rule of custom legacy patterns
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

    // C-126 (cycle 3): the DTF legacy-type parsers reject an instant beyond the epoch-millisecond range uniformly
    @Test
    public void c126_dtfLegacyTargetsRejectAnInstantBeyondTheEpochMillisecondRangeUniformly() {
        for (final String text : new String[] { "+300000000-01-01", "-292275056-01-01", "+292278995-01-01" }) {
            final java.util.List<org.junit.jupiter.api.function.Executable> calls = java.util.List.of(() -> DTF.LOCAL_DATE.parseToDate(text, UTC),
                    () -> DTF.LOCAL_DATE.parseToTime(text, UTC), () -> DTF.LOCAL_DATE.parseToCalendar(text, UTC), () -> DTF.LOCAL_DATE.parseToJUDate(text),
                    () -> DTF.LOCAL_DATE.parseToJUDate(text, UTC), () -> DTF.LOCAL_DATE.parseToTimestamp(text),
                    () -> DTF.LOCAL_DATE.parseToTimestamp(text, UTC));

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
        assertEquals(java.time.LocalDate.of(300_000_000, 1, 1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                DTF.LOCAL_DATE.parseToInstant("+300000000-01-01", UTC));

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

    // C-027: the documented example values of the DTF (CharSequence, TimeZone) trio
    @Test
    public void c027_dtfTimeZoneOverloadExamplesHold() {
        assertEquals(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.UTC), DTF.LOCAL_DATE_TIME.parseToOffsetDateTime("2023-12-25 14:30:45", UTC));
        assertEquals(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)),
                DTF.LOCAL_DATE_TIME.parseToOffsetDateTime("2023-12-25 14:30:45", KOLKATA));
        assertEquals(OffsetDateTime.of(2023, 12, 25, 14, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)),
                DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2023-12-25T14:30:45+05:30", UTC));
        assertEquals(ZonedDateTime.of(2023, 12, 25, 14, 30, 45, 0, KOLKATA.toZoneId()),
                DTF.LOCAL_DATE_TIME.parseToZonedDateTime("2023-12-25 14:30:45", KOLKATA));
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

    @Test
    public void c014_explicitFormatIsStillNamedOnceWithoutTheDtfPattern() {
        final String named = messageOf(() -> Dates.parseToJUDate("2025-01-15T10:30:45+01:00[Asia/Kolkata]", Dates.ISO_ZONED_DATE_TIME_FORMAT, NEW_YORK));

        assertTrue(named.contains("with format '" + Dates.ISO_ZONED_DATE_TIME_FORMAT + "'"), named);
        assertFalse(named.contains("with pattern"), named);
        assertEquals(1, count(named, "Cannot parse"), named);
        assertTrue(named.contains("Offset +01:00 is not valid"), named);
    }

    // C-053: DTF.format(Date)/format(Calendar) reject a fixed offset no ZoneOffset can express
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

    // C-043: the utility-class constructor is private
    @Test
    public void c043_constructorIsPrivate() throws Exception {
        assertTrue(Modifier.isPrivate(Dates.class.getDeclaredConstructor().getModifiers()));
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

    // C-025: the bracketed-zone form may omit the seconds field on every target
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

    @Test
    public void c004_preNineteenHundredArithmeticIsSymmetric() {
        TimeZone.setDefault(NY);
        final java.util.Date source = at("1905-06-15 12:00:00", NY);

        assertEquals(source, Dates.addYears(Dates.addYears(source, -25), 25));
        assertEquals("1880-06-15 12:00:00", local(Dates.addYears(source, -25), NY));
        assertEquals(source, Dates.addMonths(Dates.addMonths(source, -300), 300));
    }

    // C-004

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

    // C-007
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

    // C-121 (cycle 3): a Timestamp copy, comparison or rendering at the epoch floor does not wrap
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

    @Test
    public void c003_unambiguousTextWithAZoneNameIsUnchanged() {
        final DTF dtf = DTF.of("uuuu-MM-dd HH:mm:ss z");

        assertEquals(ZonedDateTime.of(2023, 7, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), dtf.parseToZonedDateTime("2023-07-01 12:00:00 EDT"));
        assertEquals(ZonedDateTime.of(2023, 1, 1, 12, 0, 0, 0, ZoneId.of("America/New_York")), dtf.parseToZonedDateTime("2023-01-01 12:00:00 EST"));
        assertEquals(ZoneOffset.UTC, dtf.parseToZonedDateTime("2023-07-01 12:00:00 UTC").getOffset());
        assertThrows(IllegalArgumentException.class, () -> dtf.parseToZonedDateTime(""));
        assertEquals(null, dtf.parseToZonedDateTime((CharSequence) null));
    }

    @Test
    public void c118_unverifiedDefaultUsesKnownIdWhilePlainFixedZoneUsesItsOffset() {
        final java.util.Date a = new java.util.Date(Instant.parse("2025-01-15T23:30:00Z").toEpochMilli());
        final java.util.Date b = new java.util.Date(Instant.parse("2025-01-16T00:30:00Z").toEpochMilli());

        TimeZone.setDefault(fixedOffsetSubclass(0, "America/New_York"));
        assertTrue(Dates.isSameDay(a, b));
        assertTrue(Dates.isLastDayOfMonth(new java.util.Date(Instant.parse("2025-02-01T00:30:00Z").toEpochMilli())));

        TimeZone.setDefault(fixedOffsetSubclass(-5 * 3_600_000, "America/New_York"));
        assertTrue(Dates.isSameDay(a, b));

        TimeZone.setDefault(fixedOffsetSubclass(500, "America/New_York"));
        assertTrue(Dates.isSameDay(a, b));

        TimeZone.setDefault(fixedOffsetSubclass(0, "No/Such_Zone"));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(a, b));

        // The old fixed-offset assertions still apply when the implementation proves the fixed rules.
        TimeZone.setDefault(new SimpleTimeZone(0, "America/New_York"));
        assertFalse(Dates.isSameDay(a, b));
        assertFalse(Dates.isLastDayOfMonth(new java.util.Date(Instant.parse("2025-02-01T00:30:00Z").toEpochMilli())));
        TimeZone.setDefault(new SimpleTimeZone(500, "America/New_York"));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(a, b));
        TimeZone.setDefault(new SimpleTimeZone(0, "No/Such_Zone"));
        assertFalse(Dates.isSameDay(a, b));

        // A real region default and a customised known ID are unchanged.
        TimeZone.setDefault(NY);
        assertTrue(Dates.isSameDay(a, b));
        TimeZone.setDefault(new SimpleTimeZone(-5 * 3_600_000, "America/New_York", Calendar.APRIL, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0));
        assertTrue(Dates.isSameDay(a, b));
    }

    // C-144 / C-145: DTF zone resolution is unchanged without the dead offset fallbacks, and the
    // fixed formatters' accessors carry their override zone (UTC, and GMT for HTTP_DATE)
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

    // C-030: the zone-ID constants are region IDs, not ZoneOffset.UTC
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

    // 2023-11-05T01:30-04:00[America/New_York]
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

}
