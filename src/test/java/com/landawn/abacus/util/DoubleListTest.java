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

import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.DoubleStream;

public class DoubleListTest extends DoubleListTestSupport {

    @Test
    public void testSymmetricDifferenceIncludesFirstOperandPrefix() {
        final DoubleList first = DoubleList.of(1, 9);
        final DoubleList second = DoubleList.of(1, 2, 1);
        final double[] expected = { 9, 1, 2 };

        assertArrayEquals(expected, first.symmetricDifference(second).toArray());
        assertArrayEquals(expected, first.symmetricDifference(second.toArray()).toArray());
    }

    @Test
    public void testRangedForEachRejectsNullActionForEmptyRange() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> list.forEach(0, 0, (java.util.function.DoubleConsumer) null));
    }

    @Test
    public void testConstructors() {
        {
            list = new DoubleList();
            double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
            DoubleList newList = new DoubleList(array, 3);
            assertEquals(3, newList.size());
            assertEquals(1.1, newList.get(0), DELTA);
            assertEquals(2.2, newList.get(1), DELTA);
            assertEquals(3.3, newList.get(2), DELTA);
        }
        {
            list = new DoubleList();
            double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
            DoubleList newList = new DoubleList(array);
            assertEquals(5, newList.size());
            for (int i = 0; i < array.length; i++) {
                assertEquals(array[i], newList.get(i), DELTA);
            }
        }
        {
            list = new DoubleList();
            DoubleList newList = new DoubleList(10);
            assertTrue(newList.isEmpty());
            assertEquals(0, newList.size());
        }
        {
            list = new DoubleList();
            DoubleList newList = new DoubleList();
            assertTrue(newList.isEmpty());
            assertEquals(0, newList.size());
        }
        {
            list = new DoubleList();
            double[] array = { 1.1, 2.2, 3.3 };
            assertThrows(IndexOutOfBoundsException.class, () -> new DoubleList(array, 4));
        }
        {
            list = new DoubleList();
            double[] array = { 1.1, 2.2, 3.3 };
            assertThrows(IllegalArgumentException.class, () -> new DoubleList(array, -1));
        }
    }

    @Test
    public void testConstructors_NegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new DoubleList(-1));
    }

    @Test
    public void testConstructors_Null() {
        {
            list = new DoubleList();
            assertThrows(IllegalArgumentException.class, () -> new DoubleList(null, 0));
        }
        {
            list = new DoubleList();
            assertThrows(IllegalArgumentException.class, () -> new DoubleList(null));
        }
    }

    @Test
    public void testBatch_LargeData() {
        {
            list = new DoubleList();
            int size = 1000;
            DoubleList list1 = new DoubleList();
            DoubleList list2 = new DoubleList();

            for (int i = 0; i < size; i++) {
                list1.add(i * 0.1);
            }

            for (int i = size / 2; i < size + size / 2; i++) {
                list2.add(i * 0.1);
            }

            DoubleList intersection = list1.intersection(list2);
            assertEquals(size / 2, intersection.size());

            DoubleList difference = list1.difference(list2);
            assertEquals(size / 2, difference.size());

            DoubleList symDiff = list1.symmetricDifference(list2);
            assertEquals(size, symDiff.size());
        }
        {
            list = new DoubleList();
            // triggers the Set path (c.size() > 3 && size() > 9)
            DoubleList dl = new DoubleList();
            for (int i = 1; i <= 15; i++) {
                dl.add(i * 1.0);
            }
            DoubleList retain = DoubleList.of(1.0, 3.0, 5.0, 7.0, 9.0);
            dl.retainAll(retain);
            assertEquals(5, dl.size());
            assertEquals(1.0, dl.get(0), DELTA);
            assertEquals(3.0, dl.get(1), DELTA);
        }
    }

    @Test
    public void testOf() {
        {
            list = new DoubleList();
            double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
            DoubleList newList = DoubleList.of(array, 3);
            assertEquals(3, newList.size());
            assertEquals(1.1, newList.get(0), DELTA);
            assertEquals(2.2, newList.get(1), DELTA);
            assertEquals(3.3, newList.get(2), DELTA);
        }
        {
            list = new DoubleList();
            DoubleList newList = DoubleList.of(1.1, 2.2, 3.3, 4.4, 5.5);
            assertEquals(5, newList.size());
            assertEquals(1.1, newList.get(0), DELTA);
            assertEquals(5.5, newList.get(4), DELTA);
        }
        {
            list = new DoubleList();
            double[] array = { 1.1, 2.2, 3.3 };
            DoubleList newList = DoubleList.of(array, 0);
            assertTrue(newList.isEmpty());
        }
        {
            list = new DoubleList();
            double[] array = { 1.1, 2.2, 3.3 };
            assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.of(array, 4));
        }
        {
            list = new DoubleList();
            DoubleList newList = DoubleList.of();
            assertTrue(newList.isEmpty());
        }
    }

    @Test
    public void testOf_Null() {
        {
            list = new DoubleList();
            DoubleList newList = DoubleList.of((double[]) null);
            assertTrue(newList.isEmpty());
        }
        {
            list = new DoubleList();
            assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.of(null, 5));
        }
    }

    @Test
    public void testObject() {
        DoubleList list1 = DoubleList.of(1.1, 2.2);
        DoubleList list2 = DoubleList.of(1.1, 2.2);
        DoubleList list3 = DoubleList.of(2.2, 1.1);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());

        assertEquals(list1.hashCode(), list2.hashCode());

        assertEquals("[1.1, 2.2]", list1.toString());
        assertEquals("[]", new DoubleList().toString());
    }

    @Test
    public void testCopyOf() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        DoubleList newList = DoubleList.copyOf(array, 1, 4);
        assertEquals(3, newList.size());
        assertEquals(2.2, newList.get(0), DELTA);
        assertEquals(3.3, newList.get(1), DELTA);
        assertEquals(4.4, newList.get(2), DELTA);
    }

    @Test
    public void testCopyOf_Null() {
        DoubleList newList = DoubleList.copyOf(null);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testCopyOf_Empty() {
        double[] array = { 1.1, 2.2, 3.3 };
        DoubleList newList = DoubleList.copyOf(array, 1, 1);
        assertTrue(newList.isEmpty());
    }

    @Test
    public void testCopyOf_InvalidRange() {
        double[] array = { 1.1, 2.2, 3.3, 4.4, 5.5 };
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, 3, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.copyOf(array, 2, 10));
    }

    @Test
    public void testRepeat() {
        {
            list = new DoubleList();
            DoubleList nanList = DoubleList.repeat(Double.NaN, 3);
            assertEquals(3, nanList.size());
            for (int i = 0; i < 3; i++) {
                assertTrue(Double.isNaN(nanList.get(i)));
            }

            DoubleList infList = DoubleList.repeat(Double.POSITIVE_INFINITY, 3);
            assertEquals(3, infList.size());
            for (int i = 0; i < 3; i++) {
                assertEquals(Double.POSITIVE_INFINITY, infList.get(i), DELTA);
            }
        }
        {
            list = new DoubleList();
            DoubleList nanList = DoubleList.repeat(Double.NaN, 3);
            assertEquals(3, nanList.size());
            assertTrue(Double.isNaN(nanList.get(0)));

            DoubleList infList = DoubleList.repeat(Double.POSITIVE_INFINITY, 2);
            assertEquals(2, infList.size());
            assertEquals(Double.POSITIVE_INFINITY, infList.get(0), DELTA);
        }
        {
            list = new DoubleList();
            DoubleList newList = DoubleList.repeat(7.7, 5);
            assertEquals(5, newList.size());
            for (int i = 0; i < 5; i++) {
                assertEquals(7.7, newList.get(i), DELTA);
            }
        }
        {
            list = new DoubleList();
            DoubleList newList = DoubleList.repeat(7.7, 0);
            assertTrue(newList.isEmpty());
        }
        {
            list = new DoubleList();
            assertThrows(IllegalArgumentException.class, () -> DoubleList.repeat(7.7, -1));
        }
    }

    @Test
    public void testRandom() {
        {
            list = new DoubleList();
            DoubleList newList = DoubleList.random(10);
            assertEquals(10, newList.size());
            for (int i = 0; i < 10; i++) {
                double value = newList.get(i);
                assertTrue(value >= 0.0 && value < 1.0);
            }
        }
        {
            list = new DoubleList();
            DoubleList newList = DoubleList.random(0);
            assertTrue(newList.isEmpty());
        }
    }

    @Test
    public void testArray() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            double[] array = list.internalArray();

            array[1] = 20.5;
            assertEquals(20.5, list.get(1), DELTA);

            list.clear();
            double[] newArray = list.internalArray();
            assertSame(array, newArray);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            double[] internal = list.internalArray();
            assertNotNull(internal);
            assertTrue(internal.length >= 3);
            assertEquals(1.1, internal[0], DELTA);
            assertEquals(2.2, internal[1], DELTA);
            assertEquals(3.3, internal[2], DELTA);

            // Modifying internal array affects the list
            internal[0] = 99.9;
            assertEquals(99.9, list.get(0), DELTA);

            // Empty list
            DoubleList empty = new DoubleList();
            assertNotNull(empty.internalArray());
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            double[] array = list.toArray();

            assertEquals(3, array.length);
            assertEquals(1.1, array[0], DELTA);
            assertEquals(2.2, array[1], DELTA);
            assertEquals(3.3, array[2], DELTA);
        }
    }

    @Test
    public void testArray_Empty() {
        double[] array = list.toArray();
        assertEquals(0, array.length);
    }

    @Test
    public void testGetSet() {
        {
            list = new DoubleList();
            list.add(1.1);
            list.set(0, Double.NaN);
            assertTrue(Double.isNaN(list.get(0)));

            list.set(0, Double.POSITIVE_INFINITY);
            assertEquals(Double.POSITIVE_INFINITY, list.get(0), DELTA);

            list.set(0, Double.NEGATIVE_INFINITY);
            assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);

            list.set(0, 0.0);
            assertEquals(0.0, list.get(0), DELTA);

            list.set(0, -0.0);
            assertEquals(-0.0, list.get(0), DELTA);
        }
        {
            list = new DoubleList();
            list.add(10.5);
            assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 20.5));
        }
    }

    @Test
    public void testGetSet_OutOfBounds() {
        {
            list = new DoubleList();
            list.add(10.5);
            assertThrows(IndexOutOfBoundsException.class, () -> list.get(1));
        }
        {
            list = new DoubleList();
            assertThrows(IndexOutOfBoundsException.class, () -> list.set(0, 10.5));
        }
    }

    @Test
    public void testRemoveAt() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

            assertEquals(1.1, list.removeAt(0), DELTA);
            assertEquals(4, list.size());
            assertEquals(2.2, list.get(0), DELTA);

            assertEquals(5.5, list.removeAt(list.size() - 1), DELTA);
            assertEquals(3, list.size());
            assertEquals(4.4, list.get(list.size() - 1), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

            list.removeAllAt(1, 3);
            assertEquals(3, list.size());
            assertEquals(10.5, list.get(0), DELTA);
            assertEquals(30.5, list.get(1), DELTA);
            assertEquals(50.5, list.get(2), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5 });
            list.removeAllAt(1, 1, 2);
            assertEquals(2, list.size());
            assertEquals(10.5, list.get(0), DELTA);
            assertEquals(40.5, list.get(1), DELTA);
        }
    }

    @Test
    public void testRemoveAt_Empty() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.removeAllAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAt_OutOfBounds() {
        list.add(10.5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(1));
    }

    @Test
    public void testRemoveRange() {
        list.addAll(new double[] { 10.5, 20.5, 30.5, 40.5, 50.5 });

        list.removeRange(1, 3);
        assertEquals(3, list.size());
        assertEquals(10.5, list.get(0), DELTA);
        assertEquals(40.5, list.get(1), DELTA);
        assertEquals(50.5, list.get(2), DELTA);
    }

    @Test
    public void testRemoveRange_Empty() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        list.removeRange(1, 1);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveRange_OutOfBounds() {
        list.addAll(new double[] { 10.5, 20.5, 30.5 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(1, 4));
    }

    @Test
    public void testFrequency() {
        list.addAll(new double[] { 1.1, 2.2, 1.1, 3.3, 1.1 });
        assertEquals(3, list.frequency(1.1));
        assertEquals(1, list.frequency(2.2));
        assertEquals(0, list.frequency(9.9));

        // Empty list
        DoubleList empty = new DoubleList();
        assertEquals(0, empty.frequency(1.0));
    }

    @Test
    public void testFrequency_Empty() {
        assertEquals(0, list.frequency(1.1));
    }

    @Test
    public void testContainsDuplicates() {
        {
            list = new DoubleList();
            DoubleList list1 = DoubleList.of(1.0, 2.0, 1.0);
            assertTrue(list1.containsDuplicates());

            DoubleList list2 = DoubleList.of(1.0, 2.0, 3.0);
            assertFalse(list2.containsDuplicates());

            assertFalse(new DoubleList().containsDuplicates());
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
            assertTrue(list.containsDuplicates());

            DoubleList noDupes = DoubleList.of(1.1, 2.2, 3.3);
            assertFalse(noDupes.containsDuplicates());
        }
    }

    @Test
    public void testContainsDuplicates_Empty() {
        assertFalse(list.containsDuplicates());
    }

    @Test
    public void testConcurrentModification() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });

        DoubleIterator iter = list.iterator();
        list.add(6.6);

        assertTrue(iter.hasNext());
        iter.nextDouble();
    }

    @Test
    public void testEmpty() {
        assertFalse(list.remove(1.1));
        assertFalse(list.removeAllOccurrences(1.1));
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
    public void testRetainAll() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            DoubleList toRetain = DoubleList.of(2.2, 4.4, 6.6);

            boolean result = list.retainAll(toRetain);
            assertTrue(result);
            assertEquals(2, list.size());
            assertEquals(2.2, list.get(0), DELTA);
            assertEquals(4.4, list.get(1), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            double[] toRetain = { 2.2, 4.4, 6.6 };

            boolean result = list.retainAll(toRetain);
            assertTrue(result);
            assertEquals(2, list.size());
            assertEquals(2.2, list.get(0), DELTA);
            assertEquals(4.4, list.get(1), DELTA);
        }
    }

    @Test
    public void testRetainAll_Empty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList toRetain = new DoubleList();
        boolean result = list.retainAll(toRetain);
        assertTrue(result);
        assertEquals(0, list.size());
    }

    @Test
    public void testMoveRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        list.moveRange(0, 2, 3);
        assertEquals(5, list.size());
        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(4.4, list.get(1), DELTA);
        assertEquals(5.5, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(2.2, list.get(4), DELTA);
    }

    @Test
    public void testPrecision() {
        double a = 0.1 + 0.2;
        double b = 0.3;

        list.add(a);
        list.add(b);

        int replaced = list.replaceAll(0.3, 1.0);
        assertTrue(replaced > 0);
    }

    @Test
    public void testFill() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            list.fill(Double.NaN);
            for (int i = 0; i < list.size(); i++) {
                assertTrue(Double.isNaN(list.get(i)));
            }
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            list.fill(1, 4, 9.9);

            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(9.9, list.get(1), DELTA);
            assertEquals(9.9, list.get(2), DELTA);
            assertEquals(9.9, list.get(3), DELTA);
            assertEquals(5.5, list.get(4), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
            list.fill(9.9);

            assertEquals(4, list.size());
            for (int i = 0; i < 4; i++) {
                assertEquals(9.9, list.get(i), DELTA);
            }
        }
    }

    @Test
    public void testFill_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            list.fill(1, 1, 9.9);
            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(2.2, list.get(1), DELTA);
            assertEquals(3.3, list.get(2), DELTA);
        }
        {
            list = new DoubleList();
            list.fill(10.0);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    public void testFill_InvalidRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.fill(2, 1, 10.0));
    }

    @Test
    public void testContains() {
        {
            list = new DoubleList();
            DoubleList list = DoubleList.of(1.1, 2.2, 3.3, Double.NaN);
            assertTrue(list.contains(2.2));
            assertTrue(list.contains(Double.NaN));
            assertFalse(list.contains(9.9));

            assertTrue(list.containsAll(DoubleList.of(1.1, 3.3, Double.NaN)));
            assertFalse(list.containsAll(DoubleList.of(1.1, 4.4)));

            assertTrue(list.containsAny(new double[] { 4.4, 5.5, 2.2 }));
            assertFalse(list.containsAny(new double[] { 4.4, 5.5, 6.6 }));
        }
        {
            list = new DoubleList();
            list.add(Double.NaN);
            assertTrue(list.contains(Double.NaN));

            list.add(Double.POSITIVE_INFINITY);
            assertTrue(list.contains(Double.POSITIVE_INFINITY));

            list.add(0.0);
            assertTrue(list.contains(0.0));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            assertTrue(list.contains(2.2));
            assertFalse(list.contains(4.4));
        }
    }

    @Test
    public void testContains_NaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });
        assertTrue(list.contains(Double.NaN));
    }

    @Test
    public void testContains_Empty() {
        assertFalse(list.contains(1.1));
    }

    @Test
    public void testContains_Infinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
        assertTrue(list.contains(Double.POSITIVE_INFINITY));
        assertTrue(list.contains(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testDouble() {
        double a = 0.1;
        double b = 0.2;
        double c = a + b;

        list.add(c);
        list.add(0.3);

        assertTrue(list.contains(c));
        assertTrue(list.contains(0.3));
    }

    @Test
    public void testVery() {
        {
            list = new DoubleList();
            list.add(Double.MIN_VALUE);
            list.add(Double.MIN_NORMAL);
            list.add(0.0);

            assertTrue(list.contains(Double.MIN_VALUE));
            assertTrue(list.contains(Double.MIN_NORMAL));

            list.sort();
            assertEquals(0.0, list.get(0), DELTA);
            assertEquals(Double.MIN_VALUE, list.get(1), DELTA);
            assertEquals(Double.MIN_NORMAL, list.get(2), DELTA);
        }
        {
            list = new DoubleList();
            double largeValue = 9007199254740992.0;
            list.add(largeValue);
            list.add(largeValue + 1);

            assertEquals(largeValue, list.get(0), DELTA);
            assertEquals(largeValue + 1, list.get(1), DELTA);

            assertTrue(list.contains(largeValue));
            assertEquals(0, list.indexOf(largeValue));
        }
    }

    @Test
    public void testZero() {
        list.add(-0.0);
        list.add(0.0);

        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(0.0, list.get(1), DELTA);

        assertTrue(list.contains(0.0));
        assertTrue(list.contains(-0.0));
    }

    @Test
    public void testContainsAny() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList other = DoubleList.of(3.3, 4.4, 5.5);
            assertTrue(list.containsAny(other));

            DoubleList noMatch = DoubleList.of(6.6, 7.7);
            assertFalse(list.containsAny(noMatch));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            double[] other = { 3.3, 4.4, 5.5 };
            assertTrue(list.containsAny(other));

            double[] noMatch = { 6.6, 7.7 };
            assertFalse(list.containsAny(noMatch));
        }
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0);
            assertTrue(dl.containsAny(DoubleList.of(3.0, 4.0)));
            assertFalse(dl.containsAny(DoubleList.of(4.0, 5.0)));
        }
    }

    @Test
    public void testContainsAny_Empty() {
        DoubleList other = new DoubleList();
        assertFalse(list.containsAny(other));
    }

    @Test
    public void testContainsAll() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
            DoubleList subset = DoubleList.of(2.2, 3.3);
            assertTrue(list.containsAll(subset));

            DoubleList notSubset = DoubleList.of(2.2, 5.5);
            assertFalse(list.containsAll(notSubset));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
            double[] subset = { 2.2, 3.3 };
            assertTrue(list.containsAll(subset));

            double[] notSubset = { 2.2, 5.5 };
            assertFalse(list.containsAll(notSubset));
        }
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0);
            assertTrue(dl.containsAll(new double[] { 1.0, 3.0 }));
            assertFalse(dl.containsAll(new double[] { 1.0, 9.0 }));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList empty = new DoubleList();
            assertTrue(list.containsAll(empty));
        }
    }

    @Test
    public void testContainsAll_Empty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertTrue(list.containsAll(new DoubleList()));
    }

    @Test
    public void testDisjoint() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList other = DoubleList.of(4.4, 5.5, 6.6);
            assertTrue(list.disjoint(other));

            DoubleList overlapping = DoubleList.of(3.3, 4.4);
            assertFalse(list.disjoint(overlapping));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            double[] other = { 4.4, 5.5, 6.6 };
            assertTrue(list.disjoint(other));

            double[] overlapping = { 3.3, 4.4 };
            assertFalse(list.disjoint(overlapping));
        }
    }

    @Test
    public void testDisjoint_Empty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList empty = new DoubleList();
        assertTrue(list.disjoint(empty));
    }

    @Test
    public void testIntersection() {
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 4.0);
            DoubleList result = dl.intersection(new double[] { 2.0, 4.0, 6.0 });
            assertEquals(2, result.size());
            assertTrue(result.contains(2.0));
            assertTrue(result.contains(4.0));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
            DoubleList other = DoubleList.of(1.1, 2.2, 2.2, 4.4);

            DoubleList result = list.intersection(other);
            assertEquals(2, result.size());
            assertEquals(1.1, result.get(0), DELTA);
            assertEquals(2.2, result.get(1), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList result = list.intersection(DoubleList.of(4.4, 5.5, 6.6));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testIntersection_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList other = new DoubleList();

            DoubleList result = list.intersection(other);
            assertTrue(result.isEmpty());
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList result = list.intersection(new DoubleList());
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testDifference() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
            DoubleList other = DoubleList.of(1.1, 4.4);

            DoubleList result = list.difference(other);
            assertEquals(3, result.size());
            assertEquals(1.1, result.get(0), DELTA);
            assertEquals(2.2, result.get(1), DELTA);
            assertEquals(3.3, result.get(2), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
            double[] other = { 1.1, 4.4 };

            DoubleList result = list.difference(other);
            assertEquals(3, result.size());
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList result = list.difference(DoubleList.of(1.1, 2.2, 3.3));
            assertTrue(result.isEmpty());
        }
    }

    @Test
    public void testDifference_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList other = new DoubleList();

            DoubleList result = list.difference(other);
            assertEquals(3, result.size());
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList result = list.difference(new DoubleList());
            assertEquals(3, result.size());
            assertEquals(1.1, result.get(0), DELTA);
            assertEquals(2.2, result.get(1), DELTA);
            assertEquals(3.3, result.get(2), DELTA);
        }
    }

    @Test
    public void testSymmetricDifference() {
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0);
            DoubleList result = dl.symmetricDifference(DoubleList.of(2.0, 4.0));
            assertTrue(result.contains(1.0));
            assertTrue(result.contains(3.0));
            assertTrue(result.contains(4.0));
            assertFalse(result.contains(2.0));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 1.1, 2.2, 3.3 });
            double[] other = { 2.2, 3.3, 3.3, 4.4 };

            DoubleList result = list.symmetricDifference(other);
            assertEquals(4, result.size());
            assertEquals(DoubleList.of(1.1, 1.1, 3.3, 4.4), result);
        }
    }

    @Test
    public void testSymmetricDifference_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList other = new DoubleList();

            DoubleList result = list.symmetricDifference(other);
            assertEquals(3, result.size());
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList result = list.symmetricDifference(new DoubleList());
            assertEquals(3, result.size());
        }
    }

    @Test
    public void testIndexOf() {
        {
            list = new DoubleList();
            DoubleList list = DoubleList.of(1.1, 2.2, 1.1, 3.3, 2.2);
            assertEquals(0, list.indexOf(1.1));
            assertEquals(2, list.lastIndexOf(1.1));
            assertEquals(4, list.lastIndexOf(2.2));
            assertEquals(4, list.indexOf(2.2, 2));
            assertEquals(-1, list.indexOf(9.9));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

            assertEquals(3, list.indexOf(2.2, 2));
            assertEquals(-1, list.indexOf(2.2, 4));
        }
    }

    @Test
    public void testIndexOf_NaN() {
        list.addAll(new double[] { 1.1, Double.NaN, 2.2, Double.NaN });
        assertEquals(1, list.indexOf(Double.NaN));
        assertEquals(3, list.indexOf(Double.NaN, 2));
    }

    @Test
    public void testIndexOf_Empty() {
        assertEquals(-1, list.indexOf(1.1));
    }

    @Test
    public void testIndexOf_OutOfBounds() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(-1, list.indexOf(1.1, 10));
    }

    @Test
    public void testLastIndexOf() {
        {
            list = new DoubleList();
            DoubleList dl = DoubleList.of(1.0, 2.0, 3.0, 2.0, 1.0);
            assertEquals(3, dl.lastIndexOf(2.0, 4));
            assertEquals(1, dl.lastIndexOf(2.0, 2));
            assertEquals(-1, dl.lastIndexOf(9.0, 4));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 5.5 });

            assertEquals(1, list.lastIndexOf(2.2, 2));
            assertEquals(-1, list.lastIndexOf(5.5, 3));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 2.2, 4.4 });
            assertEquals(3, list.lastIndexOf(2.2));
            assertEquals(-1, list.lastIndexOf(5.5));
        }
    }

    @Test
    public void testLastIndexOf_NaN() {
        list.addAll(new double[] { Double.NaN, 1.1, Double.NaN, 2.2 });
        assertEquals(2, list.lastIndexOf(Double.NaN));
        assertEquals(0, list.lastIndexOf(Double.NaN, 1));
    }

    @Test
    public void testLastIndexOf_Empty() {
        assertEquals(-1, list.lastIndexOf(1.1));
    }

    @Test
    public void testMin() {
        {
            list = new DoubleList();
            list.add(5.5);

            OptionalDouble min = list.min();
            assertTrue(min.isPresent());
            assertEquals(5.5, min.getAsDouble(), DELTA);

            OptionalDouble max = list.max();
            assertTrue(max.isPresent());
            assertEquals(5.5, max.getAsDouble(), DELTA);

            OptionalDouble median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals(5.5, median.getAsDouble(), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 5.5, 3.3, 1.1, 4.4, 2.2 });
            OptionalDouble min = list.min(1, 4);
            assertTrue(min.isPresent());
            assertEquals(1.1, min.get(), DELTA);
        }
    }

    @Test
    public void testMin_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });

            assertFalse(list.min(1, 1).isPresent());
            assertFalse(list.max(1, 1).isPresent());
            assertFalse(list.lowerMedian(1, 1).isPresent());
        }
        {
            list = new DoubleList();
            OptionalDouble min = list.min();
            assertFalse(min.isPresent());
        }
    }

    @Test
    public void testMin_NaN() {
        // DoubleList.min/max delegate to N.min/max, which propagate NaN per Math.min/max
        // (IEEE 754) — any NaN in the input yields a NaN result.
        list.addAll(new double[] { 1.1, Double.NaN, 2.2 });

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertTrue(Double.isNaN(min.get()));

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertTrue(Double.isNaN(max.get()));
    }

    @Test
    public void testMin_Infinity() {
        list.addAll(new double[] { 1.1, Double.NEGATIVE_INFINITY, 2.2, Double.POSITIVE_INFINITY });

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, max.getAsDouble(), DELTA);
    }

    @Test
    public void testMax() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 3.3, 5.5, 4.4, 2.2 });
            OptionalDouble max = list.max(1, 4);
            assertTrue(max.isPresent());
            assertEquals(5.5, max.get(), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
            OptionalDouble max = list.max();
            assertTrue(max.isPresent());
            assertEquals(4.4, max.get(), DELTA);
        }
    }

    @Test
    public void testMax_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.0, 2.0, 3.0 });
            assertFalse(list.max(1, 1).isPresent());
        }
        {
            list = new DoubleList();
            OptionalDouble max = list.max();
            assertFalse(max.isPresent());
        }
    }

    @Test
    public void testMedian() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.0, 2.0, 3.0, 4.0 });
            OptionalDouble median = list.lowerMedian();
            assertTrue(median.isPresent());
            assertEquals(2.0, median.getAsDouble(), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 3.3, 5.5, 2.2, 4.4 });
            OptionalDouble median = list.lowerMedian(1, 4);
            assertTrue(median.isPresent());
        }
    }

    @Test
    public void testMedian_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.0, 2.0, 3.0 });
            assertFalse(list.lowerMedian(1, 1).isPresent());
        }
        {
            list = new DoubleList();
            OptionalDouble median = list.lowerMedian();
            assertFalse(median.isPresent());
        }
    }

    @Test
    public void testEach() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            List<Double> collected = new ArrayList<>();
            list.forEach(1, 4, collected::add);

            assertEquals(3, collected.size());
            assertEquals(2.2, collected.get(0), DELTA);
            assertEquals(3.3, collected.get(1), DELTA);
            assertEquals(4.4, collected.get(2), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            List<Double> collected = new ArrayList<>();
            list.forEach(collected::add);

            assertEquals(3, collected.size());
            assertEquals(1.1, collected.get(0), DELTA);
            assertEquals(2.2, collected.get(1), DELTA);
            assertEquals(3.3, collected.get(2), DELTA);
        }
    }

    @Test
    public void testEach_Empty() {
        List<Double> result = new ArrayList<>();
        list.forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testEach_Null() {
        // Empty lists do not evaluate callbacks; non-empty lists fail naturally when invoking them.
        final DoubleList empty = new DoubleList();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.forEach((com.landawn.abacus.util.function.DoubleConsumer) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.removeIf((com.landawn.abacus.util.function.DoublePredicate) null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> empty.replaceIf((com.landawn.abacus.util.function.DoublePredicate) null, 0d));

        final DoubleList nonEmpty = DoubleList.of(1d, 2d);
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.forEach((com.landawn.abacus.util.function.DoubleConsumer) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.removeIf((com.landawn.abacus.util.function.DoublePredicate) null));
        assertThrows(IllegalArgumentException.class, () -> nonEmpty.replaceIf((com.landawn.abacus.util.function.DoublePredicate) null, 0d));
    }

    @Test
    public void testFirst() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        OptionalDouble first = list.first();
        assertTrue(first.isPresent());
        assertEquals(1.1, first.get(), DELTA);
    }

    @Test
    public void testFirst_Empty() {
        OptionalDouble first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        OptionalDouble last = list.last();
        assertTrue(last.isPresent());
        assertEquals(3.3, last.get(), DELTA);
    }

    @Test
    public void testLast_Empty() {
        OptionalDouble last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testDistinct() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3, 3.3, 3.3, 4.4 });
        DoubleList distinct = list.distinct(0, list.size());

        assertEquals(4, distinct.size());
        assertEquals(1.1, distinct.get(0), DELTA);
        assertEquals(2.2, distinct.get(1), DELTA);
        assertEquals(3.3, distinct.get(2), DELTA);
        assertEquals(4.4, distinct.get(3), DELTA);
    }

    @Test
    public void testDistinct_NaN() {
        list.addAll(new double[] { 1.1, Double.NaN, Double.NaN, 2.2 });
        DoubleList result = list.distinct(0, list.size());
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinct_Empty() {
        list.addAll(new double[] { 1.1, 2.2, 2.2, 3.3 });
        DoubleList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIsSorted() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
        assertTrue(list.isSorted());

        DoubleList unsorted = DoubleList.of(1.1, 3.3, 2.2);
        assertFalse(unsorted.isSorted());
    }

    @Test
    public void testIsSorted_Empty() {
        assertTrue(list.isSorted());
    }

    @Test
    public void testSort() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
            list.sort();

            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(2.2, list.get(1), DELTA);
            assertEquals(3.3, list.get(2), DELTA);
            assertEquals(4.4, list.get(3), DELTA);
        }
        {
            list = new DoubleList();
            list.add(5.5);
            list.sort();
            assertEquals(1, list.size());
            assertEquals(5.5, list.get(0), DELTA);
        }
    }

    @Test
    public void testSort_NaN() {
        list.addAll(new double[] { 2.2, Double.NaN, 1.1, Double.NaN, 3.3 });
        list.sort();
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
        assertTrue(Double.isNaN(list.get(3)));
        assertTrue(Double.isNaN(list.get(4)));
    }

    @Test
    public void testSort_Infinity() {
        list.addAll(new double[] { 1.1, Double.POSITIVE_INFINITY, -2.2, Double.NEGATIVE_INFINITY, 0.0 });
        list.sort();
        assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(-2.2, list.get(1), DELTA);
        assertEquals(0.0, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, list.get(4), DELTA);
    }

    @Test
    public void testParallelSort() {
        list.addAll(new double[] { 3.3, 1.1, 2.2 });
        list.parallelSort();
        assertTrue(list.isSorted());
    }

    @Test
    public void testReverseSort() {
        list.addAll(new double[] { 3.3, 1.1, 4.4, 2.2 });
        list.reverseSort();

        assertEquals(4.4, list.get(0), DELTA);
        assertEquals(3.3, list.get(1), DELTA);
        assertEquals(2.2, list.get(2), DELTA);
        assertEquals(1.1, list.get(3), DELTA);
    }

    @Test
    public void testBinarySearch() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            assertEquals(2, list.binarySearch(1, 4, 3.3));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            assertEquals(2, list.binarySearch(3.3));
            assertTrue(list.binarySearch(2.5) < 0);
        }
    }

    @Test
    public void testReverse() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            list.reverse(1, 4);

            assertEquals(1.1, list.get(0), DELTA);
            assertEquals(4.4, list.get(1), DELTA);
            assertEquals(3.3, list.get(2), DELTA);
            assertEquals(2.2, list.get(3), DELTA);
            assertEquals(5.5, list.get(4), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4 });
            list.reverse();

            assertEquals(4.4, list.get(0), DELTA);
            assertEquals(3.3, list.get(1), DELTA);
            assertEquals(2.2, list.get(2), DELTA);
            assertEquals(1.1, list.get(3), DELTA);
        }
        {
            list = new DoubleList();
            list.add(5.5);
            list.reverse();
            assertEquals(1, list.size());
            assertEquals(5.5, list.get(0), DELTA);
        }
    }

    @Test
    public void testReverse_Empty() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.reverse(1, 1);
        assertEquals(1.1, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(3.3, list.get(2), DELTA);
    }

    @Test
    public void testRotate() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            list.rotate(-2);
            assertEquals(3.3, list.get(0), DELTA);
            assertEquals(4.4, list.get(1), DELTA);
            assertEquals(5.5, list.get(2), DELTA);
            assertEquals(1.1, list.get(3), DELTA);
            assertEquals(2.2, list.get(4), DELTA);
        }
        {
            list = new DoubleList();
            list.add(5.5);
            list.rotate(10);
            assertEquals(1, list.size());
            assertEquals(5.5, list.get(0), DELTA);
        }
    }

    @Test
    public void testRotate_Empty() {
        list.rotate(5);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testShuffle() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            Random rnd = new Random(42);
            list.shuffle(rnd);

            assertEquals(5, list.size());
            assertTrue(list.contains(1.1));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            list.shuffle();

            assertEquals(5, list.size());
            assertTrue(list.contains(1.1));
            assertTrue(list.contains(5.5));
        }
        {
            list = new DoubleList();
            list.add(5.5);
            list.shuffle();
            assertEquals(1, list.size());
            assertEquals(5.5, list.get(0), DELTA);
        }
    }

    @Test
    public void testShuffle_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> new DoubleList().shuffle(null));
        assertThrows(IllegalArgumentException.class, () -> DoubleList.of(1.1).shuffle(null));
    }

    @Test
    public void testShuffle_Empty() {
        list.shuffle();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSwap() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.swap(0, 2);

        assertEquals(3.3, list.get(0), DELTA);
        assertEquals(2.2, list.get(1), DELTA);
        assertEquals(1.1, list.get(2), DELTA);
    }

    @Test
    public void testSwap_OutOfBounds() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testCopy() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            DoubleList copy = list.copy(0, 5, 3);
            assertEquals(2, copy.size());
            assertEquals(1.1, copy.get(0), DELTA);
            assertEquals(4.4, copy.get(1), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            DoubleList copy = list.copy(1, 4);

            assertEquals(3, copy.size());
            assertEquals(2.2, copy.get(0), DELTA);
            assertEquals(3.3, copy.get(1), DELTA);
            assertEquals(4.4, copy.get(2), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList copy = list.copy();

            assertEquals(3, copy.size());
            assertEquals(1.1, copy.get(0), DELTA);

            list.set(0, 99.9);
            assertEquals(1.1, copy.get(0), DELTA);
        }
    }

    @Test
    public void testCopy_Empty() {
        DoubleList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void testCopy_NegativeStep() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
        DoubleList copy = list.copy(4, 0, -1);
        assertEquals(4, copy.size());
        assertEquals(5.5, copy.get(0), DELTA);
        assertEquals(4.4, copy.get(1), DELTA);
        assertEquals(3.3, copy.get(2), DELTA);
        assertEquals(2.2, copy.get(3), DELTA);
    }

    @Test
    public void testCopy_InvalidRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 5));
    }

    @Test
    public void testSplit() {
        list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6 });
        List<DoubleList> chunks = list.split(0, 6, 2);

        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(1.1, chunks.get(0).get(0), DELTA);
        assertEquals(2.2, chunks.get(0).get(1), DELTA);
    }

    @Test
    public void testSplit_Empty() {
        List<DoubleList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testTrim() {
        list = new DoubleList(100);
        list.add(1.1);
        list.add(2.2);
        list.trimToSize();

        assertEquals(2, list.size());
        assertEquals(1.1, list.get(0), DELTA);
    }

    @Test
    public void testClear() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        list.clear();

        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testIsEmpty_Empty() {
        assertTrue(list.isEmpty());

        list.add(1.1);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add(1.1);
        assertEquals(1, list.size());

        list.add(2.2);
        assertEquals(2, list.size());

        list.removeAt(0);
        assertEquals(1, list.size());
    }

    @Test
    public void testSpecial() {
        list.add(0.0);
        list.add(-0.0);

        assertEquals(2, list.size());
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(-0.0, list.get(1), DELTA);
    }

    @Test
    public void testSpecial_NaN() {
        list.add(Double.NaN);
        list.add(1.1);
        list.add(Double.NaN);

        assertEquals(3, list.size());
        assertTrue(Double.isNaN(list.get(0)));
        assertTrue(Double.isNaN(list.get(2)));
    }

    @Test
    public void testSpecial_Infinity() {
        list.add(Double.POSITIVE_INFINITY);
        list.add(1.1);
        list.add(Double.NEGATIVE_INFINITY);

        assertEquals(3, list.size());
        assertEquals(Double.POSITIVE_INFINITY, list.get(0), DELTA);
        assertEquals(Double.NEGATIVE_INFINITY, list.get(2), DELTA);
    }

    @Test
    public void testLarge() {
        double edge = Math.pow(2, 53) - 1;
        double edgePlus = edge + 1;
        double edgePlus2 = edge + 2;

        list.add(edge);
        list.add(edgePlus);
        list.add(edgePlus2);

        assertEquals(edge, list.get(0), DELTA);
        assertEquals(edgePlus, list.get(1), DELTA);
        assertEquals(edgePlus2, list.get(2), DELTA);
    }

    @Test
    public void testLarge_LargeData() {
        for (int i = 0; i < 10000; i++) {
            list.add(i * 1.1);
        }

        assertEquals(10000, list.size());
        assertEquals(0.0, list.get(0), DELTA);
        assertEquals(9999 * 1.1, list.get(9999), DELTA);
    }

    @Test
    public void testUlp() {
        double base = 1.0;
        double nextUp = Math.nextUp(base);
        double nextDown = Math.nextDown(base);

        list.add(nextDown);
        list.add(base);
        list.add(nextUp);

        assertEquals(3, list.size());
        assertNotEquals(base, nextUp);
        assertNotEquals(base, nextDown);

        assertTrue(list.contains(base));
        assertTrue(list.contains(nextUp));
        assertTrue(list.contains(nextDown));
    }

    @Test
    public void testNa_NaN() {
        list.add(1.1);
        list.add(Double.NaN);
        list.add(2.2);

        assertEquals(3, list.size());
        assertTrue(Double.isNaN(list.get(1)));

        assertTrue(list.contains(Double.NaN));

        assertEquals(1, list.indexOf(Double.NaN));
    }

    @Test
    public void testSubnormal() {
        double subnormal = Double.MIN_VALUE / 2;
        list.add(subnormal);
        list.add(0.0);
        list.add(-subnormal);

        assertEquals(3, list.size());
        assertTrue(list.contains(subnormal));
        assertTrue(list.contains(-subnormal));
    }

    @Test
    public void testBoxed() {
        {
            list = new DoubleList();
            list.addAll(new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY });
            List<Double> boxed = list.boxed();
            assertTrue(Double.isNaN(boxed.get(0)));
            assertEquals(Double.POSITIVE_INFINITY, boxed.get(1));
            assertEquals(Double.NEGATIVE_INFINITY, boxed.get(2));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            List<Double> boxed = list.boxed(1, 4);

            assertEquals(3, boxed.size());
            assertEquals(2.2, boxed.get(0), DELTA);
            assertEquals(3.3, boxed.get(1), DELTA);
            assertEquals(4.4, boxed.get(2), DELTA);
        }
    }

    @Test
    public void testBoxed_Empty() {
        List<Double> boxed = list.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    public void testBoxed_InvalidRange() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.boxed(2, 1));
    }

    @Test
    public void testCollection() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        List<Double> collection = list.toCollection(0, 3, ArrayList::new);

        assertEquals(3, collection.size());
        assertEquals(1.1, collection.get(0), DELTA);
    }

    @Test
    public void testMultiset() {
        list.addAll(new double[] { 1.1, 2.2, 1.1, 3.3 });
        Multiset<Double> multiset = list.toMultiset(0, 4, Multiset::new);

        assertEquals(2, multiset.count(1.1));
        assertEquals(1, multiset.count(2.2));
    }

    @Test
    public void testIterator() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleIterator iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(1.1, iterator.nextDouble(), DELTA);
        assertTrue(iterator.hasNext());
        assertEquals(2.2, iterator.nextDouble(), DELTA);
        assertTrue(iterator.hasNext());
        assertEquals(3.3, iterator.nextDouble(), DELTA);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIterator_Empty() {
        DoubleIterator iter = list.iterator();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.nextDouble());
    }

    @Test
    public void testStream() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3, 4.4, 5.5 });
            DoubleStream stream = list.stream(1, 4);

            assertNotNull(stream);
            assertEquals(3, stream.count());
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleStream stream = list.stream();

            assertNotNull(stream);
            assertEquals(3, stream.count());
        }
    }

    @Test
    public void testStream_Empty() {
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleStream stream = list.stream(1, 1);
            assertEquals(0, stream.count());
        }
        {
            list = new DoubleList();
            DoubleStream stream = list.stream();
            assertEquals(0, stream.count());
        }
    }

    @Test
    public void testGetFirst() {
        {
            list = new DoubleList();
            list.add(5.5);
            assertEquals(5.5, list.getFirst(), DELTA);
            assertEquals(5.5, list.getLast(), DELTA);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            assertEquals(1.1, list.getFirst(), DELTA);
        }
    }

    @Test
    public void testGetFirst_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetLast() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(3.3, list.getLast(), DELTA);
    }

    @Test
    public void testGetLast_Empty() {
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testHashCode() {
        list.addAll(new double[] { 1.1, 2.2, 3.3 });
        DoubleList list2 = DoubleList.of(1.1, 2.2, 3.3);

        assertEquals(list.hashCode(), list2.hashCode());
    }

    @Test
    public void testHashCode_Empty() {
        DoubleList list2 = new DoubleList();
        assertEquals(list.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals() {
        {
            list = new DoubleList();
            list.add(0.1 + 0.2);

            DoubleList other = new DoubleList();
            other.add(0.3);

            boolean equalsResult = list.equals(other);
            assertNotNull(equalsResult);
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            DoubleList list2 = DoubleList.of(1.1, 2.2, 3.3);

            assertTrue(list.equals(list2));
            assertTrue(list2.equals(list));
        }
        {
            list = new DoubleList();
            list.add(1.1);
            assertFalse(list.equals("not a DoubleList"));
        }
    }

    @Test
    public void testEquals_Null() {
        assertFalse(list.equals(null));
    }

    @Test
    public void testString() {
        {
            list = new DoubleList();
            list.add(Double.NaN);
            list.add(Double.POSITIVE_INFINITY);
            list.add(Double.NEGATIVE_INFINITY);
            list.add(-0.0);
            list.add(0.0);

            String str = list.toString();
            assertTrue(str.contains("NaN"));
            assertTrue(str.contains("Infinity"));
            assertTrue(str.contains("-Infinity"));
        }
        {
            list = new DoubleList();
            list.addAll(new double[] { 1.1, 2.2, 3.3 });
            String str = list.toString();

            assertNotNull(str);
            assertTrue(str.contains("1.1"));
            assertTrue(str.contains("2.2"));
            assertTrue(str.contains("3.3"));
        }
    }

    @Test
    public void testString_Empty() {
        String str = list.toString();
        assertNotNull(str);
    }

    @Test
    public void testEnsureCapacity_LargeData() {
        // Test adding many elements beyond initial capacity
        DoubleList dl = new DoubleList(2);
        for (int i = 0; i < 100; i++) {
            dl.add(i * 1.0);
        }
        assertEquals(100, dl.size());
        assertEquals(0.0, dl.get(0), DELTA);
        assertEquals(99.0, dl.get(99), DELTA);
    }

    @Test
    public void testScientific() {
        list.add(1.23e-10);
        list.add(4.56e+10);
        list.add(7.89e100);
        list.add(-9.87e-100);

        assertEquals(1.23e-10, list.get(0), DELTA);
        assertEquals(4.56e+10, list.get(1), DELTA);
        assertEquals(7.89e100, list.get(2), DELTA);
        assertEquals(-9.87e-100, list.get(3), DELTA);
    }

    @Test
    public void testInfinity_Infinity() {
        list.add(Double.NEGATIVE_INFINITY);
        list.add(0.0);
        list.add(Double.POSITIVE_INFINITY);

        assertEquals(Double.NEGATIVE_INFINITY, list.get(0), DELTA);
        assertEquals(0.0, list.get(1), DELTA);
        assertEquals(Double.POSITIVE_INFINITY, list.get(2), DELTA);

        OptionalDouble min = list.min();
        assertTrue(min.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, min.getAsDouble(), DELTA);

        OptionalDouble max = list.max();
        assertTrue(max.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, max.getAsDouble(), DELTA);
    }

    @Test
    public void testCopyDescendingFromSizeClampsToLogicalSize() {
        // regression (template-shared across all 8 primitive lists): copy(size, -1, -step) clamped
        // the start against the backing array's CAPACITY, exposing phantom elements beyond size
        final DoubleList padded = new DoubleList(10);
        padded.add(1d);
        padded.add(2d);
        padded.add(3d);
        assertEquals(DoubleList.of(3d, 2d, 1d), padded.copy(3, -1, -1));

        final DoubleList backed = new DoubleList(new double[] { 1d, 2d, 3d, 99d, 98d }, 3);
        assertEquals(DoubleList.of(3d, 2d, 1d), backed.copy(3, -1, -1));
        assertEquals(DoubleList.of(3d, 1d), backed.copy(3, -1, -2));
    }

    @Test
    public void testConversionSuppliersMustProduceCollectionsForEmptyRanges() {
        final DoubleList empty = new DoubleList();
        assertThrows(IllegalArgumentException.class, () -> empty.toCollection(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> empty.toCollection(0, 0, ignored -> null));
        assertThrows(IllegalArgumentException.class, () -> empty.toMultiset(0, 0, null));
        assertThrows(IllegalArgumentException.class, () -> empty.toMultiset(0, 0, ignored -> null));
    }

    @Test
    public void testShared_SharedBackingArray() {
        final double[] shared = { Double.longBitsToDouble(0x7ff8000000000001L), 0d, -0d, 42d, Double.longBitsToDouble(0x7ff8000000000002L), 0d, -0d };
        final DoubleList values = DoubleList.of(shared);

        assertTrue(values.removeAll(DoubleList.of(shared, 2)));
        assertArrayEquals(new double[] { -0d, 42d, -0d }, values.toArray());
    }

    @Test
    public void reviewFixes20260906_descendingCopyClampsAgainstSizeNotTheBackingArray() {
        // DoubleList.of(array, size) keeps the WHOLE array as backing but reports the smaller size, so the slots
        // past size hold phantom values. A descending copy(from, to, step) starts at `from`, and
        // N.copyOfRange clamps against the backing array's LENGTH - so without copy()'s own
        // `N.min(size - 1, fromIndex)` clamp those phantoms would be handed to the caller.
        final DoubleList withSpareCapacity = DoubleList.of(new double[] { 1, 2, 3, 4, 5 }, 3);

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
            final DoubleList self = DoubleList.of(1d, 2d, 3d);
            self.addAll(index, self);

            assertEquals(6, self.size(), "index=" + index);

            final DoubleList expected = DoubleList.of(1d, 2d, 3d);
            final DoubleList inserted = DoubleList.of(1d, 2d, 3d);
            expected.addAll(index, DoubleList.of(1d, 2d, 3d));
            assertEquals(expected.toString(), self.toString(), "index=" + index);
            assertEquals(3, inserted.size());
        }

        final DoubleList appended = DoubleList.of(1d, 2d, 3d);
        appended.addAll(appended);
        assertEquals("[1.0, 2.0, 3.0, 1.0, 2.0, 3.0]", appended.toString());

        // The interesting middle case, spelled out.
        final DoubleList middle = DoubleList.of(1d, 2d, 3d);
        middle.addAll(1, middle);
        assertEquals("[1.0, 1.0, 2.0, 3.0, 2.0, 3.0]", middle.toString());
    }

    @Test
    public void reviewFixes20260906_retainAllClearsWhereRemoveAllIsANoOp() {
        // Every class in this family documents it, and nothing anywhere pinned it: a null/empty
        // argument makes retainAll CLEAR the list (nothing can be retained), while the identically-shaped
        // removeAll leaves it untouched. Both overloads, both directions.
        final DoubleList a = DoubleList.of(1d, 2d, 3d);
        assertFalse(a.removeAll((DoubleList) null), "removeAll(null) is a no-op");
        assertEquals(3, a.size());
        assertFalse(a.removeAll((double[]) null));
        assertFalse(a.removeAll(new double[0]));
        assertFalse(a.removeAll(new DoubleList()));
        assertEquals(3, a.size(), "no removeAll overload may change the list for an empty argument");

        final DoubleList b = DoubleList.of(1d, 2d, 3d);
        assertTrue(b.retainAll((DoubleList) null), "retainAll(null) clears a non-empty list, and reports the change");
        assertEquals(0, b.size());

        final DoubleList c = DoubleList.of(1d, 2d, 3d);
        assertTrue(c.retainAll((double[]) null));
        assertEquals(0, c.size());

        final DoubleList d = DoubleList.of(1d, 2d, 3d);
        assertTrue(d.retainAll(new double[0]));
        assertEquals(0, d.size());

        final DoubleList e = DoubleList.of(1d, 2d, 3d);
        assertTrue(e.retainAll(new DoubleList()));
        assertEquals(0, e.size());

        // Clearing an ALREADY empty list changes nothing, so it reports false.
        final DoubleList empty = new DoubleList();
        assertFalse(empty.retainAll((DoubleList) null));
        assertEquals(0, empty.size());

        // A non-empty argument still behaves normally.
        final DoubleList f = DoubleList.of(1d, 2d, 3d);
        assertTrue(f.retainAll(DoubleList.of(1d)));
        assertEquals(1, f.size());
    }

    @Test
    public void reviewFixes20260911_lowerMedianSelectsWithoutSortingTheList() {
        // The class javadoc advertised median(), sum() and average(); none of the three exists on DoubleList.
        // lowerMedian() is the only central-tendency method, and it SELECTS rather than sorting, so the
        // receiver keeps its original order. N.lowerMedian short-circuits a range of 3 or fewer elements
        // (a 3-way median for the [1, 4) range below) and only uses kthLargest, a bounded PriorityQueue,
        // above that (the 7-element whole list below).
        final DoubleList ranged = DoubleList.of(10d, 3d, 1d, 2d, 9d);
        assertEquals(2.0d, ranged.lowerMedian(1, 4).get());
        assertEquals("[10.0, 3.0, 1.0, 2.0, 9.0]", ranged.toString());

        final DoubleList whole = DoubleList.of(9d, 1d, 8d, 2d, 7d, 3d, 6d);
        assertEquals(6.0d, whole.lowerMedian().get());
        assertEquals("[9.0, 1.0, 8.0, 2.0, 7.0, 3.0, 6.0]", whole.toString());

        assertFalse(new DoubleList().lowerMedian().isPresent());
    }

    @Test
    public void reviewFixes20260911_removeIfIsFailureAtomicButReplaceAllAndReplaceIfAreNot() {
        // removeIf promises the list is untouched when its predicate throws; its two neighbours write as they
        // visit and had said nothing. All three now document what they actually do.
        final DoubleList untouched = DoubleList.of(1d, -2d, 3d, -4d, 5d);
        assertThrows(IllegalStateException.class, () -> untouched.removeIf(x -> {
            if (x == 3d) {
                throw new IllegalStateException();
            }
            return x < 0;
        }));
        assertEquals("[1.0, -2.0, 3.0, -4.0, 5.0]", untouched.toString());

        final DoubleList partiallyMapped = DoubleList.of(1d, 2d, 3d);
        assertThrows(IllegalStateException.class, () -> partiallyMapped.replaceAll(x -> {
            if (x == 2d) {
                throw new IllegalStateException();
            }
            return x * 10d;
        }));
        assertEquals("[10.0, 2.0, 3.0]", partiallyMapped.toString());

        final DoubleList partiallyReplaced = DoubleList.of(1d, 2d, 3d);
        assertThrows(IllegalStateException.class, () -> partiallyReplaced.replaceIf(x -> {
            if (x == 2d) {
                throw new IllegalStateException();
            }
            return true;
        }, 0d));
        assertEquals("[0.0, 2.0, 3.0]", partiallyReplaced.toString());
    }

    @Test
    public void reviewFixes20260911_silentComparisonMethodsUseDoubleCompareSemantics() {
        // The dedup / set / search methods stated no double equality contract while fourteen siblings did.
        // They all use Double.compare: NaN matches NaN, and -0.0 never matches 0.0.
        assertEquals(-1, DoubleList.of(0.0d).binarySearch(-0.0d));
        assertEquals(1, DoubleList.of(-1.0, -0.0, 0.0, 1.0, Double.NaN).binarySearch(-0.0));
        assertEquals(2, DoubleList.of(-1.0, -0.0, 0.0, 1.0, Double.NaN).binarySearch(0.0));
        assertEquals(4, DoubleList.of(-1.0, -0.0, 0.0, 1.0, Double.NaN).binarySearch(Double.NaN));
        assertEquals(-2, DoubleList.of(1d, 0.0d, 2d).binarySearch(1, 2, -0.0d));

        assertTrue(DoubleList.of(0.0d).disjoint(DoubleList.of(-0.0d)));
        assertTrue(DoubleList.of(0.0d).disjoint(new double[] { -0.0d }));
        assertFalse(DoubleList.of(Double.NaN).disjoint(DoubleList.of(Double.NaN)));

        // disjoint() compares two different ways and the sentence covers the method, not one branch: the
        // one-element cases above take contains() -> N.equals -> Double.compare, while needToSet(min > 3
        // && max > 9) - which 12 elements against 5 satisfies - builds a Set<Double> and uses Double.equals.
        final DoubleList twelve = DoubleList.of(0.0d, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d, 10d, 11d);
        assertTrue(twelve.disjoint(DoubleList.of(-0.0d, 100d, 101d, 102d, 103d)));
        assertTrue(twelve.disjoint(new double[] { -0.0d, 100d, 101d, 102d, 103d }));
        final DoubleList twelveWithNaN = DoubleList.of(Double.NaN, 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d, 10d, 11d);
        assertFalse(twelveWithNaN.disjoint(DoubleList.of(Double.NaN, 100d, 101d, 102d, 103d)));
        assertFalse(twelveWithNaN.disjoint(new double[] { Double.NaN, 100d, 101d, 102d, 103d }));

        assertEquals("[NaN]", DoubleList.of(Double.NaN, 1d).intersection(DoubleList.of(Double.NaN)).toString());
        assertEquals("[]", DoubleList.of(0.0d).intersection(new double[] { -0.0d }).toString());
        assertEquals("[0.0]", DoubleList.of(0.0d).difference(DoubleList.of(-0.0d)).toString());
        assertEquals("[]", DoubleList.of(Double.NaN).difference(new double[] { Double.NaN }).toString());
        assertEquals("[-0.0, 0.0]", DoubleList.of(-0.0d).symmetricDifference(DoubleList.of(0.0d)).toString());
        assertEquals("[]", DoubleList.of(Double.NaN).symmetricDifference(new double[] { Double.NaN }).toString());

        // removeDuplicates has two code paths and the javadoc now says both apply the same rule.
        final DoubleList sortedZeros = DoubleList.of(-0.0d, 0.0d); // isSorted() -> the Double.compare fast path
        assertFalse(sortedZeros.removeDuplicates());
        assertEquals("[-0.0, 0.0]", sortedZeros.toString());
        final DoubleList unsortedZeros = DoubleList.of(0.0d, -0.0d); // -> the LinkedHashSet path
        assertFalse(unsortedZeros.removeDuplicates());
        assertEquals("[0.0, -0.0]", unsortedZeros.toString());
        // Double.compare(NaN, NaN) == 0, so of(NaN, NaN) is isSorted() and takes the fast path as well.
        final DoubleList nans = DoubleList.of(Double.NaN, Double.NaN);
        assertTrue(nans.removeDuplicates());
        assertEquals("[NaN]", nans.toString());
        // The fourth cell of the matrix: NaN on the LinkedHashSet path, which dedups by Double.equals.
        final DoubleList unsortedNaNs = DoubleList.of(Double.NaN, 1d, Double.NaN);
        assertFalse(unsortedNaNs.isSorted());
        assertTrue(unsortedNaNs.removeDuplicates());
        assertEquals("[NaN, 1.0]", unsortedNaNs.toString());

        assertEquals(2, DoubleList.of(-0.0d, 0.0d).distinct(0, 2).size());
        assertEquals(1, DoubleList.of(Double.NaN, Double.NaN).distinct(0, 2).size());

        // hashCode() routes through Double.hashCode(double), so it agrees with equals() on both special cases.
        assertEquals(DoubleList.of(Double.NaN).hashCode(), DoubleList.of(Double.NaN).hashCode());
        assertEquals(DoubleList.of(Double.NaN), DoubleList.of(Double.NaN));
        assertNotEquals(DoubleList.of(0.0d).hashCode(), DoubleList.of(-0.0d).hashCode());
        assertNotEquals(DoubleList.of(0.0d), DoubleList.of(-0.0d));
    }

    @Test
    public void reviewFixes20260911_sortFamilyUsesTheDoubleCompareTotalOrder() {
        // The sort family documented its NaN rule and never its signed-zero rule, yet -0.0 sorts BEFORE 0.0
        // in the Double.compare total order that all four methods impose.
        assertFalse(DoubleList.of(0.0d, -0.0d).isSorted());
        assertTrue(DoubleList.of(-0.0d, 0.0d).isSorted());

        final DoubleList ascending = DoubleList.of(0.0d, -0.0d, Double.NaN, 1d);
        ascending.sort();
        assertEquals("[-0.0, 0.0, 1.0, NaN]", ascending.toString());

        final DoubleList parallel = DoubleList.of(0.0d, -0.0d, Double.NaN, 1d);
        parallel.parallelSort();
        assertEquals("[-0.0, 0.0, 1.0, NaN]", parallel.toString());

        final DoubleList descending = DoubleList.of(0.0d, -0.0d, Double.NaN, 1d);
        descending.reverseSort();
        assertEquals("[NaN, 1.0, 0.0, -0.0]", descending.toString());

        // Belt and braces - toString() above already renders the sign as "-0.0"; pin the raw bits as well.
        assertEquals(Double.doubleToLongBits(-0.0d), Double.doubleToLongBits(ascending.get(0)));
        assertEquals(Double.doubleToLongBits(0.0d), Double.doubleToLongBits(ascending.get(1)));
    }

    @Test
    public void reviewFixes20260911_parallelSortMatchesSortAtEverySize() {
        // parallelSort() delegates to N.parallelSort, which runs the SEQUENTIAL Arrays.sort below a
        // 3000-element threshold or on a single-core host, and above it hands off to Arrays.parallelSort,
        // which applies a threshold of its own - so the javadoc no longer claims the sub-array split always
        // happens. Whatever either picks, the result must equal sort()'s at every size.
        final Random rnd = new Random(20260911L);

        for (final int size : new int[] { 0, 1, 2, 2999, 3000, 3001, 8193 }) {
            final double[] data = new double[size];

            for (int i = 0; i < size; i++) {
                data[i] = rnd.nextInt(1000) - 500;
            }

            final DoubleList sequential = DoubleList.copyOf(data);
            final DoubleList parallel = DoubleList.copyOf(data);
            sequential.sort();
            parallel.parallelSort();

            assertArrayEquals(sequential.toArray(), parallel.toArray(), "size=" + size);
            assertTrue(parallel.isSorted(), "size=" + size);
        }
    }
}
