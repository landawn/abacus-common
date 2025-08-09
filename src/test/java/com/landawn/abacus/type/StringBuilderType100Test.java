package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class StringBuilderType100Test extends TestBase {

    private StringBuilderType stringBuilderType;

    @BeforeEach
    public void setUp() {
        stringBuilderType = (StringBuilderType) createType("StringBuilder");
    }

    @Test
    public void testClazz() {
        assertEquals(StringBuilder.class, stringBuilderType.clazz());
    }

    @Test
    public void testStringOf() {
        // Test with StringBuilder
        StringBuilder sb = new StringBuilder("test content");
        assertEquals("test content", stringBuilderType.stringOf(sb));

        // Test with null
        assertNull(stringBuilderType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        // Test with string
        StringBuilder result = stringBuilderType.valueOf("test content");
        assertNotNull(result);
        assertEquals("test content", result.toString());

        // Test with null
        assertNull(stringBuilderType.valueOf(null));
    }

    @Test
    public void testIsImmutable() {
        assertFalse(stringBuilderType.isImmutable());
    }
}
