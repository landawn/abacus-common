package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CharListAddTest extends CharListTestSupport {

    @Test
    public void testAdd() {
        {
            list = new CharList();
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
        {
            list = new CharList();
            CharList list = new CharList();
            list.add('a');
            list.add('b');
            list.add('c');
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'c');
            list.add(1, 'b');
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
        }
        {
            list = new CharList();
            CharList list = new CharList(2);
            list.add('a');
            list.add('b');
            list.add('c');
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testAdd_Invalid() {
        CharList list = CharList.of('a', 'b');
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 'x'));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(5, 'x'));
    }

    @Test
    public void testAddAll() {
        {
            list = new CharList();
            assertFalse(list.addAll(new CharList()));
            assertFalse(list.addAll(new char[0]));
            assertFalse(list.addAll(0, new CharList()));
            assertFalse(list.addAll(0, new char[0]));

            list.addAll(CharList.of('a', 'b', 'c'));
            int originalSize = list.size();
            list.addAll(list);
            assertEquals(originalSize * 2, list.size());
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b'));
            assertEquals(2, list.size());

            list.addAll(1, CharList.of('c', 'd'));
            assertArrayEquals(new char[] { 'a', 'c', 'd', 'b' }, list.toArray());
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b');
            CharList list2 = new CharList();
            assertFalse(list1.addAll(list2));
            assertEquals(2, list1.size());
        }
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'd');
            CharList list2 = CharList.of('b', 'c');
            assertTrue(list1.addAll(1, list2));
            assertEquals(4, list1.size());
            assertEquals('a', list1.get(0));
            assertEquals('b', list1.get(1));
            assertEquals('c', list1.get(2));
            assertEquals('d', list1.get(3));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'd');
            char[] arr = { 'b', 'c' };
            assertTrue(list.addAll(1, arr));
            assertEquals(4, list.size());
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
        }
    }

    @Test
    public void testAddAll_Empty() {
        CharList a = CharList.of('a', 'b');
        CharList b = new CharList();
        assertFalse(a.addAll(1, b));
        assertEquals(2, a.size());
    }

    @Test
    public void testAddAll_Invalid() {
        list.add('a');
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, new char[] { 'b' }));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(5, new char[] { 'b' }));
    }

    @Test
    public void testAddFirst() {
        {
            list = new CharList();
            list.addFirst('b');
            list.addFirst('a');
            list.addLast('c');
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, list.toArray());
        }
        {
            list = new CharList();
            list.add('b');
            list.addFirst('a');
            list.addLast('c');
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
        }
        {
            list = new CharList();
            CharList list = CharList.of('b', 'c');
            list.addFirst('a');
            assertEquals(3, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
            assertEquals('c', list.get(2));
        }
    }

    @Test
    public void testAddFirst_Empty() {
        {
            list = new CharList();
            list.add('b');
            list.addFirst('a');
            assertEquals(2, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
        }
        {
            list = new CharList();
            CharList list = new CharList();
            list.addFirst('a');
            assertEquals(1, list.size());
            assertEquals('a', list.get(0));
        }
    }

    @Test
    public void testAddLast() {
        CharList list = CharList.of('a', 'b');
        list.addLast('c');
        assertEquals(3, list.size());
        assertEquals('a', list.get(0));
        assertEquals('b', list.get(1));
        assertEquals('c', list.get(2));
    }

    @Test
    public void testAddLast_Empty() {
        {
            list = new CharList();
            list.add('a');
            list.addLast('b');
            assertEquals(2, list.size());
            assertEquals('a', list.get(0));
            assertEquals('b', list.get(1));
        }
        {
            list = new CharList();
            CharList list = new CharList();
            list.addLast('a');
            assertEquals(1, list.size());
            assertEquals('a', list.get(0));
        }
    }
}
