package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalByte;

public class ByteList100Test extends TestBase {

    private ByteList list;

    @BeforeEach
    public void setUp() {
        list = new ByteList();
    }

    @Test
    public void testConstructors() {
        // Test default constructor
        ByteList list1 = new ByteList();
        assertEquals(0, list1.size());

        // Test constructor with initial capacity
        ByteList list2 = new ByteList(10);
        assertEquals(0, list2.size());

        // Test constructor with array
        byte[] arr = { 1, 2, 3 };
        ByteList list3 = new ByteList(arr);
        assertEquals(3, list3.size());
        assertEquals((byte) 1, list3.get(0));
        assertEquals((byte) 2, list3.get(1));
        assertEquals((byte) 3, list3.get(2));

        // Test constructor with array and size
        byte[] arr2 = { 1, 2, 3, 4, 5 };
        ByteList list4 = new ByteList(arr2, 3);
        assertEquals(3, list4.size());
        assertEquals((byte) 1, list4.get(0));
        assertEquals((byte) 2, list4.get(1));
        assertEquals((byte) 3, list4.get(2));

        // Test constructor with invalid size
        assertThrows(IndexOutOfBoundsException.class, () -> new ByteList(arr2, 10));

        // Test constructor with null array
        assertThrows(NullPointerException.class, () -> new ByteList(null));
        assertThrows(NullPointerException.class, () -> new ByteList(null, 0));
    }

    @Test
    public void testStaticFactoryMethods() {
        // Test of() with varargs
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, list1.size());
        assertEquals((byte) 1, list1.get(0));
        assertEquals((byte) 2, list1.get(1));
        assertEquals((byte) 3, list1.get(2));

        // Test of() with null array
        ByteList list2 = ByteList.of((byte[]) null);
        assertEquals(0, list2.size());

        // Test of() with array and size
        byte[] arr = { 1, 2, 3, 4 };
        ByteList list3 = ByteList.of(arr, 2);
        assertEquals(2, list3.size());
        assertEquals((byte) 1, list3.get(0));
        assertEquals((byte) 2, list3.get(1));

        // Test copyOf()
        ByteList list4 = ByteList.copyOf(arr);
        assertEquals(4, list4.size());
        arr[0] = 10; // Modify original array
        assertEquals((byte) 1, list4.get(0)); // Should not affect the copy

        // Test copyOf() with range
        ByteList list5 = ByteList.copyOf(arr, 1, 3);
        assertEquals(2, list5.size());
        assertEquals((byte) 2, list5.get(0));
        assertEquals((byte) 3, list5.get(1));

        // Test repeat()
        ByteList list6 = ByteList.repeat((byte) 5, 4);
        assertEquals(4, list6.size());
        for (int i = 0; i < 4; i++) {
            assertEquals((byte) 5, list6.get(i));
        }

