package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Fraction;

public class FractionTypeTest extends TestBase {

    private FractionType fractionType;

    @BeforeEach
    public void setUp() {
        fractionType = (FractionType) createType(Fraction.class.getSimpleName());
    }

    @Test
    public void testClazz() {
        assertEquals(Fraction.class, fractionType.javaType());
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
    public void test_isCsvQuoteRequired() {
        assertFalse(fractionType.isCsvQuoteRequired());
    }

    @Test
    public void testStringOf() {
        assertNull(fractionType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(fractionType.valueOf(null));
        assertNull(fractionType.valueOf(""));

    }
}
