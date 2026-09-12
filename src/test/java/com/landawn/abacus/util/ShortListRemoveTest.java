package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.ShortPredicate;

public class ShortListRemoveTest extends ShortListTestSupport {

    @Test
    public void testRemove() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 4);

            assertTrue(list.remove((short) 2));
            assertArrayEquals(new short[] { 1, 3, 2, 4 }, list.toArray());

            assertTrue(list.removeAll(ShortList.of((short) 1, (short) 4)));
            assertArrayEquals(new short[] { 3, 2 }, list.toArray());

            list.addAll(ShortList.of((short) 5, (short) 6));
            assertTrue(list.retainAll(new short[] { 2, 6, 7 }));
            assertArrayEquals(new short[] { 2, 6 }, list.toArray());

            assertTrue(list.removeIf(val -> val > 5));
            assertArrayEquals(new short[] { 2 }, list.toArray());
        }
        {
            list = new ShortList();
            list.add((short) 10);
            list.add((short) 20);
            list.add((short) 10);
            list.add((short) 30);

            assertTrue(list.remove((short) 20));
            assertEquals(3, list.size());
            assertEquals((short) 10, list.get(0));
            assertEquals((short) 10, list.get(1));
            assertEquals((short) 30, list.get(2));

            assertFalse(list.remove((short) 40));
            assertEquals(3, list.size());

            assertTrue(list.removeAllOccurrences((short) 10));
            assertEquals(1, list.size());
            assertEquals((short) 30, list.get(0));

            assertFalse(list.removeAllOccurrences((short) 10));
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoveIf() {
        {
            list = new ShortList();
            for (short i = -100; i <= 100; i++) {
                list.add(i);
            }

            assertTrue(list.removeIf(s -> s % 3 == 0 || s % 5 == 0));

            for (int i = 0; i < list.size(); i++) {
                short value = list.get(i);
                assertTrue(value % 3 != 0 && value % 5 != 0);
            }

            assertTrue(list.removeIf(s -> s < 0));
            for (int i = 0; i < list.size(); i++) {
                assertTrue(list.get(i) >= 0);
            }
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            boolean result = list.removeIf(new ShortPredicate() {
                @Override
                public boolean test(short value) {
                    return value % 2 == 0;
                }
            });
            assertTrue(result);
            assertEquals(3, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 3, list.get(1));
            assertEquals((short) 5, list.get(2));
        }
    }

    @Test
    public void testRemoveDuplicates() {
        ShortList list = ShortList.of((short) 1, (short) 5, (short) 1, (short) 2, (short) 5);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new short[] { 1, 5, 2 }, list.toArray());

        ShortList list2 = ShortList.of((short) 1, (short) 5, (short) 1, (short) 2, (short) 5);
        assertTrue(list2.removeAllOccurrences((short) 1));
        assertArrayEquals(new short[] { 5, 2, 5 }, list2.toArray());
    }

    @Test
    public void testRemoveDuplicates_Empty() {
        assertFalse(ShortList.of().removeDuplicates());
    }

    @Test
    public void testRemoveAllOccurrences() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 2);
        boolean changed = sl.removeAllOccurrences((short) 2);
        assertTrue(changed);
        assertEquals(2, sl.size());
        assertFalse(sl.contains((short) 2));
    }

    @Test
    public void testRemoveAll() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            list.add((short) 2);

            ShortList toRemove = ShortList.of((short) 2);
            assertTrue(list.removeAll(toRemove));
            assertEquals(2, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 3, list.get(1));

            list.clear();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            assertTrue(list.removeAll(new short[] { 1, 3 }));
            assertEquals(1, list.size());
            assertEquals((short) 2, list.get(0));

            list.clear();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            ShortList toRetain = ShortList.of((short) 1, (short) 3);
            assertTrue(list.retainAll(toRetain));
            assertEquals(2, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 3, list.get(1));

            list.clear();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            assertTrue(list.retainAll(new short[] { 2 }));
            assertEquals(1, list.size());
            assertEquals((short) 2, list.get(0));
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            sl.removeAll(ShortList.of((short) 2, (short) 4));
            assertEquals(2, sl.size());
            assertFalse(sl.contains((short) 2));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            short[] arr = { (short) 2, (short) 4 };
            boolean result = list.removeAll(arr);
            assertTrue(result);
            assertEquals(2, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 3, list.get(1));
        }
    }

    @Test
    public void testRemoveAll_Empty() {
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3);
            assertFalse(sl.removeAll(ShortList.of()));
            assertEquals(3, sl.size());
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3);
            assertFalse(sl.removeAll(new short[0]));
            assertEquals(3, sl.size());
        }
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final short[] shared = { 1, 2, 3, 1 };
        final ShortList values = ShortList.of(shared);
        final ShortList removed = ShortList.of(shared, 2);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new short[] { 3 }, values.toArray());
    }

    @Test
    public void testRemoveAt_Empty() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3);
        sl.removeAllAt();
        assertEquals(3, sl.size());
    }

    @Test
    public void testRemoveRange() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        sl.removeRange(1, 3);
        assertEquals(3, sl.size());
        assertEquals((short) 1, sl.get(0));
        assertEquals((short) 4, sl.get(1));
        assertEquals((short) 5, sl.get(2));
    }

    @Test
    public void testRemoveRange_Empty() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3);
        sl.removeRange(1, 1);
        assertEquals(3, sl.size());
    }

    @Test
    public void testRemoveFirst() {
        {
            list = new ShortList();
            assertThrows(NoSuchElementException.class, () -> list.removeFirst());
            assertThrows(NoSuchElementException.class, () -> list.removeLast());

            list.add((short) 10);
            list.add((short) 20);
            list.add((short) 30);

            assertEquals((short) 10, list.removeFirst());
            assertEquals(2, list.size());
            assertEquals((short) 20, list.get(0));
            assertEquals((short) 30, list.get(1));

            assertEquals((short) 30, list.removeLast());
            assertEquals(1, list.size());
            assertEquals((short) 20, list.get(0));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            short removed = list.removeFirst();
            assertEquals((short) 1, removed);
            assertEquals(2, list.size());
            assertEquals((short) 2, list.get(0));
            assertEquals((short) 3, list.get(1));
        }
    }

    @Test
    public void testRemoveFirst_Empty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        short removed = list.removeLast();
        assertEquals((short) 3, removed);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
    }

    @Test
    public void testRemoveLast_Empty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }
}
