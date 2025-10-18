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
public class MutableLong2025Test extends TestBase {

    @Test
    public void test_of() {
        MutableLong ml = MutableLong.of(42L);
        assertNotNull(ml);
        assertEquals(42L, ml.value());
    }

    @Test
    public void test_of_zero() {
        MutableLong ml = MutableLong.of(0L);
        assertEquals(0L, ml.value());
    }

    @Test
    public void test_of_negative() {
        MutableLong ml = MutableLong.of(-100L);
        assertEquals(-100L, ml.value());
    }

    @Test
    public void test_of_maxValue() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, ml.value());
    }

    @Test
    public void test_of_minValue() {
        MutableLong ml = MutableLong.of(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, ml.value());
    }

    @Test
    public void test_value() {
        MutableLong ml = MutableLong.of(12345L);
        assertEquals(12345L, ml.value());
    }

    @Test
    public void test_getValue_deprecated() {
        MutableLong ml = MutableLong.of(99999L);
        assertEquals(99999L, ml.getValue());
    }

    @Test
    public void test_setValue() {
        MutableLong ml = MutableLong.of(10L);
        ml.setValue(20L);
        assertEquals(20L, ml.value());
    }

    @Test
    public void test_setValue_multipleChanges() {
        MutableLong ml = MutableLong.of(1L);
        ml.setValue(2L);
        assertEquals(2L, ml.value());
        ml.setValue(3L);
        assertEquals(3L, ml.value());
    }

    @Test
    public void test_setValue_negative() {
        MutableLong ml = MutableLong.of(100L);
        ml.setValue(-50L);
        assertEquals(-50L, ml.value());
    }

    @Test
    public void test_getAndSet() {
        MutableLong ml = MutableLong.of(10L);
        long oldValue = ml.getAndSet(20L);
        assertEquals(10L, oldValue);
        assertEquals(20L, ml.value());
    }

    @Test
    public void test_getAndSet_sameValue() {
        MutableLong ml = MutableLong.of(42L);
        long oldValue = ml.getAndSet(42L);
        assertEquals(42L, oldValue);
        assertEquals(42L, ml.value());
    }

    @Test
    public void test_setAndGet() {
        MutableLong ml = MutableLong.of(10L);
        long newValue = ml.setAndGet(20L);
        assertEquals(20L, newValue);
        assertEquals(20L, ml.value());
    }

    @Test
    public void test_setAndGet_chainedOperations() {
        MutableLong ml = MutableLong.of(5L);
        long val1 = ml.setAndGet(10L);
        long val2 = ml.setAndGet(15L);
        assertEquals(10L, val1);
        assertEquals(15L, val2);
        assertEquals(15L, ml.value());
    }

    @Test
    public void test_setIf_conditionTrue() throws Exception {
        MutableLong ml = MutableLong.of(10L);
        boolean updated = ml.setIf(20L, v -> v < 15L);
        assertTrue(updated);
        assertEquals(20L, ml.value());
    }

    @Test
    public void test_setIf_conditionFalse() throws Exception {
        MutableLong ml = MutableLong.of(10L);
        boolean updated = ml.setIf(20L, v -> v > 15L);
        assertFalse(updated);
        assertEquals(10L, ml.value());
    }

    @Test
    public void test_setIf_multipleConditions() throws Exception {
        MutableLong ml = MutableLong.of(10L);
        ml.setIf(20L, v -> v < 15L);
        assertEquals(20L, ml.value());

        boolean updated = ml.setIf(30L, v -> v < 15L);
        assertFalse(updated);
        assertEquals(20L, ml.value());
    }

    @Test
    public void test_setIf_equalityCondition() throws Exception {
        MutableLong ml = MutableLong.of(10L);
        boolean updated = ml.setIf(20L, v -> v == 10L);
        assertTrue(updated);
        assertEquals(20L, ml.value());
    }

    @Test
    public void test_increment() {
        MutableLong ml = MutableLong.of(10L);
        ml.increment();
        assertEquals(11L, ml.value());
    }

    @Test
    public void test_increment_multiple() {
        MutableLong ml = MutableLong.of(0L);
        ml.increment();
        ml.increment();
        ml.increment();
        assertEquals(3L, ml.value());
    }

    @Test
    public void test_increment_fromNegative() {
        MutableLong ml = MutableLong.of(-1L);
        ml.increment();
        assertEquals(0L, ml.value());
    }

    @Test
    public void test_increment_overflow() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        ml.increment();
        assertEquals(Long.MIN_VALUE, ml.value());
    }

    @Test
    public void test_decrement() {
        MutableLong ml = MutableLong.of(10L);
        ml.decrement();
        assertEquals(9L, ml.value());
    }

    @Test
    public void test_decrement_multiple() {
        MutableLong ml = MutableLong.of(5L);
        ml.decrement();
        ml.decrement();
        assertEquals(3L, ml.value());
    }

    @Test
    public void test_decrement_toNegative() {
        MutableLong ml = MutableLong.of(1L);
        ml.decrement();
        ml.decrement();
        assertEquals(-1L, ml.value());
    }

    @Test
    public void test_decrement_underflow() {
        MutableLong ml = MutableLong.of(Long.MIN_VALUE);
        ml.decrement();
        assertEquals(Long.MAX_VALUE, ml.value());
    }

    @Test
    public void test_add() {
        MutableLong ml = MutableLong.of(10L);
        ml.add(5L);
        assertEquals(15L, ml.value());
    }

    @Test
    public void test_add_negative() {
        MutableLong ml = MutableLong.of(10L);
        ml.add(-3L);
        assertEquals(7L, ml.value());
    }

    @Test
    public void test_add_zero() {
        MutableLong ml = MutableLong.of(42L);
        ml.add(0L);
        assertEquals(42L, ml.value());
    }

    @Test
    public void test_add_large() {
        MutableLong ml = MutableLong.of(1000000L);
        ml.add(2000000L);
        assertEquals(3000000L, ml.value());
    }

    @Test
    public void test_subtract() {
        MutableLong ml = MutableLong.of(10L);
        ml.subtract(3L);
        assertEquals(7L, ml.value());
    }

    @Test
    public void test_subtract_negative() {
        MutableLong ml = MutableLong.of(10L);
        ml.subtract(-5L);
        assertEquals(15L, ml.value());
    }

    @Test
    public void test_subtract_zero() {
        MutableLong ml = MutableLong.of(42L);
        ml.subtract(0L);
        assertEquals(42L, ml.value());
    }

    @Test
    public void test_subtract_toNegative() {
        MutableLong ml = MutableLong.of(5L);
        ml.subtract(10L);
        assertEquals(-5L, ml.value());
    }

    @Test
    public void test_getAndIncrement() {
        MutableLong ml = MutableLong.of(10L);
        long oldValue = ml.getAndIncrement();
        assertEquals(10L, oldValue);
        assertEquals(11L, ml.value());
    }

    @Test
    public void test_getAndIncrement_multiple() {
        MutableLong ml = MutableLong.of(0L);
        assertEquals(0L, ml.getAndIncrement());
        assertEquals(1L, ml.getAndIncrement());
        assertEquals(2L, ml.getAndIncrement());
        assertEquals(3L, ml.value());
    }

    @Test
    public void test_getAndDecrement() {
        MutableLong ml = MutableLong.of(10L);
        long oldValue = ml.getAndDecrement();
        assertEquals(10L, oldValue);
        assertEquals(9L, ml.value());
    }

    @Test
    public void test_getAndDecrement_multiple() {
        MutableLong ml = MutableLong.of(5L);
        assertEquals(5L, ml.getAndDecrement());
        assertEquals(4L, ml.getAndDecrement());
        assertEquals(3L, ml.value());
    }

    @Test
    public void test_incrementAndGet() {
        MutableLong ml = MutableLong.of(10L);
        long newValue = ml.incrementAndGet();
        assertEquals(11L, newValue);
        assertEquals(11L, ml.value());
    }

    @Test
    public void test_incrementAndGet_multiple() {
        MutableLong ml = MutableLong.of(0L);
        assertEquals(1L, ml.incrementAndGet());
        assertEquals(2L, ml.incrementAndGet());
        assertEquals(2L, ml.value());
    }

    @Test
    public void test_decrementAndGet() {
        MutableLong ml = MutableLong.of(10L);
        long newValue = ml.decrementAndGet();
        assertEquals(9L, newValue);
        assertEquals(9L, ml.value());
    }

    @Test
    public void test_decrementAndGet_multiple() {
        MutableLong ml = MutableLong.of(5L);
        assertEquals(4L, ml.decrementAndGet());
        assertEquals(3L, ml.decrementAndGet());
        assertEquals(3L, ml.value());
    }

    @Test
    public void test_getAndAdd() {
        MutableLong ml = MutableLong.of(10L);
        long oldValue = ml.getAndAdd(5L);
        assertEquals(10L, oldValue);
        assertEquals(15L, ml.value());
    }

    @Test
    public void test_getAndAdd_negative() {
        MutableLong ml = MutableLong.of(10L);
        long oldValue = ml.getAndAdd(-3L);
        assertEquals(10L, oldValue);
        assertEquals(7L, ml.value());
    }

    @Test
    public void test_getAndAdd_zero() {
        MutableLong ml = MutableLong.of(42L);
        long oldValue = ml.getAndAdd(0L);
        assertEquals(42L, oldValue);
        assertEquals(42L, ml.value());
    }

    @Test
    public void test_addAndGet() {
        MutableLong ml = MutableLong.of(10L);
        long newValue = ml.addAndGet(5L);
        assertEquals(15L, newValue);
        assertEquals(15L, ml.value());
    }

    @Test
    public void test_addAndGet_negative() {
        MutableLong ml = MutableLong.of(10L);
        long newValue = ml.addAndGet(-3L);
        assertEquals(7L, newValue);
        assertEquals(7L, ml.value());
    }

    @Test
    public void test_addAndGet_zero() {
        MutableLong ml = MutableLong.of(42L);
        long newValue = ml.addAndGet(0L);
        assertEquals(42L, newValue);
        assertEquals(42L, ml.value());
    }

    @Test
    public void test_addAndGet_multiple() {
        MutableLong ml = MutableLong.of(0L);
        assertEquals(5L, ml.addAndGet(5L));
        assertEquals(15L, ml.addAndGet(10L));
        assertEquals(15L, ml.value());
    }

    @Test
    public void test_intValue() {
        MutableLong ml = MutableLong.of(42L);
        assertEquals(42, ml.intValue());
    }

    @Test
    public void test_intValue_truncation() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        assertEquals(-1, ml.intValue());
    }

    @Test
    public void test_intValue_negative() {
        MutableLong ml = MutableLong.of(-100L);
        assertEquals(-100, ml.intValue());
    }

    @Test
    public void test_intValue_withinIntRange() {
        MutableLong ml = MutableLong.of(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, ml.intValue());
    }

    @Test
    public void test_longValue() {
        MutableLong ml = MutableLong.of(12345678901234L);
        assertEquals(12345678901234L, ml.longValue());
    }

    @Test
    public void test_longValue_maxValue() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, ml.longValue());
    }

    @Test
    public void test_longValue_minValue() {
        MutableLong ml = MutableLong.of(Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, ml.longValue());
    }

    @Test
    public void test_floatValue() {
        MutableLong ml = MutableLong.of(100L);
        assertEquals(100.0f, ml.floatValue(), 0.0001f);
    }

    @Test
    public void test_floatValue_precisionLoss() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        float result = ml.floatValue();
        assertTrue(result > 0);
    }

    @Test
    public void test_floatValue_negative() {
        MutableLong ml = MutableLong.of(-200L);
        assertEquals(-200.0f, ml.floatValue(), 0.0001f);
    }

    @Test
    public void test_doubleValue() {
        MutableLong ml = MutableLong.of(123456789L);
        assertEquals(123456789.0, ml.doubleValue(), 0.0001);
    }

    @Test
    public void test_doubleValue_maxValue() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, ml.doubleValue(), 0.0001);
    }

    @Test
    public void test_doubleValue_negative() {
        MutableLong ml = MutableLong.of(-987654321L);
        assertEquals(-987654321.0, ml.doubleValue(), 0.0001);
    }

    @Test
    public void test_compareTo_equal() {
        MutableLong ml1 = MutableLong.of(42L);
        MutableLong ml2 = MutableLong.of(42L);
        assertEquals(0, ml1.compareTo(ml2));
    }

    @Test
    public void test_compareTo_less() {
        MutableLong ml1 = MutableLong.of(10L);
        MutableLong ml2 = MutableLong.of(20L);
        assertTrue(ml1.compareTo(ml2) < 0);
    }

    @Test
    public void test_compareTo_greater() {
        MutableLong ml1 = MutableLong.of(30L);
        MutableLong ml2 = MutableLong.of(20L);
        assertTrue(ml1.compareTo(ml2) > 0);
    }

    @Test
    public void test_compareTo_negative() {
        MutableLong ml1 = MutableLong.of(-10L);
        MutableLong ml2 = MutableLong.of(-20L);
        assertTrue(ml1.compareTo(ml2) > 0);
    }

    @Test
    public void test_compareTo_mixedSigns() {
        MutableLong ml1 = MutableLong.of(-5L);
        MutableLong ml2 = MutableLong.of(5L);
        assertTrue(ml1.compareTo(ml2) < 0);
    }

    @Test
    public void test_compareTo_extremeValues() {
        MutableLong ml1 = MutableLong.of(Long.MAX_VALUE);
        MutableLong ml2 = MutableLong.of(Long.MIN_VALUE);
        assertTrue(ml1.compareTo(ml2) > 0);
    }

    @Test
    public void test_equals_same() {
        MutableLong ml = MutableLong.of(42L);
        assertTrue(ml.equals(ml));
    }

    @Test
    public void test_equals_equal() {
        MutableLong ml1 = MutableLong.of(42L);
        MutableLong ml2 = MutableLong.of(42L);
        assertTrue(ml1.equals(ml2));
        assertTrue(ml2.equals(ml1));
    }

    @Test
    public void test_equals_notEqual() {
        MutableLong ml1 = MutableLong.of(42L);
        MutableLong ml2 = MutableLong.of(43L);
        assertFalse(ml1.equals(ml2));
        assertFalse(ml2.equals(ml1));
    }

    @Test
    public void test_equals_null() {
        MutableLong ml = MutableLong.of(42L);
        assertFalse(ml.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        MutableLong ml = MutableLong.of(42L);
        assertFalse(ml.equals("42"));
        assertFalse(ml.equals(42L));
        assertFalse(ml.equals(42));
    }

    @Test
    public void test_equals_negative() {
        MutableLong ml1 = MutableLong.of(-100L);
        MutableLong ml2 = MutableLong.of(-100L);
        assertTrue(ml1.equals(ml2));
    }

    @Test
    public void test_equals_zero() {
        MutableLong ml1 = MutableLong.of(0L);
        MutableLong ml2 = MutableLong.of(0L);
        assertTrue(ml1.equals(ml2));
    }

    @Test
    public void test_hashCode_consistent() {
        MutableLong ml = MutableLong.of(42L);
        int hash1 = ml.hashCode();
        int hash2 = ml.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_hashCode_equal() {
        MutableLong ml1 = MutableLong.of(42L);
        MutableLong ml2 = MutableLong.of(42L);
        assertEquals(ml1.hashCode(), ml2.hashCode());
    }

    @Test
    public void test_hashCode_notEqual() {
        MutableLong ml1 = MutableLong.of(42L);
        MutableLong ml2 = MutableLong.of(43L);
        assertNotEquals(ml1.hashCode(), ml2.hashCode());
    }

    @Test
    public void test_hashCode_negative() {
        MutableLong ml1 = MutableLong.of(-100L);
        MutableLong ml2 = MutableLong.of(-100L);
        assertEquals(ml1.hashCode(), ml2.hashCode());
    }

    @Test
    public void test_hashCode_zero() {
        MutableLong ml = MutableLong.of(0L);
        assertEquals(Long.hashCode(0L), ml.hashCode());
    }

    @Test
    public void test_hashCode_extremeValues() {
        MutableLong mlMax = MutableLong.of(Long.MAX_VALUE);
        MutableLong mlMin = MutableLong.of(Long.MIN_VALUE);
        assertEquals(Long.hashCode(Long.MAX_VALUE), mlMax.hashCode());
        assertEquals(Long.hashCode(Long.MIN_VALUE), mlMin.hashCode());
    }

    @Test
    public void test_toString() {
        MutableLong ml = MutableLong.of(42L);
        String str = ml.toString();
        assertNotNull(str);
        assertEquals("42", str);
    }

    @Test
    public void test_toString_negative() {
        MutableLong ml = MutableLong.of(-100L);
        assertEquals("-100", ml.toString());
    }

    @Test
    public void test_toString_zero() {
        MutableLong ml = MutableLong.of(0L);
        assertEquals("0", ml.toString());
    }

    @Test
    public void test_toString_maxValue() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        assertEquals(String.valueOf(Long.MAX_VALUE), ml.toString());
    }

    @Test
    public void test_toString_minValue() {
        MutableLong ml = MutableLong.of(Long.MIN_VALUE);
        assertEquals(String.valueOf(Long.MIN_VALUE), ml.toString());
    }

    @Test
    public void test_toString_afterModification() {
        MutableLong ml = MutableLong.of(10L);
        ml.add(5L);
        assertEquals("15", ml.toString());
    }

    @Test
    public void test_combinedOperations_incrementAndAdd() {
        MutableLong ml = MutableLong.of(10L);
        ml.increment();
        ml.add(5L);
        assertEquals(16L, ml.value());
    }

    @Test
    public void test_combinedOperations_decrementAndSubtract() {
        MutableLong ml = MutableLong.of(20L);
        ml.decrement();
        ml.subtract(5L);
        assertEquals(14L, ml.value());
    }

    @Test
    public void test_combinedOperations_multipleGetAnd() {
        MutableLong ml = MutableLong.of(10L);
        long val1 = ml.getAndIncrement();
        long val2 = ml.getAndAdd(5L);
        long val3 = ml.getAndDecrement();

        assertEquals(10L, val1);
        assertEquals(11L, val2);
        assertEquals(16L, val3);
        assertEquals(15L, ml.value());
    }

    @Test
    public void test_combinedOperations_setIfChain() throws Exception {
        MutableLong ml = MutableLong.of(0L);
        ml.setIf(10L, v -> v == 0L);
        ml.setIf(20L, v -> v == 10L);
        ml.setIf(30L, v -> v < 25L);

        assertEquals(30L, ml.value());
    }

    @Test
    public void test_combinedOperations_mixedIncrementDecrement() {
        MutableLong ml = MutableLong.of(5L);
        ml.incrementAndGet();
        ml.decrementAndGet();
        ml.incrementAndGet();

        assertEquals(6L, ml.value());
    }

    @Test
    public void test_edgeCase_maxValueOperations() {
        MutableLong ml = MutableLong.of(Long.MAX_VALUE);
        long oldValue = ml.value();
        ml.increment();
        assertTrue(ml.value() < oldValue);
    }

    @Test
    public void test_edgeCase_minValueOperations() {
        MutableLong ml = MutableLong.of(Long.MIN_VALUE);
        long oldValue = ml.value();
        ml.decrement();
        assertTrue(ml.value() > oldValue);
    }

    @Test
    public void test_edgeCase_zeroOperations() {
        MutableLong ml = MutableLong.of(0L);
        ml.add(0L);
        ml.subtract(0L);
        ml.increment();
        ml.decrement();
        assertEquals(0L, ml.value());
    }

    @Test
    public void test_edgeCase_alternatingOperations() {
        MutableLong ml = MutableLong.of(0L);
        for (int i = 0; i < 100; i++) {
            ml.increment();
            ml.decrement();
        }
        assertEquals(0L, ml.value());
    }

    @Test
    public void test_edgeCase_largeAdditions() {
        MutableLong ml = MutableLong.of(0L);
        ml.add(Long.MAX_VALUE / 2);
        ml.add(Long.MAX_VALUE / 2);
        assertTrue(ml.value() > 0);
        assertEquals(Long.MAX_VALUE - 1, ml.value());
    }
}
