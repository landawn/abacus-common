package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Review pass 6 (2026-08-31b). Every civil-field operation now resolves an ambiguous or nonexistent wall
 * clock by one rule &mdash; the one {@code ZonedDateTime.ofLocal} uses and {@code truncate}/{@code round}/
 * {@code ceiling} already followed.
 *
 * <ul>
 *   <li><b>B1</b> &mdash; {@code set*} used to relocate a value sitting in the first pass of a
 *       daylight-saving overlap to the second pass, so {@code setMinutes(d, d's own minute)} moved the
 *       instant by an hour.</li>
 *   <li><b>B2</b> &mdash; {@code addMonths}/{@code addYears} resolved an ambiguous result to the
 *       standard-time offset while {@code addDays}/{@code addWeeks} kept the input's, and
 *       {@code addDays} resolved a spring-forward gap <i>backwards</i> (02:00 became 01:00) where
 *       {@code addMonths} resolved it forward.</li>
 *   <li><b>B3</b> &mdash; {@code setYears} wrote {@link Calendar#YEAR}, a year-of-era, while the rest of
 *       the class reads a proleptic ISO year.</li>
 *   <li><b>B4</b> &mdash; {@code format(Calendar)} threw {@code NullPointerException} for a calendar whose
 *       {@code getTimeZone()} returns {@code null}.</li>
 *   <li><b>D2</b> &mdash; {@code isSameLocalTime(Date, Date)} read its fields from
 *       {@code Calendar.getInstance()}, following the default locale's calendar system.</li>
 * </ul>
 */
public class DatesDstOverlapArithmeticTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** Zones with a fall-back overlap and a spring-forward gap, including 30- and 45-minute shifts. */
    private static final String[] DST_ZONES = { "America/New_York", "Europe/Berlin", "Australia/Lord_Howe", "Pacific/Chatham", "America/Sao_Paulo",
            "Europe/London" };

    private TimeZone originalTimeZone;
    private Locale originalLocale;

    @BeforeEach
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        originalLocale = Locale.getDefault();
    }

    @AfterEach
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
        Locale.setDefault(originalLocale);
    }

    // ==========================================================================================
    // B1 - set* keeps the offset the input was already on
    // ==========================================================================================

    /**
     * The headline case, in the zone and instant a reader can check by hand: 2025-11-02T01:30-04:00 is the
     * first pass of New York's autumn overlap. Every setter used to return 01:30<b>-05:00</b>, one hour
     * later, for a field it was not even changing.
     */
    @Test
    public void b1_setKeepsTheFirstPassOfAnOverlap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final long firstPass = Instant.parse("2025-11-02T05:30:00Z").toEpochMilli(); // 01:30 EDT
        final java.util.Date d = new java.util.Date(firstPass);

        assertEquals(firstPass, Dates.setYears(d, 2025).getTime());
        assertEquals(firstPass, Dates.setMonths(d, Calendar.NOVEMBER).getTime());
        assertEquals(firstPass, Dates.setDays(d, 2).getTime());
        assertEquals(firstPass, Dates.setHours(d, 1).getTime());
        assertEquals(firstPass, Dates.setMinutes(d, 30).getTime());
        assertEquals(firstPass, Dates.setSeconds(d, 0).getTime());
        assertEquals(firstPass, Dates.setMilliseconds(d, 0).getTime());

        // ... and it is the same answer java.time and this class's own truncate give.
        assertEquals(firstPass,
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(firstPass), ZoneId.of("America/New_York")).withMinute(30).toInstant().toEpochMilli());
        assertEquals(firstPass, Dates.truncate(d, Calendar.MINUTE).getTime());
    }

    /** The second pass is equally stable: a setter must not pull it back to the first. */
    @Test
    public void b1_setKeepsTheSecondPassOfAnOverlap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final long secondPass = Instant.parse("2025-11-02T06:30:00Z").toEpochMilli(); // 01:30 EST
        final java.util.Date d = new java.util.Date(secondPass);

        assertEquals(secondPass, Dates.setMinutes(d, 30).getTime());
        assertEquals(secondPass, Dates.setYears(d, 2025).getTime());
        assertEquals(secondPass, Dates.setHours(d, 1).getTime());
    }

    /**
     * The general invariant, over every overlap transition the runtime knows about in six zones:
     * {@code set*(d, d's own value for that field)} is the identity.
     */
    @Test
    public void b1_setIsAnIdentityAtEveryOverlapInEveryZone() {
        int checked = 0;

        for (final String zoneName : DST_ZONES) {
            final TimeZone tz = TimeZone.getTimeZone(zoneName);
            TimeZone.setDefault(tz);
            final ZoneId zone = tz.toZoneId();

            for (final long millis : overlapSamples(zone)) {
                final java.util.Date d = new java.util.Date(millis);
                final ZonedDateTime zdt = Instant.ofEpochMilli(millis).atZone(zone);
                final String where = zoneName + " @ " + zdt;

                assertEquals(millis, Dates.setYears(d, zdt.getYear()).getTime(), where);
                assertEquals(millis, Dates.setMonths(d, zdt.getMonthValue() - 1).getTime(), where);
                assertEquals(millis, Dates.setDays(d, zdt.getDayOfMonth()).getTime(), where);
                assertEquals(millis, Dates.setHours(d, zdt.getHour()).getTime(), where);
                assertEquals(millis, Dates.setMinutes(d, zdt.getMinute()).getTime(), where);
                assertEquals(millis, Dates.setSeconds(d, zdt.getSecond()).getTime(), where);
                assertEquals(millis, Dates.setMilliseconds(d, zdt.getNano() / 1_000_000).getTime(), where);
                checked++;
            }
        }

        assertTrue(checked > 100, "expected many overlap samples, got " + checked);
    }

    /** A field set to a genuinely different value still lands on the input's offset when that is valid. */
    @Test
    public void b1_setToADifferentValueAlsoKeepsTheInputOffset() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        // 2025-11-02T01:00-04:00 -> set minutes to 30 -> 01:30, still the first pass.
        final java.util.Date d = new java.util.Date(Instant.parse("2025-11-02T05:00:00Z").toEpochMilli());

        assertEquals(Instant.parse("2025-11-02T05:30:00Z").toEpochMilli(), Dates.setMinutes(d, 30).getTime());
        assertEquals("2025-11-02 01:30:00 EDT", Dates.format(Dates.setMinutes(d, 30), "yyyy-MM-dd HH:mm:ss zzz"));
    }

    /** A Timestamp's sub-millisecond fraction survives the new resolution path. */
    @Test
    public void b1_setPreservesSubMillisecondNanosAcrossAnOverlap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final java.sql.Timestamp ts = new java.sql.Timestamp(Instant.parse("2025-11-02T05:30:00Z").toEpochMilli());
        ts.setNanos(123_456_789);

        final java.sql.Timestamp result = Dates.setMinutes(ts, 30);
        assertEquals(ts.getTime(), result.getTime());
        assertEquals(123_456_789, result.getNanos());

        // setMilliseconds owns the whole fractional second, so it clears the sub-millisecond part.
        assertEquals(456_000_000, Dates.setMilliseconds(ts, 456).getNanos());
    }

    // ==========================================================================================
    // B1/J1 - gap behaviour is unchanged and now documented
    // ==========================================================================================

    /** A field the caller did <i>not</i> set is still resolved forward out of a gap. */
    @Test
    public void j1_aFieldTheCallerDidNotSetIsResolvedForwardOutOfAGap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo")); // local midnight does not exist on 2018-11-04

        final java.util.Date d = Dates.parseToJUDate("2018-11-03 00:30:00", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals("2018-11-04 01:30:00", Dates.format(Dates.setDays(d, 4), Dates.LOCAL_DATE_TIME_FORMAT));

        final java.util.Date octoberFourth = Dates.parseToJUDate("2018-10-04 00:30:00", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals("2018-11-04 01:30:00", Dates.format(Dates.setMonths(octoberFourth, Calendar.NOVEMBER), Dates.LOCAL_DATE_TIME_FORMAT));
    }

    /** The field the caller <i>did</i> set is honored exactly or rejected - never quietly moved. */
    @Test
    public void j1_theFieldBeingSetIsRejectedWhenTheZoneHasNoSuchWallClock() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York")); // 02:00-02:59 does not exist on 2025-03-09

        final java.util.Date d = Dates.parseToJUDate("2025-03-09 06:30:00", Dates.LOCAL_DATE_TIME_FORMAT);
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.setHours(d, 2));

        assertTrue(e.getMessage().contains("Dates.setHours(date, 2)"), e.getMessage());
        assertTrue(e.getMessage().contains("America/New_York"), e.getMessage());
        // The hour is in range for the field; only the zone makes it unusable, so the JDK diagnostic is kept.
        assertNotNull(e.getCause());
    }

    // ==========================================================================================
    // B2 - every add* field names the same instant for the same civil target
    // ==========================================================================================

    /**
     * The headline case: 2025-10-02T01:30-04:00 reaches 2025-11-02T01:30 either by 31 days or by one
     * month. {@code addMonths} used to land an hour later than {@code addDays}.
     */
    @Test
    public void b2_addDaysAndAddMonthsAgreeOnAnOverlap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final java.util.Date d = new java.util.Date(Instant.parse("2025-10-02T05:30:00Z").toEpochMilli()); // 01:30 EDT
        final long expected = Instant.parse("2025-11-02T05:30:00Z").toEpochMilli(); // 01:30 EDT, first pass

        assertEquals(expected, Dates.addDays(d, 31).getTime());
        assertEquals(expected, Dates.addWeeks(Dates.addDays(d, 3), 4).getTime());
        assertEquals(expected, Dates.addMonths(d, 1).getTime());
        assertEquals(expected, ZonedDateTime.ofInstant(d.toInstant(), ZoneId.of("America/New_York")).plusMonths(1).toInstant().toEpochMilli());
    }

    /**
     * {@code addDays} used to walk the wall clock <i>backwards</i> across a spring-forward gap:
     * 1930-04-26T02:00 plus one day returned 1930-04-27T01:00, an hour earlier in wall-clock terms and
     * only 23 elapsed hours later, while {@code addMonths} resolved the same gap forward.
     */
    @Test
    public void b2_addDaysResolvesASpringForwardGapForward() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final ZoneId zone = ZoneId.of("America/New_York");
        final ZonedDateTime source = ZonedDateTime.of(1930, 4, 26, 2, 0, 0, 0, zone);
        final java.util.Date d = new java.util.Date(source.toInstant().toEpochMilli());

        // 1930-04-27T02:00 does not exist; the gap ends at 03:00.
        assertTrue(zone.getRules().getValidOffsets(LocalDateTime.of(1930, 4, 27, 2, 0)).isEmpty());

        final long expected = source.plusDays(1).toInstant().toEpochMilli();
        assertEquals(expected, Dates.addDays(d, 1).getTime());
        assertEquals("1930-04-27 03:00:00", Dates.format(Dates.addDays(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals(expected, Dates.addWeeks(Dates.addDays(d, -6), 1).getTime());
    }

    /** Full parity with {@code ZonedDateTime.plusX} for all four calendar fields, around every transition. */
    @Test
    public void b2_addMatchesZonedDateTimeAtEveryTransition() {
        int checked = 0;

        for (final String zoneName : DST_ZONES) {
            final TimeZone tz = TimeZone.getTimeZone(zoneName);
            TimeZone.setDefault(tz);
            final ZoneId zone = tz.toZoneId();

            for (final long millis : transitionSamples(zone)) {
                final java.util.Date d = new java.util.Date(millis);
                final ZonedDateTime zdt = Instant.ofEpochMilli(millis).atZone(zone);
                final String where = zoneName + " @ " + zdt;

                for (final int amount : new int[] { 1, -1, 2, -2 }) {
                    assertEquals(zdt.plusDays(amount).toInstant().toEpochMilli(), Dates.addDays(d, amount).getTime(), where);
                    assertEquals(zdt.plusWeeks(amount).toInstant().toEpochMilli(), Dates.addWeeks(d, amount).getTime(), where);
                    assertEquals(zdt.plusMonths(amount).toInstant().toEpochMilli(), Dates.addMonths(d, amount).getTime(), where);
                    assertEquals(zdt.plusYears(amount).toInstant().toEpochMilli(), Dates.addYears(d, amount).getTime(), where);
                }

                checked++;
            }
        }

        assertTrue(checked > 500, "expected many transition samples, got " + checked);
    }

    /** The {@code Calendar} overloads resolve identically, in the calendar's own zone. */
    @Test
    public void b2_calendarOverloadsUseTheSameResolution() {
        TimeZone.setDefault(UTC); // the calendar's own zone must win, not the default

        final TimeZone newYork = TimeZone.getTimeZone("America/New_York");
        final Calendar cal = Calendar.getInstance(newYork);
        cal.setTimeInMillis(Instant.parse("2025-10-02T05:30:00Z").toEpochMilli());

        final long expected = Instant.parse("2025-11-02T05:30:00Z").toEpochMilli();
        assertEquals(expected, Dates.addDays(cal, 31).getTimeInMillis());
        assertEquals(expected, Dates.addMonths(cal, 1).getTimeInMillis());
        assertEquals(newYork.getID(), Dates.addMonths(cal, 1).getTimeZone().getID());
    }

    /** {@code addHours} and finer stay elapsed-time arithmetic on both overloads. */
    @Test
    public void b2_theTimeFieldsRemainElapsedArithmetic() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        // Spring forward: the civil day is 23 hours long, so 24 elapsed hours is a different wall clock.
        final java.util.Date d = Dates.parseToJUDate("2025-03-08 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals(d.getTime() + 24 * 3_600_000L, Dates.addHours(d, 24).getTime());
        assertEquals("2025-03-09 13:00:00", Dates.format(Dates.addHours(d, 24), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-03-09 12:00:00", Dates.format(Dates.addDays(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));

        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.setTime(d);
        assertEquals(d.getTime() + 24 * 3_600_000L, Dates.addHours(cal, 24).getTimeInMillis());
        assertEquals(d.getTime() + 90_000L, Dates.addSeconds(cal, 90).getTimeInMillis());
        assertEquals(d.getTime() + 7L, Dates.addMilliseconds(cal, 7).getTimeInMillis());
    }

    /** The overflow guard still reports the caller's own starting value. */
    @Test
    public void b2_overflowStillThrowsArithmeticExceptionNamingTheInput() {
        TimeZone.setDefault(UTC);

        final java.util.Date max = new java.util.Date(Long.MAX_VALUE);
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Dates.addYears(max, 1));
        assertTrue(e.getMessage().contains(String.valueOf(Long.MAX_VALUE)), e.getMessage());
        assertThrows(ArithmeticException.class, () -> Dates.addMonths(max, 1));
        assertThrows(ArithmeticException.class, () -> Dates.addDays(max, 1));
        assertThrows(ArithmeticException.class, () -> Dates.addWeeks(max, 1));
    }

    /** Ordinary arithmetic away from any transition is untouched, including month-end clamping. */
    @Test
    public void b2_ordinaryArithmeticIsUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final java.util.Date d = Dates.parseToJUDate("2024-01-31 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals("2024-02-29 10:30:45", Dates.format(Dates.addMonths(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-01-31 10:30:45", Dates.format(Dates.addYears(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2024-02-07 10:30:45", Dates.format(Dates.addWeeks(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2024-02-01 10:30:45", Dates.format(Dates.addDays(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));

        final java.util.Date leapDay = Dates.parseToJUDate("2024-02-29 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals("2025-02-28 10:30:45", Dates.format(Dates.addYears(leapDay, 1), Dates.LOCAL_DATE_TIME_FORMAT));
    }

    /** Field arithmetic stays proleptic Gregorian: the 1582 cutover must not reappear. */
    @Test
    public void b2_fieldArithmeticIsStillProlepticAcrossTheCutover() {
        TimeZone.setDefault(UTC);

        final java.util.Date d = Dates.parseToJUDate("1582-10-10 00:00:00", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        assertEquals("1583-10-10 00:00:00", Dates.format(Dates.addYears(d, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("1582-11-10 00:00:00", Dates.format(Dates.addMonths(d, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("1582-10-11 00:00:00", Dates.format(Dates.addDays(d, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("1582-10-17 00:00:00", Dates.format(Dates.addWeeks(d, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
    }

    /** A zone whose custom rules no {@link ZoneId} can express keeps the legacy engine end to end. */
    @Test
    public void b2_aZoneJavaTimeCannotExpressStillWorks() {
        // A SimpleTimeZone with hand-built daylight rules under an ID no registered ZoneId matches.
        final TimeZone custom = new java.util.SimpleTimeZone(-4 * 3_600_000, "Custom/Review6", Calendar.MARCH, 8, -Calendar.SUNDAY, 2 * 3_600_000,
                Calendar.NOVEMBER, 1, -Calendar.SUNDAY, 2 * 3_600_000, 3_600_000);
        TimeZone.setDefault(custom);

        // The precondition: this zone really has no ZoneId, so add* takes its legacy fallback. Without
        // this the test would keep passing while silently exercising the java.time path instead.
        final java.util.Date probe = new java.util.Date(1736937045123L);
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(probe, Calendar.DATE));

        final java.util.Date d = Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT);
        assertEquals("2026-01-15 10:30:45", Dates.format(Dates.addYears(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-02-15 10:30:45", Dates.format(Dates.addMonths(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-01-16 10:30:45", Dates.format(Dates.addDays(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-01-22 10:30:45", Dates.format(Dates.addWeeks(d, 1), Dates.LOCAL_DATE_TIME_FORMAT));

        // set* takes the same fallback, and stays an identity there.
        assertEquals(d.getTime(), Dates.setMinutes(d, 30).getTime());
    }

    // ==========================================================================================
    // B3 - setYears takes a proleptic ISO year
    // ==========================================================================================

    @Test
    public void b3_setYearsTakesAProlepticIsoYearOnBothSidesOfTheEra() {
        TimeZone.setDefault(UTC);

        final java.util.Date ce = Dates.parseToJUDate("2024-06-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        // Proleptic ISO year -500 is 501 BCE.
        final java.util.Date bce = new java.util.Date(LocalDateTime.of(-500, 6, 15, 10, 30, 45).toInstant(ZoneOffset.UTC).toEpochMilli());

        // A BCE value can now be moved into the Common Era; it used to become 2000 BCE.
        assertEquals("2000-06-15 AD", Dates.format(Dates.setYears(bce, 2000), "yyyy-MM-dd G", UTC));
        assertEquals("2024-06-15 AD", Dates.format(Dates.setYears(ce, 2024), "yyyy-MM-dd G", UTC));

        // 0 is 1 BCE, and negatives run further back.
        assertEquals("0001-06-15 BC", Dates.format(Dates.setYears(ce, 0), "yyyy-MM-dd G", UTC));
        assertEquals("0005-06-15 BC", Dates.format(Dates.setYears(ce, -4), "yyyy-MM-dd G", UTC));
        assertEquals("0501-06-15 BC", Dates.format(Dates.setYears(ce, -500), "yyyy-MM-dd G", UTC));

        // The amount means the same thing whichever era the input is in.
        assertEquals(Dates.setYears(ce, -4).getTime(), Dates.setYears(bce, -4).getTime());
    }

    /** {@code setYears} is now on the same year scale as everything else that reads a year. */
    @Test
    public void b3_setYearsAgreesWithTheYearTheClassPrintsAndAdds() {
        TimeZone.setDefault(UTC);

        final java.util.Date bce = new java.util.Date(LocalDateTime.of(-500, 6, 15, 10, 30, 45).toInstant(ZoneOffset.UTC).toEpochMilli());

        // setYears(x, y) and the java.time year of the result agree, on both sides of the era boundary.
        for (final int year : new int[] { -500, -4, 0, 1, 5, 1582, 2024 }) {
            final java.util.Date result = Dates.setYears(bce, year);
            assertEquals(year, Instant.ofEpochMilli(result.getTime()).atZone(ZoneOffset.UTC).getYear(), "setYears(bce, " + year + ")");
        }

        // add* already crossed the era correctly; the two now describe the same scale.
        assertEquals(Dates.setYears(bce, 1).getTime(), Dates.addYears(bce, 501).getTime());
    }

    /** Day-of-month clamping keeps working on both sides of the era, including proleptic leap years. */
    @Test
    public void b3_setYearsStillClampsTheDayOfMonth() {
        TimeZone.setDefault(UTC);

        final java.util.Date leapDay = Dates.parseToJUDate("2024-02-29 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        assertEquals("2025-02-28 12:00:00", Dates.format(Dates.setYears(leapDay, 2025), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("2028-02-29 12:00:00", Dates.format(Dates.setYears(leapDay, 2028), Dates.LOCAL_DATE_TIME_FORMAT, UTC));

        // Proleptic year -4 is a leap year; -3 is not.
        assertEquals("0005-02-29 BC", Dates.format(Dates.setYears(leapDay, -4), "yyyy-MM-dd G", UTC));
        assertEquals("0004-02-28 BC", Dates.format(Dates.setYears(leapDay, -3), "yyyy-MM-dd G", UTC));
    }

    /** The one year value {@code setYears} cannot express, because {@code 1 - year} overflows an int. */
    @Test
    public void b3_setYearsRejectsTheYearItCannotExpress() {
        TimeZone.setDefault(UTC);

        final java.util.Date d = Dates.parseToJUDate("2024-06-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.setYears(d, Integer.MIN_VALUE));

        assertTrue(e.getMessage().contains("Dates.setYears(date, -2147483648)"), e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(d, Integer.MAX_VALUE));
    }

    /** {@code setMonths}/{@code setDays} are era-neutral and unaffected. */
    @Test
    public void b3_theOtherSettersAreUnaffectedByTheEra() {
        TimeZone.setDefault(UTC);

        final java.util.Date bce = new java.util.Date(LocalDateTime.of(-500, 6, 15, 10, 30, 45).toInstant(ZoneOffset.UTC).toEpochMilli());

        assertEquals("0501-01-15 BC", Dates.format(Dates.setMonths(bce, Calendar.JANUARY), "yyyy-MM-dd G", UTC));
        assertEquals("0501-06-01 BC", Dates.format(Dates.setDays(bce, 1), "yyyy-MM-dd G", UTC));
        assertEquals(-500, Instant.ofEpochMilli(Dates.setHours(bce, 3).getTime()).atZone(ZoneOffset.UTC).getYear());
    }

    // ==========================================================================================
    // B4 - format(Calendar) and a null calendar zone
    // ==========================================================================================

    /**
     * A {@code Calendar} whose {@code getTimeZone()} returns {@code null} used to make the default-format
     * {@code format(Calendar)} throw {@code NullPointerException}, while every sibling operation on the
     * same value already fell back to the default zone or reported an {@code IllegalArgumentException}.
     */
    @Test
    public void b4_formatCalendarFallsBackToTheDefaultZoneForANullCalendarZone() {
        TimeZone.setDefault(UTC);

        final Calendar nullZone = new NullZoneCalendar();
        nullZone.setTimeInMillis(1736937045123L);

        assertEquals("2025-01-15T10:30:45.123Z[UTC]", Dates.format(nullZone));

        final StringBuilder sb = new StringBuilder();
        Dates.formatTo(nullZone, sb);
        assertEquals("2025-01-15T10:30:45.123Z[UTC]", sb.toString());

        // An explicit format already fell back to the default zone; that is unchanged.
        assertEquals("2025-01-15 10:30:45", Dates.format(nullZone, Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-01-15 10:30:45", Dates.format(nullZone, Dates.LOCAL_DATE_TIME_FORMAT, UTC));
    }

    /** The fallback follows the live default zone, exactly like the explicit-format path. */
    @Test
    public void b4_theFallbackFollowsTheLiveDefaultZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

        final Calendar nullZone = new NullZoneCalendar();
        nullZone.setTimeInMillis(1736937045123L);

        assertEquals("2025-01-15T16:00:45.123+05:30[Asia/Kolkata]", Dates.format(nullZone));
    }

    private static final class NullZoneCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        @Override
        public TimeZone getTimeZone() {
            return null;
        }
    }

    // ==========================================================================================
    // D2 - isSameLocalTime reads proleptic Gregorian fields
    // ==========================================================================================

    /** The two instants of a fall-back overlap show the same wall clock; that is the method's whole point. */
    @Test
    public void d2_isSameLocalTimeStillMatchesTheTwoPassesOfAnOverlap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final java.util.Date first = new java.util.Date(Instant.parse("2025-11-02T05:30:00Z").toEpochMilli());
        final java.util.Date second = new java.util.Date(Instant.parse("2025-11-02T06:30:00Z").toEpochMilli());

        assertTrue(Dates.isSameLocalTime(first, second));
        assertFalse(Dates.isSameLocalTime(first, new java.util.Date(first.getTime() + 1)));
    }

    /** The answer no longer depends on the default locale's calendar system. */
    @Test
    public void d2_isSameLocalTimeIgnoresTheDefaultLocalesCalendarSystem() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final java.util.Date first = new java.util.Date(Instant.parse("2025-11-02T05:30:00Z").toEpochMilli());
        final java.util.Date second = new java.util.Date(Instant.parse("2025-11-02T06:30:00Z").toEpochMilli());
        final java.util.Date other = new java.util.Date(Instant.parse("2025-11-03T06:30:00Z").toEpochMilli());

        for (final String tag : new String[] { "en-US", "th-TH-u-ca-buddhist", "ja-JP-u-ca-japanese" }) {
            Locale.setDefault(Locale.forLanguageTag(tag));
            assertTrue(Dates.isSameLocalTime(first, second), tag);
            assertFalse(Dates.isSameLocalTime(first, other), tag);
        }
    }

    /** Pre-1582 values follow the proleptic calendar, matching what {@code format} prints. */
    @Test
    public void d2_isSameLocalTimeIsProlepticBeforeTheCutover() {
        TimeZone.setDefault(UTC);

        final java.util.Date d = Dates.parseToJUDate("1500-03-01 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        assertTrue(Dates.isSameLocalTime(d, new java.util.Date(d.getTime())));
        assertFalse(Dates.isSameLocalTime(d, Dates.addDays(d, 1)));
        // Same wall clock exactly one (non-leap) year later is a different day-of-year, so not "same local time".
        assertFalse(Dates.isSameLocalTime(d, Dates.addYears(d, 1)));
    }

    // ==========================================================================================
    // D1 - the Calendar parse family is no longer @Beta
    // ==========================================================================================

    /** {@code format} and its documented inverse now carry the same stability marker (none). */
    @Test
    public void d1_theCalendarParseFamilyIsNoLongerMarkedBeta() {
        final List<String> family = List.of("parseToCalendar", "parseToGregorianCalendar", "parseToXMLGregorianCalendar");
        int found = 0;

        for (final java.lang.reflect.Method m : Dates.class.getDeclaredMethods()) {
            if (family.contains(m.getName())) {
                found++;
                assertFalse(m.isAnnotationPresent(com.landawn.abacus.annotation.Beta.class), m.toString());
            }
        }

        assertEquals(12, found, "expected the four overloads of each of the three parse methods");
    }

    // ==========================================================================================
    // helpers
    // ==========================================================================================

    /** One instant inside the first pass of every fall-back overlap the zone has since 1930. */
    private static List<Long> overlapSamples(final ZoneId zone) {
        final List<Long> samples = new ArrayList<>();
        final ZoneRules rules = zone.getRules();
        Instant cursor = Instant.parse("1930-01-01T00:00:00Z");

        for (int i = 0; i < 200; i++) {
            final ZoneOffsetTransition transition = rules.nextTransition(cursor);

            if (transition == null) {
                break;
            }

            cursor = transition.getInstant().plusSeconds(1);

            if (transition.isOverlap()) {
                samples.add(transition.getInstant().toEpochMilli() - 1);
                samples.add(transition.getInstant().toEpochMilli() - 60_000);
            }
        }

        return samples;
    }

    /** Instants on both sides of every transition the zone has since 1930, plus a day out on each side. */
    private static List<Long> transitionSamples(final ZoneId zone) {
        final List<Long> samples = new ArrayList<>();
        final ZoneRules rules = zone.getRules();
        Instant cursor = Instant.parse("1930-01-01T00:00:00Z");

        for (int i = 0; i < 120; i++) {
            final ZoneOffsetTransition transition = rules.nextTransition(cursor);

            if (transition == null) {
                break;
            }

            cursor = transition.getInstant().plusSeconds(1);
            final long millis = transition.getInstant().toEpochMilli();

            for (final long offset : new long[] { -1L, -1_800_000L, 1L, 1_800_000L, -86_400_000L, 86_400_000L }) {
                samples.add(millis + offset);
            }
        }

        return samples;
    }

    // ==========================================================================================
    // The Calendar zone is read once per operation, so an inconsistent calendar still falls back
    // ==========================================================================================

    /**
     * {@code addToCalendar} and {@code copyCalendarSettings} each read {@code getTimeZone()} twice - a
     * null test followed by a second call - so a calendar answering differently the second time threw
     * {@code NullPointerException} instead of honouring the documented live-default-zone fallback.
     */
    @Test
    public void testTheCalendarZoneIsReadOncePerOperation() {
        final long millis = 1736937045000L;
        final Calendar honest = new GregorianCalendar(TimeZone.getDefault());
        honest.setTimeInMillis(millis);

        assertEquals(Dates.addDays(honest, 1).getTimeInMillis(), Dates.addDays(flakyZoneCalendar(millis), 1).getTimeInMillis());
        assertEquals(Dates.addMonths(honest, 1).getTimeInMillis(), Dates.addMonths(flakyZoneCalendar(millis), 1).getTimeInMillis());
        assertEquals(Dates.addHours(honest, 1).getTimeInMillis(), Dates.addHours(flakyZoneCalendar(millis), 1).getTimeInMillis());
        assertEquals(Dates.addMilliseconds(honest, 1).getTimeInMillis(), Dates.addMilliseconds(flakyZoneCalendar(millis), 1).getTimeInMillis());
        assertEquals(Dates.truncate(honest, Calendar.DATE).getTimeInMillis(), Dates.truncate(flakyZoneCalendar(millis), Calendar.DATE).getTimeInMillis());
        assertEquals(Dates.ceiling(honest, Calendar.DATE).getTimeInMillis(), Dates.ceiling(flakyZoneCalendar(millis), Calendar.DATE).getTimeInMillis());

        // The always-null-zone calendar the class contract names keeps working, as it already did.
        final Calendar nullZone = new NullZoneCalendar();
        nullZone.setTimeInMillis(millis);

        assertEquals(Dates.addDays(honest, 1).getTimeInMillis(), Dates.addDays(nullZone, 1).getTimeInMillis());
        assertEquals(Dates.addHours(honest, 1).getTimeInMillis(), Dates.addHours(nullZone, 1).getTimeInMillis());
    }

    private static Calendar flakyZoneCalendar(final long millis) {
        final Calendar calendar = new FlakyZoneCalendar();
        calendar.setTimeInMillis(millis);

        return calendar;
    }

    private static final class FlakyZoneCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        private transient int zoneReads;

        @Override
        public TimeZone getTimeZone() {
            // The live default zone once, then null: the shape a read-the-zone-twice caller turns into an NPE.
            return ++zoneReads == 1 ? TimeZone.getDefault() : null;
        }
    }

    // ==========================================================================================
    // addDays across a daylight-saving transition differs from 24 hours by the size of the shift
    // ==========================================================================================

    /** Australia/Lord_Howe moves 30 minutes, so the epoch difference is 23.5 or 24.5 hours, not 23 or 25. */
    @Test
    public void testAddDaysDiffersFromTwentyFourHoursByTheSizeOfTheShift() {
        final ZoneId lordHowe = ZoneId.of("Australia/Lord_Howe");
        final ZoneOffsetTransition springForward = lordHowe.getRules().nextTransition(Instant.parse("2024-06-01T00:00:00Z"));

        assertNotNull(springForward);
        assertTrue(springForward.isGap());
        assertEquals(1_800_000L, springForward.getDuration().toMillis());
        assertEquals(86_400_000L - springForward.getDuration().toMillis(), addDaysElapsedMillis(lordHowe, noonTheDayBefore(springForward)));
        assertEquals(84_600_000L, addDaysElapsedMillis(lordHowe, noonTheDayBefore(springForward)));

        final ZoneOffsetTransition fallBack = lordHowe.getRules().nextTransition(springForward.getInstant());

        assertTrue(fallBack.isOverlap());
        assertEquals(-1_800_000L, fallBack.getDuration().toMillis());
        assertEquals(86_400_000L - fallBack.getDuration().toMillis(), addDaysElapsedMillis(lordHowe, noonTheDayBefore(fallBack)));
        assertEquals(88_200_000L, addDaysElapsedMillis(lordHowe, noonTheDayBefore(fallBack)));

        // The usual one-hour move still gives 23 or 25 hours.
        final ZoneId newYork = ZoneId.of("America/New_York");
        final ZoneOffsetTransition newYorkFallBack = newYork.getRules().nextTransition(Instant.parse("2024-06-01T00:00:00Z"));

        assertEquals(-3_600_000L, newYorkFallBack.getDuration().toMillis());
        assertEquals(86_400_000L - newYorkFallBack.getDuration().toMillis(), addDaysElapsedMillis(newYork, noonTheDayBefore(newYorkFallBack)));
        assertEquals(90_000_000L, addDaysElapsedMillis(newYork, noonTheDayBefore(newYorkFallBack)));
    }

    /** Why the javadoc hedges: a result the gap removes is pushed forward by the gap, back to exactly 24 hours. */
    @Test
    public void testAddDaysLandingInAGapStillMeasuresTwentyFourHours() {
        final ZoneId lordHowe = ZoneId.of("Australia/Lord_Howe");
        final ZoneOffsetTransition springForward = lordHowe.getRules().nextTransition(Instant.parse("2024-06-01T00:00:00Z"));

        assertTrue(springForward.isGap());
        assertEquals(86_400_000L, addDaysElapsedMillis(lordHowe, springForward.getDateTimeBefore().plusMinutes(15).minusDays(1)));
    }

    private static LocalDateTime noonTheDayBefore(final ZoneOffsetTransition transition) {
        return transition.getDateTimeBefore().toLocalDate().minusDays(1).atTime(12, 0);
    }

    private static long addDaysElapsedMillis(final ZoneId zone, final LocalDateTime localStart) {
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone(zone));
        calendar.setTimeInMillis(localStart.atZone(zone).toInstant().toEpochMilli());

        return Dates.addDays(calendar, 1).getTimeInMillis() - calendar.getTimeInMillis();
    }
}
