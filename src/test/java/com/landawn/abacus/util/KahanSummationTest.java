package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testCombineWithSelfIsAliasSafe() {
        final double[] values = { 1e-16, Math.ulp(1.0) / 2, 1e-16, 1.0 };

        final KahanSummation expected = KahanSummation.of(values);
        expected.combine(KahanSummation.of(values));

        final KahanSummation actual = KahanSummation.of(values);
        actual.combine(actual);

        Assertions.assertEquals(expected.count(), actual.count());
        Assertions.assertEquals(Double.doubleToLongBits(expected.sum()), Double.doubleToLongBits(actual.sum()));
    }

    @Test
    public void testAverageOverflowFallbackPreservesCancellation() {
        final KahanSummation sum = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);

        Assertions.assertEquals(0.0, sum.average().get(), 0.0);

        // Preserve the compensation accumulated before overflow as well: the four extreme
        // values cancel in exact arithmetic, leaving the otherwise sub-ULP value 1.0.
        final KahanSummation withCompensatedPrefix = KahanSummation.of(Double.MAX_VALUE, 1.0, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE);
        Assertions.assertEquals(0.2, withCompensatedPrefix.average().get(), 0.0);
    }

    @Test
    public void testAverageOverflowFallbackCombinesWithoutLosingCancellation() {
        final KahanSummation positive = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        final KahanSummation negative = KahanSummation.of(-Double.MAX_VALUE, -Double.MAX_VALUE);

        positive.combine(negative);

        Assertions.assertEquals(4, positive.count());
        Assertions.assertEquals(0.0, positive.average().get(), 0.0);

        final KahanSummation selfCombined = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        selfCombined.combine(selfCombined);
        Assertions.assertEquals(Double.MAX_VALUE, selfCombined.average().get(), 0.0);

        final KahanSummation aggregateCombined = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        aggregateCombined.combine(2, -Double.MAX_VALUE);
        Assertions.assertEquals(Double.MAX_VALUE / 4.0, aggregateCombined.average().get(), 0.0);
    }

    @Test
    public void testAverageOverflowFallbackKeepsCorrectlyRoundedThreeValueMean() {
        final KahanSummation sum = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);

        Assertions.assertEquals(Double.MAX_VALUE / 3.0, sum.average().get(), 0.0);
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

    @Test
    public void testSumRecoversCancellationAfterIntermediateOverflowInAnyOrder() {
        final KahanSummation overflowFirst = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);
        Assertions.assertEquals(Double.MAX_VALUE, overflowFirst.sum());
        Assertions.assertEquals(Double.MAX_VALUE / 3.0, overflowFirst.average().get(), 0.0);

        final KahanSummation cancelFirst = KahanSummation.of(Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE);
        Assertions.assertEquals(Double.MAX_VALUE, cancelFirst.sum());

        Assertions.assertEquals(5.0, KahanSummation.of(1e308, 1e308, -1e308, -1e308, 5).sum());
    }

    @Test
    public void testSumOfFiniteValuesAtOverflowThresholdIsCorrectlyRoundedInAnyOrder() {
        // MAX_VALUE + 2^970 is exactly halfway to 2^1024; MAX_VALUE has an odd significand, so it rounds to Infinity.
        final double p = Math.scalb(1.0, 969);
        final double[][] orders = { { Double.MAX_VALUE, p, p }, { p, Double.MAX_VALUE, p }, { p, p, Double.MAX_VALUE } };

        for (final double[] order : orders) {
            assertEquals(Double.POSITIVE_INFINITY, KahanSummation.of(order).sum());
            assertEquals(Double.NEGATIVE_INFINITY, KahanSummation.of(-order[0], -order[1], -order[2]).sum());
            assertEquals(Double.POSITIVE_INFINITY, N.sum(order));
            assertEquals(Double.POSITIVE_INFINITY, N.sumDouble(java.util.Arrays.asList(order[0], order[1], order[2]), Double::doubleValue));
        }

        // Just below the threshold the total stays MAX_VALUE.
        assertEquals(Double.MAX_VALUE, KahanSummation.of(p, Double.MAX_VALUE, p, -Double.MIN_VALUE).sum());
    }

    @Test
    public void testSmallValueBeforeLargerValueIsNotLost() {
        // Classic Kahan summation drops the leading 1.0 here (its correction is exact only while |sum| >= |value|);
        // the compensated (Kahan-Babuska) step keeps it.
        assertEquals(2.0, KahanSummation.of(1.0, 1e100, 1.0, -1e100).sum());
        assertEquals(2.0, N.sum(1.0, 1e100, 1.0, -1e100));
        assertEquals(0.5, N.average(1.0, 1e100, 1.0, -1e100));
    }

    @Test
    public void testCombineOverflowedWithOrdinaryFinite() {
        final KahanSummation a = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        final KahanSummation b = KahanSummation.of(Double.MAX_VALUE);
        a.combine(b);
        assertEquals(3, a.count());
        assertEquals(Double.MAX_VALUE, a.average().getAsDouble(), Double.MAX_VALUE * 1e-12);
    }

    @Test
    public void testCombineWithNonFiniteOther() {
        final KahanSummation a = KahanSummation.of(1.0);
        a.combine(KahanSummation.of(Double.POSITIVE_INFINITY));
        assertEquals(2, a.count());
        assertEquals(Double.POSITIVE_INFINITY, a.average().getAsDouble());
    }

    @Test
    public void testCompensationOverflowWhileSimpleSumFinite() {
        final double p = Math.scalb(1.0, 969);
        final KahanSummation a = KahanSummation.of(Double.MAX_VALUE, p, p);
        final double avg = a.average().getAsDouble();
        assertTrue(Double.isFinite(avg));
        assertEquals(Double.MAX_VALUE / 3, avg, Double.MAX_VALUE * 1e-12);
    }

    @Test
    public void testCompensationOverflowThenCancellationAverageIsExact() {
        final double p = Math.scalb(1.0, 969);
        // Exact total is 2^970; the simple sum has rounded both p values away and ends at 0.0.
        assertEquals(Math.scalb(1.0, 968), KahanSummation.of(Double.MAX_VALUE, p, p, -Double.MAX_VALUE).average().getAsDouble());

        final KahanSummation combined = KahanSummation.of(Double.MAX_VALUE, p);
        combined.combine(1, p);
        combined.combine(KahanSummation.of(-Double.MAX_VALUE));
        assertEquals(4, combined.count());
        assertEquals(Math.scalb(1.0, 968), combined.average().getAsDouble());
    }

    @Test
    public void testAverageNonFiniteValuesAloneDecideResult() {
        assertEquals(Double.NEGATIVE_INFINITY, KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY).average().getAsDouble());
        assertEquals(Double.POSITIVE_INFINITY, KahanSummation.of(-Double.MAX_VALUE, -Double.MAX_VALUE, Double.POSITIVE_INFINITY).average().getAsDouble());
        assertTrue(Double.isNaN(KahanSummation.of(Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).average().getAsDouble()));

        // sum() follows the same rule, in any order, unlike sequential addition (NaN for the first order).
        assertEquals(Double.NEGATIVE_INFINITY, KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY).sum());
        assertEquals(Double.NEGATIVE_INFINITY, KahanSummation.of(Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.MAX_VALUE).sum());
        assertEquals(Double.NEGATIVE_INFINITY, KahanSummation.of(Double.NEGATIVE_INFINITY, Double.MAX_VALUE, Double.MAX_VALUE).sum());
        assertTrue(Double.isNaN(KahanSummation.of(Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY).sum()));
        assertTrue(Double.isNaN(KahanSummation.of(1.0, Double.NaN, Double.POSITIVE_INFINITY).sum()));

        final KahanSummation viaCombineSummation = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        viaCombineSummation.combine(KahanSummation.of(Double.NEGATIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, viaCombineSummation.average().getAsDouble());

        final KahanSummation viaCombineCount = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        viaCombineCount.combine(1, Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, viaCombineCount.average().getAsDouble());

        final KahanSummation self = KahanSummation.of(Double.MAX_VALUE, Double.NEGATIVE_INFINITY);
        self.combine(self);
        assertEquals(4, self.count());
        assertEquals(Double.NEGATIVE_INFINITY, self.average().getAsDouble());
    }

    @Test
    public void testEveryDoublePathAgreesBitForBitOnFiniteSums() throws Exception {
        // N.compensatedSum (the double[] loop) must stay op-for-op identical to KahanSummation.compensatedAdd, so every
        // sequential path returns the same bits whenever the running sums stay finite.
        final java.util.Random random = new java.util.Random(20261112);

        for (int iteration = 0; iteration < 5000; iteration++) {
            final double[] values = new double[1 + random.nextInt(40)];

            for (int i = 0; i < values.length; i++) {
                values[i] = random.nextGaussian() * Math.pow(10, random.nextInt(60) - 30) * (random.nextInt(4) == 0 ? 1e250 : 1);
            }

            final java.util.List<Double> list = new java.util.ArrayList<>(values.length);

            for (final double value : values) {
                list.add(value);
            }

            final String input = java.util.Arrays.toString(values);
            final long expected = Double.doubleToLongBits(KahanSummation.of(values).sum());
            assertEquals(expected, Double.doubleToLongBits(N.sum(values)), input);
            assertEquals(expected, Double.doubleToLongBits(N.sumDouble(list, Double::doubleValue)), input);
            assertEquals(expected, Double.doubleToLongBits(Iterables.sumDouble(list, Double::doubleValue).orElseThrow()), input);
            assertEquals(expected, Double.doubleToLongBits(Seq.of(list).sumDouble(Double::doubleValue)), input);
            assertEquals(expected, Double.doubleToLongBits(com.landawn.abacus.util.stream.DoubleStream.of(values).sum()), input);
            assertEquals(expected, Double.doubleToLongBits(list.stream().collect(com.landawn.abacus.util.stream.Collectors.summingDouble(Double::doubleValue))),
                    input);

            final long expectedAverage = Double.doubleToLongBits(KahanSummation.of(values).average().orElseThrow());
            assertEquals(expectedAverage, Double.doubleToLongBits(N.average(values)), input);
            assertEquals(expectedAverage, Double.doubleToLongBits(N.averageDouble(list, Double::doubleValue)), input);
            assertEquals(expectedAverage, Double.doubleToLongBits(com.landawn.abacus.util.stream.DoubleStream.of(values).average().orElseThrow()), input);
            assertEquals(expectedAverage,
                    Double.doubleToLongBits(list.stream().collect(com.landawn.abacus.util.stream.Collectors.averagingDouble(Double::doubleValue))), input);
        }
    }

    @Test
    public void testFloatPathsKeepSmallValueBeforeLargerValue() {
        // float values are widened and added with KahanSummation, so they get the same compensation.
        assertEquals(2f, N.sum(1f, 1e30f, 1f, -1e30f));
        assertEquals(2.0, N.sumToDouble(1f, 1e30f, 1f, -1e30f));
        assertEquals(0.5, N.average(1f, 1e30f, 1f, -1e30f));
        assertEquals(2.0, com.landawn.abacus.util.stream.FloatStream.of(1f, 1e30f, 1f, -1e30f).sum());
        assertEquals(0.5, com.landawn.abacus.util.stream.FloatStream.of(1f, 1e30f, 1f, -1e30f).average().orElseThrow());

        final FloatSummaryStatistics statistics = new FloatSummaryStatistics();
        statistics.accept(1f);
        statistics.accept(1e30f);
        statistics.accept(1f);
        statistics.accept(-1e30f);
        assertEquals(2.0, statistics.getSum());
        assertEquals(0.5, statistics.getAverage());
    }

    @Test
    public void testOverflowAndNonFiniteRulesHoldInEveryDoublePath() throws Exception {
        final double max = Double.MAX_VALUE;
        final java.util.List<Double> cancelling = java.util.Arrays.asList(max, max, -max);
        final java.util.List<Double> cancellingToZero = java.util.Arrays.asList(max, max, -max, -max);
        final java.util.List<Double> negativeInfinity = java.util.Arrays.asList(max, max, Double.NEGATIVE_INFINITY);
        final java.util.List<Double> oppositeInfinities = java.util.Arrays.asList(Double.POSITIVE_INFINITY, max, Double.NEGATIVE_INFINITY);

        // A plain overflow stays infinite; the average of the same values is exact.
        assertEquals(Double.POSITIVE_INFINITY, KahanSummation.of(max, max).sum());
        assertEquals(max, KahanSummation.of(max, max).average().orElseThrow());

        assertEquals(max, Seq.of(cancelling).sumDouble(Double::doubleValue));
        assertEquals(0.0, Seq.of(cancellingToZero).averageDouble(Double::doubleValue).orElseThrow());
        assertEquals(Double.NEGATIVE_INFINITY, Seq.of(negativeInfinity).sumDouble(Double::doubleValue));
        assertEquals(Double.NEGATIVE_INFINITY, Seq.of(negativeInfinity).averageDouble(Double::doubleValue).orElseThrow());
        assertTrue(Double.isNaN(Seq.of(oppositeInfinities).sumDouble(Double::doubleValue)));

        assertEquals(max, N.sumDouble(cancelling, Double::doubleValue));
        assertEquals(max, Iterables.sumDouble(cancelling, Double::doubleValue).orElseThrow());
        assertEquals(0.0, N.averageDouble(cancellingToZero, Double::doubleValue));
        assertEquals(Double.NEGATIVE_INFINITY, N.averageDouble(negativeInfinity, Double::doubleValue));
        assertEquals(Double.NEGATIVE_INFINITY, Iterables.sumDouble(negativeInfinity, Double::doubleValue).orElseThrow());

        assertEquals(max, cancelling.stream().collect(com.landawn.abacus.util.stream.Collectors.summingDouble(Double::doubleValue)));
        assertEquals(Double.NEGATIVE_INFINITY, negativeInfinity.stream().collect(com.landawn.abacus.util.stream.Collectors.summingDouble(Double::doubleValue)));

        final Tuple.Tuple2<Double, Double> sums = cancelling.stream()
                .collect(com.landawn.abacus.util.stream.Collectors.MoreCollectors.summingDouble(Double::doubleValue, d -> -d));
        assertEquals(max, sums._1);
        assertEquals(-max, sums._2);

        final Tuple.Tuple2<Double, Double> infiniteSums = negativeInfinity.stream()
                .collect(com.landawn.abacus.util.stream.Collectors.MoreCollectors.summingDouble(Double::doubleValue, d -> -d));
        assertEquals(Double.NEGATIVE_INFINITY, infiniteSums._1);
        assertEquals(Double.POSITIVE_INFINITY, infiniteSums._2);

        final Tuple.Tuple2<Double, Double> averages = cancellingToZero.stream()
                .collect(com.landawn.abacus.util.stream.Collectors.MoreCollectors.averagingDouble(Double::doubleValue, d -> -d));
        assertEquals(0.0, averages._1);
        assertEquals(0.0, averages._2);

        assertEquals(max, com.landawn.abacus.util.stream.Stream.of(cancelling).sumDouble(Double::doubleValue));
        assertEquals(Double.NEGATIVE_INFINITY, com.landawn.abacus.util.stream.Stream.of(negativeInfinity).sumDouble(Double::doubleValue));
        assertTrue(Double.isNaN(com.landawn.abacus.util.stream.Stream.of(oppositeInfinities).sumDouble(Double::doubleValue)));
        assertEquals(0.0, com.landawn.abacus.util.stream.Stream.of(cancellingToZero).averageDouble(Double::doubleValue).orElseThrow());
        assertEquals(Double.NEGATIVE_INFINITY, com.landawn.abacus.util.stream.Stream.of(negativeInfinity).averageDouble(Double::doubleValue).orElseThrow());
        assertEquals(max, com.landawn.abacus.util.stream.Stream.of(cancelling).parallel(2).sumDouble(Double::doubleValue));

        // Parallel streams combine partial sums and must follow the same rules whatever the split.
        for (int threads = 2; threads <= 4; threads++) {
            final double[] padded = new double[1000];
            padded[100] = max;
            padded[400] = max;
            padded[700] = -max;
            padded[900] = -max;
            assertEquals(0.0, com.landawn.abacus.util.stream.DoubleStream.of(padded).parallel(threads).sum(), "threads=" + threads);
            assertEquals(0.0, com.landawn.abacus.util.stream.DoubleStream.of(padded).parallel(threads).average().orElseThrow(), "threads=" + threads);

            padded[900] = Double.NEGATIVE_INFINITY;
            padded[700] = 0;
            assertEquals(Double.NEGATIVE_INFINITY, com.landawn.abacus.util.stream.DoubleStream.of(padded).parallel(threads).sum(), "threads=" + threads);

            padded[0] = Double.POSITIVE_INFINITY;
            assertTrue(Double.isNaN(com.landawn.abacus.util.stream.DoubleStream.of(padded).parallel(threads).sum()), "threads=" + threads);
        }
    }

    @Test
    public void testOverflowSafeTotalStaysAuthoritativeAfterReturningToFiniteRange() throws Exception {
        final double max = Double.MAX_VALUE;
        final double p = 0x1p969;
        // At the third value only sum - correction overflows. The double correction then drops the 1 before the
        // cancellation, so only the exact overflow-safe total still holds the true total, 1.
        final double[] values = { max, p, p, 1, -max, -p, -p };

        final KahanSummation summation = KahanSummation.of(values);
        assertEquals(1.0, summation.sum());
        assertEquals(1.0 / 7, summation.average().orElseThrow());

        final KahanSummation merged = KahanSummation.of(max, p, p, 1);
        merged.combine(KahanSummation.of(-max, -p, -p));
        assertEquals(1.0, merged.sum());
        assertEquals(1.0 / 7, merged.average().orElseThrow());

        final KahanSummation combinedAggregates = KahanSummation.of(max, p, p, 1);
        combinedAggregates.combine(1, -max);
        combinedAggregates.combine(2, -2 * p);
        assertEquals(1.0, combinedAggregates.sum());

        final KahanSummation doubled = KahanSummation.of(values);
        doubled.combine(doubled);
        assertEquals(2.0, doubled.sum());
        assertEquals(1.0 / 7, doubled.average().orElseThrow());

        // A non-finite value still decides the result alone.
        final KahanSummation withInfinity = KahanSummation.of(values);
        withInfinity.add(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, withInfinity.sum());
        assertEquals(Double.NEGATIVE_INFINITY, withInfinity.average().orElseThrow());

        // The array overloads re-scan exactly after any intermediate overflow; the delegating APIs agree.
        final double[] padded = new double[values.length + 2];
        padded[0] = Double.NaN;
        padded[padded.length - 1] = Double.NEGATIVE_INFINITY;
        System.arraycopy(values, 0, padded, 1, values.length);
        assertEquals(1.0, N.sum(values));
        assertEquals(1.0 / 7, N.average(values));
        assertEquals(1.0, N.sum(padded, 1, padded.length - 1));
        assertEquals(1.0 / 7, N.average(padded, 1, padded.length - 1));

        final java.util.List<Double> list = new java.util.ArrayList<>();
        for (final double value : values) {
            list.add(value);
        }
        final java.util.function.ToDoubleFunction<Double> identity = Double::doubleValue;
        assertEquals(1.0, N.sumDouble(list, identity));
        assertEquals(1.0 / 7, N.averageDouble(list, identity));
        assertEquals(1.0, Iterables.sumDouble(list, identity).orElseThrow());
        assertEquals(1.0, com.landawn.abacus.util.stream.DoubleStream.of(values).sum());
        assertEquals(1.0, com.landawn.abacus.util.stream.DoubleStream.of(values).map(d -> d).sum());
        assertEquals(1.0, list.stream().collect(com.landawn.abacus.util.stream.Collectors.summingDouble(identity)));
        assertEquals(1.0 / 7, list.stream().collect(com.landawn.abacus.util.stream.Collectors.averagingDouble(identity)));
        assertEquals(1.0, Seq.of(list).sumDouble(Double::doubleValue));
        assertEquals(1.0 / 7, Seq.of(list).averageDouble(Double::doubleValue).orElseThrow());
        assertEquals(1.0, com.landawn.abacus.util.stream.Stream.of(list).sumDouble(identity));
        assertEquals(1.0 / 7, com.landawn.abacus.util.stream.Stream.of(list).averageDouble(identity).orElseThrow());
    }

    @Test
    public void testExactTotalStartsAtThresholdSoSmallValuesNextToHugeOnesAreKept() {
        // The double correction alone would drop the 3.0 before the running total overflows (it already holds about
        // 2^969), but the exact total starts at MAX_VALUE, the first value of at least 2^960, so it keeps the 3.0.
        final double max = Double.MAX_VALUE;
        final double[] values = { 1e-300, max, -3.9e-7, -0x1p969, 3.0, 0x1p1023, 0x1p969, -max, -0x1p1023 };

        assertEquals(3.0 - 3.9e-7, KahanSummation.of(values).sum());
        assertEquals((3.0 - 3.9e-7) / values.length, KahanSummation.of(values).average().orElseThrow());
        assertEquals(3.0 - 3.9e-7, N.sum(values));
        assertEquals((3.0 - 3.9e-7) / values.length, N.average(values));

        // No overflow at all: 2^969 is below half an ulp of 1e308, so the correction holds it and drops the 1.0 and 53.5...
        final double[] cancelling = { 1e308, 0x1p969, 1.0, 53.54802370310241, -1e308, -0x1p969 };
        assertEquals(54.54802370310241, KahanSummation.of(cancelling).sum());
        assertEquals(54.54802370310241, N.sum(cancelling));
        final java.util.List<Double> list = new java.util.ArrayList<>();
        for (final double value : cancelling) {
            list.add(value);
        }
        assertEquals(54.54802370310241, N.sumDouble(list, Double::doubleValue));
        assertEquals(54.54802370310241, com.landawn.abacus.util.stream.DoubleStream.of(cancelling).sum());
        assertEquals(54.54802370310241, list.stream().collect(com.landawn.abacus.util.stream.Collectors.summingDouble(Double::doubleValue)));
    }

    @Test
    public void testTotalsNearTheOverflowThresholdAreCorrectlyRounded() {
        final double max = Double.MAX_VALUE;
        final double p = 0x1p969;
        // The exact total is MAX_VALUE + 2^970 (half an ulp above MAX_VALUE) plus about 4.7e21, so it rounds to Infinity.
        final double[] justAbove = { 4.704638986609585E21, 1.0E16, 201939.76236321786, -7978.400570849809, max, p, p, -1.0 };
        assertEquals(Double.POSITIVE_INFINITY, KahanSummation.of(justAbove).sum());
        assertEquals(Double.POSITIVE_INFINITY, N.sum(justAbove));
        // The exact total is -(MAX_VALUE + 2^970) + 2953.7..., just inside the range, so it rounds to -MAX_VALUE.
        final double[] justInside = { 2957.707988316688, -max, -1.0, -3.0, -p, -p };
        assertEquals(-max, KahanSummation.of(justInside).sum());
        assertEquals(-max, N.sum(justInside));
    }

    @Test
    public void testExactTotalStartsOnlyAtThreshold() throws Exception {
        final java.lang.reflect.Field exactTotal = KahanSummation.class.getDeclaredField("overflowSafeSum");
        exactTotal.setAccessible(true);
        final double below = Math.nextDown(KahanSummation.EXACT_TOTAL_THRESHOLD);

        final KahanSummation ordinary = KahanSummation.of(below, 1.0, -below);
        assertEquals(null, exactTotal.get(ordinary));
        assertEquals(1.0, ordinary.sum());

        final KahanSummation atThreshold = KahanSummation.of(1.0, KahanSummation.EXACT_TOTAL_THRESHOLD, -KahanSummation.EXACT_TOTAL_THRESHOLD);
        assertTrue(exactTotal.get(atThreshold) != null);
        assertEquals(1.0, atThreshold.sum());

        // The running total, not only a single value, starts it.
        final KahanSummation runningTotal = KahanSummation.of(below, below);
        assertTrue(exactTotal.get(runningTotal) != null);
        assertEquals(2 * below, runningTotal.sum());

        // Combining a large aggregate starts it too.
        final KahanSummation combined = KahanSummation.of(1.0);
        combined.combine(1, KahanSummation.EXACT_TOTAL_THRESHOLD);
        combined.combine(1, -KahanSummation.EXACT_TOTAL_THRESHOLD);
        assertTrue(exactTotal.get(combined) != null);
        assertEquals(1.0, combined.sum());
    }

    @Test
    public void testZerosAfterTheExactTotalStartsAreCountedButSkipTheExactAddition() throws Exception {
        final java.lang.reflect.Field exactTotal = KahanSummation.class.getDeclaredField("overflowSafeSum");
        exactTotal.setAccessible(true);
        final double huge = 0x1p1000;

        final KahanSummation summation = KahanSummation.of(huge, 12.5, -huge);
        // 12.5 is not one of BigDecimal's cached small integers, so assertSame below detects a new BigDecimal.
        final Object exactBefore = exactTotal.get(summation);
        assertTrue(exactBefore != null);

        summation.add(0.0);
        summation.add(-0.0);
        summation.combine(3, 0.0);
        summation.combine(2, -0.0);

        // Zeros (either sign) leave the exact total untouched, without a new BigDecimal...
        assertSame(exactBefore, exactTotal.get(summation));
        // ...but still count as observations.
        assertEquals(10, summation.count());
        assertEquals(12.5, summation.sum());
        assertEquals(1.25, summation.average().getAsDouble());

        // A non-zero value after the zeros is still added exactly.
        summation.add(2.0);
        assertTrue(exactBefore != exactTotal.get(summation));
        assertEquals(14.5, summation.sum());
        assertEquals(14.5 / 11, summation.average().getAsDouble());

        // Same totals as the array path, which re-scans exactly.
        final double[] values = { huge, 12.5, -huge, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0 };
        assertEquals(N.sum(values), summation.sum());
        assertEquals(N.average(values), summation.average().getAsDouble());
    }
}
