package com.landawn.abacus.util;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntUnaryOperator;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Strings.DelimiterMatchMode;
import com.landawn.abacus.util.Strings.StrUtil;

public class StringsSubstringTest extends StringsTestSupport {
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
    public void testSubstringOrElse() {
        assertEquals("bcd", StrUtil.substringOrElse("abcdef", 1, 4, "default"));
        assertEquals("default", StrUtil.substringOrElse("abc", 10, 20, "default"));
    }

    @Test
    public void testSubstring_WithIntUnaryOperator_FuncBegin() {
        // substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substring("hello", len -> 0, 5);
        assertEquals("hello", result);
    }

    @Test
    public void testSubstringOrElse_FuncBegin() {
        // substringOrElse(str, funcOfInclusiveBeginIndex, exclusiveEndIndex, defaultStr)
        String result = StrUtil.substringOrElse("hello", len -> 0, 5, "default");
        assertEquals("hello", result);
    }

    @Test
    public void testSubstring_IntUnaryOperatorAndInt() {
        // substring(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = Strings.substring("hello world", beginIdx -> 0, 5);
        assertEquals("hello", result);
    }

    @Test
    public void testSubstring_BeginIndex() {
        assertEquals("def", Strings.substring("abcdef", 3));
        assertEquals("abcdef", Strings.substring("abcdef", 0));
        assertEquals("", Strings.substring("abcdef", 6));
        assertNull(Strings.substring(null, 0));
    }

    @Test
    public void testSubstring_BeginEndIndex() {
        assertEquals("bcd", Strings.substring("abcdef", 1, 4));
        assertEquals("abc", Strings.substring("abcdef", 0, 3));
        assertEquals("", Strings.substring("abcdef", 2, 2));
        assertNull(Strings.substring(null, 0, 3));
    }

