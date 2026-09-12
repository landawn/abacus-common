package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.util.N;

/**
 * Regression tests for the ParserUtil date/time read side: civil fields written in the configured
 * zone must be read back anchored in that same zone (the write side renders in the configured zone),
 * and empty or {@code "null"} text must map to a {@code null} property rather than throwing under the
 * strict {@code Dates.parseTo*} contract. The round-trip assertions only discriminate on a machine
 * whose default zone is not UTC.
 */
public class ParserUtilDateRoundTripTest extends TestBase {

    public static class SqlDateTimeBean {
        @JsonXmlField(dateFormat = "yyyy-MM-dd", timeZone = "UTC")
        public java.sql.Date date;

        @JsonXmlField(dateFormat = "HH:mm:ss", timeZone = "UTC")
        public java.sql.Time time;
    }

    @Test
    public void sqlDateAndTime_configuredUtcZone_roundTrip() {
        final SqlDateTimeBean bean = new SqlDateTimeBean();
        bean.date = new java.sql.Date(Instant.parse("2025-01-15T00:00:00Z").toEpochMilli());
        bean.time = new java.sql.Time(Instant.parse("1970-01-01T12:34:56Z").toEpochMilli());

        final String json = N.toJson(bean);
        final SqlDateTimeBean back = N.fromJson(json, SqlDateTimeBean.class);

        assertEquals(bean.date.getTime(), back.date.getTime());
        assertEquals(bean.time.getTime(), back.time.getTime());
    }

    @Test
    public void sqlDateAndTime_emptyOrNullMarkerText_readsNull() {
        final SqlDateTimeBean back = N.fromJson("{\"date\":\"\", \"time\":\"null\"}", SqlDateTimeBean.class);

        assertNull(back.date);
        assertNull(back.time);
    }
}
