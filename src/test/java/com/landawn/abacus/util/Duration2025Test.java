package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Duration2025Test extends TestBase {

    @Test
    public void testOfDays() {
        Duration d = Duration.ofDays(1);
        assertEquals(86400000L, d.toMillis());

        Duration zero = Duration.ofDays(0);
        assertEquals(Duration.ZERO, zero);
        assertTrue(zero.isZero());

        Duration negative = Duration.ofDays(-1);
        assertEquals(-86400000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofDays(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testOfHours() {
        Duration d = Duration.ofHours(1);
        assertEquals(3600000L, d.toMillis());

        Duration zero = Duration.ofHours(0);
        assertEquals(Duration.ZERO, zero);

        Duration negative = Duration.ofHours(-24);
        assertEquals(-86400000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofHours(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testOfMinutes() {
        Duration d = Duration.ofMinutes(1);
        assertEquals(60000L, d.toMillis());

        Duration zero = Duration.ofMinutes(0);
        assertEquals(Duration.ZERO, zero);

        Duration negative = Duration.ofMinutes(-60);
        assertEquals(-3600000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofMinutes(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testOfSeconds() {
        Duration d = Duration.ofSeconds(1);
        assertEquals(1000L, d.toMillis());

        Duration zero = Duration.ofSeconds(0);
        assertEquals(Duration.ZERO, zero);

        Duration negative = Duration.ofSeconds(-60);
        assertEquals(-60000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofSeconds(Long.MAX_VALUE));
    }

    @Test
    public void testOfMillis() {
        Duration d = Duration.ofMillis(1000);
        assertEquals(1000L, d.toMillis());

        Duration zero = Duration.ofMillis(0);
        assertEquals(Duration.ZERO, zero);

        Duration negative = Duration.ofMillis(-1000);
        assertEquals(-1000L, negative.toMillis());
    }

    @Test
    public void testIsZero() {
        assertTrue(Duration.ZERO.isZero());
        assertTrue(Duration.ofMillis(0).isZero());
        assertFalse(Duration.ofMillis(1).isZero());
        assertFalse(Duration.ofMillis(-1).isZero());
    }

    @Test
    public void testIsNegative() {
        assertTrue(Duration.ofMillis(-1).isNegative());
        assertTrue(Duration.ofSeconds(-1).isNegative());
        assertFalse(Duration.ZERO.isNegative());
        assertFalse(Duration.ofMillis(1).isNegative());
    }

    @Test
    public void testPlus() {
        Duration d1 = Duration.ofSeconds(30);
        Duration d2 = Duration.ofSeconds(20);
        Duration result = d1.plus(d2);
        assertEquals(50000L, result.toMillis());

        Duration zero = Duration.ofSeconds(10).plus(Duration.ZERO);
        assertEquals(10000L, zero.toMillis());

        Duration max = Duration.ofMillis(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> max.plus(Duration.ofMillis(1)));
    }

    @Test
    public void testPlusDays() {
        Duration d = Duration.ofDays(1);
        Duration result = d.plusDays(1);
        assertEquals(172800000L, result.toMillis());

        Duration negative = Duration.ofDays(5).plusDays(-2);
        assertEquals(259200000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofDays(1).plusDays(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testPlusHours() {
        Duration d = Duration.ofHours(1);
        Duration result = d.plusHours(2);
        assertEquals(10800000L, result.toMillis());

        Duration negative = Duration.ofHours(5).plusHours(-2);
        assertEquals(10800000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofHours(1).plusHours(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testPlusMinutes() {
        Duration d = Duration.ofMinutes(30);
        Duration result = d.plusMinutes(15);
        assertEquals(2700000L, result.toMillis());

        Duration negative = Duration.ofMinutes(30).plusMinutes(-10);
        assertEquals(1200000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofMinutes(1).plusMinutes(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testPlusSeconds() {
        Duration d = Duration.ofSeconds(30);
        Duration result = d.plusSeconds(20);
        assertEquals(50000L, result.toMillis());

        Duration negative = Duration.ofSeconds(30).plusSeconds(-10);
        assertEquals(20000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofSeconds(1).plusSeconds(Long.MAX_VALUE));
    }

    @Test
    public void testPlusMillis() {
        Duration d = Duration.ofMillis(1000);
        Duration result = d.plusMillis(500);
        assertEquals(1500L, result.toMillis());

        Duration same = d.plusMillis(0);
        assertSame(d, same);

        Duration negative = Duration.ofMillis(1000).plusMillis(-500);
        assertEquals(500L, negative.toMillis());

        Duration max = Duration.ofMillis(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> max.plusMillis(1));
    }

    @Test
    public void testMinus() {
        Duration d1 = Duration.ofSeconds(50);
        Duration d2 = Duration.ofSeconds(20);
        Duration result = d1.minus(d2);
        assertEquals(30000L, result.toMillis());

        Duration zero = Duration.ofSeconds(10).minus(Duration.ZERO);
        assertEquals(10000L, zero.toMillis());

        Duration min = Duration.ofMillis(Long.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> min.minus(Duration.ofMillis(1)));
    }

    @Test
    public void testMinusDays() {
        Duration d = Duration.ofDays(5);
        Duration result = d.minusDays(2);
        assertEquals(259200000L, result.toMillis());

        Duration negative = Duration.ofDays(1).minusDays(-1);
        assertEquals(172800000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofDays(-1).minusDays(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testMinusHours() {
        Duration d = Duration.ofHours(5);
        Duration result = d.minusHours(2);
        assertEquals(10800000L, result.toMillis());

        Duration negative = Duration.ofHours(1).minusHours(-2);
        assertEquals(10800000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofHours(-1).minusHours(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testMinusMinutes() {
        Duration d = Duration.ofMinutes(45);
        Duration result = d.minusMinutes(15);
        assertEquals(1800000L, result.toMillis());

        Duration negative = Duration.ofMinutes(30).minusMinutes(-10);
        assertEquals(2400000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofMinutes(-1).minusMinutes(Long.MAX_VALUE / 1000));
    }

    @Test
    public void testMinusSeconds() {
        Duration d = Duration.ofSeconds(50);
        Duration result = d.minusSeconds(20);
        assertEquals(30000L, result.toMillis());

        Duration negative = Duration.ofSeconds(30).minusSeconds(-10);
        assertEquals(40000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> Duration.ofSeconds(-1).minusSeconds(Long.MAX_VALUE));
    }

    @Test
    public void testMinusMillis() {
        Duration d = Duration.ofMillis(1500);
        Duration result = d.minusMillis(500);
        assertEquals(1000L, result.toMillis());

        Duration same = d.minusMillis(0);
        assertSame(d, same);

        Duration negative = Duration.ofMillis(1000).minusMillis(-500);
        assertEquals(1500L, negative.toMillis());

        Duration min = Duration.ofMillis(Long.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> min.minusMillis(1));
    }

    @Test
    public void testMultipliedBy() {
        Duration d = Duration.ofSeconds(10);
        Duration result = d.multipliedBy(3);
        assertEquals(30000L, result.toMillis());

        Duration zero = d.multipliedBy(0);
        assertEquals(Duration.ZERO, zero);

        Duration same = d.multipliedBy(1);
        assertSame(d, same);

        Duration negative = d.multipliedBy(-2);
        assertEquals(-20000L, negative.toMillis());

        Duration large = Duration.ofMillis(Long.MAX_VALUE / 2);
        assertThrows(ArithmeticException.class, () -> large.multipliedBy(3));
    }

    @Test
    public void testDividedBy() {
        Duration d = Duration.ofSeconds(30);
        Duration result = d.dividedBy(3);
        assertEquals(10000L, result.toMillis());

        Duration same = d.dividedBy(1);
        assertSame(d, same);

        Duration negative = d.dividedBy(-2);
        assertEquals(-15000L, negative.toMillis());

        assertThrows(ArithmeticException.class, () -> d.dividedBy(0));

        Duration truncated = Duration.ofMillis(100).dividedBy(3);
        assertEquals(33L, truncated.toMillis());
    }

    @Test
    public void testNegated() {
        Duration positive = Duration.ofSeconds(10);
        Duration negated = positive.negated();
        assertEquals(-10000L, negated.toMillis());

        Duration negative = Duration.ofSeconds(-10);
        Duration negatedNegative = negative.negated();
        assertEquals(10000L, negatedNegative.toMillis());

        Duration zero = Duration.ZERO.negated();
        assertEquals(Duration.ZERO, zero);

        Duration min = Duration.ofMillis(Long.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> min.negated());
    }

    @Test
    public void testAbs() {
        Duration positive = Duration.ofSeconds(10);
        Duration abs = positive.abs();
        assertSame(positive, abs);
        assertEquals(10000L, abs.toMillis());

        Duration negative = Duration.ofSeconds(-10);
        Duration absNegative = negative.abs();
        assertEquals(10000L, absNegative.toMillis());

        Duration zero = Duration.ZERO.abs();
        assertSame(zero, Duration.ZERO);

        Duration min = Duration.ofMillis(Long.MIN_VALUE);
        assertThrows(ArithmeticException.class, () -> min.abs());
    }

    @Test
    public void testToDays() {
        Duration d = Duration.ofDays(5);
        assertEquals(5L, d.toDays());

        Duration partial = Duration.ofHours(30);
        assertEquals(1L, partial.toDays());

        Duration negative = Duration.ofDays(-3);
        assertEquals(-3L, negative.toDays());

        Duration zero = Duration.ZERO;
        assertEquals(0L, zero.toDays());
    }

    @Test
    public void testToHours() {
        Duration d = Duration.ofHours(10);
        assertEquals(10L, d.toHours());

        Duration partial = Duration.ofMinutes(90);
        assertEquals(1L, partial.toHours());

        Duration negative = Duration.ofHours(-5);
        assertEquals(-5L, negative.toHours());

        Duration zero = Duration.ZERO;
        assertEquals(0L, zero.toHours());
    }

    @Test
    public void testToMinutes() {
        Duration d = Duration.ofMinutes(45);
        assertEquals(45L, d.toMinutes());

        Duration partial = Duration.ofSeconds(90);
        assertEquals(1L, partial.toMinutes());

        Duration negative = Duration.ofMinutes(-30);
        assertEquals(-30L, negative.toMinutes());

        Duration zero = Duration.ZERO;
        assertEquals(0L, zero.toMinutes());
    }

    @Test
    public void testToSeconds() {
        Duration d = Duration.ofSeconds(120);
        assertEquals(120L, d.toSeconds());

        Duration partial = Duration.ofMillis(1500);
        assertEquals(1L, partial.toSeconds());

        Duration negative = Duration.ofSeconds(-60);
        assertEquals(-60L, negative.toSeconds());

        Duration zero = Duration.ZERO;
        assertEquals(0L, zero.toSeconds());
    }

    @Test
    public void testToMillis() {
        Duration d = Duration.ofMillis(12345);
        assertEquals(12345L, d.toMillis());

        Duration negative = Duration.ofMillis(-5000);
        assertEquals(-5000L, negative.toMillis());

        Duration zero = Duration.ZERO;
        assertEquals(0L, zero.toMillis());
    }

    @Test
    public void testToJdkDuration() {
        Duration d = Duration.ofHours(2);
        java.time.Duration jdkDuration = d.toJdkDuration();
        assertEquals(7200000L, jdkDuration.toMillis());

        Duration negative = Duration.ofMinutes(-30);
        java.time.Duration jdkNegative = negative.toJdkDuration();
        assertEquals(-1800000L, jdkNegative.toMillis());

        Duration zero = Duration.ZERO;
        java.time.Duration jdkZero = zero.toJdkDuration();
        assertEquals(0L, jdkZero.toMillis());
    }

    @Test
    public void testCompareTo() {
        Duration d1 = Duration.ofSeconds(10);
        Duration d2 = Duration.ofSeconds(20);
        Duration d3 = Duration.ofSeconds(10);

        assertTrue(d1.compareTo(d2) < 0);
        assertTrue(d2.compareTo(d1) > 0);
        assertEquals(0, d1.compareTo(d3));

        Duration negative = Duration.ofSeconds(-5);
        assertTrue(negative.compareTo(d1) < 0);
        assertTrue(d1.compareTo(negative) > 0);
    }

    @Test
    public void testEquals() {
        Duration d1 = Duration.ofSeconds(10);
        Duration d2 = Duration.ofSeconds(10);
        Duration d3 = Duration.ofSeconds(20);

        assertEquals(d1, d2);
        assertNotEquals(d1, d3);
        assertNotEquals(d1, null);
        assertNotEquals(d1, "not a duration");

        assertEquals(d1, d1);

        assertEquals(Duration.ZERO, Duration.ofMillis(0));
    }

    @Test
    public void testHashCode() {
        Duration d1 = Duration.ofSeconds(10);
        Duration d2 = Duration.ofSeconds(10);
        Duration d3 = Duration.ofSeconds(20);

        assertEquals(d1.hashCode(), d2.hashCode());
        assertNotEquals(d1.hashCode(), d3.hashCode());

        assertEquals(Duration.ZERO.hashCode(), Duration.ofMillis(0).hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("PT0S", Duration.ZERO.toString());

        assertEquals("PT1H", Duration.ofHours(1).toString());

        assertEquals("PT1H30M", Duration.ofMinutes(90).toString());

        Duration complex = Duration.ofHours(1).plusMinutes(30).plusSeconds(25);
        assertEquals("PT1H30M25S", complex.toString());

        Duration withMillis = Duration.ofSeconds(25).plusMillis(500);
        assertEquals("PT25.500S", withMillis.toString());

        assertEquals("PT-0.500S", Duration.ofMillis(-500).toString());

        assertEquals("PT30S", Duration.ofSeconds(30).toString());

        assertEquals("PT5M", Duration.ofMinutes(5).toString());

        Duration negative = Duration.ofHours(-2).minusMinutes(30);
        String negativeStr = negative.toString();
        assertTrue(negativeStr.contains("-"));

        Duration secWithMillis = Duration.ofMillis(1500);
        assertEquals("PT1.500S", secWithMillis.toString());
    }

    @Test
    public void testZeroConstant() {
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
    public void testImmutability() {
        Duration original = Duration.ofSeconds(10);
        long originalMillis = original.toMillis();

        Duration plus = original.plusSeconds(5);
        assertEquals(10000L, original.toMillis());
        assertEquals(15000L, plus.toMillis());

        Duration minus = original.minusSeconds(5);
        assertEquals(10000L, original.toMillis());
        assertEquals(5000L, minus.toMillis());

        Duration multiplied = original.multipliedBy(2);
        assertEquals(10000L, original.toMillis());
        assertEquals(20000L, multiplied.toMillis());

        Duration divided = original.dividedBy(2);
        assertEquals(10000L, original.toMillis());
        assertEquals(5000L, divided.toMillis());

        Duration negated = original.negated();
        assertEquals(10000L, original.toMillis());
        assertEquals(-10000L, negated.toMillis());

        Duration abs = original.abs();
        assertEquals(10000L, original.toMillis());
        assertEquals(10000L, abs.toMillis());
    }
}
