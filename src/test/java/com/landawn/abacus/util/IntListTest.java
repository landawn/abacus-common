package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

public class IntListTest extends IntListTestSupport {

    @Test
    public void testSymmetricDifferenceIncludesFirstOperandPrefix() {
        final IntList first = IntList.of(1, 9);
        final IntList second = IntList.of(1, 2, 1);
        final int[] expected = { 9, 1, 2 };

        assertArrayEquals(expected, first.symmetricDifference(second).toArray());
        assertArrayEquals(expected, first.symmetricDifference(second.toArray()).toArray());
    }

    @Test
    public void testSortingAndSearchingClassExample() {
        final IntList numbers = IntList.of(1, 2, 3, 4, 5);
        numbers.add(42);
        numbers.set(1, 100);

        numbers.sort();
        assertEquals(4, numbers.binarySearch(42));

        numbers.reverseSort();
        assertArrayEquals(new int[] { 100, 42, 5, 4, 3, 1 }, numbers.toArray());
    }

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> list.forEach(0, 0, (java.util.function.IntConsumer) null));
    }

    @Test
    public void testConstructors() {
        {
            list = new IntList();
            int[] a = { 1, 2, 3, 4, 5 };
            IntList list = new IntList(a, 3);
            assertEquals(3, list.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
            assertThrows(IndexOutOfBoundsException.class, () -> new IntList(a, 6));
        }
        {
            list = new IntList();
            int[] a = { 1, 2, 3 };
            IntList list = new IntList(a);
            assertEquals(3, list.size());
            assertArrayEquals(a, list.toArray());
        }
        {
            list = new IntList();
            IntList list = new IntList(10);
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
            assertEquals(10, list.internalArray().length);
        }
        {
            list = new IntList();
            IntList list = new IntList();
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new IntList(-1));
    }

    @Test
    public void testConstructors_Null() {
        {
            list = new IntList();
            assertThrows(IllegalArgumentException.class, () -> new IntList((int[]) null));
        }
        {
            list = new IntList();
            assertThrows(IllegalArgumentException.class, () -> new IntList(null, 0));
        }
        {
            list = new IntList();
            assertThrows(IllegalArgumentException.class, () -> new IntList(null));
        }
    }

    @Test
    public void testConstructors_InvalidSize() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(arr, 5));
        assertThrows(IllegalArgumentException.class, () -> new IntList(arr, -1));
    }

    @Test
    public void testArray() {
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            int[] array = list.internalArray();

            array[1] = 20;
            assertEquals(20, list.get(1));

            list.clear();
            int[] newArray = list.internalArray();
            assertSame(array, newArray);
        }
        {
            list = new IntList();
            int[] a = { 1, 2, 3 };
            IntList list = new IntList(a);
            assertSame(a, list.internalArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            int[] arr = list.internalArray();
            assertNotNull(arr);
            assertTrue(arr.length >= 3);
        }
    }

    @Test
    public void testOf() {
        {
            list = new IntList();
            int[] a = { 1, 2, 3, 4, 5 };
            IntList list = IntList.of(a, 3);
            assertEquals(3, list.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
            assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(a, 6));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertEquals(3, list.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
            IntList emptyList = IntList.of();
            assertEquals(0, emptyList.size());
        }
    }

    @Test
    public void testOf_Empty() {
        IntList list = IntList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOf_Null() {
        IntList list = IntList.of((int[]) null);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOf_InvalidSize() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(arr, 5));
        assertThrows(IllegalArgumentException.class, () -> IntList.of(arr, -1));
    }

    @Test
    public void testRemoveAt() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            int deleted = list.removeAt(1);
            assertEquals(2, deleted);
            assertArrayEquals(new int[] { 1, 3 }, list.toArray());
            assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(0, 1, 2, 3, 4, 5);
            list.removeAllAt(1, 3, 5);
            assertArrayEquals(new int[] { 0, 2, 4 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });

            assertEquals(1, list.removeAt(0));
            assertEquals(4, list.size());
            assertEquals(2, list.get(0));

            assertEquals(5, list.removeAt(list.size() - 1));
            assertEquals(3, list.size());
            assertEquals(4, list.get(list.size() - 1));
        }
    }

    @Test
    public void testRemoveAt_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(3));
    }

    @Test
    public void testRemoveAt_Empty() {
        list.removeAllAt();
        assertTrue(list.isEmpty());

        list.addAll(new int[] { 1, 2, 3 });
        list.removeAllAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveRange() {
        {
            list = new IntList();
            IntList list = IntList.of(0, 1, 2, 3, 4, 5);
            list.removeRange(1, 4);
            assertArrayEquals(new int[] { 0, 4, 5 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            list.removeRange(0, 3);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testRemoveRange_Empty() {
        list.addAll(new int[] { 1, 2, 3 });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testFrequency() {
        IntList list = IntList.of(1, 2, 3, 2, 4, 2);
        assertEquals(3, list.frequency(2));
        assertEquals(1, list.frequency(1));
        assertEquals(0, list.frequency(42));
    }

    @Test
    public void testFrequency_Empty() {
        IntList list = new IntList();
        assertEquals(0, list.frequency(1));
    }

    @Test
    public void testContainsDuplicates() {
        IntList listWithDuplicates = IntList.of(1, 2, 1, 3);
        assertTrue(listWithDuplicates.containsDuplicates());
        IntList listWithoutDuplicates = IntList.of(1, 2, 3, 4);
        assertFalse(listWithoutDuplicates.containsDuplicates());
    }

    @Test
    public void testRange() {
        {
            list = new IntList();
            IntList list = IntList.rangeClosed(1, 10, 2);
            assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.range(1, 10, 2);
            assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.rangeClosed(1, 5);
            assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.range(1, 5);
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
        }
    }

    @Test
    public void testRange_Empty() {
        IntList list = IntList.range(5, 5);
        assertEquals(0, list.size());
    }

    @Test
    public void testRange_NegativeStep() {
        {
            list = new IntList();
            IntList list = IntList.rangeClosed(10, 0, -2);
            assertEquals(6, list.size());
            assertEquals(10, list.get(0));
            assertEquals(0, list.get(5));
        }
        {
            list = new IntList();
            IntList list = IntList.range(10, 0, -2);
            assertEquals(5, list.size());
            assertEquals(10, list.get(0));
            assertEquals(2, list.get(4));
        }
    }

    @Test
    public void testRange_ZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> IntList.range(0, 10, 0));
    }

    @Test
    public void testBatch() {
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3, 4, 5);
            IntList b = IntList.of(2, 4);
            assertTrue(a.removeAll(b));
            assertEquals(3, a.size());
            assertEquals(1, a.get(0));
            assertEquals(3, a.get(1));
            assertEquals(5, a.get(2));
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3, 4, 5);
            IntList b = IntList.of(2, 4);
            assertTrue(a.retainAll(b));
            assertEquals(2, a.size());
            assertEquals(2, a.get(0));
            assertEquals(4, a.get(1));
        }
    }

    @Test
    public void testBatch_LargeData() {
        {
            list = new IntList();
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
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1);
            IntList b = IntList.of(1, 1, 1, 1);
            assertTrue(a.removeAll(b));
            assertEquals(5, a.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals(2, a.get(i));
            }
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 1, 2, 1, 2, 1, 2, 1, 2, 1);
            IntList b = IntList.of(1, 1, 1, 1);
            assertTrue(a.retainAll(b));
            assertEquals(6, a.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals(1, a.get(i));
            }
        }
    }

    @Test
    public void testRepeat() {
        IntList list = IntList.repeat(5, 3);
        assertArrayEquals(new int[] { 5, 5, 5 }, list.toArray());
    }

    @Test
    public void testRandom() {
        {
            list = new IntList();
            IntList list = IntList.random(1, 10, 5);
            assertEquals(5, list.size());
            for (int i = 0; i < list.size(); i++) {
                assertTrue(list.get(i) >= 1 && list.get(i) < 10);
            }
            assertThrows(IllegalArgumentException.class, () -> IntList.random(10, 1, 5));
        }
        {
            list = new IntList();
            IntList list = IntList.random(10);
            assertEquals(10, list.size());
        }
    }

    @Test
    public void testRandom_LargeData() {
        // The interval width exceeds Integer.MAX_VALUE and exercises the bounded-long path.
        IntList list = IntList.random(Integer.MIN_VALUE, Integer.MAX_VALUE, 100);
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            int value = list.get(i);
            assertTrue(value >= Integer.MIN_VALUE && value < Integer.MAX_VALUE);
        }
    }

    @Test
    public void testRandom_InvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> IntList.random(5, 5, 10));
        assertThrows(IllegalArgumentException.class, () -> IntList.random(10, 5, 10));
    }

    @Test
    public void testGetSet() {
        IntList list = IntList.of(1, 2, 3);
        int oldVal = list.set(1, 99);
        assertEquals(2, oldVal);
        assertArrayEquals(new int[] { 1, 99, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 100));
    }

    @Test
    public void testGetSet_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 42));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 42));
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
    public void testNegative() {
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

    @Test
    public void testRetainAll() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 2, 4);
            int[] toRetain = { 2, 4, 5 };
            assertTrue(list.retainAll(toRetain));
            assertArrayEquals(new int[] { 2, 2, 4 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3, 4, 5);
            IntList list2 = IntList.of(2, 4, 6);
            assertTrue(list1.retainAll(list2));
            assertEquals(2, list1.size());
            assertEquals(2, list1.get(0));
            assertEquals(4, list1.get(1));
        }
    }

    @Test
    public void testRetainAll_Empty() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = new IntList();
            assertTrue(list1.retainAll(list2));
            assertEquals(0, list1.size());
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3);
            int[] b = {};
            assertTrue(a.retainAll(b));
            assertEquals(0, a.size());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertTrue(list.retainAll(new IntList()));
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testMoveRange() {
        {
            list = new IntList();
            IntList list = IntList.of(0, 1, 2, 3, 4, 5);
            list.moveRange(1, 3, 4);
            assertArrayEquals(new int[] { 0, 3, 4, 5, 1, 2 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });
            list.moveRange(0, 2, 3);
            assertEquals(5, list.size());
            assertEquals(3, list.get(0));
            assertEquals(4, list.get(1));
            assertEquals(5, list.get(2));
            assertEquals(1, list.get(3));
            assertEquals(2, list.get(4));
        }
    }

    @Test
    public void testInteger_Overflow() {
        list.add(Integer.MAX_VALUE);
        list.add(Integer.MAX_VALUE);

        list.replaceAll(x -> x + 1);
        assertEquals(Integer.MIN_VALUE, list.get(0));
        assertEquals(Integer.MIN_VALUE, list.get(1));
    }

    @Test
    public void testFill() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            list.fill(1, 4, 99);
            assertArrayEquals(new int[] { 1, 99, 99, 99, 5 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4);
            list.fill(99);
            assertArrayEquals(new int[] { 99, 99, 99, 99 }, list.toArray());
        }
    }

    @Test
    public void testFill_Empty() {
        list.fill(10);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFill_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(-1, 2, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(0, 4, 99));
    }

    @Test
    public void testFill_InvalidRange() {
        list.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10));
    }

    @Test
    public void testDisjoint() {
        {
            list = new IntList();
            // needToSet: min(lenA, lenB) > 3 && max(lenA, lenB) > 9
            IntList large = new IntList();
            for (int i = 0; i < 15; i++) {
                large.add(i);
            }
            IntList query = IntList.of(0, 1, 2, 3, 4);
            assertFalse(large.disjoint(query));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            int[] arr = { 3, 4, 5 };
            assertFalse(list.disjoint(arr));
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3);
            assertTrue(a.disjoint(new int[] { 99, 100 }));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertFalse(list.disjoint(list));
        }
    }

    @Test
    public void testDisjoint_LargeData() {
        IntList a = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
        IntList b = IntList.of(5, 6, 7, 8);
        assertFalse(a.disjoint(b));
    }

    @Test
    public void testIntersection() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 2, 3);
            IntList list2 = IntList.of(2, 3, 4, 2);
            IntList intersection = list1.intersection(list2);
            assertArrayEquals(new int[] { 2, 2, 3 }, intersection.toArray());
        }
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 2, 3, 4);
            IntList list2 = IntList.of(2, 2, 3, 5);
            IntList result = list1.intersection(list2);
            assertEquals(3, result.size());
            assertEquals(2, result.get(0));
            assertEquals(2, result.get(1));
            assertEquals(3, result.get(2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 2, 3, 4);
            int[] arr = { 2, 2, 3, 5 };
            IntList result = list.intersection(arr);
            assertEquals(3, result.size());
            assertEquals(2, result.get(0));
            assertEquals(2, result.get(1));
            assertEquals(3, result.get(2));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            IntList result = list.intersection(IntList.of(4, 5, 6));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testIntersection_Empty() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = new IntList();
            IntList result = list1.intersection(list2);
            assertEquals(0, result.size());
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3);
            int[] b = {};
            IntList result = a.intersection(b);
            assertEquals(0, result.size());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            IntList result = list.intersection(new IntList());
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testDifference() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 2, 3, 4);
            IntList list2 = IntList.of(2, 3, 5);
            IntList difference = list1.difference(list2);
            assertArrayEquals(new int[] { 1, 2, 4 }, difference.toArray());
        }
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 2, 3, 4);
            IntList list2 = IntList.of(2, 3, 5);
            IntList result = list1.difference(list2);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0));
            assertEquals(2, result.get(1));
            assertEquals(4, result.get(2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 2, 3, 4);
            int[] arr = { 2, 3, 5 };
            IntList result = list.difference(arr);
            assertEquals(3, result.size());
            assertEquals(1, result.get(0));
            assertEquals(2, result.get(1));
            assertEquals(4, result.get(2));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            IntList result = list.difference(IntList.of(1, 2, 3));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testDifference_Empty() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = new IntList();
            IntList result = list1.difference(list2);
            assertEquals(3, result.size());
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3);
            int[] b = {};
            IntList result = a.difference(b);
            assertEquals(3, result.size());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            IntList result = list.difference(new IntList());
            assertEquals(3, result.size());
            assertEquals(1, result.get(0));
            assertEquals(2, result.get(1));
            assertEquals(3, result.get(2));
        }
    }

    @Test
    public void testSymmetricDifference() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 2, 3);
            IntList list2 = IntList.of(2, 3, 4);
            IntList symmetricDifference = list1.symmetricDifference(list2);
            symmetricDifference.sort();
            assertArrayEquals(new int[] { 1, 2, 4 }, symmetricDifference.toArray());
        }
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 2, 3);
            IntList list2 = IntList.of(2, 3, 4, 5);
            IntList result = list1.symmetricDifference(list2);
            assertTrue(result.size() > 0);
            assertTrue(result.contains(1));
            assertTrue(result.contains(4));
            assertTrue(result.contains(5));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 2, 3);
            int[] arr = { 2, 3, 4, 5 };
            IntList result = list.symmetricDifference(arr);
            assertTrue(result.size() > 0);
            assertTrue(result.contains(1));
            assertTrue(result.contains(4));
            assertTrue(result.contains(5));
        }
    }

    @Test
    public void testSymmetricDifference_Empty() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = new IntList();
            IntList result = list1.symmetricDifference(list2);
            assertEquals(3, result.size());
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3);
            int[] b = { 2, 3, 4 };
            IntList result = a.symmetricDifference(b);
            assertEquals(2, result.size());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            IntList result = list.symmetricDifference(new IntList());
            assertEquals(3, result.size());
        }
    }

    @Test
    public void testIndexOf() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 2, 4);
            assertEquals(3, list.indexOf(2, 2));
            assertEquals(-1, list.indexOf(1, 2));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertEquals(0, list.indexOf(1, -1));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 2, 4);
            assertEquals(1, list.indexOf(2));
            assertEquals(0, list.indexOf(1));
            assertEquals(-1, list.indexOf(42));
        }
    }

    @Test
    public void testIndexOf_OutOfBounds() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertEquals(-1, list.indexOf(1, 10));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertEquals(-1, list.indexOf(1, 10));
        }
    }

    @Test
    public void testLastIndexOf() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 2, 4);
            assertEquals(1, list.lastIndexOf(2, 2));
            assertEquals(-1, list.lastIndexOf(4, 2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 2, 4);
            assertEquals(3, list.lastIndexOf(2));
            assertEquals(0, list.lastIndexOf(1));
            assertEquals(-1, list.lastIndexOf(42));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertEquals(-1, list.lastIndexOf(1, -1));
        }
    }

    @Test
    public void testLastIndexOf_OutOfBounds() {
        IntList a = IntList.of(1, 2, 3, 2, 1);
        assertEquals(4, a.lastIndexOf(1, 100));
        assertEquals(3, a.lastIndexOf(2, 100));
    }

    @Test
    public void testLastIndexOf_Empty() {
        assertEquals(-1, list.lastIndexOf(1));
    }

    @Test
    public void testMin() {
        {
            list = new IntList();
            list.add(5);

            OptionalInt min = list.min();
            assertTrue(min.isPresent());
            assertEquals(5, min.getAsInt());

            OptionalInt max = list.max();
            assertTrue(max.isPresent());
            assertEquals(5, max.getAsInt());

            OptionalInt median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals(5, median.getAsInt());
        }
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9);
            OptionalInt min = list.min(2, 5);
            assertTrue(min.isPresent());
            assertEquals(1, min.get());
        }
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9);
            assertEquals(1, list.min().getAsInt());
            assertTrue(new IntList().min().isEmpty());
        }
    }

    @Test
    public void testMin_Empty() {
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });

            assertFalse(list.min(1, 1).isPresent());
            assertFalse(list.max(1, 1).isPresent());
            assertFalse(list.lowerMedian(1, 1).isPresent());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            OptionalInt min = list.min(1, 1);
            assertFalse(min.isPresent());
        }
        {
            list = new IntList();
            IntList list = new IntList();
            OptionalInt min = list.min();
            assertFalse(min.isPresent());
        }
    }

    @Test
    public void testMax() {
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9);
            OptionalInt max = list.max(0, 3);
            assertTrue(max.isPresent());
            assertEquals(4, max.get());
        }
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9);
            OptionalInt max = list.max();
            assertTrue(max.isPresent());
            assertEquals(9, max.get());
        }
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9);
            assertEquals(9, list.max().getAsInt());
        }
    }

    @Test
    public void testMax_Empty() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            OptionalInt max = list.max(1, 1);
            assertFalse(max.isPresent());
        }
        {
            list = new IntList();
            IntList list = new IntList();
            OptionalInt max = list.max();
            assertFalse(max.isPresent());
        }
    }

    @Test
    public void testMedian() {
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4 });
            OptionalInt median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals(2, median.getAsInt());
        }
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5);
            OptionalInt median = list.lowerMedian(0, 3);
            assertTrue(median.isPresent());
        }
    }

    @Test
    public void testMedian_Empty() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            OptionalInt median = list.lowerMedian(1, 1);
            assertFalse(median.isPresent());
        }
        {
            list = new IntList();
            IntList list = new IntList();
            OptionalInt median = list.lowerMedian();
            assertFalse(median.isPresent());
        }
    }

    @Test
    public void testEach() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntList result = new IntList();
            list.forEach(1, 4, result::add);
            assertEquals(3, result.size());
            assertEquals(2, result.get(0));
            assertEquals(4, result.get(2));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });
            List<Integer> result = new ArrayList<>();

            list.forEach(4, 1, result::add);

            assertEquals(3, result.size());
            assertEquals(5, result.get(0));
            assertEquals(4, result.get(1));
            assertEquals(3, result.get(2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntList result = new IntList();
            list.forEach(result::add);
            assertEquals(5, result.size());
            assertEquals(1, result.get(0));
            assertEquals(5, result.get(4));
        }
    }

    @Test
    public void testEach_Empty() {
        List<Integer> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEach_Null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> list.forEach(null));
    }

    @Test
    public void testFirst() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertEquals(1, list.first().getAsInt());
            assertTrue(new IntList().first().isEmpty());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            OptionalInt first = list.first();
            assertTrue(first.isPresent());
            assertEquals(1, first.get());
        }
    }

    @Test
    public void testFirst_Empty() {
        IntList list = new IntList();
        OptionalInt first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertEquals(3, list.last().getAsInt());
            assertTrue(new IntList().last().isEmpty());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            OptionalInt last = list.last();
            assertTrue(last.isPresent());
            assertEquals(3, last.get());
        }
    }

    @Test
    public void testLast_Empty() {
        IntList list = new IntList();
        OptionalInt last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 1, 3, 2, 4);
            IntList distinctList = list.distinct();
            distinctList.sort();
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, distinctList.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            IntList distinct = list.distinct();
            assertEquals(3, distinct.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, distinct.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 1, 3, 2);
            IntList distinct = list.distinct();
            assertEquals(3, distinct.size());
            assertTrue(distinct.contains(1));
            assertTrue(distinct.contains(2));
            assertTrue(distinct.contains(3));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 2, 3, 3, 3, 4);
            IntList result = list.distinct(0, 7);
            assertEquals(4, result.size());
            assertEquals(1, result.get(0));
            assertEquals(2, result.get(1));
            assertEquals(3, result.get(2));
            assertEquals(4, result.get(3));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 2, 3 });
            IntList result = list.distinct(0, 1);
            assertEquals(1, result.size());
            assertEquals(1, result.get(0));
        }
    }

    @Test
    public void testDistinct_Empty() {
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 2, 3 });
            IntList result = list.distinct(1, 1);
            assertTrue(result.isEmpty());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            IntList result = list.distinct(1, 1);
            assertEquals(0, result.size());
        }
        {
            list = new IntList();
            IntList distinct = list.distinct();
            assertTrue(distinct.isEmpty());
        }
    }

    @Test
    public void testIsSorted() {
        {
            list = new IntList();
            IntList sortedList = IntList.of(1, 2, 3, 4);
            assertTrue(sortedList.isSorted());
            IntList unsortedList = IntList.of(1, 3, 2, 4);
            assertFalse(unsortedList.isSorted());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            assertTrue(list.isSorted());
        }
    }

    @Test
    public void testIsSorted_Empty() {
        IntList list = new IntList();
        assertTrue(list.isSorted());
    }

    @Test
    public void testSort() {
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9);
            list.sort();
            assertArrayEquals(new int[] { 1, 1, 3, 4, 5, 9 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9, 2, 6);
            list.sort();
            assertTrue(list.isSorted());
            assertEquals(1, list.get(0));
            assertEquals(9, list.get(7));
        }
        {
            list = new IntList();
            list.add(5);
            list.sort();
            assertEquals(1, list.size());
            assertEquals(5, list.get(0));
        }
    }

    @Test
    public void testParallelSort() {
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9);
            list.parallelSort();
            assertArrayEquals(new int[] { 1, 1, 3, 4, 5, 9 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(3, 1, 4, 1, 5, 9, 2, 6);
            list.parallelSort();
            assertTrue(list.isSorted());
            assertEquals(1, list.get(0));
            assertEquals(9, list.get(7));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 3, 1, 2 });
            list.parallelSort();
            assertTrue(list.isSorted());
        }
    }

    @Test
    public void testParallelSort_Empty() {
        IntList a = IntList.of(5, 3, 1, 4, 2);
        a.parallelSort();
        assertEquals(1, a.get(0));
        assertEquals(2, a.get(1));
        assertEquals(5, a.get(4));
    }

    @Test
    public void testReverseSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.reverseSort();
        assertArrayEquals(new int[] { 9, 5, 4, 3, 1, 1 }, list.toArray());
    }

    @Test
    public void testBinarySearch() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            int index = list.binarySearch(1, 4, 3);
            assertEquals(2, index);
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            assertEquals(2, list.binarySearch(3));
            assertTrue(list.binarySearch(6) < 0);
        }
        {
            list = new IntList();
            list.addAll(new int[] { 3, 1, 4, 1, 5 });
            int result = list.binarySearch(3);
            assertNotNull(result);
        }
    }

    @Test
    public void testReverse() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            list.reverse();
            assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            list.reverse(1, 4);
            assertEquals(1, list.get(0));
            assertEquals(4, list.get(1));
            assertEquals(3, list.get(2));
            assertEquals(2, list.get(3));
            assertEquals(5, list.get(4));
        }
        {
            list = new IntList();
            list.add(5);
            list.reverse();
            assertEquals(1, list.size());
            assertEquals(5, list.get(0));
        }
    }

    @Test
    public void testReverse_Empty() {
        list.addAll(new int[] { 1, 2, 3 });
        list.reverse(1, 1);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testRotate() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            list.rotate(2);
            assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });
            list.rotate(-2);
            assertEquals(3, list.get(0));
            assertEquals(4, list.get(1));
            assertEquals(5, list.get(2));
            assertEquals(1, list.get(3));
            assertEquals(2, list.get(4));
        }
        {
            list = new IntList();
            list.add(5);
            list.rotate(10);
            assertEquals(1, list.size());
            assertEquals(5, list.get(0));
        }
    }

    @Test
    public void testShuffle() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            IntList list2 = list1.copy();
            list1.shuffle();

            // A shuffle is a permutation, so the only guaranteed post-condition is that the same elements are
            // still there. Asserting that the ORDER changed is not a valid expectation: a uniform shuffle of 10
            // elements returns the original order once in 3,628,800 runs, which made this test fail intermittently.
            assertEquals(list2.size(), list1.size());
            assertTrue(N.isEqualCollection(list1.toList(), list2.toList()));

            // The "it really does reorder" half of the original intent, made deterministic with a seeded Random.
            final IntList seeded = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            seeded.shuffle(new Random(42));
            assertFalse(Arrays.equals(seeded.toArray(), list2.toArray()));
            assertTrue(N.isEqualCollection(seeded.toList(), list2.toList()));
        }
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
            IntList list2 = list1.copy();
            list1.shuffle(new Random(123));
            list2.shuffle(new Random(123));
            assertArrayEquals(list1.toArray(), list2.toArray());
        }
        {
            list = new IntList();
            list.add(5);
            list.shuffle();
            assertEquals(1, list.size());
            assertEquals(5, list.get(0));
        }
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new IntList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> IntList.of(1).shuffle(null));
    }

    @Test
    public void testShuffle_Empty() {
        IntList a = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Random rnd = new Random(42);
        a.shuffle(rnd);
        assertEquals(10, a.size());
    }

    @Test
    public void testSwap() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4);
            list.swap(1, 3);
            assertArrayEquals(new int[] { 1, 4, 3, 2 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            list.swap(1, 1);
            assertEquals(2, list.get(1));
        }
    }

    @Test
    public void testSwap_OutOfBounds() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(1, 3));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
        }
    }

    @Test
    public void testSplit() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7);
            List<IntList> chunks = list.split(3);
            assertEquals(3, chunks.size());
            assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0).toArray());
            assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1).toArray());
            assertArrayEquals(new int[] { 7 }, chunks.get(2).toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7, 8);
            List<IntList> chunks = list.split(0, 8, 3);
            assertEquals(3, chunks.size());
            assertEquals(3, chunks.get(0).size());
            assertEquals(3, chunks.get(1).size());
            assertEquals(2, chunks.get(2).size());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });
            List<IntList> chunks = list.split(0, 5, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(1, chunks.get(2).size());
        }
    }

    @Test
    public void testSplit_Empty() {
        {
            list = new IntList();
            List<IntList> chunks = list.split(0, 0, 2);
            assertTrue(chunks.isEmpty());
        }
        {
            list = new IntList();
            List<IntList> chunks = list.split(3);
            assertTrue(chunks.isEmpty());
        }
    }

    @Test
    public void testTrim() {
        {
            list = new IntList();
            IntList list = new IntList(100);
            list.add(1);
            list.add(2);
            list.add(3);
            IntList trimmed = list.trimToSize();
            assertEquals(3, trimmed.size());
            assertSame(list, trimmed);
        }
        {
            list = new IntList();
            IntList list = new IntList(10);
            list.add(1);
            list.add(2);
            list.add(3);
            list.trimToSize();
            assertEquals(3, list.internalArray().length);
        }
    }

    @Test
    public void testClear() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty_Empty() {
        IntList list = new IntList();
        assertTrue(list.isEmpty());

        list.add(1);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSize() {
        IntList list = new IntList();
        assertEquals(0, list.size());

        list.add(1);
        assertEquals(1, list.size());

        list.add(2);
        list.add(3);
        assertEquals(3, list.size());
    }

    @Test
    public void testBoxed() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            List<Integer> boxedList = list.boxed();
            assertEquals(3, boxedList.size());
            assertEquals(1, boxedList.get(0));
            assertEquals(2, boxedList.get(1));
            assertEquals(3, boxedList.get(2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            List<Integer> boxed = list.boxed(1, 4);
            assertEquals(3, boxed.size());
            assertEquals(Integer.valueOf(2), boxed.get(0));
            assertEquals(Integer.valueOf(4), boxed.get(2));
        }
    }

    @Test
    public void testBoxed_Empty() {
        IntList list = new IntList();
        List<Integer> boxed = list.boxed();
        assertEquals(0, boxed.size());
    }

    @Test
    public void testBoxed_InvalidRange() {
        list.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    @Test
    public void testIterator() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.nextInt());
    }

    @Test
    public void testIterator_Empty() {
        {
            list = new IntList();
            IntIterator iter = list.iterator();
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, () -> iter.nextInt());
        }
        {
            list = new IntList();
            IntList list = new IntList();
            IntIterator iter = list.iterator();
            assertFalse(iter.hasNext());
        }
    }

    @Test
    public void testStream() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntStream stream = list.stream(1, 4);
            assertEquals(3, stream.count());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntStream stream = list.stream();
            assertEquals(5, stream.count());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            long sum = list.stream().sum();
            assertEquals(15, sum);
        }
    }

    @Test
    public void testStream_Empty() {
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            IntStream stream = list.stream(1, 1);
            assertEquals(0, stream.count());
        }
        {
            list = new IntList();
            IntStream stream = list.stream();
            assertEquals(0, stream.count());
        }
    }

    @Test
    public void testStream_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.stream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.stream(0, 4));
    }

    @Test
    public void testGetFirst() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertEquals(1, list.getFirst());
            assertEquals(3, list.getLast());
            IntList emptyList = new IntList();
            assertThrows(NoSuchElementException.class, () -> emptyList.getFirst());
            assertThrows(NoSuchElementException.class, () -> emptyList.getLast());
        }
        {
            list = new IntList();
            list.add(5);
            assertEquals(5, list.getFirst());
            assertEquals(5, list.getLast());
        }
    }

    @Test
    public void testGetFirst_Empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(5, list.getLast());
    }

    @Test
    public void testGetLast_Empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
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
    public void testEquals_Null() {
        IntList list = IntList.of(1, 2, 3);
        assertFalse(list.equals(null));
    }

    @Test
    public void testEnsureCapacity() {
        {
            list = new IntList();
            // Trigger capacity growth by adding many elements
            IntList a = new IntList(2);
            for (int i = 0; i < 20; i++) {
                a.add(i);
            }
            assertEquals(20, a.size());
        }
        {
            list = new IntList();
            IntList a = new IntList();
            for (int i = 0; i < 20; i++) {
                a.add(i);
            }
            assertEquals(20, a.size());
        }
    }

    @Test
    public void testConversionSuppliersMustProduceCollectionsForEmptyRanges() {
        final IntList empty = new IntList();
        assertThrows(IllegalArgumentException.class, () -> empty.toCollection(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> empty.toCollection(0, 0, ignored -> null));
        assertThrows(IllegalArgumentException.class, () -> empty.toMultiset(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> empty.toMultiset(0, 0, ignored -> null));
    }

    @Test
    public void reviewFixes20260906_descendingCopyClampsAgainstSizeNotTheBackingArray() {
        // IntList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final IntList withSpareCapacity = IntList.of(new int[] { 1, 2, 3, 4, 5 }, 3);

        assertEquals(3, withSpareCapacity.size());
        assertEquals(5, withSpareCapacity.internalArray().length, "the test needs real spare capacity");

        assertEquals("[3, 2, 1]", withSpareCapacity.copy(3, -1, -1).toString(), "spare capacity must not leak into the result");
        assertEquals("[3, 2, 1]", withSpareCapacity.copy(2, -1, -1).toString(), "an in-range start is unaffected");

        // Ascending copies, and a copy over the whole logical range, are unchanged.
        assertEquals(withSpareCapacity.toString(), withSpareCapacity.copy(0, 3, 1).toString());
        assertEquals(3, withSpareCapacity.copy(0, 3, 1).size());

        // The source is not modified by any of this.
        assertEquals(3, withSpareCapacity.size());
    }

    @Test
    public void reviewFixes20260906_addAllAtIndexSurvivesSelfAliasing() {
        // Passing the list to itself makes source and destination the same array, across a reallocation by
        // ensureCapacity. It is correct because the tail shift runs BEFORE the source copy and the two regions
        // provably never overlap (the source is [0, numNew) and the shift writes at index + numNew or later).
        // Swap those two statements and the result is wrong, so this pins the ordering, at every index.
        for (int index = 0; index <= 3; index++) {
            final IntList self = IntList.of(1, 2, 3);
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final IntList expected = IntList.of(1, 2, 3);
            final IntList inserted = IntList.of(1, 2, 3);
            expected.addAll(index, IntList.of(1, 2, 3));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final IntList appended = IntList.of(1, 2, 3);
        appended.addAll(appended);
        assertEquals("[1, 2, 3, 1, 2, 3]", appended.toString());

        // The interesting middle case, spelled out.
        final IntList middle = IntList.of(1, 2, 3);
        middle.addAll(1, middle);
        assertEquals("[1, 1, 2, 3, 2, 3]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final IntList a = IntList.of(1, 2, 3);
        assertFalse(a.removeAll((IntList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((int[]) null));
        assertFalse(a.removeAll(new int[0]));
        assertFalse(a.removeAll(new IntList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final IntList b = IntList.of(1, 2, 3);
        assertTrue(b.retainAll((IntList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final IntList c = IntList.of(1, 2, 3);
        assertTrue(c.retainAll((int[]) null));
        assertEquals(0, c.size());

        final IntList d = IntList.of(1, 2, 3);
        assertTrue(d.retainAll(new int[0]));
        assertEquals(0, d.size());

        final IntList e = IntList.of(1, 2, 3);
        assertTrue(e.retainAll(new IntList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final IntList empty = new IntList();
        assertFalse(empty.retainAll((IntList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final IntList f = IntList.of(1, 2, 3);
        assertTrue(f.retainAll(IntList.of(1)));
        assertEquals(1, f.size());
    }
}
