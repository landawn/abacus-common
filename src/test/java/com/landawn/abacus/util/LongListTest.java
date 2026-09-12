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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.LongStream;

public class LongListTest extends LongListTestSupport {

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> list.forEach(0, 0, (java.util.function.LongConsumer) null));
    }

    @Test
    public void testConstructors() {
        long[] boundaries = { Long.MIN_VALUE, (long) Integer.MIN_VALUE - 1, (long) Integer.MAX_VALUE + 1, Long.MAX_VALUE };
        LongList fullWidth = new LongList(boundaries);
        assertArrayEquals(boundaries, fullWidth.toArray());
        assertEquals(Long.MIN_VALUE, fullWidth.getFirst());
        assertEquals(Long.MAX_VALUE, fullWidth.getLast());
        {
            list = new LongList();
            LongList list = new LongList(10);
            assertEquals(0, list.size());
            assertTrue(list.isEmpty());
            assertTrue(list.internalArray().length >= 10 || list.internalArray().length == 0);
        }
        {
            list = new LongList();
            long[] array = { 1L, 2L, 3L };
            LongList list = new LongList(array);
            assertEquals(3, list.size());
            assertEquals(1L, list.get(0));
        }
        {
            list = new LongList();
            long[] array = { 1L, 2L, 3L, 4L, 5L };
            LongList list = new LongList(array, 3);
            assertEquals(3, list.size());
        }
        {
            list = new LongList();
            LongList list = new LongList();
            assertTrue(list.isEmpty());
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LongList(-1));
    }

    @Test
    public void testConstructors_Null() {
        {
            list = new LongList();
            assertThrows(IllegalArgumentException.class, () -> new LongList(null, 0));
        }
        {
            list = new LongList();
            assertThrows(IllegalArgumentException.class, () -> new LongList(null));
        }
    }

    @Test
    public void testConstructors_InvalidSize() {
        long[] array = { 1L, 2L, 3L };
        assertThrows(IndexOutOfBoundsException.class, () -> new LongList(array, 4));
    }

    @Test
    public void testOf() {
        {
            list = new LongList();
            long[] a = { 1L, 2L, 3L, 4L, 5L };
            LongList list = LongList.of(a, 3);
            assertEquals(3, list.size());
            assertArrayEquals(new long[] { 1L, 2L, 3L }, list.toArray());
            assertThrows(IndexOutOfBoundsException.class, () -> LongList.of(a, 6));
        }
        {
            list = new LongList();
            LongList list = LongList.of(new long[] { 1L, 2L, 3L }, 2);
            assertEquals(2, list.size());
        }
        {
            list = new LongList();
            LongList list = LongList.of();
            assertTrue(list.isEmpty());
        }
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L, 3L);
            assertEquals(3, list.size());
            assertEquals(2L, list.get(1));
        }
    }

    @Test
    public void testOf_Null() {
        {
            list = new LongList();
            assertThrows(IndexOutOfBoundsException.class, () -> LongList.of(null, 5));
        }
        {
            list = new LongList();
            LongList list = LongList.of((long[]) null);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testOther() {
        LongList list = LongList.of(10L, 20L, 30L);
        FloatList floatList = list.toFloatList();
        assertArrayEquals(new float[] { 10.0f, 20.0f, 30.0f }, floatList.toArray());
    }

    @Test
    public void testBatch() {
        list.add(Long.MAX_VALUE);
        list.add(Long.MAX_VALUE - 1);
        list.add(Long.MIN_VALUE);

        LongList other = LongList.of(Long.MAX_VALUE, Long.MIN_VALUE + 1);

        LongList intersection = list.intersection(other);
        assertEquals(1, intersection.size());
        assertEquals(Long.MAX_VALUE, intersection.get(0));

        LongList difference = list.difference(other);
        assertEquals(2, difference.size());
        assertTrue(difference.contains(Long.MAX_VALUE - 1));
        assertTrue(difference.contains(Long.MIN_VALUE));
    }

    @Test
    public void testBatch_LargeData() {
        {
            list = new LongList();
            int size = 1000;
            LongList list1 = LongList.range(0, size);
            LongList list2 = LongList.range(size / 2, size + size / 2);

            LongList intersection = list1.intersection(list2);
            assertEquals(size / 2, intersection.size());

            LongList difference = list1.difference(list2);
            assertEquals(size / 2, difference.size());

            LongList symDiff = list1.symmetricDifference(list2);
            assertEquals(size, symDiff.size());
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
            LongList retain = LongList.of(1L, 3L, 5L, 7L);
            ll.retainAll(retain);
            assertEquals(4, ll.size());
            assertEquals(1L, ll.get(0));
            assertEquals(3L, ll.get(1));
        }
    }

    @Test
    public void testCopyOf() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        LongList list = LongList.copyOf(array, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2L, list.get(0));
        assertEquals(4L, list.get(2));
    }

    @Test
    public void testCopyOf_Null() {
        LongList list = LongList.copyOf(null);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testCopyOf_InvalidRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> LongList.copyOf(array, 2, 10));
    }

    @Test
    public void testRange() {
        {
            list = new LongList();
            LongList list = LongList.rangeClosed(Long.MIN_VALUE, Long.MIN_VALUE + 4);
            assertEquals(5, list.size());
            assertEquals(Long.MIN_VALUE, list.get(0));
            assertEquals(Long.MIN_VALUE + 4, list.get(4));
        }
        {
            list = new LongList();
            LongList list = LongList.range(Long.MAX_VALUE - 5, Long.MAX_VALUE);
            assertEquals(5, list.size());
            assertEquals(Long.MAX_VALUE - 5, list.get(0));
            assertEquals(Long.MAX_VALUE - 1, list.get(4));
        }
        {
            list = new LongList();
            LongList list = LongList.range(0L, 10L, 2L);
            assertEquals(5, list.size());
            assertEquals(0L, list.get(0));
            assertEquals(8L, list.get(4));
        }
        {
            list = new LongList();
            LongList list = LongList.rangeClosed(0L, 10L, 2L);
            assertEquals(6, list.size());
            assertEquals(10L, list.get(5));
        }
        {
            list = new LongList();
            LongList list = LongList.range(1L, 5L);
            assertEquals(4, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(4L, list.get(3));
        }
        {
            list = new LongList();
            LongList list = LongList.rangeClosed(1L, 5L);
            assertEquals(5, list.size());
            assertEquals(5L, list.get(4));
        }
    }

    @Test
    public void testRange_NegativeStep() {
        LongList list = LongList.range(5L, 0L, -1L);
        assertEquals(5, list.size());
        assertEquals(5L, list.get(0));
        assertEquals(1L, list.get(4));
    }

    @Test
    public void testRange_ZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> LongList.range(1L, 10L, 0L));
    }

    @Test
    public void testRepeat() {
        LongList list = LongList.repeat(5L, 3);
        assertEquals(3, list.size());
        assertEquals(5L, list.get(0));
        assertEquals(5L, list.get(2));
    }

    @Test
    public void testRandom() {
        LongList list = LongList.random(5);
        assertEquals(5, list.size());
    }

    @Test
    public void testGetSet() {
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L, 3L);
            assertEquals(2L, list.get(1));
            long oldValue = list.set(1, 99L);
            assertEquals(2L, oldValue);
            assertEquals(99L, list.get(1));
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
            assertThrows(IndexOutOfBoundsException.class, () -> list.set(3, 100L));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(1L);
            java.util.Set<Long> set = list.toSet();
            assertEquals(2, set.size());
            assertTrue(set.contains(1L));
            assertTrue(set.contains(2L));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(2L);
            java.util.Set<Long> set = list.toSet(1, 4);
            assertEquals(2, set.size());
        }
    }

    @Test
    public void testGetSet_OutOfBounds() {
        {
            list = new LongList();
            list.add(10L);
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        }
        {
            list = new LongList();
            assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10L));
        }
    }

    @Test
    public void testAdd() {
        {
            list = new LongList();
            LongList list = new LongList();
            list.addFirst(10L);
            list.addLast(30L);
            list.addFirst(0L);
            list.addLast(40L);
            assertArrayEquals(new long[] { 0L, 10L, 30L, 40L }, list.toArray());
            assertEquals(0L, list.removeFirst());
            assertEquals(40L, list.removeLast());
            assertArrayEquals(new long[] { 10L, 30L }, list.toArray());
        }
        {
            list = new LongList();
            LongList list = new LongList();
            list.add(10L);
            list.add(30L);
            list.add(1, 20L);
            assertEquals(3, list.size());
            assertArrayEquals(new long[] { 10L, 20L, 30L }, list.toArray());
            assertThrows(IndexOutOfBoundsException.class, () -> list.add(4, 40L));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 2L, 3L, 4L });
            list.add(0, 1L);
            assertEquals(4, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(2L, list.get(1));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            list.add(list.size(), 4L);
            assertEquals(4, list.size());
            assertEquals(4L, list.get(3));
        }
    }

    @Test
    public void testAdd_OutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(1, 10L));
    }

    @Test
    public void testAddAll() {
        {
            list = new LongList();
            list.add(1L);
            list.add(4L);
            LongList other = LongList.of(2L, 3L);
            assertTrue(list.addAll(1, other));
            assertEquals(4, list.size());
            assertEquals(2L, list.get(1));
            assertEquals(3L, list.get(2));
        }
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L);
            assertTrue(list.addAll(LongList.of(3L, 4L)));
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
            assertFalse(list.addAll(new LongList()));
        }
        {
            list = new LongList();
            list.add(1L);
            LongList other = LongList.of(2L, 3L);
            assertTrue(list.addAll(other));
            assertEquals(3, list.size());
            assertEquals(3L, list.get(2));
        }
        {
            list = new LongList();
            LongList list = LongList.of(1L, 4L);
            assertTrue(list.addAll(1, LongList.of(2L, 3L)));
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, list.toArray());
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 4L, 5L);
            LongList toAdd = LongList.of(2L, 3L);
            assertTrue(ll.addAll(1, toAdd));
            assertEquals(5, ll.size());
            assertEquals(2L, ll.get(1));
            assertEquals(3L, ll.get(2));
            assertEquals(4L, ll.get(3));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(4L);
            assertTrue(list.addAll(1, new long[] { 2L, 3L }));
            assertEquals(4, list.size());
            assertEquals(2L, list.get(1));
        }
        {
            list = new LongList();
            list.add(1L);
            assertTrue(list.addAll(new long[] { 2L, 3L }));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testAddAll_Empty() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            LongList empty = new LongList();

            assertFalse(list.addAll(0, empty));
            assertFalse(list.addAll(1, empty));
            assertFalse(list.addAll(list.size(), empty));
        }
        {
            list = new LongList();
            list.add(1L);
            LongList empty = new LongList();
            boolean result = list.addAll(empty);

            assertFalse(result);
            assertEquals(1, list.size());
        }
        {
            list = new LongList();
            list.add(1L);
            assertFalse(list.addAll(new LongList()));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L);
            assertFalse(ll.addAll(1, LongList.of()));
            assertEquals(2, ll.size());
        }
    }

    @Test
    public void testAddAll_Null() {
        list.add(1L);
        assertFalse(list.addAll((long[]) null));
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveAt() {
        {
            list = new LongList();
            final LongList padded = new LongList(32);
            padded.addAll(new long[] { 10L, 20L, 30L, 40L, 50L });
            final long[] backingArray = padded.internalArray();

            padded.removeAllAt(3, 1, 3, 1);

            assertSame(backingArray, padded.internalArray());
            assertArrayEquals(new long[] { 10L, 30L, 50L }, padded.toArray());
            assertEquals(0L, backingArray[3]);
            assertEquals(0L, backingArray[4]);
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

            assertEquals(1L, list.removeAt(0));
            assertEquals(4, list.size());
            assertEquals(2L, list.get(0));

            assertEquals(5L, list.removeAt(list.size() - 1));
            assertEquals(3, list.size());
            assertEquals(4L, list.get(list.size() - 1));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.removeAllAt(1, 3);
            assertEquals(2, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(3L, list.get(1));
        }
    }

    @Test
    public void testRemoveAt_Empty() {
        list.removeAllAt();
        assertTrue(list.isEmpty());

        list.addAll(new long[] { 1L, 2L, 3L });
        list.removeAllAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAt_OutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(0));
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

        LongIterator iter = list.iterator();
        list.add(6L);

        assertTrue(iter.hasNext());
        iter.nextLong();
    }

    @Test
    public void testArray() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            long[] array = list.internalArray();

            array[1] = 20L;
            assertEquals(20L, list.get(1));

            list.clear();
            long[] newArray = list.internalArray();
            assertSame(array, newArray);
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            long[] array = list.toArray();
            assertEquals(3, array.length);
            assertEquals(1L, array[0]);
        }
    }

    @Test
    public void testRemoveRange() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.removeRange(1, 3);
            assertEquals(2, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(4L, list.get(1));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            list.removeRange(0, 3);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testRemoveRange_Empty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testNegative() {
        list.addAll(new long[] { -5L, -3L, -1L, 0L, 1L, 3L, 5L });

        OptionalLong min = list.min();
        assertTrue(min.isPresent());
        assertEquals(-5L, min.getAsLong());

        list.sort();
        assertEquals(-5L, list.get(0));
        assertEquals(5L, list.get(6));

        assertEquals(2, list.indexOf(-1L));
        assertEquals(3, list.binarySearch(0L));
    }

    @Test
    public void testRemove() {
        list.add(10L);
        list.add(20L);
        list.add(10L);
        assertTrue(list.remove(10L));
        assertEquals(2, list.size());
        assertEquals(20L, list.get(0));
    }

    @Test
    public void testEmpty() {
        assertFalse(list.remove(1L));
        assertFalse(list.removeAllOccurrences(1L));
        assertFalse(list.removeIf(x -> true));
        assertFalse(list.removeDuplicates());
        assertFalse(list.containsDuplicates());
        assertTrue(list.isSorted());

        list.sort();
        list.reverse();
        list.shuffle();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testRemoveAllOccurrences() {
        {
            list = new LongList();
            list.add(10L);
            list.add(20L);
            list.add(10L);
            list.add(30L);
            assertTrue(list.removeAllOccurrences(10L));
            assertEquals(2, list.size());
            assertEquals(20L, list.get(0));
            assertEquals(30L, list.get(1));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            assertFalse(list.removeAllOccurrences(5L));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveAllOccurrences_Empty() {
        assertFalse(list.removeAllOccurrences(5L));
    }

    @Test
    public void testRemoveAll() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            LongList toRemove = LongList.of(2L, 3L);
            assertTrue(list.removeAll(toRemove));
            assertEquals(1, list.size());
            assertEquals(1L, list.get(0));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 4L, 5L);
            ll.removeAll(LongList.of(2L, 4L));
            assertEquals(3, ll.size());
            assertFalse(ll.contains(2L));
            assertFalse(ll.contains(4L));
        }
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L, 3L, 4L, 5L, 2L);
            LongList toRemove = LongList.of(2L, 4L, 6L);
            assertTrue(list.removeAll(toRemove));
            assertArrayEquals(new long[] { 1L, 3L, 5L }, list.toArray());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            assertTrue(list.removeAll(new long[] { 2L, 3L }));
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoveAll_Empty() {
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L);
            assertFalse(ll.removeAll(LongList.of()));
            assertEquals(3, ll.size());
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L);
            assertFalse(ll.removeAll(new long[0]));
            assertEquals(3, ll.size());
        }
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final long[] shared = { 1, 2, 3, 1 };
        final LongList values = LongList.of(shared);
        final LongList removed = LongList.of(shared, 2);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new long[] { 3 }, values.toArray());
    }

    @Test
    public void testRemoveIf() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            assertTrue(list.removeIf(x -> x % 2 == 0));
            assertEquals(2, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(3L, list.get(1));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            assertFalse(list.removeIf(x -> x > 10));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveDuplicates() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 1L, 2L, 2L, 2L, 3L, 3L, 4L, 5L, 5L });
            assertTrue(list.removeDuplicates());
            assertEquals(5, list.size());
            for (int i = 1; i <= 5; i++) {
                assertEquals(i, list.get(i - 1));
            }
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(1L);
            list.add(3L);
            list.add(2L);
            assertTrue(list.removeDuplicates());
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRetainAll() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            LongList toRetain = LongList.of(2L, 3L, 4L);
            assertTrue(list.retainAll(toRetain));
            assertEquals(2, list.size());
            assertEquals(2L, list.get(0));
            assertEquals(3L, list.get(1));
        }
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L, 3L, 2L, 4L);
            LongList toRetain = LongList.of(2L, 4L, 5L);
            assertTrue(list.retainAll(toRetain));
            assertArrayEquals(new long[] { 2L, 2L, 4L }, list.toArray());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            assertTrue(list.retainAll(new long[] { 2L, 3L, 4L }));
            assertEquals(2, list.size());
        }
    }

    @Test
    public void testRetainAll_Empty() {
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L);
            assertTrue(ll.retainAll(new long[0]));
            assertEquals(0, ll.size());
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            assertTrue(list.retainAll(new LongList()));
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testMoveRange() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
            list.moveRange(0, 2, 3);
            assertEquals(5, list.size());
            assertEquals(3L, list.get(0));
            assertEquals(4L, list.get(1));
            assertEquals(5L, list.get(2));
            assertEquals(1L, list.get(3));
            assertEquals(2L, list.get(4));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.moveRange(0, 2, 2);
            assertEquals(3L, list.get(0));
            assertEquals(4L, list.get(1));
            assertEquals(1L, list.get(2));
            assertEquals(2L, list.get(3));
        }
    }

    @Test
    public void testReplaceRange() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            LongList replacement = LongList.of(99L);
            list.replaceRange(1, 3, replacement);
            assertEquals(3, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(99L, list.get(1));
            assertEquals(4L, list.get(2));
        }
        {
            list = new LongList();
            LongList list = LongList.of(0L, 1L, 2L, 3L, 4L, 5L);
            LongList replacement = LongList.of(99L, 98L);
            list.replaceRange(2, 4, replacement);
            assertArrayEquals(new long[] { 0L, 1L, 99L, 98L, 4L, 5L }, list.toArray());
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
            long[] replacement = { 10L, 20L };

            list.replaceRange(1, 3, replacement);
            assertEquals(5, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(10L, list.get(1));
            assertEquals(20L, list.get(2));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 4L, 5L);
            ll.replaceRange(1, 4, new long[] { 20L, 30L });
            assertEquals(4, ll.size());
            assertEquals(20L, ll.get(1));
            assertEquals(30L, ll.get(2));
            assertEquals(5L, ll.get(3));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            list.replaceRange(1, 2, LongList.of(10L, 20L, 30L));
            assertEquals(5, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(10L, list.get(1));
            assertEquals(20L, list.get(2));
            assertEquals(30L, list.get(3));
            assertEquals(3L, list.get(4));
        }
    }

    @Test
    public void testReplaceRange_Empty() {
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 4L, 5L);
            ll.replaceRange(1, 3, new long[0]);
            assertEquals(3, ll.size());
            assertEquals(1L, ll.get(0));
            assertEquals(4L, ll.get(1));
            assertEquals(5L, ll.get(2));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
            list.replaceRange(1, 3, new LongList());
            assertEquals(3, list.size());
            assertEquals(1L, list.get(0));
            assertEquals(4L, list.get(1));
            assertEquals(5L, list.get(2));
        }
    }

    @Test
    public void testReplaceAll() {
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L, 1L, 3L, 1L);
            int count = list.replaceAll(1L, 99L);
            assertEquals(3, count);
            assertArrayEquals(new long[] { 99L, 2L, 99L, 3L, 99L }, list.toArray());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.replaceAll(x -> x * 2);
            assertEquals(2L, list.get(0));
            assertEquals(4L, list.get(1));
            assertEquals(6L, list.get(2));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });

            list.replaceAll(x -> x * 2);
            assertEquals(2L, list.get(0));
            assertEquals(4L, list.get(1));
            assertEquals(10L, list.get(4));
        }
    }

    @Test
    public void testReplaceAll_Null() {
        LongList nonEmpty = LongList.of(1L, 2L, 3L);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceAll((com.landawn.abacus.util.function.LongUnaryOperator) null));

        LongList empty = new LongList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceAll((com.landawn.abacus.util.function.LongUnaryOperator) null));
    }

    @Test
    public void testReplaceAll_Empty() {
        LongList ll = LongList.of();
        assertEquals(0, ll.replaceAll(1L, 2L));
    }

    @Test
    public void testLong() {
        {
            list = new LongList();
            list.add(Long.MIN_VALUE);
            list.add(0L);
            list.add(Long.MAX_VALUE);

            assertEquals(Long.MIN_VALUE, list.get(0));
            assertEquals(0L, list.get(1));
            assertEquals(Long.MAX_VALUE, list.get(2));

            OptionalLong min = list.min();
            assertTrue(min.isPresent());
            assertEquals(Long.MIN_VALUE, min.getAsLong());

            OptionalLong max = list.max();
            assertTrue(max.isPresent());
            assertEquals(Long.MAX_VALUE, max.getAsLong());
        }
        {
            list = new LongList();
            long bigValue1 = (long) Integer.MAX_VALUE * 2;
            long bigValue2 = (long) Integer.MIN_VALUE * 2;

            list.add(bigValue1);
            list.add(bigValue2);
            list.add(0L);

            assertTrue(list.contains(bigValue1));
            assertTrue(list.contains(bigValue2));

            list.sort();
            assertEquals(bigValue2, list.get(0));
            assertEquals(0L, list.get(1));
            assertEquals(bigValue1, list.get(2));
        }
    }

    @Test
    public void testLong_Overflow() {
        list.add(Long.MAX_VALUE);
        list.add(Long.MAX_VALUE);

        list.replaceAll(x -> x + 1);
        assertEquals(Long.MIN_VALUE, list.get(0));
        assertEquals(Long.MIN_VALUE, list.get(1));
    }

    @Test
    public void testReplaceIf() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            assertTrue(list.replaceIf(x -> x % 2 == 0, 99L));
            assertEquals(99L, list.get(1));
            assertEquals(99L, list.get(3));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            boolean result = list.replaceIf(x -> false, 10L);
            assertFalse(result);
        }
    }

    @Test
    public void testFill() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.fill(1, 3, 99L);
            assertEquals(1L, list.get(0));
            assertEquals(99L, list.get(1));
            assertEquals(99L, list.get(2));
            assertEquals(4L, list.get(3));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.fill(99L);
            assertEquals(99L, list.get(0));
            assertEquals(99L, list.get(1));
            assertEquals(99L, list.get(2));
        }
    }

    @Test
    public void testFill_Empty() {
        list.fill(10L);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFill_InvalidRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10L));
    }

    @Test
    public void testContains() {
        list.add(1L);
        list.add(2L);
        assertTrue(list.contains(1L));
        assertFalse(list.contains(3L));
    }

    @Test
    public void testContains_Empty() {
        assertFalse(list.contains(1L));
    }

    @Test
    public void testLarge() {
        long largeValue1 = Integer.MAX_VALUE + 1L;
        long largeValue2 = Integer.MAX_VALUE + 2L;

        list.add(largeValue1);
        list.add(largeValue2);

        assertEquals(largeValue1, list.get(0));
        assertEquals(largeValue2, list.get(1));

        assertTrue(list.contains(largeValue1));
        assertEquals(0, list.indexOf(largeValue1));
    }

    @Test
    public void testLarge_LargeData() {
        int size = 10000;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }

        assertEquals(size, list.size());
        assertEquals(0L, list.get(0));
        assertEquals(size - 1, list.get(size - 1));
    }

    @Test
    public void testContainsAny() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            assertTrue(list.containsAny(new long[] { 2L, 3L }));
            assertFalse(list.containsAny(new long[] { 4L, 5L }));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            assertTrue(list.containsAny(LongList.of(2L, 3L)));
            assertFalse(list.containsAny(LongList.of(4L, 5L)));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L);
            assertTrue(ll.containsAny(LongList.of(3L, 4L)));
            assertFalse(ll.containsAny(LongList.of(4L, 5L)));
        }
    }

    @Test
    public void testContainsAny_Empty() {
        LongList other = new LongList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            assertTrue(list.containsAll(new long[] { 1L, 2L }));
            assertFalse(list.containsAll(new long[] { 1L, 4L }));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            assertTrue(list.containsAll(LongList.of(1L, 2L)));
            assertFalse(list.containsAll(LongList.of(1L, 4L)));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 4L);
            assertTrue(ll.containsAll(new long[] { 1L, 3L }));
            assertFalse(ll.containsAll(new long[] { 1L, 9L }));
        }
    }

    @Test
    public void testContainsAll_Empty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertTrue(list.containsAll(new LongList()));
    }

    @Test
    public void testDisjoint() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            assertTrue(list.disjoint(new long[] { 3L, 4L }));
            assertFalse(list.disjoint(new long[] { 2L, 3L }));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            assertTrue(list.disjoint(LongList.of(3L, 4L)));
            assertFalse(list.disjoint(LongList.of(2L, 3L)));
        }
        {
            list = new LongList();
            LongList list1 = LongList.of(1L, 2L, 3L);
            LongList list2 = LongList.of(4L, 5L, 6L);
            assertTrue(list1.disjoint(list2));
            LongList list3 = LongList.of(3L, 4L, 5L);
            assertFalse(list1.disjoint(list3));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            assertFalse(list.disjoint(list));
        }
    }

    @Test
    public void testIntersection() {
        {
            list = new LongList();
            LongList list1 = LongList.of(1L, 2L, 2L, 3L, 4L);
            LongList list2 = LongList.of(2L, 3L, 5L, 2L);
            LongList intersection = list1.intersection(list2);
            intersection.sort();
            assertArrayEquals(new long[] { 2L, 2L, 3L }, intersection.toArray());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            LongList result = list.intersection(LongList.of(2L, 3L, 4L));
            assertEquals(2, result.size());
            assertTrue(result.contains(2L));
            assertTrue(result.contains(3L));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 4L);
            LongList result = ll.intersection(new long[] { 2L, 4L, 6L });
            assertEquals(2, result.size());
            assertTrue(result.contains(2L));
            assertTrue(result.contains(4L));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            LongList result = list.intersection(LongList.of(4L, 5L, 6L));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testIntersection_Empty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.intersection(new LongList());
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifference() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            LongList result = list.difference(LongList.of(2L, 4L));
            assertEquals(2, result.size());
            assertTrue(result.contains(1L));
            assertTrue(result.contains(3L));
        }
        {
            list = new LongList();
            LongList list1 = LongList.of(1L, 2L, 2L, 3L, 4L);
            LongList list2 = LongList.of(2L, 3L, 5L);
            LongList difference = list1.difference(list2);
            assertArrayEquals(new long[] { 1L, 2L, 4L }, difference.toArray());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            LongList result = list.difference(new long[] { 2L, 4L });
            assertEquals(2, result.size());
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            LongList result = list.difference(LongList.of(1L, 2L, 3L));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testDifference_Empty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.difference(new LongList());
        assertEquals(3, result.size());
        assertEquals(1L, result.get(0));
        assertEquals(2L, result.get(1));
        assertEquals(3L, result.get(2));
    }

    @Test
    public void testSymmetricDifference() {
        LongList receiver = LongList.of(1L, 9L);
        LongList other = LongList.of(1L, 2L, 1L);
        LongList expected = LongList.of(9L, 1L, 2L);
        assertEquals(expected, receiver.symmetricDifference(other));
        assertEquals(expected, receiver.symmetricDifference(other.toArray()));
        {
            list = new LongList();
            LongList list1 = LongList.of(1L, 2L, 2L, 3L);
            LongList list2 = LongList.of(2L, 3L, 4L);
            LongList symmDiff = list1.symmetricDifference(list2);
            symmDiff.sort();
            assertArrayEquals(new long[] { 1L, 2L, 4L }, symmDiff.toArray());
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L);
            LongList result = ll.symmetricDifference(new long[] { 2L, 4L });
            assertTrue(result.contains(1L));
            assertTrue(result.contains(3L));
            assertTrue(result.contains(4L));
            assertFalse(result.contains(2L));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            LongList result = list.symmetricDifference(LongList.of(2L, 3L));
            assertEquals(2, result.size());
            assertTrue(result.contains(1L));
            assertTrue(result.contains(3L));
        }
    }

    @Test
    public void testSymmetricDifference_Empty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        LongList result = list.symmetricDifference(new LongList());
        assertEquals(3, result.size());
    }

    @Test
    public void testFrequency() {
        list.add(1L);
        list.add(2L);
        list.add(1L);
        list.add(3L);
        list.add(1L);
        assertEquals(3, list.frequency(1L));
        assertEquals(0, list.frequency(4L));
    }

    @Test
    public void testIndexOf() {
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L, 3L, 2L, 1L);
            assertEquals(0, list.indexOf(1L));
            assertEquals(1, list.indexOf(2L));
            assertEquals(3, list.indexOf(2L, 2));
            assertEquals(-1, list.indexOf(4L));
            assertEquals(4, list.lastIndexOf(1L));
            assertEquals(3, list.lastIndexOf(2L));
            assertEquals(1, list.lastIndexOf(2L, 2));
            assertEquals(-1, list.lastIndexOf(4L));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

            assertEquals(3, list.indexOf(2L, 2));
            assertEquals(-1, list.indexOf(2L, 4));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(1L);
            assertEquals(2, list.indexOf(1L, 1));
        }
    }

    @Test
    public void testIndexOf_OutOfBounds() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertEquals(-1, list.indexOf(1L, 10));
    }

    @Test
    public void testLastIndexOf() {
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 2L, 1L);
            assertEquals(3, ll.lastIndexOf(2L, 4));
            assertEquals(1, ll.lastIndexOf(2L, 2));
            assertEquals(-1, ll.lastIndexOf(99L, 4));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(1L);
            list.add(2L);
            assertEquals(1, list.lastIndexOf(2L, 2));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 2L, 5L });

            assertEquals(1, list.lastIndexOf(2L, 2));
            assertEquals(-1, list.lastIndexOf(5L, 3));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(1L);
            assertEquals(2, list.lastIndexOf(1L));
        }
    }

    @Test
    public void testLastIndexOf_Empty() {
        assertEquals(-1, list.lastIndexOf(1L));
    }

    @Test
    public void testMin() {
        {
            list = new LongList();
            LongList list = LongList.of(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L, 5L);
            assertEquals(1L, list.min().getAsLong());
            assertEquals(9L, list.max().getAsLong());
            assertEquals(4L, list.lowerMedian().getAsLong());

            LongList emptyList = new LongList();
            assertTrue(emptyList.min().isEmpty());
            assertTrue(emptyList.max().isEmpty());
            assertTrue(emptyList.lowerMedian().isEmpty());
        }
        {
            list = new LongList();
            list.add(5L);

            OptionalLong min = list.min();
            assertTrue(min.isPresent());
            assertEquals(5L, min.getAsLong());

            OptionalLong max = list.max();
            assertTrue(max.isPresent());
            assertEquals(5L, max.getAsLong());

            OptionalLong median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals(5L, median.getAsLong());
        }
        {
            list = new LongList();
            list.add(3L);
            list.add(1L);
            list.add(5L);
            list.add(2L);
            OptionalLong min = list.min(1, 3);
            assertTrue(min.isPresent());
            assertEquals(1L, min.get());
        }
    }

    @Test
    public void testMin_Empty() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });

            assertFalse(list.min(1, 1).isPresent());
            assertFalse(list.max(1, 1).isPresent());
            assertFalse(list.lowerMedian(1, 1).isPresent());
        }
        {
            list = new LongList();
            assertTrue(list.min().isEmpty());
        }
    }

    @Test
    public void testMax() {
        {
            list = new LongList();
            list.add(3L);
            list.add(1L);
            list.add(5L);
            list.add(2L);
            OptionalLong max = list.max(1, 3);
            assertTrue(max.isPresent());
            assertEquals(5L, max.get());
        }
        {
            list = new LongList();
            list.add(3L);
            list.add(1L);
            list.add(2L);
            assertEquals(3L, list.max().get());
        }
    }

    @Test
    public void testMedian() {
        {
            list = new LongList();
            list.add(1L);
            list.add(3L);
            list.add(2L);
            list.add(5L);
            OptionalLong median = list.lowerMedian(0, 3);
            assertTrue(median.isPresent());
            assertEquals(2L, median.get());
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L });
            OptionalLong median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals(2L, median.getAsLong());
        }
    }

    @Test
    public void testEach() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
            List<Long> result = new ArrayList<>();

            list.forEach(4, 1, result::add);

            assertEquals(3, result.size());
            assertEquals(5L, result.get(0));
            assertEquals(4L, result.get(1));
            assertEquals(3L, result.get(2));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            final long[] sum = { 0 };
            list.forEach(1, 3, x -> sum[0] += x);
            assertEquals(5L, sum[0]);
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            final long[] sum = { 0 };
            list.forEach(x -> sum[0] += x);
            assertEquals(6L, sum[0]);
        }
    }

    @Test
    public void testEach_Empty() {
        List<Long> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEach_Null() {
        // Empty lists do not evaluate callbacks; non-empty lists fail naturally when invoking them.
        final LongList empty = new LongList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.forEach((com.landawn.abacus.util.function.LongConsumer) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.removeIf((com.landawn.abacus.util.function.LongPredicate) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceIf((com.landawn.abacus.util.function.LongPredicate) null, 0L));

        final LongList nonEmpty = LongList.of(1L, 2L);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.forEach((com.landawn.abacus.util.function.LongConsumer) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.removeIf((com.landawn.abacus.util.function.LongPredicate) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceIf((com.landawn.abacus.util.function.LongPredicate) null, 0L));
    }

    @Test
    public void testFirst() {
        {
            list = new LongList();
            LongList list = LongList.of(10L, 20L, 30L);
            assertEquals(10L, list.first().getAsLong());
            assertEquals(30L, list.last().getAsLong());
            assertTrue(new LongList().first().isEmpty());
            assertTrue(new LongList().last().isEmpty());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            OptionalLong first = list.first();
            assertTrue(first.isPresent());
            assertEquals(1L, first.get());
        }
    }

    @Test
    public void testFirst_Empty() {
        OptionalLong first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.add(1L);
        list.add(2L);
        OptionalLong last = list.last();
        assertTrue(last.isPresent());
        assertEquals(2L, last.get());
    }

    @Test
    public void testLast_Empty() {
        OptionalLong last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(2L);
            list.add(3L);
            list.add(3L);
            LongList distinct = list.distinct(1, 5);
            assertEquals(2, distinct.size());
            assertTrue(distinct.contains(2L));
            assertTrue(distinct.contains(3L));
        }
        {
            list = new LongList();
            LongList original = LongList.of(3L, 1L, 2L, 1L, 3L, 2L, 4L);
            LongList distinct = original.distinct();
            assertEquals(4, distinct.size());
            assertTrue(distinct.contains(1L));
            assertTrue(distinct.contains(2L));
            assertTrue(distinct.contains(3L));
            assertTrue(distinct.contains(4L));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 2L, 3L, 3L, 3L });

            LongList distinct = list.distinct(0, list.size());
            assertEquals(3, distinct.size());
            assertEquals(1L, distinct.get(0));
            assertEquals(2L, distinct.get(1));
            assertEquals(3L, distinct.get(2));
        }
    }

    @Test
    public void testDistinct_Empty() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 2L, 3L });
            LongList result = list.distinct(1, 1);
            assertTrue(result.isEmpty());
        }
        {
            list = new LongList();
            LongList distinct = list.distinct();
            assertTrue(distinct.isEmpty());
        }
    }

    @Test
    public void testContainsDuplicates() {
        {
            list = new LongList();
            LongList list1 = LongList.of(1L, 2L, 1L);
            assertTrue(list1.containsDuplicates());

            LongList list2 = LongList.of(1L, 2L, 3L);
            assertFalse(list2.containsDuplicates());

            assertFalse(new LongList().containsDuplicates());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            assertFalse(list.containsDuplicates());
            list.add(1L);
            assertTrue(list.containsDuplicates());
        }
    }

    @Test
    public void testIsSorted() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        assertTrue(list.isSorted());
    }

    @Test
    public void testSort() {
        {
            list = new LongList();
            LongList list = LongList.of(3L, 1L, 4L, 1L, 5L, 9L);
            list.sort();
            assertArrayEquals(new long[] { 1L, 1L, 3L, 4L, 5L, 9L }, list.toArray());
            list.reverseSort();
            assertArrayEquals(new long[] { 9L, 5L, 4L, 3L, 1L, 1L }, list.toArray());
        }
        {
            list = new LongList();
            list.add(3L);
            list.add(1L);
            list.add(2L);
            list.sort();
            assertEquals(1L, list.get(0));
            assertEquals(2L, list.get(1));
            assertEquals(3L, list.get(2));
        }
    }

    @Test
    public void testParallelSort() {
        {
            list = new LongList();
            list.addAll(new long[] { 3L, 1L, 2L });
            list.parallelSort();
            assertTrue(list.isSorted());
        }
        {
            list = new LongList();
            list.add(3L);
            list.add(1L);
            list.add(2L);
            list.parallelSort();
            assertEquals(1L, list.get(0));
            assertEquals(2L, list.get(1));
            assertEquals(3L, list.get(2));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(5L, 3L, 1L, 4L, 2L);
            ll.parallelSort();
            assertEquals(1L, ll.get(0));
            assertEquals(5L, ll.get(4));
        }
    }

    @Test
    public void testReverseSort() {
        {
            list = new LongList();
            list.add(3L);
            list.add(1L);
            list.add(2L);
            list.reverseSort();
            assertEquals(3L, list.get(0));
            assertEquals(2L, list.get(1));
            assertEquals(1L, list.get(2));
        }
        {
            list = new LongList();
            LongList ll = LongList.of(3L, 1L, 4L, 1L, 5L);
            ll.reverseSort();
            assertEquals(5L, ll.get(0));
            assertEquals(1L, ll.get(4));
        }
    }

    @Test
    public void testBinarySearch() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            assertEquals(2, list.binarySearch(1, 4, 3L));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            assertEquals(2, list.binarySearch(3L));
            assertTrue(list.binarySearch(5L) < 0);
        }
        {
            list = new LongList();
            list.addAll(new long[] { 3L, 1L, 4L, 1L, 5L });
            int result = list.binarySearch(3L);
            assertNotNull(result);
        }
    }

    @Test
    public void testReverse() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.reverse(1, 3);
            assertEquals(1L, list.get(0));
            assertEquals(3L, list.get(1));
            assertEquals(2L, list.get(2));
            assertEquals(4L, list.get(3));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.reverse();
            assertEquals(3L, list.get(0));
            assertEquals(2L, list.get(1));
            assertEquals(1L, list.get(2));
        }
    }

    @Test
    public void testReverse_Empty() {
        list.addAll(new long[] { 1L, 2L, 3L });
        list.reverse(1, 1);
        assertEquals(1L, list.get(0));
        assertEquals(2L, list.get(1));
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testRotate() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
            list.rotate(-2);
            assertEquals(3L, list.get(0));
            assertEquals(4L, list.get(1));
            assertEquals(5L, list.get(2));
            assertEquals(1L, list.get(3));
            assertEquals(2L, list.get(4));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.rotate(1);
            assertEquals(4L, list.get(0));
            assertEquals(1L, list.get(1));
        }
    }

    @Test
    public void testShuffle() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            java.util.Random rnd = new java.util.Random(12345);
            list.shuffle(rnd);
            assertEquals(3, list.size());
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L, 4L, 5L);
            Random rnd = new Random(42);
            ll.shuffle(rnd);
            assertEquals(5, ll.size());
        }
        {
            list = new LongList();
            list.add(5L);
            list.shuffle();
            assertEquals(1, list.size());
            assertEquals(5L, list.get(0));
        }
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new LongList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> LongList.of(1L).shuffle(null));
    }

    @Test
    public void testSwap() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.swap(0, 2);
            assertEquals(3L, list.get(0));
            assertEquals(1L, list.get(2));
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            list.swap(1, 1);
            assertEquals(2L, list.get(1));
        }
    }

    @Test
    public void testSwap_OutOfBounds() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopy() {
        {
            list = new LongList();
            LongList list = LongList.of(0L, 1L, 2L, 3L, 4L, 5L, 6L);
            LongList copy = list.copy(1, 5);
            assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, copy.toArray());

            List<LongList> chunks = list.split(3);
            assertEquals(3, chunks.size());
            assertArrayEquals(new long[] { 0L, 1L, 2L }, chunks.get(0).toArray());
            assertArrayEquals(new long[] { 3L, 4L, 5L }, chunks.get(1).toArray());
            assertArrayEquals(new long[] { 6L }, chunks.get(2).toArray());
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
            LongList copy = list.copy(0, 5, 3);
            assertEquals(2, copy.size());
            assertEquals(1L, copy.get(0));
            assertEquals(4L, copy.get(1));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.add(5L);
            LongList copy = list.copy(0, 5, 2);
            assertEquals(3, copy.size());
            assertEquals(1L, copy.get(0));
            assertEquals(3L, copy.get(1));
            assertEquals(5L, copy.get(2));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            LongList copy = list.copy();
            assertEquals(list.size(), copy.size());
            copy.set(0, 99L);
            assertEquals(1L, list.get(0));
        }
    }

    @Test
    public void testCopy_Empty() {
        LongList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopy_NegativeStep() {
        list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
        LongList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5L, copy.get(0));
        assertEquals(4L, copy.get(1));
        assertEquals(3L, copy.get(2));
        assertEquals(2L, copy.get(3));
    }

    @Test
    public void testCopy_InvalidRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testDescending() {
        final LongList backed = new LongList(new long[] { 1L, 2L, 3L, 99L, 98L }, 3);
        assertEquals(LongList.of(3L, 2L, 1L), backed.copy(3, -1, -1));
        assertEquals(LongList.of(3L, 1L), backed.copy(3, -1, -2));

        final LongList padded = new LongList(10);
        padded.addAll(new long[] { 1L, 2L, 3L });
        assertEquals(LongList.of(3L, 2L, 1L), padded.copy(3, -1, -1));
    }

    @Test
    public void testSplit() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            list.add(5L);
            List<LongList> chunks = list.split(0, 5, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(1, chunks.get(2).size());
        }
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L, 4L, 5L });
            List<LongList> chunks = list.split(0, 5, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(1, chunks.get(2).size());
        }
    }

    @Test
    public void testSplit_Empty() {
        List<LongList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testTrim() {
        LongList list = new LongList(10);
        list.add(1L);
        list.add(2L);
        assertEquals(10, list.internalArray().length);
        list.trimToSize();
        assertEquals(2, list.internalArray().length);
        list.clear();
        assertEquals(0, list.size());
    }

    @Test
    public void testClear() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.clear();
            assertTrue(list.isEmpty());
            assertEquals(0, list.size());
        }
        {
            list = new LongList();
            LongList ll = LongList.of(1L, 2L, 3L);
            ll.clear();
            assertEquals(0, ll.size());
            assertTrue(ll.isEmpty());
        }
    }

    @Test
    public void testIsEmpty_Empty() {
        assertTrue(list.isEmpty());
        list.add(1L);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testList() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            List<Long> boxedList = list.toList(1, 3);
            assertEquals(2, boxedList.size());
            assertEquals(Long.valueOf(2L), boxedList.get(0));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            List<Long> boxedList = list.toList();
            assertEquals(2, boxedList.size());
            assertEquals(Long.valueOf(1L), boxedList.get(0));
        }
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());
        list.add(1L);
        assertEquals(1, list.size());
    }

    @Test
    public void testBoxed() {
        {
            list = new LongList();
            long[] a = { 1L, 2L, 3L };
            LongList list = LongList.of(a);
            List<Long> boxed = list.boxed();
            assertEquals(Long.valueOf(1L), boxed.get(0));
            assertEquals(Arrays.asList(1L, 2L, 3L), boxed);

            long[] toArray = list.toArray();
            assertArrayEquals(a, toArray);
            assertNotSame(a, toArray);
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            List<Long> boxed = list.boxed(1, 3);
            assertEquals(2, boxed.size());
            assertEquals(Long.valueOf(2L), boxed.get(0));
        }
    }

    @Test
    public void testBoxed_Empty() {
        List<Long> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxed_InvalidRange() {
        list.addAll(new long[] { 1L, 2L, 3L });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    @Test
    public void testFloat() {
        list.add(1L);
        list.add(2L);
        FloatList floatList = list.toFloatList();
        assertEquals(2, floatList.size());
        assertEquals(1.0f, floatList.get(0));
    }

    @Test
    public void testConversions() {
        long preciseLong = 9223372036854775807L;
        list.add(preciseLong);

        FloatList floatList = list.toFloatList();
        assertEquals(preciseLong, (long) floatList.get(0));

        DoubleList doubleList = list.toDoubleList();
        assertEquals(preciseLong, (long) doubleList.get(0));
    }

    @Test
    public void testDouble() {
        list.add(1L);
        list.add(2L);
        DoubleList doubleList = list.toDoubleList();
        assertEquals(2, doubleList.size());
        assertEquals(1.0, doubleList.get(0));
    }

    @Test
    public void testCollection() {
        {
            list = new LongList();
            LongList original = LongList.of(10L, 20L, 30L);
            ArrayList<Long> result = original.toCollection(ArrayList::new);
            assertEquals(3, result.size());
            assertEquals(Long.valueOf(10L), result.get(0));
            assertEquals(Long.valueOf(20L), result.get(1));
            assertEquals(Long.valueOf(30L), result.get(2));
        }
        {
            list = new LongList();
            LongList list = LongList.of(1L, 2L, 3L, 4L, 5L);
            ArrayList<Long> result = list.toCollection(1, 4, ArrayList::new);
            assertEquals(Arrays.asList(2L, 3L, 4L), result);
        }
    }

    @Test
    public void testCollection_Empty() {
        ArrayList<Long> result = list.toCollection(ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMultiset() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(1L);
            list.add(3L);
            Multiset<Long> multiset = list.toMultiset(0, 4, n -> new Multiset<>());
            assertEquals(4, multiset.size());
            assertEquals(3, multiset.countOfDistinctElements());
            assertEquals(2, multiset.getCount(1L));
        }
        {
            list = new LongList();
            LongList original = LongList.of(1L, 2L, 2L, 3L);
            Multiset<Long> multiset = original.toMultiset(Multiset::new);
            assertEquals(1, multiset.getCount(Long.valueOf(1L)));
            assertEquals(2, multiset.getCount(Long.valueOf(2L)));
            assertEquals(1, multiset.getCount(Long.valueOf(3L)));
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(1L);
            Multiset<Long> multiset = list.toMultiset();
            assertEquals(3, multiset.size());
            assertEquals(2, multiset.countOfDistinctElements());
            assertEquals(2, multiset.getCount(1L));
        }
        {
            list = new LongList();
            LongList original = LongList.of(1L, 2L, 2L, 3L, 3L, 3L);
            Multiset<Long> multiset = original.toMultiset(1, 5);
            assertEquals(2, multiset.getCount(Long.valueOf(2L)));
            assertEquals(2, multiset.getCount(Long.valueOf(3L)));
        }
    }

    @Test
    public void testMultiset_Empty() {
        {
            list = new LongList();
            LongList original = LongList.of(1L, 2L, 3L);
            Multiset<Long> multiset = original.toMultiset(1, 1);
            assertEquals(0, multiset.size());
        }
        {
            list = new LongList();
            Multiset<Long> multiset = list.toMultiset(Multiset::new);
            assertEquals(0, multiset.size());
        }
    }

    @Test
    public void testIterator() {
        list.add(1L);
        list.add(2L);
        list.add(3L);
        LongIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(2L, iter.nextLong());
        assertTrue(iter.hasNext());
        assertEquals(3L, iter.nextLong());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_Empty() {
        LongIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextLong());
    }

    @Test
    public void testStream() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            list.add(4L);
            LongStream stream = list.stream(1, 3);
            assertEquals(5L, stream.sum());
        }
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            list.add(3L);
            LongStream stream = list.stream();
            assertEquals(6L, stream.sum());
        }
    }

    @Test
    public void testStream_Empty() {
        {
            list = new LongList();
            list.addAll(new long[] { 1L, 2L, 3L });
            LongStream stream = list.stream(1, 1);
            assertEquals(0, stream.count());
        }
        {
            list = new LongList();
            LongStream stream = list.stream();
            assertEquals(0, stream.count());
        }
    }

    @Test
    public void testGetFirst() {
        {
            list = new LongList();
            LongList list = LongList.of(10L, 20L, 30L);
            assertEquals(10L, list.getFirst());
            assertEquals(30L, list.getLast());
            assertThrows(NoSuchElementException.class, () -> new LongList().getFirst());
            assertThrows(NoSuchElementException.class, () -> new LongList().getLast());
        }
        {
            list = new LongList();
            list.add(5L);
            assertEquals(5L, list.getFirst());
            assertEquals(5L, list.getLast());
        }
    }

    @Test
    public void testGetFirst_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.add(1L);
        list.add(2L);
        assertEquals(2L, list.getLast());
    }

    @Test
    public void testGetLast_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testAddFirst() {
        list.add(2L);
        list.add(3L);
        list.addFirst(1L);
        assertEquals(3, list.size());
        assertEquals(1L, list.get(0));
    }

    @Test
    public void testAddLast() {
        list.add(1L);
        list.add(2L);
        list.addLast(3L);
        assertEquals(3, list.size());
        assertEquals(3L, list.get(2));
    }

    @Test
    public void testRemoveFirst() {
        list.add(5L);
        assertEquals(5L, list.removeFirst());
        assertTrue(list.isEmpty());

        list.add(10L);
        assertEquals(10L, list.removeLast());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRemoveFirst_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        list.add(1L);
        list.add(2L);
        long removed = list.removeLast();
        assertEquals(2L, removed);
        assertEquals(1, list.size());
    }

    @Test
    public void testRemoveLast_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testHashCode() {
        list.add(1L);
        list.add(2L);
        LongList other = LongList.of(1L, 2L);
        assertEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals() {
        {
            list = new LongList();
            list.add(1L);
            list.add(2L);
            LongList other = LongList.of(1L, 2L);
            assertTrue(list.equals(other));
        }
        {
            list = new LongList();
            LongList list1 = LongList.of(1L, 2L, 3L);
            LongList list2 = LongList.of(1L, 2L, 3L);
            LongList list3 = LongList.of(3L, 2L, 1L);

            assertEquals(list1, list2);
            assertEquals(list1.hashCode(), list2.hashCode());

            assertNotEquals(list1, list3);
            assertNotEquals(list1.hashCode(), list3.hashCode());

            assertNotEquals(list1, null);
            assertNotEquals(list1, new Object());
        }
    }

    @Test
    public void testString() {
        list.add(Long.MIN_VALUE);
        list.add(0L);
        list.add(Long.MAX_VALUE);
        String str = list.toString();
        assertTrue(str.contains(String.valueOf(Long.MIN_VALUE)));
        assertTrue(str.contains("0"));
        assertTrue(str.contains(String.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void testEnsureCapacity_LargeData() {
        // Test adding many elements beyond initial capacity
        LongList ll = new LongList(2);
        for (int i = 0; i < 100; i++) {
            ll.add(i);
        }
        assertEquals(100, ll.size());
        assertEquals(0L, ll.get(0));
        assertEquals(99L, ll.get(99));
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
        // LongList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final LongList withSpareCapacity = LongList.of(new long[] { 1, 2, 3, 4, 5 }, 3);

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
            final LongList self = LongList.of(1L, 2L, 3L);
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final LongList expected = LongList.of(1L, 2L, 3L);
            final LongList inserted = LongList.of(1L, 2L, 3L);
            expected.addAll(index, LongList.of(1L, 2L, 3L));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final LongList appended = LongList.of(1L, 2L, 3L);
        appended.addAll(appended);
        assertEquals("[1, 2, 3, 1, 2, 3]", appended.toString());

        // The interesting middle case, spelled out.
        final LongList middle = LongList.of(1L, 2L, 3L);
        middle.addAll(1, middle);
        assertEquals("[1, 1, 2, 3, 2, 3]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final LongList a = LongList.of(1L, 2L, 3L);
        assertFalse(a.removeAll((LongList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((long[]) null));
        assertFalse(a.removeAll(new long[0]));
        assertFalse(a.removeAll(new LongList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final LongList b = LongList.of(1L, 2L, 3L);
        assertTrue(b.retainAll((LongList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final LongList c = LongList.of(1L, 2L, 3L);
        assertTrue(c.retainAll((long[]) null));
        assertEquals(0, c.size());

        final LongList d = LongList.of(1L, 2L, 3L);
        assertTrue(d.retainAll(new long[0]));
        assertEquals(0, d.size());

        final LongList e = LongList.of(1L, 2L, 3L);
        assertTrue(e.retainAll(new LongList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final LongList empty = new LongList();
        assertFalse(empty.retainAll((LongList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final LongList f = LongList.of(1L, 2L, 3L);
        assertTrue(f.retainAll(LongList.of(1L)));
        assertEquals(1, f.size());
    }

    @Test
    public void reviewFixes20260911_streamSumAccumulatesInALongAndWrapsSilently() {
        // LongList has no sum() of its own; both javadoc examples route through LongStream.sum(), which
        // accumulates in a long and WRAPS, whereas the byte-identical IntList shape throws instead. The
        // examples now say so.
        assertEquals(12L, LongList.of(1L, 2L, 3L, 4L, 5L).stream().filter(x -> x > 2).sum());
        assertEquals(12L, LongList.of(1L, 2L, 3L, 4L, 5L, 6L).stream(2, 5).sum());

        final LongList overflowing = LongList.of(Long.MAX_VALUE, 1L);
        assertEquals(Long.MIN_VALUE, overflowing.stream().sum()); // ArrayLongStream path
        assertEquals(Long.MIN_VALUE, overflowing.stream().filter(x -> true).sum()); // IteratorLongStream path
        assertEquals(Long.MIN_VALUE, LongList.of(0L, Long.MAX_VALUE, 1L).stream(1, 3).sum());

        // The escape hatch the javadoc points at is exact.
        assertEquals("9223372036854775808", N.sumToBigInteger(overflowing.toArray()).toString());

        // ... and the contrast it draws with IntStream#sum() is real.
        assertThrows(ArithmeticException.class, () -> IntList.of(Integer.MAX_VALUE, 1).stream().sum());
    }

    @Test
    public void reviewFixes20260911_descendingForEachClampsTheStartToTheLastElement() {
        // The reverse form accepts fromIndex == size() and starts at size() - 1. The javadoc claimed the
        // iteration "starts from fromIndex", which is wrong for exactly that value; copy(int,int,int) two
        // hundred lines away already documented the same clamp.
        final LongList source = LongList.of(10L, 20L, 30L, 40L, 50L);

        final LongList fromSize = new LongList();
        source.forEach(source.size(), -1, fromSize::add);
        assertEquals("[50, 40, 30, 20, 10]", fromSize.toString());

        final LongList fromLast = new LongList();
        source.forEach(source.size() - 1, -1, fromLast::add);
        assertEquals(fromSize.toString(), fromLast.toString());

        // The clamp is not special to toIndex == -1.
        final LongList clampedMidRange = new LongList();
        source.forEach(source.size(), 1, clampedMidRange::add);
        assertEquals("[50, 40, 30]", clampedMidRange.toString());

        // One past size() is still rejected.
        assertThrows(IndexOutOfBoundsException.class, () -> source.forEach(source.size() + 1, -1, x -> {
        }));
    }
}
