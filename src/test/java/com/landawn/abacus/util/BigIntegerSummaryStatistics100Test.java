package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BigIntegerSummaryStatistics100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        Assertions.assertEquals(0, stats.getCount());
        Assertions.assertEquals(BigInteger.ZERO, stats.getSum());
        Assertions.assertNull(stats.getMin());
        Assertions.assertNull(stats.getMax());
        Assertions.assertEquals(BigDecimal.ZERO, stats.getAverage());
    }

    @Test
    public void testParameterizedConstructor() {
        BigInteger min = new BigInteger("10");
        BigInteger max = new BigInteger("30");
        BigInteger sum = new BigInteger("60");
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics(3L, min, max, sum);

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(sum, stats.getSum());
        Assertions.assertEquals(min, stats.getMin());
        Assertions.assertEquals(max, stats.getMax());
        Assertions.assertEquals(0, new BigDecimal("20").compareTo(stats.getAverage().setScale(0)));
    }

    @Test
    public void testAccept() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        stats.accept(new BigInteger("100"));
        stats.accept(new BigInteger("200"));
        stats.accept(new BigInteger("300"));

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(new BigInteger("600"), stats.getSum());
        Assertions.assertEquals(new BigInteger("100"), stats.getMin());
        Assertions.assertEquals(new BigInteger("300"), stats.getMax());
    }

    @Test
    public void testCombine() {
        BigIntegerSummaryStatistics stats1 = new BigIntegerSummaryStatistics();
        stats1.accept(new BigInteger("1000"));
        stats1.accept(new BigInteger("2000"));

        BigIntegerSummaryStatistics stats2 = new BigIntegerSummaryStatistics();
        stats2.accept(new BigInteger("3000"));
        stats2.accept(new BigInteger("4000"));

        stats1.combine(stats2);

        Assertions.assertEquals(4, stats1.getCount());
        Assertions.assertEquals(new BigInteger("10000"), stats1.getSum());
        Assertions.assertEquals(new BigInteger("1000"), stats1.getMin());
        Assertions.assertEquals(new BigInteger("4000"), stats1.getMax());
    }

    @Test
    public void testGetAverage() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        stats.accept(new BigInteger("10"));
        stats.accept(new BigInteger("20"));
        stats.accept(new BigInteger("30"));

        BigDecimal average = stats.getAverage();
        Assertions.assertEquals(0, new BigDecimal("20").compareTo(average.setScale(0)));
    }

    @Test
    public void testToString() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        stats.accept(new BigInteger("100"));
        String str = stats.toString();
        Assertions.assertTrue(str.contains("min="));
        Assertions.assertTrue(str.contains("max="));
        Assertions.assertTrue(str.contains("count="));
        Assertions.assertTrue(str.contains("sum="));
        Assertions.assertTrue(str.contains("average="));
    }

    @Test
    public void testLargeNumbers() {
        BigIntegerSummaryStatistics stats = new BigIntegerSummaryStatistics();
        BigInteger large1 = new BigInteger("1000000000000000000");
        BigInteger large2 = new BigInteger("2000000000000000000");
        BigInteger large3 = new BigInteger("3000000000000000000");

        stats.accept(large1);
        stats.accept(large2);
        stats.accept(large3);

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(new BigInteger("6000000000000000000"), stats.getSum());
        Assertions.assertEquals(large1, stats.getMin());
        Assertions.assertEquals(large3, stats.getMax());
    }
}
