package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class WeekDay100Test extends TestBase {

    @Test
    public void testIntValue() {
        Assertions.assertEquals(0, WeekDay.SUNDAY.intValue());
        Assertions.assertEquals(1, WeekDay.MONDAY.intValue());
        Assertions.assertEquals(2, WeekDay.TUESDAY.intValue());
        Assertions.assertEquals(3, WeekDay.WEDNESDAY.intValue());
        Assertions.assertEquals(4, WeekDay.THURSDAY.intValue());
        Assertions.assertEquals(5, WeekDay.FRIDAY.intValue());
        Assertions.assertEquals(6, WeekDay.SATURDAY.intValue());
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(WeekDay.SUNDAY, WeekDay.valueOf(0));
        Assertions.assertEquals(WeekDay.MONDAY, WeekDay.valueOf(1));
        Assertions.assertEquals(WeekDay.TUESDAY, WeekDay.valueOf(2));
        Assertions.assertEquals(WeekDay.WEDNESDAY, WeekDay.valueOf(3));
        Assertions.assertEquals(WeekDay.THURSDAY, WeekDay.valueOf(4));
        Assertions.assertEquals(WeekDay.FRIDAY, WeekDay.valueOf(5));
        Assertions.assertEquals(WeekDay.SATURDAY, WeekDay.valueOf(6));

        Assertions.assertSame(WeekDay.MONDAY, WeekDay.valueOf(1));
        Assertions.assertSame(WeekDay.SUNDAY, WeekDay.valueOf(0));
    }

    @Test
    public void testValueOfInvalidValue() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WeekDay.valueOf(-1);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WeekDay.valueOf(7);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WeekDay.valueOf(Integer.MAX_VALUE);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WeekDay.valueOf(Integer.MIN_VALUE);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WeekDay.valueOf(10);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            WeekDay.valueOf(100);
        });
    }

    @Test
    public void testEnumValues() {
        WeekDay[] values = WeekDay.values();
        Assertions.assertEquals(7, values.length);
        Assertions.assertEquals(WeekDay.SUNDAY, values[0]);
        Assertions.assertEquals(WeekDay.MONDAY, values[1]);
        Assertions.assertEquals(WeekDay.TUESDAY, values[2]);
        Assertions.assertEquals(WeekDay.WEDNESDAY, values[3]);
        Assertions.assertEquals(WeekDay.THURSDAY, values[4]);
        Assertions.assertEquals(WeekDay.FRIDAY, values[5]);
        Assertions.assertEquals(WeekDay.SATURDAY, values[6]);
    }

    @Test
    public void testEnumName() {
        Assertions.assertEquals("SUNDAY", WeekDay.SUNDAY.name());
        Assertions.assertEquals("MONDAY", WeekDay.MONDAY.name());
        Assertions.assertEquals("TUESDAY", WeekDay.TUESDAY.name());
        Assertions.assertEquals("WEDNESDAY", WeekDay.WEDNESDAY.name());
        Assertions.assertEquals("THURSDAY", WeekDay.THURSDAY.name());
        Assertions.assertEquals("FRIDAY", WeekDay.FRIDAY.name());
        Assertions.assertEquals("SATURDAY", WeekDay.SATURDAY.name());
    }

    @Test
    public void testEnumOrdinal() {
        Assertions.assertEquals(0, WeekDay.SUNDAY.ordinal());
        Assertions.assertEquals(1, WeekDay.MONDAY.ordinal());
        Assertions.assertEquals(2, WeekDay.TUESDAY.ordinal());
        Assertions.assertEquals(3, WeekDay.WEDNESDAY.ordinal());
        Assertions.assertEquals(4, WeekDay.THURSDAY.ordinal());
        Assertions.assertEquals(5, WeekDay.FRIDAY.ordinal());
        Assertions.assertEquals(6, WeekDay.SATURDAY.ordinal());
    }

    @Test
    public void testRoundTripConversion() {
        for (WeekDay day : WeekDay.values()) {
            int value = day.intValue();
            WeekDay converted = WeekDay.valueOf(value);
            Assertions.assertEquals(day, converted);
        }
    }

    @Test
    public void testUsageInCalendarContext() {
        WeekDay startOfWeek = WeekDay.SUNDAY;
        WeekDay endOfWorkWeek = WeekDay.FRIDAY;

        int daysUntilFriday = endOfWorkWeek.intValue() - startOfWeek.intValue();
        Assertions.assertEquals(5, daysUntilFriday);

        WeekDay saturday = WeekDay.valueOf(6);
        WeekDay sunday = WeekDay.valueOf(0);
        Assertions.assertEquals(WeekDay.SATURDAY, saturday);
        Assertions.assertEquals(WeekDay.SUNDAY, sunday);
    }

    @Test
    public void testComparison() {
        Assertions.assertTrue(WeekDay.SUNDAY.compareTo(WeekDay.MONDAY) < 0);
        Assertions.assertTrue(WeekDay.SATURDAY.compareTo(WeekDay.FRIDAY) > 0);
        Assertions.assertEquals(0, WeekDay.WEDNESDAY.compareTo(WeekDay.WEDNESDAY));

        Assertions.assertTrue(WeekDay.MONDAY.ordinal() < WeekDay.TUESDAY.ordinal());
        Assertions.assertTrue(WeekDay.FRIDAY.ordinal() > WeekDay.THURSDAY.ordinal());
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("SUNDAY", WeekDay.SUNDAY.toString());
        Assertions.assertEquals("MONDAY", WeekDay.MONDAY.toString());
        Assertions.assertEquals("TUESDAY", WeekDay.TUESDAY.toString());
        Assertions.assertEquals("WEDNESDAY", WeekDay.WEDNESDAY.toString());
        Assertions.assertEquals("THURSDAY", WeekDay.THURSDAY.toString());
        Assertions.assertEquals("FRIDAY", WeekDay.FRIDAY.toString());
        Assertions.assertEquals("SATURDAY", WeekDay.SATURDAY.toString());
    }

    @Test
    public void testWeekdaySequence() {
        WeekDay[] days = WeekDay.values();
        for (int i = 0; i < days.length; i++) {
            Assertions.assertEquals(i, days[i].intValue());
            Assertions.assertEquals(days[i], WeekDay.valueOf(i));
        }
    }

    @Test
    public void testWeekdayArithmetic() {
        WeekDay monday = WeekDay.MONDAY;
        int nextDayValue = (monday.intValue() + 1) % 7;
        WeekDay tuesday = WeekDay.valueOf(nextDayValue);
        Assertions.assertEquals(WeekDay.TUESDAY, tuesday);

        int prevDayValue = (monday.intValue() - 1 + 7) % 7;
        WeekDay sunday = WeekDay.valueOf(prevDayValue);
        Assertions.assertEquals(WeekDay.SUNDAY, sunday);

        WeekDay saturday = WeekDay.SATURDAY;
        int nextAfterSaturday = (saturday.intValue() + 1) % 7;
        WeekDay sundayAgain = WeekDay.valueOf(nextAfterSaturday);
        Assertions.assertEquals(WeekDay.SUNDAY, sundayAgain);
    }
}
