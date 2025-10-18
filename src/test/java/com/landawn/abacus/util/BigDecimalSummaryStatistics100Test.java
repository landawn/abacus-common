package com.landawn.abacus.util;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BigDecimalSummaryStatistics100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        Assertions.assertEquals(0, stats.getCount());
        Assertions.assertEquals(BigDecimal.ZERO, stats.getSum());
        Assertions.assertNull(stats.getMin());
        Assertions.assertNull(stats.getMax());
        Assertions.assertEquals(BigDecimal.ZERO, stats.getAverage());
    }

    @Test
    public void testParameterizedConstructor() {
        BigDecimal min = new BigDecimal("10.00");
        BigDecimal max = new BigDecimal("30.00");
        BigDecimal sum = new BigDecimal("60.00");
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics(3L, min, max, sum);

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(sum, stats.getSum());
        Assertions.assertEquals(min, stats.getMin());
        Assertions.assertEquals(max, stats.getMax());
        Assertions.assertEquals(new BigDecimal("20"), stats.getAverage().setScale(0));
    }

    @Test
    public void testAccept() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("10.50"));
        stats.accept(new BigDecimal("20.75"));
        stats.accept(new BigDecimal("15.25"));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(new BigDecimal("46.50"), stats.getSum());
        Assertions.assertEquals(new BigDecimal("10.50"), stats.getMin());
        Assertions.assertEquals(new BigDecimal("20.75"), stats.getMax());
    }

    @Test
    public void testCombine() {
        BigDecimalSummaryStatistics stats1 = new BigDecimalSummaryStatistics();
        stats1.accept(new BigDecimal("10"));
        stats1.accept(new BigDecimal("20"));

        BigDecimalSummaryStatistics stats2 = new BigDecimalSummaryStatistics();
        stats2.accept(new BigDecimal("30"));
        stats2.accept(new BigDecimal("40"));

        stats1.combine(stats2);

        Assertions.assertEquals(4, stats1.getCount());
        Assertions.assertEquals(new BigDecimal("100"), stats1.getSum());
        Assertions.assertEquals(new BigDecimal("10"), stats1.getMin());
        Assertions.assertEquals(new BigDecimal("40"), stats1.getMax());
    }

    @Test
    public void testGetAverage() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("10"));
        stats.accept(new BigDecimal("20"));
        stats.accept(new BigDecimal("30"));

        BigDecimal average = stats.getAverage();
        Assertions.assertEquals(0, new BigDecimal("20").compareTo(average.setScale(0)));
    }

    @Test
    public void testToString() {
        BigDecimalSummaryStatistics stats = new BigDecimalSummaryStatistics();
        stats.accept(new BigDecimal("10.5"));
        String str = stats.toString();
        Assertions.assertTrue(str.contains("min="));
        Assertions.assertTrue(str.contains("max="));
        Assertions.assertTrue(str.contains("count="));
        Assertions.assertTrue(str.contains("sum="));
        Assertions.assertTrue(str.contains("average="));
    }
}
