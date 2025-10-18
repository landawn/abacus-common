package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import lombok.Data;

@Tag("new-test")
public class CommonUtil101Test extends TestBase {

    @Test
    public void testEqualsLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L, 5L };
        long[] arr2 = { 10L, 2L, 3L, 40L, 50L };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr1, 1, 2));

        Assertions.assertTrue(CommonUtil.equals(arr1, 0, arr2, 0, 0));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testEqualsFloatArrays() {
        Assertions.assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        Assertions.assertFalse(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 3.0f }));
        Assertions.assertTrue(CommonUtil.equals((float[]) null, null));
        Assertions.assertFalse(CommonUtil.equals(new float[] { 1.0f }, null));

        Assertions.assertTrue(CommonUtil.equals(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testEqualsFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 10.0f, 2.0f, 3.0f, 40.0f };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        float[] nanArr1 = { Float.NaN, 2.0f };
        float[] nanArr2 = { Float.NaN, 2.0f };
        Assertions.assertTrue(CommonUtil.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsFloatArraysWithDelta() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.01f, 2.01f, 3.01f };
        float[] arr3 = { 1.1f, 2.1f, 3.1f };

        Assertions.assertTrue(CommonUtil.equals(arr1, arr2, 0.02f));
        Assertions.assertFalse(CommonUtil.equals(arr1, arr3, 0.02f));

        Assertions.assertTrue(CommonUtil.equals((float[]) null, null, 0.01f));
        Assertions.assertTrue(CommonUtil.equals(new float[0], new float[0], 0.01f));
        Assertions.assertFalse(CommonUtil.equals(arr1, null, 0.01f));
        Assertions.assertFalse(CommonUtil.equals(arr1, new float[] { 1.0f, 2.0f }, 0.01f));
    }

    @Test
    public void testEqualsDoubleArrays() {
        Assertions.assertTrue(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        Assertions.assertFalse(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 3.0 }));
        Assertions.assertTrue(CommonUtil.equals((double[]) null, null));
        Assertions.assertFalse(CommonUtil.equals(new double[] { 1.0 }, null));

        Assertions.assertTrue(CommonUtil.equals(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testEqualsDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 10.0, 2.0, 3.0, 40.0 };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        double[] nanArr1 = { Double.NaN, 2.0 };
        double[] nanArr2 = { Double.NaN, 2.0 };
        Assertions.assertTrue(CommonUtil.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsDoubleArraysWithDelta() {
        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.01, 2.01, 3.01 };
        double[] arr3 = { 1.1, 2.1, 3.1 };

        Assertions.assertTrue(CommonUtil.equals(arr1, arr2, 0.02));
        Assertions.assertFalse(CommonUtil.equals(arr1, arr3, 0.02));

        Assertions.assertTrue(CommonUtil.equals((double[]) null, null, 0.01));
        Assertions.assertTrue(CommonUtil.equals(new double[0], new double[0], 0.01));
        Assertions.assertFalse(CommonUtil.equals(arr1, null, 0.01));
        Assertions.assertFalse(CommonUtil.equals(arr1, new double[] { 1.0, 2.0 }, 0.01));
    }

    @Test
    public void testEqualsObjectArrays() {
        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        String[] arr3 = { "a", "b", "d" };

        Assertions.assertTrue(CommonUtil.equals(arr1, arr2));
        Assertions.assertFalse(CommonUtil.equals(arr1, arr3));
        Assertions.assertTrue(CommonUtil.equals(arr1, arr1));
        Assertions.assertTrue(CommonUtil.equals((Object[]) null, null));
        Assertions.assertFalse(CommonUtil.equals(arr1, null));

        String[] nullArr1 = { "a", null, "c" };
        String[] nullArr2 = { "a", null, "c" };
        String[] nullArr3 = { "a", "b", null };
        Assertions.assertTrue(CommonUtil.equals(nullArr1, nullArr2));
        Assertions.assertFalse(CommonUtil.equals(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertTrue(CommonUtil.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equals(arr1, 0, arr2, 0, 2));

        Integer[] intArr = { 1, 2, 3 };
        Long[] longArr = { 1L, 2L, 3L };
        Assertions.assertFalse(CommonUtil.equals(intArr, 0, longArr, 0, 2));
    }

    @Test
    public void testDeepEquals() {
        Assertions.assertTrue(CommonUtil.deepEquals(5, 5));
        Assertions.assertFalse(CommonUtil.deepEquals(5, 6));
        Assertions.assertTrue(CommonUtil.deepEquals("test", "test"));
        Assertions.assertFalse(CommonUtil.deepEquals("test", "Test"));

        int[] intArr1 = { 1, 2, 3 };
        int[] intArr2 = { 1, 2, 3 };
        int[] intArr3 = { 1, 2, 4 };
        Assertions.assertTrue(CommonUtil.deepEquals(intArr1, intArr2));
        Assertions.assertFalse(CommonUtil.deepEquals(intArr1, intArr3));

        Object[] nested1 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested2 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested3 = { 1, new int[] { 2, 4 }, "test" };
        Assertions.assertTrue(CommonUtil.deepEquals(nested1, nested2));
        Assertions.assertFalse(CommonUtil.deepEquals(nested1, nested3));

        Assertions.assertTrue(CommonUtil.deepEquals(null, null));
        Assertions.assertFalse(CommonUtil.deepEquals(null, "test"));
        Assertions.assertFalse(CommonUtil.deepEquals("test", null));
    }

    @Test
    public void testDeepEqualsArrays() {
        Object[] arr1 = { "a", 1, true };
        Object[] arr2 = { "a", 1, true };
        Object[] arr3 = { "a", 1, false };

        Assertions.assertTrue(CommonUtil.deepEquals(arr1, arr2));
        Assertions.assertFalse(CommonUtil.deepEquals(arr1, arr3));
        Assertions.assertTrue(CommonUtil.deepEquals((Object[]) null, null));
        Assertions.assertFalse(CommonUtil.deepEquals(arr1, null));

        Object[] nested1 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested2 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested3 = { new int[] { 1, 3 }, new String[] { "a", "b" } };

        Assertions.assertTrue(CommonUtil.deepEquals(nested1, nested2));
        Assertions.assertFalse(CommonUtil.deepEquals(nested1, nested3));
    }

    @Test
    public void testDeepEqualsArraysWithRange() {
        Object[] arr1 = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Object[] arr2 = { "x", new int[] { 1, 2 }, "c", new int[] { 5, 6 } };

        Assertions.assertTrue(CommonUtil.deepEquals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.deepEquals(arr1, 2, arr2, 2, 2));

        Assertions.assertFalse(CommonUtil.deepEquals(new Integer[] { 1 }, 0, new Long[] { 1L }, 0, 1));
    }

    @Test
    public void testEqualsIgnoreCase() {
        String[] arr1 = { "Hello", "World", "TEST" };
        String[] arr2 = { "hello", "WORLD", "test" };
        String[] arr3 = { "hello", "WORLD", "testing" };

        Assertions.assertTrue(CommonUtil.equalsIgnoreCase(arr1, arr2));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(arr1, arr3));
        Assertions.assertTrue(CommonUtil.equalsIgnoreCase((String[]) null, null));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(arr1, null));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(null, arr2));

        String[] nullArr1 = { "Hello", null, "TEST" };
        String[] nullArr2 = { "hello", null, "test" };
        String[] nullArr3 = { "hello", "world", "test" };
        Assertions.assertTrue(CommonUtil.equalsIgnoreCase(nullArr1, nullArr2));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsIgnoreCaseWithRange() {
        String[] arr1 = { "a", "Hello", "World", "d" };
        String[] arr2 = { "x", "HELLO", "world", "y" };

        Assertions.assertTrue(CommonUtil.equalsIgnoreCase(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(CommonUtil.equalsIgnoreCase(arr1, 0, arr2, 0, 2));
    }

    @Test
    public void testEqualsByKeys() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.put("c", 4);
        map2.put("d", 5);

        Assertions.assertTrue(CommonUtil.equalsByKeys(map1, map2, Arrays.asList("a", "b")));

        Assertions.assertFalse(CommonUtil.equalsByKeys(map1, map2, Arrays.asList("a", "c")));

        Assertions.assertTrue(CommonUtil.equalsByKeys(map1, map1, Arrays.asList("a", "b")));

        Assertions.assertFalse(CommonUtil.equalsByKeys(null, map2, Arrays.asList("a")));
        Assertions.assertFalse(CommonUtil.equalsByKeys(map1, null, Arrays.asList("a")));
        Assertions.assertTrue(CommonUtil.equalsByKeys(null, null, Arrays.asList("a")));

        Map<String, Integer> nullMap1 = new HashMap<>();
        nullMap1.put("a", null);
        nullMap1.put("b", 2);

        Map<String, Integer> nullMap2 = new HashMap<>();
        nullMap2.put("a", null);
        nullMap2.put("b", 2);

        Assertions.assertTrue(CommonUtil.equalsByKeys(nullMap1, nullMap2, Arrays.asList("a", "b")));

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommonUtil.equalsByKeys(map1, map2, Collections.emptyList()));
    }

    @Data
    public class TestBean {
        private String name;
        private int value;

        TestBean(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testEqualsByProps() {

        TestBean bean1 = new TestBean("test", 10);
        TestBean bean2 = new TestBean("test", 10);
        TestBean bean3 = new TestBean("test", 20);

        Assertions.assertTrue(Beans.equalsByProps(bean1, bean2, Arrays.asList("name", "value")));

        Assertions.assertFalse(Beans.equalsByProps(bean1, bean3, Arrays.asList("value")));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.equalsByProps(bean1, bean2, Collections.emptyList()));
    }

    @Data
    public class Bean1 {
        private String name = "test";
        private int value = 10;

    }

    @Data
    public class Bean2 {
        private String name = "test";
        private int value = 10;
        private String extra = "extra";

    }

    @Test
    public void testEqualsByCommonProps() {

        Bean1 bean1 = new Bean1();
        Bean2 bean2 = new Bean2();

        Assertions.assertTrue(Beans.equalsByCommonProps(bean1, bean2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(null, bean2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(bean1, null));
    }

    @Test
    public void testEqualsCollection() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> CommonUtil.equalsCollection(null, null));
    }

    @Test
    public void testHashCodePrimitives() {
        Assertions.assertEquals(1231, CommonUtil.hashCode(true));
        Assertions.assertEquals(1237, CommonUtil.hashCode(false));

        Assertions.assertEquals('a', CommonUtil.hashCode('a'));

        Assertions.assertEquals(10, CommonUtil.hashCode((byte) 10));

        Assertions.assertEquals(100, CommonUtil.hashCode((short) 100));

        Assertions.assertEquals(1000, CommonUtil.hashCode(1000));

        Assertions.assertEquals(Long.valueOf(1000L).hashCode(), CommonUtil.hashCode(1000L));

        Assertions.assertEquals(Float.floatToIntBits(10.5f), CommonUtil.hashCode(10.5f));

        Assertions.assertEquals(Double.valueOf(10.5).hashCode(), CommonUtil.hashCode(10.5));
    }

    @Test
    public void testHashCodeObject() {
        Assertions.assertEquals(0, CommonUtil.hashCode((Object) null));

        String str = "test";
        Assertions.assertEquals(str.hashCode(), CommonUtil.hashCode(str));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), CommonUtil.hashCode(intArr));

        String[] strArr = { "a", "b" };
        Assertions.assertEquals(Arrays.hashCode(strArr), CommonUtil.hashCode(strArr));
    }

    @Test
    public void testHashCodeArrays() {
        boolean[] boolArr = { true, false, true };
        Assertions.assertEquals(Arrays.hashCode(boolArr), CommonUtil.hashCode(boolArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((boolean[]) null));

        boolean[] boolArr2 = { false, true, false, true };
        int expected = 1;
        expected = 31 * expected + 1231;
        expected = 31 * expected + 1237;
        Assertions.assertEquals(expected, CommonUtil.hashCode(boolArr2, 1, 3));

        char[] charArr = { 'a', 'b', 'c' };
        Assertions.assertEquals(Arrays.hashCode(charArr), CommonUtil.hashCode(charArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((char[]) null));

        char[] charArr2 = { 'x', 'a', 'b', 'y' };
        int expected2 = 1;
        expected2 = 31 * expected2 + 'a';
        expected2 = 31 * expected2 + 'b';
        Assertions.assertEquals(expected2, CommonUtil.hashCode(charArr2, 1, 3));

        byte[] byteArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(byteArr), CommonUtil.hashCode(byteArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((byte[]) null));

        short[] shortArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(shortArr), CommonUtil.hashCode(shortArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((short[]) null));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), CommonUtil.hashCode(intArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((int[]) null));

        long[] longArr = { 1L, 2L, 3L };
        Assertions.assertEquals(Arrays.hashCode(longArr), CommonUtil.hashCode(longArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((long[]) null));

        float[] floatArr = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(Arrays.hashCode(floatArr), CommonUtil.hashCode(floatArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((float[]) null));

        double[] doubleArr = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(Arrays.hashCode(doubleArr), CommonUtil.hashCode(doubleArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((double[]) null));

        String[] strArr = { "a", "b", "c" };
        Assertions.assertEquals(Arrays.hashCode(strArr), CommonUtil.hashCode(strArr));
        Assertions.assertEquals(0, CommonUtil.hashCode((Object[]) null));
    }

    @Test
    public void testHashCodeArraysWithRange() {

        byte[] byteArr = { 1, 2, 3, 4, 5 };
        int expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, CommonUtil.hashCode(byteArr, 1, 3));

        short[] shortArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, CommonUtil.hashCode(shortArr, 1, 3));

        int[] intArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, CommonUtil.hashCode(intArr, 1, 3));

        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        expected = 1;
        expected = 31 * expected + Long.hashCode(2L);
        expected = 31 * expected + Long.hashCode(3L);
        Assertions.assertEquals(expected, CommonUtil.hashCode(longArr, 1, 3));

        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        expected = 1;
        expected = 31 * expected + Float.floatToIntBits(2.0f);
        expected = 31 * expected + Float.floatToIntBits(3.0f);
        Assertions.assertEquals(expected, CommonUtil.hashCode(floatArr, 1, 3));

        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        expected = 1;
        expected = 31 * expected + Double.hashCode(2.0);
        expected = 31 * expected + Double.hashCode(3.0);
        Assertions.assertEquals(expected, CommonUtil.hashCode(doubleArr, 1, 3));

        String[] strArr = { "a", "b", "c", "d", "e" };
        expected = 1;
        expected = 31 * expected + "b".hashCode();
        expected = 31 * expected + "c".hashCode();
        Assertions.assertEquals(expected, CommonUtil.hashCode(strArr, 1, 3));
    }

    @Test
    public void testDeepHashCode() {
        Assertions.assertEquals(0, CommonUtil.deepHashCode((Object) null));

        String str = "test";
        Assertions.assertEquals(str.hashCode(), CommonUtil.deepHashCode(str));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), CommonUtil.deepHashCode(intArr));

        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals(Arrays.deepHashCode(nested), CommonUtil.deepHashCode(nested));
    }

    @Test
    public void testDeepHashCodeArrays() {
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals(Arrays.deepHashCode(arr), CommonUtil.deepHashCode(arr));
        Assertions.assertEquals(0, CommonUtil.deepHashCode((Object[]) null));

        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals(Arrays.deepHashCode(nested), CommonUtil.deepHashCode(nested));
    }

    @Test
    public void testDeepHashCodeArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };

        int expected = 1;
        expected = 31 * expected + Arrays.hashCode(new int[] { 1, 2 });
        expected = 31 * expected + "c".hashCode();

        Assertions.assertEquals(expected, CommonUtil.deepHashCode(arr, 1, 3));
    }

    @Test
    public void testHashCodeEverything() {
        Assertions.assertEquals(0L, CommonUtil.hashCodeEverything(null));

        Assertions.assertEquals(31L + CommonUtil.hashCode(42), CommonUtil.hashCodeEverything(42));

        String str = "test";
        Assertions.assertEquals(31L + CommonUtil.hashCode(str), CommonUtil.hashCodeEverything(str));

        List<Integer> list = Arrays.asList(1, 2, 3);
        long expected = 1L;
        for (Integer i : list) {
            expected = 31L * expected + CommonUtil.hashCodeEverything(i);
        }
        Assertions.assertEquals(expected, CommonUtil.hashCodeEverything(list));

        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        expected = 1L;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            expected = 31L * expected + CommonUtil.hashCodeEverything(entry.getKey());
            expected = 31L * expected + CommonUtil.hashCodeEverything(entry.getValue());
        }
        Assertions.assertEquals(expected, CommonUtil.hashCodeEverything(map));

        int[] arr = { 1, 2, 3 };
        expected = 31L + CommonUtil.deepHashCode(arr);
        Assertions.assertEquals(expected, CommonUtil.hashCodeEverything(arr));

        Object[] objArr = { "a", 1, true };
        expected = 1L;
        for (Object obj : objArr) {
            expected = 31L * expected + CommonUtil.hashCodeEverything(obj);
        }
        Assertions.assertEquals(expected, CommonUtil.hashCodeEverything(objArr));
    }

    @Test
    public void testToStringPrimitives() {
        Assertions.assertEquals("true", CommonUtil.toString(true));
        Assertions.assertEquals("false", CommonUtil.toString(false));

        Assertions.assertEquals("a", CommonUtil.toString('a'));

        Assertions.assertEquals("10", CommonUtil.toString((byte) 10));

        Assertions.assertEquals("100", CommonUtil.toString((short) 100));

        Assertions.assertEquals("1000", CommonUtil.toString(1000));

        Assertions.assertEquals("1000", CommonUtil.toString(1000L));

        Assertions.assertEquals("10.5", CommonUtil.toString(10.5f));

        Assertions.assertEquals("10.5", CommonUtil.toString(10.5));
    }

    @Test
    public void testToStringObject() {
        Assertions.assertEquals("null", CommonUtil.toString((Object) null));

        Assertions.assertEquals("test", CommonUtil.toString("test"));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(intArr));

        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iter = list.iterator();
        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(iter));

        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(list));

        Object obj = new Object();
        Assertions.assertEquals(obj.toString(), CommonUtil.toString(obj));
    }

    @Test
    public void testToStringWithDefault() {
        Assertions.assertEquals("test", CommonUtil.toString("test", "default"));
        Assertions.assertEquals("default", CommonUtil.toString(null, "default"));
    }

    @Test
    public void testToStringArrays() {
        Assertions.assertEquals("[true, false, true]", CommonUtil.toString(new boolean[] { true, false, true }));
        Assertions.assertEquals("null", CommonUtil.toString((boolean[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new boolean[0]));

        boolean[] boolArr = { true, false, true, false };
        Assertions.assertEquals("[false, true]", CommonUtil.toString(boolArr, 1, 3));

        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals("null", CommonUtil.toString((char[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new char[0]));

        char[] charArr = { 'a', 'b', 'c', 'd' };
        Assertions.assertEquals("[b, c]", CommonUtil.toString(charArr, 1, 3));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals("null", CommonUtil.toString((byte[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new byte[0]));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new short[] { 1, 2, 3 }));
        Assertions.assertEquals("null", CommonUtil.toString((short[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new short[0]));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new int[] { 1, 2, 3 }));
        Assertions.assertEquals("null", CommonUtil.toString((int[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new int[0]));

        Assertions.assertEquals("[1, 2, 3]", CommonUtil.toString(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals("null", CommonUtil.toString((long[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new long[0]));

        Assertions.assertEquals("[1.0, 2.0, 3.0]", CommonUtil.toString(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals("null", CommonUtil.toString((float[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new float[0]));

        Assertions.assertEquals("[1.0, 2.0, 3.0]", CommonUtil.toString(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals("null", CommonUtil.toString((double[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new double[0]));

        Assertions.assertEquals("[a, b, c]", CommonUtil.toString(new String[] { "a", "b", "c" }));
        Assertions.assertEquals("null", CommonUtil.toString((Object[]) null));
        Assertions.assertEquals("[]", CommonUtil.toString(new Object[0]));
    }

    @Test
    public void testToStringArraysWithRange() {

        byte[] byteArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(byteArr, 1, 3));

        short[] shortArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(shortArr, 1, 3));

        int[] intArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(intArr, 1, 3));

        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        Assertions.assertEquals("[2, 3]", CommonUtil.toString(longArr, 1, 3));

        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Assertions.assertEquals("[2.0, 3.0]", CommonUtil.toString(floatArr, 1, 3));

        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Assertions.assertEquals("[2.0, 3.0]", CommonUtil.toString(doubleArr, 1, 3));

        String[] strArr = { "a", "b", "c", "d", "e" };
        Assertions.assertEquals("[b, c]", CommonUtil.toString(strArr, 1, 3));
    }

    @Test
    public void testDeepToString() {
        Assertions.assertEquals("null", CommonUtil.deepToString(null));

        Assertions.assertEquals("test", CommonUtil.deepToString("test"));

        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", CommonUtil.deepToString(intArr));

        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals("[1, [2, 3], test]", CommonUtil.deepToString(nested));

        Assertions.assertEquals("[true, false]", CommonUtil.deepToString(new boolean[] { true, false }));
        Assertions.assertEquals("[a, b]", CommonUtil.deepToString(new char[] { 'a', 'b' }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new byte[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new short[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new int[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", CommonUtil.deepToString(new long[] { 1L, 2L }));
        Assertions.assertEquals("[1.0, 2.0]", CommonUtil.deepToString(new float[] { 1.0f, 2.0f }));
        Assertions.assertEquals("[1.0, 2.0]", CommonUtil.deepToString(new double[] { 1.0, 2.0 }));
        Assertions.assertEquals("[a, b]", CommonUtil.deepToString(new String[] { "a", "b" }));
    }

    @Test
    public void testDeepToStringArrays() {
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals("[a, 1, true]", CommonUtil.deepToString(arr));
        Assertions.assertEquals("null", CommonUtil.deepToString((Object[]) null));
        Assertions.assertEquals("[]", CommonUtil.deepToString(new Object[0]));

        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals("[[1, 2], [a, b]]", CommonUtil.deepToString(nested));

        Object[] circular = new Object[2];
        circular[0] = "test";
        circular[1] = circular;
        String result = CommonUtil.deepToString(circular);
        Assertions.assertTrue(result.contains("[...]"));
    }

    @Test
    public void testDeepToStringArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Assertions.assertEquals("[[1, 2], c]", CommonUtil.deepToString(arr, 1, 3));
    }

    @Test
    public void testDeepToStringWithDefault() {
        Object[] arr = { "a", "b" };
        Assertions.assertEquals("[a, b]", CommonUtil.deepToString(arr, "default"));
        Assertions.assertEquals("default", CommonUtil.deepToString((Object[]) null, "default"));
    }

    @Test
    public void testLen() {
        Assertions.assertEquals(4, CommonUtil.len("test"));
        Assertions.assertEquals(0, CommonUtil.len(""));
        Assertions.assertEquals(0, CommonUtil.len((CharSequence) null));

        Assertions.assertEquals(3, CommonUtil.len(new boolean[] { true, false, true }));
        Assertions.assertEquals(0, CommonUtil.len(new boolean[0]));
        Assertions.assertEquals(0, CommonUtil.len((boolean[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals(0, CommonUtil.len(new char[0]));
        Assertions.assertEquals(0, CommonUtil.len((char[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals(0, CommonUtil.len(new byte[0]));
        Assertions.assertEquals(0, CommonUtil.len((byte[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new short[] { 1, 2, 3 }));
        Assertions.assertEquals(0, CommonUtil.len(new short[0]));
        Assertions.assertEquals(0, CommonUtil.len((short[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new int[] { 1, 2, 3 }));
        Assertions.assertEquals(0, CommonUtil.len(new int[0]));
        Assertions.assertEquals(0, CommonUtil.len((int[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals(0, CommonUtil.len(new long[0]));
        Assertions.assertEquals(0, CommonUtil.len((long[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals(0, CommonUtil.len(new float[0]));
        Assertions.assertEquals(0, CommonUtil.len((float[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals(0, CommonUtil.len(new double[0]));
        Assertions.assertEquals(0, CommonUtil.len((double[]) null));

        Assertions.assertEquals(3, CommonUtil.len(new Object[] { "a", "b", "c" }));
        Assertions.assertEquals(0, CommonUtil.len(new Object[0]));
        Assertions.assertEquals(0, CommonUtil.len((Object[]) null));
    }

    @Test
    public void testSize() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals(3, CommonUtil.size(list));
        Assertions.assertEquals(0, CommonUtil.size(new ArrayList<>()));
        Assertions.assertEquals(0, CommonUtil.size((Collection<?>) null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertEquals(2, CommonUtil.size(map));
        Assertions.assertEquals(0, CommonUtil.size(new HashMap<>()));
        Assertions.assertEquals(0, CommonUtil.size((Map<?, ?>) null));
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(CommonUtil.isEmpty((CharSequence) null));
        Assertions.assertTrue(CommonUtil.isEmpty(""));
        Assertions.assertFalse(CommonUtil.isEmpty("test"));

        Assertions.assertTrue(CommonUtil.isEmpty((boolean[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new boolean[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new boolean[] { true }));

        Assertions.assertTrue(CommonUtil.isEmpty((char[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new char[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new char[] { 'a' }));

        Assertions.assertTrue(CommonUtil.isEmpty((byte[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new byte[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new byte[] { 1 }));

        Assertions.assertTrue(CommonUtil.isEmpty((short[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new short[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new short[] { 1 }));

        Assertions.assertTrue(CommonUtil.isEmpty((int[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new int[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new int[] { 1 }));

        Assertions.assertTrue(CommonUtil.isEmpty((long[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new long[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new long[] { 1L }));

        Assertions.assertTrue(CommonUtil.isEmpty((float[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new float[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new float[] { 1.0f }));

        Assertions.assertTrue(CommonUtil.isEmpty((double[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new double[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new double[] { 1.0 }));

        Assertions.assertTrue(CommonUtil.isEmpty((Object[]) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new Object[0]));
        Assertions.assertFalse(CommonUtil.isEmpty(new Object[] { "a" }));

        Assertions.assertTrue(CommonUtil.isEmpty((Collection<?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new ArrayList<>()));
        Assertions.assertFalse(CommonUtil.isEmpty(Arrays.asList("a")));

        Assertions.assertTrue(CommonUtil.isEmpty((Iterable<?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertFalse(CommonUtil.isEmpty((Iterable<?>) Arrays.asList("a")));

        Assertions.assertTrue(CommonUtil.isEmpty((Iterator<?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new ArrayList<>().iterator()));
        Assertions.assertFalse(CommonUtil.isEmpty(Arrays.asList("a").iterator()));

        Assertions.assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        Assertions.assertTrue(CommonUtil.isEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertFalse(CommonUtil.isEmpty(map));
    }

    @Test
    public void testIsBlank() {
        Assertions.assertTrue(CommonUtil.isBlank(null));
        Assertions.assertTrue(CommonUtil.isBlank(""));
        Assertions.assertTrue(CommonUtil.isBlank(" "));
        Assertions.assertTrue(CommonUtil.isBlank("   "));
        Assertions.assertTrue(CommonUtil.isBlank("\t"));
        Assertions.assertTrue(CommonUtil.isBlank("\n"));
        Assertions.assertTrue(CommonUtil.isBlank("\t\n "));
        Assertions.assertFalse(CommonUtil.isBlank("a"));
        Assertions.assertFalse(CommonUtil.isBlank(" a "));
    }

    @Test
    public void testBooleanChecks() {
        Assertions.assertTrue(CommonUtil.isTrue(Boolean.TRUE));
        Assertions.assertFalse(CommonUtil.isTrue(Boolean.FALSE));
        Assertions.assertFalse(CommonUtil.isTrue(null));

        Assertions.assertTrue(CommonUtil.isNotTrue(null));
        Assertions.assertTrue(CommonUtil.isNotTrue(Boolean.FALSE));
        Assertions.assertFalse(CommonUtil.isNotTrue(Boolean.TRUE));

        Assertions.assertTrue(CommonUtil.isFalse(Boolean.FALSE));
        Assertions.assertFalse(CommonUtil.isFalse(Boolean.TRUE));
        Assertions.assertFalse(CommonUtil.isFalse(null));

        Assertions.assertTrue(CommonUtil.isNotFalse(null));
        Assertions.assertTrue(CommonUtil.isNotFalse(Boolean.TRUE));
        Assertions.assertFalse(CommonUtil.isNotFalse(Boolean.FALSE));
    }

    @Test
    public void testNotEmpty() {
        Assertions.assertFalse(CommonUtil.notEmpty((CharSequence) null));
        Assertions.assertFalse(CommonUtil.notEmpty(""));
        Assertions.assertTrue(CommonUtil.notEmpty("test"));

        Assertions.assertFalse(CommonUtil.notEmpty((boolean[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new boolean[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new boolean[] { true }));

        Assertions.assertFalse(CommonUtil.notEmpty((char[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new char[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new char[] { 'a' }));

        Assertions.assertFalse(CommonUtil.notEmpty((byte[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new byte[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new byte[] { 1 }));

        Assertions.assertFalse(CommonUtil.notEmpty((short[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new short[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new short[] { 1 }));

        Assertions.assertFalse(CommonUtil.notEmpty((int[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new int[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new int[] { 1 }));

        Assertions.assertFalse(CommonUtil.notEmpty((long[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new long[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new long[] { 1L }));

        Assertions.assertFalse(CommonUtil.notEmpty((float[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new float[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new float[] { 1.0f }));

        Assertions.assertFalse(CommonUtil.notEmpty((double[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new double[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new double[] { 1.0 }));

        Assertions.assertFalse(CommonUtil.notEmpty((Object[]) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new Object[0]));
        Assertions.assertTrue(CommonUtil.notEmpty(new Object[] { "a" }));

        Assertions.assertFalse(CommonUtil.notEmpty((Collection<?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.notEmpty(Arrays.asList("a")));

        Assertions.assertFalse(CommonUtil.notEmpty((Iterable<?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.notEmpty((Iterable<?>) Arrays.asList("a")));

        Assertions.assertFalse(CommonUtil.notEmpty((Iterator<?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new ArrayList<>().iterator()));
        Assertions.assertTrue(CommonUtil.notEmpty(Arrays.asList("a").iterator()));

        Assertions.assertFalse(CommonUtil.notEmpty((Map<?, ?>) null));
        Assertions.assertFalse(CommonUtil.notEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertTrue(CommonUtil.notEmpty(map));
    }

    @Test
    public void testNotBlank() {
        Assertions.assertFalse(CommonUtil.notBlank(null));
        Assertions.assertFalse(CommonUtil.notBlank(""));
        Assertions.assertFalse(CommonUtil.notBlank(" "));
        Assertions.assertFalse(CommonUtil.notBlank("   "));
        Assertions.assertFalse(CommonUtil.notBlank("\t"));
        Assertions.assertFalse(CommonUtil.notBlank("\n"));
        Assertions.assertFalse(CommonUtil.notBlank("\t\n "));
        Assertions.assertTrue(CommonUtil.notBlank("a"));
        Assertions.assertTrue(CommonUtil.notBlank(" a "));
    }

    @Test
    public void testAnyNull() {
        Assertions.assertTrue(CommonUtil.anyNull(null, "b"));
        Assertions.assertTrue(CommonUtil.anyNull("a", null));
        Assertions.assertTrue(CommonUtil.anyNull(null, null));
        Assertions.assertFalse(CommonUtil.anyNull("a", "b"));

        Assertions.assertTrue(CommonUtil.anyNull(null, "b", "c"));
        Assertions.assertTrue(CommonUtil.anyNull("a", null, "c"));
        Assertions.assertTrue(CommonUtil.anyNull("a", "b", null));
        Assertions.assertTrue(CommonUtil.anyNull(null, null, null));
        Assertions.assertFalse(CommonUtil.anyNull("a", "b", "c"));

        Assertions.assertFalse(CommonUtil.anyNull());
        Assertions.assertTrue(CommonUtil.anyNull("a", null, "c", "d"));
        Assertions.assertFalse(CommonUtil.anyNull("a", "b", "c", "d"));

        Assertions.assertFalse(CommonUtil.anyNull((Iterable<?>) null));
        Assertions.assertFalse(CommonUtil.anyNull(new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.anyNull(Arrays.asList("a", null, "c")));
        Assertions.assertFalse(CommonUtil.anyNull(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAnyEmpty() {
        Assertions.assertTrue(CommonUtil.anyEmpty("", "b"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", ""));
        Assertions.assertTrue(CommonUtil.anyEmpty(null, "b"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", null));
        Assertions.assertFalse(CommonUtil.anyEmpty("a", "b"));

        Assertions.assertTrue(CommonUtil.anyEmpty("", "b", "c"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", "", "c"));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", "b", ""));
        Assertions.assertTrue(CommonUtil.anyEmpty(null, "b", "c"));
        Assertions.assertFalse(CommonUtil.anyEmpty("a", "b", "c"));

        Assertions.assertFalse(CommonUtil.anyEmpty((CharSequence[]) null));
        Assertions.assertFalse(CommonUtil.anyEmpty(new CharSequence[0]));
        Assertions.assertTrue(CommonUtil.anyEmpty("a", "", "c"));
        Assertions.assertFalse(CommonUtil.anyEmpty("a", "b", "c"));

        Assertions.assertFalse(CommonUtil.anyEmpty((Iterable<? extends CharSequence>) null));
        Assertions.assertFalse(CommonUtil.anyEmpty(new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a", "", "c")));
        Assertions.assertFalse(CommonUtil.anyEmpty(Arrays.asList("a", "b", "c")));

        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "b" }));
        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0]));
        Assertions.assertTrue(CommonUtil.anyEmpty((Object[]) null, new Object[] { "b" }));
        Assertions.assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));

        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "b" }, new Object[] { "c" }));
        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "c" }));
        Assertions.assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[0]));
        Assertions.assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));

        Assertions.assertTrue(CommonUtil.anyEmpty(new ArrayList<>(), Arrays.asList("b")));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), new ArrayList<>()));
        Assertions.assertTrue(CommonUtil.anyEmpty((Collection<?>) null, Arrays.asList("b")));
        Assertions.assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));

        Assertions.assertTrue(CommonUtil.anyEmpty(new ArrayList<>(), Arrays.asList("b"), Arrays.asList("c")));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), new ArrayList<>(), Arrays.asList("c")));
        Assertions.assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), new ArrayList<>()));
        Assertions.assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
    }
}
