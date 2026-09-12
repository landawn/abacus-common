package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.BuddhistChronology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class AbstractJodaDateTimeTypeTest extends TestBase {

    private JodaDateTimeType jodaDateTimeType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        jodaDateTimeType = createType("JodaDateTime");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testIsJodaDateTime() {
        assertTrue(jodaDateTimeType.isJodaDateTime());
    }

    @Test
    public void testIsComparable() {
        assertTrue(jodaDateTimeType.isComparable());
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(jodaDateTimeType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOf() {
        // constructed in UTC: the 'Z'-suffixed output is UTC wall time (it previously leaked the
        // JVM default zone's wall time under the Z suffix)
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123, org.joda.time.DateTimeZone.UTC);
        String result = jodaDateTimeType.stringOf(dateTime);
        assertNotNull(result);
        assertTrue(result.contains("2023-06-15"));
        assertTrue(result.contains("10:30:45"));

        assertNull(jodaDateTimeType.stringOf(null));
    }

    @Test
    public void testAppendTo() throws IOException {
        // constructed in UTC: the 'Z'-suffixed output is UTC wall time
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123, org.joda.time.DateTimeZone.UTC);
        StringWriter sw = new StringWriter();

        jodaDateTimeType.appendTo(sw, dateTime);
        String result = sw.toString();
        assertNotNull(result);
        assertTrue(result.contains("2023-06-15"));
        assertTrue(result.contains("10:30:45"));

        sw = new StringWriter();
        jodaDateTimeType.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void testSerializeToWithNullValue() throws IOException {
        jodaDateTimeType.serializeTo(writer, null, null);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithLongFormat() throws IOException {
        DateTime dateTime = new DateTime(1234567890123L);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer).write(1234567890123L);
    }

    @Test
    public void testSerializeToWithISO8601DateTime() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testSerializeToWithISO8601Timestamp() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testSerializeToWithQuotation() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn('"');

        jodaDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer, times(2)).write('"');
    }

    @Test
    public void testSerializeToWithNullConfig() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);

        jodaDateTimeType.serializeTo(writer, dateTime, null);
        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testSerializeToWithUnsupportedFormat() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        DateTimeFormat unsupportedFormat = mock(DateTimeFormat.class);
        when(config.getDateTimeFormat()).thenReturn(unsupportedFormat);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.serializeTo(writer, dateTime, config);
        assertNotNull(unsupportedFormat);
    }

    // --- review fixes 2026-09-06 (T9-04 root cause, T9-05) ---

    @Test
    public void reviewFixes20260906_T904_formattersPrintIsoUtcForAnyChronology() throws IOException {
        // the shared formatters pinned only the zone (withZoneUTC), so a BuddhistChronology value printed its
        // Buddhist year ("2566-..Z"), which every parser reads as ISO year 2566 - 543 years off the real instant
        final DateTime buddhist = new DateTime(1700000000123L, BuddhistChronology.getInstance(DateTimeZone.forID("Asia/Bangkok")));
        final DateTime isoUtc = new DateTime(1700000000123L, DateTimeZone.UTC);

        assertEquals("2023-11-14T22:13:20.123Z", jodaDateTimeType.stringOf(isoUtc));
        assertEquals(jodaDateTimeType.stringOf(isoUtc), jodaDateTimeType.stringOf(buddhist));

        final StringBuilder sb = new StringBuilder();
        jodaDateTimeType.appendTo(sb, buddhist);
        assertEquals("2023-11-14T22:13:20.123Z", sb.toString());

        final JsonXmlSerConfig<?> cfg = mock(JsonXmlSerConfig.class);
        when(cfg.getStringQuotation()).thenReturn((char) 0);

        when(cfg.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        BufferedJsonWriter real = Objectory.createBufferedJsonWriter();
        jodaDateTimeType.serializeTo(real, buddhist, cfg);
        assertEquals("2023-11-14T22:13:20Z", real.toString());

        when(cfg.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        real = Objectory.createBufferedJsonWriter();
        jodaDateTimeType.serializeTo(real, buddhist, cfg);
        assertEquals("2023-11-14T22:13:20.123Z", real.toString());

        // the round trip now lands on the same instant (zone/chronology are re-defaulted by valueOf, as documented)
        assertTrue(jodaDateTimeType.valueOf(jodaDateTimeType.stringOf(buddhist)).isEqual(buddhist));
        assertEquals(1700000000123L, jodaDateTimeType.valueOf(jodaDateTimeType.stringOf(buddhist)).getMillis());

        // a Kolkata (ISO) value: unchanged output, UTC wall time under the 'Z'
        assertEquals("2023-11-14T22:13:20.123Z", jodaDateTimeType.stringOf(new DateTime(1700000000123L, DateTimeZone.forID("Asia/Kolkata"))));
    }

    @Test
    public void reviewFixes20260906_T905_outOfRangeYearIsPrintedButNotReadBack() {
        // documented: no year guard on the Joda side; a year of more than four digits, or a negative year, is
        // rejected by the inverse parser
        assertEquals("10000-01-01T00:00:00.000Z", jodaDateTimeType.stringOf(new DateTime(253402300800000L, DateTimeZone.UTC)));
        assertThrows(IllegalArgumentException.class, () -> jodaDateTimeType.valueOf("10000-01-01T00:00:00.000Z"));

        assertEquals("-0001-01-01T00:00:00.000Z", jodaDateTimeType.stringOf(new DateTime(-62198755200000L, DateTimeZone.UTC)));
        assertThrows(IllegalArgumentException.class, () -> jodaDateTimeType.valueOf("-0001-01-01T00:00:00.000Z"));

        // year 0000 is the documented exception: it prints AND reads back at the same instant (Joda accepts year
        // zero, while the Date/Calendar handlers reject it)
        assertEquals("0000-12-31T23:59:59.999Z", jodaDateTimeType.stringOf(new DateTime(-62135596800001L, DateTimeZone.UTC)));
        assertEquals(-62135596800001L, jodaDateTimeType.valueOf("0000-12-31T23:59:59.999Z").getMillis());
        assertThrows(IllegalArgumentException.class, () -> createType(java.sql.Timestamp.class).valueOf("0000-12-31T23:59:59.999Z"));

        // the last in-range instant round-trips
        assertEquals("9999-12-31T23:59:59.999Z", jodaDateTimeType.stringOf(new DateTime(253402300799999L, DateTimeZone.UTC)));
        assertEquals(253402300799999L, jodaDateTimeType.valueOf("9999-12-31T23:59:59.999Z").getMillis());
    }
}
