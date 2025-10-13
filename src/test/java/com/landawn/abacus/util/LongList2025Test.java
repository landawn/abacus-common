package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

@Tag("2025")
public class LongList2025Test extends TestBase {

    private LongList list;

    @BeforeEach
    public void setUp() {
        list = new LongList();
    }

    @Test
    public void testDefaultConstructor() {
        LongList list = new LongList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithCapacity() {
        LongList list = new LongList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LongList(-1));
    }

    @Test
    public void testConstructorWithArray() {
        long[] array = { 1L, 2L, 3L };
        LongList list = new LongList(array);
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongList list = new LongList(array, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void testOf() {
        LongList list = LongList.of(1L, 2L, 3L);
        assertEquals(3, list.size());
        assertEquals(2L, list.get(1));
    }

    @Test
    public void testOfWithArrayAndSize() {
        LongList list = LongList.of(new long[] { 1L, 2L, 3L }, 2);
        assertEquals(2, list.size());
    }

    @Test
    public void testCopyOf() {
        long[] array = { 1L, 2L, 3L };
        LongList list = LongList.copyOf(array);
        assertEquals(3, list.size());
        array[0] = 99L;
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testCopyOfRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongList list = LongList.copyOf(array, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2L, list.get(0));
        assertEquals(4L, list.get(2));
    }

    @Test
    public void testRange() {
        LongList list = LongList.range(1L, 5L);
        assertEquals(4, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(4L, list.get(3));
    }

    @Test
    public void testRangeWithStep() {
        LongList list = LongList.range(0L, 10L, 2L);
        assertEquals(5, list.size());
        assertEquals(0L, list.get(0));
        assertEquals(8L, list.get(4));
    }

    @Test
    public void testRangeClosed() {
        LongList list = LongList.rangeClosed(1L, 5L);
        assertEquals(5, list.size());
        assertEquals(5L, list.get(4));
    }

    @Test
    public void testRangeClosedWithStep() {
        LongList list = LongList.rangeClosed(0L, 10L, 2L);
        assertEquals(6, list.size());
        assertEquals(10L, list.get(5));
    }

    @Test
    public void testRepeat() {
        LongList list = LongList.repeat(5L, 3);
        assertEquals(3, list.size());
        assertEquals(5L, list.get(0));
        assertEquals(5L, list.get(2));
    }

    @Test
    public void testRandom() {
        LongList list = LongList.random(5);
        assertEquals(5, list.size());
    }

    @Test
    public void testGet() {
        list.add(10L);
        list.add(20L);
        assertEquals(10L, list.get(0));
        assertEquals(20L, list.get(1));
    }

    @Test
    public void testGetOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }

    @Test
    public void testSet() {
        list.add(10L);
        long old = list.set(0, 20L);
        assertEquals(10L, old);
        assertEquals(20L, list.get(0));
    }

    @Test
    public void testSetOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10L));
    }

    @Test
    public void testAdd() {
        list.add(10L);
        assertEquals(1, list.size());
        assertEquals(10L, list.get(0));
    }

    @Test
    public void testAddAtIndex() {
        list.add(10L);
        list.add(30L);
        list.add(1, 20L);
        assertEquals(3, list.size());
        assertEquals(20L, list.get(1));
        assertEquals(30L, list.get(2));
    }

    @Test
    public void testAddAtIndexOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10L));
    }

    @Test
    public void testAddAllLongList() {
        list.add(1L);
        LongList other = LongList.of(2L, 3L);
        assertTrue(list.addAll(other));
        assertEquals(3, list.size());
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testAddAllLongListEmpty() {
        list.add(1L);
        assertFalse(list.addAll(new LongList()));
    }

    @Test
    public void testAddAllLongListAtIndex() {
        list.add(1L);
        list.add(4L);
        LongList other = LongList.of(2L, 3L);
        assertTrue(list.addAll(1, other));
        assertEquals(4, list.size());
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testAddAllArray() {
        list.add(1L);
        assertTrue(list.addAll(new long[] { 2L, 3L }));
        assertEquals(3, list.size());
    }

    @Test
    public void testAddAllArrayAtIndex() {
        list.add(1L);
        list.add(4L);
        assertTrue(list.addAll(1, new long[] { 2L, 3L }));
        assertEquals(4, list.size());
        assertEquals(2L, list.get(1));
    }

    @Test
    public void testRemove() {
        list.add(10L);
        list.add(20L);
        list.add(10L);
        assertTrue(list.remove(10L));
        assertEquals(2, list.size());
        assertEquals(20L, list.get(0));
    }

    @Test
    public void testRemoveNotFound() {
        list.add(10L);
        assertFalse(list.remove(20L));
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.add(10L);
        list.add(20L);
        list.add(10L);
        list.add(30L);
        assertTrue(list.removeAllOccurrences(10L));
        assertEquals(2, list.size());
        assertEquals(20L, list.get(0));
        assertEquals(30L, list.get(1));
    }

    @Test
    public void testRemoveAllLongList() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongList toRemove = LongList.of(2L, 3L);
        assertTrue(list.removeAll(toRemove));
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testRemoveAllArray() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertTrue(list.removeAll(new long[] { 2L, 3L }));
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        assertTrue(list.removeIf(x -> x % 2 == 0));
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(3L, list.get(1));
    }

    @Test
    public void testRemoveDuplicates() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        list.add(3L);
        list.add(2L);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicatesNoDuplicates() {
        list.add(1L);
        list.add(2L);
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRetainAllLongList() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongList toRetain = LongList.of(2L, 3L, 4L);
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertEquals(2L, list.get(0));
        assertEquals(3L, list.get(1));
    }

    @Test
    public void testRetainAllArray() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertTrue(list.retainAll(new long[] { 2L, 3L, 4L }));
        assertEquals(2, list.size());
    }

    @Test
    public void testDelete() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        long deleted = list.delete(1);
        assertEquals(2L, deleted);
        assertEquals(2, list.size());
        assertEquals(3L, list.get(1));
    }

    @Test
    public void testDeleteOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(0));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.deleteAllByIndices(1, 3);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(3L, list.get(1));
    }

    @Test
    public void testDeleteRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.deleteRange(1, 3);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(4L, list.get(1));
    }

    @Test
    public void testMoveRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.moveRange(0, 2, 2);
        assertEquals(3L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(1L, list.get(2));
        assertEquals(2L, list.get(3));
    }

    @Test
    public void testReplaceRangeLongList() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        LongList replacement = LongList.of(99L);
        list.replaceRange(1, 3, replacement);
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(99L, list.get(1));
        assertEquals(4L, list.get(2));
    }

    @Test
    public void testReplaceRangeArray() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.replaceRange(1, 3, new long[] { 99L });
        assertEquals(3, list.size());
        assertEquals(99L, list.get(1));
    }

    @Test
    public void testReplaceAll() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        list.add(3L);
        int count = list.replaceAll(1L, 99L);
        assertEquals(2, count);
        assertEquals(99L, list.get(0));
        assertEquals(99L, list.get(2));
    }

    @Test
    public void testReplaceAllOperator() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.replaceAll(x -> x * 2);
        assertEquals(2L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(6L, list.get(2));
    }

    @Test
    public void testReplaceIf() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        assertTrue(list.replaceIf(x -> x % 2 == 0, 99L));
        assertEquals(99L, list.get(1));
        assertEquals(99L, list.get(3));
    }

    @Test
    public void testFill() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.fill(99L);
        assertEquals(99L, list.get(0));
        assertEquals(99L, list.get(1));
        assertEquals(99L, list.get(2));
    }

    @Test
    public void testFillRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.fill(1, 3, 99L);
        assertEquals(1L, list.get(0));
        assertEquals(99L, list.get(1));
        assertEquals(99L, list.get(2));
        assertEquals(4L, list.get(3));
    }

    @Test
    public void testContains() {
        list.add(1L);
        list.add(2L);
        assertTrue(list.contains(1L));
        assertFalse(list.contains(3L));
    }

    @Test
    public void testContainsAnyLongList() {
        list.add(1L);
        list.add(2L);
        assertTrue(list.containsAny(LongList.of(2L, 3L)));
        assertFalse(list.containsAny(LongList.of(4L, 5L)));
    }

    @Test
    public void testContainsAnyArray() {
        list.add(1L);
        list.add(2L);
        assertTrue(list.containsAny(new long[] { 2L, 3L }));
        assertFalse(list.containsAny(new long[] { 4L, 5L }));
    }

    @Test
    public void testContainsAllLongList() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertTrue(list.containsAll(LongList.of(1L, 2L)));
        assertFalse(list.containsAll(LongList.of(1L, 4L)));
    }

    @Test
    public void testContainsAllArray() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertTrue(list.containsAll(new long[] { 1L, 2L }));
        assertFalse(list.containsAll(new long[] { 1L, 4L }));
    }

    @Test
    public void testDisjointLongList() {
        list.add(1L);
        list.add(2L);
        assertTrue(list.disjoint(LongList.of(3L, 4L)));
        assertFalse(list.disjoint(LongList.of(2L, 3L)));
    }

    @Test
    public void testDisjointArray() {
        list.add(1L);
        list.add(2L);
        assertTrue(list.disjoint(new long[] { 3L, 4L }));
        assertFalse(list.disjoint(new long[] { 2L, 3L }));
    }

    @Test
    public void testIntersectionLongList() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongList result = list.intersection(LongList.of(2L, 3L, 4L));
        assertEquals(2, result.size());
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
    }

    @Test
    public void testIntersectionArray() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongList result = list.intersection(new long[] { 2L, 3L, 4L });
        assertEquals(2, result.size());
    }

    @Test
    public void testDifferenceLongList() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongList result = list.difference(LongList.of(2L, 4L));
        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(3L));
    }

    @Test
    public void testDifferenceArray() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongList result = list.difference(new long[] { 2L, 4L });
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifferenceLongList() {
        list.add(1L);
        list.add(2L);
        LongList result = list.symmetricDifference(LongList.of(2L, 3L));
        assertEquals(2, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(3L));
    }

    @Test
    public void testSymmetricDifferenceArray() {
        list.add(1L);
        list.add(2L);
        LongList result = list.symmetricDifference(new long[] { 2L, 3L });
        assertEquals(2, result.size());
    }

    @Test
    public void testOccurrencesOf() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        list.add(3L);
        list.add(1L);
        assertEquals(3, list.occurrencesOf(1L));
        assertEquals(0, list.occurrencesOf(4L));
    }

    @Test
    public void testIndexOf() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertEquals(1, list.indexOf(2L));
        assertEquals(-1, list.indexOf(4L));
    }

    @Test
    public void testIndexOfFromIndex() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        assertEquals(2, list.indexOf(1L, 1));
    }

    @Test
    public void testLastIndexOf() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        assertEquals(2, list.lastIndexOf(1L));
    }

    @Test
    public void testLastIndexOfFromIndex() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        list.add(2L);
        assertEquals(1, list.lastIndexOf(2L, 2));
    }

    @Test
    public void testMin() {
        list.add(3L);
        list.add(1L);
        list.add(2L);
        assertEquals(1L, list.min().get());
    }

    @Test
    public void testMinEmpty() {
        assertTrue(list.min().isEmpty());
    }

    @Test
    public void testMax() {
        list.add(3L);
        list.add(1L);
        list.add(2L);
        assertEquals(3L, list.max().get());
    }

    @Test
    public void testMedian() {
        list.add(1L);
        list.add(3L);
        list.add(2L);
        OptionalLong median = list.median();
        assertTrue(median.isPresent());
        assertEquals(2L, median.get());
    }

    @Test
    public void testSort() {
        list.add(3L);
        list.add(1L);
        list.add(2L);
        list.sort();
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testReverseSort() {
        list.add(3L);
        list.add(1L);
        list.add(2L);
        list.reverseSort();
        assertEquals(3L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(1L, list.get(2));
    }

    @Test
    public void testBinarySearch() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        assertEquals(2, list.binarySearch(3L));
        assertTrue(list.binarySearch(5L) < 0);
    }

    @Test
    public void testBinarySearchRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        assertEquals(2, list.binarySearch(1, 4, 3L));
    }

    @Test
    public void testReverse() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.reverse();
        assertEquals(3L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(1L, list.get(2));
    }

    @Test
    public void testReverseRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.reverse(1, 3);
        assertEquals(1L, list.get(0));
        assertEquals(3L, list.get(1));
        assertEquals(2L, list.get(2));
        assertEquals(4L, list.get(3));
    }

    @Test
    public void testRotate() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.rotate(1);
        assertEquals(4L, list.get(0));
        assertEquals(1L, list.get(1));
    }

    @Test
    public void testShuffle() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.shuffle();
        assertEquals(3, list.size());
    }

    @Test
    public void testSwap() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.swap(0, 2);
        assertEquals(3L, list.get(0));
        assertEquals(1L, list.get(2));
    }

    @Test
    public void testCopy() {
        list.add(1L);
        list.add(2L);
        LongList copy = list.copy();
        assertEquals(list.size(), copy.size());
        copy.set(0, 99L);
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testCopyRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        LongList copy = list.copy(1, 3);
        assertEquals(2, copy.size());
        assertEquals(2L, copy.get(0));
        assertEquals(3L, copy.get(1));
    }

    @Test
    public void testCopyAsClone() {
        list.add(1L);
        list.add(2L);
        LongList copy = list.copy();
        assertEquals(list.size(), copy.size());
        assertEquals(list.get(0), copy.get(0));
    }

    @Test
    public void testToArray() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        long[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(1L, array[0]);
    }

    @Test
    public void testToList() {
        list.add(1L);
        list.add(2L);
        List<Long> boxedList = list.toList();
        assertEquals(2, boxedList.size());
        assertEquals(Long.valueOf(1L), boxedList.get(0));
    }

    @Test
    public void testToListRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        List<Long> boxedList = list.toList(1, 3);
        assertEquals(2, boxedList.size());
        assertEquals(Long.valueOf(2L), boxedList.get(0));
    }

    @Test
    public void testToSet() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        java.util.Set<Long> set = list.toSet();
        assertEquals(2, set.size());
        assertTrue(set.contains(1L));
        assertTrue(set.contains(2L));
    }

    @Test
    public void testToSetRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(2L);
        java.util.Set<Long> set = list.toSet(1, 4);
        assertEquals(2, set.size());
    }

    @Test
    public void testToMultiset() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        Multiset<Long> multiset = list.toMultiset();
        assertEquals(3, multiset.size());
        assertEquals(2, multiset.countOfDistinctElements());
        assertEquals(2, multiset.get(1L));
    }

    @Test
    public void testForEach() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        final long[] sum = { 0 };
        list.forEach(x -> sum[0] += x);
        assertEquals(6L, sum[0]);
    }

    @Test
    public void testForEachRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        final long[] sum = { 0 };
        list.forEach(1, 3, x -> sum[0] += x);
        assertEquals(5L, sum[0]);
    }

    @Test
    public void testStream() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongStream stream = list.stream();
        assertEquals(6L, stream.sum());
    }

    @Test
    public void testStreamRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        LongStream stream = list.stream(1, 3);
        assertEquals(5L, stream.sum());
    }

    @Test
    public void testFirst() {
        list.add(1L);
        list.add(2L);
        OptionalLong first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1L, first.get());
    }

    @Test
    public void testFirstEmpty() {
        OptionalLong first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.add(1L);
        list.add(2L);
        OptionalLong last = list.last();
        assertTrue(last.isPresent());
        assertEquals(2L, last.get());
    }

    @Test
    public void testIsSorted() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertTrue(list.isSorted());
    }

    @Test
    public void testIsSortedNotSorted() {
        list.add(3L);
        list.add(1L);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());
        list.add(1L);
        assertEquals(1, list.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());
        list.add(1L);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testClear() {
        list.add(1L);
        list.add(2L);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testTrimToSize() {
        LongList list = new LongList(100);
        list.add(1L);
        list.add(2L);
        list.trimToSize();
        assertEquals(2, list.size());
    }

    @Test
    public void testEquals() {
        list.add(1L);
        list.add(2L);
        LongList other = LongList.of(1L, 2L);
        assertTrue(list.equals(other));
    }

    @Test
    public void testEqualsNotEqual() {
        list.add(1L);
        LongList other = LongList.of(2L);
        assertFalse(list.equals(other));
    }

    @Test
    public void testHashCode() {
        list.add(1L);
        list.add(2L);
        LongList other = LongList.of(1L, 2L);
        assertEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        list.add(1L);
        list.add(2L);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
    }

    @Test
    public void testIterator() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMinRange() {
        list.add(3L);
        list.add(1L);
        list.add(5L);
        list.add(2L);
        OptionalLong min = list.min(1, 3);
        assertTrue(min.isPresent());
        assertEquals(1L, min.get());
    }

    @Test
    public void testMaxRange() {
        list.add(3L);
        list.add(1L);
        list.add(5L);
        list.add(2L);
        OptionalLong max = list.max(1, 3);
        assertTrue(max.isPresent());
        assertEquals(5L, max.get());
    }

    @Test
    public void testMedianRange() {
        list.add(1L);
        list.add(3L);
        list.add(2L);
        list.add(5L);
        OptionalLong median = list.median(0, 3);
        assertTrue(median.isPresent());
        assertEquals(2L, median.get());
    }

    @Test
    public void testDistinctRange() {
        list.add(1L);
        list.add(2L);
        list.add(2L);
        list.add(3L);
        list.add(3L);
        LongList distinct = list.distinct(1, 5);
        assertEquals(2, distinct.size());
        assertTrue(distinct.contains(2L));
        assertTrue(distinct.contains(3L));
    }

    @Test
    public void testHasDuplicates() {
        list.add(1L);
        list.add(2L);
        assertFalse(list.hasDuplicates());
        list.add(1L);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void testParallelSort() {
        list.add(3L);
        list.add(1L);
        list.add(2L);
        list.parallelSort();
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testShuffleWithRandom() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        java.util.Random rnd = new java.util.Random(12345);
        list.shuffle(rnd);
        assertEquals(3, list.size());
    }

    @Test
    public void testCopyWithStep() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.add(5L);
        LongList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertEquals(1L, copy.get(0));
        assertEquals(3L, copy.get(1));
        assertEquals(5L, copy.get(2));
    }

    @Test
    public void testSplit() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.add(5L);
        List<LongList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testBoxed() {
        list.add(1L);
        list.add(2L);
        List<Long> boxed = list.boxed();
        assertEquals(2, boxed.size());
        assertEquals(Long.valueOf(1L), boxed.get(0));
    }

    @Test
    public void testBoxedRange() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        List<Long> boxed = list.boxed(1, 3);
        assertEquals(2, boxed.size());
        assertEquals(Long.valueOf(2L), boxed.get(0));
    }

    @Test
    public void testToFloatList() {
        list.add(1L);
        list.add(2L);
        FloatList floatList = list.toFloatList();
        assertEquals(2, floatList.size());
        assertEquals(1.0f, floatList.get(0));
    }

    @Test
    public void testToDoubleList() {
        list.add(1L);
        list.add(2L);
        DoubleList doubleList = list.toDoubleList();
        assertEquals(2, doubleList.size());
        assertEquals(1.0, doubleList.get(0));
    }

    @Test
    public void testToMultisetRange() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        list.add(3L);
        Multiset<Long> multiset = list.toMultiset(0, 4, n -> new Multiset<>());
        assertEquals(4, multiset.size());
        assertEquals(3, multiset.countOfDistinctElements());
        assertEquals(2, multiset.get(1L));
    }

    @Test
    public void testGetFirst() {
        list.add(1L);
        list.add(2L);
        assertEquals(1L, list.getFirst());
    }

    @Test
    public void testGetFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.add(1L);
        list.add(2L);
        assertEquals(2L, list.getLast());
    }

    @Test
    public void testGetLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testAddFirst() {
        list.add(2L);
        list.add(3L);
        list.addFirst(1L);
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testAddLast() {
        list.add(1L);
        list.add(2L);
        list.addLast(3L);
        assertEquals(3, list.size());
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testRemoveFirst() {
        list.add(1L);
        list.add(2L);
        long removed = list.removeFirst();
        assertEquals(1L, removed);
        assertEquals(1, list.size());
        assertEquals(2L, list.get(0));
    }

    @Test
    public void testRemoveFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.add(1L);
        list.add(2L);
        long removed = list.removeLast();
        assertEquals(2L, removed);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }
}
