package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
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
        StringBuilder sb = new StringBuilder("test content");
        assertEquals("test content", stringBuilderType.stringOf(sb));

        assertNull(stringBuilderType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        StringBuilder result = stringBuilderType.valueOf("test content");
        assertNotNull(result);
        assertEquals("test content", result.toString());

        assertNull(stringBuilderType.valueOf(null));
    }

    @Test
    public void testIsImmutable() {
        assertFalse(stringBuilderType.isImmutable());
    }
}
