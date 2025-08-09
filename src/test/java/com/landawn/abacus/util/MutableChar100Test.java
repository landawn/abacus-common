package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableChar100Test extends TestBase {

    @Test
    public void testOf() {
        MutableChar mutableChar = MutableChar.of('A');
        Assertions.assertEquals('A', mutableChar.value());
    }

    @Test
    public void testValue() {
        MutableChar mutableChar = MutableChar.of('Z');
        Assertions.assertEquals('Z', mutableChar.value());
    }

    @Test
    public void testGetValue() {
        MutableChar mutableChar = MutableChar.of('X');
        Assertions.assertEquals('X', mutableChar.getValue());
    }

    @Test
    public void testSetValue() {
        MutableChar mutableChar = MutableChar.of('A');
        mutableChar.setValue('B');
        Assertions.assertEquals('B', mutableChar.value());
    }

    @Test
    public void testGetAndSet() {
        MutableChar mutableChar = MutableChar.of('C');
        char oldValue = mutableChar.getAndSet('D');
        Assertions.assertEquals('C', oldValue);
        Assertions.assertEquals('D', mutableChar.value());
    }

    @Test
    public void testSetAndGet() {
        MutableChar mutableChar = MutableChar.of('E');
        char newValue = mutableChar.setAndGet('F');
        Assertions.assertEquals('F', newValue);
        Assertions.assertEquals('F', mutableChar.value());
    }

    @Test
    public void testSetIf() throws Exception {
        MutableChar mutableChar = MutableChar.of('A');
        
        // Test when predicate returns true
        boolean updated = mutableChar.setIf('Z', c -> c < 'M');
        Assertions.assertTrue(updated);
        Assertions.assertEquals('Z', mutableChar.value());
        
        // Test when predicate returns false
        updated = mutableChar.setIf('A', c -> c < 'M');
        Assertions.assertFalse(updated);
        Assertions.assertEquals('Z', mutableChar.value());
    }

    @Test
    public void testIncrement() {
        MutableChar mutableChar = MutableChar.of('A');
        mutableChar.increment();
        Assertions.assertEquals('B', mutableChar.value());
        
        mutableChar.setValue('9');
        mutableChar.increment();
        Assertions.assertEquals(':', mutableChar.value());
    }

    @Test
    public void testDecrement() {
        MutableChar mutableChar = MutableChar.of('B');
        mutableChar.decrement();
        Assertions.assertEquals('A', mutableChar.value());
        
        mutableChar.setValue('1');
        mutableChar.decrement();
        Assertions.assertEquals('0', mutableChar.value());
    }

    @Test
    public void testAdd() {
        MutableChar mutableChar = MutableChar.of('A');
        mutableChar.add((char) 3);
        Assertions.assertEquals('D', mutableChar.value());
        
        mutableChar.setValue('0');
        mutableChar.add((char) 5);
        Assertions.assertEquals('5', mutableChar.value());
    }

    @Test
    public void testSubtract() {
        MutableChar mutableChar = MutableChar.of('D');
        mutableChar.subtract((char) 3);
        Assertions.assertEquals('A', mutableChar.value());
        
        mutableChar.setValue('5');
        mutableChar.subtract((char) 2);
        Assertions.assertEquals('3', mutableChar.value());
    }

    @Test
    public void testGetAndIncrement() {
        MutableChar mutableChar = MutableChar.of('X');
        char oldValue = mutableChar.getAndIncrement();
        Assertions.assertEquals('X', oldValue);
        Assertions.assertEquals('Y', mutableChar.value());
    }

    @Test
    public void testGetAndDecrement() {
        MutableChar mutableChar = MutableChar.of('Y');
        char oldValue = mutableChar.getAndDecrement();
        Assertions.assertEquals('Y', oldValue);
        Assertions.assertEquals('X', mutableChar.value());
    }

    @Test
    public void testIncrementAndGet() {
        MutableChar mutableChar = MutableChar.of('X');
        char newValue = mutableChar.incrementAndGet();
        Assertions.assertEquals('Y', newValue);
        Assertions.assertEquals('Y', mutableChar.value());
    }

    @Test
    public void testDecrementAndGet() {
        MutableChar mutableChar = MutableChar.of('Y');
        char newValue = mutableChar.decrementAndGet();
        Assertions.assertEquals('X', newValue);
        Assertions.assertEquals('X', mutableChar.value());
    }

    @Test
    public void testGetAndAdd() {
        MutableChar mutableChar = MutableChar.of('A');
        char oldValue = mutableChar.getAndAdd((char) 5);
        Assertions.assertEquals('A', oldValue);
        Assertions.assertEquals('F', mutableChar.value());
    }

    @Test
    public void testAddAndGet() {
        MutableChar mutableChar = MutableChar.of('A');
        char newValue = mutableChar.addAndGet((char) 5);
        Assertions.assertEquals('F', newValue);
        Assertions.assertEquals('F', mutableChar.value());
    }

    @Test
    public void testCompareTo() {
        MutableChar a = MutableChar.of('A');
        MutableChar b = MutableChar.of('B');
        MutableChar c = MutableChar.of('A');
        
        Assertions.assertTrue(a.compareTo(b) < 0);
        Assertions.assertTrue(b.compareTo(a) > 0);
        Assertions.assertEquals(0, a.compareTo(c));
    }

    @Test
    public void testEquals() {
        MutableChar a = MutableChar.of('X');
        MutableChar b = MutableChar.of('X');
        MutableChar c = MutableChar.of('Y');
        
        Assertions.assertTrue(a.equals(b));
        Assertions.assertFalse(a.equals(c));
        Assertions.assertFalse(a.equals(null));
        Assertions.assertFalse(a.equals("X"));
    }

    @Test
    public void testHashCode() {
        MutableChar a = MutableChar.of('X');
        MutableChar b = MutableChar.of('X');
        MutableChar c = MutableChar.of('Y');
        
        Assertions.assertEquals(a.hashCode(), b.hashCode());
        Assertions.assertNotEquals(a.hashCode(), c.hashCode());
        Assertions.assertEquals('X', a.hashCode());
    }

    @Test
    public void testToString() {
        MutableChar mutableChar = MutableChar.of('A');
        Assertions.assertEquals("A", mutableChar.toString());
        
        mutableChar.setValue('9');
        Assertions.assertEquals("9", mutableChar.toString());
        
        mutableChar.setValue('\n');
        Assertions.assertEquals("\n", mutableChar.toString());
    }

    @Test
    public void testSpecialCharacters() {
        MutableChar mutableChar = MutableChar.of('\0');
        Assertions.assertEquals('\0', mutableChar.value());
        
        mutableChar.setValue('\t');
        Assertions.assertEquals('\t', mutableChar.value());
        
        mutableChar.setValue('\\');
        Assertions.assertEquals('\\', mutableChar.value());
    }

    @Test
    public void testUnicodeCharacters() {
        MutableChar mutableChar = MutableChar.of('\u0041'); // 'A'
        Assertions.assertEquals('A', mutableChar.value());
        
        mutableChar.setValue('\u03B1'); // Greek alpha
        Assertions.assertEquals('\u03B1', mutableChar.value());
    }
}