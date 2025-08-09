package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class YesNo100Test extends TestBase {

    @Test
    public void testIntValue() {
        Assertions.assertEquals(0, YesNo.NO.intValue());
        Assertions.assertEquals(1, YesNo.YES.intValue());
    }

    @Test
    public void testValueOf() {
        // Test valid values
        Assertions.assertEquals(YesNo.NO, YesNo.valueOf(0));
        Assertions.assertEquals(YesNo.YES, YesNo.valueOf(1));
        
        // Test edge cases
        YesNo no = YesNo.valueOf(0);
        Assertions.assertSame(YesNo.NO, no);
        
        YesNo yes = YesNo.valueOf(1);
        Assertions.assertSame(YesNo.YES, yes);
    }

    @Test
    public void testValueOfInvalidValue() {
        // Test negative value
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(-1);
        });
        
        // Test value > 1
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(2);
        });
        
        // Test large values
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(Integer.MAX_VALUE);
        });
        
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YesNo.valueOf(Integer.MIN_VALUE);
        });
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
    public void testRoundTripConversion() {
        // Test NO
        int noValue = YesNo.NO.intValue();
        YesNo noConverted = YesNo.valueOf(noValue);
        Assertions.assertEquals(YesNo.NO, noConverted);
        
        // Test YES
        int yesValue = YesNo.YES.intValue();
        YesNo yesConverted = YesNo.valueOf(yesValue);
        Assertions.assertEquals(YesNo.YES, yesConverted);
    }

    @Test
    public void testUsageInDatabaseContext() {
        // Simulate database storage and retrieval
        int dbValueForYes = YesNo.YES.intValue();
        int dbValueForNo = YesNo.NO.intValue();
        
        // Store to "database"
        Assertions.assertEquals(1, dbValueForYes);
        Assertions.assertEquals(0, dbValueForNo);
        
        // Retrieve from "database"
        YesNo retrievedYes = YesNo.valueOf(dbValueForYes);
        YesNo retrievedNo = YesNo.valueOf(dbValueForNo);
        
        Assertions.assertEquals(YesNo.YES, retrievedYes);
        Assertions.assertEquals(YesNo.NO, retrievedNo);
    }

    @Test
    public void testComparison() {
        // Test that enum comparison works as expected
        Assertions.assertTrue(YesNo.NO.compareTo(YesNo.YES) < 0);
        Assertions.assertTrue(YesNo.YES.compareTo(YesNo.NO) > 0);
        Assertions.assertEquals(0, YesNo.NO.compareTo(YesNo.NO));
        Assertions.assertEquals(0, YesNo.YES.compareTo(YesNo.YES));
    }

    @Test
    public void testToString() {
        // Default enum toString returns the name
        Assertions.assertEquals("NO", YesNo.NO.toString());
        Assertions.assertEquals("YES", YesNo.YES.toString());
    }
}