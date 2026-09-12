package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.ByteStream;

public class ByteListTest extends ByteListTestSupport {

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> list.forEach(0, 0, (com.landawn.abacus.util.function.ByteConsumer) null));
    }

    @Test
    public void testConstructors() {
        {
            list = new ByteList();
            ByteList list1 = new ByteList();
            assertTrue(list1.isEmpty());

            ByteList list2 = new ByteList(20);
            assertTrue(list2.isEmpty());
            assertEquals(20, list2.internalArray().length);

            byte[] data = { 1, 2, 3 };
            ByteList list3 = new ByteList(data);
            assertEquals(3, list3.size());
            data[0] = 5;
            assertEquals((byte) 5, list3.get(0));

            ByteList list4 = ByteList.of((byte) 1, (byte) 2);
            assertArrayEquals(new byte[] { 1, 2 }, list4.toArray());

            byte[] original = { 10, 20 };
            ByteList list5 = ByteList.copyOf(original);
            original[0] = 15;
            assertEquals((byte) 10, list5.get(0));
            assertArrayEquals(new byte[] { 10, 20 }, list5.toArray());

            ByteList list6 = ByteList.copyOf(new byte[] { 1, 2, 3, 4, 5 }, 1, 4);
            assertArrayEquals(new byte[] { 2, 3, 4 }, list6.toArray());
        }
        {
            list = new ByteList();
            ByteList list1 = new ByteList();
            assertEquals(0, list1.size());

            ByteList list2 = new ByteList(10);
            assertEquals(0, list2.size());

            byte[] arr = { 1, 2, 3 };
            ByteList list3 = new ByteList(arr);
            assertEquals(3, list3.size());
            assertEquals((byte) 1, list3.get(0));
            assertEquals((byte) 2, list3.get(1));
            assertEquals((byte) 3, list3.get(2));

            byte[] arr2 = { 1, 2, 3, 4, 5 };
            ByteList list4 = new ByteList(arr2, 3);
            assertEquals(3, list4.size());
            assertEquals((byte) 1, list4.get(0));
            assertEquals((byte) 2, list4.get(1));
            assertEquals((byte) 3, list4.get(2));

            assertThrows(IndexOutOfBoundsException.class, () -> new ByteList(arr2, 10));

            assertThrows(IllegalArgumentException.class, () -> new ByteList(null));
            assertThrows(IllegalArgumentException.class, () -> new ByteList(null, 0));
        }
        {
            list = new ByteList();
            byte[] data = { 1, 2, 3 };
            ByteList list = new ByteList(data);
            assertEquals(3, list.size());
            assertArrayEquals(data, list.toArray());
            data[0] = 10;
            assertEquals((byte) 10, list.get(0), "Internal array modification should be reflected");
        }
        {
            list = new ByteList();
            ByteList zeroCapList = new ByteList(0);
            assertEquals(0, zeroCapList.size());

            zeroCapList.add((byte) 1);
            assertEquals(1, zeroCapList.size());
            assertEquals((byte) 1, zeroCapList.get(0));
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ByteList(-1));
    }

    @Test
    public void testConstructors_InvalidSize() {
        byte[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new ByteList(arr, 5));
        assertThrows(IllegalArgumentException.class, () -> new ByteList(arr, -1));
    }

    @Test
    public void testArray() {
        {
            list = new ByteList();
            ByteList list = new ByteList(10);
            list.add((byte) 1);
            list.add((byte) 2);

            byte[] internalArray = list.internalArray();
            assertEquals(10, internalArray.length);
            assertEquals((byte) 1, internalArray[0]);
            assertEquals((byte) 2, internalArray[1]);

            internalArray[0] = (byte) 99;
            assertEquals((byte) 99, list.get(0));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            byte[] arr = list.internalArray();
            assertNotNull(arr);
            arr[0] = 99;
            assertEquals(99, list.get(0));
        }
    }

    @Test
    public void testEmpty() {
        ByteList list = new ByteList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void testOf() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(3, list.size());
        assertEquals((byte) 10, list.get(0));
        assertEquals((byte) 20, list.get(1));
        assertEquals((byte) 30, list.get(2));
        assertEquals("[10, 20, 30]", list.toString());
    }

    @Test
    public void testOf_Empty() {
        ByteList list = ByteList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOf_Null() {
        ByteList list = ByteList.of((byte[]) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testOf_InvalidSize() {
        byte[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ByteList.of(arr, 5));
    }

    @Test
    public void testRemoveAt() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);

        byte deleted = list.removeAt(1);
        assertEquals((byte) 2, deleted);
        assertArrayEquals(new byte[] { 1, 3, 4, 5, 6 }, list.toArray());

        list.removeRange(1, 3);
        assertArrayEquals(new byte[] { 1, 5, 6 }, list.toArray());

        list.removeAllAt(0, 2);
        assertArrayEquals(new byte[] { 5 }, list.toArray());
    }

    @Test
    public void testRemoveAt_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
    }

    @Test
    public void testBulk() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.addAll(ByteList.of((byte) 4, (byte) 5));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, list.toArray());

        list.removeAll(ByteList.of((byte) 2, (byte) 4));
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());

        list.retainAll(ByteList.of((byte) 3, (byte) 5, (byte) 7));
        assertArrayEquals(new byte[] { 3, 5 }, list.toArray());
    }

    @Test
    public void testConditional() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 4, (byte) 2);

        assertTrue(list.removeAllOccurrences((byte) 2));
        assertArrayEquals(new byte[] { 1, 3, 4 }, list.toArray());

        list.add((byte) -1);
        assertTrue(list.removeIf(b -> b > 2));
        assertArrayEquals(new byte[] { 1, -1 }, list.toArray());
    }

    @Test
    public void testSearch() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20, (byte) 10);
        assertTrue(list.contains((byte) 30));
        assertFalse(list.contains((byte) 99));
        assertEquals(0, list.indexOf((byte) 10));
        assertEquals(4, list.lastIndexOf((byte) 10));
        assertEquals(1, list.indexOf((byte) 20));
        assertEquals(3, list.lastIndexOf((byte) 20));
        assertEquals(-1, list.indexOf((byte) 99));
    }

    @Test
    public void testModifications() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);

        list.reverse();
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, list.toArray());

        list.rotate(1);
        assertArrayEquals(new byte[] { 1, 4, 3, 2 }, list.toArray());

        list.swap(1, 3);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());

        list.fill((byte) 0);
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, list.toArray());
    }

    @Test
    public void testConversions() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);

        List<Byte> boxed = list.boxed();
        assertEquals(List.of((byte) 10, (byte) 20), boxed);

        IntList intList = list.toIntList();
        assertArrayEquals(new int[] { 10, 20 }, intList.toArray());

        assertEquals(30, list.stream().sum());
    }

    @Test
    public void testMove() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.moveRange(1, 3, 3);
        assertArrayEquals(new byte[] { 1, 4, 5, 2, 3 }, list.toArray());

        list.replaceRange(0, 2, ByteList.of((byte) 9, (byte) 8, (byte) 7));
        assertArrayEquals(new byte[] { 9, 8, 7, 5, 2, 3 }, list.toArray());
    }

    @Test
    public void testReplace() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40);
        assertTrue(list.replaceIf(b -> b > 25, (byte) 99));
        assertArrayEquals(new byte[] { 10, 20, 99, 99 }, list.toArray());

        list.replaceAll(b -> (byte) (b / 10));
        assertArrayEquals(new byte[] { 1, 2, 9, 9 }, list.toArray());
    }

    @Test
    public void testStats() {
        ByteList list = ByteList.of((byte) 9, (byte) 2, (byte) 7, (byte) 5, (byte) 1);
        assertEquals(OptionalByte.of((byte) 1), list.min());
        assertEquals(OptionalByte.of((byte) 9), list.max());
        assertEquals(OptionalByte.of((byte) 5), list.lowerMedian());

        ByteList emptyList = new ByteList();
        assertEquals(OptionalByte.empty(), emptyList.min());
        assertEquals(OptionalByte.empty(), emptyList.max());
        assertEquals(OptionalByte.empty(), emptyList.lowerMedian());
    }

    @Test
    public void testStatic() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, list1.size());
        assertEquals((byte) 1, list1.get(0));
        assertEquals((byte) 2, list1.get(1));
        assertEquals((byte) 3, list1.get(2));

        ByteList list2 = ByteList.of((byte[]) null);
        assertEquals(0, list2.size());

        byte[] arr = { 1, 2, 3, 4 };
        ByteList list3 = ByteList.of(arr, 2);
        assertEquals(2, list3.size());
        assertEquals((byte) 1, list3.get(0));
        assertEquals((byte) 2, list3.get(1));

        ByteList list4 = ByteList.copyOf(arr);
        assertEquals(4, list4.size());
        arr[0] = 10;
        assertEquals((byte) 1, list4.get(0));

        ByteList list5 = ByteList.copyOf(arr, 1, 3);
        assertEquals(2, list5.size());
        assertEquals((byte) 2, list5.get(0));
        assertEquals((byte) 3, list5.get(1));

        ByteList list6 = ByteList.repeat((byte) 5, 4);
        assertEquals(4, list6.size());
        for (int i = 0; i < 4; i++) {
            assertEquals((byte) 5, list6.get(i));
        }

        ByteList list7 = ByteList.random(10);
        assertEquals(10, list7.size());
        for (int i = 0; i < 10; i++) {
            byte value = list7.get(i);
            assertTrue(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE);
        }
    }

    @Test
    public void testCopyOf() {
        {
            list = new ByteList();
            byte[] original = { 1, 2, 3 };
            ByteList list = ByteList.copyOf(original);
            assertEquals(3, list.size());
            assertArrayEquals(original, list.toArray());
            original[0] = 10;
            assertEquals((byte) 1, list.get(0));
        }
        {
            list = new ByteList();
            byte[] arr = { 1, 2, 3, 4, 5 };
            ByteList list = ByteList.copyOf(arr, 1, 4);
            assertEquals(3, list.size());
            assertEquals(2, list.get(0));
            assertEquals(3, list.get(1));
            assertEquals(4, list.get(2));
        }
    }

    @Test
    public void testCopyOf_Null() {
        ByteList list = ByteList.copyOf(null);
        assertEquals(0, list.size());
    }

    @Test
    public void testRange() {
        {
            list = new ByteList();
            assertArrayEquals(new byte[] { 5, 6, 7 }, ByteList.range((byte) 5, (byte) 8).toArray());
            assertArrayEquals(new byte[] { 5, 6, 7, 8 }, ByteList.rangeClosed((byte) 5, (byte) 8).toArray());
            assertArrayEquals(new byte[] { 0, 3, 6 }, ByteList.rangeClosed((byte) 0, (byte) 8, (byte) 3).toArray());
            assertArrayEquals(new byte[] { 7, 7, 7 }, ByteList.repeat((byte) 7, 3).toArray());
            assertEquals(10, ByteList.random(10).size());
        }
        {
            list = new ByteList();
            ByteList rangeList = ByteList.range((byte) 1, (byte) 5);
            assertArrayEquals(new byte[] { 1, 2, 3, 4 }, rangeList.toArray());

            ByteList rangeClosedList = ByteList.rangeClosed((byte) 1, (byte) 5);
            assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, rangeClosedList.toArray());

            ByteList rangeStepList = ByteList.range((byte) 0, (byte) 10, (byte) 2);
            assertArrayEquals(new byte[] { 0, 2, 4, 6, 8 }, rangeStepList.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.rangeClosed((byte) 0, (byte) 10, (byte) 2);
            assertEquals(6, list.size());
            assertArrayEquals(new byte[] { 0, 2, 4, 6, 8, 10 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.range((byte) 120, (byte) 127);
            assertEquals(7, list1.size());
            assertEquals((byte) 120, list1.get(0));
            assertEquals((byte) 126, list1.get(6));

            ByteList list2 = ByteList.range((byte) -5, (byte) 5);
            assertEquals(10, list2.size());
            assertEquals((byte) -5, list2.get(0));
            assertEquals((byte) 4, list2.get(9));

            ByteList list3 = ByteList.range((byte) -128, (byte) 127, (byte) 25);
            assertTrue(list3.size() > 0);
            assertEquals((byte) -128, list3.get(0));

            ByteList list4 = ByteList.rangeClosed(Byte.MIN_VALUE, Byte.MIN_VALUE);
            assertEquals(1, list4.size());
            assertEquals(Byte.MIN_VALUE, list4.get(0));

            ByteList list5 = ByteList.rangeClosed(Byte.MAX_VALUE, Byte.MAX_VALUE);
            assertEquals(1, list5.size());
            assertEquals(Byte.MAX_VALUE, list5.get(0));
        }
    }

    @Test
    public void testRange_NegativeStep() {
        {
            list = new ByteList();
            ByteList list = ByteList.range((byte) 10, (byte) 0, (byte) -2);
            assertEquals(5, list.size());
            assertArrayEquals(new byte[] { 10, 8, 6, 4, 2 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.range((byte) 10, (byte) 0, (byte) -1);
            assertEquals(10, list1.size());
            assertEquals((byte) 10, list1.get(0));
            assertEquals((byte) 1, list1.get(9));

            ByteList list2 = ByteList.rangeClosed((byte) 10, (byte) 0, (byte) -2);
            assertEquals(6, list2.size());
            assertEquals((byte) 10, list2.get(0));
            assertEquals((byte) 8, list2.get(1));
            assertEquals((byte) 0, list2.get(5));
        }
    }

    @Test
    public void testRepeat() {
        {
            list = new ByteList();
            ByteList repeatedList = ByteList.repeat((byte) 7, 4);
            assertArrayEquals(new byte[] { 7, 7, 7, 7 }, repeatedList.toArray());

            ByteList randomList = ByteList.random(10);
            assertEquals(10, randomList.size());
            assertNotNull(randomList);
        }
        {
            list = new ByteList();
            ByteList list = ByteList.repeat((byte) 5, 3);
            assertEquals(3, list.size());
            assertArrayEquals(new byte[] { 5, 5, 5 }, list.toArray());
        }
    }

    @Test
    public void testRandom() {
        ByteList list = ByteList.random(10);
        assertEquals(10, list.size());
        for (int i = 0; i < list.size(); i++) {
            byte val = list.get(i);
            assertTrue(val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE);
        }
    }

    @Test
    public void testGetSet() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 2);
            byte[] arr = { 2, 3, 3 };

            ByteList intersection = list.intersection(arr);
            assertArrayEquals(new byte[] { 2 }, intersection.toArray());

            ByteList difference = list.difference(arr);
            assertArrayEquals(new byte[] { 1, 2 }, difference.toArray());

            ByteList symmetricDifference = list.symmetricDifference(arr);
            symmetricDifference.sort();
            assertArrayEquals(new byte[] { 1, 2, 3, 3 }, symmetricDifference.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            byte oldValue = list.set(1, (byte) 25);
            assertEquals(20, oldValue);
            assertEquals(25, list.get(1));
        }
    }

    @Test
    public void testGetSet_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, (byte) 0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, (byte) 0));
    }

    @Test
    public void testFast() {
        // fastRemove is private; tested indirectly via remove(byte)
        ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40);
        boolean changed = bl.remove((byte) 20);
        assertTrue(changed);
        assertEquals(3, bl.size());
        assertEquals((byte) 10, bl.get(0));
        assertEquals((byte) 30, bl.get(1));
        assertEquals((byte) 40, bl.get(2));
    }

    @Test
    public void testRetainAll() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            ByteList toRetain = ByteList.of((byte) 2, (byte) 4, (byte) 6);
            assertTrue(list.retainAll(toRetain));
            assertEquals(2, list.size());
            assertArrayEquals(new byte[] { 2, 4 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            byte[] toRetain = { 2, 4, 6 };
            assertTrue(list.retainAll(toRetain));
            assertEquals(2, list.size());
            assertArrayEquals(new byte[] { 2, 4 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            bl.retainAll(new byte[] { (byte) 1, (byte) 3 });
            assertEquals(2, bl.size());
            assertEquals((byte) 1, bl.get(0));
            assertEquals((byte) 3, bl.get(1));
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            bl.retainAll(ByteList.of((byte) 2, (byte) 4));
            assertEquals(2, bl.size());
            assertEquals((byte) 2, bl.get(0));
            assertEquals((byte) 4, bl.get(1));
        }
    }

    @Test
    public void testBatch() {
        {
            list = new ByteList();
            // batchRemove(c, false) with small collection (c.size() <= 3 or size() <= 9)
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            boolean changed = bl.removeAll(ByteList.of((byte) 2, (byte) 4));
            assertTrue(changed);
            assertEquals(3, bl.size());
            assertFalse(bl.contains((byte) 2));
            assertFalse(bl.contains((byte) 4));
            assertTrue(bl.contains((byte) 1));
        }
        {
            list = new ByteList();
            // batchRemove(c, true) with large collection (c.size() > 3 and size() > 9)
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10);
            ByteList toRetain = ByteList.of((byte) 2, (byte) 4, (byte) 6, (byte) 8);
            boolean changed = bl.retainAll(toRetain);
            assertTrue(changed);
            assertEquals(4, bl.size());
            assertTrue(bl.contains((byte) 2));
            assertFalse(bl.contains((byte) 1));
        }
    }

    @Test
    public void testBatch_LargeData() {
        // triggers the Set path (c.size() > 3 && size() > 9)
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10);
        ByteList retain = ByteList.of((byte) 1, (byte) 3, (byte) 5, (byte) 7);
        bl.retainAll(retain);
        assertEquals(4, bl.size());
        assertEquals((byte) 1, bl.get(0));
        assertEquals((byte) 3, bl.get(1));
    }

    @Test
    public void testMoveRange() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            list.add((byte) 4);
            list.add((byte) 5);

            list.moveRange(1, 3, 0);
            assertEquals(5, list.size());
            assertEquals((byte) 2, list.get(0));
            assertEquals((byte) 3, list.get(1));
            assertEquals((byte) 1, list.get(2));
            assertEquals((byte) 4, list.get(3));
            assertEquals((byte) 5, list.get(4));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            list.moveRange(1, 3, 2);
            assertNotNull(list);
        }
    }

    @Test
    public void testReplaceRange() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            list.add((byte) 4);

            ByteList replacement = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            list.replaceRange(1, 3, replacement);
            assertEquals(5, list.size());
            assertEquals((byte) 1, list.get(0));
            assertEquals((byte) 10, list.get(1));
            assertEquals((byte) 20, list.get(2));
            assertEquals((byte) 30, list.get(3));
            assertEquals((byte) 4, list.get(4));

            list.clear();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            list.replaceRange(1, 2, new byte[] { 100, 101 });
            assertEquals(4, list.size());
            assertEquals((byte) 1, list.get(0));
            assertEquals((byte) 100, list.get(1));
            assertEquals((byte) 101, list.get(2));
            assertEquals((byte) 3, list.get(3));

            list.replaceRange(1, 3, new byte[0]);
            assertEquals(2, list.size());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            ByteList replacement = ByteList.of((byte) 10, (byte) 11);
            list.replaceRange(1, 4, replacement);
            assertEquals(4, list.size());
            assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            byte[] replacement = { 10, 11 };
            list.replaceRange(1, 4, replacement);
            assertEquals(4, list.size());
            assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3, 4, 5 });
            list.replaceRange(1, 4, ByteList.of((byte) 10, (byte) 11));
            assertEquals(4, list.size());
            assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
        }
    }

    @Test
    public void testReplaceAll() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            list.replaceAll(b -> (byte) (b * 2));
            assertArrayEquals(new byte[] { 2, 4, 6 }, list.toArray());
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3 });
            list.replaceAll(b -> (byte) (b * 2));
            assertArrayEquals(new byte[] { 2, 4, 6 }, list.toArray());
        }
    }

    @Test
    public void testReplaceAll_Null() {
        ByteList nonEmpty = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceAll((com.landawn.abacus.util.function.ByteUnaryOperator) null));

        ByteList empty = new ByteList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceAll((com.landawn.abacus.util.function.ByteUnaryOperator) null));
    }

    @Test
    public void testReplaceIf() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
            assertTrue(list.replaceIf(b -> b < 0, (byte) 0));
            assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, -2, 3, -4, 5 });
            assertTrue(list.replaceIf(b -> b < 0, (byte) 0));
            assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, list.toArray());
        }
    }

    @Test
    public void testFill() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            list.fill(1, 4, (byte) 9);
            assertArrayEquals(new byte[] { 1, 9, 9, 9, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3, 4, 5 });
            list.fill(1, 4, (byte) 9);
            assertArrayEquals(new byte[] { 1, 9, 9, 9, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            list.fill((byte) 9);
            assertArrayEquals(new byte[] { 9, 9, 9 }, list.toArray());
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            list.fill((byte) 5);
            assertEquals(3, list.size());
            for (int i = 0; i < 3; i++) {
                assertEquals((byte) 5, list.get(i));
            }

            list.fill(1, 3, (byte) 10);
            assertEquals((byte) 5, list.get(0));
            assertEquals((byte) 10, list.get(1));
            assertEquals((byte) 10, list.get(2));
        }
    }

    @Test
    public void testContains() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            assertTrue(list.contains((byte) 10));
            assertTrue(list.containsAny(new byte[] { 5, 15, 20 }));
            assertFalse(list.containsAny(new byte[] { 5, 15, 25 }));
            assertTrue(list.containsAll(ByteList.of((byte) 10, (byte) 30)));
            assertFalse(list.containsAll(ByteList.of((byte) 10, (byte) 40)));

            assertTrue(list.disjoint(ByteList.of((byte) 1, (byte) 2, (byte) 3)));
            assertFalse(list.disjoint(new byte[] { 15, 25, 30 }));
        }
        {
            list = new ByteList();
            list.add((byte) 10);
            list.add((byte) 20);

            assertTrue(list.contains((byte) 10));
            assertTrue(list.contains((byte) 20));
            assertFalse(list.contains((byte) 30));

            list.clear();
            assertFalse(list.contains((byte) 10));
        }
    }

    @Test
    public void testContainsAny() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            assertTrue(list.containsAny(ByteList.of((byte) 1)));
            assertTrue(list.containsAny(ByteList.of((byte) 4, (byte) 3)));
            assertFalse(list.containsAny(ByteList.of((byte) 4, (byte) 5)));
            assertFalse(list.containsAny(new ByteList()));

            assertTrue(list.containsAny(new byte[] { 1 }));
            assertTrue(list.containsAny(new byte[] { 4, 2 }));
            assertFalse(list.containsAny(new byte[] { 4, 5 }));

            assertTrue(list.containsAll(ByteList.of((byte) 1, (byte) 2)));
            assertTrue(list.containsAll(ByteList.of((byte) 3)));
            assertFalse(list.containsAll(ByteList.of((byte) 1, (byte) 4)));
            assertTrue(list.containsAll(new ByteList()));

            assertTrue(list.containsAll(new byte[] { 1, 2 }));
            assertFalse(list.containsAll(new byte[] { 1, 4 }));
            assertTrue(list.containsAll(new byte[0]));

            list.clear();
            assertFalse(list.containsAny(ByteList.of((byte) 1)));
            assertFalse(list.containsAll(ByteList.of((byte) 1)));
            assertTrue(list.containsAll(new ByteList()));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertTrue(list.containsAny(ByteList.of((byte) 3, (byte) 4)));
            assertFalse(list.containsAny(ByteList.of((byte) 4, (byte) 5)));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertTrue(list.containsAny(new byte[] { 3, 4 }));
            assertFalse(list.containsAny(new byte[] { 4, 5 }));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList other = ByteList.of((byte) 3, (byte) 4, (byte) 5);
            assertTrue(list.containsAny(other));
        }
    }

    @Test
    public void testContainsAll() {
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            assertTrue(bl.containsAll(ByteList.of((byte) 1, (byte) 3)));
            assertFalse(bl.containsAll(ByteList.of((byte) 1, (byte) 5)));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertTrue(list.containsAll(new byte[] { 1, 2 }));
            assertFalse(list.containsAll(new byte[] { 1, 4 }));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            ByteList other = ByteList.of((byte) 2, (byte) 3);
            assertTrue(list.containsAll(other));
        }
    }

    @Test
    public void testDisjoint() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);

            assertFalse(list.disjoint(ByteList.of((byte) 1)));
            assertFalse(list.disjoint(ByteList.of((byte) 2, (byte) 3)));
            assertTrue(list.disjoint(ByteList.of((byte) 3, (byte) 4)));
            assertTrue(list.disjoint(new ByteList()));

            assertFalse(list.disjoint(new byte[] { 1 }));
            assertTrue(list.disjoint(new byte[] { 3, 4 }));
            assertTrue(list.disjoint(new byte[0]));

            list.clear();
            assertTrue(list.disjoint(ByteList.of((byte) 1)));
            assertTrue(list.disjoint(new byte[] { 2 }));
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList list2 = ByteList.of((byte) 4, (byte) 5);
            assertTrue(list1.disjoint(list2));

            ByteList list3 = ByteList.of((byte) 3, (byte) 4);
            assertFalse(list1.disjoint(list3));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            byte[] arr1 = { 4, 5 };
            assertTrue(list.disjoint(arr1));

            byte[] arr2 = { 3, 4 };
            assertFalse(list.disjoint(arr2));
        }
    }

    @Test
    public void testIntersection() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            list.add((byte) 2);

            ByteList other = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 2);
            ByteList result = list.intersection(other);
            assertEquals(3, result.size());
            assertEquals((byte) 2, result.get(0));
            assertEquals((byte) 3, result.get(1));
            assertEquals((byte) 2, result.get(2));

            result = list.intersection(new byte[] { 1, 1, 4 });
            assertEquals(1, result.size());
            assertEquals((byte) 1, result.get(0));

            result = list.intersection(new ByteList());
            assertEquals(0, result.size());
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            ByteList list2 = ByteList.of((byte) 3, (byte) 4, (byte) 5);
            ByteList result = list1.intersection(list2);
            assertEquals(2, result.size());
            assertTrue(result.contains((byte) 3));
            assertTrue(result.contains((byte) 4));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            byte[] arr = { 3, 4, 5 };
            ByteList result = list.intersection(arr);
            assertEquals(2, result.size());
            assertTrue(result.contains((byte) 3));
            assertTrue(result.contains((byte) 4));
        }
    }

    @Test
    public void testDifference() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            list.add((byte) 2);

            ByteList other = ByteList.of((byte) 2, (byte) 2);
            ByteList result = list.difference(other);
            assertEquals(2, result.size());
            assertEquals((byte) 1, result.get(0));
            assertEquals((byte) 3, result.get(1));

            result = list.difference(new byte[] { 1, 3 });
            assertEquals(2, result.size());
            assertEquals((byte) 2, result.get(0));
            assertEquals((byte) 2, result.get(1));

            result = list.difference(new ByteList());
            assertEquals(4, result.size());
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            ByteList list2 = ByteList.of((byte) 3, (byte) 4, (byte) 5);
            ByteList result = list1.difference(list2);
            assertEquals(2, result.size());
            assertTrue(result.contains((byte) 1));
            assertTrue(result.contains((byte) 2));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            byte[] arr = { 3, 4, 5 };
            ByteList result = list.difference(arr);
            assertEquals(2, result.size());
            assertTrue(result.contains((byte) 1));
            assertTrue(result.contains((byte) 2));
        }
    }

    @Test
    public void testDifference_Empty() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList result = bl.difference(ByteList.of());
        assertEquals(3, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        {
            ByteList receiver = ByteList.of((byte) 1, (byte) 9);
            ByteList other = ByteList.of((byte) 1, (byte) 2, (byte) 1);
            ByteList expected = ByteList.of((byte) 9, (byte) 1, (byte) 2);
            assertEquals(expected, receiver.symmetricDifference(other));
            assertEquals(expected, receiver.symmetricDifference(other.toArray()));
            assertEquals(ByteList.of((byte) 1, (byte) 9), receiver);
            assertEquals(ByteList.of((byte) 1, (byte) 2, (byte) 1), other);
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            ByteList other = ByteList.of((byte) 2, (byte) 3, (byte) 4);
            ByteList result = list.symmetricDifference(other);
            assertEquals(2, result.size());
            assertEquals((byte) 1, result.get(0));
            assertEquals((byte) 4, result.get(1));

            result = list.symmetricDifference(new byte[] { 1, 2, 3, 4, 5 });
            assertEquals(2, result.size());
            assertEquals((byte) 4, result.get(0));
            assertEquals((byte) 5, result.get(1));

            result = list.symmetricDifference(new ByteList());
            assertEquals(3, result.size());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList result = bl.symmetricDifference(new byte[] { (byte) 2, (byte) 4 });
            assertTrue(result.contains((byte) 1));
            assertTrue(result.contains((byte) 3));
            assertTrue(result.contains((byte) 4));
            assertFalse(result.contains((byte) 2));
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4);
            ByteList result = list1.symmetricDifference(list2);
            assertTrue(result.contains((byte) 1));
            assertTrue(result.contains((byte) 4));
            assertFalse(result.contains((byte) 2));
        }
    }

    @Test
    public void testFrequency() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 1);
            list.add((byte) 3);
            list.add((byte) 1);

            assertEquals(3, list.frequency((byte) 1));
            assertEquals(1, list.frequency((byte) 2));
            assertEquals(1, list.frequency((byte) 3));
            assertEquals(0, list.frequency((byte) 4));

            list.clear();
            assertEquals(0, list.frequency((byte) 1));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
            assertEquals(3, list.frequency((byte) 1));
            assertEquals(1, list.frequency((byte) 2));
            assertEquals(0, list.frequency((byte) 5));
        }
    }

    @Test
    public void testIndexOf() {
        {
            list = new ByteList();
            list.add((byte) 10);
            list.add((byte) 20);
            list.add((byte) 30);
            list.add((byte) 20);

            assertEquals(0, list.indexOf((byte) 10));
            assertEquals(1, list.indexOf((byte) 20));
            assertEquals(2, list.indexOf((byte) 30));
            assertEquals(-1, list.indexOf((byte) 40));

            assertEquals(3, list.indexOf((byte) 20, 2));
            assertEquals(-1, list.indexOf((byte) 10, 1));
            assertEquals(-1, list.indexOf((byte) 20, 4));

            list.clear();
            assertEquals(-1, list.indexOf((byte) 10));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
            assertEquals(3, list.indexOf((byte) 20, 2));
            assertEquals(-1, list.indexOf((byte) 20, 4));
        }
    }

    @Test
    public void testLastIndexOf() {
        {
            list = new ByteList();
            list.add((byte) 10);
            list.add((byte) 20);
            list.add((byte) 30);
            list.add((byte) 20);

            assertEquals(0, list.lastIndexOf((byte) 10));
            assertEquals(3, list.lastIndexOf((byte) 20));
            assertEquals(2, list.lastIndexOf((byte) 30));
            assertEquals(-1, list.lastIndexOf((byte) 40));

            assertEquals(1, list.lastIndexOf((byte) 20, 2));
            assertEquals(-1, list.lastIndexOf((byte) 10, -1));
            assertEquals(3, list.lastIndexOf((byte) 20, 10));

            list.clear();
            assertEquals(-1, list.lastIndexOf((byte) 10));
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 1);
            assertEquals(3, bl.lastIndexOf((byte) 2, 4));
            assertEquals(1, bl.lastIndexOf((byte) 2, 2));
            assertEquals(-1, bl.lastIndexOf((byte) 9, 4));
        }
    }

    @Test
    public void testMin() {
        {
            list = new ByteList();
            assertFalse(list.min().isPresent());
            assertFalse(list.max().isPresent());
            assertFalse(list.lowerMedian().isPresent());

            list.add((byte) 5);
            assertEquals(OptionalByte.of((byte) 5), list.min());
            assertEquals(OptionalByte.of((byte) 5), list.max());
            assertEquals(OptionalByte.of((byte) 5), list.lowerMedian());

            list.clear();
            list.add((byte) 3);
            list.add((byte) 1);
            list.add((byte) 4);
            list.add((byte) 1);
            list.add((byte) 5);

            assertEquals(OptionalByte.of((byte) 1), list.min());
            assertEquals(OptionalByte.of((byte) 5), list.max());
            assertEquals(OptionalByte.of((byte) 3), list.lowerMedian());

            assertEquals(OptionalByte.of((byte) 1), list.min(1, 4));
            assertEquals(OptionalByte.of((byte) 4), list.max(1, 4));
            assertEquals(OptionalByte.of((byte) 1), list.lowerMedian(1, 4));

            assertFalse(list.min(2, 2).isPresent());
            assertFalse(list.max(2, 2).isPresent());
            assertFalse(list.lowerMedian(2, 2).isPresent());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
            OptionalByte min = list.min(2, 5);
            assertTrue(min.isPresent());
            assertEquals(1, min.getAsByte());
        }
    }

    @Test
    public void testMin_Empty() {
        ByteList list = new ByteList();
        OptionalByte min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMax() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
            OptionalByte max = list.max(0, 3);
            assertTrue(max.isPresent());
            assertEquals(4, max.getAsByte());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
            OptionalByte max = list.max();
            assertTrue(max.isPresent());
            assertEquals(5, max.getAsByte());
        }
    }

    @Test
    public void testMax_Empty() {
        ByteList list = new ByteList();
        OptionalByte max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testMedian() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            OptionalByte median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals((byte) 3, median.getAsByte());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            OptionalByte median = list.lowerMedian(0, 3);
            assertTrue(median.isPresent());
        }
    }

    @Test
    public void testMedian_Empty() {
        ByteList list = new ByteList();
        OptionalByte median = list.lowerMedian();
        assertFalse(median.isPresent());
    }

    @Test
    public void testEach() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            final int[] sum = { 0 };
            list.forEach(1, 4, b -> sum[0] += b);
            assertEquals(9, sum[0]);
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            AtomicInteger sum = new AtomicInteger(0);
            list.forEach(b -> sum.addAndGet(b));
            assertEquals(6, sum.get());
        }
    }

    @Test
    public void testEach_Null() {
        // Empty lists do not evaluate callbacks; non-empty lists fail naturally when invoking them.
        final ByteList empty = new ByteList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.forEach((com.landawn.abacus.util.function.ByteConsumer) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.removeIf((com.landawn.abacus.util.function.BytePredicate) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceIf((com.landawn.abacus.util.function.BytePredicate) null, (byte) 0));

        final ByteList nonEmpty = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.forEach((com.landawn.abacus.util.function.ByteConsumer) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.removeIf((com.landawn.abacus.util.function.BytePredicate) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceIf((com.landawn.abacus.util.function.BytePredicate) null, (byte) 0));
    }

    @Test
    public void testFirst() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 5, (byte) 10, (byte) 15);

            assertEquals((byte) 5, list.getFirst());
            assertEquals((byte) 15, list.getLast());
            assertEquals(OptionalByte.of((byte) 5), list.first());

            assertEquals((byte) 5, list.removeFirst());
            assertEquals((byte) 15, list.removeLast());
            assertArrayEquals(new byte[] { 10 }, list.toArray());

            ByteList emptyList = new ByteList();
            assertThrows(NoSuchElementException.class, emptyList::getFirst);
            assertThrows(NoSuchElementException.class, emptyList::removeLast);
        }
        {
            list = new ByteList();
            OptionalByte first = list.first();
            OptionalByte last = list.last();
            assertFalse(first.isPresent());
            assertFalse(last.isPresent());

            list.add((byte) 10);
            list.add((byte) 20);
            list.add((byte) 30);

            first = list.first();
            last = list.last();
            assertTrue(first.isPresent());
            assertTrue(last.isPresent());
            assertEquals((byte) 10, first.get());
            assertEquals((byte) 30, last.get());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            OptionalByte first = list.first();
            assertTrue(first.isPresent());
            assertEquals(10, first.getAsByte());
        }
    }

    @Test
    public void testFirst_Empty() {
        ByteList list = new ByteList();
        OptionalByte first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        OptionalByte last = list.last();
        assertTrue(last.isPresent());
        assertEquals(30, last.getAsByte());
    }

    @Test
    public void testLast_Empty() {
        ByteList list = new ByteList();
        OptionalByte last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 5, (byte) 1, (byte) 5, (byte) 2, (byte) 1);
            ByteList distinctList = list.distinct();
            assertArrayEquals(new byte[] { 5, 1, 2 }, distinctList.toArray());

            list.reverseSort();
            assertArrayEquals(new byte[] { 5, 5, 2, 1, 1 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1, (byte) 4);
            ByteList result = bl.distinct(1, 5);
            // distinct of [2, 2, 3, 1]
            assertEquals(3, result.size());
            assertTrue(result.contains((byte) 2));
            assertTrue(result.contains((byte) 3));
            assertTrue(result.contains((byte) 1));
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 1);
            list.add((byte) 3);
            list.add((byte) 2);

            ByteList distinct = list.distinct(0, list.size());
            assertEquals(3, distinct.size());
            assertTrue(distinct.contains((byte) 1));
            assertTrue(distinct.contains((byte) 2));
            assertTrue(distinct.contains((byte) 3));

            distinct = list.distinct(1, 4);
            assertEquals(3, distinct.size());
        }
    }

    @Test
    public void testDistinct_Empty() {
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3 });
            ByteList result = list.distinct(1, 1);
            assertTrue(result.isEmpty());
        }
        {
            list = new ByteList();
            ByteList distinct = list.distinct();
            assertTrue(distinct.isEmpty());
        }
    }

    @Test
    public void testContainsDuplicates() {
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 1);
            assertTrue(list1.containsDuplicates());

            ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertFalse(list2.containsDuplicates());

            assertFalse(new ByteList().containsDuplicates());
        }
        {
            list = new ByteList();
            assertFalse(list.containsDuplicates());

            list.add((byte) 1);
            assertFalse(list.containsDuplicates());

            list.add((byte) 2);
            assertFalse(list.containsDuplicates());

            list.add((byte) 1);
            assertTrue(list.containsDuplicates());
        }
    }

    @Test
    public void testIsSorted() {
        {
            list = new ByteList();
            assertTrue(list.isSorted());

            list.add((byte) 1);
            assertTrue(list.isSorted());

            list.add((byte) 2);
            list.add((byte) 3);
            assertTrue(list.isSorted());

            list.add((byte) 1);
            assertFalse(list.isSorted());
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertTrue(list1.isSorted());

            ByteList list2 = ByteList.of((byte) 3, (byte) 1, (byte) 2);
            assertFalse(list2.isSorted());
        }
    }

    @Test
    public void testSort() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 9, (byte) 2, (byte) 7, (byte) 5, (byte) 1);
            list.sort();
            assertArrayEquals(new byte[] { 1, 2, 5, 7, 9 }, list.toArray());
            assertTrue(list.isSorted());
            assertEquals(2, list.binarySearch((byte) 5));
            assertTrue(list.binarySearch((byte) 6) < 0);

            list.parallelSort();
            assertArrayEquals(new byte[] { 1, 2, 5, 7, 9 }, list.toArray());
        }
        {
            list = new ByteList();
            list.add((byte) 3);
            list.add((byte) 1);
            list.add((byte) 4);
            list.add((byte) 1);
            list.add((byte) 5);

            list.sort();
            assertEquals(5, list.size());
            assertEquals((byte) 1, list.get(0));
            assertEquals((byte) 1, list.get(1));
            assertEquals((byte) 3, list.get(2));
            assertEquals((byte) 4, list.get(3));
            assertEquals((byte) 5, list.get(4));
            assertTrue(list.isSorted());

            list.clear();
            list.sort();
            assertEquals(0, list.size());

            list.add((byte) 1);
            list.sort();
            assertEquals(1, list.size());
            assertEquals((byte) 1, list.get(0));
        }
    }

    @Test
    public void testParallelSort() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
            list.parallelSort();
            assertArrayEquals(new byte[] { 1, 1, 3, 4, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            for (int i = 100; i > 0; i--) {
                list.add((byte) (i % 128));
            }

            list.parallelSort();
            assertTrue(list.isSorted());

            for (int i = 1; i < list.size(); i++) {
                assertTrue(list.get(i - 1) <= list.get(i));
            }
        }
    }

    @Test
    public void testReverseSort() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
            list.reverseSort();
            assertArrayEquals(new byte[] { 5, 4, 3, 1, 1 }, list.toArray());
        }
        {
            list = new ByteList();
            list.add((byte) 3);
            list.add((byte) 1);
            list.add((byte) 4);
            list.add((byte) 1);

            list.reverseSort();
            assertEquals(4, list.size());
            assertEquals((byte) 4, list.get(0));
            assertEquals((byte) 3, list.get(1));
            assertEquals((byte) 1, list.get(2));
            assertEquals((byte) 1, list.get(3));
        }
    }

    @Test
    public void testBinarySearch() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 3);
            list.add((byte) 5);
            list.add((byte) 7);
            list.add((byte) 9);

            assertTrue(list.isSorted());

            assertEquals(0, list.binarySearch((byte) 1));
            assertEquals(2, list.binarySearch((byte) 5));
            assertEquals(4, list.binarySearch((byte) 9));
            assertTrue(list.binarySearch((byte) 2) < 0);
            assertTrue(list.binarySearch((byte) 10) < 0);

            assertEquals(1, list.binarySearch(1, 4, (byte) 3));
            assertEquals(3, list.binarySearch(2, 5, (byte) 7));
            assertTrue(list.binarySearch(0, 3, (byte) 7) < 0);
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            assertEquals(3, list.binarySearch(1, 5, (byte) 4));
            assertTrue(list.binarySearch(1, 3, (byte) 4) < 0);
        }
    }

    @Test
    public void testReverse() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            list.reverse(1, 4);
            assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, list.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            list.reverse();
            assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, list.toArray());
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            list.add((byte) 4);

            list.reverse();
            assertEquals(4, list.size());
            assertEquals((byte) 4, list.get(0));
            assertEquals((byte) 3, list.get(1));
            assertEquals((byte) 2, list.get(2));
            assertEquals((byte) 1, list.get(3));

            list.reverse(1, 3);
            assertEquals((byte) 4, list.get(0));
            assertEquals((byte) 2, list.get(1));
            assertEquals((byte) 3, list.get(2));
            assertEquals((byte) 1, list.get(3));
        }
    }

    @Test
    public void testRotate() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            list.rotate(2);
            assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, list.toArray());
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);
            list.add((byte) 4);

            list.rotate(1);
            assertEquals((byte) 4, list.get(0));
            assertEquals((byte) 1, list.get(1));
            assertEquals((byte) 2, list.get(2));
            assertEquals((byte) 3, list.get(3));

            list.rotate(-2);
            assertEquals((byte) 2, list.get(0));
            assertEquals((byte) 3, list.get(1));
            assertEquals((byte) 4, list.get(2));
            assertEquals((byte) 1, list.get(3));
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3, 4, 5 });
            list.rotate(-2);
            assertEquals((byte) 3, list.get(0));
        }
    }

    @Test
    public void testShuffle() {
        {
            list = new ByteList();
            for (int i = 0; i < 20; i++) {
                list.add((byte) i);
            }

            ByteList original = list.copy();
            list.shuffle();
            assertEquals(original.size(), list.size());

            for (int i = 0; i < original.size(); i++) {
                assertTrue(list.contains(original.get(i)));
            }

            list.shuffle(new Random(42));
            assertEquals(original.size(), list.size());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            ByteList copy = list.copy();
            list.shuffle();
            assertEquals(5, list.size());
            assertTrue(list.contains((byte) 1));
            assertTrue(list.contains((byte) 2));
            assertTrue(list.contains((byte) 3));
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            Random rnd = new Random(123);
            bl.shuffle(rnd);
            assertEquals(5, bl.size());
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3, 4, 5 });
            list.shuffle(new Random(42));
            assertEquals(5, list.size());
        }
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new ByteList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> ByteList.of((byte) 1).shuffle(null));
    }

    @Test
    public void testSwap() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            list.swap(0, 2);
            assertArrayEquals(new byte[] { 3, 2, 1 }, list.toArray());
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            list.swap(0, 2);
            assertEquals((byte) 3, list.get(0));
            assertEquals((byte) 2, list.get(1));
            assertEquals((byte) 1, list.get(2));

            list.swap(0, 1);
            assertEquals((byte) 2, list.get(0));
            assertEquals((byte) 3, list.get(1));

            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
        }
    }

    @Test
    public void testSwap_OutOfBounds() {
        list.addAll(new byte[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopy() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            ByteList copy = list.copy();
            assertEquals(list.size(), copy.size());
            for (int i = 0; i < list.size(); i++) {
                assertEquals(list.get(i), copy.get(i));
            }

            copy.set(0, (byte) 10);
            assertEquals((byte) 1, list.get(0));
            assertEquals((byte) 10, copy.get(0));

            ByteList partialCopy = list.copy(1, 3);
            assertEquals(2, partialCopy.size());
            assertEquals((byte) 2, partialCopy.get(0));
            assertEquals((byte) 3, partialCopy.get(1));

            list.clear();
            for (int i = 0; i < 10; i++) {
                list.add((byte) i);
            }
            ByteList steppedCopy = list.copy(0, 10, 2);
            assertEquals(5, steppedCopy.size());
            assertEquals((byte) 0, steppedCopy.get(0));
            assertEquals((byte) 2, steppedCopy.get(1));
            assertEquals((byte) 4, steppedCopy.get(2));
            assertEquals((byte) 6, steppedCopy.get(3));
            assertEquals((byte) 8, steppedCopy.get(4));
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
            ByteList subCopy = list.copy(1, 4);
            assertArrayEquals(new byte[] { 2, 3, 4 }, subCopy.toArray());

            ByteList stepCopy = list.copy(0, 6, 2);
            assertArrayEquals(new byte[] { 1, 3, 5 }, stepCopy.toArray());

            List<ByteList> chunks = list.split(3);
            assertEquals(2, chunks.size());
            assertArrayEquals(new byte[] { 1, 2, 3 }, chunks.get(0).toArray());
            assertArrayEquals(new byte[] { 4, 5, 6 }, chunks.get(1).toArray());
        }
        {
            list = new ByteList();
            ByteList list = new ByteList(20);
            list.addAll(new byte[] { 1, 2, 3 });
            assertEquals(20, list.internalArray().length);
            list.trimToSize();
            assertEquals(3, list.internalArray().length);

            ByteList copy = list.copy();
            assertNotSame(list, copy);
            assertEquals(list, copy);
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
            ByteList copy = list.copy(0, 6, 2);
            assertEquals(3, copy.size());
            assertArrayEquals(new byte[] { 1, 3, 5 }, copy.toArray());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            ByteList copy = list.copy(1, 4);
            assertEquals(3, copy.size());
            assertArrayEquals(new byte[] { 2, 3, 4 }, copy.toArray());
        }
    }

    @Test
    public void testCopy_Empty() {
        ByteList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testSplit() {
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
            List<ByteList> chunks = list.split(2);
            assertEquals(2, chunks.size());
            assertArrayEquals(new byte[] { 1, 2 }, chunks.get(0).toArray());
            assertArrayEquals(new byte[] { 3, 4 }, chunks.get(1).toArray());
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
            List<ByteList> chunks = bl.split(0, 6, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals((byte) 1, chunks.get(0).get(0));
            assertEquals((byte) 3, chunks.get(1).get(0));
        }
        {
            list = new ByteList();
            for (int i = 0; i < 7; i++) {
                list.add((byte) i);
            }

            List<ByteList> splits = list.split(0, list.size(), 3);
            assertEquals(3, splits.size());
            assertEquals(3, splits.get(0).size());
            assertEquals(3, splits.get(1).size());
            assertEquals(1, splits.get(2).size());

            assertEquals((byte) 0, splits.get(0).get(0));
            assertEquals((byte) 1, splits.get(0).get(1));
            assertEquals((byte) 2, splits.get(0).get(2));
        }
    }

    @Test
    public void testSplit_Empty() {
        {
            list = new ByteList();
            List<ByteList> chunks = list.split(0, 0, 2);
            assertTrue(chunks.isEmpty());
        }
        {
            list = new ByteList();
            List<ByteList> chunks = list.split(3);
            assertTrue(chunks.isEmpty());
        }
    }

    @Test
    public void testTrim() {
        {
            list = new ByteList();
            ByteList list = new ByteList(100);
            list.add((byte) 1);
            list.add((byte) 2);
            ByteList result = list.trimToSize();
            assertNotNull(result);
            assertEquals(2, result.size());
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);

            ByteList trimmed = list.trimToSize();
            assertSame(list, trimmed);
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testClear() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            list.clear();
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            list.clear();
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testClear_Empty() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        bl.clear();
        assertEquals(0, bl.size());
        assertTrue(bl.isEmpty());
    }

    @Test
    public void testIsEmpty_Empty() {
        {
            list = new ByteList();
            assertTrue(list.isEmpty());

            list.add((byte) 1);
            assertFalse(list.isEmpty());

            list.clear();
            assertTrue(list.isEmpty());
        }
        {
            list = new ByteList();
            ByteList list = new ByteList();
            assertTrue(list.isEmpty());
            list.add((byte) 1);
            assertFalse(list.isEmpty());
        }
    }

    @Test
    public void testSize() {
        {
            list = new ByteList();
            assertEquals(0, list.size());

            list.add((byte) 1);
            assertEquals(1, list.size());

            list.add((byte) 2);
            assertEquals(2, list.size());

            list.remove((byte) 1);
            assertEquals(1, list.size());
        }
        {
            list = new ByteList();
            ByteList list = new ByteList();
            assertEquals(0, list.size());
            list.add((byte) 1);
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testBoxed() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            List<Byte> boxed = list.boxed();
            assertEquals(3, boxed.size());
            assertEquals(Byte.valueOf((byte) 1), boxed.get(0));
            assertEquals(Byte.valueOf((byte) 2), boxed.get(1));
            assertEquals(Byte.valueOf((byte) 3), boxed.get(2));

            List<Byte> partialBoxed = list.boxed(1, 3);
            assertEquals(2, partialBoxed.size());
            assertEquals(Byte.valueOf((byte) 2), partialBoxed.get(0));
            assertEquals(Byte.valueOf((byte) 3), partialBoxed.get(1));
        }
        {
            list = new ByteList();
            ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40);
            List<Byte> boxed = bl.boxed(1, 3);
            assertEquals(2, boxed.size());
            assertEquals(Byte.valueOf((byte) 20), boxed.get(0));
            assertEquals(Byte.valueOf((byte) 30), boxed.get(1));
        }
        {
            list = new ByteList();
            list.addAll(new byte[] { 1, 2, 3, 4, 5 });
            List<Byte> boxed = list.boxed(1, 4);
            assertEquals(3, boxed.size());
            assertEquals(Byte.valueOf((byte) 2), boxed.get(0));
        }
    }

    @Test
    public void testBoxed_Empty() {
        assertTrue(list.boxed().isEmpty());
    }

    @Test
    public void testIterator() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
        assertEquals(2, iter.nextByte());
        assertEquals(3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Empty() {
        ByteIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);
            list.add((byte) 3);

            byte[] streamResult = list.stream().toArray();
            assertEquals(3, streamResult.length);
            assertEquals((byte) 1, streamResult[0]);
            assertEquals((byte) 2, streamResult[1]);
            assertEquals((byte) 3, streamResult[2]);

            byte[] partialStreamResult = list.stream(1, 3).toArray();
            assertEquals(2, partialStreamResult.length);
            assertEquals((byte) 2, partialStreamResult[0]);
            assertEquals((byte) 3, partialStreamResult[1]);
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
            ByteStream stream = list.stream(1, 4);
            assertNotNull(stream);
            assertEquals(9, stream.sum());
        }
    }

    @Test
    public void testStream_Empty() {
        assertEquals(0, list.stream().count());
    }

    @Test
    public void testGetFirst() {
        {
            list = new ByteList();
            assertThrows(NoSuchElementException.class, () -> list.getFirst());
            assertThrows(NoSuchElementException.class, () -> list.getLast());

            list.add((byte) 10);
            list.add((byte) 20);
            list.add((byte) 30);

            assertEquals((byte) 10, list.getFirst());
            assertEquals((byte) 30, list.getLast());
        }
        {
            list = new ByteList();
            ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
            assertEquals(10, list.getFirst());
        }
    }

    @Test
    public void testGetFirst_Empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(30, list.getLast());
    }

    @Test
    public void testGetLast_Empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testHashCode() {
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);

            ByteList other = new ByteList();
            other.add((byte) 1);
            other.add((byte) 2);

            assertEquals(list.hashCode(), other.hashCode());

            other.add((byte) 3);
            assertNotEquals(list.hashCode(), other.hashCode());
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertEquals(list1.hashCode(), list2.hashCode());
        }
    }

    @Test
    public void testEquals() {
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList list3 = ByteList.of((byte) 1, (byte) 2, (byte) 4);

            assertTrue(list1.equals(list2));
            assertFalse(list1.equals(list3));
            assertFalse(list1.equals(null));
        }
        {
            list = new ByteList();
            ByteList bl1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            ByteList bl2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertTrue(bl1.equals(bl2));
        }
        {
            list = new ByteList();
            list.add((byte) 1);
            list.add((byte) 2);

            assertEquals(list, list);

            ByteList other = new ByteList();
            other.add((byte) 1);
            other.add((byte) 2);
            assertEquals(list, other);

            other.add((byte) 3);
            assertNotEquals(list, other);

            ByteList different = new ByteList();
            different.add((byte) 2);
            different.add((byte) 1);
            assertNotEquals(list, different);

            assertNotEquals(list, null);

            assertNotEquals(list, "not a list");
        }
        {
            list = new ByteList();
            ByteList list1 = ByteList.of((byte) 1, (byte) 2);
            ByteList list2 = ByteList.of((byte) 1, (byte) 2);
            ByteList list3 = ByteList.of((byte) 2, (byte) 1);

            assertEquals(list1, list2);
            assertNotEquals(list1, list3);
            assertNotEquals(null, list1);
            assertNotEquals(list1, new Object());
            assertEquals(list1.hashCode(), list2.hashCode());
        }
    }

    @Test
    public void testEquals_Null() {
        ByteList bl = ByteList.of((byte) 1);
        assertFalse(bl.equals(null));
    }

    @Test
    public void testConversionSuppliersMustProduceCollectionsForEmptyRanges() {
        assertThrows(IllegalArgumentException.class, () -> list.toCollection(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> list.toCollection(0, 0, ignored -> null));
        assertThrows(IllegalArgumentException.class, () -> list.toMultiset(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> list.toMultiset(0, 0, ignored -> null));
    }

    @Test
    public void reviewFixes20260906_descendingCopyClampsAgainstSizeNotTheBackingArray() {
        // ByteList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final ByteList withSpareCapacity = ByteList.of(new byte[] { 1, 2, 3, 4, 5 }, 3);

        assertEquals(3, withSpareCapacity.size());
        assertEquals(5, withSpareCapacity.internalArray().length, "the test needs real spare capacity");

        assertEquals("[3, 2, 1]", withSpareCapacity.copy(3, -1, -1).toString(), "spare capacity must not leak into the result");
        assertEquals("[3, 2, 1]", withSpareCapacity.copy(2, -1, -1).toString(), "an in-range start is unaffected");

        // Ascending copies, and a copy over the whole logical range, are unchanged.
        assertEquals(withSpareCapacity.toString(), withSpareCapacity.copy(0, 3, 1).toString());
        assertEquals(3, withSpareCapacity.copy(0, 3, 1).size());

        // The source is not modified by any of this.
        assertEquals(3, withSpareCapacity.size());
    }

    @Test
    public void reviewFixes20260906_addAllAtIndexSurvivesSelfAliasing() {
        // Passing the list to itself makes source and destination the same array, across a reallocation by
        // ensureCapacity. It is correct because the tail shift runs BEFORE the source copy and the two regions
        // provably never overlap (the source is [0, numNew) and the shift writes at index + numNew or later).
        // Swap those two statements and the result is wrong, so this pins the ordering, at every index.
        for (int index = 0; index <= 3; index++) {
            final ByteList self = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final ByteList expected = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            final ByteList inserted = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            expected.addAll(index, ByteList.of((byte) 1, (byte) 2, (byte) 3));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final ByteList appended = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        appended.addAll(appended);
        assertEquals("[1, 2, 3, 1, 2, 3]", appended.toString());

        // The interesting middle case, spelled out.
        final ByteList middle = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        middle.addAll(1, middle);
        assertEquals("[1, 1, 2, 3, 2, 3]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final ByteList a = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(a.removeAll((ByteList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((byte[]) null));
        assertFalse(a.removeAll(new byte[0]));
        assertFalse(a.removeAll(new ByteList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final ByteList b = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(b.retainAll((ByteList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final ByteList c = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(c.retainAll((byte[]) null));
        assertEquals(0, c.size());

        final ByteList d = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(d.retainAll(new byte[0]));
        assertEquals(0, d.size());

        final ByteList e = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(e.retainAll(new ByteList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final ByteList empty = new ByteList();
        assertFalse(empty.retainAll((ByteList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final ByteList f = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(f.retainAll(ByteList.of((byte) 1)));
        assertEquals(1, f.size());
    }

    // Doc pin for the javadoc corrected on 2026-09-11: forEach(fromIndex, toIndex, action) validates the RANGE
    // [min, max), never the individual start index, so a backward fromIndex == size() is accepted and clamped by
    // `N.min(size - 1, fromIndex)` to the last LOGICAL element. That clamp is deliberate - it is what keeps the
    // spare capacity of a ByteList.of(array, size) out of the traversal - but the javadoc used to promise a plain
    // IndexOutOfBoundsException "if the indices are out of range" and never mentioned it.
    @Test
    public void reviewFixes20260911_rangedForEachAcceptsSizeAsABackwardStartAndClampsIt() {
        final ByteList six = ByteList.of((byte) 0, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(6, six.size());

        assertEquals("543", visitBackward(six, 6, 2), "fromIndex == size() is accepted for a backward traversal");
        assertEquals("543", visitBackward(six, 5, 2), "and produces exactly what size() - 1 produces");
        assertEquals("543210", visitBackward(six, 6, -1), "forEach(size(), -1) walks the whole list backwards");

        // size() + 1 is the first start that actually throws - that discontinuity is what the javadoc now states.
        assertThrows(IndexOutOfBoundsException.class, () -> six.forEach(7, 2, b -> {
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> six.forEach(-1, 3, b -> {
        }));
        assertThrows(IndexOutOfBoundsException.class, () -> six.forEach(0, 7, b -> {
        }));

        // The clamp is against size(), not against the backing array, so phantom slots are never visited.
        final ByteList withSpareCapacity = ByteList.of(new byte[] { 1, 2, 3, 66, 77, 88 }, 3);
        assertEquals(6, withSpareCapacity.internalArray().length, "the test needs real spare capacity");
        assertEquals("321", visitBackward(withSpareCapacity, 3, -1), "spare capacity must not leak into the traversal");
        assertEquals("321", visitBackward(withSpareCapacity, 2, -1));

        // copy(from, to, step) carries the identical clamp, and its javadoc now says so too.
        assertEquals("[3, 2, 1]", withSpareCapacity.copy(3, -1, -1).toString());
    }

    private static String visitBackward(final ByteList list, final int fromIndex, final int toIndex) {
        final StringBuilder sb = new StringBuilder();
        list.forEach(fromIndex, toIndex, b -> sb.append(b));
        return sb.toString();
    }

    // Doc pin for the @throws NullPointerException added on 2026-09-11: the three-arg copyOf REJECTS a null array,
    // unlike the one-arg copyOf, which documents (and returns) an empty list for null.
    @Test
    public void reviewFixes20260911_rangeCopyOfRejectsNullWhereWholeArrayCopyOfAcceptsIt() {
        assertThrows(IllegalArgumentException.class, () -> ByteList.copyOf(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ByteList.copyOf(null, 0, 1));

        assertEquals(0, ByteList.copyOf((byte[]) null).size(), "the one-arg factory still treats null as empty");
        assertEquals(0, ByteList.of((byte[]) null).size());
    }

    // Doc pin for the lowerMedian() example corrected on 2026-09-11: the old comment read "returns sorted:
    // [1, 2, 5, 8, 9]", which suggests the call sorts the list or returns a list. It does neither - N.lowerMedian
    // selects through a PriorityQueue and returns an OptionalByte.
    @Test
    public void reviewFixes20260911_lowerMedianNeitherSortsTheListNorReturnsOne() {
        final ByteList list = ByteList.of((byte) 5, (byte) 2, (byte) 8, (byte) 1, (byte) 9);

        assertEquals(OptionalByte.of((byte) 5), list.lowerMedian());
        assertEquals("[5, 2, 8, 1, 9]", list.toString(), "lowerMedian() must not reorder the list");

        assertEquals(OptionalByte.of((byte) 2), list.lowerMedian(1, 4));
        assertEquals("[5, 2, 8, 1, 9]", list.toString());
    }
}
