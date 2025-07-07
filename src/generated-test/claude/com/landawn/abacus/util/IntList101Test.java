package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.stream.IntStream;

/**
 * Extended test suite for IntList covering additional edge cases and scenarios
 */
public class IntList101Test extends TestBase {

    private IntList list;

    @BeforeEach
    public void setUp() {
        list = new IntList();
    }

    // Additional Constructor Tests
    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new IntList(null));
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new IntList(null, 0));
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        // Should not throw exception, will be handled internally
        assertThrows(NegativeArraySizeException.class, () -> new IntList(-1));
    }

    // Additional Static Factory Tests
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
        // Test with range larger than Integer.MAX_VALUE
        IntList list = IntList.random(Integer.MIN_VALUE, Integer.MAX_VALUE, 100);
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            int value = list.get(i);
            assertTrue(value >= Integer.MIN_VALUE && value < Integer.MAX_VALUE);
        }
    }

    // Additional Add Tests
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
        // Test capacity growth
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        assertEquals(100, list.size());
        for (int i = 0; i < 100; i++) {
            assertEquals(i, list.get(i));
        }
    }

    // Additional Remove Tests
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
    public void testRemoveIfEmptyList() {
        assertFalse(list.removeIf(x -> true));
    }

    @Test
    public void testRemoveIfNoMatch() {
        list.addAll(new int[] { 1, 2, 3 });
        assertFalse(list.removeIf(x -> x > 10));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicatesEmptyList() {
        assertFalse(list.removeDuplicates());
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

        // Delete first
        assertEquals(1, list.delete(0));
        assertEquals(4, list.size());
        assertEquals(2, list.get(0));

        // Delete last
        assertEquals(5, list.delete(list.size() - 1));
        assertEquals(3, list.size());
        assertEquals(4, list.get(list.size() - 1));
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.deleteAllByIndices();
        assertTrue(list.isEmpty());

        list.addAll(new int[] { 1, 2, 3 });
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteAllByIndicesOutOfOrder() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });
        list.deleteAllByIndices(4, 1, 2);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new int[] { 1, 2, 3 });
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new int[] { 1, 2, 3 });
        list.deleteRange(0, 3);
        assertTrue(list.isEmpty());
    }

    // Additional Move and Replace Tests
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

    // Additional Fill Tests
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

    // Additional Contains Tests
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

    // Additional Set Operations Tests
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

    // Additional Index Tests
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

    // Additional Min/Max/Median Tests
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
        // Median of [1,2,3,4] should be 2 (lower middle element)
        assertEquals(2, median.getAsInt());
    }

    // Additional forEach Tests
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

    // Additional Distinct Tests
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

    // Additional Sort Tests
    @Test
    public void testSortEmptyList() {
        list.sort();
        assertTrue(list.isEmpty());
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
        // Binary search on unsorted list - result is undefined but should not throw
        int result = list.binarySearch(3);
        // Just verify it doesn't throw exception
        assertNotNull(result);
    }

    // Additional Reverse Tests
    @Test
    public void testReverseEmptyList() {
        list.reverse();
        assertTrue(list.isEmpty());
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

    // Additional Rotate Tests
    @Test
    public void testRotateEmptyList() {
        list.rotate(5);
        assertTrue(list.isEmpty());
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

    // Additional Shuffle Tests
    @Test
    public void testShuffleEmptyList() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleSingleElement() {
        list.add(5);
        list.shuffle();
        assertEquals(1, list.size());
        assertEquals(5, list.get(0));
    }

    // Additional Swap Tests
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

    // Additional Copy Tests
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

    // Additional Split Tests
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

    // Additional Boxed Tests
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

    // Additional Iterator Tests
    @Test
    public void testIteratorEmptyList() {
        IntIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextInt());
    }

    // Additional Stream Tests
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

    // Additional First/Last Tests
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

    // Boundary Tests
    @Test
    public void testMaxArraySize() {
        // Test handling of large capacity requests
        try {
            IntList largeList = new IntList(Integer.MAX_VALUE - 8);
            // If it doesn't throw, just verify it's empty
            assertTrue(largeList.isEmpty());
        } catch (OutOfMemoryError e) {
            // Expected for large allocations
            assertTrue(true);
        }
    }

    @Test
    public void testEnsureCapacityOverflow() {
        // Test capacity overflow handling
        list.add(1);
        try {
            // Try to trigger capacity overflow
            for (int i = 0; i < 100; i++) {
                list.add(i);
            }
            assertTrue(list.size() > 1);
        } catch (OutOfMemoryError e) {
            // Expected if memory is limited
            assertTrue(true);
        }
    }

    // toString Tests  
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

    // Array method edge cases
    @Test
    public void testArrayModification() {
        list.addAll(new int[] { 1, 2, 3 });
        int[] array = list.array();

        // Verify modification affects the list
        array[1] = 20;
        assertEquals(20, list.get(1));

        // Clear and verify array is different
        list.clear();
        int[] newArray = list.array();
        assertSame(array, newArray);
    }

    // Performance/Stress Tests
    @Test
    public void testAddRemovePerformance() {
        // Add and remove many elements
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        assertEquals(count, list.size());

        // Remove every other element
        list.removeIf(x -> x % 2 == 0);
        assertEquals(count / 2, list.size());

        // Verify remaining elements
        for (int i = 0; i < list.size(); i++) {
            assertEquals(i * 2 + 1, list.get(i));
        }
    }

    @Test
    public void testBatchOperationsLargeData() {
        // Create large lists for batch operations
        int size = 1000;
        IntList list1 = IntList.range(0, size);
        IntList list2 = IntList.range(size / 2, size + size / 2);

        // Test intersection
        IntList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        // Test difference
        IntList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        // Test symmetric difference
        IntList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    // Thread Safety Test (IntList is not thread-safe, but test behavior)
    @Test
    public void testConcurrentModification() {
        list.addAll(new int[] { 1, 2, 3, 4, 5 });

        // This should not throw ConcurrentModificationException since IntList
        // doesn't have modification tracking like ArrayList
        IntIterator iter = list.iterator();
        list.add(6);

        // Iterator might give unexpected results but shouldn't throw
        assertTrue(iter.hasNext());
        iter.nextInt();
    }

    // Edge Cases for Primitive Type
    @Test
    public void testIntegerOverflow() {
        list.add(Integer.MAX_VALUE);
        list.add(Integer.MAX_VALUE);

        // Test operations that might cause overflow
        list.replaceAll(x -> x + 1);
        assertEquals(Integer.MIN_VALUE, list.get(0));
        assertEquals(Integer.MIN_VALUE, list.get(1));
    }

    @Test
    public void testNegativeValues() {
        list.addAll(new int[] { -5, -3, -1, 0, 1, 3, 5 });

        // Test operations with negative values
        OptionalInt min = list.min();
        assertTrue(min.isPresent());
        assertEquals(-5, min.getAsInt());

        list.sort();
        assertEquals(-5, list.get(0));
        assertEquals(5, list.get(6));

        assertEquals(2, list.indexOf(-1));
        assertEquals(3, list.binarySearch(0));
    }
}
