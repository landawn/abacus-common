package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PrimitiveIntType100Test extends TestBase {

    private Type<Integer> type;

    @BeforeEach
    public void setUp() {
        type = createType(int.class);
    }

    @Test
    public void testClazz() {
        assertEquals(int.class, type.clazz());
    }

    @Test
    public void testIsPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void testDefaultValue() {
        Integer defaultValue = type.defaultValue();
        assertNotNull(defaultValue);
        assertEquals(0, defaultValue);
    }
}
