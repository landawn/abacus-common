package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MutableDouble2025Test extends TestBase {

    private static final double DELTA = 0.0000001;

    @Test
    @DisplayName("Test of() factory method")
    public void testOf() {
        MutableDouble md = MutableDouble.of(42.5);
        assertNotNull(md);
        assertEquals(42.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test of() with zero")
    public void testOfZero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertEquals(0.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test of() with negative value")
    public void testOfNegative() {
        MutableDouble md = MutableDouble.of(-123.456);
        assertEquals(-123.456, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test of() with NaN")
    public void testOfNaN() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        assertTrue(Double.isNaN(md.value()));
    }

    @Test
    @DisplayName("Test of() with positive infinity")
    public void testOfPositiveInfinity() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertEquals(Double.POSITIVE_INFINITY, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test of() with negative infinity")
    public void testOfNegativeInfinity() {
        MutableDouble md = MutableDouble.of(Double.NEGATIVE_INFINITY);
        assertEquals(Double.NEGATIVE_INFINITY, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test of() with MAX_VALUE")
    public void testOfMaxValue() {
        MutableDouble md = MutableDouble.of(Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test of() with MIN_VALUE")
    public void testOfMinValue() {
        MutableDouble md = MutableDouble.of(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test value() returns correct value")
    public void testValue() {
        MutableDouble md = MutableDouble.of(99.99);
        assertEquals(99.99, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test value() after modification")
    public void testValueAfterModification() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(20.5);
        assertEquals(20.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getValue() - deprecated method")
    public void testGetValue() {
        MutableDouble md = MutableDouble.of(55.5);
        assertEquals(55.5, md.getValue(), DELTA);
    }

    @Test
    @DisplayName("Test setValue() sets new value")
    public void testSetValue() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(30.7);
        assertEquals(30.7, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setValue() with zero")
    public void testSetValueZero() {
        MutableDouble md = MutableDouble.of(100.0);
        md.setValue(0.0);
        assertEquals(0.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setValue() with negative value")
    public void testSetValueNegative() {
        MutableDouble md = MutableDouble.of(50.0);
        md.setValue(-25.5);
        assertEquals(-25.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setValue() with NaN")
    public void testSetValueNaN() {
        MutableDouble md = MutableDouble.of(10.0);
        md.setValue(Double.NaN);
        assertTrue(Double.isNaN(md.value()));
    }

    @Test
    @DisplayName("Test getAndSet() returns old value and sets new value")
    public void testGetAndSet() {
        MutableDouble md = MutableDouble.of(10.5);
        double oldValue = md.getAndSet(20.7);
        assertEquals(10.5, oldValue, DELTA);
        assertEquals(20.7, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndSet() with same value")
    public void testGetAndSetSameValue() {
        MutableDouble md = MutableDouble.of(15.0);
        double oldValue = md.getAndSet(15.0);
        assertEquals(15.0, oldValue, DELTA);
        assertEquals(15.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndSet() with zero")
    public void testGetAndSetZero() {
        MutableDouble md = MutableDouble.of(100.0);
        double oldValue = md.getAndSet(0.0);
        assertEquals(100.0, oldValue, DELTA);
        assertEquals(0.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setAndGet() sets and returns new value")
    public void testSetAndGet() {
        MutableDouble md = MutableDouble.of(10.5);
        double newValue = md.setAndGet(20.7);
        assertEquals(20.7, newValue, DELTA);
        assertEquals(20.7, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setAndGet() with negative value")
    public void testSetAndGetNegative() {
        MutableDouble md = MutableDouble.of(50.0);
        double newValue = md.setAndGet(-30.5);
        assertEquals(-30.5, newValue, DELTA);
        assertEquals(-30.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setIf() when predicate is true")
    public void testSetIfTrue() throws Exception {
        MutableDouble md = MutableDouble.of(10.5);
        boolean result = md.setIf(20.7, val -> val < 15.0);
        assertTrue(result);
        assertEquals(20.7, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setIf() when predicate is false")
    public void testSetIfFalse() throws Exception {
        MutableDouble md = MutableDouble.of(10.5);
        boolean result = md.setIf(5.0, val -> val < 10.0);
        assertFalse(result);
        assertEquals(10.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setIf() with complex predicate")
    public void testSetIfComplexPredicate() throws Exception {
        MutableDouble md = MutableDouble.of(25.5);
        boolean result = md.setIf(100.0, val -> val > 20.0 && val < 30.0);
        assertTrue(result);
        assertEquals(100.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test setIf() preserves value when false")
    public void testSetIfPreservesValue() throws Exception {
        MutableDouble md = MutableDouble.of(50.0);
        boolean result = md.setIf(75.0, val -> val < 25.0);
        assertFalse(result);
        assertEquals(50.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test isNaN() returns true for NaN")
    public void testIsNaNTrue() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        assertTrue(md.isNaN());
    }

    @Test
    @DisplayName("Test isNaN() returns false for normal value")
    public void testIsNaNFalse() {
        MutableDouble md = MutableDouble.of(42.5);
        assertFalse(md.isNaN());
    }

    @Test
    @DisplayName("Test isNaN() returns false for infinity")
    public void testIsNaNInfinity() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertFalse(md.isNaN());
    }

    @Test
    @DisplayName("Test isNaN() returns false for zero")
    public void testIsNaNZero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertFalse(md.isNaN());
    }

    @Test
    @DisplayName("Test isInfinite() returns true for positive infinity")
    public void testIsInfinitePositive() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertTrue(md.isInfinite());
    }

    @Test
    @DisplayName("Test isInfinite() returns true for negative infinity")
    public void testIsInfiniteNegative() {
        MutableDouble md = MutableDouble.of(Double.NEGATIVE_INFINITY);
        assertTrue(md.isInfinite());
    }

    @Test
    @DisplayName("Test isInfinite() returns false for normal value")
    public void testIsInfiniteFalse() {
        MutableDouble md = MutableDouble.of(123.456);
        assertFalse(md.isInfinite());
    }

    @Test
    @DisplayName("Test isInfinite() returns false for NaN")
    public void testIsInfiniteNaN() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        assertFalse(md.isInfinite());
    }

    @Test
    @DisplayName("Test isInfinite() returns false for MAX_VALUE")
    public void testIsInfiniteMaxValue() {
        MutableDouble md = MutableDouble.of(Double.MAX_VALUE);
        assertFalse(md.isInfinite());
    }

    @Test
    @DisplayName("Test increment() adds 1.0")
    public void testIncrement() {
        MutableDouble md = MutableDouble.of(5.5);
        md.increment();
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test increment() from zero")
    public void testIncrementFromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        md.increment();
        assertEquals(1.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test increment() from negative")
    public void testIncrementFromNegative() {
        MutableDouble md = MutableDouble.of(-0.5);
        md.increment();
        assertEquals(0.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test increment() multiple times")
    public void testIncrementMultiple() {
        MutableDouble md = MutableDouble.of(10.0);
        md.increment();
        md.increment();
        md.increment();
        assertEquals(13.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test decrement() subtracts 1.0")
    public void testDecrement() {
        MutableDouble md = MutableDouble.of(5.5);
        md.decrement();
        assertEquals(4.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test decrement() from zero")
    public void testDecrementFromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        md.decrement();
        assertEquals(-1.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test decrement() to negative")
    public void testDecrementToNegative() {
        MutableDouble md = MutableDouble.of(0.5);
        md.decrement();
        assertEquals(-0.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test decrement() multiple times")
    public void testDecrementMultiple() {
        MutableDouble md = MutableDouble.of(10.0);
        md.decrement();
        md.decrement();
        assertEquals(8.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test add() adds positive value")
    public void testAdd() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(5.5);
        assertEquals(15.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test add() adds negative value")
    public void testAddNegative() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(-3.5);
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test add() adds zero")
    public void testAddZero() {
        MutableDouble md = MutableDouble.of(42.0);
        md.add(0.0);
        assertEquals(42.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test add() with large values")
    public void testAddLarge() {
        MutableDouble md = MutableDouble.of(1000000.0);
        md.add(500000.5);
        assertEquals(1500000.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test add() multiple times")
    public void testAddMultiple() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(5.0);
        md.add(3.5);
        md.add(1.5);
        assertEquals(20.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test subtract() subtracts positive value")
    public void testSubtract() {
        MutableDouble md = MutableDouble.of(10.0);
        md.subtract(3.5);
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test subtract() subtracts negative value")
    public void testSubtractNegative() {
        MutableDouble md = MutableDouble.of(10.0);
        md.subtract(-5.0);
        assertEquals(15.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test subtract() subtracts zero")
    public void testSubtractZero() {
        MutableDouble md = MutableDouble.of(42.0);
        md.subtract(0.0);
        assertEquals(42.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test subtract() to negative result")
    public void testSubtractToNegative() {
        MutableDouble md = MutableDouble.of(5.0);
        md.subtract(10.5);
        assertEquals(-5.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test subtract() multiple times")
    public void testSubtractMultiple() {
        MutableDouble md = MutableDouble.of(100.0);
        md.subtract(20.0);
        md.subtract(15.5);
        assertEquals(64.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndIncrement() returns old value")
    public void testGetAndIncrement() {
        MutableDouble md = MutableDouble.of(5.0);
        double oldValue = md.getAndIncrement();
        assertEquals(5.0, oldValue, DELTA);
        assertEquals(6.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndIncrement() from zero")
    public void testGetAndIncrementFromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double oldValue = md.getAndIncrement();
        assertEquals(0.0, oldValue, DELTA);
        assertEquals(1.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndIncrement() from negative")
    public void testGetAndIncrementFromNegative() {
        MutableDouble md = MutableDouble.of(-1.5);
        double oldValue = md.getAndIncrement();
        assertEquals(-1.5, oldValue, DELTA);
        assertEquals(-0.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndDecrement() returns old value")
    public void testGetAndDecrement() {
        MutableDouble md = MutableDouble.of(5.0);
        double oldValue = md.getAndDecrement();
        assertEquals(5.0, oldValue, DELTA);
        assertEquals(4.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndDecrement() from zero")
    public void testGetAndDecrementFromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double oldValue = md.getAndDecrement();
        assertEquals(0.0, oldValue, DELTA);
        assertEquals(-1.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndDecrement() to negative")
    public void testGetAndDecrementToNegative() {
        MutableDouble md = MutableDouble.of(0.5);
        double oldValue = md.getAndDecrement();
        assertEquals(0.5, oldValue, DELTA);
        assertEquals(-0.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test incrementAndGet() returns new value")
    public void testIncrementAndGet() {
        MutableDouble md = MutableDouble.of(5.0);
        double newValue = md.incrementAndGet();
        assertEquals(6.0, newValue, DELTA);
        assertEquals(6.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test incrementAndGet() from zero")
    public void testIncrementAndGetFromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double newValue = md.incrementAndGet();
        assertEquals(1.0, newValue, DELTA);
        assertEquals(1.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test incrementAndGet() from negative")
    public void testIncrementAndGetFromNegative() {
        MutableDouble md = MutableDouble.of(-2.5);
        double newValue = md.incrementAndGet();
        assertEquals(-1.5, newValue, DELTA);
        assertEquals(-1.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test decrementAndGet() returns new value")
    public void testDecrementAndGet() {
        MutableDouble md = MutableDouble.of(5.0);
        double newValue = md.decrementAndGet();
        assertEquals(4.0, newValue, DELTA);
        assertEquals(4.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test decrementAndGet() from zero")
    public void testDecrementAndGetFromZero() {
        MutableDouble md = MutableDouble.of(0.0);
        double newValue = md.decrementAndGet();
        assertEquals(-1.0, newValue, DELTA);
        assertEquals(-1.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test decrementAndGet() from positive to negative")
    public void testDecrementAndGetToNegative() {
        MutableDouble md = MutableDouble.of(0.3);
        double newValue = md.decrementAndGet();
        assertEquals(-0.7, newValue, DELTA);
        assertEquals(-0.7, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndAdd() returns old value")
    public void testGetAndAdd() {
        MutableDouble md = MutableDouble.of(10.0);
        double oldValue = md.getAndAdd(5.5);
        assertEquals(10.0, oldValue, DELTA);
        assertEquals(15.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndAdd() with negative delta")
    public void testGetAndAddNegative() {
        MutableDouble md = MutableDouble.of(10.0);
        double oldValue = md.getAndAdd(-3.5);
        assertEquals(10.0, oldValue, DELTA);
        assertEquals(6.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndAdd() with zero")
    public void testGetAndAddZero() {
        MutableDouble md = MutableDouble.of(42.0);
        double oldValue = md.getAndAdd(0.0);
        assertEquals(42.0, oldValue, DELTA);
        assertEquals(42.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test getAndAdd() with large value")
    public void testGetAndAddLarge() {
        MutableDouble md = MutableDouble.of(100.5);
        double oldValue = md.getAndAdd(999.5);
        assertEquals(100.5, oldValue, DELTA);
        assertEquals(1100.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test addAndGet() returns new value")
    public void testAddAndGet() {
        MutableDouble md = MutableDouble.of(10.0);
        double newValue = md.addAndGet(5.5);
        assertEquals(15.5, newValue, DELTA);
        assertEquals(15.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test addAndGet() with negative delta")
    public void testAddAndGetNegative() {
        MutableDouble md = MutableDouble.of(10.0);
        double newValue = md.addAndGet(-7.5);
        assertEquals(2.5, newValue, DELTA);
        assertEquals(2.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test addAndGet() with zero")
    public void testAddAndGetZero() {
        MutableDouble md = MutableDouble.of(50.0);
        double newValue = md.addAndGet(0.0);
        assertEquals(50.0, newValue, DELTA);
        assertEquals(50.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test addAndGet() resulting in negative")
    public void testAddAndGetResultNegative() {
        MutableDouble md = MutableDouble.of(5.0);
        double newValue = md.addAndGet(-10.5);
        assertEquals(-5.5, newValue, DELTA);
        assertEquals(-5.5, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test intValue() converts to int")
    public void testIntValue() {
        MutableDouble md = MutableDouble.of(42.7);
        assertEquals(42, md.intValue());
    }

    @Test
    @DisplayName("Test intValue() truncates decimal")
    public void testIntValueTruncation() {
        MutableDouble md = MutableDouble.of(99.99);
        assertEquals(99, md.intValue());
    }

    @Test
    @DisplayName("Test intValue() with negative value")
    public void testIntValueNegative() {
        MutableDouble md = MutableDouble.of(-42.7);
        assertEquals(-42, md.intValue());
    }

    @Test
    @DisplayName("Test intValue() with zero")
    public void testIntValueZero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertEquals(0, md.intValue());
    }

    @Test
    @DisplayName("Test intValue() with very small value")
    public void testIntValueSmall() {
        MutableDouble md = MutableDouble.of(0.999);
        assertEquals(0, md.intValue());
    }

    @Test
    @DisplayName("Test longValue() converts to long")
    public void testLongValue() {
        MutableDouble md = MutableDouble.of(42.7);
        assertEquals(42L, md.longValue());
    }

    @Test
    @DisplayName("Test longValue() truncates decimal")
    public void testLongValueTruncation() {
        MutableDouble md = MutableDouble.of(123456.789);
        assertEquals(123456L, md.longValue());
    }

    @Test
    @DisplayName("Test longValue() with negative value")
    public void testLongValueNegative() {
        MutableDouble md = MutableDouble.of(-9876.543);
        assertEquals(-9876L, md.longValue());
    }

    @Test
    @DisplayName("Test longValue() with large value")
    public void testLongValueLarge() {
        MutableDouble md = MutableDouble.of(1234567890123.456);
        assertEquals(1234567890123L, md.longValue());
    }

    @Test
    @DisplayName("Test floatValue() converts to float")
    public void testFloatValue() {
        MutableDouble md = MutableDouble.of(42.5);
        assertEquals(42.5f, md.floatValue(), DELTA);
    }

    @Test
    @DisplayName("Test floatValue() with precision loss")
    public void testFloatValuePrecisionLoss() {
        MutableDouble md = MutableDouble.of(42.123456789);
        float expected = 42.123456789f;
        assertEquals(expected, md.floatValue(), 0.0001f);
    }

    @Test
    @DisplayName("Test floatValue() with negative value")
    public void testFloatValueNegative() {
        MutableDouble md = MutableDouble.of(-99.99);
        assertEquals(-99.99f, md.floatValue(), DELTA);
    }

    @Test
    @DisplayName("Test floatValue() with zero")
    public void testFloatValueZero() {
        MutableDouble md = MutableDouble.of(0.0);
        assertEquals(0.0f, md.floatValue(), DELTA);
    }

    @Test
    @DisplayName("Test doubleValue() returns exact value")
    public void testDoubleValue() {
        MutableDouble md = MutableDouble.of(42.5);
        assertEquals(42.5, md.doubleValue(), DELTA);
    }

    @Test
    @DisplayName("Test doubleValue() with high precision")
    public void testDoubleValuePrecision() {
        MutableDouble md = MutableDouble.of(3.141592653589793);
        assertEquals(3.141592653589793, md.doubleValue(), DELTA);
    }

    @Test
    @DisplayName("Test doubleValue() with negative value")
    public void testDoubleValueNegative() {
        MutableDouble md = MutableDouble.of(-123.456);
        assertEquals(-123.456, md.doubleValue(), DELTA);
    }

    @Test
    @DisplayName("Test doubleValue() matches value()")
    public void testDoubleValueMatchesValue() {
        MutableDouble md = MutableDouble.of(99.99);
        assertEquals(md.value(), md.doubleValue(), DELTA);
    }

    @Test
    @DisplayName("Test compareTo() with equal values")
    public void testCompareToEqual() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(42.5);
        assertEquals(0, md1.compareTo(md2));
    }

    @Test
    @DisplayName("Test compareTo() with less than")
    public void testCompareToLess() {
        MutableDouble md1 = MutableDouble.of(10.5);
        MutableDouble md2 = MutableDouble.of(20.5);
        assertTrue(md1.compareTo(md2) < 0);
    }

    @Test
    @DisplayName("Test compareTo() with greater than")
    public void testCompareToGreater() {
        MutableDouble md1 = MutableDouble.of(50.5);
        MutableDouble md2 = MutableDouble.of(30.5);
        assertTrue(md1.compareTo(md2) > 0);
    }

    @Test
    @DisplayName("Test compareTo() with negative values")
    public void testCompareToNegative() {
        MutableDouble md1 = MutableDouble.of(-10.5);
        MutableDouble md2 = MutableDouble.of(-5.5);
        assertTrue(md1.compareTo(md2) < 0);
    }

    @Test
    @DisplayName("Test compareTo() with zero")
    public void testCompareToZero() {
        MutableDouble md1 = MutableDouble.of(0.0);
        MutableDouble md2 = MutableDouble.of(5.0);
        assertTrue(md1.compareTo(md2) < 0);
    }

    @Test
    @DisplayName("Test compareTo() with NaN")
    public void testCompareToNaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(10.0);
        assertTrue(md1.compareTo(md2) > 0);
    }

    @Test
    @DisplayName("Test compareTo() with two NaN values")
    public void testCompareToTwoNaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(Double.NaN);
        assertEquals(0, md1.compareTo(md2));
    }

    @Test
    @DisplayName("Test compareTo() with infinity")
    public void testCompareToInfinity() {
        MutableDouble md1 = MutableDouble.of(Double.POSITIVE_INFINITY);
        MutableDouble md2 = MutableDouble.of(1000.0);
        assertTrue(md1.compareTo(md2) > 0);
    }

    @Test
    @DisplayName("Test equals() with equal values")
    public void testEquals() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(42.5);
        assertEquals(md1, md2);
    }

    @Test
    @DisplayName("Test equals() with different values")
    public void testEqualsNotEqual() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(43.5);
        assertNotEquals(md1, md2);
    }

    @Test
    @DisplayName("Test equals() with null")
    public void testEqualsNull() {
        MutableDouble md = MutableDouble.of(42.5);
        assertNotEquals(null, md);
    }

    @Test
    @DisplayName("Test equals() with different type")
    public void testEqualsDifferentType() {
        MutableDouble md = MutableDouble.of(42.5);
        assertNotEquals(md, "42.5");
    }

    @Test
    @DisplayName("Test equals() with same instance")
    public void testEqualsSameInstance() {
        MutableDouble md = MutableDouble.of(42.5);
        assertEquals(md, md);
    }

    @Test
    @DisplayName("Test equals() with NaN")
    public void testEqualsNaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(Double.NaN);
        assertEquals(md1, md2);
    }

    @Test
    @DisplayName("Test equals() with positive and negative zero")
    public void testEqualsZero() {
        MutableDouble md1 = MutableDouble.of(0.0);
        MutableDouble md2 = MutableDouble.of(-0.0);
        assertNotEquals(md1, md2);
    }

    @Test
    @DisplayName("Test equals() with infinity")
    public void testEqualsInfinity() {
        MutableDouble md1 = MutableDouble.of(Double.POSITIVE_INFINITY);
        MutableDouble md2 = MutableDouble.of(Double.POSITIVE_INFINITY);
        assertEquals(md1, md2);
    }

    @Test
    @DisplayName("Test equals() with different infinities")
    public void testEqualsDifferentInfinity() {
        MutableDouble md1 = MutableDouble.of(Double.POSITIVE_INFINITY);
        MutableDouble md2 = MutableDouble.of(Double.NEGATIVE_INFINITY);
        assertNotEquals(md1, md2);
    }

    @Test
    @DisplayName("Test hashCode() consistency")
    public void testHashCode() {
        MutableDouble md = MutableDouble.of(42.5);
        int hash1 = md.hashCode();
        int hash2 = md.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("Test hashCode() for equal objects")
    public void testHashCodeEqual() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(42.5);
        assertEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    @DisplayName("Test hashCode() for different values")
    public void testHashCodeDifferent() {
        MutableDouble md1 = MutableDouble.of(42.5);
        MutableDouble md2 = MutableDouble.of(43.5);
        assertNotEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    @DisplayName("Test hashCode() matches Double.hashCode()")
    public void testHashCodeMatchesDouble() {
        double value = 42.5;
        MutableDouble md = MutableDouble.of(value);
        assertEquals(Double.hashCode(value), md.hashCode());
    }

    @Test
    @DisplayName("Test hashCode() with NaN")
    public void testHashCodeNaN() {
        MutableDouble md1 = MutableDouble.of(Double.NaN);
        MutableDouble md2 = MutableDouble.of(Double.NaN);
        assertEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    @DisplayName("Test hashCode() with zero")
    public void testHashCodeZero() {
        MutableDouble md1 = MutableDouble.of(0.0);
        MutableDouble md2 = MutableDouble.of(-0.0);
        assertNotEquals(md1.hashCode(), md2.hashCode());
    }

    @Test
    @DisplayName("Test toString() returns string representation")
    public void testToString() {
        MutableDouble md = MutableDouble.of(42.5);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("42.5"));
    }

    @Test
    @DisplayName("Test toString() with negative value")
    public void testToStringNegative() {
        MutableDouble md = MutableDouble.of(-99.99);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("-99.99") || str.contains("-100"));
    }

    @Test
    @DisplayName("Test toString() with zero")
    public void testToStringZero() {
        MutableDouble md = MutableDouble.of(0.0);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("0"));
    }

    @Test
    @DisplayName("Test toString() with NaN")
    public void testToStringNaN() {
        MutableDouble md = MutableDouble.of(Double.NaN);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("NaN"));
    }

    @Test
    @DisplayName("Test toString() with infinity")
    public void testToStringInfinity() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        String str = md.toString();
        assertNotNull(str);
        assertTrue(str.contains("Infinity"));
    }

    @Test
    @DisplayName("Test toString() changes after modification")
    public void testToStringAfterModification() {
        MutableDouble md = MutableDouble.of(10.0);
        String str1 = md.toString();
        md.setValue(20.0);
        String str2 = md.toString();
        assertNotEquals(str1, str2);
    }

    @Test
    @DisplayName("Test chaining operations")
    public void testChainingOperations() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(5.0);
        md.subtract(3.0);
        md.increment();
        md.decrement();
        assertEquals(12.0, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test overflow behavior")
    public void testOverflow() {
        MutableDouble md = MutableDouble.of(Double.MAX_VALUE);
        md.add(Double.MAX_VALUE);
        assertTrue(md.isInfinite());
    }

    @Test
    @DisplayName("Test underflow behavior")
    public void testUnderflow() {
        MutableDouble md = MutableDouble.of(Double.MIN_VALUE);
        md.subtract(1.0);
        assertFalse(md.value() == 0.0);
    }

    @Test
    @DisplayName("Test operations with NaN propagation")
    public void testNaNPropagation() {
        MutableDouble md = MutableDouble.of(10.0);
        md.add(Double.NaN);
        assertTrue(md.isNaN());
    }

    @Test
    @DisplayName("Test infinity arithmetic")
    public void testInfinityArithmetic() {
        MutableDouble md = MutableDouble.of(Double.POSITIVE_INFINITY);
        md.add(100.0);
        assertTrue(md.isInfinite());
        assertEquals(Double.POSITIVE_INFINITY, md.value(), DELTA);
    }

    @Test
    @DisplayName("Test precision with very small increments")
    public void testPrecisionSmallIncrements() {
        MutableDouble md = MutableDouble.of(0.0);
        for (int i = 0; i < 1000; i++) {
            md.add(0.001);
        }
        assertEquals(1.0, md.value(), 0.01);
    }

    @Test
    @DisplayName("Test negative to positive transition")
    public void testNegativeToPositiveTransition() {
        MutableDouble md = MutableDouble.of(-5.0);
        md.add(10.0);
        assertEquals(5.0, md.value(), DELTA);
        assertTrue(md.value() > 0);
    }

    @Test
    @DisplayName("Test positive to negative transition")
    public void testPositiveToNegativeTransition() {
        MutableDouble md = MutableDouble.of(5.0);
        md.subtract(10.0);
        assertEquals(-5.0, md.value(), DELTA);
        assertTrue(md.value() < 0);
    }
}
