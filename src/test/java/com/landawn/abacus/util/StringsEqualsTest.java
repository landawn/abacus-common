package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StringsEqualsTest extends StringsTestSupport {

    @Test
    public void testEquals() {
        assertTrue(Strings.equals("", ""));
        assertTrue(Strings.equals("test", "test"));
        assertTrue(Strings.equals("Hello World", "Hello World"));
        assertTrue(Strings.equals("abc", "abc"));
        assertTrue(Strings.equals(null, null));

        assertFalse(Strings.equals("test", "TEST"));
        assertFalse(Strings.equals("hello", "world"));
        assertFalse(Strings.equals("abc", "def"));
        assertFalse(Strings.equals("abc", "ABC"));
        assertFalse(Strings.equals("abc", "xyz"));
        assertFalse(Strings.equals("Hello", "hello"));
        assertFalse(Strings.equals("TEST", "test"));
        assertFalse(Strings.equals(null, "test"));
        assertFalse(Strings.equals("test", null));
        assertFalse(Strings.equals("abc", null));
        assertFalse(Strings.equals(null, "abc"));
    }

    @Test
    public void testEqualsIgnoreCase() {
        assertTrue(Strings.equalsIgnoreCase("abc", "ABC"));
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertTrue(Strings.equalsIgnoreCase("test", "TEST"));
        assertTrue(Strings.equalsIgnoreCase("Hello", "hello"));
        assertTrue(Strings.equalsIgnoreCase("MiXeD", "mixed"));
        assertTrue(Strings.equalsIgnoreCase("", ""));

        assertFalse(Strings.equalsIgnoreCase("abc", "xyz"));
        assertFalse(Strings.equalsIgnoreCase("abc", null));
        assertFalse(Strings.equalsIgnoreCase(null, "test"));
        assertFalse(Strings.equalsIgnoreCase("test", null));
        assertFalse(Strings.equalsIgnoreCase("hello", "world"));
        assertFalse(Strings.equalsIgnoreCase("abc", "def"));
        assertFalse(Strings.equalsIgnoreCase(null, "abc"));
    }

    @Test
    public void testEqualsAny() {
        assertTrue(Strings.equalsAny("abc", "abc", "xyz"));
        assertTrue(Strings.equalsAny("test", "hello", "test", "world"));
        assertTrue(Strings.equalsAny("abc", "abc"));
        assertTrue(Strings.equalsAny(null, "hello", null, "world"));
        assertTrue(Strings.equalsAny("test", "TEST", "test"));
        assertTrue(Strings.equalsAny("abc", "xyz", "abc", "def"));

        assertFalse(Strings.equalsAny("abc", "xyz", "123"));
        assertFalse(Strings.equalsAny(null, "abc", "xyz"));
        assertFalse(Strings.equalsAny("test"));
        assertFalse(Strings.equalsAny("test", (String[]) null));
        assertFalse(Strings.equalsAny("test", "hello", "world", "abc"));
        assertFalse(Strings.equalsAny("xyz", "a", "b", "c"));
        assertFalse(Strings.equalsAny("test", "TEST", "Hello"));
        assertFalse(Strings.equalsAny("abc", "xyz", "def"));
        assertFalse(Strings.equalsAny(null, "abc"));
        assertFalse(Strings.equalsAny("abc"));
    }

    @Test
    public void testEqualsAnyIgnoreCase() {
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "ABC", "xyz"));
        assertTrue(Strings.equalsAnyIgnoreCase("test", "HELLO", "TEST", "world"));
        assertTrue(Strings.equalsAnyIgnoreCase("ABC", "xyz", "abc"));
        assertTrue(Strings.equalsAnyIgnoreCase("Hello", "HELLO", "world"));
        assertTrue(Strings.equalsAnyIgnoreCase("test", "TEST"));
        assertTrue(Strings.equalsAnyIgnoreCase("test", "hello", "TEST"));
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "XYZ", "ABC", "DEF"));
        assertTrue(Strings.equalsAnyIgnoreCase("hello", "HELLO", "world", "foo"));
        assertTrue(Strings.equalsAnyIgnoreCase(null, "a", null, "b"));
        assertTrue(Strings.equalsAnyIgnoreCase("hello", "HELLO", "WORLD"));
        assertTrue(Strings.equalsAnyIgnoreCase("hello", "HELLO"));

        assertFalse(Strings.equalsAnyIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "abc"));
        assertFalse(Strings.equalsAnyIgnoreCase("test"));
        assertFalse(Strings.equalsAnyIgnoreCase("test", (String[]) null));
        assertFalse(Strings.equalsAnyIgnoreCase("test", "HELLO", "WORLD", "ABC"));
        assertFalse(Strings.equalsAnyIgnoreCase("xyz", "A", "B", "C"));
        assertFalse(Strings.equalsAnyIgnoreCase("abc", "xyz", "def"));
        assertFalse(Strings.equalsAnyIgnoreCase("hello", "world", "foo", "bar"));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "a", "b", "c"));
        assertFalse(Strings.equalsAnyIgnoreCase("hello", "WORLD", "FOO"));
        assertFalse(Strings.equalsAnyIgnoreCase("hello"));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, Strings.compareIgnoreCase(null, null));
        assertEquals(0, Strings.compareIgnoreCase("", ""));
        assertEquals(0, Strings.compareIgnoreCase("test", "TEST"));
        assertEquals(0, Strings.compareIgnoreCase("Hello", "hello"));
        assertEquals(0, Strings.compareIgnoreCase("MiXeD", "mixed"));
        assertEquals(0, Strings.compareIgnoreCase("hello", "HELLO"));
        assertEquals(0, Strings.compareIgnoreCase("abc", "ABC"));

        assertTrue(Strings.compareIgnoreCase(null, "test") < 0);
        assertTrue(Strings.compareIgnoreCase("test", null) > 0);
        assertTrue(Strings.compareIgnoreCase(null, "abc") < 0);
        assertTrue(Strings.compareIgnoreCase("abc", null) > 0);
        assertTrue(Strings.compareIgnoreCase(null, "a") < 0);
        assertTrue(Strings.compareIgnoreCase("a", null) > 0);

        assertTrue(Strings.compareIgnoreCase("apple", "BANANA") < 0);
        assertTrue(Strings.compareIgnoreCase("a", "B") < 0);
        assertTrue(Strings.compareIgnoreCase("BANANA", "apple") > 0);
        assertTrue(Strings.compareIgnoreCase("B", "a") > 0);
        assertTrue(Strings.compareIgnoreCase("a", "b") < 0);
        assertTrue(Strings.compareIgnoreCase("b", "a") > 0);
        assertTrue(Strings.compareIgnoreCase("abc", "def") < 0);
        assertTrue(Strings.compareIgnoreCase("def", "abc") > 0);

        assertTrue(Strings.compareIgnoreCase("", "test") < 0);
        assertTrue(Strings.compareIgnoreCase("test", "") > 0);
        assertTrue(Strings.compareIgnoreCase("abc", "ABCD") < 0);
        assertTrue(Strings.compareIgnoreCase("ABCD", "abc") > 0);
    }
}
