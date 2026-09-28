package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * Review fixes of 2026-09-24 for the Short, Float and Double stream families.
 *
 * <ul>
 *   <li><b>C-001</b> - {@code step(n)} on an iterator-backed stream skipped the gap eagerly inside {@code next()}, so
 *       {@code step(n).limit(k)} pulled {@code n - 1} extra elements and {@code step(n).first()} evaluated (and could fail
 *       on) upstream elements that are never returned.</li>
 *   <li><b>C-014</b> - {@code DoubleStream.transformViaJdkStream(fn[, false])} left the source open when {@code fn}
 *       threw.</li>
 *   <li><b>C-016</b> - {@code toCollection/toMultiset/toMap/groupTo} with a factory returning {@code null}: a bare NPE on
 *       non-empty input and a silent {@code null} result on empty input. Now an NPE naming the factory, and the stream is closed.</li>
 *   <li><b>C-049</b> - {@code zip(Collection, *NFunction)}: a {@code null} zip result silently became {@code 0}.</li>
 *   <li>Doc-only regression locks: C-034 (Short sum/average), C-043/C-044 (DoubleStream sum/average/summaryStatistics),
 *       C-045 (merge selectors), C-046 (ShortStream.sum threshold), C-047 (boxed intersection/difference), C-063
 *       (mapPartial null optional), P2-05 (FloatStream distinct/summaryStatistics NaN).</li>
 * </ul>
 */
@Tag("unit")
public class PrimitiveStreamsBReview20260924Test extends TestBase {

    private static final double MAX = Double.MAX_VALUE;

    // ------------------------------------------------------------------------------------------------------------
    // C-001 step(n) is lazy
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC001_shortStep_limitPullsOnlyWhatItReturns() {
        final AtomicLong pulls = new AtomicLong();
        final short[] result = ShortStream.generate(() -> (short) (pulls.incrementAndGet() % 100)).step(1000).limit(2).toArray();

        assertArrayEquals(new short[] { 1, 1 }, result);
        assertEquals(1001, pulls.get());
    }

    @Test
    public void testC001_floatStep_limitPullsOnlyWhatItReturns() {
        final AtomicLong pulls = new AtomicLong();
        final float[] result = FloatStream.generate(() -> pulls.incrementAndGet()).step(1000).limit(2).toArray();

        assertArrayEquals(new float[] { 1f, 1001f }, result);
        assertEquals(1001, pulls.get());
    }

    @Test
    public void testC001_doubleStep_limitPullsOnlyWhatItReturns() {
        final AtomicLong pulls = new AtomicLong();
        final double[] result = DoubleStream.generate(() -> pulls.incrementAndGet()).step(1000).limit(2).toArray();

        assertArrayEquals(new double[] { 1d, 1001d }, result);
        assertEquals(1001, pulls.get());
    }

    @Test
    public void testC001_hugeStep_firstPullsOneElement() {
        final AtomicLong shortPulls = new AtomicLong();
        final AtomicLong floatPulls = new AtomicLong();
        final AtomicLong doublePulls = new AtomicLong();

        assertEquals(OptionalShort.of((short) 1), ShortStream.generate(() -> (short) shortPulls.incrementAndGet()).step(1_000_000).first());
        assertEquals(OptionalFloat.of(1f), FloatStream.generate(() -> floatPulls.incrementAndGet()).step(1_000_000).limit(1).first());
        assertEquals(OptionalDouble.of(1d), DoubleStream.generate(() -> doublePulls.incrementAndGet()).limit(3_000_000).step(Long.MAX_VALUE).first());

        assertEquals(1, shortPulls.get());
        assertEquals(1, floatPulls.get());
        assertEquals(1, doublePulls.get());
    }

    @Test
    public void testC001_skippedElementThatWouldFailIsNeverEvaluatedByFirst() {
        assertEquals(OptionalShort.of((short) 1), ShortStream.of((short) 1, (short) 2, (short) 3).map(x -> {
            if (x == 2) {
                throw new IllegalStateException("evaluated a skipped element");
            }
            return x;
        }).step(2).first());

        assertEquals(OptionalFloat.of(1f), FloatStream.of(1f, 2f, 3f).map(x -> {
            if (x == 2f) {
                throw new IllegalStateException("evaluated a skipped element");
            }
            return x;
        }).step(2).first());

        assertEquals(OptionalDouble.of(1d), DoubleStream.of(1d, 2d, 3d).map(x -> {
            if (x == 2d) {
                throw new IllegalStateException("evaluated a skipped element");
            }
            return x;
        }).step(2).first());
    }

