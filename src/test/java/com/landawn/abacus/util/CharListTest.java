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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.Collectors;

public class CharListTest extends CharListTestSupport {

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> list.forEach(0, 0, (com.landawn.abacus.util.function.CharConsumer) null));
    }

    @Test
    public void testConstructors() {
        {
            list = new CharList();
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
        {
            list = new CharList();
            char[] arr = { 'a', 'b', 'c', 'd', 'e' };
            CharList list = new CharList(arr, 3);
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new CharList(-1));
    }

    @Test
    public void testConstructors_InvalidSize() {
        char[] arr = { 'a', 'b', 'c' };
        assertThrows(IndexOutOfBoundsException.class, () -> new CharList(arr, 5));
        assertThrows(IllegalArgumentException.class, () -> new CharList(arr, -1));
    }

    @Test
    public void testConstructors_Null() {
        assertThrows(IllegalArgumentException.class, () -> new CharList((char[]) null));
    }

    @Test
    public void testLarge_LargeData() {
        CharList list = new CharList(1000);
        for (int i = 0; i < 1000; i++) {
            list.add((char) ('a' + (i % 26)));
        }
        assertEquals(1000, list.size());
        assertTrue(list.contains('a'));
    }

    @Test
    public void testEmpty_Empty() {
        CharList list = new CharList();
        assertFalse(list.remove('a'));
        assertEquals(0, list.frequency('a'));
        assertEquals(-1, list.indexOf('a'));
        assertEquals(-1, list.lastIndexOf('a'));
    }

    @Test
    public void testOf() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharList newList = CharList.of(array, 3);
        assertEquals(3, newList.size());
    }

    @Test
    public void testOf_Empty() {
        CharList list = CharList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOf_Null() {
        CharList newList = CharList.of((char[]) null);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testRemoveAt() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            list.removeAllAt(1, 3);
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('c', list.get(1));
            assertEquals('e', list.get(2));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            char deleted = list.removeAt(1);
            assertEquals('b', deleted);
            assertEquals(2, list.size());
            assertEquals('a', list.get(0));
            assertEquals('c', list.get(1));
        }
    }

    @Test
    public void testRemoveAt_Invalid() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(3));
    }

    @Test
    public void testRemoveAt_Empty() {
        CharList list = CharList.of('a', 'b', 'c');
        list.removeAllAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testArray() {
        {
            list = new CharList();
            list.addAll(CharList.of('q', 'w', 'e'));
            char[] internalArray = list.internalArray();
            assertEquals(3, list.size());

            assertTrue(internalArray.length >= 3);
            assertEquals('q', internalArray[0]);
            assertEquals('w', internalArray[1]);
            assertEquals('e', internalArray[2]);
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            char[] arr = list.internalArray();
            assertNotNull(arr);
            assertEquals('a', arr[0]);
            assertEquals('b', arr[1]);
            arr[0] = 'z';
            assertEquals('z', list.get(0));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            char[] arr = list.internalArray();
            assertNotNull(arr);
            assertTrue(arr.length >= 3);
        }
    }

    @Test
    public void testArray_Empty() {
        char[] arr = list.internalArray();
        assertNotNull(arr);
    }

    @Test
    public void testSingle() {
        CharList list = CharList.of('a');
        assertTrue(list.contains('a'));
        assertEquals(0, list.indexOf('a'));
        assertEquals(0, list.lastIndexOf('a'));
        assertEquals(1, list.frequency('a'));
    }

    @Test
    public void testNull_Null() {
        CharList list = CharList.of('a', 'b', 'c');
        assertFalse(list.addAll((CharList) null));
        assertFalse(list.addAll((char[]) null));
        assertFalse(list.removeAll((CharList) null));
        assertFalse(list.removeAll((char[]) null));
    }

    @Test
    public void testCopyOf() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharList newList = CharList.copyOf(array, 1, 4);
        assertEquals(3, newList.size());
        assertEquals('b', newList.get(0));
        assertEquals('c', newList.get(1));
        assertEquals('d', newList.get(2));
    }

    @Test
    public void testCopyOf_Null() {
        CharList cl = CharList.copyOf(null);
        assertEquals(0, cl.size());
    }

    @Test
    public void testRange() {
        {
            list = new CharList();
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, CharList.range('a', 'd').toArray());
            assertArrayEquals(new char[] {}, CharList.range('a', 'a').toArray());
            assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.range('a', 'f', 2).toArray());

            assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, CharList.rangeClosed('a', 'd').toArray());
            assertArrayEquals(new char[] { 'a' }, CharList.rangeClosed('a', 'a').toArray());
            assertArrayEquals(new char[] { 'a', 'c', 'e' }, CharList.rangeClosed('a', 'e', 2).toArray());
        }
        {
            list = new CharList();
            CharList list = CharList.rangeClosed('a', 'g', 2);
            assertEquals(4, list.size());
            assertEquals('a', list.get(0));
            assertEquals('c', list.get(1));
            assertEquals('e', list.get(2));
            assertEquals('g', list.get(3));
        }
    }

    @Test
    public void testRepeat() {
        CharList list = CharList.repeat('a', 5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertEquals('a', list.get(i));
        }
    }

    @Test
    public void testRandom() {
        {
            list = new CharList();
            CharList list = CharList.random('a', 'z', 10);
            assertEquals(10, list.size());
            for (int i = 0; i < list.size(); i++) {
                assertTrue(list.get(i) >= 'a' && list.get(i) < 'z');
            }
        }
        {
            list = new CharList();
            assertThrows(IllegalArgumentException.class, () -> CharList.random(new char[0], 10));
        }
        {
            list = new CharList();
            char[] candidates = { 'a', 'b', 'c', 'd' };
            CharList result = CharList.random(candidates, 10);
            assertEquals(10, result.size());
            for (int i = 0; i < result.size(); i++) {
                char c = result.get(i);
                assertTrue(c == 'a' || c == 'b' || c == 'c' || c == 'd');
            }
        }
        {
            list = new CharList();
            CharList list = CharList.random(10);
            assertEquals(10, list.size());
            assertNotNull(list);
        }
    }

    @Test
    public void testRandom_InvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> CharList.random('z', 'a', 10));
    }

    @Test
    public void testRandom_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> CharList.random('z', 'a', 10));
        assertThrows(IllegalArgumentException.class, () -> CharList.random(new char[0], 10));
    }

    @Test
    public void testRandom_OutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> CharList.random(new char[0], 5));
    }

    @Test
    public void testGetSet() {
        {
            list = new CharList();
            CharList empty = new CharList();
            CharList nonEmpty = CharList.of('a', 'b', 'c');

            assertEquals(0, list.intersection(nonEmpty).size());
            assertEquals(0, nonEmpty.intersection(empty).size());

            assertEquals(0, list.difference(nonEmpty).size());
            assertEquals(3, nonEmpty.difference(empty).size());

            assertEquals(0, list.symmetricDifference(empty).size());
            assertEquals(3, nonEmpty.symmetricDifference(empty).size());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.add('c');

            assertEquals('b', list.get(1));
            char oldValue = list.set(1, 'x');
            assertEquals('b', oldValue);
            assertEquals('x', list.get(1));
        }
    }

    @Test
    public void testGetSet_Invalid() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 'x'));
            assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 'x'));
        }
        {
            list = new CharList();
            list.add('a');
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        }
    }

    @Test
    public void testRetainAll() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'c'));
            assertTrue(list.retainAll(CharList.of('c', 'e', 'x')));
            assertArrayEquals(new char[] { 'c', 'e', 'c' }, list.toArray());
            assertFalse(list.retainAll(CharList.of('c', 'e', 'x')));
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c');
            CharList list2 = new CharList();
            assertTrue(list1.retainAll(list2));
            assertEquals(0, list1.size());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            char[] arr = { 'b', 'd', 'e' };
            assertTrue(list.retainAll(arr));
            assertEquals(2, list.size());
            assertEquals('b', list.get(0));
            assertEquals('d', list.get(1));
        }
    }

    @Test
    public void testRetainAll_Empty() {
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'c');
            CharList b = new CharList();
            assertTrue(a.retainAll(b));
            assertEquals(0, a.size());
        }
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'c');
            char[] b = {};
            assertTrue(a.retainAll(b));
            assertEquals(0, a.size());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            assertTrue(list.retainAll(new char[] {}));
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testBatch() {
        CharList a = CharList.of('a', 'b', 'a', 'c', 'a');
        CharList b = CharList.of('a');
        assertTrue(a.removeAll(b));
        assertEquals(2, a.size());
        assertEquals('b', a.get(0));
    }

    @Test
    public void testBatch_LargeData() {
        {
            list = new CharList();
            // large list retains only matching elements
            CharList a = new CharList();
            for (int i = 0; i < 15; i++) {
                a.add((char) ('a' + i % 4));
            }
            CharList b = CharList.of('a', 'b', 'c', 'd', 'e');
            boolean changed = a.retainAll(b);
            // all chars were in b, so no change
            assertFalse(changed);
        }
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'a', 'b', 'a', 'b', 'a', 'b', 'a', 'b', 'a');
            CharList b = CharList.of('a', 'a', 'a', 'a');
            assertTrue(a.removeAll(b));
            assertEquals(5, a.size());
            for (int i = 0; i < a.size(); i++) {
                assertEquals('b', a.get(i));
            }
        }
    }

    @Test
    public void testRemoveRange() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            list.removeRange(1, 4);
            assertEquals(2, list.size());
            assertEquals('a', list.get(0));
            assertEquals('e', list.get(1));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.removeRange(1, 1);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRemoveRange_InvalidRange() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(2, 1));
    }

    @Test
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
    public void testFill() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            list.fill(1, 4, 'x');
            assertEquals('a', list.get(0));
            assertEquals('x', list.get(1));
            assertEquals('x', list.get(2));
            assertEquals('x', list.get(3));
            assertEquals('e', list.get(4));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            list.fill('x');
            assertEquals(3, list.size());
            assertEquals('x', list.get(0));
            assertEquals('x', list.get(1));
            assertEquals('x', list.get(2));
        }
    }

    @Test
    public void testFill_Empty() {
        list.fill('x');
        assertTrue(list.isEmpty());

        list.fill(0, 0, 'x');
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFill_InvalidRange() {
        CharList list = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(-1, 2, 'x'));
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(0, 5, 'x'));
    }

    @Test
    public void testDisjoint() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            assertFalse(list.disjoint(CharList.of('c', 'd', 'e')));
            assertFalse(list.disjoint(new char[] { 'x', 'a' }));
            assertTrue(list.disjoint(CharList.of('x', 'y', 'z')));
            assertTrue(list.disjoint(new char[] {}));
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c');
            CharList list2 = CharList.of('x', 'y', 'z');
            assertTrue(list1.disjoint(list2));

            CharList list3 = CharList.of('c', 'd', 'e');
            assertFalse(list1.disjoint(list3));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            assertTrue(list.disjoint(new char[] { 'x', 'y', 'z' }));
            assertFalse(list.disjoint(new char[] { 'c', 'd', 'e' }));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            assertFalse(list.disjoint(new char[] { 'b', 'c' }));
        }
    }

    @Test
    public void testDisjoint_LargeData() {
        CharList a = CharList.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k');
        CharList b = CharList.of('a', 'b', 'c', 'd');
        assertFalse(a.disjoint(b));
    }

    @Test
    public void testDisjoint_Empty() {
        list.add('a');
        assertTrue(list.disjoint(new char[] {}));
    }

    @Test
    public void testIntersection() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'b', 'c'));
            CharList other = CharList.of('b', 'c', 'd', 'b');
            CharList intersection = list.intersection(other);
            assertArrayEquals(new char[] { 'b', 'b', 'c' }, intersection.toArray());
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c', 'd');
            CharList list2 = CharList.of('c', 'd', 'e', 'f');
            CharList result = list1.intersection(list2);
            assertEquals(2, result.size());
            assertTrue(result.contains('c'));
            assertTrue(result.contains('d'));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            char[] arr = { 'c', 'd', 'e', 'f' };
            CharList result = list.intersection(arr);
            assertEquals(2, result.size());
            assertTrue(result.contains('c'));
            assertTrue(result.contains('d'));
        }
    }

    @Test
    public void testIntersection_NaN() {
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
    public void testIntersection_Empty() {
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c');
            CharList list2 = new CharList();
            CharList result = list1.intersection(list2);
            assertEquals(0, result.size());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            CharList result = list.intersection(new char[] {});
            assertEquals(0, result.size());
        }
    }

    @Test
    public void testDifference() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'b', 'c'));
            CharList other = CharList.of('b', 'c', 'd');
            CharList difference = list.difference(other);
            assertArrayEquals(new char[] { 'a', 'b' }, difference.toArray());
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c', 'd');
            CharList list2 = CharList.of('c', 'd', 'e');
            CharList result = list1.difference(list2);
            assertEquals(2, result.size());
            assertTrue(result.contains('a'));
            assertTrue(result.contains('b'));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            char[] arr = { 'c', 'd', 'e' };
            CharList result = list.difference(arr);
            assertEquals(2, result.size());
            assertTrue(result.contains('a'));
            assertTrue(result.contains('b'));
        }
    }

    @Test
    public void testDifference_Empty() {
        list.add('a');
        list.add('b');
        CharList result = list.difference(new char[] {});
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        assertArrayEquals(new char[] { 'z', 'b', 'a' }, CharList.of('b', 'z').symmetricDifference(CharList.of('b', 'a', 'b')).toArray());
        assertArrayEquals(new char[] { 'z', 'b', 'a' }, CharList.of('b', 'z').symmetricDifference(new char[] { 'b', 'a', 'b' }).toArray());

        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            CharList other = CharList.of('c', 'd', 'e');
            CharList symDifference = list.symmetricDifference(other);
            symDifference.sort();
            assertArrayEquals(new char[] { 'a', 'b', 'd', 'e' }, symDifference.toArray());
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c');
            CharList list2 = CharList.of('c', 'd', 'e');
            CharList result = list1.symmetricDifference(list2);
            assertEquals(4, result.size());
            assertTrue(result.contains('a'));
            assertTrue(result.contains('b'));
            assertTrue(result.contains('d'));
            assertTrue(result.contains('e'));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            char[] arr = { 'c', 'd', 'e' };
            CharList result = list.symmetricDifference(arr);
            assertEquals(4, result.size());
            assertTrue(result.contains('a'));
            assertTrue(result.contains('b'));
            assertTrue(result.contains('d'));
            assertTrue(result.contains('e'));
        }
    }

    @Test
    public void testSymmetricDifference_Empty() {
        {
            list = new CharList();
            CharList a = new CharList();
            CharList b = CharList.of('a', 'b');
            CharList result = a.symmetricDifference(b);
            assertEquals(2, result.size());
        }
        {
            list = new CharList();
            list.add('a');
            CharList result = list.symmetricDifference(new char[] {});
            assertEquals(1, result.size());
            assertEquals('a', result.get(0));
        }
    }

    @Test
    public void testFrequency() {
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.add('a');
            list.add('c');
            list.add('a');
            assertEquals(3, list.frequency('a'));
            assertEquals(1, list.frequency('b'));
            assertEquals(0, list.frequency('z'));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'a', 'c', 'a');
            assertEquals(3, list.frequency('a'));
            assertEquals(1, list.frequency('b'));
            assertEquals(0, list.frequency('x'));
        }
        {
            list = new CharList();
            assertEquals(0, list.frequency('a'));

            list.addAll(CharList.of('a', 'a', 'a', 'a', 'a'));
            assertEquals(5, list.frequency('a'));
            assertEquals(0, list.frequency('b'));
        }
    }

    @Test
    public void testIndexOf() {
        {
            list = new CharList();
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
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'b');
            assertEquals(3, list.indexOf('b', 2));
            assertEquals(-1, list.indexOf('b', 4));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            assertEquals(-1, list.indexOf('z'));
        }
    }

    @Test
    public void testIndexOf_OutOfBounds() {
        list.add('a');
        assertEquals(-1, list.indexOf('a', 5));
    }

    @Test
    public void testLastIndexOf() {
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'a', 'b', 'a');
            assertEquals(2, a.lastIndexOf('a', 2));
            assertEquals(-1, a.lastIndexOf('b', 0));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'b');
            assertEquals(3, list.lastIndexOf('b'));
            assertEquals(-1, list.lastIndexOf('x'));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            assertEquals(-1, list.lastIndexOf('z'));
        }
    }

    @Test
    public void testLastIndexOf_OutOfBounds() {
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'a', 'b', 'a');
            assertEquals(4, a.lastIndexOf('a', 100));
            assertEquals(3, a.lastIndexOf('b', 100));
        }
        {
            list = new CharList();
            list.add('a');
            assertEquals(0, list.lastIndexOf('a', 5));
        }
    }

    @Test
    public void testMin() {
        {
            list = new CharList();
            assertTrue(list.min().isEmpty());
            assertTrue(list.max().isEmpty());
            assertTrue(list.lowerMedian().isEmpty());

            list.addAll(CharList.of('d', 'a', 'e', 'b', 'c'));
            assertEquals('a', list.min().get());
            assertEquals('e', list.max().get());
            assertEquals('c', list.lowerMedian().get());

            assertEquals('a', list.min(1, 4).get());
            assertEquals('a', list.min(0, 2).get());
            assertEquals('e', list.max(1, 4).get());
            assertEquals('c', list.lowerMedian(0, 5).get());
        }
        {
            list = new CharList();
            list.addAll(CharList.of('e', 'b', 'd', 'a', 'c'));

            OptionalChar min = list.min(1, 4);
            assertTrue(min.isPresent());
            assertEquals('a', min.get());

            OptionalChar emptyMin = list.min(2, 2);
            assertFalse(emptyMin.isPresent());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'e', 'b', 'd', 'c');
            OptionalChar min = list.min(1, 4);
            assertTrue(min.isPresent());
            assertEquals('b', min.get());
        }
        {
            list = new CharList();
            list.add('m');
            assertEquals('m', list.min().getAsChar());
        }
    }

    @Test
    public void testMin_Empty() {
        CharList list = new CharList();
        OptionalChar min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMax() {
        {
            list = new CharList();
            list.addAll(CharList.of('e', 'b', 'd', 'a', 'c'));

            OptionalChar max = list.max(1, 4);
            assertTrue(max.isPresent());
            assertEquals('d', max.get());

            OptionalChar emptyMax = list.max(2, 2);
            assertFalse(emptyMax.isPresent());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'e', 'b', 'd', 'c');
            OptionalChar max = list.max(1, 4);
            assertTrue(max.isPresent());
            assertEquals('e', max.get());
        }
        {
            list = new CharList();
            CharList list = CharList.of('c', 'a', 'd', 'b');
            OptionalChar max = list.max();
            assertTrue(max.isPresent());
            assertEquals('d', max.get());
        }
        {
            list = new CharList();
            list.add('m');
            assertEquals('m', list.max().getAsChar());
        }
    }

    @Test
    public void testMax_Empty() {
        CharList list = new CharList();
        OptionalChar max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testMedian() {
        {
            list = new CharList();
            list.addAll(CharList.of('e', 'b', 'd', 'a', 'c', 'f'));

            OptionalChar median = list.lowerMedian(1, 5);
            assertTrue(median.isPresent());

            OptionalChar emptyMedian = list.lowerMedian(3, 3);
            assertFalse(emptyMedian.isPresent());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'e', 'c', 'b', 'd');
            OptionalChar median = list.lowerMedian(1, 4);
            assertTrue(median.isPresent());
            assertEquals('c', median.get());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'c', 'b');
            OptionalChar median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals('b', median.get());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('c');
            assertTrue(list.lowerMedian().isPresent());
        }
        {
            list = new CharList();
            list.add('m');
            assertEquals('m', list.lowerMedian().getAsChar());
        }
    }

    @Test
    public void testMedian_Empty() {
        CharList list = new CharList();
        OptionalChar median = list.lowerMedian();
        assertFalse(median.isPresent());
    }

    @Test
    public void testEach() {
        {
            list = new CharList();
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
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            final StringBuilder sb = new StringBuilder();
            list.forEach(1, 4, c -> sb.append(c));
            assertEquals("bcd", sb.toString());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            final StringBuilder sb = new StringBuilder();
            list.forEach(c -> sb.append(c));
            assertEquals("abc", sb.toString());
        }
    }

    @Test
    public void testEach_Empty() {
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            List<Character> collected = new ArrayList<>();
            list.forEach(1, 1, c -> collected.add(c));
            assertTrue(collected.isEmpty());
        }
        {
            list = new CharList();
            List<Character> collected = new ArrayList<>();
            list.forEach(c -> collected.add(c));
            assertTrue(collected.isEmpty());
        }
    }

    @Test
    public void testEach_Null() {
        // Empty lists do not evaluate callbacks; non-empty lists fail naturally when invoking them.
        final CharList empty = new CharList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.forEach((com.landawn.abacus.util.function.CharConsumer) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.removeIf((com.landawn.abacus.util.function.CharPredicate) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceIf((com.landawn.abacus.util.function.CharPredicate) null, 'x'));

        final CharList nonEmpty = CharList.of('a', 'b');
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.forEach((com.landawn.abacus.util.function.CharConsumer) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.removeIf((com.landawn.abacus.util.function.CharPredicate) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceIf((com.landawn.abacus.util.function.CharPredicate) null, 'x'));
    }

    @Test
    public void testFirst() {
        {
            list = new CharList();
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
        {
            list = new CharList();
            list.add('x');
            OptionalChar result = list.first();
            assertTrue(result.isPresent());
            assertEquals('x', result.getAsChar());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            OptionalChar first = list.first();
            assertTrue(first.isPresent());
            assertEquals('a', first.get());
        }
    }

    @Test
    public void testFirst_Empty() {
        CharList list = new CharList();
        OptionalChar first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        {
            list = new CharList();
            list.add('x');
            OptionalChar result = list.last();
            assertTrue(result.isPresent());
            assertEquals('x', result.getAsChar());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            OptionalChar last = list.last();
            assertTrue(last.isPresent());
            assertEquals('c', last.get());
        }
    }

    @Test
    public void testLast_Empty() {
        CharList list = new CharList();
        OptionalChar last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'a', 'c', 'b');
            CharList distinct = list.distinct(0, list.size());
            assertEquals(3, distinct.size());
            assertTrue(distinct.contains('a'));
            assertTrue(distinct.contains('b'));
            assertTrue(distinct.contains('c'));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'b', 'c', 'c', 'd'));

            CharList distinct = list.distinct(1, 5);
            assertEquals(2, distinct.size());
            assertEquals('b', distinct.get(0));
            assertEquals('c', distinct.get(1));
        }
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b', 'a', 'c', 'b');
            CharList distinct = cl.distinct();
            assertEquals(3, distinct.size());
            assertEquals('a', distinct.get(0));
            assertEquals('b', distinct.get(1));
            assertEquals('c', distinct.get(2));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('a');
            list.add('a');
            CharList result = list.distinct(0, 3);
            assertEquals(1, result.size());
            assertEquals('a', result.get(0));
        }
    }

    @Test
    public void testDistinct_Empty() {
        {
            list = new CharList();
            CharList result = list.distinct(0, 0);
            assertEquals(0, result.size());
        }
        {
            list = new CharList();
            CharList distinct = list.distinct();
            assertEquals(0, distinct.size());
        }
    }

    @Test
    public void testContainsDuplicates() {
        CharList list1 = CharList.of('a', 'b', 'a');
        assertTrue(list1.containsDuplicates());

        CharList list2 = CharList.of('a', 'b', 'c');
        assertFalse(list2.containsDuplicates());
    }

    @Test
    public void testIsSorted() {
        {
            list = new CharList();
            CharList sorted = CharList.of('a', 'b', 'c');
            assertTrue(sorted.isSorted());

            CharList unsorted = CharList.of('c', 'a', 'b');
            assertFalse(unsorted.isSorted());
        }
        {
            list = new CharList();
            list.add('a');
            assertTrue(list.isSorted());
        }
    }

    @Test
    public void testIsSorted_Empty() {
        assertTrue(list.isSorted());
    }

    @Test
    public void testSort() {
        {
            list = new CharList();
            CharList a = CharList.of('c', 'a', 'b', 'e', 'd');
            a.sort();
            assertEquals('a', a.get(0));
            assertEquals('b', a.get(1));
            assertEquals('c', a.get(2));
            assertEquals('d', a.get(3));
            assertEquals('e', a.get(4));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.add('c');
            list.sort();
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
        }
    }

    @Test
    public void testParallelSort() {
        CharList list = CharList.of('c', 'a', 'b');
        list.parallelSort();
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    public void testParallelSort_Empty() {
        list.parallelSort();
        assertEquals(0, list.size());
    }

    @Test
    public void testReverseSort() {
        CharList list = CharList.of('a', 'c', 'b');
        list.reverseSort();
        assertEquals('c', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('a', list.get(2));
    }

    @Test
    public void testReverseSort_Empty() {
        list.reverseSort();
        assertEquals(0, list.size());
    }

    @Test
    public void testBinarySearch() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'f', 'b', 'd', 'g', 'h'));
            CharList subList = list.copy(1, 5);
            subList.sort();
            list.replaceRange(1, 5, subList);

            int index = list.binarySearch(1, 5, 'd');
            assertEquals(2, index);

            int notFound = list.binarySearch(1, 5, 'e');
            assertTrue(notFound < 0);
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            assertEquals(2, list.binarySearch(0, 5, 'c'));
            assertTrue(list.binarySearch(0, 2, 'c') < 0);
        }
        {
            list = new CharList();
            list.add('a');
            list.add('c');
            list.add('e');
            assertTrue(list.binarySearch(0, 3, 'b') < 0);
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            assertEquals(2, list.binarySearch('c'));
            assertTrue(list.binarySearch('x') < 0);
        }
    }

    @Test
    public void testReverse() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            list.reverse(1, 4);
            assertEquals('a', list.get(0));
            assertEquals('d', list.get(1));
            assertEquals('c', list.get(2));
            assertEquals('b', list.get(3));
            assertEquals('e', list.get(4));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

            list.reverse(1, 4);
            assertEquals('a', list.get(0));
            assertEquals('d', list.get(1));
            assertEquals('c', list.get(2));
            assertEquals('b', list.get(3));
            assertEquals('e', list.get(4));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            list.reverse();
            assertEquals('c', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('a', list.get(2));
        }
    }

    @Test
    public void testReverse_Empty() {
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.reverse(0, 0);
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
        }
        {
            list = new CharList();
            list.reverse();
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testRotate() {
        final CharList extreme = CharList.of('a', 'b', 'c', 'd', 'e');
        extreme.rotate(Integer.MIN_VALUE);
        assertEquals(CharList.of('d', 'e', 'a', 'b', 'c'), extreme);

        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            list.rotate(-1);
            assertEquals('b', list.get(0));
            assertEquals('c', list.get(1));
            assertEquals('d', list.get(2));
            assertEquals('a', list.get(3));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.rotate(0);
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
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
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
            CharList list2 = list1.copy();

            list1.shuffle(new Random(12345L));
            list2.shuffle(new Random(12345L));

            assertArrayEquals(list1.toArray(), list2.toArray());
        }
        {
            list = new CharList();
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
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new CharList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> CharList.of('a').shuffle(null));
    }

    @Test
    public void testShuffle_Empty() {
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j');
            Random rnd = new Random(42);
            a.shuffle(rnd);
            assertEquals(10, a.size());
        }
        {
            list = new CharList();
            list.shuffle();
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testSwap() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            list.swap(0, 2);
            assertEquals('c', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('a', list.get(2));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.swap(0, 0);
            assertEquals('a', list.get(0));
        }
    }

    @Test
    public void testSwap_OutOfBounds() {
        list.add('a');
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 5));
    }

    @Test
    public void testCopy() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            CharList copy = list.copy();
            assertEquals(3, copy.size());
            assertEquals('a', copy.get(0));
            assertEquals('b', copy.get(1));
            assertEquals('c', copy.get(2));
            assertNotSame(list, copy);
        }
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
            CharList result = a.copy(0, 6, 2);
            assertEquals(3, result.size());
            assertEquals('a', result.get(0));
            assertEquals('c', result.get(1));
            assertEquals('e', result.get(2));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            CharList copy = list.copy(1, 4);
            assertEquals(3, copy.size());
            assertEquals('b', copy.get(0));
            assertEquals('c', copy.get(1));
            assertEquals('d', copy.get(2));
        }
    }

    @Test
    public void testCopy_NegativeStep() {
        final CharList spareCapacity = new CharList(new char[] { 'a', 'b', 'c', 'x', 'y' }, 3);
        assertEquals(CharList.of('c', 'b', 'a'), spareCapacity.copy(spareCapacity.size(), -1, -1));
        final StringBuilder backwards = new StringBuilder();
        spareCapacity.forEach(spareCapacity.size(), -1, backwards::append);
        assertEquals("cba", backwards.toString());

        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

            CharList reversed = list.copy(4, 0, -1);
            assertEquals(4, reversed.size());
            assertEquals('e', reversed.get(0));
            assertEquals('d', reversed.get(1));
            assertEquals('c', reversed.get(2));
            assertEquals('b', reversed.get(3));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.add('c');
            list.add('d');
            CharList copy = list.copy(3, 0, -1);
            assertEquals(3, copy.size());
        }
    }

    @Test
    public void testCopy_Empty() {
        {
            list = new CharList();
            CharList copy = list.copy();
            assertEquals(0, copy.size());
            assertNotSame(list, copy);
        }
        {
            list = new CharList();
            CharList copy = list.copy(0, 0);
            assertEquals(0, copy.size());
        }
    }

    @Test
    public void testSplit() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'));

            List<CharList> chunks = list.split(2, 7, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(1, chunks.get(2).size());

            assertEquals('c', chunks.get(0).get(0));
            assertEquals('d', chunks.get(0).get(1));
        }
        {
            list = new CharList();
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
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e', 'f');
            List<CharList> chunks = list.split(0, 6, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(2, chunks.get(2).size());
        }
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b', 'c', 'd', 'e');
            List<CharList> chunks = cl.split(2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(1, chunks.get(2).size());
        }
    }

    @Test
    public void testSplit_Empty() {
        {
            list = new CharList();
            List<CharList> chunks = list.split(0, 0, 1);
            assertTrue(chunks.isEmpty());
        }
        {
            list = new CharList();
            List<CharList> chunks = list.split(2);
            assertTrue(chunks.isEmpty());
        }
    }

    @Test
    public void testTrim() {
        {
            list = new CharList();
            CharList list = new CharList(100);
            list.add('a');
            list.add('b');
            CharList trimmed = list.trimToSize();
            assertEquals(2, trimmed.size());
            assertNotNull(trimmed);
        }
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b');
            cl.trimToSize();
            assertEquals(2, cl.size());
        }
    }

    @Test
    public void testClear() {
        CharList list = CharList.of('a', 'b', 'c');
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClear_Empty() {
        list.add('a');
        assertFalse(list.isEmpty());
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty_Empty() {
        CharList list = new CharList();
        assertTrue(list.isEmpty());
        list.add('a');
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        CharList list = new CharList();
        assertEquals(0, list.size());
        list.add('a');
        assertEquals(1, list.size());
        list.add('b');
        assertEquals(2, list.size());
    }

    @Test
    public void testBoxed() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            List<Character> boxedList = list.boxed();
            assertEquals(Arrays.asList('a', 'b', 'c'), boxedList);

            List<Character> subList = list.boxed(1, 3);
            assertEquals(Arrays.asList('b', 'c'), subList);
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            List<Character> boxed = list.boxed(1, 4);
            assertEquals(3, boxed.size());
            assertEquals(Character.valueOf('b'), boxed.get(0));
            assertEquals(Character.valueOf('c'), boxed.get(1));
            assertEquals(Character.valueOf('d'), boxed.get(2));
        }
    }

    @Test
    public void testBoxed_Empty() {
        {
            list = new CharList();
            list.add('a');
            List<Character> boxed = list.boxed(0, 0);
            assertTrue(boxed.isEmpty());
        }
        {
            list = new CharList();
            List<Character> boxed = list.boxed();
            assertTrue(boxed.isEmpty());
        }
    }

    @Test
    public void testIterator() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            com.landawn.abacus.util.CharIterator iter = list.iterator();
            assertNotNull(iter);
            assertTrue(iter.hasNext());
            assertEquals('a', iter.nextChar());
            assertEquals('b', iter.nextChar());
            assertEquals('c', iter.nextChar());
            assertFalse(iter.hasNext());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            CharIterator iter = list.iterator();
            assertTrue(iter.hasNext());
            assertEquals('a', iter.nextChar());
            assertTrue(iter.hasNext());
            assertEquals('b', iter.nextChar());
            assertFalse(iter.hasNext());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a');
            com.landawn.abacus.util.CharIterator iter = list.iterator();
            iter.nextChar();
            assertThrows(NoSuchElementException.class, () -> iter.nextChar());
        }
    }

    @Test
    public void testIterator_Empty() {
        CharList list = new CharList();
        com.landawn.abacus.util.CharIterator iter = list.iterator();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
            long count = list.stream().filter(c -> c > 'b').count();
            assertEquals(3, count);

            CharStream subStream = list.stream(1, 4);
            String result = subStream.mapToObj(c -> String.valueOf(c)).collect(Collectors.joining());
            assertEquals("bcd", result);
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

            CharStream stream = list.stream(1, 4);
            assertNotNull(stream);
            char[] streamArray = stream.toArray();
            assertEquals(3, streamArray.length);
            assertEquals('b', streamArray[0]);
            assertEquals('c', streamArray[1]);
            assertEquals('d', streamArray[2]);
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            CharStream stream = list.stream();
            assertNotNull(stream);
            assertEquals(3, stream.count());
        }
    }

    @Test
    public void testStream_Empty() {
        {
            list = new CharList();
            list.add('a');
            CharStream s = list.stream(0, 0);
            assertEquals(0, s.toList().size());
        }
        {
            list = new CharList();
            CharStream s = list.stream();
            assertEquals(0, s.toList().size());
        }
    }

    @Test
    public void testGetFirst() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            assertEquals('a', list.getFirst());
            assertEquals('c', list.getLast());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            assertEquals('a', list.getFirst());
        }
        {
            list = new CharList();
            list.add('x');
            assertEquals('x', list.getFirst());
        }
    }

    @Test
    public void testGetFirst_Empty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            assertEquals('c', list.getLast());
        }
        {
            list = new CharList();
            list.add('x');
            assertEquals('x', list.getLast());
        }
    }

    @Test
    public void testGetLast_Empty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testHashCode() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            CharList other = CharList.of('a', 'b', 'c');
            assertEquals(list.hashCode(), other.hashCode());

            CharList different = CharList.of('a', 'b', 'd');
            assertNotEquals(list.hashCode(), different.hashCode());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            CharList other = CharList.of('a', 'b');
            assertEquals(list.hashCode(), other.hashCode());
        }
    }

    @Test
    public void testHashCode_Empty() {
        CharList other = new CharList();
        assertEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            CharList other = CharList.of('a', 'b', 'c');
            assertTrue(list.equals(other));

            CharList different = CharList.of('a', 'b', 'd');
            assertFalse(list.equals(different));

            assertFalse(list.equals(null));
            assertFalse(list.equals("not a CharList"));
            assertTrue(list.equals(list));
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c');
            CharList list2 = CharList.of('a', 'b', 'c');
            CharList list3 = CharList.of('a', 'b', 'd');

            assertEquals(list1, list2);
            assertFalse(list1.equals(list3));
            assertEquals(list1.hashCode(), list2.hashCode());
        }
        {
            list = new CharList();
            list.add('a');
            CharList other = CharList.of('a', 'b');
            assertNotEquals(list, other);
        }
    }

    @Test
    public void testEquals_Empty() {
        CharList other = new CharList();
        assertEquals(list, other);
    }

    @Test
    public void testEquals_Null() {
        assertFalse(list.equals(null));
    }

    @Test
    public void testEnsureCapacity() {
        {
            list = new CharList();
            // Trigger capacity growth by adding many elements
            CharList a = new CharList(2);
            for (int i = 0; i < 15; i++) {
                a.add((char) ('a' + i % 10));
            }
            assertEquals(15, a.size());
        }
        {
            list = new CharList();
            CharList a = new CharList();
            for (int i = 0; i < 20; i++) {
                a.add((char) ('a' + i % 26));
            }
            assertEquals(20, a.size());
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
    public void reviewFixes20260906_randomWithCandidatesRejectsANegativeLengthUniformly() {
        // The single-candidate shortcut delegates to Array.repeat, which rejects a negative length with
        // IllegalArgumentException - so the exception type used to depend on how many candidates were passed,
        // and neither matched the documented NegativeArraySizeException for one of them.
        assertThrows(NegativeArraySizeException.class, () -> CharList.random(new char[] { 'a' }, -1));
        assertThrows(NegativeArraySizeException.class, () -> CharList.random(new char[] { 'a', 'b' }, -1));
        assertThrows(NegativeArraySizeException.class, () -> CharList.random(-1));
        assertThrows(NegativeArraySizeException.class, () -> CharList.random('a', 'z', -1));

        // The shortcut itself is unchanged for a valid length.
        assertEquals(CharList.of('a', 'a', 'a'), CharList.random(new char[] { 'a' }, 3));
        assertEquals(0, CharList.random(new char[] { 'a' }, 0).size());
        assertEquals(4, CharList.random(new char[] { 'a', 'b' }, 4).size());

        // ... and the empty-candidates guard still fires first.
        assertThrows(IllegalArgumentException.class, () -> CharList.random(new char[0], -1));
    }

    @Test
    public void reviewFixes20260906_descendingCopyClampsAgainstSizeNotTheBackingArray() {
        // CharList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final CharList withSpareCapacity = CharList.of(new char[] { 'a', 'b', 'c', 'd', 'e' }, 3);

        assertEquals(3, withSpareCapacity.size());
        assertEquals(5, withSpareCapacity.internalArray().length, "the test needs real spare capacity");

        assertEquals("[c, b, a]", withSpareCapacity.copy(3, -1, -1).toString(), "spare capacity must not leak into the result");
        assertEquals("[c, b, a]", withSpareCapacity.copy(2, -1, -1).toString(), "an in-range start is unaffected");

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
            final CharList self = CharList.of('a', 'b', 'c');
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final CharList expected = CharList.of('a', 'b', 'c');
            final CharList inserted = CharList.of('a', 'b', 'c');
            expected.addAll(index, CharList.of('a', 'b', 'c'));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final CharList appended = CharList.of('a', 'b', 'c');
        appended.addAll(appended);
        assertEquals("[a, b, c, a, b, c]", appended.toString());

        // The interesting middle case, spelled out.
        final CharList middle = CharList.of('a', 'b', 'c');
        middle.addAll(1, middle);
        assertEquals("[a, a, b, c, b, c]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final CharList a = CharList.of('a', 'b', 'c');
        assertFalse(a.removeAll((CharList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((char[]) null));
        assertFalse(a.removeAll(new char[0]));
        assertFalse(a.removeAll(new CharList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final CharList b = CharList.of('a', 'b', 'c');
        assertTrue(b.retainAll((CharList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final CharList c = CharList.of('a', 'b', 'c');
        assertTrue(c.retainAll((char[]) null));
        assertEquals(0, c.size());

        final CharList d = CharList.of('a', 'b', 'c');
        assertTrue(d.retainAll(new char[0]));
        assertEquals(0, d.size());

        final CharList e = CharList.of('a', 'b', 'c');
        assertTrue(e.retainAll(new CharList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final CharList empty = new CharList();
        assertFalse(empty.retainAll((CharList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final CharList f = CharList.of('a', 'b', 'c');
        assertTrue(f.retainAll(CharList.of('a')));
        assertEquals(1, f.size());
    }

    @Test
    public void testConstructors_Null_ArrayWithSize() {
        // Pins the documented @throws NullPointerException of CharList(char[], int): the null check runs
        // before the size check, so a null array is reported as NPE even when the size is also invalid.
        assertThrows(IllegalArgumentException.class, () -> new CharList((char[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> new CharList((char[]) null, 3));
        assertThrows(IllegalArgumentException.class, () -> new CharList((char[]) null, -1));

        final CharList backed = new CharList(new char[] { 'a', 'b', 'c' }, 2);
        assertEquals(2, backed.size());
        assertEquals('a', backed.get(0));
        assertEquals('b', backed.get(1));
    }
}
