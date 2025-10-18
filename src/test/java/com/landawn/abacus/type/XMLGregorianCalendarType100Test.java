package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

@Tag("new-test")
public class XMLGregorianCalendarType100Test extends TestBase {

    private XMLGregorianCalendarType xmlCalendarType;
    private XMLGregorianCalendar testCalendar;

    @BeforeEach
    public void setUp() throws Exception {
        xmlCalendarType = (XMLGregorianCalendarType) createType(XMLGregorianCalendar.class);
        DatatypeFactory factory = DatatypeFactory.newInstance();
        testCalendar = factory.newXMLGregorianCalendar("2023-01-15T10:30:00");
    }

    @Test
    public void testClazz() {
        Class<?> clazz = xmlCalendarType.clazz();
        assertNotNull(clazz);
        assertEquals(XMLGregorianCalendar.class, clazz);
    }

    @Test
    public void testValueOf() {
        XMLGregorianCalendar result = xmlCalendarType.valueOf("2023-01-15T10:30:00");
        assertNotNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        XMLGregorianCalendar result = xmlCalendarType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        XMLGregorianCalendar result = xmlCalendarType.valueOf((String) null);
        assertNull(result);
    }

    @Test
    public void testValueOfSysTime() {
        XMLGregorianCalendar result = xmlCalendarType.valueOf("sysTime");
        assertNotNull(result);
    }

    @Test
    public void testValueOfCharArray() {
        char[] chars = "2023-01-15T10:30:00".toCharArray();
        XMLGregorianCalendar result = xmlCalendarType.valueOf(chars, 0, chars.length);
        assertNotNull(result);
    }

    @Test
    public void testValueOfCharArrayNull() {
        XMLGregorianCalendar result = xmlCalendarType.valueOf(null, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOfCharArrayEmpty() {
        char[] chars = new char[0];
        XMLGregorianCalendar result = xmlCalendarType.valueOf(chars, 0, 0);
        assertNull(result);
    }

    @Test
    public void testValueOfCharArrayLong() {
        char[] chars = "1234567890".toCharArray();
        XMLGregorianCalendar result = xmlCalendarType.valueOf(chars, 0, chars.length);
        assertNotNull(result);
    }

    @Test
    public void testStringOf() {
        String result = xmlCalendarType.stringOf(testCalendar);
        assertNotNull(result);
    }

    @Test
    public void testStringOfNull() {
        String result = xmlCalendarType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testGetFromResultSetByIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp(1)).thenReturn(timestamp);

        XMLGregorianCalendar result = xmlCalendarType.get(rs, 1);
        assertNotNull(result);
    }

    @Test
    public void testGetFromResultSetByIndexNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp(1)).thenReturn(null);

        XMLGregorianCalendar result = xmlCalendarType.get(rs, 1);
        assertNull(result);
    }

    @Test
    public void testGetFromResultSetByLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        when(rs.getTimestamp("date_column")).thenReturn(timestamp);

        XMLGregorianCalendar result = xmlCalendarType.get(rs, "date_column");
        assertNotNull(result);
    }

    @Test
    public void testGetFromResultSetByLabelNull() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("date_column")).thenReturn(null);

        XMLGregorianCalendar result = xmlCalendarType.get(rs, "date_column");
        assertNull(result);
    }

    @Test
    public void testSetInPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        xmlCalendarType.set(stmt, 1, testCalendar);

        verify(stmt).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSetInPreparedStatementNull() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);

        xmlCalendarType.set(stmt, 1, null);

        verify(stmt).setTimestamp(1, null);
    }

    @Test
    public void testSetInCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        xmlCalendarType.set(stmt, "date_param", testCalendar);

        verify(stmt).setTimestamp(eq("date_param"), any(Timestamp.class));
    }

    @Test
    public void testSetInCallableStatementNull() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);

        xmlCalendarType.set(stmt, "date_param", null);

        verify(stmt).setTimestamp("date_param", null);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();
        xmlCalendarType.appendTo(writer, testCalendar);
        String result = writer.toString();
        assertNotNull(result);
        assertFalse(result.equals("null"));
    }

    @Test
    public void testAppendToNull() throws IOException {
        StringWriter writer = new StringWriter();
        xmlCalendarType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();

        xmlCalendarType.writeCharacter(writer, testCalendar, null);

    }

    @Test
    public void testWriteCharacterNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();

        xmlCalendarType.writeCharacter(writer, null, null);

        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithConfigLong() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        xmlCalendarType.writeCharacter(writer, testCalendar, config);

        verify(writer).write(anyString());
    }

    @Test
    public void testWriteCharacterWithConfigISO8601DateTime() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        xmlCalendarType.writeCharacter(writer, testCalendar, config);

        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testWriteCharacterWithConfigISO8601Timestamp() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        xmlCalendarType.writeCharacter(writer, testCalendar, config);

        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn('"');

        xmlCalendarType.writeCharacter(writer, testCalendar, config);

        verify(writer, atLeast(2)).write('"');
    }
}
