package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class EvictionPolicy100Test extends TestBase {

    @Test
    public void testEnumValues() {
        EvictionPolicy[] values = EvictionPolicy.values();
        assertEquals(3, values.length);

        assertEquals(EvictionPolicy.LAST_ACCESS_TIME, values[0]);
        assertEquals(EvictionPolicy.EXPIRATION_TIME, values[1]);
        assertEquals(EvictionPolicy.ACCESS_COUNT, values[2]);
    }

    @Test
    public void testValueOf() {
        assertEquals(EvictionPolicy.LAST_ACCESS_TIME, EvictionPolicy.valueOf("LAST_ACCESS_TIME"));
        assertEquals(EvictionPolicy.EXPIRATION_TIME, EvictionPolicy.valueOf("EXPIRATION_TIME"));
        assertEquals(EvictionPolicy.ACCESS_COUNT, EvictionPolicy.valueOf("ACCESS_COUNT"));
    }

    @Test
    public void testValueOfWithInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> EvictionPolicy.valueOf("INVALID_POLICY"));
        assertThrows(IllegalArgumentException.class, () -> EvictionPolicy.valueOf(""));
        assertThrows(NullPointerException.class, () -> EvictionPolicy.valueOf(null));
    }

    @Test
    public void testEnumName() {
        assertEquals("LAST_ACCESS_TIME", EvictionPolicy.LAST_ACCESS_TIME.name());
        assertEquals("EXPIRATION_TIME", EvictionPolicy.EXPIRATION_TIME.name());
        assertEquals("ACCESS_COUNT", EvictionPolicy.ACCESS_COUNT.name());
    }

    @Test
    public void testEnumOrdinal() {
        assertEquals(0, EvictionPolicy.LAST_ACCESS_TIME.ordinal());
        assertEquals(1, EvictionPolicy.EXPIRATION_TIME.ordinal());
        assertEquals(2, EvictionPolicy.ACCESS_COUNT.ordinal());
    }

    @Test
    public void testEnumToString() {
        assertEquals("LAST_ACCESS_TIME", EvictionPolicy.LAST_ACCESS_TIME.toString());
        assertEquals("EXPIRATION_TIME", EvictionPolicy.EXPIRATION_TIME.toString());
        assertEquals("ACCESS_COUNT", EvictionPolicy.ACCESS_COUNT.toString());
    }

    @Test
    public void testEnumComparison() {
        assertTrue(EvictionPolicy.LAST_ACCESS_TIME.compareTo(EvictionPolicy.EXPIRATION_TIME) < 0);
        assertTrue(EvictionPolicy.EXPIRATION_TIME.compareTo(EvictionPolicy.ACCESS_COUNT) < 0);
        assertTrue(EvictionPolicy.ACCESS_COUNT.compareTo(EvictionPolicy.LAST_ACCESS_TIME) > 0);
        assertEquals(0, EvictionPolicy.LAST_ACCESS_TIME.compareTo(EvictionPolicy.LAST_ACCESS_TIME));
    }

    @Test
    public void testEnumEquality() {
        assertEquals(EvictionPolicy.LAST_ACCESS_TIME, EvictionPolicy.LAST_ACCESS_TIME);
        assertNotEquals(EvictionPolicy.LAST_ACCESS_TIME, EvictionPolicy.EXPIRATION_TIME);
        assertNotEquals(EvictionPolicy.LAST_ACCESS_TIME, EvictionPolicy.ACCESS_COUNT);
        assertNotEquals(EvictionPolicy.EXPIRATION_TIME, EvictionPolicy.ACCESS_COUNT);
    }

    @Test
    public void testEnumSwitch() {
        for (EvictionPolicy policy : EvictionPolicy.values()) {
            String description = switch (policy) {
                case LAST_ACCESS_TIME -> "LRU";
                case EXPIRATION_TIME -> "Time-based";
                case ACCESS_COUNT -> "LFU";
            };

            switch (policy) {
                case LAST_ACCESS_TIME:
                    assertEquals("LRU", description);
                    break;
                case EXPIRATION_TIME:
                    assertEquals("Time-based", description);
                    break;
                case ACCESS_COUNT:
                    assertEquals("LFU", description);
                    break;
                default:
                    fail("Unexpected policy: " + policy);
            }
        }
    }

    @Test
    public void testEnumClass() {
        assertEquals(EvictionPolicy.class, EvictionPolicy.LAST_ACCESS_TIME.getDeclaringClass());
        assertEquals(EvictionPolicy.class, EvictionPolicy.EXPIRATION_TIME.getDeclaringClass());
        assertEquals(EvictionPolicy.class, EvictionPolicy.ACCESS_COUNT.getDeclaringClass());
    }
}
