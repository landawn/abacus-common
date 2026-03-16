package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class IOCaseTest extends TestBase {

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
    public void testCheckRegiocountMatchBetweenes_sensitive() {
        IOCase ioCase = IOCase.SENSITIVE;
        assertTrue(ioCase.checkRegionMatches("File.txt", 0, "File"));
        assertFalse(ioCase.checkRegionMatches("File.txt", 0, "file"));
        assertTrue(ioCase.checkRegionMatches("Hello World", 6, "World"));
        assertFalse(ioCase.checkRegionMatches("Hello World", 6, "world"));
    }

    @Test
    public void testCheckRegiocountMatchBetweenes_insensitive() {
        IOCase ioCase = IOCase.INSENSITIVE;
        assertTrue(ioCase.checkRegionMatches("File.txt", 0, "File"));
        assertTrue(ioCase.checkRegionMatches("File.txt", 0, "file"));
        assertTrue(ioCase.checkRegionMatches("Hello World", 6, "world"));
    }

    @Test
    public void testCheckRegiocountMatchBetweenes_withNull() {
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

    @Test
    public void testForName() {
        Assertions.assertEquals(IOCase.SENSITIVE, IOCase.forName("Sensitive"));
        Assertions.assertEquals(IOCase.INSENSITIVE, IOCase.forName("Insensitive"));
        Assertions.assertEquals(IOCase.SYSTEM, IOCase.forName("System"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.forName("Invalid");
        });
    }

    @Test
    public void testGetName() {
        Assertions.assertEquals("Sensitive", IOCase.SENSITIVE.getName());
        Assertions.assertEquals("Insensitive", IOCase.INSENSITIVE.getName());
        Assertions.assertEquals("System", IOCase.SYSTEM.getName());
    }

    @Test
    public void testIsCaseSensitive() {
        Assertions.assertTrue(IOCase.SENSITIVE.isCaseSensitive());
        Assertions.assertFalse(IOCase.INSENSITIVE.isCaseSensitive());
    }

    @Test
    public void testCheckCompareTo() {
        Assertions.assertTrue(IOCase.SENSITIVE.checkCompareTo("abc", "def") < 0);
        Assertions.assertTrue(IOCase.SENSITIVE.checkCompareTo("def", "abc") > 0);
        Assertions.assertEquals(0, IOCase.SENSITIVE.checkCompareTo("abc", "abc"));
        Assertions.assertTrue(IOCase.SENSITIVE.checkCompareTo("ABC", "abc") < 0);

        Assertions.assertEquals(0, IOCase.INSENSITIVE.checkCompareTo("ABC", "abc"));
        Assertions.assertTrue(IOCase.INSENSITIVE.checkCompareTo("abc", "DEF") < 0);
    }

    @Test
    public void testCheckCompareToNullStrings() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkCompareTo(null, "test");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkCompareTo("test", null);
        });
    }

    @Test
    public void testCheckEquals() {
        Assertions.assertTrue(IOCase.SENSITIVE.checkEquals("abc", "abc"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEquals("abc", "ABC"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEquals("abc", "def"));

        Assertions.assertTrue(IOCase.INSENSITIVE.checkEquals("abc", "ABC"));
        Assertions.assertTrue(IOCase.INSENSITIVE.checkEquals("ABC", "abc"));
        Assertions.assertFalse(IOCase.INSENSITIVE.checkEquals("abc", "def"));
    }

    @Test
    public void testCheckEqualsNullStrings() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkEquals(null, "test");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkEquals("test", null);
        });
    }

    @Test
    public void testCheckStartsWith() {
        Assertions.assertTrue(IOCase.SENSITIVE.checkStartsWith("abcdef", "abc"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkStartsWith("abcdef", "ABC"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkStartsWith("abcdef", "def"));

        Assertions.assertTrue(IOCase.INSENSITIVE.checkStartsWith("abcdef", "ABC"));
        Assertions.assertTrue(IOCase.INSENSITIVE.checkStartsWith("ABCDEF", "abc"));
        Assertions.assertFalse(IOCase.INSENSITIVE.checkStartsWith("abcdef", "def"));
    }

    @Test
    public void testCheckStartsWithNullStrings() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkStartsWith(null, "test");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkStartsWith("test", null);
        });
    }

    @Test
    public void testCheckEndsWith() {
        Assertions.assertTrue(IOCase.SENSITIVE.checkEndsWith("abcdef", "def"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEndsWith("abcdef", "DEF"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEndsWith("abcdef", "abc"));

        Assertions.assertTrue(IOCase.INSENSITIVE.checkEndsWith("abcdef", "DEF"));
        Assertions.assertTrue(IOCase.INSENSITIVE.checkEndsWith("ABCDEF", "def"));
        Assertions.assertFalse(IOCase.INSENSITIVE.checkEndsWith("abcdef", "abc"));
    }

    @Test
    public void testCheckEndsWithNullStrings() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkEndsWith(null, "test");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkEndsWith("test", null);
        });
    }

    @Test
    public void testCheckIndexOf() {
        Assertions.assertEquals(2, IOCase.SENSITIVE.checkIndexOf("abcdef", 0, "cd"));
        Assertions.assertEquals(-1, IOCase.SENSITIVE.checkIndexOf("abcdef", 0, "CD"));
        Assertions.assertEquals(3, IOCase.SENSITIVE.checkIndexOf("abcdef", 3, "def"));

        Assertions.assertEquals(2, IOCase.INSENSITIVE.checkIndexOf("abcdef", 0, "CD"));
        Assertions.assertEquals(2, IOCase.INSENSITIVE.checkIndexOf("ABCDEF", 0, "cd"));
    }

    @Test
    public void testCheckIndexOfWithNegativeStartIndex() {
        Assertions.assertEquals(2, IOCase.SENSITIVE.checkIndexOf("abcdef", -100, "cd"));
        Assertions.assertEquals(2, IOCase.INSENSITIVE.checkIndexOf("ABCDEF", -100, "cd"));
    }

    @Test
    public void testCheckIndexOfNullStrings() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkIndexOf(null, 0, "test");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkIndexOf("test", 0, null);
        });
    }

    @Test
    public void testCheckRegiocountMatchBetweenes() {
        Assertions.assertTrue(IOCase.SENSITIVE.checkRegionMatches("abcdef", 2, "cd"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkRegionMatches("abcdef", 2, "CD"));

        Assertions.assertTrue(IOCase.INSENSITIVE.checkRegionMatches("abcdef", 2, "CD"));
        Assertions.assertTrue(IOCase.INSENSITIVE.checkRegionMatches("ABCDEF", 2, "cd"));
    }

    @Test
    public void testCheckRegiocountMatchBetweenesNullStrings() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkRegionMatches(null, 0, "test");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkRegionMatches("test", 0, null);
        });
    }

    @Test
    public void testSystemCaseSensitivity() {
        // SYSTEM case sensitivity depends on the OS
        boolean isSensitive = IOCase.SYSTEM.isCaseSensitive();
        if (isSensitive) {
            // Unix-like: case-sensitive
            Assertions.assertFalse(IOCase.SYSTEM.checkEquals("File.txt", "file.txt"));
            Assertions.assertTrue(IOCase.SYSTEM.checkEquals("File.txt", "File.txt"));
        } else {
            // Windows: case-insensitive
            Assertions.assertTrue(IOCase.SYSTEM.checkEquals("File.txt", "file.txt"));
        }
        Assertions.assertTrue(IOCase.SYSTEM.checkEquals("test", "test"));
    }

    @Test
    public void testCheckRegionMatches_atMiddleOfString() {
        assertTrue(IOCase.SENSITIVE.checkRegionMatches("abcdefgh", 3, "def"));
        assertFalse(IOCase.SENSITIVE.checkRegionMatches("abcdefgh", 3, "DEF"));
        assertTrue(IOCase.INSENSITIVE.checkRegionMatches("abcdefgh", 3, "DEF"));
    }

    @Test
    public void testCheckRegionMatches_emptySearch() {
        assertTrue(IOCase.SENSITIVE.checkRegionMatches("hello", 0, ""));
        assertTrue(IOCase.INSENSITIVE.checkRegionMatches("hello", 0, ""));
        assertTrue(IOCase.SENSITIVE.checkRegionMatches("hello", 3, ""));
    }

    @Test
    public void testCheckRegionMatches_fullString() {
        assertTrue(IOCase.SENSITIVE.checkRegionMatches("hello", 0, "hello"));
        assertFalse(IOCase.SENSITIVE.checkRegionMatches("hello", 0, "HELLO"));
        assertTrue(IOCase.INSENSITIVE.checkRegionMatches("hello", 0, "HELLO"));
    }

    @Test
    public void testCheckRegionMatches_startIndexBeyondLength() {
        assertFalse(IOCase.SENSITIVE.checkRegionMatches("hello", 10, "lo"));
        assertFalse(IOCase.INSENSITIVE.checkRegionMatches("hello", 10, "lo"));
    }

}
