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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.stream.Stream;

public class BooleanListTest extends TestBase {

    private BooleanList list;

    @BeforeEach
    public void setUp() {
        list = new BooleanList();
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
    public void test_constructor_withArrayAndSize() {
        boolean[] arr = { true, false, true, false };
        BooleanList list = new BooleanList(arr, 2);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    @DisplayName("Test constructor that wraps an existing array with a given size")
    public void testConstructorWithArrayAndSize() {
        boolean[] data = { true, false, true, false, true };
        BooleanList list = new BooleanList(data, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
        data[0] = false;
        assertFalse(list.get(0), "Internal array modification should be reflected in the list");
    }

    @Test
    public void testCapacityGrowth() {
        BooleanList smallList = new BooleanList(2);
        for (int i = 0; i < 100; i++) {
            smallList.add(i % 2 == 0);
        }
        assertEquals(100, smallList.size());

        for (int i = 0; i < 100; i++) {
            assertEquals(i % 2 == 0, smallList.get(i));
        }
    }

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
    public void test_constructor_withArray_empty() {
        boolean[] arr = {};
        BooleanList list = new BooleanList(arr);
        assertEquals(0, list.size());
    }

    @Test
    public void test_array() {
        boolean[] arr = { true, false };
        BooleanList list = new BooleanList(arr);
        assertSame(arr, list.internalArray());
    }

    @Test
    @DisplayName("Test default constructor and basic properties")
    public void testEmptyConstructor() {
        BooleanList list = new BooleanList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        BooleanList list = new BooleanList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithZeroCapacity() {
        BooleanList zeroCapList = new BooleanList(0);
        assertEquals(0, zeroCapList.size());

        zeroCapList.add(true);
        assertEquals(1, zeroCapList.size());
        assertEquals(true, zeroCapList.get(0));
    }

    @Test
    public void testMaxSizeOperations() {

        BooleanList smallList = new BooleanList();
        assertNotNull(smallList);
    }

    @Test
    public void test_constructor_withCapacity_negative() {
        assertThrows(IllegalArgumentException.class, () -> new BooleanList(-1));
    }

    @Test
    public void test_constructor_withArrayAndSize_invalid() {
        boolean[] arr = { true, false };
        assertThrows(IndexOutOfBoundsException.class, () -> new BooleanList(arr, 3));
        assertThrows(IllegalArgumentException.class, () -> new BooleanList(arr, -1));
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new BooleanList(null));
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new BooleanList(null, 5));
    }

    @Test
    public void testConstructors() {
        BooleanList list1 = new BooleanList();
        assertEquals(0, list1.size());

        BooleanList list2 = new BooleanList(10);
        assertEquals(0, list2.size());

        boolean[] arr = { true, false, true };
        BooleanList list3 = new BooleanList(arr);
        assertEquals(3, list3.size());
        assertEquals(true, list3.get(0));
        assertEquals(false, list3.get(1));
        assertEquals(true, list3.get(2));

        boolean[] arr2 = { true, false, true, false, true };
        BooleanList list4 = new BooleanList(arr2, 3);
        assertEquals(3, list4.size());
        assertEquals(true, list4.get(0));
        assertEquals(false, list4.get(1));
        assertEquals(true, list4.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> new BooleanList(arr2, 10));
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
    public void test_of_withSize() {
        boolean[] arr = { true, false, true, false };
        BooleanList list = BooleanList.of(arr, 2);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_delete() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean deleted = list.removeAt(1);
        assertFalse(deleted);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    @DisplayName("Test static factory 'of' method")
    public void testOf() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
        assertEquals("[true, false, true]", list.toString());
    }

    @Test
    @DisplayName("Test deleting an element at a specific index")
    public void testDelete() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean deletedValue = list.removeAt(1);
        assertFalse(deletedValue);
        assertEquals(2, list.size());
        assertEquals("[true, true]", list.toString());
    }

    @Test
    @DisplayName("Test deleting a range of elements")
    public void testDeleteRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.removeRange(1, 4);
        assertArrayEquals(new boolean[] { true, true }, list.toArray());
    }

    @Test
    @DisplayName("Test 'frequency' method")
    public void testOccurrencesOf() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        assertEquals(3, list.frequency(true));
        assertEquals(2, list.frequency(false));
    }

    @Test
    @DisplayName("Test 'containsDuplicates' method")
    public void testHasDuplicates() {
        assertTrue(BooleanList.of(true, false, true).containsDuplicates());
        assertTrue(BooleanList.of(true, true).containsDuplicates());
        assertFalse(BooleanList.of(true, false).containsDuplicates());
        assertFalse(BooleanList.of(true).containsDuplicates());
        assertFalse(new BooleanList().containsDuplicates());
    }

    @Test
    @DisplayName("Test 'sort' and 'reverseSort' methods")
    public void testSorting() {
        BooleanList list = BooleanList.of(true, false, true, false, false);
        list.sort();
        assertArrayEquals(new boolean[] { false, false, false, true, true }, list.toArray());
        list.reverseSort();
        assertArrayEquals(new boolean[] { true, true, false, false, false }, list.toArray());
    }

