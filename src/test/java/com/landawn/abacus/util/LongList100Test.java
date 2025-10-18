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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

@Tag("new-test")
public class LongList100Test extends TestBase {

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
    public void testConstructorWithArray() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongList list = new LongList(array);
        assertEquals(5, list.size());
        for (int i = 0; i < array.length; i++) {
            assertEquals(array[i], list.get(i));
        }
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongList list = new LongList(array, 3);
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
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
    public void testOf() {
        LongList list = LongList.of(1L, 2L, 3L, 4L, 5L);
        assertEquals(5, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(5L, list.get(4));
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
    public void testOfWithArrayAndSize() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongList list = LongList.of(array, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void testCopyOf() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongList list = LongList.copyOf(array);
        assertEquals(5, list.size());
        array[0] = 100L;
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
        LongList list = LongList.range(0L, 5L);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    public void testRangeWithStep() {
        LongList list = LongList.range(0L, 10L, 2L);
        assertEquals(5, list.size());
        assertEquals(0L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(8L, list.get(4));
    }

    @Test
    public void testRangeWithNegativeStep() {
        LongList list = LongList.range(5L, 0L, -1L);
        assertEquals(5, list.size());
        assertEquals(5L, list.get(0));
        assertEquals(1L, list.get(4));
    }

    @Test
    public void testRangeClosed() {
        LongList list = LongList.rangeClosed(1L, 5L);
        assertEquals(5, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(5L, list.get(4));
    }

    @Test
    public void testRangeClosedWithStep() {
        LongList list = LongList.rangeClosed(1L, 9L, 2L);
        assertEquals(5, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(9L, list.get(4));
    }

    @Test
    public void testRepeat() {
        LongList list = LongList.repeat(7L, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(7L, list.get(i));
        }
    }

    @Test
    public void testRandom() {
        LongList list = LongList.random(10);
        assertEquals(10, list.size());
        for (int i = 0; i < 10; i++) {
            assertNotNull(list.get(i));
        }
    }

    @Test
    public void testGet() {
        list.add(10L);
        list.add(20L);
        assertEquals(10L, list.get(0));
        assertEquals(20L, list.get(1));
    }

    @Test
    public void testGetThrowsException() {
        list.add(10L);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
    }

    @Test
    public void testSet() {
        list.add(10L);
        list.add(20L);
        long oldValue = list.set(1, 30L);
        assertEquals(20L, oldValue);
        assertEquals(30L, list.get(1));
    }

    @Test
    public void testSetThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10L));
    }

    @Test
    public void testAdd() {
        list.add(10L);
        assertEquals(1, list.size());
        assertEquals(10L, list.get(0));

        list.add(20L);
        assertEquals(2, list.size());
        assertEquals(20L, list.get(1));
    }

    @Test
    public void testAddAtIndex() {
        list.add(10L);
        list.add(30L);
        list.add(1, 20L);

        assertEquals(3, list.size());
        assertEquals(10L, list.get(0));
        assertEquals(20L, list.get(1));
        assertEquals(30L, list.get(2));
    }

    @Test
    public void testAddAtIndexThrowsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10L));
    }

