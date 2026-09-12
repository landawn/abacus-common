package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class IntListToTest extends IntListTestSupport {

    @Test
    public void testList() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            List<Integer> result = list.toList(1, 4);
            assertEquals(3, result.size());
            assertEquals(Integer.valueOf(2), result.get(0));
            assertEquals(Integer.valueOf(3), result.get(1));
            assertEquals(Integer.valueOf(4), result.get(2));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            List<Integer> result = list.toList();
            assertEquals(3, result.size());
            assertEquals(Integer.valueOf(1), result.get(0));
            assertEquals(Integer.valueOf(2), result.get(1));
            assertEquals(Integer.valueOf(3), result.get(2));
        }
    }

    @Test
    public void testList_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(0, 4));
    }

    @Test
    public void testList_Empty() {
        List<Integer> result = list.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSet() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 2, 3, 4);
            Set<Integer> result = list.toSet(1, 4);
            assertEquals(2, result.size());
            assertTrue(result.contains(2));
            assertTrue(result.contains(3));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 1, 3);
            Set<Integer> result = list.toSet();
            assertEquals(3, result.size());
            assertTrue(result.contains(1));
            assertTrue(result.contains(2));
            assertTrue(result.contains(3));
        }
    }

    @Test
    public void testGetSet_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(0, 4));
    }

    @Test
    public void testGetSet_Empty() {
        Set<Integer> result = list.toSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testArray() {
        {
            list = new IntList();
            int[] a = { 1, 2, 3 };
            IntList list = IntList.of(a);
            assertArrayEquals(a, list.toArray());
            assertNotSame(a, list.toArray());
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            int[] arr = list.toArray();
            assertEquals(5, arr.length);
            assertEquals(1, arr[0]);
            assertEquals(5, arr[4]);

            list.set(0, 99);
            assertEquals(1, arr[0]);
        }
    }

    @Test
    public void testArray_Empty() {
        IntList list = new IntList();
        int[] arr = list.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testLong() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        LongList longList = list.toLongList();
        assertEquals(5, longList.size());
        assertEquals(1L, longList.get(0));
        assertEquals(5L, longList.get(4));
    }

    @Test
    public void testFloat() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        FloatList floatList = list.toFloatList();
        assertEquals(5, floatList.size());
        assertEquals(1.0f, floatList.get(0), 0.001f);
        assertEquals(5.0f, floatList.get(4), 0.001f);
    }

    @Test
    public void testDouble() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        DoubleList doubleList = list.toDoubleList();
        assertEquals(5, doubleList.size());
        assertEquals(1.0, doubleList.get(0), 0.001);
        assertEquals(5.0, doubleList.get(4), 0.001);
    }

    @Test
    public void testCollection() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3, 4, 5);
            Collection<Integer> collection = list.toCollection(1, 4, ArrayList::new);
            assertEquals(3, collection.size());
            assertTrue(collection.contains(2));
            assertTrue(collection.contains(4));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            ArrayList<Integer> result = list.toCollection(ArrayList::new);
            assertEquals(3, result.size());
            assertEquals(Integer.valueOf(1), result.get(0));
            assertEquals(Integer.valueOf(2), result.get(1));
            assertEquals(Integer.valueOf(3), result.get(2));
        }
    }

    @Test
    public void testCollection_Empty() {
        ArrayList<Integer> result = list.toCollection(ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollection_OutOfBounds() {
        IntList list = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(-1, 2, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(0, 4, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(2, 1, ArrayList::new));
    }

    @Test
    public void testMultiset() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 1, 3);
            Multiset<Integer> multiset = list.toMultiset(Multiset::new);
            assertEquals(2, multiset.count(Integer.valueOf(1)));
            assertEquals(1, multiset.count(Integer.valueOf(2)));
            assertEquals(1, multiset.count(Integer.valueOf(3)));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 1, 3);
            Multiset<Integer> multiset = list.toMultiset(0, 3);
            assertEquals(2, multiset.count(Integer.valueOf(1)));
            assertEquals(1, multiset.count(Integer.valueOf(2)));
            assertEquals(0, multiset.count(Integer.valueOf(3)));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 1, 3, 1);
            Multiset<Integer> multiset = list.toMultiset();
            assertEquals(3, multiset.count(Integer.valueOf(1)));
            assertEquals(1, multiset.count(Integer.valueOf(2)));
            assertEquals(1, multiset.count(Integer.valueOf(3)));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 2, 3, 3, 3);
            Multiset<Integer> multiset = list.toMultiset(0, 6, Multiset::new);
            assertEquals(3, multiset.getCount(3));
            assertEquals(2, multiset.getCount(2));
            assertEquals(1, multiset.getCount(1));
        }
    }

    @Test
    public void testMultiset_Empty() {
        Multiset<Integer> multiset = list.toMultiset();
        assertEquals(0, multiset.size());
    }

    @Test
    public void testMultiset_OutOfBounds() {
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2, Multiset::new));
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4, Multiset::new));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4));
        }
    }

    @Test
    public void testString() {
        {
            list = new IntList();
            list.add(Integer.MIN_VALUE);
            list.add(0);
            list.add(Integer.MAX_VALUE);
            String str = list.toString();
            assertTrue(str.contains(String.valueOf(Integer.MIN_VALUE)));
            assertTrue(str.contains("0"));
            assertTrue(str.contains(String.valueOf(Integer.MAX_VALUE)));
        }
        {
            list = new IntList();
            IntList list = IntList.of(1, 2, 3);
            assertEquals("[1, 2, 3]", list.toString());
            IntList emptyList = new IntList();
            assertEquals("[]", emptyList.toString());
        }
    }

    @Test
    public void testString_Empty() {
        IntList list = new IntList();
        String str = list.toString();
        assertNotNull(str);
    }
}
