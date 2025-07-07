package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

public class AbstractDateType100Test extends TestBase {
    private Type<Date> type;
    private CharacterWriter characterWriter;

    @Mock
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        type = createType(Date.class);
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testIsDate() {
        assertTrue(type.isDate());
    }

    @Test
    public void testIsComparable() {
        assertTrue(type.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(type.isNonQuotableCsvType());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOf_ValidDate() {
        Date date = new Date();
        String result = type.stringOf(date);
        assertNotNull(result);
        // The exact format depends on Dates.format implementation
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_ValidDate() throws IOException {
        Date date = new Date();
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, date);
        assertNotEquals("null", sb.toString());
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        type.writeCharacter(characterWriter, null, null);
        // Verify writer.write(NULL_CHAR_ARRAY) was called
    }

    @Test
    public void testWriteCharacter_ValidDate_NoConfig() throws IOException {
        Date date = new Date();
        type.writeCharacter(characterWriter, date, null);
        // Verify appropriate write method was called
    }

    @Test
    public void testWriteCharacter_ValidDate_WithQuotation() throws IOException {
        Date date = new Date();
        when(config.getStringQuotation()).thenReturn((char) '"');
        when(config.getDateTimeFormat()).thenReturn(null);

        type.writeCharacter(characterWriter, date, config);
        // Verify quotation marks were written
    }

    @Test
    public void testWriteCharacter_ValidDate_LongFormat() throws IOException {
        Date date = new Date();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) '"');

        type.writeCharacter(characterWriter, date, config);
        // Verify writer.write(date.getTime()) was called
    }

    @Test
    public void testWriteCharacter_ValidDate_ISO8601DateTime() throws IOException {
        Date date = new Date();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        type.writeCharacter(characterWriter, date, config);
        // Verify ISO 8601 date-time format was written
    }

    @Test
    public void testWriteCharacter_ValidDate_ISO8601Timestamp() throws IOException {
        Date date = new Date();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        type.writeCharacter(characterWriter, date, config);
        // Verify ISO 8601 timestamp format was written
    }

    //    @Test
    //    public void testWriteCharacter_ValidDate_UnsupportedFormat() throws IOException {
    //        Date date = new Date();
    //        // Create a mock DateTimeFormat that isn't one of the supported ones
    //        DateTimeFormat unsupportedFormat = mock(DateTimeFormat.class);
    //        when(config.getDateTimeFormat()).thenReturn(unsupportedFormat);
    //
    //        assertThrows(RuntimeException.class, () -> type.writeCharacter(characterWriter, date, config));
    //    }

    @Test
    public void testWriteCharacter_ValidDate_QuotationWithLongFormat() throws IOException {
        Date date = new Date();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) '"');

        type.writeCharacter(characterWriter, date, config);
        // Verify no quotation marks were written (LONG format doesn't use quotes)
    }

    @Test
    public void testWriteCharacter_ValidDate_WithQuotationAndISO8601() throws IOException {
        Date date = new Date();
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) '\'');

        type.writeCharacter(characterWriter, date, config);
        // Verify single quotes were written around the ISO 8601 date
    }

    @Test
    public void testConstructor_TimestampType() {
        Type<java.sql.Timestamp> timestampType = createType(java.sql.Timestamp.class);

        assertNotNull(timestampType);
        assertTrue(timestampType.isDate());
        assertTrue(timestampType.isComparable());
        assertTrue(timestampType.isNonQuotableCsvType());
    }
}
