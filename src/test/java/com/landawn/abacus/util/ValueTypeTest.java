package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Range.BoundType;
import com.landawn.abacus.util.Range.LowerEndpoint;
import com.landawn.abacus.util.Range.UpperEndpoint;

public class ValueTypeTest extends TestBase {

    private static long[] closestBounded(final double v) {
        long bestN = 0;
        long bestD = 1;
        double bestErr = Math.abs(v);
        for (int d = 1; d <= 10_000; d++) {
            final long n = Math.round(v * d);
            final double err = Math.abs((double) n / d - v);
            if (err < bestErr - 1e-18) {
                bestErr = err;
                bestN = n;
                bestD = d;
            }
        }
        return new long[] { bestN, bestD };
    }

    private static void assertOptimalApproximation(final double v) {
        final Fraction actual = Fraction.of(v);
        final long[] expected = closestBounded(v);
        final double actualErr = Math.abs(actual.doubleValue() - v);
        final double bestErr = Math.abs((double) expected[0] / expected[1] - v);
        assertTrue(actualErr <= bestErr * (1 + 1e-9) || actualErr <= 1e-15, () -> "of(" + v + ") returned " + actual + " (err " + actualErr
                + "); closest bounded fraction is " + expected[0] + "/" + expected[1] + " (err " + bestErr + ")");
    }

