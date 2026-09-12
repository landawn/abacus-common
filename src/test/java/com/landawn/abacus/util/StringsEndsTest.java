package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StringsEndsTest extends StringsTestSupport {

    @Test
    public void testEndsWith() {
        assertTrue(Strings.endsWith("hello", "llo"));
        assertTrue(Strings.endsWith("test", "test"));
        assertTrue(Strings.endsWith("abcdef", "def"));
        assertTrue(Strings.endsWith("abc", "bc"));
        assertTrue(Strings.endsWith("abc", ""));
        assertTrue(Strings.endsWith("", ""));
        assertTrue(Strings.endsWith("Hello", "llo"));

        assertFalse(Strings.endsWith("ab", "abc"));
        assertFalse(Strings.endsWith("", "a"));
        assertFalse(Strings.endsWith("abcdef", "xyz"));
        assertFalse(Strings.endsWith("abc", "ab"));
        assertFalse(Strings.endsWith("Hello", "LLO"));
        assertFalse(Strings.endsWith("TEST", "test"));
        assertFalse(Strings.endsWith(null, "suffix"));
        assertFalse(Strings.endsWith(null, null));
        assertFalse(Strings.endsWith("test", null));
    }

    @Test
    public void testEndsWithIgnoreCase() {
        assertTrue(Strings.endsWithIgnoreCase("AbCdEf", "def"));
        assertTrue(Strings.endsWithIgnoreCase("Hello", "LLO"));
        assertTrue(Strings.endsWithIgnoreCase("TEST", "test"));
        assertTrue(Strings.endsWithIgnoreCase("MiXeD", "XED"));
        assertTrue(Strings.endsWithIgnoreCase("abc", "ABC"));
        assertTrue(Strings.endsWithIgnoreCase("ABC", "bc"));
        assertTrue(Strings.endsWithIgnoreCase("abc", "BC"));

        assertFalse(Strings.endsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.endsWithIgnoreCase("abc", "ab"));
        assertFalse(Strings.endsWithIgnoreCase(null, "suffix"));
        assertFalse(Strings.endsWithIgnoreCase("test", null));
        assertFalse(Strings.endsWithIgnoreCase(null, null));
        assertFalse(Strings.endsWithIgnoreCase(null, "def"));
    }

    @Test
    public void testEndsWithAny() {
        assertTrue(Strings.endsWithAny("hello", "llo", "test", "world"));
        assertTrue(Strings.endsWithAny("test", "no", "st", "maybe"));
        assertTrue(Strings.endsWithAny("abc", "xyz", "bc", "def"));
        assertTrue(Strings.endsWithAny("abcdef", "def", "xyz"));
        assertTrue(Strings.endsWithAny("abc", "xy", "bc", "cd"));

        assertFalse(Strings.endsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAny("hello", "world", "test", "abc"));
        assertFalse(Strings.endsWithAny("xyz", "a", "b", "c"));
        assertFalse(Strings.endsWithAny("abc", "xy", "cd"));
        assertFalse(Strings.endsWithAny(null, "def"));
        assertFalse(Strings.endsWithAny(null, "a", "b"));
        assertFalse(Strings.endsWithAny(null, "bc"));
    }

    @Test
    public void testEndsWithAnyIgnoreCase() {
        assertTrue(Strings.endsWithAnyIgnoreCase("AbCdEf", "def", "xyz"));
        assertTrue(Strings.endsWithAnyIgnoreCase("Hello", "LLO", "test"));
        assertTrue(Strings.endsWithAnyIgnoreCase("TEST", "xyz", "test"));
        assertTrue(Strings.endsWithAnyIgnoreCase("MiXeD", "abc", "XED"));
        assertTrue(Strings.endsWithAnyIgnoreCase("ABC", "xy", "bc"));

        assertFalse(Strings.endsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAnyIgnoreCase("abc", "xy", "cd"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "def"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "a", "b"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "bc"));
    }
}
