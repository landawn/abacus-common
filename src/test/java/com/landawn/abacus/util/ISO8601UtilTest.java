package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.ParsePosition;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ISO8601UtilTest extends TestBase {

    @Test
    public void format_utcIsExactAndCanonical() {
        assertEquals("2023-12-25T10:30:45Z", ISO8601Util.format(Instant.parse("2023-12-25T10:30:45Z")));
        assertEquals("2023-12-25T10:30:45.12Z", ISO8601Util.format(Instant.parse("2023-12-25T10:30:45.120Z")));
        assertEquals("2023-12-25T10:30:45.123456789Z", ISO8601Util.format(Instant.parse("2023-12-25T10:30:45.123456789Z")));
        assertEquals("2023-12-25T10:30:45.000000001Z", ISO8601Util.format(Instant.parse("2023-12-25T10:30:45.000000001Z")));
    }

    @Test
    public void format_usesEffectiveOffsetWithoutLosingPrecision() {
        final Instant instant = Instant.parse("2023-12-25T15:30:45.123456789Z");

        assertEquals("2023-12-25T10:30:45.123456789-05:00", ISO8601Util.format(instant, ZoneOffset.ofHours(-5)));
        assertEquals("2023-12-26T09:30:45.123456789+18:00", ISO8601Util.format(instant, ZoneOffset.ofHours(18)));
    }

    @Test
    public void format_rejectsUnrepresentableValuesAndNulls() {
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(null));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(Instant.EPOCH, null));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(Instant.EPOCH, ZoneOffset.ofTotalSeconds(30)));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(Instant.MIN));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(Instant.MAX));

        final Instant yearZero = LocalDateTime.of(0, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
        final Instant year10000 = LocalDateTime.of(10000, 1, 1, 0, 0).toInstant(ZoneOffset.UTC);
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(yearZero));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(year10000));
    }

    @Test
    public void formatAndParse_roundTripNanoseconds() {
        final Instant original = Instant.parse("2023-12-25T10:30:45.123456789Z");

        assertEquals(original, ISO8601Util.parseInstant(ISO8601Util.format(original)));
        assertEquals(original, ISO8601Util.parseInstant(ISO8601Util.format(original, ZoneOffset.ofHoursMinutes(5, 30))));
    }

    @Test
    public void parse_acceptsExtendedAndBasicForms() {
        final Instant expected = Instant.parse("2023-12-25T10:30:45Z");

        assertEquals(Instant.parse("2023-12-25T00:00:00Z"), ISO8601Util.parseInstant("2023-12-25"));
        assertEquals(Instant.parse("2023-12-25T00:00:00Z"), ISO8601Util.parseInstant("20231225"));
        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T10:30:45Z"));
        assertEquals(expected, ISO8601Util.parseInstant("20231225T103045Z"));
        assertEquals(expected, ISO8601Util.parseInstant("20231225T10:30:45Z"));
        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T103045Z"));
    }

    @Test
    public void parse_acceptsOptionalSeconds() {
        final Instant expected = Instant.parse("2023-12-25T10:30:00Z");

        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T10:30Z"));
        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T1030Z"));
        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T10:30"));
        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T1030"));
    }

    @Test
    public void parse_preservesOneThroughNineFractionDigits() {
        final String digits = "123456789";
        int expectedNanos = 0;
        int placeValue = 100_000_000;

        for (int digitCount = 1; digitCount <= digits.length(); digitCount++) {
            expectedNanos += (digits.charAt(digitCount - 1) - '0') * placeValue;
            placeValue /= 10;
            assertEquals(expectedNanos, ISO8601Util.parseInstant("2023-12-25T10:30:45." + digits.substring(0, digitCount) + "Z").getNano());
        }

        final DateTimeParseException tooPrecise = assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:45.1234567890Z"));
        assertEquals(29, tooPrecise.getErrorIndex());
    }

    @Test
    public void parse_appliesNumericOffsets() {
        final Instant expected = Instant.parse("2023-12-25T05:00:45Z");

        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T10:30:45+05:30"));
        assertEquals(expected, ISO8601Util.parseInstant("2023-12-25T10:30:45+0530"));
        assertEquals(Instant.parse("2023-12-25T15:30:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45-05:00"));
        assertEquals(ISO8601Util.parseInstant("2023-12-25T10:30:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45+00:00"));
        assertEquals(ISO8601Util.parseInstant("2023-12-25T10:30:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45-0000"));
    }

    @Test
    public void parse_enforcesOffsetBoundary() {
        assertEquals(Instant.parse("2023-12-24T16:30:00Z"), ISO8601Util.parseInstant("2023-12-25T10:30:00+18:00"));
        assertEquals(Instant.parse("2023-12-26T04:30:00Z"), ISO8601Util.parseInstant("2023-12-25T10:30:00-18:00"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:00+18:01"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:00+19:00"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:00+05:60"));
    }

    @Test
    public void parse_defaultZoneAppliesOnlyWhenOffsetIsAbsent() {
        final ZoneId kolkata = ZoneId.of("Asia/Kolkata");

        assertEquals(Instant.parse("2023-12-25T05:00:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45", kolkata));
        assertEquals(Instant.parse("2023-12-25T10:30:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45Z", kolkata));
        assertEquals(Instant.parse("2023-12-25T08:30:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45+02:00", kolkata));
    }

    @Test
    public void parse_defaultZoneSupplierIsLazyAndMustReturnAZone() {
        assertEquals(Instant.parse("2023-12-25T10:30:45Z"), ISO8601Util.parseInstantWithDefaultZone("2023-12-25T10:30:45Z", () -> {
            throw new AssertionError("An explicit offset must not consult the fallback zone");
        }));
        assertThrows(NullPointerException.class, () -> ISO8601Util.parseInstantWithDefaultZone("2023-12-25T10:30:45", () -> null));
    }

    @Test
    public void parse_rejectsDstGapsAndOverlaps() {
        final ZoneId losAngeles = ZoneId.of("America/Los_Angeles");

        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2024-03-10T02:30", losAngeles));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2024-11-03T01:30", losAngeles));
        assertEquals(Instant.parse("2024-11-03T08:30:00Z"), ISO8601Util.parseInstant("2024-11-03T01:30-07:00", losAngeles));
        assertEquals(Instant.parse("2024-11-03T09:30:00Z"), ISO8601Util.parseInstant("2024-11-03T01:30-08:00", losAngeles));
    }

    @Test
    public void parse_rejectsMalformedFieldsAndSeparators() {
        final String[] invalid = { "", "not-a-date", "0000-01-01", "2023-13-01", "2023-02-29", "2024-02-30", "2023-12-25T24:00Z", "2023-12-25T10:60Z",
                "2023-12-25T10:30:60Z", "2023-12-25T10:3045Z", "2023-12-25T1030:45Z", "2023-12-25T10:30:Z", "2023-12-25T10:30.5Z", "2023-12-25T10:30:45.Z",
                "2023-1225", "202312-25", "\u0662\u0660\u0662\u0663-12-25T10:30:00Z" };

        for (final String text : invalid) {
            assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant(text), text);
        }

        assertEquals(Instant.parse("2024-02-29T12:00:00Z"), ISO8601Util.parseInstant("2024-02-29T12:00:00Z"));
    }

    @Test
    public void completeParse_rejectsTrailingCharactersAndDateOnlyOffsets() {
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:45Zjunk"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:45 trailing"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25Z"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25+01:00"));
    }

    @Test
    public void parse_rejectsNullWithConsistentArgumentExceptions() {
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.parseInstant(null));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.parseInstant("2023-12-25", (ZoneId) null));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.parseInstant("2023-12-25", (ParsePosition) null));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.parseInstant((String) null, new ParsePosition(0)));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.parseInstantWithDefaultZone(null, () -> ZoneOffset.UTC));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.parseInstantWithDefaultZone("2023-12-25", null));
    }

    @Test
    public void prefixParse_acceptsZonedAndZoneLessPrefixes() {
        assertPrefix("2023-12-25 trailing", "2023-12-25", Instant.parse("2023-12-25T00:00:00Z"));
        assertPrefix("2023-12-25T10:30 trailing", "2023-12-25T10:30", Instant.parse("2023-12-25T10:30:00Z"));
        assertPrefix("2023-12-25T10:30:45 trailing", "2023-12-25T10:30:45", Instant.parse("2023-12-25T10:30:45Z"));
        assertPrefix("20231225T103045.123456789 trailing", "20231225T103045.123456789", Instant.parse("2023-12-25T10:30:45.123456789Z"));
        assertPrefix("2023-12-25T10:30:45Z trailing", "2023-12-25T10:30:45Z", Instant.parse("2023-12-25T10:30:45Z"));
        assertPrefix("2023-12-25T10:30:45+05:30 trailing", "2023-12-25T10:30:45+05:30", Instant.parse("2023-12-25T05:00:45Z"));
    }

    @Test
    public void prefixParse_honorsNonZeroStart() {
        final ParsePosition position = new ParsePosition(2);

        assertEquals(Instant.parse("2023-12-25T10:30:45Z"), ISO8601Util.parseInstant("xx2023-12-25T10:30:45Z tail", position));
        assertEquals(22, position.getIndex());
        assertEquals(-1, position.getErrorIndex());
    }

    @Test
    public void prefixParse_returnsNullAndSetsActualErrorIndex() {
        final ParsePosition invalidDate = new ParsePosition(2);
        assertNull(ISO8601Util.parseInstant("xx2023-02-30", invalidDate));
        assertEquals(2, invalidDate.getIndex());
        assertEquals(10, invalidDate.getErrorIndex());

        final ParsePosition invalidTime = new ParsePosition(2);
        assertNull(ISO8601Util.parseInstant("xx2023-12-25T25:00Z", invalidTime));
        assertEquals(2, invalidTime.getIndex());
        assertEquals(13, invalidTime.getErrorIndex());
    }

    @Test
    public void prefixParse_resetsStaleErrorIndexOnSuccess() {
        final ParsePosition position = new ParsePosition(0);
        position.setErrorIndex(7);

        assertNotNull(ISO8601Util.parseInstant("2023-12-25", position));
        assertEquals(10, position.getIndex());
        assertEquals(-1, position.getErrorIndex());
    }

    @Test
    public void prefixParse_rejectsInvalidInitialPosition() {
        assertThrows(IndexOutOfBoundsException.class, () -> ISO8601Util.parseInstant("2023-12-25", new ParsePosition(-1)));
        assertThrows(IndexOutOfBoundsException.class, () -> ISO8601Util.parseInstant("2023-12-25", new ParsePosition(11)));
    }

    @Test
    public void boundaryOffsetValues_roundTripInTheirOwnOffset() {
        final Instant lower = ISO8601Util.parseInstant("0001-01-01T00:00:00+18:00");
        final Instant upper = ISO8601Util.parseInstant("9999-12-31T23:59:59-18:00");

        assertEquals("0001-01-01T00:00:00+18:00", ISO8601Util.format(lower, ZoneOffset.ofHours(18)));
        assertEquals("9999-12-31T23:59:59-18:00", ISO8601Util.format(upper, ZoneOffset.ofHours(-18)));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(lower));
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(upper));
    }

    @Test
    public void format_usesTheEffectiveOffsetOfARegionZone() {
        final ZoneId losAngeles = ZoneId.of("America/Los_Angeles");

        assertEquals("2023-07-01T05:00:00-07:00", ISO8601Util.format(Instant.parse("2023-07-01T12:00:00Z"), losAngeles));
        assertEquals("2023-01-01T04:00:00-08:00", ISO8601Util.format(Instant.parse("2023-01-01T12:00:00Z"), losAngeles));

        // A historical sub-minute offset cannot be written in the ISO offset field.
        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.format(Instant.parse("1880-01-01T12:00:00Z"), ZoneId.of("Europe/Amsterdam")));
    }

    @Test
    public void parse_requiresUppercaseDesignators() {
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25t10:30:45Z"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25T10:30:45z"));
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2023-12-25 10:30:45Z"));
    }

    @Test
    public void parse_rejectsLeapSeconds() {
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2016-12-31T23:59:60Z"));
    }

    @Test
    public void parse_dateOnlyResolvesStartOfDayAndRejectsAMidnightGap() {
        final ZoneId kolkata = ZoneId.of("Asia/Kolkata");
        assertEquals(Instant.parse("2023-12-24T18:30:00Z"), ISO8601Util.parseInstant("2023-12-25", kolkata));

        // Cuba moved the clock forward at midnight, so 2018-03-11T00:00 never existed there.
        assertThrows(DateTimeParseException.class, () -> ISO8601Util.parseInstant("2018-03-11", ZoneId.of("America/Havana")));
    }

    @Test
    public void parse_supplierFailurePropagatesUnchanged() {
        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> ISO8601Util.parseInstantWithDefaultZone("2023-12-25T10:30:45", () -> {
                    throw new IllegalStateException("zone unavailable");
                }));
        assertEquals("zone unavailable", thrown.getMessage());
    }

    @Test
    public void prefixParse_honorsAnExplicitFallbackZone() {
        final ParsePosition position = new ParsePosition(0);

        assertEquals(Instant.parse("2023-12-25T05:00:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45 trailing", position, ZoneId.of("Asia/Kolkata")));
        assertEquals(19, position.getIndex());
        assertEquals(-1, position.getErrorIndex());

        // An offset in the text still wins over the supplied fallback zone.
        final ParsePosition offsetPosition = new ParsePosition(0);
        assertEquals(Instant.parse("2023-12-25T08:30:45Z"), ISO8601Util.parseInstant("2023-12-25T10:30:45+02:00", offsetPosition, ZoneId.of("Asia/Kolkata")));
        assertEquals(25, offsetPosition.getIndex());

        assertThrows(IllegalArgumentException.class, () -> ISO8601Util.parseInstant("2023-12-25", new ParsePosition(0), (ZoneId) null));
    }

    @Test
    public void prefixParse_enforcesTheOffsetAndFractionLimits() {
        final ParsePosition tooManyDigits = new ParsePosition(0);
        assertNull(ISO8601Util.parseInstant("2023-12-25T10:30:45.1234567890Z", tooManyDigits));
        assertEquals(0, tooManyDigits.getIndex());
        assertEquals(29, tooManyDigits.getErrorIndex());

        final ParsePosition badOffset = new ParsePosition(0);
        assertNull(ISO8601Util.parseInstant("2023-12-25T10:30:45+18:01", badOffset));
        assertEquals(0, badOffset.getIndex());
        assertEquals(19, badOffset.getErrorIndex());
    }

    private static void assertPrefix(final String input, final String prefix, final Instant expected) {
        final ParsePosition position = new ParsePosition(0);

        assertEquals(expected, ISO8601Util.parseInstant(input, position));
        assertEquals(prefix.length(), position.getIndex());
        assertEquals(-1, position.getErrorIndex());
        assertTrue(position.getIndex() <= input.length());
    }
}
