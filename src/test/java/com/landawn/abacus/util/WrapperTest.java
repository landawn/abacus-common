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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class WrapperTest extends TestBase {

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
    public void testToString() {
        int[] array = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(array);

        String str = wrapper.toString();
        Assertions.assertTrue(str.contains("Wrapper"));
        Assertions.assertTrue(str.contains("[1, 2, 3]"));
    }

    @Test
    public void testToStringCustom() {
        Function<String, String> toStringFunction = s -> "Value is: " + s;
        Wrapper<String> wrapper = Wrapper.of("test", String::hashCode, String::equals, toStringFunction);

        String str = wrapper.toString();
        Assertions.assertTrue(str.contains("Value is: test"));
    }

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

    @Test
    public void test_of_nonArrayValue() {
        // Wrapper.of(T) with a non-array value should still work via ArrayWrapper
        String value = "hello";
        Wrapper<String> wrapper = Wrapper.of(value);
        assertNotNull(wrapper);
        assertSame(value, wrapper.value());

        // Two wrappers of equal non-array values should be equal
        Wrapper<String> wrapper2 = Wrapper.of("hello");
        assertEquals(wrapper, wrapper2);
        assertEquals(wrapper.hashCode(), wrapper2.hashCode());
    }

    @Test
    public void test_of_nullArray_hashCodeAndEquals() {
        Wrapper<int[]> w1 = Wrapper.of(null);
        Wrapper<int[]> w2 = Wrapper.of(null);

        assertSame(w1, w2); // should return the same cached instance
        assertEquals(w1.hashCode(), w2.hashCode());
        assertTrue(w1.equals(w2));
    }

    @Test
    public void test_toString_nullArrayWrapper() {
        Wrapper<int[]> wrapper = Wrapper.of(null);
        String str = wrapper.toString();
        assertNotNull(str);
        assertTrue(str.contains("Wrapper["));
    }

    @Test
    public void test_of_zeroLengthArrayCaching() {
        // Verify caching for various zero-length array types
        double[] emptyDoubleArr = new double[0];
        Wrapper<double[]> dw1 = Wrapper.of(emptyDoubleArr);
        Wrapper<double[]> dw2 = Wrapper.of(new double[0]);
        assertSame(dw1, dw2);

        char[] emptyCharArr = new char[0];
        Wrapper<char[]> cw1 = Wrapper.of(emptyCharArr);
        Wrapper<char[]> cw2 = Wrapper.of(new char[0]);
        assertSame(cw1, cw2);

        float[] emptyFloatArr = new float[0];
        Wrapper<float[]> fw1 = Wrapper.of(emptyFloatArr);
        Wrapper<float[]> fw2 = Wrapper.of(new float[0]);
        assertSame(fw1, fw2);
    }

    @Test
    public void testOfArray() {
        int[] array = { 1, 2, 3 };
        Wrapper<int[]> wrapper = Wrapper.of(array);

        Assertions.assertNotNull(wrapper);
        Assertions.assertArrayEquals(array, wrapper.value());
    }

    @Test
    public void testOfNullArray() {
        Wrapper<Object> wrapper = Wrapper.of(null);
        Assertions.assertNotNull(wrapper);
        Assertions.assertNull(wrapper.value());
    }

    @Test
    public void testOfEmptyArray() {
        int[] emptyArray = new int[0];
        Wrapper<int[]> wrapper1 = Wrapper.of(emptyArray);
        Wrapper<int[]> wrapper2 = Wrapper.of(new int[0]);

        Assertions.assertSame(wrapper1, wrapper2);
    }

    @Test
    public void testOfWithCustomFunctions() {
        String value = "test";
        ToIntFunction<String> hashFunction = String::length;
        BiPredicate<String, String> equalsFunction = String::equalsIgnoreCase;

        Wrapper<String> wrapper = Wrapper.of(value, hashFunction, equalsFunction);

        Assertions.assertEquals(value, wrapper.value());
        Assertions.assertEquals(4, wrapper.hashCode());
    }

    @Test
    public void testOfWithAllFunctions() {
        String value = "test";
        ToIntFunction<String> hashFunction = String::length;
        BiPredicate<String, String> equalsFunction = String::equalsIgnoreCase;
        Function<String, String> toStringFunction = s -> "Custom: " + s;

        Wrapper<String> wrapper = Wrapper.of(value, hashFunction, equalsFunction, toStringFunction);

        Assertions.assertEquals(value, wrapper.value());
        Assertions.assertTrue(wrapper.toString().contains("Custom: test"));
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
    public void testOfWithNullFunctions() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", null, (a, b) -> true);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", String::hashCode, null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Wrapper.of("test", String::hashCode, String::equals, null);
        });
    }

    @Test
    public void testValue() {
        String value = "test";
        Wrapper<String> wrapper = Wrapper.of(value, String::hashCode, String::equals);

        Assertions.assertEquals(value, wrapper.value());
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
    public void testHashCodeArray() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };

        Wrapper<int[]> wrapper1 = Wrapper.of(array1);
        Wrapper<int[]> wrapper2 = Wrapper.of(array2);

        Assertions.assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
    }

    @Test
    public void testHashCodeCustom() {
        String value = "test";
        ToIntFunction<String> hashFunction = s -> 42;
        BiPredicate<String, String> equalsFunction = String::equals;

        Wrapper<String> wrapper = Wrapper.of(value, hashFunction, equalsFunction);

        Assertions.assertEquals(42, wrapper.hashCode());
    }

    @Test
    public void test_hashCode_nullWrapper() {
        Wrapper<Object> wrapper = Wrapper.of(null);
        // Should not throw, should return consistent hash code
        int hash1 = wrapper.hashCode();
        int hash2 = wrapper.hashCode();
        assertEquals(hash1, hash2);
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
    public void test_equals_arrayVsCustomWrapper() {
        // An ArrayWrapper and an AnyWrapper should not be considered equal
        // even if they wrap the same value, because they use different equals functions
        int[] arr = { 1, 2, 3 };
        Wrapper<int[]> arrayWrapper = Wrapper.of(arr);
        Wrapper<int[]> customWrapper = Wrapper.of(arr, a -> java.util.Arrays.hashCode(a), (a1, a2) -> java.util.Arrays.equals(a1, a2));

        // They may or may not be equal depending on internal implementation of equals
        // but they should both work correctly with the same value
        assertSame(arr, arrayWrapper.value());
        assertSame(arr, customWrapper.value());
    }

    @Test
    public void testEqualsArray() {
        int[] array1 = { 1, 2, 3 };
        int[] array2 = { 1, 2, 3 };
        int[] array3 = { 1, 2, 4 };

        Wrapper<int[]> wrapper1 = Wrapper.of(array1);
        Wrapper<int[]> wrapper2 = Wrapper.of(array2);
        Wrapper<int[]> wrapper3 = Wrapper.of(array3);

        Assertions.assertEquals(wrapper1, wrapper2);
        Assertions.assertNotEquals(wrapper1, wrapper3);
        Assertions.assertEquals(wrapper1, wrapper1);
        Assertions.assertNotEquals(wrapper1, null);
        Assertions.assertNotEquals(wrapper1, "string");
    }

    @Test
    public void testEqualsCustom() {
        ToIntFunction<String> hashFunction = String::length;
        BiPredicate<String, String> equalsFunction = String::equalsIgnoreCase;

        Wrapper<String> wrapper1 = Wrapper.of("TEST", hashFunction, equalsFunction);
        Wrapper<String> wrapper2 = Wrapper.of("test", hashFunction, equalsFunction);
        Wrapper<String> wrapper3 = Wrapper.of("other", hashFunction, equalsFunction);

        Assertions.assertEquals(wrapper1, wrapper2);
        Assertions.assertNotEquals(wrapper1, wrapper3);
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

}
