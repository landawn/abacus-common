package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MutableByte2025Test extends TestBase {

    @Test
    public void testOf() {
        MutableByte num = MutableByte.of((byte) 10);
        Assertions.assertNotNull(num);
        Assertions.assertEquals((byte) 10, num.value());
    }

    @Test
    public void testOfWithZero() {
        MutableByte num = MutableByte.of((byte) 0);
        Assertions.assertEquals((byte) 0, num.value());
    }

    @Test
    public void testOfWithMaxValue() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        Assertions.assertEquals(Byte.MAX_VALUE, num.value());
    }

    @Test
    public void testOfWithMinValue() {
        MutableByte num = MutableByte.of(Byte.MIN_VALUE);
        Assertions.assertEquals(Byte.MIN_VALUE, num.value());
    }

    @Test
    public void testOfWithNegativeValue() {
        MutableByte num = MutableByte.of((byte) -50);
        Assertions.assertEquals((byte) -50, num.value());
    }

    @Test
    public void testValue() {
        MutableByte num = MutableByte.of((byte) 42);
        Assertions.assertEquals((byte) 42, num.value());
    }

    @Test
    public void testValueAfterModification() {
        MutableByte num = MutableByte.of((byte) 10);
        num.setValue((byte) 20);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testGetValue() {
        MutableByte num = MutableByte.of((byte) 25);
        Assertions.assertEquals((byte) 25, num.getValue());
    }

    @Test
    public void testGetValueDeprecated() {
        MutableByte num = MutableByte.of((byte) 100);
        byte value = num.getValue();
        Assertions.assertEquals((byte) 100, value);
    }

    @Test
    public void testSetValue() {
        MutableByte num = MutableByte.of((byte) 10);
        num.setValue((byte) 30);
        Assertions.assertEquals((byte) 30, num.value());
    }

    @Test
    public void testSetValueMultipleTimes() {
        MutableByte num = MutableByte.of((byte) 1);
        num.setValue((byte) 2);
        num.setValue((byte) 3);
        num.setValue((byte) 4);
        Assertions.assertEquals((byte) 4, num.value());
    }

    @Test
    public void testSetValueToZero() {
        MutableByte num = MutableByte.of((byte) 100);
        num.setValue((byte) 0);
        Assertions.assertEquals((byte) 0, num.value());
    }

    @Test
    public void testSetValueToNegative() {
        MutableByte num = MutableByte.of((byte) 50);
        num.setValue((byte) -75);
        Assertions.assertEquals((byte) -75, num.value());
    }

    @Test
    public void testGetAndSet() {
        MutableByte num = MutableByte.of((byte) 10);
        byte oldValue = num.getAndSet((byte) 20);
        Assertions.assertEquals((byte) 10, oldValue);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testGetAndSetWithSameValue() {
        MutableByte num = MutableByte.of((byte) 15);
        byte oldValue = num.getAndSet((byte) 15);
        Assertions.assertEquals((byte) 15, oldValue);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testGetAndSetChained() {
        MutableByte num = MutableByte.of((byte) 1);
        byte val1 = num.getAndSet((byte) 2);
        byte val2 = num.getAndSet((byte) 3);
        byte val3 = num.getAndSet((byte) 4);
        Assertions.assertEquals((byte) 1, val1);
        Assertions.assertEquals((byte) 2, val2);
        Assertions.assertEquals((byte) 3, val3);
        Assertions.assertEquals((byte) 4, num.value());
    }

    @Test
    public void testSetAndGet() {
        MutableByte num = MutableByte.of((byte) 10);
        byte newValue = num.setAndGet((byte) 20);
        Assertions.assertEquals((byte) 20, newValue);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testSetAndGetWithZero() {
        MutableByte num = MutableByte.of((byte) 100);
        byte result = num.setAndGet((byte) 0);
        Assertions.assertEquals((byte) 0, result);
        Assertions.assertEquals((byte) 0, num.value());
    }

    @Test
    public void testSetAndGetWithNegative() {
        MutableByte num = MutableByte.of((byte) 50);
        byte result = num.setAndGet((byte) -25);
        Assertions.assertEquals((byte) -25, result);
        Assertions.assertEquals((byte) -25, num.value());
    }

    @Test
    public void testSetIfPredicateTrue() {
        MutableByte num = MutableByte.of((byte) 10);
        boolean updated = num.setIf((byte) 20, v -> v < 15);
        Assertions.assertTrue(updated);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testSetIfPredicateFalse() {
        MutableByte num = MutableByte.of((byte) 10);
        boolean updated = num.setIf((byte) 20, v -> v > 15);
        Assertions.assertFalse(updated);
        Assertions.assertEquals((byte) 10, num.value());
    }

    @Test
    public void testSetIfWithZeroValue() {
        MutableByte num = MutableByte.of((byte) 0);
        boolean updated = num.setIf((byte) 5, v -> v == 0);
        Assertions.assertTrue(updated);
        Assertions.assertEquals((byte) 5, num.value());
    }

    @Test
    public void testSetIfWithNegativeValue() {
        MutableByte num = MutableByte.of((byte) -10);
        boolean updated = num.setIf((byte) 10, v -> v < 0);
        Assertions.assertTrue(updated);
        Assertions.assertEquals((byte) 10, num.value());
    }

    @Test
    public void testSetIfMultipleConditions() {
        MutableByte num = MutableByte.of((byte) 10);
        boolean updated1 = num.setIf((byte) 20, v -> v == 10);
        boolean updated2 = num.setIf((byte) 30, v -> v < 15);
        Assertions.assertTrue(updated1);
        Assertions.assertFalse(updated2);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testIncrement() {
        MutableByte num = MutableByte.of((byte) 10);
        num.increment();
        Assertions.assertEquals((byte) 11, num.value());
    }

    @Test
    public void testIncrementMultipleTimes() {
        MutableByte num = MutableByte.of((byte) 0);
        num.increment();
        num.increment();
        num.increment();
        Assertions.assertEquals((byte) 3, num.value());
    }

    @Test
    public void testIncrementOverflow() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        num.increment();
        Assertions.assertEquals(Byte.MIN_VALUE, num.value());
    }

    @Test
    public void testDecrement() {
        MutableByte num = MutableByte.of((byte) 10);
        num.decrement();
        Assertions.assertEquals((byte) 9, num.value());
    }

    @Test
    public void testDecrementMultipleTimes() {
        MutableByte num = MutableByte.of((byte) 5);
        num.decrement();
        num.decrement();
        num.decrement();
        Assertions.assertEquals((byte) 2, num.value());
    }

    @Test
    public void testDecrementUnderflow() {
        MutableByte num = MutableByte.of(Byte.MIN_VALUE);
        num.decrement();
        Assertions.assertEquals(Byte.MAX_VALUE, num.value());
    }

    @Test
    public void testAdd() {
        MutableByte num = MutableByte.of((byte) 10);
        num.add((byte) 5);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testAddZero() {
        MutableByte num = MutableByte.of((byte) 20);
        num.add((byte) 0);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testAddNegative() {
        MutableByte num = MutableByte.of((byte) 20);
        num.add((byte) -5);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testAddOverflow() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        num.add((byte) 1);
        Assertions.assertEquals(Byte.MIN_VALUE, num.value());
    }

    @Test
    public void testSubtract() {
        MutableByte num = MutableByte.of((byte) 10);
        num.subtract((byte) 3);
        Assertions.assertEquals((byte) 7, num.value());
    }

    @Test
    public void testSubtractZero() {
        MutableByte num = MutableByte.of((byte) 15);
        num.subtract((byte) 0);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testSubtractNegative() {
        MutableByte num = MutableByte.of((byte) 10);
        num.subtract((byte) -5);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testSubtractUnderflow() {
        MutableByte num = MutableByte.of(Byte.MIN_VALUE);
        num.subtract((byte) 1);
        Assertions.assertEquals(Byte.MAX_VALUE, num.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableByte num = MutableByte.of((byte) 10);
        byte oldValue = num.getAndIncrement();
        Assertions.assertEquals((byte) 10, oldValue);
        Assertions.assertEquals((byte) 11, num.value());
    }

    @Test
    public void testGetAndIncrementMultipleTimes() {
        MutableByte num = MutableByte.of((byte) 0);
        Assertions.assertEquals((byte) 0, num.getAndIncrement());
        Assertions.assertEquals((byte) 1, num.getAndIncrement());
        Assertions.assertEquals((byte) 2, num.getAndIncrement());
        Assertions.assertEquals((byte) 3, num.value());
    }

    @Test
    public void testGetAndIncrementOverflow() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        byte oldValue = num.getAndIncrement();
        Assertions.assertEquals(Byte.MAX_VALUE, oldValue);
        Assertions.assertEquals(Byte.MIN_VALUE, num.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableByte num = MutableByte.of((byte) 10);
        byte oldValue = num.getAndDecrement();
        Assertions.assertEquals((byte) 10, oldValue);
        Assertions.assertEquals((byte) 9, num.value());
    }

    @Test
    public void testGetAndDecrementMultipleTimes() {
        MutableByte num = MutableByte.of((byte) 5);
        Assertions.assertEquals((byte) 5, num.getAndDecrement());
        Assertions.assertEquals((byte) 4, num.getAndDecrement());
        Assertions.assertEquals((byte) 3, num.getAndDecrement());
        Assertions.assertEquals((byte) 2, num.value());
    }

    @Test
    public void testGetAndDecrementUnderflow() {
        MutableByte num = MutableByte.of(Byte.MIN_VALUE);
        byte oldValue = num.getAndDecrement();
        Assertions.assertEquals(Byte.MIN_VALUE, oldValue);
        Assertions.assertEquals(Byte.MAX_VALUE, num.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableByte num = MutableByte.of((byte) 10);
        byte newValue = num.incrementAndGet();
        Assertions.assertEquals((byte) 11, newValue);
        Assertions.assertEquals((byte) 11, num.value());
    }

    @Test
    public void testIncrementAndGetMultipleTimes() {
        MutableByte num = MutableByte.of((byte) 0);
        Assertions.assertEquals((byte) 1, num.incrementAndGet());
        Assertions.assertEquals((byte) 2, num.incrementAndGet());
        Assertions.assertEquals((byte) 3, num.incrementAndGet());
        Assertions.assertEquals((byte) 3, num.value());
    }

    @Test
    public void testIncrementAndGetOverflow() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        byte newValue = num.incrementAndGet();
        Assertions.assertEquals(Byte.MIN_VALUE, newValue);
        Assertions.assertEquals(Byte.MIN_VALUE, num.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableByte num = MutableByte.of((byte) 10);
        byte newValue = num.decrementAndGet();
        Assertions.assertEquals((byte) 9, newValue);
        Assertions.assertEquals((byte) 9, num.value());
    }

    @Test
    public void testDecrementAndGetMultipleTimes() {
        MutableByte num = MutableByte.of((byte) 5);
        Assertions.assertEquals((byte) 4, num.decrementAndGet());
        Assertions.assertEquals((byte) 3, num.decrementAndGet());
        Assertions.assertEquals((byte) 2, num.decrementAndGet());
        Assertions.assertEquals((byte) 2, num.value());
    }

    @Test
    public void testDecrementAndGetUnderflow() {
        MutableByte num = MutableByte.of(Byte.MIN_VALUE);
        byte newValue = num.decrementAndGet();
        Assertions.assertEquals(Byte.MAX_VALUE, newValue);
        Assertions.assertEquals(Byte.MAX_VALUE, num.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableByte num = MutableByte.of((byte) 10);
        byte oldValue = num.getAndAdd((byte) 5);
        Assertions.assertEquals((byte) 10, oldValue);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testGetAndAddZero() {
        MutableByte num = MutableByte.of((byte) 20);
        byte oldValue = num.getAndAdd((byte) 0);
        Assertions.assertEquals((byte) 20, oldValue);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testGetAndAddNegative() {
        MutableByte num = MutableByte.of((byte) 20);
        byte oldValue = num.getAndAdd((byte) -5);
        Assertions.assertEquals((byte) 20, oldValue);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testGetAndAddOverflow() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        byte oldValue = num.getAndAdd((byte) 1);
        Assertions.assertEquals(Byte.MAX_VALUE, oldValue);
        Assertions.assertEquals(Byte.MIN_VALUE, num.value());
    }

    @Test
    public void testAddAndGet() {
        MutableByte num = MutableByte.of((byte) 10);
        byte newValue = num.addAndGet((byte) 5);
        Assertions.assertEquals((byte) 15, newValue);
        Assertions.assertEquals((byte) 15, num.value());
    }

    @Test
    public void testAddAndGetZero() {
        MutableByte num = MutableByte.of((byte) 25);
        byte newValue = num.addAndGet((byte) 0);
        Assertions.assertEquals((byte) 25, newValue);
        Assertions.assertEquals((byte) 25, num.value());
    }

    @Test
    public void testAddAndGetNegative() {
        MutableByte num = MutableByte.of((byte) 30);
        byte newValue = num.addAndGet((byte) -10);
        Assertions.assertEquals((byte) 20, newValue);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testAddAndGetOverflow() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        byte newValue = num.addAndGet((byte) 2);
        Assertions.assertEquals(Byte.MIN_VALUE + 1, newValue);
        Assertions.assertEquals(Byte.MIN_VALUE + 1, num.value());
    }

    @Test
    public void testByteValue() {
        MutableByte num = MutableByte.of((byte) 42);
        Assertions.assertEquals((byte) 42, num.byteValue());
    }

    @Test
    public void testByteValueNegative() {
        MutableByte num = MutableByte.of((byte) -100);
        Assertions.assertEquals((byte) -100, num.byteValue());
    }

    @Test
    public void testByteValueMinMax() {
        MutableByte numMin = MutableByte.of(Byte.MIN_VALUE);
        MutableByte numMax = MutableByte.of(Byte.MAX_VALUE);
        Assertions.assertEquals(Byte.MIN_VALUE, numMin.byteValue());
        Assertions.assertEquals(Byte.MAX_VALUE, numMax.byteValue());
    }

    @Test
    public void testShortValue() {
        MutableByte num = MutableByte.of((byte) 42);
        Assertions.assertEquals((short) 42, num.shortValue());
    }

    @Test
    public void testShortValueNegative() {
        MutableByte num = MutableByte.of((byte) -1);
        Assertions.assertEquals((short) -1, num.shortValue());
    }

    @Test
    public void testShortValueSignExtension() {
        MutableByte num = MutableByte.of((byte) -128);
        Assertions.assertEquals((short) -128, num.shortValue());
    }

    @Test
    public void testIntValue() {
        MutableByte num = MutableByte.of((byte) 100);
        Assertions.assertEquals(100, num.intValue());
    }

    @Test
    public void testIntValueNegative() {
        MutableByte num = MutableByte.of((byte) -50);
        Assertions.assertEquals(-50, num.intValue());
    }

    @Test
    public void testIntValueSignExtension() {
        MutableByte num = MutableByte.of((byte) -128);
        Assertions.assertEquals(-128, num.intValue());
    }

    @Test
    public void testLongValue() {
        MutableByte num = MutableByte.of((byte) 50);
        Assertions.assertEquals(50L, num.longValue());
    }

    @Test
    public void testLongValueNegative() {
        MutableByte num = MutableByte.of((byte) -75);
        Assertions.assertEquals(-75L, num.longValue());
    }

    @Test
    public void testLongValueSignExtension() {
        MutableByte num = MutableByte.of(Byte.MIN_VALUE);
        Assertions.assertEquals(-128L, num.longValue());
    }

    @Test
    public void testFloatValue() {
        MutableByte num = MutableByte.of((byte) 75);
        Assertions.assertEquals(75.0f, num.floatValue(), 0.0f);
    }

    @Test
    public void testFloatValueNegative() {
        MutableByte num = MutableByte.of((byte) -25);
        Assertions.assertEquals(-25.0f, num.floatValue(), 0.0f);
    }

    @Test
    public void testFloatValuePrecision() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        Assertions.assertEquals(127.0f, num.floatValue(), 0.0f);
    }

    @Test
    public void testDoubleValue() {
        MutableByte num = MutableByte.of((byte) -128);
        Assertions.assertEquals(-128.0, num.doubleValue(), 0.0);
    }

    @Test
    public void testDoubleValuePositive() {
        MutableByte num = MutableByte.of((byte) 100);
        Assertions.assertEquals(100.0, num.doubleValue(), 0.0);
    }

    @Test
    public void testDoubleValuePrecision() {
        MutableByte num = MutableByte.of(Byte.MIN_VALUE);
        Assertions.assertEquals(-128.0, num.doubleValue(), 0.0);
    }

    @Test
    public void testCompareTo() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 20);
        Assertions.assertTrue(a.compareTo(b) < 0);
    }

    @Test
    public void testCompareToEqual() {
        MutableByte a = MutableByte.of((byte) 15);
        MutableByte b = MutableByte.of((byte) 15);
        Assertions.assertEquals(0, a.compareTo(b));
    }

    @Test
    public void testCompareToGreater() {
        MutableByte a = MutableByte.of((byte) 30);
        MutableByte b = MutableByte.of((byte) 10);
        Assertions.assertTrue(a.compareTo(b) > 0);
    }

    @Test
    public void testCompareToNegativeValues() {
        MutableByte a = MutableByte.of((byte) -10);
        MutableByte b = MutableByte.of((byte) -5);
        Assertions.assertTrue(a.compareTo(b) < 0);
    }

    @Test
    public void testCompareToMixedSigns() {
        MutableByte a = MutableByte.of((byte) -10);
        MutableByte b = MutableByte.of((byte) 10);
        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
    }

    @Test
    public void testCompareToMinMax() {
        MutableByte min = MutableByte.of(Byte.MIN_VALUE);
        MutableByte max = MutableByte.of(Byte.MAX_VALUE);
        Assertions.assertTrue(min.compareTo(max) < 0);
        Assertions.assertTrue(max.compareTo(min) > 0);
    }

    @Test
    public void testEquals() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 10);
        Assertions.assertTrue(a.equals(b));
    }

    @Test
    public void testEqualsSameInstance() {
        MutableByte a = MutableByte.of((byte) 15);
        Assertions.assertTrue(a.equals(a));
    }

    @Test
    public void testEqualsNotEqual() {
        MutableByte a = MutableByte.of((byte) 10);
        MutableByte b = MutableByte.of((byte) 20);
        Assertions.assertFalse(a.equals(b));
    }

    @Test
    public void testEqualsNull() {
        MutableByte a = MutableByte.of((byte) 10);
        Assertions.assertFalse(a.equals(null));
    }

    @Test
    public void testEqualsDifferentType() {
        MutableByte a = MutableByte.of((byte) 10);
        Assertions.assertFalse(a.equals("10"));
        Assertions.assertFalse(a.equals(Integer.valueOf(10)));
    }

    @Test
    public void testEqualsZero() {
        MutableByte a = MutableByte.of((byte) 0);
        MutableByte b = MutableByte.of((byte) 0);
        Assertions.assertTrue(a.equals(b));
    }

    @Test
    public void testEqualsNegative() {
        MutableByte a = MutableByte.of((byte) -50);
        MutableByte b = MutableByte.of((byte) -50);
        Assertions.assertTrue(a.equals(b));
    }

    @Test
    public void testEqualsMinMax() {
        MutableByte minA = MutableByte.of(Byte.MIN_VALUE);
        MutableByte minB = MutableByte.of(Byte.MIN_VALUE);
        MutableByte maxA = MutableByte.of(Byte.MAX_VALUE);
        MutableByte maxB = MutableByte.of(Byte.MAX_VALUE);

        Assertions.assertTrue(minA.equals(minB));
        Assertions.assertTrue(maxA.equals(maxB));
        Assertions.assertFalse(minA.equals(maxA));
    }

    @Test
    public void testHashCode() {
        MutableByte num = MutableByte.of((byte) 42);
        Assertions.assertEquals(42, num.hashCode());
    }

    @Test
    public void testHashCodeNegative() {
        MutableByte num = MutableByte.of((byte) -10);
        Assertions.assertEquals(-10, num.hashCode());
    }

    @Test
    public void testHashCodeZero() {
        MutableByte num = MutableByte.of((byte) 0);
        Assertions.assertEquals(0, num.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        MutableByte num = MutableByte.of((byte) 25);
        int hash1 = num.hashCode();
        int hash2 = num.hashCode();
        Assertions.assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCodeEqualsContract() {
        MutableByte a = MutableByte.of((byte) 50);
        MutableByte b = MutableByte.of((byte) 50);
        Assertions.assertTrue(a.equals(b));
        Assertions.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testHashCodeMinMax() {
        MutableByte min = MutableByte.of(Byte.MIN_VALUE);
        MutableByte max = MutableByte.of(Byte.MAX_VALUE);
        Assertions.assertEquals(Byte.MIN_VALUE, min.hashCode());
        Assertions.assertEquals(Byte.MAX_VALUE, max.hashCode());
    }

    @Test
    public void testToString() {
        MutableByte num = MutableByte.of((byte) 42);
        Assertions.assertEquals("42", num.toString());
    }

    @Test
    public void testToStringZero() {
        MutableByte num = MutableByte.of((byte) 0);
        Assertions.assertEquals("0", num.toString());
    }

    @Test
    public void testToStringNegative() {
        MutableByte num = MutableByte.of((byte) -75);
        Assertions.assertEquals("-75", num.toString());
    }

    @Test
    public void testToStringMinMax() {
        MutableByte min = MutableByte.of(Byte.MIN_VALUE);
        MutableByte max = MutableByte.of(Byte.MAX_VALUE);
        Assertions.assertEquals("-128", min.toString());
        Assertions.assertEquals("127", max.toString());
    }

    @Test
    public void testToStringAfterModification() {
        MutableByte num = MutableByte.of((byte) 10);
        num.setValue((byte) 20);
        Assertions.assertEquals("20", num.toString());
    }

    @Test
    public void testOverflowUnderflowCombined() {
        MutableByte num = MutableByte.of(Byte.MAX_VALUE);
        num.increment();
        Assertions.assertEquals(Byte.MIN_VALUE, num.value());
        num.decrement();
        Assertions.assertEquals(Byte.MAX_VALUE, num.value());
    }

    @Test
    public void testComplexArithmeticSequence() {
        MutableByte num = MutableByte.of((byte) 10);
        num.add((byte) 5);
        num.increment();
        num.subtract((byte) 3);
        num.decrement();
        num.add((byte) -2);
        Assertions.assertEquals((byte) 10, num.value());
    }

    @Test
    public void testAllGetterSetterCombinations() {
        MutableByte num = MutableByte.of((byte) 5);

        Assertions.assertEquals((byte) 5, num.value());

        num.setValue((byte) 10);
        Assertions.assertEquals((byte) 10, num.getValue());

        byte old = num.getAndSet((byte) 15);
        Assertions.assertEquals((byte) 10, old);
        Assertions.assertEquals((byte) 15, num.value());

        byte newVal = num.setAndGet((byte) 20);
        Assertions.assertEquals((byte) 20, newVal);
        Assertions.assertEquals((byte) 20, num.value());
    }

    @Test
    public void testAllIncrementDecrementVariations() {
        MutableByte num = MutableByte.of((byte) 10);

        Assertions.assertEquals((byte) 10, num.getAndIncrement());
        Assertions.assertEquals((byte) 11, num.value());

        Assertions.assertEquals((byte) 12, num.incrementAndGet());
        Assertions.assertEquals((byte) 12, num.value());

        Assertions.assertEquals((byte) 12, num.getAndDecrement());
        Assertions.assertEquals((byte) 11, num.value());

        Assertions.assertEquals((byte) 10, num.decrementAndGet());
        Assertions.assertEquals((byte) 10, num.value());
    }

    @Test
    public void testAllAddVariations() {
        MutableByte num = MutableByte.of((byte) 10);

        num.add((byte) 5);
        Assertions.assertEquals((byte) 15, num.value());

        byte old = num.getAndAdd((byte) 10);
        Assertions.assertEquals((byte) 15, old);
        Assertions.assertEquals((byte) 25, num.value());

        byte result = num.addAndGet((byte) 5);
        Assertions.assertEquals((byte) 30, result);
        Assertions.assertEquals((byte) 30, num.value());
    }

    @Test
    public void testAllNumberConversions() {
        MutableByte num = MutableByte.of((byte) 100);

        Assertions.assertEquals((byte) 100, num.byteValue());
        Assertions.assertEquals((short) 100, num.shortValue());
        Assertions.assertEquals(100, num.intValue());
        Assertions.assertEquals(100L, num.longValue());
        Assertions.assertEquals(100.0f, num.floatValue(), 0.0f);
        Assertions.assertEquals(100.0, num.doubleValue(), 0.0);
    }

    @Test
    public void testMutabilityVerification() {
        MutableByte num = MutableByte.of((byte) 1);

        num.increment();
        Assertions.assertEquals((byte) 2, num.value());

        num.setValue((byte) 10);
        Assertions.assertEquals((byte) 10, num.value());

        num.add((byte) 5);
        Assertions.assertEquals((byte) 15, num.value());

        Assertions.assertNotEquals((byte) 1, num.value());
    }
}
