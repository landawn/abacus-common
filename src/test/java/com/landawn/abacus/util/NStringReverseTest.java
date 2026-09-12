package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractParserTest;

public class NStringReverseTest extends AbstractParserTest {

    private static final Random rand = new Random();

    protected void changeCPUCoreNum(final int c) {
    }

    @Test
    public void test_reverse() {
        {
            final String str = "a.b.c";
            String str2 = Strings.reverseDelimited(str, '.');
            N.println(str2);
            assertFalse(CommonUtil.equals(str, str2));

            str2 = Strings.reverseDelimited(str2, '.');
            assertTrue(CommonUtil.equals(str, str2));
        }

        {

            final String str = "abc";
            String str2 = Strings.reverse(str);
            assertFalse(CommonUtil.equals(str, str2));

            str2 = Strings.reverse(str2);
            assertTrue(CommonUtil.equals(str, str2));
        }

        {
            final List<Object> list = null;
            CommonUtil.reverse(list);
        }

        {
            final Object[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final boolean[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final char[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final byte[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final short[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final int[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final long[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final float[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final double[] a = null;
            CommonUtil.reverse(a);
        }

        {
            final List<?> a = CommonUtil.toList(1, 2, 3);
            final List<?> b = new ArrayList<>(a);

            CommonUtil.reverse(a);

            for (int i = 0, len = a.size(); i < len; i++) {
                assertEquals(a.get(i), b.get(len - i - 1));
            }
        }

        {
            final List<?> a = CommonUtil.toList(1, 2, 3, 4, 5, 6);
            final List<?> b = new ArrayList<>(a);

            CommonUtil.reverse(a);

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
                CommonUtil.reverse(b);

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
                CommonUtil.reverse(b, fromIndex, toIndex);

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
                CommonUtil.reverse(b, fromIndex, toIndex);

                for (int k = 0, size = toIndex - fromIndex; k < size; k++) {
                    assertEquals(a.get(k + fromIndex), b.get(toIndex - k - 1));
                }
            }
        }

        {
            final Object[] a = { 1, 2, 3 };
            final Object[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final boolean[] a = { false, true, true };
            final boolean[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final char[] a = { '1', '2', '3' };
            final char[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final byte[] a = { 1, 2, 3 };
            final byte[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final short[] a = { 1, 2, 3 };
            final short[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final int[] a = { 1, 2, 3 };
            final int[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final long[] a = { 1, 2, 3 };
            final long[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final float[] a = { 1, 2, 3 };
            final float[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final double[] a = { 1, 2, 3 };
            final double[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final boolean[] a = { false, true, false, false, true, true, true, true, true };
            final boolean[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final char[] a = { '1', '2', '3', '4', '5', '6', '7', '8', '9' };
            final char[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final byte[] a = { 1, 2, 3, 4, 5, 6 };
            final byte[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final short[] a = { 1, 2, 3, 4, 5, 6 };
            final short[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final int[] a = { 1, 2, 3, 4, 5, 6 };
            final int[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final long[] a = { 1, 2, 3, 4, 5, 6 };
            final long[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final float[] a = { 1, 2, 3, 4, 5, 6 };
            final float[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

        {
            final double[] a = { 1, 2, 3, 4, 5, 6 };
            final double[] b = CommonUtil.copyOf(a, a.length);
            CommonUtil.reverse(a);

            for (int i = 0, len = a.length; i < len; i++) {
                assertEquals(a[i], b[len - i - 1]);
            }
        }

    }

    @Test
    public void test_checkNullOrEmpty() {

        try {
            final String parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Object[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final boolean[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final char[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final byte[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final short[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final int[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final long[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final float[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final double[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final List<String> parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Map<String, Object> parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final String parameter = null;
            CommonUtil.checkArgNotBlank(parameter, "parameter");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty or blank", e.getMessage());
        }

        try {
            final String parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Object[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final boolean[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final char[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final byte[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final short[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final int[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final long[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final float[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final double[] parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final List<String> parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final Map<String, Object> parameter = null;
            CommonUtil.checkArgNotEmpty(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }

        try {
            final String parameter = null;
            CommonUtil.checkArgNotBlank(parameter, "'parameter' cannot be null or empty");
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {
            assertEquals("'parameter' cannot be null or empty", e.getMessage());
        }
    }

    @Test
    public void test_createNumber() throws Exception {
        N.println(Numbers.decodeInteger(null));
        N.println(Numbers.decodeInteger("123"));
        N.println(Numbers.decodeInteger("0x123"));
        N.println(Numbers.decodeLong(null));
        N.println(Numbers.decodeLong("123"));
        N.println(Numbers.decodeLong("123l"));
        N.println(Numbers.decodeLong("123L"));
        N.println(Numbers.decodeLong("0X123"));
        N.println(Numbers.parseFloat(null));
        N.println(Numbers.parseFloat("123"));
        N.println(Numbers.parseFloat("123.0139f"));
        N.println(Numbers.parseDouble("123e139f"));
        N.println(Numbers.parseDouble(null));
        N.println(Numbers.parseDouble("123"));
        N.println(Numbers.parseDouble("123.0139d"));
        N.println(Numbers.parseDouble("123e139d"));

        N.println(Numbers.decodeBigInteger(null));
        N.println(Numbers.decodeBigInteger("123"));
        N.println(Numbers.decodeBigInteger("0X123"));

        N.println(Numbers.parseBigDecimal(null));
        N.println(Numbers.parseBigDecimal("123"));
        N.println(Numbers.parseBigDecimal("123.0139"));

        N.println(Numbers.createNumber(null));
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
        assertEquals("121abc", str);

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
        assertEquals("abc121", str);

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
            final List<String> list1 = ImmutableList.of("a", "b", "c");
            N.println(list1);

            final List<String> list2 = ImmutableList.wrap(CommonUtil.toList("a", "b", "c"));
            N.println(list2);

            assertEquals(list1, list2);

            final Set<String> set1 = ImmutableSet.of("a", "b", "c");
            N.println(set1);

            final Set<String> set2 = ImmutableSet.wrap(CommonUtil.toSet("a", "b", "c"));
            N.println(set2);

            assertEquals(set1, set2);
        }

        {
            final List<String> list1 = Collections.synchronizedList(CommonUtil.toList("a", "b", "c"));
            N.println(list1);

            final List<String> list2 = Collections.synchronizedList(CommonUtil.toList("a", "b", "c"));
            N.println(list2);

            assertEquals(list1, list2);

            final Collection<String> set4 = Collections.synchronizedSortedSet(CommonUtil.toSortedSet("a", "b", "c"));
            N.println(set4);

            final Collection<String> set5 = Collections.synchronizedCollection(CommonUtil.toSet("a", "b", "c"));
            N.println(set5);

            final Map<String, String> map1 = Collections.synchronizedMap(CommonUtil.asMap("a", "1", "b", "2"));
            N.println(map1);

            @SuppressWarnings("rawtypes")
            final Map<String, String> map2 = Collections.synchronizedMap((Map) CommonUtil.asMap("a", "1", "b", "2"));
            N.println(map2);

        }

        {
            Multiset<String> multiSet = CommonUtil.toMultiset("a", "b", "c", "a", "a", "b");
            assertEquals(3, multiSet.getCount("a"));

            multiSet = new Multiset<>(CommonUtil.toList("a", "b", "c", "a", "a", "b"));
            multiSet = new Multiset<>(CommonUtil.toList("a", "b", "c", "a", "a", "b"));
            assertEquals(3, multiSet.getCount("a"));
        }

    }

    @Test
    public void test_newMultimap() throws Exception {
        assertDoesNotThrow(() -> {
            N.println(new BiMap<>());
            N.println(new BiMap<>(12));

            N.println(new Multiset<>(10));

            N.println(CommonUtil.newListMultimap(12));
            N.println(CommonUtil.newLinkedListMultimap(12));
            N.println(CommonUtil.newSortedListMultimap());
            N.println(CommonUtil.newSetMultimap(12));
            N.println(CommonUtil.newLinkedSetMultimap(12));
            N.println(CommonUtil.newSortedSetMultimap());
        });
    }

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
                CommonUtil.parallelSort(a);
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
                CommonUtil.parallelSort(a);
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
                CommonUtil.parallelSort(a);
                Arrays.sort(b);

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
                CommonUtil.parallelSort(a);
                Arrays.sort(b);

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
                CommonUtil.parallelSort(a);
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

                CommonUtil.parallelSort(a);

                for (int k = 1; k < len; k++) {
                    assertTrue(a.get(k).compareTo(a.get(k - 1)) >= 0);
                }

                CommonUtil.parallelSort(a);
                CommonUtil.sort(b);

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

                CommonUtil.parallelSort(list);

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

                CommonUtil.parallelSort(list);

                for (int k = 1; k < len; k++) {
                    assertTrue(list.get(k).compareTo(list.get(k - 1)) >= 0);
                }
            }
        }
    }

    @Test
    public void test_commonPrefix() {
        String commPrefix = Strings.commonPrefix(null, null);
        assertNull(commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix(null, "");
        assertNull(commPrefix);
        N.println(commPrefix);

        commPrefix = Strings.commonPrefix("", null);
        assertNull(commPrefix);
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
        assertNull(commPrefix);
        N.println(commPrefix);
    }

    @Test
    public void test_commonSuffix() {
        String commSuffix = Strings.commonSuffix(null, null);
        assertNull(commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix(null, null, null, null);
        assertNull(commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix(null, "");
        assertNull(commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("", null);
        assertNull(commSuffix);
        N.println(commSuffix);

        commSuffix = Strings.commonSuffix("", null, "", null);
        assertNull(commSuffix);
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
}
