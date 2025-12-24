package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.stream.ShortStream;

@Tag("2025")
public class ShortList2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        ShortList list = new ShortList();
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        ShortList list = new ShortList(10);
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructorWithZeroCapacity() {
        ShortList list = new ShortList(0);
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ShortList(-1));
    }

    @Test
    public void testConstructorWithArray() {
        short[] arr = { (short) 1, (short) 2, (short) 3 };
        ShortList list = new ShortList(arr);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        short[] arr = { (short) 1, (short) 2, (short) 3, (short) 4, (short) 5 };
        ShortList list = new ShortList(arr, 3);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testConstructorWithArrayAndInvalidSize() {
        short[] arr = { (short) 1, (short) 2, (short) 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new ShortList(arr, 5));
        assertThrows(IllegalArgumentException.class, () -> new ShortList(arr, -1));
    }

    @Test
    public void testOf() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testOfEmpty() {
        ShortList list = ShortList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithArrayAndSize() {
        short[] arr = { (short) 1, (short) 2, (short) 3, (short) 4 };
        ShortList list = ShortList.of(arr, 2);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
    }

    @Test
    public void testCopyOf() {
        short[] arr = { (short) 1, (short) 2, (short) 3 };
        ShortList list = ShortList.copyOf(arr);
        assertEquals(3, list.size());
        arr[0] = (short) 100;
        assertEquals((short) 1, list.get(0));
    }

    @Test
    public void testCopyOfNull() {
        ShortList list = ShortList.copyOf(null);
        assertEquals(0, list.size());
    }

    @Test
    public void testCopyOfRange() {
        short[] arr = { (short) 1, (short) 2, (short) 3, (short) 4, (short) 5 };
        ShortList list = ShortList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 4, list.get(2));
    }

    @Test
    public void testRange() {
        ShortList list = ShortList.range((short) 1, (short) 5);
        assertEquals(4, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 4, list.get(3));
    }

    @Test
    public void testRangeEmpty() {
        ShortList list = ShortList.range((short) 5, (short) 5);
        assertEquals(0, list.size());
    }

    @Test
    public void testRangeWithStep() {
        ShortList list = ShortList.range((short) 0, (short) 10, (short) 2);
        assertEquals(5, list.size());
        assertEquals((short) 0, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 4, list.get(2));
        assertEquals((short) 6, list.get(3));
        assertEquals((short) 8, list.get(4));
    }

    @Test
    public void testRangeClosed() {
        ShortList list = ShortList.rangeClosed((short) 1, (short) 5);
        assertEquals(5, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 5, list.get(4));
    }

    @Test
    public void testRangeClosedWithStep() {
        ShortList list = ShortList.rangeClosed((short) 0, (short) 10, (short) 2);
        assertEquals(6, list.size());
        assertEquals((short) 0, list.get(0));
        assertEquals((short) 10, list.get(5));
    }

    @Test
    public void testRepeat() {
        ShortList list = ShortList.repeat((short) 5, 3);
        assertEquals(3, list.size());
        assertEquals((short) 5, list.get(0));
        assertEquals((short) 5, list.get(1));
        assertEquals((short) 5, list.get(2));
    }

    @Test
    public void testRandom() {
        ShortList list = ShortList.random(5);
        assertEquals(5, list.size());
    }

    @Test
    public void testRandomEmpty() {
        ShortList list = ShortList.random(0);
        assertEquals(0, list.size());
    }

    @Test
    public void testArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short[] arr = list.array();
        assertEquals((short) 1, arr[0]);
        assertEquals((short) 2, arr[1]);
        assertEquals((short) 3, arr[2]);
    }

    @Test
    public void testGet() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);
        assertEquals((short) 10, list.get(0));
        assertEquals((short) 20, list.get(1));
        assertEquals((short) 30, list.get(2));
    }

    @Test
    public void testGetOutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @Test
    public void testSet() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short oldValue = list.set(1, (short) 100);
        assertEquals((short) 2, oldValue);
        assertEquals((short) 100, list.get(1));
    }

    @Test
    public void testSetOutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, (short) 100));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, (short) 100));
    }

    @Test
    public void testAdd() {
        ShortList list = new ShortList();
        list.add((short) 1);
        list.add((short) 2);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
    }

    @Test
    public void testAddAtIndex() {
        ShortList list = ShortList.of((short) 1, (short) 3);
        list.add(1, (short) 2);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testAddAtIndexOutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, (short) 100));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, (short) 100));
    }

    @Test
    public void testAddAllShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2);
        ShortList list2 = ShortList.of((short) 3, (short) 4);
        boolean result = list1.addAll(list2);
        assertTrue(result);
        assertEquals(4, list1.size());
        assertEquals((short) 3, list1.get(2));
        assertEquals((short) 4, list1.get(3));
    }

    @Test
    public void testAddAllShortListEmpty() {
        ShortList list1 = ShortList.of((short) 1, (short) 2);
        ShortList list2 = new ShortList();
        boolean result = list1.addAll(list2);
        assertFalse(result);
        assertEquals(2, list1.size());
    }

    @Test
    public void testAddAllShortListAtIndex() {
        ShortList list1 = ShortList.of((short) 1, (short) 4);
        ShortList list2 = ShortList.of((short) 2, (short) 3);
        boolean result = list1.addAll(1, list2);
        assertTrue(result);
        assertEquals(4, list1.size());
        assertEquals((short) 1, list1.get(0));
        assertEquals((short) 2, list1.get(1));
        assertEquals((short) 3, list1.get(2));
        assertEquals((short) 4, list1.get(3));
    }

    @Test
    public void testAddAllArray() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        short[] arr = { (short) 3, (short) 4 };
        boolean result = list.addAll(arr);
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 4, list.get(3));
    }

    @Test
    public void testAddAllArrayAtIndex() {
        ShortList list = ShortList.of((short) 1, (short) 4);
        short[] arr = { (short) 2, (short) 3 };
        boolean result = list.addAll(1, arr);
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testRemoveShort() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        boolean result = list.remove((short) 2);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 2, list.get(2));
    }

    @Test
    public void testRemoveShortNotFound() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        boolean result = list.remove((short) 100);
        assertFalse(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAllOccurrences() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 4, (short) 2);
        boolean result = list.removeAllOccurrences((short) 2);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 4, list.get(2));
    }

    @Test
    public void testRemoveAllOccurrencesNotFound() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        boolean result = list.removeAllOccurrences((short) 100);
        assertFalse(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAllShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortList list2 = ShortList.of((short) 2, (short) 4);
        boolean result = list1.removeAll(list2);
        assertTrue(result);
        assertEquals(2, list1.size());
        assertEquals((short) 1, list1.get(0));
        assertEquals((short) 3, list1.get(1));
    }

    @Test
    public void testRemoveAllArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        short[] arr = { (short) 2, (short) 4 };
        boolean result = list.removeAll(arr);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
    }

    @Test
    public void testRemoveIf() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        boolean result = list.removeIf(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return value % 2 == 0;
            }
        });
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 5, list.get(2));
    }

    @Test
    public void testRemoveDuplicates() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 1, (short) 4);
        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 4, list.get(3));
    }

    @Test
    public void testRemoveDuplicatesNoDuplicates() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        boolean result = list.removeDuplicates();
        assertFalse(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRetainAllShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortList list2 = ShortList.of((short) 2, (short) 4, (short) 5);
        boolean result = list1.retainAll(list2);
        assertTrue(result);
        assertEquals(2, list1.size());
        assertEquals((short) 2, list1.get(0));
        assertEquals((short) 4, list1.get(1));
    }

    @Test
    public void testRetainAllArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        short[] arr = { (short) 2, (short) 4, (short) 5 };
        boolean result = list.retainAll(arr);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 4, list.get(1));
    }

    @Test
    public void testDelete() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short deleted = list.delete(1);
        assertEquals((short) 2, deleted);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
    }

    @Test
    public void testDeleteOutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
    }

    @Test
    public void testDeleteAllByIndices() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 5, list.get(2));
    }

    @Test
    public void testDeleteRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 5, list.get(1));
    }

    @Test
    public void testDeleteRangeEmpty() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testMoveRange() {
        ShortList list = ShortList.of((short) 0, (short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.moveRange(1, 3, 3);
        assertEquals(ShortList.of((short) 0, (short) 3, (short) 4, (short) 1, (short) 2, (short) 5), list);
    }

    @Test
    public void testReplaceRangeWithShortList() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortList replacement = ShortList.of((short) 10, (short) 20);
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 5, list.get(3));
    }

    @Test
    public void testReplaceRangeWithArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        short[] replacement = { (short) 10, (short) 20 };
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 5, list.get(3));
    }

    @Test
    public void testReplaceAllOldNew() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 4);
        int count = list.replaceAll((short) 2, (short) 100);
        assertEquals(2, count);
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 100, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 100, list.get(3));
        assertEquals((short) 4, list.get(4));
    }

    @Test
    public void testReplaceAllOperator() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.replaceAll(new ShortUnaryOperator() {
            @Override
            public short applyAsShort(short operand) {
                return (short) (operand * 2);
            }
        });
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 4, list.get(1));
        assertEquals((short) 6, list.get(2));
    }

    @Test
    public void testReplaceIf() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        boolean result = list.replaceIf(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return value % 2 == 0;
            }
        }, (short) 100);
        assertTrue(result);
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 100, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 100, list.get(3));
    }

    @Test
    public void testFill() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.fill((short) 100);
        assertEquals((short) 100, list.get(0));
        assertEquals((short) 100, list.get(1));
        assertEquals((short) 100, list.get(2));
    }

    @Test
    public void testFillRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.fill(1, 4, (short) 100);
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 100, list.get(1));
        assertEquals((short) 100, list.get(2));
        assertEquals((short) 100, list.get(3));
        assertEquals((short) 5, list.get(4));
    }

    @Test
    public void testContains() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(list.contains((short) 2));
        assertFalse(list.contains((short) 100));
    }

    @Test
    public void testContainsAnyShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 3, (short) 4, (short) 5);
        assertTrue(list1.containsAny(list2));

        ShortList list3 = ShortList.of((short) 10, (short) 11);
        assertFalse(list1.containsAny(list3));
    }

    @Test
    public void testContainsAnyArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short[] arr1 = { (short) 3, (short) 4, (short) 5 };
        assertTrue(list.containsAny(arr1));

        short[] arr2 = { (short) 10, (short) 11 };
        assertFalse(list.containsAny(arr2));
    }

    @Test
    public void testContainsAllShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortList list2 = ShortList.of((short) 2, (short) 3);
        assertTrue(list1.containsAll(list2));

        ShortList list3 = ShortList.of((short) 2, (short) 5);
        assertFalse(list1.containsAll(list3));
    }

    @Test
    public void testContainsAllArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        short[] arr1 = { (short) 2, (short) 3 };
        assertTrue(list.containsAll(arr1));

        short[] arr2 = { (short) 2, (short) 5 };
        assertFalse(list.containsAll(arr2));
    }

    @Test
    public void testDisjointShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 4, (short) 5, (short) 6);
        assertTrue(list1.disjoint(list2));

        ShortList list3 = ShortList.of((short) 3, (short) 4);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void testDisjointArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short[] arr1 = { (short) 4, (short) 5, (short) 6 };
        assertTrue(list.disjoint(arr1));

        short[] arr2 = { (short) 3, (short) 4 };
        assertFalse(list.disjoint(arr2));
    }

    @Test
    public void testIntersectionShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortList list2 = ShortList.of((short) 3, (short) 4, (short) 5, (short) 6);
        ShortList result = list1.intersection(list2);
        assertEquals(2, result.size());
        assertEquals((short) 3, result.get(0));
        assertEquals((short) 4, result.get(1));
    }

    @Test
    public void testIntersectionArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        short[] arr = { (short) 3, (short) 4, (short) 5, (short) 6 };
        ShortList result = list.intersection(arr);
        assertEquals(2, result.size());
        assertEquals((short) 3, result.get(0));
        assertEquals((short) 4, result.get(1));
    }

    @Test
    public void testDifferenceShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        ShortList list2 = ShortList.of((short) 3, (short) 4, (short) 5);
        ShortList result = list1.difference(list2);
        assertEquals(2, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 2, result.get(1));
    }

    @Test
    public void testDifferenceArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        short[] arr = { (short) 3, (short) 4, (short) 5 };
        ShortList result = list.difference(arr);
        assertEquals(2, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 2, result.get(1));
    }

    @Test
    public void testSymmetricDifferenceShortList() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 3, (short) 4, (short) 5);
        ShortList result = list1.symmetricDifference(list2);
        assertTrue(result.size() > 0);
        assertTrue(result.contains((short) 1));
        assertTrue(result.contains((short) 2));
        assertTrue(result.contains((short) 4));
        assertTrue(result.contains((short) 5));
    }

    @Test
    public void testSymmetricDifferenceArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short[] arr = { (short) 3, (short) 4, (short) 5 };
        ShortList result = list.symmetricDifference(arr);
        assertTrue(result.size() > 0);
        assertTrue(result.contains((short) 1));
        assertTrue(result.contains((short) 2));
        assertTrue(result.contains((short) 4));
        assertTrue(result.contains((short) 5));
    }

    @Test
    public void testOccurrencesOf() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 2);
        assertEquals(3, list.occurrencesOf((short) 2));
        assertEquals(1, list.occurrencesOf((short) 1));
        assertEquals(0, list.occurrencesOf((short) 100));
    }

    @Test
    public void testIndexOf() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        assertEquals(1, list.indexOf((short) 2));
        assertEquals(0, list.indexOf((short) 1));
        assertEquals(-1, list.indexOf((short) 100));
    }

    @Test
    public void testIndexOfFromIndex() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        assertEquals(3, list.indexOf((short) 2, 2));
        assertEquals(-1, list.indexOf((short) 2, 4));
    }

    @Test
    public void testLastIndexOf() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        assertEquals(3, list.lastIndexOf((short) 2));
        assertEquals(0, list.lastIndexOf((short) 1));
        assertEquals(-1, list.lastIndexOf((short) 100));
    }

    @Test
    public void testLastIndexOfFromBack() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        assertEquals(1, list.lastIndexOf((short) 2, 2));
        assertEquals(-1, list.lastIndexOf((short) 2, 0));
    }

    @Test
    public void testMin() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
        OptionalShort min = list.min();
        assertTrue(min.isPresent());
        assertEquals((short) 1, min.get());
    }

    @Test
    public void testMinEmpty() {
        ShortList list = new ShortList();
        OptionalShort min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMinRange() {
        ShortList list = ShortList.of((short) 1, (short) 5, (short) 2, (short) 8, (short) 3);
        OptionalShort min = list.min(1, 4);
        assertTrue(min.isPresent());
        assertEquals((short) 2, min.get());
    }

    @Test
    public void testMax() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
        OptionalShort max = list.max();
        assertTrue(max.isPresent());
        assertEquals((short) 4, max.get());
    }

    @Test
    public void testMaxEmpty() {
        ShortList list = new ShortList();
        OptionalShort max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testMaxRange() {
        ShortList list = ShortList.of((short) 1, (short) 5, (short) 2, (short) 8, (short) 3);
        OptionalShort max = list.max(1, 4);
        assertTrue(max.isPresent());
        assertEquals((short) 8, max.get());
    }

    @Test
    public void testMedian() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 2);
        OptionalShort median = list.median();
        assertTrue(median.isPresent());
        assertEquals((short) 2, median.get());
    }

    @Test
    public void testMedianEmpty() {
        ShortList list = new ShortList();
        OptionalShort median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    public void testMedianRange() {
        ShortList list = ShortList.of((short) 1, (short) 5, (short) 2, (short) 8, (short) 3);
        OptionalShort median = list.median(1, 4);
        assertTrue(median.isPresent());
    }

    @Test
    public void testForEach() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        final ShortList result = new ShortList();
        list.forEach(new ShortConsumer() {
            @Override
            public void accept(short value) {
                result.add(value);
            }
        });
        assertEquals(3, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 2, result.get(1));
        assertEquals((short) 3, result.get(2));
    }

    @Test
    public void testForEachRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        final ShortList result = new ShortList();
        list.forEach(1, 4, new ShortConsumer() {
            @Override
            public void accept(short value) {
                result.add(value);
            }
        });
        assertEquals(3, result.size());
        assertEquals((short) 2, result.get(0));
        assertEquals((short) 3, result.get(1));
        assertEquals((short) 4, result.get(2));
    }

    @Test
    public void testFirst() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        OptionalShort first = list.first();
        assertTrue(first.isPresent());
        assertEquals((short) 1, first.get());
    }

    @Test
    public void testFirstEmpty() {
        ShortList list = new ShortList();
        OptionalShort first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        OptionalShort last = list.last();
        assertTrue(last.isPresent());
        assertEquals((short) 3, last.get());
    }

    @Test
    public void testLastEmpty() {
        ShortList list = new ShortList();
        OptionalShort last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 1);
        ShortList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertTrue(distinct.contains((short) 1));
        assertTrue(distinct.contains((short) 2));
        assertTrue(distinct.contains((short) 3));
    }

    @Test
    public void testHasDuplicates() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3);
        assertTrue(list1.hasDuplicates());

        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertFalse(list2.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        assertTrue(list1.isSorted());

        ShortList list2 = ShortList.of((short) 1, (short) 3, (short) 2, (short) 4);
        assertFalse(list2.isSorted());
    }

    @Test
    public void testSort() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
        list.sort();
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 4, list.get(3));
    }

    @Test
    public void testParallelSort() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
        list.parallelSort();
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 4, list.get(3));
    }

    @Test
    public void testReverseSort() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
        list.reverseSort();
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 2, list.get(2));
        assertEquals((short) 1, list.get(3));
    }

    @Test
    public void testBinarySearch() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        int index = list.binarySearch((short) 3);
        assertEquals(2, index);
    }

    @Test
    public void testBinarySearchNotFound() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 4, (short) 5);
        int index = list.binarySearch((short) 3);
        assertTrue(index < 0);
    }

    @Test
    public void testBinarySearchRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        int index = list.binarySearch(1, 4, (short) 3);
        assertEquals(2, index);
    }

    @Test
    public void testReverse() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        list.reverse();
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 2, list.get(2));
        assertEquals((short) 1, list.get(3));
    }

    @Test
    public void testReverseRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.reverse(1, 4);
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 4, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 2, list.get(3));
        assertEquals((short) 5, list.get(4));
    }

    @Test
    public void testRotate() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.rotate(2);
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 5, list.get(1));
        assertEquals((short) 1, list.get(2));
        assertEquals((short) 2, list.get(3));
        assertEquals((short) 3, list.get(4));
    }

    @Test
    public void testShuffle() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.shuffle();
        assertEquals(5, list.size());
        assertTrue(list.contains((short) 1));
        assertTrue(list.contains((short) 2));
        assertTrue(list.contains((short) 3));
        assertTrue(list.contains((short) 4));
        assertTrue(list.contains((short) 5));
    }

    @Test
    public void testShuffleWithRandom() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        Random rnd = new Random(12345);
        list.shuffle(rnd);
        assertEquals(5, list.size());
    }

    @Test
    public void testSwap() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.swap(0, 2);
        assertEquals((short) 3, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 1, list.get(2));
    }

    @Test
    public void testCopy() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList copy = list.copy();
        assertEquals(3, copy.size());
        assertEquals((short) 1, copy.get(0));
        assertEquals((short) 2, copy.get(1));
        assertEquals((short) 3, copy.get(2));

        list.set(0, (short) 100);
        assertEquals((short) 1, copy.get(0));
    }

    @Test
    public void testCopyRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals((short) 2, copy.get(0));
        assertEquals((short) 3, copy.get(1));
        assertEquals((short) 4, copy.get(2));
    }

    @Test
    public void testCopyWithStep() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertEquals((short) 1, copy.get(0));
        assertEquals((short) 3, copy.get(1));
        assertEquals((short) 5, copy.get(2));
    }

    @Test
    public void testSplit() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        List<ShortList> splits = list.split(0, 5, 2);
        assertEquals(3, splits.size());
        assertEquals(2, splits.get(0).size());
        assertEquals(2, splits.get(1).size());
        assertEquals(1, splits.get(2).size());
    }

    @Test
    public void testTrimToSize() {
        ShortList list = new ShortList(100);
        list.add((short) 1);
        list.add((short) 2);
        list.trimToSize();
        assertEquals(2, list.size());
    }

    @Test
    public void testClear() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        ShortList list = new ShortList();
        assertTrue(list.isEmpty());

        list.add((short) 1);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        ShortList list = new ShortList();
        assertEquals(0, list.size());

        list.add((short) 1);
        assertEquals(1, list.size());

        list.add((short) 2);
        assertEquals(2, list.size());
    }

    @Test
    public void testBoxed() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        List<Short> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Short.valueOf((short) 1), boxed.get(0));
        assertEquals(Short.valueOf((short) 2), boxed.get(1));
        assertEquals(Short.valueOf((short) 3), boxed.get(2));
    }

    @Test
    public void testBoxedRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        List<Short> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Short.valueOf((short) 2), boxed.get(0));
        assertEquals(Short.valueOf((short) 3), boxed.get(1));
        assertEquals(Short.valueOf((short) 4), boxed.get(2));
    }

    @Test
    public void testToArray() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short[] arr = list.toArray();
        assertEquals(3, arr.length);
        assertEquals((short) 1, arr[0]);
        assertEquals((short) 2, arr[1]);
        assertEquals((short) 3, arr[2]);

        arr[0] = (short) 100;
        assertEquals((short) 1, list.get(0));
    }

    @Test
    public void testToIntList() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        IntList intList = list.toIntList();
        assertEquals(3, intList.size());
        assertEquals(1, intList.get(0));
        assertEquals(2, intList.get(1));
        assertEquals(3, intList.get(2));
    }

    @Test
    public void testToCollection() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        List<Short> collection = list.toCollection(0, 3, new java.util.function.IntFunction<List<Short>>() {
            @Override
            public List<Short> apply(int capacity) {
                return new ArrayList<>(capacity);
            }
        });
        assertEquals(3, collection.size());
    }

    @Test
    public void testToMultiset() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3);
        Multiset<Short> multiset = list.toMultiset(0, 4, new java.util.function.IntFunction<Multiset<Short>>() {
            @Override
            public Multiset<Short> apply(int capacity) {
                return new Multiset<>();
            }
        });
        assertNotNull(multiset);
    }

    @Test
    public void testIterator() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortIterator iter = list.iterator();
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertEquals((short) 1, iter.nextShort());
        assertEquals((short) 2, iter.nextShort());
        assertEquals((short) 3, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortStream stream = list.stream();
        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    @Test
    public void testStreamRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        ShortStream stream = list.stream(1, 4);
        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    @Test
    public void testGetFirst() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals((short) 1, list.getFirst());
    }

    @Test
    public void testGetFirstEmpty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals((short) 3, list.getLast());
    }

    @Test
    public void testGetLastEmpty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testAddFirst() {
        ShortList list = ShortList.of((short) 2, (short) 3);
        list.addFirst((short) 1);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testAddLast() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        list.addLast((short) 3);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }

    @Test
    public void testRemoveFirst() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short removed = list.removeFirst();
        assertEquals((short) 1, removed);
        assertEquals(2, list.size());
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 3, list.get(1));
    }

    @Test
    public void testRemoveFirstEmpty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short removed = list.removeLast();
        assertEquals((short) 3, removed);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
    }

    @Test
    public void testRemoveLastEmpty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testHashCode() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list3 = ShortList.of((short) 1, (short) 2, (short) 4);

        assertTrue(list1.equals(list2));
        assertFalse(list1.equals(list3));
        assertFalse(list1.equals(null));
        assertFalse(list1.equals("string"));
    }

    @Test
    public void testEqualsSameInstance() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(list.equals(list));
    }

    @Test
    public void testToString() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void testToStringEmpty() {
        ShortList list = new ShortList();
        String str = list.toString();
        assertNotNull(str);
    }
}
