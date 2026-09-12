package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

public class XMLGregorianCalendarTypeTest extends TestBase {

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
        Class<?> clazz = xmlCalendarType.javaType();
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
    public void testValueOfNullStringLiteral() {
        XMLGregorianCalendar result = xmlCalendarType.valueOf("null");
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
    public void testValueOfCharArrayNullLiteral() {
        char[] chars = "null".toCharArray();
        XMLGregorianCalendar result = xmlCalendarType.valueOf(chars, 0, chars.length);
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
    public void testStringOfValueOfRoundTripPreservesOffsetAndFraction() throws Exception {
        final XMLGregorianCalendar value = DatatypeFactory.newInstance().newXMLGregorianCalendar("2025-01-15T10:30:45.123+05:30");
        final String text = xmlCalendarType.stringOf(value);

        assertEquals("2025-01-15T10:30:45.123+05:30", text);
        assertEquals(DatatypeConstants.EQUAL, value.compare(xmlCalendarType.valueOf(text)));
    }

    @Test
    public void testStringOfPreservesUndefinedTimezoneButValueOfResolvesIt() throws Exception {
        final XMLGregorianCalendar value = DatatypeFactory.newInstance().newXMLGregorianCalendar("2025-01-15T10:30:45.123");
        final String text = xmlCalendarType.stringOf(value);
        final XMLGregorianCalendar parsed = xmlCalendarType.valueOf(text);

        assertEquals("2025-01-15T10:30:45.123", text);
        assertEquals(DatatypeConstants.FIELD_UNDEFINED, value.getTimezone());
        assertFalse(parsed.getTimezone() == DatatypeConstants.FIELD_UNDEFINED);
        assertFalse(value.compare(parsed) == DatatypeConstants.EQUAL);

        // The XML lexical parser is the exact inverse when an undefined timezone must remain undefined.
        assertEquals(DatatypeConstants.EQUAL, value.compare(DatatypeFactory.newInstance().newXMLGregorianCalendar(text)));
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
        assertEquals(testCalendar.toXMLFormat(), result);
        assertEquals(xmlCalendarType.stringOf(testCalendar), result);
    }

    @Test
    public void testAppendToNull() throws IOException {
        StringWriter writer = new StringWriter();
        xmlCalendarType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testSerializeTo() throws IOException {
        CharacterWriter writer = createCharacterWriter();

        xmlCalendarType.serializeTo(writer, testCalendar, null);
        assertNotNull(writer);
    }

    @Test
    public void testSerializeToNull() throws IOException {
        CharacterWriter writer = createCharacterWriter();

        xmlCalendarType.serializeTo(writer, null, null);

        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithConfigLong() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        xmlCalendarType.serializeTo(writer, testCalendar, config);

        verify(writer).write(anyString());
    }

    @Test
    public void testSerializeToWithConfigISO8601DateTime() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        xmlCalendarType.serializeTo(writer, testCalendar, config);

        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testSerializeToWithConfigISO8601Timestamp() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        xmlCalendarType.serializeTo(writer, testCalendar, config);

        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testSerializeToWithQuotation() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn('"');

        xmlCalendarType.serializeTo(writer, testCalendar, config);

        verify(writer, atLeast(2)).write('"');
    }

    // --- review fixes 2026-09-06 (T9-02, T9-03): the char[] fast path is a sibling of the ones pinned in
    // DateTypeTest / CalendarTypeTest / TimestampTypeTest and carries the identical guard.

    @Test
    public void reviewFixes20260906_T902_T903_charArrayAgreesWithStringOverload() {
        // a trailing type suffix used to be stripped by parseLong(char[]) only, so the two overloads disagreed
        for (final String s : new String[] { "1700000000000L", "1700000000000d", "12345L" }) {
            assertThrows(IllegalArgumentException.class, () -> xmlCalendarType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> xmlCalendarType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        // overflowing text used to escape the char[] path as ArithmeticException("long overflow")
        for (final String s : new String[] { "99999999999999999999", "9223372036854775808", "-9223372036854775809" }) {
            assertThrows(IllegalArgumentException.class, () -> xmlCalendarType.valueOf(s), s);
            assertThrows(IllegalArgumentException.class, () -> xmlCalendarType.valueOf(s.toCharArray(), 0, s.length()), s);
        }

        for (final String s : new String[] { "1700000000000", "+1700000000000", "-1700000000000" }) {
            assertEquals(Long.parseLong(s), xmlCalendarType.valueOf(s.toCharArray(), 0, s.length()).toGregorianCalendar().getTimeInMillis(), s);
            assertEquals(Long.parseLong(s), xmlCalendarType.valueOf(s).toGregorianCalendar().getTimeInMillis(), s);
        }

        assertNull(xmlCalendarType.valueOf((char[]) null, 0, 0));
        assertNull(xmlCalendarType.valueOf(new char[0], 0, 0));
    }
}
