package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.substring;
import static com.landawn.abacus.util.Strings.substringAfter;
import static com.landawn.abacus.util.Strings.substringAfterAny;
import static com.landawn.abacus.util.Strings.substringAfterLast;
import static com.landawn.abacus.util.Strings.substringBefore;
import static com.landawn.abacus.util.Strings.substringBeforeAny;
import static com.landawn.abacus.util.Strings.substringBeforeLast;
import static com.landawn.abacus.util.Strings.substringBetween;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import java.util.function.IntUnaryOperator;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.u.Optional;

/** Strings substring contracts, including regression cases. */
public class StringsSubstringTest extends StringsTestSupport {

    protected static String longestCommonSubstringReference(final CharSequence a, final CharSequence b) {
        final int[] codePointsA = a.toString().codePoints().toArray();
        final int[] codePointsB = b.toString().codePoints().toArray();
        final int[] matches = new int[codePointsB.length + 1];
        int earliestEndInA = 0;
        int maxLength = 0;

        for (int i = 1; i <= codePointsA.length; i++) {
            for (int j = codePointsB.length; j > 0; j--) {
                if (codePointsA[i - 1] == codePointsB[j - 1]) {
                    final int matchLength = matches[j] = matches[j - 1] + 1;

                    if (matchLength > maxLength || matchLength == maxLength && i < earliestEndInA) {
                        maxLength = matchLength;
                        earliestEndInA = i;
                    }
                } else {
                    matches[j] = 0;
                }
            }
        }

        return maxLength == 0 ? "" : new String(codePointsA, earliestEndInA - maxLength, maxLength);
    }

    protected static String randomCodePointString(final Random random, final int length) {
        final int[] alphabet = { 'a', 'b', 'c', 0x0301, 0x034F, 0x03A3, 0x03C2, 0x10400, 0x10428, 0x1F600, 0x1F601, 0xD800, 0xDC00 };
        final StringBuilder result = new StringBuilder(length * 2);

        for (int i = 0; i < length; i++) {
            result.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
        }

        return result.toString();
    }

