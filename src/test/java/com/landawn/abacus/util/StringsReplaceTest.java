package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.replaceFirstDouble;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class StringsReplaceTest extends StringsTestSupport {
    @Test
    public void testReplaceAll() {
        assertEquals("xbcxbc", Strings.replaceAll("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceAll("abc", "x", "y"));
        assertNull(Strings.replaceAll(null, "a", "x"));
    }

    @Test
    public void testReplaceAll_WithFromIndex() {
        assertEquals("abcxbc", Strings.replaceAll("abcabc", 3, "a", "x"));
        assertEquals("abcabc", Strings.replaceAll("abcabc", 0, "x", "y"));
    }

    @Test
    public void testReplaceAllWithFromIndex() {
        assertEquals("azbzz", Strings.replaceAll("aabaa", 1, "a", "z"));
    }

    @Test
    public void testReplaceFirst() {
        assertEquals("xbcabc", Strings.replaceFirst("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceFirst("abc", "x", "y"));
        assertNull(Strings.replaceFirst(null, "a", "x"));
        assertEquals("", Strings.replaceFirst("", "a", "x"));
        assertEquals("abc", Strings.replaceFirst("abc", null, "x"));
        assertEquals("abc", Strings.replaceFirst("abc", "", "x"));
        assertEquals("bc", Strings.replaceFirst("abc", "a", null));
        assertEquals("xyzcde", Strings.replaceFirst("abcde", "ab", "xyz"));
        assertEquals("abcxyz", Strings.replaceFirst("abcde", "de", "xyz"));
        assertEquals("xyz", Strings.replaceFirst("abc", "abc", "xyz"));
        assertEquals("xc", Strings.replaceFirst("abc", "ab", "x"));
    }

    @Test
    public void testReplaceFirst_WithFromIndex() {
        assertEquals("abcxbc", Strings.replaceFirst("abcabc", 3, "a", "x"));
        assertEquals("abcabc", Strings.replaceFirst("abcabc", 0, "x", "y"));
        assertEquals("abzde", Strings.replaceFirst("abcde", 0, "c", "z"));
        assertEquals("abcde", Strings.replaceFirst("abcde", 3, "c", "z"));
        assertEquals("zbcde", Strings.replaceFirst("abcde", -1, "a", "z"));
        assertEquals("abcde", Strings.replaceFirst("abcde", 10, "a", "z"));
    }

    @Test
    public void testReplaceFirstSciNumber() {
        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value1.23e10test", "XXX", true));
        assertEquals("testXXX", Strings.replaceFirstDouble("test4.56E-5", "XXX", true));
        assertEquals("XXXtest", Strings.replaceFirstDouble("1e3test", "XXX", true));

        assertEquals("valueXXXtest", Strings.replaceFirstDouble("value-1.23e10test", "XXX", true));

        assertEquals("no sci numbers here", Strings.replaceFirstDouble("no sci numbers here", "XXX", true));
        assertEquals("", Strings.replaceFirstDouble("", "XXX", true));
        assertNull(Strings.replaceFirstDouble(null, "XXX", true));
        assertEquals("XXX", Strings.replaceFirstDouble("123.45", "XXX", true));

        assertEquals("firstXXXsecond4.56e-5", Strings.replaceFirstDouble("first1.23e10second4.56e-5", "XXX", true));
    }

    @Test
    public void testReplaceLastWithStartIndex() {
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 1, "a", "z"));

    }

    @Test
    public void testReplaceLast_WithStartIndex() {
        assertEquals("xbcabc", Strings.replaceLast("abcabc", 0, "a", "x"));
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
    public void testReplaceLast() {
        assertEquals("abcxbc", Strings.replaceLast("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceLast("abc", "x", "y"));
        assertNull(Strings.replaceLast(null, "a", "x"));
    }

    @Test
    public void test_replace() {
        String str = "APPLE ORange         Water";
        assertEquals("APPLE aaa         Water", Strings.replace(str, 6, 12, "aaa"));

        str = "APPLE ORange         Water";
        assertEquals("APPLE aaa         Water", Strings.replaceBetween(str, " ", " ", "aaa"));
        assertEquals(Strings.replaceFirst(str, Strings.substringBetween(str, " ", " "), "aaa"), Strings.replaceBetween(str, " ", " ", "aaa"));

        str = "APPLE ORange         Water";
        assertEquals("aaa ORange         Water", Strings.replaceBefore(str, " ", "aaa"));
        assertEquals(Strings.replaceFirst(str, Strings.substringBefore(str, " "), "aaa"), Strings.replaceBefore(str, " ", "aaa"));

        str = "APPLE ORange         Water";
        assertEquals("APPLE aaa", Strings.replaceAfter(str, " ", "aaa"));
        assertEquals(Strings.replaceFirst(str, Strings.substringAfter(str, " "), "aaa"), Strings.replaceAfter(str, " ", "aaa"));

        assertEquals("bbb", Strings.replaceBetween("bbb", "", "", "aaa"));
        assertEquals("hello", Strings.replaceAfter("hello", "", "X"));
        assertEquals("hello", Strings.replaceBefore("hello", "", "X"));

    }

    @Test
    public void testReplaceWithShrinkingReplacementUsesValidInitialCapacity() {
        assertEquals("ac", Strings.replace("abc", 0, "b", "", -1));
        assertEquals("", Strings.replace("abc", 0, "abc", null, -1));
        assertEquals("AC", Strings.replaceIgnoreCase("AbC", 0, "b", "", -1));
    }

    @Test
    public void test_replaceByPattern() {
        {
            final String source = "sent email to xyz@gmail.com from 123@outlook.cn";

            String ret = RegExUtil.replaceAll(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), "xxx");
            assertEquals("sent email to xxx from xxx", ret);

            ret = RegExUtil.replaceAll(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), it -> "xxx");
            assertEquals("sent email to xxx from xxx", ret);

            ret = RegExUtil.replaceAll(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), (from, to) -> "xxxx");
            assertEquals("sent email to xxxx from xxxx", ret);
        }

        {
            final String source = "sent email to xyz@gmail.com from 123@outlook.cn";

            String ret = RegExUtil.replaceFirst(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), "xxx");
            assertEquals("sent email to xxx from 123@outlook.cn", ret);

            ret = RegExUtil.replaceFirst(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), it -> "xxx");
            assertEquals("sent email to xxx from 123@outlook.cn", ret);

            ret = RegExUtil.replaceFirst(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), (from, to) -> "xxxx");
            assertEquals("sent email to xxxx from 123@outlook.cn", ret);

        }
        {
            final String source = "sent email to xyz@gmail.com from 123@outlook.cn";

            String ret = RegExUtil.replaceLast(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), "xxx");
            assertEquals("sent email to xyz@gmail.com from xxx", ret);

            ret = RegExUtil.replaceLast(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), it -> "xxx");
            assertEquals("sent email to xyz@gmail.com from xxx", ret);

            ret = RegExUtil.replaceLast(source, RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern(), (from, to) -> "xxxx");
            assertEquals("sent email to xyz@gmail.com from xxxx", ret);
        }

        assertEquals(2, RegExUtil.countMatches("sent email to xyz@gmail.com from 123@outlook.cn", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern()));
        assertEquals(1, RegExUtil.countMatches("sent email to xyz@gmail.com from xxxx", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern()));
        assertEquals(0, RegExUtil.countMatches("sent email  from xxxx", RegExUtil.EMAIL_ADDRESS_RFC_5322_FINDER.pattern()));

    }

    @Test
    public void testReplaceBetween_EndDelimiterNotFound() {
        assertEquals("hello[world", Strings.replaceBetween("hello[world", "[", "]", "X"));
        assertEquals("start", Strings.replaceBetween("start", "<", ">", "replacement"));
    }

    @Test
    public void test_replace_01() {
        final String str = "ababaaa";
        assertEquals("xbxbxxx", Strings.replaceAll(str, "a", "x"));
        assertEquals("ababxxx", Strings.replaceAll(str, 3, "a", "x"));
        assertEquals("ababa", Strings.replace(str, 3, "a", "", 2));
    }

    @Test
    public void testReplace_WithMax() {
        assertEquals("xbcxbc", Strings.replace("abcabc", 0, "a", "x", 2));
        assertEquals("xbcabc", Strings.replace("abcabc", 0, "a", "x", 1));
        assertEquals("abcabc", Strings.replace("abcabc", 0, "a", "x", 0));
    }

    @Test
    public void testReplace_WithFromIndexAndMax() {
        assertEquals("hello XX XX", Strings.replace("hello ab ab", 0, "ab", "XX", 10));
        assertEquals("hello XX ab", Strings.replace("hello ab ab", 0, "ab", "XX", 1));
        assertNull(Strings.replace(null, 0, "a", "b", 1));
    }

    @Test
    public void testReplace_WithIndices() {
        assertEquals("abXYZfg", Strings.replace("abcdefg", 2, 5, "XYZ"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replace(null, 0, 1, "X"));
    }

    @Test
    public void testReplaceAllIgnoreCase() {
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceAllIgnoreCase(null, "a", "x"));
    }

    @Test
    public void testReplaceAllIgnoreCase_WithFromIndex() {
        assertEquals("hello XX XX", Strings.replaceAllIgnoreCase("hello ab AB", 0, "ab", "XX"));
        assertNull(Strings.replaceAllIgnoreCase(null, 0, "a", "b"));
    }

    @Test
    public void testReplaceFirstIgnoreCase() {
        assertEquals("xbcAbc", Strings.replaceFirstIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcabc", Strings.replaceFirstIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceFirstIgnoreCase(null, "a", "x"));
    }

    @Test
    public void testReplaceFirstIgnoreCase_WithFromIndex() {
        assertEquals("hello WORLD xyz", Strings.replaceFirstIgnoreCase("hello world xyz", 0, "WORLD", "WORLD"));
        assertEquals("hello WORLD xyz", Strings.replaceFirstIgnoreCase("hello world xyz", 0, "world", "WORLD"));
        assertNull(Strings.replaceFirstIgnoreCase(null, 0, "a", "b"));
    }

    @Test
    public void testReplaceIgnoreCase() {
        assertEquals("xyzxyz", Strings.replaceIgnoreCase("AbCABC", 0, "abc", "xyz", 2));
        assertNull(Strings.replaceIgnoreCase(null, 0, "abc", "xyz", 1));
    }

    @Test
    public void testReplaceIgnoreCase_WithMax() {
        assertEquals("XX ab AB", Strings.replaceIgnoreCase("ab ab AB", 0, "ab", "XX", 1));
        assertEquals("XX XX AB", Strings.replaceIgnoreCase("ab ab AB", 0, "ab", "XX", 2));
        assertNull(Strings.replaceIgnoreCase(null, 0, "a", "b", 1));
    }

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

    @Test
    public void testReplaceRange() {
        assertEquals("abXYZfg", Strings.replaceRange("abcdefg", 2, 5, "XYZ"));
        assertEquals("X", Strings.replaceRange("", 0, 0, "X"));
        assertNull(Strings.replaceRange(null, 0, 0, "X"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceRange(null, 0, 1, "X"));
    }

    @Test
    public void testReplaceFirstInteger_EdgeCases() {
        assertEquals("abcXYZdef", Strings.replaceFirstInteger("abc123def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstInteger("abc", "XYZ"));
        assertNull(Strings.replaceFirstInteger(null, "XYZ"));
        // R-6: replacement is literal text ('$' and '\' have no special regex meaning; "$5"/"$0" previously threw or were group refs)
        assertEquals("price $5", Strings.replaceFirstInteger("price 5", "$5"));
        assertEquals("a$0b", Strings.replaceFirstInteger("a5b", "$0"));
    }

    @Test
    public void testReplaceFirstDouble() {
        assertEquals("abcXYZdef", Strings.replaceFirstDouble("abc123.45def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstDouble("abc", "XYZ"));
        assertNull(Strings.replaceFirstDouble(null, "XYZ"));
        // R-6: replacement is literal text ('$' and '\' have no special regex meaning)
        assertEquals("price $9", Strings.replaceFirstDouble("price 9.5", "$9"));
    }

    @Test
    public void testReplaceFirstDouble_WithScientific() {
        assertEquals("value=[NUM]", replaceFirstDouble("value=1.23e4", "[NUM]", true));
        assertEquals("small=[NUM]", replaceFirstDouble("small=1.23E-4", "[NUM]", true));
        assertEquals("abc[NUM]def", replaceFirstDouble("abc123.45def", "[NUM]", true));
        assertEquals("no numbers", replaceFirstDouble("no numbers", "XXX", true));
        assertEquals("", replaceFirstDouble("", "XXX", true));
        assertNull(replaceFirstDouble(null, "XXX", true));
    }

    @Test
    public void testReplaceFirstDouble_EdgeCases() {
        assertEquals("abcXYZdef", replaceFirstDouble("abc12.34def", "XYZ"));
        assertEquals("X", replaceFirstDouble(".5", "X"));
        assertEquals("x=X", replaceFirstDouble("x=-.5", "X"));
        assertEquals("X", replaceFirstDouble(".5e2", "X", true));
        assertEquals("x=X", replaceFirstDouble("x=-.5e2", "X", true));
        assertEquals("abc", replaceFirstDouble("abc", "XYZ"));
        assertNull(replaceFirstDouble(null, "XYZ"));
    }

    @Test
    public void testReplace_EmptyTargetNoInfiniteLoop() {
        // Empty target should be a no-op (NOT every-position insert / infinite loop).
        assertEquals("abc", Strings.replaceAll("abc", "", "X"));
        assertEquals("abc", Strings.replaceFirst("abc", "", "X"));
    }

    @Test
    public void testReplaceIgnoreCase_lengthChangingCaseMapping() {
        // regression: lowercasing the whole string shifted match indices for U+0130 (which lowercases to
        // "i" + combining dot), causing IndexOutOfBoundsException or replacement at the wrong position
        assertEquals("İc", Strings.replaceAllIgnoreCase("İb", "b", "c"));
        assertEquals("İXa", Strings.replaceAllIgnoreCase("İaba", "ab", "X"));
        assertEquals("İstanbul X", Strings.replaceAllIgnoreCase("İstanbul city", "CITY", "X"));
        // unchanged for plain text
        assertEquals("xYx", Strings.replaceAllIgnoreCase("aYa", "A", "x"));
        assertEquals("xYa", Strings.replaceFirstIgnoreCase("aYa", "A", "x"));
    }

    @Test
    public void testReplaceLastIgnoreCase() {
        // normal / case-insensitive
        assertEquals("aBz", Strings.replaceLastIgnoreCase("aBa", "A", "z"));
        assertEquals("Hello Hi", Strings.replaceLastIgnoreCase("Hello HELLO", "hello", "Hi"));
        assertEquals("abz", Strings.replaceLastIgnoreCase("aba", "a", "z"));
        // not found
        assertEquals("any", Strings.replaceLastIgnoreCase("any", "xyz", "z"));
        // null/empty
        assertNull(Strings.replaceLastIgnoreCase(null, "a", "z"));
        assertEquals("", Strings.replaceLastIgnoreCase("", "a", "z"));
        assertEquals("any", Strings.replaceLastIgnoreCase("any", null, "z"));
        assertEquals("any", Strings.replaceLastIgnoreCase("any", "", "z"));
    }

    @Test
    public void testReplaceLastIgnoreCase_StartIndex() {
        // normal / case-insensitive with startIndexFromBack
        assertEquals("aBaCaDzE", Strings.replaceLastIgnoreCase("aBaCaDaE", 8, "a", "z"));
        assertEquals("aBaCzDaE", Strings.replaceLastIgnoreCase("aBaCaDaE", 5, "a", "z"));
        assertEquals("Hello HELLO Hi", Strings.replaceLastIgnoreCase("Hello HELLO hello", 16, "hello", "Hi"));
        // negative index -> unchanged
        assertEquals("aBaCaDaE", Strings.replaceLastIgnoreCase("aBaCaDaE", -1, "a", "z"));
        // null/empty
        assertNull(Strings.replaceLastIgnoreCase(null, 5, "a", "z"));
        assertEquals("", Strings.replaceLastIgnoreCase("", 0, "a", "z"));
        assertEquals("any", Strings.replaceLastIgnoreCase("any", 3, null, "z"));
    }
}
