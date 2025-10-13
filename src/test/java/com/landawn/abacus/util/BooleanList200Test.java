package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalBoolean;

@Tag("new-test")
public class BooleanList200Test extends TestBase {

    @Test
    @DisplayName("Test default constructor and basic properties")
    public void testEmptyConstructor() {
        BooleanList list = new BooleanList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        BooleanList list = new BooleanList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test static factory 'of' method")
    public void testOf() {
        BooleanList list = BooleanList.of(true, false, true);
        assertEquals(3, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
        assertTrue(list.get(2));
        assertEquals("[true, false, true]", list.toString());
    }

    @Test
    @DisplayName("Test static factory 'copyOf' method")
    public void testCopyOf() {
        boolean[] original = { true, false, true };
        BooleanList list = BooleanList.copyOf(original);
        assertEquals(3, list.size());
        assertArrayEquals(original, list.toArray());
        original[0] = false;
        assertTrue(list.get(0));
    }

    @Test
    @DisplayName("Test static factory 'copyOf' method with a range")
    public void testCopyOfRange() {
        boolean[] original = { true, false, true, false, true };
        BooleanList list = BooleanList.copyOf(original, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    @DisplayName("Test static factory 'repeat' method")
    public void testRepeat() {
        BooleanList list = BooleanList.repeat(true, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(list.get(i));
        }
        assertEquals("[true, true, true, true, true]", list.toString());
    }

    @Test
    @DisplayName("Test adding elements")
    public void testAdd() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);
        assertEquals(2, list.size());
        assertEquals(true, list.get(0));
        assertEquals(false, list.get(1));
    }

    @Test
    @DisplayName("Test adding an element at a specific index")
    public void testAddAtIndex() {
        BooleanList list = BooleanList.of(true, true);
        list.add(1, false);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
    }

    @Test
    @DisplayName("Test adding all elements from another BooleanList")
    public void testAddAll() {
        BooleanList list = BooleanList.of(true);
        BooleanList toAdd = BooleanList.of(false, true);
        list.addAll(toAdd);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
    }

    @Test
    @DisplayName("Test adding all elements from an array at a specific index")
    public void testAddAllAtIndex() {
        BooleanList list = BooleanList.of(true, true);
        boolean[] toAdd = { false, false };
        list.addAll(1, toAdd);
        assertEquals(4, list.size());
        assertArrayEquals(new boolean[] { true, false, false, true }, list.toArray());
    }

    @Test
    @DisplayName("Test getting an element by index")
    public void testGet() {
        BooleanList list = BooleanList.of(false, true);
        assertTrue(list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @Test
    @DisplayName("Test setting an element at a specific index")
    public void testSet() {
        BooleanList list = BooleanList.of(true, true);
        boolean oldValue = list.set(1, false);
        assertTrue(oldValue);
        assertFalse(list.get(1));
        assertEquals("[true, false]", list.toString());
    }

    @Test
    @DisplayName("Test removing an element by value")
    public void testRemove() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.remove(true));
        assertEquals(2, list.size());
        assertEquals("[false, true]", list.toString());
        assertTrue(list.remove(true));
    }

    @Test
    @DisplayName("Test deleting an element at a specific index")
    public void testDelete() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean deletedValue = list.delete(1);
        assertFalse(deletedValue);
        assertEquals(2, list.size());
        assertEquals("[true, true]", list.toString());
    }

    @Test
    @DisplayName("Test clearing the list")
    public void testClear() {
        BooleanList list = BooleanList.of(true, false);
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test finding the index of an element")
    public void testIndexOf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        assertEquals(0, list.indexOf(true));
        assertEquals(1, list.indexOf(false));
    }

    @Test
    @DisplayName("Test finding the last index of an element")
    public void testLastIndexOf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        assertEquals(2, list.lastIndexOf(true));
        assertEquals(3, list.lastIndexOf(false));
    }

    @Test
    @DisplayName("Test 'contains' method")
    public void testContains() {
        BooleanList list = BooleanList.of(true, false);
        assertTrue(list.contains(true));
        assertTrue(list.contains(false));
    }