        // Test random()
        ByteList list7 = ByteList.random(10);
        assertEquals(10, list7.size());
        // All values should be valid bytes
        for (int i = 0; i < 10; i++) {
            byte value = list7.get(i);
            assertTrue(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE);
        }
    }

    @Test
    public void testRangeFactoryMethods() {
        // Test range(startInclusive, endExclusive)
        ByteList list1 = ByteList.range((byte) 0, (byte) 5);
        assertEquals(5, list1.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((byte) i, list1.get(i));
        }

        // Test range(startInclusive, endExclusive, by)
        ByteList list2 = ByteList.range((byte) 0, (byte) 10, (byte) 2);
        assertEquals(5, list2.size());
        assertEquals((byte) 0, list2.get(0));
        assertEquals((byte) 2, list2.get(1));
        assertEquals((byte) 4, list2.get(2));
        assertEquals((byte) 6, list2.get(3));
        assertEquals((byte) 8, list2.get(4));

        // Test rangeClosed(startInclusive, endInclusive)
        ByteList list3 = ByteList.rangeClosed((byte) 1, (byte) 5);
        assertEquals(5, list3.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((byte) (i + 1), list3.get(i));
        }

        // Test rangeClosed(startInclusive, endInclusive, by)
        ByteList list4 = ByteList.rangeClosed((byte) 1, (byte) 9, (byte) 2);
        assertEquals(5, list4.size());
        assertEquals((byte) 1, list4.get(0));
        assertEquals((byte) 3, list4.get(1));
        assertEquals((byte) 5, list4.get(2));
        assertEquals((byte) 7, list4.get(3));
        assertEquals((byte) 9, list4.get(4));

        // Test with negative ranges
        ByteList list5 = ByteList.range((byte) -5, (byte) 0);
        assertEquals(5, list5.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((byte) (-5 + i), list5.get(i));
        }
    }

    @Test
    public void testGetAndSet() {
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        // Test get()
        assertEquals((byte) 10, list.get(0));
        assertEquals((byte) 20, list.get(1));
        assertEquals((byte) 30, list.get(2));

        // Test get() with invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));

        // Test set()
        byte oldValue = list.set(1, (byte) 25);
        assertEquals((byte) 20, oldValue);
        assertEquals((byte) 25, list.get(1));

        // Test set() with invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, (byte) 40));
    }

    @Test
    public void testAdd() {
        // Test add(byte)
        list.add((byte) 1);
        assertEquals(1, list.size());
        assertEquals((byte) 1, list.get(0));

        list.add((byte) 2);
        assertEquals(2, list.size());
        assertEquals((byte) 2, list.get(1));

        // Test add(int, byte)
        list.add(1, (byte) 15);
        assertEquals(3, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 15, list.get(1));
        assertEquals((byte) 2, list.get(2));

        // Test add() at beginning
        list.add(0, (byte) 0);
        assertEquals(4, list.size());
        assertEquals((byte) 0, list.get(0));

        // Test add() at end
        list.add(list.size(), (byte) 100);
        assertEquals(5, list.size());
        assertEquals((byte) 100, list.get(4));

        // Test add() with invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, (byte) 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(6, (byte) 5));
    }

    @Test
    public void testAddAll() {
        // Test addAll(ByteList)
        ByteList other = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.addAll(other));
        assertEquals(3, list.size());

        // Test addAll(ByteList) with empty list
        assertFalse(list.addAll(new ByteList()));
        assertEquals(3, list.size());

        // Test addAll(int, ByteList)
        ByteList other2 = ByteList.of((byte) 10, (byte) 20);
        assertTrue(list.addAll(1, other2));
        assertEquals(5, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 20, list.get(2));
        assertEquals((byte) 2, list.get(3));
        assertEquals((byte) 3, list.get(4));

        // Test addAll(byte[])
        byte[] arr = { 50, 60 };
        assertTrue(list.addAll(arr));
        assertEquals(7, list.size());

        // Test addAll(int, byte[])
        byte[] arr2 = { -1, -2 };
        assertTrue(list.addAll(0, arr2));
        assertEquals(9, list.size());
        assertEquals((byte) -1, list.get(0));
        assertEquals((byte) -2, list.get(1));
    }

    @Test
    public void testRemove() {
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 10);
        list.add((byte) 30);

        // Test remove(byte)
        assertTrue(list.remove((byte) 20));
        assertEquals(3, list.size());
        assertEquals((byte) 10, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 30, list.get(2));

        // Test remove() non-existent element
        assertFalse(list.remove((byte) 40));
        assertEquals(3, list.size());

        // Test removeAllOccurrences()
        assertTrue(list.removeAllOccurrences((byte) 10));
        assertEquals(1, list.size());
        assertEquals((byte) 30, list.get(0));

        // Test removeAllOccurrences() with no occurrences
        assertFalse(list.removeAllOccurrences((byte) 10));
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);

        // Remove all even numbers
        assertTrue(list.removeIf(b -> b % 2 == 0));
        assertEquals(2, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 3, list.get(1));

        // Try to remove with predicate that matches nothing
        assertFalse(list.removeIf(b -> b > 10));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveDuplicates() {
        // Test with no duplicates
        list.add((byte) 1);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());

        // Test with unsorted duplicates
        list.clear();
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 2);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 3, list.get(2));

        // Test with sorted duplicates
        list.clear();
        list.add((byte) 1);
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 2);
        list.add((byte) 3);
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 3, list.get(2));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 2);

        // Test removeAll(ByteList)
        ByteList toRemove = ByteList.of((byte) 2);
        assertTrue(list.removeAll(toRemove));
        assertEquals(2, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 3, list.get(1));

        // Test removeAll(byte[])
        list.clear();
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        assertTrue(list.removeAll(new byte[] { 1, 3 }));
        assertEquals(1, list.size());
        assertEquals((byte) 2, list.get(0));

        // Test retainAll(ByteList)
        list.clear();
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        ByteList toRetain = ByteList.of((byte) 1, (byte) 3);
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 3, list.get(1));

        // Test retainAll(byte[])
        list.clear();
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        assertTrue(list.retainAll(new byte[] { 2 }));
        assertEquals(1, list.size());
        assertEquals((byte) 2, list.get(0));
    }

    @Test
    public void testDelete() {
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        // Test delete(int)
        assertEquals((byte) 20, list.delete(1));
        assertEquals(2, list.size());
        assertEquals((byte) 10, list.get(0));
        assertEquals((byte) 30, list.get(1));

        // Test delete() with invalid index
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(-1));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);
        list.add((byte) 5);

        // Test deleteAllByIndices()
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 5, list.get(2));

        // Test with empty indices
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRange() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);
        list.add((byte) 5);

        // Test deleteRange()
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 5, list.get(1));

        // Test deleteRange() with same indices
        list.deleteRange(1, 1);
        assertEquals(2, list.size());

        // Test deleteRange() with invalid indices
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(-1, 1));
    }

    @Test
    public void testMoveRange() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);
        list.add((byte) 5);

        // Move middle elements to beginning
        list.moveRange(1, 3, 0);
        assertEquals(5, list.size());
        assertEquals((byte) 2, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 1, list.get(2));
        assertEquals((byte) 4, list.get(3));
        assertEquals((byte) 5, list.get(4));
    }

    @Test
    public void testReplaceRange() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);

        // Test replaceRange() with ByteList
        ByteList replacement = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 20, list.get(2));
        assertEquals((byte) 30, list.get(3));
        assertEquals((byte) 4, list.get(4));

        // Test replaceRange() with byte[]
        list.clear();
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.replaceRange(1, 2, new byte[] { 100, 101 });
        assertEquals(4, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 100, list.get(1));
        assertEquals((byte) 101, list.get(2));
        assertEquals((byte) 3, list.get(3));

        // Test replaceRange() with empty replacement
        list.replaceRange(1, 3, new byte[0]);
        assertEquals(2, list.size());
    }

    @Test
    public void testReplaceAll() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);

        // Test replaceAll(byte, byte)
        assertEquals(2, list.replaceAll((byte) 1, (byte) 10));
        assertEquals(4, list.size());
        assertEquals((byte) 10, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 10, list.get(2));
        assertEquals((byte) 3, list.get(3));

        // Test replaceAll(ByteUnaryOperator)
        list.replaceAll(b -> (byte) (b * 2));
        assertEquals((byte) 20, list.get(0));
        assertEquals((byte) 4, list.get(1));
        assertEquals((byte) 20, list.get(2));
        assertEquals((byte) 6, list.get(3));

        // Test replaceIf()
        assertTrue(list.replaceIf(b -> b > 10, (byte) 0));
        assertEquals((byte) 0, list.get(0));
        assertEquals((byte) 4, list.get(1));
        assertEquals((byte) 0, list.get(2));
        assertEquals((byte) 6, list.get(3));

        // Test replaceIf() with no matches
        assertFalse(list.replaceIf(b -> b > 100, (byte) 1));
    }

    @Test
    public void testFill() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        // Test fill(byte)
        list.fill((byte) 5);
        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            assertEquals((byte) 5, list.get(i));
        }

        // Test fill(int, int, byte)
        list.fill(1, 3, (byte) 10);
        assertEquals((byte) 5, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 10, list.get(2));
    }

    @Test
    public void testContains() {
        list.add((byte) 10);
        list.add((byte) 20);

        assertTrue(list.contains((byte) 10));
        assertTrue(list.contains((byte) 20));
        assertFalse(list.contains((byte) 30));

        list.clear();
        assertFalse(list.contains((byte) 10));
    }

    @Test
    public void testContainsAnyAndAll() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        // Test containsAny(ByteList)
        assertTrue(list.containsAny(ByteList.of((byte) 1)));
        assertTrue(list.containsAny(ByteList.of((byte) 4, (byte) 3)));
        assertFalse(list.containsAny(ByteList.of((byte) 4, (byte) 5)));
        assertFalse(list.containsAny(new ByteList()));

        // Test containsAny(byte[])
        assertTrue(list.containsAny(new byte[] { 1 }));
        assertTrue(list.containsAny(new byte[] { 4, 2 }));
        assertFalse(list.containsAny(new byte[] { 4, 5 }));

        // Test containsAll(ByteList)
        assertTrue(list.containsAll(ByteList.of((byte) 1, (byte) 2)));
        assertTrue(list.containsAll(ByteList.of((byte) 3)));
        assertFalse(list.containsAll(ByteList.of((byte) 1, (byte) 4)));
        assertTrue(list.containsAll(new ByteList()));

        // Test containsAll(byte[])
        assertTrue(list.containsAll(new byte[] { 1, 2 }));
        assertFalse(list.containsAll(new byte[] { 1, 4 }));
        assertTrue(list.containsAll(new byte[0]));

        // Test with empty list
        list.clear();
        assertFalse(list.containsAny(ByteList.of((byte) 1)));
        assertFalse(list.containsAll(ByteList.of((byte) 1)));
        assertTrue(list.containsAll(new ByteList()));
    }

    @Test
    public void testDisjoint() {
        list.add((byte) 1);
        list.add((byte) 2);

        // Test disjoint(ByteList)
        assertFalse(list.disjoint(ByteList.of((byte) 1)));
        assertFalse(list.disjoint(ByteList.of((byte) 2, (byte) 3)));
        assertTrue(list.disjoint(ByteList.of((byte) 3, (byte) 4)));
        assertTrue(list.disjoint(new ByteList()));

        // Test disjoint(byte[])
        assertFalse(list.disjoint(new byte[] { 1 }));
        assertTrue(list.disjoint(new byte[] { 3, 4 }));
        assertTrue(list.disjoint(new byte[0]));

        // Test with empty list
        list.clear();
        assertTrue(list.disjoint(ByteList.of((byte) 1)));
        assertTrue(list.disjoint(new byte[] { 2 }));
    }

    @Test
    public void testIntersection() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 2);

        // Test intersection(ByteList)
        ByteList other = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 2);
        ByteList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals((byte) 2, result.get(0));
        assertEquals((byte) 3, result.get(1));
        assertEquals((byte) 2, result.get(2));

        // Test intersection(byte[])
        result = list.intersection(new byte[] { 1, 1, 4 });
        assertEquals(1, result.size());
        assertEquals((byte) 1, result.get(0));

        // Test with empty
        result = list.intersection(new ByteList());
        assertEquals(0, result.size());
    }

    @Test
    public void testDifference() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 2);

        // Test difference(ByteList)
        ByteList other = ByteList.of((byte) 2, (byte) 2);
        ByteList result = list.difference(other);
        assertEquals(2, result.size());
        assertEquals((byte) 1, result.get(0));
        assertEquals((byte) 3, result.get(1));

        // Test difference(byte[])
        result = list.difference(new byte[] { 1, 3 });
        assertEquals(2, result.size());
        assertEquals((byte) 2, result.get(0));
        assertEquals((byte) 2, result.get(1));

        // Test with empty
        result = list.difference(new ByteList());
        assertEquals(4, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        // Test symmetricDifference(ByteList)
        ByteList other = ByteList.of((byte) 2, (byte) 3, (byte) 4);
        ByteList result = list.symmetricDifference(other);
        assertEquals(2, result.size());
        assertEquals((byte) 1, result.get(0));
        assertEquals((byte) 4, result.get(1));

        // Test symmetricDifference(byte[])
        result = list.symmetricDifference(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(2, result.size());
        assertEquals((byte) 4, result.get(0));
        assertEquals((byte) 5, result.get(1));

        // Test with empty
        result = list.symmetricDifference(new ByteList());
        assertEquals(3, result.size());
    }

    @Test
    public void testOccurrencesOf() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 1);

        assertEquals(3, list.occurrencesOf((byte) 1));
        assertEquals(1, list.occurrencesOf((byte) 2));
        assertEquals(1, list.occurrencesOf((byte) 3));
        assertEquals(0, list.occurrencesOf((byte) 4));

        list.clear();
        assertEquals(0, list.occurrencesOf((byte) 1));
    }

    @Test
    public void testIndexOf() {
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);
        list.add((byte) 20);

        // Test indexOf(byte)
        assertEquals(0, list.indexOf((byte) 10));
        assertEquals(1, list.indexOf((byte) 20));
        assertEquals(2, list.indexOf((byte) 30));
        assertEquals(-1, list.indexOf((byte) 40));

        // Test indexOf(byte, int)
        assertEquals(3, list.indexOf((byte) 20, 2));
        assertEquals(-1, list.indexOf((byte) 10, 1));
        assertEquals(-1, list.indexOf((byte) 20, 4));

        // Test with empty list
        list.clear();
        assertEquals(-1, list.indexOf((byte) 10));
    }

    @Test
    public void testLastIndexOf() {
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);
        list.add((byte) 20);

        // Test lastIndexOf(byte)
        assertEquals(0, list.lastIndexOf((byte) 10));
        assertEquals(3, list.lastIndexOf((byte) 20));
        assertEquals(2, list.lastIndexOf((byte) 30));
        assertEquals(-1, list.lastIndexOf((byte) 40));

        // Test lastIndexOf(byte, int)
        assertEquals(1, list.lastIndexOf((byte) 20, 2));
        assertEquals(-1, list.lastIndexOf((byte) 10, -1));
        assertEquals(3, list.lastIndexOf((byte) 20, 10)); // Beyond size

        // Test with empty list
        list.clear();
        assertEquals(-1, list.lastIndexOf((byte) 10));
    }

    @Test
    public void testMinMaxMedian() {
        // Test with empty list
        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.median().isPresent());

        // Test with single element
        list.add((byte) 5);
        assertEquals(OptionalByte.of((byte) 5), list.min());
        assertEquals(OptionalByte.of((byte) 5), list.max());
        assertEquals(OptionalByte.of((byte) 5), list.median());

        // Test with multiple elements
        list.clear();
        list.add((byte) 3);
        list.add((byte) 1);
        list.add((byte) 4);
        list.add((byte) 1);
        list.add((byte) 5);

        assertEquals(OptionalByte.of((byte) 1), list.min());
        assertEquals(OptionalByte.of((byte) 5), list.max());
        assertEquals(OptionalByte.of((byte) 3), list.median());

        // Test min/max/median with range
        assertEquals(OptionalByte.of((byte) 1), list.min(1, 4));
        assertEquals(OptionalByte.of((byte) 4), list.max(1, 4));
        assertEquals(OptionalByte.of((byte) 1), list.median(1, 4));

        // Test with empty range
        assertFalse(list.min(2, 2).isPresent());
        assertFalse(list.max(2, 2).isPresent());
        assertFalse(list.median(2, 2).isPresent());
    }

    @Test
    public void testForEach() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        // Test forEach(ByteConsumer)
        List<Byte> collected = new ArrayList<>();
        list.forEach(b -> collected.add(b));
        assertEquals(3, collected.size());
        assertEquals(Byte.valueOf((byte) 1), collected.get(0));
        assertEquals(Byte.valueOf((byte) 2), collected.get(1));
        assertEquals(Byte.valueOf((byte) 3), collected.get(2));

        // Test forEach(int, int, ByteConsumer)
        collected.clear();
        list.forEach(1, 3, b -> collected.add(b));
        assertEquals(2, collected.size());
        assertEquals(Byte.valueOf((byte) 2), collected.get(0));
        assertEquals(Byte.valueOf((byte) 3), collected.get(1));

        // Test forEach() with reverse order
        collected.clear();
        list.forEach(2, 0, b -> collected.add(b));
        assertEquals(2, collected.size());
        assertEquals(Byte.valueOf((byte) 3), collected.get(0));
        assertEquals(Byte.valueOf((byte) 2), collected.get(1));
    }

    @Test
    public void testFirstAndLast() {
        // Test with empty list
        OptionalByte first = list.first();
        OptionalByte last = list.last();
        assertFalse(first.isPresent());
        assertFalse(last.isPresent());

        // Test with elements
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        first = list.first();
        last = list.last();
        assertTrue(first.isPresent());
        assertTrue(last.isPresent());
        assertEquals((byte) 10, first.get());
        assertEquals((byte) 30, last.get());
    }

    @Test
    public void testDistinct() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 2);

        // Test distinct()
        ByteList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertTrue(distinct.contains((byte) 1));
        assertTrue(distinct.contains((byte) 2));
        assertTrue(distinct.contains((byte) 3));

        // Test distinct() with range
        distinct = list.distinct(1, 4);
        assertEquals(3, distinct.size());
    }

    @Test
    public void testHasDuplicates() {
        // Empty list
        assertFalse(list.hasDuplicates());

        // Single element
        list.add((byte) 1);
        assertFalse(list.hasDuplicates());

        // Two different elements
        list.add((byte) 2);
        assertFalse(list.hasDuplicates());

        // Add duplicate
        list.add((byte) 1);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        // Empty list
        assertTrue(list.isSorted());

        // Single element
        list.add((byte) 1);
        assertTrue(list.isSorted());

        // Sorted list
        list.add((byte) 2);
        list.add((byte) 3);
        assertTrue(list.isSorted());

        // Unsorted list
        list.add((byte) 1);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSort() {
        // Test with mixed values
        list.add((byte) 3);
        list.add((byte) 1);
        list.add((byte) 4);
        list.add((byte) 1);
        list.add((byte) 5);

        list.sort();
        assertEquals(5, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 1, list.get(1));
        assertEquals((byte) 3, list.get(2));
        assertEquals((byte) 4, list.get(3));
        assertEquals((byte) 5, list.get(4));
        assertTrue(list.isSorted());

        // Test with empty list
        list.clear();
        list.sort();
        assertEquals(0, list.size());

        // Test with single element
        list.add((byte) 1);
        list.sort();
        assertEquals(1, list.size());
        assertEquals((byte) 1, list.get(0));
    }

    @Test
    public void testParallelSort() {
        // Add many elements to make parallel sort meaningful
        for (int i = 100; i > 0; i--) {
            list.add((byte) (i % 128));
        }

        list.parallelSort();
        assertTrue(list.isSorted());

        // Verify first few elements
        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1) <= list.get(i));
        }
    }

    @Test
    public void testReverseSort() {
        list.add((byte) 3);
        list.add((byte) 1);
        list.add((byte) 4);
        list.add((byte) 1);

        list.reverseSort();
        assertEquals(4, list.size());
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 1, list.get(2));
        assertEquals((byte) 1, list.get(3));
    }

    @Test
    public void testBinarySearch() {
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 5);
        list.add((byte) 7);
        list.add((byte) 9);

        // List must be sorted for binary search
        assertTrue(list.isSorted());

        // Test binarySearch(byte)
        assertEquals(0, list.binarySearch((byte) 1));
        assertEquals(2, list.binarySearch((byte) 5));
        assertEquals(4, list.binarySearch((byte) 9));
        assertTrue(list.binarySearch((byte) 2) < 0);
        assertTrue(list.binarySearch((byte) 10) < 0);

        // Test binarySearch(int, int, byte)
        assertEquals(1, list.binarySearch(1, 4, (byte) 3));
        assertEquals(3, list.binarySearch(2, 5, (byte) 7));
        assertTrue(list.binarySearch(0, 3, (byte) 7) < 0);
    }

    @Test
    public void testReverse() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);

        // Test reverse()
        list.reverse();
        assertEquals(4, list.size());
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 2, list.get(2));
        assertEquals((byte) 1, list.get(3));

        // Test reverse(int, int)
        list.reverse(1, 3);
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 3, list.get(2));
        assertEquals((byte) 1, list.get(3));
    }

    @Test
    public void testRotate() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);

        // Rotate right by 1
        list.rotate(1);
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 1, list.get(1));
        assertEquals((byte) 2, list.get(2));
        assertEquals((byte) 3, list.get(3));

        // Rotate left by 2
        list.rotate(-2);
        assertEquals((byte) 2, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 4, list.get(2));
        assertEquals((byte) 1, list.get(3));
    }

    @Test
    public void testShuffle() {
        // Test with sufficient elements to make shuffle meaningful
        for (int i = 0; i < 20; i++) {
            list.add((byte) i);
        }

        ByteList original = list.copy();
        list.shuffle();
        assertEquals(original.size(), list.size());

        // Check all elements are still present
        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }

        // Test shuffle with Random
        list.shuffle(new Random(42));
        assertEquals(original.size(), list.size());
    }

    @Test
    public void testSwap() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        list.swap(0, 2);
        assertEquals((byte) 3, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 1, list.get(2));

        list.swap(0, 1);
        assertEquals((byte) 2, list.get(0));
        assertEquals((byte) 3, list.get(1));

        // Test swap with invalid indices
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopy() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        // Test copy()
        ByteList copy = list.copy();
        assertEquals(list.size(), copy.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i), copy.get(i));
        }

        // Ensure it's a real copy
        copy.set(0, (byte) 10);
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 10, copy.get(0));

        // Test copy(int, int)
        ByteList partialCopy = list.copy(1, 3);
        assertEquals(2, partialCopy.size());
        assertEquals((byte) 2, partialCopy.get(0));
        assertEquals((byte) 3, partialCopy.get(1));

        // Test copy(int, int, int) with step
        list.clear();
        for (int i = 0; i < 10; i++) {
            list.add((byte) i);
        }
        ByteList steppedCopy = list.copy(0, 10, 2);
        assertEquals(5, steppedCopy.size());
        assertEquals((byte) 0, steppedCopy.get(0));
        assertEquals((byte) 2, steppedCopy.get(1));
        assertEquals((byte) 4, steppedCopy.get(2));
        assertEquals((byte) 6, steppedCopy.get(3));
        assertEquals((byte) 8, steppedCopy.get(4));
    }

    @Test
    public void testSplit() {
        for (int i = 0; i < 7; i++) {
            list.add((byte) i);
        }

        // Test split()
        List<ByteList> splits = list.split(0, list.size(), 3);
        assertEquals(3, splits.size());
        assertEquals(3, splits.get(0).size());
        assertEquals(3, splits.get(1).size());
        assertEquals(1, splits.get(2).size());

        // Verify content
        assertEquals((byte) 0, splits.get(0).get(0));
        assertEquals((byte) 1, splits.get(0).get(1));
        assertEquals((byte) 2, splits.get(0).get(2));
    }

    @Test
    public void testTrimToSize() {
        list.add((byte) 1);
        list.add((byte) 2);

        ByteList trimmed = list.trimToSize();
        assertSame(list, trimmed); // Should return the same instance
        assertEquals(2, list.size());
    }

    @Test
    public void testClear() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add((byte) 1);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add((byte) 1);
        assertEquals(1, list.size());

        list.add((byte) 2);
        assertEquals(2, list.size());

        list.remove((byte) 1);
        assertEquals(1, list.size());
    }

    @Test
    public void testBoxed() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        // Test boxed()
        List<Byte> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Byte.valueOf((byte) 1), boxed.get(0));
        assertEquals(Byte.valueOf((byte) 2), boxed.get(1));
        assertEquals(Byte.valueOf((byte) 3), boxed.get(2));

        // Test boxed(int, int)
        List<Byte> partialBoxed = list.boxed(1, 3);
        assertEquals(2, partialBoxed.size());
        assertEquals(Byte.valueOf((byte) 2), partialBoxed.get(0));
        assertEquals(Byte.valueOf((byte) 3), partialBoxed.get(1));
    }

    @Test
    public void testToArray() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        byte[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals((byte) 1, array[0]);
        assertEquals((byte) 2, array[1]);
        assertEquals((byte) 3, array[2]);

        // Ensure it's a copy
        array[0] = 10;
        assertEquals((byte) 1, list.get(0));
    }

    @Test
    public void testToIntList() {
        list.add((byte) 1);
        list.add((byte) -1);
        list.add((byte) 127);
        list.add((byte) -128);

        IntList intList = list.toIntList();
        assertEquals(4, intList.size());
        assertEquals(1, intList.get(0));
        assertEquals(-1, intList.get(1));
        assertEquals(127, intList.get(2));
        assertEquals(-128, intList.get(3));
    }

    @Test
    public void testToCollection() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        List<Byte> collection = list.toCollection(0, list.size(), ArrayList::new);
        assertEquals(3, collection.size());
        assertEquals(Byte.valueOf((byte) 1), collection.get(0));
        assertEquals(Byte.valueOf((byte) 2), collection.get(1));
        assertEquals(Byte.valueOf((byte) 3), collection.get(2));
    }

    @Test
    public void testToMultiset() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 1);

        Multiset<Byte> multiset = list.toMultiset(0, list.size(), Multiset::new);
        assertEquals(3, multiset.count(Byte.valueOf((byte) 1)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 3)));
    }

    @Test
    public void testIterator() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        ByteIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals((byte) 1, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals((byte) 2, iter.nextByte());
        assertTrue(iter.hasNext());
        assertEquals((byte) 3, iter.nextByte());
        assertFalse(iter.hasNext());

        // Test empty list iterator
        ByteList emptyList = new ByteList();
        ByteIterator emptyIter = emptyList.iterator();
        assertFalse(emptyIter.hasNext());
    }

    @Test
    public void testStream() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        // Test stream()
        byte[] streamResult = list.stream().toArray();
        assertEquals(3, streamResult.length);
        assertEquals((byte) 1, streamResult[0]);
        assertEquals((byte) 2, streamResult[1]);
        assertEquals((byte) 3, streamResult[2]);

        // Test stream(int, int)
        byte[] partialStreamResult = list.stream(1, 3).toArray();
        assertEquals(2, partialStreamResult.length);
        assertEquals((byte) 2, partialStreamResult[0]);
        assertEquals((byte) 3, partialStreamResult[1]);
    }

    @Test
    public void testGetFirstAndGetLast() {
        // Test with empty list
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        // Test with elements
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        assertEquals((byte) 10, list.getFirst());
        assertEquals((byte) 30, list.getLast());
    }

    @Test
    public void testAddFirstAndAddLast() {
        list.add((byte) 10);

        list.addFirst((byte) 5);
        assertEquals(2, list.size());
        assertEquals((byte) 5, list.get(0));
        assertEquals((byte) 10, list.get(1));

        list.addLast((byte) 15);
        assertEquals(3, list.size());
        assertEquals((byte) 5, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 15, list.get(2));
    }

    @Test
    public void testRemoveFirstAndRemoveLast() {
        // Test with empty list
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
        assertThrows(NoSuchElementException.class, () -> list.removeLast());

        // Test with elements
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        assertEquals((byte) 10, list.removeFirst());
        assertEquals(2, list.size());
        assertEquals((byte) 20, list.get(0));
        assertEquals((byte) 30, list.get(1));

        assertEquals((byte) 30, list.removeLast());
        assertEquals(1, list.size());
        assertEquals((byte) 20, list.get(0));
    }

    @Test
    public void testHashCode() {
        list.add((byte) 1);
        list.add((byte) 2);

        ByteList other = new ByteList();
        other.add((byte) 1);
        other.add((byte) 2);

        assertEquals(list.hashCode(), other.hashCode());

        other.add((byte) 3);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
        list.add((byte) 1);
        list.add((byte) 2);

        // Test equals with same instance
        assertEquals(list, list);

        // Test equals with equal list
        ByteList other = new ByteList();
        other.add((byte) 1);
        other.add((byte) 2);
        assertEquals(list, other);

        // Test not equals with different size
        other.add((byte) 3);
        assertNotEquals(list, other);

        // Test not equals with different elements
        ByteList different = new ByteList();
        different.add((byte) 2);
        different.add((byte) 1);
        assertNotEquals(list, different);

        // Test not equals with null
        assertNotEquals(list, null);

        // Test not equals with different type
        assertNotEquals(list, "not a list");
    }

    @Test
    public void testToString() {
        // Test empty list
        assertEquals("[]", list.toString());

        // Test with elements
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    public void testArray() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        byte[] array = list.array();
        assertEquals((byte) 1, array[0]);
        assertEquals((byte) 2, array[1]);
        assertEquals((byte) 3, array[2]);

        // Test that modifications to the array affect the list
        array[0] = 10;
        assertEquals((byte) 10, list.get(0));
    }
}
