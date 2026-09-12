package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class CharListToTest extends CharListTestSupport {

    @Test
    public void testList() {
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b', 'c', 'd');
            List<Character> result = cl.toList(1, 3);
            assertEquals(2, result.size());
            assertEquals(Character.valueOf('b'), result.get(0));
            assertEquals(Character.valueOf('c'), result.get(1));
        }
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b', 'c');
            List<Character> result = cl.toList();
            assertEquals(3, result.size());
            assertEquals(Character.valueOf('a'), result.get(0));
            assertEquals(Character.valueOf('b'), result.get(1));
            assertEquals(Character.valueOf('c'), result.get(2));
        }
    }

    @Test
    public void testList_OutOfBounds() {
        CharList cl = CharList.of('a', 'b', 'c');
        assertThrows(IndexOutOfBoundsException.class, () -> cl.toList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> cl.toList(0, 4));
    }

    @Test
    public void testList_Empty() {
        List<Character> result = list.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetSet() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'b', 'c', 'd'));

            Set<Character> set = list.toSet(1, 4);
            assertEquals(2, set.size());
            assertTrue(set.contains('b'));
            assertTrue(set.contains('c'));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'b', 'a'));
            Set<Character> set = list.toSet();
            assertEquals(3, set.size());
            assertTrue(set.contains('a'));
            assertTrue(set.contains('b'));
            assertTrue(set.contains('c'));
        }
    }

    @Test
    public void testArray() {
        CharList list = CharList.of('a', 'b', 'c');
        char[] arr = list.toArray();
        assertEquals(3, arr.length);
        assertEquals('a', arr[0]);
        assertEquals('b', arr[1]);
        assertEquals('c', arr[2]);
    }

    @Test
    public void testArray_Empty() {
        char[] arr = list.toArray();
        assertEquals(0, arr.length);
    }

    @Test
    public void testInt() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            IntList intList = list.toIntList();
            assertEquals(3, intList.size());
            assertEquals('a', intList.get(0));
            assertEquals('b', intList.get(1));
            assertEquals('c', intList.get(2));
        }
        {
            list = new CharList();
            list.add('A');
            list.add('B');
            IntList il = list.toIntList();
            assertEquals(2, il.size());
            assertEquals('A', il.get(0));
            assertEquals('B', il.get(1));
        }
    }

    @Test
    public void testInt_Empty() {
        IntList il = list.toIntList();
        assertEquals(0, il.size());
    }

    @Test
    public void testCollection() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));

            List<Character> result = list.toCollection(1, 4, ArrayList::new);
            assertEquals(3, result.size());
            assertEquals('b', result.get(0).charValue());
            assertEquals('c', result.get(1).charValue());
            assertEquals('d', result.get(2).charValue());
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.add('c');
            ArrayList<Character> result = list.toCollection(0, 2, ArrayList::new);
            assertEquals(2, result.size());
            assertEquals('a', result.get(0).charValue());
            assertEquals('b', result.get(1).charValue());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            ArrayList<Character> collection = list.toCollection(0, 3, ArrayList::new);
            assertEquals(3, collection.size());
            assertEquals(Character.valueOf('a'), collection.get(0));
            assertEquals(Character.valueOf('b'), collection.get(1));
            assertEquals(Character.valueOf('c'), collection.get(2));
        }
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b', 'c');
            ArrayList<Character> result = cl.toCollection(ArrayList::new);
            assertEquals(3, result.size());
            assertEquals(Character.valueOf('a'), result.get(0));
        }
    }

    @Test
    public void testCollection_NaN() {
        list.addAll(CharList.of('a', 'b', 'a', 'c'));

        ArrayList<Character> arrayList = list.toCollection(ArrayList::new);
        assertEquals(Arrays.asList('a', 'b', 'a', 'c'), arrayList);

        java.util.Set<Character> set = list.toSet();
        assertEquals(new java.util.HashSet<>(Arrays.asList('a', 'b', 'c')), set);
    }

    @Test
    public void testCollection_Empty() {
        {
            list = new CharList();
            ArrayList<Character> result = list.toCollection(0, 0, ArrayList::new);
            assertTrue(result.isEmpty());
        }
        {
            list = new CharList();
            ArrayList<Character> result = list.toCollection(ArrayList::new);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testMultiset() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'a', 'c', 'b', 'a');
            com.landawn.abacus.util.Multiset<Character> multiset = list.toMultiset(0, 4, com.landawn.abacus.util.Multiset::new);
            assertNotNull(multiset);
            assertEquals(2, multiset.getCount('a'));
            assertEquals(1, multiset.getCount('b'));
            assertEquals(1, multiset.getCount('c'));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'b', 'c', 'b'));

            Multiset<Character> multiset = list.toMultiset(1, 4);
            assertEquals(2, multiset.getCount('b'));
            assertEquals(1, multiset.getCount('c'));
            assertEquals(0, multiset.getCount('a'));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('a');
            list.add('b');
            Multiset<Character> multiset = list.toMultiset(0, 3, Multiset::new);
            assertEquals(2, multiset.getCount('a'));
            assertEquals(1, multiset.getCount('b'));
        }
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b', 'a');
            Multiset<Character> ms = cl.toMultiset(Multiset::new);
            assertEquals(2, ms.getCount('a'));
            assertEquals(1, ms.getCount('b'));
        }
        {
            list = new CharList();
            CharList cl = CharList.of('a', 'b', 'a', 'c');
            Multiset<Character> ms = cl.toMultiset();
            assertEquals(2, ms.getCount('a'));
            assertEquals(1, ms.getCount('b'));
            assertEquals(1, ms.getCount('c'));
        }
    }

    @Test
    public void testMultiset_Empty() {
        {
            list = new CharList();
            Multiset<Character> multiset = list.toMultiset(0, 0, Multiset::new);
            assertEquals(0, multiset.size());
        }
        {
            list = new CharList();
            Multiset<Character> ms = list.toMultiset();
            assertEquals(0, ms.size());
        }
    }

    @Test
    public void testString() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            String str = list.toString();
            assertNotNull(str);
            assertTrue(str.contains("a"));
            assertTrue(str.contains("b"));
            assertTrue(str.contains("c"));
        }
        {
            list = new CharList();
            list.add('a');
            assertNotNull(list.toString());
            assertTrue(list.toString().contains("a"));
        }
    }

    @Test
    public void testString_Empty() {
        assertEquals("[]", list.toString());
    }
}
