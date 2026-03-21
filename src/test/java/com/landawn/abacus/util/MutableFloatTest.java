package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableFloatTest extends TestBase {

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
    public void test_of() {
        MutableFloat mf = MutableFloat.of(10.5f);
        assertEquals(10.5f, mf.value());

        MutableFloat mfZero = MutableFloat.of(0.0f);
        assertEquals(0.0f, mfZero.value());

        MutableFloat mfNegative = MutableFloat.of(-10.5f);
        assertEquals(-10.5f, mfNegative.value());

        MutableFloat mfNaN = MutableFloat.of(Float.NaN);
        assertTrue(Float.isNaN(mfNaN.value()));

        MutableFloat mfInfinity = MutableFloat.of(Float.POSITIVE_INFINITY);
        assertEquals(Float.POSITIVE_INFINITY, mfInfinity.value());
    }

    @Test
    public void test_value() {
        MutableFloat mf = MutableFloat.of(42.5f);
        assertEquals(42.5f, mf.value());

        mf.setValue(100.7f);
        assertEquals(100.7f, mf.value());
    }

    @Test
    public void testValue() {
        MutableFloat mf = MutableFloat.of(10.5f);
        Assertions.assertEquals(10.5f, mf.value());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_getValue() {
        MutableFloat mf = MutableFloat.of(42.5f);
        assertEquals(42.5f, mf.getValue());

        mf.setValue(100.7f);
        assertEquals(100.7f, mf.getValue());
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
    public void test_setValue() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.setValue(20.7f);
        assertEquals(20.7f, mf.value());

        mf.setValue(Float.NaN);
        assertTrue(Float.isNaN(mf.value()));

        mf.setValue(Float.POSITIVE_INFINITY);
        assertEquals(Float.POSITIVE_INFINITY, mf.value());

        mf.setValue(Float.NEGATIVE_INFINITY);
        assertEquals(Float.NEGATIVE_INFINITY, mf.value());
    }

    @Test
    public void test_getAndSet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndSet(20.7f);
        assertEquals(10.5f, old);
        assertEquals(20.7f, mf.value());

        MutableFloat temperature = MutableFloat.of(98.6f);
        float previousTemp = temperature.getAndSet(99.1f);
        assertEquals(98.6f, previousTemp);
        assertEquals(99.1f, temperature.value());
    }

    @Test
    public void testGetAndSet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndSet(20.7f);
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(20.7f, mf.value());
    }

    @Test
    public void test_setAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.setAndGet(20.7f);
        assertEquals(20.7f, newVal);
        assertEquals(20.7f, mf.value());

        MutableFloat score = MutableFloat.of(0.0f);
        float result = score.setAndGet(100.0f);
        assertEquals(100.0f, result);
        assertEquals(100.0f, score.value());
    }

    @Test
    public void testSetAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.setAndGet(20.7f);
        Assertions.assertEquals(20.7f, newVal);
        Assertions.assertEquals(20.7f, mf.value());
    }

    @Test
    public void test_setIf() {
        MutableFloat mf = MutableFloat.of(10.5f);
        boolean updated = mf.setIf(v -> v < 15.0f, 20.5f);
        assertTrue(updated);
        assertEquals(20.5f, mf.value());

        updated = mf.setIf(v -> v < 15.0f, 30.5f);
        assertFalse(updated);
        assertEquals(20.5f, mf.value());

        MutableFloat temperature = MutableFloat.of(98.6f);
        updated = temperature.setIf(t -> t >= 97.0f && t <= 100.0f, 99.5f);
        assertTrue(updated);
        assertEquals(99.5f, temperature.value());
    }

    @Test
    public void test_setIf_withException() {
        MutableFloat price = MutableFloat.of(100.0f);
        assertThrows(IllegalStateException.class, () -> {
            price.setValue(-10.0f);
            price.setIf(p -> {
                if (p < 0)
                    throw new IllegalStateException("Negative price");
                return p < 150.0f;
            }, 120.0f);
        });
    }

    @Test
    public void testSetIfTrue() throws Exception {
        MutableFloat mf = MutableFloat.of(10.5f);
        boolean updated = mf.setIf(v -> v < 15.0f, 20.5f);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(20.5f, mf.value());
    }

    @Test
    public void testSetIfFalse() throws Exception {
        MutableFloat mf = MutableFloat.of(20.5f);
        boolean updated = mf.setIf(v -> v < 15.0f, 30.5f);
        Assertions.assertFalse(updated);
        Assertions.assertEquals(20.5f, mf.value());
    }

    @Test
    public void testSetIf_withException() {
        MutableFloat price = MutableFloat.of(100.0f);
        Assertions.assertThrows(IllegalStateException.class, () -> {
            price.setValue(-10.0f);
            price.setIf(p -> {
                if (p < 0)
                    throw new IllegalStateException("Negative price");
                return p < 150.0f;
            }, 120.0f);
        });
    }

    @Test
    public void testIsNaN() {
        MutableFloat mf1 = MutableFloat.of(Float.NaN);
        Assertions.assertTrue(mf1.isNaN());

        MutableFloat mf2 = MutableFloat.of(10.5f);
        Assertions.assertFalse(mf2.isNaN());
    }

    @Test
    public void test_isNaN() {
        MutableFloat mf = MutableFloat.of(Float.NaN);
        assertTrue(mf.isNaN());

        mf.setValue(10.5f);
        assertFalse(mf.isNaN());

        mf.setValue(Float.POSITIVE_INFINITY);
        assertFalse(mf.isNaN());
    }

    @Test
    public void test_isInfinite() {
        MutableFloat mf = MutableFloat.of(Float.POSITIVE_INFINITY);
        assertTrue(mf.isInfinite());

        mf.setValue(Float.NEGATIVE_INFINITY);
        assertTrue(mf.isInfinite());

        mf.setValue(10.5f);
        assertFalse(mf.isInfinite());

        mf.setValue(Float.NaN);
        assertFalse(mf.isInfinite());
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
    public void test_increment() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.increment();
        assertEquals(11.5f, mf.value());

        mf.increment();
        assertEquals(12.5f, mf.value());
    }

    @Test
    public void testIncrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.increment();
        Assertions.assertEquals(11.5f, mf.value());
    }

    @Test
    public void test_decrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.decrement();
        assertEquals(9.5f, mf.value());

        mf.decrement();
        assertEquals(8.5f, mf.value());
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
    public void test_add() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.add(5.3f);
        assertEquals(15.8f, mf.value(), 0.0001f);

        MutableFloat sum = MutableFloat.of(0.0f);
        sum.add(10.0f);
        sum.add(20.0f);
        sum.add(30.0f);
        assertEquals(60.0f, sum.value());

        MutableFloat large = MutableFloat.of(Float.MAX_VALUE);
        large.add(Float.MAX_VALUE);
        assertEquals(Float.POSITIVE_INFINITY, large.value());
    }

    @Test
    public void testSubtract() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.subtract(3.2f);
        Assertions.assertEquals(7.3f, mf.value(), 0.0001f);
    }

    @Test
    public void test_subtract() {
        MutableFloat mf = MutableFloat.of(10.5f);
        mf.subtract(3.2f);
        assertEquals(7.3f, mf.value(), 0.0001f);

        MutableFloat balance = MutableFloat.of(1000.0f);
        balance.subtract(250.0f);
        assertEquals(750.0f, balance.value());

        MutableFloat small = MutableFloat.of(-Float.MAX_VALUE);
        small.subtract(Float.MAX_VALUE);
        assertEquals(Float.NEGATIVE_INFINITY, small.value());
    }

    @Test
    public void test_getAndIncrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndIncrement();
        assertEquals(10.5f, old);
        assertEquals(11.5f, mf.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndIncrement();
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(11.5f, mf.value());
    }

    @Test
    public void test_getAndDecrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndDecrement();
        assertEquals(10.5f, old);
        assertEquals(9.5f, mf.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndDecrement();
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(9.5f, mf.value());
    }

    @Test
    public void test_incrementAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.incrementAndGet();
        assertEquals(11.5f, newVal);
        assertEquals(11.5f, mf.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.incrementAndGet();
        Assertions.assertEquals(11.5f, newVal);
        Assertions.assertEquals(11.5f, mf.value());
    }

    @Test
    public void test_decrementAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.decrementAndGet();
        assertEquals(9.5f, newVal);
        assertEquals(9.5f, mf.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.decrementAndGet();
        Assertions.assertEquals(9.5f, newVal);
        Assertions.assertEquals(9.5f, mf.value());
    }

    @Test
    public void test_getAndAdd() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndAdd(5.3f);
        assertEquals(10.5f, old);
        assertEquals(15.8f, mf.value(), 0.0001f);

        MutableFloat counter = MutableFloat.of(100.0f);
        float previous = counter.getAndAdd(25.5f);
        assertEquals(100.0f, previous);
        assertEquals(125.5f, counter.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float old = mf.getAndAdd(5.3f);
        Assertions.assertEquals(10.5f, old);
        Assertions.assertEquals(15.8f, mf.value(), 0.0001f);
    }

    @Test
    public void test_addAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.addAndGet(5.3f);
        assertEquals(15.8f, newVal, 0.0001f);
        assertEquals(15.8f, mf.value(), 0.0001f);

        MutableFloat score = MutableFloat.of(85.0f);
        float result = score.addAndGet(10.0f);
        assertEquals(95.0f, result);
        assertEquals(95.0f, score.value());
    }

    @Test
    public void testAddAndGet() {
        MutableFloat mf = MutableFloat.of(10.5f);
        float newVal = mf.addAndGet(5.3f);
        Assertions.assertEquals(15.8f, newVal, 0.0001f);
        Assertions.assertEquals(15.8f, mf.value(), 0.0001f);
    }

    @Test
    public void test_intValue() {
        MutableFloat mf = MutableFloat.of(42.7f);
        assertEquals(42, mf.intValue());

        mf.setValue(100.9f);
        assertEquals(100, mf.intValue());

        mf.setValue(-42.7f);
        assertEquals(-42, mf.intValue());
    }

    @Test
    public void testIntValue() {
        MutableFloat mf = MutableFloat.of(10.7f);
        Assertions.assertEquals(10, mf.intValue());
    }

    @Test
    public void test_longValue() {
        MutableFloat mf = MutableFloat.of(42.7f);
        assertEquals(42L, mf.longValue());

        mf.setValue(100.9f);
        assertEquals(100L, mf.longValue());

        mf.setValue(-42.7f);
        assertEquals(-42L, mf.longValue());
    }

    @Test
    public void testLongValue() {
        MutableFloat mf = MutableFloat.of(10.7f);
        Assertions.assertEquals(10L, mf.longValue());
    }

    @Test
    public void test_floatValue() {
        MutableFloat mf = MutableFloat.of(42.7f);
        assertEquals(42.7f, mf.floatValue());

        mf.setValue(100.9f);
        assertEquals(100.9f, mf.floatValue());
    }

    @Test
    public void testFloatValue() {
        MutableFloat mf = MutableFloat.of(10.5f);
        Assertions.assertEquals(10.5f, mf.floatValue());
    }

    @Test
    public void test_doubleValue() {
        MutableFloat mf = MutableFloat.of(42.7f);
        assertEquals(42.7, mf.doubleValue(), 0.0001);

        mf.setValue(100.9f);
        assertEquals(100.9, mf.doubleValue(), 0.0001);
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
    public void test_compareTo() {
        MutableFloat a = MutableFloat.of(10.5f);
        MutableFloat b = MutableFloat.of(20.5f);
        MutableFloat c = MutableFloat.of(10.5f);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, a.compareTo(c));

        MutableFloat nan1 = MutableFloat.of(Float.NaN);
        MutableFloat nan2 = MutableFloat.of(Float.NaN);
        assertEquals(0, nan1.compareTo(nan2));

        MutableFloat inf = MutableFloat.of(Float.POSITIVE_INFINITY);
        assertTrue(a.compareTo(inf) < 0);
    }

    @Test
    public void test_compareTo_null() {
        MutableFloat mf = MutableFloat.of(10.5f);
        assertThrows(NullPointerException.class, () -> mf.compareTo(null));
    }

    @Test
    public void testEqualsWithNaN() {
        MutableFloat mf1 = MutableFloat.of(Float.NaN);
        MutableFloat mf2 = MutableFloat.of(Float.NaN);

        Assertions.assertTrue(mf1.equals(mf2));
    }

    @Test
    public void test_equals() {
        MutableFloat a = MutableFloat.of(10.5f);
        MutableFloat b = MutableFloat.of(10.5f);
        MutableFloat c = MutableFloat.of(20.5f);

        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("10.5"));

        MutableFloat nan1 = MutableFloat.of(Float.NaN);
        MutableFloat nan2 = MutableFloat.of(Float.NaN);
        assertTrue(nan1.equals(nan2));

        MutableFloat posZero = MutableFloat.of(0.0f);
        MutableFloat negZero = MutableFloat.of(-0.0f);
        assertFalse(posZero.equals(negZero));
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
    public void testEqualsWithZeros() {
        MutableFloat mf1 = MutableFloat.of(0.0f);
        MutableFloat mf2 = MutableFloat.of(-0.0f);

        Assertions.assertFalse(mf1.equals(mf2));
    }

    @Test
    public void test_hashCode() {
        MutableFloat a = MutableFloat.of(10.5f);
        MutableFloat b = MutableFloat.of(10.5f);
        MutableFloat c = MutableFloat.of(20.5f);

        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a.hashCode(), c.hashCode());

        assertEquals(Float.hashCode(10.5f), a.hashCode());
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
    public void test_toString() {
        MutableFloat mf = MutableFloat.of(3.14f);
        assertNotNull(mf.toString());
        assertTrue(mf.toString().contains("3.14"));

        mf.setValue(100.0f);
        assertNotNull(mf.toString());
        assertTrue(mf.toString().contains("100"));
    }

    @Test
    public void testToString() {
        MutableFloat mf = MutableFloat.of(3.14f);
        String str = mf.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("3.14"));
    }

}
