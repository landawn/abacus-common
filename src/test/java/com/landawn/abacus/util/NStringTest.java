package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.Splitter.MapSplitter;
import com.landawn.abacus.util.Strings.StrUtil;

public class NStringTest extends AbstractParserTest {

    private static final Random rand = new Random();

    protected void changeCPUCoreNum(final int c) {
    }

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

        final String[] b = N.copyThenSetAll(CommonUtil.asArray("a", "b"), (i, s) -> Strings.strip(s));
        N.println(b);

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
        assertEquals(Strings.center("a", 4, "yz"), "yayz");
        assertEquals("    ", Strings.center(null, 4, " "));
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
        CommonUtil.rotate(chars, 0);
        assertEquals(String.valueOf(chars), "abcdefg");

        chars = "abcdefg".toCharArray();
        CommonUtil.rotate(chars, 2);
        assertEquals(String.valueOf(chars), "fgabcde");

        chars = "abcdefg".toCharArray();
        CommonUtil.rotate(chars, -2);
        assertEquals(String.valueOf(chars), "cdefgab");

        chars = "abcdefg".toCharArray();
        CommonUtil.rotate(chars, 7);
        assertEquals(String.valueOf(chars), "abcdefg");

        chars = "abcdefg".toCharArray();
        CommonUtil.rotate(chars, -7);
        assertEquals(String.valueOf(chars), "abcdefg");

        chars = "abcdefg".toCharArray();
        CommonUtil.rotate(chars, 9);
        assertEquals(String.valueOf(chars), "fgabcde");

        chars = "abcdefg".toCharArray();
        CommonUtil.rotate(chars, -9);
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
            N.removeAt(listA, indices);

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
        final Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        final String str = Joiner.withDefault().appendEntries(map).toString();
        N.println(str);

        final Map<String, Integer> map2 = MapSplitter.withDefault().split(str, String.class, Integer.class);