    @Test
    public void testC001_stepValuesUnchanged_iteratorVsArray() {
        for (int len = 0; len <= 12; len++) {
            final short[] s = new short[len];
            final float[] f = new float[len];
            final double[] d = new double[len];

            for (int i = 0; i < len; i++) {
                s[i] = (short) (i - 5);
                f[i] = i - 5.5f;
                d[i] = i - 5.25d;
            }

            for (int step = 1; step <= 5; step++) {
                assertArrayEquals(ShortStream.of(s).step(step).toArray(), ShortStream.of(s).map(x -> x).step(step).toArray(), "len=" + len + ", step=" + step);
                assertArrayEquals(FloatStream.of(f).step(step).toArray(), FloatStream.of(f).map(x -> x).step(step).toArray(), "len=" + len + ", step=" + step);
                assertArrayEquals(DoubleStream.of(d).step(step).toArray(), DoubleStream.of(d).map(x -> x).step(step).toArray(), "len=" + len + ", step=" + step);

                assertEquals(ShortStream.of(s).step(step).count(), ShortStream.of(s).map(x -> x).step(step).count());
                assertEquals(DoubleStream.of(d).step(step).count(), DoubleStream.of(d).map(x -> x).step(step).count());
            }
        }

        assertArrayEquals(new short[] { 1, 4 }, ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5).map(x -> x).step(3).toArray());
        assertArrayEquals(new double[] { 3d }, DoubleStream.of(1d, 2d, 3d, 4d, 5d).map(x -> x).step(2).skip(1).limit(1).toArray());
        assertArrayEquals(new float[0], FloatStream.empty().map(x -> x).step(7).toArray());
    }

    @Test
    public void testC001_parallelStepResultsUnchanged() {
        // step directly after parallel(): the source is still read in encounter order.
        final double[] d = new double[1000];
        final float[] f = new float[1000];
        for (int i = 0; i < d.length; i++) {
            d[i] = i;
            f[i] = i;
        }

        final double[] expected = DoubleStream.of(d).step(7).toArray();
        final float[] expectedF = FloatStream.of(f).step(7).toArray();

        assertArrayEquals(expected, DoubleStream.of(d).map(x -> x).parallel(4).step(7).sorted().toArray());
        assertArrayEquals(expected, DoubleStream.of(d).parallel(4).step(7).map(x -> x + 0.0).sorted().toArray());
        assertArrayEquals(expectedF, FloatStream.of(f).map(x -> x).parallel(4).step(7).map(x -> x).sorted().toArray());
        assertEquals(ShortStream.range((short) 0, (short) 1000).step(7).sum(),
                ShortStream.range((short) 0, (short) 1000).parallel(4).step(7).map(x -> x).sum());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-014 DoubleStream.transformViaJdkStream closes the source when the eager function throws
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC014_transformViaJdkStreamClosesSourceWhenTransferThrows() {
        final List<Supplier<DoubleStream>> sources = Arrays.asList(() -> DoubleStream.of(1, 2), () -> DoubleStream.of(1, 2).map(x -> x),
                () -> DoubleStream.of(1, 2).parallel(2), () -> DoubleStream.empty());

        for (final Supplier<DoubleStream> source : sources) {
            final AtomicInteger closed = new AtomicInteger();
            final DoubleStream s1 = source.get().onClose(closed::incrementAndGet);
            final IllegalStateException e1 = assertThrows(IllegalStateException.class, () -> s1.transformViaJdkStream(js -> {
                throw new IllegalStateException("boom");
            }));
            assertEquals("boom", e1.getMessage());
            assertEquals(1, closed.get());
            assertThrows(IllegalStateException.class, s1::sum); // closed

            final DoubleStream s2 = source.get().onClose(closed::incrementAndGet);
            assertThrows(IllegalStateException.class, () -> s2.transformViaJdkStream(js -> {
                throw new IllegalStateException("boom");
            }, false));
            assertEquals(2, closed.get());
        }
    }

    @Test
    public void testC014_transformViaJdkStreamSuccessPathUnchanged() {
        final AtomicInteger closed = new AtomicInteger();

        final double[] result = DoubleStream.of(5, 1, 3).onClose(closed::incrementAndGet).transformViaJdkStream(java.util.stream.DoubleStream::sorted).toArray();

        assertArrayEquals(new double[] { 1, 3, 5 }, result);
        assertEquals(1, closed.get());

        final DoubleStream unconsumed = DoubleStream.of(5, 1, 3).onClose(closed::incrementAndGet).transformViaJdkStream(js -> js.map(x -> x * 2), false);
        assertEquals(1, closed.get());
        unconsumed.close();
        assertEquals(2, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-016 factory returning null -> NPE, stream closed, empty and non-empty input
    // ------------------------------------------------------------------------------------------------------------

    private static <S extends BaseStream<?, ?, ?, ?, ?, ?, ?, S>> void assertNullFactoryRejected(final Supplier<S> source, final Function<S, Object> terminal,
            final String expectedName) {
        final AtomicInteger closed = new AtomicInteger();
        final S s = source.get().onClose(closed::incrementAndGet);

        final NullPointerException e = assertThrows(NullPointerException.class, () -> terminal.apply(s));

        assertTrue(e.getMessage().contains(expectedName), e.getMessage());
        assertEquals(1, closed.get());
    }

    private static List<Supplier<ShortStream>> shortSources() {
        return Arrays.asList(() -> ShortStream.of((short) 1, (short) 2, (short) 1), () -> ShortStream.of((short) 1, (short) 2, (short) 1).map(x -> x),
                () -> ShortStream.of(new short[0]), () -> ShortStream.of(new short[0]).map(x -> x), () -> ShortStream.of(new short[] { 9, 1, 2 }, 1, 1));
    }

    private static List<Supplier<FloatStream>> floatSources() {
        return Arrays.asList(() -> FloatStream.of(1f, 2f, 1f), () -> FloatStream.of(1f, 2f, 1f).map(x -> x), () -> FloatStream.of(new float[0]),
                () -> FloatStream.of(new float[0]).map(x -> x), () -> FloatStream.of(new float[] { 9f, 1f, 2f }, 1, 1));
    }

    private static List<Supplier<DoubleStream>> doubleSources() {
        return Arrays.asList(() -> DoubleStream.of(1d, 2d, 1d), () -> DoubleStream.of(1d, 2d, 1d).map(x -> x), () -> DoubleStream.of(new double[0]),
                () -> DoubleStream.of(new double[0]).map(x -> x), () -> DoubleStream.of(new double[] { 9d, 1d, 2d }, 1, 1));
    }

    @Test
    public void testC016_shortNullFactories() {
        for (final Supplier<ShortStream> src : shortSources()) {
            assertNullFactoryRejected(src, s -> s.toCollection(() -> (List<Short>) null), "supplier returned null");
            assertNullFactoryRejected(src, s -> s.toMultiset(() -> (Multiset<Short>) null), "supplier returned null");
            assertNullFactoryRejected(src, s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> (Map<Short, Short>) null), "mapFactory returned null");
            assertNullFactoryRejected(src, s -> s.toMap(x -> x, x -> x, () -> (Map<Short, Short>) null), "mapFactory returned null");
            assertNullFactoryRejected(src, s -> s.groupTo(x -> x, Collectors.counting(), () -> (Map<Short, Long>) null), "mapFactory returned null");
        }
    }

    @Test
    public void testC016_floatNullFactories() {
        for (final Supplier<FloatStream> src : floatSources()) {
            assertNullFactoryRejected(src, s -> s.toCollection(() -> (List<Float>) null), "supplier returned null");
            assertNullFactoryRejected(src, s -> s.toMultiset(() -> (Multiset<Float>) null), "supplier returned null");
            assertNullFactoryRejected(src, s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> (Map<Float, Float>) null), "mapFactory returned null");
            assertNullFactoryRejected(src, s -> s.toMap(x -> x, x -> x, () -> (Map<Float, Float>) null), "mapFactory returned null");
            assertNullFactoryRejected(src, s -> s.groupTo(x -> x, Collectors.counting(), () -> (Map<Float, Long>) null), "mapFactory returned null");
        }
    }

    @Test
    public void testC016_doubleNullFactories() {
        for (final Supplier<DoubleStream> src : doubleSources()) {
            assertNullFactoryRejected(src, s -> s.toCollection(() -> (List<Double>) null), "supplier returned null");
            assertNullFactoryRejected(src, s -> s.toMultiset(() -> (Multiset<Double>) null), "supplier returned null");
            assertNullFactoryRejected(src, s -> s.toMap(x -> x, x -> x, (a, b) -> a, () -> (Map<Double, Double>) null), "mapFactory returned null");
            assertNullFactoryRejected(src, s -> s.toMap(x -> x, x -> x, () -> (Map<Double, Double>) null), "mapFactory returned null");
            assertNullFactoryRejected(src, s -> s.groupTo(x -> x, Collectors.counting(), () -> (Map<Double, Long>) null), "mapFactory returned null");
        }
    }

    @Test
    public void testC016_nonNullFactoriesStillWork() {
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 1), ShortStream.of((short) 1, (short) 2, (short) 1).map(x -> x).toCollection(ArrayList::new));
        assertEquals(2, FloatStream.of(1f, 2f, 1f).toMultiset(Multiset::new).count(1f));
        assertEquals(new ArrayList<Double>(), DoubleStream.empty().toCollection(ArrayList::new));
        assertEquals(1, DoubleStream.of(1d, 2d, 1d).map(x -> x).groupTo(x -> x, Collectors.counting(), java.util.TreeMap::new).get(2d));
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-049 zip(Collection, NFunction) returning null -> NPE
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC049_zipCollectionNullResultRejected() {
        final AtomicInteger closed = new AtomicInteger();

        final Collection<ShortStream> shorts = Arrays.asList(ShortStream.of((short) 1).onClose(closed::incrementAndGet),
                ShortStream.of((short) 2).onClose(closed::incrementAndGet));
        final NullPointerException e = assertThrows(NullPointerException.class, () -> ShortStream.zip(shorts, a -> null).toArray());
        assertEquals("zipFunction returned null", e.getMessage());
        assertEquals(2, closed.get());

        final Collection<ShortStream> shorts2 = Arrays.asList(ShortStream.of((short) 1), ShortStream.of((short) 2, (short) 3));
        assertThrows(NullPointerException.class, () -> ShortStream.zip(shorts2, new short[] { 0, 0 }, a -> a[1] == 3 ? null : a[0]).toArray());

        final Collection<FloatStream> floats = Arrays.asList(FloatStream.of(1f), FloatStream.of(2f));
        assertThrows(NullPointerException.class, () -> FloatStream.zip(floats, a -> null).toArray());
        final Collection<FloatStream> floats2 = Arrays.asList(FloatStream.of(1f), FloatStream.empty());
        assertThrows(NullPointerException.class, () -> FloatStream.zip(floats2, new float[] { 0f, 0f }, a -> null).toList());

        final Collection<DoubleStream> doubles = Arrays.asList(DoubleStream.of(1d), DoubleStream.of(2d));
        assertThrows(NullPointerException.class, () -> DoubleStream.zip(doubles, a -> null).sum());
        final Collection<DoubleStream> doubles2 = Arrays.asList(DoubleStream.of(1d, 5d), DoubleStream.of(2d));
        assertThrows(NullPointerException.class, () -> DoubleStream.zip(doubles2, new double[] { 0d, 0d }, a -> a[0] == 5d ? null : a[0]).toArray());
    }

    @Test
    public void testC049_zipCollectionNonNullResultsUnchanged() {
        assertArrayEquals(new short[] { 11, 22 },
                ShortStream.zip(Arrays.asList(ShortStream.of((short) 1, (short) 2), ShortStream.of((short) 10, (short) 20)), a -> (short) (a[0] + a[1]))
                        .toArray());
        assertArrayEquals(new float[] { 11f, 2f },
                FloatStream.zip(Arrays.asList(FloatStream.of(1f, 2f), FloatStream.of(10f)), new float[] { 0f, 0f }, a -> a[0] + a[1]).toArray());
        assertArrayEquals(new double[] { Double.NaN, -0.0 },
                DoubleStream.zip(Arrays.asList(DoubleStream.of(Double.NaN, -0.0), DoubleStream.of(1d, 2d)), a -> a[0]).toArray());
        assertArrayEquals(new double[0], DoubleStream.zip(Arrays.asList(DoubleStream.empty(), DoubleStream.of(1d)), a -> null).toArray());
    }

    // ------------------------------------------------------------------------------------------------------------
    // C-034 (Short) iterator-backed sum()/average() - regression lock; the wrap itself needs about 2^48 elements
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC034_shortIteratorSumAndAverageBoundaries_regressionLock() {
        assertEquals(2147483646, ShortStream.repeat(Short.MAX_VALUE, 65538).sum());
        assertThrows(ArithmeticException.class, () -> ShortStream.repeat(Short.MAX_VALUE, 65539).sum());
        assertEquals(Integer.MIN_VALUE, ShortStream.repeat(Short.MIN_VALUE, 65536).sum());
        assertThrows(ArithmeticException.class, () -> ShortStream.repeat(Short.MIN_VALUE, 65537).sum());

        // The partial sum may leave the int range as long as the total comes back.
        assertEquals(0, ShortStream.concat(ShortStream.repeat(Short.MAX_VALUE, 70000), ShortStream.repeat((short) -Short.MAX_VALUE, 70000)).sum());

        assertEquals(OptionalDouble.of(Short.MIN_VALUE), ShortStream.repeat(Short.MIN_VALUE, 100_000).average());
        assertEquals(OptionalDouble.of(Short.MAX_VALUE), ShortStream.repeat(Short.MAX_VALUE, 100_000).average());
        assertEquals(OptionalDouble.of(-0.5), ShortStream.of(Short.MIN_VALUE, Short.MAX_VALUE).map(x -> x).average());
        assertEquals(OptionalDouble.empty(), ShortStream.empty().map(x -> x).average());
        assertEquals(0, ShortStream.empty().map(x -> x).sum());
    }

    // ------------------------------------------------------------------------------------------------------------
    // Doc-only regression locks (C-043, C-044, C-045, C-046, C-047, C-063, P2-05, P5-07)
    // ------------------------------------------------------------------------------------------------------------

    @Test
    public void testC044_doubleSumAverageSummaryStatistics_docLock() {
        assertEquals(1.0000000000000002E16, DoubleStream.of(1e16, 1.0, 1.0).sum(), 0.0);
        assertEquals(1.0000000000000002E16, DoubleStream.of(1e16, 1.0, 1.0).summaryStatistics().getSum(), 0.0);
        assertEquals(1.0E16, DoubleStream.of(1e16, 1.0, 1.0).reduce(0.0, Double::sum), 0.0);
        assertEquals(0.0, DoubleStream.of(1.0, 1e100, 1.0, -1e100).sum(), 0.0);

        assertEquals(Double.POSITIVE_INFINITY, DoubleStream.of(MAX, MAX).sum(), 0.0);
        assertEquals(OptionalDouble.of(MAX), DoubleStream.of(MAX, MAX).average());
        assertEquals(Double.POSITIVE_INFINITY, DoubleStream.of(MAX, MAX).summaryStatistics().getAverage(), 0.0);

        assertEquals(Double.POSITIVE_INFINITY, DoubleStream.of(MAX, MAX, -MAX, -MAX).sum(), 0.0);
        assertEquals(OptionalDouble.of(0.0), DoubleStream.of(MAX, MAX, -MAX, -MAX).average());
        assertEquals(Double.POSITIVE_INFINITY, DoubleStream.of(MAX, MAX, -MAX).sum(), 0.0);
        assertEquals(MAX, DoubleStream.of(MAX, -MAX, MAX).sum(), 0.0);
    }

    @Test
    public void testC043_doubleAverageNonFiniteOrderDependence_docLock() {
        assertTrue(Double.isNaN(DoubleStream.of(MAX, MAX, Double.NEGATIVE_INFINITY).average().get()));
        assertTrue(Double.isNaN(DoubleStream.of(MAX, MAX, Double.NEGATIVE_INFINITY).map(x -> x).average().get()));
        assertEquals(OptionalDouble.of(Double.NEGATIVE_INFINITY), DoubleStream.of(Double.NEGATIVE_INFINITY, MAX, MAX).average());
        assertEquals(java.util.stream.DoubleStream.of(MAX, MAX, Double.NEGATIVE_INFINITY).average().getAsDouble(),
                DoubleStream.of(MAX, MAX, Double.NEGATIVE_INFINITY).average().get(), 0.0);
    }

    @Test
    public void testC045_mergeSelectorsInCompareTerms_docLock() {
        final float[] a = { 2f, 3f };
        final float[] b = { 1f, Float.NaN };

        assertArrayEquals(new float[] { 1f, Float.NaN, 2f, 3f },
                FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new float[] { 1f, 2f, 3f, Float.NaN },
                FloatStream.merge(a, b, (x, y) -> Float.compare(x, y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        assertArrayEquals(new double[] { 1d, 2d, 3d, Double.NaN }, DoubleStream.of(2d, 3d)
                .mergeWith(DoubleStream.of(1d, Double.NaN), (x, y) -> Double.compare(x, y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray());
        assertArrayEquals(new float[] { -0.0f, 0.0f, 1f, 2f }, FloatStream.of(-0.0f, 1f)
                .mergeWith(FloatStream.of(0.0f, 2f), (x, y) -> Float.compare(x, y) < 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                .toArray());
    }

    @Test
    public void testC046_shortSumThreshold_docLock() {
        assertEquals(2147516413L, ShortStream.repeat(Short.MAX_VALUE, 65539).summaryStatistics().getSum());
        assertThrows(ArithmeticException.class, () -> ShortStream.of(new short[65539]).map(x -> Short.MAX_VALUE).sum());
        final short[] maxes = new short[65539];
        Arrays.fill(maxes, Short.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> ShortStream.of(maxes).sum());
        assertEquals(500500, ShortStream.range((short) 1, (short) 1001).sum());
    }

    @Test
    public void testC047_boxedEqualsInIntersectionAndDifference_docLock() {
        assertEquals(0, ShortStream.of((short) 1, (short) 2, (short) 3).intersection(Arrays.asList(1, 2)).count());
        assertArrayEquals(new short[] { 1, 2, 3 }, ShortStream.of((short) 1, (short) 2, (short) 3).difference(Arrays.asList(1, 2)).toArray());
        assertArrayEquals(new short[] { 1, 2 }, ShortStream.of((short) 1, (short) 2, (short) 3).intersection(Arrays.asList((short) 1, (short) 2)).toArray());

        assertEquals(0, FloatStream.of(1.5f, 2.5f).intersection(Arrays.asList(1.5, 2.5)).count());
        assertArrayEquals(new float[] { 1.5f, 2.5f }, FloatStream.of(1.5f, 2.5f).intersection(Arrays.asList(1.5f, 2.5f)).toArray());
        assertArrayEquals(new float[] { Float.NaN }, FloatStream.of(Float.NaN, -0.0f).intersection(Arrays.asList(Float.NaN, 0.0f)).toArray());

        assertEquals(0, DoubleStream.of(1.0, 2.0, 3.0).intersection(Arrays.asList(1, 2)).count());
        assertArrayEquals(new double[] { 3.0 }, DoubleStream.of(1.0, 2.0, 3.0).difference(Arrays.asList(1.0, 2.0)).toArray());
    }

    @Test
    public void testC063_mapPartialNullOptionalIsNpe_docLock() {
        assertThrows(NullPointerException.class, () -> ShortStream.of((short) 1).mapPartial(x -> null).toArray());
        assertThrows(NullPointerException.class, () -> FloatStream.of(1f).mapPartial(x -> null).toArray());
        assertThrows(NullPointerException.class, () -> DoubleStream.of(1d).mapPartial(x -> null).toArray());
        assertThrows(NullPointerException.class, () -> DoubleStream.of(1d).mapPartialJdk(x -> null).toArray());
    }

    @Test
    public void testP205_floatDistinctToSetAndSummaryStatisticsNaN_docLock() {
        final float otherNaN = Float.intBitsToFloat(0x7fc00001);

        assertEquals(3, FloatStream.of(Float.NaN, otherNaN, 0.0f, -0.0f).distinct().count());
        assertEquals(3, FloatStream.of(Float.NaN, otherNaN, 0.0f, -0.0f).toSet().size());

        final com.landawn.abacus.util.FloatSummaryStatistics stats = FloatStream.of(1f, Float.NaN, 3f).summaryStatistics();
        assertTrue(Float.isNaN(stats.getMin()));
        assertTrue(Float.isNaN(stats.getMax()));
        assertTrue(Double.isNaN(stats.getSum()));
        assertTrue(Double.isNaN(stats.getAverage()));
        assertEquals(3, stats.getCount());
    }

    @Test
    public void testP507_doubleToMapExampleContent_docLock() {
        final Map<String, Double> firstValues = DoubleStream.of(85.5, 92.3, 85.7, 78.9).toMap(d -> "Range-" + ((int) d / 10) * 10, d -> d, (v1, v2) -> v1);

        assertEquals(3, firstValues.size());
        assertEquals(85.5, firstValues.get("Range-80"), 0.0);
        assertEquals(92.3, firstValues.get("Range-90"), 0.0);
        assertEquals(78.9, firstValues.get("Range-70"), 0.0);
    }
}
