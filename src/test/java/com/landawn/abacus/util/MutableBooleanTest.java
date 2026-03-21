package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableBooleanTest extends TestBase {

    @Test
    public void testInvert_fromTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.negate();
        assertFalse(mb.value());
    }

    @Test
    public void testInvert_fromFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.negate();
        assertTrue(mb.value());
    }

    @Test
    public void testOf() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertTrue(mutableBoolean.value());

        mutableBoolean = MutableBoolean.of(false);
        Assertions.assertFalse(mutableBoolean.value());
    }

    @Test
    public void testInvert() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        mutableBoolean.negate();
        Assertions.assertFalse(mutableBoolean.value());

        mutableBoolean.negate();
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testInvert_multiple() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.negate();
        assertFalse(mb.value());
        mb.negate();
        assertTrue(mb.value());
        mb.negate();
        assertFalse(mb.value());
    }

    @Test
    public void testValue_true() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertTrue(mb.value());
    }

    @Test
    public void testValue_false() {
        MutableBoolean mb = MutableBoolean.of(false);
        assertFalse(mb.value());
    }

    @Test
    public void testValue_afterSetValue() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setValue(true);
        assertTrue(mb.value());
    }

    @Test
    public void testValue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testGetValue_true() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertTrue(mb.getValue());
    }

    @Test
    public void testGetValue_false() {
        MutableBoolean mb = MutableBoolean.of(false);
        assertFalse(mb.getValue());
    }

    @Test
    public void testGetValue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        Assertions.assertFalse(mutableBoolean.getValue());
    }

    @Test
    public void testSetValue_toFalse() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.setValue(false);
        assertFalse(mb.value());
    }

    @Test
    public void testSetValue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        mutableBoolean.setValue(true);
        Assertions.assertTrue(mutableBoolean.value());

        mutableBoolean.setValue(false);
        Assertions.assertFalse(mutableBoolean.value());
    }

    @Test
    public void testSetValue_multipleChanges() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setValue(true);
        assertTrue(mb.value());
        mb.setValue(false);
        assertFalse(mb.value());
        mb.setValue(true);
        assertTrue(mb.value());
    }

    @Test
    public void testGetAndSet_falseToTrue() {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean old = mb.getAndSet(true);
        assertFalse(old);
        assertTrue(mb.value());
    }

    @Test
    public void testGetAndSet_trueToFalse() {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean old = mb.getAndSet(false);
        assertTrue(old);
        assertFalse(mb.value());
    }

    @Test
    public void testGetAndSet() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        boolean oldValue = mutableBoolean.getAndSet(false);
        Assertions.assertTrue(oldValue);
        Assertions.assertFalse(mutableBoolean.value());
    }

    @Test
    public void testGetAndSet_sameValue() {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean old = mb.getAndSet(true);
        assertTrue(old);
        assertTrue(mb.value());
    }

    @Test
    public void testSetAndGet_falseToTrue() {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean result = mb.setAndGet(true);
        assertTrue(result);
        assertTrue(mb.value());
    }

    @Test
    public void testSetAndGet_trueToFalse() {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean result = mb.setAndGet(false);
        assertFalse(result);
        assertFalse(mb.value());
    }

    @Test
    public void testSetAndGet() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        boolean newValue = mutableBoolean.setAndGet(true);
        Assertions.assertTrue(newValue);
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testSetAndGet_sameValue() {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean result = mb.setAndGet(false);
        assertFalse(result);
        assertFalse(mb.value());
    }

    @Test
    public void testGetAndNegate_fromTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean old = mb.getAndNegate();
        assertTrue(old);
        assertFalse(mb.value());
    }

    @Test
    public void testGetAndNegate_fromFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean old = mb.getAndNegate();
        assertFalse(old);
        assertTrue(mb.value());
    }

    @Test
    public void testGetAndNegate() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        boolean oldValue = mutableBoolean.getAndNegate();
        Assertions.assertTrue(oldValue);
        Assertions.assertFalse(mutableBoolean.value());

        oldValue = mutableBoolean.getAndNegate();
        Assertions.assertFalse(oldValue);
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testGetAndNegate_multiple() {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean old1 = mb.getAndNegate();
        assertTrue(old1);
        assertFalse(mb.value());

        boolean old2 = mb.getAndNegate();
        assertFalse(old2);
        assertTrue(mb.value());
    }

    @Test
    public void testNegateAndGet_fromTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean result = mb.negateAndGet();
        assertFalse(result);
        assertFalse(mb.value());
    }

    @Test
    public void testNegateAndGet_fromFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean result = mb.negateAndGet();
        assertTrue(result);
        assertTrue(mb.value());
    }

    @Test
    public void testNegateAndGet() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        boolean newValue = mutableBoolean.negateAndGet();
        Assertions.assertFalse(newValue);
        Assertions.assertFalse(mutableBoolean.value());

        newValue = mutableBoolean.negateAndGet();
        Assertions.assertTrue(newValue);
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testNegateAndGet_multiple() {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean result1 = mb.negateAndGet();
        assertFalse(result1);

        boolean result2 = mb.negateAndGet();
        assertTrue(result2);
    }

    @Test
    public void testSetIf_predicateTrue() throws Exception {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean updated = mb.setIf(v -> !v, true);
        assertTrue(updated);
        assertTrue(mb.value());
    }

    @Test
    public void testSetIf_predicateFalse() throws Exception {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean updated = mb.setIf(v -> !v, false);
        assertFalse(updated);
        assertTrue(mb.value());
    }

    @Test
    public void testSetIf_alwaysTrue() throws Exception {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean updated = mb.setIf(v -> true, true);
        assertTrue(updated);
        assertTrue(mb.value());
    }

    @Test
    public void testSetIf_alwaysFalse() throws Exception {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean updated = mb.setIf(v -> false, true);
        assertFalse(updated);
        assertFalse(mb.value());
    }

    @Test
    public void testSetIf_complexPredicate() throws Exception {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean updated = mb.setIf(v -> v, false);
        assertTrue(updated);
        assertFalse(mb.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);

        boolean updated = mutableBoolean.setIf(v -> !v, true);
        Assertions.assertTrue(updated);
        Assertions.assertTrue(mutableBoolean.value());

        updated = mutableBoolean.setIf(v -> !v, false);
        Assertions.assertFalse(updated);
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testSetFalse_fromTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.setFalse();
        assertFalse(mb.value());
    }

    @Test
    public void testSetFalse_fromFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setFalse();
        assertFalse(mb.value());
    }

    @Test
    public void testSetFalse() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        mutableBoolean.setFalse();
        Assertions.assertFalse(mutableBoolean.value());

        mutableBoolean.setFalse();
        Assertions.assertFalse(mutableBoolean.value());
    }

    @Test
    public void testSetFalse_multiple() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.setFalse();
        mb.setFalse();
        assertFalse(mb.value());
    }

    @Test
    public void testSetTrue_fromFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setTrue();
        assertTrue(mb.value());
    }

    @Test
    public void testSetTrue_fromTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.setTrue();
        assertTrue(mb.value());
    }

    @Test
    public void testSetTrue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        mutableBoolean.setTrue();
        Assertions.assertTrue(mutableBoolean.value());

        mutableBoolean.setTrue();
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testSetTrue_multiple() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setTrue();
        mb.setTrue();
        assertTrue(mb.value());
    }

    @Test
    public void testIsTrue_whenTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertTrue(mb.isTrue());
    }

    @Test
    public void testIsTrue_whenFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        assertFalse(mb.isTrue());
    }

    @Test
    public void testIsTrue_afterSetTrue() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setTrue();
        assertTrue(mb.isTrue());
    }

    @Test
    public void testIsTrue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertTrue(mutableBoolean.isTrue());

        mutableBoolean.setValue(false);
        Assertions.assertFalse(mutableBoolean.isTrue());
    }

    @Test
    public void testIsFalse_whenTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertFalse(mb.isFalse());
    }

    @Test
    public void testIsFalse_whenFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        assertTrue(mb.isFalse());
    }

    @Test
    public void testIsFalse_afterSetFalse() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.setFalse();
        assertTrue(mb.isFalse());
    }

    @Test
    public void testIsFalse() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        Assertions.assertTrue(mutableBoolean.isFalse());

        mutableBoolean.setValue(true);
        Assertions.assertFalse(mutableBoolean.isFalse());
    }

    @Test
    public void testNegate() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.negate();
        Assertions.assertFalse(mb.value());

        mb.negate();
        Assertions.assertTrue(mb.value());

        MutableBoolean mbFalse = MutableBoolean.of(false);
        mbFalse.negate();
        Assertions.assertTrue(mbFalse.value());
    }

    @Test
    public void testCompareTo_bothTrue() {
        MutableBoolean mb1 = MutableBoolean.of(true);
        MutableBoolean mb2 = MutableBoolean.of(true);
        assertEquals(0, mb1.compareTo(mb2));
    }

    @Test
    public void testCompareTo_bothFalse() {
        MutableBoolean mb1 = MutableBoolean.of(false);
        MutableBoolean mb2 = MutableBoolean.of(false);
        assertEquals(0, mb1.compareTo(mb2));
    }

    @Test
    public void testCompareTo_trueVsFalse() {
        MutableBoolean mb1 = MutableBoolean.of(true);
        MutableBoolean mb2 = MutableBoolean.of(false);
        assertTrue(mb1.compareTo(mb2) > 0);
    }

    @Test
    public void testCompareTo_falseVsTrue() {
        MutableBoolean mb1 = MutableBoolean.of(false);
        MutableBoolean mb2 = MutableBoolean.of(true);
        assertTrue(mb1.compareTo(mb2) < 0);
    }

    @Test
    public void testCompareTo() {
        MutableBoolean falseVal = MutableBoolean.of(false);
        MutableBoolean trueVal = MutableBoolean.of(true);
        MutableBoolean anotherFalse = MutableBoolean.of(false);

        Assertions.assertTrue(falseVal.compareTo(trueVal) < 0);
        Assertions.assertTrue(trueVal.compareTo(falseVal) > 0);
        Assertions.assertEquals(0, falseVal.compareTo(anotherFalse));
    }

    @Test
    public void testCompareTo_sameInstance() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertEquals(0, mb.compareTo(mb));
    }

    @Test
    public void testEquals_bothTrue() {
        MutableBoolean mb1 = MutableBoolean.of(true);
        MutableBoolean mb2 = MutableBoolean.of(true);
        assertEquals(mb1, mb2);
    }

    @Test
    public void testEquals_bothFalse() {
        MutableBoolean mb1 = MutableBoolean.of(false);
        MutableBoolean mb2 = MutableBoolean.of(false);
        assertEquals(mb1, mb2);
    }

    @Test
    public void testEquals_differentValues() {
        MutableBoolean mb1 = MutableBoolean.of(true);
        MutableBoolean mb2 = MutableBoolean.of(false);
        assertNotEquals(mb1, mb2);
    }

    @Test
    public void testEquals_differentType() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertNotEquals(mb, Boolean.TRUE);
    }

    @Test
    public void testEquals_afterMutation() {
        MutableBoolean mb1 = MutableBoolean.of(true);
        MutableBoolean mb2 = MutableBoolean.of(false);
        assertNotEquals(mb1, mb2);
        mb2.setValue(true);
        assertEquals(mb1, mb2);
    }

    @Test
    public void testEquals_sameInstance() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertEquals(mb, mb);
    }

    @Test
    public void testEquals_null() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertNotEquals(mb, null);
    }

    @Test
    public void testEquals() {
        MutableBoolean a = MutableBoolean.of(true);
        MutableBoolean b = MutableBoolean.of(true);
        MutableBoolean c = MutableBoolean.of(false);

        Assertions.assertTrue(a.equals(b));
        Assertions.assertFalse(a.equals(c));
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("true"));
    }

    @Test
    public void testHashCode_true() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertEquals(Boolean.TRUE.hashCode(), mb.hashCode());
    }

    @Test
    public void testHashCode_false() {
        MutableBoolean mb = MutableBoolean.of(false);
        assertEquals(Boolean.FALSE.hashCode(), mb.hashCode());
    }

    @Test
    public void testHashCode_consistency() {
        MutableBoolean mb = MutableBoolean.of(true);
        int hash1 = mb.hashCode();
        int hash2 = mb.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testHashCode_afterMutation() {
        MutableBoolean mb = MutableBoolean.of(true);
        int hash1 = mb.hashCode();
        mb.setValue(false);
        int hash2 = mb.hashCode();
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void testHashCode() {
        MutableBoolean trueVal = MutableBoolean.of(true);
        MutableBoolean anotherTrue = MutableBoolean.of(true);
        MutableBoolean falseVal = MutableBoolean.of(false);

        Assertions.assertEquals(trueVal.hashCode(), anotherTrue.hashCode());
        Assertions.assertNotEquals(trueVal.hashCode(), falseVal.hashCode());
        Assertions.assertEquals(Boolean.TRUE.hashCode(), trueVal.hashCode());
        Assertions.assertEquals(Boolean.FALSE.hashCode(), falseVal.hashCode());
    }

    @Test
    public void testHashCode_equalObjectsSameHash() {
        MutableBoolean mb1 = MutableBoolean.of(true);
        MutableBoolean mb2 = MutableBoolean.of(true);
        assertEquals(mb1.hashCode(), mb2.hashCode());
    }

    @Test
    public void testToString_true() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertEquals("true", mb.toString());
    }

    @Test
    public void testToString_false() {
        MutableBoolean mb = MutableBoolean.of(false);
        assertEquals("false", mb.toString());
    }

    @Test
    public void testToString_afterMutation() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertEquals("true", mb.toString());
        mb.setValue(false);
        assertEquals("false", mb.toString());
    }

    @Test
    public void testToString() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertEquals("true", mutableBoolean.toString());

        mutableBoolean.setValue(false);
        Assertions.assertEquals("false", mutableBoolean.toString());
    }

    @Test
    public void testIntegration_complexScenario() {
        MutableBoolean flag = MutableBoolean.of(false);

        assertFalse(flag.value());
        assertTrue(flag.isFalse());

        flag.setTrue();
        assertTrue(flag.isTrue());

        boolean old = flag.getAndNegate();
        assertTrue(old);
        assertFalse(flag.value());

        boolean newVal = flag.negateAndGet();
        assertTrue(newVal);

        boolean updated = flag.setIf(v -> v, false);
        assertTrue(updated);
        assertFalse(flag.value());
    }

    @Test
    public void testIntegration_compareAndSort() {
        MutableBoolean mb1 = MutableBoolean.of(false);
        MutableBoolean mb2 = MutableBoolean.of(true);
        MutableBoolean mb3 = MutableBoolean.of(false);

        assertTrue(mb1.compareTo(mb2) < 0);
        assertTrue(mb2.compareTo(mb1) > 0);
        assertEquals(0, mb1.compareTo(mb3));
    }

    @Test
    public void testIntegration_lambdaUsage() throws Exception {
        MutableBoolean found = MutableBoolean.of(false);

        String[] items = { "apple", "banana", "cherry" };
        for (String item : items) {
            if (item.startsWith("b")) {
                found.setTrue();
            }
        }

        assertTrue(found.value());
    }

}
