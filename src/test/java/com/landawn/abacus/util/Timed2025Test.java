package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Timed2025Test extends TestBase {

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
    public void testEquals_same() {
        Timed<String> t1 = Timed.of("test", 1000L);
        Timed<String> t2 = Timed.of("test", 1000L);
        assertEquals(t1, t2);
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
    public void testHashCode_consistent() {
        Timed<String> timed = Timed.of("test", 1000L);
        assertEquals(timed.hashCode(), timed.hashCode());
    }

    @Test
    public void testToString() {
        Timed<String> timed = Timed.of("test", 1000L);
        String str = timed.toString();
        assertTrue(str.contains("1000"));
        assertTrue(str.contains("test"));
    }
}
