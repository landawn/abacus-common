package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableInt100Test extends TestBase {

    @Test
    public void testOf() {
        MutableInt mutableInt = MutableInt.of(42);
        Assertions.assertEquals(42, mutableInt.value());
    }

    @Test
    public void testValue() {
        MutableInt mutableInt = MutableInt.of(100);
        Assertions.assertEquals(100, mutableInt.value());
    }

    @Test
    public void testGetValue() {
        MutableInt mutableInt = MutableInt.of(50);
        Assertions.assertEquals(50, mutableInt.getValue());
    }

    @Test
    public void testSetValue() {
        MutableInt mutableInt = MutableInt.of(10);
        mutableInt.setValue(20);
        Assertions.assertEquals(20, mutableInt.value());
    }

    @Test
    public void testGetAndSet() {
        MutableInt mutableInt = MutableInt.of(30);
        int oldValue = mutableInt.getAndSet(40);
        Assertions.assertEquals(30, oldValue);
        Assertions.assertEquals(40, mutableInt.value());
    }

    @Test
    public void testSetAndGet() {
        MutableInt mutableInt = MutableInt.of(50);
        int newValue = mutableInt.setAndGet(60);
        Assertions.assertEquals(60, newValue);
        Assertions.assertEquals(60, mutableInt.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableInt mutableInt = MutableInt.of(10);
        
        // Test when predicate returns true
        boolean updated = mutableInt.setIf(20, v -> v < 15);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(20, mutableInt.value());
        
        // Test when predicate returns false
        updated = mutableInt.setIf(30, v -> v < 15);
        Assertions.assertFalse(updated);
        Assertions.assertEquals(20, mutableInt.value());
    }

    @Test
    public void testIncrement() {
        MutableInt mutableInt = MutableInt.of(5);
        mutableInt.increment();
        Assertions.assertEquals(6, mutableInt.value());
    }

    @Test
    public void testDecrement() {
        MutableInt mutableInt = MutableInt.of(5);
        mutableInt.decrement();
        Assertions.assertEquals(4, mutableInt.value());
    }

    @Test
    public void testAdd() {
        MutableInt mutableInt = MutableInt.of(10);
        mutableInt.add(5);
        Assertions.assertEquals(15, mutableInt.value());
        
        mutableInt.add(-3);
        Assertions.assertEquals(12, mutableInt.value());
    }

    @Test
    public void testSubtract() {
        MutableInt mutableInt = MutableInt.of(10);
        mutableInt.subtract(3);
        Assertions.assertEquals(7, mutableInt.value());
        
        mutableInt.subtract(-2);
        Assertions.assertEquals(9, mutableInt.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableInt mutableInt = MutableInt.of(10);
        int oldValue = mutableInt.getAndIncrement();
        Assertions.assertEquals(10, oldValue);
        Assertions.assertEquals(11, mutableInt.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableInt mutableInt = MutableInt.of(10);
        int oldValue = mutableInt.getAndDecrement();
        Assertions.assertEquals(10, oldValue);
        Assertions.assertEquals(9, mutableInt.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableInt mutableInt = MutableInt.of(10);
        int newValue = mutableInt.incrementAndGet();
        Assertions.assertEquals(11, newValue);
        Assertions.assertEquals(11, mutableInt.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableInt mutableInt = MutableInt.of(10);
        int newValue = mutableInt.decrementAndGet();
        Assertions.assertEquals(9, newValue);
        Assertions.assertEquals(9, mutableInt.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableInt mutableInt = MutableInt.of(10);
        int oldValue = mutableInt.getAndAdd(5);
        Assertions.assertEquals(10, oldValue);
        Assertions.assertEquals(15, mutableInt.value());
    }

    @Test
    public void testAddAndGet() {
        MutableInt mutableInt = MutableInt.of(10);
        int newValue = mutableInt.addAndGet(5);
        Assertions.assertEquals(15, newValue);
        Assertions.assertEquals(15, mutableInt.value());
    }

    @Test
    public void testIntValue() {
        MutableInt mutableInt = MutableInt.of(42);
        Assertions.assertEquals(42, mutableInt.intValue());
    }

    @Test
    public void testLongValue() {
        MutableInt mutableInt = MutableInt.of(42);
        Assertions.assertEquals(42L, mutableInt.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableInt mutableInt = MutableInt.of(42);
        Assertions.assertEquals(42.0f, mutableInt.floatValue());
    }

    @Test
    public void testDoubleValue() {
        MutableInt mutableInt = MutableInt.of(42);
        Assertions.assertEquals(42.0, mutableInt.doubleValue());
    }

    @Test
    public void testCompareTo() {
        MutableInt a = MutableInt.of(10);
        MutableInt b = MutableInt.of(20);
        MutableInt c = MutableInt.of(10);
        
        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(c));
    }

    @Test
    public void testEquals() {
        MutableInt a = MutableInt.of(10);
        MutableInt b = MutableInt.of(10);
        MutableInt c = MutableInt.of(20);
        
        Assertions.assertTrue(a.equals(b));
        Assertions.assertFalse(a.equals(c));
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("10"));
    }

    @Test
    public void testHashCode() {
        MutableInt a = MutableInt.of(10);
        MutableInt b = MutableInt.of(10);
        MutableInt c = MutableInt.of(20);
        
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a.hashCode(), c.hashCode());
        Assertions.assertEquals(10, a.hashCode());
    }

    @Test
    public void testToString() {
        MutableInt mutableInt = MutableInt.of(42);
        Assertions.assertEquals("42", mutableInt.toString());
        
        mutableInt.setValue(-100);
        Assertions.assertEquals("-100", mutableInt.toString());
    }

    @Test
    public void testOverflowBehavior() {
        MutableInt mutableInt = MutableInt.of(Integer.MAX_VALUE);
        mutableInt.increment();
        Assertions.assertEquals(Integer.MIN_VALUE, mutableInt.value());
        
        mutableInt.setValue(Integer.MIN_VALUE);
        mutableInt.decrement();
        Assertions.assertEquals(Integer.MAX_VALUE, mutableInt.value());
    }
}