package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Objectory;

public class AbstractDateTypeTest extends TestBase {
    private Type<Date> type;
    private CharacterWriter characterWriter;

    @Mock
    private JsonXmlSerConfig<?> config;

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
    public void testConstructor_TimestampType() {
        Type<java.sql.Timestamp> timestampType = createType(java.sql.Timestamp.class);

        assertNotNull(timestampType);
        assertTrue(timestampType.isDate());
        assertTrue(timestampType.isComparable());
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
    public void testSerializeTo_ValidDate_NoConfig() throws IOException {
        assertDoesNotThrow(() -> {
            Date date = new Date();
            type.serializeTo(characterWriter, date, null);
        });
    }

    @Test
    public void testSerializeTo_ValidDate_WithQuotation() throws IOException {
        assertDoesNotThrow(() -> {
            Date date = new Date();
            when(config.getStringQuotation()).thenReturn('"');
            when(config.getDateTimeFormat()).thenReturn(null);

            type.serializeTo(characterWriter, date, config);
        });
    }

    @Test
    public void testSerializeTo_ValidDate_ISO8601DateTime() throws IOException {
        assertDoesNotThrow(() -> {
            Date date = new Date();
            when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
            when(config.getStringQuotation()).thenReturn((char) 0);

            type.serializeTo(characterWriter, date, config);
        });
    }

    @Test
    public void testSerializeTo_ValidDate_ISO8601Timestamp() throws IOException {
        assertDoesNotThrow(() -> {
            Date date = new Date();
            when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
            when(config.getStringQuotation()).thenReturn((char) 0);

            type.serializeTo(characterWriter, date, config);
        });
    }

    @Test
    public void testSerializeTo_ValidDate_QuotationWithLongFormat() throws IOException {
        assertDoesNotThrow(() -> {
            Date date = new Date();
            when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
            when(config.getStringQuotation()).thenReturn('"');

            type.serializeTo(characterWriter, date, config);
        });
    }

    @Test
    public void testSerializeTo_ValidDate_WithQuotationAndISO8601() throws IOException {
        assertDoesNotThrow(() -> {
            Date date = new Date();
            when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
            when(config.getStringQuotation()).thenReturn('\'');

            type.serializeTo(characterWriter, date, config);
        });
    }

    // --- review fixes 2026-09-06 (T9-06) ---

    @Test
    public void reviewFixes20260906_T906_textFormsRejectYearsOutsideCommonEra0001To9999() throws IOException {
        // documented now: stringOf / appendTo / serializeTo (text formats) throw IAE outside CE 0001..9999
        final Date max = new Date(Long.MAX_VALUE);
        final Date year10000 = new Date(253402300800000L);
        final Date lastInRange = new Date(253402300799999L);

        assertThrows(IllegalArgumentException.class, () -> type.stringOf(max));
        assertThrows(IllegalArgumentException.class, () -> type.stringOf(year10000));
        assertEquals("9999-12-31T23:59:59Z", type.stringOf(lastInRange));

        assertThrows(IllegalArgumentException.class, () -> type.appendTo(new StringBuilder(), max));
        final StringBuilder sb = new StringBuilder();
        type.appendTo(sb, lastInRange);
        assertEquals("9999-12-31T23:59:59Z", sb.toString());

        when(config.getStringQuotation()).thenReturn((char) 0);

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_TIMESTAMP);
        assertThrows(IllegalArgumentException.class, () -> type.serializeTo(Objectory.createBufferedJsonWriter(), max, config));

        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.ISO_8601_DATE_TIME);
        assertThrows(IllegalArgumentException.class, () -> type.serializeTo(Objectory.createBufferedJsonWriter(), max, config));

        assertThrows(IllegalArgumentException.class, () -> type.serializeTo(Objectory.createBufferedJsonWriter(), max, null));

        // LONG is unaffected: any instant is written as millis
        when(config.getDateTimeFormat()).thenReturn(DateTimeFormat.LONG);
        final BufferedJsonWriter real = Objectory.createBufferedJsonWriter();
        type.serializeTo(real, max, config);
        assertEquals(String.valueOf(Long.MAX_VALUE), real.toString());

        // the Timestamp handler shares the contract
        final Type<java.sql.Timestamp> timestampType = createType(java.sql.Timestamp.class);
        assertThrows(IllegalArgumentException.class, () -> timestampType.stringOf(new java.sql.Timestamp(Long.MAX_VALUE)));
        assertNull(timestampType.stringOf(null));
    }
}
