package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Review pass 7 (2026-08-31c).
 *
 * <ul>
 *   <li><b>B2</b> &mdash; {@code getSDF} admitted a {@code dfPool} slot <i>before</i> constructing the
 *       {@code SimpleDateFormat}, so an invalid pattern - which always throws - left behind a queue that
 *       could never hold anything. 64 such patterns exhausted the remaining admission slots for the life
 *       of the process.</li>
 *   <li><b>B3</b> &mdash; {@code DTF.format(Calendar)} / {@code formatTo(Calendar, Appendable)} were the
 *       only {@code Calendar} operations without the class-level null-zone fallback.</li>
 *   <li><b>O3</b> &mdash; {@code parseToTimestamp}'s JDBC path reported a daylight-saving gap or overlap
 *       with a wrapped {@code "Cannot parse ..."} where every sibling target named the offending local
 *       date-time. The diagnostic is raised from inside the {@code catch}, so a successful parse pays
 *       nothing for it; a failure that is not a gap or an overlap must still get the generic message.</li>
 *   <li><b>J1</b> &mdash; the javadoc recipe for a half-open {@code isBetween} test used
 *       {@code equals}, which is not instant-based.</li>
 *   <li><b>J2/J3/J4/J5</b> &mdash; behaviour the javadoc now states explicitly, pinned here so the
 *       documentation cannot drift from it.</li>
 *   <li><b>O1/O2</b> &mdash; {@code currentGregorianCalendarPlus} had no test anywhere in the tree, and
 *       the year guard reads a year-of-era so it rejects both magnitude extremes.</li>
 * </ul>
 */
public class DatesFormatterPoolTest extends TestBase {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private TimeZone originalTimeZone;

