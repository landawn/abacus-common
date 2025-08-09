package com.landawn.abacus.util;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class FloatSummaryStatistics100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();

        Assertions.assertEquals(0, stats.getCount());
        Assertions.assertEquals(0.0, stats.getSum());
        Assertions.assertEquals(Float.POSITIVE_INFINITY, stats.getMin());
        Assertions.assertEquals(Float.NEGATIVE_INFINITY, stats.getMax());
        Assertions.assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics(5, 1.0f, 10.0f, 30.0);

        Assertions.assertEquals(5, stats.getCount());
        Assertions.assertEquals(30.0, stats.getSum());
        Assertions.assertEquals(1.0f, stats.getMin());
        Assertions.assertEquals(10.0f, stats.getMax());
        Assertions.assertEquals(6.0, stats.getAverage());
    }

    @Test
    public void testAccept() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();

        stats.accept(1.0f);
        stats.accept(2.0f);
        stats.accept(3.0f);

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(6.0, stats.getSum(), 0.0001);
        Assertions.assertEquals(1.0f, stats.getMin());
        Assertions.assertEquals(3.0f, stats.getMax());
        Assertions.assertEquals(2.0, stats.getAverage(), 0.0001);
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

        Assertions.assertEquals(4, stats1.getCount());
        Assertions.assertEquals(10.0, stats1.getSum(), 0.0001);
        Assertions.assertEquals(1.0f, stats1.getMin());
        Assertions.assertEquals(4.0f, stats1.getMax());
        Assertions.assertEquals(2.5, stats1.getAverage(), 0.0001);
    }

    @Test
    public void testGetMin() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        Assertions.assertEquals(Float.POSITIVE_INFINITY, stats.getMin());

        stats.accept(5.0f);
        stats.accept(1.0f);
        stats.accept(3.0f);

        Assertions.assertEquals(1.0f, stats.getMin());
    }

    @Test
    public void testGetMax() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        Assertions.assertEquals(Float.NEGATIVE_INFINITY, stats.getMax());

        stats.accept(1.0f);
        stats.accept(5.0f);
        stats.accept(3.0f);

        Assertions.assertEquals(5.0f, stats.getMax());
    }

    @Test
    public void testGetCount() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        Assertions.assertEquals(0, stats.getCount());

        stats.accept(1.0f);
        Assertions.assertEquals(1, stats.getCount());

        stats.accept(2.0f);
        Assertions.assertEquals(2, stats.getCount());
    }

    @Test
    public void testGetSum() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        Assertions.assertEquals(0.0, stats.getSum());

        stats.accept(1.1f);
        stats.accept(2.2f);
        stats.accept(3.3f);

        Assertions.assertEquals(6.6, stats.getSum(), 0.0001);
    }

    @Test
    public void testGetAverage() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        Assertions.assertEquals(0.0, stats.getAverage());

        stats.accept(2.0f);
        stats.accept(4.0f);
        stats.accept(6.0f);

        Assertions.assertEquals(4.0, stats.getAverage(), 0.0001);
    }

    @Test
    public void testToString() {
        FloatSummaryStatistics stats = new FloatSummaryStatistics();
        stats.accept(1.0f);
        stats.accept(2.0f);

        String str = stats.toString();
        Assertions.assertTrue(str.contains("min=1.000000"));
        Assertions.assertTrue(str.contains("max=2.000000"));
        Assertions.assertTrue(str.contains("count=2"));
        Assertions.assertTrue(str.contains("sum=3.000000"));
        Assertions.assertTrue(str.contains("average=1.500000"));
    }
}