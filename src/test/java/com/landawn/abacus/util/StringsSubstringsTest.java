package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.findFirstDouble;
import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.reverse;
import static com.landawn.abacus.util.Strings.reverseDelimited;
import static com.landawn.abacus.util.Strings.rotate;
import static com.landawn.abacus.util.Strings.shuffle;
import static com.landawn.abacus.util.Strings.sort;
import static com.landawn.abacus.util.Strings.substringBetween;
import static com.landawn.abacus.util.Strings.substringsBetween;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.DelimiterMatchMode;
import com.landawn.abacus.util.Strings.StrUtil;

public class StringsSubstringsTest extends StringsTestSupport {
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
    public void testSubstringsBetweenIgnoreNested() {
        assertEquals(list("a2[c]", "a"), substringsBetween_IgnoreNested_("3[a2[c]]2[a]", '[', ']'));
        assertEquals(list("[b[a]]c"), substringsBetween_IgnoreNested_("[[b[a]]c]", '[', ']'));
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
    public void testSubstringsBetween_NestedBracketsDefaultMode() {
        assertEquals(CommonUtil.toList("a2[c", "a"), Strings.substringsBetween("3[a2[c]]2[a]", '[', ']'));
    }

    @Test
    public void testMoveRange() {
        assertEquals("cdeabfg", Strings.moveRange("abcdefg", 2, 5, 0));
        assertEquals("abfgcde", Strings.moveRange("abcdefg", 2, 5, 4));
        assertEquals("abcdefg", Strings.moveRange("abcdefg", 2, 5, 2));
        assertEquals("abcdefg", Strings.moveRange("abcdefg", 3, 3, 0));
        assertNull(Strings.moveRange(null, 0, 0, 0));
        assertEquals("", Strings.moveRange("", 0, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 1, 2));
    }

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
    public void testLenientFormat_ExtraArgs() {
        String result = Strings.lenientFormat("hello %s", "world", "extra");
        assertTrue(result.contains("world"));
        assertTrue(result.contains("extra"));
    }

    @Test
    public void testLenientFormat_FewerArgs() {
        String result = Strings.lenientFormat("hello %s %s", "world");
        assertTrue(result.contains("world"));
        assertTrue(result.contains("%s"));
    }

    @Test
    public void testLenientFormat_NoArgs() {
        assertEquals("hello", Strings.lenientFormat("hello"));
    }

    @Test
    public void testLenientFormat() {
        assertEquals("Hello World", Strings.lenientFormat("Hello %s", "World"));
        assertEquals("Value: 123", Strings.lenientFormat("Value: %s", 123));
        assertNotNull(Strings.lenientFormat("Test %s %s", "a"));
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
    public void testLenientFormat_EdgeCases() {
        assertEquals("hello world", Strings.lenientFormat("hello %s", "world"));
        assertEquals("hello world 123", Strings.lenientFormat("hello %s %s", "world", 123));
        assertEquals("hello", Strings.lenientFormat("hello"));
        assertEquals("null", Strings.lenientFormat(null));
    }

    @Test
    public void testLenientFormat_ConvertsArgumentsOnceInOrderWithoutMutatingCallerArray() {
        final List<Integer> conversions = new ArrayList<>();
        final Object first = new Object() {
            @Override
            public String toString() {
                conversions.add(1);
                return "first";
            }
        };
        final Object second = new Object() {
            @Override
            public String toString() {
                conversions.add(2);
                return "second";
            }
        };
        final Object third = new Object() {
            @Override
            public String toString() {
                conversions.add(3);
                return "third";
            }
        };
        final Object[] args = { first, second, third };

        assertEquals("first second: [third]", Strings.lenientFormat("%s %s", args));
        assertEquals(List.of(1, 2, 3), conversions);
        assertTrue(args[0] == first);
        assertTrue(args[1] == second);
        assertTrue(args[2] == third);
    }

    @Test
    public void testLenientFormat_NullTemplate() {
        String result = Strings.lenientFormat(null, "a", "b");
        assertEquals("null: [a, b]", result);
    }

    @Test
    public void testReverse() {
        assertEquals("cba", Strings.reverse("abc"));
        assertEquals("", Strings.reverse(""));
        assertNull(Strings.reverse(null));
    }

    @Test
    public void testReverse_EdgeCases() {
        assertNull(reverse(null));
        assertEquals("", reverse(""));
        assertEquals("cba", reverse("abc"));
        assertEquals("a", reverse("a"));
    }

    @Test
    public void testReverseDelimited() {
        assertEquals("c,b,a", Strings.reverseDelimited("a,b,c", ','));
        assertNull(Strings.reverseDelimited(null, ','));
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

        assertEquals("b..a", Strings.reverseDelimited("a..b", '.'));
        assertEquals(".b.a.", Strings.reverseDelimited(".a.b.", '.'));
        assertEquals("b.a.", Strings.reverseDelimited(".a.b", '.'));
        assertEquals(".b.a", Strings.reverseDelimited("a.b.", '.'));
        assertEquals("..", Strings.reverseDelimited("..", '.'));

        final String withEmptySegments = ".a..b.";
        assertEquals(withEmptySegments, Strings.reverseDelimited(Strings.reverseDelimited(withEmptySegments, '.'), '.'));
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

        assertEquals("b..a", Strings.reverseDelimited("a..b", "."));
        assertEquals("::b::a::", Strings.reverseDelimited("::a::b::", "::"));
        assertEquals("b::::a", Strings.reverseDelimited("a::::b", "::"));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a b", (String) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.reverseDelimited("a b", ""));

        final String withEmptySegments = "::a::::b::";
        assertEquals(withEmptySegments, Strings.reverseDelimited(Strings.reverseDelimited(withEmptySegments, "::"), "::"));
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
    public void testReverseDelimited_StringDelimiter() {
        assertNull(Strings.reverseDelimited(null, "."));
        assertEquals("", Strings.reverseDelimited("", "."));
        assertEquals("c.b.a", Strings.reverseDelimited("a.b.c", "."));
        assertEquals("abc", Strings.reverseDelimited("abc", "."));
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
    public void testSort() {
        assertEquals("abc", Strings.sort("cba"));
        assertEquals("", Strings.sort(""));
        assertNull(Strings.sort(null));
    }

    @Test
    public void testSort_String() {
        assertNull(Strings.sort(null));
        assertEquals("", Strings.sort(""));
        assertEquals("abcde", Strings.sort("edcba"));
        assertEquals("aabbc", Strings.sort("abcba"));
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
    public void testSort_SupplementaryCharactersPreserved() {
        final String grinningFace = new String(Character.toChars(0x1F600));
        final String rocket = new String(Character.toChars(0x1F680));

        assertEquals("a" + grinningFace + rocket, sort(rocket + "a" + grinningFace));
        assertEquals(grinningFace, sort(grinningFace));
    }

    @Test
    public void testRotate() {
        assertEquals("cab", Strings.rotate("abc", 1));
        assertEquals("bca", Strings.rotate("abc", -1));
        assertEquals("abc", Strings.rotate("abc", 3));
        assertNull(Strings.rotate(null, 1));
    }

    @Test
    public void testRotate_String() {
        assertNull(Strings.rotate(null, 1));
        assertEquals("", Strings.rotate("", 1));
        assertEquals("oHell", Strings.rotate("Hello", 1));
        assertEquals("elloH", Strings.rotate("Hello", -1));
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
    public void testRotate_SupplementaryCharactersPreserved() {
        final String emoji = new String(Character.toChars(0x1F600));
        final String input = "a" + emoji + "b";

        assertEquals("ba" + emoji, rotate(input, 1));
        assertEquals(emoji + "b" + "a", rotate(input, -1));
        assertEquals(emoji + "b" + "a", rotate(input, 2));
        assertEquals(input, rotate(input, 3));
        assertEquals(input, rotate(input, -3));
        assertEquals("ba" + emoji, rotate(input, Integer.MIN_VALUE));
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
    public void testShuffle_SupplementaryCharactersPreserved() {
        final String emoji = new String(Character.toChars(0x1F600));
        final String input = "a" + emoji;
        final Random splitPairIfCodeUnitBased = new Random(0) {
            @Override
            public int nextInt(final int bound) {
                return bound > 1 ? 1 : 0;
            }
        };

        assertEquals(input, shuffle(input, splitPairIfCodeUnitBased));
        assertEquals(emoji, shuffle(emoji, null));
        assertThrows(IllegalArgumentException.class, () -> shuffle("ab", null));
    }

    @Test
    public void testShuffle() {
        String shuffled = Strings.shuffle("abcdef");
        assertNotNull(shuffled);
        assertEquals(6, shuffled.length());
        assertNull(Strings.shuffle(null));
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
    public void testUrlEncodeDecode() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("name", "John Doe");
        params.put("city", "New York");
        String encoded = Strings.encodeUrlQuery(params);
        assertEquals("name=John+Doe&city=New+York", encoded);

        final Map<String, String> snakeKeys = new LinkedHashMap<>();
        snakeKeys.put("first_name", "Ada");
        assertEquals("first_name=Ada", Strings.encodeUrlQuery(snakeKeys));
        assertEquals("first_name=Ada", Strings.encodeUrlQuery(snakeKeys, java.nio.charset.StandardCharsets.UTF_8));

        Map<String, String> decoded = Strings.parseUrlQuery(encoded);
        assertEquals("John Doe", decoded.get("name"));
        assertEquals("New York", decoded.get("city"));
    }

    @Test
    public void testUrlEncode() {
        String encoded = Strings.encodeUrlQuery("Hello World");
        assertEquals("Hello+World", encoded);
        String encoded2 = Strings.encodeUrlQuery("a=b&c=d");
        assertEquals("a=b&c=d", encoded2);
        assertEquals("", Strings.encodeUrlQuery(null));
    }

    @Test
    public void testUrlEncode_WithCharset() {
        String encoded = Strings.encodeUrlQuery("hello world", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(encoded);
        assertTrue(encoded.contains("hello"));
        assertEquals("hello+world", Strings.encodeUrlQuery("hello world", null));
        assertEquals("", Strings.encodeUrlQuery(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(new Object[] { "name" }, null));

        final Map<Object, Object> nonStringKey = new LinkedHashMap<>();
        nonStringKey.put(1, "value");
        assertThrows(ClassCastException.class, () -> Strings.encodeUrlQuery(nonStringKey));
        assertThrows(ClassCastException.class, () -> Strings.encodeUrlQuery(new Object[] { 1, "value" }));

        final Map<Object, Object> nullKey = new LinkedHashMap<>();
        nullKey.put(null, "value");
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(nullKey));
        assertThrows(IllegalArgumentException.class, () -> Strings.encodeUrlQuery(new Object[] { null, "value" }));

        final Map<String, Object> nullValue = new LinkedHashMap<>();
        nullValue.put("a", null);
        assertEquals("a", Strings.encodeUrlQuery(nullValue));
        assertNull(Strings.parseUrlQuery(Strings.encodeUrlQuery(nullValue)).get("a"));
        assertEquals("a", Strings.encodeUrlQuery(new Object[] { "a", null }));

        // A "null" String value is distinct from a null value and keeps its '='.
        final Map<String, Object> nullStringValue = new LinkedHashMap<>();
        nullStringValue.put("a", "null");
        assertEquals("a=null", Strings.encodeUrlQuery(nullStringValue));
        assertEquals("null", Strings.parseUrlQuery(Strings.encodeUrlQuery(nullStringValue)).get("a"));

        // An empty String value encodes as "a=" and round-trips to "".
        final Map<String, Object> emptyValue = new LinkedHashMap<>();
        emptyValue.put("a", "");
        assertEquals("a=", Strings.encodeUrlQuery(emptyValue));
        assertEquals("", Strings.parseUrlQuery(Strings.encodeUrlQuery(emptyValue)).get("a"));
    }

    @Test
    public void testUrlEncodeDecode_EdgeCases() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("hello world", "foo=bar");
        String encoded = Strings.encodeUrlQuery(params);
        assertNotNull(encoded);
        assertTrue(encoded.contains("hello"));
        // Round-trip test
        Map<String, String> decoded = Strings.parseUrlQuery("a=1&b=2");
        assertEquals("1", decoded.get("a"));
        assertEquals("2", decoded.get("b"));
    }

    @Test
    public void testUrlDecode() {
        Map<String, String> decoded = Strings.parseUrlQuery(Strings.encodeUrlQuery("a=b&c=d"));
        assertEquals(Map.of("a", "b", "c", "d"), decoded);
        assertEquals(Map.of(), Strings.parseUrlQuery(null));

        // A String containing '=' is an ALREADY-ENCODED query and is appended verbatim, so the legacy ';'
        // separator is preserved rather than normalized to '&'. Re-splitting and re-encoding such a string
        // escaped its existing escapes a second time ("q=a%20b" -> "q=a%2520b"), which is why the verbatim
        // rule exists; the second assertion pins that. parseUrlQuery accepts both separators, so the
        // round-trip on the last line still holds.
        assertEquals("a=1;b=2", Strings.encodeUrlQuery("a=1;b=2"));
        assertEquals("q=a%20b", Strings.encodeUrlQuery("q=a%20b"));
        assertEquals(Map.of("a", "1", "b", "2"), Strings.parseUrlQuery("a=1;b=2"));
        assertEquals(Map.of("a", "1", "b", "2"), Strings.parseUrlQuery(Strings.encodeUrlQuery("a=1;b=2")));

        assertEquals("a=1&flag", Strings.encodeUrlQuery("a=1&flag"));
        final Map<String, String> flagged = Strings.parseUrlQuery("a=1&flag");
        assertEquals("1", flagged.get("a"));
        assertNull(flagged.get("flag"));
        assertEquals("a=1&flag", Strings.encodeUrlQuery("a=1&flag"));
        assertEquals(flagged, Strings.parseUrlQuery(Strings.encodeUrlQuery("a=1&flag")));
    }

    @Test
    public void testUrlDecode_WithCharset() {
        Map<String, String> result = Strings.parseUrlQuery("name=test+data", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals("test data", result.get("name"));
        assertEquals("é", Strings.parseUrlQuery("name=%C3%A9", (Charset) null).get("name"));
        assertEquals(Map.of(), Strings.parseUrlQuery((String) null, (Charset) null));
    }

    @Test
    public void testUrlDecode_StructuralContract() {
        final Map<String, String> result = Strings.parseUrlQuery("a=1;a=2&flag&&trim = value ");

        assertEquals("2", result.get("a"));
        assertTrue(result.containsKey("flag"));
        assertNull(result.get("flag"));
        assertEquals(" value ", result.get("trim "));
        assertEquals(3, result.size());

        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("bad=%ZZ"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("incomplete=%A"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("unicodeHex=%１２"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("invalidUtf8=%C3"));

        final Map<String, String> lenient = Strings.parseUrlQueryLenient("bad=%ZZ&incomplete=%A&unicodeHex=%１２&invalidUtf8=%C3");
        assertEquals("%ZZ", lenient.get("bad"));
        assertEquals("%A", lenient.get("incomplete"));
        assertEquals("%１２", lenient.get("unicodeHex"));
        assertEquals("�", lenient.get("invalidUtf8"));
    }

    @Test
    public void testUrlDecode_MultimapPreservesRepeatedParameters() {
        final ListMultimap<String, String> result = Strings.parseUrlQueryToMultimap("tag=java&tag=url&flag");

        assertEquals(List.of("java", "url"), result.get("tag"));
        assertEquals(1, result.get("flag").size());
        assertNull(result.get("flag").get(0));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQueryToMultimap("tag=%C3"));
        assertEquals("�", Strings.parseUrlQueryToMultimapLenient("tag=%C3").get("tag").get(0));
        assertEquals(List.of("java", "url"), Strings.parseUrlQueryToMultimapLenient("tag=java&tag=url").get("tag"));
        assertEquals("�", Strings.parseUrlQueryToMultimapLenient("tag=%C3", StandardCharsets.UTF_8).get("tag").get(0));
    }

    @Test
    public void testUrlDecode_WithClass() {
        java.util.Map<String, String> result = Strings.parseUrlQuery("a=1&b=2", java.util.Map.class);
        assertNotNull(result);
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
    }

    @Test
    public void testUrlDecode_WithClass_Null() {
        java.util.Map<String, String> result = Strings.parseUrlQuery((String) null, java.util.Map.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery((String) null, (Class<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("", String.class));
    }

    @Test
    public void testUrlDecode_WithCharsetAndClass() {
        java.util.Map<String, String> result = Strings.parseUrlQuery("a=1&b=2", java.nio.charset.StandardCharsets.UTF_8, java.util.Map.class);
        assertNotNull(result);
        assertEquals("1", result.get("a"));
    }

    @Test
    public void testUrlDecode_WithCharsetAndClass_Null() {
        java.util.Map<String, String> result = Strings.parseUrlQuery((String) null, java.nio.charset.StandardCharsets.UTF_8, java.util.Map.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(Map.of("name", "é"), Strings.parseUrlQuery("name=%C3%A9", null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery((String) null, null, (Class<Object>) null));
    }

    @Test
    public void testFindFirstEmailAddress() {
        assertEquals("test@example.com", Strings.findFirstEmailAddress("Contact: test@example.com"));
        assertNull(Strings.findFirstEmailAddress("No email here"));
        assertNull(Strings.findFirstEmailAddress(null));
    }

    @Test
    public void testFindFirstEmailAddress_EdgeCases() {
        assertEquals("test@gmail.com", Strings.findFirstEmailAddress("contact test@gmail.com for info"));
        assertNull(Strings.findFirstEmailAddress("no email here"));
        assertNull(Strings.findFirstEmailAddress(null));
        assertNull(Strings.findFirstEmailAddress(""));
    }

    @Test
    public void testFindAllEmailAddresses() {
        List<String> emails = Strings.findAllEmailAddresses("Contact: test@example.com and admin@test.org");
        assertEquals(2, emails.size());
        assertTrue(emails.contains("test@example.com"));
        assertTrue(emails.contains("admin@test.org"));
    }

    @Test
    public void testFindAllEmailAddresses_EdgeCases() {
        List<String> emails = Strings.findAllEmailAddresses("a@b.com and c@d.com");
        assertEquals(2, emails.size());
        assertEquals(0, Strings.findAllEmailAddresses("no email").size());
        assertEquals(0, Strings.findAllEmailAddresses(null).size());
    }

    @Test
    public void testCopyThenTrim() {
        String[] result = Strings.copyThenTrim(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenTrim((String[]) null));
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
        assertArrayEquals(new String[] { "x", "\u2000x\u2000", "\u00A0x\u00A0" },
                Strings.copyThenTrim(new String[] { "\u0001x\u001F", "\u2000x\u2000", "\u00A0x\u00A0" }));
    }

    @Test
    public void testCopyThenStrip() {
        String[] result = Strings.copyThenStrip(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenStrip((String[]) null));
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
        assertArrayEquals(new String[] { "\u0001x\u0001", "x", "\u00A0x\u00A0" },
                Strings.copyThenStrip(new String[] { "\u0001x\u0001", "\u2000x\u2000", "\u00A0x\u00A0" }));
    }

    @Test
    public void testFindFirstInteger() {
        assertEquals("123", Strings.findFirstInteger("abc123def"));
        assertNull(Strings.findFirstInteger("abc"));
        assertNull(Strings.findFirstInteger(null));
    }

    @Test
    public void testFindFirstInteger_EdgeCases() {
        assertEquals("123", Strings.findFirstInteger("abc123def"));
        assertEquals("-123", Strings.findFirstInteger("abc-123def"));
        assertNull(Strings.findFirstInteger("abc"));
        assertNull(Strings.findFirstInteger(null));
        assertNull(Strings.findFirstInteger(""));
    }

    @Test
    public void testFindFirstDouble() {
        assertEquals("123.45", Strings.findFirstDouble("abc123.45def"));
        assertNull(Strings.findFirstDouble("abc"));
        assertNull(Strings.findFirstDouble(null));
    }

    @Test
    public void testFindFirstSciNumber() {
        assertEquals("1.23e10", Strings.findFirstDouble("value1.23e10test", true));
        assertEquals("4.56E-5", Strings.findFirstDouble("test4.56E-5", true));
        assertEquals("1e3", Strings.findFirstDouble("1e3test", true));

        assertEquals("-1.23e10", Strings.findFirstDouble("value-1.23e10test", true));

        assertNull(Strings.findFirstDouble("no sci numbers here", true));
        assertNull(Strings.findFirstDouble("", true));
        assertNull(Strings.findFirstDouble(null, true));
        assertEquals("123.45", Strings.findFirstDouble("123.45", true));

        assertEquals("1.23e10", Strings.findFirstDouble("first1.23e10second4.56e-5", true));
    }

    @Test
    public void testFindFirstDouble_WithScientific() {
        assertEquals("1.23e4", findFirstDouble("value=1.23e4", true));
        assertEquals("1.23E-4", findFirstDouble("small=1.23E-4", true));
        assertEquals("123.45", findFirstDouble("abc123.45def", true));
        assertNull(findFirstDouble("no numbers", true));
        assertNull(findFirstDouble("", true));
        assertNull(findFirstDouble(null, true));
    }

    @Test
    public void testFindFirstDouble_EdgeCases() {
        assertEquals("12.34", Strings.findFirstDouble("abc12.34def"));
        assertEquals("-12.34", Strings.findFirstDouble("abc-12.34def"));
        assertEquals(".5", Strings.findFirstDouble(".5"));
        assertEquals(".5", Strings.findFirstDouble("x=.5"));
        assertEquals("-.5", Strings.findFirstDouble("x=-.5"));
        assertEquals(".5e2", Strings.findFirstDouble(".5e2", true));
        assertEquals("-.5e2", Strings.findFirstDouble("x=-.5e2", true));
        assertNull(Strings.findFirstDouble("abc"));
        assertNull(Strings.findFirstDouble(null));
        assertNull(Strings.findFirstDouble(""));
    }

    @Test
    public void testFindFirstInteger_InsideDecimals() {
        assertEquals("12", Strings.findFirstInteger("12.99"));
        assertEquals("12", Strings.findFirstInteger("Price: $12.99"));
        assertEquals("1", Strings.findFirstInteger("1e10"));
        assertEquals("2", Strings.findFirstInteger("v2.0"));
        assertEquals("Price: $X.99", Strings.replaceFirstInteger("Price: $12.99", "X"));
        assertEquals("vX.0", Strings.replaceFirstInteger("v2.0", "X"));
    }

    @Test
    public void test_RegExUtil() {
        {
            String ret = "abc".replace("a", "");
            assertEquals("bc", ret);

            ret = RegExUtil.replaceFirst("any", ".", (String) null);
            assertEquals("ny", ret);

            ret = RegExUtil.replaceAll("abc", ".", (String) null);
            assertEquals("", ret);
        }

    }

    @Test
    public void test_regularExpression() {
        assertEquals("123", Strings.findFirstInteger("abc123"));
        assertEquals("-12.34e+5", Strings.findFirstDouble("abc-12.34e+5xyz", true));
        assertEquals(Double.parseDouble("-12.34e+5"), Numbers.extractFirstDouble("abc-12.34e+5xyz", true).get());

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
    public void test_surrogate() {
        assertTrue(Character.isLowSurrogate(Character.MIN_LOW_SURROGATE));
        assertTrue(Character.isLowSurrogate(Character.MAX_LOW_SURROGATE));
        assertTrue(Character.isHighSurrogate(Character.MIN_HIGH_SURROGATE));
        assertTrue(Character.isHighSurrogate(Character.MAX_HIGH_SURROGATE));
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
    public void testFormatToPercentageWithScale() {
        assertEquals("50%", Numbers.format(0.5, "0%"));

        assertEquals("33.3%", Numbers.format(1.0 / 3.0, "0.0%"));

        assertEquals("33.333%", Numbers.format(1.0 / 3.0, "0.000%"));

        assertEquals("12.3457%", Numbers.format(0.123457, "0.0000%"));

        assertEquals("66.67%", Numbers.format(2.0 / 3.0, "0.00%"));
    }

    @Test
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

    /**
     * Regression: {@link Strings#trim(String)} previously short-circuited via
     * {@link Character#isWhitespace(char)}, which is NOT equivalent to the
     * predicate used by {@link String#trim()} (chars &lt;= 0x20). Low control
     * characters such as U+0001 (SOH) and U+0005 (ENQ) are NOT considered whitespace by
     * Character#isWhitespace, but ARE removed by String#trim. The bug caused
     * Strings.trim to return the input unchanged in those cases.
     */
    @Test
    public void testTrim_LowControlCharsRegression() {
        // U+0005 ENQ is trimmed by String#trim, and is not a Java whitespace char.
        assertEquals("abc", Strings.trim("\u0005abc"));
        assertEquals("abc", Strings.trim("abc\u0005"));
        assertEquals("abc", Strings.trim("\u0001abc\u0002"));
        assertEquals("", Strings.trim("\u0001\u0002\u0003"));
        // Non-breaking space (U+00A0) is > 0x20 and NOT trimmed by String#trim.
        assertEquals("\u00A0abc\u00A0", Strings.trim("\u00A0abc\u00A0"));
    }

    @Test
    public void testRepeat_EdgeCases() {
        assertEquals("", Strings.repeat('a', 0));
        assertEquals("a", Strings.repeat('a', 1));
        assertEquals("aaa", Strings.repeat('a', 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("a", -1));
        assertEquals("", Strings.repeat((String) null, 5));
        assertEquals("", Strings.repeat("", 5));
        // delimiter overload
        assertEquals("a-a-a", Strings.repeat("a", 3, "-"));
        assertEquals("", Strings.repeat("a", 0, "-"));
        // prefix/suffix
        assertEquals("[a,a,a]", Strings.repeat("a", 3, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("a", 0, ",", "[", "]"));
    }

    @Test
    public void testCountMatches_NonOverlapping() {
        // Documented as non-overlapping: aaaa contains 2 non-overlapping "aa"s.
        assertEquals(2, Strings.countMatches("aaaa", "aa"));
        assertEquals(3, Strings.countMatches("aaaaaa", "aa"));
        assertEquals(0, Strings.countMatches("", "aa"));
        assertEquals(0, Strings.countMatches("aaaa", ""));
        assertEquals(0, Strings.countMatches(null, "aa"));
        assertEquals(0, Strings.countMatches("aaaa", null));
    }

    @Test
    public void testRotate_LargeAndNegativeShifts() {
        assertEquals("abcdefg", Strings.rotate("abcdefg", 0));
        assertEquals("abcdefg", Strings.rotate("abcdefg", 7));
        assertEquals("abcdefg", Strings.rotate("abcdefg", -7));
        assertEquals("fgabcde", Strings.rotate("abcdefg", 2));
        assertEquals("cdefgab", Strings.rotate("abcdefg", -2));
        assertEquals("fgabcde", Strings.rotate("abcdefg", 9));
        assertEquals("cdefgab", Strings.rotate("abcdefg", -9));
        assertNull(Strings.rotate(null, 5));
        assertEquals("", Strings.rotate("", 5));
    }

    @Test
    public void testReverse_SurrogatePairsPreserved() {
        // Single supplementary code-point should NOT be split into two malformed surrogates.
        // U+1F600 = grinning face emoji.
        final String emoji = new String(Character.toChars(0x1F600));
        final String input = "a" + emoji + "b";
        final String reversed = Strings.reverse(input);
        assertEquals("b" + emoji + "a", reversed);
        // Round-trip
        assertEquals(input, Strings.reverse(reversed));
    }

    @Test
    public void testValidSurrogatePairAtNullStringReturnsFalse() {
        assertFalse(Strings.validSurrogatePairAt(null, 0));
    }

    @Test
    public void testCenter_PaddingDistribution() {
        assertEquals(" ab ", Strings.center("ab", 4));
        // Odd extra: extra goes on the right.
        assertEquals(" a  ", Strings.center("a", 4));
        assertEquals("    ", Strings.center("", 4));
        assertEquals("    ", Strings.center((String) null, 4));
        assertEquals("abcd", Strings.center("abcd", 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.center("a", -1));
    }

    @Test
    public void testPadStart_LeftPadding() {
        // padStart adds padding on the LEFT.
        assertEquals("    a", Strings.padStart("a", 5));
        assertEquals("0000a", Strings.padStart("a", 5, '0'));
        assertEquals("a", Strings.padStart("a", 1));
        // null becomes empty, then padded.
        assertEquals("---", Strings.padStart(null, 3, '-'));
        assertEquals("", Strings.padStart(null, 0));
    }

    @Test
    public void testPadEnd_RightPadding() {
        assertEquals("a    ", Strings.padEnd("a", 5));
        assertEquals("a0000", Strings.padEnd("a", 5, '0'));
        assertEquals("a", Strings.padEnd("a", 1));
        assertEquals("---", Strings.padEnd(null, 3, '-'));
        assertEquals("", Strings.padEnd(null, 0));
    }

    @Test
    public void testCapitalize_Edge() {
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
        assertEquals("A", Strings.capitalize("a"));
        assertEquals("Cat", Strings.capitalize("cat"));
        assertEquals("Cat", Strings.capitalize("Cat"));
    }

    @Test
    public void testAbbreviate_TooSmallLengthThrows() {
        // maxLength must be at least abbrevMarker.length + 1.
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.abbreviate("abcdefg", "...", 3));
        // OK boundaries.
        assertEquals("a...", Strings.abbreviate("abcdefg", 4));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 7));
        assertEquals("abcdefg", Strings.abbreviate("abcdefg", 8));
        assertNull(Strings.abbreviate(null, 4));
    }

    @Test
    public void testIndicesOf_negativeFromIndexAndEmptyTargetBounds() {
        // regression: a negative fromIndex threw IllegalArgumentException (siblings clamp to 0), and an
        // empty valueToFind with fromIndex >= length emitted an out-of-range index
        assertArrayEquals(new int[] { 0, 3 }, Strings.indicesOf("abcabc", "a", -1).toArray());
        assertArrayEquals(new int[] { 0, 3 }, Strings.indicesOfIgnoreCase("abcAbc", "a", -2).toArray());
        assertEquals(0, Strings.indicesOf("abc", "", 3).count());
        assertEquals(0, Strings.indicesOf("abc", "", 4).count());

        // documented behavior unchanged
        assertArrayEquals(new int[] { 2, 3 }, Strings.indicesOf("abcA", "", 2).toArray());
        assertArrayEquals(new int[] { 3 }, Strings.indicesOf("abcabc", "a", 2).toArray());
    }

    @Test
    public void testSubstringsBetweenStackBased_honorsToIndex() {
        // regression: the ALL_LEVELS branch ignored toIndex, returning matches partially or entirely
        // outside the requested range
        assertTrue(Strings.substringsBetween("a[b]c", 0, 3, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE).isEmpty());
        assertTrue(Strings.substringsBetween("a[bcdefg]h", 0, 3, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE).isEmpty());
        assertEquals(CommonUtil.asList("a"), Strings.substringsBetween("[a]x[b]", 0, 4, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE));

        // full-range behavior unchanged
        assertEquals(CommonUtil.asList("b"), Strings.substringsBetween("a[b]c", 0, 5, "[", "]", DelimiterMatchMode.ALL_LEVELS, Integer.MAX_VALUE));
    }

    @Test
    public void testUnicodeDisplayWidthTables_AreSortedAndDisjoint() throws ReflectiveOperationException {
        assertEquals("17.0.0", Strings.DISPLAY_WIDTH_UNICODE_VERSION);
        assertEquals(Strings.DISPLAY_WIDTH_UNICODE_VERSION, Unicode17Data.UNICODE_VERSION);
        assertCodePointRangesAreSortedAndDisjoint("ZERO_WIDTH_CATEGORY_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EAST_ASIAN_WIDE_OR_FULLWIDTH_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EAST_ASIAN_AMBIGUOUS_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EMOJI_PRESENTATION_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EMOJI_VARIATION_BASE_RANGES");
        assertCodePointRangesAreSortedAndDisjoint("EXTENDED_PICTOGRAPHIC_RANGES");
        assertPropertyRangesAreSortedAndDisjoint("GRAPHEME_BREAK_RANGES");
        assertPropertyRangesAreSortedAndDisjoint("INDIC_CONJUNCT_BREAK_RANGES");
    }

    @Test
    public void testUnicode17ExtendedGraphemeClusterConformance() throws Exception {
        final String resourceName = "/GraphemeBreakTest-17.0.0.txt";
        final InputStream input = StringsTest.class.getResourceAsStream(resourceName);
        assertNotNull(input, "Missing Unicode conformance resource: " + resourceName);

        final Method boundaryMethod = Strings.class.getDeclaredMethod("nextGraphemeClusterBoundary", String.class, int.class);
        boundaryMethod.setAccessible(true);
        int testCaseCount = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                lineNumber++;
                final int commentStart = line.indexOf('#');
                final String testData = (commentStart < 0 ? line : line.substring(0, commentStart)).trim();

                if (testData.isEmpty()) {
                    continue;
                }

                final StringBuilder sampleBuilder = new StringBuilder();
                final List<Integer> expectedBoundaries = new ArrayList<>();
                int utf16Offset = 0;

                for (final String token : testData.split("\\s+")) {
                    if ("\u00F7".equals(token)) {
                        expectedBoundaries.add(utf16Offset);
                    } else if (!"\u00D7".equals(token)) {
                        final int codePoint = Integer.parseInt(token, 16);
                        sampleBuilder.appendCodePoint(codePoint);
                        utf16Offset += Character.charCount(codePoint);
                    }
                }

                final String sample = sampleBuilder.toString();
                final List<Integer> actualBoundaries = new ArrayList<>();
                actualBoundaries.add(0);

                for (int clusterStart = 0; clusterStart < sample.length();) {
                    final int clusterEnd = (Integer) boundaryMethod.invoke(null, sample, clusterStart);
                    assertTrue(clusterEnd > clusterStart && clusterEnd <= sample.length(),
                            "Invalid boundary for GraphemeBreakTest line " + lineNumber + ": " + testData);
                    actualBoundaries.add(clusterEnd);
                    clusterStart = clusterEnd;
                }

                assertEquals(expectedBoundaries, actualBoundaries, "GraphemeBreakTest line " + lineNumber + ": " + testData);
                testCaseCount++;
            }
        }

        assertEquals(766, testCaseCount, "Unexpected Unicode 17 grapheme-break test count");
    }

    @Test
    public void testDisplayWidth_FormatAndHangulJamo() {
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(Character.MAX_CODE_POINT + 1));
        assertEquals(1, Strings.codePointDisplayWidth(0xD800)); // Java's valid code-point range includes surrogate values
        assertEquals(0, Strings.codePointDisplayWidth(0x200D)); // ZWJ, FORMAT
        assertEquals(0, Strings.codePointDisplayWidth(0x200C)); // ZWNJ, FORMAT
        assertEquals(0, Strings.codePointDisplayWidth(0x2028)); // Unicode line separator
        assertEquals(0, Strings.codePointDisplayWidth(0x2029)); // Unicode paragraph separator
        assertEquals(0, Strings.displayWidth("\u2028\u2029"));
        assertEquals(0, Strings.codePointDisplayWidth(0x00AD)); // soft hyphen is invisible without line layout
        assertEquals(0, Strings.codePointDisplayWidth(0x1161)); // Hangul medial V
        assertEquals(0, Strings.codePointDisplayWidth(0x11AB)); // Hangul final T
        assertEquals(0, Strings.codePointDisplayWidth(0xD7C6)); // last Extended-B medial V
        assertEquals(1, Strings.codePointDisplayWidth(0xD7C7)); // unassigned gap after Extended-B medial V
        assertEquals(1, Strings.codePointDisplayWidth(0xD7CA)); // unassigned gap before Extended-B final T
        assertEquals(0, Strings.codePointDisplayWidth(0xD7CB)); // first Extended-B final T
        assertEquals(0, Strings.codePointDisplayWidth(0xD7FB)); // last Extended-B final T
        assertEquals(1, Strings.codePointDisplayWidth(0xD7FC)); // unassigned gap after Extended-B final T
        assertEquals(1, Strings.codePointDisplayWidth(0xD7FF)); // end of the unassigned gap
        assertEquals(2, Strings.codePointDisplayWidth(0x1100)); // Hangul leading L
        assertEquals(2, Strings.codePointDisplayWidth(0x1112)); // Hangul leading HIEUH
        assertEquals(2, Strings.codePointDisplayWidth(0x231A)); // Unicode East Asian Width W and default emoji presentation
        assertEquals(2, Strings.codePointDisplayWidth(0x1F600)); // grinning face: Emoji_Presentation
        assertEquals(2, Strings.codePointDisplayWidth(0x1F1FA)); // regional indicator U: Emoji_Presentation, not EAW W/F
        assertEquals(2, Strings.codePointDisplayWidth(0x1F3FD)); // medium skin-tone modifier
        assertEquals(1, Strings.codePointDisplayWidth(0x2764)); // text-default heavy black heart
        assertEquals(0, Strings.codePointDisplayWidth(0xFE0F)); // emoji variation selector: zero-width, cluster promoter only
        assertEquals(0, Strings.codePointDisplayWidth(0x20E3)); // keycap mark: zero-width, cluster promoter only
        assertEquals(1, Strings.codePointDisplayWidth(0x093E)); // Devanagari spacing vowel sign is visible when standalone
        assertEquals(1, Strings.displayWidth("\u093E"));
        assertEquals(1, Strings.displayWidth("\u0915\u093E")); // Mc attaches to a base without adding another cell
        assertEquals(2, Strings.displayWidth("한"));
        assertEquals(2, Strings.displayWidth(java.text.Normalizer.normalize("한", java.text.Normalizer.Form.NFD)));
        assertEquals(1, Strings.codePointDisplayWidth('a'));
        assertEquals(2, Strings.codePointDisplayWidth('中'));
        assertEquals(Strings.displayWidth(new String(Character.toChars(0x1F600))), Strings.codePointDisplayWidth(0x1F600));
        assertEquals(Strings.displayWidth(new String(Character.toChars(0x1F1FA))), Strings.codePointDisplayWidth(0x1F1FA));
        assertEquals(Strings.displayWidth(new String(Character.toChars(0x1F3FD))), Strings.codePointDisplayWidth(0x1F3FD));
        assertEquals(Strings.displayWidth("\u2764"), Strings.codePointDisplayWidth(0x2764));
        assertEquals("⌚", Strings.padEndToDisplayWidth("⌚", 2));
    }

    @Test
    public void testDisplayWidth_StringClusters() {
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        final String thumbsUpMediumSkinTone = "\uD83D\uDC4D\uD83C\uDFFD";
        final String usFlag = "\uD83C\uDDFA\uD83C\uDDF8";
        final String keycapOne = "1\uFE0F\u20E3";
        final String devanagariConjunct = "\u0915\u094D\u0937";
        final String cjkIdeographicVariationSequence = "\u4E2D" + new String(Character.toChars(0xE0100));

        assertEquals(0, Strings.displayWidth((String) null));
        assertEquals(0, Strings.displayWidth(""));
        assertEquals(3, Strings.displayWidth("abc"));
        assertEquals(4, Strings.displayWidth("\u4E2D\u6587"));
        assertEquals(1, Strings.displayWidth("\u00A1")); // East Asian Width A remains narrow.
        assertEquals(2, Strings.displayWidth("\u231A")); // Default emoji presentation.
        assertEquals(1, Strings.displayWidth("e\u0301"));
        assertEquals(1, Strings.displayWidth("A\u3099")); // Zero-width EAW-W combining mark does not promote a narrow base.
        assertEquals(0, Strings.displayWidth("\u0301"));
        assertEquals(0, Strings.displayWidth("\u200D"));
        assertEquals(2, Strings.displayWidth(java.text.Normalizer.normalize("\uD55C", java.text.Normalizer.Form.NFD)));

        assertEquals(2, Strings.displayWidth(womanTechnologist));
        assertEquals(2, Strings.displayWidth(thumbsUpMediumSkinTone));
        assertEquals(2, Strings.displayWidth(usFlag));
        assertEquals(2, Strings.displayWidth(keycapOne));
        assertEquals(1, Strings.displayWidth("\u2764"));
        assertEquals(2, Strings.displayWidth("\u2764\uFE0F"));
        assertEquals(2, Strings.displayWidth("1\uFE0F")); // standardized emoji-presentation sequence
        assertEquals(1, Strings.displayWidth("a\uFE0F")); // unsupported variation selector does not promote
        assertEquals(1, Strings.displayWidth("a\u20E3")); // keycap mark requires a valid keycap sequence
        assertEquals(1, Strings.displayWidth("1\u20E3")); // unqualified keycap sequence is not promoted
        assertEquals(1, Strings.displayWidth("a\uFE0F\u20E3"));
        assertEquals(1, Strings.displayWidth("\uD83D"));
        assertEquals(2, Strings.displayWidth(cjkIdeographicVariationSequence));
        assertEquals(2, Strings.codePointDisplayWidth(0x0915) + Strings.codePointDisplayWidth(0x094D) + Strings.codePointDisplayWidth(0x0937));
        assertEquals(1, Strings.displayWidth(devanagariConjunct));
        assertEquals(devanagariConjunct + " ", Strings.padEndToDisplayWidth(devanagariConjunct, 2));

        // Common standalone East Asian scalars share the additive fast path with ASCII.
        assertEquals(3, Strings.displayWidth("abc"));
        assertEquals(4, Strings.displayWidth("ab中"));
        assertEquals(2, Strings.displayWidth("한"));
        assertEquals(4, Strings.displayWidth("ＡＢ"));
        assertEquals(1, Strings.displayWidth("e\u0301"));
        // Segmentation path: summing code-point widths over-counts a ZWJ cluster (2+0+2 vs 2).
        assertEquals(4, Strings.codePointDisplayWidth(0x1F469) + Strings.codePointDisplayWidth(0x200D) + Strings.codePointDisplayWidth(0x1F4BB));
        assertEquals(2, Strings.displayWidth(womanTechnologist));
    }

    @Test
    public void testDisplayWidth_LateClusterPreservesAdditivePrefix() {
        final String asciiPrefix = "a".repeat(4096);
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        final String supplementaryHan = new String(Character.toChars(0x20000));
        final String ideographicVariationSelector = new String(Character.toChars(0xE0100));

        assertEquals(4096, Strings.displayWidth(asciiPrefix + "\u0301"));
        assertEquals(4096, Strings.displayWidth(asciiPrefix + "\u200D"));
        assertEquals(4096, Strings.displayWidth(asciiPrefix + "\r\n\u0301"));
        assertEquals(4097, Strings.displayWidth("a".repeat(4095) + "1\uFE0F\u20E3"));
        assertEquals(4097, Strings.displayWidth("a".repeat(4095) + supplementaryHan + ideographicVariationSelector));
        assertEquals(4097, Strings.displayWidth("a".repeat(4095) + "\uAC00\u11A8"));
        assertEquals(4098, Strings.displayWidth(asciiPrefix + womanTechnologist));
        assertEquals(4097, Strings.displayWidth(asciiPrefix + womanTechnologist, new Strings.DisplayWidthPolicy(1, 1)));
    }

    @Test
    public void testDisplayWidth_PrefixOptimizationMatchesFullSegmentation() throws ReflectiveOperationException {
        final Method nextBoundary = Strings.class.getDeclaredMethod("nextGraphemeClusterBoundary", String.class, int.class);
        final Method clusterWidth = Strings.class.getDeclaredMethod("graphemeClusterDisplayWidth", String.class, int.class, int.class,
                Strings.DisplayWidthPolicy.class);
        nextBoundary.setAccessible(true);
        clusterWidth.setAccessible(true);

        final String supplementaryHan = new String(Character.toChars(0x20000));
        final String[] additivePrefixes = { "", "a", "abc", "a".repeat(64), "\r\n", "a".repeat(64) + "\r\n", "\u4E2D\uFF21\uAC00",
                "a".repeat(64) + supplementaryHan };
        final String[] sequenceSensitiveSuffixes = { "\u0301", "\u200D", "\uFE0F", "\u1100\u1161\u11A8", "\uD83D\uDC69\u200D\uD83D\uDCBB", "\u0915\u094D\u0937",
                "\uD83C\uDDFA\uD83C\uDDF8", "\u0600a", new String(Character.toChars(0xE0100)) };
        final Strings.DisplayWidthPolicy[] policies = { Strings.DisplayWidthPolicy.DEFAULT, Strings.DisplayWidthPolicy.CJK,
                new Strings.DisplayWidthPolicy(1, 1) };

        for (final Strings.DisplayWidthPolicy policy : policies) {
            for (final String prefix : additivePrefixes) {
                for (final String suffix : sequenceSensitiveSuffixes) {
                    final String value = prefix + suffix;
                    assertEquals(fullySegmentedDisplayWidth(value, policy, nextBoundary, clusterWidth), Strings.displayWidth(value, policy));
                }
            }
        }

        final int[] alphabet = { 'a', '\r', '\n', 0x0301, 0x0600, 0x0915, 0x094D, 0x0937, 0x1100, 0x1161, 0x11A8, 0x200D, 0x2764, 0xFE0F, 0x20E3, 0xAC00,
                0x4E2D, 0xFF21, 0x1F1FA, 0x1F1F8, 0x1F469, 0x1F4BB, 0x1F3FD, 0x20000, 0xE0100, 0xD800, 0xDC00 };
        final Random random = new Random(20260830L);

        for (final Strings.DisplayWidthPolicy policy : policies) {
            for (int iteration = 0; iteration < 1_000; iteration++) {
                final StringBuilder value = new StringBuilder();
                final int length = random.nextInt(41);

                for (int i = 0; i < length; i++) {
                    value.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
                }

                assertEquals(fullySegmentedDisplayWidth(value.toString(), policy, nextBoundary, clusterWidth), Strings.displayWidth(value.toString(), policy),
                        "iteration=" + iteration + ", policy=" + policy);
            }
        }
    }

    @Test
    public void testDisplayWidth_ConfigurableTerminalPolicy() {
        final Strings.DisplayWidthPolicy singleColumnEmoji = new Strings.DisplayWidthPolicy(1, 1);

        assertEquals(1, Strings.codePointDisplayWidth(0x00A1));
        assertEquals(2, Strings.codePointDisplayWidth(0x00A1, Strings.DisplayWidthPolicy.CJK));
        assertEquals(2, Strings.codePointDisplayWidth(0x1F1FA));
        assertEquals(1, Strings.codePointDisplayWidth(0x1F1FA, singleColumnEmoji));
        assertEquals(1, Strings.codePointDisplayWidth(0x1F600, singleColumnEmoji));
        assertEquals(2, Strings.displayWidth("\u2764\uFE0F"));
        assertEquals(1, Strings.displayWidth("\u2764\uFE0F", singleColumnEmoji));
        assertEquals("¡ ", Strings.padEndToDisplayWidth("¡", 2));
        assertEquals("¡", Strings.padEndToDisplayWidth("¡", 2, Strings.DisplayWidthPolicy.CJK));

        assertThrows(IllegalArgumentException.class, () -> new Strings.DisplayWidthPolicy(0, 2));
        assertThrows(IllegalArgumentException.class, () -> new Strings.DisplayWidthPolicy(1, 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.displayWidth(null, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth('a', null));
        assertEquals("", Strings.padEndToDisplayWidth(null, 0));
        assertEquals("", Strings.padStartToDisplayWidth(null, 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.padEndToDisplayWidth(null, -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.padStartToDisplayWidth(null, -1));
        assertThrows(IllegalArgumentException.class, () -> Strings.padEndToDisplayWidth(null, 0, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.padStartToDisplayWidth(null, 0, null));
    }

    @Test
    public void testDisplayWidth_FastPathClassification() throws ReflectiveOperationException {
        final Method method = Strings.class.getDeclaredMethod("needsGraphemeClusterSegmentation", int.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(null, (int) 'A'));
        assertFalse((Boolean) method.invoke(null, 0x4E2D)); // Han ideograph
        assertFalse((Boolean) method.invoke(null, 0x20000)); // supplementary Han ideograph
        assertFalse((Boolean) method.invoke(null, 0xAC00)); // first precomposed Hangul syllable
        assertFalse((Boolean) method.invoke(null, 0xD7A3)); // last precomposed Hangul syllable
        assertFalse((Boolean) method.invoke(null, 0xFF01)); // first fullwidth ASCII form
        assertFalse((Boolean) method.invoke(null, 0xFF5E)); // last fullwidth ASCII form

        assertTrue((Boolean) method.invoke(null, 0xE0100)); // ideographic variation selector
        assertTrue((Boolean) method.invoke(null, 0x1100)); // decomposed Hangul leading jamo
        assertTrue((Boolean) method.invoke(null, 0x1161)); // decomposed Hangul medial jamo
        assertTrue((Boolean) method.invoke(null, 0x1F600)); // emoji
        assertTrue((Boolean) method.invoke(null, 0x1F1FA)); // regional indicator
        assertTrue((Boolean) method.invoke(null, 0x094D)); // Devanagari virama
    }

    @Test
    public void testCompare() {
        // normal
        assertEquals(0, Strings.compare("abc", "abc"));
        assertTrue(Strings.compare("abc", "def") < 0);
        assertTrue(Strings.compare("def", "abc") > 0);
        // case-sensitive: lowercase > uppercase
        assertTrue(Strings.compare("abc", "ABC") > 0);
        assertNotEquals(0, Strings.compare("abc", "ABC"));
        // null/empty edge
        assertEquals(-1, Strings.compare(null, "abc"));
        assertEquals(1, Strings.compare("abc", null));
        assertEquals(0, Strings.compare(null, null));
        assertTrue(Strings.compare("", "a") < 0);
        assertEquals(0, Strings.compare("", ""));
        // consistency with String.compareTo
        assertEquals("abc".compareTo("abd"), Strings.compare("abc", "abd"));
    }

    @Test
    public void testPadStartByDisplayWidth() {
        // normal
        assertEquals("   ab", Strings.padStartToDisplayWidth("ab", 5));
        // wide CJK characters count as 2 each (display width 4 -> 1 space)
        assertEquals(" 中文", Strings.padStartToDisplayWidth("中文", 5));
        // already wide enough
        assertEquals("abcde", Strings.padStartToDisplayWidth("abcde", 3));
        assertEquals("abc", Strings.padStartToDisplayWidth("abc", 3));
        // null treated as empty, then padded
        assertEquals("   ", Strings.padStartToDisplayWidth(null, 3));
        assertEquals("", Strings.padStartToDisplayWidth(null, 0));
        // empty input
        assertEquals("  ", Strings.padStartToDisplayWidth("", 2));
        // negative width throws
        assertThrows(IllegalArgumentException.class, () -> Strings.padStartToDisplayWidth("ab", -1));

        // Cluster-aware: ZWJ emoji is one display unit (width 2), so two spaces bring it to width 4.
        final String womanTechnologist = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        assertEquals("  " + womanTechnologist, Strings.padStartToDisplayWidth(womanTechnologist, 4));
        assertEquals(womanTechnologist, Strings.padStartToDisplayWidth(womanTechnologist, 2));
    }

    @Test
    public void testCountMatchesIgnoreCase() {
        // normal (same-case)
        assertEquals(3, Strings.countMatchesIgnoreCase("abcabcabc", "abc"));
        // case-insensitive match
        assertEquals(3, Strings.countMatchesIgnoreCase("abcABCabc", "abc"));
        assertEquals(2, Strings.countMatchesIgnoreCase("Hello HELLO", "hello"));
        // non-overlapping
        assertEquals(2, Strings.countMatchesIgnoreCase("aAaA", "aa"));
        // not found
        assertEquals(0, Strings.countMatchesIgnoreCase("test", "xyz"));
        // null/empty
        assertEquals(0, Strings.countMatchesIgnoreCase(null, "test"));
        assertEquals(0, Strings.countMatchesIgnoreCase("test", null));
        assertEquals(0, Strings.countMatchesIgnoreCase("", "a"));
        assertEquals(0, Strings.countMatchesIgnoreCase("test", ""));
    }

    @Test
    public void testCaseFormatSplitChar_rejectsSurrogateCodeUnits() {
        assertCaseFormatConvertersRejectSurrogate('\uD83D');
        assertCaseFormatConvertersRejectSurrogate('\uDE00');
    }

    @Test
    public void testCaseConverters_WhitespaceIsDelimiter() {
        // camel/upperCamel already split on whitespace (via regex); now snake/kebab/screaming do too
        assertEquals("firstName", Strings.toCamelCase("first name"));
        assertEquals("FirstName", Strings.toUpperCamelCase("first name"));
        assertEquals("first_name", Strings.toSnakeCase("first name"));
        assertEquals("first-name", Strings.toKebabCase("first name"));
        assertEquals("FIRST_NAME", Strings.toScreamingSnakeCase("first name"));

        // capitalized words separated by a space
        assertEquals("hello_world", Strings.toSnakeCase("Hello World"));
        assertEquals("hello-world", Strings.toKebabCase("Hello World"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("Hello World"));

        // consecutive whitespace collapses to a single separator (no duplicate separators)
        assertEquals("hello_world", Strings.toSnakeCase("hello  world"));
        assertEquals("hello-world", Strings.toKebabCase("hello  world"));
        assertEquals("HELLO_WORLD", Strings.toScreamingSnakeCase("hello  world"));

        // tab counts as whitespace
        assertEquals("a_b", Strings.toSnakeCase("a\tb"));
        assertEquals("a-b", Strings.toKebabCase("a\tb"));

        // leading/trailing separator runs are dropped
        assertEquals("hello", Strings.toSnakeCase(" hello"));
        assertEquals("hello", Strings.toKebabCase(" hello"));
        assertEquals("hello", Strings.toSnakeCase("hello "));
        assertEquals("hello", Strings.toKebabCase("hello "));
        assertEquals("HELLO", Strings.toScreamingSnakeCase("hello "));
        assertEquals("hello", Strings.toSnakeCase("hello"));
        assertEquals("hello", Strings.toKebabCase("hello"));

        // mixed whitespace + existing delimiter, still single separator
        assertEquals("a_b_c", Strings.toSnakeCase("a-b c"));
        assertEquals("a-b-c", Strings.toKebabCase("a_b c"));

        // char overloads also handle whitespace (via delegation to the no-arg form)
        assertEquals("a_b_c", Strings.toSnakeCase("a.b c", '.'));
        assertEquals("a-b-c", Strings.toKebabCase("a.b c", '.'));
        assertEquals("A_B_C", Strings.toScreamingSnakeCase("a.b c", '.'));
    }

    @Test
    public void testUnicodePredicates_SupplementaryCodePoints() {
        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        final String osmanyaDigit = new String(Character.toChars(0x104A0));

        assertTrue(Strings.isAllUpperCase(deseretCapital));
        assertTrue(Strings.isAllLowerCase(deseretSmall));
        assertTrue(Strings.isMixedCase(deseretCapital + deseretSmall));
        assertTrue(Strings.isAlpha(deseretCapital + deseretSmall));
        assertTrue(Strings.isAlphaSpace(deseretCapital + " " + deseretSmall));
        assertTrue(Strings.isAlphanumeric(deseretCapital + osmanyaDigit));
        assertTrue(Strings.isAlphanumericSpace(deseretCapital + " " + osmanyaDigit));
        assertTrue(Strings.isNumeric(osmanyaDigit));
        assertTrue(Strings.isNumericSpace(osmanyaDigit + " " + osmanyaDigit));
    }

    @Test
    public void testLongestCommonSubstring_DoesNotSplitSurrogatePairs() {
        final String grinningFace = new String(Character.toChars(0x1F600));
        final String beamingFace = new String(Character.toChars(0x1F601));

        assertEquals(grinningFace, Strings.longestCommonSubstring("a" + grinningFace + "b", "x" + grinningFace + "y"));
        assertEquals("", Strings.longestCommonSubstring(grinningFace, beamingFace));
    }

    @Test
    public void testCaseConverters_SupplementaryUnicode() {
        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));

        assertEquals(deseretSmall + "Value", Strings.toCamelCase(deseretCapital + "Value"));
        assertEquals(deseretCapital + "Value", Strings.toUpperCamelCase(deseretSmall + "Value"));
        assertEquals(deseretSmall + "_value", Strings.toSnakeCase(deseretSmall + "Value"));
        assertEquals(deseretCapital + "_VALUE", Strings.toScreamingSnakeCase(deseretSmall + "Value"));
        assertEquals(deseretSmall + "-value", Strings.toKebabCase(deseretSmall + "Value"));
    }

    @Test
    public void testSubstringsBetween_EndDelimiterMustFitWithinRange() {
        final String str = "a<x>>z";

        for (final DelimiterMatchMode strategy : DelimiterMatchMode.values()) {
            assertTrue(Strings.substringsBetween(str, 0, 4, "<", ">>", strategy, Integer.MAX_VALUE).isEmpty());
            assertEquals(CommonUtil.asList("x"), Strings.substringsBetween(str, 0, 5, "<", ">>", strategy, Integer.MAX_VALUE));
        }
    }

    @Test
    public void testOptimizedAsciiCasePaths_MatchUnicodeReference() {
        final char[] alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_- \t\n\r\f.!@#$%^&*()".toCharArray();
        final Random random = new Random(20260822L);

        for (int iteration = 0; iteration < 750; iteration++) {
            final StringBuilder value = new StringBuilder();
            final int length = random.nextInt(97);

            for (int i = 0; i < length; i++) {
                value.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final String input = value.toString();
            assertEquals(camelCaseReference(input, false), Strings.toCamelCase(input), input);
            assertEquals(camelCaseReference(input, true), Strings.toUpperCamelCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', false), Strings.toSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', true), Strings.toScreamingSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '-', false), Strings.toKebabCase(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, false), Strings.capitalizeWords(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, true), Strings.capitalizeWordsFully(input), input);
            assertEquals(swapAsciiCaseReference(input), Strings.swapCase(input), input);
        }

        final String punctuationOnly = "0123_- !";
        assertTrue(punctuationOnly == Strings.swapCase(punctuationOnly));

        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        assertEquals(deseretSmall + " VALUE", Strings.swapCase(deseretCapital + " value"));
        assertEquals(deseretSmall + "Value", Strings.toCamelCase(deseretCapital + "Value"));
    }

    @Test
    public void testOptimizedWhitespaceStripAndEscapePaths_MatchReference() {
        final char[] alphabet = { 'a', 'B', '0', ' ', '\t', '\n', '\r', '\f', '\u000B', '\u001C', '\u00A0', '\u2003', '\'', '"', '\\', '_', '-', '\uD83D',
                '\uDE00' };
        final char[] quoteChars = { '\'', '"', '\\', 'a', '\uD800' };
        final String[] stripSets = { null, "", "ab", " \t", "'\"\\", "\uD83D\uDE00" };
        final Random random = new Random(20260823L);

        for (int iteration = 0; iteration < 600; iteration++) {
            final StringBuilder value = new StringBuilder();
            final int length = random.nextInt(81);

            for (int i = 0; i < length; i++) {
                value.append(alphabet[random.nextInt(alphabet.length)]);
            }

            final String input = value.toString();
            assertEquals(normalizeSpaceReference(input), Strings.normalizeSpace(input), input);
            assertEquals(removeWhitespaceReference(input), Strings.removeWhitespace(input), input);
            assertEquals(escapeQuotesReference(input, (char) 0, true), Strings.escapeQuotes(input), input);

            for (final char quoteChar : quoteChars) {
                assertEquals(escapeQuotesReference(input, quoteChar, false), Strings.escapeQuotes(input, quoteChar), input);
            }

            for (final String stripSet : stripSets) {
                assertEquals(Strings.stripEnd(Strings.stripStart(input, stripSet), stripSet), Strings.strip(input, stripSet), input);
            }
        }

        final String normalized = "already normalized";
        final String withoutWhitespace = "alreadyNormalized";
        final String withoutQuotes = "no quote characters";
        final String withoutStripChars = "middle";
        assertTrue(normalized == Strings.normalizeSpace(normalized));
        assertTrue(withoutWhitespace == Strings.removeWhitespace(withoutWhitespace));
        assertTrue(withoutQuotes == Strings.escapeQuotes(withoutQuotes));
        assertTrue(withoutStripChars == Strings.strip(withoutStripChars, "xyz"));
        assertEquals("\\\\", Strings.escapeQuotes("\\\\", '\\'));
        assertEquals("\\\\", Strings.escapeQuotes("\\", '\\'));
        assertEquals("a b", Strings.normalizeSpace("\u00A0a\u2003\t b\u00A0"));
    }

    @Test
    public void testCharacterMembershipFastPaths_RandomizedDifferential() {
        final Random random = new Random(20260825L);

        for (int iteration = 0; iteration < 300; iteration++) {
            final char[] inputChars = new char[random.nextInt(601)];
            final char[] values = new char[random.nextInt(161)];

            for (int i = 0; i < inputChars.length; i++) {
                inputChars[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            for (int i = 0; i < values.length; i++) {
                values[i] = (char) random.nextInt(Character.MAX_VALUE + 1);
            }

            if (inputChars.length > 0 && values.length > 0 && iteration % 3 == 0) {
                values[0] = inputChars[random.nextInt(inputChars.length)];
            }

            final String input = new String(inputChars);
            final int fromIndex = random.nextInt(input.length() + 41) - 20;
            assertEquals(indexOfAnyButReference(input, fromIndex, values), Strings.indexOfAnyBut(input, fromIndex, values), "iteration " + iteration);
            assertEquals(containsAllCharsReference(input, values), Strings.containsAll(input, values), "iteration " + iteration);
            assertEquals(containsNoneCharsReference(input, values), Strings.containsNone(input, values), "iteration " + iteration);
            assertEquals(!input.isEmpty() && values.length > 0 && indexOfAnyButReference(input, 0, values) < 0 || input.isEmpty(),
                    Strings.containsOnly(input, values), "iteration " + iteration);
            assertEquals(!input.isEmpty() && values.length > 0 && !containsNoneCharsReference(input, values), Strings.containsAny(input, values),
                    "iteration " + iteration);
        }

        final char[] highValues = { '\u0000', '\u003F', '\u0040', '\uD800', '\uDC00', '\uFFFF', '\uFFFF', 'x' };
        final String highInput = "x\u0000\uD800\uDC00\uFFFF".repeat(1024);
        assertEquals(indexOfAnyButReference(highInput, 0, highValues), Strings.indexOfAnyBut(highInput, highValues));
        assertEquals(containsAllCharsReference(highInput, highValues), Strings.containsAll(highInput, highValues));
        assertEquals(containsNoneCharsReference(highInput, highValues), Strings.containsNone(highInput, highValues));
        assertFalse(Strings.containsOnly(null, highValues));
        assertTrue(Strings.containsOnly("", highValues));
    }

    @Test
    public void testIgnoreCaseKmpPipeline_RandomizedDifferential() {
        final char[] alphabet = { 'a', 'A', 'b', 'B', 'c', 'C', 'I', 'i', '\u0130', '\u0131', '\u03A3', '\u03C3', '\u03C2', '\u017F', 'S', 's', '\u212A', 'K',
                'k', '0' };
        final int[] maximumReplacements = { -1, 0, 1, 2, 5 };
        final Random random = new Random(20260826L);

        for (int iteration = 0; iteration < 180; iteration++) {
            final String target = randomString(random, alphabet, 16 + random.nextInt(33));
            final StringBuilder inputBuilder = new StringBuilder(randomString(random, alphabet, 256 + random.nextInt(257)));

            if ((iteration & 1) == 0) {
                final int matchIndex = random.nextInt(inputBuilder.length() - target.length() + 1);
                inputBuilder.replace(matchIndex, matchIndex + target.length(), caseVariant(target));
            }

            final String input = inputBuilder.toString();
            final int fromIndex = random.nextInt(input.length() + 41) - 20;
            final int startIndexFromBack = random.nextInt(input.length() + 41) - 20;
            final int max = maximumReplacements[random.nextInt(maximumReplacements.length)];
            final String replacement = iteration % 7 == 0 ? null : "<" + iteration + ">";

            assertEquals(indexOfIgnoreCaseReference(input, target, fromIndex), Strings.indexOfIgnoreCase(input, target, fromIndex), "iteration " + iteration);
            assertEquals(lastIndexOfIgnoreCaseReference(input, target, startIndexFromBack), Strings.lastIndexOfIgnoreCase(input, target, startIndexFromBack),
                    "iteration " + iteration);
            assertEquals(countMatchesIgnoreCaseReference(input, target), Strings.countMatchesIgnoreCase(input, target), "iteration " + iteration);
            assertArrayEquals(indicesOfIgnoreCaseReference(input, target, fromIndex), Strings.indicesOfIgnoreCase(input, target, fromIndex).toArray(),
                    "iteration " + iteration);
            assertEquals(replaceIgnoreCaseReference(input, fromIndex, target, replacement, max),
                    Strings.replaceIgnoreCase(input, fromIndex, target, replacement, max), "iteration " + iteration);
        }

        final String targetLength15 = "a".repeat(14) + "b";
        final String targetLength16 = "a".repeat(15) + "b";
        final String adversarial = "a".repeat(300) + "B";
        assertEquals(indexOfIgnoreCaseReference(adversarial, targetLength15, 0), Strings.indexOfIgnoreCase(adversarial, targetLength15));
        assertEquals(indexOfIgnoreCaseReference(adversarial, targetLength16, 0), Strings.indexOfIgnoreCase(adversarial, targetLength16));
        assertEquals(lastIndexOfIgnoreCaseReference(adversarial, targetLength16, adversarial.length()),
                Strings.lastIndexOfIgnoreCase(adversarial, targetLength16));

        final String deseretCapital = new String(Character.toChars(0x10400));
        final String deseretSmall = new String(Character.toChars(0x10428));
        final String supplementaryInput = ("x" + deseretCapital).repeat(200);
        final String supplementaryTarget = ("X" + deseretSmall).repeat(20);
        assertEquals(countMatchesIgnoreCaseReference(supplementaryInput, supplementaryTarget),
                Strings.countMatchesIgnoreCase(supplementaryInput, supplementaryTarget));
        assertEquals(replaceIgnoreCaseReference(supplementaryInput, 0, supplementaryTarget, "z", -1),
                Strings.replaceAllIgnoreCase(supplementaryInput, supplementaryTarget, "z"));
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
    public void testReverseDelimitedAndConcatenationOptimizations_EdgeAndRandomizedCoverage() {
        final char delimiter = '|';
        final char[] alphabet = { 'a', 'b', ' ', delimiter, '\u0000', '\uD83D', '\uDE00' };
        final Random random = new Random(20260828L);

        for (int iteration = 0; iteration < 500; iteration++) {
            final String input = randomString(random, alphabet, random.nextInt(81));
            assertEquals(reverseDelimitedReference(input, delimiter), Strings.reverseDelimited(input, delimiter), "iteration " + iteration);
            assertEquals(reverseDelimitedReference(input, delimiter), Strings.reverseDelimited(input, String.valueOf(delimiter)), "iteration " + iteration);
        }

        final String noDelimiter = "unchanged";
        assertTrue(noDelimiter == Strings.reverseDelimited(noDelimiter, delimiter));
        assertEquals("|a||", Strings.reverseDelimited("||a|", delimiter));
        assertEquals("c::b::a", Strings.reverseDelimited("a::b::c", "::"));

        assertEquals("abc", Strings.concatNullToEmpty("a", null, "b", "", "c"));
        assertEquals("abcdefghi", Strings.concatNullToEmpty("a", "b", "c", "d", "e", "f", "g", "h", "i"));
        final String[] many = { null, "a", "", "b", null, "c", "d", "", "e", "f", null, "g" };
        assertEquals(concatNullToEmptyReference(many), Strings.concatNullToEmpty(many));
        assertEquals("", Strings.concatNullToEmpty(new String[12]));

        final String emoji = new String(Character.toChars(0x1F600));
        assertEquals(emoji + emoji + "x", Strings.padStart("x", 4, emoji));
        assertEquals("x" + emoji + emoji, Strings.padEnd("x", 4, emoji));
        assertEquals("<ab::ab::ab>", Strings.repeat("ab", 3, "::", "<", ">"));
        assertEquals("[true||false||true]", Strings.join(new boolean[] { true, false, true }, 0, 3, "||", "[", "]"));
        assertEquals("[ a ||null|| b ]", Strings.join(new Object[] { " a ", null, " b " }, 0, 3, "||", "[", "]", false));
        final Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put("x", 1);
        entries.put("y", 2);
        assertEquals("x=1, y=2", Strings.joinEntries(entries, ", ", "="));
        assertEquals("A 1: [2]", Strings.lenientFormat("%s %s", "A", 1, 2));
        assertEquals("b" + emoji + "a", Strings.reverse("a" + emoji + "b"));
    }

    @Test
    public void testDisplayWidth_AsciiUncheckedFastPathExhaustive() {
        final Strings.DisplayWidthPolicy[] policies = { Strings.DisplayWidthPolicy.DEFAULT, Strings.DisplayWidthPolicy.CJK,
                new Strings.DisplayWidthPolicy(2, 1) };

        for (final Strings.DisplayWidthPolicy policy : policies) {
            for (int codePoint = 0; codePoint <= 0x7F; codePoint++) {
                final int expected = codePoint <= 0x1F || codePoint == 0x7F ? 0 : 1;
                assertEquals(expected, Strings.codePointDisplayWidth(codePoint, policy), "U+" + Integer.toHexString(codePoint));
                assertEquals(expected, Strings.displayWidth(String.valueOf((char) codePoint), policy), "U+" + Integer.toHexString(codePoint));
            }
        }

        final StringBuilder printableAscii = new StringBuilder(95);
        for (char ch = 0x20; ch < 0x7F; ch++) {
            printableAscii.append(ch);
        }
        assertEquals(95, Strings.displayWidth(printableAscii.toString()));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth(0x110000));
        assertThrows(IllegalArgumentException.class, () -> Strings.codePointDisplayWidth('a', null));
    }

    @Test
    public void testOptimizedAsciiCasePaths_ExhaustiveShortInputs() {
        final List<String> inputs = shortStrings(new char[] { 'a', 'A', 'b', 'B', '0', '_', '-', ' ', '\t', '.' }, 4);

        for (final String input : inputs) {
            assertEquals(camelCaseReference(input, false), Strings.toCamelCase(input), input);
            assertEquals(camelCaseReference(input, true), Strings.toUpperCamelCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', false), Strings.toSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '_', true), Strings.toScreamingSnakeCase(input), input);
            assertEquals(delimitedCaseReference(input, '-', false), Strings.toKebabCase(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, false), Strings.capitalizeWords(input), input);
            assertEquals(capitalizeWhitespaceWordsReference(input, true), Strings.capitalizeWordsFully(input), input);
            assertEquals(swapAsciiCaseReference(input), Strings.swapCase(input), input);

            final String customDelimiterInput = input.replace('.', '-');
            assertEquals(camelCaseReference(customDelimiterInput, false), Strings.toCamelCase(input, '.'), input);
            assertEquals(camelCaseReference(customDelimiterInput, true), Strings.toUpperCamelCase(input, '.'), input);
        }

        // capitalizeWords capitalizes the first code point, not the first later letter.
        assertEquals("2bETA !cat ABC", Strings.capitalizeWords("2bETA !cat aBC"));
        assertEquals("2beta !cat Abc", Strings.capitalizeWordsFully("2bETA !cat aBC"));
        // Camel case intentionally uses the first letter after a separator.
        assertEquals("2Beta", Strings.toCamelCase("2_beta"));
        assertEquals("2Beta", Strings.toUpperCamelCase("2_beta"));
    }

    @Test
    public void testOptimizedWhitespaceAndQuotePaths_ExhaustiveShortInputsAndPredicates() {
        final List<String> inputs = shortStrings(new char[] { 'a', ' ', '\t', '\u00A0', '\'', '"', '\\', '\uD800' }, 4);
        final char[] quoteChars = { '\'', '"', '\\', 'a', '\uD800' };

        for (final String input : inputs) {
            assertEquals(normalizeSpaceReference(input), Strings.normalizeSpace(input), input);
            assertEquals(removeWhitespaceReference(input), Strings.removeWhitespace(input), input);
            assertEquals(escapeQuotesReference(input, (char) 0, true), Strings.escapeQuotes(input), input);

            for (final char quoteChar : quoteChars) {
                assertEquals(escapeQuotesReference(input, quoteChar, false), Strings.escapeQuotes(input, quoteChar), input);
            }
        }

        for (int codePoint = Character.MIN_CODE_POINT; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                final String whitespace = new String(Character.toChars(codePoint));
                assertEquals("a b", Strings.normalizeSpace("a" + whitespace + "b"), "U+" + Integer.toHexString(codePoint));
                assertEquals("", Strings.normalizeSpace(whitespace), "U+" + Integer.toHexString(codePoint));
            }
        }

        for (int value = Character.MIN_VALUE; value <= Character.MAX_VALUE; value++) {
            final char ch = (char) value;

            if (Character.isWhitespace(ch)) {
                assertEquals("ab", Strings.removeWhitespace("a" + ch + "b"), "U+" + Integer.toHexString(value));
            }
        }

        for (int runLength = 0; runLength <= 20; runLength++) {
            final String input = "x" + "\\".repeat(runLength) + "y";
            assertEquals(escapeQuotesReference(input, '\\', false), Strings.escapeQuotes(input, '\\'), "runLength=" + runLength);
        }
    }

    @Test
    public void testAdaptiveCharacterSearches_ExhaustiveShortInputsAndThresholdBoundary() throws ReflectiveOperationException {
        final char[] alphabet = { '\0', 'a', 'b', '\uD800', '\uDC00', '\uFFFF' };
        final List<String> haystacks = shortStrings(alphabet, 3);
        final List<String> candidateStrings = shortStrings(alphabet, 2);

        for (final String haystack : haystacks) {
            final int[] searchStarts = { -2, 0, 1, haystack.length() - 1, haystack.length(), haystack.length() + 1 };

            for (final String candidateString : candidateStrings) {
                final char[] candidates = candidateString.toCharArray();

                for (final int searchStart : searchStarts) {
                    assertEquals(indexOfAnyReference(haystack, searchStart, candidates), Strings.indexOfAny(haystack, searchStart, candidates));
                    assertEquals(minIndexOfAllReference(haystack, searchStart, candidates), Strings.minIndexOfAll(haystack, searchStart, candidates));
                    assertEquals(lastIndexOfAnyReference(haystack, searchStart, candidates), Strings.lastIndexOfAny(haystack, searchStart, candidates));
                    assertEquals(maxLastIndexOfAllReference(haystack, searchStart, candidates), Strings.maxLastIndexOfAll(haystack, searchStart, candidates));
                }

                assertEquals(containsAllCharsReference(haystack, candidates), Strings.containsAll(haystack, candidates));
                assertEquals(containsNoneCharsReference(haystack, candidates), Strings.containsNone(haystack, candidates));
                assertEquals(!haystack.isEmpty() && candidates.length > 0 && !containsNoneCharsReference(haystack, candidates),
                        Strings.containsAny(haystack, candidates));
                assertEquals(haystack.isEmpty() || candidates.length > 0 && indexOfAnyButReference(haystack, 0, candidates) < 0,
                        Strings.containsOnly(haystack, candidates));
            }
        }

        final Method crossover = Strings.class.getDeclaredMethod("shouldUseCharMembership", int.class, int.class);
        crossover.setAccessible(true);
        assertFalse((boolean) crossover.invoke(null, 511, 8));
        assertTrue((boolean) crossover.invoke(null, 512, 8));
        assertFalse((boolean) crossover.invoke(null, 512, 7));

        final String thresholdHaystack = "a".repeat(512);
        final char[] orderedCandidates = { 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'a' };
        assertEquals(0, Strings.indexOfAny(thresholdHaystack, orderedCandidates));
        assertEquals(0, Strings.minIndexOfAll(thresholdHaystack, orderedCandidates));
        assertEquals(511, Strings.lastIndexOfAny(thresholdHaystack, orderedCandidates));
        assertEquals(511, Strings.maxLastIndexOfAll(thresholdHaystack, orderedCandidates));
        assertEquals(0, Strings.lastIndexOfAny("ab", 'a', 'b')); // candidate order, not the rightmost candidate
    }

    @Test
    public void testIgnoreCaseKmp_ExactCrossoverAndBmpFoldEquivalence() throws ReflectiveOperationException {
        final Method crossover = Strings.class.getDeclaredMethod("shouldUseIgnoreCaseKmp", int.class, int.class);
        crossover.setAccessible(true);
        assertFalse((boolean) crossover.invoke(null, 255, 16));
        assertTrue((boolean) crossover.invoke(null, 256, 16));
        assertFalse((boolean) crossover.invoke(null, 4096, 15));

        final String target = "a".repeat(15) + "b";
        final String belowCrossover = "a".repeat(254) + "B";
        final String atCrossover = "x" + belowCrossover;

        for (final String input : new String[] { belowCrossover, atCrossover }) {
            assertEquals(indexOfIgnoreCaseReference(input, target, 0), Strings.indexOfIgnoreCase(input, target));
            assertEquals(lastIndexOfIgnoreCaseReference(input, target, input.length()), Strings.lastIndexOfIgnoreCase(input, target));
            assertEquals(countMatchesIgnoreCaseReference(input, target), Strings.countMatchesIgnoreCase(input, target));
            assertArrayEquals(indicesOfIgnoreCaseReference(input, target, 0), Strings.indicesOfIgnoreCase(input, target).toArray());
            assertEquals(replaceIgnoreCaseReference(input, 0, target, "<match>", -1), Strings.replaceAllIgnoreCase(input, target, "<match>"));
        }

        final String filler = "\0".repeat(240);

        for (int value = Character.MIN_VALUE; value <= Character.MAX_VALUE; value++) {
            final char ch = (char) value;
            final char folded = Character.toLowerCase(Character.toUpperCase(ch));
            final String input = filler + String.valueOf(ch).repeat(16);
            final String foldedTarget = String.valueOf(folded).repeat(16);
            assertEquals(indexOfIgnoreCaseReference(input, foldedTarget, 0), Strings.indexOfIgnoreCase(input, foldedTarget), "U+" + Integer.toHexString(value));
        }

        final String loneHighSurrogateTarget = ("\uD800a").repeat(8);
        final String malformedInput = "x".repeat(240) + ("\uD800A").repeat(8) + "\uDC00";
        assertEquals(indexOfIgnoreCaseReference(malformedInput, loneHighSurrogateTarget, 0),
                Strings.indexOfIgnoreCase(malformedInput, loneHighSurrogateTarget));
        assertEquals(lastIndexOfIgnoreCaseReference(malformedInput, loneHighSurrogateTarget, malformedInput.length()),
                Strings.lastIndexOfIgnoreCase(malformedInput, loneHighSurrogateTarget));
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
    public void testStrUtilOrElse_CharDelimiterOverloads() {
        assertEquals("world", StrUtil.substringAfterOrElse("hello.world", '.', "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringAfterOrElse("hello.", '.', "default"));
        assertEquals("default", StrUtil.substringAfterOrElse(null, '.', "default"));

        assertEquals("java", StrUtil.substringAfterLastOrElse("hello.world.java", '.', "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringAfterLastOrElse("hello.world.", '.', "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse(null, '.', "default"));

        assertEquals("hello", StrUtil.substringBeforeOrElse("hello.world", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringBeforeOrElse(".world", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse(null, '.', "default"));

        assertEquals("hello.world", StrUtil.substringBeforeLastOrElse("hello.world.java", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("hello", '.', "default"));
        assertEquals("", StrUtil.substringBeforeLastOrElse(".world", '.', "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse(null, '.', "default"));

        // the char overload must agree with the String overload for a single-character delimiter
        assertEquals(StrUtil.substringAfterOrElse("a.b.c", ".", "x"), StrUtil.substringAfterOrElse("a.b.c", '.', "x"));
        assertEquals(StrUtil.substringBeforeLastOrElse("a.b.c", ".", "x"), StrUtil.substringBeforeLastOrElse("a.b.c", '.', "x"));
    }

    @Test
    public void testStrUtilIgnoreCaseOptionalWrappers() {
        assertEquals("WORLD", StrUtil.substringAfterIgnoreCase("Hello WORLD", "hello ").get());
        assertFalse(StrUtil.substringAfterIgnoreCase("NoMatch", "xyz").isPresent());
        assertFalse(StrUtil.substringAfterIgnoreCase(null, "x").isPresent());

        assertEquals("Test", StrUtil.substringAfterLastIgnoreCase("com.Example.Test", ".example.").get());
        assertFalse(StrUtil.substringAfterLastIgnoreCase("hello", ".").isPresent());

        assertEquals("User", StrUtil.substringBeforeIgnoreCase("User@Example.com", "@EXAMPLE").get());
        assertFalse(StrUtil.substringBeforeIgnoreCase("no-match", "@").isPresent());

        assertEquals("Com.Example", StrUtil.substringBeforeLastIgnoreCase("Com.Example.TEST", ".test").get());
        assertFalse(StrUtil.substringBeforeLastIgnoreCase("no-match", "@").isPresent());

        assertEquals("CONTENT", StrUtil.substringBetweenIgnoreCase("#CONTENT#", "#").get());
        assertFalse(StrUtil.substringBetweenIgnoreCase("[[only-one", "[[").isPresent());

        assertEquals("content", StrUtil.substringBetweenIgnoreCase("<TAG>content</TAG>", "<tag>", "</tag>").get());
        assertFalse(StrUtil.substringBetweenIgnoreCase("[[no-end", "[[", "]]").isPresent());

        assertEquals("text2", StrUtil.substringBetweenIgnoreCase("<A>text1</A><A>text2</A>", 5, "<a>", "</a>").get());
        assertFalse(StrUtil.substringBetweenIgnoreCase("test", 10, "t", "t").isPresent());

        // each wrapper must mirror the Strings method it delegates to
        assertEquals(Strings.substringAfterIgnoreCase("Hello WORLD", "hello "), StrUtil.substringAfterIgnoreCase("Hello WORLD", "hello ").orElseNull());
        assertNull(StrUtil.substringBeforeIgnoreCase("no-match", "@").orElseNull());
    }

    @Test
    public void testMaxIndexOfAll_CharMembershipFastPath() {
        // shouldUseCharMembership requires >= 8 candidates and length * count >= 4096,
        // so this input exercises the bitset path rather than the per-candidate indexOf loop.
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 600; i++) {
            sb.append((char) ('a' + (i * 7) % 12));
        }

        final String str = sb.toString();
        final char[] candidates = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'z', 'y' };

        for (final int fromIndex : new int[] { -5, 0, 1, 7, 123, 599, 600 }) {
            int expected = CommonUtil.INDEX_NOT_FOUND;

            for (final char ch : candidates) {
                expected = Math.max(expected, str.indexOf(ch, Math.max(0, fromIndex)));
            }

            assertEquals(expected, Strings.maxIndexOfAll(str, fromIndex, candidates), "fromIndex=" + fromIndex);
        }

        // duplicates and candidates far outside the membership word range must not disturb the result
        assertEquals(2, Strings.maxIndexOfAll("abcabc", 0, 'a', 'a', 'b', 'b', 'c', 'c', '中', '￿', 'z', 'a'));
        // a candidate that never occurs contributes nothing
        assertEquals(-1, Strings.maxIndexOfAll("abcabc", 0, 'x', 'y', 'z', 'w', 'v', 'u', 't', 's', 'r', 'q'));
    }

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
    public void testCharCaseOverloadsUseSimpleMapping() {
        // Documented divergence: a char cannot hold a length-changing mapping, so the char overloads use
        // Character.toLowerCase/toUpperCase while the String overloads apply the full Locale.ROOT mapping.
        assertEquals('i', Strings.toLowerCase((char) 0x0130)); // simple: one char
        assertEquals("i̇", Strings.toLowerCase(Character.toString(0x0130))); // full: expands to two

        assertEquals((char) 0x00DF, Strings.toUpperCase((char) 0x00DF)); // simple: unchanged
        assertEquals("SS", Strings.toUpperCase(Character.toString(0x00DF))); // full: expands to two

        // Where no expansion applies the two agree.
        for (final char ch : new char[] { 'a', 'Z', '1', ' ', (char) 0x03A9, (char) 0x03C9 }) {
            assertEquals(String.valueOf(Strings.toLowerCase(ch)), Strings.toLowerCase(String.valueOf(ch)));
            assertEquals(String.valueOf(Strings.toUpperCase(ch)), Strings.toUpperCase(String.valueOf(ch)));
        }
    }

    @Test
    public void testLenientFormatNullArgumentIsRendered() {
        // lenientToString handles null before its try block; its recovery path dereferences the argument, so a
        // null reaching it would throw an NPE out of the catch and defeat the point of "lenient" formatting.
        assertEquals("value=null", Strings.lenientFormat("value=%s", (Object) null));
        assertEquals("a=null b=null", Strings.lenientFormat("a=%s b=%s", null, null));
        assertEquals("none: [null]", Strings.lenientFormat("none", (Object) null));
        assertEquals("null: [null]", Strings.lenientFormat(null, (Object) null));
    }

    @Test
    public void testWhitespacePredicateOverloadsAgree() {
        // Pins the class-level "Whitespace predicate" note: strip/splitOnWhitespace/removeWhitespace scan UTF-16
        // code units while isBlank/isWhitespace/mapWords scan code points, and the two select the same characters.
        for (int cp = Character.MIN_SUPPLEMENTARY_CODE_POINT; cp <= Character.MAX_CODE_POINT; cp++) {
            assertFalse(Character.isWhitespace(cp), "U+" + Integer.toHexString(cp));
        }

        for (int cp = 0; cp <= Character.MAX_VALUE; cp++) {
            assertEquals(Character.isWhitespace((char) cp), Character.isWhitespace(cp), "U+" + Integer.toHexString(cp));
        }
    }

    @Test
    public void testCaseCodePoint_bmp() {
        assertEquals(0x0061, Strings.toLowerCase(0x0041));
        assertEquals(0x0041, Strings.toUpperCase(0x0061));

        assertEquals(0x007A, Strings.toLowerCase(0x005A));
        assertEquals(0x005A, Strings.toUpperCase(0x007A));

        // already in the target case
        assertEquals(0x0061, Strings.toLowerCase(0x0061));
        assertEquals(0x0041, Strings.toUpperCase(0x0041));

        // no case mapping at all
        assertEquals(0x0031, Strings.toLowerCase(0x0031));
        assertEquals(0x0031, Strings.toUpperCase(0x0031));
        assertEquals(0x0020, Strings.toLowerCase(0x0020));
        assertEquals(0x0020, Strings.toUpperCase(0x0020));

        // Greek
        assertEquals(0x03C9, Strings.toLowerCase(0x03A9));
        assertEquals(0x03A9, Strings.toUpperCase(0x03C9));

        // title case Dz: lowercases to dz, uppercases to DZ
        assertEquals(0x01C6, Strings.toLowerCase(0x01C5));
        assertEquals(0x01C4, Strings.toUpperCase(0x01C5));

        // Turkish dotless i uppercases to plain I
        assertEquals(0x0049, Strings.toUpperCase(0x0131));

        // Cherokee: the lowercase letters live far from the capitals
        assertEquals(0xAB70, Strings.toLowerCase(0x13A0));
    }

    @Test
    public void testCaseCodePoint_supplementary() {
        // Deseret
        assertEquals(0x10428, Strings.toLowerCase(0x10400));
        assertEquals(0x10400, Strings.toUpperCase(0x10428));

        // Adlam
        assertEquals(0x1E922, Strings.toLowerCase(0x1E900));
        assertEquals(0x1E900, Strings.toUpperCase(0x1E922));

        // Warang Citi
        assertEquals(0x118C0, Strings.toLowerCase(0x118A0));
        assertEquals(0x118A0, Strings.toUpperCase(0x118C0));

        // The char overloads see only the surrogate halves of these, and surrogates have no case mapping.
        final char high = Character.highSurrogate(0x10400);
        final char low = Character.lowSurrogate(0x10400);
        assertEquals(high, Strings.toLowerCase(high));
        assertEquals(low, Strings.toLowerCase(low));
    }

    @Test
    public void testCaseCodePoint_simpleMappingDivergesFromString() {
        // U+0130 LATIN CAPITAL LETTER I WITH DOT ABOVE: the full mapping expands to "i" + U+0307.
        final String dottedCapitalI = String.valueOf((char) 0x0130);
        assertEquals(0x0069, Strings.toLowerCase(0x0130));
        assertEquals(2, Strings.toLowerCase(dottedCapitalI).length());
        assertEquals(0x0069, Strings.toLowerCase(dottedCapitalI).charAt(0));
        assertEquals(0x0307, Strings.toLowerCase(dottedCapitalI).charAt(1));

        // U+00DF LATIN SMALL LETTER SHARP S: the full mapping expands to "SS"; the simple one is a no-op.
        assertEquals(0x00DF, Strings.toUpperCase(0x00DF));
        assertEquals("SS", Strings.toUpperCase(String.valueOf((char) 0x00DF)));

        // U+FB01 LATIN SMALL LIGATURE FI: the full mapping expands to "FI"; the simple one is a no-op.
        assertEquals(0xFB01, Strings.toUpperCase(0xFB01));
        assertEquals("FI", Strings.toUpperCase(String.valueOf((char) 0xFB01)));
    }

    @Test
    public void testCaseCodePoint_surrogatesAndBoundaries() {
        for (int cp = Character.MIN_SURROGATE; cp <= Character.MAX_SURROGATE; cp++) {
            assertEquals(cp, Strings.toLowerCase(cp), "U+" + Integer.toHexString(cp));
            assertEquals(cp, Strings.toUpperCase(cp), "U+" + Integer.toHexString(cp));
        }

        assertEquals(Character.MIN_CODE_POINT, Strings.toLowerCase(Character.MIN_CODE_POINT));
        assertEquals(Character.MIN_CODE_POINT, Strings.toUpperCase(Character.MIN_CODE_POINT));
        assertEquals(Character.MAX_CODE_POINT, Strings.toLowerCase(Character.MAX_CODE_POINT));
        assertEquals(Character.MAX_CODE_POINT, Strings.toUpperCase(Character.MAX_CODE_POINT));
    }

    @Test
    public void testCaseCodePoint_invalid() {
        for (final int invalid : new int[] { -1, Integer.MIN_VALUE, Character.MAX_CODE_POINT + 1, Integer.MAX_VALUE }) {
            assertThrows(IllegalArgumentException.class, () -> Strings.toLowerCase(invalid));
            assertThrows(IllegalArgumentException.class, () -> Strings.toUpperCase(invalid));
        }
    }

    @Test
    public void testCaseCodePoint_matchesCharacterEverywhere() {
        for (int cp = Character.MIN_CODE_POINT; cp <= Character.MAX_CODE_POINT; cp++) {
            assertEquals(Character.toLowerCase(cp), Strings.toLowerCase(cp), "U+" + Integer.toHexString(cp));
            assertEquals(Character.toUpperCase(cp), Strings.toUpperCase(cp), "U+" + Integer.toHexString(cp));
        }
    }

    @Test
    public void testCaseCodePoint_charAndIntOverloadsAgree() {
        // Character.toLowerCase(char) is a narrowing cast of Character.toLowerCase(int), so the two would part
        // company only if some BMP code point mapped to a supplementary one - none does, and this pins that.
        for (int cp = 0; cp <= Character.MAX_VALUE; cp++) {
            assertEquals(Strings.toLowerCase((char) cp), Strings.toLowerCase(cp), "U+" + Integer.toHexString(cp));
            assertEquals(Strings.toUpperCase((char) cp), Strings.toUpperCase(cp), "U+" + Integer.toHexString(cp));
        }
    }

    @Test
    public void testCaseCodePoint_charOverloadStillWins() {
        // These assignments compile only while a char argument still binds toLowerCase(char)/toUpperCase(char):
        // the int overload returns int, which does not narrow to char without a cast.
        final char lower = Strings.toLowerCase('A');
        final char upper = Strings.toUpperCase('a');
        assertEquals('a', lower);
        assertEquals('A', upper);

        // A boxed Character unboxes to char, and char is more specific than int, so it binds the char overload too.
        final Character boxed = 'A';
        final char fromBoxed = Strings.toLowerCase(boxed);
        assertEquals('a', fromBoxed);

        // A byte/short widens to int, which only the new overload accepts.
        final byte b = 'A';
        assertEquals(0x61, Strings.toLowerCase(b));
    }

    @Test
    public void testTrimVersusEmptyOmissionOrderAcrossSplitFamilies() {
        // The split family decides empty-token omission on the RAW token, so a whitespace-only token
        // survives (as "") once trimming runs.
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a,   ,b", ',', 5, true));
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.split("a::   ::b", "::", 5, true));

        // splitToLines decides omission AFTER trimming, so the same whitespace-only element is dropped.
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\n   \nb", true, true));
        // ... and is kept as "" when omitEmptyLines is false.
        assertArrayEquals(new String[] { "a", "", "b" }, Strings.splitToLines("a\n   \nb", true, false));

        // Without trimming the two families agree: a whitespace-only element is non-empty and is kept.
        assertArrayEquals(new String[] { "a", "   ", "b" }, Strings.split("a,   ,b", ',', false));
        assertArrayEquals(new String[] { "a", "   ", "b" }, Strings.splitToLines("a\n   \nb", false, true));
    }

    @Test
    public void testPadToDisplayWidth_padSpaceAbsorbedByBoundaryCluster() {
        // A leading Extend/SpacingMark code point pulls the adjacent pad space into its own grapheme cluster,
        // and a cluster is only as wide as its widest code point, so that space used to add no column at all.
        final String emojiModifier = new String(Character.toChars(0x1F3FB)); // EMOJI MODIFIER FITZPATRICK TYPE-1-2 (Extend, width 2)
        final String devanagariAa = new String(Character.toChars(0x093E)); // DEVANAGARI VOWEL SIGN AA (SpacingMark, width 1)
        final String malayalamDotReph = new String(Character.toChars(0x0D4E)); // MALAYALAM LETTER DOT REPH (Prepend, width 1)
        final String zhongWen = new String(Character.toChars(0x4E2D)) + new String(Character.toChars(0x6587));

        final String[] inputs = { emojiModifier, devanagariAa, malayalamDotReph, emojiModifier + "ab", devanagariAa + "ka", "ab" + malayalamDotReph };

        for (int minWidth = 0; minWidth <= 8; minWidth++) {
            for (final String s : inputs) {
                final String padStarted = Strings.padStartToDisplayWidth(s, minWidth);
                final String padEnded = Strings.padEndToDisplayWidth(s, minWidth);

                assertTrue(Strings.displayWidth(padStarted) >= minWidth, "padStartToDisplayWidth width " + Strings.displayWidth(padStarted) + " < " + minWidth);
                assertTrue(Strings.displayWidth(padEnded) >= minWidth, "padEndToDisplayWidth width " + Strings.displayWidth(padEnded) + " < " + minWidth);
                assertTrue(padStarted.endsWith(s), "padStartToDisplayWidth must keep the input as a suffix");
                assertTrue(padEnded.startsWith(s), "padEndToDisplayWidth must keep the input as a prefix");
            }
        }

        // The exact off-by-one that used to occur.
        assertEquals(4, Strings.displayWidth(Strings.padStartToDisplayWidth(emojiModifier, 4)));
        assertEquals(3, Strings.displayWidth(Strings.padStartToDisplayWidth(devanagariAa, 3)));
        assertEquals(3, Strings.displayWidth(Strings.padEndToDisplayWidth(malayalamDotReph, 3)));

        // Ordinary text keeps landing exactly on minDisplayWidth, with no compensating space.
        assertEquals("   ab", Strings.padStartToDisplayWidth("ab", 5));
        assertEquals("ab   ", Strings.padEndToDisplayWidth("ab", 5));
        assertEquals(6, Strings.displayWidth(Strings.padStartToDisplayWidth(zhongWen, 6)));
        assertEquals(6, Strings.displayWidth(Strings.padEndToDisplayWidth(zhongWen, 6)));
        assertEquals("  " + zhongWen, Strings.padStartToDisplayWidth(zhongWen, 6));
    }

    @Test
    public void testPadToDisplayWidth_postconditionAcrossAllCodePoints() {
        for (int codePoint = 0; codePoint <= Character.MAX_CODE_POINT; codePoint++) {
            if (codePoint >= Character.MIN_SURROGATE && codePoint <= Character.MAX_SURROGATE) {
                continue;
            }

            final String s = new String(Character.toChars(codePoint));
            final int width = Strings.displayWidth(s);

            // Only widths just above the input width can expose an absorbed pad space.
            for (int minWidth = width + 1; minWidth <= width + 2; minWidth++) {
                if (Strings.displayWidth(Strings.padStartToDisplayWidth(s, minWidth)) < minWidth) {
                    fail("padStartToDisplayWidth fell short for U+" + Integer.toHexString(codePoint) + " at minWidth=" + minWidth);
                }

                if (Strings.displayWidth(Strings.padEndToDisplayWidth(s, minWidth)) < minWidth) {
                    fail("padEndToDisplayWidth fell short for U+" + Integer.toHexString(codePoint) + " at minWidth=" + minWidth);
                }
            }
        }
    }

    /**
     * C-019: an empty delimiter matches twice at index zero, so {@code substringBetweenIgnoreCase} answers
     * {@code ""} for every non-{@code null} string - the one input for which it never returns {@code null}.
     * Its javadoc listed only the {@code null} outcomes; the non-ignore-case twin already documented this.
     */
    @Test
    public void test20260906_substringBetweenIgnoreCaseWithAnEmptyDelimiter() {
        assertEquals("", Strings.substringBetweenIgnoreCase("test", ""));
        assertEquals("", Strings.substringBetweenIgnoreCase("", ""));
        assertEquals(Strings.substringBetween("test", ""), Strings.substringBetweenIgnoreCase("test", ""));
        assertEquals(Strings.substringBetween("", ""), Strings.substringBetweenIgnoreCase("", ""));

        // the documented null outcomes are unchanged
        assertNull(Strings.substringBetweenIgnoreCase("test", null));
        assertNull(Strings.substringBetweenIgnoreCase(null, ""));
        assertNull(Strings.substringBetweenIgnoreCase("no-match", "[["));
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

    /**
     * C2-S1: {@code isBase64Mime} documents, in three places, that everything it accepts is decodable by
     * {@code base64MimeDecode}, and actively recommends validate-then-decode. It skipped MIME whitespace
     * wherever it appeared, including <i>between</i> the two padding characters of a final two-symbol
     * quantum - where the JDK MIME decoder requires the second {@code =} to follow the first immediately and
     * throws. A differential over 3,000 whitespace/padding/body combinations found 300 such inputs.
     */
    @Test
    public void test20260906c3_isBase64MimeRejectsWhitespaceSplittingThePadding() {
        for (final String broken : new String[] { "QQ= =", "QQ=\t=", "QQ=\r\n=", "QQ=  =", "QQ = =" }) {
            assertFalse(Strings.isBase64Mime(broken), () -> "should reject: " + broken);
            assertFalse(Strings.isBase64Mime(broken.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            assertThrows(IllegalArgumentException.class, () -> Strings.base64MimeDecode(broken));
        }

        // whitespace before and after the padding is still accepted - the decoder accepts both
        for (final String ok : new String[] { "QQ==", "SGVs bG8=", "QQ== ", "A B C D", "QUJD", "SGVsbG8=", "QQ\r\n==" }) {
            assertTrue(Strings.isBase64Mime(ok), () -> "should accept: " + ok);
            assertNotNull(Strings.base64MimeDecode(ok));
        }

        // the documented invariant itself, asserted directly
        for (final String s : new String[] { "QQ==", "QQ= =", "SGVs bG8=", "QQ== ", "QQ=\t=", "A B C D", "QUJ =", "QQ" }) {
            if (Strings.isBase64Mime(s)) {
                assertNotNull(Strings.base64MimeDecode(s), () -> "accepted but not decodable: " + s);
            }
        }
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
}
