package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fraction;

public class FractionType100Test extends TestBase {

    private FractionType fractionType;

    @BeforeEach
    public void setUp() {
        fractionType = (FractionType) createType(Fraction.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Fraction.class, fractionType.clazz());
    }

    @Test
    public void testIsNumber() {
        assertTrue(fractionType.isNumber());
    }

    @Test
    public void testIsImmutable() {
        assertTrue(fractionType.isImmutable());
    }

    @Test
    public void testIsComparable() {
        assertTrue(fractionType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(fractionType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(fractionType.stringOf(null));

        // Test with Fraction would require actual Fraction instance
        // The actual test depends on Fraction.toString() implementation
    }

    @Test
    public void testValueOf() {
        // Test with null and empty string
        assertNull(fractionType.valueOf(null));
        assertNull(fractionType.valueOf(""));

        // Test with valid string would require mocking Fraction.of()
        // The actual test depends on Fraction.of() implementation
    }
}
