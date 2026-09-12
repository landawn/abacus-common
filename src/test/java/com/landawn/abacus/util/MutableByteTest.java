package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableByteTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals((byte) 10, MutableByte.of((byte) 10).value());
        assertEquals((byte) 0, MutableByte.of((byte) 0).value());
        assertEquals((byte) -50, MutableByte.of((byte) -50).value());
        assertEquals(Byte.MAX_VALUE, MutableByte.of(Byte.MAX_VALUE).value());
        assertEquals(Byte.MIN_VALUE, MutableByte.of(Byte.MIN_VALUE).value());
        assertEquals((byte) 25, MutableByte.of((byte) 25).getValue());
    }

    @Test
    public void testGetAndSet() {
        MutableByte num = MutableByte.of((byte) 10);
        num.setValue((byte) 30);
        assertEquals((byte) 30, num.value());
        num.setValue((byte) 0);
        assertEquals((byte) 0, num.value());
        num.setValue((byte) -75);
        assertEquals((byte) -75, num.value());

        assertEquals((byte) -75, num.getAndSet((byte) 20));
        assertEquals((byte) 20, num.value());
        assertEquals((byte) 20, num.getAndSet((byte) 20));
        assertEquals((byte) 0, num.setAndGet((byte) 0));
        assertEquals((byte) 0, num.value());
        assertEquals((byte) -25, num.setAndGet((byte) -25));
    }

    @Test
    public void testSetIf() throws Exception {
        MutableByte num = MutableByte.of((byte) 10);
        assertTrue(num.setIf(v -> v < 15, (byte) 20));
        assertEquals((byte) 20, num.value());
        assertFalse(num.setIf(v -> v < 15, (byte) 30));
        assertEquals((byte) 20, num.value());
        assertTrue(MutableByte.of((byte) 0).setIf(v -> v == 0, (byte) 5));
        assertTrue(MutableByte.of((byte) -10).setIf(v -> v < 0, (byte) 10));
    }

    @Test
    public void testIncrementDecrement() {
        MutableByte num = MutableByte.of((byte) 10);
        num.increment();
        assertEquals((byte) 11, num.value());
        num.decrement();
        assertEquals((byte) 10, num.value());

        num.setValue(Byte.MAX_VALUE);
        num.increment();
        assertEquals(Byte.MIN_VALUE, num.value());
        num.decrement();
        assertEquals(Byte.MAX_VALUE, num.value());

        assertEquals((byte) 10, MutableByte.of((byte) 10).getAndIncrement());
        MutableByte gi = MutableByte.of((byte) 10);
        assertEquals((byte) 10, gi.getAndIncrement());
        assertEquals((byte) 11, gi.value());
        assertEquals(Byte.MAX_VALUE, MutableByte.of(Byte.MAX_VALUE).getAndIncrement());
        MutableByte overflow = MutableByte.of(Byte.MAX_VALUE);
        overflow.getAndIncrement();
        assertEquals(Byte.MIN_VALUE, overflow.value());

        MutableByte gd = MutableByte.of((byte) 10);
        assertEquals((byte) 10, gd.getAndDecrement());
        assertEquals((byte) 9, gd.value());
        MutableByte underflow = MutableByte.of(Byte.MIN_VALUE);
        assertEquals(Byte.MIN_VALUE, underflow.getAndDecrement());
        assertEquals(Byte.MAX_VALUE, underflow.value());

        MutableByte ig = MutableByte.of((byte) 10);
        assertEquals((byte) 11, ig.incrementAndGet());
        assertEquals((byte) 11, ig.value());
        assertEquals(Byte.MIN_VALUE, MutableByte.of(Byte.MAX_VALUE).incrementAndGet());

        MutableByte dg = MutableByte.of((byte) 10);
        assertEquals((byte) 9, dg.decrementAndGet());
        assertEquals((byte) 9, dg.value());
        assertEquals(Byte.MAX_VALUE, MutableByte.of(Byte.MIN_VALUE).decrementAndGet());
    }

    @Test
    public void testAddSubtract() {
        MutableByte num = MutableByte.of((byte) 10);
        num.add((byte) 5);
        assertEquals((byte) 15, num.value());
        num.add((byte) 0);
        assertEquals((byte) 15, num.value());
        num.add((byte) -5);
        assertEquals((byte) 10, num.value());
        num.subtract((byte) 3);
        assertEquals((byte) 7, num.value());
        num.subtract((byte) 0);
        assertEquals((byte) 7, num.value());
        num.subtract((byte) -5);
        assertEquals((byte) 12, num.value());

        MutableByte overflow = MutableByte.of(Byte.MAX_VALUE);
        overflow.add((byte) 1);
        assertEquals(Byte.MIN_VALUE, overflow.value());
        overflow.subtract((byte) 1);
        assertEquals(Byte.MAX_VALUE, overflow.value());

        MutableByte ga = MutableByte.of((byte) 10);
        assertEquals((byte) 10, ga.getAndAdd((byte) 5));
        assertEquals((byte) 15, ga.value());
        assertEquals((byte) 15, ga.getAndAdd((byte) 0));
        assertEquals((byte) 15, ga.getAndAdd((byte) -5));
        assertEquals((byte) 10, ga.value());
        MutableByte gaOverflow = MutableByte.of(Byte.MAX_VALUE);
        assertEquals(Byte.MAX_VALUE, gaOverflow.getAndAdd((byte) 1));
        assertEquals(Byte.MIN_VALUE, gaOverflow.value());

        MutableByte ag = MutableByte.of((byte) 10);
        assertEquals((byte) 15, ag.addAndGet((byte) 5));
        assertEquals((byte) 15, ag.value());
        assertEquals((byte) 15, ag.addAndGet((byte) 0));
        assertEquals((byte) 5, ag.addAndGet((byte) -10));
        assertEquals(Byte.MIN_VALUE + 1, MutableByte.of(Byte.MAX_VALUE).addAndGet((byte) 2));
    }

    @Test
    public void testNumberConversions() {
        MutableByte num = MutableByte.of((byte) 100);
        assertEquals((byte) 100, num.byteValue());
        assertEquals((short) 100, num.shortValue());
        assertEquals(100, num.intValue());
        assertEquals(100L, num.longValue());
        assertEquals(100.0f, num.floatValue(), 0.0f);
        assertEquals(100.0, num.doubleValue(), 0.0);

        MutableByte neg = MutableByte.of((byte) -128);
        assertEquals((byte) -128, neg.byteValue());
        assertEquals((short) -128, neg.shortValue());
        assertEquals(-128, neg.intValue());
        assertEquals(-128L, neg.longValue());
        assertEquals(-128.0f, neg.floatValue(), 0.0f);
        assertEquals(-128.0, neg.doubleValue(), 0.0);
        assertEquals(Byte.MAX_VALUE, MutableByte.of(Byte.MAX_VALUE).byteValue());
    }

    @Test
    public void testCompareTo() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 20);
        MutableByte c = MutableByte.of((byte) 10);
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(c));
        assertTrue(MutableByte.of((byte) -10).compareTo(MutableByte.of((byte) 10)) < 0);
        assertTrue(MutableByte.of((byte) -10).compareTo(MutableByte.of((byte) -5)) < 0);
        assertTrue(MutableByte.of(Byte.MIN_VALUE).compareTo(MutableByte.of(Byte.MAX_VALUE)) < 0);
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 10);
        MutableByte c = MutableByte.of((byte) 20);
        assertEquals(a, a);
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("10"));
        assertFalse(a.equals(Integer.valueOf(10)));
        assertTrue(MutableByte.of((byte) 0).equals(MutableByte.of((byte) 0)));
        assertTrue(MutableByte.of((byte) -50).equals(MutableByte.of((byte) -50)));
        assertTrue(MutableByte.of(Byte.MIN_VALUE).equals(MutableByte.of(Byte.MIN_VALUE)));
        assertFalse(MutableByte.of(Byte.MIN_VALUE).equals(MutableByte.of(Byte.MAX_VALUE)));

        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(42, MutableByte.of((byte) 42).hashCode());
        assertEquals(-10, MutableByte.of((byte) -10).hashCode());
        assertEquals(0, MutableByte.of((byte) 0).hashCode());
        assertEquals(Byte.MIN_VALUE, MutableByte.of(Byte.MIN_VALUE).hashCode());
        assertEquals(Byte.MAX_VALUE, MutableByte.of(Byte.MAX_VALUE).hashCode());

        assertEquals("42", MutableByte.of((byte) 42).toString());
        assertEquals("0", MutableByte.of((byte) 0).toString());
        assertEquals("-75", MutableByte.of((byte) -75).toString());
        assertEquals("-128", MutableByte.of(Byte.MIN_VALUE).toString());
        assertEquals("127", MutableByte.of(Byte.MAX_VALUE).toString());
    }
}
