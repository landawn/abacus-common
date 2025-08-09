package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Percentage100Test extends TestBase {

    @Test
    public void testDoubleValue() {
        // Test various percentage double values
        Assertions.assertEquals(0.000001, Percentage._0_0001.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.00001, Percentage._0_001.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.0001, Percentage._0_01.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.001, Percentage._0_1.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.01, Percentage._1.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.05, Percentage._5.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.10, Percentage._10.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.50, Percentage._50.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.90, Percentage._90.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.95, Percentage._95.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.99, Percentage._99.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.999, Percentage._99_9.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.9999, Percentage._99_99.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.99999, Percentage._99_999.doubleValue(), 0.0000001);
        Assertions.assertEquals(0.999999, Percentage._99_9999.doubleValue(), 0.0000001);
    }

    @Test
    public void testToString() {
        // Test string representations
        Assertions.assertEquals("0.0001%", Percentage._0_0001.toString());
        Assertions.assertEquals("0.001%", Percentage._0_001.toString());
        Assertions.assertEquals("1%", Percentage._1.toString());
        Assertions.assertEquals("5%", Percentage._5.toString());
        Assertions.assertEquals("10%", Percentage._10.toString());
        Assertions.assertEquals("50%", Percentage._50.toString());
        Assertions.assertEquals("90%", Percentage._90.toString());
        Assertions.assertEquals("95%", Percentage._95.toString());
        Assertions.assertEquals("99%", Percentage._99.toString());
        Assertions.assertEquals("99.9%", Percentage._99_9.toString());
        Assertions.assertEquals("99.99%", Percentage._99_99.toString());
        Assertions.assertEquals("99.999%", Percentage._99_999.toString());
        Assertions.assertEquals("99.9999%", Percentage._99_9999.toString());
    }

    @Test
    public void testRange() {
        // Test range with start inclusive and end exclusive
        ImmutableSet<Percentage> range1 = Percentage.range(Percentage._10, Percentage._50);
        Assertions.assertTrue(range1.contains(Percentage._10));
        Assertions.assertTrue(range1.contains(Percentage._20));
        Assertions.assertTrue(range1.contains(Percentage._30));
        Assertions.assertTrue(range1.contains(Percentage._40));
        Assertions.assertFalse(range1.contains(Percentage._50));
        Assertions.assertFalse(range1.contains(Percentage._5));
        
        // Test range with very small percentages
        ImmutableSet<Percentage> range2 = Percentage.range(Percentage._0_0001, Percentage._0_1);
        Assertions.assertTrue(range2.contains(Percentage._0_0001));
        Assertions.assertTrue(range2.contains(Percentage._0_001));
        Assertions.assertTrue(range2.contains(Percentage._0_01));
        Assertions.assertFalse(range2.contains(Percentage._0_1));
        
        // Test range with high percentages
        ImmutableSet<Percentage> range3 = Percentage.range(Percentage._90, Percentage._99_9999);
        Assertions.assertTrue(range3.contains(Percentage._90));
        Assertions.assertTrue(range3.contains(Percentage._95));
        Assertions.assertTrue(range3.contains(Percentage._99));
        Assertions.assertTrue(range3.contains(Percentage._99_999));
        Assertions.assertFalse(range3.contains(Percentage._99_9999));
        
        // Test empty range
        ImmutableSet<Percentage> emptyRange = Percentage.range(Percentage._50, Percentage._50);
        Assertions.assertTrue(emptyRange.isEmpty());
        
        // Test single element range
        ImmutableSet<Percentage> singleRange = Percentage.range(Percentage._5, Percentage._6);
        Assertions.assertEquals(1, singleRange.size());
        Assertions.assertTrue(singleRange.contains(Percentage._5));
    }

    @Test
    public void testRangeWithStep() {
        // Test range with step
        ImmutableSet<Percentage> range1 = Percentage.range(Percentage._10, Percentage._50, Percentage._10);
        Assertions.assertTrue(range1.contains(Percentage._10));
        Assertions.assertTrue(range1.contains(Percentage._20));
        Assertions.assertTrue(range1.contains(Percentage._30));
        Assertions.assertTrue(range1.contains(Percentage._40));
        Assertions.assertFalse(range1.contains(Percentage._50));
        Assertions.assertFalse(range1.contains(Percentage._5));
        
        // Test with smaller step
        ImmutableSet<Percentage> range2 = Percentage.range(Percentage._1, Percentage._10, Percentage._1);
        Assertions.assertTrue(range2.contains(Percentage._1));
        Assertions.assertTrue(range2.contains(Percentage._2));
        Assertions.assertTrue(range2.contains(Percentage._5));
        Assertions.assertTrue(range2.contains(Percentage._9));
        Assertions.assertFalse(range2.contains(Percentage._10));
        
        // Test with step that doesn't align perfectly
        ImmutableSet<Percentage> range3 = Percentage.range(Percentage._5, Percentage._20, Percentage._5);
        Assertions.assertTrue(range3.contains(Percentage._5));
        Assertions.assertTrue(range3.contains(Percentage._10));
        Assertions.assertFalse(range3.contains(Percentage._20));
        
        // Test empty range
        ImmutableSet<Percentage> emptyRange = Percentage.range(Percentage._50, Percentage._50, Percentage._10);
        Assertions.assertTrue(emptyRange.isEmpty());
    }

    @Test
    public void testRangeClosed() {
        // Test closed range (both inclusive)
        ImmutableSet<Percentage> range1 = Percentage.rangeClosed(Percentage._10, Percentage._50);
        Assertions.assertTrue(range1.contains(Percentage._10));
        Assertions.assertTrue(range1.contains(Percentage._20));
        Assertions.assertTrue(range1.contains(Percentage._30));
        Assertions.assertTrue(range1.contains(Percentage._40));
        Assertions.assertTrue(range1.contains(Percentage._50));
        Assertions.assertFalse(range1.contains(Percentage._60));
        
        // Test single element closed range
        ImmutableSet<Percentage> singleRange = Percentage.rangeClosed(Percentage._95, Percentage._95);
        Assertions.assertEquals(1, singleRange.size());
        Assertions.assertTrue(singleRange.contains(Percentage._95));
        
        // Test range with high percentages
        ImmutableSet<Percentage> highRange = Percentage.rangeClosed(Percentage._99, Percentage._99_9999);
        Assertions.assertTrue(highRange.contains(Percentage._99));
        Assertions.assertTrue(highRange.contains(Percentage._99_9));
        Assertions.assertTrue(highRange.contains(Percentage._99_99));
        Assertions.assertTrue(highRange.contains(Percentage._99_999));
        Assertions.assertTrue(highRange.contains(Percentage._99_9999));
    }

    @Test
    public void testRangeClosedWithStep() {
        // Test closed range with step
        ImmutableSet<Percentage> range1 = Percentage.rangeClosed(Percentage._10, Percentage._50, Percentage._10);
        Assertions.assertTrue(range1.contains(Percentage._10));
        Assertions.assertTrue(range1.contains(Percentage._20));
        Assertions.assertTrue(range1.contains(Percentage._30));
        Assertions.assertTrue(range1.contains(Percentage._40));
        Assertions.assertTrue(range1.contains(Percentage._50));
        Assertions.assertFalse(range1.contains(Percentage._60));
        
        // Test with smaller increments
        ImmutableSet<Percentage> range2 = Percentage.rangeClosed(Percentage._90, Percentage._99, Percentage._1);
        Assertions.assertTrue(range2.contains(Percentage._90));
        Assertions.assertTrue(range2.contains(Percentage._91));
        Assertions.assertTrue(range2.contains(Percentage._95));
        Assertions.assertTrue(range2.contains(Percentage._99));
        
        // Test single element with step
        ImmutableSet<Percentage> singleRange = Percentage.rangeClosed(Percentage._5, Percentage._5, Percentage._1);
        Assertions.assertEquals(1, singleRange.size());
        Assertions.assertTrue(singleRange.contains(Percentage._5));
    }

    @Test
    public void testRangeCaching() {
        // Test that ranges are cached properly
        ImmutableSet<Percentage> range1a = Percentage.range(Percentage._10, Percentage._50);
        ImmutableSet<Percentage> range1b = Percentage.range(Percentage._10, Percentage._50);
        Assertions.assertSame(range1a, range1b, "Same ranges should return cached instance");
        
        ImmutableSet<Percentage> range2a = Percentage.range(Percentage._10, Percentage._50, Percentage._10);
        ImmutableSet<Percentage> range2b = Percentage.range(Percentage._10, Percentage._50, Percentage._10);
        Assertions.assertSame(range2a, range2b, "Same ranges with step should return cached instance");
        
        ImmutableSet<Percentage> range3a = Percentage.rangeClosed(Percentage._20, Percentage._80);
        ImmutableSet<Percentage> range3b = Percentage.rangeClosed(Percentage._20, Percentage._80);
        Assertions.assertSame(range3a, range3b, "Same closed ranges should return cached instance");
        
        ImmutableSet<Percentage> range4a = Percentage.rangeClosed(Percentage._20, Percentage._80, Percentage._10);
        ImmutableSet<Percentage> range4b = Percentage.rangeClosed(Percentage._20, Percentage._80, Percentage._10);
        Assertions.assertSame(range4a, range4b, "Same closed ranges with step should return cached instance");
    }

    @Test
    public void testEnumValues() {
        // Test that all enum values are properly defined
        Percentage[] values = Percentage.values();
        Assertions.assertTrue(values.length > 0, "Should have percentage values");
        
        // Verify ordering (values should be in ascending order)
        for (int i = 1; i < values.length; i++) {
            Assertions.assertTrue(values[i].doubleValue() > values[i-1].doubleValue(),
                "Percentages should be in ascending order");
        }
        
        // Test valueOf
        Assertions.assertEquals(Percentage._50, Percentage.valueOf("_50"));
        Assertions.assertEquals(Percentage._99_9999, Percentage.valueOf("_99_9999"));
    }
}