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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;

@Tag("new-test")
public class BooleanList100Test extends TestBase {

    private BooleanList list;

    @BeforeEach
    public void setUp() {
        list = new BooleanList();
    }

    @Test
    public void testConstructors() {
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

    @Test
    public void testStaticFactoryMethods() {
        BooleanList list1 = BooleanList.of(true, false, true);
        assertEquals(3, list1.size());
        assertEquals(true, list1.get(0));
        assertEquals(false, list1.get(1));
        assertEquals(true, list1.get(2));

        BooleanList list2 = BooleanList.of((boolean[]) null);
        assertEquals(0, list2.size());

        boolean[] arr = { true, false, true, false };
        BooleanList list3 = BooleanList.of(arr, 2);
        assertEquals(2, list3.size());
        assertEquals(true, list3.get(0));
        assertEquals(false, list3.get(1));

        BooleanList list4 = BooleanList.copyOf(arr);
        assertEquals(4, list4.size());
        arr[0] = false;
        assertEquals(true, list4.get(0));

        BooleanList list5 = BooleanList.copyOf(arr, 1, 3);
        assertEquals(2, list5.size());
        assertEquals(false, list5.get(0));
        assertEquals(true, list5.get(1));

        BooleanList list6 = BooleanList.repeat(true, 5);
        assertEquals(5, list6.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(true, list6.get(i));
        }

        BooleanList list7 = BooleanList.random(10);
        assertEquals(10, list7.size());
    }

    @Test
    public void testGetAndSet() {
        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(true, list.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));

        boolean oldValue = list.set(1, true);
        assertEquals(false, oldValue);
        assertEquals(true, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, true));
    }

    @Test
    public void testAdd() {
        list.add(true);
        assertEquals(1, list.size());
        assertEquals(true, list.get(0));

        list.add(false);
        assertEquals(2, list.size());
        assertEquals(false, list.get(1));

        list.add(1, true);
        assertEquals(3, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(false, list.get(2));

        list.add(0, false);
        assertEquals(4, list.size());
        assertEquals(false, list.get(0));

        list.add(list.size(), true);
        assertEquals(5, list.size());
        assertEquals(true, list.get(4));

        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, true));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(6, true));
    }

    @Test
    public void testAddAll() {
        BooleanList other = BooleanList.of(true, false, true);
        assertTrue(list.addAll(other));
        assertEquals(3, list.size());

        assertFalse(list.addAll(new BooleanList()));
        assertEquals(3, list.size());

        BooleanList other2 = BooleanList.of(false, false);
        assertTrue(list.addAll(1, other2));
        assertEquals(5, list.size());
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(false, list.get(3));
        assertEquals(true, list.get(4));

        boolean[] arr = { true, true };
        assertTrue(list.addAll(arr));
        assertEquals(7, list.size());

        boolean[] arr2 = { false };
        assertTrue(list.addAll(0, arr2));
        assertEquals(8, list.size());
        assertEquals(false, list.get(0));
    }

    @Test
    public void testRemove() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        assertTrue(list.remove(false));
        assertEquals(3, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(false, list.get(2));

        list.clear();
        list.add(true);
        assertFalse(list.remove(false));
        assertEquals(1, list.size());

        list.clear();
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);
        assertTrue(list.removeAllOccurrences(true));
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(false, list.get(1));

        assertFalse(list.removeAllOccurrences(true));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveIf() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        assertTrue(list.removeIf(b -> b));
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(false, list.get(1));

        assertFalse(list.removeIf(b -> b));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveDuplicates() {
        list.add(true);
        assertFalse(list.removeDuplicates());
        assertEquals(1, list.size());

        list.clear();
        list.add(true);
        list.add(true);
        assertTrue(list.removeDuplicates());
        assertEquals(1, list.size());
        assertEquals(true, list.get(0));

        list.clear();
        list.add(true);
        list.add(false);
        assertFalse(list.removeDuplicates());
        assertEquals(2, list.size());

        list.clear();
        list.add(true);
        list.add(true);
        list.add(true);
        list.add(false);
        list.add(true);
        assertTrue(list.removeDuplicates());
        assertEquals(2, list.size());
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        BooleanList toRemove = BooleanList.of(true);
        assertTrue(list.removeAll(toRemove));
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(false, list.get(1));

        list.clear();
        list.add(true);
        list.add(false);
        list.add(true);
        assertTrue(list.removeAll(new boolean[] { true }));
        assertEquals(1, list.size());
        assertEquals(false, list.get(0));

        list.clear();
        list.add(true);
        list.add(false);
        list.add(true);
        BooleanList toRetain = BooleanList.of(true);
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));

        list.clear();
        list.add(true);
        list.add(false);
        list.add(true);
        assertTrue(list.retainAll(new boolean[] { false }));
        assertEquals(1, list.size());
        assertEquals(false, list.get(0));
    }

    @Test
    public void testDelete() {
        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(false, list.delete(1));
        assertEquals(2, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(-1));
    }

    @Test
    public void testDeleteAllByIndices() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(true, list.get(2));

        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    public void testDeleteRange() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));

        list.deleteRange(1, 1);
        assertEquals(2, list.size());

        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(-1, 1));
    }

    @Test
    public void testMoveRange() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        list.moveRange(1, 3, 0);
        assertEquals(5, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(true, list.get(2));
        assertEquals(false, list.get(3));
        assertEquals(true, list.get(4));
    }

    @Test
    public void testReplaceRange() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        BooleanList replacement = BooleanList.of(false, false, false);
        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(false, list.get(3));
        assertEquals(false, list.get(4));

        list.clear();
        list.add(true);
        list.add(false);
        list.add(true);
        list.replaceRange(1, 2, new boolean[] { true, true });
        assertEquals(4, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(true, list.get(2));
        assertEquals(true, list.get(3));

        list.replaceRange(1, 3, new boolean[0]);
        assertEquals(2, list.size());
    }

    @Test
    public void testReplaceAll() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        assertEquals(2, list.replaceAll(true, false));
        assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(false, list.get(i));
        }

        list.replaceAll(b -> !b);
        assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(true, list.get(i));
        }

        assertTrue(list.replaceIf(b -> b, false));
        assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(false, list.get(i));
        }

        assertFalse(list.replaceIf(b -> b, true));
    }

    @Test
    public void testFill() {
        list.add(true);
        list.add(false);
        list.add(true);

        list.fill(false);
        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(false, list.get(i));
        }

        list.fill(1, 3, true);
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(true, list.get(2));
    }

    @Test
    public void testContains() {
        list.add(true);
        list.add(false);

        assertTrue(list.contains(true));
        assertTrue(list.contains(false));

        list.clear();
        assertFalse(list.contains(true));
        assertFalse(list.contains(false));
    }

    @Test
    public void testContainsAnyAndAll() {
        list.add(true);
        list.add(false);
        list.add(true);

        assertTrue(list.containsAny(BooleanList.of(true)));
        assertTrue(list.containsAny(BooleanList.of(false)));
        assertTrue(list.containsAny(BooleanList.of(true, false)));
        assertFalse(list.containsAny(new BooleanList()));

        assertTrue(list.containsAny(new boolean[] { true }));
        assertTrue(list.containsAny(new boolean[] { false }));

        assertTrue(list.containsAll(BooleanList.of(true, false)));
        assertTrue(list.containsAll(BooleanList.of(true)));
        assertTrue(list.containsAll(BooleanList.of(false)));
        assertTrue(list.containsAll(new BooleanList()));

        assertTrue(list.containsAll(new boolean[] { true, false }));
        assertTrue(list.containsAll(new boolean[0]));

        list.clear();
        assertFalse(list.containsAny(BooleanList.of(true)));
        assertFalse(list.containsAll(BooleanList.of(true)));
        assertTrue(list.containsAll(new BooleanList()));
    }

    @Test
    public void testDisjoint() {
        list.add(true);
        list.add(false);

        assertFalse(list.disjoint(BooleanList.of(true)));
        assertFalse(list.disjoint(BooleanList.of(false)));
        assertFalse(list.disjoint(BooleanList.of(true, false)));
        assertTrue(list.disjoint(new BooleanList()));

        assertFalse(list.disjoint(new boolean[] { true }));
        assertFalse(list.disjoint(new boolean[] { false }));
        assertTrue(list.disjoint(new boolean[0]));

        list.clear();
        assertTrue(list.disjoint(BooleanList.of(true)));
        assertTrue(list.disjoint(new boolean[] { false }));
    }

    @Test
    public void testIntersection() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        BooleanList other = BooleanList.of(true, true, false);
        BooleanList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals(true, result.get(0));
        assertEquals(false, result.get(1));
        assertEquals(true, result.get(2));

        result = list.intersection(new boolean[] { false, false });
        assertEquals(2, result.size());
        assertEquals(false, result.get(0));
        assertEquals(false, result.get(1));

        result = list.intersection(new BooleanList());
        assertEquals(0, result.size());
    }

    @Test
    public void testDifference() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        BooleanList other = BooleanList.of(true, false);
        BooleanList result = list.difference(other);
        assertEquals(2, result.size());
        assertEquals(true, result.get(0));
        assertEquals(false, result.get(1));

        result = list.difference(new boolean[] { true, true });
        assertEquals(2, result.size());
        assertEquals(false, result.get(0));
        assertEquals(false, result.get(1));

        result = list.difference(new BooleanList());
        assertEquals(4, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList other = BooleanList.of(false, false, true);
        BooleanList result = list.symmetricDifference(other);
        assertEquals(2, result.size());
        assertEquals(true, result.get(0));
        assertEquals(false, result.get(1));

        result = list.symmetricDifference(new boolean[] { true, true, true, false });
        assertEquals(1, result.size());
        assertEquals(true, result.get(0));

        result = list.symmetricDifference(new BooleanList());
        assertEquals(3, result.size());
    }

    @Test
    public void testIndexOf() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        assertEquals(0, list.indexOf(true));
        assertEquals(1, list.indexOf(false));

        assertEquals(2, list.indexOf(true, 1));
        assertEquals(3, list.indexOf(false, 2));
        assertEquals(-1, list.indexOf(true, 3));
        assertEquals(-1, list.indexOf(false, 4));

        list.clear();
        assertEquals(-1, list.indexOf(true));
        assertEquals(-1, list.indexOf(false));
    }

    @Test
    public void testLastIndexOf() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        assertEquals(2, list.lastIndexOf(true));
        assertEquals(3, list.lastIndexOf(false));

        assertEquals(0, list.lastIndexOf(true, 1));
        assertEquals(1, list.lastIndexOf(false, 2));
        assertEquals(-1, list.lastIndexOf(true, -1));

        list.clear();
        assertEquals(-1, list.lastIndexOf(true));
        assertEquals(-1, list.lastIndexOf(false));
    }

    @Test
    public void testOccurrencesOf() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(3, list.occurrencesOf(true));
        assertEquals(2, list.occurrencesOf(false));

        list.clear();
        assertEquals(0, list.occurrencesOf(true));
        assertEquals(0, list.occurrencesOf(false));
    }

    @Test
    public void testForEach() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> collected = new ArrayList<>();
        list.forEach(b -> collected.add(b));
        assertEquals(3, collected.size());
        assertEquals(true, collected.get(0));
        assertEquals(false, collected.get(1));
        assertEquals(true, collected.get(2));

        collected.clear();
        list.forEach(1, 3, b -> collected.add(b));
        assertEquals(2, collected.size());
        assertEquals(false, collected.get(0));
        assertEquals(true, collected.get(1));

        collected.clear();
        list.forEach(2, 0, b -> collected.add(b));
        assertEquals(2, collected.size());
        assertEquals(true, collected.get(0));
        assertEquals(false, collected.get(1));
    }

    @Test
    public void testFirstAndLast() {
        OptionalBoolean first = list.first();
        OptionalBoolean last = list.last();
        assertFalse(first.isPresent());
        assertFalse(last.isPresent());

        list.add(true);
        list.add(false);
        list.add(true);

        first = list.first();
        last = list.last();
        assertTrue(first.isPresent());
        assertTrue(last.isPresent());
        assertEquals(true, first.get());
        assertEquals(true, last.get());
    }

    @Test
    public void testDistinct() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList distinct = list.distinct(0, list.size());
        assertEquals(2, distinct.size());
        assertTrue(distinct.contains(true));
        assertTrue(distinct.contains(false));

        distinct = list.distinct(1, 4);
        assertEquals(2, distinct.size());
    }

    @Test
    public void testHasDuplicates() {
        assertFalse(list.hasDuplicates());

        list.add(true);
        assertFalse(list.hasDuplicates());

        list.add(true);
        assertTrue(list.hasDuplicates());

        list.clear();
        list.add(true);
        list.add(false);
        assertFalse(list.hasDuplicates());

        list.add(true);
        assertTrue(list.hasDuplicates());
    }

    @Test
    public void testIsSorted() {
        assertTrue(list.isSorted());

        list.add(true);
        assertTrue(list.isSorted());

        list.clear();
        list.add(false);
        list.add(false);
        list.add(true);
        list.add(true);
        assertTrue(list.isSorted());

        list.clear();
        list.add(true);
        list.add(false);
        assertFalse(list.isSorted());
    }

    @Test
    public void testSort() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        list.sort();
        assertEquals(5, list.size());
        assertEquals(false, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(true, list.get(2));
        assertEquals(true, list.get(3));
        assertEquals(true, list.get(4));

        list.clear();
        list.sort();
        assertEquals(0, list.size());

        list.add(true);
        list.sort();
        assertEquals(1, list.size());
        assertEquals(true, list.get(0));
    }

    @Test
    public void testReverseSort() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        list.reverseSort();
        assertEquals(4, list.size());
        assertEquals(true, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(false, list.get(3));
    }

    @Test
    public void testReverse() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        list.reverse();
        assertEquals(4, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(true, list.get(3));

        list.reverse(1, 3);
        assertEquals(false, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(true, list.get(2));
        assertEquals(true, list.get(3));
    }

    @Test
    public void testRotate() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);

        list.rotate(1);
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(true, list.get(3));

        list.rotate(-2);
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(false, list.get(2));
        assertEquals(true, list.get(3));
    }

    @Test
    public void testShuffle() {
        for (int i = 0; i < 10; i++) {
            list.add(i % 2 == 0);
        }

        BooleanList original = list.copy();
        list.shuffle();
        assertEquals(original.size(), list.size());

        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }

        list.shuffle(new Random(42));
        assertEquals(original.size(), list.size());
    }

    @Test
    public void testSwap() {
        list.add(true);
        list.add(false);
        list.add(true);

        list.swap(0, 2);
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
        assertEquals(true, list.get(2));

        list.swap(0, 1);
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopy() {
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList copy = list.copy();
        assertEquals(list.size(), copy.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i), copy.get(i));
        }

        copy.set(0, false);
        assertEquals(true, list.get(0));
        assertEquals(false, copy.get(0));

        BooleanList partialCopy = list.copy(1, 3);
        assertEquals(2, partialCopy.size());
        assertEquals(false, partialCopy.get(0));
        assertEquals(true, partialCopy.get(1));

        list.clear();
        for (int i = 0; i < 6; i++) {
            list.add(i % 2 == 0);
        }
        BooleanList steppedCopy = list.copy(0, 6, 2);
        assertEquals(3, steppedCopy.size());
        assertEquals(true, steppedCopy.get(0));
        assertEquals(true, steppedCopy.get(1));
        assertEquals(true, steppedCopy.get(2));
    }

    @Test
    public void testSplit() {
        for (int i = 0; i < 7; i++) {
            list.add(i % 2 == 0);
        }

        List<BooleanList> splits = list.split(0, list.size(), 3);
        assertEquals(3, splits.size());
        assertEquals(3, splits.get(0).size());
        assertEquals(3, splits.get(1).size());
        assertEquals(1, splits.get(2).size());

        assertEquals(true, splits.get(0).get(0));
        assertEquals(false, splits.get(0).get(1));
        assertEquals(true, splits.get(0).get(2));
    }

    @Test
    public void testTrimToSize() {
        list.add(true);
        list.add(false);

        BooleanList trimmed = list.trimToSize();
        assertSame(list, trimmed);
        assertEquals(2, list.size());
    }

    @Test
    public void testClear() {
        list.add(true);
        list.add(false);
        list.add(true);

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add(true);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add(true);
        assertEquals(1, list.size());

        list.add(false);
        assertEquals(2, list.size());

        list.remove(true);
        assertEquals(1, list.size());
    }

    @Test
    public void testBoxed() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Boolean.TRUE, boxed.get(0));
        assertEquals(Boolean.FALSE, boxed.get(1));
        assertEquals(Boolean.TRUE, boxed.get(2));

        List<Boolean> partialBoxed = list.boxed(1, 3);
        assertEquals(2, partialBoxed.size());
        assertEquals(Boolean.FALSE, partialBoxed.get(0));
        assertEquals(Boolean.TRUE, partialBoxed.get(1));
    }

    @Test
    public void testToArray() {
        list.add(true);
        list.add(false);
        list.add(true);

        boolean[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(true, array[0]);
        assertEquals(false, array[1]);
        assertEquals(true, array[2]);

        array[0] = false;
        assertEquals(true, list.get(0));
    }

    @Test
    public void testToCollection() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> collection = list.toCollection(0, list.size(), ArrayList::new);
        assertEquals(3, collection.size());
        assertEquals(Boolean.TRUE, collection.get(0));
        assertEquals(Boolean.FALSE, collection.get(1));
        assertEquals(Boolean.TRUE, collection.get(2));
    }

    @Test
    public void testToMultiset() {
        list.add(true);
        list.add(false);
        list.add(true);
        list.add(false);
        list.add(true);

        Multiset<Boolean> multiset = list.toMultiset(0, list.size(), Multiset::new);
        assertEquals(3, multiset.count(Boolean.TRUE));
        assertEquals(2, multiset.count(Boolean.FALSE));
    }

    @Test
    public void testIterator() {
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(true, iter.nextBoolean());
        assertTrue(iter.hasNext());
        assertEquals(false, iter.nextBoolean());
        assertTrue(iter.hasNext());
        assertEquals(true, iter.nextBoolean());
        assertFalse(iter.hasNext());

        BooleanList emptyList = new BooleanList();
        BooleanIterator emptyIter = emptyList.iterator();
        assertFalse(emptyIter.hasNext());
    }

    @Test
    public void testStream() {
        list.add(true);
        list.add(false);
        list.add(true);

        List<Boolean> streamResult = list.stream().toList();
        assertEquals(3, streamResult.size());
        assertEquals(Boolean.TRUE, streamResult.get(0));
        assertEquals(Boolean.FALSE, streamResult.get(1));
        assertEquals(Boolean.TRUE, streamResult.get(2));

        List<Boolean> partialStreamResult = list.stream(1, 3).toList();
        assertEquals(2, partialStreamResult.size());
        assertEquals(Boolean.FALSE, partialStreamResult.get(0));
        assertEquals(Boolean.TRUE, partialStreamResult.get(1));
    }

    @Test
    public void testGetFirstAndGetLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(true, list.getFirst());
        assertEquals(true, list.getLast());
    }

    @Test
    public void testAddFirstAndAddLast() {
        list.add(true);

        list.addFirst(false);
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));

        list.addLast(true);
        assertEquals(3, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));
        assertEquals(true, list.get(2));
    }

    @Test
    public void testRemoveFirstAndRemoveLast() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
        assertThrows(NoSuchElementException.class, () -> list.removeLast());

        list.add(true);
        list.add(false);
        list.add(true);

        assertEquals(true, list.removeFirst());
        assertEquals(2, list.size());
        assertEquals(false, list.get(0));
        assertEquals(true, list.get(1));

        assertEquals(true, list.removeLast());
        assertEquals(1, list.size());
        assertEquals(false, list.get(0));
    }

    @Test
    public void testHashCode() {
        list.add(true);
        list.add(false);

        BooleanList other = new BooleanList();
        other.add(true);
        other.add(false);

        assertEquals(list.hashCode(), other.hashCode());

        other.add(true);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
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

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add(true);
        list.add(false);
        list.add(true);
        assertEquals("[true, false, true]", list.toString());
    }

    @Test
    public void testArray() {
        list.add(true);
        list.add(false);
        list.add(true);

        boolean[] array = list.array();
        assertEquals(true, array[0]);
        assertEquals(false, array[1]);
        assertEquals(true, array[2]);

        array[0] = false;
        assertEquals(false, list.get(0));
    }
}
