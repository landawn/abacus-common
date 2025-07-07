package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalByte;

public class ByteList200Test extends TestBase {

    @Test
    @DisplayName("Test default constructor and basic properties")
    public void testEmptyConstructor() {
        ByteList list = new ByteList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        ByteList list = new ByteList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test constructor that wraps an existing array")
    public void testConstructorWithArray() {
        byte[] data = { 1, 2, 3 };
        ByteList list = new ByteList(data);
        assertEquals(3, list.size());
        assertArrayEquals(data, list.toArray());
        // Test that it's a wrapper, not a copy
        data[0] = 10;
        assertEquals((byte) 10, list.get(0), "Internal array modification should be reflected");
    }

    @Test
    @DisplayName("Test static factory 'of' method")
    public void testOf() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(3, list.size());
        assertEquals((byte) 10, list.get(0));
        assertEquals((byte) 20, list.get(1));
        assertEquals((byte) 30, list.get(2));
        assertEquals("[10, 20, 30]", list.toString());
    }

    @Test
    @DisplayName("Test static factory 'copyOf' method")
    public void testCopyOf() {
        byte[] original = { 1, 2, 3 };
        ByteList list = ByteList.copyOf(original);
        assertEquals(3, list.size());
        assertArrayEquals(original, list.toArray());
        // Ensure it's a copy, not a reference
        original[0] = 10;
        assertEquals((byte) 1, list.get(0));
    }

    @Test
    @DisplayName("Test static factory 'range' and 'rangeClosed' methods")
    public void testRange() {
        ByteList rangeList = ByteList.range((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, rangeList.toArray());

        ByteList rangeClosedList = ByteList.rangeClosed((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, rangeClosedList.toArray());

        ByteList rangeStepList = ByteList.range((byte) 0, (byte) 10, (byte) 2);
        assertArrayEquals(new byte[] { 0, 2, 4, 6, 8 }, rangeStepList.toArray());
    }

    @Test
    @DisplayName("Test static factory 'repeat' and 'random' methods")
    public void testRepeatAndRandom() {
        ByteList repeatedList = ByteList.repeat((byte) 7, 4);
        assertArrayEquals(new byte[] { 7, 7, 7, 7 }, repeatedList.toArray());

        ByteList randomList = ByteList.random(10);
        assertEquals(10, randomList.size());
        assertNotNull(randomList);
    }

    @Test
    @DisplayName("Test adding, getting, and setting elements")
    public void testAddGetSet() {
        ByteList list = new ByteList();
        list.add((byte) 5); // [5]
        list.add(0, (byte) 1); // [1, 5]
        list.add((byte) 10); // [1, 5, 10]

        assertEquals(3, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 10, list.get(2));

        byte oldValue = list.set(1, (byte) 7);
        assertEquals((byte) 5, oldValue);
        assertEquals((byte) 7, list.get(1));
        assertArrayEquals(new byte[] { 1, 7, 10 }, list.toArray());
    }

    @Test
    @DisplayName("Test bulk operations: addAll, removeAll, retainAll")
    public void testBulkOps() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.addAll(ByteList.of((byte) 4, (byte) 5));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, list.toArray());

        list.removeAll(ByteList.of((byte) 2, (byte) 4));
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());