    @Test
    public void testAddAll() {
        list.add(1L);
        list.add(2L);

        LongList other = LongList.of(3L, 4L, 5L);
        boolean result = list.addAll(other);

        assertTrue(result);
        assertEquals(5, list.size());
        assertEquals(3L, list.get(2));
        assertEquals(5L, list.get(4));
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
    public void testAddAllAtIndex() {
        list.add(1L);
        list.add(4L);

        LongList other = LongList.of(2L, 3L);
        boolean result = list.addAll(1, other);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
        assertEquals(4L, list.get(3));
    }

    @Test
    public void testAddAllArray() {
        list.add(1L);
        long[] array = { 2L, 3L, 4L };
        boolean result = list.addAll(array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(4L, list.get(3));
    }

    @Test
    public void testAddAllArrayAtIndex() {
        list.add(1L);
        list.add(4L);
        long[] array = { 2L, 3L };
        boolean result = list.addAll(1, array);

        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testRemove() {
        list.add(10L);
        list.add(20L);
        list.add(30L);

        boolean result = list.remove(20L);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(10L, list.get(0));
        assertEquals(30L, list.get(1));
    }

    @Test
    public void testRemoveNotFound() {
        list.add(10L);
        boolean result = list.remove(20L);
        assertFalse(result);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAllOccurrences() {
        list.add(10L);
        list.add(20L);
        list.add(10L);
        list.add(30L);
        list.add(10L);

        boolean result = list.removeAllOccurrences(10L);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(20L, list.get(0));
        assertEquals(30L, list.get(1));
    }

    @Test
    public void testRemoveAll() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList toRemove = LongList.of(2L, 4L);

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(3L, list.get(1));
        assertEquals(5L, list.get(2));
    }

    @Test
    public void testRemoveAllArray() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        long[] toRemove = { 2L, 4L };

        boolean result = list.removeAll(toRemove);
        assertTrue(result);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        boolean result = list.removeIf(x -> x % 2 == 0);
        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(3L, list.get(1));
        assertEquals(5L, list.get(2));
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(new long[] { 1L, 2L, 2L, 3L, 3L, 3L, 4L });

        boolean result = list.removeDuplicates();
        assertTrue(result);
        assertEquals(4, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
        assertEquals(4L, list.get(3));
    }

    @Test
    public void testRemoveDuplicatesNoDuplicates() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        assertFalse(list.removeDuplicates());
        assertEquals(5, list.size());
    }

    @Test
    public void testRetainAll() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList toRetain = LongList.of(2L, 4L, 6L);

        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2L, list.get(0));
        assertEquals(4L, list.get(1));
    }

    @Test
    public void testDelete() {
        list.addAll(new long[] { 10L, 20L, 30L });

        long deleted = list.delete(1);
        assertEquals(20L, deleted);
        assertEquals(2, list.size());
        assertEquals(10L, list.get(0));
        assertEquals(30L, list.get(1));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.addAll(new long[] { 10L, 20L, 30L, 40L, 50L });

        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(10L, list.get(0));
        assertEquals(30L, list.get(1));
        assertEquals(50L, list.get(2));
    }

    @Test
    public void testDeleteRange() {
        list.addAll(new long[] { 10L, 20L, 30L, 40L, 50L });

        list.deleteRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(10L, list.get(0));
        assertEquals(40L, list.get(1));
        assertEquals(50L, list.get(2));
    }

    @Test
    public void testMoveRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        list.moveRange(1, 3, 3);
        assertEquals(5, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(5L, list.get(2));
        assertEquals(2L, list.get(3));
        assertEquals(3L, list.get(4));
    }

    @Test
    public void testReplaceRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList replacement = LongList.of(10L, 20L);

        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(10L, list.get(1));
        assertEquals(20L, list.get(2));
        assertEquals(4L, list.get(3));
        assertEquals(5L, list.get(4));
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
    public void testReplaceAll() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

