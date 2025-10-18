package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BigDecimalSummaryStatistics2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        assertEquals(0L, stats.getCount());
        assertEquals(BigDecimal.ZERO, stats.getSum());
        assertNull(stats.getMin());
        assertNull(stats.getMax());
        assertEquals(BigDecimal.ZERO, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        BigDecimal min = new BigDecimal("10.00");
        BigDecimal max = new BigDecimal("30.00");
        BigDecimal sum = new BigDecimal("60.00");
        long count = 3L;

        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics(count, min, max, sum);

        assertEquals(count, stats.getCount());
        assertEquals(min, stats.getMin());
        assertEquals(max, stats.getMax());
        assertEquals(sum, stats.getSum());
    }

    @Test
    public void testAcceptSingleValue() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        BigDecimal value = new BigDecimal("25.50");

        stats.accept(value);

        assertEquals(1L, stats.getCount());
        assertEquals(value, stats.getSum());
        assertEquals(value, stats.getMin());
        assertEquals(value, stats.getMax());
        assertEquals(value, stats.getAverage());
    }

    @Test
    public void testAcceptMultipleValues() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("10.00"));
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("30.00"));

        assertEquals(3L, stats.getCount());
        assertEquals(new BigDecimal("60.00"), stats.getSum());
        assertEquals(new BigDecimal("10.00"), stats.getMin());
        assertEquals(new BigDecimal("30.00"), stats.getMax());
    }

    @Test
    public void testAcceptUpdatesMin() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("10.00"));
        stats.accept(new BigDecimal("30.00"));

        assertEquals(new BigDecimal("10.00"), stats.getMin());
    }

    @Test
    public void testAcceptUpdatesMax() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("30.00"));
        stats.accept(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("30.00"), stats.getMax());
    }

    @Test
    public void testAcceptNegativeValues() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("-10.00"));
        stats.accept(new BigDecimal("-20.00"));
        stats.accept(new BigDecimal("-5.00"));

        assertEquals(3L, stats.getCount());
        assertEquals(new BigDecimal("-35.00"), stats.getSum());
        assertEquals(new BigDecimal("-20.00"), stats.getMin());
        assertEquals(new BigDecimal("-5.00"), stats.getMax());
    }

    @Test
    public void testAcceptMixedValues() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("-10.00"));
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("-5.00"));

        assertEquals(3L, stats.getCount());
        assertEquals(new BigDecimal("5.00"), stats.getSum());
        assertEquals(new BigDecimal("-10.00"), stats.getMin());
        assertEquals(new BigDecimal("20.00"), stats.getMax());
    }

    @Test
    public void testAcceptZero() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(BigDecimal.ZERO);

        assertEquals(1L, stats.getCount());
        assertEquals(BigDecimal.ZERO, stats.getSum());
        assertEquals(BigDecimal.ZERO, stats.getMin());
        assertEquals(BigDecimal.ZERO, stats.getMax());
        assertEquals(BigDecimal.ZERO, stats.getAverage());
    }

    @Test
    public void testCombine() {
        BigDecimalSummaryStatistics stats1 = new BigDecimalSummaryStatistics();
        stats1.accept(new BigDecimal("10.00"));
        stats1.accept(new BigDecimal("20.00"));

        BigDecimalSummaryStatistics stats2 = new BigDecimalSummaryStatistics();
        stats2.accept(new BigDecimal("30.00"));
        stats2.accept(new BigDecimal("40.00"));

        stats1.combine(stats2);

        assertEquals(4L, stats1.getCount());
        assertEquals(new BigDecimal("100.00"), stats1.getSum());
        assertEquals(new BigDecimal("10.00"), stats1.getMin());
        assertEquals(new BigDecimal("40.00"), stats1.getMax());
    }

    @Test
    public void testCombineWithEmpty() {
        BigDecimalSummaryStatistics stats1 = new BigDecimalSummaryStatistics();
        stats1.accept(new BigDecimal("10.00"));

        BigDecimalSummaryStatistics stats2 = new BigDecimalSummaryStatistics();

        stats1.combine(stats2);

        assertEquals(1L, stats1.getCount());
        assertEquals(new BigDecimal("10.00"), stats1.getSum());
    }

    @Test
    public void testCombineEmptyWithNonEmpty() {
        BigDecimalSummaryStatistics stats1 = new BigDecimalSummaryStatistics();
        BigDecimalSummaryStatistics stats2 = new BigDecimalSummaryStatistics();
        stats2.accept(new BigDecimal("10.00"));

        stats1.combine(stats2);

        assertEquals(1L, stats1.getCount());
        assertEquals(new BigDecimal("10.00"), stats1.getSum());
        assertEquals(new BigDecimal("10.00"), stats1.getMin());
        assertEquals(new BigDecimal("10.00"), stats1.getMax());
    }

    @Test
    public void testCombineBothEmpty() {
        BigDecimalSummaryStatistics stats1 = new BigDecimalSummaryStatistics();
        BigDecimalSummaryStatistics stats2 = new BigDecimalSummaryStatistics();

        stats1.combine(stats2);

        assertEquals(0L, stats1.getCount());
        assertEquals(BigDecimal.ZERO, stats1.getSum());
        assertNull(stats1.getMin());
        assertNull(stats1.getMax());
    }

    @Test
    public void testGetAverage() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("10.00"));
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("30.00"));

        BigDecimal average = stats.getAverage();
        assertEquals(0, new BigDecimal("20.00").compareTo(average));
    }

    @Test
    public void testGetAverageEmpty() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        assertEquals(BigDecimal.ZERO, stats.getAverage());
    }

    @Test
    public void testGetAverageSingleValue() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        BigDecimal value = new BigDecimal("42.50");
        stats.accept(value);

        assertEquals(0, value.compareTo(stats.getAverage()));
    }

    @Test
    public void testGetAveragePrecision() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("10.00"));
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("30.00"));

        BigDecimal average = stats.getAverage();
        assertNotNull(average);
        assertTrue(average.compareTo(new BigDecimal("19.99")) > 0);
        assertTrue(average.compareTo(new BigDecimal("20.01")) < 0);
    }

    @Test
    public void testGetMin() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("30.00"));
        stats.accept(new BigDecimal("10.00"));
        stats.accept(new BigDecimal("20.00"));

        assertEquals(new BigDecimal("10.00"), stats.getMin());
    }

    @Test
    public void testGetMinEmpty() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        assertNull(stats.getMin());
    }

    @Test
    public void testGetMax() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("30.00"));
        stats.accept(new BigDecimal("10.00"));

        assertEquals(new BigDecimal("30.00"), stats.getMax());
    }

    @Test
    public void testGetMaxEmpty() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        assertNull(stats.getMax());
    }

    @Test
    public void testGetCount() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        assertEquals(0L, stats.getCount());

        stats.accept(new BigDecimal("10.00"));
        assertEquals(1L, stats.getCount());

        stats.accept(new BigDecimal("20.00"));
        assertEquals(2L, stats.getCount());
    }

    @Test
    public void testGetSum() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        assertEquals(BigDecimal.ZERO, stats.getSum());

        stats.accept(new BigDecimal("10.00"));
        assertEquals(new BigDecimal("10.00"), stats.getSum());

        stats.accept(new BigDecimal("20.00"));
        assertEquals(new BigDecimal("30.00"), stats.getSum());
    }

    @Test
    public void testToString() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("10.00"));
        stats.accept(new BigDecimal("20.00"));
        stats.accept(new BigDecimal("30.00"));

        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("min="));
        assertTrue(str.contains("max="));
        assertTrue(str.contains("count="));
        assertTrue(str.contains("sum="));
        assertTrue(str.contains("average="));
    }

    @Test
    public void testToStringEmpty() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("count=0"));
    }

    @Test
    public void testLargeNumbers() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        BigDecimal large1 = new BigDecimal("999999999999999999.99");
        BigDecimal large2 = new BigDecimal("888888888888888888.88");

        stats.accept(large1);
        stats.accept(large2);

        assertEquals(2L, stats.getCount());
        assertEquals(large2, stats.getMin());
        assertEquals(large1, stats.getMax());
    }

    @Test
    public void testPrecisionRetained() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("1.111111111111111"));
        stats.accept(new BigDecimal("2.222222222222222"));
        stats.accept(new BigDecimal("3.333333333333333"));

        BigDecimal sum = stats.getSum();
        assertNotNull(sum);
        assertTrue(sum.scale() > 0);
    }

    @Test
    public void testSameValues() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        BigDecimal value = new BigDecimal("42.00");

        stats.accept(value);
        stats.accept(value);
        stats.accept(value);

        assertEquals(3L, stats.getCount());
        assertEquals(value, stats.getMin());
        assertEquals(value, stats.getMax());
        assertEquals(value, stats.getAverage());
    }

    @Test
    public void testVerySmallNumbers() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("0.000001"));
        stats.accept(new BigDecimal("0.000002"));
        stats.accept(new BigDecimal("0.000003"));

        assertEquals(3L, stats.getCount());
        assertTrue(stats.getSum().compareTo(BigDecimal.ZERO) > 0);
    }
}
