package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableLong100Test extends TestBase {

    @Test
    public void testOf() {
        MutableLong mutableLong = MutableLong.of(42L);
        Assertions.assertEquals(42L, mutableLong.value());
    }

    @Test
    public void testValue() {
        MutableLong mutableLong = MutableLong.of(100L);
        Assertions.assertEquals(100L, mutableLong.value());
    }

    @Test
    public void testGetValue() {
        MutableLong mutableLong = MutableLong.of(50L);
        Assertions.assertEquals(50L, mutableLong.getValue());
    }

    @Test
    public void testSetValue() {
        MutableLong mutableLong = MutableLong.of(10L);
        mutableLong.setValue(20L);
        Assertions.assertEquals(20L, mutableLong.value());
    }

    @Test
    public void testGetAndSet() {
        MutableLong mutableLong = MutableLong.of(30L);
        long oldValue = mutableLong.getAndSet(40L);
        Assertions.assertEquals(30L, oldValue);
        Assertions.assertEquals(40L, mutableLong.value());
    }

    @Test
    public void testSetAndGet() {
        MutableLong mutableLong = MutableLong.of(50L);
        long newValue = mutableLong.setAndGet(60L);
        Assertions.assertEquals(60L, newValue);
        Assertions.assertEquals(60L, mutableLong.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableLong mutableLong = MutableLong.of(10L);
        
        // Test when predicate returns true
        boolean updated = mutableLong.setIf(20L, v -> v < 15L);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(20L, mutableLong.value());
        
        // Test when predicate returns false
        updated = mutableLong.setIf(30L, v -> v < 15L);
        Assertions.assertFalse(updated);
        Assertions.assertEquals(20L, mutableLong.value());
    }

    @Test
    public void testIncrement() {
        MutableLong mutableLong = MutableLong.of(5L);
        mutableLong.increment();
        Assertions.assertEquals(6L, mutableLong.value());
    }

    @Test
    public void testDecrement() {
        MutableLong mutableLong = MutableLong.of(5L);
        mutableLong.decrement();
        Assertions.assertEquals(4L, mutableLong.value());
    }

    @Test
    public void testAdd() {
        MutableLong mutableLong = MutableLong.of(10L);
        mutableLong.add(5L);
        Assertions.assertEquals(15L, mutableLong.value());
        
        mutableLong.add(-3L);
        Assertions.assertEquals(12L, mutableLong.value());
    }

    @Test
    public void testSubtract() {
        MutableLong mutableLong = MutableLong.of(10L);
        mutableLong.subtract(3L);
        Assertions.assertEquals(7L, mutableLong.value());
        
        mutableLong.subtract(-2L);
        Assertions.assertEquals(9L, mutableLong.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableLong mutableLong = MutableLong.of(10L);
        long oldValue = mutableLong.getAndIncrement();
        Assertions.assertEquals(10L, oldValue);
        Assertions.assertEquals(11L, mutableLong.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableLong mutableLong = MutableLong.of(10L);
        long oldValue = mutableLong.getAndDecrement();
        Assertions.assertEquals(10L, oldValue);
        Assertions.assertEquals(9L, mutableLong.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableLong mutableLong = MutableLong.of(10L);
        long newValue = mutableLong.incrementAndGet();
        Assertions.assertEquals(11L, newValue);
        Assertions.assertEquals(11L, mutableLong.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableLong mutableLong = MutableLong.of(10L);
        long newValue = mutableLong.decrementAndGet();
        Assertions.assertEquals(9L, newValue);
        Assertions.assertEquals(9L, mutableLong.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableLong mutableLong = MutableLong.of(10L);
        long oldValue = mutableLong.getAndAdd(5L);
        Assertions.assertEquals(10L, oldValue);
        Assertions.assertEquals(15L, mutableLong.value());
    }

    @Test
    public void testAddAndGet() {
        MutableLong mutableLong = MutableLong.of(10L);
        long newValue = mutableLong.addAndGet(5L);
        Assertions.assertEquals(15L, newValue);
        Assertions.assertEquals(15L, mutableLong.value());
    }

    @Test
    public void testIntValue() {
        MutableLong mutableLong = MutableLong.of(42L);
        Assertions.assertEquals(42, mutableLong.intValue());
        
        // Test truncation
        mutableLong.setValue(Long.MAX_VALUE);
        Assertions.assertEquals(-1, mutableLong.intValue());
    }

    @Test
    public void testLongValue() {
        MutableLong mutableLong = MutableLong.of(42L);
        Assertions.assertEquals(42L, mutableLong.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableLong mutableLong = MutableLong.of(42L);
        Assertions.assertEquals(42.0f, mutableLong.floatValue());
    }

    @Test
    public void testDoubleValue() {
        MutableLong mutableLong = MutableLong.of(42L);
        Assertions.assertEquals(42.0, mutableLong.doubleValue());
    }

    @Test
    public void testCompareTo() {
        MutableLong a = MutableLong.of(10L);
        MutableLong b = MutableLong.of(20L);
        MutableLong c = MutableLong.of(10L);
        
        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(c));
    }

    @Test
    public void testEquals() {
        MutableLong a = MutableLong.of(10L);
        MutableLong b = MutableLong.of(10L);
        MutableLong c = MutableLong.of(20L);
        
        Assertions.assertTrue(a.equals(b));
        Assertions.assertFalse(a.equals(c));
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("10"));
    }

    @Test
    public void testHashCode() {
        MutableLong a = MutableLong.of(10L);
        MutableLong b = MutableLong.of(10L);
        MutableLong c = MutableLong.of(20L);
        
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a.hashCode(), c.hashCode());
        Assertions.assertEquals(Long.hashCode(10L), a.hashCode());
    }

    @Test
    public void testToString() {
        MutableLong mutableLong = MutableLong.of(42L);
        Assertions.assertEquals("42", mutableLong.toString());
        
        mutableLong.setValue(-100L);
        Assertions.assertEquals("-100", mutableLong.toString());
    }

    @Test
    public void testOverflowBehavior() {
        MutableLong mutableLong = MutableLong.of(Long.MAX_VALUE);
        mutableLong.increment();
        Assertions.assertEquals(Long.MIN_VALUE, mutableLong.value());
        
        mutableLong.setValue(Long.MIN_VALUE);
        mutableLong.decrement();
        Assertions.assertEquals(Long.MAX_VALUE, mutableLong.value());
    }
}