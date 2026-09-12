package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CommonUtilIsTest extends CommonUtilTestSupport {

    @Test
    public void testIsEmpty() {
        assertTrue(CommonUtil.isEmpty((String) null));
        assertTrue(CommonUtil.isEmpty(""));
        assertFalse(CommonUtil.isEmpty("a"));
        assertFalse(CommonUtil.isEmpty(" "));

        assertTrue(CommonUtil.isEmpty((boolean[]) null));
        assertTrue(CommonUtil.isEmpty(new boolean[0]));
        assertFalse(CommonUtil.isEmpty(new boolean[] { true }));
        assertTrue(CommonUtil.isEmpty((char[]) null));
        assertTrue(CommonUtil.isEmpty(new char[0]));
        assertFalse(CommonUtil.isEmpty(new char[] { 'a' }));
        assertTrue(CommonUtil.isEmpty((byte[]) null));
        assertTrue(CommonUtil.isEmpty(new byte[0]));
        assertFalse(CommonUtil.isEmpty(new byte[] { 1 }));
        assertTrue(CommonUtil.isEmpty((short[]) null));
        assertTrue(CommonUtil.isEmpty(new short[0]));
        assertFalse(CommonUtil.isEmpty(new short[] { 1 }));
        assertTrue(CommonUtil.isEmpty((int[]) null));
        assertTrue(CommonUtil.isEmpty(new int[0]));
        assertFalse(CommonUtil.isEmpty(new int[] { 1 }));
        assertTrue(CommonUtil.isEmpty((long[]) null));
        assertTrue(CommonUtil.isEmpty(new long[0]));
        assertFalse(CommonUtil.isEmpty(new long[] { 1L }));
        assertTrue(CommonUtil.isEmpty((float[]) null));
        assertTrue(CommonUtil.isEmpty(new float[0]));
        assertFalse(CommonUtil.isEmpty(new float[] { 1.0f }));
        assertTrue(CommonUtil.isEmpty((double[]) null));
        assertTrue(CommonUtil.isEmpty(new double[0]));
        assertFalse(CommonUtil.isEmpty(new double[] { 1.0 }));
        assertTrue(CommonUtil.isEmpty((Object[]) null));
        assertTrue(CommonUtil.isEmpty(new Object[0]));
        assertFalse(CommonUtil.isEmpty(new Object[] { "a" }));

        assertTrue(CommonUtil.isEmpty((Collection<?>) null));
        assertTrue(CommonUtil.isEmpty(new ArrayList<>()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList("a")));
        assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        assertTrue(CommonUtil.isEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(CommonUtil.isEmpty(map));

        assertTrue(CommonUtil.isEmpty((Iterator<?>) null));
        assertTrue(CommonUtil.isEmpty(Collections.emptyIterator()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1, 2).iterator()));
        assertTrue(CommonUtil.isEmpty((PrimitiveList) null));
        assertTrue(CommonUtil.isEmpty((Multiset<?>) null));
        assertTrue(CommonUtil.isEmpty((Multimap<?, ?, ?>) null));
        assertTrue(CommonUtil.isEmpty((Dataset) null));
    }

    @Test
    public void testIsBlank() {
        assertTrue(CommonUtil.isBlank(null));
        assertTrue(CommonUtil.isBlank(""));
        assertTrue(CommonUtil.isBlank(" "));
        assertTrue(CommonUtil.isBlank("   "));
        assertTrue(CommonUtil.isBlank("\t"));
        assertTrue(CommonUtil.isBlank("\n"));
        assertFalse(CommonUtil.isBlank("a"));
        assertFalse(CommonUtil.isBlank(" a "));
    }

    @Test
    public void testIsTrueFalse() {
        assertTrue(CommonUtil.isTrue(true));
        assertFalse(CommonUtil.isTrue(false));
        assertFalse(CommonUtil.isTrue(null));
        assertTrue(CommonUtil.isNotTrue(false));
        assertTrue(CommonUtil.isNotTrue(null));
        assertFalse(CommonUtil.isNotTrue(true));
        assertTrue(CommonUtil.isFalse(false));
        assertFalse(CommonUtil.isFalse(true));
        assertFalse(CommonUtil.isFalse(null));
        assertTrue(CommonUtil.isNotFalse(true));
        assertTrue(CommonUtil.isNotFalse(null));
        assertFalse(CommonUtil.isNotFalse(false));
    }

    @Test
    public void testIsBuiltinClass() {
        assertTrue(CommonUtil.isBuiltinClass(int[].class));
        assertTrue(CommonUtil.isBuiltinClass(int[][].class));
        assertTrue(CommonUtil.isBuiltinClass(String[].class));
        assertTrue(CommonUtil.isBuiltinClass(String[][].class));
        assertTrue(CommonUtil.isBuiltinClass(ArrayList.class));
        assertFalse(CommonUtil.isBuiltinClass(org.junit.jupiter.api.Test.class));
    }

    @Test
    public void testIsBetween() {
        assertTrue(CommonUtil.isBetween(5, 3, 7));
        assertTrue(CommonUtil.isBetween(3, 3, 7));
        assertTrue(CommonUtil.isBetween(7, 3, 7));
        assertFalse(CommonUtil.isBetween(2, 3, 7));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.isBetween(5, 1, 10, null));
        assertTrue(CommonUtil.isBetween(5, 10, 1, (a, b) -> b.compareTo(a)));
    }

    @Test
    public void testIsSorted() {
        assertTrue(CommonUtil.isSorted((boolean[]) null));
        assertTrue(CommonUtil.isSorted(new boolean[0]));
        assertTrue(CommonUtil.isSorted(new boolean[] { false, false, true, true }));
        assertFalse(CommonUtil.isSorted(new boolean[] { true, false }));
        assertFalse(CommonUtil.isSorted(new boolean[] { false, true, false }));

        assertTrue(CommonUtil.isSorted((char[]) null));
        assertTrue(CommonUtil.isSorted(new char[] { 'a', 'a', 'b', 'c' }));
        assertFalse(CommonUtil.isSorted(new char[] { 'c', 'b', 'a' }));
        assertTrue(CommonUtil.isSorted((byte[]) null));
        assertTrue(CommonUtil.isSorted(new byte[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new byte[] { 3, 2, 1 }));
        assertTrue(CommonUtil.isSorted((short[]) null));
        assertTrue(CommonUtil.isSorted(new short[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new short[] { 3, 2, 1 }));
        assertTrue(CommonUtil.isSorted((int[]) null));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 1, 2, 3, 4 }));
        assertFalse(CommonUtil.isSorted(new int[] { 4, 3, 2, 1 }));
        assertTrue(CommonUtil.isSorted((long[]) null));
        assertTrue(CommonUtil.isSorted(new long[] { 1L, 2L, 3L }));
        assertFalse(CommonUtil.isSorted(new long[] { 3L, 2L, 1L }));
        assertTrue(CommonUtil.isSorted((float[]) null));
        assertTrue(CommonUtil.isSorted(new float[] { 1.0f, 2.0f, Float.NaN }));
        assertFalse(CommonUtil.isSorted(new float[] { 3.0f, 2.0f, 1.0f }));
        assertTrue(CommonUtil.isSorted((double[]) null));
        assertTrue(CommonUtil.isSorted(new double[] { 1.0, 2.0, Double.NaN }));
        assertFalse(CommonUtil.isSorted(new double[] { 3.0, 2.0, 1.0 }));

        assertTrue(CommonUtil.isSorted((String[]) null));
        assertTrue(CommonUtil.isSorted(new String[] { null, null, "a", "b" }));
        assertFalse(CommonUtil.isSorted(new String[] { "a", null, "b" }));
        assertFalse(CommonUtil.isSorted(new String[] { "c", "b", "a" }));
        String[] byLength = { "aaa", "bb", "c" };
        assertTrue(CommonUtil.isSorted(byLength, Comparator.naturalOrder()));
        assertTrue(CommonUtil.isSorted(byLength, Comparator.comparing(String::length).reversed()));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.isSorted(byLength, null));

        assertTrue(CommonUtil.isSorted((Collection<String>) null));
        assertTrue(CommonUtil.isSorted(new ArrayList<String>()));
        assertTrue(CommonUtil.isSorted(Arrays.asList(null, null, "a", "b")));
        assertFalse(CommonUtil.isSorted(Arrays.asList("c", "b", "a")));
        assertTrue(CommonUtil.isSorted(Arrays.asList("aaa", "bb", "c"), Comparator.comparing(String::length).reversed()));
    }

    @Test
    public void testIsSorted_range() {
        boolean[] bools = { true, false, false, true, true };
        assertTrue(CommonUtil.isSorted(bools, 1, 3));
        assertTrue(CommonUtil.isSorted(bools, 3, 5));
        assertFalse(CommonUtil.isSorted(bools, 0, 3));
        assertTrue(CommonUtil.isSorted(bools, 2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(bools, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(bools, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(bools, 3, 2));

        char[] chars = { 'd', 'a', 'b', 'c', 'e' };
        assertTrue(CommonUtil.isSorted(chars, 1, 4));
        assertFalse(CommonUtil.isSorted(chars, 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(chars, -1, 2));

        assertTrue(CommonUtil.isSorted(new byte[] { 5, 1, 2, 3, 0 }, 1, 4));
        assertFalse(CommonUtil.isSorted(new byte[] { 5, 1, 2, 3, 0 }, 0, 3));
        assertTrue(CommonUtil.isSorted(new short[] { 5, 1, 2, 3, 0 }, 1, 4));
        assertTrue(CommonUtil.isSorted(new int[] { 5, 1, 2, 3, 0 }, 1, 4));
        assertTrue(CommonUtil.isSorted(new long[] { 5L, 1L, 2L, 3L, 0L }, 1, 4));
        assertTrue(CommonUtil.isSorted(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 0.0f }, 1, 4));
        assertTrue(CommonUtil.isSorted(new double[] { 5.0, 1.0, 2.0, 3.0, 0.0 }, 1, 4));

        String[] strings = { "d", "a", "b", "c", "e" };
        assertTrue(CommonUtil.isSorted(strings, 1, 4));
        assertFalse(CommonUtil.isSorted(strings, 0, 3));
        String[] mixed = { "d", "aaa", "bb", "c", "e" };
        assertTrue(CommonUtil.isSorted(mixed, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(CommonUtil.isSorted(mixed, 0, 3, Comparator.naturalOrder()));

        List<String> list = Arrays.asList("d", "a", "b", "c", "e");
        assertTrue(CommonUtil.isSorted(list, 1, 4));
        assertFalse(CommonUtil.isSorted(list, 0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(list, 0, 6));
        List<String> byLength = Arrays.asList("d", "aaa", "bb", "c", "e");
        assertTrue(CommonUtil.isSorted(byLength, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(CommonUtil.isSorted(byLength, 0, 3, Comparator.naturalOrder()));
    }
}
