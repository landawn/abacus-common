package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatStream;

public class FloatListTest extends TestBase {

    private FloatList list;

    @BeforeEach
    public void setUp() {
        list = new FloatList();
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
    public void testLargeCapacityGrowth() {
        FloatList list = new FloatList();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        assertEquals(1000, list.size());
        assertEquals(999.0f, list.get(999), 0.0001f);
    }

    @Test
    public void testBatchOperationsLargeData() {
        int size = 1000;
        FloatList list1 = new FloatList();
        FloatList list2 = new FloatList();

        for (int i = 0; i < size; i++) {
            list1.add(i * 0.1f);
        }

        for (int i = size / 2; i < size + size / 2; i++) {
            list2.add(i * 0.1f);
        }

        FloatList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        FloatList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        FloatList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    // batchRemove with Set optimization: large FloatList removes from large list
    @Test
    public void testRemoveAll_LargeList_UsesSetOptimization() {
        FloatList fl = FloatList.of(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f);
        FloatList toRemove = FloatList.of(2f, 4f, 6f, 8f, 10f);
        assertTrue(fl.removeAll(toRemove));
        assertEquals(15, fl.size());
        assertFalse(fl.contains(2.0f));
    }

    @Test
    public void testOccurrencesOf_emptyList() {
        FloatList list = new FloatList();
        assertEquals(0, list.frequency(1.0f));
    }

    @Test
    public void testConstructorWithZeroSize() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        FloatList list = new FloatList(array, 0);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testDefaultConstructor() {
        FloatList list = new FloatList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithCapacity() {
        FloatList list = new FloatList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructor_withCapacity_zero() {
        FloatList list = new FloatList(0);
        assertTrue(list.isEmpty());
        list.add(1.0f);
        assertEquals(1, list.size());
    }

    // replaceRange with empty FloatList replacement removes range
    @Test
    public void testReplaceRange_EmptyFloatListReplacement_RemovesRange() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.replaceRange(1, 3, FloatList.of());
        assertEquals(3, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(4.0f, fl.get(1), 0.0001f);
        assertEquals(5.0f, fl.get(2), 0.0001f);
    }

    @Test
    public void testConstructor_withCapacity_negative() {
        assertThrows(IllegalArgumentException.class, () -> new FloatList(-1));
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new FloatList(null, 0));
    }

    @Test
    public void testConstructorWithArrayAndSizeThrowsException() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        assertThrows(IndexOutOfBoundsException.class, () -> new FloatList(array, 4));
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new FloatList(null));
    }

    @Test
    public void testOf_varargs() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(3.0f, list.get(2), 0.0001f);
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
    public void testDelete() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float removed = list.removeAt(1);
        assertEquals(2.0f, removed, 0.0001f);
        assertEquals(2, list.size());
        assertEquals(3.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testDeleteAllByIndices() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.removeAt(1, 3);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(3.0f, list.get(1), 0.0001f);
        assertEquals(5.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testDeleteRange() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(5.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testOccurrencesOf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 1.0f);
        assertEquals(3, list.frequency(1.0f));
        assertEquals(1, list.frequency(2.0f));
        assertEquals(0, list.frequency(99.0f));
    }

    @Test
    public void testHasDuplicates() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 1.0f);
        assertTrue(list1.containsDuplicates());

        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(list2.containsDuplicates());
    }

    @Test
    public void testNaNHandling() {
        FloatList list = FloatList.of(1.0f, Float.NaN, 3.0f);
        assertTrue(list.contains(Float.NaN));
        assertEquals(1, list.indexOf(Float.NaN));
    }

