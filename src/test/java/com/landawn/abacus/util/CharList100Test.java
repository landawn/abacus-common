package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.CharStream;

public class CharList100Test extends TestBase {

    private CharList list;

    @BeforeEach
    public void setUp() {
        list = new CharList();
    }

    // Constructor Tests
    @Test
    @DisplayName("Test default constructor")
    public void testDefaultConstructor() {
        CharList newList = new CharList();
        assertTrue(newList.isEmpty());
        assertEquals(0, newList.size());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        CharList newList = new CharList(20);
        assertTrue(newList.isEmpty());
        assertEquals(0, newList.size());
    }

    @Test
    @DisplayName("Test constructor with array")
    public void testConstructorWithArray() {
        char[] array = { 'a', 'b', 'c' };
        CharList newList = new CharList(array);
        assertEquals(3, newList.size());
        assertEquals('a', newList.get(0));
        assertEquals('b', newList.get(1));
        assertEquals('c', newList.get(2));
    }

    @Test
    @DisplayName("Test constructor with array and size")
    public void testConstructorWithArrayAndSize() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharList newList = new CharList(array, 3);
        assertEquals(3, newList.size());
        assertEquals('a', newList.get(0));
        assertEquals('b', newList.get(1));
        assertEquals('c', newList.get(2));
    }

    @Test
    @DisplayName("Test constructor with invalid size throws exception")
    public void testConstructorWithInvalidSize() {
        char[] array = { 'a', 'b', 'c' };
        assertThrows(IndexOutOfBoundsException.class, () -> new CharList(array, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> new CharList(array, -1));
    }

    // Static Factory Method Tests
    @Test
    @DisplayName("Test of() factory method")
    public void testOf() {
        CharList newList = CharList.of('a', 'b', 'c');
        assertEquals(3, newList.size());
        assertEquals('a', newList.get(0));
        assertEquals('b', newList.get(1));
        assertEquals('c', newList.get(2));
    }

    @Test
    @DisplayName("Test of() with null array")
    public void testOfWithNull() {
        CharList newList = CharList.of((char[]) null);
        assertTrue(newList.isEmpty());
    }

    @Test
    @DisplayName("Test of() with array and size")
    public void testOfWithArrayAndSize() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharList newList = CharList.of(array, 3);
        assertEquals(3, newList.size());
    }

    @Test
    @DisplayName("Test copyOf()")
    public void testCopyOf() {
        char[] array = { 'a', 'b', 'c' };
        CharList newList = CharList.copyOf(array);
        assertEquals(3, newList.size());

        // Verify it's a copy
        array[0] = 'z';
        assertEquals('a', newList.get(0));
    }

    @Test
    @DisplayName("Test copyOf() with range")
    public void testCopyOfWithRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharList newList = CharList.copyOf(array, 1, 4);
        assertEquals(3, newList.size());
        assertEquals('b', newList.get(0));
        assertEquals('c', newList.get(1));
        assertEquals('d', newList.get(2));
    }

    // Range Tests
    @Test
    @DisplayName("Test range()")
    public void testRange() {
        CharList newList = CharList.range('a', 'f');
        assertEquals(5, newList.size());
        assertEquals('a', newList.get(0));
        assertEquals('e', newList.get(4));
    }

    @Test
    @DisplayName("Test range() with step")
    public void testRangeWithStep() {
        CharList newList = CharList.range('a', 'j', 2);
        assertEquals(5, newList.size());
        assertEquals('a', newList.get(0));
        assertEquals('c', newList.get(1));
        assertEquals('i', newList.get(4));
    }

    @Test
    @DisplayName("Test rangeClosed()")
    public void testRangeClosed() {
        CharList newList = CharList.rangeClosed('a', 'e');
        assertEquals(5, newList.size());
        assertEquals('a', newList.get(0));
        assertEquals('e', newList.get(4));
    }

    @Test
    @DisplayName("Test repeat()")
    public void testRepeat() {
        CharList newList = CharList.repeat('x', 5);
        assertEquals(5, newList.size());
        for (int i = 0; i < 5; i++) {
            assertEquals('x', newList.get(i));
        }
    }

    // Random Tests
    @Test
    @DisplayName("Test random() generates list of specified length")
    public void testRandom() {
        CharList newList = CharList.random(10);
        assertEquals(10, newList.size());
    }

    @Test
    @DisplayName("Test random() with range")
    public void testRandomWithRange() {
        CharList newList = CharList.random('a', 'f', 100);
        assertEquals(100, newList.size());
        for (int i = 0; i < 100; i++) {
            char c = newList.get(i);
            assertTrue(c >= 'a' && c < 'f');
        }
    }

    @Test
    @DisplayName("Test random() with candidates")
    public void testRandomWithCandidates() {
        char[] candidates = { 'x', 'y', 'z' };
        CharList newList = CharList.random(candidates, 20);
        assertEquals(20, newList.size());
        for (int i = 0; i < 20; i++) {
            char c = newList.get(i);
            assertTrue(c == 'x' || c == 'y' || c == 'z');
        }
    }

    // Basic Operations Tests
    @Test
    @DisplayName("Test get() and set()")
    public void testGetAndSet() {
        list.add('a');
        list.add('b');
        list.add('c');

        assertEquals('b', list.get(1));
        char oldValue = list.set(1, 'x');
        assertEquals('b', oldValue);
        assertEquals('x', list.get(1));
    }

    @Test
    @DisplayName("Test get() with invalid index")
    public void testGetWithInvalidIndex() {
        list.add('a');
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    @DisplayName("Test add()")
    public void testAdd() {
        list.add('a');
        list.add('b');
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    @DisplayName("Test add() at index")
    public void testAddAtIndex() {
        list.add('a');
        list.add('c');
        list.add(1, 'b');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test addAll() with CharList")
    public void testAddAllCharList() {
        list.add('a');
        CharList other = CharList.of('b', 'c', 'd');
        boolean modified = list.addAll(other);
        assertTrue(modified);
        assertEquals(4, list.size());
        assertEquals('d', list.get(3));
    }

    @Test
    @DisplayName("Test addAll() at index with CharList")
    public void testAddAllAtIndexCharList() {
        list.add('a');
        list.add('e');
        CharList other = CharList.of('b', 'c', 'd');
        boolean modified = list.addAll(1, other);
        assertTrue(modified);
        assertEquals(5, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('e', list.get(4));
    }

    @Test
    @DisplayName("Test addAll() with array")
    public void testAddAllArray() {
        list.add('a');
        char[] array = { 'b', 'c', 'd' };
        boolean modified = list.addAll(array);
        assertTrue(modified);
        assertEquals(4, list.size());
    }

    @Test
    @DisplayName("Test addFirst() and addLast()")
    public void testAddFirstAndLast() {
        list.add('b');
        list.addFirst('a');
        list.addLast('c');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    // Remove Operations Tests
    @Test
    @DisplayName("Test remove()")
    public void testRemove() {
        list.addAll(CharList.of('a', 'b', 'c', 'b'));
        boolean removed = list.remove('b');
        assertTrue(removed);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('b', list.get(2));
    }

    @Test
    @DisplayName("Test remove() non-existent element")
    public void testRemoveNonExistent() {
        list.addAll(CharList.of('a', 'b', 'c'));
        boolean removed = list.remove('d');
        assertFalse(removed);
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test removeAllOccurrences()")
    public void testRemoveAllOccurrences() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'b', 'd'));
        boolean removed = list.removeAllOccurrences('b');
        assertTrue(removed);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('d', list.get(2));
    }

    @Test
    @DisplayName("Test removeAll()")
    public void testRemoveAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        CharList toRemove = CharList.of('b', 'd', 'f');
        boolean modified = list.removeAll(toRemove);
        assertTrue(modified);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('e', list.get(2));
    }

    @Test
    @DisplayName("Test removeIf()")
    public void testRemoveIf() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        boolean modified = list.removeIf(c -> c > 'c');
        assertTrue(modified);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test removeDuplicates()")
    public void testRemoveDuplicates() {
        list.addAll(CharList.of('a', 'b', 'b', 'c', 'a', 'd', 'c'));
        boolean modified = list.removeDuplicates();
        assertTrue(modified);
        assertEquals(4, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
        assertEquals('d', list.get(3));
    }

    @Test
    @DisplayName("Test removeFirst() and removeLast()")
    public void testRemoveFirstAndLast() {
        list.addAll(CharList.of('a', 'b', 'c'));
        char first = list.removeFirst();
        assertEquals('a', first);
        assertEquals(2, list.size());

        char last = list.removeLast();
        assertEquals('c', last);
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("Test removeFirst() on empty list")
    public void testRemoveFirstOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    // Delete Operations Tests
    @Test
    @DisplayName("Test delete()")
    public void testDelete() {
        list.addAll(CharList.of('a', 'b', 'c'));
        char deleted = list.delete(1);
        assertEquals('b', deleted);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
    }

    @Test
    @DisplayName("Test deleteAllByIndices()")
    public void testDeleteAllByIndices() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('e', list.get(2));
    }

    @Test
    @DisplayName("Test deleteRange()")
    public void testDeleteRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('e', list.get(1));
    }

    // Retain Operations Tests
    @Test
    @DisplayName("Test retainAll()")
    public void testRetainAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        CharList toRetain = CharList.of('b', 'd', 'f');
        boolean modified = list.retainAll(toRetain);
        assertTrue(modified);
        assertEquals(2, list.size());
        assertEquals('b', list.get(0));
        assertEquals('d', list.get(1));
    }

    // Replace Operations Tests
    @Test
    @DisplayName("Test replaceAll(char, char)")
    public void testReplaceAllValues() {
        list.addAll(CharList.of('a', 'b', 'a', 'c', 'a'));
        int count = list.replaceAll('a', 'x');
        assertEquals(3, count);
        assertEquals('x', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('x', list.get(2));
    }

    @Test
    @DisplayName("Test replaceAll(CharUnaryOperator)")
    public void testReplaceAllOperator() {
        list.addAll(CharList.of('a', 'b', 'c'));
        list.replaceAll(c -> (char) (c + 1));
        assertEquals('b', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('d', list.get(2));
    }

    @Test
    @DisplayName("Test replaceIf()")
    public void testReplaceIf() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        boolean modified = list.replaceIf(c -> c > 'c', 'x');
        assertTrue(modified);
        assertEquals('a', list.get(0));
        assertEquals('x', list.get(3));
        assertEquals('x', list.get(4));
    }

    @Test
    @DisplayName("Test replaceRange()")
    public void testReplaceRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        CharList replacement = CharList.of('x', 'y');
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals('a', list.get(0));
        assertEquals('x', list.get(1));
        assertEquals('y', list.get(2));
        assertEquals('e', list.get(3));
    }

    // Fill Operations Tests
    @Test
    @DisplayName("Test fill()")
    public void testFill() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.fill('x');
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals('x', list.get(i));
        }
    }

    @Test
    @DisplayName("Test fill() with range")
    public void testFillRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.fill(1, 4, 'x');
        assertEquals('a', list.get(0));
        assertEquals('x', list.get(1));
        assertEquals('x', list.get(2));
        assertEquals('x', list.get(3));
        assertEquals('e', list.get(4));
    }

    // Contains Operations Tests
    @Test
    @DisplayName("Test contains()")
    public void testContains() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertTrue(list.contains('b'));
        assertFalse(list.contains('d'));
    }

    @Test
    @DisplayName("Test containsAny()")
    public void testContainsAny() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList other = CharList.of('d', 'e', 'b');
        assertTrue(list.containsAny(other));

        CharList noMatch = CharList.of('x', 'y', 'z');
        assertFalse(list.containsAny(noMatch));
    }

    @Test
    @DisplayName("Test containsAll()")
    public void testContainsAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        CharList subset = CharList.of('b', 'd');
        assertTrue(list.containsAll(subset));

        CharList notSubset = CharList.of('b', 'e');
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    @DisplayName("Test disjoint()")
    public void testDisjoint() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList other = CharList.of('d', 'e', 'f');
        assertTrue(list.disjoint(other));

        CharList overlap = CharList.of('c', 'd', 'e');
        assertFalse(list.disjoint(overlap));
    }

    // Set Operations Tests
    @Test
    @DisplayName("Test intersection()")
    public void testIntersection() {
        list.addAll(CharList.of('a', 'b', 'c', 'b'));
        CharList other = CharList.of('b', 'c', 'd', 'b');
        CharList result = list.intersection(other);
        assertEquals(3, result.size());
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
    }

    @Test
    @DisplayName("Test difference()")
    public void testDifference() {
        list.addAll(CharList.of('a', 'b', 'c', 'b'));
        CharList other = CharList.of('b', 'c');
        CharList result = list.difference(other);
        assertEquals(2, result.size());
        assertEquals('a', result.get(0));
        assertEquals('b', result.get(1));
    }

    @Test
    @DisplayName("Test symmetricDifference()")
    public void testSymmetricDifference() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList other = CharList.of('b', 'c', 'd', 'e');
        CharList result = list.symmetricDifference(other);
        assertEquals(3, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('d'));
        assertTrue(result.contains('e'));
    }

    // Search Operations Tests
    @Test
    @DisplayName("Test indexOf()")
    public void testIndexOf() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'e'));
        assertEquals(1, list.indexOf('b'));
        assertEquals(-1, list.indexOf('x'));
        assertEquals(2, list.indexOf('c', 1));
    }

    @Test
    @DisplayName("Test lastIndexOf()")
    public void testLastIndexOf() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'e'));
        assertEquals(3, list.lastIndexOf('b'));
        assertEquals(-1, list.lastIndexOf('x'));
        assertEquals(1, list.lastIndexOf('b', 2));
    }

    @Test
    @DisplayName("Test occurrencesOf()")
    public void testOccurrencesOf() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'b'));
        assertEquals(3, list.occurrencesOf('b'));
        assertEquals(0, list.occurrencesOf('x'));
    }

    @Test
    @DisplayName("Test binarySearch()")
    public void testBinarySearch() {
        list.addAll(CharList.of('a', 'c', 'e', 'g', 'i'));
        int index = list.binarySearch('e');
        assertEquals(2, index);

        int notFound = list.binarySearch('f');
        assertTrue(notFound < 0);
    }

    // Min/Max Operations Tests
    @Test
    @DisplayName("Test min()")
    public void testMin() {
        list.addAll(CharList.of('c', 'a', 'e', 'b', 'd'));
        OptionalChar min = list.min();
        assertTrue(min.isPresent());
        assertEquals('a', min.get());

        CharList empty = new CharList();
        assertFalse(empty.min().isPresent());
    }

    @Test
    @DisplayName("Test max()")
    public void testMax() {
        list.addAll(CharList.of('c', 'a', 'e', 'b', 'd'));
        OptionalChar max = list.max();
        assertTrue(max.isPresent());
        assertEquals('e', max.get());
    }

    @Test
    @DisplayName("Test median()")
    public void testMedian() {
        list.addAll(CharList.of('c', 'a', 'e', 'b', 'd'));
        OptionalChar median = list.median();
        assertTrue(median.isPresent());
        assertEquals('c', median.get());
    }

    // Sort and Order Operations Tests
    @Test
    @DisplayName("Test sort()")
    public void testSort() {
        list.addAll(CharList.of('c', 'a', 'e', 'b', 'd'));
        list.sort();
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
        assertEquals('d', list.get(3));
        assertEquals('e', list.get(4));
    }

    @Test
    @DisplayName("Test reverseSort()")
    public void testReverseSort() {
        list.addAll(CharList.of('c', 'a', 'e', 'b', 'd'));
        list.reverseSort();
        assertEquals('e', list.get(0));
        assertEquals('d', list.get(1));
        assertEquals('c', list.get(2));
        assertEquals('b', list.get(3));
        assertEquals('a', list.get(4));
    }

    @Test
    @DisplayName("Test reverse()")
    public void testReverse() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.reverse();
        assertEquals('e', list.get(0));
        assertEquals('d', list.get(1));
        assertEquals('a', list.get(4));
    }

    @Test
    @DisplayName("Test rotate()")
    public void testRotate() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.rotate(2);
        assertEquals('d', list.get(0));
        assertEquals('e', list.get(1));
        assertEquals('a', list.get(2));
    }

    @Test
    @DisplayName("Test shuffle()")
    public void testShuffle() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        CharList original = list.copy();
        list.shuffle();
        assertEquals(original.size(), list.size());

        // Check all elements are still present
        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }
    }

    @Test
    @DisplayName("Test swap()")
    public void testSwap() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.swap(1, 3);
        assertEquals('d', list.get(1));
        assertEquals('b', list.get(3));
    }

    // Utility Operations Tests
    @Test
    @DisplayName("Test distinct()")
    public void testDistinct() {
        list.addAll(CharList.of('a', 'b', 'b', 'c', 'a', 'd', 'c'));
        CharList distinct = list.distinct();
        assertEquals(4, distinct.size());
        assertTrue(distinct.contains('a'));
        assertTrue(distinct.contains('b'));
        assertTrue(distinct.contains('c'));
        assertTrue(distinct.contains('d'));
    }

    @Test
    @DisplayName("Test hasDuplicates()")
    public void testHasDuplicates() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertFalse(list.hasDuplicates());

        list.add('b');
        assertTrue(list.hasDuplicates());
    }

    @Test
    @DisplayName("Test isSorted()")
    public void testIsSorted() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        assertTrue(list.isSorted());

        list.add('a');
        assertFalse(list.isSorted());
    }

    // Copy Operations Tests
    @Test
    @DisplayName("Test copy()")
    public void testCopy() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList copy = list.copy();
        assertEquals(list.size(), copy.size());

        // Verify deep copy
        list.set(0, 'x');
        assertEquals('a', copy.get(0));
    }

    @Test
    @DisplayName("Test copy() with range")
    public void testCopyRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        CharList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals('b', copy.get(0));
        assertEquals('c', copy.get(1));
        assertEquals('d', copy.get(2));
    }

    @Test
    @DisplayName("Test copy() with step")
    public void testCopyWithStep() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'f'));
        CharList copy = list.copy(0, 6, 2);
        assertEquals(3, copy.size());
        assertEquals('a', copy.get(0));
        assertEquals('c', copy.get(1));
        assertEquals('e', copy.get(2));
    }

    // Split Operations Tests
    @Test
    @DisplayName("Test split()")
    public void testSplit() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'f', 'g'));
        List<CharList> chunks = list.split(3);
        assertEquals(3, chunks.size());
        assertEquals(3, chunks.get(0).size());
        assertEquals(3, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    // Conversion Operations Tests
    @Test
    @DisplayName("Test toArray()")
    public void testToArray() {
        list.addAll(CharList.of('a', 'b', 'c'));
        char[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals('a', array[0]);
        assertEquals('b', array[1]);
        assertEquals('c', array[2]);
    }

    @Test
    @DisplayName("Test toIntList()")
    public void testToIntList() {
        list.addAll(CharList.of('a', 'b', 'c'));
        IntList intList = list.toIntList();
        assertEquals(3, intList.size());
        assertEquals(97, intList.get(0)); // 'a' = 97
        assertEquals(98, intList.get(1)); // 'b' = 98
        assertEquals(99, intList.get(2)); // 'c' = 99
    }

    @Test
    @DisplayName("Test boxed()")
    public void testBoxed() {
        list.addAll(CharList.of('a', 'b', 'c'));
        List<Character> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Character.valueOf('a'), boxed.get(0));
        assertEquals(Character.valueOf('b'), boxed.get(1));
        assertEquals(Character.valueOf('c'), boxed.get(2));
    }

    @Test
    @DisplayName("Test toSet()")
    public void testToSet() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'a'));
        Set<Character> set = list.toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains('a'));
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
    }

    @Test
    @DisplayName("Test toMultiset()")
    public void testToMultiset() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'a', 'a'));
        Multiset<Character> multiset = list.toMultiset();
        assertEquals(3, multiset.occurrencesOf('a'));
        assertEquals(2, multiset.occurrencesOf('b'));
        assertEquals(1, multiset.occurrencesOf('c'));
    }

    // Iterator and Stream Tests
    @Test
    @DisplayName("Test iterator()")
    public void testIterator() {
        list.addAll(CharList.of('a', 'b', 'c'));
        Iterator<Character> iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(Character.valueOf('a'), iter.next());
        assertEquals(Character.valueOf('b'), iter.next());
        assertEquals(Character.valueOf('c'), iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test stream()")
    public void testStream() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharStream stream = list.stream();
        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    // First and Last Operations Tests
    @Test
    @DisplayName("Test first() and last()")
    public void testFirstAndLast() {
        list.addAll(CharList.of('a', 'b', 'c'));
        OptionalChar first = list.first();
        assertTrue(first.isPresent());
        assertEquals('a', first.get());

        OptionalChar last = list.last();
        assertTrue(last.isPresent());
        assertEquals('c', last.get());
    }

    @Test
    @DisplayName("Test getFirst() and getLast()")
    public void testGetFirstAndLast() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertEquals('a', list.getFirst());
        assertEquals('c', list.getLast());
    }

    @Test
    @DisplayName("Test getFirst() on empty list")
    public void testGetFirstOnEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    // Utility Methods Tests
    @Test
    @DisplayName("Test trimToSize()")
    public void testTrimToSize() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList trimmed = list.trimToSize();
        assertSame(list, trimmed);
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test clear()")
    public void testClear() {
        list.addAll(CharList.of('a', 'b', 'c'));
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test isEmpty()")
    public void testIsEmpty() {
        assertTrue(list.isEmpty());
        list.add('a');
        assertFalse(list.isEmpty());
    }

    @Test
    @DisplayName("Test size()")
    public void testSize() {
        assertEquals(0, list.size());
        list.add('a');
        assertEquals(1, list.size());
        list.add('b');
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Test moveRange()")
    public void testMoveRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.moveRange(1, 3, 3);
        assertEquals('a', list.get(0));
        assertEquals('d', list.get(1));
        assertEquals('e', list.get(2));
        assertEquals('b', list.get(3));
        assertEquals('c', list.get(4));
    }

    @Test
    @DisplayName("Test forEach()")
    public void testForEach() {
        list.addAll(CharList.of('a', 'b', 'c'));
        List<Character> collected = new ArrayList<>();
        list.forEach(c -> collected.add(c));
        assertEquals(3, collected.size());
        assertEquals('a', collected.get(0).charValue());
        assertEquals('b', collected.get(1).charValue());
        assertEquals('c', collected.get(2).charValue());
    }

    // Equals and HashCode Tests
    @Test
    @DisplayName("Test equals()")
    public void testEquals() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList other = CharList.of('a', 'b', 'c');
        assertTrue(list.equals(other));

        CharList different = CharList.of('a', 'b', 'd');
        assertFalse(list.equals(different));

        assertFalse(list.equals(null));
        assertFalse(list.equals("not a CharList"));
        assertTrue(list.equals(list));
    }

    @Test
    @DisplayName("Test hashCode()")
    public void testHashCode() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList other = CharList.of('a', 'b', 'c');
        assertEquals(list.hashCode(), other.hashCode());

        CharList different = CharList.of('a', 'b', 'd');
        assertNotEquals(list.hashCode(), different.hashCode());
    }

    @Test
    @DisplayName("Test toString()")
    public void testToString() {
        assertTrue(list.toString().equals("[]"));

        list.addAll(CharList.of('a', 'b', 'c'));
        String str = list.toString();
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.contains("c"));
    }
}
