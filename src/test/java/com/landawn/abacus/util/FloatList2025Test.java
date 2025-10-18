package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatStream;

@Tag("2025")
public class FloatList2025Test extends TestBase {

    @Test
    public void testOf_varargs() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testOf_emptyArray() {
        FloatList list = FloatList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOf_nullArray() {
        FloatList list = FloatList.of((float[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testOf_arrayWithSize() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        FloatList list = FloatList.of(arr, 3);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testOf_arrayWithSize_invalidSize() {
        float[] arr = { 1.0f, 2.0f };
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.of(arr, 5));
    }

    @Test
    public void testCopyOf() {
        float[] arr = { 1.5f, 2.5f, 3.5f };
        FloatList list = FloatList.copyOf(arr);
        assertEquals(3, list.size());

        arr[0] = 99.0f;
        assertEquals(1.5f, list.get(0), 0.0001f);
    }

    @Test
    public void testCopyOf_nullArray() {
        FloatList list = FloatList.copyOf((float[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testCopyOf_range() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatList list = FloatList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRepeat() {
        FloatList list = FloatList.repeat(5.5f, 4);
        assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(5.5f, list.get(i), 0.0001f);
        }
    }

    @Test
    public void testRepeat_zeroLength() {
        FloatList list = FloatList.repeat(5.5f, 0);
        assertEquals(0, list.size());
    }

    @Test
    public void testRandom() {
        FloatList list = FloatList.random(5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(list.get(i) >= 0.0f && list.get(i) < 1.0f);
        }
    }

    @Test
    public void testArray() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = list.array();
        assertNotNull(arr);
    }

    @Test
    public void testGet() {
        FloatList list = FloatList.of(10.5f, 20.5f, 30.5f);
        assertEquals(10.5f, list.get(0), 0.0001f);
        assertEquals(20.5f, list.get(1), 0.0001f);
        assertEquals(30.5f, list.get(2), 0.0001f);
    }

    @Test
    public void testGet_outOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void testSet() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float oldValue = list.set(1, 99.9f);
        assertEquals(2.0f, oldValue, 0.0001f);
        assertEquals(99.9f, list.get(1), 0.0001f);
    }

    @Test
    public void testSet_outOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(5, 10.0f));
    }

    @Test
    public void testAdd() {
        FloatList list = new FloatList();
        list.add(1.0f);
        list.add(2.0f);
        assertEquals(2, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testAdd_atIndex() {
        FloatList list = FloatList.of(1.0f, 3.0f);
        list.add(1, 2.0f);
        assertEquals(3, list.size());
        assertEquals(2.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testAdd_atIndex_outOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(5, 10.0f));
    }

    @Test
    public void testAddAll_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = FloatList.of(3.0f, 4.0f);
        assertTrue(list1.addAll(list2));
        assertEquals(4, list1.size());
        assertEquals(3.0f, list1.get(2), 0.0001f);
    }

    @Test
    public void testAddAll_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertFalse(list1.addAll(list2));
        assertEquals(2, list1.size());
    }

    @Test
    public void testAddAll_atIndex_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 4.0f);
        FloatList list2 = FloatList.of(2.0f, 3.0f);
        assertTrue(list1.addAll(1, list2));
        assertEquals(4, list1.size());
        assertEquals(2.0f, list1.get(1), 0.0001f);
        assertEquals(3.0f, list1.get(2), 0.0001f);
    }

    @Test
    public void testAddAll_array() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        float[] arr = { 3.0f, 4.0f };
        assertTrue(list.addAll(arr));
        assertEquals(4, list.size());
    }

