package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class OffsetDateTimeType100Test extends TestBase {

    private OffsetDateTimeType offsetDateTimeType;
    private CharacterWriter writer;
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        offsetDateTimeType = (OffsetDateTimeType) createType("OffsetDateTime");
        writer = createCharacterWriter();
        config = mock(JSONXMLSerializationConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(OffsetDateTime.class, offsetDateTimeType.clazz());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(offsetDateTimeType.stringOf(null));
    }

    @Test
    public void testStringOfWithValue() {
        OffsetDateTime dateTime = OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 0, ZoneOffset.UTC);
        String result = offsetDateTimeType.stringOf(dateTime);
        assertNotNull(result);
        assertTrue(result.contains("2023"));
        assertTrue(result.contains("05"));
        assertTrue(result.contains("15"));
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(offsetDateTimeType.valueOf((String) null));
        assertNull(offsetDateTimeType.valueOf((Object) null));
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
    public void testWriteCharacterWithNull() throws IOException {
        offsetDateTimeType.writeCharacter(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithValueNoConfig() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        offsetDateTimeType.writeCharacter(writer, dateTime, null);
        verify(writer).write(anyString());
    }

    @Test
    public void testWriteCharacterWithLongFormat() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        offsetDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer).write(anyLong());
    }

    @Test
    public void testWriteCharacterWithISO8601DateTimeFormat() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        offsetDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer).write(anyString());
    }

    @Test
    public void testWriteCharacterWithISO8601TimestampFormat() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        offsetDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer).write(anyString());
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        OffsetDateTime dateTime = OffsetDateTime.now();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn('"');

        offsetDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer, times(2)).write('"');   // Opening and closing quotes
        verify(writer).write(anyString());
    }
}
