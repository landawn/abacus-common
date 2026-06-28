package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class KahanSummationTest extends TestBase {

    @Test
    public void testOf() {
        KahanSummation sum = KahanSummation.of(1.0, 2.0, 3.0, 4.0);

        Assertions.assertEquals(4, sum.count());
        Assertions.assertEquals(10.0, sum.sum(), 0.0001);
    }

    @Test
    public void testAdd() {
        KahanSummation sum = new KahanSummation();

        sum.add(1.0);
        sum.add(2.0);
        sum.add(3.0);

        Assertions.assertEquals(3, sum.count());
        Assertions.assertEquals(6.0, sum.sum(), 0.0001);
    }

    @Test
    public void testAddAll() {
        KahanSummation sum = new KahanSummation();
        double[] values = { 1.0, 2.0, 3.0, 4.0, 5.0 };

        sum.addAll(values);

        Assertions.assertEquals(5, sum.count());
        Assertions.assertEquals(15.0, sum.sum(), 0.0001);
    }

    @Test
    public void testCombineWithCountAndSum() {
        KahanSummation sum = new KahanSummation();
        sum.add(1.0);
        sum.add(2.0);

        sum.combine(3, 10.0);

        Assertions.assertEquals(5, sum.count());
        Assertions.assertEquals(13.0, sum.sum(), 0.0001);
    }

    @Test
    public void testCombineRejectsNegativeCount() {
        KahanSummation sum = new KahanSummation();

        Assertions.assertThrows(IllegalArgumentException.class, () -> sum.combine(-1, 5.0));
        Assertions.assertEquals(0, sum.count());
        Assertions.assertEquals(0.0, sum.sum(), 0.0001);
    }

    @Test
    public void testCombineWithOtherSummation() {
        KahanSummation sum1 = new KahanSummation();
        sum1.add(1.0);
        sum1.add(2.0);

        KahanSummation sum2 = new KahanSummation();
        sum2.add(3.0);
        sum2.add(4.0);

        sum1.combine(sum2);

        Assertions.assertEquals(4, sum1.count());
        Assertions.assertEquals(10.0, sum1.sum(), 0.0001);
    }

    @Test
    public void testCount() {
        KahanSummation sum = new KahanSummation();
        Assertions.assertEquals(0, sum.count());

        sum.add(1.0);
        Assertions.assertEquals(1, sum.count());

        sum.add(2.0);
        Assertions.assertEquals(2, sum.count());
    }

    @Test
    public void testSum() {
        KahanSummation sum = new KahanSummation();
        sum.add(0.1);
        sum.add(0.2);
        sum.add(0.3);

        Assertions.assertEquals(0.6, sum.sum(), 1e-15);
    }

    @Test
    public void testSumWithNaNAndInfinity() {
        KahanSummation sum = new KahanSummation();
        sum.add(Double.POSITIVE_INFINITY);
        sum.add(1.0);

        Assertions.assertEquals(Double.POSITIVE_INFINITY, sum.sum());
    }

    @Test
    public void testSumCompensationSign() {
        // True sum is 1.0 + 2^-53, whose correctly-rounded double value is 1.0.
        // With the wrong compensation sign (sum + correction, the pre-JDK-8214761
        // bug), this returns 0.9999999999999999 - worse than naive summation.
        final double halfUlp = Math.ulp(1.0) / 2; // 2^-53
        final KahanSummation sum = KahanSummation.of(1.0, halfUlp);

        Assertions.assertEquals(1.0, sum.sum());
    }

    @Test
    public void testCombineCompensationSign() {
        // other holds sum=1.0 with correction=-2^-53 (true partial sum 1.0 + 2^-53).
        // Combining must subtract the compensation bits (JDK-8214761); adding them
        // instead yields 0.9999999999999999.
        final double halfUlp = Math.ulp(1.0) / 2; // 2^-53
        final KahanSummation other = KahanSummation.of(1.0, halfUlp);

        final KahanSummation sum = new KahanSummation();
        sum.combine(other);

        Assertions.assertEquals(2, sum.count());
        Assertions.assertEquals(1.0, sum.sum());

        // Matches java.util.DoubleSummaryStatistics on the same input.
        final java.util.DoubleSummaryStatistics jdkOther = new java.util.DoubleSummaryStatistics();
        jdkOther.accept(1.0);
        jdkOther.accept(halfUlp);
        final java.util.DoubleSummaryStatistics jdk = new java.util.DoubleSummaryStatistics();
        jdk.combine(jdkOther);
        Assertions.assertEquals(jdk.getSum(), sum.sum());
    }

    @Test
    public void testAverage() {
        KahanSummation sum = new KahanSummation();
        sum.add(1.0);
        sum.add(2.0);
        sum.add(3.0);

        Assertions.assertTrue(sum.average().isPresent());
        Assertions.assertEquals(2.0, sum.average().orElse(0.0), 0.0001);
    }

    @Test
    public void testAverageEmpty() {
        KahanSummation sum = new KahanSummation();

        Assertions.assertFalse(sum.average().isPresent());
    }

    @Test
    public void testToString() {
        KahanSummation sum = new KahanSummation();
        sum.add(1.0);
        sum.add(2.0);

        String str = sum.toString();
        Assertions.assertTrue(str.contains("count=2"));
        Assertions.assertTrue(str.contains("sum=3.000000"));
        Assertions.assertTrue(str.contains("average=1.500000"));
    }
}
