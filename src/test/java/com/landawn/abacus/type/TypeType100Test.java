package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class TypeType100Test extends TestBase {

    private TypeType typeType;

    @BeforeEach
    public void setUp() {
        typeType = (TypeType) createType("Type");
    }

    @Test
    public void testClazz() {
        Class<?> clazz = typeType.clazz();
        assertNotNull(clazz);
        assertEquals(Type.class, clazz);
    }

    @Test
    public void testIsImmutable() {
        assertTrue(typeType.isImmutable());
    }

    @Test
    public void testStringOf() {
        Type<?> testType = createType("String");
        String result = typeType.stringOf(testType);
        assertNotNull(result);
    }

    @Test
    public void testStringOfNull() {
        String result = typeType.stringOf(null);
        assertNull(result);
    }

    @Test
    public void testValueOf() {
        Type<?> result = typeType.valueOf("String");
        assertNotNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        Type<?> result = typeType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfNull() {
        Type<?> result = typeType.valueOf(null);
        assertNull(result);
    }
}
