package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class PrimitiveLongTypeTest extends TestBase {

    private final PrimitiveLongType type = new PrimitiveLongType();

    @Test
    public void test_isComparable() {
        assertTrue(type.isComparable());
    }

    @Test
    public void testClazz() {
        assertEquals(long.class, type.javaType());
    }

    @Test
    public void testDefaultValue() {
        Long defaultValue = type.defaultValue();
        assertNotNull(defaultValue);
        assertEquals(0L, defaultValue);
    }

}
