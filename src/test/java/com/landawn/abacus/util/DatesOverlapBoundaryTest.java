/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-03 Dates review (ledger {@code scripts/cross_review/IOUtil_Dates_ledger_2026-09-03.md}):
 * <ul>
 *   <li>C-009: a fall-back overlap that straddles a unit boundary (America/St_Johns 1987-2011, transition at 00:01)
 *       no longer makes {@code truncate}/{@code round}/{@code truncatedEquals} land in the next civil unit;</li>
 *   <li>C-010: the {@code Calendar} civil-field comparisons honour the documented default-zone fallback for a
 *       calendar whose {@code getTimeZone()} is {@code null};</li>
 *   <li>C-011: {@code isSameLocalTime(Date, Date)} reads the civil fields {@code format} prints, pre-1900 included;</li>
 *   <li>C-019: the XML calendar factories render pre-1900 instants through the same historical offsets as
 *       {@code format}, rejecting a sub-minute one instead of silently shifting the fields;</li>
 *   <li>C-020: an offset written in the text is preserved by the {@code Calendar} targets for a legacy pattern that
 *       spells the zone with a run of {@code Z}.</li>
 * </ul>
 */
public class DatesOverlapBoundaryTest extends TestBase {

    private static final String ST_JOHNS = "America/St_Johns";

