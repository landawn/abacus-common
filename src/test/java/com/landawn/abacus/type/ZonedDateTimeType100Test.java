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
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

public class ZonedDateTimeType100Test extends TestBase {

    private ZonedDateTimeType zonedDateTimeType;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        zonedDateTimeType = new ZonedDateTimeType();
    }

    @Test
    public void testClazz() {
        Class<ZonedDateTime> clazz = zonedDateTimeType.clazz();
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
    public void testValueOfObjectWithNull() {
        ZonedDateTime result = zonedDateTimeType.valueOf((Object) null);
        assertNull(result);
    }

    @Test
    public void testValueOfObjectWithNumber() {
        long epochMilli = 1703502645000L; // 2023-12-25 10:30:45 UTC
        ZonedDateTime result = zonedDateTimeType.valueOf((Object) epochMilli);
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
        String iso8601DateTime = "2023-12-25T10:30:45Z"; // 20 chars with Z
        ZonedDateTime result = zonedDateTimeType.valueOf(iso8601DateTime);
        assertNotNull(result);
        assertEquals(2023, result.getYear());
        assertEquals(12, result.getMonthValue());
        assertEquals(25, result.getDayOfMonth());
    }

    @Test
    public void testValueOfStringWithISO8601Timestamp() {
        String iso8601Timestamp = "2023-12-25T10:30:45.123Z"; // 24 chars with Z
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
    public void testValueOfStringWithInvalidFormat() {
        assertThrows(DateTimeParseException.class, () -> {
            zonedDateTimeType.valueOf("invalid-date-time");
        });
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
    public void testWriteCharacterWithNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();

        zonedDateTimeType.writeCharacter(writer, null, null);

        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithZonedDateTimeNoConfig() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        zonedDateTimeType.writeCharacter(writer, zonedDateTime, null);

        verify(writer).write(anyString());
    }

    @Test
    public void testWriteCharacterWithLongFormat() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        zonedDateTimeType.writeCharacter(writer, zonedDateTime, config);

        verify(writer).write(anyLong());
    }

    @Test
    public void testWriteCharacterWithISO8601DateTime() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        zonedDateTimeType.writeCharacter(writer, zonedDateTime, config);

        verify(writer).write(anyString());
    }

    @Test
    public void testWriteCharacterWithISO8601Timestamp() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        zonedDateTimeType.writeCharacter(writer, zonedDateTime, config);

        verify(writer).write(anyString());
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2023, 12, 25, 10, 30, 45, 0, ZoneId.of("UTC"));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn('"');

        zonedDateTimeType.writeCharacter(writer, zonedDateTime, config);

        verify(writer, times(2)).write('"');
        verify(writer).write(anyString());
    }
}
