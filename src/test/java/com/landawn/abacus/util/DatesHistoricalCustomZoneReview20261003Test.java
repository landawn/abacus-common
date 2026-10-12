package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

/** A zone's current DST flags do not establish that its historical offsets are fixed. */
@Tag("unit")
class DatesHistoricalCustomZoneReview20261003Test extends TestBase {
    private static final String SUMMER_TEXT = "1990-07-01 12:00:00";
    private static final Instant SUMMER_INSTANT = Instant.parse("1990-07-01T03:00:00Z");

    @ParameterizedTest
    @ValueSource(strings = { "Asia/Shanghai", "UTC", "Review/Unknown" })
    void unverifiedWrapperIsRejectedInsteadOfFlattened(final String id) {
        final TimeZone wrapper = new DelegatingZone("Asia/Shanghai", id);
        assertFalse(wrapper.useDaylightTime());
        assertEquals(0, wrapper.getDSTSavings());
        assertEquals(8 * 3_600_000, wrapper.getRawOffset());
        assertEquals(9 * 3_600_000, wrapper.getOffset(SUMMER_INSTANT.toEpochMilli()));

        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(SUMMER_TEXT, null, wrapper));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToOffsetDateTime(SUMMER_TEXT, null, wrapper));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToZonedDateTime(SUMMER_TEXT, null, wrapper));
        assertThrows(IllegalArgumentException.class, () -> Dates.DTF.LOCAL_DATE_TIME.parseToInstant(SUMMER_TEXT, wrapper));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(Date.from(SUMMER_INSTANT), Dates.ISO_ZONED_DATE_TIME_FORMAT, wrapper));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(Date.from(SUMMER_INSTANT), Date.from(SUMMER_INSTANT), wrapper));
    }

    @ParameterizedTest
    @CsvSource({ "1990-07-01 12:00:00, 1990-07-01T03:00:00Z", "1990-01-01 12:00:00, 1990-01-01T04:00:00Z", "1991-08-01 12:00:00, 1991-08-01T03:00:00Z",
            "2025-07-01 12:00:00, 2025-07-01T04:00:00Z" })
    void registeredZoneKeepsHistoricalAndModernOffsets(final String text, final String instantText) {
        final TimeZone registered = TimeZone.getTimeZone("Asia/Shanghai");
        final Instant expected = Instant.parse(instantText);
        assertEquals(expected, Dates.parseToInstant(text, null, registered));
        assertEquals(expected, Dates.parseToOffsetDateTime(text, null, registered).toInstant());
        assertEquals(ZoneId.of("Asia/Shanghai"), Dates.parseToZonedDateTime(text, null, registered).getZone());
        assertEquals(expected, Dates.DTF.LOCAL_DATE_TIME.parseToInstant(text, registered));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Asia/Shanghai", "UTC", "Review/Unknown" })
    void calendarEngineAndTextualOffsetsStillWorkWithCustomRules(final String id) {
        final TimeZone wrapper = new DelegatingZone("Asia/Shanghai", id);
        // Calendar itself cannot resolve this wrapper's historical DST with zero current DST savings.
        // Dates must preserve that rejection, rather than bypass it with an invented fixed offset.
        final SimpleDateFormat legacy = new SimpleDateFormat(Dates.LOCAL_DATE_TIME_FORMAT, Locale.ROOT);
        legacy.setTimeZone(wrapper);
        legacy.setLenient(false);
        assertThrows(ParseException.class, () -> legacy.parse(SUMMER_TEXT));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(SUMMER_TEXT, Dates.LOCAL_DATE_TIME_FORMAT, wrapper));
        final String winterText = "1990-01-01 12:00:00";
        final Instant winter = Instant.parse("1990-01-01T04:00:00Z");
        assertEquals(winter, Dates.parseToJUDate(winterText, Dates.LOCAL_DATE_TIME_FORMAT, wrapper).toInstant());
        assertEquals(winter.plusNanos(123_456_789), Dates.parseToTimestamp(winterText + ".123456789", null, wrapper).toInstant());
        assertEquals(SUMMER_INSTANT, Dates.parseToInstant("1990-07-01T12:00:00+09:00", null, wrapper));
        assertEquals(SUMMER_INSTANT, Dates.DTF.ISO_OFFSET_DATE_TIME.parseToInstant("1990-07-01T12:00:00+09:00", wrapper));
    }

    @Test
    void inheritedRuleComparisonDoesNotProveUtcEquivalence() {
        final TimeZone wrapper = new DelegatingZone("Africa/Abidjan", "UTC");
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        assertFalse(wrapper.useDaylightTime());
        assertEquals(0, wrapper.getRawOffset());
        assertTrue(wrapper.hasSameRules(utc));
        assertTrue(utc.hasSameRules(wrapper));
        assertEquals(-968_000, wrapper.getOffset(Instant.parse("1900-01-01T00:00:00Z").toEpochMilli()));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("1900-01-01 00:00:00", null, wrapper));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant("1900-01-01T00:00:00Z", Dates.ISO_8601_DATE_TIME_FORMAT, wrapper));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(0), Dates.ISO_8601_DATE_TIME_FORMAT, wrapper));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void calendarComparisonValidatesBothOperands(final boolean reverse) {
        final Calendar utc = Dates.createCalendar(0, TimeZone.getTimeZone("UTC"));
        final Calendar wrapped = Dates.createCalendar(0, new DelegatingZone("Africa/Abidjan", "UTC"));
        final Calendar first = reverse ? wrapped : utc;
        final Calendar second = reverse ? utc : wrapped;
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(first, second));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameMonth(first, second));
        assertThrows(IllegalArgumentException.class, () -> Dates.isSameYear(first, second));
    }

    @Test
    void implicitCivilQueriesKeepRegisteredHistoryForCustomDefault() {
        final TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(new DelegatingZone("Asia/Shanghai", "Asia/Shanghai"));
            final Date before = Date.from(Instant.parse("1990-06-30T15:30:00Z"));
            final Date after = Date.from(Instant.parse("1990-06-30T16:30:00Z"));
            assertTrue(Dates.isSameDay(before, after)); // 00:30 and 01:30 at the historical +09:00 offset.
            assertEquals(Dates.isSameMonth(before, after, ZoneId.of("Asia/Shanghai")), Dates.isSameMonth(before, after));
            assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(SUMMER_TEXT));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void implicitCivilQueriesRejectUnknownCustomDefault() {
        final TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(new DelegatingZone("Asia/Shanghai", "Review/Unknown"));
            assertThrows(IllegalArgumentException.class, () -> Dates.isSameDay(new Date(0), new Date(0)));
            assertThrows(IllegalArgumentException.class, () -> Dates.lengthOfMonth(new Date(0)));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void simpleTimeZoneSubclassCannotHideHistoricalOverrides() {
        final TimeZone wrapper = new SimpleTimeZone(8 * 3_600_000, "Asia/Shanghai") {
            private static final long serialVersionUID = 1L;
            private final TimeZone delegate = TimeZone.getTimeZone("Asia/Shanghai");

            @Override
            public int getOffset(final long date) {
                return delegate.getOffset(date);
            }

            @Override
            public int getOffset(final int era, final int year, final int month, final int day, final int dayOfWeek, final int millis) {
                return delegate.getOffset(era, year, month, day, dayOfWeek, millis);
            }
        };
        assertFalse(wrapper.useDaylightTime());
        assertEquals(9 * 3_600_000, wrapper.getOffset(SUMMER_INSTANT.toEpochMilli()));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(SUMMER_TEXT, null, wrapper));
    }

    static Stream<Arguments> provenFixedZones() {
        return Stream.of("Asia/Shanghai", "America/New_York", "UTC", "Review/Unknown")
                .flatMap(id -> Stream.of(-64_800_000, 0, 1_000, 28_800_000, 64_800_000).map(offset -> Arguments.of(id, offset)));
    }

    @ParameterizedTest
    @MethodSource("provenFixedZones")
    void exactSimpleTimeZoneRemainsSupported(final String id, final int offsetMillis) {
        final TimeZone fixed = new SimpleTimeZone(offsetMillis, id);
        final LocalDateTime local = LocalDateTime.of(1990, 7, 1, 12, 0);
        final ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
        assertEquals(local.toInstant(offset), Dates.parseToInstant(SUMMER_TEXT, null, fixed));
        assertEquals(offset, Dates.parseToZonedDateTime(SUMMER_TEXT, null, fixed).getZone());
        final TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(fixed);
            final Date before = Date.from(SUMMER_INSTANT);
            final Date after = Date.from(SUMMER_INSTANT.plusSeconds(50_000));
            assertEquals(Dates.isSameDay(before, after, offset), Dates.isSameDay(before, after));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /** Honest delegation includes historical transitions even when the current DST flags are false/zero. */
    private static final class DelegatingZone extends TimeZone {
        private static final long serialVersionUID = 1L;
        private final TimeZone delegate;

        DelegatingZone(final String region, final String id) {
            delegate = TimeZone.getTimeZone(region);
            setID(id);
        }

        @Override
        public int getOffset(final long date) {
            return delegate.getOffset(date);
        }

        @Override
        public int getOffset(final int era, final int year, final int month, final int day, final int dayOfWeek, final int millis) {
            return delegate.getOffset(era, year, month, day, dayOfWeek, millis);
        }

        @Override
        public void setRawOffset(final int offset) {
            delegate.setRawOffset(offset);
        }

        @Override
        public int getRawOffset() {
            return delegate.getRawOffset();
        }

        @Override
        public int getDSTSavings() {
            return delegate.getDSTSavings();
        }

        @Override
        public boolean useDaylightTime() {
            return delegate.useDaylightTime();
        }

        @Override
        public boolean inDaylightTime(final Date date) {
            return delegate.inDaylightTime(date);
        }
    }
}
