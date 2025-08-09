package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class StringBufferType100Test extends TestBase {

    private StringBufferType stringBufferType;

    @BeforeEach
    public void setUp() {
        stringBufferType = (StringBufferType) createType("StringBuffer");
    }

    @Test
    public void testClazz() {
        assertEquals(StringBuffer.class, stringBufferType.clazz());
    }

    @Test
    public void testStringOf() {
        // Test with StringBuilder
        StringBuffer sb = new StringBuffer("test content");
        assertEquals("test content", stringBufferType.stringOf(sb));

        // Test with null
        assertNull(stringBufferType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        // Test with string
        StringBuffer result = stringBufferType.valueOf("test content");
        assertNotNull(result);
        assertEquals("test content", result.toString());

        // Test with null
        assertNull(stringBufferType.valueOf(null));
    }

    @Test
    public void testIsImmutable() {
        assertFalse(stringBufferType.isImmutable());
    }
}
