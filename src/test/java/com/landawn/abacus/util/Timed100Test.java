package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Timed100Test extends TestBase {

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
    public void testTimestamp() {
        long time = 123456789L;
        Timed<String> timed = Timed.of("test", time);
        Assertions.assertEquals(time, timed.timestamp());

        Assertions.assertEquals(timed.timestamp(), timed.timestamp());
    }

    @Test
    public void testValue() {
        String value = "test value";
        Timed<String> timed = Timed.of(value, 1000L);
        Assertions.assertEquals(value, timed.value());
        Assertions.assertSame(value, timed.value());

        Timed<Object> nullTimed = Timed.of(null, 2000L);
        Assertions.assertNull(nullTimed.value());

        Integer intValue = 100;
        Timed<Integer> intTimed = Timed.of(intValue, 3000L);
        Assertions.assertEquals(intValue, intTimed.value());

        Object obj = new Object();
        Timed<Object> objTimed = Timed.of(obj, 4000L);
        Assertions.assertSame(obj, objTimed.value());
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
        Timed<String> stringTimed = Timed.of("Hello", 1000L);
        Assertions.assertEquals("1000: Hello", stringTimed.toString());

        Timed<Integer> intTimed = Timed.of(42, 2000L);
        Assertions.assertEquals("2000: 42", intTimed.toString());

        Timed<String> nullTimed = Timed.of(null, 3000L);
        String nullStr = nullTimed.toString();
        Assertions.assertTrue(nullStr.startsWith("3000: "));

        Timed<String> largeTimed = Timed.of("test", 1609459200000L);
        Assertions.assertEquals("1609459200000: test", largeTimed.toString());
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
