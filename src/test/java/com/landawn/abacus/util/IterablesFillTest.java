package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Supplier;

public class IterablesFillTest extends IterablesTestSupport {
    @Test
    public void testFillArray() {
        String[] arr = new String[5];
        Supplier<String> supplier = () -> "test";

        Iterables.fill(arr, supplier);

        for (String s : arr) {
            assertEquals("test", s);
        }
    }

    @Test
    public void testFillListWithRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Supplier<String> supplier = () -> "X";

        Iterables.fill(list, 1, 4, supplier);

        assertEquals("a", list.get(0));
        assertEquals("X", list.get(1));
        assertEquals("X", list.get(2));
        assertEquals("X", list.get(3));
        assertEquals("e", list.get(4));
    }

    @Test
    public void testFillArrayRangeWithSupplier() {
        String[] array = new String[5];
        Arrays.fill(array, "initial");
        Supplier<String> supplier = () -> "test";

        Iterables.fill(array, 1, 4, supplier);

        assertArrayEquals(new String[] { "initial", "test", "test", "test", "initial" }, array);
    }

    @Test
    public void testFillListRangeWithSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Supplier<String> supplier = () -> "test";

        Iterables.fill(list, 1, 4, supplier);

        assertEquals(Arrays.asList("a", "test", "test", "test", "e"), list);
    }

    @Test
    public void testFillListExtension() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Supplier<String> supplier = () -> "test";

        Iterables.fill(list, 0, 5, supplier);

        assertEquals(5, list.size());
        assertEquals(Arrays.asList("test", "test", "test", "test", "test"), list);
    }

    // ===================== fill Array/List overloads =====================

    @Test
    public void testFillArray_Dedicated() {
        String[] arr = new String[3];
        Iterables.fill(arr, () -> "x");
        assertArrayEquals(new String[] { "x", "x", "x" }, arr);
    }

    @Test
    public void testFillList_Dedicated() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterables.fill(list, () -> "z");
        assertEquals(Arrays.asList("z", "z", "z"), list);
    }

    @Test
    public void testFillListRange_Dedicated() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        Iterables.fill(list, 1, 3, () -> "z");
        assertEquals(Arrays.asList("a", "z", "z", "d"), list);
    }

    @Test
    public void testFill_ListWithSupplier_ExtendsExistingList() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        Supplier<Integer> supplier = () -> 99;
        Iterables.fill(list, 1, 5, supplier);
        assertEquals(5, list.size());
        assertEquals(Integer.valueOf(1), list.get(0));
        assertEquals(Integer.valueOf(99), list.get(1));
        assertEquals(Integer.valueOf(99), list.get(4));
    }

    @Test
    public void testFill_ListWithSupplier_ExistingRange() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Supplier<Integer> supplier = () -> 0;
        Iterables.fill(list, 1, 3, supplier);
        assertEquals(5, list.size());
        assertEquals(Integer.valueOf(1), list.get(0));
        assertEquals(Integer.valueOf(0), list.get(1));
        assertEquals(Integer.valueOf(0), list.get(2));
        assertEquals(Integer.valueOf(4), list.get(3));
    }

    @Test
    public void testFillArrayWithRange() {
        String[] arr = new String[5];
        Supplier<String> supplier = () -> "test";

        Iterables.fill(arr, 1, 4, supplier);

        assertNull(arr[0]);
        assertEquals("test", arr[1]);
        assertEquals("test", arr[2]);
        assertEquals("test", arr[3]);
        assertNull(arr[4]);
    }

    @Test
    public void testFillList() {
        List<String> list = new ArrayList<>(Arrays.asList(null, null, null));
        Supplier<String> supplier = () -> "filled";

        Iterables.fill(list, supplier);

        for (String s : list) {
            assertEquals("filled", s);
        }
    }

    @Test
    public void testFillArrayWithSupplier() {
        Iterables.fill((String[]) null, () -> "a");

        String[] arrEmpty = new String[0];
        Iterables.fill(arrEmpty, () -> "a");
        assertEquals(0, arrEmpty.length);

        String[] arr = new String[3];
        Iterables.fill(arr, () -> "filled");
        assertArrayEquals(new String[] { "filled", "filled", "filled" }, arr);

        final int[] counter = { 0 };
        Supplier<Integer> supplier = () -> counter[0]++;
        Integer[] arrInt = new Integer[3];
        Iterables.fill(arrInt, supplier);
        assertArrayEquals(new Integer[] { 0, 1, 2 }, arrInt);
    }

    @Test
    public void testFillArraySupplier() {
        String[] arr = new String[3];
        Supplier<String> supplier = () -> "test";
        Iterables.fill(arr, supplier);
        assertArrayEquals(new String[] { "test", "test", "test" }, arr);

        Iterables.fill((String[]) null, supplier);
        Iterables.fill(new String[0], supplier);
    }

    @Test
    public void testFillArrayRange_Dedicated() {
        String[] arr = new String[5];
        Iterables.fill(arr, 1, 4, () -> "y");
        assertNull(arr[0]);
        assertEquals("y", arr[1]);
        assertEquals("y", arr[2]);
        assertEquals("y", arr[3]);
        assertNull(arr[4]);
    }

    @Test
    public void testFill_ListWithSupplier_EmptyAndGap() {
        List<Integer> list = new ArrayList<>();
        Supplier<Integer> supplier = () -> 7;
        // fromIndex > size: the gap [size, fromIndex) is padded with null (aligned with N.fill),
        // only [fromIndex, toIndex) is populated from the supplier.
        Iterables.fill(list, 2, 4, supplier);
        assertEquals(4, list.size());
        assertNull(list.get(0));
        assertNull(list.get(1));
        assertEquals(Integer.valueOf(7), list.get(2));
        assertEquals(Integer.valueOf(7), list.get(3));
    }

    @Test
    public void testFillArrayFromToWithSupplier() {
        Iterables.fill((String[]) null, 0, 0, () -> "a");

        String[] arr = new String[5];
        Arrays.fill(arr, "original");
        Iterables.fill(arr, 1, 4, () -> "filled");
        assertArrayEquals(new String[] { "original", "filled", "filled", "filled", "original" }, arr);

        Iterables.fill(arr, 1, 1, () -> "no-fill");
        assertArrayEquals(new String[] { "original", "filled", "filled", "filled", "original" }, arr);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, -1, 2, () -> "fail"));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, 0, 6, () -> "fail"));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, 3, 2, () -> "fail"));
    }

    @Test
    public void testFillListWithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, () -> "a"));

        List<String> listEmpty = new ArrayList<>();
        Iterables.fill(listEmpty, () -> "a");
        assertTrue(listEmpty.isEmpty());

        List<String> list = new ArrayList<>(Arrays.asList("x", "y", "z"));
        Iterables.fill(list, () -> "filled");
        assertEquals(Arrays.asList("filled", "filled", "filled"), list);

        final int[] counter = { 0 };
        Supplier<Integer> supplier = () -> counter[0]++;
        List<Integer> listInt = new ArrayList<>(Arrays.asList(0, 0, 0));
        Iterables.fill(listInt, supplier);
        assertEquals(Arrays.asList(0, 1, 2), listInt);
    }

    @Test
    public void testFillListFromToWithSupplier() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, 0, 0, () -> "a"));

        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        Iterables.fill(list, 1, 4, () -> "filled");
        assertEquals(Arrays.asList("a", "filled", "filled", "filled", "e"), list);

        List<String> shortList = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList, 0, 3, () -> "new");
        assertEquals(Arrays.asList("new", "new", "new"), shortList);
        assertEquals(3, shortList.size());

        List<String> listToExtend = new ArrayList<>(Arrays.asList("a", "b"));
        Iterables.fill(listToExtend, 1, 4, () -> "Z");
        assertEquals(Arrays.asList("a", "Z", "Z", "Z"), listToExtend);

        List<Integer> listInt = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        final int[] counter = { 10 };
        Supplier<Integer> supplier = () -> counter[0]++;
        Iterables.fill(listInt, 2, 5, supplier);
        assertEquals(Arrays.asList(1, 2, 10, 11, 12), listInt);

        List<Integer> listFillAndExtend = new ArrayList<>(Arrays.asList(1, 2));
        final int[] counter2 = { 100 };
        Supplier<Integer> supplier2 = () -> counter2[0]++;
        Iterables.fill(listFillAndExtend, 1, 4, supplier2);
        assertEquals(Arrays.asList(1, 100, 101, 102), listFillAndExtend);

        List<Integer> listFillFromSize = new ArrayList<>(Arrays.asList(1, 2));
        final int[] counter3 = { 200 };
        Supplier<Integer> supplier3 = () -> counter3[0]++;
        Iterables.fill(listFillFromSize, 2, 4, supplier3);
        assertEquals(Arrays.asList(1, 2, 200, 201), listFillFromSize);

        List<Integer> listFillFromSize2 = new ArrayList<>(Arrays.asList(1, 2));
        final int[] counter4 = { 300 };
        Supplier<Integer> supplier4 = () -> counter4[0]++;
        Iterables.fill(listFillFromSize2, 3, 5, supplier4);
        // Gap [size, fromIndex) is padded with null (aligned with N.fill); only [fromIndex, toIndex) uses the supplier.
        assertEquals(Arrays.asList(1, 2, null, 300, 301), listFillFromSize2);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, -1, 2, () -> "fail"));
        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, 3, 2, () -> "fail"));

    }

    @Test
    public void testFillArrayRangeSupplier() {
        String[] arr = { "a", "b", "c", "d" };
        Supplier<String> supplier = () -> "x";
        Iterables.fill(arr, 1, 3, supplier);
        assertArrayEquals(new String[] { "a", "x", "x", "d" }, arr);

        Iterables.fill(arr, 1, 1, supplier);
        assertArrayEquals(new String[] { "a", "x", "x", "d" }, arr);

        assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(arr, 0, 5, supplier));
    }

    @Test
    public void testFillListSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Supplier<String> supplier = () -> "test";
        Iterables.fill(list, supplier);
        assertEquals(list("test", "test", "test"), list);

        List<String> emptyList = new ArrayList<>();
        Iterables.fill(emptyList, supplier);
        assertTrue(emptyList.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, supplier));
    }

    @Test
    public void testFillListRangeSupplier() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        Supplier<String> supplier = () -> "x";
        Iterables.fill(list, 1, 3, supplier);
        assertEquals(list("a", "x", "x", "d"), list);

        List<String> shortList = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList, 0, 3, supplier);
        assertEquals(list("x", "x", "x"), shortList);

        List<String> shortList2 = new ArrayList<>(Arrays.asList("a"));
        Iterables.fill(shortList2, 2, 4, supplier);
        // Gap [size, fromIndex) is padded with null (aligned with N.fill); only [fromIndex, toIndex) uses the supplier.
        assertEquals(Arrays.asList("a", null, "x", "x"), shortList2);
        Iterables.fill(list, 0, 5, supplier);
        assertEquals(list("x", "x", "x", "x", "x"), list);

        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, 0, 1, supplier));
    }

    @Test
    public void testFillNullArray() {
        assertDoesNotThrow(() -> Iterables.fill((String[]) null, () -> "test"));
    }

    @Test
    public void testFillNullList() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<String>) null, () -> "test"));
    }

    // ===================== fill edge cases =====================

    @Test
    public void testFillArrayRange_NullArray() {
        assertDoesNotThrow(() -> Iterables.fill((String[]) null, 0, 0, () -> "x"));
    }

    @Test
    public void testFill_ListWithSupplier_NullList_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> Iterables.fill((List<Integer>) null, 0, 2, () -> 1));
    }

    @Test
    public void testFill_NullSupplierNotEvaluatedForEmptyTargetsAndRanges() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.fill(new String[0], (Supplier<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.fill(new String[0], 0, 0, (Supplier<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.fill(new ArrayList<String>(), (Supplier<String>) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Iterables.fill(new ArrayList<String>(), 0, 0, (Supplier<String>) null));
    }

    @Test
    public void testFillList_invalidRange_messageDoesNotLeakIntegerMaxValue() {
        final List<String> list = new ArrayList<>(Arrays.asList("a", "b"));

        final IndexOutOfBoundsException negative = assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, -1, 2, () -> "z"));
        assertEquals("Index range [-1, 2) is invalid: expected 0 <= fromIndex <= toIndex", negative.getMessage());
        assertFalse(negative.getMessage().contains(String.valueOf(Integer.MAX_VALUE)));

        final IndexOutOfBoundsException inverted = assertThrows(IndexOutOfBoundsException.class, () -> Iterables.fill(list, 3, 1, () -> "z"));
        assertEquals("Index range [3, 1) is invalid: expected 0 <= fromIndex <= toIndex", inverted.getMessage());

        assertEquals(Arrays.asList("a", "b"), list, "the list must be untouched when the range is rejected");
    }

    @Test
    public void testFillList_toIndexBeyondSizeIsStillAllowed() {
        final List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        Iterables.fill(list, 0, 4, () -> "z");
        assertEquals(Arrays.asList("z", "z", "z", "z"), list);
    }
}
