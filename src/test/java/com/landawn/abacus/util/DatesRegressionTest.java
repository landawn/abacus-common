package com.landawn.abacus.util;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import testfixtures.dates.CreatorTypes.AliasingCalendar;
import testfixtures.dates.CreatorTypes.ChildCalendar;
import testfixtures.dates.CreatorTypes.CustomDate;
import testfixtures.dates.CreatorTypes.NoisyTimestamp;
import testfixtures.dates.CreatorTypes.ParentCalendar;
import testfixtures.dates.CreatorTypes.WrongMillisDate;

/** Regression tests for confirmed defects from the 2026-08 API reviews of {@link Dates}. */
public class DatesRegressionTest extends TestBase {

    public static final class SelfCloningCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        @Override
        public Object clone() {
            return this;
        }
    }

    /**
     * A hostile {@code clone()} and no {@code (long)} or no-arg constructor, so {@code clone()} is the
     * only strategy left for building a result of this type.
     */
    public static final class SelfCloningCalendarWithoutConstructor extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        public SelfCloningCalendarWithoutConstructor(final String unusedMarker) {
            super();
        }

        @Override
        public Object clone() {
            return this;
        }
    }

    public static final class IgnoringDateConstructor extends Date {
        private static final long serialVersionUID = 1L;

        public IgnoringDateConstructor(final long ignoredMillis) {
            super(0L);
        }
    }

    public static final class IgnoringCalendarConstructor extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        public IgnoringCalendarConstructor(final long ignoredMillis) {
            setTimeInMillis(0L);
        }
    }

    public static final class RejectingFieldAddCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        @Override
        public void add(final int field, final int amount) {
            throw new AssertionError("Calendar.add must not be used for fixed-duration arithmetic");
        }
    }

    // ------------------------------------------------------------------
    // #1: UTC fast parse must not be contaminated by earlier formatting.
    // ------------------------------------------------------------------

    @Test
    public void utcFastParse_rejectsFeb30_afterFormatting() {
        // Exercise the (formerly pooled) UTC format path first; a recycled lenient calendar
        // used to make the subsequent strict parse roll Feb 30 over to March 2.
        Dates.format(new Date(0L), Dates.ISO_8601_DATE_TIME_FORMAT);

        assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2023-02-30T00:00:00", Dates.ISO_LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
    }

    @Test
    public void utcFastParse_rejectsFeb30_beforeFormatting_reversedOrder() {
        assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2023-02-30T00:00:00", Dates.ISO_LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC")));

        assertEquals("1970-01-01T00:00:00Z", Dates.format(new Date(0L), Dates.ISO_8601_DATE_TIME_FORMAT));
    }

    @Test
    public void fastParse_sameIdZonesWithDifferentRules_differByOneHour() {
        final SimpleTimeZone zero = new SimpleTimeZone(0, "SAME-ID");
        final SimpleTimeZone plusOne = new SimpleTimeZone(3600_000, "SAME-ID");

        final long atZero = Dates.parseToJUDate("2023-12-25 10:00:00", Dates.LOCAL_DATE_TIME_FORMAT, zero).getTime();
        final long atPlusOne = Dates.parseToJUDate("2023-12-25 10:00:00", Dates.LOCAL_DATE_TIME_FORMAT, plusOne).getTime();

        assertEquals(3600_000L, atZero - atPlusOne, "zones with equal IDs but different rules must produce different instants");
    }

    @Test
    public void concurrentFormatAndParse_doNotInterfere() throws Exception {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");
        final ExecutorService pool = Executors.newFixedThreadPool(8);

        try {
            final List<Callable<Boolean>> tasks = new ArrayList<>();

            for (int i = 0; i < 200; i++) {
                tasks.add(() -> "1970-01-01T00:00:00Z".equals(Dates.format(new Date(0L), Dates.ISO_8601_DATE_TIME_FORMAT, utc)));
                tasks.add(() -> Dates.parseToJUDate("2023-12-25 10:00:00", Dates.LOCAL_DATE_TIME_FORMAT, la).getTime() == 1703527200000L);
                tasks.add(() -> {
                    // Invalid input must keep failing under concurrency.
                    try {
                        Dates.parseToJUDate("2023-02-30T00:00:00", Dates.ISO_LOCAL_DATE_TIME_FORMAT, utc);
                        return false;
                    } catch (final IllegalArgumentException e) {
                        return true;
                    }
                });
            }

            for (final Future<Boolean> f : pool.invokeAll(tasks)) {
                assertTrue(f.get());
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------
    // #8: empty half-open ranges overlap nothing.
    // ------------------------------------------------------------------

    @Test
    public void isOverlapping_emptyRange_neverOverlaps() {
        assertFalse(Dates.isOverlapping(new Date(5), new Date(5), new Date(1), new Date(10)));
        assertFalse(Dates.isOverlapping(new Date(1), new Date(10), new Date(5), new Date(5)));
        assertFalse(Dates.isOverlapping(new Date(5), new Date(5), new Date(5), new Date(5)));
        assertTrue(Dates.isOverlapping(new Date(1), new Date(10), new Date(5), new Date(15)));
        assertFalse(Dates.isOverlapping(new Date(1), new Date(10), new Date(10), new Date(15))); // adjacent
    }

    @Test
    public void isOverlapping_calendar_emptyRange_neverOverlaps() {
        final Calendar s = Dates.createCalendar(5L), s2 = Dates.createCalendar(5L);
        final Calendar a = Dates.createCalendar(1L), b = Dates.createCalendar(10L);
        assertFalse(Dates.isOverlapping(s, s2, a, b));
        assertTrue(Dates.isOverlapping(a, b, Dates.createCalendar(5L), Dates.createCalendar(15L)));
    }

    // ------------------------------------------------------------------
    // #3: DTF resolves STRICT, not SMART.
    // ------------------------------------------------------------------

    @Test
    public void dtf_strictResolution_rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToLocalDate("2023-02-30"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToLocalDateTime("2023-12-25 24:00:00"));
    }

    @Test
    public void dtf_zoned_rejectsOffsetInconsistentWithZone() {
        // July in America/Los_Angeles is UTC-07:00; -08:00 must not be silently adjusted.
        assertThrows(IllegalArgumentException.class,
                () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-07-01T12:00:00-08:00[America/Los_Angeles]"));
    }

    @Test
    public void dtf_zoned_acceptsConsistentOffset() {
        final ZonedDateTime zdt = Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2023-12-25T14:30:45+05:30[Asia/Kolkata]");
        assertEquals(14, zdt.getHour());
        assertEquals(19800, zdt.getOffset().getTotalSeconds());
        assertEquals("Asia/Kolkata", zdt.getZone().getId());
    }

    // ------------------------------------------------------------------
    // #4: the pattern is authoritative for parse and format.
    // ------------------------------------------------------------------

    @Test
    public void dtf_numericPattern_notTreatedAsEpochMillis() {
        assertEquals(LocalDate.of(2023, 12, 25), Dates.DTF.of("yyyyMMdd").parseToLocalDate("20231225"));
    }

    @Test
    public void dtf_prolepticYearPattern_parsesAndFormats() {
        final Dates.DTF dtf = Dates.DTF.of("uuuu-MM-dd");
        assertEquals(LocalDate.of(2023, 12, 25), dtf.parseToLocalDate("2023-12-25"));
        assertEquals("2023-12-25", dtf.format(LocalDate.of(2023, 12, 25)));
    }

    @Test
    public void dtf_customPattern_preservesSuppliedOffset() {
        final OffsetDateTime odt = Dates.DTF.of("dd/MM/yyyy HH:mm XXX").parseToOffsetDateTime("25/12/2023 14:30 +05:30");
        assertEquals(19800, odt.getOffset().getTotalSeconds());
        assertEquals(14, odt.getHour());
        assertEquals(30, odt.getMinute());
    }

    // ------------------------------------------------------------------
    // #6: quoted 'Z' is literal text, not zone information.
    // ------------------------------------------------------------------

    @Test
    public void utcConstants_stillMeanUtc() {
        assertEquals("1970-01-01T00:00:00Z", Dates.format(new Date(0L), Dates.ISO_8601_DATE_TIME_FORMAT));
        assertEquals("1970-01-01T00:00:00Z", Dates.DTF.ISO_8601_DATE_TIME.format(Instant.EPOCH));
        assertEquals(Instant.EPOCH, Dates.DTF.ISO_8601_DATE_TIME.parseToInstant("1970-01-01T00:00:00Z"));
    }

    @Test
    public void customQuotedZPattern_notForcedToUtc() {
        // Trailing space defeats the old endsWith("'Z'") sniffing; now the pattern is never
        // zone-significant and the explicit zone is honored.
        final String s = Dates.format(new Date(0L), "yyyy-MM-dd'T'HH:mm:ss'Z' ", TimeZone.getTimeZone("America/Los_Angeles"));
        assertEquals("1969-12-31T16:00:00Z ", s);
    }

    @Test
    public void utcFormat_rejectsZoneLessTemporal() {
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_8601_DATE_TIME.format(LocalDateTime.of(2023, 12, 25, 10, 0, 0)));
        // Instant-bearing temporals remain formattable.
        assertEquals("1970-01-01T00:00:00Z", Dates.DTF.ISO_8601_DATE_TIME.format(Instant.EPOCH));
    }

    // ------------------------------------------------------------------
    // #11: subtype state survives arithmetic.
    // ------------------------------------------------------------------

    @Test
    public void addDays_preservesGregorianCutover() {
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setGregorianChange(new Date(Long.MIN_VALUE)); // proleptic
        cal.setTimeInMillis(1703514645000L);

        final Calendar result = Dates.addDays(cal, 1);

        assertEquals(Long.MIN_VALUE, ((GregorianCalendar) result).getGregorianChange().getTime());
    }

    // ------------------------------------------------------------------
    // #14: parseToTemporalAccessor is public.
    // ------------------------------------------------------------------

    @Test
    public void parseToTemporalAccessor_isPubliclyUsable() {
        final TemporalAccessor ta = Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor("2023-12-25 15:30:45");
        assertEquals(2023, ta.get(ChronoField.YEAR));
        assertEquals(12, ta.get(ChronoField.MONTH_OF_YEAR));
        assertEquals(25, ta.get(ChronoField.DAY_OF_MONTH));

        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor(null));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor("null"));
        assertNull(Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor("NuLl"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToTemporalAccessor(""));
    }

    // ------------------------------------------------------------------
    // #16: parse failures carry input, pattern/zone, and error index.
    // ------------------------------------------------------------------

    @Test
    public void parseFailure_messageCarriesContext() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("2023-13-25", "yyyy-MM-dd", TimeZone.getTimeZone("UTC")));
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().contains("2023-13-25"), e.getMessage());
        assertTrue(e.getMessage().contains("yyyy-MM-dd"), e.getMessage());
        assertNotNull(e.getCause());

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2023-12-25 ", "yyyy-MM-dd", TimeZone.getTimeZone("UTC")));

        final IllegalArgumentException dtfError = assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE.parseToLocalDate("2023-02-30"));
        assertTrue(dtfError.getMessage().contains("2023-02-30"), dtfError.getMessage());
        assertTrue(dtfError.getMessage().contains("uuuu-MM-dd"), dtfError.getMessage());
        assertNotNull(dtfError.getCause());
    }

    // ------------------------------------------------------------------
    // #2: proleptic years and era handling.
    // ------------------------------------------------------------------

    @Test
    public void dtf_prolepticYearPattern_acceptsNegativeYears() {
        // Blanket ERA defaulting used to make STRICT reject negative years with a spurious era conflict.
        assertEquals(LocalDate.of(-1, 1, 1), Dates.DTF.of("uuuu-MM-dd").parseToLocalDate("-0001-01-01"));
        assertEquals("-0001-01-01", Dates.DTF.of("uuuu-MM-dd").format(LocalDate.of(-1, 1, 1)));
        assertEquals(LocalDate.of(0, 1, 1), Dates.DTF.LOCAL_DATE.parseToLocalDate("0000-01-01"));
        assertEquals("0000-01-01", Dates.DTF.LOCAL_DATE.format(LocalDate.of(0, 1, 1)));
        assertEquals(LocalDate.of(-1, 1, 1), Dates.DTF.LOCAL_DATE.parseToLocalDate("-0001-01-01"));
    }

    @Test
    public void dtf_yearOfEraPattern_defaultsToCE_andMixedYearFieldsResolveConsistently() {
        assertEquals(LocalDate.of(2023, 12, 25), Dates.DTF.LOCAL_DATE.parseToLocalDate("2023-12-25"));
        // explicit era is honored (1 BC == ISO year 0)
        assertEquals(LocalDate.of(0, 1, 1), Dates.DTF.of("yyyy-MM-dd G").parseToLocalDate("0001-01-01 BC"));

        final Dates.DTF mixed = Dates.DTF.of("uuuu yyyy-MM-dd");
        assertEquals(LocalDate.of(-1, 1, 1), mixed.parseToLocalDate("-0001 0002-01-01"));
        assertThrows(IllegalArgumentException.class, () -> mixed.parseToLocalDate("-0001 0001-01-01"));
    }

    // ------------------------------------------------------------------
    // #3: the DTF resolver validates consistently.
    // ------------------------------------------------------------------

    @Test
    public void dtf_allTypedParsers_rejectOffsetInconsistentWithZone() {
        final String bad = "2023-07-01T12:00:00-08:00[America/Los_Angeles]";

        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(bad));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToLocalDate(bad));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToLocalDateTime(bad));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToLocalTime(bad));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToInstant(bad));
    }

    @Test
    public void dtf_partialPatternsWithOffset_instantProducersRequireDateFields() {
        // time-only + offset: no date fields, so instant-producing parses are rejected
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("HH:mmXXX").parseToInstant("12:30+05:30"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("HH:mmXXX").parseToOffsetDateTime("12:30+05:30"));
        // date-only + offset: start of day at that offset
        assertEquals(Instant.parse("2023-12-24T18:30:00Z"), Dates.DTF.of("yyyy-MM-ddXXX").parseToInstant("2023-12-25+05:30"));
    }

    @Test
    public void dtf_dstGapAndUndisambiguatedOverlap_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("uuuu-MM-dd HH:mm VV").parseToZonedDateTime("2024-03-10 02:30 America/Los_Angeles"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("uuuu-MM-dd HH:mm VV").parseToZonedDateTime("2024-11-03 01:30 America/Los_Angeles"));
    }

    @Test
    public void dtf_partialRegionPatterns_rejectTimeOnly_andOverlapOffsetDisambiguates() {
        // time-only + region: no date fields, so the instant-producing parse is rejected
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("HH:mm VV").parseToInstant("12:30 Asia/Kolkata"));
        assertEquals(Instant.parse("2023-12-25T08:00:00Z"), Dates.DTF.of("uuuu-MM-dd VV").parseToInstant("2023-12-25 America/Los_Angeles"));

        final Dates.DTF overlap = Dates.DTF.of("uuuu-MM-dd HH:mmXXX'['VV']'");
        assertEquals(Instant.parse("2024-11-03T08:30:00Z"), overlap.parseToInstant("2024-11-03 01:30-07:00[America/Los_Angeles]"));
        assertEquals(Instant.parse("2024-11-03T09:30:00Z"), overlap.parseToInstant("2024-11-03 01:30-08:00[America/Los_Angeles]"));
    }

    // ------------------------------------------------------------------
    // #4: Timestamp nanosecond handling.
    // ------------------------------------------------------------------

    @Test
    public void timestampRounding_millisecondDropsSubMilliFraction() {
        final java.sql.Timestamp ts = new java.sql.Timestamp(1703514645123L);
        ts.setNanos(123456789);

        assertEquals(123000000, Dates.truncate(ts, Calendar.MILLISECOND).getNanos());
        assertEquals(123000000, Dates.round(ts, Calendar.MILLISECOND).getNanos());
        assertEquals(124000000, Dates.ceiling(ts, Calendar.MILLISECOND).getNanos());

        ts.setNanos(123500000);
        assertEquals(124000000, Dates.round(ts, Calendar.MILLISECOND).getNanos()); // half up

        final java.sql.Timestamp max = new java.sql.Timestamp(Long.MAX_VALUE);
        max.setNanos(807_500_000); // getTime() remains Long.MAX_VALUE, with a half-millisecond fraction
        assertEquals(Long.MAX_VALUE, max.getTime());
        assertThrows(ArithmeticException.class, () -> Dates.round(max, Calendar.MILLISECOND));
    }

    @Test
    public void setMilliseconds_replacesTimestampFractionWhileOtherSettersPreserveIt() {
        final java.sql.Timestamp source = java.sql.Timestamp.from(Instant.ofEpochSecond(1_703_514_645L, 123_456_789));

        final java.sql.Timestamp millisecondsSet = Dates.setMilliseconds(source, 500);
        assertEquals(500_000_000, millisecondsSet.getNanos());
        assertEquals(500L, Math.floorMod(millisecondsSet.getTime(), 1000L));

        final java.sql.Timestamp secondsSet = Dates.setSeconds(source, 10);
        assertEquals(123_456_789, secondsSet.getNanos());
        assertEquals(123_456_789, source.getNanos(), "the source must remain unchanged");
    }

    @Test
    public void timestampCeiling_oneNanoPastSecond_movesToNextSecond() {
        final java.sql.Timestamp ts = new java.sql.Timestamp(1703514645000L);
        ts.setNanos(1);

        final java.sql.Timestamp ceiled = Dates.ceiling(ts, Calendar.SECOND);
        assertEquals(1703514646000L, ceiled.getTime());
        assertEquals(0, ceiled.getNanos());

        // exactly on the boundary: unchanged
        final java.sql.Timestamp exact = new java.sql.Timestamp(1703514645000L);
        assertEquals(1703514645000L, Dates.ceiling(exact, Calendar.SECOND).getTime());

        final java.sql.Timestamp max = new java.sql.Timestamp(Long.MAX_VALUE);
        max.setNanos(807_000_001); // one nanosecond above Long.MAX_VALUE milliseconds
        assertEquals(Long.MAX_VALUE, max.getTime());
        assertThrows(ArithmeticException.class, () -> Dates.ceiling(max, Calendar.MILLISECOND));
    }

    @Test
    public void parseToTimestamp_nineDigitFraction_honorsExplicitZone() {
        final java.sql.Timestamp ts = Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, TimeZone.getTimeZone("UTC"));
        assertEquals(1736937045123L, ts.getTime());
        assertEquals(123456789, ts.getNanos());

        final java.sql.Timestamp emptyFormat = Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", "", TimeZone.getTimeZone("UTC"));
        assertEquals(ts, emptyFormat);
        assertEquals(123456789, emptyFormat.getNanos());

        final TimeZone fixedCustomZone = new SimpleTimeZone(3_600_000, "SAME-ID");
        final java.sql.Timestamp customZone = Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, fixedCustomZone);
        assertEquals(1736933445123L, customZone.getTime());
        assertEquals(123456789, customZone.getNanos());

        final java.sql.Timestamp dtfTimestamp = Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp("2025-01-15 10:30:45", fixedCustomZone);
        assertEquals(1736933445000L, dtfTimestamp.getTime());

        final Calendar calendar = new GregorianCalendar(fixedCustomZone);
        calendar.setTimeInMillis(1736933445000L);
        assertEquals("2025-01-15 10:30:45", Dates.DTF.LOCAL_DATE_TIME.format(calendar));

        final Calendar parsedCalendar = Dates.DTF.LOCAL_DATE_TIME.parseToCalendar("2025-01-15 10:30:45", fixedCustomZone);
        assertEquals(1736933445000L, parsedCalendar.getTimeInMillis());
        assertEquals("SAME-ID", parsedCalendar.getTimeZone().getID());
    }

    @Test
    public void parseToTimestamp_explicitZone_isIndependentOfDefault_andRejectsDstAmbiguity() {
        final TimeZone prior = TimeZone.getDefault();
        final TimeZone la = TimeZone.getTimeZone("America/Los_Angeles");

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            final java.sql.Timestamp first = Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, la);

            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
            final java.sql.Timestamp second = Dates.parseToTimestamp("2025-01-15 10:30:45.123456789", null, la);
            assertEquals(first, second);
            assertEquals(first.getNanos(), second.getNanos());

            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2024-03-10 02:30:00", null, la));
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2024-11-03 01:30:00", null, la));

            TimeZone.setDefault(la);
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2024-03-10 02:30:00"));
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp("2024-11-03 01:30:00"));
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    // ------------------------------------------------------------------
    // #5: no-zone DTF format/parse round-trip holds across a default-zone change.
    // ------------------------------------------------------------------

    @Test
    public void dtf_noZone_roundTrip_afterDefaultZoneChange() {
        final TimeZone prior = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            final String formatted = Dates.DTF.LOCAL_DATE_TIME.format(new Date(0L));
            assertEquals(0L, Dates.DTF.LOCAL_DATE_TIME.parseToJUDate(formatted).getTime());

            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
            final String formatted2 = Dates.DTF.LOCAL_DATE_TIME.format(new Date(0L));
            assertEquals(0L, Dates.DTF.LOCAL_DATE_TIME.parseToJUDate(formatted2).getTime());
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    // ------------------------------------------------------------------
    // #8: HTTP-date is fixed to GMT, regardless of the ambient/input zone.
    // ------------------------------------------------------------------

    @Test
    public void httpDate_formatsAsGmt_andRejectsNonHttpZones() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", Dates.format(new Date(0L), Dates.HTTP_DATE_FORMAT, utc));

        final ZonedDateTime kolkata = Instant.EPOCH.atZone(ZoneId.of("Asia/Kolkata"));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", Dates.DTF.HTTP_DATE.format(kolkata));

        // and it still parses back
        assertEquals(0L, Dates.parseToJUDate("Thu, 01 Jan 1970 00:00:00 GMT", Dates.HTTP_DATE_FORMAT).getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(0L), Dates.HTTP_DATE_FORMAT, TimeZone.getTimeZone("America/Los_Angeles")));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("Wed, 31 Dec 1969 16:00:00 PST", Dates.HTTP_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("Mon, 02 Jul 2024 06:53:48 GMT", Dates.HTTP_DATE_FORMAT)); // July 2 was Tuesday
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("Thu, 01 Jan 1970 00:00:00 GMT ", Dates.HTTP_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.HTTP_DATE.parseToInstant("Mon, 02 Jul 2024 06:53:48 GMT"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.HTTP_DATE.parseToInstant("Thu, 01 Jan 1970 00:00:00 GMT "));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.HTTP_DATE.parseToInstant("Sat, 01 Jan +10000 00:00:00 GMT"));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.HTTP_DATE.format(LocalDateTime.of(1970, 1, 1, 0, 0)));

        final Date year10000 = Date.from(LocalDate.of(10000, 1, 1).atStartOfDay(Dates.UTC_ZONE_ID).toInstant());
        final Date yearZero = Date.from(LocalDate.of(0, 1, 1).atStartOfDay(Dates.UTC_ZONE_ID).toInstant());
        final Date yearOne = Date.from(LocalDate.of(1, 1, 1).atStartOfDay(Dates.UTC_ZONE_ID).toInstant());
        assertEquals("Mon, 01 Jan 0001 00:00:00 GMT", Dates.format(yearOne, Dates.HTTP_DATE_FORMAT));
        assertEquals("Mon, 01 Jan 0001 00:00:00 GMT", Dates.DTF.HTTP_DATE.format(yearOne));
        assertEquals(yearOne.getTime(), Dates.parseToJUDate("Mon, 01 Jan 0001 00:00:00 GMT", Dates.HTTP_DATE_FORMAT).getTime());
        assertEquals(yearOne.getTime(), Dates.DTF.HTTP_DATE.parseToJUDate("Mon, 01 Jan 0001 00:00:00 GMT").getTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.format(year10000, Dates.HTTP_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(yearZero, Dates.HTTP_DATE_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.HTTP_DATE.format(year10000));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.HTTP_DATE.format(yearZero));
    }

    // ------------------------------------------------------------------
    // #7: creator results must carry the requested instant.
    // ------------------------------------------------------------------

    @Test
    public void creatorReturningWrongMillis_isRejected() {
        assertTrue(Dates.registerDateCreator(WrongMillisDate.class, millis -> new WrongMillisDate(0L))); // ignores millis

        try {
            assertThrows(IllegalStateException.class, () -> Dates.addDays(new WrongMillisDate(1000L), 1));
        } finally {
            assertTrue(Dates.unregisterDateCreator(WrongMillisDate.class));
        }
    }

    @Test
    public void reflectiveConstructorsReturningWrongMillis_areRejected() {
        assertThrows(IllegalStateException.class, () -> Dates.addDays(new IgnoringDateConstructor(1234L), 1));
        final IgnoringCalendarConstructor calendar = new IgnoringCalendarConstructor(0L);
        calendar.setTimeInMillis(1234L);
        assertThrows(IllegalStateException.class, () -> Dates.addDays(calendar, 1));
    }

    @Test
    public void publicCreatorLifecycle_enforcesExactType_andIsolatesTemplate() {
        assertTrue(Dates.registerDateCreator(CustomDate.class, CustomDate::new));

        try {
            final CustomDate result = Dates.addDays(new CustomDate(0L), 1);
            assertEquals(CustomDate.class, result.getClass());
            assertEquals(86_400_000L, result.getTime());
        } finally {
            assertTrue(Dates.unregisterDateCreator(CustomDate.class));
        }

        assertFalse(Dates.unregisterDateCreator(CustomDate.class));

        assertTrue(Dates.registerCalendarCreator(ParentCalendar.class, (millis, template) -> new ChildCalendar(millis)));

        try {
            assertThrows(IllegalStateException.class, () -> Dates.addDays(new ParentCalendar(0L), 1));
        } finally {
            assertTrue(Dates.unregisterCalendarCreator(ParentCalendar.class));
        }

        final AliasingCalendar source = new AliasingCalendar(1234L);
        assertTrue(Dates.registerCalendarCreator(AliasingCalendar.class, AliasingCalendar::fromTemplate));

        try {
            final AliasingCalendar copy = Dates.addDays(source, 1);
            assertEquals(1234L, source.getTimeInMillis(), "creator mutations must be confined to the template clone");
            assertTrue(copy.getTimeInMillis() > source.getTimeInMillis());
        } finally {
            assertTrue(Dates.unregisterCalendarCreator(AliasingCalendar.class));
        }
    }

    @Test
    public void creatorReturningSourceInstance_isRejectedWithoutArithmeticMutation() {
        final AliasingCalendar source = new AliasingCalendar(1234L);
        assertTrue(Dates.registerCalendarCreator(AliasingCalendar.class, (millis, template) -> source));

        try {
            assertThrows(IllegalStateException.class, () -> Dates.addDays(source, 1));
            assertEquals(1234L, source.getTimeInMillis());
        } finally {
            assertTrue(Dates.unregisterCalendarCreator(AliasingCalendar.class));
        }
    }

    // ------------------------------------------------------------------
    // #1: calendar-field arithmetic must not silently wrap the epoch range.
    // ------------------------------------------------------------------

    @Test
    public void addDays_beyondEpochRange_throwsArithmeticException() {
        assertThrows(ArithmeticException.class, () -> Dates.addDays(new Date(Long.MAX_VALUE), 1));
        assertThrows(ArithmeticException.class, () -> Dates.addMonths(new Date(Long.MIN_VALUE), -1));
        assertThrows(ArithmeticException.class, () -> Dates.addYears(Dates.createCalendar(Long.MAX_VALUE), 1));
    }

    @Test
    public void calendarArithmetic_nearBoundsDoesNotFalsePositive_andMultiWrapIsDetected() {
        final Date nearMin = new Date(Long.MIN_VALUE);
        final Date nearMax = new Date(Long.MAX_VALUE);
        assertTrue(Dates.addDays(nearMin, 1).after(nearMin));
        assertTrue(Dates.addDays(nearMax, -1).before(nearMax));

        final Date epoch = new Date(0L);
        assertThrows(ArithmeticException.class, () -> Dates.addYears(epoch, 600_000_000));
        assertThrows(ArithmeticException.class, () -> Dates.addYears(epoch, -600_000_000));
        assertEquals(0L, epoch.getTime(), "failed arithmetic must not mutate the source");
    }

    @Test
    public void prolepticLengthHelpers_ignoreGregorianCutover_andAcceptSqlDate() {
        final Date in1582 = Date.from(LocalDate.of(1582, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        assertEquals(365, Dates.lengthOfYear(in1582));
        assertEquals(30, Dates.lengthOfMonth(in1582));

        final java.sql.Date sqlDate = new java.sql.Date(in1582.getTime());
        assertEquals(365, Dates.lengthOfYear(sqlDate));
        assertTrue(Dates.isLastDayOfYear(Date.from(LocalDate.of(1582, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant())));
    }

    @Test
    public void dtfSqlDateAndTime_useOneLiveDefaultZoneSnapshotPerCall() {
        final TimeZone prior = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertEquals(0L, Dates.DTF.LOCAL_DATE.parseToDate("1970-01-01").getTime());
            assertEquals(0L, Dates.DTF.LOCAL_TIME.parseToTime("00:00:00").getTime());

            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            assertEquals(28_800_000L, Dates.DTF.LOCAL_DATE.parseToDate("1970-01-01").getTime());
            assertEquals(28_800_000L, Dates.DTF.LOCAL_TIME.parseToTime("00:00:00").getTime());
            assertNotNull(Dates.DTF.ISO_8601_DATE_TIME.parseToDate("1970-01-01T00:00:00Z"));
            assertNotNull(Dates.DTF.ISO_8601_DATE_TIME.parseToTime("1970-01-01T00:00:00Z"));
            assertNotNull(Dates.DTF.HTTP_DATE.parseToDate("Thu, 01 Jan 1970 00:00:00 GMT"));
            assertNotNull(Dates.DTF.HTTP_DATE.parseToTime("Thu, 01 Jan 1970 00:00:00 GMT"));
        } finally {
            TimeZone.setDefault(prior);
        }
    }

    @Test
    public void dtfPatternFactory_preservesPatternSemantics_andFormatsSqlTypes() {
        final Dates.DTF literalZ = Dates.DTF.of(Dates.ISO_8601_DATE_TIME_FORMAT);
        final OffsetDateTime plusFiveThirty = Instant.EPOCH.atOffset(java.time.ZoneOffset.ofHoursMinutes(5, 30));
        assertEquals("1970-01-01T05:30:00Z", literalZ.format(plusFiveThirty));
        assertEquals(Dates.ISO_8601_DATE_TIME_FORMAT, literalZ.toString());
        assertEquals("1970-01-01T00:00:00Z", Dates.DTF.ISO_8601_DATE_TIME.format(plusFiveThirty));

        final java.sql.Date sqlDate = new java.sql.Date(0L);
        final java.sql.Time sqlTime = new java.sql.Time(0L);
        assertNotNull(Dates.DTF.LOCAL_DATE.format(sqlDate));
        assertNotNull(Dates.DTF.LOCAL_TIME.format(sqlTime));
    }

    // ------------------------------------------------------------------
    // #13: auto-detected ISO failures carry the same context as patterned ones.
    // ------------------------------------------------------------------

    @Test
    public void autoDetectedIsoFailure_messageCarriesContext() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2023-13-25T00:00:00Z"));
        assertNotNull(e.getMessage());
        assertTrue(e.getMessage().contains("2023-13-25T00:00:00Z"), e.getMessage());
        assertNotNull(e.getCause());

        // bare numeric text is rejected as ambiguous even with an explicit empty format;
        // epoch millis go through the explicit parseEpochMillis API.
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("1234", ""));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate("1234", ""));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToTime("1234", ""));
        assertEquals(1234L, Dates.parseEpochMillis("1234"));
    }

    @Test
    public void temporalAccessorParser_doesNotBypassOffsetZoneValidation() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Dates.DTF.ISO_ZONED_DATE_TIME.parseToTemporalAccessor("2023-07-01T12:00:00-08:00[America/Los_Angeles]"));
        assertNotNull(e.getCause());
        assertTrue(e.getMessage().contains("America/Los_Angeles"), e.getMessage());
    }

    // ------------------------------------------------------------------
    // Follow-up review: custom-default initialization and TimeZone rule preservation.
    // ------------------------------------------------------------------

    @Test
    public void customDefaultTimeZone_doesNotPreventDatesClassInitialization() throws Exception {
        final String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final Process process = new ProcessBuilder(javaExecutable, "-cp", classPath, "testfixtures.dates.DatesCustomDefaultSmoke")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        final boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("Fresh-JVM Dates initialization probe timed out");
        }

        assertEquals(0, process.exitValue());
    }

    @Test
    public void registeredIdCollision_usesActualFixedRules_notRegisteredRules() {
        final SimpleTimeZone fixedMinusEight = new SimpleTimeZone(-8 * 60 * 60 * 1000, "America/Los_Angeles");
        final java.sql.Timestamp parsed = Dates.DTF.LOCAL_DATE_TIME.parseToTimestamp("2023-07-01 12:00:00", fixedMinusEight);

        assertEquals(Instant.parse("2023-07-01T20:00:00Z"), parsed.toInstant());
        assertEquals(-8 * 60 * 60 * 1000, Dates.DTF.LOCAL_DATE_TIME.parseToCalendar("2023-07-01 12:00:00", fixedMinusEight).getTimeZone().getRawOffset());
    }

    // ------------------------------------------------------------------
    // Follow-up review: strict, consistent legacy ISO behavior.
    // ------------------------------------------------------------------

    @Test
    public void legacyIso_zoneLessShapes_allHonorExplicitTimeZone() {
        final TimeZone losAngeles = TimeZone.getTimeZone("America/Los_Angeles");
        final long expectedWithSeconds = LocalDateTime.of(2023, 12, 25, 10, 30, 45).atZone(ZoneId.of("America/Los_Angeles")).toInstant().toEpochMilli();
        final long expectedWithoutSeconds = LocalDateTime.of(2023, 12, 25, 10, 30).atZone(ZoneId.of("America/Los_Angeles")).toInstant().toEpochMilli();
        final long expectedDate = LocalDate.of(2023, 12, 25).atStartOfDay(ZoneId.of("America/Los_Angeles")).toInstant().toEpochMilli();

        assertEquals(expectedWithSeconds, Dates.parseToJUDate("2023-12-25T10:30:45", null, losAngeles).getTime());
        assertEquals(expectedWithoutSeconds, Dates.parseToJUDate("2023-12-25T10:30", null, losAngeles).getTime());
        assertEquals(expectedWithSeconds, Dates.parseToJUDate("20231225T103045", null, losAngeles).getTime());
        assertEquals(expectedDate, Dates.parseToJUDate("2023-12-25", null, losAngeles).getTime());

        // A zone written in the value remains authoritative; the supplied zone is only a fallback.
        assertEquals(Instant.parse("2023-12-25T05:00:00Z"), Dates.parseToJUDate("2023-12-25T10:30:00+05:30", null, losAngeles).toInstant());
        assertEquals(Instant.parse("2023-12-25T10:30:00Z"),
                Dates.parseToJUDate("2023-12-25T10:30:00Z", Dates.ISO_OFFSET_DATE_TIME_FORMAT, losAngeles).toInstant());
    }

    @Test
    public void legacyIso_isProlepticGregorian_andRejectsLeapSeconds() {
        final Date octoberTenth = Dates.parseToJUDate("1582-10-10T00:00:00Z");
        assertEquals(Instant.parse("1582-10-10T00:00:00Z"), octoberTenth.toInstant());
        assertEquals("1582-10-10T00:00:00Z", Dates.format(octoberTenth));

        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final Calendar parsedCalendar = Dates.parseToCalendar("1582-10-10", Dates.LOCAL_DATE_FORMAT, utc, Locale.US);
        final GregorianCalendar parsedGregorian = Dates.parseToGregorianCalendar("1582-10-10", Dates.LOCAL_DATE_FORMAT, utc, Locale.US);
        final javax.xml.datatype.XMLGregorianCalendar parsedXml = Dates.parseToXMLGregorianCalendar("1582-10-10", Dates.LOCAL_DATE_FORMAT, utc, Locale.US);
        assertTrue(parsedCalendar instanceof GregorianCalendar);
        assertEquals(1582, parsedCalendar.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, parsedCalendar.get(Calendar.MONTH));
        assertEquals(10, parsedCalendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(Long.MIN_VALUE, ((GregorianCalendar) parsedCalendar).getGregorianChange().getTime());
        assertEquals(Long.MIN_VALUE, parsedGregorian.getGregorianChange().getTime());
        assertEquals(1582, parsedXml.getYear());
        assertEquals(10, parsedXml.getMonth());
        assertEquals(10, parsedXml.getDay());

        final Calendar dtfCalendar = Dates.DTF.LOCAL_DATE.parseToCalendar("1582-10-10", utc);
        assertEquals(1582, dtfCalendar.get(Calendar.YEAR));
        assertEquals(Calendar.OCTOBER, dtfCalendar.get(Calendar.MONTH));
        assertEquals(10, dtfCalendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(Long.MIN_VALUE, ((GregorianCalendar) dtfCalendar).getGregorianChange().getTime());
        final Calendar usWeekTemplate = new GregorianCalendar(utc, Locale.US);
        assertEquals(usWeekTemplate.getFirstDayOfWeek(), dtfCalendar.getFirstDayOfWeek());
        assertEquals(usWeekTemplate.getMinimalDaysInFirstWeek(), dtfCalendar.getMinimalDaysInFirstWeek());

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2016-12-31T23:59:60Z"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2016-12-31T23:59:61Z"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2016-12-31T23:59:62Z"));
    }

    @Test
    public void legacyIso_fourDigitYearContract_isRoundTrippable() {
        final Date yearOne = Date.from(LocalDate.of(1, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
        final Date yearZero = Date.from(LocalDate.of(0, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
        final Date year10000 = Date.from(LocalDate.of(10000, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());

        assertEquals("0001-01-01T00:00:00Z", Dates.format(yearOne));
        assertEquals(yearOne, Dates.parseToJUDate(Dates.format(yearOne)));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("0000-01-01T00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("10000-01-01T00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(yearZero));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(year10000));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(year10000, Dates.ISO_8601_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("10000-01-01 00:00:00", Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
    }

    @Test
    public void isoUtility_rejectsOffsetsItCannotRoundTrip() {
        final TimeZone plusEighteen = new SimpleTimeZone(18 * 60 * 60 * 1000, "plus-eighteen");

        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(Instant.EPOCH, ZoneOffset.ofTotalSeconds(30)));
        assertThrows(java.time.format.DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:00+18:01"));
        assertEquals(Instant.parse("2023-12-24T16:30:00Z"), ISO8601Util.parseInstant("2023-12-25T10:30:00+18:00"));
        assertEquals("1970-01-01T18:00:00+18:00", ISO8601Util.format(Instant.EPOCH, ZoneOffset.ofHours(18)));

        assertThrows(IllegalArgumentException.class,
                () -> Dates.format(new Date(0L), Dates.ISO_OFFSET_DATE_TIME_FORMAT, new SimpleTimeZone(30_000, "thirty-seconds")));
        assertThrows(IllegalArgumentException.class,
                () -> Dates.format(new Date(0L), Dates.ISO_OFFSET_DATE_TIME_FORMAT, new SimpleTimeZone(19 * 60 * 60 * 1000, "plus-nineteen")));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2023-12-25T10:30:00+18:01", Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertEquals(Instant.parse("2023-12-24T16:30:00Z"), Dates.parseToJUDate("2023-12-25T10:30:00+18:00", Dates.ISO_OFFSET_DATE_TIME_FORMAT).toInstant());
        assertEquals("1970-01-01T18:00:00+18:00", Dates.format(new Date(0L), Dates.ISO_OFFSET_DATE_TIME_FORMAT, plusEighteen));
    }

    @Test
    public void autoIsoParser_convertsLegacyFallbackZoneOnlyWhenNeeded() {
        final TimeZone fixedPlusOne = new SimpleTimeZone(60 * 60 * 1000, "fixed-plus-one");
        assertEquals(Instant.parse("2023-12-25T09:30:00Z"), Dates.parseToJUDate("20231225T103000", null, fixedPlusOne).toInstant());

        final SimpleTimeZone customDst = new SimpleTimeZone(-8 * 60 * 60 * 1000, "custom-dst", Calendar.MARCH, 2, Calendar.SUNDAY, 2 * 60 * 60 * 1000,
                Calendar.NOVEMBER, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);

        assertEquals(Instant.parse("2023-12-25T08:30:00Z"), Dates.parseToJUDate("20231225T103000+0200", null, customDst).toInstant());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("20231225T103000", null, customDst));
    }

    // ------------------------------------------------------------------
    // Follow-up review: exact Timestamp instants and creator normalization.
    // ------------------------------------------------------------------

    @Test
    public void timestampConversionAndComparisons_preserveNanoseconds() {
        final java.sql.Timestamp first = java.sql.Timestamp.from(Instant.ofEpochSecond(10, 100));
        final java.sql.Timestamp middle = java.sql.Timestamp.from(Instant.ofEpochSecond(10, 150));
        final java.sql.Timestamp second = java.sql.Timestamp.from(Instant.ofEpochSecond(10, 200));
        final java.sql.Timestamp third = java.sql.Timestamp.from(Instant.ofEpochSecond(10, 300));

        final java.sql.Timestamp copy = Dates.createTimestamp(first);
        assertEquals(first.toInstant(), copy.toInstant());
        assertFalse(Dates.isSameInstant(first, second));
        assertTrue(Dates.isBetween(middle, first, second));
        assertThrows(IllegalArgumentException.class, () -> Dates.isBetween(first, second, first));
        assertTrue(Dates.isOverlapping(first, second, middle, third));
        assertFalse(Dates.isOverlapping(first, second, second, third));

        final java.sql.Timestamp belowHalfMillisecond = java.sql.Timestamp.from(Instant.ofEpochSecond(10, 123_499_999));
        final java.sql.Timestamp atHalfMillisecond = java.sql.Timestamp.from(Instant.ofEpochSecond(10, 123_500_000));
        assertEquals(123_000_000, Dates.round(belowHalfMillisecond, Calendar.MILLISECOND).getNanos());
        assertEquals(124_000_000, Dates.round(atHalfMillisecond, Calendar.MILLISECOND).getNanos());
    }

    @Test
    public void timestampOperations_normalizeCreatorSubMillisBeforeApplyingTheirPrecisionContract() {
        assertTrue(Dates.registerDateCreator(NoisyTimestamp.class, millis -> {
            final NoisyTimestamp result = new NoisyTimestamp(millis);
            result.setNanos((int) Math.floorMod(millis, 1000L) * 1_000_000 + 999_999);
            return result;
        }));

        try {
            final NoisyTimestamp source = new NoisyTimestamp(1_700_000_000_123L);
            source.setNanos(123_456_789);
            final NoisyTimestamp result = Dates.addSeconds(source, 1);

            assertEquals(source.getTime() + 1000L, result.getTime());
            assertEquals(123_456_789, result.getNanos());

            final NoisyTimestamp millisecondsSet = Dates.setMilliseconds(source, 321);
            assertEquals(321L, Math.floorMod(millisecondsSet.getTime(), 1000L));
            assertEquals(321_000_000, millisecondsSet.getNanos());

            final NoisyTimestamp rounded = Dates.round(source, Calendar.MILLISECOND);
            final NoisyTimestamp truncated = Dates.truncate(source, Calendar.MILLISECOND);
            final NoisyTimestamp ceiled = Dates.ceiling(source, Calendar.MILLISECOND);
            assertEquals(source.getTime(), rounded.getTime());
            assertEquals(123_000_000, rounded.getNanos());
            assertEquals(source.getTime(), truncated.getTime());
            assertEquals(123_000_000, truncated.getNanos());
            assertEquals(source.getTime() + 1L, ceiled.getTime());
            assertEquals(124_000_000, ceiled.getNanos());
        } finally {
            assertTrue(Dates.unregisterDateCreator(NoisyTimestamp.class));
        }
    }

    // ------------------------------------------------------------------
    // Follow-up review: fixed-duration Calendar arithmetic.
    // ------------------------------------------------------------------

    @Test
    public void calendarSubDayIntAdders_useFixedDurationWithoutCallingCalendarAdd() {
        final RejectingFieldAddCalendar source = new RejectingFieldAddCalendar();
        source.setTimeInMillis(1_700_000_000_123L);
        final long originalMillis = source.getTimeInMillis();

        assertEquals(originalMillis + 3_600_000L, Dates.addHours(source, 1).getTimeInMillis());
        assertEquals(originalMillis + 60_000L, Dates.addMinutes(source, 1).getTimeInMillis());
        assertEquals(originalMillis + 1_000L, Dates.addSeconds(source, 1).getTimeInMillis());
        assertEquals(originalMillis + 1L, Dates.addMilliseconds(source, 1).getTimeInMillis());
        assertEquals(originalMillis, source.getTimeInMillis(), "the source must remain unchanged");
    }

    // ------------------------------------------------------------------
    // Follow-up review: normalized fixed-zone conflict diagnostics.
    // ------------------------------------------------------------------

    @Test
    public void fixedZoneConflictErrors_useTheSameIdAndOffsetDescription() {
        final TimeZone conflictingZone = new SimpleTimeZone(19_800_000, "custom-plus-five-thirty");
        final String expectedZone = "time zone: custom-plus-five-thirty (raw offset +05:30)";

        final IllegalArgumentException utcError = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("1970-01-01T00:00:00Z", Dates.ISO_8601_DATE_TIME_FORMAT, conflictingZone));
        final IllegalArgumentException httpError = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate("Thu, 01 Jan 1970 00:00:00 GMT", Dates.HTTP_DATE_FORMAT, conflictingZone));

        assertTrue(utcError.getMessage().endsWith(expectedZone), utcError.getMessage());
        assertTrue(httpError.getMessage().endsWith(expectedZone), httpError.getMessage());
    }

    // ------------------------------------------------------------------
    // Follow-up review: calendar isolation and supported rounding fields.
    // ------------------------------------------------------------------

    @Test
    public void calendarOperations_rejectSelfCloningSourcesWithoutMutation() {
        // clone() is the only way to build a result of this type, so a clone() that returns the source
        // must be rejected before it is mutated or handed back. Uniform across the whole family.
        final SelfCloningCalendarWithoutConstructor source = new SelfCloningCalendarWithoutConstructor("marker");
        source.setTimeInMillis(1_700_000_000_123L);
        final long originalMillis = source.getTimeInMillis();

        assertThrows(IllegalStateException.class, () -> Dates.truncate(source, Calendar.SECOND));
        assertThrows(IllegalStateException.class, () -> Dates.round(source, Calendar.SECOND));
        assertThrows(IllegalStateException.class, () -> Dates.ceiling(source, Calendar.SECOND));
        assertThrows(IllegalStateException.class, () -> Dates.addDays(source, 1));
        assertEquals(originalMillis, source.getTimeInMillis());
    }

    @Test
    public void calendarOperations_preferADeclaredConstructorOverAHostileClone() {
        // Same hostile clone(), but this subtype has a usable no-arg constructor. The documented
        // precedence is creator -> declared constructor -> clone(), so clone() is never consulted and
        // the operation succeeds. round/truncate/ceiling used to clone unconditionally and therefore
        // failed here while addDays, which already built its result through the creator registry,
        // succeeded; the whole family now agrees.
        final SelfCloningCalendar source = new SelfCloningCalendar();
        source.setTimeZone(TimeZone.getTimeZone("UTC"));
        source.setTimeInMillis(1_700_000_000_123L);

        assertEquals(1_700_000_000_000L, Dates.truncate(source, Calendar.SECOND).getTimeInMillis());
        assertEquals(1_700_000_000_000L, Dates.round(source, Calendar.SECOND).getTimeInMillis());
        assertEquals(1_700_000_001_000L, Dates.ceiling(source, Calendar.SECOND).getTimeInMillis());
        assertEquals(SelfCloningCalendar.class, Dates.truncate(source, Calendar.SECOND).getClass());
        assertEquals(1_700_000_000_123L, source.getTimeInMillis(), "the source must not be mutated");
    }

    @Test
    public void calendarFactories_doNotAliasCallerTimeZone_andEraIsUnsupported() {
        final SimpleTimeZone callerZone = new SimpleTimeZone(3_600_000, "caller-zone");
        final Calendar calendar = Dates.createCalendar(0L, callerZone);
        final GregorianCalendar gregorian = Dates.createGregorianCalendar(0L, callerZone);
        callerZone.setRawOffset(7_200_000);

        assertEquals(3_600_000, calendar.getTimeZone().getRawOffset());
        assertEquals(3_600_000, gregorian.getTimeZone().getRawOffset());
        assertThrows(IllegalArgumentException.class, () -> Dates.truncate(new Date(0L), Calendar.ERA));
        assertThrows(IllegalArgumentException.class, () -> Dates.round(new Date(0L), Calendar.ERA));
        assertThrows(IllegalArgumentException.class, () -> Dates.ceiling(new Date(0L), Calendar.ERA));
    }

    // ------------------------------------------------------------------
    // Follow-up review: explicit locale support and pool isolation.
    // ------------------------------------------------------------------

    @Test
    public void legacyPatternLocaleOverloads_coverAllReturnAndAppendableTypes() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final Locale french = Locale.FRENCH;
        final String pattern = "dd MMMM yyyy HH:mm:ss";
        final String text = "25 décembre 2023 14:30:45";
        final long expectedMillis = Instant.parse("2023-12-25T14:30:45Z").toEpochMilli();

        final Date juDate = Dates.parseToJUDate(text, pattern, utc, french);
        assertEquals(expectedMillis, juDate.getTime());
        // SQL Date/Time expose the same TimeZone/Locale parsing surface and retain the resolved instant.
        assertEquals(expectedMillis, Dates.parseToDate(text, pattern, utc, french).getTime());
        assertEquals(expectedMillis, Dates.parseToTime(text, pattern, utc, french).getTime());
        assertEquals(expectedMillis, Dates.parseToTimestamp(text, pattern, utc, french).getTime());

        final Calendar calendar = Dates.parseToCalendar(text, pattern, utc, french);
        final GregorianCalendar gregorian = Dates.parseToGregorianCalendar(text, pattern, utc, french);
        final javax.xml.datatype.XMLGregorianCalendar xml = Dates.parseToXMLGregorianCalendar(text, pattern, utc, french);
        assertEquals(expectedMillis, calendar.getTimeInMillis());
        assertEquals(expectedMillis, gregorian.getTimeInMillis());
        assertEquals(expectedMillis, xml.toGregorianCalendar().getTimeInMillis());

        assertEquals(text, Dates.format(juDate, pattern, utc, french));
        assertEquals(text, Dates.format(calendar, pattern, utc, french));
        assertEquals(text, Dates.format(xml, pattern, utc, french));

        final StringBuilder dateOut = new StringBuilder();
        final StringBuilder calendarOut = new StringBuilder();
        final StringBuilder xmlOut = new StringBuilder();
        Dates.formatTo(juDate, pattern, utc, french, dateOut);
        Dates.formatTo(calendar, pattern, utc, french, calendarOut);
        Dates.formatTo(xml, pattern, utc, french, xmlOut);
        assertEquals(text, dateOut.toString());
        assertEquals(text, calendarOut.toString());
        assertEquals(text, xmlOut.toString());

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, pattern, utc));
        assertEquals("January", Dates.format(new Date(0L), "MMMM", utc, Locale.US));
        assertEquals("janvier", Dates.format(new Date(0L), "MMMM", utc, french));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", Dates.format(new Date(0L), Dates.HTTP_DATE_FORMAT, utc, french));
        assertEquals("1970-01-01T00:00:00Z", Dates.format(new Date(0L), Dates.ISO_8601_DATE_TIME_FORMAT, utc, Locale.forLanguageTag("ar-SA")));

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, pattern, utc, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(juDate, pattern, utc, null));
        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(juDate, pattern, utc, french, null));
    }

    @Test
    public void dtfFactory_acceptsExplicitLocaleForTextualFields() {
        final Dates.DTF french = Dates.DTF.of("dd MMMM uuuu", Locale.FRENCH);

        assertEquals(LocalDate.of(2023, 12, 25), french.parseToLocalDate("25 décembre 2023"));
        assertEquals("25 décembre 2023", french.format(LocalDate.of(2023, 12, 25)));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.of("dd MMMM uuuu", null));
    }

    // ------------------------------------------------------------------
    // Follow-up review: offset grammar, chunk loop, registry and formatTo edges.
    // ------------------------------------------------------------------

    @Test
    public void isoOffsetFormat_acceptsColonlessOffsetsConsistentlyWithAutoDetect() {
        final Instant expected = Instant.parse("2023-12-25T09:30:00Z");

        assertEquals(expected, Dates.parseToJUDate("2023-12-25T10:30:00+0100", Dates.ISO_OFFSET_DATE_TIME_FORMAT).toInstant());
        assertEquals(expected, Dates.parseToJUDate("2023-12-25T10:30:00+01:00", Dates.ISO_OFFSET_DATE_TIME_FORMAT).toInstant());
        assertEquals(expected, Dates.parseToJUDate("2023-12-25T10:30:00+0100").toInstant());

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2023-12-25T10:30:00+1801", Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2023-12-25T10:30:00+01", Dates.ISO_OFFSET_DATE_TIME_FORMAT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("2023-12-25T10:30:00+010000", Dates.ISO_OFFSET_DATE_TIME_FORMAT));
    }

    @Test
    public void autoDetectedMillisTimestamp_enforcesFourDigitYear() {
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("10000-01-01T00:00:00.000"));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate("0000-01-01T00:00:00.000"));

        // Zone-less input resolves in the default zone, so compare against the equivalent explicit parse
        // rather than a fixed instant.
        assertEquals(Dates.parseToJUDate("2023-12-25T14:30:45.123", "yyyy-MM-dd'T'HH:mm:ss.SSS", TimeZone.getDefault()),
                Dates.parseToJUDate("2023-12-25T14:30:45.123"));
    }

    @Test
    public void addYears_acrossChunkBoundary_matchesSequentialChunks() {
        final Date start = new Date(1_700_000_000_123L); // 2023-11-14T22:13:20.123Z

        assertEquals(Dates.addYears(Dates.addYears(start, 100_000_000), 50_000_000), Dates.addYears(start, 150_000_000));
        assertEquals(Dates.addYears(Dates.addYears(start, -100_000_000), -50_000_000), Dates.addYears(start, -150_000_000));

        final Calendar result = Calendar.getInstance();
        result.setTime(Dates.addYears(start, 150_000_000));
        assertEquals(150_002_023, result.get(Calendar.YEAR));
    }

    @Test
    public void unregisterCalendarCreator_reportsFalseOnlyWhenNothingWasRegistered() {
        assertFalse(Dates.unregisterCalendarCreator(AliasingCalendar.class));
        assertThrows(IllegalArgumentException.class, () -> Dates.unregisterCalendarCreator(null));
        assertThrows(IllegalArgumentException.class, () -> Dates.unregisterCalendarCreator(GregorianCalendar.class));
    }

    @Test
    public void formatTo_httpDateFormat_appendsCanonicalGmtText() {
        final StringBuilder legacyOut = new StringBuilder("Date: ");
        Dates.formatTo(new Date(0L), Dates.HTTP_DATE_FORMAT, TimeZone.getTimeZone("UTC"), legacyOut);
        assertEquals("Date: Thu, 01 Jan 1970 00:00:00 GMT", legacyOut.toString());

        final StringBuilder dtfOut = new StringBuilder();
        Dates.DTF.HTTP_DATE.formatTo(new Date(0L), dtfOut);
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", dtfOut.toString());

        final Date year10000 = Date.from(LocalDate.of(10000, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant());
        assertThrows(IllegalArgumentException.class, () -> Dates.formatTo(year10000, Dates.HTTP_DATE_FORMAT, TimeZone.getTimeZone("UTC"), new StringBuilder()));
    }

    @Test
    public void parse_nullMarker_isJavaNull_andRoundTripsFormatTo() {
        assertNull(Dates.parseToJUDate("null"));
        assertNull(Dates.parseToJUDate("NULL"));
        assertNull(Dates.DTF.LOCAL_DATE.parseToLocalDate("NuLl"));
        assertEquals(0L, Dates.parseEpochMillis("null"));
        assertEquals(0L, Dates.parseEpochMillis("NULL"));
        assertEquals(0L, Dates.parseEpochMillis(null));
        assertEquals(Instant.EPOCH, Dates.parseEpochMillisToInstant("NuLl"));
        assertEquals(Instant.EPOCH, Dates.parseEpochMillisToInstant(null));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseEpochMillis(""));

        final StringBuilder sb = new StringBuilder();
        Dates.formatTo((Date) null, sb);
        assertEquals("null", sb.toString());
        assertNull(Dates.parseToJUDate(sb.toString()));
    }

    @Test
    public void createGregorianAndXmlFromCalendar_preserveSourceZone() {
        final TimeZone tokyo = TimeZone.getTimeZone("Asia/Tokyo");
        final long millis = 1_736_937_045_000L;
        final Calendar source = Dates.createCalendar(millis, tokyo);

        final GregorianCalendar gregorian = Dates.createGregorianCalendar(source);
        assertEquals(millis, gregorian.getTimeInMillis());
        assertEquals("Asia/Tokyo", gregorian.getTimeZone().getID());

        final javax.xml.datatype.XMLGregorianCalendar xml = Dates.createXMLGregorianCalendar(source);
        final javax.xml.datatype.XMLGregorianCalendar expected = Dates.createXMLGregorianCalendar(millis, tokyo);
        assertEquals(expected.getTimezone(), xml.getTimezone());
        assertEquals(expected.getHour(), xml.getHour());
        assertEquals(expected.toGregorianCalendar().getTimeInMillis(), xml.toGregorianCalendar().getTimeInMillis());
    }

    @Test
    public void isSameDay_explicitZone_comparesCivilDayInThatZone() {
        // 09:00Z is still 1 Jan in UTC+14; 12:00Z is already 2 Jan there.
        final Date morningUtc = Dates.parseToJUDate("2023-01-01T09:00:00Z");
        final Date noonUtc = Dates.parseToJUDate("2023-01-01T12:00:00Z");

        assertTrue(Dates.isSameDay(morningUtc, noonUtc, ZoneOffset.UTC));
        assertFalse(Dates.isSameDay(morningUtc, noonUtc, ZoneId.of("Pacific/Kiritimati")));

        final Calendar utcCal = Dates.createCalendar(morningUtc.getTime(), TimeZone.getTimeZone("UTC"));
        final Calendar kiritimatiCal = Dates.createCalendar(noonUtc.getTime(), TimeZone.getTimeZone("Pacific/Kiritimati"));
        assertTrue(Dates.isSameDay(utcCal, kiritimatiCal, ZoneId.of("UTC")));
        assertFalse(Dates.isSameDay(utcCal, kiritimatiCal, ZoneId.of("Pacific/Kiritimati")));
        // 2-arg refuses to pick a zone: both argument orders throw. Using each calendar's own zone
        // (what 2-arg would do without requireCompatibleTimeZones) disagrees: UTC → true, UTC+14 → false.
        assertTrue(Dates.isSameDay(utcCal, kiritimatiCal, utcCal.getTimeZone()));
        assertFalse(Dates.isSameDay(kiritimatiCal, utcCal, kiritimatiCal.getTimeZone()));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(utcCal, kiritimatiCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(kiritimatiCal, utcCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(utcCal, kiritimatiCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(kiritimatiCal, utcCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(utcCal, kiritimatiCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(kiritimatiCal, utcCal));
    }

    @Test
    public void isSameDay_calendarTwoArg_usesIsoCivilDayInSharedZone() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final long millis = Instant.parse("2023-06-15T12:00:00Z").toEpochMilli();

        final Calendar gregorian = Dates.createGregorianCalendar(millis, utc);
        final Calendar gmt = Dates.createCalendar(millis, TimeZone.getTimeZone("GMT"));
        assertTrue(Dates.isSameDay(gregorian, gmt));
        assertTrue(Dates.isSameMonth(gregorian, gmt));
        assertTrue(Dates.isSameYear(gregorian, gmt));

        final Calendar buddhist = Calendar.getInstance(utc, Locale.forLanguageTag("th-TH-u-ca-buddhist"));
        buddhist.setTimeInMillis(millis);
        assertTrue(buddhist.get(Calendar.YEAR) != gregorian.get(Calendar.YEAR), "Thai Buddhist calendar should report a different YEAR field than Gregorian");
        assertTrue(Dates.isSameDay(gregorian, buddhist));
        assertTrue(Dates.isSameMonth(gregorian, buddhist));
        assertTrue(Dates.isSameYear(gregorian, buddhist));

        final TimeZone customDst = new SimpleTimeZone(0, "Dates-custom-DST", Calendar.MARCH, 2, Calendar.SUNDAY, 7_200_000, Calendar.NOVEMBER, 1,
                Calendar.SUNDAY, 7_200_000);
        final Calendar custom1 = Dates.createCalendar(millis, customDst);
        final Calendar custom2 = Dates.createCalendar(millis, customDst);
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(custom1, custom2));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(custom1, custom2));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(custom1, custom2));
    }

    @Test
    public void dtfParseToCalendar_weekSettingsMatchDatesParserLocale() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final Calendar datesUs = Dates.parseToCalendar("2023-12-25", Dates.LOCAL_DATE_FORMAT, utc, Locale.US);
        final Calendar dtfUs = Dates.DTF.LOCAL_DATE.parseToCalendar("2023-12-25", utc);
        assertEquals(datesUs.getFirstDayOfWeek(), dtfUs.getFirstDayOfWeek());
        assertEquals(datesUs.getMinimalDaysInFirstWeek(), dtfUs.getMinimalDaysInFirstWeek());

        final Calendar datesDe = Dates.parseToCalendar("2023-12-25", Dates.LOCAL_DATE_FORMAT, utc, Locale.GERMANY);
        final Calendar dtfDe = Dates.DTF.of("uuuu-MM-dd", Locale.GERMANY).parseToCalendar("2023-12-25", utc);
        assertEquals(datesDe.getFirstDayOfWeek(), dtfDe.getFirstDayOfWeek());
        assertEquals(datesDe.getMinimalDaysInFirstWeek(), dtfDe.getMinimalDaysInFirstWeek());
        assertTrue(datesUs.getFirstDayOfWeek() != datesDe.getFirstDayOfWeek() || datesUs.getMinimalDaysInFirstWeek() != datesDe.getMinimalDaysInFirstWeek());
    }
}
