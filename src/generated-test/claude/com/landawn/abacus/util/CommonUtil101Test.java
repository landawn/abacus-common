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
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import lombok.Data;

public class CommonUtil101Test extends TestBase {

    @Test
    public void testEqualsLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L, 5L };
        long[] arr2 = { 10L, 2L, 3L, 40L, 50L };

        // Test equals range
        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        // Test same array reference
        Assertions.assertTrue(N.equals(arr1, 1, arr1, 1, 2));

        // Test zero length
        Assertions.assertTrue(N.equals(arr1, 0, arr2, 0, 0));

        // Test edge cases
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.equals(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.equals(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testEqualsFloatArrays() {
        // Test basic equals
        Assertions.assertTrue(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        Assertions.assertFalse(N.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 3.0f }));
        Assertions.assertTrue(N.equals((float[]) null, null));
        Assertions.assertFalse(N.equals(new float[] { 1.0f }, null));

        // Test with NaN
        Assertions.assertTrue(N.equals(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testEqualsFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 10.0f, 2.0f, 3.0f, 40.0f };

        // Test equals range
        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        // Test with NaN
        float[] nanArr1 = { Float.NaN, 2.0f };
        float[] nanArr2 = { Float.NaN, 2.0f };
        Assertions.assertTrue(N.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsFloatArraysWithDelta() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.01f, 2.01f, 3.01f };
        float[] arr3 = { 1.1f, 2.1f, 3.1f };

        // Test with delta
        Assertions.assertTrue(N.equals(arr1, arr2, 0.02f));
        Assertions.assertFalse(N.equals(arr1, arr3, 0.02f));

        // Test null and empty arrays
        Assertions.assertTrue(N.equals((float[]) null, null, 0.01f));
        Assertions.assertTrue(N.equals(new float[0], new float[0], 0.01f));
        Assertions.assertFalse(N.equals(arr1, null, 0.01f));
        Assertions.assertFalse(N.equals(arr1, new float[] { 1.0f, 2.0f }, 0.01f));
    }

    @Test
    public void testEqualsDoubleArrays() {
        // Test basic equals
        Assertions.assertTrue(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        Assertions.assertFalse(N.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 3.0 }));
        Assertions.assertTrue(N.equals((double[]) null, null));
        Assertions.assertFalse(N.equals(new double[] { 1.0 }, null));

        // Test with NaN
        Assertions.assertTrue(N.equals(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testEqualsDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 10.0, 2.0, 3.0, 40.0 };

        // Test equals range
        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        // Test with NaN
        double[] nanArr1 = { Double.NaN, 2.0 };
        double[] nanArr2 = { Double.NaN, 2.0 };
        Assertions.assertTrue(N.equals(nanArr1, 0, nanArr2, 0, 2));
    }

    @Test
    public void testEqualsDoubleArraysWithDelta() {
        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.01, 2.01, 3.01 };
        double[] arr3 = { 1.1, 2.1, 3.1 };

        // Test with delta
        Assertions.assertTrue(N.equals(arr1, arr2, 0.02));
        Assertions.assertFalse(N.equals(arr1, arr3, 0.02));

        // Test null and empty arrays
        Assertions.assertTrue(N.equals((double[]) null, null, 0.01));
        Assertions.assertTrue(N.equals(new double[0], new double[0], 0.01));
        Assertions.assertFalse(N.equals(arr1, null, 0.01));
        Assertions.assertFalse(N.equals(arr1, new double[] { 1.0, 2.0 }, 0.01));
    }

    @Test
    public void testEqualsObjectArrays() {
        // Test basic equals
        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        String[] arr3 = { "a", "b", "d" };

        Assertions.assertTrue(N.equals(arr1, arr2));
        Assertions.assertFalse(N.equals(arr1, arr3));
        Assertions.assertTrue(N.equals(arr1, arr1));
        Assertions.assertTrue(N.equals((Object[]) null, null));
        Assertions.assertFalse(N.equals(arr1, null));

        // Test with nulls in array
        String[] nullArr1 = { "a", null, "c" };
        String[] nullArr2 = { "a", null, "c" };
        String[] nullArr3 = { "a", "b", null };
        Assertions.assertTrue(N.equals(nullArr1, nullArr2));
        Assertions.assertFalse(N.equals(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        // Test equals range
        Assertions.assertTrue(N.equals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equals(arr1, 0, arr2, 0, 2));

        // Test with different array types
        Integer[] intArr = { 1, 2, 3 };
        Long[] longArr = { 1L, 2L, 3L };
        Assertions.assertFalse(N.equals(intArr, 0, longArr, 0, 2));
    }

    @Test
    public void testDeepEquals() {
        // Test primitive types
        Assertions.assertTrue(N.deepEquals(5, 5));
        Assertions.assertFalse(N.deepEquals(5, 6));
        Assertions.assertTrue(N.deepEquals("test", "test"));
        Assertions.assertFalse(N.deepEquals("test", "Test"));

        // Test arrays
        int[] intArr1 = { 1, 2, 3 };
        int[] intArr2 = { 1, 2, 3 };
        int[] intArr3 = { 1, 2, 4 };
        Assertions.assertTrue(N.deepEquals(intArr1, intArr2));
        Assertions.assertFalse(N.deepEquals(intArr1, intArr3));

        // Test nested arrays
        Object[] nested1 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested2 = { 1, new int[] { 2, 3 }, "test" };
        Object[] nested3 = { 1, new int[] { 2, 4 }, "test" };
        Assertions.assertTrue(N.deepEquals(nested1, nested2));
        Assertions.assertFalse(N.deepEquals(nested1, nested3));

        // Test null
        Assertions.assertTrue(N.deepEquals(null, null));
        Assertions.assertFalse(N.deepEquals(null, "test"));
        Assertions.assertFalse(N.deepEquals("test", null));
    }

    @Test
    public void testDeepEqualsArrays() {
        // Test simple array
        Object[] arr1 = { "a", 1, true };
        Object[] arr2 = { "a", 1, true };
        Object[] arr3 = { "a", 1, false };

        Assertions.assertTrue(N.deepEquals(arr1, arr2));
        Assertions.assertFalse(N.deepEquals(arr1, arr3));
        Assertions.assertTrue(N.deepEquals((Object[]) null, null));
        Assertions.assertFalse(N.deepEquals(arr1, null));

        // Test nested arrays
        Object[] nested1 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested2 = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Object[] nested3 = { new int[] { 1, 3 }, new String[] { "a", "b" } };

        Assertions.assertTrue(N.deepEquals(nested1, nested2));
        Assertions.assertFalse(N.deepEquals(nested1, nested3));
    }

    @Test
    public void testDeepEqualsArraysWithRange() {
        Object[] arr1 = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Object[] arr2 = { "x", new int[] { 1, 2 }, "c", new int[] { 5, 6 } };

        // Test equals range
        Assertions.assertTrue(N.deepEquals(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.deepEquals(arr1, 2, arr2, 2, 2));

        // Test with different array types
        Assertions.assertFalse(N.deepEquals(new Integer[] { 1 }, 0, new Long[] { 1L }, 0, 1));
    }

    @Test
    public void testEqualsIgnoreCase() {
        String[] arr1 = { "Hello", "World", "TEST" };
        String[] arr2 = { "hello", "WORLD", "test" };
        String[] arr3 = { "hello", "WORLD", "testing" };

        Assertions.assertTrue(N.equalsIgnoreCase(arr1, arr2));
        Assertions.assertFalse(N.equalsIgnoreCase(arr1, arr3));
        Assertions.assertTrue(N.equalsIgnoreCase((String[]) null, null));
        Assertions.assertFalse(N.equalsIgnoreCase(arr1, null));
        Assertions.assertFalse(N.equalsIgnoreCase(null, arr2));

        // Test with nulls in array
        String[] nullArr1 = { "Hello", null, "TEST" };
        String[] nullArr2 = { "hello", null, "test" };
        String[] nullArr3 = { "hello", "world", "test" };
        Assertions.assertTrue(N.equalsIgnoreCase(nullArr1, nullArr2));
        Assertions.assertFalse(N.equalsIgnoreCase(nullArr1, nullArr3));
    }

    @Test
    public void testEqualsIgnoreCaseWithRange() {
        String[] arr1 = { "a", "Hello", "World", "d" };
        String[] arr2 = { "x", "HELLO", "world", "y" };

        Assertions.assertTrue(N.equalsIgnoreCase(arr1, 1, arr2, 1, 2));
        Assertions.assertFalse(N.equalsIgnoreCase(arr1, 0, arr2, 0, 2));
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

        // Test with matching keys and values
        Assertions.assertTrue(N.equalsByKeys(map1, map2, Arrays.asList("a", "b")));

        // Test with non-matching values
        Assertions.assertFalse(N.equalsByKeys(map1, map2, Arrays.asList("a", "c")));

        // Test same map reference
        Assertions.assertTrue(N.equalsByKeys(map1, map1, Arrays.asList("a", "b")));

        // Test null maps
        Assertions.assertFalse(N.equalsByKeys(null, map2, Arrays.asList("a")));
        Assertions.assertFalse(N.equalsByKeys(map1, null, Arrays.asList("a")));
        Assertions.assertTrue(N.equalsByKeys(null, null, Arrays.asList("a")));

        // Test with null values
        Map<String, Integer> nullMap1 = new HashMap<>();
        nullMap1.put("a", null);
        nullMap1.put("b", 2);

        Map<String, Integer> nullMap2 = new HashMap<>();
        nullMap2.put("a", null);
        nullMap2.put("b", 2);

        Assertions.assertTrue(N.equalsByKeys(nullMap1, nullMap2, Arrays.asList("a", "b")));

        // Test empty keys
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.equalsByKeys(map1, map2, Collections.emptyList()));
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

        // Test with matching properties
        Assertions.assertTrue(N.equalsByProps(bean1, bean2, Arrays.asList("name", "value")));

        // Test with non-matching properties
        Assertions.assertFalse(N.equalsByProps(bean1, bean3, Arrays.asList("value")));

        // Test empty properties
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.equalsByProps(bean1, bean2, Collections.emptyList()));
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

        // Test with common properties
        Assertions.assertTrue(N.equalsByCommonProps(bean1, bean2));

        // Test null arguments
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(null, bean2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(bean1, null));
    }

    @Test
    public void testEqualsCollection() {
        // This method throws UnsupportedOperationException
        Assertions.assertThrows(UnsupportedOperationException.class, () -> N.equalsCollection(null, null));
    }

    @Test
    public void testHashCodePrimitives() {
        // boolean
        Assertions.assertEquals(1231, N.hashCode(true));
        Assertions.assertEquals(1237, N.hashCode(false));

        // char
        Assertions.assertEquals('a', N.hashCode('a'));

        // byte
        Assertions.assertEquals(10, N.hashCode((byte) 10));

        // short
        Assertions.assertEquals(100, N.hashCode((short) 100));

        // int
        Assertions.assertEquals(1000, N.hashCode(1000));

        // long
        Assertions.assertEquals(Long.valueOf(1000L).hashCode(), N.hashCode(1000L));

        // float
        Assertions.assertEquals(Float.floatToIntBits(10.5f), N.hashCode(10.5f));

        // double
        Assertions.assertEquals(Double.valueOf(10.5).hashCode(), N.hashCode(10.5));
    }

    @Test
    public void testHashCodeObject() {
        // Test null
        Assertions.assertEquals(0, N.hashCode((Object) null));

        // Test regular object
        String str = "test";
        Assertions.assertEquals(str.hashCode(), N.hashCode(str));

        // Test arrays
        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), N.hashCode(intArr));

        String[] strArr = { "a", "b" };
        Assertions.assertEquals(Arrays.hashCode(strArr), N.hashCode(strArr));
    }

    @Test
    public void testHashCodeArrays() {
        // boolean array
        boolean[] boolArr = { true, false, true };
        Assertions.assertEquals(Arrays.hashCode(boolArr), N.hashCode(boolArr));
        Assertions.assertEquals(0, N.hashCode((boolean[]) null));

        // Test with range
        boolean[] boolArr2 = { false, true, false, true };
        int expected = 1;
        expected = 31 * expected + 1231; // true
        expected = 31 * expected + 1237; // false
        Assertions.assertEquals(expected, N.hashCode(boolArr2, 1, 3));

        // char array
        char[] charArr = { 'a', 'b', 'c' };
        Assertions.assertEquals(Arrays.hashCode(charArr), N.hashCode(charArr));
        Assertions.assertEquals(0, N.hashCode((char[]) null));

        // Test with range
        char[] charArr2 = { 'x', 'a', 'b', 'y' };
        int expected2 = 1;
        expected2 = 31 * expected2 + 'a';
        expected2 = 31 * expected2 + 'b';
        Assertions.assertEquals(expected2, N.hashCode(charArr2, 1, 3));

        // byte array
        byte[] byteArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(byteArr), N.hashCode(byteArr));
        Assertions.assertEquals(0, N.hashCode((byte[]) null));

        // short array
        short[] shortArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(shortArr), N.hashCode(shortArr));
        Assertions.assertEquals(0, N.hashCode((short[]) null));

        // int array
        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), N.hashCode(intArr));
        Assertions.assertEquals(0, N.hashCode((int[]) null));

        // long array
        long[] longArr = { 1L, 2L, 3L };
        Assertions.assertEquals(Arrays.hashCode(longArr), N.hashCode(longArr));
        Assertions.assertEquals(0, N.hashCode((long[]) null));

        // float array
        float[] floatArr = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(Arrays.hashCode(floatArr), N.hashCode(floatArr));
        Assertions.assertEquals(0, N.hashCode((float[]) null));

        // double array
        double[] doubleArr = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(Arrays.hashCode(doubleArr), N.hashCode(doubleArr));
        Assertions.assertEquals(0, N.hashCode((double[]) null));

        // Object array
        String[] strArr = { "a", "b", "c" };
        Assertions.assertEquals(Arrays.hashCode(strArr), N.hashCode(strArr));
        Assertions.assertEquals(0, N.hashCode((Object[]) null));
    }

    @Test
    public void testHashCodeArraysWithRange() {
        // Test all array types with range

        // byte array with range
        byte[] byteArr = { 1, 2, 3, 4, 5 };
        int expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, N.hashCode(byteArr, 1, 3));

        // short array with range
        short[] shortArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, N.hashCode(shortArr, 1, 3));

        // int array with range
        int[] intArr = { 1, 2, 3, 4, 5 };
        expected = 1;
        expected = 31 * expected + 2;
        expected = 31 * expected + 3;
        Assertions.assertEquals(expected, N.hashCode(intArr, 1, 3));

        // long array with range
        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        expected = 1;
        expected = 31 * expected + Long.hashCode(2L);
        expected = 31 * expected + Long.hashCode(3L);
        Assertions.assertEquals(expected, N.hashCode(longArr, 1, 3));

        // float array with range
        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        expected = 1;
        expected = 31 * expected + Float.floatToIntBits(2.0f);
        expected = 31 * expected + Float.floatToIntBits(3.0f);
        Assertions.assertEquals(expected, N.hashCode(floatArr, 1, 3));

        // double array with range
        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        expected = 1;
        expected = 31 * expected + Double.hashCode(2.0);
        expected = 31 * expected + Double.hashCode(3.0);
        Assertions.assertEquals(expected, N.hashCode(doubleArr, 1, 3));

        // Object array with range
        String[] strArr = { "a", "b", "c", "d", "e" };
        expected = 1;
        expected = 31 * expected + "b".hashCode();
        expected = 31 * expected + "c".hashCode();
        Assertions.assertEquals(expected, N.hashCode(strArr, 1, 3));
    }

    @Test
    public void testDeepHashCode() {
        // Test null
        Assertions.assertEquals(0, N.deepHashCode((Object) null));

        // Test regular object
        String str = "test";
        Assertions.assertEquals(str.hashCode(), N.deepHashCode(str));

        // Test primitive arrays
        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals(Arrays.hashCode(intArr), N.deepHashCode(intArr));

        // Test nested arrays
        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals(Arrays.deepHashCode(nested), N.deepHashCode(nested));
    }

    @Test
    public void testDeepHashCodeArrays() {
        // Test simple array
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals(Arrays.deepHashCode(arr), N.deepHashCode(arr));
        Assertions.assertEquals(0, N.deepHashCode((Object[]) null));

        // Test nested arrays
        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals(Arrays.deepHashCode(nested), N.deepHashCode(nested));
    }

    @Test
    public void testDeepHashCodeArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };

        // Calculate expected hash for range [1, 3)
        int expected = 1;
        expected = 31 * expected + Arrays.hashCode(new int[] { 1, 2 });
        expected = 31 * expected + "c".hashCode();

        Assertions.assertEquals(expected, N.deepHashCode(arr, 1, 3));
    }

    @Test
    public void testHashCodeEverything() {
        // Test null
        Assertions.assertEquals(0L, N.hashCodeEverything(null));

        // Test primitive
        Assertions.assertEquals(31L + N.hashCode(42), N.hashCodeEverything(42));

        // Test String
        String str = "test";
        Assertions.assertEquals(31L + N.hashCode(str), N.hashCodeEverything(str));

        // Test Collection
        List<Integer> list = Arrays.asList(1, 2, 3);
        long expected = 1L;
        for (Integer i : list) {
            expected = 31L * expected + N.hashCodeEverything(i);
        }
        Assertions.assertEquals(expected, N.hashCodeEverything(list));

        // Test Map
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        expected = 1L;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            expected = 31L * expected + N.hashCodeEverything(entry.getKey());
            expected = 31L * expected + N.hashCodeEverything(entry.getValue());
        }
        Assertions.assertEquals(expected, N.hashCodeEverything(map));

        // Test array
        int[] arr = { 1, 2, 3 };
        expected = 31L + N.deepHashCode(arr);
        Assertions.assertEquals(expected, N.hashCodeEverything(arr));

        // Test object array
        Object[] objArr = { "a", 1, true };
        expected = 1L;
        for (Object obj : objArr) {
            expected = 31L * expected + N.hashCodeEverything(obj);
        }
        Assertions.assertEquals(expected, N.hashCodeEverything(objArr));
    }

    @Test
    public void testToStringPrimitives() {
        // boolean
        Assertions.assertEquals("true", N.toString(true));
        Assertions.assertEquals("false", N.toString(false));

        // char
        Assertions.assertEquals("a", N.toString('a'));

        // byte
        Assertions.assertEquals("10", N.toString((byte) 10));

        // short
        Assertions.assertEquals("100", N.toString((short) 100));

        // int
        Assertions.assertEquals("1000", N.toString(1000));

        // long
        Assertions.assertEquals("1000", N.toString(1000L));

        // float
        Assertions.assertEquals("10.5", N.toString(10.5f));

        // double
        Assertions.assertEquals("10.5", N.toString(10.5));
    }

    @Test
    public void testToStringObject() {
        // Test null
        Assertions.assertEquals("null", N.toString((Object) null));

        // Test String
        Assertions.assertEquals("test", N.toString("test"));

        // Test arrays
        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", N.toString(intArr));

        // Test Iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iter = list.iterator();
        Assertions.assertEquals("[a, b, c]", N.toString(iter));

        // Test Iterable
        Assertions.assertEquals("[a, b, c]", N.toString((Iterable<?>) list));

        // Test regular object
        Object obj = new Object();
        Assertions.assertEquals(obj.toString(), N.toString(obj));
    }

    @Test
    public void testToStringWithDefault() {
        Assertions.assertEquals("test", N.toString("test", "default"));
        Assertions.assertEquals("default", N.toString(null, "default"));
    }

    @Test
    public void testToStringArrays() {
        // boolean array
        Assertions.assertEquals("[true, false, true]", N.toString(new boolean[] { true, false, true }));
        Assertions.assertEquals("null", N.toString((boolean[]) null));
        Assertions.assertEquals("[]", N.toString(new boolean[0]));

        // Test with range
        boolean[] boolArr = { true, false, true, false };
        Assertions.assertEquals("[false, true]", N.toString(boolArr, 1, 3));

        // char array
        Assertions.assertEquals("[a, b, c]", N.toString(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals("null", N.toString((char[]) null));
        Assertions.assertEquals("[]", N.toString(new char[0]));

        // Test with range
        char[] charArr = { 'a', 'b', 'c', 'd' };
        Assertions.assertEquals("[b, c]", N.toString(charArr, 1, 3));

        // byte array
        Assertions.assertEquals("[1, 2, 3]", N.toString(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals("null", N.toString((byte[]) null));
        Assertions.assertEquals("[]", N.toString(new byte[0]));

        // short array
        Assertions.assertEquals("[1, 2, 3]", N.toString(new short[] { 1, 2, 3 }));
        Assertions.assertEquals("null", N.toString((short[]) null));
        Assertions.assertEquals("[]", N.toString(new short[0]));

        // int array
        Assertions.assertEquals("[1, 2, 3]", N.toString(new int[] { 1, 2, 3 }));
        Assertions.assertEquals("null", N.toString((int[]) null));
        Assertions.assertEquals("[]", N.toString(new int[0]));

        // long array
        Assertions.assertEquals("[1, 2, 3]", N.toString(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals("null", N.toString((long[]) null));
        Assertions.assertEquals("[]", N.toString(new long[0]));

        // float array
        Assertions.assertEquals("[1.0, 2.0, 3.0]", N.toString(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals("null", N.toString((float[]) null));
        Assertions.assertEquals("[]", N.toString(new float[0]));

        // double array
        Assertions.assertEquals("[1.0, 2.0, 3.0]", N.toString(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals("null", N.toString((double[]) null));
        Assertions.assertEquals("[]", N.toString(new double[0]));

        // Object array
        Assertions.assertEquals("[a, b, c]", N.toString(new String[] { "a", "b", "c" }));
        Assertions.assertEquals("null", N.toString((Object[]) null));
        Assertions.assertEquals("[]", N.toString(new Object[0]));
    }

    @Test
    public void testToStringArraysWithRange() {
        // Test all array types with range

        // byte array with range
        byte[] byteArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", N.toString(byteArr, 1, 3));

        // short array with range
        short[] shortArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", N.toString(shortArr, 1, 3));

        // int array with range
        int[] intArr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals("[2, 3]", N.toString(intArr, 1, 3));

        // long array with range
        long[] longArr = { 1L, 2L, 3L, 4L, 5L };
        Assertions.assertEquals("[2, 3]", N.toString(longArr, 1, 3));

        // float array with range
        float[] floatArr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Assertions.assertEquals("[2.0, 3.0]", N.toString(floatArr, 1, 3));

        // double array with range
        double[] doubleArr = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Assertions.assertEquals("[2.0, 3.0]", N.toString(doubleArr, 1, 3));

        // Object array with range
        String[] strArr = { "a", "b", "c", "d", "e" };
        Assertions.assertEquals("[b, c]", N.toString(strArr, 1, 3));
    }

    @Test
    public void testDeepToString() {
        // Test null
        Assertions.assertEquals("null", N.deepToString(null));

        // Test regular object
        Assertions.assertEquals("test", N.deepToString("test"));

        // Test primitive arrays
        int[] intArr = { 1, 2, 3 };
        Assertions.assertEquals("[1, 2, 3]", N.deepToString(intArr));

        // Test nested arrays
        Object[] nested = { 1, new int[] { 2, 3 }, "test" };
        Assertions.assertEquals("[1, [2, 3], test]", N.deepToString(nested));

        // Test all primitive array types
        Assertions.assertEquals("[true, false]", N.deepToString(new boolean[] { true, false }));
        Assertions.assertEquals("[a, b]", N.deepToString(new char[] { 'a', 'b' }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new byte[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new short[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new int[] { 1, 2 }));
        Assertions.assertEquals("[1, 2]", N.deepToString(new long[] { 1L, 2L }));
        Assertions.assertEquals("[1.0, 2.0]", N.deepToString(new float[] { 1.0f, 2.0f }));
        Assertions.assertEquals("[1.0, 2.0]", N.deepToString(new double[] { 1.0, 2.0 }));
        Assertions.assertEquals("[a, b]", N.deepToString(new String[] { "a", "b" }));
    }

    @Test
    public void testDeepToStringArrays() {
        // Test simple array
        Object[] arr = { "a", 1, true };
        Assertions.assertEquals("[a, 1, true]", N.deepToString(arr));
        Assertions.assertEquals("null", N.deepToString((Object[]) null));
        Assertions.assertEquals("[]", N.deepToString(new Object[0]));

        // Test nested arrays
        Object[] nested = { new int[] { 1, 2 }, new String[] { "a", "b" } };
        Assertions.assertEquals("[[1, 2], [a, b]]", N.deepToString(nested));

        // Test circular reference
        Object[] circular = new Object[2];
        circular[0] = "test";
        circular[1] = circular;
        String result = N.deepToString(circular);
        Assertions.assertTrue(result.contains("[...]"));
    }

    @Test
    public void testDeepToStringArraysWithRange() {
        Object[] arr = { "a", new int[] { 1, 2 }, "c", new int[] { 3, 4 } };
        Assertions.assertEquals("[[1, 2], c]", N.deepToString(arr, 1, 3));
    }

    @Test
    public void testDeepToStringWithDefault() {
        Object[] arr = { "a", "b" };
        Assertions.assertEquals("[a, b]", N.deepToString(arr, "default"));
        Assertions.assertEquals("default", N.deepToString((Object[]) null, "default"));
    }

    @Test
    public void testLen() {
        // CharSequence
        Assertions.assertEquals(4, N.len("test"));
        Assertions.assertEquals(0, N.len(""));
        Assertions.assertEquals(0, N.len((CharSequence) null));

        // boolean array
        Assertions.assertEquals(3, N.len(new boolean[] { true, false, true }));
        Assertions.assertEquals(0, N.len(new boolean[0]));
        Assertions.assertEquals(0, N.len((boolean[]) null));

        // char array
        Assertions.assertEquals(3, N.len(new char[] { 'a', 'b', 'c' }));
        Assertions.assertEquals(0, N.len(new char[0]));
        Assertions.assertEquals(0, N.len((char[]) null));

        // byte array
        Assertions.assertEquals(3, N.len(new byte[] { 1, 2, 3 }));
        Assertions.assertEquals(0, N.len(new byte[0]));
        Assertions.assertEquals(0, N.len((byte[]) null));

        // short array
        Assertions.assertEquals(3, N.len(new short[] { 1, 2, 3 }));
        Assertions.assertEquals(0, N.len(new short[0]));
        Assertions.assertEquals(0, N.len((short[]) null));

        // int array
        Assertions.assertEquals(3, N.len(new int[] { 1, 2, 3 }));
        Assertions.assertEquals(0, N.len(new int[0]));
        Assertions.assertEquals(0, N.len((int[]) null));

        // long array
        Assertions.assertEquals(3, N.len(new long[] { 1L, 2L, 3L }));
        Assertions.assertEquals(0, N.len(new long[0]));
        Assertions.assertEquals(0, N.len((long[]) null));

        // float array
        Assertions.assertEquals(3, N.len(new float[] { 1.0f, 2.0f, 3.0f }));
        Assertions.assertEquals(0, N.len(new float[0]));
        Assertions.assertEquals(0, N.len((float[]) null));

        // double array
        Assertions.assertEquals(3, N.len(new double[] { 1.0, 2.0, 3.0 }));
        Assertions.assertEquals(0, N.len(new double[0]));
        Assertions.assertEquals(0, N.len((double[]) null));

        // Object array
        Assertions.assertEquals(3, N.len(new Object[] { "a", "b", "c" }));
        Assertions.assertEquals(0, N.len(new Object[0]));
        Assertions.assertEquals(0, N.len((Object[]) null));
    }

    @Test
    public void testSize() {
        // Collection
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals(3, N.size(list));
        Assertions.assertEquals(0, N.size(new ArrayList<>()));
        Assertions.assertEquals(0, N.size((Collection<?>) null));

        // Map
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertEquals(2, N.size(map));
        Assertions.assertEquals(0, N.size(new HashMap<>()));
        Assertions.assertEquals(0, N.size((Map<?, ?>) null));
    }

    @Test
    public void testIsEmpty() {
        // CharSequence
        Assertions.assertTrue(N.isEmpty((CharSequence) null));
        Assertions.assertTrue(N.isEmpty(""));
        Assertions.assertFalse(N.isEmpty("test"));

        // boolean array
        Assertions.assertTrue(N.isEmpty((boolean[]) null));
        Assertions.assertTrue(N.isEmpty(new boolean[0]));
        Assertions.assertFalse(N.isEmpty(new boolean[] { true }));

        // char array
        Assertions.assertTrue(N.isEmpty((char[]) null));
        Assertions.assertTrue(N.isEmpty(new char[0]));
        Assertions.assertFalse(N.isEmpty(new char[] { 'a' }));

        // byte array
        Assertions.assertTrue(N.isEmpty((byte[]) null));
        Assertions.assertTrue(N.isEmpty(new byte[0]));
        Assertions.assertFalse(N.isEmpty(new byte[] { 1 }));

        // short array
        Assertions.assertTrue(N.isEmpty((short[]) null));
        Assertions.assertTrue(N.isEmpty(new short[0]));
        Assertions.assertFalse(N.isEmpty(new short[] { 1 }));

        // int array
        Assertions.assertTrue(N.isEmpty((int[]) null));
        Assertions.assertTrue(N.isEmpty(new int[0]));
        Assertions.assertFalse(N.isEmpty(new int[] { 1 }));

        // long array
        Assertions.assertTrue(N.isEmpty((long[]) null));
        Assertions.assertTrue(N.isEmpty(new long[0]));
        Assertions.assertFalse(N.isEmpty(new long[] { 1L }));

        // float array
        Assertions.assertTrue(N.isEmpty((float[]) null));
        Assertions.assertTrue(N.isEmpty(new float[0]));
        Assertions.assertFalse(N.isEmpty(new float[] { 1.0f }));

        // double array
        Assertions.assertTrue(N.isEmpty((double[]) null));
        Assertions.assertTrue(N.isEmpty(new double[0]));
        Assertions.assertFalse(N.isEmpty(new double[] { 1.0 }));

        // Object array
        Assertions.assertTrue(N.isEmpty((Object[]) null));
        Assertions.assertTrue(N.isEmpty(new Object[0]));
        Assertions.assertFalse(N.isEmpty(new Object[] { "a" }));

        // Collection
        Assertions.assertTrue(N.isEmpty((Collection<?>) null));
        Assertions.assertTrue(N.isEmpty(new ArrayList<>()));
        Assertions.assertFalse(N.isEmpty(Arrays.asList("a")));

        // Iterable
        Assertions.assertTrue(N.isEmpty((Iterable<?>) null));
        Assertions.assertTrue(N.isEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertFalse(N.isEmpty((Iterable<?>) Arrays.asList("a")));

        // Iterator
        Assertions.assertTrue(N.isEmpty((Iterator<?>) null));
        Assertions.assertTrue(N.isEmpty(new ArrayList<>().iterator()));
        Assertions.assertFalse(N.isEmpty(Arrays.asList("a").iterator()));

        // Map
        Assertions.assertTrue(N.isEmpty((Map<?, ?>) null));
        Assertions.assertTrue(N.isEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertFalse(N.isEmpty(map));
    }

    @Test
    public void testIsBlank() {
        Assertions.assertTrue(N.isBlank(null));
        Assertions.assertTrue(N.isBlank(""));
        Assertions.assertTrue(N.isBlank(" "));
        Assertions.assertTrue(N.isBlank("   "));
        Assertions.assertTrue(N.isBlank("\t"));
        Assertions.assertTrue(N.isBlank("\n"));
        Assertions.assertTrue(N.isBlank("\t\n "));
        Assertions.assertFalse(N.isBlank("a"));
        Assertions.assertFalse(N.isBlank(" a "));
    }

    @Test
    public void testBooleanChecks() {
        // isTrue
        Assertions.assertTrue(N.isTrue(Boolean.TRUE));
        Assertions.assertFalse(N.isTrue(Boolean.FALSE));
        Assertions.assertFalse(N.isTrue(null));

        // isNotTrue
        Assertions.assertTrue(N.isNotTrue(null));
        Assertions.assertTrue(N.isNotTrue(Boolean.FALSE));
        Assertions.assertFalse(N.isNotTrue(Boolean.TRUE));

        // isFalse
        Assertions.assertTrue(N.isFalse(Boolean.FALSE));
        Assertions.assertFalse(N.isFalse(Boolean.TRUE));
        Assertions.assertFalse(N.isFalse(null));

        // isNotFalse
        Assertions.assertTrue(N.isNotFalse(null));
        Assertions.assertTrue(N.isNotFalse(Boolean.TRUE));
        Assertions.assertFalse(N.isNotFalse(Boolean.FALSE));
    }

    @Test
    public void testNotEmpty() {
        // CharSequence
        Assertions.assertFalse(N.notEmpty((CharSequence) null));
        Assertions.assertFalse(N.notEmpty(""));
        Assertions.assertTrue(N.notEmpty("test"));

        // boolean array
        Assertions.assertFalse(N.notEmpty((boolean[]) null));
        Assertions.assertFalse(N.notEmpty(new boolean[0]));
        Assertions.assertTrue(N.notEmpty(new boolean[] { true }));

        // char array
        Assertions.assertFalse(N.notEmpty((char[]) null));
        Assertions.assertFalse(N.notEmpty(new char[0]));
        Assertions.assertTrue(N.notEmpty(new char[] { 'a' }));

        // byte array
        Assertions.assertFalse(N.notEmpty((byte[]) null));
        Assertions.assertFalse(N.notEmpty(new byte[0]));
        Assertions.assertTrue(N.notEmpty(new byte[] { 1 }));

        // short array
        Assertions.assertFalse(N.notEmpty((short[]) null));
        Assertions.assertFalse(N.notEmpty(new short[0]));
        Assertions.assertTrue(N.notEmpty(new short[] { 1 }));

        // int array
        Assertions.assertFalse(N.notEmpty((int[]) null));
        Assertions.assertFalse(N.notEmpty(new int[0]));
        Assertions.assertTrue(N.notEmpty(new int[] { 1 }));

        // long array
        Assertions.assertFalse(N.notEmpty((long[]) null));
        Assertions.assertFalse(N.notEmpty(new long[0]));
        Assertions.assertTrue(N.notEmpty(new long[] { 1L }));

        // float array
        Assertions.assertFalse(N.notEmpty((float[]) null));
        Assertions.assertFalse(N.notEmpty(new float[0]));
        Assertions.assertTrue(N.notEmpty(new float[] { 1.0f }));

        // double array
        Assertions.assertFalse(N.notEmpty((double[]) null));
        Assertions.assertFalse(N.notEmpty(new double[0]));
        Assertions.assertTrue(N.notEmpty(new double[] { 1.0 }));

        // Object array
        Assertions.assertFalse(N.notEmpty((Object[]) null));
        Assertions.assertFalse(N.notEmpty(new Object[0]));
        Assertions.assertTrue(N.notEmpty(new Object[] { "a" }));

        // Collection
        Assertions.assertFalse(N.notEmpty((Collection<?>) null));
        Assertions.assertFalse(N.notEmpty(new ArrayList<>()));
        Assertions.assertTrue(N.notEmpty(Arrays.asList("a")));

        // Iterable
        Assertions.assertFalse(N.notEmpty((Iterable<?>) null));
        Assertions.assertFalse(N.notEmpty((Iterable<?>) new ArrayList<>()));
        Assertions.assertTrue(N.notEmpty((Iterable<?>) Arrays.asList("a")));

        // Iterator
        Assertions.assertFalse(N.notEmpty((Iterator<?>) null));
        Assertions.assertFalse(N.notEmpty(new ArrayList<>().iterator()));
        Assertions.assertTrue(N.notEmpty(Arrays.asList("a").iterator()));

        // Map
        Assertions.assertFalse(N.notEmpty((Map<?, ?>) null));
        Assertions.assertFalse(N.notEmpty(new HashMap<>()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertTrue(N.notEmpty(map));
    }

    @Test
    public void testNotBlank() {
        Assertions.assertFalse(N.notBlank(null));
        Assertions.assertFalse(N.notBlank(""));
        Assertions.assertFalse(N.notBlank(" "));
        Assertions.assertFalse(N.notBlank("   "));
        Assertions.assertFalse(N.notBlank("\t"));
        Assertions.assertFalse(N.notBlank("\n"));
        Assertions.assertFalse(N.notBlank("\t\n "));
        Assertions.assertTrue(N.notBlank("a"));
        Assertions.assertTrue(N.notBlank(" a "));
    }

    @Test
    public void testAnyNull() {
        // Two arguments
        Assertions.assertTrue(N.anyNull(null, "b"));
        Assertions.assertTrue(N.anyNull("a", null));
        Assertions.assertTrue(N.anyNull(null, null));
        Assertions.assertFalse(N.anyNull("a", "b"));

        // Three arguments
        Assertions.assertTrue(N.anyNull(null, "b", "c"));
        Assertions.assertTrue(N.anyNull("a", null, "c"));
        Assertions.assertTrue(N.anyNull("a", "b", null));
        Assertions.assertTrue(N.anyNull(null, null, null));
        Assertions.assertFalse(N.anyNull("a", "b", "c"));

        // Varargs
        Assertions.assertFalse(N.anyNull()); // empty array
        Assertions.assertTrue(N.anyNull("a", null, "c", "d"));
        Assertions.assertFalse(N.anyNull("a", "b", "c", "d"));

        // Iterable
        Assertions.assertFalse(N.anyNull((Iterable<?>) null));
        Assertions.assertFalse(N.anyNull(new ArrayList<>()));
        Assertions.assertTrue(N.anyNull(Arrays.asList("a", null, "c")));
        Assertions.assertFalse(N.anyNull(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testAnyEmpty() {
        // Two CharSequences
        Assertions.assertTrue(N.anyEmpty("", "b"));
        Assertions.assertTrue(N.anyEmpty("a", ""));
        Assertions.assertTrue(N.anyEmpty(null, "b"));
        Assertions.assertTrue(N.anyEmpty("a", null));
        Assertions.assertFalse(N.anyEmpty("a", "b"));

        // Three CharSequences
        Assertions.assertTrue(N.anyEmpty("", "b", "c"));
        Assertions.assertTrue(N.anyEmpty("a", "", "c"));
        Assertions.assertTrue(N.anyEmpty("a", "b", ""));
        Assertions.assertTrue(N.anyEmpty(null, "b", "c"));
        Assertions.assertFalse(N.anyEmpty("a", "b", "c"));

        // Varargs CharSequences
        Assertions.assertFalse(N.anyEmpty((CharSequence[]) null));
        Assertions.assertFalse(N.anyEmpty(new CharSequence[0]));
        Assertions.assertTrue(N.anyEmpty("a", "", "c"));
        Assertions.assertFalse(N.anyEmpty("a", "b", "c"));

        // Iterable<CharSequence>
        Assertions.assertFalse(N.anyEmpty((Iterable<? extends CharSequence>) null));
        Assertions.assertFalse(N.anyEmpty(new ArrayList<CharSequence>()));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a", "", "c")));
        Assertions.assertFalse(N.anyEmpty(Arrays.asList("a", "b", "c")));

        // Two Object arrays
        Assertions.assertTrue(N.anyEmpty(new Object[0], new Object[] { "b" }));
        Assertions.assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0]));
        Assertions.assertTrue(N.anyEmpty((Object[]) null, new Object[] { "b" }));
        Assertions.assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));

        // Three Object arrays
        Assertions.assertTrue(N.anyEmpty(new Object[0], new Object[] { "b" }, new Object[] { "c" }));
        Assertions.assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "c" }));
        Assertions.assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[0]));
        Assertions.assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));

        // Two Collections
        Assertions.assertTrue(N.anyEmpty(new ArrayList<>(), Arrays.asList("b")));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a"), new ArrayList<>()));
        Assertions.assertTrue(N.anyEmpty((Collection<?>) null, Arrays.asList("b")));
        Assertions.assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));

        // Three Collections
        Assertions.assertTrue(N.anyEmpty(new ArrayList<>(), Arrays.asList("b"), Arrays.asList("c")));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a"), new ArrayList<>(), Arrays.asList("c")));
        Assertions.assertTrue(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), new ArrayList<>()));
        Assertions.assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
    }
}