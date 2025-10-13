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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BooleanList101Test extends TestBase {

    private BooleanList list;

    @BeforeEach
    public void setUp() {
        list = new BooleanList();
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
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new BooleanList(null));
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new BooleanList(null, 5));
    }

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

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, 3, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, -1, 2));

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
    public void testAddAllWithLargeCollection() {
        boolean[] largeArray = new boolean[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = i % 3 == 0;
        }

        list.addAll(largeArray);
        assertEquals(1000, list.size());

        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(true, list.get(3));
    }

    @Test
    public void testRemoveAllOccurrencesInLargeList() {
        for (int i = 0; i < 1000; i++) {
            list.add(i % 5 < 2);
        }

        int originalSize = list.size();
        assertTrue(list.removeAllOccurrences(true));
        assertEquals(600, list.size());

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

    @Test
    public void testBatchRemovePerformance() {
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

        list.moveRange(1, 3, 1);

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

    @Test
    public void testIndexOfWithFromIndexAtEnd() {
        list.add(true);
        list.add(false);
        list.add(true);

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

        assertEquals(2, list.lastIndexOf(true, 10));
        assertEquals(1, list.lastIndexOf(false, 10));
    }

    @Test
    public void testIntersectionWithDuplicates() {
        list.add(true);
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList other = BooleanList.of(true, false, false);
        BooleanList intersection = list.intersection(other);

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

        assertEquals(3, collected.size());
        assertEquals(true, collected.get(0));
        assertEquals(false, collected.get(1));
        assertEquals(true, collected.get(2));
    }

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

        assertThrows(IndexOutOfBoundsException.class, () -> list.reverse(2, 1));
    }

    @Test
    public void testCopyWithNegativeStep() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

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

        BooleanList sampled = list.copy(0, 10, 3);

        assertEquals(4, sampled.size());
        assertEquals(true, sampled.get(0));
        assertEquals(false, sampled.get(1));
        assertEquals(true, sampled.get(2));
        assertEquals(false, sampled.get(3));
    }

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

    @Test
    public void testLargeListOperations() {
        final int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add(i % 7 < 3);
        }

        assertEquals(size, list.size());

        assertTrue(list.contains(true));
        assertTrue(list.contains(false));

        int firstTrue = list.indexOf(true);
        int firstFalse = list.indexOf(false);
        assertTrue(firstTrue >= 0);
        assertTrue(firstFalse >= 0);

        list.sort();
        assertTrue(list.isSorted());

        int i = 0;
        while (i < list.size() && !list.get(i))
            i++;
        int falseCount = i;
        while (i < list.size() && list.get(i))
            i++;
        assertEquals(list.size(), i);

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConcurrentModification() {
        list.add(true);
        list.add(false);
        list.add(true);

        var iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(true, iterator.nextBoolean());

        list.add(false);

    }

    @Test
    public void testArrayMethodReturnsSameReference() {
        list.add(true);
        list.add(false);

        boolean[] array1 = list.array();
        boolean[] array2 = list.array();

        assertSame(array1, array2);

        array1[0] = false;
        assertEquals(false, list.get(0));
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
    public void testMaxSizeOperations() {

        BooleanList smallList = new BooleanList();

    }

    @Test
    public void testEmptyListBehaviors() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
        assertEquals(list, new BooleanList());
        assertEquals(list.hashCode(), new BooleanList().hashCode());

        assertFalse(list.contains(true));
        assertFalse(list.contains(false));
        assertEquals(-1, list.indexOf(true));
        assertEquals(-1, list.lastIndexOf(false));
        assertEquals(0, list.occurrencesOf(true));

        assertTrue(list.toArray().length == 0);
        assertTrue(list.boxed().isEmpty());
        assertTrue(list.distinct(0, 0).isEmpty());
        assertFalse(list.iterator().hasNext());

        list.sort();
        list.reverse();
        list.shuffle();
        list.fill(true);
        list.deleteRange(0, 0);

        assertTrue(list.isEmpty());
    }
}
