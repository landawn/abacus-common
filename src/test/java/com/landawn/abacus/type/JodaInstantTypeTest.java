package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.chrono.BuddhistChronology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

public class JodaInstantTypeTest extends TestBase {

    private JodaInstantType instantType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        instantType = (JodaInstantType) createType("JodaInstant");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testStringOf_Null() {
        assertNull(instantType.stringOf(null));
    }

    @Test
    public void testStringOf_ValidInstant() {
        Instant instant = new Instant(1703502645123L);
        String result = instantType.stringOf(instant);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_String_Empty() {
        assertNull(instantType.valueOf(""));
    }

    @Test
    public void testValueOf_String_SysTime() {
        long before = System.currentTimeMillis();
        Instant result = instantType.valueOf("sysTime");
        long after = System.currentTimeMillis();

        assertNotNull(result);
        assertTrue(result.getMillis() >= before);
        assertTrue(result.getMillis() <= after);
    }

    @Test
    public void testValueOf_String_NumericString() {
        long millis = 1703502645123L;
        Instant result = instantType.valueOf(String.valueOf(millis));
        assertNotNull(result);
        assertEquals(millis, result.getMillis());
    }

    @Test
    public void testValueOf_String_ISO8601DateTime() {
        String str = "2023-12-25T10:30:45";
        Instant result = instantType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(instantType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_NumericString() {
        long millis = 1703502645123L;
        char[] cbuf = String.valueOf(millis).toCharArray();
        Instant result = instantType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
        assertEquals(millis, result.getMillis());
    }

    @Test
    public void testGet_ResultSet_ByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        Instant result = instantType.get(rs, 1);
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getMillis());
    }

    @Test
    public void testGet_ResultSet_ByName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(1703502645123L);
        when(rs.getTimestamp("instant_column")).thenReturn(timestamp);

        Instant result = instantType.get(rs, "instant_column");
        assertNotNull(result);
        assertEquals(timestamp.getTime(), result.getMillis());
    }

    @Test
    public void testSet_PreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        Instant instant = new Instant(1703502645123L);

        instantType.set(stmt, 1, instant);
        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSet_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        Instant instant = new Instant(1703502645123L);

        instantType.set(stmt, "param_name", instant);
        verify(stmt).setTimestamp(eq("param_name"), any(Timestamp.class));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();
        Instant instant = new Instant(1703502645123L);

