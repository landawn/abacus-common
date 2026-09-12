package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class SeidRenderingTest {
    @Test
    void renderingAndCopiesReflectMutableValuesAtEveryMapSize() {
        for (int size = 1; size <= 4; size++) {
            final StringBuilder value = new StringBuilder("old");
            final Seid original = Seid.of("User.id", value);
            for (int i = 1; i < size; i++) {
                original.set("key" + i, i == 1 ? null : "\uD83D\uDE00");
            }
            assertTrue(original.toString().contains("id=old"));
            final Seid copy = original.copy();
            assertEquals(original.toString(), copy.toString());
            value.replace(0, value.length(), "new\uD83D\uDE00");
            assertTrue(original.toString().contains("id=new\uD83D\uDE00"));
            assertEquals(original.toString(), copy.toString());
            assertEquals(original.toString(), original.copy().toString());
            value.setLength(0);
            assertFalse(original.toString().contains("old"));
            assertEquals(original.toString(), copy.toString());
        }
    }

    @Test
    void normalMutationAndEmptyRenderingRemainConsistent() {
        final Seid id = Seid.of("User.id", 1);
        assertEquals("User: {id=1}", id.toString());
        id.set("id", 2);
        assertEquals("User: {id=2}", id.toString());
        id.clear();
        assertEquals("User: {}", id.toString());
        assertEquals(id.toString(), id.copy().toString());
    }
}