    @Test
    public void testAddAll_array_null() {
        FloatList list = FloatList.of(1.0f);
        assertFalse(list.addAll((float[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAll_atIndex_array() {
        FloatList list = FloatList.of(1.0f, 4.0f);
        float[] arr = { 2.0f, 3.0f };
        assertTrue(list.addAll(1, arr));
        assertEquals(4, list.size());
        assertEquals(2.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testRemove_value() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        assertTrue(list.remove(2.0f));
        assertEquals(3, list.size());
        assertEquals(3.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testRemove_value_notFound() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertFalse(list.remove(99.0f));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAllOccurrences() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f, 4.0f, 2.0f);
        assertTrue(list.removeAllOccurrences(2.0f));
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(3.0f, list.get(1), 0.0001f);
        assertEquals(4.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRemoveAllOccurrences_notFound() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertFalse(list.removeAllOccurrences(99.0f));
    }

    @Test
    public void testRemoveAll_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        FloatList list2 = FloatList.of(2.0f, 4.0f);
        assertTrue(list1.removeAll(list2));
        assertEquals(2, list1.size());
        assertEquals(1.0f, list1.get(0), 0.0001f);
        assertEquals(3.0f, list1.get(1), 0.0001f);
    }

    @Test
    public void testRemoveAll_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        float[] arr = { 2.0f, 4.0f };
        assertTrue(list.removeAll(arr));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveIf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertTrue(list.removeIf(f -> f > 3.0f));
        assertEquals(3, list.size());
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRemoveIf_noneMatch() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(list.removeIf(f -> f > 100.0f));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicates() {
        FloatList list = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 2.0f);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRemoveDuplicates_sorted() {
        FloatList list = FloatList.of(1.0f, 1.0f, 2.0f, 2.0f, 3.0f);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicates_noDuplicates() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRetainAll_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        FloatList list2 = FloatList.of(2.0f, 4.0f, 5.0f);
        assertTrue(list1.retainAll(list2));
        assertEquals(2, list1.size());
        assertEquals(2.0f, list1.get(0), 0.0001f);
        assertEquals(4.0f, list1.get(1), 0.0001f);
    }

    @Test
    public void testRetainAll_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertTrue(list1.retainAll(list2));
        assertEquals(0, list1.size());
    }

    @Test
    public void testRetainAll_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        float[] arr = { 2.0f, 4.0f };
        assertTrue(list.retainAll(arr));
        assertEquals(2, list.size());
    }

    @Test
    public void testDelete() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float removed = list.delete(1);
        assertEquals(2.0f, removed, 0.0001f);
        assertEquals(2, list.size());
        assertEquals(3.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testDelete_outOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(5));
    }

    @Test
    public void testDeleteAllByIndices() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(3.0f, list.get(1), 0.0001f);
        assertEquals(5.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testDeleteAllByIndices_empty() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        list.deleteAllByIndices();
        assertEquals(2, list.size());
    }

    @Test
    public void testDeleteRange() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(5.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testDeleteRange_sameIndices() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testMoveRange() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.moveRange(1, 3, 3);
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(1), 0.0001f);
        assertEquals(5.0f, list.get(2), 0.0001f);
        assertEquals(2.0f, list.get(3), 0.0001f);
        assertEquals(3.0f, list.get(4), 0.0001f);
    }

    @Test
    public void testReplaceRange_FloatList() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatList replacement = FloatList.of(10.0f, 20.0f);
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(10.0f, list.get(1), 0.0001f);
        assertEquals(20.0f, list.get(2), 0.0001f);
        assertEquals(5.0f, list.get(3), 0.0001f);
    }

    @Test
    public void testReplaceRange_FloatList_null() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.replaceRange(1, 2, (FloatList) null);
        assertEquals(2, list.size());
    }