        list.retainAll(ByteList.of((byte) 3, (byte) 5, (byte) 7));
        assertArrayEquals(new byte[] { 3, 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'removeIf' and 'removeAllOccurrences' methods")
    public void testConditionalRemove() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 4, (byte) 2);

        assertTrue(list.removeAllOccurrences((byte) 2));
        assertArrayEquals(new byte[] { 1, 3, 4 }, list.toArray());

        list.add((byte) -1); // [1, 3, 4, -1]
        assertTrue(list.removeIf(b -> b > 2));
        assertArrayEquals(new byte[] { 1, -1 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'removeDuplicates' method")
    public void testRemoveDuplicates() {
        ByteList list = ByteList.of((byte) 1, (byte) 3, (byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new byte[] { 1, 3, 2 }, list.toArray());

        ByteList sortedList = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        assertTrue(sortedList.removeDuplicates());
        assertArrayEquals(new byte[] { 1, 2, 3 }, sortedList.toArray());
    }

    @Test
    @DisplayName("Test search methods: indexOf, lastIndexOf, contains")
    public void testSearch() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20, (byte) 10);
        assertTrue(list.contains((byte) 30));
        assertFalse(list.contains((byte) 99));
        assertEquals(0, list.indexOf((byte) 10));
        assertEquals(4, list.lastIndexOf((byte) 10));
        assertEquals(1, list.indexOf((byte) 20));
        assertEquals(3, list.lastIndexOf((byte) 20));
        assertEquals(-1, list.indexOf((byte) 99));
    }

    @Test
    @DisplayName("Test statistical methods: min, max, median")
    public void testStats() {
        ByteList list = ByteList.of((byte) 9, (byte) 2, (byte) 7, (byte) 5, (byte) 1);
        assertEquals(OptionalByte.of((byte) 1), list.min());
        assertEquals(OptionalByte.of((byte) 9), list.max());
        assertEquals(OptionalByte.of((byte) 5), list.median());

        ByteList emptyList = new ByteList();
        assertEquals(OptionalByte.empty(), emptyList.min());
        assertEquals(OptionalByte.empty(), emptyList.max());
        assertEquals(OptionalByte.empty(), emptyList.median());
    }

    @Test
    @DisplayName("Test sorting and binarySearch")
    public void testSortAndSearch() {
        ByteList list = ByteList.of((byte) 9, (byte) 2, (byte) 7, (byte) 5, (byte) 1);
        list.sort();
        assertArrayEquals(new byte[] { 1, 2, 5, 7, 9 }, list.toArray());
        assertTrue(list.isSorted());
        assertEquals(2, list.binarySearch((byte) 5));
        assertTrue(list.binarySearch((byte) 6) < 0); // Not found

        list.parallelSort(); // Should remain sorted
        assertArrayEquals(new byte[] { 1, 2, 5, 7, 9 }, list.toArray());
    }

    @Test
    @DisplayName("Test in-place modifications: reverse, rotate, swap, fill")
    public void testModifications() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);

        list.reverse();
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, list.toArray());

        list.rotate(1);
        assertArrayEquals(new byte[] { 1, 4, 3, 2 }, list.toArray());

