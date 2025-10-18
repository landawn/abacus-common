package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BigIntegerSummaryStatistics2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        assertEquals(0L, stats.getCount());
        assertEquals(BigInteger.ZERO, stats.getSum());
        assertNull(stats.getMin());
        assertNull(stats.getMax());
        assertEquals(BigDecimal.ZERO, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        BigInteger min = new BigInteger("10");
        BigInteger max = new BigInteger("30");
        BigInteger sum = new BigInteger("60");
        long count = 3L;

        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics(count, min, max, sum);

        assertEquals(count, stats.getCount());
        assertEquals(min, stats.getMin());
        assertEquals(max, stats.getMax());
        assertEquals(sum, stats.getSum());
    }

    @Test
    public void testAcceptSingleValue() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        BigInteger value = new BigInteger("25");

        stats.accept(value);

        assertEquals(1L, stats.getCount());
        assertEquals(value, stats.getSum());
        assertEquals(value, stats.getMin());
        assertEquals(value, stats.getMax());
    }

    @Test
    public void testAcceptMultipleValues() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        stats.accept(new BigInteger("10"));
        stats.accept(new BigInteger("20"));
        stats.accept(new BigInteger("30"));

        assertEquals(3L, stats.getCount());
        assertEquals(new BigInteger("60"), stats.getSum());
        assertEquals(new BigInteger("10"), stats.getMin());
        assertEquals(new BigInteger("30"), stats.getMax());
    }

    @Test
    public void testAcceptNegativeValues() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        stats.accept(new BigInteger("-10"));
        stats.accept(new BigInteger("-20"));
        stats.accept(new BigInteger("-5"));

        assertEquals(3L, stats.getCount());
        assertEquals(new BigInteger("-35"), stats.getSum());
        assertEquals(new BigInteger("-20"), stats.getMin());
        assertEquals(new BigInteger("-5"), stats.getMax());
    }

    @Test
    public void testCombine() {
        BigIntegerSummaryStatistics stats1 = new BigIntegerSummaryStatistics();
        stats1.accept(new BigInteger("10"));
        stats1.accept(new BigInteger("20"));

        BigIntegerSummaryStatistics stats2 = new BigIntegerSummaryStatistics();
        stats2.accept(new BigInteger("30"));
        stats2.accept(new BigInteger("40"));

        stats1.combine(stats2);

        assertEquals(4L, stats1.getCount());
        assertEquals(new BigInteger("100"), stats1.getSum());
        assertEquals(new BigInteger("10"), stats1.getMin());
        assertEquals(new BigInteger("40"), stats1.getMax());
    }

    @Test
    public void testGetAverage() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        stats.accept(new BigInteger("10"));
        stats.accept(new BigInteger("20"));
        stats.accept(new BigInteger("30"));

        BigDecimal average = stats.getAverage();
        assertEquals(0, new BigDecimal("20").compareTo(average));
    }

    @Test
    public void testGetAverageEmpty() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        assertEquals(BigDecimal.ZERO, stats.getAverage());
    }

    @Test
    public void testToString() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        stats.accept(new BigInteger("10"));
        stats.accept(new BigInteger("20"));

        String str = stats.toString();
        assertNotNull(str);
        assertTrue(str.contains("min="));
        assertTrue(str.contains("max="));
        assertTrue(str.contains("count="));
        assertTrue(str.contains("sum="));
        assertTrue(str.contains("average="));
    }

    @Test
    public void testLargeNumbers() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        BigInteger large1 = new BigInteger("999999999999999999999999");
        BigInteger large2 = new BigInteger("888888888888888888888888");

        stats.accept(large1);
        stats.accept(large2);

        assertEquals(2L, stats.getCount());
        assertEquals(large2, stats.getMin());
        assertEquals(large1, stats.getMax());
    }
}
