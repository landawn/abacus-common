package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableFloatTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals(0.0f, new MutableFloat().value());
        assertEquals(3.14f, new MutableFloat(3.14f).value());
        assertEquals(42.5f, MutableFloat.of(42.5f).value());
        assertEquals(0.0f, MutableFloat.of(0.0f).value());
        assertEquals(-10.5f, MutableFloat.of(-10.5f).value());
        assertTrue(Float.isNaN(MutableFloat.of(Float.NaN).value()));
        assertEquals(Float.POSITIVE_INFINITY, MutableFloat.of(Float.POSITIVE_INFINITY).value());
        assertEquals(20.7f, MutableFloat.of(20.7f).getValue());
    }

    @Test
    public void testGetAndSet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.setValue(20.7f);
        assertEquals(20.7f, mf.value());
        mf.setValue(Float.NaN);
        assertTrue(Float.isNaN(mf.value()));
        mf.setValue(Float.POSITIVE_INFINITY);
        assertEquals(Float.POSITIVE_INFINITY, mf.value());
        mf.setValue(Float.NEGATIVE_INFINITY);
        assertEquals(Float.NEGATIVE_INFINITY, mf.value());

        assertEquals(Float.NEGATIVE_INFINITY, mf.getAndSet(20.7f));
        assertEquals(20.7f, mf.value());
        assertEquals(100.0f, mf.setAndGet(100.0f));
        assertEquals(100.0f, mf.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableFloat mf = MutableFloat.of(10.5f);
        assertTrue(mf.setIf(v -> v < 15.0f, 20.5f));
        assertEquals(20.5f, mf.value());
        assertFalse(mf.setIf(v -> v < 15.0f, 30.5f));
        assertEquals(20.5f, mf.value());

        MutableFloat price = MutableFloat.of(-10.0f);
        assertThrows(IllegalStateException.class, () -> price.setIf(p -> {
            if (p < 0) {
                throw new IllegalStateException("Negative price");
            }
            return p < 150.0f;
        }, 120.0f));
    }

    @Test
    public void testIsNaNAndInfinite() {
        assertTrue(MutableFloat.of(Float.NaN).isNaN());
        assertFalse(MutableFloat.of(10.5f).isNaN());
        assertFalse(MutableFloat.of(Float.POSITIVE_INFINITY).isNaN());
        assertTrue(MutableFloat.of(Float.POSITIVE_INFINITY).isInfinite());
        assertTrue(MutableFloat.of(Float.NEGATIVE_INFINITY).isInfinite());
        assertFalse(MutableFloat.of(10.5f).isInfinite());
        assertFalse(MutableFloat.of(Float.NaN).isInfinite());
    }

    @Test
    public void testIncrementDecrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.increment();
        assertEquals(11.5f, mf.value());
        mf.decrement();
        assertEquals(10.5f, mf.value());

        MutableFloat gi = MutableFloat.of(10.5f);
        assertEquals(10.5f, gi.getAndIncrement());
        assertEquals(11.5f, gi.value());
        MutableFloat gd = MutableFloat.of(10.5f);
        assertEquals(10.5f, gd.getAndDecrement());
        assertEquals(9.5f, gd.value());
        MutableFloat ig = MutableFloat.of(10.5f);
        assertEquals(11.5f, ig.incrementAndGet());
        assertEquals(11.5f, ig.value());
        MutableFloat dg = MutableFloat.of(10.5f);
        assertEquals(9.5f, dg.decrementAndGet());
        assertEquals(9.5f, dg.value());
    }

    @Test
    public void testAddSubtract() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.add(5.3f);
        assertEquals(15.8f, mf.value(), 0.0001f);
        mf.subtract(3.2f);
        assertEquals(12.6f, mf.value(), 0.0001f);

        MutableFloat large = MutableFloat.of(Float.MAX_VALUE);
        large.add(Float.MAX_VALUE);
        assertEquals(Float.POSITIVE_INFINITY, large.value());
        MutableFloat small = MutableFloat.of(-Float.MAX_VALUE);
        small.subtract(Float.MAX_VALUE);
        assertEquals(Float.NEGATIVE_INFINITY, small.value());

        MutableFloat ga = MutableFloat.of(10.5f);
        assertEquals(10.5f, ga.getAndAdd(5.3f));
        assertEquals(15.8f, ga.value(), 0.0001f);
        MutableFloat ag = MutableFloat.of(10.5f);
        assertEquals(15.8f, ag.addAndGet(5.3f), 0.0001f);
        assertEquals(15.8f, ag.value(), 0.0001f);
    }

    @Test
    public void testNumberConversions() {
        MutableFloat mf = MutableFloat.of(42.7f);
        assertEquals(42, mf.intValue());
        assertEquals(42L, mf.longValue());
        assertEquals(42.7f, mf.floatValue());
        assertEquals(42.7, mf.doubleValue(), 0.0001);
        assertEquals(-42, MutableFloat.of(-42.7f).intValue());
        assertEquals(-42L, MutableFloat.of(-42.7f).longValue());
        assertEquals(100, MutableFloat.of(100.9f).intValue());
    }

    @Test
    public void testCompareTo() {
        MutableFloat a = MutableFloat.of(10.5f);
        MutableFloat b = MutableFloat.of(20.5f);
        MutableFloat c = MutableFloat.of(10.5f);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(c));
        assertTrue(MutableFloat.of(Float.NaN).compareTo(MutableFloat.of(10.5f)) > 0);
        assertEquals(0, MutableFloat.of(Float.NaN).compareTo(MutableFloat.of(Float.NaN)));
        assertTrue(a.compareTo(MutableFloat.of(Float.POSITIVE_INFINITY)) < 0);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableFloat a = MutableFloat.of(10.5f);
        MutableFloat b = MutableFloat.of(10.5f);
        MutableFloat c = MutableFloat.of(20.5f);
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("10.5"));
        assertTrue(MutableFloat.of(Float.NaN).equals(MutableFloat.of(Float.NaN)));
        assertFalse(MutableFloat.of(0.0f).equals(MutableFloat.of(-0.0f)));

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
        assertEquals(Float.hashCode(10.5f), a.hashCode());
        assertTrue(MutableFloat.of(3.14f).toString().contains("3.14"));
        assertTrue(MutableFloat.of(100.0f).toString().contains("100"));
    }
}
