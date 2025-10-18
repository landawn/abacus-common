package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MutableInt2025Test extends TestBase {

    @Test
    public void testOf() {
        MutableInt num = MutableInt.of(42);
        assertNotNull(num);
        assertEquals(42, num.value());
    }

    @Test
    public void testOfZero() {
        MutableInt num = MutableInt.of(0);
        assertEquals(0, num.value());
    }

    @Test
    public void testOfNegative() {
        MutableInt num = MutableInt.of(-100);
        assertEquals(-100, num.value());
    }

    @Test
    public void testOfMaxValue() {
        MutableInt num = MutableInt.of(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, num.value());
    }

    @Test
    public void testOfMinValue() {
        MutableInt num = MutableInt.of(Integer.MIN_VALUE);
        assertEquals(Integer.MIN_VALUE, num.value());
    }

    @Test
    public void testValue() {
        MutableInt num = MutableInt.of(123);
        assertEquals(123, num.value());
    }

    @Test
    public void testValueAfterModification() {
        MutableInt num = MutableInt.of(10);
        num.setValue(20);
        assertEquals(20, num.value());
    }

    @Test
    public void testGetValue() {
        MutableInt num = MutableInt.of(456);
        assertEquals(456, num.getValue());
    }

    @Test
    public void testSetValue() {
        MutableInt num = MutableInt.of(10);
        num.setValue(50);
        assertEquals(50, num.value());
    }

    @Test
    public void testSetValueZero() {
        MutableInt num = MutableInt.of(100);
        num.setValue(0);
        assertEquals(0, num.value());
    }

    @Test
    public void testSetValueNegative() {
        MutableInt num = MutableInt.of(10);
        num.setValue(-25);
        assertEquals(-25, num.value());
    }

    @Test
    public void testGetAndSet() {
        MutableInt num = MutableInt.of(10);
        int old = num.getAndSet(20);
        assertEquals(10, old);
        assertEquals(20, num.value());
    }

    @Test
    public void testGetAndSetMultiple() {
        MutableInt num = MutableInt.of(5);
        assertEquals(5, num.getAndSet(10));
        assertEquals(10, num.getAndSet(15));
        assertEquals(15, num.getAndSet(20));
        assertEquals(20, num.value());
    }

    @Test
    public void testSetAndGet() {
        MutableInt num = MutableInt.of(10);
        int newVal = num.setAndGet(30);
        assertEquals(30, newVal);
        assertEquals(30, num.value());
    }

    @Test
    public void testSetAndGetZero() {
        MutableInt num = MutableInt.of(100);
        assertEquals(0, num.setAndGet(0));
        assertEquals(0, num.value());
    }

    @Test
    public void testSetIfTrue() {
        MutableInt num = MutableInt.of(10);
        boolean result = num.setIf(20, v -> v < 15);
        assertTrue(result);
        assertEquals(20, num.value());
    }

    @Test
    public void testSetIfFalse() {
        MutableInt num = MutableInt.of(10);
        boolean result = num.setIf(20, v -> v > 15);
        assertFalse(result);
        assertEquals(10, num.value());
    }

    @Test
    public void testSetIfEdgeCase() {
        MutableInt num = MutableInt.of(10);
        boolean result = num.setIf(20, v -> v == 10);
        assertTrue(result);
        assertEquals(20, num.value());
    }

    @Test
    public void testIncrement() {
        MutableInt num = MutableInt.of(10);
        num.increment();
        assertEquals(11, num.value());
    }

    @Test
    public void testIncrementMultiple() {
        MutableInt num = MutableInt.of(5);
        num.increment();
        num.increment();
        num.increment();
        assertEquals(8, num.value());
    }

    @Test
    public void testIncrementFromZero() {
        MutableInt num = MutableInt.of(0);
        num.increment();
        assertEquals(1, num.value());
    }

    @Test
    public void testIncrementNegative() {
        MutableInt num = MutableInt.of(-1);
        num.increment();
        assertEquals(0, num.value());
    }

    @Test
    public void testDecrement() {
        MutableInt num = MutableInt.of(10);
        num.decrement();
        assertEquals(9, num.value());
    }

    @Test
    public void testDecrementMultiple() {
        MutableInt num = MutableInt.of(5);
        num.decrement();
        num.decrement();
        num.decrement();
        assertEquals(2, num.value());
    }

    @Test
    public void testDecrementToZero() {
        MutableInt num = MutableInt.of(1);
        num.decrement();
        assertEquals(0, num.value());
    }

    @Test
    public void testDecrementToNegative() {
        MutableInt num = MutableInt.of(0);
        num.decrement();
        assertEquals(-1, num.value());
    }

    @Test
    public void testAdd() {
        MutableInt num = MutableInt.of(10);
        num.add(5);
        assertEquals(15, num.value());
    }

    @Test
    public void testAddZero() {
        MutableInt num = MutableInt.of(10);
        num.add(0);
        assertEquals(10, num.value());
    }

    @Test
    public void testAddNegative() {
        MutableInt num = MutableInt.of(10);
        num.add(-3);
        assertEquals(7, num.value());
    }

    @Test
    public void testAddLarge() {
        MutableInt num = MutableInt.of(100);
        num.add(900);
        assertEquals(1000, num.value());
    }

    @Test
    public void testSubtract() {
        MutableInt num = MutableInt.of(10);
        num.subtract(3);
        assertEquals(7, num.value());
    }

    @Test
    public void testSubtractZero() {
        MutableInt num = MutableInt.of(10);
        num.subtract(0);
        assertEquals(10, num.value());
    }

    @Test
    public void testSubtractNegative() {
        MutableInt num = MutableInt.of(10);
        num.subtract(-5);
        assertEquals(15, num.value());
    }

    @Test
    public void testSubtractToNegative() {
        MutableInt num = MutableInt.of(5);
        num.subtract(10);
        assertEquals(-5, num.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableInt num = MutableInt.of(10);
        int old = num.getAndIncrement();
        assertEquals(10, old);
        assertEquals(11, num.value());
    }

    @Test
    public void testGetAndIncrementMultiple() {
        MutableInt num = MutableInt.of(0);
        assertEquals(0, num.getAndIncrement());
        assertEquals(1, num.getAndIncrement());
        assertEquals(2, num.getAndIncrement());
        assertEquals(3, num.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableInt num = MutableInt.of(10);
        int old = num.getAndDecrement();
        assertEquals(10, old);
        assertEquals(9, num.value());
    }

    @Test
    public void testGetAndDecrementMultiple() {
        MutableInt num = MutableInt.of(3);
        assertEquals(3, num.getAndDecrement());
        assertEquals(2, num.getAndDecrement());
        assertEquals(1, num.getAndDecrement());
        assertEquals(0, num.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableInt num = MutableInt.of(10);
        int newVal = num.incrementAndGet();
        assertEquals(11, newVal);
        assertEquals(11, num.value());
    }

    @Test
    public void testIncrementAndGetMultiple() {
        MutableInt num = MutableInt.of(0);
        assertEquals(1, num.incrementAndGet());
        assertEquals(2, num.incrementAndGet());
        assertEquals(3, num.incrementAndGet());
        assertEquals(3, num.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableInt num = MutableInt.of(10);
        int newVal = num.decrementAndGet();
        assertEquals(9, newVal);
        assertEquals(9, num.value());
    }

    @Test
    public void testDecrementAndGetMultiple() {
        MutableInt num = MutableInt.of(3);
        assertEquals(2, num.decrementAndGet());
        assertEquals(1, num.decrementAndGet());
        assertEquals(0, num.decrementAndGet());
        assertEquals(0, num.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableInt num = MutableInt.of(10);
        int old = num.getAndAdd(5);
        assertEquals(10, old);
        assertEquals(15, num.value());
    }

    @Test
    public void testGetAndAddZero() {
        MutableInt num = MutableInt.of(10);
        int old = num.getAndAdd(0);
        assertEquals(10, old);
        assertEquals(10, num.value());
    }

    @Test
    public void testGetAndAddNegative() {
        MutableInt num = MutableInt.of(10);
        int old = num.getAndAdd(-3);
        assertEquals(10, old);
        assertEquals(7, num.value());
    }

    @Test
    public void testAddAndGet() {
        MutableInt num = MutableInt.of(10);
        int newVal = num.addAndGet(5);
        assertEquals(15, newVal);
        assertEquals(15, num.value());
    }

    @Test
    public void testAddAndGetZero() {
        MutableInt num = MutableInt.of(10);
        int newVal = num.addAndGet(0);
        assertEquals(10, newVal);
        assertEquals(10, num.value());
    }

    @Test
    public void testAddAndGetNegative() {
        MutableInt num = MutableInt.of(10);
        int newVal = num.addAndGet(-3);
        assertEquals(7, newVal);
        assertEquals(7, num.value());
    }

    @Test
    public void testIntValue() {
        MutableInt num = MutableInt.of(42);
        assertEquals(42, num.intValue());
    }

    @Test
    public void testIntValueNegative() {
        MutableInt num = MutableInt.of(-100);
        assertEquals(-100, num.intValue());
    }

    @Test
    public void testIntValueZero() {
        MutableInt num = MutableInt.of(0);
        assertEquals(0, num.intValue());
    }

    @Test
    public void testLongValue() {
        MutableInt num = MutableInt.of(42);
        assertEquals(42L, num.longValue());
    }

    @Test
    public void testLongValueNegative() {
        MutableInt num = MutableInt.of(-100);
        assertEquals(-100L, num.longValue());
    }

    @Test
    public void testLongValueMaxInt() {
        MutableInt num = MutableInt.of(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, num.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableInt num = MutableInt.of(42);
        assertEquals(42.0f, num.floatValue(), 0.0001f);
    }

    @Test
    public void testFloatValueNegative() {
        MutableInt num = MutableInt.of(-100);
        assertEquals(-100.0f, num.floatValue(), 0.0001f);
    }

    @Test
    public void testFloatValueZero() {
        MutableInt num = MutableInt.of(0);
        assertEquals(0.0f, num.floatValue(), 0.0001f);
    }

    @Test
    public void testDoubleValue() {
        MutableInt num = MutableInt.of(42);
        assertEquals(42.0, num.doubleValue(), 0.0001);
    }

    @Test
    public void testDoubleValueNegative() {
        MutableInt num = MutableInt.of(-100);
        assertEquals(-100.0, num.doubleValue(), 0.0001);
    }

    @Test
    public void testDoubleValueZero() {
        MutableInt num = MutableInt.of(0);
        assertEquals(0.0, num.doubleValue(), 0.0001);
    }

    @Test
    public void testCompareToLess() {
        MutableInt num1 = MutableInt.of(10);
        MutableInt num2 = MutableInt.of(20);
        assertTrue(num1.compareTo(num2) < 0);
    }

    @Test
    public void testCompareToGreater() {
        MutableInt num1 = MutableInt.of(20);
        MutableInt num2 = MutableInt.of(10);
        assertTrue(num1.compareTo(num2) > 0);
    }

    @Test
    public void testCompareToEqual() {
        MutableInt num1 = MutableInt.of(15);
        MutableInt num2 = MutableInt.of(15);
        assertEquals(0, num1.compareTo(num2));
    }

    @Test
    public void testCompareToNegative() {
        MutableInt num1 = MutableInt.of(-10);
        MutableInt num2 = MutableInt.of(-5);
        assertTrue(num1.compareTo(num2) < 0);
    }

    @Test
    public void testCompareToZero() {
        MutableInt num1 = MutableInt.of(0);
        MutableInt num2 = MutableInt.of(0);
        assertEquals(0, num1.compareTo(num2));
    }

    @Test
    public void testEqualsTrue() {
        MutableInt num1 = MutableInt.of(42);
        MutableInt num2 = MutableInt.of(42);
        assertTrue(num1.equals(num2));
    }

    @Test
    public void testEqualsFalse() {
        MutableInt num1 = MutableInt.of(42);
        MutableInt num2 = MutableInt.of(43);
        assertFalse(num1.equals(num2));
    }

    @Test
    public void testEqualsSameInstance() {
        MutableInt num = MutableInt.of(42);
        assertTrue(num.equals(num));
    }

    @Test
    public void testEqualsNull() {
        MutableInt num = MutableInt.of(42);
        assertFalse(num.equals(null));
    }

    @Test
    public void testEqualsDifferentType() {
        MutableInt num = MutableInt.of(42);
        assertFalse(num.equals("42"));
        assertFalse(num.equals(Integer.valueOf(42)));
    }

    @Test
    public void testEqualsZero() {
        MutableInt num1 = MutableInt.of(0);
        MutableInt num2 = MutableInt.of(0);
        assertTrue(num1.equals(num2));
    }

    @Test
    public void testEqualsNegative() {
        MutableInt num1 = MutableInt.of(-100);
        MutableInt num2 = MutableInt.of(-100);
        assertTrue(num1.equals(num2));
    }

    @Test
    public void testHashCode() {
        MutableInt num = MutableInt.of(42);
        assertEquals(42, num.hashCode());
    }

    @Test
    public void testHashCodeZero() {
        MutableInt num = MutableInt.of(0);
        assertEquals(0, num.hashCode());
    }

    @Test
    public void testHashCodeNegative() {
        MutableInt num = MutableInt.of(-100);
        assertEquals(-100, num.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        MutableInt num1 = MutableInt.of(42);
        MutableInt num2 = MutableInt.of(42);
        assertEquals(num1.hashCode(), num2.hashCode());
    }

    @Test
    public void testHashCodeAfterModification() {
        MutableInt num = MutableInt.of(10);
        int hash1 = num.hashCode();
        num.setValue(20);
        int hash2 = num.hashCode();
        assertNotEquals(hash1, hash2);
        assertEquals(20, hash2);
    }

    @Test
    public void testToString() {
        MutableInt num = MutableInt.of(42);
        assertEquals("42", num.toString());
    }

    @Test
    public void testToStringZero() {
        MutableInt num = MutableInt.of(0);
        assertEquals("0", num.toString());
    }

    @Test
    public void testToStringNegative() {
        MutableInt num = MutableInt.of(-100);
        assertEquals("-100", num.toString());
    }

    @Test
    public void testToStringMaxValue() {
        MutableInt num = MutableInt.of(Integer.MAX_VALUE);
        assertEquals(String.valueOf(Integer.MAX_VALUE), num.toString());
    }

    @Test
    public void testToStringMinValue() {
        MutableInt num = MutableInt.of(Integer.MIN_VALUE);
        assertEquals(String.valueOf(Integer.MIN_VALUE), num.toString());
    }

    @Test
    public void testChainedOperations() {
        MutableInt num = MutableInt.of(10);
        num.increment();
        num.add(5);
        num.decrement();
        num.subtract(2);
        assertEquals(13, num.value());
    }

    @Test
    public void testUseInLoop() {
        MutableInt counter = MutableInt.of(0);
        for (int i = 0; i < 10; i++) {
            counter.increment();
        }
        assertEquals(10, counter.value());
    }

    @Test
    public void testGetAndSetChain() {
        MutableInt num = MutableInt.of(1);
        int val = num.getAndSet(num.getAndSet(num.getAndSet(10)));
        assertEquals(1, val);
        assertEquals(10, num.value());
    }

    @Test
    public void testOverflow() {
        MutableInt num = MutableInt.of(Integer.MAX_VALUE);
        num.increment();
        assertEquals(Integer.MIN_VALUE, num.value());
    }

    @Test
    public void testUnderflow() {
        MutableInt num = MutableInt.of(Integer.MIN_VALUE);
        num.decrement();
        assertEquals(Integer.MAX_VALUE, num.value());
    }
}
