package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Wrapper2025Test extends TestBase {

    @Test
    public void test_of_nullArray() {
        Wrapper<int[]> wrapper = Wrapper.of(null);
        assertNotNull(wrapper);
        assertEquals(null, wrapper.value());
    }

    @Test
    public void test_of_zeroLengthPrimitiveArrays() {
        boolean[] boolArray = new boolean[0];
        Wrapper<boolean[]> boolWrapper1 = Wrapper.of(boolArray);
        Wrapper<boolean[]> boolWrapper2 = Wrapper.of(new boolean[0]);
        assertSame(boolWrapper1, boolWrapper2);

        int[] intArray = new int[0];
        Wrapper<int[]> intWrapper1 = Wrapper.of(intArray);
        Wrapper<int[]> intWrapper2 = Wrapper.of(new int[0]);
        assertSame(intWrapper1, intWrapper2);

        String[] strArray = new String[0];
        Wrapper<String[]> strWrapper1 = Wrapper.of(strArray);
        Wrapper<String[]> strWrapper2 = Wrapper.of(new String[0]);
        assertSame(strWrapper1, strWrapper2);
    }

    @Test
    public void test_of_primitiveArrays() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

        Wrapper<int[]> wrapper1 = Wrapper.of(arr1);
        Wrapper<int[]> wrapper2 = Wrapper.of(arr2);
        Wrapper<int[]> wrapper3 = Wrapper.of(arr3);

        assertNotNull(wrapper1);
        assertSame(arr1, wrapper1.value());

        assertEquals(wrapper1, wrapper2);
        assertNotEquals(wrapper1, wrapper3);

        assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
    }

    @Test
    public void test_of_multiDimensionalArrays() {
        int[][] matrix1 = { { 1, 2 }, { 3, 4 } };
        int[][] matrix2 = { { 1, 2 }, { 3, 4 } };
        int[][] matrix3 = { { 1, 2 }, { 3, 5 } };

        Wrapper<int[][]> wrapper1 = Wrapper.of(matrix1);
        Wrapper<int[][]> wrapper2 = Wrapper.of(matrix2);
        Wrapper<int[][]> wrapper3 = Wrapper.of(matrix3);

        assertEquals(wrapper1, wrapper2);
        assertNotEquals(wrapper1, wrapper3);
        assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
    }

    @Test
    public void test_of_objectArrays() {
        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        String[] arr3 = { "a", "b", "d" };

        Wrapper<String[]> wrapper1 = Wrapper.of(arr1);
        Wrapper<String[]> wrapper2 = Wrapper.of(arr2);
        Wrapper<String[]> wrapper3 = Wrapper.of(arr3);

        assertEquals(wrapper1, wrapper2);
        assertNotEquals(wrapper1, wrapper3);
        assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
    }

    @Test
    public void test_of_arrayInHashMap() {
        int[] key1 = { 1, 2, 3 };
        int[] key2 = { 1, 2, 3 };

        Map<Wrapper<int[]>, String> map = new HashMap<>();
        map.put(Wrapper.of(key1), "value1");

        assertEquals("value1", map.get(Wrapper.of(key2)));
    }

    @Test
    public void test_of_arrayInHashSet() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };

        Set<Wrapper<int[]>> set = new HashSet<>();
        set.add(Wrapper.of(arr1));

        assertTrue(set.contains(Wrapper.of(arr2)));
    }

    @Test
    public void test_of_withCustomFunctions_nullHashFunction() {
        assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", null, (s1, s2) -> s1.equals(s2));
        });
    }

    @Test
    public void test_of_withCustomFunctions_nullEqualsFunction() {
        assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", String::hashCode, null);
        });
    }

    @Test
    public void test_of_withCustomFunctions() {
        String str1 = "apple";
        String str2 = "apricot";
        String str3 = "banana";

        ToIntFunction<String> hashFunc = s -> s.charAt(0);
        BiPredicate<String, String> equalsFunc = (s1, s2) -> s1.charAt(0) == s2.charAt(0);

        Wrapper<String> wrapper1 = Wrapper.of(str1, hashFunc, equalsFunc);
        Wrapper<String> wrapper2 = Wrapper.of(str2, hashFunc, equalsFunc);
        Wrapper<String> wrapper3 = Wrapper.of(str3, hashFunc, equalsFunc);

        assertEquals(wrapper1, wrapper2);
        assertEquals(wrapper1.hashCode(), wrapper2.hashCode());

        assertNotEquals(wrapper1, wrapper3);
    }

    @Test
    public void test_of_withCustomFunctions_nullValue() {
        ToIntFunction<String> hashFunc = s -> s == null ? 0 : s.hashCode();
        BiPredicate<String, String> equalsFunc = (s1, s2) -> {
            if (s1 == null && s2 == null)
                return true;
            if (s1 == null || s2 == null)
                return false;
            return s1.equals(s2);
        };

        Wrapper<String> wrapper = Wrapper.of(null, hashFunc, equalsFunc);
        assertNotNull(wrapper);
        assertEquals(null, wrapper.value());
        assertEquals(0, wrapper.hashCode());
    }

    @Test
    public void test_of_withToString_nullHashFunction() {
        assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", null, (s1, s2) -> s1.equals(s2), String::toUpperCase);
        });
    }

    @Test
    public void test_of_withToString_nullEqualsFunction() {
        assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", String::hashCode, null, String::toUpperCase);
        });
    }

    @Test
    public void test_of_withToString_nullToStringFunction() {
        assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", String::hashCode, String::equals, null);
        });
    }

    @Test
    public void test_of_withToString() {
        String value = "test";
        ToIntFunction<String> hashFunc = String::hashCode;
        BiPredicate<String, String> equalsFunc = String::equals;
        Function<String, String> toStringFunc = s -> "CUSTOM[" + s + "]";

        Wrapper<String> wrapper = Wrapper.of(value, hashFunc, equalsFunc, toStringFunc);

        assertEquals("test", wrapper.value());
        assertEquals("Wrapper[CUSTOM[test]]", wrapper.toString());
    }

    @Test
    public void test_of_withToString_nullValue() {
        ToIntFunction<String> hashFunc = s -> s == null ? 0 : s.hashCode();
        BiPredicate<String, String> equalsFunc = (s1, s2) -> {
            if (s1 == null && s2 == null)
                return true;
            if (s1 == null || s2 == null)
                return false;
            return s1.equals(s2);
        };
        Function<String, String> toStringFunc = s -> s == null ? "NULL" : s;

        Wrapper<String> wrapper = Wrapper.of(null, hashFunc, equalsFunc, toStringFunc);
        assertEquals("Wrapper[NULL]", wrapper.toString());
    }

    @Test
    public void test_value() {
        int[] array = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(array);

        assertSame(array, wrapper.value());
    }

    @Test
    public void test_value_withCustomWrapper() {
        String str = "hello";
        Wrapper<String> wrapper = Wrapper.of(str, String::hashCode, String::equals);

        assertSame(str, wrapper.value());
    }

    @Test
    public void test_value_null() {
        Wrapper<int[]> wrapper = Wrapper.of(null);
        assertEquals(null, wrapper.value());
    }

    @Test
    public void test_hashCode_arrays() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 3, 2, 1 };

        Wrapper<int[]> wrapper1 = Wrapper.of(arr1);
        Wrapper<int[]> wrapper2 = Wrapper.of(arr2);
        Wrapper<int[]> wrapper3 = Wrapper.of(arr3);

        assertEquals(wrapper1.hashCode(), wrapper2.hashCode());

        assertNotEquals(0, wrapper1.hashCode());
    }

    @Test
    public void test_hashCode_customFunction() {
        ToIntFunction<String> hashFunc = s -> 42;
        BiPredicate<String, String> equalsFunc = String::equals;

        Wrapper<String> wrapper1 = Wrapper.of("hello", hashFunc, equalsFunc);
        Wrapper<String> wrapper2 = Wrapper.of("world", hashFunc, equalsFunc);

        assertEquals(42, wrapper1.hashCode());
        assertEquals(42, wrapper2.hashCode());
    }

    @Test
    public void test_hashCode_consistency() {
        int[] arr = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(arr);

        int hash1 = wrapper.hashCode();
        int hash2 = wrapper.hashCode();

        assertEquals(hash1, hash2);
    }

    @Test
    public void test_equals_sameInstance() {
        int[] arr = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(arr);

        assertTrue(wrapper.equals(wrapper));
    }

    @Test
    public void test_equals_nullObject() {
        int[] arr = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(arr);

        assertFalse(wrapper.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        int[] arr = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(arr);

        assertFalse(wrapper.equals("not a wrapper"));
    }

    @Test
    public void test_equals_arrays() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 4 };

        Wrapper<int[]> wrapper1 = Wrapper.of(arr1);
        Wrapper<int[]> wrapper2 = Wrapper.of(arr2);
        Wrapper<int[]> wrapper3 = Wrapper.of(arr3);

        assertTrue(wrapper1.equals(wrapper2));
        assertFalse(wrapper1.equals(wrapper3));
    }

    @Test
    public void test_equals_customFunction() {
        BiPredicate<String, String> equalsFunc = (s1, s2) -> s1.length() == s2.length();
        ToIntFunction<String> hashFunc = String::length;

        Wrapper<String> wrapper1 = Wrapper.of("abc", hashFunc, equalsFunc);
        Wrapper<String> wrapper2 = Wrapper.of("xyz", hashFunc, equalsFunc);
        Wrapper<String> wrapper3 = Wrapper.of("abcd", hashFunc, equalsFunc);

        assertTrue(wrapper1.equals(wrapper2));
        assertFalse(wrapper1.equals(wrapper3));
    }

    @Test
    public void test_equals_symmetry() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };

        Wrapper<int[]> wrapper1 = Wrapper.of(arr1);
        Wrapper<int[]> wrapper2 = Wrapper.of(arr2);

        assertTrue(wrapper1.equals(wrapper2));
        assertTrue(wrapper2.equals(wrapper1));
    }

    @Test
    public void test_equals_transitivity() {
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        int[] arr3 = { 1, 2, 3 };

        Wrapper<int[]> wrapper1 = Wrapper.of(arr1);
        Wrapper<int[]> wrapper2 = Wrapper.of(arr2);
        Wrapper<int[]> wrapper3 = Wrapper.of(arr3);

        assertTrue(wrapper1.equals(wrapper2));
        assertTrue(wrapper2.equals(wrapper3));
        assertTrue(wrapper1.equals(wrapper3));
    }

    @Test
    public void test_toString_arrayWrapper() {
        int[] arr = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(arr);

        String str = wrapper.toString();
        assertTrue(str.startsWith("Wrapper["));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void test_toString_customWrapper() {
        Function<String, String> toStringFunc = s -> "***" + s + "***";
        Wrapper<String> wrapper = Wrapper.of("hello", String::hashCode, String::equals, toStringFunc);

        assertEquals("Wrapper[***hello***]", wrapper.toString());
    }

    @Test
    public void test_integration_hashMap() {
        Map<Wrapper<int[]>, String> map = new HashMap<>();

        int[] key1 = { 1, 2, 3 };
        int[] key2 = { 1, 2, 3 };
        int[] key3 = { 4, 5, 6 };

        map.put(Wrapper.of(key1), "value1");
        map.put(Wrapper.of(key3), "value3");

        assertEquals("value1", map.get(Wrapper.of(key2)));
        assertEquals("value3", map.get(Wrapper.of(key3)));
        assertEquals(null, map.get(Wrapper.of(new int[] { 9, 9, 9 })));
    }

    @Test
    public void test_integration_hashSet() {
        Set<Wrapper<String[]>> set = new HashSet<>();

        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        String[] arr3 = { "x", "y", "z" };

        set.add(Wrapper.of(arr1));
        set.add(Wrapper.of(arr2));
        set.add(Wrapper.of(arr3));

        assertEquals(2, set.size());
        assertTrue(set.contains(Wrapper.of(new String[] { "a", "b", "c" })));
        assertTrue(set.contains(Wrapper.of(arr3)));
    }

    @Test
    public void test_differentPrimitiveTypes() {
        byte[] byteArr1 = { 1, 2, 3 };
        byte[] byteArr2 = { 1, 2, 3 };
        assertEquals(Wrapper.of(byteArr1), Wrapper.of(byteArr2));

        short[] shortArr1 = { 1, 2, 3 };
        short[] shortArr2 = { 1, 2, 3 };
        assertEquals(Wrapper.of(shortArr1), Wrapper.of(shortArr2));

        long[] longArr1 = { 1L, 2L, 3L };
        long[] longArr2 = { 1L, 2L, 3L };
        assertEquals(Wrapper.of(longArr1), Wrapper.of(longArr2));

        float[] floatArr1 = { 1.0f, 2.0f, 3.0f };
        float[] floatArr2 = { 1.0f, 2.0f, 3.0f };
        assertEquals(Wrapper.of(floatArr1), Wrapper.of(floatArr2));

        double[] doubleArr1 = { 1.0, 2.0, 3.0 };
        double[] doubleArr2 = { 1.0, 2.0, 3.0 };
        assertEquals(Wrapper.of(doubleArr1), Wrapper.of(doubleArr2));

        char[] charArr1 = { 'a', 'b', 'c' };
        char[] charArr2 = { 'a', 'b', 'c' };
        assertEquals(Wrapper.of(charArr1), Wrapper.of(charArr2));

        boolean[] boolArr1 = { true, false, true };
        boolean[] boolArr2 = { true, false, true };
        assertEquals(Wrapper.of(boolArr1), Wrapper.of(boolArr2));
    }

    @Test
    public void test_wrapperPool_differentTypes() {
        int[] intArr = new int[0];
        long[] longArr = new long[0];
        String[] strArr = new String[0];

        Wrapper<int[]> intWrapper = Wrapper.of(intArr);
        Wrapper<long[]> longWrapper = Wrapper.of(longArr);
        Wrapper<String[]> strWrapper = Wrapper.of(strArr);

        assertNotEquals(intWrapper, longWrapper);
        assertNotEquals(intWrapper, strWrapper);
        assertNotEquals(longWrapper, strWrapper);
    }

    @Test
    public void test_nestedArrays() {
        int[][][] arr1 = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        int[][][] arr2 = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };
        int[][][] arr3 = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 9 } } };

        Wrapper<int[][][]> wrapper1 = Wrapper.of(arr1);
        Wrapper<int[][][]> wrapper2 = Wrapper.of(arr2);
        Wrapper<int[][][]> wrapper3 = Wrapper.of(arr3);

        assertEquals(wrapper1, wrapper2);
        assertNotEquals(wrapper1, wrapper3);
    }
}
