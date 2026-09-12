package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CharListContainsTest extends CharListTestSupport {

    @Test
    public void testContains() {
        CharList list = CharList.of('a', 'b', 'c');
        assertTrue(list.contains('b'));
        assertFalse(list.contains('x'));
    }

    @Test
    public void testContains_Empty() {
        assertFalse(list.contains('a'));
    }

    @Test
    public void testContainsAny() {
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c');
            CharList list2 = CharList.of('c', 'd', 'e');
            assertTrue(list1.containsAny(list2));

            CharList list3 = CharList.of('x', 'y', 'z');
            assertFalse(list1.containsAny(list3));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            assertTrue(list.containsAny(new char[] { 'c', 'd' }));
            assertFalse(list.containsAny(new char[] { 'x', 'y' }));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            assertTrue(list.containsAny(CharList.of('c', 'd', 'e')));
            assertFalse(list.containsAny(CharList.of('x', 'y', 'z')));
        }
    }

    @Test
    public void testContainsAny_LargeData() {
        {
            list = new CharList();
            CharList large = new CharList();
            for (int i = 0; i < 15; i++) {
                large.add((char) ('a' + i % 4));
            }
            CharList query = CharList.of('a', 'x', 'y', 'z', 'w');
            assertTrue(large.containsAny(query));
        }
        {
            list = new CharList();
            CharList large = new CharList();
            for (int i = 0; i < 15; i++) {
                large.add((char) ('a' + i % 4));
            }
            char[] query = { 'a', 'b' };
            assertTrue(large.containsAny(query));
        }
    }

    @Test
    public void testContainsAny_Empty() {
        CharList list1 = CharList.of('a', 'b', 'c');
        CharList list2 = new CharList();
        assertFalse(list1.containsAny(list2));
    }

    @Test
    public void testContainsAll() {
        {
            list = new CharList();
            CharList list1 = CharList.of('a', 'b', 'c', 'd');
            CharList list2 = CharList.of('b', 'd');
            assertTrue(list1.containsAll(list2));

            CharList list3 = CharList.of('b', 'x');
            assertFalse(list1.containsAll(list3));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b'));
            CharList larger = CharList.of('a', 'b', 'c', 'd');

            assertFalse(list.containsAll(larger));

            assertTrue(larger.containsAll(list));
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            assertTrue(list.containsAll(new char[] { 'b', 'd' }));
            assertFalse(list.containsAll(new char[] { 'b', 'x' }));
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd'));
            assertTrue(list.containsAll(CharList.of('b', 'd')));
            assertFalse(list.containsAll(CharList.of('b', 'e')));
        }
    }

    @Test
    public void testContainsAll_LargeData() {
        {
            list = new CharList();
            CharList large = new CharList();
            for (int i = 0; i < 15; i++) {
                large.add((char) ('a' + i % 4));
            }
            char[] query = { 'a', 'b', 'c', 'd' };
            assertTrue(large.containsAll(query));
        }
        {
            list = new CharList();
            // needToSet: min(lenA,lenB) > 3 and max > 9
            CharList large = CharList.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k');
            CharList query = CharList.of('a', 'b', 'c', 'd');
            assertTrue(large.containsAll(query));
        }
    }

    @Test
    public void testContainsAll_Empty() {
        {
            list = new CharList();
            CharList a = new CharList();
            CharList b = CharList.of('a');
            assertFalse(a.containsAll(b));
        }
        {
            list = new CharList();
            list.add('a');
            assertTrue(list.containsAll(new char[] {}));
        }
    }

    @Test
    public void testContainsDuplicates() {
        list.add('a');
        list.add('b');
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_Empty() {
        assertFalse(list.containsDuplicates());
    }
}
