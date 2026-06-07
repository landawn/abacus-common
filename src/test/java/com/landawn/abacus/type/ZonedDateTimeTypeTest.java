package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

public class ZonedDateTimeTypeTest extends TestBase {

    private ZonedDateTimeType zonedDateTimeType;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        zonedDateTimeType = new ZonedDateTimeType();
    }

    @Test
    public void testClazz() {
        Class<ZonedDateTime> clazz = zonedDateTimeType.javaType();
        assertEquals(ZonedDateTime.class, clazz);
    }

    @Test
    public void testStringOfWithNull() {
        String result = zonedDateTimeType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testStringOfWithValidZonedDateTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));
        String result = zonedDateTimeType.stringOf(zonedDateTime);
        assertNotNull(result);
        assertTrue(result.contains("2023-12-25"));
        assertTrue(result.contains("10:30:45"));
    }

    @Test
    public void testStringOfUsesOffsetFormatForRoundTrip() {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("America/Los_Angeles"));
        String text = zonedDateTimeType.stringOf(zonedDateTime);
        ZonedDateTime result = zonedDateTimeType.valueOf(text);

        // stringOf serializes with the ISO_OFFSET_DATE_TIME representation: offset only, no region-zone suffix.
        assertFalse(text.contains("["));
        assertTrue(text.contains("-08:00"));
        // The round-trip preserves the instant; the region zone collapses to its offset.
        assertEquals(zonedDateTime.toInstant(), result.toInstant());
    }

    @Test
    public void testValueOfObjectWithNull() {
        ZonedDateTime result = zonedDateTimeType.valueOf((Object) null);
        assertNull(result);
    }

    @Test
    public void testValueOfObjectWithNumber() {
        long epochMilli = 1703502645000L;
        ZonedDateTime result = zonedDateTimeType.valueOf(epochMilli);
        assertNotNull(result);
        assertEquals(epochMilli, result.toInstant().toEpochMilli());
    }

    @Test
    public void testValueOfObjectWithString() {
        String dateTimeString = "2023-12-25T10:30:45Z";
        ZonedDateTime result = zonedDateTimeType.valueOf((Object) dateTimeString);
        assertNotNull(result);
    }

    @Test
    public void testValueOfStringWithNull() {
        ZonedDateTime result = zonedDateTimeType.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOfStringWithEmptyString() {
        ZonedDateTime result = zonedDateTimeType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfStringWithSysTime() {
        ZonedDateTime before = ZonedDateTime.now();
        ZonedDateTime result = zonedDateTimeType.valueOf("sysTime");
        ZonedDateTime after = ZonedDateTime.now();

        assertNotNull(result);
        assertTrue(!result.isBefore(before));
        assertTrue(!result.isAfter(after));
    }

    @Test
    public void testValueOfStringWithEpochMillis() {
        String epochMillis = "1703502645000";
        ZonedDateTime result = zonedDateTimeType.valueOf(epochMillis);
        assertNotNull(result);
        assertEquals(1703502645000L, result.toInstant().toEpochMilli());
    }

    @Test
    public void testValueOfStringWithISO8601DateTime() {
        String iso8601DateTime = "2023-12-25T10:30:45Z";
        ZonedDateTime result = zonedDateTimeType.valueOf(iso8601DateTime);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    public void testValueOfStringWithISO8601Timestamp() {
        String iso8601Timestamp = "2023-12-25T10:30:45.123Z";
        ZonedDateTime result = zonedDateTimeType.valueOf(iso8601Timestamp);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    public void testValueOfStringWithOtherFormat() {
        String dateTimeString = "2023-12-25T10:30:45+02:00";
        ZonedDateTime result = zonedDateTimeType.valueOf(dateTimeString);
        assertNotNull(result);
    }

    @Test
    public void testValueOfParsesZonedDateTimeToString() {
        // Every form produced by ZonedDateTime.toString() must round-trip through valueOf.
        ZonedDateTime[] values = { //
                ZonedDateTime.of(2023, 10, 15, 10, 30, 0, 0, ZoneOffset.UTC), // seconds omitted, "...:30Z"
                ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 0, ZoneOffset.UTC), // seconds, "...:45Z"
                ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 123000000, ZoneOffset.UTC), // millis, "...45.123Z"
                ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 123456789, ZoneOffset.UTC), // nanos
                ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 0, ZoneOffset.ofHours(1)), // numeric offset
                ZonedDateTime.of(2023, 10, 15, 10, 30, 0, 0, ZoneOffset.ofHours(1)), // offset, seconds omitted
                ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30)), // +05:30
                ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 0, ZoneOffset.ofHoursMinutesSeconds(5, 30, 15)) // +05:30:15
        };

        for (ZonedDateTime value : values) {
            String text = value.toString();
            ZonedDateTime result = zonedDateTimeType.valueOf(text);
            assertNotNull(result, () -> "Failed to parse: " + text);
            assertEquals(value.toInstant(), result.toInstant(), () -> "Instant mismatch for: " + text);
        }
    }

    @Test
    public void testValueOfParsesZonedDateTimeToStringWithRegionZone() {
        // ZonedDateTime.toString() appends the region zone in brackets; valueOf must preserve it.
        String[] zones = { "America/Los_Angeles", "Europe/Paris", "Asia/Kolkata", "Asia/Kathmandu", "UTC" };

        for (String zoneId : zones) {
            ZonedDateTime value = ZonedDateTime.of(2023, 10, 15, 10, 30, 45, 123000000, ZoneId.of(zoneId));
            String text = value.toString();
            assertTrue(text.contains("[" + zoneId + "]"), () -> "Expected region zone in: " + text);

            ZonedDateTime result = zonedDateTimeType.valueOf(text);
            assertEquals(value, result, () -> "Value mismatch for: " + text);
            assertEquals(value.getZone(), result.getZone(), () -> "Zone mismatch for: " + text);
        }
    }

    @Test
    public void testValueOfParsesZonedDateTimeNowToString() {
        // ZonedDateTime.now() typically carries nanosecond precision in a region zone.
        ZonedDateTime value = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
        ZonedDateTime result = zonedDateTimeType.valueOf(value.toString());
        assertEquals(value, result);
        assertEquals(value.getZone(), result.getZone());
    }

    @Test
    public void testValueOfCharArrayWithNull() {
        ZonedDateTime result = zonedDateTimeType.valueOf(null, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOfCharArrayWithEmptyLength() {
        char[] cbuf = new char[10];
        ZonedDateTime result = zonedDateTimeType.valueOf(cbuf, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOfCharArrayWithEpochMillis() {
        char[] cbuf = "1703502645000".toCharArray();
        ZonedDateTime result = zonedDateTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
        assertEquals(1703502645000L, result.toInstant().toEpochMilli());
    }

    @Test
    public void testValueOfCharArrayWithDateTimeString() {
        char[] cbuf = "2023-12-25T10:30:45Z".toCharArray();
        ZonedDateTime result = zonedDateTimeType.valueOf(cbuf, 0, cbuf.length);
        assertNotNull(result);
    }

    @Test
    public void testValueOfCharArrayWithOffset() {
        char[] cbuf = "xxx2023-12-25T10:30:45Z".toCharArray();
        ZonedDateTime result = zonedDateTimeType.valueOf(cbuf, 3, 20);
        assertNotNull(result);
    }

    @Test
    public void testValueOfStringWithInvalidFormat() {
        assertThrows(DateTimeParseException.class, () -> {
            zonedDateTimeType.valueOf("invalid-date-time");
        });
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        Timestamp timestamp = Timestamp.from(Instant.ofEpochMilli(1703502645000L));
        when(resultSet.getTimestamp(1)).thenReturn(timestamp);

        ZonedDateTime result = zonedDateTimeType.get(resultSet, 1);
        assertNotNull(result);
        assertEquals(1703502645000L, result.toInstant().toEpochMilli());
    }

    @Test
    public void testGetFromResultSetByIndexWithNull() throws SQLException {
        when(resultSet.getTimestamp(1)).thenReturn(null);

        ZonedDateTime result = zonedDateTimeType.get(resultSet, 1);
        assertNull(result);
    }

    @Test
    public void testGetFromResultSetByName() throws SQLException {
        Timestamp timestamp = Timestamp.from(Instant.ofEpochMilli(1703502645000L));
        when(resultSet.getTimestamp("dateColumn")).thenReturn(timestamp);

        ZonedDateTime result = zonedDateTimeType.get(resultSet, "dateColumn");
        assertNotNull(result);
        assertEquals(1703502645000L, result.toInstant().toEpochMilli());
    }

    @Test
    public void testGetFromResultSetByNameWithNull() throws SQLException {
        when(resultSet.getTimestamp("dateColumn")).thenReturn(null);

        ZonedDateTime result = zonedDateTimeType.get(resultSet, "dateColumn");
        assertNull(result);
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        zonedDateTimeType.set(preparedStatement, 1, zonedDateTime);

        verify(preparedStatement).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSetPreparedStatementWithNull() throws SQLException {
        zonedDateTimeType.set(preparedStatement, 1, null);

        verify(preparedStatement).setTimestamp(1, null);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        zonedDateTimeType.set(callableStatement, "dateParam", zonedDateTime);

        verify(callableStatement).setTimestamp(eq("dateParam"), any(Timestamp.class));
    }

    @Test
    public void testSetCallableStatementWithNull() throws SQLException {
        zonedDateTimeType.set(callableStatement, "dateParam", null);

        verify(callableStatement).setTimestamp("dateParam", null);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        zonedDateTimeType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithValidZonedDateTime() throws IOException {
        StringBuilder sb = new StringBuilder();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        zonedDateTimeType.appendTo(sb, zonedDateTime);

        assertTrue(sb.length() > 0);
        assertFalse(sb.toString().equals("null"));
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();

        zonedDateTimeType.serializeTo(writer, null, null);

        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithZonedDateTimeNoConfig() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        zonedDateTimeType.serializeTo(writer, zonedDateTime, null);

        verify(writer).write(anyString());
    }

    @Test
    public void testSerializeToWithLongFormat() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        zonedDateTimeType.serializeTo(writer, zonedDateTime, config);

        verify(writer).write(anyLong());
    }

    @Test
    public void testSerializeToWithISO8601DateTime() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        zonedDateTimeType.serializeTo(writer, zonedDateTime, config);

        verify(writer).write(anyString());
    }

    @Test
    public void testSerializeToWithISO8601Timestamp() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        zonedDateTimeType.serializeTo(writer, zonedDateTime, config);

        verify(writer).write(anyString());
    }

    @Test
    public void testSerializeToWithQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn('"');

        zonedDateTimeType.serializeTo(writer, zonedDateTime, config);

        verify(writer, times(2)).write('"');
        verify(writer).write(anyString());
    }
}
