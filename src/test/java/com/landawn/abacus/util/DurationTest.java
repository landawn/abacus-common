package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class DurationTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals(86400000L, Duration.ofDays(1).toMillis());
        assertEquals(Duration.ZERO, Duration.ofDays(0));
        assertTrue(Duration.ofDays(0).isZero());
        assertEquals(-86400000L, Duration.ofDays(-1).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofDays(Long.MAX_VALUE / 1000));
        assertThrows(ArithmeticException.class, () -> Duration.ofDays(Long.MAX_VALUE / 86400000L + 1));

        assertEquals(3600000L, Duration.ofHours(1).toMillis());
        assertEquals(Duration.ZERO, Duration.ofHours(0));
        assertEquals(-86400000L, Duration.ofHours(-24).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofHours(Long.MAX_VALUE / 1000));
        assertThrows(ArithmeticException.class, () -> Duration.ofHours(Long.MAX_VALUE / 3600000L + 1));

        assertEquals(60000L, Duration.ofMinutes(1).toMillis());
        assertEquals(Duration.ZERO, Duration.ofMinutes(0));
        assertEquals(-3600000L, Duration.ofMinutes(-60).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMinutes(Long.MAX_VALUE / 1000));

        assertEquals(1000L, Duration.ofSeconds(1).toMillis());
        assertEquals(Duration.ZERO, Duration.ofSeconds(0));
        assertEquals(-60000L, Duration.ofSeconds(-60).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofSeconds(Long.MAX_VALUE));

        assertEquals(1000L, Duration.ofMillis(1000).toMillis());
        assertEquals(Duration.ZERO, Duration.ofMillis(0));
        assertEquals(-1000L, Duration.ofMillis(-1000).toMillis());

        assertNotNull(Duration.ZERO);
        assertTrue(Duration.ZERO.isZero());
        assertEquals(0L, Duration.ZERO.toMillis());
        assertSame(Duration.ZERO, Duration.ofMillis(0));
        assertSame(Duration.ZERO, Duration.ofSeconds(0));
        assertSame(Duration.ZERO, Duration.ofMinutes(0));
        assertSame(Duration.ZERO, Duration.ofHours(0));
        assertSame(Duration.ZERO, Duration.ofDays(0));
    }

    @Test
    public void testBetween() {
        java.util.Date later = new java.util.Date(2_000L);
        java.util.Date earlier = new java.util.Date(500L);
        assertEquals(Duration.ofMillis(1_500L), Duration.between(earlier, later));
        assertEquals(Duration.ofMillis(-1_500L), Duration.between(later, earlier));
        assertEquals(Duration.ZERO, Duration.between(later, later));
        assertThrows(ArithmeticException.class, () -> Duration.between(new java.util.Date(Long.MAX_VALUE), new java.util.Date(Long.MIN_VALUE)));
        assertThrows(IllegalArgumentException.class, () -> Duration.between(later, null));
        assertThrows(IllegalArgumentException.class, () -> Duration.between(null, later));

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTimeInMillis(1_000L);
        cal2.setTimeInMillis(5_000L);
        assertEquals(Duration.ofMillis(4_000L), Duration.between(cal1, cal2));
        assertEquals(Duration.ofMillis(-4_000L), Duration.between(cal2, cal1));
        cal1.setTimeInMillis(5_000L);
        assertEquals(Duration.ZERO, Duration.between(cal1, cal2));
        Calendar start = Calendar.getInstance();
        Calendar end = Calendar.getInstance();
        start.setTimeInMillis(Long.MAX_VALUE);
        end.setTimeInMillis(Long.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> Duration.between(start, end));
        assertThrows(IllegalArgumentException.class, () -> Duration.between(cal2, null));
        assertThrows(IllegalArgumentException.class, () -> Duration.between(null, cal2));

        Instant instantStart = Instant.ofEpochMilli(1_000L);
        Instant instantEnd = Instant.ofEpochMilli(1_600L);
        assertEquals(Duration.ofMillis(600L), Duration.between(instantStart, instantEnd));
        assertEquals(Duration.ofMillis(-600L), Duration.between(instantEnd, instantStart));
        assertEquals(Duration.ZERO, Duration.between(instantStart, instantStart));
        assertThrows(IllegalArgumentException.class, () -> Duration.between(instantStart, null));
        assertThrows(IllegalArgumentException.class, () -> Duration.between(null, instantStart));

        LocalDateTime ldt1 = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime ldt2 = ldt1.plusDays(1).plusNanos(TimeUnit.MILLISECONDS.toNanos(123));
        assertEquals(Duration.ofMillis(TimeUnit.DAYS.toMillis(1) + 123), Duration.between(ldt1, ldt2));
        assertEquals(Duration.ofMillis(-(TimeUnit.DAYS.toMillis(1) + 123)), Duration.between(ldt2, ldt1));
    }

    @Test
    public void testIsZeroAndNegative() {
        assertTrue(Duration.ZERO.isZero());
        assertTrue(Duration.ofMillis(0).isZero());
        assertFalse(Duration.ofMillis(1).isZero());
        assertFalse(Duration.ofMillis(-1).isZero());
        assertTrue(Duration.ofMillis(-1).isNegative());
        assertTrue(Duration.ofSeconds(-1).isNegative());
        assertFalse(Duration.ZERO.isNegative());
        assertFalse(Duration.ofMillis(1).isNegative());
    }

    @Test
    public void testPlusAndMinus() {
        Duration d1 = Duration.ofSeconds(30);
        assertEquals(50000L, d1.plus(Duration.ofSeconds(20)).toMillis());
        assertEquals(10000L, Duration.ofSeconds(10).plus(Duration.ZERO).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MAX_VALUE).plus(Duration.ofMillis(1)));
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MAX_VALUE - 1000).plusMillis(2000));

        assertEquals(172800000L, Duration.ofDays(1).plusDays(1).toMillis());
        assertEquals(259200000L, Duration.ofDays(5).plusDays(-2).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofDays(1).plusDays(Long.MAX_VALUE / 1000));
        assertEquals(10800000L, Duration.ofHours(1).plusHours(2).toMillis());
        assertEquals(10800000L, Duration.ofHours(5).plusHours(-2).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofHours(1).plusHours(Long.MAX_VALUE / 1000));
        assertEquals(2700000L, Duration.ofMinutes(30).plusMinutes(15).toMillis());
        assertEquals(1200000L, Duration.ofMinutes(30).plusMinutes(-10).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMinutes(1).plusMinutes(Long.MAX_VALUE / 1000));
        assertEquals(50000L, Duration.ofSeconds(30).plusSeconds(20).toMillis());
        assertEquals(20000L, Duration.ofSeconds(30).plusSeconds(-10).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofSeconds(1).plusSeconds(Long.MAX_VALUE));
        Duration millis = Duration.ofMillis(1000);
        assertEquals(1500L, millis.plusMillis(500).toMillis());
        assertSame(millis, millis.plusMillis(0));
        assertEquals(500L, millis.plusMillis(-500).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MAX_VALUE).plusMillis(1));

        Duration d2 = Duration.ofSeconds(50);
        assertEquals(30000L, d2.minus(Duration.ofSeconds(20)).toMillis());
        assertEquals(10000L, Duration.ofSeconds(10).minus(Duration.ZERO).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MIN_VALUE).minus(Duration.ofMillis(1)));
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MIN_VALUE + 1000).minusMillis(2000));

        assertEquals(259200000L, Duration.ofDays(5).minusDays(2).toMillis());
        assertEquals(172800000L, Duration.ofDays(1).minusDays(-1).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofDays(-1).minusDays(Long.MAX_VALUE / 1000));
        assertEquals(10800000L, Duration.ofHours(5).minusHours(2).toMillis());
        assertEquals(10800000L, Duration.ofHours(1).minusHours(-2).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofHours(-1).minusHours(Long.MAX_VALUE / 1000));
        assertEquals(1800000L, Duration.ofMinutes(45).minusMinutes(15).toMillis());
        assertEquals(2400000L, Duration.ofMinutes(30).minusMinutes(-10).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMinutes(-1).minusMinutes(Long.MAX_VALUE / 1000));
        assertEquals(30000L, Duration.ofSeconds(50).minusSeconds(20).toMillis());
        assertEquals(40000L, Duration.ofSeconds(30).minusSeconds(-10).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofSeconds(-1).minusSeconds(Long.MAX_VALUE));
        Duration minusMillis = Duration.ofMillis(1500);
        assertEquals(1000L, minusMillis.minusMillis(500).toMillis());
        assertSame(minusMillis, minusMillis.minusMillis(0));
        assertEquals(1500L, Duration.ofMillis(1000).minusMillis(-500).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MIN_VALUE).minusMillis(1));

        Duration actualWork = Duration.ofHours(8).minus(Duration.ofMinutes(30)).minus(Duration.ofMinutes(45));
        assertEquals(405L, actualWork.toMinutes());
        Duration chained = Duration.ofDays(1).plusHours(2).plusMinutes(30).plusSeconds(15).plusMillis(500);
        assertEquals(86400000L + 7200000L + 1800000L + 15000L + 500L, chained.toMillis());
    }

    @Test
    public void testMultipliedDividedNegatedAbs() {
        Duration d = Duration.ofSeconds(10);
        assertEquals(30000L, d.multipliedBy(3).toMillis());
        assertEquals(Duration.ZERO, d.multipliedBy(0));
        assertSame(d, d.multipliedBy(1));
        assertEquals(-20000L, d.multipliedBy(-2).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MAX_VALUE / 2).multipliedBy(3));
        assertThrows(ArithmeticException.class, () -> Duration.ofDays(1000000).multipliedBy(1000000));

        Duration thirty = Duration.ofSeconds(30);
        assertEquals(10000L, thirty.dividedBy(3).toMillis());
        assertSame(thirty, thirty.dividedBy(1));
        assertEquals(-15000L, thirty.dividedBy(-2).toMillis());
        assertThrows(ArithmeticException.class, () -> thirty.dividedBy(0));
        assertEquals(33L, Duration.ofMillis(100).dividedBy(3).toMillis());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MIN_VALUE).dividedBy(-1));
        assertEquals(Duration.ofMillis(-5), Duration.ofMillis(5).dividedBy(-1));
        assertEquals(Duration.ofMillis(2), Duration.ofMillis(5).dividedBy(2));

        assertEquals(-10000L, d.negated().toMillis());
        assertEquals(10000L, Duration.ofSeconds(-10).negated().toMillis());
        assertEquals(Duration.ZERO, Duration.ZERO.negated());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MIN_VALUE).negated());

        assertSame(d, d.abs());
        assertEquals(10000L, Duration.ofSeconds(-10).abs().toMillis());
        assertSame(Duration.ZERO, Duration.ZERO.abs());
        assertThrows(ArithmeticException.class, () -> Duration.ofMillis(Long.MIN_VALUE).abs());

        Duration original = Duration.ofSeconds(10);
        original.plusSeconds(5);
        original.minusSeconds(5);
        original.multipliedBy(2);
        original.dividedBy(2);
        original.negated();
        original.abs();
        assertEquals(10000L, original.toMillis());
    }

    @Test
    public void testConversions() {
        assertEquals(5L, Duration.ofDays(5).toDays());
        assertEquals(1L, Duration.ofHours(30).toDays());
        assertEquals(-3L, Duration.ofDays(-3).toDays());
        assertEquals(0L, Duration.ZERO.toDays());
        assertEquals(10L, Duration.ofHours(10).toHours());
        assertEquals(1L, Duration.ofMinutes(90).toHours());
        assertEquals(-5L, Duration.ofHours(-5).toHours());
        assertEquals(0L, Duration.ZERO.toHours());
        assertEquals(45L, Duration.ofMinutes(45).toMinutes());
        assertEquals(1L, Duration.ofSeconds(90).toMinutes());
        assertEquals(-30L, Duration.ofMinutes(-30).toMinutes());
        assertEquals(0L, Duration.ZERO.toMinutes());
        assertEquals(120L, Duration.ofSeconds(120).toSeconds());
        assertEquals(1L, Duration.ofMillis(1500).toSeconds());
        assertEquals(-60L, Duration.ofSeconds(-60).toSeconds());
        assertEquals(0L, Duration.ZERO.toSeconds());
        assertEquals(12345L, Duration.ofMillis(12345).toMillis());
        assertEquals(-5000L, Duration.ofMillis(-5000).toMillis());
        assertEquals(0L, Duration.ZERO.toMillis());

        Duration halfSecond = Duration.ofMillis(-500);
        assertEquals(0, halfSecond.toSecondsPart());
        assertEquals(-500, halfSecond.toMillisPart());
        Duration oneAndHalfSeconds = Duration.ofMillis(-1500);
        assertEquals(-1, oneAndHalfSeconds.toSecondsPart());
        assertEquals(-500, oneAndHalfSeconds.toMillisPart());

        assertEquals(7200000L, Duration.ofHours(2).toJdkDuration().toMillis());
        assertEquals(-1800000L, Duration.ofMinutes(-30).toJdkDuration().toMillis());
        assertEquals(0L, Duration.ZERO.toJdkDuration().toMillis());
    }

    @Test
    public void testCompareEqualsHashCodeToString() {
        Duration d1 = Duration.ofSeconds(10);
        Duration d2 = Duration.ofSeconds(20);
        Duration d3 = Duration.ofSeconds(10);
        assertTrue(d1.compareTo(d2) < 0);
        assertTrue(d2.compareTo(d1) > 0);
        assertEquals(0, d1.compareTo(d3));
        Duration negative = Duration.ofSeconds(-5);
        assertTrue(negative.compareTo(d1) < 0);
        assertTrue(d1.compareTo(negative) > 0);

        assertEquals(d1, d3);
        assertEquals(d1, d1);
        assertNotEquals(d1, d2);
        assertNotEquals(d1, null);
        assertNotEquals(d1, "not a duration");
        assertEquals(Duration.ZERO, Duration.ofMillis(0));
        assertEquals(d1.hashCode(), d3.hashCode());
        assertNotEquals(d1.hashCode(), d2.hashCode());
        assertEquals(Duration.ZERO.hashCode(), Duration.ofMillis(0).hashCode());

        assertEquals("PT0S", Duration.ZERO.toString());
        assertEquals("PT1H", Duration.ofHours(1).toString());
        assertEquals("PT1H30M", Duration.ofMinutes(90).toString());
        assertEquals("PT1H30M25S", Duration.ofHours(1).plusMinutes(30).plusSeconds(25).toString());
        assertEquals("PT25.500S", Duration.ofSeconds(25).plusMillis(500).toString());
        assertEquals("PT30S", Duration.ofSeconds(30).toString());
        assertEquals("PT5M", Duration.ofMinutes(5).toString());
        assertEquals("PT1.500S", Duration.ofMillis(1500).toString());
        assertTrue(Duration.ofHours(-2).minusMinutes(30).toString().contains("-"));

        assertEquals("-PT1M30.500S", Duration.ofMillis(-90_500).toString());
        assertEquals("-PT1M30S", Duration.ofMillis(-90_000).toString());
        assertEquals("-PT1.500S", Duration.ofMillis(-1_500).toString());
        assertEquals("-PT1H1M1S", Duration.ofMillis(-3_661_000).toString());
        assertEquals("PT1M30.500S", Duration.ofMillis(90_500).toString());
        assertEquals("-PT0.500S", Duration.ofMillis(-500).toString());
        assertEquals("-PT1M", Duration.ofMinutes(-1).toString());
        assertEquals("-PT1H", Duration.ofHours(-1).toString());
        assertEquals("-PT2562047788015H12M55.808S", Duration.ofMillis(Long.MIN_VALUE).toString());

        long[] values = { -500L, -60_000L, -90_500L, -3_600_000L, -3_661_000L, Long.MIN_VALUE };
        for (long value : values) {
            String text = Duration.ofMillis(value).toString();
            assertEquals(value, java.time.Duration.parse(text).toMillis(), text);
        }
    }
}
