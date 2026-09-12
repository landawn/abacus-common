package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dates.DTF;

/**
 * ISO-8601 codec tests for {@link ISO8601Util} and the ISO constants on {@link Dates}.
 * Last-day / same-month / length-of-year coverage lives in {@link DatesCompareTest}.
 */
public class DatesIso8601Test extends TestBase {

    @Test
    public void parseInstant_timezoneLessForm_defaultsToUtc() {
        final java.text.ParsePosition pos = new java.text.ParsePosition(0);
        final Instant instant = ISO8601Util.parseInstant("2026-05-07T10:30:45", pos);
        assertEquals(Instant.parse("2026-05-07T10:30:45Z"), instant);
    }

    @Test
    public void iso8601Util_formatParse_roundTripsInstant() {
        final Date date = new Date();
        final String isoText = ISO8601Util.format(date.toInstant());
        assertEquals(date, Dates.parseToJUDate(isoText));
        assertEquals(date.toInstant(), ISO8601Util.parseInstant(isoText));
    }

    @Test
    public void datesIsoConstants_formatParse_roundTrip() {
        final Date date = Dates.parseToJUDate("2024-07-31T23:42:38Z", Dates.ISO_8601_DATE_TIME_FORMAT);
        assertNotNull(Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT));
        assertNotNull(Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT));
        assertEquals(date.getTime(), Dates.parseToJUDate(Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT), Dates.ISO_8601_DATE_TIME_FORMAT).getTime());

        // Offset form is a different instant from the same wall-clock stamped 'Z'.
        assertEquals(Dates.parseToJUDate("2024-08-01T06:42:38Z", Dates.ISO_8601_DATE_TIME_FORMAT).getTime(),
                Dates.parseToTimestamp("2024-07-31T23:42:38-07:00").getTime());
    }

    @Test
    public void dtfIso_roundTripsOffsetAndZoned() {
        final OffsetDateTime odt = OffsetDateTime.of(2024, 7, 31, 23, 42, 38, 0, ZoneOffset.ofHours(-7));
        assertEquals(odt, DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(DTF.ISO_OFFSET_DATE_TIME.format(odt)));

        final ZonedDateTime zdt = ZonedDateTime.of(2024, 7, 31, 23, 42, 38, 0, java.time.ZoneId.of("America/Los_Angeles"));
        assertEquals(zdt.toInstant(), DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(DTF.ISO_ZONED_DATE_TIME.format(zdt)).toInstant());
    }

    @Test
    public void httpDate_roundTripsThroughDatesFormat() {
        final java.sql.Timestamp ts = Dates.parseToTimestamp("Tue, 02 Jul 2024 06:53:48 GMT");
        assertEquals("Tue, 02 Jul 2024 06:53:48 GMT", Dates.format(ts, Dates.HTTP_DATE_FORMAT));
    }
}
