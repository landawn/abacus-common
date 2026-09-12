package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ByteListAddTest extends ByteListTestSupport {

    @Test
    public void testAdd() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10);
            list.add((byte) 20);
            list.add(1, (byte) 15);
            list.addFirst((byte) 5);
            list.addLast((byte) 25);
            assertArrayEquals(new byte[] { 5, 10, 15, 20, 25 }, list.toArray());

            list.addAll(new byte[] { 30, 35 });
            assertArrayEquals(new byte[] { 5, 10, 15, 20, 25, 30, 35 }, list.toArray());

            list.addAll(0, ByteList.of((byte) 1, (byte) 2));
            assertArrayEquals(new byte[] { 1, 2, 5, 10, 15, 20, 25, 30, 35 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list = new ByteList();
            list.add((byte) 5);
            list.add(0, (byte) 1);
            list.add((byte) 10);

            assertEquals(3, list.size());
            assertEquals((byte) 1, list.get(0));
            assertEquals((byte) 10, list.get(2));

            byte oldValue = list.set(1, (byte) 7);
            assertEquals((byte) 5, oldValue);
            assertEquals((byte) 7, list.get(1));
            assertArrayEquals(new byte[] { 1, 7, 10 }, list.toArray());
        }
    }

    @Test
    public void testAdd_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, (byte) 0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, (byte) 0));
    }

    @Test
    public void testAddAll() {
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 4);
            ByteList list2 = ByteList.of((byte) 2, (byte) 3);
            assertTrue(list1.addAll(1, list2));
            assertEquals(4, list1.size());
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list1.toArray());
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2);
            ByteList list2 = ByteList.of((byte) 3, (byte) 4);
            assertTrue(list1.addAll(list2));
            assertEquals(4, list1.size());
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list1.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 4);
            byte[] arr = { 2, 3 };
            assertTrue(list.addAll(1, arr));
            assertEquals(4, list.size());
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 4, (byte) 5);
            boolean changed = bl.addAll(1, new byte[] { (byte) 2, (byte) 3 });
            assertTrue(changed);
            assertEquals(5, bl.size());
            assertEquals((byte) 1, bl.get(0));
            assertEquals((byte) 2, bl.get(1));
            assertEquals((byte) 3, bl.get(2));
            assertEquals((byte) 4, bl.get(3));
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2);
            boolean changed = bl.addAll(ByteList.of((byte) 3, (byte) 4));
            assertTrue(changed);
            assertEquals(4, bl.size());
            assertEquals((byte) 3, bl.get(2));
            assertEquals((byte) 4, bl.get(3));
        }
    }

    @Test
    public void testAddAll_Empty() {
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2);
            ByteList list2 = new ByteList();
            assertFalse(list1.addAll(list2));
            assertEquals(2, list1.size());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2);
            boolean changed = bl.addAll(1, new byte[] {});
            assertFalse(changed);
            assertEquals(2, bl.size());
        }
    }

    @Test
    public void testAddAll_Null() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertFalse(list.addAll((byte[]) null));
        assertEquals(2, list.size());
    }

    @Test
    public void testAddFirst() {
        {
            list = new ByteList();
            list.add((byte) 10);

            list.addFirst((byte) 5);
            assertEquals(2, list.size());
            assertEquals((byte) 5, list.get(0));
            assertEquals((byte) 10, list.get(1));

            list.addLast((byte) 15);
            assertEquals(3, list.size());
            assertEquals((byte) 5, list.get(0));
            assertEquals((byte) 10, list.get(1));
            assertEquals((byte) 15, list.get(2));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 20, (byte) 30);
            list.addFirst((byte) 10);
            assertEquals(3, list.size());
            assertEquals(10, list.get(0));
        }
    }

    @Test
    public void testAddLast() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20);
            list.addLast((byte) 30);
            assertEquals(3, list.size());
            assertEquals(30, list.get(2));
        }
        {
            list = new ByteList();
            list.add((byte) 10);
            list.addLast((byte) 20);
            assertEquals(2, list.size());
            assertEquals((byte) 20, list.get(1));
        }
    }
}
