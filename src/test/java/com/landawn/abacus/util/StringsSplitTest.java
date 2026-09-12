package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class StringsSplitTest extends StringsTestSupport {

    @Test
    public void testSplit() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ','));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a.b.c", '.'));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a..b.c", '.'));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", ','));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split(null, ','));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", ','));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a::b::c", "::"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a,,b", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,,,b,,,c", ","));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", "::"));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split(null, ","));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", ","));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::b::", "::"));
    }

    @Test
    public void testSplit_Trim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ',', false));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a . b . c ", '.', true));
        assertEquals(0, Strings.split((String) null, ',', true).length);
        assertEquals(0, Strings.split("", ',', true).length);
        assertEquals(0, Strings.split((String) null, ',', false).length);

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ",", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ",", false));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a :: b :: c ", "::", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a :: b :: c ", "::", false));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::::b", "::", true));
        assertArrayEquals(new String[0], Strings.split(null, "::", true));
        assertArrayEquals(new String[0], Strings.split("", "::", true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", true));
    }

    @Test
    public void testSplit_Max() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ",", 3));
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a,b,c" }, Strings.split("a,b,c", ",", 1));
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.split("a.b.c", ".", 2));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ",", 5));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 3));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 100));
        assertArrayEquals(new String[] { "a", "b:c:d" }, Strings.split("a:b:c:d", ":", 2));
        assertArrayEquals(new String[] { "a", "b::c" }, Strings.split("a::::b::c", "::", 2));
        assertArrayEquals(new String[] { "a" }, Strings.split("a::::", "::", 2));
        assertArrayEquals(new String[] { "a::b" }, Strings.split("::a::b", "::", 1));
        assertArrayEquals(new String[] { "a,b" }, Strings.split(",,a,b", ",", 1));
        assertArrayEquals(new String[] { "a,b" }, Strings.split(",,a,b", ',', 1));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("::::", "::", 1));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1));
        assertArrayEquals(new String[] { "a", "b::c" }, Strings.split("a::b::c", "::", 2));
        assertArrayEquals(new String[] { "a", "b", "c:d:e" }, Strings.split("a:b:c:d:e", ":", 3));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 5));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 10));
        assertArrayEquals(new String[0], Strings.split(null, ":", 3));
        assertArrayEquals(new String[0], Strings.split("", ":", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", -1));
    }

    @Test
    public void testSplit_MaxAndTrim() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split(" a , b,c ", ",", 2, true));
        assertArrayEquals(new String[] { " a ", " b,c " }, Strings.split(" a , b,c ", ",", 2, false));
        assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.split(" a : b : c : d ", ":", 3, true));
        assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.split(" a : b : c : d ", ":", 3, false));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1, true));
        assertArrayEquals(new String[] { "  a:b:c  " }, Strings.split("  a:b:c  ", ":", 1, false));
        assertArrayEquals(new String[] { "a ::b" }, Strings.split(":: a ::b", "::", 1, true));
        assertArrayEquals(new String[] { "a,b" }, Strings.split(",,a,b", ',', 1, true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", 3, true));
        assertArrayEquals(new String[] { "a::b::c" }, Strings.split("a::b::c", "::", 1, true));
    }

    @Test
    public void testSplit_rejectsNullOrEmptyDelimiter() {
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", null, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", "", true));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", "", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("a b c", null, 2, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", "", true));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", "", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("a  b", null, 2, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.split(null, ""));
    }

    @Test
    public void testSplitOnWhitespace() {
        assertArrayEquals(new String[] { "hello", "world", "test" }, Strings.splitOnWhitespace("hello   world\ttest"));
        assertArrayEquals(new String[] { "hello", "world" }, Strings.splitOnWhitespace("  hello   world  "));
        assertArrayEquals(new String[] { "hello", "world" }, Strings.splitOnWhitespace("  hello   world  ", true));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitOnWhitespace("a  b\tc"));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitOnWhitespace(" a  b  c ", true));
        assertArrayEquals(new String[] { "a", "b c" }, Strings.splitOnWhitespace("a b c", 2));
        assertArrayEquals(new String[] { "a", "b c" }, Strings.splitOnWhitespace("a b c", 2, true));
        assertArrayEquals(new String[] { "a b" }, Strings.splitOnWhitespace("  a b", 1));
        assertArrayEquals(new String[] { "a b" }, Strings.splitOnWhitespace("  a b", 1, true));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace(null));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace(""));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace("   ", 1));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespace("   ", 1, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitOnWhitespace("a b", 0));
    }

    @Test
    public void testSplitPreserveAllTokens() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ','));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ','));
        assertArrayEquals(new String[] { "", "a" }, Strings.splitPreserveAllTokens(",a", ','));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a..b.c", '.'));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", '.'));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, '.'));
        assertArrayEquals(new String[] { "", "a", "b", "" }, Strings.splitPreserveAllTokens(".a.b.", '.'));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a,,b,c", ','));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ',', true));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a . b . c ", '.', true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a . b . c ", '.', false));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a..b", '.', true));
        assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" .. ", '.', true));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens((String) null, ',', true));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ',', true));
        assertArrayEquals(new String[] { " a ", " b " }, Strings.splitPreserveAllTokens(" a , b ", ',', false));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a::b::c", "::"));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::::b", "::"));
        assertArrayEquals(new String[] { "test" }, Strings.splitPreserveAllTokens("test", "::"));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, "::"));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", "::"));
        assertArrayEquals(new String[] { "ab", "cd", "ef" }, Strings.splitPreserveAllTokens("ab:cd:ef", ":"));
        assertArrayEquals(new String[] { "ab", "cd", "ef", "" }, Strings.splitPreserveAllTokens("ab:cd:ef:", ":"));
        assertArrayEquals(new String[] { "", "cd", "ef" }, Strings.splitPreserveAllTokens(":cd:ef", ":"));
        assertArrayEquals(new String[] { "abc", "", "def" }, Strings.splitPreserveAllTokens("abc  def", " "));
        assertArrayEquals(new String[] { "", "", "", "" }, Strings.splitPreserveAllTokens(":::", ":"));
        assertArrayEquals(new String[] { "", "" }, Strings.splitPreserveAllTokens(":", ":"));
        assertArrayEquals(new String[] { "", "a", "", "b", "" }, Strings.splitPreserveAllTokens(":a::b:", ":"));
        assertArrayEquals(new String[] { "a", "", "", "b" }, Strings.splitPreserveAllTokens("a::::::b", "::"));

        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a : b : c ", ":", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a : b : c ", ":", false));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::b", ":", true));
        assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" :: ", ":", true));
        assertEquals(0, Strings.splitPreserveAllTokens((String) null, ",", true).length);
        assertEquals(1, Strings.splitPreserveAllTokens("", ",", true).length);
        assertEquals("", Strings.splitPreserveAllTokens("", ",", true)[0]);
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens(" a , , b ", ",", true));
    }

    @Test
    public void testSplitPreserveAllTokens_Max() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.splitPreserveAllTokens("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a", ",c" }, Strings.splitPreserveAllTokens("a,,c", ",", 2));
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.splitPreserveAllTokens("a ..b.c ", "..", 3, true));
        assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, true));
        assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, false));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a::b:c", ":", 4, true));
        assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" : : ", ":", 3, true));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1, true));
        assertArrayEquals(new String[] { "  a:b:c  " }, Strings.splitPreserveAllTokens("  a:b:c  ", ":", 1, false));
        assertArrayEquals(new String[] { "a,b" }, Strings.splitPreserveAllTokens("  a,b  ", ",", 1, true));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens((String) null, ":", 5));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ":", 5));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 5));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a::c", ":", 5));
        assertEquals(0, Strings.splitPreserveAllTokens((String) null, ",", 3, true).length);
        assertEquals(1, Strings.splitPreserveAllTokens("", ",", 3, true).length);
        assertEquals(3, Strings.splitPreserveAllTokens(" a , , b , c ", ",", 3, true).length);
        assertArrayEquals(new String[] { "a", "b", "c:d" }, Strings.splitPreserveAllTokens("a:b:c:d", ":", 3));
        assertArrayEquals(new String[] { "a", "b", "::d" }, Strings.splitPreserveAllTokens("a:b:::d", ":", 3));
        assertArrayEquals(new String[] { "", "a", "b", "c" }, Strings.splitPreserveAllTokens(":a:b:c", ":", 4));
        assertArrayEquals(new String[] { "a", "b", "c", "" }, Strings.splitPreserveAllTokens("a:b:c:", ":", 4));
        assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1));
        assertArrayEquals(new String[] { ",,a,b" }, Strings.splitPreserveAllTokens(",,a,b", ',', 1));
        assertArrayEquals(new String[] { ",,a,b" }, Strings.splitPreserveAllTokens(",,a,b", ',', 1, true));
        assertArrayEquals(new String[] { "::a::b" }, Strings.splitPreserveAllTokens("::a::b", "::", 1));
        assertArrayEquals(new String[] { "a", "::b" }, Strings.splitPreserveAllTokens("a::::b", "::", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("test", ":", 0));
    }

    @Test
    public void testSplitPreserveAllTokens_MultiCharDelimiter_TrailingEmptyToken() {
        assertArrayEquals(new String[] { "a", "b", "" }, Strings.splitPreserveAllTokens("a:b:", ':'));
        assertArrayEquals(new String[] { "a", "" }, Strings.splitPreserveAllTokens("a:", ':'));
        assertArrayEquals(new String[] { "a", "b", "" }, Strings.splitPreserveAllTokens("a::b::", "::"));
        assertArrayEquals(new String[] { "a", "" }, Strings.splitPreserveAllTokens("a::", "::"));
        assertArrayEquals(new String[] { "a", "b", "", "" }, Strings.splitPreserveAllTokens("a::b::::", "::"));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a::b::c", "::"));
        assertArrayEquals(new String[] { "", "a", "b" }, Strings.splitPreserveAllTokens("::a::b", "::"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::b::", "::"));
    }

    @Test
    public void testSplitPreserveAllTokens_MultiCharDelimiter_MaxNoSpuriousTrailingEmpty() {
        assertArrayEquals(new String[] { "a", "b..c" }, Strings.splitPreserveAllTokens("a..b..c", "..", 2));
        assertArrayEquals(new String[] { "a", "b::c::d" }, Strings.splitPreserveAllTokens("a::b::c::d", "::", 2));
        assertArrayEquals(new String[] { "", "b..c" }, Strings.splitPreserveAllTokens("..b..c", "..", 2));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitPreserveAllTokens("a..b", "..", 2));
        assertArrayEquals(new String[] { "a", "b", "c..d" }, Strings.splitPreserveAllTokens("a..b..c..d", "..", 3));
        assertArrayEquals(new String[] { "a", "b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 2));
        assertArrayEquals(new String[] { "a", "b..c" }, Strings.splitPreserveAllTokens(" a .. b..c ", "..", 2, true));
    }

    @Test
    public void testSplitOnWhitespacePreserveAllTokens() {
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitOnWhitespacePreserveAllTokens("a  b"));
        assertArrayEquals(new String[] { "abc", "def" }, Strings.splitOnWhitespacePreserveAllTokens("abc def"));
        assertArrayEquals(new String[] { "" }, Strings.splitOnWhitespacePreserveAllTokens(""));
        assertArrayEquals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitOnWhitespacePreserveAllTokens(null));
        assertArrayEquals(new String[] { "a", " b c" }, Strings.splitOnWhitespacePreserveAllTokens("a  b c", 2));
        assertArrayEquals(new String[] { "", "a", "", "b", "" }, Strings.splitOnWhitespacePreserveAllTokens(" a  b ", false));
        assertArrayEquals(new String[] { "  a b" }, Strings.splitOnWhitespacePreserveAllTokens("  a b", 1));
        assertThrows(IllegalArgumentException.class, () -> Strings.splitOnWhitespacePreserveAllTokens("a b", 0));
    }

    @Test
    public void testSplitToLines() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\nb\nc"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\r\nb"));
        assertArrayEquals(new String[] { "" }, Strings.splitToLines(""));
        assertArrayEquals(new String[] {}, Strings.splitToLines(null));

        String[] substrs = Strings.splitToLines("aa\naa\r\n");
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);
        substrs = Strings.splitToLines("aa\r\naa\n");
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);
    }

    @Test
    public void testSplitToLines_WithOptions() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\n b \n\n c \r\n", true, true));
        assertArrayEquals(new String[] { "a", "b", "", "c", "" }, Strings.splitToLines("a\nb\n\nc\n", false, false));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines(" a \n b \n c ", true, false));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\n\nb", false, true));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines(" a \n\n b ", true, true));
        assertArrayEquals(new String[] { "a", "b", "\u2003" }, Strings.splitToLines("\ta\t\n\u0001b\u001F\n\u2003", true, false));
        assertArrayEquals(new String[] { "a" }, Strings.splitToLines("\t\n a \n\u0001\u001F", true, true));

        String[] substrs = Strings.splitToLines("aa\r\n  aa\n", true, false);
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);
        substrs = Strings.splitToLines("aa\r\n  aa\n", true, true);
        assertEquals(2, substrs.length);
        assertEquals(substrs[0], substrs[1]);
        substrs = Strings.splitToLines("aa\r\n  aa\n", false, true);
        assertEquals(2, substrs.length);
        assertNotEquals(substrs[0], substrs[1]);
        assertEquals(0, Strings.splitToLines("", false, true).length);
        assertEquals(0, Strings.splitToLines(null, false, true).length);
    }
}
