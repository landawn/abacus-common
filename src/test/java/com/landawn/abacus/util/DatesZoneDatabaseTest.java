package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-08-31 review cycle.
 *
 * <ul>
 * <li><b>B1</b> - the class mixed two zone databases. {@code java.util.TimeZone} discards the
 * transitions before 1900-01-01T00:00:00Z and reports the zone's present raw offset for every earlier
 * instant (its table begins at 1900 with the offset then in force), while {@code java.time} keeps the
 * complete local-mean-time history; 548 of the 604 IANA zones
 * disagree there. Formatting, the fixed-shape legacy parsers and the {@code set*}/{@code add*}/
 * {@code getFragment*} field operations ran on the legacy table while rounding, the same-day
 * comparisons and {@code parseToTimestamp} ran on {@code java.time}, so the class printed one civil
 * date and computed with another.</li>
 * <li><b>B2</b> - {@code add*}/{@code roll*(Calendar, ...)} did field arithmetic on the caller's own
 * calendar, inheriting its Julian/Gregorian cutover and calendar system.</li>
 * <li><b>B3</b> - a locale selected the calendar system of a legacy formatter, but only for
 * {@code BuddhistCalendar} (which extends {@code GregorianCalendar}), not for
 * {@code JapaneseImperialCalendar}.</li>
 * <li><b>D3</b> - parse failures named an internal {@code uuuu} pattern the caller never wrote, and a
 * partial predefined pattern leaked {@code java.time}'s internal {@code TemporalAccessor} message.</li>
 * <li><b>D5</b> - {@code 'Y'} (week-based year) was treated as satisfying the plain-year requirement
 * of a complete date.</li>
 * </ul>
 */