    @Test
    @DisplayName("Test conversion methods: 'toArray', 'boxed', 'toCollection'")
    public void testConversion() {
        boolean[] arr = { true, false, true };
        BooleanList list = BooleanList.of(arr);

        assertArrayEquals(arr, list.toArray());

        List<Boolean> boxedList = list.boxed();
        assertEquals(List.of(true, false, true), boxedList);

        ArrayList<Boolean> collected = list.toCollection(ArrayList::new);
        assertEquals(boxedList, collected);
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
    public void test_of_withSize_null() {
        BooleanList list = BooleanList.of(null, 0);
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test deleting elements by multiple indices")
    public void testDeleteAllByIndices() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.removeAt(0, 2, 4);
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
    }

    @Test
    public void testOfWithEmptyArray() {
        BooleanList emptyList = BooleanList.of(new boolean[0]);
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void test_of_withSize_invalid() {
        boolean[] arr = { true, false };
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.of(arr, 3));
    }

    @Test
    public void test_delete_outOfBounds() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
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
    public void test_copyOf_withRange() {
        boolean[] arr = { true, false, true, false };
        BooleanList list = BooleanList.copyOf(arr, 1, 3);
        assertEquals(2, list.size());
        assertFalse(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    @DisplayName("Test static factory 'copyOf' method")
    public void testCopyOf() {
        boolean[] original = { true, false, true };
        BooleanList list = BooleanList.copyOf(original);
        assertEquals(3, list.size());
        assertArrayEquals(original, list.toArray());
        original[0] = false;
        assertTrue(list.get(0));
    }

    @Test
    @DisplayName("Test static factory 'copyOf' method with a range")
    public void testCopyOfRange() {
        boolean[] original = { true, false, true, false, true };
        BooleanList list = BooleanList.copyOf(original, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    public void test_copyOf_null() {
        BooleanList list = BooleanList.copyOf((boolean[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testCopyOfWithNullArray() {
        BooleanList copyList = BooleanList.copyOf(null);
        assertEquals(0, copyList.size());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        boolean[] arr = { true, false, true, false };

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, 3, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, -1, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, 0, 10));
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
    @DisplayName("Test static factory 'repeat' method")
    public void testRepeat() {
        BooleanList list = BooleanList.repeat(true, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(list.get(i));
        }
        assertEquals("[true, true, true, true, true]", list.toString());
    }

    @Test
    public void test_repeat_zero() {
        BooleanList list = BooleanList.repeat(true, 0);
        assertEquals(0, list.size());
    }

    @Test
    public void testRepeatWithZeroLength() {
        BooleanList repeated = BooleanList.repeat(true, 0);
        assertEquals(0, repeated.size());
        assertTrue(repeated.isEmpty());
    }

    @Test
    public void testRepeatWithNegativeLength() {
        assertThrows(IllegalArgumentException.class, () -> BooleanList.repeat(true, -1));
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
    public void testArray() {
        list.add(true);
        list.add(false);
        list.add(true);

        boolean[] array = list.internalArray();
        assertEquals(true, array[0]);
        assertEquals(false, array[1]);
        assertEquals(true, array[2]);

        array[0] = false;
        assertEquals(false, list.get(0));
    }

    // ---- Additional tests for previously untested methods/overloads ----

    @Test
    public void testInternalArray_empty() {
        boolean[] arr = list.internalArray();
        assertNotNull(arr);
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
    @DisplayName("Test getting an element by index")
    public void testGet() {
        BooleanList list = BooleanList.of(false, true);
        assertTrue(list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @Test
    @DisplayName("Test 'getFirst', 'getLast', 'first', and 'last' methods")
    public void testGetAndOptionalFirstLast() {
        BooleanList list = BooleanList.of(true, false, false, false);
        assertTrue(list.getFirst());
        assertFalse(list.getLast());
        assertEquals(OptionalBoolean.of(true), list.first());
        assertEquals(OptionalBoolean.of(false), list.last());

        BooleanList emptyList = new BooleanList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::getLast);
        assertEquals(OptionalBoolean.empty(), emptyList.first());
        assertEquals(OptionalBoolean.empty(), emptyList.last());
    }

    @Test
    public void test_set() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean old = list.set(1, true);
        assertFalse(old);
        assertTrue(list.get(1));
    }

    @Test
    @DisplayName("Test setting an element at a specific index")
    public void testSet() {
        BooleanList list = BooleanList.of(true, true);
        boolean oldValue = list.set(1, false);
        assertTrue(oldValue);
        assertFalse(list.get(1));
        assertEquals("[true, false]", list.toString());
    }

    @Test
    @DisplayName("Test set operations: intersection, difference, symmetricDifference")
    public void testSetOperations() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);

        BooleanList intersection = list1.intersection(list2);
        intersection.sort();
        assertArrayEquals(new boolean[] { false, true }, intersection.toArray());

        BooleanList difference = list1.difference(list2);
        assertArrayEquals(new boolean[] { true }, difference.toArray());

        BooleanList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new boolean[] { false, true }, symmDiff.toArray());
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
    @DisplayName("Test adding elements")
    public void testAdd() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);
        assertEquals(2, list.size());
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
    }

    @Test
    @DisplayName("Test adding an element at a specific index")
    public void testAddAtIndex() {
        BooleanList list = BooleanList.of(true, true);
        list.add(1, false);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
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
    public void test_addAll_array() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = { true, true };
        boolean result = list.addAll(arr);
        assertTrue(result);
        assertEquals(4, list.size());
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
    @DisplayName("Test adding all elements from another BooleanList")
    public void testAddAll() {
        BooleanList list = BooleanList.of(true);
        BooleanList toAdd = BooleanList.of(false, true);
        list.addAll(toAdd);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
    }

    @Test
    @DisplayName("Test adding all elements from an array at a specific index")
    public void testAddAllAtIndex() {
        BooleanList list = BooleanList.of(true, true);
        boolean[] toAdd = { false, false };
        list.addAll(1, toAdd);
        assertEquals(4, list.size());
        assertArrayEquals(new boolean[] { true, false, false, true }, list.toArray());
    }

    @Test
    public void testAddAll_withIndex_BooleanList() {
        BooleanList a = BooleanList.of(true, true);
        BooleanList b = BooleanList.of(false, false);
        assertTrue(a.addAll(1, b));
        assertEquals(4, a.size());
        assertTrue(a.get(0));
        assertFalse(a.get(1));
        assertFalse(a.get(2));
        assertTrue(a.get(3));
    }

    @Test
    public void testAddAll_withIndex_BooleanList_middle() {
        BooleanList a = BooleanList.of(true, false);
        BooleanList b = BooleanList.of(true, true);
        a.addAll(1, b);
        assertEquals(4, a.size());
        assertTrue(a.get(0));
        assertTrue(a.get(1));
        assertTrue(a.get(2));
        assertFalse(a.get(3));
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
    public void test_addAll_BooleanList_withIndex_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        boolean result = list1.addAll(1, list2);
        assertFalse(result);
        assertEquals(2, list1.size());
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
    public void testAddAll_withIndex_BooleanList_empty() {
        BooleanList a = BooleanList.of(true, false);
        BooleanList b = new BooleanList();
        assertFalse(a.addAll(1, b));
        assertEquals(2, a.size());
    }

    @Test
    public void test_addAll_BooleanList_withIndex_invalid() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(true);
        assertThrows(IndexOutOfBoundsException.class, () -> list1.addAll(3, list2));
    }

    @Test
    public void testAddAll_array_withIndex_invalid() {
        list.add(true);
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, new boolean[] { false }));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(5, new boolean[] { false }));
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
    @DisplayName("Test removing an element by value")
    public void testRemove() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.remove(true));
        assertEquals(2, list.size());
        assertEquals("[false, true]", list.toString());
        assertTrue(list.remove(true));
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
    @DisplayName("Test removing all occurrences of an element")
    public void testRemoveAllOccurrences() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        assertTrue(list.removeAllOccurrences(true));
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
        assertFalse(list.removeAllOccurrences(true));
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
    public void test_removeAll_array() {
        BooleanList list = BooleanList.of(true, false, true, false);
        boolean[] arr = { true };
        boolean result = list.removeAll(arr);
        assertTrue(result);
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAll_BooleanList_noChange() {
        BooleanList a = BooleanList.of(true, true);
        BooleanList b = BooleanList.of(false);
        assertFalse(a.removeAll(b));
        assertEquals(2, a.size());
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
    public void testRemoveAll_array_empty() {
        list.add(true);
        list.add(false);
        assertFalse(list.removeAll(new boolean[] {}));
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
    @DisplayName("Test 'removeIf' method with a predicate")
    public void testRemoveIf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.removeIf(val -> val);
        assertEquals(2, list.size());
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
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
    public void test_removeDuplicates_allSame() {
        BooleanList list = BooleanList.of(true, true, true);
        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(1, list.size());
        assertTrue(list.get(0));
    }

    @Test
    public void test_removeDuplicates_sizeTwo_same() {
        BooleanList list = BooleanList.of(true, true);
        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Test 'removeDuplicates' method")
    public void testRemoveDuplicates() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        list.removeDuplicates();
        assertArrayEquals(new boolean[] { true, false }, list.toArray());

        BooleanList allSame = BooleanList.of(true, true, true);
        allSame.removeDuplicates();
        assertArrayEquals(new boolean[] { true }, allSame.toArray());
    }

    @Test
    public void testRemoveDuplicates_empty() {
        assertFalse(list.removeDuplicates());
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
    public void test_retainAll_array() {
        BooleanList list = BooleanList.of(true, false, true, false);
        boolean[] arr = { true };
        boolean result = list.retainAll(arr);
        assertTrue(result);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Test retaining all elements from another list")
    public void testRetainAll() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        boolean changed = list.retainAll(BooleanList.of(false, false));
        assertTrue(changed);
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
    }

    @Test
    public void testRetainAll_BooleanList_noChange() {
        BooleanList a = BooleanList.of(true, false, true);
        BooleanList b = BooleanList.of(true, false);
        // All elements in a are in b, nothing removed
        assertFalse(a.retainAll(b));
        assertEquals(3, a.size());
    }

    @Test
    public void testRetainAll_array_allMatch() {
        BooleanList a = BooleanList.of(true, false);
        boolean[] b = { true, false };
        assertFalse(a.retainAll(b));
        assertEquals(2, a.size());
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
    public void testRetainAll_array_empty() {
        list.add(true);
        list.add(false);
        assertTrue(list.retainAll(new boolean[] {}));
        assertEquals(0, list.size());
    }

    @Test
    public void testRetainAll_BooleanList_empty_nonEmptyList() {
        BooleanList a = BooleanList.of(true, false, true);
        BooleanList b = new BooleanList();
        assertTrue(a.retainAll(b));
        assertEquals(0, a.size());
    }

    @Test
    public void testRetainAll_array_empty_nonEmptyList() {
        BooleanList a = BooleanList.of(true, false, true);
        boolean[] b = {};
        assertTrue(a.retainAll(b));
        assertEquals(0, a.size());
    }

    @Test
    public void testBatchRemove_viaRemoveAll_largeList() {
        // batchRemove with complement=false (removeAll) triggers set path when sizes large enough
        BooleanList a = BooleanList.of(true, false, true, false, true, false, true, false, true, false, true);
        BooleanList b = BooleanList.of(true, true, true, true);
        // c.size()=4 > 3, a.size()=11 > 9 => set path
        assertTrue(a.removeAll(b));
        // all 'true' elements should be removed; only 'false' remain
        assertEquals(5, a.size());
        for (int i = 0; i < a.size(); i++) {
            assertFalse(a.get(i));
        }
    }

    @Test
    public void testBatchRemove_viaRetainAll_largeList() {
        // batchRemove with complement=true (retainAll) triggers set path
        BooleanList a = BooleanList.of(true, false, true, false, true, false, true, false, true, false, true);
        BooleanList b = BooleanList.of(true, true, true, true);
        // retain only 'true' elements
        assertTrue(a.retainAll(b));
        assertEquals(6, a.size());
        for (int i = 0; i < a.size(); i++) {
            assertTrue(a.get(i));
        }
    }

    @Test
    public void testBatchRemove_viaRemoveAll_smallList() {
        BooleanList a = BooleanList.of(true, false, true, false, true);
        BooleanList b = BooleanList.of(true);
        assertTrue(a.removeAll(b));
        assertEquals(2, a.size());
        assertFalse(a.get(0));
    }

    @Test
    public void test_removeAt() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.removeAt(1, 3);
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
        assertTrue(list.get(2));
    }

    @Test
    public void test_removeAt_empty() {
        BooleanList list = BooleanList.of(true, false);
        list.removeAt();
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAt_multipleIndices() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.removeAt(0, 2);
        assertEquals(2, list.size());
        assertFalse(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void testRemoveAt_multipleIndices_empty() {
        list.add(true);
        list.removeAt(new int[] {});
        assertEquals(1, list.size());
    }

    @Test
    public void test_removeRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void testRemoveRange_full() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.removeRange(0, 3);
        assertEquals(0, list.size());
    }

    @Test
    public void test_removeRange_sameIndices() {
        BooleanList list = BooleanList.of(true, false);
        list.removeRange(1, 1);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeRange_invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(2, 1));
    }

    @Test
    public void test_moveRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.moveRange(1, 3, 2); // true, false, false, true, true
        assertEquals(BooleanList.of(true, false, false, true, true), list);
    }

    @Test
    public void testMoveRange_forward() {
        BooleanList bl = BooleanList.of(true, false, true, false, true);
        bl.moveRange(0, 2, 3);
        assertEquals(5, bl.size());
    }

    @Test
    public void testMoveRange_backward() {
        BooleanList bl = BooleanList.of(true, false, true, false, true);
        bl.moveRange(3, 5, 0);
        assertEquals(5, bl.size());
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
    @DisplayName("Test replacing a range of elements with an array")
    public void testReplaceRange() {
        BooleanList list = BooleanList.of(true, false, false, true);
        list.replaceRange(1, 3, new boolean[] { true, true, true, true });
        assertArrayEquals(new boolean[] { true, true, true, true, true, true }, list.toArray());
    }

    @Test
    public void testReplaceRange_array_larger() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.replaceRange(1, 2, new boolean[] { false, true, false });
        assertEquals(5, list.size());
    }

    @Test
    public void testReplaceRange_BooleanList_withinBounds_expandShrink() {
        // Replace range with replacement of different size causing shrink
        BooleanList a = BooleanList.of(true, false, true, false, true);
        BooleanList rep = BooleanList.of(false);
        a.replaceRange(1, 4, rep);
        // [true, false, true]
        assertEquals(3, a.size());
        assertTrue(a.get(0));
        assertFalse(a.get(1));
        assertTrue(a.get(2));
    }

    @Test
    public void testReplaceRange_array_withinBounds_expandShrink() {
        // Replace with array
        BooleanList a = BooleanList.of(true, false, true, false, true);
        boolean[] rep = { false, false, false };
        a.replaceRange(1, 3, rep);
        // [true, false, false, false, false, true]
        assertEquals(6, a.size());
        assertTrue(a.get(0));
        assertFalse(a.get(1));
        assertFalse(a.get(2));
        assertFalse(a.get(3));
        assertFalse(a.get(4));
        assertTrue(a.get(5));
    }

    @Test
    public void testReplaceRange_BooleanList_noExpansion() {
        // replace 2 elements with 2 elements: size unchanged
        BooleanList a = BooleanList.of(true, false, true, false, true);
        BooleanList replacement = BooleanList.of(false, false);
        a.replaceRange(1, 3, replacement);
        assertEquals(5, a.size());
        assertTrue(a.get(0));
        assertFalse(a.get(1));
        assertFalse(a.get(2));
        assertFalse(a.get(3));
        assertTrue(a.get(4));
    }

    @Test
    public void testReplaceRange_array_noExpansion() {
        // replace 2 elements with 2 elements: size unchanged
        BooleanList a = BooleanList.of(true, false, true, false, true);
        boolean[] replacement = { false, false };
        a.replaceRange(1, 3, replacement);
        assertEquals(5, a.size());
        assertTrue(a.get(0));
        assertFalse(a.get(1));
        assertFalse(a.get(2));
        assertFalse(a.get(3));
        assertTrue(a.get(4));
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
    public void testReplaceRange_array_empty() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.replaceRange(1, 2, new boolean[] {});
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
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
    public void test_replaceAll_operator() {
        BooleanList list = BooleanList.of(true, false, true);
        list.replaceAll(b -> !b);
        assertFalse(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
    }

    @Test
    @DisplayName("Test replacing all occurrences of a value")
    public void testReplaceAllValue() {
        BooleanList list = BooleanList.of(true, false, true);
        int replacements = list.replaceAll(true, false);
        assertEquals(2, replacements);
        assertArrayEquals(new boolean[] { false, false, false }, list.toArray());
    }

    @Test
    @DisplayName("Test 'replaceAll' with a unary operator")
    public void testReplaceAllOperator() {
        BooleanList list = BooleanList.of(true, false, true);
        list.replaceAll(val -> !val);
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    public void test_replaceAll_oldNew_empty() {
        BooleanList list = new BooleanList();
        int count = list.replaceAll(true, false);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceAll_sameValue() {
        list.add(true);
        list.add(false);
        int count = list.replaceAll(true, true);
        assertEquals(1, count);
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
    public void testReplaceIf_allMatch() {
        list.add(true);
        list.add(true);
        list.add(true);
        assertTrue(list.replaceIf(b -> b, false));
        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            assertFalse(list.get(i));
        }
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
    @DisplayName("Test filling a range with a value")
    public void testFillRange() {
        BooleanList list = BooleanList.of(false, false, false, false);
        list.fill(1, 3, true);
        assertArrayEquals(new boolean[] { false, true, true, false }, list.toArray());
    }

    @Test
    public void testFill_empty() {
        list.fill(true);
        assertEquals(0, list.size());
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
    @DisplayName("Test 'contains' method")
    public void testContains() {
        BooleanList list = BooleanList.of(true, false);
        assertTrue(list.contains(true));
        assertTrue(list.contains(false));
    }

    @Test
    @DisplayName("Test 'contains', 'containsAny', and 'containsAll' methods")
    public void testContainsMethods() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.contains(true));
        assertTrue(list.containsAny(new boolean[] { false, false }));
        assertFalse(list.containsAny(new boolean[] {}));
        assertTrue(list.containsAll(BooleanList.of(true, false)));
        assertTrue(list.containsAll(BooleanList.of(true, true, true)));
    }

    @Test
    public void test_containsAny_BooleanList() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(false, false);
        assertTrue(list1.containsAny(list2));
    }

    @Test
    public void test_containsAny_array() {
        BooleanList list = BooleanList.of(true, true);
        boolean[] arr = { false };
        assertFalse(list.containsAny(arr));
    }

    @Test
    public void testContainsAny_array_noMatch() {
        list.add(true);
        assertFalse(list.containsAny(new boolean[] {}));
    }

    @Test
    public void testContainsAny_BooleanList_noMatch() {
        BooleanList a = BooleanList.of(true, true, true);
        BooleanList b = BooleanList.of(false, false);
        assertFalse(a.containsAny(b));
    }

    @Test
    public void testContainsAny_BooleanList_match() {
        BooleanList a = BooleanList.of(true, false, true);
        BooleanList b = BooleanList.of(false, false);
        assertTrue(a.containsAny(b));
    }

    @Test
    public void testContainsAny_array_noMatch_large() {
        BooleanList a = BooleanList.of(true, true, true);
        assertFalse(a.containsAny(new boolean[] { false }));
    }

    @Test
    public void testContainsAny_array_largeList() {
        BooleanList large = new BooleanList();
        for (int i = 0; i < 15; i++)
            large.add(i % 2 == 0);
        boolean[] query = { true, false };
        assertTrue(large.containsAny(query));
    }

    @Test
    public void testContainsAny_array_notContained_largeList() {
        // All elements are true; query has false
        BooleanList large = new BooleanList();
        for (int i = 0; i < 15; i++)
            large.add(true);
        boolean[] query = { false };
        assertFalse(large.containsAny(query));
    }

    @Test
    public void test_containsAny_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        assertFalse(list1.containsAny(list2));
    }

    @Test
    public void testContainsAny_BooleanList_largeList() {
        // Trigger needToSet path: min(lenA, lenB) > 3 && max(lenA, lenB) > 9
        BooleanList large = new BooleanList();
        for (int i = 0; i < 15; i++)
            large.add(i % 2 == 0);
        BooleanList query = BooleanList.of(true, true, true, true, true);
        assertTrue(large.containsAny(query));
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
    public void test_containsAll_array() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = { true };
        assertTrue(list.containsAll(arr));
    }

    @Test
    @DisplayName("Test 'containsAll' method")
    public void testContainsAll() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.containsAll(BooleanList.of(true, false)));
        assertTrue(list.containsAll(BooleanList.of(true, true, true)));
    }

    @Test
    public void testContainsAll_BooleanList_returnFalse_largeList() {
        // Large list: needToSet path, returns false when element not found
        BooleanList large = BooleanList.of(true, true, true, true, true, true, true, true, true, true, true);
        BooleanList query = BooleanList.of(true, false, true, false);
        // 'false' is not in large, should return false via set path
        assertFalse(large.containsAll(query));
    }

    @Test
    public void testContainsAll_array_notContained() {
        BooleanList a = BooleanList.of(true, true);
        assertFalse(a.containsAll(new boolean[] { false }));
    }

    @Test
    public void testContainsAll_array_largeList() {
        BooleanList large = new BooleanList();
        for (int i = 0; i < 15; i++)
            large.add(i % 2 == 0);
        boolean[] query = { true, false };
        assertTrue(large.containsAll(query));
    }

    @Test
    public void testContainsAll_BooleanList_smallList_true() {
        // Small lists (no set path)
        BooleanList a = BooleanList.of(true, false);
        BooleanList b = BooleanList.of(true);
        assertTrue(a.containsAll(b));
    }

    @Test
    public void test_containsAll_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true);
        BooleanList list2 = new BooleanList();
        assertTrue(list1.containsAll(list2));
    }

    @Test
    public void testContainsAll_array_empty() {
        BooleanList a = BooleanList.of(true, false);
        assertTrue(a.containsAll(new boolean[0]));
    }

    @Test
    public void testContainsAll_BooleanList_empty() {
        BooleanList a = new BooleanList();
        BooleanList b = BooleanList.of(true);
        assertFalse(a.containsAll(b));
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
    @DisplayName("Test 'disjoint' method")
    public void testDisjoint() {
        BooleanList list = BooleanList.of(true, true);
        assertTrue(list.disjoint(BooleanList.of(false, false)));
        assertFalse(list.disjoint(BooleanList.of(true, false)));
    }

    @Test
    public void testDisjoint_BooleanList_largeList_notDisjoint() {
        // needToSet path: both lists large enough
        BooleanList a = BooleanList.of(true, false, true, false, true, false, true, false, true, false, true);
        BooleanList b = BooleanList.of(false, false, false, false);
        assertFalse(a.disjoint(b));
    }

    @Test
    public void testDisjoint_BooleanList_largeList_disjoint() {
        // Large list but all-true vs all-false: disjoint via set path
        BooleanList a = BooleanList.of(true, true, true, true, true, true, true, true, true, true, true);
        BooleanList b = BooleanList.of(false, false, false, false);
        assertTrue(a.disjoint(b));
    }

    @Test
    public void testDisjoint_array_disjoint_large() {
        BooleanList a = BooleanList.of(true, true, true, true, true, true, true, true, true, true, true);
        assertFalse(a.disjoint(new boolean[] { true }));
    }

    @Test
    public void testDisjoint_array_notDisjoint() {
        BooleanList a = BooleanList.of(true, false);
        boolean[] b = { true };
        assertFalse(a.disjoint(b));
    }

    @Test
    public void testDisjoint_BooleanList_smallList() {
        BooleanList a = BooleanList.of(true, true);
        BooleanList b = BooleanList.of(false);
        assertTrue(a.disjoint(b));
    }

    @Test
    public void testDisjoint_BooleanList_smallList_notDisjoint() {
        BooleanList a = BooleanList.of(true, false);
        BooleanList b = BooleanList.of(true);
        assertFalse(a.disjoint(b));
    }

    @Test
    public void testDisjoint_array_empty() {
        list.add(true);
        assertTrue(list.disjoint(new boolean[] {}));
    }

    @Test
    public void testDisjoint_array_notEmpty() {
        BooleanList a = BooleanList.of(true, true);
        boolean[] b = { false, false };
        assertTrue(a.disjoint(b));
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
    public void test_intersection_array() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean[] arr = { true };
        BooleanList result = list.intersection(arr);
        assertEquals(1, result.size());
        assertTrue(result.get(0));
    }

    @Test
    @DisplayName("Test 'intersection' method")
    public void testIntersection() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);
        BooleanList intersection = list1.intersection(list2);
        assertEquals(2, intersection.size());
        assertTrue(intersection.contains(true));
        assertTrue(intersection.contains(false));
    }

    @Test
    public void test_intersection_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        BooleanList result = list1.intersection(list2);
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersection_array_empty() {
        list.add(true);
        list.add(false);
        BooleanList result = list.intersection(new boolean[] {});
        assertEquals(0, result.size());
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
    public void test_difference_array() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean[] arr = { false };
        BooleanList result = list.difference(arr);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Test 'difference' method")
    public void testDifference() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);
        BooleanList difference = list1.difference(list2);
        assertEquals(1, difference.size());
        assertTrue(difference.get(0));
    }

    @Test
    public void test_difference_BooleanList_empty() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = new BooleanList();
        BooleanList result = list1.difference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void testDifference_array_empty() {
        list.add(true);
        list.add(false);
        BooleanList result = list.difference(new boolean[] {});
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
    public void test_symmetricDifference_array() {
        BooleanList list = BooleanList.of(true, false);
        boolean[] arr = { false, false };
        BooleanList result = list.symmetricDifference(arr);
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Test 'symmetricDifference' method")
    public void testSymmetricDifference() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);
        BooleanList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new boolean[] { false, true }, symmDiff.toArray());
    }

    @Test
    public void testSymmetricDifference_BooleanList_noOverlap() {
        BooleanList a = BooleanList.of(true, true);
        BooleanList b = BooleanList.of(false, false);
        BooleanList result = a.symmetricDifference(b);
        assertEquals(4, result.size());
    }

    @Test
    public void testSymmetricDifference_BooleanList_noOverlap_newTest() {
        BooleanList a = BooleanList.of(true, true);
        BooleanList b = BooleanList.of(false, false);
        BooleanList result = a.symmetricDifference(b);
        assertEquals(4, result.size());
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
    public void testSymmetricDifference_array() {
        list.add(true);
        list.add(false);
        BooleanList result = list.symmetricDifference(new boolean[] { false, true });
        assertNotNull(result);
    }

    @Test
    public void testSymmetricDifference_array_empty() {
        list.add(true);
        BooleanList result = list.symmetricDifference(new boolean[] {});
        assertEquals(1, result.size());
        assertTrue(result.get(0));
    }

    @Test
    public void testSymmetricDifference_array_overlapping() {
        BooleanList a = BooleanList.of(true, false);
        boolean[] b = { true, true };
        BooleanList result = a.symmetricDifference(b);
        assertNotNull(result);
        // elements in a not in b union elements in b not in a
        assertTrue(result.size() > 0);
    }

    // Additional tests for uncovered branches

    @Test
    public void testSymmetricDifference_BooleanList_bothEmpty() {
        BooleanList a = new BooleanList();
        BooleanList b = new BooleanList();
        BooleanList result = a.symmetricDifference(b);
        assertEquals(0, result.size());
    }

    @Test
    public void testSymmetricDifference_BooleanList_thisEmpty() {
        BooleanList a = new BooleanList();
        BooleanList b = BooleanList.of(true, false);
        BooleanList result = a.symmetricDifference(b);
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference_BooleanList_otherEmpty() {
        BooleanList a = BooleanList.of(true, false);
        BooleanList b = new BooleanList();
        BooleanList result = a.symmetricDifference(b);
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference_BooleanList_withOverlap() {
        BooleanList a = BooleanList.of(true, false, true);
        BooleanList b = BooleanList.of(true, true, false);
        BooleanList result = a.symmetricDifference(b);
        // a has 2 true, 1 false; b has 2 true, 1 false -> symmetric diff = empty
        assertEquals(0, result.size());
    }

    @Test
    public void testSymmetricDifference_array_thisEmpty_newTest() {
        BooleanList a = new BooleanList();
        boolean[] b = { true, false };
        BooleanList result = a.symmetricDifference(b);
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference_array_otherEmpty_newTest() {
        BooleanList a = BooleanList.of(true, false);
        boolean[] b = {};
        BooleanList result = a.symmetricDifference(b);
        assertEquals(2, result.size());
    }

    @Test
    public void test_frequency() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        assertEquals(3, list.frequency(true));
        assertEquals(2, list.frequency(false));
    }

    @Test
    public void test_frequency_empty() {
        BooleanList list = new BooleanList();
        assertEquals(0, list.frequency(true));
    }

    @Test
    public void testFrequency_multipleTrue() {
        list.add(true);
        list.add(true);
        list.add(false);
        list.add(true);
        assertEquals(3, list.frequency(true));
        assertEquals(1, list.frequency(false));
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
    @DisplayName("Test finding the index of an element")
    public void testIndexOf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        assertEquals(0, list.indexOf(true));
        assertEquals(1, list.indexOf(false));
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
    public void test_lastIndexOf_withStartIndex_beyondSize() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(2, list.lastIndexOf(true, 10));
    }

    @Test
    public void testLastIndexOf_withStartIndex_largeBeyond() {
        BooleanList a = BooleanList.of(true, false, true, false, true);
        // startIndexFromBack beyond size - should clamp to size-1
        assertEquals(4, a.lastIndexOf(true, 100));
        assertEquals(3, a.lastIndexOf(false, 100));
    }

    @Test
    public void testLastIndexOf_withStartIndex_found() {
        BooleanList a = BooleanList.of(true, false, true, false);
        assertEquals(2, a.lastIndexOf(true, 2));
        assertEquals(1, a.lastIndexOf(false, 2));
    }

    @Test
    public void test_lastIndexOf_withStartIndex_negative() {
        BooleanList list = BooleanList.of(true, false);
        assertEquals(-1, list.lastIndexOf(true, -1));
    }

    @Test
    public void testLastIndexOf_withFromIndex() {
        BooleanList a = BooleanList.of(true, false, true, false, true);
        assertEquals(2, a.lastIndexOf(true, 2));
        assertEquals(-1, a.lastIndexOf(false, 0));
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
    public void testForEach_empty() {
        List<Boolean> collected = new ArrayList<>();
        list.forEach(b -> collected.add(b));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEach_withRange_empty() {
        list.add(true);
        list.add(false);
        List<Boolean> collected = new ArrayList<>();
        list.forEach(1, 1, b -> collected.add(b));
        assertTrue(collected.isEmpty());
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
    public void testDistinct_empty() {
        BooleanList result = list.distinct(0, 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testDistinct_allSame() {
        list.add(true);
        list.add(true);
        list.add(true);
        BooleanList result = list.distinct(0, 3);
        assertEquals(1, result.size());
        assertTrue(result.get(0));
    }

    @Test
    public void test_containsDuplicates() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.containsDuplicates());
    }

    @Test
    public void test_containsDuplicates_noDuplicates() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void test_containsDuplicates_sizeOne() {
        BooleanList list = BooleanList.of(true);
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_twoDistinct() {
        list.add(true);
        list.add(false);
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void test_containsDuplicates_empty() {
        BooleanList list = new BooleanList();
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void test_containsDuplicates_sizeTwo_same() {
        BooleanList list = BooleanList.of(true, true);
        assertTrue(list.containsDuplicates());
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
    @DisplayName("Test 'isSorted' method")
    public void testIsSorted() {
        assertTrue(BooleanList.of(false, false, true, true).isSorted());
        assertFalse(BooleanList.of(true, false).isSorted());
        assertTrue(BooleanList.of(true, true).isSorted());
        assertTrue(new BooleanList().isSorted());
    }

    @Test
    public void test_isSorted_empty() {
        BooleanList list = new BooleanList();
        assertTrue(list.isSorted());
    }

    @Test
    public void testIsSorted_singleElement() {
        list.add(true);
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
    @DisplayName("Test sorting the list")
    public void testSort() {
        BooleanList list = BooleanList.of(true, false, true, false, false);
        list.sort();
        assertArrayEquals(new boolean[] { false, false, false, true, true }, list.toArray());
    }

    @Test
    public void testSort_allTrue() {
        list.add(true);
        list.add(true);
        list.sort();
        assertTrue(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void testSort_withMixedValues() {
        BooleanList a = BooleanList.of(true, false, true, false, false, true);
        a.sort();
        // After sort, all false come first
        assertFalse(a.get(0));
        assertFalse(a.get(1));
        assertFalse(a.get(2));
        assertTrue(a.get(3));
        assertTrue(a.get(4));
        assertTrue(a.get(5));
    }

    @Test
    public void testSort_allTrue_newTest() {
        BooleanList a = BooleanList.of(true, true, false, true);
        a.sort();
        assertFalse(a.get(0));
        assertTrue(a.get(1));
        assertTrue(a.get(2));
        assertTrue(a.get(3));
    }

    @Test
    public void testSort_allFalse() {
        BooleanList a = BooleanList.of(false, false, false);
        a.sort();
        assertFalse(a.get(0));
        assertFalse(a.get(2));
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
    public void testReverseSort_empty() {
        list.reverseSort();
        assertEquals(0, list.size());
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
    public void test_reverse_withRange() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.reverse(1, 3);
        assertTrue(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
        assertFalse(list.get(3));
    }

    @Test
    @DisplayName("Test reversing the list")
    public void testReverse() {
        BooleanList list = BooleanList.of(true, false, false);
        list.reverse();
        assertArrayEquals(new boolean[] { false, false, true }, list.toArray());
    }

    @Test
    @DisplayName("Test 'reverse' method on a range")
    public void testReverseRange() {
        BooleanList list = BooleanList.of(true, false, false, true, true);
        list.reverse(1, 4);
        assertArrayEquals(new boolean[] { true, true, false, false, true }, list.toArray());
    }

    @Test
    public void test_reverse_singleElement() {
        BooleanList list = BooleanList.of(true);
        list.reverse();
        assertEquals(1, list.size());
    }

    @Test
    public void testReverse_empty() {
        list.reverse();
        assertEquals(0, list.size());
    }

    @Test
    public void testReverse_withRange_empty() {
        list.add(true);
        list.add(false);
        list.reverse(0, 0);
        assertTrue(list.get(0));
        assertFalse(list.get(1));
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
    @DisplayName("Test 'rotate' method")
    public void testRotate() {
        BooleanList list = BooleanList.of(true, true, false, false);
        list.rotate(2);
        assertArrayEquals(new boolean[] { false, false, true, true }, list.toArray());
        list.rotate(-1);
        assertArrayEquals(new boolean[] { false, true, true, false }, list.toArray());
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
    public void testRotate_empty() {
        list.rotate(5);
        assertEquals(0, list.size());
    }

    @Test
    public void testRotate_zero() {
        list.add(true);
        list.add(false);
        list.rotate(0);
        assertTrue(list.get(0));
        assertFalse(list.get(1));
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
    public void testShuffle_random() {
        BooleanList a = BooleanList.of(true, false, true, false, true, false);
        a.shuffle(new Random(42));
        assertEquals(6, a.size());
    }

    @Test
    public void test_shuffle_singleElement() {
        BooleanList list = BooleanList.of(true);
        list.shuffle();
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Test 'shuffle' method")
    public void testShuffle() {
        BooleanList list1 = BooleanList.of(true, false, true, false, true, false);
        BooleanList list2 = list1.copy();
        list1.shuffle();
        assertEquals(list1.size(), list2.size());
        assertTrue(N.isEqualCollection(list1.toList(), list2.toList()), "Both lists should contain the same elements after shuffle");
    }

    @Test
    public void testShuffle_empty() {
        list.shuffle();
        assertEquals(0, list.size());
    }

    @Test
    public void testShuffle_withRandom_nonEmpty() {
        BooleanList a = BooleanList.of(true, false, true, false, true, false, true, false, true, false);
        Random rnd = new Random(42);
        a.shuffle(rnd);
        assertEquals(10, a.size());
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
    @DisplayName("Test 'swap' method")
    public void testSwap() {
        BooleanList list = BooleanList.of(true, false, true);
        list.swap(0, 2);
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
        list.swap(0, 1);
        assertArrayEquals(new boolean[] { false, true, true }, list.toArray());
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
    public void testSwap_outOfBounds() {
        list.add(true);
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 5));
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
    @DisplayName("Test 'copy' method for deep copy")
    public void testCopy() {
        BooleanList original = BooleanList.of(true, false);
        BooleanList copied = original.copy();

        assertNotSame(original, copied);
        assertEquals(original, copied);

        original.add(true);
        assertNotEquals(original, copied);
    }

    @Test
    public void testCopy_empty() {
        BooleanList copy = list.copy();
        assertEquals(0, copy.size());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopy_withRange_empty() {
        BooleanList copy = list.copy(0, 0);
        assertEquals(0, copy.size());
    }

    @Test
    public void testCopy_withStep_negative() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        BooleanList copy = list.copy(3, 0, -1);
        assertEquals(3, copy.size());
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
    @DisplayName("Test 'split' method")
    public void testSplit() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        List<BooleanList> chunks = list.split(2);
        assertEquals(3, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0).toArray());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(1).toArray());
        assertArrayEquals(new boolean[] { true }, chunks.get(2).toArray());
    }

    @Test
    public void testSplit_withRange() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);
        List<BooleanList> chunks = list.split(1, 4, 2);
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
    }

    @Test
    public void testSplit_empty() {
        List<BooleanList> chunks = list.split(0, 0, 1);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void test_split_invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IllegalArgumentException.class, () -> list.split(0, 2, 0));
    }

    @Test
    public void testTrimToSizeActuallyTrims() {
        BooleanList largeCapList = new BooleanList(1000);
        largeCapList.add(true);
        largeCapList.add(false);

        assertEquals(2, largeCapList.size());

        largeCapList.trimToSize();
        assertEquals(2, largeCapList.size());
        assertEquals(true, largeCapList.get(0));
        assertEquals(false, largeCapList.get(1));

        largeCapList.add(true);
        assertEquals(3, largeCapList.size());
    }

    @Test
    public void testTrimToSize_alreadyTrimmed() {
        BooleanList bl = BooleanList.of(true, false);
        bl.trimToSize();
        assertEquals(2, bl.size());
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
    @DisplayName("Test clearing the list")
    public void testClear() {
        BooleanList list = BooleanList.of(true, false);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testClear_nonEmpty() {
        BooleanList a = BooleanList.of(true, false, true);
        a.clear();
        assertEquals(0, a.size());
        assertTrue(a.isEmpty());
    }

    @Test
    public void testClear_emptyList_noOp() {
        BooleanList a = new BooleanList();
        a.clear();
        assertEquals(0, a.size());
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
    @DisplayName("Test 'boxed' method to convert to List<Boolean>")
    public void testBoxed() {
        BooleanList list = BooleanList.of(true, false);
        List<Boolean> boxedList = list.boxed();
        assertEquals(2, boxedList.size());
        assertEquals(Boolean.TRUE, boxedList.get(0));
        assertEquals(Boolean.FALSE, boxedList.get(1));
    }

    @Test
    public void testBoxed_empty() {
        List<Boolean> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxed_withRange_empty() {
        list.add(true);
        List<Boolean> boxed = list.boxed(0, 0);
        assertTrue(boxed.isEmpty());
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
    @DisplayName("Test 'toArray' method")
    public void testToArray() {
        boolean[] arr = { true, false, true };
        BooleanList list = BooleanList.of(arr);
        assertArrayEquals(arr, list.toArray());
    }

    @Test
    public void testToArray_empty() {
        boolean[] arr = list.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void test_toCollection() {
        BooleanList list = BooleanList.of(true, false, true);
        List<Boolean> collection = list.toCollection(0, 3, ArrayList::new);
        assertEquals(3, collection.size());
        assertTrue(collection.get(0));
    }

    @Test
    public void testToCollection() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> collection = list.toCollection(0, list.size(), ArrayList::new);
        assertEquals(3, collection.size());
        assertEquals(Boolean.TRUE, collection.get(0));
        assertEquals(Boolean.FALSE, collection.get(1));
        assertEquals(Boolean.TRUE, collection.get(2));
    }

    @Test
    public void testToCollection_withRange() {
        list.add(true);
        list.add(false);
        list.add(true);
        ArrayList<Boolean> result = list.toCollection(0, 2, ArrayList::new);
        assertEquals(2, result.size());
        assertTrue(result.get(0));
        assertFalse(result.get(1));
    }

    @Test
    public void testToCollection_empty() {
        ArrayList<Boolean> result = list.toCollection(0, 0, ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_toMultiset() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        Multiset<Boolean> multiset = list.toMultiset(0, 5, Multiset::new);
        assertEquals(3, multiset.get(true));
        assertEquals(2, multiset.get(false));
    }

    @Test
    public void testToMultiset() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        Multiset<Boolean> multiset = list.toMultiset(0, list.size(), Multiset::new);
        assertEquals(3, multiset.count(Boolean.TRUE));
        assertEquals(2, multiset.count(Boolean.FALSE));
    }

    @Test
    public void testToMultiset_withRange() {
        list.add(true);
        list.add(true);
        list.add(false);
        Multiset<Boolean> multiset = list.toMultiset(0, 3, Multiset::new);
        assertEquals(2, multiset.getCount(true));
        assertEquals(1, multiset.getCount(false));
    }

    @Test
    public void testToMultiset_empty() {
        Multiset<Boolean> multiset = list.toMultiset(0, 0, Multiset::new);
        assertEquals(0, multiset.size());
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
    @DisplayName("Test iterator functionality")
    public void testIterator() {
        BooleanList list = BooleanList.of(true, false);
        BooleanIterator it = list.iterator();
        assertTrue(it.hasNext());
        assertTrue(it.nextBoolean());
        assertTrue(it.hasNext());
        assertFalse(it.nextBoolean());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIterator_hasNextAndNext() {
        list.add(true);
        list.add(false);
        BooleanIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertTrue(iter.nextBoolean());
        assertTrue(iter.hasNext());
        assertFalse(iter.nextBoolean());
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
    @DisplayName("Test 'stream' method functionality")
    public void testStream() {
        BooleanList list = BooleanList.of(true, false, true);
        long count = list.stream().filter(Boolean::booleanValue).count();
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test stream functionality on a range")
    public void testStreamRange() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        long trueCount = list.stream(1, 4).filter(b -> b).count();
        assertEquals(2, trueCount);
    }

    @Test
    public void testStream_empty() {
        Stream<Boolean> s = list.stream();
        assertEquals(0, s.toList().size());
    }

    @Test
    public void testStream_withRange_empty() {
        list.add(true);
        Stream<Boolean> s = list.stream(0, 0);
        assertEquals(0, s.toList().size());
    }

    @Test
    public void test_getFirst() {
        BooleanList list = BooleanList.of(true, false);
        assertTrue(list.getFirst());
    }

    @Test
    public void testGetFirst_singleElement() {
        list.add(false);
        assertFalse(list.getFirst());
    }

    @Test
    public void test_getFirst_empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    @DisplayName("Test 'getFirst' and 'getLast' methods")
    public void testGetFirstLast() {
        BooleanList list = BooleanList.of(true, false, false, true);
        assertTrue(list.getFirst());
        assertTrue(list.getLast());

        BooleanList emptyList = new BooleanList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::getLast);
    }

    @Test
    public void testGetFirstAndGetLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(true, list.getFirst());
        assertEquals(true, list.getLast());
    }

    @Test
    public void test_getLast() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.getLast());
    }

    @Test
    public void testGetLast_singleElement() {
        list.add(false);
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
    @DisplayName("Test 'addFirst' and 'addLast' methods")
    public void testAddFirstLast() {
        BooleanList list = new BooleanList();
        list.addLast(true);
        list.addFirst(false);
        list.addLast(false);
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    public void testAddFirstAndAddLast() {
        list.add(true);

        list.addFirst(false);
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));

        list.addLast(true);
        assertEquals(3, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(true, list.get(2));
    }

    @Test
    public void testAddFirst_toNonEmpty() {
        list.add(false);
        list.addFirst(true);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void test_addLast() {
        BooleanList list = BooleanList.of(true, false);
        list.addLast(true);
        assertEquals(3, list.size());
        assertTrue(list.get(2));
    }

    @Test
    public void testAddLast_toNonEmpty() {
        list.add(true);
        list.addLast(false);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
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
    @DisplayName("Test 'removeFirst' and 'removeLast' methods")
    public void testRemoveFirstLast() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.removeFirst());
        assertEquals("[false, true]", list.toString());
        assertTrue(list.removeLast());
        assertEquals("[false]", list.toString());
    }

    @Test
    public void testRemoveFirst_singleElement() {
        list.add(true);
        boolean removed = list.removeFirst();
        assertTrue(removed);
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_removeFirst_empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveFirstAndRemoveLast() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
        assertThrows(NoSuchElementException.class, () -> list.removeLast());

        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(true, list.removeFirst());
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));

        assertEquals(true, list.removeLast());
        assertEquals(1, list.size());
        assertEquals(false, list.get(0));
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
    public void testRemoveLast_singleElement() {
        list.add(false);
        boolean removed = list.removeLast();
        assertFalse(removed);
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_removeLast_empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testHashCode() {
        list.add(true);
        list.add(false);

        BooleanList other = new BooleanList();
        other.add(true);
        other.add(false);

        assertEquals(list.hashCode(), other.hashCode());

        other.add(true);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCode_differentContent() {
        list.add(true);
        BooleanList other = BooleanList.of(false);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void test_hashCode_sameContent() {
        BooleanList list1 = BooleanList.of(true, false, true);
        BooleanList list2 = BooleanList.of(true, false, true);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testHashCode_empty() {
        BooleanList other = new BooleanList();
        assertEquals(list.hashCode(), other.hashCode());
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
    public void test_equals_differentType() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.equals("not a list"));
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        BooleanList list1 = BooleanList.of(true, false, true);
        BooleanList list2 = BooleanList.of(true, false, true);
        BooleanList list3 = BooleanList.of(false, true, false);

        assertEquals(list1, list1);

        assertEquals(list1, list2);
        assertEquals(list2, list1);

        BooleanList list4 = BooleanList.of(true, false, true);
        assertEquals(list1, list2);
        assertEquals(list2, list4);
        assertEquals(list1, list4);

        assertEquals(list1.hashCode(), list2.hashCode());

        assertNotEquals(list1, list3);
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
    public void test_equals_null() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.equals(null));
    }

    @Test
    @DisplayName("Test 'equals' and 'hashCode' methods")
    public void testEqualsAndHashCode() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(true, false);
        BooleanList list3 = BooleanList.of(false, true);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(list1, null);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());
        assertNotEquals(list1.hashCode(), list3.hashCode());
    }

    @Test
    public void testEquals() {
        list.add(true);
        list.add(false);

        assertEquals(list, list);

        BooleanList other = new BooleanList();
        other.add(true);
        other.add(false);
        assertEquals(list, other);

        other.add(true);
        assertNotEquals(list, other);

        BooleanList different = new BooleanList();
        different.add(false);
        different.add(true);
        assertNotEquals(list, different);

        assertNotEquals(list, null);

        assertNotEquals(list, "not a list");
    }

    @Test
    public void testEquals_self() {
        list.add(true);
        assertEquals(list, list);
    }

    @Test
    public void testEquals_empty() {
        BooleanList other = new BooleanList();
        assertEquals(list, other);
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add(true);
        list.add(false);
        list.add(true);
        assertEquals("[true, false, true]", list.toString());
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

    @Test
    public void testToString_singleElement() {
        list.add(true);
        assertEquals("[true]", list.toString());
    }

    @Test
    public void testEnsureCapacity_exactCapacity() {
        // ensureCapacity is triggered internally when adding many elements
        BooleanList a = new BooleanList(2);
        for (int i = 0; i < 15; i++)
            a.add(i % 2 == 0);
        assertEquals(15, a.size());
        // capacity grows automatically
    }

    // --- Tests for uncovered branches in containsAll(BooleanList), disjoint(BooleanList),
    //     replaceRange, batchRemove branches, ensureCapacity, retainAll, containsAny ---

    @Test
    public void testContainsAll_BooleanList_largeList_needToSet() {
        // needToSet triggers when min(lenA,lenB) > 3 and max > 9
        // Build a large list and a query that uses the set path
        BooleanList large = BooleanList.of(true, false, true, false, true, false, true, false, true, false, true);
        BooleanList query = BooleanList.of(true, false, true, false);
        assertTrue(large.containsAll(query));

        BooleanList query2 = BooleanList.of(true, false, true, false);
        BooleanList small = BooleanList.of(true, false, true);
        // small list won't use set path; make sure both branches work
        assertTrue(small.containsAll(BooleanList.of(true)));
    }

    @Test
    public void testEnsureCapacity_forcesGrowth() {
        // Create a list that starts with default capacity, add many elements
        BooleanList a = new BooleanList();
        // Add 20 elements to trigger multiple capacity doublings
        for (int i = 0; i < 20; i++) {
            a.add(i % 2 == 0);
        }
        assertEquals(20, a.size());
    }

}