    private static Object serializeRoundTrip(final Serializable o) throws Exception {
        final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return in.readObject();
        }
    }

    private static Fraction rawFraction(final int numerator, final int denominator) throws Exception {
        final Constructor<Fraction> ctor = Fraction.class.getDeclaredConstructor(int.class, int.class);
        ctor.setAccessible(true);
        return ctor.newInstance(numerator, denominator);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static <T extends Comparable<? super T>> Range<T> rawRange(final T min, final boolean minClosed, final T max, final boolean maxClosed,
            final BoundType boundType) throws Exception {
        final Constructor<Range> ctor = Range.class.getDeclaredConstructor(LowerEndpoint.class, UpperEndpoint.class, BoundType.class);
        ctor.setAccessible(true);
        return ctor.newInstance(new LowerEndpoint<>(min, minClosed), new UpperEndpoint<>(max, maxClosed), boundType);
    }

    @Test
    public void testFractionOfDouble() {
        assertEquals(Fraction.of(9999, 10000), Fraction.of(0.99991));
        assertEquals(Fraction.of(9999, 10000), Fraction.of(0.9999005));
        assertEquals(Fraction.of(1, 10000), Fraction.of(1.0 / 10001));
        assertEquals(Fraction.of(1000000000, 1), Fraction.of(1000000000.25));
        assertEquals(1, Fraction.of(1000000000.25).denominator());
        assertEquals(Fraction.of(1, 10000), Fraction.of(0.00005));
        assertTrue(Fraction.of(4.9e-5).isZero());
        assertTrue(Fraction.of(1e-9).isZero());
        assertTrue(Fraction.of(Double.MIN_VALUE).isZero());
        assertOptimalApproximation(4.9e-5);
        assertOptimalApproximation(1e-9);
        assertFalse(Fraction.of(5.1e-5).isZero());
        assertEquals(Fraction.of(1, 10000), Fraction.of(5.1e-5));
        assertEquals(Fraction.of(355, 113), Fraction.of(Math.PI));
        assertOptimalApproximation(Math.E);
        assertOptimalApproximation(Math.sqrt(2));
        assertEquals(Fraction.of(1, 2), Fraction.of(0.5));
        assertEquals(Fraction.of(3, 4), Fraction.of(0.75));
        assertEquals(Fraction.of(1, 10), Fraction.of(0.1));
        assertEquals(Fraction.of(1, 5), Fraction.of(0.2));
        assertEquals(Fraction.of(333, 1000), Fraction.of(0.333));
        assertEquals(Fraction.of(1, 3), Fraction.of(1.0 / 3));
        assertEquals(Fraction.of(1, 7), Fraction.of(1.0 / 7));
        assertEquals(Fraction.of(0, 1), Fraction.of(0.0));
        assertEquals(Fraction.of(0, 1), Fraction.of(-0.0));
        assertEquals(Fraction.of(-9999, 10000), Fraction.of(-0.99991));
        assertEquals(Fraction.of(29999, 10000), Fraction.of(2.99991));
        assertEquals(Fraction.of(-29999, 10000), Fraction.of(-2.99991));
        assertEquals(Fraction.of(Integer.MIN_VALUE, 1), Fraction.of(Integer.MIN_VALUE));

        double worst = 0;
        for (int i = 1; i <= 200_000; i++) {
            final double v = i / 1.0e5;
            worst = Math.max(worst, Math.abs(Fraction.of(v).doubleValue() - v));
        }
        assertTrue(worst <= 5.0e-5 + 1e-12, "worst observed error " + worst);
        for (int i = 1; i < 5000; i++) {
            assertTrue(Fraction.of(i / 1.0e9).isZero());
            assertTrue(Fraction.of(-i / 1.0e9).isZero());
        }
        assertFalse(Fraction.of(5.0e-5 + 1e-9).isZero());

        final Random rnd = new Random(5L);
        for (int i = 0; i < 5000; i++) {
            final double v = rnd.nextDouble() * 200 - 100;
            final Fraction f = Fraction.of(v);
            assertEquals(f, f.reduce(), () -> "not reduced: of(" + v + ") = " + f);
        }
        final Random boundRnd = new Random(20260901L);
        for (int i = 0; i < 20_000; i++) {
            final double v = boundRnd.nextDouble() * 2000 - 1000;
            assertTrue(Fraction.of(v).denominator() <= 10_000, () -> "denominator bound broken for " + v);
        }
        for (int i = 1; i <= 4000; i++) {
            assertOptimalApproximation(i / 1.0e5);
            assertOptimalApproximation(1 - i / 2.0e6);
            assertOptimalApproximation(i / 10001.0);
        }
        final Random sampleRnd = new Random(42L);
        for (int i = 0; i < 4000; i++) {
            assertOptimalApproximation(sampleRnd.nextDouble());
            assertOptimalApproximation(sampleRnd.nextDouble() * 1000);
        }

        assertThrows(ArithmeticException.class, () -> Fraction.of(Double.NaN));
        assertThrows(ArithmeticException.class, () -> Fraction.of(Double.POSITIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> Fraction.of(Double.NEGATIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> Fraction.of(Integer.MAX_VALUE + 1.0));
        assertEquals(Fraction.of(2147483646, 1), Fraction.of(2147483646.5));
        assertEquals(Integer.MAX_VALUE, Fraction.of(1073741823.5).numerator());
        assertEquals(2, Fraction.of(1073741823.5).denominator());
        assertEquals(Fraction.of(1, 9091), Fraction.of(1.1e-4));
        assertEquals(Fraction.of(1, 6667), Fraction.of(1.5e-4));
        assertEquals(Fraction.of(1, 4000), Fraction.of(2.5e-4));
        assertOptimalApproximation(1.1e-4);
        assertOptimalApproximation(1.5e-4);
        assertEquals(Fraction.of(0, 1), Fraction.of(1.0e-5));
        assertEquals(Fraction.of(1, 10000), Fraction.of(6.0e-5));
        assertOptimalApproximation(1.0e-5);
        assertOptimalApproximation(6.0e-5);
        assertEquals(Fraction.of(1000009999, 10000), Fraction.of(100000.99991));
        assertEquals(Fraction.of(2000001, 1), Fraction.of(2000000.99991));
        assertEquals(Fraction.of(2147407157, 7158), Fraction.of(300000.99991));
        assertEquals(Fraction.of(-2000001, 1), Fraction.of(-2000000.99991));
        final Fraction overflowFallback = Fraction.of(474798.4647483182);
        assertEquals(Fraction.of(329984933, 695), overflowFallback);
        assertTrue(overflowFallback.denominator() <= 10_000);
        assertTrue(Math.abs(overflowFallback.doubleValue() - 474798.4647483182) < 1e-6);
        for (final double v : new double[] { 2000000.99991, 300000.99991, 214749.99991, 214748.99991 }) {
            final Fraction g = Fraction.of(v);
            assertTrue(g.denominator() <= 10_000);
            assertTrue(Math.abs(g.doubleValue() - v) <= Math.abs(Math.rint(v) - v) + 1e-9, () -> "worse than rounding for " + v);
        }
    }

    @Test
    public void testFractionParseAndOfMixed() {
        assertEquals(Fraction.of(-1, 2), Fraction.of("-0 1/2"));
        assertEquals(Fraction.of(-3, 4), Fraction.of("-0 3/4"));
        assertEquals(-0.5, Fraction.of("-0 1/2").doubleValue());
        assertEquals(Fraction.of(1, 2), Fraction.of("0 1/2"));
        assertEquals(Fraction.of(1, 2), Fraction.of("+0 1/2"));
        final Fraction unreduced = Fraction.of("-0 2/4");
        assertEquals(-2, unreduced.numerator());
        assertEquals(4, unreduced.denominator());
        assertTrue(Fraction.of("-0 0/2").isZero());
        assertTrue(Fraction.of("-0").isZero());
        assertEquals(Fraction.of(-3, 2), Fraction.of("-1 1/2"));
        assertEquals(Fraction.of(-7, 3), Fraction.of("-2 1/3"));
        assertEquals(Fraction.of(7, 4), Fraction.of("1 3/4"));
        assertEquals(Fraction.of(5, 3), Fraction.of("1 2/3"));
        assertEquals(Fraction.of(-1, 2), Fraction.of("-1/2"));
        assertEquals(Fraction.of(3, 4), Fraction.of("3/4"));
        assertEquals(Fraction.of(-5, 1), Fraction.of("-5"));
        assertEquals(Fraction.of(1, 4), Fraction.of("0.25"));
        assertThrows(NumberFormatException.class, () -> Fraction.of(" 3"));
        assertThrows(ArithmeticException.class, () -> Fraction.of("2 -1/3"));
        final Fraction half = Fraction.of(-1, 2);
        assertEquals("-1/2", half.toProperString());
        assertEquals(0, Fraction.of(half.toProperString()).compareTo(half));

        assertEquals(Fraction.of(1, 2), Fraction.ofMixed(0, 1, 2));
        assertEquals(Fraction.of(1, 2), Fraction.ofMixed(-0, 1, 2));
        assertTrue(Fraction.ofMixed(0, 1, 2).isPositive());
        assertEquals(-0.5, Fraction.of(-1, 2).doubleValue());
        assertEquals(-0.5, Fraction.of("-0 1/2").doubleValue());
    }

    @Test
    public void testFractionOrderingAndArithmetic() {
        final List<Fraction> both = List.of(Fraction.of(1, 2), Fraction.of(2, 4));
        assertEquals(1, new TreeSet<>(both).size());
        assertEquals(2, new HashSet<>(both).size());
        assertEquals(0, Fraction.of(1, 2).compareTo(Fraction.of(2, 4)));
        assertNotEquals(Fraction.of(1, 2), Fraction.of(2, 4));
        final Fraction sum = Fraction.of(1, 2).add(Fraction.of(2, 4));
        assertEquals("1/1", sum.toString());
        assertEquals(Fraction.ONE, sum);
        assertEquals(0, sum.compareTo(Fraction.ONE));
        assertEquals(Fraction.ONE, sum.reduce());
        assertEquals("5/6", Fraction.of(1, 2).add(Fraction.of(1, 3)).toString());
        assertEquals("1/2", Fraction.of(2, 3).multipliedBy(Fraction.of(3, 4)).toString());
        assertEquals("3/2", Fraction.of(3, 4).dividedBy(Fraction.of(1, 2)).toString());
    }

    @Test
    public void testDeserializationValidatesInvariants() throws Exception {
        final Fraction f = Fraction.of(-3, 4);
        assertEquals(f, serializeRoundTrip(f));
        assertEquals(Fraction.ZERO, serializeRoundTrip(Fraction.ZERO));
        assertThrows(InvalidObjectException.class, () -> serializeRoundTrip(rawFraction(0, 0)));
        assertThrows(InvalidObjectException.class, () -> serializeRoundTrip(rawFraction(1, -2)));

        for (final Range<Integer> r : List.of(Range.closed(1, 5), Range.open(1, 5), Range.closedOpen(1, 5), Range.openClosed(1, 5), Range.just(7))) {
            final Object back = serializeRoundTrip(r);
            assertEquals(r, back);
            assertEquals(r.boundType(), ((Range<?>) back).boundType());
        }
        assertThrows(InvalidObjectException.class, () -> serializeRoundTrip(rawRange(3, true, 2, true, BoundType.CLOSED_CLOSED)));
        assertThrows(InvalidObjectException.class, () -> serializeRoundTrip(rawRange(1, true, 5, true, BoundType.OPEN_OPEN)));
        assertThrows(InvalidObjectException.class, () -> serializeRoundTrip(rawRange(null, true, 5, true, BoundType.CLOSED_CLOSED)));
        assertThrows(InvalidObjectException.class, () -> serializeRoundTrip(rawRange(1, true, null, true, BoundType.CLOSED_CLOSED)));

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Constructor<Range> ctor = Range.class.getDeclaredConstructor(LowerEndpoint.class, UpperEndpoint.class, BoundType.class);
        ctor.setAccessible(true);
        @SuppressWarnings("rawtypes")
        final Range<?> bad = ctor.newInstance(new LowerEndpoint<>(1, true), new UpperEndpoint<>("z", true), BoundType.CLOSED_CLOSED);
        assertThrows(InvalidObjectException.class, () -> serializeRoundTrip(bad));

        for (final Range<Integer> r : List.of(Range.closed(5, 5), Range.closedOpen(5, 5), Range.open(5, 5), Range.openClosed(5, 5))) {
            final Object back = serializeRoundTrip(r);
            assertEquals(r, back);
            assertEquals(r.isEmpty(), ((Range<?>) back).isEmpty());
        }
        assertEquals(Range.closed("a", "z"), serializeRoundTrip(Range.closed("a", "z")));
        assertEquals(Range.closed(1.0, Double.NaN), serializeRoundTrip(Range.closed(1.0, Double.NaN)));
    }

    @Test
    public void testRangeEndpointAlgebra() {
        assertTrue(Range.open(5, 5).isEmpty());
        assertTrue(Range.closedOpen(5, 5).isEmpty());
        assertTrue(Range.openClosed(5, 5).isEmpty());
        assertFalse(Range.closed(5, 5).isEmpty());
        assertFalse(Range.open(5, 6).isEmpty());
        final Range<Integer> gap = Range.open(5, 6);
        assertFalse(gap.contains(5));
        assertFalse(gap.contains(6));
        assertTrue(gap.overlaps(gap));
        assertTrue(gap.containsRange(gap));

        final Range<Integer> base = Range.closed(1, 5);
        assertTrue(base.overlaps(Range.closed(5, 10)));
        assertFalse(base.overlaps(Range.openClosed(5, 10)));
        assertFalse(Range.closedOpen(1, 5).overlaps(Range.closed(5, 10)));
        assertTrue(Range.closed(5, 5).overlaps(Range.closed(5, 5)));
        assertFalse(base.overlaps(Range.open(3, 3)));
        assertFalse(base.overlaps(null));

        for (final Range<Integer> empty : List.of(Range.closedOpen(5, 5), Range.open(5, 5), Range.openClosed(5, 5))) {
            assertTrue(empty.isAfterRange(empty), () -> empty + " should be after itself");
            assertTrue(empty.isBeforeRange(empty), () -> empty + " should be before itself");
        }
        final Range<Integer> closed = Range.closed(1, 2);
        assertTrue(closed.isBeforeRange(Range.closedOpen(100, 100)));
        assertFalse(closed.isAfterRange(Range.closedOpen(100, 100)));
        assertTrue(closed.isAfterRange(Range.closedOpen(0, 0)));
        assertFalse(closed.isBeforeRange(Range.closedOpen(0, 0)));

        final List<Range<Integer>> ranges = new ArrayList<>();
        for (final int lo : new int[] { 0, 1, 2, 3 }) {
            for (final int hi : new int[] { 0, 1, 2, 3 }) {
                if (lo <= hi) {
                    ranges.add(Range.open(lo, hi));
                    ranges.add(Range.openClosed(lo, hi));
                    ranges.add(Range.closedOpen(lo, hi));
                    ranges.add(Range.closed(lo, hi));
                }
            }
        }
        for (final Range<Integer> a : ranges) {
            for (final Range<Integer> b : ranges) {
                if (!a.isEmpty() && !b.isEmpty()) {
                    assertFalse(a.isBeforeRange(b) && a.isAfterRange(b), () -> a + " vs " + b);
                }
            }
        }
    }

    @Test
    public void testTuple0AndToList() {
        final Tuple<?> empty = Tuple.fromArray(new Object[0]);
        assertInstanceOf(Tuple.Tuple0.class, empty);
        assertEquals(0, empty.arity());
        assertTrue(java.lang.reflect.Modifier.isPublic(Tuple.Tuple0.class.getModifiers()));
        assertSame(empty, Tuple.fromArray(null));
        assertSame(empty, Tuple.fromCollection(null));
        assertSame(empty, Tuple.fromCollection(List.of()));

        final List<String> list = Tuple.toList(Tuple.of("a", "b", "c"));
        list.add("d");
        list.set(0, "z");
        assertEquals(List.of("z", "b", "c", "d"), list);
        final Tuple.Tuple3<String, String, String> t = Tuple.of("a", "b", "c");
        Tuple.toList(t).clear();
        assertEquals("a", t._1);
        assertEquals("b", t._2);
        assertEquals("c", t._3);
        assertEquals(3, Tuple.toList(t).size());

        final int[] left = { 1, 2 };
        final int[] right = { 1, 2 };
        final Tuple.Tuple2<int[], String> a = Tuple.of(left, "a");
        final Tuple.Tuple2<int[], String> b = Tuple.of(right, "a");
        assertEquals(a.toString(), b.toString());
        assertEquals("([1, 2], a)", a.toString());
        assertNotEquals(a, b);
        assertTrue(CommonUtil.deepEquals(left, right));
        assertEquals(a, Tuple.of(left, "a"));
    }

    @Test
    public void testDurationPartsAndBetweenTruncation() {
        final long h = 3_600_000L;
        final long m = 60_000L;
        final long d = 86_400_000L;
        assertEquals(0, Duration.ZERO.toDaysPart());
        assertEquals(0, Duration.ZERO.toHoursPart());
        assertEquals(0, Duration.ZERO.toMinutesPart());
        assertEquals(0, Duration.ZERO.toSecondsPart());
        assertEquals(0, Duration.ZERO.toMillisPart());
        final Duration composite = Duration.ofMillis(d + 2 * h + 3 * m + 4_000 + 5);
        assertEquals(1, composite.toDaysPart());
        assertEquals(2, composite.toHoursPart());
        assertEquals(3, composite.toMinutesPart());
        assertEquals(4, composite.toSecondsPart());
        assertEquals(5, composite.toMillisPart());
        final Duration negative = Duration.ofMillis(-(d + 2 * h + 3 * m + 4_000 + 5));
        assertEquals(-1, negative.toDaysPart());
        assertEquals(-2, negative.toHoursPart());
        assertEquals(-3, negative.toMinutesPart());
        assertEquals(-4, negative.toSecondsPart());
        assertEquals(-5, negative.toMillisPart());
        assertEquals(0, Duration.ofMillis(24 * h).toHoursPart());
        assertEquals(23, Duration.ofMillis(23 * h).toHoursPart());
        assertEquals(1, Duration.ofMillis(25 * h).toHoursPart());
        assertEquals(0, Duration.ofMillis(60 * m).toMinutesPart());
        assertEquals(59, Duration.ofMillis(59 * m).toMinutesPart());
        assertEquals(1, Duration.ofMillis(61 * m).toMinutesPart());
        final Random rnd = new Random(20260901L);
        for (int i = 0; i < 20_000; i++) {
            final Duration duration = Duration.ofMillis(rnd.nextLong());
            assertTrue(Math.abs(duration.toHoursPart()) <= 23, () -> "hoursPart " + duration.toHoursPart());
            assertTrue(Math.abs(duration.toMinutesPart()) <= 59, () -> "minutesPart " + duration.toMinutesPart());
            assertTrue(Math.abs(duration.toSecondsPart()) <= 59, () -> "secondsPart " + duration.toSecondsPart());
            assertTrue(Math.abs(duration.toMillisPart()) <= 999, () -> "millisPart " + duration.toMillisPart());
            assertEquals(duration.toDays(), duration.toDaysPart());
        }
        final Duration max = Duration.ofMillis(Long.MAX_VALUE);
        final Duration min = Duration.ofMillis(Long.MIN_VALUE);
        assertEquals(max.toDays(), max.toDaysPart());
        assertEquals(min.toDays(), min.toDaysPart());
        assertTrue(Math.abs(max.toHoursPart()) <= 23);
        assertTrue(Math.abs(min.toHoursPart()) <= 23);
        assertTrue(Math.abs(max.toMinutesPart()) <= 59);
        assertTrue(Math.abs(min.toMinutesPart()) <= 59);
        assertEquals(-106751991167L, min.toDaysPart());
        assertEquals(106751991167L, max.toDaysPart());

        final java.time.Instant t0 = java.time.Instant.ofEpochSecond(0, 0);
        assertEquals(1, Duration.between(t0, t0.plusNanos(1_500_000)).toMillis());
        assertEquals(1, Duration.between(t0, t0.plusNanos(1_999_999)).toMillis());
        assertEquals(0, Duration.between(t0, t0.plusNanos(999_999)).toMillis());
        assertEquals(1, Duration.between(t0, t0.plusNanos(1_000_000)).toMillis());
        assertEquals(-1, Duration.between(t0, t0.plusNanos(-1_999_999)).toMillis());
        assertEquals(0, Duration.between(t0, t0.plusNanos(-999_999)).toMillis());
        assertEquals(-1, Duration.between(t0, t0.plusNanos(-1_500_000)).toMillis());
        final java.time.Instant s = java.time.Instant.parse("2024-01-01T00:00:00.000Z");
        final java.time.Instant e = java.time.Instant.parse("2024-01-01T00:00:00.000999Z");
        assertTrue(Duration.between(s, e).isZero());
        assertEquals(Duration.ZERO, Duration.between(s, e));
        assertEquals(999_000L, java.time.Duration.between(s, e).toNanos());
        assertEquals(250, Duration.between(new java.util.Date(1000), new java.util.Date(1250)).toMillis());
        final java.util.Calendar c1 = java.util.Calendar.getInstance();
        final java.util.Calendar c2 = java.util.Calendar.getInstance();
        c1.setTimeInMillis(1000);
        c2.setTimeInMillis(1250);
        assertEquals(250, Duration.between(c1, c2).toMillis());
        final java.time.LocalDateTime ldt = java.time.LocalDateTime.of(2024, 1, 1, 0, 0, 0, 0);
        assertEquals(1, Duration.between(ldt, ldt.plusNanos(1_500_000)).toMillis());
        final java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse("2024-01-01T00:00:00Z");
        assertEquals(1, Duration.between(odt, odt.plusNanos(1_500_000)).toMillis());
        final java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse("2024-01-01T00:00:00Z");
        assertEquals(1, Duration.between(zdt, zdt.plusNanos(1_500_000)).toMillis());
        final java.time.LocalTime lt = java.time.LocalTime.of(0, 0, 0, 0);
        assertEquals(1, Duration.between(lt, lt.plusNanos(1_500_000)).toMillis());
        assertThrows(java.time.DateTimeException.class, () -> Duration.between(java.time.LocalDate.of(2024, 1, 1), java.time.LocalDate.of(2024, 1, 2)));
        assertThrows(java.time.DateTimeException.class, () -> Duration.between(t0, java.time.LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> Duration.between((java.time.temporal.Temporal) null, t0));
        assertThrows(IllegalArgumentException.class, () -> Duration.between(t0, (java.time.temporal.Temporal) null));
    }

    @Test
    public void testDurationToStringRoundTrips() {
        final long[] values = { 0, 1, -1, 999, -999, 1000, -1000, 59_999, -59_999, 60_000, -60_000, 90_500, -90_500, 3_600_000, -3_600_000, 86_400_000,
                -86_400_000, Long.MAX_VALUE, Long.MIN_VALUE };
        for (final long millis : values) {
            final Duration duration = Duration.ofMillis(millis);
            assertEquals(millis, java.time.Duration.parse(duration.toString()).toMillis(), () -> "round-trip failed for " + millis + " -> " + duration);
        }
        assertEquals("-PT1M30.500S", Duration.ofMillis(-90_500).toString());
        assertEquals("PT-1M-30.5S", java.time.Duration.ofMillis(-90_500).toString());
        assertEquals("-PT2562047788015H12M55.808S", Duration.ofMillis(Long.MIN_VALUE).toString());
        assertEquals("PT0.500S", Duration.ofMillis(500).toString());
        assertEquals("PT0.5S", java.time.Duration.ofMillis(500).toString());
        assertEquals("PT0.010S", Duration.ofMillis(10).toString());
        assertEquals("PT0.001S", Duration.ofMillis(1).toString());
    }

    @Test
    public void testRateLimiterRateRoundTrip() {
        for (final double rate : new double[] { 1e-300, 1e-9, 1e-6, 0.001, 1, 1000, 1e9, 1e18, 1e300 }) {
            assertEquals(rate, RateLimiter.create(rate).getRate(), () -> "rate " + rate);
        }
        assertEquals(Double.POSITIVE_INFINITY, RateLimiter.create(Double.POSITIVE_INFINITY).getRate());
        assertEquals(0.0, RateLimiter.create(Double.MIN_VALUE).getRate());
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(0.0));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(-1.0));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(Double.NaN));
    }

    @Test
    public void testOverloadPairsResolveForLambdas() {
        final Pair<String, Integer> p = Pair.of("a", 1);
        final List<String> seen = new ArrayList<>();
        p.accept((l, r) -> seen.add("bi:" + l + r));
        p.accept(w -> seen.add("whole:" + w));
        assertEquals(List.of("bi:a1", "whole:(a, 1)"), seen);
        assertEquals("a1", p.map((l, r) -> l + r));
        assertEquals("(a, 1)", p.map(Object::toString));
        assertTrue(p.filter((l, r) -> r == 1).isPresent());
        assertTrue(p.filter(w -> w.left().equals("a")).isPresent());
        assertFalse(p.filter((l, r) -> r == 2).isPresent());
        assertEquals("ab", Pair.of("a", "b").map(String::concat));

        final Triple<String, Integer, Boolean> t = Triple.of("a", 1, true);
        final List<String> triSeen = new ArrayList<>();
        t.accept((l, m, r) -> triSeen.add("tri:" + l + m + r));
        t.accept(w -> triSeen.add("whole:" + w));
        assertEquals(List.of("tri:a1true", "whole:(a, 1, true)"), triSeen);
        assertEquals("a1true", t.map((l, m, r) -> "" + l + m + r));
        assertEquals("(a, 1, true)", t.map(Object::toString));
        assertTrue(t.filter((l, m, r) -> r).isPresent());
        assertTrue(t.filter(w -> w.middle() == 1).isPresent());

        final Tuple.Tuple2<String, Integer> t2 = Tuple.of("a", 1);
        final Tuple.Tuple3<String, Integer, Boolean> t3 = Tuple.of("a", 1, true);
        final List<String> tupleSeen = new ArrayList<>();
        t2.accept((a, b) -> tupleSeen.add("bi:" + a + b));
        t2.accept(w -> tupleSeen.add("whole:" + w));
        t3.accept((a, b, c) -> tupleSeen.add("tri:" + a + b + c));
        t3.accept(w -> tupleSeen.add("whole3:" + w));
        assertEquals(List.of("bi:a1", "whole:(a, 1)", "tri:a1true", "whole3:(a, 1, true)"), tupleSeen);
        assertEquals("a1", t2.map((a, b) -> a + b));
        assertEquals("(a, 1)", t2.map(Object::toString));
        assertEquals("a1true", t3.map((a, b, c) -> "" + a + b + c));
        assertTrue(t2.filter((a, b) -> b == 1).isPresent());
        assertTrue(t3.filter((a, b, c) -> c).isPresent());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testResultConditionalHandlers() throws Exception {
        final Holder<String> seen = Holder.of(null);
        Result.<String, RuntimeException> failure(new IllegalStateException("boom"))
                .ifFailureOrElse(ex -> seen.setValue("failure:" + ex.getMessage()), val -> seen.setValue("success:" + val));
        assertEquals("failure:boom", seen.value());
        Result.<String, RuntimeException> success("ok")
                .ifFailureOrElse(ex -> seen.setValue("failure:" + ex.getMessage()), val -> seen.setValue("success:" + val));
        assertEquals("success:ok", seen.value());
        Result.<String, RuntimeException> success("ok").ifSuccessOrElse(val -> seen.setValue("success:" + val), ex -> seen.setValue("failure"));
        assertEquals("success:ok", seen.value());
        Result.<String, RuntimeException> failure(new IllegalStateException("boom"))
                .ifSuccessOrElse(val -> seen.setValue("success:" + val), ex -> seen.setValue("failure:" + ex.getMessage()));
        assertEquals("failure:boom", seen.value());
        assertTrue(Result.class.getMethod("ifFailureOrElse", Throwables.Consumer.class, Throwables.Consumer.class).isAnnotationPresent(Deprecated.class));
        assertFalse(Result.class.getMethod("ifSuccessOrElse", Throwables.Consumer.class, Throwables.Consumer.class).isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void testHolderErrorMessageTemplates() {
        final Holder<String> empty = Holder.of(null);
        assertEquals("User with id 42 not found",
                assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("User with id {} not found", 42)).getMessage());
        assertEquals("a 1 b 2", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("a {} b {}", 1, 2)).getMessage());
        assertEquals("1 2 3", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("{} {} {}", 1, 2, 3)).getMessage());
        assertEquals("1 2 3 4", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("{} {} {} {}", 1, 2, 3, 4)).getMessage());
        assertEquals("a 1", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("a %s", 1)).getMessage());
        assertEquals("a 1 b 2", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("a %s b %s", 1, 2)).getMessage());
        assertEquals("1 2 3", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("%s %s %s", 1, 2, 3)).getMessage());
        assertEquals("1 2 3 4", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("%s %s %s %s", 1, 2, 3, 4)).getMessage());
        assertEquals("1 and %s: [2]", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("{} and %s", 1, 2)).getMessage());
        assertEquals("%s and 1: [2]", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("%s and {}", 1, 2)).getMessage());
        assertEquals("not found: [42]", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("not found", 42)).getMessage());
        assertEquals("none: [1, 2]", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("none", 1, 2)).getMessage());
        assertEquals("1 only: [2]", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("{} only", 1, 2)).getMessage());
        assertEquals("null: [1]", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull(null, 1)).getMessage());
        assertEquals("v=null", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("v={}", (Object) null)).getMessage());
        assertEquals("{} stays put", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("{} stays put")).getMessage());
        assertEquals("json 1 here", assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull("json {} here", 1)).getMessage());
        final Holder<String> present = Holder.of("v");
        assertEquals("v", present.orElseThrowIfNull("missing {}", 1));
        assertEquals("v", present.orElseThrowIfNull("missing {} {}", 1, 2));
        assertEquals("v", present.orElseThrowIfNull("missing {} {} {}", 1, 2, 3));
        assertEquals("v", present.orElseThrowIfNull("missing {} {} {} {}", 1, 2, 3, 4));
    }
}