    @Test
    public void test_between() {
        {
            String ret = Strings.substringBetween(null, null);
            assertEquals(null, ret);

            ret = Strings.substringBetween("", null);
            assertEquals(null, ret);

            ret = Strings.substringBetween(null, "");
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "c");
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "a");
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "b");
            assertEquals("", ret);

            ret = Strings.substringBetween("aab", "a", "b");
            assertEquals("a", ret);

            ret = Strings.substringBetween("aab", "a", "a");
            assertEquals("", ret);
        }
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
    public void test_lcs() {
        final String a = "0878e121-b14c-4a77-aef1-ec46f191d593";
        final String b = "b2b61159-491f-401b-bc17-08dd0a21e241";
        final String c = "zxabcdezy";
        final String d = "yzabcdezx";

        assertEquals("abcdez", Strings.longestCommonSubstring(c, d));
        assertEquals("08", Strings.longestCommonSubstring(a, b));
        assertEquals("11-14c7-a1e41", ListUtils.longestCommonSubsequence(a, b));
    }

    @Test
    public void test_lcs_02() {
        assertEquals("abcdez", Strings.longestCommonSubstring("zxabcdezy", "yzabcdezx"));
        assertEquals("", Strings.longestCommonSubstring("", ""));
        assertEquals("a", Strings.longestCommonSubstring("abc", "zxa"));
        assertEquals("Geeks", Strings.longestCommonSubstring("GeeksforGeeks", "GeeksQuiz"));
        assertEquals("xddd", Strings.longestCommonSubstring("xddddd", "ddxxddda"));
        assertEquals("ddddd", Strings.longestCommonSubstring("xddddd", "ddxxdddaddddd"));
        assertEquals("", Strings.longestCommonSubstring("ddddddddd", "eeeeeeeeee"));
        assertEquals("", Strings.longestCommonSubstring("", ""));
    }

    // longestCommonSubstring

    @Test
    public void testLongestCommonSubstring() {
        assertEquals("abc", Strings.longestCommonSubstring("xabcy", "zabc123"));
        assertEquals("", Strings.longestCommonSubstring("abc", "xyz"));
        assertNull(Strings.longestCommonSubstring(null, "abc"));

        // The shorter first input is used as the DP row, while ties still prefer the match ending earliest in it.
        assertEquals("abab", Strings.longestCommonSubstring("ababa", "xxababyybabazz"));
        assertEquals("abc", Strings.longestCommonSubstring("abc", "x".repeat(100) + "abc"));
    }

    @Test
    public void testLongestCommonSubstring_DoesNotSplitSurrogatePairs() {
        final String grinningFace = new String(Character.toChars(0x1F600));
        final String beamingFace = new String(Character.toChars(0x1F601));

        assertEquals(grinningFace, Strings.longestCommonSubstring("a" + grinningFace + "b", "x" + grinningFace + "y"));
        assertEquals("", Strings.longestCommonSubstring(grinningFace, beamingFace));
    }

    @Test
    public void testLongestCommonSubstring_HybridBoundaryTieAndUnicodeDifferential() throws ReflectiveOperationException {
        final Random random = new Random(20260827L);

        for (int iteration = 0; iteration < 300; iteration++) {
            final String a = randomCodePointString(random, random.nextInt(45));
            final String b = randomCodePointString(random, random.nextInt(45));
            assertEquals(longestCommonSubstringReference(a, b), Strings.longestCommonSubstring(a, b), "iteration " + iteration);
            assertEquals(longestCommonSubstringReference(b, a), Strings.longestCommonSubstring(new StringBuilder(b), new StringBuilder(a)),
                    "reversed iteration " + iteration);
        }

        final String belowThresholdA = randomString(random, "abcdefg".toCharArray(), 511);
        final String belowThresholdB = randomString(random, "abcdefg".toCharArray(), 513); // 511 * 513 == 262,143
        assertEquals(longestCommonSubstringReference(belowThresholdA, belowThresholdB), Strings.longestCommonSubstring(belowThresholdA, belowThresholdB));

        final String atThresholdA = randomString(random, "abcdefg".toCharArray(), 512);
        final String atThresholdB = randomString(random, "abcdefg".toCharArray(), 512);
        assertEquals(longestCommonSubstringReference(atThresholdA, atThresholdB), Strings.longestCommonSubstring(atThresholdA, atThresholdB));

        final String tieA = "WXYZ" + "a".repeat(510) + "ABCD";
        final String tieB = "ABCD" + "q".repeat(510) + "WXYZ";
        assertEquals("WXYZ", Strings.longestCommonSubstring(tieA, tieB));

        final String imbalanced = "a".repeat(65_536) + "wxyz";
        assertEquals("wxyz", Strings.longestCommonSubstring("wxyz", imbalanced));
        assertEquals("wxyz", Strings.longestCommonSubstring(imbalanced, "wxyz"));
        assertEquals("", Strings.longestCommonSubstring("", imbalanced));

        final Method crossover = Strings.class.getDeclaredMethod("shouldUseLongestCommonSubstringSuffixArray", int.class, int.class);
        crossover.setAccessible(true);
        assertFalse((boolean) crossover.invoke(null, 511, 513));
        assertTrue((boolean) crossover.invoke(null, 512, 512));
        assertFalse((boolean) crossover.invoke(null, 4, 65_536));
    }

    @Test
    public void testLongestCommonSubstring_LargeSuffixArrayPathMatchesDynamicProgramming() {
        final Random random = new Random(20260820L);

        for (int iteration = 0; iteration < 4; iteration++) {
            final StringBuilder a = new StringBuilder(1000);
            final StringBuilder b = new StringBuilder(1000);

            for (int i = 0; i < 1000; i++) {
                a.append((char) ('a' + random.nextInt(7)));
                b.append((char) ('a' + random.nextInt(7)));
            }

            assertEquals(longestCommonSubstringReference(a.toString(), b.toString()), Strings.longestCommonSubstring(a, b));
        }

        final String repeated = "ab".repeat(256);
        assertEquals(repeated, Strings.longestCommonSubstring(repeated, repeated));

        final String emojiA = new String(Character.toChars(0x1F600));
        final String emojiB = new String(Character.toChars(0x1F601));
        final String a = ("a" + emojiA + "b" + emojiB).repeat(250);
        final String b = ("x" + emojiA + "b" + emojiB).repeat(250);
        assertEquals(longestCommonSubstringReference(a, b), Strings.longestCommonSubstring(a, b));
    }

    @Test
    public void testLongestCommonSubstring_SuffixArrayInternalPathExhaustive() throws ReflectiveOperationException {
        final Method suffixArrayMatch = Strings.class.getDeclaredMethod("longestCommonSubstringMatchSuffixArray", int[].class, int.class, int.class);
        suffixArrayMatch.setAccessible(true);
        final List<String> inputs = shortStrings(new char[] { 'a', 'b', 'c' }, 4);

        for (final String a : inputs) {
            for (final String b : inputs) {
                final int[] combined = new int[a.length() + b.length() + 2];

                for (int i = 0; i < a.length(); i++) {
                    combined[i] = a.charAt(i) + 2;
                }

                combined[a.length()] = 1;

                for (int i = 0; i < b.length(); i++) {
                    combined[a.length() + i + 1] = b.charAt(i) + 2;
                }

                final int[] match = (int[]) suffixArrayMatch.invoke(null, combined, a.length(), b.length());
                final String actual = match[1] == 0 ? "" : a.substring(match[0] - match[1], match[0]);
                assertEquals(longestCommonSubstringReference(a, b), actual, "a=" + a + ", b=" + b);
            }
        }

        final Random random = new Random(20260829L);

        for (int iteration = 0; iteration < 16; iteration++) {
            final String a = randomString(random, "abcde".toCharArray(), 512 + iteration % 2);
            final String b = randomString(random, "abcde".toCharArray(), 512 + (iteration + 1) % 2);
            assertEquals(longestCommonSubstringReference(a, b), Strings.longestCommonSubstring(a, b), "iteration=" + iteration);
        }
    }

    @Test
    public void testLongestCommonSubstring_RejectsImpracticalDynamicProgrammingFallback() throws ReflectiveOperationException {
        final Method workGuard = Strings.class.getDeclaredMethod("checkLongestCommonSubstringFallbackWork", int.class, int.class);
        workGuard.setAccessible(true);
        final int largeLength = CommonUtil.MAX_ARRAY_SIZE / 2 + 1;

        final java.lang.reflect.InvocationTargetException exception = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> workGuard.invoke(null, largeLength, largeLength));
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertDoesNotThrow(() -> workGuard.invoke(null, 0, CommonUtil.MAX_ARRAY_SIZE));
    }

    @Test
    public void testReplaceAfter() {
        assertEquals("abc:REPLACED", Strings.replaceAfter("abc:old", ":", "REPLACED"));
        assertNull(Strings.replaceAfter(null, ":", "X"));
    }

    @Test
    public void testReplaceAfter_EdgeCases() {
        assertNull(Strings.replaceAfter(null, " ", "x"));
        assertEquals("", Strings.replaceAfter("", " ", "x"));
        assertEquals("hello x", Strings.replaceAfter("hello world", " ", "x"));
    }

    @Test
    public void testReplaceBefore() {
        assertEquals("REPLACED:xyz", Strings.replaceBefore("old:xyz", ":", "REPLACED"));
        assertNull(Strings.replaceBefore(null, ":", "X"));
    }

    @Test
    public void testReplaceBefore_EdgeCases() {
        assertNull(Strings.replaceBefore(null, " ", "x"));
        assertEquals("", Strings.replaceBefore("", " ", "x"));
        assertEquals("x world", Strings.replaceBefore("hello world", " ", "x"));
    }

    // replaceBetween

    @Test
    public void testReplaceBetween() {
        assertEquals("abc[REPLACED]xyz", Strings.replaceBetween("abc[old]xyz", "[", "]", "REPLACED"));
        assertNull(Strings.replaceBetween(null, "[", "]", "X"));
    }

    @Test
    public void testReplaceBetween_EdgeCases() {
        assertNull(Strings.replaceBetween(null, "a", "b", "x"));
        assertEquals("", Strings.replaceBetween("", "a", "b", "x"));
        assertEquals("axb", Strings.replaceBetween("acb", "a", "b", "x"));
    }

    @Test
    public void testReplaceBetween_EndDelimiterNotFound() {
        assertEquals("hello[world", Strings.replaceBetween("hello[world", "[", "]", "X"));
        assertEquals("start", Strings.replaceBetween("start", "<", ">", "replacement"));
    }



    /**
     * C-022: {@code StrUtil.substringAfterAny} answers an empty {@code Optional} for a {@code null} or empty
     * delimiter array, and tries delimiters in ARGUMENT order rather than by their position in the string.
     * Neither was documented on these two wrappers, though both {@code substringBeforeAny} twins document them.
     */
    @Test
    public void test20260906_strUtilSubstringAfterAnyNullDelimitersAndArgumentOrder() {
        assertFalse(Strings.StrUtil.substringAfterAny("hello", (char[]) null).isPresent());
        assertFalse(Strings.StrUtil.substringAfterAny("hello", new char[0]).isPresent());
        assertFalse(Strings.StrUtil.substringAfterAny("hello", (String[]) null).isPresent());
        assertFalse(Strings.StrUtil.substringAfterAny("hello", new String[0]).isPresent());

        // argument order decides, even though ':' occurs earlier in the string than '.'
        assertEquals("c", Strings.StrUtil.substringAfterAny("a:b.c", '.', ':').get());
        assertEquals("b.c", Strings.StrUtil.substringAfterAny("a:b.c", ':', '.').get());
        assertEquals("c", Strings.StrUtil.substringAfterAny("a:b.c", ".", ":").get());
        assertEquals("b.c", Strings.StrUtil.substringAfterAny("a:b.c", ":", ".").get());
    }

    @Test
    public void testStrUtil_SubstringOrElseItselfMethods() {
        assertEquals("llo World", StrUtil.substringAfterOrElseItself("Hello World", 'e'));
        assertEquals("Hello", StrUtil.substringAfterOrElseItself("Hello", 'x'));

        assertEquals("World", StrUtil.substringAfterOrElseItself("Hello World", "Hello "));
        assertEquals("Hello", StrUtil.substringAfterOrElseItself("Hello", "xyz"));

        assertEquals("Wo", StrUtil.substringAfterOrElseItself("Hello World", "Hello ", 8));
        assertEquals("Hello World", StrUtil.substringAfterOrElseItself("Hello World", "World", 8));

        assertEquals("txt", StrUtil.substringAfterLastOrElseItself("file.name.txt", '.'));
        assertEquals("Hello", StrUtil.substringAfterLastOrElseItself("Hello", 'x'));

        assertEquals("txt", StrUtil.substringAfterLastOrElseItself("file.name.txt", "."));
        assertEquals("Hello", StrUtil.substringAfterLastOrElseItself("Hello", "xyz"));

        assertEquals("name", StrUtil.substringAfterLastOrElseItself("file.name.txt", ".", 9));
        assertEquals("file.name.txt", StrUtil.substringAfterLastOrElseItself("file.name.txt", ".", 3));

        assertEquals("He", StrUtil.substringBeforeOrElseItself("Hello World", 'l'));
        assertEquals("Hello", StrUtil.substringBeforeOrElseItself("Hello", 'x'));

        assertEquals("Hello", StrUtil.substringBeforeOrElseItself("Hello World", " World"));
        assertEquals("Hello", StrUtil.substringBeforeOrElseItself("Hello", "xyz"));

        assertEquals("lo", StrUtil.substringBeforeOrElseItself("Hello World", 3, " World"));
        assertEquals("Hello World", StrUtil.substringBeforeOrElseItself("Hello World", 7, " "));

        assertEquals("file.name", StrUtil.substringBeforeLastOrElseItself("file.name.txt", '.'));
        assertEquals("Hello", StrUtil.substringBeforeLastOrElseItself("Hello", 'x'));

        assertEquals("file.name", StrUtil.substringBeforeLastOrElseItself("file.name.txt", "."));
        assertEquals("Hello", StrUtil.substringBeforeLastOrElseItself("Hello", "xyz"));

        assertEquals("le.name", StrUtil.substringBeforeLastOrElseItself("file.name.txt", 2, "."));
        assertEquals("file.name.txt", StrUtil.substringBeforeLastOrElseItself("file.name.txt", 10, "."));
    }

    @Test
    public void testStrUtil_SubstringOrElseMethods() {
        assertEquals("World", StrUtil.substringAfterOrElse("Hello World", "Hello ", "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("Hello", "xyz", "default"));

        assertEquals("txt", StrUtil.substringAfterLastOrElse("file.name.txt", ".", "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("Hello", "xyz", "default"));

        assertEquals("Hello", StrUtil.substringBeforeOrElse("Hello World", " World", "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("Hello", "xyz", "default"));

        assertEquals("file.name", StrUtil.substringBeforeLastOrElse("file.name.txt", ".", "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("Hello", "xyz", "default"));
    }

    // substring

    @Test
    public void testStrUtilSubstring() {
        assertTrue(StrUtil.substring("abc", 1).isPresent());
        assertEquals("bc", StrUtil.substring("abc", 1).get());
        assertFalse(StrUtil.substring("abc", 4).isPresent());
        assertFalse(StrUtil.substring(null, 1).isPresent());

        assertTrue(StrUtil.substring("abc", 1, 2).isPresent());
        assertEquals("b", StrUtil.substring("abc", 1, 2).get());
        assertFalse(StrUtil.substring("abc", 2, 1).isPresent());
    }

    @Test
    public void testStrUtilSubstringOptional() {
        assertTrue(StrUtil.substring("abc", 1).isPresent());
        assertEquals("bc", StrUtil.substring("abc", 1).get());
        assertFalse(StrUtil.substring("abc", 5).isPresent());
    }

    @Test
    public void testSubstring() {
        assertEquals("cde", Strings.substring("abcde", 2));
        assertNull(Strings.substring("abc", 5));
        assertNull(Strings.substring(null, 1));
    }

    @Test
    public void test_substring_01() {
        assertEquals(0, "abc".indexOf(""));
        assertEquals(-1, "abc".indexOf("c", 3));
        assertEquals(-1, "abc".indexOf("c", 10));
        assertEquals("", "abc".substring(3));
        assertEquals("", Strings.substringBetween("bab", "", ""));
        assertEquals("b", Strings.substringBetween("bab", "", "a"));
        assertEquals("", Strings.substringBetween("bab", "a", ""));
        assertEquals("", Strings.substringBetween("", "", ""));
        assertEquals("a", Strings.substringBetween("bab", 0, "b"));
        assertEquals("", Strings.substringBetween("bab", 0, ""));
        assertEquals("b", Strings.substringBetween("bab", "", 1));
        assertEquals("", "abc".substring(0, 0));
        assertEquals("", "abc".substring(2, 2));
        assertEquals("", "abc".substring(3, 3));

    }

    @Test
    public void testSubstring_BeginEndIndex() {
        assertEquals("bcd", Strings.substring("abcdef", 1, 4));
        assertEquals("abc", Strings.substring("abcdef", 0, 3));
        assertEquals("", Strings.substring("abcdef", 2, 2));
        assertNull(Strings.substring(null, 0, 3));
    }

    @Test
    public void testSubstring_BeginIndex() {
        assertEquals("def", Strings.substring("abcdef", 3));
        assertEquals("abcdef", Strings.substring("abcdef", 0));
        assertEquals("", Strings.substring("abcdef", 6));
        assertNull(Strings.substring(null, 0));
    }

    @Test
    public void testSubstring_Int_IntUnaryOperator() {
        // substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex)
        assertEquals("Hello", Strings.substring("Hello World", 0, i -> i + 5));
        assertEquals("World", Strings.substring("Hello World", 6, i -> i + 5));
        assertEquals("", Strings.substring("Hello", 5, i -> i));
        assertNull(Strings.substring(null, 0, i -> 5));
        assertNull(Strings.substring("Hello", -1, i -> 5));
        assertNull(Strings.substring("Hello", 6, i -> i + 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.substring("Hello", 6, (IntUnaryOperator) null));

        final int[] calls = { 0 };
        assertEquals("", Strings.substring("Hello", 5, i -> {
            calls[0]++;
            return i;
        }));
        assertEquals(1, calls[0]);
        assertNull(Strings.substring("Hello", 6, i -> {
            calls[0]++;
            return i + 3;
        }));
        assertEquals(1, calls[0]);
        assertNull(Strings.substring(null, 0, i -> {
            calls[0]++;
            return 5;
        }));
        assertEquals(1, calls[0]);
        assertNull(Strings.substring("Hello", -1, i -> {
            calls[0]++;
            return 5;
        }));
        assertEquals(1, calls[0]);
    }

    @Test
    public void testSubstring_IntAndIntUnaryOperator() {
        // substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex)
        String result = Strings.substring("hello world", 6, endIdx -> endIdx + 2);
        assertNotNull(result);
    }

    @Test
    public void testSubstring_IntUnaryOperatorAndInt() {
        // substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substring("hello world", beginIdx -> 0, 5);
        assertEquals("hello", result);
    }

    @Test
    public void testSubstring_OperatorAgreesWithSubstringBetween() {
        final String str = "Hello World";

        for (final int end : new int[] { 0, 1, 5, 11, 12, 50, Integer.MAX_VALUE }) {
            for (final int back : new int[] { 0, 1, 3, 11, 12 }) {
                final String expected = Strings.substring(str, Math.max(0, Math.min(end, str.length()) - back), end);
                assertEquals(expected, Strings.substring(str, e -> Math.max(0, e - back), end), "end=" + end + " back=" + back);
                // substringBetween's begin is exclusive, so begin - 1 reproduces the same range.
                assertEquals(expected, Strings.substringBetween(str, e -> Math.max(0, e - back) - 1, end), "end=" + end + " back=" + back);
            }
        }
    }

    @Test
    public void testSubstring_OperatorInRangeUnchanged() {
        assertEquals("Wor", Strings.substring("Hello World", i -> i - 3, 9));
        assertEquals("Hello", Strings.substring("Hello World", i -> i - 5, 5));
        assertEquals("Hel", Strings.substring("Hello", i -> 0, 3));
        assertEquals("", Strings.substring("Hello", i -> i, 3));
        assertNull(Strings.substring("Hello", i -> i - 3, -1));
        assertNull(Strings.substring(null, i -> 0, 5));
        assertNull(Strings.substring("Hello", i -> 5, 3));
        assertEquals("", Strings.substring("", i -> 0, 0));
        assertEquals("", Strings.substring("", i -> 0, 7));
    }

    @Test
    public void testSubstring_OperatorNotInvokedWhenShortCircuit() {
        final int[] calls = { 0 };
        final IntUnaryOperator counting = end -> {
            calls[0]++;
            return 0;
        };

        assertNull(Strings.substring(null, counting, 5));
        assertNull(Strings.substring("Hello", counting, -1));
        assertEquals(0, calls[0]);
    }



    // D1 : substring(String, IntUnaryOperator, int) clamps the end before the operator sees it
    @Test
    public void testSubstring_OperatorReceivesClampedEnd() {
        final int[] seen = { -1 };
        final IntUnaryOperator recording = end -> {
            seen[0] = end;
            return end - 2;
        };

        assertEquals("lo", Strings.substring("Hello", recording, 100));
        assertEquals(5, seen[0]);

        assertEquals("lo", Strings.substring("Hello", i -> i - 2, Integer.MAX_VALUE));
        assertEquals("Hello", Strings.substring("Hello", i -> 0, 100));
        assertEquals("", Strings.substring("Hello", i -> i, 100));
        assertNull(Strings.substring("Hello", i -> i + 1, 100)); // begin > clamped end
        assertNull(Strings.substring("Hello", i -> -1, 100)); // negative begin
    }

    @Test
    public void testSubstring_OutOfBoundsReturnsNull() {
        // Negative or out-of-range indices return null instead of throwing.
        assertNull(Strings.substring(null, 0));
        assertNull(Strings.substring("abc", -1));
        assertNull(Strings.substring("abc", 4));
        assertEquals("", Strings.substring("abc", 3));
        // 2-arg: end > length is clamped.
        assertEquals("bc", Strings.substring("abc", 1, 100));
        assertNull(Strings.substring("abc", 2, 1));
    }

    @Test
    public void testSubstring_WithIntUnaryOperator_FuncBegin() {
        // substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substring("hello", len -> 0, 5);
        assertEquals("hello", result);
    }

    @Test
    public void testSubstringBoundaryConditions() {
        Assertions.assertEquals("test", Strings.substring("test", 0, 4));
        Assertions.assertEquals("", Strings.substring("test", 4, 4));
        Assertions.assertEquals("est", Strings.substring("test", 1, 4));

        Assertions.assertNull(Strings.substring("test", 5, 10));
        Assertions.assertNull(Strings.substring("test", -1, 4));
    }

    @Test
    public void testU2001_substringOutOfRangeBeginDivergesFromCommons() {
        assertNull(Strings.substring("Hello", 10));
        assertNull(Strings.substring("", 1));
        assertNull(Strings.substring("Hello", 3, 2));
        assertNull(Strings.substring("Hello", 6, 9));
        assertEquals("", Strings.substring("Hello", 5));
        assertEquals("", Strings.substring("", 0));
        assertEquals("llo", Strings.substring("Hello", 2, 9));

        // Commons Lang returns "" instead of null for begin > length and for begin > end
        assertEquals("", StringUtils.substring("Hello", 10));
        assertEquals("", StringUtils.substring("", 1));
        assertEquals("", StringUtils.substring("Hello", 3, 2));
        assertEquals("", StringUtils.substring("Hello", 6, 9));
        assertEquals("llo", StringUtils.substring("Hello", 2, 9));
    }

    @Test
    public void testSubstringWithEnd() {
        assertEquals("cd", Strings.substring("abcde", 2, 4));
        assertEquals("cde", Strings.substring("abcde", 2, 10));
        assertNull(Strings.substring("abc", 2, 1));
    }

    @Test
    public void testSubstring_WithFunctionOfInclusiveBeginIndex() {
        assertEquals("lo", substring("Hello", i -> i - 2, 5));
        assertEquals("", substring("Hello", i -> i, 3));
        assertNull(substring("Hello", i -> -1, 5));
        assertNull(substring(null, i -> 0, 5));
        assertNull(substring("Hello", i -> 0, -1));
    }

    @Test
    public void testSubstring_WithInclusiveBeginAndExclusiveEndIndex() {
        assertEquals("Hello", substring("Hello World", 0, 5));
        assertEquals("World", substring("Hello World", 6, 11));
        assertEquals("", substring("Hello", 2, 2));
        assertNull(substring("Hello", -1, 5));
        assertNull(substring("Hello", 0, -1));
        assertNull(substring("Hello", 3, 2));
        assertNull(substring(null, 0, 5));
        assertEquals("ello", substring("Hello", 1, 10));
    }

    @Test
    public void testSubstring_WithInclusiveBeginIndex() {
        assertEquals("World", substring("Hello World", 6));
        assertEquals("Hello World", substring("Hello World", 0));
        assertNull(substring("Hello", -1));
        assertNull(substring("Hello", 6));
        assertNull(substring(null, 0));
        assertEquals("", substring("Hello", 5));
    }

    // substringAfter

    @Test
    public void testStrUtil_SubstringAfter() {
        assertEquals("llo World", StrUtil.substringAfter("Hello World", 'e').orElse(null));
        assertFalse(StrUtil.substringAfter("Hello", 'x').isPresent());
        assertFalse(StrUtil.substringAfter(null, 'a').isPresent());

        assertEquals("World", StrUtil.substringAfter("Hello World", "Hello ").orElse(null));
        assertFalse(StrUtil.substringAfter("Hello", "xyz").isPresent());

        assertEquals("Wo", StrUtil.substringAfter("Hello World", "Hello ", 8).orElse(null));
        assertFalse(StrUtil.substringAfter("Hello World", "World", 8).isPresent());
    }

    @Test
    public void testStrUtilSubstringAfter() {
        assertEquals("cde", StrUtil.substringAfter("abcde", "ab").get());
        assertFalse(StrUtil.substringAfter("abc", "x").isPresent());
    }

    @Test
    public void test_substringAfter() {
        final String str = "abc";
        assertEquals(1, str.indexOf("", 1));
        assertEquals(3, str.lastIndexOf(""));
        assertEquals("", Strings.substringAfterLast(str, ""));
        assertEquals("", Strings.substringAfterLast(str, "", 1));
    }

    @Test
    public void testSubstringAfter() {
        assertEquals("def", Strings.substringAfter("abc:def", ':'));
        assertNull(Strings.substringAfter("abc", ':'));
        assertNull(Strings.substringAfter(null, ':'));
    }

    @Test
    public void testSubstringAfter_String() {
        assertEquals("def", Strings.substringAfter("abc::def", "::"));
        assertNull(Strings.substringAfter("abc", "::"));
        assertNull(Strings.substringAfter(null, "::"));
    }

    @Test
    public void testSubstringAfter_StringWithEndIndex_DelimiterEndBeyondEndIndex() {
        // The delimiter starts before the end index but ends after it.
        assertNull(Strings.substringAfter("test", "st", 3));
        assertNull(Strings.substringAfter("abcdef", "cde", 4));
        // Delimiter ending exactly at the end index yields "".
        assertEquals("", Strings.substringAfter("abcdef", "cde", 5));
        assertEquals("f", Strings.substringAfter("abcdef", "cde", 6));
    }

    @Test
    public void testSubstringAfter_WithExclusiveEndIndex() {
        // substringAfter(str, delimiter, exclusiveEndIndex)
        assertEquals("ell", Strings.substringAfter("hello world", "h", 4));
        assertNull(Strings.substringAfter(null, "h", 4));
    }

    @Test
    public void testSubstringAfter_Char() {
        assertEquals("llo World", substringAfter("Hello World", 'e'));
        assertEquals("World", substringAfter("Hello World", ' '));
        assertEquals("", substringAfter("Hello", 'o'));
        assertNull(substringAfter("Hello", 'x'));
        assertNull(substringAfter(null, 'a'));
        assertNull(substringAfter("", 'a'));
    }

    @Test
    public void testSubstringAfter_StringWithEndIndex() {
        assertEquals("Wo", substringAfter("Hello World", "Hello ", 8));
        assertNull(substringAfter("Hello World", "World", 8));
        assertNull(substringAfter(null, "test", 5));
        assertNull(substringAfter("test", null, 5));
        assertNull(substringAfter("test", "test", -1));
        assertEquals("te", substringAfter("test", "", 2));
        assertNull(substringAfter("test", "es", 0));
    }

    // substringAfterAny

    @Test
    public void testStrUtil_SubstringAfterAny() {
        assertEquals("llo World", StrUtil.substringAfterAny("Hello World", 'x', 'e', 'z').orElse(null));
        assertFalse(StrUtil.substringAfterAny("Hello", 'x', 'y', 'z').isPresent());

        assertEquals("World", StrUtil.substringAfterAny("Hello World", "xyz", "Hello ", "abc").orElse(null));
        assertFalse(StrUtil.substringAfterAny("Hello", "xyz", "abc").isPresent());
        assertEquals("test", StrUtil.substringAfterAny("test", null, "").orElseThrow());
    }

    @Test
    public void testSubstringAfterAny_CharArray() {
        assertEquals("def", Strings.substringAfterAny("abcdef", 'c', 'x'));
        assertNull(Strings.substringAfterAny("abc", 'x', 'y'));
        assertNull(Strings.substringAfterAny(null, 'a', 'b'));
    }

    @Test
    public void testSubstringAfterAny_StringArray() {
        assertEquals("def", Strings.substringAfterAny("abc::def", "::", "##"));
        assertNull(Strings.substringAfterAny(null, "::", "##"));
    }

    @Test
    public void testSubstringAfterAny_Chars() {
        assertEquals("llo World", substringAfterAny("Hello World", 'x', 'e', 'z'));
        assertEquals(" World", substringAfterAny("Hello World", 'o', ' '));
        assertNull(substringAfterAny("Hello", 'x', 'y', 'z'));
        assertNull(substringAfterAny(null, 'a', 'b'));
        assertNull(substringAfterAny("test", CommonUtil.EMPTY_CHAR_ARRAY));
    }

    @Test
    public void testSubstringAfterAny_Strings() {
        assertEquals("World", substringAfterAny("Hello World", "xyz", "Hello ", "abc"));
        assertEquals(" World", substringAfterAny("Hello World", "abc", "Hello", "xyz"));
        assertNull(substringAfterAny("Hello", "xyz", "abc"));
        assertNull(substringAfterAny(null, "test", "abc"));
        assertNull(substringAfterAny("test", CommonUtil.EMPTY_STRING_ARRAY));
        assertEquals("test", substringAfterAny("test", null, ""));
    }

    // substringAfterLast

    @Test
    public void testStrUtil_SubstringAfterLast() {
        assertEquals("txt", StrUtil.substringAfterLast("file.name.txt", '.').orElse(null));
        assertFalse(StrUtil.substringAfterLast("Hello", 'x').isPresent());

        assertEquals("txt", StrUtil.substringAfterLast("file.name.txt", ".").orElse(null));
        assertFalse(StrUtil.substringAfterLast("Hello", "xyz").isPresent());

        assertEquals("name", StrUtil.substringAfterLast("file.name.txt", ".", 9).orElse(null));
        assertFalse(StrUtil.substringAfterLast("file.name.txt", ".", 3).isPresent());
    }

    @Test
    public void testSubstringAfterLast() {
        assertEquals("c", Strings.substringAfterLast("a:b:c", ':'));
        assertNull(Strings.substringAfterLast("abc", ':'));
        assertNull(Strings.substringAfterLast(null, ':'));
    }

    @Test
    public void testSubstringAfterLast_endIndexBeyondLength() {
        // exclusiveEndIndex greater than str.length() should not throw StringIndexOutOfBoundsException
        // and should be treated as if it were equal to str.length().
        assertEquals("def", Strings.substringAfterLast("abc.def", ".", 100));
        assertEquals("def", Strings.substringAfterLast("abc.def", ".", 7));
        assertEquals("c", Strings.substringAfterLast("a.b.c", ".", Integer.MAX_VALUE));
    }

    @Test
    public void testSubstringAfterLast_ExclusiveEndIndexRange() {
        // the '.' at index 11 is outside [0, 11), so the last in-range '.' is the one at index 3
        assertEquals("example", Strings.substringAfterLast("com.example.Test", ".", 11));
        assertEquals("Test", Strings.substringAfterLast("com.example.Test", ".", 100));
        assertEquals("c", Strings.substringAfterLast("a.b.c.d", ".", 5));
        assertEquals("", Strings.substringAfterLast("test.", ".", 5));
        assertNull(Strings.substringAfterLast("hello", ".", 5));
        assertNull(Strings.substringAfterLast("test", ".", -1));
    }

    @Test
    public void testSubstringAfterLast_String() {
        assertEquals("c", Strings.substringAfterLast("a::b::c", "::"));
        assertNull(Strings.substringAfterLast(null, "::"));
    }

    @Test
    public void testSubstringAfterLast_WithExclusiveEndIndex() {
        // substringAfterLast(str, delimiter, exclusiveEndIndex)
        assertEquals("ell", Strings.substringAfterLast("hello", "h", 4));
        assertNull(Strings.substringAfterLast(null, "h", 4));
    }

    @Test
    public void testSubstringAfterLast_Char() {
        assertEquals("txt", substringAfterLast("file.name.txt", '.'));
        assertEquals("", substringAfterLast("Hello World!", '!'));
        assertNull(substringAfterLast("Hello", 'x'));
        assertNull(substringAfterLast(null, 'a'));
        assertNull(substringAfterLast("", 'a'));
    }

    @Test
    public void testSubstringAfterLast_StringWithEndIndex() {
        assertEquals("name", substringAfterLast("file.name.txt", ".", 9));
        assertNull(substringAfterLast("file.name.txt", ".", 3));
        assertNull(substringAfterLast(null, ".", 5));
        assertNull(substringAfterLast("test", null, 5));
        assertNull(substringAfterLast("test", ".", -1));
        assertEquals("", substringAfterLast("test", "", 3));
    }

    @Test
    public void testSubstringAfterLastOrElse() {
        assertEquals("c", StrUtil.substringAfterLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("abc", ":", "default"));
    }

    // substringAfterLastOrElseItself

    @Test
    public void testSubstringAfterLastOrElseItself() {
        assertEquals("c", StrUtil.substringAfterLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringAfterLastOrElseItself("abc", ':'));
    }

    @Test
    public void testSubstringAfterLastOrElseItself_CharDelimiter() {
        assertEquals("c", StrUtil.substringAfterLastOrElseItself("a.b.c", '.'));
        assertEquals("hello", StrUtil.substringAfterLastOrElseItself("hello", '.'));
    }

    @Test
    public void testSubstringAfterLastOrElseItself_WithEndIndex() {
        assertEquals("ell", StrUtil.substringAfterLastOrElseItself("hello", "h", 4));
    }

    @Test
    public void testSubstringAfterOrElse() {
        assertEquals("def", StrUtil.substringAfterOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("abc", ":", "default"));
    }

    // substringAfterOrElseItself

    @Test
    public void testSubstringAfterOrElseItself() {
        assertEquals("def", StrUtil.substringAfterOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringAfterOrElseItself("abc", ':'));
    }

    @Test
    public void testSubstringAfterOrElseItself_CharDelimiter() {
        assertEquals("world", StrUtil.substringAfterOrElseItself("hello world", ' '));
        assertEquals("hello", StrUtil.substringAfterOrElseItself("hello", ' '));
    }

    @Test
    public void testSubstringAfterOrElseItself_WithEndIndex() {
        assertEquals("ell", StrUtil.substringAfterOrElseItself("hello", "h", 4));
    }

    // substringBefore

    @Test
    public void testStrUtil_SubstringBefore() {
        assertEquals("He", StrUtil.substringBefore("Hello World", 'l').orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", 'x').isPresent());

        assertEquals("Hello", StrUtil.substringBefore("Hello World", " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", "xyz").isPresent());

        assertEquals("lo", StrUtil.substringBefore("Hello World", 3, " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello World", 7, " ").isPresent());
        assertEquals("", StrUtil.substringBefore("test@end", 4, "@").orElse(null));
    }

    @Test
    public void testStrUtilSubstringBefore() {
        assertEquals("ab", StrUtil.substringBefore("abcde", "cd").get());
        assertFalse(StrUtil.substringBefore("abc", "x").isPresent());
    }

    @Test
    public void testSubstringBefore() {
        assertEquals("abc", Strings.substringBefore("abc:def", ':'));
        assertNull(Strings.substringBefore("abc", ':'));
        assertNull(Strings.substringBefore(null, ':'));
    }

    @Test
    public void testSubstringBefore_String() {
        assertEquals("abc", Strings.substringBefore("abc::def", "::"));
        assertNull(Strings.substringBefore(null, "::"));
    }

    @Test
    public void testSubstringBefore_WithInclusiveBeginIndex() {
        // substringBefore(str, inclusiveBeginIndex, delimiter)
        assertEquals("ello", Strings.substringBefore("hello world", 1, " "));
        assertNull(Strings.substringBefore(null, 0, " "));
    }

    @Test
    public void testSubstringBeforeAfter_NotFound() {
        assertNull(Strings.substringAfter("hello", '@'));
        assertNull(Strings.substringAfter("hello", "@@"));
        assertNull(Strings.substringBefore("hello", '@'));
        assertNull(Strings.substringBefore("hello", "@@"));
        assertEquals("hello", Strings.substringAfter("hello", ""));
    }

    @Test
    public void testC524_substringBeforeAtLengthAndEmptyInputs() {
        assertNull(Strings.substringBefore("test", 4, "@"));
        assertEquals("", Strings.substringBefore("test", 4, ""));
        assertNull(Strings.substringBefore("test", 5, "@"));
        assertEquals("", Strings.substringBefore("test@end", 4, "@"));
        assertNull(Strings.substringBefore("", 0, "@"));
        assertEquals("", Strings.substringBefore("", 0, ""));

        assertNull(Strings.substringBeforeLast("a.b", 3, "."));
        assertEquals("", Strings.substringBeforeLast("a.b", 3, ""));
        assertEquals("b.c", Strings.substringBeforeLast("a.b.c.d", 2, "."));
        assertNull(Strings.substringBeforeLast("", 0, "."));

        assertNull(Strings.substringBefore("", '@'));
        assertNull(Strings.substringBefore((String) null, '@'));
        assertEquals("", Strings.substringBefore("@x", '@'));
        assertEquals("user", Strings.substringBefore("user@example.com", '@'));
        assertNull(Strings.substringAfter("", '@'));
        assertNull(Strings.substringBeforeLast("", '@'));
    }

    @Test
    public void testC560_substringBeforeCharEmptyInputIsNull() {
        assertNull(Strings.substringBefore("", '@'));
        assertNull(Strings.substringBefore(null, '@'));
        assertEquals("", Strings.substringBefore("@leading", '@'));
        assertEquals("user", Strings.substringBefore("user@example.com", '@'));
        // the three sibling char overloads agree on ""
        assertNull(Strings.substringAfter("", '@'));
        assertNull(Strings.substringAfterLast("", '@'));
        assertNull(Strings.substringBeforeLast("", '@'));
    }

    @Test
    public void testSubstringBefore_Char() {
        assertEquals("He", substringBefore("Hello World", 'l'));
        assertEquals("Hello", substringBefore("Hello World", ' '));
        assertNull(substringBefore("Hello", 'x'));
        assertNull(substringBefore(null, 'a'));
    }

    @Test
    public void testSubstringBefore_StringWithBeginIndex() {
        assertEquals("lo", substringBefore("Hello World", 3, " World"));
        assertNull(substringBefore("Hello World", 7, " "));
        assertNull(substringBefore(null, 0, "test"));
        assertNull(substringBefore("test", 0, null));
        assertNull(substringBefore("test", -1, "st"));
        assertNull(substringBefore("test", 5, "st"));
        assertEquals("", substringBefore("test", 0, ""));
        assertEquals("", substringBefore("::a::b", 0, "::"));
        assertEquals("", substringBefore("test@end", 4, "@"));
    }

    // substringBeforeAny

    @Test
    public void testStrUtil_SubstringBeforeAny() {
        assertEquals("He", StrUtil.substringBeforeAny("Hello World", 'x', 'l', 'z').orElse(null));
        assertFalse(StrUtil.substringBeforeAny("Hello", 'x', 'y', 'z').isPresent());

        assertEquals("Hello", StrUtil.substringBeforeAny("Hello World", "xyz", " World", "abc").orElse(null));
        assertFalse(StrUtil.substringBeforeAny("Hello", "xyz", "abc").isPresent());
        assertEquals("", StrUtil.substringBeforeAny("test", null, "").orElseThrow());
    }

    @Test
    public void testSubstringBeforeAny_CharArray() {
        assertEquals("ab", Strings.substringBeforeAny("abcdef", 'c', 'x'));
        assertNull(Strings.substringBeforeAny("abc", 'x', 'y'));
        assertNull(Strings.substringBeforeAny(null, 'a', 'b'));
    }

    @Test
    public void testSubstringBeforeAny_StringArray() {
        assertEquals("abc", Strings.substringBeforeAny("abc::def", "::", "##"));
        assertNull(Strings.substringBeforeAny(null, "::", "##"));
    }

    @Test
    public void testSubstringBeforeAny_Chars() {
        assertEquals("He", substringBeforeAny("Hello World", 'x', 'l', 'z'));
        assertEquals("Hello ", substringBeforeAny("Hello World", 'W', ' '));
        assertNull(substringBeforeAny("Hello", 'x', 'y', 'z'));
        assertNull(substringBeforeAny(null, 'a', 'b'));
        assertNull(substringBeforeAny("test", CommonUtil.EMPTY_CHAR_ARRAY));
    }

    @Test
    public void testSubstringBeforeAny_Strings() {
        assertEquals("Hello", substringBeforeAny("Hello World", "xyz", " World", "abc"));
        assertEquals("", substringBeforeAny("Hello World", "abc", "Hello", "xyz"));
        assertNull(substringBeforeAny("Hello", "xyz", "abc"));
        assertNull(substringBeforeAny(null, "test", "abc"));
        assertNull(substringBeforeAny("test", CommonUtil.EMPTY_STRING_ARRAY));
        assertEquals("", substringBeforeAny("test", null, ""));
    }

    // substringBeforeLast

    @Test
    public void testStrUtil_SubstringBeforeLast() {
        assertEquals("file.name", StrUtil.substringBeforeLast("file.name.txt", '.').orElse(null));
        assertFalse(StrUtil.substringBeforeLast("Hello", 'x').isPresent());

        assertEquals("file.name", StrUtil.substringBeforeLast("file.name.txt", ".").orElse(null));
        assertFalse(StrUtil.substringBeforeLast("Hello", "xyz").isPresent());

        assertEquals("le.name", StrUtil.substringBeforeLast("file.name.txt", 2, ".").orElse(null));
        assertFalse(StrUtil.substringBeforeLast("file.name.txt", 10, ".").isPresent());
    }

    @Test
    public void testSubstringBeforeLast() {
        assertEquals("a:b", Strings.substringBeforeLast("a:b:c", ':'));
        assertNull(Strings.substringBeforeLast("abc", ':'));
        assertNull(Strings.substringBeforeLast(null, ':'));
    }

    @Test
    public void testSubstringBeforeLast_String() {
        assertEquals("a::b", Strings.substringBeforeLast("a::b::c", "::"));
        assertNull(Strings.substringBeforeLast(null, "::"));
        assertEquals("", substringBeforeLast("test@end", 4, "@"));
    }

    @Test
    public void testSubstringBeforeLast_WithInclusiveBeginIndex() {
        // substringBeforeLast(str, inclusiveBeginIndex, delimiter)
        assertEquals("ello", Strings.substringBeforeLast("hello world", 1, " "));
        assertNull(Strings.substringBeforeLast(null, 0, " "));
    }

    @Test
    public void testSubstringBeforeLast_Char() {
        assertEquals("file.name", substringBeforeLast("file.name.txt", '.'));
        assertEquals("Hello World", substringBeforeLast("Hello World!", '!'));
        assertNull(substringBeforeLast("Hello", 'x'));
        assertNull(substringBeforeLast(null, 'a'));
        assertNull(substringBeforeLast("", 'a'));
    }

    @Test
    public void testSubstringBeforeLast_StringWithBeginIndex() {
        assertEquals("le.name", substringBeforeLast("file.name.txt", 2, "."));
        assertNull(substringBeforeLast("file.name.txt", 10, "."));
        assertNull(substringBeforeLast(null, 0, "."));
        assertNull(substringBeforeLast("test", 0, null));
        assertNull(substringBeforeLast("test", -1, "."));
        assertEquals("st", substringBeforeLast("test", 2, ""));
    }

    @Test
    public void testSubstringBeforeLastOrElse() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("abc", ":", "default"));
    }

    // substringBeforeLastOrElseItself

    @Test
    public void testSubstringBeforeLastOrElseItself() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringBeforeLastOrElseItself("abc", ':'));
    }

    @Test
    public void testSubstringBeforeLastOrElseItself_CharDelimiter() {
        assertEquals("a.b", StrUtil.substringBeforeLastOrElseItself("a.b.c", '.'));
        assertEquals("hello", StrUtil.substringBeforeLastOrElseItself("hello", '.'));
    }

    @Test
    public void testSubstringBeforeLastOrElseItself_WithEndIndex() {
        assertEquals("ello", StrUtil.substringBeforeLastOrElseItself("hello world", 1, " "));
    }

    @Test
    public void testSubstringBeforeOrElse() {
        assertEquals("abc", StrUtil.substringBeforeOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("abc", ":", "default"));
    }

    // substringBeforeOrElseItself

    @Test
    public void testSubstringBeforeOrElseItself() {
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc", ':'));
    }

    @Test
    public void testSubstringBeforeOrElseItself_CharDelimiter() {
        assertEquals("hello", StrUtil.substringBeforeOrElseItself("hello world", ' '));
        assertEquals("hello", StrUtil.substringBeforeOrElseItself("hello", ' '));
    }

    @Test
    public void testSubstringBeforeOrElseItself_WithBeginIndex() {
        assertEquals("ello", StrUtil.substringBeforeOrElseItself("hello world", 1, " "));
        assertEquals("", StrUtil.substringBeforeOrElseItself("::a::b", 0, "::"));
        assertEquals("", StrUtil.substringBeforeOrElseItself("hello.world.java", 5, "."));
        assertEquals("", StrUtil.substringBeforeOrElseItself("test@end", 4, "@"));
    }

    // substringBetween

    @Test
    public void testC544_operatorSubstringBetweenNullDelimiter() {
        assertNull(Strings.substringBetween("Hello", (String) null, i -> 5));
        assertNull(Strings.substringBetween("Hello", i -> 0, (String) null));
        assertNull(Strings.substringBetween("Hello", "HelloWorld", i -> 5));
        assertNull(Strings.substringBetween("Hello", "x", i -> 5));
        assertEquals("World", Strings.substringBetween("Hello:World", ":", i -> i + 5));
        assertEquals("ello", Strings.substringBetween("Hello:World", i -> i - 5, ":"));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, (String) null, (java.util.function.IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, (java.util.function.IntUnaryOperator) null, (String) null));
    }

    @Test
    public void testStrUtilSubstringBetween() {
        assertEquals("abc", StrUtil.substringBetween("(abc)", "(", ")").get());
        assertFalse(StrUtil.substringBetween("abc", "[", "]").isPresent());
        assertEquals("ello", StrUtil.substringBetween("hello", 0, 10).orElseThrow());
        assertFalse(StrUtil.substringBetween("hello", 5, 10).isPresent());
        // The computed-begin variant anchors on the FIRST occurrence of the delimiter (delegates to Strings).
        assertEquals("a", StrUtil.substringBetween("a=b=c", i -> -1, "=").get());
    }

    @Test
    public void testStrUtilSubstringBetween_beginFunctionAndDelimiter_receivesDelimiterIndex() {
        final int[] received = new int[1];

        assertEquals("a", StrUtil.substringBetween("a::b", i -> {
            received[0] = i;
            return -1;
        }, "::").orElseNull());
        assertEquals(1, received[0]);
    }

    @Test
    public void testStrUtilSubstringBetween_delimiterAndEndFunction_receivesContentStart() {
        final int[] received = new int[1];

        assertEquals("b", StrUtil.substringBetween("a::bc", "::", i -> {
            received[0] = i;
            return i + 1;
        }).orElseNull());
        assertEquals(3, received[0]);

        assertEquals("", StrUtil.substringBetween("a::b", "::", i -> i).orElseNull());
        assertTrue(StrUtil.substringBetween("a::b", "::", i -> i - 1).isEmpty());
    }

    @Test
    public void testStrUtilSubstringBetweenRejectsNullFunctions() {
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", 0, (IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", (IntUnaryOperator) null, 5));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", "h", (IntUnaryOperator) null));
        assertThrows(IllegalArgumentException.class, () -> StrUtil.substringBetween("hello", (IntUnaryOperator) null, "o"));
    }

    @Test
    public void test_substringBetween() {
        final String str = "abc";

        assertEquals(0, "abc".lastIndexOf("a", 3));
        assertEquals(3, "abca".lastIndexOf("a", 3));
        assertEquals(2, "abca".lastIndexOf("ca", 3));
        assertEquals(2, "abca".lastIndexOf("ca", 2));
        assertEquals(-1, "abca".lastIndexOf("ca", 1));

        assertEquals("ba", Strings.substringBetween("abade", "a", "de"));
        assertEquals("bc", Strings.substringBetween(str, "a", 3));
        assertEquals("abc", Strings.substringBetween(str, "", 3));
        assertEquals("bc", Strings.substringBetween("abcdef", 0, 3));
        assertNull(Strings.substringBetween("test", Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertNull(Strings.substringBetween("test", Integer.MAX_VALUE, 0));
        assertNull(Strings.substringBetween(null, 0, 3));
        assertEquals("ba", Strings.substringBetween("abade", 'a', 'd'));
        assertEquals("b", Strings.substringBetween("abde", 'a', 'd'));
        assertEquals("abc", Strings.substringAfter(str, "", 3));
        assertEquals("", Strings.substringAfterLast(str, "", 3));
        assertEquals("", Strings.substringAfterLast(str, "", 3));
    }

    @Test
    public void testSubstringBetween() {
        assertEquals("b", Strings.substringBetween("a:b:c", ":"));
        assertNull(Strings.substringBetween("abc", ":"));
        assertNull(Strings.substringBetween(null, ":"));
    }

    @Test
    public void test_substringbetween_01() {
        assertEquals("aa", CommonUtil.firstNonBlank("aa", "bb").get());

        assertEquals("ab", StringUtils.substringBetween("abc", "", "c"));
        assertEquals("", StringUtils.substringBetween("abc", "a", ""));
        assertEquals("", StringUtils.substringBetween("abc", "", ""));
        assertEquals("b", StringUtils.substringBetween("abc", "a", "c"));

        assertEquals("ab", Strings.substringBetween("abc", "", "c"));
        assertEquals("", Strings.substringBetween("abc", "a", ""));
        assertEquals("", Strings.substringBetween("abc", "", ""));
        assertEquals("b", Strings.substringBetween("abc", "a", "c"));
        assertEquals("b", Strings.substringBetween("abc", 0, 'c'));
        assertEquals("", Strings.substringBetween("abc", 0, "bc"));
        assertEquals("b", Strings.substringBetween("abc", 0, "c"));
        assertEquals("", Strings.substringBetween("abc", "b", 2));
        assertNull(Strings.substringBetween("abc", "b", 1));
        assertEquals("bc", Strings.substringBetween("abc::tail", i -> {
            assertEquals(3, i);
            return i - 3;
        }, "::"));
    }

    @Test
    public void testSubstringBetween_charDelimiter_endIndexBeyondLength() {
        // exclusiveEndIndex greater than str.length() should not throw StringIndexOutOfBoundsException.
        assertEquals("sr", Strings.substringBetween("u@sr", '@', 100));
        assertEquals("ello", Strings.substringBetween("Hello", 'H', Integer.MAX_VALUE));
    }

    @Test
    public void testSubstringBetween_CharIntOverload() {
        // substringBetween(str, charDelimiterOfExclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substringBetween("hello world", ' ', 11);
        assertEquals("world", result);
    }

    @Test
    public void testsubstringBetween_Delimiter() {
        assertEquals("World", Strings.substringBetween("Hello:World", ":", i -> i + 5));
        assertNull(Strings.substringBetween(null, ":", i -> 5));
        assertNull(Strings.substringBetween("Hello", "x", i -> 5)); // delimiter not found
        // R-7b: a delimiter whose length equals the string length is accepted (only > length is rejected)
        assertEquals("", Strings.substringBetween("Hello", "Hello", i -> 10));
    }

    @Test
    public void testsubstringBetween_Delimiter_01() {
        assertEquals("Hello", Strings.substringBetween("Hello:World", i -> -1, ":"));
        assertNull(Strings.substringBetween(null, i -> 0, ":"));
        assertNull(Strings.substringBetween("Hello", i -> 0, "x")); // delimiter not found
        // R-7a: anchors on the FIRST occurrence of the end delimiter (not the last)
        assertEquals("a", Strings.substringBetween("a=b=c", i -> -1, "="));
        // R-7b: a delimiter whose length equals the string length is no longer rejected by the length guard
        assertNull(Strings.substringBetween("Hello", i -> 0, "Hello")); // calculated begin index >= end index
    }

    @Test
    public void testSubstringBetween_DifferentDelimiters() {
        assertEquals("content", Strings.substringBetween("<tag>content</tag>", "<tag>", "</tag>"));
        assertNull(Strings.substringBetween(null, "<", ">"));
    }

    @Test
    public void testSubstringBetween_FamilyNoteFacts() {
        // equal-length delimiter is accepted and yields "" (NOT null) for both the int and the func overload
        assertEquals("", Strings.substringBetween("ab", "ab", 2));
        assertEquals("", Strings.substringBetween("ab", "ab", i -> 2));
        // (String begin-func, String end) anchors on the FIRST occurrence of the end delimiter
        assertEquals("a", Strings.substringBetween("a=b=c", i -> -1, "="));
    }

    @Test
    public void testsubstringBetween_Int() {
        assertEquals("ello", Strings.substringBetween("Hello World", 0, i -> i + 5));
        assertNull(Strings.substringBetween(null, 0, i -> 5));
        assertNull(Strings.substringBetween("Hello", 10, i -> 15)); // beginIndex >= length
    }

    @Test
    public void testsubstringBetween_Int_01() {
        assertEquals("Hello", Strings.substringBetween("Hello World", i -> -1, 5));
        assertNull(Strings.substringBetween(null, i -> 0, 5));
        assertNull(Strings.substringBetween("Hello", i -> 0, 0)); // begin >= end
        // An end of 0 is a valid boundary (2026-09-02 review): with a computed begin of -1 it yields "", exactly like
        // the (int, int) form. See StringsReviewFixes20260902Test.
        assertEquals("", Strings.substringBetween("Hello", i -> -1, 0));
        assertEquals(Strings.substringBetween("Hello", -1, 0), Strings.substringBetween("Hello", i -> -1, 0));
        assertNull(Strings.substringBetween("Hello", i -> -1, -1)); // end < 0
        assertEquals("c", Strings.substringBetween("abc", i -> i - 2, 10)); // operator receives clamped end 3
    }

    @Test
    public void testSubstringBetween_IntCharOverload() {
        // substringBetween(str, exclusiveBeginIndex, charDelimiterOfExclusiveEndIndex)
        String result = Strings.substringBetween("hello world", 0, ' ');
        assertEquals("ello", result);
    }

    @Test
    public void testsubstringBetween_IntIndex() {
        String result = Strings.substringBetween("hello world", beginIdx -> 0, 8);
        assertEquals("ello wo", result);
    }

    @Test
    public void testsubstringBetween_IntIndex_02() {
        // str="hello world", exclusiveBeginIndex=4 (after 'o'), func returns next search position + 6
        String result = Strings.substringBetween("hello world", 4, endIdx -> endIdx + 6);
        // exclusiveBeginIndex=4 means start looking from index 5
        // funcOfExclusiveEndIndex maps from the first character's index to the end index
        assertEquals(" worl", result);
    }

    @Test
    public void testSubstringBetween_IntIntOverload() {
        // substringBetween(str, exclusiveBeginIndex, exclusiveEndIndex)
        assertEquals("ell", Strings.substringBetween("hello", 0, 4));
        assertNull(Strings.substringBetween(null, 0, 4));
    }

    @Test
    public void testSubstringBetween_IntStringOverload() {
        // substringBetween(str, exclusiveBeginIndex, stringDelimiterOfExclusiveEndIndex)
        String result = Strings.substringBetween("hello world", 0, " ");
        assertEquals("ello", result);
    }



    // D2 : substringBetween(String, IntUnaryOperator, int) accepts an end index of 0
    @Test
    public void testSubstringBetween_OperatorBeginEndZero() {
        // The one changed shape: end 0 with a computed begin of -1 is the empty range [0, 0).
        assertEquals("", Strings.substringBetween("abc", i -> -1, 0));
        assertEquals("", Strings.substringBetween("", i -> -1, 0));
        assertEquals(Optional.of(""), StrUtil.substringBetween("abc", i -> -1, 0));

        // Parity with the plain-index form for every begin at end 0.
        for (int begin = -3; begin <= 3; begin++) {
            final int b = begin;
            assertEquals(Strings.substringBetween("abc", b, 0), Strings.substringBetween("abc", i -> b, 0), "begin=" + b);
            assertEquals(Strings.substringBetween("", b, 0), Strings.substringBetween("", i -> b, 0), "begin=" + b + " on empty");
        }

        // The operator really is consulted at end 0 (it was skipped before), and it receives the clamped end 0.
        final int[] seen = { Integer.MIN_VALUE };
        assertEquals("", Strings.substringBetween("abc", i -> {
            seen[0] = i;
            return -1;
        }, 0));
        assertEquals(0, seen[0]);
    }

    @Test
    public void testSubstringBetween_OperatorBeginInvalid() {
        // Any computed begin other than -1 cannot precede an end of 0.
        assertNull(Strings.substringBetween("abc", i -> 0, 0));
        assertNull(Strings.substringBetween("abc", i -> 1, 0));
        assertNull(Strings.substringBetween("abc", i -> -2, 0));
        // A negative end is still rejected before the operator runs.
        final int[] calls = { 0 };
        assertNull(Strings.substringBetween("abc", i -> {
            calls[0]++;
            return -1;
        }, -1));
        assertEquals(0, calls[0]);
        assertNull(Strings.substringBetween(null, i -> -1, 0));
        assertEquals(Optional.empty(), StrUtil.substringBetween("abc", i -> 0, 0));
        // The operator is still validated first, even for a negative end.
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween("abc", (IntUnaryOperator) null, -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringBetween(null, (IntUnaryOperator) null, 0));
    }

    @Test
    public void testSubstringBetween_OperatorBeginPositiveEnds() {
        // Behaviour for a positive end is untouched by the guard change.
        assertEquals("Hello", Strings.substringBetween("Hello World", i -> -1, 5));
        assertEquals("World", Strings.substringBetween("Hello World", i -> i - 6, 11));
        assertEquals("", Strings.substringBetween("Hello", i -> 2, 3));
        assertEquals("lo", Strings.substringBetween("Hello", i -> i - 3, 100));
        assertNull(Strings.substringBetween("Hello", i -> 5, 3));
        assertNull(Strings.substringBetween("Hello", i -> -2, 3));
    }

    @Test
    public void testsubstringBetween_String() {
        String result = Strings.substringBetween("hello world test", "hello", endIdx -> endIdx + 6);
        assertEquals(" world", result);
    }

    @Test
    public void testsubstringBetween_String_01() {
        String result = Strings.substringBetween("hello world test", beginIdx -> 0, "test");
        assertEquals("ello world ", result);
    }

    @Test
    public void testSubstringBetween_stringDelimiter_endIndexBeyondLength() {
        // exclusiveEndIndex greater than str.length() should not throw StringIndexOutOfBoundsException.
        assertEquals("c", Strings.substringBetween("ab@c", "@", 100));
        assertEquals("ello", Strings.substringBetween("Hello", "H", Integer.MAX_VALUE));
    }

    @Test
    public void testSubstringBetween_StringIntOverload() {
        // substringBetween(str, stringDelimiterOfExclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substringBetween("hello world", " ", 11);
        assertEquals("world", result);
    }

    @Test
    public void testSubstringBetween_StringString() {
        assertNull(StringUtils.substringBetween(null, "tag"));
        assertEquals("", StringUtils.substringBetween("", ""));
        assertNull(StringUtils.substringBetween("", "abc"));
        assertEquals("", StringUtils.substringBetween("    ", " "));
        assertNull(StringUtils.substringBetween("abc", null));
        assertEquals("", StringUtils.substringBetween("abc", ""));
        assertNull(StringUtils.substringBetween("abc", "a"));
        assertEquals("bc", StringUtils.substringBetween("abca", "a"));
        assertEquals("bc", StringUtils.substringBetween("abcabca", "a"));
        assertEquals("bar", StringUtils.substringBetween("\nbar\n", "\n"));
        assertEquals("", StringUtils.substringBetween("", "", ""));
        assertEquals("", Strings.substringBetween("abc", "", ""));
    }

    @Test
    public void testSubstringBetween_StringString_01() {
        assertNull(Strings.substringBetween(null, "tag"));
        assertEquals("", Strings.substringBetween("", ""));
        assertNull(Strings.substringBetween("", "abc"));
        assertEquals("", Strings.substringBetween("    ", " "));
        assertNull(Strings.substringBetween("abc", null));
        assertEquals("", Strings.substringBetween("abc", ""));
        assertNull(Strings.substringBetween("abc", "a"));
        assertEquals("bc", Strings.substringBetween("abca", "a"));
        assertEquals("bc", Strings.substringBetween("abcabca", "a"));
        assertEquals("bar", Strings.substringBetween("\nbar\n", "\n"));
        assertEquals("", Strings.substringBetween("", "", ""));
        assertEquals("", Strings.substringBetween("abc", "", ""));
    }

    @Test
    public void testSubstringBetween_StringStringString() {
        assertNull(StringUtils.substringBetween(null, "", ""));
        assertNull(StringUtils.substringBetween("", null, ""));
        assertNull(StringUtils.substringBetween("", "", null));
        assertEquals("", StringUtils.substringBetween("", "", ""));
        assertEquals("", StringUtils.substringBetween("foo", "", ""));
        assertNull(StringUtils.substringBetween("foo", "", "]"));
        assertNull(StringUtils.substringBetween("foo", "[", "]"));
        assertEquals("", StringUtils.substringBetween("    ", " ", "  "));
        assertEquals("bar", StringUtils.substringBetween("<foo>bar</foo>", "<foo>", "</foo>"));
        assertEquals("abc", StringUtils.substringBetween("yabczyabcz", "y", "z"));
    }

    @Test
    public void testSubstringBetween_StringStringString_01() {
        assertNull(Strings.substringBetween(null, "", ""));
        assertNull(Strings.substringBetween("", (String) null, ""));
        assertNull(Strings.substringBetween("", "", (String) null));
        assertEquals("", Strings.substringBetween("", "", ""));
        assertEquals("", Strings.substringBetween("foo", "", ""));
        assertNull(Strings.substringBetween("foo", "", "]"));
        assertNull(Strings.substringBetween("foo", "[", "]"));
        assertEquals("", Strings.substringBetween("    ", " ", "  "));
        assertEquals("bar", Strings.substringBetween("<foo>bar</foo>", "<foo>", "</foo>"));
        assertEquals("abc", Strings.substringBetween("yabczyabcz", "y", "z"));
    }

    @Test
    public void testSubstringBetween_WithFromIndex() {
        // substringBetween(str, fromIndex, delimiterBegin, delimiterEnd)
        String result = Strings.substringBetween("a[b]c[d]", 3, "[", "]");
        assertEquals("d", result);
    }

    @Test
    public void testC561_substringBetweenIndexAndOperatorFormsOnEmptyInput() {
        assertEquals("", Strings.substringBetween("", -1, 0));
        assertEquals("", Strings.substringBetween("", -1, i -> 0));
        assertEquals("", Strings.substringBetween("", i -> -1, 0));
        assertEquals("", Strings.substringBetween("", "", ""));
        assertNull(Strings.substringBetween("", "[", "]"));
        // a non-negative end is capped at the length, so any end >= 0 behaves like 0 on ""
        assertEquals("", Strings.substringBetween("", -1, 5));
        assertEquals("", Strings.substringBetween("", -1, i -> 1));
        // a begin index of 0 is already past the end of ""; a negative end is invalid
        assertNull(Strings.substringBetween("", 0, 0));
        assertNull(Strings.substringBetween("", i -> 0, 0));
        assertNull(Strings.substringBetween("", -1, -1));
        assertNull(Strings.substringBetween(null, -1, 0));
    }

    @Test
    public void testSubstringBetween_CharDelimiterAndIndex() {
        assertEquals("llo", substringBetween("Hello World", 'e', 5));
        assertNull(substringBetween("Hello", 'x', 5));
        assertNull(substringBetween("Hello", 'H', 0));
        assertNull(substringBetween(null, 'e', 5));
    }

    @Test
    public void testSubstringBetween_CharDelimiters() {
        assertEquals("b", substringBetween("abc", 'a', 'c'));
        assertEquals("a", substringBetween("aac", 'a', 'c'));
        assertNull(substringBetween("abc", 'x', 'c'));
        assertNull(substringBetween("abc", 'a', 'x'));
        assertNull(substringBetween(null, 'a', 'c'));
        assertNull(substringBetween("", 'a', 'c'));
    }

    @Test
    public void testSubstringBetween_IndexAndCharDelimiter() {
        assertEquals("ll", substringBetween("Hello World", 1, 'o'));
        assertNull(substringBetween("Hello", 1, 'x'));
        assertEquals("Hell", substringBetween("Hello", -1, 'o'));
        assertNull(substringBetween("Hello", 5, 'o'));
        assertNull(substringBetween(null, 0, 'o'));
    }

    @Test
    public void testSubstringBetween_IndexAndStringDelimiter() {
        assertEquals("ello", substringBetween("Hello World", 0, " World"));
        assertNull(substringBetween("Hello", 0, "xyz"));
        assertEquals("Hel", substringBetween("Hello", -1, "lo"));
        assertNull(substringBetween("Hello", 5, "lo"));
        assertNull(substringBetween(null, 0, "lo"));
        assertNull(substringBetween("Hello", 0, (String) null));
    }

    @Test
    public void testSubstringBetween_Indexes() {
        assertEquals("ll", substringBetween("Hello", 1, 4));
        assertEquals(null, substringBetween("Hello", 2, 2));
        assertEquals(null, substringBetween("Hello", -2, 4));
        assertNull(substringBetween("Hello", 2, 1));
        assertNull(substringBetween("Hello", 5, 10));
        assertNull(substringBetween(null, 0, 5));
    }

    @Test
    public void testSubstringBetween_StringDelimiterAndIndex() {
        assertEquals("llo", substringBetween("Hello World", "He", 5));
        assertNull(substringBetween("Hello", "xyz", 5));
        assertNull(substringBetween("Hello", "He", -1));
        assertNull(substringBetween(null, "He", 5));
        assertNull(substringBetween("Hello", (String) null, 5));
    }

    @Test
    public void testSubstringBetween_StringDelimiters() {
        assertEquals("b", substringBetween("abc", "a", "c"));
        assertEquals("a", substringBetween("aac", "a", "c"));
        assertEquals("tagged", substringBetween("<tag>tagged</tag>", "<tag>", "</tag>"));
        assertEquals("abc", substringBetween("xabcx", "x"));
        assertNull(substringBetween("abc", "x", "c"));
        assertNull(substringBetween("abc", "a", "x"));
        assertNull(substringBetween(null, "a", "c"));
        assertNull(substringBetween("abc", (String) null, "c"));
        assertNull(substringBetween("abc", "a", (String) null));
    }

    @Test
    public void testSubstringBetween_StringDelimitersWithFromIndex() {
        assertEquals("", substringBetween("abcabcd", 2, "b", ""));
        assertEquals("", substringBetween("<tag1>text1</tag1><tag2>text2</tag2>", 10, ">", "<"));
        assertEquals("tag2", substringBetween("<tag1>text1</tag1><tag2>text2</tag2>", 10, "><", ">"));
        assertNull(substringBetween("abc", 5, "a", "c"));
        assertEquals("b", substringBetween("abc", -1, "a", "c"));
        assertNull(substringBetween(null, 0, "a", "c"));
    }

    // substringBetweenFirstAndLast



    // C-036: the three StrUtil.substringBetweenFirstAndLast wrappers agree with the Strings originals
    @Test
    public void testC036_strUtilSubstringBetweenFirstAndLast() {
        final String[] inputs = { null, "", "hello", "[a][b]", "x[a]y[b]z", "##", "#a#b#", "😀#x#😀" };
        final String[] delimiters = { "", "#", "[", "]", "ab" };

        for (final String str : inputs) {
            for (final String d : delimiters) {
                assertEquals(u.Optional.ofNullable(Strings.substringBetweenFirstAndLast(str, d)), StrUtil.substringBetweenFirstAndLast(str, d));

                for (final String e : delimiters) {
                    assertEquals(u.Optional.ofNullable(Strings.substringBetweenFirstAndLast(str, d, e)), StrUtil.substringBetweenFirstAndLast(str, d, e));

                    for (int from = -1; from <= 6; from++) {
                        assertEquals(u.Optional.ofNullable(Strings.substringBetweenFirstAndLast(str, from, d, e)),
                                StrUtil.substringBetweenFirstAndLast(str, from, d, e));
                    }
                }
            }
        }

        assertEquals(u.Optional.of("hello"), StrUtil.substringBetweenFirstAndLast("hello", ""));
        assertFalse(StrUtil.substringBetweenFirstAndLast(null, "#").isPresent());
    }

    @Test
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_Adjacent() {
        assertEquals("", Strings.substringBetweenFirstAndLast("[[]]", "["));
        assertEquals("", Strings.substringBetweenFirstAndLast("####", "##"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_Basic() {
        // Normal cases
        assertEquals("middle", Strings.substringBetweenFirstAndLast("[middle[]]", "["));
        assertEquals("value", Strings.substringBetweenFirstAndLast("<tag>value<tag>", "<tag>"));
        assertEquals("text", Strings.substringBetweenFirstAndLast("@@text@@", "@@"));

        // Edge cases
        assertNull(Strings.substringBetweenFirstAndLast("no-match", "[["));
        assertNull(Strings.substringBetweenFirstAndLast("[[only-one", "[["));
        assertNull(Strings.substringBetweenFirstAndLast(null, "[["));
        assertNull(Strings.substringBetweenFirstAndLast("test", null));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_Multiple() {
        assertEquals("a::b::c", Strings.substringBetweenFirstAndLast("::a::b::c::", "::"));
        assertEquals("first**second", Strings.substringBetweenFirstAndLast("**first**second**", "**"));
        assertEquals("1|2|3", Strings.substringBetweenFirstAndLast("|1|2|3|", "|"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_OnlyOne() {
        assertNull(Strings.substringBetweenFirstAndLast("[content", "["));
        assertNull(Strings.substringBetweenFirstAndLast("content]", "]"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_Basic() {
        // Normal cases
        assertEquals("middle[]", Strings.substringBetweenFirstAndLast("[middle[]]", "[", "]"));
        assertEquals("content", Strings.substringBetweenFirstAndLast("<tag>content</tag>", "<tag>", "</tag>"));
        assertEquals("World", Strings.substringBetweenFirstAndLast("Hello [World]!", "[", "]"));

        // Edge cases
        assertNull(Strings.substringBetweenFirstAndLast("no-match", "[[", "]]"));
        assertNull(Strings.substringBetweenFirstAndLast("[[no-end", "[[", "]]"));
        assertNull(Strings.substringBetweenFirstAndLast(null, "[[", "]]"));
        assertNull(Strings.substringBetweenFirstAndLast("test", null, "]]"));
        assertNull(Strings.substringBetweenFirstAndLast("test", "[[", null));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_EmptyResult() {
        assertEquals("", Strings.substringBetweenFirstAndLast("<>", "<", ">"));
        assertEquals("", Strings.substringBetweenFirstAndLast("[]", "[", "]"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_MultiplePairs() {
        assertEquals("first</tag><tag>second", Strings.substringBetweenFirstAndLast("<tag>first</tag><tag>second</tag>", "<tag>", "</tag>"));
        assertEquals("a](b)[c", Strings.substringBetweenFirstAndLast("[a](b)[c]", "[", "]"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_Nested() {
        assertEquals("outer<inner>content</inner>outer", Strings.substringBetweenFirstAndLast("<outer<inner>content</inner>outer>", "<", ">"));
        assertEquals("a[b[c]]d", Strings.substringBetweenFirstAndLast("[a[b[c]]d]", "[", "]"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_SameDelimiters() {
        assertEquals("content", Strings.substringBetweenFirstAndLast("|content|", "|", "|"));
        assertEquals("a|b", Strings.substringBetweenFirstAndLast("|a|b|", "|", "|"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_WithIndex_Basic() {
        // Normal cases
        assertEquals("data", Strings.substringBetweenFirstAndLast("<<data>>more<<data>>", 2, "<<", ">>"));
        assertEquals("y", Strings.substringBetweenFirstAndLast("{{x}}{{y}}", 3, "{{", "}}"));
        assertEquals(" ", Strings.substringBetweenFirstAndLast("Hello World", 0, "Hello", "World"));

        // Edge cases
        assertNull(Strings.substringBetweenFirstAndLast("test", 10, "t", "t"));
        assertNull(Strings.substringBetweenFirstAndLast("no-match", 0, "{{", "}}"));
        assertNull(Strings.substringBetweenFirstAndLast(null, 0, "{{", "}}"));
        assertNull(Strings.substringBetweenFirstAndLast("test", 0, null, "}}"));
    }

    // substringOrElse

    @Test
    public void testStrUtilSubstringOrElse() {
        assertEquals("bc", StrUtil.substringOrElse("abc", 1, "default"));
        assertEquals("default", StrUtil.substringOrElse("abc", 4, "default"));
        assertEquals("default", StrUtil.substringOrElse(null, 1, "default"));
    }

    @Test
    public void testSubstringOrElse() {
        assertEquals("bcd", StrUtil.substringOrElse("abcdef", 1, 4, "default"));
        assertEquals("default", StrUtil.substringOrElse("abc", 10, 20, "default"));
    }

    @Test
    public void testSubstringOrElse_FuncBegin() {
        // substringOrElse(str, funcOfInclusiveBeginIndex, exclusiveEndIndex, defaultStr)
        String result = StrUtil.substringOrElse("hello", len -> 0, 5, "default");
        assertEquals("hello", result);
    }



    /**
     * Contract pin for the empty-delimiter note on the four String-delimiter {@code ...OrElse} extractors:
     * {@code substringAfterOrElse} and {@code substringBeforeLastOrElse} return {@code str} itself and never the
     * fallback, while their two siblings return the empty string; only a {@code null} string reaches the fallback.
     */
    @Test
    public void testSubstringOrElse_EmptyDelimiterNeverReachesFallback() {
        assertEquals("hello", StrUtil.substringAfterOrElse("hello", "", "D"));
        assertEquals("hello", StrUtil.substringBeforeLastOrElse("hello", "", "D"));
        assertEquals("", StrUtil.substringAfterLastOrElse("hello", "", "D"));
        assertEquals("", StrUtil.substringBeforeOrElse("hello", "", "D"));

        assertEquals("", StrUtil.substringAfterOrElse("", "", "D"));
        assertEquals("", StrUtil.substringBeforeLastOrElse("", "", "D"));

        assertEquals("D", StrUtil.substringAfterOrElse(null, "", "D"));
        assertEquals("D", StrUtil.substringBeforeLastOrElse(null, "", "D"));
    }

    // substringOrElseItself

    @Test
    public void testStrUtilSubstringOrElseItself() {
        assertEquals("bc", StrUtil.substringOrElseItself("abc", 1));
        assertEquals("abc", StrUtil.substringOrElseItself("abc", 4));
        assertNull(StrUtil.substringOrElseItself(null, 1));
    }

    @Test
    public void testSubstringOrElseItself() {
        assertEquals("bcd", StrUtil.substringOrElseItself("abcdef", 1, 4));
        assertEquals("abc", StrUtil.substringOrElseItself("abc", 10, 20));
    }

    @Test
    public void testSubstringOrElseItself_FuncBegin() {
        // substringOrElseItself(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = StrUtil.substringOrElseItself("hello", len -> 0, 5);
        assertEquals("hello", result);
    }

}
