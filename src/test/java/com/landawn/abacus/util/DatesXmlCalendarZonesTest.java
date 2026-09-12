package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the findings of the 2026-08-30d line-by-line review of {@link Dates}.
 *
 * <ul>
 *   <li><b>B1</b> {@code createXMLGregorianCalendar}/{@code currentXMLGregorianCalendar} silently moved
 *       the instant when the effective zone offset was not a whole number of minutes.</li>
 *   <li><b>B2</b> the legacy {@code parseTo*} targets accepted non-canonical field widths that the
 *       {@code java.time} targets rejected for the same predefined constant.</li>
 *   <li><b>B3</b> the no-zone civil queries threw {@code ZoneRulesException} rather than the
 *       {@code IllegalArgumentException} they document.</li>
 *   <li><b>O1</b> {@code set*} leaked {@code Calendar}'s bare field-name message.</li>
 *   <li><b>O2</b> the {@code add*} overflow message named an intermediate instant as the input.</li>
 *   <li><b>O4</b> {@code fastDateFormat} took its pooled buffer outside the try/finally.</li>
 * </ul>
 */
public class DatesXmlCalendarZonesTest extends TestBase {

    /** 2025-01-15T10:30:45.123Z. */
    private static final long SAMPLE_MILLIS = 1736937045123L;

    /** 1938-04-24T22:13:20Z, while Africa/Monrovia still stood at -00:44:30. */
    private static final long MONROVIA_MILLIS = -1000000000000L;

    private TimeZone originalDefault;

    @BeforeEach
    public void setUp() {
        originalDefault = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
    }

    @AfterEach
    public void tearDown() {
        TimeZone.setDefault(originalDefault);
    }

    // -----------------------------------------------------------------------------------------
    // B1 - XMLGregorianCalendar cannot store a sub-minute zone offset, so the instant used to move
    // -----------------------------------------------------------------------------------------

