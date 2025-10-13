package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Timed2025Test extends TestBase {

    @Test
    public void test_of_withValue() {
        long beforeTime = System.currentTimeMillis();
        Timed<String> timed = Timed.of("test");
        long afterTime = System.currentTimeMillis();

        assertNotNull(timed);
        assertEquals("test", timed.value());
        assertTrue(timed.timestamp() >= beforeTime);
        assertTrue(timed.timestamp() <= afterTime);
    }

    @Test
    public void test_of_withNullValue() {
        Timed<String> timed = Timed.of(null);

        assertNotNull(timed);
        assertNull(timed.value());
        assertTrue(timed.timestamp() > 0);
    }

    @Test
    public void test_of_withValueAndTimestamp() {
        long timestamp = 1609459200000L;
        Timed<Integer> timed = Timed.of(42, timestamp);

        assertNotNull(timed);
        assertEquals(42, timed.value());
        assertEquals(1609459200000L, timed.timestamp());
    }

    @Test
    public void test_of_withNullValueAndTimestamp() {
        long timestamp = 1234567890L;
        Timed<String> timed = Timed.of(null, timestamp);

        assertNotNull(timed);
        assertNull(timed.value());
        assertEquals(1234567890L, timed.timestamp());
    }

    @Test
    public void test_of_withZeroTimestamp() {
        Timed<String> timed = Timed.of("test", 0L);

        assertNotNull(timed);
        assertEquals("test", timed.value());
        assertEquals(0L, timed.timestamp());
    }

    @Test
    public void test_of_withNegativeTimestamp() {
        Timed<String> timed = Timed.of("test", -1000L);

        assertNotNull(timed);
        assertEquals("test", timed.value());
        assertEquals(-1000L, timed.timestamp());
    }

    @Test
    public void test_timestamp() {
        long timestamp = 1609459200000L;
        Timed<String> timed = Timed.of("value", timestamp);

        assertEquals(1609459200000L, timed.timestamp());
    }

    @Test
    public void test_timestamp_withCurrentTime() {
        long beforeTime = System.currentTimeMillis();
        Timed<String> timed = Timed.of("value");
        long afterTime = System.currentTimeMillis();

        assertTrue(timed.timestamp() >= beforeTime);
        assertTrue(timed.timestamp() <= afterTime);
    }

    @Test
    public void test_value() {
        Timed<String> timed = Timed.of("test value", 1000L);

        assertEquals("test value", timed.value());
    }

    @Test
    public void test_value_withNull() {
        Timed<String> timed = Timed.of(null, 1000L);

        assertNull(timed.value());
    }

    @Test
    public void test_value_withComplexObject() {
        Pair<String, Integer> pair = Pair.of("key", 123);
        Timed<Pair<String, Integer>> timed = Timed.of(pair, 1000L);

        assertEquals(pair, timed.value());
        assertEquals("key", timed.value().left());
        assertEquals(123, timed.value().right());
    }

    @Test
    public void test_hashCode_sameValueAndTimestamp() {
        Timed<String> timed1 = Timed.of("test", 1000L);
        Timed<String> timed2 = Timed.of("test", 1000L);

        assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void test_hashCode_differentValues() {
        Timed<String> timed1 = Timed.of("test1", 1000L);
        Timed<String> timed2 = Timed.of("test2", 1000L);

        assertNotEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void test_hashCode_differentTimestamps() {
        Timed<String> timed1 = Timed.of("test", 1000L);
        Timed<String> timed2 = Timed.of("test", 2000L);

        assertNotEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void test_hashCode_withNullValue() {
        Timed<String> timed1 = Timed.of(null, 1000L);
        Timed<String> timed2 = Timed.of(null, 1000L);

        assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void test_hashCode_nullVsNonNull() {
        Timed<String> timed1 = Timed.of(null, 1000L);
        Timed<String> timed2 = Timed.of("test", 1000L);

        assertNotEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void test_hashCode_largeTimestamp() {
        long largeTimestamp = Long.MAX_VALUE;
        Timed<String> timed1 = Timed.of("test", largeTimestamp);
        Timed<String> timed2 = Timed.of("test", largeTimestamp);

        assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void test_equals_sameObject() {
        Timed<String> timed = Timed.of("test", 1000L);

        assertTrue(timed.equals(timed));
    }

    @Test
    public void test_equals_sameValueAndTimestamp() {
        Timed<String> timed1 = Timed.of("test", 1000L);
        Timed<String> timed2 = Timed.of("test", 1000L);

        assertTrue(timed1.equals(timed2));
        assertTrue(timed2.equals(timed1));
    }

    @Test
    public void test_equals_differentValues() {
        Timed<String> timed1 = Timed.of("test1", 1000L);
        Timed<String> timed2 = Timed.of("test2", 1000L);

        assertFalse(timed1.equals(timed2));
        assertFalse(timed2.equals(timed1));
    }

    @Test
    public void test_equals_differentTimestamps() {
        Timed<String> timed1 = Timed.of("test", 1000L);
        Timed<String> timed2 = Timed.of("test", 2000L);

        assertFalse(timed1.equals(timed2));
        assertFalse(timed2.equals(timed1));
    }

    @Test
    public void test_equals_bothNull() {
        Timed<String> timed1 = Timed.of(null, 1000L);
        Timed<String> timed2 = Timed.of(null, 1000L);

        assertTrue(timed1.equals(timed2));
        assertTrue(timed2.equals(timed1));
    }

    @Test
    public void test_equals_oneNullOneNonNull() {
        Timed<String> timed1 = Timed.of(null, 1000L);
        Timed<String> timed2 = Timed.of("test", 1000L);

        assertFalse(timed1.equals(timed2));
        assertFalse(timed2.equals(timed1));
    }

    @Test
    public void test_equals_withNull() {
        Timed<String> timed = Timed.of("test", 1000L);

        assertFalse(timed.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        Timed<String> timed = Timed.of("test", 1000L);
        String other = "test";

        assertFalse(timed.equals(other));
    }

    @Test
    public void test_equals_differentParameterTypes() {
        Timed<String> timed1 = Timed.of("123", 1000L);
        Timed<Integer> timed2 = Timed.of(123, 1000L);

        assertFalse(timed1.equals(timed2));
    }

    @Test
    public void test_toString_withStringValue() {
        Timed<String> timed = Timed.of("test", 1609459200000L);

        String result = timed.toString();
        assertEquals("1609459200000: test", result);
    }

    @Test
    public void test_toString_withNullValue() {
        Timed<String> timed = Timed.of(null, 1609459200000L);

        String result = timed.toString();
        assertEquals("1609459200000: null", result);
    }

    @Test
    public void test_toString_withIntegerValue() {
        Timed<Integer> timed = Timed.of(42, 1000L);

        String result = timed.toString();
        assertEquals("1000: 42", result);
    }

    @Test
    public void test_toString_withZeroTimestamp() {
        Timed<String> timed = Timed.of("test", 0L);

        String result = timed.toString();
        assertEquals("0: test", result);
    }

    @Test
    public void test_toString_withNegativeTimestamp() {
        Timed<String> timed = Timed.of("test", -1000L);

        String result = timed.toString();
        assertEquals("-1000: test", result);
    }

    @Test
    public void test_toString_withComplexObject() {
        Pair<String, Integer> pair = Pair.of("key", 123);
        Timed<Pair<String, Integer>> timed = Timed.of(pair, 5000L);

        String result = timed.toString();
        assertTrue(result.startsWith("5000: "));
        assertTrue(result.contains("key"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void test_immutability_valueNotAffectedByExternalChange() {
        StringBuilder sb = new StringBuilder("initial");
        Timed<StringBuilder> timed = Timed.of(sb, 1000L);

        sb.append(" modified");

        assertEquals("initial modified", timed.value().toString());
    }

    @Test
    public void test_multipleInstances_independent() {
        Timed<String> timed1 = Timed.of("value1", 1000L);
        Timed<String> timed2 = Timed.of("value2", 2000L);

        assertEquals("value1", timed1.value());
        assertEquals(1000L, timed1.timestamp());
        assertEquals("value2", timed2.value());
        assertEquals(2000L, timed2.timestamp());
    }

    @Test
    public void test_edgeCases_maxLongTimestamp() {
        Timed<String> timed = Timed.of("test", Long.MAX_VALUE);

        assertEquals(Long.MAX_VALUE, timed.timestamp());
        assertEquals("test", timed.value());
    }

    @Test
    public void test_edgeCases_minLongTimestamp() {
        Timed<String> timed = Timed.of("test", Long.MIN_VALUE);

        assertEquals(Long.MIN_VALUE, timed.timestamp());
        assertEquals("test", timed.value());
    }
}
