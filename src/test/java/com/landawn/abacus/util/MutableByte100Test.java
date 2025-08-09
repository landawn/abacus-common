package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableByte100Test extends TestBase {

    @Test
    public void testOf() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals((byte) 42, mutableByte.value());
    }

    @Test
    public void testValue() {
        MutableByte mutableByte = MutableByte.of((byte) 100);
        Assertions.assertEquals((byte) 100, mutableByte.value());
    }

    @Test
    public void testGetValue() {
        MutableByte mutableByte = MutableByte.of((byte) 50);
        Assertions.assertEquals((byte) 50, mutableByte.getValue());
    }

    @Test
    public void testSetValue() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        mutableByte.setValue((byte) 20);
        Assertions.assertEquals((byte) 20, mutableByte.value());
    }

    @Test
    public void testGetAndSet() {
        MutableByte mutableByte = MutableByte.of((byte) 30);
        byte oldValue = mutableByte.getAndSet((byte) 40);
        Assertions.assertEquals((byte) 30, oldValue);
        Assertions.assertEquals((byte) 40, mutableByte.value());
    }

    @Test
    public void testSetAndGet() {
        MutableByte mutableByte = MutableByte.of((byte) 50);
        byte newValue = mutableByte.setAndGet((byte) 60);
        Assertions.assertEquals((byte) 60, newValue);
        Assertions.assertEquals((byte) 60, mutableByte.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        
        // Test when predicate returns true
        boolean updated = mutableByte.setIf((byte) 20, v -> v < 15);
        Assertions.assertTrue(updated);
        Assertions.assertEquals((byte) 20, mutableByte.value());
        
        // Test when predicate returns false
        updated = mutableByte.setIf((byte) 30, v -> v < 15);
        Assertions.assertFalse(updated);
        Assertions.assertEquals((byte) 20, mutableByte.value());
    }

    @Test
    public void testIncrement() {
        MutableByte mutableByte = MutableByte.of((byte) 5);
        mutableByte.increment();
        Assertions.assertEquals((byte) 6, mutableByte.value());
    }

    @Test
    public void testDecrement() {
        MutableByte mutableByte = MutableByte.of((byte) 5);
        mutableByte.decrement();
        Assertions.assertEquals((byte) 4, mutableByte.value());
    }

    @Test
    public void testAdd() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        mutableByte.add((byte) 5);
        Assertions.assertEquals((byte) 15, mutableByte.value());
        
        mutableByte.add((byte) -3);
        Assertions.assertEquals((byte) 12, mutableByte.value());
    }

    @Test
    public void testSubtract() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        mutableByte.subtract((byte) 3);
        Assertions.assertEquals((byte) 7, mutableByte.value());
        
        mutableByte.subtract((byte) -2);
        Assertions.assertEquals((byte) 9, mutableByte.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        byte oldValue = mutableByte.getAndIncrement();
        Assertions.assertEquals((byte) 10, oldValue);
        Assertions.assertEquals((byte) 11, mutableByte.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        byte oldValue = mutableByte.getAndDecrement();
        Assertions.assertEquals((byte) 10, oldValue);
        Assertions.assertEquals((byte) 9, mutableByte.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        byte newValue = mutableByte.incrementAndGet();
        Assertions.assertEquals((byte) 11, newValue);
        Assertions.assertEquals((byte) 11, mutableByte.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        byte newValue = mutableByte.decrementAndGet();
        Assertions.assertEquals((byte) 9, newValue);
        Assertions.assertEquals((byte) 9, mutableByte.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        byte oldValue = mutableByte.getAndAdd((byte) 5);
        Assertions.assertEquals((byte) 10, oldValue);
        Assertions.assertEquals((byte) 15, mutableByte.value());
    }

    @Test
    public void testAddAndGet() {
        MutableByte mutableByte = MutableByte.of((byte) 10);
        byte newValue = mutableByte.addAndGet((byte) 5);
        Assertions.assertEquals((byte) 15, newValue);
        Assertions.assertEquals((byte) 15, mutableByte.value());
    }

    @Test
    public void testByteValue() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals((byte) 42, mutableByte.byteValue());
    }

    @Test
    public void testShortValue() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals((short) 42, mutableByte.shortValue());
        
        mutableByte.setValue((byte) -128);
        Assertions.assertEquals((short) -128, mutableByte.shortValue());
    }

    @Test
    public void testIntValue() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals(42, mutableByte.intValue());
        
        mutableByte.setValue((byte) -128);
        Assertions.assertEquals(-128, mutableByte.intValue());
    }

    @Test
    public void testLongValue() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals(42L, mutableByte.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals(42.0f, mutableByte.floatValue());
    }

    @Test
    public void testDoubleValue() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals(42.0, mutableByte.doubleValue());
    }

    @Test
    public void testCompareTo() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 20);
        MutableByte c = MutableByte.of((byte) 10);
        
        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(c));
    }

    @Test
    public void testEquals() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 10);
        MutableByte c = MutableByte.of((byte) 20);
        
        Assertions.assertTrue(a.equals(b));
        Assertions.assertFalse(a.equals(c));
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("10"));
    }

    @Test
    public void testHashCode() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 10);
        MutableByte c = MutableByte.of((byte) 20);
        
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a.hashCode(), c.hashCode());
        Assertions.assertEquals(10, a.hashCode());
    }

    @Test
    public void testToString() {
        MutableByte mutableByte = MutableByte.of((byte) 42);
        Assertions.assertEquals("42", mutableByte.toString());
        
        mutableByte.setValue((byte) -100);
        Assertions.assertEquals("-100", mutableByte.toString());
    }

    @Test
    public void testOverflowBehavior() {
        MutableByte mutableByte = MutableByte.of((byte) 127);
        mutableByte.increment();
        Assertions.assertEquals((byte) -128, mutableByte.value());
        
        mutableByte.setValue((byte) -128);
        mutableByte.decrement();
        Assertions.assertEquals((byte) 127, mutableByte.value());
    }

    @Test
    public void testBoundaryValues() {
        MutableByte mutableByte = MutableByte.of(Byte.MIN_VALUE);
        Assertions.assertEquals(Byte.MIN_VALUE, mutableByte.value());
        
        mutableByte.setValue(Byte.MAX_VALUE);
        Assertions.assertEquals(Byte.MAX_VALUE, mutableByte.value());
    }
}