package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;

@Tag("new-test")
public class AbstractCalendarType100Test extends TestBase {
    private Type<Calendar> type;
    private CharacterWriter characterWriter;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        type = createType(Calendar.class);
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testIsCalendar() {
        assertTrue(type.isCalendar());
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
    public void testStringOf_ValidCalendar() {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15, 10, 30, 45);
        String result = type.stringOf(calendar);
        assertNotNull(result);
    }

    @Test
    public void testSet_PreparedStatement_Null() throws SQLException {
        type.set(preparedStatement, 1, null);
        verify(preparedStatement).setTimestamp(1, null);
    }

    @Test
    public void testSet_PreparedStatement_ValidCalendar() throws SQLException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        type.set(preparedStatement, 1, calendar);
        verify(preparedStatement).setTimestamp(eq(1), any(Timestamp.class));
    }

    @Test
    public void testSet_CallableStatement_Null() throws SQLException {
        type.set(callableStatement, "param", null);
        verify(callableStatement).setTimestamp("param", null);
    }

    @Test
    public void testSet_CallableStatement_ValidCalendar() throws SQLException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        type.set(callableStatement, "param", calendar);
        verify(callableStatement).setTimestamp(eq("param"), any(Timestamp.class));
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendTo_ValidCalendar() throws IOException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        StringBuilder sb = new StringBuilder();
        type.appendTo(sb, calendar);
        assertNotEquals("null", sb.toString());
        assertTrue(sb.length() > 0);
    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        type.writeCharacter(characterWriter, null, null);
    }

    @Test
    public void testWriteCharacter_ValidCalendar_NoConfig() throws IOException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        type.writeCharacter(characterWriter, calendar, null);
    }

    @Test
    public void testWriteCharacter_ValidCalendar_WithQuotation() throws IOException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        when(config.getStringQuotation()).thenReturn((char) '"');
        when(config.getDateTimeFormat()).thenReturn(null);

        type.writeCharacter(characterWriter, calendar, config);
    }

    @Test
    public void testWriteCharacter_ValidCalendar_LongFormat() throws IOException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) '"');

        type.writeCharacter(characterWriter, calendar, config);
    }

    @Test
    public void testWriteCharacter_ValidCalendar_ISO8601DateTime() throws IOException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        when(config.getStringQuotation()).thenReturn((char) 0);

        type.writeCharacter(characterWriter, calendar, config);
    }

    @Test
    public void testWriteCharacter_ValidCalendar_ISO8601Timestamp() throws IOException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        when(config.getStringQuotation()).thenReturn((char) 0);

        type.writeCharacter(characterWriter, calendar, config);
    }

    @Test
    public void testWriteCharacter_ValidCalendar_QuotationWithLongFormat() throws IOException {
        Calendar calendar = new GregorianCalendar(2023, Calendar.JANUARY, 15);
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        when(config.getStringQuotation()).thenReturn((char) '"');

        type.writeCharacter(characterWriter, calendar, config);
    }
}
