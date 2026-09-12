package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IntListContainsTest extends IntListTestSupport {

    @Test
    public void testContains() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        assertTrue(list.contains(3));
        assertFalse(list.contains(42));
    }

    @Test
    public void testContains_Empty() {
        IntList list = new IntList();
        assertFalse(list.contains(1));
    }

    @Test
    public void testContainsAny() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = IntList.of(4, 5, 6);
            assertFalse(list1.containsAny(list2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            int[] arr = { 4, 5, 6 };
            assertFalse(list.containsAny(arr));
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2, 3);
            assertFalse(a.containsAny(new int[] { 99 }));
        }
    }

    @Test
    public void testContainsAny_LargeData() {
        IntList large = new IntList();
        for (int i = 0; i < 15; i++) {
            large.add(i);
        }
        int[] query = { 100, 101, 102, 103, 104 };
        assertFalse(large.containsAny(query));
    }

    @Test
    public void testContainsAny_Empty() {
        IntList other = new IntList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3, 4, 5);
            IntList list2 = IntList.of(2, 4);
            assertTrue(list1.containsAll(list2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            int[] arr = { 2, 4 };
            assertTrue(list.containsAll(arr));
        }
        {
            list = new IntList();
            IntList a = IntList.of(1, 2);
            assertFalse(a.containsAll(new int[] { 1, 99 }));
        }
    }

    @Test
    public void testContainsAll_LargeData() {
        {
            list = new IntList();
            IntList large = new IntList();
            for (int i = 0; i < 15; i++) {
                large.add(i);
            }
            int[] query = { 0, 1, 2, 3, 4 };
            assertTrue(large.containsAll(query));
        }
        {
            list = new IntList();
            IntList large = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11);
            IntList query = IntList.of(1, 2, 3, 4);
            assertTrue(large.containsAll(query));
        }
    }

    @Test
    public void testContainsAll_Empty() {
        {
            list = new IntList();
            IntList list1 = IntList.of(1, 2, 3);
            IntList list2 = new IntList();
            assertTrue(list1.containsAll(list2));
        }
        {
            list = new IntList();
            list.addAll(new int[] { 1, 2, 3 });
            assertTrue(list.containsAll(new IntList()));
        }
    }

    @Test
    public void testContainsDuplicates() {
        IntList list = IntList.of(1, 2, 3, 4);
        assertFalse(list.containsDuplicates());
    }
}
