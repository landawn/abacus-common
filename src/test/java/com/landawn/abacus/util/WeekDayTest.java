package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class WeekDayTest extends TestBase {

    @Test
    public void testIntValue_SUNDAY() {
        assertEquals(0, WeekDay.SUNDAY.intValue());
    }

    @Test
    public void testIntValue_MONDAY() {
        assertEquals(1, WeekDay.MONDAY.intValue());
    }

    @Test
    public void testIntValue_TUESDAY() {
        assertEquals(2, WeekDay.TUESDAY.intValue());
    }

    @Test
    public void testIntValue_WEDNESDAY() {
        assertEquals(3, WeekDay.WEDNESDAY.intValue());
    }

    @Test
    public void testIntValue_THURSDAY() {
        assertEquals(4, WeekDay.THURSDAY.intValue());
    }

    @Test
    public void testIntValue_FRIDAY() {
        assertEquals(5, WeekDay.FRIDAY.intValue());
    }

    @Test
    public void testIntValue_SATURDAY() {
        assertEquals(6, WeekDay.SATURDAY.intValue());
    }

    @Test
    public void testValueOf_0() {
        assertEquals(WeekDay.SUNDAY, WeekDay.valueOf(0));
    }

    @Test
    public void testValueOf_1() {
        assertEquals(WeekDay.MONDAY, WeekDay.valueOf(1));
    }

    @Test
    public void testValueOf_2() {
        assertEquals(WeekDay.TUESDAY, WeekDay.valueOf(2));
    }

    @Test
    public void testValueOf_3() {
        assertEquals(WeekDay.WEDNESDAY, WeekDay.valueOf(3));
    }

    @Test
    public void testValueOf_4() {
        assertEquals(WeekDay.THURSDAY, WeekDay.valueOf(4));
    }

    @Test
    public void testValueOf_5() {
        assertEquals(WeekDay.FRIDAY, WeekDay.valueOf(5));
    }

    @Test
    public void testValueOf_6() {
        assertEquals(WeekDay.SATURDAY, WeekDay.valueOf(6));
    }

    @Test
    public void testValueOf_invalid_negative() {
        assertThrows(IllegalArgumentException.class, () -> WeekDay.valueOf(-1));
    }

    @Test
    public void testValueOf_invalid_7() {
        assertThrows(IllegalArgumentException.class, () -> WeekDay.valueOf(7));
    }

    @Test
    public void testValueOf_invalid_100() {
        assertThrows(IllegalArgumentException.class, () -> WeekDay.valueOf(100));
    }

    @Test
    public void testValueOf_byName_SUNDAY() {
        assertEquals(WeekDay.SUNDAY, WeekDay.valueOf("SUNDAY"));
    }

    @Test
    public void testValueOf_byName_MONDAY() {
        assertEquals(WeekDay.MONDAY, WeekDay.valueOf("MONDAY"));
    }

    @Test
    public void testValueOf_byName_SATURDAY() {
        assertEquals(WeekDay.SATURDAY, WeekDay.valueOf("SATURDAY"));
    }

    @Test
    public void testValues() {
        WeekDay[] days = WeekDay.values();
        assertNotNull(days);
        assertEquals(7, days.length);
    }

    @Test
    public void testValues_order() {
        WeekDay[] days = WeekDay.values();
        assertEquals(WeekDay.SUNDAY, days[0]);
        assertEquals(WeekDay.MONDAY, days[1]);
        assertEquals(WeekDay.TUESDAY, days[2]);
        assertEquals(WeekDay.WEDNESDAY, days[3]);
        assertEquals(WeekDay.THURSDAY, days[4]);
        assertEquals(WeekDay.FRIDAY, days[5]);
        assertEquals(WeekDay.SATURDAY, days[6]);
    }

    @Test
    public void testIntValue_sequential() {
        WeekDay[] days = WeekDay.values();
        for (int i = 0; i < days.length; i++) {
            assertEquals(i, days[i].intValue());
        }
    }

    @Test
    public void testIntValue_uniqueness() {
        WeekDay[] days = WeekDay.values();
        for (int i = 0; i < days.length; i++) {
            for (int j = i + 1; j < days.length; j++) {
                if (days[i].intValue() == days[j].intValue()) {
                    throw new AssertionError("Duplicate int value: " + days[i] + " and " + days[j]);
                }
            }
        }
    }

    @Test
    public void testSwitchStatement() {
        WeekDay day = WeekDay.FRIDAY;
        boolean isWeekend = switch (day) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
        assertEquals(false, isWeekend);
    }

    @Test
    public void testSwitchStatement_weekend() {
        WeekDay day = WeekDay.SATURDAY;
        boolean isWeekend = switch (day) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
        assertEquals(true, isWeekend);
    }

    @Test
    public void testIntegration_allDaysRoundTrip() {
        WeekDay[] days = WeekDay.values();
        for (WeekDay day : days) {
            int value = day.intValue();
            WeekDay decoded = WeekDay.valueOf(value);
            assertEquals(day, decoded);
        }
    }

    @Test
    public void testIntegration_weekRange() {
        for (int i = 0; i < 7; i++) {
            WeekDay day = WeekDay.valueOf(i);
            assertNotNull(day);
            assertEquals(i, day.intValue());
        }
    }

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
