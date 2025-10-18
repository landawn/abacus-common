package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class WeekDay2025Test extends TestBase {

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
}
