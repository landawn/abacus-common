package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class ByteListToTest extends ByteListTestSupport {

    @Test
    public void testList() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            List<Byte> result = list.toList(1, 4);
            assertEquals(3, result.size());
            assertEquals(Byte.valueOf((byte) 2), result.get(0));
            assertEquals(Byte.valueOf((byte) 3), result.get(1));
            assertEquals(Byte.valueOf((byte) 4), result.get(2));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            List<Byte> result = list.toList();
            assertEquals(3, result.size());
            assertEquals(Byte.valueOf((byte) 1), result.get(0));
            assertEquals(Byte.valueOf((byte) 2), result.get(1));
            assertEquals(Byte.valueOf((byte) 3), result.get(2));
        }
    }

    @Test
    public void testList_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(0, 4));
    }

    @Test
    public void testList_Empty() {
        List<Byte> result = list.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSet() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 4);
            Set<Byte> result = list.toSet(1, 4);
            assertEquals(2, result.size());
            assertTrue(result.contains((byte) 2));
            assertTrue(result.contains((byte) 3));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
            Set<Byte> result = list.toSet();
            assertEquals(3, result.size());
            assertTrue(result.contains((byte) 1));
            assertTrue(result.contains((byte) 2));
            assertTrue(result.contains((byte) 3));
        }
    }

    @Test
    public void testGetSet_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(0, 4));
    }

    @Test
    public void testGetSet_Empty() {
        Set<Byte> result = list.toSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testArray() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            byte[] arr = list.toArray();
            assertArrayEquals(new byte[] { 1, 2, 3 }, arr);
            arr[0] = 99;
            assertEquals(1, list.get(0));
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            byte[] array = list.toArray();
            assertEquals(3, array.length);
            assertEquals((byte) 1, array[0]);
            assertEquals((byte) 2, array[1]);
            assertEquals((byte) 3, array[2]);

            array[0] = 10;
            assertEquals((byte) 1, list.get(0));
        }
    }

    @Test
    public void testInt() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) -1);
            list.add((byte) 127);
            list.add((byte) -128);

            IntList intList = list.toIntList();
            assertEquals(4, intList.size());
            assertEquals(1, intList.get(0));
            assertEquals(-1, intList.get(1));
            assertEquals(127, intList.get(2));
            assertEquals(-128, intList.get(3));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            IntList intList = list.toIntList();
            assertEquals(3, intList.size());
            assertEquals(1, intList.get(0));
            assertEquals(2, intList.get(1));
            assertEquals(3, intList.get(2));
        }
    }

    @Test
    public void testCollection() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 10);

            ArrayList<Byte> collection = list.toCollection(ArrayList::new);
            assertEquals(List.of((byte) 10, (byte) 20, (byte) 10), collection);

            Multiset<Byte> multiset = list.toMultiset();
            assertEquals(2, multiset.count((byte) 10));
            assertEquals(1, multiset.count((byte) 20));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            ArrayList<Byte> result = list.toCollection(1, 4, ArrayList::new);
            assertEquals(3, result.size());
            assertEquals(Byte.valueOf((byte) 2), result.get(0));
            assertEquals(Byte.valueOf((byte) 3), result.get(1));
            assertEquals(Byte.valueOf((byte) 4), result.get(2));
        }
    }

    @Test
    public void testCollection_Empty() {
        ArrayList<Byte> result = list.toCollection(ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCollection_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(-1, 2, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(0, 4, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(2, 1, ArrayList::new));
    }

    @Test
    public void testMultiset() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
            Multiset<Byte> multiset = list.toMultiset(Multiset::new);
            assertEquals(2, multiset.count(Byte.valueOf((byte) 1)));
            assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
            assertEquals(1, multiset.count(Byte.valueOf((byte) 3)));
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 1);
            list.add((byte) 3);
            list.add((byte) 1);

            Multiset<Byte> multiset = list.toMultiset(0, list.size(), Multiset::new);
            assertEquals(3, multiset.count(Byte.valueOf((byte) 1)));
            assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
            assertEquals(1, multiset.count(Byte.valueOf((byte) 3)));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
            Multiset<Byte> multiset = list.toMultiset();
            assertEquals(3, multiset.count(Byte.valueOf((byte) 1)));
            assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
            assertEquals(1, multiset.count(Byte.valueOf((byte) 3)));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
            Multiset<Byte> multiset = list.toMultiset(0, 4, Multiset::new);
            assertEquals(4, multiset.size());
            assertEquals(2, multiset.count(Byte.valueOf((byte) 1)));
        }
    }

    @Test
    public void testMultiset_Empty() {
        Multiset<Byte> multiset = list.toMultiset();
        assertEquals(0, multiset.size());
    }

    @Test
    public void testMultiset_OutOfBounds() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2, Multiset::new));
            assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4, Multiset::new));
        }
    }

    @Test
    public void testString() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            String str = list.toString();
            assertNotNull(str);
            assertTrue(str.contains("1"));
            assertTrue(str.contains("2"));
            assertTrue(str.contains("3"));
        }
        {
            list = new ByteList();
            assertEquals("[]", list.toString());

            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            assertEquals("[1, 2, 3]", list.toString());
        }
    }

    @Test
    public void testString_Empty() {
        ByteList list = new ByteList();
        String str = list.toString();
        assertNotNull(str);
    }
}
