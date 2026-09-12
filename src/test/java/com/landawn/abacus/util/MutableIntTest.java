package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableIntTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals(42, MutableInt.of(42).value());
        assertEquals(0, MutableInt.of(0).value());
        assertEquals(-100, MutableInt.of(-100).value());
        assertEquals(Integer.MAX_VALUE, MutableInt.of(Integer.MAX_VALUE).value());
        assertEquals(Integer.MIN_VALUE, MutableInt.of(Integer.MIN_VALUE).value());
        assertEquals(456, MutableInt.of(456).getValue());
    }

    @Test
    public void testGetAndSet() {
        MutableInt num = MutableInt.of(10);
        num.setValue(50);
        assertEquals(50, num.value());
        num.setValue(0);
        assertEquals(0, num.value());
        num.setValue(-25);
        assertEquals(-25, num.value());

        assertEquals(-25, num.getAndSet(20));
        assertEquals(20, num.value());
        assertEquals(20, num.getAndSet(15));
        assertEquals(15, num.getAndSet(10));
        assertEquals(10, num.value());
        assertEquals(30, num.setAndGet(30));
        assertEquals(30, num.value());
        assertEquals(0, num.setAndGet(0));
    }

    @Test
    public void testSetIf() throws Exception {
        MutableInt num = MutableInt.of(10);
        assertTrue(num.setIf(v -> v < 15, 20));
        assertEquals(20, num.value());
        assertFalse(num.setIf(v -> v < 15, 30));
        assertEquals(20, num.value());
        assertTrue(MutableInt.of(10).setIf(v -> v == 10, 20));
    }

    @Test
    public void testIncrementDecrement() {
        MutableInt num = MutableInt.of(10);
        num.increment();
        assertEquals(11, num.value());
        num.decrement();
        assertEquals(10, num.value());

        MutableInt overflow = MutableInt.of(Integer.MAX_VALUE);
        overflow.increment();
        assertEquals(Integer.MIN_VALUE, overflow.value());
        overflow.decrement();
        assertEquals(Integer.MAX_VALUE, overflow.value());

        MutableInt gi = MutableInt.of(10);
        assertEquals(10, gi.getAndIncrement());
        assertEquals(11, gi.value());
        MutableInt gd = MutableInt.of(10);
        assertEquals(10, gd.getAndDecrement());
        assertEquals(9, gd.value());
        MutableInt ig = MutableInt.of(10);
        assertEquals(11, ig.incrementAndGet());
        assertEquals(11, ig.value());
        MutableInt dg = MutableInt.of(10);
        assertEquals(9, dg.decrementAndGet());
        assertEquals(9, dg.value());

        MutableInt multi = MutableInt.of(0);
        assertEquals(0, multi.getAndIncrement());
        assertEquals(1, multi.getAndIncrement());
        assertEquals(3, multi.incrementAndGet());
        assertEquals(3, multi.getAndDecrement());
        assertEquals(1, multi.decrementAndGet());
        assertEquals(1, multi.value());
    }

    @Test
    public void testAddSubtract() {
        MutableInt num = MutableInt.of(10);
        num.add(5);
        assertEquals(15, num.value());
        num.add(0);
        assertEquals(15, num.value());
        num.add(-3);
        assertEquals(12, num.value());
        num.subtract(3);
        assertEquals(9, num.value());
        num.subtract(0);
        assertEquals(9, num.value());
        num.subtract(-5);
        assertEquals(14, num.value());
        num.subtract(20);
        assertEquals(-6, num.value());

        MutableInt ga = MutableInt.of(10);
        assertEquals(10, ga.getAndAdd(5));
        assertEquals(15, ga.value());
        assertEquals(15, ga.getAndAdd(0));
        assertEquals(15, ga.getAndAdd(-3));
        assertEquals(12, ga.value());
        MutableInt ag = MutableInt.of(10);
        assertEquals(15, ag.addAndGet(5));
        assertEquals(15, ag.value());
        assertEquals(15, ag.addAndGet(0));
        assertEquals(12, ag.addAndGet(-3));
        assertEquals(12, ag.value());
    }

    @Test
    public void testNumberConversions() {
        MutableInt num = MutableInt.of(42);
        assertEquals(42, num.intValue());
        assertEquals(42L, num.longValue());
        assertEquals(42.0f, num.floatValue(), 0.0001f);
        assertEquals(42.0, num.doubleValue(), 0.0001);
        assertEquals(-100, MutableInt.of(-100).intValue());
        assertEquals(-100L, MutableInt.of(-100).longValue());
        assertEquals(Integer.MAX_VALUE, MutableInt.of(Integer.MAX_VALUE).longValue());
        assertEquals(0, MutableInt.of(0).intValue());
        assertEquals(0.0f, MutableInt.of(0).floatValue(), 0.0001f);
        assertEquals(0.0, MutableInt.of(0).doubleValue(), 0.0001);
    }

    @Test
    public void testCompareTo() {
        MutableInt a = MutableInt.of(10);
        MutableInt b = MutableInt.of(20);
        MutableInt c = MutableInt.of(10);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(c));
        assertTrue(MutableInt.of(-10).compareTo(MutableInt.of(-5)) < 0);
        assertEquals(0, MutableInt.of(0).compareTo(MutableInt.of(0)));
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableInt a = MutableInt.of(42);
        MutableInt b = MutableInt.of(42);
        MutableInt c = MutableInt.of(43);
        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("42"));
        assertFalse(a.equals(Integer.valueOf(42)));
        assertTrue(MutableInt.of(0).equals(MutableInt.of(0)));
        assertTrue(MutableInt.of(-100).equals(MutableInt.of(-100)));

        assertEquals(42, a.hashCode());
        assertEquals(a.hashCode(), b.hashCode());
        a.setValue(20);
        assertEquals(20, a.hashCode());
        assertEquals(0, MutableInt.of(0).hashCode());
        assertEquals(-100, MutableInt.of(-100).hashCode());
        assertNotEquals(42, a.hashCode());

        assertEquals("42", MutableInt.of(42).toString());
        assertEquals("0", MutableInt.of(0).toString());
        assertEquals("-100", MutableInt.of(-100).toString());
        assertEquals(String.valueOf(Integer.MAX_VALUE), MutableInt.of(Integer.MAX_VALUE).toString());
        assertEquals(String.valueOf(Integer.MIN_VALUE), MutableInt.of(Integer.MIN_VALUE).toString());
    }
}
