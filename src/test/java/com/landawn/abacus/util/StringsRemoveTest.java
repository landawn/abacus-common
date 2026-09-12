package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class StringsRemoveTest extends StringsTestSupport {
    @Test
    public void test_remove_char() {
        final String str = "🌉";
        final char[] chs = str.toCharArray();

        String newStr = Strings.removeAll(str, chs[0]);
        assertEquals(new String(new char[] { chs[1] }), newStr);

        newStr = Strings.removeAll(str, new String(Array.of(chs[0])));
        assertEquals(new String(new char[] { chs[1] }), newStr);

        newStr = Strings.removeAll(str, new String(chs));
        assertEquals("", newStr);

        newStr = Strings.removeAll(str, str);
        assertEquals("", newStr);
    }

    @Test
    public void testRemoveStart() {
        assertEquals("bc", Strings.removeStart("abc", "a"));
        assertEquals("abc", Strings.removeStart("abc", "x"));
        assertEquals("", Strings.removeStart("abc", "abc"));
        assertNull(Strings.removeStart(null, "a"));
    }

    @Test
    public void testRemoveStart_EdgeCases() {
        assertNull(Strings.removeStart(null, "abc"));
        assertEquals("", Strings.removeStart("", "abc"));
        assertEquals("def", Strings.removeStart("abcdef", "abc"));
        assertEquals("abcdef", Strings.removeStart("abcdef", "xyz"));
        assertEquals("abcdef", Strings.removeStart("abcdef", null));
        assertEquals("abcdef", Strings.removeStart("abcdef", ""));
    }

    @Test
    public void testRemoveStartIgnoreCase() {
        assertEquals("bc", Strings.removeStartIgnoreCase("abc", "A"));
        assertEquals("bc", Strings.removeStartIgnoreCase("Abc", "a"));
        assertNull(Strings.removeStartIgnoreCase(null, "a"));
    }

    @Test
    public void testRemoveStartIgnoreCase_EdgeCases() {
        assertNull(Strings.removeStartIgnoreCase(null, "abc"));
        assertEquals("def", Strings.removeStartIgnoreCase("ABCdef", "abc"));
        assertEquals("abcdef", Strings.removeStartIgnoreCase("abcdef", "xyz"));
    }

    @Test
    public void testRemoveEnd() {
        assertEquals("ab", Strings.removeEnd("abc", "c"));
        assertEquals("abc", Strings.removeEnd("abc", "x"));
        assertEquals("", Strings.removeEnd("abc", "abc"));
        assertEquals(null, Strings.removeEnd(null, "c"));
    }

    @Test
    public void testRemoveEnd_EdgeCases() {
        assertNull(Strings.removeEnd(null, "abc"));
        assertEquals("", Strings.removeEnd("", "abc"));
        assertEquals("abc", Strings.removeEnd("abcdef", "def"));
        assertEquals("abcdef", Strings.removeEnd("abcdef", "xyz"));
        assertEquals("abcdef", Strings.removeEnd("abcdef", null));
        assertEquals("abcdef", Strings.removeEnd("abcdef", ""));
    }

    @Test
    public void testRemoveEndIgnoreCase() {
        assertEquals("ab", Strings.removeEndIgnoreCase("abc", "C"));
        assertEquals("ab", Strings.removeEndIgnoreCase("abC", "c"));
        assertNull(Strings.removeEndIgnoreCase(null, "c"));
    }

    @Test
    public void testRemoveEndIgnoreCase_EdgeCases() {
        assertNull(Strings.removeEndIgnoreCase(null, "def"));
        assertEquals("abc", Strings.removeEndIgnoreCase("abcDEF", "def"));
        assertEquals("abcdef", Strings.removeEndIgnoreCase("abcdef", "xyz"));
    }

    @Test
    public void testRemoveAllChar() {
        assertEquals("qeed", Strings.removeAll("queued", 'u'));
        assertEquals("queued", Strings.removeAll("queued", 'z'));
    }

    @Test
    public void testRemoveAllString() {
        assertEquals("qd", Strings.removeAll("queued", "ue"));
    }

    @Test
    public void testRemoveAll_Char() {
        assertEquals("bc", Strings.removeAll("abc", 'a'));
        assertEquals("", Strings.removeAll("aaa", 'a'));
        assertEquals("abc", Strings.removeAll("abc", 'x'));
        assertNull(Strings.removeAll(null, 'a'));
    }

    @Test
    public void testRemoveAll_CharWithFromIndex() {
        assertEquals("abc", Strings.removeAll("abcaa", 3, 'a'));
        assertEquals("abc", Strings.removeAll("abc", 0, 'x'));
    }

    @Test
    public void testRemoveAll_String() {
        assertEquals("cde", Strings.removeAll("ababcde", "ab"));
        assertEquals("abc", Strings.removeAll("abc", "xy"));
        assertNull(Strings.removeAll(null, "ab"));
    }

    @Test
    public void testRemoveAll_StringWithFromIndex() {
        assertEquals("ababcde", Strings.removeAll("ababcde", 4, "ab"));
        assertEquals("abc", Strings.removeAll("abc", 0, "xy"));
    }

    @Test
    public void testRemoveAll_CharEdgeCases() {
        assertNull(Strings.removeAll(null, 'a'));
        assertEquals("", Strings.removeAll("", 'a'));
        assertEquals("bc", Strings.removeAll("abc", 'a'));
        assertEquals("bc", Strings.removeAll("aabca", 'a'));
        assertEquals("abc", Strings.removeAll("abc", 'x'));
    }

    @Test
    public void testRemoveAll_StringEdgeCases() {
        assertNull(Strings.removeAll(null, "ab"));
        assertEquals("", Strings.removeAll("", "ab"));
        assertEquals("c", Strings.removeAll("abc", "ab"));
        assertEquals("abc", Strings.removeAll("abc", "xy"));
        assertEquals("abc", Strings.removeAll("abc", null));
        assertEquals("abc", Strings.removeAll("abc", ""));
    }

    @Test
    public void testRemoveAll_FromIndex_Char() {
        // basic
        assertEquals("hell wrld", Strings.removeAll("hello world", 0, 'o'));

        // fromIndex > 0
        assertEquals("hello wrld", Strings.removeAll("hello world", 5, 'o'));

        // char not found
        assertEquals("hello", Strings.removeAll("hello", 0, 'z'));

        // null/empty
        assertNull(Strings.removeAll((String) null, 0, 'a'));
        assertEquals("", Strings.removeAll("", 0, 'a'));
    }

    @Test
    public void testRemoveWhitespaceEach_Basic() {
        String[] array = { "a b", " x y z " };
        Strings.removeWhitespaceEach(array);
        assertArrayEquals(new String[] { "ab", "xyz" }, array);
    }

    // ==================== NEW TESTS: Additional coverage for important utility methods ====================

    @Test
    public void testRemoveWhitespace() {
        assertNull(Strings.removeWhitespace(null));
        assertEquals("", Strings.removeWhitespace(""));
        assertEquals("abc", Strings.removeWhitespace("abc"));
        assertEquals("abc", Strings.removeWhitespace("a b c"));
        assertEquals("abc", Strings.removeWhitespace("  a  b  c  "));
        assertEquals("abc", Strings.removeWhitespace("a\tb\nc"));
        assertEquals("abc", Strings.removeWhitespace("a b\tc\r\n"));
        assertEquals("abc", Strings.removeWhitespace("\t\n\r abc \t\n\r"));
    }

    @Test
    public void testRemoveWhitespaceEach_NullAndMixedWhitespace() {
        // null array - no exception
        Strings.removeWhitespaceEach(null);

        String[] strs = { "  hello  ", "world\t123", " h i " };
        Strings.removeWhitespaceEach(strs);
        assertEquals("hello", strs[0]);
        assertEquals("world123", strs[1]);
        assertEquals("hi", strs[2]);

        String[] withNulls = { null, "a b" };
        Strings.removeWhitespaceEach(withNulls);
        assertNull(withNulls[0]);
        assertEquals("ab", withNulls[1]);
    }

    @Test
    public void testRemoveRange() {
        assertEquals("hlo", Strings.removeRange("hello", 1, 3));
        assertEquals("hello", Strings.removeRange("hello", 0, 0));
        assertEquals("", Strings.removeRange("hello", 0, 5));
        assertNull(Strings.removeRange(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange(null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange("hello", -1, 2));
    }

    @Test
    public void testRemoveAllIgnoreCase() {
        // normal / case-insensitive
        assertEquals("qd", Strings.removeAllIgnoreCase("queued", "UE"));
        assertEquals("bB", Strings.removeAllIgnoreCase("aAbBaA", "a"));
        assertEquals("", Strings.removeAllIgnoreCase("abcABC", "abc"));
        // not found
        assertEquals("queued", Strings.removeAllIgnoreCase("queued", "zz"));
        // null/empty
        assertNull(Strings.removeAllIgnoreCase(null, "*"));
        assertEquals("", Strings.removeAllIgnoreCase("", "*"));
        assertEquals("abc", Strings.removeAllIgnoreCase("abc", null));
        assertEquals("abc", Strings.removeAllIgnoreCase("abc", ""));
    }

    @Test
    public void testRemoveAllIgnoreCase_FromIndex() {
        // normal / case-insensitive with fromIndex
        assertEquals("", Strings.removeAllIgnoreCase("ABCabcABC", 0, "abc"));
        assertEquals("ABC", Strings.removeAllIgnoreCase("ABCabcABC", 3, "abc"));
        assertEquals("hell WRLD", Strings.removeAllIgnoreCase("hello WORLD", 0, "O"));
        // no match at or after index
        assertEquals("test", Strings.removeAllIgnoreCase("test", 4, "es"));
        // negative fromIndex treated as 0
        assertEquals("bB", Strings.removeAllIgnoreCase("aAbBaA", -5, "a"));
        // null/empty
        assertNull(Strings.removeAllIgnoreCase(null, 0, "abc"));
        assertEquals("", Strings.removeAllIgnoreCase("", 0, "abc"));
        assertEquals("abc", Strings.removeAllIgnoreCase("abc", 0, null));
    }
}
