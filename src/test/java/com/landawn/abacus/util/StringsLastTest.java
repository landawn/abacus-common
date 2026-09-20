package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.StrUtil;

public class StringsLastTest extends StringsTestSupport {
    @Test
    public void test_lastIndex() {
        assertEquals(2, "aba".lastIndexOf("a", 3));
        assertEquals(2, "aba".lastIndexOf("a", 2));
        assertEquals(0, "aba".lastIndexOf("a", 1));
        assertEquals(0, "aba".lastIndexOf("a", 0));
        assertEquals(-1, "aba".lastIndexOf("a", -1));
    }

    @Test
    public void testLastIndexOf_CharWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", 'a', 2));
        assertEquals(-1, Strings.lastIndexOf("abc", 'c', 1));
    }

    @Test
    public void testLastIndexOf_StringWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOf("abc", "abc", -1));
    }

    @Test
    public void testLastIndexOf_Char() {
        assertEquals(3, Strings.lastIndexOf("abcabc", 'a'));
        assertEquals(5, Strings.lastIndexOf("abcabc", 'c'));
        assertEquals(-1, Strings.lastIndexOf("abc", 'x'));
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
    }

    @Test
    public void testLastIndexOf_String() {
        assertEquals(3, Strings.lastIndexOf("abcabc", "abc"));
        assertEquals(0, Strings.lastIndexOf("abc", "abc"));
        assertEquals(-1, Strings.lastIndexOf("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOf(null, "abc"));
    }

    @Test
    public void testLastIndexOfAllWithOverlappingPatterns() {
        String text = "ababababab";
        Assertions.assertEquals(8, Strings.maxLastIndexOfAll(text, "ab", "ba"));
        Assertions.assertEquals(7, Strings.minLastIndexOfAll(text, "ab", "ba"));

        Assertions.assertEquals(-1, Strings.maxLastIndexOfAll(text, "xyz", "qrs"));
    }

    @Test
    public void testLastIndexOf() {
        assertEquals(3, Strings.lastIndexOf("hello", 'l'));
        assertEquals(-1, Strings.lastIndexOf("hello", 'x'));
        assertEquals(-1, Strings.lastIndexOf(null, 'l'));

        assertEquals(1, Strings.lastIndexOf("hello", "ell"));
        assertEquals(-1, Strings.lastIndexOf("hello", "xyz"));
    }

    @Test
    public void testLastIndexOfChar() {
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
        assertEquals(-1, Strings.lastIndexOf("", 'a'));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", 'a'));
        assertEquals(5, Strings.lastIndexOf("aabaabaa", 'b'));
    }

    @Test
    public void testLastIndexOfString() {
        assertEquals(-1, Strings.lastIndexOf(null, "a"));
        assertEquals(-1, Strings.lastIndexOf("", "a"));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", "a"));
        assertEquals(4, Strings.lastIndexOf("aabaabaa", "ab"));
    }

    @Test
    public void testLastIndexOf_WithDelimiter() {
        int result = StrUtil.lastIndexOfToken("a,b,c,b", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfToken(null, "b", ","));
    }

    @Test
    public void testLastIndexOf_WithDelimiterAndFromBack() {
        int result = StrUtil.lastIndexOfToken("a,b,c,b", "b", ",", 10);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfToken(null, "b", ",", 0));
    }

    /**
     * Regression: lastIndexOf(str, value, delimiter, startIndexFromBack) must not
     * return a position past startIndexFromBack. Before the fix, the leading-
     * delimiter branch returned the start-of-delimiter position from
     * String.lastIndexOf(), then added delimiter.length() — pushing the result
     * past the user-supplied search bound when the value was at the end of the string.
     * Per the documented example: lastIndexOf("test value test", "test", " ", 10) == 0.
     */
    @Test
    public void testLastIndexOf_WithDelimiter_doesNotExceedStartIndexFromBack() {
        // Documented example from Javadoc.
        assertEquals(0, StrUtil.lastIndexOfToken("test value test", "test", " ", 10));

        // The trailing "hello" at position 6 is the right answer when bounded at 10,
        // not the trailing "hello" at position 12.
        assertEquals(6, StrUtil.lastIndexOfToken("hello hello hello", "hello", " ", 10));

        // Multi-char delimiter case: the trailing token must respect the bound.
        assertEquals(0, StrUtil.lastIndexOfToken("abc :: xyz :: abc", "abc", " :: ", 10));

        // Within-bounds matches still resolve correctly.
        assertEquals(13, StrUtil.lastIndexOfToken("apple,banana,apple", "apple", ",", 20));
        assertEquals(0, StrUtil.lastIndexOfToken("apple,banana,apple", "apple", ",", 10));
    }

    @Test
    public void testLastIndexOfIgnoreCase() {
        assertEquals(3, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc"));
        assertEquals(0, Strings.lastIndexOfIgnoreCase("ABC", "abc"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase(null, "abc"));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithStartIndex() {
        assertEquals(0, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz", 5));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiter() {
        int result = StrUtil.lastIndexOfTokenIgnoreCase("a,B,c,b", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(null, "b", ","));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiterAndFromBack() {
        int result = StrUtil.lastIndexOfTokenIgnoreCase("a,B,c,b", "b", ",", 10);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(null, "b", ",", 0));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiter_respectsStartIndexBound() {
        // Regression: the VALUE's position (not the leading delimiter's) must not exceed startIndexFromBack,
        // matching the case-sensitive lastIndexOf(String,String,String,int) twin. The IgnoreCase variant
        // previously omitted the "- delimiterLen" start-bound adjustment and wrongly returned a positive index.
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(",a", "a", ",", 0)); // "a" is at index 1, beyond bound 0
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("xy,z", "z", ",", 2)); // "z" is at index 3, beyond bound 2

        // Parity with the case-sensitive twin for the same inputs.
        assertEquals(StrUtil.lastIndexOfToken(",a", "a", ",", 0), StrUtil.lastIndexOfTokenIgnoreCase(",a", "a", ",", 0));
        assertEquals(StrUtil.lastIndexOfToken("xy,z", "z", ",", 2), StrUtil.lastIndexOfTokenIgnoreCase("xy,z", "z", ",", 2));

        // When the bound includes the value's position, the (case-insensitive) match is found.
        assertEquals(1, StrUtil.lastIndexOfTokenIgnoreCase(",A", "a", ",", 1));
    }

    @Test
    public void testLastIndexOfIgnoreCase_DelimiterSkipsEmbeddedToken() {
        assertEquals(10, StrUtil.lastIndexOfTokenIgnoreCase("pineapple,APPLE", "apple", ","));
        assertEquals(0, StrUtil.lastIndexOfTokenIgnoreCase("apple,pineapple", "apple", ",", 8));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("pineapple", "apple", ","));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiterAndStartIndex() {
        // empty delimiter: falls back to lastIndexOfIgnoreCase without delimiter
        assertTrue(StrUtil.lastIndexOfTokenIgnoreCase("hello world", "WORLD", "", 20) >= 0);

        // null str returns -1
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase(null, "world", ",", 20));

        // null valueToFind returns -1
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("hello world", null, ",", 20));

        // negative startIndex returns -1
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("hello world", "world", ",", -1));

        // value found at end boundary (no trailing delimiter)
        assertEquals(6, StrUtil.lastIndexOfTokenIgnoreCase("hello,world", "WORLD", ",", 20));

        // value not present
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("hello,world", "xyz", ",", 20));
    }

    @Test
    public void testLastIndexOfAny() {
        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

    }

    @Test
    public void testLastIndexOfAny_CharArray() {
        assertEquals(0, Strings.lastIndexOfAny("abc", 'a', 'c'));
        assertEquals(5, Strings.lastIndexOfAny("abcabc", new char[] { 'c' }));
        assertEquals(-1, Strings.lastIndexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.lastIndexOfAny((String) null, new char[] { 'a' }));
    }

    @Test
    public void testLastIndexOfAny_StringArray() {
        assertEquals(3, Strings.lastIndexOfAny("abcabc", "abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfAny("abc", "xyz", "123"));
        assertEquals(-1, Strings.lastIndexOfAny(null, "abc"));
    }

    @Test
    public void testLastIndexOfAnyChars() {
        assertEquals(-1, Strings.lastIndexOfAny((String) null, new char[] { 'a' }));
        assertEquals(-1, Strings.lastIndexOfAny("", new char[] { 'a' }));
        assertEquals(1, Strings.lastIndexOfAny("zzabyycdxx", 'z', 'x'));
        assertEquals(7, Strings.lastIndexOfAny("zzabyycdxx", 'd', 'x'));
    }

    @Test
    public void testLastIndexOfAnyStrings() {
        assertEquals(-1, Strings.lastIndexOfAny(null, "ab"));
        assertEquals(-1, Strings.lastIndexOfAny("", "ab"));
        assertEquals(2, Strings.lastIndexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(8, Strings.lastIndexOfAny("zzabyycdxx", "dd", "xx"));
    }

    @Test
    public void testLastIndexOfAny_WithStartIndexFromBack_Char() {
        int result = Strings.lastIndexOfAny("hello world", 10, 'o', 'd');
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOfAny(null, 0, Array.of('o')));
    }

    @Test
    public void testLastIndexOfAny_WithStartIndexFromBack_String() {
        int result = Strings.lastIndexOfAny("hello world hello", 16, "hello", "xyz");
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOfAny(null, 0, "hello"));
    }

    @Test
    public void testLastIndexOfAny_WithStartIndex() {
        // basic usage
        assertEquals(4, Strings.lastIndexOfAny("hello", 10, 'o', 'x'));
        assertEquals(1, Strings.lastIndexOfAny("hello", 10, 'e', 'x'));
        // startIndexFromBack limits search
        assertEquals(-1, Strings.lastIndexOfAny("hello", 0, 'l', 'o'));
        assertEquals(0, Strings.lastIndexOfAny("hello", 0, 'h'));
        // null / empty
        assertEquals(-1, Strings.lastIndexOfAny(null, 5, 'a'));
        assertEquals(-1, Strings.lastIndexOfAny("hello", 5, new char[0]));
        // many chars (triggers set-based path)
        assertEquals(10, Strings.lastIndexOfAny("hello world", 10, 'a', 'b', 'c', 'd')); // 'd' is at index 10
        assertEquals(9, Strings.lastIndexOfAny("hello world", 10, 'l', 'r', 'o', 'x', 'y', 'z'));
    }

    @Test
    public void testLastIndexOfAny_TreatsSurrogateAsCodeUnit() {
        // Lone surrogate code units are treated as ordinary UTF-16 code units (no validation, no exception).
        assertEquals(0, Strings.lastIndexOfAny("abc", '\uDC00', 'a'));
        assertEquals(0, Strings.lastIndexOfAny("abc", 2, '\uDC00', 'a'));
        // A lone surrogate that does not occur in the string simply does not match.
        assertEquals(-1, Strings.lastIndexOfAny("abc", '\uDC00', '\uDFFF'));
    }

    @Test
    public void testLastOrdinalIndexOf() {
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(4, Strings.lastOrdinalIndexOf("aabaabaa", "a", 3));
        assertEquals(0, Strings.lastOrdinalIndexOf("aabaabaa", "a", 6));
        assertEquals(2, Strings.lastOrdinalIndexOf("aabaabaa", "b", 2));
        assertEquals(1, Strings.lastOrdinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(-1, Strings.lastOrdinalIndexOf("aabaabaa", "c", 1));
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.lastOrdinalIndexOf("abcabc", "abc", 1));
        assertEquals(0, Strings.lastOrdinalIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.lastOrdinalIndexOf("abcabc", "abc", 3));
        assertEquals(0, Strings.lastOrdinalIndexOf("", "", 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf("", null, 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, "", 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, null, 1));
    }

    @Test
    public void testLastOrdinalIndexOf_EdgeCases() {
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(6, Strings.lastOrdinalIndexOf("aabaabaa", "a", 2));
        assertEquals(4, Strings.lastOrdinalIndexOf("aabaabaa", "a", 3));
        assertEquals(-1, Strings.lastOrdinalIndexOf("aabaabaa", "a", 10));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, "a", 1));
    }

    @Test
    public void testLastIndexOfWithDelimiter_tokenAtStartShadowedByLaterOccurrence() {
        // regression: the start-of-string fallback only tested the LAST plain occurrence, so a valid
        // token at index 0 shadowed by a later non-delimited occurrence was missed
        assertEquals(0, StrUtil.lastIndexOfToken("ab,abx", "ab", ","));
        assertEquals(0, StrUtil.lastIndexOfTokenIgnoreCase("ab,abx", "AB", ","));

        // unchanged behavior
        assertEquals(3, StrUtil.lastIndexOfToken("ab,ab", "ab", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("xab,abx", "ab", ","));
    }

    /**
     * Contract pin for the delimiter-in-token rule now documented on the four {@code lastIndexOfToken} javadocs:
     * with a NON-EMPTY delimiter, a token that itself contains the delimiter spans more than one field and can never
     * match, and the case-insensitive overloads apply that containment test case-insensitively.
     */
    @Test
    public void testLastIndexOfToken_TokenContainingDelimiterNeverMatches() {
        assertEquals(-1, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ",", 7));
        assertEquals(-1, StrUtil.lastIndexOfToken("a,b", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("a,b", "a,b", ",", 3));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("x,a,b,y", "A,B", ",", 7));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("a,b", "a,b", ",", 3));
        // a plain search does find it
        assertEquals(2, Strings.lastIndexOf("x,a,b,y", "a,b"));

        // the rule is scoped to a non-empty delimiter: an empty or null delimiter delegates to a plain search
        assertEquals(2, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", ""));
        assertEquals(2, StrUtil.lastIndexOfToken("x,a,b,y", "a,b", null));
        assertEquals(2, StrUtil.lastIndexOfTokenIgnoreCase("x,a,b,y", "A,B", ""));

        // the containment test of the IgnoreCase overloads is itself case-insensitive
        assertEquals(4, StrUtil.lastIndexOfToken("pANDxandyANDq", "xandy", "AND"));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND"));
        assertEquals(4, StrUtil.lastIndexOfToken("pANDxandyANDq", "xandy", "AND", 13));
        assertEquals(-1, StrUtil.lastIndexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND", 13));
    }
}
