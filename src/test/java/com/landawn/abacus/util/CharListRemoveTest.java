package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class CharListRemoveTest extends CharListTestSupport {

    @Test
    public void testRemove() {
        {
            list = new CharList();
            assertFalse(list.remove('a'));
            assertFalse(list.removeAllOccurrences('a'));

            assertFalse(list.removeIf(c -> true));

            assertFalse(list.removeDuplicates());
            list.add('a');
            assertFalse(list.removeDuplicates());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.add('a');
            assertTrue(list.remove('a'));
            assertEquals(2, list.size());
            assertArrayEquals(new char[] { 'b', 'a' }, list.toArray());
            assertFalse(list.remove('z'));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'b');
            assertTrue(list.remove('b'));
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('c', list.get(1));
            assertEquals('b', list.get(2));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            boolean removed = list.remove('d');
            assertFalse(removed);
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveAllOccurrences() {
        CharList list = CharList.of('a', 'b', 'c', 'b', 'd', 'b');
        assertTrue(list.removeAllOccurrences('b'));
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
        assertEquals('d', list.get(2));
    }

    @Test
    public void testRemoveAllOccurrences_Empty() {
        assertFalse(list.removeAllOccurrences('a'));
    }

    @Test
    public void testRemoveAll() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e', 'c'));
            assertTrue(list.removeAll(CharList.of('c', 'e', 'x')));
            assertArrayEquals(new char[] { 'a', 'b', 'd' }, list.toArray());
            assertFalse(list.removeAll(CharList.of('x', 'y')));
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c');
            CharList list2 = new CharList();
            assertFalse(list1.removeAll(list2));
            assertEquals(3, list1.size());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            char[] arr = { 'b', 'd' };
            assertTrue(list.removeAll(arr));
            assertEquals(2, list.size());
            assertEquals('a', list.get(0));
            assertEquals('c', list.get(1));
        }
    }

    @Test
    public void testRemoveAll_Empty() {
        list.add('a');
        list.add('b');
        assertFalse(list.removeAll(new char[] {}));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final char[] shared = { 'a', 'b', 'c', 'a' };
        final CharList values = CharList.of(shared);
        final CharList removed = CharList.of(shared, 2);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new char[] { 'c' }, values.toArray());
    }

    @Test
    public void testRemoveIf() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd', 'e');
            assertTrue(list.removeIf(c -> c == 'b' || c == 'd'));
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('c', list.get(1));
            assertEquals('e', list.get(2));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            assertFalse(list.removeIf(c -> c == 'z'));
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRemoveDuplicates() {
        list.addAll(CharList.of('a', 'b', 'a', 'c', 'b'));
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, list.toArray());
        assertFalse(CharList.of('x', 'y', 'z').removeDuplicates());
    }

    @Test
    public void testRemoveDuplicates_Empty() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRemoveAt() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.add('d');
        list.removeAllAt(0, 2);
        assertEquals(2, list.size());
        assertEquals('b', list.get(0));
        assertEquals('d', list.get(1));
    }

    @Test
    public void testRemoveAt_Empty() {
        list.add('a');
        list.removeAllAt(new int[] {});
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveRange() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.removeRange(0, 3);
        assertEquals(0, list.size());
    }

    @Test
    public void testRemoveFirst() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd'));
            assertEquals('a', list.removeFirst());
            assertEquals('d', list.removeLast());
            assertArrayEquals(new char[] { 'b', 'c' }, list.toArray());
            assertEquals(2, list.size());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            char removed = list.removeFirst();
            assertEquals('a', removed);
            assertEquals(2, list.size());
            assertEquals('b', list.get(0));
            assertEquals('c', list.get(1));
        }
        {
            list = new CharList();
            list.add('x');
            char removed = list.removeFirst();
            assertEquals('x', removed);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testRemoveFirst_Empty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            char removed = list.removeLast();
            assertEquals('c', removed);
            assertEquals(2, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
        }
        {
            list = new CharList();
            list.add('x');
            char removed = list.removeLast();
            assertEquals('x', removed);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testRemoveLast_Empty() {
        CharList list = new CharList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }
}
