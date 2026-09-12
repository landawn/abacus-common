package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StringsStartsTest extends StringsTestSupport {

    @Test
    public void testStartsWith() {
        assertTrue(Strings.startsWith("hello", "hel"));
        assertTrue(Strings.startsWith("test", "test"));
        assertTrue(Strings.startsWith("abcdef", "abc"));
        assertTrue(Strings.startsWith("abc", "ab"));
        assertTrue(Strings.startsWith("abc", ""));
        assertTrue(Strings.startsWith("", ""));
        assertTrue(Strings.startsWith("Hello", "Hello"));

        assertFalse(Strings.startsWith("ab", "abc"));
        assertFalse(Strings.startsWith("", "a"));
        assertFalse(Strings.startsWith("abcdef", "xyz"));
        assertFalse(Strings.startsWith("abc", "bc"));
        assertFalse(Strings.startsWith("Hello", "hello"));
        assertFalse(Strings.startsWith("TEST", "test"));
        assertFalse(Strings.startsWith(null, "prefix"));
        assertFalse(Strings.startsWith(null, null));
        assertFalse(Strings.startsWith("test", null));
        assertFalse(Strings.startsWith(null, "abc"));
    }

    @Test
    public void testStartsWithIgnoreCase() {
        assertTrue(Strings.startsWithIgnoreCase("AbCdEf", "abc"));
        assertTrue(Strings.startsWithIgnoreCase("Hello", "hello"));
        assertTrue(Strings.startsWithIgnoreCase("TEST", "test"));
        assertTrue(Strings.startsWithIgnoreCase("MiXeD", "mixed"));
        assertTrue(Strings.startsWithIgnoreCase("ABC", "abc"));
        assertTrue(Strings.startsWithIgnoreCase("hello world", "HELLO"));
        assertTrue(Strings.startsWithIgnoreCase("JavaScript", "java"));
        assertTrue(Strings.startsWithIgnoreCase("ABC", "ab"));
        assertTrue(Strings.startsWithIgnoreCase("abc", "AB"));

        assertFalse(Strings.startsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.startsWithIgnoreCase("hello", "world"));
        assertFalse(Strings.startsWithIgnoreCase("abc", "bc"));
        assertFalse(Strings.startsWithIgnoreCase(null, "prefix"));
        assertFalse(Strings.startsWithIgnoreCase("test", null));
        assertFalse(Strings.startsWithIgnoreCase(null, null));
        assertFalse(Strings.startsWithIgnoreCase(null, "abc"));
        assertFalse(Strings.startsWithIgnoreCase(null, "ab"));
    }

    @Test
    public void testStartsWithAny() {
        assertTrue(Strings.startsWithAny("hello", "hel", "test", "world"));
        assertTrue(Strings.startsWithAny("test", "no", "te", "maybe"));
        assertTrue(Strings.startsWithAny("abc", "xyz", "abc", "def"));
        assertTrue(Strings.startsWithAny("abcdef", "abc", "xyz"));
        assertTrue(Strings.startsWithAny("abc", "xy", "ab", "cd"));

        assertFalse(Strings.startsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAny("hello", "world", "test", "abc"));
        assertFalse(Strings.startsWithAny("xyz", "a", "b", "c"));
        assertFalse(Strings.startsWithAny("abc", "xy", "cd"));
        assertFalse(Strings.startsWithAny(null, "abc"));
        assertFalse(Strings.startsWithAny(null, "a", "b"));
        assertFalse(Strings.startsWithAny(null, "ab"));
        assertFalse(Strings.startsWithAny("test", (String[]) null));
        assertFalse(Strings.startsWithAny("test"));
    }

    @Test
    public void testStartsWithAnyIgnoreCase() {
        assertTrue(Strings.startsWithAnyIgnoreCase("AbCdEf", "abc", "xyz"));
        assertTrue(Strings.startsWithAnyIgnoreCase("Hello", "HELLO", "test"));
        assertTrue(Strings.startsWithAnyIgnoreCase("TEST", "xyz", "test"));
        assertTrue(Strings.startsWithAnyIgnoreCase("MiXeD", "abc", "MIXED"));
        assertTrue(Strings.startsWithAnyIgnoreCase("ABC", "xy", "ab"));

        assertFalse(Strings.startsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAnyIgnoreCase("abc", "xy", "cd"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "abc"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "a", "b"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "ab"));
    }
}
