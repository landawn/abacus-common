/*
 * Copyright (C) 2024 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Strings.ExtractStrategy;

public class StringsTest extends AbstractTest {

    @Test
    public void test_substringsBetween() {

        {
            // String:   3 [ a 2 [ c ] ] 2 [ a ]
            // Index:    0 1 2 3 4 5 6 7 8 9 10 11
            assertEquals("[\"a2[c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"c\", \"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"a2[c]\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            // String:   3 [ a 2 c ] ] 2 [ a ]
            // Index:    0 1 2 3 4 5 6 7 8 9 10
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"a2c\", \"a\"]", CommonUtil.stringOf(Strings.substringsBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            // String:   [ [ b [ a ] ] c ]
            // Index:    0 1 2 3 4 5 6 7 8
            assertEquals("[\"[b[a\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"a\", \"b[a]\", \"[b[a]]c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"[b[a]]c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            // String:   [ [ b [ a ] [ c ] d ]
            // Index:    0 1 2 3 4 5 6 7 8 9 10
            assertEquals("[\"[b[a\", \"c\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[\"a\", \"c\", \"b[a][c]d\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[\"b[a][c]d\"]", CommonUtil.stringOf(Strings.substringsBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

    }

    @Test
    public void test_substringIndicesBetween() {

        {
            // String:   3 [ a 2 [ c ] ] 2 [ a ]
            // Index:    0 1 2 3 4 5 6 7 8 9 10 11
            assertEquals("[[2, 6], [10, 11]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[5, 6], [2, 7], [10, 11]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[[2, 7], [10, 11]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            // String:   3 [ a 2 c ] ] 2 [ a ]
            // Index:    0 1 2 3 4 5 6 7 8 9 10
            assertEquals("[[2, 5], [9, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[2, 5], [9, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[[2, 5], [9, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            // String:   [ [ b [ a ] ] c ]
            // Index:    0 1 2 3 4 5 6 7 8
            assertEquals("[[1, 5]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[4, 5], [2, 6], [1, 8]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.STACK_BASED)));
            assertEquals("[[1, 8]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', ExtractStrategy.IGNORE_NESTED)));
        }

        {
            // String:   [ [ b [ a ] [ c ] d ]
            // Index:    0 1 2 3 4 5 6 7 8 9 10
            assertEquals("[[1, 5], [7, 8]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.DEFAULT)));
            assertEquals("[[4, 5], [7, 8], [2, 10]]", CommonUtil.stringOf(Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', ExtractStrategy.STACK_BASED)));
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
        String str = Strings.toLowerCaseWithUnderscore("ME_#A3C_AAA_1A2");
        N.println(str);

        assertEquals("me_#a3c_aaa_1a2", str);

        str = Strings.toUpperCaseWithUnderscore("me_#a3c_aaa_1a2");
        N.println(str);
        assertEquals("ME_#A3C_AAA_1A2", str);

        str = Strings.toCamelCase("me_#a3c_aaa_1a2");
        N.println(str);
        assertEquals("me#a3cAaa1a2", str);

        str = Strings.toCamelCase("ME_#A3C_AAA_1A2");
        N.println(str);
        assertEquals("me#a3cAaa1a2", str);

        assertEquals("xml_parser", Strings.toLowerCaseWithUnderscore("XMLParser"));
        assertEquals("xml_parser", Strings.toLowerCaseWithUnderscore("xmlPARSER"));
        assertEquals("io_error", Strings.toLowerCaseWithUnderscore("IOError"));
        assertEquals("io_error", Strings.toLowerCaseWithUnderscore("ioERROR"));
        assertEquals("hello_world_api", Strings.toLowerCaseWithUnderscore("helloWorldAPI"));

        assertEquals("XML_PARSER", Strings.toUpperCaseWithUnderscore("XMLParser"));
        assertEquals("XML_PARSER", Strings.toUpperCaseWithUnderscore("xmlPARSER"));
        assertEquals("IO_ERROR", Strings.toUpperCaseWithUnderscore("IOError"));
        assertEquals("IO_ERROR", Strings.toUpperCaseWithUnderscore("ioERROR"));
        assertEquals("HELLO_WORLD_API", Strings.toUpperCaseWithUnderscore("helloWorldAPI"));
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

    public int findLength2(final char[] a, final char[] b) {
        if (CommonUtil.isEmpty(a) || CommonUtil.isEmpty(b)) {
            return 0;
        }

        // Variable to store ending point of longest common subString in a.
        int end = 0;

        final int lenA = a.length;
        final int lenB = b.length;

        // dp[j] is the length of longest common subarray ending with A[i-1], B[j-1]
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
        //
        //        // Matrix to store result of two consecutive rows at a time.
        //        int len[][] = new int[2][lenA];
        //
        //        // Variable to represent which row of matrix is current row.
        //        int currRow = 0;
        //
        //        for (int i = 0; i < lenA; i++) {
        //            for (int j = 0; j < lenB; j++) {
        //                if (i == 0 || j == 0) {
        //                    len[currRow][j] = 0;
        //                } else if (a.charAt(i - 1) == b.charAt(j - 1)) {
        //                    len[currRow][j] = len[1 - currRow][j - 1] + 1;
        //
        //                    if (len[currRow][j] > result) {
        //                        result = len[currRow][j];
        //                        end = i - 1;
        //                    }
        //                } else {
        //                    len[currRow][j] = 0;
        //                }
        //            }
        //
        //            // Make current row as previous row and  previous row as new current row.
        //            currRow = 1 - currRow;
        //        }

        return maxLen;
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

        // assertEquals("", "abc".substring(3, 2));
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
        Splitter.with(",").splitToStream(" foo,,,  bar ,").println();
        Splitter.pattern(",").splitToStream(" foo,,,  bar ,").println();
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
        assertEquals(CommonUtil.asList("a2[c", "a"), Strings.substringsBetween("3[a2[c]]2[a]", '[', ']'));

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

    /**
     * Tests the substringsBetween method that returns a String Array of substrings.
     */
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

        // 'ab hello ba' will match, but 'ab non ba' won't
        // this is because the 'a' is shared between the two and can't be matched twice
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

    /**
     * Tests the substringsBetween method that returns a String Array of substrings.
     */
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

        // 'ab hello ba' will match, but 'ab non ba' won't
        // this is because the 'a' is shared between the two and can't be matched twice
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

}
