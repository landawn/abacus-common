package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dates.DTF;

/**
 * Review pass 9 (2026-09-02).
 *
 * <ul>
 *   <li><b>B1</b> &mdash; the static fixed-zone conflict check was written as
 *       {@code supplied.hasSameRules(UTC)}, whose answer depends on the runtime class of the receiver:
 *       {@code SimpleTimeZone.hasSameRules} is {@code false} for any non-{@code SimpleTimeZone} operand,
 *       so {@code new SimpleTimeZone(0, "UTC")} was rejected as not UTC-equivalent on every legacy
 *       {@code format}/{@code parseTo*} path, while the {@link DTF} path, {@code parseToInstant} and
 *       {@code format(Calendar)} accepted the very same zone and a hand-written zero-offset
 *       {@code TimeZone} subclass passed the same check.</li>
 *   <li><b>B2</b> &mdash; {@code isSameDay/Month/Year(Calendar, Calendar)} rejected a calendar in
 *       {@code UTC} and one in {@code new SimpleTimeZone(0, "Any")} as inequivalent for the same reason.</li>
 *   <li><b>D3</b> &mdash; auto-detected text ending in {@code Z} (or an auto-detected HTTP-date) treated a
 *       non-UTC fallback zone as a conflict, while {@code +00:00}, {@code Z[UTC]} and the same {@code Z} text
 *       under the explicit {@code ISO_OFFSET_DATE_TIME_FORMAT} all won over the fallback. A designator the
 *       text carries is data; only an explicitly supplied fixed-zone constant is a caller choice.</li>
 *   <li><b>D6</b> &mdash; the default {@code format(Date)} fast path built a {@code GregorianCalendar} per
 *       call; the fields now come from {@code java.time} and must be identical over the whole range.</li>
 *   <li><b>J12</b> &mdash; {@code parseToXMLGregorianCalendar} wrote {@code .123000000} where the factory wrote
 *       {@code .123}, so the default round trip was XML-equal but never text-equal.</li>
 *   <li><b>O14</b> &mdash; a fallback zone no {@code ZoneId} can express was reported as
 *       {@code Cannot parse "..."}, and a zero raw offset was printed as {@code raw offset Z}.</li>
 * </ul>
 */
public class DatesUtcEquivalentZoneTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");
    private static final long MODERN = 1736937045123L; // 2025-01-15T10:30:45.123Z
    private static final long MODERN_SECONDS = 1736937045000L;
    private static final String Z_TEXT = "2025-01-15T10:30:45Z";
    private static final String Z_FRACTION_TEXT = "2025-01-15T10:30:45.123Z";
    private static final String HTTP_TEXT = "Wed, 15 Jan 2025 10:30:45 GMT";

    private TimeZone originalTimeZone;

    @BeforeEach
    public void rememberDefaults() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(UTC);
    }

    @AfterEach
    public void restoreDefaults() {
        TimeZone.setDefault(originalTimeZone);
    }

    /** A fixed-offset zone that is neither a {@code ZoneInfo} nor a {@code SimpleTimeZone}. */
    private static TimeZone fixedOffsetSubclass(final int offsetMillis, final String id) {
        final TimeZone zone = new TimeZone() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getOffset(final int era, final int year, final int month, final int day, final int dayOfWeek, final int milliseconds) {
                return offsetMillis;
            }

            @Override
            public void setRawOffset(final int offset) {
                // fixed
            }

            @Override
            public int getRawOffset() {
                return offsetMillis;
            }

            @Override
            public boolean useDaylightTime() {
                return false;
            }

            @Override
            public boolean inDaylightTime(final Date date) {
                return false;
            }
        };

        zone.setID(id);
        return zone;
    }

    /** A zone whose daylight-saving rules no {@code ZoneId} can express. */
    private static TimeZone customDstZone() {
        return new SimpleTimeZone(3_600_000, "Custom/Dst", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0, 3_600_000);
    }

    private static TimeZone[] utcSpellings() {
        return new TimeZone[] { UTC, TimeZone.getTimeZone("GMT"), TimeZone.getTimeZone("Etc/UTC"), new SimpleTimeZone(0, "Any"), new SimpleTimeZone(0, "UTC"),
                fixedOffsetSubclass(0, "Also/Any") };
    }

    // ---------------------------------------------------------------------------------------------
    // B1 - UTC-equivalence is a property of the rules, not of the zone's class or ID

    @Test
    public void b1_fixedZoneConstants_acceptEveryUtcSpellingOnTheStaticEntryPoints() {
        final Date date = new Date(MODERN_SECONDS);
        final Timestamp timestamp = new Timestamp(MODERN);

        for (final TimeZone zone : utcSpellings()) {
            final String id = zone.getID() + "/" + zone.getClass().getSimpleName();

            // format side
            assertEquals(Z_TEXT, Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, zone), id);
            assertEquals(Z_FRACTION_TEXT, Dates.format(timestamp, Dates.ISO_8601_TIMESTAMP_FORMAT, zone), id);
            assertEquals(HTTP_TEXT, Dates.format(date, Dates.HTTP_DATE_FORMAT, zone), id);
            assertEquals(Z_TEXT, Dates.format(Dates.createCalendar(MODERN_SECONDS, NEW_YORK), Dates.ISO_8601_DATE_TIME_FORMAT, zone), id);
            assertEquals(Z_TEXT, Dates.format(Dates.createXMLGregorianCalendar(MODERN_SECONDS, NEW_YORK), Dates.ISO_8601_DATE_TIME_FORMAT, zone), id);

            final StringBuilder sb = new StringBuilder();
            Dates.formatTo(date, Dates.ISO_8601_DATE_TIME_FORMAT, zone, sb);
            assertEquals(Z_TEXT, sb.toString(), id);

            // parse side, explicit constants
            assertEquals(MODERN_SECONDS, Dates.parseToJUDate(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone).getTime(), id);
            assertEquals(MODERN_SECONDS, Dates.parseToDate(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone).getTime(), id);
            assertEquals(MODERN_SECONDS, Dates.parseToTime(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone).getTime(), id);
            assertEquals(MODERN, Dates.parseToTimestamp(Z_FRACTION_TEXT, Dates.ISO_8601_TIMESTAMP_FORMAT, zone).getTime(), id);
            assertEquals(MODERN_SECONDS, Dates.parseToCalendar(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone).getTimeInMillis(), id);
            assertEquals(MODERN_SECONDS, Dates.parseToGregorianCalendar(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone).getTimeInMillis(), id);
            assertEquals(MODERN_SECONDS,
                    Dates.parseToXMLGregorianCalendar(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone).toGregorianCalendar().getTimeInMillis(), id);
            assertEquals(MODERN_SECONDS, Dates.parseToJUDate(HTTP_TEXT, Dates.HTTP_DATE_FORMAT, zone).getTime(), id);
            assertEquals(Instant.ofEpochMilli(MODERN_SECONDS), Dates.parseToInstant(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone), id);

            // the DTF side agreed already; the two must keep agreeing
            assertEquals(MODERN_SECONDS, DTF.ISO_8601_DATE_TIME.parseToJUDate(Z_TEXT, zone).getTime(), id);
            assertEquals(MODERN_SECONDS, DTF.HTTP_DATE.parseToJUDate(HTTP_TEXT, zone).getTime(), id);
        }
    }

    @Test
    public void b1_fixedZoneConstants_stillRejectAZoneThatIsNotUtc() {
        final Date date = new Date(MODERN_SECONDS);
        final TimeZone[] notUtc = { NEW_YORK, new SimpleTimeZone(3_600_000, "PlusOne"), fixedOffsetSubclass(-3_600_000, "MinusOne"), customDstZone(),
                TimeZone.getTimeZone("Europe/London") }; // London has a zero raw offset but observes daylight saving

        for (final TimeZone zone : notUtc) {
            final String id = zone.getID();
            assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, zone), id);
            assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.HTTP_DATE_FORMAT, zone), id);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone), id);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(Z_FRACTION_TEXT, Dates.ISO_8601_TIMESTAMP_FORMAT, zone), id);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(HTTP_TEXT, Dates.HTTP_DATE_FORMAT, zone), id);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, zone), id);
            assertThrows(IllegalArgumentException.class, () -> DTF.ISO_8601_DATE_TIME.parseToJUDate(Z_TEXT, zone), id);
        }

        // O14: a zero raw offset reads "+00:00", not the bare designator "Z"
        final IllegalArgumentException london = assertThrows(IllegalArgumentException.class,
                () -> Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT, TimeZone.getTimeZone("Europe/London")));
        assertTrue(london.getMessage().contains("raw offset +00:00"), london.getMessage());

        final IllegalArgumentException newYork = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, NEW_YORK));
        assertTrue(newYork.getMessage().contains("raw offset -05:00"), newYork.getMessage());
    }

    // ---------------------------------------------------------------------------------------------
    // B2 - the Calendar civil-field comparisons compare rules, not classes

    @Test
    public void b2_calendarCivilComparisons_acceptEquivalentFixedZonesOfAnyClass() {
        final Calendar utc = Dates.createCalendar(MODERN, UTC);
        final Calendar utcLater = Dates.createCalendar(MODERN + 7 * 86_400_000L, UTC); // 2025-01-22
        final Calendar simple = Dates.createCalendar(MODERN, new SimpleTimeZone(0, "Any"));
        final Calendar subclass = Dates.createCalendar(MODERN, fixedOffsetSubclass(0, "Also/Any"));
        final Calendar etcUtc = Dates.createCalendar(MODERN, TimeZone.getTimeZone("Etc/UTC"));

        assertTrue(Dates.isSameDay(utc, simple));
        assertTrue(Dates.isSameMonth(utc, simple));
        assertTrue(Dates.isSameYear(utc, simple));
        assertTrue(Dates.isSameDay(simple, utc));
        assertTrue(Dates.isSameDay(simple, subclass));
        assertTrue(Dates.isSameDay(subclass, utc));
        assertTrue(Dates.isSameDay(utc, etcUtc));

        // a different answer is still an answer, not a rejection
        assertFalse(Dates.isSameDay(utcLater, simple));
        assertTrue(Dates.isSameMonth(utcLater, simple));

        // registered aliases share their rules by content
        assertTrue(Dates.isSameDay(Dates.createCalendar(MODERN, TimeZone.getTimeZone("Asia/Kolkata")),
                Dates.createCalendar(MODERN, TimeZone.getTimeZone("Asia/Calcutta"))));

        // genuinely different rules are still rejected
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(utc, Dates.createCalendar(MODERN, NEW_YORK)));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(simple, Dates.createCalendar(MODERN, new SimpleTimeZone(60_000, "PlusMinute"))));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(utc, Dates.createCalendar(MODERN, customDstZone())));

        // two calendars in identical custom-rule zones share their rules but no ZoneId can express them
        assertThrows(IllegalArgumentException.class,
                () -> Dates.isSameDay(Dates.createCalendar(MODERN, customDstZone()), Dates.createCalendar(MODERN, customDstZone())));
    }

    // ---------------------------------------------------------------------------------------------
    // D3 - a designator the text carries is data and wins over the fallback zone

    @Test
    public void d3_autoDetectedZ_winsOverTheFallbackZoneOnEveryTarget() {
        assertEquals(MODERN_SECONDS, Dates.parseToJUDate(Z_TEXT, null, NEW_YORK).getTime());
        assertEquals(MODERN_SECONDS, Dates.parseToDate(Z_TEXT, null, NEW_YORK).getTime());
        assertEquals(MODERN_SECONDS, Dates.parseToTime(Z_TEXT, null, NEW_YORK).getTime());
        assertEquals(MODERN_SECONDS, Dates.parseToTimestamp(Z_TEXT, null, NEW_YORK).getTime());
        assertEquals(MODERN_SECONDS, Dates.parseToJUDate(Z_FRACTION_TEXT, null, NEW_YORK).getTime() - 123);

        final Timestamp withNanos = Dates.parseToTimestamp("2025-01-15T10:30:45.123456789Z", null, NEW_YORK);
        assertEquals(MODERN, withNanos.getTime());
        assertEquals(123_456_789, withNanos.getNanos());

        // the Calendar-like results carry the textual zone, not the fallback
        final Calendar calendar = Dates.parseToCalendar(Z_TEXT, null, NEW_YORK);
        assertEquals(MODERN_SECONDS, calendar.getTimeInMillis());
        assertEquals(0, calendar.getTimeZone().getRawOffset());
        assertEquals(MODERN_SECONDS, Dates.parseToGregorianCalendar(Z_FRACTION_TEXT, null, NEW_YORK).getTimeInMillis() - 123);

        final XMLGregorianCalendar xml = Dates.parseToXMLGregorianCalendar(Z_FRACTION_TEXT, null, NEW_YORK);
        assertEquals(MODERN, xml.toGregorianCalendar().getTimeInMillis());
        assertEquals(0, xml.getTimezone());

        // java.time targets
        assertEquals(Instant.ofEpochMilli(MODERN_SECONDS), Dates.parseToInstant(Z_TEXT, null, NEW_YORK));
        assertEquals(Instant.ofEpochMilli(MODERN), Dates.parseToInstant(Z_FRACTION_TEXT, null, NEW_YORK));
        assertEquals(ZoneOffset.UTC, Dates.parseToOffsetDateTime(Z_TEXT, null, NEW_YORK).getOffset());
        assertEquals(Instant.ofEpochMilli(MODERN_SECONDS), Dates.parseToZonedDateTime(Z_TEXT, null, NEW_YORK).toInstant());

        // the same instant every way the text can spell UTC
        assertEquals(Dates.parseToInstant("2025-01-15T10:30:45+00:00", null, NEW_YORK), Dates.parseToInstant(Z_TEXT, null, NEW_YORK));
        assertEquals(Dates.parseToJUDate("2025-01-15T10:30:45Z[UTC]", null, NEW_YORK), Dates.parseToJUDate(Z_TEXT, null, NEW_YORK));

        // an undetected shape that still ends in Z reaches the general ISO reader with the same rule
        assertEquals(1736937000000L, Dates.parseToJUDate("2025-01-15T10:30Z", null, NEW_YORK).getTime());

        // the fallback is not consulted at all, so it need not even be expressible as a ZoneId
        assertEquals(Instant.ofEpochMilli(MODERN_SECONDS), Dates.parseToInstant(Z_TEXT, null, customDstZone()));
        assertEquals(MODERN_SECONDS, Dates.parseToJUDate(Z_TEXT, null, customDstZone()).getTime());
    }

    @Test
    public void d3_autoDetectedHttpDate_winsOverTheFallbackZone() {
        assertEquals(MODERN_SECONDS, Dates.parseToJUDate(HTTP_TEXT, null, NEW_YORK).getTime());
        assertEquals(MODERN_SECONDS, Dates.parseToTimestamp(HTTP_TEXT, null, NEW_YORK).getTime());

        final Calendar calendar = Dates.parseToCalendar(HTTP_TEXT, null, NEW_YORK);
        assertEquals(MODERN_SECONDS, calendar.getTimeInMillis());
        assertEquals(0, calendar.getTimeZone().getRawOffset());

        assertEquals(Instant.ofEpochMilli(MODERN_SECONDS), Dates.parseToInstant(HTTP_TEXT, null, NEW_YORK));
        assertEquals(Instant.ofEpochMilli(MODERN_SECONDS), Dates.parseToZonedDateTime(HTTP_TEXT, null, NEW_YORK).toInstant());
    }

    @Test
    public void d3_anExplicitlySuppliedFixedZoneConstant_isStillAConflict() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(Z_FRACTION_TEXT, Dates.ISO_8601_TIMESTAMP_FORMAT, NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(HTTP_TEXT, Dates.HTTP_DATE_FORMAT, NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToZonedDateTime(HTTP_TEXT, Dates.HTTP_DATE_FORMAT, NEW_YORK));
        assertThrows(IllegalArgumentException.class, () -> DTF.ISO_8601_TIMESTAMP.parseToInstant(Z_FRACTION_TEXT, NEW_YORK));

        // the format side has no auto-detection: a fixed-zone pattern with another zone is always a mistake
        assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(MODERN_SECONDS), Dates.ISO_8601_DATE_TIME_FORMAT, NEW_YORK));

        // ISO_OFFSET_DATE_TIME_FORMAT never had the conflict: Z is one of the offsets it accepts
        assertEquals(Instant.ofEpochMilli(MODERN_SECONDS), Dates.parseToInstant(Z_TEXT, Dates.ISO_OFFSET_DATE_TIME_FORMAT, NEW_YORK));
    }

    // ---------------------------------------------------------------------------------------------
    // D6 - the default format(Date) fast path reads its fields through java.time

    @Test
    public void d6_defaultFormat_matchesJavaTimeOverTheWholeSupportedRange() {
        final DateTimeFormatter seconds = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);
        final DateTimeFormatter millis = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);
        final long yearOneStart = -62_135_596_800_000L; // 0001-01-01T00:00:00Z
        final long yearNineNineNineNineEnd = 253_402_300_799_999L; // 9999-12-31T23:59:59.999Z
        final long[] fixed = { yearOneStart, yearOneStart + 1, yearNineNineNineNineEnd, yearNineNineNineNineEnd - 999, 0L, -1L, -999L, -1000L, 1L, 999L, 1000L,
                MODERN, MODERN_SECONDS, 1709251199999L /* 2024-02-29T23:59:59.999Z */, 1709251200000L /* 2024-03-01 */,
                -12219292800000L /* 1582-10-15, first Gregorian day */, -12219292800001L /* the instant before it: proleptic, no cutover */,
                -2208988800000L /* 1900-01-01 */, 951782400000L /* 2000-02-29 */ };

        for (final long ms : fixed) {
            final Instant instant = Instant.ofEpochMilli(ms);
            assertEquals(seconds.format(instant), Dates.format(new Date(ms)), Long.toString(ms));
            assertEquals(millis.format(instant), Dates.format(new Timestamp(ms)), Long.toString(ms));
            assertEquals(millis.format(instant), Dates.format(new java.sql.Date(ms)), Long.toString(ms));
        }

        final Random random = new Random(20260902L);

        for (int i = 0; i < 20_000; i++) {
            final long ms = yearOneStart + (long) (random.nextDouble() * (yearNineNineNineNineEnd - yearOneStart));
            final Instant instant = Instant.ofEpochMilli(ms);
            assertEquals(seconds.format(instant), Dates.format(new Date(ms)), Long.toString(ms));
            assertEquals(millis.format(instant), Dates.format(new Timestamp(ms)), Long.toString(ms));
        }

        // the Appendable paths write the same text
        final StringBuilder sb = new StringBuilder();
        Dates.formatTo(new Timestamp(MODERN), sb);
        assertEquals(Z_FRACTION_TEXT, sb.toString());

        final StringWriter writer = new StringWriter();
        Dates.formatTo(new Date(MODERN_SECONDS), writer);
        assertEquals(Z_TEXT, writer.toString());

        final StringWriter fractionWriter = new StringWriter();
        Dates.formatTo(new java.sql.Time(MODERN), fractionWriter);
        assertEquals(Z_FRACTION_TEXT, fractionWriter.toString());
    }

    @Test
    public void d6_defaultFormat_rejectsYearsTheFourDigitGrammarCannotWrite() {
        final long yearOneStart = -62_135_596_800_000L;
        final long yearTenThousandStart = 253_402_300_800_000L;

        for (final long ms : new long[] { yearOneStart - 1 /* 0000-12-31T23:59:59.999 */, yearTenThousandStart, Long.MIN_VALUE, Long.MAX_VALUE,
                -62_167_219_200_000L /* 0000-01-01 */ }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(ms)), Long.toString(ms));
            assertTrue(e.getMessage().contains("0001 through 9999"), e.getMessage());
            assertThrows(IllegalArgumentException.class, () -> Dates.format(new Timestamp(ms)), Long.toString(ms));
            assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(new Date(ms), new StringBuilder()), Long.toString(ms));
        }

        // formatCurrentDateTime/Timestamp share the path and the shape
        assertTrue(Dates.formatCurrentDateTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
        assertTrue(Dates.formatCurrentTimestamp().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z"));
    }

    // ---------------------------------------------------------------------------------------------
    // J12 - the XML parser writes the factory's lexical fraction

    @Test
    public void j12_parseToXMLGregorianCalendar_writesTheNarrowestFractionOfAtLeastThreeDigits() {
        assertEquals("2025-01-15T10:30:45.123Z", Dates.parseToXMLGregorianCalendar(Z_FRACTION_TEXT).toXMLFormat());
        assertEquals("2025-01-15T10:30:45.000Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.0Z").toXMLFormat());
        assertEquals("2025-01-15T10:30:45.100Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.1Z").toXMLFormat());
        assertEquals("2025-01-15T10:30:45.500Z", Dates.parseToXMLGregorianCalendar("2025-01-15 10:30:45.5", null, UTC).toXMLFormat());
        assertEquals("2025-01-15T10:30:45.0001Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.0001Z").toXMLFormat());
        assertEquals("2025-01-15T10:30:45.123456Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456000Z").toXMLFormat());
        assertEquals("2025-01-15T10:30:45.123456789Z", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789Z").toXMLFormat());
        assertEquals("2025-01-15T10:30:45.123456789+05:30", Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789+05:30").toXMLFormat());

        // the fraction-free shapes are unchanged
        assertEquals("2025-01-15T10:30:45.000Z", Dates.parseToXMLGregorianCalendar(Z_TEXT).toXMLFormat());

        // the default round trip is now text-equal, and still XML-equal to the factory's value
        assertEquals(Z_FRACTION_TEXT, Dates.format(Dates.parseToXMLGregorianCalendar(Z_FRACTION_TEXT)));
        assertEquals(Dates.createXMLGregorianCalendar(MODERN, UTC).toXMLFormat(), Dates.parseToXMLGregorianCalendar(Z_FRACTION_TEXT).toXMLFormat());
        assertEquals(DatatypeConstants.EQUAL, Dates.createXMLGregorianCalendar(MODERN, UTC).compare(Dates.parseToXMLGregorianCalendar(Z_FRACTION_TEXT)));

        // and the nanosecond value is intact
        assertEquals(new BigDecimal("0.123456789"), Dates.parseToXMLGregorianCalendar("2025-01-15T10:30:45.123456789Z").getFractionalSecond());
        assertEquals(new BigDecimal("0.123"), Dates.parseToXMLGregorianCalendar(Z_FRACTION_TEXT).getFractionalSecond());
    }

    // ---------------------------------------------------------------------------------------------
    // O14 - a zone problem is reported as a zone problem

    @Test
    public void o14_anUnexpressibleFallbackZone_isReportedAsTheZoneProblemItIs() {
        final IllegalArgumentException zone = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToInstant("2025-01-15 10:30:45", null, customDstZone()));
        assertTrue(zone.getMessage().startsWith("Time zone 'Custom/Dst'"), zone.getMessage());
        assertFalse(zone.getMessage().contains("Cannot parse"), zone.getMessage());

        final IllegalArgumentException zoned = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToZonedDateTime("2025-01-15T10:30:45", null, customDstZone()));
        assertTrue(zoned.getMessage().startsWith("Time zone 'Custom/Dst'"), zoned.getMessage());

        // the same holds when a fixed-zone constant checks the zone up front, whatever the diagnosis: an
        // unknown ID with daylight-saving rules (its cause is a DateTimeException), a fixed offset outside
        // the java.time range, a sub-second fixed offset, and a registered ID carrying different rules
        final TimeZone unknownDst = new SimpleTimeZone(0, "Mars/Olympus", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0, 3_600_000);
        final TimeZone hugeOffset = new SimpleTimeZone(19 * 3_600_000, "Huge/Offset");
        final TimeZone subSecond = new SimpleTimeZone(500, "Sub/Second");
        final TimeZone liarId = new SimpleTimeZone(-5 * 3_600_000, "America/New_York", Calendar.MARCH, 8, -Calendar.SUNDAY, 7_200_000, Calendar.NOVEMBER, 1,
                -Calendar.SUNDAY, 7_200_000, 3_600_000);

        for (final TimeZone bad : new TimeZone[] { unknownDst, hugeOffset, subSecond, liarId }) {
            final IllegalArgumentException instant = assertThrows(IllegalArgumentException.class,
                    () -> Dates.parseToInstant(Z_TEXT, Dates.ISO_8601_DATE_TIME_FORMAT, bad), bad.getID());
            assertTrue(instant.getMessage().contains("ime zone '" + bad.getID() + "'"), instant.getMessage());
            assertFalse(instant.getMessage().contains("Cannot parse"), instant.getMessage());

            final IllegalArgumentException http = assertThrows(IllegalArgumentException.class,
                    () -> Dates.parseToZonedDateTime(HTTP_TEXT, Dates.HTTP_DATE_FORMAT, bad), bad.getID());
            assertTrue(http.getMessage().contains("ime zone '" + bad.getID() + "'"), http.getMessage());
            assertFalse(http.getMessage().contains("Cannot parse"), http.getMessage());

            // DTF itself wraps every zone diagnosis the same way, with the diagnosis as the cause
            final IllegalArgumentException dtf = assertThrows(IllegalArgumentException.class, () -> DTF.ISO_8601_DATE_TIME.parseToInstant(Z_TEXT, bad),
                    bad.getID());
            assertTrue(dtf.getCause() instanceof IllegalArgumentException, String.valueOf(dtf.getCause()));
            assertTrue(dtf.getCause().getMessage().contains("ime zone '" + bad.getID() + "'"), dtf.getCause().getMessage());
        }

        // text that is actually malformed is still a parse failure naming the input
        final IllegalArgumentException malformed = assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15 10:30:4x", null, UTC));
        assertTrue(malformed.getMessage().startsWith("Cannot parse \"2025-01-15 10:30:4x\""), malformed.getMessage());

        // and a nonexistent wall time too
        final IllegalArgumentException gap = assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-03-09 02:30:00", null, NEW_YORK));
        assertTrue(gap.getMessage().startsWith("Cannot parse"), gap.getMessage());
    }
}
