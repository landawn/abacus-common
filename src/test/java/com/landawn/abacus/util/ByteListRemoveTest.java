package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class ByteListRemoveTest extends ByteListTestSupport {

    @Test
    public void testRemove() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 3);

        assertTrue(list.remove((byte) 3));
        assertArrayEquals(new byte[] { 1, 2, 4, 5, 3 }, list.toArray());

        assertTrue(list.removeAll(ByteList.of((byte) 1, (byte) 5, (byte) 9)));
        assertArrayEquals(new byte[] { 2, 4, 3 }, list.toArray());

        assertTrue(list.retainAll(new byte[] { 4, 3, 8 }));
        assertArrayEquals(new byte[] { 4, 3 }, list.toArray());
    }

    @Test
    public void testRemoveAllOccurrences() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 4, (byte) 2);
            assertTrue(list.removeAllOccurrences((byte) 2));
            assertEquals(3, list.size());
            assertArrayEquals(new byte[] { 1, 3, 4 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2);
            boolean changed = bl.removeAllOccurrences((byte) 2);
            assertTrue(changed);
            assertEquals(2, bl.size());
            assertFalse(bl.contains((byte) 2));
        }
    }

    @Test
    public void testRemoveAll() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            ByteList toRemove = ByteList.of((byte) 2, (byte) 4);
            assertTrue(list.removeAll(toRemove));
            assertEquals(3, list.size());
            assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            byte[] toRemove = { 2, 4 };
            assertTrue(list.removeAll(toRemove));
            assertEquals(3, list.size());
            assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
        }
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final byte[] shared = { 1, 2, 3, 1 };
        final ByteList values = ByteList.of(shared);
        final ByteList removed = ByteList.of(shared, 2);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new byte[] { 3 }, values.toArray());
    }

    @Test
    public void testRemoveIf() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
            assertTrue(list.removeIf(b -> b < 0));
            assertEquals(3, list.size());
            assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
            boolean changed = bl.removeIf(b -> b < 0);
            assertTrue(changed);
            assertEquals(3, bl.size());
            assertFalse(bl.contains((byte) -2));
            assertFalse(bl.contains((byte) -4));
        }
    }

    @Test
    public void testRemoveDuplicates() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 3, (byte) 1, (byte) 2, (byte) 3);
            assertTrue(list.removeDuplicates());
            assertArrayEquals(new byte[] { 1, 3, 2 }, list.toArray());

            ByteList sortedList = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3);
            assertTrue(sortedList.removeDuplicates());
            assertArrayEquals(new byte[] { 1, 2, 3 }, sortedList.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 2, (byte) 4);
            assertTrue(list.removeDuplicates());
            assertEquals(4, list.size());
            assertTrue(list.contains((byte) 1));
            assertTrue(list.contains((byte) 2));
            assertTrue(list.contains((byte) 3));
            assertTrue(list.contains((byte) 4));
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 3, (byte) 1, (byte) 2, (byte) 1, (byte) 3);
            boolean changed = bl.removeDuplicates();
            assertTrue(changed);
            assertEquals(3, bl.size());
            assertFalse(bl.containsDuplicates());
        }
    }

    @Test
    public void testRemoveDuplicates_Empty() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRemoveAt() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50);
            list.removeAllAt(1, 3);
            assertEquals(3, list.size());
            assertArrayEquals(new byte[] { 10, 30, 50 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            bl.removeAt(1);
            assertEquals(2, bl.size());
            assertEquals((byte) 10, bl.get(0));
            assertEquals((byte) 30, bl.get(1));
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 10, 20, 30 });
            byte removed = list.removeAt(1);
            assertEquals((byte) 20, removed);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRemoveAt_Empty() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        list.removeAllAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveRange() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            list.removeRange(1, 4);
            assertEquals(2, list.size());
            assertArrayEquals(new byte[] { 1, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3, 4, 5 });
            list.removeRange(1, 4);
            assertEquals(2, list.size());
            assertArrayEquals(new byte[] { 1, 5 }, list.toArray());
        }
    }

    @Test
    public void testRemoveRange_Empty() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        bl.removeRange(1, 1);
        assertEquals(3, bl.size());
    }

    @Test
    public void testRemoveFirst() {
        {
            list = new ByteList();
            assertThrows(NoSuchElementException.class, () -> list.removeFirst());
            assertThrows(NoSuchElementException.class, () -> list.removeLast());

            list.add((byte) 10);
            list.add((byte) 20);
            list.add((byte) 30);

            assertEquals((byte) 10, list.removeFirst());
            assertEquals(2, list.size());
            assertEquals((byte) 20, list.get(0));
            assertEquals((byte) 30, list.get(1));

            assertEquals((byte) 30, list.removeLast());
            assertEquals(1, list.size());
            assertEquals((byte) 20, list.get(0));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            byte removed = list.removeFirst();
            assertEquals(10, removed);
            assertEquals(2, list.size());
            assertEquals(20, list.get(0));
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 10, 20, 30 });
            byte removed = list.removeFirst();
            assertEquals((byte) 10, removed);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRemoveFirst_Empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            byte removed = list.removeLast();
            assertEquals(30, removed);
            assertEquals(2, list.size());
            assertEquals(20, list.get(1));
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 10, 20, 30 });
            byte removed = list.removeLast();
            assertEquals((byte) 30, removed);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRemoveLast_Empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }
}
