package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IOCase2025Test extends TestBase {
    @Test
    public void testCheckIndexOf_insensitive() {
        IOCase ioCase = IOCase.INSENSITIVE;
        assertEquals(7, ioCase.checkIndexOf("Find a file here", 0, "FILE"));
        assertEquals(7, ioCase.checkIndexOf("Find a FILE here", 0, "file"));
        assertEquals(0, ioCase.checkIndexOf("Hello", 0, "hello"));
    }

    @Test
    public void testCheckIndexOf_withNull() {
        assertThrows(IllegalArgumentException.class, () -> IOCase.SENSITIVE.checkIndexOf(null, 0, "test"));
        assertThrows(IllegalArgumentException.class, () -> IOCase.SENSITIVE.checkIndexOf("test", 0, null));
    }

    @Test
    public void testCheckRegionMatches_sensitive() {
        IOCase ioCase = IOCase.SENSITIVE;
        assertTrue(ioCase.checkRegionMatches("File.txt", 0, "File"));
        assertFalse(ioCase.checkRegionMatches("File.txt", 0, "file"));
        assertTrue(ioCase.checkRegionMatches("Hello World", 6, "World"));
        assertFalse(ioCase.checkRegionMatches("Hello World", 6, "world"));
    }

    @Test
    public void testCheckRegionMatches_insensitive() {
        IOCase ioCase = IOCase.INSENSITIVE;
        assertTrue(ioCase.checkRegionMatches("File.txt", 0, "File"));
        assertTrue(ioCase.checkRegionMatches("File.txt", 0, "file"));
        assertTrue(ioCase.checkRegionMatches("Hello World", 6, "world"));
    }

    @Test
    public void testCheckRegionMatches_withNull() {
        assertThrows(IllegalArgumentException.class, () -> IOCase.SENSITIVE.checkRegionMatches(null, 0, "test"));
        assertThrows(IllegalArgumentException.class, () -> IOCase.SENSITIVE.checkRegionMatches("test", 0, null));
    }

    @Test
    public void testToString() {
        assertEquals("Sensitive", IOCase.SENSITIVE.toString());
        assertEquals("Insensitive", IOCase.INSENSITIVE.toString());
        assertEquals("System", IOCase.SYSTEM.toString());
    }

    @Test
    public void testValues() {
        IOCase[] values = IOCase.values();
        assertEquals(3, values.length);
        assertEquals(IOCase.SENSITIVE, values[0]);
        assertEquals(IOCase.INSENSITIVE, values[1]);
        assertEquals(IOCase.SYSTEM, values[2]);
    }

    @Test
    public void testValueOf_withStringName() {
        assertEquals(IOCase.SENSITIVE, IOCase.valueOf("SENSITIVE"));
        assertEquals(IOCase.INSENSITIVE, IOCase.valueOf("INSENSITIVE"));
        assertEquals(IOCase.SYSTEM, IOCase.valueOf("SYSTEM"));
    }
}
