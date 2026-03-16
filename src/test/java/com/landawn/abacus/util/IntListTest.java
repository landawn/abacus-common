package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

@Tag("2025")
public class IntListTest extends TestBase {

    private IntList list;

    @BeforeEach
    public void setUp() {
        list = new IntList();
    }

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
        assertThrows(IllegalArgumentException.class, () -> new IntList(arr, -1));
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
        assertThrows(IllegalArgumentException.class, () -> IntList.of(arr, -1));
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
        int[] arr = list.internalArray();
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
        int removed = list.removeAt(2);
        assertEquals(3, removed);
        assertEquals(4, list.size());
        assertEquals(4, list.get(2));
    }

    @Test
    public void test_delete_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(3));
    }

    @Test
    public void test_removeAt() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.removeAt(1, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void test_removeAt_empty() {
        IntList list = IntList.of(1, 2, 3);
        list.removeAt();
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeRange() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(1));
    }

    @Test
    public void test_removeRange_empty() {
        IntList list = IntList.of(1, 2, 3);
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeRange_outOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(2, 1));
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
    public void test_frequency() {
        IntList list = IntList.of(1, 2, 3, 2, 4, 2);
        assertEquals(3, list.frequency(2));
        assertEquals(1, list.frequency(1));
        assertEquals(0, list.frequency(42));
    }

    @Test
    public void test_frequency_empty() {
        IntList list = new IntList();
        assertEquals(0, list.frequency(1));
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
    public void test_containsDuplicates() {
        IntList list = IntList.of(1, 2, 2, 3);
        assertTrue(list.containsDuplicates());
    }

    @Test
    public void test_containsDuplicates_none() {
        IntList list = IntList.of(1, 2, 3, 4);
        assertFalse(list.containsDuplicates());
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

    @Test
    public void testConstructorWithInitialCapacity() {
        IntList list = new IntList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertEquals(10, list.internalArray().length);
    }

    @Test
    public void testConstructorWithArray() {
        int[] a = { 1, 2, 3 };
        IntList list = new IntList(a);
        assertEquals(3, list.size());
        assertArrayEquals(a, list.toArray());
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = new IntList(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(a, 6));
    }

    @Test
    public void testOf() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        IntList emptyList = IntList.of();
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testOfArrayAndSize() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = IntList.of(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(a, 6));
    }

    @Test
    public void testCopyOf() {
        int[] a = { 1, 2, 3 };
        IntList list = IntList.copyOf(a);
        assertEquals(3, list.size());
        assertArrayEquals(a, list.toArray());
        a[0] = 99;
        assertNotEquals(99, list.get(0));
    }

    @Test
    public void testCopyOfRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(a, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRange() {
        IntList list = IntList.range(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRangeWithStep() {
        IntList list = IntList.range(1, 10, 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
    }

    @Test
    public void testRangeClosed() {
        IntList list = IntList.rangeClosed(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, list.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        IntList list = IntList.rangeClosed(1, 10, 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
    }

    @Test
    public void testRepeat() {
        IntList list = IntList.repeat(5, 3);
        assertArrayEquals(new int[] { 5, 5, 5 }, list.toArray());
    }

    @Test
    public void testRandomWithRange() {
        IntList list = IntList.random(1, 10, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 1 && list.get(i) < 10);
        }
        assertThrows(IllegalArgumentException.class, () -> IntList.random(10, 1, 5));
    }

    @Test
    public void testArray() {
        int[] a = { 1, 2, 3 };
        IntList list = new IntList(a);
        assertSame(a, list.internalArray());
    }

    @Test
    public void testGet() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    public void testSet() {
        IntList list = IntList.of(1, 2, 3);
        int oldVal = list.set(1, 99);
        assertEquals(2, oldVal);
        assertArrayEquals(new int[] { 1, 99, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 100));
    }

    @Test
    public void testAdd() {
        IntList list = new IntList();
        list.add(10);
        list.add(20);
        assertEquals(2, list.size());
        assertArrayEquals(new int[] { 10, 20 }, list.toArray());
    }

    @Test
    public void testAddAtIndex() {
        IntList list = IntList.of(10, 30);
        list.add(1, 20);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 10, 20, 30 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 40));
    }

    @Test
    public void testAddAll() {
        IntList list = IntList.of(1, 2);
        IntList toAdd = IntList.of(3, 4);
        list.addAll(toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAllAtIndex() {
        IntList list = IntList.of(1, 4);
        IntList toAdd = IntList.of(2, 3);
        list.addAll(1, toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAllArray() {
        IntList list = IntList.of(1, 2);
        int[] toAdd = { 3, 4 };
        list.addAll(toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAllArrayAtIndex() {
        IntList list = IntList.of(1, 4);
        int[] toAdd = { 2, 3 };
        list.addAll(1, toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRemove() {
        IntList list = IntList.of(1, 2, 3, 2);
        assertTrue(list.remove(2));
        assertArrayEquals(new int[] { 1, 3, 2 }, list.toArray());
        assertFalse(list.remove(99));
    }

    @Test
    public void testRemoveAllOccurrences() {
        IntList list = IntList.of(1, 2, 3, 2, 4, 2);
        assertTrue(list.removeAllOccurrences(2));
        assertArrayEquals(new int[] { 1, 3, 4 }, list.toArray());
        assertFalse(list.removeAllOccurrences(99));
    }

    @Test
    public void testRemoveAllIntList() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 2);
        IntList toRemove = IntList.of(2, 4);
        assertTrue(list.removeAll(toRemove));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveAllArray() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 2);
        int[] toRemove = { 2, 4 };
        assertTrue(list.removeAll(toRemove));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntPredicate predicate = (n) -> n % 2 == 0;
        assertTrue(list.removeIf(predicate));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveDuplicates() {
        IntList list = IntList.of(1, 2, 2, 3, 1, 4, 4);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
        IntList noDuplicates = IntList.of(1, 2, 3);
        assertFalse(noDuplicates.removeDuplicates());
    }

    @Test
    public void testRetainAll() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        IntList toRetain = IntList.of(2, 4, 5);
        assertTrue(list.retainAll(toRetain));
        assertArrayEquals(new int[] { 2, 2, 4 }, list.toArray());
    }

    @Test
    public void testRetainAllArray() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        int[] toRetain = { 2, 4, 5 };
        assertTrue(list.retainAll(toRetain));
        assertArrayEquals(new int[] { 2, 2, 4 }, list.toArray());
    }

    @Test
    public void testDelete() {
        IntList list = IntList.of(1, 2, 3);
        int deleted = list.removeAt(1);
        assertEquals(2, deleted);
        assertArrayEquals(new int[] { 1, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
    }

    @Test
    public void testDeleteAllByIndices() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.removeAt(1, 3, 5);
        assertArrayEquals(new int[] { 0, 2, 4 }, list.toArray());
    }

    @Test
    public void testDeleteRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.removeRange(1, 4);
        assertArrayEquals(new int[] { 0, 4, 5 }, list.toArray());
    }

    @Test
    public void testMoveRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.moveRange(1, 3, 4);
        assertArrayEquals(new int[] { 0, 3, 4, 5, 1, 2 }, list.toArray());
    }

    @Test
    public void testReplaceRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        IntList replacement = IntList.of(9, 8, 7);
        list.replaceRange(1, 4, replacement);
        assertArrayEquals(new int[] { 0, 9, 8, 7, 4, 5 }, list.toArray());
    }

    @Test
    public void testReplaceRangeArray() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        int[] replacement = { 9, 8, 7 };
        list.replaceRange(1, 4, replacement);
        assertArrayEquals(new int[] { 0, 9, 8, 7, 4, 5 }, list.toArray());
    }

    @Test
    public void testReplaceAll() {
        IntList list = IntList.of(1, 2, 1, 3, 1);
        int count = list.replaceAll(1, 99);
        assertEquals(3, count);
        assertArrayEquals(new int[] { 99, 2, 99, 3, 99 }, list.toArray());
    }

    @Test
    public void testReplaceAllOperator() {
        IntList list = IntList.of(1, 2, 3, 4);
        IntUnaryOperator operator = (n) -> n * 2;
        list.replaceAll(operator);
        assertArrayEquals(new int[] { 2, 4, 6, 8 }, list.toArray());
    }

    @Test
    public void testReplaceIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntPredicate predicate = (n) -> n > 3;
        assertTrue(list.replaceIf(predicate, 99));
        assertArrayEquals(new int[] { 1, 2, 3, 99, 99 }, list.toArray());
    }

    @Test
    public void testFill() {
        IntList list = IntList.of(1, 2, 3, 4);
        list.fill(99);
        assertArrayEquals(new int[] { 99, 99, 99, 99 }, list.toArray());
    }

    @Test
    public void testFillRange() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.fill(1, 4, 99);
        assertArrayEquals(new int[] { 1, 99, 99, 99, 5 }, list.toArray());
    }

    @Test
    public void testContains() {
        IntList list = IntList.of(1, 2, 3);
        assertTrue(list.contains(2));
        assertFalse(list.contains(4));
    }

    @Test
    public void testContainsAny() {
        IntList list = IntList.of(1, 2, 3);
        IntList any1 = IntList.of(4, 5, 2);
        assertTrue(list.containsAny(any1));
        IntList any2 = IntList.of(4, 5, 6);
        assertFalse(list.containsAny(any2));
    }

    @Test
    public void testContainsAll() {
        IntList list = IntList.of(1, 2, 3, 4);
        IntList all1 = IntList.of(2, 4);
        assertTrue(list.containsAll(all1));
        IntList all2 = IntList.of(2, 5);
        assertFalse(list.containsAll(all2));
    }

    @Test
    public void testDisjoint() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(4, 5, 6);
        assertTrue(list1.disjoint(list2));
        IntList list3 = IntList.of(3, 4, 5);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void testIntersection() {
        IntList list1 = IntList.of(1, 2, 2, 3);
        IntList list2 = IntList.of(2, 3, 4, 2);
        IntList intersection = list1.intersection(list2);
        assertArrayEquals(new int[] { 2, 2, 3 }, intersection.toArray());
    }

    @Test
    public void testDifference() {
        IntList list1 = IntList.of(1, 2, 2, 3, 4);
        IntList list2 = IntList.of(2, 3, 5);
        IntList difference = list1.difference(list2);
        assertArrayEquals(new int[] { 1, 2, 4 }, difference.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        IntList list1 = IntList.of(1, 2, 2, 3);
        IntList list2 = IntList.of(2, 3, 4);
        IntList symmetricDifference = list1.symmetricDifference(list2);
        symmetricDifference.sort();
        assertArrayEquals(new int[] { 1, 2, 4 }, symmetricDifference.toArray());
    }

    @Test
    public void testOccurrencesOf() {
        IntList list = IntList.of(1, 2, 1, 3, 1, 2);
        assertEquals(3, list.frequency(1));
        assertEquals(2, list.frequency(2));
        assertEquals(0, list.frequency(4));
    }

    @Test
    public void testIndexOf() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(1, list.indexOf(2));
        assertEquals(0, list.indexOf(1));
        assertEquals(-1, list.indexOf(4));
    }

    @Test
    public void testIndexOfFromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(3, list.indexOf(2, 2));
    }

    @Test
    public void testLastIndexOf() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(3, list.lastIndexOf(2));
        assertEquals(4, list.lastIndexOf(1));
        assertEquals(-1, list.lastIndexOf(4));
    }

    @Test
    public void testLastIndexOfFromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(1, list.lastIndexOf(2, 2));
    }

    @Test
    public void testMin() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        assertEquals(1, list.min().getAsInt());
        assertTrue(new IntList().min().isEmpty());
    }

    @Test
    public void testMax() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        assertEquals(9, list.max().getAsInt());
    }

    @Test
    public void testMedian() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9, 2, 6);
        assertEquals(3, list.median().getAsInt());
    }

    @Test
    public void testForEach() {
        IntList list = IntList.of(1, 2, 3);
        final int[] sum = { 0 };
        IntConsumer consumer = (n) -> sum[0] += n;
        list.forEach(consumer);
        assertEquals(6, sum[0]);
    }

    @Test
    public void testFirst() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(1, list.first().getAsInt());
        assertTrue(new IntList().first().isEmpty());
    }

    @Test
    public void testLast() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(3, list.last().getAsInt());
        assertTrue(new IntList().last().isEmpty());
    }

    @Test
    public void testDistinct() {
        IntList list = IntList.of(1, 2, 1, 3, 2, 4);
        IntList distinctList = list.distinct();
        distinctList.sort();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, distinctList.toArray());
    }

    @Test
    public void testHasDuplicates() {
        IntList listWithDuplicates = IntList.of(1, 2, 1, 3);
        assertTrue(listWithDuplicates.containsDuplicates());
        IntList listWithoutDuplicates = IntList.of(1, 2, 3, 4);
        assertFalse(listWithoutDuplicates.containsDuplicates());
    }

    @Test
    public void testIsSorted() {
        IntList sortedList = IntList.of(1, 2, 3, 4);
        assertTrue(sortedList.isSorted());
        IntList unsortedList = IntList.of(1, 3, 2, 4);
        assertFalse(unsortedList.isSorted());
    }

    @Test
    public void testSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.sort();
        assertArrayEquals(new int[] { 1, 1, 3, 4, 5, 9 }, list.toArray());
    }

    @Test
    public void testParallelSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.parallelSort();
        assertArrayEquals(new int[] { 1, 1, 3, 4, 5, 9 }, list.toArray());
    }

    @Test
    public void testReverseSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.reverseSort();
        assertArrayEquals(new int[] { 9, 5, 4, 3, 1, 1 }, list.toArray());
    }

    @Test
    public void testBinarySearch() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(2, list.binarySearch(3));
        assertTrue(list.binarySearch(6) < 0);
    }

    @Test
    public void testReverse() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.reverse();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, list.toArray());
    }

    @Test
    public void testRotate() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.rotate(2);
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testShuffle() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntList list2 = list1.copy();
        list1.shuffle();
        assertFalse(Arrays.equals(list1.toArray(), list2.toArray()) && list1.size() > 1);
    }

    @Test
    public void testShuffleWithRandom() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntList list2 = list1.copy();
        list1.shuffle(new Random(123));
        list2.shuffle(new Random(123));
        assertArrayEquals(list1.toArray(), list2.toArray());
    }

    @Test
    public void testSwap() {
        IntList list = IntList.of(1, 2, 3, 4);
        list.swap(1, 3);
        assertArrayEquals(new int[] { 1, 4, 3, 2 }, list.toArray());
    }

    @Test
    public void testCopy() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = list1.copy();
        assertArrayEquals(list1.toArray(), list2.toArray());
        assertNotSame(list1, list2);
    }

    @Test
    public void testSplit() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7);
        List<IntList> chunks = list.split(3);
        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0).toArray());
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1).toArray());
        assertArrayEquals(new int[] { 7 }, chunks.get(2).toArray());
    }

    @Test
    public void testTrimToSize() {
        IntList list = new IntList(10);
        list.add(1);
        list.add(2);
        list.add(3);
        list.trimToSize();
        assertEquals(3, list.internalArray().length);
    }

    @Test
    public void testClear() {
        IntList list = IntList.of(1, 2, 3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testBoxed() {
        IntList list = IntList.of(1, 2, 3);
        List<Integer> boxedList = list.boxed();
        assertEquals(3, boxedList.size());
        assertEquals(1, boxedList.get(0));
        assertEquals(2, boxedList.get(1));
        assertEquals(3, boxedList.get(2));
    }

    @Test
    public void testToArray() {
        int[] a = { 1, 2, 3 };
        IntList list = IntList.of(a);
        assertArrayEquals(a, list.toArray());
        assertNotSame(a, list.toArray());
    }

    @Test
    public void testGetFirstAndLast() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(1, list.getFirst());
        assertEquals(3, list.getLast());
        IntList emptyList = new IntList();
        assertThrows(NoSuchElementException.class, () -> emptyList.getFirst());
        assertThrows(NoSuchElementException.class, () -> emptyList.getLast());
    }

    @Test
    public void testAddFirstAndLast() {
        IntList list = IntList.of(2, 3);
        list.addFirst(1);
        list.addLast(4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRemoveFirstAndLast() {
        IntList list = IntList.of(1, 2, 3, 4);
        assertEquals(1, list.removeFirst());
        assertEquals(4, list.removeLast());
        assertArrayEquals(new int[] { 2, 3 }, list.toArray());
    }

    @Test
    public void testHashCode() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(1, 2, 3);
        assertEquals(list1.hashCode(), list2.hashCode());
        IntList list3 = IntList.of(1, 2, 4);
        assertNotEquals(list1.hashCode(), list3.hashCode());
    }

    @Test
    public void testEquals() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(1, 2, 3);
        IntList list3 = IntList.of(1, 2, 4);
        IntList list4 = IntList.of(1, 2);
        assertTrue(list1.equals(list2));
        assertFalse(list1.equals(list3));
        assertFalse(list1.equals(list4));
        assertFalse(list1.equals(null));
        assertFalse(list1.equals(new Object()));
    }

    @Test
    public void testToString() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals("[1, 2, 3]", list.toString());
        IntList emptyList = new IntList();
        assertEquals("[]", emptyList.toString());
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new IntList(null));
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new IntList(null, 0));
    }

    @Test
    public void testOfWithNull() {
        IntList list = IntList.of((int[]) null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOfWithNull() {
        IntList list = IntList.copyOf(null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        int[] array = { 1, 2, 3, 4, 5 };
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.copyOf(array, 2, 10));
    }

    @Test
    public void testRangeWithNegativeStep() {
        IntList list = IntList.range(5, 0, -1);
        assertEquals(5, list.size());
        assertEquals(5, list.get(0));
        assertEquals(1, list.get(4));
    }

    @Test
    public void testRangeClosedWithNegativeStep() {
        IntList list = IntList.rangeClosed(5, 1, -1);
        assertEquals(5, list.size());
        assertEquals(5, list.get(0));
        assertEquals(1, list.get(4));
    }

    @Test
    public void testRandomWithInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> IntList.random(5, 5, 10));
        assertThrows(IllegalArgumentException.class, () -> IntList.random(10, 5, 10));
    }

    @Test
    public void testRandomWithLargeRange() {
        IntList list = IntList.random(Integer.MIN_VALUE, Integer.MAX_VALUE, 100);
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            int value = list.get(i);
            assertTrue(value >= Integer.MIN_VALUE && value < Integer.MAX_VALUE);
        }
    }

    @Test
    public void testAddAtBeginning() {
        list.addAll(new int[] { 2, 3, 4 });
        list.add(0, 1);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    public void testAddAtEnd() {
        list.addAll(new int[] { 1, 2, 3 });
        list.add(list.size(), 4);
        assertEquals(4, list.size());
        assertEquals(4, list.get(3));
    }

    @Test
    public void testAddAllEmptyListAtVariousPositions() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList empty = new IntList();

        assertFalse(list.addAll(0, empty));
        assertFalse(list.addAll(1, empty));
        assertFalse(list.addAll(list.size(), empty));
    }

    @Test
    public void testAddAllWithNullArray() {
        list.add(1);
        assertFalse(list.addAll((int[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testAddToEnsureCapacityGrowth() {
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    public void testRemoveAllOccurrencesEmptyList() {
        assertFalse(list.removeAllOccurrences(5));
    }

    @Test
    public void testRemoveAllOccurrencesNotFound() {
        list.addAll(new int[] { 1, 2, 3 });
        assertFalse(list.removeAllOccurrences(5));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIfNoMatch() {
        list.addAll(new int[] { 1, 2, 3 });
        assertFalse(list.removeIf(x -> x > 10));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicatesSingleElement() {
        list.add(1);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveDuplicatesNoDuplicates() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        assertFalse(list.removeDuplicates());
        assertEquals(5, list.size());
    }

    @Test
    public void testRemoveDuplicatesSorted() {
        list.addAll(new int[] { 1, 1, 2, 2, 2, 3, 3, 4, 5, 5 });
        assertTrue(list.removeDuplicates());
        assertEquals(5, list.size());
        for (int i = 1; i <= 5; i++) {
            assertEquals(i, list.get(i - 1));
        }
    }

    @Test
    public void testRetainAllEmpty() {
        list.addAll(new int[] { 1, 2, 3 });
        assertTrue(list.retainAll(new IntList()));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testDeleteAtBoundaries() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        assertEquals(1, list.removeAt(0));
        assertEquals(4, list.size());
        assertEquals(2, list.get(0));

        assertEquals(5, list.removeAt(list.size() - 1));
        assertEquals(3, list.size());
        assertEquals(4, list.get(list.size() - 1));
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.removeAt();
        assertTrue(list.isEmpty());

        list.addAll(new int[] { 1, 2, 3 });
        list.removeAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteAllByIndicesOutOfOrder() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        list.removeAt(4, 1, 2);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new int[] { 1, 2, 3 });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new int[] { 1, 2, 3 });
        list.removeRange(0, 3);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMoveRangeToBeginning() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        list.moveRange(3, 5, 0);
        assertEquals(5, list.size());
        assertEquals(4, list.get(0));
        assertEquals(5, list.get(1));
        assertEquals(1, list.get(2));
    }

    @Test
    public void testMoveRangeToEnd() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        list.moveRange(0, 2, 3);
        assertEquals(5, list.size());
        assertEquals(3, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(5, list.get(2));
        assertEquals(1, list.get(3));
        assertEquals(2, list.get(4));
    }

    @Test
    public void testReplaceRangeWithEmpty() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        list.replaceRange(1, 3, new IntList());
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void testReplaceRangeExpanding() {
        list.addAll(new int[] { 1, 2, 3 });
        list.replaceRange(1, 2, IntList.of(10, 20, 30));
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(10, list.get(1));
        assertEquals(20, list.get(2));
        assertEquals(30, list.get(3));
        assertEquals(3, list.get(4));
    }

    @Test
    public void testReplaceAllNoMatch() {
        list.addAll(new int[] { 1, 2, 3 });
        int count = list.replaceAll(5, 10);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfFalseCondition() {
        list.addAll(new int[] { 1, 2, 3 });
        boolean result = list.replaceIf(x -> false, 10);
        assertFalse(result);
    }

    @Test
    public void testFillEmptyList() {
        list.fill(10);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFillRangeInvalidRange() {
        list.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10));
    }

    @Test
    public void testContainsEmptyList() {
        assertFalse(list.contains(1));
    }

    @Test
    public void testContainsAnyBothEmpty() {
        IntList other = new IntList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAllEmptyAgainstNonEmpty() {
        list.addAll(new int[] { 1, 2, 3 });
        assertTrue(list.containsAll(new IntList()));
    }

    @Test
    public void testDisjointWithSelf() {
        list.addAll(new int[] { 1, 2, 3 });
        assertFalse(list.disjoint(list));
    }

    @Test
    public void testIntersectionWithEmpty() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList result = list.intersection(new IntList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionNoCommon() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList result = list.intersection(IntList.of(4, 5, 6));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceWithEmpty() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList result = list.difference(new IntList());
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    public void testDifferenceAllRemoved() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList result = list.difference(IntList.of(1, 2, 3));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        IntList result = list.symmetricDifference(new IntList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceOneEmpty() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList result = list.symmetricDifference(new IntList());
        assertEquals(3, result.size());
    }

    @Test
    public void testIndexOfFromIndexBeyondSize() {
        list.addAll(new int[] { 1, 2, 3 });
        assertEquals(-1, list.indexOf(1, 10));
    }

    @Test
    public void testIndexOfNegativeFromIndex() {
        list.addAll(new int[] { 1, 2, 3 });
        assertEquals(0, list.indexOf(1, -1));
    }

    @Test
    public void testLastIndexOfEmptyList() {
        assertEquals(-1, list.lastIndexOf(1));
    }

    @Test
    public void testLastIndexOfNegativeStart() {
        list.addAll(new int[] { 1, 2, 3 });
        assertEquals(-1, list.lastIndexOf(1, -1));
    }

    @Test
    public void testMinMaxMedianSingleElement() {
        list.add(5);

        OptionalInt min = list.min();
        assertTrue(min.isPresent());
        assertEquals(5, min.getAsInt());

        OptionalInt max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5, max.getAsInt());

        OptionalInt median = list.median();
        assertTrue(median.isPresent());
        assertEquals(5, median.getAsInt());
    }

    @Test
    public void testMinMaxMedianEmptyRange() {
        list.addAll(new int[] { 1, 2, 3 });

        assertFalse(list.min(1, 1).isPresent());
        assertFalse(list.max(1, 1).isPresent());
        assertFalse(list.median(1, 1).isPresent());
    }

    @Test
    public void testMedianEvenElements() {
        list.addAll(new int[] { 1, 2, 3, 4 });
        OptionalInt median = list.median();
        assertTrue(median.isPresent());
        assertEquals(2, median.getAsInt());
    }

    @Test
    public void testForEachEmptyList() {
        List<Integer> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachReverseRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        List<Integer> result = new ArrayList<>();

        list.forEach(4, 1, result::add);

        assertEquals(3, result.size());
        assertEquals(5, result.get(0));
        assertEquals(4, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    public void testForEachWithNegativeToIndex() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        List<Integer> result = new ArrayList<>();

        list.forEach(2, -1, result::add);

        assertEquals(3, result.size());
        assertEquals(3, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(1, result.get(2));
    }

    @Test
    public void testDistinctEmptyRange() {
        list.addAll(new int[] { 1, 2, 2, 3 });
        IntList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDistinctSingleElement() {
        list.addAll(new int[] { 1, 2, 2, 3 });
        IntList result = list.distinct(0, 1);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0));
    }

    @Test
    public void testSortSingleElement() {
        list.add(5);
        list.sort();
        assertEquals(1, list.size());
        assertEquals(5, list.get(0));
    }

    @Test
    public void testParallelSortSmallList() {
        list.addAll(new int[] { 3, 1, 2 });
        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testBinarySearchUnsorted() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });
        int result = list.binarySearch(3);
        assertNotNull(result);
    }

    @Test
    public void testReverseSingleElement() {
        list.add(5);
        list.reverse();
        assertEquals(1, list.size());
        assertEquals(5, list.get(0));
    }

    @Test
    public void testReverseRangeEmptyRange() {
        list.addAll(new int[] { 1, 2, 3 });
        list.reverse(1, 1);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testRotateSingleElement() {
        list.add(5);
        list.rotate(10);
        assertEquals(1, list.size());
        assertEquals(5, list.get(0));
    }

    @Test
    public void testRotateNegativeDistance() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        list.rotate(-2);
        assertEquals(3, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(5, list.get(2));
        assertEquals(1, list.get(3));
        assertEquals(2, list.get(4));
    }

    @Test
    public void testShuffleSingleElement() {
        list.add(5);
        list.shuffle();
        assertEquals(1, list.size());
        assertEquals(5, list.get(0));
    }

    @Test
    public void testSwapSameIndex() {
        list.addAll(new int[] { 1, 2, 3 });
        list.swap(1, 1);
        assertEquals(2, list.get(1));
    }

    @Test
    public void testSwapThrowsException() {
        list.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopyEmptyList() {
        IntList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopyRangeInvalidIndices() {
        list.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testCopyWithNegativeStep() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5, copy.get(0));
        assertEquals(4, copy.get(1));
        assertEquals(3, copy.get(2));
        assertEquals(2, copy.get(3));
    }

    @Test
    public void testCopyWithStepLargerThanRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList copy = list.copy(0, 5, 3);
        assertEquals(2, copy.size());
        assertEquals(1, copy.get(0));
        assertEquals(4, copy.get(1));
    }

    @Test
    public void testSplitEmptyList() {
        List<IntList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitWithChunkSizeLargerThanList() {
        list.addAll(new int[] { 1, 2, 3 });
        List<IntList> chunks = list.split(0, 3, 10);
        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).size());
    }

    @Test
    public void testSplitUnevenChunks() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        List<IntList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testBoxedEmptyList() {
        List<Integer> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxedRangeInvalidIndices() {
        list.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    @Test
    public void testIteratorEmptyList() {
        IntIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    @Test
    public void testStreamEmptyList() {
        IntStream stream = list.stream();
        assertEquals(0, stream.count());
    }

    @Test
    public void testStreamRangeEmptyRange() {
        list.addAll(new int[] { 1, 2, 3 });
        IntStream stream = list.stream(1, 1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testGetFirstGetLastSingleElement() {
        list.add(5);
        assertEquals(5, list.getFirst());
        assertEquals(5, list.getLast());
    }

    @Test
    public void testRemoveFirstRemoveLastSingleElement() {
        list.add(5);
        assertEquals(5, list.removeFirst());
        assertTrue(list.isEmpty());

        list.add(10);
        assertEquals(10, list.removeLast());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMaxArraySize() {
        try {
            IntList largeList = new IntList(Integer.MAX_VALUE - 8);
            assertTrue(largeList.isEmpty());
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEnsureCapacityOverflow() {
        list.add(1);
        try {
            for (int i = 0; i < 100; i++) {
                list.add(i);
            }
            assertTrue(list.size() > 1);
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testToStringWithSpecialValues() {
        list.add(Integer.MIN_VALUE);
        list.add(0);
        list.add(Integer.MAX_VALUE);
        String str = list.toString();
        assertTrue(str.contains(String.valueOf(Integer.MIN_VALUE)));
        assertTrue(str.contains("0"));
        assertTrue(str.contains(String.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    public void testArrayModification() {
        list.addAll(new int[] { 1, 2, 3 });
        int[] array = list.internalArray();

        array[1] = 20;
        assertEquals(20, list.get(1));

        list.clear();
        int[] newArray = list.internalArray();
        assertSame(array, newArray);
    }

    @Test
    public void testAddRemovePerformance() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> x % 2 == 0);
        assertEquals(count / 2, list.size());

        for (int i = 0; i < list.size(); i++) {
            assertEquals(i * 2 + 1, list.get(i));
        }
    }

    @Test
    public void testBatchOperationsLargeData() {
        int size = 1000;
        IntList list1 = IntList.range(0, size);
        IntList list2 = IntList.range(size / 2, size + size / 2);

        IntList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        IntList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        IntList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        IntIterator iter = list.iterator();
        list.add(6);

        assertTrue(iter.hasNext());
        iter.nextInt();
    }

    @Test
    public void testIntegerOverflow() {
        list.add(Integer.MAX_VALUE);
        list.add(Integer.MAX_VALUE);

        list.replaceAll(x -> x + 1);
        assertEquals(Integer.MIN_VALUE, list.get(0));
        assertEquals(Integer.MIN_VALUE, list.get(1));
    }

    @Test
    public void testNegativeValues() {
        list.addAll(new int[] { -5, -3, -1, 0, 1, 3, 5 });

        OptionalInt min = list.min();
        assertTrue(min.isPresent());
        assertEquals(-5, min.getAsInt());

        list.sort();
        assertEquals(-5, list.get(0));
        assertEquals(5, list.get(6));

        assertEquals(2, list.indexOf(-1));
        assertEquals(3, list.binarySearch(0));
    }

    // --- Tests for inherited PrimitiveList methods ---

    @Test
    public void testDistinctNoArg() {
        IntList list = IntList.of(1, 2, 1, 3, 2);
        IntList distinct = list.distinct();
        assertEquals(3, distinct.size());
        assertTrue(distinct.contains(1));
        assertTrue(distinct.contains(2));
        assertTrue(distinct.contains(3));
    }

    @Test
    public void testDistinctNoArg_Empty() {
        IntList distinct = list.distinct();
        assertTrue(distinct.isEmpty());
    }

    @Test
    public void testDistinctNoArg_NoDuplicates() {
        IntList list = IntList.of(1, 2, 3);
        IntList distinct = list.distinct();
        assertEquals(3, distinct.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, distinct.toArray());
    }

    @Test
    public void testSplitOneArg() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        List<IntList> chunks = list.split(2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testSplitOneArg_ExactDivision() {
        IntList list = IntList.of(1, 2, 3, 4);
        List<IntList> chunks = list.split(2);
        assertEquals(2, chunks.size());
        assertArrayEquals(new int[] { 1, 2 }, chunks.get(0).toArray());
        assertArrayEquals(new int[] { 3, 4 }, chunks.get(1).toArray());
    }

    @Test
    public void testSplitOneArg_Empty() {
        List<IntList> chunks = list.split(3);
        assertTrue(chunks.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToList() {
        IntList list = IntList.of(1, 2, 3);
        List<Integer> result = list.toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(2), result.get(1));
        assertEquals(Integer.valueOf(3), result.get(2));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToList_Empty() {
        List<Integer> result = list.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToListWithRange() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        List<Integer> result = list.toList(1, 4);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(2), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(1));
        assertEquals(Integer.valueOf(4), result.get(2));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToListWithRange_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(0, 4));
    }

    @Test
    public void testToSet() {
        IntList list = IntList.of(1, 2, 1, 3);
        Set<Integer> result = list.toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testToSet_Empty() {
        Set<Integer> result = list.toSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToSetWithRange() {
        IntList list = IntList.of(1, 2, 2, 3, 4);
        Set<Integer> result = list.toSet(1, 4);
        assertEquals(2, result.size());
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testToSetWithRange_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(0, 4));
    }

    @Test
    public void testToCollectionOneArg() {
        IntList list = IntList.of(1, 2, 3);
        ArrayList<Integer> result = list.toCollection(ArrayList::new);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(2), result.get(1));
        assertEquals(Integer.valueOf(3), result.get(2));
    }

    @Test
    public void testToCollectionOneArg_Empty() {
        ArrayList<Integer> result = list.toCollection(ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToCollectionWithRange_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(-1, 2, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(0, 4, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(2, 1, ArrayList::new));
    }

    @Test
    public void testToMultisetNoArg() {
        IntList list = IntList.of(1, 2, 1, 3, 1);
        Multiset<Integer> multiset = list.toMultiset();
        assertEquals(3, multiset.count(Integer.valueOf(1)));
        assertEquals(1, multiset.count(Integer.valueOf(2)));
        assertEquals(1, multiset.count(Integer.valueOf(3)));
    }

    @Test
    public void testToMultisetNoArg_Empty() {
        Multiset<Integer> multiset = list.toMultiset();
        assertEquals(0, multiset.size());
    }

    @Test
    public void testToMultisetTwoArgs() {
        IntList list = IntList.of(1, 2, 1, 3);
        Multiset<Integer> multiset = list.toMultiset(0, 3);
        assertEquals(2, multiset.count(Integer.valueOf(1)));
        assertEquals(1, multiset.count(Integer.valueOf(2)));
        assertEquals(0, multiset.count(Integer.valueOf(3)));
    }

    @Test
    public void testToMultisetTwoArgs_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4));
    }

    @Test
    public void testToMultisetWithSupplier() {
        IntList list = IntList.of(1, 2, 1, 3);
        Multiset<Integer> multiset = list.toMultiset(Multiset::new);
        assertEquals(2, multiset.count(Integer.valueOf(1)));
        assertEquals(1, multiset.count(Integer.valueOf(2)));
        assertEquals(1, multiset.count(Integer.valueOf(3)));
    }

    @Test
    public void testToMultisetThreeArgs_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2, Multiset::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4, Multiset::new));
    }

}
