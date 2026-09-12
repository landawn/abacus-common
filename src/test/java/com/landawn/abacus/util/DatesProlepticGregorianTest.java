package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dates.DTF;

import testfixtures.dates.CreatorTypes.ParentCalendar;

/**
 * Regression tests for the defects found in the 2026-08-30 line-by-line review of {@link Dates}.
 *
 * <p>The theme of B1-B3 is one invariant: every operation in {@code Dates} reads and writes civil
 * fields on a <i>proleptic</i> Gregorian calendar, so no operation may disagree with the date
 * {@link Dates#format(java.util.Date)} prints for the very same instant. Field arithmetic used to read
 * pre-1582 instants through the legacy Julian/Gregorian cutover and land ten days away.</p>
 */
public class DatesProlepticGregorianTest extends TestBase {

    /** 1582-10-10 in the proleptic Gregorian calendar; 1582-09-30 under the legacy 1582 cutover. */
    private static final long PRE_CUTOVER_MILLIS = LocalDate.of(1582, 10, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final TimeZone LOS_ANGELES = TimeZone.getTimeZone("America/Los_Angeles");

    private static final TimeZone KOLKATA = TimeZone.getTimeZone("Asia/Kolkata");

    private Locale priorLocale;

    private TimeZone priorTimeZone;

    @BeforeEach
    public void captureDefaults() {
        priorLocale = Locale.getDefault();
        priorTimeZone = TimeZone.getDefault();
        // The Date overloads of set*/add*/getFragment*/round evaluate in the live default zone.
        TimeZone.setDefault(UTC);
    }

    @AfterEach
    public void restoreDefaults() {
        Locale.setDefault(priorLocale);
        TimeZone.setDefault(priorTimeZone);
    }

    private static Date preCutover() {
        return new Date(PRE_CUTOVER_MILLIS);
    }

    // ------------------------------------------------------------------------------------------------
    // B1: the set* family reads and writes proleptic civil fields.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void setYearsMonthsDays_beforeTheGregorianCutover_agreeWithFormat() {
        final Date value = preCutover();
        assertEquals("1582-10-10T00:00:00Z", Dates.format(value));

        // Read through the legacy cutover these were 1582-09-21, 1582-02-09 and 1583-09-30.
        assertEquals("1582-10-11T00:00:00Z", Dates.format(Dates.setDays(value, 11)));
        assertEquals("1582-01-10T00:00:00Z", Dates.format(Dates.setMonths(value, Calendar.JANUARY)));
        assertEquals("1583-10-10T00:00:00Z", Dates.format(Dates.setYears(value, 1583)));

        assertEquals("1582-10-10T00:00:00Z", Dates.format(value), "the input must not be mutated");
    }

    @Test
    public void setTimeOfDayFields_beforeTheCutover_keepTheCivilDate() {
        // These round-tripped correctly even through the hybrid calendar, because they never touch a
        // date field. Pinned so the shared proleptic helper cannot regress them.
        final Date value = preCutover();

        assertEquals("1582-10-10T14:00:00Z", Dates.format(Dates.setHours(value, 14)));
        assertEquals("1582-10-10T00:45:00Z", Dates.format(Dates.setMinutes(value, 45)));
        assertEquals("1582-10-10T00:00:30Z", Dates.format(Dates.setSeconds(value, 30)));
        // Asserted on the civil rendering: the epoch value is negative here, so a naive
        // getTime() % 1000 would carry the dividend's sign rather than the millisecond field.
        assertEquals("1582-10-10 00:00:00.500", Dates.format(Dates.setMilliseconds(value, 500), Dates.LOCAL_TIMESTAMP_FORMAT, UTC));
    }

