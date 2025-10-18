package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Mutable2025Test extends TestBase {

    @Test
    public void testInterfaceExists() {
        assertNotNull(Mutable.class);
    }

    @Test
    public void testIsInterface() {
        assertTrue(Mutable.class.isInterface());
    }

    @Test
    public void testMutableIntImplementsInterface() {
        MutableInt num = MutableInt.of(0);
        assertTrue(num instanceof Mutable);
    }

    @Test
    public void testMutableLongImplementsInterface() {
        MutableLong num = MutableLong.of(0L);
        assertTrue(num instanceof Mutable);
    }

    @Test
    public void testMutableDoubleImplementsInterface() {
        MutableDouble num = MutableDouble.of(0.0);
        assertTrue(num instanceof Mutable);
    }

    @Test
    public void testInterfaceHasNoMethods() {
        assertEquals(0, Mutable.class.getDeclaredMethods().length);
    }
}
