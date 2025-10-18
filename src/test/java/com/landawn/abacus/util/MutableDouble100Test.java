package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MutableDouble100Test extends TestBase {

    @Test
    public void testOf() {
        MutableDouble mutableDouble = MutableDouble.of(42.5);
        Assertions.assertEquals(42.5, mutableDouble.value());
    }

    @Test
    public void testValue() {
        MutableDouble mutableDouble = MutableDouble.of(100.75);
        Assertions.assertEquals(100.75, mutableDouble.value());
    }

    @Test
    public void testGetValue() {
        MutableDouble mutableDouble = MutableDouble.of(50.25);
        Assertions.assertEquals(50.25, mutableDouble.getValue());
    }

    @Test
    public void testSetValue() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        mutableDouble.setValue(20.75);
        Assertions.assertEquals(20.75, mutableDouble.value());
    }

    @Test
    public void testGetAndSet() {
        MutableDouble mutableDouble = MutableDouble.of(30.5);
        double oldValue = mutableDouble.getAndSet(40.75);
        Assertions.assertEquals(30.5, oldValue);
        Assertions.assertEquals(40.75, mutableDouble.value());
    }

    @Test
    public void testSetAndGet() {
        MutableDouble mutableDouble = MutableDouble.of(50.5);
        double newValue = mutableDouble.setAndGet(60.75);
        Assertions.assertEquals(60.75, newValue);
        Assertions.assertEquals(60.75, mutableDouble.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableDouble mutableDouble = MutableDouble.of(10.5);

        boolean updated = mutableDouble.setIf(20.5, v -> v < 15.0);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(20.5, mutableDouble.value());

        updated = mutableDouble.setIf(30.5, v -> v < 15.0);
        Assertions.assertFalse(updated);
        Assertions.assertEquals(20.5, mutableDouble.value());
    }

    @Test
    public void testIsNaN() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        Assertions.assertFalse(mutableDouble.isNaN());

        mutableDouble.setValue(Double.NaN);
        Assertions.assertTrue(mutableDouble.isNaN());
    }

    @Test
    public void testIsInfinite() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        Assertions.assertFalse(mutableDouble.isInfinite());

        mutableDouble.setValue(Double.POSITIVE_INFINITY);
        Assertions.assertTrue(mutableDouble.isInfinite());

        mutableDouble.setValue(Double.NEGATIVE_INFINITY);
        Assertions.assertTrue(mutableDouble.isInfinite());
    }

    @Test
    public void testIncrement() {
        MutableDouble mutableDouble = MutableDouble.of(5.5);
        mutableDouble.increment();
        Assertions.assertEquals(6.5, mutableDouble.value());
    }

    @Test
    public void testDecrement() {
        MutableDouble mutableDouble = MutableDouble.of(5.5);
        mutableDouble.decrement();
        Assertions.assertEquals(4.5, mutableDouble.value());
    }

    @Test
    public void testAdd() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        mutableDouble.add(5.25);
        Assertions.assertEquals(15.75, mutableDouble.value());

        mutableDouble.add(-3.25);
        Assertions.assertEquals(12.5, mutableDouble.value());
    }

    @Test
    public void testSubtract() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        mutableDouble.subtract(3.25);
        Assertions.assertEquals(7.25, mutableDouble.value());

        mutableDouble.subtract(-2.25);
        Assertions.assertEquals(9.5, mutableDouble.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        double oldValue = mutableDouble.getAndIncrement();
        Assertions.assertEquals(10.5, oldValue);
        Assertions.assertEquals(11.5, mutableDouble.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        double oldValue = mutableDouble.getAndDecrement();
        Assertions.assertEquals(10.5, oldValue);
        Assertions.assertEquals(9.5, mutableDouble.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        double newValue = mutableDouble.incrementAndGet();
        Assertions.assertEquals(11.5, newValue);
        Assertions.assertEquals(11.5, mutableDouble.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        double newValue = mutableDouble.decrementAndGet();
        Assertions.assertEquals(9.5, newValue);
        Assertions.assertEquals(9.5, mutableDouble.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        double oldValue = mutableDouble.getAndAdd(5.25);
        Assertions.assertEquals(10.5, oldValue);
        Assertions.assertEquals(15.75, mutableDouble.value());
    }

    @Test
    public void testAddAndGet() {
        MutableDouble mutableDouble = MutableDouble.of(10.5);
        double newValue = mutableDouble.addAndGet(5.25);
        Assertions.assertEquals(15.75, newValue);
        Assertions.assertEquals(15.75, mutableDouble.value());
    }

    @Test
    public void testIntValue() {
        MutableDouble mutableDouble = MutableDouble.of(42.7);
        Assertions.assertEquals(42, mutableDouble.intValue());

        mutableDouble.setValue(42.3);
        Assertions.assertEquals(42, mutableDouble.intValue());

        mutableDouble.setValue(-42.7);
        Assertions.assertEquals(-42, mutableDouble.intValue());
    }

    @Test
    public void testLongValue() {
        MutableDouble mutableDouble = MutableDouble.of(42.7);
        Assertions.assertEquals(42L, mutableDouble.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableDouble mutableDouble = MutableDouble.of(42.5);
        Assertions.assertEquals(42.5f, mutableDouble.floatValue());
    }

    @Test
    public void testDoubleValue() {
        MutableDouble mutableDouble = MutableDouble.of(42.5);
        Assertions.assertEquals(42.5, mutableDouble.doubleValue());
    }

    @Test
    public void testCompareTo() {
        MutableDouble a = MutableDouble.of(10.5);
        MutableDouble b = MutableDouble.of(20.5);
        MutableDouble c = MutableDouble.of(10.5);

        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(c));

        MutableDouble nan = MutableDouble.of(Double.NaN);
        MutableDouble posInf = MutableDouble.of(Double.POSITIVE_INFINITY);
        MutableDouble negInf = MutableDouble.of(Double.NEGATIVE_INFINITY);

        Assertions.assertTrue(a.compareTo(posInf) < 0);
        Assertions.assertTrue(negInf.compareTo(a) < 0);
        Assertions.assertEquals(0, nan.compareTo(nan));
    }

    @Test
    public void testEquals() {
        MutableDouble a = MutableDouble.of(10.5);
        MutableDouble b = MutableDouble.of(10.5);
        MutableDouble c = MutableDouble.of(20.5);

        Assertions.assertTrue(a.equals(b));
        Assertions.assertFalse(a.equals(c));
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("10.5"));

        MutableDouble nan1 = MutableDouble.of(Double.NaN);
        MutableDouble nan2 = MutableDouble.of(Double.NaN);
        Assertions.assertTrue(nan1.equals(nan2));

        MutableDouble posZero = MutableDouble.of(0.0);
        MutableDouble negZero = MutableDouble.of(-0.0);
        Assertions.assertFalse(posZero.equals(negZero));
    }

    @Test
    public void testHashCode() {
        MutableDouble a = MutableDouble.of(10.5);
        MutableDouble b = MutableDouble.of(10.5);
        MutableDouble c = MutableDouble.of(20.5);

        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a.hashCode(), c.hashCode());
        Assertions.assertEquals(Double.hashCode(10.5), a.hashCode());
    }

    @Test
    public void testToString() {
        MutableDouble mutableDouble = MutableDouble.of(42.5);
        Assertions.assertNotNull(mutableDouble.toString());

        mutableDouble.setValue(-100.75);
        Assertions.assertNotNull(mutableDouble.toString());

        mutableDouble.setValue(Double.NaN);
        Assertions.assertNotNull(mutableDouble.toString());

        mutableDouble.setValue(Double.POSITIVE_INFINITY);
        Assertions.assertNotNull(mutableDouble.toString());
    }

    @Test
    public void testSpecialValues() {
        MutableDouble mutableDouble = MutableDouble.of(0.0);

        mutableDouble.setValue(0.0);
        Assertions.assertEquals(0.0, mutableDouble.value());
        Assertions.assertFalse(1.0 / mutableDouble.value() < 0);

        mutableDouble.setValue(-0.0);
        Assertions.assertEquals(-0.0, mutableDouble.value());
        Assertions.assertTrue(1.0 / mutableDouble.value() < 0);

        mutableDouble.setValue(Double.POSITIVE_INFINITY);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, mutableDouble.value());

        mutableDouble.setValue(Double.NEGATIVE_INFINITY);
        Assertions.assertEquals(Double.NEGATIVE_INFINITY, mutableDouble.value());

        mutableDouble.setValue(Double.NaN);
        Assertions.assertTrue(Double.isNaN(mutableDouble.value()));
    }
}
