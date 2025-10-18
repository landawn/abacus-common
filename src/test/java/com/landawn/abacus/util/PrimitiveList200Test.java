package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class PrimitiveList200Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        IntList list = new IntList();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        IntList list = new IntList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertEquals(10, list.array().length);
    }

    @Test
    public void testConstructorWithArray() {
        int[] a = { 1, 2, 3 };
        IntList list = new IntList(a);
        assertEquals(3, list.size());
        assertArrayEquals(a, list.toArray());
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = new IntList(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(a, 6));
    }

    @Test
    public void testOf() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        IntList emptyList = IntList.of();
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testOfArrayAndSize() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = IntList.of(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(a, 6));
    }

    @Test
    public void testCopyOf() {
        int[] a = { 1, 2, 3 };
        IntList list = IntList.copyOf(a);
        assertEquals(3, list.size());
        assertArrayEquals(a, list.toArray());
        a[0] = 99;
        assertNotEquals(99, list.get(0));
    }

    @Test
    public void testCopyOfRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(a, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRange() {
        IntList list = IntList.range(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRangeWithStep() {
        IntList list = IntList.range(1, 10, 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
    }

    @Test
    public void testRangeClosed() {
        IntList list = IntList.rangeClosed(1, 5);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, list.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        IntList list = IntList.rangeClosed(1, 10, 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
    }

    @Test
    public void testRepeat() {
        IntList list = IntList.repeat(5, 3);
        assertArrayEquals(new int[] { 5, 5, 5 }, list.toArray());
    }

    @Test
    public void testRandom() {
        IntList list = IntList.random(10);
        assertEquals(10, list.size());
    }

    @Test
    public void testRandomWithRange() {
        IntList list = IntList.random(1, 10, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 1 && list.get(i) < 10);
        }
        assertThrows(IllegalArgumentException.class, () -> IntList.random(10, 1, 5));
    }

    @Test
    public void testArray() {
        int[] a = { 1, 2, 3 };
        IntList list = new IntList(a);
        assertSame(a, list.array());
    }

    @Test
    public void testGet() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    public void testSet() {
        IntList list = IntList.of(1, 2, 3);
        int oldVal = list.set(1, 99);
        assertEquals(2, oldVal);
        assertArrayEquals(new int[] { 1, 99, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 100));
    }

    @Test
    public void testAdd() {
        IntList list = new IntList();
        list.add(10);
        list.add(20);
        assertEquals(2, list.size());
        assertArrayEquals(new int[] { 10, 20 }, list.toArray());
    }

    @Test
    public void testAddAtIndex() {
        IntList list = IntList.of(10, 30);
        list.add(1, 20);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 10, 20, 30 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 40));
    }

    @Test
    public void testAddAll() {
        IntList list = IntList.of(1, 2);
        IntList toAdd = IntList.of(3, 4);
        list.addAll(toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAllAtIndex() {
        IntList list = IntList.of(1, 4);
        IntList toAdd = IntList.of(2, 3);
        list.addAll(1, toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAllArray() {
        IntList list = IntList.of(1, 2);
        int[] toAdd = { 3, 4 };
        list.addAll(toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAllArrayAtIndex() {
        IntList list = IntList.of(1, 4);
        int[] toAdd = { 2, 3 };
        list.addAll(1, toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRemove() {
        IntList list = IntList.of(1, 2, 3, 2);
        assertTrue(list.remove(2));
        assertArrayEquals(new int[] { 1, 3, 2 }, list.toArray());
        assertFalse(list.remove(99));
    }

    @Test
    public void testRemoveAllOccurrences() {
        IntList list = IntList.of(1, 2, 3, 2, 4, 2);
        assertTrue(list.removeAllOccurrences(2));
        assertArrayEquals(new int[] { 1, 3, 4 }, list.toArray());
        assertFalse(list.removeAllOccurrences(99));
    }

    @Test
    public void testRemoveAllIntList() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 2);
        IntList toRemove = IntList.of(2, 4);
        assertTrue(list.removeAll(toRemove));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveAllArray() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 2);
        int[] toRemove = { 2, 4 };
        assertTrue(list.removeAll(toRemove));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntPredicate predicate = (n) -> n % 2 == 0;
        assertTrue(list.removeIf(predicate));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveDuplicates() {
        IntList list = IntList.of(1, 2, 2, 3, 1, 4, 4);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
        IntList noDuplicates = IntList.of(1, 2, 3);
        assertFalse(noDuplicates.removeDuplicates());
    }

    @Test
    public void testRetainAll() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        IntList toRetain = IntList.of(2, 4, 5);
        assertTrue(list.retainAll(toRetain));
        assertArrayEquals(new int[] { 2, 2, 4 }, list.toArray());
    }

    @Test
    public void testRetainAllArray() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        int[] toRetain = { 2, 4, 5 };
        assertTrue(list.retainAll(toRetain));
        assertArrayEquals(new int[] { 2, 2, 4 }, list.toArray());
    }

    @Test
    public void testDelete() {
        IntList list = IntList.of(1, 2, 3);
        int deleted = list.delete(1);
        assertEquals(2, deleted);
        assertArrayEquals(new int[] { 1, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
    }

    @Test
    public void testDeleteAllByIndices() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.deleteAllByIndices(1, 3, 5);
        assertArrayEquals(new int[] { 0, 2, 4 }, list.toArray());
    }

    @Test
    public void testDeleteRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.deleteRange(1, 4);
        assertArrayEquals(new int[] { 0, 4, 5 }, list.toArray());
    }

    @Test
    public void testMoveRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.moveRange(1, 3, 4);
        assertArrayEquals(new int[] { 0, 3, 4, 5, 1, 2 }, list.toArray());
    }

    @Test
    public void testReplaceRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        IntList replacement = IntList.of(9, 8, 7);
        list.replaceRange(1, 4, replacement);
        assertArrayEquals(new int[] { 0, 9, 8, 7, 4, 5 }, list.toArray());
    }

    @Test
    public void testReplaceRangeArray() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        int[] replacement = { 9, 8, 7 };
        list.replaceRange(1, 4, replacement);
        assertArrayEquals(new int[] { 0, 9, 8, 7, 4, 5 }, list.toArray());
    }

    @Test
    public void testReplaceAll() {
        IntList list = IntList.of(1, 2, 1, 3, 1);
        int count = list.replaceAll(1, 99);
        assertEquals(3, count);
        assertArrayEquals(new int[] { 99, 2, 99, 3, 99 }, list.toArray());
    }

    @Test
    public void testReplaceAllOperator() {
        IntList list = IntList.of(1, 2, 3, 4);
        IntUnaryOperator operator = (n) -> n * 2;
        list.replaceAll(operator);
        assertArrayEquals(new int[] { 2, 4, 6, 8 }, list.toArray());
    }

    @Test
    public void testReplaceIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntPredicate predicate = (n) -> n > 3;
        assertTrue(list.replaceIf(predicate, 99));
        assertArrayEquals(new int[] { 1, 2, 3, 99, 99 }, list.toArray());
    }

    @Test
    public void testFill() {
        IntList list = IntList.of(1, 2, 3, 4);
        list.fill(99);
        assertArrayEquals(new int[] { 99, 99, 99, 99 }, list.toArray());
    }

    @Test
    public void testFillRange() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.fill(1, 4, 99);
        assertArrayEquals(new int[] { 1, 99, 99, 99, 5 }, list.toArray());
    }

    @Test
    public void testContains() {
        IntList list = IntList.of(1, 2, 3);
        assertTrue(list.contains(2));
        assertFalse(list.contains(4));
    }

    @Test
    public void testContainsAny() {
        IntList list = IntList.of(1, 2, 3);
        IntList any1 = IntList.of(4, 5, 2);
        assertTrue(list.containsAny(any1));
        IntList any2 = IntList.of(4, 5, 6);
        assertFalse(list.containsAny(any2));
    }

    @Test
    public void testContainsAll() {
        IntList list = IntList.of(1, 2, 3, 4);
        IntList all1 = IntList.of(2, 4);
        assertTrue(list.containsAll(all1));
        IntList all2 = IntList.of(2, 5);
        assertFalse(list.containsAll(all2));
    }

    @Test
    public void testDisjoint() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(4, 5, 6);
        assertTrue(list1.disjoint(list2));
        IntList list3 = IntList.of(3, 4, 5);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void testIntersection() {
        IntList list1 = IntList.of(1, 2, 2, 3);
        IntList list2 = IntList.of(2, 3, 4, 2);
        IntList intersection = list1.intersection(list2);
        assertArrayEquals(new int[] { 2, 2, 3 }, intersection.toArray());
    }

    @Test
    public void testDifference() {
        IntList list1 = IntList.of(1, 2, 2, 3, 4);
        IntList list2 = IntList.of(2, 3, 5);
        IntList difference = list1.difference(list2);
        assertArrayEquals(new int[] { 1, 2, 4 }, difference.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        IntList list1 = IntList.of(1, 2, 2, 3);
        IntList list2 = IntList.of(2, 3, 4);
        IntList symmetricDifference = list1.symmetricDifference(list2);
        symmetricDifference.sort();
        assertArrayEquals(new int[] { 1, 2, 4 }, symmetricDifference.toArray());
    }

    @Test
    public void testOccurrencesOf() {
        IntList list = IntList.of(1, 2, 1, 3, 1, 2);
        assertEquals(3, list.occurrencesOf(1));
        assertEquals(2, list.occurrencesOf(2));
        assertEquals(0, list.occurrencesOf(4));
    }

    @Test
    public void testIndexOf() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(1, list.indexOf(2));
        assertEquals(0, list.indexOf(1));
        assertEquals(-1, list.indexOf(4));
    }

    @Test
    public void testIndexOfFromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(3, list.indexOf(2, 2));
    }

    @Test
    public void testLastIndexOf() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(3, list.lastIndexOf(2));
        assertEquals(4, list.lastIndexOf(1));
        assertEquals(-1, list.lastIndexOf(4));
    }

    @Test
    public void testLastIndexOfFromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(1, list.lastIndexOf(2, 2));
    }

    @Test
    public void testMin() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        assertEquals(1, list.min().getAsInt());
        assertTrue(new IntList().min().isEmpty());
    }

    @Test
    public void testMax() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        assertEquals(9, list.max().getAsInt());
    }

    @Test
    public void testMedian() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9, 2, 6);
        assertEquals(3, list.median().getAsInt());
    }

    @Test
    public void testForEach() {
        IntList list = IntList.of(1, 2, 3);
        final int[] sum = { 0 };
        IntConsumer consumer = (n) -> sum[0] += n;
        list.forEach(consumer);
        assertEquals(6, sum[0]);
    }

    @Test
    public void testFirst() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(1, list.first().getAsInt());
        assertTrue(new IntList().first().isEmpty());
    }

    @Test
    public void testLast() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(3, list.last().getAsInt());
        assertTrue(new IntList().last().isEmpty());
    }

    @Test
    public void testDistinct() {
        IntList list = IntList.of(1, 2, 1, 3, 2, 4);
        IntList distinctList = list.distinct();
        distinctList.sort();
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, distinctList.toArray());
    }

    @Test
    public void testHasDuplicates() {
        IntList listWithDuplicates = IntList.of(1, 2, 1, 3);
        assertTrue(listWithDuplicates.hasDuplicates());
        IntList listWithoutDuplicates = IntList.of(1, 2, 3, 4);
        assertFalse(listWithoutDuplicates.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        IntList sortedList = IntList.of(1, 2, 3, 4);
        assertTrue(sortedList.isSorted());
        IntList unsortedList = IntList.of(1, 3, 2, 4);
        assertFalse(unsortedList.isSorted());
    }

    @Test
    public void testSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.sort();
        assertArrayEquals(new int[] { 1, 1, 3, 4, 5, 9 }, list.toArray());
    }

    @Test
    public void testParallelSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.parallelSort();
        assertArrayEquals(new int[] { 1, 1, 3, 4, 5, 9 }, list.toArray());
    }

    @Test
    public void testReverseSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.reverseSort();
        assertArrayEquals(new int[] { 9, 5, 4, 3, 1, 1 }, list.toArray());
    }

    @Test
    public void testBinarySearch() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertEquals(2, list.binarySearch(3));
        assertTrue(list.binarySearch(6) < 0);
    }

    @Test
    public void testReverse() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.reverse();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, list.toArray());
    }

    @Test
    public void testRotate() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.rotate(2);
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testShuffle() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5);
        IntList list2 = list1.copy();
        list1.shuffle();
        assertFalse(Arrays.equals(list1.toArray(), list2.toArray()) && list1.size() > 1);
    }

    @Test
    public void testShuffleWithRandom() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntList list2 = list1.copy();
        list1.shuffle(new Random(123));
        list2.shuffle(new Random(123));
        assertArrayEquals(list1.toArray(), list2.toArray());
    }

    @Test
    public void testSwap() {
        IntList list = IntList.of(1, 2, 3, 4);
        list.swap(1, 3);
        assertArrayEquals(new int[] { 1, 4, 3, 2 }, list.toArray());
    }

    @Test
    public void testCopy() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = list1.copy();
        assertArrayEquals(list1.toArray(), list2.toArray());
        assertNotSame(list1, list2);
    }

    @Test
    public void testSplit() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7);
        List<IntList> chunks = list.split(3);
        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0).toArray());
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1).toArray());
        assertArrayEquals(new int[] { 7 }, chunks.get(2).toArray());
    }

    @Test
    public void testTrimToSize() {
        IntList list = new IntList(10);
        list.add(1);
        list.add(2);
        list.add(3);
        list.trimToSize();
        assertEquals(3, list.array().length);
    }

    @Test
    public void testClear() {
        IntList list = IntList.of(1, 2, 3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testBoxed() {
        IntList list = IntList.of(1, 2, 3);
        List<Integer> boxedList = list.boxed();
        assertEquals(3, boxedList.size());
        assertEquals(1, boxedList.get(0));
        assertEquals(2, boxedList.get(1));
        assertEquals(3, boxedList.get(2));
    }

    @Test
    public void testToArray() {
        int[] a = { 1, 2, 3 };
        IntList list = IntList.of(a);
        assertArrayEquals(a, list.toArray());
        assertNotSame(a, list.toArray());
    }

    @Test
    public void testGetFirstAndLast() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(1, list.getFirst());
        assertEquals(3, list.getLast());
        IntList emptyList = new IntList();
        assertThrows(NoSuchElementException.class, () -> emptyList.getFirst());
        assertThrows(NoSuchElementException.class, () -> emptyList.getLast());
    }

    @Test
    public void testAddFirstAndLast() {
        IntList list = IntList.of(2, 3);
        list.addFirst(1);
        list.addLast(4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRemoveFirstAndLast() {
        IntList list = IntList.of(1, 2, 3, 4);
        assertEquals(1, list.removeFirst());
        assertEquals(4, list.removeLast());
        assertArrayEquals(new int[] { 2, 3 }, list.toArray());
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
    public void testToString() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals("[1, 2, 3]", list.toString());
        IntList emptyList = new IntList();
        assertEquals("[]", emptyList.toString());
    }
}