        assertEquals(map, map2);
    }

    @Test
    public void test_joiner() {
        assertDoesNotThrow(() -> {
            N.println(Joiner.withDefault().repeat(2, 10).toString());
            N.println(Joiner.withDefault().append(1).repeat(2, 10).toString());

            N.println(Joiner.withDefault().repeat(null, 10).toString());
            N.println(Joiner.withDefault().append(1).repeat(null, 10).toString());
        });
    }

    @Test
    public void test_12() {
        assertEquals(1, CommonUtil.compare(Array.of(1, 2, 3), Array.of(1, 2, 2)));
        assertEquals(-1, CommonUtil.compare(Array.of(1, 2), Array.of(1, 2, 0)));
    }

    @Test
    public void test_11() {
        assertTrue(CommonUtil.equals(Array.of('a', 'b', 'c', '1', '2', '3'), N.concat(Array.of('a'), Array.of('b', 'c'), Array.of('1', '2', '3'))));

        assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c", "1", "2", "3"),
                N.concat(CommonUtil.asArray("a"), CommonUtil.asArray("b", "c"), CommonUtil.asArray("1", "2", "3"))));
    }

    @Test
    public void test_10() {
        final Map<Map<Object, Object>, Map<Object, Object>> map = CommonUtil.asMap(CommonUtil.asMap("abc", 123D), CommonUtil.asMap(123D, "abc"));
        String json = N.toJson(map);
        N.println(json);
        assertEquals(map, N.fromJson(json,
                JsonDeserConfig.create().setMapKeyType(CommonUtil.typeOf("Map<String, Double>")).setMapValueType(CommonUtil.typeOf("Map<Double, String>")),
                Map.class));

        final List<Map<String, Double>> list = CommonUtil.toList(CommonUtil.asMap("abc", 123D));
        json = N.toJson(list);
        N.println(json);
        assertTrue(CommonUtil.equals(list, N.fromJson(json, JsonDeserConfig.create().setElementType(CommonUtil.typeOf("Map<String, Double>")), List.class)));
        assertFalse(CommonUtil.equals(list, N.fromJson(json, JsonDeserConfig.create().setElementType(CommonUtil.typeOf("Map<String, Float>")), List.class)));
    }

    @Test
    public void test_parse_time() throws Exception {
        final SimpleDateFormat sdf = new SimpleDateFormat(Dates.ISO_8601_TIMESTAMP_FORMAT);

        String date = "2016-04-11T00:00:04.370Z";
        N.println(Dates.parseToTimestamp(date).getTime());
        N.println(Dates.parseToTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        assertEquals(1460332804370L, Dates.parseToTimestamp(date).getTime());
        assertEquals(1460332804370L, Dates.parseToTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        N.println(sdf.parse(date).getTime());

        date = "2016-04-11T00:00:04.385Z";
        N.println(Dates.parseToTimestamp(date).getTime());
        N.println(Dates.parseToTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        assertEquals(1460332804385L, Dates.parseToTimestamp(date).getTime());
        assertEquals(1460332804385L, Dates.parseToTimestamp(date, Dates.ISO_8601_TIMESTAMP_FORMAT).getTime());
        N.println(sdf.parse(date).getTime());

    }

    @Test
    public void test_Mutable() {
        {
            final String str = CommonUtil.stringOf(MutableBoolean.of(true));
            assertEquals(MutableBoolean.of(true), CommonUtil.valueOf(str, MutableBoolean.class));
        }

        {
            final String str = CommonUtil.stringOf(MutableChar.of('c'));
            assertEquals(MutableChar.of('c'), CommonUtil.valueOf(str, MutableChar.class));
        }

        {
            final String str = CommonUtil.stringOf(MutableByte.of((byte) 1));
            assertEquals(MutableByte.of((byte) 1), CommonUtil.valueOf(str, MutableByte.class));
        }

        {
            final String str = CommonUtil.stringOf(MutableShort.of((short) 1));
            assertEquals(MutableShort.of((short) 1), CommonUtil.valueOf(str, MutableShort.class));
        }

        {
            final String str = CommonUtil.stringOf(MutableInt.of(1));
            assertEquals(MutableInt.of(1), CommonUtil.valueOf(str, MutableInt.class));
        }

        {
            final String str = CommonUtil.stringOf(MutableLong.of(1));
            assertEquals(MutableLong.of(1), CommonUtil.valueOf(str, MutableLong.class));
        }

        {
            final String str = CommonUtil.stringOf(MutableFloat.of(1));
            assertEquals(MutableFloat.of(1), CommonUtil.valueOf(str, MutableFloat.class));
        }

        {
            final String str = CommonUtil.stringOf(MutableDouble.of(1));
            assertEquals(MutableDouble.of(1), CommonUtil.valueOf(str, MutableDouble.class));
        }
    }

    @Test
    public void test_range() {
        final Range<Integer> range = Range.closed(1, 6);
        String str = CommonUtil.stringOf(range);
        assertEquals(range, CommonUtil.typeOf("Range<Integer>").valueOf(str));

        final MyEntity_1 myBean = new MyEntity_1();
        myBean.setRange(Range.closed(1.0f, 4.0f));

        str = CommonUtil.stringOf(myBean);

        assertEquals(myBean, CommonUtil.typeOf(MyEntity_1.class).valueOf(str));

        assertTrue(Range.just(1).overlaps(Range.closed(1, 2)));
    }

    @Test
    public void test_top() {
        {
            final int[] a = { 1, 5, 3, 7, 9, 2 };
            final int[] b = N.top(a, 3);
            assertTrue(CommonUtil.equals(new int[] { 5, 7, 9 }, b));
        }

        {
            final long[] a = { 1, 5, 3, 7, 9, 2 };
            final long[] b = N.top(a, 3);
            assertTrue(CommonUtil.equals(new long[] { 5, 7, 9 }, b));
        }

        {
            final float[] a = { 1, 5, 3, 7, 9, 2 };
            final float[] b = N.top(a, 3);
            assertTrue(CommonUtil.equals(new float[] { 5, 7, 9 }, b));
        }

        {
            final double[] a = { 1, 5, 3, 7, 9, 2 };
            final double[] b = N.top(a, 3);
            assertTrue(CommonUtil.equals(new double[] { 5, 7, 9 }, b));
        }

        {
            final String[] a = { "1", "5", "3", "7", "9", "2" };
            final List<String> b = N.top(a, 3);
            assertTrue(CommonUtil.equals(CommonUtil.toList("5", "7", "9"), b));
        }

        {
            final Set<String> c = CommonUtil.toLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3);
            assertTrue(CommonUtil.equals(CommonUtil.toList("5", "7", "9"), b));
        }

        {
            final Set<String> c = CommonUtil.toLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.nullsFirst());
            assertTrue(CommonUtil.equals(CommonUtil.toList("5", "7", "9"), b));
        }

        {
            final Set<String> c = CommonUtil.toLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.nullsLast());
            assertTrue(CommonUtil.equals(CommonUtil.toList("5", "7", "9"), b));
        }

        {
            final Set<String> c = CommonUtil.toLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.reverseOrder());
            N.println(b);
            assertTrue(CommonUtil.equals(CommonUtil.toList("3", "1", "2"), b));
        }

        {
            final Set<String> c = CommonUtil.toLinkedHashSet("1", "5", "3", "7", "9", "2");
            final List<String> b = N.top(c, 3, Comparators.reverseOrder(), true);
            N.println(b);
            assertTrue(CommonUtil.equals(CommonUtil.toList("1", "3", "2"), b));
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
            assertEquals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.nullToEmpty(a));

            a = new boolean[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            char[] a = null;
            assertEquals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.nullToEmpty(a));

            a = new char[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            byte[] a = null;
            assertEquals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.nullToEmpty(a));

            a = new byte[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            short[] a = null;
            assertEquals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.nullToEmpty(a));

            a = new short[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            int[] a = null;
            assertEquals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.nullToEmpty(a));

            a = new int[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            long[] a = null;
            assertEquals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.nullToEmpty(a));

            a = new long[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            float[] a = null;
            assertEquals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.nullToEmpty(a));

            a = new float[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            double[] a = null;
            assertEquals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.nullToEmpty(a));

            a = new double[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            String[] a = null;
            assertEquals(CommonUtil.EMPTY_STRING_ARRAY, CommonUtil.nullToEmpty(a));

            a = new String[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            Object[] a = null;
            assertEquals(CommonUtil.EMPTY_OBJECT_ARRAY, CommonUtil.nullToEmpty(a));

            a = new Object[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            String[] a = null;
            assertTrue(CommonUtil.equals(new String[0], CommonUtil.nullToEmpty(a, String[].class)));

            a = new String[3];
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            List<String> a = null;
            assertEquals(CommonUtil.EMPTY_LIST, CommonUtil.nullToEmpty(a));

            a = CommonUtil.toList("1", "2");
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            Set<String> a = null;
            assertEquals(CommonUtil.EMPTY_SET, CommonUtil.nullToEmpty(a));

            a = CommonUtil.toSet("1", "2");
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }

        {
            Map<String, String> a = null;
            assertEquals(CommonUtil.EMPTY_MAP, CommonUtil.nullToEmpty(a));

            a = CommonUtil.asMap("1", "2");
            assertEquals(a, CommonUtil.nullToEmpty(a));
        }
    }

    @Test
    public void test_listOf() {
        {
            final boolean[] a = { true, false };
            assertTrue(a == BooleanList.of(a).internalArray());
        }

        {
            final char[] a = { 'a', '1' };
            assertTrue(a == CharList.of(a).internalArray());
        }

        {
            final byte[] a = { 1, 2 };
            assertTrue(a == ByteList.of(a).internalArray());
        }

        {
            final short[] a = { 1, 2 };
            assertTrue(a == ShortList.of(a).internalArray());
        }

        {
            final int[] a = { 1, 2 };
            assertTrue(a == IntList.of(a).internalArray());
        }

        {
            final long[] a = { 1, 2 };
            assertTrue(a == LongList.of(a).internalArray());
        }

        {
            final float[] a = { 1, 2 };
            assertTrue(a == FloatList.of(a).internalArray());
        }

        {
            final double[] a = { 1, 2 };
            assertTrue(a == DoubleList.of(a).internalArray());
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
        assertEquals("a", Beans.normalizePropName("_a"));
        assertEquals("a", Beans.normalizePropName("_A"));
        assertEquals("a", Beans.normalizePropName("_a_"));
        assertEquals("a", Beans.normalizePropName("_A_"));
        assertEquals("aB", Beans.normalizePropName("_a_b"));
        assertEquals("aB", Beans.normalizePropName("_a__b"));
        assertEquals("aB", Beans.normalizePropName("_a_B"));
        assertEquals("aB", Beans.normalizePropName("_A_b"));
        assertEquals("aB", Beans.normalizePropName("_A_B"));
        assertEquals("aB", Beans.normalizePropName("_a_b_"));
        assertEquals("aB", Beans.normalizePropName("_a_B_"));
        assertEquals("aB", Beans.normalizePropName("_A_b_"));
        assertEquals("aB", Beans.normalizePropName("_A_B_"));
        assertEquals("aBCd", Beans.normalizePropName("_a_b_cd"));
        assertEquals("aBCd", Beans.normalizePropName("_a_B_cd"));
        assertEquals("aBCd", Beans.normalizePropName("_A_b_cd"));
        assertEquals("aBCd", Beans.normalizePropName("_A_B_cd"));
        assertEquals("aBbCd", Beans.normalizePropName("_A_bb_cd"));
        assertEquals("aBbCd", Beans.normalizePropName("_A_Bb_cd"));
        assertEquals("firstName", Beans.normalizePropName("_FIRST_NAME_"));

        assertEquals("a_bb_cd", Beans.toSnakeCase("aBBCd"));
        assertEquals("a_b_cd", Beans.toSnakeCase("aBCd"));
        assertEquals("a_b", Beans.toSnakeCase("aB"));
        assertEquals("A_BB_CD", Beans.toScreamingSnakeCase("aBBCd"));
        assertEquals("A_B_CD", Beans.toScreamingSnakeCase("aBCd"));
        assertEquals("A_B", Beans.toScreamingSnakeCase("aB"));

        assertEquals("abc123efg456", Beans.toSnakeCase("abc123EFG456"));
        assertEquals("ABC123EFG456A_AA", Beans.toScreamingSnakeCase("abc123EFG456aAa"));
    }

    @Test
    public void test_indexOfArray() {

        final String nullStr = null;

        {
            assertEquals(0, StrUtil.indexOfToken("a, b, c", "a", ","));
            assertEquals(3, StrUtil.indexOfToken("a, b, c", "b", ", "));
            assertEquals(6, StrUtil.indexOfToken("a, b, c", "c", ", "));
            assertEquals(-1, StrUtil.indexOfToken("a,  b, c", "d", ","));

            final boolean[] b = { true, true };
            assertEquals(0, CommonUtil.indexOf(b, true));
            assertEquals(-1, CommonUtil.indexOf(b, false));

            final char[] c = { '1', '3', '2' };
            assertEquals(1, CommonUtil.indexOf(c, '3'));
            assertEquals(-1, CommonUtil.indexOf(c, '4'));

            final byte[] bt = { 1, 3, 2 };
            assertEquals(1, CommonUtil.indexOf(bt, (byte) 3));
            assertEquals(-1, CommonUtil.indexOf(bt, (byte) 4));

            final short[] s = { 1, 3, 2 };
            assertEquals(1, CommonUtil.indexOf(s, (short) 3));
            assertEquals(-1, CommonUtil.indexOf(s, (short) 4));

            final int[] i = { 1, 3, 2 };
            assertEquals(1, CommonUtil.indexOf(i, 3));
            assertEquals(-1, CommonUtil.indexOf(i, 4));

            final long[] l = { 1, 3, 2 };
            assertEquals(1, CommonUtil.indexOf(l, 3));
            assertEquals(-1, CommonUtil.indexOf(l, 4));

            final float[] f = { 1, 3, 2 };
            assertEquals(1, CommonUtil.indexOf(f, 3f));
            assertEquals(-1, CommonUtil.indexOf(f, 4f));

            final double[] d = { 1, 3, 2 };
            assertEquals(1, CommonUtil.indexOf(d, 3));
            assertEquals(-1, CommonUtil.indexOf(d, 4));

            final Integer[] st = { 1, 3, 2 };
            assertEquals(1, CommonUtil.indexOf(st, 3));
            assertEquals(-1, CommonUtil.indexOf(st, 4));

            final List<Integer> list = CommonUtil.toList(1, 3, 2);
            assertEquals(1, CommonUtil.indexOf(list, 3));
            assertEquals(-1, CommonUtil.indexOf(list, 4));
        }

        {
            assertEquals(0, StrUtil.lastIndexOfToken("a,  b, c", "a", ","));
            assertEquals(3, StrUtil.lastIndexOfToken("a, b, c", "b", ", "));
            assertEquals(10, StrUtil.lastIndexOfToken("a,  b, c, b", "b", ", "));
            assertEquals(-1, StrUtil.lastIndexOfToken("a,  b, c", "d", ","));

            final boolean[] b = { true, true };
            assertEquals(1, CommonUtil.lastIndexOf(b, true));
            assertEquals(-1, CommonUtil.lastIndexOf(b, false));

            final char[] c = { '1', '3', '2' };
            assertEquals(1, CommonUtil.lastIndexOf(c, '3'));
            assertEquals(-1, CommonUtil.lastIndexOf(c, '4'));

            final byte[] bt = { 1, 3, 2 };
            assertEquals(1, CommonUtil.lastIndexOf(bt, (byte) 3));
            assertEquals(-1, CommonUtil.lastIndexOf(bt, (byte) 4));

            final short[] s = { 1, 3, 2 };
            assertEquals(1, CommonUtil.lastIndexOf(s, (short) 3));
            assertEquals(-1, CommonUtil.lastIndexOf(s, (short) 4));

            final int[] i = { 1, 3, 2 };
            assertEquals(1, CommonUtil.lastIndexOf(i, 3));
            assertEquals(-1, CommonUtil.lastIndexOf(i, 4));

            final long[] l = { 1, 3, 2 };
            assertEquals(1, CommonUtil.lastIndexOf(l, 3));
            assertEquals(-1, CommonUtil.lastIndexOf(l, 4));

            final float[] f = { 1, 3, 2 };
            assertEquals(1, CommonUtil.lastIndexOf(f, 3f));
            assertEquals(-1, CommonUtil.lastIndexOf(f, 4f));

            final double[] d = { 1, 3, 2 };
            assertEquals(1, CommonUtil.lastIndexOf(d, 3));
            assertEquals(-1, CommonUtil.lastIndexOf(d, 4));

            final Integer[] st = { 1, 3, 2 };
            assertEquals(1, CommonUtil.lastIndexOf(st, 3));
            assertEquals(-1, CommonUtil.lastIndexOf(st, 4));

            final List<Integer> list = CommonUtil.toList(1, 3, 2);
            assertEquals(1, CommonUtil.lastIndexOf(list, 3));
            assertEquals(-1, CommonUtil.lastIndexOf(list, 4));
        }

        {

            assertFalse(StrUtil.containsToken(nullStr, "a", ","));
            assertFalse(StrUtil.containsToken("", "b", ","));
            assertTrue(StrUtil.containsToken("a,  b, c", "c", ", "));
            assertFalse(StrUtil.containsToken("a,  b, c", "d", ","));

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

            final List<Integer> list = CommonUtil.toList(1, 3, 2);
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

            assertFalse(Strings.containsOnly(nullStr, 'a'));
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
        final Map<Percentage, Integer> percentiles = N.percentilesOfSorted(a);
        N.println(percentiles);
        percentiles.forEach(Fn.println("="));

        percentiles.forEach((k, v) -> N.println("     *                            " + k + "=" + v));
        assertNotNull(percentiles);
    }

    @Test
    public void test_sort_2() {
        String[] st1 = { "a", "c", "b" };
        CommonUtil.sort(st1);
        N.println(st1);
        assertEquals(st1[1], "b");

        st1 = new String[] { "a", "c", null, "b" };
        CommonUtil.sort(st1);
        N.println(st1);
        assertEquals(st1[1], "a");
    }

    @Test
    public void test_sort() {
        final char[] c = { '1', '3', '2' };
        CommonUtil.sort(c);
        N.println(c);
        assertEquals(c[1], '2');

        final byte[] bt = { 1, 3, 2 };
        CommonUtil.sort(bt);
        N.println(bt);
        assertEquals(bt[1], 2);

        final short[] s = { 1, 3, 2 };
        CommonUtil.sort(s);
        N.println(s);
        assertEquals(s[1], 2);

        final int[] i = { 1, 3, 2 };
        CommonUtil.sort(i);
        N.println(i);
        assertEquals(i[1], 2);

        final long[] l = { 1, 3, 2 };
        CommonUtil.sort(l);
        N.println(l);
        assertEquals(l[1], 2);

        final float[] f = { 1, 3, 2 };
        CommonUtil.sort(f);
        N.println(f);
        assertEquals(f[1], 2f);

        final double[] d = { 1, 3, 2 };
        CommonUtil.sort(d);
        N.println(d);
        assertEquals(d[1], 2d);

        final Integer[] st = { 1, 3, 2 };
        CommonUtil.sort(st);
        N.println(st);
        assertEquals(st[1].intValue(), 2);

        String[] st1 = { "a", "c", "b" };
        CommonUtil.sort(st1);
        N.println(st1);
        assertEquals(st1[1], "b");

        st1 = new String[] { "a", "c", "b" };
        CommonUtil.sort(st1, (Comparator<String>) (o1, o2) -> o2.compareTo(o1));
        N.println(st1);
        assertEquals(st1[1], "b");

        List<String> list = CommonUtil.toList("a", "c", "b");
        CommonUtil.sort(list);
        N.println(list);
        assertEquals(list.get(1), "b");

        list = CommonUtil.toList("a", "c", "b");
        CommonUtil.sort(list, (Comparator<String>) (o1, o2) -> o2.compareTo(o1));
        N.println(list);
        assertEquals(list.get(1), "b");

        list = new java.util.ArrayList<>();
        list.add("a");
        list.add("c");
        list.add("b");
        CommonUtil.sort(list, (Comparator<String>) (o1, o2) -> o2.compareTo(o1));
        N.println(list);
        assertEquals(list.get(1), "b");
    }

    @Test
    public void test_fill() {
        final boolean[] b = new boolean[2];
        CommonUtil.fill(b, true);
        N.println(b);
        assertEquals(b[0], true);
        CommonUtil.fill(b, 1, 2, true);

        final char[] c = new char[2];
        CommonUtil.fill(c, 'a');
        N.println(c);
        assertEquals(c[0], 'a');
        CommonUtil.fill(c, 1, 2, 'a');

        final byte[] bt = new byte[2];
        CommonUtil.fill(bt, (byte) 12);
        N.println(bt);
        assertEquals(bt[0], 12);
        CommonUtil.fill(bt, 1, 2, (byte) 12);

        final short[] s = new short[2];
        CommonUtil.fill(s, (short) 12);
        N.println(s);
        assertEquals(s[0], 12);
        CommonUtil.fill(s, 1, 2, (short) 12);

        final int[] i = new int[2];
        CommonUtil.fill(i, 12);
        N.println(i);
        assertEquals(i[0], 12);
        CommonUtil.fill(i, 1, 2, 12);

        final long[] l = new long[2];
        CommonUtil.fill(l, 12);
        N.println(l);
        assertEquals(l[0], 12);
        CommonUtil.fill(l, 1, 2, 12);

        final float[] f = new float[2];
        CommonUtil.fill(f, 1.2f);
        N.println(f);
        assertEquals(f[0], 1.2f);
        CommonUtil.fill(f, 1, 2, 1.2f);

        final double[] d = new double[2];
        CommonUtil.fill(d, 1.2);
        N.println(d);
        assertEquals(d[0], 1.2);
        CommonUtil.fill(d, 1, 2, 1.2);

        final String[] st = new String[2];
        CommonUtil.fill(st, "a");
        N.println(st);
        assertEquals(st[0], "a");
        CommonUtil.fill(st, 1, 2, "a");

        List<String> list = CommonUtil.toList("b", "b", "b");
        CommonUtil.fill(list, "a");
        N.println(list);
        assertEquals(list.get(0), "a");
        CommonUtil.fill(list, 1, 2, "a");

        for (int k = 5; k <= 1001; k++) {
            list = CommonUtil.toLinkedList();
            CommonUtil.fill(list, 0, k, null);
            CommonUtil.fill(list, 3, k - 2, "abc");
            N.println(list);

            for (int j = 3; j < k - 2; j++) {
                assertEquals("abc", list.get(j));
            }
        }
    }

    @Test
    public void test_is() {
        {
            assertFalse(Strings.isAllLowerCase(null));
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

            assertFalse(Strings.isAllUpperCase(null));
            assertTrue(Strings.isAllUpperCase(""));
            assertTrue(Strings.isAllUpperCase(new StringBuilder("")));
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
            assertFalse(Strings.isAsciiPrintable(""));
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
            assertFalse(Strings.isAsciiAlphaSpace(""));
            assertTrue(Strings.isAsciiAlphaSpace("abc "));
            assertTrue(Strings.isAsciiAlphaSpace(new StringBuilder("abc ")));
            assertFalse(Strings.isAsciiAlphaSpace("abc\n\123 "));
            assertFalse(Strings.isAsciiAlphaSpace(new StringBuilder("abc\n\123 ")));

            assertFalse(Strings.isAsciiAlphanumericSpace(null));
            assertFalse(Strings.isAsciiAlphanumericSpace(""));
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
            assertFalse(Strings.isAlphaSpace(""));
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
            assertFalse(Strings.isAlphanumericSpace(""));
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
            assertFalse(Strings.isNumericSpace(""));
            assertTrue(Strings.isNumericSpace("12 3"));
            assertTrue(Strings.isNumericSpace(new StringBuilder("12 3")));
            assertFalse(Strings.isNumericSpace("abc\n\123 "));
            assertFalse(Strings.isNumericSpace(new StringBuilder("abc\n\123 ")));

            assertFalse(Strings.isWhitespace(null));
            assertFalse(Strings.isWhitespace(""));
            assertTrue(Strings.isWhitespace(" \n \r "));
            assertFalse(Strings.isWhitespace(" \\n \\r "));
            assertTrue(Strings.isWhitespace(new StringBuilder(" \n \r ")));
            assertFalse(Strings.isWhitespace(new StringBuilder(" \\n \\r ")));
            assertFalse(Strings.isWhitespace("abc\n\123 "));
            assertFalse(Strings.isWhitespace(new StringBuilder("abc\n\123 ")));

            assertTrue(Numbers.isCreatable("0.0"));
            assertTrue(Numbers.isCreatable("0.4790"));

            assertTrue(Numbers.isCreatable("123"));
            assertTrue(Numbers.isCreatable("0X123"));
            assertTrue(Numbers.isCreatable("123f"));
            assertTrue(Numbers.isCreatable("0X123f"));
            assertFalse(Numbers.isCreatable("123g"));
            assertFalse(Numbers.isCreatable("0X123g"));

            assertTrue(Numbers.isCreatable("-123"));
            assertTrue(Numbers.isCreatable("-0X123"));
            assertTrue(Numbers.isCreatable("-123f"));
            assertTrue(Numbers.isCreatable("-0X123f"));
            assertFalse(Numbers.isCreatable("-123g"));
            assertFalse(Numbers.isCreatable("-0X123g"));

            assertTrue(Numbers.isParsable("123"));
            assertTrue(Numbers.isParsable("-123"));
            assertTrue(Numbers.isParsable("-123.00"));
            assertFalse(Numbers.isParsable("-"));
            assertFalse(Numbers.isParsable("."));

            assertTrue(Strings.isAsciiInteger("123"));
            assertTrue(Strings.isAsciiInteger("-123"));
            assertFalse(Strings.isAsciiInteger("-123.00"));
            assertFalse(Strings.isAsciiInteger("-"));
            assertFalse(Strings.isAsciiInteger("."));
        }
    }

    @Test
    public void test_chomp_chop() {
        {
            assertEquals(null, Strings.chomp(null));
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
            assertEquals(null, Strings.chop(null));
            assertEquals("", Strings.chop(""));
            assertEquals("", Strings.chop("\r"));
            assertEquals("", Strings.chop("\n"));
            assertEquals("", Strings.chop("\r\n"));
            assertEquals("abc ", Strings.chop("abc \r"));
            assertEquals("ab", Strings.chop("abc"));
        }

        {
            assertEquals(null, Strings.removeWhitespace(null));
            assertEquals("", Strings.removeWhitespace(""));
            assertEquals("abc", Strings.removeWhitespace("abc \r"));
            assertEquals("abcabc", Strings.removeWhitespace("abc\n\rabc"));
        }

    }

    @Test
    public void test_trim_strip() {
        {
            assertEquals(null, Strings.trim(null));
            assertEquals("", Strings.trim(""));

            assertEquals("aa", Strings.trim("aa"));
            assertEquals("aa", Strings.trim("  aa"));
            assertEquals("aa", Strings.trim("  aa  "));
            assertEquals("a aa a", Strings.trim(" a aa a "));

            assertEquals(null, Strings.trimToNull(null));
            assertEquals(null, Strings.trimToNull(""));

            assertEquals("", Strings.trimToEmpty(null));
            assertEquals("", Strings.trimToEmpty(""));
        }

        {
            assertEquals(null, Strings.strip(null));
            assertEquals("", Strings.strip(""));

            assertEquals("aa", Strings.strip("aa"));
            assertEquals("aa", Strings.strip("  aa"));
            assertEquals("aa", Strings.strip("  aa  "));
            assertEquals("a aa a", Strings.strip(" a aa a "));

            assertEquals(null, Strings.stripToNull(null));
            assertEquals(null, Strings.stripToNull(""));

            assertEquals("", Strings.stripToEmpty(null));
            assertEquals("", Strings.stripToEmpty(""));
        }

        {

            assertEquals(Strings.stripEnd(null, "*"), null);
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

            assertEquals(Strings.stripStart(null, "*"), null);
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
            assertEquals(Strings.stripStart(null, "*"), null);
            String[] a = CommonUtil.EMPTY_STRING_ARRAY;
            Strings.stripEach(a, "*");
            assertTrue(CommonUtil.equals(a, CommonUtil.EMPTY_STRING_ARRAY));

            a = CommonUtil.asArray("abc", "  abc");
            Strings.stripEach(a, null);
            assertTrue(CommonUtil.equals(a, CommonUtil.asArray("abc", "abc")));

            a = CommonUtil.asArray("abc  ", null);
            Strings.stripEach(a, null);
            assertTrue(CommonUtil.equals(a, CommonUtil.asArray("abc", null)));

            a = CommonUtil.asArray("yabcz", null);
            Strings.stripEach(a, "yz");
            assertTrue(CommonUtil.equals(a, CommonUtil.asArray("abc", null)));
        }

        {

            assertEquals(null, Strings.stripAccents(null));
            assertEquals("", Strings.stripAccents(""));
            assertEquals("control", Strings.stripAccents("control"));
            assertEquals("eclair", Strings.stripAccents("éclair"));
            assertEquals("한", Strings.stripAccents("한"));
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

        N.println(Strings.escapeQuotes("abc'\"def\\'\\\""));
        assertEquals("abc\\'\\\"def\\'\\\"", Strings.escapeQuotes("abc'\"def\\'\\\""));

        {
            final char[] chs = { 0x9, 0x10, 0x99, 0x100, 0x101, 0x1000, 0x9000 };
            final String[] strs = { "\\u0009", "\\u0010", "\\u0099", "\\u0100", "\\u0101", "\\u1000", "\\u9000" };
            for (int i = 0; i < chs.length; i++) {
                assertEquals(strs[i], Strings.toUnicodeEscape(chs[i]));
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
            assertTrue(CommonUtil.equals(CommonUtil.asArray(""), strs));
        }

        {
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split((String) null, '*')));
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", '*')));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a.b.c", '.')));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a..b.c", '.')));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a b c", " ")));
        }

        {
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitPreserveAllTokens(null, '*')));
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", '*')));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a.b.c", '.')));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "", "b", "c"), Strings.splitPreserveAllTokens("a..b.c", '.')));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a b c", " ")));
        }

        {
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split((String) null, '*')));
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", "*")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a.b.c", ".")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a..b.c", ".")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a b c", " ")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("ab", "cd:ef"), Strings.split("ab:cd:ef", ":", 2)));
        }

        {
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.splitPreserveAllTokens(null, '*')));
            assertTrue(CommonUtil.equals(CommonUtil.asArray(""), Strings.splitPreserveAllTokens("", "*")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a.b.c", ".")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "", "b", "c"), Strings.splitPreserveAllTokens("a..b.c", ".")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.splitPreserveAllTokens("a b c", " ")));
        }

        {
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split((String) null, '*')));
            assertTrue(CommonUtil.equals(CommonUtil.EMPTY_STRING_ARRAY, Strings.split("", "*")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a..b..c", "..")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a....b..c", "..")));
            assertTrue(CommonUtil.equals(CommonUtil.asArray("a", "b", "c"), Strings.split("a  b  c", " ")));
            assertArrayEquals(CommonUtil.asArray("ab", "cd:;ef"), Strings.split("ab:;cd:;ef", ":;", 2));
        }

    }

    @Test
    public void testRemoveElementForAllArray() {

        {
            boolean[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.removeAllOccurrences(a, true)));

            a = new boolean[] { true };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.removeAllOccurrences(a, true)));

            a = new boolean[] { true, true };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.removeAllOccurrences(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { false, false }, N.removeAllOccurrences(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { true, true, true }, N.removeAllOccurrences(a, false)));

            a = new boolean[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.remove(a, true)));

            a = new boolean[] { true };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, N.remove(a, true)));

            a = new boolean[] { true, true };
            assertTrue(Arrays.equals(new boolean[] { true }, N.remove(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { false, true, false, true }, N.remove(a, true)));

            a = new boolean[] { false, true, true, false, true };
            assertTrue(Arrays.equals(new boolean[] { true, true, false, true }, N.remove(a, false)));
        }

        {
            char[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_CHAR_ARRAY, N.removeAllOccurrences(a, '2')));

            a = new char[] { '2' };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_CHAR_ARRAY, N.removeAllOccurrences(a, '2')));

            a = new char[] { '2', '2' };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_CHAR_ARRAY, N.removeAllOccurrences(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '3' }, N.removeAllOccurrences(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '2', '2', '3', '2' }, N.removeAllOccurrences(a, '4')));

            a = null;

            a = new char[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_CHAR_ARRAY, N.remove(a, '2')));

            a = new char[] { '2' };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_CHAR_ARRAY, N.remove(a, '2')));

            a = new char[] { '2', '2' };
            assertTrue(Arrays.equals(new char[] { '2' }, N.remove(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '2', '3', '2' }, N.remove(a, '2')));

            a = new char[] { '1', '2', '2', '3', '2' };
            assertTrue(Arrays.equals(new char[] { '1', '2', '2', '3', '2' }, N.remove(a, '4')));
        }

        {
            byte[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_BYTE_ARRAY, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BYTE_ARRAY, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 2, 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BYTE_ARRAY, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 3 }, N.removeAllOccurrences(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, (byte) 4)));

            a = new byte[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BYTE_ARRAY, N.remove(a, (byte) 2)));

            a = new byte[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_BYTE_ARRAY, N.remove(a, (byte) 2)));

            a = new byte[] { 2, 2 };
            assertTrue(Arrays.equals(new byte[] { 2 }, N.remove(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 2, 3, 2 }, N.remove(a, (byte) 2)));

            a = new byte[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new byte[] { 1, 2, 2, 3, 2 }, N.remove(a, (byte) 4)));
        }

        {
            short[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_SHORT_ARRAY, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_SHORT_ARRAY, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 2, 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_SHORT_ARRAY, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 3 }, N.removeAllOccurrences(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, (short) 4)));

            a = new short[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_SHORT_ARRAY, N.remove(a, (short) 2)));

            a = new short[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_SHORT_ARRAY, N.remove(a, (short) 2)));

            a = new short[] { 2, 2 };
            assertTrue(Arrays.equals(new short[] { 2 }, N.remove(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 2, 3, 2 }, N.remove(a, (short) 2)));

            a = new short[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new short[] { 1, 2, 2, 3, 2 }, N.remove(a, (short) 4)));
        }

        {
            int[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_INT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new int[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_INT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new int[] { 2, 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_INT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            a = new int[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_INT_ARRAY, N.remove(a, 2)));

            a = new int[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_INT_ARRAY, N.remove(a, 2)));

            a = new int[] { 2, 2 };
            assertTrue(Arrays.equals(new int[] { 2 }, N.remove(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new int[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new int[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            long[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_LONG_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new long[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_LONG_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new long[] { 2, 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_LONG_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            a = new long[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_LONG_ARRAY, N.remove(a, 2)));

            a = new long[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_LONG_ARRAY, N.remove(a, 2)));

            a = new long[] { 2, 2 };
            assertTrue(Arrays.equals(new long[] { 2 }, N.remove(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new long[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new long[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            float[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_FLOAT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new float[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_FLOAT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new float[] { 2, 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_FLOAT_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            a = new float[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_FLOAT_ARRAY, N.remove(a, 2)));

            a = new float[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_FLOAT_ARRAY, N.remove(a, 2)));

            a = new float[] { 2, 2 };
            assertTrue(Arrays.equals(new float[] { 2 }, N.remove(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new float[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new float[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            double[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new double[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new double[] { 2, 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.removeAllOccurrences(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 3 }, N.removeAllOccurrences(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 2, 2, 3, 2 }, N.removeAllOccurrences(a, 4)));

            a = new double[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.remove(a, 2)));

            a = new double[] { 2 };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, N.remove(a, 2)));

            a = new double[] { 2, 2 };
            assertTrue(Arrays.equals(new double[] { 2 }, N.remove(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 2, 3, 2 }, N.remove(a, 2)));

            a = new double[] { 1, 2, 2, 3, 2 };
            assertTrue(Arrays.equals(new double[] { 1, 2, 2, 3, 2 }, N.remove(a, 4)));
        }

        {
            String[] a = {};

            assertTrue(Arrays.equals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAllOccurrences(a, "2")));

            a = new String[] { "2" };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAllOccurrences(a, "2")));

            a = new String[] { "2", "2" };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_STRING_ARRAY, N.removeAllOccurrences(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "3" }, N.removeAllOccurrences(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "2", "2", "3", "2" }, N.removeAllOccurrences(a, "4")));

            a = new String[0];
            assertTrue(Arrays.equals(CommonUtil.EMPTY_STRING_ARRAY, N.remove(a, "2")));

            a = new String[] { "2" };
            assertTrue(Arrays.equals(CommonUtil.EMPTY_STRING_ARRAY, N.remove(a, "2")));

            a = new String[] { "2", "2" };
            assertTrue(Arrays.equals(new String[] { "2" }, N.remove(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "2", "3", "2" }, N.remove(a, "2")));

            a = new String[] { "1", "2", "2", "3", "2" };
            assertTrue(Arrays.equals(new String[] { "1", "2", "2", "3", "2" }, N.remove(a, "4")));
        }
    }

    @Test
    public void test_array_removeAt() {
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
                if (CommonUtil.indexOf(indices, i) < 0) {
                    list.add(a[i]);
                }
            }

            assertTrue(CommonUtil.equals(list.trimToSize().internalArray(), N.removeAt(a, indices)));
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
                if (CommonUtil.indexOf(indices, i) < 0) {
                    list.add(a[i]);
                }
            }

            assertTrue(CommonUtil.equals(list.trimToSize().internalArray(), N.removeAt(a, indices)));
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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

            a = N.removeAt(a, 3);
            a = N.removeAt(a, 3);
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
            final List<Integer> list = CommonUtil.toList(1, 2, 1, 1);

            list.remove(Integer.valueOf(1));
            assertEquals(3, list.size());
            list.remove(Integer.valueOf(1));
            assertEquals(2, list.size());
        }

        {
            List<?> list = CommonUtil.toList(1, 2, 3);
            N.removeAt(list, 0, 1, 2);
            N.println(list);
            assertEquals(0, list.size());

            list = CommonUtil.toList(1, 2, 3);
            N.removeAt(list, 0, 1, 1);
            N.println(list);
            assertEquals(CommonUtil.toList(3), list);

            list = CommonUtil.toList(1, 2, 3);
            N.removeAt(list, 0, 2);
            N.println(list);
            assertEquals(CommonUtil.toList(2), list);

            list = CommonUtil.toList(1, 2, 3);
            N.removeAt(list, 1);
            N.println(list);
            assertEquals(CommonUtil.toList(1, 3), list);

            list = CommonUtil.toList(1, 2, 2, 3);
            N.removeAt(list, 1);
            N.println(list);
            assertEquals(CommonUtil.toList(1, 2, 3), list);
        }
    }

    @Test
    public void test_array_op_2() {
        {
            final boolean[] a = { true, false };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new boolean[] { true, true, false, false }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final char[] a = { 'a', 'b' };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new char[] { 'a', 'a', 'b', 'b' }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final byte[] a = { 1, 2 };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new byte[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final short[] a = { 1, 2 };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new short[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final int[] a = { 1, 2 };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new int[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final long[] a = { 1, 2 };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new long[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final float[] a = { 1, 2 };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new float[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final double[] a = { 1, 2 };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new double[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }

        {
            final Object[] a = { 1, 2 };
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, a.length, a)));
            assertTrue(CommonUtil.equals(N.concat(a, a), N.insertAll(a, 0, a)));
            assertTrue(CommonUtil.equals(new Object[] { 1, 1, 2, 2 }, N.insertAll(a, 1, a)));
            assertTrue(CommonUtil.equals(a, N.removeAt(N.insertAll(a, 1, a), 1, 2)));
        }
    }

    @Test
    public void test_removeDuplicates() {
        {
            final char[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new char[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new byte[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new short[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new int[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new long[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new float[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new double[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final Object[] a = { '1', '2', '1', '3' };
            assertTrue(CommonUtil.equals(new Object[] { '1', '2', '3' }, N.removeDuplicates(a)));
        }

        {
            final List<Character> c = CommonUtil.toList('1', '2', '1', '3');
            N.removeDuplicates(c);
            assertTrue(CommonUtil.equals(CommonUtil.toList('1', '2', '3'), c));
        }
    }

    @Test
    public void test_removeDuplicates_sorted() {
        {
            final char[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new char[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new byte[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new short[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new int[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new long[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new float[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new double[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final Character[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(CommonUtil.equals(new Character[] { '1', '2', '3' }, N.removeDuplicates(a, true)));
        }

        {
            final List<Character> c = CommonUtil.toList('1', '2', '1', '3', '4', '3', '3', '2');
            CommonUtil.sort(c);
            N.removeDuplicates(c, true);
            assertTrue(CommonUtil.equals(CommonUtil.toList('1', '2', '3', '4'), c));
        }
    }

    @Test
    public void test_containsDuplicates() {
        {
            final char[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final Object[] a = { '1', '2', '1', '3' };
            assertTrue(N.containsDuplicates(a));
        }

        {
            final List<?> a = CommonUtil.toList('1', '2', '1', '3');
            assertTrue(N.containsDuplicates(a));
        }
    }

    @Test
    public void test_containsDuplicates_sorted() {
        {
            final char[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final byte[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final short[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final int[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final long[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final float[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final double[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final Character[] a = { '1', '2', '1', '3' };
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }

        {
            final List<Character> a = CommonUtil.toList('1', '2', '1', '3');
            CommonUtil.sort(a);
            assertTrue(N.containsDuplicates(a, true));
        }
    }

    @Test
    public void test_containsDuplicates_2() {
        {
            final char[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final byte[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final short[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final int[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final long[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final float[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final double[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final Object[] a = { '1', '2', '3' };
            assertFalse(N.containsDuplicates(a));
        }

        {
            final List<?> a = CommonUtil.toList('1', '2', '3');
            assertFalse(N.containsDuplicates(a));
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
            List<Integer> a = CommonUtil.toList(1, 2, 2);
            N.replaceAll(a, 2, 1);
            for (final Integer element : a) {
                assertEquals(1, (int) element);
            }

            a = null;
            assertFalse(N.replaceAll(a, 2, 1) > 0);
        }

        {
            List<String> list = CommonUtil.toList("b", "b", "b");

            for (int k = 5; k <= 1001; k++) {
                list = CommonUtil.toLinkedList();
                CommonUtil.fill(list, 0, k, "123");
                N.replaceAll(list, "123", "abc");

                for (int j = 0; j < k; j++) {
                    assertEquals("abc", list.get(j));
                }
            }
        }

        {
            List<String> list = CommonUtil.toList("b", "b", "b");

            for (int k = 5; k <= 1001; k++) {
                list = CommonUtil.toLinkedList();
                CommonUtil.fill(list, 0, k, null);
                N.replaceAll(list, null, "abc");

                for (int j = 0; j < k; j++) {
                    assertEquals("abc", list.get(j));
                }
            }
        }
    }

    @Test
    public void test_equalsIgnoreCase() {

        {
            final String[] a = CommonUtil.asArray("abC");
            final String[] b = CommonUtil.asArray("aBc");

            assertTrue(CommonUtil.equalsIgnoreCase(a[0], b[0]));
            assertTrue(CommonUtil.equalsIgnoreCase(a, b));
        }
    }

    @Test
    public void test_NotNullOrEmpty() {
        {
            final Dataset parameter = null;
            assertFalse(CommonUtil.notEmpty(parameter));
        }

        {
            final String parameter = "" + Strings.CHAR_CR;
            assertFalse(Strings.isNotBlank(parameter));
        }
    }

    @Test
    public void test_swap() {

        {
            final List<?> a = CommonUtil.toList(1, 2, 3);
            final List<?> b = new ArrayList<>(a);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a.get(0), b.get(2));
            assertEquals(a.get(2), b.get(0));
        }

        {
            final Object[] a = { 1, 2, 3 };
            final Object[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final boolean[] a = { false, true, true };
            final boolean[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final char[] a = { '1', '2', '3' };
            final char[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final byte[] a = { 1, 2, 3 };
            final byte[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final short[] a = { 1, 2, 3 };
            final short[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final int[] a = { 1, 2, 3 };
            final int[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final long[] a = { 1, 2, 3 };
            final long[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final float[] a = { 1, 2, 3 };
            final float[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

        {
            final double[] a = { 1, 2, 3 };
            final double[] b = CommonUtil.copyOf(a, a.length);

            CommonUtil.swap(a, 0, 2);

            assertEquals(a[0], b[2]);
            assertEquals(a[2], b[0]);
        }

    }
}
