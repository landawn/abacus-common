package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MutableShort100Test extends TestBase {

    @Test
    public void testOf() {
        MutableShort mutableShort = MutableShort.of((short) 42);
        Assertions.assertEquals((short) 42, mutableShort.value());
    }

    @Test
    public void testValue() {
        MutableShort mutableShort = MutableShort.of((short) 100);
        Assertions.assertEquals((short) 100, mutableShort.value());
    }

    @Test
    public void testGetValue() {
        MutableShort mutableShort = MutableShort.of((short) 50);
        Assertions.assertEquals((short) 50, mutableShort.getValue());
    }

    @Test
    public void testSetValue() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        mutableShort.setValue((short) 20);
        Assertions.assertEquals((short) 20, mutableShort.value());
    }

    @Test
    public void testGetAndSet() {
        MutableShort mutableShort = MutableShort.of((short) 30);
        short oldValue = mutableShort.getAndSet((short) 40);
        Assertions.assertEquals((short) 30, oldValue);
        Assertions.assertEquals((short) 40, mutableShort.value());
    }

    @Test
    public void testSetAndGet() {
        MutableShort mutableShort = MutableShort.of((short) 50);
        short newValue = mutableShort.setAndGet((short) 60);
        Assertions.assertEquals((short) 60, newValue);
        Assertions.assertEquals((short) 60, mutableShort.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableShort mutableShort = MutableShort.of((short) 10);

        boolean updated = mutableShort.setIf((short) 20, v -> v < 15);
        Assertions.assertTrue(updated);
        Assertions.assertEquals((short) 20, mutableShort.value());

        updated = mutableShort.setIf((short) 30, v -> v < 15);
        Assertions.assertFalse(updated);
        Assertions.assertEquals((short) 20, mutableShort.value());
    }

    @Test
    public void testIncrement() {
        MutableShort mutableShort = MutableShort.of((short) 5);
        mutableShort.increment();
        Assertions.assertEquals((short) 6, mutableShort.value());
    }

    @Test
    public void testDecrement() {
        MutableShort mutableShort = MutableShort.of((short) 5);
        mutableShort.decrement();
        Assertions.assertEquals((short) 4, mutableShort.value());
    }

    @Test
    public void testAdd() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        mutableShort.add((short) 5);
        Assertions.assertEquals((short) 15, mutableShort.value());

        mutableShort.add((short) -3);
        Assertions.assertEquals((short) 12, mutableShort.value());
    }

    @Test
    public void testSubtract() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        mutableShort.subtract((short) 3);
        Assertions.assertEquals((short) 7, mutableShort.value());

        mutableShort.subtract((short) -2);
        Assertions.assertEquals((short) 9, mutableShort.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        short oldValue = mutableShort.getAndIncrement();
        Assertions.assertEquals((short) 10, oldValue);
        Assertions.assertEquals((short) 11, mutableShort.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        short oldValue = mutableShort.getAndDecrement();
        Assertions.assertEquals((short) 10, oldValue);
        Assertions.assertEquals((short) 9, mutableShort.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        short newValue = mutableShort.incrementAndGet();
        Assertions.assertEquals((short) 11, newValue);
        Assertions.assertEquals((short) 11, mutableShort.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        short newValue = mutableShort.decrementAndGet();
        Assertions.assertEquals((short) 9, newValue);
        Assertions.assertEquals((short) 9, mutableShort.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        short oldValue = mutableShort.getAndAdd((short) 5);
        Assertions.assertEquals((short) 10, oldValue);
        Assertions.assertEquals((short) 15, mutableShort.value());
    }

    @Test
    public void testAddAndGet() {
        MutableShort mutableShort = MutableShort.of((short) 10);
        short newValue = mutableShort.addAndGet((short) 5);
        Assertions.assertEquals((short) 15, newValue);
        Assertions.assertEquals((short) 15, mutableShort.value());
    }

    @Test
    public void testShortValue() {
        MutableShort mutableShort = MutableShort.of((short) 42);
        Assertions.assertEquals((short) 42, mutableShort.shortValue());
    }

    @Test
    public void testIntValue() {
        MutableShort mutableShort = MutableShort.of((short) 42);
        Assertions.assertEquals(42, mutableShort.intValue());

        mutableShort.setValue((short) -32768);
        Assertions.assertEquals(-32768, mutableShort.intValue());
    }

    @Test
    public void testLongValue() {
        MutableShort mutableShort = MutableShort.of((short) 42);
        Assertions.assertEquals(42L, mutableShort.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableShort mutableShort = MutableShort.of((short) 42);
        Assertions.assertEquals(42.0f, mutableShort.floatValue());
    }

    @Test
    public void testDoubleValue() {
        MutableShort mutableShort = MutableShort.of((short) 42);
        Assertions.assertEquals(42.0, mutableShort.doubleValue());
    }

    @Test
    public void testCompareTo() {
        MutableShort a = MutableShort.of((short) 10);
        MutableShort b = MutableShort.of((short) 20);
        MutableShort c = MutableShort.of((short) 10);

        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(c));
    }

    @Test
    public void testEquals() {
        MutableShort a = MutableShort.of((short) 10);
        MutableShort b = MutableShort.of((short) 10);
        MutableShort c = MutableShort.of((short) 20);

        Assertions.assertTrue(a.equals(b));
        Assertions.assertFalse(a.equals(c));
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("10"));
    }

    @Test
    public void testHashCode() {
        MutableShort a = MutableShort.of((short) 10);
        MutableShort b = MutableShort.of((short) 10);
        MutableShort c = MutableShort.of((short) 20);

        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a.hashCode(), c.hashCode());
        Assertions.assertEquals(10, a.hashCode());
    }

    @Test
    public void testToString() {
        MutableShort mutableShort = MutableShort.of((short) 42);
        Assertions.assertEquals("42", mutableShort.toString());

        mutableShort.setValue((short) -100);
        Assertions.assertEquals("-100", mutableShort.toString());
    }

    @Test
    public void testOverflowBehavior() {
        MutableShort mutableShort = MutableShort.of((short) 32767);
        mutableShort.increment();
        Assertions.assertEquals((short) -32768, mutableShort.value());

        mutableShort.setValue((short) -32768);
        mutableShort.decrement();
        Assertions.assertEquals((short) 32767, mutableShort.value());
    }

    @Test
    public void testBoundaryValues() {
        MutableShort mutableShort = MutableShort.of(Short.MIN_VALUE);
        Assertions.assertEquals(Short.MIN_VALUE, mutableShort.value());

        mutableShort.setValue(Short.MAX_VALUE);
        Assertions.assertEquals(Short.MAX_VALUE, mutableShort.value());
    }

    @Test
    public void testLargeAdditions() {
        MutableShort mutableShort = MutableShort.of((short) 30000);
        mutableShort.add((short) 30000);
        Assertions.assertEquals((short) -5536, mutableShort.value());
    }
}
