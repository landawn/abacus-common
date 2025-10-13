package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ShortSummaryStatistics100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();

        Assertions.assertEquals(0, stats.getCount());
        Assertions.assertEquals(0L, stats.getSum());
        Assertions.assertEquals(Short.MAX_VALUE, stats.getMin());
        Assertions.assertEquals(Short.MIN_VALUE, stats.getMax());
        Assertions.assertEquals(0.0, stats.getAverage());
    }

    @Test
    public void testConstructorWithValues() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics(5, (short) 10, (short) 50, 150);

        Assertions.assertEquals(5, stats.getCount());
        Assertions.assertEquals(150L, stats.getSum());
        Assertions.assertEquals((short) 10, stats.getMin());
        Assertions.assertEquals((short) 50, stats.getMax());
        Assertions.assertEquals(30.0, stats.getAverage());
    }

    @Test
    public void testAccept() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();

        stats.accept((short) 10);
        stats.accept((short) 20);
        stats.accept((short) 30);

        Assertions.assertEquals(3, stats.getCount());
        Assertions.assertEquals(60L, stats.getSum());
        Assertions.assertEquals((short) 10, stats.getMin());
        Assertions.assertEquals((short) 30, stats.getMax());
        Assertions.assertEquals(20.0, stats.getAverage());
    }

    @Test
    public void testCombine() {
        ShortSummaryStatistics stats1 = new ShortSummaryStatistics();
        stats1.accept((short) 10);
        stats1.accept((short) 20);

        ShortSummaryStatistics stats2 = new ShortSummaryStatistics();
        stats2.accept((short) 30);
        stats2.accept((short) 40);

        stats1.combine(stats2);

        Assertions.assertEquals(4, stats1.getCount());
        Assertions.assertEquals(100L, stats1.getSum());
        Assertions.assertEquals((short) 10, stats1.getMin());
        Assertions.assertEquals((short) 40, stats1.getMax());
        Assertions.assertEquals(25.0, stats1.getAverage());
    }

    @Test
    public void testGetMin() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();
        Assertions.assertEquals(Short.MAX_VALUE, stats.getMin());

        stats.accept((short) 50);
        stats.accept((short) 10);
        stats.accept((short) 30);

        Assertions.assertEquals((short) 10, stats.getMin());
    }

    @Test
    public void testGetMax() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();
        Assertions.assertEquals(Short.MIN_VALUE, stats.getMax());

        stats.accept((short) 10);
        stats.accept((short) 50);
        stats.accept((short) 30);

        Assertions.assertEquals((short) 50, stats.getMax());
    }

    @Test
    public void testGetCount() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();
        Assertions.assertEquals(0, stats.getCount());

        stats.accept((short) 1);
        Assertions.assertEquals(1, stats.getCount());

        stats.accept((short) 2);
        Assertions.assertEquals(2, stats.getCount());
    }

    @Test
    public void testGetSum() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();
        Assertions.assertEquals(0L, stats.getSum());

        stats.accept((short) 10);
        stats.accept((short) 20);
        stats.accept((short) 30);

        Assertions.assertEquals(60L, stats.getSum());
    }

    @Test
    public void testGetAverage() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();
        Assertions.assertEquals(0.0, stats.getAverage());

        stats.accept((short) 10);
        stats.accept((short) 20);
        stats.accept((short) 30);
        stats.accept((short) 40);

        Assertions.assertEquals(25.0, stats.getAverage());
    }

    @Test
    public void testToString() {
        ShortSummaryStatistics stats = new ShortSummaryStatistics();
        stats.accept((short) 10);
        stats.accept((short) 20);

        String str = stats.toString();
        Assertions.assertTrue(str.contains("min=10"));
        Assertions.assertTrue(str.contains("max=20"));
        Assertions.assertTrue(str.contains("count=2"));
        Assertions.assertTrue(str.contains("sum=30"));
        Assertions.assertTrue(str.contains("average=15.000000"));
    }
}
