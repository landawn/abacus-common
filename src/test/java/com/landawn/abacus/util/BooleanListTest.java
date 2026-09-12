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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.stream.Stream;

public class BooleanListTest extends BooleanListTestSupport {

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> list.forEach(0, 0, (com.landawn.abacus.util.function.BooleanConsumer) null));
    }

    @Test
    public void testConstructors() {
        {
            list = new BooleanList();
            BooleanList list1 = new BooleanList();
            assertEquals(0, list1.size());

            BooleanList list2 = new BooleanList(10);
            assertEquals(0, list2.size());

            boolean[] arr = { true, false, true };
            BooleanList list3 = new BooleanList(arr);
            assertEquals(3, list3.size());
            assertEquals(true, list3.get(0));
            assertEquals(false, list3.get(1));
            assertEquals(true, list3.get(2));

            boolean[] arr2 = { true, false, true, false, true };
            BooleanList list4 = new BooleanList(arr2, 3);
            assertEquals(3, list4.size());
            assertEquals(true, list4.get(0));
            assertEquals(false, list4.get(1));
            assertEquals(true, list4.get(2));

            assertThrows(IndexOutOfBoundsException.class, () -> new BooleanList(arr2, 10));
        }
        {
            list = new BooleanList();
            boolean[] data = { true, false, true, false, true };
            BooleanList list = new BooleanList(data, 3);
            assertEquals(3, list.size());
            assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
            data[0] = false;
            assertFalse(list.get(0), "Internal array modification should be reflected in the list");
        }
        {
            list = new BooleanList();
            BooleanList zeroCapList = new BooleanList(0);
            assertEquals(0, zeroCapList.size());

            zeroCapList.add(true);
            assertEquals(1, zeroCapList.size());
            assertEquals(true, zeroCapList.get(0));
        }
    }

    @Test
    public void testConstructors_Empty() {
        {
            list = new BooleanList();
            boolean[] arr = {};
            BooleanList list = new BooleanList(arr);
            assertEquals(0, list.size());
        }
        {
            list = new BooleanList();
            BooleanList list = new BooleanList();
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new BooleanList(-1));
    }

    @Test
    public void testConstructors_InvalidSize() {
        boolean[] arr = { true, false };
        assertThrows(IndexOutOfBoundsException.class, () -> new BooleanList(arr, 3));
        assertThrows(IllegalArgumentException.class, () -> new BooleanList(arr, -1));
    }

    @Test
    public void testConstructors_Null() {
        {
            list = new BooleanList();
            assertThrows(IllegalArgumentException.class, () -> new BooleanList(null, 5));
        }
        {
            list = new BooleanList();
            assertThrows(IllegalArgumentException.class, () -> new BooleanList(null));
        }
    }

    @Test
    public void testCapacity_LargeData() {
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
    public void testArray() {
        {
            list = new BooleanList();
            boolean[] arr = { true, false, true };
            BooleanList list = BooleanList.of(arr);
            assertArrayEquals(arr, list.toArray());
        }
        {
            list = new BooleanList();
            boolean[] arr = { true, false };
            BooleanList list = new BooleanList(arr);
            assertSame(arr, list.internalArray());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);

            boolean[] array = list.internalArray();
            assertEquals(true, array[0]);
            assertEquals(false, array[1]);
            assertEquals(true, array[2]);

            array[0] = false;
            assertEquals(false, list.get(0));
        }
    }

    @Test
    public void testArray_Empty() {
        {
            list = new BooleanList();
            boolean[] arr = list.toArray();
            assertEquals(0, arr.length);
        }
        {
            list = new BooleanList();
            boolean[] arr = list.internalArray();
            assertNotNull(arr);
        }
    }

    @Test
    public void testEmpty() {
        BooleanList list = new BooleanList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void testMax() {
        BooleanList smallList = new BooleanList();
        assertNotNull(smallList);
    }

    @Test
    public void testOf() {
        {
            list = new BooleanList();
            BooleanList emptyList = BooleanList.of(new boolean[0]);
            assertEquals(0, emptyList.size());
            assertTrue(emptyList.isEmpty());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            assertEquals(3, list.size());
            assertTrue(list.get(0));
            assertFalse(list.get(1));
            assertTrue(list.get(2));
            assertEquals("[true, false, true]", list.toString());
        }
    }

    @Test
    public void testOf_Empty() {
        BooleanList list = BooleanList.of();
        assertEquals(0, list.size());
    }

    @Test
    public void testOf_Null() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of((boolean[]) null);
            assertEquals(0, list.size());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(null, 0);
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testOf_InvalidSize() {
        boolean[] arr = { true, false };
        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.of(arr, 3));
    }

    @Test
    public void testRemoveAt() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            list.removeAllAt(0, 2, 4);
            assertArrayEquals(new boolean[] { false, false }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            boolean deleted = list.removeAt(1);
            assertFalse(deleted);
            assertEquals(2, list.size());
            assertTrue(list.get(0));
            assertTrue(list.get(1));
        }
    }

    @Test
    public void testRemoveAt_OutOfBounds() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
    }

    @Test
    public void testRemoveRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.removeRange(1, 4);
        assertArrayEquals(new boolean[] { true, true }, list.toArray());
    }

    @Test
    public void testFrequency() {
        {
            list = new BooleanList();
            list.add(true);
            list.add(true);
            list.add(false);
            list.add(true);
            assertEquals(3, list.frequency(true));
            assertEquals(1, list.frequency(false));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, true, false);
            assertEquals(3, list.frequency(true));
            assertEquals(2, list.frequency(false));
        }
    }

    @Test
    public void testFrequency_Empty() {
        BooleanList list = new BooleanList();
        assertEquals(0, list.frequency(true));
    }

    @Test
    public void testContainsDuplicates() {
        assertTrue(BooleanList.of(true, false, true).containsDuplicates());
        assertTrue(BooleanList.of(true, true).containsDuplicates());
        assertFalse(BooleanList.of(true, false).containsDuplicates());
        assertFalse(BooleanList.of(true).containsDuplicates());
        assertFalse(new BooleanList().containsDuplicates());
    }

    @Test
    public void testSort() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, false);
            list.sort();
            assertArrayEquals(new boolean[] { false, false, false, true, true }, list.toArray());
            list.reverseSort();
            assertArrayEquals(new boolean[] { true, true, false, false, false }, list.toArray());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(true);
            list.sort();
            assertTrue(list.get(0));
            assertTrue(list.get(1));
        }
    }

    @Test
    public void testSort_Empty() {
        BooleanList list = new BooleanList();
        list.sort();
        assertEquals(0, list.size());
    }

    @Test
    public void testConversion() {
        boolean[] arr = { true, false, true };
        BooleanList list = BooleanList.of(arr);

        assertArrayEquals(arr, list.toArray());

        List<Boolean> boxedList = list.boxed();
        assertEquals(List.of(true, false, true), boxedList);

        ArrayList<Boolean> collected = list.toCollection(ArrayList::new);
        assertEquals(boxedList, collected);
    }

    @Test
    public void testCopyOf() {
        boolean[] original = { true, false, true, false, true };
        BooleanList list = BooleanList.copyOf(original, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    public void testCopyOf_Null() {
        {
            list = new BooleanList();
            BooleanList copyList = BooleanList.copyOf(null);
            assertEquals(0, copyList.size());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.copyOf((boolean[]) null);
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testCopyOf_InvalidRange() {
        boolean[] arr = { true, false, true, false };

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, 3, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, -1, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> BooleanList.copyOf(arr, 0, 10));
    }

    @Test
    public void testRepeat() {
        BooleanList list = BooleanList.repeat(true, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(list.get(i));
        }
        assertEquals("[true, true, true, true, true]", list.toString());
    }

    @Test
    public void testRepeat_NegativeLength() {
        assertThrows(IllegalArgumentException.class, () -> BooleanList.repeat(true, -1));
    }

    @Test
    public void testRandom() {
        BooleanList list = BooleanList.random(10);
        assertEquals(10, list.size());
    }

    @Test
    public void testGetSet() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, false, false);
            assertTrue(list.getFirst());
            assertFalse(list.getLast());
            assertEquals(OptionalBoolean.of(true), list.first());
            assertEquals(OptionalBoolean.of(false), list.last());

            BooleanList emptyList = new BooleanList();
            assertThrows(NoSuchElementException.class, emptyList::getFirst);
            assertThrows(NoSuchElementException.class, emptyList::getLast);
            assertEquals(OptionalBoolean.empty(), emptyList.first());
            assertEquals(OptionalBoolean.empty(), emptyList.last());
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, true, false);
            BooleanList list2 = BooleanList.of(true, false, false);

            BooleanList intersection = list1.intersection(list2);
            intersection.sort();
            assertArrayEquals(new boolean[] { false, true }, intersection.toArray());

            BooleanList difference = list1.difference(list2);
            assertArrayEquals(new boolean[] { true }, difference.toArray());

            BooleanList symmDiff = list1.symmetricDifference(list2);
            symmDiff.sort();
            assertArrayEquals(new boolean[] { false, true }, symmDiff.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, true);
            boolean oldValue = list.set(1, false);
            assertTrue(oldValue);
            assertFalse(list.get(1));
            assertEquals("[true, false]", list.toString());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(false, true);
            assertTrue(list.get(1));
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
        }
    }

    @Test
    public void testGetSet_OutOfBounds() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, true));
    }

    @Test
    public void testRetainAll() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            boolean changed = list.retainAll(BooleanList.of(false, false));
            assertTrue(changed);
            assertArrayEquals(new boolean[] { false, false }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false, true, false);
            BooleanList list2 = BooleanList.of(true);
            boolean result = list1.retainAll(list2);
            assertTrue(result);
            assertEquals(2, list1.size());
            assertTrue(list1.get(0));
            assertTrue(list1.get(1));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            boolean[] arr = { true };
            boolean result = list.retainAll(arr);
            assertTrue(result);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRetainAll_Empty() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = new BooleanList();
            boolean result = list1.retainAll(list2);
            assertTrue(result);
            assertEquals(0, list1.size());
        }
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false, true);
            boolean[] b = {};
            assertTrue(a.retainAll(b));
            assertEquals(0, a.size());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            assertTrue(list.retainAll(new boolean[] {}));
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testBatch() {
        BooleanList a = BooleanList.of(true, false, true, false, true);
        BooleanList b = BooleanList.of(true);
        assertTrue(a.removeAll(b));
        assertEquals(2, a.size());
        assertFalse(a.get(0));
    }

    @Test
    public void testBatch_LargeData() {
        {
            list = new BooleanList();
            // batchRemove with complement=false (removeAll) triggers set path when sizes large enough
            BooleanList a = BooleanList.of(true, false, true, false, true, false, true, false, true, false, true);
            BooleanList b = BooleanList.of(true, true, true, true);
            // c.size()=4 > 3, a.size()=11 > 9 => set path
            assertTrue(a.removeAll(b));
            // all 'true' elements should be removed; only 'false' remain
            assertEquals(5, a.size());
            for (int i = 0; i < a.size(); i++) {
                assertFalse(a.get(i));
            }
        }
        {
            list = new BooleanList();
            // batchRemove with complement=true (retainAll) triggers set path
            BooleanList a = BooleanList.of(true, false, true, false, true, false, true, false, true, false, true);
            BooleanList b = BooleanList.of(true, true, true, true);
            // retain only 'true' elements
            assertTrue(a.retainAll(b));
            assertEquals(6, a.size());
            for (int i = 0; i < a.size(); i++) {
                assertTrue(a.get(i));
            }
        }
    }

    @Test
    public void testMoveRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.moveRange(1, 3, 2); // true, false, false, true, true
        assertEquals(BooleanList.of(true, false, false, true, true), list);
    }

    @Test
    public void testFill() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(false, false, false, false);
            list.fill(1, 3, true);
            assertArrayEquals(new boolean[] { false, true, true, false }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            list.fill(false);
            assertFalse(list.get(0));
            assertFalse(list.get(1));
            assertFalse(list.get(2));
        }
    }

    @Test
    public void testFill_Empty() {
        list.fill(true);
        assertEquals(0, list.size());
    }

    @Test
    public void testFill_InvalidRange() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(0, 3, true));
    }

    @Test
    public void testDisjoint() {
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, true, true, true, true, true, true, true, true, true, true);
            assertFalse(a.disjoint(new boolean[] { true }));
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, true);
            BooleanList list2 = BooleanList.of(false, false);
            assertTrue(list1.disjoint(list2));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, true);
            assertTrue(list.disjoint(BooleanList.of(false, false)));
            assertFalse(list.disjoint(BooleanList.of(true, false)));
        }
    }

    @Test
    public void testDisjoint_LargeData() {
        // Large list but all-true vs all-false: disjoint via set path
        BooleanList a = BooleanList.of(true, true, true, true, true, true, true, true, true, true, true);
        BooleanList b = BooleanList.of(false, false, false, false);
        assertTrue(a.disjoint(b));
    }

    @Test
    public void testDisjoint_Empty() {
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, true);
            boolean[] b = { false, false };
            assertTrue(a.disjoint(b));
        }
        {
            list = new BooleanList();
            list.add(true);
            assertTrue(list.disjoint(new boolean[] {}));
        }
    }

    @Test
    public void testIntersection() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false, true, false);
            BooleanList list2 = BooleanList.of(true, true, false);
            BooleanList result = list1.intersection(list2);
            assertEquals(3, result.size());
            assertTrue(result.get(0));
            assertFalse(result.get(1));
            assertTrue(result.get(2));
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, true, false);
            BooleanList list2 = BooleanList.of(true, false, false);
            BooleanList intersection = list1.intersection(list2);
            assertEquals(2, intersection.size());
            assertTrue(intersection.contains(true));
            assertTrue(intersection.contains(false));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            boolean[] arr = { true };
            BooleanList result = list.intersection(arr);
            assertEquals(1, result.size());
            assertTrue(result.get(0));
        }
    }

    @Test
    public void testIntersection_Empty() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = new BooleanList();
            BooleanList result = list1.intersection(list2);
            assertEquals(0, result.size());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            BooleanList result = list.intersection(new boolean[] {});
            assertEquals(0, result.size());
        }
    }

    @Test
    public void testDifference() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, true, false, true);
            BooleanList list2 = BooleanList.of(true, false);
            BooleanList result = list1.difference(list2);
            assertEquals(2, result.size());
            assertTrue(result.get(0));
            assertTrue(result.get(1));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            boolean[] arr = { false };
            BooleanList result = list.difference(arr);
            assertEquals(2, result.size());
        }
    }

    @Test
    public void testDifference_Empty() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = new BooleanList();
            BooleanList result = list1.difference(list2);
            assertEquals(2, result.size());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            BooleanList result = list.difference(new boolean[] {});
            assertEquals(2, result.size());
        }
    }

    @Test
    public void testSymmetricDifference() {
        BooleanList receiverOnlyValues = BooleanList.of(true, true);
        assertEquals(BooleanList.of(true, true, false), receiverOnlyValues.symmetricDifference(BooleanList.of(false)));
        assertEquals(BooleanList.of(true, true, false), receiverOnlyValues.symmetricDifference(new boolean[] { false }));
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, true, false);
            BooleanList list2 = BooleanList.of(true, false, false);
            BooleanList symmDiff = list1.symmetricDifference(list2);
            symmDiff.sort();
            assertArrayEquals(new boolean[] { false, true }, symmDiff.toArray());
        }
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false);
            boolean[] b = { true, true };
            BooleanList result = a.symmetricDifference(b);
            assertNotNull(result);
            // elements in a not in b union elements in b not in a
            assertTrue(result.size() > 0);
        }
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false, true);
            BooleanList b = BooleanList.of(true, true, false);
            BooleanList result = a.symmetricDifference(b);
            // a has 2 true, 1 false; b has 2 true, 1 false -> symmetric diff = empty
            assertEquals(0, result.size());
        }
        {
            list = new BooleanList();
            BooleanList a = new BooleanList();
            boolean[] b = { true, false };
            BooleanList result = a.symmetricDifference(b);
            assertEquals(2, result.size());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            BooleanList result = list.symmetricDifference(new boolean[] { false, true });
            assertNotNull(result);
        }
    }

    @Test
    public void testSymmetricDifference_Empty() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = new BooleanList();
            BooleanList result = list1.symmetricDifference(list2);
            assertEquals(2, result.size());
        }
        {
            list = new BooleanList();
            list.add(true);
            BooleanList result = list.symmetricDifference(new boolean[] {});
            assertEquals(1, result.size());
            assertTrue(result.get(0));
        }
    }

    @Test
    public void testIndexOf() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            assertEquals(2, list.indexOf(true, 1));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            assertEquals(0, list.indexOf(true));
            assertEquals(1, list.indexOf(false));
        }
    }

    @Test
    public void testIndexOf_OutOfBounds() {
        BooleanList list = BooleanList.of(true, false);
        assertEquals(-1, list.indexOf(true, 2));
    }

    @Test
    public void testLastIndexOf() {
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false, true, false, true);
            assertEquals(2, a.lastIndexOf(true, 2));
            assertEquals(-1, a.lastIndexOf(false, 0));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            assertEquals(2, list.lastIndexOf(true));
            assertEquals(3, list.lastIndexOf(false));
        }
    }

    @Test
    public void testLastIndexOf_OutOfBounds() {
        BooleanList a = BooleanList.of(true, false, true, false, true);
        // startIndexFromBack beyond size - should clamp to size-1
        assertEquals(4, a.lastIndexOf(true, 100));
        assertEquals(3, a.lastIndexOf(false, 100));
    }

    @Test
    public void testEach() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            List<Boolean> collected = new ArrayList<>();
            list.forEach(1, 3, collected::add);
            assertEquals(2, collected.size());
            assertFalse(collected.get(0));
            assertTrue(collected.get(1));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            List<Boolean> collected = new ArrayList<>();
            list.forEach(collected::add);
            assertEquals(3, collected.size());
            assertTrue(collected.get(0));
            assertFalse(collected.get(1));
            assertTrue(collected.get(2));
        }
    }

    @Test
    public void testEach_Empty() {
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            List<Boolean> collected = new ArrayList<>();
            list.forEach(1, 1, b -> collected.add(b));
            assertTrue(collected.isEmpty());
        }
        {
            list = new BooleanList();
            List<Boolean> collected = new ArrayList<>();
            list.forEach(b -> collected.add(b));
            assertTrue(collected.isEmpty());
        }
    }

    @Test
    public void testEach_Null() {
        // Empty lists do not evaluate callbacks; non-empty lists fail naturally when invoking them.
        final BooleanList empty = new BooleanList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.forEach((com.landawn.abacus.util.function.BooleanConsumer) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.removeIf((com.landawn.abacus.util.function.BooleanPredicate) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceIf((com.landawn.abacus.util.function.BooleanPredicate) null, true));

        final BooleanList nonEmpty = BooleanList.of(true, false);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.forEach((com.landawn.abacus.util.function.BooleanConsumer) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.removeIf((com.landawn.abacus.util.function.BooleanPredicate) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceIf((com.landawn.abacus.util.function.BooleanPredicate) null, true));
    }

    @Test
    public void testFirst() {
        BooleanList list = BooleanList.of(true, false);
        OptionalBoolean first = list.first();
        assertTrue(first.isPresent());
        assertTrue(first.get());
    }

    @Test
    public void testFirst_Empty() {
        BooleanList list = new BooleanList();
        OptionalBoolean first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        BooleanList list = BooleanList.of(true, false);
        OptionalBoolean last = list.last();
        assertTrue(last.isPresent());
        assertFalse(last.get());
    }

    @Test
    public void testLast_Empty() {
        BooleanList list = new BooleanList();
        OptionalBoolean last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            BooleanList distinct = list.distinct(0, 5);
            assertEquals(2, distinct.size());
            assertTrue(distinct.get(0));
            assertFalse(distinct.get(1));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(true);
            list.add(true);
            BooleanList result = list.distinct(0, 3);
            assertEquals(1, result.size());
            assertTrue(result.get(0));
        }
    }

    @Test
    public void testDistinct_Empty() {
        BooleanList result = list.distinct(0, 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testIsSorted() {
        {
            list = new BooleanList();
            assertTrue(BooleanList.of(false, false, true, true).isSorted());
            assertFalse(BooleanList.of(true, false).isSorted());
            assertTrue(BooleanList.of(true, true).isSorted());
            assertTrue(new BooleanList().isSorted());
        }
        {
            list = new BooleanList();
            list.add(true);
            assertTrue(list.isSorted());
        }
    }

    @Test
    public void testIsSorted_Empty() {
        BooleanList list = new BooleanList();
        assertTrue(list.isSorted());
    }

    @Test
    public void testReverseSort() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.reverseSort();
        assertTrue(list.get(0));
        assertTrue(list.get(1));
        assertFalse(list.get(2));
        assertFalse(list.get(3));
    }

    @Test
    public void testReverseSort_Empty() {
        list.reverseSort();
        assertEquals(0, list.size());
    }

    @Test
    public void testReverse() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, false, true, true);
            list.reverse(1, 4);
            assertArrayEquals(new boolean[] { true, true, false, false, true }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, false);
            list.reverse();
            assertArrayEquals(new boolean[] { false, false, true }, list.toArray());
        }
    }

    @Test
    public void testReverse_Empty() {
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.reverse(0, 0);
            assertTrue(list.get(0));
            assertFalse(list.get(1));
        }
        {
            list = new BooleanList();
            list.reverse();
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testRotate() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, true, false, false);
            list.rotate(2);
            assertArrayEquals(new boolean[] { false, false, true, true }, list.toArray());
            list.rotate(-1);
            assertArrayEquals(new boolean[] { false, true, true, false }, list.toArray());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.rotate(0);
            assertTrue(list.get(0));
            assertFalse(list.get(1));
        }
    }

    @Test
    public void testRotate_Empty() {
        list.rotate(5);
        assertEquals(0, list.size());
    }

    @Test
    public void testShuffle() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false, true, false, true, false);
            BooleanList list2 = list1.copy();
            list1.shuffle();
            assertEquals(list1.size(), list2.size());
            assertTrue(N.isEqualCollection(list1.toList(), list2.toList()), "Both lists should contain the same elements after shuffle");
        }
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false, true, false, true, false);
            a.shuffle(new Random(42));
            assertEquals(6, a.size());
        }
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new BooleanList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> BooleanList.of(true).shuffle(null));
    }

    @Test
    public void testShuffle_Empty() {
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false, true, false, true, false, true, false, true, false);
            Random rnd = new Random(42);
            a.shuffle(rnd);
            assertEquals(10, a.size());
        }
        {
            list = new BooleanList();
            list.shuffle();
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testSwap() {
        BooleanList list = BooleanList.of(true, false, true);
        list.swap(0, 2);
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
        list.swap(0, 1);
        assertArrayEquals(new boolean[] { false, true, true }, list.toArray());
    }

    @Test
    public void testSwap_Invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 2));
    }

    @Test
    public void testSwap_OutOfBounds() {
        list.add(true);
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 5));
    }

    @Test
    public void testCopy() {
        {
            list = new BooleanList();
            BooleanList original = BooleanList.of(true, false);
            BooleanList copied = original.copy();

            assertNotSame(original, copied);
            assertEquals(original, copied);

            original.add(true);
            assertNotEquals(original, copied);
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            BooleanList copy = list.copy(0, 5, 2);
            assertEquals(3, copy.size());
            assertTrue(copy.get(0));
            assertTrue(copy.get(1));
            assertTrue(copy.get(2));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            BooleanList copy = list.copy();
            assertEquals(3, copy.size());
            assertTrue(copy.get(0));
            list.set(0, false);
            assertTrue(copy.get(0));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            BooleanList copy = list.copy(1, 3);
            assertEquals(2, copy.size());
            assertFalse(copy.get(0));
            assertTrue(copy.get(1));
        }
    }

    @Test
    public void testCopy_Empty() {
        {
            list = new BooleanList();
            BooleanList copy = list.copy();
            assertEquals(0, copy.size());
            assertNotSame(list, copy);
        }
        {
            list = new BooleanList();
            BooleanList copy = list.copy(0, 0);
            assertEquals(0, copy.size());
        }
    }

    @Test
    public void testCopy_NegativeStep() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        BooleanList copy = list.copy(3, 0, -1);
        assertEquals(3, copy.size());
    }

    @Test
    public void testSplit() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            List<BooleanList> chunks = list.split(2);
            assertEquals(3, chunks.size());
            assertArrayEquals(new boolean[] { true, false }, chunks.get(0).toArray());
            assertArrayEquals(new boolean[] { true, false }, chunks.get(1).toArray());
            assertArrayEquals(new boolean[] { true }, chunks.get(2).toArray());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);
            list.add(false);
            list.add(true);
            List<BooleanList> chunks = list.split(1, 4, 2);
            assertEquals(2, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(1, chunks.get(1).size());
        }
    }

    @Test
    public void testSplit_Empty() {
        List<BooleanList> chunks = list.split(0, 0, 1);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplit_Invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IllegalArgumentException.class, () -> list.split(0, 2, 0));
    }

    @Test
    public void testTrim() {
        {
            list = new BooleanList();
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
        {
            list = new BooleanList();
            BooleanList list = new BooleanList(10);
            list.add(true);
            list.add(false);
            BooleanList result = list.trimToSize();
            assertSame(list, result);
            assertEquals(2, list.size());
        }
        {
            list = new BooleanList();
            BooleanList bl = BooleanList.of(true, false);
            bl.trimToSize();
            assertEquals(2, bl.size());
        }
    }

    @Test
    public void testClear() {
        BooleanList list = BooleanList.of(true, false, true);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClear_Empty() {
        {
            list = new BooleanList();
            BooleanList a = BooleanList.of(true, false, true);
            a.clear();
            assertEquals(0, a.size());
            assertTrue(a.isEmpty());
        }
        {
            list = new BooleanList();
            BooleanList a = new BooleanList();
            a.clear();
            assertEquals(0, a.size());
        }
    }

    @Test
    public void testIsEmpty_Empty() {
        BooleanList list = new BooleanList();
        assertTrue(list.isEmpty());
        list.add(true);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(3, list.size());
    }

    @Test
    public void testBoxed() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false);
            List<Boolean> boxedList = list.boxed();
            assertEquals(2, boxedList.size());
            assertEquals(Boolean.TRUE, boxedList.get(0));
            assertEquals(Boolean.FALSE, boxedList.get(1));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            List<Boolean> boxed = list.boxed(1, 3);
            assertEquals(2, boxed.size());
            assertFalse(boxed.get(0));
            assertTrue(boxed.get(1));
        }
    }

    @Test
    public void testBoxed_Empty() {
        {
            list = new BooleanList();
            list.add(true);
            List<Boolean> boxed = list.boxed(0, 0);
            assertTrue(boxed.isEmpty());
        }
        {
            list = new BooleanList();
            List<Boolean> boxed = list.boxed();
            assertTrue(boxed.isEmpty());
        }
    }

    @Test
    public void testCollection() {
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);
            ArrayList<Boolean> result = list.toCollection(0, 2, ArrayList::new);
            assertEquals(2, result.size());
            assertTrue(result.get(0));
            assertFalse(result.get(1));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);

            List<Boolean> collection = list.toCollection(0, list.size(), ArrayList::new);
            assertEquals(3, collection.size());
            assertEquals(Boolean.TRUE, collection.get(0));
            assertEquals(Boolean.FALSE, collection.get(1));
            assertEquals(Boolean.TRUE, collection.get(2));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            List<Boolean> collection = list.toCollection(0, 3, ArrayList::new);
            assertEquals(3, collection.size());
            assertTrue(collection.get(0));
        }
    }

    @Test
    public void testCollection_Empty() {
        ArrayList<Boolean> result = list.toCollection(0, 0, ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMultiset() {
        {
            list = new BooleanList();
            list.add(true);
            list.add(true);
            list.add(false);
            Multiset<Boolean> multiset = list.toMultiset(0, 3, Multiset::new);
            assertEquals(2, multiset.getCount(true));
            assertEquals(1, multiset.getCount(false));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);
            list.add(false);
            list.add(true);

            Multiset<Boolean> multiset = list.toMultiset(0, list.size(), Multiset::new);
            assertEquals(3, multiset.count(Boolean.TRUE));
            assertEquals(2, multiset.count(Boolean.FALSE));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            Multiset<Boolean> multiset = list.toMultiset(0, 5, Multiset::new);
            assertEquals(3, multiset.getCount(true));
            assertEquals(2, multiset.getCount(false));
        }
    }

    @Test
    public void testMultiset_Empty() {
        Multiset<Boolean> multiset = list.toMultiset(0, 0, Multiset::new);
        assertEquals(0, multiset.size());
    }

    @Test
    public void testIterator() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            BooleanIterator iter = list.iterator();
            assertTrue(iter.hasNext());
            assertTrue(iter.next());
            assertTrue(iter.hasNext());
            assertFalse(iter.next());
            assertTrue(iter.hasNext());
            assertTrue(iter.next());
            assertFalse(iter.hasNext());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            BooleanIterator iter = list.iterator();
            assertTrue(iter.hasNext());
            assertTrue(iter.nextBoolean());
            assertTrue(iter.hasNext());
            assertFalse(iter.nextBoolean());
            assertFalse(iter.hasNext());
        }
    }

    @Test
    public void testIterator_Empty() {
        BooleanList list = new BooleanList();
        BooleanIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, true, false);
            long trueCount = list.stream(1, 4).filter(b -> b).count();
            assertEquals(2, trueCount);
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            long count = list.stream().filter(Boolean::booleanValue).count();
            assertEquals(2, count);
        }
    }

    @Test
    public void testStream_Empty() {
        {
            list = new BooleanList();
            list.add(true);
            Stream<Boolean> s = list.stream(0, 0);
            assertEquals(0, s.toList().size());
        }
        {
            list = new BooleanList();
            Stream<Boolean> s = list.stream();
            assertEquals(0, s.toList().size());
        }
    }

    @Test
    public void testGetFirst() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, false, true);
            assertTrue(list.getFirst());
            assertTrue(list.getLast());

            BooleanList emptyList = new BooleanList();
            assertThrows(NoSuchElementException.class, emptyList::getFirst);
            assertThrows(NoSuchElementException.class, emptyList::getLast);
        }
        {
            list = new BooleanList();
            assertThrows(NoSuchElementException.class, () -> list.getFirst());
            assertThrows(NoSuchElementException.class, () -> list.getLast());

            list.add(true);
            list.add(false);
            list.add(true);

            assertEquals(true, list.getFirst());
            assertEquals(true, list.getLast());
        }
    }

    @Test
    public void testGetFirst_Empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false);
            assertFalse(list.getLast());
        }
        {
            list = new BooleanList();
            list.add(false);
            assertFalse(list.getLast());
        }
    }

    @Test
    public void testGetLast_Empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testHashCode() {
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);

            BooleanList other = new BooleanList();
            other.add(true);
            other.add(false);

            assertEquals(list.hashCode(), other.hashCode());

            other.add(true);
            assertNotEquals(list.hashCode(), other.hashCode());
        }
        {
            list = new BooleanList();
            list.add(true);
            BooleanList other = BooleanList.of(false);
            assertNotEquals(list.hashCode(), other.hashCode());
        }
    }

    @Test
    public void testHashCode_Empty() {
        BooleanList other = new BooleanList();
        assertEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
        {
            list = new BooleanList();
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
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);

            assertEquals(list, list);

            BooleanList other = new BooleanList();
            other.add(true);
            other.add(false);
            assertEquals(list, other);

            other.add(true);
            assertNotEquals(list, other);

            BooleanList different = new BooleanList();
            different.add(false);
            different.add(true);
            assertNotEquals(list, different);

            assertNotEquals(list, null);

            assertNotEquals(list, "not a list");
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false, true);
            BooleanList list2 = BooleanList.of(true, false, true);
            assertTrue(list1.equals(list2));
        }
    }

    @Test
    public void testEquals_Null() {
        BooleanList list = BooleanList.of(true, false);
        assertFalse(list.equals(null));
    }

    @Test
    public void testEquals_Empty() {
        BooleanList other = new BooleanList();
        assertEquals(list, other);
    }

    @Test
    public void testString() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            String str = list.toString();
            assertNotNull(str);
            assertTrue(str.contains("true"));
            assertTrue(str.contains("false"));
        }
        {
            list = new BooleanList();
            assertEquals("[]", list.toString());

            list.add(true);
            list.add(false);
            list.add(true);
            assertEquals("[true, false, true]", list.toString());
        }
    }

    @Test
    public void testString_Empty() {
        BooleanList list = new BooleanList();
        String str = list.toString();
        assertNotNull(str);
    }

    @Test
    public void testEnsureCapacity() {
        {
            list = new BooleanList();
            // Create a list that starts with default capacity, add many elements
            BooleanList a = new BooleanList();
            // Add 20 elements to trigger multiple capacity doublings
            for (int i = 0; i < 20; i++) {
                a.add(i % 2 == 0);
            }
            assertEquals(20, a.size());
        }
        {
            list = new BooleanList();
            // ensureCapacity is triggered internally when adding many elements
            BooleanList a = new BooleanList(2);
            for (int i = 0; i < 15; i++) {
                a.add(i % 2 == 0);
            }
            assertEquals(15, a.size());
            // capacity grows automatically
        }
    }

    @Test
    public void testConversionSuppliersMustProduceCollectionsForEmptyRanges() {
        assertThrows(IllegalArgumentException.class, () -> list.toCollection(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> list.toCollection(0, 0, ignored -> null));
        assertThrows(IllegalArgumentException.class, () -> list.toMultiset(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> list.toMultiset(0, 0, ignored -> null));
    }

    @Test
    public void reviewFixes20260906_descendingCopyClampsAgainstSizeNotTheBackingArray() {
        // BooleanList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final BooleanList withSpareCapacity = BooleanList.of(new boolean[] { true, false, true, false, false }, 3);

        assertEquals(3, withSpareCapacity.size());
        assertEquals(5, withSpareCapacity.internalArray().length, "the test needs real spare capacity");

        assertEquals("[true, false, true]", withSpareCapacity.copy(3, -1, -1).toString(), "spare capacity must not leak into the result");
        assertEquals("[true, false, true]", withSpareCapacity.copy(2, -1, -1).toString(), "an in-range start is unaffected");

        // Ascending copies, and a copy over the whole logical range, are unchanged.
        assertEquals(withSpareCapacity.toString(), withSpareCapacity.copy(0, 3, 1).toString());
        assertEquals(3, withSpareCapacity.copy(0, 3, 1).size());

        // The source is not modified by any of this.
        assertEquals(3, withSpareCapacity.size());
    }

    @Test
    public void reviewFixes20260906_addAllAtIndexSurvivesSelfAliasing() {
        // Passing the list to itself makes source and destination the same array, across a reallocation by
        // ensureCapacity. It is correct because the tail shift runs BEFORE the source copy and the two regions
        // provably never overlap (the source is [0, numNew) and the shift writes at index + numNew or later).
        // Swap those two statements and the result is wrong, so this pins the ordering, at every index.
        for (int index = 0; index <= 3; index++) {
            final BooleanList self = BooleanList.of(true, false, true);
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final BooleanList expected = BooleanList.of(true, false, true);
            final BooleanList inserted = BooleanList.of(true, false, true);
            expected.addAll(index, BooleanList.of(true, false, true));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final BooleanList appended = BooleanList.of(true, false, true);
        appended.addAll(appended);
        assertEquals("[true, false, true, true, false, true]", appended.toString());

        // The interesting middle case, spelled out.
        final BooleanList middle = BooleanList.of(true, false, true);
        middle.addAll(1, middle);
        assertEquals("[true, true, false, true, false, true]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final BooleanList a = BooleanList.of(true, false, true);
        assertFalse(a.removeAll((BooleanList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((boolean[]) null));
        assertFalse(a.removeAll(new boolean[0]));
        assertFalse(a.removeAll(new BooleanList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final BooleanList b = BooleanList.of(true, false, true);
        assertTrue(b.retainAll((BooleanList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final BooleanList c = BooleanList.of(true, false, true);
        assertTrue(c.retainAll((boolean[]) null));
        assertEquals(0, c.size());

        final BooleanList d = BooleanList.of(true, false, true);
        assertTrue(d.retainAll(new boolean[0]));
        assertEquals(0, d.size());

        final BooleanList e = BooleanList.of(true, false, true);
        assertTrue(e.retainAll(new BooleanList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final BooleanList empty = new BooleanList();
        assertFalse(empty.retainAll((BooleanList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final BooleanList f = BooleanList.of(true, false, true);
        assertTrue(f.retainAll(BooleanList.of(true)));
        assertEquals(2, f.size(), "both true elements match the retained value; only the false one goes");
    }
}