        int count = list.replaceAll(2L, 20L);
        assertEquals(2, count);
        assertEquals(1L, list.get(0));
        assertEquals(20L, list.get(1));
        assertEquals(3L, list.get(2));
        assertEquals(20L, list.get(3));
        assertEquals(5L, list.get(4));
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
    public void testReplaceIf() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        boolean result = list.replaceIf(x -> x % 2 == 0, 0L);
        assertTrue(result);
        assertEquals(1L, list.get(0));
        assertEquals(0L, list.get(1));
        assertEquals(3L, list.get(2));
        assertEquals(0L, list.get(3));
        assertEquals(5L, list.get(4));
    }

    @Test
    public void testFill() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        list.fill(10L);
        for (int i = 0; i < list.size(); i++) {
            assertEquals(10L, list.get(i));
        }
    }

    @Test
    public void testFillRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        list.fill(1, 4, 10L);
        assertEquals(1L, list.get(0));
        assertEquals(10L, list.get(1));
        assertEquals(10L, list.get(2));
        assertEquals(10L, list.get(3));
        assertEquals(5L, list.get(4));
    }

    @Test
    public void testContains() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        assertTrue(list.contains(3L));
        assertFalse(list.contains(6L));
    }

    @Test
    public void testContainsAny() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList other = LongList.of(6L, 7L, 3L);

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAnyArray() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        long[] other = { 6L, 7L, 3L };

        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList other = LongList.of(2L, 4L);

        assertTrue(list.containsAll(other));

        other = LongList.of(2L, 6L);
        assertFalse(list.containsAll(other));
    }

    @Test
    public void testDisjoint() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList other = LongList.of(4L, 5L, 6L);

        assertTrue(list.disjoint(other));

        other = LongList.of(3L, 4L, 5L);
        assertFalse(list.disjoint(other));
    }

    @Test
    public void testIntersection() {
        list.addAll(new long[] { 1L, 2L, 2L, 3L, 4L });
        LongList other = LongList.of(2L, 3L, 5L, 2L);

        LongList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals(2L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testDifference() {
        list.addAll(new long[] { 1L, 2L, 2L, 3L, 4L });
        LongList other = LongList.of(2L, 3L);

        LongList result = list.difference(other);
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(4L, result.get(2));
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList other = LongList.of(3L, 4L, 5L);

        LongList result = list.symmetricDifference(other);
        assertEquals(4, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(4L));
        assertTrue(result.contains(5L));
    }

    @Test
    public void testOccurrencesOf() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 2L, 4L });

        assertEquals(3, list.occurrencesOf(2L));
        assertEquals(1, list.occurrencesOf(1L));
        assertEquals(0, list.occurrencesOf(5L));
    }

    @Test
    public void testIndexOf() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

        assertEquals(1, list.indexOf(2L));
        assertEquals(2, list.indexOf(3L));
        assertEquals(-1, list.indexOf(6L));
    }

    @Test
    public void testIndexOfWithFromIndex() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

        assertEquals(3, list.indexOf(2L, 2));
        assertEquals(-1, list.indexOf(2L, 4));
    }

    @Test
    public void testLastIndexOf() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

        assertEquals(3, list.lastIndexOf(2L));
        assertEquals(0, list.lastIndexOf(1L));
        assertEquals(-1, list.lastIndexOf(6L));
    }

    @Test
    public void testLastIndexOfWithStart() {
        list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

        assertEquals(1, list.lastIndexOf(2L, 2));
        assertEquals(-1, list.lastIndexOf(5L, 3));
    }

    @Test
    public void testMin() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });

        OptionalLong min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1L, min.getAsLong());
    }

    @Test
    public void testMinEmpty() {
        OptionalLong min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMinRange() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });

        OptionalLong min = list.min(2, 4);
        assertTrue(min.isPresent());
        assertEquals(1L, min.getAsLong());
    }

    @Test
    public void testMax() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });

        OptionalLong max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5L, max.getAsLong());
    }

    @Test
    public void testMedian() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });

        OptionalLong median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3L, median.getAsLong());
    }

    @Test
    public void testForEach() {
        list.addAll(new long[] { 1L, 2L, 3L });
        List<Long> result = new ArrayList<>();

        list.forEach(result::add);

        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testForEachRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        List<Long> result = new ArrayList<>();

        list.forEach(1, 4, result::add);

        assertEquals(3, result.size());
        assertEquals(2L, result.get(0));
        assertEquals(3L, result.get(1));
        assertEquals(4L, result.get(2));
    }

    @Test
    public void testFirst() {
        list.addAll(new long[] { 1L, 2L, 3L });

        OptionalLong first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1L, first.getAsLong());
    }

    @Test
    public void testFirstEmpty() {
        OptionalLong first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.addAll(new long[] { 1L, 2L, 3L });

        OptionalLong last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3L, last.getAsLong());
    }

    @Test
    public void testGetFirst() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertEquals(1L, list.getFirst());
    }

    @Test
    public void testGetFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertEquals(3L, list.getLast());
    }

    @Test
    public void testAddFirst() {
        list.addAll(new long[] { 2L, 3L });
        list.addFirst(1L);

        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
    }

    @Test
    public void testAddLast() {
        list.addAll(new long[] { 1L, 2L });
        list.addLast(3L);

        assertEquals(3, list.size());
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testRemoveFirst() {
        list.addAll(new long[] { 1L, 2L, 3L });

        long removed = list.removeFirst();
        assertEquals(1L, removed);
        assertEquals(2, list.size());
        assertEquals(2L, list.get(0));
    }

    @Test
    public void testRemoveFirstThrowsException() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.addAll(new long[] { 1L, 2L, 3L });

        long removed = list.removeLast();
        assertEquals(3L, removed);
        assertEquals(2, list.size());
        assertEquals(2L, list.get(1));
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
    public void testHasDuplicates() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertFalse(list.hasDuplicates());

        list.add(2L);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        assertTrue(list.isSorted());

        list.set(2, 1L);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSort() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });

        list.sort();
        assertTrue(list.isSorted());
        assertEquals(1L, list.get(0));
        assertEquals(1L, list.get(1));
        assertEquals(5L, list.get(4));
    }

    @Test
    public void testParallelSort() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });

        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testReverseSort() {
        list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });

        list.reverseSort();
        assertEquals(5L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(1L, list.get(4));
    }

    @Test
    public void testBinarySearch() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        assertEquals(2, list.binarySearch(3L));
        assertTrue(list.binarySearch(6L) < 0);
    }

    @Test
    public void testBinarySearchRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        assertEquals(3, list.binarySearch(1, 5, 4L));
        assertTrue(list.binarySearch(1, 3, 4L) < 0);
    }

    @Test
    public void testReverse() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        list.reverse();
        assertEquals(5L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(1L, list.get(4));
    }

    @Test
    public void testReverseRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        list.reverse(1, 4);
        assertEquals(1L, list.get(0));
        assertEquals(4L, list.get(1));
        assertEquals(3L, list.get(2));
        assertEquals(2L, list.get(3));
        assertEquals(5L, list.get(4));
    }

    @Test
    public void testRotate() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        list.rotate(2);
        assertEquals(4L, list.get(0));
        assertEquals(5L, list.get(1));
        assertEquals(1L, list.get(2));
    }

    @Test
    public void testShuffle() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList original = list.copy();

        list.shuffle();
        assertEquals(original.size(), list.size());

        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }
    }

    @Test
    public void testShuffleWithRandom() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        Random rnd = new Random(42);

        list.shuffle(rnd);
        assertEquals(5, list.size());
    }

    @Test
    public void testSwap() {
        list.addAll(new long[] { 1L, 2L, 3L });

        list.swap(0, 2);
        assertEquals(3L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(1L, list.get(2));
    }

    @Test
    public void testCopy() {
        list.addAll(new long[] { 1L, 2L, 3L });

        LongList copy = list.copy();
        assertEquals(list.size(), copy.size());

        list.set(0, 10L);
        assertEquals(1L, copy.get(0));
    }

    @Test
    public void testCopyRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        LongList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals(2L, copy.get(0));
        assertEquals(4L, copy.get(2));
    }

    @Test
    public void testCopyWithStep() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        LongList copy = list.copy(0, 5, 2);
        assertEquals(3, copy.size());
        assertEquals(1L, copy.get(0));
        assertEquals(3L, copy.get(1));
        assertEquals(5L, copy.get(2));
    }

    @Test
    public void testSplit() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L, 6L });

        List<LongList> chunks = list.split(0, 6, 2);
        assertEquals(3, chunks.size());

        assertEquals(2, chunks.get(0).size());
        assertEquals(1L, chunks.get(0).get(0));
        assertEquals(2L, chunks.get(0).get(1));

        assertEquals(2, chunks.get(2).size());
        assertEquals(5L, chunks.get(2).get(0));
        assertEquals(6L, chunks.get(2).get(1));
    }

    @Test
    public void testTrimToSize() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.trimToSize();
        assertEquals(3, list.size());
    }

    @Test
    public void testClear() {
        list.addAll(new long[] { 1L, 2L, 3L });

        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add(1L);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add(1L);
        assertEquals(1, list.size());

        list.add(2L);
        assertEquals(2, list.size());
    }

    @Test
    public void testBoxed() {
        list.addAll(new long[] { 1L, 2L, 3L });

        List<Long> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Long.valueOf(1L), boxed.get(0));
        assertEquals(Long.valueOf(3L), boxed.get(2));
    }

    @Test
    public void testBoxedRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        List<Long> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Long.valueOf(2L), boxed.get(0));
        assertEquals(Long.valueOf(4L), boxed.get(2));
    }

    @Test
    public void testToArray() {
        list.addAll(new long[] { 1L, 2L, 3L });

        long[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(1L, array[0]);
        assertEquals(3L, array[2]);

        array[0] = 10L;
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testToFloatList() {
        list.addAll(new long[] { 1L, 2L, 3L });

        FloatList floatList = list.toFloatList();
        assertEquals(3, floatList.size());
        assertEquals(1.0f, floatList.get(0));
        assertEquals(3.0f, floatList.get(2));
    }

    @Test
    public void testToDoubleList() {
        list.addAll(new long[] { 1L, 2L, 3L });

        DoubleList doubleList = list.toDoubleList();
        assertEquals(3, doubleList.size());
        assertEquals(1.0, doubleList.get(0));
        assertEquals(3.0, doubleList.get(2));
    }

    @Test
    public void testToCollection() {
        list.addAll(new long[] { 1L, 2L, 3L });

        Set<Long> set = list.toCollection(0, 3, size -> new HashSet<>());
        assertEquals(3, set.size());
        assertTrue(set.contains(1L));
        assertTrue(set.contains(3L));
    }

    @Test
    public void testToMultiset() {
        list.addAll(new long[] { 1L, 2L, 2L, 3L });

        Multiset<Long> multiset = list.toMultiset(0, 4, size -> new Multiset<>());
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(2L));
    }

    @Test
    public void testIterator() {
        list.addAll(new long[] { 1L, 2L, 3L });

        LongIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertEquals(2L, iter.nextLong());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        LongStream stream = list.stream();
        assertNotNull(stream);

        long sum = stream.sum();
        assertEquals(15L, sum);
    }

    @Test
    public void testStreamRange() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        LongStream stream = list.stream(1, 4);
        long sum = stream.sum();
        assertEquals(9L, sum);
    }

    @Test
    public void testEquals() {
        list.addAll(new long[] { 1L, 2L, 3L });

        LongList other = LongList.of(1L, 2L, 3L);
        assertTrue(list.equals(other));

        other.add(4L);
        assertFalse(list.equals(other));

        assertFalse(list.equals(null));
        assertFalse(list.equals("not a list"));
        assertTrue(list.equals(list));
    }

    @Test
    public void testHashCode() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList other = LongList.of(1L, 2L, 3L);

        assertEquals(list.hashCode(), other.hashCode());

        other.add(4L);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertTrue(list.toString().contains("1"));
        assertTrue(list.toString().contains("2"));
        assertTrue(list.toString().contains("3"));
    }

    @Test
    public void testArray() {
        list.addAll(new long[] { 1L, 2L, 3L });

        long[] array = list.array();
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
        assertFalse(list.hasDuplicates());
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
}