public class DatesZoneDatabaseTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    /** 1900-01-01T00:00:00Z: the instant at which the JDK's legacy zone table begins. */
    private static final long LEGACY_ZONE_HISTORY_START = -2208988800000L;

    /** Zones whose local mean time differs from their 1900 offset, so they exercise the split. */
    private static final String[] LMT_ZONES = { "Asia/Kolkata", "Africa/Monrovia", "Europe/Dublin", "America/New_York", "Europe/Berlin", "Africa/Abidjan",
            "Asia/Tehran", "Pacific/Chatham" };

    private static final long[] PRE_1900 = { Instant.parse("1899-06-15T12:00:00Z").toEpochMilli(), Instant.parse("1850-03-01T00:00:00Z").toEpochMilli(),
            Instant.parse("1700-11-20T18:45:00Z").toEpochMilli(), Instant.parse("1899-12-31T23:59:59Z").toEpochMilli() };

    private TimeZone originalZone;

    private Locale originalLocale;

    @BeforeEach
    public void captureDefaults() {
        originalZone = TimeZone.getDefault();
        originalLocale = Locale.getDefault();
    }

    @AfterEach
    public void restoreDefaults() {
        TimeZone.setDefault(originalZone);
        Locale.setDefault(originalLocale);
    }

    // ------------------------------------------------------------------------------------------
    // B1 - one civil view
    // ------------------------------------------------------------------------------------------

    /**
     * Every legacy instant target has to resolve the same text to the same instant. Before the fix
     * {@code parseToTimestamp} (and {@code parseToInstant}) went through the java.time JDBC path while
     * the other four went through {@code Calendar}, so "1899-06-15 12:00:00" in Asia/Kolkata produced
     * two instants 8m50s apart with no explicit format and no explicit zone.
     */
    @Test
    public void everyLegacyTargetResolvesOnePreNineteenHundredInstant() {
        for (final String zoneId : LMT_ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));

            for (final String text : new String[] { "1899-06-15 12:00:00", "1850-01-01 00:00:00", "1899-06-15 12:00:00.500" }) {
                final long expected = Dates.parseToJUDate(text).getTime();

                assertEquals(expected, Dates.parseToTimestamp(text).getTime(), zoneId + " parseToTimestamp: " + text);
                assertEquals(expected, Dates.parseToDate(text).getTime(), zoneId + " parseToDate: " + text);
                assertEquals(expected, Dates.parseToTime(text).getTime(), zoneId + " parseToTime: " + text);
                assertEquals(expected, Dates.parseToCalendar(text).getTimeInMillis(), zoneId + " parseToCalendar: " + text);
                assertEquals(expected, Dates.parseToGregorianCalendar(text).getTimeInMillis(), zoneId + " parseToGregorianCalendar: " + text);
                assertEquals(expected, Dates.parseToInstant(text).toEpochMilli(), zoneId + " parseToInstant: " + text);
            }
        }
    }

    /** The same, with the constants supplied explicitly rather than auto-detected. */
    @Test
    public void everyLegacyTargetResolvesOnePreNineteenHundredInstant_explicitFormat() {
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");

        for (final String[] pair : new String[][] { { "1899-06-15 12:00:00", Dates.LOCAL_DATE_TIME_FORMAT },
                { "1899-06-15T12:00:00", Dates.ISO_LOCAL_DATE_TIME_FORMAT }, { "1899-06-15 12:00:00.500", Dates.LOCAL_TIMESTAMP_FORMAT },
                { "1899-06-15T12:00:00.500", Dates.ISO_LOCAL_TIMESTAMP_FORMAT }, { "1899-06-15", Dates.LOCAL_DATE_FORMAT } }) {
            final String text = pair[0];
            final String format = pair[1];
            final long expected = Dates.parseToJUDate(text, format, kolkata).getTime();

            assertEquals(expected, Dates.parseToTimestamp(text, format, kolkata).getTime(), format);
            assertEquals(expected, Dates.parseToDate(text, format, kolkata).getTime(), format);
            assertEquals(expected, Dates.parseToCalendar(text, format, kolkata).getTimeInMillis(), format);
            assertEquals(expected, Dates.parseToInstant(text, format, kolkata).toEpochMilli(), format);
        }
    }

    /**
     * A predefined constant's rendering of an instant must re-parse to a value that renders identically.
     * Before the fix {@code format} used the legacy zone table and {@code parseToTimestamp} used
     * java.time, so "1899-06-15 12:00:00.000" came back as "1899-06-15 12:08:50.000".
     */
    @Test
    public void predefinedConstantsRoundTripBeforeNineteenHundred() {
        final String[] formats = { Dates.LOCAL_DATE_TIME_FORMAT, Dates.ISO_LOCAL_DATE_TIME_FORMAT, Dates.LOCAL_TIMESTAMP_FORMAT,
                Dates.ISO_LOCAL_TIMESTAMP_FORMAT, Dates.ISO_8601_DATE_TIME_FORMAT, Dates.ISO_8601_TIMESTAMP_FORMAT };

        for (final String zoneId : LMT_ZONES) {
            final TimeZone zone = TimeZone.getTimeZone(zoneId);

            for (final long millis : PRE_1900) {
                for (final String format : formats) {
                    final String text;

                    try {
                        text = Dates.format(new Timestamp(millis), format, zone);
                    } catch (final IllegalArgumentException e) {
                        continue; // a fixed-UTC constant refuses a non-UTC zone; not this test's subject
                    }

                    final String reformatted;

                    try {
                        reformatted = Dates.format(Dates.parseToTimestamp(text, format, zone), format, zone);
                    } catch (final IllegalArgumentException e) {
                        continue; // a DST gap or overlap the zone-less constants deliberately reject
                    }

                    assertEquals(text, reformatted, zoneId + " " + format + " @" + millis);
                }
            }
        }
    }

    /**
     * {@code truncate} resolves through {@link ZoneId} while {@code format} used the legacy table, so
     * before the fix {@code format(truncate(x, DATE))} was not the midnight of the day {@code format(x)}
     * printed - it landed on {@code 1899-06-15 00:08:50} in Asia/Kolkata.
     */
    @Test
    public void truncateLandsOnThePrintedBoundaryBeforeNineteenHundred() {
        for (final String zoneId : LMT_ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));

            for (final long millis : PRE_1900) {
                final java.util.Date date = new java.util.Date(millis);
                final String printed = Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT);

                assertEquals(printed.substring(0, 10) + " 00:00:00", Dates.format(Dates.truncate(date, Calendar.DATE), Dates.LOCAL_DATE_TIME_FORMAT),
                        zoneId + " truncate(DATE) of " + printed);
                assertEquals(printed.substring(0, 8) + "01 00:00:00", Dates.format(Dates.truncate(date, Calendar.MONTH), Dates.LOCAL_DATE_TIME_FORMAT),
                        zoneId + " truncate(MONTH) of " + printed);
                assertEquals(printed.substring(0, 5) + "01-01 00:00:00", Dates.format(Dates.truncate(date, Calendar.YEAR), Dates.LOCAL_DATE_TIME_FORMAT),
                        zoneId + " truncate(YEAR) of " + printed);
            }
        }
    }

    /**
     * The civil-field queries resolve through {@link ZoneId}; before the fix {@code isSameDay} could
     * report two values as the same day while {@code format} printed two different dates for them.
     */
    @Test
    public void civilFieldQueriesAgreeWithTheirPrintedDateBeforeNineteenHundred() {
        for (final String zoneId : LMT_ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));

            for (final long millis : PRE_1900) {
                final java.util.Date date = new java.util.Date(millis);
                final java.util.Date tenMinutesLater = new java.util.Date(millis + 600_000L);
                final LocalDate printed = LocalDate.parse(Dates.format(date, Dates.LOCAL_DATE_FORMAT));
                final LocalDate printedLater = LocalDate.parse(Dates.format(tenMinutesLater, Dates.LOCAL_DATE_FORMAT));

                assertEquals(printed.equals(printedLater), Dates.isSameDay(date, tenMinutesLater), zoneId + " isSameDay @" + millis);
                assertEquals(printed.getDayOfMonth() == printed.lengthOfMonth(), Dates.isLastDayOfMonth(date), zoneId + " isLastDayOfMonth @" + millis);
                assertEquals(printed.lengthOfMonth(), Dates.lengthOfMonth(date), zoneId + " lengthOfMonth @" + millis);
                assertEquals(printed.lengthOfYear(), Dates.lengthOfYear(date), zoneId + " lengthOfYear @" + millis);
                assertEquals(printed.getDayOfYear(), Dates.getFragmentInDays(date, CalendarField.YEAR), zoneId + " getFragmentInDays(YEAR) @" + millis);
            }
        }
    }

    /** The field writers must stay on the same civil view as the readers. */
    @Test
    public void fieldOperationsAgreeWithTheirPrintedDateBeforeNineteenHundred() {
        for (final String zoneId : LMT_ZONES) {
            TimeZone.setDefault(TimeZone.getTimeZone(zoneId));

            for (final long millis : PRE_1900) {
                final java.util.Date date = new java.util.Date(millis);
                final String printed = Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT);
                final LocalDate printedDate = LocalDate.parse(printed.substring(0, 10));

                assertEquals(printed, Dates.format(Dates.setDays(date, printedDate.getDayOfMonth()), Dates.LOCAL_DATE_TIME_FORMAT),
                        zoneId + " setDays is a no-op on " + printed);
                assertEquals(printed, Dates.format(Dates.setMonths(date, printedDate.getMonthValue() - 1), Dates.LOCAL_DATE_TIME_FORMAT),
                        zoneId + " setMonths is a no-op on " + printed);
                assertEquals(printedDate.plusDays(1).toString(), Dates.format(Dates.addDays(date, 1), Dates.LOCAL_DATE_FORMAT),
                        zoneId + " addDays(1) from " + printed);
                assertEquals(printedDate.plusMonths(1).toString(), Dates.format(Dates.addMonths(date, 1), Dates.LOCAL_DATE_FORMAT),
                        zoneId + " addMonths(1) from " + printed);
            }
        }
    }

    /**
     * The four-digit-year guard used to read the year from a legacy {@code Calendar} while the value was
     * rendered by java.time, so {@code format} emitted {@code "0000-12-31T23:16:52-00:43:08[Africa/Monrovia]"}
     * - a year the constants forbid and this class's own parser rejects.
     */
    @Test
    public void theFourDigitYearGuardSeesTheOffsetTheRendererUses() {
        final long yearOne = Instant.parse("0001-01-01T00:00:00Z").toEpochMilli();
        final java.util.Date date = new java.util.Date(yearOne);
        final TimeZone monrovia = TimeZone.getTimeZone("Africa/Monrovia"); // -00:43:08 local mean time

        // java.time reads this instant as 0000-12-31 in Monrovia, so every predefined constant refuses it.
        assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.LOCAL_DATE_FORMAT, monrovia));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT, monrovia));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(date, Dates.ISO_ZONED_DATE_TIME_FORMAT, monrovia));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(Dates.createGregorianCalendar(yearOne, monrovia)));

        // Asia/Kolkata's +05:53:28 keeps the same instant in year 0001, so it is accepted and printed
        // with the local-mean-time offset the rest of the class uses.
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        assertEquals("0001-01-01", Dates.format(date, Dates.LOCAL_DATE_FORMAT, kolkata));
        assertEquals("0001-01-01T05:53:28+05:53:28[Asia/Kolkata]", Dates.format(date, Dates.ISO_ZONED_DATE_TIME_FORMAT, kolkata));
        assertEquals("0001-01-01T05:53:28+05:53:28[Asia/Kolkata]", Dates.format(Dates.createGregorianCalendar(yearOne, kolkata)));
    }

    /**
     * The canary for the {@code LEGACY_ZONE_HISTORY_START} fast path: the substitution is skipped from
     * 1900-01-01T00:00:00Z onwards, which is only correct while the two engines agree there.
     */
    @Test
    public void theTwoZoneEnginesAgreeFromNineteenHundredOnwards() {
        final long[] probes = { LEGACY_ZONE_HISTORY_START, LEGACY_ZONE_HISTORY_START + 1, 0L, 1736937045123L,
                Instant.parse("2038-01-19T03:14:07Z").toEpochMilli() };
        final List<String> mismatches = new ArrayList<>();

        for (final String id : ZoneId.getAvailableZoneIds()) {
            final TimeZone legacy = TimeZone.getTimeZone(id);
            final ZoneId modern = ZoneId.of(id);

            for (final long millis : probes) {
                final int legacyOffset = legacy.getOffset(millis);
                final int modernOffset = modern.getRules().getOffset(Instant.ofEpochMilli(millis)).getTotalSeconds() * 1000;

                if (legacyOffset != modernOffset) {
                    mismatches.add(id + " @" + millis + ": " + legacyOffset + " vs " + modernOffset);
                }
            }
        }

        assertTrue(mismatches.isEmpty(), "the legacy zone table is no longer complete from 1900 on: " + mismatches);

        // And they really do differ before it, so these tests are not vacuous.
        assertNotEquals(TimeZone.getTimeZone("Asia/Kolkata").getOffset(LEGACY_ZONE_HISTORY_START - 1),
                ZoneId.of("Asia/Kolkata").getRules().getOffset(Instant.ofEpochMilli(LEGACY_ZONE_HISTORY_START - 1)).getTotalSeconds() * 1000);
    }

    /** Modern values must keep the caller's own zone object, so their rendering is untouched. */
    @Test
    public void modernRenderingIsUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        final java.util.Date date = new java.util.Date(1736937045123L);

        assertEquals("2025-01-15T10:30:45Z", Dates.format(date));
        assertEquals("2025-01-15 16:00:45", Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT));
        // ISO_ZONED_DATE_TIME_FORMAT has no fraction field; the Calendar default (which does) is
        // covered by theFourDigitYearGuardSeesTheOffsetTheRendererUses.
        assertEquals("2025-01-15T16:00:45+05:30[Asia/Kolkata]", Dates.format(date, Dates.ISO_ZONED_DATE_TIME_FORMAT, TimeZone.getTimeZone("Asia/Kolkata")));
        assertEquals("Wed, 15 Jan 2025 10:30:45 GMT", Dates.format(date, Dates.HTTP_DATE_FORMAT));
        assertEquals("2025-01-15T16:00:45.123+05:30", Dates.format(new Timestamp(1736937045123L), Dates.ISO_OFFSET_TIMESTAMP_FORMAT));
        // A zone-name field still resolves against the caller's own region zone.
        assertEquals("2025-01-15 11:30:45 CET", Dates.format(date, "yyyy-MM-dd HH:mm:ss zzz", TimeZone.getTimeZone("Europe/Berlin")));
        assertEquals(1736937045000L, Dates.parseToJUDate(Dates.format(date)).getTime());
    }

    /** A zone whose rules no {@code ZoneId} can express has no java.time view to align with. */
    @Test
    public void aZoneJavaTimeCannotExpressKeepsTheLegacyEngine() {
        // Daylight-saving rules no ZoneId can express, and June falls inside them, so the zone is +02:00.
        final TimeZone custom = new SimpleTimeZone(3_600_000, "Custom/Weird", Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0);
        final long millis = Instant.parse("1899-06-15T12:00:00Z").toEpochMilli();

        assertEquals("1899-06-15 14:00:00", Dates.format(new java.util.Date(millis), Dates.LOCAL_DATE_TIME_FORMAT, custom));
        assertEquals(millis, Dates.parseToJUDate("1899-06-15 14:00:00", Dates.LOCAL_DATE_TIME_FORMAT, custom).getTime());
        assertEquals(millis, Dates.parseToTimestamp("1899-06-15 14:00:00", Dates.LOCAL_DATE_TIME_FORMAT, custom).getTime());
    }

    /** A custom pattern keeps the legacy engine on both sides, so it still round-trips with itself. */
    @Test
    public void customPatternsKeepTheLegacyEngineOnBothSides() {
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final java.util.Date date = new java.util.Date(Instant.parse("1899-06-15T12:00:00Z").toEpochMilli());
        final String text = Dates.format(date, "yyyy/MM/dd HH:mm:ss", kolkata);

        assertEquals(date.getTime(), Dates.parseToJUDate(text, "yyyy/MM/dd HH:mm:ss", kolkata).getTime());
        assertEquals(text, Dates.format(Dates.parseToJUDate(text, "yyyy/MM/dd HH:mm:ss", kolkata), "yyyy/MM/dd HH:mm:ss", kolkata));
    }

    /**
     * Characterization of the documented limit of {@code legacyRenderingZone}: the stand-in zone is a
     * single offset, so arithmetic that carries a pre-1900 value out of its local-mean-time regime
     * keeps the offset it started on and the wall clock shifts by that regime's delta. Within the
     * regime the wall clock is preserved exactly, and every result still round-trips.
     */
    @Test
    public void fieldArithmeticThatLeavesTheLocalMeanTimeRegimeKeepsTheOffsetItStartedOn() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata")); // +05:21:10 in 1899, +05:30 from 1906
        final java.util.Date old = new java.util.Date(Instant.parse("1899-06-15T06:30:00Z").toEpochMilli());

        assertEquals("1899-06-15 11:51:10", Dates.format(old, Dates.LOCAL_DATE_TIME_FORMAT));
        // Inside the regime the wall clock is preserved.
        assertEquals("1900-06-15 11:51:10", Dates.format(Dates.addYears(old, 1), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("1904-06-15 11:51:10", Dates.format(Dates.addYears(old, 5), Dates.LOCAL_DATE_TIME_FORMAT));
        // Past the 1906 transition it shifts by the 8m50s delta, as documented.
        assertEquals("2025-06-15 12:00:00", Dates.format(Dates.addYears(old, 126), Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2030-06-15 12:00:00", Dates.format(Dates.setYears(old, 2030), Dates.LOCAL_DATE_TIME_FORMAT));

        // Whatever the result, it renders and re-parses to itself.
        for (final int years : new int[] { 1, 5, 7, 126 }) {
            final String text = Dates.format(Dates.addYears(old, years), Dates.LOCAL_DATE_TIME_FORMAT);
            assertEquals(text, Dates.format(Dates.parseToJUDate(text, Dates.LOCAL_DATE_TIME_FORMAT), Dates.LOCAL_DATE_TIME_FORMAT));
        }
    }

    // ------------------------------------------------------------------------------------------
    // B2 - Calendar field arithmetic is proleptic Gregorian
    // ------------------------------------------------------------------------------------------

    /**
     * {@code addYears}/{@code addMonths} on a {@code Calendar} ran on the caller's own calendar, so a
     * default {@code GregorianCalendar}'s 1582 cutover moved the result ten days away from the date
     * {@code format} prints for the same instant, while the {@code java.util.Date} overloads and
     * {@code round}/{@code truncate}/{@code ceiling} were already proleptic.
     */
    @Test
    public void calendarArithmeticIsProlepticWhateverTheCallersCutover() {
        final long millis = Dates.parseToJUDate("1582-10-10T00:00:00Z", Dates.ISO_8601_DATE_TIME_FORMAT).getTime();
        final java.util.Date reference = new java.util.Date(millis);

        final List<Calendar> sources = new ArrayList<>();
        sources.add(new GregorianCalendar(UTC));
        sources.add(Dates.createGregorianCalendar(millis, UTC));
        sources.add(Dates.createCalendar(millis, UTC));
        sources.add(Dates.parseToCalendar("1582-10-10 00:00:00", Dates.LOCAL_DATE_TIME_FORMAT, UTC));

        for (final Calendar source : sources) {
            source.setTimeInMillis(millis);
            final String label = source.getClass().getSimpleName() + " (cutover "
                    + (source instanceof GregorianCalendar ? ((GregorianCalendar) source).getGregorianChange().getTime() : "n/a") + ")";

            assertEquals(Dates.format(Dates.addYears(reference, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC),
                    Dates.format(Dates.addYears(source, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC), label + " addYears");
            assertEquals(Dates.format(Dates.addMonths(reference, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC),
                    Dates.format(Dates.addMonths(source, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC), label + " addMonths");
            assertEquals(Dates.format(Dates.addDays(reference, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC),
                    Dates.format(Dates.addDays(source, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC), label + " addDays");
            assertEquals(Dates.format(Dates.addWeeks(reference, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC),
                    Dates.format(Dates.addWeeks(source, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC), label + " addWeeks");
        }

        // The concrete values, so a regression is legible rather than just "the two disagree".
        final GregorianCalendar legacyCutover = new GregorianCalendar(UTC);
        legacyCutover.setTimeInMillis(millis);
        assertEquals("1583-10-10 00:00:00", Dates.format(Dates.addYears(legacyCutover, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("1582-11-10 00:00:00", Dates.format(Dates.addMonths(legacyCutover, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
    }

    /** The deprecated {@code roll} alias shares the implementation, so it moved with it. */
    @Test
    @SuppressWarnings("deprecation")
    public void rollOnACalendarIsProlepticToo() {
        final long millis = Dates.parseToJUDate("1582-10-10T00:00:00Z", Dates.ISO_8601_DATE_TIME_FORMAT).getTime();
        final GregorianCalendar legacyCutover = new GregorianCalendar(UTC);
        legacyCutover.setTimeInMillis(millis);

        assertEquals("1583-10-10 00:00:00", Dates.format(Dates.roll(legacyCutover, 1, CalendarField.YEAR), Dates.LOCAL_DATE_TIME_FORMAT, UTC));
    }

    /** Rebuilding through {@code createCalendar} must preserve the caller's type, zone and settings. */
    @Test
    public void calendarArithmeticKeepsTheCallersTypeZoneAndSettings() {
        final GregorianCalendar source = new GregorianCalendar(TimeZone.getTimeZone("Asia/Kolkata"), Locale.FRANCE);
        source.setTimeInMillis(1736937045123L);
        source.setLenient(false);
        source.setMinimalDaysInFirstWeek(4);
        source.setFirstDayOfWeek(Calendar.MONDAY);
        final java.util.Date cutover = new java.util.Date(0L);
        source.setGregorianChange(cutover);

        final GregorianCalendar result = Dates.addYears(source, 1);

        assertEquals(GregorianCalendar.class, result.getClass());
        assertEquals("Asia/Kolkata", result.getTimeZone().getID());
        assertEquals(false, result.isLenient());
        assertEquals(4, result.getMinimalDaysInFirstWeek());
        assertEquals(Calendar.MONDAY, result.getFirstDayOfWeek());
        assertEquals(cutover, result.getGregorianChange());
        assertEquals(1736937045123L, source.getTimeInMillis(), "the source must not be mutated");
        assertEquals("2026-01-15 16:00:45", Dates.format(result, Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("Asia/Kolkata")));
    }

    /** A non-Gregorian default locale must not change what the arithmetic means. */
    @Test
    public void calendarArithmeticIsUnaffectedByANonGregorianDefaultLocale() {
        for (final String tag : new String[] { "th-TH-u-ca-buddhist", "ja-JP-u-ca-japanese", "en-US" }) {
            Locale.setDefault(Locale.forLanguageTag(tag));
            final Calendar cal = Calendar.getInstance(UTC);
            cal.setTimeInMillis(1736937045123L);

            assertEquals("2026-01-15 10:30:45", Dates.format(Dates.addYears(cal, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC), tag);
            assertEquals("2025-02-15 10:30:45", Dates.format(Dates.addMonths(cal, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC), tag);
            assertEquals("2025-01-16 10:30:45", Dates.format(Dates.addDays(cal, 1), Dates.LOCAL_DATE_TIME_FORMAT, UTC), tag);
        }
    }

    // ------------------------------------------------------------------------------------------
    // B3 - a locale selects text, never the calendar system
    // ------------------------------------------------------------------------------------------

    /**
     * {@code sun.util.BuddhistCalendar} extends {@code GregorianCalendar}, so the old
     * {@code instanceof} test kept it and a Thai-Buddhist locale printed 2568 for 2025 from a custom
     * pattern, while a Japanese-imperial locale - whose calendar is not a {@code GregorianCalendar} -
     * was normalized. Both now agree with {@link Dates.DTF}.
     */
    @Test
    public void aLocaleSelectsTextNotTheCalendarSystem() {
        final java.util.Date date = new java.util.Date(1736937045123L);

        for (final String tag : new String[] { "th-TH-u-ca-buddhist", "ja-JP-u-ca-japanese", "en-US", "de-DE" }) {
            final Locale locale = Locale.forLanguageTag(tag);

            assertEquals("2025/01/15", Dates.format(date, "yyyy/MM/dd", UTC, locale), tag);
            assertEquals(Dates.DTF.of("yyyy/MM/dd", locale).format(date), Dates.format(date, "yyyy/MM/dd", UTC, locale), tag + " vs DTF");
            assertEquals(1736899200000L, Dates.parseToJUDate("2025/01/15", "yyyy/MM/dd", UTC, locale).getTime(), tag + " parse");
        }

        // Text is still the locale's.
        assertEquals("15 Januar 2025", Dates.format(date, "dd MMMM yyyy", UTC, Locale.GERMAN));
        assertEquals("15 January 2025", Dates.format(date, "dd MMMM yyyy", UTC, Locale.US));
    }

    /** The same must hold when the non-Gregorian calendar comes from the default locale. */
    @Test
    public void aNonGregorianDefaultLocaleDoesNotLeakIntoFormatting() {
        Locale.setDefault(Locale.forLanguageTag("th-TH-u-ca-buddhist"));
        final java.util.Date date = new java.util.Date(1736937045123L);

        assertEquals("2025/01/15", Dates.format(date, "yyyy/MM/dd", UTC, Locale.getDefault()));
        assertEquals("2025-01-15 10:30:45", Dates.format(date, Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertEquals("2025-01-15T10:30:45Z", Dates.format(date));
    }

    // ------------------------------------------------------------------------------------------
    // D3 - diagnostics
    // ------------------------------------------------------------------------------------------

    /** The failure must name the pattern the caller passed, not the internal proleptic one. */
    @Test
    public void parseFailuresNameThePatternTheCallerPassed() {
        for (final Supplier<Object> call : List.<Supplier<Object>> of(() -> Dates.parseToLocalDate("2025-1-15", Dates.LOCAL_DATE_FORMAT),
                () -> Dates.parseToLocalDateTime("2025-1-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT),
                () -> Dates.parseToInstant("2025-1-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT))) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call::get);

            assertTrue(e.getMessage().contains("yyyy"), "should name the caller's pattern: " + e.getMessage());
            assertTrue(!e.getMessage().contains("uuuu"), "should not leak the internal pattern: " + e.getMessage());
        }
    }

    /** A predefined pattern that cannot supply the requested civil fields gets this class's message. */
    @Test
    public void aPartialPredefinedPatternIsRejectedWithThisClassesOwnMessage() {
        final IllegalArgumentException yearOnly = assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate("2023", Dates.LOCAL_YEAR_FORMAT));
        assertTrue(yearOnly.getMessage().contains("does not contain a complete local date"), yearOnly.getMessage());
        assertTrue(!yearOnly.getMessage().contains("TemporalAccessor"), yearOnly.getMessage());

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate("12-25", Dates.LOCAL_MONTH_DAY_FORMAT)).getMessage()
                .contains("does not contain a complete local date"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate("14:30:45", Dates.LOCAL_TIME_FORMAT)).getMessage()
                .contains("does not contain a complete local date"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalTime("2025-01-15", Dates.LOCAL_DATE_FORMAT)).getMessage()
                .contains("does not contain a time"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime("2025-01-15", Dates.LOCAL_DATE_FORMAT)).getMessage()
                .contains("does not contain a time"));

        // Auto-detection reaches the same check.
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate("14:30:45")).getMessage()
                .contains("does not contain a complete local date"));

        // A pattern that does carry the fields is untouched.
        assertEquals(LocalDate.of(2025, 1, 15), Dates.parseToLocalDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals(LocalDate.of(2025, 1, 15), Dates.parseToLocalDate("2025-01-15", Dates.LOCAL_DATE_FORMAT));
    }

    // ------------------------------------------------------------------------------------------
    // D5 - the week-based year
    // ------------------------------------------------------------------------------------------

    /**
     * {@code SimpleDateFormat} resolves {@code "YYYY-MM-dd"} from its <i>unset</i> {@code YEAR} field,
     * so "2025-01-15" was read as 1970-01-15 while the completeness check reported the pattern as a
     * complete date.
     */
    @Test
    public void weekBasedYearAloneIsNotACompleteDate() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15", "YYYY-MM-dd", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15", "YYYY-MM-dd", UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar("2025-01-15", "YYYY-MM-dd", UTC));

        // A genuine week date still parses: week-based year plus week-of-year plus day-of-week.
        assertEquals("2025-01-15T00:00:00Z", Dates.format(Dates.parseToJUDate("2025-W03-3", "YYYY-'W'ww-u", UTC), Dates.ISO_8601_DATE_TIME_FORMAT));

        // And the plain year field is unaffected.
        assertEquals("2025-01-15T00:00:00Z", Dates.format(Dates.parseToJUDate("2025-01-15", "yyyy-MM-dd", UTC), Dates.ISO_8601_DATE_TIME_FORMAT));
    }

    // ------------------------------------------------------------------------------------------
    // B4 - the documented format/parse asymmetry of the zone-less constants
    // ------------------------------------------------------------------------------------------

    /**
     * Characterization: a zone-less constant carries no offset, so its own rendering of an instant can
     * be a local date-time a daylight-saving overlap repeats or a gap removes, which parsing then
     * refuses. Documented on the five constants rather than changed.
     */
    @Test
    public void aZoneLessConstantCanWriteTextItRefusesToRead() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        final java.util.Date overlap = new java.util.Date(Instant.parse("2025-11-02T05:30:00Z").toEpochMilli());
        final String overlapText = Dates.format(overlap, Dates.LOCAL_DATE_TIME_FORMAT);

        assertEquals("2025-11-02 01:30:00", overlapText);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(overlapText, Dates.LOCAL_DATE_TIME_FORMAT)).getMessage()
                .contains("DST overlap"));

        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        final java.util.Date gapDay = new java.util.Date(Instant.parse("2018-11-04T05:00:00Z").toEpochMilli());
        final String gapText = Dates.format(gapDay, Dates.LOCAL_DATE_FORMAT);

        assertEquals("2018-11-04", gapText);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(gapText, Dates.LOCAL_DATE_FORMAT)).getMessage().contains("DST gap"));

        // The offset-bearing constants round-trip in the same zone.
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        assertEquals(overlap.getTime(),
                Dates.parseToJUDate(Dates.format(overlap, Dates.ISO_OFFSET_DATE_TIME_FORMAT), Dates.ISO_OFFSET_DATE_TIME_FORMAT).getTime());
    }

    // ------------------------------------------------------------------------------------------
    // preserved behaviour that the rewritten fixed-shape parser must not have changed
    // ------------------------------------------------------------------------------------------

    /** The five fixed-shape constants keep their canonical-shape and range rejections. */
    @Test
    public void theFixedShapeParserKeepsItsShapeAndRangeRejections() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-1-15", Dates.LOCAL_DATE_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-02-30", Dates.LOCAL_DATE_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-13-01", Dates.LOCAL_DATE_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15 24:00:00", Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15 10:30:45 ", Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15 10:30:45.1", Dates.LOCAL_TIMESTAMP_FORMAT, UTC));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("14:3:45", Dates.LOCAL_TIME_FORMAT, UTC));

        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15T10:30:45", Dates.ISO_LOCAL_DATE_TIME_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15 10:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15T10:30:45.123", Dates.ISO_LOCAL_TIMESTAMP_FORMAT, UTC).getTime());
        assertEquals(1736899200000L, Dates.parseToJUDate("2025-01-15", Dates.LOCAL_DATE_FORMAT, UTC).getTime());
        assertEquals(52245000L, Dates.parseToTime("14:30:45", Dates.LOCAL_TIME_FORMAT, UTC).getTime());
    }

    /** A time-only value is still anchored to 1970-01-01 in the authoritative zone. */
    @Test
    public void aTimeOnlyValueIsStillAnchoredToTheEpochDate() {
        // 1970-01-01T14:30:45 in UTC.
        assertEquals(52_245_000L, Dates.parseToTime("14:30:45", Dates.LOCAL_TIME_FORMAT, UTC).getTime());
        // The same wall time in +05:30 is five and a half hours earlier as an instant.
        assertEquals(52_245_000L - (5 * 3_600_000L + 30 * 60_000L),
                Dates.parseToTime("14:30:45", Dates.LOCAL_TIME_FORMAT, TimeZone.getTimeZone("Asia/Kolkata")).getTime());

        TimeZone.setDefault(UTC);
        assertEquals(52_245_000L, Dates.parseToTime("14:30:45").getTime());
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        assertEquals(52_245_000L - (5 * 3_600_000L + 30 * 60_000L), Dates.parseToTime("14:30:45").getTime());
    }

    /** DST gap and overlap diagnostics keep naming the local date-time, not the parser. */
    @Test
    public void dstGapAndOverlapDiagnosticsAreUnchanged() {
        final TimeZone newYork = TimeZone.getTimeZone("America/New_York");

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-03-09 02:30:00", Dates.LOCAL_DATE_TIME_FORMAT, newYork))
                .getMessage()
                .contains("Nonexistent local date-time 2025-03-09T02:30"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-11-02 01:30:00", Dates.LOCAL_DATE_TIME_FORMAT, newYork))
                .getMessage()
                .contains("Ambiguous local date-time 2025-11-02T01:30"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-11-02 01:30:00.000", Dates.LOCAL_TIMESTAMP_FORMAT, newYork))
                .getMessage()
                .contains("Ambiguous local date-time"));
    }

    /** {@code DTF.of} still returns the shared constant for a proleptic pattern. */
    @Test
    public void dtfConstantsAreStillShared() {
        assertSame(Dates.DTF.LOCAL_DATE, Dates.DTF.of("uuuu-MM-dd"));
        assertSame(Dates.DTF.LOCAL_DATE, Dates.DTF.of("uuuu-MM-dd", Locale.US));
    }

    /**
     * The pre-1900 seam the class javadoc and {@code legacyRenderingZone} describe, pinned through the
     * library's own two engines rather than through a bundled tzdb constant: the legacy table answers the
     * zone's present raw offset, the class prints the {@code java.time} local mean time, and the
     * +05:53:28 regime the javadoc dates to 1854 is already over by 1880.
     */
    @Test
    public void testPreNineteenHundredKolkataRendersTheJavaTimeLocalMeanTime() {
        final ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        final TimeZone legacy = TimeZone.getTimeZone("Asia/Kolkata");
        final long millis1880 = Instant.parse("1880-01-01T00:00:00Z").toEpochMilli();

        assertEquals(legacy.getRawOffset(), legacy.getOffset(millis1880));

        final ZonedDateTime zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis1880), kolkata);

        assertEquals(Dates.DTF.of("uuuu-MM-dd HH:mm:ss").format(zoned), Dates.format(new java.util.Date(millis1880), Dates.LOCAL_DATE_TIME_FORMAT, legacy));

        final ZoneOffset offset1880 = kolkata.getRules().getOffset(Instant.ofEpochMilli(millis1880));

        assertNotEquals(kolkata.getRules().getOffset(Instant.parse("1853-01-01T00:00:00Z")), offset1880);
        assertEquals(offset1880, kolkata.getRules().getOffset(Instant.parse("1875-01-01T00:00:00Z")));
        assertEquals(offset1880, kolkata.getRules().getOffset(Instant.parse("1899-12-31T00:00:00Z")));
        assertNotEquals(offset1880, kolkata.getRules().getOffset(Instant.parse("1910-01-01T00:00:00Z")));
    }

    /**
     * The javadoc no longer promises that a custom pattern round-trips with itself: a zone-less custom
     * pattern writes the repeated wall clock of a fall-back overlap and reads it back as the standard-time
     * pass, where a predefined zone-less constant rejects the ambiguity outright.
     */
    @Test
    public void testAZoneLessCustomPatternReadsAnOverlapBackAsTheStandardPass() {
        final String custom = "yyyy/MM/dd HH:mm:ss.SSS";
        final ZoneId zone = ZoneId.of("America/New_York");
        final TimeZone timeZone = TimeZone.getTimeZone(zone);

        ZoneOffsetTransition overlap = zone.getRules().nextTransition(Instant.parse("2020-06-01T00:00:00Z"));

        while (!overlap.isOverlap()) {
            overlap = zone.getRules().nextTransition(overlap.getInstant());
        }

        final long shift = -overlap.getDuration().toMillis();
        final long daylightPass = overlap.getInstant().toEpochMilli() - shift / 2;
        final String text = Dates.format(new java.util.Date(daylightPass), custom, timeZone);

        assertEquals(daylightPass + shift, Dates.parseToJUDate(text, custom, timeZone).getTime());
        assertEquals(text, Dates.format(new java.util.Date(daylightPass + shift), custom, timeZone));
        assertEquals(daylightPass + shift, Dates.parseToJUDate(Dates.format(new java.util.Date(daylightPass + shift), custom, timeZone), custom, timeZone)
                .getTime());

        final String predefined = Dates.format(new java.util.Date(daylightPass), Dates.LOCAL_TIMESTAMP_FORMAT, timeZone);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(predefined, Dates.LOCAL_TIMESTAMP_FORMAT, timeZone)).getMessage()
                .contains("DST overlap"));
    }

    /**
     * The other half of the same javadoc clause: RFC 822 {@code Z} has no seconds field, so a custom
     * pattern cannot write an offset that has one and the text reads back off by that remainder. The zone
     * and instant are searched for at runtime, so no offset value is hard-coded.
     */
    @Test
    public void testACustomZPatternCannotWriteAnOffsetWithASecondsComponent() {
        final String custom = "yyyy-MM-dd HH:mm:ss.SSSZ";
        int checked = 0;

        for (final String zoneId : new String[] { "Africa/Monrovia", "America/St_Johns", "Asia/Kolkata", "Europe/Dublin" }) {
            final ZoneId zone = ZoneId.of(zoneId);
            final TimeZone timeZone = TimeZone.getTimeZone(zone);

            for (int year = 1925; year <= 1970 && checked == 0; year++) {
                final Instant instant = Instant.parse(year + "-06-15T12:00:00Z");
                final int offsetSeconds = zone.getRules().getOffset(instant).getTotalSeconds();

                if (offsetSeconds % 60 == 0) {
                    continue;
                }

                final long millis = instant.toEpochMilli();
                final String text = Dates.format(new java.util.Date(millis), custom, timeZone);

                assertEquals(millis + (offsetSeconds - offsetSeconds / 60 * 60) * 1000L, Dates.parseToJUDate(text, custom, timeZone).getTime());
                checked++;
            }
        }

        assertEquals(1, checked);
    }
}
