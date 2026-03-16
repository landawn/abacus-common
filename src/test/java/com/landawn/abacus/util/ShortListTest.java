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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.stream.ShortStream;

@Tag("2025")
public class ShortListTest extends TestBase {

    private ShortList list;

    @BeforeEach
    public void setUp() {
        list = new ShortList();
    }

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
        short[] arr = list.internalArray();
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
        short deleted = list.removeAt(1);
        assertEquals((short) 2, deleted);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
    }

    @Test
    public void testDeleteOutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
    }

    @Test
    public void testDeleteAllByIndices() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.removeAt(1, 3);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 5, list.get(2));
    }

    @Test
    public void testDeleteRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 5, list.get(1));
    }

    @Test
    public void testDeleteRangeEmpty() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.removeRange(1, 1);
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
        assertEquals(3, list.frequency((short) 2));
        assertEquals(1, list.frequency((short) 1));
        assertEquals(0, list.frequency((short) 100));
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
        assertTrue(list1.containsDuplicates());

        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertFalse(list2.containsDuplicates());
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

    @Test
    @DisplayName("Test constructors and static factories")
    public void testConstructorsAndFactories() {
        ShortList list1 = new ShortList();
        assertTrue(list1.isEmpty());

        ShortList list2 = new ShortList(20);
        assertTrue(list2.isEmpty());
        assertEquals(20, list2.internalArray().length);

        short[] data = { 1, 2, 3 };
        ShortList list3 = new ShortList(data);
        assertEquals(3, list3.size());
        data[0] = 5;
        assertEquals((short) 5, list3.get(0));

        ShortList list4 = ShortList.of((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2 }, list4.toArray());

        short[] original = { 10, 20 };
        ShortList list5 = ShortList.copyOf(original);
        original[0] = 15;
        assertEquals((short) 10, list5.get(0));

        ShortList list6 = ShortList.copyOf(new short[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, list6.toArray());
    }

    @Test
    @DisplayName("Test range, repeat, and random factories")
    public void testRangeRepeatRandom() {
        assertArrayEquals(new short[] { 5, 6, 7 }, ShortList.range((short) 5, (short) 8).toArray());
        assertArrayEquals(new short[] { 5, 6, 7, 8 }, ShortList.rangeClosed((short) 5, (short) 8).toArray());
        assertArrayEquals(new short[] { 0, 3, 6 }, ShortList.rangeClosed((short) 0, (short) 8, (short) 3).toArray());
        assertArrayEquals(new short[] { 7, 7, 7 }, ShortList.repeat((short) 7, 3).toArray());
        assertEquals(10, ShortList.random(10).size());
    }

    @Test
    @DisplayName("Test add, get, set, addFirst, addLast methods")
    public void testAddGetSet() {
        ShortList list = new ShortList();
        list.add((short) 5);
        list.add(0, (short) 1);
        list.addFirst((short) 0);
        list.addLast((short) 10);
        assertArrayEquals(new short[] { 0, 1, 5, 10 }, list.toArray());
        assertEquals((short) 1, list.get(1));

        short oldValue = list.set(1, (short) 99);
        assertEquals((short) 1, oldValue);
        assertEquals((short) 99, list.get(1));
    }

    @Test
    @DisplayName("Test adding all elements at a specific index")
    public void testAddAllAtIndex() {
        ShortList list = ShortList.of((short) 1, (short) 5);
        list.addAll(1, new short[] { 2, 3, 4 });
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test remove, removeAll, retainAll, and removeIf methods")
    public void testRemoveMethods() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 4);

        assertTrue(list.remove((short) 2));
        assertArrayEquals(new short[] { 1, 3, 2, 4 }, list.toArray());

        assertTrue(list.removeAll(ShortList.of((short) 1, (short) 4)));
        assertArrayEquals(new short[] { 3, 2 }, list.toArray());

        list.addAll(ShortList.of((short) 5, (short) 6));
        assertTrue(list.retainAll(new short[] { 2, 6, 7 }));
        assertArrayEquals(new short[] { 2, 6 }, list.toArray());

        assertTrue(list.removeIf(val -> val > 5));
        assertArrayEquals(new short[] { 2 }, list.toArray());
    }

    @Test
    @DisplayName("Test removeDuplicates and removeAllOccurrences")
    public void testRemoveDuplicatesAndOccurrences() {
        ShortList list = ShortList.of((short) 1, (short) 5, (short) 1, (short) 2, (short) 5);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new short[] { 1, 5, 2 }, list.toArray());

        ShortList list2 = ShortList.of((short) 1, (short) 5, (short) 1, (short) 2, (short) 5);
        assertTrue(list2.removeAllOccurrences((short) 1));
        assertArrayEquals(new short[] { 5, 2, 5 }, list2.toArray());
    }

    @Test
    @DisplayName("Test deletion by index and range")
    public void testDeleteMethods() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);

        short deleted = list.removeAt(1);
        assertEquals((short) 2, deleted);
        assertArrayEquals(new short[] { 1, 3, 4, 5, 6 }, list.toArray());

        list.removeRange(1, 3);
        assertArrayEquals(new short[] { 1, 5, 6 }, list.toArray());

        list.removeAt(0, 2);
        assertArrayEquals(new short[] { 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test moveRange and replaceRange methods")
    public void testMoveAndReplaceRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.moveRange(1, 3, 3);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 3 }, list.toArray());

        list.replaceRange(0, 2, ShortList.of((short) 9, (short) 8, (short) 7));
        assertArrayEquals(new short[] { 9, 8, 7, 5, 2, 3 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'contains', 'containsAny', 'containsAll', and 'disjoint' methods")
    public void testQueryMethods() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30, (short) 10);
        assertTrue(list.contains((short) 20));
        assertEquals(0, list.indexOf((short) 10));
        assertEquals(3, list.lastIndexOf((short) 10));

        assertTrue(list.containsAny(new short[] { 5, 15, 20 }));
        assertFalse(list.containsAny(new short[] { 5, 15, 25 }));

        assertTrue(list.containsAll(ShortList.of((short) 10, (short) 30)));
        assertFalse(list.containsAll(ShortList.of((short) 10, (short) 40)));

        assertTrue(list.disjoint(ShortList.of((short) 1, (short) 2)));
        assertFalse(list.disjoint(new short[] { 15, 25, 30 }));
    }

    @Test
    @DisplayName("Test statistical methods with ranges")
    public void testStatsWithRange() {
        ShortList list = ShortList.of((short) 9, (short) 2, (short) 7, (short) 5, (short) 2);
        assertEquals(OptionalShort.of((short) 2), list.min(1, 4));
        assertEquals(OptionalShort.of((short) 7), list.max(1, 4));
        assertEquals(OptionalShort.of((short) 5), list.median(1, 4));
        assertEquals(2, list.frequency((short) 2));
    }

    @Test
    @DisplayName("Test sorting, binary search, and isSorted")
    public void testSortAndSearch() {
        ShortList list = ShortList.of((short) 9, (short) 2, (short) 7, (short) 5, (short) 1);
        assertFalse(list.isSorted());

        list.sort();
        assertArrayEquals(new short[] { 1, 2, 5, 7, 9 }, list.toArray());
        assertTrue(list.isSorted());

        assertEquals(2, list.binarySearch((short) 5));
        assertTrue(list.binarySearch((short) 6) < 0);

        list.parallelSort();
        assertArrayEquals(new short[] { 1, 2, 5, 7, 9 }, list.toArray());

        list.reverseSort();
        assertArrayEquals(new short[] { 9, 7, 5, 2, 1 }, list.toArray());
    }

    @Test
    @DisplayName("Test in-place modifications: reverse(range), rotate, swap, fill")
    public void testModifications() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.reverse(1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, list.toArray());
        list.rotate(-1);
        assertArrayEquals(new short[] { 4, 3, 2, 5, 1 }, list.toArray());
        list.swap(0, 4);
        assertArrayEquals(new short[] { 1, 3, 2, 5, 4 }, list.toArray());
        list.fill((short) 0);
        assertArrayEquals(new short[] { 0, 0, 0, 0, 0 }, list.toArray());
    }

    @Test
    @DisplayName("Test set-like operations: intersection, difference, symmetricDifference")
    public void testSetLikeOps() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 2, (short) 3, (short) 4, (short) 4);

        ShortList intersection = list1.intersection(list2);
        intersection.sort();
        assertArrayEquals(new short[] { 2, 3 }, intersection.toArray());

        ShortList difference = list1.difference(list2);
        assertArrayEquals(new short[] { 1, 2 }, difference.toArray());

        ShortList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new short[] { 1, 2, 4, 4 }, symmDiff.toArray());
    }

    @Test
    @DisplayName("Test conversion methods: toArray, boxed, toIntList, stream")
    public void testConversions() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);

        assertArrayEquals(new short[] { 10, 20, 30 }, list.toArray());

        List<Short> boxed = list.boxed();
        assertEquals(List.of((short) 10, (short) 20, (short) 30), boxed);

        IntList intList = list.toIntList();
        assertArrayEquals(new int[] { 10, 20, 30 }, intList.toArray());

        assertEquals(50, list.stream(1, 3).sum());
    }

    @Test
    @DisplayName("Test first/last element accessors and removers")
    public void testFirstLastAccess() {
        ShortList list = ShortList.of((short) 5, (short) 10, (short) 15);
        assertEquals((short) 5, list.getFirst());
        assertEquals((short) 15, list.getLast());
        assertEquals(OptionalShort.of((short) 5), list.first());
        assertEquals(OptionalShort.of((short) 15), list.last());

        assertEquals((short) 5, list.removeFirst());
        assertEquals((short) 15, list.removeLast());
        assertArrayEquals(new short[] { 10 }, list.toArray());

        ShortList emptyList = new ShortList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::removeLast);
    }

    @Test
    @DisplayName("Test equals and hashCode")
    public void testEqualsAndHashCode() {
        ShortList list1 = ShortList.of((short) 1, (short) 2);
        ShortList list2 = ShortList.of((short) 1, (short) 2);
        ShortList list3 = ShortList.of((short) 2, (short) 1);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    @DisplayName("Test copy, split, and trimToSize methods")
    public void testCopySplitTrim() {
        ShortList listWithCap = new ShortList(10);
        listWithCap.addAll(new short[] { 1, 2, 3 });
        assertEquals(10, listWithCap.internalArray().length);
        listWithCap.trimToSize();
        assertEquals(3, listWithCap.internalArray().length);

        ShortList copy = listWithCap.copy(0, 3, 2);
        assertArrayEquals(new short[] { 1, 3 }, copy.toArray());

        List<ShortList> chunks = listWithCap.split(2);
        assertEquals(2, chunks.size());
        assertArrayEquals(new short[] { 1, 2 }, chunks.get(0).toArray());
        assertArrayEquals(new short[] { 3 }, chunks.get(1).toArray());
    }

    @Test
    @DisplayName("Test toCollection and toMultiset")
    public void testToCollectionAndMultiset() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 10);
        ArrayList<Short> collection = list.toCollection(ArrayList::new);
        assertEquals(List.of((short) 10, (short) 20, (short) 10), collection);

        Multiset<Short> multiset = list.toMultiset();
        assertEquals(2, multiset.count((short) 10));
        assertEquals(1, multiset.count((short) 20));
    }

    @Test
    public void testShortBoundaryValues() {
        list.add(Short.MIN_VALUE);
        list.add((short) -1);
        list.add((short) 0);
        list.add((short) 1);
        list.add(Short.MAX_VALUE);

        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals((short) -1, list.get(1));
        assertEquals((short) 0, list.get(2));
        assertEquals((short) 1, list.get(3));
        assertEquals(Short.MAX_VALUE, list.get(4));

        assertEquals(OptionalShort.of(Short.MIN_VALUE), list.min());
        assertEquals(OptionalShort.of(Short.MAX_VALUE), list.max());
    }

    @Test
    public void testShortOverflowInOperations() {
        list.add(Short.MAX_VALUE);
        list.add((short) (Short.MAX_VALUE - 1));
        list.add(Short.MIN_VALUE);
        list.add((short) (Short.MIN_VALUE + 1));

        list.replaceAll(s -> (short) (s + 1));

        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals(Short.MAX_VALUE, list.get(1));
        assertEquals((short) (Short.MIN_VALUE + 1), list.get(2));
        assertEquals((short) (Short.MIN_VALUE + 2), list.get(3));
    }

    @Test
    public void testRangeWithExtremeValues() {
        ShortList list1 = ShortList.range((short) 32760, (short) 32767);
        assertEquals(7, list1.size());
        assertEquals((short) 32760, list1.get(0));
        assertEquals((short) 32766, list1.get(6));

        ShortList list2 = ShortList.range((short) 10, (short) 0, (short) -1);
        assertEquals(10, list2.size());
        assertEquals((short) 10, list2.get(0));
        assertEquals((short) 1, list2.get(9));

        ShortList list3 = ShortList.rangeClosed((short) -1000, (short) 1000, (short) 100);
        assertEquals(21, list3.size());
        assertEquals((short) -1000, list3.get(0));
        assertEquals((short) 1000, list3.get(20));
    }

    @Test
    public void testRangeWithLargeSteps() {
        ShortList list1 = ShortList.range((short) 0, (short) 10, (short) 20);
        assertEquals(1, list1.size());
        assertEquals((short) 0, list1.get(0));

        ShortList list2 = ShortList.range((short) -100, (short) -200, (short) -25);
        assertEquals(4, list2.size());
        assertEquals((short) -100, list2.get(0));
        assertEquals((short) -125, list2.get(1));
        assertEquals((short) -150, list2.get(2));
        assertEquals((short) -175, list2.get(3));
    }

    @Test
    public void testMinMaxMedianWithDuplicates() {
        for (int i = 0; i < 100; i++) {
            list.add((short) 42);
        }
        assertEquals(OptionalShort.of((short) 42), list.min());
        assertEquals(OptionalShort.of((short) 42), list.max());
        assertEquals(OptionalShort.of((short) 42), list.median());

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);
        OptionalShort median = list.median();
        assertTrue(median.isPresent());
        short medianValue = median.get();
        assertTrue(medianValue == 2 || medianValue == 3);
    }

    @Test
    public void testMinMaxMedianWithLargeDataset() {
        Random rand = new Random(42);
        for (int i = 0; i < 10000; i++) {
            list.add((short) rand.nextInt(65536));
        }

        OptionalShort min = list.min();
        OptionalShort max = list.max();
        OptionalShort median = list.median();

        assertTrue(min.isPresent());
        assertTrue(max.isPresent());
        assertTrue(median.isPresent());

        assertTrue(min.get() <= median.get());
        assertTrue(median.get() <= max.get());

        OptionalShort partialMin = list.min(1000, 2000);
        OptionalShort partialMax = list.max(1000, 2000);
        assertTrue(partialMin.isPresent());
        assertTrue(partialMax.isPresent());
        assertTrue(partialMin.get() <= partialMax.get());
    }

    @Test
    public void testBinarySearchWithDuplicates() {
        list.add((short) 1);
        list.add((short) 3);
        list.add((short) 3);
        list.add((short) 3);
        list.add((short) 5);
        list.add((short) 7);

        int index = list.binarySearch((short) 3);
        assertTrue(index >= 1 && index <= 3);
        assertEquals((short) 3, list.get(index));

        assertTrue(list.binarySearch((short) 4) < 0);
        assertTrue(list.binarySearch((short) 0) < 0);
        assertTrue(list.binarySearch((short) 10) < 0);
    }

    @Test
    public void testBinarySearchWithNegativeValues() {
        list.add((short) -100);
        list.add((short) -50);
        list.add((short) 0);
        list.add((short) 50);
        list.add((short) 100);

        assertEquals(0, list.binarySearch((short) -100));
        assertEquals(1, list.binarySearch((short) -50));
        assertEquals(2, list.binarySearch((short) 0));
        assertEquals(3, list.binarySearch((short) 50));
        assertEquals(4, list.binarySearch((short) 100));

        assertEquals(2, list.binarySearch(1, 4, (short) 0));
        assertTrue(list.binarySearch(0, 2, (short) 50) < 0);
    }

    @Test
    public void testParallelSortWithVariousDataPatterns() {
        for (int i = 0; i < 1000; i++) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        for (int i = 1000; i >= 0; i--) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        Random rand = new Random(42);
        for (int i = 0; i < 2000; i++) {
            list.add((short) rand.nextInt(65536));
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1) <= list.get(i));
        }
    }

    @Test
    public void testRemoveIfWithComplexPredicates() {
        for (short i = -100; i <= 100; i++) {
            list.add(i);
        }

        assertTrue(list.removeIf(s -> s % 3 == 0 || s % 5 == 0));

        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            assertTrue(value % 3 != 0 && value % 5 != 0);
        }

        assertTrue(list.removeIf(s -> s < 0));
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 0);
        }
    }

    @Test
    public void testBatchRemoveOptimization() {
        for (int i = 0; i < 1000; i++) {
            list.add((short) (i % 100));
        }

        ShortList toRemove = new ShortList();
        for (int i = 0; i < 50; i++) {
            toRemove.add((short) (i * 2));
        }

        int originalSize = list.size();
        list.removeAll(toRemove);
        assertTrue(list.size() < originalSize);

        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            assertTrue(value % 2 == 1 || value >= 100);
        }
    }

    @Test
    public void testReplaceAllWithOverflow() {
        list.add((short) 20000);
        list.add((short) 25000);
        list.add((short) -20000);

        list.replaceAll(s -> (short) (s * 2));

        assertEquals((short) -25536, list.get(0));
        assertEquals((short) -15536, list.get(1));
        assertEquals((short) 25536, list.get(2));
    }

    @Test
    public void testReplaceIfWithRangeConditions() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        assertTrue(list.replaceIf(s -> s >= 20 && s <= 40, (short) -1));

        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            if (i >= 20 && i <= 40) {
                assertEquals((short) -1, value);
            } else {
                assertEquals((short) i, value);
            }
        }
    }

    @Test
    public void testIntersectionWithLargeMultisets() {
        for (int i = 0; i < 100; i++) {
            list.add((short) (i % 10));
        }

        ShortList other = new ShortList();
        for (int i = 0; i < 50; i++) {
            other.add((short) (i % 5));
        }

        ShortList intersection = list.intersection(other);

        int[] counts = new int[5];
        for (int i = 0; i < intersection.size(); i++) {
            counts[intersection.get(i)]++;
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(10, counts[i]);
        }
    }

    @Test
    public void testSymmetricDifferenceWithComplexCases() {
        list.add((short) 0);
        list.add((short) 0);
        list.add((short) 1);
        list.add((short) 1);
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 2);
        list.add((short) 2);
        list.add((short) 2);

        ShortList other = ShortList.of((short) 0, (short) 1, (short) 1, (short) 2, (short) 2, (short) 2, (short) 3, (short) 3);

        ShortList symDiff = list.symmetricDifference(other);

        assertEquals(5, symDiff.size());

        int zeros = 0, ones = 0, twos = 0, threes = 0;
        for (int i = 0; i < symDiff.size(); i++) {
            switch (symDiff.get(i)) {
                case 0:
                    zeros++;
                    break;
                case 1:
                    ones++;
                    break;
                case 2:
                    twos++;
                    break;
                case 3:
                    threes++;
                    break;
            }
        }
        assertEquals(1, zeros);
        assertEquals(1, ones);
        assertEquals(1, twos);
        assertEquals(2, threes);
    }

    @Test
    public void testStreamOperations() {
        for (short i = 1; i <= 100; i++) {
            list.add(i);
        }

        ShortStream stream = list.stream();
        int sum = stream.sum();
        assertEquals(5050, sum);

        ShortStream rangeStream = list.stream(10, 20);
        short[] arr = rangeStream.toArray();
        assertEquals(10, arr.length);
        for (int i = 0; i < 10; i++) {
            assertEquals((short) (11 + i), arr[i]);
        }
    }

    @Test
    public void testStreamWithFiltering() {
        for (short i = 0; i < 50; i++) {
            list.add(i);
        }

        ShortStream stream = list.stream();
        short[] evens = stream.filter(s -> s % 2 == 0).toArray();
        assertEquals(25, evens.length);
        for (int i = 0; i < evens.length; i++) {
            assertEquals((short) (i * 2), evens[i]);
        }
    }

    @Test
    public void testToIntListWithSignExtension() {
        list.add((short) -1);
        list.add((short) -32768);
        list.add((short) 32767);
        list.add((short) 0);

        IntList intList = list.toIntList();
        assertEquals(4, intList.size());
        assertEquals(-1, intList.get(0));
        assertEquals(-32768, intList.get(1));
        assertEquals(32767, intList.get(2));
        assertEquals(0, intList.get(3));
    }

    @Test
    public void testForEachWithEarlyTermination() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        final int[] count = { 0 };
        final List<Short> collected = new ArrayList<>();

        list.forEach(s -> {
            collected.add(s);
            count[0]++;
        });

        assertEquals(100, count[0]);
        assertEquals(100, collected.size());

        collected.clear();
        list.forEach(50, 40, s -> collected.add(s));
        assertEquals(10, collected.size());
        assertEquals(Short.valueOf((short) 50), collected.get(0));
        assertEquals(Short.valueOf((short) 41), collected.get(9));
    }

    @Test
    public void testCopyWithNegativeStep() {
        for (short i = 0; i < 20; i++) {
            list.add(i);
        }

        ShortList reversed = list.copy(19, -1, -1);
        assertEquals(20, reversed.size());
        for (int i = 0; i < 20; i++) {
            assertEquals((short) (19 - i), reversed.get(i));
        }

        ShortList everyThirdReverse = list.copy(18, -1, -3);
        assertEquals(7, everyThirdReverse.size());
        assertEquals((short) 18, everyThirdReverse.get(0));
        assertEquals((short) 15, everyThirdReverse.get(1));
        assertEquals((short) 12, everyThirdReverse.get(2));
        assertEquals((short) 9, everyThirdReverse.get(3));
        assertEquals((short) 6, everyThirdReverse.get(4));
        assertEquals((short) 3, everyThirdReverse.get(5));
        assertEquals((short) 0, everyThirdReverse.get(6));
    }

    @Test
    public void testSplitWithPrimeNumberSize() {
        for (short i = 0; i < 23; i++) {
            list.add(i);
        }

        List<ShortList> chunks = list.split(0, 23, 5);
        assertEquals(5, chunks.size());
        assertEquals(5, chunks.get(0).size());
        assertEquals(5, chunks.get(1).size());
        assertEquals(5, chunks.get(2).size());
        assertEquals(5, chunks.get(3).size());
        assertEquals(3, chunks.get(4).size());

        short expected = 0;
        for (ShortList chunk : chunks) {
            for (int i = 0; i < chunk.size(); i++) {
                assertEquals(expected++, chunk.get(i));
            }
        }
    }

    @Test
    public void testArrayMethodReturnsSameReference() {
        list.add((short) 1);
        list.add((short) 2);

        short[] array1 = list.internalArray();
        short[] array2 = list.internalArray();

        assertSame(array1, array2);

        array1[0] = 100;
        assertEquals((short) 100, list.get(0));
    }

    @Test
    public void testCapacityGrowthWithLargeDataset() {
        ShortList smallList = new ShortList(2);
        for (int i = 0; i < 10000; i++) {
            smallList.add((short) i);
        }
        assertEquals(10000, smallList.size());

        for (int i = 0; i < Math.min(100, smallList.size()); i++) {
            assertEquals((short) i, smallList.get(i));
        }
    }

    @Test
    public void testLargeDatasetOperations() {
        final int size = 20000;
        for (int i = 0; i < size; i++) {
            list.add((short) (i % 1000));
        }

        assertEquals(size, list.size());

        assertTrue(list.contains((short) 500));
        assertTrue(list.indexOf((short) 999) >= 0);

        list.sort();
        assertTrue(list.isSorted());

        ShortList distinct = list.distinct(0, list.size());
        assertEquals(1000, distinct.size());

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testEmptyListBehaviors() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
        assertEquals(list, new ShortList());
        assertEquals(list.hashCode(), new ShortList().hashCode());

        assertFalse(list.contains((short) 0));
        assertEquals(-1, list.indexOf((short) 0));
        assertEquals(-1, list.lastIndexOf((short) 0));
        assertEquals(0, list.frequency((short) 0));

        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.median().isPresent());
        assertFalse(list.first().isPresent());
        assertFalse(list.last().isPresent());

        assertTrue(list.toArray().length == 0);
        assertTrue(list.boxed().isEmpty());
        assertTrue(list.distinct(0, 0).isEmpty());
        assertFalse(list.iterator().hasNext());

        list.sort();
        list.parallelSort();
        list.reverse();
        list.shuffle();
        list.fill((short) 0);
        list.removeRange(0, 0);

        assertTrue(list.isEmpty());
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list3 = ShortList.of((short) 3, (short) 2, (short) 1);

        assertEquals(list1, list1);

        assertEquals(list1, list2);
        assertEquals(list2, list1);

        ShortList list4 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(list1, list2);
        assertEquals(list2, list4);
        assertEquals(list1, list4);

        assertEquals(list1.hashCode(), list2.hashCode());

        assertNotEquals(list1, list3);

        assertNotEquals(list1, null);
        assertNotEquals(list1, new ArrayList<>());
    }

    @Test
    public void testConstructors() {
        ShortList list1 = new ShortList();
        assertEquals(0, list1.size());

        ShortList list2 = new ShortList(10);
        assertEquals(0, list2.size());

        short[] arr = { 1, 2, 3 };
        ShortList list3 = new ShortList(arr);
        assertEquals(3, list3.size());
        assertEquals((short) 1, list3.get(0));
        assertEquals((short) 2, list3.get(1));
        assertEquals((short) 3, list3.get(2));

        short[] arr2 = { 1, 2, 3, 4, 5 };
        ShortList list4 = new ShortList(arr2, 3);
        assertEquals(3, list4.size());
        assertEquals((short) 1, list4.get(0));
        assertEquals((short) 2, list4.get(1));
        assertEquals((short) 3, list4.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> new ShortList(arr2, 10));

        assertThrows(NullPointerException.class, () -> new ShortList(null));
        assertThrows(NullPointerException.class, () -> new ShortList(null, 0));
    }

    @Test
    public void testStaticFactoryMethods() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(3, list1.size());
        assertEquals((short) 1, list1.get(0));
        assertEquals((short) 2, list1.get(1));
        assertEquals((short) 3, list1.get(2));

        ShortList list2 = ShortList.of((short[]) null);
        assertEquals(0, list2.size());

        short[] arr = { 1, 2, 3, 4 };
        ShortList list3 = ShortList.of(arr, 2);
        assertEquals(2, list3.size());
        assertEquals((short) 1, list3.get(0));
        assertEquals((short) 2, list3.get(1));

        ShortList list4 = ShortList.copyOf(arr);
        assertEquals(4, list4.size());
        arr[0] = 10;
        assertEquals((short) 1, list4.get(0));

        ShortList list5 = ShortList.copyOf(arr, 1, 3);
        assertEquals(2, list5.size());
        assertEquals((short) 2, list5.get(0));
        assertEquals((short) 3, list5.get(1));

        ShortList list6 = ShortList.repeat((short) 5, 4);
        assertEquals(4, list6.size());
        for (int i = 0; i < 4; i++) {
            assertEquals((short) 5, list6.get(i));
        }

        ShortList list7 = ShortList.random(10);
        assertEquals(10, list7.size());
        for (int i = 0; i < 10; i++) {
            short value = list7.get(i);
            assertTrue(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
        }
    }

    @Test
    public void testRangeFactoryMethods() {
        ShortList list1 = ShortList.range((short) 0, (short) 5);
        assertEquals(5, list1.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((short) i, list1.get(i));
        }

        ShortList list2 = ShortList.range((short) 0, (short) 10, (short) 2);
        assertEquals(5, list2.size());
        assertEquals((short) 0, list2.get(0));
        assertEquals((short) 2, list2.get(1));
        assertEquals((short) 4, list2.get(2));
        assertEquals((short) 6, list2.get(3));
        assertEquals((short) 8, list2.get(4));

        ShortList list3 = ShortList.rangeClosed((short) 1, (short) 5);
        assertEquals(5, list3.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((short) (i + 1), list3.get(i));
        }

        ShortList list4 = ShortList.rangeClosed((short) 1, (short) 9, (short) 2);
        assertEquals(5, list4.size());
        assertEquals((short) 1, list4.get(0));
        assertEquals((short) 3, list4.get(1));
        assertEquals((short) 5, list4.get(2));
        assertEquals((short) 7, list4.get(3));
        assertEquals((short) 9, list4.get(4));

        ShortList list5 = ShortList.range((short) -5, (short) 0);
        assertEquals(5, list5.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((short) (-5 + i), list5.get(i));
        }

        ShortList list6 = ShortList.range((short) 1000, (short) 1005);
        assertEquals(5, list6.size());
        assertEquals((short) 1000, list6.get(0));
        assertEquals((short) 1004, list6.get(4));
    }

    @Test
    public void testGetAndSet() {
        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        assertEquals((short) 10, list.get(0));
        assertEquals((short) 20, list.get(1));
        assertEquals((short) 30, list.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));

        short oldValue = list.set(1, (short) 25);
        assertEquals((short) 20, oldValue);
        assertEquals((short) 25, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, (short) 40));
    }

    @Test
    public void testAddAll() {
        ShortList other = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(list.addAll(other));
        assertEquals(3, list.size());

        assertFalse(list.addAll(new ShortList()));
        assertEquals(3, list.size());

        ShortList other2 = ShortList.of((short) 10, (short) 20);
        assertTrue(list.addAll(1, other2));
        assertEquals(5, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 2, list.get(3));
        assertEquals((short) 3, list.get(4));

        short[] arr = { 50, 60 };
        assertTrue(list.addAll(arr));
        assertEquals(7, list.size());

        short[] arr2 = { -1, -2 };
        assertTrue(list.addAll(0, arr2));
        assertEquals(9, list.size());
        assertEquals((short) -1, list.get(0));
        assertEquals((short) -2, list.get(1));
    }

    @Test
    public void testRemove() {
        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 10);
        list.add((short) 30);

        assertTrue(list.remove((short) 20));
        assertEquals(3, list.size());
        assertEquals((short) 10, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 30, list.get(2));

        assertFalse(list.remove((short) 40));
        assertEquals(3, list.size());

        assertTrue(list.removeAllOccurrences((short) 10));
        assertEquals(1, list.size());
        assertEquals((short) 30, list.get(0));

        assertFalse(list.removeAllOccurrences((short) 10));
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 2);

        ShortList toRemove = ShortList.of((short) 2);
        assertTrue(list.removeAll(toRemove));
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        assertTrue(list.removeAll(new short[] { 1, 3 }));
        assertEquals(1, list.size());
        assertEquals((short) 2, list.get(0));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        ShortList toRetain = ShortList.of((short) 1, (short) 3);
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 3, list.get(1));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        assertTrue(list.retainAll(new short[] { 2 }));
        assertEquals(1, list.size());
        assertEquals((short) 2, list.get(0));
    }

    @Test
    public void testReplaceRange() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 4);

        ShortList replacement = ShortList.of((short) 10, (short) 20, (short) 30);
        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 30, list.get(3));
        assertEquals((short) 4, list.get(4));

        list.clear();
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.replaceRange(1, 2, new short[] { 100, 101 });
        assertEquals(4, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 100, list.get(1));
        assertEquals((short) 101, list.get(2));
        assertEquals((short) 3, list.get(3));

        list.replaceRange(1, 3, new short[0]);
        assertEquals(2, list.size());
    }

    @Test
    public void testReplaceAll() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 1);
        list.add((short) 3);

        assertEquals(2, list.replaceAll((short) 1, (short) 10));
        assertEquals(4, list.size());
        assertEquals((short) 10, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 10, list.get(2));
        assertEquals((short) 3, list.get(3));

        list.replaceAll(s -> (short) (s * 2));
        assertEquals((short) 20, list.get(0));
        assertEquals((short) 4, list.get(1));
        assertEquals((short) 20, list.get(2));
        assertEquals((short) 6, list.get(3));

        assertTrue(list.replaceIf(s -> s > 10, (short) 0));
        assertEquals((short) 0, list.get(0));
        assertEquals((short) 4, list.get(1));
        assertEquals((short) 0, list.get(2));
        assertEquals((short) 6, list.get(3));

        assertFalse(list.replaceIf(s -> s > 100, (short) 1));
    }

    @Test
    public void testContainsAnyAndAll() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        assertTrue(list.containsAny(ShortList.of((short) 1)));
        assertTrue(list.containsAny(ShortList.of((short) 4, (short) 3)));
        assertFalse(list.containsAny(ShortList.of((short) 4, (short) 5)));
        assertFalse(list.containsAny(new ShortList()));

        assertTrue(list.containsAny(new short[] { 1 }));
        assertTrue(list.containsAny(new short[] { 4, 2 }));
        assertFalse(list.containsAny(new short[] { 4, 5 }));

        assertTrue(list.containsAll(ShortList.of((short) 1, (short) 2)));
        assertTrue(list.containsAll(ShortList.of((short) 3)));
        assertFalse(list.containsAll(ShortList.of((short) 1, (short) 4)));
        assertTrue(list.containsAll(new ShortList()));

        assertTrue(list.containsAll(new short[] { 1, 2 }));
        assertFalse(list.containsAll(new short[] { 1, 4 }));
        assertTrue(list.containsAll(new short[0]));

        list.clear();
        assertFalse(list.containsAny(ShortList.of((short) 1)));
        assertFalse(list.containsAll(ShortList.of((short) 1)));
        assertTrue(list.containsAll(new ShortList()));
    }

    @Test
    public void testDisjoint() {
        list.add((short) 1);
        list.add((short) 2);

        assertFalse(list.disjoint(ShortList.of((short) 1)));
        assertFalse(list.disjoint(ShortList.of((short) 2, (short) 3)));
        assertTrue(list.disjoint(ShortList.of((short) 3, (short) 4)));
        assertTrue(list.disjoint(new ShortList()));

        assertFalse(list.disjoint(new short[] { 1 }));
        assertTrue(list.disjoint(new short[] { 3, 4 }));
        assertTrue(list.disjoint(new short[0]));

        list.clear();
        assertTrue(list.disjoint(ShortList.of((short) 1)));
        assertTrue(list.disjoint(new short[] { 2 }));
    }

    @Test
    public void testIntersection() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 2);

        ShortList other = ShortList.of((short) 2, (short) 3, (short) 4, (short) 2);
        ShortList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals((short) 2, result.get(0));
        assertEquals((short) 3, result.get(1));
        assertEquals((short) 2, result.get(2));

        result = list.intersection(new short[] { 1, 1, 4 });
        assertEquals(1, result.size());
        assertEquals((short) 1, result.get(0));

        result = list.intersection(new ShortList());
        assertEquals(0, result.size());
    }

    @Test
    public void testDifference() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);
        list.add((short) 2);

        ShortList other = ShortList.of((short) 2, (short) 2);
        ShortList result = list.difference(other);
        assertEquals(2, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 3, result.get(1));

        result = list.difference(new short[] { 1, 3 });
        assertEquals(2, result.size());
        assertEquals((short) 2, result.get(0));
        assertEquals((short) 2, result.get(1));

        result = list.difference(new ShortList());
        assertEquals(4, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        list.add((short) 1);
        list.add((short) 2);
        list.add((short) 3);

        ShortList other = ShortList.of((short) 2, (short) 3, (short) 4);
        ShortList result = list.symmetricDifference(other);
        assertEquals(2, result.size());
        assertEquals((short) 1, result.get(0));
        assertEquals((short) 4, result.get(1));

        result = list.symmetricDifference(new short[] { 1, 2, 3, 4, 5 });
        assertEquals(2, result.size());
        assertEquals((short) 4, result.get(0));
        assertEquals((short) 5, result.get(1));

        result = list.symmetricDifference(new ShortList());
        assertEquals(3, result.size());
    }

    @Test
    public void testMinMaxMedian() {
        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.median().isPresent());

        list.add((short) 5);
        assertEquals(OptionalShort.of((short) 5), list.min());
        assertEquals(OptionalShort.of((short) 5), list.max());
        assertEquals(OptionalShort.of((short) 5), list.median());

        list.clear();
        list.add((short) 3);
        list.add((short) 1);
        list.add((short) 4);
        list.add((short) 1);
        list.add((short) 5);

        assertEquals(OptionalShort.of((short) 1), list.min());
        assertEquals(OptionalShort.of((short) 5), list.max());
        assertEquals(OptionalShort.of((short) 3), list.median());

        assertEquals(OptionalShort.of((short) 1), list.min(1, 4));
        assertEquals(OptionalShort.of((short) 4), list.max(1, 4));
        assertEquals(OptionalShort.of((short) 1), list.median(1, 4));

        assertFalse(list.min(2, 2).isPresent());
        assertFalse(list.max(2, 2).isPresent());
        assertFalse(list.median(2, 2).isPresent());
    }

    @Test
    public void testFirstAndLast() {
        OptionalShort first = list.first();
        OptionalShort last = list.last();
        assertFalse(first.isPresent());
        assertFalse(last.isPresent());

        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        first = list.first();
        last = list.last();
        assertTrue(first.isPresent());
        assertTrue(last.isPresent());
        assertEquals((short) 10, first.get());
        assertEquals((short) 30, last.get());
    }

    @Test
    public void testGetFirstAndGetLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        assertEquals((short) 10, list.getFirst());
        assertEquals((short) 30, list.getLast());
    }

    @Test
    public void testAddFirstAndAddLast() {
        list.add((short) 10);

        list.addFirst((short) 5);
        assertEquals(2, list.size());
        assertEquals((short) 5, list.get(0));
        assertEquals((short) 10, list.get(1));

        list.addLast((short) 15);
        assertEquals(3, list.size());
        assertEquals((short) 5, list.get(0));
        assertEquals((short) 10, list.get(1));
        assertEquals((short) 15, list.get(2));
    }

    @Test
    public void testRemoveFirstAndRemoveLast() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
        assertThrows(NoSuchElementException.class, () -> list.removeLast());

        list.add((short) 10);
        list.add((short) 20);
        list.add((short) 30);

        assertEquals((short) 10, list.removeFirst());
        assertEquals(2, list.size());
        assertEquals((short) 20, list.get(0));
        assertEquals((short) 30, list.get(1));

        assertEquals((short) 30, list.removeLast());
        assertEquals(1, list.size());
        assertEquals((short) 20, list.get(0));
    }

    // --- Missing dedicated tests ---

    @Test
    public void testContainsDuplicates() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 1);
        assertTrue(list1.containsDuplicates());

        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertFalse(list2.containsDuplicates());

        assertFalse(new ShortList().containsDuplicates());
    }

    @Test
    public void testLastIndexOfFromIndex() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30, (short) 20);
        assertEquals(1, list.lastIndexOf((short) 20, 2));
        assertEquals(-1, list.lastIndexOf((short) 30, 1));
    }

    @Test
    public void testReplaceIfNoneMatch() {
        list.addAll(new short[] { 1, 2, 3 });
        assertFalse(list.replaceIf(v -> v > 100, (short) 0));
        assertEquals(3, list.size());
    }

    @Test
    public void testSwapOutOfBounds() {
        list.addAll(new short[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopyEmptyList() {
        ShortList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testRotateNegative() {
        list.addAll(new short[] { 1, 2, 3, 4, 5 });
        list.rotate(-2);
        assertEquals((short) 3, list.get(0));
        assertEquals((short) 4, list.get(1));
        assertEquals((short) 5, list.get(2));
    }

    @Test
    public void testReverseSingleElement() {
        list.add((short) 42);
        list.reverse();
        assertEquals(1, list.size());
        assertEquals((short) 42, list.get(0));
    }

    @Test
    public void testSortSingleElement() {
        list.add((short) 42);
        list.sort();
        assertEquals(1, list.size());
        assertEquals((short) 42, list.get(0));
    }

    @Test
    public void testSplitEmptyList() {
        List<ShortList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitUnevenChunks() {
        list.addAll(new short[] { 1, 2, 3, 4, 5 });
        List<ShortList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testBoxedEmptyList() {
        List<Short> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testIteratorEmptyList() {
        ShortIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStreamEmptyList() {
        assertEquals(0, list.stream().count());
    }

}
