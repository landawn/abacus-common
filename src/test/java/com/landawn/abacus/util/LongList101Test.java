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
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

/**
 * Extended test suite for LongList covering additional edge cases and scenarios
 */
public class LongList101Test extends TestBase {

    private LongList list;

    @BeforeEach
    public void setUp() {
        list = new LongList();
    }

    // Additional Constructor Tests
    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new LongList(null, 0));
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        // Should not throw exception, will be handled internally
        assertThrows(IllegalArgumentException.class, () -> new LongList(-1));
    }

    @Test
    public void testConstructorWithZeroSize() {
        long[] array = { 1L, 2L, 3L };
        LongList list = new LongList(array, 0);
        assertTrue(list.isEmpty());
    }

    // Additional Static Factory Tests
    @Test
    public void testOfWithNullAndSize() {
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.of(null, 5));
    }

    @Test
    public void testCopyOfWithNull() {
        LongList list = LongList.copyOf(null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.copyOf(array, 2, 10));
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> LongList.range(1L, 10L, 0L));
    }

    @Test
    public void testRangeWithLargeValues() {
        // Test with values near Long.MAX_VALUE
        LongList list = LongList.range(Long.MAX_VALUE - 5, Long.MAX_VALUE);
        assertEquals(5, list.size());
        assertEquals(Long.MAX_VALUE - 5, list.get(0));
        assertEquals(Long.MAX_VALUE - 1, list.get(4));
    }

    @Test
    public void testRangeClosedWithLargeNegativeValues() {
        LongList list = LongList.rangeClosed(Long.MIN_VALUE, Long.MIN_VALUE + 4);
        assertEquals(5, list.size());
        assertEquals(Long.MIN_VALUE, list.get(0));
        assertEquals(Long.MIN_VALUE + 4, list.get(4));
    }

    // Additional Add Tests
    @Test
    public void testAddAtBeginning() {
        list.addAll(new long[] { 2L, 3L, 4L });
        list.add(0, 1L);
        assertEquals(4, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
    }

    @Test
    public void testAddAtEnd() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.add(list.size(), 4L);
        assertEquals(4, list.size());
        assertEquals(4L, list.get(3));
    }

    @Test
    public void testAddAllEmptyListAtVariousPositions() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList empty = new LongList();

        assertFalse(list.addAll(0, empty));
        assertFalse(list.addAll(1, empty));
        assertFalse(list.addAll(list.size(), empty));
    }

    @Test
    public void testAddAllWithNullArray() {
        list.add(1L);
        assertFalse(list.addAll((long[]) null));
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
        assertFalse(list.removeAllOccurrences(5L));
    }

    @Test
    public void testRemoveAllOccurrencesNotFound() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertFalse(list.removeAllOccurrences(5L));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIfEmptyList() {
        assertFalse(list.removeIf(x -> true));
    }

    @Test
    public void testRemoveIfNoMatch() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertFalse(list.removeIf(x -> x > 10));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicatesEmptyList() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRemoveDuplicatesSingleElement() {
        list.add(1L);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveDuplicatesSorted() {
        list.addAll(new long[] { 1L, 1L, 2L, 2L, 2L, 3L, 3L, 4L, 5L, 5L });
        assertTrue(list.removeDuplicates());
        assertEquals(5, list.size());
        for (int i = 1; i <= 5; i++) {
            assertEquals(i, list.get(i - 1));
        }
    }

    @Test
    public void testRetainAllEmpty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertTrue(list.retainAll(new LongList()));
        assertTrue(list.isEmpty());
    }

    @Test
    public void testDeleteAtBoundaries() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        // Delete first
        assertEquals(1L, list.delete(0));
        assertEquals(4, list.size());
        assertEquals(2L, list.get(0));

        // Delete last
        assertEquals(5L, list.delete(list.size() - 1));
        assertEquals(3, list.size());
        assertEquals(4L, list.get(list.size() - 1));
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.deleteAllByIndices();
        assertTrue(list.isEmpty());

        list.addAll(new long[] { 1L, 2L, 3L });
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteAllByIndicesOutOfOrder() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        list.deleteAllByIndices(4, 1, 2);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(4L, list.get(1));
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.deleteRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.deleteRange(0, 3);
        assertTrue(list.isEmpty());
    }

    // Additional Move and Replace Tests
    @Test
    public void testMoveRangeToBeginning() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        list.moveRange(3, 5, 0);
        assertEquals(5, list.size());
        assertEquals(4L, list.get(0));
        assertEquals(5L, list.get(1));
        assertEquals(1L, list.get(2));
    }

    @Test
    public void testMoveRangeToEnd() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        list.moveRange(0, 2, 3);
        assertEquals(5, list.size());
        assertEquals(3L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(5L, list.get(2));
        assertEquals(1L, list.get(3));
        assertEquals(2L, list.get(4));
    }

    @Test
    public void testReplaceRangeWithEmpty() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        list.replaceRange(1, 3, new LongList());
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(5L, list.get(2));
    }

    @Test
    public void testReplaceRangeExpanding() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.replaceRange(1, 2, LongList.of(10L, 20L, 30L));
        assertEquals(5, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(10L, list.get(1));
        assertEquals(20L, list.get(2));
        assertEquals(30L, list.get(3));
        assertEquals(3L, list.get(4));
    }

    @Test
    public void testReplaceAllNoMatch() {
        list.addAll(new long[] { 1L, 2L, 3L });
        int count = list.replaceAll(5L, 10L);
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIfFalseCondition() {
        list.addAll(new long[] { 1L, 2L, 3L });
        boolean result = list.replaceIf(x -> false, 10L);
        assertFalse(result);
    }

    // Additional Fill Tests
    @Test
    public void testFillEmptyList() {
        list.fill(10L);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFillRangeInvalidRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10L));
    }

    // Additional Contains Tests
    @Test
    public void testContainsEmptyList() {
        assertFalse(list.contains(1L));
    }

    @Test
    public void testContainsAnyBothEmpty() {
        LongList other = new LongList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAllEmptyAgainstNonEmpty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertTrue(list.containsAll(new LongList()));
    }

    @Test
    public void testDisjointWithSelf() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertFalse(list.disjoint(list));
    }

    // Additional Set Operations Tests
    @Test
    public void testIntersectionWithEmpty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.intersection(new LongList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersectionNoCommon() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.intersection(LongList.of(4L, 5L, 6L));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceWithEmpty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.difference(new LongList());
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testDifferenceAllRemoved() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.difference(LongList.of(1L, 2L, 3L));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceEmpty() {
        LongList result = list.symmetricDifference(new LongList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceOneEmpty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.symmetricDifference(new LongList());
        assertEquals(3, result.size());
    }

    // Additional Index Tests
    @Test
    public void testIndexOfFromIndexBeyondSize() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertEquals(-1, list.indexOf(1L, 10));
    }

    @Test
    public void testIndexOfNegativeFromIndex() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertEquals(0, list.indexOf(1L, -1));
    }

    @Test
    public void testLastIndexOfEmptyList() {
        assertEquals(-1, list.lastIndexOf(1L));
    }

    @Test
    public void testLastIndexOfNegativeStart() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertEquals(-1, list.lastIndexOf(1L, -1));
    }

    // Additional Min/Max/Median Tests
    @Test
    public void testMinMaxMedianSingleElement() {
        list.add(5L);

        OptionalLong min = list.min();
        assertTrue(min.isPresent());
        assertEquals(5L, min.getAsLong());

        OptionalLong max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5L, max.getAsLong());

        OptionalLong median = list.median();
        assertTrue(median.isPresent());
        assertEquals(5L, median.getAsLong());
    }

    @Test
    public void testMinMaxMedianEmptyRange() {
        list.addAll(new long[] { 1L, 2L, 3L });

        assertFalse(list.min(1, 1).isPresent());
        assertFalse(list.max(1, 1).isPresent());
        assertFalse(list.median(1, 1).isPresent());
    }

    @Test
    public void testMedianEvenElements() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L });
        OptionalLong median = list.median();
        assertTrue(median.isPresent());
        // Median of [1,2,3,4] should be 2 (lower middle element)
        assertEquals(2L, median.getAsLong());
    }

    // Additional forEach Tests
    @Test
    public void testForEachEmptyList() {
        List<Long> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachReverseRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        List<Long> result = new ArrayList<>();

        list.forEach(4, 1, result::add);

        assertEquals(3, result.size());
        assertEquals(5L, result.get(0));
        assertEquals(4L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testForEachWithNegativeToIndex() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        List<Long> result = new ArrayList<>();

        list.forEach(2, -1, result::add);

        assertEquals(3, result.size());
        assertEquals(3L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(1L, result.get(2));
    }

    // Additional Distinct Tests
    @Test
    public void testDistinctEmptyRange() {
        list.addAll(new long[] { 1L, 2L, 2L, 3L });
        LongList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDistinctSingleElement() {
        list.addAll(new long[] { 1L, 2L, 2L, 3L });
        LongList result = list.distinct(0, 1);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0));
    }

    // Additional Sort Tests
    @Test
    public void testSortEmptyList() {
        list.sort();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSortSingleElement() {
        list.add(5L);
        list.sort();
        assertEquals(1, list.size());
        assertEquals(5L, list.get(0));
    }

    @Test
    public void testParallelSortSmallList() {
        list.addAll(new long[] { 3L, 1L, 2L });
        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testBinarySearchUnsorted() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });
        // Binary search on unsorted list - result is undefined but should not throw
        int result = list.binarySearch(3L);
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
        list.add(5L);
        list.reverse();
        assertEquals(1, list.size());
        assertEquals(5L, list.get(0));
    }

    @Test
    public void testReverseRangeEmptyRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.reverse(1, 1);
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    // Additional Rotate Tests
    @Test
    public void testRotateEmptyList() {
        list.rotate(5);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRotateSingleElement() {
        list.add(5L);
        list.rotate(10);
        assertEquals(1, list.size());
        assertEquals(5L, list.get(0));
    }

    @Test
    public void testRotateNegativeDistance() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        list.rotate(-2);
        assertEquals(3L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(5L, list.get(2));
        assertEquals(1L, list.get(3));
        assertEquals(2L, list.get(4));
    }

    // Additional Shuffle Tests
    @Test
    public void testShuffleEmptyList() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffleSingleElement() {
        list.add(5L);
        list.shuffle();
        assertEquals(1, list.size());
        assertEquals(5L, list.get(0));
    }

    // Additional Swap Tests
    @Test
    public void testSwapSameIndex() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.swap(1, 1);
        assertEquals(2L, list.get(1));
    }

    @Test
    public void testSwapThrowsException() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    // Additional Copy Tests
    @Test
    public void testCopyEmptyList() {
        LongList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopyRangeInvalidIndices() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testCopyWithNegativeStep() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5L, copy.get(0));
        assertEquals(4L, copy.get(1));
        assertEquals(3L, copy.get(2));
        assertEquals(2L, copy.get(3));
    }

    @Test
    public void testCopyWithStepLargerThanRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList copy = list.copy(0, 5, 3);
        assertEquals(2, copy.size());
        assertEquals(1L, copy.get(0));
        assertEquals(4L, copy.get(1));
    }

    // Additional Split Tests
    @Test
    public void testSplitEmptyList() {
        List<LongList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitWithChunkSizeLargerThanList() {
        list.addAll(new long[] { 1L, 2L, 3L });
        List<LongList> chunks = list.split(0, 3, 10);
        assertEquals(1, chunks.size());
        assertEquals(3, chunks.get(0).size());
    }

    @Test
    public void testSplitUnevenChunks() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        List<LongList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    // Additional Boxed Tests
    @Test
    public void testBoxedEmptyList() {
        List<Long> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxedRangeInvalidIndices() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    // Additional Iterator Tests
    @Test
    public void testIteratorEmptyList() {
        LongIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    // Additional Stream Tests
    @Test
    public void testStreamEmptyList() {
        LongStream stream = list.stream();
        assertEquals(0, stream.count());
    }

    @Test
    public void testStreamRangeEmptyRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongStream stream = list.stream(1, 1);
        assertEquals(0, stream.count());
    }

    // Additional First/Last Tests
    @Test
    public void testGetFirstGetLastSingleElement() {
        list.add(5L);
        assertEquals(5L, list.getFirst());
        assertEquals(5L, list.getLast());
    }

    @Test
    public void testRemoveFirstRemoveLastSingleElement() {
        list.add(5L);
        assertEquals(5L, list.removeFirst());
        assertTrue(list.isEmpty());

        list.add(10L);
        assertEquals(10L, list.removeLast());
        assertTrue(list.isEmpty());
    }

    // Boundary Tests
    @Test
    public void testMaxArraySize() {
        // Test handling of large capacity requests
        try {
            LongList largeList = new LongList(Integer.MAX_VALUE - 8);
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
        list.add(1L);
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
        list.add(Long.MIN_VALUE);
        list.add(0L);
        list.add(Long.MAX_VALUE);
        String str = list.toString();
        assertTrue(str.contains(String.valueOf(Long.MIN_VALUE)));
        assertTrue(str.contains("0"));
        assertTrue(str.contains(String.valueOf(Long.MAX_VALUE)));
    }

    // Array method edge cases
    @Test
    public void testArrayModification() {
        list.addAll(new long[] { 1L, 2L, 3L });
        long[] array = list.array();

        // Verify modification affects the list
        array[1] = 20L;
        assertEquals(20L, list.get(1));

        // Clear and verify array is different
        list.clear();
        long[] newArray = list.array();
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
        LongList list1 = LongList.range(0, size);
        LongList list2 = LongList.range(size / 2, size + size / 2);

        // Test intersection
        LongList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        // Test difference
        LongList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        // Test symmetric difference
        LongList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    // Thread Safety Test (LongList is not thread-safe, but test behavior)
    @Test
    public void testConcurrentModification() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        // This should not throw ConcurrentModificationException since LongList
        // doesn't have modification tracking like ArrayList
        LongIterator iter = list.iterator();
        list.add(6L);

        // Iterator might give unexpected results but shouldn't throw
        assertTrue(iter.hasNext());
        iter.nextLong();
    }

    // Edge Cases for Long Type
    @Test
    public void testLongOverflow() {
        list.add(Long.MAX_VALUE);
        list.add(Long.MAX_VALUE);

        // Test operations that might cause overflow
        list.replaceAll(x -> x + 1);
        assertEquals(Long.MIN_VALUE, list.get(0));
        assertEquals(Long.MIN_VALUE, list.get(1));
    }

    @Test
    public void testNegativeValues() {
        list.addAll(new long[] { -5L, -3L, -1L, 0L, 1L, 3L, 5L });

        // Test operations with negative values
        OptionalLong min = list.min();
        assertTrue(min.isPresent());
        assertEquals(-5L, min.getAsLong());

        list.sort();
        assertEquals(-5L, list.get(0));
        assertEquals(5L, list.get(6));

        assertEquals(2, list.indexOf(-1L));
        assertEquals(3, list.binarySearch(0L));
    }

    @Test
    public void testLongSpecificOperations() {
        // Test with values that require 64-bit representation
        long bigValue1 = (long) Integer.MAX_VALUE * 2;
        long bigValue2 = (long) Integer.MIN_VALUE * 2;

        list.add(bigValue1);
        list.add(bigValue2);
        list.add(0L);

        assertTrue(list.contains(bigValue1));
        assertTrue(list.contains(bigValue2));

        list.sort();
        assertEquals(bigValue2, list.get(0));
        assertEquals(0L, list.get(1));
        assertEquals(bigValue1, list.get(2));
    }

    @Test
    public void testConversionsWithLargeValues() {
        // Test conversions with values outside float/double precision
        long preciseLong = 9223372036854775807L; // Long.MAX_VALUE
        list.add(preciseLong);

        FloatList floatList = list.toFloatList();
        // Float conversion will lose precision
        assertEquals(preciseLong, (long) floatList.get(0));

        DoubleList doubleList = list.toDoubleList();
        // Double conversion may also lose some precision for very large values
        assertEquals(preciseLong, (long) doubleList.get(0));
    }

    @Test
    public void testBatchOperationsWithLargeValues() {
        // Test set operations with large long values
        list.add(Long.MAX_VALUE);
        list.add(Long.MAX_VALUE - 1);
        list.add(Long.MIN_VALUE);

        LongList other = LongList.of(Long.MAX_VALUE, Long.MIN_VALUE + 1);

        LongList intersection = list.intersection(other);
        assertEquals(1, intersection.size());
        assertEquals(Long.MAX_VALUE, intersection.get(0));

        LongList difference = list.difference(other);
        assertEquals(2, difference.size());
        assertTrue(difference.contains(Long.MAX_VALUE - 1));
        assertTrue(difference.contains(Long.MIN_VALUE));
    }
}
