package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AbstractPrimaryType100Test extends TestBase {

    private Type<Integer> primaryType;

    @BeforeEach
    public void setUp() {
        primaryType = createType("Integer");
    }

    @Test
    public void testIsImmutable() {
        assertTrue(primaryType.isImmutable());
    }

    @Test
    public void testIsComparable() {
        assertTrue(primaryType.isComparable());
    }

    @Test
    public void testValueOfObject() {
        assertNull(primaryType.valueOf((Object) null));
        assertEquals(123, primaryType.valueOf(123));
        assertEquals(456, primaryType.valueOf("456"));
        assertEquals(789, primaryType.valueOf(789L));
        assertThrows(NumberFormatException.class, () -> primaryType.valueOf(true));
        assertThrows(NumberFormatException.class, () -> primaryType.valueOf(false));
    }
}
