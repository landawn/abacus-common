package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableLongTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals(42L, MutableLong.of(42L).value());
        assertEquals(0L, MutableLong.of(0L).value());
        assertEquals(-100L, MutableLong.of(-100L).value());
        assertEquals(Long.MAX_VALUE, MutableLong.of(Long.MAX_VALUE).value());
        assertEquals(Long.MIN_VALUE, MutableLong.of(Long.MIN_VALUE).value());
        assertEquals(99999L, MutableLong.of(99999L).getValue());
    }

    @Test
    public void testGetAndSet() {
        MutableLong ml = MutableLong.of(10L);
        ml.setValue(20L);
        assertEquals(20L, ml.value());
        ml.setValue(-50L);
        assertEquals(-50L, ml.value());

        assertEquals(-50L, ml.getAndSet(20L));
        assertEquals(20L, ml.value());
        assertEquals(42L, MutableLong.of(42L).getAndSet(42L));
        assertEquals(20L, ml.setAndGet(20L));
        assertEquals(15L, ml.setAndGet(15L));
        assertEquals(15L, ml.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableLong ml = MutableLong.of(10L);
        assertTrue(ml.setIf(v -> v < 15L, 20L));
        assertEquals(20L, ml.value());
        assertFalse(ml.setIf(v -> v < 15L, 30L));
        assertEquals(20L, ml.value());
        assertTrue(MutableLong.of(10L).setIf(v -> v == 10L, 20L));
    }

    @Test
    public void testIncrementDecrement() {
        MutableLong ml = MutableLong.of(10L);
        ml.increment();
        assertEquals(11L, ml.value());
        ml.decrement();
        assertEquals(10L, ml.value());

        MutableLong overflow = MutableLong.of(Long.MAX_VALUE);
        overflow.increment();
        assertEquals(Long.MIN_VALUE, overflow.value());
        overflow.decrement();
        assertEquals(Long.MAX_VALUE, overflow.value());

        MutableLong gi = MutableLong.of(10L);
        assertEquals(10L, gi.getAndIncrement());
        assertEquals(11L, gi.value());
        MutableLong gd = MutableLong.of(10L);
        assertEquals(10L, gd.getAndDecrement());
        assertEquals(9L, gd.value());
        MutableLong ig = MutableLong.of(10L);
        assertEquals(11L, ig.incrementAndGet());
        assertEquals(11L, ig.value());
        MutableLong dg = MutableLong.of(10L);
        assertEquals(9L, dg.decrementAndGet());
        assertEquals(9L, dg.value());

        MutableLong multi = MutableLong.of(0L);
        assertEquals(0L, multi.getAndIncrement());
        assertEquals(1L, multi.getAndIncrement());
        assertEquals(3L, multi.incrementAndGet());
        assertEquals(3L, multi.value());
    }

    @Test
    public void testAddSubtract() {
        MutableLong ml = MutableLong.of(10L);
        ml.add(5L);
        assertEquals(15L, ml.value());
        ml.add(0L);
        assertEquals(15L, ml.value());
        ml.add(-3L);
        assertEquals(12L, ml.value());
        ml.subtract(3L);
        assertEquals(9L, ml.value());
        ml.subtract(0L);
        assertEquals(9L, ml.value());
        ml.subtract(-5L);
        assertEquals(14L, ml.value());
        ml.subtract(20L);
        assertEquals(-6L, ml.value());

        MutableLong large = MutableLong.of(0L);
        large.add(Long.MAX_VALUE / 2);
        large.add(Long.MAX_VALUE / 2);
        assertEquals(Long.MAX_VALUE - 1, large.value());

        MutableLong ga = MutableLong.of(10L);
        assertEquals(10L, ga.getAndAdd(5L));
        assertEquals(15L, ga.value());
        assertEquals(15L, ga.getAndAdd(0L));
        assertEquals(15L, ga.getAndAdd(-3L));
        assertEquals(12L, ga.value());
        MutableLong ag = MutableLong.of(10L);
        assertEquals(15L, ag.addAndGet(5L));
        assertEquals(15L, ag.value());
        assertEquals(15L, ag.addAndGet(0L));
        assertEquals(12L, ag.addAndGet(-3L));
        assertEquals(12L, ag.value());
    }

    @Test
    public void testNumberConversions() {
        MutableLong ml = MutableLong.of(42L);
        assertEquals(42, ml.intValue());
        assertEquals(42L, ml.longValue());
        assertEquals(42.0f, ml.floatValue());
        assertEquals(42.0, ml.doubleValue());
        assertEquals(-1, MutableLong.of(Long.MAX_VALUE).intValue());
        assertEquals(Integer.MAX_VALUE, MutableLong.of(Integer.MAX_VALUE).intValue());
        assertEquals(-100, MutableLong.of(-100L).intValue());
        assertEquals(Long.MAX_VALUE, MutableLong.of(Long.MAX_VALUE).longValue());
        assertEquals(Long.MIN_VALUE, MutableLong.of(Long.MIN_VALUE).longValue());
        assertEquals(-200.0f, MutableLong.of(-200L).floatValue(), 0.0001f);
        assertTrue(MutableLong.of(Long.MAX_VALUE).floatValue() > 0);
        assertEquals(Long.MAX_VALUE, MutableLong.of(Long.MAX_VALUE).doubleValue(), 0.0001);
        assertEquals(-987654321.0, MutableLong.of(-987654321L).doubleValue(), 0.0001);
    }

    @Test
    public void testCompareTo() {
        MutableLong a = MutableLong.of(10L);
        MutableLong b = MutableLong.of(20L);
        MutableLong c = MutableLong.of(10L);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(c));
        assertTrue(MutableLong.of(-5L).compareTo(MutableLong.of(5L)) < 0);
        assertTrue(MutableLong.of(-10L).compareTo(MutableLong.of(-20L)) > 0);
        assertTrue(MutableLong.of(Long.MAX_VALUE).compareTo(MutableLong.of(Long.MIN_VALUE)) > 0);
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableLong a = MutableLong.of(42L);
        MutableLong b = MutableLong.of(42L);
        MutableLong c = MutableLong.of(43L);
        assertTrue(a.equals(a));
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("42"));
        assertFalse(a.equals(42L));
        assertTrue(MutableLong.of(-100L).equals(MutableLong.of(-100L)));
        assertTrue(MutableLong.of(0L).equals(MutableLong.of(0L)));

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
        assertEquals(Long.hashCode(10L), MutableLong.of(10L).hashCode());
        assertEquals(Long.hashCode(0L), MutableLong.of(0L).hashCode());
        assertEquals(Long.hashCode(Long.MAX_VALUE), MutableLong.of(Long.MAX_VALUE).hashCode());
        assertEquals(Long.hashCode(Long.MIN_VALUE), MutableLong.of(Long.MIN_VALUE).hashCode());

        assertEquals("42", MutableLong.of(42L).toString());
        assertEquals("-100", MutableLong.of(-100L).toString());
        assertEquals("0", MutableLong.of(0L).toString());
        assertEquals(String.valueOf(Long.MAX_VALUE), MutableLong.of(Long.MAX_VALUE).toString());
        assertEquals(String.valueOf(Long.MIN_VALUE), MutableLong.of(Long.MIN_VALUE).toString());
    }
}
