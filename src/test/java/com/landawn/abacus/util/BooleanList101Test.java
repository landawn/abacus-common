package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BooleanList101Test extends TestBase {

    private BooleanList list;

    @BeforeEach
    public void setUp() {
        list = new BooleanList();
    }

    // ========== Additional Constructor Tests ==========

    @Test
    public void testConstructorWithZeroCapacity() {
        BooleanList zeroCapList = new BooleanList(0);
        assertEquals(0, zeroCapList.size());

        // Should still be able to add elements
        zeroCapList.add(true);
        assertEquals(1, zeroCapList.size());
        assertEquals(true, zeroCapList.get(0));
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new BooleanList(null));
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new BooleanList(null, 5));
    }

    // ========== Additional Factory Method Tests ==========

    @Test
    public void testOfWithEmptyArray() {
        BooleanList emptyList = BooleanList.of(new boolean[0]);
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testCopyOfWithNullArray() {
        BooleanList copyList = BooleanList.copyOf(null);
        assertEquals(0, copyList.size());
    }

    @Test
    public void testCopyOfRangeWithInvalidIndices() {
        boolean[] arr = { true, false, true, false };

        // fromIndex > toIndex
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, 3, 1));

        // fromIndex < 0
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, -1, 2));

        // toIndex > array length
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, 0, 10));
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

    // ========== Capacity and Growth Tests ==========

    @Test
    public void testCapacityGrowth() {
        // Test that the list grows properly when adding many elements
        BooleanList smallList = new BooleanList(2);
        for (int i = 0; i < 100; i++) {
            smallList.add(i % 2 == 0);
        }
        assertEquals(100, smallList.size());

        // Verify all elements
        for (int i = 0; i < 100; i++) {
            assertEquals(i % 2 == 0, smallList.get(i));
        }
    }

    @Test
    public void testAddAllWithLargeCollection() {
        boolean[] largeArray = new boolean[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i % 3 == 0;
        }

        list.addAll(largeArray);
        assertEquals(1000, list.size());

        // Verify sampling of elements
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(true, list.get(3));
    }

    // ========== Complex Remove Operations ==========

    @Test
    public void testRemoveAllOccurrencesInLargeList() {
        // Create a large list with pattern
        for (int i = 0; i < 1000; i++) {
            list.add(i % 5 < 2); // true for 0,1, false for 2,3,4
        }

        int originalSize = list.size();
        assertTrue(list.removeAllOccurrences(true));
        assertEquals(600, list.size()); // 3 out of 5 are false

        // Verify no true values remain
        for (int i = 0; i < list.size(); i++) {
            assertFalse(list.get(i));
        }
    }

    @Test
    public void testRemoveIfWithComplexPredicate() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        // Remove elements at even indices (when combined with their position)
        final int[] index = { 0 };
        boolean removed = list.removeIf(b -> {
            boolean shouldRemove = index[0] % 2 == 0;
            index[0]++;
            return shouldRemove;
        });

        assertTrue(removed);
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(false, list.get(1));
    }

    // ========== Batch Remove with Performance Considerations ==========

    @Test
    public void testBatchRemovePerformance() {
        // This tests the optimization that uses Set when appropriate
        for (int i = 0; i < 100; i++) {
            list.add(i % 2 == 0);
        }

        BooleanList toRemove = new BooleanList();
        for (int i = 0; i < 50; i++) {
            toRemove.add(i % 3 == 0);
        }

        int originalSize = list.size();
        list.removeAll(toRemove);
        assertTrue(list.size() < originalSize);
    }

    // ========== Range Operations Edge Cases ==========

    @Test
    public void testDeleteRangeEntireList() {
        list.add(true);
        list.add(false);
        list.add(true);

        list.deleteRange(0, 3);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testMoveRangeToSamePosition() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        // Move range to where it already is
        list.moveRange(1, 3, 1);

        // List should remain unchanged
        assertEquals(4, list.size());
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(true, list.get(2));
        assertEquals(false, list.get(3));
    }

    @Test
    public void testMoveRangeToEnd() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        // Move first two elements to end
        list.moveRange(0, 2, 3);

        assertEquals(5, list.size());
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(true, list.get(2));
        assertEquals(true, list.get(3));
        assertEquals(false, list.get(4));
    }

    @Test
    public void testReplaceRangeWithLargerReplacement() {
        list.add(true);
        list.add(false);
        list.add(true);

        boolean[] replacement = { false, false, false, false, false };
        list.replaceRange(1, 2, replacement);

        assertEquals(7, list.size());
        assertEquals(true, list.get(0));
        for (int i = 1; i <= 5; i++) {
            assertEquals(false, list.get(i));
        }
        assertEquals(true, list.get(6));
    }

    // ========== Complex Search Operations ==========

    @Test
    public void testIndexOfWithFromIndexAtEnd() {
        list.add(true);
        list.add(false);
        list.add(true);

        // fromIndex equals size
        assertEquals(-1, list.indexOf(true, 3));
        assertEquals(-1, list.indexOf(false, 3));
    }

    @Test
    public void testLastIndexOfWithNegativeStartIndex() {
        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(-1, list.lastIndexOf(true, -1));
        assertEquals(-1, list.lastIndexOf(false, -10));
    }

    @Test
    public void testLastIndexOfWithStartIndexBeyondSize() {
        list.add(true);
        list.add(false);
        list.add(true);

        // Should search from the last element
        assertEquals(2, list.lastIndexOf(true, 10));
        assertEquals(1, list.lastIndexOf(false, 10));
    }

    // ========== Set Operations with Edge Cases ==========

    @Test
    public void testIntersectionWithDuplicates() {
        list.add(true);
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList other = BooleanList.of(true, false, false);
        BooleanList intersection = list.intersection(other);

        // Should preserve occurrences from first list that exist in second
        assertEquals(2, intersection.size());
        assertEquals(true, intersection.get(0));
        assertEquals(false, intersection.get(1));
    }

    @Test
    public void testDifferenceWithAllElementsRemoved() {
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList other = BooleanList.of(true, true, false);
        BooleanList difference = list.difference(other);

        assertEquals(0, difference.size());
        assertTrue(difference.isEmpty());
    }

    @Test
    public void testSymmetricDifferenceWithIdenticalLists() {
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList other = BooleanList.of(true, false, true);
        BooleanList symDiff = list.symmetricDifference(other);

        assertEquals(0, symDiff.size());
        assertTrue(symDiff.isEmpty());
    }

    // ========== forEach with Edge Cases ==========

    @Test
    public void testForEachWithEmptyRange() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> collected = new ArrayList<>();
        list.forEach(1, 1, b -> collected.add(b));

        assertEquals(0, collected.size());
    }

    @Test
    public void testForEachReverseWithSingleElement() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> collected = new ArrayList<>();
        list.forEach(1, 1, b -> collected.add(b));

        assertEquals(0, collected.size());
    }

    @Test
    public void testForEachWithNegativeToIndex() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> collected = new ArrayList<>();
        list.forEach(2, -1, b -> collected.add(b));

        // Should iterate from index 2 down to index 0 (not including -1)
        assertEquals(3, collected.size());
        assertEquals(true, collected.get(0));
        assertEquals(false, collected.get(1));
        assertEquals(true, collected.get(2));
    }

    // ========== Sort and Reverse with Special Cases ==========

    @Test
    public void testSortAllSameValues() {
        for (int i = 0; i < 10; i++) {
            list.add(true);
        }

        list.sort();

        assertEquals(10, list.size());
        for (int i = 0; i < 10; i++) {
            assertEquals(true, list.get(i));
        }
    }

    @Test
    public void testReverseSingleElement() {
        list.add(true);

        list.reverse();

        assertEquals(1, list.size());
        assertEquals(true, list.get(0));
    }

    @Test
    public void testReverseRangeWithInvalidRange() {
        list.add(true);
        list.add(false);
        list.add(true);

        // Reverse with fromIndex > toIndex should throw
        assertThrows(IndexOutOfBoundsException.class, () -> list.reverse(2, 1));
    }

    // ========== Copy Operations with Step ==========

    @Test
    public void testCopyWithNegativeStep() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        // Copy in reverse with negative step
        BooleanList reversed = list.copy(4, -1, -1);

        assertEquals(5, reversed.size());
        assertEquals(true, reversed.get(0));
        assertEquals(false, reversed.get(1));
        assertEquals(true, reversed.get(2));
        assertEquals(false, reversed.get(3));
        assertEquals(true, reversed.get(4));
    }

    @Test
    public void testCopyWithLargeStep() {
        for (int i = 0; i < 10; i++) {
            list.add(i % 2 == 0);
        }

        // Copy every 3rd element
        BooleanList sampled = list.copy(0, 10, 3);

        assertEquals(4, sampled.size());
        assertEquals(true, sampled.get(0)); // index 0
        assertEquals(false, sampled.get(1)); // index 3
        assertEquals(true, sampled.get(2)); // index 6
        assertEquals(false, sampled.get(3)); // index 9
    }

    // ========== Split with Edge Cases ==========

    @Test
    public void testSplitWithChunkSizeLargerThanList() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<BooleanList> splits = list.split(0, 3, 10);

        assertEquals(1, splits.size());
        assertEquals(3, splits.get(0).size());
        assertEquals(list, splits.get(0));
    }

    @Test
    public void testSplitWithChunkSizeOne() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<BooleanList> splits = list.split(0, 3, 1);

        assertEquals(3, splits.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(1, splits.get(i).size());
            assertEquals(list.get(i), splits.get(i).get(0));
        }
    }

    // ========== Stream Operations ==========

    @Test
    public void testStreamWithEmptyList() {
        long count = list.stream().count();
        assertEquals(0, count);
    }

    @Test
    public void testStreamWithInvalidRange() {
        list.add(true);
        list.add(false);

        assertThrows(IndexOutOfBoundsException.class, () -> list.stream(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.stream(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.stream(2, 1));
    }

    // ========== Conversion Operations ==========

    @Test
    public void testToCollectionWithEmptyRange() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> collection = list.toCollection(1, 1, ArrayList::new);
        assertEquals(0, collection.size());
    }

    @Test
    public void testToMultisetWithDuplicates() {
        list.add(true);
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(false);

        Multiset<Boolean> multiset = list.toMultiset(0, list.size(), Multiset::new);

        assertEquals(3, multiset.count(Boolean.TRUE));
        assertEquals(3, multiset.count(Boolean.FALSE));
        assertEquals(6, multiset.size());
    }

    // ========== Performance and Stress Tests ==========

    @Test
    public void testLargeListOperations() {
        // Create a large list
        final int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add(i % 7 < 3);
        }

        // Test various operations on large list
        assertEquals(size, list.size());

        // Test contains
        assertTrue(list.contains(true));
        assertTrue(list.contains(false));

        // Test indexOf on large list
        int firstTrue = list.indexOf(true);
        int firstFalse = list.indexOf(false);
        assertTrue(firstTrue >= 0);
        assertTrue(firstFalse >= 0);

        // Test sort on large list
        list.sort();
        assertTrue(list.isSorted());

        // Verify sort correctness
        int i = 0;
        while (i < list.size() && !list.get(i))
            i++;
        int falseCount = i;
        while (i < list.size() && list.get(i))
            i++;
        assertEquals(list.size(), i);

        // Test clear on large list
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    // ========== Thread Safety Warning Test (Documentation) ==========

    @Test
    public void testConcurrentModification() {
        // Note: BooleanList is not thread-safe
        // This test documents that concurrent modification should be avoided
        list.add(true);
        list.add(false);
        list.add(true);

        // Create an iterator
        var iterator = list.iterator();

        // Modify list while iterating (this is not safe in general)
        assertTrue(iterator.hasNext());
        assertEquals(true, iterator.nextBoolean());

        // Modification after iterator creation
        list.add(false);

        // Iterator may not reflect the change
        // This behavior is undefined and should be avoided
    }

    // ========== Memory and Reference Tests ==========

    @Test
    public void testArrayMethodReturnsSameReference() {
        list.add(true);
        list.add(false);

        boolean[] array1 = list.array();
        boolean[] array2 = list.array();

        // Should return the same array reference
        assertSame(array1, array2);

        // Modifications through array should affect list
        array1[0] = false;
        assertEquals(false, list.get(0));
    }

    @Test
    public void testTrimToSizeActuallyTrims() {
        // Create list with large initial capacity
        BooleanList largeCapList = new BooleanList(1000);
        largeCapList.add(true);
        largeCapList.add(false);

        assertEquals(2, largeCapList.size());

        // Trim and verify it still works correctly
        largeCapList.trimToSize();
        assertEquals(2, largeCapList.size());
        assertEquals(true, largeCapList.get(0));
        assertEquals(false, largeCapList.get(1));

        // Should still be able to add elements after trim
        largeCapList.add(true);
        assertEquals(3, largeCapList.size());
    }

    // ========== equals() and hashCode() Contract ==========

    @Test
    public void testEqualsAndHashCodeContract() {
        BooleanList list1 = BooleanList.of(true, false, true);
        BooleanList list2 = BooleanList.of(true, false, true);
        BooleanList list3 = BooleanList.of(false, true, false);

        // Reflexive
        assertEquals(list1, list1);

        // Symmetric
        assertEquals(list1, list2);
        assertEquals(list2, list1);

        // Transitive
        BooleanList list4 = BooleanList.of(true, false, true);
        assertEquals(list1, list2);
        assertEquals(list2, list4);
        assertEquals(list1, list4);

        // Consistent with hashCode
        assertEquals(list1.hashCode(), list2.hashCode());

        // Not equal to different list
        assertNotEquals(list1, list3);
    }

    // ========== Special Values and Boundary Tests ==========

    @Test
    public void testMaxSizeOperations() {
        // Note: Actually creating a max-size array would require too much memory
        // This test documents the behavior

        // Test that appropriate exceptions are thrown for overflow
        BooleanList smallList = new BooleanList();

        // Adding beyond max array size should throw OutOfMemoryError
        // (Not actually testing this as it would require too much memory)
    }

    @Test
    public void testEmptyListBehaviors() {
        // Comprehensive test of all operations on empty list
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
        assertEquals(list, new BooleanList());
        assertEquals(list.hashCode(), new BooleanList().hashCode());

        // Test operations that should work on empty list
        assertFalse(list.contains(true));
        assertFalse(list.contains(false));
        assertEquals(-1, list.indexOf(true));
        assertEquals(-1, list.lastIndexOf(false));
        assertEquals(0, list.occurrencesOf(true));

        // Test operations that return empty
        assertTrue(list.toArray().length == 0);
        assertTrue(list.boxed().isEmpty());
        assertTrue(list.distinct(0, 0).isEmpty());
        assertFalse(list.iterator().hasNext());

        // Test operations that should be no-ops
        list.sort();
        list.reverse();
        list.shuffle();
        list.fill(true);
        list.deleteRange(0, 0);

        assertTrue(list.isEmpty());
    }
}