    @Test
    public void setYearsAndMonths_afterTheCutover_stillClampTheDayOfMonth() {
        final Date leapDay = Dates.parseToJUDate("2024-02-29 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        assertEquals("2025-02-28 10:30:45", Dates.format(Dates.setYears(leapDay, 2025), Dates.LOCAL_DATE_TIME_FORMAT, UTC));

        final Date endOfJanuary = Dates.parseToJUDate("2024-01-31 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        assertEquals("2024-02-29 10:30:45", Dates.format(Dates.setMonths(endOfJanuary, Calendar.FEBRUARY), Dates.LOCAL_DATE_TIME_FORMAT, UTC));

        // An explicit out-of-range day is still rejected rather than clamped.
        assertThrows(IllegalArgumentException.class, () -> Dates.setDays(endOfJanuary, 32));
        // 0 is the proleptic ISO year 1 BCE, not an out-of-range year-of-era.
        assertEquals("0001-01-31 10:30:45 BC", Dates.format(Dates.setYears(endOfJanuary, 0), "yyyy-MM-dd HH:mm:ss G", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.setYears(endOfJanuary, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(endOfJanuary, 12));
    }

    @Test
    public void set_underANonGregorianDefaultLocale_stillReadsGregorianFields() {
        Locale.setDefault(Locale.forLanguageTag("th-TH-u-ca-buddhist"));

        final Date value = Dates.parseToJUDate("2024-11-24 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        assertEquals("2025-11-24 10:30:45", Dates.format(Dates.setYears(value, 2025), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
    }

    // ------------------------------------------------------------------------------------------------
    // B2: Date-side calendar arithmetic is proleptic too.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void addYearsAndMonths_beforeTheGregorianCutover_agreeWithFormat() {
        final Date value = preCutover();

        // Read through the legacy cutover these were 1583-09-30 and 1582-10-30.
        assertEquals("1583-10-10T00:00:00Z", Dates.format(Dates.addYears(value, 1)));
        assertEquals("1582-11-10T00:00:00Z", Dates.format(Dates.addMonths(value, 1)));
        assertEquals("1581-10-10T00:00:00Z", Dates.format(Dates.addYears(value, -1)));
        assertEquals("1582-09-10T00:00:00Z", Dates.format(Dates.addMonths(value, -1)));

        assertEquals("1582-10-10T00:00:00Z", Dates.format(value), "the input must not be mutated");
    }

    @Test
    public void addDaysAndWeeks_acrossTheCutover_remainPlainTimelineArithmetic() {
        // Day and week addition was always correct: Calendar.add advances the timeline for those fields,
        // which the cutover cannot affect. Pinned as a no-change guard.
        final Date value = preCutover();

        assertEquals("1582-10-11T00:00:00Z", Dates.format(Dates.addDays(value, 1)));
        assertEquals("1582-10-20T00:00:00Z", Dates.format(Dates.addDays(value, 10)));
        assertEquals("1582-09-30T00:00:00Z", Dates.format(Dates.addDays(value, -10)));
        assertEquals("1582-10-17T00:00:00Z", Dates.format(Dates.addWeeks(value, 1)));
    }

    @Test
    public void addDays_stillPreservesWallClockTimeAcrossADstTransition() {
        // The daylight-saving-aware contract of addDays must survive the calendar swap.
        TimeZone.setDefault(LOS_ANGELES);

        final Date beforeSpringForward = Dates.parseToJUDate("2025-03-08 10:30:00", Dates.LOCAL_DATE_TIME_FORMAT);
        final Date nextDay = Dates.addDays(beforeSpringForward, 1);

        assertEquals("2025-03-09 10:30:00", Dates.format(nextDay, Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals(23L * 60 * 60 * 1000, nextDay.getTime() - beforeSpringForward.getTime(), "a spring-forward day is 23 elapsed hours");
    }

    @Test
    public void addYears_underANonGregorianDefaultLocale_producesGregorianResults() {
        Locale.setDefault(Locale.forLanguageTag("ja-JP-u-ca-japanese"));

        final Date value = Dates.parseToJUDate("1988-05-15 10:00:00", Dates.LOCAL_DATE_TIME_FORMAT, UTC);
        assertEquals("1993-05-15 10:00:00", Dates.format(Dates.addYears(value, 5), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("2028-05-15 10:00:00", Dates.format(Dates.addYears(value, 40), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("1989-05-15 10:00:00", Dates.format(Dates.addMonths(value, 12), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
    }

    @Test
    public void addYears_preservesTheSubMillisecondFractionOfATimestamp() {
        final Timestamp value = new Timestamp(PRE_CUTOVER_MILLIS);
        value.setNanos(123_456_789);

        final Timestamp result = Dates.addYears(value, 1);

        assertEquals(123_456_789, result.getNanos());
        assertEquals("1583-10-10T00:00:00Z", Dates.format(new Date(result.getTime() - result.getNanos() / 1_000_000)));
    }

    // ------------------------------------------------------------------------------------------------
    // B3: getFragmentIn*(Date, ...) is proleptic, as its contract promises.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void getFragmentInDays_beforeTheCutover_matchesTheProlepticDayOfYear() {
        final LocalDate civil = LocalDate.of(1000, 3, 1);
        final Date value = new Date(civil.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());

        assertEquals("1000-03-01T00:00:00Z", Dates.format(value));
        // The legacy cutover reported 55 here, five short of the date format() prints.
        assertEquals(civil.getDayOfYear(), Dates.getFragmentInDays(value, CalendarField.YEAR));
        assertEquals(60, Dates.getFragmentInDays(value, CalendarField.YEAR));
        assertEquals(civil.getDayOfMonth(), Dates.getFragmentInDays(value, CalendarField.MONTH));
    }

    @Test
    public void getFragmentIn_dateOverloads_underANonGregorianDefaultLocale_stayGregorian() {
        Locale.setDefault(Locale.forLanguageTag("th-TH-u-ca-buddhist"));

        final Date value = Dates.parseToJUDate("2023-02-28 07:15:10.538", Dates.LOCAL_TIMESTAMP_FORMAT, UTC);

        assertEquals(59, Dates.getFragmentInDays(value, CalendarField.YEAR));
        assertEquals(538, Dates.getFragmentInMilliseconds(value, CalendarField.SECOND));
        assertEquals(0, Dates.getFragmentInDays(value, CalendarField.DAY_OF_MONTH));
    }

    @Test
    public void getFragmentIn_calendarOverloads_stillFollowTheCallersOwnCalendar() {
        // Unchanged by the fix: the Calendar overloads deliberately read the caller's calendar system.
        final Calendar calendar = Dates.createCalendar(0L, UTC);

        assertEquals(1, Dates.getFragmentInDays(calendar, CalendarField.YEAR));
        assertEquals(0, Dates.getFragmentInMilliseconds(calendar, CalendarField.SECOND));
        assertThrows(IllegalArgumentException.class, () -> Dates.getFragmentInDays(calendar, CalendarField.WEEK_OF_YEAR));
    }

    // ------------------------------------------------------------------------------------------------
    // B4: round/truncate/ceiling(Calendar, ...) honor the published subtype-preserving contract.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void roundTruncateCeiling_onACalendar_useTheRegisteredCreator() {
        final AtomicInteger creatorCalls = new AtomicInteger();
        assertTrue(Dates.registerCalendarCreator(ParentCalendar.class, (millis, template) -> {
            creatorCalls.incrementAndGet();
            return new ParentCalendar(millis);
        }));

        try {
            final ParentCalendar source = new ParentCalendar(1736937045123L);
            source.setTimeZone(UTC);

            // Previously all three cloned directly and never consulted the creator.
            assertEquals(ParentCalendar.class, Dates.truncate(source, CalendarField.DAY_OF_MONTH).getClass());
            assertEquals(1, creatorCalls.get());

            assertEquals(ParentCalendar.class, Dates.round(source, CalendarField.HOUR_OF_DAY).getClass());
            assertEquals(2, creatorCalls.get());

            assertEquals(ParentCalendar.class, Dates.ceiling(source, CalendarField.HOUR_OF_DAY).getClass());
            assertEquals(3, creatorCalls.get());

            // A no-op field must still go through the same path.
            assertEquals(ParentCalendar.class, Dates.truncate(source, CalendarField.MILLISECOND).getClass());
            assertEquals(4, creatorCalls.get());
        } finally {
            assertTrue(Dates.unregisterCalendarCreator(ParentCalendar.class));
        }
    }

    @Test
    public void roundTruncateCeiling_onACalendar_rejectABrokenCreator() {
        assertTrue(Dates.registerCalendarCreator(ParentCalendar.class, (millis, template) -> new ParentCalendar(0L)));

        try {
            final ParentCalendar source = new ParentCalendar(1736937045123L);
            source.setTimeZone(UTC);

            assertThrows(IllegalStateException.class, () -> Dates.truncate(source, CalendarField.DAY_OF_MONTH));
            assertThrows(IllegalStateException.class, () -> Dates.round(source, CalendarField.HOUR_OF_DAY));
            assertThrows(IllegalStateException.class, () -> Dates.ceiling(source, CalendarField.HOUR_OF_DAY));
        } finally {
            assertTrue(Dates.unregisterCalendarCreator(ParentCalendar.class));
        }
    }

    @Test
    public void truncate_onACalendar_preservesZoneLeniencyWeekRulesAndCutover_withoutMutatingTheSource() {
        final GregorianCalendar source = new GregorianCalendar(KOLKATA, Locale.FRANCE);
        source.setGregorianChange(new Date(Long.MIN_VALUE));
        source.setLenient(false);
        source.setTimeInMillis(1736937045123L);

        final GregorianCalendar result = Dates.truncate(source, CalendarField.DAY_OF_MONTH);

        assertNotSame(source, result);
        assertEquals(1736937045123L, source.getTimeInMillis(), "the source must not be mutated");
        assertEquals(KOLKATA.getID(), result.getTimeZone().getID());
        assertFalse(result.isLenient());
        assertEquals(source.getFirstDayOfWeek(), result.getFirstDayOfWeek());
        assertEquals(source.getMinimalDaysInFirstWeek(), result.getMinimalDaysInFirstWeek());
        assertEquals(Long.MIN_VALUE, result.getGregorianChange().getTime());
        assertEquals("2025-01-15 00:00:00", Dates.format(result, Dates.LOCAL_DATE_TIME_FORMAT, KOLKATA));
    }

    @Test
    public void roundTruncateCeiling_stillSupportTheJdkAlternateCalendars() {
        // These have neither a registered creator nor an invocable constructor, so they reach the
        // clone() fallback. Nothing may regress that path.
        for (final String tag : new String[] { "th-TH-u-ca-buddhist", "ja-JP-u-ca-japanese" }) {
            Locale.setDefault(Locale.forLanguageTag(tag));

            final Calendar source = Calendar.getInstance(UTC);
            source.setTimeInMillis(1736937045123L);

            final Calendar truncated = Dates.truncate(source, CalendarField.DAY_OF_MONTH);

            assertEquals(source.getClass(), truncated.getClass(), tag);
            assertEquals(1736899200000L, truncated.getTimeInMillis(), tag); // 2025-01-15T00:00:00Z
            assertEquals(1736937045123L, source.getTimeInMillis(), tag);
            assertEquals(1736899200000L + 86_400_000L, Dates.ceiling(source, CalendarField.DAY_OF_MONTH).getTimeInMillis(), tag);
        }
    }

    @Test
    public void roundTruncateCeiling_onACalendar_keepTheirDaylightSavingBoundarySemantics() {
        // The boundary engine is untouched by the creator change; these pin the overlap rules.
        final long firstOverlap = LocalDateTime.of(2025, 11, 2, 1, 30)
                .atZone(ZoneId.of("America/Los_Angeles"))
                .withEarlierOffsetAtOverlap()
                .toInstant()
                .toEpochMilli();
        final long secondOverlap = LocalDateTime.of(2025, 11, 2, 1, 30)
                .atZone(ZoneId.of("America/Los_Angeles"))
                .withLaterOffsetAtOverlap()
                .toInstant()
                .toEpochMilli();

        final Calendar first = Dates.createCalendar(firstOverlap, LOS_ANGELES);
        final Calendar second = Dates.createCalendar(secondOverlap, LOS_ANGELES);

        assertEquals(firstOverlap - 30 * 60 * 1000, Dates.truncate(first, CalendarField.HOUR_OF_DAY).getTimeInMillis());
        assertEquals(secondOverlap - 30 * 60 * 1000, Dates.truncate(second, CalendarField.HOUR_OF_DAY).getTimeInMillis());
        assertEquals(secondOverlap - 30 * 60 * 1000, Dates.ceiling(first, CalendarField.HOUR_OF_DAY).getTimeInMillis());
    }

    @Test
    public void roundTruncateCeiling_onACalendar_stillRejectUnsupportedFieldsAndNulls() {
        final Calendar calendar = Dates.createCalendar(1736937045123L, UTC);

        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(calendar, Calendar.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.round(calendar, Calendar.ERA));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling(calendar, Calendar.DAY_OF_WEEK));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate((Calendar) null, CalendarField.DAY_OF_MONTH));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(calendar, (CalendarField) null));
    }

    // ------------------------------------------------------------------------------------------------
    // B5: parseToTimestamp accepts the custom zones its siblings accept, without losing nanoseconds.
    // ------------------------------------------------------------------------------------------------

    /** A zone reusing a registered ID with rules no {@link ZoneId} can express. */
    private static TimeZone customRuleZone() {
        return new SimpleTimeZone(3600000, "Europe/Paris", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0);
    }

    @Test
    public void parseToTimestamp_withACustomRuleZone_agreesWithItsLegacySiblings() {
        final TimeZone zone = customRuleZone();
        final String text = "2025-01-15 10:30:45";

        final long expected = Dates.parseToJUDate(text, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime();

        assertEquals(expected, Dates.parseToDate(text, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime());
        // Previously the only target that rejected this zone outright.
        assertEquals(expected, Dates.parseToTimestamp(text, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime());
        assertEquals(expected, Dates.parseToCalendar(text, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTimeInMillis());
    }

    @Test
    public void parseToTimestamp_withACustomRuleZone_keepsTheNanosecondFraction() {
        final TimeZone zone = customRuleZone();

        final Timestamp value = Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, zone);

        assertEquals(123456789, value.getNanos());
        assertEquals(Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime() + 123, value.getTime());
    }

    @Test
    public void autoDetectedIsoLocalTimestamp_withACustomRuleZone_keepsItsFraction() {
        final TimeZone zone = customRuleZone();

        // The T-separated auto-detected branch resolves through the same fallback.
        assertEquals(Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime() + 500,
                Dates.parseToJUDate("2025-01-15T10:30:45.5", null, zone).getTime());
    }

    @Test
    public void customRuleZone_overlap_isAcceptedRatherThanRejected() {
        // The documented cost of the fallback: Calendar resolution has no overlap detection, so an
        // ambiguous wall time is silently resolved instead of rejected - exactly as the legacy siblings
        // already did for this zone. DST ends on 1 October at 00:00 here, so 00:00-00:59 occurs twice.
        final TimeZone zone = customRuleZone();
        final String ambiguous = "2025-10-01 00:30:00";

        final long viaJUDate = Dates.parseToJUDate(ambiguous, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime();

        assertEquals(viaJUDate, Dates.parseToTimestamp(ambiguous, Dates.LOCAL_DATE_TIME_FORMAT, zone).getTime());
        assertEquals(viaJUDate, Dates.parseToTimestamp(ambiguous + ".000", Dates.LOCAL_TIMESTAMP_FORMAT, zone).getTime());
    }

    @Test
    public void parseToTimestamp_withARepresentableZone_stillRejectsGapsAndOverlaps() {
        // The fallback must not weaken strict resolution for zones java.time can express.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-03-09 02:30:00.000", Dates.LOCAL_TIMESTAMP_FORMAT, LOS_ANGELES));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-11-02 01:30:00.000", Dates.LOCAL_TIMESTAMP_FORMAT, LOS_ANGELES));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-03-09 02:30:00.123456789", null, LOS_ANGELES));
    }

    @Test
    public void parseToTimestamp_nanosecondPrecision_isUnchangedForOrdinaryZones() {
        assertEquals(123456789, Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, UTC).getNanos());
        assertEquals(500000000, Dates.parseToTimestamp("2025-01-15 10:30:45.5", null, UTC).getNanos());
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
    }

    // ------------------------------------------------------------------------------------------------
    // B6: a daylight-saving gap is named as precisely as an overlap.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void daylightSavingGap_isReportedWithTheNonexistentLocalTime_forEveryZoneLessShape() {
        assertGapMessage(() -> Dates.parseToJUDate("2025-03-09 02:30:00", Dates.LOCAL_DATE_TIME_FORMAT, LOS_ANGELES));
        assertGapMessage(() -> Dates.parseToJUDate("2025-03-09T02:30:00", Dates.ISO_LOCAL_DATE_TIME_FORMAT, LOS_ANGELES));
        assertGapMessage(() -> Dates.parseToJUDate("2025-03-09 02:30:00.000", Dates.LOCAL_TIMESTAMP_FORMAT, LOS_ANGELES));
        assertGapMessage(() -> Dates.parseToJUDate("2025-03-09T02:30:00.000", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, LOS_ANGELES));
        // Date-only input resolves at local midnight; Sao Paulo used to spring forward at midnight.
        assertGapMessage(() -> Dates.parseToJUDate("2018-11-04", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("America/Sao_Paulo")));
    }

    private static void assertGapMessage(final org.junit.jupiter.api.function.Executable parse) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, parse);
        assertTrue(e.getMessage().contains("Nonexistent local date-time"), "expected a DST-gap diagnostic but got: " + e.getMessage());
        assertTrue(e.getMessage().contains("DST gap"), e.getMessage());
    }

    @Test
    public void daylightSavingOverlap_diagnosticIsUnchanged() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2025-11-02 01:30:00", Dates.LOCAL_DATE_TIME_FORMAT, LOS_ANGELES));

        assertTrue(e.getMessage().contains("Ambiguous local date-time"), e.getMessage());
        assertTrue(e.getMessage().contains("DST overlap"), e.getMessage());
    }

    @Test
    public void malformedText_isNeverReportedAsADaylightSavingGapOrOverlap() {
        // The check now runs before the parser, so it must match the whole shape: text that merely
        // starts like it is a syntax error and has to be reported as one.
        assertSyntaxError(() -> Dates.parseToJUDate("2025-11-02 01:30:00 trailing", Dates.LOCAL_DATE_TIME_FORMAT, LOS_ANGELES));
        assertSyntaxError(() -> Dates.parseToJUDate("2025-03-09 02:30:00xyz", Dates.LOCAL_DATE_TIME_FORMAT, LOS_ANGELES));
        assertSyntaxError(() -> Dates.parseToJUDate("2025-03-09T02:30:00", Dates.LOCAL_DATE_TIME_FORMAT, LOS_ANGELES));
        assertSyntaxError(() -> Dates.parseToJUDate("2025-03-09 02:30:00.00", Dates.LOCAL_TIMESTAMP_FORMAT, LOS_ANGELES));
        assertSyntaxError(() -> Dates.parseToJUDate("2018-11-04 ", Dates.LOCAL_DATE_FORMAT, TimeZone.getTimeZone("America/Sao_Paulo")));
    }

    private static void assertSyntaxError(final org.junit.jupiter.api.function.Executable parse) {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, parse);
        assertFalse(e.getMessage().contains("DST gap"), "malformed input reported as a DST gap: " + e.getMessage());
        assertFalse(e.getMessage().contains("DST overlap"), "malformed input reported as a DST overlap: " + e.getMessage());
    }

    @Test
    public void validZoneLessInput_isUnaffectedByTheEarlierGapCheck() {
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15T10:30:45", Dates.ISO_LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToJUDate("2025-01-15T10:30:45.123", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736899200000L, Dates.parseToJUDate("2025-01-15", Dates.LOCAL_DATE_FORMAT, UTC).getTime());
    }

    @Test
    public void customPatterns_keepLegacySimpleDateFormatOverlapResolution() {
        // Only the named zone-less shapes get the strict check; a custom pattern still resolves silently.
        assertEquals(1762075800000L, Dates.parseToJUDate("02/11/2025 01:30", "dd/MM/yyyy HH:mm", LOS_ANGELES).getTime());
    }

    // ------------------------------------------------------------------------------------------------
    // B7: the bracketed-zone fast path requires the ISO 'T' separator.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void bracketedZoneFastPath_requiresTheIsoSeparator() {
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z[UTC]").getTime());

        // A space separator is not the ISO zoned grammar; it must not be routed there.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15 10:30:45.123[Asia/Kolkata]"));
    }

    // ------------------------------------------------------------------------------------------------
    // D1: DTF.ISO_OFFSET_TIMESTAMP completes the formatter family.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void dtfIsoOffsetTimestamp_roundTripsAndIsReusedByOf() {
        assertSame(DTF.ISO_OFFSET_TIMESTAMP, DTF.of("uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX"));
        assertEquals("uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX", DTF.ISO_OFFSET_TIMESTAMP.toString());

        final Timestamp value = new Timestamp(1736937045123L);
        assertEquals("2025-01-15T16:00:45.123+05:30", DTF.ISO_OFFSET_TIMESTAMP.format(value.toInstant().atZone(KOLKATA.toZoneId())));
        assertEquals(1736937045123L, DTF.ISO_OFFSET_TIMESTAMP.parseToTimestamp("2025-01-15T16:00:45.123+05:30").getTime());
        assertNull(DTF.ISO_OFFSET_TIMESTAMP.format((java.util.Date) null));
    }

    @Test
    public void datesParseTo_withIsoOffsetTimestampFormat_isUnchanged() {
        final String text = "2025-01-15T16:00:45.123+05:30";

        assertEquals(1736937045123L, Dates.parseToInstant(text, Dates.ISO_OFFSET_TIMESTAMP_FORMAT).toEpochMilli());
        assertEquals(1736937045123L, Dates.parseToJUDate(text, Dates.ISO_OFFSET_TIMESTAMP_FORMAT).getTime());
        assertEquals(1736937045123L, Dates.parseToOffsetDateTime(text, Dates.ISO_OFFSET_TIMESTAMP_FORMAT).toInstant().toEpochMilli());
        assertEquals(LocalDateTime.of(2025, 1, 15, 16, 0, 45, 123_000_000), Dates.parseToLocalDateTime(text, Dates.ISO_OFFSET_TIMESTAMP_FORMAT));

        // The historical compact +HHmm extension still parses.
        assertEquals(1736937045123L, Dates.parseToInstant("2025-01-15T16:00:45.123+0530", Dates.ISO_OFFSET_TIMESTAMP_FORMAT).toEpochMilli());

        // The strict grammar is unchanged: exactly three fraction digits, and no offset seconds.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T16:00:45.1+05:30", Dates.ISO_OFFSET_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T16:00:45.123+05:30:15", Dates.ISO_OFFSET_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-01-15T16:00:45.123", Dates.ISO_OFFSET_TIMESTAMP_FORMAT));
    }

    @Test
    public void datesFormat_withIsoOffsetTimestampFormat_isUnchanged() {
        assertEquals("2025-01-15T16:00:45.123+05:30", Dates.format(new Timestamp(1736937045123L), Dates.ISO_OFFSET_TIMESTAMP_FORMAT, KOLKATA));
        // The zone-bearing default for SQL types with an explicit zone is this same pattern.
        assertEquals("2025-01-15T16:00:45.123+05:30", Dates.format(new java.sql.Date(1736937045123L), null, KOLKATA));
    }

    // ------------------------------------------------------------------------------------------------
    // D2: the saturated format caches stay correct.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void manyOneOffPatterns_doNotBreakSubsequentFormatting() {
        final Date value = new Date(1736937045123L);

        for (int i = 0; i < 200; i++) {
            assertEquals("2025-01-15 " + i, Dates.format(value, "yyyy-MM-dd '" + i + "'", UTC));
        }

        assertEquals("2025-01-15", Dates.format(value, Dates.LOCAL_DATE_FORMAT, UTC));
        assertEquals("2025-01-15T10:30:45.123Z", Dates.format(new Timestamp(1736937045123L)));
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
    }

    @Test
    public void manyOneOffDtfPatterns_stillReturnEqualFormatters() {
        for (int i = 0; i < 200; i++) {
            final String pattern = "yyyy-MM-dd '" + i + "'";
            assertEquals(DTF.of(pattern), DTF.of(pattern));
        }

        assertSame(DTF.LOCAL_DATE, DTF.of("uuuu-MM-dd"));
    }

    // ------------------------------------------------------------------------------------------------
    // D3: a custom pattern renders an XMLGregorianCalendar in its own timezone, like Calendar.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void formatXmlCalendar_withACustomPattern_retainsTheXmlTimeZone() {
        final XMLGregorianCalendar xml = Dates.createXMLGregorianCalendar(1736937045000L, KOLKATA);
        final Calendar calendar = Dates.createCalendar(1736937045000L, KOLKATA);

        // Previously "2025-01-15 10:30:45": the value's +05:30 offset was dropped for the default zone.
        assertEquals("2025-01-15 16:00:45", Dates.format(xml, Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals(Dates.format(calendar, Dates.LOCAL_DATE_TIME_FORMAT), Dates.format(xml, Dates.LOCAL_DATE_TIME_FORMAT));

        final StringBuilder sb = new StringBuilder();
        Dates.formatTo(xml, Dates.LOCAL_DATE_TIME_FORMAT, sb);
        assertEquals("2025-01-15 16:00:45", sb.toString());
    }

    @Test
    public void formatXmlCalendar_explicitZoneAndFixedZoneFormats_stillWin() {
        final XMLGregorianCalendar xml = Dates.createXMLGregorianCalendar(1736937045000L, KOLKATA);

        assertEquals("2025-01-15 10:30:45", Dates.format(xml, Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("2025-01-15T10:30:45Z", Dates.format(xml, Dates.ISO_8601_DATE_TIME_FORMAT));
        assertEquals("Wed, 15 Jan 2025 10:30:45 GMT", Dates.format(xml, Dates.HTTP_DATE_FORMAT));
    }

    @Test
    public void formatXmlCalendar_withAnUndefinedXmlTimeZone_usesTheDefaultZone() {
        TimeZone.setDefault(KOLKATA);

        final XMLGregorianCalendar xml = Dates.createXMLGregorianCalendar(1736937045000L, KOLKATA);
        xml.setTimezone(javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED);

        // toGregorianCalendar() resolves an undefined XML timezone to the live default zone.
        assertEquals("2025-01-15 16:00:45", Dates.format(xml, Dates.LOCAL_DATE_TIME_FORMAT));
    }

    @Test
    public void formatXmlCalendar_lexicalDefault_isUnchanged() {
        final XMLGregorianCalendar xml = Dates.createXMLGregorianCalendar(1736917245123L, KOLKATA);

        assertEquals("2025-01-15T10:30:45.123+05:30", Dates.format(xml));
        assertEquals("2025-01-15T05:00:45.123Z", Dates.format(xml, null, UTC));
    }

    // ------------------------------------------------------------------------------------------------
    // Remaining items: XML factory ordering, and the two rewritten comparisons.
    // ------------------------------------------------------------------------------------------------

    @Test
    public void parseToXmlCalendar_stillHonorsTheNullContractBeforeAnyOtherCheck() {
        assertNull(Dates.parseToXMLGregorianCalendar((String) null));
        assertNull(Dates.parseToXMLGregorianCalendar("null"));
        assertNull(Dates.parseToXMLGregorianCalendar("NULL", Dates.LOCAL_DATE_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(""));
    }

    @Test
    public void isSameInstant_onCalendars_isUnchanged() {
        assertTrue(Dates.isSameInstant(Dates.createCalendar(1672585530123L, UTC), Dates.createCalendar(1672585530123L, KOLKATA)));
        assertFalse(Dates.isSameInstant(Dates.createCalendar(1672585530123L, UTC), Dates.createCalendar(1672585530124L, UTC)));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameInstant((Calendar) null, Dates.createCalendar(0L, UTC)));
    }

    @Test
    public void isOverlapping_onDates_isUnchanged() {
        final Date d1 = new Date(1000L);
        final Date d5 = new Date(5000L);
        final Date d10 = new Date(10000L);
        final Date d15 = new Date(15000L);
        final Date d20 = new Date(20000L);

        assertTrue(Dates.isOverlapping(d1, d10, d5, d15));
        assertFalse(Dates.isOverlapping(d1, d10, d15, d20));
        assertFalse(Dates.isOverlapping(d1, d10, d10, d20), "endpoints are exclusive");
        assertFalse(Dates.isOverlapping(d5, d5, d1, d10), "an empty range never overlaps");
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(d10, d1, d5, d15));
        assertThrows(IllegalArgumentException.class, () -> Dates.isOverlapping(null, d10, d5, d15));

        // Timestamp endpoints still compare at nanosecond precision.
        final Timestamp justAfter = new Timestamp(10000L);
        justAfter.setNanos(1);
        assertTrue(Dates.isOverlapping(d1, justAfter, d10, d20));
        assertFalse(Dates.isOverlapping(d1, d10, d10, d20));
    }
}
