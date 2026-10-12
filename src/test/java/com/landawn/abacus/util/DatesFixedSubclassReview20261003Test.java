package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DatesFixedSubclassReview20261003Test extends TestBase {
    private static final String TEXT = "2025-01-15 10:30:45";
    private static final LocalDateTime LOCAL = LocalDateTime.of(2025, 1, 15, 10, 30, 45);

    static Stream<Arguments> fixedZones() {
        return Stream.of("Review/Fixed", "UTC", "America/New_York", "Asia/Shanghai")
                .flatMap(id -> Stream.of(0, 3_600_000, -19_800_000, 1_000, -64_800_000, 64_800_000).map(offset -> Arguments.of(id, offset)));
    }

    @ParameterizedTest
    @MethodSource("fixedZones")
    void unverifiedSubclassIsRejectedWhilePlainFixedZoneIsAccepted(final String id, final int offsetMillis) {
        // A fixture being fixed does not make arbitrary TimeZone subclasses verifiably fixed to Dates.
        final TimeZone custom = fixedZone(id, offsetMillis);
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(TEXT, null, custom));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToOffsetDateTime(TEXT, null, custom));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToZonedDateTime(TEXT, null, custom));
        final TimeZone zone = new SimpleTimeZone(offsetMillis, id);
        final ZoneOffset offset = ZoneOffset.ofTotalSeconds(offsetMillis / 1000);
        final Instant expected = LOCAL.toInstant(offset);

        assertEquals(expected, Dates.parseToInstant(TEXT, null, zone));
        assertEquals(LOCAL.atOffset(offset), Dates.parseToOffsetDateTime(TEXT, null, zone));
        final ZonedDateTime zoned = Dates.parseToZonedDateTime(TEXT, null, zone);
        assertEquals(LOCAL, zoned.toLocalDateTime());
        assertEquals(offset, zoned.getOffset());
        assertEquals(expected, Dates.DTF.LOCAL_DATE_TIME.parseToInstant(TEXT, zone));
        assertEquals(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(LOCAL.atOffset(offset)) + '[' + zoned.getZone().getId() + ']',
                Dates.format(Date.from(expected), Dates.ISO_ZONED_DATE_TIME_FORMAT, zone));
        assertEquals(Dates.isSameDay(Date.from(expected), Date.from(expected.plusSeconds(50_000)), offset),
                Dates.isSameDay(Date.from(expected), Date.from(expected.plusSeconds(50_000)), zone));
    }

    @ParameterizedTest
    @ValueSource(strings = { "UTC", "America/New_York", "Review/Fixed" })
    void zeroOffsetRequiresVerifiableRulesForFixedUtcFormats(final String id) {
        final TimeZone custom = fixedZone(id, 0);
        final TimeZone zone = new SimpleTimeZone(0, id);
        final Instant expected = LOCAL.toInstant(ZoneOffset.UTC);
        final String text = "2025-01-15T10:30:45Z";
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(text, Dates.ISO_8601_DATE_TIME_FORMAT, custom));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToJUDate(text, Dates.ISO_8601_DATE_TIME_FORMAT, custom));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(Date.from(expected), Dates.ISO_8601_DATE_TIME_FORMAT, custom));
        assertEquals(expected, Dates.parseToInstant(text, Dates.ISO_8601_DATE_TIME_FORMAT, zone));
        assertEquals(expected.toEpochMilli(), Dates.parseToJUDate(text, Dates.ISO_8601_DATE_TIME_FORMAT, zone).getTime());
        assertEquals(text, Dates.format(Date.from(expected), Dates.ISO_8601_DATE_TIME_FORMAT, zone));
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(text, Dates.ISO_8601_DATE_TIME_FORMAT, fixedZone(id, 3_600_000)));
    }

    static Stream<Arguments> invalidOffsets() {
        return Stream.of("Review/Fixed", "UTC", "America/New_York")
                .flatMap(id -> Stream.of(500, -500, 64_801_000, -64_801_000).map(offset -> Arguments.of(id, offset)));
    }

    @ParameterizedTest
    @MethodSource("invalidOffsets")
    void unrepresentableOffsetsStillFail(final String id, final int offsetMillis) {
        final TimeZone zone = fixedZone(id, offsetMillis);
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(TEXT, null, zone));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(0), Dates.ISO_ZONED_DATE_TIME_FORMAT, zone));
        final TimeZone plain = new SimpleTimeZone(offsetMillis, id);
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(TEXT, null, plain));
        assertThrows(IllegalArgumentException.class, () -> Dates.format(new Date(0), Dates.ISO_ZONED_DATE_TIME_FORMAT, plain));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Asia/Shanghai", "Asia/Kolkata" })
    void registeredRegionsKeepHistoricalRules(final String id) {
        final TimeZone zone = TimeZone.getTimeZone(id);
        final ZoneId region = ZoneId.of(id);
        final LocalDateTime historical = LocalDateTime.of(1880, 1, 15, 10, 30, 45);
        assertFalse(zone.useDaylightTime());
        assertFalse(region.getRules().isFixedOffset());
        assertEquals(historical.atZone(region), Dates.parseToZonedDateTime("1880-01-15 10:30:45", null, zone));
        assertEquals(historical.atZone(region).toInstant(), Dates.parseToInstant("1880-01-15 10:30:45", null, zone));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Asia/Shanghai", "Asia/Kolkata" })
    void mutatedNativeRegionsAreNotTreatedAsFixed(final String id) {
        final TimeZone zone = TimeZone.getTimeZone(id);
        zone.setRawOffset(zone.getRawOffset() + 3_600_000);
        assertFalse(zone.useDaylightTime());
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(TEXT, null, zone));
    }

    @ParameterizedTest
    @ValueSource(strings = { "UTC", "America/New_York", "Review/Fixed" })
    void simpleFixedAndCustomDaylightControls(final String id) {
        assertEquals(LOCAL.toInstant(ZoneOffset.ofHours(1)), Dates.parseToInstant(TEXT, null, new SimpleTimeZone(3_600_000, id)));
        final TimeZone daylight = new SimpleTimeZone(3_600_000, id, Calendar.MARCH, 1, 0, 0, Calendar.OCTOBER, 1, 0, 0, 3_600_000);
        assertThrows(IllegalArgumentException.class, () -> Dates.parseToInstant(TEXT, null, daylight));
    }

    private static TimeZone fixedZone(final String id, final int offsetMillis) {
        final TimeZone zone = new TimeZone() {
            private static final long serialVersionUID = 1L;

            @Override
            public int getOffset(final int era, final int year, final int month, final int day, final int dayOfWeek, final int milliseconds) {
                return offsetMillis;
            }

            @Override
            public void setRawOffset(final int offset) {
                // This fixture has immutable rules.
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
}
