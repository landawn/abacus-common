package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalDouble;

public class AggregationTest extends TestBase {

    @Test
    public void testNPrimitiveDoubleAverageAvoidsIntermediateOverflow() {
        Assertions.assertEquals(Double.MAX_VALUE, N.average(Double.MAX_VALUE, Double.MAX_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE, N.average(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
        Assertions.assertEquals(-Double.MAX_VALUE, N.average(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        Assertions.assertEquals(0d, N.average(Double.MAX_VALUE, -Double.MAX_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE / 3d, N.average(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE));

        final double[] values = { 1d, Double.MAX_VALUE, Double.MAX_VALUE, -1d };
        Assertions.assertEquals(Double.MAX_VALUE, N.average(values, 1, 3));
    }

    @Test
    public void testNBoxedDoubleAverageAvoidsIntermediateOverflow() {
        final Double[] values = { Double.MAX_VALUE, Double.MAX_VALUE };

        Assertions.assertEquals(Double.MAX_VALUE, N.averageDouble(values));
        Assertions.assertEquals(Double.MAX_VALUE, N.averageDouble(values, 0, values.length));
        Assertions.assertEquals(Double.MAX_VALUE, N.averageDouble(Arrays.asList(values)));
        Assertions.assertEquals(Double.MAX_VALUE, N.averageDouble(new LinkedList<>(Arrays.asList(values)), 0, values.length));
    }

    @Test
    public void testDoubleAverageInvokesExtractorOncePerElement() {
        final AtomicInteger invocationCount = new AtomicInteger();
        final double average = N.averageDouble(new String[] { "a", "b" }, value -> {
            invocationCount.incrementAndGet();
            return Double.MAX_VALUE;
        });

        Assertions.assertEquals(Double.MAX_VALUE, average);
        Assertions.assertEquals(2, invocationCount.get());
    }

    @Test
    public void testDoubleAveragePreservesNonFinitePropagation() {
        Assertions.assertEquals(Double.POSITIVE_INFINITY, N.average(Double.POSITIVE_INFINITY, 1d));
        Assertions.assertTrue(Double.isNaN(N.average(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, N.averageDouble(Arrays.asList(Double.POSITIVE_INFINITY, 1d)));
        Assertions.assertTrue(Double.isNaN(N.averageDouble(Arrays.asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))));
    }

    @Test
    public void testIterablesAndSeqDoubleAverageAvoidIntermediateOverflow() throws Exception {
        final OptionalDouble iterableAverage = Iterables.averageDouble(Arrays.asList(Double.MAX_VALUE, Double.MAX_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE, iterableAverage.orElseThrow());

        final OptionalDouble sequenceAverage = Seq.of(Double.MAX_VALUE, Double.MAX_VALUE).averageDouble(value -> value);
        Assertions.assertEquals(Double.MAX_VALUE, sequenceAverage.orElseThrow());
    }

    @Test
    public void testKahanAverageAvoidsIntermediateOverflowAfterAddAndCombine() {
        final KahanSummation direct = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, direct.sum());
        Assertions.assertEquals(Double.MAX_VALUE, direct.average().orElseThrow());

        final KahanSummation combined = KahanSummation.of(Double.MAX_VALUE);
        combined.combine(KahanSummation.of(Double.MAX_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE, combined.average().orElseThrow());

        final KahanSummation cancelledAfterOverflow = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE);
        final double expectedCancelledAverage = Double.MAX_VALUE / 3d;
        Assertions.assertEquals(expectedCancelledAverage, cancelledAfterOverflow.average().orElseThrow(), Math.ulp(expectedCancelledAverage));

        final KahanSummation positiveOverflow = KahanSummation.of(Double.MAX_VALUE, Double.MAX_VALUE);
        positiveOverflow.combine(KahanSummation.of(-Double.MAX_VALUE, -Double.MAX_VALUE));
        Assertions.assertEquals(0d, positiveOverflow.average().orElseThrow());

        final KahanSummation combinedAggregate = KahanSummation.of(Double.MAX_VALUE);
        combinedAggregate.combine(1, Double.MAX_VALUE);
        Assertions.assertEquals(Double.MAX_VALUE, combinedAggregate.average().orElseThrow());

        direct.add(Double.NEGATIVE_INFINITY);
        Assertions.assertTrue(Double.isNaN(direct.average().orElseThrow()));
    }

    @Test
    public void testNPrimitiveLongAverageHandlesExtremeFiniteValues() {
        Assertions.assertEquals(Long.MAX_VALUE, N.average(Long.MAX_VALUE, Long.MAX_VALUE));
        Assertions.assertEquals(Long.MIN_VALUE, N.average(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    @Test
    public void testNumbersMeanHandlesExtremeFiniteValues() {
        Assertions.assertEquals(-0.5d, Numbers.mean(Long.MIN_VALUE, Long.MAX_VALUE));
        Assertions.assertEquals(0d, Numbers.mean(-Double.MAX_VALUE, Double.MAX_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE, Numbers.mean(Double.MAX_VALUE, Double.MAX_VALUE));
        Assertions.assertEquals(Double.MAX_VALUE, Numbers.mean(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE));
        Assertions.assertEquals(-Double.MAX_VALUE, Numbers.mean(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        Assertions.assertEquals(0d, Numbers.mean(0d, Double.MIN_VALUE));
        Assertions.assertEquals(Double.MIN_VALUE, Numbers.mean(Double.MIN_VALUE, Double.MIN_VALUE));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.POSITIVE_INFINITY));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Numbers.mean(Double.NaN));
    }

    @Test
    public void testLongAveragesUseExactAllocationFreeOverflowAccumulator() throws Exception {
        final Long[] positiveOverflow = { Long.MAX_VALUE, Long.MAX_VALUE };
        final Long[] cancelledOverflow = { Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE };
        final Long[] negativeOverflow = { Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MAX_VALUE };

        Assertions.assertEquals(Long.MAX_VALUE, N.averageLong(positiveOverflow));
        Assertions.assertEquals(-0.5d, N.averageLong(cancelledOverflow));
        Assertions.assertEquals(-0.5d, N.averageLong(negativeOverflow));
        Assertions.assertEquals(-0.5d, Iterables.averageLong(Arrays.asList(cancelledOverflow)).orElseThrow());
        Assertions.assertEquals(-0.5d, Iterables.averageLong(Arrays.asList(negativeOverflow)).orElseThrow());
        Assertions.assertEquals(-0.5d, Seq.of(cancelledOverflow).averageLong(value -> value).orElseThrow());
        Assertions.assertEquals(-0.5d, Seq.of(negativeOverflow).averageLong(value -> value).orElseThrow());
    }
}
