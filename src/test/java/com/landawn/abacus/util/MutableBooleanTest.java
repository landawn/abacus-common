package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableBooleanTest extends TestBase {

    @Test
    public void testOf() {
        assertTrue(MutableBoolean.of(true).value());
        assertFalse(MutableBoolean.of(false).value());
        assertTrue(MutableBoolean.of(true).getValue());
        assertFalse(MutableBoolean.of(false).getValue());
        assertTrue(MutableBoolean.of(true).isTrue());
        assertFalse(MutableBoolean.of(true).isFalse());
        assertTrue(MutableBoolean.of(false).isFalse());
        assertFalse(MutableBoolean.of(false).isTrue());
    }

    @Test
    public void testGetAndSet() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setValue(true);
        assertTrue(mb.value());
        mb.setFalse();
        assertFalse(mb.value());
        mb.setTrue();
        assertTrue(mb.value());
        mb.setTrue();
        assertTrue(mb.isTrue());
        mb.setFalse();
        assertTrue(mb.isFalse());

        assertFalse(mb.getAndSet(true));
        assertTrue(mb.value());
        assertTrue(mb.getAndSet(true));
        assertTrue(mb.setAndGet(true));
        assertFalse(mb.setAndGet(false));
        assertFalse(mb.value());
    }

    @Test
    public void testNegate() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.negate();
        assertFalse(mb.value());
        mb.negate();
        assertTrue(mb.value());

        assertTrue(mb.getAndNegate());
        assertFalse(mb.value());
        assertFalse(mb.getAndNegate());
        assertTrue(mb.value());

        assertFalse(mb.negateAndGet());
        assertFalse(mb.value());
        assertTrue(mb.negateAndGet());
        assertTrue(mb.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableBoolean mb = MutableBoolean.of(false);
        assertTrue(mb.setIf(v -> !v, true));
        assertTrue(mb.value());
        assertFalse(mb.setIf(v -> !v, false));
        assertTrue(mb.value());
        assertTrue(mb.setIf(v -> true, false));
        assertFalse(mb.value());
        assertFalse(mb.setIf(v -> false, true));
        assertFalse(mb.value());
    }

    @Test
    public void testCompareTo() {
        MutableBoolean t = MutableBoolean.of(true);
        MutableBoolean f = MutableBoolean.of(false);
        assertEquals(0, t.compareTo(t));
        assertEquals(0, t.compareTo(MutableBoolean.of(true)));
        assertEquals(0, f.compareTo(MutableBoolean.of(false)));
        assertTrue(t.compareTo(f) > 0);
        assertTrue(f.compareTo(t) < 0);
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableBoolean a = MutableBoolean.of(true);
        MutableBoolean b = MutableBoolean.of(true);
        MutableBoolean c = MutableBoolean.of(false);
        assertEquals(a, a);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, Boolean.TRUE);
        assertNotEquals(a, "true");
        assertNotEquals(a, null);
        c.setValue(true);
        assertEquals(a, c);

        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(Boolean.TRUE.hashCode(), a.hashCode());
        assertEquals(Boolean.FALSE.hashCode(), MutableBoolean.of(false).hashCode());
        a.setValue(false);
        assertEquals(Boolean.FALSE.hashCode(), a.hashCode());

        assertEquals("true", MutableBoolean.of(true).toString());
        assertEquals("false", MutableBoolean.of(false).toString());
    }

    /**
     * G54-002: value-based {@code equals}/{@code hashCode} on a mutable object - mutating a live key
     * makes the map entry unreachable in its old bucket, as the class javadoc now warns.
     */
    @Test
    public void testHashKeyHazard() {
        final MutableBoolean key = MutableBoolean.of(true);
        final Map<MutableBoolean, String> map = new HashMap<>();
        map.put(key, "v");
        assertEquals("v", map.get(MutableBoolean.of(true)));

        key.setFalse();

        assertFalse(map.containsKey(key));
        assertFalse(map.containsKey(MutableBoolean.of(true)));
        assertFalse(map.containsKey(MutableBoolean.of(false)));
        assertEquals(1, map.size()); // still there, just unreachable
    }
}
