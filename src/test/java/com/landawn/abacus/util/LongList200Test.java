package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.LongConsumer;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class LongList200Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        LongList list = new LongList();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        LongList list = new LongList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertTrue(list.array().length >= 10 || list.array().length == 0);
    }

    @Test
    public void testConstructorWithArray() {
        long[] a = { 1L, 2L, 3L };
        LongList list = new LongList(a);
        assertEquals(3, list.size());
        assertArrayEquals(a, list.toArray());
        assertSame(a, list.array());
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        LongList list = new LongList(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> new LongList(a, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> new LongList(a, -1));
    }

    @Test
    public void testOf() {
        LongList list = LongList.of(1L, 2L, 3L);
        assertEquals(3, list.size());
        assertArrayEquals(new long[] { 1L, 2L, 3L }, list.toArray());

        LongList emptyList = LongList.of();
        assertEquals(0, emptyList.size());
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
    public void testCopyOf() {
        long[] a = { 1L, 2L, 3L };
        LongList list = LongList.copyOf(a);
        assertEquals(3, list.size());
        assertArrayEquals(a, list.toArray());
        a[0] = 99L;
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testCopyOfRange() {
        long[] a = { 1L, 2L, 3L, 4L, 5L };
        LongList list = LongList.copyOf(a, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new long[] { 2L, 3L, 4L }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.copyOf(a, 0, 6));
    }

    @Test
    public void testRange() {
        LongList list = LongList.range(1L, 5L);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
    }

    @Test
    public void testRangeWithStep() {
        LongList list = LongList.range(1L, 10L, 2L);
        assertArrayEquals(new long[] { 1L, 3L, 5L, 7L, 9L }, list.toArray());
    }

    @Test
    public void testRangeClosed() {
        LongList list = LongList.rangeClosed(1L, 5L);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, list.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        LongList list = LongList.rangeClosed(1L, 10L, 2L);
        assertArrayEquals(new long[] { 1L, 3L, 5L, 7L, 9L }, list.toArray());
    }

    @Test
    public void testRepeat() {
        LongList list = LongList.repeat(5L, 4);
        assertArrayEquals(new long[] { 5L, 5L, 5L, 5L }, list.toArray());
    }

    @Test
    public void testRandom() {
        LongList list = LongList.random(10);
        assertEquals(10, list.size());
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
    public void testAddAllArray() {
        LongList list = LongList.of(1L, 2L);
        long[] toAdd = { 3L, 4L };
        assertTrue(list.addAll(toAdd));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
    }

    @Test
    public void testAddAllArrayAtIndex() {
        LongList list = LongList.of(1L, 4L);
        long[] toAdd = { 2L, 3L };
        assertTrue(list.addAll(1, toAdd));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
    }

    @Test
    public void testRemove() {
        LongList list = LongList.of(1L, 2L, 3L, 2L);
        assertTrue(list.remove(2L));
        assertArrayEquals(new long[] { 1L, 3L, 2L }, list.toArray());
        assertFalse(list.remove(99L));
    }

    @Test
    public void testRemoveAllOccurrences() {
        LongList list = LongList.of(1L, 2L, 3L, 2L, 4L, 2L);
        assertTrue(list.removeAllOccurrences(2L));
        assertArrayEquals(new long[] { 1L, 3L, 4L }, list.toArray());
        assertFalse(list.removeAllOccurrences(99L));
    }

    @Test
    public void testRemoveAll() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L, 2L);
        LongList toRemove = LongList.of(2L, 4L, 6L);
        assertTrue(list.removeAll(toRemove));
        assertArrayEquals(new long[] { 1L, 3L, 5L }, list.toArray());
    }

    @Test
    public void testRemoveIf() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L, 6L);
        LongPredicate isEven = n -> n % 2 == 0;
        assertTrue(list.removeIf(isEven));
        assertArrayEquals(new long[] { 1L, 3L, 5L }, list.toArray());
        assertFalse(list.removeIf(isEven));
    }

    @Test
    public void testRemoveDuplicates() {
        LongList list = LongList.of(1L, 2L, 2L, 3L, 1L, 4L, 4L);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRetainAll() {
        LongList list = LongList.of(1L, 2L, 3L, 2L, 4L);
        LongList toRetain = LongList.of(2L, 4L, 5L);
        assertTrue(list.retainAll(toRetain));
        assertArrayEquals(new long[] { 2L, 2L, 4L }, list.toArray());
    }

    @Test
    public void testDelete() {
        LongList list = LongList.of(10L, 20L, 30L);
        long deleted = list.delete(1);
        assertEquals(20L, deleted);
        assertArrayEquals(new long[] { 10L, 30L }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
    }

    @Test
    public void testDeleteAllByIndices() {
        LongList list = LongList.of(0L, 1L, 2L, 3L, 4L, 5L);
        list.deleteAllByIndices(1, 3, 5);
        assertArrayEquals(new long[] { 0L, 2L, 4L }, list.toArray());
        list.deleteAllByIndices();
        assertArrayEquals(new long[] { 0L, 2L, 4L }, list.toArray());
    }

    @Test
    public void testDeleteRange() {
        LongList list = LongList.of(0L, 1L, 2L, 3L, 4L, 5L);
        list.deleteRange(1, 4);
        assertArrayEquals(new long[] { 0L, 4L, 5L }, list.toArray());
        list.deleteRange(2, 2);
        assertArrayEquals(new long[] { 0L, 4L, 5L }, list.toArray());
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
    public void testReplaceAllOperator() {
        LongList list = LongList.of(1L, 2L, 3L, 4L);
        LongUnaryOperator operator = n -> n * 10L;
        list.replaceAll(operator);
        assertArrayEquals(new long[] { 10L, 20L, 30L, 40L }, list.toArray());
    }

    @Test
    public void testReplaceIf() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L);
        LongPredicate predicate = n -> n > 3L;
        assertTrue(list.replaceIf(predicate, 100L));
        assertArrayEquals(new long[] { 1L, 2L, 3L, 100L, 100L }, list.toArray());
        assertFalse(list.replaceIf(n -> n < 0, 0L));
    }

    @Test
    public void testFill() {
        LongList list = LongList.of(1L, 2L, 3L, 4L);
        list.fill(0L);
        assertArrayEquals(new long[] { 0L, 0L, 0L, 0L }, list.toArray());
        list.fill(1, 3, 88L);
        assertArrayEquals(new long[] { 0L, 88L, 88L, 0L }, list.toArray());
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
    public void testOccurrencesOf() {
        LongList list = LongList.of(1L, 2L, 1L, 3L, 1L, 2L);
        assertEquals(3, list.occurrencesOf(1L));
        assertEquals(0, list.occurrencesOf(4L));
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
    public void testForEach() {
        LongList list = LongList.of(1L, 2L, 3L);
        final long[] sum = { 0 };
        LongConsumer consumer = n -> sum[0] += n;
        list.forEach(consumer);
        assertEquals(6L, sum[0]);
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
    public void testHasDuplicates() {
        assertTrue(LongList.of(1L, 2L, 1L).hasDuplicates());
        assertFalse(LongList.of(1L, 2L, 3L).hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        assertTrue(LongList.of(1L, 2L, 3L, 3L).isSorted());
        assertFalse(LongList.of(1L, 3L, 2L).isSorted());
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
    public void testBinarySearch() {
        LongList list = LongList.of(11L, 22L, 33L, 44L, 55L);
        assertEquals(2, list.binarySearch(33L));
        assertTrue(list.binarySearch(30L) < 0);
    }

    @Test
    public void testReverse() {
        LongList list = LongList.of(1L, 2L, 3L, 4L);
        list.reverse();
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, list.toArray());
        list.reverse(1, 3);
        assertArrayEquals(new long[] { 4L, 2L, 3L, 1L }, list.toArray());
    }

    @Test
    public void testRotate() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L);
        list.rotate(2);
        assertArrayEquals(new long[] { 4L, 5L, 1L, 2L, 3L }, list.toArray());
        list.rotate(-2);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, list.toArray());
    }

    @Test
    public void testShuffle() {
        LongList sortedList = LongList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L);
        LongList shuffledList = sortedList.copy();
        shuffledList.shuffle(new Random(123));
        assertFalse(Arrays.equals(sortedList.toArray(), shuffledList.toArray()));
    }

    @Test
    public void testSwap() {
        LongList list = LongList.of(10L, 20L, 30L, 40L);
        list.swap(1, 3);
        assertArrayEquals(new long[] { 10L, 40L, 30L, 20L }, list.toArray());
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
        assertEquals(10, list.array().length);
        list.trimToSize();
        assertEquals(2, list.array().length);
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
    public void testToString() {
        LongList list = LongList.of(1L, 2L, 3L);
        assertEquals("[1, 2, 3]", list.toString());
        assertEquals("[]", new LongList().toString());
    }

    @Test
    public void testToCollection() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L);
        ArrayList<Long> result = list.toCollection(1, 4, ArrayList::new);
        assertEquals(Arrays.asList(2L, 3L, 4L), result);
    }

    @Test
    public void testToMultiset() {
        LongList list = LongList.of(1L, 2L, 2L, 3L, 1L);
        Multiset<Long> multiset = list.toMultiset();
        assertEquals(2, multiset.count(1L));
        assertEquals(2, multiset.count(2L));
        assertEquals(1, multiset.count(3L));
    }
}