    private static String render(final long millis, final String zone) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.of(zone)).toString();
    }

    private interface Body {
        void run() throws Exception;
    }

    private static void withDefault(final String zone, final Body body) throws Exception {
        final TimeZone orig = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(zone));

        try {
            body.run();
        } finally {
            TimeZone.setDefault(orig);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-009: straddling overlap
    // ------------------------------------------------------------------------------------------------

    /** 2010-11-07T03:00Z = 2010-11-06 23:30 NST, in the replayed 23:01-00:01 window (second pass). */
    private static final long SECOND_PASS = 1289098800000L;
    /** 2010-11-07T02:00Z = 2010-11-06 23:30 NDT, the same wall time on the first pass. */
    private static final long FIRST_PASS = 1289095200000L;
    /** 2010-11-07T02:30:30Z = 2010-11-07 00:00:30 NDT, the one minute of Nov 7 that was later replayed away. */
    private static final long BRIEF_NOV_7 = 1289097030000L;

    @Test
    public void truncateStaysOnTheInputsCivilDateWhenTheOverlapStraddlesMidnight() throws Exception {
        withDefault(ST_JOHNS, () -> {
            final Date d = new Date(SECOND_PASS);
            assertEquals("2010-11-06T23:30-03:30[America/St_Johns]", render(SECOND_PASS, ST_JOHNS));

            assertEquals("2010-11-06T00:00-02:30[America/St_Johns]", render(Dates.truncate(d, Calendar.DATE).getTime(), ST_JOHNS));
            assertEquals("2010-11-06T23:00-02:30[America/St_Johns]", render(Dates.truncate(d, Calendar.HOUR_OF_DAY).getTime(), ST_JOHNS));
            assertEquals("2010-11-06T23:30-03:30[America/St_Johns]", render(Dates.truncate(d, Calendar.MINUTE).getTime(), ST_JOHNS));
            assertEquals("2010-11-01T00:00-02:30[America/St_Johns]", render(Dates.truncate(d, Calendar.MONTH).getTime(), ST_JOHNS));
            assertEquals("2010-11-07T00:00-03:30[America/St_Johns]", render(Dates.ceiling(d, Calendar.DATE).getTime(), ST_JOHNS));
            assertEquals("2010-11-07T00:00-03:30[America/St_Johns]", render(Dates.ceiling(d, Calendar.HOUR_OF_DAY).getTime(), ST_JOHNS));
            // 30 minutes to the next day, 24.5 hours back to the start of this one
            assertEquals("2010-11-07T00:00-03:30[America/St_Johns]", render(Dates.round(d, Calendar.DATE).getTime(), ST_JOHNS));
            assertEquals("2010-11-07T00:00-03:30[America/St_Johns]", render(Dates.round(d, Calendar.HOUR_OF_DAY).getTime(), ST_JOHNS));

            assertTrue(Dates.isSameDay(d, Dates.truncate(d, Calendar.DATE)));
            assertTrue(Dates.truncatedEquals(d, new Date(FIRST_PASS), CalendarField.DAY_OF_MONTH));
            assertTrue(Dates.truncatedEquals(d, new Date(FIRST_PASS), CalendarField.HOUR_OF_DAY));
            assertEquals(0, Dates.truncatedCompareTo(new Date(FIRST_PASS), d, CalendarField.DAY_OF_MONTH));
            assertTrue(Dates.truncatedCompareTo(d, new Date(BRIEF_NOV_7), CalendarField.DAY_OF_MONTH) < 0);

            // the Calendar overloads take the same route
            final Calendar c = Dates.createCalendar(SECOND_PASS, TimeZone.getTimeZone(ST_JOHNS));
            assertEquals("2010-11-06T00:00-02:30[America/St_Johns]", render(Dates.truncate(c, Calendar.DATE).getTimeInMillis(), ST_JOHNS));
            assertEquals("2010-11-06T23:00-02:30[America/St_Johns]", render(Dates.truncate(c, Calendar.HOUR_OF_DAY).getTimeInMillis(), ST_JOHNS));
        });
    }

    @Test
    public void theFirstPassAndTheReplayedMinuteOfNov7AreUnchanged() throws Exception {
        withDefault(ST_JOHNS, () -> {
            final Date first = new Date(FIRST_PASS);
            assertEquals("2010-11-06T00:00-02:30[America/St_Johns]", render(Dates.truncate(first, Calendar.DATE).getTime(), ST_JOHNS));
            assertEquals("2010-11-06T23:00-02:30[America/St_Johns]", render(Dates.truncate(first, Calendar.HOUR_OF_DAY).getTime(), ST_JOHNS));
            // the next day boundary after the first pass is the brief Nov 7 midnight that really happened
            assertEquals("2010-11-07T00:00-02:30[America/St_Johns]", render(Dates.ceiling(first, Calendar.DATE).getTime(), ST_JOHNS));

            final Date brief = new Date(BRIEF_NOV_7);
            assertEquals("2010-11-07T00:00:30-02:30[America/St_Johns]", render(BRIEF_NOV_7, ST_JOHNS));
            assertEquals("2010-11-07T00:00-02:30[America/St_Johns]", render(Dates.truncate(brief, Calendar.DATE).getTime(), ST_JOHNS));
            assertEquals("2010-11-07T00:00-02:30[America/St_Johns]", render(Dates.truncate(brief, Calendar.HOUR_OF_DAY).getTime(), ST_JOHNS));
            assertFalse(Dates.truncatedEquals(brief, new Date(SECOND_PASS), CalendarField.DAY_OF_MONTH));
        });
    }

    @Test
    public void truncateIsMonotonicIdempotentAndDateStableAcrossEveryStJohnsOverlap() throws Exception {
        withDefault(ST_JOHNS, () -> {
            final ZoneId zone = ZoneId.of(ST_JOHNS);
            final ZoneRules rules = zone.getRules();
            Instant cur = Instant.parse("1985-01-01T00:00:00Z");

            for (int i = 0; i < 80; i++) {
                final ZoneOffsetTransition t = rules.nextTransition(cur);

                if (t == null) {
                    break;
                }

                cur = t.getInstant();

                if (!t.isOverlap()) {
                    continue;
                }

                final long tm = cur.toEpochMilli();
                long previousMs = Long.MIN_VALUE;

                for (long delta = -7_200_000L; delta <= 7_200_000L; delta += 15_000L) {
                    final long ms = tm + delta;
                    final Date d = new Date(ms);

                    for (final int field : new int[] { Calendar.DATE, Calendar.HOUR_OF_DAY, Calendar.MINUTE }) {
                        final long floor = Dates.truncate(d, field).getTime();
                        assertTrue(floor <= ms, "floor after input at " + render(ms, ST_JOHNS));
                        assertEquals(floor, Dates.truncate(new Date(floor), field).getTime(), "not idempotent at " + render(ms, ST_JOHNS));
                        assertTrue(Dates.ceiling(d, field).getTime() >= ms, "ceiling before input at " + render(ms, ST_JOHNS));
                    }

                    final long dayFloor = Dates.truncate(d, Calendar.DATE).getTime();
                    final ZonedDateTime z = Instant.ofEpochMilli(ms).atZone(zone);
                    assertEquals(z.toLocalDate(), Instant.ofEpochMilli(dayFloor).atZone(zone).toLocalDate(), "wrong civil date at " + z);

                    // The civil days interleave in instant order across such a fall-back (Nov 7 00:00-00:00:59 was struck
                    // before the clock fell back into Nov 6 23:01), so the day floor cannot be monotonic in instant order;
                    // what must hold is that truncatedCompareTo orders the samples exactly as their civil dates do.
                    if (previousMs != Long.MIN_VALUE) {
                        final ZonedDateTime previous = Instant.ofEpochMilli(previousMs).atZone(zone);
                        assertEquals(Integer.signum(z.toLocalDate().compareTo(previous.toLocalDate())),
                                Integer.signum(Dates.truncatedCompareTo(d, new Date(previousMs), CalendarField.DAY_OF_MONTH)), "civil order at " + z);
                        assertEquals(z.toLocalDate().equals(previous.toLocalDate()), Dates.truncatedEquals(d, new Date(previousMs), CalendarField.DAY_OF_MONTH),
                                "truncatedEquals(DAY) vs the civil dates at " + z);
                    }

                    previousMs = ms;
                }
            }
        });
    }

    // ------------------------------------------------------------------------------------------------
    // C-010: null-zone Calendar fallback
    // ------------------------------------------------------------------------------------------------

    private static Calendar nullZoneCalendar(final long millis) {
        final Calendar c = new GregorianCalendar() {
            @Override
            public TimeZone getTimeZone() {
                return null;
            }
        };
        c.setTimeInMillis(millis);
        return c;
    }

    @Test
    public void calendarCivilComparisonsFallBackToTheDefaultZoneForANullZoneCalendar() throws Exception {
        withDefault("Asia/Tokyo", () -> {
            final long noon = ZonedDateTime.parse("2024-03-10T12:00+09:00[Asia/Tokyo]").toInstant().toEpochMilli();
            final long evening = ZonedDateTime.parse("2024-03-10T23:30+09:00[Asia/Tokyo]").toInstant().toEpochMilli();
            final long nextDay = ZonedDateTime.parse("2024-03-11T00:30+09:00[Asia/Tokyo]").toInstant().toEpochMilli();

            final Calendar tokyo = Dates.createCalendar(noon, TimeZone.getTimeZone("Asia/Tokyo"));
            assertTrue(Dates.isSameDay(nullZoneCalendar(evening), tokyo));
            assertTrue(Dates.isSameDay(tokyo, nullZoneCalendar(evening)));
            assertFalse(Dates.isSameDay(nullZoneCalendar(nextDay), tokyo));
            assertTrue(Dates.isSameDay(nullZoneCalendar(noon), nullZoneCalendar(evening)));
            assertTrue(Dates.isSameMonth(nullZoneCalendar(noon), tokyo));
            assertTrue(Dates.isSameYear(nullZoneCalendar(noon), tokyo));

            // a calendar in another zone is still incompatible, null zone or not
            final Calendar london = Dates.createCalendar(noon, TimeZone.getTimeZone("Europe/London"));
            assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(nullZoneCalendar(noon), london));
            assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(london, nullZoneCalendar(noon)));
        });
    }

    // ------------------------------------------------------------------------------------------------
    // C-011: isSameLocalTime pre-1900
    // ------------------------------------------------------------------------------------------------

    @Test
    public void isSameLocalTimeReadsTheFieldsFormatPrintsBefore1900() throws Exception {
        withDefault("America/New_York", () -> {
            // 1883-11-18: New York went from LMT -4:56:02 to EST -5:00 at 17:00Z, so 12:02:58 local happened twice.
            final Date lmt = Date.from(Instant.parse("1883-11-18T16:59:00Z"));
            final Date est = Date.from(Instant.parse("1883-11-18T17:02:58Z"));
            assertEquals("1883-11-18 12:02:58", Dates.format(lmt, Dates.LOCAL_DATE_TIME_FORMAT));
            assertEquals("1883-11-18 12:02:58", Dates.format(est, Dates.LOCAL_DATE_TIME_FORMAT));

            assertTrue(Dates.isSameLocalTime(lmt, est), "same rendered fields must compare equal");
            assertFalse(Dates.isSameLocalTime(lmt, new Date(lmt.getTime() + 1000)));
            assertEquals(Dates.getFragmentInSeconds(lmt, CalendarField.DAY_OF_MONTH), Dates.getFragmentInSeconds(est, CalendarField.DAY_OF_MONTH));

            // after 1900 nothing changes
            final Date a = Date.from(Instant.parse("2024-07-04T16:30:00Z"));
            final Date b = Date.from(Instant.parse("2023-07-04T16:30:00Z"));
            assertFalse(Dates.isSameLocalTime(a, b));
            assertTrue(Dates.isSameLocalTime(a, new Date(a.getTime())));
        });
    }

    // ------------------------------------------------------------------------------------------------
    // C-019: XML calendars and pre-1900 offsets
    // ------------------------------------------------------------------------------------------------

    @Test
    public void xmlCalendarsRenderPre1900InstantsLikeFormatOrRejectAnUnrepresentableOffset() {
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        // 1899 Calcutta ran on +05:21:10: format prints the text back, the XML offset cannot carry the seconds
        final Date d = Dates.parseToJUDate("1899-06-15 12:00:00", null, kolkata);
        assertEquals("1899-06-15 12:00:00", Dates.format(d, Dates.LOCAL_DATE_TIME_FORMAT, kolkata));
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToXMLGregorianCalendar("1899-06-15 12:00:00", null, kolkata));
        assertTrue(e.getMessage().contains("whole-minute"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(d.getTime(), kolkata));

        // a pre-1900 instant on a whole-minute historical offset renders exactly as format does
        final TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        final Date ny1890 = Dates.parseToJUDate("1890-06-15 12:00:00", null, newYork);
        final XMLGregorianCalendar xml = Dates.parseToXMLGregorianCalendar("1890-06-15 12:00:00", null, newYork);
        assertEquals("1890-06-15T12:00:00.000-05:00", xml.toXMLFormat());
        assertEquals("1890-06-15T12:00:00.000-05:00", Dates.createXMLGregorianCalendar(ny1890.getTime(), newYork).toXMLFormat());

        // and nothing changes after 1900
        assertEquals("2024-01-15T12:00:00.000+05:30", Dates.parseToXMLGregorianCalendar("2024-01-15 12:00:00", null, kolkata).toXMLFormat());
        assertEquals("2024-01-15T12:00:00.000+05:30",
                Dates.createXMLGregorianCalendar(Dates.parseToJUDate("2024-01-15 12:00:00", null, kolkata).getTime(), kolkata).toXMLFormat());
    }

    // ------------------------------------------------------------------------------------------------
    // C-020: zone provenance for a run of Z
    // ------------------------------------------------------------------------------------------------

    @Test
    public void calendarTargetsPreserveAnOffsetSpelledWithARunOfZ() {
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final long expected = Instant.parse("2025-01-15T18:30:45Z").toEpochMilli();

        for (final String pattern : new String[] { "yyyy-MM-dd HH:mm:ss Z", "yyyy-MM-dd HH:mm:ss ZZ", "yyyy-MM-dd HH:mm:ss ZZZZ" }) {
            final Calendar c = Dates.parseToCalendar("2025-01-15 10:30:45 -0800", pattern, kolkata);
            assertEquals(expected, c.getTimeInMillis(), pattern);
            assertEquals(-8 * 3_600_000, c.getTimeZone().getRawOffset(), pattern);
            assertEquals(-8 * 3_600_000, Dates.parseToGregorianCalendar("2025-01-15 10:30:45 -0800", pattern, kolkata).getTimeZone().getRawOffset(), pattern);
            assertEquals(-480, Dates.parseToXMLGregorianCalendar("2025-01-15 10:30:45 -0800", pattern, kolkata).getTimezone(), pattern);
        }

        // no zone in the text: the supplied fallback zone is what the calendar carries
        assertEquals("Asia/Kolkata", Dates.parseToCalendar("2025-01-15 10:30:45", "yyyy-MM-dd HH:mm:ss", kolkata).getTimeZone().getID());
    }

    // ------------------------------------------------------------------------------------------------
    // C-035: year 0000 on the auto-detected fraction shapes
    // ------------------------------------------------------------------------------------------------

    @Test
    public void yearZeroIsRejectedOnEveryAutoDetectedFractionShape() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        for (final String text : new String[] { "0000-01-15T10:30:45.123", "0000-01-15T10:30:45.123Z", "0000-01-15T10:30:45.123Z[UTC]",
                "0000-01-15T10:30:45.123+05:30", "0000-01-15 10:30:45.123", "0000-01-15T10:30:45.123456789Z" }) {
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(text), text);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(text, null, utc), text);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(text), text);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text), text);
        }

        // year 0001 stays accepted on the same shapes, fraction intact
        assertEquals(Instant.parse("0001-01-15T10:30:45.123Z").toEpochMilli(), Dates.parseToTimestamp("0001-01-15T10:30:45.123Z").getTime());
        assertEquals(123_000_000, Dates.parseToTimestamp("0001-01-15T10:30:45.123Z").getNanos());
        assertEquals(Instant.parse("0001-01-15T10:30:45.123Z").toEpochMilli(), Dates.parseToTimestamp("0001-01-15T10:30:45.123Z[UTC]").getTime());
        assertEquals(Instant.parse("0001-01-15T10:30:45.123Z").toEpochMilli(), Dates.parseToTimestamp("0001-01-15T10:30:45.123", null, utc).getTime());
        assertEquals(1, Dates.parseToXMLGregorianCalendar("0001-01-15T10:30:45.123Z").getYear());
    }

    // ------------------------------------------------------------------------------------------------
    // C-036: the XML default rendering with an override zone checks the offset it renders
    // ------------------------------------------------------------------------------------------------

    @Test
    public void xmlDefaultFormatWithAnOverrideZoneChecksTheOffsetItRenders() {
        final TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final XMLGregorianCalendar utc1880 = Dates.createXMLGregorianCalendar(Instant.parse("1880-06-15T12:00:00Z").toEpochMilli(), utc);
        final XMLGregorianCalendar utc1890 = Dates.createXMLGregorianCalendar(Instant.parse("1890-06-15T12:00:00Z").toEpochMilli(), utc);

        // New York's local-mean-time offset before 1883-11-18 (-04:56:02) is not a whole minute, which
        // XML Schema cannot carry: the legacy table's flat -05:00 must not let a "-04:56:02" through
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.format(utc1880, null, newYork));
        assertTrue(e.getMessage().contains("whole-minute"), e.getMessage());

        // a representable override still renders, before and after 1900
        assertEquals("1880-06-15T12:00:00Z", Dates.format(utc1880, null, utc));
        assertEquals("1880-06-15T13:00:00+01:00", Dates.format(utc1880, null, TimeZone.getTimeZone("GMT+01:00")));
        assertEquals("1890-06-15T07:00:00-05:00", Dates.format(utc1890, null, newYork));
        final XMLGregorianCalendar utc2024 = Dates.createXMLGregorianCalendar(Instant.parse("2024-06-15T12:00:00Z").toEpochMilli(), utc);
        assertEquals("2024-06-15T08:00:00-04:00", Dates.format(utc2024, null, newYork));
        assertEquals("2024-06-15T12:00:00.000Z", Dates.format(utc2024, null, (TimeZone) null)); // the XML lexical form keeps its own fraction
    }

    // ------------------------------------------------------------------------------------------------
    // C-039: an hour-only offset is rejected with a fraction exactly as without one
    // ------------------------------------------------------------------------------------------------

    @Test
    public void hourOnlyOffsetIsRejectedWithAndWithoutAFraction() {
        final long expected = Instant.parse("2025-01-15T18:30:45.123Z").toEpochMilli();

        for (final String ok : new String[] { "2025-01-15T10:30:45.123-08:00", "2025-01-15T10:30:45.123-0800", "2025-01-15T10:30:45.123-08:00:00" }) {
            assertEquals(expected, Dates.parseToTimestamp(ok).getTime(), ok);
            assertEquals(expected, Dates.parseToJUDate(ok).getTime(), ok);
        }

        for (final String bad : new String[] { "2025-01-15T10:30:45.123-08", "2025-01-15T10:30:45-08", "2025-01-15T10:30:45.123-8:00",
                "2025-01-15T10:30:45.123-08:0", "2025-01-15T10:30:45.123-08:00:0", "2025-01-15T10:30:45.123-08:00:00:00" }) {
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(bad), bad);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C-040: the seconds-less bracketed-zone form is detected
    // ------------------------------------------------------------------------------------------------

    @Test
    public void secondsLessBracketedZoneFormIsDetected() {
        final long expected = Instant.parse("2025-01-15T10:30:00Z").toEpochMilli();

        assertEquals(expected, Dates.parseToTimestamp("2025-01-15T10:30Z[UTC]").getTime());
        assertEquals(expected, Dates.parseToJUDate("2025-01-15T10:30Z[UTC]").getTime());
        assertEquals(expected, Dates.parseToCalendar("2025-01-15T10:30Z[UTC]").getTimeInMillis());
        assertEquals(Instant.parse("2025-01-15T05:00:00Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15T10:30+05:30[Asia/Kolkata]").getTime());

        // the seconds form was always accepted
        assertEquals(expected, Dates.parseToJUDate("2025-01-15T10:30:00Z[UTC]").getTime());
    }

    // ------------------------------------------------------------------------------------------------
    // C-034: a zone NAME in the text must not hand the calendar a region that shows another wall clock
    // ------------------------------------------------------------------------------------------------

    @Test
    public void zoneNameInTheTextKeepsTheWallClockTheTextSpells() {
        final String pattern = "yyyy-MM-dd HH:mm:ss z";
        final TimeZone brisbane = TimeZone.getTimeZone("Australia/Brisbane");
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        // AEST with a Brisbane fallback: SimpleDateFormat resolved Brisbane (+10) while java.time maps
        // AEST to Sydney, which is on +11 in January - the calendar used to show 21:30 AEDT
        final Calendar fromBrisbane = Dates.parseToCalendar("2025-01-15 20:30:45 AEST", pattern, brisbane);
        assertEquals(Instant.parse("2025-01-15T10:30:45Z").toEpochMilli(), fromBrisbane.getTimeInMillis());
        assertEquals("Australia/Brisbane", fromBrisbane.getTimeZone().getID());
        assertEquals(20, fromBrisbane.get(Calendar.HOUR_OF_DAY));
        assertEquals("2025-01-15 20:30:45 AEST", Dates.format(fromBrisbane.getTime(), pattern, fromBrisbane.getTimeZone()));

        // IST with Kolkata (java.time: Africa/Abidjan, +0) and CST with Shanghai (java.time: Chicago, the previous day)
        final Calendar fromKolkata = Dates.parseToCalendar("2025-01-15 12:30:45 IST", pattern, kolkata);
        assertEquals("Asia/Kolkata", fromKolkata.getTimeZone().getID());
        assertEquals(12, fromKolkata.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, fromKolkata.get(Calendar.MINUTE));
        final Calendar fromShanghai = Dates.parseToCalendar("2025-01-15 10:30:45 CST", pattern, TimeZone.getTimeZone("Asia/Shanghai"));
        assertEquals("Asia/Shanghai", fromShanghai.getTimeZone().getID());
        assertEquals(15, fromShanghai.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, fromShanghai.get(Calendar.HOUR_OF_DAY));

        // the GregorianCalendar and XML targets agree
        assertEquals(20, Dates.parseToGregorianCalendar("2025-01-15 20:30:45 AEST", pattern, brisbane).get(Calendar.HOUR_OF_DAY));
        final XMLGregorianCalendar xml = Dates.parseToXMLGregorianCalendar("2025-01-15 20:30:45 AEST", pattern, brisbane);
        assertEquals(20, xml.getHour());
        assertEquals(600, xml.getTimezone());

        // a name the fallback does not own resolves to the JDK's region when that region shows the same wall clock
        final Calendar la = Dates.parseToCalendar("2025-01-15 12:30:45 PST", pattern, utc);
        assertEquals("America/Los_Angeles", la.getTimeZone().getID());
        assertEquals(12, la.get(Calendar.HOUR_OF_DAY));

        // a standard-time name on a date the zone spends on daylight time reproduces in no zone: the fields
        // stay the text's, on the fixed offset the text implies
        final Calendar summerAest = Dates.parseToCalendar("2025-01-15 20:30:45 AEST", pattern, TimeZone.getTimeZone("Australia/Sydney"));
        assertEquals(20, summerAest.get(Calendar.HOUR_OF_DAY));
        assertEquals(10 * 3_600_000, summerAest.getTimeZone().getOffset(summerAest.getTimeInMillis()));
        assertEquals(Instant.parse("2025-01-15T10:30:45Z").toEpochMilli(), summerAest.getTimeInMillis());

        // a wall clock a fall-back overlap repeats keeps the region: 01:30 EST is the second occurrence
        final Calendar overlap = Dates.parseToCalendar("2025-11-02 01:30:00 EST", pattern, utc);
        assertEquals("America/New_York", overlap.getTimeZone().getID());
        assertEquals(1, overlap.get(Calendar.HOUR_OF_DAY));
        assertEquals(Instant.parse("2025-11-02T06:30:00Z").toEpochMilli(), overlap.getTimeInMillis());

        // lower case and a long name against a single 'z' are re-parsed as SimpleDateFormat read them
        final Calendar lowerCase = Dates.parseToCalendar("2025-01-15 20:30:45 aest", pattern, brisbane);
        assertEquals("Australia/Brisbane", lowerCase.getTimeZone().getID());
        assertEquals(20, lowerCase.get(Calendar.HOUR_OF_DAY));
        final Calendar longName = Dates.parseToCalendar("2025-01-15 12:30:45 Pacific Standard Time", pattern, utc);
        assertEquals("America/Los_Angeles", longName.getTimeZone().getID());
        assertEquals(12, longName.get(Calendar.HOUR_OF_DAY));

        // fixed-offset text keeps its own identity, and zone-less text keeps the fallback
        assertEquals("GMT+05:30", Dates.parseToCalendar("2025-01-15 12:30:45 GMT+05:30", pattern, kolkata).getTimeZone().getID());
        assertEquals("UTC", Dates.parseToCalendar("2025-01-15 12:30:45 UTC", pattern, kolkata).getTimeZone().getID());
        assertEquals("Asia/Kolkata", Dates.parseToCalendar("2025-01-15 12:30:45", "yyyy-MM-dd HH:mm:ss", kolkata).getTimeZone().getID());
    }

    @Test
    public void zoneNameRoundTripsThroughEveryZoneAndBothSeasons() {
        final String pattern = "yyyy-MM-dd HH:mm:ss z";
        final long january = Instant.parse("2025-01-15T10:30:45Z").toEpochMilli();
        final long july = Instant.parse("2025-07-15T10:30:45Z").toEpochMilli();
        final List<String> wrongWallClock = new ArrayList<>();

        for (final String id : TimeZone.getAvailableIDs()) {
            final TimeZone zone = TimeZone.getTimeZone(id);

            for (final long instant : new long[] { january, july }) {
                final Calendar original = Dates.createCalendar(instant, zone);
                final String text = Dates.format(original.getTime(), pattern, zone);
                final Calendar back;

                try {
                    back = Dates.parseToCalendar(text, pattern, zone);
                } catch (final IllegalArgumentException e) {
                    continue; // a rendering the legacy parser cannot read back is not this finding
                }

                if (back.get(Calendar.HOUR_OF_DAY) != original.get(Calendar.HOUR_OF_DAY) || back.get(Calendar.MINUTE) != original.get(Calendar.MINUTE)
                        || back.get(Calendar.DAY_OF_MONTH) != original.get(Calendar.DAY_OF_MONTH)) {
                    wrongWallClock.add(id + " " + text + " -> " + Dates.format(back.getTime(), pattern, back.getTimeZone()));
                }
            }
        }

        assertEquals(Collections.emptyList(), wrongWallClock);
    }

    // ------------------------------------------------------------------------------------------------
    // C-042 / C-043: the legacy zone table also ends at 2100, and a set* target outside the trusted
    //                range resolves on the offset java.time gives it
    // ------------------------------------------------------------------------------------------------

    @Test
    public void legacyEngineFollowsJavaTimeWhereTheLegacyTableEndsIn2100() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            for (final String id : new String[] { "Africa/Casablanca", "Africa/El_Aaiun", "Africa/Windhoek" }) {
                TimeZone.setDefault(TimeZone.getTimeZone(id));

                // a 2100 rendering round-trips, and the day floor prints on the same day
                final Date d = Dates.parseToJUDate("2100-06-15 00:30:00", Dates.LOCAL_DATE_TIME_FORMAT);
                assertEquals("2100-06-15 00:30:00", Dates.format(d, Dates.LOCAL_DATE_TIME_FORMAT), id);
                assertEquals("2100-06-15 00:00:00", Dates.format(Dates.truncate(d, Calendar.DAY_OF_MONTH), Dates.LOCAL_DATE_TIME_FORMAT), id);
                assertTrue(Dates.isSameDay(d, Dates.truncate(d, Calendar.DAY_OF_MONTH)), id);

                // set* lands where add* does, far in the future and before 1900
                final Date c = Dates.parseToJUDate("2025-06-15 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT);
                assertEquals(Dates.addYears(c, 7974).getTime(), Dates.setYears(c, 9999).getTime(), id);
                assertEquals("9999-06-15 12:00:00", Dates.format(Dates.setYears(c, 9999), Dates.LOCAL_DATE_TIME_FORMAT), id);
                assertEquals("9999-06-15 12:00:00", Dates.format(Dates.addYears(c, 7974), Dates.LOCAL_DATE_TIME_FORMAT), id);
                assertEquals(Dates.addYears(c, -175).getTime(), Dates.setYears(c, 1850).getTime(), id);

                // setting a field to the value it already holds never moves a far-future instant
                final Date far = Dates.setYears(c, 9999);
                assertEquals(far.getTime(), Dates.setYears(far, 9999).getTime(), id);
                assertEquals(far.getTime(), Dates.setHours(far, 12).getTime(), id);
                assertEquals(far.getTime(), Dates.setMonths(far, 5).getTime(), id);
            }

            // a zone whose recurring rule both engines project alike is untouched, and its pre-1900 set*
            // target follows java.time's local-mean-time offset like add* does
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
            final Date ny = Dates.parseToJUDate("2100-07-04 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT);
            assertEquals("2100-07-04 12:00:00 EDT", Dates.format(ny, "yyyy-MM-dd HH:mm:ss z"));
            assertEquals("2100-07-04 12:00:00",
                    Dates.format(Dates.setYears(Dates.parseToJUDate("2025-07-04 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT), 2100), Dates.LOCAL_DATE_TIME_FORMAT));
            final Date c = Dates.parseToJUDate("2025-06-15 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT);
            assertEquals(Instant.parse("1850-06-15T16:56:02Z").toEpochMilli(), Dates.setYears(c, 1850).getTime());
            assertEquals(Dates.addYears(c, -175).getTime(), Dates.setYears(c, 1850).getTime());
            assertEquals("1850-06-15 12:00:00", Dates.format(Dates.setYears(c, 1850), Dates.LOCAL_DATE_TIME_FORMAT));
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    @Test
    public void farFutureSourceMovedBackIntoTheZonesHistoryFollowsThatHistory() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            final ZoneId casablanca = ZoneId.of("Africa/Casablanca");
            TimeZone.setDefault(TimeZone.getTimeZone(casablanca));

            // 2100-03-15 12:00 moved 75 years back lands in Ramadan 2025, when Casablanca is on +00: the
            // stand-in that renders the 2100 value must not resolve the result on its single +01 offset
            final ZonedDateTime far = ZonedDateTime.of(2100, 3, 15, 12, 0, 0, 0, casablanca);
            final Date farDate = Date.from(far.toInstant());
            assertEquals(far.minusYears(75).toInstant().toEpochMilli(), Dates.addYears(farDate, -75).getTime());
            assertEquals("2025-03-15 12:00:00", Dates.format(Dates.addYears(farDate, -75), Dates.LOCAL_DATE_TIME_FORMAT));
            assertEquals(Dates.addYears(farDate, -75).getTime(), Dates.setYears(farDate, 2025).getTime());

            // 2100-04-06 02:30 set to 2025 names a wall clock that year's spring-forward gap removes: forward,
            // as ZonedDateTime resolves it, with the year that was set intact
            final ZonedDateTime gapSource = ZonedDateTime.of(2100, 4, 6, 2, 30, 0, 0, casablanca);
            final Date gapSourceDate = Date.from(gapSource.toInstant());
            assertEquals(gapSource.withYear(2025).toInstant().toEpochMilli(), Dates.setYears(gapSourceDate, 2025).getTime());
            assertEquals("2025-04-06 03:30:00", Dates.format(Dates.setYears(gapSourceDate, 2025), Dates.LOCAL_DATE_TIME_FORMAT));

            // Windhoek's table closes at local standard midnight: the last hour of 2099 already renders on +02
            final ZoneId windhoek = ZoneId.of("Africa/Windhoek");
            TimeZone.setDefault(TimeZone.getTimeZone(windhoek));
            final Date lastHour = new Date(Instant.parse("2099-12-31T23:30:00Z").toEpochMilli());
            assertEquals("2100-01-01 01:30:00", Dates.format(lastHour, Dates.LOCAL_DATE_TIME_FORMAT));
            assertEquals(lastHour.getTime(), Dates.setHours(lastHour, 1).getTime());
            assertEquals(lastHour.getTime(), Dates.setYears(lastHour, 2100).getTime());
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 3: C-070 two-digit years with a zone name, C-071 spellings only SimpleDateFormat accepts,
    //          C-072 explicit ISO_OFFSET_TIMESTAMP_FORMAT beyond +14:00, C-074 hour-only offset before a
    //          bracketed zone, C-075 a space-separated text ending in Z
    // ------------------------------------------------------------------------------------------------

    @Test
    public void twoDigitYearWithAZoneNameStillKeepsTheWallClockTheTextSpells() {
        final TimeZone brisbane = TimeZone.getTimeZone("Australia/Brisbane");
        final String pattern = "yy-MM-dd HH:mm:ss z";

        // SimpleDateFormat reads "75" as 1975 (its 80-years-back window); the java.time re-parse read it as 2075,
        // so the implied offset was days off, and the unverified region showed 11:30
        for (final String yy : new String[] { "75", "99", "25", "46" }) {
            final Calendar c = Dates.parseToCalendar(yy + "-07-15 10:30:45 PST", pattern, brisbane);
            assertEquals(10, c.get(Calendar.HOUR_OF_DAY), yy);
            assertEquals(30, c.get(Calendar.MINUTE), yy);
            assertEquals(Dates.parseToJUDate(yy + "-07-15 10:30:45 PST", pattern, brisbane).getTime(), c.getTimeInMillis(), yy);
            assertEquals(10, Dates.parseToXMLGregorianCalendar(yy + "-07-15 10:30:45 PST", pattern, brisbane).getHour(), yy);
        }

        final Calendar summer = Dates.parseToCalendar("75-07-15 10:30:45 PST", pattern, brisbane);
        assertEquals(1975, summer.get(Calendar.YEAR));
        // a standard-time name on a July date reproduces in no region: the fixed -08:00 the text implies
        assertEquals(-8 * 3_600_000, summer.getTimeZone().getOffset(summer.getTimeInMillis()));
        assertEquals("America/Los_Angeles", Dates.parseToCalendar("75-01-15 10:30:45 PST", pattern, brisbane).getTimeZone().getID());
    }

    @Test
    public void spellingsSimpleDateFormatAcceptsAreReReadForTheZoneToo() {
        final TimeZone brisbane = TimeZone.getTimeZone("Australia/Brisbane");
        final long expected = Instant.parse("2025-01-15T18:30:45Z").toEpochMilli();

        // a long weekday and month name under EEE / MMM
        final Calendar longNames = Dates.parseToCalendar("Wednesday 15 January 2025 10:30:45 PST", "EEE dd MMM yyyy HH:mm:ss z", brisbane);
        assertEquals(expected, longNames.getTimeInMillis());
        assertEquals(10, longNames.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, longNames.get(Calendar.DAY_OF_MONTH));
        assertEquals("America/Los_Angeles", longNames.getTimeZone().getID());

        // a short name under EEEE / MMMM
        final Calendar shortNames = Dates.parseToCalendar("Wed 15 Jan 2025 10:30:45 PST", "EEEE dd MMMM yyyy HH:mm:ss z", brisbane);
        assertEquals(expected, shortNames.getTimeInMillis());
        assertEquals(10, shortNames.get(Calendar.HOUR_OF_DAY));

        // a millisecond count under a single S, which java.time reads as a tenth of a second
        final Calendar count = Dates.parseToCalendar("2025-01-15 10:30:45.123 PST", "yyyy-MM-dd HH:mm:ss.S z", brisbane);
        assertEquals(expected + 123, count.getTimeInMillis());
        assertEquals(10, count.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void explicitOffsetTimestampConstantAcceptsEveryOffsetItsDocumentationPromises() {
        final String f = Dates.ISO_OFFSET_TIMESTAMP_FORMAT;

        for (final String offset : new String[] { "-14:00", "-13:30", "+14:01", "+15:00", "-18:00", "+18:00", "-1400" }) {
            final String text = "2025-01-15T10:30:45.123" + offset;
            final long expected = Dates.parseToInstant(text, f).toEpochMilli();
            assertEquals(expected, Dates.parseToTimestamp(text, f).getTime(), text);
            assertEquals(expected, Dates.parseToJUDate(text, f).getTime(), text);
            assertEquals(expected, Dates.parseToCalendar(text, f).getTimeInMillis(), text);
            assertEquals(expected, Dates.parseToTimestamp(text).getTime(), text); // and auto-detected, as before
        }

        // the constant's own rendering of an instant in a zone with a wide local-mean-time offset reads back
        final TimeZone guam = TimeZone.getTimeZone("Pacific/Guam");
        final Timestamp early = new Timestamp(Instant.parse("0001-06-15T12:00:00Z").toEpochMilli());
        final String rendered = Dates.format(early, f, guam);
        assertTrue(rendered.endsWith("-14:21"), rendered);
        assertEquals(early.getTime(), Dates.parseToTimestamp(rendered, f, guam).getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.123+19:00", f));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.1234-14:00", f));
    }

    @Test
    public void hourOnlyOffsetBeforeABracketedZoneIsRejectedLikeEverywhereElse() {
        for (final String bad : new String[] { "2025-01-15T10:30:45-05[America/New_York]", "2025-01-15T10:30:45.123-05[America/New_York]",
                "2025-01-15T10:30:45-5:00[America/New_York]" }) {
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(bad), bad);
        }

        final long expected = Instant.parse("2025-01-15T15:30:45Z").toEpochMilli();
        assertEquals(expected, Dates.parseToInstant("2025-01-15T10:30:45-05:00[America/New_York]").toEpochMilli());
        assertEquals(expected, Dates.parseToJUDate("2025-01-15T10:30:45-05:00:00[America/New_York]").getTime());
        assertEquals(Instant.parse("2025-01-15T10:30:45Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15T10:30:45Z[UTC]").getTime());
    }

    @Test
    public void spaceSeparatedTextEndingInZIsNotMistakenForTheTForm() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15 10:30:45Z"));
        assertFalse(e.getMessage().contains("'T'HH:mm:ss'Z'"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15 10:30:45Z"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("2025-01-15 10:30:45Z"));

        // the T form with Z, and the space form without, are unchanged
        assertEquals(Instant.parse("2025-01-15T10:30:45Z").toEpochMilli(), Dates.parseToJUDate("2025-01-15T10:30:45Z").getTime());
        assertEquals(Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT).getTime(), Dates.parseToJUDate("2025-01-15 10:30:45").getTime());
    }

    // ------------------------------------------------------------------------------------------------
    // C-069: set* resolves through the real rules, not the legacy table's phantom transitions
    // ------------------------------------------------------------------------------------------------

    @Test
    public void setResolvesThroughTheRealRulesNotTheLegacyTablesPhantomTransitions() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            // (a) the legacy table starts at 1900-01-01T00:00Z with the offset then in force and reports the PRESENT
            // raw offset before it - a phantom 30-minute gap in Windhoek (+01 -> +01:30), 5h43 in Shanghai
            final ZoneId windhoek = ZoneId.of("Africa/Windhoek");
            TimeZone.setDefault(TimeZone.getTimeZone(windhoek));
            final Date h = new Date(-2208988800000L); // 1900-01-01T00:00Z = 01:30 +01:30
            final ZonedDateTime hz = ZonedDateTime.ofInstant(h.toInstant(), windhoek);
            assertEquals(30, hz.getMinute());
            assertEquals(hz.withMinute(0).toInstant().toEpochMilli(), Dates.setMinutes(h, 0).getTime());
            assertEquals("1900-01-01 01:00:00", Dates.format(Dates.setMinutes(h, 0), Dates.LOCAL_DATE_TIME_FORMAT));
            final Date h30 = new Date(h.getTime() + 30 * 60_000L); // 02:00 +01:30
            assertEquals(ZonedDateTime.ofInstant(h30.toInstant(), windhoek).withHour(1).toInstant().toEpochMilli(), Dates.setHours(h30, 1).getTime());

            final ZoneId shanghai = ZoneId.of("Asia/Shanghai");
            TimeZone.setDefault(TimeZone.getTimeZone(shanghai));
            final Date sh = new Date(-2208988800000L + 30 * 60_000L); // 08:30 +08
            assertEquals(ZonedDateTime.ofInstant(sh.toInstant(), shanghai).withMinute(0).toInstant().toEpochMilli(), Dates.setMinutes(sh, 0).getTime());

            final ZoneId cambridgeBay = ZoneId.of("America/Cambridge_Bay");
            TimeZone.setDefault(TimeZone.getTimeZone(cambridgeBay));
            final Date cb = Date.from(Instant.parse("1901-12-31T19:00:00Z"));
            assertEquals(ZonedDateTime.ofInstant(cb.toInstant(), cambridgeBay).withYear(1899).toInstant().toEpochMilli(), Dates.setYears(cb, 1899).getTime());

            // (b) after 2100 the legacy table projects Gaza's rule a week away from java.time's in some years
            final ZoneId gaza = ZoneId.of("Asia/Gaza");
            TimeZone.setDefault(TimeZone.getTimeZone(gaza));
            final ZonedDateTime g1 = ZonedDateTime.of(2103, 3, 31, 5, 0, 0, 0, gaza);

            if (g1.withHour(2).getHour() == 2) {
                assertEquals(g1.withHour(2).toInstant().toEpochMilli(), Dates.setHours(Date.from(g1.toInstant()), 2).getTime());
            }

            final ZonedDateTime g2 = ZonedDateTime.of(2103, 3, 24, 1, 30, 0, 0, gaza);
            final Date g2Date = Date.from(g2.toInstant());

            if (g2.withHour(2).getHour() != 2) {
                // the real gap: the hour that was set cannot be kept
                assertThrows(IllegalArgumentException.class, () -> Dates.setHours(g2Date, 2));
            } else {
                assertEquals(g2.withHour(2).toInstant().toEpochMilli(), Dates.setHours(g2Date, 2).getTime());
            }

            final ZonedDateTime g3 = ZonedDateTime.of(2100, 1, 1, 2, 0, 0, 0, gaza);
            assertEquals(g3.withYear(1900).toInstant().toEpochMilli(), Dates.setYears(Date.from(g3.toInstant()), 1900).getTime());
            assertEquals(Dates.addYears(Date.from(g3.toInstant()), -200).getTime(), Dates.setYears(Date.from(g3.toInstant()), 1900).getTime());

            // the documented rules survive the new engine
            final ZoneId newYork = ZoneId.of("America/New_York");
            TimeZone.setDefault(TimeZone.getTimeZone(newYork));
            final Date beforeGap = Date.from(ZonedDateTime.of(2025, 3, 9, 1, 30, 0, 0, newYork).toInstant());
            assertThrows(IllegalArgumentException.class, () -> Dates.setHours(beforeGap, 2)); // 02:xx does not exist that day
            final Date mar8 = Date.from(ZonedDateTime.of(2025, 3, 8, 2, 30, 0, 0, newYork).toInstant());
            assertEquals(ZonedDateTime.of(2025, 3, 9, 3, 30, 0, 0, newYork).toInstant().toEpochMilli(), Dates.setDays(mar8, 9).getTime()); // forward, day intact
            final Date secondPass = Date.from(Instant.parse("2025-11-02T06:30:00Z")); // 01:30 EST, the second 01:30
            assertEquals(secondPass.getTime(), Dates.setMinutes(secondPass, 30).getTime());
            assertEquals(Instant.parse("2025-11-02T06:45:00Z").toEpochMilli(), Dates.setMinutes(secondPass, 45).getTime()); // keeps the source offset
            final Date jan31 = Date.from(ZonedDateTime.of(2025, 1, 31, 12, 0, 0, 0, newYork).toInstant());
            assertEquals(ZonedDateTime.of(2025, 2, 28, 12, 0, 0, 0, newYork).toInstant().toEpochMilli(), Dates.setMonths(jan31, Calendar.FEBRUARY).getTime());
            final Timestamp nanos = new Timestamp(jan31.getTime());
            nanos.setNanos(123_456_789);
            assertEquals(123_456_789, Dates.setHours(nanos, 15).getNanos());
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.setHours(beforeGap, 2));
            assertTrue(e.getMessage().contains("America/New_York"), e.getMessage());

            final ZoneId apia = ZoneId.of("Pacific/Apia");
            TimeZone.setDefault(TimeZone.getTimeZone(apia));
            final Date dec29 = Date.from(ZonedDateTime.of(2011, 12, 29, 12, 0, 0, 0, apia).toInstant());
            assertThrows(IllegalArgumentException.class, () -> Dates.setDays(dec29, 30)); // 2011-12-30 was skipped in Apia
            assertEquals(ZonedDateTime.of(2011, 12, 31, 12, 0, 0, 0, apia).toInstant().toEpochMilli(), Dates.setDays(dec29, 31).getTime());
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 4: C-092 zone spellings under the other zone letter, C-093 the single y letter, C-094 the
    //          .SSS offset constant's compact offset on Calendar targets, C-095 a compact offset before
    //          a bracketed zone, C-097 one name for the day-of-month field
    // ------------------------------------------------------------------------------------------------

    @Test
    public void zoneSpellingsUnderTheOtherZoneLetterAreReReadToo() {
        final TimeZone brisbane = TimeZone.getTimeZone("Australia/Brisbane");
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        final String zPattern = "yyyy-MM-dd HH:mm:ss Z";
        final String namePattern = "yyyy-MM-dd HH:mm:ss z";

        // a zone name, GMT+05:30, GMT and UTC under Z: SimpleDateFormat accepts them all
        final Calendar name = Dates.parseToCalendar("2025-01-15 10:30:45 PST", zPattern, brisbane);
        assertEquals(10, name.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, name.get(Calendar.DAY_OF_MONTH));
        assertEquals("America/Los_Angeles", name.getTimeZone().getID());
        final Calendar gmtOffset = Dates.parseToCalendar("2025-01-15 10:30:45 GMT+05:30", zPattern, tokyo);
        assertEquals(10, gmtOffset.get(Calendar.HOUR_OF_DAY));
        assertEquals("GMT+05:30", gmtOffset.getTimeZone().getID());
        assertEquals(10, Dates.parseToCalendar("2025-01-15 10:30:45 UTC", zPattern, tokyo).get(Calendar.HOUR_OF_DAY));
        assertEquals(10, Dates.parseToCalendar("2025-01-15 10:30:45 GMT", zPattern, tokyo).get(Calendar.HOUR_OF_DAY));
        assertEquals(10, Dates.parseToCalendar("2025-01-15 10:30:45 Pacific Standard Time", zPattern, brisbane).get(Calendar.HOUR_OF_DAY));
        assertEquals(10, Dates.parseToCalendar("2025-01-15 10:30:45 pst", "yyyy-MM-dd HH:mm:ss ZZZZ", brisbane).get(Calendar.HOUR_OF_DAY));

        // an RFC 822 offset under z
        final Calendar rfc = Dates.parseToCalendar("2025-01-15 10:30:45 -0800", namePattern, tokyo);
        assertEquals(10, rfc.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, rfc.get(Calendar.DAY_OF_MONTH));
        assertEquals(-8 * 3_600_000, rfc.getTimeZone().getOffset(rfc.getTimeInMillis()));
        final Calendar zero = Dates.parseToCalendar("2025-01-15 10:30:45 +0000", namePattern, tokyo);
        assertEquals(10, zero.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, zero.getTimeZone().getOffset(zero.getTimeInMillis()));
        assertEquals(10, Dates.parseToCalendar("2025-01-15 10:30:45 -0800", "yyyy-MM-dd HH:mm:ss zzzz", tokyo).get(Calendar.HOUR_OF_DAY));

        // NZDT: java.time's full-name trie holds the ID "NZ", which used to swallow the front of the name
        final Calendar nz = Dates.parseToCalendar("2025-01-15 10:30:45 NZDT", namePattern, TimeZone.getTimeZone("UTC"));
        assertEquals(10, nz.get(Calendar.HOUR_OF_DAY));
        assertEquals(13 * 3_600_000, nz.getTimeZone().getOffset(nz.getTimeInMillis()));

        // the XML target agrees
        final XMLGregorianCalendar xml = Dates.parseToXMLGregorianCalendar("2025-01-15 10:30:45 PST", zPattern, brisbane);
        assertEquals(10, xml.getHour());
        assertEquals(-480, xml.getTimezone());

        // a foreign fallback over every zone and both seasons: the wall clock always reads as the text spells it
        final long january = Instant.parse("2025-01-15T10:30:45Z").toEpochMilli();
        final long july = Instant.parse("2025-07-15T10:30:45Z").toEpochMilli();
        final List<String> wrong = new ArrayList<>();

        for (final String id : TimeZone.getAvailableIDs()) {
            final TimeZone zone = TimeZone.getTimeZone(id);

            for (final long instant : new long[] { january, july }) {
                for (final String[] pair : new String[][] { { namePattern, zPattern }, { zPattern, namePattern }, { "yyyy-MM-dd HH:mm:ss zzzz", zPattern } }) {
                    final String text = Dates.format(new Date(instant), pair[0], zone);
                    final Calendar back;

                    try {
                        back = Dates.parseToCalendar(text, pair[1], tokyo);
                    } catch (final IllegalArgumentException e) {
                        continue; // a spelling the legacy parser itself refuses under the other letter
                    }

                    final String wallClock = String.format("%04d-%02d-%02d %02d:%02d:%02d", back.get(Calendar.YEAR), back.get(Calendar.MONTH) + 1,
                            back.get(Calendar.DAY_OF_MONTH), back.get(Calendar.HOUR_OF_DAY), back.get(Calendar.MINUTE), back.get(Calendar.SECOND));

                    if (!text.startsWith(wallClock)) {
                        wrong.add(id + " '" + text + "' under '" + pair[1] + "' -> " + wallClock);
                    }
                }
            }
        }

        assertEquals(Collections.emptyList(), wrong);
    }

    @Test
    public void singleYLetterUsesTheTwoDigitYearWindowOnlyForTwoDigitText() {
        final TimeZone brisbane = TimeZone.getTimeZone("Australia/Brisbane");
        // July: Los Angeles is on daylight time, so the standard name reproduces in no region and the fixed
        // -08:00 the text implies is what an unverified region (which showed 11:30) must not replace
        final Calendar two = Dates.parseToCalendar("75-07-15 10:30:45 PST", "y-MM-dd HH:mm:ss z", brisbane);
        assertEquals(1975, two.get(Calendar.YEAR));
        assertEquals(10, two.get(Calendar.HOUR_OF_DAY));
        assertEquals(-8 * 3_600_000, two.getTimeZone().getOffset(two.getTimeInMillis()));
        assertEquals("America/Los_Angeles", Dates.parseToCalendar("75-01-15 10:30:45 PST", "y-MM-dd HH:mm:ss z", brisbane).getTimeZone().getID());
        final Calendar four = Dates.parseToCalendar("1875-07-15 10:30:45 PST", "y-MM-dd HH:mm:ss z", brisbane);
        assertEquals(1875, four.get(Calendar.YEAR));
        assertEquals(10, four.get(Calendar.HOUR_OF_DAY));
    }

    @Test
    public void explicitOffsetTimestampConstantKeepsACompactOffsetOnTheCalendarTargets() {
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        final String f = Dates.ISO_OFFSET_TIMESTAMP_FORMAT;

        for (final String text : new String[] { "2025-01-15T10:30:45.123+0530", "2025-01-15T10:30:45.123+05:30" }) {
            final Calendar c = Dates.parseToCalendar(text, f, tokyo);
            assertEquals(10, c.get(Calendar.HOUR_OF_DAY), text);
            assertEquals(330 * 60_000, c.getTimeZone().getOffset(c.getTimeInMillis()), text);
            assertEquals(Instant.parse("2025-01-15T05:00:45.123Z").toEpochMilli(), c.getTimeInMillis(), text);
            assertEquals(330, Dates.parseToXMLGregorianCalendar(text, f, tokyo).getTimezone(), text);
            assertEquals(10, Dates.parseToGregorianCalendar(text, f, tokyo).get(Calendar.HOUR_OF_DAY), text);
        }
    }

    @Test
    public void compactOffsetBeforeABracketedZoneIsAcceptedLikeTheUnbracketedOne() {
        final long expected = Instant.parse("2025-01-15T05:00:45Z").toEpochMilli();
        assertEquals(expected, Dates.parseToInstant("2025-01-15T10:30:45+0530[Asia/Kolkata]").toEpochMilli());
        assertEquals(expected, Dates.parseToJUDate("2025-01-15T10:30:45+0530[Asia/Kolkata]").getTime());
        assertEquals(expected, Dates.parseToCalendar("2025-01-15T10:30:45+0530[Asia/Kolkata]").getTimeInMillis());
        assertEquals(expected + 123, Dates.parseToTimestamp("2025-01-15T10:30:45.123+0530[Asia/Kolkata]").getTime());
        assertEquals(expected, Dates.parseToZonedDateTime("2025-01-15T10:30:45+0530[Asia/Kolkata]").toInstant().toEpochMilli());
        assertEquals("Asia/Kolkata", Dates.parseToCalendar("2025-01-15T10:30:45+0530[Asia/Kolkata]").getTimeZone().getID());
        // an offset the region does not have on that date is still rejected
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T10:30:45+0100[Asia/Kolkata]"));
    }

    @Test
    public void dayOfMonthRejectionsUseOneFieldName() {
        final TimeZone saved = TimeZone.getDefault();

        try {
            final ZoneId apia = ZoneId.of("Pacific/Apia");
            TimeZone.setDefault(TimeZone.getTimeZone(apia));
            final Date dec29 = Date.from(ZonedDateTime.of(2011, 12, 29, 12, 0, 0, 0, apia).toInstant());
            final IllegalArgumentException days = assertThrows(IllegalArgumentException.class, () -> Dates.setDays(dec29, 30));
            assertTrue(days.getMessage().contains("DAY_OF_MONTH: 30 -> "), days.getMessage());
            assertFalse(days.getMessage().contains("DATE:"), days.getMessage());
            final Date nov30 = Date.from(ZonedDateTime.of(2011, 11, 30, 12, 0, 0, 0, apia).toInstant());
            final IllegalArgumentException months = assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(nov30, Calendar.DECEMBER));
            assertTrue(months.getMessage().contains("DAY_OF_MONTH: 30 -> 31"), months.getMessage());
        } finally {
            TimeZone.setDefault(saved);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Cycle 5: C-100..C-103 - the offset SimpleDateFormat applied is the wall clock's, so a spelling the
    //          java.time re-read cannot follow (literal years under yy, locale digits, padded letters,
    //          GMT+5:30, week dates, a zone letter run into a digit field) still shows the text's fields
    // ------------------------------------------------------------------------------------------------

    private static void assertWallClock(final String expectedPrefix, final Calendar c) {
        final String wallClock = String.format("%04d-%02d-%02d %02d:%02d:%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
        assertEquals(expectedPrefix, wallClock);
    }

    @Test
    public void literalYearsUnderAShortYearLetterKeepTheWallClock() {
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");

        // SimpleDateFormat reads 4- and 5-digit text under yy literally; the re-read used to shift it a century
        for (final String[] textAndPattern : new String[][] { { "1920-07-15 10:30:45 EST", "yy-MM-dd HH:mm:ss z" },
                { "2050-07-15 10:30:45 EST", "yy-MM-dd HH:mm:ss z" }, { "12025-07-15 10:30:45 EST", "yy-MM-dd HH:mm:ss z" },
                { "5-07-15 10:30:45 EST", "y-MM-dd HH:mm:ss z" } }) {
            final Calendar c = Dates.parseToCalendar(textAndPattern[0], textAndPattern[1], tokyo);
            assertEquals(10, c.get(Calendar.HOUR_OF_DAY), textAndPattern[0]);
            assertEquals(30, c.get(Calendar.MINUTE), textAndPattern[0]);
            assertEquals(15, c.get(Calendar.DAY_OF_MONTH), textAndPattern[0]);
            assertEquals(-5 * 3_600_000, c.getTimeZone().getOffset(c.getTimeInMillis()), textAndPattern[0]);
            assertEquals(Dates.parseToJUDate(textAndPattern[0], textAndPattern[1], tokyo).getTime(), c.getTimeInMillis(), textAndPattern[0]);
        }

        assertEquals(1920, Dates.parseToCalendar("1920-07-15 10:30:45 EST", "yy-MM-dd HH:mm:ss z", tokyo).get(Calendar.YEAR));
    }

    @Test
    public void localeDigitsAndPaddedPatternLettersKeepTheWallClock() {
        final TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        final Locale arabicEgypt = Locale.forLanguageTag("ar-EG");
        final Date instant = new Date(Instant.parse("2025-07-15T15:30:45Z").toEpochMilli());

        // the class's own rendering in Arabic-Indic digits reads back with a foreign fallback
        final String arabic = Dates.format(instant, "yyyy-MM-dd HH:mm:ss z", newYork, arabicEgypt);
        final Calendar fromArabic = Dates.parseToCalendar(arabic, "yyyy-MM-dd HH:mm:ss z", tokyo, arabicEgypt);
        assertEquals(instant.getTime(), fromArabic.getTimeInMillis());
        assertWallClock("2025-07-15 11:30:45", fromArabic);
        final String arabicOffset = Dates.format(instant, "yyyy-MM-dd HH:mm:ss Z", newYork, arabicEgypt);
        assertWallClock("2025-07-15 11:30:45", Dates.parseToCalendar(arabicOffset, "yyyy-MM-dd HH:mm:ss Z", tokyo, arabicEgypt));

        // letter runs SimpleDateFormat pads but java.time rejects
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("15/07/2025 10:30:45 AM GMT-05:00", "dd/MM/yyyy hh:mm:ss aa z", tokyo));
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("15/07/2025 10:30:45 AM -0500", "dd/MM/yyyy hh:mm:ss aa Z", tokyo));
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("2025-07-15 010:30:45 -0500", "yyyy-MM-dd HHH:mm:ss Z", tokyo));
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("2025-07-015 10:30:45 -0500", "yyyy-MM-ddd HH:mm:ss Z", tokyo));
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("2025-July-15 10:30:45 -0500", "yyyy-MMMMMM-dd HH:mm:ss Z", tokyo));
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("2025-07-15 10:30:45.123 -0500", "yyyy-MM-dd HH:mm:ss.SSSSSSSSSS Z", tokyo));
        assertEquals(-5 * 3_600_000, Dates.parseToCalendar("15/07/2025 10:30:45 AM GMT-05:00", "dd/MM/yyyy hh:mm:ss aa z", tokyo).getTimeZone().getOffset(0));
    }

    @Test
    public void spellingsOnlySimpleDateFormatUnderstandsKeepTheWallClockOnAFixedOffset() {
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        final TimeZone brisbane = TimeZone.getTimeZone("Australia/Brisbane");

        // a one-digit GMT hour
        final Calendar oneDigit = Dates.parseToCalendar("2025-07-15 10:30:45 GMT+5:30", "yyyy-MM-dd HH:mm:ss z", tokyo);
        assertWallClock("2025-07-15 10:30:45", oneDigit);
        assertEquals(330 * 60_000, oneDigit.getTimeZone().getOffset(oneDigit.getTimeInMillis()));

        // a week-date pattern (no plain date for the re-read to check against)
        final Calendar week = Dates.parseToCalendar("2025-W29-2 10:30:45 EST", "YYYY-'W'ww-u HH:mm:ss z", tokyo);
        assertEquals(10, week.get(Calendar.HOUR_OF_DAY));
        assertEquals(-5 * 3_600_000, week.getTimeZone().getOffset(week.getTimeInMillis()));

        // SimpleDateFormat's own day-of-week letter, and extra whitespace
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("2025-07-15 2 10:30:45 PST", "yyyy-MM-dd u HH:mm:ss z", brisbane));
        assertWallClock("2025-07-15 10:30:45", Dates.parseToCalendar("2025-07-15  10:30:45 PST", "yyyy-MM-dd HH:mm:ss z", brisbane));

        // the XML target agrees
        assertEquals(10, Dates.parseToXMLGregorianCalendar("2025-07-15 10:30:45 GMT+5:30", "yyyy-MM-dd HH:mm:ss z", tokyo).getHour());
        assertEquals(330, Dates.parseToXMLGregorianCalendar("2025-07-15 10:30:45 GMT+5:30", "yyyy-MM-dd HH:mm:ss z", tokyo).getTimezone());

        // zone-less text keeps the fallback zone itself, as before
        assertEquals("Asia/Tokyo", Dates.parseToCalendar("2025-07-15 10:30:45", "yyyy-MM-dd HH:mm:ss", tokyo).getTimeZone().getID());
        assertEquals("Asia/Tokyo", Dates.parseToCalendar("2025-07-15 10:30:45 JST", "yyyy-MM-dd HH:mm:ss z", tokyo).getTimeZone().getID());
    }
}
