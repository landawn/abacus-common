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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.Collectors;

@Tag("2025")
public class CharListTest extends TestBase {

    private CharList list;

    @BeforeEach
    public void setUp() {
        list = new CharList();
    }

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
        assertThrows(IllegalArgumentException.class, () -> new CharList(arr, -1));
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
        char deleted = list.removeAt(1);
        assertEquals('b', deleted);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
    }

    @Test
    @DisplayName("Test delete() with invalid index throws exception")
    public void testDeleteInvalidIndex() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(3));
    }

    @Test
    @DisplayName("Test removeAt()")
    public void testDeleteAllByIndices() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.removeAt(1, 3);
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('e', list.get(2));
    }

    @Test
    @DisplayName("Test removeAt() with empty indices")
    public void testDeleteAllByIndicesEmpty() {
        CharList list = CharList.of('a', 'b', 'c');
        list.removeAt();
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test removeRange()")
    public void testDeleteRange() {
        CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('e', list.get(1));
    }

    @Test
    @DisplayName("Test removeRange() with invalid range throws exception")
    public void testDeleteRangeInvalid() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(2, 1));
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
    @DisplayName("Test frequency()")
    public void testOccurrencesOf() {
        CharList list = CharList.of('a', 'b', 'a', 'c', 'a');
        assertEquals(3, list.frequency('a'));
        assertEquals(1, list.frequency('b'));
        assertEquals(0, list.frequency('x'));
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
    @DisplayName("Test containsDuplicates()")
    public void testHasDuplicates() {
        CharList list1 = CharList.of('a', 'b', 'a');
        assertTrue(list1.containsDuplicates());

        CharList list2 = CharList.of('a', 'b', 'c');
        assertFalse(list2.containsDuplicates());
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
        assertEquals('a', intList.get(0));
        assertEquals('b', intList.get(1));
        assertEquals('c', intList.get(2));
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
        char[] arr = list.internalArray();
        assertNotNull(arr);
        assertTrue(arr.length >= 3);
    }

    @Test
    @DisplayName("Test empty list operations")
    public void testEmptyListOperations() {
        CharList list = new CharList();
        assertFalse(list.remove('a'));
        assertEquals(0, list.frequency('a'));
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
        assertEquals(1, list.frequency('a'));
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

    @Test
    public void testConstructors() {
        assertEquals(0, new CharList().size());
        assertEquals(0, new CharList(10).size());
        assertTrue(new CharList(10).isEmpty());

        char[] data = { 'a', 'b', 'c' };
        CharList fromArray = new CharList(data);
        assertEquals(3, fromArray.size());
        assertArrayEquals(data, fromArray.toArray());

        CharList fromArrayWithSize = new CharList(data, 2);
        assertEquals(2, fromArrayWithSize.size());
        assertEquals('a', fromArrayWithSize.get(0));
        assertEquals('b', fromArrayWithSize.get(1));
    }

    @Test
    public void testAddAndGet() {
        list.add('a');
        list.add('b');
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));

        list.add(1, 'c');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('b', list.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    public void testAddAll() {
        list.addAll(CharList.of('a', 'b'));
        assertEquals(2, list.size());

        list.addAll(1, CharList.of('c', 'd'));
        assertArrayEquals(new char[] { 'a', 'c', 'd', 'b' }, list.toArray());
    }

    @Test
    public void testRemove() {
        list.add('a');
        list.add('b');
        list.add('a');
        assertTrue(list.remove('a'));
        assertEquals(2, list.size());
        assertArrayEquals(new char[] { 'b', 'a' }, list.toArray());
        assertFalse(list.remove('z'));
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(CharList.of('a', 'b', 'a', 'c', 'b'));
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, list.toArray());
        assertFalse(CharList.of('x', 'y', 'z').removeDuplicates());
    }

    @Test
    public void testClearAndIsEmpty() {
        list.add('a');
        assertFalse(list.isEmpty());
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testContainsAny() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertTrue(list.containsAny(CharList.of('c', 'd', 'e')));
        assertFalse(list.containsAny(CharList.of('x', 'y', 'z')));
    }

    @Test
    public void testContainsAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        assertTrue(list.containsAll(CharList.of('b', 'd')));
        assertFalse(list.containsAll(CharList.of('b', 'e')));
    }

    @Test
    public void testFirstAndLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        list.addAll(CharList.of('a', 'b', 'c'));
        assertEquals('a', list.getFirst());
        assertEquals('c', list.getLast());
    }

    @Test
    public void testAddFirstLast() {
        list.addFirst('b');
        list.addFirst('a');
        list.addLast('c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, list.toArray());
    }

    @Test
    public void testRemoveFirstLast() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        assertEquals('a', list.removeFirst());
        assertEquals('d', list.removeLast());
        assertArrayEquals(new char[] { 'b', 'c' }, list.toArray());
        assertEquals(2, list.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = CharList.of('a', 'b', 'c');
        CharList list3 = CharList.of('a', 'b', 'd');

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertEquals(list1.hashCode(), list2.hashCode());
        assertNotEquals(list1.hashCode(), list3.hashCode());
        assertNotEquals(list1, new Object());
    }

    @Test
    public void testReplaceAll() {
        list.addAll(CharList.of('a', 'b', 'a', 'c'));
        int replacements = list.replaceAll('a', 'z');
        assertEquals(2, replacements);
        assertArrayEquals(new char[] { 'z', 'b', 'z', 'c' }, list.toArray());
    }

    @Test
    public void testReplaceAllWithOperator() {
        list.addAll(CharList.of('a', 'b', 'c'));
        list.replaceAll(c -> (char) (c + 1));
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, list.toArray());
    }

    @Test
    public void testIntersection() {
        list.addAll(CharList.of('a', 'b', 'b', 'c'));
        CharList other = CharList.of('b', 'c', 'd', 'b');
        CharList intersection = list.intersection(other);
        assertArrayEquals(new char[] { 'b', 'b', 'c' }, intersection.toArray());
    }

    @Test
    public void testDifference() {
        list.addAll(CharList.of('a', 'b', 'b', 'c'));
        CharList other = CharList.of('b', 'c', 'd');
        CharList difference = list.difference(other);
        assertArrayEquals(new char[] { 'a', 'b' }, difference.toArray());
    }

    @Test
    public void testSymmetricDifference() {
        list.addAll(CharList.of('a', 'b', 'c'));
        CharList other = CharList.of('c', 'd', 'e');
        CharList symDifference = list.symmetricDifference(other);
        symDifference.sort();
        assertArrayEquals(new char[] { 'a', 'b', 'd', 'e' }, symDifference.toArray());
    }

    @Test
    public void testMinMax() {
        assertTrue(list.min().isEmpty());
        assertTrue(list.max().isEmpty());

        list.addAll(CharList.of('d', 'a', 'e', 'c', 'b'));
        assertEquals('a', list.min().get());
        assertEquals('e', list.max().get());
        assertEquals('a', list.min(1, 4).get());
    }

    @Test
    public void testStream() {
        list.addAll(CharList.of('a', 'b', 'c', 'd'));
        long count = list.stream().filter(c -> c > 'b').count();
        assertEquals(2, count);
    }

    @Test
    public void testRangeAndRangeClosed() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, CharList.range('a', 'd').toArray());
        assertArrayEquals(new char[] {}, CharList.range('a', 'a').toArray());
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.range('a', 'f', 2).toArray());

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, CharList.rangeClosed('a', 'd').toArray());
        assertArrayEquals(new char[] { 'a' }, CharList.rangeClosed('a', 'a').toArray());
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.rangeClosed('a', 'e', 2).toArray());
    }

    @Test
    public void testRemoveAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'c'));
        assertTrue(list.removeAll(CharList.of('c', 'e', 'x')));
        assertArrayEquals(new char[] { 'a', 'b', 'd' }, list.toArray());
        assertFalse(list.removeAll(CharList.of('x', 'y')));
    }

    @Test
    public void testRetainAll() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'c'));
        assertTrue(list.retainAll(CharList.of('c', 'e', 'x')));
        assertArrayEquals(new char[] { 'c', 'e', 'c' }, list.toArray());
        assertFalse(list.retainAll(CharList.of('c', 'e', 'x')));
    }

    @Test
    public void testReplaceRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        list.replaceRange(2, 4, CharList.of('x', 'y', 'z'));
        assertArrayEquals(new char[] { 'a', 'b', 'x', 'y', 'z', 'e' }, list.toArray());

        list.replaceRange(1, 4, CharList.of('m'));
        assertArrayEquals(new char[] { 'a', 'm', 'z', 'e' }, list.toArray());

        list.replaceRange(1, 2, CharList.of());
        assertArrayEquals(new char[] { 'a', 'z', 'e' }, list.toArray());
    }

    @Test
    public void testDisjoint() {
        list.addAll(CharList.of('a', 'b', 'c'));
        assertFalse(list.disjoint(CharList.of('c', 'd', 'e')));
        assertFalse(list.disjoint(new char[] { 'x', 'a' }));
        assertTrue(list.disjoint(CharList.of('x', 'y', 'z')));
        assertTrue(list.disjoint(new char[] {}));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        list.addAll(CharList.of('a', 'b', 'c', 'b', 'd'));
        assertEquals(1, list.indexOf('b'));
        assertEquals(1, list.indexOf('b', 0));
        assertEquals(3, list.indexOf('b', 2));
        assertEquals(-1, list.indexOf('b', 4));

        assertEquals(3, list.lastIndexOf('b'));
        assertEquals(1, list.lastIndexOf('b', 2));
        assertEquals(3, list.lastIndexOf('b', 4));
        assertEquals(-1, list.lastIndexOf('a', -1));
    }

    @Test
    public void testFirstLastAndOptionalFirstLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());
        assertTrue(list.first().isEmpty());
        assertTrue(list.last().isEmpty());

        list.addAll(CharList.of('a', 'b', 'c'));
        assertEquals('a', list.getFirst());
        assertEquals('c', list.getLast());
        assertEquals('a', list.first().get());
        assertEquals('c', list.last().get());
    }

    @Test
    public void testMinMaxMedian() {
        assertTrue(list.min().isEmpty());
        assertTrue(list.max().isEmpty());
        assertTrue(list.median().isEmpty());

        list.addAll(CharList.of('d', 'a', 'e', 'b', 'c'));
        assertEquals('a', list.min().get());
        assertEquals('e', list.max().get());
        assertEquals('c', list.median().get());

        assertEquals('a', list.min(1, 4).get());
        assertEquals('a', list.min(0, 2).get());
        assertEquals('e', list.max(1, 4).get());
        assertEquals('c', list.median(0, 5).get());
    }

    @Test
    public void testIntersectionAndDifference() {
        list.addAll(CharList.of('a', 'b', 'b', 'c'));
        CharList other = CharList.of('b', 'c', 'd', 'b');

        CharList intersection = list.intersection(other);
        intersection.sort();
        assertArrayEquals(new char[] { 'b', 'b', 'c' }, intersection.toArray());

        CharList difference = list.difference(other);
        assertArrayEquals(new char[] { 'a' }, difference.toArray());

        CharList differenceFromArray = list.difference(new char[] { 'b', 'x' });
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, differenceFromArray.toArray());
    }

    @Test
    public void testStreamAndSubStream() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        long count = list.stream().filter(c -> c > 'b').count();
        assertEquals(3, count);

        CharStream subStream = list.stream(1, 4);
        String result = subStream.mapToObj(c -> String.valueOf(c)).collect(Collectors.joining());
        assertEquals("bcd", result);
    }

    @Test
    public void testArray() {
        list.addAll(CharList.of('q', 'w', 'e'));
        char[] internalArray = list.internalArray();
        assertEquals(3, list.size());

        assertTrue(internalArray.length >= 3);
        assertEquals('q', internalArray[0]);
        assertEquals('w', internalArray[1]);
        assertEquals('e', internalArray[2]);
    }

    @Test
    public void testBoxedAndToList() {
        list.addAll(CharList.of('a', 'b', 'c'));
        List<Character> boxedList = list.boxed();
        assertEquals(Arrays.asList('a', 'b', 'c'), boxedList);

        List<Character> subList = list.boxed(1, 3);
        assertEquals(Arrays.asList('b', 'c'), subList);
    }

    @Test
    public void testToCollectionAndToSet() {
        list.addAll(CharList.of('a', 'b', 'a', 'c'));

        ArrayList<Character> arrayList = list.toCollection(ArrayList::new);
        assertEquals(Arrays.asList('a', 'b', 'a', 'c'), arrayList);

        java.util.Set<Character> set = list.toSet();
        assertEquals(new java.util.HashSet<>(Arrays.asList('a', 'b', 'c')), set);
    }

    @Test
    public void testShuffleWithSeededRandom() {
        CharList list1 = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
        CharList list2 = list1.copy();

        list1.shuffle(new Random(12345L));
        list2.shuffle(new Random(12345L));

        assertArrayEquals(list1.toArray(), list2.toArray());
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
    @DisplayName("Test copyOf() with range")
    public void testCopyOfWithRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharList newList = CharList.copyOf(array, 1, 4);
        assertEquals(3, newList.size());
        assertEquals('b', newList.get(0));
        assertEquals('c', newList.get(1));
        assertEquals('d', newList.get(2));
    }

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

    @Test
    @DisplayName("Test remove() non-existent element")
    public void testRemoveNonExistent() {
        list.addAll(CharList.of('a', 'b', 'c'));
        boolean removed = list.remove('d');
        assertFalse(removed);
        assertEquals(3, list.size());
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
    @DisplayName("Test forEach() with range")
    public void testForEachWithRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
        List<Character> collected = new ArrayList<>();

        list.forEach(1, 4, c -> collected.add(c));
        assertEquals(3, collected.size());
        assertEquals('b', collected.get(0).charValue());
        assertEquals('c', collected.get(1).charValue());
        assertEquals('d', collected.get(2).charValue());

        collected.clear();
        list.forEach(3, 1, c -> collected.add(c));
        assertEquals(2, collected.size());
        assertEquals('d', collected.get(0).charValue());
        assertEquals('c', collected.get(1).charValue());
    }

    @Test
    @DisplayName("Test min() with range")
    public void testMinWithRange() {
        list.addAll(CharList.of('e', 'b', 'd', 'a', 'c'));

        OptionalChar min = list.min(1, 4);
        assertTrue(min.isPresent());
        assertEquals('a', min.get());

        OptionalChar emptyMin = list.min(2, 2);
        assertFalse(emptyMin.isPresent());
    }

    @Test
    @DisplayName("Test max() with range")
    public void testMaxWithRange() {
        list.addAll(CharList.of('e', 'b', 'd', 'a', 'c'));

        OptionalChar max = list.max(1, 4);
        assertTrue(max.isPresent());
        assertEquals('d', max.get());

        OptionalChar emptyMax = list.max(2, 2);
        assertFalse(emptyMax.isPresent());
    }

    @Test
    @DisplayName("Test median() with range")
    public void testMedianWithRange() {
        list.addAll(CharList.of('e', 'b', 'd', 'a', 'c', 'f'));

        OptionalChar median = list.median(1, 5);
        assertTrue(median.isPresent());

        OptionalChar emptyMedian = list.median(3, 3);
        assertFalse(emptyMedian.isPresent());
    }

    @Test
    @DisplayName("Test binarySearch() with range")
    public void testBinarySearchWithRange() {
        list.addAll(CharList.of('a', 'f', 'b', 'd', 'g', 'h'));
        CharList subList = list.copy(1, 5);
        subList.sort();
        list.replaceRange(1, 5, subList);

        int index = list.binarySearch(1, 5, 'd');
        assertEquals(2, index);

        int notFound = list.binarySearch(1, 5, 'e');
        assertTrue(notFound < 0);
    }

    @Test
    @DisplayName("Test distinct() with range")
    public void testDistinctWithRange() {
        list.addAll(CharList.of('a', 'b', 'b', 'c', 'c', 'd'));

        CharList distinct = list.distinct(1, 5);
        assertEquals(2, distinct.size());
        assertEquals('b', distinct.get(0));
        assertEquals('c', distinct.get(1));
    }

    @Test
    @DisplayName("Test reverse() with range")
    public void testReverseWithRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

        list.reverse(1, 4);
        assertEquals('a', list.get(0));
        assertEquals('d', list.get(1));
        assertEquals('c', list.get(2));
        assertEquals('b', list.get(3));
        assertEquals('e', list.get(4));
    }

    @Test
    @DisplayName("Test split() with range")
    public void testSplitWithRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'));

        List<CharList> chunks = list.split(2, 7, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());

        assertEquals('c', chunks.get(0).get(0));
        assertEquals('d', chunks.get(0).get(1));
    }

    @Test
    @DisplayName("Test stream() with range")
    public void testStreamWithRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

        CharStream stream = list.stream(1, 4);
        assertNotNull(stream);
        char[] streamArray = stream.toArray();
        assertEquals(3, streamArray.length);
        assertEquals('b', streamArray[0]);
        assertEquals('c', streamArray[1]);
        assertEquals('d', streamArray[2]);
    }

    @Test
    @DisplayName("Test toCollection() with range")
    public void testToCollectionWithRange() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

        List<Character> result = list.toCollection(1, 4, ArrayList::new);
        assertEquals(3, result.size());
        assertEquals('b', result.get(0).charValue());
        assertEquals('c', result.get(1).charValue());
        assertEquals('d', result.get(2).charValue());
    }

    @Test
    @DisplayName("Test toSet() with range")
    public void testToSetWithRange() {
        list.addAll(CharList.of('a', 'b', 'b', 'c', 'd'));

        Set<Character> set = list.toSet(1, 4);
        assertEquals(2, set.size());
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
    }

    @Test
    @DisplayName("Test toMultiset() with range")
    public void testToMultisetWithRange() {
        list.addAll(CharList.of('a', 'b', 'b', 'c', 'b'));

        Multiset<Character> multiset = list.toMultiset(1, 4);
        assertEquals(2, multiset.getCount('b'));
        assertEquals(1, multiset.getCount('c'));
        assertEquals(0, multiset.getCount('a'));
    }

    @Test
    @DisplayName("Test edge cases for add operations")
    public void testAddEdgeCases() {
        list.add('a');
        list.add(1, 'b');
        assertEquals(2, list.size());
        assertEquals('b', list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, 'c'));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 'c'));
    }

    @Test
    @DisplayName("Test edge cases for remove operations")
    public void testRemoveEdgeCases() {
        assertFalse(list.remove('a'));
        assertFalse(list.removeAllOccurrences('a'));

        assertFalse(list.removeIf(c -> true));

        assertFalse(list.removeDuplicates());
        list.add('a');
        assertFalse(list.removeDuplicates());
    }

    @Test
    @DisplayName("Test edge cases for set operations")
    public void testSetOperationsEdgeCases() {
        CharList empty = new CharList();
        CharList nonEmpty = CharList.of('a', 'b', 'c');

        assertEquals(0, list.intersection(nonEmpty).size());
        assertEquals(0, nonEmpty.intersection(empty).size());

        assertEquals(0, list.difference(nonEmpty).size());
        assertEquals(3, nonEmpty.difference(empty).size());

        assertEquals(0, list.symmetricDifference(empty).size());
        assertEquals(3, nonEmpty.symmetricDifference(empty).size());
    }

    @Test
    @DisplayName("Test addAll edge cases")
    public void testAddAllEdgeCases() {
        assertFalse(list.addAll(new CharList()));
        assertFalse(list.addAll(new char[0]));
        assertFalse(list.addAll(0, new CharList()));
        assertFalse(list.addAll(0, new char[0]));

        list.addAll(CharList.of('a', 'b', 'c'));
        int originalSize = list.size();
        list.addAll(list);
        assertEquals(originalSize * 2, list.size());
    }

    @Test
    @DisplayName("Test replaceRange edge cases")
    public void testReplaceRangeEdgeCases() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

        list.replaceRange(1, 3, new CharList());
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('d', list.get(1));
        assertEquals('e', list.get(2));

        list.replaceRange(3, 3, CharList.of('f', 'g'));
        assertEquals(5, list.size());
        assertEquals('f', list.get(3));
        assertEquals('g', list.get(4));
    }

    @Test
    @DisplayName("Test containsAll with larger list")
    public void testContainsAllWithLargerList() {
        list.addAll(CharList.of('a', 'b'));
        CharList larger = CharList.of('a', 'b', 'c', 'd');

        assertFalse(list.containsAll(larger));

        assertTrue(larger.containsAll(list));
    }

    @Test
    @DisplayName("Test frequency edge cases")
    public void testOccurrencesOfEdgeCases() {
        assertEquals(0, list.frequency('a'));

        list.addAll(CharList.of('a', 'a', 'a', 'a', 'a'));
        assertEquals(5, list.frequency('a'));
        assertEquals(0, list.frequency('b'));
    }

    @Test
    @DisplayName("Test copy with negative step")
    public void testCopyWithNegativeStep() {
        list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

        CharList reversed = list.copy(4, 0, -1);
        assertEquals(4, reversed.size());
        assertEquals('e', reversed.get(0));
        assertEquals('d', reversed.get(1));
        assertEquals('c', reversed.get(2));
        assertEquals('b', reversed.get(3));
    }

    @Test
    @DisplayName("Test fill on empty list")
    public void testFillOnEmptyList() {
        list.fill('x');
        assertTrue(list.isEmpty());

        list.fill(0, 0, 'x');
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test random with invalid arguments")
    public void testRandomWithInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> CharList.random('z', 'a', 10));
        assertThrows(IllegalArgumentException.class, () -> CharList.random(new char[0], 10));
    }

    // ---- Additional tests for previously untested methods/overloads ----

    @Test
    public void testInternalArray() {
        list.add('a');
        list.add('b');
        char[] arr = list.internalArray();
        assertNotNull(arr);
        assertEquals('a', arr[0]);
        assertEquals('b', arr[1]);
        arr[0] = 'z';
        assertEquals('z', list.get(0));
    }

    @Test
    public void testInternalArray_empty() {
        char[] arr = list.internalArray();
        assertNotNull(arr);
    }

    @Test
    public void testAddAll_array_withIndex_invalid() {
        list.add('a');
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, new char[] { 'b' }));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(5, new char[] { 'b' }));
    }

    @Test
    public void testRemoveAll_array_empty() {
        list.add('a');
        list.add('b');
        assertFalse(list.removeAll(new char[] {}));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAt_multipleIndices() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.add('d');
        list.removeAt(0, 2);
        assertEquals(2, list.size());
        assertEquals('b', list.get(0));
        assertEquals('d', list.get(1));
    }

    @Test
    public void testRemoveAt_multipleIndices_empty() {
        list.add('a');
        list.removeAt(new int[] {});
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveRange_full() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.removeRange(0, 3);
        assertEquals(0, list.size());
    }

    @Test
    public void testMoveRange_forward() {
        CharList cl = CharList.of('a', 'b', 'c', 'd', 'e');
        cl.moveRange(0, 2, 3);
        assertEquals(5, cl.size());
    }

    @Test
    public void testMoveRange_backward() {
        CharList cl = CharList.of('a', 'b', 'c', 'd', 'e');
        cl.moveRange(3, 5, 0);
        assertEquals(5, cl.size());
    }

    @Test
    public void testReplaceRange_array_empty() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.replaceRange(1, 2, new char[] {});
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
    }

    @Test
    public void testReplaceRange_array_larger() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.replaceRange(1, 2, new char[] { 'x', 'y', 'z' });
        assertEquals(5, list.size());
    }

    @Test
    public void testReplaceAll_sameValue() {
        list.add('a');
        list.add('b');
        int count = list.replaceAll('a', 'a');
        assertEquals(1, count);
    }

    @Test
    public void testReplaceIf_allMatch() {
        list.add('a');
        list.add('a');
        list.add('a');
        assertTrue(list.replaceIf(c -> c == 'a', 'z'));
        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            assertEquals('z', list.get(i));
        }
    }

    @Test
    public void testFill_empty() {
        list.fill('x');
        assertEquals(0, list.size());
    }

    @Test
    public void testContainsAny_array_noMatch() {
        list.add('a');
        assertFalse(list.containsAny(new char[] {}));
    }

    @Test
    public void testContainsAll_array_empty() {
        list.add('a');
        assertTrue(list.containsAll(new char[] {}));
    }

    @Test
    public void testDisjoint_array_empty() {
        list.add('a');
        assertTrue(list.disjoint(new char[] {}));
    }

    @Test
    public void testIntersection_array_empty() {
        list.add('a');
        list.add('b');
        CharList result = list.intersection(new char[] {});
        assertEquals(0, result.size());
    }

    @Test
    public void testDifference_array_empty() {
        list.add('a');
        list.add('b');
        CharList result = list.difference(new char[] {});
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference_array_empty() {
        list.add('a');
        CharList result = list.symmetricDifference(new char[] {});
        assertEquals(1, result.size());
        assertEquals('a', result.get(0));
    }

    @Test
    public void testFrequency_multiple() {
        list.add('a');
        list.add('b');
        list.add('a');
        list.add('c');
        list.add('a');
        assertEquals(3, list.frequency('a'));
        assertEquals(1, list.frequency('b'));
        assertEquals(0, list.frequency('z'));
    }

    @Test
    public void testForEach_empty() {
        List<Character> collected = new ArrayList<>();
        list.forEach(c -> collected.add(c));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForEach_withRange_empty() {
        list.add('a');
        list.add('b');
        List<Character> collected = new ArrayList<>();
        list.forEach(1, 1, c -> collected.add(c));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testDistinct_empty() {
        CharList result = list.distinct(0, 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testDistinct_allSame() {
        list.add('a');
        list.add('a');
        list.add('a');
        CharList result = list.distinct(0, 3);
        assertEquals(1, result.size());
        assertEquals('a', result.get(0));
    }

    @Test
    public void testSort_alreadySorted() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.sort();
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    public void testParallelSort_empty() {
        list.parallelSort();
        assertEquals(0, list.size());
    }

    @Test
    public void testReverseSort_empty() {
        list.reverseSort();
        assertEquals(0, list.size());
    }

    @Test
    public void testBinarySearch_notFound() {
        list.add('a');
        list.add('c');
        list.add('e');
        assertTrue(list.binarySearch('b') < 0);
    }

    @Test
    public void testBinarySearch_rangeNotFound() {
        list.add('a');
        list.add('c');
        list.add('e');
        assertTrue(list.binarySearch(0, 3, 'b') < 0);
    }

    @Test
    public void testReverse_empty() {
        list.reverse();
        assertEquals(0, list.size());
    }

    @Test
    public void testReverse_withRange_empty() {
        list.add('a');
        list.add('b');
        list.reverse(0, 0);
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    public void testRotate_empty() {
        list.rotate(5);
        assertEquals(0, list.size());
    }

    @Test
    public void testRotate_zero() {
        list.add('a');
        list.add('b');
        list.rotate(0);
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    public void testShuffle_empty() {
        list.shuffle();
        assertEquals(0, list.size());
    }

    @Test
    public void testCopy_empty() {
        CharList copy = list.copy();
        assertEquals(0, copy.size());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopy_withRange_empty() {
        CharList copy = list.copy(0, 0);
        assertEquals(0, copy.size());
    }

    @Test
    public void testCopy_withStep_negative() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.add('d');
        CharList copy = list.copy(3, 0, -1);
        assertEquals(3, copy.size());
    }

    @Test
    public void testSplit_withRange() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.add('d');
        list.add('e');
        List<CharList> chunks = list.split(1, 4, 2);
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
    }

    @Test
    public void testSplit_empty() {
        List<CharList> chunks = list.split(0, 0, 1);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testTrimToSize_alreadyTrimmed() {
        CharList cl = CharList.of('a', 'b');
        cl.trimToSize();
        assertEquals(2, cl.size());
    }

    @Test
    public void testBoxed_empty() {
        List<Character> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxed_withRange_empty() {
        list.add('a');
        List<Character> boxed = list.boxed(0, 0);
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testToArray_empty() {
        char[] arr = list.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testToIntList_empty() {
        IntList il = list.toIntList();
        assertEquals(0, il.size());
    }

    @Test
    public void testToIntList_values() {
        list.add('A');
        list.add('B');
        IntList il = list.toIntList();
        assertEquals(2, il.size());
        assertEquals((int) 'A', il.get(0));
        assertEquals((int) 'B', il.get(1));
    }

    @Test
    public void testToCollection_withRange() {
        list.add('a');
        list.add('b');
        list.add('c');
        ArrayList<Character> result = list.toCollection(0, 2, ArrayList::new);
        assertEquals(2, result.size());
        assertEquals('a', result.get(0).charValue());
        assertEquals('b', result.get(1).charValue());
    }

    @Test
    public void testToCollection_empty() {
        ArrayList<Character> result = list.toCollection(0, 0, ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToMultiset_withRange() {
        list.add('a');
        list.add('a');
        list.add('b');
        Multiset<Character> multiset = list.toMultiset(0, 3, Multiset::new);
        assertEquals(2, multiset.getCount('a'));
        assertEquals(1, multiset.getCount('b'));
    }

    @Test
    public void testToMultiset_empty() {
        Multiset<Character> multiset = list.toMultiset(0, 0, Multiset::new);
        assertEquals(0, multiset.size());
    }

    @Test
    public void testIterator_hasNextAndNext() {
        list.add('a');
        list.add('b');
        CharIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals('a', iter.nextChar());
        assertTrue(iter.hasNext());
        assertEquals('b', iter.nextChar());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream_empty() {
        CharStream s = list.stream();
        assertEquals(0, s.toList().size());
    }

    @Test
    public void testStream_withRange_empty() {
        list.add('a');
        CharStream s = list.stream(0, 0);
        assertEquals(0, s.toList().size());
    }

    @Test
    public void testGetFirst_singleElement() {
        list.add('x');
        assertEquals('x', list.getFirst());
    }

    @Test
    public void testGetLast_singleElement() {
        list.add('x');
        assertEquals('x', list.getLast());
    }

    @Test
    public void testAddFirst_toNonEmpty() {
        list.add('b');
        list.addFirst('a');
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    public void testAddLast_toNonEmpty() {
        list.add('a');
        list.addLast('b');
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    public void testRemoveFirst_singleElement() {
        list.add('x');
        char removed = list.removeFirst();
        assertEquals('x', removed);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRemoveLast_singleElement() {
        list.add('x');
        char removed = list.removeLast();
        assertEquals('x', removed);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testEquals_self() {
        list.add('a');
        assertEquals(list, list);
    }

    @Test
    public void testEquals_empty() {
        CharList other = new CharList();
        assertEquals(list, other);
    }

    @Test
    public void testEquals_null() {
        assertFalse(list.equals(null));
    }

    @Test
    public void testEquals_differentType() {
        assertFalse(list.equals("not a list"));
    }

    @Test
    public void testEquals_differentContent() {
        list.add('a');
        CharList other = CharList.of('b');
        assertNotEquals(list, other);
    }

    @Test
    public void testEquals_differentSize() {
        list.add('a');
        CharList other = CharList.of('a', 'b');
        assertNotEquals(list, other);
    }

    @Test
    public void testHashCode_empty() {
        CharList other = new CharList();
        assertEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCode_sameContent() {
        list.add('a');
        list.add('b');
        CharList other = CharList.of('a', 'b');
        assertEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCode_differentContent() {
        list.add('a');
        CharList other = CharList.of('b');
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testToString_empty() {
        assertEquals("[]", list.toString());
    }

    @Test
    public void testToString_singleElement() {
        list.add('a');
        assertNotNull(list.toString());
        assertTrue(list.toString().contains("a"));
    }

    @Test
    public void testRetainAll_array_empty() {
        list.add('a');
        list.add('b');
        assertTrue(list.retainAll(new char[] {}));
        assertEquals(0, list.size());
    }

    @Test
    public void testRemoveDuplicates_empty() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testIsSorted_singleElement() {
        list.add('a');
        assertTrue(list.isSorted());
    }

    @Test
    public void testIsSorted_empty() {
        assertTrue(list.isSorted());
    }

    @Test
    public void testContainsDuplicates_twoDistinct() {
        list.add('a');
        list.add('b');
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_empty() {
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_singleElement() {
        list.add('a');
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void testSwap_sameIndex() {
        list.add('a');
        list.add('b');
        list.swap(0, 0);
        assertEquals('a', list.get(0));
    }

    @Test
    public void testSwap_outOfBounds() {
        list.add('a');
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 5));
    }

    @Test
    public void testMin_singleElement() {
        list.add('m');
        assertEquals('m', list.min().getAsChar());
    }

    @Test
    public void testMax_singleElement() {
        list.add('m');
        assertEquals('m', list.max().getAsChar());
    }

    @Test
    public void testMedian_singleElement() {
        list.add('m');
        assertEquals('m', list.median().getAsChar());
    }

    @Test
    public void testMedian_twoElements() {
        list.add('a');
        list.add('c');
        assertTrue(list.median().isPresent());
    }

    @Test
    public void testIndexOf_notFound() {
        list.add('a');
        list.add('b');
        assertEquals(-1, list.indexOf('z'));
    }

    @Test
    public void testIndexOf_fromIndexBeyondSize() {
        list.add('a');
        assertEquals(-1, list.indexOf('a', 5));
    }

    @Test
    public void testLastIndexOf_notFound() {
        list.add('a');
        list.add('b');
        assertEquals(-1, list.lastIndexOf('z'));
    }

    @Test
    public void testLastIndexOf_fromBackBeyondSize() {
        list.add('a');
        assertEquals(0, list.lastIndexOf('a', 5));
    }

    @Test
    public void testRemoveIf_noMatch() {
        list.add('a');
        list.add('b');
        assertFalse(list.removeIf(c -> c == 'z'));
        assertEquals(2, list.size());
    }

    @Test
    public void testReplaceIf_noMatch() {
        list.add('a');
        list.add('b');
        assertFalse(list.replaceIf(c -> c == 'z', 'x'));
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
    }

    @Test
    public void testRemoveAllOccurrences_empty() {
        assertFalse(list.removeAllOccurrences('a'));
    }

    @Test
    public void testContains_empty() {
        assertFalse(list.contains('a'));
    }

    @Test
    public void testFirst_singleElement() {
        list.add('x');
        OptionalChar result = list.first();
        assertTrue(result.isPresent());
        assertEquals('x', result.getAsChar());
    }

    @Test
    public void testLast_singleElement() {
        list.add('x');
        OptionalChar result = list.last();
        assertTrue(result.isPresent());
        assertEquals('x', result.getAsChar());
    }

    @Test
    public void testRandom_zeroLength() {
        CharList cl = CharList.random(0);
        assertEquals(0, cl.size());
    }

    @Test
    public void testRepeat_zeroLength() {
        CharList cl = CharList.repeat('a', 0);
        assertEquals(0, cl.size());
    }

    @Test
    public void testRange_sameStartEnd() {
        CharList cl = CharList.range('a', 'a');
        assertEquals(0, cl.size());
    }

    @Test
    public void testRangeClosed_sameStartEnd() {
        CharList cl = CharList.rangeClosed('a', 'a');
        assertEquals(1, cl.size());
        assertEquals('a', cl.get(0));
    }

    @Test
    public void testCopyOf_null() {
        CharList cl = CharList.copyOf(null);
        assertEquals(0, cl.size());
    }

    @Test
    public void testOf_null() {
        CharList cl = CharList.of((char[]) null);
        assertEquals(0, cl.size());
    }

    @Test
    public void testConstructorWithNullArray() {
        assertThrows(NullPointerException.class, () -> new CharList((char[]) null));
    }

    @Test
    public void testDeleteRange_sameIndices() {
        list.add('a');
        list.add('b');
        list.removeRange(1, 1);
        assertEquals(2, list.size());
    }

    @Test
    public void testDisjoint_notDisjoint() {
        list.add('a');
        list.add('b');
        assertFalse(list.disjoint(CharList.of('b', 'c')));
    }

    @Test
    public void testDisjoint_disjoint() {
        list.add('a');
        list.add('b');
        assertTrue(list.disjoint(CharList.of('c', 'd')));
    }

    @Test
    public void testDisjoint_arrayNotDisjoint() {
        list.add('a');
        list.add('b');
        assertFalse(list.disjoint(new char[] { 'b', 'c' }));
    }

    @Test
    public void testDisjoint_arrayDisjoint() {
        list.add('a');
        list.add('b');
        assertTrue(list.disjoint(new char[] { 'c', 'd' }));
    }

    // ---- Tests for inherited PrimitiveList methods ----

    @Test
    @SuppressWarnings("deprecation")
    public void testToList() {
        CharList cl = CharList.of('a', 'b', 'c');
        List<Character> result = cl.toList();
        assertEquals(3, result.size());
        assertEquals(Character.valueOf('a'), result.get(0));
        assertEquals(Character.valueOf('b'), result.get(1));
        assertEquals(Character.valueOf('c'), result.get(2));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToList_Empty() {
        List<Character> result = list.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToList_WithRange() {
        CharList cl = CharList.of('a', 'b', 'c', 'd');
        List<Character> result = cl.toList(1, 3);
        assertEquals(2, result.size());
        assertEquals(Character.valueOf('b'), result.get(0));
        assertEquals(Character.valueOf('c'), result.get(1));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToList_WithRange_OutOfBounds() {
        CharList cl = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> cl.toList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> cl.toList(0, 4));
    }

    @Test
    public void testDistinctNoArg() {
        CharList cl = CharList.of('a', 'b', 'a', 'c', 'b');
        CharList distinct = cl.distinct();
        assertEquals(3, distinct.size());
        assertEquals('a', distinct.get(0));
        assertEquals('b', distinct.get(1));
        assertEquals('c', distinct.get(2));
    }

    @Test
    public void testDistinctNoArg_Empty() {
        CharList distinct = list.distinct();
        assertEquals(0, distinct.size());
    }

    @Test
    public void testDistinctNoArg_NoDuplicates() {
        CharList cl = CharList.of('x', 'y', 'z');
        CharList distinct = cl.distinct();
        assertEquals(3, distinct.size());
    }

    @Test
    public void testSplitOneArg() {
        CharList cl = CharList.of('a', 'b', 'c', 'd', 'e');
        List<CharList> chunks = cl.split(2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testSplitOneArg_ExactDivision() {
        CharList cl = CharList.of('a', 'b', 'c', 'd');
        List<CharList> chunks = cl.split(2);
        assertEquals(2, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
    }

    @Test
    public void testSplitOneArg_Empty() {
        List<CharList> chunks = list.split(2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testToMultisetNoArg() {
        CharList cl = CharList.of('a', 'b', 'a', 'c');
        Multiset<Character> ms = cl.toMultiset();
        assertEquals(2, ms.get('a'));
        assertEquals(1, ms.get('b'));
        assertEquals(1, ms.get('c'));
    }

    @Test
    public void testToMultisetNoArg_Empty() {
        Multiset<Character> ms = list.toMultiset();
        assertEquals(0, ms.size());
    }

    @Test
    public void testToMultisetWithSupplier() {
        CharList cl = CharList.of('a', 'b', 'a');
        Multiset<Character> ms = cl.toMultiset(Multiset::new);
        assertEquals(2, ms.get('a'));
        assertEquals(1, ms.get('b'));
    }

    @Test
    public void testToCollectionOneArg() {
        CharList cl = CharList.of('a', 'b', 'c');
        ArrayList<Character> result = cl.toCollection(ArrayList::new);
        assertEquals(3, result.size());
        assertEquals(Character.valueOf('a'), result.get(0));
    }

    @Test
    public void testToCollectionOneArg_Empty() {
        ArrayList<Character> result = list.toCollection(ArrayList::new);
        assertTrue(result.isEmpty());
    }

}