    @Test
    public void testReplaceRange_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        float[] replacement = { 10.0f, 20.0f, 30.0f };
        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(10.0f, list.get(1), 0.0001f);
        assertEquals(20.0f, list.get(2), 0.0001f);
        assertEquals(30.0f, list.get(3), 0.0001f);
    }

    @Test
    public void testReplaceAll_values() {
        FloatList list = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f);
        int count = list.replaceAll(1.0f, 99.0f);
        assertEquals(2, count);
        assertEquals(99.0f, list.get(0), 0.0001f);
        assertEquals(99.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testReplaceAll_values_notFound() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        int count = list.replaceAll(99.0f, 100.0f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAll_values_emptyList() {
        FloatList list = new FloatList();
        int count = list.replaceAll(1.0f, 2.0f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAll_operator() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.replaceAll(f -> f * 2);
        assertEquals(2.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(1), 0.0001f);
        assertEquals(6.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testReplaceIf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        assertTrue(list.replaceIf(f -> f > 2.0f, 99.0f));
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(99.0f, list.get(2), 0.0001f);
        assertEquals(99.0f, list.get(3), 0.0001f);
    }

    @Test
    public void testReplaceIf_noneMatch() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertFalse(list.replaceIf(f -> f > 100.0f, 99.0f));
    }

    @Test
    public void testFill() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.fill(9.0f);
        assertEquals(9.0f, list.get(0), 0.0001f);
        assertEquals(9.0f, list.get(1), 0.0001f);
        assertEquals(9.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testFill_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.fill(1, 4, 99.0f);
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(99.0f, list.get(1), 0.0001f);
        assertEquals(99.0f, list.get(2), 0.0001f);
        assertEquals(99.0f, list.get(3), 0.0001f);
        assertEquals(5.0f, list.get(4), 0.0001f);
    }

    @Test
    public void testContains() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertTrue(list.contains(2.0f));
        assertFalse(list.contains(99.0f));
    }

    @Test
    public void testContainsAny_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list2 = FloatList.of(3.0f, 4.0f);
        assertTrue(list1.containsAny(list2));

        FloatList list3 = FloatList.of(99.0f, 100.0f);
        assertFalse(list1.containsAny(list3));
    }

    @Test
    public void testContainsAny_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertFalse(list1.containsAny(list2));
    }

    @Test
    public void testContainsAny_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 3.0f, 4.0f };
        assertTrue(list.containsAny(arr));
    }

    @Test
    public void testContainsAll_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        FloatList list2 = FloatList.of(2.0f, 3.0f);
        assertTrue(list1.containsAll(list2));

        FloatList list3 = FloatList.of(2.0f, 99.0f);
        assertFalse(list1.containsAll(list3));
    }

    @Test
    public void testContainsAll_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertTrue(list1.containsAll(list2));
    }

    @Test
    public void testContainsAll_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 3.0f };
        assertTrue(list.containsAll(arr));
    }

    @Test
    public void testDisjoint_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list2 = FloatList.of(4.0f, 5.0f);
        assertTrue(list1.disjoint(list2));

        FloatList list3 = FloatList.of(3.0f, 4.0f);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void testDisjoint_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr1 = { 4.0f, 5.0f };
        assertTrue(list.disjoint(arr1));

        float[] arr2 = { 3.0f, 4.0f };
        assertFalse(list.disjoint(arr2));
    }

    @Test
    public void testIntersection_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        FloatList list2 = FloatList.of(2.0f, 3.0f, 4.0f);
        FloatList result = list1.intersection(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(3.0f));
    }

    @Test
    public void testIntersection_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        FloatList result = list1.intersection(list2);
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersection_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 3.0f, 4.0f };
        FloatList result = list.intersection(arr);
        assertEquals(2, result.size());
    }

    @Test
    public void testDifference_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        FloatList list2 = FloatList.of(2.0f, 4.0f);
        FloatList result = list1.difference(list2);
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.0001f);
        assertEquals(3.0f, result.get(1), 0.0001f);
        assertEquals(2.0f, result.get(2), 0.0001f);
    }

    @Test
    public void testDifference_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        FloatList result = list1.difference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void testDifference_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 4.0f };
        FloatList result = list.difference(arr);
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference_FloatList() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list2 = FloatList.of(2.0f, 3.0f, 4.0f);
        FloatList result = list1.symmetricDifference(list2);
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(4.0f));
    }

    @Test
    public void testSymmetricDifference_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        FloatList result = list1.symmetricDifference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 3.0f, 4.0f };
        FloatList result = list.symmetricDifference(arr);
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(4.0f));
    }

    @Test
    public void testOccurrencesOf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 1.0f);
        assertEquals(3, list.occurrencesOf(1.0f));
        assertEquals(1, list.occurrencesOf(2.0f));
        assertEquals(0, list.occurrencesOf(99.0f));
    }

    @Test
    public void testOccurrencesOf_emptyList() {
        FloatList list = new FloatList();
        assertEquals(0, list.occurrencesOf(1.0f));
    }

    @Test
    public void testIndexOf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        assertEquals(1, list.indexOf(2.0f));
        assertEquals(-1, list.indexOf(99.0f));
    }

    @Test
    public void testIndexOf_fromIndex() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        assertEquals(3, list.indexOf(2.0f, 2));
        assertEquals(-1, list.indexOf(2.0f, 10));
    }

    @Test
    public void testLastIndexOf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        assertEquals(3, list.lastIndexOf(2.0f));
        assertEquals(-1, list.lastIndexOf(99.0f));
    }

    @Test
    public void testLastIndexOf_startIndex() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        assertEquals(1, list.lastIndexOf(2.0f, 2));
        assertEquals(-1, list.lastIndexOf(2.0f, -1));
    }

    @Test
    public void testLastIndexOf_emptyList() {
        FloatList list = new FloatList();
        assertEquals(-1, list.lastIndexOf(1.0f, 0));
    }

    @Test
    public void testMin() {
        FloatList list = FloatList.of(3.0f, 1.0f, 4.0f, 2.0f);
        OptionalFloat min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1.0f, min.get(), 0.0001f);
    }

    @Test
    public void testMin_emptyList() {
        FloatList list = new FloatList();
        OptionalFloat min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMin_range() {
        FloatList list = FloatList.of(5.0f, 1.0f, 3.0f, 2.0f, 4.0f);
        OptionalFloat min = list.min(1, 4);
        assertTrue(min.isPresent());
        assertEquals(1.0f, min.get(), 0.0001f);
    }

    @Test
    public void testMin_range_empty() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        OptionalFloat min = list.min(1, 1);
        assertFalse(min.isPresent());
    }

    @Test
    public void testMax() {
        FloatList list = FloatList.of(3.0f, 1.0f, 4.0f, 2.0f);
        OptionalFloat max = list.max();
        assertTrue(max.isPresent());
        assertEquals(4.0f, max.get(), 0.0001f);
    }

    @Test
    public void testMax_emptyList() {
        FloatList list = new FloatList();
        OptionalFloat max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testMax_range() {
        FloatList list = FloatList.of(1.0f, 5.0f, 3.0f, 2.0f, 4.0f);
        OptionalFloat max = list.max(1, 4);
        assertTrue(max.isPresent());
        assertEquals(5.0f, max.get(), 0.0001f);
    }

    @Test
    public void testMedian() {
        FloatList list = FloatList.of(3.0f, 1.0f, 2.0f);
        OptionalFloat median = list.median();
        assertTrue(median.isPresent());
        assertEquals(2.0f, median.get(), 0.0001f);
    }

    @Test
    public void testMedian_emptyList() {
        FloatList list = new FloatList();
        OptionalFloat median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    public void testMedian_range() {
        FloatList list = FloatList.of(5.0f, 3.0f, 1.0f, 2.0f, 4.0f);
        OptionalFloat median = list.median(1, 4);
        assertTrue(median.isPresent());
    }

    @Test
    public void testForEach() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        List<Float> result = new ArrayList<>();
        list.forEach(result::add);
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testForEach_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        List<Float> result = new ArrayList<>();
        list.forEach(1, 4, result::add);
        assertEquals(3, result.size());
        assertEquals(2.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testForEach_reverseRange() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        List<Float> result = new ArrayList<>();
        list.forEach(4, 1, result::add);
        assertEquals(3, result.size());
        assertEquals(5.0f, result.get(0), 0.0001f);
    }

    @Test
    public void testFirst() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        OptionalFloat first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1.0f, first.get(), 0.0001f);
    }

    @Test
    public void testFirst_emptyList() {
        FloatList list = new FloatList();
        OptionalFloat first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        OptionalFloat last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3.0f, last.get(), 0.0001f);
    }

    @Test
    public void testLast_emptyList() {
        FloatList list = new FloatList();
        OptionalFloat last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 4.0f);
        FloatList result = list.distinct(1, 5);
        assertEquals(2, result.size());
        assertEquals(2.0f, result.get(0), 0.0001f);
        assertEquals(3.0f, result.get(1), 0.0001f);
    }

    @Test
    public void testHasDuplicates() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 1.0f);
        assertTrue(list1.hasDuplicates());

        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(list2.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertTrue(list1.isSorted());

        FloatList list2 = FloatList.of(3.0f, 1.0f, 2.0f);
        assertFalse(list2.isSorted());
    }

    @Test
    public void testSort() {
        FloatList list = FloatList.of(3.0f, 1.0f, 2.0f);
        list.sort();
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testSort_alreadySorted() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.sort();
        assertEquals(1.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testParallelSort() {
        FloatList list = FloatList.of(5.0f, 2.0f, 8.0f, 1.0f, 9.0f);
        list.parallelSort();
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(9.0f, list.get(4), 0.0001f);
    }

    @Test
    public void testReverseSort() {
        FloatList list = FloatList.of(3.0f, 1.0f, 2.0f);
        list.reverseSort();
        assertEquals(3.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(1.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testBinarySearch() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertEquals(2, list.binarySearch(3.0f));
        assertTrue(list.binarySearch(2.5f) < 0);
    }

    @Test
    public void testBinarySearch_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertEquals(2, list.binarySearch(0, 5, 3.0f));
    }

    @Test
    public void testReverse() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.reverse();
        assertEquals(3.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(1.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testReverse_singleElement() {
        FloatList list = FloatList.of(1.0f);
        list.reverse();
        assertEquals(1.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testReverse_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.reverse(1, 4);
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(1), 0.0001f);
        assertEquals(3.0f, list.get(2), 0.0001f);
        assertEquals(2.0f, list.get(3), 0.0001f);
        assertEquals(5.0f, list.get(4), 0.0001f);
    }

    @Test
    public void testRotate() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.rotate(2);
        assertEquals(4.0f, list.get(0), 0.0001f);
        assertEquals(5.0f, list.get(1), 0.0001f);
        assertEquals(1.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRotate_negative() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.rotate(-2);
        assertEquals(3.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testShuffle() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatList copy = list.copy();
        list.shuffle();
        assertEquals(5, list.size());
    }

    @Test
    public void testShuffle_withRandom() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.shuffle(new Random(42));
        assertEquals(5, list.size());
    }

    @Test
    public void testSwap() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.swap(0, 2);
        assertEquals(3.0f, list.get(0), 0.0001f);
        assertEquals(1.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testSwap_outOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 5));
    }

    @Test
    public void testCopy() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList copy = list.copy();
        assertEquals(3, copy.size());
        assertNotSame(list, copy);

        list.set(0, 99.0f);
        assertEquals(1.0f, copy.get(0), 0.0001f);
    }

    @Test
    public void testCopy_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals(2.0f, copy.get(0), 0.0001f);
        assertEquals(4.0f, copy.get(2), 0.0001f);
    }

    @Test
    public void testCopy_withStep() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertEquals(1.0f, copy.get(0), 0.0001f);
        assertEquals(3.0f, copy.get(1), 0.0001f);
        assertEquals(5.0f, copy.get(2), 0.0001f);
    }

    @Test
    public void testSplit() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        List<FloatList> chunks = list.split(0, 6, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1.0f, chunks.get(0).get(0), 0.0001f);
        assertEquals(5.0f, chunks.get(2).get(0), 0.0001f);
    }

    @Test
    public void testTrimToSize() {
        FloatList list = new FloatList(100);
        list.add(1.0f);
        list.add(2.0f);
        FloatList result = list.trimToSize();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testClear() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        FloatList list = new FloatList();
        assertTrue(list.isEmpty());

        list.add(1.0f);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        FloatList list = new FloatList();
        assertEquals(0, list.size());

        list.add(1.0f);
        assertEquals(1, list.size());

        list.add(2.0f);
        assertEquals(2, list.size());
    }

    @Test
    public void testBoxed() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        List<Float> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(1.0f, boxed.get(0), 0.0001f);
        assertTrue(boxed.get(0) instanceof Float);
    }

    @Test
    public void testBoxed_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        List<Float> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(2.0f, boxed.get(0), 0.0001f);
    }

    @Test
    public void testToArray() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = list.toArray();
        assertEquals(3, arr.length);
        assertEquals(1.0f, arr[0], 0.0001f);

        arr[0] = 99.0f;
        assertEquals(1.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testToDoubleList() {
        FloatList list = FloatList.of(1.5f, 2.5f, 3.5f);
        DoubleList doubleList = list.toDoubleList();
        assertEquals(3, doubleList.size());
        assertEquals(1.5, doubleList.get(0), 0.0001);
    }

    @Test
    public void testToCollection() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        Set<Float> set = list.toCollection(1, 4, HashSet::new);
        assertEquals(3, set.size());
        assertTrue(set.contains(2.0f));
    }

    @Test
    public void testToMultiset() {
        FloatList list = FloatList.of(1.0f, 2.0f, 2.0f, 3.0f);
        Multiset<Float> multiset = list.toMultiset(0, 4, Multiset::new);
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(2.0f));
    }

    @Test
    public void testIterator() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.next(), 0.0001f);
    }

    @Test
    public void testIterator_emptyList() {
        FloatList list = new FloatList();
        FloatIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatStream stream = list.stream();
        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    @Test
    public void testStream_range() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatStream stream = list.stream(1, 4);
        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    @Test
    public void testGetFirst() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(1.0f, list.getFirst(), 0.0001f);
    }

    @Test
    public void testGetFirst_emptyList() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(3.0f, list.getLast(), 0.0001f);
    }

    @Test
    public void testGetLast_emptyList() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testAddFirst() {
        FloatList list = FloatList.of(2.0f, 3.0f);
        list.addFirst(1.0f);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testAddLast() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        list.addLast(3.0f);
        assertEquals(3, list.size());
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRemoveFirst() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float removed = list.removeFirst();
        assertEquals(1.0f, removed, 0.0001f);
        assertEquals(2, list.size());
        assertEquals(2.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testRemoveFirst_emptyList() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float removed = list.removeLast();
        assertEquals(3.0f, removed, 0.0001f);
        assertEquals(2, list.size());
        assertEquals(2.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testRemoveLast_emptyList() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testHashCode() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list3 = FloatList.of(1.0f, 2.0f, 4.0f);

        assertTrue(list1.equals(list1));
        assertTrue(list1.equals(list2));
        assertFalse(list1.equals(list3));
        assertFalse(list1.equals(null));
        assertFalse(list1.equals("not a FloatList"));
    }

    @Test
    public void testToString() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("1.0"));
    }

    @Test
    public void testToString_emptyList() {
        FloatList list = new FloatList();
        String str = list.toString();
        assertNotNull(str);
    }

    @Test
    public void testConstructor_default() {
        FloatList list = new FloatList();
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructor_withCapacity() {
        FloatList list = new FloatList(10);
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructor_withCapacity_negative() {
        assertThrows(IllegalArgumentException.class, () -> new FloatList(-1));
    }

    @Test
    public void testConstructor_withArray() {
        float[] arr = { 1.0f, 2.0f, 3.0f };
        FloatList list = new FloatList(arr);
        assertEquals(3, list.size());
    }

    @Test
    public void testConstructor_withArrayAndSize() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
        FloatList list = new FloatList(arr, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void testNaNHandling() {
        FloatList list = FloatList.of(1.0f, Float.NaN, 3.0f);
        assertTrue(list.contains(Float.NaN));
        assertEquals(1, list.indexOf(Float.NaN));
    }

    @Test
    public void testInfinityHandling() {
        FloatList list = FloatList.of(Float.POSITIVE_INFINITY, 1.0f, Float.NEGATIVE_INFINITY);
        assertTrue(list.contains(Float.POSITIVE_INFINITY));
        assertTrue(list.contains(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testLargeCapacityGrowth() {
        FloatList list = new FloatList();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        assertEquals(1000, list.size());
        assertEquals(999.0f, list.get(999), 0.0001f);
    }
}