        list.swap(1, 3);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());

        list.fill((byte) 0);
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'replaceAll' methods")
    public void testReplaceAll() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        int count = list.replaceAll((byte) 1, (byte) 5);
        assertEquals(2, count);
        assertArrayEquals(new byte[] { 5, 2, 5, 3 }, list.toArray());

        list.replaceAll(b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 10, 4, 10, 6 }, list.toArray());
    }

    @Test
    @DisplayName("Test set-like operations: intersection, difference, symmetricDifference")
    public void testSetLikeOps() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 4);

        ByteList intersection = list1.intersection(list2);
        intersection.sort();
        assertArrayEquals(new byte[] { 2, 3 }, intersection.toArray());

        ByteList difference = list1.difference(list2);
        assertArrayEquals(new byte[] { 1, 2 }, difference.toArray());

        ByteList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new byte[] { 1, 2, 4, 4 }, symmDiff.toArray());
    }

    @Test
    @DisplayName("Test conversion methods: boxed, toIntList, stream")
    public void testConversions() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);

        List<Byte> boxed = list.boxed();
        assertEquals(List.of((byte) 10, (byte) 20), boxed);

        IntList intList = list.toIntList();
        assertArrayEquals(new int[] { 10, 20 }, intList.toArray());

        assertEquals(30, list.stream().sum());
    }

    @Test
    @DisplayName("Test first/last element accessors and removers")
    public void testFirstLastAccess() {
        ByteList list = ByteList.of((byte) 5, (byte) 10, (byte) 15);

        assertEquals((byte) 5, list.getFirst());
        assertEquals((byte) 15, list.getLast());
        assertEquals(OptionalByte.of((byte) 5), list.first());

        assertEquals((byte) 5, list.removeFirst());
        assertEquals((byte) 15, list.removeLast());
        assertArrayEquals(new byte[] { 10 }, list.toArray());

        ByteList emptyList = new ByteList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::removeLast);
    }

    @Test
    @DisplayName("Test equals and hashCode")
    public void testEqualsAndHashCode() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2);
        ByteList list2 = ByteList.of((byte) 1, (byte) 2);
        ByteList list3 = ByteList.of((byte) 2, (byte) 1);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    @DisplayName("Test iterator functionality")
    public void testIterator() {
        ByteList list = ByteList.of((byte) 100, (byte) -100);
        ByteIterator it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals((byte) 100, it.nextByte());
        assertTrue(it.hasNext());
        assertEquals((byte) -100, it.nextByte());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("Test 'copy' and 'trimToSize' methods")
    public void testCopyAndTrim() {
        ByteList list = new ByteList(20);
        list.addAll(new byte[] { 1, 2, 3 });
        assertEquals(20, list.array().length);
        list.trimToSize();
        assertEquals(3, list.array().length);

        ByteList copy = list.copy();
        assertNotSame(list, copy);
        assertEquals(list, copy);
    }

    @Test
    @DisplayName("Test constructors and static factories")
    public void testConstructorsAndFactories() {
        // Default constructor
        ByteList list1 = new ByteList();
        assertTrue(list1.isEmpty());

        // Constructor with capacity
        ByteList list2 = new ByteList(20);
        assertTrue(list2.isEmpty());
        assertEquals(20, list2.array().length);

        // Constructor wrapping an array
        byte[] data = { 1, 2, 3 };
        ByteList list3 = new ByteList(data);
        assertEquals(3, list3.size());
        data[0] = 5; // Should reflect change
        assertEquals((byte) 5, list3.get(0));

        // 'of' factory
        ByteList list4 = ByteList.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, list4.toArray());

        // 'copyOf' factory
        byte[] original = { 10, 20 };
        ByteList list5 = ByteList.copyOf(original);
        original[0] = 15; // Should not reflect change
        assertEquals((byte) 10, list5.get(0));
        assertArrayEquals(new byte[] { 10, 20 }, list5.toArray());

        // 'copyOfRange' factory
        ByteList list6 = ByteList.copyOf(new byte[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 4 }, list6.toArray());
    }

    @Test
    @DisplayName("Test range, repeat, and random factories")
    public void testRangeRepeatRandom() {
        assertArrayEquals(new byte[] { 5, 6, 7 }, ByteList.range((byte) 5, (byte) 8).toArray());
        assertArrayEquals(new byte[] { 5, 6, 7, 8 }, ByteList.rangeClosed((byte) 5, (byte) 8).toArray());
        assertArrayEquals(new byte[] { 0, 3, 6 }, ByteList.rangeClosed((byte) 0, (byte) 8, (byte) 3).toArray());
        assertArrayEquals(new byte[] { 7, 7, 7 }, ByteList.repeat((byte) 7, 3).toArray());
        assertEquals(10, ByteList.random(10).size());
    }

    @Test
    @DisplayName("Test add, addFirst, addLast, and addAll methods")
    public void testAddMethods() {
        ByteList list = ByteList.of((byte) 10);
        list.add((byte) 20); // add to end
        list.add(1, (byte) 15); // add at index
        list.addFirst((byte) 5); // add to start
        list.addLast((byte) 25); // add to end again
        assertArrayEquals(new byte[] { 5, 10, 15, 20, 25 }, list.toArray());

        // addAll array
        list.addAll(new byte[] { 30, 35 });
        assertArrayEquals(new byte[] { 5, 10, 15, 20, 25, 30, 35 }, list.toArray());

        // addAll list at index
        list.addAll(0, ByteList.of((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2, 5, 10, 15, 20, 25, 30, 35 }, list.toArray());
    }

    @Test
    @DisplayName("Test remove, removeAll, and retainAll methods")
    public void testRemoveMethods() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 3);

        assertTrue(list.remove((byte) 3)); // removes first 3
        assertArrayEquals(new byte[] { 1, 2, 4, 5, 3 }, list.toArray());

        assertTrue(list.removeAll(ByteList.of((byte) 1, (byte) 5, (byte) 9)));
        assertArrayEquals(new byte[] { 2, 4, 3 }, list.toArray());

        assertTrue(list.retainAll(new byte[] { 4, 3, 8 }));
        assertArrayEquals(new byte[] { 4, 3 }, list.toArray());
    }

    @Test
    @DisplayName("Test deletion by index and range")
    public void testDeleteMethods() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);

        byte deleted = list.delete(1);
        assertEquals((byte) 2, deleted);
        assertArrayEquals(new byte[] { 1, 3, 4, 5, 6 }, list.toArray());

        list.deleteRange(1, 3);
        assertArrayEquals(new byte[] { 1, 5, 6 }, list.toArray());

        list.deleteAllByIndices(0, 2);
        assertArrayEquals(new byte[] { 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test moveRange and replaceRange methods")
    public void testMoveAndReplaceRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.moveRange(1, 3, 3); // move {2,3} to the end
        assertArrayEquals(new byte[] { 1, 4, 5, 2, 3 }, list.toArray());

        list.replaceRange(0, 2, ByteList.of((byte) 9, (byte) 8, (byte) 7));
        assertArrayEquals(new byte[] { 9, 8, 7, 5, 2, 3 }, list.toArray());
    }

    @Test
    @DisplayName("Test replaceIf and replaceAll methods")
    public void testReplaceMethods() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40);
        assertTrue(list.replaceIf(b -> b > 25, (byte) 99));
        assertArrayEquals(new byte[] { 10, 20, 99, 99 }, list.toArray());

        list.replaceAll(b -> (byte) (b / 10));
        assertArrayEquals(new byte[] { 1, 2, 9, 9 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'contains', 'containsAny', 'containsAll', and 'disjoint' methods")
    public void testContainsAndDisjoint() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertTrue(list.contains((byte) 10));
        assertTrue(list.containsAny(new byte[] { 5, 15, 20 }));
        assertFalse(list.containsAny(new byte[] { 5, 15, 25 }));
        assertTrue(list.containsAll(ByteList.of((byte) 10, (byte) 30)));
        assertFalse(list.containsAll(ByteList.of((byte) 10, (byte) 40)));

        assertTrue(list.disjoint(ByteList.of((byte) 1, (byte) 2, (byte) 3)));
        assertFalse(list.disjoint(new byte[] { 15, 25, 30 }));
    }

    @Test
    @DisplayName("Test set-like operations with primitive array inputs")
    public void testSetLikeOpsWithArrays() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 2);
        byte[] arr = { 2, 3, 3 };

        ByteList intersection = list.intersection(arr);
        assertArrayEquals(new byte[] { 2 }, intersection.toArray());

        ByteList difference = list.difference(arr);
        assertArrayEquals(new byte[] { 1, 2 }, difference.toArray());

        ByteList symmetricDifference = list.symmetricDifference(arr);
        symmetricDifference.sort();
        assertArrayEquals(new byte[] { 1, 2, 3, 3 }, symmetricDifference.toArray());
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        AtomicInteger sum = new AtomicInteger(0);
        list.forEach(b -> sum.addAndGet(b));
        assertEquals(6, sum.get());
    }

    @Test
    @DisplayName("Test 'distinct' and 'reverseSort' methods")
    public void testDistinctAndReverseSort() {
        ByteList list = ByteList.of((byte) 5, (byte) 1, (byte) 5, (byte) 2, (byte) 1);
        ByteList distinctList = list.distinct();
        assertArrayEquals(new byte[] { 5, 1, 2 }, distinctList.toArray());

        list.reverseSort();
        assertArrayEquals(new byte[] { 5, 5, 2, 1, 1 }, list.toArray());
    }

    @Test
    @DisplayName("Test in-place modification on a sub-range")
    public void testSubRangeModification() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.reverse(1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'copy' and 'split' methods")
    public void testCopyAndSplit() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        ByteList subCopy = list.copy(1, 4);
        assertArrayEquals(new byte[] { 2, 3, 4 }, subCopy.toArray());

        ByteList stepCopy = list.copy(0, 6, 2);
        assertArrayEquals(new byte[] { 1, 3, 5 }, stepCopy.toArray());

        List<ByteList> chunks = list.split(3);
        assertEquals(2, chunks.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, chunks.get(0).toArray());
        assertArrayEquals(new byte[] { 4, 5, 6 }, chunks.get(1).toArray());
    }

    @Test
    @DisplayName("Test 'toMultiset' and 'toCollection' methods")
    public void testToCollection() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 10);

        ArrayList<Byte> collection = list.toCollection(ArrayList::new);
        assertEquals(List.of((byte) 10, (byte) 20, (byte) 10), collection);

        Multiset<Byte> multiset = list.toMultiset();
        assertEquals(2, multiset.count((byte) 10));
        assertEquals(1, multiset.count((byte) 20));
    }

    @Test
    @DisplayName("Test stream creation on a sub-range")
    public void testStreamRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        long sum = list.stream(1, 4).sum(); // streams over {2, 3, 4}
        assertEquals(9, sum);
    }

    @Test
    @DisplayName("Test 'array' method for direct access")
    public void testArrayMethod() {
        ByteList list = new ByteList(10);
        list.add((byte) 1);
        list.add((byte) 2);

        byte[] internalArray = list.array();
        assertEquals(10, internalArray.length);
        assertEquals((byte) 1, internalArray[0]);
        assertEquals((byte) 2, internalArray[1]);

        // Modify through the exposed array
        internalArray[0] = (byte) 99;
        assertEquals((byte) 99, list.get(0));
    }
}
