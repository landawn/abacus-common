package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.findFirstDouble;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.replaceFirstDouble;
import static com.landawn.abacus.util.Strings.substringAfter;
import static com.landawn.abacus.util.Strings.substringBefore;
import static com.landawn.abacus.util.Strings.substringBetween;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Strings replace contracts, including regression cases. */
public class StringsReplaceTest extends StringsTestSupport {

    @Test
    public void testDeleteRange() {
        assertEquals("abfg", Strings.removeRange("abcdefg", 2, 5));
        assertEquals("abcdefg", Strings.removeRange("abcdefg", 2, 2));
        assertEquals("", Strings.removeRange("abc", 0, 3));
        assertEquals("", Strings.removeRange("", 0, 0));
        assertNull(Strings.removeRange(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange(null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange("abc", 3, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange("abc", 0, 10));
    }

    @Test
    public void testMethodCombinations() {
        String result = join(new String[] { "Hello", "World" }, " ");

        assertEquals("Hello World", result);

        String replaced = Strings.replaceAll("aaabbbccc", "b", "x");
        assertEquals(3, Strings.countMatches(replaced, 'x'));

        String[] parts = Strings.split("a,b,c", ",");
        assertEquals(3, parts.length);
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

    // removeAll

    @Test
    public void testC512_negativeIndexRulesOfCaseSensitiveRemoveAndReplace() {
        assertEquals("bcbc", Strings.removeAll("abcabc", -1, 'a'));
        assertEquals("bcbc", Strings.removeAll("abcabc", -1, "a"));
        assertEquals("zbz", Strings.replaceAll("aba", -1, "a", "z"));
        assertEquals("abcabc", Strings.replaceLast("abcabc", -1, "a", "z"));
        assertEquals("abczbc", Strings.replaceLast("abcabc", 3, "a", "z"));
    }

    @Test
    public void testRemoveAll_Char() {
        assertEquals("bc", Strings.removeAll("abc", 'a'));
        assertEquals("", Strings.removeAll("aaa", 'a'));
        assertEquals("abc", Strings.removeAll("abc", 'x'));
        assertNull(Strings.removeAll(null, 'a'));
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
    public void testRemoveAll_CharWithFromIndex() {
        assertEquals("abc", Strings.removeAll("abcaa", 3, 'a'));
        assertEquals("abc", Strings.removeAll("abc", 0, 'x'));
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
    public void testRemoveAll_String() {
        assertEquals("cde", Strings.removeAll("ababcde", "ab"));
        assertEquals("abc", Strings.removeAll("abc", "xy"));
        assertNull(Strings.removeAll(null, "ab"));
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
    public void testRemoveAll_StringWithFromIndex() {
        assertEquals("ababcde", Strings.removeAll("ababcde", 4, "ab"));
        assertEquals("abc", Strings.removeAll("abc", 0, "xy"));
    }

    @Test
    public void testRemoveAllChar() {
        assertEquals("qeed", Strings.removeAll("queued", 'u'));
        assertEquals("queued", Strings.removeAll("queued", 'z'));
    }

    @Test
    public void testC551_removeAllDelegatesGuardsToTheReplaceEngine() {
        assertEquals("acac", Strings.removeAll("abcabc", -5, "b"));
        assertEquals("acac", Strings.removeAll("abcabc", Integer.MIN_VALUE, "b"));
        assertEquals("abcac", Strings.removeAll("abcabc", 2, "b"));
        assertEquals("ACAC", Strings.removeAllIgnoreCase("ABCABC", -1, "b"));
        assertEquals("aBcAc", Strings.removeAllIgnoreCase("aBcABc", 3, "b"));

        final String str = "abc";
        assertSame(str, Strings.removeAll(str, 3, "c"));
        assertSame(str, Strings.removeAll(str, Integer.MAX_VALUE, "a"));
        assertSame(str, Strings.removeAll(str, 0, ""));
        assertSame(str, Strings.removeAll(str, 0, null));
        assertSame(str, Strings.removeAll(str, 0, "x"));
        assertSame(str, Strings.removeAllIgnoreCase(str, 3, "C"));
        assertSame(str, Strings.removeAllIgnoreCase(str, 0, null));
        assertNull(Strings.removeAll(null, 0, "a"));
        assertNull(Strings.removeAllIgnoreCase(null, -1, "a"));
        assertEquals("", Strings.removeAll("", 5, "x"));
        assertEquals("", Strings.removeAllIgnoreCase("", -5, "x"));

        // Unicode: a supplementary removeStr is removed as a whole pair
        assertEquals("ab", Strings.removeAll("a" + SMILE + "b" + SMILE, 0, SMILE));
    }

    @Test
    public void testRemoveAllString() {
        assertEquals("qd", Strings.removeAll("queued", "ue"));
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
    public void testRemoveRange() {
        assertEquals("hlo", Strings.removeRange("hello", 1, 3));
        assertEquals("hello", Strings.removeRange("hello", 0, 0));
        assertEquals("", Strings.removeRange("hello", 0, 5));
        assertNull(Strings.removeRange(null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange(null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.removeRange("hello", -1, 2));
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

    // replace

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
    public void test_replace_01() {
        final String str = "ababaaa";
        assertEquals("xbxbxxx", Strings.replaceAll(str, "a", "x"));
        assertEquals("ababxxx", Strings.replaceAll(str, 3, "a", "x"));
        assertEquals("ababa", Strings.replace(str, 3, "a", "", 2));
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
    public void testReplace_WithMax() {
        assertEquals("xbcxbc", Strings.replace("abcabc", 0, "a", "x", 2));
        assertEquals("xbcabc", Strings.replace("abcabc", 0, "a", "x", 1));
        assertEquals("abcabc", Strings.replace("abcabc", 0, "a", "x", 0));
    }

    @Test
    public void testReplaceWithShrinkingReplacementUsesValidInitialCapacity() {
        assertEquals("ac", Strings.replace("abc", 0, "b", "", -1));
        assertEquals("", Strings.replace("abc", 0, "abc", null, -1));
        assertEquals("AC", Strings.replaceIgnoreCase("AbC", 0, "b", "", -1));
    }

    @Test
    public void testReplace_EmptyTargetNoInfiniteLoop() {
        // Empty target should be a no-op (NOT every-position insert / infinite loop).
        assertEquals("abc", Strings.replaceAll("abc", "", "X"));
        assertEquals("abc", Strings.replaceFirst("abc", "", "X"));
    }

    // replaceAll

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

    // replaceFirst

    @Test
    public void testFindFirstNumberAgreesWithReplaceFirstNumber() {
        // The finders read the whole match while the replacers use Matcher.replaceFirst, which also replaces the
        // whole match. Pinning the agreement stops the two from drifting if a *_FINDER pattern ever grows
        // anything outside its capturing group.
        final String[] inputs = { "Room 404, Floor 2", "Temperature: -5 degrees", "Price: $12.99 (was $15.99)", "x=-.5", ".5", "1.", "Result: 1.23e10 units",
                "no digits here", "+7 and -8", "007" };

        for (final String input : inputs) {
            final String foundInt = Strings.findFirstInteger(input);

            if (foundInt == null) {
                assertEquals(input, Strings.replaceFirstInteger(input, "X"), input);
            } else {
                assertEquals(Strings.replaceFirstInteger(input, "X"), Strings.replaceFirst(input, foundInt, "X"), input);
                assertEquals(input.length() - foundInt.length(), Strings.replaceFirstInteger(input, "").length(), input);
            }

            for (final boolean sci : new boolean[] { false, true }) {
                final String foundNum = Strings.findFirstDouble(input, sci);

                if (foundNum == null) {
                    assertEquals(input, Strings.replaceFirstDouble(input, "X", sci), input);
                } else {
                    assertEquals(input.length() - foundNum.length(), Strings.replaceFirstDouble(input, "", sci).length(), input + "/" + sci);
                }
            }
        }
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
    public void testReplaceFirstInteger_EdgeCases() {
        assertEquals("abcXYZdef", Strings.replaceFirstInteger("abc123def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstInteger("abc", "XYZ"));
        assertNull(Strings.replaceFirstInteger(null, "XYZ"));
        // R-6: replacement is literal text ('$' and '\' have no special regex meaning; "$5"/"$0" previously threw or were group refs)
        assertEquals("price $5", Strings.replaceFirstInteger("price 5", "$5"));
        assertEquals("a$0b", Strings.replaceFirstInteger("a5b", "$0"));
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

    // replaceLast

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
    public void testReplaceLast_WithStartIndex() {
        assertEquals("xbcabc", Strings.replaceLast("abcabc", 0, "a", "x"));
    }

    @Test
    public void testReplaceLastWithStartIndex() {
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 2, "a", "z"));
        assertEquals("azbaa", Strings.replaceLast("aabaa", 1, "a", "z"));

    }

    @Test
    public void testReplaceRange() {
        assertEquals("abXYZfg", Strings.replaceRange("abcdefg", 2, 5, "XYZ"));
        assertEquals("X", Strings.replaceRange("", 0, 0, "X"));
        assertNull(Strings.replaceRange(null, 0, 0, "X"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceRange(null, 0, 1, "X"));
    }

}
