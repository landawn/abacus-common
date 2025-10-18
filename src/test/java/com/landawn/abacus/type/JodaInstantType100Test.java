package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.joda.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

@Tag("new-test")
public class JodaInstantType100Test extends TestBase {

    private JodaInstantType instantType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        instantType = (JodaInstantType) createType("JodaInstant");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(Instant.class, instantType.clazz());
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
    public void testValueOf_String_Null() {
        assertNull(instantType.valueOf((String) null));
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
    public void testValueOf_String_ISO8601Timestamp() {
        String str = "2023-12-25T10:30:45.123Z";
        Instant result = instantType.valueOf(str);
        assertNotNull(result);
    }

    @Test
    public void testValueOf_CharArray_Null() {
        assertNull(instantType.valueOf(null, 0, 0));
    }

    @Test
    public void testValueOf_CharArray_Empty() {
        char[] cbuf = new char[0];
        assertNull(instantType.valueOf(cbuf, 0, 0));
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
    public void testGet_ResultSet_ByIndex_Null() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        assertNull(instantType.get(rs, 1));
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
    public void testSet_PreparedStatement_Null() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        instantType.set(stmt, 1, null);
        verify(stmt).setTimestamp(1, null);
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
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();

        instantType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        instantType.writeCharacter(characterWriter, null, null);
        verify(characterWriter).write(any(char[].class));
    }

    @Test
    public void testWriteCharacter_NoConfig() throws IOException {
        Instant instant = new Instant(1703502645123L);

        instantType.writeCharacter(characterWriter, instant, null);
        verify(characterWriter).write(anyString());
    }

    @Test
    public void testWriteCharacter_LongFormat() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.writeCharacter(characterWriter, instant, config);
        verify(characterWriter).write(1703502645123L);
    }

    @Test
    public void testWriteCharacter_ISO8601DateTime() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.writeCharacter(characterWriter, instant, config);
        verify(characterWriter).write(anyString());
    }

    @Test
    public void testWriteCharacter_ISO8601Timestamp() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        instantType.writeCharacter(characterWriter, instant, config);
        verify(characterWriter).write(anyString());
    }

    @Test
    public void testWriteCharacter_WithQuotes() throws IOException {
        Instant instant = new Instant(1703502645123L);
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn('"');

        instantType.writeCharacter(characterWriter, instant, config);
        verify(characterWriter, times(2)).write('"');
        verify(characterWriter).write(anyString());
    }
}
