package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.base64Encode;
import static com.landawn.abacus.util.Strings.base64EncodeString;
import static com.landawn.abacus.util.Strings.concat;
import static com.landawn.abacus.util.Strings.extractFirstDouble;
import static com.landawn.abacus.util.Strings.isBase64;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.joinEntries;
import static com.landawn.abacus.util.Strings.replaceFirstDouble;
import static com.landawn.abacus.util.Strings.reverse;
import static com.landawn.abacus.util.Strings.reverseDelimited;
import static com.landawn.abacus.util.Strings.rotate;
import static com.landawn.abacus.util.Strings.shuffle;
import static com.landawn.abacus.util.Strings.sort;
import static com.landawn.abacus.util.Strings.substring;
import static com.landawn.abacus.util.Strings.substringAfter;
import static com.landawn.abacus.util.Strings.substringAfterAny;
import static com.landawn.abacus.util.Strings.substringAfterLast;
import static com.landawn.abacus.util.Strings.substringBefore;
import static com.landawn.abacus.util.Strings.substringBeforeAny;
import static com.landawn.abacus.util.Strings.substringBeforeLast;
import static com.landawn.abacus.util.Strings.substringBetween;
import static com.landawn.abacus.util.Strings.substringBetweenIgnoreCase;
import static com.landawn.abacus.util.Strings.substringIndicesBetween;
import static com.landawn.abacus.util.Strings.substringsBetween;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Strings.ExtractStrategy;
import com.landawn.abacus.util.Strings.StrUtil;

@Tag("old-test")
public class StringsTest extends AbstractTest {

    public int findLength2(final char[] a, final char[] b) {
        if (CommonUtil.isEmpty(a) || CommonUtil.isEmpty(b)) {
            return 0;
        }

        int end = 0;

        final int lenA = a.length;
        final int lenB = b.length;

        final int[] dp = new int[lenB + 1];
        int maxLen = 0;

        for (int i = 1; i <= lenA; i++) {
            for (int j = lenB; j > 0; j--) {
                if (a[i - 1] == b[j - 1]) {
                    dp[j] = 1 + dp[j - 1];

                    maxLen = Math.max(maxLen, dp[j]);

                    if (dp[j] > maxLen) {
                        maxLen = dp[j];
                    }

                    end = i - 1;
                } else {
                    dp[j] = 0;
                }
            }
        }

        N.println(new String(CommonUtil.copyOfRange(a, end - maxLen, end)));

        return maxLen;
    }

    @SafeVarargs
    private final <T> List<T> list(final T... elements) {
        return Arrays.asList(elements);
    }

    private List<String> substringsBetween_StackBased_(final String str, final char fromDelimiter, final char toDelimiter) {
        return Strings.substringsBetween(str, fromDelimiter, toDelimiter, ExtractStrategy.STACK_BASED);
    }

    private List<String> substringsBetween_IgnoreNested_(final String str, final char fromDelimiter, final char toDelimiter) {
        return Strings.substringsBetween(str, fromDelimiter, toDelimiter, ExtractStrategy.IGNORE_NESTED);
    }

    @Test
    public void test_substringsBetween() {

        {
            assertEquals("[\"a2[c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"c\", \"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            assertEquals("[\"[b[a\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"a\", \"b[a]\", \"[b[a]]c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"[b[a]]c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            assertEquals("[\"[b[a\", \"c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"a\", \"c\", \"b[a][c]d\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"b[a][c]d\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

    }

    @Test
    public void test_substringIndicesBetween() {

        {
            assertEquals("[[2, 6], [10, 11]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[5, 6], [2, 7], [10, 11]]",
                    CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[[2, 7], [10, 11]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            assertEquals("[[2, 5], [9, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[2, 5], [9, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[[2, 5], [9, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            assertEquals("[[1, 5]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[4, 5], [2, 6], [1, 8]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[[1, 8]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            assertEquals("[[1, 5], [7, 8]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[4, 5], [7, 8], [2, 10]]",
                    CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[[2, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

    }

    @Test
    public void test_lastIndex() {
        assertEquals(2, "aba".lastIndexOf("a", 3));
        assertEquals(2, "aba".lastIndexOf("a", 2));
        assertEquals(0, "aba".lastIndexOf("a", 1));
        assertEquals(0, "aba".lastIndexOf("a", 0));
        assertEquals(-1, "aba".lastIndexOf("a", -1));
    }

    @Test
    public void test_regularExpression() {
        assertEquals("123", Strings.extractFirstInteger("abc123"));
        assertEquals("-12.34e+5", Strings.extractFirstDouble("abc-12.34e+5xyz", true));
        assertEquals(Double.parseDouble("-12.34e+5"), Numbers.extractFirstDouble("abc-12.34e+5xyz", true).get());

        RegExUtil.matchResults("123", RegExUtil.INTEGER_MATCHER).map(MatchResult::group).forEach(N::println);
        RegExUtil.matchResults("123 456", RegExUtil.INTEGER_MATCHER).map(MatchResult::group).forEach(N::println);
        RegExUtil.matchResults("abc 123", RegExUtil.INTEGER_MATCHER).map(MatchResult::group).forEach(N::println);
        RegExUtil.matchResults("abc-12.34e+5xyz", RegExUtil.SCIENTIFIC_NUMBER_MATCHER).map(MatchResult::group).forEach(N::println);

        assertEquals(false, RegExUtil.INTEGER_MATCHER.matcher("123 456").find());
        assertEquals(true, RegExUtil.INTEGER_FINDER.matcher("123 456").find());

        assertEquals(false, RegExUtil.INTEGER_MATCHER.matcher("123 456").matches());
        assertEquals(false, RegExUtil.INTEGER_FINDER.matcher("123 456").matches());

        assertEquals(true, RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher("123@email.com").matches());
        assertEquals(true, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher("123@email.com").find());

        assertEquals(false, RegExUtil.EMAIL_ADDRESS_RFC_5322_MATCHER.matcher(" 123@email.com ").find());
        assertEquals(true, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.matcher(" 123@email.com ").find());
    }

    @Test
    public void test_toCamelCase() {
        {
            assertEquals("a", Strings.toCamelCase("a"));
            assertEquals("accountContact", Strings.toCamelCase("account_contact"));
        }
    }

    @Test
    public void test_between() {
        {
            String ret = Strings.substringBetween(null, null);
            N.println(ret);
            assertEquals(null, ret);

            ret = Strings.substringBetween("", null);
            N.println(ret);
            assertEquals(null, ret);

            ret = Strings.substringBetween(null, "");
            N.println(ret);
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "c");
            N.println(ret);
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "a");
            N.println(ret);
            assertEquals(null, ret);

            ret = Strings.substringBetween("ab", "a", "b");
            N.println(ret);
            assertEquals("", ret);

            ret = Strings.substringBetween("aab", "a", "b");
            N.println(ret);
            assertEquals("a", ret);

            ret = Strings.substringBetween("aab", "a", "a");
            N.println(ret);
            assertEquals("", ret);
        }
    }

    @Test
    public void test_indicesOf() {
        {
            String ret = Strings.indicesOf("abca", "a").join(", ");
            N.println(ret);
            assertEquals("0, 3", ret);

            ret = Strings.indicesOf("abca", "a", 0).join(", ");
            N.println(ret);
            assertEquals("0, 3", ret);

            ret = Strings.indicesOf("abca", "a", 1).join(", ");
            N.println(ret);
            assertEquals("3", ret);
        }
        {
            String ret = Strings.indicesOf("abcA", "a").join(", ");
            N.println(ret);
            assertEquals("0", ret);

            ret = Strings.indicesOf("abcA", "a", 0).join(", ");
            N.println(ret);
            assertEquals("0", ret);

            ret = Strings.indicesOf("abcA", "a", 1).join(", ");
            N.println(ret);
            assertEquals("", ret);
        }

        {
            String ret = Strings.indicesOfIgnoreCase("abca", "a").join(", ");
            N.println(ret);
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abca", "a", 0).join(", ");
            N.println(ret);
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abca", "a", 1).join(", ");
            N.println(ret);
            assertEquals("3", ret);
        }
        {
            String ret = Strings.indicesOfIgnoreCase("abcA", "a").join(", ");
            N.println(ret);
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abcA", "a", 0).join(", ");
            N.println(ret);
            assertEquals("0, 3", ret);

            ret = Strings.indicesOfIgnoreCase("abcA", "a", 1).join(", ");
            N.println(ret);
            assertEquals("3", ret);
        }

        {
            String ret = Strings.indicesOf("abca", "").join(", ");
            N.println(ret);
            assertEquals("0, 1, 2, 3", ret);

            ret = Strings.indicesOf("abca", null).join(", ");
            N.println(ret);
            assertEquals("", ret);

            ret = Strings.indicesOf(null, "").join(", ");
            N.println(ret);
            assertEquals("", ret);

            ret = Strings.indicesOf("", null).join(", ");
            N.println(ret);
            assertEquals("", ret);
        }

        {
            String ret = RegExUtil.matchIndices("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(", "));
            N.println(ret);
            assertEquals("14, 33", ret);

            ret = RegExUtil.matchResults("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER)
                    .map(MatchResult::group)
                    .collect(Collectors.joining(", "));
            N.println(ret);
            assertEquals("xyz@gmail.com, 123@outlook.cn", ret);
        }

    }

    @Test
    public void test_RegExUtil() {
        {
            String ret = "abc".replace("a", "");
            N.println(ret);
            assertEquals("bc", ret);

            ret = RegExUtil.replaceFirst("any", ".", (String) null);
            N.println(ret);
            assertEquals("ny", ret);

            ret = RegExUtil.replaceAll("abc", ".", (String) null);
            N.println(ret);
            assertEquals("", ret);
        }

    }

    @Test
    public void test_replaceLast() {
        assertEquals(null, Strings.replaceLast(null, "ab", "cc"));
        assertEquals("", Strings.replaceLast("", "ab", "cc"));
        assertEquals("any", Strings.replaceLast("any", null, "cc"));
        assertEquals("any", Strings.replaceLast("any", "*", null));
        assertEquals("any", Strings.replaceLast("any", "", ""));
        assertEquals("any", Strings.replaceLast("any", "", "cc"));
        assertEquals("ab", Strings.replaceLast("aba", "a", null));
        assertEquals("ab", Strings.replaceLast("aba", "a", ""));
        assertEquals("abz", Strings.replaceLast("aba", "a", "z"));
    }

    @Test
    public void test_perf() {
        final String[] a = Array.repeat(Strings.uuid(), 67);

        Profiler.run(3, 10, 3, "Joiner.with", () -> Joiner.with(", ").append(a).toString()).printResult();
        Profiler.run(3, 10, 3, "Strings.join", () -> Strings.join(a, ", ")).printResult();
        assertNotNull(a);
    }

    @Test
    public void test_splitToLines() {
        String[] substrs = Strings.splitToLines("aa\naa\r\n");
        N.println(substrs);
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);

        substrs = Strings.splitToLines("aa\r\naa\n");
        N.println(substrs);
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);

        substrs = Strings.splitToLines("aa\r\n  aa\n", true, false);
        N.println(substrs);
        assertEquals(3, substrs.length);
        assertEquals(substrs[0], substrs[1]);

        substrs = Strings.splitToLines("aa\r\n  aa\n", true, true);
        N.println(substrs);
        assertEquals(2, substrs.length);
        assertEquals(substrs[0], substrs[1]);

        substrs = Strings.splitToLines("aa\r\n  aa\n", false, true);
        N.println(substrs);
        assertEquals(2, substrs.length);
        assertNotEquals(substrs[0], substrs[1]);

        substrs = Strings.splitToLines("", false, true);
        N.println(substrs);
        assertEquals(0, substrs.length);

        substrs = Strings.splitToLines(null, false, true);
        N.println(substrs);
        assertEquals(0, substrs.length);
    }

    @Test
    public void test_surrogate() {
        for (int i = Character.MIN_LOW_SURROGATE; i < Character.MAX_LOW_SURROGATE; i++) {
            N.println((char) i);
        }

        for (int i = Character.MIN_HIGH_SURROGATE; i < Character.MAX_HIGH_SURROGATE; i++) {
            N.println((char) i);
        }
    }

    @Test
    public void test_replace_01() {
        final String str = "ababaaa";
        N.println(Strings.replaceAll(str, "a", "x"));
        N.println(Strings.replaceAll(str, 3, "a", "x"));
        N.println(Strings.replace(str, 3, "a", "", 2));
        assertNotNull(str);
    }

    @Test
    public void test_remove_char() {
        final String str = "🌉";
        final char[] chs = str.toCharArray();
        N.println(chs);

        String newStr = Strings.removeAll(str, chs[0]);
        N.println(newStr);

        newStr = Strings.removeAll(str, new String(Array.of(chs[0])));
        N.println(newStr);

        newStr = Strings.removeAll(str, new String(chs));
        N.println("xxx: " + newStr);

        newStr = Strings.removeAll(str, str);
        N.println("xxx: " + newStr);
        assertNotNull(newStr);
    }

    @Test
    public void test_whilespace() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("abc\n"));
        assertTrue(Strings.containsWhitespace("abc\t"));
    }

    @Test
    public void test_replace() {
        String str = "APPLE ORange         Water";
        assertEquals("APPLE aaa         Water", N.println(Strings.replace(str, 6, 12, "aaa")));

        str = "APPLE ORange         Water";
        assertEquals("APPLE aaa         Water", N.println(Strings.replaceBetween(str, " ", " ", "aaa")));
        assertEquals(Strings.replaceFirst(str, Strings.substringBetween(str, " ", " "), "aaa"), N.println(Strings.replaceBetween(str, " ", " ", "aaa")));

        str = "APPLE ORange         Water";
        assertEquals("aaa ORange         Water", N.println(Strings.replaceBefore(str, " ", "aaa")));
        assertEquals(Strings.replaceFirst(str, Strings.substringBefore(str, " "), "aaa"), N.println(Strings.replaceBefore(str, " ", "aaa")));

        str = "APPLE ORange         Water";
        assertEquals("APPLE aaa", Strings.replaceFirst(str, Strings.substringAfter(str, " "), "aaa"), N.println(Strings.replaceAfter(str, " ", "aaa")));
        assertEquals(Strings.replaceFirst(str, Strings.substringAfter(str, " "), "aaa"), N.println(Strings.replaceAfter(str, " ", "aaa")));

        assertEquals("aaabbb", Strings.replaceBetween("bbb", "", "", "aaa"));

    }

    @Test
    public void test_capitalizeFully() {
        String str = "APPLE ORange         Water";
        assertEquals("APPLE ORange         Water", N.println(Strings.capitalizeFully(str)));

        str = "aPPLE";
        assertEquals("APPLE", N.println(Strings.capitalizeFully(str)));

        str = "APPLE oRange    of     WaTer Of";
        assertEquals("APPLE ORange    of     WaTer Of", N.println(Strings.capitalizeFully(str, " ", "of")));
    }

    @Test
    public void test_findEmail() {
        final String str = "*** test@gmail.orgg&&^ test2@gmail.cn ((& ";
        N.println(Strings.isValidEmailAddress("test@gmail.com"));
        N.println(Strings.findFirstEmailAddress(str));
        N.println(Strings.findAllEmailAddresses(str));
        assertNotNull(str);
    }

    @Test
    public void test_substringbetween_01() {

        CommonUtil.firstNonBlank("aa", "bb").ifPresentOrElse(N::println, () -> N.println("empty"));

        String str = StringUtils.substringBetween("abc", "", "c");
        N.println(str);
        str = StringUtils.substringBetween("abc", "a", "");
        N.println(str);
        str = StringUtils.substringBetween("abc", "", "");
        N.println(str);
        str = StringUtils.substringBetween("abc", "a", "c");
        N.println(str);

        str = Strings.substringBetween("abc", "", "c");
        N.println(str);
        str = Strings.substringBetween("abc", "a", "");
        N.println(str);
        str = Strings.substringBetween("abc", "", "");
        N.println(str);
        str = Strings.substringBetween("abc", "a", "c");
        N.println(str);
        str = Strings.substringBetween("abc", 0, 'c');
        N.println(str);
        str = Strings.substringBetween("abc", 0, "bc");
        N.println(str);
        str = Strings.substringBetween("abc", 0, "c");
        N.println(str);
        str = Strings.substringBetween("abc", "b", 2);
        N.println(str);
        str = Strings.substringBetween("abc", "b", 1);
        N.println(str);
        assertNull(str);
    }

    @Test
    public void test_isMix() {
        assertFalse(Strings.isMixedCase(null));
        assertFalse(Strings.isMixedCase(""));
        assertFalse(Strings.isMixedCase("a"));
        assertFalse(Strings.isMixedCase("B"));
        assertFalse(Strings.isMixedCase("a黎"));
        assertFalse(Strings.isMixedCase("黎B"));
        assertTrue(Strings.isMixedCase("aB"));
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

        assertEquals(4, Strings.indexOf("abc,,", "", ","));
        assertEquals(0, Strings.indexOf(",,abc", "", ","));
        assertEquals(1, Strings.indexOf(",,abc", "", ",", 1));
        assertEquals(-1, Strings.indexOf("abc,abc", "", ","));

        assertEquals(5, Strings.lastIndexOf("abc, aa, aa", "aa", ", ", 6));
        assertEquals(7, Strings.lastIndexOf("abc,aa,aa", "aa", ","));
        assertEquals(-1, Strings.lastIndexOf("aaaa", "aa", ",", 3));

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
        assertEquals("ba", Strings.substringBetween("abade", 'a', 'd'));
        assertEquals("b", Strings.substringBetween("abde", 'a', 'd'));
        assertEquals("abc", Strings.substringAfter(str, "", 3));
        assertEquals("", Strings.substringAfterLast(str, "", 3));
        assertEquals("", Strings.substringAfterLast(str, "", 3));
    }

    @Test
    public void test_containsAll() {
        final String str = "abcD12";
        assertTrue(Strings.containsAll(str, 'a', 'D', '2'));
        assertTrue(Strings.containsAllIgnoreCase(str, "A", "d", "2"));
        assertFalse(Strings.containsAll(str, "a", "d", "2"));
    }

    @Test
    public void test_substringAfter() {
        final String str = "abc";
        N.println(str.indexOf("", 1));
        N.println(str.lastIndexOf(""));
        N.println(Strings.substringAfterLast(str, ""));
        N.println(Strings.substringAfterLast(str, "", 1));
        assertNotNull(str);
    }

    @Test
    public void test_isValidJavaIdentifier() {
        assertTrue(Strings.isValidJavaIdentifier("code123"));
        assertTrue(Strings.isValidJavaIdentifier("$geeks123"));
        assertFalse(Strings.isValidJavaIdentifier("$gee ks123"));
        assertFalse(Strings.isValidJavaIdentifier("232codespeedy"));
    }

    @Test
    public void test_unwrap() {
        assertEquals(Strings.unwrap("ababa", "aba"), Strings.unwrap("ababa", "aba"));
    }

    @Test
    public void test_format_propName() {
        String str = Strings.toSnakeCase("ME_#A3C_AAA_1A2");
        N.println(str);

        assertEquals("me_#a3c_aaa_1a2", str);

        str = Strings.toScreamingSnakeCase("me_#a3c_aaa_1a2");
        N.println(str);
        assertEquals("ME_#A3C_AAA_1A2", str);

        str = Strings.toCamelCase("me_#a3c_aaa_1a2");
        N.println(str);
        assertEquals("me#a3cAaa1a2", str);

        str = Strings.toCamelCase("ME_#A3C_AAA_1A2");
        N.println(str);
        assertEquals("me#a3cAaa1a2", str);

        assertEquals("xml_parser", Strings.toSnakeCase("XmlParser"));
        assertEquals("xml_parser", Strings.toSnakeCase("xmlPARSER"));
        assertEquals("io_error", Strings.toSnakeCase("IOError"));
        assertEquals("io_error", Strings.toSnakeCase("ioERROR"));
        assertEquals("hello_world_api", Strings.toSnakeCase("helloWorldAPI"));

        assertEquals("XML_PARSER", Strings.toScreamingSnakeCase("XmlParser"));
        assertEquals("XML_PARSER", Strings.toScreamingSnakeCase("xmlPARSER"));
        assertEquals("IO_ERROR", Strings.toScreamingSnakeCase("IOError"));
        assertEquals("IO_ERROR", Strings.toScreamingSnakeCase("ioERROR"));
        assertEquals("HELLO_WORLD_API", Strings.toScreamingSnakeCase("helloWorldAPI"));
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

        Profiler.run(1, 10, 3, "longestCommonSubstring", () -> Strings.longestCommonSubstring(a, b)).printResult();

        Profiler.run(1, 10, 3, "longestCommonSubsequence", () -> ListUtils.longestCommonSubsequence(a, b)).printResult();
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
        assertEquals("ba", Strings.substring("bab", 0, "b"));
        assertEquals("", Strings.substring("", 0, ""));
        assertEquals("", Strings.substring("", "", 0));
        assertEquals("", Strings.substring("abc", 0, ""));
        assertEquals("", Strings.substring("abc", 2, ""));
        assertEquals("", Strings.substring("abc", 3, ""));
        assertEquals("", Strings.substring("abc", "", 0));
        assertEquals("", Strings.substring("abc", "", 2));
        assertEquals("", Strings.substring("abc", "", 3));
        assertEquals("", "abc".substring(0, 0));
        assertEquals("", "abc".substring(2, 2));
        assertEquals("", "abc".substring(3, 3));

    }

    @Test
    public void test_first_last() {

        assertEquals("a", Strings.firstChars("abc", 1));
        assertEquals("ab", Strings.firstChars("abc", 2));
        assertEquals("abc", Strings.firstChars("abc", 3));
        assertEquals("abc", Strings.firstChars("abc", 4));

        assertEquals("c", Strings.lastChars("abc", 1));
        assertEquals("bc", Strings.lastChars("abc", 2));
        assertEquals("abc", Strings.lastChars("abc", 3));
        assertEquals("abc", Strings.lastChars("abc", 4));

        assertEquals('a', Strings.firstChar("abc").get());
        assertEquals('c', Strings.lastChar("abc").get());
    }

    @Test
    public void test_split() {
        assertDoesNotThrow(() -> {
            Splitter.with(",").splitToStream(" foo,,,  bar ,").println();
            Splitter.pattern(",").splitToStream(" foo,,,  bar ,").println();
        });
    }

    @Test
    public void test_repeat() {
        String sql = Strings.repeat("?", 0, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        N.println(sql);
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN ()", sql);

        sql = Strings.repeat("?", 1, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        N.println(sql);
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?)", sql);

        sql = Strings.repeat("?", 2, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        N.println(sql);
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?)", sql);

        sql = Strings.repeat("?", 3, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        N.println(sql);
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?)", sql);

        sql = Strings.repeat("?", 4, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        N.println(sql);
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?, ?)", sql);

        sql = Strings.repeat("?", 5, ", ", "DELETE FROM project WHERE employee_project.project_id IN (", ")");
        N.println(sql);
        assertEquals("DELETE FROM project WHERE employee_project.project_id IN (?, ?, ?, ?, ?)", sql);

        for (int n = 0; n < 10; n++) {
            N.println("==================================" + n);

            N.println(Strings.repeat('a', n));
            assertEquals(new String(Array.repeat('a', n)), Strings.repeat('a', n));
            N.println(Strings.repeat('a', n, ','));
            N.println(Strings.repeat("ab", n));
            N.println(Strings.repeat("ab", n, ", "));
        }
    }

    @Test
    public void test_repeat_2() {
        for (int n = 0; n < 10; n++) {
            N.println("==================================" + n);

            N.println(Strings.repeat("ab", n, ", ", null, ")"));
            N.println(Strings.repeat("ab", n, ", ", "(", null));
            N.println(Strings.repeat("ab", n, ", ", "(", ")"));
        }
    }

    @Test
    public void test_findAllIndicesBetween() {
        N.println(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']'));
        N.println(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']'));

        assertEquals("[[2, 6], [10, 11]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']')));
        assertEquals(CommonUtil.toList("a2[c", "a"), Strings.substringsBetween("3[a2[c]]2[a]", '[', ']'));

        N.println(Strings.toCamelCase("a_B_c_D"));
        N.println(Strings.toCamelCase("B_B_c_d"));
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
    public void testSubstringsBetween_StringStringString() {

        String[] results = StringUtils.substringsBetween("[one], [two], [three]", "[", "]");
        assertEquals(3, results.length);
        assertEquals("one", results[0]);
        assertEquals("two", results[1]);
        assertEquals("three", results[2]);

        results = StringUtils.substringsBetween("[one], [two], [three]", "", "");
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], three", "[", "]");
        assertEquals(2, results.length);
        assertEquals("one", results[0]);
        assertEquals("two", results[1]);

        results = StringUtils.substringsBetween("[one], [two], three]", "[", "]");
        assertEquals(2, results.length);
        assertEquals("one", results[0]);
        assertEquals("two", results[1]);

        results = StringUtils.substringsBetween("[one], two], three]", "[", "]");
        assertEquals(1, results.length);
        assertEquals("one", results[0]);

        results = StringUtils.substringsBetween("one], two], [three]", "[", "]");
        assertEquals(1, results.length);
        assertEquals("three", results[0]);

        results = StringUtils.substringsBetween("aabhellobabnonba", "ab", "ba");
        assertEquals(1, results.length);
        assertEquals("hello", results[0]);

        results = StringUtils.substringsBetween("one, two, three", "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("[one, two, three", "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("one, two, three]", "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], [three]", "[", null);
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], [three]", null, "]");
        assertNull(results);

        results = StringUtils.substringsBetween("[one], [two], [three]", "", "");
        assertNull(results);

        results = StringUtils.substringsBetween(null, "[", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("", "[", "]");
        assertEquals(0, results.length);

        results = StringUtils.substringsBetween("", "", "]");
        assertNull(results);

        results = StringUtils.substringsBetween("", "[", "");
        assertNull(results);

        results = StringUtils.substringsBetween("", "", "");
        assertNull(results);
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
    public void testSubstringsBetween_StringStringString_01() {

        List<String> results = Strings.substringsBetween("[one], [two], [three]", "[", "]");
        assertEquals(3, results.size());
        assertEquals("one", results.get(0));
        assertEquals("two", results.get(1));
        assertEquals("three", results.get(2));

        results = Strings.substringsBetween("[one], [two], [three]", "", "");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], three", "[", "]");
        assertEquals(2, results.size());
        assertEquals("one", results.get(0));
        assertEquals("two", results.get(1));

        results = Strings.substringsBetween("[one], [two], three]", "[", "]");
        assertEquals(2, results.size());
        assertEquals("one", results.get(0));
        assertEquals("two", results.get(1));

        results = Strings.substringsBetween("[one], two], three]", "[", "]");
        assertEquals(1, results.size());
        assertEquals("one", results.get(0));

        results = Strings.substringsBetween("one], two], [three]", "[", "]");
        assertEquals(1, results.size());
        assertEquals("three", results.get(0));

        results = Strings.substringsBetween("aabhellobabnonba", "ab", "ba");
        assertEquals(1, results.size());
        assertEquals("hello", results.get(0));

        results = Strings.substringsBetween("one, two, three", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one, two, three", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("one, two, three]", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], [three]", "[", null);
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], [three]", null, "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("[one], [two], [three]", "", "");
        assertEquals(0, results.size());

        results = Strings.substringsBetween(null, "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "[", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "", "]");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "[", "");
        assertEquals(0, results.size());

        results = Strings.substringsBetween("", "", "");
        assertEquals(0, results.size());
    }

    @Test
    public void test_replaceByPattern() {
        {
            N.println(Strings.repeat("=", 80));
            final String source = "sent email to xyz@gmail.com from 123@outlook.cn";

            String ret = RegExUtil.replaceAll(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), "xxx");
            N.println(ret);
            assertEquals("sent email to xxx from xxx", ret);

            ret = RegExUtil.replaceAll(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), it -> "xxx");
            N.println(ret);
            assertEquals("sent email to xxx from xxx", ret);

            ret = RegExUtil.replaceAll(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), (from, to) -> "xxxx");
            N.println(ret);
            assertEquals("sent email to xxxx from xxxx", ret);
        }

        {
            N.println(Strings.repeat("=", 80));
            final String source = "sent email to xyz@gmail.com from 123@outlook.cn";

            String ret = RegExUtil.replaceFirst(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), "xxx");
            N.println(ret);
            assertEquals("sent email to xxx from 123@outlook.cn", ret);

            ret = RegExUtil.replaceFirst(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), it -> "xxx");
            N.println(ret);
            assertEquals("sent email to xxx from 123@outlook.cn", ret);

            ret = RegExUtil.replaceFirst(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), (from, to) -> "xxxx");
            N.println(ret);
            assertEquals("sent email to xxxx from 123@outlook.cn", ret);

        }
        {
            N.println(Strings.repeat("=", 80));
            final String source = "sent email to xyz@gmail.com from 123@outlook.cn";

            String ret = RegExUtil.replaceLast(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), "xxx");
            N.println(ret);
            assertEquals("sent email to xyz@gmail.com from xxx", ret);

            ret = RegExUtil.replaceLast(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), it -> "xxx");
            N.println(ret);
            assertEquals("sent email to xyz@gmail.com from xxx", ret);

            ret = RegExUtil.replaceLast(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), (from, to) -> "xxxx");
            N.println(ret);
            assertEquals("sent email to xyz@gmail.com from xxxx", ret);
        }

        assertEquals(2, RegExUtil.countMatches("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern()));
        assertEquals(1, RegExUtil.countMatches("sent email to xyz@gmail.com from xxxx", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern()));
        assertEquals(0, RegExUtil.countMatches("sent email  from xxxx", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern()));

    }

    @Test
    @DisplayName("Test uuid() generates valid UUID")
    public void testUuid() {
        String uuid = Strings.uuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
    }

    @Test
    @DisplayName("Test uuid() generates unique UUIDs")
    public void testUuid_Unique() {
        String uuid1 = Strings.uuid();
        String uuid2 = Strings.uuid();
        assertFalse(uuid1.equals(uuid2));
    }

    @Test
    @DisplayName("Test guid() generates valid GUID")
    public void testGuid() {
        String guid = Strings.uuidWithoutHyphens();
        assertNotNull(guid);
        assertEquals(32, guid.length());
        assertFalse(guid.contains("-"));
        assertTrue(guid.matches("[a-f0-9]{32}"));
    }

    @Test
    @DisplayName("Test guid() generates unique GUIDs")
    public void testGuid_Unique() {
        String guid1 = Strings.uuidWithoutHyphens();
        String guid2 = Strings.uuidWithoutHyphens();
        assertFalse(guid1.equals(guid2));
    }

    @Test
    @DisplayName("Test valueOf() with null")
    public void testValueOf_Null() {
        assertNull(Strings.valueOf(null));
    }

    @Test
    @DisplayName("Test valueOf() with empty array")
    public void testValueOf_EmptyArray() {
        assertEquals("", Strings.valueOf(new char[0]));
    }

    @Test
    @DisplayName("Test valueOf() with char array")
    public void testValueOf_CharArray() {
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
        assertEquals("Hello World", Strings.valueOf("Hello World".toCharArray()));
    }

    @Test
    @DisplayName("Test isValidJavaIdentifier() with valid identifiers")
    public void testIsValidJavaIdentifier_Valid() {
        assertTrue(Strings.isValidJavaIdentifier("variable"));
        assertTrue(Strings.isValidJavaIdentifier("_var"));
        assertTrue(Strings.isValidJavaIdentifier("$var"));
        assertTrue(Strings.isValidJavaIdentifier("var123"));
        assertTrue(Strings.isValidJavaIdentifier("MAX_VALUE"));
        assertTrue(Strings.isValidJavaIdentifier("class"));
    }

    @Test
    @DisplayName("Test isValidJavaIdentifier() with invalid identifiers")
    public void testIsValidJavaIdentifier_Invalid() {
        assertFalse(Strings.isValidJavaIdentifier("123var"));
        assertFalse(Strings.isValidJavaIdentifier("var-name"));
        assertFalse(Strings.isValidJavaIdentifier("var name"));
        assertFalse(Strings.isValidJavaIdentifier(null));
        assertFalse(Strings.isValidJavaIdentifier(""));
        assertFalse(Strings.isValidJavaIdentifier("  "));
    }

    @Test
    @DisplayName("Test isKeyword() with Java keywords")
    public void testIsKeyword_Valid() {
        assertTrue(Strings.isKeyword("class"));
        assertTrue(Strings.isKeyword("public"));
        assertTrue(Strings.isKeyword("if"));
        assertTrue(Strings.isKeyword("return"));
        assertTrue(Strings.isKeyword("void"));
        assertTrue(Strings.isKeyword("abstract"));
    }

    @Test
    @DisplayName("Test isKeyword() with non-keywords")
    public void testIsKeyword_Invalid() {
        assertFalse(Strings.isKeyword("Class"));
        assertFalse(Strings.isKeyword("hello"));
        assertFalse(Strings.isKeyword("myVariable"));
        assertFalse(Strings.isKeyword(null));
        assertFalse(Strings.isKeyword(""));
    }

    @Test
    @DisplayName("Test isValidEmailAddress() with valid emails")
    public void testIsValidEmailAddress_Valid() {
        assertTrue(Strings.isValidEmailAddress("test@example.com"));
        assertTrue(Strings.isValidEmailAddress("user.name@example.co.uk"));
        assertTrue(Strings.isValidEmailAddress("user+tag@example.com"));
        assertTrue(Strings.isValidEmailAddress("admin@domain.org"));
    }

    @Test
    @DisplayName("Test isValidEmailAddress() with invalid emails")
    public void testIsValidEmailAddress_Invalid() {
        assertFalse(Strings.isValidEmailAddress("@example.com"));
        assertFalse(Strings.isValidEmailAddress("test@"));
        assertFalse(Strings.isValidEmailAddress("test"));
        assertFalse(Strings.isValidEmailAddress("test.example.com"));
        assertFalse(Strings.isValidEmailAddress(null));
        assertFalse(Strings.isValidEmailAddress(""));
    }

    @Test
    @DisplayName("Test isValidUrl() with valid URLs")
    public void testIsValidUrl_Valid() {
        assertTrue(Strings.isValidUrl("http://example.com"));
        assertTrue(Strings.isValidUrl("https://example.com"));
        assertTrue(Strings.isValidUrl("ftp://example.com"));
        assertTrue(Strings.isValidUrl("http://example.com:8080/path"));
        assertTrue(Strings.isValidUrl("file://C:/Users/test.txt"));
    }

    @Test
    @DisplayName("Test isValidUrl() with invalid URLs")
    public void testIsValidUrl_Invalid() {
        assertFalse(Strings.isValidUrl("not a url"));
        assertFalse(Strings.isValidUrl("www.example.com"));
        assertFalse(Strings.isValidUrl(null));
        assertFalse(Strings.isValidUrl(""));
    }

    @Test
    @DisplayName("Test isValidHttpUrl() with valid HTTP URLs")
    public void testIsValidHttpUrl_Valid() {
        assertTrue(Strings.isValidHttpUrl("http://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com:8443/path"));
        assertTrue(Strings.isValidHttpUrl("http://localhost:8080"));
    }

    @Test
    @DisplayName("Test isValidHttpUrl() with invalid HTTP URLs")
    public void testIsValidHttpUrl_Invalid() {
        assertFalse(Strings.isValidHttpUrl("ftp://example.com")); // not HTTP
        assertFalse(Strings.isValidHttpUrl("file:///C:/doc.txt")); // not HTTP
        assertFalse(Strings.isValidHttpUrl("www.example.com"));
        assertFalse(Strings.isValidHttpUrl(null));
        assertFalse(Strings.isValidHttpUrl(""));
    }

    @Test
    @DisplayName("Test isEmpty() with various inputs")
    public void testIsEmpty() {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("abc"));
        assertFalse(Strings.isEmpty("  abc  "));
        assertTrue(Strings.isEmpty(new StringBuilder()));
        assertFalse(Strings.isEmpty(new StringBuilder("test")));
    }

    @Test
    @DisplayName("Test isBlank() with various inputs")
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("   \t\n\r  "));
        assertTrue(Strings.isBlank("\t"));
        assertTrue(Strings.isBlank("\n"));
        assertFalse(Strings.isBlank("abc"));
        assertFalse(Strings.isBlank("  abc  "));
    }

    @Test
    @DisplayName("Test isNotEmpty() with various inputs")
    public void testIsNotEmpty() {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("abc"));
        assertTrue(Strings.isNotEmpty("  abc  "));
    }

    @Test
    @DisplayName("Test isNotBlank() with various inputs")
    public void testIsNotBlank() {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertFalse(Strings.isNotBlank("   \t\n\r  "));
        assertTrue(Strings.isNotBlank("abc"));
        assertTrue(Strings.isNotBlank("  abc  "));
    }

    @Test
    @DisplayName("Test isAllEmpty() with two arguments")
    public void testIsAllEmpty_TwoArgs() {
        assertTrue(Strings.isAllEmpty(null, null));
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertTrue(Strings.isAllEmpty("", null));
        assertFalse(Strings.isAllEmpty("abc", ""));
        assertFalse(Strings.isAllEmpty("", "xyz"));
        assertFalse(Strings.isAllEmpty("abc", "xyz"));
        assertFalse(Strings.isAllEmpty(" ", ""));
    }

    @Test
    @DisplayName("Test isAllEmpty() with three arguments")
    public void testIsAllEmpty_ThreeArgs() {
        assertTrue(Strings.isAllEmpty(null, null, null));
        assertTrue(Strings.isAllEmpty("", "", ""));
        assertTrue(Strings.isAllEmpty(null, "", null));
        assertFalse(Strings.isAllEmpty("abc", "", ""));
        assertFalse(Strings.isAllEmpty("", "xyz", ""));
        assertFalse(Strings.isAllEmpty("", "", "123"));
    }

    @Test
    @DisplayName("Test isAllEmpty() with varargs")
    public void testIsAllEmpty_VarArgs() {
        assertTrue(Strings.isAllEmpty());
        assertTrue(Strings.isAllEmpty((CharSequence[]) null));
        assertTrue(Strings.isAllEmpty(null, "", null));
        assertFalse(Strings.isAllEmpty(null, "foo", ""));
        assertFalse(Strings.isAllEmpty("", "bar", null));
        assertFalse(Strings.isAllEmpty(" ", "", null));
    }

    @Test
    @DisplayName("Test isAllEmpty() with Iterable")
    public void testIsAllEmpty_Iterable() {
        assertTrue(Strings.isAllEmpty((Iterable<CharSequence>) null));
        assertTrue(Strings.isAllEmpty(new ArrayList<>()));
        assertTrue(Strings.isAllEmpty(Arrays.asList(null, "", null)));
        assertFalse(Strings.isAllEmpty(Arrays.asList("", "abc", "")));
        assertFalse(Strings.isAllEmpty(Arrays.asList("abc")));
    }

    @Test
    @DisplayName("Test isAllBlank() with two arguments")
    public void testIsAllBlank_TwoArgs() {
        assertTrue(Strings.isAllBlank(null, null));
        assertTrue(Strings.isAllBlank("", ""));
        assertTrue(Strings.isAllBlank("   ", "\t\n"));
        assertTrue(Strings.isAllBlank(null, "   "));
        assertFalse(Strings.isAllBlank("abc", "   "));
        assertFalse(Strings.isAllBlank("   ", "xyz"));
    }

    @Test
    @DisplayName("Test isAllBlank() with three arguments")
    public void testIsAllBlank_ThreeArgs() {
        assertTrue(Strings.isAllBlank(null, null, null));
        assertTrue(Strings.isAllBlank("", "", ""));
        assertTrue(Strings.isAllBlank("   ", "\t", "\n"));
        assertFalse(Strings.isAllBlank("abc", "   ", ""));
        assertFalse(Strings.isAllBlank("", "xyz", "   "));
    }

    @Test
    @DisplayName("Test isAllBlank() with varargs")
    public void testIsAllBlank_VarArgs() {
        assertTrue(Strings.isAllBlank());
        assertTrue(Strings.isAllBlank((CharSequence[]) null));
        assertTrue(Strings.isAllBlank(null, "", "  "));
        assertFalse(Strings.isAllBlank(null, "foo", "  "));
        assertFalse(Strings.isAllBlank("  ", "bar", null));
    }

    @Test
    @DisplayName("Test isAllBlank() with Iterable")
    public void testIsAllBlank_Iterable() {
        assertTrue(Strings.isAllBlank((Iterable<CharSequence>) null));
        assertTrue(Strings.isAllBlank(new ArrayList<>()));
        assertTrue(Strings.isAllBlank(Arrays.asList(null, "", "   ")));
        assertFalse(Strings.isAllBlank(Arrays.asList("   ", "abc", "")));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with two arguments")
    public void testIsAnyEmpty_TwoArgs() {
        assertTrue(Strings.isAnyEmpty(null, null));
        assertTrue(Strings.isAnyEmpty("", ""));
        assertTrue(Strings.isAnyEmpty("abc", ""));
        assertTrue(Strings.isAnyEmpty("", "xyz"));
        assertTrue(Strings.isAnyEmpty(null, "xyz"));
        assertFalse(Strings.isAnyEmpty("abc", "xyz"));
        assertFalse(Strings.isAnyEmpty("   ", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with three arguments")
    public void testIsAnyEmpty_ThreeArgs() {
        assertTrue(Strings.isAnyEmpty(null, null, null));
        assertTrue(Strings.isAnyEmpty("abc", "", "xyz"));
        assertTrue(Strings.isAnyEmpty("", "def", "xyz"));
        assertTrue(Strings.isAnyEmpty("abc", "def", null));
        assertFalse(Strings.isAnyEmpty("abc", "def", "xyz"));
        assertFalse(Strings.isAnyEmpty("   ", "def", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with varargs")
    public void testIsAnyEmpty_VarArgs() {
        assertFalse(Strings.isAnyEmpty());
        assertFalse(Strings.isAnyEmpty((CharSequence[]) null));
        assertTrue(Strings.isAnyEmpty(null, "foo"));
        assertTrue(Strings.isAnyEmpty("", "bar"));
        assertTrue(Strings.isAnyEmpty("bob", ""));
        assertTrue(Strings.isAnyEmpty("  bob  ", null));
        assertFalse(Strings.isAnyEmpty(" ", "bar"));
        assertFalse(Strings.isAnyEmpty("foo", "bar"));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with Iterable")
    public void testIsAnyEmpty_Iterable() {
        assertFalse(Strings.isAnyEmpty((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyEmpty(new ArrayList<>()));
        assertTrue(Strings.isAnyEmpty(Arrays.asList("abc", "", "xyz")));
        assertFalse(Strings.isAnyEmpty(Arrays.asList("abc", "def", "xyz")));
    }

    @Test
    @DisplayName("Test isAnyBlank() with two arguments")
    public void testIsAnyBlank_TwoArgs() {
        assertTrue(Strings.isAnyBlank(null, null));
        assertTrue(Strings.isAnyBlank("", ""));
        assertTrue(Strings.isAnyBlank("   ", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "\t\n"));
        assertTrue(Strings.isAnyBlank(null, "xyz"));
        assertFalse(Strings.isAnyBlank("abc", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyBlank() with three arguments")
    public void testIsAnyBlank_ThreeArgs() {
        assertTrue(Strings.isAnyBlank(null, null, null));
        assertTrue(Strings.isAnyBlank("abc", "   ", "xyz"));
        assertTrue(Strings.isAnyBlank("", "def", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "def", null));
        assertFalse(Strings.isAnyBlank("abc", "def", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyBlank() with varargs")
    public void testIsAnyBlank_VarArgs() {
        assertFalse(Strings.isAnyBlank());
        assertFalse(Strings.isAnyBlank((CharSequence[]) null));
        assertTrue(Strings.isAnyBlank(null, "foo"));
        assertTrue(Strings.isAnyBlank("", "bar"));
        assertTrue(Strings.isAnyBlank("  bob  ", null));
        assertTrue(Strings.isAnyBlank(" ", "bar"));
        assertFalse(Strings.isAnyBlank("foo", "bar"));
    }

    @Test
    @DisplayName("Test isAnyBlank() with Iterable")
    public void testIsAnyBlank_Iterable() {
        assertFalse(Strings.isAnyBlank((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyBlank(new ArrayList<>()));
        assertTrue(Strings.isAnyBlank(Arrays.asList("abc", "   ", "xyz")));
        assertFalse(Strings.isAnyBlank(Arrays.asList("abc", "def", "xyz")));
    }

    @Test
    @DisplayName("Test isWrappedWith() with same prefix/suffix")
    public void testIsWrappedWith_SamePrefixSuffix() {
        assertTrue(Strings.isWrappedWith("'hello'", "'"));
        assertTrue(Strings.isWrappedWith("\"text\"", "\""));
        assertTrue(Strings.isWrappedWith("--comment--", "--"));
        assertFalse(Strings.isWrappedWith("hello", "'"));
        assertFalse(Strings.isWrappedWith("'hello\"", "'"));
        assertFalse(Strings.isWrappedWith(null, "'"));
        assertFalse(Strings.isWrappedWith("''", "''"));
    }

    @Test
    @DisplayName("Test isWrappedWith() throws on empty prefix/suffix")
    public void testIsWrappedWith_EmptyPrefixSuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", ""));
    }

    @Test
    @DisplayName("Test isWrappedWith() with different prefix and suffix")
    public void testIsWrappedWith_DifferentPrefixSuffix() {
        assertTrue(Strings.isWrappedWith("<html>content</html>", "<html>", "</html>"));
        assertTrue(Strings.isWrappedWith("{data}", "{", "}"));
        assertTrue(Strings.isWrappedWith("[array]", "[", "]"));
        assertFalse(Strings.isWrappedWith("hello", "<", ">"));
        assertFalse(Strings.isWrappedWith("<hello", "<", ">"));
        assertFalse(Strings.isWrappedWith(null, "<", ">"));
    }

    @Test
    @DisplayName("Test isWrappedWith() throws on empty prefix or suffix")
    public void testIsWrappedWith_EmptyPrefixOrSuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "", ">"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "<", ""));
    }

    @Test
    @DisplayName("Test defaultIfNull() returns value when not null")
    public void testDefaultIfNull_NotNull() {
        assertEquals("hello", Strings.defaultIfNull("hello", "default"));
        assertEquals("", Strings.defaultIfNull("", "default"));
        assertEquals("   ", Strings.defaultIfNull("   ", "default"));
    }

    @Test
    @DisplayName("Test defaultIfNull() returns default when null")
    public void testDefaultIfNull_Null() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
    }

    @Test
    @DisplayName("Test defaultIfNull() throws when default is null")
    public void testDefaultIfNull_NullDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((String) null, (String) null));
    }

    @Test
    @DisplayName("Test defaultIfNull() with Supplier")
    public void testDefaultIfNull_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfNull("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfNull(null, () -> "default"));
    }

    @Test
    @DisplayName("Test defaultIfNull() with Supplier throws when default is null")
    public void testDefaultIfNull_SupplierNullDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.<String> defaultIfNull(null, () -> null));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() returns value when not empty")
    public void testDefaultIfEmpty_NotEmpty() {
        assertEquals("hello", Strings.defaultIfEmpty("hello", "default"));
        assertEquals("   ", Strings.defaultIfEmpty("   ", "default"));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() returns default when empty")
    public void testDefaultIfEmpty_Empty() {
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() throws when default is empty")
    public void testDefaultIfEmpty_EmptyDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, (String) null));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() with Supplier")
    public void testDefaultIfEmpty_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfEmpty("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty("", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty(null, () -> "default"));
    }

    @Test
    @DisplayName("Test defaultIfBlank() returns value when not blank")
    public void testDefaultIfBlank_NotBlank() {
        assertEquals("hello", Strings.defaultIfBlank("hello", "default"));
        assertEquals("  abc  ", Strings.defaultIfBlank("  abc  ", "default"));
    }

    @Test
    @DisplayName("Test defaultIfBlank() returns default when blank")
    public void testDefaultIfBlank_Blank() {
        assertEquals("default", Strings.defaultIfBlank("   ", "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
    }

    @Test
    @DisplayName("Test defaultIfBlank() throws when default is blank")
    public void testDefaultIfBlank_BlankDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, "  "));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, (String) null));
    }

    @Test
    @DisplayName("Test defaultIfBlank() with Supplier")
    public void testDefaultIfBlank_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfBlank("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank("   ", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank(null, () -> "default"));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with two arguments")
    public void testFirstNonEmpty_TwoArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world"));
        assertEquals("world", Strings.firstNonEmpty("", "world"));
        assertEquals("world", Strings.firstNonEmpty(null, "world"));
        assertEquals("", Strings.firstNonEmpty("", ""));
        assertEquals("", Strings.firstNonEmpty(null, null));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with three arguments")
    public void testFirstNonEmpty_ThreeArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world", "!"));
        assertEquals("world", Strings.firstNonEmpty("", "world", "!"));
        assertEquals("!", Strings.firstNonEmpty("", "", "!"));
        assertEquals("!", Strings.firstNonEmpty(null, null, "!"));
        assertEquals("", Strings.firstNonEmpty("", "", ""));
        assertEquals("", Strings.firstNonEmpty(null, null, null));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with varargs")
    public void testFirstNonEmpty_VarArgs() {
        assertEquals("", Strings.firstNonEmpty());
        assertEquals("", Strings.firstNonEmpty((String[]) null));
        assertEquals("", Strings.firstNonEmpty(null, null, null));
        assertEquals(" ", Strings.firstNonEmpty(null, "", " "));
        assertEquals("abc", Strings.firstNonEmpty("abc"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty("", "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz", "abc"));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with Iterable")
    public void testFirstNonEmpty_Iterable() {
        assertEquals("", Strings.firstNonEmpty((Iterable<String>) null));
        assertEquals("", Strings.firstNonEmpty(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonEmpty(Arrays.asList("", null, "hello")));
        assertEquals("", Strings.firstNonEmpty(Arrays.asList("", null, "")));
    }

    @Test
    @DisplayName("Test firstNonBlank() with two arguments")
    public void testFirstNonBlank_TwoArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world"));
        assertEquals("world", Strings.firstNonBlank("   ", "world"));
        assertEquals("world", Strings.firstNonBlank(null, "world"));
        assertEquals("", Strings.firstNonBlank("", ""));
        assertEquals("", Strings.firstNonBlank(null, null));
        assertEquals("", Strings.firstNonBlank("  ", "  "));
    }

    @Test
    @DisplayName("Test firstNonBlank() with three arguments")
    public void testFirstNonBlank_ThreeArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world", "!"));
        assertEquals("world", Strings.firstNonBlank("   ", "world", "!"));
        assertEquals("!", Strings.firstNonBlank("  ", "", "!"));
        assertEquals("!", Strings.firstNonBlank(null, null, "!"));
        assertEquals("", Strings.firstNonBlank("", "", ""));
    }

    @Test
    @DisplayName("Test firstNonBlank() with varargs")
    public void testFirstNonBlank_VarArgs() {
        assertEquals("", Strings.firstNonBlank());
        assertEquals("", Strings.firstNonBlank((String[]) null));
        assertEquals("abc", Strings.firstNonBlank(null, "  ", "abc"));
        assertEquals("", Strings.firstNonBlank(null, "", "   "));
    }

    @Test
    @DisplayName("Test firstNonBlank() with Iterable")
    public void testFirstNonBlank_Iterable() {
        assertEquals("", Strings.firstNonBlank((Iterable<String>) null));
        assertEquals("", Strings.firstNonBlank(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonBlank(Arrays.asList("   ", null, "hello")));
        assertEquals("", Strings.firstNonBlank(Arrays.asList("  ", null, "")));
    }

    @Test
    @DisplayName("Test nullToEmpty()")
    public void testNullToEmpty() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
        assertEquals("   ", Strings.nullToEmpty("   "));
    }

    @Test
    @DisplayName("Test nullElementsToEmpty() with array")
    public void testNullElementsToEmpty_Array() {
        String[] array = { null, "abc", null, "xyz" };
        Strings.nullElementsToEmpty(array);
        assertArrayEquals(new String[] { "", "abc", "", "xyz" }, array);

        String[] emptyArray = {};
        Strings.nullElementsToEmpty(emptyArray);
        assertArrayEquals(new String[] {}, emptyArray);
    }

    @Test
    @DisplayName("Test emptyToNull()")
    public void testEmptyToNull() {
        assertNull(Strings.emptyToNull(""));
        assertNull(Strings.emptyToNull((String) null));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals("   ", Strings.emptyToNull("   "));
    }

    @Test
    @DisplayName("Test emptyElementsToNull() with array")
    public void testEmptyElementsToNull_Array() {
        String[] array = { "", "abc", "", "xyz" };
        Strings.emptyElementsToNull(array);
        assertArrayEquals(new String[] { null, "abc", null, "xyz" }, array);
    }

    @Test
    @DisplayName("Test blankToEmpty()")
    public void testBlankToEmpty() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
        assertEquals("  abc  ", Strings.blankToEmpty("  abc  "));
    }

    @Test
    @DisplayName("Test blankElementsToEmpty() with array")
    public void testBlankElementsToEmpty_Array() {
        String[] array = { null, "abc", "  ", "xyz" };
        Strings.blankElementsToEmpty(array);
        assertArrayEquals(new String[] { "", "abc", "", "xyz" }, array);
    }

    @Test
    @DisplayName("Test blankToNull()")
    public void testBlankToNull() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
        assertEquals("  abc  ", Strings.blankToNull("  abc  "));
    }

    @Test
    @DisplayName("Test blankElementsToNull() with array")
    public void testBlankElementsToNull_Array() {
        String[] array = { "  ", "abc", "", "xyz" };
        Strings.blankElementsToNull(array);
        assertArrayEquals(new String[] { null, "abc", null, "xyz" }, array);
    }

    @Test
    @DisplayName("Test abbreviate() with normal cases")
    public void testAbbreviate() {
        assertEquals("abc", Strings.abbreviate("abc", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", 5));
        assertNull(Strings.abbreviate(null, 5));
        assertEquals("", Strings.abbreviate("", 5));
    }

    @Test
    @DisplayName("Test abbreviate() with custom marker")
    public void testAbbreviate_WithMarker() {
        assertEquals("abc", Strings.abbreviate("abc", "...", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", "...", 5));
        assertEquals("abcd*", Strings.abbreviate("abcdefg", "*", 5));
        assertNull(Strings.abbreviate(null, "...", 5));
    }

    @Test
    @DisplayName("Test abbreviateMiddle()")
    public void testAbbreviateMiddle() {
        assertEquals("abc...xyz", Strings.abbreviateMiddle("abcdefghijklmnopqrstuvwxyz", "...", 9));
        assertEquals("abc", Strings.abbreviateMiddle("abc", "...", 10));
        assertNull(Strings.abbreviateMiddle(null, "...", 5));
    }

    @Test
    @DisplayName("Test center() with default space")
    public void testCenter() {
        assertEquals("  abc  ", Strings.center("abc", 7));
        assertEquals(" abc  ", Strings.center("abc", 6));
        assertEquals("abc", Strings.center("abc", 3));
        assertEquals("abc", Strings.center("abc", 2));
        assertEquals("   ", Strings.center(null, 3));
    }

    @Test
    @DisplayName("Test center() with custom pad char")
    public void testCenter_WithChar() {
        assertEquals("**abc**", Strings.center("abc", 7, '*'));
        assertEquals("*abc**", Strings.center("abc", 6, '*'));
        assertEquals("abc", Strings.center("abc", 3, '*'));
    }

    @Test
    @DisplayName("Test center() with custom pad string")
    public void testCenter_WithString() {
        assertEquals("--abc--", Strings.center("abc", 7, "--"));
        assertEquals("-abc--", Strings.center("abc", 6, "-"));
        assertEquals("abc", Strings.center("abc", 3, "-"));
        assertEquals("-----", Strings.center(null, 5, "-"));
    }

    @Test
    @DisplayName("Test padStart() with default space")
    public void testPadStart() {
        assertEquals("  abc", Strings.padStart("abc", 5));
        assertEquals("abc", Strings.padStart("abc", 3));
        assertEquals("abc", Strings.padStart("abc", 2));
        assertEquals("     ", Strings.padStart(null, 5));
    }

    @Test
    @DisplayName("Test padStart() with custom char")
    public void testPadStart_WithChar() {
        assertEquals("00abc", Strings.padStart("abc", 5, '0'));
        assertEquals("abc", Strings.padStart("abc", 3, '0'));
    }

    @Test
    @DisplayName("Test padStart() with custom string")
    public void testPadStart_WithString() {
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
        assertEquals("abc", Strings.padStart("abc", 3, "-"));
    }

    @Test
    @DisplayName("Test padEnd() with default space")
    public void testPadEnd() {
        assertEquals("abc  ", Strings.padEnd("abc", 5));
        assertEquals("abc", Strings.padEnd("abc", 3));
        assertEquals("abc", Strings.padEnd("abc", 2));
        assertEquals("     ", Strings.padEnd(null, 5));
    }

    @Test
    @DisplayName("Test padEnd() with custom char")
    public void testPadEnd_WithChar() {
        assertEquals("abc00", Strings.padEnd("abc", 5, '0'));
        assertEquals("abc", Strings.padEnd("abc", 3, '0'));
    }

    @Test
    @DisplayName("Test padEnd() with custom string")
    public void testPadEnd_WithString() {
        assertEquals("abc--", Strings.padEnd("abc", 5, "--"));
        assertEquals("abc", Strings.padEnd("abc", 3, "-"));
    }

    @Test
    @DisplayName("Test repeat() with char")
    public void testRepeat_Char() {
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat('a', 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    @DisplayName("Test repeat() with char and delimiter")
    public void testRepeat_CharWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("a", Strings.repeat('a', 1, ','));
        assertEquals("", Strings.repeat('a', 0, ','));
    }

    @Test
    @DisplayName("Test repeat() with string")
    public void testRepeat_String() {
        assertEquals("abcabcabc", Strings.repeat("abc", 3));
        assertEquals("", Strings.repeat("abc", 0));
        assertEquals("", Strings.repeat(null, 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("abc", -1));
    }

    @Test
    @DisplayName("Test repeat() with string and delimiter")
    public void testRepeat_StringWithDelimiter() {
        assertEquals("abc,abc,abc", Strings.repeat("abc", 3, ","));
        assertEquals("abc", Strings.repeat("abc", 1, ","));
        assertEquals("", Strings.repeat("abc", 0, ","));
    }

    @Test
    @DisplayName("Test repeat() with prefix and suffix")
    public void testRepeat_WithPrefixSuffix() {
        assertEquals("[abc,abc,abc]", Strings.repeat("abc", 3, ",", "[", "]"));
        assertEquals("[abc]", Strings.repeat("abc", 1, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("abc", 0, ",", "[", "]"));
    }

    @Test
    @DisplayName("Test getBytes() with default encoding")
    public void testGetBytes() {
        assertArrayEquals("abc".getBytes(), Strings.getBytes("abc"));
        assertNull(Strings.getBytes(null));
    }

    @Test
    @DisplayName("Test getBytes() with charset")
    public void testGetBytes_WithCharset() {
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Strings.getBytes("abc", StandardCharsets.UTF_8));
        assertNull(Strings.getBytes(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test getBytesUtf8()")
    public void testGetBytesUtf8() {
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8("abc"));
        assertNull(Strings.getBytesUtf8(null));
    }

    @Test
    @DisplayName("Test toCharArray()")
    public void testToCharArray() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Strings.toCharArray("abc"));
        assertArrayEquals(new char[0], Strings.toCharArray(""));
        assertNull(Strings.toCharArray(null));
    }

    @Test
    @DisplayName("Test toLowerCase() with char")
    public void testToLowerCase_Char() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals('z', Strings.toLowerCase('Z'));
        assertEquals('a', Strings.toLowerCase('a'));
        assertEquals('1', Strings.toLowerCase('1'));
    }

    @Test
    @DisplayName("Test toLowerCase() with string")
    public void testToLowerCase_String() {
        assertEquals("abc", Strings.toLowerCase("ABC"));
        assertEquals("abc", Strings.toLowerCase("abc"));
        assertEquals("abc123", Strings.toLowerCase("ABC123"));
        assertNull(Strings.toLowerCase(null));
        assertEquals("", Strings.toLowerCase(""));
    }

    @Test
    @DisplayName("Test toLowerCase() with locale")
    public void testToLowerCase_WithLocale() {
        assertEquals("abc", Strings.toLowerCase("ABC", Locale.ENGLISH));
        assertNull(Strings.toLowerCase(null, Locale.ENGLISH));
    }

    @Test
    @DisplayName("Test toSnakeCase()")
    public void testToSnakeCase() {
        assertEquals("hello_world", Strings.toSnakeCase("HelloWorld"));
        assertEquals("hello_world", Strings.toSnakeCase("helloWorld"));
        assertEquals("abc", Strings.toSnakeCase("abc"));
        assertNull(Strings.toSnakeCase(null));
    }

    @Test
    @DisplayName("Test toUpperCase() with char")
    public void testToUpperCase_Char() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals('Z', Strings.toUpperCase('z'));
        assertEquals('A', Strings.toUpperCase('A'));
        assertEquals('1', Strings.toUpperCase('1'));
    }

    @Test
    @DisplayName("Test toUpperCase() with string")
    public void testToUpperCase_String() {
        assertEquals("ABC", Strings.toUpperCase("abc"));
        assertEquals("ABC", Strings.toUpperCase("ABC"));
        assertEquals("ABC123", Strings.toUpperCase("abc123"));
        assertNull(Strings.toUpperCase(null));
        assertEquals("", Strings.toUpperCase(""));
    }

    @Test
    @DisplayName("Test toUpperCase() with locale")
    public void testToUpperCase_WithLocale() {
        assertEquals("ABC", Strings.toUpperCase("abc", Locale.ENGLISH));
        assertNull(Strings.toUpperCase(null, Locale.ENGLISH));
    }

    @Test
    @DisplayName("Test toScreamingSnakeCase()")
    public void testToScreamingSnakeCase() {
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("HelloWorld"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("helloWorld"));
        assertEquals("ABC", Strings.toScreamingSnakeCase("ABC"));
        assertNull(Strings.toScreamingSnakeCase(null));
    }

    @Test
    @DisplayName("Test toCamelCase()")
    public void testToCamelCase() {
        assertEquals("helloWorld", Strings.toCamelCase("hello_world"));
        assertEquals("helloWorld", Strings.toCamelCase("HELLO_WORLD"));
        assertEquals("helloWorld", Strings.toCamelCase("hello-world"));
        assertNull(Strings.toCamelCase(null));
        assertEquals("", Strings.toCamelCase(""));
    }

    @Test
    @DisplayName("Test toCamelCase() with custom split char")
    public void testToCamelCase_WithSplitChar() {
        assertEquals("helloWorld", Strings.toCamelCase("hello_world", '_'));
        assertEquals("helloWorld", Strings.toCamelCase("hello-world", '-'));
        assertNull(Strings.toCamelCase(null, '_'));
    }

    @Test
    @DisplayName("Test toUpperCamelCase()")
    public void testToUpperCamelCase() {
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello_world"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("HELLO_WORLD"));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello-world"));
        assertNull(Strings.toUpperCamelCase(null));
        assertEquals("", Strings.toUpperCamelCase(""));
    }

    @Test
    @DisplayName("Test toUpperCamelCase() with custom split char")
    public void testToUpperCamelCase_WithSplitChar() {
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello_world", '_'));
        assertEquals("HelloWorld", Strings.toUpperCamelCase("hello-world", '-'));
        assertNull(Strings.toUpperCamelCase(null, '_'));
    }

    @Test
    @DisplayName("Test swapCase() with char")
    public void testSwapCase_Char() {
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('a', Strings.swapCase('A'));
        assertEquals('1', Strings.swapCase('1'));
    }

    @Test
    @DisplayName("Test swapCase() with string")
    public void testSwapCase_String() {
        assertEquals("ABC", Strings.swapCase("abc"));
        assertEquals("abc", Strings.swapCase("ABC"));
        assertEquals("AbC", Strings.swapCase("aBc"));
        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
    }

    @Test
    @DisplayName("Test capitalize()")
    public void testCapitalize() {
        assertEquals("Abc", Strings.capitalize("abc"));
        assertEquals("Abc", Strings.capitalize("Abc"));
        assertEquals("ABC", Strings.capitalize("ABC"));
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
    }

    @Test
    @DisplayName("Test uncapitalize()")
    public void testUncapitalize() {
        assertEquals("abc", Strings.uncapitalize("Abc"));
        assertEquals("abc", Strings.uncapitalize("abc"));
        assertEquals("aBC", Strings.uncapitalize("ABC"));
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
    }

    @Test
    @DisplayName("Test capitalizeFully()")
    public void testCapitalizeFully() {
        assertEquals("Abc Def", Strings.capitalizeFully("abc def"));
        assertEquals("ABC DEF", Strings.capitalizeFully("ABC DEF"));
        assertNull(Strings.capitalizeFully(null));
        assertEquals("", Strings.capitalizeFully(""));
    }

    @Test
    @DisplayName("Test capitalizeFully() with delimiter")
    public void testCapitalizeFully_WithDelimiter() {
        assertEquals("Abc-Def", Strings.capitalizeFully("abc-def", "-"));
        assertEquals("ABC_DEF", Strings.capitalizeFully("ABC_DEF", "_"));
        assertNull(Strings.capitalizeFully(null, "-"));
    }

    @Test
    @DisplayName("Test capitalizeFully() with excluded words")
    public void testCapitalizeFully_WithExcludedWords() {
        assertEquals("The Quick Brown Fox", Strings.capitalizeFully("the quick brown fox", " ", "the"));
        assertEquals("Hello and Goodbye", Strings.capitalizeFully("hello and goodbye", " ", "and"));
    }

    @Test
    @DisplayName("Test capitalizeFully() with excluded words collection")
    public void testCapitalizeFully_WithExcludedWordsCollection() {
        List<String> excludedWords = Arrays.asList("the", "and", "or");
        assertEquals("The Quick Brown Fox", Strings.capitalizeFully("the quick brown fox", " ", excludedWords));
    }

    @Test
    @DisplayName("Test quoteEscaped() with default quote")
    public void testQuoteEscaped() {
        assertEquals("\\\"hello\\\"", Strings.quoteEscaped("\"hello\""));
        assertEquals("abc", Strings.quoteEscaped("abc"));
        assertNull(Strings.quoteEscaped(null));
    }

    @Test
    @DisplayName("Test quoteEscaped() with custom quote char")
    public void testQuoteEscaped_WithQuoteChar() {
        assertEquals("\\'hello\\'", Strings.quoteEscaped("'hello'", '\''));
        assertEquals("abc", Strings.quoteEscaped("abc", '\''));
        assertNull(Strings.quoteEscaped(null, '\''));
    }

    @Test
    @DisplayName("Test unicodeEscaped()")
    public void testUnicodeEscaped() {
        assertEquals("\\u0041", Strings.unicodeEscaped('A'));
        assertEquals("\\u0061", Strings.unicodeEscaped('a'));
        assertEquals("\\u0031", Strings.unicodeEscaped('1'));
    }

    @Test
    @DisplayName("Test normalizeSpace()")
    public void testNormalizeSpace() {
        assertEquals("a b c", Strings.normalizeSpace("a  b   c"));
        assertEquals("abc", Strings.normalizeSpace("abc"));
        assertEquals("a b", Strings.normalizeSpace("  a  b  "));
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
    }

    @Test
    @DisplayName("Test replaceAll()")
    public void testReplaceAll() {
        assertEquals("xbcxbc", Strings.replaceAll("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceAll("abc", "x", "y"));
        assertNull(Strings.replaceAll(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceAll() with fromIndex")
    public void testReplaceAll_WithFromIndex() {
        assertEquals("abcxbc", Strings.replaceAll("abcabc", 3, "a", "x"));
        assertEquals("abcabc", Strings.replaceAll("abcabc", 0, "x", "y"));
    }

    @Test
    @DisplayName("Test replaceFirst()")
    public void testReplaceFirst() {
        assertEquals("xbcabc", Strings.replaceFirst("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceFirst("abc", "x", "y"));
        assertNull(Strings.replaceFirst(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceFirst() with fromIndex")
    public void testReplaceFirst_WithFromIndex() {
        assertEquals("abcxbc", Strings.replaceFirst("abcabc", 3, "a", "x"));
        assertEquals("abcabc", Strings.replaceFirst("abcabc", 0, "x", "y"));
    }

    @Test
    @DisplayName("Test replaceOnce()")
    public void testReplaceOnce() {
        assertEquals("xbcabc", Strings.replaceOnce("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceOnce("abc", "x", "y"));
        assertNull(Strings.replaceOnce(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceLast()")
    public void testReplaceLast() {
        assertEquals("abcxbc", Strings.replaceLast("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceLast("abc", "x", "y"));
        assertNull(Strings.replaceLast(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replace() with max")
    public void testReplace_WithMax() {
        assertEquals("xbcxbc", Strings.replace("abcabc", 0, "a", "x", 2));
        assertEquals("xbcabc", Strings.replace("abcabc", 0, "a", "x", 1));
        assertEquals("abcabc", Strings.replace("abcabc", 0, "a", "x", 0));
    }

    @Test
    @DisplayName("Test replaceAllIgnoreCase()")
    public void testReplaceAllIgnoreCase() {
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceAllIgnoreCase(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceFirstIgnoreCase()")
    public void testReplaceFirstIgnoreCase() {
        assertEquals("xbcAbc", Strings.replaceFirstIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcabc", Strings.replaceFirstIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceFirstIgnoreCase(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replace() with fromIndex and toIndex")
    public void testReplace_WithIndices() {
        assertEquals("abXYZfg", Strings.replace("abcdefg", 2, 5, "XYZ"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replace(null, 0, 1, "X"));
    }

    @Test
    @DisplayName("Test replaceBetween()")
    public void testReplaceBetween() {
        assertEquals("abc[REPLACED]xyz", Strings.replaceBetween("abc[old]xyz", "[", "]", "REPLACED"));
        assertNull(Strings.replaceBetween(null, "[", "]", "X"));
    }

    @Test
    @DisplayName("Test replaceAfter()")
    public void testReplaceAfter() {
        assertEquals("abc:REPLACED", Strings.replaceAfter("abc:old", ":", "REPLACED"));
        assertNull(Strings.replaceAfter(null, ":", "X"));
    }

    @Test
    @DisplayName("Test replaceBefore()")
    public void testReplaceBefore() {
        assertEquals("REPLACED:xyz", Strings.replaceBefore("old:xyz", ":", "REPLACED"));
        assertNull(Strings.replaceBefore(null, ":", "X"));
    }

    @Test
    @DisplayName("Test removeStart()")
    public void testRemoveStart() {
        assertEquals("bc", Strings.removeStart("abc", "a"));
        assertEquals("abc", Strings.removeStart("abc", "x"));
        assertEquals("", Strings.removeStart("abc", "abc"));
        assertNull(Strings.removeStart(null, "a"));
    }

    @Test
    @DisplayName("Test removeStartIgnoreCase()")
    public void testRemoveStartIgnoreCase() {
        assertEquals("bc", Strings.removeStartIgnoreCase("abc", "A"));
        assertEquals("bc", Strings.removeStartIgnoreCase("Abc", "a"));
        assertNull(Strings.removeStartIgnoreCase(null, "a"));
    }

    @Test
    @DisplayName("Test removeEnd()")
    public void testRemoveEnd() {
        assertEquals("ab", Strings.removeEnd("abc", "c"));
        assertEquals("abc", Strings.removeEnd("abc", "x"));
        assertEquals("", Strings.removeEnd("abc", "abc"));
        assertEquals(null, Strings.removeEnd(null, "c"));
    }

    @Test
    @DisplayName("Test removeEndIgnoreCase()")
    public void testRemoveEndIgnoreCase() {
        assertEquals("ab", Strings.removeEndIgnoreCase("abc", "C"));
        assertEquals("ab", Strings.removeEndIgnoreCase("abC", "c"));
        assertNull(Strings.removeEndIgnoreCase(null, "c"));
    }

    @Test
    @DisplayName("Test removeAll() with char")
    public void testRemoveAll_Char() {
        assertEquals("bc", Strings.removeAll("abc", 'a'));
        assertEquals("", Strings.removeAll("aaa", 'a'));
        assertEquals("abc", Strings.removeAll("abc", 'x'));
        assertNull(Strings.removeAll(null, 'a'));
    }

    @Test
    @DisplayName("Test removeAll() with char and fromIndex")
    public void testRemoveAll_CharWithFromIndex() {
        assertEquals("abc", Strings.removeAll("abcaa", 3, 'a'));
        assertEquals("abc", Strings.removeAll("abc", 0, 'x'));
    }

    @Test
    @DisplayName("Test removeAll() with string")
    public void testRemoveAll_String() {
        assertEquals("cde", Strings.removeAll("ababcde", "ab"));
        assertEquals("abc", Strings.removeAll("abc", "xy"));
        assertNull(Strings.removeAll(null, "ab"));
    }

    @Test
    @DisplayName("Test removeAll() with string and fromIndex")
    public void testRemoveAll_StringWithFromIndex() {
        assertEquals("ababcde", Strings.removeAll("ababcde", 4, "ab"));
        assertEquals("abc", Strings.removeAll("abc", 0, "xy"));
    }

    @Test
    @DisplayName("Test split() with char delimiter")
    public void testSplit_Char() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ','));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", ','));
        assertArrayEquals(new String[] {}, Strings.split(null, ','));
    }

    @Test
    @DisplayName("Test split() with char delimiter and trim")
    public void testSplit_CharWithTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ',', false));
    }

    @Test
    @DisplayName("Test split() with string delimiter")
    public void testSplit_String() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a::b::c", "::"));
        assertArrayEquals(new String[] {}, Strings.split(null, ","));
    }

    @Test
    @DisplayName("Test split() with string delimiter and trim")
    public void testSplit_StringWithTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ",", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ",", false));
    }

    @Test
    @DisplayName("Test split() with max")
    public void testSplit_WithMax() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ",", 3));
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a,b,c" }, Strings.split("a,b,c", ",", 1));
    }

    @Test
    @DisplayName("Test split() with max and trim")
    public void testSplit_WithMaxAndTrim() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split(" a , b,c ", ",", 2, true));
        assertArrayEquals(new String[] { " a ", " b,c " }, Strings.split(" a , b,c ", ",", 2, false));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with char")
    public void testSplitPreserveAllTokens_Char() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ','));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ','));
        assertArrayEquals(new String[] { "", "a" }, Strings.splitPreserveAllTokens(",a", ','));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with char and trim")
    public void testSplitPreserveAllTokens_CharWithTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ',', true));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with string")
    public void testSplitPreserveAllTokens_String() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ","));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with max")
    public void testSplitPreserveAllTokens_WithMax() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.splitPreserveAllTokens("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a", ",c" }, Strings.splitPreserveAllTokens("a,,c", ",", 2));
    }

    @Test
    @DisplayName("Test splitToLines()")
    public void testSplitToLines() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\nb\nc"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\r\nb"));
        assertArrayEquals(new String[] { "" }, Strings.splitToLines(""));
        assertArrayEquals(new String[] {}, Strings.splitToLines(null));
    }

    @Test
    @DisplayName("Test splitToLines() with trim and omitEmpty")
    public void testSplitToLines_WithOptions() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines(" a \n b \n c ", true, false));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\n\nb", false, true));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines(" a \n\n b ", true, true));
    }

    @Test
    @DisplayName("Test trim()")
    public void testTrim() {
        assertEquals("abc", Strings.trim("  abc  "));
        assertEquals("abc", Strings.trim("abc"));
        assertEquals("", Strings.trim("   "));
        assertNull(Strings.trim((String) null));
    }

    @Test
    @DisplayName("Test trim() with array")
    public void testTrim_Array() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.trim(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    @DisplayName("Test trimToNull()")
    public void testTrimToNull() {
        assertEquals("abc", Strings.trimToNull("  abc  "));
        assertNull(Strings.trimToNull("   "));
        assertNull(Strings.trimToNull((String) null));
    }

    @Test
    @DisplayName("Test trimToNull() with array")
    public void testTrimToNull_Array() {
        String[] array = { "  a  ", "   ", "c" };
        Strings.trimToNull(array);
        assertArrayEquals(new String[] { "a", null, "c" }, array);
    }

    @Test
    @DisplayName("Test trimToEmpty()")
    public void testTrimToEmpty() {
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
        assertEquals("", Strings.trimToEmpty("   "));
        assertEquals("", Strings.trimToEmpty((String) null));
    }

    @Test
    @DisplayName("Test trimToEmpty() with array")
    public void testTrimToEmpty_Array() {
        String[] array = { "  a  ", "   ", null };
        Strings.trimToEmpty(array);
        assertArrayEquals(new String[] { "a", "", "" }, array);
    }

    @Test
    @DisplayName("Test strip()")
    public void testStrip() {
        assertEquals("abc", Strings.strip("  abc  "));
        assertEquals("abc", Strings.strip("abc"));
        assertEquals("", Strings.strip("   "));
        assertNull(Strings.strip((String) null));
    }

    @Test
    @DisplayName("Test strip() with array")
    public void testStrip_Array() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.strip(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    @DisplayName("Test stripToNull()")
    public void testStripToNull() {
        assertEquals("abc", Strings.stripToNull("  abc  "));
        assertNull(Strings.stripToNull("   "));
        assertNull(Strings.stripToNull((String) null));
    }

    @Test
    @DisplayName("Test stripToEmpty()")
    public void testStripToEmpty() {
        assertEquals("abc", Strings.stripToEmpty("  abc  "));
        assertEquals("", Strings.stripToEmpty("   "));
        assertEquals("", Strings.stripToEmpty((String) null));
    }

    @Test
    @DisplayName("Test strip() with custom chars")
    public void testStrip_WithChars() {
        assertEquals("abc", Strings.strip("xxabcxx", "x"));
        assertEquals("abc", Strings.strip("--abc--", "-"));
        assertNull(Strings.strip((String) null, "x"));
    }

    @Test
    @DisplayName("Test stripStart()")
    public void testStripStart() {
        assertEquals("abc  ", Strings.stripStart("  abc  "));
        assertEquals("abc", Strings.stripStart("abc"));
        assertNull(Strings.stripStart((String) null));
    }

    @Test
    @DisplayName("Test stripStart() with custom chars")
    public void testStripStart_WithChars() {
        assertEquals("abcxx", Strings.stripStart("xxabcxx", "x"));
        assertNull(Strings.stripStart((String) null, "x"));
    }

    @Test
    @DisplayName("Test stripEnd()")
    public void testStripEnd() {
        assertEquals("  abc", Strings.stripEnd("  abc  "));
        assertEquals("abc", Strings.stripEnd("abc"));
        assertNull(Strings.stripEnd((String) null));
    }

    @Test
    @DisplayName("Test stripEnd() with custom chars")
    public void testStripEnd_WithChars() {
        assertEquals("xxabc", Strings.stripEnd("xxabcxx", "x"));
        assertNull(Strings.stripEnd((String) null, "x"));
    }

    @Test
    @DisplayName("Test stripAccents()")
    public void testStripAccents() {
        assertEquals("aeiou", Strings.stripAccents("\u00e0\u00e9\u00ed\u00f3\u00fa"));
        assertEquals("abc", Strings.stripAccents("abc"));
        assertNull(Strings.stripAccents((String) null));
    }

    @Test
    @DisplayName("Test stripAccents() with array")
    public void testStripAccents_Array() {
        String[] array = { "\u00e0bc", "xyz" };
        Strings.stripAccents(array);
        assertArrayEquals(new String[] { "abc", "xyz" }, array);
    }

    @Test
    @DisplayName("Test chomp()")
    public void testChomp() {
        assertEquals("abc", Strings.chomp("abc\n"));
        assertEquals("abc", Strings.chomp("abc\r\n"));
        assertEquals("abc", Strings.chomp("abc\r"));
        assertEquals("abc", Strings.chomp("abc"));
        assertNull(Strings.chomp((String) null));
    }

    @Test
    @DisplayName("Test chomp() with array")
    public void testChomp_Array() {
        String[] array = { "a\n", "b\r\n", "c" };
        Strings.chomp(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    @DisplayName("Test chop()")
    public void testChop() {
        assertEquals("ab", Strings.chop("abc"));
        assertEquals("abc", Strings.chop("abc\r\n"));
        assertEquals("", Strings.chop("a"));
        assertEquals("", Strings.chop(""));
        assertNull(Strings.chop((String) null));
    }

    @Test
    @DisplayName("Test chop() with array")
    public void testChop_Array() {
        String[] array = { "abc", "xy", "z" };
        Strings.chop(array);
        assertArrayEquals(new String[] { "ab", "x", "" }, array);
    }

    @Test
    @DisplayName("Test truncate()")
    public void testTruncate() {
        assertEquals("abc", Strings.truncate("abcdef", 3));
        assertEquals("abc", Strings.truncate("abc", 5));
        assertNull(Strings.truncate((String) null, 3));
    }

    @Test
    @DisplayName("Test truncate() with offset")
    public void testTruncate_WithOffset() {
        assertEquals("cde", Strings.truncate("abcdef", 2, 3));
        assertEquals("abc", Strings.truncate("abc", 0, 5));
    }

    @Test
    @DisplayName("Test truncate() with array")
    public void testTruncate_Array() {
        String[] array = { "abcdef", "xyz" };
        Strings.truncate(array, 3);
        assertArrayEquals(new String[] { "abc", "xyz" }, array);
    }

    @Test
    @DisplayName("Test truncate() with array and offset")
    public void testTruncate_ArrayWithOffset() {
        String[] array = { "abcdef", "xyz" };
        Strings.truncate(array, 1, 3);
        assertArrayEquals(new String[] { "bcd", "yz" }, array);
    }

    @Test
    @DisplayName("Test deleteWhitespace()")
    public void testDeleteWhitespace() {
        assertEquals("abc", Strings.removeWhitespace("a b c"));
        assertEquals("abc", Strings.removeWhitespace("  a  b  c  "));
        assertEquals("abc", Strings.removeWhitespace("abc"));
        assertNull(Strings.removeWhitespace((String) null));
    }

    @Test
    @DisplayName("Test deleteWhitespace() with array")
    public void testDeleteWhitespace_Array() {
        String[] array = { "a b", " x y z " };
        Strings.removeWhitespace(array);
        assertArrayEquals(new String[] { "ab", "xyz" }, array);
    }

    @Test
    @DisplayName("Test appendIfMissing()")
    public void testAppendIfMissing() {
        assertEquals("abc.txt", Strings.appendIfMissing("abc", ".txt"));
        assertEquals("abc.txt", Strings.appendIfMissing("abc.txt", ".txt"));
        assertEquals(".txt", Strings.appendIfMissing(null, ".txt"));
    }

    @Test
    @DisplayName("Test appendIfMissing() throws on empty suffix")
    public void testAppendIfMissing_EmptySuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissing("abc", ""));
    }

    @Test
    @DisplayName("Test appendIfMissingIgnoreCase()")
    public void testAppendIfMissingIgnoreCase() {
        assertEquals("abc.txt", Strings.appendIfMissingIgnoreCase("abc", ".txt"));
        assertEquals("abc.TXT", Strings.appendIfMissingIgnoreCase("abc.TXT", ".txt"));
        assertEquals(".txt", Strings.appendIfMissingIgnoreCase(null, ".txt"));
    }

    @Test
    @DisplayName("Test prependIfMissing()")
    public void testPrependIfMissing() {
        assertEquals("http://abc", Strings.prependIfMissing("abc", "http://"));
        assertEquals("http://abc", Strings.prependIfMissing("http://abc", "http://"));
        assertEquals("http://", Strings.prependIfMissing(null, "http://"));
    }

    @Test
    @DisplayName("Test prependIfMissingIgnoreCase()")
    public void testPrependIfMissingIgnoreCase() {
        assertEquals("http://abc", Strings.prependIfMissingIgnoreCase("abc", "http://"));
        assertEquals("HTTP://abc", Strings.prependIfMissingIgnoreCase("HTTP://abc", "http://"));
        assertEquals("http://", Strings.prependIfMissingIgnoreCase(null, "http://"));
    }

    @Test
    @DisplayName("Test wrapIfMissing() with same prefix/suffix")
    public void testWrapIfMissing() {
        assertEquals("'abc'", Strings.wrapIfMissing("abc", "'"));
        assertEquals("'abc'", Strings.wrapIfMissing("'abc'", "'"));
        assertEquals("''", Strings.wrapIfMissing(null, "'"));
    }

    @Test
    @DisplayName("Test wrapIfMissing() with different prefix and suffix")
    public void testWrapIfMissing_DifferentPrefixSuffix() {
        assertEquals("<abc>", Strings.wrapIfMissing("abc", "<", ">"));
        assertEquals("<abc>", Strings.wrapIfMissing("<abc>", "<", ">"));
        assertEquals("<>", Strings.wrapIfMissing(null, "<", ">"));
    }

    @Test
    @DisplayName("Test wrap()")
    public void testWrap() {
        assertEquals("'abc'", Strings.wrap("abc", "'"));
        assertEquals("''abc''", Strings.wrap("'abc'", "'"));
        assertEquals("''", Strings.wrap(null, "'"));
    }

    @Test
    @DisplayName("Test wrap() with different prefix and suffix")
    public void testWrap_DifferentPrefixSuffix() {
        assertEquals("<abc>", Strings.wrap("abc", "<", ">"));
        assertEquals("<<abc>>", Strings.wrap("<abc>", "<", ">"));
        assertEquals("<>", Strings.wrap(null, "<", ">"));
    }

    @Test
    @DisplayName("Test unwrap()")
    public void testUnwrap() {
        assertEquals("abc", Strings.unwrap("'abc'", "'"));
        assertEquals("abc", Strings.unwrap("abc", "'"));
        assertNull(Strings.unwrap(null, "'"));
    }

    @Test
    @DisplayName("Test unwrap() with different prefix and suffix")
    public void testUnwrap_DifferentPrefixSuffix() {
        assertEquals("abc", Strings.unwrap("<abc>", "<", ">"));
        assertEquals("abc", Strings.unwrap("abc", "<", ">"));
        assertNull(Strings.unwrap(null, "<", ">"));
    }

    @Test
    @DisplayName("Test isLowerCase()")
    public void testIsLowerCase() {
        assertTrue(Strings.isLowerCase('a'));
        assertTrue(Strings.isLowerCase('z'));
        assertFalse(Strings.isLowerCase('A'));
        assertFalse(Strings.isLowerCase('1'));
    }

    @Test
    @DisplayName("Test isAsciiLowerCase()")
    public void testIsAsciiLowerCase() {
        assertTrue(Strings.isAsciiLowerCase('a'));
        assertTrue(Strings.isAsciiLowerCase('z'));
        assertFalse(Strings.isAsciiLowerCase('A'));
        assertFalse(Strings.isAsciiLowerCase('1'));
    }

    @Test
    @DisplayName("Test isUpperCase()")
    public void testIsUpperCase() {
        assertTrue(Strings.isUpperCase('A'));
        assertTrue(Strings.isUpperCase('Z'));
        assertFalse(Strings.isUpperCase('a'));
        assertFalse(Strings.isUpperCase('1'));
    }

    @Test
    @DisplayName("Test isAsciiUpperCase()")
    public void testIsAsciiUpperCase() {
        assertTrue(Strings.isAsciiUpperCase('A'));
        assertTrue(Strings.isAsciiUpperCase('Z'));
        assertFalse(Strings.isAsciiUpperCase('a'));
        assertFalse(Strings.isAsciiUpperCase('1'));
    }

    @Test
    @DisplayName("Test isAllLowerCase()")
    public void testIsAllLowerCase() {
        assertTrue(Strings.isAllLowerCase("abc"));
        assertTrue(Strings.isAllLowerCase("a"));
        assertFalse(Strings.isAllLowerCase("Abc"));
        assertFalse(Strings.isAllLowerCase("ABC"));
        assertTrue(Strings.isAllLowerCase(null));
        assertTrue(Strings.isAllLowerCase(""));
    }

    @Test
    @DisplayName("Test isAllUpperCase()")
    public void testIsAllUpperCase() {
        assertTrue(Strings.isAllUpperCase("ABC"));
        assertTrue(Strings.isAllUpperCase("A"));
        assertFalse(Strings.isAllUpperCase("Abc"));
        assertFalse(Strings.isAllUpperCase("abc"));
        assertTrue(Strings.isAllUpperCase(null));
        assertTrue(Strings.isAllUpperCase(""));
    }

    @Test
    @DisplayName("Test isMixedCase()")
    public void testIsMixedCase() {
        assertTrue(Strings.isMixedCase("Abc"));
        assertTrue(Strings.isMixedCase("aBc"));
        assertFalse(Strings.isMixedCase("abc"));
        assertFalse(Strings.isMixedCase("ABC"));
        assertFalse(Strings.isMixedCase(null));
        assertFalse(Strings.isMixedCase(""));
    }

    @Test
    @DisplayName("Test isDigit()")
    public void testIsDigit() {
        assertTrue(Strings.isDigit('0'));
        assertTrue(Strings.isDigit('9'));
        assertFalse(Strings.isDigit('a'));
        assertFalse(Strings.isDigit('A'));
    }

    @Test
    @DisplayName("Test isLetter()")
    public void testIsLetter() {
        assertTrue(Strings.isLetter('a'));
        assertTrue(Strings.isLetter('A'));
        assertTrue(Strings.isLetter('z'));
        assertFalse(Strings.isLetter('1'));
        assertFalse(Strings.isLetter('!'));
    }

    @Test
    @DisplayName("Test isLetterOrDigit()")
    public void testIsLetterOrDigit() {
        assertTrue(Strings.isLetterOrDigit('a'));
        assertTrue(Strings.isLetterOrDigit('A'));
        assertTrue(Strings.isLetterOrDigit('1'));
        assertFalse(Strings.isLetterOrDigit('!'));
        assertFalse(Strings.isLetterOrDigit(' '));
    }

    @Test
    @DisplayName("Test isAscii()")
    public void testIsAscii() {
        assertTrue(Strings.isAscii('a'));
        assertTrue(Strings.isAscii('A'));
        assertTrue(Strings.isAscii('1'));
        assertTrue(Strings.isAscii(' '));
        assertFalse(Strings.isAscii('\u00e9'));
    }

    @Test
    @DisplayName("Test isAsciiPrintable() with char")
    public void testIsAsciiPrintable_Char() {
        assertTrue(Strings.isAsciiPrintable('a'));
        assertTrue(Strings.isAsciiPrintable('1'));
        assertTrue(Strings.isAsciiPrintable(' '));
        assertFalse(Strings.isAsciiPrintable('\t'));
        assertFalse(Strings.isAsciiPrintable('\n'));
    }

    @Test
    @DisplayName("Test isAsciiControl()")
    public void testIsAsciiControl() {
        assertTrue(Strings.isAsciiControl('\t'));
        assertTrue(Strings.isAsciiControl('\n'));
        assertFalse(Strings.isAsciiControl('a'));
        assertFalse(Strings.isAsciiControl(' '));
    }

    @Test
    @DisplayName("Test isAsciiAlpha() with char")
    public void testIsAsciiAlpha_Char() {
        assertTrue(Strings.isAsciiAlpha('a'));
        assertTrue(Strings.isAsciiAlpha('Z'));
        assertFalse(Strings.isAsciiAlpha('1'));
        assertFalse(Strings.isAsciiAlpha(' '));
    }

    @Test
    @DisplayName("Test isAsciiAlphaUpper()")
    public void testIsAsciiAlphaUpper() {
        assertTrue(Strings.isAsciiAlphaUpper('A'));
        assertTrue(Strings.isAsciiAlphaUpper('Z'));
        assertFalse(Strings.isAsciiAlphaUpper('a'));
        assertFalse(Strings.isAsciiAlphaUpper('1'));
    }

    @Test
    @DisplayName("Test isAsciiAlphaLower()")
    public void testIsAsciiAlphaLower() {
        assertTrue(Strings.isAsciiAlphaLower('a'));
        assertTrue(Strings.isAsciiAlphaLower('z'));
        assertFalse(Strings.isAsciiAlphaLower('A'));
        assertFalse(Strings.isAsciiAlphaLower('1'));
    }

    @Test
    @DisplayName("Test isAsciiNumeric() with char")
    public void testIsAsciiNumeric_Char() {
        assertTrue(Strings.isAsciiNumeric('0'));
        assertTrue(Strings.isAsciiNumeric('9'));
        assertFalse(Strings.isAsciiNumeric('a'));
        assertFalse(Strings.isAsciiNumeric(' '));
    }

    @Test
    @DisplayName("Test isAsciiAlphanumeric() with char")
    public void testIsAsciiAlphanumeric_Char() {
        assertTrue(Strings.isAsciiAlphanumeric('a'));
        assertTrue(Strings.isAsciiAlphanumeric('A'));
        assertTrue(Strings.isAsciiAlphanumeric('1'));
        assertFalse(Strings.isAsciiAlphanumeric(' '));
        assertFalse(Strings.isAsciiAlphanumeric('!'));
    }

    @Test
    @DisplayName("Test isAsciiPrintable() with string")
    public void testIsAsciiPrintable_String() {
        assertFalse(Strings.isAsciiPrintable(null));
        assertFalse(Strings.isAsciiPrintable("abc\t123"));
        assertTrue(Strings.isAsciiPrintable("abc123"));
        assertTrue(Strings.isAsciiPrintable("abc 123"));
        assertTrue(Strings.isAsciiPrintable(""));
    }

    @Test
    @DisplayName("Test isAsciiAlpha() with string")
    public void testIsAsciiAlpha_String() {
        assertTrue(Strings.isAsciiAlpha("abc"));
        assertTrue(Strings.isAsciiAlpha("ABC"));
        assertFalse(Strings.isAsciiAlpha("abc123"));
        assertFalse(Strings.isAsciiAlpha(null));
        assertFalse(Strings.isAsciiAlpha(""));
    }

    @Test
    @DisplayName("Test isAsciiAlphaSpace()")
    public void testIsAsciiAlphaSpace() {
        assertTrue(Strings.isAsciiAlphaSpace(""));
        assertTrue(Strings.isAsciiAlphaSpace("abc"));
        assertTrue(Strings.isAsciiAlphaSpace("abc def"));
        assertFalse(Strings.isAsciiAlphaSpace("abc123"));
        assertFalse(Strings.isAsciiAlphaSpace(null));
    }

    @Test
    @DisplayName("Test isAsciiAlphanumeric() with string")
    public void testIsAsciiAlphanumeric_String() {
        assertTrue(Strings.isAsciiAlphanumeric("abc123"));
        assertTrue(Strings.isAsciiAlphanumeric("ABC"));
        assertFalse(Strings.isAsciiAlphanumeric("abc 123"));
        assertFalse(Strings.isAsciiAlphanumeric(null));
        assertFalse(Strings.isAsciiAlphanumeric(""));
    }

    @Test
    @DisplayName("Test isAsciiAlphanumericSpace()")
    public void testIsAsciiAlphanumericSpace() {
        assertTrue(Strings.isAsciiAlphanumericSpace("abc123"));
        assertTrue(Strings.isAsciiAlphanumericSpace("abc 123"));
        assertFalse(Strings.isAsciiAlphanumericSpace("abc!123"));
        assertFalse(Strings.isAsciiAlphanumericSpace(null));
        assertTrue(Strings.isAsciiAlphanumericSpace(""));
    }

    @Test
    @DisplayName("Test isAsciiNumeric() with string")
    public void testIsAsciiNumeric_String() {
        assertTrue(Strings.isAsciiNumeric("123"));
        assertTrue(Strings.isAsciiNumeric("0"));
        assertFalse(Strings.isAsciiNumeric("abc"));
        assertFalse(Strings.isAsciiNumeric("12.3"));
        assertFalse(Strings.isAsciiNumeric(null));
        assertFalse(Strings.isAsciiNumeric(""));
    }

    @Test
    @DisplayName("Test isAlpha()")
    public void testIsAlpha() {
        assertTrue(Strings.isAlpha("abc"));
        assertTrue(Strings.isAlpha("ABC"));
        assertFalse(Strings.isAlpha("abc123"));
        assertFalse(Strings.isAlpha("abc "));
        assertFalse(Strings.isAlpha(null));
        assertFalse(Strings.isAlpha(""));
    }

    @Test
    @DisplayName("Test isAlphaSpace()")
    public void testIsAlphaSpace() {
        assertTrue(Strings.isAlphaSpace("abc"));
        assertTrue(Strings.isAlphaSpace("abc def"));
        assertFalse(Strings.isAlphaSpace("abc123"));
        assertFalse(Strings.isAlphaSpace(null));
        assertTrue(Strings.isAlphaSpace(""));
    }

    @Test
    @DisplayName("Test isAlphanumeric()")
    public void testIsAlphanumeric() {
        assertTrue(Strings.isAlphanumeric("abc123"));
        assertTrue(Strings.isAlphanumeric("ABC"));
        assertFalse(Strings.isAlphanumeric("abc 123"));
        assertFalse(Strings.isAlphanumeric(null));
        assertFalse(Strings.isAlphanumeric(""));
    }

    @Test
    @DisplayName("Test isAlphanumericSpace()")
    public void testIsAlphanumericSpace() {
        assertTrue(Strings.isAlphanumericSpace(""));
        assertTrue(Strings.isAlphanumericSpace("abc123"));
        assertTrue(Strings.isAlphanumericSpace("abc 123"));
        assertTrue(Strings.isAlphanumericSpace("你好 123"));
        assertTrue(Strings.isAlphanumericSpace("café au lait 2023"));
        assertFalse(Strings.isAlphanumericSpace("abc!123"));
        assertFalse(Strings.isAlphanumericSpace(null));
    }

    @Test
    @DisplayName("Test isNumeric()")
    public void testIsNumeric() {
        assertTrue(Strings.isNumeric("123"));
        assertTrue(Strings.isNumeric("0"));
        assertFalse(Strings.isNumeric("12.3"));
        assertFalse(Strings.isNumeric("abc"));
        assertFalse(Strings.isNumeric(null));
        assertFalse(Strings.isNumeric(""));
    }

    @Test
    @DisplayName("Test isNumericSpace()")
    public void testIsNumericSpace() {
        assertTrue(Strings.isNumericSpace(""));
        assertTrue(Strings.isNumericSpace("123"));
        assertTrue(Strings.isNumericSpace("1 2 3"));
        assertFalse(Strings.isNumericSpace("12.3"));
        assertFalse(Strings.isNumericSpace(null));
    }

    @Test
    @DisplayName("Test isWhitespace()")
    public void testIsWhitespace() {
        assertTrue(Strings.isWhitespace(""));
        assertTrue(Strings.isWhitespace("   "));
        assertTrue(Strings.isWhitespace("\t\n\r"));
        assertTrue(Strings.isWhitespace(" "));
        assertFalse(Strings.isWhitespace("abc"));
        assertFalse(Strings.isWhitespace(" a "));
        assertFalse(Strings.isWhitespace(null));
    }

    @Test
    @DisplayName("Test isNumber()")
    public void testIsNumber() {
        assertTrue(Strings.isNumber("123"));
        assertTrue(Strings.isNumber("12.3"));
        assertTrue(Strings.isNumber("-123"));
        assertTrue(Strings.isNumber("1.23e4"));
        assertFalse(Strings.isNumber("abc"));
        assertFalse(Strings.isNumber(null));
        assertFalse(Strings.isNumber(""));
    }

    @Test
    @DisplayName("Test isAsciiNumber()")
    public void testisAsciiNumber() {
        assertTrue(Strings.isAsciiNumber("123"));
        assertTrue(Strings.isAsciiNumber("-123"));
        assertTrue(Strings.isAsciiNumber("12.3"));
        assertFalse(Strings.isAsciiNumber("abc"));
        assertFalse(Strings.isAsciiNumber(null));
        assertFalse(Strings.isAsciiNumber(""));
    }

    @Test
    @DisplayName("Test convertWords() with function")
    public void testConvertWords() {
        assertEquals("HELLO WORLD", Strings.convertWords("hello world", String::toUpperCase));
        assertEquals("abc def", Strings.convertWords("ABC DEF", String::toLowerCase));
        assertNull(Strings.convertWords(null, String::toUpperCase));
    }

    @Test
    @DisplayName("Test convertWords() with delimiter")
    public void testConvertWords_WithDelimiter() {
        assertEquals("HELLO-WORLD", Strings.convertWords("hello-world", "-", String::toUpperCase));
        assertNull(Strings.convertWords(null, "-", String::toUpperCase));
    }

    @Test
    @DisplayName("Test convertWords() with excluded words")
    public void testConvertWords_WithExcludedWords() {
        List<String> excluded = Arrays.asList("the", "and");
        assertEquals("HELLO the WORLD", Strings.convertWords("hello the world", " ", excluded, String::toUpperCase));
    }

    @Test
    @DisplayName("Test base64Encode() with byte array")
    public void testBase64Encode() {
        byte[] data = "Hello World".getBytes();
        String encoded = Strings.base64Encode(data);
        assertNotNull(encoded);
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64Encode((byte[]) null));
    }

    @Test
    @DisplayName("Test base64EncodeString()")
    public void testBase64EncodeString() {
        String encoded = Strings.base64EncodeString("Hello World");
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64EncodeString(null));
    }

    @Test
    @DisplayName("Test base64EncodeUtf8String()")
    public void testBase64EncodeUtf8String() {
        String encoded = Strings.base64EncodeUtf8String("Hello World");
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64EncodeUtf8String(null));
    }

    @Test
    @DisplayName("Test base64Decode()")
    public void testBase64Decode() {
        byte[] decoded = Strings.base64Decode("SGVsbG8gV29ybGQ=");
        assertArrayEquals("Hello World".getBytes(), decoded);
        assertArrayEquals("".getBytes(), Strings.base64Decode((String) null));
    }

    @Test
    @DisplayName("Test base64DecodeToString()")
    public void testBase64DecodeToString() {
        String decoded = Strings.base64DecodeToString("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64DecodeToString(null));
    }

    @Test
    @DisplayName("Test base64DecodeToUtf8String()")
    public void testBase64DecodeToUtf8String() {
        String decoded = Strings.base64DecodeToUtf8String("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64DecodeToUtf8String(null));
    }

    @Test
    @DisplayName("Test base64UrlEncode()")
    public void testBase64UrlEncode() {
        byte[] data = "Hello+World/Test".getBytes();
        String encoded = Strings.base64UrlEncode(data);
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertEquals("", Strings.base64UrlEncode((byte[]) null));
    }

    @Test
    @DisplayName("Test base64UrlDecode()")
    public void testBase64UrlDecode() {
        String encoded = Strings.base64UrlEncode("Hello+World/Test".getBytes());
        byte[] decoded = Strings.base64UrlDecode(encoded);
        assertArrayEquals("Hello+World/Test".getBytes(), decoded);
        assertArrayEquals("".getBytes(), Strings.base64UrlDecode(""));
        assertArrayEquals("".getBytes(), Strings.base64UrlDecode((String) null));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToString()")
    public void testBase64UrlDecodeToString() {
        String encoded = Strings.base64UrlEncode("Hello World".getBytes());
        String decoded = Strings.base64UrlDecodeToString(encoded);
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64UrlDecodeToString(null));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToUtf8String()")
    public void testBase64UrlDecodeToUtf8String() {
        String encoded = Strings.base64UrlEncode("Hello World".getBytes());
        String decoded = Strings.base64UrlDecodeToUtf8String(encoded);
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64UrlDecodeToUtf8String(null));
    }

    @Test
    @DisplayName("Test isBase64()")
    public void testIsBase64() {
        assertTrue(Strings.isBase64("SGVsbG8gV29ybGQ="));
        assertTrue(Strings.isBase64("YWJjZGVm"));
        assertFalse(Strings.isBase64("Hello World!"));
        assertFalse(Strings.isBase64("ABC!@#"));
        assertFalse(Strings.isBase64((String) null));
        assertFalse(Strings.isBase64((byte[]) null));
        assertTrue(Strings.isBase64(""));
    }

    @Test
    @DisplayName("Test urlEncode()")
    public void testUrlEncode() {
        String encoded = Strings.urlEncode("Hello World");
        assertEquals("Hello+World", encoded);
        String encoded2 = Strings.urlEncode("a=b&c=d");
        assertEquals("a=b&c=d", encoded2);
        assertEquals("", Strings.urlEncode(null));
    }

    @Test
    @DisplayName("Test urlDecode()")
    public void testUrlDecode() {
        Map<String, String> decoded = Strings.urlDecode(Strings.urlEncode("a=b&c=d"));
        assertEquals(Map.of("a", "b", "c", "d"), decoded);
        assertEquals(Map.of(), Strings.urlDecode(null));
    }

    @Test
    @DisplayName("Test indexOf() with char")
    public void testIndexOf_Char() {
        assertEquals(0, Strings.indexOf("abc", 'a'));
        assertEquals(2, Strings.indexOf("abc", 'c'));
        assertEquals(-1, Strings.indexOf("abc", 'x'));
        assertEquals(-1, Strings.indexOf(null, 'a'));
    }

    @Test
    @DisplayName("Test indexOf() with char and fromIndex")
    public void testIndexOf_CharWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", 'a', 1));
        assertEquals(-1, Strings.indexOf("abc", 'a', 5));
        assertEquals(-1, Strings.indexOf(null, 'a', 0));
    }

    @Test
    @DisplayName("Test indexOf() with string")
    public void testIndexOf_String() {
        assertEquals(0, Strings.indexOf("abcdef", "abc"));
        assertEquals(3, Strings.indexOf("abcdef", "def"));
        assertEquals(-1, Strings.indexOf("abcdef", "xyz"));
        assertEquals(-1, Strings.indexOf(null, "abc"));
    }

    @Test
    @DisplayName("Test indexOf() with string and fromIndex")
    public void testIndexOf_StringWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", "abc", 1));
        assertEquals(-1, Strings.indexOf("abc", "abc", 5));
        assertEquals(-1, Strings.indexOf(null, "abc", 0));
    }

    @Test
    @DisplayName("Test indexOfAny() with char array")
    public void testIndexOfAny_CharArray() {
        assertEquals(0, Strings.indexOfAny("abc", 'a', 'x'));
        assertEquals(1, Strings.indexOfAny("abc", 'b', 'c'));
        assertEquals(-1, Strings.indexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test indexOfAny() with char array and fromIndex")
    public void testIndexOfAny_CharArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, 'a'));
        assertEquals(-1, Strings.indexOfAny("abc", 5, 'a'));
    }

    @Test
    @DisplayName("Test indexOfAny() with string array")
    public void testIndexOfAny_StringArray() {
        assertEquals(0, Strings.indexOfAny("abcdef", "abc", "xyz"));
        assertEquals(3, Strings.indexOfAny("abcdef", "def", "xyz"));
        assertEquals(-1, Strings.indexOfAny("abcdef", "xyz", "123"));
        assertEquals(-1, Strings.indexOfAny(null, "abc", "def"));
    }

    @Test
    @DisplayName("Test indexOfAny() with string array and fromIndex")
    public void testIndexOfAny_StringArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, "abc"));
        assertEquals(-1, Strings.indexOfAny("abc", 5, "abc"));
    }

    @Test
    @DisplayName("Test indexOfAnyBut() with char array")
    public void testIndexOfAnyBut() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 'a'));
        assertEquals(0, Strings.indexOfAnyBut("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 'a'));
        assertEquals(-1, Strings.indexOfAnyBut(null, 'a'));
    }

    @Test
    @DisplayName("Test indexOfAnyBut() with fromIndex")
    public void testIndexOfAnyBut_WithFromIndex() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 3, 'a'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 0, 'a'));
        assertEquals(2, Strings.indexOfAnyBut("abc", 2));
        assertEquals(-1, Strings.indexOfAnyBut("abc", 3));
    }

    @Test
    @DisplayName("Test indexOfIgnoreCase()")
    public void testIndexOfIgnoreCase() {
        assertEquals(0, Strings.indexOfIgnoreCase("AbCdEf", "abc"));
        assertEquals(3, Strings.indexOfIgnoreCase("abcDEF", "def"));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test indexOfIgnoreCase() with fromIndex")
    public void testIndexOfIgnoreCase_WithFromIndex() {
        assertEquals(3, Strings.indexOfIgnoreCase("AbCaBc", "abc", 1));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "abc", 5));
    }

    @Test
    @DisplayName("Test indexOfDifference()")
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
    @DisplayName("Test indexOfDifference() with array")
    public void testIndexOfDifference_Array() {
        assertEquals(3, Strings.indexOfDifference("abcxyz", "abcdef", "abc123"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc", "abc"));
    }

    @Test
    @DisplayName("Test indicesOf()")
    public void testIndicesOf() {
        int[] indices = Strings.indicesOf("abcabc", "abc").toArray();
        assertArrayEquals(new int[] { 0, 3 }, indices);
        int[] indices2 = Strings.indicesOf("abc", "xyz").toArray();
        assertArrayEquals(new int[0], indices2);
    }

    @Test
    @DisplayName("Test indicesOfIgnoreCase()")
    public void testIndicesOfIgnoreCase() {
        {
            int[] indices = Strings.indicesOfIgnoreCase("AbCaBc", "abc").toArray();
            assertArrayEquals(new int[] { 0, 3 }, indices);
            int[] indices2 = Strings.indicesOfIgnoreCase("abc", "xyz").toArray();
            assertArrayEquals(new int[0], indices2);

            int[] indices3 = Strings.indicesOfIgnoreCase("abc", "").toArray();
            assertArrayEquals(new int[] { 0, 1, 2 }, indices3);

            int[] indices4 = Strings.indicesOfIgnoreCase("abc", null).toArray();
            assertEquals(0, indices4.length);
        }

        {
            int[] indices = Strings.indicesOfIgnoreCase("AbCaBc", "abc", 1).toArray();
            assertArrayEquals(new int[] { 3 }, indices);
            int[] indices2 = Strings.indicesOfIgnoreCase("abc", "xyz", 1).toArray();
            assertArrayEquals(new int[0], indices2);

            int[] indices3 = Strings.indicesOfIgnoreCase("abc", "", 1).toArray();
            assertArrayEquals(new int[] { 1, 2 }, indices3);

            int[] indices4 = Strings.indicesOfIgnoreCase("abc", null, 1).toArray();
            assertEquals(0, indices4.length);
        }
    }

    @Test
    @DisplayName("Test lastIndexOf() with char")
    public void testLastIndexOf_Char() {
        assertEquals(3, Strings.lastIndexOf("abcabc", 'a'));
        assertEquals(5, Strings.lastIndexOf("abcabc", 'c'));
        assertEquals(-1, Strings.lastIndexOf("abc", 'x'));
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
    }

    @Test
    @DisplayName("Test lastIndexOf() with char and startIndexFromBack")
    public void testLastIndexOf_CharWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", 'a', 2));
        assertEquals(-1, Strings.lastIndexOf("abc", 'c', 1));
    }

    @Test
    @DisplayName("Test lastIndexOf() with string")
    public void testLastIndexOf_String() {
        assertEquals(3, Strings.lastIndexOf("abcabc", "abc"));
        assertEquals(0, Strings.lastIndexOf("abc", "abc"));
        assertEquals(-1, Strings.lastIndexOf("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOf(null, "abc"));
    }

    @Test
    @DisplayName("Test lastIndexOf() with string and startIndexFromBack")
    public void testLastIndexOf_StringWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOf("abc", "abc", -1));
    }

    @Test
    @DisplayName("Test lastIndexOfIgnoreCase()")
    public void testLastIndexOfIgnoreCase() {
        assertEquals(3, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc"));
        assertEquals(0, Strings.lastIndexOfIgnoreCase("ABC", "abc"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test lastIndexOfIgnoreCase() with startIndexFromBack")
    public void testLastIndexOfIgnoreCase_WithStartIndex() {
        assertEquals(0, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz", 5));
    }

    @Test
    @DisplayName("Test lastIndexOfAny() with char array")
    public void testLastIndexOfAny_CharArray() {
        assertEquals(0, Strings.lastIndexOfAny("abc", 'a', 'c'));
        assertEquals(5, Strings.lastIndexOfAny("abcabc", new char[] { 'c' }));
        assertEquals(-1, Strings.lastIndexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.lastIndexOfAny((String) null, new char[] { 'a' }));
    }

    @Test
    @DisplayName("Test lastIndexOfAny() with string array")
    public void testLastIndexOfAny_StringArray() {
        assertEquals(3, Strings.lastIndexOfAny("abcabc", "abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfAny("abc", "xyz", "123"));
        assertEquals(-1, Strings.lastIndexOfAny(null, "abc"));
    }

    @Test
    @DisplayName("Test ordinalIndexOf()")
    public void testOrdinalIndexOf() {
        assertEquals(0, Strings.ordinalIndexOf("", "", 1));
        assertEquals(-1, Strings.ordinalIndexOf("", "", 2));
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(5, Strings.ordinalIndexOf("aabaabaa", "b", 2));
        assertEquals(4, Strings.ordinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(0, Strings.ordinalIndexOf("abcabc", "abc", 1));
        assertEquals(3, Strings.ordinalIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.ordinalIndexOf("abcabc", "abc", 3));
        assertEquals(-1, Strings.ordinalIndexOf(null, "abc", 1));
        assertEquals(0, Strings.ordinalIndexOf("abc", "", 1));
        assertEquals(1, Strings.ordinalIndexOf("abc", "", 2));
        assertEquals(-1, Strings.ordinalIndexOf("", null, 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, "", 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, null, 1));
    }

    @Test
    @DisplayName("Test lastOrdinalIndexOf()")
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
    @DisplayName("Test minIndexOfAll()")
    public void testminIndexOfAll() {
        assertEquals(0, Strings.minIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.minIndexOfAll("abcdefg", "def", "efg"));
        assertEquals(-1, Strings.minIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test minIndexOfAll() with fromIndex")
    public void testminIndexOfAll_WithFromIndex() {
        assertEquals(3, Strings.minIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.minIndexOfAll("abc", 5, "abc"));
    }

    @Test
    @DisplayName("Test maxIndexOfAll()")
    public void testmaxIndexOfAll() {
        assertEquals(4, Strings.maxIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.maxIndexOfAll("abcdefg", "def", "abc"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test maxIndexOfAll() with fromIndex")
    public void testmaxIndexOfAll_WithFromIndex() {
        assertEquals(4, Strings.maxIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.maxIndexOfAll("abc", 5, "abc"));
    }

    @Test
    @DisplayName("Test maxLastIndexOfAll()")
    public void testmaxLastIndexOfAll() {
        assertEquals(6, Strings.maxLastIndexOfAll("abcabcabc", "abc"));
        assertEquals(6, Strings.maxLastIndexOfAll("abcdefabc", "abc", "def"));
    }

    @Test
    @DisplayName("Test minLastIndexOfAll()")
    public void testminLastIndexOfAll() {
        assertEquals(3, Strings.minLastIndexOfAll("abcdefabc", "abc", "def"));
        assertEquals(6, Strings.minLastIndexOfAll("abcabcabc", "abc"));
    }

    @Test
    @DisplayName("Test contains() with char")
    public void testContains_Char() {
        assertTrue(Strings.contains("abc", 'a'));
        assertTrue(Strings.contains("abc", 'c'));
        assertFalse(Strings.contains("abc", 'x'));
        assertFalse(Strings.contains(null, 'a'));
    }

    @Test
    @DisplayName("Test contains() with string")
    public void testContains_String() {
        assertTrue(Strings.contains("abcdef", "abc"));
        assertTrue(Strings.contains("abcdef", "def"));
        assertFalse(Strings.contains("abcdef", "xyz"));
        assertFalse(Strings.contains(null, "abc"));
    }

    @Test
    @DisplayName("Test containsIgnoreCase()")
    public void testContainsIgnoreCase() {
        assertTrue(Strings.containsIgnoreCase("AbCdEf", "abc"));
        assertTrue(Strings.containsIgnoreCase("abcdef", "DEF"));
        assertFalse(Strings.containsIgnoreCase("abc", "xyz"));
        assertFalse(Strings.containsIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsAll() with char array")
    public void testContainsAll_CharArray() {
        assertTrue(Strings.containsAll("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsAll("abc", 'a', 'x'));
        assertTrue(Strings.containsAll("abc", (char[]) null));
        assertFalse(Strings.containsAll(null, 'a', 'b'));
        assertThrows(IllegalArgumentException.class, () -> Strings.containsAll("abc", '\uDC00', 'a'));
        assertThrows(IllegalArgumentException.class, () -> Strings.containsAll("abc", '\uDFFF', 'a'));
    }

    @Test
    @DisplayName("Test containsAll() with string array")
    public void testContainsAll_StringArray() {
        assertTrue(Strings.containsAll("abcdefg", "abc", "def"));
        assertFalse(Strings.containsAll("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAll(null, "abc", "def"));
    }

    @Test
    @DisplayName("Test containsAllIgnoreCase()")
    public void testContainsAllIgnoreCase() {
        assertTrue(Strings.containsAllIgnoreCase("AbCdEfG", "abc", "def"));
        assertFalse(Strings.containsAllIgnoreCase("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAllIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsAny() with char array")
    public void testContainsAny_CharArray() {
        assertTrue(Strings.containsAny("abc", 'a', 'x'));
        assertTrue(Strings.containsAny("abc", 'c', 'y'));
        assertFalse(Strings.containsAny("abc", 'x', 'y'));
        assertFalse(Strings.containsAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test containsAny() with string array")
    public void testContainsAny_StringArray() {
        assertTrue(Strings.containsAny("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsAny("abcdef", "xyz", "def"));
        assertFalse(Strings.containsAny("abc", "xyz", "123"));
        assertFalse(Strings.containsAny(null, "abc", "def"));
    }

    @Test
    @DisplayName("Test containsAnyIgnoreCase()")
    public void testContainsAnyIgnoreCase() {
        assertTrue(Strings.containsAnyIgnoreCase("AbCdEf", "abc", "xyz"));
        assertFalse(Strings.containsAnyIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.containsAnyIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsNone() with char array")
    public void testContainsNone_CharArray() {
        assertTrue(Strings.containsNone("abc", 'x', 'y'));
        assertTrue(Strings.containsNone("", 'a'));
        assertTrue(Strings.containsNone(null, 'a'));
        assertFalse(Strings.containsNone("abc", 'a', 'x'));
    }

    @Test
    @DisplayName("Test containsNone() with string array")
    public void testContainsNone_StringArray() {
        assertTrue(Strings.containsNone("abc", "xyz", "123"));
        assertFalse(Strings.containsNone("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsNone(null, "abc"));
    }

    @Test
    @DisplayName("Test containsNoneIgnoreCase()")
    public void testContainsNoneIgnoreCase() {
        assertTrue(Strings.containsNoneIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.containsNoneIgnoreCase("AbCdEf", "abc", "xyz"));
        assertTrue(Strings.containsNoneIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsOnly() with char array")
    public void testContainsOnly() {
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'a', 'b'));
        assertTrue(Strings.containsOnly(null, 'a'));
    }

    @Test
    @DisplayName("Test containsWhitespace()")
    public void testContainsWhitespace() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("a\tb"));
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    @DisplayName("Test countMatches() with char")
    public void testCountMatches_Char() {
        assertEquals(2, Strings.countMatches("abcabc", 'a'));
        assertEquals(2, Strings.countMatches("abcabc", 'c'));
        assertEquals(0, Strings.countMatches("abc", 'x'));
        assertEquals(0, Strings.countMatches(null, 'a'));
    }

    @Test
    @DisplayName("Test countMatches() with string")
    public void testCountMatches_String() {
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(2, Strings.countMatches("aaaa", "aa"));
        assertEquals(0, Strings.countMatches("abc", "xyz"));
        assertEquals(0, Strings.countMatches(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWith()")
    public void testStartsWith() {
        assertTrue(Strings.startsWith("abcdef", "abc"));
        assertFalse(Strings.startsWith("abcdef", "xyz"));
        assertFalse(Strings.startsWith(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWithIgnoreCase()")
    public void testStartsWithIgnoreCase() {
        assertTrue(Strings.startsWithIgnoreCase("AbCdEf", "abc"));
        assertFalse(Strings.startsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.startsWithIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWithAny()")
    public void testStartsWithAny() {
        assertTrue(Strings.startsWithAny("abcdef", "abc", "xyz"));
        assertFalse(Strings.startsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAny(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWithAnyIgnoreCase()")
    public void testStartsWithAnyIgnoreCase() {
        assertTrue(Strings.startsWithAnyIgnoreCase("AbCdEf", "abc", "xyz"));
        assertFalse(Strings.startsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test endsWith()")
    public void testEndsWith() {
        assertTrue(Strings.endsWith("abcdef", "def"));
        assertFalse(Strings.endsWith("abcdef", "xyz"));
        assertFalse(Strings.endsWith(null, "def"));
    }

    @Test
    @DisplayName("Test endsWithIgnoreCase()")
    public void testEndsWithIgnoreCase() {
        assertTrue(Strings.endsWithIgnoreCase("AbCdEf", "def"));
        assertFalse(Strings.endsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.endsWithIgnoreCase(null, "def"));
    }

    @Test
    @DisplayName("Test endsWithAny()")
    public void testEndsWithAny() {
        assertTrue(Strings.endsWithAny("abcdef", "def", "xyz"));
        assertFalse(Strings.endsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAny(null, "def"));
    }

    @Test
    @DisplayName("Test endsWithAnyIgnoreCase()")
    public void testEndsWithAnyIgnoreCase() {
        assertTrue(Strings.endsWithAnyIgnoreCase("AbCdEf", "def", "xyz"));
        assertFalse(Strings.endsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "def"));
    }

    @Test
    @DisplayName("Test equals()")
    public void testEquals() {
        assertTrue(Strings.equals("abc", "abc"));
        assertFalse(Strings.equals("abc", "ABC"));
        assertFalse(Strings.equals("abc", "xyz"));
        assertTrue(Strings.equals(null, null));
        assertFalse(Strings.equals("abc", null));
    }

    @Test
    @DisplayName("Test equalsIgnoreCase()")
    public void testEqualsIgnoreCase() {
        assertTrue(Strings.equalsIgnoreCase("abc", "ABC"));
        assertFalse(Strings.equalsIgnoreCase("abc", "xyz"));
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertFalse(Strings.equalsIgnoreCase("abc", null));
    }

    @Test
    @DisplayName("Test equalsAny()")
    public void testEqualsAny() {
        assertTrue(Strings.equalsAny("abc", "abc", "xyz"));
        assertFalse(Strings.equalsAny("abc", "xyz", "123"));
        assertFalse(Strings.equalsAny(null, "abc", "xyz"));
    }

    @Test
    @DisplayName("Test equalsAnyIgnoreCase()")
    public void testEqualsAnyIgnoreCase() {
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "ABC", "xyz"));
        assertFalse(Strings.equalsAnyIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test substring() with inclusiveBeginIndex")
    public void testSubstring_BeginIndex() {
        assertEquals("def", Strings.substring("abcdef", 3));
        assertEquals("abcdef", Strings.substring("abcdef", 0));
        assertEquals("", Strings.substring("abcdef", 6));
        assertNull(Strings.substring(null, 0));
    }

    @Test
    @DisplayName("Test substring() with begin and end index")
    public void testSubstring_BeginEndIndex() {
        assertEquals("bcd", Strings.substring("abcdef", 1, 4));
        assertEquals("abc", Strings.substring("abcdef", 0, 3));
        assertEquals("", Strings.substring("abcdef", 2, 2));
        assertNull(Strings.substring(null, 0, 3));
    }

    @Test
    @DisplayName("Test substringAfter()")
    public void testSubstringAfter() {
        assertEquals("def", Strings.substringAfter("abc:def", ':'));
        assertNull(Strings.substringAfter("abc", ':'));
        assertNull(Strings.substringAfter(null, ':'));
    }

    @Test
    @DisplayName("Test substringAfter() with string delimiter")
    public void testSubstringAfter_String() {
        assertEquals("def", Strings.substringAfter("abc::def", "::"));
        assertNull(Strings.substringAfter("abc", "::"));
        assertNull(Strings.substringAfter(null, "::"));
    }

    @Test
    @DisplayName("Test substringAfterLast()")
    public void testSubstringAfterLast() {
        assertEquals("c", Strings.substringAfterLast("a:b:c", ':'));
        assertNull(Strings.substringAfterLast("abc", ':'));
        assertNull(Strings.substringAfterLast(null, ':'));
    }

    @Test
    @DisplayName("Test substringAfterLast() with string")
    public void testSubstringAfterLast_String() {
        assertEquals("c", Strings.substringAfterLast("a::b::c", "::"));
        assertNull(Strings.substringAfterLast(null, "::"));
    }

    @Test
    @DisplayName("Test substringAfterLastIgnoreCase()")
    public void testSubstringAfterLastIgnoreCase() {
        assertEquals("C", Strings.substringAfterLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringAfterLastIgnoreCase(null, ":"));
    }

    @Test
    @DisplayName("Test substringAfterAny() with char array")
    public void testSubstringAfterAny_CharArray() {
        assertEquals("def", Strings.substringAfterAny("abcdef", 'c', 'x'));
        assertNull(Strings.substringAfterAny("abc", 'x', 'y'));
        assertNull(Strings.substringAfterAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test substringAfterAny() with string array")
    public void testSubstringAfterAny_StringArray() {
        assertEquals("def", Strings.substringAfterAny("abc::def", "::", "##"));
        assertNull(Strings.substringAfterAny(null, "::", "##"));
    }

    @Test
    @DisplayName("Test substringBefore()")
    public void testSubstringBefore() {
        assertEquals("abc", Strings.substringBefore("abc:def", ':'));
        assertNull(Strings.substringBefore("abc", ':'));
        assertNull(Strings.substringBefore(null, ':'));
    }

    @Test
    @DisplayName("Test substringBefore() with string")
    public void testSubstringBefore_String() {
        assertEquals("abc", Strings.substringBefore("abc::def", "::"));
        assertNull(Strings.substringBefore(null, "::"));
    }

    @Test
    @DisplayName("Test substringBeforeIgnoreCase()")
    public void testSubstringBeforeIgnoreCase() {
        assertEquals("abc", Strings.substringBeforeIgnoreCase("abc:DEF", ":"));
        assertNull(Strings.substringBeforeIgnoreCase(null, ":"));
    }

    @Test
    @DisplayName("Test substringBeforeLast()")
    public void testSubstringBeforeLast() {
        assertEquals("a:b", Strings.substringBeforeLast("a:b:c", ':'));
        assertNull(Strings.substringBeforeLast("abc", ':'));
        assertNull(Strings.substringBeforeLast(null, ':'));
    }

    @Test
    @DisplayName("Test substringBeforeLast() with string")
    public void testSubstringBeforeLast_String() {
        assertEquals("a::b", Strings.substringBeforeLast("a::b::c", "::"));
        assertNull(Strings.substringBeforeLast(null, "::"));
    }

    @Test
    @DisplayName("Test substringBeforeLastIgnoreCase()")
    public void testSubstringBeforeLastIgnoreCase() {
        assertEquals("a:b", Strings.substringBeforeLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringBeforeLastIgnoreCase(null, ":"));
    }

    @Test
    @DisplayName("Test substringBeforeAny() with char array")
    public void testSubstringBeforeAny_CharArray() {
        assertEquals("ab", Strings.substringBeforeAny("abcdef", 'c', 'x'));
        assertNull(Strings.substringBeforeAny("abc", 'x', 'y'));
        assertNull(Strings.substringBeforeAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test substringBeforeAny() with string array")
    public void testSubstringBeforeAny_StringArray() {
        assertEquals("abc", Strings.substringBeforeAny("abc::def", "::", "##"));
        assertNull(Strings.substringBeforeAny(null, "::", "##"));
    }

    @Test
    @DisplayName("Test substringBetween() with delimiter")
    public void testSubstringBetween() {
        assertEquals("b", Strings.substringBetween("a:b:c", ":"));
        assertNull(Strings.substringBetween("abc", ":"));
        assertNull(Strings.substringBetween(null, ":"));
    }

    @Test
    @DisplayName("Test substringBetween() with different delimiters")
    public void testSubstringBetween_DifferentDelimiters() {
        assertEquals("content", Strings.substringBetween("<tag>content</tag>", "<tag>", "</tag>"));
        assertNull(Strings.substringBetween(null, "<", ">"));
    }

    @Test
    @DisplayName("Test substringBetweenIgnoreCase()")
    public void testsubstringBetweenIgnoreCase() {
        assertEquals("content", Strings.substringBetweenIgnoreCase("<TAG>content</tag>", "<tag>", "</TAG>"));
        assertNull(Strings.substringBetweenIgnoreCase(null, "<", ">"));
    }

    @Test
    @DisplayName("Test substringsBetween()")
    public void testSubstringsBetween() {
        List<String> result = Strings.substringsBetween("a:b:c:d", ':', ':');
        assertEquals(Arrays.asList("b"), result);
    }

    @Test
    @DisplayName("Test substringIndicesBetween()")
    public void testSubstringIndicesBetween() {
        List<int[]> result = Strings.substringIndicesBetween("a:b:c", ':', ':');
        assertEquals(1, result.size());
        assertArrayEquals(new int[] { 2, 3 }, result.get(0));
    }

    @Test
    @DisplayName("Test substringOrElse()")
    public void testSubstringOrElse() {
        assertEquals("bcd", StrUtil.substringOrElse("abcdef", 1, 4, "default"));
        assertEquals("default", StrUtil.substringOrElse("abc", 10, 20, "default"));
    }

    @Test
    @DisplayName("Test substringOrElseItself()")
    public void testSubstringOrElseItself() {
        assertEquals("bcd", StrUtil.substringOrElseItself("abcdef", 1, 4));
        assertEquals("abc", StrUtil.substringOrElseItself("abc", 10, 20));
    }

    @Test
    @DisplayName("Test substringAfterOrElse()")
    public void testSubstringAfterOrElse() {
        assertEquals("def", StrUtil.substringAfterOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringAfterOrElseItself()")
    public void testSubstringAfterOrElseItself() {
        assertEquals("def", StrUtil.substringAfterOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringAfterOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test substringAfterLastOrElse()")
    public void testSubstringAfterLastOrElse() {
        assertEquals("c", StrUtil.substringAfterLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringAfterLastOrElseItself()")
    public void testSubstringAfterLastOrElseItself() {
        assertEquals("c", StrUtil.substringAfterLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringAfterLastOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test substringBeforeOrElse()")
    public void testSubstringBeforeOrElse() {
        assertEquals("abc", StrUtil.substringBeforeOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringBeforeOrElseItself()")
    public void testSubstringBeforeOrElseItself() {
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test substringBeforeLastOrElse()")
    public void testSubstringBeforeLastOrElse() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringBeforeLastOrElseItself()")
    public void testSubstringBeforeLastOrElseItself() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringBeforeLastOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test commonPrefix() with two strings")
    public void testCommonPrefix() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertEquals("", Strings.commonPrefix(null, "abc"));
    }

    @Test
    @DisplayName("Test commonPrefix() with multiple strings")
    public void testCommonPrefix_Multiple() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz", "abc123"));
        assertEquals("", Strings.commonPrefix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test commonSuffix() with two strings")
    public void testCommonSuffix() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertEquals("", Strings.commonSuffix(null, "abc"));
    }

    @Test
    @DisplayName("Test commonSuffix() with multiple strings")
    public void testCommonSuffix_Multiple() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz", "123xyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test lengthOfCommonPrefix()")
    public void testLengthOfCommonPrefix() {
        assertEquals(3, Strings.lengthOfCommonPrefix("abcdef", "abcxyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix(null, "abc"));
    }

    @Test
    @DisplayName("Test lengthOfCommonSuffix()")
    public void testLengthOfCommonSuffix() {
        assertEquals(3, Strings.lengthOfCommonSuffix("abcxyz", "defxyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix(null, "abc"));
    }

    @Test
    @DisplayName("Test longestCommonSubstring()")
    public void testLongestCommonSubstring() {
        assertEquals("abc", Strings.longestCommonSubstring("xabcy", "zabc123"));
        assertEquals("", Strings.longestCommonSubstring("abc", "xyz"));
        assertEquals("", Strings.longestCommonSubstring(null, "abc"));
    }

    @Test
    @DisplayName("Test firstChar()")
    public void testFirstChar() {
        assertTrue(Strings.firstChar("abc").isPresent());
        assertEquals('a', Strings.firstChar("abc").get());
        assertFalse(Strings.firstChar("").isPresent());
        assertFalse(Strings.firstChar(null).isPresent());
    }

    @Test
    @DisplayName("Test lastChar()")
    public void testLastChar() {
        assertTrue(Strings.lastChar("abc").isPresent());
        assertEquals('c', Strings.lastChar("abc").get());
        assertFalse(Strings.lastChar("").isPresent());
        assertFalse(Strings.lastChar(null).isPresent());
    }

    @Test
    @DisplayName("Test firstChars()")
    public void testFirstChars() {
        assertEquals("ab", Strings.firstChars("abcdef", 2));
        assertEquals("abcdef", Strings.firstChars("abcdef", 10));
        assertEquals("", Strings.firstChars(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.firstChars("abc", -1));
    }

    @Test
    @DisplayName("Test lastChars()")
    public void testLastChars() {
        assertEquals("ef", Strings.lastChars("abcdef", 2));
        assertEquals("abcdef", Strings.lastChars("abcdef", 10));
        assertEquals("", Strings.lastChars(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.lastChars("abc", -1));
    }

    @Test
    @DisplayName("Test toCodePoints()")
    public void testToCodePoints() {
        int[] codePoints = Strings.toCodePoints("abc");
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, codePoints);
        assertArrayEquals(new int[0], Strings.toCodePoints(""));
        assertNull(Strings.toCodePoints(null));
    }

    @Test
    @DisplayName("Test concat() with multiple strings")
    public void testConcat() {
        assertEquals("abcdef", Strings.concat("abc", "def"));
        assertEquals("abc", Strings.concat("abc", null));
        assertEquals("def", Strings.concat(null, "def"));
        assertEquals("", Strings.concat((String[]) null));
    }

    @Test
    @DisplayName("Test join() with array")
    public void testJoin() {
        assertEquals("a,b,c", Strings.join(new String[] { "a", "b", "c" }, ","));
        assertEquals("abc", Strings.join(new String[] { "a", "b", "c" }, ""));
        assertEquals("", Strings.join((String[]) null, ","));
    }

    @Test
    @DisplayName("Test join() with Iterable")
    public void testJoin_Iterable() {
        assertEquals("a,b,c", Strings.join(Arrays.asList("a", "b", "c"), ","));
        assertEquals("", Strings.join(new ArrayList<>(), ","));
        assertEquals("", Strings.join((Iterable<String>) null, ","));
    }

    //     @Test
    //     @DisplayName("Test joinEntries()")
    //     public void testJoinEntries() {
    //         Map<String, String> map = CommonUtil.asLinkedHashMap("a", "1", "b", "2");
    //         String result = Strings.joinEntries(map, ",", "=");
    //         assertEquals("a=1,b=2", result);
    //     }

    //

    @Test
    @DisplayName("Test parseInt()")
    public void testParseInt() {
        assertEquals(123, Strings.parseInt("123"));
        assertEquals(-456, Strings.parseInt("-456"));
        assertEquals(0, Strings.parseInt(null));
        assertEquals(0, Strings.parseInt(""));
        assertThrows(NumberFormatException.class, () -> Strings.parseInt("abc"));
    }

    @Test
    @DisplayName("Test parseLong()")
    public void testParseLong() {
        assertEquals(123L, Strings.parseLong("123"));
        assertEquals(-456L, Strings.parseLong("-456"));
        assertThrows(NumberFormatException.class, () -> Strings.parseLong("abc"));
    }

    @Test
    @DisplayName("Test parseDouble()")
    public void testParseDouble() {
        assertEquals(123.45, Strings.parseDouble("123.45"), 0.001);
        assertEquals(-456.78, Strings.parseDouble("-456.78"), 0.001);
        assertThrows(NumberFormatException.class, () -> Strings.parseDouble("abc"));
    }

    @Test
    @DisplayName("Test parseFloat()")
    public void testParseFloat() {
        assertEquals(123.45f, Strings.parseFloat("123.45"), 0.001);
        assertEquals(-456.78f, Strings.parseFloat("-456.78"), 0.001);
        assertThrows(NumberFormatException.class, () -> Strings.parseFloat("abc"));
    }

    @Test
    @DisplayName("Test parseBoolean()")
    public void testParseBoolean() {
        assertTrue(Strings.parseBoolean("true"));
        assertFalse(Strings.parseBoolean("false"));
        assertFalse(Strings.parseBoolean("abc"));
        assertFalse(Strings.parseBoolean(null));
    }

    @Test
    @DisplayName("Test parseByte()")
    public void testParseByte() {
        assertEquals((byte) 123, Strings.parseByte("123"));
        assertEquals((byte) -45, Strings.parseByte("-45"));
        assertThrows(NumberFormatException.class, () -> Strings.parseByte("abc"));
    }

    @Test
    @DisplayName("Test parseShort()")
    public void testParseShort() {
        assertEquals((short) 123, Strings.parseShort("123"));
        assertEquals((short) -456, Strings.parseShort("-456"));
        assertThrows(NumberFormatException.class, () -> Strings.parseShort("abc"));
    }

    @Test
    @DisplayName("Test parseChar()")
    public void testParseChar() {
        assertEquals('a', Strings.parseChar("a"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseChar("abc"));
        assertEquals('\0', Strings.parseChar(null));
    }

    @Test
    @DisplayName("Test createNumber()")
    public void testCreateNumber() {
        assertEquals(123, StrUtil.createNumber("123").orElseThrow());
        assertEquals(123.45d, StrUtil.createNumber("123.45").orElseThrow());
        assertEquals(123L, StrUtil.createNumber("123L").orElseThrow());
        assertTrue(StrUtil.createNumber(null).isEmpty());
    }

    @Test
    @DisplayName("Test createInteger()")
    public void testCreateInteger() {
        assertEquals(Integer.valueOf(123), StrUtil.createInteger("123").orElseThrow());
        assertTrue(StrUtil.createInteger(null).isEmpty());
        assertTrue(StrUtil.createInteger("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createLong()")
    public void testCreateLong() {
        assertEquals(Long.valueOf(123), StrUtil.createLong("123").orElseThrow());
        assertTrue(StrUtil.createLong(null).isEmpty());
        assertTrue(StrUtil.createLong("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createDouble()")
    public void testCreateDouble() {
        assertEquals(Double.valueOf(123.45), StrUtil.createDouble("123.45").orElseThrow());
        assertTrue(StrUtil.createDouble(null).isEmpty());
        assertTrue(StrUtil.createDouble("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createFloat()")
    public void testCreateFloat() {
        assertEquals(Float.valueOf(123.45f), StrUtil.createFloat("123.45").orElseThrow());
        assertTrue(StrUtil.createFloat(null).isEmpty());
        assertTrue(StrUtil.createFloat("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createBigInteger()")
    public void testCreateBigInteger() {
        assertEquals(new BigInteger("123"), StrUtil.createBigInteger("123").orElseThrow());
        assertTrue(StrUtil.createBigInteger(null).isEmpty());
        assertTrue(StrUtil.createBigInteger("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createBigDecimal()")
    public void testCreateBigDecimal() {
        assertEquals(new BigDecimal("123.45"), StrUtil.createBigDecimal("123.45").orElseThrow());
        assertTrue(StrUtil.createBigDecimal(null).isEmpty());
        assertTrue(StrUtil.createBigDecimal("abc").isEmpty());
    }

    @Test
    @DisplayName("Test extractFirstInteger()")
    public void testExtractFirstInteger() {
        assertEquals("123", Strings.extractFirstInteger("abc123def"));
        assertNull(Strings.extractFirstInteger("abc"));
        assertNull(Strings.extractFirstInteger(null));
    }

    @Test
    @DisplayName("Test extractFirstDouble()")
    public void testExtractFirstDouble() {
        assertEquals("123.45", Strings.extractFirstDouble("abc123.45def"));
        assertNull(Strings.extractFirstDouble("abc"));
        assertNull(Strings.extractFirstDouble(null));
    }

    @Test
    @DisplayName("Test findFirstEmailAddress()")
    public void testFindFirstEmailAddress() {
        assertEquals("test@example.com", Strings.findFirstEmailAddress("Contact: test@example.com"));
        assertNull(Strings.findFirstEmailAddress("No email here"));
        assertNull(Strings.findFirstEmailAddress(null));
    }

    @Test
    @DisplayName("Test findAllEmailAddresses()")
    public void testFindAllEmailAddresses() {
        List<String> emails = Strings.findAllEmailAddresses("Contact: test@example.com and admin@test.org");
        assertEquals(2, emails.size());
        assertTrue(emails.contains("test@example.com"));
        assertTrue(emails.contains("admin@test.org"));
    }

    @Test
    @DisplayName("Test reverse()")
    public void testReverse() {
        assertEquals("cba", Strings.reverse("abc"));
        assertEquals("", Strings.reverse(""));
        assertNull(Strings.reverse(null));
    }

    @Test
    @DisplayName("Test reverseDelimited()")
    public void testReverseDelimited() {
        assertEquals("c,b,a", Strings.reverseDelimited("a,b,c", ','));
        assertNull(Strings.reverseDelimited(null, ','));
    }

    @Test
    @DisplayName("Test rotate()")
    public void testRotate() {
        assertEquals("cab", Strings.rotate("abc", 1));
        assertEquals("bca", Strings.rotate("abc", -1));
        assertEquals("abc", Strings.rotate("abc", 3));
        assertNull(Strings.rotate(null, 1));
    }

    @Test
    @DisplayName("Test shuffle()")
    public void testShuffle() {
        String shuffled = Strings.shuffle("abcdef");
        assertNotNull(shuffled);
        assertEquals(6, shuffled.length());
        assertNull(Strings.shuffle(null));
    }

    @Test
    @DisplayName("Test sort()")
    public void testSort() {
        assertEquals("abc", Strings.sort("cba"));
        assertEquals("", Strings.sort(""));
        assertNull(Strings.sort(null));
    }

    @Test
    @DisplayName("Test overlay()")
    public void testOverlay() {
        assertEquals("abXYZfg", Strings.overlay("abcdefg", "XYZ", 2, 5));
        assertEquals("XYZ", Strings.overlay(null, "XYZ", 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.overlay(null, "XYZ", 0, 1));
    }

    @Test
    @DisplayName("Test removeRange()")
    public void testDeleteRange() {
        assertEquals("abfg", Strings.removeRange("abcdefg", 2, 5));
        assertEquals("abcdefg", Strings.removeRange("abcdefg", 2, 2));
        assertEquals("", Strings.removeRange("abc", 0, 3));
        assertEquals("", Strings.removeRange("", 0, 0));
        assertEquals("", Strings.removeRange(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange(null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange("abc", 3, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange("abc", 0, 10));
    }

    @Test
    @DisplayName("Test moveRange()")
    public void testMoveRange() {
        assertEquals("cdeabfg", Strings.moveRange("abcdefg", 2, 5, 0));
        assertEquals("abfgcde", Strings.moveRange("abcdefg", 2, 5, 4));
        assertEquals("abcdefg", Strings.moveRange("abcdefg", 2, 5, 2));
        assertEquals("abcdefg", Strings.moveRange("abcdefg", 3, 3, 0));
        assertEquals("", Strings.moveRange(null, 0, 0, 0));
        assertEquals("", Strings.moveRange("", 0, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 1, 2));
    }

    @Test
    @DisplayName("Test replaceRange()")
    public void testReplaceRange() {
        assertEquals("abXYZfg", Strings.replaceRange("abcdefg", 2, 5, "XYZ"));
        assertEquals("X", Strings.replaceRange("", 0, 0, "X"));
        assertEquals("X", Strings.replaceRange(null, 0, 0, "X"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceRange(null, 0, 1, "X"));
    }

    @Test
    @DisplayName("Test replaceFirstInteger()")
    public void testReplaceFirstInteger() {
        assertEquals("abcXYZdef", Strings.replaceFirstInteger("abc123def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstInteger("abc", "XYZ"));
        assertEquals("", Strings.replaceFirstInteger(null, "XYZ"));
    }

    @Test
    @DisplayName("Test replaceFirstDouble()")
    public void testReplaceFirstDouble() {
        assertEquals("abcXYZdef", Strings.replaceFirstDouble("abc123.45def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstDouble("abc", "XYZ"));
        assertEquals("", Strings.replaceFirstDouble(null, "XYZ"));
    }

    @Test
    @DisplayName("Test replaceIgnoreCase()")
    public void testReplaceIgnoreCase() {
        assertEquals("xyzxyz", Strings.replaceIgnoreCase("AbCABC", 0, "abc", "xyz", 2));
        assertNull(Strings.replaceIgnoreCase(null, 0, "abc", "xyz", 1));
    }

    @Test
    @DisplayName("Test copyThenTrim()")
    public void testCopyThenTrim() {
        String[] result = Strings.copyThenTrim(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenTrim((String[]) null));
    }

    @Test
    @DisplayName("Test copyThenStrip()")
    public void testCopyThenStrip() {
        String[] result = Strings.copyThenStrip(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenStrip((String[]) null));
    }

    @Test
    @DisplayName("Test isAsciiInteger()")
    public void testisAsciiInteger() {
        assertTrue(Strings.isAsciiInteger("123"));
        assertTrue(Strings.isAsciiInteger("-123"));
        assertTrue(Strings.isAsciiInteger("+123"));
        assertFalse(Strings.isAsciiInteger("12.3"));
        assertFalse(Strings.isAsciiInteger("abc"));
        assertFalse(Strings.isAsciiInteger(null));
        assertFalse(Strings.isAsciiInteger(""));
    }

    @Test
    @DisplayName("Test lenientFormat()")
    public void testLenientFormat() {
        assertEquals("Hello World", Strings.lenientFormat("Hello %s", "World"));
        assertEquals("Value: 123", Strings.lenientFormat("Value: %s", 123));
        assertNotNull(Strings.lenientFormat("Test %s %s", "a"));
    }

    // ===== Tests for substringBetweenFirstAndLast methods in Strings class =====

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, String) - basic cases")
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
    @DisplayName("Test substringBetweenFirstAndLast(String, String) - multiple occurrences")
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_Multiple() {
        assertEquals("a::b::c", Strings.substringBetweenFirstAndLast("::a::b::c::", "::"));
        assertEquals("first**second", Strings.substringBetweenFirstAndLast("**first**second**", "**"));
        assertEquals("1|2|3", Strings.substringBetweenFirstAndLast("|1|2|3|", "|"));
    }

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, String) - adjacent delimiters")
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_Adjacent() {
        assertEquals("", Strings.substringBetweenFirstAndLast("[[]]", "["));
        assertEquals("", Strings.substringBetweenFirstAndLast("####", "##"));
    }

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, String) - single occurrence")
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_OnlyOne() {
        assertNull(Strings.substringBetweenFirstAndLast("[content", "["));
        assertNull(Strings.substringBetweenFirstAndLast("content]", "]"));
    }

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, String, String) - basic cases")
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
    @DisplayName("Test substringBetweenFirstAndLast(String, String, String) - nested delimiters")
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_Nested() {
        assertEquals("outer<inner>content</inner>outer", Strings.substringBetweenFirstAndLast("<outer<inner>content</inner>outer>", "<", ">"));
        assertEquals("a[b[c]]d", Strings.substringBetweenFirstAndLast("[a[b[c]]d]", "[", "]"));
    }

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, String, String) - multiple pairs")
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_MultiplePairs() {
        assertEquals("first</tag><tag>second", Strings.substringBetweenFirstAndLast("<tag>first</tag><tag>second</tag>", "<tag>", "</tag>"));
        assertEquals("a](b)[c", Strings.substringBetweenFirstAndLast("[a](b)[c]", "[", "]"));
    }

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, String, String) - same delimiters")
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_SameDelimiters() {
        assertEquals("content", Strings.substringBetweenFirstAndLast("|content|", "|", "|"));
        assertEquals("a|b", Strings.substringBetweenFirstAndLast("|a|b|", "|", "|"));
    }

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, String, String) - empty result")
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_EmptyResult() {
        assertEquals("", Strings.substringBetweenFirstAndLast("<>", "<", ">"));
        assertEquals("", Strings.substringBetweenFirstAndLast("[]", "[", "]"));
    }

    @Test
    @DisplayName("Test substringBetweenFirstAndLast(String, int, String, String) - basic cases")
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

    @Test
    public void testConstants() {
        assertEquals("null", Strings.NULL);
        assertEquals("", Strings.EMPTY);
        assertEquals(" ", Strings.SPACE);
        assertEquals("\n", Strings.LF);
        assertEquals("\r", Strings.CR);
        assertEquals("\r\n", Strings.CR_LF);
        assertEquals('\0', Strings.CHAR_ZERO);
        assertEquals(' ', Strings.CHAR_SPACE);
        assertEquals('\n', Strings.CHAR_LF);
        assertEquals('\r', Strings.CHAR_CR);
        assertEquals(", ", Strings.COMMA_SPACE);
        assertEquals(", ", Strings.ELEMENT_SEPARATOR);
    }

    @Test
    public void testValueOfCharArray() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testIsKeyword() {
        assertTrue(Strings.isKeyword("int"));
        assertTrue(Strings.isKeyword("for"));
        assertFalse(Strings.isKeyword("myVar"));
        assertFalse(Strings.isKeyword(null));
        assertFalse(Strings.isKeyword(""));
    }

    @Test
    public void testIsValidJavaIdentifier() {
        assertTrue(Strings.isValidJavaIdentifier("myVar"));
        assertTrue(Strings.isValidJavaIdentifier("_myVar"));
        assertTrue(Strings.isValidJavaIdentifier("$myVar"));
        assertTrue(Strings.isValidJavaIdentifier("myVar123"));
        assertFalse(Strings.isValidJavaIdentifier("123myVar"));
        assertFalse(Strings.isValidJavaIdentifier("my-Var"));
        assertTrue(Strings.isValidJavaIdentifier("class"));
        assertFalse(Strings.isValidJavaIdentifier(null));
        assertFalse(Strings.isValidJavaIdentifier(""));
    }

    @Test
    public void testIsValidEmailAddress() {
        assertTrue(Strings.isValidEmailAddress("test@example.com"));
        assertTrue(Strings.isValidEmailAddress("test.name@example.co.uk"));
        assertFalse(Strings.isValidEmailAddress("test@example"));
        assertFalse(Strings.isValidEmailAddress("test"));
        assertFalse(Strings.isValidEmailAddress(null));
        assertFalse(Strings.isValidEmailAddress(""));
        assertTrue(Strings.isValidEmailAddress("user.name+tag+sorting@example.com"));
        assertFalse(Strings.isValidEmailAddress("user name@example.com"));

    }

    @Test
    public void testIsValidUrl() {
        assertTrue(Strings.isValidUrl("http://example.com"));
        assertTrue(Strings.isValidUrl("https://example.com/path?query=val#fragment"));
        assertTrue(Strings.isValidUrl("ftp://user:pass@example.com:21/path"));
        assertFalse(Strings.isValidUrl("example.com"));
        assertFalse(Strings.isValidHttpUrl("htp://example.com")); // Invalid scheme
        assertFalse(Strings.isValidUrl(null));
        assertFalse(Strings.isValidUrl(""));
    }

    @Test
    public void testIsValidHttpUrl() {
        assertTrue(Strings.isValidHttpUrl("http://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com/path"));
        assertFalse(Strings.isValidHttpUrl("ftp://example.com"));
        assertFalse(Strings.isValidHttpUrl("example.com"));
        assertFalse(Strings.isValidHttpUrl(null));
        assertFalse(Strings.isValidHttpUrl(""));
    }

    @Test
    public void testIsAllEmptyVarArgs() {
        assertTrue(Strings.isAllEmpty((CharSequence) null));
        assertTrue(Strings.isAllEmpty(null, null));
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertFalse(Strings.isAllEmpty("a", ""));
        assertFalse(Strings.isAllEmpty("a", "b"));
        assertTrue(Strings.isAllEmpty());
    }

    @Test
    public void testIsAllEmptyIterable() {
        assertTrue(Strings.isAllEmpty(list(null, "", null)));
        assertFalse(Strings.isAllEmpty(list(null, "a", "")));
        assertTrue(Strings.isAllEmpty(new ArrayList<>()));
        assertTrue(Strings.isAllEmpty((Iterable<String>) null));
    }

    @Test
    public void testIsAllBlankVarArgs() {
        assertTrue(Strings.isAllBlank((CharSequence) null));
        assertTrue(Strings.isAllBlank(null, null, " ", "\t"));
        assertFalse(Strings.isAllBlank(null, "a", " "));
        assertTrue(Strings.isAllBlank());
    }

    @Test
    public void testIsAllBlankIterable() {
        assertTrue(Strings.isAllBlank(list(null, " ", "\n")));
        assertFalse(Strings.isAllBlank(list(null, "a", " ")));
        assertTrue(Strings.isAllBlank(new ArrayList<>()));
        assertTrue(Strings.isAllBlank((Iterable<String>) null));
    }

    @Test
    public void testIsAnyEmptyVarArgs() {
        assertTrue(Strings.isAnyEmpty(null, "a"));
        assertTrue(Strings.isAnyEmpty("", "a"));
        assertFalse(Strings.isAnyEmpty("a", "b"));
        assertFalse(Strings.isAnyEmpty());
        assertTrue(Strings.isAnyEmpty("a", "b", null, "d"));
    }

    @Test
    public void testIsAnyEmptyIterable() {
        assertTrue(Strings.isAnyEmpty(list("a", "", "b")));
        assertFalse(Strings.isAnyEmpty(list("a", "b")));
        assertFalse(Strings.isAnyEmpty(new ArrayList<>()));
        assertFalse(Strings.isAnyEmpty((Iterable<String>) null));
    }

    @Test
    public void testIsAnyBlankVarArgs() {
        assertTrue(Strings.isAnyBlank(null, "a"));
        assertTrue(Strings.isAnyBlank(" ", "a"));
        assertFalse(Strings.isAnyBlank("a", "b"));
        assertFalse(Strings.isAnyBlank());
        assertTrue(Strings.isAnyBlank("a", "b", "\t", "d"));

    }

    @Test
    public void testIsAnyBlankIterable() {
        assertTrue(Strings.isAnyBlank(list("a", " ", "b")));
        assertFalse(Strings.isAnyBlank(list("a", "b")));
        assertFalse(Strings.isAnyBlank(new ArrayList<>()));
        assertFalse(Strings.isAnyBlank((Iterable<String>) null));
    }

    @Test
    public void testIsWrappedWithSingleArg() {
        assertFalse(Strings.isWrappedWith("[abc]", "["));
        assertFalse(Strings.isWrappedWith("[abc]", "["));
        assertTrue(Strings.isWrappedWith("wrap_me_wrap", "wrap"));
        assertFalse(Strings.isWrappedWith("wrap_me_no", "wrap"));
        assertFalse(Strings.isWrappedWith("nowrap_me_wrap", "wrap"));
        assertFalse(Strings.isWrappedWith("short", "longer_wrap"));
        assertFalse(Strings.isWrappedWith(null, "wrap"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", ""));
    }

    @Test
    public void testIsWrappedWithTwoArgs() {
        assertTrue(Strings.isWrappedWith("[abc]", "[", "]"));
        assertFalse(Strings.isWrappedWith("(abc]", "[", "]"));
        assertFalse(Strings.isWrappedWith("[abc)", "[", "]"));
        assertFalse(Strings.isWrappedWith("short", "long_prefix", "]"));
        assertFalse(Strings.isWrappedWith(null, "[", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", "", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", "[", ""));
    }

    @Test
    public void testFirstNonEmptyVarArgs() {
        assertEquals("a", Strings.firstNonEmpty(null, "", "a", "b"));
        assertEquals("", Strings.firstNonEmpty(null, ""));
        assertEquals("", Strings.firstNonEmpty());
    }

    @Test
    public void testFirstNonEmptyIterable() {
        assertEquals("a", Strings.firstNonEmpty(list(null, "", "a", "b")));
        assertEquals("", Strings.firstNonEmpty(list(null, "")));
        assertEquals("", Strings.firstNonEmpty(new ArrayList<>()));
        assertEquals("", Strings.firstNonEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstNonBlankVarArgs() {
        assertEquals("a", Strings.firstNonBlank(null, " ", "\t", "a", "b"));
        assertEquals("", Strings.firstNonBlank(null, " ", "\t"));
        assertEquals("", Strings.firstNonBlank());
    }

    @Test
    public void testFirstNonBlankIterable() {
        assertEquals("a", Strings.firstNonBlank(list(null, " ", "\t", "a", "b")));
        assertEquals("", Strings.firstNonBlank(list(null, " ", "\t")));
        assertEquals("", Strings.firstNonBlank(new ArrayList<>()));
        assertEquals("", Strings.firstNonBlank((Iterable<String>) null));
    }

    @Test
    public void testDefaultIfNull() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
        assertEquals("abc", Strings.defaultIfNull("abc", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull("abc", (String) null));
    }

    @Test
    public void testDefaultIfNullSupplier() {
        assertEquals("supplied", Strings.defaultIfNull((String) null, Fn.s(() -> "supplied")));
        assertEquals("abc", Strings.defaultIfNull("abc", Fn.s(() -> "supplied")));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((CharSequence) null, (Supplier<? extends CharSequence>) () -> null));
    }

    @Test
    public void testDefaultIfEmpty() {
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("abc", Strings.defaultIfEmpty("abc", "default"));
        assertEquals(" ", Strings.defaultIfEmpty(" ", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("abc", ""));
    }

    @Test
    public void testDefaultIfEmptySupplier() {
        Supplier<String> supplier = () -> "supplied";
        assertEquals("supplied", Strings.defaultIfEmpty("", supplier));
        assertEquals("abc", Strings.defaultIfEmpty("abc", supplier));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty("", Fn.s(() -> "")));
    }

    @Test
    public void testDefaultIfBlank() {
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank(" ", "default"));
        assertEquals("abc", Strings.defaultIfBlank("abc", "default"));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank("abc", " "));
    }

    @Test
    public void testDefaultIfBlankSupplier() {
        Supplier<String> supplier = () -> "supplied";
        assertEquals("supplied", Strings.defaultIfBlank(" ", supplier));
        assertEquals("abc", Strings.defaultIfBlank("abc", supplier));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank(" ", Fn.s(() -> " ")));
    }

    @Test
    public void testNullToEmptyString() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
    }

    @Test
    public void testNullElementsToEmptyArray() {
        String[] arr = { null, "a", null, "" };
        Strings.nullElementsToEmpty(arr);
        assertArrayEquals(new String[] { "", "a", "", "" }, arr);
        Strings.nullElementsToEmpty((String[]) null);
    }

    @Test
    public void testEmptyToNullString() {
        assertNull(Strings.emptyToNull((String) null));
        assertNull(Strings.emptyToNull(""));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals(" ", Strings.emptyToNull(" "));
    }

    @Test
    public void testEmptyElementsToNullArray() {
        String[] arr = { null, "a", "", " " };
        Strings.emptyElementsToNull(arr);
        assertArrayEquals(new String[] { null, "a", null, " " }, arr);
        Strings.emptyElementsToNull((String[]) null);
    }

    @Test
    public void testBlankToEmptyString() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
    }

    @Test
    public void testBlankElementsToEmptyArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankElementsToEmpty(arr);
        assertArrayEquals(new String[] { "", "a", "", "" }, arr);
    }

    @Test
    public void testBlankToNullString() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
    }

    @Test
    public void testBlankElementsToNullArray() {
        String[] arr = { null, "a", " ", "\t" };
        Strings.blankElementsToNull(arr);
        assertArrayEquals(new String[] { null, "a", null, null }, arr);
    }

    @Test
    public void testAbbreviateMaxWidth() {
        assertNull(Strings.abbreviate(null, 10));
        assertEquals("", Strings.abbreviate("", 10));
        assertEquals("abc...", Strings.abbreviate("abcdefg", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("a...", Strings.abbreviate("abcdefg", 4));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", 3));
    }

    @Test
    public void testAbbreviateMarkerMaxWidth() {
        assertNull(Strings.abbreviate(null, "...", 10));
        assertEquals("", Strings.abbreviate("", "...", 10));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", null, 10));
        assertEquals("abc..", Strings.abbreviate("abcdefg", "..", 5));
        assertEquals("a..", Strings.abbreviate("abcdefg", "..", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abc", "..", 2));
    }

    @Test
    public void testCenterSize() {
        assertEquals("    ", Strings.center(null, 4));
        assertEquals("    ", Strings.center("", 4));
        assertEquals(" ab ", Strings.center("ab", 4));
        assertEquals("abcd", Strings.center("abcd", 2));
        assertEquals(" a  ", Strings.center("a", 4));
    }

    @Test
    public void testCenterSizeChar() {
        assertEquals("yyyy", Strings.center(null, 4, 'y'));
        assertEquals("yayy", Strings.center("a", 4, 'y'));
    }

    @Test
    public void testCenterSizeStr() {
        assertEquals("yzayz", Strings.center("a", 5, "yz"));
        assertEquals("  abc  ", Strings.center("abc", 7, ""));
        assertEquals("ab", Strings.center("ab", 1, "yz"));
    }

    @Test
    public void testRepeatString() {
        assertEquals("ababab", Strings.repeat("ab", 3));
        assertEquals("", Strings.repeat("ab", 0));
        assertEquals("", Strings.repeat(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("ab", -1));
    }

    @Test
    public void testRepeatStringWithDelimiter() {
        assertEquals("ab,ab,ab", Strings.repeat("ab", 3, ","));
        assertEquals("[ab,ab]", Strings.repeat("ab", 2, ",", "[", "]"));
        assertEquals("[ab]", Strings.repeat("ab", 1, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("ab", 0, ",", "[", "]"));
        assertEquals("[,]", Strings.repeat("", 2, ",", "[", "]"));
    }

    @Test
    public void testToLowerCaseChar() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals('a', Strings.toLowerCase('a'));
    }

    @Test
    public void testToLowerCaseString() {
        assertNull(Strings.toLowerCase(null));
        assertEquals("", Strings.toLowerCase(""));
        assertEquals("abc", Strings.toLowerCase("aBc"));
    }

    @Test
    public void testToLowerCaseStringLocale() {
        assertEquals("ı", Strings.toLowerCase("I", new Locale("tr")));
    }

    @Test
    public void testToUpperCaseChar() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals('A', Strings.toUpperCase('A'));
    }

    @Test
    public void testToUpperCaseString() {
        assertNull(Strings.toUpperCase(null));
        assertEquals("", Strings.toUpperCase(""));
        assertEquals("ABC", Strings.toUpperCase("aBc"));
    }

    @Test
    public void testSwapCaseChar() {
        assertEquals('a', Strings.swapCase('A'));
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('1', Strings.swapCase('1'));
    }

    @Test
    public void testSwapCaseString() {
        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
        assertEquals("tHE dOG hAS a bONE", Strings.swapCase("The Dog Has A Bone"));
    }

    @Test
    public void testCapitalizeFullyWithExclusions() {
        assertEquals("The First Name of the Person", Strings.capitalizeFully("the first name of the person", " ", "the", "of"));
    }

    @Test
    public void testQuoteEscapedWithChar() {
        assertEquals("ab\\\"c", Strings.quoteEscaped("ab\"c", '"'));
        assertEquals("ab'c", Strings.quoteEscaped("ab'c", '"')); // ' not escaped
        assertNull(Strings.quoteEscaped(null, '"'));
    }

    @Test
    public void testReplaceAllWithFromIndex() {
        assertEquals("azbzz", Strings.replaceAll("aabaa", 1, "a", "z"));
    }

    @Test
    public void testReplaceLastWithStartIndex() {
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 1, "a", "z"));

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
    public void testSplitChar() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a.b.c", '.'));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a..b.c", '.'));
    }

    @Test
    public void testSplitStringTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a . b . c ", '.', true));
    }

    @Test
    public void testSplitStringMax() {
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.split("a.b.c", ".", 2));
    }

    @Test
    public void testSplitPreserveAllTokensChar() {
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a..b.c", '.'));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", '.'));
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, '.'));
        assertArrayEquals(new String[] { "", "a", "b", "" }, Strings.splitPreserveAllTokens(".a.b.", '.'));
    }

    @Test
    public void testSplitPreserveAllTokensStringMaxTrim() {
        assertArrayEquals(new String[] { "a", "b.c" }, Strings.splitPreserveAllTokens("a ..b.c ", "..", 3, true));
    }

    @Test
    public void testSplitToLinesWithOptions() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\n b \n\n c \r\n", true, true));
        assertArrayEquals(new String[] { "a", "b", "", "c", "" }, Strings.splitToLines("a\nb\n\nc\n", false, false));
    }

    @Test
    public void testTrimArray() {
        String[] arr = { "  a  ", null, "b" };
        Strings.trim(arr);
        assertArrayEquals(new String[] { "a", null, "b" }, arr);
    }

    @Test
    public void testStripWithChars() {
        assertEquals("abc", Strings.strip("xyabcxyz", "xyz"));
    }

    @Test
    public void testTruncateWithOffset() {
        assertEquals("cde", Strings.truncate("abcdefg", 2, 3));
        assertEquals("", Strings.truncate("abc", 3, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", -1, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("abc", 0, -1));
    }

    @Test
    public void testIsAsciiPrintableCharSequence() {
        assertTrue(Strings.isAsciiPrintable("abc"));
        assertFalse(Strings.isAsciiPrintable("éclair"));
        assertFalse(Strings.isAsciiPrintable(null));
    }

    @Test
    public void testLastIndexOfAny() {
        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

        assertEquals(5, Strings.lastIndexOfAny("abacaba", 'b', 'c'));
        assertEquals(3, Strings.lastIndexOfAny("abacaba", 'c', 'b'));

    }

    @Test
    public void testSubstring() {
        assertEquals("cde", Strings.substring("abcde", 2));
        assertNull(Strings.substring("abc", 5));
        assertNull(Strings.substring(null, 1));
    }

    @Test
    public void testSubstringWithEnd() {
        assertEquals("cd", Strings.substring("abcde", 2, 4));
        assertEquals("cde", Strings.substring("abcde", 2, 10));
        assertNull(Strings.substring("abc", 2, 1));
    }

    @Test
    public void testStrUtilSubstringOptional() {
        assertTrue(StrUtil.substring("abc", 1).isPresent());
        assertEquals("bc", StrUtil.substring("abc", 1).get());
        assertFalse(StrUtil.substring("abc", 5).isPresent());
    }

    @Test
    public void testSubstringsBetweenDefaultStrategy() {
        assertEquals(list("a", "b"), Strings.substringsBetween("[a][b]", '[', ']'));
        assertEquals(list("a[b"), Strings.substringsBetween("[a[b]c]", '[', ']'));
    }

    @Test
    public void testSubstringsBetweenStackBased() {
        assertEquals(list("c", "a2[c]", "a"), substringsBetween_StackBased_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("a", "b[a]", "[b[a]]c"), substringsBetween_StackBased_("[[b[a]]c]", '[', ']'));
    }

    @Test
    public void testSubstringsBetweenIgnoreNested() {
        assertEquals(list("a2[c]", "a"), substringsBetween_IgnoreNested_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("[b[a]]c"), substringsBetween_IgnoreNested_("[[b[a]]c]", '[', ']'));
    }

    @Test
    public void testJoinObjectArray() {
        assertEquals("a, b, c", Strings.join(new Object[] { "a", "b", "c" }));
        assertEquals("a|b|c", Strings.join(new Object[] { "a", "b", "c" }, "|"));
        assertEquals("[a|b|c]", Strings.join(new Object[] { "a", "b", "c" }, "|", "[", "]"));
    }

    @Test
    public void testJoinIterable() {
        assertEquals("1, 2, 3", Strings.join(list(1, 2, 3)));
        assertEquals("1-2-3", Strings.join(list(1, 2, 3), "-"));
    }

    @Test
    public void testJoinEntriesMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1, b=2", Strings.joinEntries(map));
        assertEquals("a:1;b:2", Strings.joinEntries(map, ";", ":"));
        assertEquals("{a:1;b:2}", Strings.joinEntries(map, ";", ":", "{", "}"));
    }

    @Test
    public void testConcatStrings() {
        assertEquals("abc", Strings.concat("a", "b", "c"));
        assertEquals("a", Strings.concat("a", null, ""));
        assertEquals("", Strings.concat(null, null));
    }

    @Test
    public void testBase64() {
        String original = "hello world";
        String encoded = Strings.base64EncodeString(original);
        assertNotNull(encoded);
        assertNotEquals(original, encoded);
        assertEquals(original, Strings.base64DecodeToString(encoded));

        byte[] data = { 0, 1, 2, 3, 4, 5 };
        String encodedBytes = Strings.base64Encode(data);
        assertArrayEquals(data, Strings.base64Decode(encodedBytes));

        String originalUrl = "abc?=/&123";
        byte[] originalUrlBytes = Strings.getBytesUtf8(originalUrl);
        String urlEncoded = Strings.base64UrlEncode(originalUrlBytes);
        assertFalse(urlEncoded.contains("+"));
        assertFalse(urlEncoded.contains("/"));
        assertFalse(urlEncoded.contains("="));
        assertArrayEquals(originalUrlBytes, Strings.base64UrlDecode(urlEncoded));
        assertEquals(originalUrl, Strings.base64UrlDecodeToUtf8String(urlEncoded));
    }

    @Test
    public void testUrlEncodeDecode() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "John Doe");
        params.put("city", "New York");
        String encoded = Strings.urlEncode(params);
        assertEquals("name=John+Doe&city=New+York", encoded);

        Map<String, String> decoded = Strings.urlDecode(encoded);
        assertEquals("John Doe", decoded.get("name"));
        assertEquals("New York", decoded.get("city"));
    }

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
    public void testStrUtilSubstringOrElse() {
        assertEquals("bc", StrUtil.substringOrElse("abc", 1, "default"));
        assertEquals("default", StrUtil.substringOrElse("abc", 4, "default"));
        assertEquals("default", StrUtil.substringOrElse(null, 1, "default"));
    }

    @Test
    public void testStrUtilSubstringOrElseItself() {
        assertEquals("bc", StrUtil.substringOrElseItself("abc", 1));
        assertEquals("abc", StrUtil.substringOrElseItself("abc", 4));
        assertNull(StrUtil.substringOrElseItself(null, 1));
    }

    @Test
    public void testStrUtilSubstringAfter() {
        assertEquals("cde", StrUtil.substringAfter("abcde", "ab").get());
        assertFalse(StrUtil.substringAfter("abc", "x").isPresent());
    }

    @Test
    public void testStrUtilSubstringBefore() {
        assertEquals("ab", StrUtil.substringBefore("abcde", "cd").get());
        assertFalse(StrUtil.substringBefore("abc", "x").isPresent());
    }

    @Test
    public void testStrUtilSubstringBetween() {
        assertEquals("abc", StrUtil.substringBetween("(abc)", "(", ")").get());
        assertFalse(StrUtil.substringBetween("abc", "[", "]").isPresent());
    }

    @Test
    public void testStrUtilCreateInteger() {
        assertTrue(StrUtil.createInteger("123").isPresent());
        assertEquals(123, StrUtil.createInteger("123").getAsInt());
        assertFalse(StrUtil.createInteger("abc").isPresent());
        assertFalse(StrUtil.createInteger("").isPresent());
        assertFalse(StrUtil.createInteger(null).isPresent());
        assertFalse(StrUtil.createInteger("123.45").isPresent());
    }

    @Test
    public void testStrUtilCreateLong() {
        assertTrue(StrUtil.createLong("12345678900").isPresent());
        assertEquals(12345678900L, StrUtil.createLong("12345678900").getAsLong());
        assertFalse(StrUtil.createLong("abc").isPresent());
    }

    @Test
    public void testStrUtilCreateFloat() {
        assertTrue(StrUtil.createFloat("123.45").isPresent());
        assertEquals(123.45f, StrUtil.createFloat("123.45").get(), 0.001f);
        assertFalse(StrUtil.createFloat("abc").isPresent());
    }

    @Test
    public void testStrUtilCreateDouble() {
        assertTrue(StrUtil.createDouble("123.456789").isPresent());
        assertEquals(123.456789, StrUtil.createDouble("123.456789").getAsDouble(), 0.000001);
        assertFalse(StrUtil.createDouble("abc").isPresent());
    }

    @Test
    public void testStrUtilCreateBigInteger() {
        assertTrue(StrUtil.createBigInteger("12345678901234567890").isPresent());
        assertEquals(new BigInteger("12345678901234567890"), StrUtil.createBigInteger("12345678901234567890").get());
        assertFalse(StrUtil.createBigInteger("abc.def").isPresent());
    }

    @Test
    public void testStrUtilCreateBigDecimal() {
        assertTrue(StrUtil.createBigDecimal("123.4567890123456789").isPresent());
        assertEquals(new BigDecimal("123.4567890123456789"), StrUtil.createBigDecimal("123.4567890123456789").get());
        assertFalse(StrUtil.createBigDecimal("abc efg").isPresent());
    }

    @Test
    public void testStrUtilCreateNumber() {
        assertTrue(StrUtil.createNumber("123").isPresent());
        assertEquals(123, StrUtil.createNumber("123").get());
        assertTrue(StrUtil.createNumber("123.45").isPresent());
        assertEquals(123.45, StrUtil.createNumber("123.45").get());
        assertFalse(StrUtil.createNumber("12L3").isPresent());
    }

    @Test
    public void testReplaceOnceWithIndex() {
        Assertions.assertEquals("abzde", Strings.replaceOnce("abcde", 0, "c", "z"));
        Assertions.assertEquals("abcde", Strings.replaceOnce("abcde", 3, "c", "z"));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceOnce("abcde", -1, "a", "z"));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceOnce("abcde", 10, "a", "z"));
    }

    @Test
    public void testSplitWithTrim() {
        Assertions.assertArrayEquals(new String[0], Strings.split(null, "::", true));
        Assertions.assertArrayEquals(new String[0], Strings.split("", "::", true));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a :: b :: c ", "::", true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a :: b :: c ", "::", false));

        Assertions.assertArrayEquals(new String[] { "a", "b" }, Strings.split("a::::b", "::", true));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a  b  c ", null, true));
    }

    @Test
    public void testSplitWithMax() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ":", -1));

        Assertions.assertArrayEquals(new String[0], Strings.split(null, ":", 3));
        Assertions.assertArrayEquals(new String[0], Strings.split("", ":", 3));

        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c:d:e" }, Strings.split("a:b:c:d:e", ":", 3));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 5));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Strings.split("a:b:c:d:e", ":", 10));
    }

    @Test
    public void testSplitWithMaxAndTrim() {
        Assertions.assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.split(" a : b : c : d ", ":", 3, true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.split(" a : b : c : d ", ":", 3, false));

        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.split("a:b:c", ":", 1, true));
        Assertions.assertArrayEquals(new String[] { "  a:b:c  " }, Strings.split("  a:b:c  ", ":", 1, false));
    }

    @Test
    public void testSplitPreserveAllTokensCharWithTrim() {
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a . b . c ", '.', true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a . b . c ", '.', false));

        Assertions.assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a..b", '.', true));
        Assertions.assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" .. ", '.', true));
    }

    @Test
    public void testSplitPreserveAllTokensString() {
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, "::"));
        Assertions.assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", "::"));

        Assertions.assertArrayEquals(new String[] { "ab", "cd", "ef" }, Strings.splitPreserveAllTokens("ab:cd:ef", ":"));
        Assertions.assertArrayEquals(new String[] { "ab", "cd", "ef", "" }, Strings.splitPreserveAllTokens("ab:cd:ef:", ":"));
        Assertions.assertArrayEquals(new String[] { "", "cd", "ef" }, Strings.splitPreserveAllTokens(":cd:ef", ":"));

        Assertions.assertArrayEquals(new String[] { "abc", "def" }, Strings.splitPreserveAllTokens("abc def", null));
        Assertions.assertArrayEquals(new String[] { "abc", "", "def" }, Strings.splitPreserveAllTokens("abc  def", " "));
    }

    @Test
    public void testSplitPreserveAllTokensStringWithTrim() {
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a : b : c ", ":", true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.splitPreserveAllTokens(" a : b : c ", ":", false));

        Assertions.assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::b", ":", true));
        Assertions.assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" :: ", ":", true));
    }

    @Test
    public void testSplitPreserveAllTokensWithMax() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("test", ":", 0));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c:d" }, Strings.splitPreserveAllTokens("a:b:c:d", ":", 3));
        Assertions.assertArrayEquals(new String[] { "a", "b", "::d" }, Strings.splitPreserveAllTokens("a:b:::d", ":", 3));
        Assertions.assertArrayEquals(new String[] { "", "a", "b", "c" }, Strings.splitPreserveAllTokens(":a:b:c", ":", 4));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c", "" }, Strings.splitPreserveAllTokens("a:b:c:", ":", 4));

        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1));
    }

    @Test
    public void testSplitPreserveAllTokensWithMaxAndTrim() {
        Assertions.assertArrayEquals(new String[] { "a", "b", "c : d" }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, true));
        Assertions.assertArrayEquals(new String[] { " a ", " b ", " c : d " }, Strings.splitPreserveAllTokens(" a : b : c : d ", ":", 3, false));

        Assertions.assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a::b:c", ":", 4, true));
        Assertions.assertArrayEquals(new String[] { "", "", "" }, Strings.splitPreserveAllTokens(" : : ", ":", 3, true));

        Assertions.assertArrayEquals(new String[] { "a:b:c" }, Strings.splitPreserveAllTokens("a:b:c", ":", 1, true));
        Assertions.assertArrayEquals(new String[] { "  a:b:c  " }, Strings.splitPreserveAllTokens("  a:b:c  ", ":", 1, false));
    }

    @Test
    public void testStripStartArray() {
        String[] arr = { "xxabc", "xydef", "xyz", null };
        Strings.stripStart(arr, "xyz");
        Assertions.assertArrayEquals(new String[] { "abc", "def", "", null }, arr);

        String[] arr2 = { "  test", null, " \tabc" };
        Strings.stripStart(arr2, null);
        Assertions.assertArrayEquals(new String[] { "test", null, "abc" }, arr2);

        Strings.stripStart((String[]) null, "xyz");

        Strings.stripStart(new String[0], "xyz");
    }

    @Test
    public void testStripEndArray() {
        String[] arr = { "abcxx", "defxy", "xyz", null };
        Strings.stripEnd(arr, "xyz");
        Assertions.assertArrayEquals(new String[] { "abc", "def", "", null }, arr);

        String[] arr2 = { "test  ", null, "abc \t" };
        Strings.stripEnd(arr2, null);
        Assertions.assertArrayEquals(new String[] { "test", null, "abc" }, arr2);

        Strings.stripEnd((String[]) null, "xyz");

        Strings.stripEnd(new String[0], "xyz");
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiter() {
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,APPLE,banana", "apple", ","));
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,APPLE,banana", "APPLE", ","));
        Assertions.assertEquals(10, Strings.indexOfIgnoreCase("pineapple,apple", "apple", ","));
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("test", "test", ","));

        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase(null, "test", ","));
        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("test", null, ","));

        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("test", "test", ""));

        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("apple banana", "apple", ","));
    }

    @Test
    public void testIndexOfIgnoreCaseWithDelimiterAndFromIndex() {
        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,APPLE,banana", "apple", ",", 0));
        Assertions.assertEquals(6, Strings.indexOfIgnoreCase("apple,APPLE,banana", "apple", ",", 5));
        Assertions.assertEquals(10, Strings.indexOfIgnoreCase("pineapple,apple", "apple", ",", 0));
        Assertions.assertEquals(11, Strings.indexOfIgnoreCase("test value test", "test", " ", 5));

        Assertions.assertEquals(0, Strings.indexOfIgnoreCase("apple,banana", "apple", ",", -5));

        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("apple", "apple", ",", 10));
    }

    @Test
    public void testminLastIndexOfAllWithIndex() {
        Assertions.assertEquals(6, Strings.minLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(4, Strings.minLastIndexOfAll("Hello World", 5, "o", "World"));
        Assertions.assertEquals(-1, Strings.minLastIndexOfAll("Hello World", 3, "o", "World"));

        Assertions.assertEquals(-1, Strings.minLastIndexOfAll("test", -1, "test"));

        Assertions.assertEquals(0, Strings.minLastIndexOfAll("test", 100, "test"));
    }

    @Test
    public void testmaxLastIndexOfAllWithIndex() {
        Assertions.assertEquals(7, Strings.maxLastIndexOfAll("Hello World", 10, "o", "World"));
        Assertions.assertEquals(6, Strings.maxLastIndexOfAll("Hello World", 6, "o", "World"));
        Assertions.assertEquals(0, Strings.maxLastIndexOfAll("Hello World", 3, "o", "H"));

        Assertions.assertEquals(-1, Strings.maxLastIndexOfAll("test", -1, "test"));

        Assertions.assertEquals(0, Strings.maxLastIndexOfAll("test", 100, "test"));
    }

    @Test
    public void testContainsIgnoreCaseWithDelimiter() {
        Assertions.assertTrue(Strings.containsIgnoreCase("Apple,Banana,Orange", "banana", ","));
        Assertions.assertTrue(Strings.containsIgnoreCase("ONE TWO THREE", "two", " "));
        Assertions.assertTrue(Strings.containsIgnoreCase("One-Two-Three", "TWO", "-"));

        Assertions.assertFalse(Strings.containsIgnoreCase("Apple,Banana,Orange", "BAN", ","));

        Assertions.assertFalse(Strings.containsIgnoreCase(null, "test", ","));
        Assertions.assertFalse(Strings.containsIgnoreCase("test", null, ","));
    }

    @Test
    public void testSubstringChar() {
        Assertions.assertEquals(" World", Strings.substring("Hello World", ' '));
        Assertions.assertEquals("@example.com", Strings.substring("user@example.com", '@'));

        Assertions.assertNull(Strings.substring("Hello", 'x'));

        Assertions.assertNull(Strings.substring("", 'a'));
        Assertions.assertNull(Strings.substring(null, 'a'));
    }

    @Test
    public void testSubstringString() {
        Assertions.assertEquals("World", Strings.substring("Hello World", "Wo"));
        Assertions.assertEquals("@example.com", Strings.substring("user@example.com", "@"));

        Assertions.assertNull(Strings.substring("Hello", "xyz"));

        Assertions.assertEquals("Hello", Strings.substring("Hello", ""));

        Assertions.assertNull(Strings.substring(null, "test"));
        Assertions.assertNull(Strings.substring("test", null));
    }

    @Test
    public void testSubstringWithIndexAndChar() {
        Assertions.assertEquals("Hello", Strings.substring("Hello, World!", 0, ','));
        Assertions.assertEquals("b", Strings.substring("a-b-c-d", 2, '-'));

        Assertions.assertNull(Strings.substring("test", 0, 'x'));

        Assertions.assertNull(Strings.substring("test", -1, 't'));
        Assertions.assertNull(Strings.substring("test", 10, 't'));

        Assertions.assertNull(Strings.substring(null, 0, ','));
    }

    @Test
    public void testSubstringWithIndexAndString() {
        Assertions.assertEquals("Hello", Strings.substring("Hello, World!", 0, ", "));
        Assertions.assertEquals("content", Strings.substring("<tag>content</tag>", 5, "</"));

        Assertions.assertNull(Strings.substring("test", 0, "xyz"));

        Assertions.assertEquals("", Strings.substring("test", 0, ""));

        Assertions.assertNull(Strings.substring(null, 0, "test"));
        Assertions.assertNull(Strings.substring("test", 0, (String) null));
    }

    @Test
    public void testSubstringWithCharAndEndIndex() {
        Assertions.assertEquals("-d", Strings.substring("a-b-c-d", '-', 7));
        Assertions.assertEquals(" world", Strings.substring("hello world", ' ', 11));

        Assertions.assertNull(Strings.substring("test", 'x', 4));

        Assertions.assertNull(Strings.substring("test", 't', -1));

        Assertions.assertNull(Strings.substring(null, 'a', 5));
    }

    @Test
    public void testSubstringWithStringAndEndIndex() {
        Assertions.assertEquals("<c>", Strings.substring("<a><b><c>", "<", 9));
        Assertions.assertEquals("-baz", Strings.substring("foo-bar-baz", "-", 11));

        Assertions.assertNull(Strings.substring("test", "xyz", 4));

        Assertions.assertEquals("", Strings.substring("test", "", 4));

        Assertions.assertNull(Strings.substring(null, "test", 5));
        Assertions.assertNull(Strings.substring("test", (String) null, 5));
    }

    @Test
    public void testJoinByteArray() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[1, 2, 3]", Strings.join(arr, 0, 3, ", ", "[", "]"));
        Assertions.assertEquals("Bytes: 2 | 3 | 4", Strings.join(arr, 1, 4, " | ", "Bytes: ", ""));
        Assertions.assertEquals("(3)", Strings.join(arr, 2, 3, ", ", "(", ")"));

        Assertions.assertEquals("[]", Strings.join(arr, 1, 1, ", ", "[", "]"));

        Assertions.assertEquals("[]", Strings.join((byte[]) null, 0, 0, ", ", "[", "]"));

        Assertions.assertEquals("<12>", Strings.join(arr, 0, 2, "", "<", ">"));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(arr, -1, 3, ", ", "", ""));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(arr, 0, 10, ", ", "", ""));
    }

    @Test
    public void testJoinShortArray() {
        short[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("1, 2, 3, 4, 5", Strings.join(arr, 0, 5, ", ", "", ""));
        Assertions.assertEquals("2-3-4", Strings.join(arr, 1, 4, "-", "", ""));
        Assertions.assertEquals("[1, 2, 3]", Strings.join(arr, 0, 3, ", ", "[", "]"));
        Assertions.assertEquals("Numbers: 1, 2, 3, 4, 5", Strings.join(arr, 0, 5, ", ", "Numbers: ", ""));

        Assertions.assertEquals("", Strings.join((short[]) null, 0, 0, ", ", "", ""));
        Assertions.assertEquals("[]", Strings.join(arr, 2, 2, ", ", "[", "]"));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(arr, -1, 3, ", ", "", ""));
    }

    @Test
    public void testJoinLongArray() {
        long[] arr = { 100L, 200L, 300L, 400L, 500L };
        Assertions.assertEquals("100, 200, 300, 400, 500", Strings.join(arr, 0, 5, ", ", "", ""));
        Assertions.assertEquals("200-300-400", Strings.join(arr, 1, 4, "-", "", ""));
        Assertions.assertEquals("[100, 200, 300]", Strings.join(arr, 0, 3, ", ", "[", "]"));
        Assertions.assertEquals("Values: 100, 200, 300, 400, 500", Strings.join(arr, 0, 5, ", ", "Values: ", ""));
        Assertions.assertEquals("Result: 300 | 400!", Strings.join(arr, 2, 4, " | ", "Result: ", "!"));

        Assertions.assertEquals("[]", Strings.join((long[]) null, 0, 0, ", ", "[", "]"));
        Assertions.assertEquals("[]", Strings.join(arr, 2, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoinEntriesWithPrefixSuffix() {
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>(" name ", 1), new AbstractMap.SimpleEntry<>(" age ", 25));

        Assertions.assertEquals("[ name =1,  age =25]", Strings.joinEntries(entries, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals("[name=1, age=25]", Strings.joinEntries(entries, ", ", "=", "[", "]", true, Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals("Total: Apple costs $1.99 + Banana costs $0.99",
                Strings.joinEntries(Arrays.asList(new AbstractMap.SimpleEntry<>("Apple", 1.99), new AbstractMap.SimpleEntry<>("Banana", 0.99)), " + ",
                        " costs $", "Total: ", "", false, Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals("[]",
                Strings.joinEntries(new ArrayList<Map.Entry<String, Integer>>(), ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    public void testConcat5Strings() {
        Assertions.assertEquals("Hello World!!", Strings.concat("Hello", " ", "World", "!", "!"));
        Assertions.assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));

        Assertions.assertEquals("123", Strings.concat("1", null, "2", null, "3"));
        Assertions.assertEquals("", Strings.concat(null, null, null, null, null));

        Assertions.assertEquals("AC", Strings.concat("A", "", "", "", "C"));
    }

    @Test
    public void testConcat6Strings() {
        Assertions.assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        Assertions.assertEquals("123456", Strings.concat("1", "2", "3", "4", "5", "6"));
        Assertions.assertEquals("Hello!", Strings.concat("H", "e", "l", "l", "o", "!"));

        Assertions.assertEquals("", Strings.concat(null, null, null, null, null, null));
    }

    @Test
    public void testConcat7Strings() {
        Assertions.assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        Assertions.assertEquals("1234567", Strings.concat("1", "2", "3", "4", "5", "6", "7"));
        Assertions.assertEquals("Hello 7", Strings.concat("H", "e", "l", "l", "o", " ", "7"));
    }

    @Test
    public void testConcat8Strings() {
        Assertions.assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        Assertions.assertEquals("12345678", Strings.concat("1", "2", "3", "4", "5", "6", "7", "8"));
        Assertions.assertEquals("Hello 8!", Strings.concat("H", "e", "l", "l", "o", " ", "8", "!"));
    }

    @Test
    public void testConcat9Strings() {
        Assertions.assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        Assertions.assertEquals("123456789", Strings.concat("1", "2", "3", "4", "5", "6", "7", "8", "9"));
        Assertions.assertEquals("Hello 9!!", Strings.concat("H", "e", "l", "l", "o", " ", "9", "!", "!"));
    }

    @Test
    public void testConcatStringArray() {
        Assertions.assertEquals("Hello World", Strings.concat(new String[] { "Hello", " ", "World" }));
        Assertions.assertEquals("ABCD", Strings.concat(new String[] { "A", "B", "C", "D" }));

        Assertions.assertEquals("HelloWorld", Strings.concat(new String[] { "Hello", null, "World" }));
        Assertions.assertEquals("", Strings.concat(new String[] { null, null }));

        Assertions.assertEquals("", Strings.concat(new String[0]));

        Assertions.assertEquals("", Strings.concat((String[]) null));

        Assertions.assertEquals("A", Strings.concat(new String[] { "A" }));
        Assertions.assertEquals("AB", Strings.concat(new String[] { "A", "B" }));
        Assertions.assertEquals("ABC", Strings.concat(new String[] { "A", "B", "C" }));
    }

    @Test
    public void testConcat2Objects() {
        Assertions.assertEquals("Hello123", Strings.concat("Hello", 123));
        Assertions.assertEquals("42 is the answer", Strings.concat(42, " is the answer"));

        Assertions.assertEquals("World", Strings.concat(null, "World"));
        Assertions.assertEquals("Hello", Strings.concat("Hello", null));
        Assertions.assertEquals("", Strings.concat(null, null));

        Assertions.assertEquals("3.14true", Strings.concat(3.14, true));
    }

    @Test
    public void testConcat3Objects() {
        Assertions.assertEquals("Hello World", Strings.concat("Hello", " ", "World"));
        Assertions.assertEquals("123", Strings.concat(1, 2, 3));
        Assertions.assertEquals("Value: 42!", Strings.concat("Value: ", 42, "!"));

        Assertions.assertEquals("", Strings.concat(null, null, null));
    }

    @Test
    public void testConcat4Objects() {
        Assertions.assertEquals("ABCD", Strings.concat("A", "B", "C", "D"));
        Assertions.assertEquals("1+2=3", Strings.concat(1, "+", 2, "=3"));
        Assertions.assertEquals("Result: 10 out of 20", Strings.concat("Result: ", 10, " out of ", 20));

        Assertions.assertEquals("", Strings.concat(null, null, null, null));
    }

    @Test
    public void testConcat5Objects() {
        Assertions.assertEquals("ABCDE", Strings.concat("A", "B", "C", "D", "E"));
        Assertions.assertEquals("1 2 3", Strings.concat(1, " ", 2, " ", 3));
        Assertions.assertEquals("Sum of 1+2=3", Strings.concat("Sum of ", 1, "+", 2, "=3"));

        Assertions.assertEquals("", Strings.concat(null, null, null, null, null));
    }

    @Test
    public void testConcat6Objects() {
        Assertions.assertEquals("ABCDEF", Strings.concat("A", "B", "C", "D", "E", "F"));
        Assertions.assertEquals("123456", Strings.concat(1, 2, 3, 4, 5, 6));
        Assertions.assertEquals("Hello!", Strings.concat("H", "e", "l", "l", "o", "!"));
    }

    @Test
    public void testConcat7Objects() {
        Assertions.assertEquals("ABCDEFG", Strings.concat("A", "B", "C", "D", "E", "F", "G"));
        Assertions.assertEquals("1234567", Strings.concat(1, 2, 3, 4, 5, 6, 7));
        Assertions.assertEquals("Hello 7", Strings.concat("H", "e", "l", "l", "o", " ", 7));
    }

    @Test
    public void testConcat8Objects() {
        Assertions.assertEquals("ABCDEFGH", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H"));
        Assertions.assertEquals("12345678", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8));
        Assertions.assertEquals("Hello 8!", Strings.concat("H", "e", "l", "l", "o", " ", 8, "!"));
    }

    @Test
    public void testConcat9Objects() {
        Assertions.assertEquals("ABCDEFGHI", Strings.concat("A", "B", "C", "D", "E", "F", "G", "H", "I"));
        Assertions.assertEquals("123456789", Strings.concat(1, 2, 3, 4, 5, 6, 7, 8, 9));
        Assertions.assertEquals("Hello 9!!", Strings.concat("H", "e", "l", "l", "o", " ", 9, "!", "!"));
    }

    @Test
    public void testSplitWithSingleCharacterDelimiter() {
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
        Assertions.assertArrayEquals(new String[] { "abc" }, Strings.split("abc", ","));
        Assertions.assertArrayEquals(new String[] {}, Strings.split("", ","));

        Assertions.assertArrayEquals(new String[] { "a", "b" }, Strings.split("a,,b", ","));
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,,,b,,,c", ","));
    }

    @Test
    public void testSplitPreserveAllTokensEdgeCases() {
        Assertions.assertArrayEquals(new String[] { "", "", "", "" }, Strings.splitPreserveAllTokens(":::", ":"));
        Assertions.assertArrayEquals(new String[] { "", "" }, Strings.splitPreserveAllTokens(":", ":"));

        Assertions.assertArrayEquals(new String[] { "", "a", "", "b", "" }, Strings.splitPreserveAllTokens(":a::b:", ":"));
    }

    @Test
    public void testSubstringEdgeCases() {
        Assertions.assertEquals(",test", Strings.substring(",test", ','));

        Assertions.assertEquals(",", Strings.substring("test,", ','));

        Assertions.assertEquals(",test,more", Strings.substring("first,test,more", ','));
    }

    @Test
    public void testJoinEntriesWithNullValues() {
        List<Map.Entry<String, String>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", null), new AbstractMap.SimpleEntry<>(null, "value2"),
                new AbstractMap.SimpleEntry<>("key3", "value3"));

        String result = Strings.joinEntries(entries, ", ", "=", Map.Entry::getKey, Map.Entry::getValue);
        Assertions.assertEquals("key1=null, null=value2, key3=value3", result);
    }

    @Test
    public void testConcatWithLargeArrays() {
        String[] largeArray = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J" };
        Assertions.assertEquals("ABCDEFGHIJ", Strings.concat(largeArray));

        String[] mixedArray = { "A", null, "B", null, "C", "D", "E", "F", "G", "H" };
        Assertions.assertEquals("ABCDEFGH", Strings.concat(mixedArray));
    }

    @Test
    public void testIndexOfIgnoreCaseComplexScenarios() {
        Assertions.assertEquals(7, Strings.indexOfIgnoreCase("apple, APPLE, banana", "apple", ", ", 1));

        Assertions.assertEquals(-1, Strings.indexOfIgnoreCase("test,value", "st,va", ","));

        Assertions.assertEquals(1, Strings.indexOfIgnoreCase(",apple,", "apple", ","));
    }

    @Test
    public void testReplaceOnceEdgeCases() {
        Assertions.assertEquals("xyzcde", Strings.replaceOnce("abcde", "ab", "xyz"));

        Assertions.assertEquals("abcxyz", Strings.replaceOnce("abcde", "de", "xyz"));

        Assertions.assertEquals("xyz", Strings.replaceOnce("abc", "abc", "xyz"));

        Assertions.assertEquals("xc", Strings.replaceOnce("abc", "ab", "x"));
    }

    @Test
    public void testSplitWithWhitespaceDelimiter() {
        Assertions.assertArrayEquals(new String[] { "hello", "world", "test" }, Strings.split("hello   world\ttest", null));
        Assertions.assertArrayEquals(new String[] { "hello", "world" }, Strings.split("  hello   world  ", null));

        Assertions.assertArrayEquals(new String[] { "hello", "world" }, Strings.split("  hello   world  ", null, true));
    }

    @Test
    public void testJoinWithEmptyElements() {
        byte[] arr = { 1, 2, 3 };
        Assertions.assertEquals("1,2,3", Strings.join(arr, 0, 3, ",", "", ""));

        Assertions.assertEquals("Values: 1,2,3", Strings.join(arr, 0, 3, ",", "Values: ", ""));

        Assertions.assertEquals("1,2,3!", Strings.join(arr, 0, 3, ",", "", "!"));
    }

    @Test
    public void testConvertWordsEdgeCases() {
        Assertions.assertEquals("HELLO", Strings.convertWords("hello", " ", null, String::toUpperCase));

        Function<String, String> reverser = s -> new StringBuilder(s).reverse().toString();
        Assertions.assertEquals("olleh dlrow", Strings.convertWords("hello world", " ", null, reverser));

        Set<String> allExcluded = new HashSet<>(Arrays.asList("hello", "world"));
        Assertions.assertEquals("hello world", Strings.convertWords("hello world", " ", allExcluded, String::toUpperCase));
    }

    @Test
    public void testArrayMethodsWithSingleElement() {
        String[] single = { "  test  " };
        Strings.trimToNull(single);
        Assertions.assertEquals("test", single[0]);

        String[] single2 = { "  " };
        Strings.trimToNull(single2);
        Assertions.assertNull(single2[0]);

        String[] single3 = { "xyztest" };
        Strings.stripStart(single3, "xyz");
        Assertions.assertEquals("test", single3[0]);
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
    public void testParseNumericEdgeCases() {
        Assertions.assertThrows(NumberFormatException.class, () -> Strings.parseInt(" 123 "));

        Assertions.assertEquals(Integer.MAX_VALUE, Strings.parseInt(String.valueOf(Integer.MAX_VALUE)));
        Assertions.assertEquals(Integer.MIN_VALUE, Strings.parseInt(String.valueOf(Integer.MIN_VALUE)));

        Assertions.assertEquals('\n', Strings.parseChar("10"));
        Assertions.assertEquals('9', Strings.parseChar("9"));
    }

    @Test
    public void testLastIndexOfAllWithOverlappingPatterns() {
        String text = "ababababab";
        Assertions.assertEquals(8, Strings.maxLastIndexOfAll(text, "ab", "ba"));
        Assertions.assertEquals(7, Strings.minLastIndexOfAll(text, "ab", "ba"));

        Assertions.assertEquals(-1, Strings.maxLastIndexOfAll(text, "xyz", "qrs"));
    }

    @Test
    public void testSplitPreserveAllTokensWithComplexDelimiters() {
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a::b::c", "::"));
        Assertions.assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitPreserveAllTokens("a::::b", "::"));

        Assertions.assertArrayEquals(new String[] { "test" }, Strings.splitPreserveAllTokens("test", "::"));
    }

    @Test
    public void testJoinEntriesWithSingleEntry() {
        List<Map.Entry<String, String>> singleEntry = Arrays.asList(new AbstractMap.SimpleEntry<>("key", "value"));

        Assertions.assertEquals("key=value", Strings.joinEntries(singleEntry, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));

        Assertions.assertEquals("[key=value]", Strings.joinEntries(singleEntry, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    public void testConcatPerformanceOptimization() {
        Assertions.assertEquals("", Strings.concat());

        Assertions.assertEquals("A", Strings.concat("A"));

        for (int i = 2; i <= 7; i++) {
            String[] arr = new String[i];
            Arrays.fill(arr, "X");
            String expected = "X".repeat(i);
            Assertions.assertEquals(expected, Strings.concat(arr));
        }

        String[] arr = new String[10];
        Arrays.fill(arr, "Y");
        Assertions.assertEquals("Y".repeat(10), Strings.concat(arr));
    }

    @Test
    public void testSplitMaxBoundaryConditions() {
        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 3));

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a:b:c", ":", 100));

        Assertions.assertArrayEquals(new String[] { "a", "b:c:d" }, Strings.split("a:b:c:d", ":", 2));
    }

    @Test
    public void testAppendPrependCaseSensitivity() {
        Assertions.assertEquals("File.TXT", Strings.appendIfMissingIgnoreCase("File", ".TXT"));
        Assertions.assertEquals("File.txt", Strings.appendIfMissingIgnoreCase("File.txt", ".TXT"));
        Assertions.assertEquals("File.TxT", Strings.appendIfMissingIgnoreCase("File.TxT", ".txt"));

        Assertions.assertEquals("HTTP://site.com", Strings.prependIfMissingIgnoreCase("site.com", "HTTP://"));
        Assertions.assertEquals("HtTp://site.com", Strings.prependIfMissingIgnoreCase("HtTp://site.com", "http://"));
    }

    @Test
    public void testNullHandlingConsistency() {

        Assertions.assertArrayEquals(new String[0], Strings.split(null, ","));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", true));
        Assertions.assertArrayEquals(new String[0], Strings.split(null, ",", 5, true));

        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ','));
        Assertions.assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ","));

        Strings.trimToNull((String[]) null);
        Strings.trimToEmpty((String[]) null);
        Strings.stripToNull((String[]) null);
        Strings.stripToEmpty((String[]) null);
        Strings.removeWhitespace((String[]) null);

    }

    @Test
    public void testValueOf() {
        assertNull(Strings.valueOf(null));
        assertEquals("", Strings.valueOf(new char[0]));
        assertEquals("hello", Strings.valueOf(new char[] { 'h', 'e', 'l', 'l', 'o' }));
    }

    @Test
    public void testIsAllEmpty() {
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertFalse(Strings.isAllEmpty("", "test"));
        assertTrue(Strings.isAllEmpty());
        assertTrue(Strings.isAllEmpty("", null, ""));
        assertFalse(Strings.isAllEmpty("", null, "test"));
    }

    @Test
    public void testIsAllBlank() {
        assertTrue(Strings.isAllBlank("", ""));
        assertTrue(Strings.isAllBlank(null, " "));
        assertFalse(Strings.isAllBlank("", "test"));
        assertTrue(Strings.isAllBlank());
        assertTrue(Strings.isAllBlank("", " ", "\t"));
        assertFalse(Strings.isAllBlank("", " ", "test"));
    }

    @Test
    public void testIsAnyEmpty() {
        assertTrue(Strings.isAnyEmpty("", "test"));
        assertTrue(Strings.isAnyEmpty(null, "test"));
        assertFalse(Strings.isAnyEmpty("hello", "world"));
        assertFalse(Strings.isAnyEmpty());
        assertTrue(Strings.isAnyEmpty("hello", "", "world"));
    }

    @Test
    public void testIsAnyBlank() {
        assertTrue(Strings.isAnyBlank("", "test"));
        assertTrue(Strings.isAnyBlank(" ", "test"));
        assertFalse(Strings.isAnyBlank("hello", "world"));
        assertFalse(Strings.isAnyBlank());
        assertTrue(Strings.isAnyBlank("hello", " ", "world"));
    }

    @Test
    public void testIsWrappedWith() {
        assertTrue(Strings.isWrappedWith("(test)", "(", ")"));
        assertTrue(Strings.isWrappedWith("\"hello\"", "\""));
        assertFalse(Strings.isWrappedWith("test", "(", ")"));
        assertFalse(Strings.isWrappedWith(null, "(", ")"));
        assertFalse(Strings.isWrappedWith("(test", "(", ")"));
    }

    @Test
    public void testFirstNonEmpty() {
        assertEquals("test", Strings.firstNonEmpty("", "test", "another"));
        assertEquals("first", Strings.firstNonEmpty("first", "second"));
        assertEquals("", Strings.firstNonEmpty("", "", null));
        assertEquals("", Strings.firstNonEmpty());
    }

    @Test
    public void testFirstNonBlank() {
        assertEquals("test", Strings.firstNonBlank("", " ", "test"));
        assertEquals("first", Strings.firstNonBlank("first", "second"));
        assertEquals("", Strings.firstNonBlank("", " ", null));
        assertEquals("", Strings.firstNonBlank());
    }

    @Test
    public void testAbbreviateWithMarker() {
        assertEquals("abc***", Strings.abbreviate("abcdefg", "***", 6));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", "***", 7));
        assertNull(Strings.abbreviate(null, "***", 4));
    }

    @Test
    public void testCenterWithChar() {
        assertEquals("xxabxx", Strings.center("ab", 6, 'x'));
        assertEquals("xabxx", Strings.center("ab", 5, 'x'));
    }

    @Test
    public void testCenterWithString() {
        assertEquals("--ab--", Strings.center("ab", 6, "--"));
        assertEquals("-ab--", Strings.center("ab", 5, "-"));
    }

    @Test
    public void testRepeat() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat("hello", 0));
        assertEquals("hellohello", Strings.repeat("hello", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    public void testRepeatWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("hello,hello", Strings.repeat("hello", 2, ","));
        assertEquals("", Strings.repeat("hello", 0, ","));
    }

    @Test
    public void testRepeatWithPrefixSuffix() {
        assertEquals("[a,a,a]", Strings.repeat("a", 3, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("a", 0, ",", "[", "]"));
    }

    @Test
    public void testToLowerCase() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals("hello", Strings.toLowerCase("HELLO"));
        assertEquals("", Strings.toLowerCase(""));
        assertNull(Strings.toLowerCase(null));
    }

    @Test
    public void testToUpperCase() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals("HELLO", Strings.toUpperCase("hello"));
        assertEquals("", Strings.toUpperCase(""));
        assertNull(Strings.toUpperCase(null));
    }

    @Test
    public void testSwapCase() {
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('a', Strings.swapCase('A'));
        assertEquals("hELLO", Strings.swapCase("Hello"));
    }

    @Test
    public void testIsAsciiPrintable() {
        assertTrue(Strings.isAsciiPrintable('a'));
        assertTrue(Strings.isAsciiPrintable('1'));
        assertTrue(Strings.isAsciiPrintable(' '));
        assertFalse(Strings.isAsciiPrintable('\n'));

        assertTrue(Strings.isAsciiPrintable("hello"));
        assertFalse(Strings.isAsciiPrintable("hello\n"));
        assertFalse(Strings.isAsciiPrintable(null));
    }

    @Test
    public void testIsAsciiAlpha() {
        assertTrue(Strings.isAsciiAlpha('a'));
        assertTrue(Strings.isAsciiAlpha('Z'));
        assertFalse(Strings.isAsciiAlpha('1'));

        assertTrue(Strings.isAsciiAlpha("hello"));
        assertFalse(Strings.isAsciiAlpha("hello1"));
        assertFalse(Strings.isAsciiAlpha(""));
    }

    @Test
    public void testIsAsciiNumeric() {
        assertTrue(Strings.isAsciiNumeric('5'));
        assertFalse(Strings.isAsciiNumeric('a'));

        assertTrue(Strings.isAsciiNumeric("12345"));
        assertFalse(Strings.isAsciiNumeric("123a"));
        assertFalse(Strings.isAsciiNumeric(""));
    }

    @Test
    public void testIsAsciiAlphanumeric() {
        assertTrue(Strings.isAsciiAlphanumeric('a'));
        assertTrue(Strings.isAsciiAlphanumeric('5'));
        assertFalse(Strings.isAsciiAlphanumeric(' '));

        assertTrue(Strings.isAsciiAlphanumeric("hello123"));
        assertFalse(Strings.isAsciiAlphanumeric("hello 123"));
        assertFalse(Strings.isAsciiAlphanumeric(""));
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
    public void testIndexOfAny() {
        assertEquals(1, Strings.indexOfAny("hello", 'e', 'x'));
        assertEquals(-1, Strings.indexOfAny("hello", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny((String) null, Array.of('e')));
        assertEquals(-1, Strings.indexOfAny("", Array.of('e')));

        assertEquals(1, Strings.indexOfAny("hello", "ell", "xyz"));
        assertEquals(-1, Strings.indexOfAny("hello", "xyz", "abc"));
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
    public void testContains() {
        assertTrue(Strings.contains("hello", 'e'));
        assertFalse(Strings.contains("hello", 'x'));
        assertFalse(Strings.contains(null, 'e'));

        assertTrue(Strings.contains("hello", "ell"));
        assertFalse(Strings.contains("hello", "xyz"));
        assertFalse(Strings.contains(null, "ell"));
    }

    @Test
    public void testContainsAll() {
        assertTrue(Strings.containsAll("hello world", 'h', 'e', 'l'));
        assertFalse(Strings.containsAll("hello", 'h', 'x'));
        assertTrue(Strings.containsAll("", new char[0]));

        assertTrue(Strings.containsAll("hello world", "hello", "world"));
        assertFalse(Strings.containsAll("hello", "hello", "xyz"));
    }

    @Test
    public void testContainsAny() {
        assertTrue(Strings.containsAny("hello", 'h', 'x'));
        assertFalse(Strings.containsAny("hello", 'x', 'y'));
        assertFalse(Strings.containsAny("", 'x'));

        assertTrue(Strings.containsAny("hello", "world", "ell"));
        assertFalse(Strings.containsAny("hello", "xyz", "abc"));
    }

    @Test
    public void testContainsNone() {
        assertTrue(Strings.containsNone("hello", 'x', 'y'));
        assertFalse(Strings.containsNone("hello", 'h', 'x'));
        assertTrue(Strings.containsNone("", 'x'));
        assertTrue(Strings.containsNone(null, 'x'));

        assertTrue(Strings.containsNone("hello", "xyz", "abc"));
        assertFalse(Strings.containsNone("hello", "world", "ell"));
    }

    @Test
    @DisplayName("containsOnly should return true for null string")
    public void testContainsOnly_NullString() {
        assertTrue(Strings.containsOnly(null, 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly(null, 'x'));
        assertTrue(Strings.containsOnly(null));
    }

    @Test
    @DisplayName("containsOnly should return true for empty string")
    public void testContainsOnly_EmptyString() {
        assertTrue(Strings.containsOnly("", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("", 'x'));
        assertTrue(Strings.containsOnly("", new char[0]));
    }

    @Test
    @DisplayName("containsOnly should return false when valuesToFind is null or empty and string is not empty")
    public void testContainsOnly_EmptyValuesToFind() {
        assertFalse(Strings.containsOnly("abc", new char[0]));
        assertFalse(Strings.containsOnly("a", new char[0]));
        assertFalse(Strings.containsOnly(" ", new char[0]));

        assertFalse(Strings.containsOnly("abc", (char[]) null));
    }

    @Test
    @DisplayName("containsOnly should return true when string contains only specified characters")
    public void testContainsOnly_StringContainsOnlySpecifiedChars() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abcabc", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("cba", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should return false when string contains characters not in valuesToFind")
    public void testContainsOnly_StringContainsOtherChars() {
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("axb", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("xyz", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("ab c", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("123", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle single character strings")
    public void testContainsOnly_SingleCharacter() {
        assertTrue(Strings.containsOnly("a", 'a'));
        assertTrue(Strings.containsOnly("a", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("x", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("a", 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle special characters")
    public void testContainsOnly_SpecialCharacters() {
        assertTrue(Strings.containsOnly(" ", ' '));
        assertTrue(Strings.containsOnly("\t\n", '\t', '\n'));
        assertTrue(Strings.containsOnly("!@#", '!', '@', '#'));
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));

        assertFalse(Strings.containsOnly("Hello World", 'H', 'e', 'l', 'o'));
        assertTrue(Strings.containsOnly("Hello", 'H', 'e', 'l', 'o'));
    }

    @Test
    @DisplayName("containsOnly should handle unicode characters")
    public void testContainsOnly_UnicodeCharacters() {
        assertTrue(Strings.containsOnly("αβγ", 'α', 'β', 'γ'));
        assertFalse(Strings.containsOnly("αβγδ", 'α', 'β', 'γ'));
    }

    @Test
    @DisplayName("containsOnly should handle repeated characters in valuesToFind")
    public void testContainsOnly_RepeatedValuesToFind() {
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c', 'a', 'b'));
        assertTrue(Strings.containsOnly("aaa", 'a', 'a', 'a'));
        assertFalse(Strings.containsOnly("abcd", 'a', 'b', 'c', 'a', 'b'));
    }

    @Test
    @DisplayName("containsOnly should handle case sensitivity")
    public void testContainsOnly_CaseSensitive() {
        assertFalse(Strings.containsOnly("ABC", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("ABC", 'A', 'B', 'C'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle long strings")
    public void testContainsOnly_LongStrings() {
        String longString = "a".repeat(1000);
        assertTrue(Strings.containsOnly(longString, 'a'));
        assertFalse(Strings.containsOnly(longString + "b", 'a'));

        String mixedLongString = "abcabc".repeat(100);
        assertTrue(Strings.containsOnly(mixedLongString, 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly(mixedLongString + "d", 'a', 'b', 'c'));
    }

    @Test
    @DisplayName("containsOnly should handle whitespace characters")
    public void testContainsOnly_Whitespace() {
        assertTrue(Strings.containsOnly("   ", ' '));
        assertTrue(Strings.containsOnly("\t\t\t", '\t'));
        assertTrue(Strings.containsOnly("\n\n", '\n'));
        assertTrue(Strings.containsOnly(" \t\n", ' ', '\t', '\n'));

        assertFalse(Strings.containsOnly("a b", 'a', 'b'));
        assertTrue(Strings.containsOnly("a b", 'a', 'b', ' '));
    }

    @Test
    @DisplayName("containsOnly should handle numeric strings")
    public void testContainsOnly_Numeric() {
        assertTrue(Strings.containsOnly("123", '1', '2', '3'));
        assertTrue(Strings.containsOnly("000", '0'));
        assertTrue(Strings.containsOnly("1234567890", '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));

        assertFalse(Strings.containsOnly("123a", '1', '2', '3'));
        assertFalse(Strings.containsOnly("12.3", '1', '2', '3'));
    }

    @Test
    @DisplayName("containsOnly should validate input characters for surrogate pairs")
    public void testContainsOnly_SurrogateValidation() {
        char highSurrogate = '\uD800';
        char lowSurrogate = '\uDC00';

        assertThrows(IllegalArgumentException.class, () -> Strings.containsOnly("test", highSurrogate));

        assertThrows(IllegalArgumentException.class, () -> Strings.containsOnly("test", lowSurrogate));

        assertThrows(IllegalArgumentException.class, () -> Strings.containsOnly("test", 'a', highSurrogate, 'b'));
    }

    @Test
    @DisplayName("containsOnly edge cases")
    public void testContainsOnly_EdgeCases() {
        assertTrue(Strings.containsOnly("a", 'a'));

        assertTrue(Strings.containsOnly("aaabbbccc", 'a', 'b', 'c'));

        assertTrue(Strings.containsOnly("ab", 'a', 'b', 'c', 'd', 'e'));

        assertTrue(Strings.containsOnly("aaaa", 'a'));

        assertTrue(Strings.containsOnly("abcd", 'a', 'b', 'c', 'd'));
    }

    @Test
    @DisplayName("should return false for null string")
    public void testContainsWhitespace_NullString() {
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    @DisplayName("should return false for empty string")
    public void testContainsWhitespace_EmptyString() {
        assertFalse(Strings.containsWhitespace(""));
    }

    @Test
    @DisplayName("should return false for string without whitespace")
    public void testContainsWhitespace_NoWhitespace() {
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace("123"));
        assertFalse(Strings.containsWhitespace("!@#$%"));
        assertFalse(Strings.containsWhitespace("αβγ"));
    }

    @Test
    @DisplayName("should return true for string with space")
    public void testContainsWhitespace_WithSpace() {
        assertTrue(Strings.containsWhitespace(" "));
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace(" abc"));
        assertTrue(Strings.containsWhitespace("abc "));
        assertTrue(Strings.containsWhitespace("a b c"));
    }

    @Test
    @DisplayName("should return true for string with various whitespace characters")
    public void testContainsWhitespace_VariousWhitespace() {
        assertTrue(Strings.containsWhitespace("\t"));
        assertTrue(Strings.containsWhitespace("\n"));
        assertTrue(Strings.containsWhitespace("\r"));
        assertTrue(Strings.containsWhitespace("\f"));
        assertTrue(Strings.containsWhitespace("a\tb"));
        assertTrue(Strings.containsWhitespace("line1\nline2"));
        assertTrue(Strings.containsWhitespace("text\rtext"));
    }

    @Test
    @DisplayName("should return false for null string")
    public void testStartsWith_NullString() {
        assertFalse(Strings.startsWith(null, "prefix"));
        assertFalse(Strings.startsWith(null, null));
    }

    @Test
    @DisplayName("should return false for null prefix")
    public void testStartsWith_NullPrefix() {
        assertFalse(Strings.startsWith("test", null));
    }

    @Test
    @DisplayName("should return false when prefix is longer than string")
    public void testStartsWith_PrefixLongerThanString() {
        assertFalse(Strings.startsWith("ab", "abc"));
        assertFalse(Strings.startsWith("", "a"));
    }

    @Test
    @DisplayName("should return true for valid prefix")
    public void testStartsWith_ValidPrefix() {
        assertTrue(Strings.startsWith("hello", "hel"));
        assertTrue(Strings.startsWith("test", "test"));
        assertTrue(Strings.startsWith("abc", ""));
        assertTrue(Strings.startsWith("", ""));
    }

    @Test
    @DisplayName("should be case sensitive")
    public void testStartsWith_CaseSensitive() {
        assertFalse(Strings.startsWith("Hello", "hello"));
        assertFalse(Strings.startsWith("TEST", "test"));
        assertTrue(Strings.startsWith("Hello", "Hello"));
    }

    @Test
    @DisplayName("should return false for null string or prefix")
    public void testStartsWithIgnoreCase_NullInputs() {
        assertFalse(Strings.startsWithIgnoreCase(null, "prefix"));
        assertFalse(Strings.startsWithIgnoreCase("test", null));
        assertFalse(Strings.startsWithIgnoreCase(null, null));
    }

    @Test
    @DisplayName("should ignore case when matching")
    public void testStartsWithIgnoreCase_IgnoreCase() {
        assertTrue(Strings.startsWithIgnoreCase("Hello", "hello"));
        assertTrue(Strings.startsWithIgnoreCase("TEST", "test"));
        assertTrue(Strings.startsWithIgnoreCase("MiXeD", "mixed"));
        assertTrue(Strings.startsWithIgnoreCase("ABC", "abc"));
    }

    @Test
    @DisplayName("should return true for valid prefix regardless of case")
    public void testStartsWithIgnoreCase_ValidPrefix() {
        assertTrue(Strings.startsWithIgnoreCase("hello world", "HELLO"));
        assertTrue(Strings.startsWithIgnoreCase("JavaScript", "java"));
        assertFalse(Strings.startsWithIgnoreCase("hello", "world"));
    }

    @Test
    @DisplayName("should return false for null string")
    public void testStartsWithAny_NullString() {
        assertFalse(Strings.startsWithAny(null, "a", "b"));
    }

    @Test
    @DisplayName("should return false for null or empty prefixes")
    public void testStartsWithAny_NullOrEmptyPrefixes() {
        assertFalse(Strings.startsWithAny("test", (String[]) null));
        assertFalse(Strings.startsWithAny("test"));
    }

    @Test
    @DisplayName("should return true if string starts with any prefix")
    public void testStartsWithAny_MatchingPrefix() {
        assertTrue(Strings.startsWithAny("hello", "hel", "test", "world"));
        assertTrue(Strings.startsWithAny("test", "no", "te", "maybe"));
        assertTrue(Strings.startsWithAny("abc", "xyz", "abc", "def"));
    }

    @Test
    @DisplayName("should return false if string starts with none of the prefixes")
    public void testStartsWithAny_NoMatchingPrefix() {
        assertFalse(Strings.startsWithAny("hello", "world", "test", "abc"));
        assertFalse(Strings.startsWithAny("xyz", "a", "b", "c"));
    }

    @Test
    @DisplayName("should return false for null string")
    public void testStartsWithAnyIgnoreCase_NullString() {
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "a", "b"));
    }

    @Test
    @DisplayName("should ignore case when matching any prefix")
    public void testStartsWithAnyIgnoreCase_IgnoreCase() {
        assertTrue(Strings.startsWithAnyIgnoreCase("Hello", "HELLO", "test"));
        assertTrue(Strings.startsWithAnyIgnoreCase("TEST", "xyz", "test"));
        assertTrue(Strings.startsWithAnyIgnoreCase("MiXeD", "abc", "MIXED"));
    }

    @Test
    @DisplayName("should return false if no prefix matches")
    public void testStartsWithAnyIgnoreCase_NoMatch() {
        assertFalse(Strings.startsWithAnyIgnoreCase("hello", "WORLD", "TEST"));
        assertFalse(Strings.startsWithAnyIgnoreCase("abc", "XYZ", "DEF"));
    }

    @Test
    @DisplayName("should return false for null string")
    public void testEndsWith_NullString() {
        assertFalse(Strings.endsWith(null, "suffix"));
        assertFalse(Strings.endsWith(null, null));
    }

    @Test
    @DisplayName("should return false for null suffix")
    public void testEndsWith_NullSuffix() {
        assertFalse(Strings.endsWith("test", null));
    }

    @Test
    @DisplayName("should return false when suffix is longer than string")
    public void testEndsWith_SuffixLongerThanString() {
        assertFalse(Strings.endsWith("ab", "abc"));
        assertFalse(Strings.endsWith("", "a"));
    }

    @Test
    @DisplayName("should return true for valid suffix")
    public void testEndsWith_ValidSuffix() {
        assertTrue(Strings.endsWith("hello", "llo"));
        assertTrue(Strings.endsWith("test", "test"));
        assertTrue(Strings.endsWith("abc", ""));
        assertTrue(Strings.endsWith("", ""));
    }

    @Test
    @DisplayName("should be case sensitive")
    public void testEndsWith_CaseSensitive() {
        assertFalse(Strings.endsWith("Hello", "LLO"));
        assertFalse(Strings.endsWith("TEST", "test"));
        assertTrue(Strings.endsWith("Hello", "llo"));
    }

    @Test
    @DisplayName("should return false for null inputs")
    public void testEndsWithIgnoreCase_NullInputs() {
        assertFalse(Strings.endsWithIgnoreCase(null, "suffix"));
        assertFalse(Strings.endsWithIgnoreCase("test", null));
        assertFalse(Strings.endsWithIgnoreCase(null, null));
    }

    @Test
    @DisplayName("should ignore case when matching")
    public void testEndsWithIgnoreCase_IgnoreCase() {
        assertTrue(Strings.endsWithIgnoreCase("Hello", "LLO"));
        assertTrue(Strings.endsWithIgnoreCase("TEST", "test"));
        assertTrue(Strings.endsWithIgnoreCase("MiXeD", "XED"));
        assertTrue(Strings.endsWithIgnoreCase("abc", "ABC"));
    }

    @Test
    @DisplayName("should return false for null string")
    public void testEndsWithAny_NullString() {
        assertFalse(Strings.endsWithAny(null, "a", "b"));
    }

    @Test
    @DisplayName("should return true if string ends with any suffix")
    public void testEndsWithAny_MatchingSuffix() {
        assertTrue(Strings.endsWithAny("hello", "llo", "test", "world"));
        assertTrue(Strings.endsWithAny("test", "no", "st", "maybe"));
        assertTrue(Strings.endsWithAny("abc", "xyz", "bc", "def"));
    }

    @Test
    @DisplayName("should return false if string ends with none of the suffixes")
    public void testEndsWithAny_NoMatchingSuffix() {
        assertFalse(Strings.endsWithAny("hello", "world", "test", "abc"));
        assertFalse(Strings.endsWithAny("xyz", "a", "b", "c"));
    }

    @Test
    @DisplayName("should return false for null string")
    public void testEndsWithAnyIgnoreCase_NullString() {
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "a", "b"));
    }

    @Test
    @DisplayName("should ignore case when matching any suffix")
    public void testEndsWithAnyIgnoreCase_IgnoreCase() {
        assertTrue(Strings.endsWithAnyIgnoreCase("Hello", "LLO", "test"));
        assertTrue(Strings.endsWithAnyIgnoreCase("TEST", "xyz", "test"));
        assertTrue(Strings.endsWithAnyIgnoreCase("MiXeD", "abc", "XED"));
    }

    @Test
    @DisplayName("should return true for both null strings")
    public void testEquals_BothNull() {
        assertTrue(Strings.equals(null, null));
    }

    @Test
    @DisplayName("should return false when one string is null")
    public void testEquals_OneNull() {
        assertFalse(Strings.equals(null, "test"));
        assertFalse(Strings.equals("test", null));
    }

    @Test
    @DisplayName("should return true for equal strings")
    public void testEquals_EqualStrings() {
        assertTrue(Strings.equals("", ""));
        assertTrue(Strings.equals("test", "test"));
        assertTrue(Strings.equals("Hello World", "Hello World"));
    }

    @Test
    @DisplayName("should return false for different strings")
    public void testEquals_DifferentStrings() {
        assertFalse(Strings.equals("test", "TEST"));
        assertFalse(Strings.equals("hello", "world"));
        assertFalse(Strings.equals("abc", "def"));
    }

    @Test
    @DisplayName("should be case sensitive")
    public void testEquals_CaseSensitive() {
        assertFalse(Strings.equals("Hello", "hello"));
        assertFalse(Strings.equals("TEST", "test"));
    }

    @Test
    @DisplayName("should return true for both null strings")
    public void testEqualsIgnoreCase_BothNull() {
        assertTrue(Strings.equalsIgnoreCase(null, null));
    }

    @Test
    @DisplayName("should return false when one string is null")
    public void testEqualsIgnoreCase_OneNull() {
        assertFalse(Strings.equalsIgnoreCase(null, "test"));
        assertFalse(Strings.equalsIgnoreCase("test", null));
    }

    @Test
    @DisplayName("should ignore case when comparing")
    public void testEqualsIgnoreCase_IgnoreCase() {
        assertTrue(Strings.equalsIgnoreCase("test", "TEST"));
        assertTrue(Strings.equalsIgnoreCase("Hello", "hello"));
        assertTrue(Strings.equalsIgnoreCase("MiXeD", "mixed"));
    }

    @Test
    @DisplayName("should return false for different strings")
    public void testEqualsIgnoreCase_DifferentStrings() {
        assertFalse(Strings.equalsIgnoreCase("hello", "world"));
        assertFalse(Strings.equalsIgnoreCase("abc", "def"));
    }

    @Test
    @DisplayName("should return false for empty search strings")
    public void testEqualsAny_EmptySearchStrings() {
        assertFalse(Strings.equalsAny("test"));
        assertFalse(Strings.equalsAny("test", (String[]) null));
    }

    @Test
    @DisplayName("should return true if string equals any search string")
    public void testEqualsAny_MatchingString() {
        assertTrue(Strings.equalsAny("test", "hello", "test", "world"));
        assertTrue(Strings.equalsAny("abc", "abc"));
        assertTrue(Strings.equalsAny(null, "hello", null, "world"));
    }

    @Test
    @DisplayName("should return false if string equals none of the search strings")
    public void testEqualsAny_NoMatchingString() {
        assertFalse(Strings.equalsAny("test", "hello", "world", "abc"));
        assertFalse(Strings.equalsAny("xyz", "a", "b", "c"));
    }

    @Test
    @DisplayName("should be case sensitive")
    public void testEqualsAny_CaseSensitive() {
        assertFalse(Strings.equalsAny("test", "TEST", "Hello"));
        assertTrue(Strings.equalsAny("test", "TEST", "test"));
    }

    @Test
    @DisplayName("should return false for empty search strings")
    public void testEqualsAnyIgnoreCase_EmptySearchStrings() {
        assertFalse(Strings.equalsAnyIgnoreCase("test"));
        assertFalse(Strings.equalsAnyIgnoreCase("test", (String[]) null));
    }

    @Test
    @DisplayName("should ignore case when comparing")
    public void testEqualsAnyIgnoreCase_IgnoreCase() {
        assertTrue(Strings.equalsAnyIgnoreCase("test", "HELLO", "TEST", "world"));
        assertTrue(Strings.equalsAnyIgnoreCase("ABC", "xyz", "abc"));
        assertTrue(Strings.equalsAnyIgnoreCase("Hello", "HELLO", "world"));
    }

    @Test
    @DisplayName("should return false if string equals none of the search strings")
    public void testEqualsAnyIgnoreCase_NoMatch() {
        assertFalse(Strings.equalsAnyIgnoreCase("test", "HELLO", "WORLD", "ABC"));
        assertFalse(Strings.equalsAnyIgnoreCase("xyz", "A", "B", "C"));
    }

    @Test
    @DisplayName("should handle single and multiple search strings")
    public void testEqualsAnyIgnoreCase_SingleAndMultiple() {
        assertTrue(Strings.equalsAnyIgnoreCase("test", "TEST"));
        assertTrue(Strings.equalsAnyIgnoreCase("test", "hello", "TEST"));
    }

    @Test
    @DisplayName("should return 0 for both null strings")
    public void testCompareIgnoreCase_BothNull() {
        assertEquals(0, Strings.compareIgnoreCase(null, null));
    }

    @Test
    @DisplayName("should return negative when first string is null")
    public void testCompareIgnoreCase_FirstNull() {
        assertTrue(Strings.compareIgnoreCase(null, "test") < 0);
    }

    @Test
    @DisplayName("should return positive when second string is null")
    public void testCompareIgnoreCase_SecondNull() {
        assertTrue(Strings.compareIgnoreCase("test", null) > 0);
    }

    @Test
    @DisplayName("should return 0 for equal strings ignoring case")
    public void testCompareIgnoreCase_EqualStringsIgnoreCase() {
        assertEquals(0, Strings.compareIgnoreCase("test", "TEST"));
        assertEquals(0, Strings.compareIgnoreCase("Hello", "hello"));
        assertEquals(0, Strings.compareIgnoreCase("MiXeD", "mixed"));
    }

    @Test
    @DisplayName("should return negative for lexicographically smaller first string")
    public void testCompareIgnoreCase_FirstSmaller() {
        assertTrue(Strings.compareIgnoreCase("apple", "BANANA") < 0);
        assertTrue(Strings.compareIgnoreCase("a", "B") < 0);
    }

    @Test
    @DisplayName("should return positive for lexicographically larger first string")
    public void testCompareIgnoreCase_FirstLarger() {
        assertTrue(Strings.compareIgnoreCase("BANANA", "apple") > 0);
        assertTrue(Strings.compareIgnoreCase("B", "a") > 0);
    }

    @Test
    @DisplayName("should handle empty strings")
    public void testCompareIgnoreCase_EmptyStrings() {
        assertEquals(0, Strings.compareIgnoreCase("", ""));
        assertTrue(Strings.compareIgnoreCase("", "test") < 0);
        assertTrue(Strings.compareIgnoreCase("test", "") > 0);
    }

    @Test
    @DisplayName("should handle strings of different lengths")
    public void testCompareIgnoreCase_DifferentLengths() {
        assertTrue(Strings.compareIgnoreCase("abc", "ABCD") < 0);
        assertTrue(Strings.compareIgnoreCase("ABCD", "abc") > 0);
    }

    @Test
    public void testReverseDelimitedChar() {
        assertNull(Strings.reverseDelimited(null, '.'));

        assertEquals("", Strings.reverseDelimited("", '.'));

        assertEquals("a", Strings.reverseDelimited("a", '.'));

        assertEquals("abc", Strings.reverseDelimited("abc", '.'));

        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", '.'));

        assertEquals("d.c.b.a", Strings.reverseDelimited("a.b.c.d", '.'));

        assertEquals("String.lang.java", Strings.reverseDelimited("java.lang.String", '.'));
    }

    @Test
    public void testReverseDelimitedString() {
        assertNull(Strings.reverseDelimited(null, "."));

        assertEquals("", Strings.reverseDelimited("", "."));

        assertEquals("a", Strings.reverseDelimited("a", "."));

        assertEquals("abc", Strings.reverseDelimited("abc", "."));

        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));

        assertEquals("c::b::a", Strings.reverseDelimited("a::b::c", "::"));

        assertEquals("world|hello", Strings.reverseDelimited("hello|world", "|"));
    }

    @Test
    public void testFormatToPercentageWithScale() {
        assertEquals("50%", Numbers.format(0.5, "0%"));

        assertEquals("33.3%", Numbers.format(1.0 / 3.0, "0.0%"));

        assertEquals("33.333%", Numbers.format(1.0 / 3.0, "0.000%"));

        assertEquals("12.3457%", Numbers.format(0.123457, "0.0000%"));

        assertEquals("66.67%", Numbers.format(2.0 / 3.0, "0.00%"));
    }

    @Test
    @DisplayName("Test base64EncodeString with charset")
    public void testBase64EncodeStringWithCharset() {
        assertEquals("SGVsbG8=", Strings.base64EncodeString("Hello", StandardCharsets.UTF_8));
        assertEquals("SGVsbG8=", Strings.base64EncodeString("Hello", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64EncodeString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64EncodeString(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test base64DecodeToString with charset")
    public void testBase64DecodeToStringWithCharset() {
        assertEquals("Hello", Strings.base64DecodeToString("SGVsbG8=", StandardCharsets.UTF_8));
        assertEquals("Hello", Strings.base64DecodeToString("SGVsbG8=", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64DecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64DecodeToString(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToString with charset")
    public void testBase64UrlDecodeToStringWithCharset() {
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8", StandardCharsets.UTF_8));
        assertEquals("Hello", Strings.base64UrlDecodeToString("SGVsbG8", StandardCharsets.US_ASCII));
        assertEquals("", Strings.base64UrlDecodeToString("", StandardCharsets.UTF_8));
        assertEquals("", Strings.base64UrlDecodeToString(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test isBase64 byte method")
    public void testIsBase64Byte() {
        assertTrue(Strings.isBase64((byte) 'A'));
        assertTrue(Strings.isBase64((byte) 'Z'));
        assertTrue(Strings.isBase64((byte) 'a'));
        assertTrue(Strings.isBase64((byte) 'z'));
        assertTrue(Strings.isBase64((byte) '0'));
        assertTrue(Strings.isBase64((byte) '9'));
        assertTrue(Strings.isBase64((byte) '+'));
        assertTrue(Strings.isBase64((byte) '/'));
        assertTrue(Strings.isBase64((byte) '='));

        assertFalse(Strings.isBase64((byte) '@'));
        assertFalse(Strings.isBase64((byte) '['));
        assertFalse(Strings.isBase64((byte) '`'));
        assertFalse(Strings.isBase64((byte) '{'));
        assertFalse(Strings.isBase64((byte) '!'));
    }

    @Test
    @DisplayName("Test isBase64 byte array method")
    public void testIsBase64ByteArray() {
        assertTrue(Strings.isBase64("SGVsbG8=".getBytes()));
        assertTrue(Strings.isBase64("V29ybGQ=".getBytes()));
        assertTrue(Strings.isBase64(new byte[0]));

        assertFalse(Strings.isBase64("Hello@World".getBytes()));

        assertTrue(Strings.isBase64("SGVs bG8=".getBytes()));
    }

    @Test
    @DisplayName("Test isBase64 string method")
    public void testIsBase64String() {
        assertTrue(Strings.isBase64("SGVsbG8="));
        assertTrue(Strings.isBase64("V29ybGQ="));
        assertTrue(Strings.isBase64(""));

        assertFalse(Strings.isBase64("Hello@World"));

        assertTrue(Strings.isBase64("SGVs bG8="));
    }

    @Test
    @DisplayName("Test formatToPercentage methods")
    public void testFormatToPercentage() {
        assertEquals("50.0%", Numbers.format(0.5, "0.0%"));
        assertEquals("100.0%", Numbers.format(1.0, "0.0%"));
        assertEquals("0.0%", Numbers.format(0.0, "0.0%"));
        assertEquals("25.5%", Numbers.format(0.255, "0.0%"));

        assertEquals("50%", Numbers.format(0.5, "0%"));
        assertEquals("50.0%", Numbers.format(0.5, "0.0%"));
        assertEquals("51.6%", Numbers.format(0.5156, "0.0%"));
        assertEquals("50.00%", Numbers.format(0.5, "0.00%"));
        assertEquals("33.333%", Numbers.format(1.0 / 3.0, "0.000%"));

        assertEquals("-25.0%", Numbers.format(-0.25, "0.0%"));
        assertEquals("-25%", Numbers.format(-0.25, "0%"));
    }

    @Test
    @DisplayName("Test extractFirstSciNumber method")
    public void testExtractFirstSciNumber() {
        assertEquals("1.23e10", Strings.extractFirstDouble("value1.23e10test", true));
        assertEquals("4.56E-5", Strings.extractFirstDouble("test4.56E-5", true));
        assertEquals("1e3", Strings.extractFirstDouble("1e3test", true));

        assertEquals("-1.23e10", Strings.extractFirstDouble("value-1.23e10test", true));

        assertNull(Strings.extractFirstDouble("no sci numbers here", true));
        assertNull(Strings.extractFirstDouble("", true));
        assertNull(Strings.extractFirstDouble(null, true));
        assertEquals("123.45", Strings.extractFirstDouble("123.45", true));

        assertEquals("1.23e10", Strings.extractFirstDouble("first1.23e10second4.56e-5", true));
    }

    @Test
    @DisplayName("Test replaceFirstSciNumber method")
    public void testReplaceFirstSciNumber() {
        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value1.23e10test", "XXX", true));
        assertEquals("testXXX", Strings.replaceFirstDouble("test4.56E-5", "XXX", true));
        assertEquals("XXXtest", Strings.replaceFirstDouble("1e3test", "XXX", true));

        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value-1.23e10test", "XXX", true));

        assertEquals("no sci numbers here", Strings.replaceFirstDouble("no sci numbers here", "XXX", true));
        assertEquals("", Strings.replaceFirstDouble("", "XXX", true));
        assertEquals("", Strings.replaceFirstDouble(null, "XXX", true));
        assertEquals("XXX", Strings.replaceFirstDouble("123.45", "XXX", true));

        assertEquals("firstXXXsecond4.56e-5", Strings.replaceFirstDouble("first1.23e10second4.56e-5", "XXX", true));
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
    public void testSubstring_WithFunctionOfInclusiveBeginIndex() {
        assertEquals("lo", substring("Hello", i -> i - 2, 5));
        assertEquals("", substring("Hello", i -> i, 3));
        assertNull(substring("Hello", i -> -1, 5));
        assertNull(substring(null, i -> 0, 5));
        assertNull(substring("Hello", i -> 0, -1));
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
    public void testSubstringBetween_CharDelimiterAndIndex() {
        assertEquals("llo", substringBetween("Hello World", 'e', 5));
        assertNull(substringBetween("Hello", 'x', 5));
        assertNull(substringBetween("Hello", 'H', 0));
        assertNull(substringBetween(null, 'e', 5));
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
    public void testSubstringBetween_CharDelimiters() {
        assertEquals("b", substringBetween("abc", 'a', 'c'));
        assertEquals("a", substringBetween("aac", 'a', 'c'));
        assertNull(substringBetween("abc", 'x', 'c'));
        assertNull(substringBetween("abc", 'a', 'x'));
        assertNull(substringBetween(null, 'a', 'c'));
        assertNull(substringBetween("", 'a', 'c'));
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

    @Test
    public void testSubstringBetweenIgnoreCase() {
        assertEquals("B", substringBetweenIgnoreCase("aBc", "A", "C"));
        assertEquals("tagged", substringBetweenIgnoreCase("<TAG>tagged</TAG>", "<tag>", "</tag>"));
        assertEquals("ABC", substringBetweenIgnoreCase("xABCx", "X"));
        assertNull(substringBetweenIgnoreCase("abc", "x", "c"));
        assertNull(substringBetweenIgnoreCase(null, "a", "c"));
    }

    @Test
    public void testSubstringsBetween_Chars() {
        List<String> result = substringsBetween("3[a2[c]]2[a]", '[', ']');

        assertEquals(2, result.size());
        assertEquals("a2[c", result.get(0));
        assertEquals("a", result.get(1));

        assertTrue(substringsBetween("abc", '[', ']').isEmpty());
        assertTrue(substringsBetween(null, '[', ']').isEmpty());
        assertTrue(substringsBetween("", '[', ']').isEmpty());
    }

    @Test
    public void testSubstringsBetween_CharsWithRange() {
        List<String> result = substringsBetween("3[a2[c]]2[a]", 0, 8, '[', ']');

        assertEquals("a2[c", substringBetween("3[a2[c]]2[a]", '[', ']'));

        assertEquals(1, result.size());
        assertEquals("a2[c", result.get(0));

        assertTrue(substringsBetween("abc", 0, 3, '[', ']').isEmpty());
    }

    @Test
    public void testSubstringsBetween_Strings() {
        List<String> result = substringsBetween("<tag>text1</tag><tag>text2</tag>", "<tag>", "</tag>");

        assertEquals(2, result.size());
        assertEquals("text1", result.get(0));
        assertEquals("text2", result.get(1));

        assertTrue(substringsBetween("abc", "<", ">").isEmpty());
        assertTrue(substringsBetween(null, "<", ">").isEmpty());
        assertTrue(substringsBetween("test", null, ">").isEmpty());
        assertTrue(substringsBetween("test", "<", null).isEmpty());
    }

    @Test
    public void testSubstringsBetween_StringsWithRange() {
        List<String> result = substringsBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>");

        assertEquals(1, result.size());
        assertEquals("text1", result.get(0));

        assertTrue(substringsBetween("abc", 0, 3, "<", ">").isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_Chars() {
        List<int[]> indices = substringIndicesBetween("3[a2[c]]2[a]", '[', ']');
        assertEquals(2, indices.size());
        assertArrayEquals(new int[] { 2, 6 }, indices.get(0));
        assertArrayEquals(new int[] { 10, 11 }, indices.get(1));

        assertTrue(substringIndicesBetween("abc", '[', ']').isEmpty());
        assertTrue(substringIndicesBetween(null, '[', ']').isEmpty());
        assertTrue(substringIndicesBetween("", '[', ']').isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_CharsWithRange() {
        List<int[]> indices = substringIndicesBetween("3[a2[c]]2[a]", 0, 8, '[', ']');

        assertEquals(1, indices.size());
        assertArrayEquals(new int[] { 2, 6 }, indices.get(0));
    }

    @Test
    public void testSubstringIndicesBetween_Strings() {
        List<int[]> indices = substringIndicesBetween("<tag>text1</tag>", "<tag>", "</tag>");

        assertEquals(1, indices.size());
        assertArrayEquals(new int[] { 5, 10 }, indices.get(0));

        assertTrue(substringIndicesBetween("abc", "<", ">").isEmpty());
        assertTrue(substringIndicesBetween(null, "<", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "<", "").isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_StringsWithRange() {
        List<int[]> indices = substringIndicesBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>");

        assertEquals("text1", substringsBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>").get(0));
        assertEquals(1, indices.size());
        assertArrayEquals(new int[] { 5, 10 }, indices.get(0));

    }

    @Test
    public void testJoin_BooleanArray() {
        assertEquals("true, false, true", join(new boolean[] { true, false, true }));
        assertEquals("true:false:true", join(new boolean[] { true, false, true }, ":"));
        assertEquals("true|false|true", join(new boolean[] { true, false, true }, "|"));
        assertEquals("", join(new boolean[] {}));
        assertEquals("", join((boolean[]) null));
        assertEquals("true", join(new boolean[] { true }));
    }

    @Test
    public void testJoin_BooleanArrayWithRange() {
        assertEquals("false,true", join(new boolean[] { true, false, true }, 1, 3, ","));
        assertEquals("false|true", join(new boolean[] { true, false, true }, 1, 3, "|"));
        assertEquals("", join(new boolean[] { true, false, true }, 1, 1, ","));

        try {
            join(new boolean[] { true, false }, 0, 3, ",");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testJoin_BooleanArrayWithPrefixSuffix() {
        assertEquals("[true, false]", join(new boolean[] { true, false }, 0, 2, ", ", "[", "]"));
        assertEquals("true", join(new boolean[] { true }, 0, 1, ", ", "", ""));
        assertEquals("[]", join(new boolean[] {}, 0, 0, ", ", "[", "]"));
        assertEquals("prefix", join(new boolean[] {}, 0, 0, ", ", "prefix", null));
        assertEquals("suffix", join(new boolean[] {}, 0, 0, ", ", null, "suffix"));
    }

    @Test
    public void testJoin_CharArray() {
        assertEquals("a, b, c", join(new char[] { 'a', 'b', 'c' }));
        assertEquals("a:b:c", join(new char[] { 'a', 'b', 'c' }, ":"));
        assertEquals("abc", join(new char[] { 'a', 'b', 'c' }, ""));
        assertEquals("", join(new char[] {}));
        assertEquals("", join((char[]) null));
        assertEquals("x", join(new char[] { 'x' }));
    }

    @Test
    public void testJoin_CharArrayWithRange() {
        assertEquals("b,c", join(new char[] { 'a', 'b', 'c' }, 1, 3, ","));
        assertEquals("b|c", join(new char[] { 'a', 'b', 'c' }, 1, 3, "|"));
        assertEquals("", join(new char[] { 'a', 'b', 'c' }, 1, 1, ","));
    }

    @Test
    public void testJoin_CharArrayWithPrefixSuffix() {
        assertEquals("[a, b]", join(new char[] { 'a', 'b' }, 0, 2, ", ", "[", "]"));
        assertEquals("<a-b-c>", join(new char[] { 'a', 'b', 'c' }, 0, 3, "-", "<", ">"));
    }

    @Test
    public void testJoin_ByteArray() {
        assertEquals("1, 2, 3", join(new byte[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new byte[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new byte[] { 1, 2, 3 }, ""));
        assertEquals("", join(new byte[] {}));
        assertEquals("", join((byte[]) null));
    }

    @Test
    public void testJoin_ByteArrayWithRange() {
        assertEquals("2,3", join(new byte[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("2|3", join(new byte[] { 1, 2, 3 }, 1, 3, "|"));
        assertEquals("", join(new byte[] { 1, 2, 3 }, 1, 1, ","));
    }

    @Test
    public void testJoin_ByteArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new byte[] { 1, 2 }, 0, 2, ", ", "[", "]"));
        assertEquals("{1-2-3}", join(new byte[] { 1, 2, 3 }, 0, 3, "-", "{", "}"));
    }

    @Test
    public void testJoin_ShortArray() {
        assertEquals("1, 2, 3", join(new short[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new short[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new short[] { 1, 2, 3 }, ""));
        assertEquals("", join(new short[] {}));
        assertEquals("", join((short[]) null));
    }

    @Test
    public void testJoin_ShortArrayWithRange() {
        assertEquals("2,3", join(new short[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("2|3", join(new short[] { 1, 2, 3 }, 1, 3, "|"));
    }

    @Test
    public void testJoin_ShortArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new short[] { 1, 2 }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_IntArray() {
        assertEquals("1, 2, 3", join(new int[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new int[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new int[] { 1, 2, 3 }, ""));
        assertEquals("", join(new int[] {}));
        assertEquals("", join((int[]) null));
    }

    @Test
    public void testJoin_IntArrayWithRange() {
        assertEquals("2,3", join(new int[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("2|3", join(new int[] { 1, 2, 3 }, 1, 3, "|"));
    }

    @Test
    public void testJoin_IntArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new int[] { 1, 2 }, 0, 2, ", ", "[", "]"));
        assertEquals("START:100:200:END", join(new int[] { 100, 200 }, 0, 2, ":", "START:", ":END"));
    }

    @Test
    public void testJoin_LongArray() {
        assertEquals("1, 2, 3", join(new long[] { 1L, 2L, 3L }));
        assertEquals("1:2:3", join(new long[] { 1L, 2L, 3L }, ":"));
        assertEquals("123", join(new long[] { 1L, 2L, 3L }, ""));
        assertEquals("", join(new long[] {}));
        assertEquals("", join((long[]) null));
    }

    @Test
    public void testJoin_LongArrayWithRange() {
        assertEquals("2,3", join(new long[] { 1L, 2L, 3L }, 1, 3, ","));
        assertEquals("2|3", join(new long[] { 1L, 2L, 3L }, 1, 3, "|"));
    }

    @Test
    public void testJoin_LongArrayWithPrefixSuffix() {
        assertEquals("[1, 2]", join(new long[] { 1L, 2L }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_FloatArray() {
        assertEquals("1.0, 2.0, 3.0", join(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals("1.0:2.0:3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, ":"));
        assertEquals("1.02.03.0", join(new float[] { 1.0f, 2.0f, 3.0f }, ""));
        assertEquals("", join(new float[] {}));
        assertEquals("", join((float[]) null));
    }

    @Test
    public void testJoin_FloatArrayWithRange() {
        assertEquals("2.0,3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, 1, 3, ","));
        assertEquals("2.0|3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, 1, 3, "|"));
    }

    @Test
    public void testJoin_FloatArrayWithPrefixSuffix() {
        assertEquals("[1.0, 2.0]", join(new float[] { 1.0f, 2.0f }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_DoubleArray() {
        assertEquals("1.0, 2.0, 3.0", join(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals("1.0:2.0:3.0", join(new double[] { 1.0, 2.0, 3.0 }, ":"));
        assertEquals("1.02.03.0", join(new double[] { 1.0, 2.0, 3.0 }, ""));
        assertEquals("", join(new double[] {}));
        assertEquals("", join((double[]) null));
    }

    @Test
    public void testJoin_DoubleArrayWithRange() {
        assertEquals("2.0,3.0", join(new double[] { 1.0, 2.0, 3.0 }, 1, 3, ","));
        assertEquals("2.0|3.0", join(new double[] { 1.0, 2.0, 3.0 }, 1, 3, "|"));
    }

    @Test
    public void testJoin_DoubleArrayWithPrefixSuffix() {
        assertEquals("[1.0, 2.0]", join(new double[] { 1.0, 2.0 }, 0, 2, ", ", "[", "]"));
    }

    @Test
    public void testJoin_ObjectArray() {
        assertEquals("a, b, c", join(new Object[] { "a", "b", "c" }));
        assertEquals("a:b:c", join(new Object[] { "a", "b", "c" }, ":"));
        assertEquals("abc", join(new Object[] { "a", "b", "c" }, ""));
        assertEquals("1, null, 3", join(new Object[] { 1, null, 3 }));
        assertEquals("", join(new Object[] {}));
        assertEquals("", join((Object[]) null));
    }

    @Test
    public void testJoin_ObjectArrayWithRange() {
        assertEquals("b,c", join(new Object[] { "a", "b", "c" }, 1, 3, ","));
        assertEquals("b|c", join(new Object[] { "a", "b", "c" }, 1, 3, "|"));
        assertEquals("", join(new Object[] { "a", "b", "c" }, 1, 1, ","));
    }

    @Test
    public void testJoin_ObjectArrayWithPrefixSuffix() {
        assertEquals("[a, b]", join(new Object[] { "a", "b" }, 0, 2, ", ", "[", "]"));
        assertEquals("{1-null-3}", join(new Object[] { 1, null, 3 }, 0, 3, "-", "{", "}"));
    }

    @Test
    public void testJoin_ObjectArrayWithTrim() {
        assertEquals("a, b, c", join(new Object[] { " a ", " b ", " c " }, ", ", "", "", true));
        assertEquals("b:c", join(new Object[] { " a ", " b ", " c " }, 1, 3, ":", true));
        assertEquals("b|c", join(new Object[] { " a ", " b ", " c " }, 1, 3, "|", true));
        assertEquals("[a, b]", join(new Object[] { " a ", " b " }, 0, 2, ", ", "[", "]", true));
    }

    @Test
    public void testJoin_IterableWithPrefixSuffix() {
        assertEquals("[a, b]", join(Arrays.asList("a", "b"), ", ", "[", "]"));
        assertEquals("{1-2-3}", join(Arrays.asList(1, 2, 3), "-", "{", "}"));
    }

    @Test
    public void testJoin_IterableWithTrim() {
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c "), ", ", "", "", true));
        assertEquals("[a, b]", join(Arrays.asList(" a ", " b "), ", ", "[", "]", true));
    }

    @Test
    public void testJoin_CollectionWithRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b,c", join(list, 1, 3, ","));
        assertEquals("b|c", join(list, 1, 3, "|"));
        assertEquals("b:c", join(list, 1, 3, ":"));
        assertEquals("", join(list, 2, 2, ","));

        try {
            join(list, 0, 5, ",");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testJoin_CollectionWithPrefixSuffix() {
        List<String> list = Arrays.asList("a", "b", "c");

        assertEquals("[a, b, c]", join(list, 0, 3, ", ", "[", "]", false));
        assertEquals("START:b:c:END", join(list, 1, 3, ":", "START:", ":END", false));
    }

    @Test
    public void testJoin_CollectionWithTrim() {
        List<String> list = Arrays.asList(" a ", " b ", " c ");

        assertEquals("a, b, c", join(list, 0, 3, ", ", true));
        assertEquals("a:b:c", join(list, 0, 3, ":", true));
        assertEquals("[a, b]", join(list, 0, 2, ", ", "[", "]", true));
    }

    @Test
    public void testJoin_Iterator() {
        assertEquals("a, b, c", join(Arrays.asList("a", "b", "c").iterator()));
        assertEquals("a:b:c", join(Arrays.asList("a", "b", "c").iterator(), ":"));
        assertEquals("abc", join(Arrays.asList("a", "b", "c").iterator(), ""));
        assertEquals("", join(Collections.emptyList().iterator()));
        assertEquals("", join((Iterator<?>) null));
    }

    @Test
    public void testJoin_IteratorWithPrefixSuffix() {
        assertEquals("[a, b]", join(Arrays.asList("a", "b").iterator(), ", ", "[", "]"));
        assertEquals("{1-2-3}", join(Arrays.asList(1, 2, 3).iterator(), "-", "{", "}"));
    }

    @Test
    public void testJoin_IteratorWithTrim() {
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c ").iterator(), ", ", "", "", true));
        assertEquals("[a, b]", join(Arrays.asList(" a ", " b ").iterator(), ", ", "[", "]", true));
    }

    @Test
    public void testJoinEntries_Map() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1, b=2", joinEntries(map));
        assertEquals("a=1;b=2", joinEntries(map, ";"));
        assertEquals("a=1|b=2", joinEntries(map, "|"));
        assertEquals("", joinEntries(Collections.emptyMap()));
        assertEquals("", joinEntries((Map<?, ?>) null));
    }

    @Test
    public void testJoinEntries_MapWithDelimiters() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a:1;b:2", joinEntries(map, ";", ":"));
        assertEquals("a:1|b:2", joinEntries(map, "|", ":"));
        assertEquals("a->1, b->2", joinEntries(map, ", ", "->"));
    }

    @Test
    public void testJoinEntries_MapWithPrefixSuffix() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("{a=1, b=2}", joinEntries(map, ", ", "=", "{", "}"));
        assertEquals("[a:1|b:2]", joinEntries(map, "|", ":", "[", "]"));
    }

    @Test
    public void testJoinEntries_MapWithTrim() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(" a ", " 1 ");
        map.put(" b ", " 2 ");
        assertEquals("a=1, b=2", joinEntries(map, ", ", "=", "", "", true));
        assertEquals("{a:1|b:2}", joinEntries(map, "|", ":", "{", "}", true));
    }

    @Test
    public void testJoinEntries_MapWithExtractors() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 100);
        map.put("key2", 200);

        Function<Map.Entry<String, Integer>, String> keyExtractor = e -> e.getKey().toUpperCase();
        Function<Map.Entry<String, Integer>, String> valueExtractor = e -> String.valueOf(e.getValue() * 2);

        assertEquals("KEY1=200, KEY2=400", joinEntries(map.entrySet(), ", ", "=", "", "", false, keyExtractor, valueExtractor));
    }

    @Test
    public void testJoinEntries_MapWithRange() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("b=2", joinEntries(map, 1, 2, ","));
        assertEquals("b:2", joinEntries(map, 1, 2, ",", ":"));
        assertEquals("b=2, c=3", joinEntries(map, 1, 3, ", "));
        assertEquals("[b:2|c:3]", joinEntries(map, 1, 3, "|", ":", "[", "]", false));
        assertEquals("", joinEntries(map, 1, 1, ","));

        try {
            joinEntries(map, 0, 4, ",");
            fail("Should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {
        }
    }

    @Test
    public void testConcat_TwoStrings() {
        assertEquals("HelloWorld", concat("Hello", "World"));
        assertEquals("Hello", concat("Hello", ""));
        assertEquals("World", concat("", "World"));
        assertEquals("", concat("", ""));
        assertEquals("Hello", concat("Hello", null));
        assertEquals("World", concat(null, "World"));
        assertEquals("", concat(null, null));
    }

    @Test
    public void testConcat_ThreeStrings() {
        assertEquals("HelloWorldTest", concat("Hello", "World", "Test"));
        assertEquals("HelloWorld", concat("Hello", "World", ""));
        assertEquals("HelloTest", concat("Hello", "", "Test"));
        assertEquals("Hello", concat("Hello", null, null));
        assertEquals("", concat(null, null, null));
    }

    @Test
    public void testConcat_MultipleStrings() {
        assertEquals("abcde", concat("a", "b", "c", "d", "e"));
        assertEquals("abcdefghi", concat("a", "b", "c", "d", "e", "f", "g", "h", "i"));
        assertEquals("abc", concat("a", null, "b", null, "c"));
        assertEquals("", concat(new String[] {}));
        assertEquals("", concat((String[]) null));
    }

    @Test
    public void testConcat_Objects() {
        assertEquals("12", concat(1, 2));
        assertEquals("123", concat(1, 2, 3));
        assertEquals("1null3", concat(1, null, 3));
        assertEquals("true42", concat(true, 42));
        assertEquals("", concat(null, null));
    }

    @Test
    public void testReverseDelimited_Char() {
        assertEquals("c.b.a", reverseDelimited("a.b.c", '.'));
        assertEquals("a.b.c", reverseDelimited("a.b.c", 'x'));
        assertEquals("", reverseDelimited("", '.'));
        assertNull(reverseDelimited(null, '.'));
        assertEquals("a", reverseDelimited("a", '.'));
    }

    @Test
    public void testReverseDelimited_String() {
        assertEquals("c.b.a", reverseDelimited("a.b.c", "."));
        assertEquals("789->456->123", reverseDelimited("123->456->789", "->"));
        assertEquals("a.b.c", reverseDelimited("a.b.c", "xyz"));
        assertEquals("", reverseDelimited("", "."));
        assertNull(reverseDelimited(null, "."));
    }

    @Test
    public void testShuffle_WithRandom() {
        Random rnd = new Random(12345);
        String original = "abcdefghijk";
        String shuffled = shuffle(original, rnd);
        assertEquals(original.length(), shuffled.length());

        for (char c : original.toCharArray()) {
            assertTrue(shuffled.indexOf(c) >= 0);
        }
    }

    @Test
    public void testIsBase64_Byte() {
        assertTrue(isBase64((byte) 'A'));
        assertTrue(isBase64((byte) 'a'));
        assertTrue(isBase64((byte) '0'));
        assertTrue(isBase64((byte) '+'));
        assertTrue(isBase64((byte) '/'));
        assertTrue(isBase64((byte) '='));
        assertFalse(isBase64((byte) '#'));
        assertFalse(isBase64((byte) ' '));
    }

    @Test
    public void testIsBase64_ByteArray() {
        assertTrue(isBase64("SGVsbG8gV29ybGQ=".getBytes()));
        assertTrue(isBase64("".getBytes()));
        assertFalse(isBase64("Hello#World".getBytes()));
        assertTrue(isBase64("SGVsbG8 V29ybGQ=".getBytes()));
    }

    @Test
    public void testIsBase64_String() {
        assertTrue(isBase64("SGVsbG8gV29ybGQ="));
        assertTrue(isBase64(""));
        assertFalse(isBase64("Hello#World"));
        assertTrue(isBase64("SGVsbG8 V29ybGQ="));
    }

    @Test
    public void testExtractFirstDouble_WithScientific() {
        assertEquals("1.23e4", extractFirstDouble("value=1.23e4", true));
        assertEquals("1.23E-4", extractFirstDouble("small=1.23E-4", true));
        assertEquals("123.45", extractFirstDouble("abc123.45def", true));
        assertNull(extractFirstDouble("no numbers", true));
        assertNull(extractFirstDouble("", true));
        assertNull(extractFirstDouble(null, true));
    }

    @Test
    public void testReplaceFirstDouble_WithScientific() {
        assertEquals("value=[NUM]", replaceFirstDouble("value=1.23e4", "[NUM]", true));
        assertEquals("small=[NUM]", replaceFirstDouble("small=1.23E-4", "[NUM]", true));
        assertEquals("abc[NUM]def", replaceFirstDouble("abc123.45def", "[NUM]", true));
        assertEquals("no numbers", replaceFirstDouble("no numbers", "XXX", true));
        assertEquals("", replaceFirstDouble("", "XXX", true));
        assertEquals("", replaceFirstDouble(null, "XXX", true));
    }

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
    public void testStrUtil_SubstringAfterLast() {
        assertEquals("txt", StrUtil.substringAfterLast("file.name.txt", '.').orElse(null));
        assertFalse(StrUtil.substringAfterLast("Hello", 'x').isPresent());

        assertEquals("txt", StrUtil.substringAfterLast("file.name.txt", ".").orElse(null));
        assertFalse(StrUtil.substringAfterLast("Hello", "xyz").isPresent());

        assertEquals("name", StrUtil.substringAfterLast("file.name.txt", ".", 9).orElse(null));
        assertFalse(StrUtil.substringAfterLast("file.name.txt", ".", 3).isPresent());
    }

    @Test
    public void testStrUtil_SubstringAfterAny() {
        assertEquals("llo World", StrUtil.substringAfterAny("Hello World", 'x', 'e', 'z').orElse(null));
        assertFalse(StrUtil.substringAfterAny("Hello", 'x', 'y', 'z').isPresent());

        assertEquals("World", StrUtil.substringAfterAny("Hello World", "xyz", "Hello ", "abc").orElse(null));
        assertFalse(StrUtil.substringAfterAny("Hello", "xyz", "abc").isPresent());
    }

    @Test
    public void testStrUtil_SubstringBefore() {
        assertEquals("He", StrUtil.substringBefore("Hello World", 'l').orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", 'x').isPresent());

        assertEquals("Hello", StrUtil.substringBefore("Hello World", " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello", "xyz").isPresent());

        assertEquals("lo", StrUtil.substringBefore("Hello World", 3, " World").orElse(null));
        assertFalse(StrUtil.substringBefore("Hello World", 7, " ").isPresent());
    }

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
    public void testStrUtil_SubstringBeforeAny() {
        assertEquals("He", StrUtil.substringBeforeAny("Hello World", 'x', 'l', 'z').orElse(null));
        assertFalse(StrUtil.substringBeforeAny("Hello", 'x', 'y', 'z').isPresent());

        assertEquals("Hello", StrUtil.substringBeforeAny("Hello World", "xyz", " World", "abc").orElse(null));
        assertFalse(StrUtil.substringBeforeAny("Hello", "xyz", "abc").isPresent());
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
    public void testStrUtil_CreateNumber() {
        assertEquals(123, StrUtil.createInteger("123").orElse(-1));
        assertFalse(StrUtil.createInteger("abc").isPresent());
        assertFalse(StrUtil.createInteger("").isPresent());
        assertFalse(StrUtil.createInteger(null).isPresent());

        assertEquals(123L, StrUtil.createLong("123").orElse(-1L));
        assertFalse(StrUtil.createLong("abc").isPresent());
        assertFalse(StrUtil.createLong("").isPresent());
        assertFalse(StrUtil.createLong(null).isPresent());

        assertEquals(123.45f, StrUtil.createFloat("123.45").orElse(-1f), 0.001f);
        assertFalse(StrUtil.createFloat("abc").isPresent());
        assertFalse(StrUtil.createFloat("").isPresent());
        assertFalse(StrUtil.createFloat(null).isPresent());

        assertEquals(123.45, StrUtil.createDouble("123.45").orElse(-1.0), 0.001);
        assertFalse(StrUtil.createDouble("abc").isPresent());
        assertFalse(StrUtil.createDouble("").isPresent());
        assertFalse(StrUtil.createDouble(null).isPresent());

        assertEquals(new BigInteger("123456789012345678901234567890"), StrUtil.createBigInteger("123456789012345678901234567890").orElse(null));
        assertFalse(StrUtil.createBigInteger("abc").isPresent());
        assertFalse(StrUtil.createBigInteger("").isPresent());
        assertFalse(StrUtil.createBigInteger(null).isPresent());

        assertEquals(new BigDecimal("123.45678901234567890"), StrUtil.createBigDecimal("123.45678901234567890").orElse(null));
        assertFalse(StrUtil.createBigDecimal("abc").isPresent());
        assertFalse(StrUtil.createBigDecimal("").isPresent());
        assertFalse(StrUtil.createBigDecimal(null).isPresent());

        assertTrue(StrUtil.createNumber("123").isPresent());
        assertTrue(StrUtil.createNumber("123.45").isPresent());
        assertTrue(StrUtil.createNumber("123L").isPresent());
        assertFalse(StrUtil.createNumber("abc").isPresent());
        assertFalse(StrUtil.createNumber("").isPresent());
        assertFalse(StrUtil.createNumber(null).isPresent());
    }

    @Test
    public void testEdgeCases() {
        String longString = "a".repeat(10000);
        assertEquals(10000, reverse(longString).length());
        assertEquals(10000, sort(longString).length());

        String unicode = "Hello 世界 🌍";
        assertNotNull(reverse(unicode));
        assertNotNull(sort(unicode));

        String special = "!@#$%^&*()_+-=[] {}|;:'\",.<>?/\\`~";
        assertNotNull(reverse(special));
        assertNotNull(sort(special));
    }

    @Test
    public void testNullHandling() {
        assertNull(substring(null, 0));
        assertNull(substringAfter(null, 'a'));
        assertNull(substringBefore(null, 'a'));
        assertNull(reverse(null));
        assertNull(sort(null));
        assertNull(rotate(null, 5));
        assertNull(shuffle(null));
        assertEquals("", join((Object[]) null));
        assertEquals("", base64Encode(null));
        assertEquals("", base64EncodeString(null));
    }

    @Test
    public void testEmptyStringHandling() {
        assertEquals("", substring("", 0));
        assertNull(substringAfter("", 'a'));
        assertEquals("", reverse(""));
        assertEquals("", sort(""));
        assertEquals("", rotate("", 5));
        assertEquals("", shuffle(""));
        assertEquals("", join(new Object[] {}));
        assertEquals("", base64Encode(new byte[] {}));
        assertEquals("", base64EncodeString(""));
    }

    @Test
    @DisplayName("Test isAllEmpty(CharSequence, CharSequence)")
    public void testIsAllEmptyTwoArgs() {
        assertTrue(Strings.isAllEmpty(null, null));
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertFalse(Strings.isAllEmpty(null, "foo"));
        assertFalse(Strings.isAllEmpty("", "bar"));
        assertFalse(Strings.isAllEmpty("bob", ""));
    }

    @Test
    @DisplayName("Test isWrappedWith() with prefix and suffix")
    public void testIsWrappedWithPrefixSuffix() {
        assertTrue(Strings.isWrappedWith("[test]", "[", "]"));
        assertFalse(Strings.isWrappedWith("test]", "[", "]"));
        assertFalse(Strings.isWrappedWith("[test", "[", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "", "]"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "[", ""));
    }

    @Test
    @DisplayName("Test repeat(char, int)")
    public void testRepeatChar() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("a", Strings.repeat('a', 1));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    @DisplayName("Test split(String)")
    public void testSplitString() {
        assertArrayEquals(new String[0], Strings.split(null, ","));
        assertArrayEquals(new String[0], Strings.split("", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens()")
    public void testSplitPreserveAllTokens() {
        assertArrayEquals(new String[0], Strings.splitPreserveAllTokens(null, ','));
        assertArrayEquals(new String[] { "" }, Strings.splitPreserveAllTokens("", ','));
        assertArrayEquals(new String[] { "a", "", "b", "c" }, Strings.splitPreserveAllTokens("a,,b,c", ','));
    }

    @Test
    @DisplayName("Test isAsciiPrintable(char)")
    public void testIsAsciiPrintableChar() {
        assertTrue(Strings.isAsciiPrintable('a'));
        assertTrue(Strings.isAsciiPrintable('A'));
        assertFalse(Strings.isAsciiPrintable('\n'));
    }

    @Test
    @DisplayName("Test indexOf(char)")
    public void testIndexOfChar() {
        assertEquals(-1, Strings.indexOf(null, 'a'));
        assertEquals(-1, Strings.indexOf("", 'a'));
        assertEquals(0, Strings.indexOf("aabaabaa", 'a'));
        assertEquals(2, Strings.indexOf("aabaabaa", 'b'));
    }

    @Test
    @DisplayName("Test indexOf(String)")
    public void testIndexOfString() {
        assertEquals(-1, Strings.indexOf(null, "a"));
        assertEquals(-1, Strings.indexOf("", "a"));
        assertEquals(0, Strings.indexOf("aabaabaa", "a"));
        assertEquals(2, Strings.indexOf("aabaabaa", "b"));
        assertEquals(1, Strings.indexOf("aabaabaa", "ab"));
    }

    @Test
    @DisplayName("Test indexOfAny(char...)")
    public void testIndexOfAnyChars() {
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
        assertEquals(-1, Strings.indexOfAny("", 'a', 'b'));
        assertEquals(0, Strings.indexOfAny("zzabyycdxx", 'z', 'a'));
        assertEquals(3, Strings.indexOfAny("zzabyycdxx", 'b', 'd'));
    }

    @Test
    @DisplayName("Test indexOfAny(String...)")
    public void testIndexOfAnyStrings() {
        assertEquals(-1, Strings.indexOfAny(null, "ab", "cd"));
        assertEquals(-1, Strings.indexOfAny("", "ab", "cd"));
        assertEquals(2, Strings.indexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(6, Strings.indexOfAny("zzabyycdxx", "cd", "ef"));
    }

    @Test
    @DisplayName("Test lastIndexOf(char)")
    public void testLastIndexOfChar() {
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
        assertEquals(-1, Strings.lastIndexOf("", 'a'));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", 'a'));
        assertEquals(5, Strings.lastIndexOf("aabaabaa", 'b'));
    }

    @Test
    @DisplayName("Test lastIndexOf(String)")
    public void testLastIndexOfString() {
        assertEquals(-1, Strings.lastIndexOf(null, "a"));
        assertEquals(-1, Strings.lastIndexOf("", "a"));
        assertEquals(7, Strings.lastIndexOf("aabaabaa", "a"));
        assertEquals(4, Strings.lastIndexOf("aabaabaa", "ab"));
    }

    @Test
    @DisplayName("Test lastIndexOfAny(char...)")
    public void testLastIndexOfAnyChars() {
        assertEquals(-1, Strings.lastIndexOfAny((String) null, new char[] { 'a' }));
        assertEquals(-1, Strings.lastIndexOfAny("", new char[] { 'a' }));
        assertEquals(1, Strings.lastIndexOfAny("zzabyycdxx", 'z', 'x'));
        assertEquals(7, Strings.lastIndexOfAny("zzabyycdxx", 'd', 'x'));
    }

    @Test
    @DisplayName("Test lastIndexOfAny(String...)")
    public void testLastIndexOfAnyStrings() {
        assertEquals(-1, Strings.lastIndexOfAny(null, "ab"));
        assertEquals(-1, Strings.lastIndexOfAny("", "ab"));
        assertEquals(2, Strings.lastIndexOfAny("zzabyycdxx", "ab", "cd"));
        assertEquals(8, Strings.lastIndexOfAny("zzabyycdxx", "dd", "xx"));
    }

    @Test
    @DisplayName("Test countMatches(char)")
    public void testCountMatchesChar() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(6, Strings.countMatches("aabaabaa", 'a'));
        assertEquals(2, Strings.countMatches("aabaabaa", 'b'));
    }

    @Test
    @DisplayName("Test countMatches(String)")
    public void testCountMatchesString() {
        assertEquals(0, Strings.countMatches(null, "a"));
        assertEquals(0, Strings.countMatches("", "a"));
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(1, Strings.countMatches("abcdef", "def"));
    }

    @Test
    @DisplayName("Test contains(char)")
    public void testContainsChar() {
        assertFalse(Strings.contains(null, 'a'));
        assertFalse(Strings.contains("", 'a'));
        assertTrue(Strings.contains("abc", 'a'));
        assertFalse(Strings.contains("abc", 'd'));
    }

    @Test
    @DisplayName("Test contains(String)")
    public void testContainsString() {
        assertFalse(Strings.contains(null, "a"));
        assertFalse(Strings.contains("", "a"));
        assertTrue(Strings.contains("abc", "a"));
        assertTrue(Strings.contains("abc", "ab"));
        assertFalse(Strings.contains("abc", "d"));
    }

    @Test
    @DisplayName("Test containsAll(char...)")
    public void testContainsAllChars() {
        assertTrue(Strings.containsAll("abcd", 'a', 'b'));
        assertFalse(Strings.containsAll("abcd", 'a', 'e'));
        assertTrue(Strings.containsAll("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAll(null, 'a'));
    }

    @Test
    @DisplayName("Test containsAll(String...)")
    public void testContainsAllStrings() {
        assertTrue(Strings.containsAll("abcdef", "ab", "cd"));
        assertFalse(Strings.containsAll("abcdef", "ab", "gh"));
        assertTrue(Strings.containsAll("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAll(null, "a"));
    }

    @Test
    @DisplayName("Test containsAny(char...)")
    public void testContainsAnyChars() {
        assertTrue(Strings.containsAny("abcd", 'a', 'e'));
        assertFalse(Strings.containsAny("abcd", 'e', 'f'));
        assertFalse(Strings.containsAny("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertFalse(Strings.containsAny(null, 'a'));
    }

    @Test
    @DisplayName("Test containsAny(String...)")
    public void testContainsAnyStrings() {
        assertTrue(Strings.containsAny("abcdef", "ab", "gh"));
        assertFalse(Strings.containsAny("abcdef", "gh", "ij"));
        assertFalse(Strings.containsAny("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertFalse(Strings.containsAny(null, "a"));
    }

    @Test
    @DisplayName("Test containsNone(char...)")
    public void testContainsNoneChars() {
        assertTrue(Strings.containsNone("abcd", 'e', 'f'));
        assertFalse(Strings.containsNone("abcd", 'a', 'e'));
        assertTrue(Strings.containsNone("abcd", CommonUtil.EMPTY_CHAR_ARRAY));
        assertTrue(Strings.containsNone(null, 'a'));
    }

    @Test
    @DisplayName("Test containsNone(String...)")
    public void testContainsNoneStrings() {
        assertTrue(Strings.containsNone("abcdef", "gh", "ij"));
        assertFalse(Strings.containsNone("abcdef", "ab", "gh"));
        assertTrue(Strings.containsNone("abcdef", CommonUtil.EMPTY_STRING_ARRAY));
        assertTrue(Strings.containsNone(null, "a"));
    }

    @Test
    @DisplayName("Test indexOfDifference(String, String)")
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
    @DisplayName("Test indexOfDifference(String...)")
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
    @DisplayName("Test commonPrefix(CharSequence, CharSequence)")
    public void testCommonPrefixTwoArgs() {
        assertEquals("", Strings.commonPrefix(null, null));
        assertEquals("", Strings.commonPrefix("", "abc"));
        assertEquals("", Strings.commonPrefix("abc", ""));
        assertEquals("abc", Strings.commonPrefix("abc", "abc"));
        assertEquals("ab", Strings.commonPrefix("abc", "abxyz"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
    }

    @Test
    @DisplayName("Test commonPrefix(CharSequence...)")
    public void testCommonPrefixVarArgs() {
        assertEquals("", Strings.commonPrefix());
        assertEquals("", Strings.commonPrefix((CharSequence[]) null));
        assertEquals("abc", Strings.commonPrefix("abc"));
        assertEquals("a", Strings.commonPrefix("abc", "axy", "aijk"));
        assertEquals("", Strings.commonPrefix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test commonSuffix(CharSequence, CharSequence)")
    public void testCommonSuffixTwoArgs() {
        assertEquals("", Strings.commonSuffix(null, null));
        assertEquals("", Strings.commonSuffix("", "abc"));
        assertEquals("", Strings.commonSuffix("abc", ""));
        assertEquals("abc", Strings.commonSuffix("abc", "abc"));
        assertEquals("bc", Strings.commonSuffix("abc", "xbc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
    }

    @Test
    @DisplayName("Test commonSuffix(CharSequence...)")
    public void testCommonSuffixVarArgs() {
        assertEquals("", Strings.commonSuffix());
        assertEquals("", Strings.commonSuffix((CharSequence[]) null));
        assertEquals("abc", Strings.commonSuffix("abc"));
        assertEquals("c", Strings.commonSuffix("abc", "xyc", "ijkc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test strip(String[])")
    public void testStripArray() {
        String[] strs = { " a ", "  b  ", " c" };
        Strings.strip(strs);
        assertArrayEquals(new String[] { "a", "b", "c" }, strs);
    }

    @Test
    @DisplayName("Test methods with StringBuilder input")
    public void testStringBuilderInput() {
        StringBuilder sb = new StringBuilder("test");
        assertTrue(Strings.isNotEmpty(sb));
        assertFalse(Strings.isBlank(sb));
        assertEquals(4, Strings.lengthOfCommonPrefix(sb, "test"));
    }

    @Test
    @DisplayName("Test methods with very long strings")
    public void testLongStrings() {
        String longStr = Strings.repeat('a', 10000);
        assertEquals(10000, longStr.length());
        assertTrue(Strings.isAllLowerCase(longStr));
        assertEquals(1, Strings.countMatches(longStr + "b" + longStr, "b"));
    }

    @Test
    @DisplayName("Test methods with unicode characters")
    public void testUnicodeCharacters() {
        String unicode = "Hello 世界";

        assertTrue(Strings.isNotEmpty(unicode));
        assertFalse(Strings.isAsciiPrintable(unicode));
        assertTrue(Strings.contains(unicode, "世界"));
    }

    @Test
    @DisplayName("Test methods with empty string edge cases")
    public void testEmptyStringEdgeCases() {
        assertEquals("", Strings.repeat("", 5));
        assertEquals(0, Strings.countMatches("", ""));
        assertTrue(Strings.contains("abc", ""));
        assertEquals("abc", Strings.removeAll("abc", ""));
    }

    @Test
    @DisplayName("Test delimiter-based methods")
    public void testDelimiterMethods() {
        assertTrue(Strings.contains("a,b,c", "b", ","));
        assertFalse(Strings.contains("abc", "b", ","));
        assertEquals(2, Strings.indexOf("a,b,c", "b", ","));
        assertEquals(-1, Strings.indexOf("abc", "b", ","));
    }

    @Test
    @DisplayName("Test performance-optimized methods")
    public void testPerformanceOptimized() {
        String str = "abc";

        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "abc"));
        assertEquals(0, Strings.indexOfIgnoreCase("ABC", "ABC"));
    }

    @Test
    @DisplayName("Test null safety across all methods")
    public void testNullSafety() {
        assertDoesNotThrow(() -> {
            Strings.isEmpty(null);
            Strings.isBlank(null);
            Strings.trim((String) null);
            Strings.toLowerCase(null);
            Strings.toUpperCase(null);
            Strings.contains(null, "test");
            Strings.indexOf(null, "test");
            Strings.split(null, ",");
            Strings.replaceAll((String) null, "a", "b");
        });
    }

    @Test
    @DisplayName("Test parameter validation")
    public void testParameterValidation() {
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("test", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.center("test", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.truncate("test", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.split("test", ",", -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissing("test", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.prependIfMissing("test", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.wrapIfMissing("test", ""));
    }

    @Test
    @DisplayName("Test method combinations")
    public void testMethodCombinations() {
        String result = join(new String[] { "Hello", "World" }, " ");

        assertEquals("Hello World", result);

        String replaced = Strings.replaceAll("aaabbbccc", "b", "x");
        assertEquals(3, Strings.countMatches(replaced, 'x'));

        String[] parts = Strings.split("a,b,c", ",");
        assertEquals(3, parts.length);
    }

    @Test
    @DisplayName("Test locale-sensitive methods")
    public void testLocaleSensitive() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));

            String turkishI = "I";
            String lowered = Strings.toLowerCase(turkishI);

            assertEquals("i", Strings.toLowerCase("I", Locale.ENGLISH));

        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    // ==================== NEW TESTS FOR UNTESTED METHODS ====================

    @Test
    public void testUuidWithoutHyphens() {
        String result = Strings.uuidWithoutHyphens();
        assertNotNull(result);
        assertEquals(32, result.length());
        assertFalse(result.contains("-"));
        // Should generate unique values
        assertNotEquals(result, Strings.uuidWithoutHyphens());
    }

    @Test
    public void testToPascalCase() {
        // toPascalCase is deprecated and delegates to toUpperCamelCase
        assertNull(Strings.toPascalCase(null));
        assertEquals("", Strings.toPascalCase(""));
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world"));
    }

    @Test
    public void testToPascalCase_WithSplitChar() {
        assertEquals("FirstName", Strings.toPascalCase("first.name", '.'));
        assertNull(Strings.toPascalCase(null, '.'));
    }

    @Test
    public void testToKebabCase() {
        assertNull(Strings.toKebabCase(null));
        assertEquals("", Strings.toKebabCase(""));
        assertEquals("hello-world", Strings.toKebabCase("helloWorld"));
        assertEquals("hello-world", Strings.toKebabCase("HelloWorld"));
        assertEquals("hello-world-api", Strings.toKebabCase("helloWorldAPI"));
    }

    @Test
    public void testRemoveWhitespace_Array() {
        // null array - no exception
        Strings.removeWhitespace((String[]) null);

        String[] strs = { "  hello  ", "world\t123", " h i " };
        Strings.removeWhitespace(strs);
        assertEquals("hello", strs[0]);
        assertEquals("world123", strs[1]);
        assertEquals("hi", strs[2]);
    }

    @Test
    public void testUrlDecode_WithCharset() {
        Map<String, String> result = Strings.urlDecode("name=test+data", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals("test data", result.get("name"));
    }

    @Test
    public void testReverseDelimited_StringDelimiter() {
        assertNull(Strings.reverseDelimited(null, "."));
        assertEquals("", Strings.reverseDelimited("", "."));
        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));
        assertEquals("abc", Strings.reverseDelimited("abc", "."));
    }

    @Test
    public void testReplaceFirstIgnoreCase_WithFromIndex() {
        assertEquals("hello WORLD xyz", Strings.replaceFirstIgnoreCase("hello world xyz", 0, "WORLD", "WORLD"));
        assertEquals("hello WORLD xyz", Strings.replaceFirstIgnoreCase("hello world xyz", 0, "world", "WORLD"));
        assertNull(Strings.replaceFirstIgnoreCase(null, 0, "a", "b"));
    }

    @Test
    public void testReplaceAllIgnoreCase_WithFromIndex() {
        assertEquals("hello XX XX", Strings.replaceAllIgnoreCase("hello ab AB", 0, "ab", "XX"));
        assertNull(Strings.replaceAllIgnoreCase(null, 0, "a", "b"));
    }

    @Test
    public void testReplaceIgnoreCase_WithMax() {
        assertEquals("XX ab AB", Strings.replaceIgnoreCase("ab ab AB", 0, "ab", "XX", 1));
        assertEquals("XX XX AB", Strings.replaceIgnoreCase("ab ab AB", 0, "ab", "XX", 2));
        assertNull(Strings.replaceIgnoreCase(null, 0, "a", "b", 1));
    }

    @Test
    public void testReplace_WithFromIndexAndMax() {
        assertEquals("hello XX XX", Strings.replace("hello ab ab", 0, "ab", "XX", 10));
        assertEquals("hello XX ab", Strings.replace("hello ab ab", 0, "ab", "XX", 1));
        assertNull(Strings.replace(null, 0, "a", "b", 1));
    }

    @Test
    public void testIndexOf_WithDelimiter() {
        // indexOf with delimiter searches for valueToFind as a whole token separated by delimiter
        int result = Strings.indexOf("a,b,c", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, Strings.indexOf(null, "b", ","));
        assertEquals(-1, Strings.indexOf("a,b,c", null, ","));
    }

    @Test
    public void testIndexOf_WithDelimiterAndFromIndex() {
        int result = Strings.indexOf("a,b,c,b", "b", ",", 3);
        assertTrue(result >= 0);

        assertEquals(-1, Strings.indexOf(null, "b", ",", 0));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiter() {
        int result = Strings.indexOfIgnoreCase("a,B,c", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, Strings.indexOfIgnoreCase(null, "b", ","));
    }

    @Test
    public void testIndexOfIgnoreCase_WithDelimiterAndFromIndex() {
        int result = Strings.indexOfIgnoreCase("a,B,c,b", "b", ",", 3);
        assertTrue(result >= 0);

        assertEquals(-1, Strings.indexOfIgnoreCase(null, "b", ",", 0));
    }

    @Test
    public void testLastIndexOf_WithDelimiter() {
        int result = Strings.lastIndexOf("a,b,c,b", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOf(null, "b", ","));
    }

    @Test
    public void testLastIndexOf_WithDelimiterAndFromBack() {
        int result = Strings.lastIndexOf("a,b,c,b", "b", ",", 10);
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOf(null, "b", ",", 0));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiter() {
        int result = Strings.lastIndexOfIgnoreCase("a,B,c,b", "b", ",");
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOfIgnoreCase(null, "b", ","));
    }

    @Test
    public void testLastIndexOfIgnoreCase_WithDelimiterAndFromBack() {
        int result = Strings.lastIndexOfIgnoreCase("a,B,c,b", "b", ",", 10);
        assertTrue(result >= 0);

        assertEquals(-1, Strings.lastIndexOfIgnoreCase(null, "b", ",", 0));
    }

    @Test
    public void testContains_WithDelimiter() {
        assertTrue(Strings.contains("a,b,c", "b", ","));
        assertFalse(Strings.contains("a,b,c", "d", ","));
        assertFalse(Strings.contains(null, "b", ","));
    }

    @Test
    public void testContainsIgnoreCase_WithDelimiter() {
        assertTrue(Strings.containsIgnoreCase("a,B,c", "b", ","));
        assertFalse(Strings.containsIgnoreCase("a,b,c", "d", ","));
        assertFalse(Strings.containsIgnoreCase(null, "b", ","));
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
    public void testSubstringAfter_WithExclusiveEndIndex() {
        // substringAfter(str, delimiter, exclusiveEndIndex)
        assertEquals("ell", Strings.substringAfter("hello world", "h", 4));
        assertNull(Strings.substringAfter(null, "h", 4));
    }

    @Test
    public void testSubstringAfterLast_WithExclusiveEndIndex() {
        // substringAfterLast(str, delimiter, exclusiveEndIndex)
        assertEquals("ell", Strings.substringAfterLast("hello", "h", 4));
        assertNull(Strings.substringAfterLast(null, "h", 4));
    }

    @Test
    public void testSubstringBefore_WithInclusiveBeginIndex() {
        // substringBefore(str, inclusiveBeginIndex, delimiter)
        assertEquals("ello", Strings.substringBefore("hello world", 1, " "));
        assertNull(Strings.substringBefore(null, 0, " "));
    }

    @Test
    public void testSubstringBeforeLast_WithInclusiveBeginIndex() {
        // substringBeforeLast(str, inclusiveBeginIndex, delimiter)
        assertEquals("ello", Strings.substringBeforeLast("hello world", 1, " "));
        assertNull(Strings.substringBeforeLast(null, 0, " "));
    }

    @Test
    public void testBase64EncodeString_WithCharset() {
        String encoded = Strings.base64EncodeString("hello", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(encoded);
        assertEquals("hello", Strings.base64DecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    public void testBase64DecodeToString_WithCharset() {
        String encoded = Strings.base64EncodeString("hello");
        String decoded = Strings.base64DecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello", decoded);
    }

    @Test
    public void testBase64UrlDecodeToString_WithCharset() {
        byte[] data = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String encoded = Strings.base64UrlEncode(data);
        String decoded = Strings.base64UrlDecodeToString(encoded, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals("hello", decoded);
    }

    @Test
    public void testUrlEncode_WithCharset() {
        String encoded = Strings.urlEncode("hello world", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(encoded);
        assertTrue(encoded.contains("hello"));
    }

    @Test
    public void testJoin_ObjectArray_WithPrefixSuffix() {
        assertEquals("[a, b, c]", Strings.join(new Object[] { "a", "b", "c" }, ", ", "[", "]"));
    }

    @Test
    public void testJoin_ObjectArray_WithFromToIndex() {
        assertEquals("b, c", Strings.join(new Object[] { "a", "b", "c", "d" }, 1, 3, ", "));
    }

    @Test
    public void testJoin_ObjectArray_WithTrim() {
        assertEquals("[a, b]", Strings.join(new Object[] { " a ", " b " }, ", ", "[", "]", true));
    }

    @Test
    public void testJoin_Collection_WithFromToIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b, c", Strings.join(list, 1, 3, ", "));
    }

    @Test
    public void testJoin_Iterator_WithPrefixSuffix() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        assertEquals("[a, b, c]", Strings.join(iter, ", ", "[", "]"));
    }

    @Test
    public void testJoinEntries_WithEntryDelimiterAndKeyValueDelimiter() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        assertEquals("a=1, b=2", Strings.joinEntries(map, ", ", "="));
    }

    @Test
    public void testJoinEntries_WithPrefixSuffix() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        assertEquals("{a=1, b=2}", Strings.joinEntries(map, ", ", "=", "{", "}"));
    }

    @Test
    public void testJoinEntries_WithFromToIndex() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        assertEquals("a=1", Strings.joinEntries(map, 0, 1, ", ", "="));
    }

    // TODO: test remaining joinEntries overloads with trim, fromIndex/toIndex, prefix/suffix combinations

    @Test
    public void testSubstringBetween_IntIntOverload() {
        // substringBetween(str, exclusiveBeginIndex, exclusiveEndIndex)
        assertEquals("ell", Strings.substringBetween("hello", 0, 4));
        assertNull(Strings.substringBetween(null, 0, 4));
    }

    @Test
    public void testSubstringBetween_IntCharOverload() {
        // substringBetween(str, exclusiveBeginIndex, charDelimiterOfExclusiveEndIndex)
        String result = Strings.substringBetween("hello world", 0, ' ');
        assertEquals("ello", result);
    }

    @Test
    public void testSubstringBetween_CharIntOverload() {
        // substringBetween(str, charDelimiterOfExclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substringBetween("hello world", ' ', 11);
        assertEquals("world", result);
    }

    @Test
    public void testSubstringBetween_IntStringOverload() {
        // substringBetween(str, exclusiveBeginIndex, stringDelimiterOfExclusiveEndIndex)
        String result = Strings.substringBetween("hello world", 0, " ");
        assertEquals("ello", result);
    }

    @Test
    public void testSubstringBetween_StringIntOverload() {
        // substringBetween(str, stringDelimiterOfExclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substringBetween("hello world", " ", 11);
        assertEquals("world", result);
    }

    @Test
    public void testSubstringBetween_WithFromIndex() {
        // substringBetween(str, fromIndex, delimiterBegin, delimiterEnd)
        String result = Strings.substringBetween("a[b]c[d]", 3, "[", "]");
        assertEquals("d", result);
    }

    @Test
    public void testSubstringBetweenIgnoreCase_WithFromIndex() {
        String result = Strings.substringBetweenIgnoreCase("aXbYcXdY", 3, "x", "y");
        assertEquals("d", result);
    }

    @Test
    public void testSubstring_WithIntUnaryOperator_FuncBegin() {
        // substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substring("hello", len -> 0, 5);
        assertEquals("hello", result);
    }

    @Test
    public void testSubstring_CharDelimiterOfBeginIndex() {
        // substring(str, charDelimiterOfInclusiveBeginIndex)
        String result = Strings.substring("hello world", ' ');
        assertEquals(" world", result);
    }

    @Test
    public void testSubstring_StringDelimiterOfBeginIndex() {
        // substring(str, stringDelimiterOfInclusiveBeginIndex)
        String result = Strings.substring("hello world", " ");
        assertEquals(" world", result);
    }

    @Test
    public void testSubstring_IntCharDelimiterEnd() {
        // substring(str, inclusiveBeginIndex, charDelimiterOfExclusiveEndIndex)
        String result = Strings.substring("hello world", 0, ' ');
        assertEquals("hello", result);
    }

    @Test
    public void testSubstring_IntStringDelimiterEnd() {
        // substring(str, inclusiveBeginIndex, stringDelimiterOfExclusiveEndIndex)
        String result = Strings.substring("hello world", 0, " ");
        assertEquals("hello", result);
    }

    @Test
    public void testSubstring_CharDelimiterBeginIntEnd() {
        // substring(str, charDelimiterOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substring("hello world", ' ', 11);
        assertEquals(" world", result);
    }

    @Test
    public void testSubstring_StringDelimiterBeginIntEnd() {
        // substring(str, stringDelimiterOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substring("hello world", " ", 11);
        assertEquals(" world", result);
    }

    @Test
    public void testSubstringAfterIgnoreCase() {
        assertEquals("WORLD", Strings.substringAfterIgnoreCase("hello WORLD", "hello "));
        assertEquals("world", Strings.substringAfterIgnoreCase("Hello world", "HELLO "));
        assertNull(Strings.substringAfterIgnoreCase(null, "hello"));
    }

    @Test
    public void testStripStart_NoArgs() {
        assertEquals("hello  ", Strings.stripStart("  hello  "));
        assertNull(Strings.stripStart(null));
    }

    @Test
    public void testStripEnd_NoArgs() {
        assertEquals("  hello", Strings.stripEnd("  hello  "));
        assertNull(Strings.stripEnd(null));
    }

    @Test
    public void testStripStart_ArrayWithChars() {
        String[] strs = { "xxhello", "yworld" };
        Strings.stripStart(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStripEnd_ArrayWithChars() {
        String[] strs = { "helloxx", "worldy" };
        Strings.stripEnd(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStrip_ArrayWithChars() {
        String[] strs = { "xxhelloxx", "yworldy" };
        Strings.strip(strs, "xy");
        assertEquals("hello", strs[0]);
        assertEquals("world", strs[1]);
    }

    @Test
    public void testStripToNull_Array() {
        String[] strs = { "  hello  ", "   " };
        Strings.stripToNull(strs);
        assertEquals("hello", strs[0]);
        assertNull(strs[1]);
    }

    @Test
    public void testStripToEmpty_Array() {
        String[] strs = { "  hello  ", null };
        Strings.stripToEmpty(strs);
        assertEquals("hello", strs[0]);
        assertEquals("", strs[1]);
    }

    @Test
    public void testIndicesOfIgnoreCase_WithFromIndex() {
        int[] indices = Strings.indicesOfIgnoreCase("aAbAa", "a", 2).toArray();
        assertNotNull(indices);
        assertTrue(indices.length >= 1);
    }

    @Test
    public void testIndicesOf_WithFromIndex() {
        int[] indices = Strings.indicesOf("abcabc", "abc", 1).toArray();
        assertNotNull(indices);
        assertEquals(1, indices.length);
        assertEquals(3, indices[0]);
    }

    @Test
    public void testSplitPreserveAllTokens_StringDelimiter_WithTrim() {
        String[] result = Strings.splitPreserveAllTokens(" a , , b ", ",", true);
        assertNotNull(result);
        assertEquals("a", result[0]);
        assertEquals("", result[1]);
        assertEquals("b", result[2]);
    }

    @Test
    public void testSplitPreserveAllTokens_StringDelimiter_WithMax_Trim() {
        String[] result = Strings.splitPreserveAllTokens(" a , , b , c ", ",", 3, true);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    public void testConvertWords_WithDelimiterAndExcludedWords() {
        String result = Strings.convertWords("hello world", " ", Arrays.asList("world"), s -> s.toUpperCase());
        assertNotNull(result);
        assertTrue(result.contains("HELLO"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void testMinIndexOfAll() {
        assertEquals(0, Strings.minIndexOfAll("hello world", "hello", "world"));
        assertEquals(-1, Strings.minIndexOfAll("hello world", "xyz", "abc"));
        assertEquals(-1, Strings.minIndexOfAll(null, "a"));
    }

    @Test
    public void testMinIndexOfAll_WithFromIndex() {
        int result = Strings.minIndexOfAll("hello world", 1, "ello", "world");
        assertEquals(1, result);
    }

    @Test
    public void testMaxIndexOfAll() {
        assertEquals(6, Strings.maxIndexOfAll("hello world", "hello", "world"));
        assertEquals(-1, Strings.maxIndexOfAll("hello world", "xyz", "abc"));
    }

    @Test
    public void testMaxIndexOfAll_WithFromIndex() {
        int result = Strings.maxIndexOfAll("hello world", 1, "ello", "world");
        assertEquals(6, result);
    }

    @Test
    public void testMinLastIndexOfAll() {
        int result = Strings.minLastIndexOfAll("hello hello world", "hello", "world");
        assertTrue(result >= 0);
        assertEquals(-1, Strings.minLastIndexOfAll(null, "a"));
    }

    @Test
    public void testMaxLastIndexOfAll() {
        int result = Strings.maxLastIndexOfAll("hello hello world", "hello", "world");
        assertTrue(result >= 0);
        assertEquals(-1, Strings.maxLastIndexOfAll(null, "a"));
    }

    @Test
    public void testCompareIgnoreCase() {
        assertEquals(0, Strings.compareIgnoreCase("hello", "HELLO"));
        assertTrue(Strings.compareIgnoreCase("a", "b") < 0);
        assertTrue(Strings.compareIgnoreCase("b", "a") > 0);
        assertTrue(Strings.compareIgnoreCase(null, "a") < 0);
        assertTrue(Strings.compareIgnoreCase("a", null) > 0);
        assertEquals(0, Strings.compareIgnoreCase(null, null));
    }

    @Test
    public void testSubstringAfterOrElseItself_CharDelimiter() {
        assertEquals("world", Strings.StrUtil.substringAfterOrElseItself("hello world", ' '));
        assertEquals("hello", Strings.StrUtil.substringAfterOrElseItself("hello", ' '));
    }

    @Test
    public void testSubstringAfterLastOrElseItself_CharDelimiter() {
        assertEquals("c", Strings.StrUtil.substringAfterLastOrElseItself("a.b.c", '.'));
        assertEquals("hello", Strings.StrUtil.substringAfterLastOrElseItself("hello", '.'));
    }

    @Test
    public void testSubstringBeforeOrElseItself_CharDelimiter() {
        assertEquals("hello", Strings.StrUtil.substringBeforeOrElseItself("hello world", ' '));
        assertEquals("hello", Strings.StrUtil.substringBeforeOrElseItself("hello", ' '));
    }

    @Test
    public void testSubstringBeforeLastOrElseItself_CharDelimiter() {
        assertEquals("a.b", Strings.StrUtil.substringBeforeLastOrElseItself("a.b.c", '.'));
        assertEquals("hello", Strings.StrUtil.substringBeforeLastOrElseItself("hello", '.'));
    }

    @Test
    public void testSubstringAfterOrElseItself_WithEndIndex() {
        assertEquals("ell", Strings.StrUtil.substringAfterOrElseItself("hello", "h", 4));
    }

    @Test
    public void testSubstringAfterLastOrElseItself_WithEndIndex() {
        assertEquals("ell", Strings.StrUtil.substringAfterLastOrElseItself("hello", "h", 4));
    }

    @Test
    public void testSubstringBeforeOrElseItself_WithBeginIndex() {
        assertEquals("ello", Strings.StrUtil.substringBeforeOrElseItself("hello world", 1, " "));
    }

    @Test
    public void testSubstringBeforeLastOrElseItself_WithEndIndex() {
        assertEquals("ello", Strings.StrUtil.substringBeforeLastOrElseItself("hello world", 1, " "));
    }

    @Test
    public void testSubstringOrElse_FuncBegin() {
        // substringOrElse(str, funcOfInclusiveBeginIndex, exclusiveEndIndex, defaultStr)
        String result = Strings.StrUtil.substringOrElse("hello", len -> 0, 5, "default");
        assertEquals("hello", result);
    }

    @Test
    public void testSubstringOrElseItself_FuncBegin() {
        // substringOrElseItself(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.StrUtil.substringOrElseItself("hello", len -> 0, 5);
        assertEquals("hello", result);
    }

    @Test
    public void testLenientFormat_MoreArgsThanPlaceholders() {
        String result = Strings.lenientFormat("hello %s", "world", "extra");
        assertNotNull(result);
        assertTrue(result.contains("world"));
        assertTrue(result.contains("extra"));
    }

    @Test
    public void testLenientFormat_FewerArgsThanPlaceholders() {
        String result = Strings.lenientFormat("hello %s %s", "world");
        assertNotNull(result);
        assertTrue(result.contains("world"));
    }

    @Test
    public void testScreamingSnakeCase() {
        assertNull(Strings.toScreamingSnakeCase(null));
        assertEquals("", Strings.toScreamingSnakeCase(""));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("helloWorld"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("HelloWorld"));
    }

    @Test
    public void testRemoveRange() {
        assertEquals("hlo", Strings.removeRange("hello", 1, 3));
        assertEquals("hello", Strings.removeRange("hello", 0, 0));
        assertEquals("", Strings.removeRange("hello", 0, 5));
        assertEquals("", Strings.removeRange(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange(null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange("hello", -1, 2));
    }

    @Test
    public void testQuoteEscaped_WithCustomChar() {
        String result = Strings.quoteEscaped("hello 'world'", '\'');
        assertNotNull(result);
    }

    @Test
    public void testSort_String() {
        assertNull(Strings.sort(null));
        assertEquals("", Strings.sort(""));
        assertEquals("abcde", Strings.sort("edcba"));
        assertEquals("aabbc", Strings.sort("abcba"));
    }

    @Test
    public void testRotate_String() {
        assertNull(Strings.rotate(null, 1));
        assertEquals("", Strings.rotate("", 1));
        assertEquals("oHell", Strings.rotate("Hello", 1));
        assertEquals("elloH", Strings.rotate("Hello", -1));
    }

    @Test
    public void testIsAsciiInteger() {
        assertTrue(Strings.isAsciiInteger("123"));
        assertTrue(Strings.isAsciiInteger("-123"));
        assertTrue(Strings.isAsciiInteger("+123"));
        assertFalse(Strings.isAsciiInteger("12.3"));
        assertFalse(Strings.isAsciiInteger(null));
        assertFalse(Strings.isAsciiInteger(""));
        assertFalse(Strings.isAsciiInteger("abc"));
    }

    // ==================== NEW TESTS: Additional coverage for important utility methods ====================

    @Test
    public void testRemoveWhitespace() {
        assertNull(Strings.removeWhitespace((String) null));
        assertEquals("", Strings.removeWhitespace(""));
        assertEquals("abc", Strings.removeWhitespace("abc"));
        assertEquals("abc", Strings.removeWhitespace("a b c"));
        assertEquals("abc", Strings.removeWhitespace("  a  b  c  "));
        assertEquals("abc", Strings.removeWhitespace("a\tb\nc"));
        assertEquals("abc", Strings.removeWhitespace("a b\tc\r\n"));
        assertEquals("abc", Strings.removeWhitespace("\t\n\r abc \t\n\r"));
    }

    @Test
    public void testIsNumber_EdgeCases() {
        assertTrue(Strings.isNumber("123"));
        assertTrue(Strings.isNumber("-123"));
        assertTrue(Strings.isNumber("12.3"));
        assertTrue(Strings.isNumber("-12.3"));
        assertFalse(Strings.isNumber(null));
        assertFalse(Strings.isNumber(""));
        assertFalse(Strings.isNumber("abc"));
        assertFalse(Strings.isNumber("12.3.4"));
    }

    @Test
    public void testIsAsciiNumber_EdgeCases() {
        assertTrue(Strings.isAsciiNumber("123"));
        assertTrue(Strings.isAsciiNumber("-123"));
        assertTrue(Strings.isAsciiNumber("+123"));
        assertTrue(Strings.isAsciiNumber("12.3"));
        assertFalse(Strings.isAsciiNumber(null));
        assertFalse(Strings.isAsciiNumber(""));
        assertFalse(Strings.isAsciiNumber("abc"));
        assertFalse(Strings.isAsciiNumber("12.3.4"));
    }

    @Test
    public void testParseBoolean_EdgeCases() {
        assertTrue(Strings.parseBoolean("true"));
        assertTrue(Strings.parseBoolean("True"));
        assertTrue(Strings.parseBoolean("TRUE"));
        assertFalse(Strings.parseBoolean("false"));
        assertFalse(Strings.parseBoolean("yes"));
        assertFalse(Strings.parseBoolean("1"));
        assertFalse(Strings.parseBoolean(""));
        assertFalse(Strings.parseBoolean(null));
        assertFalse(Strings.parseBoolean("  true  "));
    }

    @Test
    public void testParseChar_EdgeCases() {
        assertEquals('A', Strings.parseChar("A"));
        assertEquals('$', Strings.parseChar("$"));
        assertEquals('\0', Strings.parseChar(null));
        assertEquals('\0', Strings.parseChar(""));
        // Parse numeric char codes
        assertEquals('A', Strings.parseChar("65"));
        assertEquals('0', Strings.parseChar("48"));
        // Out of range
        assertThrows(IllegalArgumentException.class, () -> Strings.parseChar("-1"));
    }

    @Test
    public void testParseByte_EdgeCases() {
        assertEquals((byte) 0, Strings.parseByte(null));
        assertEquals((byte) 0, Strings.parseByte(""));
        assertEquals((byte) 127, Strings.parseByte("127"));
        assertEquals((byte) -128, Strings.parseByte("-128"));
        assertThrows(NumberFormatException.class, () -> Strings.parseByte("abc"));
    }

    @Test
    public void testParseShort_EdgeCases() {
        assertEquals((short) 0, Strings.parseShort(null));
        assertEquals((short) 0, Strings.parseShort(""));
        assertEquals((short) 32767, Strings.parseShort("32767"));
        assertEquals((short) -32768, Strings.parseShort("-32768"));
    }

    @Test
    public void testParseInt_EdgeCases() {
        assertEquals(0, Strings.parseInt(null));
        assertEquals(0, Strings.parseInt(""));
        assertEquals(123, Strings.parseInt("123"));
        assertEquals(-456, Strings.parseInt("-456"));
        assertThrows(NumberFormatException.class, () -> Strings.parseInt("abc"));
    }

    @Test
    public void testParseLong_EdgeCases() {
        assertEquals(0L, Strings.parseLong(null));
        assertEquals(0L, Strings.parseLong(""));
        assertEquals(123L, Strings.parseLong("123"));
        assertEquals(-456L, Strings.parseLong("-456"));
    }

    @Test
    public void testParseFloat_EdgeCases() {
        assertEquals(0.0f, Strings.parseFloat(null));
        assertEquals(0.0f, Strings.parseFloat(""));
        assertEquals(1.5f, Strings.parseFloat("1.5"));
        assertEquals(-2.5f, Strings.parseFloat("-2.5"));
    }

    @Test
    public void testParseDouble_EdgeCases() {
        assertEquals(0.0, Strings.parseDouble(null));
        assertEquals(0.0, Strings.parseDouble(""));
        assertEquals(1.5, Strings.parseDouble("1.5"));
        assertEquals(-2.5, Strings.parseDouble("-2.5"));
    }

    @Test
    public void testBase64EncodeUtf8String_EdgeCases() {
        assertEquals("", Strings.base64EncodeUtf8String(null));
        assertEquals("", Strings.base64EncodeUtf8String(""));
        String encoded = Strings.base64EncodeUtf8String("Hello World");
        assertNotNull(encoded);
        assertEquals("Hello World", Strings.base64DecodeToUtf8String(encoded));
    }

    @Test
    public void testBase64UrlEncode_EdgeCases() {
        assertEquals("", Strings.base64UrlEncode(null));
        String encoded = Strings.base64UrlEncode("Hello World".getBytes(StandardCharsets.UTF_8));
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        byte[] decoded = Strings.base64UrlDecode(encoded);
        assertEquals("Hello World", new String(decoded, StandardCharsets.UTF_8));
    }

    @Test
    public void testBase64UrlDecodeToString_EdgeCases() {
        assertEquals("", Strings.base64UrlDecodeToString(null));
        String encoded = Strings.base64UrlEncode("Test String".getBytes(StandardCharsets.UTF_8));
        assertEquals("Test String", Strings.base64UrlDecodeToString(encoded));
        assertEquals("Test String", Strings.base64UrlDecodeToUtf8String(encoded));
    }

    @Test
    public void testNormalizeSpace_EdgeCases() {
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
        assertEquals("", Strings.normalizeSpace("   "));
        assertEquals("abc", Strings.normalizeSpace("abc"));
        assertEquals("abc", Strings.normalizeSpace("  abc  "));
        assertEquals("abc def", Strings.normalizeSpace("  abc    def  "));
        assertEquals("abc def", Strings.normalizeSpace("abc\n\tdef"));
        assertEquals("a b c", Strings.normalizeSpace("  a \t b \n c  "));
    }

    @Test
    public void testUnicodeEscaped_EdgeCases() {
        assertEquals("\\u0041", Strings.unicodeEscaped('A'));
        assertEquals("\\u0000", Strings.unicodeEscaped('\0'));
        assertEquals("\\u000a", Strings.unicodeEscaped('\n'));
        assertEquals("\\u0009", Strings.unicodeEscaped('\t'));
        // Multi-byte chars
        String escaped = Strings.unicodeEscaped('\u00E9'); // e with acute
        assertTrue(escaped.startsWith("\\u"));
    }

    @Test
    public void testChomp_EdgeCases() {
        assertNull(Strings.chomp((String) null));
        assertEquals("", Strings.chomp(""));
        assertEquals("abc", Strings.chomp("abc\r"));
        assertEquals("abc", Strings.chomp("abc\n"));
        assertEquals("abc", Strings.chomp("abc\r\n"));
        assertEquals("abc\r\n", Strings.chomp("abc\r\n\r\n"));
        assertEquals("abc", Strings.chomp("abc"));
    }

    @Test
    public void testChop_EdgeCases() {
        assertNull(Strings.chop((String) null));
        assertEquals("", Strings.chop(""));
        assertEquals("ab", Strings.chop("abc"));
        assertEquals("abc", Strings.chop("abc\r\n"));
        assertEquals("abc\n", Strings.chop("abc\n\r"));
        assertEquals("", Strings.chop("a"));
    }

    @Test
    public void testStripAccents_EdgeCases() {
        assertNull(Strings.stripAccents((String) null));
        assertEquals("", Strings.stripAccents(""));
        assertEquals("abc", Strings.stripAccents("abc"));
        assertEquals("eclair", Strings.stripAccents("\u00E9clair"));
        assertEquals("uber", Strings.stripAccents("\u00FCber"));
    }

    @Test
    public void testCapitalize_EdgeCases() {
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
        assertEquals("A", Strings.capitalize("a"));
        assertEquals("Hello", Strings.capitalize("hello"));
        assertEquals("Hello", Strings.capitalize("Hello"));
        assertEquals("123", Strings.capitalize("123"));
        assertEquals(" abc", Strings.capitalize(" abc"));
    }

    @Test
    public void testUncapitalize_EdgeCases() {
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
        assertEquals("a", Strings.uncapitalize("A"));
        assertEquals("hello", Strings.uncapitalize("Hello"));
        assertEquals("hello", Strings.uncapitalize("hello"));
        assertEquals("123", Strings.uncapitalize("123"));
    }

    @Test
    public void testSwapCase_EdgeCases() {
        assertEquals('a', Strings.swapCase('A'));
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('1', Strings.swapCase('1'));
        assertEquals(' ', Strings.swapCase(' '));

        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
        assertEquals("tHE DOG HAS A bone", Strings.swapCase("The dog has a BONE"));
        assertEquals("abc123XYZ", Strings.swapCase("ABC123xyz"));
    }

    @Test
    public void testToKebabCase_EdgeCases() {
        assertNull(Strings.toKebabCase(null));
        assertEquals("", Strings.toKebabCase(""));
        assertEquals("hello-world", Strings.toKebabCase("HelloWorld"));
        assertEquals("hello-world", Strings.toKebabCase("helloWorld"));
        assertEquals("abc", Strings.toKebabCase("abc"));
        assertEquals("io-error", Strings.toKebabCase("IOError"));
    }

    @Test
    public void testToPascalCase_EdgeCases() {
        assertNull(Strings.toPascalCase(null));
        assertEquals("", Strings.toPascalCase(""));
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world"));
        assertEquals("HelloWorld", Strings.toPascalCase("HELLO_WORLD"));
    }

    @Test
    public void testEquals_EdgeCases() {
        assertTrue(Strings.equals(null, null));
        assertFalse(Strings.equals(null, "abc"));
        assertFalse(Strings.equals("abc", null));
        assertTrue(Strings.equals("abc", "abc"));
        assertFalse(Strings.equals("abc", "ABC"));
        assertTrue(Strings.equals("", ""));
    }

    @Test
    public void testEqualsIgnoreCase_EdgeCases() {
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertFalse(Strings.equalsIgnoreCase(null, "abc"));
        assertFalse(Strings.equalsIgnoreCase("abc", null));
        assertTrue(Strings.equalsIgnoreCase("abc", "ABC"));
        assertTrue(Strings.equalsIgnoreCase("", ""));
        assertFalse(Strings.equalsIgnoreCase("abc", "def"));
    }

    @Test
    public void testCompareIgnoreCase_EdgeCases() {
        assertEquals(0, Strings.compareIgnoreCase(null, null));
        assertTrue(Strings.compareIgnoreCase(null, "abc") < 0);
        assertTrue(Strings.compareIgnoreCase("abc", null) > 0);
        assertEquals(0, Strings.compareIgnoreCase("abc", "ABC"));
        assertTrue(Strings.compareIgnoreCase("abc", "def") < 0);
        assertTrue(Strings.compareIgnoreCase("def", "abc") > 0);
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
    public void testLengthOfCommonPrefix_EdgeCases() {
        assertEquals(0, Strings.lengthOfCommonPrefix(null, null));
        assertEquals(0, Strings.lengthOfCommonPrefix(null, "abc"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", null));
        assertEquals(3, Strings.lengthOfCommonPrefix("abc", "abcdef"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
        assertEquals(2, Strings.lengthOfCommonPrefix("abc", "abx"));
    }

    @Test
    public void testLengthOfCommonSuffix_EdgeCases() {
        assertEquals(0, Strings.lengthOfCommonSuffix(null, null));
        assertEquals(0, Strings.lengthOfCommonSuffix(null, "abc"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", null));
        assertEquals(3, Strings.lengthOfCommonSuffix("abc", "xyzabc"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "xyz"));
        assertEquals(2, Strings.lengthOfCommonSuffix("abc", "xbc"));
    }

    @Test
    public void testCommonPrefix_EdgeCases() {
        assertEquals("", Strings.commonPrefix(null, null));
        assertEquals("", Strings.commonPrefix(null, "abc"));
        assertEquals("", Strings.commonPrefix("abc", null));
        assertEquals("abc", Strings.commonPrefix("abc", "abcdef"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertEquals("ab", Strings.commonPrefix("abc", "abx"));
    }

    @Test
    public void testCommonSuffix_EdgeCases() {
        assertEquals("", Strings.commonSuffix(null, null));
        assertEquals("", Strings.commonSuffix(null, "abc"));
        assertEquals("", Strings.commonSuffix("abc", null));
        assertEquals("abc", Strings.commonSuffix("abc", "xyzabc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertEquals("bc", Strings.commonSuffix("abc", "xbc"));
    }

    @Test
    public void testConvertWords_EdgeCases() {
        assertNull(Strings.convertWords(null, String::toUpperCase));
        assertEquals("", Strings.convertWords("", String::toUpperCase));
        assertEquals("HELLO WORLD", Strings.convertWords("hello world", String::toUpperCase));
        assertEquals("HELLO-WORLD", Strings.convertWords("hello-world", "-", String::toUpperCase));
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
    public void testRemoveEnd_EdgeCases() {
        assertNull(Strings.removeEnd(null, "abc"));
        assertEquals("", Strings.removeEnd("", "abc"));
        assertEquals("abc", Strings.removeEnd("abcdef", "def"));
        assertEquals("abcdef", Strings.removeEnd("abcdef", "xyz"));
        assertEquals("abcdef", Strings.removeEnd("abcdef", null));
        assertEquals("abcdef", Strings.removeEnd("abcdef", ""));
    }

    @Test
    public void testRemoveStartIgnoreCase_EdgeCases() {
        assertNull(Strings.removeStartIgnoreCase(null, "abc"));
        assertEquals("def", Strings.removeStartIgnoreCase("ABCdef", "abc"));
        assertEquals("abcdef", Strings.removeStartIgnoreCase("abcdef", "xyz"));
    }

    @Test
    public void testRemoveEndIgnoreCase_EdgeCases() {
        assertNull(Strings.removeEndIgnoreCase(null, "def"));
        assertEquals("abc", Strings.removeEndIgnoreCase("abcDEF", "def"));
        assertEquals("abcdef", Strings.removeEndIgnoreCase("abcdef", "xyz"));
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
    public void testReplaceBetween_EdgeCases() {
        assertNull(Strings.replaceBetween(null, "a", "b", "x"));
        assertEquals("", Strings.replaceBetween("", "a", "b", "x"));
        assertEquals("axb", Strings.replaceBetween("acb", "a", "b", "x"));
    }

    @Test
    public void testReplaceAfter_EdgeCases() {
        assertNull(Strings.replaceAfter(null, " ", "x"));
        assertEquals("", Strings.replaceAfter("", " ", "x"));
        assertEquals("hello x", Strings.replaceAfter("hello world", " ", "x"));
    }

    @Test
    public void testReplaceBefore_EdgeCases() {
        assertNull(Strings.replaceBefore(null, " ", "x"));
        assertEquals("", Strings.replaceBefore("", " ", "x"));
        assertEquals("x world", Strings.replaceBefore("hello world", " ", "x"));
    }

    @Test
    public void testReplaceOnce_EdgeCases() {
        assertNull(Strings.replaceOnce(null, "a", "x"));
        assertEquals("", Strings.replaceOnce("", "a", "x"));
        assertEquals("xbcabc", Strings.replaceOnce("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceOnce("abc", null, "x"));
        assertEquals("abc", Strings.replaceOnce("abc", "", "x"));
        assertEquals("bc", Strings.replaceOnce("abc", "a", null));
    }

    @Test
    public void testReplaceLast_WithStartIndex() {
        assertEquals("xbcabc", Strings.replaceLast("abcabc", 0, "a", "x"));
    }

    @Test
    public void testIsAlpha_EdgeCases() {
        assertTrue(Strings.isAlpha("abc"));
        assertTrue(Strings.isAlpha("ABC"));
        assertFalse(Strings.isAlpha("abc123"));
        assertFalse(Strings.isAlpha("abc def"));
        assertFalse(Strings.isAlpha(null));
        assertFalse(Strings.isAlpha(""));
    }

    @Test
    public void testIsAlphaSpace_EdgeCases() {
        assertTrue(Strings.isAlphaSpace("abc"));
        assertTrue(Strings.isAlphaSpace("abc def"));
        assertFalse(Strings.isAlphaSpace("abc123"));
        assertFalse(Strings.isAlphaSpace(null));
        assertTrue(Strings.isAlphaSpace(""));
    }

    @Test
    public void testIsAlphanumeric_EdgeCases() {
        assertTrue(Strings.isAlphanumeric("abc123"));
        assertTrue(Strings.isAlphanumeric("abc"));
        assertTrue(Strings.isAlphanumeric("123"));
        assertFalse(Strings.isAlphanumeric("abc 123"));
        assertFalse(Strings.isAlphanumeric(null));
        assertFalse(Strings.isAlphanumeric(""));
    }

    @Test
    public void testIsAlphanumericSpace_EdgeCases() {
        assertTrue(Strings.isAlphanumericSpace("abc 123"));
        assertTrue(Strings.isAlphanumericSpace("abc123"));
        assertFalse(Strings.isAlphanumericSpace("abc@123"));
        assertFalse(Strings.isAlphanumericSpace(null));
        assertTrue(Strings.isAlphanumericSpace(""));
    }

    @Test
    public void testIsNumeric_EdgeCases() {
        assertTrue(Strings.isNumeric("123"));
        assertFalse(Strings.isNumeric("12.3"));
        assertFalse(Strings.isNumeric("-123"));
        assertFalse(Strings.isNumeric("abc"));
        assertFalse(Strings.isNumeric(null));
        assertFalse(Strings.isNumeric(""));
    }

    @Test
    public void testIsNumericSpace_EdgeCases() {
        assertTrue(Strings.isNumericSpace("123"));
        assertTrue(Strings.isNumericSpace("123 456"));
        assertFalse(Strings.isNumericSpace("12.3"));
        assertFalse(Strings.isNumericSpace(null));
        assertTrue(Strings.isNumericSpace(""));
    }

    @Test
    public void testIsWhitespace_EdgeCases() {
        assertTrue(Strings.isWhitespace("   "));
        assertTrue(Strings.isWhitespace("\t\n\r"));
        assertFalse(Strings.isWhitespace("abc"));
        assertFalse(Strings.isWhitespace(" abc "));
        assertFalse(Strings.isWhitespace(null));
        assertTrue(Strings.isWhitespace(""));
    }

    @Test
    public void testIsAllLowerCase_EdgeCases() {
        assertTrue(Strings.isAllLowerCase("abc"));
        assertFalse(Strings.isAllLowerCase("aBc"));
        assertFalse(Strings.isAllLowerCase("ABC"));
        assertTrue(Strings.isAllLowerCase(null));
        assertTrue(Strings.isAllLowerCase(""));
        assertFalse(Strings.isAllLowerCase("abc123"));
    }

    @Test
    public void testIsAllUpperCase_EdgeCases() {
        assertTrue(Strings.isAllUpperCase("ABC"));
        assertFalse(Strings.isAllUpperCase("AbC"));
        assertFalse(Strings.isAllUpperCase("abc"));
        assertTrue(Strings.isAllUpperCase(null));
        assertTrue(Strings.isAllUpperCase(""));
        assertFalse(Strings.isAllUpperCase("ABC123"));
    }

    @Test
    public void testContainsWhitespace_EdgeCases() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("abc\t"));
        assertTrue(Strings.containsWhitespace("abc\n"));
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace(""));
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    public void testCountMatches_CharEdgeCases() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(2, Strings.countMatches("abcabc", 'a'));
        assertEquals(0, Strings.countMatches("abc", 'x'));
    }

    @Test
    public void testCountMatches_StringEdgeCases() {
        assertEquals(0, Strings.countMatches(null, "a"));
        assertEquals(0, Strings.countMatches("", "a"));
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(0, Strings.countMatches("abc", "xyz"));
        assertEquals(0, Strings.countMatches("abc", null));
        assertEquals(0, Strings.countMatches("abc", ""));
    }

    @Test
    public void testOrdinalIndexOf_EdgeCases() {
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(1, Strings.ordinalIndexOf("aabaabaa", "a", 2));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(-1, Strings.ordinalIndexOf("aabaabaa", "a", 10));
        assertEquals(-1, Strings.ordinalIndexOf(null, "a", 1));
        assertEquals(-1, Strings.ordinalIndexOf("abc", null, 1));
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
    public void testSplit_CharEdgeCases() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ','));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", ','));
        assertArrayEquals(N.EMPTY_STRING_ARRAY, Strings.split(null, ','));
        assertArrayEquals(N.EMPTY_STRING_ARRAY, Strings.split("", ','));
    }

    @Test
    public void testSplit_CharWithTrimEdgeCases() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ',', true));
    }

    @Test
    public void testSplit_StringEdgeCases() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a::b::c", "::"));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", "::"));
        assertArrayEquals(N.EMPTY_STRING_ARRAY, Strings.split(null, "::"));
        assertArrayEquals(N.EMPTY_STRING_ARRAY, Strings.split("", "::"));
    }

    @Test
    public void testSplit_WithMaxEdgeCases() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ",", 5));
    }

    @Test
    public void testSplitPreserveAllTokens_CharEdgeCases() {
        String[] result = Strings.splitPreserveAllTokens("a,,b", ',');
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("", result[1]);
        assertEquals("b", result[2]);
    }

    @Test
    public void testSplitPreserveAllTokens_StringEdgeCases() {
        String[] result = Strings.splitPreserveAllTokens("a::::::b", "::");
        assertEquals(4, result.length);
        assertEquals("a", result[0]);
        assertEquals("", result[1]);
        assertEquals("", result[2]);
        assertEquals("b", result[3]);
    }

    @Test
    public void testTrim_EdgeCases() {
        assertNull(Strings.trim((String) null));
        assertEquals("", Strings.trim(""));
        assertEquals("", Strings.trim("   "));
        assertEquals("abc", Strings.trim("  abc  "));
        assertEquals("abc", Strings.trim("abc"));
    }

    @Test
    public void testTrimToNull_EdgeCases() {
        assertNull(Strings.trimToNull((String) null));
        assertNull(Strings.trimToNull(""));
        assertNull(Strings.trimToNull("   "));
        assertEquals("abc", Strings.trimToNull("  abc  "));
    }

    @Test
    public void testTrimToEmpty_EdgeCases() {
        assertEquals("", Strings.trimToEmpty((String) null));
        assertEquals("", Strings.trimToEmpty(""));
        assertEquals("", Strings.trimToEmpty("   "));
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
    }

    @Test
    public void testStrip_EdgeCases() {
        assertNull(Strings.strip((String) null));
        assertEquals("", Strings.strip(""));
        assertEquals("", Strings.strip("   "));
        assertEquals("abc", Strings.strip("  abc  "));
    }

    @Test
    public void testStripToNull_EdgeCases() {
        assertNull(Strings.stripToNull((String) null));
        assertNull(Strings.stripToNull(""));
        assertNull(Strings.stripToNull("   "));
        assertEquals("abc", Strings.stripToNull("  abc  "));
    }

    @Test
    public void testStripToEmpty_EdgeCases() {
        assertEquals("", Strings.stripToEmpty((String) null));
        assertEquals("", Strings.stripToEmpty(""));
        assertEquals("", Strings.stripToEmpty("   "));
        assertEquals("abc", Strings.stripToEmpty("  abc  "));
    }

    @Test
    public void testStrip_WithCharsEdgeCases() {
        assertEquals("abc", Strings.strip("xxabcxx", "x"));
        assertEquals("abc", Strings.strip("xyabcyx", "xy"));
        assertNull(Strings.strip((String) null, "x"));
    }

    @Test
    public void testStripStart_EdgeCases() {
        assertNull(Strings.stripStart(null));
        assertEquals("", Strings.stripStart(""));
        assertEquals("abc  ", Strings.stripStart("  abc  "));
        assertEquals("abc", Strings.stripStart("abc"));
    }

    @Test
    public void testStripStart_WithCharsEdgeCases() {
        assertEquals("abcxx", Strings.stripStart("xxabcxx", "x"));
        assertNull(Strings.stripStart((String) null, "x"));
    }

    @Test
    public void testStripEnd_EdgeCases() {
        assertNull(Strings.stripEnd(null));
        assertEquals("", Strings.stripEnd(""));
        assertEquals("  abc", Strings.stripEnd("  abc  "));
        assertEquals("abc", Strings.stripEnd("abc"));
    }

    @Test
    public void testCopyThenTrim_EdgeCases() {
        String[] strs = { "  abc  ", " xyz ", null };
        String[] result = Strings.copyThenTrim(strs);
        assertEquals("abc", result[0]);
        assertEquals("xyz", result[1]);
        assertNull(result[2]);
        // Ensure original is not modified
        assertEquals("  abc  ", strs[0]);
    }

    @Test
    public void testCopyThenStrip_EdgeCases() {
        String[] strs = { "  abc  ", " xyz ", null };
        String[] result = Strings.copyThenStrip(strs);
        assertEquals("abc", result[0]);
        assertEquals("xyz", result[1]);
        assertNull(result[2]);
        // Ensure original is not modified
        assertEquals("  abc  ", strs[0]);
    }

    @Test
    public void testExtractFirstInteger_EdgeCases() {
        assertEquals("123", Strings.extractFirstInteger("abc123def"));
        assertEquals("-123", Strings.extractFirstInteger("abc-123def"));
        assertNull(Strings.extractFirstInteger("abc"));
        assertNull(Strings.extractFirstInteger(null));
        assertNull(Strings.extractFirstInteger(""));
    }

    @Test
    public void testExtractFirstDouble_EdgeCases() {
        assertEquals("12.34", Strings.extractFirstDouble("abc12.34def"));
        assertEquals("-12.34", Strings.extractFirstDouble("abc-12.34def"));
        assertNull(Strings.extractFirstDouble("abc"));
        assertNull(Strings.extractFirstDouble(null));
        assertNull(Strings.extractFirstDouble(""));
    }

    @Test
    public void testReplaceFirstInteger_EdgeCases() {
        assertEquals("abcXYZdef", Strings.replaceFirstInteger("abc123def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstInteger("abc", "XYZ"));
        assertEquals("", Strings.replaceFirstInteger(null, "XYZ"));
    }

    @Test
    public void testReplaceFirstDouble_EdgeCases() {
        assertEquals("abcXYZdef", replaceFirstDouble("abc12.34def", "XYZ"));
        assertEquals("abc", replaceFirstDouble("abc", "XYZ"));
        assertEquals("", replaceFirstDouble(null, "XYZ"));
    }

    @Test
    public void testLenientFormat_EdgeCases() {
        assertEquals("hello world", Strings.lenientFormat("hello %s", "world"));
        assertEquals("hello world 123", Strings.lenientFormat("hello %s %s", "world", 123));
        assertEquals("hello", Strings.lenientFormat("hello"));
        assertEquals("null", Strings.lenientFormat(null));
    }

    @Test
    public void testUrlEncodeDecode_EdgeCases() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("hello world", "foo=bar");
        String encoded = Strings.urlEncode(params);
        assertNotNull(encoded);
        assertTrue(encoded.contains("hello"));
        // Round-trip test
        Map<String, String> decoded = Strings.urlDecode("a=1&b=2");
        assertEquals("1", decoded.get("a"));
        assertEquals("2", decoded.get("b"));
    }

    @Test
    public void testFindFirstEmailAddress_EdgeCases() {
        assertEquals("test@gmail.com", Strings.findFirstEmailAddress("contact test@gmail.com for info"));
        assertNull(Strings.findFirstEmailAddress("no email here"));
        assertNull(Strings.findFirstEmailAddress(null));
        assertNull(Strings.findFirstEmailAddress(""));
    }

    @Test
    public void testFindAllEmailAddresses_EdgeCases() {
        List<String> emails = Strings.findAllEmailAddresses("a@b.com and c@d.com");
        assertEquals(2, emails.size());
        assertEquals(0, Strings.findAllEmailAddresses("no email").size());
        assertEquals(0, Strings.findAllEmailAddresses(null).size());
    }

    @Test
    public void testIsBase64_StringEdgeCases() {
        assertTrue(isBase64("SGVsbG8=")); // "Hello"
        assertTrue(isBase64(base64EncodeString("test")));
        assertFalse(isBase64("not base64!!!"));
        assertFalse(isBase64((String) null));
    }

    @Test
    public void testBase64RoundTrip() {
        String original = "Hello, World! \u00E9\u00FC";
        byte[] encoded = base64Encode(original.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        String encodedStr = base64EncodeString(original);
        assertEquals(original, Strings.base64DecodeToString(encodedStr));
    }

    @Test
    public void testConcat_TwoStrings_EdgeCases() {
        assertEquals("ab", concat("a", "b"));
        assertEquals("a", concat("a", ""));
        assertEquals("b", concat("", "b"));
        assertEquals("b", concat(null, "b"));
    }

    @Test
    public void testConcat_ThreeStrings_EdgeCases() {
        assertEquals("abc", concat("a", "b", "c"));
    }

    @Test
    public void testConcat_Objects_EdgeCases() {
        assertEquals("123", Strings.concat((Object) 1, (Object) 2, (Object) 3));
        assertEquals("truenull", Strings.concat((Object) true, null));
    }

    @Test
    public void testReverse_EdgeCases() {
        assertNull(reverse(null));
        assertEquals("", reverse(""));
        assertEquals("cba", reverse("abc"));
        assertEquals("a", reverse("a"));
    }

    @Test
    public void testReverseDelimited_CharEdgeCases() {
        assertNull(reverseDelimited(null, ','));
        assertEquals("", reverseDelimited("", ','));
        assertEquals("c,b,a", reverseDelimited("a,b,c", ','));
        assertEquals("abc", reverseDelimited("abc", ','));
    }

    @Test
    public void testReverseDelimited_StringEdgeCases() {
        assertNull(reverseDelimited(null, "::"));
        assertEquals("", reverseDelimited("", "::"));
        assertEquals("c::b::a", reverseDelimited("a::b::c", "::"));
    }

    @Test
    public void testSort_EdgeCases() {
        assertNull(sort(null));
        assertEquals("", sort(""));
        assertEquals("abcde", sort("edcba"));
        assertEquals("aabbc", sort("abcba"));
        assertEquals("a", sort("a"));
    }

    @Test
    public void testShuffle_EdgeCases() {
        assertNull(shuffle(null));
        assertEquals("", shuffle(""));
        assertEquals(1, shuffle("a").length());
        String original = "abcdefghij";
        String shuffled = shuffle(original);
        assertEquals(original.length(), shuffled.length());
        // All chars should be present
        char[] origChars = original.toCharArray();
        char[] shuffChars = shuffled.toCharArray();
        java.util.Arrays.sort(origChars);
        java.util.Arrays.sort(shuffChars);
        assertArrayEquals(origChars, shuffChars);
    }

    @Test
    public void testRotate_EdgeCases() {
        assertNull(rotate(null, 1));
        assertEquals("", rotate("", 1));
        assertEquals("oHell", rotate("Hello", 1));
        assertEquals("elloH", rotate("Hello", -1));
        assertEquals("Hello", rotate("Hello", 0));
        assertEquals("Hello", rotate("Hello", 5));
    }

    @Test
    public void testSubstring_EdgeCases() {
        assertEquals("bc", substring("abc", 1));
        assertEquals("b", substring("abc", 1, 2));
        assertNull(substring(null, 1));
        assertEquals("", substring("abc", 3));
    }

    @Test
    public void testSubstringBefore_EdgeCases() {
        assertEquals("abc", substringBefore("abc.def", "."));
        assertNull(substringBefore(null, "."));
        assertNull(substringBefore("abc", null));
        assertNull(substringBefore("abc", "xyz"));
    }

    @Test
    public void testSubstringAfter_EdgeCases() {
        assertEquals("def", substringAfter("abc.def", "."));
        assertNull(substringAfter(null, "."));
        assertNull(substringAfter("abc", null));
        assertNull(substringAfter("abc", "xyz"));
    }

    @Test
    public void testSubstringBeforeLast_EdgeCases() {
        assertEquals("abc.def", substringBeforeLast("abc.def.ghi", "."));
        assertNull(substringBeforeLast(null, "."));
        assertNull(substringBeforeLast("abc", "xyz"));
    }

    @Test
    public void testSubstringAfterLast_EdgeCases() {
        assertEquals("ghi", substringAfterLast("abc.def.ghi", "."));
        assertNull(substringAfterLast(null, "."));
        assertNull(substringAfterLast("abc", "xyz"));
    }

    @Test
    public void testSubstringBetween_EdgeCases() {
        assertEquals("def", substringBetween("abc[def]ghi", "[", "]"));
        assertNull(substringBetween(null, "[", "]"));
        assertEquals("", substringBetween("ab", "a", "b"));
    }

    @Test
    public void testSubstringBetweenIgnoreCase_EdgeCases() {
        assertEquals("def", substringBetweenIgnoreCase("ABC[def]GHI", "[", "]"));
        assertNull(substringBetweenIgnoreCase(null, "a", "b"));
    }

    @Test
    public void testSubstringBeforeAny_EdgeCases() {
        assertEquals("abc", substringBeforeAny("abc.def,ghi", ".", ","));
        assertNull(substringBeforeAny(null, "."));
    }

    @Test
    public void testSubstringAfterAny_EdgeCases() {
        assertEquals("def,ghi", substringAfterAny("abc.def,ghi", "."));
        assertNull(substringAfterAny(null, "."));
    }

    @Test
    public void testJoin_IntArrayEdgeCases() {
        assertEquals("1, 2, 3", join(new int[] { 1, 2, 3 }));
        assertEquals("1|2|3", Strings.join(new int[] { 1, 2, 3 }, "|"));
        assertEquals("", Strings.join((int[]) null));
    }

    @Test
    public void testJoin_IterableEdgeCases() {
        assertEquals("a, b, c", join(Arrays.asList("a", "b", "c")));
        assertEquals("a|b|c", Strings.join(Arrays.asList("a", "b", "c"), "|"));
        assertEquals("", join((Iterable<?>) null));
    }

    @Test
    public void testJoin_IteratorEdgeCases() {
        assertEquals("a, b, c", join(Arrays.asList("a", "b", "c").iterator()));
        assertEquals("", join((Iterator<?>) null));
    }

    @Test
    public void testJoinEntries_EdgeCases() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        String result = joinEntries(map, ", ", "=");
        assertEquals("a=1, b=2", result);

        assertEquals("", joinEntries((Map<?, ?>) null, ", ", "="));
    }

    @Test
    public void testWrap_EdgeCases() {
        assertEquals("\"abc\"", Strings.wrap("abc", "\""));
        assertEquals("[abc]", Strings.wrap("abc", "[", "]"));
        assertEquals("\"\"", Strings.wrap(null, "\""));
        assertEquals("[]", Strings.wrap(null, "[", "]"));
    }

    @Test
    public void testUnwrap_EdgeCases() {
        assertEquals("abc", Strings.unwrap("\"abc\"", "\""));
        assertEquals("abc", Strings.unwrap("[abc]", "[", "]"));
        assertEquals("abc", Strings.unwrap("abc", "\""));
        assertNull(Strings.unwrap(null, "\""));
    }

    @Test
    public void testWrapIfMissing_EdgeCases() {
        assertEquals("\"abc\"", Strings.wrapIfMissing("abc", "\""));
        assertEquals("\"abc\"", Strings.wrapIfMissing("\"abc\"", "\""));
        assertEquals("[abc]", Strings.wrapIfMissing("abc", "[", "]"));
        assertEquals("[abc]", Strings.wrapIfMissing("[abc]", "[", "]"));
        assertEquals("\"\"", Strings.wrapIfMissing(null, "\""));
        assertEquals("[]", Strings.wrapIfMissing(null, "[", "]"));
    }

    @Test
    public void testIsLowerCase_EdgeCases() {
        assertTrue(Strings.isLowerCase('a'));
        assertFalse(Strings.isLowerCase('A'));
        assertFalse(Strings.isLowerCase('1'));
    }

    @Test
    public void testIsUpperCase_EdgeCases() {
        assertTrue(Strings.isUpperCase('A'));
        assertFalse(Strings.isUpperCase('a'));
        assertFalse(Strings.isUpperCase('1'));
    }

    @Test
    public void testIsAsciiLowerCase_EdgeCases() {
        assertTrue(Strings.isAsciiLowerCase('a'));
        assertTrue(Strings.isAsciiLowerCase('z'));
        assertFalse(Strings.isAsciiLowerCase('A'));
        assertFalse(Strings.isAsciiLowerCase('1'));
    }

    @Test
    public void testIsAsciiUpperCase_EdgeCases() {
        assertTrue(Strings.isAsciiUpperCase('A'));
        assertTrue(Strings.isAsciiUpperCase('Z'));
        assertFalse(Strings.isAsciiUpperCase('a'));
        assertFalse(Strings.isAsciiUpperCase('1'));
    }

    @Test
    public void testIsDigit_EdgeCases() {
        assertTrue(Strings.isDigit('0'));
        assertTrue(Strings.isDigit('9'));
        assertFalse(Strings.isDigit('a'));
        assertFalse(Strings.isDigit(' '));
    }

    @Test
    public void testIsLetter_EdgeCases() {
        assertTrue(Strings.isLetter('a'));
        assertTrue(Strings.isLetter('Z'));
        assertFalse(Strings.isLetter('1'));
        assertFalse(Strings.isLetter(' '));
    }

    @Test
    public void testIsLetterOrDigit_EdgeCases() {
        assertTrue(Strings.isLetterOrDigit('a'));
        assertTrue(Strings.isLetterOrDigit('1'));
        assertFalse(Strings.isLetterOrDigit(' '));
        assertFalse(Strings.isLetterOrDigit('@'));
    }

    @Test
    public void testIsAscii_EdgeCases() {
        assertTrue(Strings.isAscii('A'));
        assertTrue(Strings.isAscii('0'));
        assertTrue(Strings.isAscii(' '));
        assertFalse(Strings.isAscii('\u00E9'));
    }

    @Test
    public void testIsAsciiPrintable_CharEdgeCases() {
        assertTrue(Strings.isAsciiPrintable('A'));
        assertTrue(Strings.isAsciiPrintable(' '));
        assertFalse(Strings.isAsciiPrintable('\n'));
        assertFalse(Strings.isAsciiPrintable('\t'));
    }

    @Test
    public void testIsAsciiControl_EdgeCases() {
        assertTrue(Strings.isAsciiControl('\n'));
        assertTrue(Strings.isAsciiControl('\t'));
        assertTrue(Strings.isAsciiControl('\0'));
        assertFalse(Strings.isAsciiControl('A'));
    }

    @Test
    public void testIsAsciiAlpha_CharEdgeCases() {
        assertTrue(Strings.isAsciiAlpha('a'));
        assertTrue(Strings.isAsciiAlpha('Z'));
        assertFalse(Strings.isAsciiAlpha('1'));
        assertFalse(Strings.isAsciiAlpha('@'));
    }

    @Test
    public void testIsAsciiAlphanumeric_CharEdgeCases() {
        assertTrue(Strings.isAsciiAlphanumeric('a'));
        assertTrue(Strings.isAsciiAlphanumeric('Z'));
        assertTrue(Strings.isAsciiAlphanumeric('5'));
        assertFalse(Strings.isAsciiAlphanumeric('@'));
    }

    @Test
    public void testIsAsciiNumeric_CharEdgeCases() {
        assertTrue(Strings.isAsciiNumeric('0'));
        assertTrue(Strings.isAsciiNumeric('9'));
        assertFalse(Strings.isAsciiNumeric('a'));
        assertFalse(Strings.isAsciiNumeric(' '));
    }

    @Test
    public void testIsAsciiAlphaUpper_EdgeCases() {
        assertTrue(Strings.isAsciiAlphaUpper('A'));
        assertTrue(Strings.isAsciiAlphaUpper('Z'));
        assertFalse(Strings.isAsciiAlphaUpper('a'));
        assertFalse(Strings.isAsciiAlphaUpper('1'));
    }

    @Test
    public void testIsAsciiAlphaLower_EdgeCases() {
        assertTrue(Strings.isAsciiAlphaLower('a'));
        assertTrue(Strings.isAsciiAlphaLower('z'));
        assertFalse(Strings.isAsciiAlphaLower('A'));
        assertFalse(Strings.isAsciiAlphaLower('1'));
    }

    @Test
    public void testIsAsciiPrintable_StringEdgeCases() {
        assertTrue(Strings.isAsciiPrintable("abc 123"));
        assertFalse(Strings.isAsciiPrintable("abc\n"));
        assertFalse(Strings.isAsciiPrintable(null));
        assertTrue(Strings.isAsciiPrintable(""));
    }

    @Test
    public void testIsAsciiAlpha_StringEdgeCases() {
        assertTrue(Strings.isAsciiAlpha("abc"));
        assertTrue(Strings.isAsciiAlpha("ABC"));
        assertFalse(Strings.isAsciiAlpha("abc123"));
        assertFalse(Strings.isAsciiAlpha(null));
        assertFalse(Strings.isAsciiAlpha(""));
    }

    @Test
    public void testIsAsciiAlphaSpace_EdgeCases() {
        assertTrue(Strings.isAsciiAlphaSpace("abc def"));
        assertTrue(Strings.isAsciiAlphaSpace("abc"));
        assertFalse(Strings.isAsciiAlphaSpace("abc123"));
        assertFalse(Strings.isAsciiAlphaSpace(null));
        assertTrue(Strings.isAsciiAlphaSpace(""));
    }

    @Test
    public void testIsAsciiAlphanumeric_StringEdgeCases() {
        assertTrue(Strings.isAsciiAlphanumeric("abc123"));
        assertFalse(Strings.isAsciiAlphanumeric("abc 123"));
        assertFalse(Strings.isAsciiAlphanumeric(null));
        assertFalse(Strings.isAsciiAlphanumeric(""));
    }

    @Test
    public void testIsAsciiAlphanumericSpace_EdgeCases() {
        assertTrue(Strings.isAsciiAlphanumericSpace("abc 123"));
        assertFalse(Strings.isAsciiAlphanumericSpace("abc@123"));
        assertFalse(Strings.isAsciiAlphanumericSpace(null));
        assertTrue(Strings.isAsciiAlphanumericSpace(""));
    }

    @Test
    public void testIsAsciiNumeric_StringEdgeCases() {
        assertTrue(Strings.isAsciiNumeric("123"));
        assertFalse(Strings.isAsciiNumeric("12a3"));
        assertFalse(Strings.isAsciiNumeric(null));
        assertFalse(Strings.isAsciiNumeric(""));
    }

    @Test
    public void testEqualsAny_EdgeCases() {
        assertTrue(Strings.equalsAny("abc", "xyz", "abc", "def"));
        assertFalse(Strings.equalsAny("abc", "xyz", "def"));
        assertFalse(Strings.equalsAny(null, "abc"));
        assertFalse(Strings.equalsAny("abc"));
    }

    @Test
    public void testEqualsAnyIgnoreCase_EdgeCases() {
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "XYZ", "ABC", "DEF"));
        assertFalse(Strings.equalsAnyIgnoreCase("abc", "xyz", "def"));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "abc"));
    }

    @Test
    public void testContains_CharEdgeCases() {
        assertTrue(Strings.contains("abc", 'b'));
        assertFalse(Strings.contains("abc", 'x'));
        assertFalse(Strings.contains(null, 'a'));
        assertFalse(Strings.contains("", 'a'));
    }

    @Test
    public void testContains_StringEdgeCases() {
        assertTrue(Strings.contains("abc", "bc"));
        assertFalse(Strings.contains("abc", "xy"));
        assertFalse(Strings.contains(null, "a"));
        assertFalse(Strings.contains("abc", null));
    }

    @Test
    public void testContainsIgnoreCase_EdgeCases() {
        assertTrue(Strings.containsIgnoreCase("ABC", "abc"));
        assertTrue(Strings.containsIgnoreCase("abc", "BC"));
        assertFalse(Strings.containsIgnoreCase("abc", "xy"));
        assertFalse(Strings.containsIgnoreCase(null, "a"));
    }

    @Test
    public void testStartsWith_EdgeCases() {
        assertTrue(Strings.startsWith("abc", "ab"));
        assertTrue(Strings.startsWith("abc", ""));
        assertFalse(Strings.startsWith("abc", "bc"));
        assertFalse(Strings.startsWith(null, "ab"));
        assertFalse(Strings.startsWith("abc", null));
    }

    @Test
    public void testStartsWithIgnoreCase_EdgeCases() {
        assertTrue(Strings.startsWithIgnoreCase("ABC", "ab"));
        assertTrue(Strings.startsWithIgnoreCase("abc", "AB"));
        assertFalse(Strings.startsWithIgnoreCase("abc", "bc"));
        assertFalse(Strings.startsWithIgnoreCase(null, "ab"));
    }

    @Test
    public void testEndsWith_EdgeCases() {
        assertTrue(Strings.endsWith("abc", "bc"));
        assertTrue(Strings.endsWith("abc", ""));
        assertFalse(Strings.endsWith("abc", "ab"));
        assertFalse(Strings.endsWith(null, "bc"));
        assertFalse(Strings.endsWith("abc", null));
    }

    @Test
    public void testEndsWithIgnoreCase_EdgeCases() {
        assertTrue(Strings.endsWithIgnoreCase("ABC", "bc"));
        assertTrue(Strings.endsWithIgnoreCase("abc", "BC"));
        assertFalse(Strings.endsWithIgnoreCase("abc", "ab"));
        assertFalse(Strings.endsWithIgnoreCase(null, "bc"));
    }

    @Test
    public void testStartsWithAny_EdgeCases() {
        assertTrue(Strings.startsWithAny("abc", "xy", "ab", "cd"));
        assertFalse(Strings.startsWithAny("abc", "xy", "cd"));
        assertFalse(Strings.startsWithAny(null, "ab"));
    }

    @Test
    public void testStartsWithAnyIgnoreCase_EdgeCases() {
        assertTrue(Strings.startsWithAnyIgnoreCase("ABC", "xy", "ab"));
        assertFalse(Strings.startsWithAnyIgnoreCase("abc", "xy", "cd"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "ab"));
    }

    @Test
    public void testEndsWithAny_EdgeCases() {
        assertTrue(Strings.endsWithAny("abc", "xy", "bc", "cd"));
        assertFalse(Strings.endsWithAny("abc", "xy", "cd"));
        assertFalse(Strings.endsWithAny(null, "bc"));
    }

    @Test
    public void testEndsWithAnyIgnoreCase_EdgeCases() {
        assertTrue(Strings.endsWithAnyIgnoreCase("ABC", "xy", "bc"));
        assertFalse(Strings.endsWithAnyIgnoreCase("abc", "xy", "cd"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "bc"));
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
    public void testCommonPrefix_VarArgs_EdgeCases() {
        assertEquals("ab", Strings.commonPrefix("abc", "abd", "abe"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertEquals("", Strings.commonPrefix((CharSequence[]) null));
    }

    @Test
    public void testCommonSuffix_VarArgs_EdgeCases() {
        assertEquals("bc", Strings.commonSuffix("abc", "xbc", "ybc"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertEquals("", Strings.commonSuffix((CharSequence[]) null));
    }

    @Test
    public void testTrim_Array_EdgeCases() {
        String[] array = { "  abc  ", " xyz ", null };
        Strings.trim(array);
        assertEquals("abc", array[0]);
        assertEquals("xyz", array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testTrimToNull_Array_EdgeCases() {
        String[] array = { "  abc  ", "   ", null };
        Strings.trimToNull(array);
        assertEquals("abc", array[0]);
        assertNull(array[1]);
        assertNull(array[2]);
    }

    @Test
    public void testTrimToEmpty_Array_EdgeCases() {
        String[] array = { "  abc  ", null, "   " };
        Strings.trimToEmpty(array);
        assertEquals("abc", array[0]);
        assertEquals("", array[1]);
        assertEquals("", array[2]);
    }

    @Test
    public void testStrip_Array_EdgeCases() {
        String[] array = { "  abc  ", " xyz " };
        Strings.strip(array);
        assertEquals("abc", array[0]);
        assertEquals("xyz", array[1]);
    }

    @Test
    public void testStripToNull_Array_EdgeCases() {
        String[] array = { "  abc  ", "   " };
        Strings.stripToNull(array);
        assertEquals("abc", array[0]);
        assertNull(array[1]);
    }

    @Test
    public void testStripToEmpty_Array_EdgeCases() {
        String[] array = { "  abc  ", null };
        Strings.stripToEmpty(array);
        assertEquals("abc", array[0]);
        assertEquals("", array[1]);
    }

    @Test
    public void testStrip_WithChars_Array_EdgeCases() {
        String[] array = { "xxabcxx", "yydefyy" };
        Strings.strip(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    @Test
    public void testStripStart_Array_EdgeCases() {
        String[] array = { "xxabc", "yydef" };
        Strings.stripStart(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    @Test
    public void testStripEnd_Array_EdgeCases() {
        String[] array = { "abcxx", "defyy" };
        Strings.stripEnd(array, "xy");
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
    }

    @Test
    public void testStripAccents_Array_EdgeCases() {
        String[] array = { "\u00E9clair", "caf\u00E9" };
        Strings.stripAccents(array);
        assertEquals("eclair", array[0]);
        assertEquals("cafe", array[1]);
    }

    @Test
    public void testChomp_Array_EdgeCases() {
        String[] array = { "abc\n", "def\r\n", "ghi" };
        Strings.chomp(array);
        assertEquals("abc", array[0]);
        assertEquals("def", array[1]);
        assertEquals("ghi", array[2]);
    }

    @Test
    public void testChop_Array_EdgeCases() {
        String[] array = { "abc", "de", "f" };
        Strings.chop(array);
        assertEquals("ab", array[0]);
        assertEquals("d", array[1]);
        assertEquals("", array[2]);
    }

    @Test
    public void testIsAsciiNumber_ExponentForms() {
        assertTrue(Strings.isAsciiNumber("2e10"));
        assertTrue(Strings.isAsciiNumber("-3.14E+5"));
        assertFalse(Strings.isAsciiNumber("e10"));
        assertFalse(Strings.isAsciiNumber("12e"));
    }

    @Test
    public void testLastIndexOfIgnoreCase_DelimiterSkipsEmbeddedToken() {
        assertEquals(10, Strings.lastIndexOfIgnoreCase("pineapple,APPLE", "apple", ","));
        assertEquals(0, Strings.lastIndexOfIgnoreCase("apple,pineapple", "apple", ",", 8));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("pineapple", "apple", ","));
    }

    @Test
    public void testLastIndexOfAny_RejectsSurrogateInput() {
        assertThrows(IllegalArgumentException.class, () -> Strings.lastIndexOfAny("abc", '\uDC00', 'a'));
        assertThrows(IllegalArgumentException.class, () -> Strings.lastIndexOfAny("abc", 2, '\uDC00', 'a'));
    }

}
