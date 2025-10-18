package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Immutable2025Test extends TestBase {

    @Test
    public void testInterfaceExists() {
        assertNotNull(Immutable.class);
    }

    @Test
    public void testIsInterface() {
        assertTrue(Immutable.class.isInterface());
    }

    @Test
    public void testImmutableListImplementsInterface() {
        ImmutableList<String> list = ImmutableList.empty();
        assertTrue(list instanceof Immutable);
    }

    @Test
    public void testImmutableSetImplementsInterface() {
        ImmutableSet<String> set = ImmutableSet.empty();
        assertTrue(set instanceof Immutable);
    }

    @Test
    public void testInterfaceHasNoMethods() {
        assertEquals(0, Immutable.class.getDeclaredMethods().length);
    }
}
