package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableShortTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals((short) 100, MutableShort.of((short) 100).value());
        assertEquals(Short.MIN_VALUE, MutableShort.of(Short.MIN_VALUE).value());
        assertEquals(Short.MAX_VALUE, MutableShort.of(Short.MAX_VALUE).value());
        assertEquals((short) 50, MutableShort.of((short) 50).getValue());
    }

    @Test
    public void testGetAndSet() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.setValue((short) 20);
        assertEquals((short) 20, ms.value());
        assertEquals((short) 20, ms.getAndSet((short) 30));
        assertEquals((short) 30, ms.value());
        assertEquals((short) 40, ms.setAndGet((short) 40));
        assertEquals((short) 40, ms.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableShort ms = MutableShort.of((short) 10);
        assertTrue(ms.setIf(v -> v < 15, (short) 20));
        assertEquals((short) 20, ms.value());
        assertFalse(ms.setIf(v -> v < 15, (short) 30));
        assertEquals((short) 20, ms.value());
        assertTrue(MutableShort.of((short) 50).setIf(v -> v >= 50, (short) 100));
    }

    @Test
    public void testIncrementDecrement() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.increment();
        assertEquals((short) 11, ms.value());
        ms.decrement();
        assertEquals((short) 10, ms.value());

        MutableShort overflow = MutableShort.of(Short.MAX_VALUE);
        overflow.increment();
        assertEquals(Short.MIN_VALUE, overflow.value());
        overflow.decrement();
        assertEquals(Short.MAX_VALUE, overflow.value());

        MutableShort gi = MutableShort.of((short) 10);
        assertEquals((short) 10, gi.getAndIncrement());
        assertEquals((short) 11, gi.value());
        MutableShort gd = MutableShort.of((short) 10);
        assertEquals((short) 10, gd.getAndDecrement());
        assertEquals((short) 9, gd.value());
        MutableShort ig = MutableShort.of((short) 10);
        assertEquals((short) 11, ig.incrementAndGet());
        assertEquals((short) 11, ig.value());
        MutableShort dg = MutableShort.of((short) 10);
        assertEquals((short) 9, dg.decrementAndGet());
        assertEquals((short) 9, dg.value());
    }

    @Test
    public void testAddSubtract() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.add((short) 5);
        assertEquals((short) 15, ms.value());
        ms.add((short) -3);
        assertEquals((short) 12, ms.value());
        ms.subtract((short) 3);
        assertEquals((short) 9, ms.value());
        ms.subtract((short) -5);
        assertEquals((short) 14, ms.value());

        MutableShort wrap = MutableShort.of((short) 30000);
        wrap.add((short) 30000);
        assertEquals((short) -5536, wrap.value());

        MutableShort ga = MutableShort.of((short) 10);
        assertEquals((short) 10, ga.getAndAdd((short) 5));
        assertEquals((short) 15, ga.value());
        assertEquals((short) 15, ga.getAndAdd((short) -30));
        assertEquals((short) -15, ga.value());
        MutableShort ag = MutableShort.of((short) 10);
        assertEquals((short) 15, ag.addAndGet((short) 5));
        assertEquals((short) 15, ag.value());
    }

    @Test
    public void testNumberConversions() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals((short) 42, ms.shortValue());
        assertEquals(42, ms.intValue());
        assertEquals(42L, ms.longValue());
        assertEquals(42.0f, ms.floatValue());
        assertEquals(42.0, ms.doubleValue());
    }

    @Test
    public void testCompareTo() {
        MutableShort a = MutableShort.of((short) 10);
        MutableShort b = MutableShort.of((short) 20);
        MutableShort c = MutableShort.of((short) 10);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(c));
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableShort a = MutableShort.of((short) 10);
        MutableShort b = MutableShort.of((short) 10);
        MutableShort c = MutableShort.of((short) 20);
        assertEquals(a, a);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, Short.valueOf((short) 10));
        assertNotEquals(a, null);
        assertNotEquals(a, "10");

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
        assertEquals(10, a.hashCode());
        assertEquals(42, MutableShort.of((short) 42).hashCode());

        assertEquals("42", MutableShort.of((short) 42).toString());
        assertEquals("-42", MutableShort.of((short) -42).toString());
    }
}
