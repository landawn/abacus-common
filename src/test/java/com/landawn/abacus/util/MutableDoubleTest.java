package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableDoubleTest extends TestBase {

    private static final double DELTA = 0.0000001;

    @Test
    public void testOf_NaN() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        assertTrue(Double.isNaN(md.value()));
    }

    // ---- of(double) ----

    @Test
    public void testOf() {
        MutableDouble md = MutableDouble.of(42.5);
        assertNotNull(md);
        assertEquals(42.5, md.value(), DELTA);
    }

    @Test
    public void testOf_Zero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertEquals(0.0, md.value(), DELTA);
    }

    @Test
    public void testOf_Negative() {
        MutableDouble md = MutableDouble.of(-123.456);
        assertEquals(-123.456, md.value(), DELTA);
    }

    @Test
    public void testOf_PositiveInfinity() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, md.value(), DELTA);
    }

    @Test
    public void testOf_NegativeInfinity() {
        MutableDouble md = MutableDouble.of(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, md.value(), DELTA);
    }

    @Test
    public void testOf_MaxValue() {
        MutableDouble md = MutableDouble.of(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, md.value(), DELTA);
    }

    @Test
    public void testOf_MinValue() {
        MutableDouble md = MutableDouble.of(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, md.value(), DELTA);
    }

    @Test
    public void testOverflow() {
        MutableDouble md = MutableDouble.of(Double.MAX_VALUE);
        md.add(Double.MAX_VALUE);
        assertTrue(md.isInfinite());
    }

    @Test
    public void testInfinityArithmetic() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        md.add(100.0);
        assertTrue(md.isInfinite());
        assertEquals(Double.POSITIVE_INFINITY, md.value(), DELTA);
    }

    @Test
    public void testNegativeToPositiveTransition() {
        MutableDouble md = MutableDouble.of(-5.0);
        md.add(10.0);
        assertEquals(5.0, md.value(), DELTA);
        assertTrue(md.value() > 0);
    }

    @Test
    public void testPositiveToNegativeTransition() {
        MutableDouble md = MutableDouble.of(5.0);
        md.subtract(10.0);
        assertEquals(-5.0, md.value(), DELTA);
        assertTrue(md.value() < 0);
    }

    // ---- value() ----

    @Test
    public void testValue() {
        MutableDouble md = MutableDouble.of(99.99);
        assertEquals(99.99, md.value(), DELTA);
    }

    @Test
    public void testValue_AfterModification() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(20.5);
        assertEquals(20.5, md.value(), DELTA);
    }

    // ---- getValue() ----

    @Test
    public void testGetValue() {
        MutableDouble md = MutableDouble.of(55.5);
        assertEquals(55.5, md.getValue(), DELTA);
    }

    @Test
    public void testGetValue_MatchesValue() {
        MutableDouble md = MutableDouble.of(3.14);
        assertEquals(md.value(), md.getValue(), DELTA);
    }

    // ---- setValue(double) ----

    @Test
    public void testSetValue() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(30.7);
        assertEquals(30.7, md.value(), DELTA);
    }

    @Test
    public void testSetValue_NaN() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(Double.NaN);
        assertTrue(Double.isNaN(md.value()));
    }

    @Test
    public void testSetValue_Zero() {
        MutableDouble md = MutableDouble.of(100.0);
        md.setValue(0.0);
        assertEquals(0.0, md.value(), DELTA);
    }

    @Test
    public void testSetValue_Negative() {
        MutableDouble md = MutableDouble.of(50.0);
        md.setValue(-25.5);
        assertEquals(-25.5, md.value(), DELTA);
    }

    @Test
    public void testSetValue_Infinity() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, md.value(), DELTA);
    }

    // ---- getAndSet(double) ----

    @Test
    public void testGetAndSet() {
        MutableDouble md = MutableDouble.of(10.5);
        double oldValue = md.getAndSet(20.7);
        assertEquals(10.5, oldValue, DELTA);
        assertEquals(20.7, md.value(), DELTA);
    }

    @Test
    public void testGetAndSet_SameValue() {
        MutableDouble md = MutableDouble.of(15.0);
        double oldValue = md.getAndSet(15.0);
        assertEquals(15.0, oldValue, DELTA);
        assertEquals(15.0, md.value(), DELTA);
    }

    @Test
    public void testGetAndSet_Zero() {
        MutableDouble md = MutableDouble.of(100.0);
        double oldValue = md.getAndSet(0.0);
        assertEquals(100.0, oldValue, DELTA);
        assertEquals(0.0, md.value(), DELTA);
    }

    // ---- setAndGet(double) ----

    @Test
    public void testSetAndGet() {
        MutableDouble md = MutableDouble.of(10.5);
        double newValue = md.setAndGet(20.7);
        assertEquals(20.7, newValue, DELTA);
        assertEquals(20.7, md.value(), DELTA);
    }

    @Test
    public void testSetAndGet_Negative() {
        MutableDouble md = MutableDouble.of(50.0);
        double newValue = md.setAndGet(-30.5);
        assertEquals(-30.5, newValue, DELTA);
        assertEquals(-30.5, md.value(), DELTA);
    }

    // ---- setIf(predicate, double) ----

    @Test
    public void testSetIf() throws Exception {
        MutableDouble md = MutableDouble.of(10.5);
        boolean result = md.setIf(val -> val < 15.0, 20.7);
        assertTrue(result);
        assertEquals(20.7, md.value(), DELTA);
    }

    @Test
    public void testSetIf_PredicateFalse() throws Exception {
        MutableDouble md = MutableDouble.of(10.5);
        boolean result = md.setIf(val -> val < 10.0, 5.0);
        assertFalse(result);
        assertEquals(10.5, md.value(), DELTA);
    }

    @Test
    public void testSetIf_ComplexPredicate() throws Exception {
        MutableDouble md = MutableDouble.of(25.5);
        boolean result = md.setIf(val -> val > 20.0 && val < 30.0, 100.0);
        assertTrue(result);
        assertEquals(100.0, md.value(), DELTA);
    }

    @Test
    public void testSetIf_PreservesValueWhenFalse() throws Exception {
        MutableDouble md = MutableDouble.of(50.0);
        boolean result = md.setIf(val -> val < 25.0, 75.0);
        assertFalse(result);
        assertEquals(50.0, md.value(), DELTA);
    }

    // ---- isNaN() ----

    @Test
    public void testIsNaN() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        assertTrue(md.isNaN());
    }

    @Test
    public void testIsNaN_NormalValue() {
        MutableDouble md = MutableDouble.of(42.5);
        assertFalse(md.isNaN());
    }

    @Test
    public void testIsNaN_AfterSetValue() {
        MutableDouble md = MutableDouble.of(10.5);
        assertFalse(md.isNaN());
        md.setValue(Double.NaN);
        assertTrue(md.isNaN());
    }

    @Test
    public void testIsNaN_Infinity() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertFalse(md.isNaN());
    }

    @Test
    public void testIsNaN_Zero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertFalse(md.isNaN());
    }

    @Test
    public void testIsInfinite_NormalValue() {
        MutableDouble md = MutableDouble.of(123.456);
        assertFalse(md.isInfinite());
    }

    @Test
    public void testIsInfinite_NaN() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        assertFalse(md.isInfinite());
    }

    // ---- isInfinite() ----

    @Test
    public void testIsInfinite() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertTrue(md.isInfinite());
    }

    @Test
    public void testIsInfinite_NegativeInfinity() {
        MutableDouble md = MutableDouble.of(Double.NEGATIVE_INFINITY);
        assertTrue(md.isInfinite());
    }

    @Test
    public void testIsInfinite_MaxValue() {
        MutableDouble md = MutableDouble.of(Double.MAX_VALUE);
        assertFalse(md.isInfinite());
    }

    @Test
    public void testIsInfinite_AfterOverflow() {
        MutableDouble md = MutableDouble.of(Double.MAX_VALUE);
        md.add(Double.MAX_VALUE);
        assertTrue(md.isInfinite());
    }

    // ---- increment() ----

    @Test
    public void testIncrement() {
        MutableDouble md = MutableDouble.of(5.5);
        md.increment();
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    public void testIncrement_FromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        md.increment();
        assertEquals(1.0, md.value(), DELTA);
    }

    @Test
    public void testIncrement_FromNegative() {
        MutableDouble md = MutableDouble.of(-0.5);
        md.increment();
        assertEquals(0.5, md.value(), DELTA);
    }

    @Test
    public void testIncrement_Multiple() {
        MutableDouble md = MutableDouble.of(10.0);
        md.increment();
        md.increment();
        md.increment();
        assertEquals(13.0, md.value(), DELTA);
    }

    // ---- decrement() ----

    @Test
    public void testDecrement() {
        MutableDouble md = MutableDouble.of(5.5);
        md.decrement();
        assertEquals(4.5, md.value(), DELTA);
    }

    @Test
    public void testDecrement_FromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        md.decrement();
        assertEquals(-1.0, md.value(), DELTA);
    }

    @Test
    public void testDecrement_ToNegative() {
        MutableDouble md = MutableDouble.of(0.5);
        md.decrement();
        assertEquals(-0.5, md.value(), DELTA);
    }

    @Test
    public void testDecrement_Multiple() {
        MutableDouble md = MutableDouble.of(10.0);
        md.decrement();
        md.decrement();
        assertEquals(8.0, md.value(), DELTA);
    }

    // ---- add(double) ----

    @Test
    public void testAdd() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(5.5);
        assertEquals(15.5, md.value(), DELTA);
    }

    @Test
    public void testAdd_NaN() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(Double.NaN);
        assertTrue(md.isNaN());
    }

    @Test
    public void testAdd_Negative() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(-3.5);
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    public void testAdd_Zero() {
        MutableDouble md = MutableDouble.of(42.0);
        md.add(0.0);
        assertEquals(42.0, md.value(), DELTA);
    }

    @Test
    public void testAdd_Multiple() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(5.0);
        md.add(3.5);
        md.add(1.5);
        assertEquals(20.0, md.value(), DELTA);
    }

    // ---- subtract(double) ----

    @Test
    public void testSubtract() {
        MutableDouble md = MutableDouble.of(10.0);
        md.subtract(3.5);
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    public void testSubtract_Negative() {
        MutableDouble md = MutableDouble.of(10.0);
        md.subtract(-5.0);
        assertEquals(15.0, md.value(), DELTA);
    }

    @Test
    public void testSubtract_Zero() {
        MutableDouble md = MutableDouble.of(42.0);
        md.subtract(0.0);
        assertEquals(42.0, md.value(), DELTA);
    }

    @Test
    public void testSubtract_ToNegative() {
        MutableDouble md = MutableDouble.of(5.0);
        md.subtract(10.5);
        assertEquals(-5.5, md.value(), DELTA);
    }

    // ---- getAndIncrement() ----

    @Test
    public void testGetAndIncrement() {
        MutableDouble md = MutableDouble.of(5.0);
        double oldValue = md.getAndIncrement();
        assertEquals(5.0, oldValue, DELTA);
        assertEquals(6.0, md.value(), DELTA);
    }

    @Test
    public void testGetAndIncrement_FromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double oldValue = md.getAndIncrement();
        assertEquals(0.0, oldValue, DELTA);
        assertEquals(1.0, md.value(), DELTA);
    }

    @Test
    public void testGetAndIncrement_FromNegative() {
        MutableDouble md = MutableDouble.of(-1.5);
        double oldValue = md.getAndIncrement();
        assertEquals(-1.5, oldValue, DELTA);
        assertEquals(-0.5, md.value(), DELTA);
    }

    // ---- getAndDecrement() ----

    @Test
    public void testGetAndDecrement() {
        MutableDouble md = MutableDouble.of(5.0);
        double oldValue = md.getAndDecrement();
        assertEquals(5.0, oldValue, DELTA);
        assertEquals(4.0, md.value(), DELTA);
    }

    @Test
    public void testGetAndDecrement_FromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double oldValue = md.getAndDecrement();
        assertEquals(0.0, oldValue, DELTA);
        assertEquals(-1.0, md.value(), DELTA);
    }

    @Test
    public void testGetAndDecrement_ToNegative() {
        MutableDouble md = MutableDouble.of(0.5);
        double oldValue = md.getAndDecrement();
        assertEquals(0.5, oldValue, DELTA);
        assertEquals(-0.5, md.value(), DELTA);
    }

    // ---- incrementAndGet() ----

    @Test
    public void testIncrementAndGet() {
        MutableDouble md = MutableDouble.of(5.0);
        double newValue = md.incrementAndGet();
        assertEquals(6.0, newValue, DELTA);
        assertEquals(6.0, md.value(), DELTA);
    }

    @Test
    public void testIncrementAndGet_FromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double newValue = md.incrementAndGet();
        assertEquals(1.0, newValue, DELTA);
    }

    @Test
    public void testIncrementAndGet_FromNegative() {
        MutableDouble md = MutableDouble.of(-2.5);
        double newValue = md.incrementAndGet();
        assertEquals(-1.5, newValue, DELTA);
    }

    // ---- decrementAndGet() ----

    @Test
    public void testDecrementAndGet() {
        MutableDouble md = MutableDouble.of(5.0);
        double newValue = md.decrementAndGet();
        assertEquals(4.0, newValue, DELTA);
        assertEquals(4.0, md.value(), DELTA);
    }

    @Test
    public void testDecrementAndGet_FromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double newValue = md.decrementAndGet();
        assertEquals(-1.0, newValue, DELTA);
    }

    @Test
    public void testDecrementAndGet_ToNegative() {
        MutableDouble md = MutableDouble.of(0.3);
        double newValue = md.decrementAndGet();
        assertEquals(-0.7, newValue, DELTA);
    }

    // ---- getAndAdd(double) ----

    @Test
    public void testGetAndAdd() {
        MutableDouble md = MutableDouble.of(10.0);
        double oldValue = md.getAndAdd(5.5);
        assertEquals(10.0, oldValue, DELTA);
        assertEquals(15.5, md.value(), DELTA);
    }

    @Test
    public void testGetAndAdd_Negative() {
        MutableDouble md = MutableDouble.of(10.0);
        double oldValue = md.getAndAdd(-3.5);
        assertEquals(10.0, oldValue, DELTA);
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    public void testGetAndAdd_Zero() {
        MutableDouble md = MutableDouble.of(42.0);
        double oldValue = md.getAndAdd(0.0);
        assertEquals(42.0, oldValue, DELTA);
        assertEquals(42.0, md.value(), DELTA);
    }

    // ---- addAndGet(double) ----

    @Test
    public void testAddAndGet() {
        MutableDouble md = MutableDouble.of(10.0);
        double newValue = md.addAndGet(5.5);
        assertEquals(15.5, newValue, DELTA);
        assertEquals(15.5, md.value(), DELTA);
    }

    @Test
    public void testAddAndGet_Negative() {
        MutableDouble md = MutableDouble.of(10.0);
        double newValue = md.addAndGet(-7.5);
        assertEquals(2.5, newValue, DELTA);
    }

    @Test
    public void testAddAndGet_Zero() {
        MutableDouble md = MutableDouble.of(50.0);
        double newValue = md.addAndGet(0.0);
        assertEquals(50.0, newValue, DELTA);
    }

    @Test
    public void testAddAndGet_ResultNegative() {
        MutableDouble md = MutableDouble.of(5.0);
        double newValue = md.addAndGet(-10.5);
        assertEquals(-5.5, newValue, DELTA);
    }

    // ---- intValue() ----

    @Test
    public void testIntValue() {
        MutableDouble md = MutableDouble.of(42.7);
        assertEquals(42, md.intValue());
    }

    @Test
    public void testIntValue_Truncation() {
        MutableDouble md = MutableDouble.of(99.99);
        assertEquals(99, md.intValue());
    }

    @Test
    public void testIntValue_SmallFraction() {
        MutableDouble md = MutableDouble.of(0.999);
        assertEquals(0, md.intValue());
    }

    @Test
    public void testIntValue_Negative() {
        MutableDouble md = MutableDouble.of(-42.7);
        assertEquals(-42, md.intValue());
    }

    @Test
    public void testIntValue_Zero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertEquals(0, md.intValue());
    }

    // ---- longValue() ----

    @Test
    public void testLongValue() {
        MutableDouble md = MutableDouble.of(42.7);
        assertEquals(42L, md.longValue());
    }

    @Test
    public void testLongValue_Truncation() {
        MutableDouble md = MutableDouble.of(123456.789);
        assertEquals(123456L, md.longValue());
    }

    @Test
    public void testLongValue_Large() {
        MutableDouble md = MutableDouble.of(1234567890123.456);
        assertEquals(1234567890123L, md.longValue());
    }

    @Test
    public void testLongValue_Negative() {
        MutableDouble md = MutableDouble.of(-9876.543);
        assertEquals(-9876L, md.longValue());
    }

    // ---- floatValue() ----

    @Test
    public void testFloatValue() {
        MutableDouble md = MutableDouble.of(42.5);
        assertEquals(42.5f, md.floatValue(), DELTA);
    }

    @Test
    public void testFloatValue_PrecisionLoss() {
        MutableDouble md = MutableDouble.of(42.123456789);
        float expected = 42.123456789f;
        assertEquals(expected, md.floatValue(), 0.0001f);
    }

    @Test
    public void testFloatValue_Negative() {
        MutableDouble md = MutableDouble.of(-99.99);
        assertEquals(-99.99f, md.floatValue(), 0.01f);
    }

    @Test
    public void testFloatValue_Zero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertEquals(0.0f, md.floatValue(), DELTA);
    }

    // ---- doubleValue() ----

    @Test
    public void testDoubleValue() {
        MutableDouble md = MutableDouble.of(42.5);
        assertEquals(42.5, md.doubleValue(), DELTA);
    }

    @Test
    public void testDoubleValue_Precision() {
        MutableDouble md = MutableDouble.of(3.141592653589793);
        assertEquals(3.141592653589793, md.doubleValue(), DELTA);
    }

    @Test
    public void testDoubleValue_MatchesValue() {
        MutableDouble md = MutableDouble.of(99.99);
        assertEquals(md.value(), md.doubleValue(), DELTA);
    }

    @Test
    public void testDoubleValue_Negative() {
        MutableDouble md = MutableDouble.of(-123.456);
        assertEquals(-123.456, md.doubleValue(), DELTA);
    }

    // ---- compareTo(MutableDouble) ----

    @Test
    public void testCompareTo() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(42.5);
        assertEquals(0, md1.compareTo(md2));
    }

    @Test
    public void testCompareTo_LessThan() {
        MutableDouble md1 = MutableDouble.of(10.5);
        MutableDouble md2 = MutableDouble.of(20.5);
        assertTrue(md1.compareTo(md2) < 0);
    }

    @Test
    public void testCompareTo_GreaterThan() {
        MutableDouble md1 = MutableDouble.of(50.5);
        MutableDouble md2 = MutableDouble.of(30.5);
        assertTrue(md1.compareTo(md2) > 0);
    }

    @Test
    public void testCompareTo_NaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(10.0);
        assertTrue(md1.compareTo(md2) > 0);
    }

    @Test
    public void testCompareTo_TwoNaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(Double.NaN);
        assertEquals(0, md1.compareTo(md2));
    }

    @Test
    public void testCompareTo_Negative() {
        MutableDouble md1 = MutableDouble.of(-10.5);
        MutableDouble md2 = MutableDouble.of(-5.5);
        assertTrue(md1.compareTo(md2) < 0);
    }

    @Test
    public void testCompareTo_WithZero() {
        MutableDouble md1 = MutableDouble.of(0.0);
        MutableDouble md2 = MutableDouble.of(5.0);
        assertTrue(md1.compareTo(md2) < 0);
    }

    @Test
    public void testCompareTo_Infinity() {
        MutableDouble md1 = MutableDouble.of(Double.POSITIVE_INFINITY);
        MutableDouble md2 = MutableDouble.of(1000.0);
        assertTrue(md1.compareTo(md2) > 0);
    }

    @Test
    public void testCompareTo_NegativeZeroVsPositiveZero() {
        MutableDouble md1 = MutableDouble.of(-0.0);
        MutableDouble md2 = MutableDouble.of(0.0);
        assertTrue(md1.compareTo(md2) < 0);
    }

    // ---- equals(Object) ----

    @Test
    public void testEquals() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(42.5);
        assertEquals(md1, md2);
    }

    @Test
    public void testEquals_NotEqual() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(43.5);
        assertNotEquals(md1, md2);
    }

    @Test
    public void testEquals_DifferentType() {
        MutableDouble md = MutableDouble.of(42.5);
        assertNotEquals(md, "42.5");
    }

    @Test
    public void testEquals_NaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(Double.NaN);
        assertEquals(md1, md2);
    }

    @Test
    public void testEquals_Null() {
        MutableDouble md = MutableDouble.of(42.5);
        assertNotEquals(null, md);
    }

    @Test
    public void testEquals_SameInstance() {
        MutableDouble md = MutableDouble.of(42.5);
        assertEquals(md, md);
    }

    @Test
    public void testEquals_PositiveAndNegativeZero() {
        MutableDouble md1 = MutableDouble.of(0.0);
        MutableDouble md2 = MutableDouble.of(-0.0);
        assertNotEquals(md1, md2);
    }

    @Test
    public void testEquals_Infinity() {
        MutableDouble md1 = MutableDouble.of(Double.POSITIVE_INFINITY);
        MutableDouble md2 = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertEquals(md1, md2);
    }

    @Test
    public void testEquals_DifferentInfinities() {
        MutableDouble md1 = MutableDouble.of(Double.POSITIVE_INFINITY);
        MutableDouble md2 = MutableDouble.of(Double.NEGATIVE_INFINITY);
        assertNotEquals(md1, md2);
    }

    // ---- hashCode() ----

    @Test
    public void testHashCode() {
        MutableDouble md = MutableDouble.of(42.5);
        int hash1 = md.hashCode();
        int hash2 = md.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_EqualObjects() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(42.5);
        assertEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    public void testHashCode_DifferentValues() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(43.5);
        assertNotEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    public void testHashCode_MatchesDoubleHashCode() {
        double value = 42.5;
        MutableDouble md = MutableDouble.of(value);
        assertEquals(Double.hashCode(value), md.hashCode());
    }

    @Test
    public void testHashCode_NaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(Double.NaN);
        assertEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    public void testHashCode_PositiveVsNegativeZero() {
        MutableDouble md1 = MutableDouble.of(0.0);
        MutableDouble md2 = MutableDouble.of(-0.0);
        assertNotEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    public void testToString_ChangesAfterModification() {
        MutableDouble md = MutableDouble.of(10.0);
        String str1 = md.toString();
        md.setValue(20.0);
        String str2 = md.toString();
        assertNotEquals(str1, str2);
    }

    // ---- toString() ----

    @Test
    public void testToString() {
        MutableDouble md = MutableDouble.of(42.5);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("42.5"));
    }

    @Test
    public void testToString_Negative() {
        MutableDouble md = MutableDouble.of(-99.99);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("-99.99"));
    }

    @Test
    public void testToString_Zero() {
        MutableDouble md = MutableDouble.of(0.0);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("0"));
    }

    @Test
    public void testToString_NaN() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("NaN"));
    }

    @Test
    public void testToString_Infinity() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("Infinity"));
    }

    @Test
    public void testChainingOperations() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(5.0);
        md.subtract(3.0);
        md.increment();
        md.decrement();
        assertEquals(12.0, md.value(), DELTA);
    }

    // ---- Integration / edge-case tests ----

    @Test
    public void testSpecialValues() {
        MutableDouble md = MutableDouble.of(0.0);

        md.setValue(0.0);
        assertEquals(0.0, md.value());
        assertFalse(1.0 / md.value() < 0);

        md.setValue(-0.0);
        assertEquals(-0.0, md.value());
        assertTrue(1.0 / md.value() < 0);

        md.setValue(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, md.value());

        md.setValue(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, md.value());

        md.setValue(Double.NaN);
        assertTrue(Double.isNaN(md.value()));
    }
}
