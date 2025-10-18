package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CalendarField2025Test extends TestBase {

    @Test
    public void testValue() {
        assertEquals(Calendar.MILLISECOND, CalendarField.MILLISECOND.value());
        assertEquals(Calendar.SECOND, CalendarField.SECOND.value());
        assertEquals(Calendar.MINUTE, CalendarField.MINUTE.value());
        assertEquals(Calendar.HOUR_OF_DAY, CalendarField.HOUR_OF_DAY.value());
        assertEquals(Calendar.DAY_OF_MONTH, CalendarField.DAY_OF_MONTH.value());
        assertEquals(Calendar.WEEK_OF_YEAR, CalendarField.WEEK_OF_YEAR.value());
        assertEquals(Calendar.MONTH, CalendarField.MONTH.value());
        assertEquals(Calendar.YEAR, CalendarField.YEAR.value());
    }

    @Test
    public void testOf_withValidValues() {
        assertEquals(CalendarField.MILLISECOND, CalendarField.of(Calendar.MILLISECOND));
        assertEquals(CalendarField.SECOND, CalendarField.of(Calendar.SECOND));
        assertEquals(CalendarField.MINUTE, CalendarField.of(Calendar.MINUTE));
        assertEquals(CalendarField.HOUR_OF_DAY, CalendarField.of(Calendar.HOUR_OF_DAY));
        assertEquals(CalendarField.DAY_OF_MONTH, CalendarField.of(Calendar.DAY_OF_MONTH));
        assertEquals(CalendarField.WEEK_OF_YEAR, CalendarField.of(Calendar.WEEK_OF_YEAR));
        assertEquals(CalendarField.MONTH, CalendarField.of(Calendar.MONTH));
        assertEquals(CalendarField.YEAR, CalendarField.of(Calendar.YEAR));
    }

    @Test
    public void testOf_withInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> CalendarField.of(999));
        assertThrows(IllegalArgumentException.class, () -> CalendarField.of(-1));
        assertThrows(IllegalArgumentException.class, () -> CalendarField.of(Calendar.HOUR)); // Not supported
        assertThrows(IllegalArgumentException.class, () -> CalendarField.of(Calendar.AM_PM)); // Not supported
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testValueOf_deprecated() {
        assertEquals(CalendarField.MILLISECOND, CalendarField.valueOf(Calendar.MILLISECOND));
        assertEquals(CalendarField.SECOND, CalendarField.valueOf(Calendar.SECOND));
        assertEquals(CalendarField.MINUTE, CalendarField.valueOf(Calendar.MINUTE));
        assertEquals(CalendarField.HOUR_OF_DAY, CalendarField.valueOf(Calendar.HOUR_OF_DAY));
        assertEquals(CalendarField.DAY_OF_MONTH, CalendarField.valueOf(Calendar.DAY_OF_MONTH));
        assertEquals(CalendarField.WEEK_OF_YEAR, CalendarField.valueOf(Calendar.WEEK_OF_YEAR));
        assertEquals(CalendarField.MONTH, CalendarField.valueOf(Calendar.MONTH));
        assertEquals(CalendarField.YEAR, CalendarField.valueOf(Calendar.YEAR));
    }

    @Test
    public void testValueOf_withStringName() {
        assertEquals(CalendarField.MILLISECOND, CalendarField.valueOf("MILLISECOND"));
        assertEquals(CalendarField.SECOND, CalendarField.valueOf("SECOND"));
        assertEquals(CalendarField.MINUTE, CalendarField.valueOf("MINUTE"));
        assertEquals(CalendarField.HOUR_OF_DAY, CalendarField.valueOf("HOUR_OF_DAY"));
        assertEquals(CalendarField.DAY_OF_MONTH, CalendarField.valueOf("DAY_OF_MONTH"));
        assertEquals(CalendarField.WEEK_OF_YEAR, CalendarField.valueOf("WEEK_OF_YEAR"));
        assertEquals(CalendarField.MONTH, CalendarField.valueOf("MONTH"));
        assertEquals(CalendarField.YEAR, CalendarField.valueOf("YEAR"));
    }

    @Test
    public void testValues() {
        CalendarField[] values = CalendarField.values();
        assertEquals(8, values.length);
        assertEquals(CalendarField.MILLISECOND, values[0]);
        assertEquals(CalendarField.YEAR, values[values.length - 1]);
    }

    @Test
    public void testEnumName() {
        assertEquals("MILLISECOND", CalendarField.MILLISECOND.name());
        assertEquals("SECOND", CalendarField.SECOND.name());
        assertEquals("MINUTE", CalendarField.MINUTE.name());
        assertEquals("HOUR_OF_DAY", CalendarField.HOUR_OF_DAY.name());
        assertEquals("DAY_OF_MONTH", CalendarField.DAY_OF_MONTH.name());
        assertEquals("WEEK_OF_YEAR", CalendarField.WEEK_OF_YEAR.name());
        assertEquals("MONTH", CalendarField.MONTH.name());
        assertEquals("YEAR", CalendarField.YEAR.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("MILLISECOND", CalendarField.MILLISECOND.toString());
        assertEquals("SECOND", CalendarField.SECOND.toString());
        assertEquals("MINUTE", CalendarField.MINUTE.toString());
        assertEquals("HOUR_OF_DAY", CalendarField.HOUR_OF_DAY.toString());
        assertEquals("DAY_OF_MONTH", CalendarField.DAY_OF_MONTH.toString());
        assertEquals("WEEK_OF_YEAR", CalendarField.WEEK_OF_YEAR.toString());
        assertEquals("MONTH", CalendarField.MONTH.toString());
        assertEquals("YEAR", CalendarField.YEAR.toString());
    }

    @Test
    public void testIntegrationWithCalendar() {
        Calendar cal = Calendar.getInstance();

        // Test that CalendarField values can be used with Calendar methods
        cal.set(CalendarField.YEAR.value(), 2024);
        assertEquals(2024, cal.get(CalendarField.YEAR.value()));

        cal.set(CalendarField.MONTH.value(), Calendar.JANUARY);
        assertEquals(Calendar.JANUARY, cal.get(CalendarField.MONTH.value()));

        cal.set(CalendarField.DAY_OF_MONTH.value(), 15);
        assertEquals(15, cal.get(CalendarField.DAY_OF_MONTH.value()));
    }

    @Test
    public void testOrdinal() {
        assertEquals(0, CalendarField.MILLISECOND.ordinal());
        assertEquals(1, CalendarField.SECOND.ordinal());
        assertEquals(2, CalendarField.MINUTE.ordinal());
        assertEquals(3, CalendarField.HOUR_OF_DAY.ordinal());
        assertEquals(4, CalendarField.DAY_OF_MONTH.ordinal());
        assertEquals(5, CalendarField.WEEK_OF_YEAR.ordinal());
        assertEquals(6, CalendarField.MONTH.ordinal());
        assertEquals(7, CalendarField.YEAR.ordinal());
    }
}
