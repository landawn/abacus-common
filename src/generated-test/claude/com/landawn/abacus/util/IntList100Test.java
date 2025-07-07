package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

public class IntList100Test extends TestBase {

    private IntList list;

    @BeforeEach
    public void setUp() {
        list = new IntList();
    }

    // Constructor Tests
    @Test
    public void testDefaultConstructor() {
        IntList list = new IntList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithCapacity() {
        IntList list = new IntList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntList list = new IntList(array);
        assertEquals(5, list.size());
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], list.get(i));
        }
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntList list = new IntList(array, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void testConstructorWithArrayAndSizeThrowsException() {
        int[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(array, 4));
    }

    // Static Factory Method Tests
    @Test
    public void testOf() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testOfWithEmptyArray() {
        IntList list = IntList.of();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithArrayAndSize() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntList list = IntList.of(array, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void testCopyOf() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(array);
        assertEquals(5, list.size());
        // Modify original array
        array[0] = 100;
        // List should not be affected
        assertEquals(1, list.get(0));
    }

    @Test
    public void testCopyOfRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(array, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2, list.get(0));
        assertEquals(4, list.get(2));
    }

    @Test
    public void testRange() {
        IntList list = IntList.range(0, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    public void testRangeWithStep() {
        IntList list = IntList.range(0, 10, 2);
        assertEquals(5, list.size());
        assertEquals(0, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(8, list.get(4));
    }

    @Test
    public void testRangeClosed() {
        IntList list = IntList.rangeClosed(1, 5);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testRangeClosedWithStep() {
        IntList list = IntList.rangeClosed(1, 9, 2);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(9, list.get(4));
    }

    @Test
    public void testRepeat() {
        IntList list = IntList.repeat(7, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(7, list.get(i));
        }
    }

    @Test
    public void testRandom() {
        IntList list = IntList.random(10);
        assertEquals(10, list.size());
        // Just check that it contains integers
        for (int i = 0; i < 10; i++) {
            assertNotNull(list.get(i));
        }
    }

    @Test
    public void testRandomWithRange() {
        IntList list = IntList.random(0, 10, 100);
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            assertTrue(list.get(i) >= 0 && list.get(i) < 10);
        }
    }

    // Get and Set Tests
    @Test
    public void testGet() {
        list.add(10);
        list.add(20);
        assertEquals(10, list.get(0));
        assertEquals(20, list.get(1));
    }

    @Test
    public void testGetThrowsException() {
        list.add(10);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testSet() {
        list.add(10);
        list.add(20);
        int oldValue = list.set(1, 30);
        assertEquals(20, oldValue);
        assertEquals(30, list.get(1));
    }

    @Test
    public void testSetThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10));
    }

    // Add Tests
    @Test
    public void testAdd() {
        list.add(10);
        assertEquals(1, list.size());
        assertEquals(10, list.get(0));

        list.add(20);
        assertEquals(2, list.size());
        assertEquals(20, list.get(1));
    }

    @Test
    public void testAddAtIndex() {
        list.add(10);
        list.add(30);
        list.add(1, 20);

        assertEquals(3, list.size());
        assertEquals(10, list.get(0));
        assertEquals(20, list.get(1));
        assertEquals(30, list.get(2));
    }

    @Test
    public void testAddAtIndexThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10));
    }

    @Test
    public void testAddAll() {
        list.add(1);
        list.add(2);

        IntList other = IntList.of(3, 4, 5);
        boolean result = list.addAll(other);

        assertTrue(result);
        assertEquals(5, list.size());
        assertEquals(3, list.get(2));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testAddAllEmpty() {
        list.add(1);
        IntList empty = new IntList();
        boolean result = list.addAll(empty);

        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAllAtIndex() {
        list.add(1);
        list.add(4);

        IntList other = IntList.of(2, 3);
        boolean result = list.addAll(1, other);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(4, list.get(3));
    }

    @Test
    public void testAddAllArray() {
        list.add(1);
        int[] array = { 2, 3, 4 };
        boolean result = list.addAll(array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(4, list.get(3));
    }

    @Test
    public void testAddAllArrayAtIndex() {
        list.add(1);
        list.add(4);
        int[] array = { 2, 3 };
        boolean result = list.addAll(1, array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    // Remove Tests
    @Test
    public void testRemove() {
        list.add(10);
        list.add(20);
        list.add(30);

        boolean result = list.remove(20);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(10, list.get(0));
        assertEquals(30, list.get(1));
    }

    @Test
    public void testRemoveNotFound() {
        list.add(10);
        boolean result = list.remove(20);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.add(10);
        list.add(20);
        list.add(10);
        list.add(30);
        list.add(10);

        boolean result = list.removeAllOccurrences(10);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(20, list.get(0));
        assertEquals(30, list.get(1));
    }

    @Test
    public void testRemoveAll() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList toRemove = IntList.of(2, 4);

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void testRemoveAllArray() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        int[] toRemove = { 2, 4 };

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        boolean result = list.removeIf(x -> x % 2 == 0);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(new int[] { 1, 2, 2, 3, 3, 3, 4 });

        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(4, list.get(3));
    }

    @Test
    public void testRetainAll() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList toRetain = IntList.of(2, 4, 6);

        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
        assertEquals(4, list.get(1));
    }

    @Test
    public void testDelete() {
        list.addAll(new int[] { 10, 20, 30 });

        int deleted = list.delete(1);
        assertEquals(20, deleted);
        assertEquals(2, list.size());
        assertEquals(10, list.get(0));
        assertEquals(30, list.get(1));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.addAll(new int[] { 10, 20, 30, 40, 50 });

        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(10, list.get(0));
        assertEquals(30, list.get(1));
        assertEquals(50, list.get(2));
    }

    @Test
    public void testDeleteRange() {
        list.addAll(new int[] { 10, 20, 30, 40, 50 });

        list.deleteRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(10, list.get(0));
        assertEquals(40, list.get(1));
        assertEquals(50, list.get(2));
    }

    // Move and Replace Tests
    @Test
    public void testMoveRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        list.moveRange(1, 3, 3);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(5, list.get(2));
        assertEquals(2, list.get(3));
        assertEquals(3, list.get(4));
    }

    @Test
    public void testReplaceRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList replacement = IntList.of(10, 20);

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(10, list.get(1));
        assertEquals(20, list.get(2));
        assertEquals(4, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testReplaceRangeWithArray() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        int[] replacement = { 10, 20 };

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(10, list.get(1));
        assertEquals(20, list.get(2));
    }

    @Test
    public void testReplaceAll() {
        list.addAll(new int[] { 1, 2, 3, 2, 5 });

        int count = list.replaceAll(2, 20);
        assertEquals(2, count);
        assertEquals(1, list.get(0));
        assertEquals(20, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(20, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testReplaceAllWithOperator() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        list.replaceAll(x -> x * 2);
        assertEquals(2, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(10, list.get(4));
    }

    @Test
    public void testReplaceIf() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        boolean result = list.replaceIf(x -> x % 2 == 0, 0);
        assertTrue(result);
        assertEquals(1, list.get(0));
        assertEquals(0, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(0, list.get(3));
        assertEquals(5, list.get(4));
    }

    // Fill Tests
    @Test
    public void testFill() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        list.fill(10);
        for (int i = 0; i < list.size(); i++) {
            assertEquals(10, list.get(i));
        }
    }

    @Test
    public void testFillRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        list.fill(1, 4, 10);
        assertEquals(1, list.get(0));
        assertEquals(10, list.get(1));
        assertEquals(10, list.get(2));
        assertEquals(10, list.get(3));
        assertEquals(5, list.get(4));
    }

    // Contains Tests
    @Test
    public void testContains() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        assertTrue(list.contains(3));
        assertFalse(list.contains(6));
    }

    @Test
    public void testContainsAny() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList other = IntList.of(6, 7, 3);

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAnyArray() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        int[] other = { 6, 7, 3 };

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList other = IntList.of(2, 4);

        assertTrue(list.containsAll(other));

        other = IntList.of(2, 6);
        assertFalse(list.containsAll(other));
    }

    @Test
    public void testDisjoint() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList other = IntList.of(4, 5, 6);

        assertTrue(list.disjoint(other));

        other = IntList.of(3, 4, 5);
        assertFalse(list.disjoint(other));
    }

    // Set Operations Tests
    @Test
    public void testIntersection() {
        list.addAll(new int[] { 1, 2, 2, 3, 4 });
        IntList other = IntList.of(2, 3, 5, 2);

        IntList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals(2, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    public void testDifference() {
        list.addAll(new int[] { 1, 2, 2, 3, 4 });
        IntList other = IntList.of(2, 3);

        IntList result = list.difference(other);
        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(4, result.get(2));
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList other = IntList.of(3, 4, 5);

        IntList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(4));
        assertTrue(result.contains(5));
    }

    // Index and Occurrence Tests
    @Test
    public void testOccurrencesOf() {
        list.addAll(new int[] { 1, 2, 3, 2, 2, 4 });

        assertEquals(3, list.occurrencesOf(2));
        assertEquals(1, list.occurrencesOf(1));
        assertEquals(0, list.occurrencesOf(5));
    }

    @Test
    public void testIndexOf() {
        list.addAll(new int[] { 1, 2, 3, 2, 5 });

        assertEquals(1, list.indexOf(2));
        assertEquals(2, list.indexOf(3));
        assertEquals(-1, list.indexOf(6));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        list.addAll(new int[] { 1, 2, 3, 2, 5 });

        assertEquals(3, list.indexOf(2, 2));
        assertEquals(-1, list.indexOf(2, 4));
    }

    @Test
    public void testLastIndexOf() {
        list.addAll(new int[] { 1, 2, 3, 2, 5 });

        assertEquals(3, list.lastIndexOf(2));
        assertEquals(0, list.lastIndexOf(1));
        assertEquals(-1, list.lastIndexOf(6));
    }

    @Test
    public void testLastIndexOfWithStart() {
        list.addAll(new int[] { 1, 2, 3, 2, 5 });

        assertEquals(1, list.lastIndexOf(2, 2));
        assertEquals(-1, list.lastIndexOf(5, 3));
    }

    // Min/Max/Median Tests
    @Test
    public void testMin() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });

        OptionalInt min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsInt());
    }

    @Test
    public void testMinEmpty() {
        OptionalInt min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMinRange() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });

        OptionalInt min = list.min(2, 4);
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsInt());
    }

    @Test
    public void testMax() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });

        OptionalInt max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5, max.getAsInt());
    }

    @Test
    public void testMedian() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });

        OptionalInt median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3, median.getAsInt());
    }

    // forEach Tests
    @Test
    public void testForEach() {
        list.addAll(new int[] { 1, 2, 3 });
        List<Integer> result = new ArrayList<>();

        list.forEach(result::add);

        assertEquals(3, result.size());
        assertEquals(1, result.get(0));
        assertEquals(2, result.get(1));
        assertEquals(3, result.get(2));
    }

    @Test
    public void testForEachRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        List<Integer> result = new ArrayList<>();

        list.forEach(1, 4, result::add);

        assertEquals(3, result.size());
        assertEquals(2, result.get(0));
        assertEquals(3, result.get(1));
        assertEquals(4, result.get(2));
    }

    // First/Last Tests
    @Test
    public void testFirst() {
        list.addAll(new int[] { 1, 2, 3 });

        OptionalInt first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1, first.getAsInt());
    }

    @Test
    public void testFirstEmpty() {
        OptionalInt first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.addAll(new int[] { 1, 2, 3 });

        OptionalInt last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3, last.getAsInt());
    }

    @Test
    public void testGetFirst() {
        list.addAll(new int[] { 1, 2, 3 });
        assertEquals(1, list.getFirst());
    }

    @Test
    public void testGetFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.addAll(new int[] { 1, 2, 3 });
        assertEquals(3, list.getLast());
    }

    @Test
    public void testAddFirst() {
        list.addAll(new int[] { 2, 3 });
        list.addFirst(1);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    public void testAddLast() {
        list.addAll(new int[] { 1, 2 });
        list.addLast(3);

        assertEquals(3, list.size());
        assertEquals(3, list.get(2));
    }

    @Test
    public void testRemoveFirst() {
        list.addAll(new int[] { 1, 2, 3 });

        int removed = list.removeFirst();
        assertEquals(1, removed);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
    }

    @Test
    public void testRemoveFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.addAll(new int[] { 1, 2, 3 });

        int removed = list.removeLast();
        assertEquals(3, removed);
        assertEquals(2, list.size());
        assertEquals(2, list.get(1));
    }

    // Distinct and Duplicates Tests
    @Test
    public void testDistinct() {
        list.addAll(new int[] { 1, 2, 2, 3, 3, 3 });

        IntList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertEquals(1, distinct.get(0));
        assertEquals(2, distinct.get(1));
        assertEquals(3, distinct.get(2));
    }

    @Test
    public void testHasDuplicates() {
        list.addAll(new int[] { 1, 2, 3 });
        assertFalse(list.hasDuplicates());

        list.add(2);
        assertTrue(list.hasDuplicates());
    }

    // Sort Tests
    @Test
    public void testIsSorted() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        assertTrue(list.isSorted());

        list.set(2, 1);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSort() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });

        list.sort();
        assertTrue(list.isSorted());
        assertEquals(1, list.get(0));
        assertEquals(1, list.get(1));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testParallelSort() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });

        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testReverseSort() {
        list.addAll(new int[] { 3, 1, 4, 1, 5 });

        list.reverseSort();
        assertEquals(5, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(1, list.get(4));
    }

    @Test
    public void testBinarySearch() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        assertEquals(2, list.binarySearch(3));
        assertTrue(list.binarySearch(6) < 0);
    }

    @Test
    public void testBinarySearchRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        assertEquals(3, list.binarySearch(1, 5, 4));
        assertTrue(list.binarySearch(1, 3, 4) < 0);
    }

    // Reverse and Rotate Tests
    @Test
    public void testReverse() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        list.reverse();
        assertEquals(5, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(1, list.get(4));
    }

    @Test
    public void testReverseRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        list.reverse(1, 4);
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(2, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    public void testRotate() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        list.rotate(2);
        assertEquals(4, list.get(0));
        assertEquals(5, list.get(1));
        assertEquals(1, list.get(2));
    }

    @Test
    public void testShuffle() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        IntList original = list.copy();

        list.shuffle();
        assertEquals(original.size(), list.size());

        // Check all elements are still there
        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }
    }

    @Test
    public void testShuffleWithRandom() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        Random rnd = new Random(42);

        list.shuffle(rnd);
        assertEquals(5, list.size());
    }

    @Test
    public void testSwap() {
        list.addAll(new int[] { 1, 2, 3 });

        list.swap(0, 2);
        assertEquals(3, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(1, list.get(2));
    }

    // Copy Tests
    @Test
    public void testCopy() {
        list.addAll(new int[] { 1, 2, 3 });

        IntList copy = list.copy();
        assertEquals(list.size(), copy.size());

        // Modify original
        list.set(0, 10);
        // Copy should not be affected
        assertEquals(1, copy.get(0));
    }

    @Test
    public void testCopyRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        IntList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals(2, copy.get(0));
        assertEquals(4, copy.get(2));
    }

    @Test
    public void testCopyWithStep() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        IntList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertEquals(1, copy.get(0));
        assertEquals(3, copy.get(1));
        assertEquals(5, copy.get(2));
    }

    // Split Tests
    @Test
    public void testSplit() {
        list.addAll(new int[] { 1, 2, 3, 4, 5, 6 });

        List<IntList> chunks = list.split(0, 6, 2);
        assertEquals(3, chunks.size());

        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(0).get(0));
        assertEquals(2, chunks.get(0).get(1));

        assertEquals(2, chunks.get(2).size());
        assertEquals(5, chunks.get(2).get(0));
        assertEquals(6, chunks.get(2).get(1));
    }

    // Utility Tests
    @Test
    public void testTrimToSize() {
        list.addAll(new int[] { 1, 2, 3 });
        list.trimToSize();
        assertEquals(3, list.size());
    }

    @Test
    public void testClear() {
        list.addAll(new int[] { 1, 2, 3 });

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add(1);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add(1);
        assertEquals(1, list.size());

        list.add(2);
        assertEquals(2, list.size());
    }

    // Conversion Tests
    @Test
    public void testBoxed() {
        list.addAll(new int[] { 1, 2, 3 });

        List<Integer> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Integer.valueOf(1), boxed.get(0));
        assertEquals(Integer.valueOf(3), boxed.get(2));
    }

    @Test
    public void testBoxedRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        List<Integer> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Integer.valueOf(2), boxed.get(0));
        assertEquals(Integer.valueOf(4), boxed.get(2));
    }

    @Test
    public void testToArray() {
        list.addAll(new int[] { 1, 2, 3 });

        int[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(1, array[0]);
        assertEquals(3, array[2]);

        // Modify array should not affect list
        array[0] = 10;
        assertEquals(1, list.get(0));
    }

    @Test
    public void testToLongList() {
        list.addAll(new int[] { 1, 2, 3 });

        LongList longList = list.toLongList();
        assertEquals(3, longList.size());
        assertEquals(1L, longList.get(0));
        assertEquals(3L, longList.get(2));
    }

    @Test
    public void testToFloatList() {
        list.addAll(new int[] { 1, 2, 3 });

        FloatList floatList = list.toFloatList();
        assertEquals(3, floatList.size());
        assertEquals(1.0f, floatList.get(0));
        assertEquals(3.0f, floatList.get(2));
    }

    @Test
    public void testToDoubleList() {
        list.addAll(new int[] { 1, 2, 3 });

        DoubleList doubleList = list.toDoubleList();
        assertEquals(3, doubleList.size());
        assertEquals(1.0, doubleList.get(0));
        assertEquals(3.0, doubleList.get(2));
    }

    @Test
    public void testToCollection() {
        list.addAll(new int[] { 1, 2, 3 });

        Set<Integer> set = list.toCollection(0, 3, size -> new HashSet<>());
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(3));
    }

    @Test
    public void testToMultiset() {
        list.addAll(new int[] { 1, 2, 2, 3 });

        Multiset<Integer> multiset = list.toMultiset(0, 4, size -> new Multiset<>());
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(2));
    }

    // Iterator and Stream Tests
    @Test
    public void testIterator() {
        list.addAll(new int[] { 1, 2, 3 });

        IntIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextInt());
        assertEquals(2, iter.nextInt());
        assertEquals(3, iter.nextInt());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        IntStream stream = list.stream();
        assertNotNull(stream);

        int sum = stream.sum();
        assertEquals(15, sum);
    }

    @Test
    public void testStreamRange() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        IntStream stream = list.stream(1, 4);
        int sum = stream.sum();
        assertEquals(9, sum); // 2 + 3 + 4
    }

    // Equals and HashCode Tests
    @Test
    public void testEquals() {
        list.addAll(new int[] { 1, 2, 3 });

        IntList other = IntList.of(1, 2, 3);
        assertTrue(list.equals(other));

        other.add(4);
        assertFalse(list.equals(other));

        assertFalse(list.equals(null));
        assertFalse(list.equals("not a list"));
        assertTrue(list.equals(list));
    }

    @Test
    public void testHashCode() {
        list.addAll(new int[] { 1, 2, 3 });
        IntList other = IntList.of(1, 2, 3);

        assertEquals(list.hashCode(), other.hashCode());

        other.add(4);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add(1);
        list.add(2);
        list.add(3);
        assertTrue(list.toString().contains("1"));
        assertTrue(list.toString().contains("2"));
        assertTrue(list.toString().contains("3"));
    }

    // Array method test
    @Test
    public void testArray() {
        list.addAll(new int[] { 1, 2, 3 });

        int[] array = list.array();
        assertEquals(1, array[0]);
        assertEquals(2, array[1]);
        assertEquals(3, array[2]);

        // Modifying the array should affect the list (since it's the internal array)
        array[0] = 10;
        assertEquals(10, list.get(0));
    }

    // Edge case tests
    @Test
    public void testLargeList() {
        int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }

        assertEquals(size, list.size());
        assertEquals(0, list.get(0));
        assertEquals(size - 1, list.get(size - 1));
    }

    @Test
    public void testEmptyOperations() {
        // Test operations on empty list
        assertFalse(list.remove(1));
        assertFalse(list.removeAllOccurrences(1));
        assertFalse(list.removeIf(x -> true));
        assertFalse(list.removeDuplicates());
        assertFalse(list.hasDuplicates());
        assertTrue(list.isSorted());

        list.sort();
        list.reverse();
        list.shuffle();

        assertTrue(list.isEmpty());
    }
}
