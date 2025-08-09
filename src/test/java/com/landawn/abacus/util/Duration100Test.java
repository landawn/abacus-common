package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Duration100Test extends TestBase {
    
    @Test
    public void testZeroConstant() {
        Duration zero = Duration.ZERO;
        Assertions.assertNotNull(zero);
        Assertions.assertEquals(0L, zero.toMillis());
        Assertions.assertTrue(zero.isZero());
        Assertions.assertFalse(zero.isNegative());
    }
    
    @Test
    public void testOfDays() {
        Duration duration = Duration.ofDays(1);
        Assertions.assertEquals(86400000L, duration.toMillis());
        Assertions.assertEquals(1L, duration.toDays());
        
        duration = Duration.ofDays(7);
        Assertions.assertEquals(604800000L, duration.toMillis());
        Assertions.assertEquals(7L, duration.toDays());
        
        duration = Duration.ofDays(-2);
        Assertions.assertEquals(-172800000L, duration.toMillis());
        Assertions.assertEquals(-2L, duration.toDays());
        Assertions.assertTrue(duration.isNegative());
        
        duration = Duration.ofDays(0);
        Assertions.assertSame(Duration.ZERO, duration);
    }
    
    @Test
    public void testOfHours() {
        Duration duration = Duration.ofHours(1);
        Assertions.assertEquals(3600000L, duration.toMillis());
        Assertions.assertEquals(1L, duration.toHours());
        
        duration = Duration.ofHours(24);
        Assertions.assertEquals(86400000L, duration.toMillis());
        Assertions.assertEquals(24L, duration.toHours());
        Assertions.assertEquals(1L, duration.toDays());
        
        duration = Duration.ofHours(-5);
        Assertions.assertEquals(-18000000L, duration.toMillis());
        Assertions.assertEquals(-5L, duration.toHours());
        Assertions.assertTrue(duration.isNegative());
        
        duration = Duration.ofHours(0);
        Assertions.assertSame(Duration.ZERO, duration);
    }
    
    @Test
    public void testOfMinutes() {
        Duration duration = Duration.ofMinutes(1);
        Assertions.assertEquals(60000L, duration.toMillis());
        Assertions.assertEquals(1L, duration.toMinutes());
        
        duration = Duration.ofMinutes(60);
        Assertions.assertEquals(3600000L, duration.toMillis());
        Assertions.assertEquals(60L, duration.toMinutes());
        Assertions.assertEquals(1L, duration.toHours());
        
        duration = Duration.ofMinutes(-30);
        Assertions.assertEquals(-1800000L, duration.toMillis());
        Assertions.assertEquals(-30L, duration.toMinutes());
        Assertions.assertTrue(duration.isNegative());
        
        duration = Duration.ofMinutes(0);
        Assertions.assertSame(Duration.ZERO, duration);
    }
    
    @Test
    public void testOfSeconds() {
        Duration duration = Duration.ofSeconds(1);
        Assertions.assertEquals(1000L, duration.toMillis());
        Assertions.assertEquals(1L, duration.toSeconds());
        
        duration = Duration.ofSeconds(60);
        Assertions.assertEquals(60000L, duration.toMillis());
        Assertions.assertEquals(60L, duration.toSeconds());
        Assertions.assertEquals(1L, duration.toMinutes());
        
        duration = Duration.ofSeconds(-15);
        Assertions.assertEquals(-15000L, duration.toMillis());
        Assertions.assertEquals(-15L, duration.toSeconds());
        Assertions.assertTrue(duration.isNegative());
        
        duration = Duration.ofSeconds(0);
        Assertions.assertSame(Duration.ZERO, duration);
    }
    
    @Test
    public void testOfMillis() {
        Duration duration = Duration.ofMillis(1000);
        Assertions.assertEquals(1000L, duration.toMillis());
        Assertions.assertEquals(1L, duration.toSeconds());
        
        duration = Duration.ofMillis(123456);
        Assertions.assertEquals(123456L, duration.toMillis());
        
        duration = Duration.ofMillis(-500);
        Assertions.assertEquals(-500L, duration.toMillis());
        Assertions.assertTrue(duration.isNegative());
        
        duration = Duration.ofMillis(0);
        Assertions.assertSame(Duration.ZERO, duration);
    }
    
    @Test
    public void testIsZero() {
        Assertions.assertTrue(Duration.ZERO.isZero());
        Assertions.assertTrue(Duration.ofMillis(0).isZero());
        Assertions.assertFalse(Duration.ofMillis(1).isZero());
        Assertions.assertFalse(Duration.ofMillis(-1).isZero());
    }
    
    @Test
    public void testIsNegative() {
        Assertions.assertFalse(Duration.ZERO.isNegative());
        Assertions.assertFalse(Duration.ofMillis(1).isNegative());
        Assertions.assertTrue(Duration.ofMillis(-1).isNegative());
        Assertions.assertTrue(Duration.ofDays(-1).isNegative());
    }
    
    @Test
    public void testPlus() {
        Duration d1 = Duration.ofHours(2);
        Duration d2 = Duration.ofMinutes(30);
        Duration result = d1.plus(d2);
        
        Assertions.assertEquals(9000000L, result.toMillis()); // 2.5 hours
        Assertions.assertEquals(150L, result.toMinutes());
        
        // Test adding negative
        Duration d3 = Duration.ofHours(-1);
        result = d1.plus(d3);
        Assertions.assertEquals(3600000L, result.toMillis()); // 1 hour
        
        // Test adding to zero
        result = Duration.ZERO.plus(d1);
        Assertions.assertEquals(d1.toMillis(), result.toMillis());
    }
    
    @Test
    public void testPlusDays() {
        Duration duration = Duration.ofDays(1);
        Duration result = duration.plusDays(2);
        
        Assertions.assertEquals(3L, result.toDays());
        Assertions.assertEquals(259200000L, result.toMillis());
        
        // Test adding negative days
        result = duration.plusDays(-3);
        Assertions.assertEquals(-2L, result.toDays());
        Assertions.assertTrue(result.isNegative());
        
        // Test adding zero days
        result = duration.plusDays(0);
        Assertions.assertEquals(duration.toMillis(), result.toMillis());
    }
    
    @Test
    public void testPlusHours() {
        Duration duration = Duration.ofHours(10);
        Duration result = duration.plusHours(5);
        
        Assertions.assertEquals(15L, result.toHours());
        Assertions.assertEquals(54000000L, result.toMillis());
        
        // Test adding negative hours
        result = duration.plusHours(-15);
        Assertions.assertEquals(-5L, result.toHours());
        Assertions.assertTrue(result.isNegative());
    }
    
    @Test
    public void testPlusMinutes() {
        Duration duration = Duration.ofMinutes(30);
        Duration result = duration.plusMinutes(15);
        
        Assertions.assertEquals(45L, result.toMinutes());
        Assertions.assertEquals(2700000L, result.toMillis());
        
        // Test adding negative minutes
        result = duration.plusMinutes(-45);
        Assertions.assertEquals(-15L, result.toMinutes());
        Assertions.assertTrue(result.isNegative());
    }
    
    @Test
    public void testPlusSeconds() {
        Duration duration = Duration.ofSeconds(30);
        Duration result = duration.plusSeconds(30);
        
        Assertions.assertEquals(60L, result.toSeconds());
        Assertions.assertEquals(60000L, result.toMillis());
        Assertions.assertEquals(1L, result.toMinutes());
        
        // Test adding negative seconds
        result = duration.plusSeconds(-45);
        Assertions.assertEquals(-15L, result.toSeconds());
        Assertions.assertTrue(result.isNegative());
    }
    
    @Test
    public void testPlusMillis() {
        Duration duration = Duration.ofMillis(500);
        Duration result = duration.plusMillis(500);
        
        Assertions.assertEquals(1000L, result.toMillis());
        Assertions.assertEquals(1L, result.toSeconds());
        
        // Test adding zero millis - should return same instance
        result = duration.plusMillis(0);
        Assertions.assertSame(duration, result);
        
        // Test adding negative millis
        result = duration.plusMillis(-700);
        Assertions.assertEquals(-200L, result.toMillis());
        Assertions.assertTrue(result.isNegative());
    }
    
    @Test
    public void testMinus() {
        Duration d1 = Duration.ofHours(3);
        Duration d2 = Duration.ofHours(1);
        Duration result = d1.minus(d2);
        
        Assertions.assertEquals(2L, result.toHours());
        Assertions.assertEquals(7200000L, result.toMillis());
        
        // Test subtracting larger value
        result = d2.minus(d1);
        Assertions.assertEquals(-2L, result.toHours());
        Assertions.assertTrue(result.isNegative());
    }
    
    @Test
    public void testMinusDays() {
        Duration duration = Duration.ofDays(5);
        Duration result = duration.minusDays(2);
        
        Assertions.assertEquals(3L, result.toDays());
        
        // Test subtracting negative (adding)
        result = duration.minusDays(-2);
        Assertions.assertEquals(7L, result.toDays());
    }
    
    @Test
    public void testMinusHours() {
        Duration duration = Duration.ofHours(10);
        Duration result = duration.minusHours(3);
        
        Assertions.assertEquals(7L, result.toHours());
        
        // Test subtracting more than available
        result = duration.minusHours(15);
        Assertions.assertEquals(-5L, result.toHours());
        Assertions.assertTrue(result.isNegative());
    }
    
    @Test
    public void testMinusMinutes() {
        Duration duration = Duration.ofMinutes(60);
        Duration result = duration.minusMinutes(15);
        
        Assertions.assertEquals(45L, result.toMinutes());
    }
    
    @Test
    public void testMinusSeconds() {
        Duration duration = Duration.ofSeconds(60);
        Duration result = duration.minusSeconds(15);
        
        Assertions.assertEquals(45L, result.toSeconds());
    }
    
    @Test
    public void testMinusMillis() {
        Duration duration = Duration.ofMillis(1000);
        Duration result = duration.minusMillis(250);
        
        Assertions.assertEquals(750L, result.toMillis());
        
        // Test subtracting zero - should return same instance
        result = duration.minusMillis(0);
        Assertions.assertSame(duration, result);
    }
    
    @Test
    public void testMultipliedBy() {
        Duration duration = Duration.ofHours(2);
        Duration result = duration.multipliedBy(3);
        
        Assertions.assertEquals(6L, result.toHours());
        Assertions.assertEquals(21600000L, result.toMillis());
        
        // Test multiply by 0 - returns ZERO
        result = duration.multipliedBy(0);
        Assertions.assertSame(Duration.ZERO, result);
        
        // Test multiply by 1 - returns same instance
        result = duration.multipliedBy(1);
        Assertions.assertSame(duration, result);
        
        // Test multiply by negative
        result = duration.multipliedBy(-2);
        Assertions.assertEquals(-4L, result.toHours());
        Assertions.assertTrue(result.isNegative());
    }
    
    @Test
    public void testDividedBy() {
        Duration duration = Duration.ofHours(6);
        Duration result = duration.dividedBy(2);
        
        Assertions.assertEquals(3L, result.toHours());
        
        // Test divide by 1 - returns same instance
        result = duration.dividedBy(1);
        Assertions.assertSame(duration, result);
        
        // Test divide by negative
        result = duration.dividedBy(-3);
        Assertions.assertEquals(-2L, result.toHours());
        Assertions.assertTrue(result.isNegative());
        
        // Test divide by zero - should throw
        Assertions.assertThrows(ArithmeticException.class, () -> duration.dividedBy(0));
        
        // Test integer division truncation
        Duration duration2 = Duration.ofMinutes(70);
        result = duration2.dividedBy(60);
        Assertions.assertEquals(1L, result.toMinutes()); // 70/60 = 1 (truncated)
    }
    
    @Test
    public void testNegated() {
        Duration positive = Duration.ofHours(5);
        Duration negative = positive.negated();
        
        Assertions.assertEquals(-5L, negative.toHours());
        Assertions.assertTrue(negative.isNegative());
        
        // Test negating negative
        Duration doubleNegative = negative.negated();
        Assertions.assertEquals(5L, doubleNegative.toHours());
        Assertions.assertFalse(doubleNegative.isNegative());
        
        // Test negating zero
        Duration zeroNegated = Duration.ZERO.negated();
        Assertions.assertTrue(zeroNegated.isZero());
    }
    
    @Test
    public void testAbs() {
        Duration positive = Duration.ofHours(5);
        Duration absPositive = positive.abs();
        
        // Positive should return same instance
        Assertions.assertSame(positive, absPositive);
        
        Duration negative = Duration.ofHours(-5);
        Duration absNegative = negative.abs();
        
        Assertions.assertEquals(5L, absNegative.toHours());
        Assertions.assertFalse(absNegative.isNegative());
        
        // Test zero
        Duration absZero = Duration.ZERO.abs();
        Assertions.assertSame(Duration.ZERO, absZero);
    }
    
    @Test
    public void testToDays() {
        Duration duration = Duration.ofDays(2);
        Assertions.assertEquals(2L, duration.toDays());
        
        // Test with hours
        duration = Duration.ofHours(48);
        Assertions.assertEquals(2L, duration.toDays());
        
        // Test with partial days (truncated)
        duration = Duration.ofHours(36);
        Assertions.assertEquals(1L, duration.toDays()); // 1.5 days truncated to 1
        
        // Test negative
        duration = Duration.ofDays(-3);
        Assertions.assertEquals(-3L, duration.toDays());
    }
    
    @Test
    public void testToHours() {
        Duration duration = Duration.ofHours(5);
        Assertions.assertEquals(5L, duration.toHours());
        
        // Test with minutes
        duration = Duration.ofMinutes(120);
        Assertions.assertEquals(2L, duration.toHours());
        
        // Test with partial hours (truncated)
        duration = Duration.ofMinutes(90);
        Assertions.assertEquals(1L, duration.toHours()); // 1.5 hours truncated to 1
    }
    
    @Test
    public void testToMinutes() {
        Duration duration = Duration.ofMinutes(30);
        Assertions.assertEquals(30L, duration.toMinutes());
        
        // Test with seconds
        duration = Duration.ofSeconds(120);
        Assertions.assertEquals(2L, duration.toMinutes());
        
        // Test with partial minutes (truncated)
        duration = Duration.ofSeconds(90);
        Assertions.assertEquals(1L, duration.toMinutes()); // 1.5 minutes truncated to 1
    }
    
    @Test
    public void testToSeconds() {
        Duration duration = Duration.ofSeconds(45);
        Assertions.assertEquals(45L, duration.toSeconds());
        
        // Test with milliseconds
        duration = Duration.ofMillis(3000);
        Assertions.assertEquals(3L, duration.toSeconds());
        
        // Test with partial seconds (truncated)
        duration = Duration.ofMillis(1500);
        Assertions.assertEquals(1L, duration.toSeconds()); // 1.5 seconds truncated to 1
    }
    
    @Test
    public void testToMillis() {
        Duration duration = Duration.ofMillis(12345);
        Assertions.assertEquals(12345L, duration.toMillis());
        
        // Test conversions from other units
        duration = Duration.ofSeconds(2);
        Assertions.assertEquals(2000L, duration.toMillis());
        
        duration = Duration.ofMinutes(1);
        Assertions.assertEquals(60000L, duration.toMillis());
        
        duration = Duration.ofHours(1);
        Assertions.assertEquals(3600000L, duration.toMillis());
        
        duration = Duration.ofDays(1);
        Assertions.assertEquals(86400000L, duration.toMillis());
    }
    
    @Test
    public void testCompareTo() {
        Duration d1 = Duration.ofHours(1);
        Duration d2 = Duration.ofHours(2);
        Duration d3 = Duration.ofMinutes(60); // Same as 1 hour
        
        Assertions.assertTrue(d1.compareTo(d2) < 0);
        Assertions.assertTrue(d2.compareTo(d1) > 0);
        Assertions.assertEquals(0, d1.compareTo(d3));
        Assertions.assertEquals(0, d1.compareTo(d1));
        
        // Test with negative durations
        Duration negative = Duration.ofHours(-1);
        Assertions.assertTrue(negative.compareTo(d1) < 0);
        Assertions.assertTrue(d1.compareTo(negative) > 0);
    }
    
    @Test
    public void testEquals() {
        Duration d1 = Duration.ofHours(2);
        Duration d2 = Duration.ofHours(2);
        Duration d3 = Duration.ofMinutes(120); // Same as 2 hours
        Duration d4 = Duration.ofHours(3);
        
        // Test equality
        Assertions.assertEquals(d1, d1); // Same instance
        Assertions.assertEquals(d1, d2); // Same value
        Assertions.assertEquals(d1, d3); // Same value, different units
        Assertions.assertNotEquals(d1, d4); // Different value
        Assertions.assertNotEquals(d1, null);
        Assertions.assertNotEquals(d1, "not a duration");
        
        // Test with Duration object of different value
        Assertions.assertNotEquals(d1, Duration.ZERO);
    }
    
    @Test
    public void testHashCode() {
        Duration d1 = Duration.ofHours(2);
        Duration d2 = Duration.ofHours(2);
        Duration d3 = Duration.ofMinutes(120); // Same as 2 hours
        
        // Equal durations should have same hash code
        Assertions.assertEquals(d1.hashCode(), d2.hashCode());
        Assertions.assertEquals(d1.hashCode(), d3.hashCode());
        
        // Different durations likely have different hash codes (not guaranteed but likely)
        Duration d4 = Duration.ofHours(3);
        // We can't assert they're different as hash collisions are possible
    }
    
    @Test
    public void testToString() {
        // Test zero
        Assertions.assertEquals("PT0S", Duration.ZERO.toString());
        
        // Test hours only
        Assertions.assertEquals("PT1H", Duration.ofHours(1).toString());
        Assertions.assertEquals("PT-5H", Duration.ofHours(-5).toString());
        
        // Test minutes only
        Assertions.assertEquals("PT30M", Duration.ofMinutes(30).toString());
        
        // Test seconds only
        Assertions.assertEquals("PT45S", Duration.ofSeconds(45).toString());
        
        // Test combined hours and minutes
        Duration d = Duration.ofHours(1).plusMinutes(30);
        Assertions.assertEquals("PT1H30M", d.toString());
        
        // Test combined hours, minutes and seconds
        d = Duration.ofHours(1).plusMinutes(30).plusSeconds(25);
        Assertions.assertEquals("PT1H30M25S", d.toString());
        
        // Test with milliseconds
        d = Duration.ofSeconds(25).plusMillis(500);
        Assertions.assertEquals("PT25.500S", d.toString());
        
        // Test negative milliseconds
        d = Duration.ofMillis(-500);
        Assertions.assertEquals("PT-0.500S", d.toString());
        
        // Test edge case: negative seconds with positive millis component
        d = Duration.ofSeconds(-1).plusMillis(500);
        Assertions.assertEquals("PT-0.500S", d.toString());
        
        // Test multiple units
        d = Duration.ofDays(2).plusHours(3).plusMinutes(45).plusSeconds(30);
        Assertions.assertEquals("PT51H45M30S", d.toString());
    }
    
    @Test
    public void testOverflowHandling() {
        // Test overflow in factory methods
        Assertions.assertThrows(ArithmeticException.class, () -> 
            Duration.ofDays(Long.MAX_VALUE / 86400000L + 1)
        );
        
        Assertions.assertThrows(ArithmeticException.class, () -> 
            Duration.ofHours(Long.MAX_VALUE / 3600000L + 1)
        );
        
        // Test overflow in arithmetic operations
        Duration large = Duration.ofMillis(Long.MAX_VALUE - 1000);
        Assertions.assertThrows(ArithmeticException.class, () -> 
            large.plusMillis(2000)
        );
        
        Duration small = Duration.ofMillis(Long.MIN_VALUE + 1000);
        Assertions.assertThrows(ArithmeticException.class, () -> 
            small.minusMillis(2000)
        );
        
        // Test overflow in multiplication
        Duration d = Duration.ofDays(1000000);
        Assertions.assertThrows(ArithmeticException.class, () -> 
            d.multipliedBy(1000000)
        );
    }
    
    @Test
    public void testCombinedOperations() {
        // Complex duration calculation
        Duration workDay = Duration.ofHours(8);
        Duration lunch = Duration.ofMinutes(30);
        Duration meeting = Duration.ofMinutes(45);
        
        Duration actualWork = workDay.minus(lunch).minus(meeting);
        Assertions.assertEquals(405L, actualWork.toMinutes()); // 6 hours 45 minutes
        
        // Test chaining operations
        Duration result = Duration.ofDays(1)
            .plusHours(2)
            .plusMinutes(30)
            .plusSeconds(15)
            .plusMillis(500);
        
        long expectedMillis = 86400000L + 7200000L + 1800000L + 15000L + 500L;
        Assertions.assertEquals(expectedMillis, result.toMillis());
    }
}