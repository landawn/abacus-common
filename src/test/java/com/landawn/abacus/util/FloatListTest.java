package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.stream.FloatStream;

public class FloatListTest extends FloatListTestSupport {

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> list.forEach(0, 0, (com.landawn.abacus.util.function.FloatConsumer) null));
    }

    @Test
    public void testConstructors() {
        {
            list = new FloatList();
            FloatList list = new FloatList(0);
            assertTrue(list.isEmpty());
            list.add(1.0f);
            assertEquals(1, list.size());
        }
        {
            list = new FloatList();
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
            FloatList list = new FloatList(arr, 3);
            assertEquals(3, list.size());
        }
        {
            list = new FloatList();
            float[] arr = { 1.0f, 2.0f, 3.0f };
            FloatList list = new FloatList(arr);
            assertEquals(3, list.size());
        }
        {
            list = new FloatList();
            FloatList list = new FloatList();
            assertTrue(list.isEmpty());
            assertEquals(0, list.size());
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new FloatList(-1));
    }

    @Test
    public void testConstructors_Null() {
        {
            list = new FloatList();
            assertThrows(IllegalArgumentException.class, () -> new FloatList(null, 0));
        }
        {
            list = new FloatList();
            assertThrows(IllegalArgumentException.class, () -> new FloatList(null));
        }
    }

    @Test
    public void testConstructors_InvalidSize() {
        float[] array = { 1.1f, 2.2f, 3.3f };
        assertThrows(IndexOutOfBoundsException.class, () -> new FloatList(array, 4));
    }

    @Test
    public void testLarge_LargeData() {
        FloatList list = new FloatList();
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }
        assertEquals(1000, list.size());
        assertEquals(999.0f, list.get(999), 0.0001f);
    }

    @Test
    public void testBatch_LargeData() {
        int size = 1000;
        FloatList list1 = new FloatList();
        FloatList list2 = new FloatList();

        for (int i = 0; i < size; i++) {
            list1.add(i * 0.1f);
        }

        for (int i = size / 2; i < size + size / 2; i++) {
            list2.add(i * 0.1f);
        }

        FloatList intersection = list1.intersection(list2);
        assertEquals(size / 2, intersection.size());

        FloatList difference = list1.difference(list2);
        assertEquals(size / 2, difference.size());

        FloatList symDiff = list1.symmetricDifference(list2);
        assertEquals(size, symDiff.size());
    }

    @Test
    public void testRemoveAll() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
            FloatList list2 = FloatList.of(2.0f, 4.0f);
            assertTrue(list1.removeAll(list2));
            assertEquals(2, list1.size());
            assertEquals(1.0f, list1.get(0), 0.0001f);
            assertEquals(3.0f, list1.get(1), 0.0001f);
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
            float[] toRemove = { 2.2f, 4.4f };

            boolean result = list.removeAll(toRemove);
            assertTrue(result);
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveAll_LargeData() {
        FloatList fl = FloatList.of(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f, 17f, 18f, 19f, 20f);
        FloatList toRemove = FloatList.of(2f, 4f, 6f, 8f, 10f);
        assertTrue(fl.removeAll(toRemove));
        assertEquals(15, fl.size());
        assertFalse(fl.contains(2.0f));
    }

    @Test
    public void testRemoveAll_Empty() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
            assertFalse(fl.removeAll(FloatList.of()));
            assertEquals(3, fl.size());
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
            assertFalse(fl.removeAll(new float[0]));
            assertEquals(3, fl.size());
        }
    }

    @Test
    public void testRemoveAll_SharedBackingArray() {
        final float[] shared = { 1, 2, 3, 1 };
        final FloatList values = FloatList.of(shared);
        final FloatList removed = FloatList.of(shared, 2);

        assertTrue(values.removeAll(removed));
        assertArrayEquals(new float[] { 3 }, values.toArray());
    }

    @Test
    public void testFrequency() {
        FloatList list = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 1.0f);
        assertEquals(3, list.frequency(1.0f));
        assertEquals(1, list.frequency(2.0f));
        assertEquals(0, list.frequency(99.0f));
    }

    @Test
    public void testFrequency_Empty() {
        FloatList list = new FloatList();
        assertEquals(0, list.frequency(1.0f));
    }

    @Test
    public void testReplaceRange() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            FloatList replacement = FloatList.of(10.0f, 20.0f);
            list.replaceRange(1, 4, replacement);
            assertEquals(4, list.size());
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(10.0f, list.get(1), 0.0001f);
            assertEquals(20.0f, list.get(2), 0.0001f);
            assertEquals(5.0f, list.get(3), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
            float[] replacement = { 10.0f, 20.0f, 30.0f };
            list.replaceRange(1, 3, replacement);
            assertEquals(5, list.size());
            assertEquals(10.0f, list.get(1), 0.0001f);
            assertEquals(20.0f, list.get(2), 0.0001f);
            assertEquals(30.0f, list.get(3), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            fl.replaceRange(1, 4, new float[] { 20.0f, 30.0f });
            assertEquals(4, fl.size());
            assertEquals(20.0f, fl.get(1), 0.0001f);
            assertEquals(30.0f, fl.get(2), 0.0001f);
            assertEquals(5.0f, fl.get(3), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            fl.replaceRange(1, 3, FloatList.of(20.0f, 30.0f, 40.0f));
            assertEquals(6, fl.size());
            assertEquals(20.0f, fl.get(1), 0.0001f);
        }
    }

    @Test
    public void testReplaceRange_Empty() {
        FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        fl.replaceRange(1, 3, FloatList.of());
        assertEquals(3, fl.size());
        assertEquals(1.0f, fl.get(0), 0.0001f);
        assertEquals(4.0f, fl.get(1), 0.0001f);
        assertEquals(5.0f, fl.get(2), 0.0001f);
    }

    @Test
    public void testReplaceRange_Null() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.replaceRange(1, 2, (FloatList) null);
        assertEquals(2, list.size());
    }

    @Test
    public void testOf() {
        {
            list = new FloatList();
            float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f };
            FloatList list = FloatList.of(arr, 3);
            assertEquals(3, list.size());
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(3.0f, list.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of();
            assertTrue(list.isEmpty());
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            assertEquals(3, list.size());
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(2.0f, list.get(1), 0.0001f);
            assertEquals(3.0f, list.get(2), 0.0001f);
        }
    }

    @Test
    public void testOf_Empty() {
        FloatList list = FloatList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testOf_Null() {
        {
            list = new FloatList();
            FloatList list = FloatList.of((float[]) null);
            assertEquals(0, list.size());
        }
        {
            list = new FloatList();
            assertThrows(IndexOutOfBoundsException.class, () -> FloatList.of(null, 5));
        }
    }

    @Test
    public void testOf_InvalidSize() {
        float[] arr = { 1.0f, 2.0f };
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.of(arr, 5));
    }

    @Test
    public void testRemoveAt() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(10.0f, 20.0f, 30.0f, 40.0f, 50.0f);
            fl.removeAllAt(new int[] { 1, 3 });
            assertEquals(3, fl.size());
            assertEquals(10.0f, fl.get(0), 0.0001f);
            assertEquals(30.0f, fl.get(1), 0.0001f);
            assertEquals(50.0f, fl.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            list.removeAllAt(1, 3);
            assertEquals(3, list.size());
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(3.0f, list.get(1), 0.0001f);
            assertEquals(5.0f, list.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            float removed = list.removeAt(1);
            assertEquals(2.0f, removed, 0.0001f);
            assertEquals(2, list.size());
            assertEquals(3.0f, list.get(1), 0.0001f);
        }
    }

    @Test
    public void testRemoveAt_Empty() {
        {
            list = new FloatList();
            list.removeAllAt();
            assertTrue(list.isEmpty());

            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            list.removeAllAt();
            assertEquals(3, list.size());
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
            fl.removeAllAt(new int[] {});
            assertEquals(3, fl.size());
        }
    }

    @Test
    public void testRemoveAt_OutOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(5));
    }

    @Test
    public void testRemoveRange() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            list.removeRange(1, 4);
            assertEquals(2, list.size());
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(5.0f, list.get(1), 0.0001f);
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            list.removeRange(0, 3);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testRemoveRange_Empty() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testContainsDuplicates() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 1.0f);
        assertTrue(list1.containsDuplicates());

        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertFalse(list2.containsDuplicates());

        FloatList emptyList = new FloatList();
        assertFalse(emptyList.containsDuplicates());
    }

    @Test
    public void testNa_NaN() {
        FloatList list = FloatList.of(1.0f, Float.NaN, 3.0f);
        assertTrue(list.contains(Float.NaN));
        assertEquals(1, list.indexOf(Float.NaN));
    }

    @Test
    public void testArray() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            float[] arr = list.toArray();
            assertEquals(3, arr.length);
            assertEquals(1.0f, arr[0], 0.0001f);

            arr[0] = 99.0f;
            assertEquals(1.0f, list.get(0), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            float[] arr = list.internalArray();
            assertNotNull(arr);
        }
    }

    @Test
    public void testInfinity_Infinity() {
        FloatList list = FloatList.of(Float.POSITIVE_INFINITY, 1.0f, Float.NEGATIVE_INFINITY);
        assertTrue(list.contains(Float.POSITIVE_INFINITY));
        assertTrue(list.contains(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testObject() {
        FloatList list1 = FloatList.of(1.1f, 2.2f);
        FloatList list2 = FloatList.of(1.1f, 2.2f);
        FloatList list3 = FloatList.of(2.2f, 1.1f);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());
        assertNotEquals(list1.hashCode(), list3.hashCode());

        assertEquals("[1.1, 2.2]", list1.toString());
        assertEquals("[]", new FloatList().toString());
    }

    @Test
    public void testCopyOf() {
        float[] arr = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        FloatList list = FloatList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testCopyOf_Null() {
        {
            list = new FloatList();
            FloatList list = FloatList.copyOf((float[]) null);
            assertEquals(0, list.size());
        }
        {
            list = new FloatList();
            FloatList list = FloatList.copyOf(null);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testCopyOf_InvalidRange() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> FloatList.copyOf(array, 2, 10));
    }

    @Test
    public void testRepeat() {
        FloatList list = FloatList.repeat(5.5f, 4);
        assertEquals(4, list.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(5.5f, list.get(i), 0.0001f);
        }
    }

    @Test
    public void testRandom() {
        FloatList list = FloatList.random(5);
        assertEquals(5, list.size());
        for (int i = 0; i < 5; i++) {
            assertTrue(list.get(i) >= 0.0f && list.get(i) < 1.0f);
        }
    }

    @Test
    public void testGetSet() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float oldValue = list.set(1, 99.9f);
        assertEquals(2.0f, oldValue, 0.0001f);
        assertEquals(99.9f, list.get(1), 0.0001f);
    }

    @Test
    public void testGetSet_OutOfBounds() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f);
            assertThrows(IndexOutOfBoundsException.class, () -> list.set(5, 10.0f));
        }
        {
            list = new FloatList();
            list.add(10.5f);
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        }
    }

    @Test
    public void testAdd() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 3.0f, 4.0f);
            fl.add(1, 2.0f);
            assertEquals(4, fl.size());
            assertEquals(1.0f, fl.get(0), 0.0001f);
            assertEquals(2.0f, fl.get(1), 0.0001f);
            assertEquals(3.0f, fl.get(2), 0.0001f);
            assertEquals(4.0f, fl.get(3), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = new FloatList();
            list.add(1.0f);
            list.add(2.0f);
            assertEquals(2, list.size());
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(2.0f, list.get(1), 0.0001f);
        }
    }

    @Test
    public void testAdd_LargeData() {
        int count = 1000;
        for (int i = 0; i < count; i++) {
            list.add(i * 0.1f);
        }
        assertEquals(count, list.size());

        list.removeIf(x -> ((int) (x * 10)) % 2 == 0);
        assertEquals(count / 2, list.size());
    }

    @Test
    public void testAdd_OutOfBounds() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(5, 10.0f));
    }

    @Test
    public void testAddAll() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 4.0f, 5.0f);
            fl.addAll(1, FloatList.of(2.0f, 3.0f));
            assertEquals(5, fl.size());
            assertEquals(1.0f, fl.get(0), 0.0001f);
            assertEquals(2.0f, fl.get(1), 0.0001f);
            assertEquals(3.0f, fl.get(2), 0.0001f);
            assertEquals(4.0f, fl.get(3), 0.0001f);
            assertEquals(5.0f, fl.get(4), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 4.0f);
            boolean changed = fl.addAll(1, new float[] { 2.0f, 3.0f });
            assertTrue(changed);
            assertEquals(4, fl.size());
            assertEquals(1.0f, fl.get(0), 0.0001f);
            assertEquals(2.0f, fl.get(1), 0.0001f);
            assertEquals(3.0f, fl.get(2), 0.0001f);
            assertEquals(4.0f, fl.get(3), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 4.0f);
            FloatList list2 = FloatList.of(2.0f, 3.0f);
            assertTrue(list1.addAll(1, list2));
            assertEquals(4, list1.size());
            assertEquals(2.0f, list1.get(1), 0.0001f);
            assertEquals(3.0f, list1.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f);
            FloatList list2 = FloatList.of(3.0f, 4.0f);
            assertTrue(list1.addAll(list2));
            assertEquals(4, list1.size());
            assertEquals(3.0f, list1.get(2), 0.0001f);
        }
    }

    @Test
    public void testAddAll_Empty() {
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatList empty = new FloatList();

            assertFalse(list.addAll(0, empty));
            assertFalse(list.addAll(1, empty));
            assertFalse(list.addAll(list.size(), empty));
        }
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f);
            FloatList list2 = new FloatList();
            assertFalse(list1.addAll(list2));
            assertEquals(2, list1.size());
        }
        {
            list = new FloatList();
            list.add(1.1f);
            FloatList empty = new FloatList();
            boolean result = list.addAll(empty);

            assertFalse(result);
            assertEquals(1, list.size());
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f);
            boolean changed = fl.addAll(FloatList.of());
            assertFalse(changed);
            assertEquals(2, fl.size());
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f);
            boolean changed = fl.addAll(1, FloatList.of());
            assertFalse(changed);
            assertEquals(2, fl.size());
        }
    }

    @Test
    public void testAddAll_Null() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f);
            assertFalse(list.addAll((float[]) null));
            assertEquals(1, list.size());
        }
        {
            list = new FloatList();
            list.add(1.1f);
            assertFalse(list.addAll((float[]) null));
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

        FloatIterator iter = list.iterator();
        list.add(6.6f);

        assertTrue(iter.hasNext());
        iter.nextFloat();
    }

    @Test
    public void testRemove() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
            assertTrue(list.remove(2.0f));
            assertEquals(3, list.size());
            assertEquals(3.0f, list.get(1), 0.0001f);
        }
        {
            list = new FloatList();
            list.add(10.5f);
            boolean result = list.remove(20.5f);
            assertFalse(result);
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testEmpty() {
        assertFalse(list.remove(1.1f));
        assertFalse(list.removeAllOccurrences(1.1f));
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
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f, 4.0f, 2.0f);
        assertTrue(list.removeAllOccurrences(2.0f));
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(3.0f, list.get(1), 0.0001f);
        assertEquals(4.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRemoveIf() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            boolean changed = fl.removeIf(e -> e > 3.0f);
            assertTrue(changed);
            assertEquals(3, fl.size());
            assertEquals(1.0f, fl.get(0), 0.0001f);
            assertEquals(2.0f, fl.get(1), 0.0001f);
            assertEquals(3.0f, fl.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            assertFalse(list.removeIf(x -> x > 10.0f));
            assertEquals(3, list.size());
        }
    }

    @Test
    public void testRemoveDuplicates() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(3.0f, 1.0f, 2.0f, 1.0f, 3.0f, 4.0f);
            boolean changed = fl.removeDuplicates();
            assertTrue(changed);
            assertEquals(4, fl.size());
            assertTrue(fl.contains(1.0f));
            assertTrue(fl.contains(2.0f));
            assertTrue(fl.contains(3.0f));
            assertTrue(fl.contains(4.0f));
        }
        {
            list = new FloatList();
            list.add(1.1f);
            assertFalse(list.removeDuplicates());
            assertEquals(1, list.size());
        }
    }

    @Test
    public void testRemoveDuplicates_NaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, Float.NaN, 2.2f });
        assertTrue(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveDuplicates_Empty() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void testRetainAll() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
            FloatList list2 = FloatList.of(2.0f, 4.0f, 5.0f);
            assertTrue(list1.retainAll(list2));
            assertEquals(2, list1.size());
            assertEquals(2.0f, list1.get(0), 0.0001f);
            assertEquals(4.0f, list1.get(1), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
            fl.retainAll(new float[] { 2.0f, 4.0f });
            assertEquals(2, fl.size());
            assertEquals(2.0f, fl.get(0), 0.0001f);
            assertEquals(4.0f, fl.get(1), 0.0001f);
        }
    }

    @Test
    public void testRetainAll_Empty() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f);
            FloatList list2 = new FloatList();
            assertTrue(list1.retainAll(list2));
            assertEquals(0, list1.size());
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
            assertTrue(fl.retainAll(new float[0]));
            assertEquals(0, fl.size());
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            assertTrue(list.retainAll(new FloatList()));
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testMoveRange() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.moveRange(1, 3, 3);
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(4.0f, list.get(1), 0.0001f);
        assertEquals(5.0f, list.get(2), 0.0001f);
        assertEquals(2.0f, list.get(3), 0.0001f);
        assertEquals(3.0f, list.get(4), 0.0001f);
    }

    @Test
    public void testReplaceAll() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            list.replaceAll(f -> f * 2);
            assertEquals(2.0f, list.get(0), 0.0001f);
            assertEquals(4.0f, list.get(1), 0.0001f);
            assertEquals(6.0f, list.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 1.0f, 3.0f, 1.0f);
            int count = fl.replaceAll(1.0f, 9.0f);
            assertEquals(3, count);
            assertEquals(9.0f, fl.get(0), 0.0001f);
            assertEquals(2.0f, fl.get(1), 0.0001f);
            assertEquals(9.0f, fl.get(2), 0.0001f);
            assertEquals(3.0f, fl.get(3), 0.0001f);
            assertEquals(9.0f, fl.get(4), 0.0001f);
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            int count = list.replaceAll(5.5f, 10.0f);
            assertEquals(0, count);
        }
    }

    @Test
    public void testReplaceAll_Null() {
        FloatList nonEmpty = FloatList.of(1.0f, 2.0f, 3.0f);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceAll((com.landawn.abacus.util.function.FloatUnaryOperator) null));

        FloatList empty = new FloatList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceAll((com.landawn.abacus.util.function.FloatUnaryOperator) null));
    }

    @Test
    public void testReplaceAll_Empty() {
        FloatList list = new FloatList();
        int count = list.replaceAll(1.0f, 2.0f);
        assertEquals(0, count);
    }

    @Test
    public void testPrecision() {
        float a = 0.1f + 0.2f;
        float b = 0.3f;

        list.add(a);
        list.add(b);

        int replaced = list.replaceAll(0.3f, 1.0f);
        assertTrue(replaced > 0);
    }

    @Test
    public void testReplaceIf() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
            boolean changed = fl.replaceIf(e -> e % 2 == 0, 0.0f);
            assertTrue(changed);
            assertEquals(1.0f, fl.get(0), 0.0001f);
            assertEquals(0.0f, fl.get(1), 0.0001f);
            assertEquals(3.0f, fl.get(2), 0.0001f);
            assertEquals(0.0f, fl.get(3), 0.0001f);
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            boolean result = list.replaceIf(x -> false, 10.0f);
            assertFalse(result);
        }
    }

    @Test
    public void testFill() {
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            list.fill(Float.NaN);
            for (int i = 0; i < list.size(); i++) {
                assertTrue(Float.isNaN(list.get(i)));
            }
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            list.fill(1, 4, 99.0f);
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(99.0f, list.get(1), 0.0001f);
            assertEquals(99.0f, list.get(2), 0.0001f);
            assertEquals(99.0f, list.get(3), 0.0001f);
            assertEquals(5.0f, list.get(4), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            list.fill(9.0f);
            assertEquals(9.0f, list.get(0), 0.0001f);
            assertEquals(9.0f, list.get(1), 0.0001f);
            assertEquals(9.0f, list.get(2), 0.0001f);
        }
    }

    @Test
    public void testFill_Empty() {
        list.fill(10.0f);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testFill_InvalidRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10.0f));
    }

    @Test
    public void testContains() {
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f);
        assertTrue(list.contains(2.2f));
        assertFalse(list.contains(9.9f));

        assertTrue(list.containsAll(FloatList.of(1.1f, 3.3f)));
        assertFalse(list.containsAll(FloatList.of(1.1f, 4.4f)));

        assertTrue(list.containsAny(new float[] { 4.4f, 5.5f, 2.2f }));
        assertFalse(list.containsAny(new float[] { 4.4f, 5.5f, 6.6f }));
    }

    @Test
    public void testContains_NaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f });
        assertTrue(list.contains(Float.NaN));
    }

    @Test
    public void testContains_Empty() {
        assertFalse(list.contains(1.1f));
    }

    @Test
    public void testContains_Infinity() {
        list.addAll(new float[] { 1.1f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY });
        assertTrue(list.contains(Float.POSITIVE_INFINITY));
        assertTrue(list.contains(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testFloat() {
        float a = 0.1f;
        float b = 0.2f;
        float c = a + b;

        list.add(c);
        list.add(0.3f);

        assertTrue(list.contains(c));
        assertTrue(list.contains(0.3f));
    }

    @Test
    public void testContainsAny() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatList list2 = FloatList.of(3.0f, 4.0f);
            assertTrue(list1.containsAny(list2));

            FloatList list3 = FloatList.of(99.0f, 100.0f);
            assertFalse(list1.containsAny(list3));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
            FloatList other = FloatList.of(6.6f, 7.7f, 3.3f);

            assertTrue(list.containsAny(other));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
            float[] other = { 6.6f, 7.7f, 3.3f };

            assertTrue(list.containsAny(other));
        }
    }

    @Test
    public void testContainsAny_Empty() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f);
            FloatList list2 = new FloatList();
            assertFalse(list1.containsAny(list2));
        }
        {
            list = new FloatList();
            assertFalse(FloatList.of().containsAny(new float[] { 1.0f }));
        }
    }

    @Test
    public void testContainsAll() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
            assertTrue(fl.containsAll(FloatList.of(1.0f, 3.0f)));
            assertFalse(fl.containsAll(FloatList.of(1.0f, 4.0f)));
            assertTrue(fl.containsAll(FloatList.of()));
        }
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f);
            FloatList list2 = FloatList.of(2.0f, 3.0f);
            assertTrue(list1.containsAll(list2));

            FloatList list3 = FloatList.of(2.0f, 99.0f);
            assertFalse(list1.containsAll(list3));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
            FloatList other = FloatList.of(2.2f, 4.4f);

            assertTrue(list.containsAll(other));

            other = FloatList.of(2.2f, 6.6f);
            assertFalse(list.containsAll(other));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            float[] arr = { 2.0f, 3.0f };
            assertTrue(list.containsAll(arr));
        }
        {
            list = new FloatList();
            assertFalse(FloatList.of().containsAll(FloatList.of(1.0f)));
        }
    }

    @Test
    public void testContainsAll_Empty() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f);
            FloatList list2 = new FloatList();
            assertTrue(list1.containsAll(list2));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            assertTrue(list.containsAll(new FloatList()));
        }
    }

    @Test
    public void testDisjoint() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
            assertTrue(fl.disjoint(FloatList.of(4.0f, 5.0f)));
            assertFalse(fl.disjoint(FloatList.of(3.0f, 6.0f)));
            assertTrue(fl.disjoint(FloatList.of()));
        }
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatList list2 = FloatList.of(4.0f, 5.0f);
            assertTrue(list1.disjoint(list2));

            FloatList list3 = FloatList.of(3.0f, 4.0f);
            assertFalse(list1.disjoint(list3));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatList other = FloatList.of(4.4f, 5.5f, 6.6f);

            assertTrue(list.disjoint(other));

            other = FloatList.of(3.3f, 4.4f, 5.5f);
            assertFalse(list.disjoint(other));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            float[] arr1 = { 4.0f, 5.0f };
            assertTrue(list.disjoint(arr1));

            float[] arr2 = { 3.0f, 4.0f };
            assertFalse(list.disjoint(arr2));
        }
    }

    @Test
    public void testDisjoint_LargeData() {
        FloatList fl1 = FloatList.of(1f, 3f, 5f, 7f, 9f, 11f, 13f, 15f, 17f, 19f, 21f, 23f);
        FloatList fl2 = FloatList.of(2f, 4f, 6f, 8f, 10f, 12f, 14f, 16f, 18f, 20f, 22f, 24f);
        assertTrue(fl1.disjoint(fl2));
    }

    @Test
    public void testDisjoint_Empty() {
        assertTrue(FloatList.of().disjoint(new float[] { 1.0f }));
    }

    @Test
    public void testIntersection() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 2.0f, 3.0f, 4.0f);
            FloatList other = FloatList.of(2.0f, 4.0f, 5.0f);
            FloatList result = fl.intersection(other);
            assertTrue(result.contains(2.0f));
            assertTrue(result.contains(4.0f));
            assertFalse(result.contains(1.0f));
            assertFalse(result.contains(3.0f));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            float[] arr = { 2.0f, 3.0f, 4.0f };
            FloatList result = list.intersection(arr);
            assertEquals(2, result.size());
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatList result = list.intersection(FloatList.of(4.4f, 5.5f, 6.6f));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testIntersection_Empty() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f);
            FloatList list2 = new FloatList();
            FloatList result = list1.intersection(list2);
            assertEquals(0, result.size());
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatList result = list.intersection(new FloatList());
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testDifference() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
            FloatList list2 = FloatList.of(2.0f, 4.0f);
            FloatList result = list1.difference(list2);
            assertEquals(3, result.size());
            assertEquals(1.0f, result.get(0), 0.0001f);
            assertEquals(3.0f, result.get(1), 0.0001f);
            assertEquals(2.0f, result.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
            FloatList result = fl.difference(new float[] { 2.0f });
            assertEquals(3, result.size());
            assertEquals(1.0f, result.get(0), 0.0001f);
            assertEquals(3.0f, result.get(1), 0.0001f);
            assertEquals(2.0f, result.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatList result = list.difference(FloatList.of(1.1f, 2.2f, 3.3f));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testDifference_Empty() {
        FloatList list1 = FloatList.of(1.0f, 2.0f);
        FloatList list2 = new FloatList();
        FloatList result = list1.difference(list2);
        assertEquals(2, result.size());
    }

    @Test
    public void testSymmetricDifference() {
        {
            FloatList receiver = FloatList.of(1f, 9f);
            FloatList other = FloatList.of(1f, 2f, 1f);
            FloatList expected = FloatList.of(9f, 1f, 2f);
            assertEquals(expected, receiver.symmetricDifference(other));
            assertEquals(expected, receiver.symmetricDifference(other.toArray()));
            assertEquals(FloatList.of(1f, 9f), receiver);
            assertEquals(FloatList.of(1f, 2f, 1f), other);
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatList other = FloatList.of(3.3f, 4.4f, 5.5f);

            FloatList result = list.symmetricDifference(other);
            assertEquals(4, result.size());
            assertTrue(result.contains(1.1f));
            assertTrue(result.contains(2.2f));
            assertTrue(result.contains(4.4f));
            assertTrue(result.contains(5.5f));
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatList result = fl.symmetricDifference(new float[] { 2.0f, 4.0f });
            assertTrue(result.contains(1.0f));
            assertTrue(result.contains(3.0f));
            assertTrue(result.contains(4.0f));
            assertFalse(result.contains(2.0f));
        }
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatList list2 = FloatList.of(2.0f, 3.0f, 4.0f);
            FloatList result = list1.symmetricDifference(list2);
            assertTrue(result.contains(1.0f));
            assertTrue(result.contains(4.0f));
        }
    }

    @Test
    public void testSymmetricDifference_Empty() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f);
            FloatList list2 = new FloatList();
            FloatList result = list1.symmetricDifference(list2);
            assertEquals(2, result.size());
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatList result = list.symmetricDifference(new FloatList());
            assertEquals(3, result.size());
        }
    }

    @Test
    public void testIndexOf() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.1f, 2.2f, 1.1f, 3.3f, 2.2f);
            assertEquals(0, list.indexOf(1.1f));
            assertEquals(2, list.lastIndexOf(1.1f));
            assertEquals(4, list.lastIndexOf(2.2f));
            assertEquals(4, list.indexOf(2.2f, 2));
            assertEquals(-1, list.indexOf(9.9f));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

            assertEquals(3, list.indexOf(2.2f, 2));
            assertEquals(-1, list.indexOf(2.2f, 4));
        }
    }

    @Test
    public void testIndexOf_NaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, 2.2f, Float.NaN });
        assertEquals(1, list.indexOf(Float.NaN));
        assertEquals(3, list.indexOf(Float.NaN, 2));
    }

    @Test
    public void testIndexOf_OutOfBounds() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertEquals(-1, list.indexOf(1.1f, 10));
    }

    @Test
    public void testLastIndexOf() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
            assertEquals(1, list.lastIndexOf(2.0f, 2));
            assertEquals(-1, list.lastIndexOf(2.0f, -1));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 2.2f, 5.5f });

            assertEquals(1, list.lastIndexOf(2.2f, 2));
            assertEquals(-1, list.lastIndexOf(5.5f, 3));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
            assertEquals(3, list.lastIndexOf(2.0f));
            assertEquals(-1, list.lastIndexOf(99.0f));
        }
    }

    @Test
    public void testLastIndexOf_NaN() {
        list.addAll(new float[] { Float.NaN, 1.1f, Float.NaN, 2.2f });
        assertEquals(2, list.lastIndexOf(Float.NaN));
        assertEquals(0, list.lastIndexOf(Float.NaN, 1));
    }

    @Test
    public void testLastIndexOf_Empty() {
        {
            list = new FloatList();
            FloatList list = new FloatList();
            assertEquals(-1, list.lastIndexOf(1.0f, 0));
        }
        {
            list = new FloatList();
            assertEquals(-1, list.lastIndexOf(1.1f));
        }
    }

    @Test
    public void testMin() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(5.0f, 1.0f, 3.0f, 2.0f, 4.0f);
            OptionalFloat min = list.min(1, 4);
            assertTrue(min.isPresent());
            assertEquals(1.0f, min.get(), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(3.0f, 1.0f, 4.0f, 2.0f);
            OptionalFloat min = list.min();
            assertTrue(min.isPresent());
            assertEquals(1.0f, min.get(), 0.0001f);
        }
    }

    @Test
    public void testMin_Empty() {
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });

            assertFalse(list.min(1, 1).isPresent());
            assertFalse(list.max(1, 1).isPresent());
            assertFalse(list.lowerMedian(1, 1).isPresent());
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f);
            OptionalFloat min = list.min(1, 1);
            assertFalse(min.isPresent());
        }
        {
            list = new FloatList();
            FloatList list = new FloatList();
            OptionalFloat min = list.min();
            assertFalse(min.isPresent());
        }
    }

    @Test
    public void testMax() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 5.0f, 3.0f, 2.0f, 4.0f);
            OptionalFloat max = list.max(1, 4);
            assertTrue(max.isPresent());
            assertEquals(5.0f, max.get(), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(3.0f, 1.0f, 4.0f, 2.0f);
            OptionalFloat max = list.max();
            assertTrue(max.isPresent());
            assertEquals(4.0f, max.get(), 0.0001f);
        }
    }

    @Test
    public void testMax_Empty() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            OptionalFloat max = list.max(1, 1);
            assertFalse(max.isPresent());
        }
        {
            list = new FloatList();
            FloatList list = new FloatList();
            OptionalFloat max = list.max();
            assertFalse(max.isPresent());
        }
    }

    @Test
    public void testMedian() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(5.0f, 3.0f, 1.0f, 2.0f, 4.0f);
            OptionalFloat median = list.lowerMedian(1, 4);
            assertTrue(median.isPresent());
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(3.0f, 1.0f, 2.0f);
            OptionalFloat median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals(2.0f, median.get(), 0.0001f);
        }
    }

    @Test
    public void testMedian_Empty() {
        FloatList list = new FloatList();
        OptionalFloat median = list.lowerMedian();
        assertFalse(median.isPresent());
    }

    @Test
    public void testEach() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            List<Float> result = new ArrayList<>();
            list.forEach(1, 4, result::add);
            assertEquals(3, result.size());
            assertEquals(2.0f, result.get(0), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            List<Float> result = new ArrayList<>();
            list.forEach(result::add);
            assertEquals(3, result.size());
            assertEquals(1.0f, result.get(0), 0.0001f);
        }
    }

    @Test
    public void testEach_Empty() {
        List<Float> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEach_Null() {
        // Empty lists do not evaluate callbacks; non-empty lists fail naturally when invoking them.
        final FloatList empty = new FloatList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.forEach((com.landawn.abacus.util.function.FloatConsumer) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.removeIf((com.landawn.abacus.util.function.FloatPredicate) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceIf((com.landawn.abacus.util.function.FloatPredicate) null, 0f));

        final FloatList nonEmpty = FloatList.of(1f, 2f);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.forEach((com.landawn.abacus.util.function.FloatConsumer) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.removeIf((com.landawn.abacus.util.function.FloatPredicate) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceIf((com.landawn.abacus.util.function.FloatPredicate) null, 0f));
    }

    @Test
    public void testFirst() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        OptionalFloat first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1.0f, first.get(), 0.0001f);
    }

    @Test
    public void testFirst_Empty() {
        FloatList list = new FloatList();
        OptionalFloat first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        OptionalFloat last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3.0f, last.get(), 0.0001f);
    }

    @Test
    public void testLast_Empty() {
        FloatList list = new FloatList();
        OptionalFloat last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        FloatList list = FloatList.of(1.0f, 2.0f, 2.0f, 3.0f, 3.0f, 4.0f);
        FloatList result = list.distinct(1, 5);
        assertEquals(2, result.size());
        assertEquals(2.0f, result.get(0), 0.0001f);
        assertEquals(3.0f, result.get(1), 0.0001f);
    }

    @Test
    public void testDistinct_NaN() {
        list.addAll(new float[] { 1.1f, Float.NaN, Float.NaN, 2.2f });
        FloatList result = list.distinct(0, list.size());
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinct_Empty() {
        list.addAll(new float[] { 1.1f, 2.2f, 2.2f, 3.3f });
        FloatList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIsSorted() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertTrue(list1.isSorted());

        FloatList list2 = FloatList.of(3.0f, 1.0f, 2.0f);
        assertFalse(list2.isSorted());
    }

    @Test
    public void testSort() {
        FloatList list = FloatList.of(3.0f, 1.0f, 2.0f);
        list.sort();
        assertEquals(1.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testSort_Empty() {
        list.sort();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testParallelSort() {
        {
            list = new FloatList();
            list.addAll(new float[] { 3.3f, 1.1f, 2.2f });
            list.parallelSort();
            assertTrue(list.isSorted());
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(5.0f, 2.0f, 8.0f, 1.0f, 9.0f);
            list.parallelSort();
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(9.0f, list.get(4), 0.0001f);
        }
    }

    @Test
    public void testReverseSort() {
        FloatList list = FloatList.of(3.0f, 1.0f, 2.0f);
        list.reverseSort();
        assertEquals(3.0f, list.get(0), 0.0001f);
        assertEquals(2.0f, list.get(1), 0.0001f);
        assertEquals(1.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testBinarySearch() {
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

            assertEquals(3, list.binarySearch(1, 5, 4.4f));
            assertTrue(list.binarySearch(1, 3, 4.4f) < 0);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            assertEquals(2, list.binarySearch(0, 5, 3.0f));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            assertEquals(2, list.binarySearch(3.0f));
            assertTrue(list.binarySearch(2.5f) < 0);
        }
    }

    @Test
    public void testReverse() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            list.reverse(1, 4);
            assertEquals(1.0f, list.get(0), 0.0001f);
            assertEquals(4.0f, list.get(1), 0.0001f);
            assertEquals(3.0f, list.get(2), 0.0001f);
            assertEquals(2.0f, list.get(3), 0.0001f);
            assertEquals(5.0f, list.get(4), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            list.reverse();
            assertEquals(3.0f, list.get(0), 0.0001f);
            assertEquals(2.0f, list.get(1), 0.0001f);
            assertEquals(1.0f, list.get(2), 0.0001f);
        }
    }

    @Test
    public void testReverse_Empty() {
        list.reverse();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testRotate() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
        list.rotate(2);
        assertEquals(4.0f, list.get(0), 0.0001f);
        assertEquals(5.0f, list.get(1), 0.0001f);
        assertEquals(1.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testShuffle() {
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
            Random rnd = new Random(42);

            list.shuffle(rnd);
            assertEquals(5, list.size());
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            FloatList copy = list.copy();
            list.shuffle();
            assertEquals(5, list.size());
        }
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new FloatList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> FloatList.of(1.0f).shuffle(null));
    }

    @Test
    public void testShuffle_Empty() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSwap() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.swap(0, 2);
        assertEquals(3.0f, list.get(0), 0.0001f);
        assertEquals(1.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testSwap_OutOfBounds() {
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f);
            assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 5));
        }
    }

    @Test
    public void testCopy() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatList copy = list.copy();
            assertEquals(3, copy.size());
            assertNotSame(list, copy);

            list.set(0, 99.0f);
            assertEquals(1.0f, copy.get(0), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            FloatList copied = fl.copy(0, 5, 2);
            assertEquals(3, copied.size());
            assertEquals(1.0f, copied.get(0), 0.0001f);
            assertEquals(3.0f, copied.get(1), 0.0001f);
            assertEquals(5.0f, copied.get(2), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            FloatList copy = list.copy(1, 4);
            assertEquals(3, copy.size());
            assertEquals(2.0f, copy.get(0), 0.0001f);
            assertEquals(4.0f, copy.get(2), 0.0001f);
        }
    }

    @Test
    public void testCopy_Empty() {
        FloatList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopy_InvalidRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testSplit() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
            List<FloatList> chunks = fl.split(0, 6, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(1.0f, chunks.get(0).get(0), 0.0001f);
            assertEquals(2.0f, chunks.get(0).get(1), 0.0001f);
            assertEquals(2, chunks.get(1).size());
            assertEquals(2, chunks.get(2).size());
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });
            List<FloatList> chunks = list.split(0, 5, 2);
            assertEquals(3, chunks.size());
            assertEquals(2, chunks.get(0).size());
            assertEquals(2, chunks.get(1).size());
            assertEquals(1, chunks.get(2).size());
        }
    }

    @Test
    public void testSplit_Empty() {
        List<FloatList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testTrim() {
        FloatList list = new FloatList(100);
        list.add(1.0f);
        list.add(2.0f);
        FloatList result = list.trimToSize();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testClear() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testIsEmpty_Empty() {
        FloatList list = new FloatList();
        assertTrue(list.isEmpty());

        list.add(1.0f);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testSize() {
        FloatList list = new FloatList();
        assertEquals(0, list.size());

        list.add(1.0f);
        assertEquals(1, list.size());

        list.add(2.0f);
        assertEquals(2, list.size());
    }

    @Test
    public void testSubnormal() {
        float subnormal = Float.MIN_VALUE / 2;
        list.add(subnormal);
        list.add(0.0f);
        list.add(-subnormal);

        assertEquals(3, list.size());
        assertTrue(list.contains(subnormal));
        assertTrue(list.contains(-subnormal));
    }

    @Test
    public void testBoxed() {
        {
            list = new FloatList();
            list.addAll(new float[] { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY });
            List<Float> boxed = list.boxed();
            assertTrue(Float.isNaN(boxed.get(0)));
            assertEquals(Float.POSITIVE_INFINITY, boxed.get(1));
            assertEquals(Float.NEGATIVE_INFINITY, boxed.get(2));
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f });

            List<Float> boxed = list.boxed(1, 4);
            assertEquals(3, boxed.size());
            assertEquals(Float.valueOf(2.2f), boxed.get(0));
            assertEquals(Float.valueOf(4.4f), boxed.get(2));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            List<Float> boxed = list.boxed(1, 4);
            assertEquals(3, boxed.size());
            assertEquals(2.0f, boxed.get(0), 0.0001f);
        }
    }

    @Test
    public void testBoxed_Empty() {
        List<Float> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxed_InvalidRange() {
        list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    @Test
    public void testDouble() {
        FloatList list = FloatList.of(1.5f, 2.5f, 3.5f);
        DoubleList doubleList = list.toDoubleList();
        assertEquals(3, doubleList.size());
        assertEquals(1.5, doubleList.get(0), 0.0001);
    }

    @Test
    public void testCollection() {
        {
            list = new FloatList();
            FloatList fl = FloatList.of(10.0f, 20.0f, 30.0f, 40.0f);
            java.util.ArrayList<Float> col = fl.toCollection(1, 3, java.util.ArrayList::new);
            assertEquals(2, col.size());
            assertEquals(20.0f, col.get(0), 0.0001f);
            assertEquals(30.0f, col.get(1), 0.0001f);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            Set<Float> set = list.toCollection(1, 4, HashSet::new);
            assertEquals(3, set.size());
            assertTrue(set.contains(2.0f));
        }
    }

    @Test
    public void testMultiset() {
        FloatList list = FloatList.of(1.0f, 2.0f, 2.0f, 3.0f);
        Multiset<Float> multiset = list.toMultiset(0, 4, Multiset::new);
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(2.0f));
    }

    @Test
    public void testIterator() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1.0f, iter.next(), 0.0001f);
    }

    @Test
    public void testIterator_Empty() {
        {
            list = new FloatList();
            FloatIterator iter = list.iterator();
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, () -> iter.nextFloat());
        }
        {
            list = new FloatList();
            FloatList list = new FloatList();
            FloatIterator iter = list.iterator();
            assertFalse(iter.hasNext());
        }
    }

    @Test
    public void testStream() {
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            FloatStream stream = list.stream(1, 4);
            assertNotNull(stream);
            assertEquals(3, stream.count());
        }
        {
            list = new FloatList();
            list.addAll(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f });

            FloatStream stream = list.stream(1, 4);
            double sum = stream.sum();
            assertEquals(9.0, sum, 0.0001);
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatStream stream = list.stream();
            assertNotNull(stream);
            assertEquals(3, stream.count());
        }
    }

    @Test
    public void testStream_Empty() {
        {
            list = new FloatList();
            list.addAll(new float[] { 1.1f, 2.2f, 3.3f });
            FloatStream stream = list.stream(1, 1);
            assertEquals(0, stream.count());
        }
        {
            list = new FloatList();
            FloatStream stream = list.stream();
            assertEquals(0, stream.count());
        }
    }

    @Test
    public void testGetFirst() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(1.0f, list.getFirst(), 0.0001f);
    }

    @Test
    public void testGetFirst_Empty() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetFirst_OutOfBounds() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(3.0f, list.getLast(), 0.0001f);
    }

    @Test
    public void testGetLast_Empty() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testGetLast_OutOfBounds() {
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testAddFirst() {
        FloatList list = FloatList.of(2.0f, 3.0f);
        list.addFirst(1.0f);
        assertEquals(3, list.size());
        assertEquals(1.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testAddLast() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        list.addLast(3.0f);
        assertEquals(3, list.size());
        assertEquals(3.0f, list.get(2), 0.0001f);
    }

    @Test
    public void testRemoveFirst() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float removed = list.removeFirst();
        assertEquals(1.0f, removed, 0.0001f);
        assertEquals(2, list.size());
        assertEquals(2.0f, list.get(0), 0.0001f);
    }

    @Test
    public void testRemoveFirst_Empty() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveFirst_OutOfBounds() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveLast() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        float removed = list.removeLast();
        assertEquals(3.0f, removed, 0.0001f);
        assertEquals(2, list.size());
        assertEquals(2.0f, list.get(1), 0.0001f);
    }

    @Test
    public void testRemoveLast_Empty() {
        FloatList list = new FloatList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testRemoveLast_OutOfBounds() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testHashCode() {
        FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals() {
        {
            list = new FloatList();
            FloatList list1 = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatList list2 = FloatList.of(1.0f, 2.0f, 3.0f);
            FloatList list3 = FloatList.of(1.0f, 2.0f, 4.0f);

            assertTrue(list1.equals(list1));
            assertTrue(list1.equals(list2));
            assertFalse(list1.equals(list3));
            assertFalse(list1.equals(null));
            assertFalse(list1.equals("not a FloatList"));
        }
        {
            list = new FloatList();
            list.add(0.1f + 0.2f);

            FloatList other = new FloatList();
            other.add(0.3f);

            boolean equalsResult = list.equals(other);
            assertNotNull(equalsResult);
        }
    }

    @Test
    public void testEquals_Null() {
        FloatList fl = FloatList.of(1.0f);
        assertNotEquals(fl, null);
    }

    @Test
    public void testString() {
        {
            list = new FloatList();
            list.add(Float.NaN);
            list.add(Float.POSITIVE_INFINITY);
            list.add(Float.NEGATIVE_INFINITY);
            list.add(-0.0f);
            list.add(0.0f);

            String str = list.toString();
            assertTrue(str.contains("NaN"));
            assertTrue(str.contains("Infinity"));
            assertTrue(str.contains("-Infinity"));
        }
        {
            list = new FloatList();
            FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
            String str = list.toString();
            assertNotNull(str);
            assertTrue(str.contains("1.0"));
        }
    }

    @Test
    public void testString_Empty() {
        FloatList list = new FloatList();
        String str = list.toString();
        assertNotNull(str);
    }

    @Test
    public void testEnsureCapacity_LargeData() {
        // Test that adding many elements (beyond initial capacity) works correctly
        FloatList fl = new FloatList(2);
        for (int i = 0; i < 100; i++) {
            fl.add(i * 1.0f);
        }
        assertEquals(100, fl.size());
        assertEquals(0.0f, fl.get(0), 0.0001f);
        assertEquals(99.0f, fl.get(99), 0.0001f);
    }

    @Test
    public void testConversionSuppliersMustProduceCollectionsForEmptyRanges() {
        assertThrows(IllegalArgumentException.class, () -> list.toCollection(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> list.toCollection(0, 0, ignored -> null));
        assertThrows(IllegalArgumentException.class, () -> list.toMultiset(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> list.toMultiset(0, 0, ignored -> null));
    }

    @Test
    public void testShared_SharedBackingArray() {
        final float[] shared = { Float.intBitsToFloat(0x7fc00001), 0f, -0f, 42f, Float.intBitsToFloat(0x7fc00002), 0f, -0f };
        final FloatList values = FloatList.of(shared);

        assertTrue(values.removeAll(FloatList.of(shared, 2)));
        assertArrayEquals(new float[] { -0f, 42f, -0f }, values.toArray());
    }

    @Test
    public void reviewFixes20260906_descendingCopyClampsAgainstSizeNotTheBackingArray() {
        // FloatList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final FloatList withSpareCapacity = FloatList.of(new float[] { 1, 2, 3, 4, 5 }, 3);

        assertEquals(3, withSpareCapacity.size());
        assertEquals(5, withSpareCapacity.internalArray().length, "the test needs real spare capacity");

        assertEquals("[3.0, 2.0, 1.0]", withSpareCapacity.copy(3, -1, -1).toString(), "spare capacity must not leak into the result");
        assertEquals("[3.0, 2.0, 1.0]", withSpareCapacity.copy(2, -1, -1).toString(), "an in-range start is unaffected");

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
            final FloatList self = FloatList.of(1f, 2f, 3f);
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final FloatList expected = FloatList.of(1f, 2f, 3f);
            final FloatList inserted = FloatList.of(1f, 2f, 3f);
            expected.addAll(index, FloatList.of(1f, 2f, 3f));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final FloatList appended = FloatList.of(1f, 2f, 3f);
        appended.addAll(appended);
        assertEquals("[1.0, 2.0, 3.0, 1.0, 2.0, 3.0]", appended.toString());

        // The interesting middle case, spelled out.
        final FloatList middle = FloatList.of(1f, 2f, 3f);
        middle.addAll(1, middle);
        assertEquals("[1.0, 1.0, 2.0, 3.0, 2.0, 3.0]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final FloatList a = FloatList.of(1f, 2f, 3f);
        assertFalse(a.removeAll((FloatList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((float[]) null));
        assertFalse(a.removeAll(new float[0]));
        assertFalse(a.removeAll(new FloatList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final FloatList b = FloatList.of(1f, 2f, 3f);
        assertTrue(b.retainAll((FloatList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final FloatList c = FloatList.of(1f, 2f, 3f);
        assertTrue(c.retainAll((float[]) null));
        assertEquals(0, c.size());

        final FloatList d = FloatList.of(1f, 2f, 3f);
        assertTrue(d.retainAll(new float[0]));
        assertEquals(0, d.size());

        final FloatList e = FloatList.of(1f, 2f, 3f);
        assertTrue(e.retainAll(new FloatList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final FloatList empty = new FloatList();
        assertFalse(empty.retainAll((FloatList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final FloatList f = FloatList.of(1f, 2f, 3f);
        assertTrue(f.retainAll(FloatList.of(1f)));
        assertEquals(1, f.size());
    }

    @Test
    public void reviewFixes20260911_lowerMedianSelectsWithoutSortingTheList() {
        // The class javadoc advertised median() and sum(); neither exists on FloatList. lowerMedian() is the
        // only central-tendency method, and it SELECTS rather than sorting, so the receiver keeps its
        // original order. N.lowerMedian short-circuits a range of 3 or fewer elements (a 3-way median for
        // the [1, 4) range below) and only uses kthLargest, a bounded PriorityQueue, above that (the
        // 7-element whole list below).
        final FloatList ranged = FloatList.of(10f, 3f, 1f, 2f, 9f);
        assertEquals(2.0f, ranged.lowerMedian(1, 4).get());
        assertEquals("[10.0, 3.0, 1.0, 2.0, 9.0]", ranged.toString());

        final FloatList whole = FloatList.of(9f, 1f, 8f, 2f, 7f, 3f, 6f);
        assertEquals(6.0f, whole.lowerMedian().get());
        assertEquals("[9.0, 1.0, 8.0, 2.0, 7.0, 3.0, 6.0]", whole.toString());

        assertFalse(new FloatList().lowerMedian().isPresent());
    }

    @Test
    public void reviewFixes20260911_removeIfIsFailureAtomicButReplaceAllAndReplaceIfAreNot() {
        // removeIf promises the list is untouched when its predicate throws; its two neighbours write as they
        // visit and had said nothing. All three now document what they actually do.
        final FloatList untouched = FloatList.of(1f, -2f, 3f, -4f, 5f);
        assertThrows(IllegalStateException.class, () -> untouched.removeIf(x -> {
            if (x == 3f) {
                throw new IllegalStateException();
            }
            return x < 0;
        }));
        assertEquals("[1.0, -2.0, 3.0, -4.0, 5.0]", untouched.toString());

        final FloatList partiallyMapped = FloatList.of(1f, 2f, 3f);
        assertThrows(IllegalStateException.class, () -> partiallyMapped.replaceAll(x -> {
            if (x == 2f) {
                throw new IllegalStateException();
            }
            return x * 10f;
        }));
        assertEquals("[10.0, 2.0, 3.0]", partiallyMapped.toString());

        final FloatList partiallyReplaced = FloatList.of(1f, 2f, 3f);
        assertThrows(IllegalStateException.class, () -> partiallyReplaced.replaceIf(x -> {
            if (x == 2f) {
                throw new IllegalStateException();
            }
            return true;
        }, 0f));
        assertEquals("[0.0, 2.0, 3.0]", partiallyReplaced.toString());
    }

    @Test
    public void reviewFixes20260911_silentComparisonMethodsUseFloatCompareSemantics() {
        // Thirteen value-comparing methods stated no float equality contract while seventeen siblings did.
        // They all use Float.compare: NaN matches NaN, and -0.0f never matches 0.0f.
        assertEquals(-1, FloatList.of(0.0f).binarySearch(-0.0f));
        assertEquals(-2, FloatList.of(-0.0f).binarySearch(0.0f));
        assertEquals(-3, FloatList.of(1f, 2f).binarySearch(Float.NaN));
        assertEquals(2, FloatList.of(1f, 2f, Float.NaN).binarySearch(Float.NaN));
        assertEquals(-2, FloatList.of(1f, 0.0f, 2f).binarySearch(1, 2, -0.0f));

        assertTrue(FloatList.of(0.0f).disjoint(FloatList.of(-0.0f)));
        assertTrue(FloatList.of(0.0f).disjoint(new float[] { -0.0f }));
        assertFalse(FloatList.of(Float.NaN).disjoint(FloatList.of(Float.NaN)));

        // disjoint() compares two different ways and the sentence covers the method, not one branch: the
        // one-element cases above take contains() -> N.equals -> Float.compare, while needToSet(min > 3
        // && max > 9) - which 12 elements against 5 satisfies - builds a Set<Float> and uses Float.equals.
        final FloatList twelve = FloatList.of(0.0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f);
        assertTrue(twelve.disjoint(FloatList.of(-0.0f, 100f, 101f, 102f, 103f)));
        assertTrue(twelve.disjoint(new float[] { -0.0f, 100f, 101f, 102f, 103f }));
        final FloatList twelveWithNaN = FloatList.of(Float.NaN, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f);
        assertFalse(twelveWithNaN.disjoint(FloatList.of(Float.NaN, 100f, 101f, 102f, 103f)));
        assertFalse(twelveWithNaN.disjoint(new float[] { Float.NaN, 100f, 101f, 102f, 103f }));

        assertEquals("[NaN]", FloatList.of(Float.NaN, 1f).intersection(FloatList.of(Float.NaN)).toString());
        assertEquals("[]", FloatList.of(0.0f).intersection(new float[] { -0.0f }).toString());
        assertEquals("[0.0]", FloatList.of(0.0f).difference(FloatList.of(-0.0f)).toString());
        assertEquals("[]", FloatList.of(Float.NaN).difference(new float[] { Float.NaN }).toString());
        assertEquals("[0.0, -0.0]", FloatList.of(0.0f).symmetricDifference(FloatList.of(-0.0f)).toString());
        assertEquals("[]", FloatList.of(Float.NaN).symmetricDifference(new float[] { Float.NaN }).toString());

        // removeDuplicates has two code paths and the javadoc now says both apply the same rule.
        final FloatList sortedZeros = FloatList.of(-0.0f, 0.0f); // isSorted() -> the Float.compare fast path
        assertFalse(sortedZeros.removeDuplicates());
        assertEquals("[-0.0, 0.0]", sortedZeros.toString());
        final FloatList unsortedZeros = FloatList.of(0.0f, -0.0f); // -> the LinkedHashSet path
        assertFalse(unsortedZeros.removeDuplicates());
        assertEquals("[0.0, -0.0]", unsortedZeros.toString());
        // Float.compare(NaN, NaN) == 0, so of(NaN, NaN) is isSorted() and takes the fast path as well.
        final FloatList nans = FloatList.of(Float.NaN, Float.NaN);
        assertTrue(nans.removeDuplicates());
        assertEquals("[NaN]", nans.toString());
        // The fourth cell of the matrix: NaN on the LinkedHashSet path, which dedups by Float.equals.
        final FloatList unsortedNaNs = FloatList.of(Float.NaN, 1f, Float.NaN);
        assertFalse(unsortedNaNs.isSorted());
        assertTrue(unsortedNaNs.removeDuplicates());
        assertEquals("[NaN, 1.0]", unsortedNaNs.toString());

        assertEquals(2, FloatList.of(-0.0f, 0.0f).distinct(0, 2).size());
        assertEquals(1, FloatList.of(Float.NaN, Float.NaN).distinct(0, 2).size());

        // hashCode() hashes Float.floatToIntBits, so it agrees with equals() on both special cases.
        assertEquals(FloatList.of(Float.NaN).hashCode(), FloatList.of(Float.NaN).hashCode());
        assertEquals(FloatList.of(Float.NaN), FloatList.of(Float.NaN));
        assertNotEquals(FloatList.of(0.0f).hashCode(), FloatList.of(-0.0f).hashCode());
        assertNotEquals(FloatList.of(0.0f), FloatList.of(-0.0f));
    }

    @Test
    public void reviewFixes20260911_sortFamilyUsesTheFloatCompareTotalOrder() {
        // The sort family documented its NaN rule and never its signed-zero rule, yet -0.0f sorts BEFORE
        // 0.0f in the Float.compare total order that all four methods impose.
        assertFalse(FloatList.of(0.0f, -0.0f).isSorted());
        assertTrue(FloatList.of(-0.0f, 0.0f).isSorted());

        final FloatList ascending = FloatList.of(0.0f, -0.0f, Float.NaN, 1f);
        ascending.sort();
        assertEquals("[-0.0, 0.0, 1.0, NaN]", ascending.toString());

        final FloatList parallel = FloatList.of(0.0f, -0.0f, Float.NaN, 1f);
        parallel.parallelSort();
        assertEquals("[-0.0, 0.0, 1.0, NaN]", parallel.toString());

        final FloatList descending = FloatList.of(0.0f, -0.0f, Float.NaN, 1f);
        descending.reverseSort();
        assertEquals("[NaN, 1.0, 0.0, -0.0]", descending.toString());

        // Belt and braces - toString() above already renders the sign as "-0.0"; pin the raw bits as well.
        assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(ascending.get(0)));
        assertEquals(Float.floatToIntBits(0.0f), Float.floatToIntBits(ascending.get(1)));
    }

    @Test
    public void reviewFixes20260911_parallelSortMatchesSortAtEverySize() {
        // The float twin of the DoubleList sweep: parallelSort() delegates to the same N.parallelSort, which
        // runs the SEQUENTIAL Arrays.sort below a 3000-element threshold or on a single-core host, and above
        // it hands off to Arrays.parallelSort(float[], ...), which applies a threshold of its own. Before
        // this sweep no test handed parallelSort() more than 5 floats, so that hand-off never executed.
        final Random rnd = new Random(20260911L);

        for (final int size : new int[] { 0, 1, 2, 2999, 3000, 3001, 8193 }) {
            final float[] data = new float[size];

            for (int i = 0; i < size; i++) {
                data[i] = rnd.nextInt(1000) - 500;
            }

            final FloatList sequential = FloatList.copyOf(data);
            final FloatList parallel = FloatList.copyOf(data);
            sequential.sort();
            parallel.parallelSort();

            assertArrayEquals(sequential.toArray(), parallel.toArray(), "size=" + size);
            assertTrue(parallel.isSorted(), "size=" + size);
        }
    }
}
