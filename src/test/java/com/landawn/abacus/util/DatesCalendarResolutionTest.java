package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.util.Dates.DTF;

/** Date resolution and diagnostic regression contracts. */
public class DatesCalendarResolutionTest extends DatesResolutionTestSupport {

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



    // C-083: the Calendar range checks reject a non-lenient calendar holding invalid fields
    @Test
    public void c083_calendarRangeChecksRejectANonLenientCalendarWithInvalidFields() {
        final Calendar c1 = Dates.createCalendar(1000L), c5 = Dates.createCalendar(5000L), c10 = Dates.createCalendar(10000L),
                c15 = Dates.createCalendar(15000L);
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



    // C-041: the Calendar truncation comparison keeps the null-zone fallback through zoneOf
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



    // C-028: the offset-versus-zone check applies to the civil-field parsers too
    @Test
    public void c028_civilParsersRejectAnOffsetInconsistentWithItsZone() {
        final String text = "2023-07-01T12:00:00-08:00[America/Los_Angeles]";

        for (final Executable call : new Executable[] { () -> DTF.ISO_ZONED_DATE_TIME.parseToLocalDate(text),
                () -> DTF.ISO_ZONED_DATE_TIME.parseToLocalTime(text), () -> DTF.ISO_ZONED_DATE_TIME.parseToLocalDateTime(text) }) {
            assertTrue(messageOf(call).contains("Offset -08:00 is not valid"));
        }

        assertEquals(LocalDate.of(2023, 7, 1), DTF.ISO_ZONED_DATE_TIME.parseToLocalDate("2023-07-01T12:00:00-07:00[America/Los_Angeles]"));
    }



    // C-129: the create*(Calendar) factories pass on a non-lenient calendar's own rejection
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



    // C-017: the documented split between the Date and the Calendar overloads
    @Test
    public void c017_dateOverloadsUseTheRegisteredRulesButCalendarOverloadsRejectACustomizedKnownId() {
        final SimpleTimeZone customized = new SimpleTimeZone(-5 * 3600000, "America/New_York", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0, 3600000);
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



    // C-076 (cycle 2): set* resolves the zone once; behaviour is unchanged
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



    // C-002
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

}
