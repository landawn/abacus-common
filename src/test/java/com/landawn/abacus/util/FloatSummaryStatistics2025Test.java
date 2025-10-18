package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class FloatSummaryStatistics2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        assertEquals(0L, stats.getCount());
        assertEquals(0.0, stats.getSum());
        assertEquals(Float.POSITIVE_INFINITY, stats.getMin());
        assertEquals(Float.NEGATIVE_INFINITY, stats.getMax());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics(3, 1.0f, 10.0f, 15.0);
        assertEquals(3L, stats.getCount());
        assertEquals(1.0f, stats.getMin());
        assertEquals(10.0f, stats.getMax());
        assertEquals(15.0, stats.getSum(), 0.0001);
    }

    @Test
    public void testAcceptSingleValue() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(5.5f);

        assertEquals(1L, stats.getCount());
        assertEquals(5.5, stats.getSum(), 0.0001);
        assertEquals(5.5f, stats.getMin());
        assertEquals(5.5f, stats.getMax());
        assertEquals(5.5, stats.getAverage(), 0.0001);
    }

    @Test
    public void testAcceptMultipleValues() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(1.0f);
        stats.accept(2.0f);
        stats.accept(3.0f);

        assertEquals(3L, stats.getCount());
        assertEquals(6.0, stats.getSum(), 0.0001);
        assertEquals(1.0f, stats.getMin());
        assertEquals(3.0f, stats.getMax());
        assertEquals(2.0, stats.getAverage(), 0.0001);
    }

    @Test
    public void testAcceptNegativeValues() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(-1.0f);
        stats.accept(-2.0f);
        stats.accept(-3.0f);

        assertEquals(3L, stats.getCount());
        assertEquals(-6.0, stats.getSum(), 0.0001);
        assertEquals(-3.0f, stats.getMin());
        assertEquals(-1.0f, stats.getMax());
    }

    @Test
    public void testCombine() {
        FloatSummaryStatistics stats1 = new FloatSummaryStatistics();
        stats1.accept(1.0f);
        stats1.accept(2.0f);

        FloatSummaryStatistics stats2 = new FloatSummaryStatistics();
        stats2.accept(3.0f);
        stats2.accept(4.0f);

        stats1.combine(stats2);

        assertEquals(4L, stats1.getCount());
        assertEquals(10.0, stats1.getSum(), 0.0001);
        assertEquals(1.0f, stats1.getMin());
        assertEquals(4.0f, stats1.getMax());
    }

    @Test
    public void testGetMin() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(5.0f);
        stats.accept(2.0f);
        stats.accept(8.0f);

        assertEquals(2.0f, stats.getMin());
    }

    @Test
    public void testGetMax() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(5.0f);
        stats.accept(2.0f);
        stats.accept(8.0f);

        assertEquals(8.0f, stats.getMax());
    }

    @Test
    public void testGetAverage() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(2.0f);
        stats.accept(4.0f);
        stats.accept(6.0f);

        assertEquals(4.0, stats.getAverage(), 0.0001);
    }

    @Test
    public void testGetAverageEmpty() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testToString() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(1.0f);
        stats.accept(2.0f);

        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("min="));
        assertTrue(str.contains("max="));
        assertTrue(str.contains("count="));
        assertTrue(str.contains("sum="));
        assertTrue(str.contains("average="));
    }

    @Test
    public void testVerySmallNumbers() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(0.0001f);
        stats.accept(0.0002f);
        stats.accept(0.0003f);

        assertEquals(3L, stats.getCount());
        assertTrue(stats.getSum() > 0);
    }

    @Test
    public void testZeroValue() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(0.0f);

        assertEquals(1L, stats.getCount());
        assertEquals(0.0, stats.getSum());
        assertEquals(0.0f, stats.getMin());
        assertEquals(0.0f, stats.getMax());
    }
}
