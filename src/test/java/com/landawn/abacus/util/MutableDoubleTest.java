package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableDoubleTest extends TestBase {

    private static final double DELTA = 0.0000001;

    @Test
    public void testOf() {
        assertEquals(0.0, new MutableDouble().value(), DELTA);
        assertEquals(42.5, MutableDouble.of(42.5).value(), DELTA);
        assertEquals(0.0, MutableDouble.of(0.0).value(), DELTA);
        assertEquals(-123.456, MutableDouble.of(-123.456).value(), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, MutableDouble.of(Double.POSITIVE_INFINITY).value(), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, MutableDouble.of(Double.NEGATIVE_INFINITY).value(), DELTA);
        assertTrue(Double.isNaN(MutableDouble.of(Double.NaN).value()));
        assertEquals(Double.MAX_VALUE, MutableDouble.of(Double.MAX_VALUE).value(), DELTA);
        assertEquals(Double.MIN_VALUE, MutableDouble.of(Double.MIN_VALUE).value(), DELTA);
        assertEquals(55.5, MutableDouble.of(55.5).getValue(), DELTA);
        assertEquals(MutableDouble.of(3.14).value(), MutableDouble.of(3.14).getValue(), DELTA);
    }

    @Test
    public void testGetAndSet() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(30.7);
        assertEquals(30.7, md.value(), DELTA);
        md.setValue(Double.NaN);
        assertTrue(Double.isNaN(md.value()));
        md.setValue(0.0);
        assertEquals(0.0, md.value(), DELTA);
        md.setValue(-25.5);
        assertEquals(-25.5, md.value(), DELTA);
        md.setValue(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, md.value(), DELTA);

        assertEquals(Double.POSITIVE_INFINITY, md.getAndSet(20.7), DELTA);
        assertEquals(20.7, md.value(), DELTA);
        assertEquals(20.7, md.getAndSet(20.7), DELTA);
        assertEquals(-30.5, md.setAndGet(-30.5), DELTA);
        assertEquals(-30.5, md.value(), DELTA);
    }

    @Test
    public void testSetIf() throws Exception {
        MutableDouble md = MutableDouble.of(10.5);
        assertTrue(md.setIf(val -> val < 15.0, 20.7));
        assertEquals(20.7, md.value(), DELTA);
        assertFalse(md.setIf(val -> val < 10.0, 5.0));
        assertEquals(20.7, md.value(), DELTA);
        assertTrue(MutableDouble.of(25.5).setIf(val -> val > 20.0 && val < 30.0, 100.0));
    }

    @Test
    public void testIsNaNAndInfinite() {
        assertTrue(MutableDouble.of(Double.NaN).isNaN());
        assertFalse(MutableDouble.of(42.5).isNaN());
        assertFalse(MutableDouble.of(Double.POSITIVE_INFINITY).isNaN());
        assertFalse(MutableDouble.of(0.0).isNaN());
        MutableDouble md = MutableDouble.of(10.5);
        md.setValue(Double.NaN);
        assertTrue(md.isNaN());

        assertTrue(MutableDouble.of(Double.POSITIVE_INFINITY).isInfinite());
        assertTrue(MutableDouble.of(Double.NEGATIVE_INFINITY).isInfinite());
        assertFalse(MutableDouble.of(123.456).isInfinite());
        assertFalse(MutableDouble.of(Double.NaN).isInfinite());
        assertFalse(MutableDouble.of(Double.MAX_VALUE).isInfinite());
        MutableDouble overflow = MutableDouble.of(Double.MAX_VALUE);
        overflow.add(Double.MAX_VALUE);
        assertTrue(overflow.isInfinite());
    }

    @Test
    public void testIncrementDecrement() {
        MutableDouble md = MutableDouble.of(5.5);
        md.increment();
        assertEquals(6.5, md.value(), DELTA);
        md.decrement();
        assertEquals(5.5, md.value(), DELTA);
        MutableDouble fromZero = MutableDouble.of(0.0);
        fromZero.increment();
        assertEquals(1.0, fromZero.value(), DELTA);
        fromZero.decrement();
        fromZero.decrement();
        assertEquals(-1.0, fromZero.value(), DELTA);

        MutableDouble gi = MutableDouble.of(-1.5);
        assertEquals(-1.5, gi.getAndIncrement(), DELTA);
        assertEquals(-0.5, gi.value(), DELTA);
        MutableDouble gd = MutableDouble.of(0.5);
        assertEquals(0.5, gd.getAndDecrement(), DELTA);
        assertEquals(-0.5, gd.value(), DELTA);
        MutableDouble ig = MutableDouble.of(-2.5);
        assertEquals(-1.5, ig.incrementAndGet(), DELTA);
        assertEquals(-1.5, ig.value(), DELTA);
        MutableDouble dg = MutableDouble.of(0.3);
        assertEquals(-0.7, dg.decrementAndGet(), DELTA);
        assertEquals(-0.7, dg.value(), DELTA);
    }

    @Test
    public void testAddSubtract() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(5.5);
        assertEquals(15.5, md.value(), DELTA);
        md.add(-3.5);
        assertEquals(12.0, md.value(), DELTA);
        md.add(0.0);
        assertEquals(12.0, md.value(), DELTA);
        md.subtract(3.5);
        assertEquals(8.5, md.value(), DELTA);
        md.subtract(-5.0);
        assertEquals(13.5, md.value(), DELTA);
        md.add(Double.NaN);
        assertTrue(md.isNaN());

        MutableDouble inf = MutableDouble.of(Double.POSITIVE_INFINITY);
        inf.add(100.0);
        assertEquals(Double.POSITIVE_INFINITY, inf.value(), DELTA);

        MutableDouble ga = MutableDouble.of(10.0);
        assertEquals(10.0, ga.getAndAdd(5.5), DELTA);
        assertEquals(15.5, ga.value(), DELTA);
        assertEquals(15.5, ga.getAndAdd(-3.5), DELTA);
        assertEquals(12.0, ga.value(), DELTA);
        MutableDouble ag = MutableDouble.of(10.0);
        assertEquals(15.5, ag.addAndGet(5.5), DELTA);
        assertEquals(15.5, ag.value(), DELTA);
        assertEquals(-5.5, MutableDouble.of(5.0).addAndGet(-10.5), DELTA);
    }

    @Test
    public void testNumberConversions() {
        MutableDouble md = MutableDouble.of(42.7);
        assertEquals(42, md.intValue());
        assertEquals(42L, md.longValue());
        assertEquals(42.7f, md.floatValue(), 0.0001f);
        assertEquals(42.7, md.doubleValue(), DELTA);
        assertEquals(-42, MutableDouble.of(-42.7).intValue());
        assertEquals(0, MutableDouble.of(0.999).intValue());
        assertEquals(99, MutableDouble.of(99.99).intValue());
        assertEquals(123456L, MutableDouble.of(123456.789).longValue());
        assertEquals(1234567890123L, MutableDouble.of(1234567890123.456).longValue());
        assertEquals(-9876L, MutableDouble.of(-9876.543).longValue());
        assertEquals(md.value(), md.doubleValue(), DELTA);
    }

    @Test
    public void testCompareTo() {
        MutableDouble a = MutableDouble.of(42.5);
        MutableDouble b = MutableDouble.of(50.5);
        MutableDouble c = MutableDouble.of(42.5);
        assertEquals(0, a.compareTo(c));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(MutableDouble.of(Double.NaN).compareTo(MutableDouble.of(10.0)) > 0);
        assertEquals(0, MutableDouble.of(Double.NaN).compareTo(MutableDouble.of(Double.NaN)));
        assertTrue(MutableDouble.of(-10.5).compareTo(MutableDouble.of(-5.5)) < 0);
        assertTrue(MutableDouble.of(Double.POSITIVE_INFINITY).compareTo(MutableDouble.of(1000.0)) > 0);
        assertTrue(MutableDouble.of(-0.0).compareTo(MutableDouble.of(0.0)) < 0);
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableDouble a = MutableDouble.of(42.5);
        MutableDouble b = MutableDouble.of(42.5);
        MutableDouble c = MutableDouble.of(43.5);
        assertEquals(a, a);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertNotEquals(a, "42.5");
        assertNotEquals(a, null);
        assertEquals(MutableDouble.of(Double.NaN), MutableDouble.of(Double.NaN));
        assertNotEquals(MutableDouble.of(0.0), MutableDouble.of(-0.0));
        assertEquals(MutableDouble.of(Double.POSITIVE_INFINITY), MutableDouble.of(Double.POSITIVE_INFINITY));
        assertNotEquals(MutableDouble.of(Double.POSITIVE_INFINITY), MutableDouble.of(Double.NEGATIVE_INFINITY));

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());
        assertEquals(Double.hashCode(42.5), a.hashCode());
        assertEquals(MutableDouble.of(Double.NaN).hashCode(), MutableDouble.of(Double.NaN).hashCode());
        assertNotEquals(MutableDouble.of(0.0).hashCode(), MutableDouble.of(-0.0).hashCode());

        assertTrue(a.toString().contains("42.5"));
        assertTrue(MutableDouble.of(-99.99).toString().contains("-99.99"));
        assertTrue(MutableDouble.of(0.0).toString().contains("0"));
        assertTrue(MutableDouble.of(Double.NaN).toString().contains("NaN"));
        assertTrue(MutableDouble.of(Double.POSITIVE_INFINITY).toString().contains("Infinity"));
    }
}
