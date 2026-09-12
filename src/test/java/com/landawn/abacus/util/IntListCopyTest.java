package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IntListCopyTest extends IntListTestSupport {

    @Test
    public void testCopyOf() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(a, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testCopyOf_Null() {
        {
            list = new IntList();
            IntList list = IntList.copyOf((int[]) null);
            assertEquals(0, list.size());
        }
        {
            list = new IntList();
            IntList list = IntList.copyOf(null);
            assertTrue(list.isEmpty());
        }
        {
            // Unlike copyOf(int[]), the range overload does NOT tolerate a null array - not even when the
            // requested range is empty, which every other range method in the class does accept.
            assertThrows(IllegalArgumentException.class, () -> IntList.copyOf(null, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> IntList.copyOf(null, 0, 1));

            // The tag's real content is an ORDERING claim: the method documents both NullPointerException and
            // IndexOutOfBoundsException, and for a null array with a bad index both tags apply textually.
            // N.copyOfRange dereferences a.length as an ARGUMENT to checkFromToIndex, so the NPE always wins.
            assertThrows(IllegalArgumentException.class, () -> IntList.copyOf(null, -1, 0));
            assertThrows(IllegalArgumentException.class, () -> IntList.copyOf(null, 3, 1));
            assertThrows(IllegalArgumentException.class, () -> IntList.copyOf(null, Integer.MIN_VALUE, Integer.MAX_VALUE));
        }
    }

    @Test
    public void testCopyOf_InvalidRange() {
        {
            list = new IntList();
            int[] array = { 1, 2, 3, 4, 5 };
            assertThrows(IndexOutOfBoundsException.class, () -> IntList.copyOf(array, 3, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> IntList.copyOf(array, -1, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> IntList.copyOf(array, 2, 10));
        }
        {
            list = new IntList();
            int[] arr = { 1, 2, 3 };
            assertThrows(Exception.class, () -> IntList.copyOf(arr, 2, 1));
            assertThrows(Exception.class, () -> IntList.copyOf(arr, -1, 2));
        }
    }

    @Test
    public void testCopy() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = list1.copy();
            assertArrayEquals(list1.toArray(), list2.toArray());
            assertNotSame(list1, list2);
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });
            IntList copy = list.copy(0, 5, 3);
            assertEquals(2, copy.size());
            assertEquals(1, copy.get(0));
            assertEquals(4, copy.get(1));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntList copy = list.copy();
            assertEquals(5, copy.size());
            assertEquals(list.get(0), copy.get(0));

            list.set(0, 99);
            assertEquals(1, copy.get(0));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            IntList copy = list.copy(1, 4);
            assertEquals(3, copy.size());
            assertEquals(2, copy.get(0));
            assertEquals(4, copy.get(2));
        }
    }

    @Test
    public void testCopy_NegativeStep() {
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3, 4, 5 });
            IntList copy = list.copy(4, 0, -1);
            assertEquals(4, copy.size());
            assertEquals(5, copy.get(0));
            assertEquals(4, copy.get(1));
            assertEquals(3, copy.get(2));
            assertEquals(2, copy.get(3));
        }
        {
            list = new IntList();
            IntList list = IntList.of(0, 1, 2, 3, 4, 5);
            IntList copy = list.copy(5, 0, -1);
            assertEquals(5, copy.size());
            assertEquals(5, copy.get(0));
            assertEquals(1, copy.get(4));
        }
    }

    @Test
    public void testCopy_Empty() {
        IntList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopy_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(0, 4));
    }

    @Test
    public void testCopy_InvalidRange() {
        list.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testCopyDescendingFromSizeClampsToLogicalSize() {
        // regression: copy(size, -1, -step) clamped the start against the backing array's CAPACITY,
        // exposing phantom elements beyond the logical size when capacity > size
        final IntList padded = new IntList(10);
        padded.add(1);
        padded.add(2);
        padded.add(3);
        assertEquals(IntList.of(3, 2, 1), padded.copy(3, -1, -1));

        final IntList backed = new IntList(new int[] { 1, 2, 3, 99, 98 }, 3);
        assertEquals(IntList.of(3, 2, 1), backed.copy(3, -1, -1));
        assertEquals(IntList.of(3, 1), backed.copy(3, -1, -2));

        // trimmed-capacity behavior unchanged
        assertEquals(IntList.of(3, 2, 1), IntList.of(1, 2, 3).copy(3, -1, -1));
        assertEquals(IntList.of(2, 3), IntList.of(1, 2, 3).copy(1, 3, 1));
    }
}
