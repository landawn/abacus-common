package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PrimitiveList100Test extends TestBase {

    private IntList list;

    @BeforeEach
    public void setUp() {
        list = IntList.of(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("Test array() method")
    public void testArray() {
        int[] array = list.array();
        assertNotNull(array);
        assertTrue(array.length >= list.size());
    }

    @Test
    @DisplayName("Test addAll(L c) method")
    public void testAddAllList() {
        IntList other = IntList.of(6, 7, 8);
        boolean result = list.addAll(other);

        assertTrue(result);
        assertEquals(8, list.size());
        assertEquals(6, list.get(5));
        assertEquals(7, list.get(6));
        assertEquals(8, list.get(7));
    }

    @Test
    @DisplayName("Test addAll(int index, L c) method")
    public void testAddAllListAtIndex() {
        IntList other = IntList.of(10, 11);
        boolean result = list.addAll(2, other);

        assertTrue(result);
        assertEquals(7, list.size());
        assertEquals(10, list.get(2));
        assertEquals(11, list.get(3));
        assertEquals(3, list.get(4));
    }

    @Test
    @DisplayName("Test addAll(A a) method")
    public void testAddAllArray() {
        int[] array = { 6, 7, 8 };
        boolean result = list.addAll(array);

        assertTrue(result);
        assertEquals(8, list.size());
        assertEquals(6, list.get(5));
        assertEquals(7, list.get(6));
        assertEquals(8, list.get(7));
    }

    @Test
    @DisplayName("Test addAll(int index, A a) method")
    public void testAddAllArrayAtIndex() {
        int[] array = { 10, 11 };
        boolean result = list.addAll(2, array);

        assertTrue(result);
        assertEquals(7, list.size());
        assertEquals(10, list.get(2));
        assertEquals(11, list.get(3));
    }

    @Test
    @DisplayName("Test removeAll(L c) method")
    public void testRemoveAllList() {
        IntList toRemove = IntList.of(2, 4);
        boolean result = list.removeAll(toRemove);

        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    @DisplayName("Test removeAll(A a) method")
    public void testRemoveAllArray() {
        int[] toRemove = { 2, 4 };
        boolean result = list.removeAll(toRemove);

        assertTrue(result);
        assertEquals(3, list.size());
        assertFalse(list.contains(2));
        assertFalse(list.contains(4));
    }

    @Test
    @DisplayName("Test removeDuplicates() method")
    public void testRemoveDuplicates() {
        IntList listWithDups = IntList.of(1, 2, 2, 3, 3, 3, 4);
        boolean result = listWithDups.removeDuplicates();

        assertTrue(result);
        assertEquals(4, listWithDups.size());
        assertEquals(IntList.of(1, 2, 3, 4), listWithDups);
    }

    @Test
    @DisplayName("Test retainAll(L c) method")
    public void testRetainAllList() {
        IntList toRetain = IntList.of(2, 3, 6);
        boolean result = list.retainAll(toRetain);

        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
        assertEquals(3, list.get(1));
    }

    @Test
    @DisplayName("Test retainAll(A a) method")
    public void testRetainAllArray() {
        int[] toRetain = { 2, 3, 6 };
        boolean result = list.retainAll(toRetain);

        assertTrue(result);
        assertEquals(2, list.size());
        assertTrue(list.contains(2));
        assertTrue(list.contains(3));
    }

    @Test
    @DisplayName("Test deleteAllByIndices(int... indices) method")
    public void testDeleteAllByIndices() {
        list.deleteAllByIndices(1, 3);

        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    @DisplayName("Test deleteRange(int fromIndex, int toIndex) method")
    public void testDeleteRange() {
        list.deleteRange(1, 4);

        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(1));
    }

    @Test
    @DisplayName("Test moveRange(int fromIndex, int toIndex, int newPosition) method")
    public void testMoveRange() {
        list.moveRange(1, 3, 3);

        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(5, list.get(2));
        assertEquals(2, list.get(3));
        assertEquals(3, list.get(4));
    }

    @Test
    @DisplayName("Test replaceRange(int fromIndex, int toIndex, L replacement) method")
    public void testReplaceRangeList() {
        IntList replacement = IntList.of(10, 11);
        list.replaceRange(1, 3, replacement);

        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(10, list.get(1));
        assertEquals(11, list.get(2));
        assertEquals(4, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    @DisplayName("Test replaceRange(int fromIndex, int toIndex, A replacement) method")
    public void testReplaceRangeArray() {
        int[] replacement = { 10, 11 };
        list.replaceRange(1, 3, replacement);

        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(10, list.get(1));
        assertEquals(11, list.get(2));
    }

    @Test
    @DisplayName("Test containsAny(L l) method")
    public void testContainsAnyList() {
        IntList other1 = IntList.of(6, 7, 3);
        IntList other2 = IntList.of(6, 7, 8);

        assertTrue(list.containsAny(other1));
        assertFalse(list.containsAny(other2));
    }

    @Test
    @DisplayName("Test containsAny(A a) method")
    public void testContainsAnyArray() {
        int[] array1 = { 6, 7, 3 };
        int[] array2 = { 6, 7, 8 };

        assertTrue(list.containsAny(array1));
        assertFalse(list.containsAny(array2));
    }

    @Test
    @DisplayName("Test containsAll(L l) method")
    public void testContainsAllList() {
        IntList subset = IntList.of(2, 3);
        IntList notSubset = IntList.of(2, 6);

        assertTrue(list.containsAll(subset));
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    @DisplayName("Test containsAll(A a) method")
    public void testContainsAllArray() {
        int[] subset = { 2, 3 };
        int[] notSubset = { 2, 6 };

        assertTrue(list.containsAll(subset));
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    @DisplayName("Test disjoint(L l) method")
    public void testDisjointList() {
        IntList disjoint = IntList.of(6, 7, 8);
        IntList notDisjoint = IntList.of(3, 6, 7);

        assertTrue(list.disjoint(disjoint));
        assertFalse(list.disjoint(notDisjoint));
    }

    @Test
    @DisplayName("Test disjoint(A a) method")
    public void testDisjointArray() {
        int[] disjoint = { 6, 7, 8 };
        int[] notDisjoint = { 3, 6, 7 };

        assertTrue(list.disjoint(disjoint));
        assertFalse(list.disjoint(notDisjoint));
    }

    @Test
    @DisplayName("Test intersection(L b) method")
    public void testIntersectionList() {
        IntList other = IntList.of(3, 4, 5, 6, 7);
        IntList result = list.intersection(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(3, 4, 5), result);
    }

    @Test
    @DisplayName("Test intersection(A b) method")
    public void testIntersectionArray() {
        int[] other = { 3, 4, 5, 6, 7 };
        IntList result = list.intersection(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(3, 4, 5), result);
    }

    @Test
    @DisplayName("Test difference(L b) method")
    public void testDifferenceList() {
        IntList other = IntList.of(3, 4, 6);
        IntList result = list.difference(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(1, 2, 5), result);
    }

    @Test
    @DisplayName("Test difference(A a) method")
    public void testDifferenceArray() {
        int[] other = { 3, 4, 6 };
        IntList result = list.difference(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(1, 2, 5), result);
    }

    @Test
    @DisplayName("Test symmetricDifference(L b) method")
    public void testSymmetricDifferenceList() {
        IntList other = IntList.of(4, 5, 6, 7);
        IntList result = list.symmetricDifference(other);

        assertEquals(5, result.size());
        assertTrue(result.containsAll(IntList.of(1, 2, 3, 6, 7)));
    }

    @Test
    @DisplayName("Test symmetricDifference(A b) method")
    public void testSymmetricDifferenceArray() {
        int[] other = { 4, 5, 6, 7 };
        IntList result = list.symmetricDifference(other);

        assertEquals(5, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
        assertTrue(result.contains(6));
        assertTrue(result.contains(7));
    }

    @Test
    @DisplayName("Test hasDuplicates() method")
    public void testHasDuplicates() {
        assertFalse(list.hasDuplicates());

        IntList withDups = IntList.of(1, 2, 2, 3);
        assertTrue(withDups.hasDuplicates());
    }

    @Test
    @DisplayName("Test distinct() method")
    public void testDistinct() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3, 3);
        IntList result = withDups.distinct();

        assertEquals(3, result.size());
        assertEquals(IntList.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("Test distinct(int fromIndex, int toIndex) method")
    public void testDistinctRange() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3, 3, 4);
        IntList result = withDups.distinct(1, 6);

        assertEquals(2, result.size());
        assertEquals(IntList.of(2, 3), result);
    }

    @Test
    @DisplayName("Test isSorted() method")
    public void testIsSorted() {
        assertTrue(list.isSorted());

        IntList unsorted = IntList.of(3, 1, 4, 2);
        assertFalse(unsorted.isSorted());
    }

    @Test
    @DisplayName("Test sort() method")
    public void testSort() {
        IntList unsorted = IntList.of(3, 1, 4, 2, 5);
        unsorted.sort();

        assertTrue(unsorted.isSorted());
        assertEquals(IntList.of(1, 2, 3, 4, 5), unsorted);
    }

    @Test
    @DisplayName("Test reverseSort() method")
    public void testReverseSort() {
        IntList unsorted = IntList.of(3, 1, 4, 2, 5);
        unsorted.reverseSort();

        assertEquals(IntList.of(5, 4, 3, 2, 1), unsorted);
    }

    @Test
    @DisplayName("Test reverse() method")
    public void testReverse() {
        list.reverse();

        assertEquals(IntList.of(5, 4, 3, 2, 1), list);
    }

    @Test
    @DisplayName("Test reverse(int fromIndex, int toIndex) method")
    public void testReverseRange() {
        list.reverse(1, 4);

        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(2, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    @DisplayName("Test rotate(int distance) method")
    public void testRotate() {
        list.rotate(2);

        assertEquals(IntList.of(4, 5, 1, 2, 3), list);
    }

    @Test
    @DisplayName("Test shuffle() method")
    public void testShuffle() {
        IntList original = list.copy();
        list.shuffle();

        assertEquals(original.size(), list.size());
        assertTrue(list.containsAll(original));
    }

    @Test
    @DisplayName("Test shuffle(Random rnd) method")
    public void testShuffleWithRandom() {
        Random rnd = new Random(42);
        IntList original = list.copy();
        list.shuffle(rnd);

        assertEquals(original.size(), list.size());
        assertTrue(list.containsAll(original));
    }

    @Test
    @DisplayName("Test swap(int i, int j) method")
    public void testSwap() {
        list.swap(1, 3);

        assertEquals(4, list.get(1));
        assertEquals(2, list.get(3));
    }

    @Test
    @DisplayName("Test copy() method")
    public void testCopy() {
        IntList copy = list.copy();

        assertEquals(list, copy);
        assertNotSame(list, copy);
    }

    @Test
    @DisplayName("Test copy(int fromIndex, int toIndex) method")
    public void testCopyRange() {
        IntList copy = list.copy(1, 4);

        assertEquals(3, copy.size());
        assertEquals(IntList.of(2, 3, 4), copy);
    }

    @Test
    @DisplayName("Test copy(int fromIndex, int toIndex, int step) method")
    public void testCopyRangeWithStep() {
        IntList copy = list.copy(0, 5, 2);

        assertEquals(3, copy.size());
        assertEquals(IntList.of(1, 3, 5), copy);
    }

    @Test
    @DisplayName("Test split(int chunkSize) method")
    public void testSplit() {
        List<IntList> chunks = list.split(2);

        assertEquals(3, chunks.size());
        assertEquals(IntList.of(1, 2), chunks.get(0));
        assertEquals(IntList.of(3, 4), chunks.get(1));
        assertEquals(IntList.of(5), chunks.get(2));
    }

    @Test
    @DisplayName("Test split(int fromIndex, int toIndex, int chunkSize) method")
    public void testSplitRange() {
        List<IntList> chunks = list.split(1, 5, 2);

        assertEquals(2, chunks.size());
        assertEquals(IntList.of(2, 3), chunks.get(0));
        assertEquals(IntList.of(4, 5), chunks.get(1));
    }

    @Test
    @DisplayName("Test trimToSize() method")
    public void testTrimToSize() {
        IntList trimmed = list.trimToSize();

        assertSame(list, trimmed);
        assertEquals(5, trimmed.size());
    }

    @Test
    @DisplayName("Test clear() method")
    public void testClear() {
        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test isEmpty() method")
    public void testIsEmpty() {
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test size() method")
    public void testSize() {
        assertEquals(5, list.size());

        list.add(6);
        assertEquals(6, list.size());

        list.clear();
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test toArray() method")
    public void testToArray() {
        int[] array = list.toArray();

        assertNotNull(array);
        assertEquals(5, array.length);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, array);
    }

    @Test
    @DisplayName("Test boxed() method")
    public void testBoxed() {
        List<Integer> boxed = list.boxed();

        assertEquals(5, boxed.size());
        assertEquals(Integer.valueOf(1), boxed.get(0));
        assertEquals(Integer.valueOf(5), boxed.get(4));
    }

    @Test
    @DisplayName("Test boxed(int fromIndex, int toIndex) method")
    public void testBoxedRange() {
        List<Integer> boxed = list.boxed(1, 4);

        assertEquals(3, boxed.size());
        assertEquals(Integer.valueOf(2), boxed.get(0));
        assertEquals(Integer.valueOf(4), boxed.get(2));
    }

    @Test
    @DisplayName("Test toSet() method")
    public void testToSet() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3);
        Set<Integer> set = withDups.toSet();

        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    @DisplayName("Test toSet(int fromIndex, int toIndex) method")
    public void testToSetRange() {
        Set<Integer> set = list.toSet(1, 4);

        assertEquals(3, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }

    @Test
    @DisplayName("Test toCollection(IntFunction<C> supplier) method")
    public void testToCollection() {
        LinkedList<Integer> collection = list.toCollection(size -> new LinkedList<>());

        assertEquals(5, collection.size());
        assertEquals(Integer.valueOf(1), collection.getFirst());
        assertEquals(Integer.valueOf(5), collection.getLast());
    }

    @Test
    @DisplayName("Test toCollection(int fromIndex, int toIndex, IntFunction<C> supplier) method")
    public void testToCollectionRange() {
        ArrayList<Integer> collection = list.toCollection(1, 4, ArrayList::new);

        assertEquals(3, collection.size());
        assertEquals(Integer.valueOf(2), collection.get(0));
        assertEquals(Integer.valueOf(4), collection.get(2));
    }

    @Test
    @DisplayName("Test toMultiset() method")
    public void testToMultiset() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3, 3);
        Multiset<Integer> multiset = withDups.toMultiset();

        assertEquals(1, multiset.occurrencesOf(1));
        assertEquals(2, multiset.occurrencesOf(2));
        assertEquals(3, multiset.occurrencesOf(3));
    }

    @Test
    @DisplayName("Test toMultiset(int fromIndex, int toIndex) method")
    public void testToMultisetRange() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3, 3);
        Multiset<Integer> multiset = withDups.toMultiset(1, 5);

        assertEquals(2, multiset.occurrencesOf(2));
        assertEquals(2, multiset.occurrencesOf(3));
        assertEquals(0, multiset.occurrencesOf(1));
    }

    @Test
    @DisplayName("Test toMultiset(IntFunction<Multiset<B>> supplier) method")
    public void testToMultisetWithSupplier() {
        Multiset<Integer> multiset = list.toMultiset(size -> new Multiset<>());

        assertEquals(1, multiset.occurrencesOf(1));
        assertEquals(1, multiset.occurrencesOf(5));
    }

    @Test
    @DisplayName("Test toMultiset(int fromIndex, int toIndex, IntFunction<Multiset<B>> supplier) method")
    public void testToMultisetRangeWithSupplier() {
        Multiset<Integer> multiset = list.toMultiset(1, 4, size -> new Multiset<>());

        assertEquals(1, multiset.occurrencesOf(2));
        assertEquals(1, multiset.occurrencesOf(3));
        assertEquals(1, multiset.occurrencesOf(4));
        assertEquals(0, multiset.occurrencesOf(1));
    }

    @Test
    @DisplayName("Test iterator() method")
    public void testIterator() {
        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertEquals(Integer.valueOf(2), iterator.next());

        int count = 2;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    @DisplayName("Test println() method")
    public void testPrintln() {
        assertDoesNotThrow(() -> list.println());
    }

    @Test
    @DisplayName("Test edge cases and exceptions")
    public void testEdgeCases() {
        IntList emptyList = IntList.of();

        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
        assertFalse(emptyList.hasDuplicates());
        assertTrue(emptyList.isSorted());

        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(3, 1));

        list.addAll((IntList) null);
        list.removeAll((IntList) null);
    }
}
