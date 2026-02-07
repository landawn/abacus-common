package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class MutableBoolean100Test extends TestBase {

    @Test
    public void testOf() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertTrue(mutableBoolean.value());

        mutableBoolean = MutableBoolean.of(false);
        Assertions.assertFalse(mutableBoolean.value());
    }

    @Test
    public void testValue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertTrue(mutableBoolean.value());
    }

    @Test
    public void testGetValue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        Assertions.assertFalse(mutableBoolean.getValue());
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
    public void testGetAndSet() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        boolean oldValue = mutableBoolean.getAndSet(false);
        Assertions.assertTrue(oldValue);
        Assertions.assertFalse(mutableBoolean.value());
    }

    @Test
    public void testSetAndGet() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        boolean newValue = mutableBoolean.setAndGet(true);
        Assertions.assertTrue(newValue);
        Assertions.assertTrue(mutableBoolean.value());
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
    public void testSetFalse() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        mutableBoolean.setFalse();
        Assertions.assertFalse(mutableBoolean.value());

        mutableBoolean.setFalse();
        Assertions.assertFalse(mutableBoolean.value());
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
    public void testIsTrue() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertTrue(mutableBoolean.isTrue());

        mutableBoolean.setValue(false);
        Assertions.assertFalse(mutableBoolean.isTrue());
    }

    @Test
    public void testIsFalse() {
        MutableBoolean mutableBoolean = MutableBoolean.of(false);
        Assertions.assertTrue(mutableBoolean.isFalse());

        mutableBoolean.setValue(true);
        Assertions.assertFalse(mutableBoolean.isFalse());
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
    public void testCompareTo() {
        MutableBoolean falseVal = MutableBoolean.of(false);
        MutableBoolean trueVal = MutableBoolean.of(true);
        MutableBoolean anotherFalse = MutableBoolean.of(false);

        Assertions.assertTrue(falseVal.compareTo(trueVal) < 0);
        Assertions.assertTrue(trueVal.compareTo(falseVal) > 0);
        Assertions.assertEquals(0, falseVal.compareTo(anotherFalse));
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
    public void testToString() {
        MutableBoolean mutableBoolean = MutableBoolean.of(true);
        Assertions.assertEquals("true", mutableBoolean.toString());

        mutableBoolean.setValue(false);
        Assertions.assertEquals("false", mutableBoolean.toString());
    }
}
