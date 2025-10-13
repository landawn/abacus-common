package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("new-test")
public class ShortList200Test extends TestBase {

    @Test
    @DisplayName("Test constructors and static factories")
    public void testConstructorsAndFactories() {
        ShortList list1 = new ShortList();
        assertTrue(list1.isEmpty());

        ShortList list2 = new ShortList(20);
        assertTrue(list2.isEmpty());
        assertEquals(20, list2.array().length);

        short[] data = { 1, 2, 3 };
        ShortList list3 = new ShortList(data);
        assertEquals(3, list3.size());
        data[0] = 5;
        assertEquals((short) 5, list3.get(0));

        ShortList list4 = ShortList.of((short) 1, (short) 2);
        assertArrayEquals(new short[] { 1, 2 }, list4.toArray());

        short[] original = { 10, 20 };
        ShortList list5 = ShortList.copyOf(original);
        original[0] = 15;
        assertEquals((short) 10, list5.get(0));

        ShortList list6 = ShortList.copyOf(new short[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 4 }, list6.toArray());
    }

    @Test
    @DisplayName("Test range, repeat, and random factories")
    public void testRangeRepeatRandom() {
        assertArrayEquals(new short[] { 5, 6, 7 }, ShortList.range((short) 5, (short) 8).toArray());
        assertArrayEquals(new short[] { 5, 6, 7, 8 }, ShortList.rangeClosed((short) 5, (short) 8).toArray());
        assertArrayEquals(new short[] { 0, 3, 6 }, ShortList.rangeClosed((short) 0, (short) 8, (short) 3).toArray());
        assertArrayEquals(new short[] { 7, 7, 7 }, ShortList.repeat((short) 7, 3).toArray());
        assertEquals(10, ShortList.random(10).size());
    }

    @Test
    @DisplayName("Test add, get, set, addFirst, addLast methods")
    public void testAddGetSet() {
        ShortList list = new ShortList();
        list.add((short) 5);
        list.add(0, (short) 1);
        list.addFirst((short) 0);
        list.addLast((short) 10);
        assertArrayEquals(new short[] { 0, 1, 5, 10 }, list.toArray());
        assertEquals((short) 1, list.get(1));

        short oldValue = list.set(1, (short) 99);
        assertEquals((short) 1, oldValue);
        assertEquals((short) 99, list.get(1));
    }

    @Test
    @DisplayName("Test adding all elements at a specific index")
    public void testAddAllAtIndex() {
        ShortList list = ShortList.of((short) 1, (short) 5);
        list.addAll(1, new short[] { 2, 3, 4 });
        assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test remove, removeAll, retainAll, and removeIf methods")
    public void testRemoveMethods() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 4);

        assertTrue(list.remove((short) 2));
        assertArrayEquals(new short[] { 1, 3, 2, 4 }, list.toArray());

        assertTrue(list.removeAll(ShortList.of((short) 1, (short) 4)));
        assertArrayEquals(new short[] { 3, 2 }, list.toArray());

        list.addAll(ShortList.of((short) 5, (short) 6));
        assertTrue(list.retainAll(new short[] { 2, 6, 7 }));
        assertArrayEquals(new short[] { 2, 6 }, list.toArray());

