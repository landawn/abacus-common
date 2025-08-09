/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.Splitter.MapSplitter;
import com.landawn.abacus.util.Strings.StrUtil;

public class NStringTest extends AbstractParserTest {

    private static final Random rand = new Random();

    @Test
    public void test_ImmutableList() {
        N.println(ImmutableList.of("a", "b", null, "c"));
        N.println(ImmutableSet.of("a", "b", null, "c"));
        N.println(ImmutableMap.of("a", null, null, "c"));

        assertEquals("[a, b, null, c]", Strings.join(ImmutableList.of("a", "b", null, "c"), ", ", "[", "]"));
        assertEquals("[a, b, null, c]", Strings.join(ImmutableSet.of("a", "b", null, "c"), ", ", "[", "]"));
    }

    @Test
    public void test_toJson() {
        final String str = "abc";
        final String json = N.toJson("abc");
        N.println(json);

        assertEquals(str, json);
    }

    @Test
    public void test_abbreviate() {
        assertTrue("".endsWith(""));
        assertTrue("".startsWith(""));

        final String[] b = N.copyThenSetAll(N.asArray("a", "b"), (i, s) -> Strings.strip(s));
        N.println(b);

        try {
            Strings.abbreviate("", 1);
            fail("Shuld throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        assertEquals("", Strings.abbreviate("", 5));

        assertEquals("a", Strings.abbreviate("a", 5));

        assertEquals("aa.", Strings.abbreviate("aa.", 5));
    }

    @Test
    public void test_abbreviate_2() {

        assertEquals(Strings.abbreviate(null, 0, 4), null);
        assertEquals(Strings.abbreviate("", 0, 4), "");
        assertEquals(Strings.abbreviate("abcdefghijklmno", -1, 10), "abcdefg...");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 0, 10), "abcdefg...");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 1, 10), "abcdefg...");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 4, 10), "abcdefg...");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 5, 10), "...fghi...");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 6, 10), "...ghij...");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 8, 10), "...ijklmno");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 10, 10), "...ijklmno");
        assertEquals(Strings.abbreviate("abcdefghijklmno", 12, 10), "...ijklmno");

    }

    @Test
    public void test_center() {
        assertEquals(Strings.center("a", 4, "yz"), "yzayz");
        assertEquals(Strings.center(null, 4, " "), "    ");
        assertEquals(Strings.center("", 4, " "), "    ");
        assertEquals(Strings.center("ab", 4, " "), " ab ");
        assertEquals(Strings.center("abcd", 2, " "), "abcd");
        assertEquals(Strings.center("a", 4, " "), " a  ");
        assertEquals(Strings.center("abc", 7, ""), "  abc  ");
    }

    @Test
    public void test_rotate() {
        assertEquals(Strings.rotate(null, 0), null);
        assertEquals(Strings.rotate("", 1), "");
        assertEquals(Strings.rotate("abcdefg", 0), "abcdefg");
        assertEquals(Strings.rotate("abcdefg", 2), "fgabcde");
        assertEquals(Strings.rotate("abcdefg", -2), "cdefgab");
        assertEquals(Strings.rotate("abcdefg", 7), "abcdefg");
        assertEquals(Strings.rotate("abcdefg", -7), "abcdefg");
        assertEquals(Strings.rotate("abcdefg", 9), "fgabcde");
        assertEquals(Strings.rotate("abcdefg", -9), "cdefgab");

        char[] chars = "abcdefg".toCharArray();
        N.rotate(chars, 0);
        assertEquals(String.valueOf(chars), "abcdefg");

        chars = "abcdefg".toCharArray();
        N.rotate(chars, 2);
        assertEquals(String.valueOf(chars), "fgabcde");

        chars = "abcdefg".toCharArray();
        N.rotate(chars, -2);
        assertEquals(String.valueOf(chars), "cdefgab");

        chars = "abcdefg".toCharArray();
        N.rotate(chars, 7);
        assertEquals(String.valueOf(chars), "abcdefg");

        chars = "abcdefg".toCharArray();
        N.rotate(chars, -7);
        assertEquals(String.valueOf(chars), "abcdefg");

        chars = "abcdefg".toCharArray();
        N.rotate(chars, 9);
        assertEquals(String.valueOf(chars), "fgabcde");

        chars = "abcdefg".toCharArray();
        N.rotate(chars, -9);
        assertEquals(String.valueOf(chars), "cdefgab");

        N.println(Strings.shuffle("abcdefg"));
    }

    @Test
    public void test_deleteAll_2() {
        final int maxNum = 100;
        for (int i = 0; i < 100; i++) {
            final int num = rand.nextInt(maxNum);
            final int b = N.min(10, num);
            final int[] indices = new int[b];

            for (int j = 0; j < b; j++) {
                indices[j] = rand.nextInt(num);
            }
            final List<Integer> list = IntList.range(0, maxNum).toList();
            final List<Integer> listA = new ArrayList<>(list);
            N.deleteAllByIndices(listA, indices);

            final IntList tmp = IntList.range(0, maxNum);
            tmp.removeAll(indices);
            final List<Integer> listB = tmp.toList();
            N.println(indices);
            N.println(listA);
            N.println(listB);
            assertEquals(listA, listB);
        }
    }

    @Test
    public void test_13() {
        final Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);
        final String str = Joiner.defauLt().appendEntries(map).toString();
        N.println(str);

        final Map<String, Integer> map2 = MapSplitter.defauLt().split(str, String.class, Integer.class);

        assertEquals(map, map2);
    }

    @Test
    public void test_joiner() {
        N.println(Joiner.defauLt().repeat(2, 10).toString());
        N.println(Joiner.defauLt().append(1).repeat(2, 10).toString());

        N.println(Joiner.defauLt().repeat(null, 10).toString());
        N.println(Joiner.defauLt().append(1).repeat(null, 10).toString());
    }

    @Test
    public void test_12() {
        assertEquals(1, N.compare(Array.of(1, 2, 3), Array.of(1, 2, 2)));
        assertEquals(-1, N.compare(Array.of(1, 2), Array.of(1, 2, 0)));
    }

    @Test
    public void test_11() {
        assertTrue(N.equals(Array.of('a', 'b', 'c', '1', '2', '3'), N.concat(Array.of('a'), Array.of('b', 'c'), Array.of('1', '2', '3'))));

        assertTrue(N.equals(N.asArray("a", "b", "c", "1", "2", "3"), N.concat(N.asArray("a"), N.asArray("b", "c"), N.asArray("1", "2", "3"))));
    }

    @Test
    public void test_10() {
        final Map<Map<Object, Object>, Map<Object, Object>> map = N.asMap(N.asMap("abc", 123D), N.asMap(123D, "abc"));
        String json = N.toJson(map);
        N.println(json);
        assertEquals(map,
                N.fromJson(json, JDC.create().setMapKeyType(N.typeOf("Map<String, Double>")).setMapValueType(N.typeOf("Map<Double, String>")), Map.class));

        final List<Map<String, Double>> list = N.asList(N.asMap("abc", 123D));
        json = N.toJson(list);
        N.println(json);
        assertTrue(N.equals(list, N.fromJson(json, JDC.create().setElementType(N.typeOf("Map<String, Double>")), List.class)));
        assertFalse(N.equals(list, N.fromJson(json, JDC.create().setElementType(N.typeOf("Map<String, Float>")), List.class)));
        // assertTrue(N.equals(list, N.fromJson(List.class, json)));
    }

    @Test
    public void test_parse_time() throws Exception {
        final SimpleDateFormat sdf = new SimpleDateFormat(Dates.ISO_8601_TIMESTAMP_FORMAT);

        String date = "2016-04-11T00:00:04.370Z";
        N.println(Dates.parseTimestamp(date).getTime());
        N.println(Dates.parseTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        assertEquals(1460332804370L, Dates.parseTimestamp(date).getTime());
        assertEquals(1460332804370L, Dates.parseTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        N.println(sdf.parse(date).getTime());

        date = "2016-04-11T00:00:04.385Z";
        N.println(Dates.parseTimestamp(date).getTime());
        N.println(Dates.parseTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        assertEquals(1460332804385L, Dates.parseTimestamp(date).getTime());
        assertEquals(1460332804385L, Dates.parseTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        N.println(sdf.parse(date).getTime());

    }

    @Test
    public void test_Mutable() {
        {
            final String str = N.stringOf(MutableBoolean.of(true));
            assertEquals(MutableBoolean.of(true), N.valueOf(str, MutableBoolean.class));
        }

        {
            final String str = N.stringOf(MutableChar.of('c'));
            assertEquals(MutableChar.of('c'), N.valueOf(str, MutableChar.class));
        }

        {
            final String str = N.stringOf(MutableByte.of((byte) 1));
            assertEquals(MutableByte.of((byte) 1), N.valueOf(str, MutableByte.class));
        }

        {
            final String str = N.stringOf(MutableShort.of((short) 1));
            assertEquals(MutableShort.of((short) 1), N.valueOf(str, MutableShort.class));
        }

        {
            final String str = N.stringOf(MutableInt.of(1));
            assertEquals(MutableInt.of(1), N.valueOf(str, MutableInt.class));
        }

        {
            final String str = N.stringOf(MutableLong.of(1));
            assertEquals(MutableLong.of(1), N.valueOf(str, MutableLong.class));
        }

        {
            final String str = N.stringOf(MutableFloat.of(1));
            assertEquals(MutableFloat.of(1), N.valueOf(str, MutableFloat.class));
        }

        {
            final String str = N.stringOf(MutableDouble.of(1));
            assertEquals(MutableDouble.of(1), N.valueOf(str, MutableDouble.class));
        }
    }

    @Test
    public void test_range() {
        final Range<Integer> range = Range.closed(1, 6);
        String str = N.stringOf(range);
        assertEquals(range, N.typeOf("Range<Integer>").valueOf(str));

        final MyEntity_1 myBean = new MyEntity_1();
        myBean.setRange(Range.closed(1.0f, 4.0f));

        str = N.stringOf(myBean);

        assertEquals(myBean, N.typeOf(MyEntity_1.class).valueOf(str));

        assertTrue(Range.just(1).isOverlappedBy(Range.closed(1, 2)));
    }

    @Test
    public void test_top() {
        {
            final int[] a = { 1, 5, 3, 7, 9, 2 };
            final int[] b = N.top(a, 3);
            assertTrue(N.equals(new int[] { 5, 7, 9 }, b));
        }

        {
            final long[] a = { 1, 5, 3, 7, 9, 2 };
            final long[] b = N.top(a, 3);
            assertTrue(N.equals(new long[] { 5, 7, 9 }, b));
        }

        {
            final float[] a = { 1, 5, 3, 7, 9, 2 };
            final float[] b = N.top(a, 3);
            assertTrue(N.equals(new float[] { 5, 7, 9 }, b));
        }

        {
            final double[] a = { 1, 5, 3, 7, 9, 2 };
            final double[] b = N.top(a, 3);
            assertTrue(N.equals(new double[] { 5, 7, 9 }, b));
        }

        {
            final String[] a = { "1", "5", "3", "7", "9", "2" };
            final List<String> b = N.top(a, 3);
            assertTrue(N.equals(N.asList("5", "7", "9"), b));
        }

        {
            final Set<String> c = N.asLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3);
            assertTrue(N.equals(N.asList("5", "7", "9"), b));
        }

        {
            final Set<String> c = N.asLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.nullsFirst());
            assertTrue(N.equals(N.asList("5", "7", "9"), b));
        }

        {
            final Set<String> c = N.asLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.nullsLast());
            assertTrue(N.equals(N.asList("5", "7", "9"), b));
        }

        {
            final Set<String> c = N.asLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.reverseOrder());
            N.println(b);
            assertTrue(N.equals(N.asList("3", "1", "2"), b));
        }

        {
            final Set<String> c = N.asLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.reverseOrder(), true);
            N.println(b);
            assertTrue(N.equals(N.asList("1", "3", "2"), b));
        }
    }

    @Test
    public void test_kthLargest() {
        final char[] a = { 'b', 'a', 'd', 'c', 'f', 'e' };
        assertEquals('f', N.kthLargest(a, 1));
        assertEquals('a', N.kthLargest(a, 6));

        assertEquals('e', N.kthLargest(a, 2));
        assertEquals('b', N.kthLargest(a, 5));

        assertEquals('d', N.kthLargest(a, 3));
        assertEquals('c', N.kthLargest(a, 4));
    }

    @Test
    public void test_nullToEmpty() {
        {
            String str = null;
            assertEquals(Strings.EMPTY, Strings.nullToEmpty(str));

            str = "123";
            assertEquals(str, Strings.nullToEmpty(str));
        }

        {
            boolean[] a = null;
            assertEquals(N.EMPTY_BOOLEAN_ARRAY, N.nullToEmpty(a));

            a = new boolean[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            char[] a = null;
            assertEquals(N.EMPTY_CHAR_ARRAY, N.nullToEmpty(a));

            a = new char[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            byte[] a = null;
            assertEquals(N.EMPTY_BYTE_ARRAY, N.nullToEmpty(a));

            a = new byte[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            short[] a = null;
            assertEquals(N.EMPTY_SHORT_ARRAY, N.nullToEmpty(a));

            a = new short[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            int[] a = null;
            assertEquals(N.EMPTY_INT_ARRAY, N.nullToEmpty(a));

            a = new int[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            long[] a = null;
            assertEquals(N.EMPTY_LONG_ARRAY, N.nullToEmpty(a));

            a = new long[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            float[] a = null;
            assertEquals(N.EMPTY_FLOAT_ARRAY, N.nullToEmpty(a));

            a = new float[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            double[] a = null;
            assertEquals(N.EMPTY_DOUBLE_ARRAY, N.nullToEmpty(a));

            a = new double[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            String[] a = null;
            assertEquals(N.EMPTY_STRING_ARRAY, N.nullToEmpty(a));

            a = new String[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            Object[] a = null;
            assertEquals(N.EMPTY_OBJECT_ARRAY, N.nullToEmpty(a));

            a = new Object[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            String[] a = null;
            assertTrue(N.equals(new String[0], N.nullToEmpty(a, String[].class)));

            a = new String[3];
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            List<String> a = null;
            assertEquals(N.EMPTY_LIST, N.nullToEmpty(a));

            a = N.asList("1", "2");
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            Set<String> a = null;
            assertEquals(N.EMPTY_SET, N.nullToEmpty(a));

            a = N.asSet("1", "2");
            assertEquals(a, N.nullToEmpty(a));
        }

        {
            Map<String, String> a = null;
            assertEquals(N.EMPTY_MAP, N.nullToEmpty(a));

            a = N.asMap("1", "2");
            assertEquals(a, N.nullToEmpty(a));
        }
    }

    @Test
    public void test_listOf() {
        {
            final boolean[] a = { true, false };
            assertTrue(a == BooleanList.of(a).array());
        }

        {
            final char[] a = { 'a', '1' };
            assertTrue(a == CharList.of(a).array());
        }

        {
            final byte[] a = { 1, 2 };
            assertTrue(a == ByteList.of(a).array());
        }

        {
            final short[] a = { 1, 2 };
            assertTrue(a == ShortList.of(a).array());
        }

        {
            final int[] a = { 1, 2 };
            assertTrue(a == IntList.of(a).array());
        }

        {
            final long[] a = { 1, 2 };
            assertTrue(a == LongList.of(a).array());
        }

        {
            final float[] a = { 1, 2 };
            assertTrue(a == FloatList.of(a).array());
        }

        {
            final double[] a = { 1, 2 };
            assertTrue(a == DoubleList.of(a).array());
        }
    }

    @Test
    public void test_parseIntLong() {
        assertEquals(123, Numbers.toInt("123"));
        assertEquals(123, Numbers.toInt("+123"));
        assertEquals(-123, Numbers.toInt("-123"));

        assertEquals(123, Numbers.toLong("123"));
        assertEquals(123, Numbers.toLong("+123"));
        assertEquals(-123, Numbers.toLong("-123"));
    }

    @Test
    public void test_countMatches() {
        assertEquals(0, Strings.countMatches(null, 'a'));
        assertEquals(0, Strings.countMatches("", 'a'));
        assertEquals(0, Strings.countMatches("  ", 'a'));
        assertEquals(0, Strings.countMatches(" b ", 'a'));
        assertEquals(1, Strings.countMatches("a", 'a'));
        assertEquals(3, Strings.countMatches("aaa", 'a'));

        assertEquals(0, Strings.countMatches(null, "aa"));
        assertEquals(0, Strings.countMatches("", "aa"));
        assertEquals(0, Strings.countMatches("  ", "aa"));
        assertEquals(0, Strings.countMatches(" b ", "aa"));
        assertEquals(1, Strings.countMatches("aa", "aa"));
        assertEquals(1, Strings.countMatches("aaa", "aa"));
        assertEquals(2, Strings.countMatches("aaaaa", "aa"));
    }

    @Test
    public void test_formalizePropName() {
        assertEquals("a", Beans.formalizePropName("_a"));
        assertEquals("a", Beans.formalizePropName("_A"));
        assertEquals("a", Beans.formalizePropName("_a_"));
        assertEquals("a", Beans.formalizePropName("_A_"));
        assertEquals("aB", Beans.formalizePropName("_a_b"));
        assertEquals("aB", Beans.formalizePropName("_a__b"));
        assertEquals("aB", Beans.formalizePropName("_a_B"));
        assertEquals("aB", Beans.formalizePropName("_A_b"));
        assertEquals("aB", Beans.formalizePropName("_A_B"));
        assertEquals("aB", Beans.formalizePropName("_a_b_"));
        assertEquals("aB", Beans.formalizePropName("_a_B_"));
        assertEquals("aB", Beans.formalizePropName("_A_b_"));
        assertEquals("aB", Beans.formalizePropName("_A_B_"));
        assertEquals("aBCd", Beans.formalizePropName("_a_b_cd"));
        assertEquals("aBCd", Beans.formalizePropName("_a_B_cd"));
        assertEquals("aBCd", Beans.formalizePropName("_A_b_cd"));
        assertEquals("aBCd", Beans.formalizePropName("_A_B_cd"));
        assertEquals("aBbCd", Beans.formalizePropName("_A_bb_cd"));
        assertEquals("aBbCd", Beans.formalizePropName("_A_Bb_cd"));
        assertEquals("firstName", Beans.formalizePropName("_FIRST_NAME_"));

        assertEquals("a_bb_cd", Beans.toLowerCaseWithUnderscore("aBBCd"));
        assertEquals("a_b_cd", Beans.toLowerCaseWithUnderscore("aBCd"));
        assertEquals("a_b", Beans.toLowerCaseWithUnderscore("aB"));
        assertEquals("A_BB_CD", Beans.toUpperCaseWithUnderscore("aBBCd"));
        assertEquals("A_B_CD", Beans.toUpperCaseWithUnderscore("aBCd"));
        assertEquals("A_B", Beans.toUpperCaseWithUnderscore("aB"));

        assertEquals("abc123efg456", Beans.toLowerCaseWithUnderscore("abc123EFG456"));
        assertEquals("ABC123EFG456A_AA", Beans.toUpperCaseWithUnderscore("abc123EFG456aAa"));
    }

    @Test
    public void test_indexOfArray() {

        final String nullStr = null;

        {
            assertEquals(0, Strings.indexOf("a, b, c", "a", ","));
            assertEquals(3, Strings.indexOf("a, b, c", "b", ", "));
            assertEquals(6, Strings.indexOf("a, b, c", "c", ", "));
            assertEquals(-1, Strings.indexOf("a,  b, c", "d", ","));

            final boolean[] b = { true, true };
            assertEquals(0, N.indexOf(b, true));
            assertEquals(-1, N.indexOf(b, false));

            final char[] c = { '1', '3', '2' };
            assertEquals(1, N.indexOf(c, '3'));
            assertEquals(-1, N.indexOf(c, '4'));

            final byte[] bt = { 1, 3, 2 };
            assertEquals(1, N.indexOf(bt, (byte) 3));
            assertEquals(-1, N.indexOf(bt, (byte) 4));

            final short[] s = { 1, 3, 2 };
            assertEquals(1, N.indexOf(s, (short) 3));
            assertEquals(-1, N.indexOf(s, (short) 4));

            final int[] i = { 1, 3, 2 };
            assertEquals(1, N.indexOf(i, 3));
            assertEquals(-1, N.indexOf(i, 4));

            final long[] l = { 1, 3, 2 };
            assertEquals(1, N.indexOf(l, 3));
            assertEquals(-1, N.indexOf(l, 4));

            final float[] f = { 1, 3, 2 };
            assertEquals(1, N.indexOf(f, 3f));
            assertEquals(-1, N.indexOf(f, 4f));

            final double[] d = { 1, 3, 2 };
            assertEquals(1, N.indexOf(d, 3));
            assertEquals(-1, N.indexOf(d, 4));

            final Integer[] st = { 1, 3, 2 };
            assertEquals(1, N.indexOf(st, 3));
            assertEquals(-1, N.indexOf(st, 4));

            final List<Integer> list = N.asList(1, 3, 2);
            assertEquals(1, N.indexOf(list, 3));
            assertEquals(-1, N.indexOf(list, 4));
        }

        {
            assertEquals(0, Strings.lastIndexOf("a,  b, c", "a", ","));
            assertEquals(3, Strings.lastIndexOf("a, b, c", "b", ", "));
            assertEquals(10, Strings.lastIndexOf("a,  b, c, b", "b", ", "));
            assertEquals(-1, Strings.lastIndexOf("a,  b, c", "d", ","));

            final boolean[] b = { true, true };
            assertEquals(1, N.lastIndexOf(b, true));
            assertEquals(-1, N.lastIndexOf(b, false));

            final char[] c = { '1', '3', '2' };
            assertEquals(1, N.lastIndexOf(c, '3'));
            assertEquals(-1, N.lastIndexOf(c, '4'));

            final byte[] bt = { 1, 3, 2 };
            assertEquals(1, N.lastIndexOf(bt, (byte) 3));
            assertEquals(-1, N.lastIndexOf(bt, (byte) 4));

            final short[] s = { 1, 3, 2 };
            assertEquals(1, N.lastIndexOf(s, (short) 3));
            assertEquals(-1, N.lastIndexOf(s, (short) 4));

            final int[] i = { 1, 3, 2 };
            assertEquals(1, N.lastIndexOf(i, 3));
            assertEquals(-1, N.lastIndexOf(i, 4));

            final long[] l = { 1, 3, 2 };
            assertEquals(1, N.lastIndexOf(l, 3));
            assertEquals(-1, N.lastIndexOf(l, 4));

            final float[] f = { 1, 3, 2 };
            assertEquals(1, N.lastIndexOf(f, 3f));
            assertEquals(-1, N.lastIndexOf(f, 4f));

            final double[] d = { 1, 3, 2 };
            assertEquals(1, N.lastIndexOf(d, 3));
            assertEquals(-1, N.lastIndexOf(d, 4));

            final Integer[] st = { 1, 3, 2 };
            assertEquals(1, N.lastIndexOf(st, 3));
            assertEquals(-1, N.lastIndexOf(st, 4));

            final List<Integer> list = N.asList(1, 3, 2);
            assertEquals(1, N.lastIndexOf(list, 3));
            assertEquals(-1, N.lastIndexOf(list, 4));
        }

        {

            assertFalse(Strings.contains(nullStr, "a", ","));
            assertFalse(Strings.contains("", "b", ","));
            assertTrue(Strings.contains("a,  b, c", "c", ", "));
            assertFalse(Strings.contains("a,  b, c", "d", ","));

            final boolean[] b = { true, true };
            assertTrue(N.contains(b, true));
            assertFalse(N.contains(b, false));

            final char[] c = { '1', '3', '2' };
            assertTrue(N.contains(c, '3'));
            assertFalse(N.contains(c, '4'));

            final byte[] bt = { 1, 3, 2 };
            assertTrue(N.contains(bt, (byte) 3));
            assertFalse(N.contains(bt, (byte) 4));

            final short[] s = { 1, 3, 2 };
            assertTrue(N.contains(s, (short) 3));
            assertFalse(N.contains(s, (short) 4));

            final int[] i = { 1, 3, 2 };
            assertTrue(N.contains(i, 3));
            assertFalse(N.contains(i, 4));

            final long[] l = { 1, 3, 2 };
            assertTrue(N.contains(l, 3));
            assertFalse(N.contains(l, 4));

            final float[] f = { 1, 3, 2 };
            assertTrue(N.contains(f, 3f));
            assertFalse(N.contains(f, 4f));

            final double[] d = { 1, 3, 2 };
            assertTrue(N.contains(d, 3));
            assertFalse(N.contains(d, 4));

            final Integer[] st = { 1, 3, 2 };
            assertTrue(N.contains(st, 3));
            assertFalse(N.contains(st, 4));

            final List<Integer> list = N.asList(1, 3, 2);
            assertTrue(N.contains(list, 3));
            assertFalse(N.contains(list, 4));

            assertTrue(Strings.contains("abc", 'a'));
            assertFalse(Strings.contains("abc", 'd'));
            assertFalse(Strings.contains("abc", nullStr));
            assertTrue(Strings.contains("abc", ""));
            assertFalse(Strings.contains(nullStr, 'a'));
            assertFalse(Strings.contains("", 'a'));

            assertFalse(Strings.containsAny(nullStr, 'a'));
            assertFalse(Strings.containsAny("", 'a'));
            assertTrue(Strings.containsAny("abc", 'a'));

            assertTrue(Strings.containsOnly(nullStr, 'a'));
            assertTrue(Strings.containsOnly("", 'a'));
            assertFalse(Strings.containsOnly("abc", 'a', 'b'));
            assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));

            assertTrue(Strings.containsNone(nullStr, 'a'));
            assertTrue(Strings.containsNone("", 'a'));
            assertTrue(Strings.containsNone("abc", 'd', 'e', 'f'));
            assertFalse(Strings.containsNone("abc", 'a', 'e', 'f'));
            assertFalse(Strings.containsNone("abc", 'b'));

        }

        {
            assertTrue("abc".startsWith(""));
            assertFalse(Strings.startsWith(nullStr, "a"));
            assertFalse(Strings.startsWith("", "a"));
            assertFalse(Strings.startsWith("abc", nullStr));
            assertTrue(Strings.startsWith("abc", ""));
            assertTrue(Strings.startsWith("abc", "a"));
            assertFalse(Strings.startsWith("abc", "A"));
            assertTrue(Strings.startsWithIgnoreCase("abc", "A"));
            assertFalse(Strings.startsWith("abc", "b"));

            assertFalse(Strings.startsWithAny(nullStr, "a"));
            assertFalse(Strings.startsWithAny("", "a"));
            assertFalse(Strings.startsWithAny("abc", nullStr));
            assertTrue(Strings.startsWithAny("abc", ""));
            assertTrue(Strings.startsWithAny("abc", "a", "b"));
            assertFalse(Strings.startsWithAny("abc", "A", "B"));

            assertTrue("abc".endsWith(""));

            assertFalse(Strings.endsWith(nullStr, "a"));
            assertFalse(Strings.endsWith("", "a"));
            assertFalse(Strings.endsWith("abc", nullStr));
            assertTrue(Strings.endsWith("abc", ""));
            assertTrue(Strings.endsWith("abc", "c"));
            assertFalse(Strings.endsWith("abc", "C"));
            assertTrue(Strings.endsWithIgnoreCase("abc", "C"));
            assertFalse(Strings.endsWith("abc", "b"));

            assertFalse(Strings.endsWithAny(nullStr, "a"));
            assertFalse(Strings.endsWithAny("", "a"));
            assertFalse(Strings.endsWithAny("abc", nullStr));
            assertTrue(Strings.endsWithAny("abc", ""));
            assertTrue(Strings.endsWithAny("abc", "a", "c"));
            assertFalse(Strings.endsWithAny("abc", "a", "C"));
        }
    }

    @Test
    public void test_indexOfString() {
        final String str = "abc";

        assertEquals(0, "".indexOf(""));

        assertEquals(0, Strings.indexOf(str, ""));
        assertEquals(-1, Strings.indexOf(str, null));

        assertEquals(1, Strings.indexOf(str, 'b'));
        assertEquals(1, Strings.indexOf(str, 'b', 1));
        assertEquals(1, Strings.indexOf(str, "b"));
        assertEquals(1, Strings.indexOf(str, "b", 1));

        assertEquals(1, Strings.indexOfIgnoreCase(str, "B"));
        assertEquals(1, Strings.indexOfIgnoreCase(str, "B", 1));

        assertEquals(-1, Strings.indexOfAny(null, 'c', 'd'));
        assertEquals(-1, Strings.indexOfAny("", 'c', 'd'));

        assertEquals(2, Strings.indexOfAny(str, 'c', 'd'));
        assertEquals(2, Strings.indexOfAny(str, "c", "d"));

        assertEquals(0, Strings.indexOfAnyBut(str, 'c', 'd'));
        assertEquals(-1, Strings.indexOfAnyBut(str, 'a', 'b', 'c'));

        assertEquals(1, Strings.lastIndexOf(str, 'b'));
        assertEquals(1, Strings.lastIndexOf(str, 'b', 1));
        assertEquals(1, Strings.lastIndexOf(str, "b"));
        assertEquals(1, Strings.lastIndexOf(str, "b", 1));

        assertEquals(1, Strings.lastIndexOfIgnoreCase(str, "B"));
        assertEquals(1, Strings.lastIndexOfIgnoreCase(str, "B", 1));

        assertEquals(-1, Strings.lastIndexOfAny(null, 'c', 'd'));
        assertEquals(-1, Strings.lastIndexOfAny("", 'c', 'd'));

        assertEquals(2, Strings.lastIndexOfAny(str, 'c', 'd'));
        assertEquals(2, Strings.lastIndexOfAny(str, "c", "d"));

        assertEquals(0, Strings.indexOfAnyBut(str, 'c', 'd'));
        assertEquals(-1, Strings.indexOfAnyBut(str, 'a', 'b', 'c'));
    }

    @Test
    public void test_percentiles() {
        final int[] a = Array.range(1, 101);
        final Map<Percentage, Integer> percentiles = N.percentiles(a);
        N.println(percentiles);
        percentiles.forEach(Fn.println("="));

        percentiles.forEach((k, v) -> N.println("     *                            " + k + "=" + v));
    }

    @Test
    public void test_sort_2() {
        String[] st1 = { "a", "c", "b" };
        N.sort(st1);
        N.println(st1);
        assertEquals(st1[1], "b");

        st1 = new String[] { "a", "c", null, "b" };
        N.sort(st1);
        N.println(st1);
        assertEquals(st1[1], "a");
    }

    @Test
    public void test_sort() {
        final char[] c = { '1', '3', '2' };
        N.sort(c);
        N.println(c);
        assertEquals(c[1], '2');

        final byte[] bt = { 1, 3, 2 };
        N.sort(bt);
        N.println(bt);
        assertEquals(bt[1], 2);

        final short[] s = { 1, 3, 2 };
        N.sort(s);
        N.println(s);
        assertEquals(s[1], 2);

        final int[] i = { 1, 3, 2 };
        N.sort(i);
        N.println(i);
        assertEquals(i[1], 2);

        final long[] l = { 1, 3, 2 };
        N.sort(l);
        N.println(l);
        assertEquals(l[1], 2);

        final float[] f = { 1, 3, 2 };
        N.sort(f);
        N.println(f);
        assertEquals(f[1], 2f);

        final double[] d = { 1, 3, 2 };
        N.sort(d);
        N.println(d);
        assertEquals(d[1], 2d);

        final Integer[] st = { 1, 3, 2 };
        N.sort(st);
        N.println(st);
        assertEquals(st[1].intValue(), 2);

        String[] st1 = { "a", "c", "b" };
        N.sort(st1);
        N.println(st1);
        assertEquals(st1[1], "b");

        st1 = new String[] { "a", "c", "b" };
        N.sort(st1, (Comparator<String>) (o1, o2) -> o2.compareTo(o1));
        N.println(st1);
        assertEquals(st1[1], "b");

        List<String> list = N.asList("a", "c", "b");
        N.sort(list);
        N.println(list);
        assertEquals(list.get(1), "b");

        list = N.asList("a", "c", "b");
        N.sort(list, (Comparator<String>) (o1, o2) -> o2.compareTo(o1));
        N.println(list);
        assertEquals(list.get(1), "b");

        list = new java.util.ArrayList<>();
        list.add("a");
        list.add("c");
        list.add("b");
        N.sort(list, (Comparator<String>) (o1, o2) -> o2.compareTo(o1));
        N.println(list);
        assertEquals(list.get(1), "b");
    }

    @Test
    public void test_fill() {
        final boolean[] b = new boolean[2];
        N.fill(b, true);
        N.println(b);
        assertEquals(b[0], true);
        N.fill(b, 1, 2, true);

        final char[] c = new char[2];
        N.fill(c, 'a');
        N.println(c);
        assertEquals(c[0], 'a');
        N.fill(c, 1, 2, 'a');

        final byte[] bt = new byte[2];
        N.fill(bt, (byte) 12);
        N.println(bt);
        assertEquals(bt[0], 12);
        N.fill(bt, 1, 2, (byte) 12);

        final short[] s = new short[2];
        N.fill(s, (short) 12);
        N.println(s);
        assertEquals(s[0], 12);
        N.fill(s, 1, 2, (short) 12);

        final int[] i = new int[2];
        N.fill(i, 12);
        N.println(i);
        assertEquals(i[0], 12);
        N.fill(i, 1, 2, 12);

        final long[] l = new long[2];
        N.fill(l, 12);
        N.println(l);
        assertEquals(l[0], 12);
        N.fill(l, 1, 2, 12);

        final float[] f = new float[2];
        N.fill(f, 1.2f);
        N.println(f);
        assertEquals(f[0], 1.2f);
        N.fill(f, 1, 2, 1.2f);

        final double[] d = new double[2];
        N.fill(d, 1.2);
        N.println(d);
        assertEquals(d[0], 1.2);
        N.fill(d, 1, 2, 1.2);

        final String[] st = new String[2];
        N.fill(st, "a");
        N.println(st);
        assertEquals(st[0], "a");
        N.fill(st, 1, 2, "a");

        List<String> list = N.asList("b", "b", "b");
        N.fill(list, "a");
        N.println(list);
        assertEquals(list.get(0), "a");
        N.fill(list, 1, 2, "a");

        for (int k = 5; k <= 1001; k++) {
            list = N.asLinkedList();
            N.fill(list, 0, k, null);
            N.fill(list, 3, k - 2, "abc");
            N.println(list);

            for (int j = 3; j < k - 2; j++) {
                assertEquals("abc", list.get(j));
            }
        }
    }

    @Test
    public void test_is() {
        {
            assertTrue(Strings.isAllLowerCase(null));
            assertTrue(Strings.isAllLowerCase(""));
            assertTrue(Strings.isAllLowerCase("abc"));
            assertFalse(Strings.isAllLowerCase("abc黎"));
            assertFalse(Strings.isAllLowerCase("ABC"));
            assertFalse(Strings.isAllLowerCase("ABC黎"));

            assertTrue(Strings.isAllLowerCase(new StringBuilder("")));
            assertTrue(Strings.isAllLowerCase(new StringBuilder("abc")));
            assertFalse(Strings.isAllLowerCase(new StringBuilder("abc黎")));
            assertFalse(Strings.isAllLowerCase(new StringBuilder("ABC")));
            assertFalse(Strings.isAllLowerCase(new StringBuilder("ABC黎")));

            assertTrue(Strings.isAllUpperCase(null));
            assertTrue(Strings.isAllUpperCase(""));
            assertFalse(Strings.isAllUpperCase(new StringBuilder("abc")));
            assertFalse(Strings.isAllUpperCase(new StringBuilder("abc黎")));
            assertTrue(Strings.isAllUpperCase(new StringBuilder("ABC")));
            assertFalse(Strings.isAllUpperCase(new StringBuilder("ABC黎")));

            assertFalse(Strings.isAllUpperCase("abc"));
            assertFalse(Strings.isAllUpperCase("abc黎"));
            assertTrue(Strings.isAllUpperCase("ABC"));
            assertFalse(Strings.isAllUpperCase("ABC黎"));
        }

        {
            assertFalse(Strings.isAsciiAlphanumeric(null));
            assertFalse(Strings.isAsciiAlphanumeric(""));
            assertTrue(Strings.isAsciiAlphanumeric("abc"));
            assertTrue(Strings.isAsciiAlphanumeric(new StringBuilder("abc")));
            assertFalse(Strings.isAsciiAlphanumeric("abc\n\123"));
            assertFalse(Strings.isAsciiAlphanumeric(new StringBuilder("abc\n\123")));

            assertFalse(Strings.isAsciiPrintable(null));
            assertTrue(Strings.isAsciiPrintable(""));
            assertTrue(Strings.isAsciiPrintable("abc"));
            assertTrue(Strings.isAsciiPrintable(new StringBuilder("abc")));
            assertFalse(Strings.isAsciiPrintable("abc\n\123"));
            assertFalse(Strings.isAsciiPrintable(new StringBuilder("abc\n\123")));

            assertFalse(Strings.isAsciiAlpha(null));
            assertFalse(Strings.isAsciiAlpha(""));
            assertTrue(Strings.isAsciiAlpha("abc"));
            assertTrue(Strings.isAsciiAlpha(new StringBuilder("abc")));
            assertFalse(Strings.isAsciiAlpha("abc\n\123"));
            assertFalse(Strings.isAsciiAlpha(new StringBuilder("abc\n\123")));

            assertFalse(Strings.isAsciiAlphaSpace(null));
            assertTrue(Strings.isAsciiAlphaSpace(""));
            assertTrue(Strings.isAsciiAlphaSpace("abc "));
            assertTrue(Strings.isAsciiAlphaSpace(new StringBuilder("abc ")));
            assertFalse(Strings.isAsciiAlphaSpace("abc\n\123 "));
            assertFalse(Strings.isAsciiAlphaSpace(new StringBuilder("abc\n\123 ")));

            assertFalse(Strings.isAsciiAlphanumericSpace(null));
            assertTrue(Strings.isAsciiAlphanumericSpace(""));
            assertTrue(Strings.isAsciiAlphanumericSpace("abc "));
            assertTrue(Strings.isAsciiAlphanumericSpace(new StringBuilder("abc ")));
            assertFalse(Strings.isAsciiAlphanumericSpace("abc\n\123 "));
            assertFalse(Strings.isAsciiAlphanumericSpace(new StringBuilder("abc\n\123 ")));

            assertFalse(Strings.isAsciiNumeric(null));
            assertFalse(Strings.isAsciiNumeric(""));
            assertTrue(Strings.isAsciiNumeric("123"));
            assertTrue(Strings.isAsciiNumeric(new StringBuilder("123")));
            assertFalse(Strings.isAsciiNumeric("123\n\123"));
            assertFalse(Strings.isAsciiNumeric(new StringBuilder("123\n\123")));

            assertFalse(Strings.isAlpha(null));
            assertFalse(Strings.isAlpha(""));
            assertTrue(Strings.isAlpha("abc"));
            assertTrue(Strings.isAlpha(new StringBuilder("abc")));
            assertFalse(Strings.isAlpha("abc\n\123"));
            assertFalse(Strings.isAlpha(new StringBuilder("abc\n\123")));

            assertFalse(Strings.isAlphaSpace(null));
            assertTrue(Strings.isAlphaSpace(""));
            assertTrue(Strings.isAlphaSpace("abc"));
            assertTrue(Strings.isAlphaSpace(new StringBuilder("abc")));
            assertFalse(Strings.isAlphaSpace("abc\n\123"));
            assertFalse(Strings.isAlphaSpace(new StringBuilder("abc\n\123")));

            assertFalse(Strings.isAlphanumeric(null));
            assertFalse(Strings.isAlphanumeric(""));
            assertTrue(Strings.isAlphanumeric("abc123"));
            assertTrue(Strings.isAlphanumeric(new StringBuilder("abc123")));
            assertFalse(Strings.isAlphanumeric("abc\n\123"));
            assertFalse(Strings.isAlphanumeric(new StringBuilder("abc\n\123")));

            assertFalse(Strings.isAlphanumericSpace(null));
            assertTrue(Strings.isAlphanumericSpace(""));
            assertTrue(Strings.isAlphanumericSpace("abc123 "));
            assertTrue(Strings.isAlphanumericSpace(new StringBuilder("abc123 ")));
            assertFalse(Strings.isAlphanumericSpace("abc\n\123 "));
            assertFalse(Strings.isAlphanumericSpace(new StringBuilder("abc\n\123 ")));

            assertFalse(Strings.isNumeric(null));
            assertFalse(Strings.isNumeric(""));
            assertTrue(Strings.isNumeric("123"));
            assertTrue(Strings.isNumeric(new StringBuilder("123")));
            assertFalse(Strings.isNumeric("abc\n\123 "));
            assertFalse(Strings.isNumeric(new StringBuilder("abc\n\123 ")));

            assertFalse(Strings.isNumericSpace(null));
            assertTrue(Strings.isNumericSpace(""));
            assertTrue(Strings.isNumericSpace("12 3"));
            assertTrue(Strings.isNumericSpace(new StringBuilder("12 3")));
            assertFalse(Strings.isNumericSpace("abc\n\123 "));
            assertFalse(Strings.isNumericSpace(new StringBuilder("abc\n\123 ")));

            assertFalse(Strings.isWhitespace(null));
            assertTrue(Strings.isWhitespace(""));
            assertTrue(Strings.isWhitespace(" \n \r "));
            assertFalse(Strings.isWhitespace(" \\n \\r "));
            assertTrue(Strings.isWhitespace(new StringBuilder(" \n \r ")));
            assertFalse(Strings.isWhitespace(new StringBuilder(" \\n \\r ")));
            assertFalse(Strings.isWhitespace("abc\n\123 "));
            assertFalse(Strings.isWhitespace(new StringBuilder("abc\n\123 ")));

            assertTrue(Numbers.isNumber("0.0"));
            assertTrue(Numbers.isNumber("0.4790"));

            assertTrue(Numbers.isNumber("123"));
            assertTrue(Numbers.isNumber("0X123"));
            assertTrue(Numbers.isNumber("123f"));
            assertTrue(Numbers.isNumber("0X123f"));
            assertFalse(Numbers.isNumber("123g"));
            assertFalse(Numbers.isNumber("0X123g"));

            assertTrue(Numbers.isNumber("-123"));
            assertTrue(Numbers.isNumber("-0X123"));
            assertTrue(Numbers.isNumber("-123f"));
            assertTrue(Numbers.isNumber("-0X123f"));
            assertFalse(Numbers.isNumber("-123g"));
            assertFalse(Numbers.isNumber("-0X123g"));

            assertTrue(Strings.isAsciiDigitalNumber("123"));
            assertTrue(Strings.isAsciiDigitalNumber("-123"));
            assertTrue(Strings.isAsciiDigitalNumber("-123.00"));
            assertFalse(Strings.isAsciiDigitalNumber("-"));
            assertFalse(Strings.isAsciiDigitalNumber("."));

            assertTrue(Strings.isAsciiDigitalInteger("123"));
            assertTrue(Strings.isAsciiDigitalInteger("-123"));
            assertFalse(Strings.isAsciiDigitalInteger("-123.00"));
            assertFalse(Strings.isAsciiDigitalInteger("-"));
            assertFalse(Strings.isAsciiDigitalInteger("."));
        }
    }

    @Test
    public void test_chomp_chop() {
        {
            assertEquals(null, Strings.chomp((String) null));
            assertEquals("", Strings.chomp(""));
            assertEquals("abc ", Strings.chomp("abc \r"));
            assertEquals("abc\n\rabc", Strings.chomp("abc\n\rabc"));

            assertEquals("abc ", Strings.chomp("abc \r"));
            assertEquals("abc", Strings.chomp("abc\n"));
            assertEquals("abc", Strings.chomp("abc\r\n"));
            assertEquals("abc\r\n", Strings.chomp("abc\r\n\r\n"));
            assertEquals("abc\n", Strings.chomp("abc\n\r"));
            assertEquals("abc\n\rabc", Strings.chomp("abc\n\rabc"));
            assertEquals("", Strings.chomp("\r"));
            assertEquals("", Strings.chomp("\n"));
            assertEquals("", Strings.chomp("\r\n"));
        }

        {
            assertEquals(null, Strings.chop((String) null));
            assertEquals("", Strings.chop(""));
            assertEquals("", Strings.chop("\r"));
            assertEquals("", Strings.chop("\n"));
            assertEquals("", Strings.chop("\r\n"));
            assertEquals("abc ", Strings.chop("abc \r"));
            assertEquals("ab", Strings.chop("abc"));
        }

        {
            assertEquals(null, Strings.deleteWhitespace((String) null));
            assertEquals("", Strings.deleteWhitespace(""));
            assertEquals("abc", Strings.deleteWhitespace("abc \r"));
            assertEquals("abcabc", Strings.deleteWhitespace("abc\n\rabc"));
        }

    }

    @Test
    public void test_trim_strip() {
        {
            assertEquals(null, Strings.trim((String) null));
            assertEquals("", Strings.trim(""));

            assertEquals("aa", Strings.trim("aa"));
            assertEquals("aa", Strings.trim("  aa"));
            assertEquals("aa", Strings.trim("  aa  "));
            assertEquals("a aa a", Strings.trim(" a aa a "));

            assertEquals(null, Strings.trimToNull((String) null));
            assertEquals(null, Strings.trimToNull(""));

            assertEquals("", Strings.trimToEmpty((String) null));
            assertEquals("", Strings.trimToEmpty(""));
        }

        {
            assertEquals(null, Strings.strip((String) null));
            assertEquals("", Strings.strip(""));

            assertEquals("aa", Strings.strip("aa"));
            assertEquals("aa", Strings.strip("  aa"));
            assertEquals("aa", Strings.strip("  aa  "));
            assertEquals("a aa a", Strings.strip(" a aa a "));

            assertEquals(null, Strings.stripToNull((String) null));
            assertEquals(null, Strings.stripToNull(""));

            assertEquals("", Strings.stripToEmpty((String) null));
            assertEquals("", Strings.stripToEmpty(""));
        }

        {

            assertEquals(Strings.stripEnd((String) null, "*"), null);
            assertEquals(Strings.stripEnd("", "*"), "");
            assertEquals(Strings.stripEnd("abc", ""), "abc");
            assertEquals(Strings.stripEnd("abc", null), "abc");
            assertEquals(Strings.stripEnd("  abc", null), "  abc");
            assertEquals(Strings.stripEnd("abc  ", null), "abc");
            assertEquals(Strings.stripEnd(" abc ", null), " abc");
            assertEquals(Strings.stripEnd("  abcyx", "xyz"), "  abc");
            assertEquals(Strings.stripEnd("120.00", ".0"), "12");
        }

        {

            assertEquals(Strings.stripStart((String) null, "*"), null);
            assertEquals(Strings.stripStart("", "*"), "");
            assertEquals(Strings.stripStart("abc", ""), "abc");
            assertEquals(Strings.stripStart("abc", null), "abc");
            assertEquals(Strings.stripStart("  abc", null), "abc");
            assertEquals(Strings.stripStart("abc  ", null), "abc  ");
            assertEquals(Strings.stripStart(" abc ", null), "abc ");
            assertEquals(Strings.stripStart("yxabc  ", "xyz"), "abc  ");
            assertEquals(Strings.stripStart("00.120", "0."), "120");
        }

        {
            assertEquals(Strings.stripStart((String) null, "*"), null);
            String[] a = N.EMPTY_STRING_ARRAY;
            Strings.strip(a, "*");
            assertTrue(N.equals(a, N.EMPTY_STRING_ARRAY));

            a = N.asArray("abc", "  abc");
            Strings.strip(a, null);
            assertTrue(N.equals(a, N.asArray("abc", "abc")));

            a = N.asArray("abc  ", null);
            Strings.strip(a, null);
            assertTrue(N.equals(a, N.asArray("abc", null)));

            a = N.asArray("yabcz", null);
            Strings.strip(a, "yz");
            assertTrue(N.equals(a, N.asArray("abc", null)));
        }

        {

            assertEquals(null, Strings.stripAccents((String) null));
            assertEquals("", Strings.stripAccents(""));
            assertEquals("control", Strings.stripAccents("control"));
            assertEquals("eclair", Strings.stripAccents("éclair"));
        }
    }

    @Test
    public void test_concat() {
        assertEquals(true, N.concat(new boolean[] { true }, new boolean[] { true })[1]);

        assertEquals('a', N.concat(new char[] { 'a' }, new char[] { 'a' })[1]);

        assertEquals(1, N.concat(new byte[] { 1 }, new byte[] { 1 })[1]);

        assertEquals(1, N.concat(new short[] { 1 }, new short[] { 1 })[1]);

        assertEquals(1, N.concat(new int[] { 1 }, new int[] { 1 })[1]);

        assertEquals(1, N.concat(new long[] { 1 }, new long[] { 1 })[1]);

        assertEquals(1.1f, N.concat(new float[] { 1.1f }, new float[] { 1.1f })[1]);

        assertEquals(1.1, N.concat(new double[] { 1.1 }, new double[] { 1.1 })[1]);

        assertEquals("a", N.concat(new String[] { "a" }, new String[] { "a" })[1]);
    }

    @Test
    public void test_min_max_median() {
        final int len = 17;

        {
            assertEquals('1', N.min('1', '2'));
            assertEquals('1', N.min('2', '1'));
            assertEquals('2', N.max('1', '2'));
            assertEquals('2', N.max('2', '2'));
            assertEquals('1', N.median('1', '2'));
            assertEquals('1', N.median('2', '1'));

            assertEquals('1', N.min('1', '2', '3'));
            assertEquals('1', N.min('2', '1', '3'));
            assertEquals('1', N.min('3', '2', '1'));

            assertEquals('3', N.max('1', '2', '3'));
            assertEquals('3', N.max('2', '1', '3'));
            assertEquals('3', N.max('3', '2', '1'));

            assertEquals('2', N.median('1', '2', '3'));
            assertEquals('2', N.median('2', '1', '3'));
            assertEquals('2', N.median('3', '2', '1'));
            assertEquals('2', N.median('1', '3', '2'));

            char[] a = new char[len];
            for (int i = 0; i < len; i++) {
                a[i] = (char) rand.nextInt(1000);
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of('1');
            assertEquals('1', N.median(a));

            a = Array.of('2', '1');
            assertEquals('1', N.median(a));

            a = Array.of('2', '1', '3');
            assertEquals('2', N.median(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals((byte) 1, N.min((byte) 1, (byte) 2));
            assertEquals((byte) 1, N.min((byte) 2, (byte) 1));
            assertEquals((byte) 2, N.max((byte) 1, (byte) 2));
            assertEquals((byte) 2, N.max((byte) 2, (byte) 2));
            assertEquals((byte) 1, N.median((byte) 1, (byte) 2));
            assertEquals((byte) 1, N.median((byte) 2, (byte) 1));

            assertEquals((byte) 1, N.min((byte) 1, (byte) 2, (byte) 3));
            assertEquals((byte) 1, N.min((byte) 2, (byte) 1, (byte) 3));
            assertEquals((byte) 1, N.min((byte) 3, (byte) 2, (byte) 1));

            assertEquals((byte) 3, N.max((byte) 1, (byte) 2, (byte) 3));
            assertEquals((byte) 3, N.max((byte) 2, (byte) 1, (byte) 3));
            assertEquals((byte) 3, N.max((byte) 3, (byte) 2, (byte) 1));

            assertEquals((byte) 2, N.median((byte) 1, (byte) 2, (byte) 3));
            assertEquals((byte) 2, N.median((byte) 2, (byte) 1, (byte) 3));
            assertEquals((byte) 2, N.median((byte) 3, (byte) 2, (byte) 1));
            assertEquals((byte) 2, N.median((byte) 1, (byte) 3, (byte) 2));

            byte[] a = new byte[len];
            for (int i = 0; i < len; i++) {
                a[i] = (byte) rand.nextInt(127);
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            assertTrue(count <= (a.length) / 2);

            a = Array.of((byte) 1);
            assertEquals(1, N.median(a));

            a = Array.of((byte) 2, (byte) 1);
            assertEquals(1, N.median(a));

            a = Array.of((byte) 2, (byte) 1, (byte) 3);
            assertEquals(2, N.median(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals((short) 1, N.min((short) 1, (short) 2));
            assertEquals((short) 1, N.min((short) 2, (short) 1));
            assertEquals((short) 2, N.max((short) 1, (short) 2));
            assertEquals((short) 2, N.max((short) 2, (short) 2));
            assertEquals((short) 1, N.median((short) 1, (short) 2));
            assertEquals((short) 1, N.median((short) 2, (short) 1));

            assertEquals((short) 1, N.min((short) 1, (short) 2, (short) 3));
            assertEquals((short) 1, N.min((short) 2, (short) 1, (short) 3));
            assertEquals((short) 1, N.min((short) 3, (short) 2, (short) 1));

            assertEquals((short) 3, N.max((short) 1, (short) 2, (short) 3));
            assertEquals((short) 3, N.max((short) 2, (short) 1, (short) 3));
            assertEquals((short) 3, N.max((short) 3, (short) 2, (short) 1));

            assertEquals((short) 2, N.median((short) 1, (short) 2, (short) 3));
            assertEquals((short) 2, N.median((short) 2, (short) 1, (short) 3));
            assertEquals((short) 2, N.median((short) 3, (short) 2, (short) 1));
            assertEquals((short) 2, N.median((short) 1, (short) 3, (short) 2));

            short[] a = new short[len];
            for (int i = 0; i < len; i++) {
                a[i] = (short) rand.nextInt(127);
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((short) 1);
            assertEquals((short) 1, N.median(a));

            a = Array.of((short) 2, (short) 1);
            assertEquals((short) 1, N.median(a));

            a = Array.of((short) 2, (short) 1, (short) 3);
            assertEquals(2, N.median(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(1, N.min(1, 2));
            assertEquals(1, N.min(2, 1));
            assertEquals(2, N.max(1, 2));
            assertEquals(2, N.max(2, 2));
            assertEquals(1, N.median(1, 2));
            assertEquals(1, N.median(2, 1));

            assertEquals(1, N.min(1, 2, 3));
            assertEquals(1, N.min(2, 1, 3));
            assertEquals(1, N.min(3, 2, 1));

            assertEquals(3, N.max(1, 2, 3));
            assertEquals(3, N.max(2, 1, 3));
            assertEquals(3, N.max(3, 2, 1));

            assertEquals(2, N.median(1, 2, 3));
            assertEquals(2, N.median(2, 1, 3));
            assertEquals(2, N.median(3, 2, 1));
            assertEquals(2, N.median(1, 3, 2));

            int[] a = new int[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt();
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final int median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of(1);
            assertEquals(1, N.median(a));

            a = Array.of(2, 1);
            assertEquals(1, N.median(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(1, N.min((long) 1, (long) 2));
            assertEquals(1, N.min((long) 2, (long) 1));
            assertEquals(2, N.max((long) 1, (long) 2));
            assertEquals(2, N.max((long) 2, (long) 2));
            assertEquals(1, N.median((long) 1, (long) 2));
            assertEquals(1, N.median((long) 2, (long) 1));

            assertEquals(1, N.min((long) 1, (long) 2, (long) 3));
            assertEquals(1, N.min((long) 2, (long) 1, (long) 3));
            assertEquals(1, N.min((long) 3, (long) 2, (long) 1));

            assertEquals(3, N.max((long) 1, (long) 2, (long) 3));
            assertEquals(3, N.max((long) 2, (long) 1, (long) 3));
            assertEquals(3, N.max((long) 3, (long) 2, (long) 1));

            assertEquals(2, N.median((long) 1, (long) 2, (long) 3));
            assertEquals(2, N.median((long) 2, (long) 1, (long) 3));
            assertEquals(2, N.median((long) 3, (long) 2, (long) 1));
            assertEquals(2, N.median((long) 1, (long) 3, (long) 2));

            long[] a = new long[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt(127);
            }

            final long min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final long max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final long median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((long) 1);
            assertEquals(1, N.median(a));

            a = Array.of((long) 2, (long) 1);
            assertEquals(1, N.median(a));

            a = Array.of((long) 2, (long) 1, (long) 3);
            assertEquals(2, N.median(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals((float) 1, N.min((float) 1, (float) 2));
            assertEquals((float) 1, N.min((float) 2, (float) 1));
            assertEquals((float) 2, N.max((float) 1, (float) 2));
            assertEquals((float) 2, N.max((float) 2, (float) 2));
            assertEquals((float) 1, N.median((float) 1, (float) 2));
            assertEquals((float) 1, N.median((float) 2, (float) 1));

            assertEquals((float) 1, N.min((float) 1, (float) 2, (float) 3));
            assertEquals((float) 1, N.min((float) 2, (float) 1, (float) 3));
            assertEquals((float) 1, N.min((float) 3, (float) 2, (float) 1));

            assertEquals((float) 3, N.max((float) 1, (float) 2, (float) 3));
            assertEquals((float) 3, N.max((float) 2, (float) 1, (float) 3));
            assertEquals((float) 3, N.max((float) 3, (float) 2, (float) 1));

            assertEquals((float) 2, N.median((float) 1, (float) 2, (float) 3));
            assertEquals((float) 2, N.median((float) 2, (float) 1, (float) 3));
            assertEquals((float) 2, N.median((float) 3, (float) 2, (float) 1));
            assertEquals((float) 2, N.median((float) 1, (float) 3, (float) 2));

            float[] a = new float[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt(127);
            }

            final float min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final float max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final float median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((float) 1);
            assertEquals((float) 1, N.median(a));

            a = Array.of((float) 2, (float) 1);
            assertEquals((float) 1, N.median(a));

            a = Array.of((float) 2, (float) 1, (float) 3);
            assertEquals((float) 2, N.median(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals((double) 1, N.min((double) 1, (double) 2));
            assertEquals((double) 1, N.min((double) 2, (double) 1));
            assertEquals((double) 2, N.max((double) 1, (double) 2));
            assertEquals((double) 2, N.max((double) 2, (double) 2));
            assertEquals((double) 1, N.median((double) 1, (double) 2));
            assertEquals((double) 1, N.median((double) 2, (double) 1));

            assertEquals((double) 1, N.min((double) 1, (double) 2, (double) 3));
            assertEquals((double) 1, N.min((double) 2, (double) 1, (double) 3));
            assertEquals((double) 1, N.min((double) 3, (double) 2, (double) 1));

            assertEquals((double) 3, N.max((double) 1, (double) 2, (double) 3));
            assertEquals((double) 3, N.max((double) 2, (double) 1, (double) 3));
            assertEquals((double) 3, N.max((double) 3, (double) 2, (double) 1));

            assertEquals((double) 2, N.median((double) 1, (double) 2, (double) 3));
            assertEquals((double) 2, N.median((double) 2, (double) 1, (double) 3));
            assertEquals((double) 2, N.median((double) 3, (double) 2, (double) 1));
            assertEquals((double) 2, N.median((double) 1, (double) 3, (double) 2));

            double[] a = new double[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt(127);
            }

            final double min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] >= min);
            }

            final double max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a[i] <= max);
            }

            final double median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a[i] < median) {
                    count++;
                }
            }

            assertTrue(count <= (a.length) / 2);

            a = Array.of((double) 1);
            assertEquals((double) 1, N.median(a));

            a = Array.of((double) 2, (double) 1);
            assertEquals((double) 1, N.median(a));

            a = Array.of((double) 2, (double) 1, (double) 3);
            assertEquals((double) 2, N.median(a));

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {

            List<Integer> a = new ArrayList<>();
            for (int i = 0; i < len; i++) {
                a.add(rand.nextInt());
            }

            final int min = N.min(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a.get(i) >= min);
            }

            final int max = N.max(a);

            for (int i = 0; i < len; i++) {
                assertTrue(a.get(i) <= max);
            }

            final int median = N.median(a);

            int count = 0;
            for (int i = 0; i < len; i++) {
                if (a.get(i) < median) {
                    count++;
                }
            }

            assertTrue(count <= len / 2);

            a = N.asList(1);
            assertEquals(1, N.median(a).intValue());

            a = N.asList(2, 1);
            assertEquals(1, N.median(a).intValue());

            a = N.asList(2, 1, 3);
            assertEquals(2, N.median(a).intValue());

            a = null;
            try {
                N.min(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.max(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
            try {
                N.median(a);
                fail("Should throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            assertEquals(5, N.median(1, 2, 3, 4, 5, 6, 7, 8, 9));

            assertEquals(5, N.median(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 8));

            assertEquals(4, N.median(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 7));

            assertEquals(3, N.median(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 6));

            assertEquals(3, N.median(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 5));

            assertEquals(4, N.median(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 6));

            assertEquals(4, N.median(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 5));
        }

        {
            assertEquals(5, Median.of(1, 2, 3, 4, 5, 6, 7, 8, 9).left().intValue());

            assertEquals(5, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 8).left().intValue());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 7).left().intValue());

            assertEquals(3, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 6).left().intValue());

            assertEquals(3, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 5).left().intValue());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 6).left().intValue());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 5).left().intValue());
        }

        {
            assertEquals(true, Median.of(1, 2, 3, 4, 5, 6, 7, 8, 9).right().isEmpty());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 8).right().isEmpty());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 7).right().isEmpty());

            assertEquals(4, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 6).right().get());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 0, 5).right().isEmpty());

            assertEquals(5, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 6).right().get());

            assertEquals(true, Median.of(Array.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2, 5).right().isEmpty());
        }
    }

    @Test
    public void test_avg_sum() {
        {
            byte[] a = { 1, 2, 3, 4, 5 };
            assertEquals(15, N.sum(a));
            assertEquals(3d, N.average(a));

            a = new byte[0];
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));

            a = null;
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));
        }

        {
            short[] a = { 1, 2, 3, 4, 5 };
            assertEquals(15, N.sum(a));
            assertEquals(3d, N.average(a));

            a = new short[0];
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));

            a = null;
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));
        }

        {
            int[] a = { 1, 2, 3, 4, 5 };
            assertEquals(15, N.sum(a));
            assertEquals(3d, N.average(a));

            a = new int[0];
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));

            a = null;
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));
        }

        {
            long[] a = { 1, 2, 3, 4, 5 };
            assertEquals(15, N.sum(a));
            assertEquals(3d, N.average(a));

            a = new long[0];
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));

            a = null;
            assertEquals(0, N.sum(a));
            assertEquals(0d, N.average(a));
        }

        {
            float[] a = { 1, 2, 3, 4, 5 };
            assertEquals(15f, N.sum(a));
            assertEquals(3d, N.average(a));

            a = new float[0];
            assertEquals(0f, N.sum(a));
            assertEquals(0d, N.average(a));

            a = null;
            assertEquals(0f, N.sum(a));
            assertEquals(0d, N.average(a));
        }

        {
            double[] a = { 1, 2, 3, 4, 5 };
            assertEquals(15d, N.sum(a));
            assertEquals(3d, N.average(a));

            a = new double[0];
            assertEquals(0d, N.sum(a));
            assertEquals(0d, N.average(a));

            a = null;
            assertEquals(0d, N.sum(a));
            assertEquals(0d, N.average(a));
        }
    }

    @Test
    public void test_string() {
        {
            final String[] strs = { null, "", "  ", "黎" };

            for (final String str : strs) {
                assertEquals(str, Strings.toUpperCase(str));
                assertEquals(str, Strings.toUpperCase(str, Locale.US));
                assertEquals(str, Strings.toLowerCase(str));
                assertEquals(str, Strings.toLowerCase(str, Locale.US));
                assertEquals(str, Strings.swapCase(str));
            }
        }

        assertEquals("ABC", Strings.toUpperCase("aBc"));
        assertEquals("abc", Strings.toLowerCase("aBc"));
        assertEquals("abc", Strings.swapCase("ABC"));
        assertEquals("ABC", Strings.swapCase("abc"));
        final String uuid = Strings.uuid();
        assertEquals(uuid, Strings.swapCase(Strings.swapCase(uuid)));

        assertEquals("ABC", Strings.capitalize("aBC"));
        assertEquals("ABC", Strings.capitalize("ABC"));
        assertEquals("aBC", Strings.uncapitalize("aBC"));
        assertEquals("aBC", Strings.uncapitalize("ABC"));

        N.println(Strings.quoteEscaped("abc'\"def\\'\\\""));
        assertEquals("abc\\'\\\"def\\'\\\"", Strings.quoteEscaped("abc'\"def\\'\\\""));

        {
            final char[] chs = { 0x9, 0x10, 0x99, 0x100, 0x101, 0x1000, 0x9000 };
            final String[] strs = { "\\u0009", "\\u0010", "\\u0099", "\\u0100", "\\u0101", "\\u1000", "\\u9000" };
            for (int i = 0; i < chs.length; i++) {
                assertEquals(strs[i], Strings.unicodeEscaped(chs[i]));
            }
        }

        assertEquals("abc d e", Strings.normalizeSpace("abc  d \n e \r"));

        assertEquals("bbbbb", Strings.replaceAll("aaaaa", "a", "b"));
        assertEquals("bbaaa", Strings.replace("aaaaa", 0, "a", "b", 2));
        assertEquals("bbbbbb", RegExUtil.replaceAll("abc123", "[0-9a-zA-Z]", "b"));
        assertEquals("", RegExUtil.removeAll("abc123", "[0-9a-zA-Z]"));

        {
            assertEquals("", Strings.removeStart("", "add"));
            assertEquals(null, Strings.removeStart(null, "add"));

            assertEquals("abc", Strings.removeStart("abc", null));
            assertEquals("abc", Strings.removeStart("abc", ""));

            assertEquals("abc", Strings.removeStart("abc", "123"));
            assertEquals("abc", Strings.removeStart("abc", "b"));
            assertEquals("abc", Strings.removeStart("abc", "A"));
            assertEquals("bc", Strings.removeStart("abc", "a"));

            assertEquals("", Strings.removeStartIgnoreCase("", "add"));
            assertEquals(null, Strings.removeStartIgnoreCase(null, "add"));

            assertEquals("abc", Strings.removeStartIgnoreCase("abc", null));
            assertEquals("abc", Strings.removeStartIgnoreCase("abc", ""));

            assertEquals("abc", Strings.removeStartIgnoreCase("abc", "123"));
            assertEquals("abc", Strings.removeStartIgnoreCase("abc", "b"));

            assertEquals("bc", Strings.removeStartIgnoreCase("abc", "A"));
            assertEquals("bc", Strings.removeStartIgnoreCase("abc", "a"));
        }

        {
            assertEquals("", Strings.removeEnd("", "add"));
            assertEquals(null, Strings.removeEnd(null, "add"));

            assertEquals("abc", Strings.removeEnd("abc", null));
            assertEquals("abc", Strings.removeEnd("abc", ""));

            assertEquals("abc", Strings.removeEnd("abc", "123"));
            assertEquals("abc", Strings.removeEnd("abc", "b"));
            assertEquals("abc", Strings.removeEnd("abc", "C"));
            assertEquals("ab", Strings.removeEnd("abc", "c"));

            assertEquals("", Strings.removeEndIgnoreCase("", "add"));
            assertEquals(null, Strings.removeEndIgnoreCase(null, "add"));

            assertEquals("abc", Strings.removeEndIgnoreCase("abc", null));
            assertEquals("abc", Strings.removeEndIgnoreCase("abc", ""));

            assertEquals("abc", Strings.removeEndIgnoreCase("abc", "123"));
            assertEquals("abc", Strings.removeEndIgnoreCase("abc", "b"));

            assertEquals("ab", Strings.removeEndIgnoreCase("abc", "C"));
            assertEquals("ab", Strings.removeEndIgnoreCase("abc", "c"));
        }

        {
            assertEquals("", Strings.removeAll("", 'a'));
            assertEquals(null, Strings.removeAll(null, 'a'));

            assertEquals("abc", Strings.removeAll("abc", 'd'));

            assertEquals("ac", Strings.removeAll("abc", 'b'));

            assertEquals("", Strings.removeAll("", "add"));
            assertEquals(null, Strings.removeAll(null, "add"));

            assertEquals("abc", Strings.removeAll("abc", "d"));

            assertEquals("ac", Strings.removeAll("abc", "b"));
        }
    }

    @Test
    public void test_repeat() {
        {

            Strings.repeat('a', 0);

            Strings.repeat("abc", 0);

            Strings.repeat("abc", 1, null);
        }

        {
            final String str = "a";
            for (int i = 1; i < 71; i++) {
                final String str1 = Strings.repeat('a', i);

                String str2 = "";
                for (int j = 0; j < i; j++) {
                    str2 += str;
                }

                assertEquals(str1, str2);
            }
        }

        {
            final String str = "a";
            for (int i = 1; i < 71; i++) {
                final String str1 = Strings.repeat(str, i);

                String str2 = "";
                for (int j = 0; j < i; j++) {
                    str2 += str;
                }

                assertEquals(str1, str2);
            }
        }

        {
            final String str = "abc";
            final String separator = "";
            for (int i = 1; i < 71; i++) {
                final String str1 = Strings.repeat(str, i, separator);

                String str2 = "";
                for (int j = 0; j < i; j++) {
                    if (j > 0) {
                        str2 += separator;
                    }

                    str2 += str;
                }

                assertEquals(str1, str2);
            }
        }

        {
            final String str = "a";
            final String separator = ", ";
            for (int i = 1; i < 71; i++) {
                final String str1 = Strings.repeat("a", i, separator);

                String str2 = "";
                for (int j = 0; j < i; j++) {
                    if (j > 0) {
                        str2 += separator;
                    }

                    str2 += str;
                }

                assertEquals(str1, str2);
            }
        }
    }

    @Test
    public void test_split() {
        {
            final String[] strs = "".split(", ");
            assertEquals(1, strs.length);
            assertTrue(N.equals(N.asArray(""), strs));
        }

        {
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.split((String) null, '*')));
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.split("", '*')));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a.b.c", '.')));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a..b.c", '.')));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a b c", " ")));
        }

        {
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.splitPreserveAllTokens(null, '*')));
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.split("", '*')));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a.b.c", '.')));
            assertTrue(N.equals(N.asArray("a", "", "b", "c"), Strings.splitPreserveAllTokens("a..b.c", '.')));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a b c", " ")));
        }

        {
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.split((String) null, '*')));
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.split("", "*")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a.b.c", ".")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a..b.c", ".")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a b c", " ")));
            assertTrue(N.equals(N.asArray("ab", "cd:ef"), Strings.split("ab:cd:ef", ":", 2)));
        }

        {
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.splitPreserveAllTokens(null, '*')));
            assertTrue(N.equals(N.asArray(""), Strings.splitPreserveAllTokens("", "*")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a.b.c", ".")));
            assertTrue(N.equals(N.asArray("a", "", "b", "c"), Strings.splitPreserveAllTokens("a..b.c", ".")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a b c", " ")));
        }

        {
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.split((String) null, '*')));
            assertTrue(N.equals(N.EMPTY_STRING_ARRAY, Strings.split("", "*")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a..b..c", "..")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a....b..c", "..")));
            assertTrue(N.equals(N.asArray("a", "b", "c"), Strings.split("a  b  c", " ")));
            assertArrayEquals(N.asArray("ab", "cd:;ef"), Strings.split("ab:;cd:;ef", ":;", 2));
        }

    }

    @Test
    public void testRemoveElementForAllArray() {

        {
            boolean[] a = {};

            assertTrue(Arrays.equals(N.EMPTY_BOOLEAN_ARRAY, N.removeAllOccurrences(a, true)));

            a = new boolean[] { true };
            assertTrue(Arrays.equals(N.EMPTY_BOOLEAN_ARRAY, N.removeAllOccurrences(a, true)));

            a = new boolean[] { true, true };
            assertTrue(Arrays.equals(N.EMPTY_BOOLEAN_ARRAY, N.removeAllOccurrences(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { false, false }, N.removeAllOccurrences(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { true, true, true }, N.removeAllOccurrences(a, false)));

            // test false ...
            //            a = null;
            //            try {
            //                assertNull(N.remove(a, true));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new boolean[0];
            assertTrue(Arrays.equals(N.EMPTY_BOOLEAN_ARRAY, N.remove(a, true)));

            a = new boolean[] { true };
            assertTrue(Arrays.equals(N.EMPTY_BOOLEAN_ARRAY, N.remove(a, true)));

            a = new boolean[] { true, true };
            assertTrue(Arrays.equals(new boolean[] { true }, N.remove(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { false, true, false, true }, N.remove(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { true, true, false, true }, N.remove(a, false)));
        }

        {
            char[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, '2'));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_CHAR_ARRAY, N.removeAllOccurrences(a, '2')));

            a = new char[] { '2' };
            assertTrue(Arrays.equals(N.EMPTY_CHAR_ARRAY, N.removeAllOccurrences(a, '2')));

            a = new char[] { '2', '2' };
            assertTrue(Arrays.equals(N.EMPTY_CHAR_ARRAY, N.removeAllOccurrences(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '3' }, N.removeAllOccurrences(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '2', '2', '3', '2' }, N.removeAllOccurrences(a, '4')));

            // test false ...
            a = null;

            //            try {
            //                assertNull(N.remove(a, '2'));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new char[0];
            assertTrue(Arrays.equals(N.EMPTY_CHAR_ARRAY, N.remove(a, '2')));

            a = new char[] { '2' };
            assertTrue(Arrays.equals(N.EMPTY_CHAR_ARRAY, N.remove(a, '2')));

            a = new char[] { '2', '2' };
            assertTrue(Arrays.equals(new char[] { '2' }, N.remove(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '2', '3', '2' }, N.remove(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '2', '2', '3', '2' }, N.remove(a, '4')));
        }

        {
            byte[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, (byte) 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_BYTE_ARRAY, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_BYTE_ARRAY, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 2, 2 };
            assertTrue(Arrays.equals(N.EMPTY_BYTE_ARRAY, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 3 }, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, (byte) 4)));

            //            // test false ...
            //            a = null;
            //
            //            try {
            //                assertNull(N.remove(a, (byte) 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new byte[0];
            assertTrue(Arrays.equals(N.EMPTY_BYTE_ARRAY, N.remove(a, (byte) 2)));

            a = new byte[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_BYTE_ARRAY, N.remove(a, (byte) 2)));

            a = new byte[] { 2, 2 };
            assertTrue(Arrays.equals(new byte[] { 2 }, N.remove(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 2, 3, 2 }, N.remove(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 2, 2, 3, 2 }, N.remove(a, (byte) 4)));
        }

        {
            short[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, (short) 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_SHORT_ARRAY, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_SHORT_ARRAY, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 2, 2 };
            assertTrue(Arrays.equals(N.EMPTY_SHORT_ARRAY, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 3 }, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, (short) 4)));
            //
            //            // test false ...
            //            a = null;
            //
            //            try {
            //                assertNull(N.remove(a, (short) 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new short[0];
            assertTrue(Arrays.equals(N.EMPTY_SHORT_ARRAY, N.remove(a, (short) 2)));

            a = new short[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_SHORT_ARRAY, N.remove(a, (short) 2)));

            a = new short[] { 2, 2 };
            assertTrue(Arrays.equals(new short[] { 2 }, N.remove(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 2, 3, 2 }, N.remove(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 2, 2, 3, 2 }, N.remove(a, (short) 4)));
        }

        {
            int[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_INT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new int[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_INT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new int[] { 2, 2 };
            assertTrue(Arrays.equals(N.EMPTY_INT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            //            // test false ...
            //            a = null;
            //
            //            try {
            //                assertNull(N.remove(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new int[0];
            assertTrue(Arrays.equals(N.EMPTY_INT_ARRAY, N.remove(a, 2)));

            a = new int[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_INT_ARRAY, N.remove(a, 2)));

            a = new int[] { 2, 2 };
            assertTrue(Arrays.equals(new int[] { 2 }, N.remove(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            long[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_LONG_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new long[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_LONG_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new long[] { 2, 2 };
            assertTrue(Arrays.equals(N.EMPTY_LONG_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            //            // test false ...
            //            a = null;
            //
            //            try {
            //                assertNull(N.remove(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new long[0];
            assertTrue(Arrays.equals(N.EMPTY_LONG_ARRAY, N.remove(a, 2)));

            a = new long[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_LONG_ARRAY, N.remove(a, 2)));

            a = new long[] { 2, 2 };
            assertTrue(Arrays.equals(new long[] { 2 }, N.remove(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            float[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_FLOAT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new float[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_FLOAT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new float[] { 2, 2 };
            assertTrue(Arrays.equals(N.EMPTY_FLOAT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            //            // test false ...
            //            a = null;
            //
            //            try {
            //                assertNull(N.remove(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new float[0];
            assertTrue(Arrays.equals(N.EMPTY_FLOAT_ARRAY, N.remove(a, 2)));

            a = new float[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_FLOAT_ARRAY, N.remove(a, 2)));

            a = new float[] { 2, 2 };
            assertTrue(Arrays.equals(new float[] { 2 }, N.remove(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            double[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_DOUBLE_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new double[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_DOUBLE_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new double[] { 2, 2 };
            assertTrue(Arrays.equals(N.EMPTY_DOUBLE_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            //            // test false ...
            //            a = null;
            //
            //            try {
            //                assertNull(N.remove(a, 2));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new double[0];
            assertTrue(Arrays.equals(N.EMPTY_DOUBLE_ARRAY, N.remove(a, 2)));

            a = new double[] { 2 };
            assertTrue(Arrays.equals(N.EMPTY_DOUBLE_ARRAY, N.remove(a, 2)));

            a = new double[] { 2, 2 };
            assertTrue(Arrays.equals(new double[] { 2 }, N.remove(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            String[] a = {};

            //            try {
            //                assertNull(N.removeAllOccurrences(a, "2"));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            assertTrue(Arrays.equals(N.EMPTY_STRING_ARRAY, N.removeAllOccurrences(a, "2")));

            a = new String[] { "2" };
            assertTrue(Arrays.equals(N.EMPTY_STRING_ARRAY, N.removeAllOccurrences(a, "2")));

            a = new String[] { "2", "2" };
            assertTrue(Arrays.equals(N.EMPTY_STRING_ARRAY, N.removeAllOccurrences(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "3" }, N.removeAllOccurrences(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "2", "2", "3", "2" }, N.removeAllOccurrences(a, "4")));

            //            // test false ...
            //            a = null;
            //
            //            try {
            //                assertNull(N.remove(a, "2"));
            //                fail("Should throw NullPointerException");
            //            } catch (NullPointerException e) {
            //
            //            }

            a = new String[0];
            assertTrue(Arrays.equals(N.EMPTY_STRING_ARRAY, N.remove(a, "2")));

            a = new String[] { "2" };
            assertTrue(Arrays.equals(N.EMPTY_STRING_ARRAY, N.remove(a, "2")));

            a = new String[] { "2", "2" };
            assertTrue(Arrays.equals(new String[] { "2" }, N.remove(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "2", "3", "2" }, N.remove(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "2", "2", "3", "2" }, N.remove(a, "4")));
        }
    }

    @Test
    public void test_array_deleteAllByIndices() {
        final int len = 37;

        {
            final int[] a = new int[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt();
            }

            final int[] indices = new int[17];
            for (int i = 0; i < 17; i++) {
                indices[i] = Math.abs(rand.nextInt(len));
            }

            final IntList list = IntList.of(new int[a.length - indices.length], 0);
            for (int i = 0; i < len; i++) {
                if (N.indexOf(indices, i) < 0) {
                    list.add(a[i]);
                }
            }

            assertTrue(N.equals(list.trimToSize().array(), N.deleteAllByIndices(a, indices)));
        }

        {
            final long[] a = new long[len];
            for (int i = 0; i < len; i++) {
                a[i] = rand.nextInt();
            }

            final int[] indices = new int[17];
            for (int i = 0; i < 17; i++) {
                indices[i] = Math.abs(rand.nextInt(len));
            }

            final LongList list = LongList.of(new long[a.length - indices.length], 0);
            for (int i = 0; i < len; i++) {
                if (N.indexOf(indices, i) < 0) {
                    list.add(a[i]);
                }
            }

            assertTrue(N.equals(list.trimToSize().array(), N.deleteAllByIndices(a, indices)));
        }
    }

    @Test
    public void test_array_op() {
        {
            boolean[] a = { true, false };
            a = N.add(a, false);
            assertEquals(false, a[2]);
            a = N.addAll(a, true, true);
            assertEquals(true, a[3]);
            assertEquals(true, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(false, a[2]);

            a = N.remove(a, true);
            assertEquals(false, a[0]);

            a = N.removeAllOccurrences(a, true);
            assertEquals(false, a[0]);

            a = N.removeAll(a, true, false, false);
            assertEquals(0, a.length);

            assertEquals(0, a.length);

            a = N.insert(a, 0, false);
            assertEquals(false, a[0]);

            a = N.insert(a, 0, true);
            assertEquals(false, a[1]);

            a = N.removeAllOccurrences(a, true);
            assertEquals(false, a[0]);

            a = N.removeAll(a);
            assertEquals(1, a.length);
            a = N.removeAll(a);
            assertEquals(1, a.length);
        }

        {
            char[] a = { 'a', 'b' };
            a = N.add(a, 'c');
            assertEquals('c', a[2]);
            a = N.addAll(a, 'd', 'e');
            assertEquals('d', a[3]);
            assertEquals('e', a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals('c', a[2]);

            a = N.remove(a, 'a');
            assertEquals('b', a[0]);

            a = N.removeAllOccurrences(a, 'b');
            assertEquals('c', a[0]);

            a = N.removeAll(a, 'c', 'c', 'a');
            assertEquals(0, a.length);

            a = N.insert(a, 0, 'a');
            assertEquals('a', a[0]);

            a = N.insert(a, 0, 'b');
            assertEquals('a', a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a, new char[0]);
            assertEquals(2, a.length);
        }

        {
            byte[] a = { 1, 2 };
            a = N.add(a, (byte) 3);
            assertEquals(3, a[2]);
            a = N.addAll(a, (byte) 4, (byte) 5);
            assertEquals(4, a[3]);
            assertEquals(5, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(3, a[2]);

            a = N.remove(a, (byte) 1);
            assertEquals(2, a[0]);

            a = N.removeAllOccurrences(a, (byte) 2);
            assertEquals(3, a[0]);

            a = N.removeAll(a, (byte) 3, (byte) 3, (byte) 1);
            assertEquals(0, a.length);

            a = N.insert(a, 0, (byte) 1);
            assertEquals(1, a[0]);

            a = N.insert(a, 0, (byte) 2);
            assertEquals(1, a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a, new byte[0]);
            assertEquals(2, a.length);
        }

        {
            short[] a = { 1, 2 };
            a = N.add(a, (short) 3);
            assertEquals(3, a[2]);
            a = N.addAll(a, (short) 4, (short) 5);
            assertEquals(4, a[3]);
            assertEquals(5, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(3, a[2]);

            a = N.remove(a, (short) 1);
            assertEquals(2, a[0]);

            a = N.removeAllOccurrences(a, (short) 2);
            assertEquals(3, a[0]);

            a = N.removeAll(a, (short) 3, (short) 3, (short) 1);
            assertEquals(0, a.length);

            a = N.insert(a, 0, (short) 1);
            assertEquals(1, a[0]);

            a = N.insert(a, 0, (short) 2);
            assertEquals(1, a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a, new short[0]);
            assertEquals(2, a.length);
        }

        {
            int[] a = { 1, 2 };
            a = N.add(a, 3);
            assertEquals(3, a[2]);
            a = N.addAll(a, 4, 5);
            assertEquals(4, a[3]);
            assertEquals(5, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(3, a[2]);

            a = N.remove(a, 1);
            assertEquals(2, a[0]);

            a = N.removeAllOccurrences(a, 2);
            assertEquals(3, a[0]);

            a = N.removeAll(a, 3, 3, 1);
            assertEquals(0, a.length);

            a = N.insert(a, 0, 1);
            assertEquals(1, a[0]);

            a = N.insert(a, 0, 2);
            assertEquals(1, a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a);
            assertEquals(2, a.length);
        }

        {
            long[] a = { 1, 2 };
            a = N.add(a, 3);
            assertEquals(3, a[2]);
            a = N.addAll(a, 4, 5);
            assertEquals(4, a[3]);
            assertEquals(5, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(3, a[2]);

            a = N.remove(a, 1);
            assertEquals(2, a[0]);

            a = N.removeAllOccurrences(a, 2);
            assertEquals(3, a[0]);

            a = N.removeAll(a, 3, 3, 1);
            assertEquals(0, a.length);

            a = N.insert(a, 0, 1);
            assertEquals(1, a[0]);

            a = N.insert(a, 0, 2);
            assertEquals(1, a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a);
            assertEquals(2, a.length);
        }

        {
            float[] a = { 1, 2 };
            a = N.add(a, 3);
            assertEquals(3f, a[2]);
            a = N.addAll(a, 4, 5);
            assertEquals(4f, a[3]);
            assertEquals(5f, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(3f, a[2]);

            a = N.remove(a, 1);
            assertEquals(2f, a[0]);

            a = N.removeAllOccurrences(a, 2);
            assertEquals(3f, a[0]);

            a = N.removeAll(a, 3, 3, 1);
            assertEquals(0, a.length);

            a = N.insert(a, 0, 1);
            assertEquals(1f, a[0]);

            a = N.insert(a, 0, 2);
            assertEquals(1f, a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a);
            assertEquals(2, a.length);
        }

        {
            double[] a = { 1, 2 };
            a = N.add(a, 3);
            assertEquals(3d, a[2]);
            a = N.addAll(a, 4, 5);
            assertEquals(4d, a[3]);
            assertEquals(5d, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(3d, a[2]);

            a = N.remove(a, 1);
            assertEquals(2d, a[0]);

            a = N.removeAllOccurrences(a, 2);
            assertEquals(3d, a[0]);

            a = N.removeAll(a, 3, 3, 1);
            assertEquals(0, a.length);

            a = N.insert(a, 0, 1);
            assertEquals(1d, a[0]);

            a = N.insert(a, 0, 2);
            assertEquals(1d, a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a);
            assertEquals(2, a.length);
        }

        {
            Object[] a = { 1, 2 };
            a = N.add(a, 3);
            assertEquals(3, a[2]);
            a = N.addAll(a, 4, 5);
            assertEquals(4, a[3]);
            assertEquals(5, a[4]);

            a = N.deleteByIndex(a, 3);
            a = N.deleteAllByIndices(a, 3);
            assertEquals(3, a[2]);

            a = N.remove(a, 1);
            assertEquals(2, a[0]);

            a = N.removeAllOccurrences(a, 2);
            assertEquals(3, a[0]);

            a = N.removeAll(a, 3, 3, 1);
            assertEquals(0, a.length);

            a = N.insert(a, 0, 1);
            assertEquals(1, a[0]);

            a = N.insert(a, 0, 2);
            assertEquals(1, a[1]);

            a = N.removeAll(a);
            assertEquals(2, a.length);
            a = N.removeAll(a);
            assertEquals(2, a.length);
        }

        {
            final List<Integer> list = N.asList(1, 2, 1, 1);

            list.remove(Integer.valueOf(1));
            assertEquals(3, list.size());
            list.remove(Integer.valueOf(1));
            assertEquals(2, list.size());
        }

        {
            List<?> list = N.asList(1, 2, 3);
            N.deleteAllByIndices(list, 0, 1, 2);
            N.println(list);
            assertEquals(0, list.size());

            list = N.asList(1, 2, 3);
            N.deleteAllByIndices(list, 0, 1, 1);
            N.println(list);
            assertEquals(N.asList(3), list);

            list = N.asList(1, 2, 3);
            N.deleteAllByIndices(list, 0, 2);
            N.println(list);
            assertEquals(N.asList(2), list);

            list = N.asList(1, 2, 3);
            N.deleteAllByIndices(list, 1);
            N.println(list);
            assertEquals(N.asList(1, 3), list);

            list = N.asList(1, 2, 2, 3);
            N.deleteAllByIndices(list, 1);
            N.println(list);
            assertEquals(N.asList(1, 2, 3), list);
        }
    }

    @Test
    public void test_array_op_2() {
        {
            final boolean[] a = { true, false };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new boolean[] { true, true, false, false }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final char[] a = { 'a', 'b' };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new char[] { 'a', 'a', 'b', 'b' }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final byte[] a = { 1, 2 };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new byte[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final short[] a = { 1, 2 };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new short[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final int[] a = { 1, 2 };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new int[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final long[] a = { 1, 2 };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new long[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final float[] a = { 1, 2 };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new float[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final double[] a = { 1, 2 };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new double[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final Object[] a = { 1, 2 };
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(N.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(N.equals(new Object[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(N.equals(a, N.deleteAllByIndices(N.insertAll(a, 1, a), 1, 2)));
        }
    }

    @Test
    public void test_removeDuplicates() {
        {
            final char[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new char[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new byte[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new short[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new int[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new long[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new float[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new double[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final Object[] a = { '1', '2', '1', '3' };
            assertTrue(N.equals(new Object[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final List<Character> c = N.asList('1', '2', '1', '3');
            N.removeDuplicates(c);
            assertTrue(N.equals(N.asList('1', '2', '3'), c));
        }
    }

    @Test
    public void test_removeDuplicates_sorted() {
        {
            final char[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new char[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new byte[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new short[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new int[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new long[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new float[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new double[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final Character[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.equals(new Character[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final List<Character> c = N.asList('1', '2', '1', '3', '4', '3', '3', '2');
            N.sort(c);
            N.removeDuplicates(c, true);
            assertTrue(N.equals(N.asList('1', '2', '3', '4'), c));
        }
    }

    @Test
    public void test_hasDuplicates() {
        {
            final char[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final Object[] a = { '1', '2', '1', '3' };
            assertTrue(N.hasDuplicates(a));
        }

        {
            final List<?> a = N.asList('1', '2', '1', '3');
            assertTrue(N.hasDuplicates(a));
        }
    }

    @Test
    public void test_hasDuplicates_sorted() {
        {
            final char[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final Character[] a = { '1', '2', '1', '3' };
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }

        {
            final List<Character> a = N.asList('1', '2', '1', '3');
            N.sort(a);
            assertTrue(N.hasDuplicates(a, true));
        }
    }

    @Test
    public void test_hasDuplicates_2() {
        {
            final char[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final byte[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final short[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final int[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final long[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final float[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final double[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final Object[] a = { '1', '2', '3' };
            assertFalse(N.hasDuplicates(a));
        }

        {
            final List<?> a = N.asList('1', '2', '3');
            assertFalse(N.hasDuplicates(a));
        }
    }

    @Test
    public void test_replaceAll() {
        {
            boolean[] a = { true, false, false };
            N.replaceAll(a, false, true);
            for (final boolean element : a) {
                assertEquals(true, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, false, true) > 0);
        }

        {
            char[] a = { '1', '2', '2' };
            N.replaceAll(a, '2', '1');
            for (final char element : a) {
                assertEquals('1', element);
            }

            a = null;
            assertFalse(N.replaceAll(a, '2', '1') > 0);
        }

        {
            byte[] a = { 1, 2, 2 };
            N.replaceAll(a, (byte) 2, (byte) 1);
            for (final byte element : a) {
                assertEquals(1, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, (byte) 2, (byte) 1) > 0);
        }

        {
            short[] a = { 1, 2, 2 };
            N.replaceAll(a, (short) 2, (short) 1);
            for (final short element : a) {
                assertEquals(1, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, (short) 2, (short) 1) > 0);
        }

        {
            int[] a = { 1, 2, 2 };
            N.replaceAll(a, 2, 1);
            for (final int element : a) {
                assertEquals(1, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, 2, 1) > 0);
        }

        {
            long[] a = { 1, 2, 2 };
            N.replaceAll(a, 2, 1);
            for (final long element : a) {
                assertEquals(1, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, 2, 1) > 0);
        }

        {
            float[] a = { 1, 2, 2 };
            N.replaceAll(a, 2, 1);
            for (final float element : a) {
                assertEquals(1f, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, 2, 1) > 0);
        }

        {
            double[] a = { 1, 2, 2 };
            N.replaceAll(a, 2, 1);
            for (final double element : a) {
                assertEquals(1d, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, 2, 1) > 0);
        }

        {
            int[] a = Array.of(1, 2, 2);
            N.replaceAll(a, 2, 1);
            for (final int element : a) {
                assertEquals(1, element);
            }

            a = null;
            assertFalse(N.replaceAll(a, 2, 1) > 0);
        }

        {
            List<Integer> a = N.asList(1, 2, 2);
            N.replaceAll(a, 2, 1);
            for (final Integer element : a) {
                assertEquals(1, (int) element);
            }

            a = null;
            assertFalse(N.replaceAll(a, 2, 1) > 0);
        }

        {
            List<String> list = N.asList("b", "b", "b");

            for (int k = 5; k <= 1001; k++) {
                list = N.asLinkedList();
                N.fill(list, 0, k, "123");
                N.replaceAll(list, "123", "abc");
                // N.println(list);

                for (int j = 0; j < k; j++) {
                    assertEquals("abc", list.get(j));
                }
            }
        }

        {
            List<String> list = N.asList("b", "b", "b");

            for (int k = 5; k <= 1001; k++) {
                list = N.asLinkedList();
                N.fill(list, 0, k, null);
                N.replaceAll(list, null, "abc");
                // N.println(list);

                for (int j = 0; j < k; j++) {
                    assertEquals("abc", list.get(j));
                }
            }
        }
    }

    @Test
    public void test_equalsIgnoreCase() {

        {
            final String[] a = N.asArray("abC");
            final String[] b = N.asArray("aBc");

            assertTrue(N.equalsIgnoreCase(a[0], b[0]));
            assertTrue(N.equalsIgnoreCase(a, b));
        }
    }

    @Test
    public void test_NotNullOrEmpty() {
        {
            final DataSet parameter = null;
            assertFalse(N.notEmpty(parameter));
        }

        {
            final String parameter = "" + Strings.CHAR_CR;
            assertFalse(Strings.isNotBlank(parameter));
        }
    }

    @Test
    public void test_swap() {

        {
            final List<?> a = N.asList(1, 2, 3);
            final List<?> b = new ArrayList<>(a);

            N.swap(a, 0, 2);

            assertEquals(a.get(0), b.get(2));
            assertEquals(a.get(2), b.get(0));
        }

        {
            final Object[] a = { 1, 2, 3 };
            final Object[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final boolean[] a = { false, true, true };
            final boolean[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final char[] a = { '1', '2', '3' };
            final char[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final byte[] a = { 1, 2, 3 };
            final byte[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final short[] a = { 1, 2, 3 };
            final short[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final int[] a = { 1, 2, 3 };
            final int[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final long[] a = { 1, 2, 3 };
            final long[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final float[] a = { 1, 2, 3 };
            final float[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final double[] a = { 1, 2, 3 };
            final double[] b = N.copyOf(a, a.length);

            N.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

    }

    @Test
    public void test_reverse() {
        {
            final String str = "a.b.c";
            String str2 = Strings.reverseDelimited(str, '.');
            N.println(str2);
            assertFalse(N.equals(str, str2));

            str2 = Strings.reverseDelimited(str2, '.');
            assertTrue(N.equals(str, str2));
        }

        {

            final String str = "abc";
            String str2 = Strings.reverse(str);
            assertFalse(N.equals(str, str2));

            str2 = Strings.reverse(str2);
            assertTrue(N.equals(str, str2));
        }

        {
            final List<Object> list = null;
            N.reverse(list);
        }

        {
            final Object[] a = null;
            N.reverse(a);
        }

        {
            final boolean[] a = null;
            N.reverse(a);
        }

        {
            final char[] a = null;
            N.reverse(a);
        }

        {
            final byte[] a = null;
            N.reverse(a);
        }

        {
            final short[] a = null;
            N.reverse(a);
        }

        {
            final int[] a = null;
            N.reverse(a);
        }

        {
            final long[] a = null;
            N.reverse(a);
        }

        {
            final float[] a = null;
            N.reverse(a);
        }

        {
            final double[] a = null;
            N.reverse(a);
        }

        {
            final List<?> a = N.asList(1, 2, 3);
            final List<?> b = new ArrayList<>(a);

            N.reverse(a);

            for (int i = 0, len = a.size(); i < len; i++) {
                assertEquals(a.get(i), b.get(len - i - 1));
            }
        }

        {
            final List<?> a = N.asList(1, 2, 3, 4, 5, 6);
            final List<?> b = new ArrayList<>(a);

            N.reverse(a);

            for (int i = 0, len = a.size(); i < len; i++) {
                assertEquals(a.get(i), b.get(len - i - 1));
            }
        }

        {
            for (int i = 0; i <= 1001; i++) {
                final List<Object> a = new ArrayList<>(i);

                for (int j = 0; j < i; j++) {
                    a.add(j);
                }

                final List<Object> b = new ArrayList<>(a);
                N.reverse(b);

                for (int k = 0, size = a.size(); k < size; k++) {
                    assertEquals(a.get(k), b.get(size - k - 1));
                }
            }
        }

        {
            for (int i = 5; i <= 1001; i++) {
                final List<Object> a = new ArrayList<>(i);

                for (int j = 0; j < i; j++) {
                    a.add(j);
                }

                final List<Object> b = new ArrayList<>(a);
                final int fromIndex = 3;
                final int toIndex = b.size() - 2;
                N.reverse(b, fromIndex, toIndex);
                // N.println(b);

                for (int k = 0, size = toIndex - fromIndex; k < size; k++) {
                    assertEquals(a.get(k + fromIndex), b.get(toIndex - k - 1));
                }
            }
        }

        {
            for (int i = 5; i <= 1001; i++) {
                final List<Object> a = new ArrayList<>(i);

                for (int j = 0; j < i; j++) {
                    a.add(j);
                }

                final List<Object> b = new LinkedList<>(a);
                final int fromIndex = 3;
                final int toIndex = b.size() - 2;
                N.reverse(b, fromIndex, toIndex);
                // N.println(b);

                for (int k = 0, size = toIndex - fromIndex; k < size; k++) {
                    assertEquals(a.get(k + fromIndex), b.get(toIndex - k - 1));
                }
            }
        }

        {
            final Object[] a = { 1, 2, 3 };
            final Object[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final boolean[] a = { false, true, true };
            final boolean[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final char[] a = { '1', '2', '3' };
            final char[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final byte[] a = { 1, 2, 3 };
            final byte[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final short[] a = { 1, 2, 3 };
            final short[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final int[] a = { 1, 2, 3 };
            final int[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final long[] a = { 1, 2, 3 };
            final long[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final float[] a = { 1, 2, 3 };
            final float[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final double[] a = { 1, 2, 3 };
            final double[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final boolean[] a = { false, true, false, false, true, true, true, true, true };
            final boolean[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final char[] a = { '1', '2', '3', '4', '5', '6', '7', '8', '9' };
            final char[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final byte[] a = { 1, 2, 3, 4, 5, 6 };
            final byte[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final short[] a = { 1, 2, 3, 4, 5, 6 };
            final short[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final int[] a = { 1, 2, 3, 4, 5, 6 };
            final int[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final long[] a = { 1, 2, 3, 4, 5, 6 };
            final long[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final float[] a = { 1, 2, 3, 4, 5, 6 };
            final float[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final double[] a = { 1, 2, 3, 4, 5, 6 };
            final double[] b = N.copyOf(a, a.length);
            N.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

    }

    @Test
    public void test_checkNullOrEmpty() {
        //        try {
        //            Object parameter = null;
        //            N.checkNull(parameter, "parameter");
        //            fail("Should throw IllegalArgumentException");
        //        } catch (IllegalArgumentException e) {
        //            assertEquals("The parameter 'parameter' can't be null", e.getMessage());
        //        }

        try {
            final String parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Object[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final boolean[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final char[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final byte[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final short[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final int[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final long[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final float[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final double[] parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final List<String> parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Map<String, Object> parameter = null;
            N.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        //        try {
        //            NodeList parameter = null;
        //            N.checkArgNotEmpty(parameter, "parameter");
        //            fail("Should throw IllegalArgumentException");
        //        } catch (IllegalArgumentException e) {
        //            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        //        }
        //
        //        try {
        //            NodeList parameter = null;
        //            N.checkArgNotEmpty(parameter, "NodeList can't be null");
        //            fail("Should throw IllegalArgumentException");
        //        } catch (IllegalArgumentException e) {
        //            assertEquals("NodeList can't be null", e.getMessage());
        //        }

        try {
            final String parameter = null;
            N.checkArgNotBlank(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty or blank", e.getMessage());
        }

        //        try {
        //            Object parameter = null;
        //            N.checkNull(parameter, "'parameter' cannot be null or empty");
        //            fail("Should throw IllegalArgumentException");
        //        } catch (IllegalArgumentException e) {
        //            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        //        }

        try {
            final String parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Object[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final boolean[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final char[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final byte[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final short[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final int[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final long[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final float[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final double[] parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final List<String> parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Map<String, Object> parameter = null;
            N.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final String parameter = null;
            N.checkArgNotBlank(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }
    }
    //
    //    @Test
    //    public void test_join() {
    //
    //        assertEquals("", Strings.join(new ArrayList<>(), WD.COMMA_SPACE));
    //        assertEquals("true, false, true", Strings.join(N.asList(true, false, true), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(), WD.COMMA_SPACE));
    //        assertEquals("true, false, true", Strings.join(Array.of(true, false, true), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(new char[0]), WD.COMMA_SPACE));
    //        assertEquals("1, 2, 3", Strings.join(Array.of('1', '2', '3'), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(new byte[0]), WD.COMMA_SPACE));
    //        assertEquals("1, 2, 3", Strings.join(Array.of((byte) 1, (byte) 2, (byte) 3), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(new short[0]), WD.COMMA_SPACE));
    //        assertEquals("1, 2, 3", Strings.join(Array.of((short) 1, (short) 2, (short) 3), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(), WD.COMMA_SPACE));
    //        assertEquals("1, 2, 3", Strings.join(Array.of(1, 2, 3), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(), WD.COMMA_SPACE));
    //        assertEquals("1, 2, 3", Strings.join(Array.of(1l, 2l, 3l), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(), WD.COMMA_SPACE));
    //        assertEquals("1.0, 2.0, 3.0", Strings.join(Array.of(1f, 2f, 3f), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(), WD.COMMA_SPACE));
    //        assertEquals("1.0, 2.0, 3.0", Strings.join(Array.of(1d, 2d, 3d), WD.COMMA_SPACE));
    //
    //        assertEquals("", Strings.join(Array.of(), WD._COMMA));
    //        assertEquals("true,false,true", Strings.join(Array.of(true, false, true), WD._COMMA));
    //
    //        assertEquals("", Strings.join(Array.of(new char[0]), WD._COMMA));
    //        assertEquals("1,2,3", Strings.join(Array.of('1', '2', '3'), WD._COMMA));
    //
    //        assertEquals("", Strings.join(Array.of(new byte[0]), WD._COMMA));
    //        assertEquals("1,2,3", Strings.join(Array.of((byte) 1, (byte) 2, (byte) 3), WD._COMMA));
    //
    //        assertEquals("", Strings.join(Array.of(new short[0]), WD._COMMA));
    //        assertEquals("1,2,3", Strings.join(Array.of((short) 1, (short) 2, (short) 3), WD._COMMA));
    //
    //        assertEquals("", Strings.join(Array.of(), WD._COMMA));
    //        assertEquals("1,2,3", Strings.join(Array.of(1, 2, 3), WD._COMMA));
    //
    //        assertEquals("", Strings.join(Array.of(), WD._COMMA));
    //        assertEquals("1,2,3", Strings.join(Array.of(1l, 2l, 3l), WD._COMMA));
    //
    //        assertEquals("", Strings.join(Array.of(), WD._COMMA));
    //        assertEquals("1.0,2.0,3.0", Strings.join(Array.of(1f, 2f, 3f), WD._COMMA));
    //
    //        assertEquals("", Strings.join(Array.of(), WD._COMMA));
    //        assertEquals("1.0,2.0,3.0", Strings.join(Array.of(1d, 2d, 3d), WD._COMMA));
    //
    //        assertEquals("abc", Strings.join(N.asArray("abc"), ","));
    //        assertEquals("null", Strings.join(new Object[] { null }, ","));
    //        assertEquals("null", Strings.join(new Object[] { "null" }, ","));
    //
    //        assertEquals("", Strings.join(new ArrayList<>(), WD._COMMA));
    //        assertEquals("1, 2, 3", Strings.join(N.asList("1", "2", "3")));
    //        assertEquals("1,2,3", Strings.join(N.asList("1", "2", "3"), WD._COMMA));
    //        assertEquals("2", Strings.join(N.asList("1", "2", "3"), 1, 2, WD._COMMA));
    //
    //        assertEquals("", Strings.joinEntries(new HashMap<>(), WD._COMMA));
    //        assertEquals("a=1, b=2, c=3", Strings.joinEntries(N.asLinkedHashMap("a", "1", "b", "2", "c", "3")));
    //        assertEquals("a=1,b=2,c=3", Strings.joinEntries(N.asLinkedHashMap("a", "1", "b", "2", "c", "3"), WD._COMMA));
    //        assertEquals("b=2", Strings.joinEntries(N.asLinkedHashMap("a", "1", "b", "2", "c", "3"), 1, 2, WD._COMMA));
    //    }

    @Test
    public void test_createNumber() throws Exception {
        N.println(Numbers.createInteger(null));
        N.println(StrUtil.createInteger(""));
        N.println(Numbers.createInteger("123"));
        N.println(Numbers.createInteger("0x123"));
        N.println(Numbers.createLong(null));
        N.println(StrUtil.createLong(""));
        N.println(Numbers.createLong("123"));
        N.println(Numbers.createLong("123l"));
        N.println(Numbers.createLong("123L"));
        N.println(Numbers.createLong("0X123"));
        N.println(Numbers.createFloat(null));
        N.println(StrUtil.createFloat(""));
        N.println(Numbers.createFloat("123"));
        N.println(Numbers.createFloat("123.0139f"));
        N.println(Numbers.createDouble("123e139f"));
        N.println(Numbers.createDouble(null));
        N.println(StrUtil.createDouble(""));
        N.println(Numbers.createDouble("123"));
        N.println(Numbers.createDouble("123.0139d"));
        N.println(Numbers.createDouble("123e139d"));

        N.println(Numbers.createBigInteger(null));
        N.println(StrUtil.createBigInteger(""));
        N.println(Numbers.createBigInteger("123"));
        N.println(Numbers.createBigInteger("0X123"));

        N.println(Numbers.createBigDecimal(null));
        N.println(StrUtil.createBigDecimal(""));
        N.println(Numbers.createBigDecimal("123"));
        N.println(Numbers.createBigDecimal("123.0139"));

        N.println(Numbers.createNumber(null));
        N.println(StrUtil.createNumber(""));
        N.println(Numbers.createNumber("123"));
        N.println(Numbers.createNumber("123l"));
        N.println(Numbers.createNumber("123.0139f"));
        N.println(Numbers.createNumber("123.0139d"));
        N.println(Numbers.createNumber("123"));
        N.println(Numbers.createNumber("-160952.54").floatValue());
        assertEquals(-160952.54f, Numbers.createNumber("-160952.54").floatValue());
    }

    @Test
    public void test_padStartEnd() throws Exception {
        String str = Strings.padStart("abc", 6);
        N.println(str);
        assertEquals("   abc", str);

        str = Strings.padStart("abc", 6, "123");
        N.println(str);
        assertEquals("123abc", str);

        str = Strings.padStart("abc", 6, "12");
        N.println(str);
        assertEquals("1212abc", str);

        str = Strings.padStart("abc", 6, "1");
        N.println(str);
        assertEquals("111abc", str);

        str = Strings.padStart("abc", 8, "1");
        N.println(str);
        assertEquals("11111abc", str);

        str = Strings.padEnd("abc", 6);
        N.println(str);
        assertEquals("abc   ", str);

        str = Strings.padEnd("abc", 6, "123");
        N.println(str);
        assertEquals("abc123", str);

        str = Strings.padEnd("abc", 6, "12");
        N.println(str);
        assertEquals("abc1212", str);

        str = Strings.padEnd("abc", 6, "1");
        N.println(str);
        assertEquals("abc111", str);

        str = Strings.padEnd("abc", 8, "1");
        N.println(str);
        assertEquals("abc11111", str);
    }

    @Test
    public void test_as() {
        {
            try {
                N.asMap("abc");
                fail("Shuld throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }

            try {
                N.asMap("a", "b", "c");
                fail("Shuld throw IllegalArgumentException");
            } catch (final IllegalArgumentException e) {

            }
        }

        {
            final List<String> list1 = ImmutableList.of("a", "b", "c");
            N.println(list1);

            final List<String> list2 = ImmutableList.wrap(N.asList("a", "b", "c"));
            N.println(list2);

            assertEquals(list1, list2);

            final Set<String> set1 = ImmutableSet.of("a", "b", "c");
            N.println(set1);

            final Set<String> set2 = ImmutableSet.wrap(N.asSet("a", "b", "c"));
            N.println(set2);

            assertEquals(set1, set2);
        }

        {
            final List<String> list1 = Collections.synchronizedList(N.asList("a", "b", "c"));
            N.println(list1);

            final List<String> list2 = Collections.synchronizedList(N.asList("a", "b", "c"));
            N.println(list2);

            assertEquals(list1, list2);

            final Collection<String> set4 = Collections.synchronizedSortedSet(N.asSortedSet("a", "b", "c"));
            N.println(set4);

            final Collection<String> set5 = Collections.synchronizedCollection(N.asSet("a", "b", "c"));
            N.println(set5);

            final Map<String, String> map1 = Collections.synchronizedMap(N.asMap("a", "1", "b", "2"));
            N.println(map1);

            @SuppressWarnings("rawtypes")
            final Map<String, String> map2 = Collections.synchronizedMap((Map) N.asMap("a", "1", "b", "2"));
            N.println(map2);

        }

        {
            Multiset<String> multiSet = N.asMultiset("a", "b", "c", "a", "a", "b");
            assertEquals(3, multiSet.getCount("a"));

            multiSet = new Multiset<>(N.asList("a", "b", "c", "a", "a", "b"));
            multiSet = new Multiset<>(N.asList("a", "b", "c", "a", "a", "b"));
            assertEquals(3, multiSet.getCount("a"));
        }

        //        {
        //            Multimap<Object, Object, List<Object>> multiMap = N.asListMultimap(createAccount(Account.class));
        //            N.println(multiMap);
        //
        //            multiMap = N.asListMultimap(createAccount(com.landawn.abacus.entity.pjo.basic.Account.class));
        //            N.println(multiMap);
        //
        //            multiMap = N.asListMultimap(Beans.bean2Map(createAccount(Account.class)));
        //            N.println(multiMap);
        //
        //            try {
        //                N.asListMultimap("abc");
        //                fail("Shuld throw IllegalArgumentException");
        //            } catch (IllegalArgumentException e) {
        //
        //            }
        //
        //            try {
        //                N.asListMultimap("a", "b", "c");
        //                fail("Shuld throw IllegalArgumentException");
        //            } catch (IllegalArgumentException e) {
        //
        //            }
        //        }
        //
        //        {
        //            Multimap<Object, Object, List<Object>> multiMap = N.asListMultimap("a", "1", "b", "2");
        //            N.println(multiMap);
        //
        //            Multimap<String, String, List<String>> multiMap2 = N.newListMultimap(N.asMap("a", "1", "b", "2"));
        //            N.println(multiMap2);
        //
        //            assertEquals(multiMap, multiMap2);
        //        }
        //
        //        {
        //            Multimap<Object, Object, List<Object>> multiMap = N.asListMultimap("a", "1", "b", "2");
        //            N.println(multiMap);
        //
        //            Multimap<String, String, List<String>> multiMap2 = N.newLinkedListMultimap(N.asMap("a", "1", "b", "2"));
        //            N.println(multiMap2);
        //
        //            assertEquals(multiMap, multiMap2);
        //        }
        //
        //        {
        //            Multimap<Object, Object, List<Object>> multiMap = N.asListMultimap("a", "1", "b", "2");
        //            N.println(multiMap);
        //
        //            Multimap<String, String, List<String>> multiMap2 = N.newSortedListMultimap(N.asMap("a", "1", "b", "2"));
        //            N.println(multiMap2);
        //
        //            assertEquals(multiMap, multiMap2);
        //        }
        //
        //        {
        //            Multimap<Object, Object, Set<Object>> multiMap = N.asSetMultimap("a", "1", "b", "2");
        //            N.println(multiMap);
        //
        //            Multimap<String, String, Set<String>> multiMap2 = N.newSetMultimap(N.asMap("a", "1", "b", "2"));
        //            N.println(multiMap2);
        //
        //            assertEquals(multiMap, multiMap2);
        //        }
        //
        //        {
        //            Multimap<Object, Object, Set<Object>> multiMap = N.asSetMultimap("a", "1", "b", "2");
        //            N.println(multiMap);
        //
        //            Multimap<String, String, Set<String>> multiMap2 = N.newLinkedSetMultimap(N.asMap("a", "1", "b", "2"));
        //            N.println(multiMap2);
        //
        //            assertEquals(multiMap, multiMap2);
        //        }
        //
        //        {
        //            Multimap<Object, Object, Set<Object>> multiMap = N.asSetMultimap("a", "1", "b", "2");
        //            N.println(multiMap);
        //
        //            Multimap<String, String, Set<String>> multiMap2 = N.newSortedSetMultimap(N.asMap("a", "1", "b", "2"));
        //            N.println(multiMap2);
        //
        //            assertEquals(multiMap, multiMap2);
        //        }
    }

    //    @Test
    //    public void test_Multimap() throws Exception {
    //        Account account = super.createAccount(Account.class);
    //
    //        N.println(N.newListMultimap(10));
    //
    //        Multimap<Object, Object, List<Object>> multiMap = N.newListMultimap(account);
    //        N.println(multiMap);
    //    }
    //
    //    @Test
    //    public void test_asMapEntity() throws Exception {
    //        Account account = super.createAccount(Account.class);
    //        MapEntity mapEntity = N.asMapEntity(Account.__, account);
    //        N.println(mapEntity);
    //
    //        com.landawn.abacus.entity.pjo.basic.Account account2 = super.createAccount(com.landawn.abacus.entity.pjo.basic.Account.class);
    //        mapEntity = N.asMapEntity(Account.__, account2);
    //        N.println(mapEntity);
    //
    //        account = super.createAccount(Account.class);
    //        mapEntity = N.asMapEntity(Account.__, Beans.bean2Map(account));
    //        N.println(mapEntity);
    //
    //        MapEntity mapEntity2 = new MapEntity(Account.__, Beans.bean2Map(account));
    //        N.println(mapEntity2);
    //
    //        assertTrue(N.equals(mapEntity, mapEntity2));
    //
    //        try {
    //            N.asMapEntity(Account.__, "abc");
    //            fail("Should throw IllegalArgumentException");
    //        } catch (IllegalArgumentException e) {
    //
    //        }
    //
    //        try {
    //            N.asMapEntity(Account.__, "abc", 1, 2);
    //            fail("Should throw IllegalArgumentException");
    //        } catch (IllegalArgumentException e) {
    //
    //        }
    //    }

    @Test
    public void test_newMultimap() throws Exception {
        N.println(new BiMap<>());
        N.println(new BiMap<>(12));

        N.println(new Multiset<>(10));

        N.println(N.newListMultimap(12));
        N.println(N.newLinkedListMultimap(12));
        N.println(N.newSortedListMultimap());
        N.println(N.newSetMultimap(12));
        N.println(N.newLinkedSetMultimap(12));
        N.println(N.newSortedSetMultimap());
    }

    //    @Test
    //    public void test_parallelSort_byte() throws Exception {
    //        final int maxSize = 10000;
    //
    //        for (int c = 1; c < 17; c++) {
    //            changeCPUCoreNum(c);
    //
    //            for (int i = 0; i < 13; i++) {
    //                byte[] array = new byte[rand.nextInt(maxSize)];
    //
    //                for (int k = 0, len = array.length; k < len; k++) {
    //                    array[k] = (byte) (rand.nextInt() % Byte.MAX_VALUE);
    //                }
    //
    //                N.parallelSort(array);
    //
    //                for (int k = 1, len = array.length; k < len; k++) {
    //                    assertTrue(array[k] >= array[k - 1]);
    //                }
    //            }
    //        }
    //    }

    //    @Test
    //    public void test_parallelSort_short() throws Exception {
    //        final int maxSize = 10000;
    //
    //        for (int c = 1; c < 17; c++) {
    //            changeCPUCoreNum(c);
    //
    //            for (int i = 0; i < 13; i++) {
    //                short[] array = new short[rand.nextInt(maxSize)];
    //
    //                for (int k = 0, len = array.length; k < len; k++) {
    //                    array[k] = (short) (rand.nextInt() % Short.MAX_VALUE);
    //                }
    //
    //                N.parallelSort(array);
    //
    //                for (int k = 1, len = array.length; k < len; k++) {
    //                    assertTrue(array[k] >= array[k - 1]);
    //                }
    //            }
    //        }
    //
    //    }

    @Test
    public void test_parallelSort_int() throws Exception {
        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final int[] a = new int[rand.nextInt(maxSize)];

                for (int k = 0, len = a.length; k < len; k++) {
                    a[k] = rand.nextInt();
                }

                final int[] b = a.clone();
                N.parallelSort(a);
                Arrays.sort(b);

                for (int k = 0, len = a.length; k < len; k++) {
                    assertEquals(b[k], a[k]);
                }
            }
        }
    }

    @Test
    public void test_parallelSort_long() throws Exception {
        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final long[] a = new long[rand.nextInt(maxSize)];

                for (int k = 0, len = a.length; k < len; k++) {
                    a[k] = rand.nextLong();
                }

                final long[] b = a.clone();
                N.parallelSort(a);
                Arrays.sort(b);

                for (int k = 0, len = a.length; k < len; k++) {
                    assertEquals(b[k], a[k]);
                }
            }
        }
    }

    @Test
    public void test_parallelSort_float() throws Exception {
        assertFalse(1f > Float.NaN);
        assertFalse(1f < Float.NaN);
        assertFalse(1f == Float.NaN);
        assertFalse(1f <= Float.NaN);

        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final float[] a = new float[rand.nextInt(maxSize)];

                for (int k = 0, len = a.length; k < len; k++) {
                    a[k] = k % 3 == 0 ? Float.NaN : rand.nextFloat();
                }

                final float[] b = a.clone();
                N.parallelSort(a);
                Arrays.sort(b);

                //            for (int k = 0, len = a.length; k < len; k++) {
                //                assertEquals(b[k], a[k]);
                //            }
            }
        }
    }

    @Test
    public void test_parallelSort_double() throws Exception {
        assertFalse(1d > Double.NaN);
        assertFalse(1d < Double.NaN);
        assertFalse(1d == Double.NaN);
        assertFalse(1d <= Double.NaN);

        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final double[] a = new double[rand.nextInt(maxSize)];

                for (int k = 0, len = a.length; k < len; k++) {
                    a[k] = k % 3 == 0 ? Double.NaN : rand.nextFloat();
                }

                final double[] b = a.clone();
                // N.println(a);
                N.parallelSort(a);
                // N.println(a);
                Arrays.sort(b);
                // N.println(b);

                //            for (int k = 0, len = a.length; k < len; k++) {
                //                assertEquals(b[k], a[k]);
                //            }

                // TODO There is a bug in Arrays.parallelSort(double[])?
                // assertTrue(N.isSorted(a));
            }
        }
    }

    @Test
    public void test_parallelSort_String() throws Exception {
        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final String[] a = new String[rand.nextInt(maxSize)];

                for (int k = 0, len = a.length; k < len; k++) {
                    a[k] = String.valueOf(rand.nextInt());
                }

                final String[] b = a.clone();
                N.parallelSort(a);
                Arrays.sort(b);

                for (int k = 0, len = a.length; k < len; k++) {
                    assertEquals(b[k], a[k]);
                }
            }
        }
    }

    @Test
    public void test_parallelSort_List() throws Exception {
        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final int len = rand.nextInt(maxSize);
                final List<String> a = new ArrayList<>(len);

                for (int k = 0; k < len; k++) {
                    a.add(String.valueOf(rand.nextInt()));
                }

                final List<String> b = new ArrayList<>(a);

                N.parallelSort(a);

                for (int k = 1; k < len; k++) {
                    assertTrue(a.get(k).compareTo(a.get(k - 1)) >= 0);
                }

                N.parallelSort(a);
                N.sort(b);

                for (int k = 0, size = a.size(); k < size; k++) {
                    assertEquals(a.get(k), b.get(k));
                }
            }
        }
    }

    @Test
    public void test_parallelSort_List_2() throws Exception {
        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final int len = rand.nextInt(maxSize);
                final List<String> list = new java.util.ArrayList<>(len);

                for (int k = 0; k < len; k++) {
                    list.add(String.valueOf(rand.nextInt()));
                }

                N.parallelSort(list);

                for (int k = 1; k < len; k++) {
                    assertTrue(list.get(k).compareTo(list.get(k - 1)) >= 0);
                }
            }
        }
    }

    @Test
    public void test_parallelSort_List_3() throws Exception {
        final int maxSize = 10000;

        for (int c = 1; c < 17; c++) {
            changeCPUCoreNum(c);

            for (int i = 0; i < 13; i++) {
                final int len = rand.nextInt(maxSize);
                final List<String> list = new java.util.LinkedList<>();

                for (int k = 0; k < len; k++) {
                    list.add(String.valueOf(rand.nextInt()));
                }

                N.parallelSort(list);

                for (int k = 1; k < len; k++) {
                    assertTrue(list.get(k).compareTo(list.get(k - 1)) >= 0);
                }
            }
        }
    }

    @Test
    public void test_commonPrefix() {
        String commPrefix = Strings.commonPrefix(null, null);
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix(null, "");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("", null);
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("", "");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aaa", "");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("", "aaa");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aaa", "bbb");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aaa", "aabbb");
        assertEquals("aa", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aaa", "aaa");
        assertEquals("aaa", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aa", "aa");
        assertEquals("aa", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("a", "a");
        assertEquals("a", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aaa", "aabbb", "ccc");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aaa", "aabbb", "aaaccc");
        assertEquals("aa", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aaa", "aaa", "aaa");
        assertEquals("aaa", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("aa", "aa", "aa");
        assertEquals("aa", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("a", "a", "a");
        assertEquals("a", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("", "a", "a");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("", "", "");
        assertEquals("", commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("", null, "");
        assertEquals("", commPrefix);
        N.println(commPrefix);
    }

    @Test
    public void test_commonSuffix() {
        String commSuffix = Strings.commonSuffix(null, null);
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix(null, null, null, null);
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix(null, "");
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("", null);
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("", null, "", null);
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("", "");
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("", "", "", "");
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("aaa", "");
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("", "aaa");
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("aaa", "bbb");
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("aaa", "aabbb");
        assertEquals("", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("aaabb", "aabbb");
        assertEquals("bb", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("aaabb", "aabbb", "aaabb", "aabbb");
        assertEquals("bb", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("bb", "aabbb");
        assertEquals("bb", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("b", "bbb");
        assertEquals("b", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("bbb", "bbb");
        assertEquals("bbb", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("bb", "bb");
        assertEquals("bb", commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("b", "b");
        assertEquals("b", commSuffix);
        N.println(commSuffix);
    }

    @Test
    public void test_indexOfDifference() {
        final String[] strs = null;
        assertTrue(Strings.indexOfDifference(strs) == -1);
        assertTrue(Strings.indexOfDifference() == -1);
        assertTrue(Strings.indexOfDifference("abc") == -1);
        assertTrue(Strings.indexOfDifference(new String[] { null, null }) == -1);
        assertTrue(Strings.indexOfDifference(new String[] { "", "" }) == -1);
        assertTrue(Strings.indexOfDifference(new String[] { "", null }) == -1);
        assertTrue(Strings.indexOfDifference("abc", null, null) == 0);
        assertTrue(Strings.indexOfDifference(null, null, "abc") == 0);
        assertTrue(Strings.indexOfDifference(new String[] { "", "abc" }) == 0);
        assertTrue(Strings.indexOfDifference(new String[] { "abc", "" }) == 0);
        assertTrue(Strings.indexOfDifference(new String[] { "abc", "abc" }) == -1);
        assertTrue(Strings.indexOfDifference(new String[] { "abc", "a" }) == 1);
        assertTrue(Strings.indexOfDifference(new String[] { "ab", "abxyz" }) == 2);
        assertTrue(Strings.indexOfDifference(new String[] { "abcde", "abxyz" }) == 2);
        assertTrue(Strings.indexOfDifference(new String[] { "abcde", "xyz" }) == 0);
        assertTrue(Strings.indexOfDifference(new String[] { "xyz", "abcde" }) == 0);
        assertTrue(Strings.indexOfDifference(new String[] { "i am a machine", "i am a robot" }) == 7);
    }

    //    @Test
    //    public void test_difference() {
    //        N.println(N.differenceOf(null, null));
    //
    //        String diff = N.differenceOf("abc", "123");
    //        N.println(diff);
    //        assertEquals(diff, "123");
    //
    //        diff = N.differenceOf("abc", "");
    //        N.println(diff);
    //        assertEquals(diff, "");
    //
    //        diff = N.differenceOf("abc", null);
    //        N.println(diff);
    //        assertEquals(diff, "");
    //
    //        diff = N.differenceOf("", null);
    //        N.println(diff);
    //        assertEquals(diff, "");
    //
    //        diff = N.differenceOf("", "");
    //        N.println(diff);
    //        assertEquals(diff, "");
    //
    //        diff = N.differenceOf("", "123");
    //        N.println(diff);
    //        assertEquals(diff, "123");
    //
    //        diff = N.differenceOf(null, "123");
    //        N.println(diff);
    //        assertEquals(diff, "123");
    //    }

    @Test
    public void test_ordinaryIndex() {

        String str = "aaaaaaaaaa";

        String substr = "a";
        int index = Strings.ordinalIndexOf(str, substr, 3);
        String tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "aa--aaaaaaaa");

        substr = "aa";
        index = Strings.ordinalIndexOf(str, substr, 3);
        tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "aaaa--aaaaaa");

        substr = "aaa";
        index = Strings.ordinalIndexOf(str, substr, 3);
        tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "aaaaaa--aaaa");

        substr = str;
        index = Strings.ordinalIndexOf(str, substr, 1);
        tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "--aaaaaaaaaa");

        str = "aaaaaaa";
        assertEquals(2, Strings.ordinalIndexOf(str, "aa", 2));

        str = "aaaaaaa";
        assertEquals(3, Strings.lastOrdinalIndexOf(str, "aa", 2));

        // assertEquals(2, StringUtils.ordinalIndexOf("aaaaaaa", "aa", 2));
    }

    @Test
    public void test_lastOrdinaryIndex() {

        final String str = "aaaaaaaaaa";

        String substr = "a";
        int index = Strings.lastOrdinalIndexOf(str, substr, 3);
        String tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "aaaaaaa--aaa");

        substr = "aa";
        index = Strings.lastOrdinalIndexOf(str, substr, 3);
        tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "aaaa--aaaaaa");

        substr = "aaa";
        index = Strings.lastOrdinalIndexOf(str, substr, 3);
        tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "a--aaaaaaaaa");

        substr = "aaa";
        index = Strings.lastOrdinalIndexOf(str, substr, 1);
        tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "aaaaaaa--aaa");

        substr = str;
        index = Strings.lastOrdinalIndexOf(str, substr, 1);
        tmp = str.substring(0, index) + "--" + str.substring(index);
        N.println(tmp);
        assertEquals(tmp, "--aaaaaaaaaa");

    }

    protected void changeCPUCoreNum(final int c) {
        // Array.CPU_CORES = c;
    }
}
