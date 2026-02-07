package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class MutableChar2025Test extends TestBase {

    @Test
    public void test_of() {
        MutableChar mc = MutableChar.of('A');
        assertEquals('A', mc.value());
    }

    @Test
    public void test_of_nullChar() {
        MutableChar mc = MutableChar.of('\0');
        assertEquals('\0', mc.value());
    }

    @Test
    public void test_of_maxChar() {
        MutableChar mc = MutableChar.of('\uffff');
        assertEquals('\uffff', mc.value());
    }

    @Test
    public void test_of_specialChars() {
        MutableChar mc = MutableChar.of('\n');
        assertEquals('\n', mc.value());

        mc = MutableChar.of('\t');
        assertEquals('\t', mc.value());

        mc = MutableChar.of(' ');
        assertEquals(' ', mc.value());
    }

    @Test
    public void test_value() {
        MutableChar mc = MutableChar.of('Z');
        assertEquals('Z', mc.value());
    }

    @Test
    public void test_value_afterModification() {
        MutableChar mc = MutableChar.of('A');
        mc.setValue('B');
        assertEquals('B', mc.value());
    }

    @Test
    public void test_getValue() {
        MutableChar mc = MutableChar.of('X');
        assertEquals('X', mc.getValue());
    }

    @Test
    public void test_setValue() {
        MutableChar mc = MutableChar.of('A');
        mc.setValue('Z');
        assertEquals('Z', mc.value());
    }

    @Test
    public void test_setValue_nullChar() {
        MutableChar mc = MutableChar.of('A');
        mc.setValue('\0');
        assertEquals('\0', mc.value());
    }

    @Test
    public void test_setValue_maxChar() {
        MutableChar mc = MutableChar.of('A');
        mc.setValue('\uffff');
        assertEquals('\uffff', mc.value());
    }

    @Test
    public void test_getAndSet() {
        MutableChar mc = MutableChar.of('A');
        char old = mc.getAndSet('B');
        assertEquals('A', old);
        assertEquals('B', mc.value());
    }

    @Test
    public void test_getAndSet_sameValue() {
        MutableChar mc = MutableChar.of('C');
        char old = mc.getAndSet('C');
        assertEquals('C', old);
        assertEquals('C', mc.value());
    }

    @Test
    public void test_getAndSet_nullChar() {
        MutableChar mc = MutableChar.of('X');
        char old = mc.getAndSet('\0');
        assertEquals('X', old);
        assertEquals('\0', mc.value());
    }

    @Test
    public void test_setAndGet() {
        MutableChar mc = MutableChar.of('A');
        char newVal = mc.setAndGet('B');
        assertEquals('B', newVal);
        assertEquals('B', mc.value());
    }

    @Test
    public void test_setAndGet_sameValue() {
        MutableChar mc = MutableChar.of('D');
        char newVal = mc.setAndGet('D');
        assertEquals('D', newVal);
        assertEquals('D', mc.value());
    }

    @Test
    public void test_setIf_conditionTrue() {
        MutableChar mc = MutableChar.of('A');
        boolean updated = mc.setIf(c -> c < 'M', 'Z');
        assertTrue(updated);
        assertEquals('Z', mc.value());
    }

    @Test
    public void test_setIf_conditionFalse() {
        MutableChar mc = MutableChar.of('Z');
        boolean updated = mc.setIf(c -> c < 'M', 'A');
        assertFalse(updated);
        assertEquals('Z', mc.value());
    }

    @Test
    public void test_setIf_alwaysTrue() {
        MutableChar mc = MutableChar.of('B');
        boolean updated = mc.setIf(c -> true, 'C');
        assertTrue(updated);
        assertEquals('C', mc.value());
    }

    @Test
    public void test_setIf_alwaysFalse() {
        MutableChar mc = MutableChar.of('B');
        boolean updated = mc.setIf(c -> false, 'C');
        assertFalse(updated);
        assertEquals('B', mc.value());
    }

    @Test
    public void test_setIf_predicateWithNullChar() {
        MutableChar mc = MutableChar.of('\0');
        boolean updated = mc.setIf(c -> c == '\0', 'A');
        assertTrue(updated);
        assertEquals('A', mc.value());
    }

    @Test
    public void test_increment() {
        MutableChar mc = MutableChar.of('A');
        mc.increment();
        assertEquals('B', mc.value());
    }

    @Test
    public void test_increment_multiple() {
        MutableChar mc = MutableChar.of('A');
        mc.increment();
        mc.increment();
        mc.increment();
        assertEquals('D', mc.value());
    }

    @Test
    public void test_increment_overflow() {
        MutableChar mc = MutableChar.of('\uffff');
        mc.increment();
        assertEquals('\0', mc.value());
    }

    @Test
    public void test_increment_fromZero() {
        MutableChar mc = MutableChar.of('\0');
        mc.increment();
        assertEquals('\u0001', mc.value());
    }

    @Test
    public void test_decrement() {
        MutableChar mc = MutableChar.of('B');
        mc.decrement();
        assertEquals('A', mc.value());
    }

    @Test
    public void test_decrement_multiple() {
        MutableChar mc = MutableChar.of('D');
        mc.decrement();
        mc.decrement();
        mc.decrement();
        assertEquals('A', mc.value());
    }

    @Test
    public void test_decrement_underflow() {
        MutableChar mc = MutableChar.of('\0');
        mc.decrement();
        assertEquals('\uffff', mc.value());
    }

    @Test
    public void test_decrement_fromOne() {
        MutableChar mc = MutableChar.of('\u0001');
        mc.decrement();
        assertEquals('\0', mc.value());
    }

    @Test
    public void test_getAndIncrement() {
        MutableChar mc = MutableChar.of('A');
        char old = mc.getAndIncrement();
        assertEquals('A', old);
        assertEquals('B', mc.value());
    }

    @Test
    public void test_getAndIncrement_multiple() {
        MutableChar mc = MutableChar.of('A');
        char val1 = mc.getAndIncrement();
        char val2 = mc.getAndIncrement();
        char val3 = mc.getAndIncrement();
        assertEquals('A', val1);
        assertEquals('B', val2);
        assertEquals('C', val3);
        assertEquals('D', mc.value());
    }

    @Test
    public void test_getAndIncrement_overflow() {
        MutableChar mc = MutableChar.of('\uffff');
        char old = mc.getAndIncrement();
        assertEquals('\uffff', old);
        assertEquals('\0', mc.value());
    }

    @Test
    public void test_getAndDecrement() {
        MutableChar mc = MutableChar.of('B');
        char old = mc.getAndDecrement();
        assertEquals('B', old);
        assertEquals('A', mc.value());
    }

    @Test
    public void test_getAndDecrement_multiple() {
        MutableChar mc = MutableChar.of('D');
        char val1 = mc.getAndDecrement();
        char val2 = mc.getAndDecrement();
        char val3 = mc.getAndDecrement();
        assertEquals('D', val1);
        assertEquals('C', val2);
        assertEquals('B', val3);
        assertEquals('A', mc.value());
    }

    @Test
    public void test_getAndDecrement_underflow() {
        MutableChar mc = MutableChar.of('\0');
        char old = mc.getAndDecrement();
        assertEquals('\0', old);
        assertEquals('\uffff', mc.value());
    }

    @Test
    public void test_incrementAndGet() {
        MutableChar mc = MutableChar.of('A');
        char newVal = mc.incrementAndGet();
        assertEquals('B', newVal);
        assertEquals('B', mc.value());
    }

    @Test
    public void test_incrementAndGet_multiple() {
        MutableChar mc = MutableChar.of('A');
        char val1 = mc.incrementAndGet();
        char val2 = mc.incrementAndGet();
        char val3 = mc.incrementAndGet();
        assertEquals('B', val1);
        assertEquals('C', val2);
        assertEquals('D', val3);
        assertEquals('D', mc.value());
    }

    @Test
    public void test_incrementAndGet_overflow() {
        MutableChar mc = MutableChar.of('\uffff');
        char newVal = mc.incrementAndGet();
        assertEquals('\0', newVal);
        assertEquals('\0', mc.value());
    }

    @Test
    public void test_decrementAndGet() {
        MutableChar mc = MutableChar.of('B');
        char newVal = mc.decrementAndGet();
        assertEquals('A', newVal);
        assertEquals('A', mc.value());
    }

    @Test
    public void test_decrementAndGet_multiple() {
        MutableChar mc = MutableChar.of('D');
        char val1 = mc.decrementAndGet();
        char val2 = mc.decrementAndGet();
        char val3 = mc.decrementAndGet();
        assertEquals('C', val1);
        assertEquals('B', val2);
        assertEquals('A', val3);
        assertEquals('A', mc.value());
    }

    @Test
    public void test_decrementAndGet_underflow() {
        MutableChar mc = MutableChar.of('\0');
        char newVal = mc.decrementAndGet();
        assertEquals('\uffff', newVal);
        assertEquals('\uffff', mc.value());
    }

    @Test
    public void test_compareTo_equal() {
        MutableChar mc1 = MutableChar.of('A');
        MutableChar mc2 = MutableChar.of('A');
        assertEquals(0, mc1.compareTo(mc2));
    }

    @Test
    public void test_compareTo_lessThan() {
        MutableChar mc1 = MutableChar.of('A');
        MutableChar mc2 = MutableChar.of('B');
        assertTrue(mc1.compareTo(mc2) < 0);
    }

    @Test
    public void test_compareTo_greaterThan() {
        MutableChar mc1 = MutableChar.of('B');
        MutableChar mc2 = MutableChar.of('A');
        assertTrue(mc1.compareTo(mc2) > 0);
    }

    @Test
    public void test_compareTo_nullChar() {
        MutableChar mc1 = MutableChar.of('\0');
        MutableChar mc2 = MutableChar.of('\0');
        assertEquals(0, mc1.compareTo(mc2));
    }

    @Test
    public void test_compareTo_maxChar() {
        MutableChar mc1 = MutableChar.of('\ufffe');
        MutableChar mc2 = MutableChar.of('\uffff');
        assertTrue(mc1.compareTo(mc2) < 0);
    }

    @Test
    public void test_compareTo_nullPointer() {
        MutableChar mc = MutableChar.of('A');
        assertThrows(NullPointerException.class, () -> mc.compareTo(null));
    }

    @Test
    public void test_equals_same() {
        MutableChar mc = MutableChar.of('A');
        assertTrue(mc.equals(mc));
    }

    @Test
    public void test_equals_equalValue() {
        MutableChar mc1 = MutableChar.of('A');
        MutableChar mc2 = MutableChar.of('A');
        assertTrue(mc1.equals(mc2));
        assertTrue(mc2.equals(mc1));
    }

    @Test
    public void test_equals_differentValue() {
        MutableChar mc1 = MutableChar.of('A');
        MutableChar mc2 = MutableChar.of('B');
        assertFalse(mc1.equals(mc2));
        assertFalse(mc2.equals(mc1));
    }

    @Test
    public void test_equals_null() {
        MutableChar mc = MutableChar.of('A');
        assertFalse(mc.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        MutableChar mc = MutableChar.of('A');
        assertFalse(mc.equals(Character.valueOf('A')));
        assertFalse(mc.equals("A"));
        assertFalse(mc.equals(65));
    }

    @Test
    public void test_equals_nullChar() {
        MutableChar mc1 = MutableChar.of('\0');
        MutableChar mc2 = MutableChar.of('\0');
        assertTrue(mc1.equals(mc2));
    }

    @Test
    public void test_equals_maxChar() {
        MutableChar mc1 = MutableChar.of('\uffff');
        MutableChar mc2 = MutableChar.of('\uffff');
        assertTrue(mc1.equals(mc2));
    }

    @Test
    public void test_hashCode_consistency() {
        MutableChar mc = MutableChar.of('A');
        int hash1 = mc.hashCode();
        int hash2 = mc.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_hashCode_equalObjects() {
        MutableChar mc1 = MutableChar.of('A');
        MutableChar mc2 = MutableChar.of('A');
        assertEquals(mc1.hashCode(), mc2.hashCode());
    }

    @Test
    public void test_hashCode_value() {
        MutableChar mc = MutableChar.of('A');
        assertEquals(65, mc.hashCode());
    }

    @Test
    public void test_hashCode_nullChar() {
        MutableChar mc = MutableChar.of('\0');
        assertEquals(0, mc.hashCode());
    }

    @Test
    public void test_hashCode_maxChar() {
        MutableChar mc = MutableChar.of('\uffff');
        assertEquals(65535, mc.hashCode());
    }

    @Test
    public void test_toString() {
        MutableChar mc = MutableChar.of('A');
        assertEquals("A", mc.toString());
    }

    @Test
    public void test_toString_nullChar() {
        MutableChar mc = MutableChar.of('\0');
        assertEquals("\0", mc.toString());
    }

    @Test
    public void test_toString_specialChar() {
        MutableChar mc = MutableChar.of('\n');
        assertEquals("\n", mc.toString());

        mc = MutableChar.of('\t');
        assertEquals("\t", mc.toString());
    }

    @Test
    public void test_toString_digit() {
        MutableChar mc = MutableChar.of('5');
        assertEquals("5", mc.toString());
    }

    @Test
    public void test_toString_symbol() {
        MutableChar mc = MutableChar.of('@');
        assertEquals("@", mc.toString());
    }

    @Test
    public void test_getAndSet_chain() {
        MutableChar mc = MutableChar.of('A');
        char old1 = mc.getAndSet('B');
        char old2 = mc.getAndSet('C');
        char old3 = mc.getAndSet('D');
        assertEquals('A', old1);
        assertEquals('B', old2);
        assertEquals('C', old3);
        assertEquals('D', mc.value());
    }

    @Test
    public void test_setAndGet_chain() {
        MutableChar mc = MutableChar.of('A');
        char new1 = mc.setAndGet('B');
        char new2 = mc.setAndGet('C');
        char new3 = mc.setAndGet('D');
        assertEquals('B', new1);
        assertEquals('C', new2);
        assertEquals('D', new3);
        assertEquals('D', mc.value());
    }

    @Test
    public void test_incrementAndDecrement() {
        MutableChar mc = MutableChar.of('M');
        mc.increment();
        mc.increment();
        mc.decrement();
        assertEquals('N', mc.value());
    }

    @Test
    public void test_unicodeChar() {
        MutableChar mc = MutableChar.of('\u4E2D');
        assertEquals('\u4E2D', mc.value());
        mc.increment();
        assertEquals('\u4E2E', mc.value());
    }

    @Test
    public void test_comparison_sameInstance() {
        MutableChar mc = MutableChar.of('X');
        assertEquals(0, mc.compareTo(mc));
    }
}