    @Test
    public void b1_createXMLGregorianCalendar_rejectsSubMinuteOffsetZoneInsteadOfMovingTheInstant() {
        // Pre-fix: the local civil fields were kept and the XML timezone field was rounded to 00:00,
        // so the value came back 30 s LATER than the instant that was asked for.
        final TimeZone plus30s = new SimpleTimeZone(30_000, "plus30s");
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(SAMPLE_MILLIS, plus30s));
        assertTrue(e.getMessage().contains("whole-minute offset"), e.getMessage());

        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(SAMPLE_MILLIS, new SimpleTimeZone(-30_000, "minus30s")));
        assertThrows(IllegalArgumentException.class,
                () -> Dates.createXMLGregorianCalendar(SAMPLE_MILLIS, new SimpleTimeZone(5 * 3600_000 + 30 * 60_000 + 20_000, "odd")));
    }

    @Test
    public void b1_createXMLGregorianCalendar_rejectsARealHistoricalSubMinuteZone() {
        // Africa/Monrovia really was -00:44:30 until 1972, so this is reachable with stock tzdb data.
        final TimeZone monrovia = TimeZone.getTimeZone("Africa/Monrovia");
        assertEquals(-2670000, monrovia.getOffset(MONROVIA_MILLIS));

        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(MONROVIA_MILLIS, monrovia));

        // The parse side already rejected the same zone; the two sides now agree.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar("1938-04-24 21:28:50", Dates.LOCAL_DATE_TIME_FORMAT, monrovia));
    }

    @Test
    public void b1_everyCreateOverloadAndCurrentAreGuarded() {
        final TimeZone plus30s = new SimpleTimeZone(30_000, "plus30s");

        final Calendar calendar = Calendar.getInstance(plus30s);
        calendar.setTimeInMillis(SAMPLE_MILLIS);
        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(calendar));

        TimeZone.setDefault(plus30s);
        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(SAMPLE_MILLIS));
        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(new Date(SAMPLE_MILLIS)));
        assertThrows(IllegalArgumentException.class, Dates::currentXMLGregorianCalendar);
        assertThrows(IllegalArgumentException.class, () -> Dates.currentXMLGregorianCalendarPlus(1, java.util.concurrent.TimeUnit.HOURS));
    }

    @Test
    public void b1_offsetsBeyondFourteenHoursReportThisClassesOwnMessage() {
        // Pre-fix this escaped as the JDK's "Invalid value 870 for Timezone field."
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dates.createXMLGregorianCalendar(SAMPLE_MILLIS, TimeZone.getTimeZone("GMT+14:30")));
        assertTrue(e.getMessage().contains("-14:00 through +14:00"), e.getMessage());

        assertThrows(IllegalArgumentException.class, () -> Dates.createXMLGregorianCalendar(SAMPLE_MILLIS, TimeZone.getTimeZone("GMT-14:30")));
    }

    @Test
    public void b1_representableZonesStillPreserveTheInstantExactly() {
        for (final String id : new String[] { "UTC", "Asia/Kolkata", "Australia/Lord_Howe", "GMT+14:00", "GMT-14:00", "Pacific/Kiritimati" }) {
            final XMLGregorianCalendar value = Dates.createXMLGregorianCalendar(SAMPLE_MILLIS, TimeZone.getTimeZone(id));
            assertEquals(SAMPLE_MILLIS, value.toGregorianCalendar().getTimeInMillis(), id);
        }

        assertEquals(0L, Dates.createXMLGregorianCalendar(0L, TimeZone.getTimeZone("UTC")).toGregorianCalendar().getTimeInMillis());
    }

    @Test
    public void b1_everyTzdbZoneAtAModernInstantIsRepresentableAndExact() {
        // No modern zone has a sub-minute offset, so the guard must not reject anything in normal use.
        for (final String id : ZoneId.getAvailableZoneIds()) {
            final XMLGregorianCalendar value = Dates.createXMLGregorianCalendar(SAMPLE_MILLIS, TimeZone.getTimeZone(id));
            assertEquals(SAMPLE_MILLIS, value.toGregorianCalendar().getTimeInMillis(), id);
        }
    }

    @Test
    public void b1_currentXMLGregorianCalendarStillReportsNow() {
        final long before = System.currentTimeMillis();
        final XMLGregorianCalendar now = Dates.currentXMLGregorianCalendar();
        final long after = System.currentTimeMillis();
        final long value = now.toGregorianCalendar().getTimeInMillis();

        assertTrue(value >= before - 1000 && value <= after + 1000, "expected " + value + " within [" + before + ", " + after + "]");
    }

    // -----------------------------------------------------------------------------------------
    // B2 - one predefined constant must mean one grammar on every entry point
    // -----------------------------------------------------------------------------------------

    /** {format, non-canonical text, the canonical spelling of the same value}. */
    private static final String[][] WIDTH_CASES = { //
            { Dates.LOCAL_DATE_FORMAT, "2025-1-15", "2025-01-15" }, //
            { Dates.LOCAL_DATE_FORMAT, "2025-01-5", "2025-01-05" }, //
            { Dates.LOCAL_TIME_FORMAT, "1:30:45", "01:30:45" }, //
            { Dates.LOCAL_TIME_FORMAT, "10:3:45", "10:03:45" }, //
            { Dates.LOCAL_DATE_TIME_FORMAT, "2025-01-15 1:30:45", "2025-01-15 01:30:45" }, //
            { Dates.LOCAL_DATE_TIME_FORMAT, "2025-1-15 10:30:45", "2025-01-15 10:30:45" }, //
            { Dates.ISO_LOCAL_DATE_TIME_FORMAT, "2025-01-15T1:30:45", "2025-01-15T01:30:45" }, //
            { Dates.ISO_LOCAL_DATE_TIME_FORMAT, "2025-1-15T10:30:45", "2025-01-15T10:30:45" }, //
            { Dates.ISO_8601_DATE_TIME_FORMAT, "2025-1-15T10:30:45Z", "2025-01-15T10:30:45Z" }, //
            { Dates.ISO_8601_DATE_TIME_FORMAT, "2025-01-15T1:30:45Z", "2025-01-15T01:30:45Z" } };

    @Test
    public void b2_legacyTargetsRejectNonCanonicalFieldWidths() {
        for (final String[] testCase : WIDTH_CASES) {
            final String format = testCase[0];
            final String text = testCase[1];
            final boolean timeOnly = Dates.LOCAL_TIME_FORMAT.equals(format);

            if (timeOnly) {
                // A time-only pattern only reaches parseToTime; the date-bearing targets reject it earlier.
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime(text, format), text);
            } else {
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, format), text);
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(text, format), text);
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(text, format), text);
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(text, format), text);
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToGregorianCalendar(text, format), text);
                assertThrows(IllegalArgumentException.class, () -> Dates.parseToXMLGregorianCalendar(text, format), text);
            }
        }
    }

    @Test
    public void b2_theCanonicalSpellingOfEachCaseIsStillAccepted() {
        for (final String[] testCase : WIDTH_CASES) {
            final String format = testCase[0];
            final String canonical = testCase[2];

            if (Dates.LOCAL_TIME_FORMAT.equals(format)) {
                assertNotNull(Dates.parseToTime(canonical, format), canonical);
                assertNotNull(Dates.parseToLocalTime(canonical, format), canonical);
            } else {
                final Date legacy = Dates.parseToJUDate(canonical, format);
                assertNotNull(legacy, canonical);
                // The java.time target that reads the same fields must agree on the value.
                assertEquals(legacy.getTime(), Dates.parseToTimestamp(canonical, format).getTime(), canonical);
            }
        }
    }

    @Test
    public void b2_theJavaTimeTargetsRejectExactlyTheSameText() {
        // The whole point of the fix: the two engines no longer disagree about one named constant.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate("2025-1-15", Dates.LOCAL_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalTime("1:30:45", Dates.LOCAL_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDateTime("2025-01-15 1:30:45", Dates.LOCAL_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("2025-1-15T10:30:45Z", Dates.ISO_8601_DATE_TIME_FORMAT));
    }

    @Test
    public void b2_theRejectionMessageNamesTheMigrationPath() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-1-15", Dates.LOCAL_DATE_FORMAT));
        assertTrue(e.getMessage().contains("full width"), e.getMessage());
        assertTrue(e.getMessage().contains("yyyy-M-d"), e.getMessage());
    }

    @Test
    public void b2_variableWidthCustomPatternsAreUnaffected() {
        // The documented migration: a pattern that never promised fixed widths still accepts them.
        assertEquals(Dates.parseToJUDate("2025-01-15", "yyyy-M-d").getTime(), Dates.parseToJUDate("2025-1-15", "yyyy-M-d").getTime());
        assertEquals(Dates.parseToJUDate("2025-01-15 01:30:45", "yyyy-M-d H:m:s").getTime(),
                Dates.parseToJUDate("2025-1-15 1:30:45", "yyyy-M-d H:m:s").getTime());
    }

    @Test
    public void b2_formatOutputStillRoundTripsThroughTheStricterParser() {
        // format() always zero-pads, so nothing this class writes can be rejected by the new check.
        final Timestamp value = new Timestamp(SAMPLE_MILLIS);
        final TimeZone utc = TimeZone.getTimeZone("UTC");

        for (final String format : new String[] { Dates.LOCAL_DATE_FORMAT, Dates.LOCAL_DATE_TIME_FORMAT, Dates.ISO_LOCAL_DATE_TIME_FORMAT,
                Dates.ISO_8601_DATE_TIME_FORMAT }) {
            final String text = Dates.format(value, format, utc);
            assertNotNull(Dates.parseToJUDate(text, format, utc), format);
        }

        final String timeText = Dates.format(value, Dates.LOCAL_TIME_FORMAT, utc);
        assertNotNull(Dates.parseToTime(timeText, Dates.LOCAL_TIME_FORMAT, utc));
    }

    @Test
    public void b2_theFractionAndOffsetConstantsKeepTheirOwnExistingChecks() {
        // These were already width-checked elsewhere; confirm the new check did not weaken them.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15 10:30:45.1", Dates.LOCAL_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2025-01-15T10:30:45.1Z", Dates.ISO_8601_TIMESTAMP_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-01-15T10:30:45+5:30", Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("Wed, 5 Jan 2025 10:30:45 GMT", Dates.HTTP_DATE_FORMAT));

        // ...and that the canonical forms of those same constants still parse.
        assertEquals(SAMPLE_MILLIS, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z", Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        assertEquals(1736937045000L, Dates.parseToJUDate("Wed, 15 Jan 2025 10:30:45 GMT", Dates.HTTP_DATE_FORMAT).getTime());
    }

    @Test
    public void b2_autoDetectionIsUnchanged() {
        // Auto-detection already keyed on the exact length, so it must behave exactly as before.
        assertEquals(SAMPLE_MILLIS, Dates.parseToTimestamp("2025-01-15T10:30:45.123Z").getTime());
        assertEquals(1736937045000L, Dates.parseToJUDate("2025-01-15T10:30:45Z").getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2025-1-15"));
    }

    // -----------------------------------------------------------------------------------------
    // B3 - the no-zone civil queries must reject an unknown default zone the way the rest of the
    //      class rejects an unusable one: with an IllegalArgumentException
    // -----------------------------------------------------------------------------------------

    @Test
    public void b3_anUnknownDefaultZoneIdIsAnIllegalArgumentExceptionNotAZoneRulesException() {
        // ZoneId.systemDefault() throws ZoneRulesException (a DateTimeException) for an ID java.time
        // does not know; every other zone rejection in Dates is an IllegalArgumentException.
        // Since the 2026-09-01 pass a fixed-offset default is accepted under any ID (the class-level
        // policy), so the unknown zone here must carry daylight-saving rules to be unusable.
        TimeZone.setDefault(new SimpleTimeZone(-5 * 3600_000, "NoSuchZoneId", Calendar.MARCH, 8, -Calendar.SUNDAY, 2 * 3600_000, Calendar.NOVEMBER, 1,
                -Calendar.SUNDAY, 2 * 3600_000, 3600_000));
        final Date date = new Date(SAMPLE_MILLIS);

        for (final Runnable call : new Runnable[] { //
                () -> Dates.isLastDayOfMonth(date), //
                () -> Dates.isLastDayOfYear(date), //
                () -> Dates.lengthOfMonth(date), //
                () -> Dates.lengthOfYear(date), //
                () -> Dates.isSameDay(date, date), //
                () -> Dates.isSameMonth(date, date), //
                () -> Dates.isSameYear(date, date) }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call::run);
            assertTrue(e.getMessage().contains("NoSuchZoneId"), e.getMessage());
            assertInstanceOf(java.time.DateTimeException.class, e.getCause());
        }

        // the same unknown ID with no daylight-saving rules is a fixed offset, and is accepted
        TimeZone.setDefault(new SimpleTimeZone(-5 * 3600_000, "NoSuchZoneId"));
        assertTrue(assertDoesNotThrow(() -> Dates.isSameDay(date, date)));
        assertEquals(31, Dates.lengthOfMonth(date));
    }

    @Test
    public void b3_aCustomRuleZoneWithAKnownIdIsStillAcceptedAndUsesThatIdsRules() {
        // Documented behaviour: these are civil-field queries, so ID-derived rules are good enough.
        TimeZone.setDefault(new SimpleTimeZone(-5 * 3600_000, "America/New_York", Calendar.MARCH, 8, -Calendar.SUNDAY, 2 * 3600_000, Calendar.NOVEMBER, 1,
                -Calendar.SUNDAY, 2 * 3600_000, 3600_000));
        final Date endOfJanuary = Dates.parseToJUDate("2025-01-31T12:00:00Z");

        assertTrue(assertDoesNotThrow(() -> Dates.isLastDayOfMonth(endOfJanuary)));
        assertEquals(31, Dates.lengthOfMonth(endOfJanuary));
        assertEquals(365, Dates.lengthOfYear(endOfJanuary));
        assertFalse(Dates.isLastDayOfYear(endOfJanuary));
        assertTrue(Dates.isSameDay(endOfJanuary, endOfJanuary));
        assertTrue(Dates.isSameMonth(endOfJanuary, endOfJanuary));
        assertTrue(Dates.isSameYear(endOfJanuary, endOfJanuary));
    }

    @Test
    public void b3_ordinaryDefaultZonesAreUnaffected() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        final Date value = Dates.parseToJUDate("2024-02-29T12:00:00Z");

        assertTrue(Dates.isLastDayOfMonth(value));
        assertEquals(29, Dates.lengthOfMonth(value));
        assertEquals(366, Dates.lengthOfYear(value));
        assertFalse(Dates.isLastDayOfYear(value));
    }

    // -----------------------------------------------------------------------------------------
    // O1 - set* must not leak Calendar's bare field-name message
    // -----------------------------------------------------------------------------------------

    @Test
    public void o1_setMethodsReportTheCallTheValueAndTheSourceInstant() {
        final Date leapDay = Dates.parseToJUDate("2024-02-29T12:00:00Z");

        final IllegalArgumentException days = assertThrows(IllegalArgumentException.class, () -> Dates.setDays(leapDay, 31));
        assertTrue(days.getMessage().contains("Dates.setDays(date, 31)"), days.getMessage());
        assertTrue(days.getMessage().contains("2024-02-29T12:00:00Z"), days.getMessage());
        assertTrue(days.getMessage().contains("America/New_York"), days.getMessage());
        // The JDK's own diagnostic is kept as the cause rather than discarded.
        assertInstanceOf(IllegalArgumentException.class, days.getCause());

        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.setMonths(leapDay, 12)).getMessage().contains("Dates.setMonths(date, 12)"));
        // 0 is now the proleptic ISO year 1 BCE; Integer.MIN_VALUE is the value setYears cannot express.
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.setYears(leapDay, Integer.MIN_VALUE)).getMessage()
                .contains("Dates.setYears(date, -2147483648)"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.setHours(leapDay, 24)).getMessage().contains("Dates.setHours(date, 24)"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.setMinutes(leapDay, 60)).getMessage().contains("Dates.setMinutes(date, 60)"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.setSeconds(leapDay, 60)).getMessage().contains("Dates.setSeconds(date, 60)"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.setMilliseconds(leapDay, 1000)).getMessage()
                .contains("Dates.setMilliseconds(date, 1000)"));
    }

    @Test
    public void o1_theSuccessfulSetPathsAreUnchanged() {
        final Date leapDay = Dates.parseToJUDate("2024-02-29T12:00:00Z");

        // YEAR and MONTH still clamp an impossible day-of-month rather than rolling over.
        assertEquals("2025-02-28T12:00:00Z", Dates.format(Dates.setYears(leapDay, 2025)));
        assertEquals("2024-03-29T11:00:00Z", Dates.format(Dates.setMonths(leapDay, Calendar.MARCH)));
        assertEquals("2024-02-28T12:00:00Z", Dates.format(Dates.setDays(leapDay, 28)));
        assertEquals("2024-02-29T15:00:00Z", Dates.format(Dates.setHours(leapDay, 10)));

        // The source is never mutated.
        assertEquals(Dates.parseToJUDate("2024-02-29T12:00:00Z").getTime(), leapDay.getTime());
    }

    // -----------------------------------------------------------------------------------------
    // O2 - the overflow message must name the instant the caller passed
    // -----------------------------------------------------------------------------------------

    @Test
    public void o2_addOverflowMessageNamesTheCallersInstantNotAChunkBoundary() {
        final Date epoch = new Date(0L);
        final ArithmeticException e = assertThrows(ArithmeticException.class, () -> Dates.addYears(epoch, Integer.MAX_VALUE));
        assertTrue(e.getMessage().contains("epoch millis 0"), e.getMessage());
        assertTrue(e.getMessage().contains("2147483647 YEAR"), e.getMessage());

        final Calendar calendar = Dates.createCalendar(0L);
        assertTrue(assertThrows(ArithmeticException.class, () -> Dates.addYears(calendar, Integer.MAX_VALUE)).getMessage().contains("epoch millis 0"));

        // A non-zero start is reported as itself too.
        final Date sample = new Date(SAMPLE_MILLIS);
        assertTrue(assertThrows(ArithmeticException.class, () -> Dates.addYears(sample, Integer.MAX_VALUE)).getMessage()
                .contains("epoch millis " + SAMPLE_MILLIS));
    }

    @Test
    public void o2_amountsThatDoNotOverflowStillSucceed() {
        // Both values are the ZonedDateTime.plus* result for the same civil target: +178958940-07-31T19:00-04:00
        // and +5881580-07-10T19:00-04:00 in America/New_York. The month value is NOT the day-30 instant the
        // pre-r9516 chunking produced, when an intermediate 100-million-month chunk clamped 12-31 to 04-30.
        assertEquals(5647336530735600000L, Dates.addMonths(new Date(0L), Integer.MAX_VALUE).getTime());
        assertEquals(185542587097200000L, Dates.addDays(new Date(0L), Integer.MAX_VALUE).getTime());
    }

    // -----------------------------------------------------------------------------------------
    // O4 - the pooled formatting buffer must survive a rejected value
    // -----------------------------------------------------------------------------------------

    @Test
    public void o4_aRejectedInstantDoesNotDisturbTheDefaultFormattingBuffer() {
        // +10000-01-01Z is outside the four-digit ISO year range the default format promises.
        final Date outOfRange = new Date(253402300800000L);
        for (int i = 0; i < 200; i++) {
            assertThrows(IllegalArgumentException.class, () -> Dates.format(outOfRange));
        }

        // The pool is POOL_SIZE-bounded; if a rejected call had consumed a buffer without returning it,
        // this would still work but allocate - so assert the observable contract instead: output stays
        // correct after many rejections interleaved with successes.
        for (int i = 0; i < 200; i++) {
            assertEquals("2025-01-15T10:30:45.123Z", Dates.format(new Timestamp(SAMPLE_MILLIS)));
            assertEquals("2025-01-15T10:30:45Z", Dates.format(new Date(SAMPLE_MILLIS)));
            assertThrows(IllegalArgumentException.class, () -> Dates.format(outOfRange));
        }
    }

    // -----------------------------------------------------------------------------------------
    // O3 - the HTTP-date grammar is reached through the non-deprecated DTF constant
    // -----------------------------------------------------------------------------------------

    @Test
    public void o3_httpDateStillParsesAndFormatsThroughTheNonDeprecatedConstant() {
        final String text = "Wed, 15 Jan 2025 10:30:45 GMT";
        assertEquals(1736937045000L, Dates.parseToJUDate(text, Dates.HTTP_DATE_FORMAT).getTime());
        assertEquals(text, Dates.format(new Date(1736937045000L), Dates.HTTP_DATE_FORMAT));
        assertEquals(Instant.ofEpochMilli(1736937045000L), Dates.DTF.HTTP_DATE.parseToInstant(text));
        assertEquals(text, Dates.DTF.HTTP_DATE.format(Instant.ofEpochMilli(1736937045000L)));
    }
}
