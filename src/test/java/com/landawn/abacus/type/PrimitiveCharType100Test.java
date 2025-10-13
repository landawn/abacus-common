package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PrimitiveCharType100Test extends TestBase {

    private PrimitiveCharType type;
    private Type<Character> createdType;

    @BeforeEach
    public void setUp() {
        createdType = createType(char.class);
        type = (PrimitiveCharType) createdType;
    }

    @Test
    public void testClazz() {
        assertEquals(char.class, type.clazz());
    }

    @Test
    public void testIsPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void testDefaultValue() {
        Character defaultValue = type.defaultValue();
        assertNotNull(defaultValue);
        assertEquals((char) 0, defaultValue);
    }
}