        assertTrue(list.removeIf(val -> val > 5));
        assertArrayEquals(new short[] { 2 }, list.toArray());
    }

    @Test
    @DisplayName("Test removeDuplicates and removeAllOccurrences")
    public void testRemoveDuplicatesAndOccurrences() {
        ShortList list = ShortList.of((short) 1, (short) 5, (short) 1, (short) 2, (short) 5);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new short[] { 1, 5, 2 }, list.toArray());

        ShortList list2 = ShortList.of((short) 1, (short) 5, (short) 1, (short) 2, (short) 5);
        assertTrue(list2.removeAllOccurrences((short) 1));
        assertArrayEquals(new short[] { 5, 2, 5 }, list2.toArray());
    }

    @Test
    @DisplayName("Test deletion by index and range")
    public void testDeleteMethods() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);

        short deleted = list.delete(1);
        assertEquals((short) 2, deleted);
        assertArrayEquals(new short[] { 1, 3, 4, 5, 6 }, list.toArray());

        list.deleteRange(1, 3);
        assertArrayEquals(new short[] { 1, 5, 6 }, list.toArray());

        list.deleteAllByIndices(0, 2);
        assertArrayEquals(new short[] { 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test moveRange and replaceRange methods")
    public void testMoveAndReplaceRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.moveRange(1, 3, 3);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 3 }, list.toArray());

        list.replaceRange(0, 2, ShortList.of((short) 9, (short) 8, (short) 7));
        assertArrayEquals(new short[] { 9, 8, 7, 5, 2, 3 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'contains', 'containsAny', 'containsAll', and 'disjoint' methods")
    public void testQueryMethods() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30, (short) 10);
        assertTrue(list.contains((short) 20));
        assertEquals(0, list.indexOf((short) 10));
        assertEquals(3, list.lastIndexOf((short) 10));

        assertTrue(list.containsAny(new short[] { 5, 15, 20 }));
        assertFalse(list.containsAny(new short[] { 5, 15, 25 }));

        assertTrue(list.containsAll(ShortList.of((short) 10, (short) 30)));
        assertFalse(list.containsAll(ShortList.of((short) 10, (short) 40)));

        assertTrue(list.disjoint(ShortList.of((short) 1, (short) 2)));
        assertFalse(list.disjoint(new short[] { 15, 25, 30 }));
    }

    @Test
    @DisplayName("Test statistical methods with ranges")
    public void testStatsWithRange() {
        ShortList list = ShortList.of((short) 9, (short) 2, (short) 7, (short) 5, (short) 2);
        assertEquals(OptionalShort.of((short) 2), list.min(1, 4));
        assertEquals(OptionalShort.of((short) 7), list.max(1, 4));
        assertEquals(OptionalShort.of((short) 5), list.median(1, 4));
        assertEquals(2, list.occurrencesOf((short) 2));
    }

    @Test
    @DisplayName("Test sorting, binary search, and isSorted")
    public void testSortAndSearch() {
        ShortList list = ShortList.of((short) 9, (short) 2, (short) 7, (short) 5, (short) 1);
        assertFalse(list.isSorted());

        list.sort();
        assertArrayEquals(new short[] { 1, 2, 5, 7, 9 }, list.toArray());
        assertTrue(list.isSorted());

        assertEquals(2, list.binarySearch((short) 5));
        assertTrue(list.binarySearch((short) 6) < 0);

        list.parallelSort();
        assertArrayEquals(new short[] { 1, 2, 5, 7, 9 }, list.toArray());

        list.reverseSort();
        assertArrayEquals(new short[] { 9, 7, 5, 2, 1 }, list.toArray());
    }

    @Test
    @DisplayName("Test in-place modifications: reverse(range), rotate, swap, fill")
    public void testModifications() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.reverse(1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, list.toArray());
        list.rotate(-1);
        assertArrayEquals(new short[] { 4, 3, 2, 5, 1 }, list.toArray());
        list.swap(0, 4);
        assertArrayEquals(new short[] { 1, 3, 2, 5, 4 }, list.toArray());
        list.fill((short) 0);
        assertArrayEquals(new short[] { 0, 0, 0, 0, 0 }, list.toArray());
    }

    @Test
    @DisplayName("Test set-like operations: intersection, difference, symmetricDifference")
    public void testSetLikeOps() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 2, (short) 3, (short) 4, (short) 4);

        ShortList intersection = list1.intersection(list2);
        intersection.sort();
        assertArrayEquals(new short[] { 2, 3 }, intersection.toArray());

        ShortList difference = list1.difference(list2);
        assertArrayEquals(new short[] { 1, 2 }, difference.toArray());

        ShortList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new short[] { 1, 2, 4, 4 }, symmDiff.toArray());
    }

    @Test
    @DisplayName("Test conversion methods: toArray, boxed, toIntList, stream")
    public void testConversions() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);

        assertArrayEquals(new short[] { 10, 20, 30 }, list.toArray());

        List<Short> boxed = list.boxed();
        assertEquals(List.of((short) 10, (short) 20, (short) 30), boxed);

        IntList intList = list.toIntList();
        assertArrayEquals(new int[] { 10, 20, 30 }, intList.toArray());

        assertEquals(50, list.stream(1, 3).sum());
    }

    @Test
    @DisplayName("Test first/last element accessors and removers")
    public void testFirstLastAccess() {
        ShortList list = ShortList.of((short) 5, (short) 10, (short) 15);
        assertEquals((short) 5, list.getFirst());
        assertEquals((short) 15, list.getLast());
        assertEquals(OptionalShort.of((short) 5), list.first());
        assertEquals(OptionalShort.of((short) 15), list.last());

        assertEquals((short) 5, list.removeFirst());
        assertEquals((short) 15, list.removeLast());
        assertArrayEquals(new short[] { 10 }, list.toArray());

        ShortList emptyList = new ShortList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::removeLast);
    }

    @Test
    @DisplayName("Test equals and hashCode")
    public void testEqualsAndHashCode() {
        ShortList list1 = ShortList.of((short) 1, (short) 2);
        ShortList list2 = ShortList.of((short) 1, (short) 2);
        ShortList list3 = ShortList.of((short) 2, (short) 1);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    @DisplayName("Test iterator functionality")
    public void testIterator() {
        ShortList list = ShortList.of((short) 100, (short) -100);
        ShortIterator it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals((short) 100, it.nextShort());
        assertTrue(it.hasNext());
        assertEquals((short) -100, it.nextShort());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("Test copy, split, and trimToSize methods")
    public void testCopySplitTrim() {
        ShortList listWithCap = new ShortList(10);
        listWithCap.addAll(new short[] { 1, 2, 3 });
        assertEquals(10, listWithCap.array().length);
        listWithCap.trimToSize();
        assertEquals(3, listWithCap.array().length);

        ShortList copy = listWithCap.copy(0, 3, 2);
        assertArrayEquals(new short[] { 1, 3 }, copy.toArray());

        List<ShortList> chunks = listWithCap.split(2);
        assertEquals(2, chunks.size());
        assertArrayEquals(new short[] { 1, 2 }, chunks.get(0).toArray());
        assertArrayEquals(new short[] { 3 }, chunks.get(1).toArray());
    }

    @Test
    @DisplayName("Test toCollection and toMultiset")
    public void testToCollectionAndMultiset() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 10);
        ArrayList<Short> collection = list.toCollection(ArrayList::new);
        assertEquals(List.of((short) 10, (short) 20, (short) 10), collection);

        Multiset<Short> multiset = list.toMultiset();
        assertEquals(2, multiset.count((short) 10));
        assertEquals(1, multiset.count((short) 20));
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        AtomicInteger sum = new AtomicInteger(0);
        list.forEach(b -> sum.addAndGet(b));
        assertEquals(6, sum.get());
    }
}