    @BeforeEach
    public void rememberDefaults() {
        originalTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    public void restoreDefaults() {
        TimeZone.setDefault(originalTimeZone);
    }

    // ---------------------------------------------------------------------------------------------
    // B2 - an invalid pattern must not consume a pooling slot
    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> dateFormatPool() throws Exception {
        final Field field = Dates.class.getDeclaredField("dfPool");
        field.setAccessible(true);
        return (Map<Object, Object>) field.get(null);
    }

    private static int maxPooledFormats() throws Exception {
        final Field field = Dates.class.getDeclaredField("MAX_POOLED_FORMATS");
        field.setAccessible(true);
        return field.getInt(null);
    }

    /**
     * B2: patterns that cannot be compiled must leave the pool exactly as they found it. Before the fix
     * each one created an entry that no {@code DateFormat} could ever be recycled into, so
     * {@code MAX_POOLED_FORMATS} of them permanently refused admission to every later valid pattern.
     */
    @Test
    public void invalidPatterns_doNotConsumePoolSlots() throws Exception {
        TimeZone.setDefault(UTC);
        final Map<Object, Object> pool = dateFormatPool();

        // The pool never evicts, so filling it would refuse admission to every later caller in this JVM.
        // Snapshot now and restore in the finally, exactly as dtf_cacheBoundHoldsUnderConcurrency does.
        final Map<Object, Object> before = new LinkedHashMap<>(pool);

        try {
            synchronized (pool) {
                pool.clear();
            }

            final int attempts = maxPooledFormats() + 16;

            for (int i = 0; i < attempts; i++) {
                // an unterminated quote: SimpleDateFormat's constructor always throws for this
                final String invalid = "yyyy-MM-dd'unterminated" + i;
                assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(0L), invalid, UTC), "an unterminated quote must be rejected");
            }

            assertEquals(0, pool.size(), "invalid patterns must not be admitted to the DateFormat pool");

            // and the pool is still open for business: a valid pattern is admitted and then pools its
            // formatter, which is the whole point of surviving the invalid ones.
            assertEquals("1970-01-01 00:00:00", Dates.format(new Date(0L), "yyyy-MM-dd HH:mm:ss", UTC));
            assertEquals(1, pool.size(), "a valid pattern must still be admitted after invalid ones");
            assertEquals("1970-01-01 00:00:00", Dates.format(new Date(0L), "yyyy-MM-dd HH:mm:ss", UTC));
            assertEquals(1, pool.size(), "a repeated valid pattern must reuse its entry");
        } finally {
            synchronized (pool) {
                pool.clear();
                pool.putAll(before);
            }
        }
    }

    /** B2: the admission bound itself still holds - the fix must not turn the pool unbounded. */
    @Test
    public void validPatterns_stillRespectTheAdmissionBound() throws Exception {
        TimeZone.setDefault(UTC);
        final Map<Object, Object> pool = dateFormatPool();
        final Map<Object, Object> before = new LinkedHashMap<>(pool);
        final int bound = maxPooledFormats();

        try {
            synchronized (pool) {
                pool.clear();
            }

            for (int i = 0; i < bound + 40; i++) {
                Dates.format(new Date(0L), "'p" + i + "'yyyy-MM-dd", UTC);
            }

            assertEquals(bound, pool.size(), "the DateFormat pool overran its bound");
        } finally {
            synchronized (pool) {
                pool.clear();
                pool.putAll(before);
            }
        }
    }

    /**
     * B2 follow-up: the admission bound is enforced atomically. The fix moved admission out of the
     * lookup, so the {@code synchronized} block is now the only thing holding the bound - sizing and
     * inserting as separate steps would let concurrent callers push the pool past it.
     */
    @Test
    public void poolBoundHoldsUnderConcurrency() throws Exception {
        TimeZone.setDefault(UTC);
        final Map<Object, Object> pool = dateFormatPool();
        final Map<Object, Object> before = new LinkedHashMap<>(pool);
        final int bound = maxPooledFormats();
        final int threads = 16;
        final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threads);

        try {
            synchronized (pool) {
                pool.clear();
            }

            final java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            final java.util.List<java.util.concurrent.Future<?>> futures = new java.util.ArrayList<>();

            for (int t = 0; t < threads; t++) {
                final int offset = t;
                futures.add(executor.submit(() -> {
                    start.await();

                    for (int i = 0; i < 200; i++) {
                        // 400 distinct patterns raced through a pool that admits at most 64
                        Dates.format(new Date(0L), "'q" + ((offset * 200 + i) % 400) + "'yyyy-MM-dd", UTC);
                    }

                    return null;
                }));
            }

            start.countDown();

            for (final java.util.concurrent.Future<?> f : futures) {
                f.get();
            }

            assertEquals(bound, pool.size(), "the DateFormat pool overran its bound under concurrency");
        } finally {
            executor.shutdownNow();

            synchronized (pool) {
                pool.clear();
                pool.putAll(before);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B3 - DTF Calendar overloads honour the class-level null-zone fallback
    // ---------------------------------------------------------------------------------------------

    /** A {@code Calendar} whose {@code getTimeZone()} returns {@code null}; legal, and rare in the wild. */
    private static final class NullZoneCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        @Override
        public TimeZone getTimeZone() {
            return null;
        }
    }

    /**
     * B3: every {@code Calendar} operation falls back to the live default zone when the calendar has no
     * zone of its own. {@code DTF.format(Calendar)} and {@code DTF.formatTo} used to throw
     * {@code IllegalArgumentException: 'timeZone' cannot be null} instead - a message that also named a
     * parameter the caller never passed.
     */
    @Test
    public void dtfCalendarOverloads_fallBackToTheDefaultZoneForANullCalendarZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final Calendar calendar = new NullZoneCalendar();
        calendar.setTimeInMillis(1736937045123L); // 2025-01-15T10:30:45.123Z = 05:30:45.123 in New York

        assertEquals("2025-01-15 05:30:45", Dates.DTF.LOCAL_DATE_TIME.format(calendar));
        assertEquals("2025-01-15", Dates.DTF.LOCAL_DATE.format(calendar));

        final StringBuilder sb = new StringBuilder();
        Dates.DTF.LOCAL_DATE_TIME.formatTo(calendar, sb);
        assertEquals("2025-01-15 05:30:45", sb.toString());

        // a fixed-zone formatter still overrides the calendar's (missing) zone with its own
        assertEquals("2025-01-15T10:30:45.123Z", Dates.DTF.ISO_8601_TIMESTAMP.format(calendar));
        assertEquals("Wed, 15 Jan 2025 10:30:45 GMT", Dates.DTF.HTTP_DATE.format(calendar));

        // and the static siblings agree, which is what made this the odd one out
        assertEquals("2025-01-15 05:30:45", Dates.format(calendar, Dates.LOCAL_DATE_TIME_FORMAT));
        assertEquals("2025-01-15T05:30:45.123-05:00[America/New_York]", Dates.format(calendar));

        // the fallback must not swallow the other documented rejection: a calendar carrying rules no
        // ZoneId can express is still refused by a zone-sensitive pattern, and still accepted by a
        // fixed-zone one, which never consults the calendar's zone at all
        final TimeZone unrepresentable = new SimpleTimeZone(3_600_000, "CustomZone", Calendar.MARCH, -1, Calendar.SUNDAY, 3_600_000, Calendar.OCTOBER, -1,
                Calendar.SUNDAY, 3_600_000, 3_600_000);
        final Calendar custom = Calendar.getInstance(unrepresentable);
        custom.setTimeInMillis(1736937045123L);

        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.format(custom));
        assertEquals("2025-01-15T10:30:45.123Z", Dates.DTF.ISO_8601_TIMESTAMP.format(custom));
        assertEquals("Wed, 15 Jan 2025 10:30:45 GMT", Dates.DTF.HTTP_DATE.format(custom));
    }

    /** A {@code Calendar} that counts how many times its zone is read, and can change its answer. */
    private static final class CountingZoneCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        private transient int reads;
        private transient TimeZone answer;

        @Override
        public TimeZone getTimeZone() {
            reads++;
            return answer;
        }
    }

    /**
     * B3: the zone must be read exactly once - a {@code Calendar} is free to answer differently on a
     * second call - and a fixed-zone formatter must not read it at all. An earlier draft of the fix
     * hoisted the read out of the branch, which made {@code ISO_8601_TIMESTAMP} and {@code HTTP_DATE}
     * consult a zone they never use.
     */
    @Test
    public void dtfCalendarOverloads_readTheCalendarZoneExactlyOnceAndOnlyWhenNeeded() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final CountingZoneCalendar calendar = new CountingZoneCalendar();
        calendar.setTimeInMillis(1736937045123L);
        calendar.answer = TimeZone.getTimeZone("Asia/Kolkata");

        calendar.reads = 0;
        assertEquals("2025-01-15 16:00:45", Dates.DTF.LOCAL_DATE_TIME.format(calendar));
        assertEquals(1, calendar.reads, "a zone-sensitive pattern must read the calendar's zone exactly once");

        calendar.reads = 0;
        assertEquals("2025-01-15T10:30:45.123Z", Dates.DTF.ISO_8601_TIMESTAMP.format(calendar));
        assertEquals(0, calendar.reads, "a fixed-UTC formatter must not consult the calendar's zone at all");

        calendar.reads = 0;
        assertEquals("Wed, 15 Jan 2025 10:30:45 GMT", Dates.DTF.HTTP_DATE.format(calendar));
        assertEquals(0, calendar.reads, "a fixed-GMT formatter must not consult the calendar's zone at all");

        // and the single read is the one that decides: a calendar that would answer differently the
        // second time cannot make the result disagree with itself
        calendar.answer = null;
        calendar.reads = 0;
        assertEquals("2025-01-15 05:30:45", Dates.DTF.LOCAL_DATE_TIME.format(calendar));
        assertEquals(1, calendar.reads);
    }

    /** B3: the fallback follows the live default zone at call time. */
    @Test
    public void dtfCalendarOverloads_useTheLiveDefaultZone() {
        final Calendar calendar = new NullZoneCalendar();
        calendar.setTimeInMillis(1736937045123L);

        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        assertEquals("2025-01-15 16:00:45", Dates.DTF.LOCAL_DATE_TIME.format(calendar));

        TimeZone.setDefault(UTC);
        assertEquals("2025-01-15 10:30:45", Dates.DTF.LOCAL_DATE_TIME.format(calendar));
    }

    // ---------------------------------------------------------------------------------------------
    // O3 - one daylight-saving diagnostic for every parse target
    // ---------------------------------------------------------------------------------------------

    /**
     * O3: {@code parseToTimestamp} routes the standard local shapes through the JDBC resolver, which
     * rejected the same gap and overlap as its siblings but reported them as a wrapped
     * {@code "Cannot parse ..."} rather than naming the nonexistent or ambiguous local date-time.
     */
    @Test
    public void parseToTimestamp_reportsDstOverlapLikeEverySiblingTarget() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        final String ambiguous = "2025-11-02 01:30:00";

        final String expected = assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(ambiguous, Dates.LOCAL_DATE_TIME_FORMAT)).getMessage();

        assertTrue(expected.contains("Ambiguous local date-time 2025-11-02T01:30"), expected);
        assertTrue(expected.contains("(DST overlap)"), expected);

        for (final String format : new String[] { null, Dates.LOCAL_DATE_TIME_FORMAT }) {
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(ambiguous, format)).getMessage(),
                    "format=" + format);
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Dates.parseToDate(ambiguous, format)).getMessage(), "format=" + format);
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Dates.parseToCalendar(ambiguous, format)).getMessage(),
                    "format=" + format);
        }

        // the .SSS shape takes the same JDBC route and must report the same way
        final String ambiguousMillis = "2025-11-02 01:30:00.000";
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Dates.parseToTimestamp(ambiguousMillis, Dates.LOCAL_TIMESTAMP_FORMAT)).getMessage()
                .contains("(DST overlap)"));
    }

    /** O3: a spring-forward gap gets the precise message too, and nothing valid became unparseable. */
    @Test
    public void parseToTimestamp_reportsDstGapAndStillParsesValidText() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));

        final String nonexistent = "2025-03-09 02:30:00";
        final IllegalArgumentException sibling = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToJUDate(nonexistent, Dates.LOCAL_DATE_TIME_FORMAT));
        final IllegalArgumentException timestamp = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToTimestamp(nonexistent, Dates.LOCAL_DATE_TIME_FORMAT));

        assertTrue(sibling.getMessage().contains("Nonexistent local date-time 2025-03-09T02:30"), sibling.getMessage());
        assertEquals(sibling.getMessage(), timestamp.getMessage());
        // the diagnostic is raised by checkGapAndOverlap itself, so - like every sibling target - it
        // carries no cause; the JDBC resolver's own exception said the same thing and is not chained
        assertNull(sibling.getCause());
        assertNull(timestamp.getCause());

        // regression guard: the new check must not reject anything that used to parse
        assertEquals(1736937045000L, Dates.parseToTimestamp("2025-01-15 05:30:45", Dates.LOCAL_DATE_TIME_FORMAT).getTime());
        assertEquals(123_456_789, Dates.parseToTimestamp("2025-01-15 05:30:45.123456789").getNanos());
        assertEquals(1736937045123L, Dates.parseToTimestamp("2025-01-15 05:30:45.123", Dates.LOCAL_TIMESTAMP_FORMAT).getTime());

        // a zone whose rules no ZoneId expresses keeps the legacy resolution, with no overlap check
        final TimeZone unrepresentable = new SimpleTimeZone(3_600_000, "CustomZone", Calendar.MARCH, -1, Calendar.SUNDAY, 3_600_000, Calendar.OCTOBER, -1,
                Calendar.SUNDAY, 3_600_000, 3_600_000);
        assertNotNull(Dates.parseToTimestamp("2025-11-02 01:30:00", Dates.LOCAL_DATE_TIME_FORMAT, unrepresentable));

        // a failure that is NOT a gap or an overlap still falls through to the generic wrapped message:
        // the diagnostic is computed on the failure path, so it must stay silent for everything else
        final String invalidDay = "2025-02-30 10:30:45";
        final IllegalArgumentException generic = assertThrows(IllegalArgumentException.class,
                () -> Dates.parseToTimestamp(invalidDay, Dates.LOCAL_DATE_TIME_FORMAT));

        assertTrue(generic.getMessage().startsWith("Cannot parse \"2025-02-30 10:30:45\""), generic.getMessage());
        assertFalse(generic.getMessage().contains("DST"), generic.getMessage());
        assertNotNull(generic.getCause(), "the generic path must still chain the resolver's exception");
    }

    // ---------------------------------------------------------------------------------------------
    // J1 - the documented half-open composition
    // ---------------------------------------------------------------------------------------------

    /**
     * J1: {@code isBetween} compares instants, so a half-open test must exclude the end by instant too.
     * The javadoc used to recommend {@code !x.equals(end)}; {@code Timestamp.equals} rejects a plain
     * {@code java.util.Date} at the same instant, so the recommended composition reported a value that
     * <i>is</i> the end boundary as strictly inside the range.
     */
    @Test
    public void isBetween_halfOpenCompositionUsesIsSameInstant() {
        final Date start = new Date(1000L);
        final Date end = new Date(3000L);
        final Timestamp atEnd = new Timestamp(3000L);

        assertTrue(Dates.isSameInstant(atEnd, end));
        assertTrue(Dates.isBetween(atEnd, start, end));

        // the documented recipe
        assertFalse(Dates.isBetween(atEnd, start, end) && !Dates.isSameInstant(atEnd, end), "the end boundary must be excluded");
        assertTrue(Dates.isBetween(new Timestamp(2000L), start, end) && !Dates.isSameInstant(new Timestamp(2000L), end));

        // why equals cannot stand in: it is neither instant-based nor symmetric across these types
        assertFalse(atEnd.equals(end));
        assertTrue(end.equals(atEnd));
    }

    /** J1: the {@code Calendar} overload has the same trap - {@code Calendar.equals} compares the zone. */
    @Test
    public void isBetween_calendarHalfOpenCompositionUsesIsSameInstant() {
        final Calendar start = Dates.createCalendar(1000L, UTC);
        final Calendar end = Dates.createCalendar(3000L, UTC);
        final Calendar atEndElsewhere = Dates.createCalendar(3000L, TimeZone.getTimeZone("Asia/Kolkata"));

        assertTrue(Dates.isSameInstant(atEndElsewhere, end));
        assertTrue(Dates.isBetween(atEndElsewhere, start, end));

        assertFalse(Dates.isBetween(atEndElsewhere, start, end) && !Dates.isSameInstant(atEndElsewhere, end), "the end boundary must be excluded");

        assertFalse(atEndElsewhere.equals(end));
    }

    // ---------------------------------------------------------------------------------------------
    // J3 - the resolved-civil-boundary contract the javadoc now states
    // ---------------------------------------------------------------------------------------------

    private static String render(final long millis, final String zoneId) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.of(zoneId)).toString();
    }

    /**
     * J3: a gap resolves the boundary it removes onto the instant the gap ends, so the result stays in
     * the same civil unit as the input and can therefore show non-zero finer fields. Pacific/Chatham
     * moves 02:45 to 03:45, so civil hour 03 begins at 03:45 that morning.
     */
    @Test
    public void boundaryOps_resolveAGapOntoItsEndEvenWhenThatIsNotAlignedToTheUnit() {
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Chatham"));
        final long millis = ZonedDateTime.of(2025, 9, 28, 3, 45, 0, 0, ZoneId.of("Pacific/Chatham")).toInstant().toEpochMilli();
        final Date input = new Date(millis);

        assertEquals("2025-09-28T03:45+13:45[Pacific/Chatham]", render(Dates.truncate(input, Calendar.HOUR_OF_DAY).getTime(), "Pacific/Chatham"));
        assertEquals("2025-09-28T03:45+13:45[Pacific/Chatham]", render(Dates.ceiling(input, Calendar.HOUR_OF_DAY).getTime(), "Pacific/Chatham"));
        assertEquals("2025-09-28T03:45+13:45[Pacific/Chatham]", render(Dates.round(input, Calendar.HOUR_OF_DAY).getTime(), "Pacific/Chatham"));

        // the guarantees that do hold everywhere
        assertTrue(Dates.truncate(input, Calendar.HOUR_OF_DAY).getTime() <= millis, "truncation must never move a value forward");
        assertTrue(Dates.ceiling(input, Calendar.HOUR_OF_DAY).getTime() >= millis, "a ceiling must never move a value backward");

        // past the top of the next hour the gap is behind us and the ordinary aligned answer returns
        final Date later = new Date(millis + 20 * 60_000L);
        assertEquals("2025-09-28T04:00+13:45[Pacific/Chatham]", render(Dates.truncate(later, Calendar.HOUR_OF_DAY).getTime(), "Pacific/Chatham"));
    }

    /** J3: the same rule with a 30-minute shift, and the aligned control that hides it. */
    @Test
    public void boundaryOps_gapCollapseIsUnaffectedByAnAlignedTransition() {
        TimeZone.setDefault(TimeZone.getTimeZone("Australia/Lord_Howe"));
        final long lordHowe = ZonedDateTime.of(2025, 10, 5, 2, 30, 0, 0, ZoneId.of("Australia/Lord_Howe")).toInstant().toEpochMilli();
        assertEquals("2025-10-05T02:30+11:00[Australia/Lord_Howe]",
                render(Dates.truncate(new Date(lordHowe), Calendar.HOUR_OF_DAY).getTime(), "Australia/Lord_Howe"));

        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        final long newYork = ZonedDateTime.of(2025, 3, 9, 3, 0, 0, 0, ZoneId.of("America/New_York")).toInstant().toEpochMilli();
        assertEquals("2025-03-09T03:00-04:00[America/New_York]", render(Dates.truncate(new Date(newYork), Calendar.HOUR_OF_DAY).getTime(), "America/New_York"));
    }

    /**
     * J3: the gap collapse is what keeps a truncation inside the same civil day. America/Sao_Paulo had no
     * midnight on 4 November 2018, and the day still starts that day rather than the previous one.
     */
    @Test
    public void truncateToDate_staysInTheSameCivilDayWhenMidnightDoesNotExist() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        final ZoneId zone = ZoneId.of("America/Sao_Paulo");
        final long millis = ZonedDateTime.of(2018, 11, 4, 10, 30, 0, 0, zone).toInstant().toEpochMilli();

        final long truncated = Dates.truncate(new Date(millis), Calendar.DATE).getTime();

        assertEquals("2018-11-04T01:00-02:00[America/Sao_Paulo]", render(truncated, "America/Sao_Paulo"));
        assertEquals(LocalDate.of(2018, 11, 4), Instant.ofEpochMilli(truncated).atZone(zone).toLocalDate());
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli(), truncated);
    }

    /**
     * J3: an overlap yields two boundaries and the one on the required side of the value is taken - the
     * later occurrence for a truncation at or before it, the earlier one for a ceiling after it.
     */
    @Test
    public void boundaryOps_takeTheOccurrenceOnTheRequiredSideOfAnOverlap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Havana"));
        final ZoneId zone = ZoneId.of("America/Havana");

        // 2026-11-01 00:59:59.999-04:00 is one millisecond before midnight replays at -05:00
        final long millis = java.time.OffsetDateTime.parse("2026-11-01T00:59:59.999-04:00").toInstant().toEpochMilli();

        assertEquals("2026-11-01T00:00-04:00[America/Havana]", render(Dates.truncate(new Date(millis), Calendar.AM_PM).getTime(), "America/Havana"));
        assertEquals("2026-11-01T00:00-05:00[America/Havana]", render(Dates.ceiling(new Date(millis), Calendar.AM_PM).getTime(), "America/Havana"));

        // both are real instants of the same nominal local boundary
        assertEquals(2, zone.getRules().getValidOffsets(java.time.LocalDateTime.of(2026, 11, 1, 0, 0)).size());
    }

    // ---------------------------------------------------------------------------------------------
    // J2 - the failure modes truncatedEquals/truncatedCompareTo inherit from truncate
    // ---------------------------------------------------------------------------------------------

    /** A {@code Calendar} that cannot be rebuilt: no usable constructor, and a {@code clone()} that lies. */
    private static final class HostileCalendar extends GregorianCalendar {
        private static final long serialVersionUID = 1L;

        HostileCalendar(final long millis, final TimeZone zone) {
            super(zone);
            setTimeInMillis(millis);
        }

        @Override
        public Object clone() {
            return this; // not a distinct instance, which the subtype contract forbids
        }
    }

    /** The {@code java.util.Date} counterpart: no declared {@code (long)} constructor, and a lying clone. */
    private static final class HostileDate extends Date {
        private static final long serialVersionUID = 1L;

        HostileDate() {
            super(1736937045123L);
        }

        @Override
        public Object clone() {
            return this;
        }
    }

    /**
     * J2: these eight methods delegate to {@code truncate} and therefore inherit all of its failure
     * modes; the javadoc used to promise only the {@code null} one.
     */
    @Test
    public void truncatedComparisons_throwEverythingTruncateThrows() {
        TimeZone.setDefault(UTC);

        // an unsupported field
        final Date epoch = new Date(0L);
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(epoch, epoch, Calendar.WEEK_OF_YEAR));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo(epoch, epoch, Calendar.ERA));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(Dates.createCalendar(0L), Dates.createCalendar(0L), Calendar.DAY_OF_WEEK));

        // a time zone no ZoneId can represent - for the Calendar overloads it is the calendar's own...
        final TimeZone unrepresentable = new SimpleTimeZone(3_600_000, "CustomZone", Calendar.MARCH, -1, Calendar.SUNDAY, 3_600_000, Calendar.OCTOBER, -1,
                Calendar.SUNDAY, 3_600_000, 3_600_000);
        final Calendar custom = Calendar.getInstance(unrepresentable);
        custom.setTimeInMillis(1736937045123L);
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo(custom, custom, Calendar.DATE));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(custom, custom, CalendarField.DAY_OF_MONTH));

        // ...and for the Date overloads it is the JVM default, which the javadoc now says as well
        final Date value = new Date(1736937045123L);
        TimeZone.setDefault(unrepresentable);
        try {
            assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo(value, value, Calendar.DATE));
            assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals(value, value, CalendarField.DAY_OF_MONTH));
        } finally {
            TimeZone.setDefault(UTC);
        }

        // a year magnitude past the guard, in both directions, on both families
        assertThrows(ArithmeticException.class, () -> Dates.truncatedEquals(new Date(Long.MAX_VALUE), new Date(Long.MAX_VALUE), Calendar.YEAR));
        assertThrows(ArithmeticException.class, () -> Dates.truncatedCompareTo(new Date(Long.MIN_VALUE), new Date(Long.MIN_VALUE), Calendar.YEAR));

        final Calendar huge = Dates.createCalendar(Long.MAX_VALUE, UTC);
        assertThrows(ArithmeticException.class, () -> Dates.truncatedCompareTo(huge, huge, Calendar.YEAR));
        assertThrows(ArithmeticException.class, () -> Dates.truncatedEquals(huge, huge, CalendarField.YEAR));

        // a subtype that cannot be rebuilt no longer matters to a comparison (2026-09-01 pass): the
        // truncated instants are compared as longs, so the creator/clone contract is neither invoked
        // nor able to fail it - while truncate itself, which does build a result, still enforces it
        final Calendar hostileCalendar = new HostileCalendar(1736937045123L, UTC);
        assertEquals(0, Dates.truncatedCompareTo(hostileCalendar, hostileCalendar, Calendar.DATE));
        assertThrows(IllegalStateException.class, () -> Dates.truncate(hostileCalendar, Calendar.DATE));

        final Date hostileDate = new HostileDate();
        assertEquals(0, Dates.truncatedCompareTo(hostileDate, hostileDate, Calendar.DATE));
        assertTrue(Dates.truncatedEquals(hostileDate, hostileDate, CalendarField.DAY_OF_MONTH));
        assertThrows(IllegalStateException.class, () -> Dates.truncate(hostileDate, Calendar.DATE));

        // and the null contract still holds
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedEquals((Date) null, epoch, Calendar.DATE));
        assertThrows(IllegalArgumentException.class, () -> Dates.truncatedCompareTo((Calendar) null, Dates.createCalendar(0L), Calendar.DATE));
    }

    /** O2: the guard reads a year-of-era, so the most negative values trip it as well as the largest. */
    @Test
    public void boundaryOps_rejectBothYearMagnitudeExtremes() {
        TimeZone.setDefault(UTC);

        assertThrows(ArithmeticException.class, () -> Dates.truncate(new Date(Long.MAX_VALUE), Calendar.YEAR));
        assertThrows(ArithmeticException.class, () -> Dates.truncate(new Date(Long.MIN_VALUE), Calendar.YEAR));
        assertThrows(ArithmeticException.class, () -> Dates.ceiling(new Date(Long.MIN_VALUE), Calendar.MONTH));
        assertThrows(ArithmeticException.class, () -> Dates.round(new Date(Long.MIN_VALUE), Calendar.DATE));
    }

    // ---------------------------------------------------------------------------------------------
    // J5 - isSameLocalTime(Calendar, Calendar) is zone-blind, unlike its isSame* siblings
    // ---------------------------------------------------------------------------------------------

    /**
     * J5: two calendars in different zones showing the same wall clock are the same local time, while
     * {@code isSameDay}/{@code isSameMonth}/{@code isSameYear} reject calendars whose zones disagree.
     */
    @Test
    public void isSameLocalTime_ignoresBothZonesUnlikeTheOtherIsSameComparisons() {
        TimeZone.setDefault(UTC);

        // 2025-01-15T10:30:45.000 local in each of two different zones: same wall clock, different instant
        final Calendar utcCal = Dates.createCalendar(Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, UTC).getTime(), UTC);
        final TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        final Calendar kolkataCal = Dates.createCalendar(Dates.parseToJUDate("2025-01-15 10:30:45", Dates.LOCAL_DATE_TIME_FORMAT, kolkata).getTime(), kolkata);

        assertFalse(Dates.isSameInstant(utcCal, kolkataCal));
        assertTrue(Dates.isSameLocalTime(utcCal, kolkataCal), "the wall-clock fields match, and the zones are not compared");

        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(utcCal, kolkataCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(utcCal, kolkataCal));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(utcCal, kolkataCal));

        // the same-runtime-type requirement still stands
        final Calendar sameFieldsOtherType = new NullZoneCalendar();
        sameFieldsOtherType.setTimeInMillis(utcCal.getTimeInMillis());
        assertFalse(Dates.isSameLocalTime(utcCal, sameFieldsOtherType));
    }

    // ---------------------------------------------------------------------------------------------
    // J4 - DTF.of(pattern, locale) is the documented route for localized text on the java.time targets
    // ---------------------------------------------------------------------------------------------

    /**
     * J4: the {@code java.time} {@code parseTo*} entry points have no locale parameter and always read a
     * custom pattern with {@code Locale.US}; the class javadoc now names {@code DTF.of(String, Locale)}
     * instead of pointing at overloads that do not exist.
     */
    @Test
    public void javaTimeParseTargets_areUsEnglishAndDelegateLocalizedTextToDtf() {
        TimeZone.setDefault(UTC);
        final String german = "15 Jan. 2025";
        final String pattern = "dd MMM yyyy";

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToLocalDate(german, pattern));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(german, pattern, UTC));

        assertEquals(LocalDate.of(2025, 1, 15), Dates.DTF.of(pattern, Locale.GERMANY).parseToLocalDate(german));

        // the legacy targets do have the locale overload the paragraph describes
        assertEquals(1736899200000L, Dates.parseToJUDate(german, pattern, UTC, Locale.GERMANY).getTime());

        // and the US default still reads US text
        assertEquals(LocalDate.of(2025, 1, 15), Dates.parseToLocalDate("15 Jan 2025", pattern));
    }

    // ---------------------------------------------------------------------------------------------
    // O1 - currentGregorianCalendarPlus had no coverage anywhere in the tree
    // ---------------------------------------------------------------------------------------------

    /** O1: the {@code GregorianCalendar} counterpart of {@code currentCalendarPlus}. */
    @Test
    public void currentGregorianCalendarPlus_appliesTheAmountAndValidatesItsArguments() {
        TimeZone.setDefault(UTC);

        final long before = System.currentTimeMillis();
        final GregorianCalendar nextHour = Dates.currentGregorianCalendarPlus(1, TimeUnit.HOURS);
        final GregorianCalendar lastWeek = Dates.currentGregorianCalendarPlus(-7, TimeUnit.DAYS);
        final long after = System.currentTimeMillis();

        assertNotNull(nextHour);
        assertEquals(GregorianCalendar.class, nextHour.getClass());
        assertTrue(nextHour.getTimeInMillis() >= before + TimeUnit.HOURS.toMillis(1));
        assertTrue(nextHour.getTimeInMillis() <= after + TimeUnit.HOURS.toMillis(1));
        assertTrue(lastWeek.getTimeInMillis() <= after - TimeUnit.DAYS.toMillis(7));
        assertTrue(nextHour.getTimeInMillis() > lastWeek.getTimeInMillis());

        // each call returns a distinct object
        assertNotSame(Dates.currentGregorianCalendarPlus(0, TimeUnit.SECONDS), Dates.currentGregorianCalendarPlus(0, TimeUnit.SECONDS));

        // A sub-millisecond amount adds nothing, so the result is just a clock reading taken during the
        // call. Bracketing it is the only clock-independent assertion available here; the truncation rule
        // itself is pinned deterministically below, on the same toMillisExact conversion.
        final long lo = System.currentTimeMillis();
        final long sub = Dates.currentGregorianCalendarPlus(500, TimeUnit.MICROSECONDS).getTimeInMillis();
        final long hi = System.currentTimeMillis();
        assertTrue(sub >= lo && sub <= hi, "a sub-millisecond amount must not move the clock reading");

        // toMillisExact truncates toward zero in both directions, and overflows exactly rather than
        // saturating the way TimeUnit.toMillis does
        final Date fixed = new Date(1000L);
        assertEquals(1000L, Dates.roll(fixed, 999_999, TimeUnit.NANOSECONDS).getTime());
        assertEquals(1001L, Dates.roll(fixed, 1_000_000, TimeUnit.NANOSECONDS).getTime());
        assertEquals(1000L, Dates.roll(fixed, -999_999, TimeUnit.NANOSECONDS).getTime());
        assertEquals(999L, Dates.roll(fixed, -1_000_000, TimeUnit.NANOSECONDS).getTime());
        assertEquals(1000L, Dates.roll(fixed, 999, TimeUnit.MICROSECONDS).getTime());

        assertThrows(IllegalArgumentException.class, () -> Dates.currentGregorianCalendarPlus(1, null));
        assertThrows(ArithmeticException.class, () -> Dates.currentGregorianCalendarPlus(Long.MAX_VALUE, TimeUnit.DAYS));
    }
}
