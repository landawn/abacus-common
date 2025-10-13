package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

@Tag("2025")
public class IntList2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        IntList list = new IntList();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity() {
        IntList list = new IntList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity_zero() {
        IntList list = new IntList(0);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity_negative() {
        assertThrows(IllegalArgumentException.class, () -> new IntList(-1));
    }

    @Test
    public void test_constructor_withArray() {
        int[] arr = { 1, 2, 3, 4, 5 };
        IntList list = new IntList(arr);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_constructor_withArray_null() {
        assertThrows(NullPointerException.class, () -> new IntList((int[]) null));
    }

    @Test
    public void test_constructor_withArrayAndSize() {
        int[] arr = { 1, 2, 3, 4, 5 };
        IntList list = new IntList(arr, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_constructor_withArrayAndSize_invalidSize() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(arr, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(arr, -1));
    }

    @Test
    public void test_of_varargs() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_of_varargs_empty() {
        IntList list = IntList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_of_varargs_null() {
        IntList list = IntList.of((int[]) null);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_of_arrayWithSize() {
        int[] arr = { 1, 2, 3, 4, 5 };
        IntList list = IntList.of(arr, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_of_arrayWithSize_invalidSize() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(arr, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(arr, -1));
    }

    @Test
    public void test_copyOf() {
        int[] arr = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(arr);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));

        arr[0] = 100;
        assertEquals(1, list.get(0));
    }

    @Test
    public void test_copyOf_null() {
        IntList list = IntList.copyOf((int[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void test_copyOf_withRange() {
        int[] arr = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2, list.get(0));
        assertEquals(4, list.get(2));
    }

    @Test
    public void test_copyOf_withRange_invalid() {
        int[] arr = { 1, 2, 3 };
        assertThrows(Exception.class, () -> IntList.copyOf(arr, 2, 1));
        assertThrows(Exception.class, () -> IntList.copyOf(arr, -1, 2));
    }

    @Test
    public void test_range() {
        IntList list = IntList.range(1, 5);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(3));
    }

    @Test
    public void test_range_empty() {
        IntList list = IntList.range(5, 5);
        assertEquals(0, list.size());
    }

    @Test
    public void test_range_withStep() {
        IntList list = IntList.range(0, 10, 2);
        assertEquals(5, list.size());
        assertEquals(0, list.get(0));
        assertEquals(8, list.get(4));
    }

    @Test
    public void test_range_withStep_negative() {
        IntList list = IntList.range(10, 0, -2);
        assertEquals(5, list.size());
        assertEquals(10, list.get(0));
        assertEquals(2, list.get(4));
    }

    @Test
    public void test_range_withStep_zero() {
        assertThrows(IllegalArgumentException.class, () -> IntList.range(0, 10, 0));
    }

    @Test
    public void test_rangeClosed() {
        IntList list = IntList.rangeClosed(1, 5);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_rangeClosed_single() {
        IntList list = IntList.rangeClosed(5, 5);
        assertEquals(1, list.size());
        assertEquals(5, list.get(0));
    }

    @Test
    public void test_rangeClosed_withStep() {
        IntList list = IntList.rangeClosed(0, 10, 2);
        assertEquals(6, list.size());
        assertEquals(0, list.get(0));
        assertEquals(10, list.get(5));
    }

    @Test
    public void test_rangeClosed_withStep_negative() {
        IntList list = IntList.rangeClosed(10, 0, -2);
        assertEquals(6, list.size());
        assertEquals(10, list.get(0));
        assertEquals(0, list.get(5));
    }

    @Test
    public void test_repeat() {
        IntList list = IntList.repeat(42, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(42, list.get(i));
        }
    }

    @Test
    public void test_repeat_zero() {
        IntList list = IntList.repeat(42, 0);
        assertEquals(0, list.size());
    }

    @Test
    public void test_random() {
        IntList list = IntList.random(10);
        assertEquals(10, list.size());
    }

    @Test
    public void test_random_zero() {
        IntList list = IntList.random(0);
        assertEquals(0, list.size());
    }

    @Test
    public void test_random_withRange() {
        IntList list = IntList.random(0, 100, 10);
        assertEquals(10, list.size());
        for (int i = 0; i < list.size(); i++) {
            int val = list.get(i);
            assertTrue(val >= 0 && val < 100);
        }
    }

    @Test
    public void test_random_withRange_invalid() {
        assertThrows(IllegalArgumentException.class, () -> IntList.random(100, 0, 10));
    }

    @Test
    public void test_array() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = list.array();
        assertNotNull(arr);
        assertTrue(arr.length >= 3);
    }

    @Test
    public void test_get() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(2));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_get_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(10));
    }

    @Test
    public void test_set() {
        IntList list = IntList.of(1, 2, 3);
        int oldValue = list.set(1, 42);
        assertEquals(2, oldValue);
        assertEquals(42, list.get(1));
    }

    @Test
    public void test_set_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 42));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 42));
    }

    @Test
    public void test_add() {
        IntList list = new IntList();
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_add_atIndex() {
        IntList list = IntList.of(1, 2, 4, 5);
        list.add(2, 3);
        assertEquals(5, list.size());
        assertEquals(3, list.get(2));
        assertEquals(4, list.get(3));
    }

    @Test
    public void test_add_atIndex_beginning() {
        IntList list = IntList.of(2, 3, 4);
        list.add(0, 1);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
    }

    @Test
    public void test_add_atIndex_end() {
        IntList list = IntList.of(1, 2, 3);
        list.add(3, 4);
        assertEquals(4, list.size());
        assertEquals(4, list.get(3));
    }

    @Test
    public void test_add_atIndex_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 42));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 42));
    }

    @Test
    public void test_addAll_IntList() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(4, 5, 6);
        assertTrue(list1.addAll(list2));
        assertEquals(6, list1.size());
        assertEquals(4, list1.get(3));
        assertEquals(6, list1.get(5));
    }

    @Test
    public void test_addAll_IntList_empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        assertFalse(list1.addAll(list2));
        assertEquals(3, list1.size());
    }

    @Test
    public void test_addAll_IntList_atIndex() {
        IntList list1 = IntList.of(1, 2, 5, 6);
        IntList list2 = IntList.of(3, 4);
        assertTrue(list1.addAll(2, list2));
        assertEquals(6, list1.size());
        assertEquals(3, list1.get(2));
        assertEquals(4, list1.get(3));
    }

    @Test
    public void test_addAll_IntList_atIndex_outOfBounds() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(4, 5);
        assertThrows(IndexOutOfBoundsException.class, () -> list1.addAll(-1, list2));
        assertThrows(IndexOutOfBoundsException.class, () -> list1.addAll(4, list2));
    }

    @Test
    public void test_addAll_array() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = { 4, 5, 6 };
        assertTrue(list.addAll(arr));
        assertEquals(6, list.size());
        assertEquals(4, list.get(3));
        assertEquals(6, list.get(5));
    }

    @Test
    public void test_addAll_array_empty() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = {};
        assertFalse(list.addAll(arr));
        assertEquals(3, list.size());
    }

    @Test
    public void test_addAll_array_atIndex() {
        IntList list = IntList.of(1, 2, 5, 6);
        int[] arr = { 3, 4 };
        assertTrue(list.addAll(2, arr));
        assertEquals(6, list.size());
        assertEquals(3, list.get(2));
        assertEquals(4, list.get(3));
    }

    @Test
    public void test_addAll_array_atIndex_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = { 4, 5 };
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, arr));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(4, arr));
    }

    @Test
    public void test_remove() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        assertTrue(list.remove(2));
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(2, list.get(2));
    }

    @Test
    public void test_remove_notFound() {
        IntList list = IntList.of(1, 2, 3);
        assertFalse(list.remove(42));
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeAllOccurrences() {
        IntList list = IntList.of(1, 2, 3, 2, 4, 2);
        assertTrue(list.removeAllOccurrences(2));
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(4, list.get(2));
    }

    @Test
    public void test_removeAllOccurrences_notFound() {
        IntList list = IntList.of(1, 2, 3);
        assertFalse(list.removeAllOccurrences(42));
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeAll_IntList() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5);
        IntList list2 = IntList.of(2, 4);
        assertTrue(list1.removeAll(list2));
        assertEquals(3, list1.size());
        assertEquals(1, list1.get(0));
        assertEquals(3, list1.get(1));
        assertEquals(5, list1.get(2));
    }

    @Test
    public void test_removeAll_IntList_empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        assertFalse(list1.removeAll(list2));
        assertEquals(3, list1.size());
    }

    @Test
    public void test_removeAll_array() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int[] arr = { 2, 4 };
        assertTrue(list.removeAll(arr));
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void test_removeIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6);
        assertTrue(list.removeIf(x -> x % 2 == 0));
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void test_removeIf_none() {
        IntList list = IntList.of(1, 3, 5);
        assertFalse(list.removeIf(x -> x % 2 == 0));
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeIf_null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(NullPointerException.class, () -> list.removeIf(null));
    }

    @Test
    public void test_removeDuplicates() {
        IntList list = IntList.of(1, 2, 2, 3, 3, 3, 4);
        assertTrue(list.removeDuplicates());
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(4, list.get(3));
    }

    @Test
    public void test_removeDuplicates_none() {
        IntList list = IntList.of(1, 2, 3, 4);
        assertFalse(list.removeDuplicates());
        assertEquals(4, list.size());
    }

    @Test
    public void test_removeDuplicates_sorted() {
        IntList list = IntList.of(1, 1, 2, 2, 3, 3);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_retainAll_IntList() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5);
        IntList list2 = IntList.of(2, 4, 6);
        assertTrue(list1.retainAll(list2));
        assertEquals(2, list1.size());
        assertEquals(2, list1.get(0));
        assertEquals(4, list1.get(1));
    }

    @Test
    public void test_retainAll_IntList_empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        assertTrue(list1.retainAll(list2));
        assertEquals(0, list1.size());
    }

    @Test
    public void test_retainAll_array() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int[] arr = { 2, 4, 6 };
        assertTrue(list.retainAll(arr));
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
        assertEquals(4, list.get(1));
    }

    @Test
    public void test_delete() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int removed = list.delete(2);
        assertEquals(3, removed);
        assertEquals(4, list.size());
        assertEquals(4, list.get(2));
    }

    @Test
    public void test_delete_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(3));
    }

    @Test
    public void test_deleteAllByIndices() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void test_deleteAllByIndices_empty() {
        IntList list = IntList.of(1, 2, 3);
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void test_deleteRange() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(1));
    }

    @Test
    public void test_deleteRange_empty() {
        IntList list = IntList.of(1, 2, 3);
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void test_deleteRange_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(2, 1));
    }

    @Test
    public void test_moveRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.moveRange(1, 3, 3);
        assertEquals(IntList.of(0, 3, 4, 1, 2, 5), list);
    }

    @Test
    public void test_replaceRange_IntList() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntList replacement = IntList.of(7, 8);
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(7, list.get(1));
        assertEquals(8, list.get(2));
        assertEquals(5, list.get(3));
    }

    @Test
    public void test_replaceRange_IntList_empty() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntList replacement = new IntList();
        list.replaceRange(1, 4, replacement);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(1));
    }

    @Test
    public void test_replaceRange_array() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int[] replacement = { 7, 8 };
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(7, list.get(1));
        assertEquals(8, list.get(2));
        assertEquals(5, list.get(3));
    }

    @Test
    public void test_replaceAll_values() {
        IntList list = IntList.of(1, 2, 3, 2, 4, 2);
        int count = list.replaceAll(2, 9);
        assertEquals(3, count);
        assertEquals(1, list.get(0));
        assertEquals(9, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(9, list.get(3));
    }

    @Test
    public void test_replaceAll_values_notFound() {
        IntList list = IntList.of(1, 2, 3);
        int count = list.replaceAll(42, 9);
        assertEquals(0, count);
    }

    @Test
    public void test_replaceAll_operator() {
        IntList list = IntList.of(1, 2, 3, 4);
        list.replaceAll(x -> x * 2);
        assertEquals(2, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(6, list.get(2));
        assertEquals(8, list.get(3));
    }

    @Test
    public void test_replaceAll_operator_null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(NullPointerException.class, () -> list.replaceAll((java.util.function.IntUnaryOperator) null));
    }

    @Test
    public void test_replaceIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6);
        assertTrue(list.replaceIf(x -> x % 2 == 0, 99));
        assertEquals(1, list.get(0));
        assertEquals(99, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(99, list.get(3));
    }

    @Test
    public void test_replaceIf_none() {
        IntList list = IntList.of(1, 3, 5);
        assertFalse(list.replaceIf(x -> x % 2 == 0, 99));
    }

    @Test
    public void test_replaceIf_null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(NullPointerException.class, () -> list.replaceIf(null, 99));
    }

    @Test
    public void test_fill() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.fill(99);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(99, list.get(i));
        }
    }

    @Test
    public void test_fill_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.fill(1, 4, 99);
        assertEquals(1, list.get(0));
        assertEquals(99, list.get(1));
        assertEquals(99, list.get(2));
        assertEquals(99, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_fill_range_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(-1, 2, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(0, 4, 99));
    }

    @Test
    public void test_contains() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertTrue(list.contains(3));
        assertFalse(list.contains(42));
    }

    @Test
    public void test_contains_empty() {
        IntList list = new IntList();
        assertFalse(list.contains(1));
    }

    @Test
    public void test_containsAny_IntList() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(3, 4, 5);
        assertTrue(list1.containsAny(list2));
    }

    @Test
    public void test_containsAny_IntList_none() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(4, 5, 6);
        assertFalse(list1.containsAny(list2));
    }

    @Test
    public void test_containsAny_array() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = { 3, 4, 5 };
        assertTrue(list.containsAny(arr));
    }

    @Test
    public void test_containsAny_array_none() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = { 4, 5, 6 };
        assertFalse(list.containsAny(arr));
    }

    @Test
    public void test_containsAll_IntList() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5);
        IntList list2 = IntList.of(2, 4);
        assertTrue(list1.containsAll(list2));
    }

    @Test
    public void test_containsAll_IntList_not() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(3, 4);
        assertFalse(list1.containsAll(list2));
    }

    @Test
    public void test_containsAll_IntList_empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        assertTrue(list1.containsAll(list2));
    }

    @Test
    public void test_containsAll_array() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int[] arr = { 2, 4 };
        assertTrue(list.containsAll(arr));
    }

    @Test
    public void test_containsAll_array_not() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = { 3, 4 };
        assertFalse(list.containsAll(arr));
    }

    @Test
    public void test_disjoint_IntList() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(4, 5, 6);
        assertTrue(list1.disjoint(list2));
    }

    @Test
    public void test_disjoint_IntList_not() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(3, 4, 5);
        assertFalse(list1.disjoint(list2));
    }

    @Test
    public void test_disjoint_array() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = { 4, 5, 6 };
        assertTrue(list.disjoint(arr));
    }

    @Test
    public void test_disjoint_array_not() {
        IntList list = IntList.of(1, 2, 3);
        int[] arr = { 3, 4, 5 };
        assertFalse(list.disjoint(arr));
    }

    @Test
    public void test_intersection_IntList() {
        IntList list1 = IntList.of(1, 2, 2, 3, 4);
        IntList list2 = IntList.of(2, 2, 3, 5);
        IntList result = list1.intersection(list2);
        assertEquals(3, result.size());
        assertEquals(2, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    public void test_intersection_IntList_empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        IntList result = list1.intersection(list2);
        assertEquals(0, result.size());
    }

    @Test
    public void test_intersection_array() {
        IntList list = IntList.of(1, 2, 2, 3, 4);
        int[] arr = { 2, 2, 3, 5 };
        IntList result = list.intersection(arr);
        assertEquals(3, result.size());
        assertEquals(2, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    public void test_difference_IntList() {
        IntList list1 = IntList.of(1, 2, 2, 3, 4);
        IntList list2 = IntList.of(2, 3, 5);
        IntList result = list1.difference(list2);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(4, result.get(2));
    }

    @Test
    public void test_difference_IntList_empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        IntList result = list1.difference(list2);
        assertEquals(3, result.size());
    }

    @Test
    public void test_difference_array() {
        IntList list = IntList.of(1, 2, 2, 3, 4);
        int[] arr = { 2, 3, 5 };
        IntList result = list.difference(arr);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(4, result.get(2));
    }

    @Test
    public void test_symmetricDifference_IntList() {
        IntList list1 = IntList.of(1, 2, 2, 3);
        IntList list2 = IntList.of(2, 3, 4, 5);
        IntList result = list1.symmetricDifference(list2);
        assertTrue(result.size() > 0);
        assertTrue(result.contains(1));
        assertTrue(result.contains(4));
        assertTrue(result.contains(5));
    }

    @Test
    public void test_symmetricDifference_IntList_empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        IntList result = list1.symmetricDifference(list2);
        assertEquals(3, result.size());
    }

    @Test
    public void test_symmetricDifference_array() {
        IntList list = IntList.of(1, 2, 2, 3);
        int[] arr = { 2, 3, 4, 5 };
        IntList result = list.symmetricDifference(arr);
        assertTrue(result.size() > 0);
        assertTrue(result.contains(1));
        assertTrue(result.contains(4));
        assertTrue(result.contains(5));
    }

    @Test
    public void test_occurrencesOf() {
        IntList list = IntList.of(1, 2, 3, 2, 4, 2);
        assertEquals(3, list.occurrencesOf(2));
        assertEquals(1, list.occurrencesOf(1));
        assertEquals(0, list.occurrencesOf(42));
    }

    @Test
    public void test_occurrencesOf_empty() {
        IntList list = new IntList();
        assertEquals(0, list.occurrencesOf(1));
    }

    @Test
    public void test_indexOf() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        assertEquals(1, list.indexOf(2));
        assertEquals(0, list.indexOf(1));
        assertEquals(-1, list.indexOf(42));
    }

    @Test
    public void test_indexOf_fromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        assertEquals(3, list.indexOf(2, 2));
        assertEquals(-1, list.indexOf(1, 2));
    }

    @Test
    public void test_indexOf_fromIndex_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(-1, list.indexOf(1, 10));
    }

    @Test
    public void test_lastIndexOf() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        assertEquals(3, list.lastIndexOf(2));
        assertEquals(0, list.lastIndexOf(1));
        assertEquals(-1, list.lastIndexOf(42));
    }

    @Test
    public void test_lastIndexOf_fromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        assertEquals(1, list.lastIndexOf(2, 2));
        assertEquals(-1, list.lastIndexOf(4, 2));
    }

    @Test
    public void test_lastIndexOf_fromIndex_negative() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(-1, list.lastIndexOf(1, -1));
    }

    @Test
    public void test_min() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        OptionalInt min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.get());
    }

    @Test
    public void test_min_empty() {
        IntList list = new IntList();
        OptionalInt min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void test_min_range() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        OptionalInt min = list.min(2, 5);
        assertTrue(min.isPresent());
        assertEquals(1, min.get());
    }

    @Test
    public void test_min_range_empty() {
        IntList list = IntList.of(1, 2, 3);
        OptionalInt min = list.min(1, 1);
        assertFalse(min.isPresent());
    }

    @Test
    public void test_max() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        OptionalInt max = list.max();
        assertTrue(max.isPresent());
        assertEquals(9, max.get());
    }

    @Test
    public void test_max_empty() {
        IntList list = new IntList();
        OptionalInt max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void test_max_range() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        OptionalInt max = list.max(0, 3);
        assertTrue(max.isPresent());
        assertEquals(4, max.get());
    }

    @Test
    public void test_max_range_empty() {
        IntList list = IntList.of(1, 2, 3);
        OptionalInt max = list.max(1, 1);
        assertFalse(max.isPresent());
    }

    @Test
    public void test_median() {
        IntList list = IntList.of(3, 1, 4, 1, 5);
        OptionalInt median = list.median();
        assertTrue(median.isPresent());
    }

    @Test
    public void test_median_empty() {
        IntList list = new IntList();
        OptionalInt median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    public void test_median_range() {
        IntList list = IntList.of(3, 1, 4, 1, 5);
        OptionalInt median = list.median(0, 3);
        assertTrue(median.isPresent());
    }

    @Test
    public void test_median_range_empty() {
        IntList list = IntList.of(1, 2, 3);
        OptionalInt median = list.median(1, 1);
        assertFalse(median.isPresent());
    }

    @Test
    public void test_forEach() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntList result = new IntList();
        list.forEach(result::add);
        assertEquals(5, result.size());
        assertEquals(1, result.get(0));
        assertEquals(5, result.get(4));
    }

    @Test
    public void test_forEach_null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(NullPointerException.class, () -> list.forEach(null));
    }

    @Test
    public void test_forEach_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntList result = new IntList();
        list.forEach(1, 4, result::add);
        assertEquals(3, result.size());
        assertEquals(2, result.get(0));
        assertEquals(4, result.get(2));
    }

    @Test
    public void test_forEach_range_reverse() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntList result = new IntList();
        list.forEach(4, 1, result::add);
        assertEquals(3, result.size());
        assertEquals(5, result.get(0));
        assertEquals(3, result.get(2));
    }

    @Test
    public void test_first() {
        IntList list = IntList.of(1, 2, 3);
        OptionalInt first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1, first.get());
    }

    @Test
    public void test_first_empty() {
        IntList list = new IntList();
        OptionalInt first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_last() {
        IntList list = IntList.of(1, 2, 3);
        OptionalInt last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3, last.get());
    }

    @Test
    public void test_last_empty() {
        IntList list = new IntList();
        OptionalInt last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_distinct() {
        IntList list = IntList.of(1, 2, 2, 3, 3, 3, 4);
        IntList result = list.distinct(0, 7);
        assertEquals(4, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
        assertEquals(4, result.get(3));
    }

    @Test
    public void test_distinct_empty() {
        IntList list = IntList.of(1, 2, 3);
        IntList result = list.distinct(1, 1);
        assertEquals(0, result.size());
    }

    @Test
    public void test_hasDuplicates() {
        IntList list = IntList.of(1, 2, 2, 3);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void test_hasDuplicates_none() {
        IntList list = IntList.of(1, 2, 3, 4);
        assertFalse(list.hasDuplicates());
    }

    @Test
    public void test_isSorted() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertTrue(list.isSorted());
    }

    @Test
    public void test_isSorted_not() {
        IntList list = IntList.of(1, 3, 2, 4);
        assertFalse(list.isSorted());
    }

    @Test
    public void test_isSorted_empty() {
        IntList list = new IntList();
        assertTrue(list.isSorted());
    }

    @Test
    public void test_sort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9, 2, 6);
        list.sort();
        assertTrue(list.isSorted());
        assertEquals(1, list.get(0));
        assertEquals(9, list.get(7));
    }

    @Test
    public void test_sort_alreadySorted() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.sort();
        assertTrue(list.isSorted());
    }

    @Test
    public void test_parallelSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9, 2, 6);
        list.parallelSort();
        assertTrue(list.isSorted());
        assertEquals(1, list.get(0));
        assertEquals(9, list.get(7));
    }

    @Test
    public void test_reverseSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9, 2, 6);
        list.reverseSort();
        assertEquals(9, list.get(0));
        assertEquals(1, list.get(7));
    }

    @Test
    public void test_binarySearch() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int index = list.binarySearch(3);
        assertEquals(2, index);
    }

    @Test
    public void test_binarySearch_notFound() {
        IntList list = IntList.of(1, 2, 4, 5);
        int index = list.binarySearch(3);
        assertTrue(index < 0);
    }

    @Test
    public void test_binarySearch_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int index = list.binarySearch(1, 4, 3);
        assertEquals(2, index);
    }

    @Test
    public void test_binarySearch_range_notFound() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int index = list.binarySearch(0, 2, 3);
        assertTrue(index < 0);
    }

    @Test
    public void test_reverse() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.reverse();
        assertEquals(5, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(1, list.get(4));
    }

    @Test
    public void test_reverse_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.reverse(1, 4);
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(2, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_rotate() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.rotate(2);
        assertEquals(4, list.get(0));
        assertEquals(5, list.get(1));
        assertEquals(1, list.get(2));
    }

    @Test
    public void test_rotate_negative() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.rotate(-2);
        assertEquals(3, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void test_shuffle() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntList copy = list.copy();
        list.shuffle();
        assertEquals(10, list.size());
    }

    @Test
    public void test_shuffle_withRandom() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Random rnd = new Random(12345);
        list.shuffle(rnd);
        assertEquals(10, list.size());
    }

    @Test
    public void test_swap() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.swap(1, 3);
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(2, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    public void test_swap_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(1, 3));
    }

    @Test
    public void test_copy() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntList copy = list.copy();
        assertEquals(5, copy.size());
        assertEquals(list.get(0), copy.get(0));

        list.set(0, 99);
        assertEquals(1, copy.get(0));
    }

    @Test
    public void test_copy_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals(2, copy.get(0));
        assertEquals(4, copy.get(2));
    }

    @Test
    public void test_copy_range_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(0, 4));
    }

    @Test
    public void test_copy_withStep() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        IntList copy = list.copy(0, 10, 2);
        assertEquals(5, copy.size());
        assertEquals(0, copy.get(0));
        assertEquals(2, copy.get(1));
        assertEquals(8, copy.get(4));
    }

    @Test
    public void test_copy_withStep_negative() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        IntList copy = list.copy(5, 0, -1);
        assertEquals(5, copy.size());
        assertEquals(5, copy.get(0));
        assertEquals(1, copy.get(4));
    }

    @Test
    public void test_split() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<IntList> chunks = list.split(0, 8, 3);
        assertEquals(3, chunks.size());
        assertEquals(3, chunks.get(0).size());
        assertEquals(3, chunks.get(1).size());
        assertEquals(2, chunks.get(2).size());
    }

    @Test
    public void test_split_exactDivision() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6);
        List<IntList> chunks = list.split(0, 6, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(2).size());
    }

    @Test
    public void test_trimToSize() {
        IntList list = new IntList(100);
        list.add(1);
        list.add(2);
        list.add(3);
        IntList trimmed = list.trimToSize();
        assertEquals(3, trimmed.size());
        assertSame(list, trimmed);
    }

    @Test
    public void test_clear() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_isEmpty() {
        IntList list = new IntList();
        assertTrue(list.isEmpty());

        list.add(1);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_size() {
        IntList list = new IntList();
        assertEquals(0, list.size());

        list.add(1);
        assertEquals(1, list.size());

        list.add(2);
        list.add(3);
        assertEquals(3, list.size());
    }

    @Test
    public void test_boxed() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        List<Integer> boxed = list.boxed();
        assertEquals(5, boxed.size());
        assertEquals(Integer.valueOf(1), boxed.get(0));
        assertEquals(Integer.valueOf(5), boxed.get(4));
    }

    @Test
    public void test_boxed_empty() {
        IntList list = new IntList();
        List<Integer> boxed = list.boxed();
        assertEquals(0, boxed.size());
    }

    @Test
    public void test_boxed_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        List<Integer> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Integer.valueOf(2), boxed.get(0));
        assertEquals(Integer.valueOf(4), boxed.get(2));
    }

    @Test
    public void test_toArray() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int[] arr = list.toArray();
        assertEquals(5, arr.length);
        assertEquals(1, arr[0]);
        assertEquals(5, arr[4]);

        list.set(0, 99);
        assertEquals(1, arr[0]);
    }

    @Test
    public void test_toArray_empty() {
        IntList list = new IntList();
        int[] arr = list.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void test_toLongList() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        LongList longList = list.toLongList();
        assertEquals(5, longList.size());
        assertEquals(1L, longList.get(0));
        assertEquals(5L, longList.get(4));
    }

    @Test
    public void test_toFloatList() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        FloatList floatList = list.toFloatList();
        assertEquals(5, floatList.size());
        assertEquals(1.0f, floatList.get(0), 0.001f);
        assertEquals(5.0f, floatList.get(4), 0.001f);
    }

    @Test
    public void test_toDoubleList() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        DoubleList doubleList = list.toDoubleList();
        assertEquals(5, doubleList.size());
        assertEquals(1.0, doubleList.get(0), 0.001);
        assertEquals(5.0, doubleList.get(4), 0.001);
    }

    @Test
    public void test_toCollection() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        Collection<Integer> collection = list.toCollection(0, 5, ArrayList::new);
        assertEquals(5, collection.size());
        assertTrue(collection.contains(1));
        assertTrue(collection.contains(5));
    }

    @Test
    public void test_toCollection_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        Collection<Integer> collection = list.toCollection(1, 4, ArrayList::new);
        assertEquals(3, collection.size());
        assertTrue(collection.contains(2));
        assertTrue(collection.contains(4));
    }

    @Test
    public void test_toMultiset() {
        IntList list = IntList.of(1, 2, 2, 3, 3, 3);
        Multiset<Integer> multiset = list.toMultiset(0, 6, Multiset::new);
        assertEquals(3, multiset.get(3));
        assertEquals(2, multiset.get(2));
        assertEquals(1, multiset.get(1));
    }

    @Test
    public void test_iterator() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextInt());
    }

    @Test
    public void test_iterator_empty() {
        IntList list = new IntList();
        IntIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_stream() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntStream stream = list.stream();
        assertEquals(5, stream.count());
    }

    @Test
    public void test_stream_operations() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        long sum = list.stream().sum();
        assertEquals(15, sum);
    }

    @Test
    public void test_stream_range() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntStream stream = list.stream(1, 4);
        assertEquals(3, stream.count());
    }

    @Test
    public void test_stream_range_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.stream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.stream(0, 4));
    }

    @Test
    public void test_getFirst() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(1, list.getFirst());
    }

    @Test
    public void test_getFirst_empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void test_getLast() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(5, list.getLast());
    }

    @Test
    public void test_getLast_empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void test_addFirst() {
        IntList list = IntList.of(2, 3, 4);
        list.addFirst(1);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    public void test_addLast() {
        IntList list = IntList.of(1, 2, 3);
        list.addLast(4);
        assertEquals(4, list.size());
        assertEquals(4, list.get(3));
    }

    @Test
    public void test_removeFirst() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int removed = list.removeFirst();
        assertEquals(1, removed);
        assertEquals(4, list.size());
        assertEquals(2, list.get(0));
    }

    @Test
    public void test_removeFirst_empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void test_removeLast() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int removed = list.removeLast();
        assertEquals(5, removed);
        assertEquals(4, list.size());
        assertEquals(4, list.get(3));
    }

    @Test
    public void test_removeLast_empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void test_hashCode_consistent() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5);
        IntList list2 = IntList.of(1, 2, 3, 4, 5);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void test_hashCode_different() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(1, 2, 4);
        assertNotEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void test_equals_same() {
        IntList list = IntList.of(1, 2, 3);
        assertTrue(list.equals(list));
    }

    @Test
    public void test_equals_equal() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5);
        IntList list2 = IntList.of(1, 2, 3, 4, 5);
        assertTrue(list1.equals(list2));
        assertTrue(list2.equals(list1));
    }

    @Test
    public void test_equals_notEqual() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(1, 2, 4);
        assertFalse(list1.equals(list2));
    }

    @Test
    public void test_equals_differentSize() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(1, 2);
        assertFalse(list1.equals(list2));
    }

    @Test
    public void test_equals_null() {
        IntList list = IntList.of(1, 2, 3);
        assertFalse(list.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        IntList list = IntList.of(1, 2, 3);
        assertFalse(list.equals("not a list"));
    }

    @Test
    public void test_toString() {
        IntList list = IntList.of(1, 2, 3);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void test_toString_empty() {
        IntList list = new IntList();
        String str = list.toString();
        assertNotNull(str);
    }
}
