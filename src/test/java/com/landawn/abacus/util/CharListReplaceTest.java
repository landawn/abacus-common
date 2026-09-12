package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CharListReplaceTest extends CharListTestSupport {

    @Test
    public void testReplaceRange() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c', 'd', 'e'));
            list.replaceRange(2, 4, CharList.of('x', 'y', 'z'));
            assertArrayEquals(new char[] { 'a', 'b', 'x', 'y', 'z', 'e' }, list.toArray());

            list.replaceRange(1, 4, CharList.of('m'));
            assertArrayEquals(new char[] { 'a', 'm', 'z', 'e' }, list.toArray());

            list.replaceRange(1, 2, CharList.of());
            assertArrayEquals(new char[] { 'a', 'z', 'e' }, list.toArray());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            CharList replacement = new CharList();
            list.replaceRange(1, 3, replacement);
            assertEquals(2, list.size());
            assertEquals('a', list.get(0));
            assertEquals('d', list.get(1));
        }
        {
            list = new CharList();
            CharList a = CharList.of('a', 'b', 'c', 'd', 'e');
            char[] rep = { 'x', 'y', 'z' };
            a.replaceRange(1, 3, rep);
            assertEquals(6, a.size());
            assertEquals('a', a.get(0));
            assertEquals('x', a.get(1));
            assertEquals('y', a.get(2));
            assertEquals('z', a.get(3));
            assertEquals('d', a.get(4));
            assertEquals('e', a.get(5));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            list.add('c');
            list.replaceRange(1, 2, new char[] { 'x', 'y', 'z' });
            assertEquals(5, list.size());
        }
    }

    @Test
    public void testReplaceRange_Empty() {
        list.add('a');
        list.add('b');
        list.add('c');
        list.replaceRange(1, 2, new char[] {});
        assertEquals(2, list.size());
        assertEquals('a', list.get(0));
        assertEquals('c', list.get(1));
    }

    @Test
    public void testReplaceAll() {
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'c'));
            list.replaceAll(c -> (char) (c + 1));
            assertArrayEquals(new char[] { 'b', 'c', 'd' }, list.toArray());
        }
        {
            list = new CharList();
            list.addAll(CharList.of('a', 'b', 'a', 'c'));
            int replacements = list.replaceAll('a', 'z');
            assertEquals(2, replacements);
            assertArrayEquals(new char[] { 'z', 'b', 'z', 'c' }, list.toArray());
        }
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c');
            list.replaceAll(c -> (char) (c + 1));
            assertEquals('b', list.get(0));
            assertEquals('c', list.get(1));
            assertEquals('d', list.get(2));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('b');
            int count = list.replaceAll('a', 'a');
            assertEquals(1, count);
        }
    }

    @Test
    public void testReplaceAll_Null() {
        CharList nonEmpty = CharList.of('a', 'b', 'c');
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceAll((com.landawn.abacus.util.function.CharUnaryOperator) null));

        CharList empty = new CharList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceAll((com.landawn.abacus.util.function.CharUnaryOperator) null));
    }

    @Test
    public void testReplaceAll_Empty() {
        CharList a = new CharList();
        int count = a.replaceAll('a', 'b');
        assertEquals(0, count);
    }

    @Test
    public void testReplaceIf() {
        {
            list = new CharList();
            CharList list = CharList.of('a', 'b', 'c', 'd');
            assertTrue(list.replaceIf(c -> c == 'b' || c == 'd', 'x'));
            assertEquals('a', list.get(0));
            assertEquals('x', list.get(1));
            assertEquals('c', list.get(2));
            assertEquals('x', list.get(3));
        }
        {
            list = new CharList();
            list.add('a');
            list.add('a');
            list.add('a');
            assertTrue(list.replaceIf(c -> c == 'a', 'z'));
            assertEquals(3, list.size());
            for (int i = 0; i < 3; i++) {
                assertEquals('z', list.get(i));
            }
        }
    }
}
