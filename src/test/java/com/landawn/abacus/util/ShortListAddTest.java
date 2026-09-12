package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ShortListAddTest extends ShortListTestSupport {

    @Test
    public void testAdd() {
        {
            list = new ShortList();
            ShortList list = new ShortList();
            list.add((short) 5);
            list.add(0, (short) 1);
            list.addFirst((short) 0);
            list.addLast((short) 10);
            assertArrayEquals(new short[] { 0, 1, 5, 10 }, list.toArray());
            assertEquals((short) 1, list.get(1));

            short oldValue = list.set(1, (short) 99);
            assertEquals((short) 1, oldValue);
            assertEquals((short) 99, list.get(1));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 3);
            list.add(1, (short) 2);
            assertEquals(3, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 2, list.get(1));
            assertEquals((short) 3, list.get(2));
        }
    }

    @Test
    public void testAdd_OutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, (short) 100));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, (short) 100));
    }

    @Test
    public void testAddAll() {
        {
            list = new ShortList();
            ShortList other = ShortList.of((short) 1, (short) 2, (short) 3);
            assertTrue(list.addAll(other));
            assertEquals(3, list.size());

            assertFalse(list.addAll(new ShortList()));
            assertEquals(3, list.size());

            ShortList other2 = ShortList.of((short) 10, (short) 20);
            assertTrue(list.addAll(1, other2));
            assertEquals(5, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 10, list.get(1));
            assertEquals((short) 20, list.get(2));
            assertEquals((short) 2, list.get(3));
            assertEquals((short) 3, list.get(4));

            short[] arr = { 50, 60 };
            assertTrue(list.addAll(arr));
            assertEquals(7, list.size());

            short[] arr2 = { -1, -2 };
            assertTrue(list.addAll(0, arr2));
            assertEquals(9, list.size());
            assertEquals((short) -1, list.get(0));
            assertEquals((short) -2, list.get(1));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 5);
            list.addAll(1, new short[] { 2, 3, 4 });
            assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, list.toArray());
        }
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 4);
            ShortList list2 = ShortList.of((short) 2, (short) 3);
            boolean result = list1.addAll(1, list2);
            assertTrue(result);
            assertEquals(4, list1.size());
            assertEquals((short) 1, list1.get(0));
            assertEquals((short) 2, list1.get(1));
            assertEquals((short) 3, list1.get(2));
            assertEquals((short) 4, list1.get(3));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 4);
            short[] arr = { (short) 2, (short) 3 };
            boolean result = list.addAll(1, arr);
            assertTrue(result);
            assertEquals(4, list.size());
            assertEquals((short) 2, list.get(1));
            assertEquals((short) 3, list.get(2));
        }
    }

    @Test
    public void testAddAll_Empty() {
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2);
            ShortList list2 = new ShortList();
            boolean result = list1.addAll(list2);
            assertFalse(result);
            assertEquals(2, list1.size());
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2);
            boolean changed = sl.addAll(ShortList.of());
            assertFalse(changed);
            assertEquals(2, sl.size());
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2);
            assertFalse(sl.addAll(1, ShortList.of()));
            assertEquals(2, sl.size());
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2);
            assertFalse(sl.addAll(0, new short[0]));
            assertEquals(2, sl.size());
        }
    }

    @Test
    public void testAddFirst() {
        {
            list = new ShortList();
            list.add((short) 10);

            list.addFirst((short) 5);
            assertEquals(2, list.size());
            assertEquals((short) 5, list.get(0));
            assertEquals((short) 10, list.get(1));

            list.addLast((short) 15);
            assertEquals(3, list.size());
            assertEquals((short) 5, list.get(0));
            assertEquals((short) 10, list.get(1));
            assertEquals((short) 15, list.get(2));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 2, (short) 3);
            list.addFirst((short) 1);
            assertEquals(3, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 2, list.get(1));
            assertEquals((short) 3, list.get(2));
        }
    }

    @Test
    public void testAddLast() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        list.addLast((short) 3);
        assertEquals(3, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
    }
}
