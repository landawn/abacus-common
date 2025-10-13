package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PrimitiveLongType100Test extends TestBase {

    private Type<Long> type;

    @BeforeEach
    public void setUp() {
        type = createType(long.class);
    }

    @Test
    public void testClazz() {
        assertEquals(long.class, type.clazz());
    }

    @Test
    public void testIsPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void testDefaultValue() {
        Long defaultValue = type.defaultValue();
        assertNotNull(defaultValue);
        assertEquals(0L, defaultValue);
    }
}
