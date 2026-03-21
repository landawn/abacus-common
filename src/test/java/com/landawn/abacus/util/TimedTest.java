package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TimedTest extends TestBase {

    @Test
    public void testUsageScenarios() {
        String cachedData = "cached result";
        long cacheTime = System.currentTimeMillis();
        Timed<String> cachedResult = Timed.of(cachedData, cacheTime);

        long currentTime = System.currentTimeMillis();
        boolean cacheValid = (currentTime - cachedResult.timestamp()) < 60000;
        Assertions.assertTrue(cacheValid || (currentTime - cachedResult.timestamp()) >= 0);

        String event = "User logged in";
        Timed<String> logEntry = Timed.of(event);
        Assertions.assertEquals(event, logEntry.value());
        Assertions.assertTrue(logEntry.timestamp() <= System.currentTimeMillis());

        long historicalTime = System.currentTimeMillis() - 86400000;
        Double historicalPrice = 99.99;
        Timed<Double> pricePoint = Timed.of(historicalPrice, historicalTime);
        Assertions.assertEquals(historicalPrice, pricePoint.value());
        Assertions.assertEquals(historicalTime, pricePoint.timestamp());
    }

    @Test
    public void testOf_value() {
        Timed<String> timed = Timed.of("test");
        assertNotNull(timed);
        assertEquals("test", timed.value());
    }

    @Test
    public void testOf_valueWithTimestamp() {
        long timestamp = 1000L;
        Timed<String> timed = Timed.of("test", timestamp);
        assertNotNull(timed);
        assertEquals("test", timed.value());
        assertEquals(timestamp, timed.timestamp());
    }

    @Test
    public void testOf_nullValue() {
        Timed<String> timed = Timed.of(null);
        assertNotNull(timed);
        assertNull(timed.value());
    }

    @Test
    public void testOfWithValue() {
        String value = "test value";
        long beforeTime = System.currentTimeMillis();
        Timed<String> timed = Timed.of(value);
        long afterTime = System.currentTimeMillis();

        Assertions.assertEquals(value, timed.value());
        Assertions.assertTrue(timed.timestamp() >= beforeTime);
        Assertions.assertTrue(timed.timestamp() <= afterTime);

        Timed<String> nullTimed = Timed.of(null);
        Assertions.assertNull(nullTimed.value());
        Assertions.assertTrue(nullTimed.timestamp() >= beforeTime);

        Timed<Integer> intTimed = Timed.of(42);
        Assertions.assertEquals(42, intTimed.value());

        Timed<Object> objTimed = Timed.of(new Object());
        Assertions.assertNotNull(objTimed.value());
    }

    @Test
    public void testOfWithValueAndTime() {
        String value = "specific time";
        long specificTime = 1000000L;
        Timed<String> timed = Timed.of(value, specificTime);

        Assertions.assertEquals(value, timed.value());
        Assertions.assertEquals(specificTime, timed.timestamp());

        long pastTime = System.currentTimeMillis() - 3600000;
        Timed<String> pastTimed = Timed.of("past", pastTime);
        Assertions.assertEquals(pastTime, pastTimed.timestamp());

        long futureTime = System.currentTimeMillis() + 3600000;
        Timed<String> futureTimed = Timed.of("future", futureTime);
        Assertions.assertEquals(futureTime, futureTimed.timestamp());

        Timed<String> zeroTime = Timed.of("zero", 0L);
        Assertions.assertEquals(0L, zeroTime.timestamp());

        Timed<String> maxTime = Timed.of("max", Long.MAX_VALUE);
        Assertions.assertEquals(Long.MAX_VALUE, maxTime.timestamp());
    }

    @Test
    public void testImmutability() {
        String value = "original";
        long time = 1000L;
        Timed<String> timed = Timed.of(value, time);

        Assertions.assertEquals(value, timed.value());
        Assertions.assertEquals(time, timed.timestamp());

        StringBuilder mutableValue = new StringBuilder("mutable");
        Timed<StringBuilder> mutableTimed = Timed.of(mutableValue, 2000L);
        mutableValue.append(" modified");

        Assertions.assertSame(mutableValue, mutableTimed.value());
        Assertions.assertEquals("mutable modified", mutableTimed.value().toString());
    }

    @Test
    public void testTimestamp() {
        long before = System.currentTimeMillis();
        Timed<String> timed = Timed.of("test");
        long after = System.currentTimeMillis();
        assertTrue(timed.timestamp() >= before && timed.timestamp() <= after);
    }

    @Test
    public void testValue() {
        Timed<Integer> timed = Timed.of(42, 1000L);
        assertEquals(Integer.valueOf(42), timed.value());
    }

    @Test
    public void testHashCode_consistent() {
        Timed<String> timed = Timed.of("test", 1000L);
        assertEquals(timed.hashCode(), timed.hashCode());
    }

    @Test
    public void testHashCode() {
        Timed<String> timed1 = Timed.of("test", 1000L);
        Timed<String> timed2 = Timed.of("test", 1000L);
        Timed<String> timed3 = Timed.of("test", 2000L);
        Timed<String> timed4 = Timed.of("different", 1000L);
        Timed<String> timed5 = Timed.of(null, 1000L);

        Assertions.assertEquals(timed1.hashCode(), timed2.hashCode());

        Assertions.assertNotEquals(timed1.hashCode(), timed3.hashCode());

        Assertions.assertNotEquals(timed1.hashCode(), timed4.hashCode());

        int expectedNullHash = (int) (1000L * 31);
        Assertions.assertEquals(expectedNullHash, timed5.hashCode());

        String testValue = "hashtest";
        long testTime = 5000L;
        Timed<String> hashTimed = Timed.of(testValue, testTime);
        int expectedHash = (int) (testTime * 31 + testValue.hashCode());
        Assertions.assertEquals(expectedHash, hashTimed.hashCode());
    }

    @Test
    public void testEquals_differentValue() {
        Timed<String> t1 = Timed.of("test1", 1000L);
        Timed<String> t2 = Timed.of("test2", 1000L);
        assertNotEquals(t1, t2);
    }

    @Test
    public void testEquals_differentTimestamp() {
        Timed<String> t1 = Timed.of("test", 1000L);
        Timed<String> t2 = Timed.of("test", 2000L);
        assertNotEquals(t1, t2);
    }

    @Test
    public void testEquals_same() {
        Timed<String> t1 = Timed.of("test", 1000L);
        Timed<String> t2 = Timed.of("test", 1000L);
        assertEquals(t1, t2);
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

        Assertions.assertTrue(timed1.equals(timed1));

        Assertions.assertTrue(timed1.equals(timed2));
        Assertions.assertTrue(timed2.equals(timed1));

        Assertions.assertFalse(timed1.equals(timed3));

        Assertions.assertFalse(timed1.equals(timed4));

        Assertions.assertFalse(timed1.equals(timed5));

        Assertions.assertFalse(timed1.equals(null));

        Assertions.assertFalse(timed1.equals("not a Timed"));

        Assertions.assertTrue(timed6.equals(timed7));
        Assertions.assertFalse(timed1.equals(timed6));
    }

    @Test
    public void testToString() {
        Timed<String> timed = Timed.of("test", 1000L);
        String str = timed.toString();
        assertTrue(str.contains("1000"));
        assertTrue(str.contains("test"));
    }

    @Test
    public void testWithComplexObjects() {
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
                if (this == o)
                    return true;
                if (!(o instanceof CustomData))
                    return false;
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

        Timed<CustomData> timedData2 = Timed.of(new CustomData("test", 100), 5000L);
        Assertions.assertEquals(timedData, timedData2);
    }

}
