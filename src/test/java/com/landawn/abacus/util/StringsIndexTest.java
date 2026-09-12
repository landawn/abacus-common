package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.StrUtil;

public class StringsIndexTest extends StringsTestSupport {
    @Test
    public void testIndexOf_WithDelimiter_ValueAtEnd() {
        assertEquals(8, StrUtil.indexOfToken("one,two,three", "three", ",", 0));
        assertEquals(-1, StrUtil.indexOfToken("one,two,three", "four", ",", 0));
    }

    @Test
    public void testIndexOf_Char() {
        assertEquals(0, Strings.indexOf("abc", 'a'));
        assertEquals(2, Strings.indexOf("abc", 'c'));
        assertEquals(-1, Strings.indexOf("abc", 'x'));
        assertEquals(-1, Strings.indexOf(null, 'a'));
    }

    @Test
    public void testIndexOf_CharWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", 'a', 1));
        assertEquals(-1, Strings.indexOf("abc", 'a', 5));
        assertEquals(-1, Strings.indexOf(null, 'a', 0));
    }

    @Test
    public void testIndexOf_String() {
        assertEquals(0, Strings.indexOf("abcdef", "abc"));
        assertEquals(3, Strings.indexOf("abcdef", "def"));
        assertEquals(-1, Strings.indexOf("abcdef", "xyz"));
        assertEquals(-1, Strings.indexOf(null, "abc"));
    }

    @Test
    public void testIndexOf_StringWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", "abc", 1));
        assertEquals(-1, Strings.indexOf("abc", "abc", 5));
        assertEquals(-1, Strings.indexOf(null, "abc", 0));
    }

    @Test
    public void testIndexOf() {
        assertEquals(1, Strings.indexOf("hello", 'e'));
        assertEquals(-1, Strings.indexOf("hello", 'x'));
        assertEquals(-1, Strings.indexOf(null, 'e'));
        assertEquals(-1, Strings.indexOf("", 'e'));

        assertEquals(1, Strings.indexOf("hello", "ell"));
        assertEquals(-1, Strings.indexOf("hello", "xyz"));
        assertEquals(-1, Strings.indexOf(null, "ell"));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        assertEquals(3, Strings.indexOf("hello", 'l', 3));
        assertEquals(-1, Strings.indexOf("hello", 'e', 2));

        assertEquals(4, Strings.indexOf("hello hello", "o", 2));
        assertEquals(-1, Strings.indexOf("hello", "ell", 2));
    }

    @Test
    public void testIndexOfChar() {
        assertEquals(-1, Strings.indexOf(null, 'a'));
        assertEquals(-1, Strings.indexOf("", 'a'));
        assertEquals(0, Strings.indexOf("aabaabaa", 'a'));
        assertEquals(2, Strings.indexOf("aabaabaa", 'b'));
    }

    @Test
    public void testIndexOfString() {
        assertEquals(-1, Strings.indexOf(null, "a"));
        assertEquals(-1, Strings.indexOf("", "a"));
        assertEquals(0, Strings.indexOf("aabaabaa", "a"));
        assertEquals(2, Strings.indexOf("aabaabaa", "b"));
        assertEquals(1, Strings.indexOf("aabaabaa", "ab"));
    }

    @Test
    public void testIndexOf_WithDelimiter() {
        // indexOf with delimiter searches for valueToFind as a whole token separated by delimiter
        int result = StrUtil.indexOfToken("a,b,c", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfToken(null, "b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b,c", null, ","));
    }

    @Test
    public void testIndexOf_WithDelimiterAndFromIndex() {
        int result = StrUtil.indexOfToken("a,b,c,b", "b", ",", 3);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfToken(null, "b", ",", 0));
    }

    @Test
    public void testIndexOf_WithDelimiterAndFromIndex_ExtendedCoverage() {
        // empty delimiter falls back to simple indexOf
        assertEquals(2, StrUtil.indexOfToken("a,b,c", "b", "", 0));

        // with delimiter, value found at start boundary
        assertEquals(2, StrUtil.indexOfToken("a,b,c", "b", ",", 0));

        // value at end boundary
        assertEquals(4, StrUtil.indexOfToken("a,b,c", "c", ",", 0));

        // value not found
        assertEquals(-1, StrUtil.indexOfToken("a,b,c", "d", ",", 0));

        // null str
        assertEquals(-1, StrUtil.indexOfToken(null, "b", ",", 0));

        // null valueToFind
        assertEquals(-1, StrUtil.indexOfToken("a,b,c", null, ",", 0));
    }

    @Test
    public void testIndexOf_Delimited_SeparatorOverlapsValue() {
        // "bab" | "a" | "bab" — first raw 'a' sits inside the leading separator
        assertEquals(3, StrUtil.indexOfToken("bababab", "a", "bab"));
        assertEquals(3, StrUtil.indexOfToken("bababab", "a", "bab", 0));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("BaBaBaB", "A", "BAB"));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("BaBaBaB", "a", "bab", 0));

        // Still rejects undelimited substrings
        assertEquals(-1, StrUtil.indexOfToken("bababab", "aba", "bab"));
        assertEquals(-1, StrUtil.indexOfToken("pineapple", "apple", ","));
        assertEquals(-1, StrUtil.indexOfToken("apple,ban,cherry", "banana", ","));

        // Multi-char separator, value only at end after separator
        assertEquals(4, StrUtil.indexOfToken("xy::z", "z", "::"));
        assertEquals(0, StrUtil.indexOfToken("z::xy", "z", "::"));
    }

    @Test
    public void testIndexOf_Delimited_FromIndexDoesNotCreateBoundary() {
        assertEquals(10, StrUtil.indexOfToken("pineapple,apple", "apple", ",", 4));
        assertEquals(-1, StrUtil.indexOfToken("pineapple", "apple", ",", 4));
        assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineAPPLE,APPLE", "apple", ",", 4));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("pineAPPLE", "apple", ",", 4));

        // A real multi-character separator may begin before the candidate-search lower bound.
        assertEquals(3, StrUtil.indexOfToken("a::b", "b", "::", 2));
        assertEquals(3, StrUtil.indexOfToken("a::b", "b", "::", 3));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("a::B", "b", "::", 2));
        assertEquals(3, StrUtil.indexOfTokenIgnoreCase("a::B", "b", "::", 3));
    }

    @Test
    public void testIndexOfAny_WithEmptyElementInVarargs() {
        // "" is tried first (per-needle order) and matches at fromIndex
        assertEquals(0, Strings.indexOfAny("hello world", 0, "", "world"));
        assertEquals(0, Strings.indexOfAny("hello world", 0, "", "xyz"));
        // a null element is still skipped, so "world" is found at 6
        assertEquals(6, Strings.indexOfAny("hello world", 0, null, "world"));
        // lastIndexOfAny: "" matches at min(startIndexFromBack, str.length()); null is skipped
        assertEquals(10, Strings.lastIndexOfAny("hello world", 10, "", "world"));
        assertEquals(6, Strings.lastIndexOfAny("hello world", 10, null, "world"));
        assertEquals(3, Strings.lastIndexOfAny("abc", ""));
        assertEquals(0, Strings.lastIndexOfAny("", ""));
        assertEquals(3, Strings.lastIndexOfAny("abc", 100, ""));
        assertEquals(Strings.lastIndexOf("abc", ""), Strings.lastIndexOfAny("abc", ""));
        assertEquals(Strings.lastIndexOf("", ""), Strings.lastIndexOfAny("", ""));
        assertEquals(Strings.maxLastIndexOfAll("abc", ""), Strings.lastIndexOfAny("abc", ""));
    }

    @Test
    public void test_indexOf() {
        assertEquals(0, Strings.lastIndexOf("aaaa", "aa", 0));
        assertEquals(1, Strings.lastIndexOf("aaaa", "aa", 1));
        assertEquals(2, Strings.lastIndexOf("aaaa", "aa", 2));
        assertEquals(2, Strings.lastIndexOf("aaaa", "aa", 3));
        assertEquals(0, "".indexOf(""));
        assertEquals(0, "".lastIndexOf(""));

        assertEquals(0, "abc".indexOf(""));
        assertEquals(3, "abc".lastIndexOf(""));

        assertEquals(4, StrUtil.indexOfToken("abc,,", "", ","));
        assertEquals(0, StrUtil.indexOfToken(",,abc", "", ","));
        assertEquals(1, StrUtil.indexOfToken(",,abc", "", ",", 1));
        assertEquals(-1, StrUtil.indexOfToken("abc,abc", "", ","));

        assertEquals(5, StrUtil.lastIndexOfToken("abc, aa, aa", "aa", ", ", 6));
        assertEquals(7, StrUtil.lastIndexOfToken("abc,aa,aa", "aa", ","));
        assertEquals(-1, StrUtil.lastIndexOfToken("aaaa", "aa", ",", 3));
        assertEquals(5, StrUtil.lastIndexOfToken("abc,,", "", ","));
        assertEquals(4, StrUtil.lastIndexOfToken("abc,,", "", ",", 4));
        assertEquals(5, StrUtil.lastIndexOfTokenIgnoreCase("abc,,", "", ","));
        assertEquals(4, StrUtil.lastIndexOfTokenIgnoreCase("abc,,", "", ",", 4));
        assertEquals(4, StrUtil.lastIndexOfTokenIgnoreCase("aEND", "", "end"));

        assertEquals(0, Strings.indexOf("abc,,", ""));
        assertEquals(1, Strings.indexOf("abc,,", "", 1));
        assertEquals(5, Strings.lastIndexOf("abc,,", ""));
        assertEquals(1, Strings.lastIndexOf("abc,,", "", 1));
        assertEquals(3, Strings.lastIndexOf("abc,,", "", 3));
        assertEquals(0, Strings.lastIndexOf("abc,,", "", 0));
        assertEquals(0, "aaa".indexOf("", -1));
        assertEquals(0, "aaa".indexOf("aa", -1));
        assertEquals(0, "aaa".indexOf(""));
        assertEquals(3, "aaa".indexOf("", 3));
        assertEquals(3, "aaa".indexOf("", 4));
        assertEquals(3, "aaa".lastIndexOf(""));
        assertEquals(0, "aaa".lastIndexOf("", 0));
        assertEquals(-1, "aaa".lastIndexOf("", -1));
        assertEquals(-1, "aaa".lastIndexOf("a", -1));

        assertEquals(5, Index.lastOfSubArray(Array.of(1, 2, 3, 4, 5), new int[] {}).orElseThrow());
        assertEquals(0, Index.lastOfSubArray(Array.of(1, 2, 3, 4, 5), 0, new int[] {}).orElseThrow());
    }

    @Test
    public void testIndexOfAny_CharArray() {
        assertEquals(0, Strings.indexOfAny("abc", 'a', 'x'));
        assertEquals(1, Strings.indexOfAny("abc", 'b', 'c'));
        assertEquals(-1, Strings.indexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
    }

    @Test
    public void testIndexOfAny_CharArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, 'a'));
        assertEquals(-1, Strings.indexOfAny("abc", 5, 'a'));
    }

    @Test
    public void testIndexOfAny_StringArray() {
        assertEquals(0, Strings.indexOfAny("abcdef", "abc", "xyz"));
        assertEquals(3, Strings.indexOfAny("abcdef", "def", "xyz"));
        assertEquals(-1, Strings.indexOfAny("abcdef", "xyz", "123"));
        assertEquals(-1, Strings.indexOfAny(null, "abc", "def"));
    }

    @Test
    public void testIndexOfAny_StringArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, "abc"));
        assertEquals(-1, Strings.indexOfAny("abc", 5, "abc"));
    }

    @Test
    public void testIndexOfAny() {
        assertEquals(1, Strings.indexOfAny("hello", 'e', 'x'));
        assertEquals(-1, Strings.indexOfAny("hello", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny((String) null, Array.of('e')));
        assertEquals(-1, Strings.indexOfAny("", Array.of('e')));

        assertEquals(1, Strings.indexOfAny("hello", "ell", "xyz"));
        assertEquals(-1, Strings.indexOfAny("hello", "xyz", "abc"));
    }

    @Test
    public void testIndexOfAnyChars() {
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
        assertEquals(-1, Strings.indexOfAny("", 'a', 'b'));
        assertEquals(0, Strings.indexOfAny("zzabyycdxx", 'z', 'a'));
        assertEquals(3, Strings.indexOfAny("zzabyycdxx", 'b', 'd'));
    }

    @Test
    public void testIndexOfAnyStrings() {
        assertEquals(-1, Strings.indexOfAny(null, "ab", "cd"));
        assertEquals(-1, Strings.indexOfAny("", "ab", "cd"));
        assertEquals(2, Strings.indexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(6, Strings.indexOfAny("zzabyycdxx", "cd", "ef"));
    }

    @Test
    public void testIndexOfAny_WithFromIndex() {
        // basic found
        assertEquals(2, Strings.indexOfAny("hello", 0, 'l', 'z'));

        // fromIndex > 0 skips first occurrence
        assertEquals(3, Strings.indexOfAny("hello", 3, 'l', 'z'));

        // not found
        assertEquals(-1, Strings.indexOfAny("hello", 0, 'z', 'q'));

        // null/empty str
        assertEquals(-1, Strings.indexOfAny(null, 0, 'a'));
        assertEquals(-1, Strings.indexOfAny("", 0, 'a'));

        // empty valuesToFind
        assertEquals(-1, Strings.indexOfAny("hello", 0, new char[0]));

        // many chars (> 3) path - large char set
        assertEquals(0, Strings.indexOfAny("hello world", 0, 'h', 'e', 'l', 'o', ' '));
        assertEquals(-1, Strings.indexOfAny("hello world", 0, 'z', 'q', 'x', 'y', 'v'));
    }

    @Test
    public void testIndexOfAnyBut() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 'a'));
        assertEquals(0, Strings.indexOfAnyBut("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 'a'));
        assertEquals(-1, Strings.indexOfAnyBut(null, 'a'));
    }

    @Test
    public void testIndexOfAnyBut_WithFromIndex() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 3, 'a'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 0, 'a'));
        assertEquals(2, Strings.indexOfAnyBut("abc", 2));
        assertEquals(-1, Strings.indexOfAnyBut("abc", 3));
    }

    @Test
    public void testIndexOfIgnoreCase() {
        assertEquals(0, Strings.indexOfIgnoreCase("AbCdEf", "abc"));
        assertEquals(3, Strings.indexOfIgnoreCase("abcDEF", "def"));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "abc"));
    }

    @Test
    public void testIndexOfIgnoreCase_sameLengthCompareNotCaseFold() {
        // regionMatches(true) is a same-length UTF-16 compare, not Unicode case folding.
        assertEquals(-1, Strings.indexOfIgnoreCase("stra\u00dfe", "SS"));
        assertEquals(-1, Strings.indexOfIgnoreCase("SS", "\u00df"));
        assertFalse(Strings.containsIgnoreCase("stra\u00dfe", "SS"));
        assertEquals(0, Strings.countMatchesIgnoreCase("stra\u00dfe", "SS"));
        assertEquals("stra\u00dfe", Strings.replaceIgnoreCase("stra\u00dfe", 0, "SS", "ss", -1));
        // ASCII same-length ignore-case still matches.
        assertEquals(0, Strings.indexOfIgnoreCase("SS", "ss"));
    }

    @Test
    public void testIndexOfIgnoreCase_LargeKmpPathMatchesRegionMatches() {
        final String adversarialTarget = "a".repeat(256) + "b";
        final String adversarialInput = "a".repeat(5000) + "B";
        assertEquals(4744, Strings.indexOfIgnoreCase(adversarialInput, adversarialTarget));
        assertEquals(4744, Strings.lastIndexOfIgnoreCase(adversarialInput, adversarialTarget));
        assertEquals(-1, Strings.indexOfIgnoreCase(adversarialInput, "a".repeat(256) + "c"));

        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        final String supplementaryInput = "x".repeat(300) + deseretCapital.repeat(20);
        final String supplementaryTarget = deseretSmall.repeat(20);
        assertEquals(indexOfIgnoreCaseReference(supplementaryInput, supplementaryTarget, 0),
                Strings.indexOfIgnoreCase(supplementaryInput, supplementaryTarget));
        assertEquals(lastIndexOfIgnoreCaseReference(supplementaryInput, supplementaryTarget, supplementaryInput.length()),
                Strings.lastIndexOfIgnoreCase(supplementaryInput, supplementaryTarget));

        final char[] alphabet = { 'I', 'i', '\u0130', '\u0131', '\u03A3', '\u03C3', '\u03C2', '\u212A', 'K', 'k', 'x', '\uD800', '\uDC00' };
        final Random random = new Random(20260821L);

        for (int iteration = 0; iteration < 100; iteration++) {
            final StringBuilder input = new StringBuilder(320);
            final StringBuilder target = new StringBuilder(20);

            for (int i = 0; i < 320; i++) {
                input.append(alphabet[random.nextInt(alphabet.length)]);
            }

            for (int i = 0; i < 20; i++) {
                target.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final int fromIndex = random.nextInt(340) - 10;
            final int startIndexFromBack = random.nextInt(340) - 10;
            assertEquals(indexOfIgnoreCaseReference(input.toString(), target.toString(), fromIndex),
                    Strings.indexOfIgnoreCase(input.toString(), target.toString(), fromIndex));
            assertEquals(lastIndexOfIgnoreCaseReference(input.toString(), target.toString(), startIndexFromBack),
                    Strings.lastIndexOfIgnoreCase(input.toString(), target.toString(), startIndexFromBack));
        }
    }

    @Test
    public void testIndexOfIgnoreCase_WithFromIndex() {
        assertEquals(3, Strings.indexOfIgnoreCase("AbCaBc", "abc", 1));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "abc", 5));
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiter() {
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ","));
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "APPLE", ","));
        Assertions.assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineapple,apple", "apple", ","));
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("test", "test", ","));

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "test", ","));
        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("test", null, ","));

        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("test", "test", ""));

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("apple banana", "apple", ","));
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiterAndFromIndex() {
        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 0));
        Assertions.assertEquals(6, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 5));
        Assertions.assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineapple,apple", "apple", ",", 0));
        Assertions.assertEquals(11, StrUtil.indexOfTokenIgnoreCase("test value test", "test", " ", 5));

        Assertions.assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,banana", "apple", ",", -5));

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("apple", "apple", ",", 10));
    }

    @Test
    public void testIndexOfIgnoreCaseComplexScenarios() {
        Assertions.assertEquals(7, StrUtil.indexOfTokenIgnoreCase("apple, APPLE, banana", "apple", ", ", 1));

        Assertions.assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("test,value", "st,va", ","));

        Assertions.assertEquals(1, StrUtil.indexOfTokenIgnoreCase(",apple,", "apple", ","));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiter() {
        int result = StrUtil.indexOfTokenIgnoreCase("a,B,c", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "b", ","));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiterAndFromIndex() {
        int result = StrUtil.indexOfTokenIgnoreCase("a,B,c,b", "b", ",", 3);
        assertTrue(result >= 0);

        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "b", ",", 0));
    }

    @Test
    public void testIndexOfIgnoreCase_WithFromIndex_FullCoverage() {
        // basic found
        assertEquals(6, Strings.indexOfIgnoreCase("hello WORLD", "world", 0));
        assertEquals(6, Strings.indexOfIgnoreCase("hello WORLD", "WORLD", 3));

        // null inputs
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "world", 0));
        assertEquals(-1, Strings.indexOfIgnoreCase("hello", null, 0));

        // fromIndex beyond length
        assertEquals(-1, Strings.indexOfIgnoreCase("hello", "ell", 3));

        // negative fromIndex treated as 0
        assertEquals(1, Strings.indexOfIgnoreCase("hello", "ELL", -5));

        // short string optimization path (<= 3 chars, case-insensitive same)
        assertEquals(1, Strings.indexOfIgnoreCase("hello", "ell", 0));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiterAndFromIndex_FullCoverage() {
        // null delimiter (delegates to no-delimiter version)
        assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "APPLE", (String) null, 0));

        // empty delimiter
        assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "APPLE", "", 0));

        // with delimiter, found at start
        assertEquals(0, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 0));

        // with delimiter, found at end
        assertEquals(10, StrUtil.indexOfTokenIgnoreCase("pineapple,apple", "apple", ",", 0));

        // null source
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase(null, "apple", ",", 0));

        // null valueToFind
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("apple", null, ",", 0));

        // not found
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("hello world", "test", ",", 0));

        // with fromIndex
        assertEquals(6, StrUtil.indexOfTokenIgnoreCase("apple,APPLE,banana", "apple", ",", 3));

        // targetLen < substrLen
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("ab", "longer-string", ",", 0));
    }

    @Test
    public void testIndexOfDifference_Array() {
        assertEquals(3, Strings.indexOfDifference("abcxyz", "abcdef", "abc123"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc", "abc"));
    }

    @Test
    public void testIndexOfDifference() {
        assertEquals(3, Strings.indexOfDifference("abcxyz", "abcdef"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(0, Strings.indexOfDifference(null, "abc"));
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(-1, Strings.indexOfDifference(null, ""));
    }

    @Test
    public void testIndexOfDifferenceTwoStrings() {
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(0, Strings.indexOfDifference("", "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", ""));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(2, Strings.indexOfDifference("abcde", "abxyz"));
        assertEquals(0, Strings.indexOfDifference("abcde", "xyz"));
    }

    @Test
    public void testIndexOfDifferenceVarArgs() {
        assertEquals(-1, Strings.indexOfDifference());
        assertEquals(-1, Strings.indexOfDifference((String[]) null));
        assertEquals(-1, Strings.indexOfDifference("abc"));
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(0, Strings.indexOfDifference(null, null, "abc"));
        assertEquals(0, Strings.indexOfDifference("", "abc"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(1, Strings.indexOfDifference("abc", "a"));
        assertEquals(2, Strings.indexOfDifference("ab", "abxyz"));
        assertEquals(7, Strings.indexOfDifference("i am a machine", "i am a robot"));
    }

    @Test
    public void testIndexOfDifference_EdgeCases() {
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(0, Strings.indexOfDifference(null, "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", null));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(2, Strings.indexOfDifference("abc", "abx"));
        assertEquals(3, Strings.indexOfDifference("abc", "abcd"));
    }

    @Test
    public void testIndexOfDifference_VarArgs_EdgeCases() {
        assertEquals(-1, Strings.indexOfDifference("abc", "abc", "abc"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz", "abc"));
        assertEquals(1, Strings.indexOfDifference("abc", "axc"));
        assertEquals(-1, Strings.indexOfDifference((String[]) null));
        assertEquals(-1, Strings.indexOfDifference());
    }

    @Test
    public void testIndexOf_EmptyPattern() {
        // Mirrors String#indexOf semantics: empty pattern is found at index 0.
        assertEquals(0, Strings.indexOf("hello", ""));
        assertEquals(0, Strings.indexOf("", ""));
        // Null pattern -> NOT_FOUND.
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOf("hello", (String) null));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOf((String) null, ""));
    }

    @Test
    public void testIndexOfDifference_EmptyArrays() {
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference((String[]) null));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[0]));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[] { "abc" }));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[] { null, null }));
        assertEquals(CommonUtil.INDEX_NOT_FOUND, Strings.indexOfDifference(new String[] { "", "" }));
    }

    @Test
    public void testIndexOfWithDelimiter_terminalTokenAfterNonTerminalOccurrence() {
        // regression: only the FIRST "delimiter + value" occurrence was tested against end-of-string,
        // so a valid terminal token shadowed by an earlier non-terminal one was missed
        assertEquals(8, StrUtil.indexOfToken("xab,abx,ab", "ab", ","));
        assertEquals(8, StrUtil.indexOfTokenIgnoreCase("xab,abx,ab", "AB", ","));
        assertTrue(StrUtil.containsToken("xab,abx,ab", "ab", ","));
        assertTrue(StrUtil.containsTokenIgnoreCase("xab,abx,ab", "AB", ","));

        // unchanged behavior
        assertEquals(8, StrUtil.indexOfToken("xbanana,banana", "banana", ","));
        assertEquals(-1, StrUtil.indexOfToken("xab,abx", "ab", ","));
    }

    @Test
    public void testIndexOfDifference_Varargs() {
        assertEquals(7, Strings.indexOfDifference("i am a machine", "i am a robot", "i am a maniac"));
        assertEquals(1, Strings.indexOfDifference("abc", "abc", "a"));
    }
    /**
     * Contract pin for the "Passing exactly one character" note on {@code indexOfAny(String, char...)}: all four
     * overloads exist, so BOTH {@code char} overloads take part in the ambiguity and a single {@code char} has to be
     * passed as an explicit array. {@code indexOfAnyBut} has no {@code String} overloads and needs no workaround.
     */
    @Test
    public void testIndexOfAny_SingleCharacterNeedsExplicitArray() {
        // the documented workarounds
        assertEquals(0, Strings.indexOfAny("hello", new char[] { 'h' }));
        assertEquals(2, Strings.indexOf("hello", 'l'));
        // two or more characters resolve normally
        assertEquals(1, Strings.indexOfAny("hello", 'e', 'o'));
        // indexOfAnyBut has no String overloads, so a single char compiles there
        assertEquals(1, Strings.indexOfAnyBut("hello", 'h'));
        assertEquals(3, Strings.lastIndexOfAny("hello", new char[] { 'l' }));
        assertEquals(4, Strings.minIndexOfAll("hello", new char[] { 'o' }));
        assertEquals(2, Strings.maxIndexOfAll("hello", new char[] { 'l' }));
        assertEquals(3, Strings.minLastIndexOfAll("hello", new char[] { 'l' }));
        assertEquals(3, Strings.maxLastIndexOfAll("hello", new char[] { 'l' }));

        // the overload set the note describes: both char overloads exist alongside both String ones
        for (final String name : new String[] { "indexOfAny", "lastIndexOfAny", "minIndexOfAll", "maxIndexOfAll", "minLastIndexOfAll",
                "maxLastIndexOfAll" }) {
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, char[].class), name);
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, int.class, char[].class), name);
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, String[].class), name);
            Assertions.assertDoesNotThrow(() -> Strings.class.getMethod(name, String.class, int.class, String[].class), name);
        }
        Assertions.assertThrows(NoSuchMethodException.class, () -> Strings.class.getMethod("indexOfAnyBut", String.class, String[].class));
        Assertions.assertThrows(NoSuchMethodException.class,
                () -> Strings.class.getMethod("indexOfAnyBut", String.class, int.class, String[].class));
    }

    /**
     * Contract pin for the delimiter-in-token rule now documented on the four {@code indexOfToken} javadocs: with a
     * NON-EMPTY delimiter, a token that itself contains the delimiter spans more than one field and can never match,
     * and the case-insensitive overloads apply that containment test case-insensitively.
     */
    @Test
    public void testIndexOfToken_TokenContainingDelimiterNeverMatches() {
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfToken("x,a,b,y", "a,b", ",", 0));
        assertEquals(-1, StrUtil.indexOfToken("a,b", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfToken("a,b", "a,b", ",", 0));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "a,b", ","));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "A,B", ",", 0));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("a,b", "a,b", ",", 0));
        assertFalse(StrUtil.containsToken("x,a,b,y", "a,b", ","));
        // a plain search does find it
        assertEquals(2, Strings.indexOf("x,a,b,y", "a,b"));

        // the rule is scoped to a non-empty delimiter: an empty or null delimiter delegates to a plain search
        assertEquals(2, StrUtil.indexOfToken("x,a,b,y", "a,b", ""));
        assertEquals(2, StrUtil.indexOfToken("x,a,b,y", "a,b", null));
        assertEquals(2, StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "A,B", ""));
        assertEquals(2, StrUtil.indexOfTokenIgnoreCase("x,a,b,y", "A,B", null));

        // the containment test of the IgnoreCase overloads is itself case-insensitive
        assertEquals(4, StrUtil.indexOfToken("pANDxandyANDq", "xandy", "AND"));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND"));
        assertEquals(4, StrUtil.indexOfToken("pANDxandyANDq", "xandy", "AND", 0));
        assertEquals(-1, StrUtil.indexOfTokenIgnoreCase("pANDxandyANDq", "xandy", "AND", 0));
    }
}
