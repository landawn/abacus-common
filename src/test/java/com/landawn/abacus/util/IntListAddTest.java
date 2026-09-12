package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class IntListAddTest extends IntListTestSupport {

    @Test
    public void testAdd() {
        {
            list = new IntList();
            IntList list = IntList.of(10, 30);
            list.add(1, 20);
            assertEquals(3, list.size());
            assertArrayEquals(new int[] { 10, 20, 30 }, list.toArray());
            assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 40));
        }
        {
            list = new IntList();
            IntList list = new IntList();
            list.add(10);
            list.add(20);
            assertEquals(2, list.size());
            assertArrayEquals(new int[] { 10, 20 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 2, 3, 4 });
            list.add(0, 1);
            assertEquals(4, list.size());
            assertEquals(1, list.get(0));
            assertEquals(2, list.get(1));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            list.add(list.size(), 4);
            assertEquals(4, list.size());
            assertEquals(4, list.get(3));
        }
    }

    @Test
    public void testAdd_LargeData() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> x % 2 == 0);
        assertEquals(count / 2, list.size());

        for (int i = 0; i < list.size(); i++) {
            assertEquals(i * 2 + 1, list.get(i));
        }
    }

    @Test
    public void testAdd_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 42));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 42));
    }

    @Test
    public void testAddAll() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 4);
            int[] toAdd = { 2, 3 };
            list.addAll(1, toAdd);
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2);
            int[] toAdd = { 3, 4 };
            list.addAll(toAdd);
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 5);
            IntList b = IntList.of(2, 3, 4);
            a.addAll(1, b);
            assertEquals(5, a.size());
            assertEquals(1, a.get(0));
            assertEquals(2, a.get(1));
            assertEquals(3, a.get(2));
            assertEquals(4, a.get(3));
            assertEquals(5, a.get(4));
        }
    }

    @Test
    public void testAddAll_Empty() {
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            IntList empty = new IntList();

            assertFalse(list.addAll(0, empty));
            assertFalse(list.addAll(1, empty));
            assertFalse(list.addAll(list.size(), empty));
        }
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = new IntList();
            assertFalse(list1.addAll(list2));
            assertEquals(3, list1.size());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            int[] arr = {};
            assertFalse(list.addAll(arr));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testAddAll_Null() {
        list.add(1);
        assertFalse(list.addAll((int[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testAddAll_OutOfBounds() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = IntList.of(4, 5);
            assertThrows(IndexOutOfBoundsException.class, () -> list1.addAll(-1, list2));
            assertThrows(IndexOutOfBoundsException.class, () -> list1.addAll(4, list2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            int[] arr = { 4, 5 };
            assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, arr));
            assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(4, arr));
        }
    }

    @Test
    public void testAddFirst() {
        IntList list = IntList.of(2, 3);
        list.addFirst(1);
        list.addLast(4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddLast() {
        IntList list = IntList.of(1, 2, 3);
        list.addLast(4);
        assertEquals(4, list.size());
        assertEquals(4, list.get(3));
    }
}