    @Test
    @DisplayName("Test 'containsAll' method")
    public void testContainsAll() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.containsAll(BooleanList.of(true, false)));
        assertTrue(list.containsAll(BooleanList.of(true, true, true)));
    }

    @Test
    @DisplayName("Test sorting the list")
    public void testSort() {
        BooleanList list = BooleanList.of(true, false, true, false, false);
        list.sort();
        assertArrayEquals(new boolean[] { false, false, false, true, true }, list.toArray());
    }

    @Test
    @DisplayName("Test reversing the list")
    public void testReverse() {
        BooleanList list = BooleanList.of(true, false, false);
        list.reverse();
        assertArrayEquals(new boolean[] { false, false, true }, list.toArray());
    }

    @Test
    @DisplayName("Test 'equals' and 'hashCode' methods")
    public void testEqualsAndHashCode() {
        BooleanList list1 = BooleanList.of(true, false);
        BooleanList list2 = BooleanList.of(true, false);
        BooleanList list3 = BooleanList.of(false, true);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(list1, null);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());
        assertNotEquals(list1.hashCode(), list3.hashCode());
    }

    @Test
    @DisplayName("Test 'toArray' method")
    public void testToArray() {
        boolean[] arr = { true, false, true };
        BooleanList list = BooleanList.of(arr);
        assertArrayEquals(arr, list.toArray());
    }

    @Test
    @DisplayName("Test 'boxed' method to convert to List<Boolean>")
    public void testBoxed() {
        BooleanList list = BooleanList.of(true, false);
        List<Boolean> boxedList = list.boxed();
        assertEquals(2, boxedList.size());
        assertEquals(Boolean.TRUE, boxedList.get(0));
        assertEquals(Boolean.FALSE, boxedList.get(1));
    }

    @Test
    @DisplayName("Test 'removeIf' method with a predicate")
    public void testRemoveIf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.removeIf(val -> val);
        assertEquals(2, list.size());
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
    }

    @Test
    @DisplayName("Test 'replaceAll' method")
    public void testReplaceAll() {
        BooleanList list = BooleanList.of(true, false, true);
        list.replaceAll(val -> !val);
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    @DisplayName("Test 'getFirst' and 'getLast' methods")
    public void testGetFirstLast() {
        BooleanList list = BooleanList.of(true, false, false, true);
        assertTrue(list.getFirst());
        assertTrue(list.getLast());

        BooleanList emptyList = new BooleanList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::getLast);
    }

    @Test
    @DisplayName("Test 'removeFirst' and 'removeLast' methods")
    public void testRemoveFirstLast() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.removeFirst());
        assertEquals("[false, true]", list.toString());
        assertTrue(list.removeLast());
        assertEquals("[false]", list.toString());
    }

    @Test
    @DisplayName("Test 'intersection' method")
    public void testIntersection() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);
        BooleanList intersection = list1.intersection(list2);
        assertEquals(2, intersection.size());
        assertTrue(intersection.contains(true));
        assertTrue(intersection.contains(false));
    }

    @Test
    @DisplayName("Test 'difference' method")
    public void testDifference() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);
        BooleanList difference = list1.difference(list2);
        assertEquals(1, difference.size());
        assertTrue(difference.get(0));
    }

    @Test
    @DisplayName("Test 'symmetricDifference' method")
    public void testSymmetricDifference() {
        BooleanList list1 = BooleanList.of(true, true, false);
        BooleanList list2 = BooleanList.of(true, false, false);
        BooleanList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new boolean[] { false, true }, symmDiff.toArray());
    }

    @Test
    @DisplayName("Test 'removeDuplicates' method")
    public void testRemoveDuplicates() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        list.removeDuplicates();
        assertArrayEquals(new boolean[] { true, false }, list.toArray());

        BooleanList allSame = BooleanList.of(true, true, true);
        allSame.removeDuplicates();
        assertArrayEquals(new boolean[] { true }, allSame.toArray());
    }

    @Test
    @DisplayName("Test 'stream' method functionality")
    public void testStream() {
        BooleanList list = BooleanList.of(true, false, true);
        long count = list.stream().filter(Boolean::booleanValue).count();
        assertEquals(2, count);
    }

    @Test
    @DisplayName("Test 'copy' method for deep copy")
    public void testCopy() {
        BooleanList original = BooleanList.of(true, false);
        BooleanList copied = original.copy();

        assertNotSame(original, copied);
        assertEquals(original, copied);

        original.add(true);
        assertNotEquals(original, copied);
    }

    @Test
    @DisplayName("Test constructor that wraps an existing array with a given size")
    public void testConstructorWithArrayAndSize() {
        boolean[] data = { true, false, true, false, true };
        BooleanList list = new BooleanList(data, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
        data[0] = false;
        assertFalse(list.get(0), "Internal array modification should be reflected in the list");
    }

    @Test
    @DisplayName("Test 'addFirst' and 'addLast' methods")
    public void testAddFirstLast() {
        BooleanList list = new BooleanList();
        list.addLast(true);
        list.addFirst(false);
        list.addLast(false);
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    @DisplayName("Test removing all occurrences of an element")
    public void testRemoveAllOccurrences() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        assertTrue(list.removeAllOccurrences(true));
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
        assertFalse(list.removeAllOccurrences(true));
    }

    @Test
    @DisplayName("Test deleting elements by multiple indices")
    public void testDeleteAllByIndices() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.deleteAllByIndices(0, 2, 4);
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
    }

    @Test
    @DisplayName("Test deleting a range of elements")
    public void testDeleteRange() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        list.deleteRange(1, 4);
        assertArrayEquals(new boolean[] { true, true }, list.toArray());
    }

    @Test
    @DisplayName("Test retaining all elements from another list")
    public void testRetainAll() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        boolean changed = list.retainAll(BooleanList.of(false, false));
        assertTrue(changed);
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
    }

    @Test
    @DisplayName("Test replacing all occurrences of a value")
    public void testReplaceAllValue() {
        BooleanList list = BooleanList.of(true, false, true);
        int replacements = list.replaceAll(true, false);
        assertEquals(2, replacements);
        assertArrayEquals(new boolean[] { false, false, false }, list.toArray());
    }

    @Test
    @DisplayName("Test 'replaceAll' with a unary operator")
    public void testReplaceAllOperator() {
        BooleanList list = BooleanList.of(true, false, true);
        list.replaceAll(val -> !val);
        assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
    }

    @Test
    @DisplayName("Test replacing a range of elements with an array")
    public void testReplaceRange() {
        BooleanList list = BooleanList.of(true, false, false, true);
        list.replaceRange(1, 3, new boolean[] { true, true, true, true });
        assertArrayEquals(new boolean[] { true, true, true, true, true, true }, list.toArray());
    }

    @Test
    @DisplayName("Test filling a range with a value")
    public void testFillRange() {
        BooleanList list = BooleanList.of(false, false, false, false);
        list.fill(1, 3, true);
        assertArrayEquals(new boolean[] { false, true, true, false }, list.toArray());
    }

    @Test
    @DisplayName("Test 'occurrencesOf' method")
    public void testOccurrencesOf() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        assertEquals(3, list.occurrencesOf(true));
        assertEquals(2, list.occurrencesOf(false));
    }

    @Test
    @DisplayName("Test 'contains', 'containsAny', and 'containsAll' methods")
    public void testContainsMethods() {
        BooleanList list = BooleanList.of(true, false, true);
        assertTrue(list.contains(true));
        assertTrue(list.containsAny(new boolean[] { false, false }));
        assertFalse(list.containsAny(new boolean[] {}));
        assertTrue(list.containsAll(BooleanList.of(true, false)));
        assertTrue(list.containsAll(BooleanList.of(true, true, true)));
    }

    @Test
    @DisplayName("Test 'disjoint' method")
    public void testDisjoint() {
        BooleanList list = BooleanList.of(true, true);
        assertTrue(list.disjoint(BooleanList.of(false, false)));
        assertFalse(list.disjoint(BooleanList.of(true, false)));
    }

    @Test
    @DisplayName("Test set operations: intersection, difference, symmetricDifference")
    public void testSetOperations() {
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

    @Test
    @DisplayName("Test 'hasDuplicates' method")
    public void testHasDuplicates() {
        assertTrue(BooleanList.of(true, false, true).hasDuplicates());
        assertTrue(BooleanList.of(true, true).hasDuplicates());
        assertFalse(BooleanList.of(true, false).hasDuplicates());
        assertFalse(BooleanList.of(true).hasDuplicates());
        assertFalse(new BooleanList().hasDuplicates());
    }

    @Test
    @DisplayName("Test 'isSorted' method")
    public void testIsSorted() {
        assertTrue(BooleanList.of(false, false, true, true).isSorted());
        assertFalse(BooleanList.of(true, false).isSorted());
        assertTrue(BooleanList.of(true, true).isSorted());
        assertTrue(new BooleanList().isSorted());
    }

    @Test
    @DisplayName("Test 'sort' and 'reverseSort' methods")
    public void testSorting() {
        BooleanList list = BooleanList.of(true, false, true, false, false);
        list.sort();
        assertArrayEquals(new boolean[] { false, false, false, true, true }, list.toArray());
        list.reverseSort();
        assertArrayEquals(new boolean[] { true, true, false, false, false }, list.toArray());
    }

    @Test
    @DisplayName("Test 'reverse' method on a range")
    public void testReverseRange() {
        BooleanList list = BooleanList.of(true, false, false, true, true);
        list.reverse(1, 4);
        assertArrayEquals(new boolean[] { true, true, false, false, true }, list.toArray());
    }

    @Test
    @DisplayName("Test 'rotate' method")
    public void testRotate() {
        BooleanList list = BooleanList.of(true, true, false, false);
        list.rotate(2);
        assertArrayEquals(new boolean[] { false, false, true, true }, list.toArray());
        list.rotate(-1);
        assertArrayEquals(new boolean[] { false, true, true, false }, list.toArray());
    }

    @Test
    @DisplayName("Test 'shuffle' method")
    public void testShuffle() {
        BooleanList list1 = BooleanList.of(true, false, true, false, true, false);
        BooleanList list2 = list1.copy();
        list1.shuffle();
        assertEquals(list1.size(), list2.size());
        assertTrue(N.isEqualCollection(list1.toList(), list2.toList()), "Both lists should contain the same elements after shuffle");
    }

    @Test
    @DisplayName("Test 'swap' method")
    public void testSwap() {
        BooleanList list = BooleanList.of(true, false, true);
        list.swap(0, 2);
        assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
        list.swap(0, 1);
        assertArrayEquals(new boolean[] { false, true, true }, list.toArray());
    }

    @Test
    @DisplayName("Test 'split' method")
    public void testSplit() {
        BooleanList list = BooleanList.of(true, false, true, false, true);
        List<BooleanList> chunks = list.split(2);
        assertEquals(3, chunks.size());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(0).toArray());
        assertArrayEquals(new boolean[] { true, false }, chunks.get(1).toArray());
        assertArrayEquals(new boolean[] { true }, chunks.get(2).toArray());
    }

    @Test
    @DisplayName("Test conversion methods: 'toArray', 'boxed', 'toCollection'")
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
    @DisplayName("Test iterator functionality")
    public void testIterator() {
        BooleanList list = BooleanList.of(true, false);
        BooleanIterator it = list.iterator();
        assertTrue(it.hasNext());
        assertTrue(it.nextBoolean());
        assertTrue(it.hasNext());
        assertFalse(it.nextBoolean());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("Test stream functionality on a range")
    public void testStreamRange() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        long trueCount = list.stream(1, 4).filter(b -> b).count();
        assertEquals(2, trueCount);
    }

    @Test
    @DisplayName("Test 'getFirst', 'getLast', 'first', and 'last' methods")
    public void testGetAndOptionalFirstLast() {
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
}