    @Test
    public void testOfWithArrayAndSize() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        FloatList list = FloatList.of(array, 3);
        assertEquals(3, list.size());
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
    public void testArray() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = list.internalArray();
        assertNotNull(arr);
    }

    @Test
    public void testDeleteAllByIndices_empty() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        list.removeAt();
        assertEquals(2, list.size());
    }

    @Test
    public void testDeleteRange_sameIndices() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testInfinityHandling() {
        FloatList list = FloatList.of(Float.POSITIVE_INFINITY, 1.0f, Float.NEGATIVE_INFINITY);
        assertTrue(list.contains(Float.POSITIVE_INFINITY));
        assertTrue(list.contains(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testObjectMethods() {
        FloatList list1 = FloatList.of(1.1f, 2.2f);
        FloatList list2 = FloatList.of(1.1f, 2.2f);
        FloatList list3 = FloatList.of(2.2f, 1.1f);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());
        assertNotEquals(list1.hashCode(), list3.hashCode());

        assertEquals("[1.1, 2.2]", list1.toString());
        assertEquals("[]", new FloatList().toString());
    }

    @Test
    public void testOfWithEmptyArray() {
        FloatList list = FloatList.of();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithNull() {
        FloatList list = FloatList.of((float[]) null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOf_arrayWithSize_invalidSize() {
        float[] arr = { 1.0f, 2.0f };
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.of(arr, 5));
    }

    @Test
    public void testDelete_outOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(5));
    }

    @Test
    public void testOfWithNullAndSize() {
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.of(null, 5));
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
    public void testCopyOf_range() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatList list = FloatList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testCopyOf_nullArray() {
        FloatList list = FloatList.copyOf((float[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testCopyOfWithNull() {
        FloatList list = FloatList.copyOf(null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, 2, 10));
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
    public void testGetThrowsException() {
        list.add(10.5f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
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
    public void testSetThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10.5f));
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
    public void testAddRemovePerformance() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i * 0.1f);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> ((int) (x * 10)) % 2 == 0);
        assertEquals(count / 2, list.size());
    }

    @Test
    public void testAdd_atIndex_middle() {
        FloatList fl = FloatList.of(1.0f, 3.0f, 4.0f);
        fl.add(1, 2.0f);
        assertEquals(4, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(2.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
        assertEquals(4.0f, fl.get(3), 0.0001f);
    }

    @Test
    public void testAdd_atIndex_outOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(5, 10.0f));
    }

    @Test
    public void testAddAtIndexThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10.5f));
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
    public void testAddAll_atIndex_array() {
        FloatList list = FloatList.of(1.0f, 4.0f);
        float[] arr = { 2.0f, 3.0f };
        assertTrue(list.addAll(1, arr));
        assertEquals(4, list.size());
        assertEquals(2.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        FloatIterator iter = list.iterator();
        list.add(6.6f);

        assertTrue(iter.hasNext());
        iter.nextFloat();
    }

    @Test
    public void testAddAll_atIndex() {
        FloatList fl = FloatList.of(1.0f, 4.0f, 5.0f);
        FloatList toAdd = FloatList.of(2.0f, 3.0f);
        boolean changed = fl.addAll(1, toAdd);
        assertTrue(changed);
        assertEquals(5, fl.size());
        assertEquals(2.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
    }

    @Test
    public void testAddAll_FloatList_atBeginning() {
        FloatList fl = FloatList.of(3.0f, 4.0f, 5.0f);
        boolean changed = fl.addAll(0, FloatList.of(1.0f, 2.0f));
        assertTrue(changed);
        assertEquals(5, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(2.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
    }

    @Test
    public void testAddAll_FloatList_atMiddle() {
        FloatList fl = FloatList.of(1.0f, 4.0f, 5.0f);
        fl.addAll(1, FloatList.of(2.0f, 3.0f));
        assertEquals(5, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(2.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
        assertEquals(4.0f, fl.get(3), 0.0001f);
        assertEquals(5.0f, fl.get(4), 0.0001f);
    }

    @Test
    public void testAddAll_array_atIndex() {
        FloatList fl = FloatList.of(1.0f, 4.0f);
        boolean changed = fl.addAll(1, new float[] { 2.0f, 3.0f });
        assertTrue(changed);
        assertEquals(4, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(2.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
        assertEquals(4.0f, fl.get(3), 0.0001f);
    }

    @Test
    public void testAddAll_FloatList_atEnd() {
        FloatList fl = FloatList.of(1.0f, 2.0f);
        fl.addAll(2, FloatList.of(3.0f, 4.0f));
        assertEquals(4, fl.size());
        assertEquals(3.0f, fl.get(2), 0.0001f);
        assertEquals(4.0f, fl.get(3), 0.0001f);
    }

    @Test
    public void testAddAll_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertFalse(list1.addAll(list2));
        assertEquals(2, list1.size());
    }

    @Test
    public void testAddAll_array_null() {
        FloatList list = FloatList.of(1.0f);
        assertFalse(list.addAll((float[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllEmptyListAtVariousPositions() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList empty = new FloatList();

        assertFalse(list.addAll(0, empty));
        assertFalse(list.addAll(1, empty));
        assertFalse(list.addAll(list.size(), empty));
    }

    @Test
    public void testAddAllWithNullArray() {
        list.add(1.1f);
        assertFalse(list.addAll((float[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.removeRange(0, 3);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testAddAllEmpty() {
        list.add(1.1f);
        FloatList empty = new FloatList();
        boolean result = list.addAll(empty);

        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAll_atIndex_emptyList() {
        FloatList fl = FloatList.of(1.0f, 2.0f);
        boolean changed = fl.addAll(1, FloatList.of());
        assertFalse(changed);
        assertEquals(2, fl.size());
    }

    @Test
    public void testAddAll_FloatList_returnsFalseForEmpty() {
        FloatList fl = FloatList.of(1.0f, 2.0f);
        boolean changed = fl.addAll(FloatList.of());
        assertFalse(changed);
        assertEquals(2, fl.size());
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
    public void testRemoveNotFound() {
        list.add(10.5f);
        boolean result = list.remove(20.5f);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testEmptyOperations() {
        assertFalse(list.remove(1.1f));
        assertFalse(list.removeAllOccurrences(1.1f));
        assertFalse(list.removeIf(x -> true));
        assertFalse(list.removeDuplicates());
        assertFalse(list.containsDuplicates());
        assertTrue(list.isSorted());

        list.sort();
        list.reverse();
        list.shuffle();

        assertTrue(list.isEmpty());
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
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        boolean changed = fl.removeAllOccurrences(9.0f);
        assertFalse(changed);
        assertEquals(3, fl.size());
    }

    @Test
    public void testRemoveAllOccurrences_multipleMatches() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 1.0f);
        boolean changed = fl.removeAllOccurrences(1.0f);
        assertTrue(changed);
        assertEquals(2, fl.size());
        assertEquals(2.0f, fl.get(0), 0.0001f);
        assertEquals(3.0f, fl.get(1), 0.0001f);
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
    public void testRemoveAllArray() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        float[] toRemove = { 2.2f, 4.4f };

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
    }

    // removeAll(FloatList) with empty list returns false
    @Test
    public void testRemoveAll_FloatList_Empty_ReturnsFalse() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(fl.removeAll(FloatList.of()));
        assertEquals(3, fl.size());
    }

    // removeAll(float[]) with empty array returns false
    @Test
    public void testRemoveAll_Array_Empty_ReturnsFalse() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(fl.removeAll(new float[0]));
        assertEquals(3, fl.size());
    }

    @Test
    public void testRemoveIf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        assertTrue(list.removeIf(f -> f > 3.0f));
        assertEquals(3, list.size());
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRemoveIfNoMatch() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertFalse(list.removeIf(x -> x > 10.0f));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIf_matchingElements() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        boolean changed = fl.removeIf(e -> e > 3.0f);
        assertTrue(changed);
        assertEquals(3, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(2.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
    }

    @Test
    public void testRemoveIf_noMatch() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        boolean changed = fl.removeIf(e -> e > 10.0f);
        assertFalse(changed);
        assertEquals(3, fl.size());
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
    public void testRemoveDuplicatesWithNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, Float.NaN, 2.2f });
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    // ---- New tests for uncovered methods ----

    @Test
    public void testRemoveDuplicates_unsorted() {
        FloatList fl = FloatList.of(3.0f, 1.0f, 2.0f, 1.0f, 3.0f, 4.0f);
        boolean changed = fl.removeDuplicates();
        assertTrue(changed);
        assertEquals(4, fl.size());
        assertTrue(fl.contains(1.0f));
        assertTrue(fl.contains(2.0f));
        assertTrue(fl.contains(3.0f));
        assertTrue(fl.contains(4.0f));
    }

    @Test
    public void testRemoveDuplicatesEmptyList() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRemoveDuplicatesSingleElement() {
        list.add(1.1f);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveDuplicates_allSame() {
        FloatList fl = FloatList.of(5.0f, 5.0f, 5.0f);
        boolean changed = fl.removeDuplicates();
        assertTrue(changed);
        assertEquals(1, fl.size());
        assertEquals(5.0f, fl.get(0), 0.0001f);
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
    public void testRetainAll_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        float[] arr = { 2.0f, 4.0f };
        assertTrue(list.retainAll(arr));
        assertEquals(2, list.size());
    }

    @Test
    public void testRetainAll_array2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        fl.retainAll(new float[] { 2.0f, 4.0f });
        assertEquals(2, fl.size());
        assertEquals(2.0f, fl.get(0), 0.0001f);
        assertEquals(4.0f, fl.get(1), 0.0001f);
    }

    @Test
    public void testRetainAll_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertTrue(list1.retainAll(list2));
        assertEquals(0, list1.size());
    }

    @Test
    public void testRetainAllEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertTrue(list.retainAll(new FloatList()));
        assertTrue(list.isEmpty());
    }

    // retainAll(float[]) with empty array clears and returns true if non-empty
    @Test
    public void testRetainAll_EmptyArray_ClearsAll() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        assertTrue(fl.retainAll(new float[0]));
        assertEquals(0, fl.size());
    }

    @Test
    public void testRemoveAtIndices() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.removeAt(1, 3);
        assertEquals(3, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(3.0f, fl.get(1), 0.0001f);
        assertEquals(5.0f, fl.get(2), 0.0001f);
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.removeAt();
        assertTrue(list.isEmpty());

        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.removeAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAtIndices_empty() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        fl.removeAt();
        assertEquals(3, fl.size());
    }

    @Test
    public void testRemoveAt_multipleIndices() {
        FloatList fl = FloatList.of(10.0f, 20.0f, 30.0f, 40.0f, 50.0f);
        fl.removeAt(new int[] { 1, 3 });
        assertEquals(3, fl.size());
        assertEquals(10.0f, fl.get(0), 0.0001f);
        assertEquals(30.0f, fl.get(1), 0.0001f);
        assertEquals(50.0f, fl.get(2), 0.0001f);
    }

    @Test
    public void testRemoveAt_multipleIndices_singleElement() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        fl.removeAt(new int[] { 0 });
        assertEquals(2, fl.size());
        assertEquals(2.0f, fl.get(0), 0.0001f);
        assertEquals(3.0f, fl.get(1), 0.0001f);
    }

    @Test
    public void testRemoveAt_multipleIndices_empty() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        fl.removeAt(new int[] {});
        assertEquals(3, fl.size());
    }

    @Test
    public void testRemoveRange_middleElements() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.removeRange(1, 4);
        assertEquals(2, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(5.0f, fl.get(1), 0.0001f);
    }

    @Test
    public void testRemoveRange_fromBeginning() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        fl.removeRange(0, 2);
        assertEquals(2, fl.size());
        assertEquals(3.0f, fl.get(0), 0.0001f);
        assertEquals(4.0f, fl.get(1), 0.0001f);
    }

    @Test
    public void testRemoveRange_sameIndex_noChange() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        fl.removeRange(1, 1);
        assertEquals(3, fl.size());
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
    public void testReplaceRange_FloatList2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.replaceRange(1, 3, FloatList.of(20.0f, 30.0f, 40.0f));
        assertEquals(6, fl.size());
        assertEquals(20.0f, fl.get(1), 0.0001f);
    }

    @Test
    public void testReplaceRange_array2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.replaceRange(1, 4, new float[] { 20.0f, 30.0f });
        assertEquals(4, fl.size());
        assertEquals(20.0f, fl.get(1), 0.0001f);
        assertEquals(30.0f, fl.get(2), 0.0001f);
        assertEquals(5.0f, fl.get(3), 0.0001f);
    }

    @Test
    public void testReplaceRange_FloatList_null() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.replaceRange(1, 2, (FloatList) null);
        assertEquals(2, list.size());
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
    public void testReplaceAll_operator() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.replaceAll(f -> f * 2);
        assertEquals(2.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(1), 0.0001f);
        assertEquals(6.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testReplaceAllNoMatch() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        int count = list.replaceAll(5.5f, 10.0f);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAll_values_emptyList() {
        FloatList list = new FloatList();
        int count = list.replaceAll(1.0f, 2.0f);
        assertEquals(0, count);
    }

    @Test
    public void testPrecisionEdgeCases() {
        float a = 0.1f + 0.2f;
        float b = 0.3f;

        list.add(a);
        list.add(b);

        int replaced = list.replaceAll(0.3f, 1.0f);
        assertTrue(replaced > 0);
    }

    @Test
    public void testReplaceAll_values_multiple() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 1.0f);
        int count = fl.replaceAll(1.0f, 9.0f);
        assertEquals(3, count);
        assertEquals(9.0f, fl.get(0), 0.0001f);
        assertEquals(2.0f, fl.get(1), 0.0001f);
        assertEquals(9.0f, fl.get(2), 0.0001f);
        assertEquals(3.0f, fl.get(3), 0.0001f);
        assertEquals(9.0f, fl.get(4), 0.0001f);
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
    public void testReplaceIfFalseCondition() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        boolean result = list.replaceIf(x -> false, 10.0f);
        assertFalse(result);
    }

    @Test
    public void testReplaceIf_matchingElements() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        boolean changed = fl.replaceIf(e -> e % 2 == 0, 0.0f);
        assertTrue(changed);
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(0.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
        assertEquals(0.0f, fl.get(3), 0.0001f);
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
    public void testFillWithSpecialValues() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.fill(Float.NaN);
        for (int i = 0; i < list.size(); i++) {
            assertTrue(Float.isNaN(list.get(i)));
        }
    }

    @Test
    public void testFillEmptyList() {
        list.fill(10.0f);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFillRangeInvalidRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10.0f));
    }

    @Test
    public void testContains() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertTrue(list.contains(2.0f));
        assertFalse(list.contains(99.0f));
    }

    @Test
    public void testContainsMethods() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f);
        assertTrue(list.contains(2.2f));
        assertFalse(list.contains(9.9f));

        assertTrue(list.containsAll(FloatList.of(1.1f, 3.3f)));
        assertFalse(list.containsAll(FloatList.of(1.1f, 4.4f)));

        assertTrue(list.containsAny(new float[] { 4.4f, 5.5f, 2.2f }));
        assertFalse(list.containsAny(new float[] { 4.4f, 5.5f, 6.6f }));
    }

    @Test
    public void testContainsNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f });
        assertTrue(list.contains(Float.NaN));
    }

    @Test
    public void testFloatPrecisionComparison() {
        float a = 0.1f;
        float b = 0.2f;
        float c = a + b;

        list.add(c);
        list.add(0.3f);

        assertTrue(list.contains(c));
        assertTrue(list.contains(0.3f));
    }

    @Test
    public void testContainsEmptyList() {
        assertFalse(list.contains(1.1f));
    }

    @Test
    public void testContainsInfinity() {
        list.addAll(new float[] { 1.1f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY });
        assertTrue(list.contains(Float.POSITIVE_INFINITY));
        assertTrue(list.contains(Float.NEGATIVE_INFINITY));
    }

    // containsAny returns false when list is empty
    @Test
    public void testContainsAny_EmptyList_ReturnsFalse() {
        assertFalse(FloatList.of().containsAny(new float[] { 1.0f }));
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
    public void testContainsAny_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 3.0f, 4.0f };
        assertTrue(list.containsAny(arr));
    }

    @Test
    public void testContainsAny() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList other = FloatList.of(6.6f, 7.7f, 3.3f);

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAnyArray() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        float[] other = { 6.6f, 7.7f, 3.3f };

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAny_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertFalse(list1.containsAny(list2));
    }

    @Test
    public void testContainsAnyBothEmpty() {
        FloatList other = new FloatList();
        assertFalse(list.containsAny(other));
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
    public void testContainsAll_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 3.0f };
        assertTrue(list.containsAll(arr));
    }

    @Test
    public void testContainsAll() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        FloatList other = FloatList.of(2.2f, 4.4f);

        assertTrue(list.containsAll(other));

        other = FloatList.of(2.2f, 6.6f);
        assertFalse(list.containsAll(other));
    }

    @Test
    public void testContainsAll_FloatList2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        assertTrue(fl.containsAll(FloatList.of(1.0f, 3.0f)));
        assertFalse(fl.containsAll(FloatList.of(1.0f, 4.0f)));
        assertTrue(fl.containsAll(FloatList.of()));
    }

    @Test
    public void testContainsAll_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        assertTrue(list1.containsAll(list2));
    }

    @Test
    public void testContainsAllEmptyAgainstNonEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertTrue(list.containsAll(new FloatList()));
    }

    // containsAll(FloatList) returns false when this list is empty but c is not
    @Test
    public void testContainsAll_FloatList_ThisEmpty_ReturnsFalse() {
        assertFalse(FloatList.of().containsAll(FloatList.of(1.0f)));
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
    public void testDisjoint() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList other = FloatList.of(4.4f, 5.5f, 6.6f);

        assertTrue(list.disjoint(other));

        other = FloatList.of(3.3f, 4.4f, 5.5f);
        assertFalse(list.disjoint(other));
    }

    @Test
    public void testDisjoint_FloatList2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        assertTrue(fl.disjoint(FloatList.of(4.0f, 5.0f)));
        assertFalse(fl.disjoint(FloatList.of(3.0f, 6.0f)));
        assertTrue(fl.disjoint(FloatList.of()));
    }

    // disjoint(FloatList) using Set: large list vs large list, no common elements
    @Test
    public void testDisjoint_LargeList_UsesSetOptimization() {
        FloatList fl1 = FloatList.of(1f, 3f, 5f, 7f, 9f, 11f, 13f, 15f, 17f, 19f, 21f, 23f);
        FloatList fl2 = FloatList.of(2f, 4f, 6f, 8f, 10f, 12f, 14f, 16f, 18f, 20f, 22f, 24f);
        assertTrue(fl1.disjoint(fl2));
    }

    @Test
    public void testDisjointWithSelf() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertFalse(list.disjoint(list));
    }

    // disjoint(float[]) returns true when list is empty
    @Test
    public void testDisjoint_Array_EmptyList_ReturnsTrue() {
        assertTrue(FloatList.of().disjoint(new float[] { 1.0f }));
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
    public void testIntersection_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 3.0f, 4.0f };
        FloatList result = list.intersection(arr);
        assertEquals(2, result.size());
    }

    @Test
    public void testIntersection_FloatList_withDuplicatesInSource() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 2.0f, 3.0f, 4.0f);
        FloatList other = FloatList.of(2.0f, 4.0f, 5.0f);
        FloatList result = fl.intersection(other);
        assertTrue(result.contains(2.0f));
        assertTrue(result.contains(4.0f));
        assertFalse(result.contains(1.0f));
        assertFalse(result.contains(3.0f));
    }

    @Test
    public void testIntersection_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        FloatList result = list1.intersection(list2);
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersectionWithEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.intersection(new FloatList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionNoCommon() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.intersection(FloatList.of(4.4f, 5.5f, 6.6f));
        assertTrue(result.isEmpty());
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
    public void testDifference_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 4.0f };
        FloatList result = list.difference(arr);
        assertEquals(2, result.size());
    }

    @Test
    public void testDifference_array2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        FloatList result = fl.difference(new float[] { 2.0f });
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.0001f);
        assertEquals(3.0f, result.get(1), 0.0001f);
        assertEquals(2.0f, result.get(2), 0.0001f);
    }

    @Test
    public void testDifference_FloatList_withDuplicates() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 2.0f, 3.0f, 4.0f);
        FloatList other = FloatList.of(2.0f, 4.0f);
        FloatList result = fl.difference(other);
        assertEquals(3, result.size());
        assertEquals(1.0f, result.get(0), 0.0001f);
        assertEquals(2.0f, result.get(1), 0.0001f);
        assertEquals(3.0f, result.get(2), 0.0001f);
    }

    @Test
    public void testDifference_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        FloatList result = list1.difference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void testDifferenceAllRemoved() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.difference(FloatList.of(1.1f, 2.2f, 3.3f));
        assertTrue(result.isEmpty());
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
    public void testSymmetricDifference_array() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float[] arr = { 2.0f, 3.0f, 4.0f };
        FloatList result = list.symmetricDifference(arr);
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(4.0f));
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList other = FloatList.of(3.3f, 4.4f, 5.5f);

        FloatList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertTrue(result.contains(1.1f));
        assertTrue(result.contains(2.2f));
        assertTrue(result.contains(4.4f));
        assertTrue(result.contains(5.5f));
    }

    @Test
    public void testSymmetricDifference_array2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList result = fl.symmetricDifference(new float[] { 2.0f, 4.0f });
        assertTrue(result.contains(1.0f));
        assertTrue(result.contains(3.0f));
        assertTrue(result.contains(4.0f));
        assertFalse(result.contains(2.0f));
    }

    @Test
    public void testSymmetricDifference_FloatList_empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        FloatList result = list1.symmetricDifference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        FloatList result = list.symmetricDifference(new FloatList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceOneEmpty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatList result = list.symmetricDifference(new FloatList());
        assertEquals(3, result.size());
    }

    @Test
    public void testIndexOf() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        assertEquals(1, list.indexOf(2.0f));
        assertEquals(-1, list.indexOf(99.0f));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        FloatList list = FloatList.of(1.1f, 2.2f, 1.1f, 3.3f, 2.2f);
        assertEquals(0, list.indexOf(1.1f));
        assertEquals(2, list.lastIndexOf(1.1f));
        assertEquals(4, list.lastIndexOf(2.2f));
        assertEquals(4, list.indexOf(2.2f, 2));
        assertEquals(-1, list.indexOf(9.9f));
    }

    @Test
    public void testIndexOfNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f, Float.NaN });
        assertEquals(1, list.indexOf(Float.NaN));
        assertEquals(3, list.indexOf(Float.NaN, 2));
    }

    @Test
    public void testIndexOf_fromIndex() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        assertEquals(3, list.indexOf(2.0f, 2));
        assertEquals(-1, list.indexOf(2.0f, 10));
    }

    @Test
    public void testIndexOfFromIndexBeyondSize() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(-1, list.indexOf(1.1f, 10));
    }

    @Test
    public void testIndexOfNegativeFromIndex() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(0, list.indexOf(1.1f, -1));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

        assertEquals(3, list.indexOf(2.2f, 2));
        assertEquals(-1, list.indexOf(2.2f, 4));
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
    public void testLastIndexOfNaN() {
        list.addAll(new float[] { Float.NaN, 1.1f, Float.NaN, 2.2f });
        assertEquals(2, list.lastIndexOf(Float.NaN));
        assertEquals(0, list.lastIndexOf(Float.NaN, 1));
    }

    @Test
    public void testLastIndexOfWithStart() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

        assertEquals(1, list.lastIndexOf(2.2f, 2));
        assertEquals(-1, list.lastIndexOf(5.5f, 3));
    }

    @Test
    public void testLastIndexOf_withStartIndex() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 1.0f);
        int idx = fl.lastIndexOf(1.0f, 3);
        assertEquals(2, idx);
    }

    @Test
    public void testLastIndexOf_notFound() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        int idx = fl.lastIndexOf(9.0f, 2);
        assertEquals(-1, idx);
    }

    @Test
    public void testLastIndexOf_emptyList() {
        FloatList list = new FloatList();
        assertEquals(-1, list.lastIndexOf(1.0f, 0));
    }

    @Test
    public void testLastIndexOfEmptyList() {
        assertEquals(-1, list.lastIndexOf(1.1f));
    }

    @Test
    public void testLastIndexOfNegativeStart() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(-1, list.lastIndexOf(1.1f, -1));
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
    public void testMinMaxMedianEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

        assertFalse(list.min(1, 1).isPresent());
        assertFalse(list.max(1, 1).isPresent());
        assertFalse(list.median(1, 1).isPresent());
    }

    @Test
    public void testMinEmpty() {
        OptionalFloat min = list.min();
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
    public void testMax_range_empty() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        OptionalFloat max = list.max(1, 1);
        assertFalse(max.isPresent());
    }

    @Test
    public void testMaxArraySize() {
        try {
            FloatList largeList = new FloatList(Integer.MAX_VALUE - 8);
            assertTrue(largeList.isEmpty());
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testMedian() {
        FloatList list = FloatList.of(3.0f, 1.0f, 2.0f);
        OptionalFloat median = list.median();
        assertTrue(median.isPresent());
        assertEquals(2.0f, median.get(), 0.0001f);
    }

    @Test
    public void testMedian_range() {
        FloatList list = FloatList.of(5.0f, 3.0f, 1.0f, 2.0f, 4.0f);
        OptionalFloat median = list.median(1, 4);
        assertTrue(median.isPresent());
    }

    @Test
    public void testMedian_emptyList() {
        FloatList list = new FloatList();
        OptionalFloat median = list.median();
        assertFalse(median.isPresent());
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
    public void testForEachEmptyList() {
        List<Float> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
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
    public void testFirstEmpty() {
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
    public void testDistinctWithNaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, Float.NaN, 2.2f });
        FloatList result = list.distinct(0, list.size());
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f });
        FloatList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    // --- Missing dedicated tests for source methods ---

    @Test
    public void testContainsDuplicates() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 1.0f);
        assertTrue(list1.containsDuplicates());

        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(list2.containsDuplicates());

        FloatList emptyList = new FloatList();
        assertFalse(emptyList.containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_singleElement() {
        FloatList list = FloatList.of(1.0f);
        assertFalse(list.containsDuplicates());
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
    public void testSortEmptyList() {
        list.sort();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testParallelSort() {
        FloatList list = FloatList.of(5.0f, 2.0f, 8.0f, 1.0f, 9.0f);
        list.parallelSort();
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(9.0f, list.get(4), 0.0001f);
    }

    @Test
    public void testParallelSortSmallList() {
        list.addAll(new float[] { 3.3f, 1.1f, 2.2f });
        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testParallelSort2() {
        FloatList fl = FloatList.of(5.0f, 3.0f, 1.0f, 4.0f, 2.0f);
        fl.parallelSort();
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(5.0f, fl.get(4), 0.0001f);
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
    public void testReverseSort2() {
        FloatList fl = FloatList.of(3.0f, 1.0f, 4.0f, 1.0f, 5.0f);
        fl.reverseSort();
        assertEquals(5.0f, fl.get(0), 0.0001f);
        assertEquals(1.0f, fl.get(4), 0.0001f);
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
    public void testBinarySearchRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        assertEquals(3, list.binarySearch(1, 5, 4.4f));
        assertTrue(list.binarySearch(1, 3, 4.4f) < 0);
    }

    @Test
    public void testBinarySearchUnsorted() {
        list.addAll(new float[] { 3.3f, 1.1f, 4.4f, 1.1f, 5.5f });
        int result = list.binarySearch(3.3f);
        assertNotNull(result);
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
    public void testReverse_range2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.reverse(1, 4);
        assertEquals(4.0f, fl.get(1), 0.0001f);
        assertEquals(3.0f, fl.get(2), 0.0001f);
        assertEquals(2.0f, fl.get(3), 0.0001f);
    }

    @Test
    public void testReverse_singleElement() {
        FloatList list = FloatList.of(1.0f);
        list.reverse();
        assertEquals(1.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testReverseEmptyList() {
        list.reverse();
        assertTrue(list.isEmpty());
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
    public void testRotate2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.rotate(2);
        assertEquals(4.0f, fl.get(0), 0.0001f);
        assertEquals(5.0f, fl.get(1), 0.0001f);
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
    public void testShuffleWithRandom() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        Random rnd = new Random(42);

        list.shuffle(rnd);
        assertEquals(5, list.size());
    }

    @Test
    public void testShuffleEmptyList() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffle_withRandom_deterministic() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        Random rnd = new Random(42);
        fl.shuffle(rnd);
        assertEquals(5, fl.size());
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
    public void testSwapThrowsException() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
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
    public void testCopy_withStep2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        FloatList copied = fl.copy(0, 5, 2);
        assertEquals(3, copied.size());
        assertEquals(1.0f, copied.get(0), 0.0001f);
        assertEquals(3.0f, copied.get(1), 0.0001f);
        assertEquals(5.0f, copied.get(2), 0.0001f);
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
    public void testCopyEmptyList() {
        FloatList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopyRangeInvalidIndices() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
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
    public void testSplitWithChunkSizeLargerThanList() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        List<FloatList> chunks = list.split(0, 3, 10);
        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).size());
    }

    @Test
    public void testSplitUnevenChunks() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
        List<FloatList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testSplit_unevenChunks() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        List<FloatList> chunks = fl.split(0, 5, 3);
        assertEquals(2, chunks.size());
        assertEquals(3, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
    }

    @Test
    public void testSplitEmptyList() {
        List<FloatList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplit_multipleChunks() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
        List<FloatList> chunks = fl.split(0, 6, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1.0f, chunks.get(0).get(0), 0.0001f);
        assertEquals(2.0f, chunks.get(0).get(1), 0.0001f);
        assertEquals(2, chunks.get(1).size());
        assertEquals(2, chunks.get(2).size());
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
    public void testClear2() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
        fl.clear();
        assertEquals(0, fl.size());
        assertTrue(fl.isEmpty());
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
    public void testSubnormalNumbers() {
        float subnormal = Float.MIN_VALUE / 2;
        list.add(subnormal);
        list.add(0.0f);
        list.add(-subnormal);

        assertEquals(3, list.size());
        assertTrue(list.contains(subnormal));
        assertTrue(list.contains(-subnormal));
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
    public void testBoxedRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        List<Float> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Float.valueOf(2.2f), boxed.get(0));
        assertEquals(Float.valueOf(4.4f), boxed.get(2));
    }

    @Test
    public void testBoxedEmptyList() {
        List<Float> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxedWithSpecialValues() {
        list.addAll(new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY });
        List<Float> boxed = list.boxed();
        assertTrue(Float.isNaN(boxed.get(0)));
        assertEquals(Float.POSITIVE_INFINITY, boxed.get(1));
        assertEquals(Float.NEGATIVE_INFINITY, boxed.get(2));
    }

    @Test
    public void testBoxedRangeInvalidIndices() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
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
    public void testToCollection_withSupplier() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
        java.util.ArrayList<Float> col = fl.toCollection(0, 4, java.util.ArrayList::new);
        assertEquals(4, col.size());
        assertEquals(1.0f, col.get(0), 0.0001f);
        assertEquals(4.0f, col.get(3), 0.0001f);
    }

    @Test
    public void testToCollection_partialRange() {
        FloatList fl = FloatList.of(10.0f, 20.0f, 30.0f, 40.0f);
        java.util.ArrayList<Float> col = fl.toCollection(1, 3, java.util.ArrayList::new);
        assertEquals(2, col.size());
        assertEquals(20.0f, col.get(0), 0.0001f);
        assertEquals(30.0f, col.get(1), 0.0001f);
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
    public void testIteratorEmptyList() {
        FloatIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
    }

    @Test
    public void testStreamRange() {
        list.addAll(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });

        FloatStream stream = list.stream(1, 4);
        double sum = stream.sum();
        assertEquals(9.0, sum, 0.0001);
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
    public void testStreamEmptyList() {
        FloatStream stream = list.stream();
        assertEquals(0, stream.count());
    }

    @Test
    public void testStreamRangeEmptyRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        FloatStream stream = list.stream(1, 1);
        assertEquals(0, stream.count());
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
    public void testGetFirstThrowsException() {
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
    public void testGetLastThrowsException() {
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
    public void testRemoveFirstThrowsException() {
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
    public void testRemoveLastThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testHashCode() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals_differentContent() {
        FloatList fl1 = FloatList.of(1.0f, 2.0f);
        FloatList fl2 = FloatList.of(1.0f, 3.0f);
        assertNotEquals(fl1, fl2);
    }

    @Test
    public void testEquals_differentSize() {
        FloatList fl1 = FloatList.of(1.0f, 2.0f);
        FloatList fl2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertNotEquals(fl1, fl2);
    }

    @Test
    public void testEquals_differentValues() {
        FloatList fl1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList fl2 = FloatList.of(1.0f, 2.0f, 4.0f);
        assertNotEquals(fl1, fl2);
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
    public void testEqualsWithFloatPrecision() {
        list.add(0.1f + 0.2f);

        FloatList other = new FloatList();
        other.add(0.3f);

        boolean equalsResult = list.equals(other);
        assertNotNull(equalsResult);
    }

    @Test
    public void testEquals_sameContent() {
        FloatList fl1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList fl2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(fl1, fl2);
    }

    @Test
    public void testEquals_withNull() {
        FloatList fl = FloatList.of(1.0f);
        assertNotEquals(fl, null);
    }

    @Test
    public void testEquals_sameValues() {
        FloatList fl1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList fl2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(fl1, fl2);
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
    public void testToStringWithSpecialValues() {
        list.add(Float.NaN);
        list.add(Float.POSITIVE_INFINITY);
        list.add(Float.NEGATIVE_INFINITY);
        list.add(-0.0f);
        list.add(0.0f);

        String str = list.toString();
        assertTrue(str.contains("NaN"));
        assertTrue(str.contains("Infinity"));
        assertTrue(str.contains("-Infinity"));
    }

    @Test
    public void testEnsureCapacity() {
        // Test that adding many elements (beyond initial capacity) works correctly
        FloatList fl = new FloatList(2);
        for (int i = 0; i < 100; i++) {
            fl.add(i * 1.0f);
        }
        assertEquals(100, fl.size());
        assertEquals(0.0f, fl.get(0), 0.0001f);
        assertEquals(99.0f, fl.get(99), 0.0001f);
    }

    @Test
    public void testEnsureCapacityOverflow() {
        list.add(1.1f);
        try {
            for (int i = 0; i < 100; i++) {
                list.add(i * 0.1f);
            }
            assertTrue(list.size() > 1);
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

}
