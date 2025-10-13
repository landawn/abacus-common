package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PrimitiveDoubleType100Test extends TestBase {

    private Type<Double> type;

    @BeforeEach
    public void setUp() {
        type = createType(double.class);
    }

    @Test
    public void testClazz() {
        assertEquals(double.class, type.clazz());
    }

    @Test
    public void testIsPrimitiveType() {
        assertTrue(type.isPrimitiveType());
    }

    @Test
    public void testDefaultValue() {
        Double defaultValue = type.defaultValue();
        assertNotNull(defaultValue);
        assertEquals(0.0, defaultValue);
    }
}
