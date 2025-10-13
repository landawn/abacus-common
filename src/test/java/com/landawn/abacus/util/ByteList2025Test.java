package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.ByteStream;

@Tag("2025")
public class ByteList2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        ByteList list = new ByteList();
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity() {
        ByteList list = new ByteList(10);
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity_negative() {
        assertThrows(IllegalArgumentException.class, () -> new ByteList(-1));
    }

    @Test
    public void test_constructor_withArray() {
        byte[] arr = { 1, 2, 3 };
        ByteList list = new ByteList(arr);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_constructor_withArrayAndSize() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteList list = new ByteList(arr, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_constructor_withArrayAndSize_invalidSize() {
        byte[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new ByteList(arr, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> new ByteList(arr, -1));
    }

    @Test
    public void test_of_varargs() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_of_emptyVarargs() {
        ByteList list = ByteList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_of_nullArray() {
        ByteList list = ByteList.of((byte[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void test_of_withSize() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteList list = ByteList.of(arr, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void test_of_withSize_invalidSize() {
        byte[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ByteList.of(arr, 5));
    }

    @Test
    public void test_copyOf() {
        byte[] arr = { 1, 2, 3 };
        ByteList list = ByteList.copyOf(arr);
        assertEquals(3, list.size());
        arr[0] = 99;
        assertEquals(1, list.get(0));
    }

    @Test
    public void test_copyOf_null() {
        ByteList list = ByteList.copyOf(null);
        assertEquals(0, list.size());
    }

    @Test
    public void test_copyOf_withRange() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteList list = ByteList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(4, list.get(2));
    }

    @Test
    public void test_range() {
        ByteList list = ByteList.range((byte) 1, (byte) 5);
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void test_range_withStep() {
        ByteList list = ByteList.range((byte) 0, (byte) 10, (byte) 2);
        assertEquals(5, list.size());
        assertArrayEquals(new byte[] { 0, 2, 4, 6, 8 }, list.toArray());
    }

    @Test
    public void test_range_negativeStep() {
        ByteList list = ByteList.range((byte) 10, (byte) 0, (byte) -2);
        assertEquals(5, list.size());
        assertArrayEquals(new byte[] { 10, 8, 6, 4, 2 }, list.toArray());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = list.array();
        assertNotNull(arr);
        arr[0] = 99;
        assertEquals(99, list.get(0));
    }

    @Test
    public void test_rangeClosed() {
        ByteList list = ByteList.rangeClosed((byte) 1, (byte) 5);
        assertEquals(5, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, list.toArray());
    }

    @Test
    public void test_rangeClosed_withStep() {
        ByteList list = ByteList.rangeClosed((byte) 0, (byte) 10, (byte) 2);
        assertEquals(6, list.size());
        assertArrayEquals(new byte[] { 0, 2, 4, 6, 8, 10 }, list.toArray());
    }

    @Test
    public void test_repeat() {
        ByteList list = ByteList.repeat((byte) 5, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 5, 5, 5 }, list.toArray());
    }

    @Test
    public void test_repeat_zeroLength() {
        ByteList list = ByteList.repeat((byte) 5, 0);
        assertEquals(0, list.size());
    }

    @Test
    public void test_random() {
        ByteList list = ByteList.random(10);
        assertEquals(10, list.size());
        for (int i = 0; i < list.size(); i++) {
            byte val = list.get(i);
            assertTrue(val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE);
        }
    }

    @Test
    public void test_get() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(10, list.get(0));
        assertEquals(20, list.get(1));
        assertEquals(30, list.get(2));
    }

    @Test
    public void test_get_indexOutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @Test
    public void test_set() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte oldValue = list.set(1, (byte) 25);
        assertEquals(20, oldValue);
        assertEquals(25, list.get(1));
    }

    @Test
    public void test_set_indexOutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, (byte) 0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, (byte) 0));
    }

    @Test
    public void test_add() {
        ByteList list = new ByteList();
        list.add((byte) 1);
        list.add((byte) 2);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    public void test_add_withIndex() {
        ByteList list = ByteList.of((byte) 1, (byte) 3);
        list.add(1, (byte) 2);
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, list.toArray());
    }

    @Test
    public void test_add_withIndex_atEnd() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        list.add(2, (byte) 3);
        assertEquals(3, list.size());
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_add_withIndex_outOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, (byte) 0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, (byte) 0));
    }

    @Test
    public void test_addAll_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2);
        ByteList list2 = ByteList.of((byte) 3, (byte) 4);
        assertTrue(list1.addAll(list2));
        assertEquals(4, list1.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list1.toArray());
    }

    @Test
    public void test_addAll_byteList_empty() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2);
        ByteList list2 = new ByteList();
        assertFalse(list1.addAll(list2));
        assertEquals(2, list1.size());
    }

    @Test
    public void test_addAll_byteList_withIndex() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 4);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3);
        assertTrue(list1.addAll(1, list2));
        assertEquals(4, list1.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list1.toArray());
    }

    @Test
    public void test_addAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        byte[] arr = { 3, 4 };
        assertTrue(list.addAll(arr));
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void test_addAll_array_null() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertFalse(list.addAll((byte[]) null));
        assertEquals(2, list.size());
    }

    @Test
    public void test_addAll_array_withIndex() {
        ByteList list = ByteList.of((byte) 1, (byte) 4);
        byte[] arr = { 2, 3 };
        assertTrue(list.addAll(1, arr));
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void test_remove() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        assertTrue(list.remove((byte) 2));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 2 }, list.toArray());
    }

    @Test
    public void test_remove_notFound() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list.remove((byte) 5));
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeAllOccurrences() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 4, (byte) 2);
        assertTrue(list.removeAllOccurrences((byte) 2));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 4 }, list.toArray());
    }

    @Test
    public void test_removeAllOccurrences_notFound() {
        ByteList list = ByteList.of((byte) 1, (byte) 3, (byte) 4);
        assertFalse(list.removeAllOccurrences((byte) 2));
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeAll_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList toRemove = ByteList.of((byte) 2, (byte) 4);
        assertTrue(list.removeAll(toRemove));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void test_removeAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] toRemove = { 2, 4 };
        assertTrue(list.removeAll(toRemove));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void test_removeIf() {
        ByteList list = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
        assertTrue(list.removeIf(b -> b < 0));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void test_removeIf_noneMatch() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list.removeIf(b -> b < 0));
        assertEquals(3, list.size());
    }

    @Test
    public void test_removeDuplicates() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 2, (byte) 4);
        assertTrue(list.removeDuplicates());
        assertEquals(4, list.size());
        assertTrue(list.contains((byte) 1));
        assertTrue(list.contains((byte) 2));
        assertTrue(list.contains((byte) 3));
        assertTrue(list.contains((byte) 4));
    }

    @Test
    public void test_removeDuplicates_noDuplicates() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void test_retainAll_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList toRetain = ByteList.of((byte) 2, (byte) 4, (byte) 6);
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 2, 4 }, list.toArray());
    }

    @Test
    public void test_retainAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] toRetain = { 2, 4, 6 };
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 2, 4 }, list.toArray());
    }

    @Test
    public void test_delete() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte deleted = list.delete(1);
        assertEquals(20, deleted);
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 10, 30 }, list.toArray());
    }

    @Test
    public void test_delete_indexOutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
    }

    @Test
    public void test_deleteAllByIndices() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50);
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 10, 30, 50 }, list.toArray());
    }

    @Test
    public void test_deleteAllByIndices_empty() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void test_deleteRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 1, 5 }, list.toArray());
    }

    @Test
    public void test_moveRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.moveRange(1, 3, 2);
    }

    @Test
    public void test_replaceRange_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList replacement = ByteList.of((byte) 10, (byte) 11);
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
    }

    @Test
    public void test_replaceRange_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] replacement = { 10, 11 };
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
    }

    @Test
    public void test_replaceAll_values() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
        int count = list.replaceAll((byte) 1, (byte) 9);
        assertEquals(3, count);
        assertArrayEquals(new byte[] { 9, 2, 9, 3, 9 }, list.toArray());
    }

    @Test
    public void test_replaceAll_operator() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.replaceAll(b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 2, 4, 6 }, list.toArray());
    }

    @Test
    public void test_replaceIf() {
        ByteList list = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
        assertTrue(list.replaceIf(b -> b < 0, (byte) 0));
        assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, list.toArray());
    }

    @Test
    public void test_fill() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.fill((byte) 9);
        assertArrayEquals(new byte[] { 9, 9, 9 }, list.toArray());
    }

    @Test
    public void test_fill_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.fill(1, 4, (byte) 9);
        assertArrayEquals(new byte[] { 1, 9, 9, 9, 5 }, list.toArray());
    }

    @Test
    public void test_contains() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.contains((byte) 2));
        assertFalse(list.contains((byte) 5));
    }

    @Test
    public void test_containsAny_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList other = ByteList.of((byte) 3, (byte) 4, (byte) 5);
        assertTrue(list.containsAny(other));
    }

    @Test
    public void test_containsAny_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] other = { 3, 4, 5 };
        assertTrue(list.containsAny(other));
    }

    @Test
    public void test_containsAll_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteList other = ByteList.of((byte) 2, (byte) 3);
        assertTrue(list.containsAll(other));
    }

    @Test
    public void test_containsAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] other = { 2, 3 };
        assertTrue(list.containsAll(other));
    }

    @Test
    public void test_disjoint_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 4, (byte) 5);
        assertTrue(list1.disjoint(list2));

        ByteList list3 = ByteList.of((byte) 3, (byte) 4);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void test_disjoint_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr1 = { 4, 5 };
        assertTrue(list.disjoint(arr1));

        byte[] arr2 = { 3, 4 };
        assertFalse(list.disjoint(arr2));
    }

    @Test
    public void test_intersection_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteList list2 = ByteList.of((byte) 3, (byte) 4, (byte) 5);
        ByteList result = list1.intersection(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 3));
        assertTrue(result.contains((byte) 4));
    }

    @Test
    public void test_intersection_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] arr = { 3, 4, 5 };
        ByteList result = list.intersection(arr);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 3));
        assertTrue(result.contains((byte) 4));
    }

    @Test
    public void test_difference_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteList list2 = ByteList.of((byte) 3, (byte) 4, (byte) 5);
        ByteList result = list1.difference(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 2));
    }

    @Test
    public void test_difference_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] arr = { 3, 4, 5 };
        ByteList result = list.difference(arr);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 2));
    }

    @Test
    public void test_symmetricDifference_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4);
        ByteList result = list1.symmetricDifference(list2);
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 4));
        assertFalse(result.contains((byte) 2));
    }

    @Test
    public void test_symmetricDifference_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = { 2, 3, 4 };
        ByteList result = list.symmetricDifference(arr);
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 4));
    }

    @Test
    public void test_occurrencesOf() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
        assertEquals(3, list.occurrencesOf((byte) 1));
        assertEquals(1, list.occurrencesOf((byte) 2));
        assertEquals(0, list.occurrencesOf((byte) 5));
    }

    @Test
    public void test_indexOf() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(1, list.indexOf((byte) 20));
        assertEquals(-1, list.indexOf((byte) 50));
    }

    @Test
    public void test_indexOf_fromIndex() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(3, list.indexOf((byte) 20, 2));
        assertEquals(-1, list.indexOf((byte) 20, 4));
    }

    @Test
    public void test_lastIndexOf() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(3, list.lastIndexOf((byte) 20));
        assertEquals(-1, list.lastIndexOf((byte) 50));
    }

    @Test
    public void test_lastIndexOf_fromIndex() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(1, list.lastIndexOf((byte) 20, 2));
    }

    @Test
    public void test_min() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsByte());
    }

    @Test
    public void test_min_empty() {
        ByteList list = new ByteList();
        OptionalByte min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void test_min_withRange() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte min = list.min(2, 5);
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsByte());
    }

    @Test
    public void test_max() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5, max.getAsByte());
    }

    @Test
    public void test_max_empty() {
        ByteList list = new ByteList();
        OptionalByte max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void test_max_withRange() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte max = list.max(0, 3);
        assertTrue(max.isPresent());
        assertEquals(4, max.getAsByte());
    }

    @Test
    public void test_median() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        OptionalByte median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3, median.getAsByte());
    }

    @Test
    public void test_median_empty() {
        ByteList list = new ByteList();
        OptionalByte median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    public void test_median_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        OptionalByte median = list.median(0, 3);
        assertTrue(median.isPresent());
    }

    @Test
    public void test_forEach() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        final int[] sum = { 0 };
        list.forEach(b -> sum[0] += b);
        assertEquals(6, sum[0]);
    }

    @Test
    public void test_forEach_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        final int[] sum = { 0 };
        list.forEach(1, 4, b -> sum[0] += b);
        assertEquals(9, sum[0]);
    }

    @Test
    public void test_first() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        OptionalByte first = list.first();
        assertTrue(first.isPresent());
        assertEquals(10, first.getAsByte());
    }

    @Test
    public void test_first_empty() {
        ByteList list = new ByteList();
        OptionalByte first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_last() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        OptionalByte last = list.last();
        assertTrue(last.isPresent());
        assertEquals(30, last.getAsByte());
    }

    @Test
    public void test_last_empty() {
        ByteList list = new ByteList();
        OptionalByte last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_distinct() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 2, (byte) 4);
        ByteList distinct = list.distinct(0, list.size());
        assertEquals(4, distinct.size());
        assertTrue(distinct.contains((byte) 1));
        assertTrue(distinct.contains((byte) 2));
        assertTrue(distinct.contains((byte) 3));
        assertTrue(distinct.contains((byte) 4));
    }

    @Test
    public void test_hasDuplicates() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 1);
        assertTrue(list1.hasDuplicates());

        ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list2.hasDuplicates());
    }

    @Test
    public void test_isSorted() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list1.isSorted());

        ByteList list2 = ByteList.of((byte) 3, (byte) 1, (byte) 2);
        assertFalse(list2.isSorted());
    }

    @Test
    public void test_sort() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        list.sort();
        assertArrayEquals(new byte[] { 1, 1, 3, 4, 5 }, list.toArray());
    }

    @Test
    public void test_parallelSort() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        list.parallelSort();
        assertArrayEquals(new byte[] { 1, 1, 3, 4, 5 }, list.toArray());
    }

    @Test
    public void test_reverseSort() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        list.reverseSort();
        assertArrayEquals(new byte[] { 5, 4, 3, 1, 1 }, list.toArray());
    }

    @Test
    public void test_binarySearch() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        int index = list.binarySearch((byte) 3);
        assertEquals(2, index);
    }

    @Test
    public void test_binarySearch_notFound() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 4, (byte) 5);
        int index = list.binarySearch((byte) 3);
        assertTrue(index < 0);
    }

    @Test
    public void test_binarySearch_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        int index = list.binarySearch(2, 5, (byte) 4);
        assertEquals(3, index);
    }

    @Test
    public void test_reverse() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.reverse();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, list.toArray());
    }

    @Test
    public void test_reverse_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.reverse(1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, list.toArray());
    }

    @Test
    public void test_rotate() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.rotate(2);
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, list.toArray());
    }

    @Test
    public void test_shuffle() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList copy = list.copy();
        list.shuffle();
        assertEquals(5, list.size());
        assertTrue(list.contains((byte) 1));
        assertTrue(list.contains((byte) 2));
        assertTrue(list.contains((byte) 3));
    }

    @Test
    public void test_shuffle_withRandom() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.shuffle(new java.util.Random(42));
        assertEquals(5, list.size());
    }

    @Test
    public void test_swap() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.swap(0, 2);
        assertArrayEquals(new byte[] { 3, 2, 1 }, list.toArray());
    }

    @Test
    public void test_copy() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList copy = list.copy();
        assertNotSame(list, copy);
        assertEquals(list.size(), copy.size());
        assertArrayEquals(list.toArray(), copy.toArray());
    }

    @Test
    public void test_copy_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertArrayEquals(new byte[] { 2, 3, 4 }, copy.toArray());
    }

    @Test
    public void test_copy_withStep() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        ByteList copy = list.copy(0, 6, 2);
        assertEquals(3, copy.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, copy.toArray());
    }

    @Test
    public void test_split() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<ByteList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void test_trimToSize() {
        ByteList list = new ByteList(100);
        list.add((byte) 1);
        list.add((byte) 2);
        ByteList result = list.trimToSize();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void test_clear() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_isEmpty() {
        ByteList list = new ByteList();
        assertTrue(list.isEmpty());
        list.add((byte) 1);
        assertFalse(list.isEmpty());
    }

    @Test
    public void test_size() {
        ByteList list = new ByteList();
        assertEquals(0, list.size());
        list.add((byte) 1);
        assertEquals(1, list.size());
    }

    @Test
    public void test_boxed() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Byte.valueOf((byte) 1), boxed.get(0));
        assertEquals(Byte.valueOf((byte) 2), boxed.get(1));
        assertEquals(Byte.valueOf((byte) 3), boxed.get(2));
    }

    @Test
    public void test_boxed_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Byte.valueOf((byte) 2), boxed.get(0));
    }

    @Test
    public void test_toArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = list.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, arr);
        arr[0] = 99;
        assertEquals(1, list.get(0));
    }

    @Test
    public void test_toIntList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        IntList intList = list.toIntList();
        assertEquals(3, intList.size());
        assertEquals(1, intList.get(0));
        assertEquals(2, intList.get(1));
        assertEquals(3, intList.get(2));
    }

    @Test
    public void test_toCollection() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> collection = list.toCollection(0, 3, ArrayList::new);
        assertEquals(3, collection.size());
        assertTrue(collection instanceof ArrayList);
    }

    @Test
    public void test_toMultiset() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        Multiset<Byte> multiset = list.toMultiset(0, 4, Multiset::new);
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(Byte.valueOf((byte) 1)));
    }

    @Test
    public void test_iterator() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
        assertEquals(2, iter.nextByte());
        assertEquals(3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_stream() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream stream = list.stream();
        assertNotNull(stream);
        assertEquals(15, stream.sum());
    }

    @Test
    public void test_stream_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream stream = list.stream(1, 4);
        assertNotNull(stream);
        assertEquals(9, stream.sum());
    }

    @Test
    public void test_getFirst() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(10, list.getFirst());
    }

    @Test
    public void test_getFirst_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void test_getLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(30, list.getLast());
    }

    @Test
    public void test_getLast_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void test_addFirst() {
        ByteList list = ByteList.of((byte) 20, (byte) 30);
        list.addFirst((byte) 10);
        assertEquals(3, list.size());
        assertEquals(10, list.get(0));
    }

    @Test
    public void test_addLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);
        list.addLast((byte) 30);
        assertEquals(3, list.size());
        assertEquals(30, list.get(2));
    }

    @Test
    public void test_removeFirst() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte removed = list.removeFirst();
        assertEquals(10, removed);
        assertEquals(2, list.size());
        assertEquals(20, list.get(0));
    }

    @Test
    public void test_removeFirst_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void test_removeLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte removed = list.removeLast();
        assertEquals(30, removed);
        assertEquals(2, list.size());
        assertEquals(20, list.get(1));
    }

    @Test
    public void test_removeLast_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void test_hashCode() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void test_equals() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list3 = ByteList.of((byte) 1, (byte) 2, (byte) 4);

        assertTrue(list1.equals(list2));
        assertFalse(list1.equals(list3));
        assertFalse(list1.equals(null));
    }

    @Test
    public void test_toString() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void test_toString_empty() {
        ByteList list = new ByteList();
        String str = list.toString();
        assertNotNull(str);
    }
}
