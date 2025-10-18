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
        StringBuffer sb = new StringBuffer("test content");
        assertEquals("test content", stringBufferType.stringOf(sb));

        assertNull(stringBufferType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        StringBuffer result = stringBufferType.valueOf("test content");
        assertNotNull(result);
        assertEquals("test content", result.toString());

        assertNull(stringBufferType.valueOf(null));
    }

    @Test
    public void testIsImmutable() {
        assertFalse(stringBufferType.isImmutable());
    }
}
