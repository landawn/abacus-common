package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class YesNoTest extends TestBase {

    @Test
    public void testIntValue_NO() {
        assertEquals(0, YesNo.NO.intValue());
    }

    @Test
    public void testIntValue_YES() {
        assertEquals(1, YesNo.YES.intValue());
    }

    @Test
    public void testIntValue_uniqueness() {
        YesNo[] values = YesNo.values();
        assertEquals(0, values[0].intValue());
        assertEquals(1, values[1].intValue());
    }

    @Test
    public void testIntValue() {
        Assertions.assertEquals(0, YesNo.NO.intValue());
        Assertions.assertEquals(1, YesNo.YES.intValue());
    }

    @Test
    public void testRoundTripConversion() {
        int noValue = YesNo.NO.intValue();
        YesNo noConverted = YesNo.valueOf(noValue);
        Assertions.assertEquals(YesNo.NO, noConverted);

        int yesValue = YesNo.YES.intValue();
        YesNo yesConverted = YesNo.valueOf(yesValue);
        Assertions.assertEquals(YesNo.YES, yesConverted);
    }

    @Test
    public void testUsageInDatabaseContext() {
        int dbValueForYes = YesNo.YES.intValue();
        int dbValueForNo = YesNo.NO.intValue();

        Assertions.assertEquals(1, dbValueForYes);
        Assertions.assertEquals(0, dbValueForNo);

        YesNo retrievedYes = YesNo.valueOf(dbValueForYes);
        YesNo retrievedNo = YesNo.valueOf(dbValueForNo);

        Assertions.assertEquals(YesNo.YES, retrievedYes);
        Assertions.assertEquals(YesNo.NO, retrievedNo);
    }

    @Test
    public void testValueOf_0() {
        assertEquals(YesNo.NO, YesNo.valueOf(0));
    }

    @Test
    public void testValueOf_1() {
        assertEquals(YesNo.YES, YesNo.valueOf(1));
    }

    @Test
    public void testValueOf_byName_NO() {
        assertEquals(YesNo.NO, YesNo.valueOf("NO"));
    }

    @Test
    public void testValueOf_byName_YES() {
        assertEquals(YesNo.YES, YesNo.valueOf("YES"));
    }

    @Test
    public void testValueOf_roundTrip_NO() {
        YesNo original = YesNo.NO;
        YesNo converted = YesNo.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf_roundTrip_YES() {
        YesNo original = YesNo.YES;
        YesNo converted = YesNo.valueOf(original.intValue());
        assertEquals(original, converted);
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(YesNo.NO, YesNo.valueOf(0));
        Assertions.assertEquals(YesNo.YES, YesNo.valueOf(1));

        YesNo no = YesNo.valueOf(0);
        Assertions.assertSame(YesNo.NO, no);

        YesNo yes = YesNo.valueOf(1);
        Assertions.assertSame(YesNo.YES, yes);
    }

    @Test
    public void testValueOf_invalid_negative() {
        assertThrows(IllegalArgumentException.class, () -> YesNo.valueOf(-1));
    }

    @Test
    public void testValueOf_invalid_2() {
        assertThrows(IllegalArgumentException.class, () -> YesNo.valueOf(2));
    }

    @Test
    public void testValueOf_invalid_100() {
        assertThrows(IllegalArgumentException.class, () -> YesNo.valueOf(100));
    }

    @Test
    public void testValueOfInvalidValue() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(-1);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(2);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(Integer.MAX_VALUE);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(Integer.MIN_VALUE);
        });
    }

    @Test
    public void testIntegration_booleanMapping() {
        boolean trueValue = true;
        YesNo yesValue = trueValue ? YesNo.YES : YesNo.NO;
        assertEquals(YesNo.YES, yesValue);
        assertEquals(1, yesValue.intValue());

        boolean falseValue = false;
        YesNo noValue = falseValue ? YesNo.YES : YesNo.NO;
        assertEquals(YesNo.NO, noValue);
        assertEquals(0, noValue.intValue());
    }

    @Test
    public void testIntegration_databaseSimulation() {
        int dbValue = 1;
        YesNo consent = YesNo.valueOf(dbValue);
        assertEquals(YesNo.YES, consent);

        int storedValue = consent.intValue();
        assertEquals(1, storedValue);
    }

    @Test
    public void testValues() {
        YesNo[] values = YesNo.values();
        assertNotNull(values);
        assertEquals(2, values.length);
    }

    @Test
    public void testValues_order() {
        YesNo[] values = YesNo.values();
        assertEquals(YesNo.NO, values[0]);
        assertEquals(YesNo.YES, values[1]);
    }

    @Test
    public void testSwitchStatement_NO() {
        YesNo answer = YesNo.NO;
        boolean result = switch (answer) {
            case YES -> true;
            case NO -> false;
        };
        assertEquals(false, result);
    }

    @Test
    public void testSwitchStatement_YES() {
        YesNo answer = YesNo.YES;
        boolean result = switch (answer) {
            case YES -> true;
            case NO -> false;
        };
        assertEquals(true, result);
    }

    @Test
    public void testEnumValues() {
        YesNo[] values = YesNo.values();
        Assertions.assertEquals(2, values.length);
        Assertions.assertEquals(YesNo.NO, values[0]);
        Assertions.assertEquals(YesNo.YES, values[1]);
    }

    @Test
    public void testEnumName() {
        Assertions.assertEquals("NO", YesNo.NO.name());
        Assertions.assertEquals("YES", YesNo.YES.name());
    }

    @Test
    public void testEnumOrdinal() {
        Assertions.assertEquals(0, YesNo.NO.ordinal());
        Assertions.assertEquals(1, YesNo.YES.ordinal());
    }

    @Test
    public void testComparison() {
        Assertions.assertTrue(YesNo.NO.compareTo(YesNo.YES) < 0);
        Assertions.assertTrue(YesNo.YES.compareTo(YesNo.NO) > 0);
        Assertions.assertEquals(0, YesNo.NO.compareTo(YesNo.NO));
        Assertions.assertEquals(0, YesNo.YES.compareTo(YesNo.YES));
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("NO", YesNo.NO.toString());
        Assertions.assertEquals("YES", YesNo.YES.toString());
    }

}