        instantType.appendTo(sb, instant);
        assertNotNull(sb.toString());
        assertFalse(sb.toString().isEmpty());
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        instantType.serializeTo(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testSerializeTo_NoConfig() throws IOException {
        Instant instant = new Instant(1703502645123L);

        instantType.serializeTo(characterWriter, instant, null);
        verify(characterWriter).write(anyString());
    }

    @Test
    public void testSerializeTo_LongFormat() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter).write(1703502645123L);
    }

    @Test
    public void testSerializeTo_ISO8601DateTime() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter).write(anyString());
    }

    @Test
    public void testSerializeTo_ISO8601Timestamp() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter).write(anyString());
    }

    @Test
    public void testSerializeTo_WithQuotes() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn('"');

        instantType.serializeTo(characterWriter, instant, config);
        verify(characterWriter, times(2)).write('"');
        verify(characterWriter).write(anyString());
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testStringOfValueOfRoundTripIsUtc() {
        // regression: the shared Joda formatters lacked withZoneUTC(), so printing used the
        // value's chronology zone while parsing used the JVM default zone - round trips drifted
        // by the JVM's UTC offset on any non-UTC machine, and the emitted wall time contradicted
        // the literal 'Z' (UTC) suffix
        final Instant epoch = new Instant(0L);

        assertEquals("1970-01-01T00:00:00.000Z", instantType.stringOf(epoch));
        assertEquals(0L, instantType.valueOf("1970-01-01T00:00:00.000Z").getMillis());
        assertEquals(0L, instantType.valueOf("1970-01-01T00:00:00Z").getMillis());

        final Instant now = new Instant(1703502645123L);
        assertEquals(now.getMillis(), instantType.valueOf(instantType.stringOf(now)).getMillis());
    }

    // --- review fixes 2026-09-06 (T9-01..T9-05) ---

    @Test
    public void reviewFixes20260906_T901_compactOffsetAndFractionShapesParseLikeTimestamp() {
        final TimestampType tsType = new TimestampType();

        for (final String s : new String[] { "2024-01-01T00:00:00+0000", "2024-01-01T00:00:00-0800", "2024-01-01 00:00:00.1234", "2024-01-01T00:00:00.1234" }) {
            assertEquals(tsType.valueOf(s).getTime(), instantType.valueOf(s).getMillis(), s);
            assertEquals(tsType.valueOf(s).getTime(), instantType.valueOf(s.toCharArray(), 0, s.length()).getMillis(), s);
        }

        assertEquals(1704067200000L, instantType.valueOf("2024-01-01T00:00:00+0000").getMillis());
        assertEquals(1704096000000L, instantType.valueOf("2024-01-01T00:00:00-0800").getMillis());
        assertEquals(1704096000123L, instantType.valueOf("2024-01-01 00:00:00.1234").getMillis());
        assertEquals(1704096000123L, instantType.valueOf("2024-01-01T00:00:00.1234").getMillis());

        // keep-alive: 'Z' shapes and lower-case leniency
        assertEquals(1704067200000L, instantType.valueOf("2024-01-01T00:00:00Z").getMillis());
        assertEquals(1704067200123L, instantType.valueOf("2024-01-01T00:00:00.123Z").getMillis());
        assertEquals(1704067200000L, instantType.valueOf("2024-01-01t00:00:00z").getMillis());
        assertEquals(1704067200123L, instantType.valueOf("2024-01-01T00:00:00.123z").getMillis());

        assertThrows(IllegalArgumentException.class, () -> instantType.valueOf("2024-13-01T00:00:00Z"));
        assertThrows(IllegalArgumentException.class, () -> instantType.valueOf("2024-01-01 00:00:00Z"));
        assertNull(instantType.valueOf(""));
        assertNull(instantType.valueOf("null"));
    }

    @Test
    public void reviewFixes20260906_T902_T903_numericGrammarAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> instantType.valueOf("0x1F4A0"));
        assertThrows(IllegalArgumentException.class, () -> instantType.valueOf("0x1F4A0".toCharArray(), 0, 7));

        for (final String s : new String[] { "1700000000000L", "1700000000000d", "12345L", "99999999999999999999", "9223372036854775808",
                "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> instantType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> instantType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807" }) {
            final long expected = Long.parseLong(s);
            assertEquals(expected, instantType.valueOf(s).getMillis(), s);
            assertEquals(expected, instantType.valueOf(s.toCharArray(), 0, s.length()).getMillis(), s);
        }
    }

    @Test
    public void reviewFixes20260906_T904_valueOfObjectFromJodaValues() {
        final Instant instant = new Instant(1700000000123L);
        assertEquals(instant, instantType.valueOf((Object) instant));

        // a Buddhist-chronology DateTime used to be stringified with its Buddhist year and re-read 543 years off
        final DateTime buddhist = new DateTime(1700000000123L, BuddhistChronology.getInstance(DateTimeZone.forID("Asia/Bangkok")));
        assertEquals(1700000000123L, instantType.valueOf((Object) buddhist).getMillis());

        final DateTime kolkata = new DateTime(1700000000123L, DateTimeZone.forID("Asia/Kolkata"));
        assertEquals(1700000000123L, instantType.valueOf((Object) kolkata).getMillis());

        assertNull(instantType.valueOf((Object) null));
    }

    @Test
    public void reviewFixes20260906_T905_outOfRangeYearIsPrintedButNotReadBack() {
        // documented: no year guard on stringOf; a year of more than four digits, or a negative year, is rejected
        // by the inverse parser
        final Instant year10000 = new Instant(253402300800000L);
        assertEquals("10000-01-01T00:00:00.000Z", instantType.stringOf(year10000));
        assertThrows(IllegalArgumentException.class, () -> instantType.valueOf("10000-01-01T00:00:00.000Z"));

        assertEquals("-0001-01-01T00:00:00.000Z", instantType.stringOf(new Instant(-62198755200000L)));
        assertThrows(IllegalArgumentException.class, () -> instantType.valueOf("-0001-01-01T00:00:00.000Z"));

        // year 0000 is the documented exception: it prints AND reads back at the same instant
        assertEquals("0000-12-31T23:59:59.999Z", instantType.stringOf(new Instant(-62135596800001L)));
        assertEquals(-62135596800001L, instantType.valueOf("0000-12-31T23:59:59.999Z").getMillis());

        assertEquals("9999-12-31T23:59:59.999Z", instantType.stringOf(new Instant(253402300799999L)));
        assertEquals(253402300799999L, instantType.valueOf("9999-12-31T23:59:59.999Z").getMillis());
    }
}
