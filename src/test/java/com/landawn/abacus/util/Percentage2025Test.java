package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Percentage2025Test extends TestBase {

    @Test
    public void testDoubleValue_50() {
        assertEquals(0.50, Percentage._50.doubleValue(), 0.0001);
    }

    @Test
    public void testDoubleValue_95() {
        assertEquals(0.95, Percentage._95.doubleValue(), 0.0001);
    }

    @Test
    public void testToString_50() {
        assertEquals("50%", Percentage._50.toString());
    }

    @Test
    public void testToString_95() {
        assertEquals("95%", Percentage._95.toString());
    }

    @Test
    public void testValues() {
        Percentage[] percentages = Percentage.values();
        assertNotNull(percentages);
        assertTrue(percentages.length > 0);
    }

    @Test
    public void testRange_inclusive() {
        ImmutableSet<Percentage> range = Percentage.range(Percentage._40, Percentage._60);
        assertNotNull(range);
        assertTrue(range.contains(Percentage._40));
        assertTrue(range.contains(Percentage._50));
        assertFalse(range.contains(Percentage._60));
    }

    @Test
    public void testRangeClosed_inclusive() {
        ImmutableSet<Percentage> range = Percentage.rangeClosed(Percentage._40, Percentage._60);
        assertNotNull(range);
        assertTrue(range.contains(Percentage._40));
        assertTrue(range.contains(Percentage._50));
        assertTrue(range.contains(Percentage._60));
    }

    @Test
    public void testRange_withStep() {
        ImmutableSet<Percentage> range = Percentage.range(Percentage._10, Percentage._50, Percentage._10);
        assertNotNull(range);
        assertTrue(range.contains(Percentage._10));
        assertTrue(range.contains(Percentage._20));
        assertTrue(range.contains(Percentage._30));
        assertTrue(range.contains(Percentage._40));
        assertFalse(range.contains(Percentage._50));
    }

    @Test
    public void testRangeClosed_withStep() {
        ImmutableSet<Percentage> range = Percentage.rangeClosed(Percentage._10, Percentage._50, Percentage._10);
        assertNotNull(range);
        assertTrue(range.contains(Percentage._10));
        assertTrue(range.contains(Percentage._20));
        assertTrue(range.contains(Percentage._30));
        assertTrue(range.contains(Percentage._40));
        assertTrue(range.contains(Percentage._50));
    }

    @Test
    public void testDoubleValue_smallPercentages() {
        assertEquals(0.000001, Percentage._0_0001.doubleValue(), 0.0000001);
        assertEquals(0.00001, Percentage._0_001.doubleValue(), 0.000001);
        assertEquals(0.0001, Percentage._0_01.doubleValue(), 0.00001);
        assertEquals(0.001, Percentage._0_1.doubleValue(), 0.0001);
    }

    @Test
    public void testDoubleValue_highPercentages() {
        assertEquals(0.99, Percentage._99.doubleValue(), 0.0001);
        assertEquals(0.999, Percentage._99_9.doubleValue(), 0.0001);
        assertEquals(0.9999, Percentage._99_99.doubleValue(), 0.00001);
        assertEquals(0.99999, Percentage._99_999.doubleValue(), 0.000001);
    }

    @Test
    public void testIntegration_calculation() {
        double total = 1000.0;
        double result = total * Percentage._10.doubleValue();
        assertEquals(100.0, result, 0.01);
    }
}
