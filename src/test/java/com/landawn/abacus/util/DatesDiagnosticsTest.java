package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.util.Dates.DTF;

/** Date resolution and diagnostic regression contracts. */
public class DatesDiagnosticsTest extends DatesResolutionTestSupport {



    // C-014: every auto-detected failure says "the auto-detected format" exactly once
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



    // C-074 / C-075 (cycle 2): every auto-detected failure names "the auto-detected format", never a pattern
    @Test
    public void c074_c075_autoDetectedFailuresNeverNameAPatternTheCallerDidNotWrite() {
        TimeZone.setDefault(NY);
        final String[] texts = { "2025-13-15T10:30:45.5Z", "2025-13-15T10:30:45.123Z", "2025-02-30T10:30:45.5Z", "2025-13-15 10:30:45", "2025-13-15",
                "2025-13-15T10:30:45", "2025-01-15T23:59:60Z", "2025-13-15T10:30:45+01:00", "2025-13-15T10:30:45.123" };
        final java.util.List<org.junit.jupiter.api.function.ThrowingConsumer<String>> targets = java.util.List.of(Dates::parseToJUDate, Dates::parseToTimestamp,
                Dates::parseToCalendar, Dates::parseToXMLGregorianCalendar, Dates::parseToInstant, Dates::parseToDate);

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



    // C-047: the Calendar default names no pattern in its year-range error
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



    // C-119 revised 2026-10-04: distinguish unverifiable custom rules from supported but different rules.
    @Test
    public void c119_diagnosticsDistinguishUnverifiableFromDifferentRules() {
        final SimpleTimeZone customized = new SimpleTimeZone(-5 * 3600000, "America/New_York", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0, 3600000);
        final Calendar custom = Calendar.getInstance(customized);
        custom.setTimeInMillis(1736937045000L);
        final Calendar registered = Calendar.getInstance(NEW_YORK);
        registered.setTimeInMillis(1736937045000L);

        final String sameId = messageOf(() -> Dates.isSameDay(custom, registered));
        assertEquals("Time zone 'America/New_York' carries rules that cannot be verified against the registered java.time zone with the same ID", sameId);
        assertEquals(sameId, messageOf(() -> Dates.isSameMonth(custom, registered)));
        assertEquals(sameId, messageOf(() -> Dates.isSameYear(custom, registered)));
        assertEquals(sameId, messageOf(() -> Dates.isSameDay(registered, custom)));
        assertEquals(sameId, messageOf(() -> Dates.isSameDay(custom, custom)));

        final Calendar fixed = Calendar.getInstance(new SimpleTimeZone(0, "same"));
        final Calendar shifted = Calendar.getInstance(new SimpleTimeZone(3600000, "same"));
        assertTrue(messageOf(() -> Dates.isSameDay(fixed, shifted)).contains("but with different rules"));

        final Calendar kolkata = Calendar.getInstance(KOLKATA);
        kolkata.setTimeInMillis(1736937045000L);
        assertEquals(
                "Calendars must share equivalent time-zone rules to compare civil fields; got 'America/New_York' and 'Asia/Kolkata'."
                        + " Pass an explicit ZoneId or TimeZone to compare both instants in one zone.",
                messageOf(() -> Dates.isSameMonth(registered, kolkata)));

        // Same ID, same rules, different classes: still equivalent; so are two registered zones.
        final Calendar simpleUtc = Calendar.getInstance(new SimpleTimeZone(0, "UTC"));
        simpleUtc.setTimeInMillis(1736937045000L);
        final Calendar utc = Calendar.getInstance(UTC);
        utc.setTimeInMillis(1736937045000L);
        assertTrue(Dates.isSameDay(simpleUtc, utc));
        assertTrue(Dates.isSameYear(registered, Dates.createCalendar(1736937045000L, NEW_YORK)));
    }



    // C-052: the DTF failure prints the error index once
    @Test
    public void c052_dtfParseFailurePrintsTheErrorIndexOnce() {
        final String compact = messageOf(() -> DTF.of("uuuu-MM-dd'T'HH:mm:ssXXXXX").parseToOffsetDateTime("2023-12-25T14:30:45+0530"));
        assertEquals(
                "Cannot parse \"2023-12-25T14:30:45+0530\" with pattern 'uuuu-MM-dd'T'HH:mm:ssXXXXX': Text '2023-12-25T14:30:45+0530' could not be parsed at index 19",
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



    // C-078: the DTF-routed branch of parse() names no fallback zone for text that carries its own
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
        assertTrue(
                messageOf(() -> Dates.parseToJUDate("2025-13-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, NEW_YORK)).contains("in time zone America/New_York"));

        // Success is unchanged.
        assertEquals(Instant.parse("2025-01-15T05:00:45Z").toEpochMilli(),
                Dates.parseToJUDate("2025-01-15T10:30:45+05:30[Asia/Kolkata]", Dates.ISO_ZONED_DATE_TIME_FORMAT, NEW_YORK).getTime());
        assertEquals(Instant.parse("2025-01-15T05:00:45.5Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15T10:30:45.5+05:30", null, NEW_YORK).getTime());
    }



    // C-079: the explicit-zone Date/SQL and XMLGregorianCalendar defaults name no pattern in their
    // year-range error, exactly as the Calendar default (C-047) and the UTC default do
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



    // C-123 (cycle 3): a fixed-zone conflict on the java.time entry points names the caller's constant
    @Test
    public void c123_fixedZoneConflictOnTheJavaTimeEntryPointsNamesTheCallersConstant() {
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final String[][] cases = { { "2025-01-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT },
                { "2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT } };

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



    // C-080: the fractional-offset auto branch reports "the auto-detected format" like every other
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



    // C-133: parseISO8601 names the fallback zone only for zone-less text
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



    // C-139: the java.time targets report a malformed auto-detected ISO offset like the legacy ones
    @Test
    public void c139_javaTimeTargetsReportAMalformedAutoDetectedOffsetInTheLegacyFamily() {
        final String shape = "Format 'yyyy-MM-dd'T'HH:mm:ssXXX' requires yyyy-MM-dd'T'HH:mm:ss followed by Z or a [+-]HH:mm (or [+-]HHmm) offset: "
                + "\"2025-01-15T10:30:45+5:30\"";
        assertEquals(shape, messageOf(() -> Dates.parseToJUDate("2025-01-15T10:30:45+5:30")));

        for (final Executable call : new Executable[] { () -> Dates.parseToInstant("2025-01-15T10:30:45+5:30"),
                () -> Dates.parseToOffsetDateTime("2025-01-15T10:30:45+5:30"), () -> Dates.parseToZonedDateTime("2025-01-15T10:30:45+5:30"),
                () -> Dates.parseToLocalDateTime("2025-01-15T10:30:45+5:30"), () -> Dates.parseToLocalDate("2025-01-15T10:30:45+5:30"),
                () -> Dates.parseToLocalTime("2025-01-15T10:30:45+5:30"),
                () -> Dates.parseToInstant("2025-01-15T10:30:45+5:30", Dates.ISO_OFFSET_DATE_TIME_FORMAT) }) {
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



    // C-122 (cycle 3): one diagnostic per malformed auto-detected text on every legacy target
    @Test
    public void c122_malformedAutoDetectedTextGetsOneDiagnosticOnEveryLegacyTarget() {
        TimeZone.setDefault(UTC);
        final java.util.List<org.junit.jupiter.api.function.ThrowingConsumer<String>> targets = java.util.List.of(Dates::parseToJUDate, Dates::parseToTimestamp,
                Dates::parseToCalendar, Dates::parseToXMLGregorianCalendar, Dates::parseToDate, Dates::parseToGregorianCalendar);

        for (final String text : new String[] { "2025-01x15T10:30:45.123", "2025-01x15T10:30:45.5", "2025-01x15T10:30:45.123Z",
                "2025-01x15T10:30:45.123456789Z", "2025-01-15T10:30x45.123", "2025-01-15T10:30x45.123Z" }) {
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



    // C-016: one DST gap/overlap wording whichever resolver reports it
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



    // C-135: one ArithmeticException shape across the add*/roll family
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



    // C-073 (cycle 2): a set* whose target leaves the epoch-millisecond range is an ArithmeticException
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



    // C-045: year 0000 reports the year-range diagnostic on every entry point
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
                () -> Dates.parseToTimestamp("0000-01-15 10:30:45.5"),
                () -> Dates.parseToTimestamp("0000-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC) }) {
            assertFalse(messageOf(call).contains("JDBC timestamp year"));
        }

        // The boundaries and the ordinary case are untouched.
        assertEquals(Dates.parseToJUDate("0001-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime(),
                Dates.parseToTimestamp("0001-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(-62134349355000L, Dates.parseToTimestamp("0001-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045000L, Dates.parseToTimestamp("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045500L, Dates.parseToTimestamp("2025-01-15 10:30:45.5", null, UTC).getTime());
        assertEquals(Instant.parse("9999-12-31T10:30:45Z").toEpochMilli(),
                Dates.parseToTimestamp("9999-12-31 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertNull(Dates.parseToTimestamp((String) null, Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertTrue(messageOf(() -> Dates.parseToTimestamp("", Dates.LOCAL_DATE_TIME_FORMAT, UTC)).contains("empty"));
    }

}
