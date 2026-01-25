package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

@Tag("new-test")
public class AbstractJodaDateTimeType100Test extends TestBase {

    private JodaDateTimeType jodaDateTimeType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        jodaDateTimeType = createType("JodaDateTime");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
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
    public void testIsNonQuotableCsvType() {
        assertTrue(jodaDateTimeType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOf() {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        String result = jodaDateTimeType.stringOf(dateTime);
        assertNotNull(result);
        assertTrue(result.contains("2023-06-15"));
        assertTrue(result.contains("10:30:45"));

        assertNull(jodaDateTimeType.stringOf(null));
    }

    @Test
    public void testAppendTo() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
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
    public void testWriteCharacterWithNullValue() throws IOException {
        jodaDateTimeType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testWriteCharacterWithLongFormat() throws IOException {
        DateTime dateTime = new DateTime(1234567890123L);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer).write(1234567890123L);
    }

    @Test
    public void testWriteCharacterWithISO8601DateTime() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testWriteCharacterWithISO8601Timestamp() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testWriteCharacterWithQuotation() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn('"');

        jodaDateTimeType.writeCharacter(writer, dateTime, config);
        verify(writer, times(2)).write('"');
    }

    @Test
    public void testWriteCharacterWithNullConfig() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);

        jodaDateTimeType.writeCharacter(writer, dateTime, null);
        verify(writer, atLeastOnce()).append(anyString());
    }

    @Test
    public void testWriteCharacterWithUnsupportedFormat() throws IOException {
        DateTime dateTime = new DateTime(2023, 6, 15, 10, 30, 45, 123);
        DateTimeFormat unsupportedFormat = mock(DateTimeFormat.class);
        when(config.getDateTimeFormat()).thenReturn(unsupportedFormat);
        when(config.getStringQuotation()).thenReturn((char) 0);

        jodaDateTimeType.writeCharacter(writer, dateTime, config);
    }
}
