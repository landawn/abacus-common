package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class BooleanListAddTest extends BooleanListTestSupport {

    @Test
    public void testAdd() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, true);
            list.add(1, false);
            assertEquals(3, list.size());
            assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list = new BooleanList();
            list.add(true);
            list.add(false);
            assertEquals(2, list.size());
            assertEquals(true, list.get(0));
            assertEquals(false, list.get(1));
        }
    }

    @Test
    public void testAdd_Invalid() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, true));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, true));
    }

    @Test
    public void testAddAll() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, true);
            boolean[] toAdd = { false, false };
            list.addAll(1, toAdd);
            assertEquals(4, list.size());
            assertArrayEquals(new boolean[] { true, false, false, true }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true);
            BooleanList toAdd = BooleanList.of(false, true);
            list.addAll(toAdd);
            assertEquals(3, list.size());
            assertArrayEquals(new boolean[] { true, false, true }, list.toArray());
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = BooleanList.of(true, true);
            boolean result = list1.addAll(1, list2);
            assertTrue(result);
            assertEquals(4, list1.size());
            assertTrue(list1.get(0));
            assertTrue(list1.get(1));
            assertTrue(list1.get(2));
            assertFalse(list1.get(3));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false);
            boolean[] arr = { true, true };
            boolean result = list.addAll(1, arr);
            assertTrue(result);
            assertEquals(4, list.size());
            assertTrue(list.get(1));
            assertTrue(list.get(2));
        }
    }

    @Test
    public void testAddAll_Empty() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = new BooleanList();
            boolean result = list1.addAll(1, list2);
            assertFalse(result);
            assertEquals(2, list1.size());
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = new BooleanList();
            boolean result = list1.addAll(list2);
            assertFalse(result);
            assertEquals(2, list1.size());
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false);
            boolean[] arr = {};
            boolean result = list.addAll(arr);
            assertFalse(result);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testAddAll_Invalid() {
        {
            list = new BooleanList();
            list.add(true);
            assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, new boolean[] { false }));
            assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(5, new boolean[] { false }));
        }
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = BooleanList.of(true);
            assertThrows(IndexOutOfBoundsException.class, () -> list1.addAll(3, list2));
        }
    }

    @Test
    public void testAddFirst() {
        {
            list = new BooleanList();
            BooleanList list = new BooleanList();
            list.addLast(true);
            list.addFirst(false);
            list.addLast(false);
            assertArrayEquals(new boolean[] { false, true, false }, list.toArray());
        }
        {
            list = new BooleanList();
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
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false);
            list.addFirst(false);
            assertEquals(3, list.size());
            assertFalse(list.get(0));
            assertTrue(list.get(1));
            assertFalse(list.get(2));
        }
    }

    @Test
    public void testAddFirst_Empty() {
        list.add(false);
        list.addFirst(true);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }

    @Test
    public void testAddLast() {
        BooleanList list = BooleanList.of(true, false);
        list.addLast(true);
        assertEquals(3, list.size());
        assertTrue(list.get(2));
    }

    @Test
    public void testAddLast_Empty() {
        list.add(true);
        list.addLast(false);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertFalse(list.get(1));
    }
}
