package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class BooleanListRemoveTest extends BooleanListTestSupport {

    @Test
    public void testRemove() {
        BooleanList list = BooleanList.of(true, false, true);
        boolean result = list.remove(false);
        assertTrue(result);
        assertEquals(2, list.size());
        assertTrue(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void testRemoveAllOccurrences() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        assertTrue(list.removeAllOccurrences(true));
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
        assertFalse(list.removeAllOccurrences(true));
    }

    @Test
    public void testRemoveAll() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false, true, false);
            BooleanList list2 = BooleanList.of(true);
            boolean result = list1.removeAll(list2);
            assertTrue(result);
            assertEquals(2, list1.size());
            assertFalse(list1.get(0));
            assertFalse(list1.get(1));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false);
            boolean[] arr = { true };
            boolean result = list.removeAll(arr);
            assertTrue(result);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRemoveAll_Empty() {
        {
            list = new BooleanList();
            BooleanList list1 = BooleanList.of(true, false);
            BooleanList list2 = new BooleanList();
            boolean result = list1.removeAll(list2);
            assertFalse(result);
            assertEquals(2, list1.size());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            assertFalse(list.removeAll(new boolean[] {}));
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final boolean[] shared = { true, false, true, false };
        final BooleanList values = BooleanList.of(shared);
        final BooleanList removed = BooleanList.of(shared, 1);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new boolean[] { false, false }, values.toArray());
    }

    @Test
    public void testRemoveIf() {
        BooleanList list = BooleanList.of(true, false, true, false);
        list.removeIf(val -> val);
        assertEquals(2, list.size());
        assertArrayEquals(new boolean[] { false, false }, list.toArray());
    }

    @Test
    public void testRemoveDuplicates() {
        BooleanList list = BooleanList.of(true, false, true, true, false);
        list.removeDuplicates();
        assertArrayEquals(new boolean[] { true, false }, list.toArray());

        BooleanList allSame = BooleanList.of(true, true, true);
        allSame.removeDuplicates();
        assertArrayEquals(new boolean[] { true }, allSame.toArray());
    }

    @Test
    public void testRemoveDuplicates_Empty() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRemoveAt() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            list.removeAllAt(1, 3);
            assertEquals(3, list.size());
            assertTrue(list.get(0));
            assertTrue(list.get(1));
            assertTrue(list.get(2));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);
            list.add(false);
            list.removeAllAt(0, 2);
            assertEquals(2, list.size());
            assertFalse(list.get(0));
            assertFalse(list.get(1));
        }
    }

    @Test
    public void testRemoveAt_Empty() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false);
            list.removeAllAt();
            assertEquals(2, list.size());
        }
        {
            list = new BooleanList();
            list.add(true);
            list.removeAllAt(new int[] {});
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoveRange() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true, false, true);
            list.removeRange(1, 4);
            assertEquals(2, list.size());
            assertTrue(list.get(0));
            assertTrue(list.get(1));
        }
        {
            list = new BooleanList();
            list.add(true);
            list.add(false);
            list.add(true);
            list.removeRange(0, 3);
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testRemoveRange_InvalidRange() {
        BooleanList list = BooleanList.of(true, false);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(2, 1));
    }

    @Test
    public void testRemoveFirst() {
        {
            list = new BooleanList();
            assertThrows(NoSuchElementException.class, () -> list.removeFirst());
            assertThrows(NoSuchElementException.class, () -> list.removeLast());

            list.add(true);
            list.add(false);
            list.add(true);

            assertEquals(true, list.removeFirst());
            assertEquals(2, list.size());
            assertEquals(false, list.get(0));
            assertEquals(true, list.get(1));

            assertEquals(true, list.removeLast());
            assertEquals(1, list.size());
            assertEquals(false, list.get(0));
        }
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            assertTrue(list.removeFirst());
            assertEquals("[false, true]", list.toString());
            assertTrue(list.removeLast());
            assertEquals("[false]", list.toString());
        }
    }

    @Test
    public void testRemoveFirst_Empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        {
            list = new BooleanList();
            BooleanList list = BooleanList.of(true, false, true);
            boolean removed = list.removeLast();
            assertTrue(removed);
            assertEquals(2, list.size());
            assertTrue(list.get(0));
            assertFalse(list.get(1));
        }
        {
            list = new BooleanList();
            list.add(false);
            boolean removed = list.removeLast();
            assertFalse(removed);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testRemoveLast_Empty() {
        BooleanList list = new BooleanList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }
}
