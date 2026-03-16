package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteSummaryStatisticsTest extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getCount());
        assertEquals(0L, stats.getSum());
        assertEquals(Byte.MAX_VALUE, stats.getMin());
        assertEquals(Byte.MIN_VALUE, stats.getMax());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics(3, (byte) 10, (byte) 30, 60);
        assertEquals(3L, stats.getCount());
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 30, stats.getMax());
        assertEquals(60L, stats.getSum());
    }

    @Test
    public void testAcceptSingleValue() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 25);

        assertEquals(1L, stats.getCount());
        assertEquals(25L, stats.getSum());
        assertEquals((byte) 25, stats.getMin());
        assertEquals((byte) 25, stats.getMax());
        assertEquals(25.0, stats.getAverage());
    }

    @Test
    public void testAcceptMultipleValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);
        stats.accept((byte) 30);

        assertEquals(3L, stats.getCount());
        assertEquals(60L, stats.getSum());
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 30, stats.getMax());
        assertEquals(20.0, stats.getAverage());
    }

    @Test
    public void testAcceptNegativeValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) -10);
        stats.accept((byte) -20);
        stats.accept((byte) -5);

        assertEquals(3L, stats.getCount());
        assertEquals(-35L, stats.getSum());
        assertEquals((byte) -20, stats.getMin());
        assertEquals((byte) -5, stats.getMax());
    }

    @Test
    public void testCombine() {
        ByteSummaryStatistics stats1 = new ByteSummaryStatistics();
        stats1.accept((byte) 10);
        stats1.accept((byte) 20);

        ByteSummaryStatistics stats2 = new ByteSummaryStatistics();
        stats2.accept((byte) 30);
        stats2.accept((byte) 40);

        stats1.combine(stats2);

        assertEquals(4L, stats1.getCount());
        assertEquals(100L, stats1.getSum());
        assertEquals((byte) 10, stats1.getMin());
        assertEquals((byte) 40, stats1.getMax());
    }

    @Test
    public void testGetMin() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 30);
        stats.accept((byte) 10);
        stats.accept((byte) 20);

        assertEquals((byte) 10, stats.getMin());
    }

    @Test
    public void testGetMax() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 20);
        stats.accept((byte) 30);
        stats.accept((byte) 10);

        assertEquals((byte) 30, stats.getMax());
    }

    @Test
    public void testGetCount() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getCount());

        stats.accept((byte) 10);
        assertEquals(1L, stats.getCount());

        stats.accept((byte) 20);
        assertEquals(2L, stats.getCount());
    }

    @Test
    public void testGetSum() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getSum());

        stats.accept((byte) 10);
        assertEquals(10L, stats.getSum());

        stats.accept((byte) 20);
        assertEquals(30L, stats.getSum());
    }

    @Test
    public void testGetAverage() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);
        stats.accept((byte) 30);

        assertEquals(20.0, stats.getAverage());
    }

    @Test
    public void testGetAverageEmpty() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testToString() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);

        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("min="));
        assertTrue(str.contains("max="));
        assertTrue(str.contains("count="));
        assertTrue(str.contains("sum="));
        assertTrue(str.contains("average="));
    }

    @Test
    public void testExtremeValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept(Byte.MIN_VALUE);
        stats.accept(Byte.MAX_VALUE);

        assertEquals(2L, stats.getCount());
        assertEquals(Byte.MIN_VALUE, stats.getMin());
        assertEquals(Byte.MAX_VALUE, stats.getMax());
    }

    @Test
    public void testParameterizedConstructor() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics(5, (byte) 10, (byte) 50, 150);
        assertEquals(5, stats.getCount());
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 50, stats.getMax());
        assertEquals(150L, stats.getSum());
        assertEquals(30.0, stats.getAverage());
    }

    @Test
    public void testCombineWithEmpty() {
        ByteSummaryStatistics stats1 = new ByteSummaryStatistics();
        stats1.accept((byte) 10);
        stats1.accept((byte) 20);

        ByteSummaryStatistics stats2 = new ByteSummaryStatistics();

        stats1.combine(stats2);

        assertEquals(2, stats1.getCount());
        assertEquals((byte) 10, stats1.getMin());
        assertEquals((byte) 20, stats1.getMax());
        assertEquals(30L, stats1.getSum());
        assertEquals(15.0, stats1.getAverage());
    }

    @Test
    public void testCombineEmptyWithNonEmpty() {
        ByteSummaryStatistics stats1 = new ByteSummaryStatistics();

        ByteSummaryStatistics stats2 = new ByteSummaryStatistics();
        stats2.accept((byte) 30);
        stats2.accept((byte) 40);

        stats1.combine(stats2);

        assertEquals(2, stats1.getCount());
        assertEquals((byte) 30, stats1.getMin());
        assertEquals((byte) 40, stats1.getMax());
        assertEquals(70L, stats1.getSum());
        assertEquals(35.0, stats1.getAverage());
    }

    @Test
    public void testGetMinWithNoValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(Byte.MAX_VALUE, stats.getMin());
    }

    @Test
    public void testGetMaxWithNoValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(Byte.MIN_VALUE, stats.getMax());
    }

    @Test
    public void testGetCountEmpty() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testGetSumEmpty() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0L, stats.getSum());
    }

    @Test
    public void testBoundaryValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept(Byte.MIN_VALUE);
        stats.accept(Byte.MAX_VALUE);

        assertEquals(2, stats.getCount());
        assertEquals(Byte.MIN_VALUE, stats.getMin());
        assertEquals(Byte.MAX_VALUE, stats.getMax());
        assertEquals(-1L, stats.getSum());
        assertEquals(-0.5, stats.getAverage());
    }

    @Test
    public void testAcceptZero() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 0);

        assertEquals(1L, stats.getCount());
        assertEquals(0L, stats.getSum());
        assertEquals((byte) 0, stats.getMin());
        assertEquals((byte) 0, stats.getMax());
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testAcceptSameValueMultipleTimes() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 5);
        stats.accept((byte) 5);
        stats.accept((byte) 5);

        assertEquals(3L, stats.getCount());
        assertEquals(15L, stats.getSum());
        assertEquals((byte) 5, stats.getMin());
        assertEquals((byte) 5, stats.getMax());
        assertEquals(5.0, stats.getAverage());
    }

    @Test
    public void testAcceptUpdatesMinAndMax() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();

        stats.accept((byte) 50);
        assertEquals((byte) 50, stats.getMin());
        assertEquals((byte) 50, stats.getMax());

        stats.accept((byte) 10);
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 50, stats.getMax());

        stats.accept((byte) 100);
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 100, stats.getMax());
    }

}
