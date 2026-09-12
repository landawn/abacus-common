package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;

public class OffsetDateTimeTypeTest extends TestBase {

    private OffsetDateTimeType offsetDateTimeType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        offsetDateTimeType = (OffsetDateTimeType) createType("OffsetDateTime");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OffsetDateTime.class, offsetDateTimeType.javaType());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(offsetDateTimeType.stringOf(null));
    }

    @Test
    public void testStringOfWithValue() {
        OffsetDateTime dateTime = OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 123456789, ZoneOffset.ofHoursMinutes(5, 30));
        String result = offsetDateTimeType.stringOf(dateTime);
        assertEquals("2023-05-15T10:30:45.123456789+05:30", result);
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(offsetDateTimeType.valueOf((String) null));
        assertNull(offsetDateTimeType.valueOf((Object) null));
    }

    @Test
    public void testValueOf_Object_OffsetDateTime_identity() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 123456789, ZoneOffset.UTC);
        assertEquals(odt, offsetDateTimeType.valueOf(odt));
        assertEquals(123456789, offsetDateTimeType.valueOf(odt).getNano());
    }

    @Test
    public void testValueOf_Object_Date_and_Calendar() {
        long millis = 1703502645123L;
        OffsetDateTime fromDate = offsetDateTimeType.valueOf(new Date(millis));
        assertNotNull(fromDate);
        assertEquals(millis, fromDate.toInstant().toEpochMilli());

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        OffsetDateTime fromCal = offsetDateTimeType.valueOf(cal);
        assertNotNull(fromCal);
        assertEquals(millis, fromCal.toInstant().toEpochMilli());
    }

    @Test
    public void testValueOfWithEmptyString() {
        assertNull(offsetDateTimeType.valueOf(""));
    }

    @Test
    public void testValueOfWithNullString() {
        assertNull(offsetDateTimeType.valueOf("null"));
        assertNull(offsetDateTimeType.valueOf("NULL"));
    }

    @Test
    public void testValueOfWithSysTime() {
        OffsetDateTime result = offsetDateTimeType.valueOf("sysTime");
        assertNotNull(result);
        assertTrue(Math.abs(result.toEpochSecond() - OffsetDateTime.now().toEpochSecond()) < 5);
    }

    @Test
    public void testValueOfWithEpochMillis() {
        long epochMillis = 1684150000000L;
        OffsetDateTime result = offsetDateTimeType.valueOf(String.valueOf(epochMillis));
        assertNotNull(result);
        assertEquals(epochMillis, result.toInstant().toEpochMilli());
    }

    @Test
    public void testValueOfWithNumber() {
        Long epochMillis = 1684150000000L;
        OffsetDateTime result = offsetDateTimeType.valueOf(epochMillis);
        assertNotNull(result);
        assertEquals(epochMillis.longValue(), result.toInstant().toEpochMilli());
    }

    @Test
    public void testValueOfWithISO8601DateTime() {
        String isoDateTime = "2023-05-15T10:30:45Z";
        OffsetDateTime result = offsetDateTimeType.valueOf(isoDateTime);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(5, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        N.println(result.toString());
    }

    @Test
    public void testValueOfWithISO8601Timestamp() {
        String isoTimestamp = "2023-05-15T10:30:45.323Z";
        OffsetDateTime result = offsetDateTimeType.valueOf(isoTimestamp);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(5, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        N.println(result.toString());
    }

    @Test
    public void testValueOfParsesOffsetDateTimeToString() {
        // Every form produced by OffsetDateTime.toString() must round-trip through valueOf.
        OffsetDateTime[] values = { //
                OffsetDateTime.of(2023, 10, 15, 10, 30, 0, 0, ZoneOffset.UTC), // seconds omitted, "...:30Z"
                OffsetDateTime.of(2023, 10, 15, 10, 30, 45, 0, ZoneOffset.UTC), // seconds, "...:45Z"
                OffsetDateTime.of(2023, 10, 15, 10, 30, 45, 123000000, ZoneOffset.UTC), // millis, "...45.123Z"
                OffsetDateTime.of(2023, 10, 15, 10, 30, 45, 123456789, ZoneOffset.UTC), // nanos
                OffsetDateTime.of(2023, 10, 15, 10, 30, 45, 0, ZoneOffset.ofHours(1)), // numeric offset
                OffsetDateTime.of(2023, 10, 15, 10, 30, 0, 0, ZoneOffset.ofHours(-8)), // offset, seconds omitted
                OffsetDateTime.of(2023, 10, 15, 10, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)), // +05:30
                OffsetDateTime.of(2023, 10, 15, 10, 30, 45, 123456789, ZoneOffset.ofHoursMinutesSeconds(5, 30, 15)) // +05:30:15
        };

        for (OffsetDateTime value : values) {
            String text = value.toString();
            OffsetDateTime result = offsetDateTimeType.valueOf(text);
            assertNotNull(result, () -> "Failed to parse: " + text);
            assertEquals(value.toInstant(), result.toInstant(), () -> "Instant mismatch for: " + text);
            assertEquals(value, result, () -> "Value mismatch for: " + text);
        }
    }

    @Test
    public void testValueOfParsesOffsetDateTimeNowToString() {
        OffsetDateTime value = OffsetDateTime.now();
        OffsetDateTime result = offsetDateTimeType.valueOf(value.toString());
        assertEquals(value, result);
    }

    @Test
    public void testValueOfWithCharArray() {
        String dateStr = "2023-05-15T03:30:45.000Z";
        char[] chars = dateStr.toCharArray();
        OffsetDateTime result = offsetDateTimeType.valueOf(chars, 0, chars.length);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
    }

    @Test
    public void testValueOfWithNullCharArray() {
        assertNull(offsetDateTimeType.valueOf(null, 0, 0));
        assertNull(offsetDateTimeType.valueOf(new char[0], 0, 0));
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        OffsetDateTime result = offsetDateTimeType.get(rs, 1);
        assertNotNull(result);
        assertEquals(timestamp.toInstant().toEpochMilli(), result.toInstant().toEpochMilli());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        assertNull(offsetDateTimeType.get(rs, 1));
    }

    @Test
    public void testGetFromResultSetByName() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp("date_column")).thenReturn(timestamp);

        OffsetDateTime result = offsetDateTimeType.get(rs, "date_column");
        assertNotNull(result);
        assertEquals(timestamp.toInstant().toEpochMilli(), result.toInstant().toEpochMilli());
    }

    @Test
    public void testGetFromResultSetByNameWithNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("date_column")).thenReturn(null);

        assertNull(offsetDateTimeType.get(rs, "date_column"));
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        offsetDateTimeType.set(stmt, 1, null);
        verify(stmt).setTimestamp(1, null);
    }

    @Test
    public void testSetPreparedStatementWithValue() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        OffsetDateTime dateTime = OffsetDateTime.now();
        offsetDateTimeType.set(stmt, 1, dateTime);
        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        offsetDateTimeType.set(stmt, "param", null);
        verify(stmt).setTimestamp("param", null);
    }

    @Test
    public void testSetCallableStatementWithValue() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        OffsetDateTime dateTime = OffsetDateTime.now();
        offsetDateTimeType.set(stmt, "param", dateTime);
        verify(stmt).setTimestamp(eq("param"), any(Timestamp.class));
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        offsetDateTimeType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValue() throws IOException {
        StringBuilder sb = new StringBuilder();
        OffsetDateTime dateTime = OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        offsetDateTimeType.appendTo(sb, dateTime);
        String result = sb.toString();
        assertNotNull(result);
        assertTrue(result.contains("2023"));
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        offsetDateTimeType.serializeTo(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithValueNoConfig() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        offsetDateTimeType.serializeTo(writer, dateTime, null);
        verify(writer).write(anyString());
    }

    @Test
    public void testSerializeToWithLongFormat() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        offsetDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer).write(anyLong());
    }

    @Test
    public void testSerializeToWithISO8601DateTimeFormat() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.of(2023, 12, 25, 10, 30, 45, 123456789, ZoneOffset.ofHoursMinutes(5, 30));
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        offsetDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer).write("2023-12-25T10:30:45+05:30");
    }

    @Test
    public void testSerializeToWithISO8601TimestampFormat() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30));
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        offsetDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer).write("2023-12-25T10:30:45.000+05:30");
    }

    @Test
    public void testSerializeToWithQuotation() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn('"');

        offsetDateTimeType.serializeTo(writer, dateTime, config);
        verify(writer, times(2)).write('"'); // Opening and closing quotes
        verify(writer).write(anyString());
    }

    // --- review fixes 2026-09-06 (T10-01, T10-02, T10-03) ---

    @Test
    public void reviewFixes20260906_T1001_fastPathRejectsImpossibleCalendarValues() {
        // the 20/24-char 'Z' fast path resolved with SMART and silently moved Feb 30 -> Feb 28, 24:00 -> next day
        for (final String s : new String[] { "2023-02-30T10:30:45Z", "2023-02-30T10:30:45.123Z", "2023-04-31T10:30:45Z", "2023-02-29T00:00:00Z",
                "2023-02-29T00:00:00.000Z", "2023-10-15T24:00:00Z", "2023-10-15T24:00:00.000Z" }) {
            assertThrows(DateTimeParseException.class, () -> offsetDateTimeType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> offsetDateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        assertThrows(DateTimeParseException.class, () -> offsetDateTimeType.valueOf("2023-02-30T10:30:45+00:00"));

        for (final String s : new String[] { "2024-02-29T00:00:00Z", "2024-02-29T00:00:00.000Z", "0000-02-29T00:00:00Z", "0001-01-01T00:00:00.000Z",
                "9999-12-31T23:59:59.999Z", "2023-10-15T10:30:45.123456789Z", "2023-10-15T10:30:45+05:30:15" }) {
            assertEquals(OffsetDateTime.parse(s), offsetDateTimeType.valueOf(s), s);
            assertEquals(OffsetDateTime.parse(s), offsetDateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        final OffsetDateTime x = OffsetDateTime.parse("2024-02-29T12:34:56.789+05:30");
        assertEquals(x, offsetDateTimeType.valueOf(offsetDateTimeType.stringOf(x)));
    }

    @Test
    public void reviewFixes20260906_T1002_T1003_numericGrammarAndOverflow() {
        for (final String s : new String[] { "170000000000000000000", "9223372036854775808", "-9223372036854775809", "0x1F4A0", "1700000000000L",
                "1700000000000d", "12345L" }) {
            assertThrows(DateTimeParseException.class, () -> offsetDateTimeType.valueOf(s), s);
            assertThrows(DateTimeParseException.class, () -> offsetDateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "9223372036854775807" }) {
            final OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneId.systemDefault());
            assertEquals(expected, offsetDateTimeType.valueOf(s), s);
            assertEquals(expected, offsetDateTimeType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(12345L), ZoneId.systemDefault()), offsetDateTimeType.valueOf("12345"));
        assertThrows(DateTimeParseException.class, () -> offsetDateTimeType.valueOf("1234"));
        assertThrows(DateTimeParseException.class, () -> offsetDateTimeType.valueOf("0"));
    }

    // Finding 125 sibling (2026-09-08): the Calendar branch read only getTimeInMillis() and rebuilt the value in
    // the JVM default zone, discarding the zone the caller had explicitly attached - in a target type whose whole
    // point is to carry an offset, and which serializes that offset through stringOf.
    @Test
    public void reviewFixes20260908_calendarKeepsItsOwnOffset() {
        final long millis = 1703502645123L;

        for (final String zoneName : new String[] { "Asia/Tokyo", "America/Los_Angeles", "UTC", "GMT+05:30" }) {
            final TimeZone tz = TimeZone.getTimeZone(zoneName);
            final GregorianCalendar cal = new GregorianCalendar(tz);
            cal.setTimeInMillis(millis);

            final OffsetDateTime odt = offsetDateTimeType.valueOf(cal);

            assertEquals(millis, odt.toInstant().toEpochMilli(), zoneName);
            assertEquals(tz.toZoneId().getRules().getOffset(Instant.ofEpochMilli(millis)), odt.getOffset(), zoneName);
            // Same result as GregorianCalendar.toZonedDateTime(), which is the JDK's own conversion.
            assertEquals(cal.toZonedDateTime().toOffsetDateTime(), odt, zoneName);
        }

        final GregorianCalendar tokyo = new GregorianCalendar(TimeZone.getTimeZone("Asia/Tokyo"));
        tokyo.setTimeInMillis(millis);
        assertTrue(offsetDateTimeType.stringOf(offsetDateTimeType.valueOf(tokyo)).endsWith("+09:00"));

        // A java.util.Date carries no zone, so it keeps being read in the default zone.
        assertEquals(ZoneId.systemDefault().getRules().getOffset(Instant.ofEpochMilli(millis)), offsetDateTimeType.valueOf(new Date(millis)).getOffset());
    }
}
