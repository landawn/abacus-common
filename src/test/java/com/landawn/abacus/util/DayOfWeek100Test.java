package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class DayOfWeek100Test extends TestBase {

    @Test
    public void testEnumValues() {
        DayOfWeek[] values = DayOfWeek.values();
        assertEquals(7, values.length);

        assertEquals(DayOfWeek.SUNDAY, values[0]);
        assertEquals(DayOfWeek.MONDAY, values[1]);
        assertEquals(DayOfWeek.TUESDAY, values[2]);
        assertEquals(DayOfWeek.WEDNESDAY, values[3]);
        assertEquals(DayOfWeek.THURSDAY, values[4]);
        assertEquals(DayOfWeek.FRIDAY, values[5]);
        assertEquals(DayOfWeek.SATURDAY, values[6]);
    }

    @Test
    public void testIntValue() {
        assertEquals(0, DayOfWeek.SUNDAY.intValue());
        assertEquals(1, DayOfWeek.MONDAY.intValue());
        assertEquals(2, DayOfWeek.TUESDAY.intValue());
        assertEquals(3, DayOfWeek.WEDNESDAY.intValue());
        assertEquals(4, DayOfWeek.THURSDAY.intValue());
        assertEquals(5, DayOfWeek.FRIDAY.intValue());
        assertEquals(6, DayOfWeek.SATURDAY.intValue());
    }

    @Test
    public void testValueOfInt() {
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.valueOf(0));
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.valueOf(1));
        assertEquals(DayOfWeek.TUESDAY, DayOfWeek.valueOf(2));
        assertEquals(DayOfWeek.WEDNESDAY, DayOfWeek.valueOf(3));
        assertEquals(DayOfWeek.THURSDAY, DayOfWeek.valueOf(4));
        assertEquals(DayOfWeek.FRIDAY, DayOfWeek.valueOf(5));
        assertEquals(DayOfWeek.SATURDAY, DayOfWeek.valueOf(6));
    }

    @Test
    public void testValueOfIntInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DayOfWeek.valueOf(-1));
        assertThrows(IllegalArgumentException.class, () -> DayOfWeek.valueOf(7));
        assertThrows(IllegalArgumentException.class, () -> DayOfWeek.valueOf(100));
    }

    @Test
    public void testValueOfString() {
        assertEquals(DayOfWeek.SUNDAY, DayOfWeek.valueOf("SUNDAY"));
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.valueOf("MONDAY"));
        assertEquals(DayOfWeek.TUESDAY, DayOfWeek.valueOf("TUESDAY"));
        assertEquals(DayOfWeek.WEDNESDAY, DayOfWeek.valueOf("WEDNESDAY"));
        assertEquals(DayOfWeek.THURSDAY, DayOfWeek.valueOf("THURSDAY"));
        assertEquals(DayOfWeek.FRIDAY, DayOfWeek.valueOf("FRIDAY"));
        assertEquals(DayOfWeek.SATURDAY, DayOfWeek.valueOf("SATURDAY"));
    }

    @Test
    public void testValueOfStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DayOfWeek.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> DayOfWeek.valueOf("sunday"));
    }

    @Test
    public void testEnumName() {
        assertEquals("SUNDAY", DayOfWeek.SUNDAY.name());
        assertEquals("MONDAY", DayOfWeek.MONDAY.name());
        assertEquals("TUESDAY", DayOfWeek.TUESDAY.name());
        assertEquals("WEDNESDAY", DayOfWeek.WEDNESDAY.name());
        assertEquals("THURSDAY", DayOfWeek.THURSDAY.name());
        assertEquals("FRIDAY", DayOfWeek.FRIDAY.name());
        assertEquals("SATURDAY", DayOfWeek.SATURDAY.name());
    }

    @Test
    public void testEnumOrdinal() {
        assertEquals(0, DayOfWeek.SUNDAY.ordinal());
        assertEquals(1, DayOfWeek.MONDAY.ordinal());
        assertEquals(2, DayOfWeek.TUESDAY.ordinal());
        assertEquals(3, DayOfWeek.WEDNESDAY.ordinal());
        assertEquals(4, DayOfWeek.THURSDAY.ordinal());
        assertEquals(5, DayOfWeek.FRIDAY.ordinal());
        assertEquals(6, DayOfWeek.SATURDAY.ordinal());
    }

    @Test
    public void testEnumToString() {
        assertEquals("SUNDAY", DayOfWeek.SUNDAY.toString());
        assertEquals("MONDAY", DayOfWeek.MONDAY.toString());
        assertEquals("TUESDAY", DayOfWeek.TUESDAY.toString());
        assertEquals("WEDNESDAY", DayOfWeek.WEDNESDAY.toString());
        assertEquals("THURSDAY", DayOfWeek.THURSDAY.toString());
        assertEquals("FRIDAY", DayOfWeek.FRIDAY.toString());
        assertEquals("SATURDAY", DayOfWeek.SATURDAY.toString());
    }

    @Test
    public void testRoundTrip() {
        for (DayOfWeek day : DayOfWeek.values()) {
            assertEquals(day, DayOfWeek.valueOf(day.intValue()));
            assertEquals(day.intValue(), DayOfWeek.valueOf(day.intValue()).intValue());
        }
    }
}
