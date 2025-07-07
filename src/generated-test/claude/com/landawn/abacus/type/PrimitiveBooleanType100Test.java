package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PrimitiveBooleanType100Test extends TestBase {

    private PrimitiveBooleanType primitiveBooleanType;

    @BeforeEach
    public void setUp() {
        primitiveBooleanType = (PrimitiveBooleanType) createType("boolean");
    }

    @Test
    public void testClazz() {
        assertEquals(boolean.class, primitiveBooleanType.clazz());
    }

    @Test
    public void testIsPrimitiveType() {
        assertTrue(primitiveBooleanType.isPrimitiveType());
    }

    @Test
    public void testDefaultValue() {
        assertEquals(Boolean.FALSE, primitiveBooleanType.defaultValue());
    }

    @Test
    public void testName() {
        assertEquals("boolean", primitiveBooleanType.name());
    }

    @Test
    public void testAlternativeName() {
        // Test that the type can be created with alternative name
        PrimitiveBooleanType boolType = (PrimitiveBooleanType) createType("bool");
        assertNotNull(boolType);
    }

    @Test
    public void testIsBoolean() {
        assertTrue(primitiveBooleanType.isBoolean());
    }

    @Test
    public void testIsNumber() {
        assertFalse(primitiveBooleanType.isNumber());
    }

    @Test
    public void testIsString() {
        assertFalse(primitiveBooleanType.isString());
    }

    @Test
    public void testIsObjectType() {
        assertFalse(primitiveBooleanType.isObjectType());
    }

    @Test
    public void testValueOfWithTrue() {
        assertEquals(Boolean.TRUE, primitiveBooleanType.valueOf("true"));
        assertEquals(Boolean.TRUE, primitiveBooleanType.valueOf("TRUE"));
        assertEquals(Boolean.TRUE, primitiveBooleanType.valueOf("True"));
        assertEquals(Boolean.TRUE, primitiveBooleanType.valueOf("1"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("yes"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("YES"));
        assertEquals(Boolean.TRUE, primitiveBooleanType.valueOf("y"));
        assertEquals(Boolean.TRUE, primitiveBooleanType.valueOf("Y"));
    }

    @Test
    public void testValueOfWithFalse() {
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("false"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("FALSE"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("False"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("0"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("no"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("NO"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("N"));
    }

    @Test
    public void testValueOfWithNull() {
        // For primitive boolean type, null should return default value
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf(""));
    }

    @Test
    public void testValueOfWithInvalidString() {
        // Invalid strings should return false
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("invalid"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("maybe"));
        assertEquals(Boolean.FALSE, primitiveBooleanType.valueOf("2"));
    }

    @Test
    public void testStringOfWithTrue() {
        assertEquals("true", primitiveBooleanType.stringOf(Boolean.TRUE));
    }

    @Test
    public void testStringOfWithFalse() {
        assertEquals("false", primitiveBooleanType.stringOf(Boolean.FALSE));
    }

    @Test
    public void testStringOfWithNull() {
        // For primitive types, null typically becomes the default value
        assertEquals(null, primitiveBooleanType.stringOf(null));
    }

    @Test
    public void testIsComparable() {
        assertTrue(primitiveBooleanType.isComparable());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(primitiveBooleanType.isNonQuotableCsvType());
    }

    @Test
    public void testHashCode() {
        assertEquals(Boolean.TRUE.hashCode(), primitiveBooleanType.hashCode(Boolean.TRUE));
        assertEquals(Boolean.FALSE.hashCode(), primitiveBooleanType.hashCode(Boolean.FALSE));
        assertEquals(0, primitiveBooleanType.hashCode(null));
    }

    @Test
    public void testEquals() {
        assertTrue(primitiveBooleanType.equals(Boolean.TRUE, Boolean.TRUE));
        assertTrue(primitiveBooleanType.equals(Boolean.FALSE, Boolean.FALSE));
        assertFalse(primitiveBooleanType.equals(Boolean.TRUE, Boolean.FALSE));
        assertFalse(primitiveBooleanType.equals(Boolean.FALSE, Boolean.TRUE));
        assertTrue(primitiveBooleanType.equals(null, null));
        assertFalse(primitiveBooleanType.equals(Boolean.TRUE, null));
        assertFalse(primitiveBooleanType.equals(null, Boolean.FALSE));
    }

    @Test
    public void testToString() {
        assertEquals("true", primitiveBooleanType.toString(Boolean.TRUE));
        assertEquals("false", primitiveBooleanType.toString(Boolean.FALSE));
        assertEquals("null", primitiveBooleanType.toString(null));
    }
}
