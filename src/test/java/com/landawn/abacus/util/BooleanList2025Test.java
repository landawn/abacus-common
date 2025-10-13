package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class BooleanList2025Test extends TestBase {

    @Test
    public void test_constructor_empty() {
        BooleanList list = new BooleanList();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity() {
        BooleanList list = new BooleanList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity_zero() {
        BooleanList list = new BooleanList(0);
        assertEquals(0, list.size());
    }

    @Test
    public void test_constructor_withCapacity_negative() {
        assertThrows(IllegalArgumentException.class, () -> new BooleanList(-1));
    }

    @Test
    public void test_constructor_withArray() {
        boolean[] arr = { true, false, true };
        BooleanList list = new BooleanList(arr);
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_constructor_withArray_empty() {
        boolean[] arr = {};
        BooleanList list = new BooleanList(arr);
        assertEquals(0, list.size());
    }

    @Test
    public void test_constructor_withArrayAndSize() {
        boolean[] arr = { true, false, true, false };
        BooleanList list = new BooleanList(arr, 2);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_constructor_withArrayAndSize_invalid() {
        boolean[] arr = { true, false };
        assertThrows(IndexOutOfBoundsException.class, () -> new BooleanList(arr, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> new BooleanList(arr, -1));
    }

    @Test
    public void test_of_varargs() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_of_varargs_empty() {
        BooleanList list = BooleanList.of();
        assertEquals(0, list.size());
    }

    @Test
    public void test_of_varargs_null() {
        BooleanList list = BooleanList.of((boolean[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void test_of_withSize() {
        boolean[] arr = { true, false, true, false };
        BooleanList list = BooleanList.of(arr, 2);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_of_withSize_null() {
        BooleanList list = BooleanList.of(null, 0);
        assertEquals(0, list.size());
    }

    @Test
    public void test_of_withSize_invalid() {
        boolean[] arr = { true, false };
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.of(arr, 3));
    }

    @Test
    public void test_copyOf() {
        boolean[] arr = { true, false, true };
        BooleanList list = BooleanList.copyOf(arr);
        assertEquals(3, list.size());
        arr[0] = false;
        assertTrue(list.get(0));
    }

    @Test
    public void test_copyOf_null() {
        BooleanList list = BooleanList.copyOf((boolean[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void test_copyOf_withRange() {
        boolean[] arr = { true, false, true, false };
        BooleanList list = BooleanList.copyOf(arr, 1, 3);
        assertEquals(2, list.size());
        assertFalse(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void test_repeat() {
        BooleanList list = BooleanList.repeat(true, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(list.get(i));
        }
    }

    @Test
    public void test_repeat_zero() {
        BooleanList list = BooleanList.repeat(true, 0);
        assertEquals(0, list.size());
    }

    @Test
    public void test_random() {
        BooleanList list = BooleanList.random(10);
        assertEquals(10, list.size());
    }

    @Test
    public void test_random_zero() {
        BooleanList list = BooleanList.random(0);
        assertEquals(0, list.size());
    }

    @Test
    public void test_array() {
        boolean[] arr = { true, false };
        BooleanList list = new BooleanList(arr);
        assertSame(arr, list.array());
    }

    @Test
    public void test_get() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_get_outOfBounds() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void test_set() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean old = list.set(1, true);
        assertFalse(old);
        assertTrue(list.get(1));
    }

    @Test
    public void test_set_outOfBounds() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, true));
    }

    @Test
    public void test_add() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_add_withIndex() {
        BooleanList list = BooleanList.of(true, false);
        list.add(1, true);
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
    }

    @Test
    public void test_add_withIndex_atBeginning() {
        BooleanList list = BooleanList.of(true, false);
        list.add(0, false);
        assertEquals(3, list.size());
        assertFalse(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
    }

    @Test
    public void test_add_withIndex_atEnd() {
        BooleanList list = BooleanList.of(true, false);
        list.add(2, true);
        assertEquals(3, list.size());
        assertTrue(list.get(2));
    }

    @Test
    public void test_add_withIndex_invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, true));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, true));
    }

    @Test
    public void test_addAll_BooleanList() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(true, true);
        boolean result = list1.addAll(list2);
        assertTrue(result);
        assertEquals(4, list1.size());
        assertTrue(list1.get(2));
        assertTrue(list1.get(3));
    }

    @Test
    public void test_addAll_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        boolean result = list1.addAll(list2);
        assertFalse(result);
        assertEquals(2, list1.size());
    }

    @Test
    public void test_addAll_BooleanList_withIndex() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(true, true);
        boolean result = list1.addAll(1, list2);
        assertTrue(result);
        assertEquals(4, list1.size());
        assertTrue(list1.get(0));
        assertTrue(list1.get(1));
        assertTrue(list1.get(2));
        assertFalse(list1.get(3));
    }

    @Test
    public void test_addAll_BooleanList_withIndex_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        boolean result = list1.addAll(1, list2);
        assertFalse(result);
        assertEquals(2, list1.size());
    }

    @Test
    public void test_addAll_BooleanList_withIndex_invalid() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(true);
        assertThrows(IndexOutOfBoundsException.class, () -> list1.addAll(3, list2));
    }

    @Test
    public void test_addAll_array() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = { true, true };
        boolean result = list.addAll(arr);
        assertTrue(result);
        assertEquals(4, list.size());
    }

    @Test
    public void test_addAll_array_empty() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = {};
        boolean result = list.addAll(arr);
        assertFalse(result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_addAll_array_withIndex() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = { true, true };
        boolean result = list.addAll(1, arr);
        assertTrue(result);
        assertEquals(4, list.size());
        assertTrue(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_remove_element() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean result = list.remove(false);
        assertTrue(result);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void test_remove_element_notFound() {
        BooleanList list = BooleanList.of(true, true);
        boolean result = list.remove(false);
        assertFalse(result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeAllOccurrences() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        boolean result = list.removeAllOccurrences(true);
        assertTrue(result);
        assertEquals(2, list.size());
        assertFalse(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_removeAllOccurrences_notFound() {
        BooleanList list = BooleanList.of(true, true);
        boolean result = list.removeAllOccurrences(false);
        assertFalse(result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeAll_BooleanList() {
        BooleanList list1 = BooleanList.of(true, false, true, false);
        BooleanList list2 = BooleanList.of(true);
        boolean result = list1.removeAll(list2);
        assertTrue(result);
        assertEquals(2, list1.size());
        assertFalse(list1.get(0));
        assertFalse(list1.get(1));
    }

    @Test
    public void test_removeAll_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        boolean result = list1.removeAll(list2);
        assertFalse(result);
        assertEquals(2, list1.size());
    }

    @Test
    public void test_removeAll_array() {
        BooleanList list = BooleanList.of(true, false, true, false);
        boolean[] arr = { true };
        boolean result = list.removeAll(arr);
        assertTrue(result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeIf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        boolean result = list.removeIf(b -> b);
        assertTrue(result);
        assertEquals(2, list.size());
        assertFalse(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_removeIf_noMatch() {
        BooleanList list = BooleanList.of(true, true);
        boolean result = list.removeIf(b -> !b);
        assertFalse(result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeDuplicates() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_removeDuplicates_allSame() {
        BooleanList list = BooleanList.of(true, true, true);
        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(1, list.size());
        assertTrue(list.get(0));
    }

    @Test
    public void test_removeDuplicates_noDuplicates() {
        BooleanList list = BooleanList.of(true, false);
        boolean result = list.removeDuplicates();
        assertFalse(result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeDuplicates_sizeOne() {
        BooleanList list = BooleanList.of(true);
        boolean result = list.removeDuplicates();
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void test_removeDuplicates_sizeTwo_same() {
        BooleanList list = BooleanList.of(true, true);
        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(1, list.size());
    }

    @Test
    public void test_retainAll_BooleanList() {
        BooleanList list1 = BooleanList.of(true, false, true, false);
        BooleanList list2 = BooleanList.of(true);
        boolean result = list1.retainAll(list2);
        assertTrue(result);
        assertEquals(2, list1.size());
        assertTrue(list1.get(0));
        assertTrue(list1.get(1));
    }

    @Test
    public void test_retainAll_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        boolean result = list1.retainAll(list2);
        assertTrue(result);
        assertEquals(0, list1.size());
    }

    @Test
    public void test_retainAll_array() {
        BooleanList list = BooleanList.of(true, false, true, false);
        boolean[] arr = { true };
        boolean result = list.retainAll(arr);
        assertTrue(result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_delete() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean deleted = list.delete(1);
        assertFalse(deleted);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void test_delete_outOfBounds() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
    }

    @Test
    public void test_deleteAllByIndices() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_deleteAllByIndices_empty() {
        BooleanList list = BooleanList.of(true, false);
        list.deleteAllByIndices();
        assertEquals(2, list.size());
    }

    @Test
    public void test_deleteRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void test_deleteRange_sameIndices() {
        BooleanList list = BooleanList.of(true, false);
        list.deleteRange(1, 1);
        assertEquals(2, list.size());
    }

    @Test
    public void test_deleteRange_invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(2, 1));
    }

    @Test
    public void test_moveRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.moveRange(1, 3, 2); // true, false, false, true, true
        assertEquals(BooleanList.of(true, false, false, true, true), list);
    }

    @Test
    public void test_replaceRange_BooleanList() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        BooleanList replacement = BooleanList.of(false, false);
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertFalse(list.get(2));
        assertTrue(list.get(3));
    }

    @Test
    public void test_replaceRange_BooleanList_empty() {
        BooleanList list = BooleanList.of(true, false, true, false);
        BooleanList replacement = new BooleanList();
        list.replaceRange(1, 3, replacement);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_replaceRange_array() {
        BooleanList list = BooleanList.of(true, false, true, false);
        boolean[] replacement = { false, false };
        list.replaceRange(1, 3, replacement);
        assertEquals(4, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertFalse(list.get(2));
        assertFalse(list.get(3));
    }

    @Test
    public void test_replaceAll_oldNew() {
        BooleanList list = BooleanList.of(true, false, true, false);
        int count = list.replaceAll(true, false);
        assertEquals(2, count);
        assertEquals(4, list.size());
        assertFalse(list.get(0));
        assertFalse(list.get(1));
        assertFalse(list.get(2));
        assertFalse(list.get(3));
    }

    @Test
    public void test_replaceAll_oldNew_empty() {
        BooleanList list = new BooleanList();
        int count = list.replaceAll(true, false);
        assertEquals(0, count);
    }

    @Test
    public void test_replaceAll_operator() {
        BooleanList list = BooleanList.of(true, false, true);
        list.replaceAll(b -> !b);
        assertFalse(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
    }

    @Test
    public void test_replaceIf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        boolean result = list.replaceIf(b -> b, false);
        assertTrue(result);
        assertFalse(list.get(0));
        assertFalse(list.get(1));
        assertFalse(list.get(2));
        assertFalse(list.get(3));
    }

    @Test
    public void test_replaceIf_noMatch() {
        BooleanList list = BooleanList.of(false, false);
        boolean result = list.replaceIf(b -> b, false);
        assertFalse(result);
    }

    @Test
    public void test_fill() {
        BooleanList list = BooleanList.of(true, false, true);
        list.fill(false);
        assertFalse(list.get(0));
        assertFalse(list.get(1));
        assertFalse(list.get(2));
    }

    @Test
    public void test_fill_withRange() {
        BooleanList list = BooleanList.of(true, true, true, true);
        list.fill(1, 3, false);
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertFalse(list.get(2));
        assertTrue(list.get(3));
    }

    @Test
    public void test_fill_withRange_invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(0, 3, true));
    }

    @Test
    public void test_contains() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.contains(true));
        assertTrue(list.contains(false));
    }

    @Test
    public void test_contains_notFound() {
        BooleanList list = BooleanList.of(true, true);
        assertFalse(list.contains(false));
    }

    @Test
    public void test_containsAny_BooleanList() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(false, false);
        assertTrue(list1.containsAny(list2));
    }

    @Test
    public void test_containsAny_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        assertFalse(list1.containsAny(list2));
    }

    @Test
    public void test_containsAny_array() {
        BooleanList list = BooleanList.of(true, true);
        boolean[] arr = { false };
        assertFalse(list.containsAny(arr));
    }

    @Test
    public void test_containsAll_BooleanList() {
        BooleanList list1 = BooleanList.of(true, false, true);
        BooleanList list2 = BooleanList.of(true, false);
        assertTrue(list1.containsAll(list2));
    }

    @Test
    public void test_containsAll_BooleanList_false() {
        BooleanList list1 = BooleanList.of(true, true);
        BooleanList list2 = BooleanList.of(false);
        assertFalse(list1.containsAll(list2));
    }

    @Test
    public void test_containsAll_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true);
        BooleanList list2 = new BooleanList();
        assertTrue(list1.containsAll(list2));
    }

    @Test
    public void test_containsAll_array() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = { true };
        assertTrue(list.containsAll(arr));
    }

    @Test
    public void test_disjoint_BooleanList() {
        BooleanList list1 = BooleanList.of(true, true);
        BooleanList list2 = BooleanList.of(false, false);
        assertTrue(list1.disjoint(list2));
    }

    @Test
    public void test_disjoint_BooleanList_notDisjoint() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(false);
        assertFalse(list1.disjoint(list2));
    }

    @Test
    public void test_disjoint_array() {
        BooleanList list = BooleanList.of(true, true);
        boolean[] arr = { false };
        assertTrue(list.disjoint(arr));
    }

    @Test
    public void test_intersection_BooleanList() {
        BooleanList list1 = BooleanList.of(true, false, true, false);
        BooleanList list2 = BooleanList.of(true, true, false);
        BooleanList result = list1.intersection(list2);
        assertEquals(3, result.size());
        assertTrue(result.get(0));
        assertFalse(result.get(1));
        assertTrue(result.get(2));
    }

    @Test
    public void test_intersection_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        BooleanList result = list1.intersection(list2);
        assertEquals(0, result.size());
    }

    @Test
    public void test_intersection_array() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean[] arr = { true };
        BooleanList result = list.intersection(arr);
        assertEquals(1, result.size());
        assertTrue(result.get(0));
    }

    @Test
    public void test_difference_BooleanList() {
        BooleanList list1 = BooleanList.of(true, true, false, true);
        BooleanList list2 = BooleanList.of(true, false);
        BooleanList result = list1.difference(list2);
        assertEquals(2, result.size());
        assertTrue(result.get(0));
        assertTrue(result.get(1));
    }

    @Test
    public void test_difference_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        BooleanList result = list1.difference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void test_difference_array() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean[] arr = { false };
        BooleanList result = list.difference(arr);
        assertEquals(2, result.size());
    }

    @Test
    public void test_symmetricDifference_BooleanList() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);
        BooleanList result = list1.symmetricDifference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void test_symmetricDifference_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        BooleanList result = list1.symmetricDifference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void test_symmetricDifference_BooleanList_thisEmpty() {
        BooleanList list1 = new BooleanList();
        BooleanList list2 = BooleanList.of(true, false);
        BooleanList result = list1.symmetricDifference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void test_symmetricDifference_array() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = { false, false };
        BooleanList result = list.symmetricDifference(arr);
        assertEquals(2, result.size());
    }

    @Test
    public void test_occurrencesOf() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        assertEquals(3, list.occurrencesOf(true));
        assertEquals(2, list.occurrencesOf(false));
    }

    @Test
    public void test_occurrencesOf_empty() {
        BooleanList list = new BooleanList();
        assertEquals(0, list.occurrencesOf(true));
    }

    @Test
    public void test_indexOf() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(0, list.indexOf(true));
        assertEquals(1, list.indexOf(false));
    }

    @Test
    public void test_indexOf_notFound() {
        BooleanList list = BooleanList.of(true, true);
        assertEquals(-1, list.indexOf(false));
    }

    @Test
    public void test_indexOf_withFromIndex() {
        BooleanList list = BooleanList.of(true, false, true, false);
        assertEquals(2, list.indexOf(true, 1));
    }

    @Test
    public void test_indexOf_withFromIndex_negative() {
        BooleanList list = BooleanList.of(true, false);
        assertEquals(0, list.indexOf(true, -1));
    }

    @Test
    public void test_indexOf_withFromIndex_beyondSize() {
        BooleanList list = BooleanList.of(true, false);
        assertEquals(-1, list.indexOf(true, 2));
    }

    @Test
    public void test_lastIndexOf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        assertEquals(2, list.lastIndexOf(true));
        assertEquals(3, list.lastIndexOf(false));
    }

    @Test
    public void test_lastIndexOf_notFound() {
        BooleanList list = BooleanList.of(true, true);
        assertEquals(-1, list.lastIndexOf(false));
    }

    @Test
    public void test_lastIndexOf_withStartIndex() {
        BooleanList list = BooleanList.of(true, false, true, false);
        assertEquals(0, list.lastIndexOf(true, 1));
    }

    @Test
    public void test_lastIndexOf_withStartIndex_negative() {
        BooleanList list = BooleanList.of(true, false);
        assertEquals(-1, list.lastIndexOf(true, -1));
    }

    @Test
    public void test_lastIndexOf_withStartIndex_beyondSize() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(2, list.lastIndexOf(true, 10));
    }

    @Test
    public void test_forEach() {
        BooleanList list = BooleanList.of(true, false, true);
        List<Boolean> collected = new ArrayList<>();
        list.forEach(collected::add);
        assertEquals(3, collected.size());
        assertTrue(collected.get(0));
        assertFalse(collected.get(1));
        assertTrue(collected.get(2));
    }

    @Test
    public void test_forEach_withRange() {
        BooleanList list = BooleanList.of(true, false, true, false);
        List<Boolean> collected = new ArrayList<>();
        list.forEach(1, 3, collected::add);
        assertEquals(2, collected.size());
        assertFalse(collected.get(0));
        assertTrue(collected.get(1));
    }

    @Test
    public void test_forEach_withRange_reverse() {
        BooleanList list = BooleanList.of(true, false, true, false);
        List<Boolean> collected = new ArrayList<>();
        list.forEach(3, 1, collected::add);
        assertEquals(2, collected.size());
        assertFalse(collected.get(0));
        assertTrue(collected.get(1));
    }

    @Test
    public void test_forEach_withRange_minusOne() {
        BooleanList list = BooleanList.of(true, false, true);
        List<Boolean> collected = new ArrayList<>();
        list.forEach(2, -1, collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void test_first() {
        BooleanList list = BooleanList.of(true, false);
        OptionalBoolean first = list.first();
        assertTrue(first.isPresent());
        assertTrue(first.get());
    }

    @Test
    public void test_first_empty() {
        BooleanList list = new BooleanList();
        OptionalBoolean first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_last() {
        BooleanList list = BooleanList.of(true, false);
        OptionalBoolean last = list.last();
        assertTrue(last.isPresent());
        assertFalse(last.get());
    }

    @Test
    public void test_last_empty() {
        BooleanList list = new BooleanList();
        OptionalBoolean last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_distinct() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        BooleanList distinct = list.distinct(0, 5);
        assertEquals(2, distinct.size());
        assertTrue(distinct.get(0));
        assertFalse(distinct.get(1));
    }

    @Test
    public void test_distinct_singleElement() {
        BooleanList list = BooleanList.of(true);
        BooleanList distinct = list.distinct(0, 1);
        assertEquals(1, distinct.size());
    }

    @Test
    public void test_hasDuplicates() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void test_hasDuplicates_noDuplicates() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.hasDuplicates());
    }

    @Test
    public void test_hasDuplicates_empty() {
        BooleanList list = new BooleanList();
        assertFalse(list.hasDuplicates());
    }

    @Test
    public void test_hasDuplicates_sizeOne() {
        BooleanList list = BooleanList.of(true);
        assertFalse(list.hasDuplicates());
    }

    @Test
    public void test_hasDuplicates_sizeTwo_same() {
        BooleanList list = BooleanList.of(true, true);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void test_isSorted() {
        BooleanList list = BooleanList.of(false, false, true, true);
        assertTrue(list.isSorted());
    }

    @Test
    public void test_isSorted_unsorted() {
        BooleanList list = BooleanList.of(true, false, true);
        assertFalse(list.isSorted());
    }

    @Test
    public void test_isSorted_empty() {
        BooleanList list = new BooleanList();
        assertTrue(list.isSorted());
    }

    @Test
    public void test_sort() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.sort();
        assertFalse(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
        assertTrue(list.get(3));
    }

    @Test
    public void test_sort_empty() {
        BooleanList list = new BooleanList();
        list.sort();
        assertEquals(0, list.size());
    }

    @Test
    public void test_sort_singleElement() {
        BooleanList list = BooleanList.of(true);
        list.sort();
        assertEquals(1, list.size());
        assertTrue(list.get(0));
    }

    @Test
    public void test_reverseSort() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.reverseSort();
        assertTrue(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
        assertFalse(list.get(3));
    }

    @Test
    public void test_reverseSort_singleElement() {
        BooleanList list = BooleanList.of(true);
        list.reverseSort();
        assertEquals(1, list.size());
    }

    @Test
    public void test_reverse() {
        BooleanList list = BooleanList.of(true, false, true);
        list.reverse();
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_reverse_singleElement() {
        BooleanList list = BooleanList.of(true);
        list.reverse();
        assertEquals(1, list.size());
    }

    @Test
    public void test_reverse_withRange() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.reverse(1, 3);
        assertTrue(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
        assertFalse(list.get(3));
    }

    @Test
    public void test_rotate() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.rotate(1);
        assertFalse(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
        assertTrue(list.get(3));
    }

    @Test
    public void test_rotate_negative() {
        BooleanList list = BooleanList.of(true, false, true);
        list.rotate(-1);
        assertFalse(list.get(0));
        assertTrue(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_rotate_singleElement() {
        BooleanList list = BooleanList.of(true);
        list.rotate(1);
        assertEquals(1, list.size());
    }

    @Test
    public void test_shuffle() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.shuffle();
        assertEquals(5, list.size());
    }

    @Test
    public void test_shuffle_withRandom() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.shuffle(new Random(123));
        assertEquals(4, list.size());
    }

    @Test
    public void test_shuffle_singleElement() {
        BooleanList list = BooleanList.of(true);
        list.shuffle();
        assertEquals(1, list.size());
    }

    @Test
    public void test_swap() {
        BooleanList list = BooleanList.of(true, false, true);
        list.swap(0, 1);
        assertFalse(list.get(0));
        assertTrue(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_swap_sameIndex() {
        BooleanList list = BooleanList.of(true, false);
        list.swap(0, 0);
        assertTrue(list.get(0));
    }

    @Test
    public void test_swap_invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 2));
    }

    @Test
    public void test_copy() {
        BooleanList list = BooleanList.of(true, false, true);
        BooleanList copy = list.copy();
        assertEquals(3, copy.size());
        assertTrue(copy.get(0));
        list.set(0, false);
        assertTrue(copy.get(0));
    }

    @Test
    public void test_copy_withRange() {
        BooleanList list = BooleanList.of(true, false, true, false);
        BooleanList copy = list.copy(1, 3);
        assertEquals(2, copy.size());
        assertFalse(copy.get(0));
        assertTrue(copy.get(1));
    }

    @Test
    public void test_copy_withStep() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        BooleanList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertTrue(copy.get(0));
        assertTrue(copy.get(1));
        assertTrue(copy.get(2));
    }

    @Test
    public void test_copy_withStep_reverse() {
        BooleanList list = BooleanList.of(true, false, true, false);
        BooleanList copy = list.copy(3, -1, -1);
        assertEquals(4, copy.size());
    }

    @Test
    public void test_split() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        List<BooleanList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void test_split_invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IllegalArgumentException.class, () -> list.split(0, 2, 0));
    }

    @Test
    public void test_trimToSize() {
        BooleanList list = new BooleanList(10);
        list.add(true);
        list.add(false);
        BooleanList result = list.trimToSize();
        assertSame(list, result);
        assertEquals(2, list.size());
    }

    @Test
    public void test_clear() {
        BooleanList list = BooleanList.of(true, false, true);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_isEmpty() {
        BooleanList list = new BooleanList();
        assertTrue(list.isEmpty());
        list.add(true);
        assertFalse(list.isEmpty());
    }

    @Test
    public void test_size() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(3, list.size());
    }

    @Test
    public void test_boxed() {
        BooleanList list = BooleanList.of(true, false, true);
        List<Boolean> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertTrue(boxed.get(0));
        assertFalse(boxed.get(1));
        assertTrue(boxed.get(2));
    }

    @Test
    public void test_boxed_withRange() {
        BooleanList list = BooleanList.of(true, false, true, false);
        List<Boolean> boxed = list.boxed(1, 3);
        assertEquals(2, boxed.size());
        assertFalse(boxed.get(0));
        assertTrue(boxed.get(1));
    }

    @Test
    public void test_toArray() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean[] arr = list.toArray();
        assertEquals(3, arr.length);
        assertTrue(arr[0]);
        assertFalse(arr[1]);
        assertTrue(arr[2]);
    }

    @Test
    public void test_toCollection() {
        BooleanList list = BooleanList.of(true, false, true);
        List<Boolean> collection = list.toCollection(0, 3, ArrayList::new);
        assertEquals(3, collection.size());
        assertTrue(collection.get(0));
    }

    @Test
    public void test_toMultiset() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        Multiset<Boolean> multiset = list.toMultiset(0, 5, Multiset::new);
        assertEquals(3, multiset.get(true));
        assertEquals(2, multiset.get(false));
    }

    @Test
    public void test_iterator() {
        BooleanList list = BooleanList.of(true, false, true);
        BooleanIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertTrue(iter.next());
        assertTrue(iter.hasNext());
        assertFalse(iter.next());
        assertTrue(iter.hasNext());
        assertTrue(iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_iterator_empty() {
        BooleanList list = new BooleanList();
        BooleanIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void test_stream() {
        BooleanList list = BooleanList.of(true, false, true);
        Stream<Boolean> stream = list.stream();
        assertEquals(3, stream.count());
    }

    @Test
    public void test_stream_withRange() {
        BooleanList list = BooleanList.of(true, false, true, false);
        Stream<Boolean> stream = list.stream(1, 3);
        assertEquals(2, stream.count());
    }

    @Test
    public void test_getFirst() {
        BooleanList list = BooleanList.of(true, false);
        assertTrue(list.getFirst());
    }

    @Test
    public void test_getFirst_empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void test_getLast() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.getLast());
    }

    @Test
    public void test_getLast_empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void test_addFirst() {
        BooleanList list = BooleanList.of(true, false);
        list.addFirst(false);
        assertEquals(3, list.size());
        assertFalse(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
    }

    @Test
    public void test_addLast() {
        BooleanList list = BooleanList.of(true, false);
        list.addLast(true);
        assertEquals(3, list.size());
        assertTrue(list.get(2));
    }

    @Test
    public void test_removeFirst() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean removed = list.removeFirst();
        assertTrue(removed);
        assertEquals(2, list.size());
        assertFalse(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void test_removeFirst_empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void test_removeLast() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean removed = list.removeLast();
        assertTrue(removed);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_removeLast_empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void test_hashCode_sameContent() {
        BooleanList list1 = BooleanList.of(true, false, true);
        BooleanList list2 = BooleanList.of(true, false, true);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void test_equals_same() {
        BooleanList list = BooleanList.of(true, false);
        assertTrue(list.equals(list));
    }

    @Test
    public void test_equals_sameContent() {
        BooleanList list1 = BooleanList.of(true, false, true);
        BooleanList list2 = BooleanList.of(true, false, true);
        assertTrue(list1.equals(list2));
    }

    @Test
    public void test_equals_differentContent() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(false, true);
        assertFalse(list1.equals(list2));
    }

    @Test
    public void test_equals_differentSize() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(true);
        assertFalse(list1.equals(list2));
    }

    @Test
    public void test_equals_null() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.equals(null));
    }

    @Test
    public void test_equals_differentType() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.equals("not a list"));
    }

    @Test
    public void test_toString() {
        BooleanList list = BooleanList.of(true, false, true);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("true"));
        assertTrue(str.contains("false"));
    }

    @Test
    public void test_toString_empty() {
        BooleanList list = new BooleanList();
        String str = list.toString();
        assertNotNull(str);
    }
}
