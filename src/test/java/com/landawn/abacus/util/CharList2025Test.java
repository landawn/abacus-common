package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.CharStream;

@Tag("2025")
public class CharList2025Test extends TestBase {

    @Test
    @DisplayName("Test default constructor")
    public void testDefaultConstructor() {
        CharList list = new CharList();
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        CharList list = new CharList(10);
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test constructor with negative capacity throws exception")
    public void testConstructorWithNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CharList(-1));
    }

    @Test
    @DisplayName("Test constructor with array")
    public void testConstructorWithArray() {
        char[] arr = { 'a', 'b', 'c' };
        CharList list = new CharList(arr);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test constructor with array and size")
    public void testConstructorWithArrayAndSize() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        CharList list = new CharList(arr, 3);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test constructor with invalid size throws exception")
    public void testConstructorWithInvalidSize() {
        char[] arr = { 'a', 'b', 'c' };
        assertThrows(IndexOutOfBoundsException.class, () -> new CharList(arr, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> new CharList(arr, -1));
    }

    @Test
    @DisplayName("Test of() method")
    public void testOf() {
        CharList list = CharList.of('a', 'b', 'c');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test of() with empty array")
    public void testOfEmpty() {
        CharList list = CharList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test of() with array and size")
    public void testOfWithSize() {
        char[] arr = { 'a', 'b', 'c', 'd' };
        CharList list = CharList.of(arr, 2);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    @DisplayName("Test copyOf() method")
    public void testCopyOf() {
        char[] arr = { 'a', 'b', 'c' };
        CharList list = CharList.copyOf(arr);
        assertEquals(3, list.size());
        arr[0] = 'x';
        assertEquals('a', list.get(0));
    }

    @Test
    @DisplayName("Test copyOf() with range")
    public void testCopyOfRange() {
        char[] arr = { 'a', 'b', 'c', 'd', 'e' };
        CharList list = CharList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals('b', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('d', list.get(2));
    }

    @Test
    @DisplayName("Test range() method")
    public void testRange() {
        CharList list = CharList.range('a', 'd');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test range() with step")
    public void testRangeWithStep() {
        CharList list = CharList.range('a', 'g', 2);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('e', list.get(2));
    }

    @Test
    @DisplayName("Test rangeClosed() method")
    public void testRangeClosed() {
        CharList list = CharList.rangeClosed('a', 'd');
        assertEquals(4, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
        assertEquals('d', list.get(3));
    }

    @Test
    @DisplayName("Test rangeClosed() with step")
    public void testRangeClosedWithStep() {
        CharList list = CharList.rangeClosed('a', 'g', 2);
        assertEquals(4, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('e', list.get(2));
        assertEquals('g', list.get(3));
    }

    @Test
    @DisplayName("Test repeat() method")
    public void testRepeat() {
        CharList list = CharList.repeat('a', 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals('a', list.get(i));
        }
    }

    @Test
    @DisplayName("Test random() method")
    public void testRandom() {
        CharList list = CharList.random(10);
        assertEquals(10, list.size());
        assertNotNull(list);
    }

    @Test
    @DisplayName("Test random() with range")
    public void testRandomWithRange() {
        CharList list = CharList.random('a', 'z', 10);
        assertEquals(10, list.size());
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 'a' && list.get(i) < 'z');
        }
    }

    @Test
    @DisplayName("Test random() with invalid range throws exception")
    public void testRandomWithInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> CharList.random('z', 'a', 10));
    }

    @Test
    @DisplayName("Test random() with candidates")
    public void testRandomWithCandidates() {
        char[] candidates = { 'a', 'b', 'c' };
        CharList list = CharList.random(candidates, 10);
        assertEquals(10, list.size());
        for (int i = 0; i < list.size(); i++) {
            char c = list.get(i);
            assertTrue(c == 'a' || c == 'b' || c == 'c');
        }
    }

    @Test
    @DisplayName("Test random() with single candidate")
    public void testRandomWithSingleCandidate() {
        char[] candidates = { 'a' };
        CharList list = CharList.random(candidates, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals('a', list.get(i));
        }
    }

    @Test
    @DisplayName("Test random() with empty candidates throws exception")
    public void testRandomWithEmptyCandidates() {
        assertThrows(IllegalArgumentException.class, () -> CharList.random(new char[0], 10));
    }

    @Test
    @DisplayName("Test get() method")
    public void testGet() {
        CharList list = CharList.of('a', 'b', 'c');
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test get() with invalid index throws exception")
    public void testGetInvalidIndex() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    @DisplayName("Test set() method")
    public void testSet() {
        CharList list = CharList.of('a', 'b', 'c');
        char old = list.set(1, 'x');
        assertEquals('b', old);
        assertEquals('x', list.get(1));
    }

    @Test
    @DisplayName("Test set() with invalid index throws exception")
    public void testSetInvalidIndex() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 'x'));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 'x'));
    }

    @Test
    @DisplayName("Test add() method")
    public void testAdd() {
        CharList list = new CharList();
        list.add('a');
        list.add('b');
        list.add('c');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test add() at index")
    public void testAddAtIndex() {
        CharList list = CharList.of('a', 'c');
        list.add(1, 'b');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test add() at invalid index throws exception")
    public void testAddAtInvalidIndex() {
        CharList list = CharList.of('a', 'b');
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 'x'));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(5, 'x'));
    }

    @Test
    @DisplayName("Test addAll() with CharList")
    public void testAddAllCharList() {
        CharList list1 = CharList.of('a', 'b');
        CharList list2 = CharList.of('c', 'd');
        assertTrue(list1.addAll(list2));
        assertEquals(4, list1.size());
        assertEquals('c', list1.get(2));
        assertEquals('d', list1.get(3));
    }

    @Test
    @DisplayName("Test addAll() with empty CharList")
    public void testAddAllEmptyCharList() {
        CharList list1 = CharList.of('a', 'b');
        CharList list2 = new CharList();
        assertFalse(list1.addAll(list2));
        assertEquals(2, list1.size());
    }

    @Test
    @DisplayName("Test addAll() at index with CharList")
    public void testAddAllAtIndexCharList() {
        CharList list1 = CharList.of('a', 'd');
        CharList list2 = CharList.of('b', 'c');
        assertTrue(list1.addAll(1, list2));
        assertEquals(4, list1.size());
        assertEquals('a', list1.get(0));
        assertEquals('b', list1.get(1));
        assertEquals('c', list1.get(2));
        assertEquals('d', list1.get(3));
    }

    @Test
    @DisplayName("Test addAll() with array")
    public void testAddAllArray() {
        CharList list = CharList.of('a', 'b');
        char[] arr = { 'c', 'd' };
        assertTrue(list.addAll(arr));
        assertEquals(4, list.size());
        assertEquals('c', list.get(2));
        assertEquals('d', list.get(3));
    }

    @Test
    @DisplayName("Test addAll() with empty array")
    public void testAddAllEmptyArray() {
        CharList list = CharList.of('a', 'b');
        assertFalse(list.addAll(new char[0]));
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Test addAll() at index with array")
    public void testAddAllAtIndexArray() {
        CharList list = CharList.of('a', 'd');
        char[] arr = { 'b', 'c' };
        assertTrue(list.addAll(1, arr));
        assertEquals(4, list.size());
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test remove() by value")
    public void testRemoveByValue() {
        CharList list = CharList.of('a', 'b', 'c', 'b');
        assertTrue(list.remove('b'));
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('b', list.get(2));
    }

    @Test
    @DisplayName("Test remove() non-existing value")
    public void testRemoveNonExisting() {
        CharList list = CharList.of('a', 'b', 'c');
        assertFalse(list.remove('x'));
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test removeAllOccurrences()")
    public void testRemoveAllOccurrences() {
        CharList list = CharList.of('a', 'b', 'c', 'b', 'd', 'b');
        assertTrue(list.removeAllOccurrences('b'));
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('d', list.get(2));
    }

    @Test
    @DisplayName("Test removeAllOccurrences() non-existing value")
    public void testRemoveAllOccurrencesNonExisting() {
        CharList list = CharList.of('a', 'b', 'c');
        assertFalse(list.removeAllOccurrences('x'));
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test removeAll() with CharList")
    public void testRemoveAllCharList() {
        CharList list1 = CharList.of('a', 'b', 'c', 'd');
        CharList list2 = CharList.of('b', 'd');
        assertTrue(list1.removeAll(list2));
        assertEquals(2, list1.size());
        assertEquals('a', list1.get(0));
        assertEquals('c', list1.get(1));
    }

    @Test
    @DisplayName("Test removeAll() with empty CharList")
    public void testRemoveAllEmptyCharList() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = new CharList();
        assertFalse(list1.removeAll(list2));
        assertEquals(3, list1.size());
    }

    @Test
    @DisplayName("Test removeAll() with array")
    public void testRemoveAllArray() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        char[] arr = { 'b', 'd' };
        assertTrue(list.removeAll(arr));
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
    }

    @Test
    @DisplayName("Test removeIf()")
    public void testRemoveIf() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        assertTrue(list.removeIf(c -> c == 'b' || c == 'd'));
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('e', list.get(2));
    }

    @Test
    @DisplayName("Test removeDuplicates() on sorted list")
    public void testRemoveDuplicatesSorted() {
        CharList list = CharList.of('a', 'a', 'b', 'b', 'c');
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test removeDuplicates() on unsorted list")
    public void testRemoveDuplicatesUnsorted() {
        CharList list = CharList.of('c', 'a', 'b', 'a', 'c');
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
        assertEquals('c', list.get(0));
        assertEquals('a', list.get(1));
        assertEquals('b', list.get(2));
    }

    @Test
    @DisplayName("Test removeDuplicates() with no duplicates")
    public void testRemoveDuplicatesNoDuplicates() {
        CharList list = CharList.of('a', 'b', 'c');
        assertFalse(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test retainAll() with CharList")
    public void testRetainAllCharList() {
        CharList list1 = CharList.of('a', 'b', 'c', 'd');
        CharList list2 = CharList.of('b', 'd', 'e');
        assertTrue(list1.retainAll(list2));
        assertEquals(2, list1.size());
        assertEquals('b', list1.get(0));
        assertEquals('d', list1.get(1));
    }

    @Test
    @DisplayName("Test retainAll() with empty CharList")
    public void testRetainAllEmptyCharList() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = new CharList();
        assertTrue(list1.retainAll(list2));
        assertEquals(0, list1.size());
    }

    @Test
    @DisplayName("Test retainAll() with array")
    public void testRetainAllArray() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        char[] arr = { 'b', 'd', 'e' };
        assertTrue(list.retainAll(arr));
        assertEquals(2, list.size());
        assertEquals('b', list.get(0));
        assertEquals('d', list.get(1));
    }

    @Test
    @DisplayName("Test delete() by index")
    public void testDelete() {
        CharList list = CharList.of('a', 'b', 'c');
        char deleted = list.delete(1);
        assertEquals('b', deleted);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
    }

    @Test
    @DisplayName("Test delete() with invalid index throws exception")
    public void testDeleteInvalidIndex() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.delete(3));
    }

    @Test
    @DisplayName("Test deleteAllByIndices()")
    public void testDeleteAllByIndices() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.deleteAllByIndices(1, 3);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('e', list.get(2));
    }

    @Test
    @DisplayName("Test deleteAllByIndices() with empty indices")
    public void testDeleteAllByIndicesEmpty() {
        CharList list = CharList.of('a', 'b', 'c');
        list.deleteAllByIndices();
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test deleteRange()")
    public void testDeleteRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.deleteRange(1, 4);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('e', list.get(1));
    }

    @Test
    @DisplayName("Test deleteRange() with invalid range throws exception")
    public void testDeleteRangeInvalid() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.deleteRange(2, 1));
    }

    @Test
    @DisplayName("Test replaceAll() old value with new value")
    public void testReplaceAllValues() {
        CharList list = CharList.of('a', 'b', 'a', 'c', 'a');
        int count = list.replaceAll('a', 'x');
        assertEquals(3, count);
        assertEquals('x', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('x', list.get(2));
        assertEquals('c', list.get(3));
        assertEquals('x', list.get(4));
    }

    @Test
    @DisplayName("Test replaceAll() with no matches")
    public void testReplaceAllNoMatches() {
        CharList list = CharList.of('a', 'b', 'c');
        int count = list.replaceAll('x', 'y');
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Test replaceAll() with operator")
    public void testReplaceAllOperator() {
        CharList list = CharList.of('a', 'b', 'c');
        list.replaceAll(c -> (char) (c + 1));
        assertEquals('b', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('d', list.get(2));
    }

    @Test
    @DisplayName("Test replaceIf()")
    public void testReplaceIf() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        assertTrue(list.replaceIf(c -> c == 'b' || c == 'd', 'x'));
        assertEquals('a', list.get(0));
        assertEquals('x', list.get(1));
        assertEquals('c', list.get(2));
        assertEquals('x', list.get(3));
    }

    @Test
    @DisplayName("Test replaceIf() with no matches")
    public void testReplaceIfNoMatches() {
        CharList list = CharList.of('a', 'b', 'c');
        assertFalse(list.replaceIf(c -> c == 'x', 'y'));
    }

    @Test
    @DisplayName("Test replaceRange() with CharList")
    public void testReplaceRangeCharList() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        CharList replacement = CharList.of('x', 'y');
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals('a', list.get(0));
        assertEquals('x', list.get(1));
        assertEquals('y', list.get(2));
        assertEquals('e', list.get(3));
    }

    @Test
    @DisplayName("Test replaceRange() with empty CharList")
    public void testReplaceRangeEmptyCharList() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        CharList replacement = new CharList();
        list.replaceRange(1, 3, replacement);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('d', list.get(1));
    }

    @Test
    @DisplayName("Test replaceRange() with array")
    public void testReplaceRangeArray() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        char[] replacement = { 'x', 'y' };
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertEquals('a', list.get(0));
        assertEquals('x', list.get(1));
        assertEquals('y', list.get(2));
        assertEquals('e', list.get(3));
    }

    @Test
    @DisplayName("Test fill() entire list")
    public void testFill() {
        CharList list = CharList.of('a', 'b', 'c');
        list.fill('x');
        assertEquals(3, list.size());
        assertEquals('x', list.get(0));
        assertEquals('x', list.get(1));
        assertEquals('x', list.get(2));
    }

    @Test
    @DisplayName("Test fill() with range")
    public void testFillRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.fill(1, 4, 'x');
        assertEquals('a', list.get(0));
        assertEquals('x', list.get(1));
        assertEquals('x', list.get(2));
        assertEquals('x', list.get(3));
        assertEquals('e', list.get(4));
    }

    @Test
    @DisplayName("Test fill() with invalid range throws exception")
    public void testFillInvalidRange() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(-1, 2, 'x'));
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(0, 5, 'x'));
    }

    @Test
    @DisplayName("Test contains()")
    public void testContains() {
        CharList list = CharList.of('a', 'b', 'c');
        assertTrue(list.contains('b'));
        assertFalse(list.contains('x'));
    }

    @Test
    @DisplayName("Test containsAny() with CharList")
    public void testContainsAnyCharList() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = CharList.of('c', 'd', 'e');
        assertTrue(list1.containsAny(list2));

        CharList list3 = CharList.of('x', 'y', 'z');
        assertFalse(list1.containsAny(list3));
    }

    @Test
    @DisplayName("Test containsAny() with empty list")
    public void testContainsAnyEmpty() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = new CharList();
        assertFalse(list1.containsAny(list2));
    }

    @Test
    @DisplayName("Test containsAny() with array")
    public void testContainsAnyArray() {
        CharList list = CharList.of('a', 'b', 'c');
        assertTrue(list.containsAny(new char[] { 'c', 'd' }));
        assertFalse(list.containsAny(new char[] { 'x', 'y' }));
    }

    @Test
    @DisplayName("Test containsAll() with CharList")
    public void testContainsAllCharList() {
        CharList list1 = CharList.of('a', 'b', 'c', 'd');
        CharList list2 = CharList.of('b', 'd');
        assertTrue(list1.containsAll(list2));

        CharList list3 = CharList.of('b', 'x');
        assertFalse(list1.containsAll(list3));
    }

    @Test
    @DisplayName("Test containsAll() with empty list")
    public void testContainsAllEmpty() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = new CharList();
        assertTrue(list1.containsAll(list2));
    }

    @Test
    @DisplayName("Test containsAll() with array")
    public void testContainsAllArray() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        assertTrue(list.containsAll(new char[] { 'b', 'd' }));
        assertFalse(list.containsAll(new char[] { 'b', 'x' }));
    }

    @Test
    @DisplayName("Test disjoint() with CharList")
    public void testDisjointCharList() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = CharList.of('x', 'y', 'z');
        assertTrue(list1.disjoint(list2));

        CharList list3 = CharList.of('c', 'd', 'e');
        assertFalse(list1.disjoint(list3));
    }

    @Test
    @DisplayName("Test disjoint() with array")
    public void testDisjointArray() {
        CharList list = CharList.of('a', 'b', 'c');
        assertTrue(list.disjoint(new char[] { 'x', 'y', 'z' }));
        assertFalse(list.disjoint(new char[] { 'c', 'd', 'e' }));
    }

    @Test
    @DisplayName("Test intersection() with CharList")
    public void testIntersectionCharList() {
        CharList list1 = CharList.of('a', 'b', 'c', 'd');
        CharList list2 = CharList.of('c', 'd', 'e', 'f');
        CharList result = list1.intersection(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains('c'));
        assertTrue(result.contains('d'));
    }

    @Test
    @DisplayName("Test intersection() with empty list")
    public void testIntersectionEmpty() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = new CharList();
        CharList result = list1.intersection(list2);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Test intersection() with array")
    public void testIntersectionArray() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        char[] arr = { 'c', 'd', 'e', 'f' };
        CharList result = list.intersection(arr);
        assertEquals(2, result.size());
        assertTrue(result.contains('c'));
        assertTrue(result.contains('d'));
    }

    @Test
    @DisplayName("Test difference() with CharList")
    public void testDifferenceCharList() {
        CharList list1 = CharList.of('a', 'b', 'c', 'd');
        CharList list2 = CharList.of('c', 'd', 'e');
        CharList result = list1.difference(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
    }

    @Test
    @DisplayName("Test difference() with array")
    public void testDifferenceArray() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        char[] arr = { 'c', 'd', 'e' };
        CharList result = list.difference(arr);
        assertEquals(2, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
    }

    @Test
    @DisplayName("Test symmetricDifference() with CharList")
    public void testSymmetricDifferenceCharList() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = CharList.of('c', 'd', 'e');
        CharList result = list1.symmetricDifference(list2);
        assertEquals(4, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('d'));
        assertTrue(result.contains('e'));
    }

    @Test
    @DisplayName("Test symmetricDifference() with array")
    public void testSymmetricDifferenceArray() {
        CharList list = CharList.of('a', 'b', 'c');
        char[] arr = { 'c', 'd', 'e' };
        CharList result = list.symmetricDifference(arr);
        assertEquals(4, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('d'));
        assertTrue(result.contains('e'));
    }

    @Test
    @DisplayName("Test occurrencesOf()")
    public void testOccurrencesOf() {
        CharList list = CharList.of('a', 'b', 'a', 'c', 'a');
        assertEquals(3, list.occurrencesOf('a'));
        assertEquals(1, list.occurrencesOf('b'));
        assertEquals(0, list.occurrencesOf('x'));
    }

    @Test
    @DisplayName("Test indexOf()")
    public void testIndexOf() {
        CharList list = CharList.of('a', 'b', 'c', 'b');
        assertEquals(1, list.indexOf('b'));
        assertEquals(-1, list.indexOf('x'));
    }

    @Test
    @DisplayName("Test indexOf() with fromIndex")
    public void testIndexOfFromIndex() {
        CharList list = CharList.of('a', 'b', 'c', 'b');
        assertEquals(3, list.indexOf('b', 2));
        assertEquals(-1, list.indexOf('b', 4));
    }

    @Test
    @DisplayName("Test lastIndexOf()")
    public void testLastIndexOf() {
        CharList list = CharList.of('a', 'b', 'c', 'b');
        assertEquals(3, list.lastIndexOf('b'));
        assertEquals(-1, list.lastIndexOf('x'));
    }

    @Test
    @DisplayName("Test lastIndexOf() with startIndexFromBack")
    public void testLastIndexOfFromBack() {
        CharList list = CharList.of('a', 'b', 'c', 'b', 'd');
        assertEquals(1, list.lastIndexOf('b', 2));
        assertEquals(-1, list.lastIndexOf('b', 0));
    }

    @Test
    @DisplayName("Test min()")
    public void testMin() {
        CharList list = CharList.of('c', 'a', 'd', 'b');
        OptionalChar min = list.min();
        assertTrue(min.isPresent());
        assertEquals('a', min.get());
    }

    @Test
    @DisplayName("Test min() on empty list")
    public void testMinEmpty() {
        CharList list = new CharList();
        OptionalChar min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    @DisplayName("Test min() with range")
    public void testMinRange() {
        CharList list = CharList.of('a', 'e', 'b', 'd', 'c');
        OptionalChar min = list.min(1, 4);
        assertTrue(min.isPresent());
        assertEquals('b', min.get());
    }

    @Test
    @DisplayName("Test max()")
    public void testMax() {
        CharList list = CharList.of('c', 'a', 'd', 'b');
        OptionalChar max = list.max();
        assertTrue(max.isPresent());
        assertEquals('d', max.get());
    }

    @Test
    @DisplayName("Test max() on empty list")
    public void testMaxEmpty() {
        CharList list = new CharList();
        OptionalChar max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    @DisplayName("Test max() with range")
    public void testMaxRange() {
        CharList list = CharList.of('a', 'e', 'b', 'd', 'c');
        OptionalChar max = list.max(1, 4);
        assertTrue(max.isPresent());
        assertEquals('e', max.get());
    }

    @Test
    @DisplayName("Test median()")
    public void testMedian() {
        CharList list = CharList.of('a', 'c', 'b');
        OptionalChar median = list.median();
        assertTrue(median.isPresent());
        assertEquals('b', median.get());
    }

    @Test
    @DisplayName("Test median() on empty list")
    public void testMedianEmpty() {
        CharList list = new CharList();
        OptionalChar median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    @DisplayName("Test median() with range")
    public void testMedianRange() {
        CharList list = CharList.of('a', 'e', 'c', 'b', 'd');
        OptionalChar median = list.median(1, 4);
        assertTrue(median.isPresent());
        assertEquals('c', median.get());
    }

    @Test
    @DisplayName("Test first()")
    public void testFirst() {
        CharList list = CharList.of('a', 'b', 'c');
        OptionalChar first = list.first();
        assertTrue(first.isPresent());
        assertEquals('a', first.get());
    }

    @Test
    @DisplayName("Test first() on empty list")
    public void testFirstEmpty() {
        CharList list = new CharList();
        OptionalChar first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    @DisplayName("Test last()")
    public void testLast() {
        CharList list = CharList.of('a', 'b', 'c');
        OptionalChar last = list.last();
        assertTrue(last.isPresent());
        assertEquals('c', last.get());
    }

    @Test
    @DisplayName("Test last() on empty list")
    public void testLastEmpty() {
        CharList list = new CharList();
        OptionalChar last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    @DisplayName("Test distinct()")
    public void testDistinct() {
        CharList list = CharList.of('a', 'b', 'a', 'c', 'b');
        CharList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertTrue(distinct.contains('a'));
        assertTrue(distinct.contains('b'));
        assertTrue(distinct.contains('c'));
    }

    @Test
    @DisplayName("Test hasDuplicates()")
    public void testHasDuplicates() {
        CharList list1 = CharList.of('a', 'b', 'a');
        assertTrue(list1.hasDuplicates());

        CharList list2 = CharList.of('a', 'b', 'c');
        assertFalse(list2.hasDuplicates());
    }

    @Test
    @DisplayName("Test isSorted()")
    public void testIsSorted() {
        CharList sorted = CharList.of('a', 'b', 'c');
        assertTrue(sorted.isSorted());

        CharList unsorted = CharList.of('c', 'a', 'b');
        assertFalse(unsorted.isSorted());
    }

    @Test
    @DisplayName("Test sort()")
    public void testSort() {
        CharList list = CharList.of('c', 'a', 'b');
        list.sort();
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test parallelSort()")
    public void testParallelSort() {
        CharList list = CharList.of('c', 'a', 'b');
        list.parallelSort();
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test reverseSort()")
    public void testReverseSort() {
        CharList list = CharList.of('a', 'c', 'b');
        list.reverseSort();
        assertEquals('c', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('a', list.get(2));
    }

    @Test
    @DisplayName("Test binarySearch()")
    public void testBinarySearch() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        assertEquals(2, list.binarySearch('c'));
        assertTrue(list.binarySearch('x') < 0);
    }

    @Test
    @DisplayName("Test binarySearch() with range")
    public void testBinarySearchRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        assertEquals(2, list.binarySearch(0, 5, 'c'));
        assertTrue(list.binarySearch(0, 2, 'c') < 0);
    }

    @Test
    @DisplayName("Test reverse()")
    public void testReverse() {
        CharList list = CharList.of('a', 'b', 'c');
        list.reverse();
        assertEquals('c', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('a', list.get(2));
    }

    @Test
    @DisplayName("Test reverse() with range")
    public void testReverseRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.reverse(1, 4);
        assertEquals('a', list.get(0));
        assertEquals('d', list.get(1));
        assertEquals('c', list.get(2));
        assertEquals('b', list.get(3));
        assertEquals('e', list.get(4));
    }

    @Test
    @DisplayName("Test rotate()")
    public void testRotate() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        list.rotate(1);
        assertEquals('d', list.get(0));
        assertEquals('a', list.get(1));
        assertEquals('b', list.get(2));
        assertEquals('c', list.get(3));
    }

    @Test
    @DisplayName("Test rotate() with negative distance")
    public void testRotateNegative() {
        CharList list = CharList.of('a', 'b', 'c', 'd');
        list.rotate(-1);
        assertEquals('b', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('d', list.get(2));
        assertEquals('a', list.get(3));
    }

    @Test
    @DisplayName("Test shuffle()")
    public void testShuffle() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        CharList copy = list.copy();
        list.shuffle();
        assertEquals(5, list.size());
        assertTrue(list.contains('a'));
        assertTrue(list.contains('b'));
        assertTrue(list.contains('c'));
        assertTrue(list.contains('d'));
        assertTrue(list.contains('e'));
    }

    @Test
    @DisplayName("Test shuffle() with Random")
    public void testShuffleWithRandom() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.shuffle(new Random(42));
        assertEquals(5, list.size());
    }

    @Test
    @DisplayName("Test swap()")
    public void testSwap() {
        CharList list = CharList.of('a', 'b', 'c');
        list.swap(0, 2);
        assertEquals('c', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('a', list.get(2));
    }

    @Test
    @DisplayName("Test copy()")
    public void testCopy() {
        CharList list = CharList.of('a', 'b', 'c');
        CharList copy = list.copy();
        assertEquals(3, copy.size());
        assertEquals('a', copy.get(0));
        assertEquals('b', copy.get(1));
        assertEquals('c', copy.get(2));
        assertNotSame(list, copy);
    }

    @Test
    @DisplayName("Test copy() with range")
    public void testCopyRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        CharList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertEquals('b', copy.get(0));
        assertEquals('c', copy.get(1));
        assertEquals('d', copy.get(2));
    }

    @Test
    @DisplayName("Test copy() with step")
    public void testCopyWithStep() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
        CharList copy = list.copy(0, 6, 2);
        assertEquals(3, copy.size());
        assertEquals('a', copy.get(0));
        assertEquals('c', copy.get(1));
        assertEquals('e', copy.get(2));
    }

    @Test
    @DisplayName("Test split()")
    public void testSplit() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
        List<CharList> chunks = list.split(0, 6, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(2, chunks.get(2).size());
    }

    @Test
    @DisplayName("Test trimToSize()")
    public void testTrimToSize() {
        CharList list = new CharList(100);
        list.add('a');
        list.add('b');
        CharList trimmed = list.trimToSize();
        assertEquals(2, trimmed.size());
        assertNotNull(trimmed);
    }

    @Test
    @DisplayName("Test clear()")
    public void testClear() {
        CharList list = CharList.of('a', 'b', 'c');
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test isEmpty()")
    public void testIsEmpty() {
        CharList list = new CharList();
        assertTrue(list.isEmpty());
        list.add('a');
        assertFalse(list.isEmpty());
    }

    @Test
    @DisplayName("Test size()")
    public void testSize() {
        CharList list = new CharList();
        assertEquals(0, list.size());
        list.add('a');
        assertEquals(1, list.size());
        list.add('b');
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Test toArray()")
    public void testToArray() {
        CharList list = CharList.of('a', 'b', 'c');
        char[] arr = list.toArray();
        assertEquals(3, arr.length);
        assertEquals('a', arr[0]);
        assertEquals('b', arr[1]);
        assertEquals('c', arr[2]);
    }

    @Test
    @DisplayName("Test boxed()")
    public void testBoxed() {
        CharList list = CharList.of('a', 'b', 'c');
        List<Character> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Character.valueOf('a'), boxed.get(0));
        assertEquals(Character.valueOf('b'), boxed.get(1));
        assertEquals(Character.valueOf('c'), boxed.get(2));
    }

    @Test
    @DisplayName("Test boxed() with range")
    public void testBoxedRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        List<Character> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Character.valueOf('b'), boxed.get(0));
        assertEquals(Character.valueOf('c'), boxed.get(1));
        assertEquals(Character.valueOf('d'), boxed.get(2));
    }

    @Test
    @DisplayName("Test toIntList()")
    public void testToIntList() {
        CharList list = CharList.of('a', 'b', 'c');
        IntList intList = list.toIntList();
        assertEquals(3, intList.size());
        assertEquals((int) 'a', intList.get(0));
        assertEquals((int) 'b', intList.get(1));
        assertEquals((int) 'c', intList.get(2));
    }

    @Test
    @DisplayName("Test toCollection()")
    public void testToCollection() {
        CharList list = CharList.of('a', 'b', 'c');
        ArrayList<Character> collection = list.toCollection(0, 3, ArrayList::new);
        assertEquals(3, collection.size());
        assertEquals(Character.valueOf('a'), collection.get(0));
        assertEquals(Character.valueOf('b'), collection.get(1));
        assertEquals(Character.valueOf('c'), collection.get(2));
    }

    @Test
    @DisplayName("Test forEach()")
    public void testForEach() {
        CharList list = CharList.of('a', 'b', 'c');
        final StringBuilder sb = new StringBuilder();
        list.forEach(c -> sb.append(c));
        assertEquals("abc", sb.toString());
    }

    @Test
    @DisplayName("Test forEach() with range")
    public void testForEachRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        final StringBuilder sb = new StringBuilder();
        list.forEach(1, 4, c -> sb.append(c));
        assertEquals("bcd", sb.toString());
    }

    @Test
    @DisplayName("Test moveRange()")
    public void testMoveRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.moveRange(1, 3, 0);
        assertEquals('b', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('a', list.get(2));
        assertEquals('d', list.get(3));
        assertEquals('e', list.get(4));
    }

    @Test
    @DisplayName("Test add() with capacity expansion")
    public void testAddWithCapacityExpansion() {
        CharList list = new CharList(2);
        list.add('a');
        list.add('b');
        list.add('c');
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test array() method")
    public void testArrayMethod() {
        CharList list = CharList.of('a', 'b', 'c');
        char[] arr = list.array();
        assertNotNull(arr);
        assertTrue(arr.length >= 3);
    }

    @Test
    @DisplayName("Test empty list operations")
    public void testEmptyListOperations() {
        CharList list = new CharList();
        assertFalse(list.remove('a'));
        assertEquals(0, list.occurrencesOf('a'));
        assertEquals(-1, list.indexOf('a'));
        assertEquals(-1, list.lastIndexOf('a'));
    }

    @Test
    @DisplayName("Test single element list operations")
    public void testSingleElementOperations() {
        CharList list = CharList.of('a');
        assertTrue(list.contains('a'));
        assertEquals(0, list.indexOf('a'));
        assertEquals(0, list.lastIndexOf('a'));
        assertEquals(1, list.occurrencesOf('a'));
    }

    @Test
    @DisplayName("Test large list operations")
    public void testLargeListOperations() {
        CharList list = new CharList(1000);
        for (int i = 0; i < 1000; i++) {
            list.add((char) ('a' + (i % 26)));
        }
        assertEquals(1000, list.size());
        assertTrue(list.contains('a'));
    }

    @Test
    @DisplayName("Test null handling in methods")
    public void testNullHandling() {
        CharList list = CharList.of('a', 'b', 'c');
        assertFalse(list.addAll((CharList) null));
        assertFalse(list.addAll((char[]) null));
        assertFalse(list.removeAll((CharList) null));
        assertFalse(list.removeAll((char[]) null));
    }

    @Test
    @DisplayName("Test stream conversion")
    public void testStreamConversion() {
        CharList list = CharList.of('a', 'b', 'c');
        CharStream stream = list.stream();
        assertNotNull(stream);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("Test equals and hashCode")
    public void testEqualsHashCode() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = CharList.of('a', 'b', 'c');
        CharList list3 = CharList.of('a', 'b', 'd');

        assertEquals(list1, list2);
        assertFalse(list1.equals(list3));
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    @DisplayName("Test toString()")
    public void testToString() {
        CharList list = CharList.of('a', 'b', 'c');
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.contains("c"));
    }

    @Test
    @DisplayName("Test getFirst()")
    public void testGetFirst() {
        CharList list = CharList.of('a', 'b', 'c');
        assertEquals('a', list.getFirst());
    }

    @Test
    @DisplayName("Test getFirst() on empty list throws exception")
    public void testGetFirstEmpty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    @DisplayName("Test getLast()")
    public void testGetLast() {
        CharList list = CharList.of('a', 'b', 'c');
        assertEquals('c', list.getLast());
    }

    @Test
    @DisplayName("Test getLast() on empty list throws exception")
    public void testGetLastEmpty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    @DisplayName("Test addFirst()")
    public void testAddFirst() {
        CharList list = CharList.of('b', 'c');
        list.addFirst('a');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test addFirst() on empty list")
    public void testAddFirstEmpty() {
        CharList list = new CharList();
        list.addFirst('a');
        assertEquals(1, list.size());
        assertEquals('a', list.get(0));
    }

    @Test
    @DisplayName("Test addLast()")
    public void testAddLast() {
        CharList list = CharList.of('a', 'b');
        list.addLast('c');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    @DisplayName("Test addLast() on empty list")
    public void testAddLastEmpty() {
        CharList list = new CharList();
        list.addLast('a');
        assertEquals(1, list.size());
        assertEquals('a', list.get(0));
    }

    @Test
    @DisplayName("Test removeFirst()")
    public void testRemoveFirst() {
        CharList list = CharList.of('a', 'b', 'c');
        char removed = list.removeFirst();
        assertEquals('a', removed);
        assertEquals(2, list.size());
        assertEquals('b', list.get(0));
        assertEquals('c', list.get(1));
    }

    @Test
    @DisplayName("Test removeFirst() on empty list throws exception")
    public void testRemoveFirstEmpty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    @DisplayName("Test removeLast()")
    public void testRemoveLast() {
        CharList list = CharList.of('a', 'b', 'c');
        char removed = list.removeLast();
        assertEquals('c', removed);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    @DisplayName("Test removeLast() on empty list throws exception")
    public void testRemoveLastEmpty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    @DisplayName("Test iterator()")
    public void testIterator() {
        CharList list = CharList.of('a', 'b', 'c');
        com.landawn.abacus.util.CharIterator iter = list.iterator();
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
        assertEquals('b', iter.nextChar());
        assertEquals('c', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test iterator() on empty list")
    public void testIteratorEmpty() {
        CharList list = new CharList();
        com.landawn.abacus.util.CharIterator iter = list.iterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test iterator() throws on next without hasNext")
    public void testIteratorNoSuchElement() {
        CharList list = CharList.of('a');
        com.landawn.abacus.util.CharIterator iter = list.iterator();
        iter.nextChar();
        assertThrows(NoSuchElementException.class, () -> iter.nextChar());
    }

    @Test
    @DisplayName("Test toMultiset()")
    public void testToMultiset() {
        CharList list = CharList.of('a', 'b', 'a', 'c', 'b', 'a');
        com.landawn.abacus.util.Multiset<Character> multiset = list.toMultiset(0, list.size(), com.landawn.abacus.util.Multiset::new);
        assertNotNull(multiset);
        assertEquals(3, multiset.get('a'));
        assertEquals(2, multiset.get('b'));
        assertEquals(1, multiset.get('c'));
    }

    @Test
    @DisplayName("Test toMultiset() with range")
    public void testToMultisetRange() {
        CharList list = CharList.of('a', 'b', 'a', 'c', 'b', 'a');
        com.landawn.abacus.util.Multiset<Character> multiset = list.toMultiset(0, 4, com.landawn.abacus.util.Multiset::new);
        assertNotNull(multiset);
        assertEquals(2, multiset.get('a'));
        assertEquals(1, multiset.get('b'));
        assertEquals(1, multiset.get('c'));
    }
}
