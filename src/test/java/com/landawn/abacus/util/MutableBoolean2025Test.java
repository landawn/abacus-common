package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MutableBoolean2025Test extends TestBase {

    @Test
    public void testOf_true() {
        MutableBoolean mb = MutableBoolean.of(true);
        assertTrue(mb.value());
    }

    @Test
    public void testOf_false() {
        MutableBoolean mb = MutableBoolean.of(false);
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
    public void testSetValue_toTrue() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.setValue(true);
        assertTrue(mb.value());
    }

    @Test
    public void testSetValue_toFalse() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.setValue(false);
        assertFalse(mb.value());
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
        boolean updated = mb.setIf(true, v -> !v);
        assertTrue(updated);
        assertTrue(mb.value());
    }

    @Test
    public void testSetIf_predicateFalse() throws Exception {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean updated = mb.setIf(false, v -> !v);
        assertFalse(updated);
        assertTrue(mb.value());
    }

    @Test
    public void testSetIf_alwaysTrue() throws Exception {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean updated = mb.setIf(true, v -> true);
        assertTrue(updated);
        assertTrue(mb.value());
    }

    @Test
    public void testSetIf_alwaysFalse() throws Exception {
        MutableBoolean mb = MutableBoolean.of(false);
        boolean updated = mb.setIf(true, v -> false);
        assertFalse(updated);
        assertFalse(mb.value());
    }

    @Test
    public void testSetIf_complexPredicate() throws Exception {
        MutableBoolean mb = MutableBoolean.of(true);
        boolean updated = mb.setIf(false, v -> v);
        assertTrue(updated);
        assertFalse(mb.value());
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
    public void testInvert_fromTrue() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.invert();
        assertFalse(mb.value());
    }

    @Test
    public void testInvert_fromFalse() {
        MutableBoolean mb = MutableBoolean.of(false);
        mb.invert();
        assertTrue(mb.value());
    }

    @Test
    public void testInvert_multiple() {
        MutableBoolean mb = MutableBoolean.of(true);
        mb.invert();
        assertFalse(mb.value());
        mb.invert();
        assertTrue(mb.value());
        mb.invert();
        assertFalse(mb.value());
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
    public void testHashCode_equalObjectsSameHash() {
        MutableBoolean mb1 = MutableBoolean.of(true);
        MutableBoolean mb2 = MutableBoolean.of(true);
        assertEquals(mb1.hashCode(), mb2.hashCode());
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

        boolean updated = flag.setIf(false, v -> v);
        assertTrue(updated);
        assertFalse(flag.value());
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

    @Test
    public void testIntegration_compareAndSort() {
        MutableBoolean mb1 = MutableBoolean.of(false);
        MutableBoolean mb2 = MutableBoolean.of(true);
        MutableBoolean mb3 = MutableBoolean.of(false);

        assertTrue(mb1.compareTo(mb2) < 0);
        assertTrue(mb2.compareTo(mb1) > 0);
        assertEquals(0, mb1.compareTo(mb3));
    }
}
