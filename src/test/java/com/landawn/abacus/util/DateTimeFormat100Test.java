package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DateTimeFormat100Test extends TestBase {

    @Test
    public void testEnumValues() {
        DateTimeFormat[] values = DateTimeFormat.values();
        assertEquals(3, values.length);
        
        assertEquals(DateTimeFormat.LONG, values[0]);
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, values[1]);
        assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, values[2]);
    }

    @Test
    public void testValueOf() {
        assertEquals(DateTimeFormat.LONG, DateTimeFormat.valueOf("LONG"));
        assertEquals(DateTimeFormat.ISO_8601_DATE_TIME, DateTimeFormat.valueOf("ISO_8601_DATE_TIME"));
        assertEquals(DateTimeFormat.ISO_8601_TIMESTAMP, DateTimeFormat.valueOf("ISO_8601_TIMESTAMP"));
    }

    @Test
    public void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeFormat.valueOf("INVALID"));
    }

    @Test
    public void testEnumToString() {
        assertEquals("LONG", DateTimeFormat.LONG.toString());
        assertEquals("ISO_8601_DATE_TIME", DateTimeFormat.ISO_8601_DATE_TIME.toString());
        assertEquals("ISO_8601_TIMESTAMP", DateTimeFormat.ISO_8601_TIMESTAMP.toString());
    }

    @Test
    public void testEnumName() {
        assertEquals("LONG", DateTimeFormat.LONG.name());
        assertEquals("ISO_8601_DATE_TIME", DateTimeFormat.ISO_8601_DATE_TIME.name());
        assertEquals("ISO_8601_TIMESTAMP", DateTimeFormat.ISO_8601_TIMESTAMP.name());
    }

    @Test
    public void testEnumOrdinal() {
        assertEquals(0, DateTimeFormat.LONG.ordinal());
        assertEquals(1, DateTimeFormat.ISO_8601_DATE_TIME.ordinal());
        assertEquals(2, DateTimeFormat.ISO_8601_TIMESTAMP.ordinal());
    }
}