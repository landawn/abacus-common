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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.stream.ShortStream;

public class ShortListTest extends ShortListTestSupport {

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> list.forEach(0, 0, (com.landawn.abacus.util.function.ShortConsumer) null));
    }

    @Test
    public void testConstructors() {
        {
            list = new ShortList();
            ShortList list1 = new ShortList();
            assertTrue(list1.isEmpty());

            ShortList list2 = new ShortList(20);
            assertTrue(list2.isEmpty());
            assertEquals(20, list2.internalArray().length);

            short[] data = { 1, 2, 3 };
            ShortList list3 = new ShortList(data);
            assertEquals(3, list3.size());
            data[0] = 5;
            assertEquals((short) 5, list3.get(0));

            ShortList list4 = ShortList.of((short) 1, (short) 2);
            assertArrayEquals(new short[] { 1, 2 }, list4.toArray());

            short[] original = { 10, 20 };
            ShortList list5 = ShortList.copyOf(original);
            original[0] = 15;
            assertEquals((short) 10, list5.get(0));

            ShortList list6 = ShortList.copyOf(new short[] { 1, 2, 3, 4, 5 }, 1, 4);
            assertArrayEquals(new short[] { 2, 3, 4 }, list6.toArray());
        }
        {
            list = new ShortList();
            ShortList list1 = new ShortList();
            assertEquals(0, list1.size());

            ShortList list2 = new ShortList(10);
            assertEquals(0, list2.size());

            short[] arr = { 1, 2, 3 };
            ShortList list3 = new ShortList(arr);
            assertEquals(3, list3.size());
            assertEquals((short) 1, list3.get(0));
            assertEquals((short) 2, list3.get(1));
            assertEquals((short) 3, list3.get(2));

            short[] arr2 = { 1, 2, 3, 4, 5 };
            ShortList list4 = new ShortList(arr2, 3);
            assertEquals(3, list4.size());
            assertEquals((short) 1, list4.get(0));
            assertEquals((short) 2, list4.get(1));
            assertEquals((short) 3, list4.get(2));

            assertThrows(IndexOutOfBoundsException.class, () -> new ShortList(arr2, 10));

            assertThrows(IllegalArgumentException.class, () -> new ShortList(null));
            assertThrows(IllegalArgumentException.class, () -> new ShortList(null, 0));
        }
        {
            list = new ShortList();
            short[] arr = { (short) 1, (short) 2, (short) 3, (short) 4, (short) 5 };
            ShortList list = new ShortList(arr, 3);
            assertEquals(3, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 2, list.get(1));
            assertEquals((short) 3, list.get(2));
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new ShortList(-1));
    }

    @Test
    public void testConstructors_InvalidSize() {
        short[] arr = { (short) 1, (short) 2, (short) 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new ShortList(arr, 5));
        assertThrows(IllegalArgumentException.class, () -> new ShortList(arr, -1));
    }

    @Test
    public void testCapacity_LargeData() {
        ShortList smallList = new ShortList(2);
        for (int i = 0; i < 10000; i++) {
            smallList.add((short) i);
        }
        assertEquals(10000, smallList.size());

        for (int i = 0; i < Math.min(100, smallList.size()); i++) {
            assertEquals((short) i, smallList.get(i));
        }
    }

    @Test
    public void testOf() {
        short[] arr = { (short) 1, (short) 2, (short) 3, (short) 4 };
        ShortList list = ShortList.of(arr, 2);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
    }

    @Test
    public void testOf_Empty() {
        ShortList list = ShortList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testArray() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);

            short[] array1 = list.internalArray();
            short[] array2 = list.internalArray();

            assertSame(array1, array2);

            array1[0] = 100;
            assertEquals((short) 100, list.get(0));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            short[] arr = list.toArray();
            assertEquals(3, arr.length);
            assertEquals((short) 1, arr[0]);
            assertEquals((short) 2, arr[1]);
            assertEquals((short) 3, arr[2]);

            arr[0] = (short) 100;
            assertEquals((short) 1, list.get(0));
        }
    }

    @Test
    public void testRemoveAt() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);

        short deleted = list.removeAt(1);
        assertEquals((short) 2, deleted);
        assertArrayEquals(new short[] { 1, 3, 4, 5, 6 }, list.toArray());

        list.removeRange(1, 3);
        assertArrayEquals(new short[] { 1, 5, 6 }, list.toArray());

        list.removeAllAt(0, 2);
        assertArrayEquals(new short[] { 5 }, list.toArray());
    }

    @Test
    public void testRemoveAt_OutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
    }

    @Test
    public void testRemoveRange() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 5, list.get(1));
    }

    @Test
    public void testRemoveRange_Empty() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testFrequency() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 2);
        assertEquals(3, list.frequency((short) 2));
        assertEquals(1, list.frequency((short) 1));
        assertEquals(0, list.frequency((short) 100));
    }

    @Test
    public void testContainsDuplicates() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 1);
        assertTrue(list1.containsDuplicates());

        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertFalse(list2.containsDuplicates());

        assertFalse(new ShortList().containsDuplicates());
    }

    @Test
    public void testMove() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.moveRange(1, 3, 3);
        assertArrayEquals(new short[] { 1, 4, 5, 2, 3 }, list.toArray());

        list.replaceRange(0, 2, ShortList.of((short) 9, (short) 8, (short) 7));
        assertArrayEquals(new short[] { 9, 8, 7, 5, 2, 3 }, list.toArray());
    }

    @Test
    public void testQuery() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30, (short) 10);
        assertTrue(list.contains((short) 20));
        assertEquals(0, list.indexOf((short) 10));
        assertEquals(3, list.lastIndexOf((short) 10));

        assertTrue(list.containsAny(new short[] { 5, 15, 20 }));
        assertFalse(list.containsAny(new short[] { 5, 15, 25 }));

        assertTrue(list.containsAll(ShortList.of((short) 10, (short) 30)));
        assertFalse(list.containsAll(ShortList.of((short) 10, (short) 40)));

        assertTrue(list.disjoint(ShortList.of((short) 1, (short) 2)));
        assertFalse(list.disjoint(new short[] { 15, 25, 30 }));
    }

    @Test
    public void testConversions() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);

        assertArrayEquals(new short[] { 10, 20, 30 }, list.toArray());

        List<Short> boxed = list.boxed();
        assertEquals(List.of((short) 10, (short) 20, (short) 30), boxed);

        IntList intList = list.toIntList();
        assertArrayEquals(new int[] { 10, 20, 30 }, intList.toArray());

        assertEquals(50, list.stream(1, 3).sum());
    }

    @Test
    public void testStats() {
        ShortList list = ShortList.of((short) 9, (short) 2, (short) 7, (short) 5, (short) 2);
        assertEquals(OptionalShort.of((short) 2), list.min(1, 4));
        assertEquals(OptionalShort.of((short) 7), list.max(1, 4));
        assertEquals(OptionalShort.of((short) 5), list.lowerMedian(1, 4));
        assertEquals(2, list.frequency((short) 2));
    }

    @Test
    public void testStatic() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(3, list1.size());
        assertEquals((short) 1, list1.get(0));
        assertEquals((short) 2, list1.get(1));
        assertEquals((short) 3, list1.get(2));

        ShortList list2 = ShortList.of((short[]) null);
        assertEquals(0, list2.size());

        short[] arr = { 1, 2, 3, 4 };
        ShortList list3 = ShortList.of(arr, 2);
        assertEquals(2, list3.size());
        assertEquals((short) 1, list3.get(0));
        assertEquals((short) 2, list3.get(1));

        ShortList list4 = ShortList.copyOf(arr);
        assertEquals(4, list4.size());
        arr[0] = 10;
        assertEquals((short) 1, list4.get(0));

        ShortList list5 = ShortList.copyOf(arr, 1, 3);
        assertEquals(2, list5.size());
        assertEquals((short) 2, list5.get(0));
        assertEquals((short) 3, list5.get(1));

        ShortList list6 = ShortList.repeat((short) 5, 4);
        assertEquals(4, list6.size());
        for (int i = 0; i < 4; i++) {
            assertEquals((short) 5, list6.get(i));
        }

        ShortList list7 = ShortList.random(10);
        assertEquals(10, list7.size());
        for (int i = 0; i < 10; i++) {
            short value = list7.get(i);
            assertTrue(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE);
        }
    }

    @Test
    public void testCopyOf() {
        short[] arr = { (short) 1, (short) 2, (short) 3, (short) 4, (short) 5 };
        ShortList list = ShortList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals((short) 2, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 4, list.get(2));
    }

    @Test
    public void testCopyOf_Null() {
        ShortList list = ShortList.copyOf(null);
        assertEquals(0, list.size());
    }

    @Test
    public void testRange() {
        {
            list = new ShortList();
            assertArrayEquals(new short[] { 5, 6, 7 }, ShortList.range((short) 5, (short) 8).toArray());
            assertArrayEquals(new short[] { 5, 6, 7, 8 }, ShortList.rangeClosed((short) 5, (short) 8).toArray());
            assertArrayEquals(new short[] { 0, 3, 6 }, ShortList.rangeClosed((short) 0, (short) 8, (short) 3).toArray());
            assertArrayEquals(new short[] { 7, 7, 7 }, ShortList.repeat((short) 7, 3).toArray());
            assertEquals(10, ShortList.random(10).size());
        }
        {
            list = new ShortList();
            ShortList list1 = ShortList.range((short) 0, (short) 10, (short) 20);
            assertEquals(1, list1.size());
            assertEquals((short) 0, list1.get(0));

            ShortList list2 = ShortList.range((short) -100, (short) -200, (short) -25);
            assertEquals(4, list2.size());
            assertEquals((short) -100, list2.get(0));
            assertEquals((short) -125, list2.get(1));
            assertEquals((short) -150, list2.get(2));
            assertEquals((short) -175, list2.get(3));
        }
    }

    @Test
    public void testRange_Empty() {
        ShortList list = ShortList.range((short) 5, (short) 5);
        assertEquals(0, list.size());
    }

    @Test
    public void testReplaceRange() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            list.add((short) 4);

            ShortList replacement = ShortList.of((short) 10, (short) 20, (short) 30);
            list.replaceRange(1, 3, replacement);
            assertEquals(5, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 10, list.get(1));
            assertEquals((short) 20, list.get(2));
            assertEquals((short) 30, list.get(3));
            assertEquals((short) 4, list.get(4));

            list.clear();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            list.replaceRange(1, 2, new short[] { 100, 101 });
            assertEquals(4, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 100, list.get(1));
            assertEquals((short) 101, list.get(2));
            assertEquals((short) 3, list.get(3));

            list.replaceRange(1, 3, new short[0]);
            assertEquals(2, list.size());
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            ShortList replacement = ShortList.of((short) 10, (short) 20);
            list.replaceRange(1, 4, replacement);
            assertEquals(4, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 10, list.get(1));
            assertEquals((short) 20, list.get(2));
            assertEquals((short) 5, list.get(3));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            short[] replacement = { (short) 10, (short) 20 };
            list.replaceRange(1, 4, replacement);
            assertEquals(4, list.size());
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 10, list.get(1));
            assertEquals((short) 20, list.get(2));
            assertEquals((short) 5, list.get(3));
        }
    }

    @Test
    public void testReplaceRange_Empty() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        sl.replaceRange(1, 3, ShortList.of());
        assertEquals(3, sl.size());
        assertEquals((short) 1, sl.get(0));
        assertEquals((short) 4, sl.get(1));
        assertEquals((short) 5, sl.get(2));
    }

    @Test
    public void testRepeat() {
        ShortList list = ShortList.repeat((short) 5, 3);
        assertEquals(3, list.size());
        assertEquals((short) 5, list.get(0));
        assertEquals((short) 5, list.get(1));
        assertEquals((short) 5, list.get(2));
    }

    @Test
    public void testRandom() {
        ShortList list = ShortList.random(5);
        assertEquals(5, list.size());
    }

    @Test
    public void testRandom_Empty() {
        ShortList list = ShortList.random(0);
        assertEquals(0, list.size());
    }

    @Test
    public void testGetSet() {
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3);
            ShortList list2 = ShortList.of((short) 2, (short) 3, (short) 4, (short) 4);

            ShortList intersection = list1.intersection(list2);
            intersection.sort();
            assertArrayEquals(new short[] { 2, 3 }, intersection.toArray());

            ShortList difference = list1.difference(list2);
            assertArrayEquals(new short[] { 1, 2 }, difference.toArray());

            ShortList symmDiff = list1.symmetricDifference(list2);
            symmDiff.sort();
            assertArrayEquals(new short[] { 1, 2, 4, 4 }, symmDiff.toArray());
        }
        {
            list = new ShortList();
            list.add((short) 10);
            list.add((short) 20);
            list.add((short) 30);

            assertEquals((short) 10, list.get(0));
            assertEquals((short) 20, list.get(1));
            assertEquals((short) 30, list.get(2));

            assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));

            short oldValue = list.set(1, (short) 25);
            assertEquals((short) 20, oldValue);
            assertEquals((short) 25, list.get(1));

            assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, (short) 40));
        }
    }

    @Test
    public void testGetSet_OutOfBounds() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, (short) 100));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, (short) 100));
    }

    @Test
    public void testFast() {
        // fastRemove is private; exercised indirectly by remove(short)
        ShortList sl = ShortList.of((short) 10, (short) 20, (short) 30, (short) 40);
        boolean changed = sl.remove((short) 20);
        assertTrue(changed);
        assertEquals(3, sl.size());
        assertEquals((short) 10, sl.get(0));
        assertEquals((short) 30, sl.get(1));
        assertEquals((short) 40, sl.get(2));
    }

    @Test
    public void testRetainAll() {
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            ShortList list2 = ShortList.of((short) 2, (short) 4, (short) 5);
            boolean result = list1.retainAll(list2);
            assertTrue(result);
            assertEquals(2, list1.size());
            assertEquals((short) 2, list1.get(0));
            assertEquals((short) 4, list1.get(1));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            short[] arr = { (short) 2, (short) 4, (short) 5 };
            boolean result = list.retainAll(arr);
            assertTrue(result);
            assertEquals(2, list.size());
            assertEquals((short) 2, list.get(0));
            assertEquals((short) 4, list.get(1));
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            sl.retainAll(new short[] { (short) 1, (short) 3 });
            assertEquals(2, sl.size());
            assertEquals((short) 1, sl.get(0));
            assertEquals((short) 3, sl.get(1));
        }
    }

    @Test
    public void testRetainAll_Empty() {
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3);
            assertTrue(sl.retainAll(ShortList.of()));
            assertEquals(0, sl.size());
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2);
            assertTrue(sl.retainAll(new short[0]));
            assertEquals(0, sl.size());
        }
    }

    @Test
    public void testBatch() {
        {
            list = new ShortList();
            // batchRemove(c, false) path when c.size() <= 3 or this.size() <= 9
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            boolean changed = sl.removeAll(ShortList.of((short) 2, (short) 4));
            assertTrue(changed);
            assertEquals(3, sl.size());
            assertFalse(sl.contains((short) 2));
            assertFalse(sl.contains((short) 4));
            assertTrue(sl.contains((short) 1));
            assertTrue(sl.contains((short) 3));
            assertTrue(sl.contains((short) 5));
        }
        {
            list = new ShortList();
            // batchRemove(c, true) path when c.size() > 3 and size() > 9
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 7, (short) 8, (short) 9, (short) 10);
            ShortList toRetain = ShortList.of((short) 2, (short) 4, (short) 6, (short) 8);
            boolean changed = sl.retainAll(toRetain);
            assertTrue(changed);
            assertEquals(4, sl.size());
            assertTrue(sl.contains((short) 2));
            assertTrue(sl.contains((short) 4));
            assertFalse(sl.contains((short) 1));
            assertFalse(sl.contains((short) 5));
        }
    }

    @Test
    public void testBatch_LargeData() {
        {
            list = new ShortList();
            // triggers the Set path (c.size() > 3 && size() > 9)
            ShortList sl = new ShortList();
            for (short i = 1; i <= 15; i++) {
                sl.add(i);
            }
            ShortList retain = ShortList.of((short) 1, (short) 3, (short) 5, (short) 7, (short) 9);
            sl.retainAll(retain);
            assertEquals(5, sl.size());
            assertEquals((short) 1, sl.get(0));
            assertEquals((short) 3, sl.get(1));
        }
        {
            list = new ShortList();
            for (int i = 0; i < 1000; i++) {
                list.add((short) (i % 100));
            }

            ShortList toRemove = new ShortList();
            for (int i = 0; i < 50; i++) {
                toRemove.add((short) (i * 2));
            }

            int originalSize = list.size();
            list.removeAll(toRemove);
            assertTrue(list.size() < originalSize);

            for (int i = 0; i < list.size(); i++) {
                short value = list.get(i);
                assertTrue(value % 2 == 1 || value >= 100);
            }
        }
    }

    @Test
    public void testMoveRange() {
        ShortList list = ShortList.of((short) 0, (short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.moveRange(1, 3, 3);
        assertEquals(ShortList.of((short) 0, (short) 3, (short) 4, (short) 1, (short) 2, (short) 5), list);
    }

    @Test
    public void testReplaceAll() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 1);
            list.add((short) 3);

            assertEquals(2, list.replaceAll((short) 1, (short) 10));
            assertEquals(4, list.size());
            assertEquals((short) 10, list.get(0));
            assertEquals((short) 2, list.get(1));
            assertEquals((short) 10, list.get(2));
            assertEquals((short) 3, list.get(3));

            list.replaceAll(s -> (short) (s * 2));
            assertEquals((short) 20, list.get(0));
            assertEquals((short) 4, list.get(1));
            assertEquals((short) 20, list.get(2));
            assertEquals((short) 6, list.get(3));

            assertTrue(list.replaceIf(s -> s > 10, (short) 0));
            assertEquals((short) 0, list.get(0));
            assertEquals((short) 4, list.get(1));
            assertEquals((short) 0, list.get(2));
            assertEquals((short) 6, list.get(3));

            assertFalse(list.replaceIf(s -> s > 100, (short) 1));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            list.replaceAll(new ShortUnaryOperator() {
                @Override
                public short applyAsShort(short operand) {
                    return (short) (operand * 2);
                }
            });
            assertEquals((short) 2, list.get(0));
            assertEquals((short) 4, list.get(1));
            assertEquals((short) 6, list.get(2));
        }
    }

    @Test
    public void testReplaceAll_Overflow() {
        list.add((short) 20000);
        list.add((short) 25000);
        list.add((short) -20000);

        list.replaceAll(s -> (short) (s * 2));

        assertEquals((short) -25536, list.get(0));
        assertEquals((short) -15536, list.get(1));
        assertEquals((short) 25536, list.get(2));
    }

    @Test
    public void testReplaceAll_Null() {
        ShortList nonEmpty = ShortList.of((short) 1, (short) 2, (short) 3);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceAll((com.landawn.abacus.util.function.ShortUnaryOperator) null));

        ShortList empty = new ShortList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceAll((com.landawn.abacus.util.function.ShortUnaryOperator) null));
    }

    @Test
    public void testShort() {
        list.add(Short.MIN_VALUE);
        list.add((short) -1);
        list.add((short) 0);
        list.add((short) 1);
        list.add(Short.MAX_VALUE);

        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals((short) -1, list.get(1));
        assertEquals((short) 0, list.get(2));
        assertEquals((short) 1, list.get(3));
        assertEquals(Short.MAX_VALUE, list.get(4));

        assertEquals(OptionalShort.of(Short.MIN_VALUE), list.min());
        assertEquals(OptionalShort.of(Short.MAX_VALUE), list.max());
    }

    @Test
    public void testShort_Overflow() {
        list.add(Short.MAX_VALUE);
        list.add((short) (Short.MAX_VALUE - 1));
        list.add(Short.MIN_VALUE);
        list.add((short) (Short.MIN_VALUE + 1));

        list.replaceAll(s -> (short) (s + 1));

        assertEquals(Short.MIN_VALUE, list.get(0));
        assertEquals(Short.MAX_VALUE, list.get(1));
        assertEquals((short) (Short.MIN_VALUE + 1), list.get(2));
        assertEquals((short) (Short.MIN_VALUE + 2), list.get(3));
    }

    @Test
    public void testReplaceIf() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            boolean result = list.replaceIf(new ShortPredicate() {
                @Override
                public boolean test(short value) {
                    return value % 2 == 0;
                }
            }, (short) 100);
            assertTrue(result);
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 100, list.get(1));
            assertEquals((short) 3, list.get(2));
            assertEquals((short) 100, list.get(3));
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) -2, (short) 3, (short) -4);
            boolean changed = sl.replaceIf(s -> s < 0, (short) 0);
            assertTrue(changed);
            assertEquals((short) 0, sl.get(1));
            assertEquals((short) 0, sl.get(3));
            assertEquals((short) 1, sl.get(0));
        }
        {
            list = new ShortList();
            list.addAll(new short[] { 1, 2, 3 });
            assertFalse(list.replaceIf(v -> v > 100, (short) 0));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testReplaceIf_LargeData() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        assertTrue(list.replaceIf(s -> s >= 20 && s <= 40, (short) -1));

        for (int i = 0; i < list.size(); i++) {
            short value = list.get(i);
            if (i >= 20 && i <= 40) {
                assertEquals((short) -1, value);
            } else {
                assertEquals((short) i, value);
            }
        }
    }

    @Test
    public void testFill() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            list.fill(1, 4, (short) 100);
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 100, list.get(1));
            assertEquals((short) 100, list.get(2));
            assertEquals((short) 100, list.get(3));
            assertEquals((short) 5, list.get(4));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            list.fill((short) 100);
            assertEquals((short) 100, list.get(0));
            assertEquals((short) 100, list.get(1));
            assertEquals((short) 100, list.get(2));
        }
    }

    @Test
    public void testContains() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(list.contains((short) 2));
        assertFalse(list.contains((short) 100));
    }

    @Test
    public void testContainsAny() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);

            assertTrue(list.containsAny(ShortList.of((short) 1)));
            assertTrue(list.containsAny(ShortList.of((short) 4, (short) 3)));
            assertFalse(list.containsAny(ShortList.of((short) 4, (short) 5)));
            assertFalse(list.containsAny(new ShortList()));

            assertTrue(list.containsAny(new short[] { 1 }));
            assertTrue(list.containsAny(new short[] { 4, 2 }));
            assertFalse(list.containsAny(new short[] { 4, 5 }));

            assertTrue(list.containsAll(ShortList.of((short) 1, (short) 2)));
            assertTrue(list.containsAll(ShortList.of((short) 3)));
            assertFalse(list.containsAll(ShortList.of((short) 1, (short) 4)));
            assertTrue(list.containsAll(new ShortList()));

            assertTrue(list.containsAll(new short[] { 1, 2 }));
            assertFalse(list.containsAll(new short[] { 1, 4 }));
            assertTrue(list.containsAll(new short[0]));

            list.clear();
            assertFalse(list.containsAny(ShortList.of((short) 1)));
            assertFalse(list.containsAll(ShortList.of((short) 1)));
            assertTrue(list.containsAll(new ShortList()));
        }
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList list2 = ShortList.of((short) 3, (short) 4, (short) 5);
            assertTrue(list1.containsAny(list2));

            ShortList list3 = ShortList.of((short) 10, (short) 11);
            assertFalse(list1.containsAny(list3));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            short[] arr1 = { (short) 3, (short) 4, (short) 5 };
            assertTrue(list.containsAny(arr1));

            short[] arr2 = { (short) 10, (short) 11 };
            assertFalse(list.containsAny(arr2));
        }
    }

    @Test
    public void testContainsAll() {
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            ShortList list2 = ShortList.of((short) 2, (short) 3);
            assertTrue(list1.containsAll(list2));

            ShortList list3 = ShortList.of((short) 2, (short) 5);
            assertFalse(list1.containsAll(list3));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            short[] arr1 = { (short) 2, (short) 3 };
            assertTrue(list.containsAll(arr1));

            short[] arr2 = { (short) 2, (short) 5 };
            assertFalse(list.containsAll(arr2));
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            assertTrue(sl.containsAll(ShortList.of((short) 1, (short) 3)));
            assertFalse(sl.containsAll(ShortList.of((short) 1, (short) 5)));
        }
    }

    @Test
    public void testDisjoint() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);

            assertFalse(list.disjoint(ShortList.of((short) 1)));
            assertFalse(list.disjoint(ShortList.of((short) 2, (short) 3)));
            assertTrue(list.disjoint(ShortList.of((short) 3, (short) 4)));
            assertTrue(list.disjoint(new ShortList()));

            assertFalse(list.disjoint(new short[] { 1 }));
            assertTrue(list.disjoint(new short[] { 3, 4 }));
            assertTrue(list.disjoint(new short[0]));

            list.clear();
            assertTrue(list.disjoint(ShortList.of((short) 1)));
            assertTrue(list.disjoint(new short[] { 2 }));
        }
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList list2 = ShortList.of((short) 4, (short) 5, (short) 6);
            assertTrue(list1.disjoint(list2));

            ShortList list3 = ShortList.of((short) 3, (short) 4);
            assertFalse(list1.disjoint(list3));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            short[] arr1 = { (short) 4, (short) 5, (short) 6 };
            assertTrue(list.disjoint(arr1));

            short[] arr2 = { (short) 3, (short) 4 };
            assertFalse(list.disjoint(arr2));
        }
    }

    @Test
    public void testIntersection() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            list.add((short) 2);

            ShortList other = ShortList.of((short) 2, (short) 3, (short) 4, (short) 2);
            ShortList result = list.intersection(other);
            assertEquals(3, result.size());
            assertEquals((short) 2, result.get(0));
            assertEquals((short) 3, result.get(1));
            assertEquals((short) 2, result.get(2));

            result = list.intersection(new short[] { 1, 1, 4 });
            assertEquals(1, result.size());
            assertEquals((short) 1, result.get(0));

            result = list.intersection(new ShortList());
            assertEquals(0, result.size());
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3);
            ShortList result = sl.intersection(ShortList.of((short) 2, (short) 2, (short) 4));
            // multiset intersection: both sides have 2 occurrences of 2
            assertEquals(2, result.size());
            assertTrue(result.contains((short) 2));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            short[] arr = { (short) 3, (short) 4, (short) 5, (short) 6 };
            ShortList result = list.intersection(arr);
            assertEquals(2, result.size());
            assertEquals((short) 3, result.get(0));
            assertEquals((short) 4, result.get(1));
        }
    }

    @Test
    public void testIntersection_LargeData() {
        for (int i = 0; i < 100; i++) {
            list.add((short) (i % 10));
        }

        ShortList other = new ShortList();
        for (int i = 0; i < 50; i++) {
            other.add((short) (i % 5));
        }

        ShortList intersection = list.intersection(other);

        int[] counts = new int[5];
        for (int i = 0; i < intersection.size(); i++) {
            counts[intersection.get(i)]++;
        }

        for (int i = 0; i < 5; i++) {
            assertEquals(10, counts[i]);
        }
    }

    @Test
    public void testDifference() {
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            list.add((short) 2);

            ShortList other = ShortList.of((short) 2, (short) 2);
            ShortList result = list.difference(other);
            assertEquals(2, result.size());
            assertEquals((short) 1, result.get(0));
            assertEquals((short) 3, result.get(1));

            result = list.difference(new short[] { 1, 3 });
            assertEquals(2, result.size());
            assertEquals((short) 2, result.get(0));
            assertEquals((short) 2, result.get(1));

            result = list.difference(new ShortList());
            assertEquals(4, result.size());
        }
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
            ShortList result = sl.difference(ShortList.of((short) 2));
            // removes one occurrence of 2, leaving [1, 3, 2]
            assertEquals(3, result.size());
            assertTrue(result.contains((short) 1));
            assertTrue(result.contains((short) 3));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            short[] arr = { (short) 3, (short) 4, (short) 5 };
            ShortList result = list.difference(arr);
            assertEquals(2, result.size());
            assertEquals((short) 1, result.get(0));
            assertEquals((short) 2, result.get(1));
        }
    }

    @Test
    public void testDifference_Empty() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList result = sl.difference(ShortList.of());
        assertEquals(3, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        ShortList receiver = ShortList.of((short) 1, (short) 9);
        ShortList repeatedOther = ShortList.of((short) 1, (short) 2, (short) 1);
        ShortList expected = ShortList.of((short) 9, (short) 1, (short) 2);
        assertEquals(expected, receiver.symmetricDifference(repeatedOther));
        assertEquals(expected, receiver.symmetricDifference(repeatedOther.toArray()));
        {
            list = new ShortList();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);

            ShortList other = ShortList.of((short) 2, (short) 3, (short) 4);
            ShortList result = list.symmetricDifference(other);
            assertEquals(2, result.size());
            assertEquals((short) 1, result.get(0));
            assertEquals((short) 4, result.get(1));

            result = list.symmetricDifference(new short[] { 1, 2, 3, 4, 5 });
            assertEquals(2, result.size());
            assertEquals((short) 4, result.get(0));
            assertEquals((short) 5, result.get(1));

            result = list.symmetricDifference(new ShortList());
            assertEquals(3, result.size());
        }
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList list2 = ShortList.of((short) 3, (short) 4, (short) 5);
            ShortList result = list1.symmetricDifference(list2);
            assertTrue(result.size() > 0);
            assertTrue(result.contains((short) 1));
            assertTrue(result.contains((short) 2));
            assertTrue(result.contains((short) 4));
            assertTrue(result.contains((short) 5));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            short[] arr = { (short) 3, (short) 4, (short) 5 };
            ShortList result = list.symmetricDifference(arr);
            assertTrue(result.size() > 0);
            assertTrue(result.contains((short) 1));
            assertTrue(result.contains((short) 2));
            assertTrue(result.contains((short) 4));
            assertTrue(result.contains((short) 5));
        }
    }

    @Test
    public void testIndexOf() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        assertEquals(3, list.indexOf((short) 2, 2));
        assertEquals(-1, list.indexOf((short) 2, 4));
    }

    @Test
    public void testLastIndexOf() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2, (short) 1);
        assertEquals(3, sl.lastIndexOf((short) 2, 4));
        assertEquals(1, sl.lastIndexOf((short) 2, 2));
        assertEquals(-1, sl.lastIndexOf((short) 9, 4));
    }

    @Test
    public void testMin() {
        {
            list = new ShortList();
            assertFalse(list.min().isPresent());
            assertFalse(list.max().isPresent());
            assertFalse(list.lowerMedian().isPresent());

            list.add((short) 5);
            assertEquals(OptionalShort.of((short) 5), list.min());
            assertEquals(OptionalShort.of((short) 5), list.max());
            assertEquals(OptionalShort.of((short) 5), list.lowerMedian());

            list.clear();
            list.add((short) 3);
            list.add((short) 1);
            list.add((short) 4);
            list.add((short) 1);
            list.add((short) 5);

            assertEquals(OptionalShort.of((short) 1), list.min());
            assertEquals(OptionalShort.of((short) 5), list.max());
            assertEquals(OptionalShort.of((short) 3), list.lowerMedian());

            assertEquals(OptionalShort.of((short) 1), list.min(1, 4));
            assertEquals(OptionalShort.of((short) 4), list.max(1, 4));
            assertEquals(OptionalShort.of((short) 1), list.lowerMedian(1, 4));

            assertFalse(list.min(2, 2).isPresent());
            assertFalse(list.max(2, 2).isPresent());
            assertFalse(list.lowerMedian(2, 2).isPresent());
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 5, (short) 2, (short) 8, (short) 3);
            OptionalShort min = list.min(1, 4);
            assertTrue(min.isPresent());
            assertEquals((short) 2, min.get());
        }
    }

    @Test
    public void testMin_Empty() {
        ShortList list = new ShortList();
        OptionalShort min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void testMin_LargeData() {
        {
            list = new ShortList();
            Random rand = new Random(42);
            for (int i = 0; i < 10000; i++) {
                list.add((short) rand.nextInt(65536));
            }

            OptionalShort min = list.min();
            OptionalShort max = list.max();
            OptionalShort median = list.lowerMedian();

            assertTrue(min.isPresent());
            assertTrue(max.isPresent());
            assertTrue(median.isPresent());

            assertTrue(min.get() <= median.get());
            assertTrue(median.get() <= max.get());

            OptionalShort partialMin = list.min(1000, 2000);
            OptionalShort partialMax = list.max(1000, 2000);
            assertTrue(partialMin.isPresent());
            assertTrue(partialMax.isPresent());
            assertTrue(partialMin.get() <= partialMax.get());
        }
        {
            list = new ShortList();
            for (int i = 0; i < 100; i++) {
                list.add((short) 42);
            }
            assertEquals(OptionalShort.of((short) 42), list.min());
            assertEquals(OptionalShort.of((short) 42), list.max());
            assertEquals(OptionalShort.of((short) 42), list.lowerMedian());

            list.clear();
            list.add((short) 1);
            list.add((short) 2);
            list.add((short) 3);
            list.add((short) 4);
            OptionalShort median = list.lowerMedian();
            assertTrue(median.isPresent());
            short medianValue = median.get();
            assertTrue(medianValue == 2 || medianValue == 3);
        }
    }

    @Test
    public void testMax() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 5, (short) 2, (short) 8, (short) 3);
            OptionalShort max = list.max(1, 4);
            assertTrue(max.isPresent());
            assertEquals((short) 8, max.get());
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
            OptionalShort max = list.max();
            assertTrue(max.isPresent());
            assertEquals((short) 4, max.get());
        }
    }

    @Test
    public void testMax_Empty() {
        ShortList list = new ShortList();
        OptionalShort max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void testMedian() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 5, (short) 2, (short) 8, (short) 3);
            OptionalShort median = list.lowerMedian(1, 4);
            assertTrue(median.isPresent());
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 3, (short) 1, (short) 2);
            OptionalShort median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals((short) 2, median.get());
        }
    }

    @Test
    public void testMedian_Empty() {
        ShortList list = new ShortList();
        OptionalShort median = list.lowerMedian();
        assertFalse(median.isPresent());
    }

    @Test
    public void testEach() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            final ShortList result = new ShortList();
            list.forEach(1, 4, new ShortConsumer() {
                @Override
                public void accept(short value) {
                    result.add(value);
                }
            });
            assertEquals(3, result.size());
            assertEquals((short) 2, result.get(0));
            assertEquals((short) 3, result.get(1));
            assertEquals((short) 4, result.get(2));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            final ShortList result = new ShortList();
            list.forEach(new ShortConsumer() {
                @Override
                public void accept(short value) {
                    result.add(value);
                }
            });
            assertEquals(3, result.size());
            assertEquals((short) 1, result.get(0));
            assertEquals((short) 2, result.get(1));
            assertEquals((short) 3, result.get(2));
        }
    }

    @Test
    public void testEach_LargeData() {
        for (short i = 0; i < 100; i++) {
            list.add(i);
        }

        final int[] count = { 0 };
        final List<Short> collected = new ArrayList<>();

        list.forEach(s -> {
            collected.add(s);
            count[0]++;
        });

        assertEquals(100, count[0]);
        assertEquals(100, collected.size());

        collected.clear();
        list.forEach(50, 40, s -> collected.add(s));
        assertEquals(10, collected.size());
        assertEquals(Short.valueOf((short) 50), collected.get(0));
        assertEquals(Short.valueOf((short) 41), collected.get(9));
    }

    @Test
    public void testEach_Null() {
        // Empty lists do not evaluate callbacks; non-empty lists fail naturally when invoking them.
        final ShortList empty = new ShortList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.forEach((com.landawn.abacus.util.function.ShortConsumer) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.removeIf((com.landawn.abacus.util.function.ShortPredicate) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceIf((com.landawn.abacus.util.function.ShortPredicate) null, (short) 0));

        final ShortList nonEmpty = ShortList.of((short) 1, (short) 2);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.forEach((com.landawn.abacus.util.function.ShortConsumer) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.removeIf((com.landawn.abacus.util.function.ShortPredicate) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceIf((com.landawn.abacus.util.function.ShortPredicate) null, (short) 0));
    }

    @Test
    public void testFirst() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 5, (short) 10, (short) 15);
            assertEquals((short) 5, list.getFirst());
            assertEquals((short) 15, list.getLast());
            assertEquals(OptionalShort.of((short) 5), list.first());
            assertEquals(OptionalShort.of((short) 15), list.last());

            assertEquals((short) 5, list.removeFirst());
            assertEquals((short) 15, list.removeLast());
            assertArrayEquals(new short[] { 10 }, list.toArray());

            ShortList emptyList = new ShortList();
            assertThrows(NoSuchElementException.class, emptyList::getFirst);
            assertThrows(NoSuchElementException.class, emptyList::removeLast);
        }
        {
            list = new ShortList();
            OptionalShort first = list.first();
            OptionalShort last = list.last();
            assertFalse(first.isPresent());
            assertFalse(last.isPresent());

            list.add((short) 10);
            list.add((short) 20);
            list.add((short) 30);

            first = list.first();
            last = list.last();
            assertTrue(first.isPresent());
            assertTrue(last.isPresent());
            assertEquals((short) 10, first.get());
            assertEquals((short) 30, last.get());
        }
    }

    @Test
    public void testFirst_Empty() {
        ShortList list = new ShortList();
        OptionalShort first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        OptionalShort last = list.last();
        assertTrue(last.isPresent());
        assertEquals((short) 3, last.get());
    }

    @Test
    public void testLast_Empty() {
        ShortList list = new ShortList();
        OptionalShort last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        ShortList sl = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3, (short) 1, (short) 4);
        ShortList result = sl.distinct(1, 5);
        // distinct of [2, 2, 3, 1]
        assertEquals(3, result.size());
        assertTrue(result.contains((short) 2));
        assertTrue(result.contains((short) 3));
        assertTrue(result.contains((short) 1));
    }

    @Test
    public void testIsSorted() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
        assertTrue(list1.isSorted());

        ShortList list2 = ShortList.of((short) 1, (short) 3, (short) 2, (short) 4);
        assertFalse(list2.isSorted());
    }

    @Test
    public void testSort() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 9, (short) 2, (short) 7, (short) 5, (short) 1);
            assertFalse(list.isSorted());

            list.sort();
            assertArrayEquals(new short[] { 1, 2, 5, 7, 9 }, list.toArray());
            assertTrue(list.isSorted());

            assertEquals(2, list.binarySearch((short) 5));
            assertTrue(list.binarySearch((short) 6) < 0);

            list.parallelSort();
            assertArrayEquals(new short[] { 1, 2, 5, 7, 9 }, list.toArray());

            list.reverseSort();
            assertArrayEquals(new short[] { 9, 7, 5, 2, 1 }, list.toArray());
        }
        {
            list = new ShortList();
            list.add((short) 42);
            list.sort();
            assertEquals(1, list.size());
            assertEquals((short) 42, list.get(0));
        }
    }

    @Test
    public void testParallelSort() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
        list.parallelSort();
        assertEquals((short) 1, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 3, list.get(2));
        assertEquals((short) 4, list.get(3));
    }

    @Test
    public void testParallelSort_LargeData() {
        for (int i = 0; i < 1000; i++) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        for (int i = 1000; i >= 0; i--) {
            list.add((short) i);
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        list.clear();
        Random rand = new Random(42);
        for (int i = 0; i < 2000; i++) {
            list.add((short) rand.nextInt(65536));
        }
        list.parallelSort();
        assertTrue(list.isSorted());

        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1) <= list.get(i));
        }
    }

    @Test
    public void testReverseSort() {
        ShortList list = ShortList.of((short) 3, (short) 1, (short) 4, (short) 2);
        list.reverseSort();
        assertEquals((short) 4, list.get(0));
        assertEquals((short) 3, list.get(1));
        assertEquals((short) 2, list.get(2));
        assertEquals((short) 1, list.get(3));
    }

    @Test
    public void testBinarySearch() {
        {
            list = new ShortList();
            list.add((short) -100);
            list.add((short) -50);
            list.add((short) 0);
            list.add((short) 50);
            list.add((short) 100);

            assertEquals(0, list.binarySearch((short) -100));
            assertEquals(1, list.binarySearch((short) -50));
            assertEquals(2, list.binarySearch((short) 0));
            assertEquals(3, list.binarySearch((short) 50));
            assertEquals(4, list.binarySearch((short) 100));

            assertEquals(2, list.binarySearch(1, 4, (short) 0));
            assertTrue(list.binarySearch(0, 2, (short) 50) < 0);
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            int index = list.binarySearch(1, 4, (short) 3);
            assertEquals(2, index);
        }
    }

    @Test
    public void testReverse() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            list.reverse(1, 4);
            assertEquals((short) 1, list.get(0));
            assertEquals((short) 4, list.get(1));
            assertEquals((short) 3, list.get(2));
            assertEquals((short) 2, list.get(3));
            assertEquals((short) 5, list.get(4));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4);
            list.reverse();
            assertEquals((short) 4, list.get(0));
            assertEquals((short) 3, list.get(1));
            assertEquals((short) 2, list.get(2));
            assertEquals((short) 1, list.get(3));
        }
        {
            list = new ShortList();
            list.add((short) 42);
            list.reverse();
            assertEquals(1, list.size());
            assertEquals((short) 42, list.get(0));
        }
    }

    @Test
    public void testModifications() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
        list.reverse(1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, list.toArray());
        list.rotate(-1);
        assertArrayEquals(new short[] { 4, 3, 2, 5, 1 }, list.toArray());
        list.swap(0, 4);
        assertArrayEquals(new short[] { 1, 3, 2, 5, 4 }, list.toArray());
        list.fill((short) 0);
        assertArrayEquals(new short[] { 0, 0, 0, 0, 0 }, list.toArray());
    }

    @Test
    public void testRotate() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            list.rotate(2);
            assertEquals((short) 4, list.get(0));
            assertEquals((short) 5, list.get(1));
            assertEquals((short) 1, list.get(2));
            assertEquals((short) 2, list.get(3));
            assertEquals((short) 3, list.get(4));
        }
        {
            list = new ShortList();
            list.addAll(new short[] { 1, 2, 3, 4, 5 });
            list.rotate(-2);
            assertEquals((short) 3, list.get(0));
            assertEquals((short) 4, list.get(1));
            assertEquals((short) 5, list.get(2));
        }
    }

    @Test
    public void testShuffle() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            list.shuffle();
            assertEquals(5, list.size());
            assertTrue(list.contains((short) 1));
            assertTrue(list.contains((short) 2));
            assertTrue(list.contains((short) 3));
            assertTrue(list.contains((short) 4));
            assertTrue(list.contains((short) 5));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            Random rnd = new Random(12345);
            list.shuffle(rnd);
            assertEquals(5, list.size());
        }
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new ShortList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> ShortList.of((short) 1).shuffle(null));
    }

    @Test
    public void testSwap() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.swap(0, 2);
        assertEquals((short) 3, list.get(0));
        assertEquals((short) 2, list.get(1));
        assertEquals((short) 1, list.get(2));
    }

    @Test
    public void testSwap_OutOfBounds() {
        list.addAll(new short[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopy() {
        {
            list = new ShortList();
            ShortList listWithCap = new ShortList(10);
            listWithCap.addAll(new short[] { 1, 2, 3 });
            assertEquals(10, listWithCap.internalArray().length);
            listWithCap.trimToSize();
            assertEquals(3, listWithCap.internalArray().length);

            ShortList copy = listWithCap.copy(0, 3, 2);
            assertArrayEquals(new short[] { 1, 3 }, copy.toArray());

            List<ShortList> chunks = listWithCap.split(2);
            assertEquals(2, chunks.size());
            assertArrayEquals(new short[] { 1, 2 }, chunks.get(0).toArray());
            assertArrayEquals(new short[] { 3 }, chunks.get(1).toArray());
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList copy = list.copy();
            assertEquals(3, copy.size());
            assertEquals((short) 1, copy.get(0));
            assertEquals((short) 2, copy.get(1));
            assertEquals((short) 3, copy.get(2));

            list.set(0, (short) 100);
            assertEquals((short) 1, copy.get(0));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            ShortList copy = list.copy(0, 5, 2);
            assertEquals(3, copy.size());
            assertEquals((short) 1, copy.get(0));
            assertEquals((short) 3, copy.get(1));
            assertEquals((short) 5, copy.get(2));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            ShortList copy = list.copy(1, 4);
            assertEquals(3, copy.size());
            assertEquals((short) 2, copy.get(0));
            assertEquals((short) 3, copy.get(1));
            assertEquals((short) 4, copy.get(2));
        }
    }

    @Test
    public void testCopy_NegativeStep() {
        for (short i = 0; i < 20; i++) {
            list.add(i);
        }

        ShortList reversed = list.copy(19, -1, -1);
        assertEquals(20, reversed.size());
        for (int i = 0; i < 20; i++) {
            assertEquals((short) (19 - i), reversed.get(i));
        }

        ShortList everyThirdReverse = list.copy(18, -1, -3);
        assertEquals(7, everyThirdReverse.size());
        assertEquals((short) 18, everyThirdReverse.get(0));
        assertEquals((short) 15, everyThirdReverse.get(1));
        assertEquals((short) 12, everyThirdReverse.get(2));
        assertEquals((short) 9, everyThirdReverse.get(3));
        assertEquals((short) 6, everyThirdReverse.get(4));
        assertEquals((short) 3, everyThirdReverse.get(5));
        assertEquals((short) 0, everyThirdReverse.get(6));
    }

    @Test
    public void testCopy_Empty() {
        ShortList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testSplit() {
        {
            list = new ShortList();
            ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6);
            List<ShortList> chunks = sl.split(0, 6, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals((short) 1, chunks.get(0).get(0));
            assertEquals((short) 3, chunks.get(1).get(0));
        }
        {
            list = new ShortList();
            for (short i = 0; i < 23; i++) {
                list.add(i);
            }

            List<ShortList> chunks = list.split(0, 23, 5);
            assertEquals(5, chunks.size());
            assertEquals(5, chunks.get(0).size());
            assertEquals(5, chunks.get(1).size());
            assertEquals(5, chunks.get(2).size());
            assertEquals(5, chunks.get(3).size());
            assertEquals(3, chunks.get(4).size());

            short expected = 0;
            for (ShortList chunk : chunks) {
                for (int i = 0; i < chunk.size(); i++) {
                    assertEquals(expected++, chunk.get(i));
                }
            }
        }
        {
            list = new ShortList();
            list.addAll(new short[] { 1, 2, 3, 4, 5 });
            List<ShortList> chunks = list.split(0, 5, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(1, chunks.get(2).size());
        }
    }

    @Test
    public void testSplit_Empty() {
        List<ShortList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testTrim() {
        ShortList list = new ShortList(100);
        list.add((short) 1);
        list.add((short) 2);
        list.trimToSize();
        assertEquals(2, list.size());
    }

    @Test
    public void testClear() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty_Empty() {
        ShortList list = new ShortList();
        assertTrue(list.isEmpty());

        list.add((short) 1);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testEmpty_Empty() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
        assertEquals(list, new ShortList());
        assertEquals(list.hashCode(), new ShortList().hashCode());

        assertFalse(list.contains((short) 0));
        assertEquals(-1, list.indexOf((short) 0));
        assertEquals(-1, list.lastIndexOf((short) 0));
        assertEquals(0, list.frequency((short) 0));

        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.lowerMedian().isPresent());
        assertFalse(list.first().isPresent());
        assertFalse(list.last().isPresent());

        assertTrue(list.toArray().length == 0);
        assertTrue(list.boxed().isEmpty());
        assertTrue(list.distinct(0, 0).isEmpty());
        assertFalse(list.iterator().hasNext());

        list.sort();
        list.parallelSort();
        list.reverse();
        list.shuffle();
        list.fill((short) 0);
        list.removeRange(0, 0);

        assertTrue(list.isEmpty());
    }

    @Test
    public void testSize() {
        ShortList list = new ShortList();
        assertEquals(0, list.size());

        list.add((short) 1);
        assertEquals(1, list.size());

        list.add((short) 2);
        assertEquals(2, list.size());
    }

    @Test
    public void testLarge_LargeData() {
        final int size = 20000;
        for (int i = 0; i < size; i++) {
            list.add((short) (i % 1000));
        }

        assertEquals(size, list.size());

        assertTrue(list.contains((short) 500));
        assertTrue(list.indexOf((short) 999) >= 0);

        list.sort();
        assertTrue(list.isSorted());

        ShortList distinct = list.distinct(0, list.size());
        assertEquals(1000, distinct.size());

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testBoxed() {
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            List<Short> boxed = list.boxed(1, 4);
            assertEquals(3, boxed.size());
            assertEquals(Short.valueOf((short) 2), boxed.get(0));
            assertEquals(Short.valueOf((short) 3), boxed.get(1));
            assertEquals(Short.valueOf((short) 4), boxed.get(2));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            List<Short> boxed = list.boxed();
            assertEquals(3, boxed.size());
            assertEquals(Short.valueOf((short) 1), boxed.get(0));
            assertEquals(Short.valueOf((short) 2), boxed.get(1));
            assertEquals(Short.valueOf((short) 3), boxed.get(2));
        }
    }

    @Test
    public void testBoxed_Empty() {
        List<Short> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testInt() {
        {
            list = new ShortList();
            list.add((short) -1);
            list.add((short) -32768);
            list.add((short) 32767);
            list.add((short) 0);

            IntList intList = list.toIntList();
            assertEquals(4, intList.size());
            assertEquals(-1, intList.get(0));
            assertEquals(-32768, intList.get(1));
            assertEquals(32767, intList.get(2));
            assertEquals(0, intList.get(3));
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            IntList intList = list.toIntList();
            assertEquals(3, intList.size());
            assertEquals(1, intList.get(0));
            assertEquals(2, intList.get(1));
            assertEquals(3, intList.get(2));
        }
    }

    @Test
    public void testCollection() {
        ShortList sl = ShortList.of((short) 5, (short) 6, (short) 7, (short) 8);
        List<Short> result = sl.toCollection(1, 3, ArrayList::new);
        assertEquals(2, result.size());
        assertEquals(Short.valueOf((short) 6), result.get(0));
        assertEquals(Short.valueOf((short) 7), result.get(1));
    }

    @Test
    public void testCollection_NaN() {
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 10);
        ArrayList<Short> collection = list.toCollection(ArrayList::new);
        assertEquals(List.of((short) 10, (short) 20, (short) 10), collection);

        Multiset<Short> multiset = list.toMultiset();
        assertEquals(2, multiset.count((short) 10));
        assertEquals(1, multiset.count((short) 20));
    }

    @Test
    public void testMultiset() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 2, (short) 3);
        Multiset<Short> multiset = list.toMultiset(0, 4, new java.util.function.IntFunction<Multiset<Short>>() {
            @Override
            public Multiset<Short> apply(int capacity) {
                return new Multiset<>();
            }
        });
        assertNotNull(multiset);
    }

    @Test
    public void testIterator() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortIterator iter = list.iterator();
        assertNotNull(iter);
        assertTrue(iter.hasNext());
        assertEquals((short) 1, iter.nextShort());
        assertEquals((short) 2, iter.nextShort());
        assertEquals((short) 3, iter.nextShort());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Empty() {
        ShortIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testStream() {
        {
            list = new ShortList();
            for (short i = 1; i <= 100; i++) {
                list.add(i);
            }

            ShortStream stream = list.stream();
            int sum = stream.sum();
            assertEquals(5050, sum);

            ShortStream rangeStream = list.stream(10, 20);
            short[] arr = rangeStream.toArray();
            assertEquals(10, arr.length);
            for (int i = 0; i < 10; i++) {
                assertEquals((short) (11 + i), arr[i]);
            }
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 4, (short) 5);
            ShortStream stream = list.stream(1, 4);
            assertNotNull(stream);
            assertEquals(3, stream.count());
        }
    }

    @Test
    public void testStream_LargeData() {
        for (short i = 0; i < 50; i++) {
            list.add(i);
        }

        ShortStream stream = list.stream();
        short[] evens = stream.filter(s -> s % 2 == 0).toArray();
        assertEquals(25, evens.length);
        for (int i = 0; i < evens.length; i++) {
            assertEquals((short) (i * 2), evens[i]);
        }
    }

    @Test
    public void testStream_Empty() {
        assertEquals(0, list.stream().count());
    }

    @Test
    public void testGetFirst() {
        {
            list = new ShortList();
            assertThrows(NoSuchElementException.class, () -> list.getFirst());
            assertThrows(NoSuchElementException.class, () -> list.getLast());

            list.add((short) 10);
            list.add((short) 20);
            list.add((short) 30);

            assertEquals((short) 10, list.getFirst());
            assertEquals((short) 30, list.getLast());
        }
        {
            list = new ShortList();
            ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
            assertEquals((short) 1, list.getFirst());
        }
    }

    @Test
    public void testGetFirst_Empty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals((short) 3, list.getLast());
    }

    @Test
    public void testGetLast_Empty() {
        ShortList list = new ShortList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testHashCode() {
        ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals() {
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList list3 = ShortList.of((short) 1, (short) 2, (short) 4);

            assertTrue(list1.equals(list2));
            assertFalse(list1.equals(list3));
            assertFalse(list1.equals(null));
            assertFalse(list1.equals("string"));
        }
        {
            list = new ShortList();
            ShortList sl1 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList sl2 = ShortList.of((short) 1, (short) 2, (short) 3);
            assertTrue(sl1.equals(sl2));
        }
        {
            list = new ShortList();
            ShortList list1 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList list2 = ShortList.of((short) 1, (short) 2, (short) 3);
            ShortList list3 = ShortList.of((short) 3, (short) 2, (short) 1);

            assertEquals(list1, list1);

            assertEquals(list1, list2);
            assertEquals(list2, list1);

            ShortList list4 = ShortList.of((short) 1, (short) 2, (short) 3);
            assertEquals(list1, list2);
            assertEquals(list2, list4);
            assertEquals(list1, list4);

            assertEquals(list1.hashCode(), list2.hashCode());

            assertNotEquals(list1, list3);

            assertNotEquals(list1, null);
            assertNotEquals(list1, new ArrayList<>());
        }
    }

    @Test
    public void testEquals_Null() {
        ShortList sl = ShortList.of((short) 1);
        assertFalse(sl.equals(null));
    }

    @Test
    public void testString() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void testString_Empty() {
        ShortList list = new ShortList();
        String str = list.toString();
        assertNotNull(str);
    }

    @Test
    public void testEnsureCapacity_LargeData() {
        // Test adding many elements beyond initial capacity
        ShortList sl = new ShortList(2);
        for (short i = 0; i < 100; i++) {
            sl.add(i);
        }
        assertEquals(100, sl.size());
        assertEquals((short) 0, sl.get(0));
        assertEquals((short) 99, sl.get(99));
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
        // ShortList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final ShortList withSpareCapacity = ShortList.of(new short[] { 1, 2, 3, 4, 5 }, 3);

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
            final ShortList self = ShortList.of((short) 1, (short) 2, (short) 3);
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final ShortList expected = ShortList.of((short) 1, (short) 2, (short) 3);
            final ShortList inserted = ShortList.of((short) 1, (short) 2, (short) 3);
            expected.addAll(index, ShortList.of((short) 1, (short) 2, (short) 3));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final ShortList appended = ShortList.of((short) 1, (short) 2, (short) 3);
        appended.addAll(appended);
        assertEquals("[1, 2, 3, 1, 2, 3]", appended.toString());

        // The interesting middle case, spelled out.
        final ShortList middle = ShortList.of((short) 1, (short) 2, (short) 3);
        middle.addAll(1, middle);
        assertEquals("[1, 1, 2, 3, 2, 3]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final ShortList a = ShortList.of((short) 1, (short) 2, (short) 3);
        assertFalse(a.removeAll((ShortList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((short[]) null));
        assertFalse(a.removeAll(new short[0]));
        assertFalse(a.removeAll(new ShortList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final ShortList b = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(b.retainAll((ShortList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final ShortList c = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(c.retainAll((short[]) null));
        assertEquals(0, c.size());

        final ShortList d = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(d.retainAll(new short[0]));
        assertEquals(0, d.size());

        final ShortList e = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(e.retainAll(new ShortList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final ShortList empty = new ShortList();
        assertFalse(empty.retainAll((ShortList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final ShortList f = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(f.retainAll(ShortList.of((short) 1)));
        assertEquals(1, f.size());
    }
}
