package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class PrimitiveIntTypeTest extends TestBase {

    private final PrimitiveIntType type = new PrimitiveIntType();

    @Test
    public void test_isComparable() {
        assertTrue(type.isComparable());
    }

    @Test
    public void testClazz() {
        assertEquals(int.class, type.javaType());
    }

    @Test
    public void testDefaultValue() {
        Integer defaultValue = type.defaultValue();
        assertNotNull(defaultValue);
        assertEquals(0, defaultValue);
    }

}
