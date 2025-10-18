package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MutableShort2025Test extends TestBase {

    @Test
    public void testOf() {
        MutableShort ms = MutableShort.of((short) 100);
        assertEquals((short) 100, ms.value());
    }

    @Test
    public void testValue() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals((short) 42, ms.value());
    }

    @Test
    public void testGetValue() {
        MutableShort ms = MutableShort.of((short) 50);
        assertEquals((short) 50, ms.getValue());
    }

    @Test
    public void testSetValue() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.setValue((short) 20);
        assertEquals((short) 20, ms.value());
    }

    @Test
    public void testGetAndSet() {
        MutableShort ms = MutableShort.of((short) 10);
        short old = ms.getAndSet((short) 20);
        assertEquals((short) 10, old);
        assertEquals((short) 20, ms.value());
    }

    @Test
    public void testSetAndGet() {
        MutableShort ms = MutableShort.of((short) 10);
        short result = ms.setAndGet((short) 30);
        assertEquals((short) 30, result);
        assertEquals((short) 30, ms.value());
    }

    @Test
    public void testSetIf_true() throws Exception {
        MutableShort ms = MutableShort.of((short) 10);
        boolean updated = ms.setIf((short) 20, v -> v < 15);
        assertTrue(updated);
        assertEquals((short) 20, ms.value());
    }

    @Test
    public void testSetIf_false() throws Exception {
        MutableShort ms = MutableShort.of((short) 10);
        boolean updated = ms.setIf((short) 20, v -> v > 15);
        assertFalse(updated);
        assertEquals((short) 10, ms.value());
    }

    @Test
    public void testIncrement() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.increment();
        assertEquals((short) 11, ms.value());
    }

    @Test
    public void testIncrement_overflow() {
        MutableShort ms = MutableShort.of(Short.MAX_VALUE);
        ms.increment();
        assertEquals(Short.MIN_VALUE, ms.value());
    }

    @Test
    public void testDecrement() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.decrement();
        assertEquals((short) 9, ms.value());
    }

    @Test
    public void testDecrement_underflow() {
        MutableShort ms = MutableShort.of(Short.MIN_VALUE);
        ms.decrement();
        assertEquals(Short.MAX_VALUE, ms.value());
    }

    @Test
    public void testAdd() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.add((short) 5);
        assertEquals((short) 15, ms.value());
    }

    @Test
    public void testAdd_negative() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.add((short) -3);
        assertEquals((short) 7, ms.value());
    }

    @Test
    public void testSubtract() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.subtract((short) 3);
        assertEquals((short) 7, ms.value());
    }

    @Test
    public void testSubtract_negative() {
        MutableShort ms = MutableShort.of((short) 10);
        ms.subtract((short) -5);
        assertEquals((short) 15, ms.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableShort ms = MutableShort.of((short) 10);
        short old = ms.getAndIncrement();
        assertEquals((short) 10, old);
        assertEquals((short) 11, ms.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableShort ms = MutableShort.of((short) 10);
        short old = ms.getAndDecrement();
        assertEquals((short) 10, old);
        assertEquals((short) 9, ms.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableShort ms = MutableShort.of((short) 10);
        short result = ms.incrementAndGet();
        assertEquals((short) 11, result);
        assertEquals((short) 11, ms.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableShort ms = MutableShort.of((short) 10);
        short result = ms.decrementAndGet();
        assertEquals((short) 9, result);
        assertEquals((short) 9, ms.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableShort ms = MutableShort.of((short) 10);
        short old = ms.getAndAdd((short) 5);
        assertEquals((short) 10, old);
        assertEquals((short) 15, ms.value());
    }

    @Test
    public void testAddAndGet() {
        MutableShort ms = MutableShort.of((short) 10);
        short result = ms.addAndGet((short) 5);
        assertEquals((short) 15, result);
        assertEquals((short) 15, ms.value());
    }

    @Test
    public void testShortValue() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals((short) 42, ms.shortValue());
    }

    @Test
    public void testIntValue() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals(42, ms.intValue());
    }

    @Test
    public void testLongValue() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals(42L, ms.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals(42.0f, ms.floatValue());
    }

    @Test
    public void testDoubleValue() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals(42.0, ms.doubleValue());
    }

    @Test
    public void testCompareTo_equal() {
        MutableShort ms1 = MutableShort.of((short) 10);
        MutableShort ms2 = MutableShort.of((short) 10);
        assertEquals(0, ms1.compareTo(ms2));
    }

    @Test
    public void testCompareTo_less() {
        MutableShort ms1 = MutableShort.of((short) 10);
        MutableShort ms2 = MutableShort.of((short) 20);
        assertTrue(ms1.compareTo(ms2) < 0);
    }

    @Test
    public void testCompareTo_greater() {
        MutableShort ms1 = MutableShort.of((short) 20);
        MutableShort ms2 = MutableShort.of((short) 10);
        assertTrue(ms1.compareTo(ms2) > 0);
    }

    @Test
    public void testEquals_same() {
        MutableShort ms = MutableShort.of((short) 10);
        assertEquals(ms, ms);
    }

    @Test
    public void testEquals_equal() {
        MutableShort ms1 = MutableShort.of((short) 10);
        MutableShort ms2 = MutableShort.of((short) 10);
        assertEquals(ms1, ms2);
    }

    @Test
    public void testEquals_notEqual() {
        MutableShort ms1 = MutableShort.of((short) 10);
        MutableShort ms2 = MutableShort.of((short) 20);
        assertNotEquals(ms1, ms2);
    }

    @Test
    public void testEquals_null() {
        MutableShort ms = MutableShort.of((short) 10);
        assertNotEquals(ms, null);
    }

    @Test
    public void testEquals_differentType() {
        MutableShort ms = MutableShort.of((short) 10);
        assertNotEquals(ms, Short.valueOf((short) 10));
    }

    @Test
    public void testHashCode_consistent() {
        MutableShort ms = MutableShort.of((short) 42);
        int hash1 = ms.hashCode();
        int hash2 = ms.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_equal() {
        MutableShort ms1 = MutableShort.of((short) 42);
        MutableShort ms2 = MutableShort.of((short) 42);
        assertEquals(ms1.hashCode(), ms2.hashCode());
    }

    @Test
    public void testToString() {
        MutableShort ms = MutableShort.of((short) 42);
        assertEquals("42", ms.toString());
    }

    @Test
    public void testToString_negative() {
        MutableShort ms = MutableShort.of((short) -42);
        assertEquals("-42", ms.toString());
    }

    @Test
    public void testIntegration_counter() {
        MutableShort counter = MutableShort.of((short) 0);
        short[] values = { 1, 2, 3, 4, 5 };
        for (short value : values) {
            counter.add(value);
        }
        assertEquals((short) 15, counter.value());
    }

    @Test
    public void testIntegration_complexOperations() {
        MutableShort ms = MutableShort.of((short) 100);

        ms.add((short) 50);
        assertEquals((short) 150, ms.value());

        short old = ms.getAndAdd((short) -30);
        assertEquals((short) 150, old);
        assertEquals((short) 120, ms.value());

        ms.increment();
        ms.increment();
        assertEquals((short) 122, ms.value());

        ms.decrement();
        assertEquals((short) 121, ms.value());
    }

    @Test
    public void testGetAndSubtract() {
        MutableShort ms = MutableShort.of((short) 100);
        short old = ms.getAndAdd((short) -30);
        assertEquals((short) 100, old);
        assertEquals((short) 70, ms.value());
    }

    @Test
    public void testIntegration_predicate() throws Exception {
        MutableShort ms = MutableShort.of((short) 50);

        boolean updated = ms.setIf((short) 100, v -> v >= 50);
        assertTrue(updated);
        assertEquals((short) 100, ms.value());

        updated = ms.setIf((short) 200, v -> v < 50);
        assertFalse(updated);
        assertEquals((short) 100, ms.value());
    }
}
