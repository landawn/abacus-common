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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

@Tag("2025")
public class LongListTest extends TestBase {

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
        long deleted = list.removeAt(1);
        assertEquals(2L, deleted);
        assertEquals(2, list.size());
        assertEquals(3L, list.get(1));
    }

    @Test
    public void testDeleteOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(0));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        list.add(4L);
        list.removeAt(1, 3);
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
        list.removeRange(1, 3);
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
        assertEquals(3, list.frequency(1L));
        assertEquals(0, list.frequency(4L));
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
        assertFalse(list.containsDuplicates());
        list.add(1L);
        assertTrue(list.containsDuplicates());
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
    public void testGetLast() {
        list.add(1L);
        list.add(2L);
        assertEquals(2L, list.getLast());
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
    public void testRemoveLast() {
        list.add(1L);
        list.add(2L);
        long removed = list.removeLast();
        assertEquals(2L, removed);
        assertEquals(1, list.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        LongList list = new LongList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertTrue(list.internalArray().length >= 10 || list.internalArray().length == 0);
    }

    @Test
    public void testOfArrayAndSize() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        LongList list = LongList.of(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.of(a, 6));
    }

    @Test
    public void testGetAndSet() {
        LongList list = LongList.of(1L, 2L, 3L);
        assertEquals(2L, list.get(1));
        long oldValue = list.set(1, 99L);
        assertEquals(2L, oldValue);
        assertEquals(99L, list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 100L));
    }

    @Test
    public void testAddAndAddAtIndex() {
        LongList list = new LongList();
        list.add(10L);
        list.add(30L);
        list.add(1, 20L);
        assertEquals(3, list.size());
        assertArrayEquals(new long[] { 10L, 20L, 30L }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 40L));
    }

    @Test
    public void testAddAll() {
        LongList list = LongList.of(1L, 2L);
        assertTrue(list.addAll(LongList.of(3L, 4L)));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
        assertFalse(list.addAll(new LongList()));
    }

    @Test
    public void testAddAllAtIndex() {
        LongList list = LongList.of(1L, 4L);
        assertTrue(list.addAll(1, LongList.of(2L, 3L)));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
    }

    @Test
    public void testRemoveAll() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L, 2L);
        LongList toRemove = LongList.of(2L, 4L, 6L);
        assertTrue(list.removeAll(toRemove));
        assertArrayEquals(new long[] { 1L, 3L, 5L }, list.toArray());
    }

    @Test
    public void testRetainAll() {
        LongList list = LongList.of(1L, 2L, 3L, 2L, 4L);
        LongList toRetain = LongList.of(2L, 4L, 5L);
        assertTrue(list.retainAll(toRetain));
        assertArrayEquals(new long[] { 2L, 2L, 4L }, list.toArray());
    }

    @Test
    public void testReplaceRange() {
        LongList list = LongList.of(0L, 1L, 2L, 3L, 4L, 5L);
        LongList replacement = LongList.of(99L, 98L);
        list.replaceRange(2, 4, replacement);
        assertArrayEquals(new long[] { 0L, 1L, 99L, 98L, 4L, 5L }, list.toArray());
    }

    @Test
    public void testReplaceAllValues() {
        LongList list = LongList.of(1L, 2L, 1L, 3L, 1L);
        int count = list.replaceAll(1L, 99L);
        assertEquals(3, count);
        assertArrayEquals(new long[] { 99L, 2L, 99L, 3L, 99L }, list.toArray());
    }

    @Test
    public void testContainsAny() {
        LongList list = LongList.of(1L, 2L, 3L);
        assertTrue(list.containsAny(LongList.of(4L, 5L, 2L)));
        assertFalse(list.containsAny(LongList.of(4L, 5L, 6L)));
    }

    @Test
    public void testContainsAll() {
        LongList list = LongList.of(1L, 2L, 3L, 4L);
        assertTrue(list.containsAll(LongList.of(2L, 4L)));
        assertFalse(list.containsAll(LongList.of(2L, 5L)));
    }

    @Test
    public void testDisjoint() {
        LongList list1 = LongList.of(1L, 2L, 3L);
        LongList list2 = LongList.of(4L, 5L, 6L);
        assertTrue(list1.disjoint(list2));
        LongList list3 = LongList.of(3L, 4L, 5L);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void testIntersection() {
        LongList list1 = LongList.of(1L, 2L, 2L, 3L, 4L);
        LongList list2 = LongList.of(2L, 3L, 5L, 2L);
        LongList intersection = list1.intersection(list2);
        intersection.sort();
        assertArrayEquals(new long[] { 2L, 2L, 3L }, intersection.toArray());
    }

    @Test
    public void testDifference() {
        LongList list1 = LongList.of(1L, 2L, 2L, 3L, 4L);
        LongList list2 = LongList.of(2L, 3L, 5L);
        LongList difference = list1.difference(list2);
        assertArrayEquals(new long[] { 1L, 2L, 4L }, difference.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        LongList list1 = LongList.of(1L, 2L, 2L, 3L);
        LongList list2 = LongList.of(2L, 3L, 4L);
        LongList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new long[] { 1L, 2L, 4L }, symmDiff.toArray());
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        LongList list = LongList.of(1L, 2L, 3L, 2L, 1L);
        assertEquals(0, list.indexOf(1L));
        assertEquals(1, list.indexOf(2L));
        assertEquals(3, list.indexOf(2L, 2));
        assertEquals(-1, list.indexOf(4L));
        assertEquals(4, list.lastIndexOf(1L));
        assertEquals(3, list.lastIndexOf(2L));
        assertEquals(1, list.lastIndexOf(2L, 2));
        assertEquals(-1, list.lastIndexOf(4L));
    }

    @Test
    public void testMinMaxMedian() {
        LongList list = LongList.of(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L, 5L);
        assertEquals(1L, list.min().getAsLong());
        assertEquals(9L, list.max().getAsLong());
        assertEquals(4L, list.median().getAsLong());

        LongList emptyList = new LongList();
        assertTrue(emptyList.min().isEmpty());
        assertTrue(emptyList.max().isEmpty());
        assertTrue(emptyList.median().isEmpty());
    }

    @Test
    public void testFirstLastOptional() {
        LongList list = LongList.of(10L, 20L, 30L);
        assertEquals(10L, list.first().getAsLong());
        assertEquals(30L, list.last().getAsLong());
        assertTrue(new LongList().first().isEmpty());
        assertTrue(new LongList().last().isEmpty());
    }

    @Test
    public void testSortAndReverseSort() {
        LongList list = LongList.of(3L, 1L, 4L, 1L, 5L, 9L);
        list.sort();
        assertArrayEquals(new long[] { 1L, 1L, 3L, 4L, 5L, 9L }, list.toArray());
        list.reverseSort();
        assertArrayEquals(new long[] { 9L, 5L, 4L, 3L, 1L, 1L }, list.toArray());
    }

    @Test
    public void testCopyAndSplit() {
        LongList list = LongList.of(0L, 1L, 2L, 3L, 4L, 5L, 6L);
        LongList copy = list.copy(1, 5);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, copy.toArray());

        List<LongList> chunks = list.split(3);
        assertEquals(3, chunks.size());
        assertArrayEquals(new long[] { 0L, 1L, 2L }, chunks.get(0).toArray());
        assertArrayEquals(new long[] { 3L, 4L, 5L }, chunks.get(1).toArray());
        assertArrayEquals(new long[] { 6L }, chunks.get(2).toArray());
    }

    @Test
    public void testTrimToSizeAndClear() {
        LongList list = new LongList(10);
        list.add(1L);
        list.add(2L);
        assertEquals(10, list.internalArray().length);
        list.trimToSize();
        assertEquals(2, list.internalArray().length);
        list.clear();
        assertEquals(0, list.size());
    }

    @Test
    public void testBoxedAndToArray() {
        long[] a = { 1L, 2L, 3L };
        LongList list = LongList.of(a);
        List<Long> boxed = list.boxed();
        assertEquals(Long.valueOf(1L), boxed.get(0));
        assertEquals(Arrays.asList(1L, 2L, 3L), boxed);

        long[] toArray = list.toArray();
        assertArrayEquals(a, toArray);
        assertNotSame(a, toArray);
    }

    @Test
    public void testToOtherLists() {
        LongList list = LongList.of(10L, 20L, 30L);
        FloatList floatList = list.toFloatList();
        assertArrayEquals(new float[] { 10.0f, 20.0f, 30.0f }, floatList.toArray());
    }

    @Test
    public void testGetFirstLast() {
        LongList list = LongList.of(10L, 20L, 30L);
        assertEquals(10L, list.getFirst());
        assertEquals(30L, list.getLast());
        assertThrows(NoSuchElementException.class, () -> new LongList().getFirst());
        assertThrows(NoSuchElementException.class, () -> new LongList().getLast());
    }

    @Test
    public void testAddRemoveFirstLast() {
        LongList list = new LongList();
        list.addFirst(10L);
        list.addLast(30L);
        list.addFirst(0L);
        list.addLast(40L);
        assertArrayEquals(new long[] { 0L, 10L, 30L, 40L }, list.toArray());
        assertEquals(0L, list.removeFirst());
        assertEquals(40L, list.removeLast());
        assertArrayEquals(new long[] { 10L, 30L }, list.toArray());
    }

    @Test
    public void testEqualsAndHashCode() {
        LongList list1 = LongList.of(1L, 2L, 3L);
        LongList list2 = LongList.of(1L, 2L, 3L);
        LongList list3 = LongList.of(3L, 2L, 1L);

        assertEquals(list1, list2);
        assertEquals(list1.hashCode(), list2.hashCode());

        assertNotEquals(list1, list3);
        assertNotEquals(list1.hashCode(), list3.hashCode());

        assertNotEquals(list1, null);
        assertNotEquals(list1, new Object());
    }

    @Test
    public void testToCollection() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L);
        ArrayList<Long> result = list.toCollection(1, 4, ArrayList::new);
        assertEquals(Arrays.asList(2L, 3L, 4L), result);
    }

    @Test
    public void testConstructorWithNullArrayAndSize() {
        assertThrows(NullPointerException.class, () -> new LongList(null, 0));
    }

    @Test
    public void testConstructorWithZeroSize() {
        long[] array = { 1L, 2L, 3L };
        LongList list = new LongList(array, 0);
        assertTrue(list.isEmpty());
    }

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
    public void testRemoveIfNoMatch() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertFalse(list.removeIf(x -> x > 10));
        assertEquals(3, list.size());
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

        assertEquals(1L, list.removeAt(0));
        assertEquals(4, list.size());
        assertEquals(2L, list.get(0));

        assertEquals(5L, list.removeAt(list.size() - 1));
        assertEquals(3, list.size());
        assertEquals(4L, list.get(list.size() - 1));
    }

    @Test
    public void testDeleteAllByIndicesEmpty() {
        list.removeAt();
        assertTrue(list.isEmpty());

        list.addAll(new long[] { 1L, 2L, 3L });
        list.removeAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteAllByIndicesOutOfOrder() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        list.removeAt(4, 1, 2);
        assertEquals(2, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(4L, list.get(1));
    }

    @Test
    public void testDeleteRangeEmptyRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRangeEntireList() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.removeRange(0, 3);
        assertTrue(list.isEmpty());
    }

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
        assertEquals(2L, median.getAsLong());
    }

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
        int result = list.binarySearch(3L);
        assertNotNull(result);
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

    @Test
    public void testShuffleSingleElement() {
        list.add(5L);
        list.shuffle();
        assertEquals(1, list.size());
        assertEquals(5L, list.get(0));
    }

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

    @Test
    public void testIteratorEmptyList() {
        LongIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

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

    @Test
    public void testMaxArraySize() {
        try {
            LongList largeList = new LongList(Integer.MAX_VALUE - 8);
            assertTrue(largeList.isEmpty());
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEnsureCapacityOverflow() {
        list.add(1L);
        try {
            for (int i = 0; i < 100; i++) {
                list.add(i);
            }
            assertTrue(list.size() > 1);
        } catch (OutOfMemoryError e) {
            assertTrue(true);
        }
    }

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

    @Test
    public void testArrayModification() {
        list.addAll(new long[] { 1L, 2L, 3L });
        long[] array = list.internalArray();

        array[1] = 20L;
        assertEquals(20L, list.get(1));

        list.clear();
        long[] newArray = list.internalArray();
        assertSame(array, newArray);
    }

    @Test
    public void testBatchOperationsLargeData() {
        int size = 1000;
        LongList list1 = LongList.range(0, size);
        LongList list2 = LongList.range(size / 2, size + size / 2);

        LongList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        LongList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        LongList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        LongIterator iter = list.iterator();
        list.add(6L);

        assertTrue(iter.hasNext());
        iter.nextLong();
    }

    @Test
    public void testLongOverflow() {
        list.add(Long.MAX_VALUE);
        list.add(Long.MAX_VALUE);

        list.replaceAll(x -> x + 1);
        assertEquals(Long.MIN_VALUE, list.get(0));
        assertEquals(Long.MIN_VALUE, list.get(1));
    }

    @Test
    public void testNegativeValues() {
        list.addAll(new long[] { -5L, -3L, -1L, 0L, 1L, 3L, 5L });

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
        long preciseLong = 9223372036854775807L;
        list.add(preciseLong);

        FloatList floatList = list.toFloatList();
        assertEquals(preciseLong, (long) floatList.get(0));

        DoubleList doubleList = list.toDoubleList();
        assertEquals(preciseLong, (long) doubleList.get(0));
    }

    @Test
    public void testBatchOperationsWithLargeValues() {
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

    @Test
    public void testConstructorWithArrayAndSizeThrowsException() {
        long[] array = { 1L, 2L, 3L };
        assertThrows(IndexOutOfBoundsException.class, () -> new LongList(array, 4));
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new LongList(null));
    }

    @Test
    public void testOfWithEmptyArray() {
        LongList list = LongList.of();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOfWithNull() {
        LongList list = LongList.of((long[]) null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRangeWithNegativeStep() {
        LongList list = LongList.range(5L, 0L, -1L);
        assertEquals(5, list.size());
        assertEquals(5L, list.get(0));
        assertEquals(1L, list.get(4));
    }

    @Test
    public void testGetThrowsException() {
        list.add(10L);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testAddAllEmpty() {
        list.add(1L);
        LongList empty = new LongList();
        boolean result = list.addAll(empty);

        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testReplaceRangeWithArray() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        long[] replacement = { 10L, 20L };

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(10L, list.get(1));
        assertEquals(20L, list.get(2));
    }

    @Test
    public void testReplaceAllWithOperator() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        list.replaceAll(x -> x * 2);
        assertEquals(2L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(10L, list.get(4));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

        assertEquals(3, list.indexOf(2L, 2));
        assertEquals(-1, list.indexOf(2L, 4));
    }

    @Test
    public void testLastIndexOfWithStart() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

        assertEquals(1, list.lastIndexOf(2L, 2));
        assertEquals(-1, list.lastIndexOf(5L, 3));
    }

    @Test
    public void testDistinct() {
        list.addAll(new long[] { 1L, 2L, 2L, 3L, 3L, 3L });

        LongList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertEquals(1L, distinct.get(0));
        assertEquals(2L, distinct.get(1));
        assertEquals(3L, distinct.get(2));
    }

    @Test
    public void testArray() {
        list.addAll(new long[] { 1L, 2L, 3L });

        long[] array = list.internalArray();
        assertEquals(1L, array[0]);
        assertEquals(2L, array[1]);
        assertEquals(3L, array[2]);

        array[0] = 10L;
        assertEquals(10L, list.get(0));
    }

    @Test
    public void testLargeList() {
        int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }

        assertEquals(size, list.size());
        assertEquals(0L, list.get(0));
        assertEquals(size - 1, list.get(size - 1));
    }

    @Test
    public void testEmptyOperations() {
        assertFalse(list.remove(1L));
        assertFalse(list.removeAllOccurrences(1L));
        assertFalse(list.removeIf(x -> true));
        assertFalse(list.removeDuplicates());
        assertFalse(list.containsDuplicates());
        assertTrue(list.isSorted());

        list.sort();
        list.reverse();
        list.shuffle();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testLongMinMaxValues() {
        list.add(Long.MIN_VALUE);
        list.add(0L);
        list.add(Long.MAX_VALUE);

        assertEquals(Long.MIN_VALUE, list.get(0));
        assertEquals(0L, list.get(1));
        assertEquals(Long.MAX_VALUE, list.get(2));

        OptionalLong min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Long.MIN_VALUE, min.getAsLong());

        OptionalLong max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Long.MAX_VALUE, max.getAsLong());
    }

    @Test
    public void testLargeValues() {
        long largeValue1 = Integer.MAX_VALUE + 1L;
        long largeValue2 = Integer.MAX_VALUE + 2L;

        list.add(largeValue1);
        list.add(largeValue2);

        assertEquals(largeValue1, list.get(0));
        assertEquals(largeValue2, list.get(1));

        assertTrue(list.contains(largeValue1));
        assertEquals(0, list.indexOf(largeValue1));
    }

    // --- Missing dedicated tests ---

    @Test
    public void testContainsDuplicates() {
        LongList list1 = LongList.of(1L, 2L, 1L);
        assertTrue(list1.containsDuplicates());

        LongList list2 = LongList.of(1L, 2L, 3L);
        assertFalse(list2.containsDuplicates());

        assertFalse(new LongList().containsDuplicates());
    }

    @Test
    public void testLastEmpty() {
        OptionalLong last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testGetFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testRemoveFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    // --- Tests for parent class PrimitiveList methods ---

    @Test
    public void testDistinctNoArg() {
        LongList original = LongList.of(3L, 1L, 2L, 1L, 3L, 2L, 4L);
        LongList distinct = original.distinct();
        assertEquals(4, distinct.size());
        assertTrue(distinct.contains(1L));
        assertTrue(distinct.contains(2L));
        assertTrue(distinct.contains(3L));
        assertTrue(distinct.contains(4L));
    }

    @Test
    public void testDistinctNoArg_Empty() {
        LongList distinct = list.distinct();
        assertTrue(distinct.isEmpty());
    }

    @Test
    public void testDistinctNoArg_NoDuplicates() {
        LongList original = LongList.of(1L, 2L, 3L);
        LongList distinct = original.distinct();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testToCollectionOneArg() {
        LongList original = LongList.of(10L, 20L, 30L);
        ArrayList<Long> result = original.toCollection(ArrayList::new);
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(10L), result.get(0));
        assertEquals(Long.valueOf(20L), result.get(1));
        assertEquals(Long.valueOf(30L), result.get(2));
    }

    @Test
    public void testToCollectionOneArg_Empty() {
        ArrayList<Long> result = list.toCollection(ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMultisetTwoArgs() {
        LongList original = LongList.of(1L, 2L, 2L, 3L, 3L, 3L);
        Multiset<Long> multiset = original.toMultiset(1, 5);
        assertEquals(2, multiset.get(Long.valueOf(2L)));
        assertEquals(2, multiset.get(Long.valueOf(3L)));
    }

    @Test
    public void testToMultisetTwoArgs_Empty() {
        LongList original = LongList.of(1L, 2L, 3L);
        Multiset<Long> multiset = original.toMultiset(1, 1);
        assertEquals(0, multiset.size());
    }

    @Test
    public void testToMultisetWithSupplier() {
        LongList original = LongList.of(1L, 2L, 2L, 3L);
        Multiset<Long> multiset = original.toMultiset(Multiset::new);
        assertEquals(1, multiset.get(Long.valueOf(1L)));
        assertEquals(2, multiset.get(Long.valueOf(2L)));
        assertEquals(1, multiset.get(Long.valueOf(3L)));
    }

    @Test
    public void testToMultisetWithSupplier_Empty() {
        Multiset<Long> multiset = list.toMultiset(Multiset::new);
        assertEquals(0, multiset.size());
    }

}
