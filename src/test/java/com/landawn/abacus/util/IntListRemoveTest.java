package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

public class IntListRemoveTest extends IntListTestSupport {

    @Test
    public void testRemove() {
        IntList list = IntList.of(1, 2, 3, 2);
        assertTrue(list.remove(2));
        assertArrayEquals(new int[] { 1, 3, 2 }, list.toArray());
        assertFalse(list.remove(99));
    }

    @Test
    public void testRemoveAllOccurrences() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 2, 4, 2);
            assertTrue(list.removeAllOccurrences(2));
            assertArrayEquals(new int[] { 1, 3, 4 }, list.toArray());
            assertFalse(list.removeAllOccurrences(99));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertFalse(list.removeAllOccurrences(5));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveAllOccurrences_Empty() {
        assertFalse(list.removeAllOccurrences(5));
    }

    @Test
    public void testRemoveAll() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5, 2);
            IntList toRemove = IntList.of(2, 4);
            assertTrue(list.removeAll(toRemove));
            assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5, 2);
            int[] toRemove = { 2, 4 };
            assertTrue(list.removeAll(toRemove));
            assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
        }
    }

    @Test
    public void testRemoveAll_Empty() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = new IntList();
        assertFalse(list1.removeAll(list2));
        assertEquals(3, list1.size());
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final int[] shared = { 1, 2, 3, 1 };
        final IntList values = IntList.of(shared);
        final IntList removed = IntList.of(shared, 2);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new int[] { 3 }, values.toArray());
    }

    @Test
    public void testRemoveIf() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntPredicate predicate = (n) -> n % 2 == 0;
            assertTrue(list.removeIf(predicate));
            assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertFalse(list.removeIf(x -> x > 10));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveIf_Null() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> list.removeIf(null));
    }

    @Test
    public void testRemoveDuplicates() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 2, 3, 1, 4, 4);
            assertTrue(list.removeDuplicates());
            assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
            IntList noDuplicates = IntList.of(1, 2, 3);
            assertFalse(noDuplicates.removeDuplicates());
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 1, 2, 2, 2, 3, 3, 4, 5, 5 });
            assertTrue(list.removeDuplicates());
            assertEquals(5, list.size());
            for (int i = 1; i <= 5; i++) {
                assertEquals(i, list.get(i - 1));
            }
        }
        {
            list = new IntList();
            list.add(1);
            assertFalse(list.removeDuplicates());
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoveAt() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.removeAllAt(1, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    public void testRemoveAt_Empty() {
        IntList list = IntList.of(1, 2, 3);
        list.removeAllAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveRange() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(5, list.get(1));
    }

    @Test
    public void testRemoveRange_Empty() {
        IntList list = IntList.of(1, 2, 3);
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveRange_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(2, 1));
    }

    @Test
    public void testRemoveFirst() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4);
            assertEquals(1, list.removeFirst());
            assertEquals(4, list.removeLast());
            assertArrayEquals(new int[] { 2, 3 }, list.toArray());
        }
        {
            list = new IntList();
            list.add(5);
            assertEquals(5, list.removeFirst());
            assertTrue(list.isEmpty());

            list.add(10);
            assertEquals(10, list.removeLast());
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testRemoveFirst_Empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        int removed = list.removeLast();
        assertEquals(5, removed);
        assertEquals(4, list.size());
        assertEquals(4, list.get(3));
    }

    @Test
    public void testRemoveLast_Empty() {
        IntList list = new IntList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }
}
