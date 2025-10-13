package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ByteSummaryStatistics100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0, stats.getCount());
        assertEquals(Byte.MAX_VALUE, stats.getMin());
        assertEquals(Byte.MIN_VALUE, stats.getMax());
        assertEquals(0L, stats.getSum());
        assertEquals(0.0, stats.getAverage());
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
    public void testAcceptSingleValue() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 42);

        assertEquals(1, stats.getCount());
        assertEquals((byte) 42, stats.getMin());
        assertEquals((byte) 42, stats.getMax());
        assertEquals(42L, stats.getSum());
        assertEquals(42.0, stats.getAverage());
    }

    @Test
    public void testAcceptMultipleValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);
        stats.accept((byte) 30);

        assertEquals(3, stats.getCount());
        assertEquals((byte) 10, stats.getMin());
        assertEquals((byte) 30, stats.getMax());
        assertEquals(60L, stats.getSum());
        assertEquals(20.0, stats.getAverage());
    }

    @Test
    public void testAcceptNegativeValues() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) -10);
        stats.accept((byte) -20);
        stats.accept((byte) 30);

        assertEquals(3, stats.getCount());
        assertEquals((byte) -20, stats.getMin());
        assertEquals((byte) 30, stats.getMax());
        assertEquals(0L, stats.getSum());
        assertEquals(0.0, stats.getAverage());
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

        assertEquals(4, stats1.getCount());
        assertEquals((byte) 10, stats1.getMin());
        assertEquals((byte) 40, stats1.getMax());
        assertEquals(100L, stats1.getSum());
        assertEquals(25.0, stats1.getAverage());
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
    public void testGetAverageEmpty() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testToString() {
        ByteSummaryStatistics stats = new ByteSummaryStatistics();
        stats.accept((byte) 10);
        stats.accept((byte) 20);

        String result = stats.toString();
        assertTrue(result.contains("min=10"));
        assertTrue(result.contains("max=20"));
        assertTrue(result.contains("count=2"));
        assertTrue(result.contains("sum=30"));
        assertTrue(result.contains("average=15.000000"));
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
}
