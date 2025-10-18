package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Month2025Test extends TestBase {

    @Test
    public void testIntValue_JANUARY() {
        assertEquals(1, Month.JANUARY.intValue());
    }

    @Test
    public void testIntValue_FEBRUARY() {
        assertEquals(2, Month.FEBRUARY.intValue());
    }

    @Test
    public void testIntValue_MARCH() {
        assertEquals(3, Month.MARCH.intValue());
    }

    @Test
    public void testIntValue_APRIL() {
        assertEquals(4, Month.APRIL.intValue());
    }

    @Test
    public void testIntValue_MAY() {
        assertEquals(5, Month.MAY.intValue());
    }

    @Test
    public void testIntValue_JUNE() {
        assertEquals(6, Month.JUNE.intValue());
    }

    @Test
    public void testIntValue_JULY() {
        assertEquals(7, Month.JULY.intValue());
    }

    @Test
    public void testIntValue_AUGUST() {
        assertEquals(8, Month.AUGUST.intValue());
    }

    @Test
    public void testIntValue_SEPTEMBER() {
        assertEquals(9, Month.SEPTEMBER.intValue());
    }

    @Test
    public void testIntValue_OCTOBER() {
        assertEquals(10, Month.OCTOBER.intValue());
    }

    @Test
    public void testIntValue_NOVEMBER() {
        assertEquals(11, Month.NOVEMBER.intValue());
    }

    @Test
    public void testIntValue_DECEMBER() {
        assertEquals(12, Month.DECEMBER.intValue());
    }

    @Test
    public void testValueOf_1() {
        Month month = Month.valueOf(1);
        assertEquals(Month.JANUARY, month);
    }

    @Test
    public void testValueOf_2() {
        Month month = Month.valueOf(2);
        assertEquals(Month.FEBRUARY, month);
    }

    @Test
    public void testValueOf_3() {
        Month month = Month.valueOf(3);
        assertEquals(Month.MARCH, month);
    }

    @Test
    public void testValueOf_4() {
        Month month = Month.valueOf(4);
        assertEquals(Month.APRIL, month);
    }

    @Test
    public void testValueOf_5() {
        Month month = Month.valueOf(5);
        assertEquals(Month.MAY, month);
    }

    @Test
    public void testValueOf_6() {
        Month month = Month.valueOf(6);
        assertEquals(Month.JUNE, month);
    }

    @Test
    public void testValueOf_7() {
        Month month = Month.valueOf(7);
        assertEquals(Month.JULY, month);
    }

    @Test
    public void testValueOf_8() {
        Month month = Month.valueOf(8);
        assertEquals(Month.AUGUST, month);
    }

    @Test
    public void testValueOf_9() {
        Month month = Month.valueOf(9);
        assertEquals(Month.SEPTEMBER, month);
    }

    @Test
    public void testValueOf_10() {
        Month month = Month.valueOf(10);
        assertEquals(Month.OCTOBER, month);
    }

    @Test
    public void testValueOf_11() {
        Month month = Month.valueOf(11);
        assertEquals(Month.NOVEMBER, month);
    }

    @Test
    public void testValueOf_12() {
        Month month = Month.valueOf(12);
        assertEquals(Month.DECEMBER, month);
    }

    @Test
    public void testValueOf_invalid_0() {
        assertThrows(IllegalArgumentException.class, () -> Month.valueOf(0));
    }

    @Test
    public void testValueOf_invalid_13() {
        assertThrows(IllegalArgumentException.class, () -> Month.valueOf(13));
    }

    @Test
    public void testValueOf_invalid_negative() {
        assertThrows(IllegalArgumentException.class, () -> Month.valueOf(-1));
    }

    @Test
    public void testValueOf_invalid_100() {
        assertThrows(IllegalArgumentException.class, () -> Month.valueOf(100));
    }

    @Test
    public void testValueOf_roundTrip_JANUARY() {
        Month original = Month.JANUARY;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_FEBRUARY() {
        Month original = Month.FEBRUARY;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_MARCH() {
        Month original = Month.MARCH;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_APRIL() {
        Month original = Month.APRIL;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_MAY() {
        Month original = Month.MAY;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_JUNE() {
        Month original = Month.JUNE;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_JULY() {
        Month original = Month.JULY;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_AUGUST() {
        Month original = Month.AUGUST;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_SEPTEMBER() {
        Month original = Month.SEPTEMBER;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_OCTOBER() {
        Month original = Month.OCTOBER;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_NOVEMBER() {
        Month original = Month.NOVEMBER;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_DECEMBER() {
        Month original = Month.DECEMBER;
        Month converted = Month.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_byName_JANUARY() {
        Month month = Month.valueOf("JANUARY");
        assertEquals(Month.JANUARY, month);
    }

    @Test
    public void testValueOf_byName_DECEMBER() {
        Month month = Month.valueOf("DECEMBER");
        assertEquals(Month.DECEMBER, month);
    }

    @Test
    public void testValues() {
        Month[] months = Month.values();
        assertNotNull(months);
        assertEquals(12, months.length);
    }

    @Test
    public void testValues_order() {
        Month[] months = Month.values();
        assertEquals(Month.JANUARY, months[0]);
        assertEquals(Month.FEBRUARY, months[1]);
        assertEquals(Month.MARCH, months[2]);
        assertEquals(Month.APRIL, months[3]);
        assertEquals(Month.MAY, months[4]);
        assertEquals(Month.JUNE, months[5]);
        assertEquals(Month.JULY, months[6]);
        assertEquals(Month.AUGUST, months[7]);
        assertEquals(Month.SEPTEMBER, months[8]);
        assertEquals(Month.OCTOBER, months[9]);
        assertEquals(Month.NOVEMBER, months[10]);
        assertEquals(Month.DECEMBER, months[11]);
    }

    @Test
    public void testIntValue_sequential() {
        Month[] months = Month.values();
        for (int i = 0; i < months.length; i++) {
            assertEquals(i + 1, months[i].intValue());
        }
    }

    @Test
    public void testIntValue_uniqueness() {
        Month[] months = Month.values();
        for (int i = 0; i < months.length; i++) {
            for (int j = i + 1; j < months.length; j++) {
                if (months[i].intValue() == months[j].intValue()) {
                    throw new AssertionError("Duplicate int value: " + months[i] + " and " + months[j]);
                }
            }
        }
    }

    @Test
    public void testSwitchStatement() {
        Month month = Month.JUNE;
        String season = switch (month) {
            case DECEMBER, JANUARY, FEBRUARY -> "Winter";
            case MARCH, APRIL, MAY -> "Spring";
            case JUNE, JULY, AUGUST -> "Summer";
            case SEPTEMBER, OCTOBER, NOVEMBER -> "Fall";
        };
        assertEquals("Summer", season);
    }

    @Test
    public void testIntegration_allMonthsRoundTrip() {
        Month[] months = Month.values();
        for (Month month : months) {
            int value = month.intValue();
            Month decoded = Month.valueOf(value);
            assertEquals(month, decoded);
        }
    }

    @Test
    public void testIntegration_monthRange() {
        for (int i = 1; i <= 12; i++) {
            Month month = Month.valueOf(i);
            assertNotNull(month);
            assertEquals(i, month.intValue());
        }
    }
}
