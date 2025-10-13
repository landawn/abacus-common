package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ShortType100Test extends TestBase {

    private ShortType shortType;

    @BeforeEach
    public void setUp() {
        shortType = (ShortType) createType("Short");
    }

    @Test
    public void testClazz() {
        assertEquals(Short.class, shortType.clazz());
    }

    @Test
    public void testIsPrimitiveWrapper() {
        assertTrue(shortType.isPrimitiveWrapper());
    }
}
