package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IOCase100Test extends TestBase {

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
        // SYSTEM depends on OS
    }

    @Test
    public void testCheckCompareTo() {
        // Case sensitive
        Assertions.assertTrue(IOCase.SENSITIVE.checkCompareTo("abc", "def") < 0);
        Assertions.assertTrue(IOCase.SENSITIVE.checkCompareTo("def", "abc") > 0);
        Assertions.assertEquals(0, IOCase.SENSITIVE.checkCompareTo("abc", "abc"));
        Assertions.assertTrue(IOCase.SENSITIVE.checkCompareTo("ABC", "abc") < 0);

        // Case insensitive
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
        // Case sensitive
        Assertions.assertTrue(IOCase.SENSITIVE.checkEquals("abc", "abc"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEquals("abc", "ABC"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEquals("abc", "def"));

        // Case insensitive
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
        // Case sensitive
        Assertions.assertTrue(IOCase.SENSITIVE.checkStartsWith("abcdef", "abc"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkStartsWith("abcdef", "ABC"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkStartsWith("abcdef", "def"));

        // Case insensitive
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
        // Case sensitive
        Assertions.assertTrue(IOCase.SENSITIVE.checkEndsWith("abcdef", "def"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEndsWith("abcdef", "DEF"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkEndsWith("abcdef", "abc"));

        // Case insensitive
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
        // Case sensitive
        Assertions.assertEquals(2, IOCase.SENSITIVE.checkIndexOf("abcdef", 0, "cd"));
        Assertions.assertEquals(-1, IOCase.SENSITIVE.checkIndexOf("abcdef", 0, "CD"));
        Assertions.assertEquals(3, IOCase.SENSITIVE.checkIndexOf("abcdef", 3, "def"));

        // Case insensitive
        Assertions.assertEquals(2, IOCase.INSENSITIVE.checkIndexOf("abcdef", 0, "CD"));
        Assertions.assertEquals(2, IOCase.INSENSITIVE.checkIndexOf("ABCDEF", 0, "cd"));
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
    public void testCheckRegionMatches() {
        // Case sensitive
        Assertions.assertTrue(IOCase.SENSITIVE.checkRegionMatches("abcdef", 2, "cd"));
        Assertions.assertFalse(IOCase.SENSITIVE.checkRegionMatches("abcdef", 2, "CD"));

        // Case insensitive
        Assertions.assertTrue(IOCase.INSENSITIVE.checkRegionMatches("abcdef", 2, "CD"));
        Assertions.assertTrue(IOCase.INSENSITIVE.checkRegionMatches("ABCDEF", 2, "cd"));
    }

    @Test
    public void testCheckRegionMatchesNullStrings() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkRegionMatches(null, 0, "test");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IOCase.SENSITIVE.checkRegionMatches("test", 0, null);
        });
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("Sensitive", IOCase.SENSITIVE.toString());
        Assertions.assertEquals("Insensitive", IOCase.INSENSITIVE.toString());
        Assertions.assertEquals("System", IOCase.SYSTEM.toString());
    }
}
