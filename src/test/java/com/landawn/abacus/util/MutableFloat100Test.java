package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MutableFloat100Test extends TestBase {

    @Test
    public void testConstructorDefault() {
        MutableFloat mf = new MutableFloat();
        Assertions.assertEquals(0.0f, mf.value());
    }

    @Test
    public void testConstructorWithValue() {
        MutableFloat mf = new MutableFloat(3.14f);
        Assertions.assertEquals(3.14f, mf.value());
    }

    @Test
    public void testOf() {
        MutableFloat mf = MutableFloat.of(42.5f);
        Assertions.assertEquals(42.5f, mf.value());
    }

    @Test
    public void testValue() {
        MutableFloat mf = MutableFloat.of(10.5f);
        Assertions.assertEquals(10.5f, mf.value());
    }

    @Test
    public void testGetValue() {
        MutableFloat mf = MutableFloat.of(20.7f);
        Assertions.assertEquals(20.7f, mf.getValue());
    }

    @Test
    public void testSetValue() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.setValue(20.7f);
        Assertions.assertEquals(20.7f, mf.value());
    }

    @Test
    public void testGetAndSet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndSet(20.7f);
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(20.7f, mf.value());
    }

    @Test
    public void testSetAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.setAndGet(20.7f);
        Assertions.assertEquals(20.7f, newVal);
        Assertions.assertEquals(20.7f, mf.value());
    }

    @Test
    public void testSetIfTrue() throws Exception {
        MutableFloat mf = MutableFloat.of(10.5f);
        boolean updated = mf.setIf(20.5f, v -> v < 15.0f);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(20.5f, mf.value());
    }

    @Test
    public void testSetIfFalse() throws Exception {
        MutableFloat mf = MutableFloat.of(20.5f);
        boolean updated = mf.setIf(30.5f, v -> v < 15.0f);
        Assertions.assertFalse(updated);
        Assertions.assertEquals(20.5f, mf.value());
    }

    @Test
    public void testIsNaN() {
        MutableFloat mf1 = MutableFloat.of(Float.NaN);
        Assertions.assertTrue(mf1.isNaN());

        MutableFloat mf2 = MutableFloat.of(10.5f);
        Assertions.assertFalse(mf2.isNaN());
    }

    @Test
    public void testIsInfinite() {
        MutableFloat mf1 = MutableFloat.of(Float.POSITIVE_INFINITY);
        Assertions.assertTrue(mf1.isInfinite());

        MutableFloat mf2 = MutableFloat.of(Float.NEGATIVE_INFINITY);
        Assertions.assertTrue(mf2.isInfinite());

        MutableFloat mf3 = MutableFloat.of(10.5f);
        Assertions.assertFalse(mf3.isInfinite());
    }

    @Test
    public void testIncrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.increment();
        Assertions.assertEquals(11.5f, mf.value());
    }

    @Test
    public void testDecrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.decrement();
        Assertions.assertEquals(9.5f, mf.value());
    }

    @Test
    public void testAdd() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.add(5.3f);
        Assertions.assertEquals(15.8f, mf.value(), 0.0001f);
    }

    @Test
    public void testSubtract() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.subtract(3.2f);
        Assertions.assertEquals(7.3f, mf.value(), 0.0001f);
    }

    @Test
    public void testGetAndIncrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndIncrement();
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(11.5f, mf.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndDecrement();
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(9.5f, mf.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.incrementAndGet();
        Assertions.assertEquals(11.5f, newVal);
        Assertions.assertEquals(11.5f, mf.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.decrementAndGet();
        Assertions.assertEquals(9.5f, newVal);
        Assertions.assertEquals(9.5f, mf.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndAdd(5.3f);
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(15.8f, mf.value(), 0.0001f);
    }

    @Test
    public void testAddAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.addAndGet(5.3f);
        Assertions.assertEquals(15.8f, newVal, 0.0001f);
        Assertions.assertEquals(15.8f, mf.value(), 0.0001f);
    }

    @Test
    public void testIntValue() {
        MutableFloat mf = MutableFloat.of(10.7f);
        Assertions.assertEquals(10, mf.intValue());
    }

    @Test
    public void testLongValue() {
        MutableFloat mf = MutableFloat.of(10.7f);
        Assertions.assertEquals(10L, mf.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableFloat mf = MutableFloat.of(10.5f);
        Assertions.assertEquals(10.5f, mf.floatValue());
    }

    @Test
    public void testDoubleValue() {
        MutableFloat mf = MutableFloat.of(10.5f);
        Assertions.assertEquals(10.5d, mf.doubleValue(), 0.0001d);
    }

    @Test
    public void testCompareTo() {
        MutableFloat mf1 = MutableFloat.of(10.5f);
        MutableFloat mf2 = MutableFloat.of(20.5f);
        MutableFloat mf3 = MutableFloat.of(10.5f);

        Assertions.assertTrue(mf1.compareTo(mf2) < 0);
        Assertions.assertTrue(mf2.compareTo(mf1) > 0);
        Assertions.assertEquals(0, mf1.compareTo(mf3));
    }

    @Test
    public void testCompareToWithNaN() {
        MutableFloat mf1 = MutableFloat.of(Float.NaN);
        MutableFloat mf2 = MutableFloat.of(10.5f);

        Assertions.assertTrue(mf1.compareTo(mf2) > 0);
        Assertions.assertTrue(mf2.compareTo(mf1) < 0);
    }

    @Test
    public void testEquals() {
        MutableFloat mf1 = MutableFloat.of(10.5f);
        MutableFloat mf2 = MutableFloat.of(10.5f);
        MutableFloat mf3 = MutableFloat.of(20.5f);

        Assertions.assertTrue(mf1.equals(mf2));
        Assertions.assertFalse(mf1.equals(mf3));
        Assertions.assertFalse(mf1.equals(null));
        Assertions.assertFalse(mf1.equals("string"));
    }

    @Test
    public void testEqualsWithNaN() {
        MutableFloat mf1 = MutableFloat.of(Float.NaN);
        MutableFloat mf2 = MutableFloat.of(Float.NaN);

        Assertions.assertTrue(mf1.equals(mf2));
    }

    @Test
    public void testEqualsWithZeros() {
        MutableFloat mf1 = MutableFloat.of(0.0f);
        MutableFloat mf2 = MutableFloat.of(-0.0f);

        Assertions.assertFalse(mf1.equals(mf2));
    }

    @Test
    public void testHashCode() {
        MutableFloat mf1 = MutableFloat.of(10.5f);
        MutableFloat mf2 = MutableFloat.of(10.5f);
        MutableFloat mf3 = MutableFloat.of(20.5f);

        Assertions.assertEquals(mf1.hashCode(), mf2.hashCode());
        Assertions.assertNotEquals(mf1.hashCode(), mf3.hashCode());
    }

    @Test
    public void testToString() {
        MutableFloat mf = MutableFloat.of(3.14f);
        String str = mf.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("3.14"));
    }
}
