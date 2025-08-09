package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Timed100Test extends TestBase {

    @Test
    public void testOfWithValue() {
        // Test creation with current time
        String value = "test value";
        long beforeTime = System.currentTimeMillis();
        Timed<String> timed = Timed.of(value);
        long afterTime = System.currentTimeMillis();
        
        Assertions.assertEquals(value, timed.value());
        Assertions.assertTrue(timed.timestamp() >= beforeTime);
        Assertions.assertTrue(timed.timestamp() <= afterTime);
        
        // Test with null value
        Timed<String> nullTimed = Timed.of(null);
        Assertions.assertNull(nullTimed.value());
        Assertions.assertTrue(nullTimed.timestamp() >= beforeTime);
        
        // Test with different types
        Timed<Integer> intTimed = Timed.of(42);
        Assertions.assertEquals(42, intTimed.value());
        
        Timed<Object> objTimed = Timed.of(new Object());
        Assertions.assertNotNull(objTimed.value());
    }

    @Test
    public void testOfWithValueAndTime() {
        // Test with specific timestamp
        String value = "specific time";
        long specificTime = 1000000L;
        Timed<String> timed = Timed.of(value, specificTime);
        
        Assertions.assertEquals(value, timed.value());
        Assertions.assertEquals(specificTime, timed.timestamp());
        
        // Test with various timestamps
        long pastTime = System.currentTimeMillis() - 3600000; // 1 hour ago
        Timed<String> pastTimed = Timed.of("past", pastTime);
        Assertions.assertEquals(pastTime, pastTimed.timestamp());
        
        long futureTime = System.currentTimeMillis() + 3600000; // 1 hour from now
        Timed<String> futureTimed = Timed.of("future", futureTime);
        Assertions.assertEquals(futureTime, futureTimed.timestamp());
        
        // Test with edge cases
        Timed<String> zeroTime = Timed.of("zero", 0L);
        Assertions.assertEquals(0L, zeroTime.timestamp());
        
        Timed<String> maxTime = Timed.of("max", Long.MAX_VALUE);
        Assertions.assertEquals(Long.MAX_VALUE, maxTime.timestamp());
    }

    @Test
    public void testTimestamp() {
        long time = 123456789L;
        Timed<String> timed = Timed.of("test", time);
        Assertions.assertEquals(time, timed.timestamp());
        
        // Multiple calls should return same value
        Assertions.assertEquals(timed.timestamp(), timed.timestamp());
    }

    @Test
    public void testValue() {
        String value = "test value";
        Timed<String> timed = Timed.of(value, 1000L);
        Assertions.assertEquals(value, timed.value());
        Assertions.assertSame(value, timed.value()); // Should return same instance
        
        // Test with null
        Timed<Object> nullTimed = Timed.of(null, 2000L);
        Assertions.assertNull(nullTimed.value());
        
        // Test with various types
        Integer intValue = 100;
        Timed<Integer> intTimed = Timed.of(intValue, 3000L);
        Assertions.assertEquals(intValue, intTimed.value());
        
        Object obj = new Object();
        Timed<Object> objTimed = Timed.of(obj, 4000L);
        Assertions.assertSame(obj, objTimed.value());
    }

    @Test
    public void testHashCode() {
        // Test hash code calculation
        Timed<String> timed1 = Timed.of("test", 1000L);
        Timed<String> timed2 = Timed.of("test", 1000L);
        Timed<String> timed3 = Timed.of("test", 2000L);
        Timed<String> timed4 = Timed.of("different", 1000L);
        Timed<String> timed5 = Timed.of(null, 1000L);
        
        // Same content should have same hash code
        Assertions.assertEquals(timed1.hashCode(), timed2.hashCode());
        
        // Different timestamp should have different hash code
        Assertions.assertNotEquals(timed1.hashCode(), timed3.hashCode());
        
        // Different value should have different hash code
        Assertions.assertNotEquals(timed1.hashCode(), timed4.hashCode());
        
        // Test null value hash code
        int expectedNullHash = (int)(1000L * 31);
        Assertions.assertEquals(expectedNullHash, timed5.hashCode());
        
        // Test hash code formula
        String testValue = "hashtest";
        long testTime = 5000L;
        Timed<String> hashTimed = Timed.of(testValue, testTime);
        int expectedHash = (int)(testTime * 31 + testValue.hashCode());
        Assertions.assertEquals(expectedHash, hashTimed.hashCode());
    }

    @Test
    public void testEquals() {
        Timed<String> timed1 = Timed.of("test", 1000L);
        Timed<String> timed2 = Timed.of("test", 1000L);
        Timed<String> timed3 = Timed.of("test", 2000L);
        Timed<String> timed4 = Timed.of("different", 1000L);
        Timed<Integer> timed5 = Timed.of(100, 1000L);
        Timed<String> timed6 = Timed.of(null, 1000L);
        Timed<String> timed7 = Timed.of(null, 1000L);
        
        // Test equals with same object
        Assertions.assertTrue(timed1.equals(timed1));
        
        // Test equals with equal content
        Assertions.assertTrue(timed1.equals(timed2));
        Assertions.assertTrue(timed2.equals(timed1));
        
        // Test not equals with different timestamp
        Assertions.assertFalse(timed1.equals(timed3));
        
        // Test not equals with different value
        Assertions.assertFalse(timed1.equals(timed4));
        
        // Test not equals with different type (still Timed but different value type)
        Assertions.assertFalse(timed1.equals(timed5));
        
        // Test not equals with null
        Assertions.assertFalse(timed1.equals(null));
        
        // Test not equals with different class
        Assertions.assertFalse(timed1.equals("not a Timed"));
        
        // Test equals with null values
        Assertions.assertTrue(timed6.equals(timed7));
        Assertions.assertFalse(timed1.equals(timed6));
    }

    @Test
    public void testToString() {
        // Test basic toString
        Timed<String> stringTimed = Timed.of("Hello", 1000L);
        Assertions.assertEquals("1000: Hello", stringTimed.toString());
        
        Timed<Integer> intTimed = Timed.of(42, 2000L);
        Assertions.assertEquals("2000: 42", intTimed.toString());
        
        // Test with null value
        Timed<String> nullTimed = Timed.of(null, 3000L);
        String nullStr = nullTimed.toString();
        Assertions.assertTrue(nullStr.startsWith("3000: "));
        
        // Test with large timestamp
        Timed<String> largeTimed = Timed.of("test", 1609459200000L);
        Assertions.assertEquals("1609459200000: test", largeTimed.toString());
    }

    @Test
    public void testImmutability() {
        // Test that Timed is immutable
        String value = "original";
        long time = 1000L;
        Timed<String> timed = Timed.of(value, time);
        
        // Values should not change
        Assertions.assertEquals(value, timed.value());
        Assertions.assertEquals(time, timed.timestamp());
        
        // Even if we modify the original reference (for mutable objects)
        StringBuilder mutableValue = new StringBuilder("mutable");
        Timed<StringBuilder> mutableTimed = Timed.of(mutableValue, 2000L);
        mutableValue.append(" modified");
        
        // The timed object still holds the same reference
        Assertions.assertSame(mutableValue, mutableTimed.value());
        Assertions.assertEquals("mutable modified", mutableTimed.value().toString());
    }

    @Test
    public void testUsageScenarios() {
        // Test caching scenario
        String cachedData = "cached result";
        long cacheTime = System.currentTimeMillis();
        Timed<String> cachedResult = Timed.of(cachedData, cacheTime);
        
        // Check if cache is still valid (e.g., within 1 minute)
        long currentTime = System.currentTimeMillis();
        boolean cacheValid = (currentTime - cachedResult.timestamp()) < 60000;
        Assertions.assertTrue(cacheValid || (currentTime - cachedResult.timestamp()) >= 0);
        
        // Test event logging scenario
        String event = "User logged in";
        Timed<String> logEntry = Timed.of(event);
        Assertions.assertEquals(event, logEntry.value());
        Assertions.assertTrue(logEntry.timestamp() <= System.currentTimeMillis());
        
        // Test historical data scenario
        long historicalTime = System.currentTimeMillis() - 86400000; // 1 day ago
        Double historicalPrice = 99.99;
        Timed<Double> pricePoint = Timed.of(historicalPrice, historicalTime);
        Assertions.assertEquals(historicalPrice, pricePoint.value());
        Assertions.assertEquals(historicalTime, pricePoint.timestamp());
    }

    @Test
    public void testWithComplexObjects() {
        // Test with custom objects
        class CustomData {
            final String name;
            final int value;
            
            CustomData(String name, int value) {
                this.name = name;
                this.value = value;
            }
            
            @Override
            public String toString() {
                return "CustomData{name='" + name + "', value=" + value + "}";
            }
            
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof CustomData)) return false;
                CustomData that = (CustomData) o;
                return value == that.value && name.equals(that.name);
            }
            
            @Override
            public int hashCode() {
                return name.hashCode() * 31 + value;
            }
        }
        
        CustomData data = new CustomData("test", 100);
        long time = 5000L;
        Timed<CustomData> timedData = Timed.of(data, time);
        
        Assertions.assertEquals(data, timedData.value());
        Assertions.assertEquals(time, timedData.timestamp());
        Assertions.assertEquals("5000: CustomData{name='test', value=100}", timedData.toString());
        
        // Test equals with custom objects
        Timed<CustomData> timedData2 = Timed.of(new CustomData("test", 100), 5000L);
        Assertions.assertEquals(timedData, timedData2);
    }
}