    @Test
    public void testSubstringOrElseItself() {
        assertEquals("bcd", StrUtil.substringOrElseItself("abcdef", 1, 4));
        assertEquals("abc", StrUtil.substringOrElseItself("abc", 10, 20));
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
    public void testSubstringBoundaryConditions() {
        Assertions.assertEquals("test", Strings.substring("test", 0, 4));
        Assertions.assertEquals("", Strings.substring("test", 4, 4));
        Assertions.assertEquals("est", Strings.substring("test", 1, 4));

        Assertions.assertNull(Strings.substring("test", 5, 10));
        Assertions.assertNull(Strings.substring("test", -1, 4));
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
    public void testSubstringOrElseItself_FuncBegin() {
        // substringOrElseItself(str, funcOfInclusiveBeginIndex, exclusiveEndIndex)
        String result = StrUtil.substringOrElseItself("hello", len -> 0, 5);
        assertEquals("hello", result);
    }

    @Test
    public void testSubstring_EdgeCases() {
        assertEquals("bc", substring("abc", 1));
        assertEquals("b", substring("abc", 1, 2));
        assertNull(substring(null, 1));
        assertEquals("", substring("abc", 3));
    }

    @Test
    public void testSubstring_IntAndIntUnaryOperator() {
        // substring(str, inclusiveBeginIndex, funcOfExclusiveEndIndex)
        String result = Strings.substring("hello world", 6, endIdx -> endIdx + 2);
        assertNotNull(result);
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
    public void testSubstringAfterOrElse() {
        assertEquals("def", StrUtil.substringAfterOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("abc", ":", "default"));
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
    public void testSubstringAfterOrElseItself() {
        assertEquals("def", StrUtil.substringAfterOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringAfterOrElseItself("abc", ':'));
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
    public void testSubstringAfter_WithExclusiveEndIndex() {
        // substringAfter(str, delimiter, exclusiveEndIndex)
        assertEquals("ell", Strings.substringAfter("hello world", "h", 4));
        assertNull(Strings.substringAfter(null, "h", 4));
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

    @Test
    public void testSubstringAfter_EdgeCases() {
        assertEquals("def", substringAfter("abc.def", "."));
        assertNull(substringAfter(null, "."));
        assertNull(substringAfter("abc", null));
        assertNull(substringAfter("abc", "xyz"));
    }

    @Test
    public void testSubstringAfterIgnoreCase() {
        assertEquals("WORLD", Strings.substringAfterIgnoreCase("hello WORLD", "hello "));
        assertEquals("world", Strings.substringAfterIgnoreCase("Hello world", "HELLO "));
        assertNull(Strings.substringAfterIgnoreCase(null, "hello"));
    }

    @Test
    public void testSubstringAfterLastOrElse() {
        assertEquals("c", StrUtil.substringAfterLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("abc", ":", "default"));
    }

    @Test
    public void testSubstringAfterLast() {
        assertEquals("c", Strings.substringAfterLast("a:b:c", ':'));
        assertNull(Strings.substringAfterLast("abc", ':'));
        assertNull(Strings.substringAfterLast(null, ':'));
    }

    @Test
    public void testSubstringAfterLast_String() {
        assertEquals("c", Strings.substringAfterLast("a::b::c", "::"));
        assertNull(Strings.substringAfterLast(null, "::"));
    }

    @Test
    public void testSubstringAfterLastOrElseItself() {
        assertEquals("c", StrUtil.substringAfterLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringAfterLastOrElseItself("abc", ':'));
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
    public void testSubstringAfterLast_WithExclusiveEndIndex() {
        // substringAfterLast(str, delimiter, exclusiveEndIndex)
        assertEquals("ell", Strings.substringAfterLast("hello", "h", 4));
        assertNull(Strings.substringAfterLast(null, "h", 4));
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
    public void testSubstringBetween_charDelimiter_endIndexBeyondLength() {
        // exclusiveEndIndex greater than str.length() should not throw StringIndexOutOfBoundsException.
        assertEquals("sr", Strings.substringBetween("u@sr", '@', 100));
        assertEquals("ello", Strings.substringBetween("Hello", 'H', Integer.MAX_VALUE));
    }

    @Test
    public void testSubstringBetween_stringDelimiter_endIndexBeyondLength() {
        // exclusiveEndIndex greater than str.length() should not throw StringIndexOutOfBoundsException.
        assertEquals("c", Strings.substringBetween("ab@c", "@", 100));
        assertEquals("ello", Strings.substringBetween("Hello", "H", Integer.MAX_VALUE));
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
    public void testSubstringAfterLast_EdgeCases() {
        assertEquals("ghi", substringAfterLast("abc.def.ghi", "."));
        assertNull(substringAfterLast(null, "."));
        assertNull(substringAfterLast("abc", "xyz"));
    }

    @Test
    public void testSubstringAfterLastIgnoreCase() {
        assertEquals("C", Strings.substringAfterLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringAfterLastIgnoreCase(null, ":"));
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

    @Test
    public void testSubstringAfterAny_EdgeCases() {
        assertEquals("def,ghi", substringAfterAny("abc.def,ghi", "."));
        assertNull(substringAfterAny(null, "."));
    }

    @Test
    public void testSubstringBeforeOrElse() {
        assertEquals("abc", StrUtil.substringBeforeOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("abc", ":", "default"));
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
    public void testSubstringBeforeOrElseItself() {
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc", ':'));
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

    @Test
    public void testSubstringBefore_WithInclusiveBeginIndex() {
        // substringBefore(str, inclusiveBeginIndex, delimiter)
        assertEquals("ello", Strings.substringBefore("hello world", 1, " "));
        assertNull(Strings.substringBefore(null, 0, " "));
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

    @Test
    public void testSubstringBefore_EdgeCases() {
        assertEquals("abc", substringBefore("abc.def", "."));
        assertNull(substringBefore(null, "."));
        assertNull(substringBefore("abc", null));
        assertNull(substringBefore("abc", "xyz"));
    }

    @Test
    public void testSubstringBeforeIgnoreCase() {
        assertEquals("abc", Strings.substringBeforeIgnoreCase("abc:DEF", ":"));
        assertNull(Strings.substringBeforeIgnoreCase(null, ":"));
    }

    @Test
    public void testSubstringBeforeLastOrElse() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("abc", ":", "default"));
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
    public void testSubstringBeforeLastOrElseItself() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringBeforeLastOrElseItself("abc", ':'));
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
    public void testSubstringBeforeLast_WithInclusiveBeginIndex() {
        // substringBeforeLast(str, inclusiveBeginIndex, delimiter)
        assertEquals("ello", Strings.substringBeforeLast("hello world", 1, " "));
        assertNull(Strings.substringBeforeLast(null, 0, " "));
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
    public void testSubstringBeforeLast_EdgeCases() {
        assertEquals("abc.def", substringBeforeLast("abc.def.ghi", "."));
        assertNull(substringBeforeLast(null, "."));
        assertNull(substringBeforeLast("abc", "xyz"));
    }

    @Test
    public void testSubstringBeforeLastIgnoreCase() {
        assertEquals("a:b", Strings.substringBeforeLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringBeforeLastIgnoreCase(null, ":"));
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

    @Test
    public void testSubstringBeforeAny_EdgeCases() {
        assertEquals("abc", substringBeforeAny("abc.def,ghi", ".", ","));
        assertNull(substringBeforeAny(null, "."));
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
    public void testSubstringBetween() {
        assertEquals("b", Strings.substringBetween("a:b:c", ":"));
        assertNull(Strings.substringBetween("abc", ":"));
        assertNull(Strings.substringBetween(null, ":"));
    }

    @Test
    public void testSubstringBetween_DifferentDelimiters() {
        assertEquals("content", Strings.substringBetween("<tag>content</tag>", "<tag>", "</tag>"));
        assertNull(Strings.substringBetween(null, "<", ">"));
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
    public void testSubstringBetween_WithFromIndex() {
        // substringBetween(str, fromIndex, delimiterBegin, delimiterEnd)
        String result = Strings.substringBetween("a[b]c[d]", 3, "[", "]");
        assertEquals("d", result);
    }

    @Test
    public void testSubstringBetween_EdgeCases() {
        assertEquals("def", substringBetween("abc[def]ghi", "[", "]"));
        assertNull(substringBetween(null, "[", "]"));
        assertEquals("", substringBetween("ab", "a", "b"));
    }

    @Test
    public void testsubstringBetween_IntIndex() {
        String result = Strings.substringBetween("hello world", beginIdx -> 0, 8);
        assertEquals("ello wo", result);
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
    public void testsubstringBetween_IntIndex_02() {
        // str="hello world", exclusiveBeginIndex=4 (after 'o'), func returns next search position + 6
        String result = Strings.substringBetween("hello world", 4, endIdx -> endIdx + 6);
        // exclusiveBeginIndex=4 means start looking from index 5
        // funcOfExclusiveEndIndex maps from the first character's index to the end index
        assertEquals(" worl", result);
    }

    @Test
    public void testsubstringBetweenIgnoreCase() {
        assertEquals("content", Strings.substringBetweenIgnoreCase("<TAG>content</tag>", "<tag>", "</TAG>"));
        assertNull(Strings.substringBetweenIgnoreCase(null, "<", ">"));
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
    public void testSubstringBetweenIgnoreCase_WithFromIndex() {
        String result = Strings.substringBetweenIgnoreCase("aXbYcXdY", 3, "x", "y");
        assertEquals("d", result);
    }

    @Test
    public void testSubstringBetweenIgnoreCase_EdgeCases() {
        assertEquals("def", substringBetweenIgnoreCase("ABC[def]GHI", "[", "]"));
        assertNull(substringBetweenIgnoreCase(null, "a", "b"));
    }

    // ===== Tests for substringBetweenFirstAndLast methods in Strings class =====

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
    public void testSubstringBetweenFirstAndLast_SingleDelimiter_Adjacent() {
        assertEquals("", Strings.substringBetweenFirstAndLast("[[]]", "["));
        assertEquals("", Strings.substringBetweenFirstAndLast("####", "##"));
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
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_Nested() {
        assertEquals("outer<inner>content</inner>outer", Strings.substringBetweenFirstAndLast("<outer<inner>content</inner>outer>", "<", ">"));
        assertEquals("a[b[c]]d", Strings.substringBetweenFirstAndLast("[a[b[c]]d]", "[", "]"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_MultiplePairs() {
        assertEquals("first</tag><tag>second", Strings.substringBetweenFirstAndLast("<tag>first</tag><tag>second</tag>", "<tag>", "</tag>"));
        assertEquals("a](b)[c", Strings.substringBetweenFirstAndLast("[a](b)[c]", "[", "]"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_SameDelimiters() {
        assertEquals("content", Strings.substringBetweenFirstAndLast("|content|", "|", "|"));
        assertEquals("a|b", Strings.substringBetweenFirstAndLast("|a|b|", "|", "|"));
    }

    @Test
    public void testSubstringBetweenFirstAndLast_TwoDelimiters_EmptyResult() {
        assertEquals("", Strings.substringBetweenFirstAndLast("<>", "<", ">"));
        assertEquals("", Strings.substringBetweenFirstAndLast("[]", "[", "]"));
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

    @Test
    public void testSubstringIndicesBetween() {
        List<IndexRange> result = Strings.substringIndicesBetween("a:b:c", ':', ':');
        assertEquals(1, result.size());
        assertEquals(new IndexRange(2, 3), result.get(0));
    }

    @Test
    public void testSubstringIndicesBetween_CharsWithRange() {
        List<IndexRange> indices = substringIndicesBetween("3[a2[c]]2[a]", 0, 8, '[', ']');

        assertEquals(1, indices.size());
        assertEquals(new IndexRange(2, 6), indices.get(0));
    }

    @Test
    public void testSubstringIndicesBetween_StringsWithRange() {
        List<IndexRange> indices = substringIndicesBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>");

        assertEquals("text1", substringsBetween("<tag>text1</tag><tag>text2</tag>", 0, 16, "<tag>", "</tag>").get(0));
        assertEquals(1, indices.size());
        assertEquals(new IndexRange(5, 10), indices.get(0));

    }

    @Test
    public void test_substringIndicesBetween() {

        {
            assertEquals(List.of(new IndexRange(2, 6), new IndexRange(10, 11)),
                    Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(5, 6), new IndexRange(2, 7), new IndexRange(10, 11)),
                    Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(2, 7), new IndexRange(10, 11)),
                    Strings.substringIndicesBetween("3[a2[c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

        {
            assertEquals(List.of(new IndexRange(2, 5), new IndexRange(9, 10)),
                    Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(2, 5), new IndexRange(9, 10)),
                    Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(2, 5), new IndexRange(9, 10)),
                    Strings.substringIndicesBetween("3[a2c]]2[a]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

        {
            assertEquals(List.of(new IndexRange(1, 5)), Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(4, 5), new IndexRange(2, 6), new IndexRange(1, 8)),
                    Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(1, 8)), Strings.substringIndicesBetween("[[b[a]]c]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

        {
            assertEquals(List.of(new IndexRange(1, 5), new IndexRange(7, 8)),
                    Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.SEQUENTIAL));
            assertEquals(List.of(new IndexRange(4, 5), new IndexRange(7, 8), new IndexRange(2, 10)),
                    Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.ALL_LEVELS));
            assertEquals(List.of(new IndexRange(2, 10)), Strings.substringIndicesBetween("[[b[a][c]d]", '[', ']', DelimiterMatchMode.OUTERMOST_ONLY));
        }

    }

    @Test
    public void test_substringIndicesBetween_maxCount() {
        final String str = "3[a2[c]]2[a]";

        // SEQUENTIAL: maxCount simply limits sequential matches.
        assertEquals(List.of(new IndexRange(2, 6)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.SEQUENTIAL, 1));
        assertEquals(List.of(new IndexRange(2, 6), new IndexRange(10, 11)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.SEQUENTIAL, 2));

        // ALL_LEVELS: matches are reported inner-first, so maxCount limits in that discovery order.
        assertEquals(List.of(new IndexRange(5, 6)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 1));
        assertEquals(List.of(new IndexRange(5, 6), new IndexRange(2, 7)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.ALL_LEVELS, 2));

        // OUTERMOST_ONLY: nested matches must never be returned, even when maxCount stops the scan early.
        // Before the fix, maxCount=1 returned [[5, 6]] ("c"), a nested match that the strategy promises to ignore.
        assertEquals(List.of(new IndexRange(2, 7)), Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of(new IndexRange(2, 7), new IndexRange(10, 11)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(List.of(new IndexRange(2, 7), new IndexRange(10, 11)),
                Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 10));

        // Value-returning variant goes through the same code path.
        assertEquals(CommonUtil.toList("a2[c]"), Strings.substringsBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));

        // Regression: an enclosing match discovered after maxCount sibling matches must still replace them,
        // and the result must be trimmed to the first maxCount outermost matches.
        assertEquals(List.of(new IndexRange(1, 7)), Strings.substringIndicesBetween("[[a][b]]", 0, 8, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));
        assertEquals(List.of(new IndexRange(1, 2), new IndexRange(4, 10)),
                Strings.substringIndicesBetween("[x][[a][b]]", 0, 11, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 2));
        assertEquals(CommonUtil.toList("[a][b]"), Strings.substringsBetween("[[a][b]]", 0, 8, "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));

        // Unclosed outer delimiter: pending matches stay, but the result must still honor maxCount.
        final String unclosed = "[a[b]c[d]";
        assertEquals(List.of(new IndexRange(3, 4), new IndexRange(7, 8)),
                Strings.substringIndicesBetween(unclosed, 0, unclosed.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 10));
        assertEquals(List.of(new IndexRange(3, 4)),
                Strings.substringIndicesBetween(unclosed, 0, unclosed.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 1));

        // maxCount == 0 returns an empty list.
        assertTrue(Strings.substringIndicesBetween(str, 0, str.length(), "[", "]", DelimiterMatchMode.OUTERMOST_ONLY, 0).isEmpty());
    }

    @Test
    public void test_substringIndicesBetween_overlappingDelimiters() {
        // Regression: with multi-character delimiters whose occurrences overlap (begin "ab" and end "bc"
        // share the 'b' at index 3), the nested modes must not treat the overlapping begin as nested.
        // Before the fix, ALL_LEVELS/OUTERMOST_ONLY pushed a content start past the end delimiter and
        // threw IllegalArgumentException from IndexRange(4, 3).
        final List<IndexRange> expected = List.of(new IndexRange(2, 3));
        assertEquals(expected, Strings.substringIndicesBetween("ababc", "ab", "bc", DelimiterMatchMode.SEQUENTIAL));
        assertEquals(expected, Strings.substringIndicesBetween("ababc", "ab", "bc", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(expected, Strings.substringIndicesBetween("ababc", "ab", "bc", DelimiterMatchMode.OUTERMOST_ONLY));
        assertEquals(CommonUtil.toList("a"), Strings.substringsBetween("ababc", "ab", "bc", DelimiterMatchMode.ALL_LEVELS));

        // Same input through the range-bounded overload; the end delimiter must be wholly inside the range.
        assertEquals(expected, Strings.substringIndicesBetween("ababc", 0, 5, "ab", "bc", DelimiterMatchMode.ALL_LEVELS, 10));
        assertTrue(Strings.substringIndicesBetween("ababc", 0, 4, "ab", "bc", DelimiterMatchMode.ALL_LEVELS, 10).isEmpty());

        // Equal begin/end delimiters remain pairable.
        assertEquals(List.of(new IndexRange(3, 4)), Strings.substringIndicesBetween("xabxabx", "ab", "ab", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of(new IndexRange(3, 4)), Strings.substringIndicesBetween("xabxabx", "ab", "ab", DelimiterMatchMode.OUTERMOST_ONLY));

        // Non-overlapping multi-character delimiters still nest normally.
        assertEquals(List.of(new IndexRange(5, 6), new IndexRange(2, 9)),
                Strings.substringIndicesBetween("<<a<<b>>c>>", "<<", ">>", DelimiterMatchMode.ALL_LEVELS));
        assertEquals(List.of(new IndexRange(2, 9)), Strings.substringIndicesBetween("<<a<<b>>c>>", "<<", ">>", DelimiterMatchMode.OUTERMOST_ONLY));
    }

    @Test
    public void testSubstringIndicesBetween_Chars() {
        List<IndexRange> indices = substringIndicesBetween("3[a2[c]]2[a]", '[', ']');
        assertEquals(2, indices.size());
        assertEquals(new IndexRange(2, 6), indices.get(0));
        assertEquals(new IndexRange(10, 11), indices.get(1));

        assertTrue(substringIndicesBetween("abc", '[', ']').isEmpty());
        assertTrue(substringIndicesBetween(null, '[', ']').isEmpty());
        assertTrue(substringIndicesBetween("", '[', ']').isEmpty());
    }

    @Test
    public void testSubstringIndicesBetween_Strings() {
        List<IndexRange> indices = substringIndicesBetween("<tag>text1</tag>", "<tag>", "</tag>");

        assertEquals(1, indices.size());
        assertEquals(new IndexRange(5, 10), indices.get(0));

        assertTrue(substringIndicesBetween("abc", "<", ">").isEmpty());
        assertTrue(substringIndicesBetween(null, "<", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "", ">").isEmpty());
        assertTrue(substringIndicesBetween("test", "<", "").isEmpty());
    }

    @Test
    public void testSubstringBetween_IntIntOverload() {
        // substringBetween(str, exclusiveBeginIndex, exclusiveEndIndex)
        assertEquals("ell", Strings.substringBetween("hello", 0, 4));
        assertNull(Strings.substringBetween(null, 0, 4));
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
    public void testSubstringBeforeAfter_NotFound() {
        assertNull(Strings.substringAfter("hello", '@'));
        assertNull(Strings.substringAfter("hello", "@@"));
        assertNull(Strings.substringBefore("hello", '@'));
        assertNull(Strings.substringBefore("hello", "@@"));
        assertEquals("hello", Strings.substringAfter("hello", ""));
    }

    @Test
    public void testSubstringAfterIgnoreCase_StringDelimiter() {
        // normal / case-insensitive
        assertEquals("Example.com", Strings.substringAfterIgnoreCase("www.Example.com", "WWW."));
        assertEquals("World", Strings.substringAfterIgnoreCase("Hello::World", "::"));
        // not found
        assertNull(Strings.substringAfterIgnoreCase("NoDelimiter", "xyz"));
        // empty delimiter -> whole string
        assertEquals("test", Strings.substringAfterIgnoreCase("test", ""));
        // null
        assertNull(Strings.substringAfterIgnoreCase(null, "test"));
        assertNull(Strings.substringAfterIgnoreCase("test", null));
    }

    @Test
    public void testSubstringBeforeIgnoreCase_StringDelimiter() {
        // normal / case-insensitive
        assertEquals("user", Strings.substringBeforeIgnoreCase("user@Example.com", "@"));
        assertEquals("Hello", Strings.substringBeforeIgnoreCase("Hello World Java", " world"));
        // not found
        assertNull(Strings.substringBeforeIgnoreCase("no-delimiter", "@"));
        // leading delimiter -> empty
        assertEquals("", Strings.substringBeforeIgnoreCase("@leading", "@"));
        // empty delimiter -> empty
        assertEquals("", Strings.substringBeforeIgnoreCase("test", ""));
        // null
        assertNull(Strings.substringBeforeIgnoreCase(null, "@"));
        assertNull(Strings.substringBeforeIgnoreCase("test", null));
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
    public void testSubstringAfterLast_ExclusiveEndIndexRange() {
        // the '.' at index 11 is outside [0, 11), so the last in-range '.' is the one at index 3
        assertEquals("example", Strings.substringAfterLast("com.example.Test", ".", 11));
        assertEquals("Test", Strings.substringAfterLast("com.example.Test", ".", 100));
        assertEquals("c", Strings.substringAfterLast("a.b.c.d", ".", 5));
        assertEquals("", Strings.substringAfterLast("test.", ".", 5));
        assertNull(Strings.substringAfterLast("hello", ".", 5));
        assertNull(Strings.substringAfterLast("test", ".", -1));
    }
}
