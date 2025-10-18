package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PrimitiveByteType100Test extends TestBase {

    private Type<Byte> type;

    @BeforeEach
    public void setUp() {
        type = createType(byte.class);
    }

    @Test
    public void testClazz() {
        assertEquals(byte.class, type.clazz());
    }

    @Test
    public void testIsPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void testDefaultValue() {
        Byte defaultValue = type.defaultValue();
        assertNotNull(defaultValue);
        assertEquals((byte) 0, defaultValue);
    }
}
